/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.jdbc.kernel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Map;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.PrimaryRow;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowImpl;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.RowManagerImpl;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Update manager that writes SQL in object-level operation order
 *
 * @author Abe White
 */
public class OperationOrderUpdateManager
    extends AbstractUpdateManager {

    public boolean orderDirty() {
        return true;
    }

    protected RowManager newRowManager() {
        return new RowManagerImpl(true);
    }

    protected PreparedStatementManager newPreparedStatementManager
        (JDBCStore store, Connection conn) {
        return new PreparedStatementManagerImpl(store, conn);
    }

    protected Collection flush(RowManager rowMgr,
        PreparedStatementManager psMgr, Collection exceps) {
        RowManagerImpl rmimpl = (RowManagerImpl) rowMgr;

        // first take care of all secondary table deletes and 'all row' deletes
        // (which are probably secondary table deletes), since no foreign
        // keys ever rely on secondary table pks
        flush(rmimpl.getAllRowDeletes(), psMgr);
        flush(rmimpl.getSecondaryDeletes(), psMgr);

        // now do any 'all row' updates, which typically null keys
        flush(rmimpl.getAllRowUpdates(), psMgr);
        
        // map statemanagers to primaryrows
        Map smMap = mapStateManagers(rmimpl.getOrdered());
        
        // order rows to avoid constraint violations
        List orderedRows = orderRows(rmimpl, smMap);

        // gather any updates we need to avoid fk constraints on deletes
        Collection constraintUpdates = null;
        for (Iterator itr = orderedRows.iterator(); itr.hasNext();) {
            try {
                constraintUpdates = analyzeDeleteConstraints(rmimpl,
                    (PrimaryRow) itr.next(), constraintUpdates, smMap,
                    orderedRows);
            } catch (SQLException se) {
                exceps = addException(exceps, SQLExceptions.getStore
                    (se, dict));
            }
        }
        if (constraintUpdates != null) {
            flush(constraintUpdates, psMgr);
            constraintUpdates.clear();
        }
        
        // flush primary rows in order
        for (Iterator itr = orderedRows.iterator(); itr.hasNext();) {
            try {
                constraintUpdates = flushPrimaryRow(rmimpl, (PrimaryRow)
                    itr.next(), psMgr, constraintUpdates, smMap, orderedRows);
            } catch (SQLException se) {
                exceps = addException(exceps, SQLExceptions.getStore
                    (se, dict));
            }
        }

        if (constraintUpdates != null)
            flush(constraintUpdates, psMgr);

        // take care of all secondary table inserts and updates last, since
        // they may rely on previous inserts or updates, but nothing relies
        // on them
        flush(rmimpl.getSecondaryUpdates(), psMgr);

        // flush any left over prepared statements
        psMgr.flush();
        return exceps;
    }

    /**
     * Reorders all rows provided by the specified RowManagerImpl such that
     * no foreign key constraints are violated (assuming a proper schema).
     * @param rmimpl RowManagerImpl
     */
    private List orderRows(RowManagerImpl rmimpl, Map smMap) {
        List orderedRows = new ArrayList();
        if (rmimpl.getOrdered().size() > 0) {
            List inserts = new ArrayList(rmimpl.getInserts());
            List updates = new ArrayList(rmimpl.getUpdates());
            List deletes = new ArrayList(rmimpl.getDeletes());

            orderedRows.addAll(orderRows(inserts, smMap));
            orderedRows.addAll(updates);
            orderedRows.addAll(orderRows(deletes, smMap));
        }
        return orderedRows;
    }

    private List orderRows(List unorderedList, Map smMap) {
        List orderedList = new ArrayList();
        // this iterates in a while loop instead of with an iterator to
        // avoid ConcurrentModificationExceptions, as unorderedList is
        // mutated in the orderRow() invocation.
        while (!unorderedList.isEmpty()) {
            PrimaryRow nextRow = (PrimaryRow) unorderedList.get(0);
            orderRow(nextRow, unorderedList, orderedList, smMap, new Stack());
        }
        return orderedList;
    }

    private void orderRow(PrimaryRow currentRow, Collection unordered,
        List orderedList, Map smMap, Stack visitedRows) {
        if (orderedList.contains(currentRow)) {
            return;
        }

        // a circular reference found which means there is a problem
        // with the underlying database schema and/or class metadata
        // definitions. nothing can be done here to correct the problem.
        if (visitedRows.contains(currentRow)) {
            orderedList.addAll(unordered);
            unordered.clear();
            return;
        }

        if (currentRow.getAction() == Row.ACTION_INSERT) {
            ForeignKey[] fks = currentRow.getTable().getForeignKeys();
            OpenJPAStateManager sm;
            for (int i = 0; i < fks.length; i++) {
                sm = currentRow.getForeignKeySet(fks[i]);
                if (sm == null)
                    continue;
                // if the foreign key is new and it's primary key is
                // auto assigned
                PrimaryRow fkRow = (PrimaryRow) smMap.get(sm);
                if (fkRow.getAction() == Row.ACTION_INSERT) {
                    boolean nullable = true;
                    Column[] columns = fks[i].getColumns();
                    for (int j = 0; j < columns.length; j++) {
                        if (columns[j].isNotNull()) {
                            nullable = false;
                            break;
                        }
                    }
                    if (!nullable) {
                        visitedRows.push(currentRow);
                        PrimaryRow nextRow = (PrimaryRow) smMap.get(sm);
                        orderRow(nextRow, unordered, orderedList, smMap,
                            visitedRows);
                        visitedRows.pop();
                    }
                }
            }
            if (!orderedList.contains(currentRow)) {
                unordered.remove(currentRow);
                orderedList.add(currentRow);
            }
        } else if (currentRow.getAction() == Row.ACTION_DELETE) {
            ForeignKey[] fks = currentRow.getTable().getForeignKeys();
            OpenJPAStateManager sm;
            for (int i = 0; i < fks.length; i++) {
                sm = currentRow.getForeignKeySet(fks[i]);
                if (sm == null)
                    continue;
                PrimaryRow fkRow = (PrimaryRow) smMap.get(sm);
                // if the foreign key is going to be deleted
                if (!orderedList.contains(fkRow)
                    && fkRow.getAction() == Row.ACTION_DELETE) {
                    visitedRows.add(currentRow);
                    orderRow(fkRow, unordered, orderedList, smMap, visitedRows);
                    visitedRows.remove(currentRow);
                }
            }
            unordered.remove(currentRow);
            orderedList.add(0, currentRow);
        }
    }

    private Map mapStateManagers(List rowList) {
        Map smMap = new HashMap();
        for (Iterator iter = rowList.iterator(); iter.hasNext();) {
            PrimaryRow row = (PrimaryRow) iter.next();
            smMap.put(row.getPrimaryKey(), row);
        }
        return smMap;
    }

    /**
     * Analyze the delete constraints on the given row, gathering necessary
     * updates to null fks before deleting.
     */
    private Collection analyzeDeleteConstraints(RowManagerImpl rowMgr,
        PrimaryRow row, Collection updates, Map smMap, List orderedRows)
        throws SQLException {
        if (!row.isValid() || row.getAction() != Row.ACTION_DELETE)
            return updates;

        ForeignKey[] fks = row.getTable().getForeignKeys();
        OpenJPAStateManager sm;
        PrimaryRow rel;
        RowImpl update;
        for (int i = 0; i < fks.length; i++) {
            // when deleting ref fks we set the where value instead
            sm = row.getForeignKeySet(fks[i]);
            if (sm == null)
                sm = row.getForeignKeyWhere(fks[i]);
            if (sm == null)
                continue;
            PrimaryRow fkRow = (PrimaryRow) smMap.get(sm);
            int fkIndex = orderedRows.indexOf(fkRow);
            int rIndex = orderedRows.indexOf(row);
            if (fkIndex > rIndex)
                continue;

            // only need an update if we have an fk to a row that's being
            // deleted before we are
            rel = (PrimaryRow) rowMgr.getRow(fks[i].getPrimaryKeyTable(),
                Row.ACTION_DELETE, sm, false);
            if (rel == null || !rel.isValid()
                || rel.getIndex() >= row.getIndex())
                continue;

            // create an update to null the offending fk before deleting.  use
            // a primary row to be sure to copy delayed-flush pks/fks
            update = new PrimaryRow(row.getTable(), Row.ACTION_UPDATE, null);
            row.copyInto(update, true);
            update.setForeignKey(fks[i], row.getForeignKeyIO(fks[i]), null);
            if (updates == null)
                updates = new ArrayList();
            updates.add(update);
        }
        return updates;
    }

    /**
     * Flush the given row, creating deferred updates for dependencies.
     */
    private Collection flushPrimaryRow(RowManagerImpl rowMgr, PrimaryRow row,
        PreparedStatementManager psMgr, Collection updates, Map smMap,
        List orderedRows)
        throws SQLException {
        if (!row.isValid())
            return updates;

        // already analyzed deletes
        if (row.getAction() == Row.ACTION_DELETE) {
            psMgr.flush(row);
            return updates;
        }

        ForeignKey[] fks = row.getTable().getForeignKeys();
        OpenJPAStateManager sm;
        PrimaryRow rel;
        PrimaryRow update;
        for (int i = 0; i < fks.length; i++) {
            sm = row.getForeignKeySet(fks[i]);
            if (sm == null)
                continue;

            PrimaryRow fkRow = (PrimaryRow) smMap.get(sm);
            int fkIndex = orderedRows.indexOf(fkRow);
            int rIndex = orderedRows.indexOf(row);
            // consider sm flushed, no need to defer
            if (rIndex > fkIndex)
                continue;

            // only need an update if we have an fk to a row that's being
            // inserted after we are; if row is dependent on itself and no
            // fk, must be an auto-inc because otherwise we wouldn't have
            // recorded it
            rel = (PrimaryRow) rowMgr.getRow(fks[i].getPrimaryKeyTable(),
                Row.ACTION_INSERT, sm, false);
            if (rel == null || !rel.isValid()
                || rel.getIndex() < row.getIndex()
                || (rel == row && !fks[i].isDeferred() && !fks[i].isLogical()))
                continue;

            // don't insert or update with the given fk; create a deferred
            // update for after the rel row has been inserted; use a primary row
            // to prevent setting values until after flush to get auto-inc
            update = new PrimaryRow(row.getTable(), Row.ACTION_UPDATE, null);
            if (row.getAction() == Row.ACTION_INSERT)
                update.wherePrimaryKey(row.getPrimaryKey());
            else
                row.copyInto(update, true);
            update.setForeignKey(fks[i], row.getForeignKeyIO(fks[i]), sm);
            row.clearForeignKey(fks[i]);

            if (updates == null)
                updates = new ArrayList();
            updates.add(update);
        }

        if (row.isValid()) // if update, maybe no longer needed
            psMgr.flush(row);
        return updates;
    }

    /**
     * Flush the given collection of secondary rows.
     */
    protected void flush(Collection rows, PreparedStatementManager psMgr) {
        if (rows.isEmpty())
            return;

        RowImpl row;
        for (Iterator itr = rows.iterator(); itr.hasNext();) {
            row = (RowImpl) itr.next();
            if (row.isValid())
                psMgr.flush(row);
        }
    }
}

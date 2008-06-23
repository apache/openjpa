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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.PrimaryRow;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowImpl;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.RowManagerImpl;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.graph.DepthFirstAnalysis;
import org.apache.openjpa.lib.graph.Edge;
import org.apache.openjpa.lib.graph.Graph;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.OpenJPAException;

/**
 * <p>Standard update manager, capable of foreign key constraint evaluation.</p>
 *
 * @since 1.0.0
 */
public class ConstraintUpdateManager
    extends AbstractUpdateManager {

    private static final Localizer _loc = Localizer.forPackage
        (ConstraintUpdateManager.class);

    public boolean orderDirty() {
        return false;
    }

    protected PreparedStatementManager newPreparedStatementManager
        (JDBCStore store, Connection conn) {
        return new PreparedStatementManagerImpl(store, conn);
    }

    protected RowManager newRowManager() {
        return new RowManagerImpl(false);
    }

    protected Collection flush(RowManager rowMgr,
        PreparedStatementManager psMgr, Collection exceps) {
        RowManagerImpl rmimpl = (RowManagerImpl) rowMgr;

        // first take care of all secondary table deletes and 'all row' deletes
        // (which are probably secondary table deletes), since no foreign
        // keys ever rely on secondary table pks
        flush(rmimpl.getAllRowDeletes(), psMgr);
        flush(rmimpl.getSecondaryDeletes(), psMgr);

        // now do any 'all row' updates
        flush(rmimpl.getAllRowUpdates(), psMgr);

        // analyze foreign keys
        Collection inserts = rmimpl.getInserts();
        Collection updates = rmimpl.getUpdates();
        Collection deletes = rmimpl.getDeletes();
        Graph[] graphs = new Graph[2];    // insert graph, delete graph
        analyzeForeignKeys(inserts, updates, deletes, rmimpl, graphs);

        // flush insert graph, if any
        boolean autoAssign = rmimpl.hasAutoAssignConstraints();
        try {
            flushGraph(graphs[0], psMgr, autoAssign);
        } catch (SQLException se) {
            exceps = addException(exceps, SQLExceptions.getStore(se, dict));
        } catch (OpenJPAException ke) {
            exceps = addException(exceps, ke);
        }

        // flush the rest of the inserts and updates; inserts before updates
        // because some update fks might reference pks that have to be inserted
        flush(inserts, psMgr);
        flush(updates, psMgr);

        // flush the delete graph, if any
        try {
            flushGraph(graphs[1], psMgr, autoAssign);
        } catch (SQLException se) {
            exceps = addException(exceps, SQLExceptions.getStore(se, dict));
        } catch (OpenJPAException ke) {
            exceps = addException(exceps, ke);
        }

        // put the remainder of the deletes after updates because some updates
        // may be nulling fks to rows that are going to be deleted
        flush(deletes, psMgr);

        // take care of all secondary table inserts and updates last, since
        // they may rely on previous inserts or updates, but nothing relies
        // on them
        flush(rmimpl.getSecondaryUpdates(), psMgr);

        // flush any left over prepared statements
        psMgr.flush();
        return exceps;
    }

    /**
     * Analyze foreign key dependencies on the given rows
     * and create an insert and a delete graph to execute.  The insert
     * graph will be flushed before all other rows, and the delete graph will
     * be flushed after them.
     */
    private void analyzeForeignKeys(Collection inserts, Collection updates,
        Collection deletes, RowManagerImpl rowMgr, Graph[] graphs) {
        // if there are any deletes, we have to map the insert objects on their
        // oids so we'll be able to detect delete-then-insert-same-pk cases
        Map insertMap = null;
        OpenJPAStateManager sm;
        if (!deletes.isEmpty() && !inserts.isEmpty()) {
            insertMap = new HashMap((int) (inserts.size() * 1.33 + 1));
            for (Iterator itr = inserts.iterator(); itr.hasNext();) {
                sm = ((Row) itr.next()).getPrimaryKey();
                if (sm != null && sm.getObjectId() != null)
                    insertMap.put(sm.getObjectId(), sm);
            }
        }

        // first construct the graph for deletes; this may expand to include
        // inserts and updates as well if there are any inserts that rely on
        // deletes (delete-then-insert-same-pk cases)
        PrimaryRow row;
        Row row2;
        ForeignKey[] fks;
        OpenJPAStateManager fkVal;
        boolean ignoreUpdates = true;
        for (Iterator itr = deletes.iterator(); itr.hasNext();) {
            row = (PrimaryRow) itr.next();
            if (!row.isValid())
                continue;

            row2 = getInsertRow(insertMap, rowMgr, row);
            if (row2 != null) {
                ignoreUpdates = false;
                graphs[1] = addEdge(graphs[1], row, (PrimaryRow) row2, null);
            }

            // now check this row's fks against other deletes
            fks = row.getTable().getForeignKeys();
            for (int j = 0; j < fks.length; j++) {
                // when deleting ref fks they'll just set a where value, so
                // check both for fk updates (relation fks) and wheres (ref fks)
                fkVal = row.getForeignKeySet(fks[j]);
                if (fkVal == null)
                    fkVal = row.getForeignKeyWhere(fks[j]);
                if (fkVal == null)
                    continue;

                row2 = rowMgr.getRow(fks[j].getPrimaryKeyTable(),
                    Row.ACTION_DELETE, fkVal, false);
                if (row2 != null && row2.isValid() && row2 != row)
                    graphs[1] = addEdge(graphs[1], row, (PrimaryRow) row2,
                        fks[j]);
            }
        }

        if (ignoreUpdates)
            graphs[0] = analyzeAgainstInserts(inserts, rowMgr, graphs[0]);
        else {
            // put inserts *and updates* in the delete graph; they all rely
            // on each other
            graphs[1] = analyzeAgainstInserts(updates, rowMgr, graphs[1]);
            graphs[1] = analyzeAgainstInserts(inserts, rowMgr, graphs[1]);
        }
    }

    /**
     * Check to see if there is an insert for for the same table and primary
     * key values as the given delete row.
     */
    private Row getInsertRow(Map insertMap, RowManagerImpl rowMgr, Row row) {
        if (insertMap == null)
            return null;

        OpenJPAStateManager sm = row.getPrimaryKey();
        if (sm == null)
            return null;

        // look for a new object whose insert id is the same as this delete one
        Object oid = sm.getObjectId();
        OpenJPAStateManager nsm = (OpenJPAStateManager) insertMap.get(oid);
        if (nsm == null)
            return null;

        // found new object; get its row
        row = rowMgr.getRow(row.getTable(), Row.ACTION_INSERT, nsm, false);
        return (row == null || row.isValid()) ? row : null;
    }

    /**
     * Analyze the given rows against the inserts, placing dependencies
     * in the given graph.
     */
    private Graph analyzeAgainstInserts(Collection rows, RowManagerImpl rowMgr,
        Graph graph) {
        PrimaryRow row;
        Row row2;
        ForeignKey[] fks;
        Column[] cols;
        for (Iterator itr = rows.iterator(); itr.hasNext();) {
            row = (PrimaryRow) itr.next();
            if (!row.isValid())
                continue;

            // check this row's fks against inserts; a logical fk to an auto-inc
            // column is treated just as actual database fk because the result
            // is the same: the pk row has to be inserted before the fk row
            fks = row.getTable().getForeignKeys();
            for (int j = 0; j < fks.length; j++) {
                if (row.getForeignKeySet(fks[j]) == null)
                    continue;

                // see if this row is dependent on another.  if it's only
                // depenent on itself, see if the fk is logical or deferred, in
                // which case it must be an auto-inc because otherwise we
                // wouldn't have recorded it
                row2 = rowMgr.getRow(fks[j].getPrimaryKeyTable(),
                    Row.ACTION_INSERT, row.getForeignKeySet(fks[j]), false);
                if (row2 != null && row2.isValid() && (row2 != row
                    || fks[j].isDeferred() || fks[j].isLogical()))
                    graph = addEdge(graph, (PrimaryRow) row2, row, fks[j]);
            }

            // see if there are any relation id columns dependent on
            // auto-inc objects
            cols = row.getTable().getRelationIdColumns();
            for (int j = 0; j < cols.length; j++) {
                OpenJPAStateManager sm = row.getRelationIdSet(cols[j]);
                if (sm == null)
                    continue;

                row2 = rowMgr.getRow(getBaseTable(sm), Row.ACTION_INSERT,
                    sm, false);
                if (row2 != null && row2.isValid())
                    graph = addEdge(graph, (PrimaryRow) row2, row, cols[j]);
            }
        }
        return graph;
    }

    /**
     * Return the base table for the given instance.
     */
    private static Table getBaseTable(OpenJPAStateManager sm) {
        ClassMapping cls = (ClassMapping) sm.getMetaData();
        while (cls.getJoinablePCSuperclassMapping() != null)
            cls = cls.getJoinablePCSuperclassMapping();
        return cls.getTable();
    }

    /**
     * Add an edge between the given rows in the given foreign key graph.
     */
    private Graph addEdge(Graph graph, PrimaryRow row1, PrimaryRow row2,
        Object fk) {
        // delay creation of the graph
        if (graph == null)
            graph = new Graph();

        row1.setDependent(true);
        row2.setDependent(true);
        graph.addNode(row1);
        graph.addNode(row2);

        // add an edge from row1 to row2, and set the fk causing the
        // dependency as the user object so we can retrieve it when resolving
        // circular constraints
        Edge edge = new Edge(row1, row2, true);
        edge.setUserObject(fk);
        graph.addEdge(edge);

        return graph;
    }

    /**
     * Flush the given graph of rows in the proper order.
     * @param graph The graph of statements to be walked
     * @param psMgr The prepared statement manager to use to issue the
     * statements
     * @param autoAssign Whether any of the rows in the graph have any
     * auto-assign constraints
     */
    protected void flushGraph(Graph graph, PreparedStatementManager psMgr,
        boolean autoAssign)
        throws SQLException {
        if (graph == null)
            return;

        DepthFirstAnalysis dfa = newDepthFirstAnalysis(graph, autoAssign);
        Collection nodes = dfa.getSortedNodes();
        Collection backs = dfa.getEdges(Edge.TYPE_BACK);

        // handle circular constraints:
        // - if deleted row A has a ciricular fk to deleted row B, then use an
        //   update statement to null A's fk to B
        // - if inserted row A has a circular fk to updated/inserted row B,
        //   then null the fk in the B row object, and after flushing, use
        //   an update to set the fk to back to A
        Collection insertUpdates = null;
        Collection deleteUpdates = null;
        PrimaryRow row;
        RowImpl update;
        Edge edge;
        ForeignKey fk;
        Column col;
        for (Iterator itr = backs.iterator(); itr.hasNext();) {
            edge = (Edge) itr.next();
            if (edge.getUserObject() == null)
                throw new InternalException(_loc.get("del-ins-cycle"));

            // use a primary row update to prevent setting pk and fk values
            // until after flush, to get latest auto-increment values
            row = (PrimaryRow) edge.getTo();
            if (row.getAction() == Row.ACTION_DELETE) {
                // copy where conditions into new update that nulls the fk
                row = (PrimaryRow) edge.getFrom();
                update = new PrimaryRow(row.getTable(), Row.ACTION_UPDATE,null);
                row.copyInto(update, true);
                if (edge.getUserObject() instanceof ForeignKey) {
                    fk = (ForeignKey) edge.getUserObject();
                    update.setForeignKey(fk, row.getForeignKeyIO(fk), null);
                } else
                    update.setNull((Column) edge.getUserObject());

                if (deleteUpdates == null)
                    deleteUpdates = new LinkedList();
                deleteUpdates.add(update);
            } else {
                // copy where conditions into new update that sets the fk
                update = new PrimaryRow(row.getTable(), Row.ACTION_UPDATE,null);
                if (row.getAction() == Row.ACTION_INSERT) {
                    if (row.getPrimaryKey() == null)
                        throw new InternalException(_loc.get("ref-cycle"));
                    update.wherePrimaryKey(row.getPrimaryKey());
                } else
                    row.copyInto(update, true);
                if (edge.getUserObject() instanceof ForeignKey) {
                    fk = (ForeignKey) edge.getUserObject();
                    update.setForeignKey(fk, row.getForeignKeyIO(fk),
                        row.getForeignKeySet(fk));
                    row.clearForeignKey(fk);
                } else {
                    col = (Column) edge.getUserObject();
                    update.setRelationId(col, row.getRelationIdSet(col),
                        row.getRelationIdCallback(col));
                    row.clearRelationId(col);
                }

                if (insertUpdates == null)
                    insertUpdates = new LinkedList();
                insertUpdates.add(update);
            }
        }

        // flush delete updates to null fks, then all rows in order, then
        // the insert updates to set circular fk values
        if (deleteUpdates != null)
            flush(deleteUpdates, psMgr);
        for (Iterator itr = nodes.iterator(); itr.hasNext();)
            psMgr.flush((RowImpl) itr.next());
        if (insertUpdates != null)
            flush(insertUpdates, psMgr);
	}

    /**
     * Create a new {@link DepthFirstAnalysis} suitable for the given graph
     * and auto-assign settings.
     */
    protected DepthFirstAnalysis newDepthFirstAnalysis(Graph graph,
        boolean autoAssign) {
        return new DepthFirstAnalysis(graph);
    }

    /**
     * Flush the given collection of secondary rows.
     */
    protected void flush(Collection rows, PreparedStatementManager psMgr) {
        if (rows.size() == 0)
            return;

        RowImpl row;
        for (Iterator itr = rows.iterator(); itr.hasNext(); ) {
            row = (RowImpl) itr.next();
            if (row.isValid() && !row.isDependent())
                psMgr.flush(row);
        }
    }
}
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
package org.apache.openjpa.jdbc.meta.strats;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.lib.log.Log;

/**
 * Uses a timestamp for optimistic versioning.
 *
 * @author Abe White
 */
public class TimestampVersionStrategy extends ColumnVersionStrategy {
    private static final long serialVersionUID = 1L;
    public static final String ALIAS = "timestamp";

    @Override
    public String getAlias() {
        return ALIAS;
    }

    @Override
    protected int getJavaType() {
        return JavaSQLTypes.TIMESTAMP;
    }

    /**
     * The granularity, in nanoseconds, of the values the version column
     * holds when that is coarser than what the dictionary assumes; 0 while
     * unknown or not coarser.
     */
    private volatile int _precision = 0;
    private volatile boolean _precisionResolved = false;

    /**
     * The granularity resolved for a version column (0 if not coarser), by
     * column instance.
     */
    private static final Map<Column, Integer> RESOLVED =
        Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    protected Object nextVersion(Object version) {
        Timestamp ts = currentTimestamp();
        int precision = _precision;
        return precision > 0 ? StateManagerImpl.roundTimestamp(ts, precision) : ts;
    }

    /**
     * The current time, which the next version is derived from.
     */
    protected Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    public void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        resolvePrecision(store);
        super.insert(sm, store, rm);
    }

    @Override
    public void update(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        resolvePrecision(store);
        super.update(sm, store, rm);
    }

    /**
     * The version value has to compare equal to the stored one in the
     * <code>WHERE</code> clause of the next update, but a column may keep
     * fewer fractional digits of a second than the dictionary assumes - a
     * MySQL <code>TIMESTAMP</code> or <code>DATETIME</code> without a
     * fractional seconds precision keeps none, a SQL Server
     * <code>DATETIME</code> has a granularity of 1/300 second - and the
     * in-memory version, as well as the value bound, would then never match
     * the stored one. If it is coarser than the dictionary's
     * {@link DBDictionary#datePrecision}, the versions are generated at the
     * precision of the column.
     * <p>
     * The precision declared in the mapping (<code>secondPrecision</code>,
     * or a precision set on the column) is used as it is. Otherwise it is
     * read from the database metadata on the first insert or update, and if
     * coarser recorded on the column so that
     * {@link DBDictionary#getTimestampPrecision} binds the values at it. The
     * result is kept per column: several entities mapped to the same table
     * share the column, and the lookup is made once for all of them.
     */
    private void resolvePrecision(JDBCStore store) {
        if (_precisionResolved) {
            return;
        }
        Column[] cols = vers.getColumns();
        if (cols == null || cols.length != 1) {
            _precisionResolved = true;
            return;
        }
        Column col = cols[0];
        Integer known = RESOLVED.get(col);
        if (known != null) {
            _precision = known;
            _precisionResolved = true;
            return;
        }
        DBDictionary dict = store.getDBDictionary();
        int digits = col.getSecondPrecision() >= 0 ? col.getSecondPrecision() : col.getPrecision();
        int precision = 0;
        if (digits >= 0) {
            Column probe = new Column();
            probe.setPrecision(digits);
            probe.setTypeIdentifier(col.getTypeIdentifier());
            precision = coarserPrecision(dict, probe);
        } else if (!dict.requiresAutoCommitForMetaData) {
            // the lookup would end the transaction of a dictionary that needs
            // auto-commit for metadata operations
            Column dbCol = lookupColumn(store, dict, col);
            if (dbCol != null) {
                precision = coarserPrecision(dict, dbCol);
                if (precision > 0) {
                    col.setPrecision(dbCol.getPrecision());
                }
            }
        }
        RESOLVED.put(col, precision);
        _precision = precision;
        _precisionResolved = true;
    }

    /**
     * The granularity of the given column if it is coarser than the
     * dictionary's default, 0 otherwise.
     */
    private static int coarserPrecision(DBDictionary dict, Column col) {
        int precision = dict.getTimestampPrecision(col);
        return precision > dict.getTimestampPrecision(null) ? precision : 0;
    }

    /**
     * Reads the version column from the database metadata, with its
     * precision set to the number of fractional digits it keeps. Null if
     * that cannot be told.
     */
    private Column lookupColumn(JDBCStore store, DBDictionary dict, Column col) {
        Table table = col.getTable();
        Column found = null;
        Connection conn = store.getConnection();
        try {
            Column[] dbCols = dict.getColumns(conn.getMetaData(), DBIdentifier.NULL,
                table.getSchemaIdentifier(), table.getIdentifier(), col.getIdentifier(), conn);
            for (int i = 0; dbCols != null && i < dbCols.length; i++) {
                int d = dbCols[i].getDecimalDigits();
                if (d < 0 || d > 9 || (found != null && (found.getDecimalDigits() != d
                    || !found.getTypeIdentifier().equals(dbCols[i].getTypeIdentifier())))) {
                    // unusable or ambiguous (same table name in several schemas)
                    return null;
                }
                found = dbCols[i];
            }
        } catch (SQLException se) {
            Log log = vers.getMappingRepository().getLog();
            if (log.isTraceEnabled()) {
                log.trace("Could not read the precision of version column " + col + ": " + se);
            }
            return null;
        } finally {
            try {
                conn.close();
            } catch (SQLException se) {
                // ignore
            }
        }
        if (found != null) {
            found.setPrecision(found.getDecimalDigits());
        }
        return found;
    }

    @Override
    public Map<Column, Object> getBulkUpdateValues() {
        Column[] cols = vers.getColumns();
        Map<Column, Object> map = new HashMap<>(cols.length);
        Object d = nextVersion(null);
        for (Column col : cols) {
            map.put(col, d);
        }
        return map;
    }
}

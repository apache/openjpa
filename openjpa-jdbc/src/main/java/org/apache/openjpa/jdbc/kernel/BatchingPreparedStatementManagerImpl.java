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

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowImpl;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.OptimisticException;

/**
 * Batch prepared statement manager implementation. This prepared statement
 * manager will utilize the JDBC addBatch() and exceuteBatch() to batch the SQL
 * statements together to improve the execution performance.
 * 
 * @author Teresa Kan
 */

public class BatchingPreparedStatementManagerImpl extends
        PreparedStatementManagerImpl {

    private final static Localizer _loc = Localizer
            .forPackage(BatchingPreparedStatementManagerImpl.class);

    private Map _cacheSql = null;
    private int _batchLimit;
    private boolean _disableBatch = false;
    private transient Log _log = null;

    /**
     * Constructor. Supply connection.
     */
    public BatchingPreparedStatementManagerImpl(JDBCStore store,
            Connection conn, int batchLimit) {

        super(store, conn);
        _batchLimit = batchLimit;
        _log = store.getConfiguration().getLog(JDBCConfiguration.LOG_JDBC);
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("batch_limit", String.valueOf(_batchLimit)));
    }

    /**
     * Flush the given row. This method will cache the statement in a cache. The
     * statement will be executed in the flush() method.
     */
    protected void flushInternal(RowImpl row) throws SQLException {
        if (_batchLimit == 0 || _disableBatch) {
            super.flushInternal(row);
            return;
        }
        Column[] autoAssign = null;
        if (row.getAction() == Row.ACTION_INSERT)
            autoAssign = row.getTable().getAutoAssignedColumns();

        // prepare statement
        String sql = row.getSQL(_dict);
        OpenJPAStateManager sm = row.getPrimaryKey();
        ClassMapping cmd = null;
        if (sm != null)
            cmd = (ClassMapping) sm.getMetaData();
        // validate batch capability
        _disableBatch = _dict.validateBatchProcess(row, autoAssign, sm, cmd);

        // process the sql statement, either execute it immediately or
        // cache them.
        processSql(sql, row);

        // set auto assign values
        if (autoAssign != null && autoAssign.length > 0 && sm != null) {
            Object val;
            for (int i = 0; i < autoAssign.length; i++) {
                val = _dict.getGeneratedKey(autoAssign[i], _conn);
                cmd.assertJoinable(autoAssign[i]).setAutoAssignedValue(sm,
                        _store, autoAssign[i], val);
            }
            sm.setObjectId(ApplicationIds.create(sm.getPersistenceCapable(),
                    cmd));
        }
    }

    private void processSql(String sql, RowImpl row) throws SQLException {
        ArrayList temprow;

        if (_cacheSql == null)
            _cacheSql = Collections.synchronizedMap(new LinkedHashMap());
        if (_disableBatch) {
            // if there were some statements batched before, then
            // we need to flush them out first before processing the
            // current non batch process.
            if (!_cacheSql.isEmpty())
                flush();
            execute(sql, row);

        } else {
            // else start batch support. If the sql string is in the cache,
            // just adds the row to the cache
            if (_cacheSql.containsKey(sql)) {
                temprow = (ArrayList) _cacheSql.get(sql);
                temprow.add(row);
                _cacheSql.put(sql, temprow);
            } else {
                // no sql exists in the cache, cache the sql string and its rows
                ArrayList inputrow = new ArrayList();
                inputrow.add(row);
                _cacheSql.put(sql, inputrow);
            }
        } // end of batch support
    }

    private void execute(String sql, RowImpl row) throws SQLException {
        PreparedStatement stmnt = null;
        try {
            ResultSet rs = null;
            stmnt = _conn.prepareStatement(sql);
            row.flush(stmnt, _dict, _store);
            int count = stmnt.executeUpdate();
            if (count != 1) {
                Object failed = row.getFailedObject();
                if (failed != null)
                    _exceptions.add(new OptimisticException(failed));
                else if (row.getAction() == Row.ACTION_INSERT)
                    throw new SQLException(_loc.get(
                            "update-failed-no-failed-obj",
                            String.valueOf(count), sql).getMessage());
            }
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, row.getFailedObject(), _dict);
        } finally {
            try {
                if (stmnt != null)
                    stmnt.close();
            } catch (SQLException se) {
                // ignore the exception for this case.
            }
        }
    }

    public void flush() {
        PreparedStatement ps = null;
        ArrayList list;
        RowImpl onerow = null;

        // go thru the cache to process all the sql stmt.
        if (_cacheSql == null || _cacheSql.isEmpty()) {
            super.flush();
            return;
        }
        Set e = _cacheSql.keySet();

        for (Iterator itr = e.iterator(); itr.hasNext();) {
            String key = (String) itr.next();
            try {
                ps = _conn.prepareStatement(key);
            } catch (SQLException se) {
                throw SQLExceptions.getStore(se, ps, _dict);
            }
            list = (ArrayList) _cacheSql.get(key);
            if (list == null) {
                return;
            }

            // if only 1 row for this statement, then execute it right away
            int rowsize = list.size();

            try {
                if (rowsize == 1) {
                    onerow = (RowImpl) list.get(0);
                    onerow.flush(ps, _dict, _store);
                    int count = ps.executeUpdate();
                    if (count != 1) {
                        Object failed = onerow.getFailedObject();
                        if (failed != null)
                            _exceptions.add(new OptimisticException(failed));
                        else if (onerow.getAction() == Row.ACTION_INSERT)
                            throw new SQLException(_loc.get(
                                    "update-failed-no-failed-obj",
                                    String.valueOf(count), key).getMessage());
                    }
                } else {
                    // has more than one rows for this statement, use addBatch
                    int count = 0;
                    for (int i = 0; i < list.size(); i++) {
                        onerow = (RowImpl) list.get(i);
                        if (count < _batchLimit || _batchLimit == -1) {
                            onerow.flush(ps, _dict, _store);
                            ps.addBatch();
                            count++;

                        } else {
                            // reach the batchLimit , execute it
                            try {
                                int[] rtn = ps.executeBatch();
                                checkUpdateCount(rtn, onerow, key);
                            } catch (BatchUpdateException bex) {
                                SQLException sqex = bex.getNextException();
                                if (sqex == null)
                                    sqex = bex;
                                throw SQLExceptions.getStore(sqex, ps, _dict);
                            }
                            onerow.flush(ps, _dict, _store);
                            ps.addBatch();
                            count = 1; // reset the count to 1 for new batch
                        }
                    }
                    // end of the loop, execute the batch
                    try {
                        int[] rtn = ps.executeBatch();
                        checkUpdateCount(rtn, onerow, key);
                    } catch (BatchUpdateException bex) {
                        SQLException sqex = bex.getNextException();
                        if (sqex == null)
                            sqex = bex;
                        throw SQLExceptions.getStore(sqex, ps, _dict);
                    }
                }
            } catch (SQLException se) {
                SQLException sqex = se.getNextException();
                if (sqex == null)
                    sqex = se;
                throw SQLExceptions.getStore(sqex, ps, _dict);
            }
            try {
                ps.close();
            } catch (SQLException sqex) {
                throw SQLExceptions.getStore(sqex, ps, _dict);
            }
        }
        // instead of calling _cacheSql.clear, null it out to improve the
        // performance.
        _cacheSql = null;
    }

    private void checkUpdateCount(int[] count, RowImpl row, String sql)
            throws SQLException {
        int cnt = 0;
        Object failed = null;
        for (int i = 0; i < count.length; i++) {
            cnt = count[i];
            switch (cnt) {
            case Statement.EXECUTE_FAILED: // -3
                failed = row.getFailedObject();
                if (failed != null || row.getAction() == Row.ACTION_UPDATE)
                    _exceptions.add(new OptimisticException(failed));
                else if (row.getAction() == Row.ACTION_INSERT)
                    throw new SQLException(_loc.get(
                            "update-failed-no-failed-obj",
                            String.valueOf(count[i]), sql).getMessage());
                break;
            case Statement.SUCCESS_NO_INFO: // -2
                if (_log.isTraceEnabled())
                    _log.trace(_loc.get("batch_update_info",
                            String.valueOf(cnt), sql).getMessage());
                break;
            case 0: // no row is inserted, treats it as failed
                // case
                failed = row.getFailedObject();
                if ((failed != null || row.getAction() == Row.ACTION_INSERT))
                    throw new SQLException(_loc.get(
                            "update-failed-no-failed-obj",
                            String.valueOf(count[i]), sql).getMessage());
            }
        }
    }
}

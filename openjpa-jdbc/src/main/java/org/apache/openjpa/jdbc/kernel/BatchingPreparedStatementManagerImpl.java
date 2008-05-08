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
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowImpl;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
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

    private String _batchedSql = null;
    private List _batchedRows = new ArrayList();
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
     * Flush the given row immediately or deferred the flush in batch.
     */
    protected void flushAndUpdate(RowImpl row) throws SQLException {
        if (isBatchDisabled(row)) {
            // if there were some statements batched before, then
            // we need to flush them out first before processing the
            // current non batch process.
            flushBatch();

            super.flushAndUpdate(row);
        } else {
            // process the SQL statement, either execute it immediately or
            // batch it for later execution.
            String sql = row.getSQL(_dict);
            if (_batchedSql == null) {
                // brand new SQL
                _batchedSql = sql;
            } else if (!sql.equals(_batchedSql)) {
                // SQL statements changed.
                switch (_batchedRows.size()) {
                case 0:
                    break;
                case 1:
                    // single entry in cache, direct SQL execution. 
                    super.flushAndUpdate((RowImpl) _batchedRows.get(0));
                    _batchedRows.clear();
                    break;
                default:
                    // flush all entries in cache in batch.
                    flushBatch();
                }
                _batchedSql = sql;
            }
            _batchedRows.add(row);
        }
    }

    /*
     * Compute if batching is disabled, based on values of batch limit
     * and database characteristics.
     */
    private boolean isBatchDisabled(RowImpl row) {
        boolean rtnVal = true;
        if (_batchLimit != 0 && !_disableBatch) {
            String sql = row.getSQL(_dict);
            OpenJPAStateManager sm = row.getPrimaryKey();
            ClassMapping cmd = null;
            if (sm != null)
                cmd = (ClassMapping) sm.getMetaData();
            Column[] autoAssign = null;
            if (row.getAction() == Row.ACTION_INSERT)
                autoAssign = row.getTable().getAutoAssignedColumns();
            // validate batch capability
            _disableBatch = _dict
                .validateBatchProcess(row, autoAssign, sm, cmd);
            rtnVal = _disableBatch;
        }
        return rtnVal;
    }
    
    /**
     * flush all cached up statements to be executed as a single or batched
     * prepared statements.
     */
    protected void flushBatch() {
        if (_batchedSql != null && _batchedRows.size() > 0) {
            PreparedStatement ps = null;
            try {
                RowImpl onerow = null;
                ps = _conn.prepareStatement(_batchedSql);
                if (_batchedRows.size() == 1) {
                    // execute a single row.
                    onerow = (RowImpl) _batchedRows.get(0);
                    flushSingleRow(onerow, ps);
                } else {
                    // cache has more than one rows, execute as batch.
                    int count = 0;
                    int batchedRowsBaseIndex = 0;
                    Iterator itr = _batchedRows.iterator();
                    while (itr.hasNext()) {
                        onerow = (RowImpl) itr.next();
                        if (_batchLimit == 1) {
                            flushSingleRow(onerow, ps);
                        } else {
                            if (count < _batchLimit || _batchLimit == -1) {
                                onerow.flush(ps, _dict, _store);
                                ps.addBatch();
                                count++;
                            } else {
                                // reach the batchLimit, execute the batch
                                int[] rtn = ps.executeBatch();
                                checkUpdateCount(rtn, batchedRowsBaseIndex);

                                batchedRowsBaseIndex += _batchLimit;

                                onerow.flush(ps, _dict, _store);
                                ps.addBatch();
                                // reset the count to 1 for new batch
                                count = 1;
                            }
                        }
                    }
                    // end of the loop, execute the batch
                    int[] rtn = ps.executeBatch();
                    checkUpdateCount(rtn, batchedRowsBaseIndex);
                }
            } catch (SQLException se) {
                SQLException sqex = se.getNextException();
                if (sqex == null)
                    sqex = se;
                throw SQLExceptions.getStore(sqex, ps, _dict);
            } finally {
                _batchedSql = null;
                _batchedRows.clear();
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException sqex) {
                        throw SQLExceptions.getStore(sqex, ps, _dict);
                    }
                }
            }
        }
    }

    /*
     * Execute an update of a single row.
     */
    private void flushSingleRow(RowImpl row, PreparedStatement ps)
        throws SQLException {
        row.flush(ps, _dict, _store);
        int count = ps.executeUpdate();
        if (count != 1) {
            Object failed = row.getFailedObject();
            if (failed != null)
                _exceptions.add(new OptimisticException(failed));
            else if (row.getAction() == Row.ACTION_INSERT)
                throw new SQLException(_loc.get("update-failed-no-failed-obj",
                    String.valueOf(count), row.getSQL(_dict)).getMessage());
        }
    }

    /*
     * Process executeBatch function array of return counts.
     */
    private void checkUpdateCount(int[] count, int batchedRowsBaseIndex)
        throws SQLException {
        int cnt = 0;
        Object failed = null;
        for (int i = 0; i < count.length; i++) {
            cnt = count[i];
            RowImpl row = (RowImpl) _batchedRows.get(batchedRowsBaseIndex + i);
            failed = row.getFailedObject();
            switch (cnt) {
            case Statement.EXECUTE_FAILED: // -3
                if (failed != null || row.getAction() == Row.ACTION_UPDATE)
                    _exceptions.add(new OptimisticException(failed));
                else if (row.getAction() == Row.ACTION_INSERT)
                    throw new SQLException(_loc.get(
                        "update-failed-no-failed-obj",
                        String.valueOf(count[i]), _batchedSql).getMessage());
                break;
            case Statement.SUCCESS_NO_INFO: // -2
                if (failed != null || row.getAction() == Row.ACTION_UPDATE)
                    _exceptions.add(new OptimisticException(failed));
                else if (_log.isTraceEnabled())
                    _log.trace(_loc.get("batch_update_info",
                        String.valueOf(cnt), _batchedSql).getMessage());
                break;
            case 0: // no row is inserted, treats it as failed
                // case
                if (failed != null || row.getAction() == Row.ACTION_UPDATE)
                    _exceptions.add(new OptimisticException(failed));
                else
                    throw new SQLException(_loc.get(
                        "update-failed-no-failed-obj",
                        String.valueOf(count[i]), _batchedSql).getMessage());
            }
        }
    }
}

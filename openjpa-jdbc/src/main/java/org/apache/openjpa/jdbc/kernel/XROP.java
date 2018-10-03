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


import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.sql.ResultSetResult;
import org.apache.openjpa.lib.rop.BatchedResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.util.InternalException;

/**
 * Gets multiple Result Object Providers each with different mapping.
 *
 * @author Pinaki Poddar
 *
 */
public class XROP implements BatchedResultObjectProvider {
    private final CallableStatement stmt;
    private final JDBCFetchConfiguration fetch;
    private final List<QueryResultMapping> _multi;
    private final List<Class<?>> _resultClasses;
    private int index;
    private final JDBCStore store;
    // Result of first execution
    private boolean executionResult;

    public XROP(List<QueryResultMapping> mappings, List<Class<?>> classes,
                JDBCStore store, JDBCFetchConfiguration fetch,
                CallableStatement stmt) {
        _multi = mappings;
        _resultClasses = classes;
        this.stmt = stmt;
        this.fetch = fetch;
        this.store = store;
    }

    /**
     * Does not support random access.
     */
    @Override
    public boolean supportsRandomAccess() {
        return false;
    }

    /**
     * Opens this provider by executing the underlying Statment.
     * The result of execution is memorized.
     */
    @Override
    public void open() throws Exception {
        executionResult = stmt.execute();
    }

    /**
     * Gets the current result set, wraps it with a {@link ResultSetResult}, then wraps
     * again with appropriate ROP based on the result set mapping.
     * <br>
     * The ResultSet and the associated connection must not be closed.
     *
     * @return a provider or null if the underlying statement has no more results.
     */
    @Override
    public ResultObjectProvider getResultObject() throws Exception {
        ResultSet rs = stmt.getResultSet();
        if (rs == null)
            return null;

        ResultSetResult res = new ResultSetResult(rs, store.getDBDictionary());
        res.setCloseConnection(false);
        res.setCloseStatement(false);
        try {
            if (_resultClasses != null && _resultClasses.size() > index) {
                Class<?> mapping = _resultClasses.get(index);
                if (mapping != null) {
                    return new GenericResultObjectProvider(mapping, store, fetch, res);
                }
            }
            if (_multi != null && _multi.size() > index) {
                QueryResultMapping mapping = _multi.get(index);
                if (mapping != null) {
                    return new MappedQueryResultObjectProvider(mapping, store, fetch, res);
                }
            }
            return new SQLProjectionResultObjectProvider(store, fetch, res, null);
        } finally {
            index++;
        }
    }


    /**
     * Closes the underlying statement.
     */
    @Override
    public void close() throws Exception {
        stmt.close();
    }

    /**
     * Affirms if more result sets are available.
     * <br>
     * <b.NOTE</b>: The side effect is to advance to the statement's next result.
     */
    @Override
    public boolean hasMoreResults() {
        try {
            return stmt.getMoreResults();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean getExecutionResult() {
        return executionResult;
    }

    /**
     * Gets the update count, provided the current result of the statement is not a result set.
     */
    @Override
    public int getUpdateCount() {
        try {
            return stmt.getUpdateCount();
        } catch (SQLException e) {
            return -1;
        }
    }

    @Override
    public Object getOut(String name) {
        try {
            return stmt.getObject(name);
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public Object getOut(int position) {
        try {
            return stmt.getObject(position);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Throws exception.
     */
    @Override
    public boolean next() throws Exception {
        throw new InternalException();
    }

    /**
     * Returns false.
     */
    @Override
    public boolean absolute(int pos) throws Exception {
        return false;
    }

    /**
     * Returns {@code -1}.
     */
    @Override
    public int size() throws Exception {
        return -1;
    }

    /**
     * Throws exception.
     */
    @Override
    public void reset() throws Exception {
        throw new InternalException();
    }

    @Override
    public void handleCheckedException(Exception e) {
        throw new RuntimeException(e);
    }

}

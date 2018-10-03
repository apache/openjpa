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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.ResultSetResult;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.ResultPacker;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UnsupportedException;

/**
 * Provides all column data in a {@link ResultSet}.
 *
 * @author Abe White
 */
class SQLProjectionResultObjectProvider
    implements ResultObjectProvider {

    private final JDBCStore _store;
    private final JDBCFetchConfiguration _fetch;
    private final ResultSetResult _res;
    private final ResultPacker _packer;
    private final int _cols;

    /**
     * Constructor.
     *
     * @param res the result data
     * @param cls the result class; may be null for the default
     */
    public SQLProjectionResultObjectProvider(JDBCStore store,
        JDBCFetchConfiguration fetch, ResultSetResult res, Class cls)
        throws SQLException {
        _store = store;
        _fetch = fetch;

        ResultSetMetaData meta = res.getResultSet().getMetaData();
        _res = res;
        _cols = meta.getColumnCount();

        if (cls != null) {
            String[] aliases = new String[_cols];
            for (int i = 0; i < _cols; i++)
                aliases[i] = meta.getColumnLabel(i + 1);
            _packer = new ResultPacker(null, aliases, cls);
        } else
            _packer = null;
    }

    @Override
    public boolean supportsRandomAccess() {
        try {
            return _res.supportsRandomAccess();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void open() {
    }

    @Override
    public Object getResultObject()
        throws SQLException {
        if (_cols == 1) {
            Object val = _res.getObject(1,
                JavaSQLTypes.JDBC_DEFAULT, null);
            return (_packer == null) ? val : _packer.pack(val);
        }

        Object[] vals = new Object[_cols];
        for (int i = 0; i < vals.length; i++)
            vals[i] = _res.getObject(i + 1,
                JavaSQLTypes.JDBC_DEFAULT, null);
        return (_packer == null) ? vals : _packer.pack(vals);
    }

    @Override
    public boolean next()
        throws SQLException {
        return _res.next();
    }

    @Override
    public boolean absolute(int pos)
        throws SQLException {
        return _res.absolute(pos);
    }

    @Override
    public int size()
        throws SQLException {
        if (_fetch.getLRSSize() == LRSSizes.SIZE_UNKNOWN
            || !supportsRandomAccess())
            return Integer.MAX_VALUE;
        return _res.size();
    }

    @Override
    public void reset() {
        throw new UnsupportedException();
    }

    @Override
    public void close() {
        _res.close();
    }

    @Override
    public void handleCheckedException(Exception e) {
        if (e instanceof SQLException)
            throw SQLExceptions.getStore((SQLException) e,
                _store.getDBDictionary());
        throw new StoreException(e);
    }
}

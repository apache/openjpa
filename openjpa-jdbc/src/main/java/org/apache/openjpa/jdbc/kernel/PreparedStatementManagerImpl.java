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
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowImpl;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.OptimisticException;

/**
 * Basic prepared statement manager implementation.
 *
 * @author Abe White
 */
public class PreparedStatementManagerImpl 
    implements PreparedStatementManager {

    private final static Localizer _loc = Localizer
        .forPackage(PreparedStatementManagerImpl.class);

    protected final JDBCStore _store;
    protected final Connection _conn;
    protected final DBDictionary _dict;

    // track exceptions
    protected final Collection _exceptions = new LinkedList();

    /**
     * Constructor. Supply connection.
     */
    public PreparedStatementManagerImpl(JDBCStore store, Connection conn) {
        _store = store;
        _dict = store.getDBDictionary();
        _conn = conn;
    }

    public Collection getExceptions() {
        return _exceptions;
    }

    public void flush(RowImpl row) {
        try {
        	if (!row.isFlushed())
                flushInternal(row);
        } catch (SQLException se) {
            _exceptions.add(SQLExceptions.getStore(se, _dict));
        } catch (OpenJPAException ke) {
            _exceptions.add(ke);
        }
    }

    /**
     * Flush the given row.
     */
    protected void flushInternal(RowImpl row) throws SQLException {
        // can't batch rows with auto-inc columns
        Column[] autoAssign = null;
        if (row.getAction() == Row.ACTION_INSERT)
            autoAssign = row.getTable().getAutoAssignedColumns();

        flushAndUpdate(row);

        // set auto assign values
        if (autoAssign != null && autoAssign.length > 0
            && row.getPrimaryKey() != null) {
            OpenJPAStateManager sm = row.getPrimaryKey();
            ClassMapping mapping = (ClassMapping) sm.getMetaData();
            Object val;
            for (int i = 0; i < autoAssign.length; i++) {
                val = _dict.getGeneratedKey(autoAssign[i], _conn);
                mapping.assertJoinable(autoAssign[i]).setAutoAssignedValue(sm,
                    _store, autoAssign[i], val);
            }
            sm.setObjectId(
                ApplicationIds.create(sm.getPersistenceCapable(), mapping));
        }
    }

    /**
     * Flush the given row immediately. 
     */
    protected void flushAndUpdate(RowImpl row)
        throws SQLException {
        // prepare statement
        String sql = row.getSQL(_dict);
        PreparedStatement stmnt = prepareStatement(sql);

        // setup parameters and execute statement
        if (stmnt != null)
            row.flush(stmnt, _dict, _store);
        try {
            int count = executeUpdate(stmnt, sql, row);
            if (count != 1) {
                Object failed = row.getFailedObject();
                if (failed != null)
                    _exceptions.add(new OptimisticException(failed));
                else if (row.getAction() == Row.ACTION_INSERT)
                    throw new SQLException(_loc.get(
                        "update-failed-no-failed-obj", String.valueOf(count),
                        sql).getMessage());
            }
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, row.getFailedObject(), _dict);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException se) {
                }
            }
        }
    }

    public void flush() {
    }
    
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of executing update.
     */
    protected int executeUpdate(PreparedStatement stmnt, String sql, 
        RowImpl row) throws SQLException {
        return stmnt.executeUpdate();
    }
        
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of preparing statement.
     */
    protected PreparedStatement prepareStatement(String sql)
        throws SQLException {
        return _conn.prepareStatement(sql);
    }
}

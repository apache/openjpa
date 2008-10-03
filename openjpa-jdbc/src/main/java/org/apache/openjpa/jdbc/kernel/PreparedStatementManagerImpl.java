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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
        flushAndUpdate(row);
    }

    /**
     * Flush the given row immediately. 
     */
    protected void flushAndUpdate(RowImpl row)
    throws SQLException {
        Column[] autoAssign = getAutoAssignColumns(row);
        String[] autoAssignColNames = getAutoAssignColNames(autoAssign, row);

        // prepare statement
        String sql = row.getSQL(_dict);
        PreparedStatement stmnt = prepareStatement(sql, autoAssignColNames);

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
            if (autoAssignColNames != null)
                populateAutoAssignCols(stmnt, autoAssign, autoAssignColNames, row);

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

    /** 
     * This method will only be called when there is auto assign columns.
     * If database supports getGeneratedKeys, the keys will be obtained
     * from the result set associated with the stmnt. If not, a separate 
     * sql to select the key will be issued from DBDictionary. 
     */
    protected List populateAutoAssignCols(PreparedStatement stmnt, 
        Column[] autoAssign, String[] autoAssignColNames, RowImpl row) 
        throws SQLException {
        List vals = null;
        if (_dict.supportsGetGeneratedKeys) {
            // set auto assign values to id col
            vals = getGeneratedKeys(stmnt, autoAssignColNames);
        }
        setObjectId(vals, autoAssign, autoAssignColNames, row);
        return vals;
    }

    protected void setObjectId(List vals, Column[] autoAssign, 
        String[] autoAssignColNames, RowImpl row) 
        throws SQLException{
        OpenJPAStateManager sm = row.getPrimaryKey();
        ClassMapping mapping = (ClassMapping) sm.getMetaData();
        Object val = null;
        for (int i = 0; i < autoAssign.length; i++) {
            if (_dict.supportsGetGeneratedKeys && vals != null && 
                vals.size() > 0)
                val = vals.get(i);
            else
                val = _dict.getGeneratedKey(autoAssign[i], _conn);
            mapping.assertJoinable(autoAssign[i]).setAutoAssignedValue(sm,
                _store, autoAssign[i], val);
        }
        sm.setObjectId(
            ApplicationIds.create(sm.getPersistenceCapable(), mapping));
    }

    /**
     * This method will only be called when the database supports
     * getGeneratedKeys.
     */
    protected List getGeneratedKeys(PreparedStatement stmnt, 
        String[] autoAssignColNames) 
        throws SQLException {
        ResultSet rs = stmnt.getGeneratedKeys();
        List vals = new ArrayList();
        while (rs.next()) {
            for (int i = 0; i < autoAssignColNames.length; i++)
                vals.add(rs.getObject(autoAssignColNames[i]));
        }
        rs.close();
        return vals;
    }

    protected Column[] getAutoAssignColumns(RowImpl row) {
        Column[] autoAssign = null;
        if (row.getAction() == Row.ACTION_INSERT)
            autoAssign = row.getTable().getAutoAssignedColumns();
        return autoAssign;
    }

    protected String[] getAutoAssignColNames(Column[] autoAssign, RowImpl row) {
        String[] autoAssignColNames = null;
        if (autoAssign != null && autoAssign.length > 0
            && row.getPrimaryKey() != null) {
            autoAssignColNames = new String[autoAssign.length];
            for (int i = 0; i < autoAssign.length; i++)
                autoAssignColNames[i] = autoAssign[i].getName();
        }
        return autoAssignColNames;
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
        return prepareStatement(sql, null);
    }    
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of preparing statement.
     */
    protected PreparedStatement prepareStatement(String sql, 
        String[] autoAssignColNames)
        throws SQLException {
        // pass in AutoAssignColumn names
        if (autoAssignColNames != null && _dict.supportsGetGeneratedKeys) 
            return _conn.prepareStatement(sql, autoAssignColNames);
        else
            return _conn.prepareStatement(sql);
    }
}

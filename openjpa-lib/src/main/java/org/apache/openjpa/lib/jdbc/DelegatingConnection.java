/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.jdbc;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.exception.NestableRuntimeException;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import serp.util.Numbers;

/**
 * Wrapper around an existing connection. Subclasses can override the
 * methods whose behavior they mean to change. The <code>equals</code> and
 * <code>hashCode</code> methods pass through to the base underlying data
 * store connection.
 *
 * @author Abe White
 */
public class DelegatingConnection implements Connection, Closeable {

    // jdbc 3 method keys
    private static final Object SET_HOLDABILITY = new Object();
    private static final Object GET_HOLDABILITY = new Object();
    private static final Object SET_SAVEPOINT_NONAME = new Object();
    private static final Object SET_SAVEPOINT = new Object();
    private static final Object ROLLBACK_SAVEPOINT = new Object();
    private static final Object RELEASE_SAVEPOINT = new Object();
    private static final Object CREATE_STATEMENT = new Object();
    private static final Object PREPARE_STATEMENT = new Object();
    private static final Object PREPARE_CALL = new Object();
    private static final Object PREPARE_WITH_KEYS = new Object();
    private static final Object PREPARE_WITH_INDEX = new Object();
    private static final Object PREPARE_WITH_NAMES = new Object();

    private static final Localizer _loc = Localizer.forPackage
        (DelegatingConnection.class);

    private static final Map _jdbc3;

    static {
        boolean jdbc3 = false;
        Method m = null;
        try {
            m = Connection.class.getMethod("setSavepoint",
                new Class[]{ String.class });
            jdbc3 = true;
        } catch (Throwable t) {
        }

        if (jdbc3) {
            _jdbc3 = new HashMap();
            _jdbc3.put(SET_SAVEPOINT, m);
        } else
            _jdbc3 = null;
    }

    private final Connection _conn;
    private final DelegatingConnection _del;

    public DelegatingConnection(Connection conn) {
        _conn = conn;
        if (conn instanceof DelegatingConnection)
            _del = (DelegatingConnection) _conn;
        else
            _del = null;
    }

    /**
     * Return the wrapped connection.
     */
    public Connection getDelegate() {
        return _conn;
    }

    /**
     * Return the base underlying data store connection.
     */
    public Connection getInnermostDelegate() {
        return (_del == null) ? _conn : _del.getInnermostDelegate();
    }

    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingConnection)
            other = ((DelegatingConnection) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("conn ").append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

    public Statement createStatement() throws SQLException {
        return createStatement(true);
    }

    /**
     * Create a statement, with the option of not wrapping it in a
     * {@link DelegatingStatement}, which is the default.
     */
    protected Statement createStatement(boolean wrap) throws SQLException {
        Statement stmnt;
        if (_del != null)
            stmnt = _del.createStatement(false);
        else
            stmnt = _conn.createStatement();
        if (wrap)
            stmnt = new DelegatingStatement(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String str) throws SQLException {
        return prepareStatement(str, true);
    }

    /**
     * Prepare a statement, with the option of not wrapping it in a
     * {@link DelegatingPreparedStatement}, which is the default.
     */
    protected PreparedStatement prepareStatement(String str, boolean wrap)
        throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(str, false);
        else
            stmnt = _conn.prepareStatement(str);
        if (wrap)
            stmnt = new DelegatingPreparedStatement(stmnt, this);
        return stmnt;
    }

    public CallableStatement prepareCall(String str) throws SQLException {
        return prepareCall(str, true);
    }

    /**
     * Prepare a call, with the option of not wrapping it in a
     * {@link DelegatingCallableStatement}, which is the default.
     */
    protected CallableStatement prepareCall(String str, boolean wrap)
        throws SQLException {
        CallableStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareCall(str, false);
        else
            stmnt = _conn.prepareCall(str);
        if (wrap)
            stmnt = new DelegatingCallableStatement(stmnt, this);
        return stmnt;
    }

    public String nativeSQL(String str) throws SQLException {
        return _conn.nativeSQL(str);
    }

    public void setAutoCommit(boolean bool) throws SQLException {
        _conn.setAutoCommit(bool);
    }

    public boolean getAutoCommit() throws SQLException {
        return _conn.getAutoCommit();
    }

    public void commit() throws SQLException {
        _conn.commit();
    }

    public void rollback() throws SQLException {
        _conn.rollback();
    }

    public void close() throws SQLException {
        _conn.close();
    }

    public boolean isClosed() throws SQLException {
        return _conn.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getMetaData(true);
    }

    /**
     * Return the metadata, with the option of not wrapping it in a
     * {@link DelegatingDatabaseMetaData}, which is the default.
     */
    protected DatabaseMetaData getMetaData(boolean wrap) throws SQLException {
        DatabaseMetaData meta;
        if (_del != null)
            meta = _del.getMetaData(false);
        else
            meta = _conn.getMetaData();
        if (wrap)
            meta = new DelegatingDatabaseMetaData(meta, this);
        return meta;
    }

    public void setReadOnly(boolean bool) throws SQLException {
        _conn.setReadOnly(bool);
    }

    public boolean isReadOnly() throws SQLException {
        return _conn.isReadOnly();
    }

    public void setCatalog(String str) throws SQLException {
        _conn.setCatalog(str);
    }

    public String getCatalog() throws SQLException {
        return _conn.getCatalog();
    }

    public void setTransactionIsolation(int i) throws SQLException {
        _conn.setTransactionIsolation(i);
    }

    public int getTransactionIsolation() throws SQLException {
        return _conn.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return _conn.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        _conn.clearWarnings();
    }

    public Statement createStatement(int type, int concur) throws SQLException {
        return createStatement(type, concur, true);
    }

    /**
     * Create a statement, with the option of not wrapping it in a
     * {@link DelegatingStatement}, which is the default.
     */
    protected Statement createStatement(int type, int concur, boolean wrap)
        throws SQLException {
        Statement stmnt;
        if (_del != null)
            stmnt = _del.createStatement(type, concur, false);
        else
            stmnt = _conn.createStatement(type, concur);
        if (wrap)
            stmnt = new DelegatingStatement(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String str, int type, int concur)
        throws SQLException {
        return prepareStatement(str, type, concur, true);
    }

    /**
     * Prepare a statement, with the option of not wrapping it in a
     * {@link DelegatingPreparedStatement}, which is the default.
     */
    protected PreparedStatement prepareStatement(String str, int type,
        int concur, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(str, type, concur, false);
        else
            stmnt = _conn.prepareStatement(str, type, concur);
        if (wrap)
            stmnt = new DelegatingPreparedStatement(stmnt, this);
        return stmnt;
    }

    public CallableStatement prepareCall(String str, int type, int concur)
        throws SQLException {
        return prepareCall(str, type, concur, true);
    }

    /**
     * Prepare a call, with the option of not wrapping it in a
     * {@link DelegatingCallableStatement}, which is the default.
     */
    protected CallableStatement prepareCall(String str, int type, int concur,
        boolean wrap) throws SQLException {
        CallableStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareCall(str, type, concur, false);
        else
            stmnt = _conn.prepareCall(str, type, concur);
        if (wrap)
            stmnt = new DelegatingCallableStatement(stmnt, this);
        return stmnt;
    }

    public Map getTypeMap() throws SQLException {
        return _conn.getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        _conn.setTypeMap(map);
    }

    // JDBC 3.0 methods follow; these are required to be able to
    // compile against JDK 1.4; these methods will not work on
    // previous JVMs

    public void setHoldability(int holdability) throws SQLException {
        assertJDBC3();
        Method m = (Method) _jdbc3.get(SET_HOLDABILITY);
        if (m == null)
            m = createJDBC3Method(SET_HOLDABILITY, "setHoldability",
                new Class[]{ int.class });
        invokeJDBC3(m, new Object[]{ Numbers.valueOf(holdability) });
    }

    public int getHoldability() throws SQLException {
        assertJDBC3();
        Method m = (Method) _jdbc3.get(GET_HOLDABILITY);
        if (m == null)
            m = createJDBC3Method(GET_HOLDABILITY, "getHoldability", null);
        return ((Number) invokeJDBC3(m, null)).intValue();
    }

    public Savepoint setSavepoint() throws SQLException {
        assertJDBC3();
        Method m = (Method) _jdbc3.get(SET_SAVEPOINT_NONAME);
        if (m == null)
            m = createJDBC3Method(SET_SAVEPOINT_NONAME, "setSavepoint", null);
        return (Savepoint) invokeJDBC3(m, null);
    }

    public Savepoint setSavepoint(String savepoint) throws SQLException {
        assertJDBC3();
        Method m = (Method) _jdbc3.get(SET_SAVEPOINT);
        if (m == null)
            m = createJDBC3Method(SET_SAVEPOINT, "setSavepoint",
                new Class[]{ String.class });
        return (Savepoint) invokeJDBC3(m, new Object[]{ savepoint });
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        assertJDBC3();
        Method m = (Method) _jdbc3.get(ROLLBACK_SAVEPOINT);
        if (m == null)
            m = createJDBC3Method(ROLLBACK_SAVEPOINT, "rollback",
                new Class[]{ Savepoint.class });
        invokeJDBC3(m, new Object[]{ savepoint });
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        assertJDBC3();
        Method m = (Method) _jdbc3.get(RELEASE_SAVEPOINT);
        if (m == null)
            m = createJDBC3Method(RELEASE_SAVEPOINT, "releaseSavepoint",
                new Class[]{ Savepoint.class });
        invokeJDBC3(m, new Object[]{ savepoint });
    }

    public Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        assertJDBC3();
        return createStatement(resultSetType, resultSetConcurrency,
            resultSetHoldability, true);
    }

    protected Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability, boolean wrap)
        throws SQLException {
        Statement stmnt;
        if (_del != null)
            stmnt = _del.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability, false);
        else {
            Method m = (Method) _jdbc3.get(CREATE_STATEMENT);
            if (m == null)
                m = createJDBC3Method(CREATE_STATEMENT, "createStatement",
                    new Class[]{ int.class, int.class, int.class });
            stmnt = (Statement) invokeJDBC3(m, new Object[]{
                Numbers.valueOf(resultSetType),
                Numbers.valueOf(resultSetConcurrency),
                Numbers.valueOf(resultSetHoldability) });
        }
        if (wrap)
            stmnt = new DelegatingStatement(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql,
        int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        assertJDBC3();
        return prepareStatement(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        int resultSetType, int resultSetConcurrency, int resultSetHoldability,
        boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability, false);
        else {
            Method m = (Method) _jdbc3.get(PREPARE_STATEMENT);
            if (m == null)
                m = createJDBC3Method(PREPARE_STATEMENT, "prepareStatement",
                    new Class[]{ String.class, int.class, int.class,
                        int.class });
            stmnt = (PreparedStatement) invokeJDBC3(m, new Object[]{ sql,
                Numbers.valueOf(resultSetType),
                Numbers.valueOf(resultSetConcurrency),
                Numbers.valueOf(resultSetHoldability) });
        }
        if (wrap)
            stmnt = new DelegatingPreparedStatement(stmnt, this);
        return stmnt;
    }

    public CallableStatement prepareCall(String sql,
        int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        assertJDBC3();
        return prepareCall(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability, true);
    }

    protected CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability, boolean wrap)
        throws SQLException {
        CallableStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareCall(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability, false);
        else {
            Method m = (Method) _jdbc3.get(PREPARE_CALL);
            if (m == null)
                m = createJDBC3Method(PREPARE_CALL, "prepareCall",
                    new Class[]{ String.class, int.class, int.class,
                        int.class });
            stmnt = (CallableStatement) invokeJDBC3(m, new Object[]{ sql,
                Numbers.valueOf(resultSetType),
                Numbers.valueOf(resultSetConcurrency),
                Numbers.valueOf(resultSetHoldability) });
        }
        if (wrap)
            stmnt = new DelegatingCallableStatement(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
        assertJDBC3();
        return prepareStatement(sql, autoGeneratedKeys, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        int autoGeneratedKeys, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, autoGeneratedKeys);
        else {
            Method m = (Method) _jdbc3.get(PREPARE_WITH_KEYS);
            if (m == null)
                m = createJDBC3Method(PREPARE_WITH_KEYS, "prepareStatement",
                    new Class[]{ String.class, int.class });
            stmnt = (PreparedStatement) invokeJDBC3(m, new Object[]{ sql,
                Numbers.valueOf(autoGeneratedKeys) });
        }
        if (wrap)
            stmnt = new DelegatingPreparedStatement(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException {
        assertJDBC3();
        return prepareStatement(sql, columnIndexes, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        int[] columnIndexes, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, columnIndexes, wrap);
        else {
            Method m = (Method) _jdbc3.get(PREPARE_WITH_INDEX);
            if (m == null)
                m = createJDBC3Method(PREPARE_WITH_INDEX, "prepareStatement",
                    new Class[]{ String.class, int[].class });
            stmnt = (PreparedStatement) invokeJDBC3(m, new Object[]{ sql,
                columnIndexes });
        }
        if (wrap)
            stmnt = new DelegatingPreparedStatement(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
        assertJDBC3();
        return prepareStatement(sql, columnNames, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        String[] columnNames, boolean wrap) throws SQLException {
        assertJDBC3();
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, columnNames, wrap);
        else {
            Method m = (Method) _jdbc3.get(PREPARE_WITH_NAMES);
            if (m == null)
                m = createJDBC3Method(PREPARE_WITH_NAMES, "prepareStatement",
                    new Class[]{ String.class, String[].class });
            stmnt = (PreparedStatement) invokeJDBC3(m, new Object[]{ sql,
                columnNames });
        }
        if (wrap)
            stmnt = new DelegatingPreparedStatement(stmnt, this);
        return stmnt;
    }

    private static void assertJDBC3() {
        if (_jdbc3 == null)
            throw new UnsupportedOperationException(_loc.get("not-jdbc3"));
    }

    private Object invokeJDBC3(Method m, Object[] args) throws SQLException {
        try {
            return m.invoke(_conn, args);
        } catch (Throwable t) {
            if (t instanceof SQLException)
                throw(SQLException) t;
            throw new NestableRuntimeException(_loc.get("invoke-jdbc3"), t);
        }
    }

    private static Method createJDBC3Method(Object key, String name,
        Class[] args) {
        try {
            Method m = Connection.class.getMethod(name, args);
            _jdbc3.put(key, m);
            return m;
        } catch (Throwable t) {
            throw new NestableRuntimeException(_loc.get("error-jdbc3"), t);
        }
    }
}

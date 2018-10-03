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
package org.apache.openjpa.lib.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.apache.openjpa.lib.util.Closeable;

/**
 * {@link CallableStatement} that delegates to an internal statement.
 *
 * @author Abe White
 */
public class DelegatingCallableStatement
    implements CallableStatement, Closeable {

    private final CallableStatement _stmnt;
    private final DelegatingCallableStatement _del;
    private final Connection _conn;

    public DelegatingCallableStatement(CallableStatement stmnt,
        Connection conn) {
        _conn = conn;
        _stmnt = stmnt;
        if (_stmnt instanceof DelegatingCallableStatement)
            _del = (DelegatingCallableStatement) _stmnt;
        else
            _del = null;
    }

    protected ResultSet wrapResult(boolean wrap, ResultSet rs) {
        if (!wrap)
            return rs;

        // never wrap null
        if (rs == null)
            return null;

        return new DelegatingResultSet(rs, this);
    }

    /**
     * Return the wrapped statement.
     */
    public CallableStatement getDelegate() {
        return _stmnt;
    }

    /**
     * Return the base underlying data store statement.
     */
    public CallableStatement getInnermostDelegate() {
        return (_del == null) ? _stmnt : _del.getInnermostDelegate();
    }

    @Override
    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingCallableStatement)
            other = ((DelegatingCallableStatement) other).
                getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("callstmnt ").append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

    @Override
    public ResultSet executeQuery(String str) throws SQLException {
        return executeQuery(true);
    }

    /**
     * Execute the query, with the option of not wrapping it in a
     * {@link DelegatingResultSet}, which is the default.
     */
    protected ResultSet executeQuery(String sql, boolean wrap)
        throws SQLException {
        ResultSet rs;
        if (_del != null)
            rs = _del.executeQuery(sql, false);
        else
            rs = _stmnt.executeQuery(sql);

        return wrapResult(wrap, rs);
    }

    @Override
    public int executeUpdate(String str) throws SQLException {
        return _stmnt.executeUpdate(str);
    }

    @Override
    public boolean execute(String str) throws SQLException {
        return _stmnt.execute(str);
    }

    @Override
    public void close() throws SQLException {
        _stmnt.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return _stmnt.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int i) throws SQLException {
        _stmnt.setMaxFieldSize(i);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return _stmnt.getMaxRows();
    }

    @Override
    public void setMaxRows(int i) throws SQLException {
        _stmnt.setMaxRows(i);
    }

    @Override
    public void setEscapeProcessing(boolean bool) throws SQLException {
        _stmnt.setEscapeProcessing(bool);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return _stmnt.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int i) throws SQLException {
        _stmnt.setQueryTimeout(i);
    }

    @Override
    public void cancel() throws SQLException {
        _stmnt.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return _stmnt.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        _stmnt.clearWarnings();
    }

    @Override
    public void setCursorName(String str) throws SQLException {
        _stmnt.setCursorName(str);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return getResultSet(true);
    }

    /**
     * Get the last result set, with the option of not wrapping it in a
     * {@link DelegatingResultSet}, which is the default.
     */
    protected ResultSet getResultSet(boolean wrap) throws SQLException {
        ResultSet rs;
        if (_del != null)
            rs = _del.getResultSet(false);
        else
            rs = _stmnt.getResultSet();

        return wrapResult(wrap, rs);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return _stmnt.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return _stmnt.getMoreResults();
    }

    @Override
    public void setFetchDirection(int i) throws SQLException {
        _stmnt.setFetchDirection(i);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return _stmnt.getFetchDirection();
    }

    @Override
    public void setFetchSize(int i) throws SQLException {
        _stmnt.setFetchSize(i);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return _stmnt.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return _stmnt.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return _stmnt.getResultSetType();
    }

    @Override
    public void addBatch(String str) throws SQLException {
        _stmnt.addBatch(str);
    }

    @Override
    public void clearBatch() throws SQLException {
        _stmnt.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return _stmnt.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return _conn;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return executeQuery(true);
    }

    /**
     * Execute the query, with the option of not wrapping it in a
     * {@link DelegatingResultSet}, which is the default.
     */
    protected ResultSet executeQuery(boolean wrap) throws SQLException {
        ResultSet rs;
        if (_del != null)
            rs = _del.executeQuery(false);
        else
            rs = _stmnt.executeQuery();

        return wrapResult(wrap, rs);
    }

    @Override
    public int executeUpdate() throws SQLException {
        return _stmnt.executeUpdate();
    }

    @Override
    public void setNull(int i1, int i2) throws SQLException {
        _stmnt.setNull(i1, i2);
    }

    @Override
    public void setBoolean(int i, boolean b) throws SQLException {
        _stmnt.setBoolean(i, b);
    }

    @Override
    public void setByte(int i, byte b) throws SQLException {
        _stmnt.setByte(i, b);
    }

    @Override
    public void setShort(int i, short s) throws SQLException {
        _stmnt.setShort(i, s);
    }

    @Override
    public void setInt(int i1, int i2) throws SQLException {
        _stmnt.setInt(i1, i2);
    }

    @Override
    public void setLong(int i, long l) throws SQLException {
        _stmnt.setLong(i, l);
    }

    @Override
    public void setFloat(int i, float f) throws SQLException {
        _stmnt.setFloat(i, f);
    }

    @Override
    public void setDouble(int i, double d) throws SQLException {
        _stmnt.setDouble(i, d);
    }

    @Override
    public void setBigDecimal(int i, BigDecimal bd) throws SQLException {
        _stmnt.setBigDecimal(i, bd);
    }

    @Override
    public void setString(int i, String s) throws SQLException {
        _stmnt.setString(i, s);
    }

    @Override
    public void setBytes(int i, byte[] b) throws SQLException {
        _stmnt.setBytes(i, b);
    }

    @Override
    public void setDate(int i, Date d) throws SQLException {
        _stmnt.setDate(i, d);
    }

    @Override
    public void setTime(int i, Time t) throws SQLException {
        _stmnt.setTime(i, t);
    }

    @Override
    public void setTimestamp(int i, Timestamp t) throws SQLException {
        _stmnt.setTimestamp(i, t);
    }

    @Override
    public void setAsciiStream(int i1, InputStream is, int i2)
        throws SQLException {
        _stmnt.setAsciiStream(i1, is, i2);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void setUnicodeStream(int i1, InputStream is, int i2)
        throws SQLException {
        _stmnt.setUnicodeStream(i1, is, i2);
    }

    @Override
    public void setBinaryStream(int i1, InputStream is, int i2)
        throws SQLException {
        _stmnt.setBinaryStream(i1, is, i2);
    }

    @Override
    public void clearParameters() throws SQLException {
        _stmnt.clearParameters();
    }

    @Override
    public void setObject(int i1, Object o, int i2, int i3)
        throws SQLException {
        _stmnt.setObject(i1, o, i2, i3);
    }

    @Override
    public void setObject(int i1, Object o, int i2) throws SQLException {
        _stmnt.setObject(i1, o, i2);
    }

    @Override
    public void setObject(int i, Object o) throws SQLException {
        _stmnt.setObject(i, o);
    }

    @Override
    public boolean execute() throws SQLException {
        return _stmnt.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        _stmnt.addBatch();
    }

    @Override
    public void setCharacterStream(int i1, Reader r, int i2)
        throws SQLException {
        _stmnt.setCharacterStream(i1, r, i2);
    }

    @Override
    public void setRef(int i, Ref r) throws SQLException {
        _stmnt.setRef(i, r);
    }

    @Override
    public void setBlob(int i, Blob b) throws SQLException {
        _stmnt.setBlob(i, b);
    }

    @Override
    public void setClob(int i, Clob c) throws SQLException {
        _stmnt.setClob(i, c);
    }

    @Override
    public void setArray(int i, Array a) throws SQLException {
        _stmnt.setArray(i, a);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return _stmnt.getMetaData();
    }

    @Override
    public void setDate(int i, Date d, Calendar c) throws SQLException {
        _stmnt.setDate(i, d, c);
    }

    @Override
    public void setTime(int i, Time t, Calendar c) throws SQLException {
        _stmnt.setTime(i, t, c);
    }

    @Override
    public void setTimestamp(int i, Timestamp t, Calendar c)
        throws SQLException {
        _stmnt.setTimestamp(i, t, c);
    }

    @Override
    public void setNull(int i1, int i2, String s) throws SQLException {
        _stmnt.setNull(i1, i2, s);
    }

    // JDBC 3 methods follow.

    @Override
    public boolean getMoreResults(int i) throws SQLException {
        return _stmnt.getMoreResults(i);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return _stmnt.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String s, int i) throws SQLException {
        return _stmnt.executeUpdate(s, i);
    }

    @Override
    public int executeUpdate(String s, int[] ia) throws SQLException {
        return _stmnt.executeUpdate(s, ia);
    }

    @Override
    public int executeUpdate(String s, String[] sa) throws SQLException {
        return _stmnt.executeUpdate(s, sa);
    }

    @Override
    public boolean execute(String s, int i) throws SQLException {
        return _stmnt.execute(s, i);
    }

    @Override
    public boolean execute(String s, int[] ia) throws SQLException {
        return _stmnt.execute(s, ia);
    }

    @Override
    public boolean execute(String s, String[] sa) throws SQLException {
        return _stmnt.execute(s, sa);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return _stmnt.getResultSetHoldability();
    }

    @Override
    public void setURL(int i, URL url) throws SQLException {
        _stmnt.setURL(i, url);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return _stmnt.getParameterMetaData();
    }

    /////////////////////////////
    // CallableStatement methods
    /////////////////////////////

    @Override
    public void registerOutParameter(int i1, int i2) throws SQLException {
        _stmnt.registerOutParameter(i1, i2);
    }

    @Override
    public void registerOutParameter(int i1, int i2, int i3)
        throws SQLException {
        _stmnt.registerOutParameter(i1, i2, i3);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return _stmnt.wasNull();
    }

    @Override
    public String getString(int i) throws SQLException {
        return _stmnt.getString(i);
    }

    @Override
    public boolean getBoolean(int i) throws SQLException {
        return _stmnt.getBoolean(i);
    }

    @Override
    public byte getByte(int i) throws SQLException {
        return _stmnt.getByte(i);
    }

    @Override
    public short getShort(int i) throws SQLException {
        return _stmnt.getShort(i);
    }

    @Override
    public int getInt(int i) throws SQLException {
        return _stmnt.getInt(i);
    }

    @Override
    public long getLong(int i) throws SQLException {
        return _stmnt.getLong(i);
    }

    @Override
    public float getFloat(int i) throws SQLException {
        return _stmnt.getFloat(i);
    }

    @Override
    public double getDouble(int i) throws SQLException {
        return _stmnt.getDouble(i);
    }

    /**
     * @deprecated use <code>getBigDecimal(int parameterIndex)</code> or
     *             <code>getBigDecimal(String parameterName)</code>
     */
    @Deprecated
    @Override
    public BigDecimal getBigDecimal(int a, int b) throws SQLException {
        return _stmnt.getBigDecimal(a, b);
    }

    @Override
    public byte[] getBytes(int i) throws SQLException {
        return _stmnt.getBytes(i);
    }

    @Override
    public Date getDate(int i) throws SQLException {
        return _stmnt.getDate(i);
    }

    @Override
    public Time getTime(int i) throws SQLException {
        return _stmnt.getTime(i);
    }

    @Override
    public Timestamp getTimestamp(int i) throws SQLException {
        return _stmnt.getTimestamp(i);
    }

    @Override
    public Object getObject(int i) throws SQLException {
        return _stmnt.getObject(i);
    }

    @Override
    public BigDecimal getBigDecimal(int i) throws SQLException {
        return _stmnt.getBigDecimal(i);
    }

    @Override
    public Object getObject(int i, Map<String,Class<?>> m) throws SQLException {
        return _stmnt.getObject(i, m);
    }

    @Override
    public Ref getRef(int i) throws SQLException {
        return _stmnt.getRef(i);
    }

    @Override
    public Blob getBlob(int i) throws SQLException {
        return _stmnt.getBlob(i);
    }

    @Override
    public Clob getClob(int i) throws SQLException {
        return _stmnt.getClob(i);
    }

    @Override
    public Array getArray(int i) throws SQLException {
        return _stmnt.getArray(i);
    }

    @Override
    public Date getDate(int i, Calendar c) throws SQLException {
        return _stmnt.getDate(i, c);
    }

    @Override
    public Time getTime(int i, Calendar c) throws SQLException {
        return _stmnt.getTime(i, c);
    }

    @Override
    public Timestamp getTimestamp(int i, Calendar c) throws SQLException {
        return _stmnt.getTimestamp(i, c);
    }

    @Override
    public void registerOutParameter(int i1, int i2, String s)
        throws SQLException {
        _stmnt.registerOutParameter(i1, i2, s);
    }

    // JDBC 3 methods follow.

    @Override
    public void registerOutParameter(String s, int i) throws SQLException {
        _stmnt.registerOutParameter(s, i);
    }

    @Override
    public void registerOutParameter(String s, int i1, int i2)
        throws SQLException {
        _stmnt.registerOutParameter(s, i1, i2);
    }

    @Override
    public void registerOutParameter(String s1, int i, String s2)
        throws SQLException {
        _stmnt.registerOutParameter(s1, i, s2);
    }

    @Override
    public URL getURL(int i) throws SQLException {
        return _stmnt.getURL(i);
    }

    @Override
    public void setURL(String a, URL b) throws SQLException {
        _stmnt.setURL(a, b);
    }

    @Override
    public URL getURL(String a) throws SQLException {
        return _stmnt.getURL(a);
    }

    @Override
    public void setNull(String a, int b) throws SQLException {
        _stmnt.setNull(a, b);
    }

    @Override
    public void setBoolean(String a, boolean b) throws SQLException {
        _stmnt.setBoolean(a, b);
    }

    @Override
    public void setByte(String a, byte b) throws SQLException {
        _stmnt.setByte(a, b);
    }

    @Override
    public void setShort(String a, short b) throws SQLException {
        _stmnt.setShort(a, b);
    }

    @Override
    public void setInt(String a, int b) throws SQLException {
        _stmnt.setInt(a, b);
    }

    @Override
    public void setLong(String a, long b) throws SQLException {
        _stmnt.setLong(a, b);
    }

    @Override
    public void setFloat(String a, float b) throws SQLException {
        _stmnt.setFloat(a, b);
    }

    @Override
    public void setDouble(String a, double b) throws SQLException {
        _stmnt.setDouble(a, b);
    }

    @Override
    public void setBigDecimal(String a, BigDecimal b) throws SQLException {
        _stmnt.setBigDecimal(a, b);
    }

    @Override
    public void setString(String a, String b) throws SQLException {
        _stmnt.setString(a, b);
    }

    @Override
    public void setBytes(String a, byte[] b) throws SQLException {
        _stmnt.setBytes(a, b);
    }

    @Override
    public void setDate(String a, Date b) throws SQLException {
        _stmnt.setDate(a, b);
    }

    @Override
    public void setTime(String a, Time b) throws SQLException {
        _stmnt.setTime(a, b);
    }

    @Override
    public void setTimestamp(String a, Timestamp b) throws SQLException {
        _stmnt.setTimestamp(a, b);
    }

    @Override
    public void setAsciiStream(String a, InputStream b, int c)
        throws SQLException {
        _stmnt.setAsciiStream(a, b, c);
    }

    @Override
    public void setBinaryStream(String a, InputStream b, int c)
        throws SQLException {
        _stmnt.setBinaryStream(a, b, c);
    }

    @Override
    public void setObject(String a, Object b, int c, int d)
        throws SQLException {
        _stmnt.setObject(a, b, c, d);
    }

    @Override
    public void setObject(String a, Object b, int c) throws SQLException {
        _stmnt.setObject(a, b, c);
    }

    @Override
    public void setObject(String a, Object b) throws SQLException {
        _stmnt.setObject(a, b);
    }

    @Override
    public void setCharacterStream(String a, Reader b, int c)
        throws SQLException {
        _stmnt.setCharacterStream(a, b, c);
    }

    @Override
    public void setDate(String a, Date b, Calendar c) throws SQLException {
        _stmnt.setDate(a, b, c);
    }

    @Override
    public void setTime(String a, Time b, Calendar c) throws SQLException {
        _stmnt.setTime(a, b, c);
    }

    @Override
    public void setTimestamp(String a, Timestamp b, Calendar c)
        throws SQLException {
        _stmnt.setTimestamp(a, b, c);
    }

    @Override
    public void setNull(String a, int b, String c) throws SQLException {
        _stmnt.setNull(a, b, c);
    }

    @Override
    public String getString(String a) throws SQLException {
        return _stmnt.getString(a);
    }

    @Override
    public boolean getBoolean(String a) throws SQLException {
        return _stmnt.getBoolean(a);
    }

    @Override
    public byte getByte(String a) throws SQLException {
        return _stmnt.getByte(a);
    }

    @Override
    public short getShort(String a) throws SQLException {
        return _stmnt.getShort(a);
    }

    @Override
    public int getInt(String a) throws SQLException {
        return _stmnt.getInt(a);
    }

    @Override
    public long getLong(String a) throws SQLException {
        return _stmnt.getLong(a);
    }

    @Override
    public float getFloat(String a) throws SQLException {
        return _stmnt.getFloat(a);
    }

    @Override
    public double getDouble(String a) throws SQLException {
        return _stmnt.getDouble(a);
    }

    @Override
    public byte[] getBytes(String a) throws SQLException {
        return _stmnt.getBytes(a);
    }

    @Override
    public Date getDate(String a) throws SQLException {
        return _stmnt.getDate(a);
    }

    @Override
    public Time getTime(String a) throws SQLException {
        return _stmnt.getTime(a);
    }

    @Override
    public Timestamp getTimestamp(String a) throws SQLException {
        return _stmnt.getTimestamp(a);
    }

    @Override
    public Object getObject(String a) throws SQLException {
        return _stmnt.getObject(a);
    }

    @Override
    public BigDecimal getBigDecimal(String a) throws SQLException {
        return _stmnt.getBigDecimal(a);
    }

    @Override
    public Object getObject(String a, Map<String, Class<?>>b) throws
            SQLException {
        return _stmnt.getObject(a, b);
    }

    @Override
    public Ref getRef(String a) throws SQLException {
        return _stmnt.getRef(a);
    }

    @Override
    public Blob getBlob(String a) throws SQLException {
        return _stmnt.getBlob(a);
    }

    @Override
    public Clob getClob(String a) throws SQLException {
        return _stmnt.getClob(a);
    }

    @Override
    public Array getArray(String a) throws SQLException {
        return _stmnt.getArray(a);
    }

    @Override
    public Date getDate(String a, Calendar b) throws SQLException {
        return _stmnt.getDate(a, b);
    }

    @Override
    public Time getTime(String a, Calendar b) throws SQLException {
        return _stmnt.getTime(a, b);
    }

    @Override
    public Timestamp getTimestamp(String a, Calendar b) throws SQLException {
        return _stmnt.getTimestamp(a, b);
    }

    // JDBC 4 methods follow.

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(getDelegate().getClass());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return (T) getDelegate();
        else
            return null;
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return _stmnt.getCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return _stmnt.getCharacterStream(parameterName);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return _stmnt.getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return _stmnt.getNCharacterStream(parameterName);
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        return _stmnt.getNClob(parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        return _stmnt.getNClob(parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        return _stmnt.getNString(parameterIndex);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        return _stmnt.getNString(parameterName);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        return _stmnt.getRowId(parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        return _stmnt.getRowId(parameterName);
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return _stmnt.getSQLXML(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return _stmnt.getSQLXML(parameterName);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        _stmnt.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        _stmnt.setAsciiStream(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        _stmnt.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        _stmnt.setBinaryStream(parameterName, x);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        _stmnt.setBlob(parameterName, x);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        _stmnt.setBlob(parameterName, inputStream, length);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        _stmnt.setBlob(parameterName, inputStream);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        _stmnt.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        _stmnt.setCharacterStream(parameterName, reader);
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        _stmnt.setClob(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        _stmnt.setClob(parameterName, reader, length);
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
        _stmnt.setClob(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        _stmnt.setNCharacterStream(parameterName, value, length);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        _stmnt.setNCharacterStream(parameterName, value);
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        _stmnt.setNClob(parameterName, value);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        _stmnt.setNClob(parameterName, reader, length);
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        _stmnt.setNClob(parameterName, reader);
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        _stmnt.setNString(parameterName, value);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        _stmnt.setRowId(parameterName, x);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        _stmnt.setSQLXML(parameterName, xmlObject);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        _stmnt.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        _stmnt.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        _stmnt.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        _stmnt.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        _stmnt.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        _stmnt.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        _stmnt.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        _stmnt.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        _stmnt.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        _stmnt.setClob(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        _stmnt.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        _stmnt.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        _stmnt.setNClob(parameterIndex, value);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        _stmnt.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        _stmnt.setNClob(parameterIndex, reader);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        _stmnt.setNString(parameterIndex, value);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        _stmnt.setRowId(parameterIndex, x);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        _stmnt.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return _stmnt.isClosed();
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return _stmnt.isPoolable();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        _stmnt.setPoolable(poolable);
    }

    // Java 7 methods follow

    @Override
    public <T>T getObject(String columnLabel, Class<T> type) throws SQLException{
    	throw new UnsupportedOperationException();
    }

    @Override
    public <T>T getObject(int columnIndex, Class<T> type) throws SQLException{
    	throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException{
    	throw new UnsupportedOperationException();
    }

    @Override
    public void closeOnCompletion() throws SQLException{
    	throw new UnsupportedOperationException();
    }
}

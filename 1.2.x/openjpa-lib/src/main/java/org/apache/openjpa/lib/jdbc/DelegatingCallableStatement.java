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
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
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

    private ResultSet wrapResult(boolean wrap, ResultSet rs) {
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

    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingCallableStatement)
            other = ((DelegatingCallableStatement) other).
                getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("prepstmnt ").append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

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

    public int executeUpdate(String str) throws SQLException {
        return _stmnt.executeUpdate(str);
    }

    public boolean execute(String str) throws SQLException {
        return _stmnt.execute(str);
    }

    public void close() throws SQLException {
        _stmnt.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return _stmnt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int i) throws SQLException {
        _stmnt.setMaxFieldSize(i);
    }

    public int getMaxRows() throws SQLException {
        return _stmnt.getMaxRows();
    }

    public void setMaxRows(int i) throws SQLException {
        _stmnt.setMaxRows(i);
    }

    public void setEscapeProcessing(boolean bool) throws SQLException {
        _stmnt.setEscapeProcessing(bool);
    }

    public int getQueryTimeout() throws SQLException {
        return _stmnt.getQueryTimeout();
    }

    public void setQueryTimeout(int i) throws SQLException {
        _stmnt.setQueryTimeout(i);
    }

    public void cancel() throws SQLException {
        _stmnt.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return _stmnt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        _stmnt.clearWarnings();
    }

    public void setCursorName(String str) throws SQLException {
        _stmnt.setCursorName(str);
    }

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

    public int getUpdateCount() throws SQLException {
        return _stmnt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return _stmnt.getMoreResults();
    }

    public void setFetchDirection(int i) throws SQLException {
        _stmnt.setFetchDirection(i);
    }

    public int getFetchDirection() throws SQLException {
        return _stmnt.getFetchDirection();
    }

    public void setFetchSize(int i) throws SQLException {
        _stmnt.setFetchSize(i);
    }

    public int getFetchSize() throws SQLException {
        return _stmnt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return _stmnt.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return _stmnt.getResultSetType();
    }

    public void addBatch(String str) throws SQLException {
        _stmnt.addBatch(str);
    }

    public void clearBatch() throws SQLException {
        _stmnt.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        return _stmnt.executeBatch();
    }

    public Connection getConnection() throws SQLException {
        return _conn;
    }

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

    public int executeUpdate() throws SQLException {
        return _stmnt.executeUpdate();
    }

    public void setNull(int i1, int i2) throws SQLException {
        _stmnt.setNull(i1, i2);
    }

    public void setBoolean(int i, boolean b) throws SQLException {
        _stmnt.setBoolean(i, b);
    }

    public void setByte(int i, byte b) throws SQLException {
        _stmnt.setByte(i, b);
    }

    public void setShort(int i, short s) throws SQLException {
        _stmnt.setShort(i, s);
    }

    public void setInt(int i1, int i2) throws SQLException {
        _stmnt.setInt(i1, i2);
    }

    public void setLong(int i, long l) throws SQLException {
        _stmnt.setLong(i, l);
    }

    public void setFloat(int i, float f) throws SQLException {
        _stmnt.setFloat(i, f);
    }

    public void setDouble(int i, double d) throws SQLException {
        _stmnt.setDouble(i, d);
    }

    public void setBigDecimal(int i, BigDecimal bd) throws SQLException {
        _stmnt.setBigDecimal(i, bd);
    }

    public void setString(int i, String s) throws SQLException {
        _stmnt.setString(i, s);
    }

    public void setBytes(int i, byte[] b) throws SQLException {
        _stmnt.setBytes(i, b);
    }

    public void setDate(int i, Date d) throws SQLException {
        _stmnt.setDate(i, d);
    }

    public void setTime(int i, Time t) throws SQLException {
        _stmnt.setTime(i, t);
    }

    public void setTimestamp(int i, Timestamp t) throws SQLException {
        _stmnt.setTimestamp(i, t);
    }

    public void setAsciiStream(int i1, InputStream is, int i2)
        throws SQLException {
        _stmnt.setAsciiStream(i1, is, i2);
    }

    public void setUnicodeStream(int i1, InputStream is, int i2)
        throws SQLException {
        _stmnt.setUnicodeStream(i1, is, i2);
    }

    public void setBinaryStream(int i1, InputStream is, int i2)
        throws SQLException {
        _stmnt.setBinaryStream(i1, is, i2);
    }

    public void clearParameters() throws SQLException {
        _stmnt.clearParameters();
    }

    public void setObject(int i1, Object o, int i2, int i3)
        throws SQLException {
        _stmnt.setObject(i1, o, i2, i3);
    }

    public void setObject(int i1, Object o, int i2) throws SQLException {
        _stmnt.setObject(i1, o, i2);
    }

    public void setObject(int i, Object o) throws SQLException {
        _stmnt.setObject(i, o);
    }

    public boolean execute() throws SQLException {
        return _stmnt.execute();
    }

    public void addBatch() throws SQLException {
        _stmnt.addBatch();
    }

    public void setCharacterStream(int i1, Reader r, int i2)
        throws SQLException {
        _stmnt.setCharacterStream(i1, r, i2);
    }

    public void setRef(int i, Ref r) throws SQLException {
        _stmnt.setRef(i, r);
    }

    public void setBlob(int i, Blob b) throws SQLException {
        _stmnt.setBlob(i, b);
    }

    public void setClob(int i, Clob c) throws SQLException {
        _stmnt.setClob(i, c);
    }

    public void setArray(int i, Array a) throws SQLException {
        _stmnt.setArray(i, a);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return _stmnt.getMetaData();
    }

    public void setDate(int i, Date d, Calendar c) throws SQLException {
        _stmnt.setDate(i, d, c);
    }

    public void setTime(int i, Time t, Calendar c) throws SQLException {
        _stmnt.setTime(i, t, c);
    }

    public void setTimestamp(int i, Timestamp t, Calendar c)
        throws SQLException {
        _stmnt.setTimestamp(i, t, c);
    }

    public void setNull(int i1, int i2, String s) throws SQLException {
        _stmnt.setNull(i1, i2, s);
    }

    // JDBC 3.0 (unsupported) methods follow; these are required to be able
    // to compile against JDK 1.4

    public boolean getMoreResults(int i) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int executeUpdate(String s, int i) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int executeUpdate(String s, int[] ia) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int executeUpdate(String s, String[] sa) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean execute(String s, int i) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean execute(String s, int[] ia) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean execute(String s, String[] sa) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getResultSetHoldability() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setURL(int i, URL url) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /////////////////////////////
    // CallableStatement methods
    /////////////////////////////

    public void registerOutParameter(int i1, int i2) throws SQLException {
        _stmnt.registerOutParameter(i1, i2);
    }

    public void registerOutParameter(int i1, int i2, int i3)
        throws SQLException {
        _stmnt.registerOutParameter(i1, i2, i3);
    }

    public boolean wasNull() throws SQLException {
        return _stmnt.wasNull();
    }

    public String getString(int i) throws SQLException {
        return _stmnt.getString(i);
    }

    public boolean getBoolean(int i) throws SQLException {
        return _stmnt.getBoolean(i);
    }

    public byte getByte(int i) throws SQLException {
        return _stmnt.getByte(i);
    }

    public short getShort(int i) throws SQLException {
        return _stmnt.getShort(i);
    }

    public int getInt(int i) throws SQLException {
        return _stmnt.getInt(i);
    }

    public long getLong(int i) throws SQLException {
        return _stmnt.getLong(i);
    }

    public float getFloat(int i) throws SQLException {
        return _stmnt.getFloat(i);
    }

    public double getDouble(int i) throws SQLException {
        return _stmnt.getDouble(i);
    }

    public BigDecimal getBigDecimal(int a, int b) throws SQLException {
        return _stmnt.getBigDecimal(a, b);
    }

    public byte[] getBytes(int i) throws SQLException {
        return _stmnt.getBytes(i);
    }

    public Date getDate(int i) throws SQLException {
        return _stmnt.getDate(i);
    }

    public Time getTime(int i) throws SQLException {
        return _stmnt.getTime(i);
    }

    public Timestamp getTimestamp(int i) throws SQLException {
        return _stmnt.getTimestamp(i);
    }

    public Object getObject(int i) throws SQLException {
        return _stmnt.getObject(i);
    }

    public BigDecimal getBigDecimal(int i) throws SQLException {
        return _stmnt.getBigDecimal(i);
    }

    public Object getObject(int i, Map m) throws SQLException {
        return _stmnt.getObject(i, m);
    }

    public Ref getRef(int i) throws SQLException {
        return _stmnt.getRef(i);
    }

    public Blob getBlob(int i) throws SQLException {
        return _stmnt.getBlob(i);
    }

    public Clob getClob(int i) throws SQLException {
        return _stmnt.getClob(i);
    }

    public Array getArray(int i) throws SQLException {
        return _stmnt.getArray(i);
    }

    public Date getDate(int i, Calendar c) throws SQLException {
        return _stmnt.getDate(i, c);
    }

    public Time getTime(int i, Calendar c) throws SQLException {
        return _stmnt.getTime(i, c);
    }

    public Timestamp getTimestamp(int i, Calendar c) throws SQLException {
        return _stmnt.getTimestamp(i, c);
    }

    public void registerOutParameter(int i1, int i2, String s)
        throws SQLException {
        _stmnt.registerOutParameter(i1, i2, s);
    }

    // JDBC 3.0 (unsupported) methods follow; these are required to be able
    // to compile against JDK 1.4

    public void registerOutParameter(String s, int i) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void registerOutParameter(String s, int i1, int i2)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void registerOutParameter(String s1, int i, String s2)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public URL getURL(int i) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setURL(String a, URL b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public URL getURL(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setNull(String a, int b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setBoolean(String a, boolean b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setByte(String a, byte b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setShort(String a, short b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setInt(String a, int b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setLong(String a, long b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setFloat(String a, float b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setDouble(String a, double b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setBigDecimal(String a, BigDecimal b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setString(String a, String b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setBytes(String a, byte[] b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setDate(String a, Date b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setTime(String a, Time b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setTimestamp(String a, Timestamp b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setAsciiStream(String a, InputStream b, int c)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setBinaryStream(String a, InputStream b, int c)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setObject(String a, Object b, int c, int d)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setObject(String a, Object b, int c) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setObject(String a, Object b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setCharacterStream(String a, Reader b, int c)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setDate(String a, Date b, Calendar c) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setTime(String a, Time b, Calendar c) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setTimestamp(String a, Timestamp b, Calendar c)
        throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void setNull(String a, int b, String c) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getString(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean getBoolean(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public byte getByte(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public short getShort(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getInt(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public long getLong(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public float getFloat(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public double getDouble(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public byte[] getBytes(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Date getDate(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Time getTime(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Timestamp getTimestamp(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Object getObject(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public BigDecimal getBigDecimal(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Object getObject(String a, Map b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Ref getRef(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Blob getBlob(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Clob getClob(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Array getArray(String a) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Date getDate(String a, Calendar b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Time getTime(String a, Calendar b) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Timestamp getTimestamp(String a, Calendar b) throws SQLException {
        throw new UnsupportedOperationException();
    }
}

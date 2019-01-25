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
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.apache.openjpa.lib.util.Closeable;

/**
 * Wrapper around an existing result set. Subclasses can override the
 * methods whose behavior they mean to change. The <code>equals</code> and
 * <code>hashCode</code> methods pass through to the base underlying data
 * store statement.
 *
 * @author Marc Prud'hommeaux
 */
public class DelegatingResultSet implements ResultSet, Closeable {

    private final ResultSet _rs;
    private final DelegatingResultSet _del;
    private final Statement _stmnt;

    public DelegatingResultSet(ResultSet rs, Statement stmnt) {
        if (rs == null)
            throw new IllegalArgumentException();

        _stmnt = stmnt;
        _rs = rs;
        if (_rs instanceof DelegatingResultSet)
            _del = (DelegatingResultSet) _rs;
        else
            _del = null;
    }

    /**
     * Return the wrapped result set.
     */
    public ResultSet getDelegate() {
        return _rs;
    }

    /**
     * Return the inner-most wrapped delegate.
     */
    public ResultSet getInnermostDelegate() {
        return (_del == null) ? _rs : _del.getInnermostDelegate();
    }

    @Override
    public int hashCode() {
        return _rs.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingResultSet)
            other = ((DelegatingResultSet) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("resultset ").append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

    @Override
    public boolean next() throws SQLException {
        return _rs.next();
    }

    @Override
    public void close() throws SQLException {
        _rs.close();
    }

    @Override
    public boolean wasNull() throws SQLException {
        return _rs.wasNull();
    }

    @Override
    public String getString(int a) throws SQLException {
        return _rs.getString(a);
    }

    @Override
    public boolean getBoolean(int a) throws SQLException {
        return _rs.getBoolean(a);
    }

    @Override
    public byte getByte(int a) throws SQLException {
        return _rs.getByte(a);
    }

    @Override
    public short getShort(int a) throws SQLException {
        return _rs.getShort(a);
    }

    @Override
    public int getInt(int a) throws SQLException {
        return _rs.getInt(a);
    }

    @Override
    public long getLong(int a) throws SQLException {
        return _rs.getLong(a);
    }

    @Override
    public float getFloat(int a) throws SQLException {
        return _rs.getFloat(a);
    }

    @Override
    public double getDouble(int a) throws SQLException {
        return _rs.getDouble(a);
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int a, int b) throws SQLException {
        return _rs.getBigDecimal(a, b);
    }

    @Override
    public byte[] getBytes(int a) throws SQLException {
        return _rs.getBytes(a);
    }

    @Override
    public Date getDate(int a) throws SQLException {
        return _rs.getDate(a);
    }

    @Override
    public Time getTime(int a) throws SQLException {
        return _rs.getTime(a);
    }

    @Override
    public Timestamp getTimestamp(int a) throws SQLException {
        return _rs.getTimestamp(a);
    }

    @Override
    public InputStream getAsciiStream(int a) throws SQLException {
        return _rs.getAsciiStream(a);
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(int a) throws SQLException {
        return _rs.getUnicodeStream(a);
    }

    @Override
    public InputStream getBinaryStream(int a) throws SQLException {
        return _rs.getBinaryStream(a);
    }

    @Override
    public String getString(String a) throws SQLException {
        return _rs.getString(a);
    }

    @Override
    public boolean getBoolean(String a) throws SQLException {
        return _rs.getBoolean(a);
    }

    @Override
    public byte getByte(String a) throws SQLException {
        return _rs.getByte(a);
    }

    @Override
    public short getShort(String a) throws SQLException {
        return _rs.getShort(a);
    }

    @Override
    public int getInt(String a) throws SQLException {
        return _rs.getInt(a);
    }

    @Override
    public long getLong(String a) throws SQLException {
        return _rs.getLong(a);
    }

    @Override
    public float getFloat(String a) throws SQLException {
        return _rs.getFloat(a);
    }

    @Override
    public double getDouble(String a) throws SQLException {
        return _rs.getDouble(a);
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(String a, int b) throws SQLException {
        return _rs.getBigDecimal(a, b);
    }

    @Override
    public byte[] getBytes(String a) throws SQLException {
        return _rs.getBytes(a);
    }

    @Override
    public Date getDate(String a) throws SQLException {
        return _rs.getDate(a);
    }

    @Override
    public Time getTime(String a) throws SQLException {
        return _rs.getTime(a);
    }

    @Override
    public Timestamp getTimestamp(String a) throws SQLException {
        return _rs.getTimestamp(a);
    }

    @Override
    public InputStream getAsciiStream(String a) throws SQLException {
        return _rs.getAsciiStream(a);
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(String a) throws SQLException {
        return _rs.getUnicodeStream(a);
    }

    @Override
    public InputStream getBinaryStream(String a) throws SQLException {
        return _rs.getBinaryStream(a);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return _rs.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        _rs.clearWarnings();
    }

    @Override
    public String getCursorName() throws SQLException {
        return _rs.getCursorName();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return _rs.getMetaData();
    }

    @Override
    public Object getObject(int a) throws SQLException {
        return _rs.getObject(a);
    }

    @Override
    public Object getObject(String a) throws SQLException {
        return _rs.getObject(a);
    }

    @Override
    public int findColumn(String a) throws SQLException {
        return _rs.findColumn(a);
    }

    @Override
    public Reader getCharacterStream(int a) throws SQLException {
        return _rs.getCharacterStream(a);
    }

    @Override
    public Reader getCharacterStream(String a) throws SQLException {
        return _rs.getCharacterStream(a);
    }

    @Override
    public BigDecimal getBigDecimal(int a) throws SQLException {
        return _rs.getBigDecimal(a);
    }

    @Override
    public BigDecimal getBigDecimal(String a) throws SQLException {
        return _rs.getBigDecimal(a);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return _rs.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return _rs.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return _rs.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return _rs.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        _rs.beforeFirst();
    }

    @Override
    public void afterLast() throws SQLException {
        _rs.afterLast();
    }

    @Override
    public boolean first() throws SQLException {
        return _rs.first();
    }

    @Override
    public boolean last() throws SQLException {
        return _rs.last();
    }

    @Override
    public int getRow() throws SQLException {
        return _rs.getRow();
    }

    @Override
    public boolean absolute(int a) throws SQLException {
        return _rs.absolute(a);
    }

    @Override
    public boolean relative(int a) throws SQLException {
        return _rs.relative(a);
    }

    @Override
    public boolean previous() throws SQLException {
        return _rs.previous();
    }

    @Override
    public void setFetchDirection(int a) throws SQLException {
        _rs.setFetchDirection(a);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return _rs.getFetchDirection();
    }

    @Override
    public void setFetchSize(int a) throws SQLException {
        _rs.setFetchSize(a);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return _rs.getFetchSize();
    }

    @Override
    public int getType() throws SQLException {
        return _rs.getType();
    }

    @Override
    public int getConcurrency() throws SQLException {
        return _rs.getConcurrency();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return _rs.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return _rs.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return _rs.rowDeleted();
    }

    @Override
    public void updateNull(int a) throws SQLException {
        _rs.updateNull(a);
    }

    @Override
    public void updateBoolean(int a, boolean b) throws SQLException {
        _rs.updateBoolean(a, b);
    }

    @Override
    public void updateByte(int a, byte b) throws SQLException {
        _rs.updateByte(a, b);
    }

    @Override
    public void updateShort(int a, short b) throws SQLException {
        _rs.updateShort(a, b);
    }

    @Override
    public void updateInt(int a, int b) throws SQLException {
        _rs.updateInt(a, b);
    }

    @Override
    public void updateLong(int a, long b) throws SQLException {
        _rs.updateLong(a, b);
    }

    @Override
    public void updateFloat(int a, float b) throws SQLException {
        _rs.updateFloat(a, b);
    }

    @Override
    public void updateDouble(int a, double b) throws SQLException {
        _rs.updateDouble(a, b);
    }

    @Override
    public void updateBigDecimal(int a, BigDecimal b) throws SQLException {
        _rs.updateBigDecimal(a, b);
    }

    @Override
    public void updateString(int a, String b) throws SQLException {
        _rs.updateString(a, b);
    }

    @Override
    public void updateBytes(int a, byte[] b) throws SQLException {
        _rs.updateBytes(a, b);
    }

    @Override
    public void updateDate(int a, Date b) throws SQLException {
        _rs.updateDate(a, b);
    }

    @Override
    public void updateTime(int a, Time b) throws SQLException {
        _rs.updateTime(a, b);
    }

    @Override
    public void updateTimestamp(int a, Timestamp b) throws SQLException {
        _rs.updateTimestamp(a, b);
    }

    @Override
    public void updateAsciiStream(int a, InputStream in, int b)
        throws SQLException {
        _rs.updateAsciiStream(a, in, b);
    }

    @Override
    public void updateBinaryStream(int a, InputStream in, int b)
        throws SQLException {
        _rs.updateBinaryStream(a, in, b);
    }

    @Override
    public void updateBlob(int a, Blob blob) throws SQLException {
        _rs.updateBlob(a, blob);
    }

    @Override
    public void updateCharacterStream(int a, Reader reader, int b)
        throws SQLException {
        _rs.updateCharacterStream(a, reader, b);
    }

    @Override
    public void updateClob(int a, Clob clob) throws SQLException {
       _rs.updateClob(a, clob);
    }

    @Override
    public void updateObject(int a, Object ob, int b) throws SQLException {
        _rs.updateObject(a, ob, b);
    }

    @Override
    public void updateObject(int a, Object ob) throws SQLException {
        _rs.updateObject(a, ob);
    }

    @Override
    public void updateNull(String a) throws SQLException {
        _rs.updateNull(a);
    }

    @Override
    public void updateBoolean(String a, boolean b) throws SQLException {
        _rs.updateBoolean(a, b);
    }

    @Override
    public void updateByte(String a, byte b) throws SQLException {
        _rs.updateByte(a, b);
    }

    @Override
    public void updateShort(String a, short b) throws SQLException {
        _rs.updateShort(a, b);
    }

    @Override
    public void updateInt(String a, int b) throws SQLException {
        _rs.updateInt(a, b);
    }

    @Override
    public void updateLong(String a, long b) throws SQLException {
        _rs.updateLong(a, b);
    }

    @Override
    public void updateFloat(String a, float b) throws SQLException {
        _rs.updateFloat(a, b);
    }

    @Override
    public void updateDouble(String a, double b) throws SQLException {
        _rs.updateDouble(a, b);
    }

    @Override
    public void updateBigDecimal(String a, BigDecimal b) throws SQLException {
        _rs.updateBigDecimal(a, b);
    }

    @Override
    public void updateString(String a, String b) throws SQLException {
        _rs.updateString(a, b);
    }

    @Override
    public void updateBytes(String a, byte[] b) throws SQLException {
        _rs.updateBytes(a, b);
    }

    @Override
    public void updateDate(String a, Date b) throws SQLException {
        _rs.updateDate(a, b);
    }

    @Override
    public void updateTime(String a, Time b) throws SQLException {
        _rs.updateTime(a, b);
    }

    @Override
    public void updateTimestamp(String a, Timestamp b) throws SQLException {
        _rs.updateTimestamp(a, b);
    }

    @Override
    public void updateAsciiStream(String a, InputStream in, int b)
        throws SQLException {
        _rs.updateAsciiStream(a, in, b);
    }

    @Override
    public void updateBinaryStream(String a, InputStream in, int b)
        throws SQLException {
        _rs.updateBinaryStream(a, in, b);
    }

    @Override
    public void updateCharacterStream(String a, Reader reader, int b)
        throws SQLException {
        _rs.updateCharacterStream(a, reader, b);
    }

    @Override
    public void updateObject(String a, Object ob, int b) throws SQLException {
        _rs.updateObject(a, ob, b);
    }

    @Override
    public void updateObject(String a, Object b) throws SQLException {
        _rs.updateObject(a, b);
    }

    @Override
    public void insertRow() throws SQLException {
        _rs.insertRow();
    }

    @Override
    public void updateRow() throws SQLException {
        _rs.updateRow();
    }

    @Override
    public void deleteRow() throws SQLException {
        _rs.deleteRow();
    }

    @Override
    public void refreshRow() throws SQLException {
        _rs.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        _rs.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        _rs.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        _rs.moveToCurrentRow();
    }

    @Override
    public Statement getStatement() throws SQLException {
        return _stmnt;
    }

    @Override
    public Object getObject(int a, Map<String, Class<?>> b) throws
            SQLException {
        return _rs.getObject(a, b);
    }

    @Override
    public Ref getRef(int a) throws SQLException {
        return _rs.getRef(a);
    }

    @Override
    public Blob getBlob(int a) throws SQLException {
        return _rs.getBlob(a);
    }

    @Override
    public Clob getClob(int a) throws SQLException {
        return _rs.getClob(a);
    }

    @Override
    public Array getArray(int a) throws SQLException {
        return _rs.getArray(a);
    }

    @Override
    public Object getObject(String a, Map<String, Class<?>> b) throws
            SQLException {
        return _rs.getObject(a, b);
    }

    @Override
    public Ref getRef(String a) throws SQLException {
        return _rs.getRef(a);
    }

    @Override
    public Blob getBlob(String a) throws SQLException {
        return _rs.getBlob(a);
    }

    @Override
    public Clob getClob(String a) throws SQLException {
        return _rs.getClob(a);
    }

    @Override
    public Array getArray(String a) throws SQLException {
        return _rs.getArray(a);
    }

    @Override
    public Date getDate(int a, Calendar b) throws SQLException {
        return _rs.getDate(a, b);
    }

    @Override
    public Date getDate(String a, Calendar b) throws SQLException {
        return _rs.getDate(a, b);
    }

    @Override
    public Time getTime(int a, Calendar b) throws SQLException {
        return _rs.getTime(a, b);
    }

    @Override
    public Time getTime(String a, Calendar b) throws SQLException {
        return _rs.getTime(a, b);
    }

    @Override
    public Timestamp getTimestamp(int a, Calendar b) throws SQLException {
        return _rs.getTimestamp(a, b);
    }

    @Override
    public Timestamp getTimestamp(String a, Calendar b) throws SQLException {
        return _rs.getTimestamp(a, b);
    }

    // JDBC 3.0 methods follow.

    @Override
    public URL getURL(int column) throws SQLException {
        return _rs.getURL(column);
    }

    @Override
    public URL getURL(String columnName) throws SQLException {
        return _rs.getURL(columnName);
    }

    @Override
    public void updateRef(int column, Ref ref) throws SQLException {
        _rs.updateRef(column, ref);
    }

    @Override
    public void updateRef(String columnName, Ref ref) throws SQLException {
        _rs.updateRef(columnName, ref);
    }

    @Override
    public void updateBlob(String columnName, Blob blob) throws SQLException {
        _rs.updateBlob(columnName, blob);
    }

    @Override
    public void updateClob(String columnName, Clob clob) throws SQLException {
        _rs.updateClob(columnName, clob);
    }

    @Override
    public void updateArray(int column, Array array) throws SQLException {
        _rs.updateArray(column, array);
    }

    @Override
    public void updateArray(String columnName, Array array)
        throws SQLException {
        _rs.updateArray(columnName, array);
    }

    // JDBC 4.0 methods follow.

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
    public int getHoldability() throws SQLException {
        return _rs.getHoldability();
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return _rs.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return _rs.getNCharacterStream(columnLabel);
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return _rs.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return _rs.getNClob(columnLabel);
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return _rs.getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return _rs.getNString(columnLabel);
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return _rs.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return _rs.getRowId(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return _rs.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return _rs.getSQLXML(columnLabel);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return _rs.isClosed();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        _rs.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        _rs.updateAsciiStream(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        _rs.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        _rs.updateAsciiStream(columnLabel, x);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        _rs.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        _rs.updateBinaryStream(columnIndex, x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        _rs.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        _rs.updateBinaryStream(columnLabel, x);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        _rs.updateBlob(columnIndex, inputStream, length);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        _rs.updateBlob(columnIndex, inputStream);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        _rs.updateBlob(columnLabel, inputStream, length);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        _rs.updateBlob(columnLabel, inputStream);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        _rs.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        _rs.updateCharacterStream(columnIndex, x);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        _rs.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        _rs.updateCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        _rs.updateClob(columnIndex, reader, length);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        _rs.updateClob(columnIndex, reader);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        _rs.updateClob(columnLabel, reader, length);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        _rs.updateClob(columnLabel, reader);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        _rs.updateNCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        _rs.updateNCharacterStream(columnIndex, x);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        _rs.updateNCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        _rs.updateNCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        _rs.updateNClob(columnIndex, nClob);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        _rs.updateNClob(columnIndex, reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        _rs.updateNClob(columnIndex, reader);
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        _rs.updateNClob(columnLabel, nClob);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        _rs.updateNClob(columnLabel, reader, length);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        _rs.updateNClob(columnLabel, reader);
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        _rs.updateNString(columnIndex, nString);
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        _rs.updateNString(columnLabel, nString);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        _rs.updateRowId(columnIndex, x);
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        _rs.updateRowId(columnLabel, x);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        _rs.updateSQLXML(columnIndex, xmlObject);
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        _rs.updateSQLXML(columnLabel, xmlObject);
    }

    // Java 7 methods follow

    @Override
    public <T>T getObject(String columnLabel, Class<T> type) throws SQLException{
        return _rs.getObject(columnLabel, type);
    }

    @Override
    public <T>T getObject(int columnIndex, Class<T> type) throws SQLException{
        return _rs.getObject(columnIndex, type);
    }
}


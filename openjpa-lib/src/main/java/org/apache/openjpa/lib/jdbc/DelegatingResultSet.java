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

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import org.apache.openjpa.lib.util.*;
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
        return(_del == null) ? _rs : _del.getInnermostDelegate();
    }

    public int hashCode() {
        return _rs.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingResultSet)
            other = ((DelegatingResultSet) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("resultset ").append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

    public boolean next() throws SQLException {
        return _rs.next();
    }

    public void close() throws SQLException {
        _rs.close();
    }

    public boolean wasNull() throws SQLException {
        return _rs.wasNull();
    }

    public String getString(int a) throws SQLException {
        return _rs.getString(a);
    }

    public boolean getBoolean(int a) throws SQLException {
        return _rs.getBoolean(a);
    }

    public byte getByte(int a) throws SQLException {
        return _rs.getByte(a);
    }

    public short getShort(int a) throws SQLException {
        return _rs.getShort(a);
    }

    public int getInt(int a) throws SQLException {
        return _rs.getInt(a);
    }

    public long getLong(int a) throws SQLException {
        return _rs.getLong(a);
    }

    public float getFloat(int a) throws SQLException {
        return _rs.getFloat(a);
    }

    public double getDouble(int a) throws SQLException {
        return _rs.getDouble(a);
    }

    public BigDecimal getBigDecimal(int a, int b) throws SQLException {
        return _rs.getBigDecimal(a, b);
    }

    public byte[] getBytes(int a) throws SQLException {
        return _rs.getBytes(a);
    }

    public Date getDate(int a) throws SQLException {
        return _rs.getDate(a);
    }

    public Time getTime(int a) throws SQLException {
        return _rs.getTime(a);
    }

    public Timestamp getTimestamp(int a) throws SQLException {
        return _rs.getTimestamp(a);
    }

    public InputStream getAsciiStream(int a) throws SQLException {
        return _rs.getAsciiStream(a);
    }

    public InputStream getUnicodeStream(int a) throws SQLException {
        return _rs.getUnicodeStream(a);
    }

    public InputStream getBinaryStream(int a) throws SQLException {
        return _rs.getBinaryStream(a);
    }

    public String getString(String a) throws SQLException {
        return _rs.getString(a);
    }

    public boolean getBoolean(String a) throws SQLException {
        return _rs.getBoolean(a);
    }

    public byte getByte(String a) throws SQLException {
        return _rs.getByte(a);
    }

    public short getShort(String a) throws SQLException {
        return _rs.getShort(a);
    }

    public int getInt(String a) throws SQLException {
        return _rs.getInt(a);
    }

    public long getLong(String a) throws SQLException {
        return _rs.getLong(a);
    }

    public float getFloat(String a) throws SQLException {
        return _rs.getFloat(a);
    }

    public double getDouble(String a) throws SQLException {
        return _rs.getDouble(a);
    }

    public BigDecimal getBigDecimal(String a, int b) throws SQLException {
        return _rs.getBigDecimal(a, b);
    }

    public byte[] getBytes(String a) throws SQLException {
        return _rs.getBytes(a);
    }

    public Date getDate(String a) throws SQLException {
        return _rs.getDate(a);
    }

    public Time getTime(String a) throws SQLException {
        return _rs.getTime(a);
    }

    public Timestamp getTimestamp(String a) throws SQLException {
        return _rs.getTimestamp(a);
    }

    public InputStream getAsciiStream(String a) throws SQLException {
        return _rs.getAsciiStream(a);
    }

    public InputStream getUnicodeStream(String a) throws SQLException {
        return _rs.getUnicodeStream(a);
    }

    public InputStream getBinaryStream(String a) throws SQLException {
        return _rs.getBinaryStream(a);
    }

    public SQLWarning getWarnings() throws SQLException {
        return _rs.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        _rs.clearWarnings();
    }

    public String getCursorName() throws SQLException {
        return _rs.getCursorName();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return _rs.getMetaData();
    }

    public Object getObject(int a) throws SQLException {
        return _rs.getObject(a);
    }

    public Object getObject(String a) throws SQLException {
        return _rs.getObject(a);
    }

    public int findColumn(String a) throws SQLException {
        return _rs.findColumn(a);
    }

    public Reader getCharacterStream(int a) throws SQLException {
        return _rs.getCharacterStream(a);
    }

    public Reader getCharacterStream(String a) throws SQLException {
        return _rs.getCharacterStream(a);
    }

    public BigDecimal getBigDecimal(int a) throws SQLException {
        return _rs.getBigDecimal(a);
    }

    public BigDecimal getBigDecimal(String a) throws SQLException {
        return _rs.getBigDecimal(a);
    }

    public boolean isBeforeFirst() throws SQLException {
        return _rs.isBeforeFirst();
    }

    public boolean isAfterLast() throws SQLException {
        return _rs.isAfterLast();
    }

    public boolean isFirst() throws SQLException {
        return _rs.isFirst();
    }

    public boolean isLast() throws SQLException {
        return _rs.isLast();
    }

    public void beforeFirst() throws SQLException {
        _rs.beforeFirst();
    }

    public void afterLast() throws SQLException {
        _rs.afterLast();
    }

    public boolean first() throws SQLException {
        return _rs.first();
    }

    public boolean last() throws SQLException {
        return _rs.last();
    }

    public int getRow() throws SQLException {
        return _rs.getRow();
    }

    public boolean absolute(int a) throws SQLException {
        return _rs.absolute(a);
    }

    public boolean relative(int a) throws SQLException {
        return _rs.relative(a);
    }

    public boolean previous() throws SQLException {
        return _rs.previous();
    }

    public void setFetchDirection(int a) throws SQLException {
        _rs.setFetchDirection(a);
    }

    public int getFetchDirection() throws SQLException {
        return _rs.getFetchDirection();
    }

    public void setFetchSize(int a) throws SQLException {
        _rs.setFetchSize(a);
    }

    public int getFetchSize() throws SQLException {
        return _rs.getFetchSize();
    }

    public int getType() throws SQLException {
        return _rs.getType();
    }

    public int getConcurrency() throws SQLException {
        return _rs.getConcurrency();
    }

    public boolean rowUpdated() throws SQLException {
        return _rs.rowUpdated();
    }

    public boolean rowInserted() throws SQLException {
        return _rs.rowInserted();
    }

    public boolean rowDeleted() throws SQLException {
        return _rs.rowDeleted();
    }

    public void updateNull(int a) throws SQLException {
        _rs.updateNull(a);
    }

    public void updateBoolean(int a, boolean b) throws SQLException {
        _rs.updateBoolean(a, b);
    }

    public void updateByte(int a, byte b) throws SQLException {
        _rs.updateByte(a, b);
    }

    public void updateShort(int a, short b) throws SQLException {
        _rs.updateShort(a, b);
    }

    public void updateInt(int a, int b) throws SQLException {
        _rs.updateInt(a, b);
    }

    public void updateLong(int a, long b) throws SQLException {
        _rs.updateLong(a, b);
    }

    public void updateFloat(int a, float b) throws SQLException {
        _rs.updateFloat(a, b);
    }

    public void updateDouble(int a, double b) throws SQLException {
        _rs.updateDouble(a, b);
    }

    public void updateBigDecimal(int a, BigDecimal b) throws SQLException {
        _rs.updateBigDecimal(a, b);
    }

    public void updateString(int a, String b) throws SQLException {
        _rs.updateString(a, b);
    }

    public void updateBytes(int a, byte[] b) throws SQLException {
        _rs.updateBytes(a, b);
    }

    public void updateDate(int a, Date b) throws SQLException {
        _rs.updateDate(a, b);
    }

    public void updateTime(int a, Time b) throws SQLException {
        _rs.updateTime(a, b);
    }

    public void updateTimestamp(int a, Timestamp b) throws SQLException {
        _rs.updateTimestamp(a, b);
    }

    public void updateAsciiStream(int a, InputStream in, int b)
        throws SQLException {
        _rs.updateAsciiStream(a, in, b);
    }

    public void updateBinaryStream(int a, InputStream in, int b)
        throws SQLException {
        _rs.updateBinaryStream(a, in, b);
    }

    public void updateCharacterStream(int a, Reader reader, int b)
        throws SQLException {
        _rs.updateCharacterStream(a, reader, b);
    }

    public void updateObject(int a, Object ob, int b) throws SQLException {
        _rs.updateObject(a, ob, b);
    }

    public void updateObject(int a, Object ob) throws SQLException {
        _rs.updateObject(a, ob);
    }

    public void updateNull(String a) throws SQLException {
        _rs.updateNull(a);
    }

    public void updateBoolean(String a, boolean b) throws SQLException {
        _rs.updateBoolean(a, b);
    }

    public void updateByte(String a, byte b) throws SQLException {
        _rs.updateByte(a, b);
    }

    public void updateShort(String a, short b) throws SQLException {
        _rs.updateShort(a, b);
    }

    public void updateInt(String a, int b) throws SQLException {
        _rs.updateInt(a, b);
    }

    public void updateLong(String a, long b) throws SQLException {
        _rs.updateLong(a, b);
    }

    public void updateFloat(String a, float b) throws SQLException {
        _rs.updateFloat(a, b);
    }

    public void updateDouble(String a, double b) throws SQLException {
        _rs.updateDouble(a, b);
    }

    public void updateBigDecimal(String a, BigDecimal b) throws SQLException {
        _rs.updateBigDecimal(a, b);
    }

    public void updateString(String a, String b) throws SQLException {
        _rs.updateString(a, b);
    }

    public void updateBytes(String a, byte[] b) throws SQLException {
        _rs.updateBytes(a, b);
    }

    public void updateDate(String a, Date b) throws SQLException {
        _rs.updateDate(a, b);
    }

    public void updateTime(String a, Time b) throws SQLException {
        _rs.updateTime(a, b);
    }

    public void updateTimestamp(String a, Timestamp b) throws SQLException {
        _rs.updateTimestamp(a, b);
    }

    public void updateAsciiStream(String a, InputStream in, int b)
        throws SQLException {
        _rs.updateAsciiStream(a, in, b);
    }

    public void updateBinaryStream(String a, InputStream in, int b)
        throws SQLException {
        _rs.updateBinaryStream(a, in, b);
    }

    public void updateCharacterStream(String a, Reader reader, int b)
        throws SQLException {
        _rs.updateCharacterStream(a, reader, b);
    }

    public void updateObject(String a, Object ob, int b) throws SQLException {
        _rs.updateObject(a, ob, b);
    }

    public void updateObject(String a, Object b) throws SQLException {
        _rs.updateObject(a, b);
    }

    public void insertRow() throws SQLException {
        _rs.insertRow();
    }

    public void updateRow() throws SQLException {
        _rs.updateRow();
    }

    public void deleteRow() throws SQLException {
        _rs.deleteRow();
    }

    public void refreshRow() throws SQLException {
        _rs.refreshRow();
    }

    public void cancelRowUpdates() throws SQLException {
        _rs.cancelRowUpdates();
    }

    public void moveToInsertRow() throws SQLException {
        _rs.moveToInsertRow();
    }

    public void moveToCurrentRow() throws SQLException {
        _rs.moveToCurrentRow();
    }

    public Statement getStatement() throws SQLException {
        return _stmnt;
    }

    public Object getObject(int a, Map b) throws SQLException {
        return _rs.getObject(a, b);
    }

    public Ref getRef(int a) throws SQLException {
        return _rs.getRef(a);
    }

    public Blob getBlob(int a) throws SQLException {
        return _rs.getBlob(a);
    }

    public Clob getClob(int a) throws SQLException {
        return _rs.getClob(a);
    }

    public Array getArray(int a) throws SQLException {
        return _rs.getArray(a);
    }

    public Object getObject(String a, Map b) throws SQLException {
        return _rs.getObject(a, b);
    }

    public Ref getRef(String a) throws SQLException {
        return _rs.getRef(a);
    }

    public Blob getBlob(String a) throws SQLException {
        return _rs.getBlob(a);
    }

    public Clob getClob(String a) throws SQLException {
        return _rs.getClob(a);
    }

    public Array getArray(String a) throws SQLException {
        return _rs.getArray(a);
    }

    public Date getDate(int a, Calendar b) throws SQLException {
        return _rs.getDate(a, b);
    }

    public Date getDate(String a, Calendar b) throws SQLException {
        return _rs.getDate(a, b);
    }

    public Time getTime(int a, Calendar b) throws SQLException {
        return _rs.getTime(a, b);
    }

    public Time getTime(String a, Calendar b) throws SQLException {
        return _rs.getTime(a, b);
    }

    public Timestamp getTimestamp(int a, Calendar b) throws SQLException {
        return _rs.getTimestamp(a, b);
    }

    public Timestamp getTimestamp(String a, Calendar b) throws SQLException {
        return _rs.getTimestamp(a, b);
    }

    // JDBC 3.0 (unsupported) method follow; these are required to be able
    // to compile against JDK 1.4

    public URL getURL(int column) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public URL getURL(String columnName) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateRef(int column, Ref ref) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateRef(String columnName, Ref ref) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBlob(int column, Blob blob) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateBlob(String columnName, Blob blob) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateClob(int column, Clob clob) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateClob(String columnName, Clob clob) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateArray(int column, Array array) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void updateArray(String columnName, Array array) throws SQLException {
        throw new UnsupportedOperationException();
    }
}


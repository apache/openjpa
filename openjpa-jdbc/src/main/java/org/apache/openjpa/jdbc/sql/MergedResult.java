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
package org.apache.openjpa.jdbc.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.util.UnsupportedException;

/**
 * Result that merges multiple result delegates. Support exists for
 * maintaining ordering of the internally-held results, provided that each
 * of the individual results is itself ordered.
 *
 * @author Abe White
 */
public class MergedResult implements Result {

    private static final byte NEXT = 0;
    private static final byte CURRENT = 1;
    private static final byte DONE = 2;

    private final Result[] _res;
    private final byte[] _status;
    private final ResultComparator _comp;
    private final Object[] _order;
    private int _idx = 0;
    private boolean _pushedBack = false;

    /**
     * Constructor; supply delegates.
     */
    public MergedResult(Result[] res) {
        this(res, null);
    }

    /**
     * Constructor; supply delegates and comparator for ordering results.
     */
    public MergedResult(Result[] res, ResultComparator comp) {
        _res = res;
        _comp = comp;
        _order = (comp == null) ? null : new Object[res.length];
        _status = (comp == null) ? null : new byte[res.length];
    }

    @Override
    public Object getEager(FieldMapping key) {
        return _res[_idx].getEager(key);
    }

    @Override
    public void putEager(FieldMapping key, Object res) {
        _res[_idx].putEager(key, res);
    }

    @Override
    public Joins newJoins() {
        return _res[_idx].newJoins();
    }

    @Override
    public void close() {
        for (int i = 0; i < _res.length; i++)
            _res[i].close();
    }

    @Override
    public void setLocking(boolean locking) {
        _res[_idx].setLocking(locking);
    }

    @Override
    public boolean isLocking() {
        return _res[_idx].isLocking();
    }

    @Override
    public boolean supportsRandomAccess()
        throws SQLException {
        return false;
    }

    @Override
    public boolean absolute(int row)
        throws SQLException {
        throw new UnsupportedException();
    }

    @Override
    public boolean next()
        throws SQLException {
        if (_pushedBack) {
            _pushedBack = false;
            return true;
        }

        if (_comp == null) {
            while (!_res[_idx].next()) {
                if (_idx == _res.length - 1)
                    return false;
                _idx++;
            }
            return true;
        }

        // ordering is involved; extract order values from each result
        boolean hasValue = false;
        for (int i = 0; i < _status.length; i++) {
            switch (_status[i]) {
                case NEXT:
                    if (_res[i].next()) {
                        hasValue = true;
                        _status[i] = CURRENT;
                        _order[i] = _comp.getOrderingValue(_res[i], i);
                    } else
                        _status[i] = DONE;
                    break;
                case CURRENT:
                    hasValue = true;
                    break;
            }
        }

        // all results exhausted
        if (!hasValue)
            return false;

        // for all results with values, find the 'least' one according to
        // the comparator
        int least = -1;
        Object orderVal = null;
        for (int i = 0; i < _order.length; i++) {
            if (_status[i] != CURRENT)
                continue;
            if (least == -1 || _comp.compare(_order[i], orderVal) < 0) {
                least = i;
                orderVal = _order[i];
            }
        }

        // make the current result the one with the least value, and clear
        // the cached value for that result
        _idx = least;
        _order[least] = null;
        _status[least] = NEXT;
        return true;
    }

    @Override
    public void pushBack()
        throws SQLException {
        _pushedBack = true;
    }

    @Override
    public int size()
        throws SQLException {
        int size = 0;
        for (int i = 0; i < _res.length; i++)
            size += _res[i].size();
        return size;
    }

    @Override
    public boolean contains(Object obj)
        throws SQLException {
        return _res[_idx].contains(obj);
    }

    @Override
    public boolean containsAll(Object[] objs)
        throws SQLException {
        return _res[_idx].containsAll(objs);
    }

    @Override
    public boolean contains(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].contains(col, joins);
    }

    @Override
    public boolean containsAll(Column[] cols, Joins joins)
        throws SQLException {
        return _res[_idx].containsAll(cols, joins);
    }

    @Override
    public ClassMapping getBaseMapping() {
        return _res[_idx].getBaseMapping();
    }

    @Override
    public void setBaseMapping(ClassMapping mapping) {
        _res[_idx].setBaseMapping(mapping);
    }

    @Override
    public FieldMapping getMappedByFieldMapping() {
        return _res[_idx].getMappedByFieldMapping();
    }

    @Override
    public void setMappedByFieldMapping(FieldMapping fieldMapping) {
        _res[_idx].setMappedByFieldMapping(fieldMapping);
    }

    @Override
    public Object getMappedByValue() {
        return _res[_idx].getMappedByValue();
    }

    @Override
    public void setMappedByValue(Object mappedByValue) {
        _res[_idx].setMappedByValue(mappedByValue);
    }

    @Override
    public int indexOf() {
        return _res[_idx].indexOf();
    }

    @Override
    public Object load(ClassMapping mapping, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return _res[_idx].load(mapping, store, fetch);
    }

    @Override
    public Object load(ClassMapping mapping, JDBCStore store,
        JDBCFetchConfiguration fetch, Joins joins)
        throws SQLException {
        return _res[_idx].load(mapping, store, fetch, joins);
    }

    @Override
    public Array getArray(Object obj)
        throws SQLException {
        return _res[_idx].getArray(obj);
    }

    @Override
    public InputStream getAsciiStream(Object obj)
        throws SQLException {
        return _res[_idx].getAsciiStream(obj);
    }

    @Override
    public BigDecimal getBigDecimal(Object obj)
        throws SQLException {
        return _res[_idx].getBigDecimal(obj);
    }

    @Override
    public BigInteger getBigInteger(Object obj)
        throws SQLException {
        return _res[_idx].getBigInteger(obj);
    }

    @Override
    public InputStream getBinaryStream(Object obj)
        throws SQLException {
        return _res[_idx].getBinaryStream(obj);
    }

    @Override
    public InputStream getLOBStream(JDBCStore store, Object obj)
        throws SQLException {
        return _res[_idx].getLOBStream(store, obj);
    }

    @Override
    public Blob getBlob(Object obj)
        throws SQLException {
        return _res[_idx].getBlob(obj);
    }

    @Override
    public boolean getBoolean(Object obj)
        throws SQLException {
        return _res[_idx].getBoolean(obj);
    }

    @Override
    public byte getByte(Object obj)
        throws SQLException {
        return _res[_idx].getByte(obj);
    }

    @Override
    public byte[] getBytes(Object obj)
        throws SQLException {
        return _res[_idx].getBytes(obj);
    }

    @Override
    public Calendar getCalendar(Object obj)
        throws SQLException {
        return _res[_idx].getCalendar(obj);
    }

    @Override
    public LocalDate getLocalDate(Object obj) throws SQLException {
        return _res[_idx].getLocalDate(obj);
    }

    @Override
    public LocalTime getLocalTime(Object obj) throws SQLException {
        return _res[_idx].getLocalTime(obj);
    }

    @Override
    public LocalDateTime getLocalDateTime(Object obj) throws SQLException {
        return _res[_idx].getLocalDateTime(obj);
    }

    @Override
    public char getChar(Object obj)
        throws SQLException {
        return _res[_idx].getChar(obj);
    }

    @Override
    public Reader getCharacterStream(Object obj)
        throws SQLException {
        return _res[_idx].getCharacterStream(obj);
    }

    @Override
    public Clob getClob(Object obj)
        throws SQLException {
        return _res[_idx].getClob(obj);
    }

    @Override
    public Date getDate(Object obj)
        throws SQLException {
        return _res[_idx].getDate(obj);
    }

    @Override
    public java.sql.Date getDate(Object obj, Calendar cal)
        throws SQLException {
        return _res[_idx].getDate(obj, cal);
    }

    @Override
    public double getDouble(Object obj)
        throws SQLException {
        return _res[_idx].getDouble(obj);
    }

    @Override
    public float getFloat(Object obj)
        throws SQLException {
        return _res[_idx].getFloat(obj);
    }

    @Override
    public int getInt(Object obj)
        throws SQLException {
        return _res[_idx].getInt(obj);
    }

    @Override
    public Locale getLocale(Object obj)
        throws SQLException {
        return _res[_idx].getLocale(obj);
    }

    @Override
    public long getLong(Object obj)
        throws SQLException {
        return _res[_idx].getLong(obj);
    }

    @Override
    public Number getNumber(Object obj)
        throws SQLException {
        return _res[_idx].getNumber(obj);
    }

    @Override
    public Object getObject(Object obj, int metaType, Object arg)
        throws SQLException {
        return _res[_idx].getObject(obj, metaType, arg);
    }

    @Override
    public Object getSQLObject(Object obj, Map map)
        throws SQLException {
        return _res[_idx].getSQLObject(obj, map);
    }

    @Override
    public Ref getRef(Object obj, Map map)
        throws SQLException {
        return _res[_idx].getRef(obj, map);
    }

    @Override
    public short getShort(Object obj)
        throws SQLException {
        return _res[_idx].getShort(obj);
    }

    @Override
    public String getString(Object obj)
        throws SQLException {
        return _res[_idx].getString(obj);
    }

    @Override
    public Time getTime(Object obj, Calendar cal)
        throws SQLException {
        return _res[_idx].getTime(obj, cal);
    }

    @Override
    public Timestamp getTimestamp(Object obj, Calendar cal)
        throws SQLException {
        return _res[_idx].getTimestamp(obj, cal);
    }

    @Override
    public Array getArray(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getArray(col, joins);
    }

    @Override
    public InputStream getAsciiStream(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getAsciiStream(col, joins);
    }

    @Override
    public BigDecimal getBigDecimal(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getBigDecimal(col, joins);
    }

    @Override
    public BigInteger getBigInteger(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getBigInteger(col, joins);
    }

    @Override
    public InputStream getBinaryStream(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getBinaryStream(col, joins);
    }

    @Override
    public Blob getBlob(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getBlob(col, joins);
    }

    @Override
    public boolean getBoolean(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getBoolean(col, joins);
    }

    @Override
    public byte getByte(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getByte(col, joins);
    }

    @Override
    public byte[] getBytes(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getBytes(col, joins);
    }

    @Override
    public Calendar getCalendar(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getCalendar(col, joins);
    }

    @Override
    public char getChar(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getChar(col, joins);
    }

    @Override
    public Reader getCharacterStream(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getCharacterStream(col, joins);
    }

    @Override
    public Clob getClob(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getClob(col, joins);
    }

    @Override
    public Date getDate(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getDate(col, joins);
    }

    @Override
    public java.sql.Date getDate(Column col, Calendar cal, Joins joins)
        throws SQLException {
        return _res[_idx].getDate(col, cal, joins);
    }

    @Override
    public double getDouble(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getDouble(col, joins);
    }

    @Override
    public float getFloat(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getFloat(col, joins);
    }

    @Override
    public int getInt(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getInt(col, joins);
    }

    @Override
    public Locale getLocale(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getLocale(col, joins);
    }

    @Override
    public long getLong(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getLong(col, joins);
    }

    @Override
    public Number getNumber(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getNumber(col, joins);
    }

    @Override
    public Object getObject(Column col, Object arg, Joins joins)
        throws SQLException {
        return _res[_idx].getObject(col, arg, joins);
    }

    @Override
    public Object getSQLObject(Column col, Map map, Joins joins)
        throws SQLException {
        return _res[_idx].getSQLObject(col, map, joins);
    }

    @Override
    public Ref getRef(Column col, Map map, Joins joins)
        throws SQLException {
        return _res[_idx].getRef(col, map, joins);
    }

    @Override
    public short getShort(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getShort(col, joins);
    }

    @Override
    public String getString(Column col, Joins joins)
        throws SQLException {
        return _res[_idx].getString(col, joins);
    }

    @Override
    public Time getTime(Column col, Calendar cal, Joins joins)
        throws SQLException {
        return _res[_idx].getTime(col, cal, joins);
    }

    @Override
    public Timestamp getTimestamp(Column col, Calendar cal, Joins joins)
        throws SQLException {
        return _res[_idx].getTimestamp(col, cal, joins);
    }

    @Override
    public boolean wasNull()
        throws SQLException {
        return _res[_idx].wasNull();
    }

    @Override
    public void startDataRequest(Object mapping) {
        for (int i = 0; i < _res.length; i++)
            _res[i].startDataRequest(mapping);
    }

    @Override
    public void endDataRequest() {
        for (int i = 0; i < _res.length; i++)
            _res[i].endDataRequest();
    }

    /**
     * Comparator for ordering result rows.
     */
    public interface ResultComparator
        extends Comparator {

        /**
         * Return the ordering value of the current row of the given result.
         */
        Object getOrderingValue(Result res, int idx);
    }
}

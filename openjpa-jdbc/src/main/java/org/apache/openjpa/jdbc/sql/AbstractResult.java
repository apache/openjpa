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
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.JDBCStoreManager;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.UnsupportedException;

/**
 * A {@link Result} implementation designed to be subclassed easily by
 * implementations. All <code>get&lt;type&gt;</code> calls are delegated to
 * the {@link #getObjectInternal(Object,int,Object,Joins)} method, which
 * should be implemented by subclasses along with {@link #nextInternal},
 * {@link #containsInternal}, and {@link Result#size}.
 *  Most of the methods of this class will accept return values from
 * {@link #getObjectInternal(Object,int,Object,Joins)} that are not exactly
 * the right type. For example, any numeric type can be returned as any
 * {@link Number} type, and dates, locales, characters, and booleans can be
 * returned as strings.
 *
 * @author Abe White
 * @see ResultSetResult
 */
public abstract class AbstractResult
    implements Result {

    private static final Joins JOINS = new NoOpJoins();

    private Map _eager = null;
    private ClassMapping _base = null;
    private int _index = 0;
    private boolean _gotEager = false;
    private boolean _wasNull = false;
    private boolean _locking = false;
    private boolean _ignoreNext = false;
    private boolean _last = false;
    private FieldMapping _mappedByFieldMapping = null;
    private Object _mappedByValue = null;

    @Override
    public Object getEager(FieldMapping key) {
        Map map = getEagerMap(true);
        return (map == null) ? null : map.get(key);
    }

    @Override
    public void putEager(FieldMapping key, Object res) {
        Map map = getEagerMap(false);
        if (map == null) {
            map = new HashMap();
            setEagerMap(map);
        }
        map.put(key, res);
    }

    /**
     * Raw eager information. May be null.
     *
     * @param client whether the client is accessing eager information
     */
    protected Map getEagerMap(boolean client) {
        if (client)
            _gotEager = true;
        return _eager;
    }

    /**
     * Raw eager information.
     */
    protected void setEagerMap(Map eager) {
        _eager = eager;
    }

    /**
     * Closes all eager results.
     */
    @Override
    public void close() {
        closeEagerMap(_eager);
        _mappedByFieldMapping = null;
        _mappedByValue = null;
    }

    /**
     * Close all results in eager map.
     */
    protected void closeEagerMap(Map eager) {
        if (eager != null) {
            Object res;
            for (Iterator itr = eager.values().iterator(); itr.hasNext();) {
                res = itr.next();
                if (res != this && res instanceof Closeable)
                    try {
                        ((Closeable) res).close();
                    } catch (Exception e) {
                    }
            }
        }
    }

    /**
     * Returns false by default.
     */
    @Override
    public boolean supportsRandomAccess()
        throws SQLException {
        return false;
    }

    @Override
    public boolean absolute(int row)
        throws SQLException {
        _gotEager = false;
        return absoluteInternal(row);
    }

    /**
     * Throws an exception by default.
     */
    protected boolean absoluteInternal(int row)
        throws SQLException {
        throw new UnsupportedException();
    }

    @Override
    public boolean next()
        throws SQLException {
        _gotEager = false;
        if (_ignoreNext) {
            _ignoreNext = false;
            return _last;
        }
        _last = nextInternal();
        return _last;
    }

    /**
     * Advance this row.
     */
    protected abstract boolean nextInternal()
        throws SQLException;

    @Override
    public void pushBack()
        throws SQLException {
        _ignoreNext = true;
    }

    /**
     * Returns a no-op joins object by default.
     */
    @Override
    public Joins newJoins() {
        return JOINS;
    }

    @Override
    public boolean contains(Object obj)
        throws SQLException {
        return containsInternal(obj, null);
    }

    @Override
    public boolean containsAll(Object[] objs)
        throws SQLException {
        return containsAllInternal(objs, null);
    }

    @Override
    public boolean contains(Column col, Joins joins)
        throws SQLException {
        return containsInternal(col, joins);
    }

    @Override
    public boolean containsAll(Column[] cols, Joins joins)
        throws SQLException {
        return containsAllInternal(cols, joins);
    }

    /**
     * Return whether this result contains data for the given id or column.
     * The id or column has not beed passed through {@link #translate}.
     */
    protected abstract boolean containsInternal(Object obj, Joins joins)
        throws SQLException;

    /**
     * Return whether this result contains data for all the given ids or
     * columns. The ids or columns have not been passed through
     * {@link #translate}. Delegates to {@link #containsInternal} by default.
     */
    protected boolean containsAllInternal(Object[] objs, Joins joins)
        throws SQLException {
        for (int i = 0; i < objs.length; i++)
            if (!containsInternal(objs[i], joins))
                return false;
        return true;
    }

    @Override
    public ClassMapping getBaseMapping() {
        // if we've returned an eager result this call might be for that eager
        // result instead of our primary mapping, so return null
        return (_gotEager) ? null : _base;
    }

    @Override
    public void setBaseMapping(ClassMapping base) {
        _base = base;
    }

    @Override
    public FieldMapping getMappedByFieldMapping() {
        return _mappedByFieldMapping;
    }

    @Override
    public void setMappedByFieldMapping(FieldMapping fieldMapping) {
        _mappedByFieldMapping = fieldMapping;
    }

    @Override
    public Object getMappedByValue() {
        return _mappedByValue;
    }

    @Override
    public void setMappedByValue(Object mappedByValue) {
        _mappedByValue = mappedByValue;
    }

    @Override
    public int indexOf() {
        return _index;
    }

    public void setIndexOf(int idx) {
        _index = idx;
    }

    @Override
    public Object load(ClassMapping mapping, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return load(mapping, store, fetch, null);
    }

    @Override
    public Object load(ClassMapping mapping, JDBCStore store,
        JDBCFetchConfiguration fetch, Joins joins)
        throws SQLException {
        return ((JDBCStoreManager) store).load(mapping, fetch, null, this);
    }

    @Override
    public Array getArray(Object obj)
        throws SQLException {
        return getArrayInternal(translate(obj, null), null);
    }

    @Override
    public Array getArray(Column col, Joins joins)
        throws SQLException {
        return getArrayInternal(translate(col, joins), joins);
    }

    protected Array getArrayInternal(Object obj, Joins joins)
        throws SQLException {
        return (Array) checkNull(getObjectInternal(obj,
            JavaSQLTypes.SQL_ARRAY, null, joins));
    }

    @Override
    public InputStream getAsciiStream(Object obj)
        throws SQLException {
        return getAsciiStreamInternal(translate(obj, null), null);
    }

    @Override
    public InputStream getAsciiStream(Column col, Joins joins)
        throws SQLException {
        return getAsciiStreamInternal(translate(col, joins), joins);
    }

    protected InputStream getAsciiStreamInternal(Object obj, Joins joins)
        throws SQLException {
        return (InputStream) checkNull(getObjectInternal(obj,
            JavaSQLTypes.ASCII_STREAM, null, joins));
    }

    @Override
    public BigDecimal getBigDecimal(Object obj)
        throws SQLException {
        return getBigDecimalInternal(translate(obj, null), null);
    }

    @Override
    public BigDecimal getBigDecimal(Column col, Joins joins)
        throws SQLException {
        return getBigDecimalInternal(translate(col, joins), joins);
    }

    protected BigDecimal getBigDecimalInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj,
            JavaTypes.BIGDECIMAL, null, joins));
        if (val == null)
            return null;
        if (val instanceof BigDecimal)
            return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    @Override
    public BigInteger getBigInteger(Object obj)
        throws SQLException {
        return getBigIntegerInternal(translate(obj, null), null);
    }

    @Override
    public BigInteger getBigInteger(Column col, Joins joins)
        throws SQLException {
        return getBigIntegerInternal(translate(col, joins), joins);
    }

    protected BigInteger getBigIntegerInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj,
            JavaTypes.BIGINTEGER, null, joins));
        if (val == null)
            return null;
        if (val instanceof BigInteger)
            return (BigInteger) val;
        return new BigInteger(val.toString());
    }

    @Override
    public InputStream getBinaryStream(Object obj)
        throws SQLException {
        return getBinaryStreamInternal(translate(obj, null), null);
    }

    @Override
    public InputStream getBinaryStream(Column col, Joins joins)
        throws SQLException {
        return getBinaryStreamInternal(translate(col, joins), joins);
    }

    @Override
    public InputStream getLOBStream(JDBCStore store, Object obj)
        throws SQLException {
        return getLOBStreamInternal(store, translate(obj, null), null);
    }

    protected InputStream getBinaryStreamInternal(Object obj, Joins joins)
        throws SQLException {
        return (InputStream) checkNull(getObjectInternal(obj,
            JavaSQLTypes.BINARY_STREAM, null, joins));
    }

    protected InputStream getLOBStreamInternal(JDBCStore store, Object obj,
        Joins joins) throws SQLException {
        return (InputStream) checkNull(getStreamInternal(store, obj,
            JavaSQLTypes.BINARY_STREAM, null, joins));
    }

    @Override
    public Blob getBlob(Object obj)
        throws SQLException {
        return getBlobInternal(translate(obj, null), null);
    }

    @Override
    public Blob getBlob(Column col, Joins joins)
        throws SQLException {
        return getBlobInternal(translate(col, joins), joins);
    }

    protected Blob getBlobInternal(Object obj, Joins joins)
        throws SQLException {
        return (Blob) checkNull(getObjectInternal(obj, JavaSQLTypes.BLOB,
            null, joins));
    }

    @Override
    public boolean getBoolean(Object obj)
        throws SQLException {
        return getBooleanInternal(translate(obj, null), null);
    }

    @Override
    public boolean getBoolean(Column col, Joins joins)
        throws SQLException {
        return getBooleanInternal(translate(col, joins), joins);
    }

    protected boolean getBooleanInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj, JavaTypes.BOOLEAN,
            null, joins));
        if (val == null)
            return false;
        return Boolean.valueOf(val.toString()).booleanValue();
    }

    @Override
    public byte getByte(Object obj)
        throws SQLException {
        return getByteInternal(translate(obj, null), null);
    }

    @Override
    public byte getByte(Column col, Joins joins)
        throws SQLException {
        return getByteInternal(translate(col, joins), joins);
    }

    protected byte getByteInternal(Object obj, Joins joins)
        throws SQLException {
        Number val = (Number) checkNull(getObjectInternal(obj,
            JavaTypes.BYTE, null, joins));
        return (val == null) ? 0 : val.byteValue();
    }

    @Override
    public byte[] getBytes(Object obj)
        throws SQLException {
        return getBytesInternal(translate(obj, null), null);
    }

    @Override
    public byte[] getBytes(Column col, Joins joins)
        throws SQLException {
        return getBytesInternal(translate(col, joins), joins);
    }

    protected byte[] getBytesInternal(Object obj, Joins joins)
        throws SQLException {
        return (byte[]) checkNull(getObjectInternal(obj,
            JavaSQLTypes.BYTES, null, joins));
    }

    @Override
    public Calendar getCalendar(Object obj)
        throws SQLException {
        return getCalendarInternal(translate(obj, null), null);
    }

    @Override
    public Calendar getCalendar(Column col, Joins joins)
        throws SQLException {
        return getCalendarInternal(translate(col, joins), joins);
    }

    protected Calendar getCalendarInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj, JavaTypes.CALENDAR,
            null, joins));
        if (val == null)
            return null;
        if (val instanceof Calendar)
            return (Calendar) val;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(val.toString()));
        return cal;
    }

    @Override
    public char getChar(Object obj)
        throws SQLException {
        return getCharInternal(translate(obj, null), null);
    }

    @Override
    public char getChar(Column col, Joins joins)
        throws SQLException {
        return getCharInternal(translate(col, joins), joins);
    }

    protected char getCharInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj, JavaTypes.CHAR,
            null, joins));
        if (val == null)
            return 0;
        if (val instanceof Character)
            return ((Character) val).charValue();

        String str = val.toString();
        return (str.length() == 0) ? 0 : str.charAt(0);
    }

    @Override
    public Reader getCharacterStream(Object obj)
        throws SQLException {
        return getCharacterStreamInternal(translate(obj, null), null);
    }

    @Override
    public Reader getCharacterStream(Column col, Joins joins)
        throws SQLException {
        return getCharacterStreamInternal(translate(col, joins), joins);
    }

    protected Reader getCharacterStreamInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj,
            JavaSQLTypes.CHAR_STREAM, null, joins));
        if (val == null)
            return null;
        if (val instanceof Reader)
            return (Reader) val;
        return new StringReader(val.toString());
    }

    @Override
    public Clob getClob(Object obj)
        throws SQLException {
        return getClobInternal(translate(obj, null), null);
    }

    @Override
    public Clob getClob(Column col, Joins joins)
        throws SQLException {
        return getClobInternal(translate(col, joins), joins);
    }

    protected Clob getClobInternal(Object obj, Joins joins)
        throws SQLException {
        return (Clob) checkNull(getObjectInternal(obj, JavaSQLTypes.CLOB,
            null, joins));
    }

    @Override
    public Date getDate(Object obj)
        throws SQLException {
        return getDateInternal(translate(obj, null), null);
    }

    @Override
    public Date getDate(Column col, Joins joins)
        throws SQLException {
        return getDateInternal(translate(col, joins), joins);
    }

    protected Date getDateInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj, JavaTypes.DATE,
            null, joins));
        if (val == null)
            return null;
        if (val instanceof Date)
            return (Date) val;
        return new Date(val.toString());
    }

    @Override
    public java.sql.Date getDate(Object obj, Calendar cal)
        throws SQLException {
        return getDateInternal(translate(obj, null), cal, null);
    }

    @Override
    public java.sql.Date getDate(Column col, Calendar cal, Joins joins)
        throws SQLException {
        return getDateInternal(translate(col, joins), cal, joins);
    }

    protected java.sql.Date getDateInternal(Object obj, Calendar cal,
        Joins joins)
        throws SQLException {
        return (java.sql.Date) checkNull(getObjectInternal(obj,
            JavaSQLTypes.SQL_DATE, cal, joins));
    }

    @Override
    public double getDouble(Object obj)
        throws SQLException {
        return getDoubleInternal(translate(obj, null), null);
    }

    @Override
    public double getDouble(Column col, Joins joins)
        throws SQLException {
        return getDoubleInternal(translate(col, joins), joins);
    }

    protected double getDoubleInternal(Object obj, Joins joins)
        throws SQLException {
        Number val = (Number) checkNull(getObjectInternal(obj,
            JavaTypes.DOUBLE, null, joins));
        return (val == null) ? 0 : val.doubleValue();
    }

    @Override
    public float getFloat(Object obj)
        throws SQLException {
        return getFloatInternal(translate(obj, null), null);
    }

    @Override
    public float getFloat(Column col, Joins joins)
        throws SQLException {
        return getFloatInternal(translate(col, joins), joins);
    }

    protected float getFloatInternal(Object obj, Joins joins)
        throws SQLException {
        Number val = (Number) checkNull(getObjectInternal(obj,
            JavaTypes.FLOAT, null, joins));
        return (val == null) ? 0 : val.floatValue();
    }

    @Override
    public int getInt(Object obj)
        throws SQLException {
        return getIntInternal(translate(obj, null), null);
    }

    @Override
    public int getInt(Column col, Joins joins)
        throws SQLException {
        return getIntInternal(translate(col, joins), joins);
    }

    protected int getIntInternal(Object obj, Joins joins)
        throws SQLException {
        Number val = (Number) checkNull(getObjectInternal(obj,
            JavaTypes.INT, null, joins));
        return (val == null) ? 0 : val.intValue();
    }

    @Override
    public Locale getLocale(Object obj)
        throws SQLException {
        return getLocaleInternal(translate(obj, null), null);
    }

    @Override
    public Locale getLocale(Column col, Joins joins)
        throws SQLException {
        return getLocaleInternal(translate(col, joins), joins);
    }

    protected Locale getLocaleInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj, JavaTypes.LOCALE,
            null, joins));
        if (val == null)
            return null;
        if (val instanceof Locale)
            return (Locale) val;
        String[] vals = StringUtil.split(val.toString(), "_", 0);
        if (vals.length < 2)
            throw new SQLException(val.toString());
        if (vals.length == 2)
            return new Locale(vals[0], vals[1]);
        return new Locale(vals[0], vals[1], vals[2]);
    }

    @Override
    public long getLong(Object obj)
        throws SQLException {
        return getLongInternal(translate(obj, null), null);
    }

    @Override
    public long getLong(Column col, Joins joins)
        throws SQLException {
        return getLongInternal(translate(col, joins), joins);
    }

    protected long getLongInternal(Object obj, Joins joins)
        throws SQLException {
        Number val = (Number) checkNull(getObjectInternal(obj,
            JavaTypes.LONG, null, joins));
        return (val == null) ? 0 : val.longValue();
    }

    @Override
    public Number getNumber(Object obj)
        throws SQLException {
        return getNumberInternal(translate(obj, null), null);
    }

    @Override
    public Number getNumber(Column col, Joins joins)
        throws SQLException {
        return getNumberInternal(translate(col, joins), joins);
    }

    protected Number getNumberInternal(Object obj, Joins joins)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj,
            JavaTypes.NUMBER, null, joins));
        if (val == null)
            return null;
        if (val instanceof Number)
            return (Number) val;
        return new BigDecimal(val.toString());
    }

    @Override
    public Object getObject(Object obj, int metaType, Object arg)
        throws SQLException {
        return getObjectInternal(obj, metaType, arg, null);
    }

    @Override
    public Object getObject(Column col, Object arg, Joins joins)
        throws SQLException {
        return getObjectInternal(col, col.getJavaType(),
            arg, joins);
    }

    /**
     * Return the value stored in the given id or column.
     */
    protected abstract Object getObjectInternal(Object obj, int metaType,
        Object arg, Joins joins)
        throws SQLException;

    protected abstract Object getStreamInternal(JDBCStore store, Object obj,
            int metaType, Object arg, Joins joins) throws SQLException;

    @Override
    public Object getSQLObject(Object obj, Map map)
        throws SQLException {
        return getSQLObjectInternal(translate(obj, null), map, null);
    }

    @Override
    public Object getSQLObject(Column col, Map map, Joins joins)
        throws SQLException {
        return getSQLObjectInternal(translate(col, joins), map, joins);
    }

    protected Object getSQLObjectInternal(Object obj, Map map, Joins joins)
        throws SQLException {
        return checkNull(getObjectInternal(obj, JavaSQLTypes.SQL_OBJECT,
            map, joins));
    }

    @Override
    public Ref getRef(Object obj, Map map)
        throws SQLException {
        return getRefInternal(translate(obj, null), map, null);
    }

    @Override
    public Ref getRef(Column col, Map map, Joins joins)
        throws SQLException {
        return getRefInternal(translate(col, joins), map, joins);
    }

    protected Ref getRefInternal(Object obj, Map map, Joins joins)
        throws SQLException {
        return (Ref) checkNull(getObjectInternal(obj, JavaSQLTypes.REF,
            map, joins));
    }

    @Override
    public short getShort(Object obj)
        throws SQLException {
        return getShortInternal(translate(obj, null), null);
    }

    @Override
    public short getShort(Column col, Joins joins)
        throws SQLException {
        return getShortInternal(translate(col, joins), joins);
    }

    protected short getShortInternal(Object obj, Joins joins)
        throws SQLException {
        Number val = (Number) checkNull(getObjectInternal(obj,
            JavaTypes.SHORT, null, joins));
        return (val == null) ? 0 : val.shortValue();
    }

    @Override
    public String getString(Object obj)
        throws SQLException {
        return getStringInternal(translate(obj, null), null,
            obj instanceof Column && ((Column) obj).getType() == Types.CLOB);
    }

    @Override
    public String getString(Column col, Joins joins)
        throws SQLException {
        return getStringInternal(translate(col, joins), joins,
            col.getType() == Types.CLOB);
    }

    protected String getStringInternal(Object obj, Joins joins, boolean isClobString)
        throws SQLException {
        Object val = checkNull(getObjectInternal(obj, JavaTypes.STRING,
            null, joins));
        return (val == null) ? null : val.toString();
    }

    @Override
    public Time getTime(Object obj, Calendar cal)
        throws SQLException {
        return getTimeInternal(translate(obj, null), cal, null);
    }

    @Override
    public Time getTime(Column col, Calendar cal, Joins joins)
        throws SQLException {
        return getTimeInternal(translate(col, joins), cal, joins);
    }

    protected Time getTimeInternal(Object obj, Calendar cal, Joins joins)
        throws SQLException {
        return (Time) checkNull(getObjectInternal(obj, JavaSQLTypes.TIME,
            cal, joins));
    }

    @Override
    public Timestamp getTimestamp(Object obj, Calendar cal)
        throws SQLException {
        return getTimestampInternal(translate(obj, null), cal, null);
    }

    @Override
    public Timestamp getTimestamp(Column col, Calendar cal, Joins joins)
        throws SQLException {
        return getTimestampInternal(translate(col, joins), cal, joins);
    }

    protected Timestamp getTimestampInternal(Object obj, Calendar cal,
        Joins joins)
        throws SQLException {
        return (Timestamp) checkNull(getObjectInternal(obj,
            JavaSQLTypes.TIMESTAMP, cal, joins));
    }

    @Override
    public boolean wasNull()
        throws SQLException {
        return _wasNull;
    }

    protected Object checkNull(Object val) {
        _wasNull = (val == null);
        return val;
    }

    @Override
    public void setLocking(boolean locking) {
        _locking = locking;
    }

    @Override
    public boolean isLocking() {
        return _locking;
    }

    @Override
    public void startDataRequest(Object mapping) {
    }

    @Override
    public void endDataRequest() {
    }

    /**
     * Translate the user-given id or column. This method is called before
     * delegating to any <code>get*Internal</code> methods with the exception of
     * <code>getObjectInternal</code>. Return the
     * original value by default.
     */
    protected Object translate(Object obj, Joins joins)
        throws SQLException {
        return obj;
    }

    /**
     * Do-nothing joins implementation.
     */
    private static class NoOpJoins
        implements Joins {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean isOuter() {
            return false;
        }

        @Override
        public Joins crossJoin(Table localTable, Table foreignTable) {
            return this;
        }

        @Override
        public Joins join(ForeignKey fk, boolean inverse, boolean toMany) {
            return this;
        }

        @Override
        public Joins outerJoin(ForeignKey fk, boolean inverse, boolean toMany) {
            return this;
        }

        @Override
        public Joins joinRelation(String name, ForeignKey fk,
            ClassMapping target, int subs, boolean inverse, boolean toMany) {
            return this;
        }

        @Override
        public Joins outerJoinRelation(String name, ForeignKey fk,
            ClassMapping target, int subs, boolean inverse, boolean toMany) {
            return this;
        }

        @Override
        public Joins setVariable(String var) {
            return this;
        }

        @Override
        public Joins setSubselect(String alias) {
            return this;
        }

        @Override
        public Joins setJoinContext(Context context) {
            return this;
        }

        public void appendTo(SQLBuffer buf) {
        }

        @Override
        public Joins setCorrelatedVariable(String var) {
            return this;
        }

        @Override
        public String getCorrelatedVariable() {
            return null;
        }

        @Override
        public void moveJoinsToParent() {
        }
    }
}

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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.AbstractResult;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UnsupportedException;

/**
 * Provides the data from query result mapped by a {@link QueryResultMapping}.
 *
 * @author Pinaki Poddar
 * @author Abe White
 */
class MappedQueryResultObjectProvider
    implements ResultObjectProvider {

    private final QueryResultMapping _map;
    private final JDBCStore _store;
    private final JDBCFetchConfiguration _fetch;
    private final MappingResult _mres;

    /**
     * Constructor.
     *
     * @param res the result data
     */
    public MappedQueryResultObjectProvider(QueryResultMapping map,
        JDBCStore store, JDBCFetchConfiguration fetch, Result res) {
        _map = map;
        _store = store;
        _fetch = (fetch == null) ? store.getFetchConfiguration() : fetch;
        _mres = new MappingResult(res);
    }

    @Override
    public boolean supportsRandomAccess() {
        try {
            return _mres.supportsRandomAccess();
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
        QueryResultMapping.PCResult[] pcs = _map.getPCResults();
        Object[] cols = _map.getColumnResults();

        // single object cases
        if (pcs.length == 0 && cols.length == 1)
            return _mres.getObject(cols[0], JavaSQLTypes.JDBC_DEFAULT, null);
        if (pcs.length == 1 && cols.length == 0)
            return _mres.load(pcs[0], _store, _fetch);

        // multiple objects
        Object[] ret = new Object[pcs.length + cols.length];
        for (int i = 0; i < pcs.length; i++)
            ret[i] = _mres.load(pcs[i], _store, _fetch);
        for (int i = 0; i < cols.length; i++)
            ret[pcs.length + i] = _mres.getObject(cols[i],
                JavaSQLTypes.JDBC_DEFAULT, null);
        return ret;
    }

    @Override
    public boolean next()
        throws SQLException {
        return _mres.next();
    }

    @Override
    public boolean absolute(int pos)
        throws SQLException {
        return _mres.absolute(pos);
    }

    @Override
    public int size()
        throws SQLException {
        if (_fetch.getLRSSize() == LRSSizes.SIZE_UNKNOWN
            || !supportsRandomAccess())
            return Integer.MAX_VALUE;
        return _mres.size();
    }

    @Override
    public void reset() {
        throw new UnsupportedException();
    }

    @Override
    public void close() {
        _mres.close();
    }

    @Override
    public void handleCheckedException(Exception e) {
        if (e instanceof SQLException)
            throw SQLExceptions.getStore((SQLException) e,
                _store.getDBDictionary());
        throw new StoreException(e);
    }

    /**
     * Result type that maps requests using a given
     * {@link QueryResultMapping.PCResult}.
     */
    private static class MappingResult
        extends AbstractResult {

        private final Result _res;
        private final Stack _requests = new Stack();
        private QueryResultMapping.PCResult _pc = null;

        /**
         * Supply delegate on construction.
         */
        public MappingResult(Result res) {
            _res = res;
        }

        /**
         * Load an instance of the given type. Should be used in place of
         * {@link Result#load}.
         */
        public Object load(QueryResultMapping.PCResult pc, JDBCStore store,
            JDBCFetchConfiguration fetch)
            throws SQLException {
            _pc = pc;
            try {
                return load(pc.getCandidateTypeMapping(), store, fetch);
            } finally {
                _pc = null;
            }
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
            if (_pc == null)
                return super.load(mapping, store, fetch, joins);

            // we go direct to the store manager so we can tell it not to load
            // anything additional
            return ((JDBCStoreManager) store).load(mapping, fetch,
                _pc.getExcludes(_requests), this);
        }

        @Override
        public Object getEager(FieldMapping key) {
            Object ret = _res.getEager(key);
            if (_pc == null || ret != null)
                return ret;
            return (_pc.hasEager(_requests, key)) ? this : null;
        }

        @Override
        public void putEager(FieldMapping key, Object res) {
            _res.putEager(key, res);
        }

        @Override
        public void close() {
            _res.close();
        }

        @Override
        public Joins newJoins() {
            return _res.newJoins();
        }

        @Override
        public boolean supportsRandomAccess()
            throws SQLException {
            return _res.supportsRandomAccess();
        }

        @Override
        public ClassMapping getBaseMapping() {
            return _res.getBaseMapping();
        }

        @Override
        public int size()
            throws SQLException {
            return _res.size();
        }

        @Override
        public void startDataRequest(Object mapping) {
            _requests.push(mapping);
        }

        @Override
        public void endDataRequest() {
            _requests.pop();
        }

        @Override
        public boolean wasNull()
            throws SQLException {
            return _res.wasNull();
        }

        @Override
        protected Object translate(Object obj, Joins joins) {
            return (_pc == null) ? obj : _pc.map(_requests, obj, joins);
        }

        @Override
        protected boolean absoluteInternal(int row)
            throws SQLException {
            return _res.absolute(row);
        }

        @Override
        protected boolean nextInternal()
            throws SQLException {
            return _res.next();
        }

        @Override
        protected boolean containsInternal(Object obj, Joins joins)
            throws SQLException {
            return _res.contains(translate(obj, joins));
        }

        @Override
        protected Array getArrayInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getArray((Column) obj, joins);
            return _res.getArray(obj);
        }

        @Override
        protected InputStream getAsciiStreamInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getAsciiStream((Column) obj, joins);
            return _res.getAsciiStream(obj);
        }

        @Override
        protected BigDecimal getBigDecimalInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getBigDecimal((Column) obj, joins);
            return _res.getBigDecimal(obj);
        }

        @Override
        protected Number getNumberInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getNumber((Column) obj, joins);
            return _res.getNumber(obj);
        }

        @Override
        protected BigInteger getBigIntegerInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getBigInteger((Column) obj, joins);
            return _res.getBigInteger(obj);
        }

        @Override
        protected InputStream getBinaryStreamInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getBinaryStream((Column) obj, joins);
            return _res.getBinaryStream(obj);
        }

        @Override
        protected Blob getBlobInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getBlob((Column) obj, joins);
            return _res.getBlob(obj);
        }

        @Override
        protected boolean getBooleanInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getBoolean((Column) obj, joins);
            return _res.getBoolean(obj);
        }

        @Override
        protected byte getByteInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getByte((Column) obj, joins);
            return _res.getByte(obj);
        }

        @Override
        protected byte[] getBytesInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getBytes((Column) obj, joins);
            return _res.getBytes(obj);
        }

        @Override
        protected Calendar getCalendarInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getCalendar((Column) obj, joins);
            return _res.getCalendar(obj);
        }

        @Override
        protected char getCharInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getChar((Column) obj, joins);
            return _res.getChar(obj);
        }

        @Override
        protected Reader getCharacterStreamInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getCharacterStream((Column) obj, joins);
            return _res.getCharacterStream(obj);
        }

        @Override
        protected Clob getClobInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getClob((Column) obj, joins);
            return _res.getClob(obj);
        }

        @Override
        protected Date getDateInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getDate((Column) obj, joins);
            return _res.getDate(obj);
        }

        @Override
        protected java.sql.Date getDateInternal(Object obj, Calendar cal,
            Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getDate((Column) obj, cal, joins);
            return _res.getDate(obj, cal);
        }

        @Override
        protected double getDoubleInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getDouble((Column) obj, joins);
            return _res.getDouble(obj);
        }

        @Override
        protected float getFloatInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getFloat((Column) obj, joins);
            return _res.getFloat(obj);
        }

        @Override
        protected int getIntInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getInt((Column) obj, joins);
            return _res.getInt(obj);
        }

        @Override
        protected Locale getLocaleInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getLocale((Column) obj, joins);
            return _res.getLocale(obj);
        }

        @Override
        protected long getLongInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getLong((Column) obj, joins);
            return _res.getLong(obj);
        }

        /*
         * OPENJPA-2651: Added to allow the column to be translated (from the
         * actual column name to the name provided in an @SqlResultSetMapping/@FieldResult.
         *
         * (non-Javadoc)
         * @see org.apache.openjpa.jdbc.sql.AbstractResult#getObject(org.apache.
         * openjpa.jdbc.schema.Column, java.lang.Object, org.apache.openjpa.jdbc.sql.Joins)
         */
        @Override
        public Object getObject(Column col, Object arg, Joins joins)
            throws SQLException {
            return getObjectInternal(translate(col, joins), col.getJavaType(),
                arg, joins);
        }

        @Override
        protected Object getObjectInternal(Object obj, int metaTypeCode,
            Object arg, Joins joins)
            throws SQLException {
            if (obj instanceof Column){
                Column col = (Column) obj;
                Object resultCol = _pc.getMapping(col.toString());
                if (resultCol != null) {
                    int javaType = col.getJavaType();
                    col = new Column(DBIdentifier.newColumn(resultCol.toString()), col.getTable());
                    col.setJavaType(javaType);
                }
                return _res.getObject(col, arg, joins);
            }
            return _res.getObject(obj, metaTypeCode, arg);
        }

        @Override
        protected Object getSQLObjectInternal(Object obj, Map map, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getSQLObject((Column) obj, map, joins);
            return _res.getSQLObject(obj, map);
        }

        @Override
        protected Object getStreamInternal(JDBCStore store, Object obj,
            int metaTypeCode, Object arg, Joins joins) throws SQLException {
            if (obj instanceof Column)
                return _res.getObject((Column) obj, arg, joins);
            return _res.getObject(obj, metaTypeCode, arg);
        }

        @Override
        protected Ref getRefInternal(Object obj, Map map, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getRef((Column) obj, map, joins);
            return _res.getRef(obj, map);
        }

        @Override
        protected short getShortInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getShort((Column) obj, joins);
            return _res.getShort(obj);
        }

        protected String getStringInternal(Object obj, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getString((Column) obj, joins);
            return _res.getString(obj);
        }

        @Override
        protected Time getTimeInternal(Object obj, Calendar cal, Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getTime((Column) obj, cal, joins);
            return _res.getTime(obj, cal);
        }

        @Override
        protected Timestamp getTimestampInternal(Object obj, Calendar cal,
            Joins joins)
            throws SQLException {
            if (obj instanceof Column)
                return _res.getTimestamp((Column) obj, cal, joins);
            return _res.getTimestamp(obj, cal);
        }
    }
}

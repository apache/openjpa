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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.Joinable;
import org.apache.openjpa.jdbc.meta.RelationId;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.InternalException;

/**
 * Basic {@link Row} implementation.
 *
 * @author Abe White
 */
public class RowImpl
    implements Row, Cloneable {

    public static final Object NULL = new Object();
    protected static final int VALID = 2 << 0;

    public static final int RAW = Integer.MIN_VALUE;

    protected byte flags = 0;
    private final Column[] _cols;
    private final int _action;
    private final Object[] _vals;
    private final int[] _types;

    private String _sql = null;
    private boolean _isFlushed = false;

    /**
     * Constructor.
     *
     * @param table the table the row is a part of
     * @param action the action on the row
     */
    public RowImpl(Table table, int action) {
        this(table.getColumns(), action);
    }

    protected RowImpl(Column[] cols, int action) {
        _cols = cols;
        _action = action;

        // we need room for values and types for all columns; if an update or
        // delete, then we need to double that for where column conditions
        int len = _cols.length;
        if (action != ACTION_INSERT)
            len *= 2;
        _vals = new Object[len];
        _types = new int[len];
    }

    @Override
    public Table getTable() {
        return _cols[0].getTable();
    }

    public Column[] getColumns() {
        return _cols;
    }

    @Override
    public int getAction() {
        return _action;
    }

    @Override
    public boolean isValid() {
        return (flags & VALID) != 0;
    }

    @Override
    public void setValid(boolean valid) {
        if (valid)
            flags |= VALID;
        else
            flags &= ~VALID;
    }

    /**
     * This implementation does not track primary keys.
     */
    @Override
    public OpenJPAStateManager getPrimaryKey() {
        return null;
    }

    /**
     * This implementation does not track failed objects.
     */
    @Override
    public Object getFailedObject() {
        return null;
    }

    /**
     * This implementation does not track failed objects.
     */
    @Override
    public void setFailedObject(Object failed) {
        throw new InternalException();
    }

    /**
     * Secondary rows cannot be dependent.
     */
    public boolean isDependent() {
        return false;
    }

    /**
     * Return the value set for update on the given column.
     */
    public Object getSet(Column col) {
        return _vals[getSetIndex(col)];
    }

    /**
     * Return the value set for where on the given column.
     */
    public Object getWhere(Column col) {
        return _vals[getWhereIndex(col)];
    }

    @Override
    public void setPrimaryKey(OpenJPAStateManager sm)
        throws SQLException {
        setPrimaryKey(null, sm);
    }

    @Override
    public void setPrimaryKey(ColumnIO io, OpenJPAStateManager sm)
        throws SQLException {
        flushPrimaryKey(sm, io, true);
    }

    @Override
    public void wherePrimaryKey(OpenJPAStateManager sm)
        throws SQLException {
        flushPrimaryKey(sm, null, false);
    }

    /**
     * Flush the primary key values.
     */
    private void flushPrimaryKey(OpenJPAStateManager sm, ColumnIO io,
        boolean set)
        throws SQLException {
        ClassMapping mapping = (ClassMapping) sm.getMetaData();
        while (mapping.getTable() != getTable())
            mapping = mapping.getPCSuperclassMapping();
        Column[] cols = mapping.getPrimaryKeyColumns();
        Object oid = mapping.useIdClassFromParent() ? sm.getObjectId() : null;
        flushJoinValues(sm, oid, cols, cols, io, set);
    }

    @Override
    public void setForeignKey(ForeignKey fk, OpenJPAStateManager sm)
        throws SQLException {
        setForeignKey(fk, null, sm);
    }

    @Override
    public void setForeignKey(ForeignKey fk, ColumnIO io,
        OpenJPAStateManager sm)
        throws SQLException {
        flushForeignKey(fk, io, sm, true);
    }

    @Override
    public void whereForeignKey(ForeignKey fk, OpenJPAStateManager sm)
        throws SQLException {
        flushForeignKey(fk, null, sm, false);
    }

    /**
     * Clear a circular foreign key.
     */
    public void clearForeignKey(ForeignKey fk)
        throws SQLException {
        _sql = null;
        Column[] cols = fk.getColumns();
        for (int i = 0; i < cols.length; i++)
            _vals[getSetIndex(cols[i])] = null;
    }

    /**
     * Flush the foreign key values.
     */
    private void flushForeignKey(ForeignKey fk, ColumnIO io,
        OpenJPAStateManager sm, boolean set)
        throws SQLException {
        flushJoinValues(sm, null, fk.getPrimaryKeyColumns(), fk.getColumns(),
            io, set);
        if (sm != null) {
            Column[] cols = fk.getConstantColumns();
            int len = fk.getColumns().length;
            Object obj;
            int type;
            for (int i = 0; i < cols.length; i++) {
                obj = fk.getConstant(cols[i]);
                type = cols[i].getJavaType();
                if (set && canSet(io, i + len, obj == null))
                    setObject(cols[i], obj, type, false);
                else if (!set)
                    whereObject(cols[i], obj, type);
            }
        }
    }

    /**
     * Flush the given instance value to the given columns. Note that
     * foreign keys may include columns also mapped by simple values. We
     * use a priority mechanism to ensure that we do not let the nulling
     * of a foreign key null columns also owned by simple values.
     *
     * @param to the instance being joined to
     * @param toCols the columns being joined to
     * @param fromCols the columns being joined from
     * @param io information about I/O capabilities in this context
     * @param set whether this should be flushed as an update or
     * as a where condition
     */
    private void flushJoinValues(OpenJPAStateManager to, Object oid, Column[] toCols,
        Column[] fromCols, ColumnIO io, boolean set)
        throws SQLException {
        if (to == null) {
            for (int i = 0; i < fromCols.length; i++) {
                if (set && canSet(io, i, true))
                    setNull(fromCols[i]);
                else if (!set)
                    whereNull(fromCols[i]);
            }
            return;
        }
        if (set && !canSetAny(io, fromCols.length, false))
            return;

        ClassMapping toMapping = (ClassMapping) to.getMetaData();
        Joinable join;
        Object val;
        for (int i = 0; i < toCols.length; i++) {
            // don't even translate join value if unsettable
            if (set) {
                if (_action == ACTION_INSERT && fromCols[i].isAutoAssigned())
                    continue;
                if (!canSet(io, i, false))
                    continue;
            }

            join = toMapping.assertJoinable(toCols[i]);
            if (oid != null)
                val = join.getJoinValue(oid, toCols[i], (JDBCStore) to.
                    getContext().getStoreManager().getInnermostDelegate());
            else
                val = join.getJoinValue(to, toCols[i], (JDBCStore) to.
                    getContext().getStoreManager().getInnermostDelegate());

            if (set && val == null) {
                if (canSet(io, i, true))
                    setNull(fromCols[i]);
            } else if (set && val instanceof Raw)
                setRaw(fromCols[i], val.toString());
            else if (set) {
                setObject(fromCols[i], val, toCols[i].getJavaType(), false);
                setJoinRefColumn(to, fromCols, toCols[i], val);
            } else if (val == null)
                whereNull(fromCols[i]);
            else if (val instanceof Raw)
                whereRaw(fromCols[i], val.toString());
            else
                whereObject(fromCols[i], val, toCols[i].getJavaType());
        }
    }

    private void setJoinRefColumn(OpenJPAStateManager inverseSm, Column ownerCols[], Column inverseCol,
                                   Object val) {
        OpenJPAStateManager ownerSm = getPrimaryKey();
        if (ownerSm != null) {
            ClassMetaData ownerMeta = ownerSm.getMetaData();
            // loop through all the fields in the owner entity
            for (FieldMetaData ownerFM : ownerMeta.getFields()) {
                // look for any single column in this field references the
                // same column as the foreign key target column
                Column cols[] = ((FieldMapping) ownerFM).getColumns();
                if (cols.length == 1            // only support attribute of non-compound foreign key
                        && cols != ownerCols    // not @Id field
                        && cols[0].getIdentifier().equals(ownerCols[0].getIdentifier())) {
                    // copy the foreign key value to the current field.
                    FieldMetaData inverseFM = inverseSm.getMetaData().getField(
                                    inverseCol.getIdentifier().getName());
                    if (inverseFM != null) {
                        int inverseValIndex = inverseFM.getIndex();
                        Class<?> inverseType = inverseSm.getMetaData().getField(inverseValIndex).getType();
                        int ownerIndex = ownerFM.getIndex();
                        Class<?> ownerType = ownerSm.getMetaData().getField(ownerIndex).getType();
                        if (inverseType == ownerType) {
                            Object inverseVal = inverseSm.fetch(inverseValIndex);
                            ownerSm.storeField(ownerIndex, inverseVal);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return true if any of the given column indexes are settable.
     */
    protected boolean canSetAny(ColumnIO io, int i, boolean nullValue) {
        if (io == null)
            return true;
        if (_action == ACTION_INSERT)
            return io.isAnyInsertable(i, nullValue);
        if (_action == ACTION_UPDATE)
            return io.isAnyUpdatable(i, nullValue);
        return true;
    }

    /**
     * Return true if the given column index is settable.
     */
    protected boolean canSet(ColumnIO io, int i, boolean nullValue) {
        if (io == null)
            return true;
        if (_action == ACTION_INSERT)
            return io.isInsertable(i, nullValue);
        if (_action == ACTION_UPDATE)
            return io.isUpdatable(i, nullValue);
        return true;
    }

    @Override
    public void setRelationId(Column col, OpenJPAStateManager sm,
        RelationId rel)
        throws SQLException {
        setObject(col, rel.toRelationDataStoreValue(sm, col),
            col.getJavaType(), false);
    }

    /**
     * Clear a circular relation id.
     */
    public void clearRelationId(Column col)
        throws SQLException {
        _sql = null;
        _vals[getSetIndex(col)] = null;
    }

    @Override
    public void setArray(Column col, Array val)
        throws SQLException {
        setObject(col, val, JavaTypes.ARRAY, false);
    }

    @Override
    public void setAsciiStream(Column col, InputStream val, int length)
        throws SQLException {
        setObject(col, (val == null) ? null : new Sized(val, length),
            JavaSQLTypes.ASCII_STREAM, false);
    }

    @Override
    public void setBigDecimal(Column col, BigDecimal val)
        throws SQLException {
        setObject(col, val, JavaTypes.BIGDECIMAL, false);
    }

    @Override
    public void setBigInteger(Column col, BigInteger val)
        throws SQLException {
        setObject(col, val, JavaTypes.BIGINTEGER, false);
    }

    @Override
    public void setBinaryStream(Column col, InputStream val, int length)
        throws SQLException {
        setObject(col, (val == null) ? null : new Sized(val, length),
            JavaSQLTypes.BINARY_STREAM, false);
    }

    @Override
    public void setBlob(Column col, Blob val)
        throws SQLException {
        setObject(col, val, JavaSQLTypes.BLOB, false);
    }

    @Override
    public void setBoolean(Column col, boolean val)
        throws SQLException {
        setObject(col, ((val) ? Boolean.TRUE : Boolean.FALSE),
            JavaTypes.BOOLEAN, false);
    }

    @Override
    public void setByte(Column col, byte val)
        throws SQLException {
        setObject(col, val, JavaTypes.BYTE, false);
    }

    @Override
    public void setBytes(Column col, byte[] val)
        throws SQLException {
        setObject(col, val, JavaSQLTypes.BYTES, false);
    }

    @Override
    public void setCalendar(Column col, Calendar val)
        throws SQLException {
        setObject(col, val, JavaTypes.CALENDAR, false);
    }

    @Override
    public void setChar(Column col, char val)
        throws SQLException {
        setObject(col, val, JavaTypes.CHAR, false);
    }

    @Override
    public void setCharacterStream(Column col, Reader val, int length)
        throws SQLException {
        setObject(col, (val == null) ? null : new Sized(val, length),
            JavaSQLTypes.CHAR_STREAM, false);
    }

    @Override
    public void setClob(Column col, Clob val)
        throws SQLException {
        setObject(col, val, JavaSQLTypes.CLOB, false);
    }

    @Override
    public void setDate(Column col, Date val)
        throws SQLException {
        setObject(col, val, JavaTypes.DATE, false);
    }

    @Override
    public void setDate(Column col, java.sql.Date val, Calendar cal)
        throws SQLException {
        Object obj;
        if (val == null || cal == null)
            obj = val;
        else
            obj = new Calendard(val, cal);
        setObject(col, obj, JavaSQLTypes.SQL_DATE, false);
    }

    @Override
    public void setDouble(Column col, double val)
        throws SQLException {
        setObject(col, val, JavaTypes.DOUBLE, false);
    }

    @Override
    public void setFloat(Column col, float val)
        throws SQLException {
        setObject(col, val, JavaTypes.FLOAT, false);
    }

    @Override
    public void setInt(Column col, int val)
        throws SQLException {
        setObject(col, val, JavaTypes.INT, false);
    }

    @Override
    public void setLong(Column col, long val)
        throws SQLException {
        setObject(col, val, JavaTypes.LONG, false);
    }

    @Override
    public void setLocale(Column col, Locale val)
        throws SQLException {
        setObject(col, val, JavaTypes.LOCALE, false);
    }

    @Override
    public void setNull(Column col)
        throws SQLException {
        setNull(col, false);
    }

    @Override
    public void setNull(Column col, boolean overrideDefault)
        throws SQLException {
        setObject(col, null, col.getJavaType(), overrideDefault);
    }

    @Override
    public void setNumber(Column col, Number val)
        throws SQLException {
        setObject(col, val, JavaTypes.NUMBER, false);
    }

    @Override
    public void setRaw(Column col, String val)
        throws SQLException {
        setObject(col, val, RAW, false);
    }

    @Override
    public void setShort(Column col, short val)
        throws SQLException {
        setObject(col, val, JavaTypes.SHORT, false);
    }

    @Override
    public void setString(Column col, String val)
        throws SQLException {
        setObject(col, val, JavaTypes.STRING, false);
    }

    @Override
    public void setTime(Column col, Time val, Calendar cal)
        throws SQLException {
        Object obj;
        if (val == null || cal == null)
            obj = val;
        else
            obj = new Calendard(val, cal);
        setObject(col, obj, JavaSQLTypes.TIME, false);
    }

    @Override
    public void setTimestamp(Column col, Timestamp val, Calendar cal)
        throws SQLException {
        Object obj;
        if (val == null || cal == null)
            obj = val;
        else
            obj = new Calendard(val, cal);
        setObject(col, obj, JavaSQLTypes.TIMESTAMP, false);
    }

    @Override
    public void setObject(Column col, Object val)
        throws SQLException {
        if (val instanceof Raw)
            setObject(col, val, RAW, false);
        else
            setObject(col, val, col.getJavaType(), false);
    }

    @Override
    public void whereArray(Column col, Array val)
        throws SQLException {
        whereObject(col, val, JavaSQLTypes.SQL_ARRAY);
    }

    @Override
    public void whereAsciiStream(Column col, InputStream val, int length)
        throws SQLException {
        whereObject(col, (val == null) ? null : new Sized(val, length),
            JavaSQLTypes.ASCII_STREAM);
    }

    @Override
    public void whereBigDecimal(Column col, BigDecimal val)
        throws SQLException {
        whereObject(col, val, JavaTypes.BIGDECIMAL);
    }

    @Override
    public void whereBigInteger(Column col, BigInteger val)
        throws SQLException {
        whereObject(col, val, JavaTypes.BIGINTEGER);
    }

    @Override
    public void whereBinaryStream(Column col, InputStream val, int length)
        throws SQLException {
        whereObject(col, (val == null) ? null : new Sized(val, length),
            JavaSQLTypes.BINARY_STREAM);
    }

    @Override
    public void whereBlob(Column col, Blob val)
        throws SQLException {
        whereObject(col, val, JavaSQLTypes.BLOB);
    }

    @Override
    public void whereBoolean(Column col, boolean val)
        throws SQLException {
        whereObject(col, ((val) ? Boolean.TRUE : Boolean.FALSE),
            JavaTypes.BOOLEAN);
    }

    @Override
    public void whereByte(Column col, byte val)
        throws SQLException {
        whereObject(col, val, JavaTypes.BYTE);
    }

    @Override
    public void whereBytes(Column col, byte[] val)
        throws SQLException {
        whereObject(col, val, JavaSQLTypes.BYTES);
    }

    @Override
    public void whereCalendar(Column col, Calendar val)
        throws SQLException {
        whereObject(col, val, JavaTypes.CALENDAR);
    }

    @Override
    public void whereChar(Column col, char val)
        throws SQLException {
        whereObject(col, val, JavaTypes.CHAR);
    }

    @Override
    public void whereCharacterStream(Column col, Reader val, int length)
        throws SQLException {
        whereObject(col, (val == null) ? null : new Sized(val, length),
            JavaSQLTypes.CHAR_STREAM);
    }

    @Override
    public void whereClob(Column col, Clob val)
        throws SQLException {
        whereObject(col, val, JavaSQLTypes.CLOB);
    }

    @Override
    public void whereDate(Column col, Date val)
        throws SQLException {
        whereObject(col, val, JavaTypes.DATE);
    }

    @Override
    public void whereDate(Column col, java.sql.Date val, Calendar cal)
        throws SQLException {
        Object obj;
        if (val == null || cal == null)
            obj = val;
        else
            obj = new Calendard(val, cal);
        whereObject(col, obj, JavaSQLTypes.SQL_DATE);
    }

    @Override
    public void whereDouble(Column col, double val)
        throws SQLException {
        whereObject(col, val, JavaTypes.DOUBLE);
    }

    @Override
    public void whereFloat(Column col, float val)
        throws SQLException {
        whereObject(col, val, JavaTypes.FLOAT);
    }

    @Override
    public void whereInt(Column col, int val)
        throws SQLException {
        whereObject(col, val, JavaTypes.INT);
    }

    @Override
    public void whereLong(Column col, long val)
        throws SQLException {
        whereObject(col, val, JavaTypes.LONG);
    }

    @Override
    public void whereLocale(Column col, Locale val)
        throws SQLException {
        whereObject(col, val, JavaTypes.LOCALE);
    }

    @Override
    public void whereNull(Column col)
        throws SQLException {
        whereObject(col, null, col.getJavaType());
    }

    @Override
    public void whereNumber(Column col, Number val)
        throws SQLException {
        whereObject(col, val, JavaTypes.NUMBER);
    }

    @Override
    public void whereRaw(Column col, String val)
        throws SQLException {
        whereObject(col, val, RAW);
    }

    @Override
    public void whereShort(Column col, short val)
        throws SQLException {
        whereObject(col, val, JavaTypes.SHORT);
    }

    @Override
    public void whereString(Column col, String val)
        throws SQLException {
        whereObject(col, val, JavaTypes.STRING);
    }

    @Override
    public void whereTime(Column col, Time val, Calendar cal)
        throws SQLException {
        Object obj;
        if (val == null || cal == null)
            obj = val;
        else
            obj = new Calendard(val, cal);
        whereObject(col, obj, JavaSQLTypes.TIME);
    }

    @Override
    public void whereTimestamp(Column col, Timestamp val, Calendar cal)
        throws SQLException {
        Object obj;
        if (val == null || cal == null)
            obj = val;
        else
            obj = new Calendard(val, cal);
        whereObject(col, obj, JavaSQLTypes.TIMESTAMP);
    }

    @Override
    public void whereObject(Column col, Object val)
        throws SQLException {
        if (val instanceof Raw)
            whereObject(col, val, RAW);
        else
            whereObject(col, val, col.getJavaType());
    }

    /**
     * All set column methods delegate to this one. Set the given object
     * unless this is an insert and the given column is auto-assigned.
     */
    protected void setObject(Column col, Object val, int metaType,
        boolean overrideDefault)
        throws SQLException {
        // never set auto increment columns and honor column defaults
        if (_action == ACTION_INSERT) {
            if (col.isAutoAssigned()) {
                // OPENJPA-349: validate because this can be the only column
                setValid(true);
                return;
            }
            if (!overrideDefault && val == null
                && col.getDefaultString() != null)
                return;
        }
        flush(col, val, metaType, true);
    }

    /**
     * All where column methods delegate to this one.
     */
    protected void whereObject(Column col, Object val, int metaType)
        throws SQLException {
        flush(col, val, metaType, false);
    }

    /**
     * Flush the given value as a set or where condition.
     */
    private void flush(Column col, Object val, int metaType, boolean set) {
        int idx = (set) ? getSetIndex(col) : getWhereIndex(col);
        _types[idx] = metaType;
        if (val == null)
            _vals[idx] = NULL;
        else
            _vals[idx] = val;
        if (set || _action == ACTION_DELETE)
            setValid(true);
    }

    /**
     * Return the SQL for the operation on this row.
     */
    public String getSQL(DBDictionary dict) {
        if (!isValid())
            return "";
        if (_sql == null)
            _sql = generateSQL(dict);
        return _sql;
    }

    /**
     * Generate the SQL for this row; the result of this method is cached.
     */
    protected String generateSQL(DBDictionary dict) {
        switch (getAction()) {
            case ACTION_UPDATE:
                return getUpdateSQL(dict);
            case ACTION_INSERT:
                return getInsertSQL(dict);
            default:
                return getDeleteSQL(dict);
        }
    }

    /**
     * Return the SQL for a prepared statement update on this row.
     */
    private String getUpdateSQL(DBDictionary dict) {
        StringBuilder buf = new StringBuilder();
        buf.append("UPDATE ").append(dict.getFullName(getTable(), false)).
            append(" SET ");

        boolean hasVal = false;
        for (int i = 0; i < _cols.length; i++) {
            if (_vals[i] == null)
                continue;

            if (hasVal)
                buf.append(", ");
            buf.append(dict.getColumnDBName(_cols[i]));
            if (_types[i] == RAW)
                buf.append(" = ").append(_vals[i]);
            else {
                buf.append(" = ");
                buf.append(dict.getMarkerForInsertUpdate(_cols[i], _vals[i]));
            }
            hasVal = true;
        }

        appendWhere(buf, dict);
        return buf.toString();
    }

    /**
     * Return the SQL for a prepared statement insert on this row.
     */
    private String getInsertSQL(DBDictionary dict) {
        StringBuilder buf = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        buf.append("INSERT INTO ").
            append(dict.getFullName(getTable(), false)).append(" (");

        boolean hasVal = false;
        for (int i = 0; i < _cols.length; i++) {
            if (_vals[i] == null)
                continue;

            if (hasVal) {
                buf.append(", ");
                vals.append(", ");
            }
            buf.append(dict.getColumnDBName(_cols[i]));
            if (_types[i] == RAW)
                vals.append(_vals[i]);
            else
                vals.append(dict.getMarkerForInsertUpdate(_cols[i], _vals[i]));
            hasVal = true;
        }

        buf.append(") VALUES (").append(vals.toString()).append(")");
        return buf.toString();
    }

    /**
     * Return the SQL for a prepared statement delete on this row.
     */
    private String getDeleteSQL(DBDictionary dict) {
        StringBuilder buf = new StringBuilder();
        buf.append("DELETE FROM ").
            append(dict.getFullName(getTable(), false));
        appendWhere(buf, dict);
        return buf.toString();
    }

    /**
     * Appends the where clause onto the given sql buffer.
     */
    private void appendWhere(StringBuilder buf, DBDictionary dict) {
        boolean hasWhere = false;
        for (int i = 0; i < _cols.length; i++) {
            if (_vals[getWhereIndex(_cols[i])] == null)
                continue;

            if (!hasWhere)
                buf.append(" WHERE ");
            else
                buf.append(" AND ");

            // Get platform specific version column name
            if (_cols[i].getVersionStrategy() != null)
               buf.append(dict.toDBName(dict.getVersionColumn(_cols[i], _cols[i]
                   .getTableIdentifier()))).append(" = ?");
            // sqlserver seems to have problems using null parameters in the
            // where clause
            else if (_vals[getWhereIndex(_cols[i])] == NULL)
                buf.append(dict.getColumnDBName(_cols[i])).append(" IS NULL");
            else if (_types[i] == RAW)
                buf.append(dict.getColumnDBName(_cols[i])).append(" = ").append(_vals[i]);
            else
                buf.append(dict.getColumnDBName(_cols[i])).append(" = ?");
            hasWhere = true;
        }
    }

    /**
     * The number of parameters that will be set for this row.
     */
    public int getParameterCount() {
        return _vals.length;
    }

    /**
     * Flush the row's values to the given prepared statement.
     */
    public void flush(PreparedStatement stmnt, DBDictionary dict,
        JDBCStore store)
        throws SQLException {
        flush(stmnt, 1, dict, store);
    }

    /**
     * Flush the row's values to the given prepared statement.
     */
    public void flush(PreparedStatement stmnt, int idx, DBDictionary dict,
        JDBCStore store)
        throws SQLException {

        // this simple method works because the SQL is always prepared
        // based on the indexing of the columns in the table object -- the
        // same ordering we use when storing values and meta types. skip
        // updates when setting params for DELETEs; the updates are just there
        // to let us eval fk constraints
        int i = (getAction() == ACTION_DELETE) ? _cols.length: 0;
        Column col;
        Object val;
        int half = _vals.length / 2;
        for (; i < _vals.length; i++) {
            if (_vals[i] == null)
                continue;

            // we don't set null params in the WHERE clause; we use the NULL
            // keyword instead to satisfy sqlserver
            if (_vals[i] == NULL && getAction() != ACTION_INSERT && i >= half)
                continue;

            // if this is an update the vals array will be 2 x the cols
            // array length; it repeats for where values
            if (i < _cols.length)
                col = _cols[i];
            else
                col = _cols[i - _cols.length];

            val = _vals[i];
            if (val == NULL)
                val = null;

            if (val == null || _types[i] != RAW) {
                dict.setTyped(stmnt, idx, val, col, _types[i], store);
                idx++;
            }
        }
        setFlushed(true);
    }

    /**
     * The array value array index for the given column's value.
     */
    private int getSetIndex(Column col) {
        return col.getIndex();
    }

    /**
     * The array value array index for the given column's value.
     */
    private int getWhereIndex(Column col) {
        return col.getIndex() + _cols.length;
    }

    /**
     * Performs a proper deep clone.
     */
    @Override
    public Object clone() {
        RowImpl clone = newInstance(getColumns(), getAction());
        copyInto(clone, false);
        return clone;
    }

    /**
     * Return a new row.
     */
    protected RowImpl newInstance(Column[] cols, int action) {
        return new RowImpl(cols, action);
    }

    /**
     * Copy all values from this row into the given one.
     *
     * @param whereOnly if true, only copy where conditions
     */
    public void copyInto(RowImpl row, boolean whereOnly) {
        int action = getAction();
        int rowAction = row.getAction();

        int start;
        int len;
        if (whereOnly) {
            if (action == ACTION_INSERT || rowAction == ACTION_INSERT)
                start = len = 0;
            else
                start = len = _vals.length / 2;
        } else {
            start = 0;
            if (rowAction == ACTION_INSERT && action != ACTION_INSERT)
                len = _vals.length / 2;
            else
                len = _vals.length;
        }

        System.arraycopy(_vals, start, row._vals, start, len);
        System.arraycopy(_types, start, row._types, start, len);
        if (isValid())
            row.setValid(true);
    }

    public Object[] getVals() {
        return _vals;
    }

    public int[] getTypes() {
        return _types;
    }

    public boolean isFlushed() {
        return _isFlushed;
    }

    public void setFlushed(boolean isFlushed) {
        _isFlushed = isFlushed;
    }
}

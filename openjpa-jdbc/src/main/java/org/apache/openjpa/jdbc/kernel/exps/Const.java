/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.kernel.exps;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.Constant;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;

/**
 * A literal or parameter in the filter.
 *
 * @author Abe White
 */
abstract class Const
    implements Val, Constant {

    private ClassMetaData _meta = null;
    private Column[] _cols = null;

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public boolean isVariable() {
        return false;
    }

    /**
     * Return the column for the value at the specified index, or null.
     */
    public Column getColumn(int index) {
        return (_cols != null && _cols.length > index) ? _cols[index] : null;
    }

    /**
     * Return the value of this constant.
     */
    public abstract Object getValue();

    public Object getValue(Object[] parameters) {
        return getValue();
    }

    /**
     * Return the SQL value of this constant.
     */
    public Object getSQLValue() {
        return getValue();
    }

    /**
     * Return true if this constant's SQL value is equivalent to NULL.
     */
    public boolean isSQLValueNull() {
        Object val = getSQLValue();
        if (val == null)
            return true;
        if (!(val instanceof Object[]))
            return false;

        // all-null array is considered null
        Object[] arr = (Object[]) val;
        for (int i = 0; i < arr.length; i++)
            if (arr[i] != null)
                return false;
        return true;
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
    }

    public Joins getJoins() {
        return null;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        if (other instanceof PCPath)
            _cols = ((PCPath) other).getColumns();
        else
            _cols = null;
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        sel.select(newSQLBuffer(sel, store, params, fetch), this);
    }

    private SQLBuffer newSQLBuffer(Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        calculateValue(sel, store, params, null, fetch);
        SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
        appendTo(buf, 0, sel, store, params, fetch);
        clearParameters();
        return buf;
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
        sel.groupBy(newSQLBuffer(sel, store, params, fetch), false);
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
        sel.orderBy(newSQLBuffer(sel, store, params, fetch), asc, false);
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        int code = JavaTypes.getTypeCode(getType());
        if (code == JavaTypes.OBJECT)
            code = JavaSQLTypes.JDBC_DEFAULT;
        return Filters.convert(res.getObject(this, code, null), getType());
    }

    public boolean hasVariable(Variable var) {
        return false;
    }

    public int length() {
        return 1;
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        Object obj = getValue();
        if (obj instanceof Collection && ((Collection) obj).isEmpty())
            sql.append("1 = 1");
        else if (obj instanceof Map && ((Map) obj).isEmpty())
            sql.append("1 = 1");
        else
            sql.append("1 <> 1");
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        Object obj = getValue();
        if (obj instanceof Collection && ((Collection) obj).isEmpty())
            sql.append("1 <> 1");
        else if (obj instanceof Map && ((Map) obj).isEmpty())
            sql.append("1 <> 1");
        else
            sql.append("1 = 1");
    }

    public void appendSize(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        Object obj = getValue();
        if (obj instanceof Collection)
            sql.appendValue(((Collection) obj).size());
        else if (obj instanceof Map)
            sql.appendValue(((Map) obj).size());
        else
            sql.append("1");
    }

    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (getSQLValue() == null)
            sql.append("1 = 1");
        else
            sql.append("1 <> 1");
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (getSQLValue() != null)
            sql.append("1 = 1");
        else
            sql.append("1 <> 1");
    }
}

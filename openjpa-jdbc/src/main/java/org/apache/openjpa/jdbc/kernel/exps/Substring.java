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

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Take a substring of a string.
 *
 * @author Abe White
 */
class Substring
    extends AbstractVal
    implements Val {

    private final Val _val1;
    private final Val _val2;
    private Joins _joins = null;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Provide the strings to operate on.
     */
    public Substring(Val val1, Val val2) {
        _val1 = val1;
        _val2 = val2;
    }

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        return String.class;
    }

    public void setImplicitType(Class type) {
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        _val1.initialize(sel, store, false);
        _val2.initialize(sel, store, false);
        _joins = sel.and(_val1.getJoins(), _val2.getJoins());
    }

    public Joins getJoins() {
        return _joins;
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchState fetchState) {
        sel.select(newSQLBuffer(sel, store, params, fetchState), this);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
        _val1.selectColumns(sel, store, params, true, fetchState);
        _val2.selectColumns(sel, store, params, true, fetchState);
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchState fetchState) {
        sel.groupBy(newSQLBuffer(sel, store, params, fetchState), false);
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchState fetchState) {
        sel.orderBy(newSQLBuffer(sel, store, params, fetchState), asc, false);
    }

    private SQLBuffer newSQLBuffer(Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState) {
        calculateValue(sel, store, params, null, fetchState);
        SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
        appendTo(buf, 0, sel, store, params, fetchState);
        clearParameters();
        return buf;
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchState fetchState)
        throws SQLException {
        return Filters.convert(res.getObject(this,
            JavaSQLTypes.JDBC_DEFAULT, null), getType());
    }

    public boolean hasVariable(Variable var) {
        return _val1.hasVariable(var) || _val2.hasVariable(var);
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchState fetchState) {
        _val1.calculateValue(sel, store, params, null, fetchState);
        _val2.calculateValue(sel, store, params, null, fetchState);
    }

    public void clearParameters() {
        _val1.clearParameters();
        _val2.clearParameters();
    }

    public int length() {
        return 1;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
        FilterValue str = new FilterValueImpl(_val1, sel, store, params,
            fetchState);
        FilterValue start;
        FilterValue end = null;
        if (_val2 instanceof Args) {
            Val[] args = ((Args) _val2).getVals();
            start =
                new FilterValueImpl(args[0], sel, store, params, fetchState);
            end = new FilterValueImpl(args[1], sel, store, params, fetchState);
        } else
            start = new FilterValueImpl(_val2, sel, store, params, fetchState);

        store.getDBDictionary().substring(sql, str, start, end);
    }
}


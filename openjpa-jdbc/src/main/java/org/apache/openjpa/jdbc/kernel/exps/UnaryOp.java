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
 * Value produced by a unary operation on a value.
 *
 * @author Abe White
 */
abstract class UnaryOp
    extends AbstractVal
    implements Val {

    private final Val _val;
    private ClassMetaData _meta = null;
    private Class _cast = null;

    /**
     * Constructor. Provide the value to operate on.
     */
    public UnaryOp(Val val) {
        _val = val;
    }

    protected Val getVal() {
        return _val;
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
        if (_cast != null)
            return _cast;
        return getType(_val.getType());
    }

    public void setImplicitType(Class type) {
        _cast = type;
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        _val.initialize(sel, store, false);
    }

    public Joins getJoins() {
        return _val.getJoins();
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchState fetchState) {
        sel.select(newSQLBuffer(sel, store, params, fetchState), this);
        if (isAggregate())
            sel.setAggregate(true);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
        _val.selectColumns(sel, store, params, true, fetchState);
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
        return _val.hasVariable(var);
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchState fetchState) {
        _val.calculateValue(sel, store, params, null, fetchState);
    }

    public void clearParameters() {
        _val.clearParameters();
    }

    public int length() {
        return 1;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
        sql.append(getOperator());
        sql.append("(");
        _val.appendTo(sql, 0, sel, store, params, fetchState);
        sql.append(")");
    }

    /**
     * Return whether this operator is an aggregate.
     */
    protected boolean isAggregate() {
        return false;
    }

    /**
     * Return the type of this value based on the argument type. Returns
     * the argument type by default.
     */
    protected Class getType(Class c) {
        return c;
    }

    /**
     * Return the name of this operator.
     */
    protected abstract String getOperator();
}


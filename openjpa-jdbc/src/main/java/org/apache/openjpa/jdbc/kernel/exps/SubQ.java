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

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Subquery;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A subquery.
 *
 * @author Abe White
 */
class SubQ
    implements Val, Subquery {

    private final ClassMapping _candidate;
    private final boolean _subs;
    private final String _alias;
    private final SelectConstructor _cons = new SelectConstructor();

    private Class _type = null;
    private ClassMetaData _meta = null;
    private QueryExpressions _exps = null;
    private long _startIdx = 0;
    private long _endIdx = Long.MAX_VALUE;

    /**
     * Constructor. Supply candidate, whether subclasses are included in
     * the query, and the query alias.
     */
    public SubQ(ClassMapping candidate, boolean subs, String alias) {
        _candidate = candidate;
        _subs = subs;
        _alias = alias;
    }

    /**
     * Return the subquery candidate type.
     */
    public ClassMapping getCandidate() {
        return _candidate;
    }

    public Class getType() {
        if (_exps != null) {
            if (_exps.projections.length == 0)
                return _candidate.getDescribedType();
            if (_exps.projections.length == 1)
                return _exps.projections[0].getType();
        }
        return _type;
    }

    public void setImplicitType(Class type) {
        if (_exps != null && _exps.projections.length == 1)
            _exps.projections[0].setImplicitType(type);
        _type = type;
    }

    public boolean isVariable() {
        return false;
    }

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public String getCandidateAlias() {
        return _alias;
    }

    public void setQueryExpressions(QueryExpressions query, long startIdx,
        long endIdx) {
        _exps = query;
        _startIdx = startIdx;
        _endIdx = endIdx;
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
    }

    public Joins getJoins() {
        return null;
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        if (_exps.projections.length == 0)
            return _candidate.toDataStoreValue(val,
                _candidate.getPrimaryKeyColumns(), store);
        if (_exps.projections.length == 1)
            return ((Val) _exps.projections[0]).toDataStoreValue(val, store);
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        selectColumns(sel, store, params, pks, fetch);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        sel.select(newSQLBuffer(sel, store, params, fetch), this);
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
        sel.groupBy(newSQLBuffer(sel, store, params, fetch), false);
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
        sel.orderBy(newSQLBuffer(sel, store, params, fetch), asc, false);
    }

    private SQLBuffer newSQLBuffer(Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
        appendTo(buf, 0, sel, store, params, fetch);
        return buf;
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return Filters.convert(res.getObject(this,
            JavaSQLTypes.JDBC_DEFAULT, null), getType());
    }

    public boolean hasVariable(Variable var) {
        for (int i = 0; i < _exps.projections.length; i++)
            if (((Val) _exps.projections[i]).hasVariable(var))
                return true;
        if (_exps.filter != null)
            if (((Exp) _exps.filter).hasVariable(var))
                return true;
        for (int i = 0; i < _exps.grouping.length; i++)
            if (((Val) _exps.grouping[i]).hasVariable(var))
                return true;
        if (_exps.having != null)
            if (((Exp) _exps.having).hasVariable(var))
                return true;
        for (int i = 0; i < _exps.ordering.length; i++)
            if (((Val) _exps.ordering[i]).hasVariable(var))
                return true;
        return false;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
    }

    public void clearParameters() {
    }

    public int length() {
        return 1;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        appendTo(sql, index, sel, store, params, fetch, false);
    }

    private void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch,
        boolean size) {
        sel = _cons.evaluate(store, sel, _alias, _exps, params,
            _cons.CACHE_NULL, fetch);
        _cons.select(store, _candidate, _subs, sel, _exps, params,
            fetch, fetch.EAGER_NONE);
        sel.setRange(_startIdx, _endIdx);

        if (size)
            sql.appendCount(sel, fetch);
        else
            sql.append(sel, fetch);
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        sql.append("NOT EXISTS ");
        appendTo(sql, 0, sel, store, params, fetch);
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        sql.append("EXISTS ");
        appendTo(sql, 0, sel, store, params, fetch);
    }

    public void appendSize(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        appendTo(sql, 0, sel, store, params, fetch, true);
    }

    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        appendTo(sql, 0, sel, store, params, fetch);
        sql.append(" IS NULL");
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        appendTo(sql, 0, sel, store, params, fetch);
        sql.append(" IS NOT NULL");
    }
}

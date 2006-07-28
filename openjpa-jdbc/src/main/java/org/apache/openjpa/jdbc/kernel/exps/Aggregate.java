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
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Aggregate listener that evaluates to a value.
 *
 * @author Abe White
 */
class Aggregate
    extends AbstractVal
    implements Val {

    private final JDBCAggregateListener _listener;
    private final Val _arg;
    private final ClassMapping _candidate;
    private Joins _joins = null;
    private ClassMetaData _meta = null;
    private Class _cast = null;

    /**
     * Constructor.
     */
    public Aggregate(JDBCAggregateListener listener, Val arg,
        ClassMapping candidate) {
        _listener = listener;
        _arg = arg;
        _candidate = candidate;
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
        return _listener.getType(getArgTypes());
    }

    private Class[] getArgTypes() {
        if (_arg == null)
            return null;
        if (_arg instanceof Args)
            return ((Args) _arg).getTypes();
        return new Class[]{ _arg.getType() };
    }

    public void setImplicitType(Class type) {
        _cast = type;
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        // note that we tell targets and args to extensions that are sql
        // paths to go ahead and join to their related object (if any),
        // because we assume that, unlike most operations, if a relation
        // field like a 1-1 is given as the target of an extension, then
        // the extension probably acts on some field or column in the
        // related object, not the 1-1 field itself
        if (_arg != null) {
            _arg.initialize(sel, store, false);
            if (_arg instanceof PCPath)
                ((PCPath) _arg).joinRelation();
            _joins = _arg.getJoins();
        }
    }

    public Joins getJoins() {
        return _joins;
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        sel.select(newSQLBuffer(sel, store, params, fetch), this);
        sel.setAggregate(true);
    }

    public void selectColumns(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        if (_arg != null)
            _arg.selectColumns(sel, store, params, true, fetch);
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
        calculateValue(sel, store, params, null, fetch);
        SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
        appendTo(buf, 0, sel, store, params, fetch);
        clearParameters();
        return buf;
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return Filters.convert(res.getObject(this,
            JavaSQLTypes.JDBC_DEFAULT, null), getType());
    }

    public boolean hasVariable(Variable var) {
        if (_arg != null)
            return _arg.hasVariable(var);
        return false;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        if (_arg != null)
            _arg.calculateValue(sel, store, params, null, fetch);
    }

    public void clearParameters() {
        if (_arg != null)
            _arg.clearParameters();
    }

    public int length() {
        return 1;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        _listener.appendTo(sql, getArgs(sel, store, params, fetch),
            _candidate, store);
        sel.append(sql, _joins);
    }

    private FilterValue[] getArgs(Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        if (_arg == null)
            return null;
        if (_arg instanceof Args) {
            Val[] vals = ((Args) _arg).getVals();
            FilterValue[] filts = new FilterValue[vals.length];
            for (int i = 0; i < vals.length; i++)
                filts[i] = new FilterValueImpl(vals[i], sel, store, params,
                    fetch);
            return filts;
        }
        return new FilterValue[]{
            new FilterValueImpl(_arg, sel, store, params, fetch)
        };
    }
}

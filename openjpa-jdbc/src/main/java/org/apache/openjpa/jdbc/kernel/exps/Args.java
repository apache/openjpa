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

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.Arguments;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A list of arguments to a multi-argument function.
 *
 * @author Abe White
 */
public class Args
    extends AbstractVal
    implements Arguments {

    private final Val[] _args;
    private Joins _joins = null;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Supply values being combined.
     */
    public Args(Val val1, Val val2) {
        int len1 = (val1 instanceof Args) ? ((Args) val1)._args.length : 1;
        int len2 = (val2 instanceof Args) ? ((Args) val2)._args.length : 1;

        _args = new Val[len1 + len2];
        if (val1 instanceof Args)
            System.arraycopy(((Args) val1)._args, 0, _args, 0, len1);
        else
            _args[0] = val1;
        if (val2 instanceof Args)
            System.arraycopy(((Args) val2)._args, 0, _args, len1, len2);
        else
            _args[len1] = val2;
    }

    public Value[] getValues() {
        return _args;
    }

    public Val[] getVals() {
        return _args;
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
        return Object[].class;
    }

    public Class[] getTypes() {
        Class[] c = new Class[_args.length];
        for (int i = 0; i < _args.length; i++)
            c[i] = _args[i].getType();
        return c;
    }

    public void setImplicitType(Class type) {
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        for (int i = 0; i < _args.length; i++) {
            _args[i].initialize(sel, store, nullTest);
            if (_joins == null)
                _joins = _args[i].getJoins();
            else
                _joins = sel.and(_joins, _args[i].getJoins());
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
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        for (int i = 0; i < _args.length; i++)
            _args[i].selectColumns(sel, store, params, pks, fetch);
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch) {
        return null;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        for (int i = 0; i < _args.length; i++)
            _args[i].calculateValue(sel, store, params, null, fetch);
    }

    public void clearParameters() {
        for (int i = 0; i < _args.length; i++)
            _args[i].clearParameters();
    }

    public int length() {
        return 0;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
    }

    public void appendSize(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
    }

    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        for (int i = 0; i < _args.length; i++)
            _args[i].acceptVisit(visitor);
        visitor.exit(this);
    }
}

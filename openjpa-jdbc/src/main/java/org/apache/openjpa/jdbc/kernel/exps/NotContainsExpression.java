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

import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Negates a contains expression using a subselect to make sure no
 * elements meet the criteria.
 *
 * @author Abe White
 */
class NotContainsExpression
    implements Exp {

    private final Exp _exp;
    private Map _contains = null;

    /**
     * Constructor. Supply the expression to negate.
     */
    public NotContainsExpression(Exp exp) {
        _exp = exp;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _contains = contains;
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState) {
        DBDictionary dict = store.getDBDictionary();
        dict.assertSupport(dict.supportsSubselect, "SupportsSubselect");

        Select sub = store.getSQLFactory().newSelect();
        sub.setParent(sel, null);
        _exp.initialize(sub, store, params, _contains);
        sub.where(sub.and(null, _exp.getJoins()));

        SQLBuffer where = new SQLBuffer(dict).append("(");
        _exp.appendTo(where, sub, store, params, fetchState);
        if (where.getSQL().length() > 1)
            sub.where(where.append(")"));

        buf.append("0 = ");
        buf.appendCount(sub, fetchState.getJDBCFetchConfiguration());
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
        _exp.selectColumns(sel, store, params, true, fetchState);
    }

    public Joins getJoins() {
        return null;
    }

    public boolean hasContainsExpression() {
        return false;
    }

    public boolean hasVariable(Variable var) {
        return _exp.hasVariable(var);
    }
}

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

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Combines a bind variable expression with another.
 *
 * @author Abe White
 */
class BindVariableAndExpression
    implements Exp {

    private final BindVariableExpression _bind;
    private final Exp _exp;
    private Joins _joins = null;

    /**
     * Constructor. Supply the two combined expressions.
     */
    public BindVariableAndExpression(BindVariableExpression bind, Exp exp) {
        _bind = bind;
        _exp = exp;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _bind.initialize(sel, store, params, contains);
        _exp.initialize(sel, store, params, contains);
        _joins = sel.and(_bind.getJoins(), _exp.getJoins());
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        boolean or = _exp instanceof OrExpression;
        if (or)
            buf.append("(");
        _exp.appendTo(buf, sel, store, params, fetch);
        if (or)
            buf.append(")");
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _exp.selectColumns(sel, store, params, pks, fetch);
    }

    public Joins getJoins() {
        return _joins;
    }

    public boolean hasContainsExpression() {
        return true;
    }

    public boolean hasVariable(Variable var) {
        return _exp.hasVariable(var);
    }
}

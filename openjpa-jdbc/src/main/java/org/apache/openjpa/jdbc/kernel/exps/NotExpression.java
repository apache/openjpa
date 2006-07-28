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
 * Negates an expression.
 *
 * @author Abe White
 */
class NotExpression
    implements Exp {

    private final Exp _exp;
    private Joins _joins = null;

    /**
     * Constructor. Supply the expression to negate.
     */
    public NotExpression(Exp exp) {
        _exp = exp;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _exp.initialize(sel, store, params, contains);
        _joins = sel.or(_exp.getJoins(), null);
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        buf.append("NOT (");
        _exp.appendTo(buf, sel, store, params, fetch);
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
        return _exp.hasContainsExpression();
    }

    public boolean hasVariable(Variable var) {
        return _exp.hasVariable(var);
    }
}

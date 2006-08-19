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
import org.apache.openjpa.kernel.exps.ExpressionVisitor;

/**
 * Combines two expressions.
 *
 * @author Abe White
 */
class AndExpression
    implements Exp {

    private final Exp _exp1;
    private final Exp _exp2;
    private Joins _joins = null;
    private boolean _paren1 = false;
    private boolean _paren2 = false;

    /**
     * Constructor. Supply the expressions to combine.
     */
    public AndExpression(Exp exp1, Exp exp2) {
        _exp1 = exp1;
        _exp2 = exp2;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _exp1.initialize(sel, store, params, contains);
        _exp2.initialize(sel, store, params, contains);
        _joins = sel.and(_exp1.getJoins(), _exp2.getJoins());

        _paren1 = _exp1 instanceof OrExpression;
        _paren2 = _exp2 instanceof OrExpression;
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        if (_paren1)
            buf.append("(");
        _exp1.appendTo(buf, sel, store, params, fetch);
        if (_paren1)
            buf.append(")");
        buf.append(" AND ");
        if (_paren2)
            buf.append("(");
        _exp2.appendTo(buf, sel, store, params, fetch);
        if (_paren2)
            buf.append(")");
        sel.append(buf, _joins);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _exp1.selectColumns(sel, store, params, pks, fetch);
        _exp2.selectColumns(sel, store, params, pks, fetch);
    }

    public Joins getJoins() {
        return _joins;
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _exp1.acceptVisit(visitor);
        _exp2.acceptVisit(visitor);
        visitor.exit(this);
    }
}

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Combines two expressions.
 *
 * @author Abe White
 */
class OrExpression
    implements Exp {

    private final Exp _exp1;
    private final Exp _exp2;
    private Joins _joins = null;

    /**
     * Constructor. Supply the expressions to combine.
     */
    public OrExpression(Exp exp1, Exp exp2) {
        _exp1 = exp1;
        _exp2 = exp2;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        // when OR'ing expressions each expression gets its own copy of the
        // contains counts, cause it's OK for each to use the same aliases
        Map contains2 = null;
        if (contains != null)
            contains2 = new HashMap(contains);

        _exp1.initialize(sel, store, params, contains);
        _exp2.initialize(sel, store, params, contains2);
        _joins = sel.or(_exp1.getJoins(), _exp2.getJoins());
        if (contains == null)
            return;

        // combine the contains counts from the copy into the main map
        Map.Entry entry;
        Integer val1, val2;
        for (Iterator itr = contains2.entrySet().iterator();
            itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            val2 = (Integer) entry.getValue();
            val1 = (Integer) contains.get(entry.getKey());
            if (val1 == null || val2.intValue() > val1.intValue())
                contains.put(entry.getKey(), val2);
        }
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        boolean paren = _joins != null && !_joins.isEmpty();
        if (paren)
            buf.append("(");

        _exp1.appendTo(buf, sel, store, params, fetch);
        buf.append(" OR ");
        _exp2.appendTo(buf, sel, store, params, fetch);

        if (paren)
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

    public boolean hasContainsExpression() {
        return _exp1.hasContainsExpression() || _exp2.hasContainsExpression();
    }

    public boolean hasVariable(Variable var) {
        return _exp1.hasVariable(var) || _exp2.hasVariable(var);
    }
}

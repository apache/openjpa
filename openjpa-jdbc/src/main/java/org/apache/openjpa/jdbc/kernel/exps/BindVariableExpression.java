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
 * Binds a variable to a value. Typically, the {@link #initialize} and
 * {@link #getJoins} methods of this expression are not called. They are
 * only called if the variable being bound is otherwise unused in the filter,
 * in which case we must at least make the joins to the variable because the
 * act of binding a variable should at least guarantee that an instance
 * represting the variable could exist (i.e. the binding collection is not
 * empty).
 *
 * @author Abe White
 */
class BindVariableExpression
    extends EmptyExpression {

    private final Variable _var;

    /**
     * Constructor. Supply values.
     */
    public BindVariableExpression(Variable var, PCPath val, boolean key) {
        if (key)
            val.getKey();
        var.setPCPath(val);
        _var = var;
    }

    public Variable getVariable() {
        return _var;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _var.initialize(sel, store, false);
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        buf.append("1 = 1");
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
    }

    public Joins getJoins() {
        return _var.getJoins();
    }

    public boolean hasContainsExpression() {
        return false;
    }

    public boolean hasVariable(Variable var) {
        return _var == var;
    }
}

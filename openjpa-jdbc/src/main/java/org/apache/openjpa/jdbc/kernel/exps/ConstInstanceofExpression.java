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
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;

/**
 * Tests whether a value is an instance of a class.
 *
 * @author Abe White
 */
class ConstInstanceofExpression
    implements Exp {

    private final Const _const;
    private final Class _cls;

    /**
     * Constructor. Supply the constant to test and the class.
     */
    public ConstInstanceofExpression(Const val, Class cls) {
        _const = val;
        _cls = Filters.wrap(cls);
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _const.initialize(sel, store, false);
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState) {
        _const.calculateValue(sel, store, params, null, fetchState);
        if (_cls.isInstance(_const.getValue()))
            buf.append("1 = 1");
        else
            buf.append("1 <> 1");
        _const.clearParameters();
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
        _const.selectColumns(sel, store, params, pks, fetchState);
    }

    public Joins getJoins() {
        return _const.getJoins();
    }

    public boolean hasContainsExpression() {
        return false;
    }

    public boolean hasVariable(Variable var) {
        return _const.hasVariable(var);
    }
}

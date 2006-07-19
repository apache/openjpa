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
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * Compares two values.
 *
 * @author Abe White
 */
class CompareExpression
    implements Exp {

    public static final String LESS = "<";
    public static final String GREATER = ">";
    public static final String LESS_EQUAL = "<=";
    public static final String GREATER_EQUAL = ">=";

    private static final Localizer _loc = Localizer.forPackage
        (CompareExpression.class);

    private final Val _val1;
    private final Val _val2;
    private final String _op;
    private Joins _joins = null;

    /**
     * Constructor. Supply values and operator.
     */
    public CompareExpression(Val val1, Val val2, String op) {
        _val1 = val1;
        _val2 = val2;
        _op = op;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _val1.initialize(sel, store, false);
        _val2.initialize(sel, store, false);
        _joins = sel.and(_val1.getJoins(), _val2.getJoins());
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState) {
        _val1.calculateValue(sel, store, params, _val2, fetchState);
        _val2.calculateValue(sel, store, params, _val1, fetchState);
        if (!Filters.canConvert(_val1.getType(), _val2.getType(), false)
            && !Filters.canConvert(_val2.getType(), _val1.getType(), false))
            throw new UserException(_loc.get("cant-convert", _val1.getType(),
                _val2.getType()));

        store.getDBDictionary().comparison(buf, _op,
            new FilterValueImpl(_val1, sel, store, params, fetchState),
            new FilterValueImpl(_val2, sel, store, params, fetchState));
        sel.append(buf, _joins);

        _val1.clearParameters();
        _val2.clearParameters();
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
        _val1.selectColumns(sel, store, params, true, fetchState);
        _val2.selectColumns(sel, store, params, true, fetchState);
    }

    public Joins getJoins() {
        return _joins;
    }

    public boolean hasContainsExpression() {
        return false;
    }

    public boolean hasVariable(Variable var) {
        return _val1.hasVariable(var) || _val2.hasVariable(var);
    }
}

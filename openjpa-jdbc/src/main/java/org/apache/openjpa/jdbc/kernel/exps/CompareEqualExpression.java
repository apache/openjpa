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
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * Compares two values for equality.
 *
 * @author Abe White
 */
abstract class CompareEqualExpression
    implements Exp {

    private static final Localizer _loc = Localizer.forPackage
        (CompareEqualExpression.class);

    private final Val _val1;
    private final Val _val2;
    private Joins _joins = null;

    /**
     * Constructor. Supply values to compare.
     */
    public CompareEqualExpression(Val val1, Val val2) {
        _val1 = val1;
        _val2 = val2;
    }

    public Val getValue1() {
        return _val1;
    }

    public Val getValue2() {
        return _val2;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        boolean direct = isDirectComparison();
        _val1.initialize(sel, store, direct && isNull(_val2, params));
        _val2.initialize(sel, store, direct && isNull(_val1, params));
        _joins = sel.and(_val1.getJoins(), _val2.getJoins());
    }

    /**
     * Return whether the given value is null.
     */
    private boolean isNull(Val val, Object[] params) {
        if (val instanceof Null)
            return true;
        if (!(val instanceof Param))
            return false;

        Param param = (Param) val;
        return params[param.getIndex()] == null;
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        _val1.calculateValue(sel, store, params, _val2, fetch);
        _val2.calculateValue(sel, store, params, _val1, fetch);
        if (!Filters.canConvert(_val1.getType(), _val2.getType(), false)
            && !Filters.canConvert(_val2.getType(), _val1.getType(), false))
            throw new UserException(_loc.get("cant-convert", _val1.getType(),
                _val2.getType()));

        boolean val1Null = _val1 instanceof Const
            && ((Const) _val1).isSQLValueNull();
        boolean val2Null = _val2 instanceof Const
            && ((Const) _val2).isSQLValueNull();
        appendTo(buf, sel, store, params, fetch, val1Null, val2Null);
        sel.append(buf, _joins);

        _val1.clearParameters();
        _val2.clearParameters();
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _val1.selectColumns(sel, store, params, true, fetch);
        _val2.selectColumns(sel, store, params, true, fetch);
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

    /**
     * Append the SQL for the comparison.
     */
    protected abstract void appendTo(SQLBuffer buf, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch,
        boolean val1Null, boolean val2Null);

    /**
     * Subclasses can override this method if, when they compare to another,
     * value, the comparison is indirect. For example, field.contains (x)
     * should compare element values to null, not the field itself.
     */
    protected boolean isDirectComparison() {
        return true;
    }
}

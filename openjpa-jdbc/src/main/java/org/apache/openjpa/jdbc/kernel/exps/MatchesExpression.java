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
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import serp.util.Strings;

/**
 * Test if a string matches a regexp.
 *
 * @author Abe White
 */
class MatchesExpression
    implements Exp {

    private final Val _val;
    private final Const _const;
    private final String _single;
    private final String _multi;
    private final String _escape;
    private Joins _joins = null;

    /**
     * Constructor. Supply values.
     */
    public MatchesExpression(Val val, Const con,
        String single, String multi, String escape) {
        _val = val;
        _const = con;
        _single = single;
        _multi = multi;
        _escape = escape;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _val.initialize(sel, store, false);
        _const.initialize(sel, store, false);
        _joins = sel.and(_val.getJoins(), _const.getJoins());
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        _val.calculateValue(sel, store, params, _const, fetch);
        _const.calculateValue(sel, store, params, _val, fetch);

        Column col = null;
        if (_val instanceof PCPath) {
            Column[] cols = ((PCPath) _val).getColumns();
            if (cols.length == 1)
                col = cols[0];
        }

        Object o = _const.getValue();
        if (o == null)
            buf.append("1 <> 1");
        else {
            // look for ignore case flag and strip it out if present
            boolean ignoreCase = false;
            String str = o.toString();
            int idx = str.indexOf("(?i)");
            if (idx != -1) {
                ignoreCase = true;
                if (idx + 4 < str.length())
                    str = str.substring(0, idx) + str.substring(idx + 4);
                else
                    str = str.substring(0, idx);
                str = str.toLowerCase();
            }

            // append target
            if (ignoreCase)
                buf.append("LOWER(");
            _val.appendTo(buf, 0, sel, store, params, fetch);
            if (ignoreCase)
                buf.append(")");

            // create a DB wildcard string by replacing the
            // multi token (e.g., '.*') and the single token (e.g., ".")
            // with '%' and '.' with '_'
            str = Strings.replace(str, _multi, "%");
            str = Strings.replace(str, _single, "_");

            buf.append(" LIKE ").appendValue(str, col);

            // escape out characters by using the database's escape sequence
            if (_escape != null)
                buf.append(" ESCAPE '").append(_escape).append("'");
        }
        sel.append(buf, _joins);

        _val.clearParameters();
        _const.clearParameters();
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _val.selectColumns(sel, store, params, true, fetch);
        _const.selectColumns(sel, store, params, true, fetch);
    }

    public Joins getJoins() {
        return _joins;
    }

    public boolean hasContainsExpression() {
        return false;
    }

    public boolean hasVariable(Variable var) {
        return _val.hasVariable(var) || _const.hasVariable(var);
    }
}

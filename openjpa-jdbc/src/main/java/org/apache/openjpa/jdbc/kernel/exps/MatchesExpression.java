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

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
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

    public ExpState initialize(Select sel, ExpContext ctx, Map contains) {
        ExpState s1 = _val.initialize(sel, ctx, 0);
        ExpState s2 = _const.initialize(sel, ctx, 0);
        return new BinaryOpExpState(sel.and(s1.joins, s2.joins), s1, s2);
    }

    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer buf) {
        BinaryOpExpState bstate = (BinaryOpExpState) state;
        _val.calculateValue(sel, ctx, bstate.state1, _const, bstate.state2);
        _const.calculateValue(sel, ctx, bstate.state2, _val, bstate.state1);

        Column col = null;
        if (_val instanceof PCPath) {
            Column[] cols = ((PCPath) _val).getColumns(bstate.state1);
            if (cols.length == 1)
                col = cols[0];
        }

        Object o = _const.getValue(ctx, bstate.state2);
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
            _val.appendTo(sel, ctx, bstate.state1, buf, 0);
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
        sel.append(buf, state.joins);
    }

    public void selectColumns(Select sel, ExpContext ctx, ExpState state, 
        boolean pks) {
        BinaryOpExpState bstate = (BinaryOpExpState) state;
        _val.selectColumns(sel, ctx, bstate.state1, true);
        _const.selectColumns(sel, ctx, bstate.state2, true);
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        _const.acceptVisit(visitor);
        visitor.exit(this);
    }
}

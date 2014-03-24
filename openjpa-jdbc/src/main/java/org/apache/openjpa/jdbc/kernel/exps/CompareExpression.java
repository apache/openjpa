/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.jdbc.kernel.exps;

import java.util.Map;

import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;
import org.apache.openjpa.jdbc.sql.Raw;

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

    /**
     * Constructor. Supply values and operator.
     */
    public CompareExpression(Val val1, Val val2, String op) {
        _val1 = val1;
        _val2 = val2;
        _op = op;
    }

    public ExpState initialize(Select sel, ExpContext ctx, Map contains) {
        ExpState s1 = _val1.initialize(sel, ctx, 0);
        ExpState s2 = _val2.initialize(sel, ctx, 0);
        return new BinaryOpExpState(sel.and(s1.joins, s2.joins), s1, s2);
    }

    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer buf) {
        BinaryOpExpState bstate = (BinaryOpExpState) state;
        _val1.calculateValue(sel, ctx, bstate.state1, _val2, bstate.state2);
        _val2.calculateValue(sel, ctx, bstate.state2, _val1, bstate.state1);
        Class val1Type = _val1.getType();
        Class val2Type = _val2.getType();
        // For purposes of the 'canConvert', when dealing with a Lit with Raw
        // use a String type since Raw contains a String.
        if (_val1 instanceof Lit && val1Type.isAssignableFrom(Raw.class)){
            val1Type = String.class;
        }
        if (_val2 instanceof Lit && val2Type.isAssignableFrom(Raw.class)){
            val2Type = String.class;
        }
        if (!Filters.canConvert(val1Type, val2Type, false)
            && !Filters.canConvert(val2Type, val1Type, false))
            throw new UserException(_loc.get("cant-convert", val1Type, val2Type));

        ctx.store.getDBDictionary().comparison(buf, _op,
            new FilterValueImpl(sel, ctx, bstate.state1, _val1),
            new FilterValueImpl(sel, ctx, bstate.state2, _val2));
        if (sel != null)
            sel.append(buf, state.joins);
    }

    public void selectColumns(Select sel, ExpContext ctx, ExpState state, 
        boolean pks) {
        BinaryOpExpState bstate = (BinaryOpExpState) state;
        _val1.selectColumns(sel, ctx, bstate.state1, true);
        _val2.selectColumns(sel, ctx, bstate.state2, true);
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val1.acceptVisit(visitor);
        _val2.acceptVisit(visitor);
        visitor.exit(this);
    }
}

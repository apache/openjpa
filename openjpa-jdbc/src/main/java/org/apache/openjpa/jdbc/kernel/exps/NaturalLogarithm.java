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

import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Natural logarithm (base e) value.
 */
public class NaturalLogarithm
    extends UnaryOp {


    private static final long serialVersionUID = 1L;

    /**
     * Constructor. Provide the value from which the natural logarithm should be calculated.
     */
    public NaturalLogarithm(Val val) {
        super(val);
    }

    @Override
    protected Class getType(Class c) {
        return double.class;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state, SQLBuffer sql, int index) {
        final String operator = ctx.store.getDBDictionary().naturalLogarithmFunction;
        sql.append(operator);
        sql.append(getNoParen() ? " " : "(");
        Val _val = getValue();
        _val.appendTo(sel, ctx, state, sql, 0);

        // OPENJPA-2149: If _val (Val) is an 'Arg', we need to get the Val[]
        // from it, and the single element it contains because the
        // 'addCastForParam' method gets the 'type' from the Val it receives.
        // In the case where _val is an Arg, when addCastForParam gets the
        // type, it will be getting the type of the Val (an Object) rather
        // the type of the Arg.
        sql.addCastForParam(operator,
            (_val instanceof Args aVal) ? (aVal.getVals())[0] : _val);
        if (!getNoParen()) {
            sql.append(")");
        }
    }

    @Override
    protected String getOperator() {
        return "LN";
    }

    @Override
    public int getId() {
        return Val.LN_VAL;
    }
}


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
package org.apache.openjpa.kernel.exps;

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.Localizer;

/**
 * An in-memory representation of a general case expression
 *
 * @author Catalina Wei
 */
class GeneralCase
    extends Val {

    
    private static final long serialVersionUID = 1L;

    private static final Localizer _loc = Localizer.forPackage(
        GeneralCase.class);

    private final Exp[] _exp;
    private final Val _val;

    public GeneralCase(Exp[] exp, Val val) {
        _exp = exp;
        _val = val;
    }

    @Override
    protected Object eval(Object candidate, Object orig, StoreContext ctx,
        Object[] params) {
        for (Exp exp : _exp) {
            boolean compare = ((WhenCondition) exp).getExp().
                    eval(candidate, orig, ctx, params);

            if (compare)
                return ((WhenCondition) exp).getVal().
                        eval(candidate, orig, ctx, params);
            else
                continue;
        }
        return _val.eval(candidate, orig, ctx, params);
    }

    protected Object eval(Object candidate,StoreContext ctx,
        Object[] params) {

        for (Exp exp : _exp) {
            boolean compare = ((WhenCondition) exp).getExp().
                    eval(candidate, null, ctx, params);

            if (compare)
                return ((WhenCondition) exp).getVal().
                        eval(candidate, null, ctx, params);
            else
                continue;
        }
        return _val.eval(candidate, null, ctx, params);
    }

    @Override
    public Class getType() {
        Class c1 = _val.getType();
        for (Exp exp : _exp) {
            Class c2 = ((WhenCondition) exp).getVal().getType();
            c1 = Filters.promote(c1, c2);
        }
        return c1;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        for (Exp exp : _exp) {
            exp.acceptVisit(visitor);
        }
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }
}

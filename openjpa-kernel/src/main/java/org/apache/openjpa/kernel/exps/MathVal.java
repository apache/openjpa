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
package org.apache.openjpa.kernel.exps;

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.StoreContext;

/**
 * Value produced by a mathematical operation on two values.
 *
 * @author Abe White
 */
abstract class MathVal
    extends Val {

    private final Val _val1;
    private final Val _val2;

    /**
     * Constructor. Provide the values to operate on.
     */
    public MathVal(Val val1, Val val2) {
        _val1 = val1;
        _val2 = val2;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        Class c1 = _val1.getType();
        Class c2 = _val2.getType();
        return Filters.promote(c1, c2);
    }

    public void setImplicitType(Class type) {
    }

    public boolean hasVariables() {
        return _val1.hasVariables() || _val2.hasVariables();
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        Object o1 = _val1.eval(candidate, orig, ctx, params);
        Object o2 = _val2.eval(candidate, orig, ctx, params);
        return operate(o1, _val1.getType(), o2, _val2.getType());
    }

    /**
     * Return the result of this mathematical operation on the two values.
     */
    protected abstract Object operate(Object o1, Class c1, Object o2,
        Class c2);
}

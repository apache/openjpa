/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel.exps;

import java.util.Collection;

import org.apache.openjpa.kernel.StoreContext;

/**
 * Boolean value used as an expression.
 *
 * @author Abe White
 */
class ValExpression extends Exp {

    private final Val _val;

    /**
     * Constructor. Supply value.
     */
    public ValExpression(Val val) {
        _val = val;
    }

    protected boolean eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        Object o = _val.eval(candidate, orig, ctx, params);
        return o != null && ((Boolean) o).booleanValue();
    }

    protected boolean eval(Collection candidates, StoreContext ctx,
        Object[] params) {
        Collection c = _val.eval(candidates, null, ctx, params);
        Object o = (c == null || c.isEmpty()) ? null : c.iterator().next();
        return o != null && ((Boolean) o).booleanValue();
    }
}

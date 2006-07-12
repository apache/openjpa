/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel.exps;

import org.apache.openjpa.kernel.StoreContext;
import serp.util.Numbers;

/**
 * <p>Returns the number of characters in the String.</p>
 *
 * @author Marc Prud'hommeaux
 */
class StringLength
    extends Val {

    private final Val _val;
    private Class _cast = null;

    /**
     * Constructor.  Provide value to upper-case.
     */
    public StringLength(Val val) {
        _val = val;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        if (_cast != null)
            return _cast;
        return int.class;
    }

    public void setImplicitType(Class type) {
        _cast = type;
    }

    public boolean hasVariables() {
        return _val.hasVariables();
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        Object eval = _val.eval(candidate, orig, ctx, params);
        if (eval == null)
            return Numbers.valueOf(0);

        return Numbers.valueOf(eval.toString().length());
    }
}


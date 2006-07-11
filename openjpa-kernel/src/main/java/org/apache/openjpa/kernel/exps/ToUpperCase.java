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

import org.apache.openjpa.kernel.StoreContext;

/**
 * Upper-case a string.
 *
 * @author Abe White
 */
class ToUpperCase extends Val {

    private final Val _val;

    /**
     * Constructor. Provide value to upper-case.
     */
    public ToUpperCase(Val val) {
        _val = val;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        return String.class;
    }

    public void setImplicitType(Class type) {
    }

    public boolean hasVariables() {
        return _val.hasVariables();
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        return _val.eval(candidate, orig, ctx, params).toString().
            toUpperCase();
    }
}

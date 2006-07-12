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

/**
 * <p>Trims leading, trailing, or both charactes from a String.</p>
 *
 * @author Marc Prud'hommeaux
 */
class Trim
    extends Val {

    private final Val _val;
    private final Val _trimChar;
    private final Boolean _where;

    /**
     * Constructor.  Provide value to upper-case.
     */
    public Trim(Val val, Val trimChar, Boolean where) {
        _val = val;
        _trimChar = trimChar;
        _where = where;
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
        Object eval = _val.eval(candidate, orig, ctx, params);

        if (eval == null)
            return null;

        String toTrim = _trimChar.eval(candidate, orig, ctx, params).
            toString();

        String str = eval.toString();

        // null indicates both, TRUE indicates leading
        if (_where == null || Boolean.TRUE.equals(_where)) {
            while (str.startsWith(toTrim))
                str = str.substring(toTrim.length());
        }

        // null indicates both, FALSE indicates trailing
        if (_where == null || Boolean.FALSE.equals(_where)) {
            while (str.endsWith(toTrim))
                str = str.substring(0, str.length() - toTrim.length());
        }

        return str;
    }
}


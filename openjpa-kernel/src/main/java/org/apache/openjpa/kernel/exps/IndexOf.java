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
 * <p>Find the index of one string within another.</p>
 *
 * @author Abe White
 */
class IndexOf
    extends Val {

    private final Val _val;
    private final Val _args;

    /**
     * Constructor.  Provide target string and the arguments to the
     * indexOf method.
     */
    public IndexOf(Val val, Val args) {
        _val = val;
        _args = args;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        return int.class;
    }

    public void setImplicitType(Class type) {
    }

    public boolean hasVariables() {
        return _val.hasVariables() || _args.hasVariables();
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        Object str = _val.eval(candidate, orig, ctx, params);
        Object arg = _args.eval(candidate, orig, ctx, params);
        int idx;
        if (arg instanceof Object[]) {
            Object[] args = (Object[]) arg;
            idx = str.toString().indexOf(args[0].toString(),
                ((Number) args[1]).intValue());
        } else
            idx = str.toString().indexOf(arg.toString());
        return Numbers.valueOf(idx);
    }
}

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

import java.util.Collection;

import org.apache.openjpa.kernel.StoreContext;

/**
 * An expression that AND's two others together.
 *
 * @author Abe White
 */
class AndExpression
    extends Exp {

    private final Exp _exp1;
    private final Exp _exp2;

    /**
     * Constructor. Supply expressions to combine.
     */
    public AndExpression(Exp exp1, Exp exp2) {
        _exp1 = exp1;
        _exp2 = exp2;
    }

    public Exp getExpression1() {
        return _exp1;
    }

    public Exp getExpression2() {
        return _exp2;
    }

    protected boolean eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        return _exp1.evaluate(candidate, orig, ctx, params)
            && _exp2.evaluate(candidate, orig, ctx, params);
    }

    protected boolean eval(Collection candidates, StoreContext ctx,
        Object[] params) {
        return _exp1.evaluate(candidates, ctx, params)
            && _exp2.evaluate(candidates, ctx, params);
    }
}


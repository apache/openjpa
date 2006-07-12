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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.StoreContext;

/**
 * <p>Represents a cast.</p>
 *
 * @author Abe White
 */
class Cast
    extends Val {

    private final Val _val;
    private final Class _cast;

    /**
     * Constructor.  Provide value to cast and type to cast to.
     */
    public Cast(Val val, Class cast) {
        _val = val;
        _cast = cast;
    }

    public boolean isVariable() {
        return _val.isVariable();
    }

    public Class getType() {
        return _cast;
    }

    public void setImplicitType(Class type) {
    }

    public boolean hasVariables() {
        return _val.hasVariables();
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        return Filters.convert(_val.eval(candidate, orig, ctx, params), _cast);
    }

    protected Collection eval(Collection candidates, Object orig,
        StoreContext ctx, Object[] params) {
        Collection res = _val.eval(candidates, orig, ctx, params);
        if (res == null || res.isEmpty())
            return res;

        Collection casts = new ArrayList(res.size());
        for (Iterator itr = res.iterator(); itr.hasNext();)
            casts.add(Filters.convert(itr.next(), _cast));
        return casts;
    }
}

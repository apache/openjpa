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

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.StoreContext;

/**
 * Represents a literal.
 *
 * @author Abe White
 */
class Lit extends Val implements Literal {

    private Object _val;
    private final int _ptype;

    /**
     * Constructor. Provide constant value.
     */
    public Lit(Object val, int ptype) {
        _val = val;
        _ptype = ptype;
    }

    public boolean isVariable() {
        return false;
    }

    public Object getValue() {
        return _val;
    }

    public void setValue(Object val) {
        _val = val;
    }

    public int getParseType() {
        return _ptype;
    }

    public Object getValue(Object[] parameters) {
        return _val;
    }

    public Class getType() {
        return (_val == null) ? Object.class : _val.getClass();
    }

    public void setImplicitType(Class type) {
        _val = Filters.convert(_val, type);
    }

    public boolean hasVariables() {
        return false;
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        return _val;
    }
}

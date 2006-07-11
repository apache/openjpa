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
 * Represents a parameter.
 *
 * @author Abe White
 */
class Param extends Val implements Parameter {

    private String _name = null;
    private Class _type = null;
    private int _index = -1;

    /**
     * Constructor. Provide parameter name and type.
     */
    public Param(String name, Class type) {
        _name = name;
        _type = type;
    }

    public String getParameterName() {
        return _name;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        return _type;
    }

    public void setImplicitType(Class type) {
        _type = type;
    }

    public boolean hasVariables() {
        return false;
    }

    public void setIndex(int index) {
        _index = index;
    }

    public Object getValue(Object[] params) {
        return params[_index];
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        return getValue(params);
    }
}


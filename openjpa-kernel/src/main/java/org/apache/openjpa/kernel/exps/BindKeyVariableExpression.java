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
import java.util.Map;

/**
 * {@link BindVariableExpression} for map key sets.
 *
 * @author Abe White
 */
class BindKeyVariableExpression
    extends BindVariableExpression {

    /**
     * Constructor.
     *
     * @param var the bound variable
     * @param val the value the variable is bound to
     */
    public BindKeyVariableExpression(BoundVariable var, Val val) {
        super(var, val);
    }

    protected Collection getCollection(Object values) {
        Map map = (Map) values;
        return (map == null) ? null : map.keySet();
    }
}


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

import org.apache.openjpa.kernel.Filters;

/**
 * <p>Value produced by one value being divided by another.</p>
 *
 * @author Abe White
 */
class Divide
    extends MathVal {

    /**
     * Constructor.  Provide the values to divide.
     */
    public Divide(Val val1, Val val2) {
        super(val1, val2);
    }

    protected Object operate(Object o1, Class c1, Object o2, Class c2) {
        return Filters.divide(o1, c1, o2, c2);
    }
}


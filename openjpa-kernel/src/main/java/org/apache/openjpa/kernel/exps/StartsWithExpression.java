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

/**
 * Expression that compares two others.
 *
 * @author Abe White
 */
class StartsWithExpression extends CompareExpression {

    /**
     * Constructor. Supply values to compare.
     */
    public StartsWithExpression(Val val1, Val val2) {
        super(val1, val2);
    }

    protected boolean compare(Object o1, Object o2) {
        if (o1 == null || o2 == null)
            return false;
        return o1.toString().startsWith(o2.toString());
    }
}

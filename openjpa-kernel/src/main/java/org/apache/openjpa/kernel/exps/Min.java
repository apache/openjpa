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

import java.util.Collection;
import java.util.Iterator;

/**
 * Find the min.
 *
 * @author Abe White
 */
class Min extends AggregateVal {

    /**
     * Constructor. Provide the value to minimize.
     */
    public Min(Val val) {
        super(val);
    }

    protected Class getType(Class c) {
        return c;
    }

    protected Object operate(Collection os, Class c) {
        Comparable min = null;
        Comparable cur;
        for (Iterator itr = os.iterator(); itr.hasNext();) {
            cur = (Comparable) itr.next();
            if (cur != null && (min == null || min.compareTo(cur) > 0))
                min = cur;
        }
        return min;
    }
}

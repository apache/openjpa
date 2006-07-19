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
import java.util.Iterator;

import serp.util.Numbers;

/**
 * Count non-null values.
 *
 * @author Abe White
 */
class Count
    extends AggregateVal {

    /**
     * Constructor. Provide the value to count.
     */
    public Count(Val val) {
        super(val);
    }

    protected Class getType(Class c) {
        return long.class;
    }

    protected Object operate(Collection os, Class c) {
        long count = 0;
        for (Iterator itr = os.iterator(); itr.hasNext();)
            if (itr.next() != null)
                count++;
        return Numbers.valueOf(count);
    }
}

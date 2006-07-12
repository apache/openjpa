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

import java.util.Collection;
import java.util.Iterator;

import org.apache.openjpa.kernel.Filters;
import serp.util.Numbers;

/**
 * <p>Average values.</p>
 *
 * @author Abe White
 */
class Avg
    extends AggregateVal {

    /**
     * Constructor.  Provide the value to average.
     */
    public Avg(Val val) {
        super(val);
    }

    protected Class getType(Class c) {
        return c;
    }

    protected Object operate(Collection os, Class c) {
        if (os.isEmpty())
            return null;

        Object sum = Filters.convert(Numbers.valueOf(0), c);
        Object cur;
        int size = 0;
        for (Iterator itr = os.iterator(); itr.hasNext();) {
            cur = itr.next();
            if (cur == null)
                continue;

            sum = Filters.add(sum, c, cur, c);
            size++;
        }
        if (size == 0)
            return null;
        return Filters.divide(sum, c, Numbers.valueOf(size), int.class);
    }
}

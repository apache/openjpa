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

import serp.util.Numbers;

/**
 * Returns the count of a collection.
 *
 * @author Marc Prud'hommeaux
 */
class Size
    extends UnaryMathVal {

    public Size(Val val) {
        super(val);
    }

    protected Class getType(Class c) {
        return int.class;
    }

    protected Object operate(Object o, Class c) {
        if (o instanceof Collection)
            return Numbers.valueOf(((Collection) o).size());
        if (o instanceof Map)
            return Numbers.valueOf(((Map) o).size());
        return (o == null) ? Numbers.valueOf(0) : Numbers.valueOf(1);
    }
}

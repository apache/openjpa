/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.kernel.exps;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.openjpa.kernel.Filters;

/**
 * Take the sign of a number.
 */
class Sign
    extends UnaryMathVal {

    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor. Provide the value whose sign will be found.
     */
    public Sign(Val val) {
        super(val);
    }

    @Override
    protected Class getType(Class c) {
        return int.class;
    }

    @Override
    protected Object operate(Object o, Class c) {
        c = Filters.wrap(c);
        float value = 0;
        if (c == Integer.class || c == Float.class || c == Double.class || c == Long.class)
            value = ((Number) o).floatValue();
        else if (c == BigDecimal.class)
            value = ((BigDecimal) o).floatValue();
        else if (c == BigInteger.class)
            value = ((BigInteger) o).floatValue();
        return Math.round(Math.signum(value));
    }
}

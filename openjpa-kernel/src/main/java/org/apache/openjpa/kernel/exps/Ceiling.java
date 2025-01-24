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
import java.math.RoundingMode;

import org.apache.openjpa.kernel.Filters;

/**
 * Take the ceiling value of a number.
 */
class Ceiling
    extends UnaryMathVal {

    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor. Provide the number whose ceiling value to calculate.
     */
    public Ceiling(Val val) {
        super(val);
    }

    @Override
    protected Class getType(Class c) {
        Class wrap = Filters.wrap(c);
        if (wrap == Integer.class
            || wrap == Float.class
            || wrap == Double.class
            || wrap == Long.class
            || wrap == BigDecimal.class
            || wrap == BigInteger.class) {
            return Filters.unwrap(c);
        }
        return int.class;
    }

    @Override
    protected Object operate(Object o, Class c) {
        c = Filters.wrap(c);
        if (c == Integer.class)
            return Math.ceil(((Number) o).intValue());
        if (c == Float.class)
            return Math.ceil(((Number) o).floatValue());
        if (c == Double.class)
            return Math.ceil(((Number) o).doubleValue());
        if (c == Long.class)
            return Math.ceil(((Number) o).longValue());
        if (c == BigDecimal.class)
            return ((BigDecimal) o).setScale(0, RoundingMode.CEILING);
        if (c == BigInteger.class)
            return ((BigInteger) o);

        // default to int
        return Math.ceil(((Number) o).intValue());
    }
}

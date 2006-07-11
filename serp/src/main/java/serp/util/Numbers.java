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
package serp.util;

/**
 * Number utilities.
 *
 * @author Abe White
 */
public class Numbers {

    private static final Integer INT_NEGONE = new Integer(-1);
    private static final Long LONG_NEGONE = new Long(-1);
    private static final Integer[] INTEGERS = new Integer[50];
    private static final Long[] LONGS = new Long[50];

    static {
        for (int i = 0; i < INTEGERS.length; i++)
            INTEGERS[i] = new Integer(i);
        for (int i = 0; i < LONGS.length; i++)
            LONGS[i] = new Long(i);
    }

    /**
     * Return the wrapper for the given number, taking advantage of cached
     * common values.
     */
    public static Integer valueOf(int n) {
        if (n == -1)
            return INT_NEGONE;
        if (n >= 0 && n < INTEGERS.length)
            return INTEGERS[n];
        return new Integer(n);
    }

    /**
     * Return the wrapper for the given number, taking advantage of cached
     * common values.
     */
    public static Long valueOf(long n) {
        if (n == -1)
            return LONG_NEGONE;
        if (n >= 0 && n < LONGS.length)
            return LONGS[(int) n];
        return new Long(n);
    }
}

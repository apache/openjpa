/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


public final class StringUtil {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];


    private static final Byte      BYTE_ZERO    = Byte.valueOf((byte) 0);
    private static final Character CHAR_ZERO    = Character.valueOf((char) 0);
    private static final Double    DOUBLE_ZERO  = Double.valueOf(0.0d);
    private static final Float     FLOAT_ZERO   = Float.valueOf(0.0f);
    private static final Integer   INTEGER_ZERO = Integer.valueOf(0);
    private static final Long      LONG_ZERO    = Long.valueOf(0);
    private static final Short     SHORT_ZERO   = Short.valueOf((short) 0);

    private StringUtil() {
    }

    /**
     * Splits the given string on the given token. Follows the semantics
     * of the Java 1.4 {@link String#split(String, int)} method, but does
     * not treat the given token as a regular expression.
     */
    public static String[] split(String str, String token, int max) {
        if (str == null || str.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException("token: [" + token + "]");
        }

        // split on token
        List<String> ret = new ArrayList<>();
        int start = 0;
        int len = str.length();
        int tlen = token.length();

        int pos = 0;
        while (pos != -1) {
            pos = str.indexOf(token, start);
            if (pos != -1) {
                ret.add(str.substring(start, pos));
                start = pos + tlen;
            }
        }
        if (start < len) {
            ret.add(str.substring(start));
        }
        else if (start == len) {
            ret.add("");
        }


        // now take max into account; this isn't the most efficient way
        // of doing things since we split the maximum number of times
        // regardless of the given parameters, but it makes things easy
        if (max == 0) {
            int size = ret.size();
            // discard any trailing empty splits
            while (ret.get(--size).isEmpty()) {
                ret.remove(size);
            }
        }
        else if (max > 0 && ret.size() > max) {
            // move all splits over max into the last split
            StringBuilder sb = new StringBuilder(256);
            sb.append(ret.get(max - 1));
            ret.remove(max - 1);
            while (ret.size() >= max) {
                sb.append(token).append(ret.get(max - 1));
                ret.remove(max - 1);
            }
            ret.add(sb.toString());
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Replace all instances of <code>from</code> in <code>str</code>
     * with <code>to</code>.
     *
     * @param str  the candidate string to replace
     * @param from the token to replace
     * @param to   the new token
     * @return the string with all the replacements made
     */
    public static String replace(String str, String from, String to) {
        if (from.equals(to)) {
            return str;
        }
        String[] split = split(str, from, Integer.MAX_VALUE);
        return StringUtils.join(split, to);
    }

    /**
     * Parse the given
     *
     * @param val  value to parse
     * @param type the target type of the the parsed value
     * @return the converted value
     */
    public static <T> T parse(String val, Class<T> type) {
        if (type == null) {
            throw new NullPointerException("target type must not be null");
        }

        // handle primitives
        if (type == byte.class) {
            return (T) (val == null ? BYTE_ZERO : Byte.valueOf(val));
        }
        if (type == char.class) {
            return (T) (val == null ? CHAR_ZERO : parseCharString(val));
        }
        if (type == double.class) {
            return (T) (val == null ? DOUBLE_ZERO : Double.valueOf(val));
        }
        if (type == float.class) {
            return (T) (val == null ? FLOAT_ZERO : Float.valueOf(val));
        }
        if (type == int.class) {
            return (T) (val == null ? INTEGER_ZERO : Integer.valueOf(val));
        }
        if (type == long.class) {
            return (T) (val == null ? LONG_ZERO : Long.valueOf(val));
        }
        if (type == short.class) {
            return (T) (val == null ? SHORT_ZERO : Short.valueOf(val));
        }
        if (type == boolean.class) {
            return (T) (val == null ? Boolean.FALSE : Boolean.valueOf(val));
        }
        if (type == void.class) {
            throw new IllegalStateException("Cannot parse void type");
        }

        // handle wrapper types
        if (type == Byte.class) {
            return (T) (val == null ? null : Byte.valueOf(val));
        }
        if (type == Character.class) {
            return (T) (val == null ? null : parseCharString(val));
        }
        if (type == Double.class) {
            return (T) (val == null ? null : Double.valueOf(val));
        }
        if (type == Float.class) {
            return (T) (val == null ? null : Float.valueOf(val));
        }
        if (type == Integer.class) {
            return (T) (val == null ? null : Integer.valueOf(val));
        }
        if (type == Long.class) {
            return (T) (val == null ? null : Long.valueOf(val));
        }
        if (type == Short.class) {
            return (T) (val == null ? null : Short.valueOf(val));
        }
        if (type == Boolean.class) {
            return (T) (val == null ? null : Boolean.valueOf(val));
        }

        throw new IllegalArgumentException("Unsupported type: " + type.getCanonicalName());
    }

    private static Character parseCharString(String val) {
        if (val.length() ==  0) {
            return Character.valueOf((char) 0);
        }
        if (val.length() ==  1) {
            return val.charAt(0);
        }
        throw new IllegalArgumentException("'" + val + "' is longer than one character.");
    }
}

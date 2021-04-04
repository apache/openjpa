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

import java.util.ArrayList;
import java.util.List;



public final class StringUtil {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];


    private static final Byte      BYTE_ZERO    = (byte) 0;
    private static final Character CHAR_ZERO    = (char) 0;
    private static final Double    DOUBLE_ZERO  = 0.0d;
    private static final Float     FLOAT_ZERO   = 0.0f;
    private static final Integer   INTEGER_ZERO = 0;
    private static final Long      LONG_ZERO    = 0L;
    private static final Short     SHORT_ZERO   = (short) 0;

    private StringUtil() {
    }

    /**
     * @return {@code true} if the given string is null or empty.
     */
    public static boolean isEmpty(String val) {
        return val == null || val.isEmpty();
    }

    public static boolean isNotEmpty(String val) {
        return !isEmpty(val);
    }

    /**
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * Ported over from Apache commons-lang3
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * Ported over from Apache commons-lang3
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }


    /**
     * @param val the string to search in
     * @param charToSearchFor the character to search for
     * @return {@code true} if the charToSearchFor is contained in the String val
     */
    public static boolean contains(String val, char charToSearchFor) {
        return val != null && val.indexOf(charToSearchFor) > -1;
    }


    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2;
        }
        else if (str1 == str2) {
            return true;
        }

        return str1.equalsIgnoreCase(str2);
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
        return join(split, to);
    }


    /**
     * Null-safe {@link String#trim()}
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * @return the trimmed string str or {@code null} if the trimmed string would be empty.
     */
    public static String trimToNull(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        str = str.trim();
        if (str.isEmpty()) {
            return null;
        }
        return str;
    }

    public static String join(Object[] values, String joinToken) {
        if (values == null) {
            return null;
        }
        if (values.length == 0) {
            return "";
        }
        if (values.length == 1) {
            return values[0].toString();
        }
        if (joinToken == null) {
            joinToken = "null"; // backward compat with commons-lang StringUtils...
        }

        StringBuilder sb = new StringBuilder(values.length * (16 + joinToken.length()));
        sb.append(values[0]);
        for (int i = 1; i < values.length; i++) {
            sb.append(joinToken).append(values[i]);
        }
        return sb.toString();
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

    /**
     * <p>Capitalizes a String changing the first letter to title case as
     * per {@link Character#toTitleCase(char)}. No other letters are changed.</p>
     *
     *
     * <pre>
     * StringUtil.capitalize(null)  = null
     * StringUtil.capitalize("")    = ""
     * StringUtil.capitalize("cat") = "Cat"
     * StringUtil.capitalize("cAt") = "CAt"
     * </pre>
     *
     * Ported over from Apache commons-lang3
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, {@code null} if null String input
     * @see #uncapitalize(String)
     */
    public static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            // already capitalized
            return str;
        }

        return new StringBuilder(strLen)
                .append(Character.toTitleCase(firstChar))
                .append(str.substring(1))
                .toString();
    }

    /**
     * <p>Uncapitalizes a String changing the first letter to title case as
     * per {@link Character#toLowerCase(char)}. No other letters are changed.</p>
     *
     * <pre>
     * StringUtil.uncapitalize(null)  = null
     * StringUtil.uncapitalize("")    = ""
     * StringUtil.uncapitalize("Cat") = "cat"
     * StringUtil.uncapitalize("CAT") = "cAT"
     * </pre>
     *
     * Ported over from Apache commons-lang3
     *
     * @param str the String to uncapitalize, may be null
     * @return the uncapitalized String, {@code null} if null String input
     * @see #capitalize(String)
     */
    public static String uncapitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final char firstChar = str.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            // already uncapitalized
            return str;
        }

        return new StringBuilder(strLen)
                .append(Character.toLowerCase(firstChar))
                .append(str.substring(1))
                .toString();
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null) {
            return str == null && suffix == null;
        }
        int strlen = str.length();
        if (suffix.length() > strlen) {
            return false;
        }

        return str.substring(str.length() - suffix.length(), strlen).equalsIgnoreCase(suffix);
    }


    /**
     * <p>Strips any of a set of characters from the end of a String.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>If the stripChars String is {@code null}, whitespace is
     * stripped as defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtils.stripEnd(null, *)          = null
     * StringUtils.stripEnd("", *)            = ""
     * StringUtils.stripEnd("abc", "")        = "abc"
     * StringUtils.stripEnd("abc", null)      = "abc"
     * StringUtils.stripEnd("  abc", null)    = "  abc"
     * StringUtils.stripEnd("abc  ", null)    = "abc"
     * StringUtils.stripEnd(" abc ", null)    = " abc"
     * StringUtils.stripEnd("  abcyx", "xyz") = "  abc"
     * StringUtils.stripEnd("120.00", ".0")   = "12"
     * </pre>
     *
     * Ported over from Apache commons-lang3
     *
     * @param str  the String to remove characters from, may be null
     * @param stripChars  the set of characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripEnd(final String str, final String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != -1) {
                end--;
            }
        }
        return str.substring(0, end);
    }



    private static Character parseCharString(String val) {
        if (val.length() ==  0) {
            return (char) 0;
        }
        if (val.length() ==  1) {
            return val.charAt(0);
        }
        throw new IllegalArgumentException("'" + val + "' is longer than one character.");
    }

}

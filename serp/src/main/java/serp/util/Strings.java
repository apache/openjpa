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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedList;

/**
 * String utiltity methods.
 *
 * @author Abe White
 */
public class Strings {

    private static final Object[][] _codes = new Object[][]{
        { byte.class, "byte", "B" }, { char.class, "char", "C" },
        { double.class, "double", "D" }, { float.class, "float", "F" },
        { int.class, "int", "I" }, { long.class, "long", "J" },
        { short.class, "short", "S" }, { boolean.class, "boolean", "Z" },
        { void.class, "void", "V" }
    };

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
        String[] split = split(str, from, Integer.MAX_VALUE);
        return join(split, to);
    }

    /**
     * Splits the given string on the given token. Follows the semantics
     * of the Java 1.4 {@link String#split(String,int)} method, but does
     * not treat the given token as a regular expression.
     */
    public static String[] split(String str, String token, int max) {
        if (str == null || str.length() == 0)
            return new String[0];
        if (token == null || token.length() == 0)
            throw new IllegalArgumentException("token: [" + token + "]");

        // split on token
        LinkedList ret = new LinkedList();
        int start = 0;
        for (int split = 0; split != -1;) {
            split = str.indexOf(token, start);
            if (split == -1 && start >= str.length())
                ret.add("");
            else if (split == -1)
                ret.add(str.substring(start));
            else {
                ret.add(str.substring(start, split));
                start = split + token.length();
            }
        }

        // now take max into account; this isn't the most efficient way
        // of doing things since we split the maximum number of times
        // regardless of the given parameters, but it makes things easy
        if (max == 0) {
            // discard any trailing empty splits
            while (ret.getLast().equals(""))
                ret.removeLast();
        } else if (max > 0 && ret.size() > max) {
            // move all splits over max into the last split
            StringBuffer buf = new StringBuffer(ret.removeLast().toString());
            while (ret.size() >= max) {
                buf.insert(0, token);
                buf.insert(0, ret.removeLast());
            }
            ret.add(buf.toString());
        }

        return (String[]) ret.toArray(new String[ret.size()]);
    }

    /**
     * Joins the given strings, placing the given token between them.
     */
    public static String join(Object[] strings, String token) {
        if (strings == null)
            return null;

        StringBuffer buf = new StringBuffer(20 * strings.length);
        for (int i = 0; i < strings.length; i++) {
            if (i > 0)
                buf.append(token);
            if (strings[i] != null)
                buf.append(strings[i]);
        }
        return buf.toString();
    }

    /**
     * Return the class for the given string, correctly handling
     * primitive types. If the given class loader is null, the context
     * loader of the current thread will be used.
     *
     * @throws RuntimeException on load error
     */
    public static Class toClass(String str, ClassLoader loader) {
        return toClass(str, false, loader);
    }

    /**
     * Return the class for the given string, correctly handling
     * primitive types. If the given class loader is null, the context
     * loader of the current thread will be used.
     *
     * @throws RuntimeException on load error
     */
    public static Class toClass(String str, boolean resolve,
        ClassLoader loader) {
        if (str == null)
            throw new NullPointerException("str == null");

        // array handling
        int dims = 0;
        while (str.endsWith("[]")) {
            dims++;
            str = str.substring(0, str.length() - 2);
        }

        // check against primitive types
        boolean primitive = false;
        if (str.indexOf('.') == -1) {
            for (int i = 0; !primitive && i < _codes.length; i++) {
                if (_codes[i][1].equals(str)) {
                    if (dims == 0)
                        return (Class) _codes[i][0];
                    str = (String) _codes[i][2];
                    primitive = true;
                }
            }
        }

        if (dims > 0) {
            int size = str.length() + dims;
            if (!primitive)
                size += 2;
            StringBuffer buf = new StringBuffer(size);
            for (int i = 0; i < dims; i++)
                buf.append('[');
            if (!primitive)
                buf.append('L');
            buf.append(str);
            if (!primitive)
                buf.append(';');
            str = buf.toString();
        }

        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();

        try {
            return Class.forName(str, resolve, loader);
        } catch (Throwable t) {
            throw new IllegalArgumentException(t.toString());
        }
    }

    /**
     * Return only the class name, without package.
     */
    public static String getClassName(Class cls) {
        return (cls == null) ? null : getClassName(cls.getName());
    }

    /**
     * Return only the class name.
     */
    public static String getClassName(String fullName) {
        if (fullName == null)
            return null;

        // special case for arrays
        int dims = 0;
        for (int i = 0; i < fullName.length(); i++) {
            if (fullName.charAt(i) != '[') {
                dims = i;
                break;
            }
        }
        if (dims > 0)
            fullName = fullName.substring(dims);

        // check for primitives
        for (int i = 0; i < _codes.length; i++) {
            if (_codes[i][2].equals(fullName)) {
                fullName = (String) _codes[i][1];
                break;
            }
        }

        fullName = fullName.substring(fullName.lastIndexOf('.') + 1);
        for (int i = 0; i < dims; i++)
            fullName = fullName + "[]";
        return fullName;
    }

    /**
     * Return only the package, or empty string if none.
     */
    public static String getPackageName(Class cls) {
        return (cls == null) ? null : getPackageName(cls.getName());
    }

    /**
     * Return only the package, or empty string if none.
     */
    public static String getPackageName(String fullName) {
        if (fullName == null)
            return null;

        int dotIdx = fullName.lastIndexOf('.');
        return (dotIdx == -1) ? "" : fullName.substring(0, dotIdx);
    }

    /**
     * Return <code>val</code> as the type specified by
     * <code>type</code>. If <code>type</code> is a primitive, the
     * primitive wrapper type is created and returned, and
     * <code>null</code>s are converted to the Java default for the
     * primitive type.
     *
     * @param val  The string value to parse
     * @param type The type to parse. This must be a primitive or a
     *             primitive wrapper, or one of {@link BigDecimal},
     *             {@link BigInteger}, {@link String}, {@link Date}.
     * @throws IllegalArgumentException if <code>type</code> is not a
     *                                  supported type, or if <code>val</code> cannot be
     *                                  converted into an instance of type <code>type</code>.
     */
    public static Object parse(String val, Class type) {
        if (!canParse(type))
            throw new IllegalArgumentException("invalid type: "
                + type.getName());

        // deal with null value
        if (val == null) {
            if (!type.isPrimitive())
                return null;
            if (type == boolean.class)
                return Boolean.FALSE;
            if (type == byte.class)
                return new Byte((byte) 0);
            if (type == char.class)
                return new Character((char) 0);
            if (type == double.class)
                return new Double(0);
            if (type == float.class)
                return new Float(0);
            if (type == int.class)
                return Numbers.valueOf(0);
            if (type == long.class)
                return Numbers.valueOf(0L);
            if (type == short.class)
                return new Short((short) 0);
            throw new IllegalStateException("invalid type: " + type);
        }

        // deal with non-null value
        if (type == boolean.class || type == Boolean.class)
            return Boolean.valueOf(val);
        if (type == byte.class || type == Byte.class)
            return Byte.valueOf(val);
        if (type == char.class || type == Character.class) {
            if (val.length() == 0)
                return new Character((char) 0);
            if (val.length() == 1)
                return new Character(val.charAt(0));
            throw new IllegalArgumentException("'" + val + "' is longer than "
                + "one character.");
        }
        if (type == double.class || type == Double.class)
            return Double.valueOf(val);
        if (type == float.class || type == Float.class)
            return Float.valueOf(val);
        if (type == int.class || type == Integer.class)
            return Integer.valueOf(val);
        if (type == long.class || type == Long.class)
            return Long.valueOf(val);
        if (type == short.class || type == Short.class)
            return Short.valueOf(val);
        if (type == String.class)
            return val;
        if (type == Date.class)
            return new Date(val);
        if (type == BigInteger.class)
            return new BigInteger(val);
        if (type == BigDecimal.class)
            return new BigDecimal(val);
        throw new IllegalArgumentException("Invalid type: " + type);
    }

    /**
     * Whether the given type is parsable via {@link #parse}.
     */
    public static boolean canParse(Class type) {
        return type.isPrimitive()
            || type == Boolean.class || type == Byte.class
            || type == Character.class || type == Short.class
            || type == Integer.class || type == Long.class
            || type == Float.class || type == Double.class
            || type == String.class || type == Date.class
            || type == BigInteger.class || type == BigDecimal.class;
    }
}

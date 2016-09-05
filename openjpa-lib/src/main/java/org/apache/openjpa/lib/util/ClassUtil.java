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


/**
 * Various helper methods to deal with Classes
 */
public final class ClassUtil {


    private static final Object[][] _codes = new Object[][]{
            {byte.class, "byte", "B"},
            {char.class, "char", "C"},
            {double.class, "double", "D"},
            {float.class, "float", "F"},
            {int.class, "int", "I"},
            {long.class, "long", "J"},
            {short.class, "short", "S"},
            {boolean.class, "boolean", "Z"},
            {void.class, "void", "V"}
    };

    private ClassUtil() {
    }

    /**
     * Return the class for the given string, correctly handling
     * primitive types. If the given class loader is null, the context
     * loader of the current thread will be used.
     *
     * @throws RuntimeException on load error
     * @author Abe White, taken from the Serp project
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
     * @author Abe White, taken from the Serp project
     */
    public static Class toClass(String str, boolean resolve,
                                ClassLoader loader) {
        if (str == null) {
            throw new NullPointerException("str == null");
        }

        // array handling
        int dims = 0;
        while (str.endsWith("[]")) {
            dims++;
            str = str.substring(0, str.length() - 2);
        }

        // check against primitive types
        boolean primitive = false;
        if (str.indexOf('.') == -1) {
            for (int i = 0; !primitive && (i < _codes.length); i++) {
                if (_codes[i][1].equals(str)) {
                    if (dims == 0) {
                        return (Class) _codes[i][0];
                    }
                    str = (String) _codes[i][2];
                    primitive = true;
                }
            }
        }

        if (dims > 0) {
            StringBuilder buf = new StringBuilder(str.length() + dims + 2);
            for (int i = 0; i < dims; i++) {
                buf.append('[');
            }
            if (!primitive) {
                buf.append('L');
            }
            buf.append(str);
            if (!primitive) {
                buf.append(';');
            }
            str = buf.toString();
        }

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        try {
            return Class.forName(str, resolve, loader);
        }
        catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Return only the class name, without package.
     */
    public static String getClassName(Class cls) {
        if (cls == null) {
            return null;
        }
        return getClassName(cls.getName());
    }

    /**
     * Return only the class name.
     */
    public static String getClassName(String fullName) {
        if (fullName == null) {
            return null;
        }
        if (fullName.isEmpty()) {
            return fullName;
        }

        int dims = 0;
        while (fullName.charAt(dims) == '[') {
            dims++;
        }
        if (dims > 0) {
            if (fullName.length() == dims + 1) {
                String classCode = fullName.substring(dims);
                for (int i = 0; i < _codes.length; i++) {
                    if (_codes[i][2].equals(classCode)) {
                        fullName = (String)_codes[i][1];
                        break;
                    }
                }
            }
            else {
                if (fullName.charAt(fullName.length()-1) == ';') {
                    fullName = fullName.substring(dims + 1, fullName.length() - 1);
                }
                else {
                    fullName = fullName.substring(dims + 1);
                }
            }
        }

        int lastDot = fullName.lastIndexOf('.');
        String simpleName = lastDot > -1 ? fullName.substring(lastDot + 1) : fullName;

        if (dims > 0) {
            StringBuilder sb = new StringBuilder(simpleName.length() + dims * 2);
            sb.append(simpleName);
            for (int i = 0; i < dims; i++) {
                sb.append("[]");
            }
            simpleName = sb.toString();
        }
        return simpleName;
    }
}

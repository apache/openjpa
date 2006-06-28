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
package org.apache.openjpa.lib.util;

import java.io.*;

import java.net.*;

import java.util.*;


/**
 * <p>Utility classes to locate services, as defined in the <a
 * href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html
 * #Service%20Provider">Jar File Specification</a>. Most of the methods in this
 * class can also be found in the <em>sun.misc.Service</em> class, but since
 * it is undocumented, we cannot rely on its API.</p>
 *
 * <p>Service location for a specified interface is done by searching for the
 * resource <em>/META-INF/services/</em><i>service.class.name</i>, and
 * loading the resource.</p>
 *
 * <p>Methods in this class that do not declare exceptions will never throw
 * Runtime exceptions: exceptions are silently swallowed and empty array values
 * are returned.</p>
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class Services {
    private static final String PREFIX = "META-INF/services/";

    /**
     * Return an array of Strings of class names of all known service
     * implementors of the specified interface or class.
     */
    public static String[] getImplementors(Class serviceClass) {
        return getImplementors(serviceClass, null);
    }

    /**
     * Return an array of Strings of class names of all known service
     * implementors of the specified interface or class.
     */
    public static String[] getImplementors(Class serviceClass,
        ClassLoader loader) {
        return getImplementors(serviceClass.getName(), loader);
    }

    /**
     * Return an array of Strings of class names of all known service
     * implementors of the specified class name (as resolved by the current
     * thread's context class loader).
     */
    public static String[] getImplementors(String serviceName) {
        return getImplementors(serviceName, null);
    }

    /**
     * Return an array of Strings of class names of all known service
     * implementors of the specified class name, as resolved by the specified
     * {@link ClassLoader}.
     */
    public static String[] getImplementors(String serviceName,
        ClassLoader loader) {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        try {
            Set resourceList = new TreeSet();
            Enumeration resources = loader.getResources(PREFIX + serviceName);

            while (resources.hasMoreElements())
                addResources((URL) resources.nextElement(), resourceList);

            return (String[]) resourceList.toArray(new String[resourceList.size()]);
        } catch (Exception e) {
            // silently swallow all exceptions.
            return new String[0];
        }
    }

    /**
     * Parse the URL resource and add the listed class names to the specified
     * Set. Class names are separated by lines. Lines starting with '#' are
     * ignored.
     */
    private static void addResources(URL url, Set set)
        throws IOException {
        InputStream in = url.openConnection().getInputStream();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("#") || (line.trim().length() == 0)) {
                    continue;
                }

                StringTokenizer tok = new StringTokenizer(line, "# \t");

                if (tok.hasMoreTokens()) {
                    String next = tok.nextToken();

                    if (next != null) {
                        next = next.trim();

                        if ((next.length() > 0) && !next.startsWith("#")) {
                            set.add(next);
                        }
                    }
                }
            }
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // silently consume exception
            }
        }
    }

    public static Class[] getImplementorClasses(Class serviceClass) {
        return getImplementorClasses(serviceClass.getName(), null);
    }

    public static Class[] getImplementorClasses(Class serviceClass,
        ClassLoader loader) {
        return getImplementorClasses(serviceClass.getName(), loader);
    }

    /**
     * Return an array of Class objects of all known service implementors of the
     * specified class name (as resolved by the current thread's context class
     * loader).
     */
    public static Class[] getImplementorClasses(String serviceName) {
        return getImplementorClasses(serviceName, null);
    }

    public static Class[] getImplementorClasses(String serviceName,
        ClassLoader loader) {
        try {
            return getImplementorClasses(serviceName, loader, true);
        } catch (Exception cnfe) {
            // this will never happen with skipmissing
            return new Class[0];
        }
    }

    /**
     * Return an array of Class objects of all known service implementors of the
     * specified class name, as resolved by the specified {@link ClassLoader}.
     *
     * @param skipMissing if true, then ignore classes that cannot be loaded by
     * the classloader; otherwise, resolution failures will throw a
     * {@link ClassNotFoundException}.
     */
    public static Class[] getImplementorClasses(String serviceName,
        ClassLoader loader, boolean skipMissing) throws ClassNotFoundException {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        String[] names = getImplementors(serviceName, loader);

        if (names == null) {
            return new Class[0];
        }

        List classes = new ArrayList(names.length);

        for (int i = 0; i < names.length; i++) {
            try {
                classes.add(Class.forName(names[i], false, loader));
            } catch (ClassNotFoundException e) {
                if (!skipMissing) {
                    throw e;
                }
            } catch (UnsupportedClassVersionError ecve) {
                if (!skipMissing) {
                    throw ecve;
                }
            } catch (LinkageError le) {
                if (!skipMissing) {
                    throw le;
                }
            }
        }

        return (Class[]) classes.toArray(new Class[classes.size()]);
    }
}

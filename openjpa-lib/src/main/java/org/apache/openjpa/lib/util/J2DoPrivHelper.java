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
package org.apache.openjpa.lib.util;

import java.lang.reflect.Modifier;

/**
 * Helper class to obtain the Privilege(Exception)Action object to perform
 * Java 2 doPrivilege security sensitive function call in the following
 * methods:
 * <ul>
 * <li>AccessibleObject.setAccessible
 * <li>Class.forName
 * <li>Class.getClassLoader
 * <li>Class.getDeclaredField
 * <li>Class.getDeclaredFields
 * <li>Class.getDeclaredMethod
 * <li>Class.getDeclaredMethods
 * <li>Class.getProtectionDomain
 * <li>Class.getResource
 * <li>Class.newInstance
 * <li>ClassLoader.getParent
 * <li>ClassLoader.getResource
 * <li>ClassLoader.getResources
 * <li>ClassLoader.getSystemClassLoader
 * <li>File.deleteOnExit
 * <li>File.delete
 * <li>File.exists
 * <li>File.getAbsoluteFile
 * <li>File.getAbsolutePath
 * <li>File.getCanonicalPath
 * <li>File.listFiles
 * <li>File.length
 * <li>File.isDirectory
 * <li>File.mkdirs
 * <li>File.renameTo
 * <li>File.toURL
 * <li>FileInputStream new
 * <li>FileOutputStream new
 * <li>System.getProperties
 * <li>InetAddress.getByName
 * <li>MultiClassLoader new
 * <li>ServerSocket new
 * <li>Socket new
 * <li>Socket.accept
 * <li>System.getProperty
 * <li>Thread.getContextClassLoader
 * <li>Thread.setContextClassLoader
 * <li>Thread new
 * <li>TemporaryClassLoader new
 * <li>URL.openStream
 * <li>URLConnection.getContent
 * <li>ZipFile new
 * <li>AnnotatedElement.getAnnotations
 * <li>AnnotatedElement.getDeclaredAnnotations
 * <li>AnnotatedElement.isAnnotationPresent
 * <li>jakarta.validation.Validator.validate
 * <li>jakarta.validation.Validation.buildDefaultValidatorFactory
 * </ul>
 *
 * If these methods are used, the following sample usage patterns should be
 * followed to ensure proper privilege is granted:
 * <pre>
 * 1) No security risk method call. E.g.
 *
 *    private static final String SEP = J2DoPrivHelper.getLineSeparator();
 *
 * 2) Methods with no exception thrown. PrivilegedAction is returned from
 *    J2DoPrivHelper.*Action(). E.g.
 *
 *    ClassLoader loader = AccessController.doPrivileged(
 *                             J2DoPrivHelper.getClassLoaderAction(clazz));
 *
 *    ClassLoader loader = (ClassLoader) (System.getSecurityManager() == null)
 *                         ? clazz.getClassLoader()
 *                         : AccessController.doPrivileged(
 *                             J2DoPrivHelper.getClassLoaderAction(clazz));
 * 3) Methods with exception thrown. PrivilegedExceptionAction is returned
 *    from J2DoPrivHelper.*Action(). E.g.
 *
 *    try {
 *      method = AccessController.doPrivileged(
 *        J2DoPrivHelper.getDeclaredMethodAction(clazz, name, parameterType));
 *    } catch (PrivilegedActionException pae) {
 *      throw (NoSuchMethodException) pae.getException();
 *    }
 *
 *    try {
 *      method = (System.getSecurityManager() == null)
 *        ? clazz.getDeclaredMethod(name,parameterType)
 *        : AccessController.doPrivileged(
 *            J2DoPrivHelper.getDeclaredMethodAction(
 *              clazz, name, parameterType));
 *    } catch (PrivilegedActionException pae) {
 *        throw (NoSuchMethodException) pae.getException()
 *    }
 * </pre>
 * @author Albert Lee
 */

public abstract class J2DoPrivHelper {
	
    private static String lineSeparator = null;
    private static String pathSeparator = null;

    /**
     * Return the value of the "line.separator" system property.
     *
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     */
    public static String getLineSeparator() {
        if (lineSeparator == null) {
            lineSeparator = System.getProperty("line.separator");
        }
        return lineSeparator;
    }

    /**
     * Return the value of the "path.separator" system property.
     *
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     */
    public static String getPathSeparator() {
        if (pathSeparator == null) {
            pathSeparator = System.getProperty("path.separator");
        }
        return pathSeparator;
    }

    /**
     * Return a new object for clazz.newInstance().
     *
     *
     * @return A new instance of the provided class.
     * @exception IllegalAccessException
     * @exception InstantiationException
     */
    public static <T> T newInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException {
    	try {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
        		return clazz.getDeclaredConstructor().newInstance();
            } else {
                return (T) clazz.getMethod("newInstance", new Class[]{}).invoke(null, new Object[]{});
            }
    	} catch (Throwable t) {
    		throw new InstantiationException(t.toString());
    	}
    }

    /**
     * Returns a new daemon Thread.
     *
     * @return Thread
     */
    public static Thread newDeamonThread(final Runnable target, final String name) {
    	Thread thread = new Thread(target, name);
    	thread.setDaemon(true);
    	return thread;
    }

}

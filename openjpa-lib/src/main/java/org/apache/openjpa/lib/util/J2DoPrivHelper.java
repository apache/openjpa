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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import serp.bytecode.BCClass;
import serp.bytecode.Code;

/**
 * Helper class to obtain the Privilege(Exception)Action object to perform
 * Java 2 doPrivilege security sensitive function call in the following
 * methods:
 * <ul>
 * <li>Class.getClassLoader
 * <li>Class.getDeclaredField
 * <li>Class.getDeclaredFields
 * <li>Class.getDeclaredMethod
 * <li>Class.getDeclaredMethods
 * <li>Class.getResource
 * <li>Class.newInstance
 * <li>ClassLoader.getParent
 * <li>ClassLoader.getResource
 * <li>ClassLoader.getResources
 * <li>ClassLoader.getSystemClassLoader
 * <li>File.exists
 * <li>File.getAbsolutePath
 * <li>File.getCanonicalPath
 * <li>File.length
 * <li>File.mkdirs
 * <li>File.renameTo
 * <li>FileInputStream new
 * <li>FileOutputStream new
 * <li>System.getProperties
 * <li>System.getProperty
 * <li>Thread.getContextClassLoader
 * <li>URL.openStream
 * <li>URLConnection.getContent
 * <li>serp.bytecode.Code new
 * <li>serp.bytecode.BCClass.isInstanceOf
 * </ul>
 * 
 * If these methods are used, the following sample usage patterns should be
 * followed to ensure proper privilege is granted:
 * <xmp>
 * 1) No security risk method call. E.g.
 *  
 *    private static final String SEP = J2DoPrivHelper.getLineSeparator();
 * 
 * 2) Methods with no exception thrown. PrivilegedAction is returned from
 *    J2DoPrivHelper.*Action(). E.g.
 *      
 *    ClassLoader loader = (ClassLoader)AccessController.doPrivileged( 
 *                             J2DoPrivHelper.getClassLoaderAction( clazz ));
 *                               
 *    ClassLoader loader = (ClassLoader) (System.getSecurityManager() == null)
 *                         ? clazz.getClassLoader()
 *                         : AccessController.doPrivileged( 
 *                             J2DoPrivHelper.getClassLoaderAction( clazz ));
 * 3) Methods with exception thrown. PrivilegedExceptionAction is returned
 *    from J2DoPrivHelper.*Action(). E.g.
 *    
 *    try {
 *      method = (Method) AccessController.doPrivileged(
 *        J2DoPrivHelper.getDeclaredMethodAction(clazz, name, parameterType));
 *    } catch( PrivilegedActionException pae ) {
 *      throw (NoSuchMethodException)pae.getException();
 *    }
 *    
 *    try {
 *      method = ( System.getSecurityManager() == null )
 *        ? clazz.getDeclaredMethod(name,parameterType)
 *        : (Method) AccessController.doPrivileged(
 *            J2DoPrivHelper.getDeclaredMethodAction(
 *              clazz, name, parameterType));
 *    } catch( PrivilegedActionException pae ) {
 *        throw (NoSuchMethodException)pae.getException()
 *    }                               
 * </xmp> 
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
    public static final String getLineSeparator() {
        if (lineSeparator == null) {
            lineSeparator = (String) AccessController
                    .doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty("line.separator");
                        }
                    });
        }
        return lineSeparator;
    }

    /**
     * Return the value of the "path.separator" system property.
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     */
    public static final String getPathSeparator() {
        if (pathSeparator == null) {
            pathSeparator = (String) AccessController
                    .doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty("path.separator");
                        }
                    });
        }
        return pathSeparator;
    }

    /**
     * Return a PrivilegeAction object for clazz.getClassloader().
     * 
     * Notes: No doPrivilege wrapping is required in the caller if:
     *     "the caller's class loader is not null and the caller's class loader
     *      is not the same as or an ancestor of the class loader for the class
     *      whose class loader is requested". E.g.
     *      
     *         this.getClass().getClassLoader();
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return Classloader
     */
    public static final PrivilegedAction getClassLoaderAction(
        final Class clazz) {
        return new PrivilegedAction() {
            public Object run() {
                return clazz.getClassLoader();
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for clazz.getDeclaredField().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *   
     * @return Field
     * @exception NoSuchFieldException
     */
    public static final PrivilegedExceptionAction getDeclaredFieldAction(
        final Class clazz, final String name) {
        return new PrivilegedExceptionAction() {
            public Object run() throws NoSuchFieldException {
                return clazz.getDeclaredField(name);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for class.getDeclaredFields().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *   
     * @return Field[]
     */
    public static final PrivilegedAction getDeclaredFieldsAction(
        final Class clazz) {
        return new PrivilegedAction() {
            public Object run() {
                return clazz.getDeclaredFields();
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for clazz.getDeclaredMethod().
     * 
     * Requires security policy
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *   
     * @return Method
     * @exception NoSuchMethodException
     */
    public static final PrivilegedExceptionAction getDeclaredMethodAction(
        final Class clazz, final String name, final Class[] parameterTypes) {
        return new PrivilegedExceptionAction() {
            public Object run() throws NoSuchMethodException {
                return clazz.getDeclaredMethod(name, parameterTypes);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for clazz.getDeclaredMethods().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *   
     * @return Method[]
     */
    public static final PrivilegedAction getDeclaredMethodsAction(
        final Class clazz) {
        return new PrivilegedAction() {
            public Object run() {
                return clazz.getDeclaredMethods();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for clazz.getResource().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *   
     * @return URL
     */
    public static final PrivilegedAction getResourceAction(
        final Class clazz, final String resource) {
        return new PrivilegedAction() {
            public Object run() {
                return clazz.getResource(resource);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for clazz.newInstance().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return Object
     * @exception IllegalAccessException
     * @exception InstantiationException
     */
    public static final PrivilegedExceptionAction newInstanceAction(
        final Class clazz) throws IllegalAccessException,
        InstantiationException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IllegalAccessException,
                    InstantiationException {
                return clazz.newInstance();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for loader.getParent().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return ClassLoader
     */
    public static final PrivilegedAction getParentAction(
        final ClassLoader loader) {
        return new PrivilegedAction() {
            public Object run() {
                return loader.getParent();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for loader.getResource().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *   
     * @return URL
     */
    public static final PrivilegedAction getResourceAction(
        final ClassLoader loader, final String resource) {
        return new PrivilegedAction() {
            public Object run() {
                return loader.getResource(resource);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for loader.getResources().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *   
     * @return Enumeration
     * @exception IOException
     */
    public static final PrivilegedExceptionAction getResourcesAction(
        final ClassLoader loader, final String resource) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return loader.getResources(resource);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for ClassLoader.getSystemClassLoader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return ClassLoader
     */
    public static final PrivilegedAction getSystemClassLoaderAction() {
        return new PrivilegedAction() {
            public Object run() {
                return ClassLoader.getSystemClassLoader();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.exists().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *   
     * @return Boolean
     */
    public static final PrivilegedAction existsAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                try {
                    return f.exists() ? Boolean.TRUE : Boolean.FALSE;
                } catch (NullPointerException npe) {
                    return Boolean.FALSE;
                }
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.getAbsolutePath().
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     *   
     * @return String
     */
    public static final PrivilegedAction getAbsolutePathAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.getAbsolutePath();
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for f.getCanonicalPath().
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     *   
     * @return String
     * @exception IOException
     */
    public static final PrivilegedExceptionAction getCanonicalPathAction(
        final File f) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return f.getCanonicalPath();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.length().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *   
     * @return Long
     */
    public static final PrivilegedAction lengthAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return new Long( f.length() );
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.mkdirs().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "write";'
     *   
     * @return Boolean
     */
    public static final PrivilegedAction mkdirsAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.mkdirs() ? Boolean.TRUE : Boolean.FALSE;
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.renameTo().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "write";'
     *   
     * @return Boolean
     */
    public static final PrivilegedAction renameToAction(final File from,
        final File to) {
        return new PrivilegedAction() {
            public Object run() {
                return from.renameTo(to) ? Boolean.TRUE : Boolean.FALSE;
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for new FileInputStream().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return FileInputStream
     * @throws FileNotFoundException
     */
    public static final PrivilegedExceptionAction newFileInputStreamAction(
        final File f) throws FileNotFoundException {
        return new PrivilegedExceptionAction() {
            public Object run() throws FileNotFoundException {
                return new FileInputStream(f);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for new FileOutputStream().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "write";'
     * 
     * @return FileOutputStream
     * @throws FileNotFoundException
     */
    public static final PrivilegedExceptionAction newFileOutputStreamAction(
        final File f) throws FileNotFoundException {
        return new PrivilegedExceptionAction() {
            public Object run() throws FileNotFoundException {
                return new FileOutputStream(f);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for new FileOutputStream().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "write";'
     * 
     * @return FileOutputStream
     * @throws FileNotFoundException
     */
    public static final PrivilegedExceptionAction newFileOutputStreamAction(
        final String f, final boolean append) throws FileNotFoundException {
        return new PrivilegedExceptionAction() {
            public Object run() throws FileNotFoundException {
                return new FileOutputStream(f, append);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for System.getProperties().
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     *   
     * @return Properties
     */
    public static final PrivilegedAction getPropertiesAction() {
        return new PrivilegedAction() {
            public Object run() {
                return System.getProperties();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for System.getProperty().
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     *   
     * @return String
     */
    public static final PrivilegedAction getPropertyAction(final String name) {
        return new PrivilegedAction() {
            public Object run() {
                return System.getProperty(name);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for Thread.currentThread
     *   .getContextClassLoader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return ClassLoader
     */
    public static final PrivilegedAction getContextClassLoaderAction() {
        return new PrivilegedAction() {
            public Object run() {
                return Thread.currentThread().getContextClassLoader();
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for url.openStream().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return InputStream
     * @throws IOException
     */
    public static final PrivilegedExceptionAction openStreamAction(
        final URL url) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return url.openStream();
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object con.getContent().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return Object
     * @throws IOException
     */
    public static final PrivilegedExceptionAction getContentAction(
        final URLConnection con) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return con.getContent();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for new serp.bytecode.Code().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return serp.bytecode.Code
     */
    public static final PrivilegedAction newCodeAction() {
        return new PrivilegedAction() {
            public Object run() {
                return new Code();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for bcClass.isInstanceOf().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return Boolean
     */
    public static final PrivilegedAction isInstanceOfAction(
        final BCClass bcClass, final Class clazz) {
        return new PrivilegedAction() {
            public Object run() {
                return bcClass.isInstanceOf(clazz) ? Boolean.TRUE
                    : Boolean.FALSE;
            }
        };
    }
}

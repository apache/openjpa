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
import java.lang.reflect.AccessibleObject;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.zip.ZipFile;

import serp.bytecode.BCClass;
import serp.bytecode.BCClassLoader;
import serp.bytecode.Code;
import serp.bytecode.FieldInstruction;
import serp.bytecode.Project;

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
 * <li>Class.getResource
 * <li>Class.newInstance
 * <li>ClassLoader.getParent
 * <li>ClassLoader.getResource
 * <li>ClassLoader.getResources
 * <li>ClassLoader.getSystemClassLoader
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
 * <li>Thread new
 * <li>TemporaryClassLoader new
 * <li>URL.openStream
 * <li>URLConnection.getContent
 * <li>ZipFile new
 * <li>serp.bytecode.Code new
 * <li>serp.bytecode.BCClassLoader new
 * <li>serp.bytecode.BCClass.write
 * <li>serp.bytecode.BCClass.getFields
 * <li>serp.bytecode.FieldInstruction.getField
 * <li>serp.bytecode.Project.loadClass
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
 *    ClassLoader loader = (ClassLoader) AccessController.doPrivileged(
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
 *      method = (Method) AccessController.doPrivileged(
 *        J2DoPrivHelper.getDeclaredMethodAction(clazz, name, parameterType));
 *    } catch (PrivilegedActionException pae) {
 *      throw (NoSuchMethodException) pae.getException();
 *    }
 *    
 *    try {
 *      method = (System.getSecurityManager() == null)
 *        ? clazz.getDeclaredMethod(name,parameterType)
 *        : (Method) AccessController.doPrivileged(
 *            J2DoPrivHelper.getDeclaredMethodAction(
 *              clazz, name, parameterType));
 *    } catch (PrivilegedActionException pae) {
 *        throw (NoSuchMethodException) pae.getException()
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
     * Return a PrivilegeAction object for aObj.setAccessible().
     * 
     * Requires security policy: 'permission java.lang.reflect.ReflectPermission
     * "suppressAccessChecks";'
     */
    public static final PrivilegedAction setAccessibleAction(
        final AccessibleObject aObj, final boolean flag) {
        return new PrivilegedAction() {
            public Object run() {
                aObj.setAccessible(flag);
                return (Object) null;
            }
        };
    }

    /**
     * Return a PrivilegeAction object for Class.forName().
     * 
     * Notes: doPriv of Class.forName call is required only if the input
     * classloader argument is null. E.g.
     * 
     * Class.forName("x", false, Collection.class.getClassLoader());
     * 
     * Requires security policy: 'permission java.lang.RuntimePermission
     * "getClassLoader";'
     * 
     * @return Classloader
     */
    public static final PrivilegedExceptionAction getForNameAction(
        final String className, final boolean initializeBoolean,
        final ClassLoader classLoader) {
        return new PrivilegedExceptionAction() {
            public Object run() throws ClassNotFoundException {
                return Class.forName(className, initializeBoolean, classLoader);
            }
        };
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
     * Return a PrivilegeAction object for f.delete().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "delete";'
     * 
     * @return Boolean
     */
    public static final PrivilegedAction deleteAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.delete() ? Boolean.TRUE : Boolean.FALSE;
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
     * Return a PrivilegeAction object for f.getAbsoluteFile().
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     * 
     * @return File
     */
    public static final PrivilegedAction getAbsoluteFileAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.getAbsoluteFile();
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
     * Return a PrivilegeAction object for f.isDirectory().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return Boolean
     */
    public static final PrivilegedAction isDirectoryAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.isDirectory() ? Boolean.TRUE : Boolean.FALSE;
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.isFile().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return Boolean
     */
    public static final PrivilegedAction isFileAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.isFile() ? Boolean.TRUE : Boolean.FALSE;
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
                return new Long(f.length());
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.listFiles().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return File[]
     */
    public static final PrivilegedAction listFilesAction(final File f) {
        return new PrivilegedAction() {
            public Object run() {
                return f.listFiles();
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
     * Return a PrivilegedExceptionAction object for f.toURL().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *   
     * @return Boolean
     * @throws MalformedURLException
     */
    public static final PrivilegedExceptionAction toURLAction(final File file)
        throws MalformedURLException {
        return new PrivilegedExceptionAction() {
            public Object run() throws MalformedURLException {
                return file.toURL();
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
     * Return a PrivilegedExceptionAction object for InetAdress.getByName().
     * 
     * Requires security policy:
     *   'permission java.net.SocketPermission "connect";'
     * 
     * @return InetAddress
     * @throws UnknownHostException
     */
    public static final PrivilegedExceptionAction getByNameAction(
        final String hostname) throws UnknownHostException {
        return new PrivilegedExceptionAction() {
            public Object run() throws UnknownHostException {
                return InetAddress.getByName(hostname);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for new Socket().
     * 
     * Requires security policy:
     *   'permission java.net.SocketPermission "connect";'
     * 
     * @return Socket
     * @throws IOException
     */
    public static final PrivilegedExceptionAction newSocketAction(
        final InetAddress host, final int port) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return new Socket(host, port);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for new ServerSocket().
     * 
     * Requires security policy:
     *   'permission java.net.SocketPermission "listen";'
     * 
     * @return ServerSocket
     * @throws IOException
     */
    public static final PrivilegedExceptionAction newServerSocketAction(
        final int port) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return new ServerSocket(port);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for ServerSocket.accept().
     * 
     * Requires security policy:
     *   'permission java.net.SocketPermission "listen";'
     * 
     * @return Socket
     * @throws IOException
     */
    public static final PrivilegedExceptionAction acceptAction(
        final ServerSocket ss) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return ss.accept();
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
     * Return a PrivilegeAction object for Thread.currentThread
     *   .setContextClassLoader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "setContextClassLoader";'
     *   
     * @return ClassLoader
     */
    public static final PrivilegedAction setContextClassLoaderAction(final ClassLoader loader) {
        return new PrivilegedAction() {
            public Object run() {
                Thread.currentThread().setContextClassLoader(loader);
                return Boolean.TRUE;
            }
        };
    } 

    /**
     * Return a PrivilegedAction object for new Thread().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "modifyThreadGroup";'
     *   'permission java.lang.RuntimePermission "modifyThread";'
     * 
     * @return Thread
     */
    public static final PrivilegedAction newDaemonThreadAction(
        final Runnable target, final String name) {
        return new PrivilegedAction() {
            public Object run() {
                Thread thread = new Thread(target, name);
                thread.setDaemon(true);
                return thread;
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
     * Return a PrivilegedExceptionAction object for new ZipFile().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     * 
     * @return ZipFile
     * @throws IOException
     */
    public static final PrivilegedExceptionAction newZipFileAction(final File f)
        throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                return new ZipFile(f);
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
     * Return a PrivilegeAction object for new TemporaryClassLoader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "createClassLoader";'
     *   
     * @return TemporaryClassLoader
     */
    public static final PrivilegedAction newTemporaryClassLoaderAction(
        final ClassLoader parent) {
        return new PrivilegedAction() {
            public Object run() {
                return new TemporaryClassLoader(parent);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for new MultiClassLoader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "createClassLoader";'
     *   
     * @return MultiClassLoader
     */
    public static final PrivilegedAction newMultiClassLoaderAction() {
        return new PrivilegedAction() {
            public Object run() {
                return new MultiClassLoader();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for new BCClassLoader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "createClassLoader";'
     *   
     * @return BCClassLoader
     */
    public static final PrivilegedAction newBCClassLoaderAction(
        final Project project, final ClassLoader parent) {
        return new PrivilegedAction() {
            public Object run() {
                return new BCClassLoader(project, parent);
            }
        };
    }

    public static final PrivilegedAction newBCClassLoaderAction(
        final Project project) {
        return new PrivilegedAction() {
            public Object run() {
                return new BCClassLoader(project);
            }
        };
    }
    
    /**
     * Return a PrivilegedExceptionAction object for BCClass.write().
     * 
     * Requires security policy:
     *   'permission java.io.FilePermission "write";'
     * 
     * @return File
     * @throws IOException
     */
    public static final PrivilegedExceptionAction bCClassWrite(
        final BCClass bc, final File f) throws IOException {
        return new PrivilegedExceptionAction() {
            public Object run() throws IOException {
                bc.write(f);
                return null;
            }
        };
    }

    /**
     * Return a PrivilegeAction object for BCClass.getFields().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return BCField
     */
    public static final PrivilegedAction getBCClassFieldsAction(
        final BCClass bcClass, final String fieldName) {
        return new PrivilegedAction() {
            public Object run() {
                return bcClass.getFields(fieldName);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for FieldInstruction.getField().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return BCField
     */
    public static final PrivilegedAction getFieldInstructionFieldAction(
        final FieldInstruction instruction) {
        return new PrivilegedAction() {
            public Object run() {
                return instruction.getField();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for Project.loadClass().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "createClassLoader";'
     *   
     * @return BCClass
     */
    public static final PrivilegedAction loadProjectClassAction(
        final Project project, final Class clazz) {
        return new PrivilegedAction() {
            public Object run() {
                return project.loadClass(clazz);
            }
        };
    }
    
    /**
     * Return a PrivilegeAction object for Project.loadClass().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return BCClass
     */
    public static final PrivilegedAction loadProjectClassAction(
        final Project project, final String clazzName) {
        return new PrivilegedAction() {
            public Object run() {
                return project.loadClass(clazzName);
            }
        };
    }
}

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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;


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
            lineSeparator =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
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
    public static String getPathSeparator() {
        if (pathSeparator == null) {
            pathSeparator =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
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
    public static PrivilegedAction<Object> setAccessibleAction(
        final AccessibleObject aObj, final boolean flag) {
        return new PrivilegedAction<Object>() {
            @Override
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
     * @return Class
     */
    public static PrivilegedExceptionAction<Class<?>> getForNameAction(
        final String className, final boolean initializeBoolean,
        final ClassLoader classLoader) {
        return new PrivilegedExceptionAction<Class<?>>() {
            @Override
            public Class<?> run() throws ClassNotFoundException {
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
    public static PrivilegedAction<ClassLoader> getClassLoaderAction(
        final Class<?> clazz) {
        return new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
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
    public static PrivilegedExceptionAction<Field> getDeclaredFieldAction(
        final Class<?> clazz, final String name) {
        return new PrivilegedExceptionAction<Field>() {
            @Override
            public Field run() throws NoSuchFieldException {
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
    public static PrivilegedAction<Field []> getDeclaredFieldsAction(
        final Class<?> clazz) {
        return new PrivilegedAction<Field []>() {
            @Override
            public Field[] run() {
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
    public static PrivilegedExceptionAction<Method>
        getDeclaredMethodAction(
            final Class<?> clazz, final String name,
            final Class<?>[] parameterTypes) {
        return new PrivilegedExceptionAction<Method>() {
            @Override
            public Method run() throws NoSuchMethodException {
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
    public static PrivilegedAction<Method []> getDeclaredMethodsAction(
        final Class<?> clazz) {
        return new PrivilegedAction<Method []>() {
            @Override
            public Method[] run() {
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
    public static PrivilegedAction<URL> getResourceAction(
        final Class<?> clazz, final String resource) {
        return new PrivilegedAction<URL>() {
            @Override
            public URL run() {
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
     * @return A new instance of the provided class.
     * @exception IllegalAccessException
     * @exception InstantiationException
     */
    public static <T> PrivilegedExceptionAction<T> newInstanceAction(
        final Class<T> clazz) throws IllegalAccessException,
        InstantiationException {
        return new PrivilegedExceptionAction<T>() {
            @Override
            public T run() throws IllegalAccessException,
                    InstantiationException {
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    return clazz.newInstance();
                } else {
                    try {
                        return (T)clazz.getMethod("newInstance",
                            new Class[]{}).invoke(null, new Object[]{});
                    } catch (Throwable t) {
                        throw new InstantiationException(t.toString());
                    }
                }
            }
        };
    }

    /**
     * Return a PrivilegeAction object for class.getProtectionDomain().
     *
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getProtectionDomain";'
     *
     * @return ProtectionDomain
     */
    public static PrivilegedAction<ProtectionDomain> getProtectionDomainAction(
        final Class<?> clazz) {
        return new PrivilegedAction<ProtectionDomain>() {
            @Override
            public ProtectionDomain run() {
                return clazz.getProtectionDomain();
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
    public static PrivilegedAction<ClassLoader> getParentAction(
        final ClassLoader loader) {
        return new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
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
    public static PrivilegedAction<URL> getResourceAction(
        final ClassLoader loader, final String resource) {
        return new PrivilegedAction<URL>() {
            @Override
            public URL run() {
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
    public static PrivilegedExceptionAction<Enumeration<URL>>
        getResourcesAction(
        final ClassLoader loader, final String resource) throws IOException {
        return new PrivilegedExceptionAction<Enumeration<URL>>() {
            @Override
            public Enumeration<URL> run() throws IOException {
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
    public static PrivilegedAction<ClassLoader>
        getSystemClassLoaderAction() {
        return new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
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
    public static PrivilegedAction<Boolean> deleteAction(final File f) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
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
    public static PrivilegedAction<Boolean> existsAction(final File f) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    return f.exists() ? Boolean.TRUE : Boolean.FALSE;
                } catch (NullPointerException npe) {
                    return Boolean.FALSE;
                }
            }
        };
    }

    /**
     * Return a PrivilegeAction object for f.deleteOnExit().
     *
     * Requires security policy:
     *   'permission java.io.FilePermission "delete";'
     */
    public static PrivilegedAction<Boolean> deleteOnExitAction(
        final File f) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                f.deleteOnExit();
                return Boolean.TRUE;
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
    public static PrivilegedAction<File> getAbsoluteFileAction(
            final File f) {
        return new PrivilegedAction<File>() {
            @Override
            public File run() {
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
    public static PrivilegedAction<String>
        getAbsolutePathAction(final File f) {
        return new PrivilegedAction<String>() {
            @Override
            public String run() {
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
    public static PrivilegedExceptionAction<String>
        getCanonicalPathAction(
        final File f) throws IOException {
        return new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws IOException {
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
    public static PrivilegedAction<Boolean>
        isDirectoryAction(final File f) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
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
    public static PrivilegedAction<Boolean> isFileAction(final File f) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
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
    public static PrivilegedAction<Long> lengthAction(final File f) {
        return new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return f.length();
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
    public static PrivilegedAction<File []>
        listFilesAction(final File f) {
        return new PrivilegedAction<File []>() {
            @Override
            public File [] run() {
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
    public static PrivilegedAction<Boolean> mkdirsAction(final File f) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
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
    public static PrivilegedAction<Boolean> renameToAction(
        final File from, final File to) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
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
     * @return URL
     * @throws MalformedURLException
     */
    public static PrivilegedExceptionAction<URL> toURLAction(
        final File file)
        throws MalformedURLException {
        return new PrivilegedExceptionAction<URL>() {
            @Override
            public URL run() throws MalformedURLException {
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
    public static PrivilegedExceptionAction<FileInputStream>
        newFileInputStreamAction(
        final File f) throws FileNotFoundException {
        return new PrivilegedExceptionAction<FileInputStream>() {
            @Override
            public FileInputStream run() throws FileNotFoundException {
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
    public static PrivilegedExceptionAction<FileOutputStream>
        newFileOutputStreamAction(
        final File f) throws FileNotFoundException {
        return new PrivilegedExceptionAction<FileOutputStream>() {
            @Override
            public FileOutputStream run() throws FileNotFoundException {
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
    public static PrivilegedExceptionAction<FileOutputStream>
        newFileOutputStreamAction(
        final String f, final boolean append) throws FileNotFoundException {
        return new PrivilegedExceptionAction<FileOutputStream>() {
            @Override
            public FileOutputStream run() throws FileNotFoundException {
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
    public static PrivilegedExceptionAction<InetAddress> getByNameAction(
        final String hostname) throws UnknownHostException {
        return new PrivilegedExceptionAction<InetAddress>() {
            @Override
            public InetAddress run() throws UnknownHostException {
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
    public static PrivilegedExceptionAction<Socket> newSocketAction(
        final InetAddress host, final int port) throws IOException {
        return new PrivilegedExceptionAction<Socket>() {
            @Override
            public Socket run() throws IOException {
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
    public static PrivilegedExceptionAction<ServerSocket>
            newServerSocketAction(
        final int port) throws IOException {
        return new PrivilegedExceptionAction<ServerSocket>() {
            @Override
            public ServerSocket run() throws IOException {
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
    public static PrivilegedExceptionAction<Socket> acceptAction(
        final ServerSocket ss) throws IOException {
        return new PrivilegedExceptionAction<Socket>() {
            @Override
            public Socket run() throws IOException {
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
    public static PrivilegedAction<Properties> getPropertiesAction() {
        return new PrivilegedAction<Properties>() {
            @Override
            public Properties run() {
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
    public static PrivilegedAction<String> getPropertyAction(
        final String name) {
        return new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name);
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
    public static PrivilegedAction<String> getPropertyAction(
        final String name, final String def) {
        return new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name, def);
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
    public static PrivilegedAction<ClassLoader>
            getContextClassLoaderAction() {
        return new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
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
    public static PrivilegedAction<Boolean>
            setContextClassLoaderAction(final ClassLoader loader) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
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
    public static PrivilegedAction<Thread> newDaemonThreadAction(
        final Runnable target, final String name) {
        return new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
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
    public static PrivilegedExceptionAction<InputStream> openStreamAction(
        final URL url) throws IOException {
        return new PrivilegedExceptionAction<InputStream>() {
            @Override
            public InputStream run() throws IOException {
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
    public static PrivilegedExceptionAction<Object> getContentAction(
        final URLConnection con) throws IOException {
        return new PrivilegedExceptionAction<Object>() {
            @Override
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
    public static PrivilegedExceptionAction<ZipFile> newZipFileAction(
        final File f)
        throws IOException {
        return new PrivilegedExceptionAction<ZipFile>() {
            @Override
            public ZipFile run() throws IOException {
                return new ZipFile(f);
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for con.getJarFile().
     *
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *
     * @return JarFile
     * @throws IOException
     */
    public static PrivilegedExceptionAction<JarFile> getJarFileAction(final JarURLConnection con)
        throws IOException {
        return new PrivilegedExceptionAction<JarFile>() {
            @Override
            public JarFile run() throws IOException {
                return con.getJarFile();
            }
        };
    }

    /**
     * Return a PrivilegedExceptionAction object for con.getJarEntry().
     *
     * Requires security policy:
     *   'permission java.io.FilePermission "read";'
     *
     * @return JarEntry
     * @throws IOException
     */
    public static PrivilegedExceptionAction<JarEntry> getJarEntryAction(final JarURLConnection con)
        throws IOException {
        return new PrivilegedExceptionAction<JarEntry>() {
            @Override
            public JarEntry run() throws IOException {
                return con.getJarEntry();
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
    public static PrivilegedAction<TemporaryClassLoader>
        newTemporaryClassLoaderAction(
        final ClassLoader parent) {
        return new PrivilegedAction<TemporaryClassLoader>() {
            @Override
            public TemporaryClassLoader run() {
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
    public static PrivilegedAction<MultiClassLoader> newMultiClassLoaderAction() {
        return new PrivilegedAction() {
            @Override
            public MultiClassLoader run() {
                return new MultiClassLoader();
            }
        };
    }


    /**
     * Return a PrivilegeAction object for AnnotatedElement.getAnnotations().
     *
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *
     * @return Annotation[]
     */
    public static PrivilegedAction<Annotation []> getAnnotationsAction(
        final AnnotatedElement element) {
        return new PrivilegedAction<Annotation []>() {
            @Override
            public Annotation [] run() {
                return element.getAnnotations();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for
     *   AnnotatedElement.getDeclaredAnnotations().
     *
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *
     * @return Annotation[]
     */
    public static PrivilegedAction<Annotation []>
        getDeclaredAnnotationsAction(
        final AnnotatedElement element) {
        return new PrivilegedAction<Annotation[]>() {
            @Override
            public Annotation [] run() {
                return element.getDeclaredAnnotations();
            }
        };
    }

    /**
     * Return a PrivilegeAction object for
     *   AnnotatedElement.isAnnotationPresent().
     *
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *
     * @return Boolean
     */
    public static PrivilegedAction<Boolean> isAnnotationPresentAction(
        final AnnotatedElement element,
        final Class<? extends Annotation> annotationClazz) {
        return new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return element.isAnnotationPresent(annotationClazz)
                    ? Boolean.TRUE : Boolean.FALSE;
            }
        };
    }

    /**
     * Return a PrivilegedAction object for
     *   AnnotatedElement.getAnnotation().
     *
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *
     * @return Annotation
     */
    public static <T extends Annotation> PrivilegedAction<T>
        getAnnotationAction(
        final AnnotatedElement element,
        final Class<T> annotationClazz) {
        return new PrivilegedAction<T>() {
            @Override
            public T run() {
                return (T) element.getAnnotation(annotationClazz);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for jakarta.validation.Validator.validate().
     *
     * Requires security policy: 'permission java.lang.RuntimePermission "accessDeclaredMemeber";'
     */
    public static <T> PrivilegedAction<Set<ConstraintViolation<T>>> validateAction(
        final Validator validator, final T arg0, final Class<?>[] groups) {
        return new PrivilegedAction<Set<ConstraintViolation<T>>>() {
            @Override
            public Set<ConstraintViolation<T>> run() {
                return validator.validate(arg0, groups);
            }
        };
    }

    /**
     * Return a PrivilegeAction object for jakarta.validation.Validation.buildDefaultValidatorFactory().
     *
     * Requires security policy: 'permission java.lang.RuntimePermission "createClassLoader";'
     */
    public static <T> PrivilegedAction<ValidatorFactory> buildDefaultValidatorFactoryAction() {
        return new PrivilegedAction<ValidatorFactory>() {
            @Override
            public ValidatorFactory run() {
                return Validation.buildDefaultValidatorFactory();
            }
        };
    }

    public static PrivilegedExceptionAction<URL> createURL(final String url) throws MalformedURLException {
        return new PrivilegedExceptionAction<URL>() {
            @Override
            public URL run() throws MalformedURLException {
                return new URL(url);
            }
        };
    }
}

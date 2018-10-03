/*
 * TestSecurityContext.java
 *
 * Created on October 13, 2006, 5:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
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
package org.apache.openjpa.persistence.kernel;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.kernel.common.apps.RuntimeTest1;
import org.apache.openjpa.persistence.kernel.common.apps.RuntimeTest2;
import org.apache.openjpa.persistence.kernel.common.apps.RuntimeTest3;

public class TestSecurityContext extends BaseKernelTest {

    private SecurityManager oldManager;

    /**
     * Creates a new instance of TestSecurityContext
     */
    public TestSecurityContext() {
    }

    public TestSecurityContext(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp(RuntimeTest1.class, RuntimeTest2.class, RuntimeTest3.class);
        oldManager = System.getSecurityManager();
        // System.setSecurityManager (ssm = new StrictSecurityManager ());
    }

    @Override
    public void tearDown()
        throws Exception {
        System.setSecurityManager(oldManager);
        oldManager = null;
        super.tearDown();
    }

    public void testInSecureClassLoader() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        pm.createExtent(RuntimeTest1.class, true).iterator().hasNext();
        endTx(pm);
        endEm(pm);
    }

    public class StrictSecurityManager
        extends SecurityManager {

        private void debug(String msg) {
            // log.debug (msg);
        }

        @Override
        public void checkAccept(String host, int port) {
            debug("checkAccept: " + host + "," + port);
            super.checkAccept(host, port);
        }

        @Override
        public void checkAccess(Thread t) {
            debug("checkAccess: " + t);
            super.checkAccess(t);
        }

        @Override
        public void checkAccess(ThreadGroup g) {
            debug("checkAccess: " + g);
            super.checkAccess(g);
        }

        @Override
        public void checkAwtEventQueueAccess() {
            debug("checkAwtEventQueueAccess");
            super.checkAwtEventQueueAccess();
        }

        @Override
        public void checkConnect(String host, int port) {
            debug("checkConnect: " + host + "," + port);
            super.checkConnect(host, port);
        }

        @Override
        public void checkConnect(String host, int port, Object context) {
            debug("checkConnect: " + host + "," + port + "," + context);
            super.checkConnect(host, port, context);
        }

        @Override
        public void checkCreateClassLoader() {
            debug("checkCreateClassLoader");
            super.checkCreateClassLoader();
        }

        @Override
        public void checkDelete(String file) {
            debug("checkDelete: " + file);
            super.checkDelete(file);
        }

        @Override
        public void checkExec(String cmd) {
            debug("checkExec: " + cmd);
            super.checkExec(cmd);
        }

        @Override
        public void checkExit(int status) {
            debug("checkExit: " + status);
            super.checkExit(status);
        }

        @Override
        public void checkLink(String lib) {
            debug("checkLink: " + lib);
            super.checkLink(lib);
        }

        @Override
        public void checkListen(int port) {
            debug("checkListen: " + port);
            super.checkListen(port);
        }

        @Override
        public void checkMemberAccess(Class clazz, int which) {
            debug("checkMemberAccess: " + clazz + "," + which);
            super.checkMemberAccess(clazz, which);
        }

        @Override
        public void checkMulticast(InetAddress maddr) {
            debug("checkMulticast: " + maddr);
            super.checkMulticast(maddr);
        }

        @Override
        public void checkMulticast(InetAddress maddr, byte ttl) {
            debug("checkMulticast: " + maddr + "," + ttl);
            super.checkMulticast(maddr, ttl);
        }

        @Override
        public void checkPackageAccess(String pkg) {
            debug("checkPackageAccess: " + pkg);
            super.checkPackageAccess(pkg);
        }

        @Override
        public void checkPackageDefinition(String pkg) {
            debug("checkPackageDefinition: " + pkg);
            super.checkPackageDefinition(pkg);
        }

        @Override
        public void checkPermission(Permission perm) {
            debug("checkPermission: " + perm);
            super.checkPermission(perm);
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            debug("checkPermission: " + perm + "," + context);
            super.checkPermission(perm, context);
        }

        @Override
        public void checkPrintJobAccess() {
            debug("checkPrintJobAccess");
            super.checkPrintJobAccess();
        }

        @Override
        public void checkPropertiesAccess() {
            debug("checkPropertiesAccess");
            super.checkPropertiesAccess();
        }

        @Override
        public void checkPropertyAccess(String key) {
            debug("checkPropertyAccess: " + key);
            super.checkPropertyAccess(key);
        }

        @Override
        public void checkRead(FileDescriptor fd) {
            debug("checkRead: " + fd);
            super.checkRead(fd);
        }

        @Override
        public void checkRead(String file) {
            debug("checkRead: " + file);
            super.checkRead(file);
        }

        @Override
        public void checkRead(String file, Object context) {
            debug("checkRead: " + file + "," + context);
            super.checkRead(file, context);
        }

        @Override
        public void checkSecurityAccess(String target) {
            debug("checkSecurityAccess: " + target);
            super.checkSecurityAccess(target);
        }

        @Override
        public void checkSetFactory() {
            debug("checkSetFactory");
            super.checkSetFactory();
        }

        @Override
        public void checkSystemClipboardAccess() {
            debug("checkSystemClipboardAccess");
            super.checkSystemClipboardAccess();
        }

        @Override
        public boolean checkTopLevelWindow(Object window) {
            debug("checkTopLevelWindow: " + window);
            return super.checkTopLevelWindow(window);
        }

        @Override
        public void checkWrite(FileDescriptor fd) {
            debug("checkWrite: " + fd);
            super.checkWrite(fd);
        }

        @Override
        public void checkWrite(String file) {
            debug("checkWrite: " + file);
            super.checkWrite(file);
        }

        @Override
        protected int classDepth(String name) {
            debug("classDepth: " + name);
            return super.classDepth(name);
        }

        @Override
        protected int classLoaderDepth() {
            debug("classLoaderDepth");
            return super.classLoaderDepth();
        }

        @Override
        protected ClassLoader currentClassLoader() {
            debug("currentClassLoader");
            return super.currentClassLoader();
        }

        @Override
        protected Class currentLoadedClass() {
            debug("currentLoadedClass");
            return super.currentLoadedClass();
        }

        @Override
        protected Class[] getClassContext() {
            debug("getClassContext");
            return super.getClassContext();
        }

        @Override
        public boolean getInCheck() {
            debug("getInCheck");
            return super.getInCheck();
        }

        @Override
        public Object getSecurityContext() {
            debug("getSecurityContext");
            return super.getSecurityContext();
        }

        @Override
        public ThreadGroup getThreadGroup() {
            debug("getThreadGroup");
            return super.getThreadGroup();
        }

        @Override
        protected boolean inClass(String name) {
            debug("inClass: " + name);
            return super.inClass(name);
        }

        @Override
        protected boolean inClassLoader() {
            debug("inClassLoader");
            return super.inClassLoader();
        }
    }
}

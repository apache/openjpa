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

import java.lang.reflect.AnnotatedElement;
import java.security.PrivilegedAction;

/**
 * Helper class to obtain the Privilege(Exception)Action object to perform
 * Java 2 doPrivilege security sensitive function call in the following
 * methods:
 * <ul>
 * <li>AnnotatedElement.getAnnotations
 * <li>AnnotatedElement.getDeclaredAnnotations
 * <li>AnnotatedElement.isAnnotationPresent
 * </ul>
 *
 * @author Albert Lee
 */

public abstract class J2DoPriv5Helper extends J2DoPrivHelper {

    /**
     * Return a PrivilegeAction object for AnnotatedElement.getAnnotations().
     *
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *
     * @return Annotation[]
     */
    public static final PrivilegedAction getAnnotationsAction(
        final AnnotatedElement element) {
        return new PrivilegedAction() {
            public Object run() {
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
    public static final PrivilegedAction getDeclaredAnnotationsAction(
        final AnnotatedElement element) {
        return new PrivilegedAction() {
            public Object run() {
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
    public static final PrivilegedAction isAnnotationPresentAction(
        final AnnotatedElement element, final Class annotationClazz) {
        return new PrivilegedAction() {
            public Object run() {
                return element.isAnnotationPresent(annotationClazz)
                    ? Boolean.TRUE : Boolean.FALSE;
            }
        };
    }
}

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
package org.apache.openjpa.util;

import java.security.AccessController;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Utility methods for managing proxies.
 *
 * @author Abe White
 */
public class Proxies {

    private static final Localizer _loc = Localizer.forPackage(Proxies.class);

    /**
     * Used by proxy types to check if the given owners and field names
     * are equivalent.
     */
    public static boolean isOwner(Proxy proxy, OpenJPAStateManager sm,
        int field) {
        return proxy.getOwner() == sm && proxy.getOwnerField() == field;
    }

    /**
     * Used by proxy types to check that an attempt to add a new value is legal.
     */
    public static void assertAllowedType(Object value, Class allowed) {
        if (value != null && allowed != null && !allowed.isInstance(value)) {
            throw new UserException(_loc.get("bad-elem-type", new Object[]{
                (ClassLoader) AccessController.doPrivileged(
                    J2DoPrivHelper.getClassLoaderAction(allowed)),
                allowed,
                (ClassLoader) AccessController.doPrivileged(
                    J2DoPrivHelper.getClassLoaderAction(value.getClass())),
                value.getClass()
            }));
        }
    }

    /**
     * Used by proxy types to dirty their owner.
     */
    public static void dirty(Proxy proxy, boolean stopTracking) {
        if (proxy.getOwner() != null)
            proxy.getOwner().dirty(proxy.getOwnerField());
        if (stopTracking && proxy.getChangeTracker() != null)
            proxy.getChangeTracker().stopTracking();
    }

    /**
     * Used by proxy types to notify collection owner on element removal.
     */
    public static void removed(Proxy proxy, Object removed, boolean key) {
        if (proxy.getOwner() != null && removed != null)
            proxy.getOwner().removed(proxy.getOwnerField(), removed, key);
    }

    /**
     * Used by proxy types to serialize non-proxy versions.
     */
    public static Object writeReplace(Proxy proxy, boolean detachable) {
        /* OPENJPA-1097 Always remove $proxy classes during serialization if detachable
            if (detachable && (proxy == null || proxy.getOwner() == null
                || proxy.getOwner().isDetached()))
                return proxy;
        */
        if (!detachable || proxy == null || proxy.getOwner() == null) {
            return proxy;
        } else {
            return proxy.copy(proxy);
        }
    }
}


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
package org.apache.openjpa.persistence;

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.openjpa.event.LifecycleCallbacks;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.event.MethodLifecycleCallbacks;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MetaDataDefaults;
import org.apache.openjpa.util.UserException;

/**
 * Common utilities for persistence metadata parsers.
 *
 * @author Abe White
 */
class MetaDataParsers {

    private static final Localizer _loc = Localizer.forPackage
        (MetaDataParsers.class);

    /**
     * Return the event type constants for the given tag, or null if none.
     */
    public static int[] getEventTypes(MetaDataTag tag) {
        switch (tag) {
            case PRE_PERSIST:
                return new int[]{ LifecycleEvent.BEFORE_PERSIST };
            case POST_PERSIST:
                return new int[]{ LifecycleEvent.AFTER_PERSIST };
            case PRE_REMOVE:
                return new int[]{ LifecycleEvent.BEFORE_DELETE };
            case POST_REMOVE:
                return new int[]{ LifecycleEvent.AFTER_DELETE };
            case PRE_UPDATE:
                return new int[]{ LifecycleEvent.BEFORE_STORE };
            case POST_UPDATE:
                return new int[]{ LifecycleEvent.AFTER_STORE };
            case POST_LOAD:
                return new int[]{ LifecycleEvent.AFTER_LOAD,
                    LifecycleEvent.AFTER_REFRESH };
            default:
                return null;
        }
    }

    /**
     * Validate that the given listener class does not have multiple methods
     * listening for the same lifecycle event, which is forbidden by the spec.  
     */
    public static void validateMethodsForSameCallback(Class cls, 
        Collection<LifecycleCallbacks> callbacks, Method method, 
        MetaDataTag tag, MetaDataDefaults def, Log log) {
        if (callbacks == null || callbacks.isEmpty())
            return;

        for (LifecycleCallbacks lc: callbacks) {
            if (!(lc instanceof MethodLifecycleCallbacks))
                continue;
            Method exists = ((MethodLifecycleCallbacks)lc).getCallbackMethod();
            if (!exists.getDeclaringClass().equals(method.getDeclaringClass()))
                continue;

            PersistenceMetaDataDefaults defaults = getPersistenceDefaults(def);
            Localizer.Message msg = _loc.get("multiple-methods-on-callback", 
                new Object[] { method.getDeclaringClass().getName(), 
                method.getName(), exists.getName(), tag.toString() });
            if (defaults == null 
                || defaults.getAllowsMultipleMethodsForSameCallback())
                log.warn(msg);
            else
                throw new UserException(msg);
        }
    }
    
    /**
     * Return the {@link PersistenceMetaDataDefaults} in use, or null if not
     * using JPA defaults.
     */
    private static PersistenceMetaDataDefaults getPersistenceDefaults
        (MetaDataDefaults def) {
        if (def instanceof PersistenceMetaDataDefaults)
            return (PersistenceMetaDataDefaults) def;
        return null;
    }
}

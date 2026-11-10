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
package org.apache.openjpa.datacache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;

class Caches {

    private static final Localizer _loc = Localizer.forPackage(Caches.class);

    /**
     * Utility to build up a set of classes from their class names
     * when operating outside the context of a persistence manager.
     * The set classes can be null, in which case a new Set will be created.
     * The names are looked up among the persistent types known to the
     * metadata repository and the persistent types already registered;
     * names of other types are ignored, and no class is loaded by name.
     */
    static Set<Class<?>> addTypesByName(OpenJPAConfiguration conf,
        Collection<String> classNames, Set<Class<?>> classes) {
        if (classNames == null || classNames.isEmpty()) {
            return classes;
        }

        MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
        ClassLoader loader = conf.getClassResolverInstance().getClassLoader(null, null);
        Log log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        for (String className : classNames) {
            Class<?> cls;
            try {
                cls = getPersistentType(repos, loader, className);
            } catch (RuntimeException e) {
                log.warn(null, e);
                continue;
            }
            if (cls == null) {
                if (log.isTraceEnabled()) {
                    log.trace(_loc.get("cache-unknown-type", className));
                }
                continue;
            }
            if (classes == null) {
                classes = new HashSet<>();
            }
            classes.add(cls);
        }
        return classes;
    }

    /**
     * Return the persistent type of the given name, or null if the name does
     * not denote a persistent type known to the repository. A type whose
     * metadata is not loaded yet is found among the registered persistent
     * types, which are loaded classes already.
     */
    private static Class<?> getPersistentType(MetaDataRepository repos, ClassLoader loader, String className) {
        ClassMetaData meta = repos.getCachedMetaData(className);
        if (meta != null) {
            return meta.getDescribedType();
        }
        for (Class<?> cls : PCRegistry.getRegisteredTypes()) {
            if (cls.getName().equals(className) && repos.getMetaData(cls, loader, false) != null) {
                return cls;
            }
        }
        return null;
    }
}

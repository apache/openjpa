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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.BlacklistClassResolver;

class Caches {

    private static final Localizer _loc = Localizer.forPackage(Caches.class);

    /**
     * Utility to build up a set of classes from their class names
     * when operating outside the context of a persistence manager.
     * The set classes can be null, in which case a new Set will be created.
     * The names are looked up among the persistent types known to the
     * metadata repository first. Any other name is resolved through the
     * configured class loader without initializing its class, and is only
     * accepted if it names an enhanced or a configured persistent type, so
     * that persistent types that have not been loaded yet are not missed.
     * Names of other types are ignored, and their classes are never
     * initialized.
     */
    static Set<Class<?>> addTypesByName(OpenJPAConfiguration conf,
        Collection<String> classNames, Set<Class<?>> classes) {
        if (classNames == null || classNames.isEmpty()) {
            return classes;
        }

        PersistentTypes types = new PersistentTypes(conf);
        Log log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        for (String className : new LinkedHashSet<>(classNames)) {
            if (className == null) {
                continue;
            }
            Class<?> cls;
            try {
                cls = types.get(className);
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
     * Looks up the persistent types of one set of names. The name lookups
     * are built once, on first use.
     */
    private static final class PersistentTypes {

        private final MetaDataRepository _repos;
        private final ClassLoader _loader;
        private Map<String, Class<?>> _known;
        private Set<String> _configured;

        PersistentTypes(OpenJPAConfiguration conf) {
            _repos = conf.getMetaDataRepositoryInstance();
            _loader = conf.getClassResolverInstance().getClassLoader(null, null);
        }

        /**
         * Return the persistent type of the given name, or null if the name
         * does not denote a persistent type of this repository.
         */
        Class<?> get(String className) {
            Class<?> cls = getKnown().get(className);
            if (cls != null) {
                return cls;
            }

            // a persistent type without metadata yet, for instance a subclass
            // that has not been loaded, whose superclass may have cached
            // query results; resolve the name through the configured loader
            // without initializing the class, as other loaders' types of the
            // same name do not belong to this repository
            cls = resolve(className, false);
            if (cls == null) {
                return null;
            }
            if (!PersistenceCapable.class.isAssignableFrom(cls) && !getConfigured().contains(className)) {
                return null;
            }
            // initializing an enhanced type registers it with the PCRegistry
            cls = resolve(className, true);
            return cls != null && hasMetaData(cls) ? cls : null;
        }

        private boolean hasMetaData(Class<?> cls) {
            return _repos.getMetaData(cls, _loader, false) != null;
        }

        private Class<?> resolve(String className, boolean initialize) {
            try {
                BlacklistClassResolver.DEFAULT.check(className);
                return Class.forName(className, initialize, _loader);
            } catch (ClassNotFoundException | LinkageError | SecurityException e) {
                return null;
            }
        }

        private Map<String, Class<?>> getKnown() {
            if (_known == null) {
                _known = new HashMap<>();
                for (ClassMetaData meta : _repos.getMetaDatas()) {
                    _known.put(meta.getDescribedType().getName(), meta.getDescribedType());
                }
            }
            return _known;
        }

        private Set<String> getConfigured() {
            if (_configured == null) {
                Set<String> names = _repos.getPersistentTypeNames(false, _loader);
                _configured = names == null ? new HashSet<>() : names;
            }
            return _configured;
        }
    }
}

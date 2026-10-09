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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.MultiClassLoader;

/**
 * Helper class to serialize and deserialize persistent objects,
 * subtituting oids into the serialized stream and subtituting the persistent
 * objects back during deserialization.
 *
 * @author Abe White
 * @since 0.3.3
 */
public class Serialization {
    private static final Localizer _loc = Localizer.forPackage(Serialization.class);

    /**
     * Serialize a value that might contain persistent objects. Replaces
     * persistent objects with their oids.
     */
    public static byte[] serialize(Object val, StoreContext ctx) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objs = new PersistentObjectOutputStream(bytes, ctx);
            objs.writeObject(val);
            objs.flush();
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new StoreException(e);
        }
    }

    /**
     * Deserialize an object value from the given bytes.
     */
    public static Object deserialize(byte[] bytes, StoreContext ctx) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return deserialize(in, ctx);
    }

    /**
     * Deserialize an object value from the given stream.
     */
    public static Object deserialize(InputStream in, StoreContext ctx) {
        try {
            return ctx == null
                ? new ClassResolvingObjectInputStream(in).readObject()
                : new PersistentObjectInputStream(in, ctx).readObject();
        } catch (Exception e) {
            throw new StoreException(e);
        }
    }

    /**
     * Object output stream that replaces persistent objects with their oids.
     */
    public static class PersistentObjectOutputStream extends ObjectOutputStream {
        private StoreContext _ctx;

        /**
         * Constructor; supply underlying stream.
         */
        public PersistentObjectOutputStream(OutputStream delegate, StoreContext ctx) throws IOException {
            super(delegate);
            _ctx = ctx;
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            Object oid = _ctx.getObjectId(obj);
            return (oid == null) ? obj : new ObjectIdMarker(oid);
        }
    }

    public static class ClassResolvingObjectInputStream extends ObjectInputStream {
        /**
         * The loaders to try, in order, resolved on first use. Resolving them in
         * the constructor is not possible: {@link PersistentObjectInputStream}
         * assigns its context after <code>super(delegate)</code> and contributes a
         * loader from it, which is not available yet at that point.
         */
        private ClassLoader[] _loaders;

        public ClassResolvingObjectInputStream(InputStream delegate) throws IOException {
            super(delegate);
        }

        @Override
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = BlacklistClassResolver.DEFAULT.check(desc.getName());
            ClassNotFoundException notFound = null;
            for (ClassLoader loader : getCandidateLoaders()) {
                try {
                    return Class.forName(name, true, loader);
                } catch (ClassNotFoundException e) {
                    if (notFound == null) {
                        notFound = e;
                    } else {
                        notFound.addSuppressed(e);
                    }
                }
            }

            // primitive types and anything the candidates could not see
            try {
                return super.resolveClass(desc);
            } catch (ClassNotFoundException e) {
                throw notFound == null ? e : notFound;
            }
        }

        private ClassLoader[] getCandidateLoaders() {
            if (_loaders == null) {
                List<ClassLoader> candidates = new ArrayList<>(3);
                addContextClassLoaders(candidates);
                candidates.add(getClass().getClassLoader());
                candidates.add(MultiClassLoader.SYSTEM_LOADER);
                _loaders = candidates.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toArray(ClassLoader[]::new);
            }
            return _loaders;
        }

        /**
         * Add the loaders to consult before the loader of this class and the
         * system loader. The order is kept, duplicates and nulls are discarded.
         */
        protected void addContextClassLoaders(List<ClassLoader> loaders) {
            loaders.add(Thread.currentThread().getContextClassLoader());
        }

        /**
         * @deprecated no longer called; override
         * {@link #addContextClassLoaders(List)} instead.
         */
        @Deprecated(forRemoval = true)
        protected void addContextClassLoaders(MultiClassLoader loader) {
            loader.addClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }

    /**
     * Object input stream that replaces oids with their objects.
     */
    public static class PersistentObjectInputStream extends ClassResolvingObjectInputStream {
        private final StoreContext _ctx;

        /**
         * Constructor; supply source stream and broker to
         * use for persistent object lookups.
         */
        public PersistentObjectInputStream(InputStream delegate, StoreContext ctx) throws IOException {
            super(delegate);
            _ctx = ctx;
            enableResolveObject(true);
        }

        @Override
        protected void addContextClassLoaders(List<ClassLoader> loaders) {
            super.addContextClassLoaders(loaders);
            loaders.add(_ctx.getClassLoader());
        }

        @Override
        protected Object resolveObject(Object obj) {
            Object oid = null;
            if (obj instanceof ObjectIdMarker marker) {
                oid = marker.oid;
            } else {
                return obj;
            }
            if (oid == null) {
                return null;
            }

            Object pc = _ctx.find(oid, null, null, null, 0);
            if (pc == null) {
                Log log = _ctx.getConfiguration().getLog(OpenJPAConfiguration.LOG_RUNTIME);
                if (log.isWarnEnabled()) {
                    log.warn(_loc.get("bad-ser-oid", oid));
                }
                if (log.isTraceEnabled()) {
                    log.trace(new ObjectNotFoundException(oid));
                }
            }
            return pc;
        }
    }

    /**
     * Marker for oids.
     */
    private static class ObjectIdMarker implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object oid;

        public ObjectIdMarker(Object oid) {
            this.oid = oid;
        }
    }
}

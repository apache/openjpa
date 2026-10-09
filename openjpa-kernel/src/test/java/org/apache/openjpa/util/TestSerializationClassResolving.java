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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

/**
 * Tests the class resolution of
 * {@link Serialization.ClassResolvingObjectInputStream}: every candidate loader
 * is consulted, the thread context loader takes precedence, and descriptors that
 * no loader can resolve by name fall back to the stream's own resolution.
 */
public class TestSerializationClassResolving {

    /**
     * A descriptor for a primitive type carries the type name, which
     * {@link Class#forName(String, boolean, ClassLoader)} rejects, so it is
     * resolvable only through {@link java.io.ObjectInputStream#resolveClass}.
     */
    @Test
    public void testPrimitiveDescriptorIsResolved() throws Exception {
        assertEquals(int.class, deserialize(serialize(int.class)));
    }

    @Test
    public void testArrayClassIsResolved() throws Exception {
        assertEquals(int[].class, deserialize(serialize(int[].class)));
        assertEquals(String[].class, deserialize(serialize(String[].class)));
    }

    @Test
    public void testArrayInstanceIsResolved() throws Exception {
        int[] values = new int[]{ 1, 2, 3 };
        assertEquals(int[].class, deserialize(serialize(values)).getClass());
    }

    /**
     * A class that only the thread context loader can see must be resolved
     * through it, not through the loader of the deserializing code.
     */
    @Test
    public void testClassOfContextLoaderWins() throws Exception {
        RedefiningClassLoader loader =
            new RedefiningClassLoader(getClass().getClassLoader());
        Class<?> redefined = loader.loadClass(Payload.class.getName());
        assertNotSame("the loader must define its own copy",
            Payload.class, redefined);

        Object payload = redefined.getConstructor().newInstance();
        byte[] bytes = serialize((Serializable) payload);

        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            Object read = deserialize(bytes);
            assertSame(redefined, read.getClass());
            assertSame(loader, read.getClass().getClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static byte[] serialize(Serializable value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(bytes)) {
            objects.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes) {
        return Serialization.deserialize(new ByteArrayInputStream(bytes), null);
    }

    /**
     * Instances of this class are serialized by a copy of it defined by
     * {@link RedefiningClassLoader}; both copies have the same bytes and
     * therefore the same serial version.
     */
    public static class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Defines the classes of this test itself instead of delegating to its
     * parent, so that a class with the same name is reachable from one loader
     * only.
     */
    private static class RedefiningClassLoader extends ClassLoader {

        RedefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
            if (!name.startsWith(TestSerializationClassResolving.class.getName())) {
                return super.loadClass(name, resolve);
            }
            Class<?> found = findLoadedClass(name);
            if (found == null) {
                found = defineClass(name, read(name), 0, read(name).length);
            }
            if (resolve) {
                resolveClass(found);
            }
            return found;
        }

        private byte[] read(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream in = getParent().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                return in.readAllBytes();
            } catch (IOException ioe) {
                throw new ClassNotFoundException(name, ioe);
            }
        }
    }
}

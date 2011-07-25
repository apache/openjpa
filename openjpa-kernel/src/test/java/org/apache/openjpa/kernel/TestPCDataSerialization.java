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
package org.apache.openjpa.kernel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.DynamicStorage;
import org.apache.openjpa.enhance.DynamicStorageGenerator;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.IntId;
import org.apache.openjpa.util.OpenJPAId;

/**
 * This test ensures that we can stream a PCData and OpenJPAId to a client which may not have the Entities on it's
 * classpath. In a real use case we would have multiple processes, but for the sake of unit testing this behavior is
 * simulated via multiple classloaders.
 */
public class TestPCDataSerialization extends TestCase {
    ClassLoader _ccl;

    @Override
    protected void setUp() throws Exception {
        _ccl = Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected void tearDown() throws Exception {
        Thread.currentThread().setContextClassLoader(_ccl);
    }

    public void test() throws Exception {
        // Generate a new class. This will create a new class, and load it with a new classloader.
        Class<?> cls = generateClass();
        ClassLoader newLoader = cls.getClassLoader();
        Thread.currentThread().setContextClassLoader(newLoader);

        // Create moc objects
        MetaDataRepository repo = new DummyMetaDataRepository();
        ClassMetaData cmd = new DummyClassMetaData(cls, repo);

        OpenJPAId oid = new IntId(cls, 7);
        PCDataImpl pcdi = new PCDataImpl(oid, cmd);

        // Write the object out using the newly created classloader
        byte[] bytes = writeObject(pcdi);

        // Switch contextclassloader back to the original and try to deserialize
        Thread.currentThread().setContextClassLoader(_ccl);

        pcdi = (PCDataImpl) readObject(bytes);
        assertNotNull(pcdi);
        try {
            // This will throw a wrapped ClassNotFoundException because the domain class isn't available.
            pcdi.getType();
            fail("Should have thrown an exception.");
        } catch (RuntimeException cnfe) {
            // expected
        }
        // Write object without the class
        bytes = writeObject(pcdi);

        // Switch to loader that has the new class and make sure we find it again.
        Thread.currentThread().setContextClassLoader(newLoader);
        pcdi = (PCDataImpl) readObject(bytes);
        assertNotNull(pcdi);
        assertEquals(cls, pcdi.getType());

    }

    private byte[] writeObject(Object o) throws Exception {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);

            return baos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
    }

    private Object readObject(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            return ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
            }
            if (bais != null) {
                bais.close();
            }
        }
    }

    private Class<?> generateClass() {
        DynamicStorageGenerator gen = new DynamicStorageGenerator();
        int[] types =
            new int[] { JavaTypes.BOOLEAN, JavaTypes.BYTE, JavaTypes.CHAR, JavaTypes.INT, JavaTypes.SHORT,
                JavaTypes.LONG, JavaTypes.FLOAT, JavaTypes.DOUBLE, JavaTypes.STRING, JavaTypes.OBJECT };
        DynamicStorage storage = gen.generateStorage(types, "org.apache.openjpa.enhance.Test");
        storage = storage.newInstance();

        return storage.getClass();
    }

    @SuppressWarnings("serial")
    class DummyClassMetaData extends ClassMetaData {
        public DummyClassMetaData(Class<?> cls, MetaDataRepository repo) {
            super(cls, repo);
        }
    }

    @SuppressWarnings("serial")
    class DummyMetaDataRepository extends MetaDataRepository implements InvocationHandler {
        OpenJPAConfiguration _conf;

        public DummyMetaDataRepository() {
            _conf =
                (OpenJPAConfiguration) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[] { OpenJPAConfiguration.class }, this);
        }

        @Override
        public OpenJPAConfiguration getConfiguration() {
            return _conf;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}

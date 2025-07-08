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
package org.apache.openjpa.enhance;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests that {@link Reflection#getDeclaredMethod(Class, String, Class)}
 * returns the most-derived class's method when called from a type hierarchy.
 * See OPENJPA-251.
 */
public class TestGetDeclaredMethod {

    @Test
    public void testGetDeclaredMethod() {
        Method meth =
            Reflection.getDeclaredMethod(Impl.class, "getObject", null);
        assertEquals(Impl.class, meth.getDeclaringClass());
        assertEquals(String.class, meth.getReturnType());
    }

    @Test
    public void testMostDerived() throws NoSuchMethodException {
        Method impl = Impl.class.getDeclaredMethod("getObject");
        Method iface = Iface.class.getDeclaredMethod("getObject");
        Method other = Other.class.getDeclaredMethod("getObject");
        assertEquals(Impl.class, Reflection.mostDerived(impl, iface)
            .getDeclaringClass());
        assertEquals(Impl.class, Reflection.mostDerived(iface, impl)
            .getDeclaringClass());
        try {
            Reflection.mostDerived(iface, other);
            fail("'iface' and 'other' are not from related types");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testGenerics() throws NoSuchMethodException {
        List<Method> meths = new ArrayList<>();
        for (Method meth : GenericsImpl.class.getDeclaredMethods()) {
            if ("getObject".equals(meth.getName()))
                meths.add(meth);
        }
        assertEquals(2, meths.size());
        assertEquals(String.class, Reflection.mostDerived(meths.get(0),
            meths.get(1)).getReturnType());
    }

    interface Iface {
        Object getObject();
    }

    static class Impl implements Iface {
        @Override
        public String getObject() {
            return "string";
        }
    }

    static class Other {
        public String getObject() {
            return "other";
        }
    }

    interface GenericsIface<T> {
        T getObject();
    }

    static class GenericsImpl implements GenericsIface {
        @Override
        public String getObject() {
            return null;
        }
    }
}

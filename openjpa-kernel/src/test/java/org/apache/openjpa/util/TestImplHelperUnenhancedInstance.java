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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.junit.Test;

/**
 * Pins the registry contract of
 * {@link ImplHelper#registerUnenhancedInstance(Object, PersistenceCapable)}.
 * <p>
 * The registrations are read back through the registry itself: resolving them
 * through {@link ImplHelper#toPersistenceCapable(Object, Object)} would need a
 * configuration and manageable metadata, which is beyond a unit test here.
 */
@SuppressWarnings("deprecation")
public class TestImplHelperUnenhancedInstance {

    record Point(int x, int y) {
    }

    /**
     * Records are the reason this registry exists, and two equal records must
     * not share one registration.
     */
    @Test
    public void testKeysByIdentityNotEquality() {
        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        assertEquals(p1, p2);

        PersistenceCapable pc1 = newPersistenceCapable();
        PersistenceCapable pc2 = newPersistenceCapable();
        ImplHelper.registerUnenhancedInstance(p1, pc1);
        ImplHelper.registerUnenhancedInstance(p2, pc2);

        assertSame(pc1, ImplHelper._unenhancedInstanceMap.get(p1));
        assertSame(pc2, ImplHelper._unenhancedInstanceMap.get(p2));
        assertNull(ImplHelper._unenhancedInstanceMap.get(new Point(3, 4)));
    }

    @Test
    public void testLastRegistrationWins() {
        Point p = new Point(5, 6);
        PersistenceCapable pcA = newPersistenceCapable();
        PersistenceCapable pcB = newPersistenceCapable();

        ImplHelper.registerUnenhancedInstance(p, pcA);
        ImplHelper.registerUnenhancedInstance(p, pcB);
        assertSame(pcB, ImplHelper._unenhancedInstanceMap.get(p));
    }

    /**
     * A do-nothing {@link PersistenceCapable} stub; no mock framework is
     * available on this module's test classpath.
     */
    private static PersistenceCapable newPersistenceCapable() {
        InvocationHandler handler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                switch (method.getName()) {
                    case "equals":
                        return proxy == args[0];
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "toString":
                        return "PersistenceCapable stub";
                    default:
                        return null;
                }
            }
        };
        return (PersistenceCapable) Proxy.newProxyInstance(
            PersistenceCapable.class.getClassLoader(),
            new Class<?>[]{ PersistenceCapable.class }, handler);
    }
}

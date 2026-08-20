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
package org.apache.openjpa.event;

import java.io.Serializable;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests that MethodLifecycleCallbacks and BeanLifecycleCallbacks
 * correctly find callback methods when parameter types are compatible
 * but not exact matches.
 *
 * Mirrors TCK issue: ListenerAA.prePersist(CallbackStatusIF) must
 * be found when searching with Object.class (default entity listener
 * with unknown entity type at XML parse time).
 */
public class TestMethodLifecycleCallbacks {

    public interface SomeInterface {
        void doSomething();
    }

    public static class ListenerWithInterfaceParam {
        public void prePersist(SomeInterface entity) {
            // callback takes interface type
        }
        public void postPersist(Object entity) {
            // callback takes Object type
        }
    }

    public static class ListenerWithExactType {
        public void prePersist(String entity) {
            // callback takes String
        }
    }

    /**
     * When searching with Object.class, should find methods that take
     * an interface type (since at runtime the actual entity will be passed).
     * This is the TCK scenario: default entity listeners defined in XML
     * where entity type is unknown, so Object.class is used.
     */
    @Test
    public void testFindMethodWithInterfaceParamWhenSearchingForObject() {
        // BeanLifecycleCallbacks passes type=Object.class for default listeners
        BeanLifecycleCallbacks cb = new BeanLifecycleCallbacks(
            ListenerWithInterfaceParam.class, "prePersist", false, Object.class);
        assertNotNull(cb.getCallbackMethod());
        assertEquals("prePersist", cb.getCallbackMethod().getName());
    }

    /**
     * When searching with Object.class, should find methods that take Object.
     */
    @Test
    public void testFindMethodWithObjectParam() {
        BeanLifecycleCallbacks cb = new BeanLifecycleCallbacks(
            ListenerWithInterfaceParam.class, "postPersist", false, Object.class);
        assertNotNull(cb.getCallbackMethod());
        assertEquals("postPersist", cb.getCallbackMethod().getName());
    }

    /**
     * When searching with a specific type, should find methods that take
     * a supertype (Object or interface).
     */
    @Test
    public void testFindMethodWithObjectParamWhenSearchingForSpecificType() {
        BeanLifecycleCallbacks cb = new BeanLifecycleCallbacks(
            ListenerWithInterfaceParam.class, "postPersist", false, String.class);
        assertNotNull(cb.getCallbackMethod());
        assertEquals("postPersist", cb.getCallbackMethod().getName());
    }

    /**
     * MethodLifecycleCallbacks (entity callback, not listener) with Object arg.
     */
    @Test
    public void testMethodLifecycleCallbacksWithObjectArg() {
        MethodLifecycleCallbacks cb = new MethodLifecycleCallbacks(
            ListenerWithInterfaceParam.class, "postPersist", true);
        assertNotNull(cb.getCallbackMethod());
        assertEquals("postPersist", cb.getCallbackMethod().getName());
    }
}

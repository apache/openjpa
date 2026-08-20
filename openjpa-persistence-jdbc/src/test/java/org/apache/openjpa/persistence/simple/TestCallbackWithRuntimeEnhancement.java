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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests entity lifecycle callbacks with runtime enhancement.
 * Verifies:
 * 1. Listener methods with interface parameter types (e.g., CallbackTracker)
 *    are properly matched — not just Object or exact entity type.
 * 2. The actual entity (not ReflectingPersistenceCapable wrapper) is passed
 *    to listener callback methods.
 *
 * Mirrors TCK callback tests (callback.listener, callback.xml).
 */
public class TestCallbackWithRuntimeEnhancement extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedCallbackEntity.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false");
    }

    /**
     * Test that prePersist and postPersist callbacks fire and
     * the entity (not a PC wrapper) is passed to the listener.
     */
    public void testPrePersistAndPostPersistCallbacks() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedCallbackEntity e = new UnenhancedCallbackEntity(1, "test");
        em.persist(e);
        em.getTransaction().commit();

        assertTrue("prePersist should have been called",
            e.getCallbackLog().contains("prePersist"));
        assertTrue("postPersist should have been called",
            e.getCallbackLog().contains("postPersist"));

        em.close();
    }

    /**
     * Test that preRemove and postRemove callbacks fire and
     * the entity (not ReflectingPersistenceCapable) is passed to the listener.
     * This specifically tests the fix for BrokerImpl.fireLifecycleEvent
     * using getManagedInstance() instead of getPersistenceCapable().
     */
    public void testPreRemoveAndPostRemoveCallbacks() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedCallbackEntity e = new UnenhancedCallbackEntity(2, "remove-test");
        em.persist(e);
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        UnenhancedCallbackEntity found = em.find(UnenhancedCallbackEntity.class, 2);
        assertNotNull(found);
        em.remove(found);
        em.getTransaction().commit();

        assertTrue("preRemove should have been called",
            found.getCallbackLog().contains("preRemove"));
        assertTrue("postRemove should have been called",
            found.getCallbackLog().contains("postRemove"));

        em.close();
    }

    /**
     * Test that listener receives the actual entity type, not a wrapper.
     * The listener casts to CallbackTracker — if a ReflectingPersistenceCapable
     * is passed, it will throw ClassCastException.
     */
    public void testListenerReceivesEntityNotWrapper() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedCallbackEntity e = new UnenhancedCallbackEntity(3, "cast-test");
        // If the listener receives ReflectingPersistenceCapable instead
        // of the entity, the cast to CallbackTracker in postPersist will fail.
        em.persist(e);
        em.getTransaction().commit();

        // If we get here without ClassCastException, the fix works
        assertEquals(2, e.getCallbackLog().size());

        em.close();
    }
}

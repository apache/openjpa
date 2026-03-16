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
package org.apache.openjpa.persistence.callbacks;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that default listeners (from persistence-unit-defaults in orm.xml)
 * are not duplicated when entity-level listeners are also defined in XML.
 *
 * Reproduces TCK listeneroverride pattern:
 * - ListenerA is a default listener (persistence-unit-defaults)
 * - ListenerB, ListenerC are entity-level listeners (XML entity-listeners)
 * - @EntityListeners annotation on entity should be overridden by XML
 * - Expected: [ListenerA, ListenerB, ListenerC] — each fires exactly once
 */
public class TestListenerOverride extends SingleEMFTestCase {

    @Override
    protected String getPersistenceUnitName() {
        return "listener-override-pu";
    }

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES);
        ListenerA.calls.clear();
    }

    public void testDefaultListenerNotDuplicated() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedListenerOverrideEntity entity = new UnenhancedListenerOverrideEntity();
        entity.setId(1);
        entity.setName("test");
        em.persist(entity);

        em.getTransaction().commit();
        em.close();

        List<String> expected = Arrays.asList("ListenerA", "ListenerB", "ListenerC");
        System.err.println("Listener calls: " + ListenerA.calls);
        assertEquals("Each listener should fire exactly once: " + ListenerA.calls,
            expected, ListenerA.calls);
    }

    /**
     * Same test but creating a second EM to verify no accumulation across EMs.
     */
    public void testDefaultListenerNotDuplicatedSecondEM() {
        // First persist
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        UnenhancedListenerOverrideEntity entity = new UnenhancedListenerOverrideEntity();
        entity.setId(10);
        entity.setName("first");
        em.persist(entity);
        em.getTransaction().commit();
        em.close();
        ListenerA.calls.clear();

        // Second persist in new EM
        em = emf.createEntityManager();
        em.getTransaction().begin();
        UnenhancedListenerOverrideEntity entity2 = new UnenhancedListenerOverrideEntity();
        entity2.setId(11);
        entity2.setName("second");
        em.persist(entity2);
        em.getTransaction().commit();
        em.close();

        List<String> expected = Arrays.asList("ListenerA", "ListenerB", "ListenerC");
        System.err.println("Listener calls (2nd EM): " + ListenerA.calls);
        assertEquals("Each listener should fire exactly once on 2nd EM: " + ListenerA.calls,
            expected, ListenerA.calls);
    }
}

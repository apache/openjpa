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
 * Tests that entities with lowercase property names after get/set prefix
 * (e.g. getdescription/setdescription) work correctly with runtime
 * enhancement (subclass-redefinition), mirroring the TCK Order entity
 * pattern.
 */
public class TestUnenhancedLowercaseGetter extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(UnenhancedLowercaseGetterOrder.class,
            CLEAR_TABLES,
            "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * Mirrors TCK core.entityManager.Client1#mergeTest:
     * merge a new entity, clear, find, assert description persisted.
     */
    public void testMergeWithLowercaseGetter() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedLowercaseGetterOrder order =
            new UnenhancedLowercaseGetterOrder(1, "merged description");
        em.merge(order);

        em.getTransaction().commit();
        em.clear();

        UnenhancedLowercaseGetterOrder found =
            em.find(UnenhancedLowercaseGetterOrder.class, 1);
        assertNotNull(found);
        assertEquals("merged description", found.getdescription());

        em.close();
    }

    /**
     * Tests persist and find with lowercase getter.
     */
    public void testPersistAndFindLowercaseGetter() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedLowercaseGetterOrder order =
            new UnenhancedLowercaseGetterOrder(2, "persisted description");
        em.persist(order);

        em.getTransaction().commit();
        em.clear();

        UnenhancedLowercaseGetterOrder found =
            em.find(UnenhancedLowercaseGetterOrder.class, 2);
        assertNotNull(found);
        assertEquals("persisted description", found.getdescription());

        em.close();
    }
}

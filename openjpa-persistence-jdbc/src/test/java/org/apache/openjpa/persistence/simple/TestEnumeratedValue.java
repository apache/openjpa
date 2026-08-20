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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA 3.2 @EnumeratedValue for custom enum-to-DB mapping.
 */
public class TestEnumeratedValue extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, EnumeratedValueEntity.class);
    }

    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueEntity e = new EnumeratedValueEntity();
        e.setId(1);
        e.setStatus(EnumeratedValueStatus.ACTIVE);
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        EnumeratedValueEntity loaded = em.find(EnumeratedValueEntity.class, 1L);
        assertNotNull(loaded);
        assertEquals(EnumeratedValueStatus.ACTIVE, loaded.getStatus());
        em.close();
    }

    public void testAllEnumValues() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < EnumeratedValueStatus.values().length; i++) {
            EnumeratedValueEntity e = new EnumeratedValueEntity();
            e.setId(10 + i);
            e.setStatus(EnumeratedValueStatus.values()[i]);
            em.persist(e);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        for (int i = 0; i < EnumeratedValueStatus.values().length; i++) {
            EnumeratedValueEntity loaded = em.find(
                EnumeratedValueEntity.class, (long) (10 + i));
            assertNotNull(loaded);
            assertEquals(EnumeratedValueStatus.values()[i], loaded.getStatus());
        }
        em.close();
    }

    public void testStoredAsCustomValue() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueEntity e = new EnumeratedValueEntity();
        e.setId(100);
        e.setStatus(EnumeratedValueStatus.INACTIVE);
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        // Verify the DB stores "I" not "INACTIVE" or "1"
        em = emf.createEntityManager();
        Query q = em.createNativeQuery(
            "SELECT STATUS FROM EnumeratedValueEntity WHERE ID = 100");
        Object result = q.getSingleResult();
        assertEquals("I", result);
        em.close();
    }
}

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
 * Tests that JPA 3.2 allows entity and embeddable classes to be
 * static inner classes.
 */
public class TestStaticInnerClasses extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, StaticInnerEntity.InnerEntity.class,
                StaticInnerEntity.InnerEmbeddable.class);
    }

    public void testPersistAndFindInnerEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        StaticInnerEntity.InnerEntity e = new StaticInnerEntity.InnerEntity();
        e.setId(1);
        e.setName("test");
        e.setAddress(new StaticInnerEntity.InnerEmbeddable("123 Main St", "Springfield"));
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        StaticInnerEntity.InnerEntity loaded = em.find(
                StaticInnerEntity.InnerEntity.class, 1L);
        assertNotNull(loaded);
        assertEquals("test", loaded.getName());
        assertNotNull(loaded.getAddress());
        assertEquals("123 Main St", loaded.getAddress().getStreet());
        assertEquals("Springfield", loaded.getAddress().getCity());
        em.close();
    }

    public void testQueryInnerEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        StaticInnerEntity.InnerEntity e = new StaticInnerEntity.InnerEntity();
        e.setId(2);
        e.setName("querytest");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        StaticInnerEntity.InnerEntity result = em.createQuery(
                "SELECT e FROM StaticInnerEntity$InnerEntity e WHERE e.name = :name",
                StaticInnerEntity.InnerEntity.class)
                .setParameter("name", "querytest")
                .getSingleResult();
        assertNotNull(result);
        assertEquals(2L, result.getId());
        em.close();
    }
}

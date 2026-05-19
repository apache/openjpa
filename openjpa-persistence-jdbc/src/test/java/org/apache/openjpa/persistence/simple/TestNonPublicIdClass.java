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
 * Tests that JPA 3.2 allows primary key classes that are not public
 * and not Serializable.
 */
public class TestNonPublicIdClass extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, NonPublicIdEntity.class);
    }

    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        NonPublicIdEntity e = new NonPublicIdEntity();
        e.key1 = 1;
        e.key2 = "abc";
        e.data = "hello";
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        NonPublicIdEntity loaded = em.find(NonPublicIdEntity.class,
                new NonPublicId(1, "abc"));
        assertNotNull(loaded);
        assertEquals("hello", loaded.data);
        em.close();
    }

    public void testFindMultiple() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        NonPublicIdEntity e1 = new NonPublicIdEntity();
        e1.key1 = 2;
        e1.key2 = "def";
        e1.data = "first";
        em.persist(e1);

        NonPublicIdEntity e2 = new NonPublicIdEntity();
        e2.key1 = 3;
        e2.key2 = "ghi";
        e2.data = "second";
        em.persist(e2);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        NonPublicIdEntity loaded1 = em.find(NonPublicIdEntity.class,
                new NonPublicId(2, "def"));
        NonPublicIdEntity loaded2 = em.find(NonPublicIdEntity.class,
                new NonPublicId(3, "ghi"));
        assertNotNull(loaded1);
        assertNotNull(loaded2);
        assertEquals("first", loaded1.data);
        assertEquals("second", loaded2.data);
        em.close();
    }
}

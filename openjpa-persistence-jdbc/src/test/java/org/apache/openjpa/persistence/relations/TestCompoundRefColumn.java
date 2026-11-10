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
package org.apache.openjpa.persistence.relations;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Verifies that read-only attributes mapped to the columns of a compound foreign key
 * are filled with the value of the matching id column of the referenced entity
 * once the owner is flushed (OPENJPA-2929).
 */
public class TestCompoundRefColumn extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, CompoundRefTarget.class, CompoundRefOwner.class, CompoundRefSwappedOwner.class);
    }

    public void testCompoundRefColumnsAreFilled() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CompoundRefTarget b = new CompoundRefTarget("first", "second", "b");
            em.persist(b);
            CompoundRefOwner a = new CompoundRefOwner(1);
            a.setB(b);
            em.persist(a);
            em.flush();
            assertEquals("first", a.getBId1());
            assertEquals("second", a.getBId2());
            em.getTransaction().commit();
            assertEquals("first", a.getBId1());
            assertEquals("second", a.getBId2());

            em.clear();
            CompoundRefOwner found = em.find(CompoundRefOwner.class, 1L);
            assertEquals("first", found.getBId1());
            assertEquals("second", found.getBId2());
            assertEquals("b", found.getB().getName());
        }
    }

    public void testCompoundRefColumnsAreFilledWithSwappedJoinColumns() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CompoundRefTarget b = new CompoundRefTarget("first", "second", "b");
            em.persist(b);
            CompoundRefSwappedOwner a = new CompoundRefSwappedOwner(1);
            a.setB(b);
            em.persist(a);
            em.flush();
            assertEquals("first", a.getBId1());
            assertEquals("second", a.getBId2());
            em.getTransaction().commit();
            assertEquals("first", a.getBId1());
            assertEquals("second", a.getBId2());

            em.clear();
            CompoundRefSwappedOwner found = em.find(CompoundRefSwappedOwner.class, 1L);
            assertEquals("first", found.getBId1());
            assertEquals("second", found.getBId2());
            assertEquals("b", found.getB().getName());
        }
    }

    public void testCompoundRefColumnsAreUpdatedWhenRelationChanges() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CompoundRefTarget b1 = new CompoundRefTarget("first", "second", "b1");
            CompoundRefTarget b2 = new CompoundRefTarget("third", "fourth", "b2");
            em.persist(b1);
            em.persist(b2);
            CompoundRefOwner a = new CompoundRefOwner(1);
            a.setB(b1);
            em.persist(a);
            em.getTransaction().commit();

            em.getTransaction().begin();
            a.setB(b2);
            em.getTransaction().commit();
            assertEquals("third", a.getBId1());
            assertEquals("fourth", a.getBId2());

            em.clear();
            CompoundRefOwner found = em.find(CompoundRefOwner.class, 1L);
            assertEquals("third", found.getBId1());
            assertEquals("fourth", found.getBId2());
            assertEquals("b2", found.getB().getName());
        }
    }
}

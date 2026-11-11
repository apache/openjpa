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
package org.apache.openjpa.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.spi.LoadState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.openjpa.persistence.entity.EntityA;
import org.apache.openjpa.persistence.entity.EntityB;
import org.apache.openjpa.persistence.entity.EntityC;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestOpenJPA2330 extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(EntityA.class, EntityB.class, EntityC.class);
    }

    @Test
    public void testOpenJPA2330() {
        try (final EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            EntityA a = new EntityA();
            EntityB b = new EntityB(a);
            // set back pointer
            a.getBs().add(b);

            EntityC c = new EntityC(b);
            // set back pointer
            b.getCs().add(c);

            em.persist(a);
            em.persist(c);
            em.getTransaction().commit();

            assertEquals(LoadState.LOADED, OpenJPAPersistenceUtil.isLoaded(b, "center"));
        }
    }

    @Test
    public void testOpenJPA2335() {
        EntityA a = new EntityA();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            EntityB b1 = new EntityB(a);
            b1.setName("b1");

            EntityB b2 = new EntityB(a);
            b2.setName("b2");

            EntityB b3 = new EntityB(a);
            b3.setName("b3");

            EntityB b4 = new EntityB(a);
            b4.setName("b4");

            a.getBs().add(b1);
            a.getBs().add(b2);
            a.getBs().add(b3);
            a.getBs().add(b4);

            em.persist(a);

            em.getTransaction().commit();
        }

        // now read all back in
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            EntityA a2 = em.find(EntityA.class, a.getId());
            assertNotNull(a2);
            assertNotNull(a2.getBs());
            assertEquals(4, a2.getBs().size());

            em.getTransaction().commit();
        }
    }

    @Test
    public void testOrthanRemoval() {
        EntityA a = new EntityA("testA_forRemoval");
        EntityB b1 = new EntityB();
        b1.setName("b1");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            EntityB b2 = new EntityB();
            b2.setName("b2");
            EntityB b3 = new EntityB();
            b3.setName("b3");
            a.getBs().addAll(List.of(b1, b2, b3));
            em.persist(a);
            em.getTransaction().commit();
        }
        assertNotNull(a.getId(), "A should be persisted");
        assertNotNull(b1.getId(), "B1 should be persisted");
        assertEquals(3, a.getBs().size(), "There should be 3 Bs in a");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            EntityA newA = em.find(EntityA.class, a.getId());
            EntityB newB = newA.getBs().iterator().next();
            assertTrue(newA.getBs().remove(newB), "newB should be removed successfully");
            assertEquals(2, newA.getBs().size(), "There should be 2 Bs in a after removal");
            em.merge(newA);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            EntityA newA = em.find(EntityA.class, a.getId());
            assertEquals(2, newA.getBs().size(), "There should be 2 Bs in a");
            assertNull(em.find(EntityB.class, b1.getId()), "B1 must be deleted");
        }
    }
}

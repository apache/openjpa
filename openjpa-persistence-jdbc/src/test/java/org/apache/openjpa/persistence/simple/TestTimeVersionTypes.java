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
import java.time.Instant;
import java.time.LocalDateTime;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPA 3.2 LocalDateTime and Instant as @Version types.
 */
public class TestTimeVersionTypes extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES,
                LocalDateTimeVersionEntity.class,
                InstantVersionEntity.class);
    }

    public void testLocalDateTimeVersionAssignedOnPersist() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LocalDateTimeVersionEntity e = new LocalDateTimeVersionEntity();
        e.setId(1);
        e.setName("test");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        LocalDateTimeVersionEntity loaded = em.find(LocalDateTimeVersionEntity.class, 1L);
        assertNotNull(loaded.getVersion());
        assertTrue(loaded.getVersion() instanceof LocalDateTime);
        em.close();
    }

    public void testLocalDateTimeVersionIncrementsOnUpdate() throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LocalDateTimeVersionEntity e = new LocalDateTimeVersionEntity();
        e.setId(2);
        e.setName("before");
        em.persist(e);
        em.getTransaction().commit();
        LocalDateTime v1 = e.getVersion();
        em.close();

        // small delay to ensure version changes
        Thread.sleep(50);

        em = emf.createEntityManager();
        em.getTransaction().begin();
        e = em.find(LocalDateTimeVersionEntity.class, 2L);
        e.setName("after");
        em.getTransaction().commit();
        LocalDateTime v2 = e.getVersion();
        em.close();

        assertNotNull(v1);
        assertNotNull(v2);
        assertTrue("Version should have increased", v2.isAfter(v1));
    }

    public void testInstantVersionAssignedOnPersist() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        InstantVersionEntity e = new InstantVersionEntity();
        e.setId(1);
        e.setName("test");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        InstantVersionEntity loaded = em.find(InstantVersionEntity.class, 1L);
        assertNotNull(loaded.getVersion());
        assertTrue(loaded.getVersion() instanceof Instant);
        em.close();
    }

    public void testInstantVersionIncrementsOnUpdate() throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        InstantVersionEntity e = new InstantVersionEntity();
        e.setId(2);
        e.setName("before");
        em.persist(e);
        em.getTransaction().commit();
        Instant v1 = e.getVersion();
        em.close();

        // small delay to ensure version changes
        Thread.sleep(50);

        em = emf.createEntityManager();
        em.getTransaction().begin();
        e = em.find(InstantVersionEntity.class, 2L);
        e.setName("after");
        em.getTransaction().commit();
        Instant v2 = e.getVersion();
        em.close();

        assertNotNull(v1);
        assertNotNull(v2);
        assertTrue("Version should have increased", v2.isAfter(v1));
    }

    public void testLocalDateTimeOptimisticLocking() throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LocalDateTimeVersionEntity e = new LocalDateTimeVersionEntity();
        e.setId(3);
        e.setName("original");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        // small delay
        Thread.sleep(50);

        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();

        LocalDateTimeVersionEntity pc1 = em1.find(LocalDateTimeVersionEntity.class, 3L);
        LocalDateTimeVersionEntity pc2 = em2.find(LocalDateTimeVersionEntity.class, 3L);
        assertEquals(pc1.getVersion(), pc2.getVersion());

        pc1.setName("updated-1");
        pc2.setName("updated-2");
        em1.getTransaction().commit();
        em1.close();

        try {
            em2.getTransaction().commit();
            fail("Expected optimistic lock exception");
        } catch (Exception ex) {
            // expected
        } finally {
            em2.close();
        }
    }

    public void testInstantOptimisticLocking() throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        InstantVersionEntity e = new InstantVersionEntity();
        e.setId(3);
        e.setName("original");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        // small delay
        Thread.sleep(50);

        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();

        InstantVersionEntity pc1 = em1.find(InstantVersionEntity.class, 3L);
        InstantVersionEntity pc2 = em2.find(InstantVersionEntity.class, 3L);
        assertEquals(pc1.getVersion(), pc2.getVersion());

        pc1.setName("updated-1");
        pc2.setName("updated-2");
        em1.getTransaction().commit();
        em1.close();

        try {
            em2.getTransaction().commit();
            fail("Expected optimistic lock exception");
        } catch (Exception ex) {
            // expected
        } finally {
            em2.close();
        }
    }
}

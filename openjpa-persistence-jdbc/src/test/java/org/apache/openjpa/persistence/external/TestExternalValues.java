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
package org.apache.openjpa.persistence.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.RollbackException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestExternalValues extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        super.setUp(DROP_TABLES, EntityA.class);
    }

    @Test
    void externalValues() {
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        EntityA entity = new EntityA();

        entity.setS1("SMALL");
        entity.setS2("MEDIUM");
        entity.setUseStreaming(true);

        em.persist(entity);

        em.getTransaction().commit();

        // Validate

        Query q = em.createQuery("SELECT a from EntityA a");
        EntityA aPrime = (EntityA) q.getSingleResult();
        assertEquals("SMALL", aPrime.getS1());
        assertEquals("MEDIUM", aPrime.getS2());
        assertTrue(aPrime.getUseStreaming());

        em.getTransaction().begin();

        entity = new EntityA();
        entity.setS1("LARGE");
        entity.setS2("LARGE");
        entity.setUseStreaming(false);
        em.persist(entity);

        em.getTransaction().commit();

        q = em.createQuery("SELECT t0.id, t0.s1, t0._useStreaming, t0.s2 FROM EntityA t0 ORDER BY t0.s1 DESC");
        List<Object[]> res = q.getResultList();

        Iterator<Object[]> itr = res.iterator();
        Object[] values = itr.next();
        assertEquals("SMALL", values[1]);
        assertEquals(Boolean.TRUE, values[2]);
        assertEquals("MEDIUM", values[3]);

        values = itr.next();
        assertEquals("LARGE", values[1]);
        assertEquals(Boolean.FALSE, values[2]);
        assertEquals("LARGE", values[3]);

        em.close();
    }

    @Test
    void unrecognizedExternalValue() {
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        EntityA entity = new EntityA();

        entity.setS1("ABDEF");
        entity.setS2("NOT_VALID");

        em.persist(entity);

        try {
            em.getTransaction().commit();
            fail("Expected an exception at commit time");
        } catch (RollbackException e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            assertTrue(t.getMessage().contains(
                    "was not found in the list of ExternalValues"));
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
        em.close();
    }
}

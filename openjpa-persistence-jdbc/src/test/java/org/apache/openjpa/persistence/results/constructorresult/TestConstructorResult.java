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
package org.apache.openjpa.persistence.results.constructorresult;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests JPA 2.1 ConstructorResult support in @SqlResultSetMapping.
 */
public class TestConstructorResult extends SQLListenerTestCase {

    @Override
    public void setUp() {
        setUp(CrEntity.class, DROP_TABLES);
        assertNotNull(emf);
        populate();
    }

    public void testConstructorResultWithNamedNativeQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            Query q = em.createNamedQuery("CrEntity.findAllDto");
            List<?> results = q.getResultList();
            assertEquals(2, results.size());

            CrDto dto1 = (CrDto) results.get(0);
            assertEquals(Long.valueOf(1L), dto1.getId());
            assertEquals("Widget", dto1.getName());
            assertEquals(9.99, dto1.getPrice(), 0.001);

            CrDto dto2 = (CrDto) results.get(1);
            assertEquals(Long.valueOf(2L), dto2.getId());
            assertEquals("Gadget", dto2.getName());
            assertEquals(19.99, dto2.getPrice(), 0.001);
        } finally {
            em.close();
        }
    }

    public void testConstructorResultWithInlineNativeQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            Query q = em.createNativeQuery(
                "SELECT e.ID, e.NAME, e.PRICE FROM CR_ENTITY e ORDER BY e.ID",
                "CrDtoMapping");
            List<?> results = q.getResultList();
            assertEquals(2, results.size());

            CrDto dto = (CrDto) results.get(0);
            assertNotNull(dto);
            assertEquals(Long.valueOf(1L), dto.getId());
            assertEquals("Widget", dto.getName());
        } finally {
            em.close();
        }
    }

    public void testConstructorResultSingleRow() {
        EntityManager em = emf.createEntityManager();
        try {
            Query q = em.createNativeQuery(
                "SELECT e.ID, e.NAME, e.PRICE FROM CR_ENTITY e WHERE e.ID = 1",
                "CrDtoMapping");
            Object result = q.getSingleResult();
            assertNotNull(result);
            assertTrue(result instanceof CrDto);

            CrDto dto = (CrDto) result;
            assertEquals(Long.valueOf(1L), dto.getId());
            assertEquals("Widget", dto.getName());
            assertEquals(9.99, dto.getPrice(), 0.001);
        } finally {
            em.close();
        }
    }

    private void populate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new CrEntity(1L, "Widget", 9.99));
        em.persist(new CrEntity(2L, "Gadget", 19.99));
        em.getTransaction().commit();
        em.close();
    }
}

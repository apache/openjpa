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
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for {@link jakarta.persistence.TypedQuery#getSingleResultOrNull()}
 * added in JPA 3.2.
 */
public class TestGetSingleResultOrNull extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
    }

    public void testReturnsNullWhenNoResult() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.intField = 99999",
            AllFieldTypes.class);
        AllFieldTypes result = q.getSingleResultOrNull();
        assertNull(result);
        em.close();
    }

    public void testReturnsSingleResult() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes e = new AllFieldTypes();
        e.setIntField(42);
        e.setStringField("testSingle");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = 'testSingle'",
            AllFieldTypes.class);
        AllFieldTypes result = q.getSingleResultOrNull();
        assertNotNull(result);
        assertEquals(42, result.getIntField());
        em.close();
    }

    public void testReturnsNullValueFromProjection() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes e = new AllFieldTypes();
        e.setIntField(7);
        // stringField is null by default
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        TypedQuery<String> q = em.createQuery(
            "SELECT o.stringField FROM AllFieldTypes o WHERE o.intField = 7",
            String.class);
        String result = q.getSingleResultOrNull();
        assertNull(result);
        em.close();
    }

    public void testThrowsNonUniqueResultException() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes e1 = new AllFieldTypes();
        e1.setIntField(100);
        em.persist(e1);
        AllFieldTypes e2 = new AllFieldTypes();
        e2.setIntField(100);
        em.persist(e2);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.intField = 100",
            AllFieldTypes.class);
        try {
            q.getSingleResultOrNull();
            fail("Expected NonUniqueResultException");
        } catch (NonUniqueResultException expected) {
            // pass
        }
        em.close();
    }

    public void testOnClosedEntityManagerThrows() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o", AllFieldTypes.class);
        em.close();
        try {
            q.getSingleResultOrNull();
            fail("Expected exception on closed EntityManager");
        } catch (Exception expected) {
            // pass — either IllegalStateException or PersistenceException
        }
    }
}

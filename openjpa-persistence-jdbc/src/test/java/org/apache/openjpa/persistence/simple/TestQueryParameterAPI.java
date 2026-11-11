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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Query parameter API compliance (JPA spec).
 */
class TestQueryParameterAPI extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
    }

    /**
     * getParameter(name, type) should return the parameter when the type matches.
     */
    @Test
    void getParameterStringClassMatch() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = :name",
            AllFieldTypes.class);
        Parameter<String> p = q.getParameter("name", String.class);
        assertNotNull(p);
        assertEquals("name", p.getName());
        em.close();
    }

    /**
     * getParameter(name, type) should throw IllegalArgumentException
     * when the type does not match.
     */
    @Test
    void getParameterStringClassMismatch() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = :name",
            AllFieldTypes.class);
        try {
            q.getParameter("name", List.class);
            fail("Expected IllegalArgumentException for type mismatch");
        } catch (IllegalArgumentException expected) {
            // pass
        }
        em.close();
    }

    /**
     * getParameter(pos, type) should return the parameter when the type matches.
     */
    @Test
    void getParameterIntClassMatch() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = ?1");
        q.setParameter(1, "test");
        Parameter<String> p = q.getParameter(1, String.class);
        assertNotNull(p);
        em.close();
    }

    /**
     * getParameter(pos, type) should throw IllegalArgumentException
     * when the type does not match.
     */
    @Test
    void getParameterIntClassMismatch() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = ?1");
        q.setParameter(1, "test");
        try {
            q.getParameter(1, List.class);
            fail("Expected IllegalArgumentException for type mismatch");
        } catch (IllegalArgumentException expected) {
            // pass
        }
        em.close();
    }

    /**
     * getParameterValue(Parameter) should throw IllegalStateException
     * when the parameter has not been bound.
     */
    @Test
    void getParameterValueParameterUnbound() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = :name",
            AllFieldTypes.class);
        Parameter<?> p = q.getParameter("name");
        try {
            q.getParameterValue(p);
            fail("Expected IllegalStateException for unbound parameter");
        } catch (IllegalStateException expected) {
            // pass
        }
        em.close();
    }

    /**
     * getParameterValue(String) should throw IllegalStateException
     * when the parameter has not been bound.
     */
    @Test
    void getParameterValueStringUnbound() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = :name",
            AllFieldTypes.class);
        try {
            q.getParameterValue("name");
            fail("Expected IllegalStateException for unbound parameter");
        } catch (IllegalStateException expected) {
            // pass
        }
        em.close();
    }

    /**
     * setParameter(Parameter, Object) should throw IllegalArgumentException
     * when the parameter does not belong to the query.
     */
    @Test
    void setParameterFromDifferentQuery() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<AllFieldTypes> q1 = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = :name1",
            AllFieldTypes.class);
        TypedQuery<AllFieldTypes> q2 = em.createQuery(
            "SELECT o FROM AllFieldTypes o WHERE o.stringField = :name2",
            AllFieldTypes.class);
        // Get parameter from q2
        @SuppressWarnings("unchecked")
        Parameter<String> p2 = (Parameter<String>) q2.getParameter("name2");
        // Try to set it on q1 — should fail
        try {
            q1.setParameter(p2, "value");
            fail("Expected IllegalArgumentException for parameter from different query");
        } catch (IllegalArgumentException expected) {
            // pass
        }
        em.close();
    }
}

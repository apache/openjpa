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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that methods called on a closed EntityManager and its associated
 * Query/TypedQuery objects throw IllegalStateException per JPA spec.
 */
class TestEntityManagerAfterClose extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(AllFieldTypes.class);
    }

    @Test
    void getEntityManagerFactoryAfterClose() {
        EntityManager em = emf.createEntityManager();
        em.close();
        try {
            em.getEntityManagerFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void getCriteriaBuilderAfterClose() {
        EntityManager em = emf.createEntityManager();
        em.close();
        try {
            em.getCriteriaBuilder();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void getMetamodelAfterClose() {
        EntityManager em = emf.createEntityManager();
        em.close();
        try {
            em.getMetamodel();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void isJoinedToTransactionAfterClose() {
        EntityManager em = emf.createEntityManager();
        em.close();
        try {
            em.isJoinedToTransaction();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void setPropertyAfterClose() {
        EntityManager em = emf.createEntityManager();
        em.close();
        try {
            em.setProperty("foo", "bar");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void queryGetFlushModeAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.getFlushMode();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void queryGetHintsAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.getHints();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void queryGetLockModeAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.getLockMode();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void queryGetParametersAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.getParameters();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void querySetFlushModeAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.setFlushMode(jakarta.persistence.FlushModeType.AUTO);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void querySetHintAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.setHint("foo", "bar");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void querySetLockModeAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.setLockMode(jakarta.persistence.LockModeType.NONE);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void queryIsBoundAfterClose() {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a WHERE a.intField = :p");
        jakarta.persistence.Parameter<?> param = query.getParameter("p");
        em.close();
        try {
            query.isBound(param);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * Test that getRollbackOnly() does not throw after close with active tx.
     * Mimics TCK queryMethodsAfterClose5Test pattern: begin tx, close EM,
     * call query method (throws ISE), then check getRollbackOnly() in catch.
     */
    @Test
    void getRollbackOnlyAfterCloseWithActiveTx() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("SELECT a FROM AllFieldTypes a");
        em.close();
        try {
            query.getLockMode();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // close() no longer rolls back; tx is still active (deferred close)
            // The ISE from getLockMode() on a close-invoked EM marks rollback
            boolean rollbackOnly = em.getTransaction().getRollbackOnly();
            assertFalse(rollbackOnly, "Transaction should not be marked rollback-only");
        }
        // Transaction should still be active since close() defers until tx completes
        assertTrue(em.getTransaction().isActive(),
            "Transaction should still be active after close with deferred close");
        // Clean up: rollback the still-active transaction
        em.getTransaction().rollback();
    }
}

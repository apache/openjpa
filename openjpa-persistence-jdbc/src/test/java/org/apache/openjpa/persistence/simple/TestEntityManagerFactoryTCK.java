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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPA 3.2 EntityManagerFactory methods:
 * - addNamedQuery with FlushMode, MaxResults, LockMode preservation
 * - getMetamodel on closed EMF throws IllegalStateException
 * - callInTransaction
 */
public class TestEntityManagerFactoryTCK extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, AllFieldTypes.class, PropertyAccessMember.class);
    }

    /**
     * Test that addNamedQuery preserves MaxResults from the source query
     * and that createNamedQuery returns a query with those MaxResults.
     */
    public void testAddNamedQueryMaxResults() {
        EntityManager em = emf.createEntityManager();
        try {
            // Insert test data
            em.getTransaction().begin();
            for (int i = 0; i < 5; i++) {
                AllFieldTypes aft = new AllFieldTypes();
                aft.setStringField("item" + i);
                em.persist(aft);
            }
            em.getTransaction().commit();

            // Create a JPQL query with maxResults=2
            Query query = em.createQuery(
                "SELECT a FROM AllFieldTypes a ORDER BY a.stringField");
            query.setMaxResults(2);

            // Register as named query
            emf.addNamedQuery("testMaxQuery", query);

            // Create named query and verify maxResults is preserved
            Query namedQuery = em.createNamedQuery("testMaxQuery");
            assertEquals("MaxResults should be preserved from addNamedQuery",
                2, namedQuery.getMaxResults());

            // Execute and verify only 2 results returned
            em.getTransaction().begin();
            List results = namedQuery.getResultList();
            assertEquals("Should return maxResults number of results",
                2, results.size());
            em.getTransaction().commit();

            // Verify changing maxResults on instance doesn't affect template
            namedQuery.setMaxResults(3);
            assertEquals(3, namedQuery.getMaxResults());

            // New instance should still have original maxResults
            Query namedQuery2 = em.createNamedQuery("testMaxQuery");
            assertEquals("Template maxResults should be unchanged",
                2, namedQuery2.getMaxResults());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test that addNamedQuery preserves FlushMode from the source query.
     */
    public void testAddNamedQueryFlushMode() {
        EntityManager em = emf.createEntityManager();
        try {
            // Create a JPQL query with FlushMode.AUTO
            Query query = em.createQuery(
                "SELECT a FROM AllFieldTypes a");
            query.setFlushMode(FlushModeType.AUTO);

            // Register as named query
            emf.addNamedQuery("testFlushQuery", query);

            // Create named query and verify flushMode is preserved
            Query namedQuery = em.createNamedQuery("testFlushQuery");
            assertEquals("FlushMode should be preserved from addNamedQuery",
                FlushModeType.AUTO, namedQuery.getFlushMode());

            // Change flush mode on instance
            namedQuery.setFlushMode(FlushModeType.COMMIT);
            assertEquals(FlushModeType.COMMIT, namedQuery.getFlushMode());

            // New instance should still have original flush mode
            Query namedQuery2 = em.createNamedQuery("testFlushQuery");
            assertEquals("Template FlushMode should be unchanged",
                FlushModeType.AUTO, namedQuery2.getFlushMode());
        } finally {
            em.close();
        }
    }

    /**
     * Test that addNamedQuery preserves LockMode from the source query.
     */
    public void testAddNamedQueryLockMode() {
        EntityManager em = emf.createEntityManager();
        try {
            // Create a JPQL query with LockMode.NONE
            Query query = em.createQuery(
                "SELECT a FROM AllFieldTypes a");
            query.setLockMode(LockModeType.NONE);

            // Register as named query
            emf.addNamedQuery("testLockQuery", query);

            // Create named query and verify lockMode is preserved
            em.getTransaction().begin();
            Query namedQuery = em.createNamedQuery("testLockQuery");
            LockModeType lmt = namedQuery.getLockMode();
            assertNotNull("LockMode should not be null", lmt);
            assertEquals("LockMode should be preserved from addNamedQuery",
                LockModeType.NONE, lmt);

            // Change lock mode on instance
            namedQuery.setLockMode(LockModeType.PESSIMISTIC_READ);
            LockModeType newLmt = namedQuery.getLockMode();
            assertTrue("LockMode should be changed",
                newLmt == LockModeType.PESSIMISTIC_READ
                || newLmt == LockModeType.PESSIMISTIC_WRITE);
            em.getTransaction().commit();

            // New instance should still have original lock mode
            em.getTransaction().begin();
            Query namedQuery2 = em.createNamedQuery("testLockQuery");
            assertEquals("Template LockMode should be unchanged",
                LockModeType.NONE, namedQuery2.getLockMode());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test that addNamedQuery can replace an existing named query.
     */
    public void testAddNamedQueryReplacement() {
        EntityManager em = emf.createEntityManager();
        try {
            // Register a query with maxResults=1
            Query query1 = em.createQuery(
                "SELECT a FROM AllFieldTypes a");
            query1.setMaxResults(1);
            emf.addNamedQuery("replaceableQuery", query1);

            Query nq1 = em.createNamedQuery("replaceableQuery");
            assertEquals(1, nq1.getMaxResults());

            // Replace with a query with maxResults=5
            Query query2 = em.createQuery(
                "SELECT a FROM AllFieldTypes a");
            query2.setMaxResults(5);
            emf.addNamedQuery("replaceableQuery", query2);

            Query nq2 = em.createNamedQuery("replaceableQuery");
            assertEquals("Replaced query should have new maxResults",
                5, nq2.getMaxResults());
        } finally {
            em.close();
        }
    }

    /**
     * Test that addNamedQuery works with native queries.
     */
    public void testAddNamedQueryNative() {
        EntityManager em = emf.createEntityManager();
        try {
            // Create a native query with maxResults
            Query nativeQuery = em.createNativeQuery(
                "SELECT * FROM AllFieldTypes ORDER BY 1");
            nativeQuery.setMaxResults(1);

            // Register as named query
            emf.addNamedQuery("testNativeQuery", nativeQuery);

            // Create named query and verify maxResults is preserved
            Query namedQuery = em.createNamedQuery("testNativeQuery");
            assertEquals("MaxResults should be preserved for native query",
                1, namedQuery.getMaxResults());
        } finally {
            em.close();
        }
    }

    /**
     * Test that getMetamodel() on a closed EMF throws IllegalStateException.
     */
    public void testGetMetamodelAfterCloseThrowsIllegalState() {
        // Create a separate EMF so we can close it without affecting
        // other tests
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.getMetamodel();
            fail("getMetamodel() on closed EMF should throw "
                + "IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * Test callInTransaction: persist an entity inside the transaction
     * function and verify it is committed.
     */
    public void testCallInTransaction() {
        // Use callInTransaction to persist an entity
        AllFieldTypes result = emf.callInTransaction(em -> {
            AllFieldTypes aft = new AllFieldTypes();
            aft.setStringField("callInTransaction");
            em.persist(aft);
            return aft;
        });

        assertNotNull("Result from callInTransaction should not be null",
            result);

        // Verify the entity was committed to the database
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(a) FROM AllFieldTypes a "
                + "WHERE a.stringField = 'callInTransaction'",
                Long.class);
            long count = q.getSingleResult();
            assertEquals("Entity should be committed to database", 1L, count);
        } finally {
            em.close();
        }
    }

    /**
     * Test callInTransaction rolls back on exception.
     */
    public void testCallInTransactionRollback() {
        try {
            emf.callInTransaction(em -> {
                AllFieldTypes aft = new AllFieldTypes();
                aft.setStringField("shouldBeRolledBack");
                em.persist(aft);
                throw new RuntimeException("Intentional failure");
            });
            fail("Should have thrown exception");
        } catch (Exception e) {
            // expected
        }

        // Verify the entity was NOT committed
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(a) FROM AllFieldTypes a "
                + "WHERE a.stringField = 'shouldBeRolledBack'",
                Long.class);
            long count = q.getSingleResult();
            assertEquals("Entity should not be in database after rollback",
                0L, count);
        } finally {
            em.close();
        }
    }

    /**
     * Test runInTransaction: persist an entity and verify commit.
     */
    public void testRunInTransaction() {
        emf.runInTransaction(em -> {
            AllFieldTypes aft = new AllFieldTypes();
            aft.setStringField("runInTransaction");
            em.persist(aft);
        });

        // Verify the entity was committed
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(a) FROM AllFieldTypes a "
                + "WHERE a.stringField = 'runInTransaction'",
                Long.class);
            long count = q.getSingleResult();
            assertEquals("Entity should be committed to database", 1L, count);
        } finally {
            em.close();
        }
    }

    /**
     * Test addNamedQuery with criteria-based TypedQuery preserves MaxResults.
     * This mirrors the TCK addNamedQueryMaxResultTest which fails with
     * AbstractMethodError when criteria queries are stored and recreated.
     */
    public void testAddNamedQueryCriteriaMaxResults() {
        EntityManager em = emf.createEntityManager();
        try {
            // Insert test data
            em.getTransaction().begin();
            for (int i = 0; i < 5; i++) {
                AllFieldTypes aft = new AllFieldTypes();
                aft.setStringField("criteriaItem" + i);
                em.persist(aft);
            }
            em.getTransaction().commit();

            // Create criteria query
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root);
            cquery.orderBy(cb.asc(root.get("stringField")));

            TypedQuery<AllFieldTypes> typedQuery = em.createQuery(cquery);
            typedQuery.setMaxResults(2);
            emf.addNamedQuery("criteria_max_query", typedQuery);

            // Recreate from named query - this should not throw AbstractMethodError
            em.getTransaction().begin();
            TypedQuery<AllFieldTypes> namedQuery = em.createNamedQuery(
                "criteria_max_query", AllFieldTypes.class);
            assertEquals("MaxResults should be preserved for criteria query",
                2, namedQuery.getMaxResults());

            List<AllFieldTypes> results = namedQuery.getResultList();
            assertEquals("Should return maxResults number of results",
                2, results.size());
            em.getTransaction().commit();

            // Verify changing maxResults on instance doesn't affect template
            namedQuery.setMaxResults(3);
            TypedQuery<AllFieldTypes> namedQuery2 = em.createNamedQuery(
                "criteria_max_query", AllFieldTypes.class);
            assertEquals("Template maxResults should be unchanged",
                2, namedQuery2.getMaxResults());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test addNamedQuery with criteria-based TypedQuery preserves FlushMode.
     * Mirrors TCK addNamedQueryFlushModeTest.
     */
    public void testAddNamedQueryCriteriaFlushMode() {
        EntityManager em = emf.createEntityManager();
        try {
            // Create criteria query with FlushMode.AUTO
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root);

            TypedQuery<AllFieldTypes> typedQuery = em.createQuery(cquery);
            typedQuery.setFlushMode(FlushModeType.AUTO);
            emf.addNamedQuery("criteria_flush_query", typedQuery);

            // Recreate and verify flush mode
            TypedQuery<AllFieldTypes> namedQuery = em.createNamedQuery(
                "criteria_flush_query", AllFieldTypes.class);
            assertEquals("FlushMode should be preserved for criteria query",
                FlushModeType.AUTO, namedQuery.getFlushMode());

            // Change on instance should not affect template
            namedQuery.setFlushMode(FlushModeType.COMMIT);
            TypedQuery<AllFieldTypes> namedQuery2 = em.createNamedQuery(
                "criteria_flush_query", AllFieldTypes.class);
            assertEquals("Template FlushMode should be unchanged",
                FlushModeType.AUTO, namedQuery2.getFlushMode());
        } finally {
            em.close();
        }
    }

    /**
     * Test addNamedQuery with criteria-based TypedQuery preserves LockMode.
     * Mirrors TCK addNamedQueryLockModeTest.
     */
    public void testAddNamedQueryCriteriaLockMode() {
        EntityManager em = emf.createEntityManager();
        try {
            // Create criteria query with LockMode.NONE
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root);

            TypedQuery<AllFieldTypes> typedQuery = em.createQuery(cquery);
            typedQuery.setLockMode(LockModeType.NONE);
            emf.addNamedQuery("criteria_lock_query", typedQuery);

            // Recreate and verify lock mode
            em.getTransaction().begin();
            TypedQuery<AllFieldTypes> namedQuery = em.createNamedQuery(
                "criteria_lock_query", AllFieldTypes.class);
            LockModeType lmt = namedQuery.getLockMode();
            assertNotNull("LockMode should not be null", lmt);
            assertEquals("LockMode should be preserved for criteria query",
                LockModeType.NONE, lmt);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test callInTransaction with a property-access entity that has
     * equals()/hashCode() using getClass() check (mirrors TCK callInTransactionTest).
     * Verifies that the returned entity from callInTransaction can be
     * found and compared via equals() with an entity from find().
     */
    public void testCallInTransactionPropertyAccessEquals() {
        final int MEMBER_ID = 42;

        PropertyAccessMember member = emf.callInTransaction(em -> {
            PropertyAccessMember m = new PropertyAccessMember(MEMBER_ID,
                String.valueOf(MEMBER_ID));
            em.persist(m);
            return m;
        });

        assertNotNull("Result from callInTransaction should not be null",
            member);

        // Find the entity via a different EntityManager
        EntityManager em = emf.createEntityManager();
        try {
            PropertyAccessMember found = em.find(PropertyAccessMember.class,
                MEMBER_ID);
            assertNotNull("Entity should be found after callInTransaction",
                found);
            assertEquals("Entity memberId should match",
                MEMBER_ID, found.getMemberId());
            assertEquals("Entity memberName should match",
                String.valueOf(MEMBER_ID), found.getMemberName());
            // This is the key assertion: equals() with getClass() check
            assertEquals("Persisted and found entities should be equal",
                member, found);
        } finally {
            em.close();
        }
    }
}

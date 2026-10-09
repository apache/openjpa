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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.criteria.OpenJPACriteriaBuilder;
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
     * A criteria query registered via addNamedQuery must be replayed as a
     * criteria query. Its CQL rendering is not guaranteed to be parseable JPQL,
     * so re-labelling the named query as JPQL either fails to parse or silently
     * changes the query.
     */
    public void testAddNamedQueryCriteriaWithParameter() {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            ParameterExpression<String> param = cb.parameter(String.class, "sf");
            cquery.select(root).where(cb.equal(root.get("stringField"), param));

            TypedQuery<AllFieldTypes> typedQuery = em.createQuery(cquery);
            List<AllFieldTypes> expected = typedQuery.setParameter("sf", "named1").getResultList();
            assertEquals(1, expected.size());

            emf.addNamedQuery("criteria_param_query", typedQuery);

            TypedQuery<AllFieldTypes> namedQuery = em.createNamedQuery(
                "criteria_param_query", AllFieldTypes.class);
            // the named query must still be a criteria query, not JPQL
            assertEquals(OpenJPACriteriaBuilder.LANG_CRITERIA,
                ((OpenJPAQuery<?>) namedQuery).getLanguage());
            assertEquals(typedQuery.unwrap(OpenJPAQuery.class).getQueryString(),
                ((OpenJPAQuery<?>) namedQuery).getQueryString());
            List<AllFieldTypes> actual = namedQuery.setParameter("sf", "named1").getResultList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.get(0).getStringField(), actual.get(0).getStringField());
        } finally {
            em.close();
        }
    }

    /**
     * A string literal of a criteria query is rendered unescaped by the CQL
     * toString(), so a named query derived from that string would not parse.
     */
    public void testAddNamedQueryCriteriaWithQuotedLiteral() {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root).where(cb.equal(root.get("stringField"), "it's named"));

            TypedQuery<AllFieldTypes> typedQuery = em.createQuery(cquery);
            List<AllFieldTypes> expected = typedQuery.getResultList();
            assertEquals(1, expected.size());

            emf.addNamedQuery("criteria_literal_query", typedQuery);

            List<AllFieldTypes> actual = em.createNamedQuery(
                "criteria_literal_query", AllFieldTypes.class).getResultList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.get(0).getStringField(), actual.get(0).getStringField());
        } finally {
            em.close();
        }
    }

    /**
     * A subquery of a criteria query is rendered without enclosing parenthesis
     * by the CQL toString(), so a named query derived from that string would not
     * parse.
     */
    public void testAddNamedQueryCriteriaWithSubquery() {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            Subquery<Integer> sub = cquery.subquery(Integer.class);
            Root<AllFieldTypes> subRoot = sub.from(AllFieldTypes.class);
            sub.select(cb.max(subRoot.<Integer>get("intField")));
            cquery.select(root).where(cb.equal(root.get("intField"), sub));

            TypedQuery<AllFieldTypes> typedQuery = em.createQuery(cquery);
            List<AllFieldTypes> expected = typedQuery.getResultList();
            assertEquals(1, expected.size());

            emf.addNamedQuery("criteria_subquery_query", typedQuery);

            List<AllFieldTypes> actual = em.createNamedQuery(
                "criteria_subquery_query", AllFieldTypes.class).getResultList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.get(0).getIntField(), actual.get(0).getIntField());
        } finally {
            em.close();
        }
    }

    /**
     * The parsed criteria form kept in the query metadata is shared by every
     * replay, so replaying it repeatedly - including through the untyped
     * createNamedQuery(String) which does not narrow the result class - must
     * keep yielding the same result.
     * <p>
     * The tree combines a quoted string literal with a subquery so that its CQL
     * rendering is not parseable JPQL; a replay that went back through that
     * string would fail rather than return the rows.
     */
    public void testAddNamedQueryCriteriaRepeatedReplay() {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            Subquery<Integer> sub = cquery.subquery(Integer.class);
            Root<AllFieldTypes> subRoot = sub.from(AllFieldTypes.class);
            sub.select(cb.max(subRoot.<Integer>get("intField")))
               .where(cb.equal(subRoot.get("stringField"), "it's named"));
            cquery.select(root).where(cb.equal(root.get("intField"), sub));

            emf.addNamedQuery("criteria_replay_query", em.createQuery(cquery));

            // the named query must be registered as a criteria query, not as the
            // JPQL re-labelling of a rendering that does not parse
            assertEquals(OpenJPACriteriaBuilder.LANG_CRITERIA,
                emf.getConfiguration().getMetaDataRepositoryInstance()
                    .getQueryMetaData(null, "criteria_replay_query",
                        getClass().getClassLoader(), true).getLanguage());

            for (int i = 0; i < 3; i++) {
                List<AllFieldTypes> typed = em.createNamedQuery(
                    "criteria_replay_query", AllFieldTypes.class).getResultList();
                assertEquals(1, typed.size());
                assertEquals(99, typed.get(0).getIntField());

                List<?> untyped = em.createNamedQuery("criteria_replay_query").getResultList();
                assertEquals(1, untyped.size());
                assertEquals(99, ((AllFieldTypes) untyped.get(0)).getIntField());

                EntityManager other = emf.createEntityManager();
                try {
                    List<AllFieldTypes> fromOther = other.createNamedQuery(
                        "criteria_replay_query", AllFieldTypes.class).getResultList();
                    assertEquals(1, fromOther.size());
                } finally {
                    other.close();
                }
            }
        } finally {
            em.close();
        }
    }

    /**
     * A snapshot of a criteria query deliberately shares its mutable
     * translation state with the original query and with every other snapshot,
     * so two named queries registered from the same tree are two objects over
     * one piece of state. Replaying both concurrently must still be safe.
     */
    public void testAddNamedQueryCriteriaConcurrentReplayOfTwoSnapshots() throws Exception {
        seedCriteriaData();
        final String[] names = {"criteria_shared_tree_a", "criteria_shared_tree_b"};
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            ParameterExpression<String> param = cb.parameter(String.class, "sf");
            Subquery<Integer> sub = cquery.subquery(Integer.class);
            Root<AllFieldTypes> subRoot = sub.from(AllFieldTypes.class);
            sub.select(cb.max(subRoot.<Integer>get("intField")))
               .where(cb.equal(subRoot.get("stringField"), param));
            cquery.select(root).where(cb.equal(root.get("intField"), sub));

            // every createQuery() takes its own snapshot of the same tree, and
            // all of those snapshots share one translation state
            for (String name : names) {
                emf.addNamedQuery(name, em.createQuery(cquery));
            }
        } finally {
            em.close();
        }

        replayConcurrently(names, "named2", 2);
    }

    /**
     * A criteria named query is an EntityManagerFactory-wide artefact and must
     * be usable concurrently from many EntityManagers. The parsed form kept in
     * the QueryMetaData is shared by every replay, so its translation to a
     * kernel expression tree must not be corrupted by a concurrent replay.
     */
    public void testAddNamedQueryCriteriaConcurrentReplay() throws Exception {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            ParameterExpression<String> param = cb.parameter(String.class, "sf");
            Subquery<Integer> sub = cquery.subquery(Integer.class);
            Root<AllFieldTypes> subRoot = sub.from(AllFieldTypes.class);
            sub.select(cb.max(subRoot.<Integer>get("intField")))
               .where(cb.equal(subRoot.get("stringField"), param));
            cquery.select(root).where(cb.equal(root.get("intField"), sub));

            emf.addNamedQuery("criteria_concurrent_query", em.createQuery(cquery));
        } finally {
            em.close();
        }

        replayConcurrently(new String[] {"criteria_concurrent_query"}, "named2", 2);
    }

    /**
     * Replays the given named queries from many threads at once, each thread
     * cycling through all of them, and asserts that every replay returns the
     * single expected row.
     */
    private void replayConcurrently(final String[] names, final String parameter,
        final int expectedIntField) throws Exception {
        replayConcurrently(names, 8, 40, (tem, name) -> {
            List<AllFieldTypes> result = tem.createNamedQuery(name, AllFieldTypes.class)
                .setParameter("sf", parameter).getResultList();
            assertEquals(1, result.size());
            assertEquals(expectedIntField, result.get(0).getIntField());
        });
    }

    /**
     * Replays the given named queries from many threads at once, each thread
     * cycling through all of them with its own EntityManager, and fails if any
     * replay did.
     */
    private void replayConcurrently(final String[] names, final int threads, final int iterations,
        final BiConsumer<EntityManager, String> replay) throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                final int offset = t;
                pool.execute(() -> {
                    try {
                        barrier.await();
                        for (int i = 0; i < iterations; i++) {
                            String name = names[(offset + i) % names.length];
                            EntityManager tem = emf.createEntityManager();
                            try {
                                replay.accept(tem, name);
                            } finally {
                                if (tem.getTransaction().isActive()) {
                                    tem.getTransaction().rollback();
                                }
                                tem.close();
                            }
                        }
                    } catch (Throwable e) {
                        failures.add(e);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.MINUTES));
        } finally {
            pool.shutdownNow();
        }
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.size() + " of " + (threads * iterations)
                + " concurrent replays failed, first: " + failures.get(0), failures.get(0));
        }
    }

    /**
     * A criteria update and a criteria delete are registered through the same
     * path and have no lock mode to capture, so they must be replayable too.
     */
    public void testAddNamedQueryCriteriaUpdateAndDelete() {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();

            CriteriaUpdate<AllFieldTypes> cupdate = cb.createCriteriaUpdate(AllFieldTypes.class);
            Root<AllFieldTypes> uroot = cupdate.from(AllFieldTypes.class);
            cupdate.set(uroot.<String>get("stringField"), "renamed")
                   .where(cb.equal(uroot.get("intField"), 1));
            emf.addNamedQuery("criteria_update_query", em.createQuery(cupdate));

            CriteriaDelete<AllFieldTypes> cdelete = cb.createCriteriaDelete(AllFieldTypes.class);
            Root<AllFieldTypes> droot = cdelete.from(AllFieldTypes.class);
            cdelete.where(cb.equal(droot.get("intField"), 2));
            emf.addNamedQuery("criteria_delete_query", em.createQuery(cdelete));

            em.getTransaction().begin();
            assertEquals(1, em.createNamedQuery("criteria_update_query").executeUpdate());
            assertEquals(1, em.createNamedQuery("criteria_delete_query").executeUpdate());
            em.getTransaction().commit();

            assertEquals(1, em.createQuery(
                "SELECT a FROM AllFieldTypes a WHERE a.stringField = 'renamed'").getResultList().size());
            assertEquals(0, em.createQuery(
                "SELECT a FROM AllFieldTypes a WHERE a.intField = 2").getResultList().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * A set operation is the one criteria query that translates more than one
     * tree: its operands are independent queries with their own translation
     * state, so replaying it has to take all of their locks. Unlike a plain
     * criteria query, a set operation is not snapshotted when it is handed to
     * createQuery(), so the named query shares the very trees the caller built.
     */
    public void testAddNamedQueryCriteriaSetOperation() throws Exception {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();

            CriteriaQuery<String> q1 = cb.createQuery(String.class);
            Root<AllFieldTypes> r1 = q1.from(AllFieldTypes.class);
            q1.select(r1.<String>get("stringField")).where(cb.equal(r1.get("intField"), 0));

            CriteriaQuery<String> q2 = cb.createQuery(String.class);
            Root<AllFieldTypes> r2 = q2.from(AllFieldTypes.class);
            q2.select(r2.<String>get("stringField")).where(cb.equal(r2.get("intField"), 1));

            CriteriaSelect<String> union = cb.union(q1, q2);
            emf.addNamedQuery("criteria_union_query", em.createQuery(union));

            assertEquals(OpenJPACriteriaBuilder.LANG_CRITERIA,
                emf.getConfiguration().getMetaDataRepositoryInstance()
                    .getQueryMetaData(null, "criteria_union_query",
                        getClass().getClassLoader(), true).getLanguage());

            assertEquals(List.of("named0", "named1"), selected(em.createQuery(union).getResultList()));

            for (int i = 0; i < 3; i++) {
                assertEquals("replay " + i, List.of("named0", "named1"),
                    selected(em.createNamedQuery("criteria_union_query").getResultList()));
            }
        } finally {
            em.close();
        }

        replayConcurrently(new String[] {"criteria_union_query"}, 8, 40, (tem, name) ->
            assertEquals(List.of("named0", "named1"), selected(tem.createNamedQuery(name).getResultList())));
    }

    /**
     * A criteria update and a criteria delete carry a translation lock of their
     * own, so they too must be replayable concurrently. Both match no row, so
     * every replay is expected to update nothing, whichever order they run in.
     */
    public void testAddNamedQueryCriteriaUpdateAndDeleteConcurrentReplay() throws Exception {
        seedCriteriaData();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();

            CriteriaUpdate<AllFieldTypes> cupdate = cb.createCriteriaUpdate(AllFieldTypes.class);
            Root<AllFieldTypes> uroot = cupdate.from(AllFieldTypes.class);
            cupdate.set(uroot.<String>get("stringField"), "unreachable")
                   .where(cb.equal(uroot.get("intField"), -1));
            emf.addNamedQuery("criteria_update_noop_query", em.createQuery(cupdate));

            CriteriaDelete<AllFieldTypes> cdelete = cb.createCriteriaDelete(AllFieldTypes.class);
            Root<AllFieldTypes> droot = cdelete.from(AllFieldTypes.class);
            cdelete.where(cb.equal(droot.get("intField"), -2));
            emf.addNamedQuery("criteria_delete_noop_query", em.createQuery(cdelete));
        } finally {
            em.close();
        }

        replayConcurrently(new String[] {"criteria_update_noop_query", "criteria_delete_noop_query"},
            4, 10, (tem, name) -> {
                tem.getTransaction().begin();
                assertEquals(0, tem.createNamedQuery(name).executeUpdate());
                tem.getTransaction().commit();
            });

        EntityManager em2 = emf.createEntityManager();
        try {
            assertEquals(4, em2.createQuery("SELECT a FROM AllFieldTypes a").getResultList().size());
        } finally {
            em2.close();
        }
    }

    /**
     * The rows of a set operation are packed as single-element object arrays
     * rather than as the projected value itself; returns those values, sorted,
     * so that the result of a union can be compared.
     */
    private static List<Object> selected(List<?> rows) {
        List<Object> values = new ArrayList<>();
        for (Object row : rows) {
            values.add(((Object[]) row)[0]);
        }
        values.sort(Comparator.comparing(Object::toString));
        return values;
    }

    private void seedCriteriaData() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int i = 0; i < 3; i++) {
                AllFieldTypes aft = new AllFieldTypes();
                aft.setStringField("named" + i);
                aft.setIntField(i);
                em.persist(aft);
            }
            AllFieldTypes quoted = new AllFieldTypes();
            quoted.setStringField("it's named");
            quoted.setIntField(99);
            em.persist(quoted);
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

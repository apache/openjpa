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
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.criteria.OpenJPACriteriaBuilder;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A criteria query registered by {@code addNamedQuery()} is identified by the
 * CQL rendering of its criteria tree, which is not a parseable query string.
 * The prepared query cache must therefore not cache it under that identifier,
 * or replaying it a second time would rebuild it by parsing that rendering as
 * JPQL.
 */
public class TestNamedCriteriaQueryWithSQLCache extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, AllFieldTypes.class,
            "openjpa.jdbc.QuerySQLCache", "true");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int i = 0; i < 3; i++) {
                AllFieldTypes aft = new AllFieldTypes();
                aft.setStringField("cached" + i);
                aft.setIntField(i);
                em.persist(aft);
            }
            AllFieldTypes quoted = new AllFieldTypes();
            quoted.setStringField("it's cached");
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
     * A quoted string literal is rendered unescaped by the CQL toString().
     */
    public void testReplayWithQuotedLiteralIsNotCachedByItsRendering() {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root).where(cb.equal(root.get("stringField"), "it's cached"));

            emf.addNamedQuery("cached_literal_query", em.createQuery(cquery));

            for (int i = 0; i < 2; i++) {
                TypedQuery<AllFieldTypes> replay = em.createNamedQuery(
                    "cached_literal_query", AllFieldTypes.class);
                List<AllFieldTypes> result = replay.getResultList();
                assertEquals("replay " + i, 1, result.size());
                assertEquals("it's cached", result.get(0).getStringField());
            }
        } finally {
            em.close();
        }
    }

    /**
     * Once a replay has been registered in the prepared query cache under the
     * CQL rendering of its tree, anything that makes a later replay abandon the
     * cached entry - here setting a lock mode, which the prepared SQL cannot
     * carry - recreates the query by parsing that rendering as JPQL.
     */
    public void testReplayWithQuotedLiteralUnderLockMode() {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root).where(cb.equal(root.get("stringField"), "it's cached"));

            emf.addNamedQuery("locked_literal_query", em.createQuery(cquery));

            // the first replay registers the query in the prepared query cache
            assertEquals(1, em.createNamedQuery(
                "locked_literal_query", AllFieldTypes.class).getResultList().size());

            em.getTransaction().begin();
            TypedQuery<AllFieldTypes> locked = em.createNamedQuery(
                "locked_literal_query", AllFieldTypes.class);
            locked.setLockMode(LockModeType.PESSIMISTIC_READ);
            List<AllFieldTypes> result = locked.getResultList();
            assertEquals(1, result.size());
            assertEquals("it's cached", result.get(0).getStringField());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * The CQL rendering of a simple criteria query happens to be parseable JPQL,
     * so a JPQL query of exactly that text can be held in the prepared query
     * cache under the identifier of the criteria query. Replaying the criteria
     * query must not pick up that entry and rebuild itself from its own
     * identifier as JPQL.
     */
    public void testJpqlCachedUnderTheSameTextDoesNotHijackReplayUnderLockMode() {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            cquery.select(root).where(cb.equal(root.get("intField"), 1));

            emf.addNamedQuery("collided_lock_query", em.createQuery(cquery));

            OpenJPAQuery<AllFieldTypes> replay = (OpenJPAQuery<AllFieldTypes>) em.createNamedQuery(
                "collided_lock_query", AllFieldTypes.class);
            String rendering = replay.getQueryString();
            assertEquals(1, replay.getResultList().size());

            // prime the prepared query cache with a JPQL query of exactly the
            // text the criteria query is identified by
            for (int i = 0; i < 2; i++) {
                assertEquals(1, em.createQuery(rendering, AllFieldTypes.class).getResultList().size());
            }

            em.getTransaction().begin();
            OpenJPAQuery<AllFieldTypes> locked = (OpenJPAQuery<AllFieldTypes>) em.createNamedQuery(
                "collided_lock_query", AllFieldTypes.class);
            locked.setLockMode(LockModeType.PESSIMISTIC_READ);
            List<AllFieldTypes> result = locked.getResultList();
            assertEquals(OpenJPACriteriaBuilder.LANG_CRITERIA, locked.getLanguage());
            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getIntField());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * A null parameter value also makes an execution abandon the prepared query
     * cache, which is the other way into the rebuilding of the query from its
     * identifier.
     */
    public void testJpqlCachedUnderTheSameTextDoesNotHijackReplayWithNullParameter() {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            ParameterExpression<String> param = cb.parameter(String.class, "sf");
            cquery.select(root).where(cb.equal(root.get("stringField"), param));

            emf.addNamedQuery("collided_param_query", em.createQuery(cquery));

            OpenJPAQuery<AllFieldTypes> replay = (OpenJPAQuery<AllFieldTypes>) em.createNamedQuery(
                "collided_param_query", AllFieldTypes.class);
            String rendering = replay.getQueryString();
            assertEquals(1, replay.setParameter("sf", "cached1").getResultList().size());

            for (int i = 0; i < 2; i++) {
                assertEquals(1, em.createQuery(rendering, AllFieldTypes.class)
                    .setParameter("sf", "cached1").getResultList().size());
            }

            OpenJPAQuery<AllFieldTypes> withNull = (OpenJPAQuery<AllFieldTypes>) em.createNamedQuery(
                "collided_param_query", AllFieldTypes.class);
            List<AllFieldTypes> result = withNull.setParameter("sf", null).getResultList();
            assertEquals(OpenJPACriteriaBuilder.LANG_CRITERIA, withNull.getLanguage());
            assertEquals(0, result.size());
        } finally {
            em.close();
        }
    }

    /**
     * A subquery is rendered without the enclosing parenthesis by the CQL
     * toString().
     */
    public void testReplayWithSubqueryIsNotCachedByItsRendering() {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<AllFieldTypes> cquery = cb.createQuery(AllFieldTypes.class);
            Root<AllFieldTypes> root = cquery.from(AllFieldTypes.class);
            Subquery<Integer> sub = cquery.subquery(Integer.class);
            Root<AllFieldTypes> subRoot = sub.from(AllFieldTypes.class);
            sub.select(cb.max(subRoot.<Integer>get("intField")));
            cquery.select(root).where(cb.equal(root.get("intField"), sub));

            emf.addNamedQuery("cached_subquery_query", em.createQuery(cquery));

            for (int i = 0; i < 2; i++) {
                TypedQuery<AllFieldTypes> replay = em.createNamedQuery(
                    "cached_subquery_query", AllFieldTypes.class);
                List<AllFieldTypes> result = replay.getResultList();
                assertEquals("replay " + i, 1, result.size());
                assertEquals(99, result.get(0).getIntField());
            }
        } finally {
            em.close();
        }
    }
}

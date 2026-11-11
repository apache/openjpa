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
package org.apache.openjpa.persistence.jpql.literals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.Test;

/**
 * String and character values which contain a quote or a backslash have to
 * reach the database unchanged, also when the query writes them into the SQL
 * text as literals: with the {@link QueryHints#HINT_USE_LITERAL_IN_SQL} hint,
 * and for the values of CASE, COALESCE and NULLIF, which are always written
 * as literals.
 */
public class TestStringLiteralValues extends SingleEMFTestCase {

    private static final Object[][] ROWS = {
        { 1, "abc", 'a' },
        { 2, "abc\\'", '\\' },
        { 3, "abc''", '\'' },
        { 4, "O'Brien", 'o' },
        { 5, "a\\b", 'b' },
        { 6, "trail\\", 't' },
        { 7, "trail", 'u' },
        { 8, "100%", 'p' },
        { 9, "1000", 'q' },
        { 10, "a_c", 'r' },
        { 11, "abc\\%", 's' },
    };

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, StringValue.class, "openjpa.jdbc.QuerySQLCache", "false");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (Object[] row : ROWS) {
                em.persist(new StringValue((Integer) row[0], (String) row[1], (Character) row[2]));
            }
            em.getTransaction().commit();
        }
    }

    @Test
    public void testEqualsLiteralInJPQL() {
        try (EntityManager em = emf.createEntityManager()) {
            for (Object[] row : ROWS) {
                String jpql = "SELECT e.id FROM StringValue e WHERE e.name = " + toJPQLLiteral((String) row[1]);
                TypedQuery<Integer> q = em.createQuery(jpql, Integer.class);
                q.setHint(QueryHints.HINT_USE_LITERAL_IN_SQL, true);
                assertIds(jpql, q.getResultList(), (Integer) row[0]);
            }
        }
    }

    @Test
    public void testEqualsValueInCriteria() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            for (Object[] row : ROWS) {
                CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
                Root<StringValue> e = cq.from(StringValue.class);
                cq.select(e.get("id")).where(cb.equal(e.get("name"), row[1]));
                TypedQuery<Integer> q = em.createQuery(cq);
                q.setHint(QueryHints.HINT_USE_LITERAL_IN_SQL, true);
                assertIds("name = " + row[1], q.getResultList(), (Integer) row[0]);
            }
        }
    }

    @Test
    public void testEqualsCharacterInCriteria() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            for (Object[] row : ROWS) {
                CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
                Root<StringValue> e = cq.from(StringValue.class);
                cq.select(e.get("id")).where(cb.equal(e.get("code"), row[2]));
                TypedQuery<Integer> q = em.createQuery(cq);
                q.setHint(QueryHints.HINT_USE_LITERAL_IN_SQL, true);
                assertIds("code = " + row[2], q.getResultList(), (Integer) row[0]);
            }
        }
    }

    @Test
    public void testLikeLiteralInJPQL() {
        assertLikeInJPQL("'100\\%' ESCAPE '\\'", 8);
        assertLikeInJPQL("'a\\_c' ESCAPE '\\'", 10);
        assertLikeInJPQL("'100''%' ESCAPE ''''", 8);
        assertLikeInJPQL("'abc\\%'", 2, 11);
        assertLikeInJPQL("'trail\\'", 6);
        assertLikeInJPQL("'abc\\'''", 2);
        assertLikeInJPQL("'a_c'", 1, 10);
    }

    @Test
    public void testLikeValueInCriteria() {
        assertLikeInCriteria("100\\%", '\\', 8);
        assertLikeInCriteria("a\\_c", '\\', 10);
        assertLikeInCriteria("100'%", '\'', 8);
        assertLikeInCriteria("abc\\%", null, 2, 11);
        assertLikeInCriteria("trail\\", null, 6);
        assertLikeInCriteria("abc\\'", null, 2);
        assertLikeInCriteria("a_c", null, 1, 10);
    }

    @Test
    public void testCaseLiteralInJPQL() {
        try (EntityManager em = emf.createEntityManager()) {
            for (Object[] row : ROWS) {
                String jpql = "SELECT CASE WHEN e.id = 1 THEN " + toJPQLLiteral((String) row[1])
                    + " ELSE e.name END FROM StringValue e WHERE e.id = 1";
                List<String> result = em.createQuery(jpql, String.class).getResultList();
                assertEquals(Collections.singletonList(row[1]), result, jpql);
            }
        }
    }

    @Test
    public void testCaseCoalesceAndNullIfValuesInCriteria() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            for (Object[] row : ROWS) {
                String value = (String) row[1];

                CriteriaQuery<String> cq = cb.createQuery(String.class);
                Root<StringValue> e = cq.from(StringValue.class);
                Expression<String> name = e.get("name");
                cq.select(cb.<String> selectCase().when(cb.equal(e.get("id"), 1), value).otherwise(name))
                    .where(cb.equal(e.get("id"), 1));
                assertEquals(Collections.singletonList(value), em.createQuery(cq).getResultList(), "case " + value);

                cq = cb.createQuery(String.class);
                e = cq.from(StringValue.class);
                Expression<String> missing = e.get("missing");
                cq.select(cb.coalesce(missing, value)).where(cb.equal(e.get("id"), 1));
                assertEquals(Collections.singletonList(value), em.createQuery(cq).getResultList(), "coalesce " + value);

                CriteriaQuery<Integer> iq = cb.createQuery(Integer.class);
                Root<StringValue> r = iq.from(StringValue.class);
                Expression<String> rname = r.get("name");
                iq.select(r.get("id")).where(cb.isNull(cb.nullif(rname, value)));
                assertIds("nullif " + value, em.createQuery(iq).getResultList(), (Integer) row[0]);
            }
        }
    }

    private void assertLikeInJPQL(String pattern, Integer... expected) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT e.id FROM StringValue e WHERE e.name LIKE " + pattern;
            TypedQuery<Integer> q = em.createQuery(jpql, Integer.class);
            q.setHint(QueryHints.HINT_USE_LITERAL_IN_SQL, true);
            assertIds(jpql, q.getResultList(), expected);
        }
    }

    private void assertLikeInCriteria(String pattern, Character escape, Integer... expected) {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
            Root<StringValue> e = cq.from(StringValue.class);
            Expression<String> name = e.get("name");
            Predicate like = escape == null ? cb.like(name, pattern) : cb.like(name, pattern, escape);
            cq.select(e.get("id")).where(like);
            TypedQuery<Integer> q = em.createQuery(cq);
            q.setHint(QueryHints.HINT_USE_LITERAL_IN_SQL, true);
            assertIds("like " + pattern + " escape " + escape, q.getResultList(), expected);
        }
    }

    private static void assertIds(String message, List<Integer> actual, Integer... expected) {
        List<Integer> sorted = new ArrayList<>(actual);
        Collections.sort(sorted);
        assertEquals(Arrays.asList(expected), sorted, message);
    }

    private static String toJPQLLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}

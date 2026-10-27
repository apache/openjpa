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
package org.apache.openjpa.persistence.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Compares JPQL parameters with each other. The parser can not tell the type
 * of such a parameter, so databases such as Derby, which reject a comparison
 * of two untyped parameter markers, need a cast based on the bound values.
 */
public class TestUntypedParameterComparison extends SingleEMFTestCase {

    private static final String EQUAL = "SELECT s FROM simple s WHERE :a = :b";

    @Override
    public void setUp() {
        super.setUp(SimpleEntity.class, CLEAR_TABLES);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new SimpleEntity("foo", "bar"));
            em.getTransaction().commit();
        }
    }

    public void testEqualStrings() {
        assertMatches(true, EQUAL, "abc", "abc");
        assertMatches(false, EQUAL, "abc", "xyz");
    }

    public void testEqualIntegers() {
        assertMatches(true, EQUAL, 42, 42);
        assertMatches(false, EQUAL, 42, 7);
    }

    public void testEqualLongs() {
        assertMatches(true, EQUAL, 42L, 42L);
        assertMatches(false, EQUAL, 42L, 7L);
    }

    public void testEqualDoubles() {
        assertMatches(true, EQUAL, 1.5d, 1.5d);
        assertMatches(false, EQUAL, 1.5d, 2.5d);
    }

    public void testEqualBigDecimals() {
        assertMatches(true, EQUAL, new BigDecimal("1.25"), new BigDecimal("1.25"));
        assertMatches(false, EQUAL, new BigDecimal("1.25"), new BigDecimal("2.5"));
    }

    public void testEqualDates() {
        assertMatches(true, EQUAL, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1));
        assertMatches(false, EQUAL, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 2));
    }

    public void testEqualBooleans() {
        assertMatches(true, EQUAL, true, true);
        assertMatches(false, EQUAL, true, false);
    }

    public void testNotEqual() {
        String jpql = "SELECT s FROM simple s WHERE :a <> :b";
        assertMatches(true, jpql, "abc", "xyz");
        assertMatches(false, jpql, "abc", "abc");
        assertMatches(true, jpql, 1, 2);
        assertMatches(false, jpql, 1, 1);
    }

    public void testLessAndGreater() {
        assertMatches(true, "SELECT s FROM simple s WHERE :a < :b", 9, 10);
        assertMatches(false, "SELECT s FROM simple s WHERE :a < :b", 10, 9);
        assertMatches(true, "SELECT s FROM simple s WHERE :a > :b", "b", "a");
        assertMatches(false, "SELECT s FROM simple s WHERE :a > :b", "a", "b");
        assertMatches(true, "SELECT s FROM simple s WHERE :a <= :b",
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1));
        assertMatches(false, "SELECT s FROM simple s WHERE :a >= :b",
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 2));
    }

    public void testBetween() {
        String jpql = "SELECT s FROM simple s WHERE :a BETWEEN :b AND :c";
        assertMatches(true, jpql, 5, 1, 10);
        assertMatches(false, jpql, 50, 1, 10);
        assertMatches(true, jpql, "m", "a", "z");
        assertMatches(false, jpql, "m", "n", "z");
    }

    /**
     * The first parameter occurs twice in the SQL with the second one in
     * between, and a cached query must set both occurrences to the value of a
     * later execution. Databases that cast the parameters do not cache this
     * query, see TestSQLBuffer for the positions on every database.
     */
    public void testParameterBetweenParameterAndPath() {
        String jpql = "SELECT s FROM simple s WHERE :a BETWEEN :b AND s.name";
        for (int i = 0; i < 2; i++) {
            assertMatches(true, jpql, "cat", "bar");
            assertMatches(false, jpql, "zebra", "bar");
            assertMatches(false, jpql, "cat", "dog");
        }
    }

    /**
     * JPQL does not accept a parameter on the left of IN, so build the
     * comparison with untyped criteria parameters instead.
     */
    public void testIn() {
        assertInMatches(true, "abc", "xyz", "abc");
        assertInMatches(false, "abc", "xyz", "uvw");
        assertInMatches(true, 3, 2, 3);
        assertInMatches(false, 3, 2, 4);
    }

    public void testEqualUntypedCriteriaParameters() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntity> q = cb.createQuery(SimpleEntity.class);
            Root<SimpleEntity> root = q.from(SimpleEntity.class);
            ParameterExpression<Object> left = cb.parameter(Object.class);
            ParameterExpression<Object> right = cb.parameter(Object.class);
            q.select(root).where(cb.equal(left, right));

            TypedQuery<SimpleEntity> query = em.createQuery(q);
            query.setParameter(left, 42);
            query.setParameter(right, 42);
            assertEquals(1, query.getResultList().size());

            query.setParameter(right, 7);
            assertEquals(0, query.getResultList().size());
        }
    }

    /**
     * Executes the same query object with values of another type, so a cast
     * chosen for the first values must not be reused for the second.
     */
    public void testReexecuteWithOtherType() {
        String jpql = "SELECT s FROM simple s WHERE :a < :b";
        try (EntityManager em = emf.createEntityManager()) {
            Query q = em.createQuery(jpql);
            q.setParameter("a", "10");
            q.setParameter("b", "9");
            assertEquals(1, q.getResultList().size());

            q.setParameter("a", 10);
            q.setParameter("b", 9);
            assertEquals(0, q.getResultList().size());

            q.setParameter("a", 9);
            q.setParameter("b", 10);
            assertEquals(1, q.getResultList().size());
        }
    }

    /**
     * Executes the same JPQL string in new queries with values of another
     * type, which goes through the prepared query cache.
     */
    public void testRecreateWithOtherType() {
        String jpql = "SELECT s FROM simple s WHERE :a < :b";
        for (int i = 0; i < 2; i++) {
            assertMatches(true, jpql, "10", "9");
            assertMatches(false, jpql, 10, 9);
            assertMatches(true, jpql, 9, 10);
            assertMatches(false, jpql, "9", "10");
        }
    }

    /**
     * Executes the same JPQL string with a comparison of parameters in the
     * select list in new queries with values of another type, so the cast of
     * the first execution must not be reused from the prepared query cache.
     */
    public void testCaseInSelectWithOtherType() {
        String jpql = "SELECT CASE WHEN :a < :b THEN s.name ELSE s.value END FROM simple s";
        for (int i = 0; i < 2; i++) {
            assertCaseResult("foo", jpql, "10", "9");
            assertCaseResult("bar", jpql, 10, 9);
            assertCaseResult("foo", jpql, 9, 10);
            assertCaseResult("bar", jpql, "9", "10");
        }
    }

    private void assertInMatches(boolean expected, Object value, Object first, Object second) {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntity> q = cb.createQuery(SimpleEntity.class);
            Root<SimpleEntity> root = q.from(SimpleEntity.class);
            ParameterExpression<Object> a = cb.parameter(Object.class);
            ParameterExpression<Object> b = cb.parameter(Object.class);
            ParameterExpression<Object> c = cb.parameter(Object.class);
            q.select(root).where(a.in(b, c));

            TypedQuery<SimpleEntity> query = em.createQuery(q);
            query.setParameter(a, value);
            query.setParameter(b, first);
            query.setParameter(c, second);
            assertEquals(value + " IN (" + first + ", " + second + ")",
                expected ? 1 : 0, query.getResultList().size());
        }
    }

    private void assertCaseResult(String expected, String jpql, Object a, Object b) {
        try (EntityManager em = emf.createEntityManager()) {
            Query q = em.createQuery(jpql);
            q.setParameter("a", a);
            q.setParameter("b", b);
            assertEquals(jpql + " with [" + a + ", " + b + "]",
                expected, q.getSingleResult());
        }
    }

    private void assertMatches(boolean expected, String jpql, Object... values) {
        try (EntityManager em = emf.createEntityManager()) {
            Query q = em.createQuery(jpql);
            for (int i = 0; i < values.length; i++) {
                q.setParameter(String.valueOf((char) ('a' + i)), values[i]);
            }
            assertEquals(jpql + " with " + Arrays.toString(values),
                expected ? 1 : 0, q.getResultList().size());
        }
    }
}

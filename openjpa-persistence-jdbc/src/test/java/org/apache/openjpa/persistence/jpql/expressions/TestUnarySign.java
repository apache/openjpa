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
package org.apache.openjpa.persistence.jpql.expressions;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A sign in front of an arithmetic primary, as in
 * {@code arithmetic_factor ::= [{+ | -}] arithmetic_primary}, evaluated by the
 * database and in memory. The result has the type of the operand.
 */
public class TestUnarySign extends SingleEMFTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, SignedOperands.class);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new SignedOperands(1, 7, 3, (short) 2, 10L, new BigDecimal("25"), 1.5));
            em.persist(new SignedOperands(2, -4, 5, (short) -3, -20L, new BigDecimal("-15"), -0.5));
            em.getTransaction().commit();
        }
    }

    public void testUnaryMinusOnPath() {
        Object[] row = single("SELECT -n.intA, -n.intB, -n.shortA, -n.longA, -n.decA, -n.doubleA "
            + "FROM SignedOperands n WHERE n.id = 1");
        assertValue(Integer.valueOf(-7), row[0]);
        assertValue(Integer.valueOf(-3), row[1]);
        assertValue(Integer.valueOf(-2), row[2]);
        assertValue(Long.valueOf(-10), row[3]);
        assertDecimal(new BigDecimal("-25"), row[4]);
        assertValue(Double.valueOf(-1.5), row[5]);
    }

    public void testUnaryPlusOnPath() {
        Object[] row = single("SELECT +n.intA, +n.longA, +n.decA FROM SignedOperands n WHERE n.id = 1");
        assertValue(Integer.valueOf(7), row[0]);
        assertValue(Long.valueOf(10), row[1]);
        assertDecimal(new BigDecimal("25"), row[2]);
    }

    public void testUnaryMinusOnNegativeValue() {
        Object[] row = single("SELECT -n.intA, -n.longA FROM SignedOperands n WHERE n.id = 2");
        assertValue(Integer.valueOf(4), row[0]);
        assertValue(Long.valueOf(20), row[1]);
    }

    public void testUnaryMinusOnParenthesizedExpression() {
        Object[] row = single("SELECT -(n.intA + n.intB), -(n.intA - n.longA), -n.intA * 2, "
            + "n.intA - -n.intB FROM SignedOperands n WHERE n.id = 1");
        assertValue(Integer.valueOf(-10), row[0]);
        assertValue(Long.valueOf(3), row[1]);
        assertValue(Integer.valueOf(-14), row[2]);
        assertValue(Integer.valueOf(10), row[3]);
    }

    public void testUnaryMinusOnCase() {
        Object[] row = single("SELECT -CASE WHEN n.intA > 0 THEN n.intA ELSE n.intB END, "
            + "-ABS(n.intA) FROM SignedOperands n WHERE n.id = 2");
        assertValue(Integer.valueOf(-5), row[0]);
        assertValue(Integer.valueOf(-4), row[1]);
    }

    public void testUnaryMinusOnAggregate() {
        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = (Object[]) em.createQuery("SELECT -SUM(n.intA), -MAX(n.longA) "
                + "FROM SignedOperands n HAVING -SUM(n.intA) < 0").getSingleResult();
            assertValue(Long.valueOf(-3), row[0]);
            assertValue(Long.valueOf(-10), row[1]);
        }
    }

    public void testUnaryMinusInWhere() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Long> ids = em.createQuery("SELECT n.id FROM SignedOperands n WHERE -n.intA < 0",
                Long.class).getResultList();
            assertEquals(List.of(1L), ids);
            ids = em.createQuery("SELECT n.id FROM SignedOperands n WHERE n.intA = -:value",
                Long.class).setParameter("value", 4).getResultList();
            assertEquals(List.of(2L), ids);
            ids = em.createQuery("SELECT n.id FROM SignedOperands n WHERE -n.decA > 0",
                Long.class).getResultList();
            assertEquals(List.of(2L), ids);
        }
    }

    public void testUnaryMinusOnParameterKeepsType() {
        assertValue(Long.valueOf(-7), selectNegated(7L));
        assertValue(Integer.valueOf(-4), selectNegated(4));
        assertValue(Long.valueOf(-5000000000L), selectNegated(5000000000L));
        assertValue(Double.valueOf(-2.5), selectNegated(2.5d));
        assertDecimal(new BigDecimal("-2.5"), selectNegated(new BigDecimal("2.5")));
        assertValue(Long.valueOf(-7), selectNegated(7L));
        // the same query again with an int, after it ran with other types
        assertValue(Integer.valueOf(-4), selectNegated(4));
    }

    public void testUnaryMinusOnParameterInWhere() {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT n.id FROM SignedOperands n WHERE n.doubleA = -:value";
            assertEquals(List.of(2L), em.createQuery(jpql, Long.class)
                .setParameter("value", 0.5d).getResultList());
            assertEquals(List.of(1L), em.createQuery(jpql, Long.class)
                .setParameter("value", -1.5d).getResultList());
            assertEquals(List.of(2L), em.createQuery("SELECT n.id FROM SignedOperands n "
                + "WHERE n.decA = -:value", Long.class)
                .setParameter("value", new BigDecimal("15")).getResultList());
            assertEquals(List.of(), em.createQuery("SELECT n.id FROM SignedOperands n "
                + "WHERE n.decA = -:value", Long.class)
                .setParameter("value", new BigDecimal("15.4")).getResultList());
            assertEquals(List.of(2L), em.createQuery("SELECT n.id FROM SignedOperands n "
                + "WHERE n.longA = -:value", Long.class)
                .setParameter("value", 20L).getResultList());
            assertEquals(List.of(1L, 2L), em.createQuery("SELECT n.id FROM SignedOperands n "
                + "WHERE n.longA > -?1 ORDER BY n.id", Long.class)
                .setParameter(1, 5000000000L).getResultList());
        }
    }

    public void testUnaryMinusInOrderBy() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Long> ids = em.createQuery("SELECT n.id FROM SignedOperands n ORDER BY -n.intA",
                Long.class).getResultList();
            assertEquals(List.of(1L, 2L), ids);
            ids = em.createQuery("SELECT n.id FROM SignedOperands n ORDER BY -n.intA DESC",
                Long.class).getResultList();
            assertEquals(List.of(2L, 1L), ids);
        }
    }

    public void testSignedLiterals() {
        Object[] row = single("SELECT n.intA - -1, n.intA -1, n.intA - - 1, -1, n.intA + -1.5 "
            + "FROM SignedOperands n WHERE n.id = 1");
        assertValue(Integer.valueOf(8), row[0]);
        assertValue(Integer.valueOf(6), row[1]);
        assertValue(Integer.valueOf(8), row[2]);
        assertValue(Integer.valueOf(-1), row[3]);
        assertDecimal(new BigDecimal("5.5"), row[4]);
    }

    public void testUnaryMinusInMemory() {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> candidates = em.createQuery("SELECT n FROM SignedOperands n WHERE n.id = 1")
                .getResultList();
            Object[] row = (Object[]) inMemory(em, candidates, "SELECT -n.intA, -n.shortA, -n.longA, "
                + "-n.decA, -n.doubleA, -(n.intA + n.intB), +n.intA FROM SignedOperands n").get(0);
            assertValue(Integer.valueOf(-7), row[0]);
            assertValue(Integer.valueOf(-2), row[1]);
            assertValue(Long.valueOf(-10), row[2]);
            assertDecimal(new BigDecimal("-25"), row[3]);
            assertValue(Double.valueOf(-1.5), row[4]);
            assertValue(Integer.valueOf(-10), row[5]);
            assertValue(Integer.valueOf(7), row[6]);

            assertEquals(1, inMemory(em, candidates,
                "SELECT n FROM SignedOperands n WHERE -n.intA < 0").size());
            assertEquals(0, inMemory(em, candidates,
                "SELECT n FROM SignedOperands n WHERE -n.intA > 0").size());
        }
    }

    public void testUnaryMinusInMemoryWithParameterAndCase() {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> candidates = em.createQuery("SELECT n FROM SignedOperands n").getResultList();
            org.apache.openjpa.persistence.QueryImpl<?> q = query(em, candidates,
                "SELECT n.id FROM SignedOperands n WHERE n.doubleA = -:value");
            assertEquals(List.of(2L), q.setParameter("value", 0.5d).getResultList());

            q = query(em, candidates, "SELECT -:value FROM SignedOperands n WHERE n.id = 1");
            assertValue(Long.valueOf(-7), q.setParameter("value", 7L).getSingleResult());
            assertValue(Double.valueOf(-2.5), q.setParameter("value", 2.5d).getSingleResult());

            assertEquals(List.of(-5), inMemory(em, candidates, "SELECT -CASE WHEN n.intA > 0 "
                + "THEN n.intA ELSE n.intB END FROM SignedOperands n WHERE n.id = 2"));
            assertEquals(List.of(1L, 2L), inMemory(em, candidates,
                "SELECT n.id FROM SignedOperands n ORDER BY -n.intA"));
        }
    }

    private Object selectNegated(Object value) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT -:value FROM SignedOperands n WHERE n.id = 1")
                .setParameter("value", value).getSingleResult();
        }
    }

    private Object[] single(String jpql) {
        try (EntityManager em = emf.createEntityManager()) {
            return (Object[]) em.createQuery(jpql).getSingleResult();
        }
    }

    private static List<?> inMemory(EntityManager em, List<?> candidates, String jpql) {
        return query(em, candidates, jpql).getResultList();
    }

    private static org.apache.openjpa.persistence.QueryImpl<?> query(EntityManager em,
        List<?> candidates, String jpql) {
        org.apache.openjpa.persistence.QueryImpl<?> q =
            (org.apache.openjpa.persistence.QueryImpl<?>) em.createQuery(jpql);
        ((QueryImpl) q.getDelegate()).setCandidateCollection(candidates);
        return q;
    }

    private static void assertValue(Object expected, Object actual) {
        assertEquals(expected.getClass(), actual == null ? null : actual.getClass());
        assertEquals(expected, actual);
    }

    private static void assertDecimal(BigDecimal expected, Object actual) {
        assertEquals(BigDecimal.class, actual.getClass());
        assertEquals(expected + " <> " + actual, 0, expected.compareTo((BigDecimal) actual));
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Arithmetic with a null operand evaluated in memory yields null, as it does
 * in the database, instead of treating the null operand as zero.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-3050">OPENJPA-3050</A>
 */
class TestNullArithmeticInMemory extends SingleEMFTestCase {

    private static final String ARITHMETIC = "SELECT -n.intB, 0 - n.intB, n.intA + n.intB, "
        + "n.intA * n.intB, n.intA / n.intB, MOD(n.intA, n.intB), n.longA - n.intB, "
        + "-n.decA, n.decA + 1, n.doubleA * n.decA FROM SignedOperands n WHERE n.id = 1";

    @BeforeEach
    public void setUp() {
        super.setUp(CLEAR_TABLES, SignedOperands.class);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new SignedOperands(1, 7, null, (short) 2, 10L, null, 1.5));
            em.persist(new SignedOperands(2, 7, 0, (short) 2, 10L, BigDecimal.ONE, 1.5));
            em.getTransaction().commit();
        }
    }

    @Test
    void arithmeticWithNullOperandInDatabase() {
        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = (Object[]) em.createQuery(ARITHMETIC).getSingleResult();
            assertAllNull(row);
        }
    }

    @Test
    void arithmeticWithNullOperandInMemory() {
        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = (Object[]) inMemory(em, ARITHMETIC).get(0);
            assertAllNull(row);
        }
    }

    @Test
    void numericFunctionsWithNullOperandInMemory() {
        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = (Object[]) inMemory(em, "SELECT ABS(n.intB), SQRT(n.intB), "
                + "SIGN(n.intB), CEILING(n.decA), FLOOR(n.decA), EXP(n.decA), LN(n.decA), "
                + "POWER(n.intA, n.intB), ROUND(n.decA, 1) FROM SignedOperands n "
                + "WHERE n.id = 1").get(0);
            assertAllNull(row);
        }
    }

    @Test
    void nullResultInsideCoalesceInMemory() {
        try (EntityManager em = emf.createEntityManager()) {
            // a function nested in another expression must return null itself,
            // at the top level a failure on null is turned into null anyway
            Object[] row = (Object[]) inMemory(em, "SELECT COALESCE(n.intB + 1, 5), "
                + "COALESCE(-n.intB, 6), COALESCE(ABS(n.intB), 7), COALESCE(SQRT(n.decA), 8), "
                + "COALESCE(SIGN(n.intB), 9), COALESCE(CEILING(n.decA), 10), "
                + "COALESCE(FLOOR(n.decA), 11), COALESCE(EXP(n.decA), 12), "
                + "COALESCE(LN(n.decA), 13) FROM SignedOperands n WHERE n.id = 1").get(0);
            for (int i = 0; i < row.length; i++) {
                String column = "column " + i + " of " + Arrays.toString(row);
                assertNotNull(row[i], column);
                assertEquals(i + 5, ((Number) row[i]).intValue(), column);
            }
        }
    }

    @Test
    void nullOperandDoesNotMatchInMemory() {
        try (EntityManager em = emf.createEntityManager()) {
            // with the null operand taken as zero, the row with intB = null
            // matched just like the row with intB = 0
            assertEquals(List.of(2L), em.createQuery("SELECT m.id FROM SignedOperands m "
                + "WHERE m.intA + m.intB = 7").getResultList());
            assertEquals(List.of(2L), inMemory(em, "SELECT n.id FROM SignedOperands n "
                + "WHERE n.intA + n.intB = 7"));
            assertEquals(List.of(2L), inMemory(em, "SELECT n.id FROM SignedOperands n "
                + "WHERE -n.intB = 0"));
            assertEquals(List.of(2L), inMemory(em, "SELECT n.id FROM SignedOperands n "
                + "WHERE n.decA * 2 > 1"));
        }
    }

    @Test
    void aggregatesSkipNullInMemory() {
        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = (Object[]) inMemory(em, "SELECT SUM(n.intB), AVG(n.decA), "
                + "SUM(n.intA) FROM SignedOperands n").get(0);
            assertEquals(0L, ((Number) row[0]).longValue());
            assertEquals(0, BigDecimal.ONE.compareTo(new BigDecimal(row[1].toString())));
            assertEquals(14L, ((Number) row[2]).longValue());
        }
    }

    /**
     * An update over a candidate collection evaluates the new values with the
     * in-memory expressions.
     */
    @Test
    void bulkUpdateOverCandidatesKeepsNull() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            List<?> candidates = em.createQuery("SELECT n FROM SignedOperands n").getResultList();
            org.apache.openjpa.persistence.QueryImpl<?> q = query(em, candidates,
                "UPDATE SignedOperands n SET n.intB = n.intB - 1, n.decA = -n.decA");
            assertEquals(2, q.executeUpdate());
            em.getTransaction().commit();
        }
        assertStored(1, null, null);
        assertStored(2, -1, new BigDecimal("-1"));
    }

    /**
     * A bulk update the database dictionary cannot express is executed in
     * memory by the JDBC store query.
     */
    @Test
    void bulkUpdateInMemoryByStoreKeepsNull() {
        DBDictionary dict = getDBDictionary();
        boolean supportsSubselect = dict.supportsSubselect;
        dict.supportsSubselect = false;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(2, em.createQuery("UPDATE SignedOperands n "
                + "SET n.intB = n.intB + 1, n.decA = n.decA * 2 WHERE n.intA = 7")
                .executeUpdate());
            assertEquals(2, em.createQuery("UPDATE SignedOperands n "
                + "SET n.intB = ABS(n.intB) WHERE n.intA = 7").executeUpdate());
            // the primitive field cannot hold null and gets 0
            assertEquals(1, em.createQuery("UPDATE SignedOperands n "
                + "SET n.doubleA = SQRT(n.intB) WHERE n.id = 1").executeUpdate());
            em.getTransaction().commit();
        } finally {
            dict.supportsSubselect = supportsSubselect;
        }
        assertStored(1, null, null);
        assertStored(2, 1, new BigDecimal("2"));
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(0D, em.find(SignedOperands.class, 1L).getDoubleA());
        }
    }

    private void assertStored(long id, Integer intB, BigDecimal decA) {
        try (EntityManager em = emf.createEntityManager()) {
            SignedOperands n = em.find(SignedOperands.class, id);
            assertEquals(intB, n.getIntB());
            if (decA == null) {
                assertNull(n.getDecA());
            } else {
                assertEquals(0, decA.compareTo(n.getDecA()));
            }
        }
    }

    private static void assertAllNull(Object[] row) {
        for (int i = 0; i < row.length; i++) {
            assertNull(row[i], "column " + i + " of " + Arrays.toString(row));
        }
    }

    private static List<?> inMemory(EntityManager em, String jpql) {
        List<?> candidates = em.createQuery("SELECT n FROM SignedOperands n ORDER BY n.id")
            .getResultList();
        return query(em, candidates, jpql).getResultList();
    }

    private static org.apache.openjpa.persistence.QueryImpl<?> query(EntityManager em,
        List<?> candidates, String jpql) {
        org.apache.openjpa.persistence.QueryImpl<?> q =
            (org.apache.openjpa.persistence.QueryImpl<?>) em.createQuery(jpql);
        ((QueryImpl) q.getDelegate()).setCandidateCollection(candidates);
        return q;
    }
}

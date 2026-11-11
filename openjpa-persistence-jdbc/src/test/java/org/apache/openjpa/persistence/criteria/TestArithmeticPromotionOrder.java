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
package org.apache.openjpa.persistence.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that JPQL arithmetic on a BigDecimal or BigInteger operand and a
 * Float or Double operand has the floating point result type given by the
 * numeric promotion rules of the specification, in the database and in
 * memory, and that JPQL and the Criteria API agree on the result type.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-3049">OPENJPA-3049</A>
 */
public class TestArithmeticPromotionOrder extends SingleEMFTestCase {
    private CriteriaBuilder cb;
    private EntityManager em;

    @BeforeEach
    public void setUp() {
        // a query evaluated in memory must not be served from the SQL cache
        super.setUp(CLEAR_TABLES, NumericOperands.class, "openjpa.jdbc.QuerySQLCache", "false");
        cb = emf.getCriteriaBuilder();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        NumericOperands n = new NumericOperands(1, 7, 3, (short) 2, 10L, 4L, new BigDecimal("2"));
        n.setFloatA(0.5f);
        n.setDoubleA(1.5);
        n.setBigIntA(BigInteger.valueOf(3));
        em.persist(n);
        em.getTransaction().commit();
        em.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (em != null && em.isOpen()) {
            em.close();
        }
        super.tearDown();
    }

    @Test
    public void testBigDecimalAndDouble() {
        assertArithmetic(Double.valueOf(3.5), "n.decA + n.doubleA",
            r -> cb.sum(r.<BigDecimal> get("decA"), r.<Double> get("doubleA")));
        assertArithmetic(Double.valueOf(3.0), "n.doubleA * n.decA",
            r -> cb.prod(r.<Double> get("doubleA"), r.<BigDecimal> get("decA")));
        assertArithmetic(Double.valueOf(0.5), "n.decA - n.doubleA",
            r -> cb.diff(r.<BigDecimal> get("decA"), r.<Double> get("doubleA")));
        assertArithmetic(Double.valueOf(0.75), "n.doubleA / n.decA",
            r -> cb.quot(r.<Double> get("doubleA"), r.<BigDecimal> get("decA")));
    }

    @Test
    public void testBigDecimalAndFloat() {
        assertArithmetic(Float.valueOf(2.5f), "n.decA + n.floatA",
            r -> cb.sum(r.<BigDecimal> get("decA"), r.<Float> get("floatA")));
        assertArithmetic(Float.valueOf(1.0f), "n.floatA * n.decA",
            r -> cb.prod(r.<Float> get("floatA"), r.<BigDecimal> get("decA")));
    }

    @Test
    public void testBigIntegerAndFloatingPoint() {
        assertArithmetic(Double.valueOf(4.5), "n.bigIntA * n.doubleA",
            r -> cb.prod(r.<BigInteger> get("bigIntA"), r.<Double> get("doubleA")));
        assertArithmetic(Float.valueOf(3.5f), "n.floatA + n.bigIntA",
            r -> cb.sum(r.<Float> get("floatA"), r.<BigInteger> get("bigIntA")));
    }

    @Test
    public void testBigDecimalAndIntegralOperands() {
        assertArithmetic(new BigDecimal("9"), "n.decA + n.intA",
            r -> cb.sum(r.<BigDecimal> get("decA"), r.<Integer> get("intA")));
        assertArithmetic(new BigDecimal("20"), "n.longA * n.decA",
            r -> cb.prod(r.<Long> get("longA"), r.<BigDecimal> get("decA")));
        assertArithmetic(new BigDecimal("5"), "n.decA + n.bigIntA",
            r -> cb.sum(r.<BigDecimal> get("decA"), r.<BigInteger> get("bigIntA")));
    }

    /**
     * A JPQL literal with a decimal point and without a suffix is an exact
     * numeric literal of type BigDecimal, like a BigDecimal literal of the
     * Criteria API.
     */
    @Test
    public void testDecimalLiteral() {
        assertArithmetic(new BigDecimal("3.0"), "n.decA * 1.5",
            r -> cb.prod(r.<BigDecimal> get("decA"), new BigDecimal("1.5")));
        assertArithmetic(new BigDecimal("10.5"), "n.intA * 1.5",
            r -> cb.prod(r.<Integer> get("intA"), new BigDecimal("1.5")));
        assertArithmetic(Float.valueOf(0.75f), "n.floatA * 1.5",
            r -> cb.prod(r.<Float> get("floatA"), new BigDecimal("1.5")));
        assertArithmetic(Double.valueOf(4.0), "2.5 + n.doubleA",
            r -> cb.sum(new BigDecimal("2.5"), r.<Double> get("doubleA")));
    }

    /**
     * Compares a BigDecimal and a Double operand as before, as exact values.
     */
    @Test
    public void testComparison() {
        String jpql = "SELECT n.id FROM NumericOperands n WHERE n.decA > n.doubleA "
            + "AND n.decA + n.doubleA = 3.5 AND n.floatA * 1.5 < n.decA";
        assertEquals(List.of(1L), em.createQuery(jpql).getResultList());
        assertEquals(List.of(1L), inMemory(em.createQuery(jpql)).getResultList());
    }

    /**
     * Asserts that the JPQL arithmetic expression and the criteria expression
     * have the expected value and Java type, evaluated by the database and
     * evaluated in memory.
     */
    private void assertArithmetic(Number expected, String jpql,
        Function<Root<NumericOperands>, Expression<?>> criteria) {
        Query q = em.createQuery("SELECT " + jpql + " FROM NumericOperands n");
        assertValue("JPQL " + jpql, expected, q.getSingleResult());
        q = em.createQuery("SELECT " + jpql + " FROM NumericOperands n");
        assertValue("JPQL in memory " + jpql, expected, inMemory(q).getSingleResult());

        CriteriaQuery<Object> c = cb.createQuery();
        Root<NumericOperands> r = c.from(NumericOperands.class);
        Expression<?> e = criteria.apply(r);
        c.select(e);
        assertEquals(expected.getClass(), Filters.wrap(e.getJavaType()), "criteria type " + jpql);
        assertValue("criteria " + jpql, expected, em.createQuery(c).getSingleResult());
        assertValue("criteria in memory " + jpql, expected,
            inMemory(em.createQuery(c)).getSingleResult());
    }

    private static void assertValue(String message, Number expected, Object actual) {
        assertNotNull(actual, message);
        assertEquals(expected.getClass(), actual.getClass(), message + " = " + actual);
        if (expected instanceof BigDecimal) {
            assertEquals(0, ((BigDecimal) expected).compareTo((BigDecimal) actual),
                message + " = " + actual);
        } else {
            assertEquals(expected, actual, message);
        }
    }

    /**
     * Makes the given query evaluate in memory over all stored instances.
     */
    private Query inMemory(Query q) {
        List<?> candidates = em.createQuery("SELECT n FROM NumericOperands n").getResultList();
        ((QueryImpl) ((org.apache.openjpa.persistence.QueryImpl<?>) q).getDelegate())
            .setCandidateCollection(candidates);
        return q;
    }
}

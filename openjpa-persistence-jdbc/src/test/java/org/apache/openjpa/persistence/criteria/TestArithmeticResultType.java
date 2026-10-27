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

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Checks that the value of an arithmetic expression has the Java type given by
 * the numeric promotion rules of the specification, whatever type the database
 * returns for the operation. MySQL, for example, returns BIGINT for the sum of
 * two INTEGER columns.
 */
public class TestArithmeticResultType extends SingleEMFTestCase {
    private CriteriaBuilder cb;
    private EntityManager em;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, NumericOperands.class);
        cb = emf.getCriteriaBuilder();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new NumericOperands(1, 7, 3, (short) 2, 10L, 4L, new BigDecimal("2")));
        em.getTransaction().commit();
        em.clear();
    }

    @Override
    public void tearDown() throws Exception {
        if (em != null && em.isOpen()) {
            em.close();
        }
        super.tearDown();
    }

    public void testIntegerOperands() {
        assertResult(Integer.valueOf(10), r -> cb.sum(r.get("intA"), r.get("intB")));
        assertResult(Integer.valueOf(4), r -> cb.diff(r.get("intA"), r.get("intB")));
        assertResult(Integer.valueOf(21), r -> cb.prod(r.get("intA"), r.get("intB")));
        assertResult(Integer.valueOf(-7), r -> cb.neg(r.get("intA")));
        assertResult(Integer.valueOf(-3), r -> cb.neg(r.get("intB")));
        assertResult(Integer.valueOf(1), r -> cb.mod(r.get("intA"), r.get("intB")));
        assertResult(Integer.valueOf(4), r -> cb.sum(r.get("shortA"), r.get("shortA")));
    }

    public void testIntegerOperandAndLiteral() {
        assertResult(Integer.valueOf(12), r -> cb.sum(5, r.<Integer> get("intA")));
        assertResult(Integer.valueOf(12), r -> cb.sum(r.<Integer> get("intA"), 5));
        assertResult(Integer.valueOf(-2), r -> cb.diff(5, r.<Integer> get("intA")));
        assertResult(Integer.valueOf(2), r -> cb.diff(r.<Integer> get("intA"), 5));
        assertResult(Integer.valueOf(14), r -> cb.prod(r.<Integer> get("intA"), 2));
        assertResult(Integer.valueOf(14), r -> cb.prod(2, r.<Integer> get("intA")));
    }

    public void testLongOperands() {
        assertResult(Long.valueOf(14), r -> cb.sum(r.get("longA"), r.get("longB")));
        assertResult(Long.valueOf(6), r -> cb.diff(r.get("longA"), r.get("longB")));
        assertResult(Long.valueOf(40), r -> cb.prod(r.get("longA"), r.get("longB")));
        assertResult(Long.valueOf(-10), r -> cb.neg(r.get("longA")));
        assertResult(Long.valueOf(-5), r -> cb.diff(5, r.<Long> get("longA")));
        assertResult(Long.valueOf(15), r -> cb.sum(r.<Long> get("longA"), 5L));
    }

    public void testMixedOperands() {
        assertResult(Long.valueOf(17), r -> cb.sum(r.<Integer> get("intA"), r.<Long> get("longA")));
        assertResult(Long.valueOf(17), r -> cb.sum(r.<Long> get("longA"), r.<Integer> get("intA")));
        assertResult(Long.valueOf(3), r -> cb.diff(r.<Integer> get("intA"), r.<Long> get("longB")));
        assertResult(Long.valueOf(28), r -> cb.prod(r.<Integer> get("intA"), r.<Long> get("longB")));
        assertResult(Long.valueOf(12), r -> cb.sum(r.<Integer> get("intA"), 5L));
        assertResult(Long.valueOf(-2), r -> cb.diff(5L, r.<Integer> get("intA")));
        assertDecimal(new BigDecimal("9"), r -> cb.sum(r.<Integer> get("intA"), r.<BigDecimal> get("decA")));
        assertDecimal(new BigDecimal("9"), r -> cb.sum(r.<BigDecimal> get("decA"), r.<Integer> get("intA")));
        assertDecimal(new BigDecimal("-2"), r -> cb.neg(r.<BigDecimal> get("decA")));
        assertDecimal(new BigDecimal("20"), r -> cb.prod(r.<Long> get("longA"), r.<BigDecimal> get("decA")));
        assertEquals(BigDecimal.class,
            select(r -> cb.quot(r.<Integer> get("intA"), r.<BigDecimal> get("decA"))).getClass());
        assertResult(Double.valueOf(9.5), r -> cb.sum(r.<Integer> get("intA"), 2.5));
        assertResult(Double.valueOf(9.5), r -> cb.sum(2.5, r.<Integer> get("intA")));
        assertResult(Double.valueOf(3.5), r -> cb.prod(r.<Integer> get("intA"), 0.5));
    }

    public void testIntegerDivision() {
        // MySQL returns DECIMAL for the division of two INTEGER columns
        assertResult(Integer.valueOf(2), r -> cb.quot(r.<Integer> get("intA"), r.<Integer> get("intB")));
        assertResult(Long.valueOf(2), r -> cb.quot(r.<Long> get("longA"), r.<Long> get("longB")));
        Object[] row = (Object[]) em.createQuery("SELECT n.intA / n.intB, n.longA / n.longB "
            + "FROM NumericOperands n").getSingleResult();
        assertValue(Integer.valueOf(2), row[0]);
        assertValue(Long.valueOf(2), row[1]);
    }

    public void testJPQLProjection() {
        Object[] row = (Object[]) em.createQuery("SELECT n.intA + n.intB, n.intA - n.intB, "
            + "n.intA * n.intB, n.intA + n.longA, n.longA - n.intB, n.intA - 5, n.intB + n.decA "
            + "FROM NumericOperands n").getSingleResult();
        assertValue(Integer.valueOf(10), row[0]);
        assertValue(Integer.valueOf(4), row[1]);
        assertValue(Integer.valueOf(21), row[2]);
        assertValue(Long.valueOf(17), row[3]);
        assertValue(Long.valueOf(7), row[4]);
        assertValue(Integer.valueOf(2), row[5]);
        assertEquals(BigDecimal.class, row[6].getClass());
        assertEquals("value " + row[6], 0, new BigDecimal("5").compareTo((BigDecimal) row[6]));

        row = (Object[]) em.createQuery("SELECT n.shortA + n.shortA, n.shortA - n.shortA, "
            + "n.shortA * n.shortA, n.shortA / n.shortA, n.shortA + n.intA FROM NumericOperands n")
            .getSingleResult();
        assertValue(Integer.valueOf(4), row[0]);
        assertValue(Integer.valueOf(0), row[1]);
        assertValue(Integer.valueOf(4), row[2]);
        assertValue(Integer.valueOf(1), row[3]);
        assertValue(Integer.valueOf(9), row[4]);
    }

    public void testConstructorWithIntParameters() {
        CriteriaQuery<IntPair> q = cb.createQuery(IntPair.class);
        Root<NumericOperands> r = q.from(NumericOperands.class);
        q.select(cb.construct(IntPair.class, cb.diff(r.get("intA"), r.get("intB")),
            cb.neg(r.<Integer> get("intA"))));
        IntPair pair = em.createQuery(q).getSingleResult();
        assertEquals(4, pair.first);
        assertEquals(-7, pair.second);

        pair = em.createQuery("SELECT NEW " + IntPair.class.getName()
            + "(n.intA - n.intB, n.intA + n.intB) FROM NumericOperands n", IntPair.class)
            .getSingleResult();
        assertEquals(4, pair.first);
        assertEquals(10, pair.second);
    }

    public void testComparison() {
        CriteriaQuery<NumericOperands> q = cb.createQuery(NumericOperands.class);
        Root<NumericOperands> r = q.from(NumericOperands.class);
        q.where(cb.equal(cb.neg(r.<Integer> get("intA")), -7),
            cb.equal(cb.diff(5, r.<Integer> get("intA")), -2),
            cb.equal(cb.sum(r.<Integer> get("intA"), r.<Long> get("longA")), 17L));
        assertEquals(1, em.createQuery(q).getResultList().size());

        List<?> result = em.createQuery("SELECT n FROM NumericOperands n "
            + "WHERE n.intA - n.intB = 4 AND n.intA + n.longA > 16").getResultList();
        assertEquals(1, result.size());
    }

    private void assertResult(Number expected, Function<Root<NumericOperands>, Expression<?>> expr) {
        assertValue(expected, select(expr));
    }

    private void assertDecimal(BigDecimal expected, Function<Root<NumericOperands>, Expression<?>> expr) {
        Object value = select(expr);
        assertEquals(BigDecimal.class, value.getClass());
        assertEquals("value " + value, 0, expected.compareTo((BigDecimal) value));
    }

    private void assertValue(Number expected, Object actual) {
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }

    private Object select(Function<Root<NumericOperands>, Expression<?>> expr) {
        CriteriaQuery<Object> q = cb.createQuery();
        Root<NumericOperands> r = q.from(NumericOperands.class);
        q.select(expr.apply(r));
        return em.createQuery(q).getSingleResult();
    }

    /**
     * Result class of a constructor expression with primitive int parameters.
     */
    public static class IntPair {
        private final int first;
        private final int second;

        public IntPair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }
}

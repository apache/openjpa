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
package org.apache.openjpa.jira1794;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * OPENJPA-1794 Verifies the return value of aggregate functions when a query
 * result set is empty. In this set of variations, the compatibility flag is not
 * set so null is expected.
 */
public class TestAggregateFunctions extends SingleEMFTestCase {

    private static final int MAX = 0;
    private static final int MIN = 1;
    private static final int SUM = 2;

    private static final String[] numericAggregateFunctions = { "MAX", "AVG",
            "MIN", "SUM" };

    private static final String[] stringAggregateFunctions = { "MAX", "MIN" };

    private static final String[] numericAttributes = { "ae.pintVal",
            "ae.intVal", "ae.shortVal", "ae.pshortVal", "ae.pintVal",
            "ae.intVal", "ae.plongVal", "ae.longVal", "ae.pfloatVal",
            "ae.floatVal", "ae.pdblVal", "ae.dblVal" };

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, AggEntity.class);
    }

    protected boolean nullResultExpected() {
        return true;
    }

    public void testAggregateJPQL() {
        EntityManager em = emf.createEntityManager();

        // Verify all numeric types for all aggregate functions return null
        // if there is no query result
        verifyResult(em, numericAggregateFunctions, numericAttributes, true);

        // Verify a string for all applicable aggregate functions return null
        // if there is no query result
        verifyResult(em, stringAggregateFunctions,
                new String[] { "ae.stringVal" }, true, true);

        verifyResult(em, stringAggregateFunctions,
                new String[]{"ae.utilDate", "ae.sqlDate"}, true, true);

        // Add a row to the table and re-test
        AggEntity ae = new AggEntity();
        ae.init();
        em.getTransaction().begin();
        em.persist(ae);
        em.getTransaction().commit();

        verifyResult(em, stringAggregateFunctions,  "ae.sqlDate", java.sql.Date.class);

        // Verify all numeric types for all aggregate functions return a
        // non-null value when there is a query result
        verifyResult(em, numericAggregateFunctions, numericAttributes, false);
        // Verify string types for all applicable aggregate functions return a
        // non-null value when there is a query result
        verifyResult(em, stringAggregateFunctions,
                new String[] { "ae.stringVal" }, false);

        verifyResult(em, stringAggregateFunctions,  "ae.utilDate", java.util.Date.class);

        em.close();
    }

    /**
     * if a SUM(CASE statement is used, then the effective type might be different
     */
    public void testAggregateWithCase() {
        EntityManager em = emf.createEntityManager();

        // Add a row to the table and re-test
        em.getTransaction().begin();
        AggEntity ae = new AggEntity();
        ae.init();
        ae.setStringVal("bare");
        em.persist(ae);
        AggEntity ae2 = new AggEntity();
        ae2.init();
        ae2.setStringVal("foot");
        em.persist(ae2);
        em.getTransaction().commit();

        em.getTransaction().begin();
        final TypedQuery<Long> q2 = em.createQuery("select SUM(ae.intVal) from AggEntity AS ae", Long.class);
        final Long sum = q2.getSingleResult();
        assertEquals(2L, (long) sum);

        final TypedQuery<Long> q = em.createQuery("select SUM(CASE ae.stringVal WHEN 'bare' THEN 1 ELSE 0 END) from AggEntity AS ae", Long.class);
        final Long sumC = q.getSingleResult();
        assertEquals(1L, (long) sumC);

        em.getTransaction().commit();

        em.close();
    }

    public void testAggregateCriteria() {
        EntityManager em = emf.createEntityManager();
        Metamodel mm = emf.getMetamodel();
        mm.getEntities();

        Query q = null;
        // Verify all types of criteria query that return a Numeric type
        for (int agg = MAX; agg <= SUM; agg++) {
            CriteriaQuery<Short> cqs = buildNumericCriteriaQuery(em,
                    Short.class, AggEntity_.shortVal, agg);
            q = em.createQuery(cqs);
            verifyQueryResult(q, true);

            cqs = buildNumericCriteriaQuery(em, Short.class,
                    AggEntity_.pshortVal, agg);
            q = em.createQuery(cqs);
            verifyQueryResult(q, true);

            CriteriaQuery<Integer> cqi = buildNumericCriteriaQuery(em,
                    Integer.class, AggEntity_.intVal, agg);
            q = em.createQuery(cqi);
            verifyQueryResult(q, true);

            cqi = buildNumericCriteriaQuery(em, Integer.class,
                    AggEntity_.pintVal, agg);
            q = em.createQuery(cqi);
            verifyQueryResult(q, true);

            CriteriaQuery<Float> cqf = buildNumericCriteriaQuery(em,
                    Float.class, AggEntity_.floatVal, agg);
            q = em.createQuery(cqf);
            verifyQueryResult(q, true);

            cqf = buildNumericCriteriaQuery(em, Float.class,
                    AggEntity_.pfloatVal, agg);
            q = em.createQuery(cqi);
            verifyQueryResult(q, true);

            CriteriaQuery<Double> cqd = buildNumericCriteriaQuery(em,
                    Double.class, AggEntity_.dblVal, agg);
            q = em.createQuery(cqd);
            verifyQueryResult(q, true);

            cqd = buildNumericCriteriaQuery(em, Double.class,
                    AggEntity_.pdblVal, agg);
            q = em.createQuery(cqi);
            verifyQueryResult(q, true);
        }

        // Verify AVG criteria query - it strictly returns type 'Double' so
        // unlike other aggregates,
        // it cannot be handled generically (as Numeric).
        CriteriaQuery<Double> cqd = buildAvgCriteriaQuery(em, Double.class,
                AggEntity_.dblVal);
        q = em.createQuery(cqd);
        verifyQueryResult(q, true);

        cqd = buildAvgCriteriaQuery(em, Double.class, AggEntity_.pdblVal);
        q = em.createQuery(cqd);
        verifyQueryResult(q, true);

        em.close();
    }

    private <T extends Number> CriteriaQuery<T> buildNumericCriteriaQuery(
            EntityManager em, Class<T> type,
            SingularAttribute<AggEntity, T> sa, int at) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(type);
        Root<AggEntity> aer = cq.from(AggEntity.class);
        Path<T> path = aer.get(sa);
        Expression<T> exp = null;
        switch (at) {
        case MAX:
            exp = cb.max(path);
            break;
        case MIN:
            exp = cb.min(path);
            break;
        case SUM:
            exp = cb.sum(path);
            break;
        }
        cq.select(exp);
        return cq;
    }

    private CriteriaQuery<Double> buildAvgCriteriaQuery(EntityManager em,
            Class<Double> type, SingularAttribute<AggEntity, Double> sa) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Double> cq = cb.createQuery(type);
        Root<AggEntity> aer = cq.from(AggEntity.class);
        return cq.select(cb.avg(aer.get(sa)));
    }

    private void verifyResult(EntityManager em, String[] aggregates,
            String[] attributes, boolean expectNull) {
        verifyResult(em, aggregates, attributes, expectNull, false);
    }

    private void verifyResult(EntityManager em, String[] aggregates,
            String[] attributes, boolean expectNull, boolean isString) {
        for (String func : aggregates) {
            for (String attr : attributes) {
                // JPQL with aggregate and aggregate in subselect
                String sql = "SELECT " + func + "(" + attr + ")"
                        + " FROM AggEntity ae WHERE " + attr + " <= "
                        + "(SELECT " + func + "("
                        + attr.replaceFirst("^ae.", "ae2.")
                        + ") FROM AggEntity ae2)";
                Query q = em.createQuery(sql);
                verifyQueryResult(q, expectNull, isString);
            }
        }
    }

    private <T> void verifyResult(EntityManager em, String[] aggregates,
                              String attr, Class<T> expectedType) {
        for (String func : aggregates) {
            // JPQL with aggregate and aggregate in subselect
            String sql = "SELECT " + func + "(" + attr + ")"
                    + " FROM AggEntity ae WHERE " + attr + " <= "
                    + "(SELECT " + func + "("
                    + attr.replaceFirst("^ae.", "ae2.")
                    + ") FROM AggEntity ae2)";
            TypedQuery<T> q = em.createQuery(sql, expectedType);
            T intance = q.getSingleResult();
            assertEquals(intance.getClass(), expectedType);
        }
    }

    private void verifyQueryResult(Query q, boolean emptyRs) {
        verifyQueryResult(q, emptyRs, false);
    }

    private void verifyQueryResult(Query q, boolean emptyRs, boolean isString) {
        Object result = q.getSingleResult();
        if (!emptyRs && !isString) {
            assertNotNull(result);
        } else if (isString || nullResultExpected()) {
            assertNull(result);
        } else {
            assertNotNull(result);
        }
        List<?> resultList = q.getResultList();
        assertEquals(1, resultList.size());
        if (!emptyRs && !isString) {
            assertNotNull(result);
        } else if (isString || nullResultExpected()) {
            assertNull(result);
        } else {
            assertNotNull(result);
        }
    }
}

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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that a parameter used only in the selection, grouping or ordering of a
 * criteria query is registered with the query and can be bound.
 */
public class TestSelectionParameters extends SingleEMFTestCase {
    private CriteriaBuilder cb;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, CompUser.class, Address.class);
        cb = emf.getCriteriaBuilder();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new CompUser("Alice", "c1", null, 30));
            em.persist(new CompUser("Bob", "c2", null, 40));
            em.getTransaction().commit();
        }
    }

    public void testParameterInFunctionOfSelect() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> len = cb.parameter(Integer.class, "len");
        cq.select(cb.substring(u.get(CompUser_.name), cb.literal(1), len));
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery(cq).setParameter("len", 2).getResultList();
            assertEquals(List.of("Al", "Bo"), result);
        }
    }

    public void testParameterInArithmeticOfMultiselect() {
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> add = cb.parameter(Integer.class, "add");
        cq.multiselect(u.get(CompUser_.name), cb.sum(u.get(CompUser_.age), add));
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> result = em.createQuery(cq).setParameter("add", 5).getResultList();
            assertEquals(2, result.size());
            assertEquals("Alice", result.get(0)[0]);
            assertEquals(35, ((Number) result.get(0)[1]).intValue());
            assertEquals("Bob", result.get(1)[0]);
            assertEquals(45, ((Number) result.get(1)[1]).intValue());
        }
    }

    public void testPositionalParameterInSelect() {
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> factor = cb.parameter(Integer.class);
        cq.select(cb.prod(u.get(CompUser_.age), factor));
        cq.where(cb.equal(u.get(CompUser_.name), "Bob"));

        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> result = em.createQuery(cq).setParameter(factor, 2).getResultList();
            assertEquals(1, result.size());
            assertEquals(80, result.get(0).intValue());
        }
    }

    public void testParameterInSelectAndWhere() {
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> add = cb.parameter(Integer.class, "add");
        ParameterExpression<Integer> minAge = cb.parameter(Integer.class, "minAge");
        cq.multiselect(u.get(CompUser_.name), cb.sum(u.get(CompUser_.age), add));
        cq.where(cb.gt(u.get(CompUser_.age), minAge));

        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> result = em.createQuery(cq)
                .setParameter("minAge", 35)
                .setParameter("add", 1)
                .getResultList();
            assertEquals(1, result.size());
            assertEquals("Bob", result.get(0)[0]);
            assertEquals(41, ((Number) result.get(0)[1]).intValue());
        }
    }

    public void testParameterInTuple() {
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<String> suffix = cb.parameter(String.class, "suffix");
        cq.select(cb.tuple(u.get(CompUser_.name).alias("n"),
            cb.concat(u.get(CompUser_.computerName), suffix).alias("c")));
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            List<Tuple> result = em.createQuery(cq).setParameter("suffix", "-x").getResultList();
            assertEquals(2, result.size());
            assertEquals("Alice", result.get(0).get("n"));
            assertEquals("c1-x", result.get(0).get("c"));
            assertEquals("c2-x", result.get(1).get("c"));
        }
    }

    public void testParameterInConstruct() {
        CriteriaQuery<NameAndAge> cq = cb.createQuery(NameAndAge.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> add = cb.parameter(Integer.class, "add");
        cq.select(cb.construct(NameAndAge.class, u.get(CompUser_.name), cb.sum(u.get(CompUser_.age), add)));
        cq.where(cb.equal(u.get(CompUser_.name), "Alice"));

        try (EntityManager em = emf.createEntityManager()) {
            List<NameAndAge> result = em.createQuery(cq).setParameter("add", 10).getResultList();
            assertEquals(1, result.size());
            assertEquals("Alice", result.get(0).name);
            assertEquals(40, result.get(0).age);
        }
    }

    public void testParameterInNestedCompoundSelection() {
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> add = cb.parameter(Integer.class, "add");
        cq.multiselect(u.get(CompUser_.name),
            cb.construct(NameAndAge.class, u.get(CompUser_.name), cb.sum(u.get(CompUser_.age), add)));
        cq.where(cb.equal(u.get(CompUser_.name), "Bob"));

        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> result = em.createQuery(cq).setParameter("add", 3).getResultList();
            assertEquals(1, result.size());
            assertEquals("Bob", result.get(0)[0]);
            NameAndAge dto = (NameAndAge) result.get(0)[1];
            assertEquals("Bob", dto.name);
            assertEquals(43, dto.age);
        }
    }

    public void testParameterInCaseOfSelect() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> limit = cb.parameter(Integer.class, "limit");
        Expression<String> group = cb.<String> selectCase()
            .when(cb.gt(u.get(CompUser_.age), limit), "old")
            .otherwise("young");
        cq.select(group);
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery(cq).setParameter("limit", 35).getResultList();
            assertEquals(List.of("young", "old"), result);
        }
    }

    public void testParameterInCoalesceOfSelect() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<String> fallback = cb.parameter(String.class, "fallback");
        cq.select(cb.coalesce(u.get(CompUser_.computerName), fallback));
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery(cq).setParameter("fallback", "none").getResultList();
            assertEquals(List.of("c1", "c2"), result);
        }
    }

    public void testParameterInGroupByAndOrderByIsRegistered() {
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> group = cb.parameter(Integer.class, "grp");
        ParameterExpression<Integer> order = cb.parameter(Integer.class, "ord");
        cq.multiselect(u.get(CompUser_.name), cb.count(u));
        cq.groupBy(u.get(CompUser_.name), cb.prod(u.get(CompUser_.age), group));
        cq.orderBy(cb.asc(cb.prod(u.get(CompUser_.age), order)));

        assertEquals(2, cq.getParameters().size());
        assertTrue(cq.getParameters().contains(group));
        assertTrue(cq.getParameters().contains(order));

        try (EntityManager em = emf.createEntityManager()) {
            assertNotNull(em.createQuery(cq).getParameter("grp"));
            assertNotNull(em.createQuery(cq).getParameter("ord"));
        }
    }

    public void testParameterInOrderByOnly() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> factor = cb.parameter(Integer.class, "factor");
        cq.select(u.get(CompUser_.name));
        cq.orderBy(cb.asc(cb.prod(u.get(CompUser_.age), factor)));

        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(List.of("Alice", "Bob"),
                em.createQuery(cq).setParameter("factor", 1).getResultList());
            assertEquals(List.of("Bob", "Alice"),
                em.createQuery(cq).setParameter("factor", -1).getResultList());
        }
    }

    public void testReplacedSelectionDropsItsParameter() {
        CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<Integer> factor = cb.parameter(Integer.class, "factor");
        cq.select(cb.prod(u.get(CompUser_.age), factor));
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(List.of(60, 80),
                em.createQuery(cq).setParameter("factor", 2).getResultList());

            cq.select(u.get(CompUser_.age));
            assertTrue(cq.getParameters().isEmpty());
            assertEquals(List.of(30, 40), em.createQuery(cq).getResultList());
        }
    }

    public static class NameAndAge {
        private final String name;
        private final int age;

        public NameAndAge(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}

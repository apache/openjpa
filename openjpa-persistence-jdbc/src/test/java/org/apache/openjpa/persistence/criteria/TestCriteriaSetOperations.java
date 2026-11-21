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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the results of criteria set operations (OPENJPA-3092): a set operation
 * has the result of its first operand, so scalar operands yield their values,
 * multiselect operands yield Object[] rows, constructed operands yield
 * instances of the constructed class, and UNION ALL keeps its duplicates.
 */
class TestCriteriaSetOperations extends SingleEMFTestCase {
    private CriteriaBuilder cb;

    @BeforeEach
    public void setUp() {
        super.setUp(CLEAR_TABLES, CompUser.class, Address.class);
        cb = emf.getCriteriaBuilder();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new CompUser("Ana", "c1", null, 21));
            em.persist(new CompUser("Ben", "c2", null, 26));
            em.persist(new CompUser("Cy", "c3", null, 36));
            em.persist(new CompUser("Di", "c4", null, 41));
            em.getTransaction().commit();
        }
    }

    @Test
    void scalarResults() {
        assertEquals(List.of("Ana", "Cy", "Di"),
            execute(cb.union(namesOlderThan(30), namesEqualTo("Ana"))));
        assertEquals(List.of("Ben", "Cy", "Cy", "Di", "Di"),
            execute(cb.unionAll(namesOlderThan(25), namesOlderThan(30))));
        assertEquals(List.of("Cy", "Di"),
            execute(cb.intersect(namesOlderThan(20), namesOlderThan(30))));
        assertEquals(List.of("Ana", "Ben"),
            execute(cb.except(namesOlderThan(20), namesOlderThan(30))));
    }

    @Test
    void nestedSetOperation() {
        CriteriaSelect<String> union = cb.unionAll(namesOlderThan(35), namesEqualTo("Ana"));
        assertEquals(List.of("Ana", "Cy"), execute(cb.except(union, namesOlderThan(40))));
    }

    @Test
    void multiselectResults() {
        List<Object> rows = new ArrayList<>();
        for (Object[] row : execute(cb.union(namesAndAges(30), namesAndAges(40)))) {
            assertEquals(2, row.length);
            rows.add(row[0] + " " + row[1]);
        }
        assertEquals(List.of("Cy 36", "Di 41"), sorted(rows));
    }

    @Test
    void constructedResults() {
        List<Object> results = new ArrayList<>();
        for (Object o : execute(cb.union(constructed(30), constructed(40)))) {
            assertInstanceOf(NameAndAge.class, o);
            results.add(o.toString());
        }
        assertEquals(List.of("Cy 36", "Di 41"), sorted(results));
    }

    @Test
    void constructedAmongSeveralSelections() {
        List<Object> rows = new ArrayList<>();
        for (Object[] row : execute(cb.union(constructedAndAge(30), constructedAndAge(40)))) {
            assertEquals(2, row.length);
            assertInstanceOf(NameAndAge.class, row[0]);
            rows.add(row[0] + " " + row[1]);
        }
        assertEquals(List.of("Cy 36 36", "Di 41 41"), sorted(rows));
    }

    @Test
    void tupleResults() {
        List<Object> rows = new ArrayList<>();
        for (Tuple row : execute(cb.union(tuples(30), tuples(40)))) {
            rows.add(row.get("name") + " " + row.get("age"));
        }
        assertEquals(List.of("Cy 36", "Di 41"), sorted(rows));
    }

    private CriteriaQuery<Object[]> constructedAndAge(int olderThan) {
        CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
        Root<CompUser> u = q.from(CompUser.class);
        return q.multiselect(cb.construct(NameAndAge.class, u.get(CompUser_.name), u.get(CompUser_.age)),
                u.get(CompUser_.age))
            .where(cb.gt(u.get(CompUser_.age), olderThan));
    }

    private CriteriaQuery<Tuple> tuples(int olderThan) {
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<CompUser> u = q.from(CompUser.class);
        return q.multiselect(u.get(CompUser_.name).alias("name"), u.get(CompUser_.age).alias("age"))
            .where(cb.gt(u.get(CompUser_.age), olderThan));
    }

    private CriteriaQuery<String> namesOlderThan(int age) {
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Root<CompUser> u = q.from(CompUser.class);
        return q.select(u.get(CompUser_.name)).where(cb.gt(u.get(CompUser_.age), age));
    }

    private CriteriaQuery<String> namesEqualTo(String name) {
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Root<CompUser> u = q.from(CompUser.class);
        return q.select(u.get(CompUser_.name)).where(cb.equal(u.get(CompUser_.name), name));
    }

    private CriteriaQuery<Object[]> namesAndAges(int olderThan) {
        CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
        Root<CompUser> u = q.from(CompUser.class);
        return q.multiselect(u.get(CompUser_.name), u.get(CompUser_.age))
            .where(cb.gt(u.get(CompUser_.age), olderThan));
    }

    private CriteriaQuery<NameAndAge> constructed(int olderThan) {
        CriteriaQuery<NameAndAge> q = cb.createQuery(NameAndAge.class);
        Root<CompUser> u = q.from(CompUser.class);
        return q.select(cb.construct(NameAndAge.class, u.get(CompUser_.name), u.get(CompUser_.age)))
            .where(cb.gt(u.get(CompUser_.age), olderThan));
    }

    /**
     * Executes the set operation twice, since the second execution may take a
     * cached path, and returns the results of the second one in a defined order.
     */
    private <T> List<T> execute(CriteriaSelect<T> select) {
        try (EntityManager em = emf.createEntityManager()) {
            List<T> first = em.createQuery(select).getResultList();
            List<T> second = em.createQuery(select).getResultList();
            assertEquals(first.size(), second.size(), "repeated execution");
            List<T> result = new ArrayList<>(second);
            if (!result.isEmpty() && !(result.get(0) instanceof Object[]) && !(result.get(0) instanceof Tuple))
                result.sort(Comparator.comparing(String::valueOf));
            return result;
        }
    }

    private static List<Object> sorted(List<Object> results) {
        results.sort(Comparator.comparing(String::valueOf));
        return results;
    }

    public static class NameAndAge {
        private final String name;
        private final int age;

        public NameAndAge(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return name + " " + age;
        }
    }
}

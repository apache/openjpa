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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A CASE expression whose results are string literals or parameters of
 * different lengths returns the result as it is. HSQLDB types a string
 * literal as CHAR and padded the shorter results with spaces (OPENJPA-3063).
 */
public class TestCaseStringResult extends SingleEMFTestCase {
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

    public void testGeneralCaseWithLiterals() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT CASE WHEN u.age > 35 THEN 'old' ELSE 'young' END "
                + "FROM CompUser u ORDER BY u.name", String.class).getResultList();
            assertEquals(List.of("young", "old"), result);
        }
    }

    public void testGeneralCaseWithLiteralsInWhenClauses() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT CASE WHEN u.age > 35 THEN 'old' "
                + "WHEN u.age > 25 THEN 'middle aged' ELSE 'young' END "
                + "FROM CompUser u ORDER BY u.name", String.class).getResultList();
            assertEquals(List.of("middle aged", "old"), result);
        }
    }

    public void testSimpleCaseWithLiterals() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT CASE u.age WHEN 40 THEN 'forty' ELSE 'other value' END "
                + "FROM CompUser u ORDER BY u.name", String.class).getResultList();
            assertEquals(List.of("other value", "forty"), result);
        }
    }

    public void testCaseComparedWithLiteral() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT u.name FROM CompUser u "
                + "WHERE CASE WHEN u.age > 35 THEN 'old' ELSE 'young' END = 'old'", String.class)
                .getResultList();
            assertEquals(List.of("Bob"), result);
        }
    }

    public void testConcatOfCase() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT CONCAT(CASE WHEN u.age > 35 THEN 'old' ELSE 'young' END, '!') "
                + "FROM CompUser u ORDER BY u.name", String.class).getResultList();
            assertEquals(List.of("young!", "old!"), result);
        }
    }

    public void testGeneralCaseWithParameters() {
        if (getDBDictionary() instanceof DerbyDictionary) {
            // Derby requires a result of known type, a parameter has none
            return;
        }
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT CASE WHEN u.age > 35 THEN :old ELSE :young END "
                + "FROM CompUser u ORDER BY u.name", String.class)
                .setParameter("old", "old").setParameter("young", "young").getResultList();
            assertEquals(List.of("young", "old"), result);
        }
    }

    public void testGeneralCaseWithLiteralAndParameter() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery("SELECT CASE WHEN u.age > 35 THEN 'old' ELSE :young END "
                + "FROM CompUser u ORDER BY u.name", String.class)
                .setParameter("young", "young person").getResultList();
            assertEquals(List.of("young person", "old"), result);
        }
    }

    public void testCriteriaCaseWithLiterals() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<CompUser> u = cq.from(CompUser.class);
        Expression<String> group = cb.<String> selectCase()
            .when(cb.gt(u.get(CompUser_.age), 35), "old")
            .otherwise("young");
        cq.select(group);
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(List.of("young", "old"), em.createQuery(cq).getResultList());
        }
    }

    public void testCriteriaCaseWithParameters() {
        if (getDBDictionary() instanceof DerbyDictionary) {
            // Derby requires a result of known type, a parameter has none
            return;
        }
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<CompUser> u = cq.from(CompUser.class);
        ParameterExpression<String> old = cb.parameter(String.class, "old");
        ParameterExpression<String> young = cb.parameter(String.class, "young");
        Expression<String> group = cb.<String> selectCase()
            .when(cb.gt(u.get(CompUser_.age), 35), old)
            .otherwise(young);
        cq.select(group);
        cq.orderBy(cb.asc(u.get(CompUser_.name)));

        try (EntityManager em = emf.createEntityManager()) {
            List<String> result = em.createQuery(cq).setParameter("old", "old")
                .setParameter("young", "young").getResultList();
            assertEquals(List.of("young", "old"), result);
        }
    }
}

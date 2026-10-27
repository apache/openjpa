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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMTestCase;

/**
 * Compares string parameters with each other, which databases such as Derby
 * only accept when both sides are cast to a sized VARCHAR.
 */
public class TestStringParameterComparison extends SingleEMTestCase {

    @Override
    public void setUp() {
        super.setUp(SimpleEntity.class, CLEAR_TABLES);

        EntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        em1.persist(new SimpleEntity("foo", "bar"));
        em1.getTransaction().commit();
        em1.close();
    }

    public void testEqualityOfTwoStringParameters() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SimpleEntity> q = cb.createQuery(SimpleEntity.class);
        Root<SimpleEntity> root = q.from(SimpleEntity.class);
        ParameterExpression<String> left = cb.parameter(String.class);
        ParameterExpression<String> right = cb.parameter(String.class);
        q.select(root).where(cb.equal(left, right));

        TypedQuery<SimpleEntity> query = em.createQuery(q);
        query.setParameter(left, "abc");
        query.setParameter(right, "abc");
        assertEquals(1, query.getResultList().size());

        query.setParameter(right, "xyz");
        assertEquals(0, query.getResultList().size());
    }

    public void testStringParameterInParameters() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SimpleEntity> q = cb.createQuery(SimpleEntity.class);
        Root<SimpleEntity> root = q.from(SimpleEntity.class);
        ParameterExpression<String> value = cb.parameter(String.class);
        ParameterExpression<String> first = cb.parameter(String.class);
        ParameterExpression<String> second = cb.parameter(String.class);
        q.select(root).where(value.in(first, second));

        TypedQuery<SimpleEntity> query = em.createQuery(q);
        query.setParameter(value, "abc");
        query.setParameter(first, "xyz");
        query.setParameter(second, "abc");
        assertEquals(1, query.getResultList().size());

        query.setParameter(second, "uvw");
        assertEquals(0, query.getResultList().size());
    }
}

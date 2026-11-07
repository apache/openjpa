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

import java.sql.Date;
import java.sql.Time;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * COALESCE and CASE over a java.sql.Date or java.sql.Time field return a value of the type of the field, also
 * when the driver returns another type for the column, such as the Timestamp the Oracle driver returns for the
 * DATE column of such a field.
 */
public class TestTemporalConditionalExpressions extends SingleEMFTestCase {

    private static final Date DATE = Date.valueOf("2021-01-03");
    private static final Time TIME = Time.valueOf("10:20:30");

    @Override
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            AllFieldTypes e = new AllFieldTypes();
            e.setSqlDateField(DATE);
            e.setSqlTimeField(TIME);
            em.persist(e);
            em.getTransaction().commit();
        }
    }

    public void testDate() {
        assertResult(DATE, "SELECT COALESCE(e.sqlDateField, e.sqlDateField) FROM AllFieldTypes e");
        assertResult(DATE, "SELECT CASE WHEN e.intField = 0 THEN e.sqlDateField ELSE e.sqlDateField END "
            + "FROM AllFieldTypes e");
    }

    public void testTime() {
        assertResult(TIME, "SELECT COALESCE(e.sqlTimeField, e.sqlTimeField) FROM AllFieldTypes e");
        assertResult(TIME, "SELECT CASE WHEN e.intField = 0 THEN e.sqlTimeField ELSE e.sqlTimeField END "
            + "FROM AllFieldTypes e");
    }

    private void assertResult(Object expected, String jpql) {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> result = em.createQuery(jpql).getResultList();
            assertEquals(jpql, 1, result.size());
            assertEquals(jpql, expected.getClass(), result.get(0).getClass());
            assertEquals(jpql, expected.toString(), result.get(0).toString());
        }
    }
}

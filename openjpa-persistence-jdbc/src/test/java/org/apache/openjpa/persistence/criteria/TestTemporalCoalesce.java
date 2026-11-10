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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Temporal values used as the fallback of a COALESCE or as a CASE result in the
 * selection and ordering of a criteria query must not be written unquoted into
 * the SQL (OPENJPA-2935).
 */
public class TestTemporalCoalesce extends SingleEMFTestCase {
    private CriteriaBuilder cb;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, TemporalHolder.class);
        cb = emf.getCriteriaBuilder();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // the fallback value sorts before the stored value
            TemporalHolder set = new TemporalHolder(1, "set");
            set.setDue(Timestamp.valueOf("2025-04-08 11:56:02.0"));
            set.setDueDate(Date.valueOf("2025-04-08"));
            set.setDueTime(Time.valueOf("11:56:02"));
            set.setDueLocalDateTime(LocalDateTime.of(2025, 4, 8, 11, 56, 2));
            set.setDueLocalDate(LocalDate.of(2025, 4, 8));
            set.setDueLocalTime(LocalTime.of(11, 56, 2));
            em.persist(set);
            em.persist(new TemporalHolder(2, "unset"));
            em.getTransaction().commit();
        }
    }

    public void testTimestamp() {
        verify("due", Timestamp.valueOf("2020-01-02 03:04:05.0"));
    }

    public void testSqlDate() {
        verify("dueDate", Date.valueOf("2020-01-02"));
    }

    public void testSqlTime() {
        verify("dueTime", Time.valueOf("03:04:05"));
    }

    public void testLocalDateTime() {
        verify("dueLocalDateTime", LocalDateTime.of(2020, 1, 2, 3, 4, 5));
    }

    public void testLocalDate() {
        verify("dueLocalDate", LocalDate.of(2020, 1, 2));
    }

    public void testLocalTime() {
        verify("dueLocalTime", LocalTime.of(3, 4, 5));
    }

    public void testTimestampInCase() {
        Timestamp fallback = Timestamp.valueOf("2020-01-02 03:04:05.0");
        for (boolean limit : new boolean[] { false, true }) {
            CriteriaQuery<Tuple> cq = cb.createTupleQuery();
            Root<TemporalHolder> root = cq.from(TemporalHolder.class);
            Expression<Timestamp> due = root.get("due");
            Expression<Timestamp> expr = cb.<Timestamp> selectCase()
                    .when(cb.isNull(due), fallback)
                    .otherwise(due);
            cq.multiselect(root.get("name"), expr);
            cq.orderBy(cb.asc(expr), cb.asc(root.get("id")));
            assertOrdered(cq, limit, fallback);
        }
    }

    private <T extends Comparable<? super T>> void verify(String attribute, T fallback) {
        for (boolean literal : new boolean[] { false, true }) {
            for (boolean limit : new boolean[] { false, true }) {
                CriteriaQuery<Tuple> cq = cb.createTupleQuery();
                Root<TemporalHolder> root = cq.from(TemporalHolder.class);
                Expression<T> path = root.get(attribute);
                Expression<T> expr = literal
                        ? cb.coalesce(path, cb.literal(fallback))
                        : cb.coalesce(path, fallback);
                cq.multiselect(root.get("name"), expr);
                cq.orderBy(cb.asc(expr), cb.asc(root.get("id")));
                assertOrdered(cq, limit, fallback);
            }
        }
    }

    private void assertOrdered(CriteriaQuery<Tuple> cq, boolean limit, Object fallback) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Tuple> q = em.createQuery(cq);
            if (limit) {
                q.setMaxResults(10);
            }
            List<Tuple> result = q.getResultList();
            assertEquals(2, result.size());
            assertEquals("unset", result.get(0).get(0));
            assertEquals("set", result.get(1).get(0));
            assertEquals(fallback.toString(), String.valueOf(result.get(0).get(1)));
        }
    }
}

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
package org.apache.openjpa.persistence.jpql.functions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.persistence.simple.Java8TimeTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.LocalDateField;
import jakarta.persistence.criteria.LocalDateTimeField;
import jakarta.persistence.criteria.LocalTimeField;
import jakarta.persistence.criteria.Root;

/**
 * Evaluates EXTRACT on persistent temporal columns, so that every database renders each field
 * with its own SQL rather than relying on a generic EXTRACT(field FROM x).
 */
public class TestExtractDateTimeField extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, Java8TimeTypes.class);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // 2024-01-01 is a Monday: ISO week 1, but week 0 when weeks start on Sunday
            Java8TimeTypes e1 = new Java8TimeTypes();
            e1.setId(1);
            e1.setLocalDateField(LocalDate.of(2024, 1, 1));
            e1.setLocalDateTimeField(LocalDateTime.of(2024, 5, 17, 13, 45, 30));
            e1.setLocalTimeField(LocalTime.of(13, 45, 30));
            em.persist(e1);

            // 2023-12-31 is a Sunday: ISO week 52 of 2023, but week 53 when weeks start on Sunday
            Java8TimeTypes e2 = new Java8TimeTypes();
            e2.setId(2);
            e2.setLocalDateField(LocalDate.of(2023, 12, 31));
            e2.setLocalDateTimeField(LocalDateTime.of(2023, 11, 2, 7, 5, 9));
            e2.setLocalTimeField(LocalTime.of(7, 5, 9));
            em.persist(e2);

            em.getTransaction().commit();
        }
    }

    public void testExtractYear() {
        assertEquals(List.of(2024, 2023), extract("YEAR", "localDateField"));
        assertEquals(List.of(2024, 2023), extract("YEAR", "localDateTimeField"));
    }

    public void testExtractQuarter() {
        if (getDBDictionary() instanceof OracleDictionary) {
            // Oracle does not support EXTRACT(QUARTER)
            return;
        }
        assertEquals(List.of(1, 4), extract("QUARTER", "localDateField"));
        assertEquals(List.of(2, 4), extract("QUARTER", "localDateTimeField"));
    }

    public void testExtractMonth() {
        assertEquals(List.of(1, 12), extract("MONTH", "localDateField"));
        assertEquals(List.of(5, 11), extract("MONTH", "localDateTimeField"));
    }

    public void testExtractDay() {
        assertEquals(List.of(1, 31), extract("DAY", "localDateField"));
        assertEquals(List.of(17, 2), extract("DAY", "localDateTimeField"));
    }

    public void testExtractHour() {
        assertEquals(List.of(13, 7), extract("HOUR", "localDateTimeField"));
        if (!(getDBDictionary() instanceof OracleDictionary)) {
            // Oracle has no TIME data type
            assertEquals(List.of(13, 7), extract("HOUR", "localTimeField"));
        }
    }

    public void testExtractMinute() {
        assertEquals(List.of(45, 5), extract("MINUTE", "localDateTimeField"));
        if (!(getDBDictionary() instanceof OracleDictionary)) {
            assertEquals(List.of(45, 5), extract("MINUTE", "localTimeField"));
        }
    }

    public void testExtractSecond() {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> result = em.createQuery("SELECT EXTRACT(SECOND FROM e.localDateTimeField) "
                    + "FROM Java8TimeTypes e ORDER BY e.id").getResultList();
            assertEquals(2, result.size());
            assertEquals(30d, ((Number) result.get(0)).doubleValue());
            assertEquals(9d, ((Number) result.get(1)).doubleValue());
        }
    }

    public void testExtractInWhereClause() {
        if (getDBDictionary() instanceof OracleDictionary) {
            return;
        }
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                    + "WHERE EXTRACT(QUARTER FROM e.localDateField) = 4 AND EXTRACT(DAY FROM e.localDateField) = 31",
                    Integer.class).getResultList();
            assertEquals(List.of(2), ids);
        }
    }

    public void testExtractWeek() {
        DBDictionary dict = getDBDictionary();
        if (dict instanceof OracleDictionary) {
            // Oracle does not support EXTRACT(WEEK)
            return;
        }
        if (dict instanceof DerbyDictionary) {
            try (EntityManager em = emf.createEntityManager()) {
                em.createQuery("SELECT EXTRACT(WEEK FROM e.localDateField) FROM Java8TimeTypes e")
                        .getResultList();
                fail("Expected EXTRACT(WEEK) to be rejected on Derby");
            } catch (RuntimeException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("does not support \"EXTRACT(WEEK)\""));
            }
            return;
        }
        assertEquals(List.of(1, 52), extract("WEEK", "localDateField"));
    }

    public void testCriteriaExtract() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();

            CriteriaQuery<Integer> year = cb.createQuery(Integer.class);
            Root<Java8TimeTypes> r1 = year.from(Java8TimeTypes.class);
            year.select(cb.extract(LocalDateField.YEAR, r1.get("localDateField"))).orderBy(cb.asc(r1.get("id")));
            assertEquals(List.of(2024, 2023), em.createQuery(year).getResultList());

            CriteriaQuery<Integer> day = cb.createQuery(Integer.class);
            Root<Java8TimeTypes> r2 = day.from(Java8TimeTypes.class);
            day.select(r2.get("id"))
                    .where(cb.equal(cb.extract(LocalDateTimeField.DAY, r2.get("localDateTimeField")), 17));
            assertEquals(List.of(1), em.createQuery(day).getResultList());

            if (!(getDBDictionary() instanceof OracleDictionary)) {
                CriteriaQuery<Integer> quarter = cb.createQuery(Integer.class);
                Root<Java8TimeTypes> r3 = quarter.from(Java8TimeTypes.class);
                quarter.select(cb.extract(LocalDateField.QUARTER, r3.get("localDateField")))
                        .orderBy(cb.asc(r3.get("id")));
                assertEquals(List.of(1, 4), em.createQuery(quarter).getResultList());

                CriteriaQuery<Integer> minute = cb.createQuery(Integer.class);
                Root<Java8TimeTypes> r4 = minute.from(Java8TimeTypes.class);
                minute.select(cb.extract(LocalTimeField.MINUTE, r4.get("localTimeField")))
                        .orderBy(cb.asc(r4.get("id")));
                assertEquals(List.of(45, 5), em.createQuery(minute).getResultList());
            }
        }
    }

    public void testExtractFromParameter() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                    + "WHERE EXTRACT(YEAR FROM e.localDateField) = EXTRACT(YEAR FROM :d) "
                    + "AND EXTRACT(HOUR FROM e.localDateTimeField) = EXTRACT(HOUR FROM :ts)", Integer.class)
                    .setParameter("d", LocalDate.of(2023, 6, 1))
                    .setParameter("ts", LocalDateTime.of(2020, 1, 1, 7, 0))
                    .getResultList();
            assertEquals(List.of(2), ids);

            if (!(getDBDictionary() instanceof OracleDictionary)) {
                ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                        + "WHERE EXTRACT(QUARTER FROM e.localDateField) = EXTRACT(QUARTER FROM :d) "
                        + "AND EXTRACT(MINUTE FROM e.localTimeField) = EXTRACT(MINUTE FROM :t)", Integer.class)
                        .setParameter("d", LocalDate.of(2020, 2, 1))
                        .setParameter("t", LocalTime.of(0, 45))
                        .getResultList();
                assertEquals(List.of(1), ids);
            }
        }
    }

    public void testCriteriaExtractFromLiteral() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
            Root<Java8TimeTypes> r = q.from(Java8TimeTypes.class);
            q.select(r.get("id")).where(cb.equal(cb.extract(LocalDateField.YEAR, r.get("localDateField")),
                    cb.extract(LocalDateField.YEAR, cb.literal(LocalDate.of(2024, 3, 3)))));
            assertEquals(List.of(1), em.createQuery(q).getResultList());
        }
    }

    private List<Integer> extract(String field, String attribute) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT EXTRACT(" + field + " FROM e." + attribute + ") "
                    + "FROM Java8TimeTypes e ORDER BY e.id", Integer.class).getResultList();
        }
    }
}

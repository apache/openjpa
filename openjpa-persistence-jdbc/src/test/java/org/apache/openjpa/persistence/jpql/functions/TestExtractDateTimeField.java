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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
        // a time column is a DATE column on Oracle
        assertEquals(List.of(13, 7), extract("HOUR", "localTimeField"));
    }

    public void testExtractMinute() {
        assertEquals(List.of(45, 5), extract("MINUTE", "localDateTimeField"));
        assertEquals(List.of(45, 5), extract("MINUTE", "localTimeField"));
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

    /**
     * On Oracle an OffsetDateTime is held in a TIMESTAMP WITH TIME ZONE column, whose EXTRACT(HOUR) and
     * EXTRACT(MINUTE) are in UTC, whereas the cast to TIMESTAMP keeps the time of the stored offset, as
     * OffsetDateTime.getHour() returns it. Other databases convert such a value to UTC or to the time
     * zone of the session when storing or reading it.
     */
    public void testExtractHourAndMinuteFromOffsetDateTimeOnOracle() {
        if (!(getDBDictionary() instanceof OracleDictionary)) {
            return;
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Java8TimeTypes e = new Java8TimeTypes();
            e.setId(3);
            e.setOffsetDateTimeField(OffsetDateTime.of(2024, 5, 17, 13, 45, 30, 0, ZoneOffset.ofHoursMinutes(2, 30)));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(13, em.find(Java8TimeTypes.class, 3).getOffsetDateTimeField().getHour());
            assertEquals(List.of(13), em.createQuery("SELECT EXTRACT(HOUR FROM e.offsetDateTimeField) "
                    + "FROM Java8TimeTypes e WHERE e.id = 3", Integer.class).getResultList());
            assertEquals(List.of(45), em.createQuery("SELECT EXTRACT(MINUTE FROM e.offsetDateTimeField) "
                    + "FROM Java8TimeTypes e WHERE e.id = 3", Integer.class).getResultList());
        }
    }

    public void testExtractInWhereClause() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                    + "WHERE EXTRACT(QUARTER FROM e.localDateField) = 4 AND EXTRACT(DAY FROM e.localDateField) = 31",
                    Integer.class).getResultList();
            assertEquals(List.of(2), ids);
        }
    }

    public void testExtractWeek() {
        assertEquals(List.of(1, 52), extract("WEEK", "localDateField"));
    }

    /**
     * The ISO week at year boundaries, in leap and common years, and from a column, a
     * parameter and in the WHERE and ORDER BY clauses.
     */
    public void testExtractWeekAtYearBoundaries() {
        LocalDate[] dates = {
            LocalDate.of(2020, 12, 31), // Thursday of week 53 of 2020
            LocalDate.of(2021, 1, 3), // Sunday of week 53 of 2020
            LocalDate.of(2021, 1, 4), // Monday of week 1 of 2021
            LocalDate.of(2019, 12, 30), // Monday of week 1 of 2020
            LocalDate.of(2016, 1, 1), // Friday of week 53 of 2015
            LocalDate.of(2017, 1, 1), // Sunday of week 52 of 2016, a leap year
            LocalDate.of(2008, 12, 29), // Monday of week 1 of 2009
            LocalDate.of(2000, 2, 29), // Tuesday of week 9 of 2000
            LocalDate.of(1918, 9, 28), // Saturday of week 39 of 1918
        };
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (int i = 0; i < dates.length; i++) {
                Java8TimeTypes e = new Java8TimeTypes();
                e.setId(10 + i);
                e.setLocalDateField(dates[i]);
                e.setLocalDateTimeField(dates[i].atTime(23, 59, 59));
                em.persist(e);
            }
            em.getTransaction().commit();
        }
        List<Integer> expected = List.of(53, 53, 1, 1, 53, 52, 1, 9, 39);
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(expected, em.createQuery("SELECT EXTRACT(WEEK FROM e.localDateField) "
                    + "FROM Java8TimeTypes e WHERE e.id >= 10 ORDER BY e.id", Integer.class).getResultList());
            assertEquals(expected, em.createQuery("SELECT EXTRACT(WEEK FROM e.localDateTimeField) "
                    + "FROM Java8TimeTypes e WHERE e.id >= 10 ORDER BY e.id", Integer.class).getResultList());

            assertEquals(List.of(10, 11, 14), em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                    + "WHERE EXTRACT(WEEK FROM e.localDateField) = 53 ORDER BY e.id", Integer.class).getResultList());
            assertEquals(List.of(12, 13, 16), em.createQuery("SELECT e.id FROM Java8TimeTypes e WHERE e.id >= 10 "
                    + "AND EXTRACT(WEEK FROM e.localDateField) = EXTRACT(WEEK FROM :d) ORDER BY e.id", Integer.class)
                    .setParameter("d", LocalDate.of(2024, 1, 7)).getResultList());
            assertEquals(List.of(12, 13, 16, 17, 18, 15, 10, 11, 14), em.createQuery("SELECT e.id "
                    + "FROM Java8TimeTypes e WHERE e.id >= 10 ORDER BY EXTRACT(WEEK FROM e.localDateField), e.id",
                    Integer.class).getResultList());

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
            Root<Java8TimeTypes> r = q.from(Java8TimeTypes.class);
            q.select(r.get("id")).where(cb.equal(cb.parameter(Integer.class, "week"),
                    cb.extract(LocalDateField.WEEK, r.get("localDateField"))));
            assertEquals(List.of(18), em.createQuery(q).setParameter("week", 39).getResultList());
        }
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

    public void testExtractFromParameter() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                    + "WHERE EXTRACT(YEAR FROM e.localDateField) = EXTRACT(YEAR FROM :d) "
                    + "AND EXTRACT(HOUR FROM e.localDateTimeField) = EXTRACT(HOUR FROM :ts)", Integer.class)
                    .setParameter("d", LocalDate.of(2023, 6, 1))
                    .setParameter("ts", LocalDateTime.of(2020, 1, 1, 7, 0))
                    .getResultList();
            assertEquals(List.of(2), ids);

            ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e "
                    + "WHERE EXTRACT(QUARTER FROM e.localDateField) = EXTRACT(QUARTER FROM :d) "
                    + "AND EXTRACT(MINUTE FROM e.localTimeField) = EXTRACT(MINUTE FROM :t)", Integer.class)
                    .setParameter("d", LocalDate.of(2020, 2, 1))
                    .setParameter("t", LocalTime.of(0, 45))
                    .getResultList();
            assertEquals(List.of(1), ids);
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

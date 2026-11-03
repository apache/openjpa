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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * OPENJPA-3048: a JDBC escape literal such as {d '2021-01-03'} evaluated in
 * memory is a date, time or timestamp, as it is on the database, and not the
 * text of the escape.
 */
public class TestInMemoryJDBCEscapeLiterals extends SingleEMFTestCase {

    @Override
    public void setUp() {
        // the same JPQL runs on the database and in memory
        setUp(AllFieldTypes.class, CLEAR_TABLES, "openjpa.jdbc.QuerySQLCache", "false");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(newEntity("2021-01-03", "10:00:00"));
        em.persist(newEntity("2022-06-15", "18:30:00"));
        em.getTransaction().commit();
        em.close();
    }

    private static AllFieldTypes newEntity(String date, String time) {
        AllFieldTypes e = new AllFieldTypes();
        e.setStringField(date);
        e.setSqlDateField(Date.valueOf(date));
        e.setSqlTimeField(Time.valueOf(time));
        e.setSqlTimestampField(Timestamp.valueOf(date + " " + time));
        e.setDateField(new java.util.Date(Timestamp.valueOf(date + " " + time).getTime()));
        e.setLocalDateField(LocalDate.parse(date));
        e.setLocalTimeField(LocalTime.parse(time));
        e.setLocalDateTimeField(LocalDateTime.parse(date + "T" + time));
        return e;
    }

    public void testExtractInMemory() {
        EntityManager em = emf.createEntityManager();
        List<?> all = em.createQuery("SELECT e FROM AllFieldTypes e").getResultList();

        assertEquals(2021, inMemory(em, all, "SELECT EXTRACT(YEAR FROM {d '2021-01-03'}) FROM AllFieldTypes e").get(0));
        assertEquals(1, inMemory(em, all, "SELECT EXTRACT(MONTH FROM {d '2021-01-03'}) FROM AllFieldTypes e").get(0));
        assertEquals(53, inMemory(em, all, "SELECT EXTRACT(WEEK FROM {d '2021-01-03'}) FROM AllFieldTypes e").get(0));
        assertEquals(Date.valueOf("2021-01-03"),
            inMemory(em, all, "SELECT EXTRACT(DATE FROM {d '2021-01-03'}) FROM AllFieldTypes e").get(0));

        assertEquals(10, inMemory(em, all, "SELECT EXTRACT(HOUR FROM {t '10:20:30'}) FROM AllFieldTypes e").get(0));
        assertEquals(20, inMemory(em, all, "SELECT EXTRACT(MINUTE FROM {t '10:20:30'}) FROM AllFieldTypes e").get(0));
        assertEquals(Time.valueOf("10:20:30"),
            inMemory(em, all, "SELECT EXTRACT(TIME FROM {t '10:20:30'}) FROM AllFieldTypes e").get(0));

        assertEquals(2021,
            inMemory(em, all, "SELECT EXTRACT(YEAR FROM {ts '2021-01-03 10:20:30'}) FROM AllFieldTypes e").get(0));
        assertEquals(10,
            inMemory(em, all, "SELECT EXTRACT(HOUR FROM {ts '2021-01-03 10:20:30'}) FROM AllFieldTypes e").get(0));
        assertEquals(30f,
            inMemory(em, all, "SELECT EXTRACT(SECOND FROM {ts '2021-01-03 10:20:30'}) FROM AllFieldTypes e").get(0));
        em.close();
    }

    public void testCompareInMemory() {
        EntityManager em = emf.createEntityManager();
        List<?> all = em.createQuery("SELECT e FROM AllFieldTypes e").getResultList();
        assertCompareInMemory(em, all);
        em.close();
    }

    /**
     * Candidates that are not managed hold plain dates instead of proxies.
     */
    public void testCompareUnmanagedInMemory() {
        EntityManager em = emf.createEntityManager();
        List<AllFieldTypes> all = new ArrayList<>();
        all.add(newEntity("2021-01-03", "10:00:00"));
        all.add(newEntity("2022-06-15", "18:30:00"));
        assertCompareInMemory(em, all);
        em.close();
    }

    private void assertCompareInMemory(EntityManager em, List<?> candidates) {
        String[] where = {
            "e.sqlDateField = {d '2021-01-03'}",
            "e.sqlDateField > {d '2021-01-03'}",
            "e.sqlDateField <= {d '2021-01-03'}",
            "{d '2022-06-15'} = e.sqlDateField",
            "e.sqlDateField BETWEEN {d '2021-01-01'} AND {d '2021-12-31'}",
            "e.sqlTimeField = {t '10:00:00'}",
            "e.sqlTimeField > {t '12:00:00'}",
            "e.sqlTimeField < {t '12:00:00'}",
            "{t '18:30:00'} = e.sqlTimeField",
            "e.sqlTimestampField = {ts '2021-01-03 10:00:00'}",
            "e.sqlTimestampField > {ts '2021-01-03 10:00:00'}",
            "e.sqlTimestampField < {ts '2022-01-01 00:00:00'}",
            "{ts '2021-01-03 10:00:00'} = e.sqlTimestampField",
            "e.dateField = {ts '2022-06-15 18:30:00'}",
            "e.dateField < {ts '2022-01-01 00:00:00'}",
            "{ts '2022-06-15 18:30:00'} = e.dateField",
            "{ts '2022-01-01 00:00:00'} > e.dateField",
            "e.localDateField = {d '2021-01-03'}",
            "e.localDateField > {d '2021-01-03'}",
            "{d '2021-01-03'} = e.localDateField",
            "e.localTimeField = {t '10:00:00'}",
            "e.localTimeField > {t '12:00:00'}",
            "{t '10:00:00'} = e.localTimeField",
            "e.localDateTimeField = {ts '2021-01-03 10:00:00'}",
            "e.localDateTimeField < {ts '2022-01-01 00:00:00'}",
            "{ts '2021-01-03 10:00:00'} = e.localDateTimeField",
        };
        for (String w : where) {
            String jpql = "SELECT e.stringField FROM AllFieldTypes e WHERE " + w;
            List<String> db = sorted(em.createQuery(jpql).getResultList());
            assertEquals(jpql, 1, db.size());
            assertEquals(jpql, db, sorted(inMemory(em, candidates, jpql)));
        }
    }

    /**
     * A field compared with an escape of another kind. Not every database
     * accepts these comparisons, so only the in-memory result is checked.
     */
    public void testCompareOtherKindInMemory() {
        EntityManager em = emf.createEntityManager();
        List<?> all = em.createQuery("SELECT e FROM AllFieldTypes e").getResultList();

        String[][] where = {
            { "e.localDateTimeField > {d '2021-06-01'}", "2022-06-15" },
            { "e.localDateTimeField < {d '2021-06-01'}", "2021-01-03" },
            { "e.localDateField < {ts '2022-01-01 00:00:00'}", "2021-01-03" },
            { "e.sqlTimestampField > {d '2021-06-01'}", "2022-06-15" },
            { "e.sqlDateField < {ts '2021-01-03 12:00:00'}", "2021-01-03" },
            { "e.dateField > {d '2021-06-01'}", "2022-06-15" },
        };
        for (String[] w : where) {
            String jpql = "SELECT e.stringField FROM AllFieldTypes e WHERE " + w[0];
            assertEquals(jpql, Collections.singletonList(w[1]), sorted(inMemory(em, all, jpql)));
        }
        em.close();
    }

    public void testCaseAndNullIfInMemory() {
        EntityManager em = emf.createEntityManager();
        List<?> all = em.createQuery("SELECT e FROM AllFieldTypes e").getResultList();

        String[] select = {
            "CASE e.sqlDateField WHEN {d '2021-01-03'} THEN 1 ELSE 0 END",
            "CASE e.sqlTimeField WHEN {t '10:00:00'} THEN 1 ELSE 0 END",
            "CASE e.sqlTimestampField WHEN {ts '2021-01-03 10:00:00'} THEN 1 ELSE 0 END",
            "CASE e.dateField WHEN {ts '2021-01-03 10:00:00'} THEN 1 ELSE 0 END",
            "CASE e.localDateField WHEN {d '2021-01-03'} THEN 1 ELSE 0 END",
            "CASE e.localTimeField WHEN {t '10:00:00'} THEN 1 ELSE 0 END",
            "CASE e.localDateTimeField WHEN {ts '2021-01-03 10:00:00'} THEN 1 ELSE 0 END",
            "NULLIF(e.sqlDateField, {d '2021-01-03'})",
            "NULLIF(e.sqlTimestampField, {ts '2021-01-03 10:00:00'})",
            "NULLIF(e.localDateField, {d '2021-01-03'})",
        };
        for (String s : select) {
            String jpql = "SELECT " + s + " FROM AllFieldTypes e";
            List<String> db = sorted(em.createQuery(jpql).getResultList());
            assertEquals(jpql, 2, db.size());
            assertFalse(jpql, db.get(0).equals(db.get(1)));
            assertEquals(jpql, db, sorted(inMemory(em, all, jpql)));
        }

        // the database returns these as java.sql types, so only the in-memory
        // values are checked
        String[][] nullIf = {
            { "NULLIF(e.localTimeField, {t '10:00:00'})", "18:30" },
            { "NULLIF(e.localDateTimeField, {ts '2021-01-03 10:00:00'})", "2022-06-15T18:30" },
        };
        for (String[] n : nullIf) {
            String jpql = "SELECT " + n[0] + " FROM AllFieldTypes e";
            assertEquals(jpql, Arrays.asList(n[1], "null"), sorted(inMemory(em, all, jpql)));
        }
        em.close();
    }

    private static List<?> inMemory(EntityManager em, List<?> candidates, String jpql) {
        org.apache.openjpa.persistence.QueryImpl<?> q =
            (org.apache.openjpa.persistence.QueryImpl<?>) em.createQuery(jpql);
        ((QueryImpl) q.getDelegate()).setCandidateCollection(candidates);
        return q.getResultList();
    }

    private static List<String> sorted(List<?> values) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            result.add(String.valueOf(value));
        }
        Collections.sort(result);
        return result;
    }
}

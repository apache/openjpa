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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

import org.apache.openjpa.persistence.detach.Entity20;
import org.apache.openjpa.persistence.simple.TemporalFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A Calendar, java.util.Date or java.sql.Date bound for a DATE column or with TemporalType.DATE, as a query
 * parameter or as the value of a DATE attribute, is bound as a java.sql.Date without its time of day, so that
 * dates compare equal also on databases such as Oracle whose DATE values have a time of day.
 */
class TestDateTemporalTypeParameter extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, Entity20.class, TemporalFieldTypes.class);
    }

    @Test
    void calendarAndDateParametersAreBoundWithoutTimeOfDay() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 13);
        java.sql.Date today = java.sql.Date.valueOf(LocalDate.of(now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)));
        persistEntity20(today);

        try (EntityManager em = emf.createEntityManager()) {
            Query q = em.createQuery("SELECT e FROM Entity20 e WHERE e.sqlDate = ?1")
                .setParameter(1, now, TemporalType.DATE);
            assertEquals(today, q.getParameterValue(1));
            assertIds(q.getResultList());

            q = em.createQuery("SELECT e FROM Entity20 e WHERE e.sqlDate = :d")
                .setParameter("d", now.getTime(), TemporalType.DATE);
            assertEquals(today, q.getParameterValue("d"));
            assertIds(q.getResultList());

            // the parameter is of the type of the attribute, a Calendar is bound to it with a TemporalType
            q = em.createQuery("SELECT e FROM Entity20 e WHERE e.sqlDate = :d");
            @SuppressWarnings("unchecked")
            Parameter<Calendar> d = (Parameter<Calendar>) q.getParameter("d");
            q.setParameter(d, now, TemporalType.DATE);
            assertSame(now, q.getParameterValue("d"));
            assertIds(q.getResultList());
        }
    }

    @Test
    void dateAttributesAreStoredWithoutTimeOfDay() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 13);
        java.sql.Date today = java.sql.Date.valueOf(LocalDate.of(now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)));

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            TemporalFieldTypes e = new TemporalFieldTypes();
            e.setDateDateField(now.getTime());
            e.setCalendarDateField((Calendar) now.clone());
            em.persist(e);
            em.getTransaction().commit();
            em.clear();

            assertEquals(1, em.createQuery("SELECT t FROM TemporalFieldTypes t WHERE t.dateDateField = :d")
                .setParameter("d", now, TemporalType.DATE).getResultList().size());
            assertEquals(1, em.createQuery("SELECT t FROM TemporalFieldTypes t WHERE t.calendarDateField = :d")
                .setParameter("d", now, TemporalType.DATE).getResultList().size());
            assertEquals(1, em.createQuery("SELECT t FROM TemporalFieldTypes t WHERE t.dateDateField = :d")
                .setParameter("d", today).getResultList().size());
            assertEquals(1, em.createQuery("SELECT t FROM TemporalFieldTypes t WHERE t.calendarDateField = :d")
                .setParameter("d", today).getResultList().size());
        }
    }

    @Test
    void sqlDateWithTimeOfDayIsBoundWithoutIt() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 13);
        java.sql.Date today = java.sql.Date.valueOf(LocalDate.of(now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)));
        java.sql.Date withTimeOfDay = new java.sql.Date(now.getTimeInMillis());
        persistEntity20(withTimeOfDay);

        try (EntityManager em = emf.createEntityManager()) {
            assertIds(em.createQuery("SELECT e FROM Entity20 e WHERE e.sqlDate = :d")
                .setParameter("d", today).getResultList());
            assertIds(em.createQuery("SELECT e FROM Entity20 e WHERE e.sqlDate = :d")
                .setParameter("d", withTimeOfDay).getResultList());
            assertIds(em.createQuery("SELECT e FROM Entity20 e WHERE e.sqlDate = :d")
                .setParameter("d", now, TemporalType.DATE).getResultList());
        }
    }

    private void persistEntity20(java.sql.Date date) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Entity20 e = new Entity20(1);
            e.setDate(date);
            em.persist(e);
            em.getTransaction().commit();
        }
    }

    private static void assertIds(List<?> result) {
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), ((Entity20) result.get(0)).getId());
    }
}

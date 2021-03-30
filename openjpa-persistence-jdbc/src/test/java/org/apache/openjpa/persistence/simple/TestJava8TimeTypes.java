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
package org.apache.openjpa.persistence.simple;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * Test for JPA-2.2 java.time.* functionality
 */
public class TestJava8TimeTypes extends SingleEMFTestCase {
    private static String VAL_LOCAL_DATE = "2019-01-01";
    private static String VAL_LOCAL_TIME = "04:57:15";
    private static String VAL_LOCAL_DATETIME = "2019-01-01T01:00:00";


    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, Java8TimeTypes.class);
    }

    public void testJava8Types() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Java8TimeTypes e = new Java8TimeTypes();
        e.setId(1);
        e.setOldDateField(new Date());
        e.setLocalTimeField(LocalTime.parse(VAL_LOCAL_TIME));
        e.setLocalDateField(LocalDate.parse(VAL_LOCAL_DATE));
        e.setLocalDateTimeField(LocalDateTime.parse(VAL_LOCAL_DATETIME));
        e.setOffsetTimeField(e.getLocalTimeField().atOffset(ZoneOffset.ofHours(-9)));
        e.setOffsetDateTimeField(e.getLocalDateTimeField().atOffset(ZoneOffset.ofHours(-9)));

        em.persist(e);
        em.getTransaction().commit();
        em.close();

        // now read it back.
        em = emf.createEntityManager();
        Java8TimeTypes eRead = em.find(Java8TimeTypes.class, 1);

        assertEquals(LocalTime.parse(VAL_LOCAL_TIME), eRead.getLocalTimeField());
        assertEquals(LocalDate.parse(VAL_LOCAL_DATE), eRead.getLocalDateField());
        assertEquals(LocalDateTime.parse(VAL_LOCAL_DATETIME), eRead.getLocalDateTimeField());


        // Many databases do not support WITH TIMEZONE syntax.
        // Thus we can only portably ensure tha the same instant is used at least.
        assertEquals(Instant.from(e.getOffsetDateTimeField()),
                Instant.from(eRead.getOffsetDateTimeField()));

        assertEquals(e.getOffsetTimeField().withOffsetSameInstant(eRead.getOffsetTimeField().getOffset()),
                eRead.getOffsetTimeField());

        // we've got reports from various functions not properly working with Java8 Dates.

        {
            final TypedQuery<LocalDate> qry = em.createQuery("select t.localDateField from Java8TimeTypes AS t", LocalDate.class);
            final LocalDate date = qry.getSingleResult();
            assertNotNull(date);
        }

        // max function
        {
            final TypedQuery<LocalDate> qry = em.createQuery("select max(t.localDateField) from Java8TimeTypes AS t", LocalDate.class);
            final LocalDate max = qry.getSingleResult();
            assertEquals(LocalDate.parse(VAL_LOCAL_DATE), max);
        }
        {
            final TypedQuery<LocalDateTime> qry = em.createQuery("select max(t.localDateTimeField) from Java8TimeTypes AS t", LocalDateTime.class);
            final LocalDateTime max = qry.getSingleResult();
            assertEquals(LocalDateTime.parse(VAL_LOCAL_DATETIME), max);
        }
        {
            final TypedQuery<LocalTime> qry = em.createQuery("select max(t.localTimeField) from Java8TimeTypes AS t", LocalTime.class);
            final LocalTime max = qry.getSingleResult();
            assertEquals(LocalTime.parse(VAL_LOCAL_TIME), max);
        }
        {
            final TypedQuery<OffsetTime> qry = em.createQuery("select max(t.offsetTimeField) from Java8TimeTypes AS t", OffsetTime.class);
            final OffsetTime max = qry.getSingleResult();
            assertEquals(e.getOffsetTimeField().withOffsetSameInstant(eRead.getOffsetTimeField().getOffset()),
                    max.withOffsetSameInstant(eRead.getOffsetTimeField().getOffset()));
        }
        {
            final TypedQuery<OffsetDateTime> qry = em.createQuery("select max(t.offsetDateTimeField) from Java8TimeTypes AS t", OffsetDateTime.class);
            final OffsetDateTime max = qry.getSingleResult();
            assertEquals(Instant.from(e.getOffsetDateTimeField()),
                    Instant.from(max));
        }

        // min function
        {
            final TypedQuery<LocalDate> qry = em.createQuery("select min(t.localDateField) from Java8TimeTypes AS t", LocalDate.class);
            final LocalDate min = qry.getSingleResult();
            assertEquals(LocalDate.parse(VAL_LOCAL_DATE), min);
        }
        {
            final TypedQuery<LocalDateTime> qry = em.createQuery("select min(t.localDateTimeField) from Java8TimeTypes AS t", LocalDateTime.class);
            final LocalDateTime min = qry.getSingleResult();
            assertEquals(LocalDateTime.parse(VAL_LOCAL_DATETIME), min);
        }
        {
            final TypedQuery<LocalTime> qry = em.createQuery("select min(t.localTimeField) from Java8TimeTypes AS t", LocalTime.class);
            final LocalTime min = qry.getSingleResult();
            assertEquals(LocalTime.parse(VAL_LOCAL_TIME), min);
        }
        {
            final TypedQuery<OffsetTime> qry = em.createQuery("select min(t.offsetTimeField) from Java8TimeTypes AS t", OffsetTime.class);
            final OffsetTime min = qry.getSingleResult();
            assertEquals(e.getOffsetTimeField().withOffsetSameInstant(eRead.getOffsetTimeField().getOffset()),
                    min.withOffsetSameInstant(eRead.getOffsetTimeField().getOffset()));
        }
        {
            final TypedQuery<OffsetDateTime> qry = em.createQuery("select min(t.offsetDateTimeField) from Java8TimeTypes AS t", OffsetDateTime.class);
            final OffsetDateTime min = qry.getSingleResult();
            assertEquals(Instant.from(e.getOffsetDateTimeField()),
                    Instant.from(min));
        }

    }



}

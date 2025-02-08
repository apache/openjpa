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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
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

    private Java8TimeTypes insertedEntity = new Java8TimeTypes();

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, Java8TimeTypes.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        
        insertedEntity.setId(1);
        insertedEntity.setOldDateField(new Date());
        insertedEntity.setLocalTimeField(LocalTime.parse(VAL_LOCAL_TIME));
        insertedEntity.setLocalDateField(LocalDate.parse(VAL_LOCAL_DATE));
        insertedEntity.setLocalDateTimeField(LocalDateTime.parse(VAL_LOCAL_DATETIME));
        insertedEntity.setOffsetTimeField(insertedEntity.getLocalTimeField().atOffset(ZoneOffset.ofHours(-9)));
        insertedEntity.setOffsetDateTimeField(insertedEntity.getLocalDateTimeField().atOffset(ZoneOffset.ofHours(-9)));

        em.persist(insertedEntity);
        em.getTransaction().commit();
        em.close();

    }

    public void testReadJava8Types() {

        // now read it back.
        EntityManager em = emf.createEntityManager();
        Java8TimeTypes eRead = em.find(Java8TimeTypes.class, 1);

        assertEquals(LocalTime.parse(VAL_LOCAL_TIME), eRead.getLocalTimeField());
        assertEquals(LocalDate.parse(VAL_LOCAL_DATE), eRead.getLocalDateField());
        assertEquals(LocalDateTime.parse(VAL_LOCAL_DATETIME), eRead.getLocalDateTimeField());


        // Many databases do not support WITH TIMEZONE syntax.
        // Thus we can only portably ensure tha the same instant is used at least.
        assertEquals(Instant.from(insertedEntity.getOffsetDateTimeField()),
                Instant.from(eRead.getOffsetDateTimeField()));

        assertEquals(insertedEntity.getOffsetTimeField().withOffsetSameInstant(eRead.getOffsetTimeField().getOffset()),
                eRead.getOffsetTimeField());
        em.close();
    }
    
        // we've got reports from various functions not properly working with Java8 Dates.
    public void testReadLocalDate() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalDate> qry = em.createQuery("select t.localDateField from Java8TimeTypes AS t", LocalDate.class);
        final LocalDate date = qry.getSingleResult();
        assertNotNull(date);
        em.close();
    }

    // max function
    public void testMaxLocalDate() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalDate> qry = em.createQuery("select max(t.localDateField) from Java8TimeTypes AS t", LocalDate.class);
        final LocalDate max = qry.getSingleResult();
        assertEquals(LocalDate.parse(VAL_LOCAL_DATE), max);
        em.close();
    }

    public void testMaxLocalDateTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalDateTime> qry = em.createQuery("select max(t.localDateTimeField) from Java8TimeTypes AS t", LocalDateTime.class);
        final LocalDateTime max = qry.getSingleResult();
        assertEquals(LocalDateTime.parse(VAL_LOCAL_DATETIME), max);
        em.close();
    }

    public void testMaxLocalTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalTime> qry = em.createQuery("select max(t.localTimeField) from Java8TimeTypes AS t", LocalTime.class);
        final LocalTime max = qry.getSingleResult();
        assertEquals(LocalTime.parse(VAL_LOCAL_TIME), max);
        em.close();
    }

    public void testMaxOffsetTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<OffsetTime> qry = em.createQuery("select max(t.offsetTimeField) from Java8TimeTypes AS t", OffsetTime.class);
        final OffsetTime max = qry.getSingleResult();
        assertEquals(insertedEntity.getOffsetTimeField().withOffsetSameInstant(insertedEntity.getOffsetTimeField().getOffset()),
                max.withOffsetSameInstant(insertedEntity.getOffsetTimeField().getOffset()));
        em.close();
    }

    public void testMaxOffsetDateTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<OffsetDateTime> qry = em.createQuery("select max(t.offsetDateTimeField) from Java8TimeTypes AS t", OffsetDateTime.class);
        final OffsetDateTime max = qry.getSingleResult();
        assertEquals(Instant.from(insertedEntity.getOffsetDateTimeField()),
                Instant.from(max));
        em.close();
    }

    // min function
    public void testMinLocalDate() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalDate> qry = em.createQuery("select min(t.localDateField) from Java8TimeTypes AS t", LocalDate.class);
        final LocalDate min = qry.getSingleResult();
        assertEquals(LocalDate.parse(VAL_LOCAL_DATE), min);
    }

    public void testMinLocalDateTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalDateTime> qry = em.createQuery("select min(t.localDateTimeField) from Java8TimeTypes AS t", LocalDateTime.class);
        final LocalDateTime min = qry.getSingleResult();
        assertEquals(LocalDateTime.parse(VAL_LOCAL_DATETIME), min);
        em.close();
    }

    public void testMinLocalTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<LocalTime> qry = em.createQuery("select min(t.localTimeField) from Java8TimeTypes AS t", LocalTime.class);
        final LocalTime min = qry.getSingleResult();
        assertEquals(LocalTime.parse(VAL_LOCAL_TIME), min);
        em.close();
    }

    public void testMinOffsetTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<OffsetTime> qry = em.createQuery("select min(t.offsetTimeField) from Java8TimeTypes AS t", OffsetTime.class);
        final OffsetTime min = qry.getSingleResult();
        assertEquals(insertedEntity.getOffsetTimeField().withOffsetSameInstant(insertedEntity.getOffsetTimeField().getOffset()),
                min.withOffsetSameInstant(insertedEntity.getOffsetTimeField().getOffset()));
        em.close();
    }

    public void testMinOffsetDateTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<OffsetDateTime> qry
                = em.createQuery("select min(t.offsetDateTimeField) from Java8TimeTypes AS t", OffsetDateTime.class);
        final OffsetDateTime min = qry.getSingleResult();
        assertEquals(Instant.from(insertedEntity.getOffsetDateTimeField()),
                Instant.from(min));
        em.close();
    }

    public void testCurrentDateLocalDate() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<Java8TimeTypes> qry
                = em.createQuery("select j from Java8TimeTypes AS j where j.localDateField < CURRENT_DATE", Java8TimeTypes.class);
        final List<Java8TimeTypes> times = qry.getResultList();
        assertNotNull(times);
        assertTrue(!times.isEmpty());
        em.close();
    }
    public void testCurrentDateLocalDateTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<Java8TimeTypes> qry
                = em.createQuery("select j from Java8TimeTypes AS j where j.localDateTimeField < CURRENT_TIMESTAMP", Java8TimeTypes.class);
        final List<Java8TimeTypes> times = qry.getResultList();
        assertNotNull(times);
        assertTrue(!times.isEmpty());
        em.close();
    }

    public void testGetCurrentLocalDate() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<Java8TimeTypes> qry
                = em.createQuery("select j from Java8TimeTypes AS j where j.localDateField < LOCAL DATE", Java8TimeTypes.class);
        final List<Java8TimeTypes> times = qry.getResultList();
        assertNotNull(times);
        assertTrue(!times.isEmpty());
        em.close();
    }

    public void testGetCurrentLocalDateTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<Java8TimeTypes> qry
                = em.createQuery("select j from Java8TimeTypes AS j where j.localDateTimeField < LOCAL  DATETIME", Java8TimeTypes.class);
        final List<Java8TimeTypes> times = qry.getResultList();
        assertNotNull(times);
        assertTrue(!times.isEmpty());
        em.close();
    }

    public void testGetCurrentLocalTime() {
        EntityManager em = emf.createEntityManager();
        final TypedQuery<Java8TimeTypes> qry
                = em.createQuery("select j from Java8TimeTypes AS j where j.localTimeField < LOCAL TIME", Java8TimeTypes.class);
        final List<Java8TimeTypes> times = qry.getResultList();
        assertNotNull(times);
        assertTrue(!times.isEmpty());
        em.close();
    }

}

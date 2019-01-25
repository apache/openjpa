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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

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
    }



}

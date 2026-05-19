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

import java.util.Calendar;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that java.util.Calendar can be used as a primary key type.
 * JPA allows Calendar as a basic type, and the TCK uses it as an @Id.
 */
public class TestCalendarIdEntity extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES, CalendarIdEntity.class);
    }

    public void testPersistAndFindWithCalendarId() {
        Calendar now = Calendar.getInstance();
        // truncate to seconds to avoid DB precision issues
        now.set(Calendar.MILLISECOND, 0);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        CalendarIdEntity entity = new CalendarIdEntity(now, "test");
        em.persist(entity);
        em.getTransaction().commit();

        em.clear();

        CalendarIdEntity found = em.find(CalendarIdEntity.class, now);
        assertNotNull("Entity should be found by Calendar id", found);
        assertEquals("test", found.getName());
        assertEquals(now.getTimeInMillis(), found.getId().getTimeInMillis());

        em.close();
    }

    public void testCalendarIdEntityCreatesEMF() {
        // Just verify that creating the EMF with a Calendar @Id doesn't throw
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        em.close();
    }
}

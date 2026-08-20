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
 * Tests Calendar @Id with runtime enhancement (unenhanced entity),
 * mirroring the TCK entity A2_Property which uses property access
 * with @Id @Temporal(TemporalType.DATE) on a Calendar getter.
 */
public class TestUnenhancedCalendarId extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedCalendarIdPropertyEntity.class,
            "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    public void testCreateEMFWithCalendarId() {
        // Verify that creating the EMF with a Calendar @Id entity doesn't throw
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        em.close();
    }

    public void testPersistAndFindWithCalendarId() {
        Calendar now = Calendar.getInstance();
        // Truncate to day precision to match @Temporal(DATE)
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        UnenhancedCalendarIdPropertyEntity entity =
            new UnenhancedCalendarIdPropertyEntity(now, "test");
        em.persist(entity);
        em.getTransaction().commit();

        em.clear();

        UnenhancedCalendarIdPropertyEntity found =
            em.find(UnenhancedCalendarIdPropertyEntity.class, now);
        assertNotNull("Entity should be found by Calendar id", found);
        assertEquals("test", found.getStringVersion());

        em.close();
    }
}

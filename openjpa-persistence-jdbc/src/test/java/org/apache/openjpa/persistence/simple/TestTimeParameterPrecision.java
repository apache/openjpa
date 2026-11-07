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

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A time parameter with a fraction of a second finds the entity stored with the same time, also where
 * the time column cannot hold the fraction, such as the DATE column a time is mapped to on Oracle.
 */
public class TestTimeParameterPrecision extends SingleEMFTestCase {

    private static final LocalTime TIME = LocalTime.of(10, 11, 12, 345_678_000);
    private static final OffsetTime OFFSET_TIME = OffsetTime.of(TIME, ZoneOffset.UTC);

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, Java8TimeTypes.class);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Java8TimeTypes e = new Java8TimeTypes();
            e.setId(1);
            e.setLocalTimeField(TIME);
            e.setOffsetTimeField(OFFSET_TIME);
            em.persist(e);
            em.getTransaction().commit();
        }
    }

    public void testFindByLocalTime() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e WHERE e.localTimeField = :t",
                Integer.class).setParameter("t", TIME).getResultList();
            assertEquals(List.of(1), ids);
        }
    }

    public void testFindByOffsetTime() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM Java8TimeTypes e WHERE e.offsetTimeField = :t",
                Integer.class).setParameter("t", OFFSET_TIME).getResultList();
            assertEquals(List.of(1), ids);
        }
    }
}

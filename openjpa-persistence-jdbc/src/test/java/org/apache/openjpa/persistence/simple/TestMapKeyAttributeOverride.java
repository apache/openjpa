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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that @AttributeOverrides with unqualified names work on
 * @ElementCollection Map fields with embeddable values.
 * Mirrors TCK mapkeyenumerated and mapkeytemporal Department4 tests.
 *
 * Per JPA spec section 2.6, unqualified @AttributeOverride names on
 * a Map @ElementCollection refer to the map value (the embeddable),
 * not the key.
 */
public class TestMapKeyAttributeOverride extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            MapKeyEnumEntity.class,
            MapKeyTemporalEntity.class,
            MapValueEmbeddable.class);
    }

    /**
     * Test that an entity with @ElementCollection Map<Enum, Embeddable>
     * and @AttributeOverrides using unqualified names can be persisted
     * and retrieved. This verifies the fix for the "not a persistent
     * property in the embedded type" error.
     */
    public void testMapKeyEnumeratedWithAttributeOverride() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MapKeyEnumEntity dept = new MapKeyEnumEntity(1, "Engineering");
        Map<MapKeyEnum, MapValueEmbeddable> employees = new HashMap<>();
        employees.put(MapKeyEnum.ONE,
            new MapValueEmbeddable(100, "Alice"));
        employees.put(MapKeyEnum.TWO,
            new MapValueEmbeddable(200, "Bob"));
        dept.setLastNameEmployees(employees);
        em.persist(dept);

        em.getTransaction().commit();
        em.clear();

        MapKeyEnumEntity found = em.find(MapKeyEnumEntity.class, 1);
        assertNotNull(found);
        assertNotNull(found.getLastNameEmployees());
        assertEquals(2, found.getLastNameEmployees().size());

        MapValueEmbeddable alice = found.getLastNameEmployees().get(
            MapKeyEnum.ONE);
        assertNotNull(alice);
        assertEquals(100, alice.employeeId);
        assertEquals("Alice", alice.employeeName);

        MapValueEmbeddable bob = found.getLastNameEmployees().get(
            MapKeyEnum.TWO);
        assertNotNull(bob);
        assertEquals(200, bob.employeeId);
        assertEquals("Bob", bob.employeeName);

        em.close();
    }

    /**
     * Test that an entity with @ElementCollection Map<Date, Embeddable>
     * and @AttributeOverrides using unqualified names can be persisted
     * and retrieved.
     */
    public void testMapKeyTemporalWithAttributeOverride() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MapKeyTemporalEntity dept =
            new MapKeyTemporalEntity(1, "Sales");
        Map<Date, MapValueEmbeddable> employees = new HashMap<>();
        Date date1 = new Date(1000000000000L); // 2001-09-08
        Date date2 = new Date(1100000000000L); // 2004-11-09
        employees.put(date1,
            new MapValueEmbeddable(300, "Charlie"));
        employees.put(date2,
            new MapValueEmbeddable(400, "Diana"));
        dept.setLastNameEmployees(employees);
        em.persist(dept);

        em.getTransaction().commit();
        em.clear();

        MapKeyTemporalEntity found =
            em.find(MapKeyTemporalEntity.class, 1);
        assertNotNull(found);
        assertNotNull(found.getLastNameEmployees());
        assertEquals(2, found.getLastNameEmployees().size());

        em.close();
    }
}

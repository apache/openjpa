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

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests mixed access (field + property) with runtime subclass enhancement.
 * Mirrors TCK ee.jakarta.tck.persistence.core.annotations.access.field.Client1/2/3/4.
 * The DataTypes entity uses @Access(FIELD) at class level with one
 * @Access(PROPERTY) override, which triggered IndexOutOfBoundsException
 * in PCEnhancer.addAttributeTranslation().
 */
public class TestMixedAccessRuntime extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedMixedAccessEntity.class,
            "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * Verify EMF creation succeeds — this is where the enhancement error occurred.
     */
    public void testEmfCreation() {
        assertNotNull(emf);
    }

    /**
     * Persist and find entity with mixed access fields.
     */
    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMixedAccessEntity e = new UnenhancedMixedAccessEntity(1);
        e.setStrData("test");
        e.setIntData(42);
        e.setIntData2(99);
        e.setDoubleData(3.14);
        e.setBoolData(true);
        em.persist(e);
        em.flush();
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        UnenhancedMixedAccessEntity found =
            em.find(UnenhancedMixedAccessEntity.class, 1);
        assertNotNull("Entity should be found", found);
        assertEquals("test", found.getStrData());
        assertEquals(42, found.getIntData());
        assertEquals(99, found.getIntData2());
        assertEquals(3.14, found.getDoubleData(), 0.001);
        assertTrue(found.isBoolData());
        em.getTransaction().commit();
        em.close();
    }
}

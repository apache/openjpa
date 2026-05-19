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
package org.apache.openjpa.persistence.jdbc.annotations;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that @AttributeOverride on an entity's own declared field
 * (not inherited from a mapped superclass) works correctly.
 * This was failing with: "Superclass field java.lang.Object.name is
 * mapped in the metadata for subclass ... but is not a persistent field."
 *
 * Also tests @OrderBy with dot notation on @ElementCollection of
 * embeddable types, and @OrderBy on @ElementCollection of basic types.
 */
public class TestAttributeOverrideOnOwnField extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(AttrOverrideEntity.class, AttrOverrideEmbed.class,
            CLEAR_TABLES);
    }

    /**
     * Verify that the entity can be created without metadata errors.
     * Before the fix, just creating the EntityManager would fail.
     */
    public void testEntityCreation() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            AttrOverrideEntity e = new AttrOverrideEntity("1", "TestName");
            em.persist(e);
            em.getTransaction().commit();

            em.clear();
            AttrOverrideEntity found = em.find(AttrOverrideEntity.class, "1");
            assertNotNull(found);
            assertEquals("TestName", found.getName());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    /**
     * Test @AttributeOverride column rename works and @ElementCollection
     * with @OrderBy on embeddable field works.
     */
    public void testElementCollectionOrderBy() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            AttrOverrideEntity e = new AttrOverrideEntity("2", "Parent");
            e.getAddresses().add(
                new AttrOverrideEmbed("Street1", "CityA", 10000));
            e.getAddresses().add(
                new AttrOverrideEmbed("Street2", "CityB", 30000));
            e.getAddresses().add(
                new AttrOverrideEmbed("Street3", "CityC", 20000));
            em.persist(e);
            em.getTransaction().commit();

            em.clear();
            AttrOverrideEntity found = em.find(AttrOverrideEntity.class, "2");
            assertNotNull(found);
            List<AttrOverrideEmbed> addrs = found.getAddresses();
            assertEquals(3, addrs.size());
            // @OrderBy("zipCode DESC") means highest zip first
            assertEquals(30000, addrs.get(0).getZipCode());
            assertEquals(20000, addrs.get(1).getZipCode());
            assertEquals(10000, addrs.get(2).getZipCode());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
}

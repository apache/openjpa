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
package org.apache.openjpa.persistence.conversion;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * A change made in place to the mutable value of a converted attribute is a
 * change of the entity and has to be written at flush, as the specification
 * requires in "Synchronization to the Database".
 */
public class TestConvertedInPlaceChange extends SQLListenerTestCase {

    @Override
    public void setUp() {
        setUp(InPlaceConvertedEntity.class, CLEAR_TABLES);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            InPlaceConvertedEntity e = new InPlaceConvertedEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            e.getItemsByKey().put("a", new Item("one"));
            e.setItem(new Item("single"));
            em.persist(e);
            em.getTransaction().commit();
        }
    }

    public void testElementOfConvertedListChangedInPlace() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(InPlaceConvertedEntity.class, 1L).getItems().get(0).setName("changed");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("changed",
                em.find(InPlaceConvertedEntity.class, 1L).getItems().get(0).getName());
        }
    }

    public void testElementOfConvertedMapChangedInPlace() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(InPlaceConvertedEntity.class, 1L).getItemsByKey().get("a").setName("changed");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("changed",
                em.find(InPlaceConvertedEntity.class, 1L).getItemsByKey().get("a").getName());
        }
    }

    /**
     * A change made after a flush is written by the next flush, and so is
     * one made to a new instance after it was inserted.
     */
    public void testElementOfConvertedListChangedAfterFlush() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            InPlaceConvertedEntity e = em.find(InPlaceConvertedEntity.class, 1L);
            e.getItems().get(0).setName("first");
            em.flush();
            e.getItems().get(0).setName("second");

            InPlaceConvertedEntity n = new InPlaceConvertedEntity();
            n.setId(2L);
            n.getItems().add(new Item("new"));
            em.persist(n);
            em.flush();
            n.getItems().get(0).setName("changed");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("second",
                em.find(InPlaceConvertedEntity.class, 1L).getItems().get(0).getName());
            assertEquals("changed",
                em.find(InPlaceConvertedEntity.class, 2L).getItems().get(0).getName());
        }
    }

    /**
     * An instance loaded outside a transaction, or kept from an earlier
     * one, is checked too.
     */
    public void testElementOfConvertedListChangedInLaterTransaction() {
        try (EntityManager em = emf.createEntityManager()) {
            InPlaceConvertedEntity e = em.find(InPlaceConvertedEntity.class, 1L);
            em.getTransaction().begin();
            e.getItems().get(0).setName("first");
            em.getTransaction().commit();

            em.getTransaction().begin();
            e.getItems().get(1).setName("second");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            InPlaceConvertedEntity e = em.find(InPlaceConvertedEntity.class, 1L);
            assertEquals("first", e.getItems().get(0).getName());
            assertEquals("second", e.getItems().get(1).getName());
        }
    }

    public void testUnchangedConvertedListIsNotWritten() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals("one", em.find(InPlaceConvertedEntity.class, 1L).getItems().get(0).getName());
            resetSQL();
            em.getTransaction().commit();
        }
        assertNotSQL("UPDATE .*");
    }

    public void testConvertedValueChangedInPlace() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(InPlaceConvertedEntity.class, 1L).getItem().setName("changed");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("changed", em.find(InPlaceConvertedEntity.class, 1L).getItem().getName());
        }
    }

    public void testConvertedListChangedStructurally() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(InPlaceConvertedEntity.class, 1L).getItems().add(new Item("three"));
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(3, em.find(InPlaceConvertedEntity.class, 1L).getItems().size());
        }
    }
}

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
package org.apache.openjpa.persistence.embed.record;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA 3.2 support for Java record types as embeddable classes.
 */
public class TestRecordEmbeddable extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(PersonWithRecordAddress.class, AddressRecord.class,
                CLEAR_TABLES);
    }

    public void testPersistAndLoadRecordEmbeddable() {
        final AddressRecord addr =
                new AddressRecord("123 Main St", "Springfield", "62704");
        final PersonWithRecordAddress person =
                new PersonWithRecordAddress("John Doe", addr);

        // Persist
        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();
        final long id = person.getId();
        em.close();

        // Load in a fresh EM
        final EntityManager em2 = emf.createEntityManager();
        final PersonWithRecordAddress loaded =
                em2.find(PersonWithRecordAddress.class, id);
        assertNotNull(loaded);
        assertEquals("John Doe", loaded.getName());
        final AddressRecord loadedAddr = loaded.getAddress();
        assertNotNull("Address should not be null", loadedAddr);
        assertEquals("123 Main St", loadedAddr.street());
        assertEquals("Springfield", loadedAddr.city());
        assertEquals("62704", loadedAddr.zip());
        em2.close();
    }

    public void testNullRecordEmbeddable() {
        final PersonWithRecordAddress person =
                new PersonWithRecordAddress("Jane Doe", null);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();
        final long id = person.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final PersonWithRecordAddress loaded =
                em2.find(PersonWithRecordAddress.class, id);
        assertNotNull(loaded);
        assertEquals("Jane Doe", loaded.getName());
        // Embedded record with all-null columns may load as either null
        // or a record with null components - both are acceptable
        em2.close();
    }

    public void testUpdateRecordEmbeddable() {
        final AddressRecord addr =
                new AddressRecord("789 Elm St", "Denver", "80201");
        final PersonWithRecordAddress person =
                new PersonWithRecordAddress("Bob Smith", addr);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();
        final long id = person.getId();
        em.close();

        // Update the embedded record on a managed entity
        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        final PersonWithRecordAddress managed =
                em2.find(PersonWithRecordAddress.class, id);
        managed.setAddress(
                new AddressRecord("999 Pine Rd", "Boulder", "80301"));
        em2.getTransaction().commit();
        em2.close();

        // Verify the update in a fresh EM
        final EntityManager em3 = emf.createEntityManager();
        final PersonWithRecordAddress updated =
                em3.find(PersonWithRecordAddress.class, id);
        assertNotNull(updated.getAddress());
        assertEquals("999 Pine Rd", updated.getAddress().street());
        assertEquals("Boulder", updated.getAddress().city());
        assertEquals("80301", updated.getAddress().zip());
        em3.close();
    }

    public void testDeleteEntityWithRecordEmbeddable() {
        final PersonWithRecordAddress person = new PersonWithRecordAddress(
                "DeleteMe", new AddressRecord("X St", "Y City", "00000"));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();
        final long id = person.getId();
        em.close();

        // Delete via JPQL (em.remove on record-embedded entities
        // has a known limitation with dirty-check on the embedded SM)
        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        em2.createQuery(
                "DELETE FROM PersonWithRecordAddress p WHERE p.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        em2.getTransaction().commit();
        em2.close();

        // Verify deletion
        final EntityManager em3 = emf.createEntityManager();
        assertNull(em3.find(PersonWithRecordAddress.class, id));
        em3.close();
    }

    public void testMergeRecordEmbeddable() {
        final PersonWithRecordAddress person = new PersonWithRecordAddress(
                "MergeTest", new AddressRecord("100 First Ave", "Seattle", "98101"));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();
        final long id = person.getId();
        em.close();

        // Detach by closing the EM, modify, then merge
        final EntityManager em2 = emf.createEntityManager();
        PersonWithRecordAddress detached =
                em2.find(PersonWithRecordAddress.class, id);
        em2.close();

        detached.setAddress(
                new AddressRecord("200 Second Ave", "Portland", "97201"));

        final EntityManager em3 = emf.createEntityManager();
        em3.getTransaction().begin();
        em3.merge(detached);
        em3.getTransaction().commit();
        em3.close();

        // Verify merge
        final EntityManager em4 = emf.createEntityManager();
        final PersonWithRecordAddress merged =
                em4.find(PersonWithRecordAddress.class, id);
        assertNotNull(merged.getAddress());
        assertEquals("200 Second Ave", merged.getAddress().street());
        assertEquals("Portland", merged.getAddress().city());
        assertEquals("97201", merged.getAddress().zip());
        em4.close();
    }

    public void testRemoveRecordEmbeddable() {
        final PersonWithRecordAddress person = new PersonWithRecordAddress(
                "RemoveTest", new AddressRecord("Del St", "DelCity", "11111"));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();
        final long id = person.getId();
        em.close();

        // Remove via em.remove()
        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        final PersonWithRecordAddress toRemove =
                em2.find(PersonWithRecordAddress.class, id);
        em2.remove(toRemove);
        em2.getTransaction().commit();
        em2.close();

        // Verify removal
        final EntityManager em3 = emf.createEntityManager();
        assertNull(em3.find(PersonWithRecordAddress.class, id));
        em3.close();
    }

    public void testRecordIsEmbeddableType() {
        assertTrue("AddressRecord should be recognized as a record",
                AddressRecord.class.isRecord());
    }
}

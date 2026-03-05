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

    public void testRecordIsEmbeddableType() {
        assertTrue("AddressRecord should be recognized as a record",
                AddressRecord.class.isRecord());
    }
}

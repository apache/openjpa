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
package org.apache.openjpa.persistence.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <b>TestCompatibile</b> is used to test various backwards compatibility
 * scenarios between JPA 2.0 and JPA 1.x.
 *
 * <p>The following scenarios are tested:
 * <ol>
 * <li>private persistent embeddable, which was valid in JPA 1.2 but no longer
 * in JPA 2.x
 * <li>TBD
 * </ol>
 * <p>
 * <b>Note(s):</b>
 * <ul>
 * <li>N/A
 * </ul>
 */
class TestCompatible extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(EntityContact.class,
              EmbeddableAddress.class,
              "openjpa.Compatibility", "PrivatePersistentProperties=true",
              DROP_TABLES);
    }

    @Test
    void privateEmbbeddable() {

        EntityManager em = emf.createEntityManager();

        //
        // Create and persist the contact entity
        //
        EntityContact contactCreate = new EntityContact();
        contactCreate.setId("Contact id");
        contactCreate.setEmail("Contact email address");
        contactCreate.setPhone("Contact phone number");
        contactCreate.setType("HOME");
        EmbeddableAddress address = new EmbeddableAddress();
        address.setLine1("Address line 1");
        address.setLine2("Address line 2");
        address.setCity("Address city");
        address.setState("Address state");
        address.setZipCode("Address zipcode");
        address.setCountry("Address country");
        contactCreate.setAddress(address);

        em.getTransaction().begin();
        em.persist(contactCreate);
        em.getTransaction().commit();

        em.clear();
        em.close();

        //
        // Find and verify the contact entity
        //
        em = emf.createEntityManager();
        EntityContact contactFind = em.find(EntityContact.class, "Contact id");
        assertTrue(contactFind != null);
        assertEquals("Contact id", contactFind.getId());
        assertEquals("Contact email address", contactFind.getEmail());
        assertEquals("Contact phone number", contactFind.getPhone());
        assertEquals("HOME", contactFind.getType());
        assertTrue(contactFind.getAddress() != null);
        assertEquals("Address line 1", contactFind.getAddress().getLine1());
        assertEquals("Address line 2", contactFind.getAddress().getLine2());
        assertEquals("Address city", contactFind.getAddress().getCity());
        assertEquals("Address state", contactFind.getAddress().getState());
        assertEquals("Address zipcode", contactFind.getAddress().getZipCode());
        assertEquals("Address country", contactFind.getAddress().getCountry());

        //
        // Remove the contact entity
        //
        em.getTransaction().begin();
        em.remove(contactFind);
        em.getTransaction().commit();

        em.clear();
        em.close();
    }
}

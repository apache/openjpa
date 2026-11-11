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
package org.apache.openjpa.persistence.enhance.fieldaccess;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests that entities with PROPERTY access (@Id on getter) and fields
 * with lowercase accessor names (getdescription instead of getDescription)
 * are handled correctly.
 *
 * The JPA 3.2 TCK (e.g. Order entity) treats getdescription/setdescription
 * as valid property accessors, so 'description' must be persistent.
 */
public class TestFieldAccessWithNonStandardAccessors {

    @Test
    public void testPropertyAccessWithLowercaseAccessorIsPersistent() {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + FieldAccessOrder.class.getName() + ")");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
            "test", props);

        try {
            ClassMetaData meta = JPAFacadeHelper.getMetaData(emf,
                FieldAccessOrder.class);
            assertNotNull(meta, "Metadata should exist for FieldAccessOrder");

            assertTrue(AccessCode.isProperty(meta.getAccessType()),
                "Access type should be PROPERTY");

            // All three fields should be persistent — getdescription() is
            // a valid property accessor per JPA 3.2 TCK
            assertNotNull(meta.getField("id"),
                "'id' should be persistent");
            assertNotNull(meta.getField("total"),
                "'total' should be persistent");
            assertNotNull(meta.getField("description"),
                "'description' should be persistent");
        } finally {
            emf.close();
        }
    }

    @Test
    public void testPropertyAccessEntityCanPersistAndFind() {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + FieldAccessOrder.class.getName() + ")");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
            "test", props);
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            FieldAccessOrder order = new FieldAccessOrder(99, 100,
                "test order");
            em.persist(order);

            em.getTransaction().commit();
            em.clear();

            FieldAccessOrder found = em.find(FieldAccessOrder.class, 99);
            assertNotNull(found, "Entity should be found");
            assertEquals(99, found.getId());
            assertEquals(100, found.getTotal());
            assertEquals("test order", found.getdescription());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }
}

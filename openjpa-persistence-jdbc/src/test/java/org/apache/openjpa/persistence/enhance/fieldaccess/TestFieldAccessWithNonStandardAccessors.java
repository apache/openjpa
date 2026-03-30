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

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

import org.junit.Test;

import static org.junit.Assert.*;

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
            assertNotNull("Metadata should exist for FieldAccessOrder", meta);

            assertTrue("Access type should be PROPERTY",
                AccessCode.isProperty(meta.getAccessType()));

            // All three fields should be persistent — getdescription() is
            // a valid property accessor per JPA 3.2 TCK
            assertNotNull("'id' should be persistent",
                meta.getField("id"));
            assertNotNull("'total' should be persistent",
                meta.getField("total"));
            assertNotNull("'description' should be persistent",
                meta.getField("description"));
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
            assertNotNull("Entity should be found", found);
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

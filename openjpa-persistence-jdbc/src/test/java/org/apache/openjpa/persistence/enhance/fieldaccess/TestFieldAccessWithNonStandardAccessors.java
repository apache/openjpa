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
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests that entities with PROPERTY access (@Id on getter) and fields
 * with non-standard accessor names (getdescription instead of getDescription)
 * are handled correctly.
 *
 * Per the JPA spec (Section 2.3.1), when PROPERTY access is used, only
 * properties following JavaBeans naming conventions are persistent.
 * A field with non-JavaBeans accessors should be treated as non-persistent.
 */
public class TestFieldAccessWithNonStandardAccessors {

    @Test
    public void testPropertyAccessWithNonStandardAccessorIsNotPersistent() {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + FieldAccessOrder.class.getName() + ")");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
            "test", props);

        try {
            // Check metadata: 'description' should NOT be a persistent field
            // because getdescription/setdescription don't follow JavaBeans
            ClassMetaData meta = JPAFacadeHelper.getMetaData(emf,
                FieldAccessOrder.class);
            assertNotNull("Metadata should exist for FieldAccessOrder", meta);

            // Access type should be PROPERTY since @Id is on getId()
            assertTrue("Access type should be PROPERTY",
                AccessCode.isProperty(meta.getAccessType()));

            // 'description' should not be in the persistent fields
            FieldMetaData descField = meta.getField("description");
            assertNull("'description' with non-JavaBeans accessors should " +
                "not be a persistent field in PROPERTY access mode",
                descField);

            // 'id' and 'total' should be persistent (proper JavaBeans names)
            assertNotNull("'id' should be persistent",
                meta.getField("id"));
            assertNotNull("'total' should be persistent",
                meta.getField("total"));
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

            FieldAccessOrder order = new FieldAccessOrder(1, 100,
                "test order");
            em.persist(order);

            em.getTransaction().commit();
            em.clear();

            FieldAccessOrder found = em.find(FieldAccessOrder.class, 1);
            assertNotNull("Entity should be found", found);
            assertEquals(1, found.getId());
            assertEquals(100, found.getTotal());
            // description is non-persistent, so it should be null after find
            assertNull("'description' should be null (non-persistent)",
                found.getdescription());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }
}

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

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import junit.framework.TestCase;

/**
 * Tests that unenhanced entities with PROPERTY access and inheritance
 * can be persisted. Reproduces the TCK createProductData NPE where
 * HardwareProduct (subclass) inherits @Id from Product (parent) and
 * the enhancer fails to map the inherited property to its backing field.
 */
public class TestInheritedPropertyAccessPersist extends TestCase {

    private EntityManagerFactory emf;

    @Override
    public void setUp() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.RuntimeUnenhancedClasses", "supported");
        props.put("openjpa.DynamicEnhancementAgent", "false");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types="
                + UnenhancedTCKProduct.class.getName() + ";"
                + UnenhancedTCKHardwareProduct.class.getName()
                + ")");
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    @Override
    public void tearDown() throws Exception {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Test persisting a subclass entity that inherits @Id from parent.
     * This reproduces the TCK createProductData NPE:
     * ReflectingPersistenceCapable.pcNewObjectIdInstance() fails because
     * pcAttributeIndexToFieldName() returns null for inherited PK field.
     */
    public void testPersistSubclassWithInheritedId() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            UnenhancedTCKHardwareProduct hw = new UnenhancedTCKHardwareProduct();
            hw.setId("1");
            hw.setName("Test Product");
            hw.setPrice(100.0);
            hw.setQuantity(10);
            hw.setModelNumber(5000);
            em.persist(hw);
            em.flush();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("Persisting HardwareProduct with inherited @Id failed: "
                + e.getMessage());
        } finally {
            em.close();
        }

        // Verify we can read it back
        em = emf.createEntityManager();
        try {
            UnenhancedTCKHardwareProduct found = em.find(
                UnenhancedTCKHardwareProduct.class, "1");
            assertNotNull("Should find persisted HardwareProduct", found);
            assertEquals("1", found.getId());
            assertEquals("Test Product", found.getName());
            assertEquals(5000, found.getModelNumber());
        } finally {
            em.close();
        }
    }

    /**
     * Test persisting the parent class directly works too.
     */
    public void testPersistParentClassDirectly() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            UnenhancedTCKProduct product = new UnenhancedTCKProduct(
                "100", "Base Product", 50.0, 5);
            em.persist(product);
            em.flush();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("Persisting Product failed: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}

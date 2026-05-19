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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import junit.framework.TestCase;

/**
 * Tests that non-entity classes (exceptions, ID classes, listeners) listed
 * in persistence.xml {@code <class>} elements are gracefully skipped.
 *
 * Reproduces TCK scenario where LineItemException (a plain Exception
 * subclass with no JPA annotations) is listed alongside real entities
 * and causes MetaDataException in MetamodelImpl when getCriteriaBuilder()
 * or getMetamodel() is called.
 */
public class TestNonEntityInPersistenceUnit extends TestCase {

    private EntityManagerFactory emf;

    @Override
    public void setUp() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.RuntimeUnenhancedClasses", "supported");
        props.put("openjpa.DynamicEnhancementAgent", "false");
        // Include both a real entity AND a non-entity exception class
        props.put("openjpa.MetaDataFactory",
            "jpa(Types="
                + AllFieldTypes.class.getName() + ";"
                + NotAnEntity.class.getName()
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
     * Test that getMetamodel() does not throw when a non-entity class
     * is listed in the persistence unit types.
     */
    public void testGetMetamodelWithNonEntityClass() {
        try {
            Metamodel metamodel = emf.getMetamodel();
            assertNotNull("Metamodel should not be null", metamodel);
        } catch (Exception e) {
            fail("getMetamodel() should not throw when non-entity classes "
                + "are in the persistence unit: " + e.getMessage());
        }
    }

    /**
     * Test that getCriteriaBuilder() does not throw when a non-entity
     * class is listed in the persistence unit types.
     */
    public void testGetCriteriaBuilderWithNonEntityClass() {
        try {
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            assertNotNull("CriteriaBuilder should not be null", cb);
        } catch (Exception e) {
            fail("getCriteriaBuilder() should not throw when non-entity "
                + "classes are in the persistence unit: " + e.getMessage());
        }
    }

    /**
     * Test that creating an EntityManager and persisting a real entity
     * works fine even when a non-entity class is in the persistence unit.
     */
    public void testPersistRealEntityWithNonEntityInUnit() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            AllFieldTypes aft = new AllFieldTypes();
            aft.setStringField("test");
            em.persist(aft);
            em.flush();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("Persisting a real entity should work even with "
                + "non-entity classes in the persistence unit: "
                + e.getMessage());
        } finally {
            em.close();
        }
    }
}

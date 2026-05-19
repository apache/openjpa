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
package org.apache.openjpa.persistence.access;

import java.sql.Date;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.ManagedClassSubclasser;
import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.asm.EnhancementProject;

/**
 * Tests that a MappedSuperclass with field access (determined by @Id on field)
 * can have @Column annotations on getter methods without causing an
 * access type conflict during enhancement.
 *
 * This reproduces the JPA TCK metamodelapi.metamodel test failure where
 * Employee (MappedSuperclass) has @Id on field but @Column on getters,
 * and FullTimeEmployee (Entity) extends Employee with @Access(PROPERTY).
 *
 * The key scenario is: when PCEnhancer validates properties for a
 * property-access entity that inherits field-access fields from a
 * MappedSuperclass, it must not flag those inherited field-access fields
 * as violations.
 */
public class TestFieldAccessColumnOnGetter extends SingleEMFTestCase {

    @Override
    public void setUp() throws Exception {
        setUp(FieldAccessColumnOnGetterEntity.class,
              FieldAccessColumnOnGetter.class,
              CLEAR_TABLES);
    }

    /**
     * Test that the entity can be persisted and loaded — verifying that
     * the metadata setup doesn't throw an access type conflict error.
     */
    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            FieldAccessColumnOnGetterEntity e =
                new FieldAccessColumnOnGetterEntity(
                    1, "John", "Doe",
                    Date.valueOf("2020-01-15"), "Engineering");
            em.persist(e);
            em.getTransaction().commit();

            em.clear();

            FieldAccessColumnOnGetterEntity found =
                em.find(FieldAccessColumnOnGetterEntity.class, 1);
            assertNotNull(found);
            assertEquals("John", found.getFirstName());
            assertEquals("Doe", found.getLastName());
            assertEquals("Engineering", found.getDepartment());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test that the metamodel is accessible for the entity — the TCK
     * tests query the metamodel and fail during EMF setup.
     */
    public void testMetamodelAccess() {
        Metamodel metamodel = emf.getMetamodel();
        assertNotNull(metamodel);

        EntityType<FieldAccessColumnOnGetterEntity> entityType =
            metamodel.entity(FieldAccessColumnOnGetterEntity.class);
        assertNotNull(entityType);

        // Verify attributes from the MappedSuperclass are accessible
        assertNotNull(entityType.getAttribute("firstName"));
        assertNotNull(entityType.getAttribute("lastName"));
        assertNotNull(entityType.getAttribute("hireDate"));
        assertNotNull(entityType.getAttribute("id"));
        assertNotNull(entityType.getAttribute("department"));

        // Verify the MappedSuperclass is in the managed types
        boolean foundMappedSuper = false;
        for (ManagedType<?> mt : metamodel.getManagedTypes()) {
            if (mt.getJavaType() == FieldAccessColumnOnGetter.class) {
                foundMappedSuper = true;
                break;
            }
        }
        assertTrue("MappedSuperclass should be in managed types",
                   foundMappedSuper);
    }

    /**
     * Test that PCEnhancer.validateProperties() does not flag inherited
     * field-access fields as violations when the entity uses property
     * access. This directly reproduces the TCK runtime enhancement
     * scenario without needing truly unenhanced classes.
     */
    public void testEnhancerValidatesPropertyAccessSubclass() {
        OpenJPAConfiguration conf = emf.getConfiguration();
        MetaDataRepository repos = conf.newMetaDataRepositoryInstance();
        repos.setSourceMode(MetaDataModes.MODE_META);

        // Load the metadata for the entity subclass
        ClassMetaData meta = repos.getMetaData(
            FieldAccessColumnOnGetterEntity.class, null, true);
        assertNotNull("Metadata should exist for entity", meta);

        // Verify the entity has property access (from @Access(PROPERTY))
        assertTrue("Entity should use property access",
            AccessCode.isProperty(meta.getAccessType()));

        // Verify the MappedSuperclass has field access
        ClassMetaData superMeta = repos.getMetaData(
            FieldAccessColumnOnGetter.class, null, true);
        if (superMeta != null) {
            assertTrue("MappedSuperclass should use field access",
                AccessCode.isField(superMeta.getAccessType()));
        }

        // Create a PCEnhancer and run it — this should NOT throw
        // even though the entity inherits field-access fields from
        // its MappedSuperclass. The enhancer's validateProperties()
        // must skip inherited field-access fields.
        EnhancementProject project = new EnhancementProject();
        PCEnhancer enhancer = new PCEnhancer(
            conf,
            project.loadClass(FieldAccessColumnOnGetterEntity.class),
            repos);

        enhancer.setCreateSubclass(true);
        enhancer.setAddDefaultConstructor(true);

        // This must not throw — previously it threw with:
        // "The member for for persistent property was not a method"
        // for inherited field-access fields like firstName, lastName, etc.
        try {
            enhancer.run();
        } catch (Exception e) {
            fail("PCEnhancer should not fail for property-access entity "
                + "inheriting field-access fields from MappedSuperclass: "
                + e.getMessage());
        }
    }
}

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that Persistence.generateSchema() with database.action=drop
 * and a drop-script-source actually drops the table.
 * Reproduces TCK se.schemaGeneration.annotations.table pattern.
 */
public class TestSchemaGenDrop extends SingleEMFTestCase {

    private static final String SCHEMA_DIR = System.getProperty("java.io.tmpdir")
        + File.separator + "openjpa_schemagen_test";

    @Override
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
    }

    public void testDropViaGenerateSchema() throws Exception {
        File dir = new File(SCHEMA_DIR);
        dir.mkdirs();

        String createFile = SCHEMA_DIR + File.separator + "create.sql";
        String dropFile = SCHEMA_DIR + File.separator + "drop.sql";
        new File(createFile).delete();
        new File(dropFile).delete();

        // Step 1: Generate create and drop scripts using our EMF
        Map<String, Object> props = getPropertiesMap(
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                new File(createFile).toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                new File(dropFile).toURI().toString()
        );

        Persistence.generateSchema("test", props);

        assertTrue("Create script should exist", new File(createFile).exists());
        assertTrue("Drop script should exist", new File(dropFile).exists());
        assertTrue("Create script should have content", new File(createFile).length() > 0);
        assertTrue("Drop script should have content", new File(dropFile).length() > 0);

        // Step 2: Verify data persists (table exists from setUp)
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes aft = new AllFieldTypes();
        em.persist(aft);
        em.getTransaction().commit();
        em.close();

        // Step 3: Execute drop via generateSchema
        props = getPropertiesMap(
            "jakarta.persistence.schema-generation.database.action", "drop",
            "jakarta.persistence.schema-generation.scripts.action", "none",
            "jakarta.persistence.schema-generation.drop-script-source",
                new File(dropFile).toURI().toString()
        );

        Persistence.generateSchema("test", props);

        // Step 4: Create a NEW EMF (simulates TCK creating fresh EMF after drop)
        // and try to persist — should fail because table is dropped.
        // The new EMF has buildSchema which must NOT recreate the table.
        Map<String, Object> persistProps = new HashMap<>(emf.getProperties());
        persistProps.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        jakarta.persistence.EntityManagerFactory emf2 =
            Persistence.createEntityManagerFactory("test", persistProps);
        EntityManager em2 = emf2.createEntityManager();
        try {
            em2.getTransaction().begin();
            AllFieldTypes aft2 = new AllFieldTypes();
            em2.persist(aft2);
            em2.flush();
            em2.getTransaction().commit();
            fail("Should have thrown exception — table should be dropped");
        } catch (Exception ex) {
            // Expected — table doesn't exist
            if (em2.getTransaction().isActive()) {
                em2.getTransaction().rollback();
            }
        } finally {
            em2.close();
            emf2.close();
        }
    }

    private Map<String, Object> getPropertiesMap(String... kvPairs) {
        Map<String, Object> props = new HashMap<>(emf.getProperties());
        // Simulate TCK profile: buildSchema is always present
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        for (int i = 0; i < kvPairs.length; i += 2) {
            props.put(kvPairs[i], kvPairs[i + 1]);
        }
        return props;
    }
}

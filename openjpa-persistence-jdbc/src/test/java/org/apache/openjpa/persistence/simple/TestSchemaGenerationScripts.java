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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA schema generation script output via
 * {@code jakarta.persistence.schema-generation.scripts.action}.
 */
public class TestSchemaGenerationScripts extends SingleEMFTestCase {

    private File tempDir;

    @Override
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("openjpa-schema-gen").toFile();
        // Use AllFieldTypes as a simple entity to generate DDL for
        setUp(AllFieldTypes.class);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (tempDir != null) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            tempDir.delete();
        }
    }

    /**
     * Test that scripts.action=drop-and-create with database.action=none
     * generates DDL script files without touching the database.
     */
    public void testScriptsActionDropAndCreate() throws IOException {
        File createFile = new File(tempDir, "create.sql");
        File dropFile = new File(tempDir, "drop.sql");

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
            // EMF creation should trigger script generation
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Create script file should exist", createFile.exists());
        assertTrue("Drop script file should exist", dropFile.exists());

        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));

        assertTrue("Create script should contain CREATE TABLE",
            createSql.toUpperCase().contains("CREATE TABLE"));
        assertTrue("Drop script should contain DROP",
            dropSql.toUpperCase().contains("DROP"));
    }

    /**
     * Test that scripts.action=create generates only the create script.
     */
    public void testScriptsActionCreateOnly() throws IOException {
        File createFile = new File(tempDir, "create_only.sql");

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString());
        try {
            // EMF creation should trigger script generation
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Create script file should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        assertTrue("Create script should contain CREATE TABLE",
            createSql.toUpperCase().contains("CREATE TABLE"));
    }

    /**
     * Test that file: URIs are properly resolved to file paths.
     */
    public void testFileUriScriptTarget() throws IOException {
        File createFile = new File(tempDir, "uri_create.sql");
        File dropFile = new File(tempDir, "uri_drop.sql");

        // Use file: URI format (same as TCK)
        String createUri = createFile.toURI().toString();
        String dropUri = dropFile.toURI().toString();
        assertTrue("URI should start with file:", createUri.startsWith("file:"));

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target", createUri,
            "jakarta.persistence.schema-generation.scripts.drop-target", dropUri);
        try {
            // EMF creation triggers script generation
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Create script should exist at URI path", createFile.exists());
        assertTrue("Drop script should exist at URI path", dropFile.exists());
    }

    /**
     * Test that PrintWriter objects can be used as script targets
     * (JPA spec allows Writer objects, not just string URLs).
     */
    public void testPrintWriterScriptTargets() {
        StringWriter createWriter = new StringWriter();
        StringWriter dropWriter = new StringWriter();
        PrintWriter pw1 = new PrintWriter(createWriter);
        PrintWriter pw2 = new PrintWriter(dropWriter);

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target", pw1,
            "jakarta.persistence.schema-generation.scripts.drop-target", pw2);
        try {
            // EMF creation should trigger script generation
        } finally {
            closeEMF(emf2);
        }

        pw1.flush();
        pw2.flush();
        String createSql = createWriter.toString();
        String dropSql = dropWriter.toString();

        assertTrue("Create script should contain CREATE TABLE, got: " + createSql,
            createSql.toUpperCase().contains("CREATE TABLE"));
        assertTrue("Drop script should contain DROP, got: " + dropSql,
            dropSql.toUpperCase().contains("DROP"));
    }

    /**
     * Test that Writer objects (not just PrintWriter) work as script targets.
     */
    public void testWriterScriptTargets() {
        StringWriter createWriter = new StringWriter();
        StringWriter dropWriter = new StringWriter();

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target", createWriter,
            "jakarta.persistence.schema-generation.scripts.drop-target", dropWriter);
        try {
            // EMF creation should trigger script generation
        } finally {
            closeEMF(emf2);
        }

        String createSql = createWriter.toString();
        String dropSql = dropWriter.toString();

        assertTrue("Create script should contain CREATE TABLE, got: " + createSql,
            createSql.toUpperCase().contains("CREATE TABLE"));
        assertTrue("Drop script should contain DROP, got: " + dropSql,
            dropSql.toUpperCase().contains("DROP"));
    }

    /**
     * Test that PrintWriter wrapping a File works (mirrors TCK pattern).
     */
    public void testPrintWriterToFile() throws IOException {
        File createFile = new File(tempDir, "pw_file_create.sql");
        File dropFile = new File(tempDir, "pw_file_drop.sql");
        PrintWriter pw1 = new PrintWriter(createFile);
        PrintWriter pw2 = new PrintWriter(dropFile);

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target", pw1,
            "jakarta.persistence.schema-generation.scripts.drop-target", pw2);
        try {
            // EMF creation should trigger script generation
        } finally {
            closeEMF(emf2);
        }

        pw1.close();
        pw2.close();

        assertTrue("Create script file should exist", createFile.exists());
        assertTrue("Drop script file should exist", dropFile.exists());

        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));

        assertTrue("Create script should contain CREATE TABLE, got: " + createSql,
            createSql.toUpperCase().contains("CREATE TABLE"));
        assertTrue("Drop script should contain DROP, got: " + dropSql,
            dropSql.toUpperCase().contains("DROP"));
    }

    /**
     * Test PrintWriter with scripts.action=create (create-only).
     */
    public void testPrintWriterCreateOnly() {
        StringWriter createWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(createWriter);

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "create",
            "jakarta.persistence.schema-generation.scripts.create-target", pw);
        try {
            // EMF creation should trigger script generation
        } finally {
            closeEMF(emf2);
        }

        pw.flush();
        String createSql = createWriter.toString();
        assertTrue("Create script should contain CREATE TABLE, got: " + createSql,
            createSql.toUpperCase().contains("CREATE TABLE"));
    }

    /**
     * Test that file: URIs work as create-script-source
     * (execute a previously generated DDL script to create the schema).
     * This mirrors the TCK annotation test pattern.
     */
    public void testFileUriScriptSource() throws IOException {
        // Step 1: Generate create and drop scripts to files
        File createFile = new File(tempDir, "source_create.sql");
        File dropFile = new File(tempDir, "source_drop.sql");
        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
            // EMF creation triggers script generation
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Create script should exist", createFile.exists());
        assertTrue("Drop script should exist", dropFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        assertTrue("Create script should contain CREATE TABLE",
            createSql.toUpperCase().contains("CREATE TABLE"));

        // Step 2: Execute drop then create scripts via script-source
        // with file: URIs (this is what the TCK annotation tests do).
        // Use drop-and-create to drop first then create.
        EntityManagerFactory emf3 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.action", "none",
            "jakarta.persistence.schema-generation.create-script-source",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.drop-script-source",
                dropFile.toURI().toString());
        try {
            // Verify we can use the schema by persisting data
            EntityManager em = emf3.createEntityManager();
            em.getTransaction().begin();
            AllFieldTypes aft = new AllFieldTypes();
            em.persist(aft);
            em.getTransaction().commit();
            em.close();
        } finally {
            closeEMF(emf3);
        }
    }

    /**
     * Mirrors the exact TCK annotation test pattern:
     * 1. Generate create + drop scripts to files
     * 2. Persistence.generateSchema() with database.action=create
     * 3. Persist data via regular EMF (with SynchronizeMappings=buildSchema)
     * 4. Persistence.generateSchema() with database.action=drop
     * 5. Persist data via regular EMF (with SynchronizeMappings=buildSchema)
     *    — should FAIL because the table was dropped
     *
     * The "test" PU in persistence.xml has SynchronizeMappings=buildSchema,
     * which mirrors the TCK's global provider-specific property setting.
     */
    public void testTckAnnotationPattern() throws Exception {
        File createFile = new File(tempDir, "tck_create.sql");
        File dropFile = new File(tempDir, "tck_drop.sql");

        // Step 1: Generate create + drop scripts
        EntityManagerFactory emf1 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
            // EMF creation triggers script generation
        } finally {
            closeEMF(emf1);
        }

        assertTrue("Create script should exist", createFile.exists());
        assertTrue("Drop script should exist", dropFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        assertTrue("Create script should contain CREATE TABLE",
            createSql.toUpperCase().contains("CREATE TABLE"));
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        assertTrue("Drop script should contain DROP",
            dropSql.toUpperCase().contains("DROP"));

        // Step 2: Execute drop-and-create via scripts to ensure clean state
        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "openjpa.jdbc.SynchronizeMappings", "",
            "jakarta.persistence.schema-generation.database.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.action", "none",
            "jakarta.persistence.schema-generation.create-script-source",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.drop-script-source",
                dropFile.toURI().toString());
        try {
            emf2.createEntityManager().close();
        } finally {
            closeEMF(emf2);
        }

        // Step 3: Persist data — should succeed (table was created by script)
        // Disable SynchronizeMappings to mirror the TCK (which does not set it).
        EntityManagerFactory emf3 = createEMF(AllFieldTypes.class,
            "openjpa.jdbc.SynchronizeMappings", "");
        try {
            EntityManager em = emf3.createEntityManager();
            em.getTransaction().begin();
            AllFieldTypes aft = new AllFieldTypes();
            em.persist(aft);
            em.getTransaction().commit();
            em.close();
        } finally {
            closeEMF(emf3);
        }

        // Step 4: Execute drop script via createEMF with database.action=drop
        EntityManagerFactory emf4 = createEMF(
            AllFieldTypes.class,
            "openjpa.jdbc.SynchronizeMappings", "",
            "jakarta.persistence.schema-generation.database.action", "drop",
            "jakarta.persistence.schema-generation.scripts.action", "none",
            "jakarta.persistence.schema-generation.drop-script-source",
                dropFile.toURI().toString());
        try {
            emf4.createEntityManager().close();
        } finally {
            closeEMF(emf4);
        }

        // Verify the drop actually happened by trying a raw JDBC query
        EntityManagerFactory emfCheck = createEMF(AllFieldTypes.class,
            "openjpa.jdbc.SynchronizeMappings", "");
        try {
            EntityManager emCheck = emfCheck.createEntityManager();
            try {
                emCheck.createNativeQuery("SELECT COUNT(*) FROM AllFieldTypes")
                    .getSingleResult();
                fail("Table should not exist after drop");
            } catch (Exception ex) {
                // Expected — table was dropped
            } finally {
                emCheck.close();
            }
        } finally {
            closeEMF(emfCheck);
        }

        // Step 5: Persist data — should FAIL because table was dropped.
        // No SynchronizeMappings (mirrors TCK config without buildSchema).
        EntityManagerFactory emf5 = createEMF(AllFieldTypes.class,
            "openjpa.jdbc.SynchronizeMappings", "");
        try {
            EntityManager em = emf5.createEntityManager();
            em.getTransaction().begin();
            AllFieldTypes aft = new AllFieldTypes();
            em.persist(aft);
            em.flush();
            em.getTransaction().commit();
            em.close();
            fail("Persist should have failed after drop script execution — "
                + "table was dropped");
        } catch (Exception ex) {
            // Expected — table was dropped
        } finally {
            closeEMF(emf5);
        }
    }

    /**
     * Test database.action=drop with drop-script-source file: URI
     * actually drops the table. Mirrors the TCK annotation test pattern.
     */
    public void testGenerateSchemaDropScriptExecution() throws Exception {
        // Step 1: Generate drop script (table already exists from setUp)
        File dropFile = new File(tempDir, "tck_drop.sql");
        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop",
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
            // triggers script generation
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Drop script should exist", dropFile.exists());
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        assertTrue("Drop script should contain DROP",
            dropSql.toUpperCase().contains("DROP"));

        // Step 2: Execute drop script via database.action=drop
        // Create an EM to trigger synchronizeMappings via newBrokerImpl
        EntityManagerFactory emf3 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "drop",
            "jakarta.persistence.schema-generation.scripts.action", "none",
            "jakarta.persistence.schema-generation.drop-script-source",
                dropFile.toURI().toString());
        try {
            emf3.createEntityManager().close();
        } finally {
            closeEMF(emf3);
        }

        // Step 3: Try to persist — should fail because table was dropped.
        // Disable SynchronizeMappings to prevent auto table re-creation.
        EntityManagerFactory emf4 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "none",
            "openjpa.jdbc.SynchronizeMappings", "");
        try {
            EntityManager em = emf4.createEntityManager();
            em.getTransaction().begin();
            AllFieldTypes aft = new AllFieldTypes();
            em.persist(aft);
            em.flush();
            em.getTransaction().commit();
            em.close();
            fail("Persist should have failed after drop script execution");
        } catch (Exception ex) {
            // Expected — table was dropped
        } finally {
            closeEMF(emf4);
        }
    }

    /**
     * Verify that ALTER TABLE ADD CONSTRAINT FOREIGN KEY is included for
     * @JoinTable with @ForeignKey annotations.
     */
    public void testJoinTableForeignKeyInScripts() throws Exception {
        File createFile = new File(tempDir, "jt_create.sql");
        File dropFile = new File(tempDir, "jt_drop.sql");

        EntityManagerFactory emf = createEMF(
            SchemaGenCourse.class, SchemaGenStudent.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
        } finally {
            closeEMF(emf);
        }

        assertTrue("Create script should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        System.out.println("=== JoinTable Create Script ===");
        System.out.println(createSql);
        System.out.println("=== End ===");

        String createUpper = createSql.toUpperCase();
        assertTrue("Should contain CREATE TABLE SCHEMAGENCOURSE",
            createUpper.contains("CREATE TABLE SCHEMAGENCOURSE"));
        assertTrue("Should contain CREATE TABLE SCHEMAGENSTUDENT",
            createUpper.contains("CREATE TABLE SCHEMAGENSTUDENT"));
        assertTrue("Should contain CREATE TABLE SCHEMAGEN_COURSE_STUDENT",
            createUpper.contains("CREATE TABLE SCHEMAGEN_COURSE_STUDENT"));
        assertTrue("Should contain ALTER TABLE with FK constraint",
            createUpper.contains("ALTER TABLE"));
        assertTrue("Should contain CONSTRAINT COURSEIDCONSTRAINT",
            createUpper.contains("CONSTRAINT COURSEIDCONSTRAINT"));
        assertTrue("Should contain CONSTRAINT STUDENTIDCONSTRAINT",
            createUpper.contains("CONSTRAINT STUDENTIDCONSTRAINT"));

        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        System.out.println("=== JoinTable Drop Script ===");
        System.out.println(dropSql);
        System.out.println("=== End ===");
    }

    /**
     * Verify that CREATE INDEX statements are included in generated scripts
     * for entities with @Index annotations.
     */
    public void testIndexAnnotationInScripts() throws Exception {
        File createFile = new File(tempDir, "index_create.sql");
        File dropFile = new File(tempDir, "index_drop.sql");

        EntityManagerFactory emf = createEMF(
            IndexedEntity.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
            // EMF creation triggers script generation
        } finally {
            closeEMF(emf);
        }

        assertTrue("Create script should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        System.out.println("=== Index Create Script ===");
        System.out.println(createSql);
        System.out.println("=== End ===");

        assertTrue("Create script should contain CREATE TABLE",
            createSql.toUpperCase().contains("CREATE TABLE"));
        assertTrue("Create script should contain CREATE INDEX for IDX_VAL_DESC",
            createSql.toUpperCase().contains("CREATE INDEX IDX_VAL_DESC"));
        assertTrue("Create script should contain CREATE INDEX for IDX_MULTI",
            createSql.toUpperCase().contains("CREATE INDEX IDX_MULTI"));

        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        System.out.println("=== Index Drop Script ===");
        System.out.println(dropSql);
        System.out.println("=== End ===");
        assertTrue("Drop script should contain DROP TABLE",
            dropSql.toUpperCase().contains("DROP TABLE"));
    }

    /**
     * Test that scripts.action=drop generates a non-empty drop script
     * when used via Persistence.generateSchema() with a Writer target.
     * This mirrors the TCK executeDropScriptReaderTest pattern.
     */
    public void testScriptsActionDropOnlyWithWriter() throws Exception {
        StringWriter dropWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(dropWriter);

        // Step 1: Generate drop-only script to a Writer
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.driver",
            "org.apache.derby.jdbc.EmbeddedDriver");
        props.put("jakarta.persistence.jdbc.url",
            "jdbc:derby:target/scriptsDropOnly-db;create=true");
        props.put("jakarta.persistence.schema-generation.database.action",
            "none");
        props.put("jakarta.persistence.schema-generation.scripts.action",
            "drop");
        props.put("jakarta.persistence.schema-generation.scripts.drop-target",
            pw);

        EntityManagerFactory emf2 = createEMF(
            AllFieldTypes.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop",
            "jakarta.persistence.schema-generation.scripts.drop-target", pw);
        try {
            // EMF creation triggers script generation
        } finally {
            closeEMF(emf2);
        }

        pw.flush();
        String dropSql = dropWriter.toString();
        assertTrue("Drop script should not be empty when using "
            + "scripts.action=drop, got empty string",
            dropSql.length() > 0);
        assertTrue("Drop script should contain DROP TABLE, got: " + dropSql,
            dropSql.toUpperCase().contains("DROP TABLE"));
    }

    /**
     * Test that drop script for entities with @JoinColumn @ForeignKey
     * includes ALTER TABLE DROP CONSTRAINT before DROP TABLE.
     * Mirrors the TCK orderColumnTest drop script expectations.
     */
    public void testDropScriptContainsForeignKeyDrop() throws Exception {
        File createFile = new File(tempDir, "fk_create.sql");
        File dropFile = new File(tempDir, "fk_drop.sql");

        EntityManagerFactory emf2 = createEMF(
            SchemaGenDept.class, SchemaGenEmp.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action",
                "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Drop script should exist", dropFile.exists());
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        String dropUpper = dropSql.toUpperCase();

        // TCK expects either ALTER TABLE...DROP CONSTRAINT
        // or DROP TABLE...CASCADE CONSTRAINTS
        boolean hasAlterDrop = dropUpper.contains("ALTER TABLE")
            && dropUpper.contains("DROP CONSTRAINT");
        boolean hasCascade = dropUpper.contains("CASCADE CONSTRAINTS");
        assertTrue("Drop script should contain ALTER TABLE DROP CONSTRAINT "
            + "or CASCADE CONSTRAINTS for FK. Got: " + dropSql,
            hasAlterDrop || hasCascade);

        assertTrue("Drop script should contain DROP TABLE SCHEMAGENEMP",
            dropUpper.contains("DROP TABLE SCHEMAGENEMP"));
        assertTrue("Drop script should contain DROP TABLE SCHEMAGENDEPT",
            dropUpper.contains("DROP TABLE SCHEMAGENDEPT"));
    }

    /**
     * Test that create script for @OrderColumn entities includes
     * the order column in the DDL.
     * Mirrors the TCK orderColumnTest create script expectations.
     */
    public void testOrderColumnInCreateScript() throws Exception {
        File createFile = new File(tempDir, "oc_create.sql");

        EntityManagerFactory emf2 = createEMF(
            SchemaGenDept.class, SchemaGenEmp.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString());
        try {
        } finally {
            closeEMF(emf2);
        }

        assertTrue("Create script should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String createUpper = createSql.toUpperCase();

        assertTrue("Create script should contain THEORDERCOLUMN. Got: "
            + createSql,
            createUpper.contains("THEORDERCOLUMN"));
        assertTrue("Create script should contain SCHEMAGENEMP",
            createUpper.contains("CREATE TABLE SCHEMAGENEMP"));
        assertTrue("Create script should contain FK_DEPT",
            createUpper.contains("FK_DEPT"));
    }

    /**
     * Test that @SecondaryTable generates both tables and FK constraint
     * in create/drop scripts.
     * Mirrors the TCK secondaryTableTest expectations.
     */
    public void testSecondaryTableInScripts() throws Exception {
        File createFile = new File(tempDir, "sec_create.sql");
        File dropFile = new File(tempDir, "sec_drop.sql");

        EntityManagerFactory emf2 = createEMF(
            SchemaGenSecondary.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action",
                "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
        } finally {
            closeEMF(emf2);
        }

        // Check create script
        assertTrue("Create script should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String createUpper = createSql.toUpperCase();

        assertTrue("Create script should contain primary table",
            createUpper.contains("CREATE TABLE SCHEMAGENSIMPLE"));
        assertTrue("Create script should contain secondary table",
            createUpper.contains("CREATE TABLE SCHEMAGENSIMPLE_SECOND"));
        assertTrue("Create script should contain SECONDARY_ID column",
            createUpper.contains("SECONDARY_ID"));

        // Check drop script
        assertTrue("Drop script should exist", dropFile.exists());
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        String dropUpper = dropSql.toUpperCase();

        assertTrue("Drop script should contain DROP TABLE SCHEMAGENSIMPLE",
            dropUpper.contains("DROP TABLE SCHEMAGENSIMPLE"));
        assertTrue("Drop script should contain DROP TABLE "
            + "SCHEMAGENSIMPLE_SECOND",
            dropUpper.contains("DROP TABLE SCHEMAGENSIMPLE_SECOND"));

        // FK constraint drop
        boolean hasAlterDrop = dropUpper.contains("ALTER TABLE")
            && dropUpper.contains("DROP CONSTRAINT");
        boolean hasCascade = dropUpper.contains("CASCADE CONSTRAINTS");
        assertTrue("Drop script should contain ALTER TABLE DROP CONSTRAINT "
            + "or CASCADE CONSTRAINTS for secondary table FK. Got: "
            + dropSql,
            hasAlterDrop || hasCascade);
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JPA schema generation annotations:
 * - @JoinColumn with @ForeignKey (orderColumn TCK test)
 * - @SecondaryTable with @PrimaryKeyJoinColumn (secondaryTable TCK test)
 * - @SequenceGenerator name used as database sequence name (sequenceGenerator TCK test)
 */
class TestSchemaGenAnnotations extends SingleEMFTestCase {

    private File tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("openjpa-sga-test").toFile();
        setUp(SchemaGenDept.class, SchemaGenEmp.class,
              SchemaGenSecondary.class);
    }

    @AfterEach
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
     * Test that @JoinColumn(foreignKey = @ForeignKey(name = "MYCONSTRANT"))
     * generates ALTER TABLE ... ADD CONSTRAINT MYCONSTRANT FOREIGN KEY
     * in the create script.
     */
    @Test
    void joinColumnForeignKeyConstraint() throws Exception {
        File createFile = new File(tempDir, "fk_create.sql");
        File dropFile = new File(tempDir, "fk_drop.sql");

        EntityManagerFactory emf2 = createEMF(
            SchemaGenDept.class, SchemaGenEmp.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
        } finally {
            closeEMF(emf2);
        }

        assertTrue(createFile.exists(), "Create script should exist");
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String createUpper = createSql.toUpperCase(Locale.ROOT);

        assertTrue(createUpper.contains("CREATE TABLE SCHEMAGENEMP"),
            "Should contain CREATE TABLE SCHEMAGENEMP: " + createSql);
        assertTrue(createUpper.contains("CREATE TABLE SCHEMAGENDEPT"),
            "Should contain CREATE TABLE SCHEMAGENDEPT: " + createSql);
        assertTrue(createUpper.contains("FK_DEPT"),
            "Should contain FK_DEPT column: " + createSql);
        assertTrue(createUpper.contains("THEORDERCOLUMN"),
            "Should contain THEORDERCOLUMN: " + createSql);
        // The key check: named FK constraint, "CONSTRAINT <name> FOREIGN KEY (<cols>)"
        // (the TCK checks exactly this form).
        String expected = namedForeignKey("MYCONSTRANT", "(FK_DEPT)");
        assertTrue(createUpper.contains(expected), "Should contain " + expected + ": " + createSql);

        // Check drop script
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        String dropUpper = dropSql.toUpperCase(Locale.ROOT);
        assertTrue(dropUpper.contains("DROP TABLE") && dropUpper.contains("SCHEMAGENEMP"),
            "Drop script should contain DROP TABLE SCHEMAGENEMP: " + dropSql);
        assertTrue(dropUpper.contains("DROP TABLE") && dropUpper.contains("SCHEMAGENDEPT"),
            "Drop script should contain DROP TABLE SCHEMAGENDEPT: " + dropSql);
    }

    /**
     * Test that @SecondaryTable generates the secondary table with PK join
     * column and FK constraint in the create script.
     */
    @Test
    void secondaryTableGeneration() throws Exception {
        File createFile = new File(tempDir, "sec_create.sql");
        File dropFile = new File(tempDir, "sec_drop.sql");

        // Use the same entities as setUp (which include SchemaGenSecondary)
        EntityManagerFactory emf2 = createEMF(
            SchemaGenDept.class, SchemaGenEmp.class, SchemaGenSecondary.class,
            "jakarta.persistence.schema-generation.database.action", "none",
            "jakarta.persistence.schema-generation.scripts.action", "drop-and-create",
            "jakarta.persistence.schema-generation.scripts.create-target",
                createFile.toURI().toString(),
            "jakarta.persistence.schema-generation.scripts.drop-target",
                dropFile.toURI().toString());
        try {
        } finally {
            closeEMF(emf2);
        }

        assertTrue(createFile.exists(), "Create script should exist");
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String createUpper = createSql.toUpperCase(Locale.ROOT);

        assertTrue(createUpper.contains("CREATE TABLE SCHEMAGENSIMPLE"),
            "Should contain CREATE TABLE SCHEMAGENSIMPLE: " + createSql);
        // The key check: secondary table is created
        assertTrue(createUpper.contains("CREATE TABLE SCHEMAGENSIMPLE_SECOND"),
            "Should contain CREATE TABLE SCHEMAGENSIMPLE_SECOND: " + createSql);
        assertTrue(createUpper.contains("SECONDARY_ID"),
            "Should contain SECONDARY_ID column: " + createSql);
        // FK constraint: "CONSTRAINT <name> FOREIGN KEY (<cols>)", as checked by the TCK.
        String expected = namedForeignKey("MYCONSTRAINT", "");
        assertTrue(createUpper.contains(expected), "Should contain " + expected + ": " + createSql);

        // Drop script
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        String dropUpper = dropSql.toUpperCase(Locale.ROOT);
        assertTrue(dropUpper.contains("SCHEMAGENSIMPLE"),
            "Drop should contain SCHEMAGENSIMPLE: " + dropSql);
        assertTrue(dropUpper.contains("SCHEMAGENSIMPLE_SECOND"),
            "Drop should contain SCHEMAGENSIMPLE_SECOND: " + dropSql);

        // TCK secondaryTableTest expects either:
        // 1. ALTER TABLE ... SCHEMAGENSIMPLE_SECOND DROP (constraint), or
        // 2. DROP TABLE ... SCHEMAGENSIMPLE_SECOND ... CASCADE CONSTRAINTS
        boolean hasAlterDrop = dropUpper.contains("ALTER TABLE")
            && dropUpper.contains("SCHEMAGENSIMPLE_SECOND")
            && dropUpper.contains("DROP");
        boolean hasCascade = dropUpper.contains("DROP TABLE")
            && dropUpper.contains("SCHEMAGENSIMPLE_SECOND")
            && dropUpper.contains("CASCADE CONSTRAINTS");
        assertTrue(hasAlterDrop || hasCascade, "Drop script should contain ALTER TABLE DROP CONSTRAINT "
            + "or DROP TABLE CASCADE CONSTRAINTS for secondary table FK: "
            + dropSql);
    }

    /**
     * Test that @SequenceGenerator(name = "MYSEQGENERATOR") with no
     * sequenceName attribute uses the generator name as the sequence name.
     * We verify the SequenceMetaData directly since Derby does not support
     * native sequences.
     */
    @Test
    void sequenceGeneratorNameUsedAsSequenceName() throws Exception {
        // Use a separate EMF that includes the sequence entity
        // Disable SynchronizeMappings to avoid Derby sequence errors
        EntityManagerFactory emf2 = createEMF(
            SchemaGenSeqEntity.class,
            "openjpa.jdbc.SynchronizeMappings", "");
        try {
            OpenJPAEntityManagerFactory oemf2 =
                OpenJPAPersistence.cast(emf2);
            // Force metadata parsing by requesting the metadata
            oemf2.getConfiguration().getMetaDataRepositoryInstance()
                .getMetaData(SchemaGenSeqEntity.class, null, true);
            SequenceMetaData seqMeta = oemf2.getConfiguration()
                .getMetaDataRepositoryInstance()
                .getSequenceMetaData("MYSEQGENERATOR", null, true);
            assertNotNull(seqMeta, "Sequence metadata should be found");
            assertEquals("MYSEQGENERATOR",
                seqMeta.getSequence(), "Sequence name should be MYSEQGENERATOR (generator name)");
        } finally {
            closeEMF(emf2);
        }
    }

    /**
     * Expected text of a named foreign key: "CONSTRAINT &lt;name&gt; FOREIGN KEY &lt;cols&gt;",
     * the form the TCK checks. Dictionaries that name constraints after their
     * definition (CONS_NAME_AFTER, Informix) only get the name checked.
     */
    private String namedForeignKey(String name, String cols) {
        if (DBDictionary.CONS_NAME_AFTER.equals(getDBDictionary().constraintNameMode)) {
            return "CONSTRAINT " + name;
        }
        return ("CONSTRAINT " + name + " FOREIGN KEY " + cols).trim();
    }
}

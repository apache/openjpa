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
import java.nio.file.Files;

import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPA schema generation annotations:
 * - @JoinColumn with @ForeignKey (orderColumn TCK test)
 * - @SecondaryTable with @PrimaryKeyJoinColumn (secondaryTable TCK test)
 * - @SequenceGenerator name used as database sequence name (sequenceGenerator TCK test)
 */
public class TestSchemaGenAnnotations extends SingleEMFTestCase {

    private File tempDir;

    @Override
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("openjpa-sga-test").toFile();
        setUp(SchemaGenDept.class, SchemaGenEmp.class,
              SchemaGenSecondary.class);
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
     * Test that @JoinColumn(foreignKey = @ForeignKey(name = "MYCONSTRANT"))
     * generates ALTER TABLE ... ADD CONSTRAINT MYCONSTRANT FOREIGN KEY
     * in the create script.
     */
    public void testJoinColumnForeignKeyConstraint() throws Exception {
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

        assertTrue("Create script should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String createUpper = createSql.toUpperCase();

        assertTrue("Should contain CREATE TABLE SCHEMAGENEMP: " + createSql,
            createUpper.contains("CREATE TABLE SCHEMAGENEMP"));
        assertTrue("Should contain CREATE TABLE SCHEMAGENDEPT: " + createSql,
            createUpper.contains("CREATE TABLE SCHEMAGENDEPT"));
        assertTrue("Should contain FK_DEPT column: " + createSql,
            createUpper.contains("FK_DEPT"));
        assertTrue("Should contain THEORDERCOLUMN: " + createSql,
            createUpper.contains("THEORDERCOLUMN"));
        // The key check: named FK constraint
        assertTrue("Should contain ALTER TABLE with CONSTRAINT MYCONSTRANT: " + createSql,
            createUpper.contains("CONSTRAINT MYCONSTRANT"));
        assertTrue("Should contain FOREIGN KEY (FK_DEPT): " + createSql,
            createUpper.contains("FOREIGN KEY (FK_DEPT)"));

        // Check drop script
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        String dropUpper = dropSql.toUpperCase();
        assertTrue("Drop script should contain DROP TABLE SCHEMAGENEMP: " + dropSql,
            dropUpper.contains("DROP TABLE") && dropUpper.contains("SCHEMAGENEMP"));
        assertTrue("Drop script should contain DROP TABLE SCHEMAGENDEPT: " + dropSql,
            dropUpper.contains("DROP TABLE") && dropUpper.contains("SCHEMAGENDEPT"));
    }

    /**
     * Test that @SecondaryTable generates the secondary table with PK join
     * column and FK constraint in the create script.
     */
    public void testSecondaryTableGeneration() throws Exception {
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

        assertTrue("Create script should exist", createFile.exists());
        String createSql = new String(Files.readAllBytes(createFile.toPath()));
        String createUpper = createSql.toUpperCase();

        assertTrue("Should contain CREATE TABLE SCHEMAGENSIMPLE: " + createSql,
            createUpper.contains("CREATE TABLE SCHEMAGENSIMPLE"));
        // The key check: secondary table is created
        assertTrue("Should contain CREATE TABLE SCHEMAGENSIMPLE_SECOND: " + createSql,
            createUpper.contains("CREATE TABLE SCHEMAGENSIMPLE_SECOND"));
        assertTrue("Should contain SECONDARY_ID column: " + createSql,
            createUpper.contains("SECONDARY_ID"));
        // FK constraint
        assertTrue("Should contain CONSTRAINT MYCONSTRAINT: " + createSql,
            createUpper.contains("CONSTRAINT MYCONSTRAINT"));

        // Drop script
        String dropSql = new String(Files.readAllBytes(dropFile.toPath()));
        String dropUpper = dropSql.toUpperCase();
        assertTrue("Drop should contain SCHEMAGENSIMPLE: " + dropSql,
            dropUpper.contains("SCHEMAGENSIMPLE"));
        assertTrue("Drop should contain SCHEMAGENSIMPLE_SECOND: " + dropSql,
            dropUpper.contains("SCHEMAGENSIMPLE_SECOND"));

        // TCK secondaryTableTest expects either:
        // 1. ALTER TABLE ... SCHEMAGENSIMPLE_SECOND DROP (constraint), or
        // 2. DROP TABLE ... SCHEMAGENSIMPLE_SECOND ... CASCADE CONSTRAINTS
        boolean hasAlterDrop = dropUpper.contains("ALTER TABLE")
            && dropUpper.contains("SCHEMAGENSIMPLE_SECOND")
            && dropUpper.contains("DROP");
        boolean hasCascade = dropUpper.contains("DROP TABLE")
            && dropUpper.contains("SCHEMAGENSIMPLE_SECOND")
            && dropUpper.contains("CASCADE CONSTRAINTS");
        assertTrue("Drop script should contain ALTER TABLE DROP CONSTRAINT "
            + "or DROP TABLE CASCADE CONSTRAINTS for secondary table FK: "
            + dropSql, hasAlterDrop || hasCascade);
    }

    /**
     * Test that @SequenceGenerator(name = "MYSEQGENERATOR") with no
     * sequenceName attribute uses the generator name as the sequence name.
     * We verify the SequenceMetaData directly since Derby does not support
     * native sequences.
     */
    public void testSequenceGeneratorNameUsedAsSequenceName() throws Exception {
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
            assertNotNull("Sequence metadata should be found", seqMeta);
            assertEquals("Sequence name should be MYSEQGENERATOR (generator name)",
                "MYSEQGENERATOR", seqMeta.getSequence());
        } finally {
            closeEMF(emf2);
        }
    }
}

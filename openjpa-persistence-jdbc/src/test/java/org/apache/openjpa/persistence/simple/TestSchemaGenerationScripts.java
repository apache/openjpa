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

import jakarta.persistence.EntityManagerFactory;

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
}

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
package org.apache.openjpa.persistence.jdbc.schema;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.jdbc.common.apps.InvertA;
import org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.openjpa.jdbc.identifier.DBIdentifier.newSchema;
import static org.apache.openjpa.jdbc.identifier.DBIdentifier.newTable;
import static org.apache.openjpa.jdbc.identifier.QualifiedDBIdentifier.getPath;

/**
 * @author Roberto Cortez
 */
public class TestSchemaGenerationProperties extends BaseJDBCTest {
    private static final String[] TABLES_NAME = {
            "AUTOINCPC1",
            "AUTOINCPC3",
            "CONJOINPC4",
            "CONJOINPC5",
            "CUSTMAPPC",
            "DFGTEST",
            "EAGERPC",
            "EAGERPCSUB",
            "HELPERPC",
            "HELPERPC2",
            "HELPERPC3",
            "INVERTA",
            "INVERTB",
            "EAGEROUTERJOINPC"
    };

    public TestSchemaGenerationProperties(String name) {
        super(name);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "TestConv";
    }

    @Override
    protected void addProperties(Map map) {}

    @Override
    public void setUp() throws Exception {
        super.setUp();

        OpenJPAEntityManagerFactory pmf = getEmf(new HashMap());
        pmf.createEntityManager();
        JDBCConfiguration conf = (JDBCConfiguration) ((OpenJPAEntityManagerFactorySPI) pmf).getConfiguration();

        MappingTool tool = new MappingTool(conf, "drop", false);
        SchemaTool schemaTool = new SchemaTool(conf, "drop");
        schemaTool.setSchemaGroup(tool.getSchemaGroup());
        schemaTool.run();
    }

    // TODO - Add validation when source uses script but no script is provided?.

    public void testSchemaGenMetadataDrop() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNull("Table " + tableName + " should not exist in the DB.",
                       dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenMetadataCreate() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "create");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenMetadataDropAndCreate() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop-and-create");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenScriptDrop() throws Exception {
        testSchemaGenMetadataCreate();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop");
        properties.put("javax.persistence.schema-generation.drop-source", "script");
        properties.put("javax.persistence.schema-generation.drop-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/drop.sql");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNull("Table " + tableName + " should not exist in the DB.",
                       dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenScriptCreate() throws Exception {
        testSchemaGenMetadataDrop();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "create");
        properties.put("javax.persistence.schema-generation.create-source", "script");
        properties.put("javax.persistence.schema-generation.create-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/create.sql");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenScriptDropAndCreate() throws Exception {
        testSchemaGenMetadataCreate();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop-and-create");
        properties.put("javax.persistence.schema-generation.drop-source", "script");
        properties.put("javax.persistence.schema-generation.drop-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/drop.sql");
        properties.put("javax.persistence.schema-generation.create-source", "script");
        properties.put("javax.persistence.schema-generation.create-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/create.sql");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenMetadataThenScriptDropAndCreate() throws Exception {
        try {
            OpenJPAEntityManagerFactory pmf = getEmf();
            pmf.createEntityManager();
            JDBCConfiguration conf = (JDBCConfiguration) ((OpenJPAEntityManagerFactorySPI) pmf).getConfiguration();

            SchemaTool schemaTool = new SchemaTool(conf, SchemaTool.ACTION_EXECUTE_SCRIPT);
            schemaTool.setScriptToExecute("org/apache/openjpa/persistence/jdbc/schema/create-after-metadata.sql");
            schemaTool.run();
        } catch (SQLException e) {}

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop-and-create");
        properties.put("javax.persistence.schema-generation.drop-source", "metadata-then-script");
        properties.put("javax.persistence.schema-generation.drop-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/drop-after-metadata.sql");
        properties.put("javax.persistence.schema-generation.create-source", "metadata-then-script");
        properties.put("javax.persistence.schema-generation.create-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/create-after-metadata.sql");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }

        assertNotNull("Table " + "CREATE_AFTER_METADATA" + " should have been created in the DB, but does not exists.",
                      dbSchemaGroup.findTable(getPath(newTable("CREATE_AFTER_METADATA"))));
    }

    public void testSchemaGenNoCreateSourceSpecifiedAndCreateScriptSourceSpecified() throws Exception {
        try {
            OpenJPAEntityManagerFactory pmf = getEmf();
            pmf.createEntityManager();
            JDBCConfiguration conf = (JDBCConfiguration) ((OpenJPAEntityManagerFactorySPI) pmf).getConfiguration();

            SchemaTool schemaTool = new SchemaTool(conf, SchemaTool.ACTION_EXECUTE_SCRIPT);
            schemaTool.setScriptToExecute("org/apache/openjpa/persistence/jdbc/schema/drop-after-metadata.sql");
            schemaTool.run();
        } catch (SQLException e) {}

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "create");
        properties.put("javax.persistence.schema-generation.create-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/create-after-metadata.sql");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        assertNotNull("Table " + "CREATE_AFTER_METADATA" + " should have been created in the DB, but does not exists.",
                      dbSchemaGroup.findTable(getPath(newTable("CREATE_AFTER_METADATA"))));
    }

    public void testSchemaGenNoCreateSourceAndCreateScriptSourceSpecified() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "create");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenNoDropSourceSpecifiedAndDropScriptSourceSpecified() throws Exception {
        testSchemaGenMetadataCreate();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop");;
        properties.put("javax.persistence.schema-generation.drop-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/drop.sql");

        SchemaGroup dbSchemaGroup = getSchemaGroup(properties);

        for (String tableName : TABLES_NAME) {
            assertNull("Table " + tableName + " should not exist in the DB.",
                       dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }
    }

    public void testSchemaGenScriptLoad() throws Exception {
        testSchemaGenMetadataDropAndCreate();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.sql-load-script-source",
                       "org/apache/openjpa/persistence/jdbc/schema/load.sql");

        OpenJPAEntityManagerFactory pmf = getEmf(properties);
        OpenJPAEntityManager entityManager = pmf.createEntityManager();
        JDBCConfiguration conf = (JDBCConfiguration) ((OpenJPAEntityManagerFactorySPI) pmf).getConfiguration();

        SchemaTool schemaTool = new SchemaTool(conf);
        SchemaGroup dbSchemaGroup = schemaTool.getDBSchemaGroup();

        for (String tableName : TABLES_NAME) {
            assertNotNull("Table " + tableName + " should have been created in the DB, but does not exists.",
                          dbSchemaGroup.findTable(getPath(newTable(tableName))));
        }

        InvertA invertA = entityManager.find(InvertA.class, 1);
        assertEquals(1, invertA.getId());
        assertEquals("script load test", invertA.getTest());
    }

    public void testSchemaGenOutputScriptCreate() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "create");
        properties.put("javax.persistence.schema-generation.scripts.create-target",
                       "target/create-db-output.sql");

        getEmf(properties).createEntityManager();

        File createFile = new File("target/create-db-output.sql");
        assertTrue(createFile.exists());
        assertTrue(createFile.length() > 0);
    }

    public void testSchemaGenOutputScriptDrop() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop");
        properties.put("javax.persistence.schema-generation.scripts.drop-target",
                       "target/drop-db-output.sql");

        getEmf(properties).createEntityManager();

        File dropFile = new File("target/drop-db-output.sql");
        assertTrue(dropFile.exists());
        assertTrue(dropFile.length() > 0);
    }

    public void testSchemaGenOutputScriptDropAndCreate() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.schema-generation.database.action", "drop-and-create");
        properties.put("javax.persistence.schema-generation.scripts.create-target",
                       "target/create-db-output.sql");
        properties.put("javax.persistence.schema-generation.scripts.drop-target",
                       "target/drop-db-output.sql");

        getEmf(properties).createEntityManager();

        File createFile = new File("target/create-db-output.sql");
        assertTrue(createFile.exists());
        assertTrue(createFile.length() > 0);

        File dropFile = new File("target/drop-db-output.sql");
        assertTrue(dropFile.exists());
        assertTrue(dropFile.length() > 0);
    }

    private SchemaGroup getSchemaGroup(Map<String, String> properties) {
        OpenJPAEntityManagerFactory pmf = getEmf(properties);
        pmf.createEntityManager();
        JDBCConfiguration conf = (JDBCConfiguration) ((OpenJPAEntityManagerFactorySPI) pmf).getConfiguration();

        SchemaTool schemaTool = new SchemaTool(conf);
        return schemaTool.getDBSchemaGroup();
    }
}

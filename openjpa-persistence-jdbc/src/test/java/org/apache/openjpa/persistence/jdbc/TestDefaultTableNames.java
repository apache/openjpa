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
package org.apache.openjpa.persistence.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.meta.NoneMetaDataFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests the table names the JPA mapping defaults derive from entity and field
 * names: names the database rejects are made valid, all others are used as
 * they are.
 */
class TestDefaultTableNames {

    /**
     * The described type of the mapping, whose type alias is set to the entity
     * name under test.
     */
    public static class Holder {
    }

    private PersistenceMappingDefaults defaults;
    private ClassMapping cls;
    private Schema schema;

    private void setUp(String dictionary) {
        JDBCConfigurationImpl conf = new JDBCConfigurationImpl();
        conf.setDBDictionary(dictionary);
        defaults = new PersistenceMappingDefaults();
        defaults.setConfiguration(conf);
        MappingRepository repos = new MappingRepository();
        repos.setConfiguration(conf);
        repos.setMetaDataFactory(new NoneMetaDataFactory());
        repos.setMappingDefaults(defaults);
        cls = (ClassMapping) repos.addMetaData(Holder.class);
        for (String field : List.of("items", "user")) {
            cls.addDeclaredField(field, List.class);
        }
        schema = new SchemaGroup().addSchema();
    }

    private List<String> getTableNames(String entityName) {
        return getTableNames(entityName, "items");
    }

    /**
     * Return the default name of the entity table, and of the join table and
     * the collection table of the given field, for the given entity name.
     */
    private List<String> getTableNames(String entityName, String field) {
        cls.setTypeAlias(entityName);
        String table = defaults.getTableName(cls, schema);
        cls.setTable(new Table(DBIdentifier.newTable(table), schema));
        FieldMapping fm = cls.getDeclaredFieldMapping(field);
        fm.setElementCollection(false);
        String joinTable = defaults.getTableName(fm, schema);
        fm.setElementCollection(true);
        String collectionTable = defaults.getTableName(fm, schema);
        return List.of(table, joinTable, collectionTable);
    }

    @Test
    void derbyRejectedNames() {
        setUp("derby");
        assertEquals(List.of("WINDOW0", "WINDOW0_items", "Window_items"), getTableNames("Window"));
        assertEquals(List.of("ORDER0", "ORDER0_items", "order_items"), getTableNames("order"));
        assertEquals(List.of("USER0", "USER0_items", "USER_items"), getTableNames("USER"));
    }

    @Test
    void derbyRejectedCombinedNames() {
        setUp("derby");
        assertEquals(List.of("Session", "SESSION_USER0", "SESSION_USER0"), getTableNames("Session", "user"));
        assertEquals(List.of("System", "SYSTEM_USER0", "SYSTEM_USER0"), getTableNames("System", "user"));
        assertEquals(List.of("Person", "Person_user", "Person_user"), getTableNames("Person", "user"));
    }

    @Test
    void derbyAcceptedNames() {
        setUp("derby");
        for (String name : List.of("Person", "Windows", "Action", "Count", "Session", "System")) {
            assertEquals(List.of(name, name + "_items", name + "_items"), getTableNames(name));
        }
    }

    @Test
    void otherDictionaries() {
        for (String dictionary : List.of("db2", "h2", "hsql", "mariadb", "mysql", "oracle", "postgres",
                "sqlserver")) {
            setUp(dictionary);
            for (String name : List.of("Window", "Order", "User", "Session", "Person")) {
                assertEquals(name, getTableNames(name).get(0), dictionary);
            }
            // the naming rule of MySQL and MariaDB delimits the names combined
            // from reserved words, so only names of other words are compared
            assertEquals(List.of("Person", "Person_items", "Person_items"), getTableNames("Person"), dictionary);
        }
    }
}

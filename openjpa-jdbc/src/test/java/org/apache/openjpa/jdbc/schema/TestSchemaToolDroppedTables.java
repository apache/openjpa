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
package org.apache.openjpa.jdbc.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The tables a schema generation script drops are tracked statically, because
 * the tracking has to outlive the configuration that wrote it. It must
 * therefore be partitioned, so that one persistence unit cannot consume or
 * clear the entries of another.
 */
class TestSchemaToolDroppedTables {

    @AfterEach
    void clearTracking() {
        SchemaTool.clearDroppedTables();
    }

    private JDBCConfiguration conf(String id, String url) {
        JDBCConfigurationImpl conf = new JDBCConfigurationImpl();
        conf.setId(id);
        if (url != null) {
            conf.setConnectionURL(url);
        }
        return conf;
    }

    @Test
    void keyIsTheConfigurationId() {
        assertEquals("unit-a", SchemaTool.trackingKey(conf("unit-a", "jdbc:h2:mem:x")));
    }

    @Test
    void keyFallsBackToTheConnectionWithoutAnId() {
        assertEquals("jdbc:h2:mem:x", SchemaTool.trackingKey(conf(null, "jdbc:h2:mem:x")));
    }

    @Test
    void keyIsStableWithNeitherIdNorConnection() {
        assertEquals(SchemaTool.trackingKey(conf(null, null)),
            SchemaTool.trackingKey(conf(null, null)));
    }

    @Test
    void onePersistenceUnitDoesNotSeeAnothersDrop() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        JDBCConfiguration b = conf("unit-b", "jdbc:h2:mem:b");

        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");

        assertFalse(SchemaTool.isDroppedTable(b, "FOO"),
            "unit-b must not see unit-a's drop");
        assertTrue(SchemaTool.isDroppedTable(a, "FOO"),
            "unit-a must see its own drop");
    }

    @Test
    void twoFactoriesOfOneUnitShareTheTracking() {
        // this is what lets a factory see what a closed generateSchema()
        // factory dropped; the two are separate configuration instances
        SchemaTool.trackScriptDdl(conf("unit-a", "jdbc:h2:mem:a"), "DROP TABLE FOO");

        assertTrue(SchemaTool.isDroppedTable(conf("unit-a", "jdbc:h2:mem:a"), "FOO"));
    }

    @Test
    void clearingOneUnitLeavesAnotherIntact() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        JDBCConfiguration b = conf("unit-b", "jdbc:h2:mem:b");
        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");
        SchemaTool.trackScriptDdl(b, "DROP TABLE FOO");

        SchemaTool.clearDroppedTables(b);

        assertTrue(SchemaTool.isDroppedTable(a, "FOO"),
            "clearing unit-b must not discard unit-a's tracking");
        assertFalse(SchemaTool.isDroppedTable(b, "FOO"));
    }

    @Test
    void creatingTheTableAgainForgetsIt() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");
        SchemaTool.trackScriptDdl(a, "CREATE TABLE FOO (ID INTEGER)");

        assertFalse(SchemaTool.isDroppedTable(a, "FOO"));
    }

    @Test
    void entryIsConsumedWhenNotSpecCompliant() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");

        assertTrue(SchemaTool.isDroppedTable(a, "FOO"));
        assertFalse(SchemaTool.isDroppedTable(a, "FOO"),
            "the entry suppresses one create only");
    }
}

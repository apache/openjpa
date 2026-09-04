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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.junit.After;
import org.junit.Test;

/**
 * The tables a schema generation script drops are tracked statically, because
 * the tracking has to outlive the configuration that wrote it. It must
 * therefore be partitioned, so that one persistence unit cannot consume or
 * clear the entries of another.
 */
public class TestSchemaToolDroppedTables {

    @After
    public void clearTracking() {
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
    public void testKeyIsTheConfigurationId() {
        assertEquals("unit-a", SchemaTool.trackingKey(conf("unit-a", "jdbc:h2:mem:x")));
    }

    @Test
    public void testKeyFallsBackToTheConnectionWithoutAnId() {
        assertEquals("jdbc:h2:mem:x", SchemaTool.trackingKey(conf(null, "jdbc:h2:mem:x")));
    }

    @Test
    public void testKeyIsStableWithNeitherIdNorConnection() {
        assertEquals(SchemaTool.trackingKey(conf(null, null)),
            SchemaTool.trackingKey(conf(null, null)));
    }

    @Test
    public void testOnePersistenceUnitDoesNotSeeAnothersDrop() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        JDBCConfiguration b = conf("unit-b", "jdbc:h2:mem:b");

        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");

        assertFalse("unit-b must not see unit-a's drop",
            SchemaTool.isDroppedTable(b, "FOO"));
        assertTrue("unit-a must see its own drop",
            SchemaTool.isDroppedTable(a, "FOO"));
    }

    @Test
    public void testTwoFactoriesOfOneUnitShareTheTracking() {
        // this is what lets a factory see what a closed generateSchema()
        // factory dropped; the two are separate configuration instances
        SchemaTool.trackScriptDdl(conf("unit-a", "jdbc:h2:mem:a"), "DROP TABLE FOO");

        assertTrue(SchemaTool.isDroppedTable(conf("unit-a", "jdbc:h2:mem:a"), "FOO"));
    }

    @Test
    public void testClearingOneUnitLeavesAnotherIntact() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        JDBCConfiguration b = conf("unit-b", "jdbc:h2:mem:b");
        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");
        SchemaTool.trackScriptDdl(b, "DROP TABLE FOO");

        SchemaTool.clearDroppedTables(b);

        assertTrue("clearing unit-b must not discard unit-a's tracking",
            SchemaTool.isDroppedTable(a, "FOO"));
        assertFalse(SchemaTool.isDroppedTable(b, "FOO"));
    }

    @Test
    public void testCreatingTheTableAgainForgetsIt() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");
        SchemaTool.trackScriptDdl(a, "CREATE TABLE FOO (ID INTEGER)");

        assertFalse(SchemaTool.isDroppedTable(a, "FOO"));
    }

    @Test
    public void testEntryIsConsumedWhenNotSpecCompliant() {
        JDBCConfiguration a = conf("unit-a", "jdbc:h2:mem:a");
        SchemaTool.trackScriptDdl(a, "DROP TABLE FOO");

        assertTrue(SchemaTool.isDroppedTable(a, "FOO"));
        assertFalse("the entry suppresses one create only",
            SchemaTool.isDroppedTable(a, "FOO"));
    }
}

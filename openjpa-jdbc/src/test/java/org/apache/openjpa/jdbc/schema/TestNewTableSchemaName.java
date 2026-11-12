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

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.junit.Test;

/**
 * The schema configured for new tables is named for the dictionary of the
 * configuration: on MySQL and MariaDB a name that is a reserved word is
 * delimited, so that it equals the name that is read back from the
 * database, on other databases it is left alone (OPENJPA-3076).
 */
public class TestNewTableSchemaName {

    @Test
    public void testConfiguredSchema() {
        assertEquals("\"Current\"", Schemas.getNewTableSchemaIdentifier(configuration("mysql", "Current", null))
            .getName());
        assertEquals("\"Current\"", Schemas.getNewTableSchemaIdentifier(configuration("mariadb", "Current", null))
            .getName());
        assertEquals("Current", Schemas.getNewTableSchemaIdentifier(configuration("derby", "Current", null))
            .getName());
    }

    @Test
    public void testFirstOfTheConfiguredSchemas() {
        assertEquals("\"Current\"", Schemas.getNewTableSchemaIdentifier(
            configuration("mysql", null, "Current.Window,Item")).getName());
        assertEquals("Shop", Schemas.getNewTableSchemaIdentifier(configuration("mysql", null, "Shop.Item"))
            .getName());
    }

    private static JDBCConfiguration configuration(String dictionary, String schema, String schemas) {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setDBDictionary(dictionary);
        conf.setSchema(schema);
        conf.setSchemas(schemas);
        return conf;
    }
}

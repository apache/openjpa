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
package org.apache.openjpa.jdbc.sql;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.lib.conf.Configurations;
import org.junit.jupiter.api.Test;

/**
 * Tests that dictionaries without measured invalid table words do not change
 * default table and sequence names, even if they are reserved words.
 */
class TestValidDefaultTableName {

    @Test
    void defaultTableNamesAreNotChanged() throws Exception {
        for (Class<? extends DBDictionary> cls : List.of(DBDictionary.class, DB2Dictionary.class, H2Dictionary.class,
                HSQLDictionary.class, MariaDBDictionary.class, MySQLDictionary.class, OracleDictionary.class,
                PostgresDictionary.class, SQLServerDictionary.class)) {
            DBDictionary dict = cls.getDeclaredConstructor().newInstance();
            Configurations.configureInstance(dict, new JDBCConfigurationImpl(), (String) null, "DBDictionary");
            assertTrue(dict.getInvalidTableWordSet().isEmpty(), cls.getSimpleName());
            for (String name : List.of("Window", "Order", "User", "SELECT", "Current_date", "Person")) {
                DBIdentifier sName = DBIdentifier.newTable(name);
                assertSame(sName, dict.getValidDefaultTableName(sName), cls.getSimpleName() + " " + name);
                DBIdentifier seqName = DBIdentifier.newSequence(name);
                assertSame(seqName, dict.getValidDefaultSequenceName(seqName), cls.getSimpleName() + " " + name);
            }
        }
    }
}

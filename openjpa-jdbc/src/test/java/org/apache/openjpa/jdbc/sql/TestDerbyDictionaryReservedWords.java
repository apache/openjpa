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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.conf.Configurations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that the words Derby rejects as unquoted identifiers are not used as
 * unquoted table or column names by the {@link DerbyDictionary}, and that only
 * default table and sequence names Derby rejects are changed.
 */
public class TestDerbyDictionaryReservedWords {

    /**
     * Words Derby rejects as unquoted table, sequence, index, constraint and
     * schema names that the dictionary did not know as reserved words.
     */
    private static final List<String> TABLE_WORDS = List.of(
        "BIGINT", "CURRENT_ROLE", "CURSORS", "DEFINER", "DETERMINISTIC",
        "GETCURRENTCONNECTION", "INVOKER", "WINDOW", "XMLQUERY");

    private DerbyDictionary dict;
    private Schema schema;

    @Before
    public void setUp() {
        dict = new DerbyDictionary();
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), (String) null, "DBDictionary");
        schema = new SchemaGroup().addSchema();
    }

    @Test
    public void testTableNamesAreNotReservedWords() {
        for (String word : TABLE_WORDS) {
            assertEquals(word + "0", dict.getValidTableName(word, schema));
            assertEquals(word + "0", dict.getValidTableName(word.toLowerCase(Locale.ENGLISH), schema));
        }
        assertEquals("WINDOWS", dict.getValidTableName("WINDOWS", schema));
    }

    @Test
    public void testColumnNamesAreNotReservedWords() {
        Table table = schema.addTable(DBIdentifier.newTable("T"));
        for (String word : TABLE_WORDS) {
            assertTrue(word, dict.getInvalidColumnWordSet().contains(word));
            assertEquals(word + "0", dict.getValidColumnName(word, table));
        }
        assertEquals("CURSOR0", dict.getValidColumnName("CURSOR", table));
        assertEquals("WINDOWS", dict.getValidColumnName("WINDOWS", table));
    }

    @Test
    public void testInvalidTableWordsAreReservedWords() {
        assertTrue(dict.getInvalidTableWordSet().containsAll(TABLE_WORDS));
        for (String word : dict.getInvalidTableWordSet()) {
            if (word.indexOf('-') < 0) {
                assertEquals(word + "0", dict.getValidDefaultTableName(DBIdentifier.newTable(word)).getName());
            }
        }
    }

    @Test
    public void testInvalidColumnWordsAreInvalidTableWordsAndCompatWords() {
        Set<String> expected = new HashSet<>(dict.getInvalidTableWordSet());
        expected.addAll(List.of("COALESCE", "OVER", "ROW_NUMBER"));
        assertEquals(expected, dict.getInvalidColumnWordSet());
    }

    @Test
    public void testRejectedDefaultTableNames() {
        for (String name : List.of("Window", "window", "WINDOW", "Order", "User", "Current_date", "Session_user")) {
            assertEquals(name.toUpperCase(Locale.ENGLISH) + "0",
                dict.getValidDefaultTableName(DBIdentifier.newTable(name)).getName());
        }
    }

    @Test
    public void testAcceptedDefaultTableNamesAreNotChanged() {
        // reserved words Derby accepts as table names are kept, unlike in
        // getValidTableName
        for (String name : List.of("Action", "Count", "Session", "Windows", "Window_panes", "Person")) {
            DBIdentifier sName = DBIdentifier.newTable(name);
            assertSame(name, sName, dict.getValidDefaultTableName(sName));
        }
        assertEquals("COUNT0", dict.getValidTableName("Count", schema));
    }

    @Test
    public void testDefaultTableNameIsNotMadeUnique() {
        // an existing table must not lead to another name for the same entity
        schema.addTable(DBIdentifier.newTable("WINDOW0"));
        assertEquals("WINDOW0", dict.getValidDefaultTableName(DBIdentifier.newTable("Window")).getName());
        assertEquals("WINDOW01", dict.getValidTableName("Window", schema));
    }

    @Test
    public void testRejectedDefaultSequenceNames() {
        for (String name : List.of("Window", "window", "Order", "User", "Cursors")) {
            assertEquals(name.toUpperCase(Locale.ENGLISH) + "0",
                dict.getValidDefaultSequenceName(DBIdentifier.newSequence(name)).getName());
        }
    }

    @Test
    public void testAcceptedDefaultSequenceNamesAreNotChanged() {
        for (String name : List.of("Action", "Count", "Windows", "Person_seq", "OPENJPA_SEQUENCE")) {
            DBIdentifier sName = DBIdentifier.newSequence(name);
            assertSame(name, sName, dict.getValidDefaultSequenceName(sName));
        }
    }

    @Test
    public void testDelimitedDefaultSequenceNamesAreNotChanged() {
        DBIdentifier delimited = DBIdentifier.newSequence("Window", true);
        assertSame(delimited, dict.getValidDefaultSequenceName(delimited));
    }

    @Test
    public void testDelimitedDefaultTableNamesAreNotChanged() {
        DBIdentifier delimited = DBIdentifier.newTable("Window", true);
        assertSame(delimited, dict.getValidDefaultTableName(delimited));

        DerbyDictionary delimitingDict = new DerbyDictionary();
        Configurations.configureInstance(delimitingDict, new JDBCConfigurationImpl(), "DelimitIdentifiers=true",
            "DBDictionary");
        DBIdentifier sName = DBIdentifier.newTable("Window");
        assertSame(sName, delimitingDict.getValidDefaultTableName(sName));
    }
}

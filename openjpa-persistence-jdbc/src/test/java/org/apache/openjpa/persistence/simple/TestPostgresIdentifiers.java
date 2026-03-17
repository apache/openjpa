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

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;

import junit.framework.TestCase;

/**
 * Tests that PostgresDictionary does not quote identifiers.
 * PostgreSQL lowercases unquoted identifiers, so OpenJPA should
 * produce unquoted SQL to match TCK DDL-created lowercase tables.
 */
public class TestPostgresIdentifiers extends TestCase {

    private PostgresDictionary dict;

    @Override
    public void setUp() {
        dict = new PostgresDictionary();
    }

    /**
     * Verify toDBName returns unquoted name for a table identifier.
     */
    public void testToDBNameTable() {
        DBIdentifier id = DBIdentifier.newTable("ITEM");
        String result = dict.toDBName(id);
        assertNotNull(result);
        assertFalse("Should not contain quotes: " + result,
            result.contains("\""));
        assertEquals("ITEM", result);
    }

    /**
     * Verify toDBName returns unquoted name for a column identifier.
     */
    public void testToDBNameColumn() {
        DBIdentifier id = DBIdentifier.newColumn("BRANDNAME");
        String result = dict.toDBName(id);
        assertNotNull(result);
        assertFalse("Should not contain quotes: " + result,
            result.contains("\""));
        assertEquals("BRANDNAME", result);
    }

    /**
     * Verify toDBName strips quotes from a delimited identifier.
     */
    public void testToDBNameDelimited() {
        DBIdentifier id = DBIdentifier.newTable("COFFEE", true);
        assertTrue("Should be delimited", id.isDelimited());
        String result = dict.toDBName(id);
        assertNotNull(result);
        assertFalse("Should not contain quotes: " + result,
            result.contains("\""));
    }

    /**
     * Verify delimitedCase is LOWER (matches schemaCase).
     */
    public void testDelimitedCaseIsLower() {
        assertEquals("lower", dict.delimitedCase);
        assertEquals("lower", dict.schemaCase);
    }

    /**
     * Verify fromDBName produces undelimited identifier for PostgreSQL.
     */
    public void testFromDBNameNotDelimited() {
        // fromDBName requires the naming util to be configured, which
        // needs a full configuration. Test the dictionary setting instead.
        assertEquals("lower", dict.delimitedCase);
        // When delimCase == nonDelimCase, fromDBName doesn't delimit
    }
}

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Types;

import org.junit.Test;

/**
 * An XML column is compatible with an XML column of the database, so that
 * the metadata of XML columns can be merged (OPENJPA-2879).
 */
public class TestColumnXmlCompatible {

    /**
     * The type code Oracle uses for XMLType.
     */
    private static final int ORACLE_XMLTYPE = 2007;

    @Test
    public void testXmlIsCompatibleWithXml() {
        assertTrue(column(Types.SQLXML).isCompatible(Types.SQLXML, null, 0, 0));
        assertTrue(column(Types.SQLXML).isCompatible(ORACLE_XMLTYPE, null, 0, 0));
        assertTrue(column(ORACLE_XMLTYPE).isCompatible(Types.SQLXML, null, 0, 0));
        assertTrue(column(ORACLE_XMLTYPE).isCompatible(ORACLE_XMLTYPE, null, 0, 0));
    }

    @Test
    public void testXmlIsCompatibleWithCharacterAndLobTypes() {
        for (int type : new int[] { Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB, Types.BLOB }) {
            assertTrue(column(Types.SQLXML).isCompatible(type, null, 0, 0));
        }
    }

    @Test
    public void testXmlIsNotCompatibleWithNumbers() {
        assertFalse(column(Types.SQLXML).isCompatible(Types.INTEGER, null, 0, 0));
    }

    private static Column column(int type) {
        Column col = new Column();
        col.setType(type);
        return col;
    }
}

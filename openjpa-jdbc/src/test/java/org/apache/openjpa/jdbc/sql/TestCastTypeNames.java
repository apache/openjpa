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

import org.junit.Test;

/**
 * Verifies the type names rendered as the target of a SQL CAST, see OPENJPA-2966.
 */
public class TestCastTypeNames {

    /**
     * A 64 bit value must not be cast to an unsized DECIMAL: that defaults to DECIMAL(10,0) on
     * MySQL/MariaDB (silent truncation) and to DECIMAL(5,0) on Derby (SQLState 22003).
     */
    @Test
    public void testLongCastTypeName() {
        assertEquals("BIGINT", new DBDictionary().getNumberCastTypeName(long.class));
        assertEquals("BIGINT", new DerbyDictionary().getNumberCastTypeName(long.class));
        assertEquals("BIGINT", new H2Dictionary().getNumberCastTypeName(long.class));
        assertEquals("BIGINT", new PostgresDictionary().getNumberCastTypeName(long.class));
        assertEquals("BIGINT", new DB2Dictionary().getNumberCastTypeName(long.class));

        // MySQL and MariaDB do not accept BIGINT as a CAST target, SIGNED [INTEGER] is 64 bit there
        assertEquals("SIGNED", new MySQLDictionary().getNumberCastTypeName(long.class));
        assertEquals("SIGNED", new MariaDBDictionary().getNumberCastTypeName(long.class));

        // size markers of the DDL type name must not leak into the CAST
        assertEquals("NUMBER", new OracleDictionary().getNumberCastTypeName(long.class));
    }

    /**
     * The cast type name is resolved lazily, so a dictionary which assigns its DDL type name after
     * field initialisation (e.g. DB2 z/OS v8 in connectedConfiguration()) is honoured.
     */
    @Test
    public void testLongCastTypeNameIsResolvedLazily() {
        DBDictionary dict = new DB2Dictionary();
        dict.bigintTypeName = "DECIMAL(31,0)";
        assertEquals("DECIMAL(31,0)", dict.getNumberCastTypeName(long.class));

        dict.longCastTypeName = "SIGNED";
        assertEquals("SIGNED", dict.getNumberCastTypeName(long.class));
    }

    @Test
    public void testOtherNumberCastTypeNames() {
        assertEquals("INTEGER", new DBDictionary().getNumberCastTypeName(int.class));
        assertEquals("SIGNED", new MySQLDictionary().getNumberCastTypeName(int.class));
        assertEquals("NUMBER", new OracleDictionary().getNumberCastTypeName(int.class)); // tests lazy inint
        assertEquals("FLOAT", new DBDictionary().getNumberCastTypeName(float.class));
        assertEquals("DOUBLE", new DBDictionary().getNumberCastTypeName(double.class));
    }

    /**
     * The string cast must strip the DDL size marker as well - FoxPro used to render
     * CAST(x AS CHARACTER{0}).
     */
    @Test
    public void testStringCastTypeName() {
        assertEquals("VARCHAR", new DBDictionary().getStringCastTypeName());
        assertEquals("CHARACTER", new FoxProDictionary().getStringCastTypeName());
        assertEquals("CHAR(255)", new MySQLDictionary().getStringCastTypeName());
        assertEquals("CHAR(255)", new MariaDBDictionary().getStringCastTypeName());
        assertEquals("VARCHAR(255)", new OracleDictionary().getStringCastTypeName());
    }
}

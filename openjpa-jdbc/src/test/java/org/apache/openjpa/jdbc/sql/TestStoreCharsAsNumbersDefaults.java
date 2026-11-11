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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the <code>StoreCharsAsNumbers</code> default of the Derby, H2,
 * SQL Server, MySQL, MariaDB and Oracle dictionaries, which store Java
 * <code>char</code> fields in <code>CHAR</code> columns unless configured
 * otherwise (OPENJPA-3001, OPENJPA-3033, OPENJPA-3060, OPENJPA-3028).
 */
class TestStoreCharsAsNumbersDefaults {

    /**
     * Puts back the delimiting of reserved words that a configured MySQL or
     * MariaDB dictionary sets for the whole JVM.
     */
    @AfterEach
    public void tearDown() {
        TestAbstractMySQLDictionary.resetDelimitingOfReservedWords();
    }

    @Test
    void derbyStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new DerbyDictionary());
    }

    @Test
    void sqlServerStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new SQLServerDictionary());
    }

    @Test
    void h2StoresCharsAsCharByDefault() {
        assertNativeCharDefault(new H2Dictionary());
    }

    @Test
    void h2ExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new H2Dictionary());
    }

    @Test
    void derbyExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new DerbyDictionary());
    }

    @Test
    void sqlServerExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new SQLServerDictionary());
    }

    @Test
    void mySQLStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new MySQLDictionary());
    }

    @Test
    void mySQLExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new MySQLDictionary());
    }

    @Test
    void mariaDBStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new MariaDBDictionary());
    }

    @Test
    void mariaDBExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new MariaDBDictionary());
    }

    @Test
    void oracleStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new OracleDictionary());
    }

    @Test
    void oracleExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new OracleDictionary());
    }

    @Test
    void oracleDeclaresCharColumnsWithCharacterSemantics() {
        DBDictionary dict = configure(new OracleDictionary(), null);
        assertEquals("CHAR(1 CHAR)", dict.getTypeName(column(Types.CHAR, JavaTypes.CHAR, 1)));
        assertEquals("CHAR(1 CHAR)", dict.getTypeName(column(Types.CHAR, JavaTypes.CHAR_OBJ, 1)));
        // other CHAR and VARCHAR columns keep the database's length semantics
        assertEquals("CHAR(10)", dict.getTypeName(column(Types.CHAR, JavaTypes.STRING, 10)));
        assertEquals("VARCHAR2(1)", dict.getTypeName(column(Types.VARCHAR, JavaTypes.CHAR, 1)));
    }

    private static Column column(int type, int javaType, int size) {
        Column col = new Column();
        col.setType(type);
        col.setJavaType(javaType);
        col.setSize(size);
        return col;
    }

    @Test
    void sybaseDefaultUnchanged() {
        DBDictionary dict = configure(new SybaseDictionary(), null);
        assertTrue(dict.storeCharsAsNumbers);
    }

    private static void assertNativeCharDefault(DBDictionary dict) {
        configure(dict, null);
        assertFalse(dict.storeCharsAsNumbers);
        assertFalse(dict.isStoreCharsAsNumbersExplicit());
        assertEquals(Types.CHAR, dict.getJDBCType(JavaTypes.CHAR, false));
        assertEquals(Types.CHAR, dict.getJDBCType(JavaTypes.CHAR_OBJ, false));
    }

    private static void assertExplicitNumbers(DBDictionary dict) {
        configure(dict, "StoreCharsAsNumbers=true");
        assertTrue(dict.storeCharsAsNumbers);
        assertTrue(dict.isStoreCharsAsNumbersExplicit());
        assertEquals(Types.INTEGER, dict.getJDBCType(JavaTypes.CHAR, false));
        assertEquals(Types.INTEGER, dict.getJDBCType(JavaTypes.CHAR_OBJ, false));
    }

    /**
     * Applies <code>props</code> the same way user supplied
     * <code>openjpa.jdbc.DBDictionary=derby(...)</code> plugin properties are applied.
     */
    private static DBDictionary configure(DBDictionary dict, String props) {
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), props, "DBDictionary");
        return dict;
    }
}

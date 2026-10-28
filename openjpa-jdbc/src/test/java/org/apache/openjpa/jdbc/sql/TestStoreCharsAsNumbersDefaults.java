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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.Test;

/**
 * Tests the <code>StoreCharsAsNumbers</code> default of the Derby, H2 and
 * SQL Server dictionaries, which store Java <code>char</code> fields in
 * <code>CHAR</code> columns unless configured otherwise (OPENJPA-3033).
 */
public class TestStoreCharsAsNumbersDefaults {

    @Test
    public void testDerbyStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new DerbyDictionary());
    }

    @Test
    public void testSQLServerStoresCharsAsCharByDefault() {
        assertNativeCharDefault(new SQLServerDictionary());
    }

    @Test
    public void testH2StoresCharsAsCharByDefault() {
        assertNativeCharDefault(new H2Dictionary());
    }

    @Test
    public void testH2ExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new H2Dictionary());
    }

    @Test
    public void testDerbyExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new DerbyDictionary());
    }

    @Test
    public void testSQLServerExplicitStoreCharsAsNumbers() {
        assertExplicitNumbers(new SQLServerDictionary());
    }

    @Test
    public void testSybaseDefaultUnchanged() {
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

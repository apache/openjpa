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

import static org.apache.openjpa.jdbc.sql.Columns.column;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.jupiter.api.Test;

/**
 * Verifies that H2 maps a <code>BigInteger</code> attribute to a
 * <code>NUMERIC</code> column, as a <code>BIGINT</code> column cannot hold a
 * value with more than 19 digits (OPENJPA-3058).
 */
public class TestH2BigIntegerType {

    @Test
    public void testBigIntegerIsNumeric() {
        H2Dictionary dict = new H2Dictionary();
        assertEquals(Types.NUMERIC, dict.getJDBCType(JavaTypes.BIGINTEGER, false));
        // the overloads used by the column mapping
        assertEquals(Types.NUMERIC, dict.getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0));
        assertEquals(Types.NUMERIC, dict.getJDBCType(JavaTypes.BIGINTEGER, false, 30, 0));
        assertEquals(Types.NUMERIC, dict.getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0, false));
    }

    @Test
    public void testBigIntegerTypeName() {
        for (int version = 1; version <= 2; version++) {
            H2Dictionary dict = new H2Dictionary();
            dict.setMajorVersion(version);
            int type = dict.getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0);
            Column col = column(type, null, 0);
            col.setJavaType(JavaTypes.BIGINTEGER);
            assertEquals("NUMERIC", dict.getTypeName(col));
        }
    }

    @Test
    public void testOtherIntegralTypesAreUnchanged() {
        H2Dictionary dict = new H2Dictionary();
        assertEquals(Types.BIGINT, dict.getJDBCType(JavaTypes.LONG, false, 0, 0));
        assertEquals(Types.BIGINT, dict.getJDBCType(JavaTypes.LONG_OBJ, false, 0, 0));
        assertEquals(Types.INTEGER, dict.getJDBCType(JavaTypes.INT_OBJ, false, 0, 0));
        assertEquals(Types.NUMERIC, dict.getJDBCType(JavaTypes.BIGDECIMAL, false, 0, 0));
    }

    @Test
    public void testStoreLargeNumbersAsStrings() {
        H2Dictionary dict = new H2Dictionary();
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(),
            "StoreLargeNumbersAsStrings=true", "DBDictionary");
        assertEquals(Types.VARCHAR, dict.getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0));
    }

    @Test
    public void testOtherDictionariesAreUnchanged() {
        assertEquals(Types.BIGINT, new DerbyDictionary().getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0));
        assertEquals(Types.BIGINT, new HSQLDictionary().getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0));
    }
}

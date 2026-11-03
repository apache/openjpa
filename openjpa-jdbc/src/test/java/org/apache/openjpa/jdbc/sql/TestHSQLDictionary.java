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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Types;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.Test;

/**
 * Verifies the types {@link HSQLDictionary} uses for columns and casts
 * on HSQLDB 2.x (OPENJPA-3063).
 */
public class TestHSQLDictionary {

    @Test
    public void testLobTypesBeforeFirstConnection() {
        // the version is not known yet, which must not select the HSQLDB 1.x types
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals(Types.CLOB, dict.getPreferredType(Types.CLOB));
        assertEquals(Types.BLOB, dict.getPreferredType(Types.BLOB));
    }

    @Test
    public void testPrecisionAndScaleAreKept() {
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals("NUMERIC(10, 2)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));
        assertEquals("NUMERIC(10)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 0)));
        assertEquals("NUMERIC(19)", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 19, 0)));
    }

    @Test
    public void testUnsizedDecimalKeepsFraction() {
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals("NUMERIC(128,32)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("NUMERIC(128,32)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 2)));
        assertEquals("NUMERIC(128,32)", dict.getTypeName(decimal(JavaTypes.NUMBER, 0, 0)));
        assertEquals("NUMERIC(128,32)", dict.getTypeName(decimal(JavaTypes.DOUBLE, 0, 3)));

        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setType(Types.DECIMAL);
        assertEquals("DECIMAL(128,32)", dict.getTypeName(col));
    }

    @Test
    public void testUnsizedIntegralNumericIsUnchanged() {
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 0, 0)));
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.LONG, 0, 0)));
    }

    @Test
    public void testColumnDefinitionIsUnchanged() {
        HSQLDictionary dict = new HSQLDictionary();
        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setTypeName("NUMERIC");
        assertEquals("NUMERIC", dict.getTypeName(col));
        col.setTypeName("NUMERIC(7,1)");
        assertEquals("NUMERIC(7,1)", dict.getTypeName(col));
    }

    @Test
    public void testConfiguredTypeNameIsKept() {
        HSQLDictionary dict = new HSQLDictionary();
        dict.numericTypeName = "NUMERIC(19,4)";
        assertEquals("NUMERIC(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));

        dict.numericTypeName = "DOUBLE";
        assertEquals("DOUBLE", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
    }

    @Test
    public void testDecimalCastKeepsFraction() {
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals("CAST(? AS NUMERIC(128,32))", cast(dict, Types.NUMERIC));
        assertEquals("CAST(? AS DATE)", cast(dict, Types.DATE));
        assertEquals("CAST(? AS VARCHAR)", cast(dict, Types.VARCHAR));
    }

    @Test
    public void testComparisonCastsKeepColumnPrecision() {
        // TIMESTAMP(9) would keep nanoseconds a TIMESTAMP(6) column has cut
        // off, and an equality comparison with the stored value would fail
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals("CAST(? AS TIME)", cast(dict, Types.TIME));
        assertEquals("CAST(? AS TIMESTAMP)", cast(dict, Types.TIMESTAMP));
    }

    @Test
    public void testExtractOperandCastsKeepFractions() {
        HSQLDictionary dict = new HSQLDictionary();
        assertEquals("DATE", dict.getExtractOperandCastType(Types.DATE));
        assertEquals("TIME(9)", dict.getExtractOperandCastType(Types.TIME));
        assertEquals("TIMESTAMP(9)", dict.getExtractOperandCastType(Types.TIMESTAMP));
    }

    @Test
    public void testOperandsThatNeedACast() {
        HSQLDictionary dict = new HSQLDictionary();
        assertTrue(dict.requiresCastForExtractOperand);
        assertTrue(dict.requiresCastForCaseStringResult);
    }

    private static String cast(DBDictionary dict, int type) {
        SQLBuffer buf = new SQLBuffer(dict);
        dict.appendCast(buf, "?", type);
        return buf.getSQL();
    }

    private static Column decimal(int javaType, int precision, int scale) {
        Column col = column(Types.NUMERIC, null, precision);
        col.setJavaType(javaType);
        col.setDecimalDigits(scale);
        return col;
    }
}

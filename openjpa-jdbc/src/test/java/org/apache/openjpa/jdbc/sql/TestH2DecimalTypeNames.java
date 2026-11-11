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

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.jupiter.api.Test;

/**
 * Verifies the DDL type names of decimal columns on H2. H2 2.x reads an
 * unsized <code>NUMERIC</code> as <code>NUMERIC(100000, 0)</code>, which
 * rounds every stored value to an integer (OPENJPA-3051).
 */
class TestH2DecimalTypeNames {

    @Test
    void precisionAndScaleAreKept() {
        H2Dictionary dict = h2(2);
        assertEquals("NUMERIC(10, 2)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));
        assertEquals("NUMERIC(10)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 0)));
        assertEquals("NUMERIC(19)", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 19, 0)));
        assertEquals("NUMERIC(12, 4)", dict.getTypeName(decimal(JavaTypes.DOUBLE, 12, 4)));

        Column col = decimal(JavaTypes.BIGDECIMAL, 8, 3);
        col.setType(Types.DECIMAL);
        assertEquals("DECIMAL(8, 3)", dict.getTypeName(col));
    }

    @Test
    void unsizedDecimalKeepsFraction() {
        H2Dictionary dict = h2(2);
        assertEquals("DECFLOAT(38)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("DECFLOAT(38)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 2)));
        assertEquals("DECFLOAT(38)", dict.getTypeName(decimal(JavaTypes.NUMBER, 0, 0)));
        assertEquals("DECFLOAT(38)", dict.getTypeName(decimal(JavaTypes.DOUBLE, 0, 3)));
    }

    @Test
    void unsizedIntegralNumericIsUnchanged() {
        H2Dictionary dict = h2(2);
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 0, 0)));
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.LONG, 0, 0)));
    }

    @Test
    void columnDefinitionIsUnchanged() {
        H2Dictionary dict = h2(2);
        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setTypeName("NUMERIC");
        assertEquals("NUMERIC", dict.getTypeName(col));
        col.setTypeName("NUMERIC(7,1)");
        assertEquals("NUMERIC(7,1)", dict.getTypeName(col));
    }

    @Test
    void configuredTypeNameIsKept() {
        H2Dictionary dict = h2(2);
        dict.numericTypeName = "NUMERIC(19,4)";
        assertEquals("NUMERIC(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("NUMERIC(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));

        dict.numericTypeName = "DECFLOAT";
        assertEquals("DECFLOAT", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));

        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setType(Types.DECIMAL);
        dict.decimalTypeName = "DECIMAL(20,6)";
        assertEquals("DECIMAL(20,6)", dict.getTypeName(col));
    }

    @Test
    void h2Version1KeepsUnsizedNumeric() {
        // H2 1.x has no DECFLOAT, its unsized NUMERIC keeps the scale of the value
        H2Dictionary dict = h2(1);
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("NUMERIC(10, 2)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));
    }

    private static H2Dictionary h2(int majorVersion) {
        H2Dictionary dict = new H2Dictionary();
        dict.setMajorVersion(majorVersion);
        return dict;
    }

    private static Column decimal(int javaType, int precision, int scale) {
        Column col = column(Types.NUMERIC, null, precision);
        col.setJavaType(javaType);
        col.setDecimalDigits(scale);
        return col;
    }
}

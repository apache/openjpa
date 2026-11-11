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
package org.apache.openjpa.persistence.jdbc.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Types;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestPrecisionMapping extends SingleEMFTestCase {

    private static final String[] _DOUBLE_FIELDS =
            { "primDbl", "dbl" };
    private static final String _BIG_DECIMAL_FIELD = "bigDecimal";

    @BeforeEach
    public void setUp() {
        setUp(PrecisionTestEntity.class);
    }

    @Test
    void unspecified() {
        doubleMapping("", Types.DOUBLE,0,0);
        bigDecimalMapping("", Types.NUMERIC, 0, 0);
    }

    @Test
    void precisionOnly() {
        doubleMapping("Precis", Types.NUMERIC, 10, 0);
        bigDecimalMapping("Precis", Types.NUMERIC, 10, 0);
    }

    @Test
    void scaleOnly() {
        doubleMapping("Scale", Types.NUMERIC, 0, 10);
        bigDecimalMapping("Scale", Types.NUMERIC, 0, 10);
    }

    @Test
    void precisionAndScale() {
        doubleMapping("PrecisScale", Types.NUMERIC,10,10);
        bigDecimalMapping("PrecisScale", Types.NUMERIC, 10, 10);
    }

    @Test
    private void bigDecimalMapping(String fieldSuffix, int expectedType,
            int expectedPrecision, int expectedScale) {
        ClassMapping mapping = getMapping(PrecisionTestEntity.class);
        FieldMapping fm =
                mapping.getFieldMapping(_BIG_DECIMAL_FIELD + fieldSuffix);

        Column[] cols = fm.getColumns();
        assertEquals(1, cols.length);
        assertEquals(expectedType, cols[0].getType());
        assertEquals(expectedPrecision, cols[0].getSize());
        assertEquals(expectedScale, cols[0].getDecimalDigits());
    }

    @Test
    private void doubleMapping(String fieldSuffix, int expectedType,
            int expectedPrecision, int expectedScale) {
        ClassMapping mapping = getMapping(PrecisionTestEntity.class);
        FieldMapping fm;

        for(String s : _DOUBLE_FIELDS) {
            fm = mapping.getFieldMapping(s + fieldSuffix);

            Column[] cols = fm.getColumns();
            assertEquals(1, cols.length);
            assertEquals(expectedType ,cols[0].getType());
            assertEquals(expectedPrecision, cols[0].getSize());
            assertEquals(expectedScale, cols[0].getDecimalDigits());
        }
    }
}

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

import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.junit.Test;

public class TestOracleDictionary {

    /**
     * Oracle has no EXTRACT(QUARTER) and no EXTRACT(WEEK); the ISO week is the IW format of TO_CHAR.
     */
    @Test
    public void testExtractQuarterAndIsoWeek() {
        DBDictionary dict = new OracleDictionary();
        assertEquals("TO_NUMBER(TO_CHAR({1}, 'Q'))", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.QUARTER));
        assertEquals("TO_NUMBER(TO_CHAR({1}, 'IW'))", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.WEEK));
    }

    /**
     * EXTRACT(HOUR/MINUTE/SECOND) is only allowed on TIMESTAMP values, but times are held in DATE columns.
     */
    @Test
    public void testExtractTimeFieldsFromTimestamp() {
        DBDictionary dict = new OracleDictionary();
        assertEquals("EXTRACT(HOUR FROM CAST({1} AS TIMESTAMP))",
            dict.getExtractDateTimeFieldFunction(DateTimeExtractField.HOUR));
        assertEquals("EXTRACT(MINUTE FROM CAST({1} AS TIMESTAMP))",
            dict.getExtractDateTimeFieldFunction(DateTimeExtractField.MINUTE));
        assertEquals("EXTRACT(SECOND FROM CAST({1} AS TIMESTAMP))",
            dict.getExtractDateTimeFieldFunction(DateTimeExtractField.SECOND));
        assertEquals("EXTRACT(DAY FROM {1})", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.DAY));
        assertEquals("EXTRACT(YEAR FROM {1})", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.YEAR));
    }
}

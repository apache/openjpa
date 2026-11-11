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

import java.lang.reflect.Proxy;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.DateTimeExtractPart;
import org.junit.jupiter.api.Test;

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

    /**
     * Oracle has no CURRENT_TIME; a time is held as the time of day on 1970-01-01, as the driver binds it.
     */
    @Test
    public void testCurrentTime() {
        assertEquals("(DATE '1970-01-01' + (CURRENT_DATE - TRUNC(CURRENT_DATE)))", new OracleDictionary().currentTimeFunction);
    }

    /**
     * The CURRENT_DATE of Oracle is a DATE with the time of day.
     */
    @Test
    public void testCurrentDate() {
        assertEquals("TRUNC(CURRENT_DATE)", new OracleDictionary().currentDateFunction);
    }

    /**
     * An Oracle DATE has a time of day and there is no TIME type, so EXTRACT(DATE) truncates the value
     * and EXTRACT(TIME) moves its time of day to 1970-01-01.
     */
    @Test
    public void testExtractDateAndTimePart() {
        DBDictionary dict = new OracleDictionary();
        assertEquals("TRUNC({1})", dict.getExtractDateTimePartFunction(DateTimeExtractPart.DATE));
        assertEquals("(DATE '1970-01-01' + (CAST({1} AS DATE) - TRUNC({1})))",
            dict.getExtractDateTimePartFunction(DateTimeExtractPart.TIME));

        DBDictionary standard = new DBDictionary();
        assertEquals("CAST({1} AS DATE)", standard.getExtractDateTimePartFunction(DateTimeExtractPart.DATE));
        assertEquals("CAST({1} AS TIME)", standard.getExtractDateTimePartFunction(DateTimeExtractPart.TIME));
    }

    /**
     * SUBSTR(str, -length) is NULL when length exceeds the length of the string, so RIGHT computes the
     * start position from the length of the string. A length of 0 starts behind the last character, which
     * gives NULL, the empty string of Oracle.
     */
    @Test
    public void testRight() {
        DBDictionary dict = new OracleDictionary();
        SQLBuffer buf = new SQLBuffer(dict);
        dict.right(buf, value(null, "t0.NAME"), value(3, "?"));
        assertEquals("SUBSTR(t0.NAME, GREATEST(LENGTH(t0.NAME) - (3) + 1, 1))", buf.getSQL());

        buf = new SQLBuffer(dict);
        dict.right(buf, value(null, "t0.NAME"), value(0, "?"));
        assertEquals("SUBSTR(t0.NAME, GREATEST(LENGTH(t0.NAME) - (0) + 1, 1))", buf.getSQL());

        buf = new SQLBuffer(dict);
        dict.right(buf, value(null, "t0.NAME"), value(null, "t0.LEN"));
        assertEquals("SUBSTR(t0.NAME, GREATEST(LENGTH(t0.NAME) - (t0.LEN) + 1, 1))", buf.getSQL());
    }

    /**
     * A query value with the given constant value, appending the given SQL.
     */
    private static FilterValue value(Object constant, String sql) {
        return (FilterValue) Proxy.newProxyInstance(FilterValue.class.getClassLoader(), new Class<?>[] { FilterValue.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getValue":
                        return constant;
                    case "appendTo":
                        if (args[0] instanceof SQLBuffer buf) {
                            buf.append(sql);
                        }
                        return null;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }
}

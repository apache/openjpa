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

import org.apache.openjpa.jdbc.schema.Column;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDBDictionary {
    @Test
    public void snakeCase() {
        final DBDictionary dictionary = new DBDictionary();
        assertEquals("foo", dictionary.toSnakeCase("foo"));
        assertEquals("foo_bar", dictionary.toSnakeCase("fooBar"));
        assertEquals("fooba_r", dictionary.toSnakeCase("FoobaR"));
        assertEquals("o_f_o_ob", dictionary.toSnakeCase("oFOOb"));
        assertEquals("\"foo_bar\"", dictionary.toSnakeCase("\"fooBar\""));
    }

    /**
     * A column whose precision (fractional digits of a second) is known holds
     * multiples of the corresponding power of ten; otherwise timestamps are
     * rounded to the dictionary's datePrecision (OPENJPA-3034).
     */
    @Test
    public void timestampPrecision() {
        final DBDictionary dictionary = new DBDictionary();
        assertEquals(dictionary.datePrecision, dictionary.getTimestampPrecision(null));
        Column col = new Column();
        assertEquals(dictionary.datePrecision, dictionary.getTimestampPrecision(col));
        int expected = 1_000_000_000;
        for (int digits = 0; digits <= 9; digits++) {
            col.setPrecision(digits);
            assertEquals("digits " + digits, expected, dictionary.getTimestampPrecision(col));
            expected /= 10;
        }
        // not a number of fractional digits, e.g. the precision of a procedure parameter
        col.setPrecision(23);
        assertEquals(dictionary.datePrecision, dictionary.getTimestampPrecision(col));
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.apache.openjpa.jdbc.kernel.exps.Param;
import org.apache.openjpa.kernel.exps.Parameter;
import org.junit.jupiter.api.Test;

class TestSQLBuffer {

    /**
     * A parameter that occurs twice with another parameter in between, as
     * :a in :a BETWEEN :b AND x, must keep the position of each occurrence.
     */
    @Test
    void userParameterRepeatedWithOtherInBetween() {
        DBDictionary dict = new DBDictionary();
        Parameter a = new Param("a", Object.class);
        Parameter b = new Param("b", Object.class);

        SQLBuffer cond = new SQLBuffer(dict);
        cond.appendValue("x", null, a).append(" >= ").appendValue("y", null, b)
            .append(" AND ").appendValue("x", null, a).append(" <= t0.NAME");

        SQLBuffer buf = new SQLBuffer(dict);
        buf.append("SELECT t0.ID FROM T t0 WHERE ").append(cond);

        assertEquals(Arrays.asList(0, a, 1, b, 2, a), buf.getUserParameters());
    }

    /**
     * The values of a parameter that occurs once per element of an IN list
     * keep their own positions.
     */
    @Test
    void userParameterRepeatedInSequence() {
        DBDictionary dict = new DBDictionary();
        Parameter a = new Param("a", Object.class);
        Parameter b = new Param("b", Object.class);

        SQLBuffer cond = new SQLBuffer(dict);
        cond.append("t0.NAME = ").appendValue("y", null, b).append(" AND t0.ID IN (")
            .appendValue(1, null, a).append(", ").appendValue(2, null, a).append(")");

        SQLBuffer buf = new SQLBuffer(dict);
        buf.append("SELECT t0.ID FROM T t0 WHERE ").append(cond);

        assertEquals(Arrays.asList(0, b, 1, a, 2, a), buf.getUserParameters());
    }

    /**
     * With literals instead of parameters, a string value is written with its
     * single quotes doubled.
     */
    @Test
    void stringLiteralWithQuote() {
        SQLBuffer buf = new SQLBuffer(new DBDictionary());
        buf.append("t0.NAME = ").appendValue("O'Brien", null, null, false);

        assertEquals("t0.NAME = 'O''Brien'", buf.getSQL());
        assertEquals(Collections.emptyList(), buf.getParameters());
    }

    /**
     * A string value with a backslash is written as a literal for a database
     * which reads the backslash as an ordinary character, and bound as a
     * parameter otherwise.
     */
    @Test
    void stringLiteralWithBackslash() {
        SQLBuffer buf = new SQLBuffer(new DBDictionary());
        buf.append("t0.NAME = ").appendValue("abc\\'", null, null, false);
        assertEquals("t0.NAME = 'abc\\'''", buf.getSQL());

        DBDictionary dict = new DBDictionary();
        dict.backslashEscapesInStringLiterals = true;
        dict.storeCharsAsNumbers = false;
        buf = new SQLBuffer(dict);
        buf.append("t0.NAME = ").appendValue("abc\\'", null, null, false)
            .append(" AND t0.CODE = ").appendValue('\\', null, null, false)
            .append(" AND t0.ALT = ").appendValue("O'Brien", null, null, false);
        assertEquals("t0.NAME = ? AND t0.CODE = ? AND t0.ALT = 'O''Brien'", buf.getSQL());
        assertEquals(Arrays.asList("abc\\'", '\\'), buf.getParameters());
    }

    @Test
    void toStringLiteral() {
        DBDictionary dict = new DBDictionary();
        assertEquals("'a\\b'", dict.toStringLiteral("a\\b"));
        assertEquals("''''", dict.toStringLiteral("'"));

        dict.backslashEscapesInStringLiterals = true;
        assertNull(dict.toStringLiteral("trail\\"));
        assertEquals("'O''Brien'", dict.toStringLiteral("O'Brien"));
    }

    @Test
    void toStringLiteralSQLServer() {
        DBDictionary dict = new SQLServerDictionary();
        assertEquals("'O''Brien'", dict.toStringLiteral("O'Brien"));
        assertEquals("'a\\b'", dict.toStringLiteral("a\\b"));
        assertNull(dict.toStringLiteral("\u00e9t\u00e9"));
        assertNull(dict.toStringLiteral("\u03a9"));

        SQLBuffer buf = new SQLBuffer(dict);
        buf.append("t0.NAME = ").appendValue("\u03a9", null, null, false)
            .append(" AND t0.ALT = ").appendValue("abc", null, null, false);
        assertEquals("t0.NAME = ? AND t0.ALT = 'abc'", buf.getSQL());
        assertEquals(Arrays.asList("\u03a9"), buf.getParameters());
    }
}

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

import java.util.Arrays;

import org.apache.openjpa.jdbc.kernel.exps.Param;
import org.apache.openjpa.kernel.exps.Parameter;
import org.junit.Test;

public class TestSQLBuffer {

    /**
     * A parameter that occurs twice with another parameter in between, as
     * :a in :a BETWEEN :b AND x, must keep the position of each occurrence.
     */
    @Test
    public void testUserParameterRepeatedWithOtherInBetween() {
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
    public void testUserParameterRepeatedInSequence() {
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
}

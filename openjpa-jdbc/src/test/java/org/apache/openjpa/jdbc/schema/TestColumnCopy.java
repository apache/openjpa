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
package org.apache.openjpa.jdbc.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link Column#copy} carries the JPA 3.2 <code>secondPrecision</code> of a
 * template, as it does its size and scale. A <code>@Version</code> field
 * reaches its version column through such a copy (OPENJPA-3034).
 */
class TestColumnCopy {

    @Test
    void secondPrecisionIsCopied() {
        Column from = new Column();
        from.setSecondPrecision(0);
        Column to = new Column();
        to.copy(from);
        assertEquals(0, to.getSecondPrecision());
    }

    @Test
    void secondPrecisionOfTargetIsKept() {
        Column from = new Column();
        from.setSecondPrecision(3);
        Column to = new Column();
        to.setSecondPrecision(6);
        to.copy(from);
        assertEquals(6, to.getSecondPrecision());

        Column unset = new Column();
        to = new Column();
        to.copy(unset);
        assertEquals(-1, to.getSecondPrecision());
    }
}

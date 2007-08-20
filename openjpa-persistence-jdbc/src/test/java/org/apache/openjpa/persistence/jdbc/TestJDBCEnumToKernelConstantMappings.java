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
package org.apache.openjpa.persistence.jdbc;

import junit.framework.TestCase;
import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.LRSSizes;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;

public class TestJDBCEnumToKernelConstantMappings
    extends TestCase {

    public void testEagerFetchModes() {
        assertEquals(EagerFetchModes.EAGER_NONE,
            EagerFetchType.NONE.toKernelConstant());
        assertEquals(EagerFetchType.NONE,
            EagerFetchType.fromKernelConstant(
                EagerFetchModes.EAGER_NONE));
        assertEquals(EagerFetchType.NONE.toKernelConstant(),
            EagerFetchType.NONE.ordinal());

        assertEquals(EagerFetchModes.EAGER_JOIN,
            EagerFetchType.JOIN.toKernelConstant());
        assertEquals(EagerFetchType.JOIN,
            EagerFetchType.fromKernelConstant(
                EagerFetchModes.EAGER_JOIN));
        assertEquals(EagerFetchType.JOIN.toKernelConstant(),
            EagerFetchType.JOIN.ordinal());

        assertEquals(EagerFetchModes.EAGER_PARALLEL,
            EagerFetchType.PARALLEL.toKernelConstant());
        assertEquals(EagerFetchType.PARALLEL,
            EagerFetchType.fromKernelConstant(
                EagerFetchModes.EAGER_PARALLEL));
        assertEquals(EagerFetchType.PARALLEL.toKernelConstant(),
            EagerFetchType.PARALLEL.ordinal());

        assertEquals(getConstantCount(EagerFetchModes.class),
            EagerFetchType.values().length);
    }

    public void testLRSSizeType() {
        assertEquals(LRSSizes.SIZE_UNKNOWN,
            LRSSizeType.UNKNOWN.toKernelConstant());
        assertEquals(LRSSizeType.UNKNOWN,
            LRSSizeType.fromKernelConstant(
                LRSSizes.SIZE_UNKNOWN));
        assertEquals(LRSSizeType.UNKNOWN.toKernelConstant(),
            LRSSizeType.UNKNOWN.ordinal());

        assertEquals(LRSSizes.SIZE_LAST,
            LRSSizeType.LAST.toKernelConstant());
        assertEquals(LRSSizeType.LAST,
            LRSSizeType.fromKernelConstant(
                LRSSizes.SIZE_LAST));
        assertEquals(LRSSizeType.LAST.toKernelConstant(),
            LRSSizeType.LAST.ordinal());

        assertEquals(LRSSizes.SIZE_QUERY,
            LRSSizeType.QUERY.toKernelConstant());
        assertEquals(LRSSizeType.QUERY,
            LRSSizeType.fromKernelConstant(
                LRSSizes.SIZE_QUERY));
        assertEquals(LRSSizeType.QUERY.toKernelConstant(),
            LRSSizeType.QUERY.ordinal());

        assertEquals(getConstantCount(LRSSizes.class),
            LRSSizeType.values().length);
    }

    public void testJoinSyntaxType() {
        assertEquals(JoinSyntaxes.SYNTAX_SQL92,
            JoinSyntaxType.SQL92.toKernelConstant());
        assertEquals(JoinSyntaxType.SQL92,
            JoinSyntaxType.fromKernelConstant(
                JoinSyntaxes.SYNTAX_SQL92));
        assertEquals(JoinSyntaxType.SQL92.toKernelConstant(),
            JoinSyntaxType.SQL92.ordinal());

        assertEquals(JoinSyntaxes.SYNTAX_TRADITIONAL,
            JoinSyntaxType.TRADITIONAL.toKernelConstant());
        assertEquals(JoinSyntaxType.TRADITIONAL,
            JoinSyntaxType.fromKernelConstant(
                JoinSyntaxes.SYNTAX_TRADITIONAL));
        assertEquals(JoinSyntaxType.TRADITIONAL.toKernelConstant(),
            JoinSyntaxType.TRADITIONAL.ordinal());

        assertEquals(JoinSyntaxes.SYNTAX_DATABASE,
            JoinSyntaxType.DATABASE.toKernelConstant());
        assertEquals(JoinSyntaxType.DATABASE,
            JoinSyntaxType.fromKernelConstant(
                JoinSyntaxes.SYNTAX_DATABASE));
        assertEquals(JoinSyntaxType.DATABASE.toKernelConstant(),
            JoinSyntaxType.DATABASE.ordinal());

        assertEquals(getConstantCount(JoinSyntaxes.class),
            JoinSyntaxType.values().length);
    }

    private int getConstantCount(Class cls) {
        return cls.getDeclaredFields().length;
    }
}
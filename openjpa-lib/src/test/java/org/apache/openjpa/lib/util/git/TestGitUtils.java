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

package org.apache.openjpa.lib.util.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestGitUtils {

    @Test
    public void testNull() {
        assertEquals(-1, GitUtils.convertGitInfoToPCEnhancerVersion(null));
    }

    @Test
    public void testBasic() {
        long i = 0xBC614E;
        assertEquals(i, GitUtils.convertGitInfoToPCEnhancerVersion("BC614E"));
    }

    @Test
    public void testBasic2() {
        int i = 0xfef543b;
        assertEquals(i, GitUtils.convertGitInfoToPCEnhancerVersion("fef543b"));
    }

    @Test
    public void testGoodTrailingString() {
        long i = 0xBC614E;
        assertEquals(i, GitUtils.convertGitInfoToPCEnhancerVersion("BC614Em"));
        assertEquals(i, GitUtils.convertGitInfoToPCEnhancerVersion("BC614EM"));
    }

    @Test
    public void testBad() {
        long i = 0xBC614E;
        assertEquals(-1, GitUtils.convertGitInfoToPCEnhancerVersion(i + "BC614Ems"));
        assertEquals(-1, GitUtils.convertGitInfoToPCEnhancerVersion("ZC614EM"));
        assertEquals(-1, GitUtils.convertGitInfoToPCEnhancerVersion("ZC614E"));
    }
}

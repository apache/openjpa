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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TestGitUtils {

    @ParameterizedTest(name = "#{index} convertGitInfoToPCEnhancerVersion('{0}') = -1")
    @NullAndEmptySource
    @ValueSource(strings = {"ZC614EM", "ZC614E", "1BC614Ems"})
    void testNullAndBad(String input) {
        assertEquals(-1, GitUtils.convertGitInfoToPCEnhancerVersion(input));
    }

    @ParameterizedTest(name = "#{index} convertGitInfoToPCEnhancerVersion('{0}') = {1}")
    @MethodSource("validGitInfo")
    void validGitInfo(String input, long expected) {
        assertEquals(expected, GitUtils.convertGitInfoToPCEnhancerVersion(input));
    }

    private static Stream<Arguments> validGitInfo() {
        return Stream.of(
            Arguments.of("BC614E", 0xBC614EL),
            Arguments.of("BC614Em", 0xBC614EL),
            Arguments.of("BC614EM", 0xBC614EL),
            Arguments.of("fef543b", 0xfef543bL)
        );
    }
}

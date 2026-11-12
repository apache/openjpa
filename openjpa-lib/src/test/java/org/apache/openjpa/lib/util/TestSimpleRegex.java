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
package org.apache.openjpa.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests simple regex for use in in-memory query execution.
 *
 * @author Greg Campbell
 */
class TestSimpleRegex  {

    private boolean matchExpr(String target, String expr, boolean caseInsens) {
        SimpleRegex re = new SimpleRegex(expr, caseInsens);
        return re.matches(target);
    }

    @ParameterizedTest(name = "#{index} matchExpr(\"{0}\", \"{1}\", {2}) = {3}")
    @CsvSource({
        "Hello, Hello, false, true",
        "Hello, Bye, false, false",
        "Hello, ByeBye, false, false",
        "Hello, Hellooo, false, false",
        "Hello, HHello, false, false",
        "Hello, H.llo, false, true",
        "Hello, Hell.*, false, true",
        "Yo Hello, .*ello, false, true",
        "Hello, .*ello, false, true",
        "Hello, .*ell.*, false, true",
        "Hellow, .*ell.*, false, true",
        "Hello, Hel.*lo, false, true",
        "HelYolo, Hel.*lo, false, true",
        "Hello, H.*lo, false, true",
        "Hellowe, H.*lo, false, false",
        "Hello, h.*lo, true, true",
        "Hello, h.*lo, false, false",
        "The quick brown fox jumped over the lazy dog, The .*brown.*dog, false, true",
        "The quick brown fox jumped over the lazy dog, The .*br.wn.*d.g, false, true",
        "the quick BRown fox jumped over the lazy dog, The .*br.wn.*d.g, true, true",
        "The quick brown fox jumped over the lazy dog, The .*brown.*dogg, false, false",
        "The quick brown fox jumped over the lazy dog, TThe .*brown.*dogg, false, false",
        "Yo Hellow, .*ello, false, false",
        "Hellow, .*YoHello, false, false"
    })
    void wildcards(String target, String expr, boolean caseInsens, boolean expected) {
        assertEquals(expected, matchExpr(target, expr, caseInsens));
    }

}

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
package org.apache.openjpa.conf;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SpecificationAdditionalTests {

	String myString = "JPA";
	String myCompleteString = "JPA 2.0-draft";
	String myCompleteString2 = "JPA 2.1-draft";
	String myHalfCompleteString = "JPA 2";
	String myHalfCompleteString2 = "JPQ 2";
	String myNullString = null;


    @Test
    void EqualsTest() {
		Specification spec = new Specification(myString);
        assertEquals(spec, spec); // parse() is overidded in Specification
	}

    @Test
    void EqualsTest2() {
		Specification spec = new Specification(myString);
        assertNotEquals(spec, myNullString); // parse() is overidded in Specification
	}


    @Test
    void EqualsTest3() {
		Specification spec = new Specification(myString);
		Specification spec2 = new Specification(myHalfCompleteString);
        assertNotEquals(spec, spec2); // parse() is overidded in Specification
	}

    @Test
    void EqualsTest4() {
		Specification spec = new Specification(myString);
        assertNotEquals(1, spec); // parse() is overidded in Specification
	}

    @Test
    void EqualsTest5() {
		Specification spec = new Specification(myHalfCompleteString);
		Specification spec2 = new Specification(myHalfCompleteString2);
        assertNotEquals(spec, spec2);
	}

    @Test
    void EqualsTest6() {
		Specification spec = new Specification(myCompleteString);
		Specification spec2 = new Specification(myHalfCompleteString2);
        assertNotEquals(spec, spec2);
	}
}

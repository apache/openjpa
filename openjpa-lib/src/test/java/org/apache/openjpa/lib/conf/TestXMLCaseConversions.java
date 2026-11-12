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
package org.apache.openjpa.lib.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestXMLCaseConversions {

	private static final Logger logger = Logger.getLogger(TestXMLCaseConversions.class.getCanonicalName());

    @ParameterizedTest(name = "#{index} toXMLName(\"{0}\") = \"{1}\"")
    @CsvSource({
        "easyXmlConversion, easy-xml-conversion",
        "InitialCaps, initial-caps",
        "nodash, nodash",
        "Anothernodash, anothernodash",
        "MUltipleCaps, multiple-caps",
        "TrailingMultiCAPS, trailing-multi-caps",
        "TwoINnerCaps, two-i-nner-caps",
        "FourINNErCaps, four-inn-er-caps",
        "Inner3Number, inner-3-number",
        "Inner03Number, inner-03-number"
    })
    void toXMLName(String input, String expected) {
        assertEquals(expected, ConfigurationImpl.toXMLName(input));
    }

    public static void main(String[] args) throws IOException {
    	try (BufferedReader r = new BufferedReader (new FileReader(new File(args[0])))) {
    		while (true) {
    			String s = r.readLine();
    			if (s == null)
    				break;
    			logger.fine(() -> String.format("%s: %s", s, ConfigurationImpl.toXMLName(s)));
    		}
    	}
    }
}

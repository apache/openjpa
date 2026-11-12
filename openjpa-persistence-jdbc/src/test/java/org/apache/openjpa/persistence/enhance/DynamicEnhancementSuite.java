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
package org.apache.openjpa.persistence.enhance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.openjpa.DynamicEnhancementTest;
import org.apache.openjpa.enhance.PCEnhancerAgent;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import jakarta.persistence.Persistence;

@Suite
@SuiteDisplayName("JUnit Platform Suite Demo")
@SelectPackages("org.apache.openjpa.persistence")
@IncludeTags(DynamicEnhancementTest.TAG_NAME)
public class DynamicEnhancementSuite {
    @BeforeSuite
    static void beforeSuite() throws IOException {
        Persistence.createEntityManagerFactory("test", System.getProperties());

        /*
         * Fails if the suite runs without the enhancer agent or on test classes
         * that were enhanced at build time. In both cases the other tests do not
         * exercise the class file transformer at all.
         */
        assertTrue(PCEnhancerAgent.getLoadSuccessful(),
            "PCEnhancerAgent was not started, run the suite with -Ptest-dynamic-enhancer");
        byte[] bytes;
        try (InputStream in = AllFieldTypes.class.getResourceAsStream("AllFieldTypes.class")) {
            assertNotNull(in, "AllFieldTypes.class not found");
            bytes = in.readAllBytes();
        }
        String classFile = new String(bytes, StandardCharsets.ISO_8859_1);
        assertFalse(classFile.contains("org/apache/openjpa/enhance/PersistenceCapable"),
            "AllFieldTypes.class was enhanced at build time, run mvn clean first");
        assertTrue(PersistenceCapable.class.isAssignableFrom(AllFieldTypes.class),
            "AllFieldTypes was not enhanced when it was loaded");
    }
}

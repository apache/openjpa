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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class TestConfigurationProviderPrefixes {

    private static final String CUSTOM_PREFIX =
        TestConfigurationProviderPrefixes.class.getName();

    private String[] _origPrefixes;

    @BeforeEach
    public void setUp() {
        _origPrefixes = ProductDerivations.getConfigurationPrefixes();
        List l = new ArrayList(Arrays.asList(_origPrefixes));
        l.add(CUSTOM_PREFIX);
        ProductDerivations.setConfigurationPrefixes(
            (String[]) l.toArray(new String[0]));
    }

    @AfterEach
    public void tearDown() {
        ProductDerivations.setConfigurationPrefixes(_origPrefixes);
    }

    @Test
    public void testPrefixContents() {
        String[] prefixes = ProductDerivations.getConfigurationPrefixes();
        Assertions.assertEquals(CUSTOM_PREFIX, prefixes[prefixes.length - 1]);
        Assertions.assertEquals("openjpa", prefixes[0]);
    }

    @Test
    public void testPartialKeyAndNullMap() {
        assertEquals("openjpa.Foo", "Foo", (Map) null, null);
    }

    @Test
    public void testPartialKeyWithInvalidPrefix() {
        Map<String, String> map = Map.of("bar.Foo", "value");
        assertEquals("openjpa.Foo", "Foo", map, null);
    }

    @Test
    public void testPartialKeyWithoutMatch() {
        Map<String, String> map = Map.of("bar.Baz", "value");
        assertEquals("openjpa.Foo", "Foo", map, null);
    }

    @Test
    public void testPartialKeyWithOpenJPAMatch() {
        Map<String, String> map = Map.of("openjpa.Foo", "value");
        assertEquals("openjpa.Foo", "Foo", map, "value");
    }

    @Test
    public void testPartialKeyWithCustomMatch() {
        Map<String, String> map = Map.of(CUSTOM_PREFIX + ".Foo", "value");
        assertEquals(CUSTOM_PREFIX + ".Foo", "Foo", map, "value");
    }

    @Test
    public void testPartialKeyDuplicateFullKeys() {
        Map<String, String> map = Map.of(CUSTOM_PREFIX + ".Foo", "value",
            "openjpa.Foo", "value");
        try {
            ProductDerivations.getConfigurationKey("Foo", map);
            fail("duplicate keys should result in an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    private static void assertEquals(String fullKey, String partialKey, Map map, Object value) {
        Assertions.assertEquals(fullKey, ProductDerivations.getConfigurationKey(partialKey, map));
        if (map != null) {
            Assertions.assertEquals(value, map.get(fullKey));
        }
    }
}

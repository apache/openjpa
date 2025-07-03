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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test UUID generation.
 *
 * @author Abe White
 */
public class TestUUIDGenerator {

    @Test
    public void testUniqueString() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++)
            assertTrue(seen.add(UUIDGenerator.nextString(UUIDGenerator.TYPE1)));
    }

    @Test
    public void testUniqueHex() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++)
            assertTrue(seen.add(UUIDGenerator.nextHex(UUIDGenerator.TYPE1)));
    }

    @Test
    public void testUniqueType4String() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++)
            assertTrue(seen.add(UUIDGenerator.nextString(UUIDGenerator.TYPE4)));
    }

    @Test
    public void testUniqueType4Hex() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++)
            assertTrue(seen.add(UUIDGenerator.nextHex(UUIDGenerator.TYPE4)));
    }

    @Test
    public void testUniqueMixedTypesHex() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            int type = (i % 2 == 0) ?
                UUIDGenerator.TYPE4 : UUIDGenerator.TYPE1;
            assertTrue(seen.add(UUIDGenerator.nextHex(type)));
        }
    }

    @Test
    public void testGetTime() {
        long time = 0;
        for (int i = 0; i < 10000; i++) {
            long newTime = UUIDGenerator.getTime();
            assertTrue(newTime != time);
            time = newTime;
        }
    }

    @Test
    public void testInitType1MultiThreaded() throws Exception {
        // This test method depends IP and RANDOM in UUIDGenerator to be null
        // and type1Initialized to be false. Using reflection to ensure that
        // those fields are null.
        Class<UUIDGenerator> uuid = UUIDGenerator.class;
        Field[] fields = uuid.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals("type1Initialized")) {
                f.setAccessible(true);
                f.set(null, false);
            } else if (f.getName().equals("IP") || f.getName().equals("RANDOM")) {
                f.setAccessible(true);
                f.set(null, null);
            }
        }
        Thread t = new Thread() {
            @Override
            public void run() {
                UUIDGenerator.createType1();
            }
        };

        t.start();
        UUIDGenerator.createType1();
    }
    
}

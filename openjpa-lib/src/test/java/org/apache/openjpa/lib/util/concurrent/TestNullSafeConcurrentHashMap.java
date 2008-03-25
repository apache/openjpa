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
package org.apache.openjpa.lib.util.concurrent;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Map.Entry;

import org.apache.openjpa.lib.test.AbstractTestCase;

public class TestNullSafeConcurrentHashMap extends AbstractTestCase {

    private Map newMap() {
        return new NullSafeConcurrentHashMap();
//        return new HashMap();
    }

    public void testNullKeys() throws ClassNotFoundException, IOException {
        Map m = newMap();
        helper(m, null, "value 0", "value 1", "value 2");
    }

    private void helper(Map m, Object key, Object value0,
        Object value1, Object value2)
        throws IOException, ClassNotFoundException {

        // initial put
        m.put(key, value0);

        // get etc.
        assertEquals(value0, m.get(key));
        assertTrue(m.containsKey(key));
        assertTrue(m.containsValue(value0));

        // keySet
        Set keys = m.keySet();
        assertTrue(keys.contains(key));
        assertEquals(1, keys.size());
        assertEquals(key, keys.iterator().next());

        // entrySet
        Set entries = m.entrySet();
        Entry e = (Entry) entries.iterator().next();
        assertEquals(key, e.getKey());
        assertEquals(value0, e.getValue());

        // values
        Collection values = m.values();
        assertEquals(1, values.size());
        assertEquals(value0, values.iterator().next());

        // serializability
        assertEquals(m, roundtrip(m, true));

        // put
        assertEquals(value0, m.put(key, value1));
//        m.putAll(); #####

        // remove
        assertEquals(value1, m.put(key, value1));
        assertEquals(value1, m.remove(key));
        m.put(key, value1);

        // ConcurrentMap stuff
        ConcurrentMap cm = (ConcurrentMap) m;
        assertFalse(cm.remove("invalid key", value0));
        assertTrue(cm.remove(key, value1));
        assertNull(cm.putIfAbsent(key, value0)); // null == prev unset

        // value0 might be null; can't disambiguate from above in OpenJPA
        // interpretation
        assertEquals(value0, cm.putIfAbsent(key, "invalid value"));

        // replace
        assertEquals(value0, cm.replace(key, value1));
        assertTrue(cm.replace(key, value1, value2));
    }

    public void testNullValues() throws ClassNotFoundException, IOException {
        Map m = newMap();
        nullValsHelper(m, "foo");
    }

    private void nullValsHelper(Map m, Object key)
        throws IOException, ClassNotFoundException {
        helper(m, key, null, null, null);
        helper(m, key, "bar", "baz", "quux");

        helper(m, key, "bar", "baz", null);
        helper(m, key, null, "baz", "quux");
        helper(m, key, "bar", null, "quux");

        helper(m, key, "bar", null, null);
        helper(m, key, null, "baz", null);
        helper(m, key, null, null, "quux");
    }

    public void testNullKeysAndValues()
        throws ClassNotFoundException, IOException {
        Map m = newMap();
        nullValsHelper(m, null);
    }
}

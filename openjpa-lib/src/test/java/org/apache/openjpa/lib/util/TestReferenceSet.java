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
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Iterator;

import org.apache.openjpa.lib.util.collections.AbstractReferenceMap.ReferenceStrength;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link ReferenceHashSet}.
 *
 * @author Abe White
 */
class TestReferenceSet {

    private ReferenceHashSet _coll = new ReferenceHashSet(ReferenceStrength.WEAK);

    private Object _heldValue = 2;

    @BeforeEach
    void setUp() {
        _coll.add(_heldValue);
        _coll.add(1);
    }

    /**
     * Tests basic add/contains/remove functionality.
     */
    @Test
    void basics() {
        Collection coll = new ReferenceHashSet(ReferenceStrength.WEAK);
        assertEquals(0, coll.size());
        assertFalse(coll.contains("foo"));

        assertTrue(coll.add("foo"));
        assertEquals(1, coll.size());
        assertTrue(coll.contains("foo"));
        assertEquals("foo", coll.iterator().next());

        assertFalse(coll.remove("bar"));
        assertEquals(1, coll.size());
        assertTrue(coll.remove("foo"));
        assertEquals(0, coll.size());
        assertTrue(coll.isEmpty());
    }

    /**
     * Test that values with strong references are not gc'd.
     */
    @Test
    void heldReference() {
        if (JavaVersions.VERSION >= 5)
            return;

        System.gc();
        System.gc();
        assertTrue(_coll.contains(_heldValue));
    }

    /**
     * Test that weak references are gc'd.
     */
    @Test
    void weakReference() {
        if (JavaVersions.VERSION >= 5)
            return;

        System.gc();
        System.gc();
        assertFalse(_coll.contains(1));
        assertEquals(1, _coll.size());

        int size = 0;
        for (Iterator itr = _coll.iterator(); itr.hasNext(); size++)
            assertEquals(_heldValue, itr.next());
        assertEquals(1, size);

        // run a mutator method to ensure expired elements are removed
        _coll.add("foo");
        assertEquals(2, _coll.size());
        assertTrue(_coll.contains("foo"));
        assertTrue(_coll.contains(_heldValue));
        assertFalse(_coll.contains(1));
    }

    /**
     * Test that values that have been replaced aren't expired.
     */
    @Test
    void changeValue() {
        if (JavaVersions.VERSION >= 5)
            return;

        Object held = 1;
        assertTrue(_coll.remove(held));
        assertTrue(_coll.add(held));
        System.gc();
        System.gc();

        // run a mutator to clear expired references
        _coll.add("foo");
        assertTrue(_coll.contains(held));
    }
}

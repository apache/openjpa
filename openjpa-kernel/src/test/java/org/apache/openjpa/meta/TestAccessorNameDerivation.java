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
package org.apache.openjpa.meta;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the accessor recognition and property-name derivation rules of
 * {@link AbstractMetaDataDefaults}.
 * <p>
 * OpenJPA lower-cases the first character after the <code>get</code> /
 * <code>is</code> prefix unconditionally, so the JavaBeans-Introspector
 * spelling (<code>getaWord()</code>) and the capitalized spelling
 * (<code>getAWord()</code>) resolve to the same property. Neither getter form
 * may require a particular case for that character. See OPENJPA-2467 and
 * OPENJPA-2993.
 */
public class TestAccessorNameDerivation {

    private static class Fixture {
        public int getaWord() {
            return 0;
        }

        public int getAWord() {
            return 0;
        }

        public int getaCAPITAL() {
            return 0;
        }

        public int getACAPITAL() {
            return 0;
        }

        public boolean isaBoolean() {
            return false;
        }

        public boolean isABoolean() {
            return false;
        }

        public int getA() {
            return 0;
        }

        public int getA1() {
            return 0;
        }

        public String getURL() {
            return null;
        }

        public String isValue() {
            return null;
        }

        public void getaway() {
        }
    }

    private static Method method(String name) {
        try {
            return Fixture.class.getDeclaredMethod(name);
        }
        catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException(name, nsme);
        }
    }

    private static String fieldName(String methodName) {
        return AbstractMetaDataDefaults.getFieldName(method(methodName));
    }

    @Test
    public void testBothGetterSpellingsYieldTheSameName() {
        assertTrue(AbstractMetaDataDefaults.isNormalGetter(method("getaWord")));
        assertTrue(AbstractMetaDataDefaults.isNormalGetter(method("getAWord")));
        assertEquals("aWord", fieldName("getaWord"));
        assertEquals("aWord", fieldName("getAWord"));
        assertEquals("aCAPITAL", fieldName("getaCAPITAL"));
        assertEquals("aCAPITAL", fieldName("getACAPITAL"));
    }

    @Test
    public void testBothBooleanGetterSpellingsYieldTheSameName() {
        // OPENJPA-2993: is<lowercase>() must stay a recognized boolean getter,
        // otherwise the property is silently dropped from the metadata.
        assertTrue(AbstractMetaDataDefaults.isBooleanGetter(method("isaBoolean")));
        assertTrue(AbstractMetaDataDefaults.isBooleanGetter(method("isABoolean")));
        assertTrue(AbstractMetaDataDefaults.isGetter(method("isaBoolean"), false));
        assertEquals("aBoolean", fieldName("isaBoolean"));
        assertEquals("aBoolean", fieldName("isABoolean"));
    }

    @Test
    public void testNonGetters() {
        // non-boolean return type
        assertFalse(AbstractMetaDataDefaults.isBooleanGetter(method("isValue")));
        assertNull(fieldName("isValue"));
        // void return type
        assertFalse(AbstractMetaDataDefaults.isNormalGetter(method("getaway")));
        assertNull(fieldName("getaway"));
    }

    @Test
    public void testShortNames() {
        assertEquals("a", fieldName("getA"));
        assertEquals("a1", fieldName("getA1"));
    }

    @Test
    public void testDivergenceFromIntrospectorDecapitalize() {
        // OPENJPA-2993: OpenJPA deliberately does not implement the JavaBeans
        // rule that leaves a name starting with two upper-case characters
        // alone. Aligning with it would rename existing properties and their
        // default column names, so the divergence is documented, not fixed.
        assertEquals("uRL", fieldName("getURL"));
        assertEquals("URL", java.beans.Introspector.decapitalize("URL"));
    }
}

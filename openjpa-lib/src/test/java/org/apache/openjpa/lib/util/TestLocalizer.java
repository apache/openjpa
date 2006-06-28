/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import junit.framework.*;

import junit.textui.*;

import org.apache.openjpa.lib.util.testlocalizer.*;

import java.util.*;


/**
 *  <p>Tests the Localizer.</p>
 *
 *  @author Abe White
 */
public class TestLocalizer extends TestCase {
    private Localizer _locals = null;

    public TestLocalizer(String test) {
        super(test);
    }

    public void setUp() {
        _locals = Localizer.forPackage(LocalizerTestHelper.class);
    }

    /**
     *  Test getting a string for a class.
     */
    public void testForClass() {
        assertEquals("value1", _locals.get("test.local1"));
    }

    /**
     *  Test getting a string for a non-default locale.
     */
    public void testForLocale() {
        Localizer locl = Localizer.forPackage(LocalizerTestHelper.class,
                Locale.GERMANY);
        assertEquals("value1_de", locl.get("test.local1"));
    }

    /**
     *  Tests that if a locale is missing the system falls back to
     *  the default.
     */
    public void testFallbackLocale() {
        Localizer locl = Localizer.forPackage(LocalizerTestHelper.class,
                Locale.FRANCE);
        assertEquals("value1", locl.get("test.local1"));
    }

    /**
     *  Tests that a null class accesses the localizer.properties file
     *  for the top-level (no package).
     */
    public void testTopLevel() {
        Localizer system = Localizer.forPackage(null);
        assertEquals("systemvalue1", system.get("test.systemlocal"));
    }

    /**
     *  Test that the message formatting works correctly.
     */
    public void testMessageFormat() {
        assertEquals("value2 x sep y",
            _locals.get("test.local2", new String[] { "x", "y" }));

        // test that it treates single objects as single-element arrays
        assertEquals("value2 x sep {1}", _locals.get("test.local2", "x"));
    }

    /**
     *  Test that a {@link MissingResourceException} is thrown for missing
     *  resource bundles.
     */
    public void testMissingBundle() {
        Localizer missing = Localizer.forPackage(String.class);
        assertEquals("foo.bar", missing.get("foo.bar"));

        try {
            missing.getFatal("foo.bar");
            fail("No exception for fatal get on missing bundle.");
        } catch (MissingResourceException mre) {
        }
    }

    /**
     *  Test that a {@link MissingResourceException} is thrown for missing
     *  keys.
     */
    public void testMissingKey() {
        assertEquals("foo.bar", _locals.get("foo.bar"));

        try {
            _locals.getFatal("foo.bar");
            fail("No exception for fatal get on missing key.");
        } catch (MissingResourceException mre) {
        }
    }

    public static Test suite() {
        return new TestSuite(TestLocalizer.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

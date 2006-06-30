/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.util;

import junit.framework.*;
import junit.textui.*;

/**
 * Tests the {@link Strings} type.
 * 
 * @author Abe White
 */
public class TestStrings extends TestCase {
    public TestStrings(String test) {
        super(test);
    }

    /**
     * Test {@link Strings#split}.
     */
    public void testSplit() {
        String str = "boo:and:foo";

        assertEquals(new String[] { "boo", "and:foo" },
            Strings.split(str, ":", 2));
        assertEquals(new String[] { "boo:and:foo" },
            Strings.split(str, ":", 1));
        assertEquals(new String[] { "boo", "and", "foo" },
            Strings.split(str, ":", 0));
        assertEquals(new String[] { "boo", "and", "foo" },
            Strings.split(str, ":", -2));
        assertEquals(new String[] { "b", "", ":and:f", "", "" },
            Strings.split(str, "o", 5));
        assertEquals(new String[] { "b", "", ":and:f", "o" },
            Strings.split(str, "o", 4));
        assertEquals(new String[] { "b", "", ":and:f", "", "" },
            Strings.split(str, "o", -2));
        assertEquals(new String[] { "b", "", ":and:f" },
            Strings.split(str, "o", 0));
        assertEquals(new String[] { "", "b", "", ":and:f" },
            Strings.split("o" + str, "o", 0));
    }

    /**
     * Test {@link Strings#classForName}.
     */
    public void testClassForName() {
        // test primitives
        assertEquals(boolean.class, Strings.toClass("boolean", null));
        assertEquals(byte.class, Strings.toClass("byte", null));
        assertEquals(char.class, Strings.toClass("char", null));
        assertEquals(double.class, Strings.toClass("double", null));
        assertEquals(float.class, Strings.toClass("float", null));
        assertEquals(int.class, Strings.toClass("int", null));
        assertEquals(long.class, Strings.toClass("long", null));
        assertEquals(short.class, Strings.toClass("short", null));
        assertEquals(void.class, Strings.toClass("void", null));

        // test objects
        assertEquals(String.class, Strings.toClass
            (String.class.getName(), null));

        // test arrays
        assertEquals(boolean[].class, Strings.toClass("[Z", null));
        assertEquals(byte[].class, Strings.toClass("[B", null));
        assertEquals(char[].class, Strings.toClass("[C", null));
        assertEquals(double[].class, Strings.toClass("[D", null));
        assertEquals(float[].class, Strings.toClass("[F", null));
        assertEquals(int[].class, Strings.toClass("[I", null));
        assertEquals(long[].class, Strings.toClass("[J", null));
        assertEquals(short[].class, Strings.toClass("[S", null));
        assertEquals(String[].class, Strings.toClass
            (String[].class.getName(), null));
        assertEquals(boolean[][].class, Strings.toClass("[[Z", null));
        assertEquals(String[][].class, Strings.toClass
            (String[][].class.getName(), null));

        assertEquals(boolean[].class, Strings.toClass("boolean[]", null));
        assertEquals(byte[].class, Strings.toClass("byte[]", null));
        assertEquals(char[].class, Strings.toClass("char[]", null));
        assertEquals(double[].class, Strings.toClass("double[]", null));
        assertEquals(float[].class, Strings.toClass("float[]", null));
        assertEquals(int[].class, Strings.toClass("int[]", null));
        assertEquals(long[].class, Strings.toClass("long[]", null));
        assertEquals(short[].class, Strings.toClass("short[]", null));
        assertEquals(String[].class, Strings.toClass("java.lang.String[]",
            null));

        try {
            Strings.toClass("[V", null);
            fail("Allowed invalid class name");
        } catch (RuntimeException re) {
        }
        try {
            Strings.toClass("java.lang.Foo", null);
            fail("Allowed invalid class name");
        } catch (RuntimeException re) {
        }
    }

    private void assertEquals(String[] arr1, String[] arr2) {
        assertEquals(arr1.length, arr2.length);
        for (int i = 0; i < arr1.length; i++)
            assertEquals(arr1[i], arr2[i]);
    }

    public static Test suite() {
        return new TestSuite(TestStrings.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class StringUtilTest {
	
	private static final Logger logger = Logger.getLogger(StringUtilTest.class.getCanonicalName());

    @Test
    void stringSplit() {
        String val = " a b c \n d \n  \n";
        String[] jsplit = val.split(" ");
        String[] split1 = StringUtil.split(val, " ", 0);
        String[] split2 = StringUtil.split(val, " ", 30);

        String[] split3 = StringUtil.split(val, " ", 3);
        assertEquals(3, split3.length);
        assertEquals("", split3[0]);
        assertEquals("a", split3[1]);
        assertEquals("b c \n d \n  \n", split3[2]);
    }

    @Test
    void stringSplitEnding() {
        String val = "a%B%C%";
        String[] jsplit = val.split("%");
        String[] ssplit = StringUtil.split(val, "%", Integer.MAX_VALUE);
        assertEquals(4, ssplit.length);
        assertArrayEquals(new String[]{"a", "B", "C", ""}, ssplit);
    }

    @Test
    void stringSplitFatTokenEnding() {
        String val = "a-.-B-.-C-.-";
        String[] jsplit = val.split("-.-");
        String[] ssplit = StringUtil.split(val, "-.-", Integer.MAX_VALUE);
        assertEquals(4, ssplit.length);
        assertArrayEquals(new String[]{"a", "B", "C", ""}, ssplit);
    }

    @Test
    void trimToNull() {
        assertNull(StringUtil.trimToNull(null));
        assertNull(StringUtil.trimToNull(" "));
        assertNull(StringUtil.trimToNull("   "));
        assertNull(StringUtil.trimToNull("   \n  "));
        assertEquals("A", StringUtil.trimToNull("  A"));
        assertEquals("A", StringUtil.trimToNull("A  "));
        assertEquals("A", StringUtil.trimToNull("  A  "));
        assertEquals("A", StringUtil.trimToNull("  A  \n  "));
    }

    @Test
    void stringCapitalize() {
        assertNull(StringUtil.capitalize(null));
        assertEquals("", StringUtil.capitalize(""));
        assertEquals(" ", StringUtil.capitalize(" "));
        assertEquals("Ahoi", StringUtil.capitalize("ahoi"));
        assertEquals("Ahoi", StringUtil.capitalize("Ahoi"));
        assertEquals(" ahoi", StringUtil.capitalize(" ahoi")); // no trim
        assertEquals("\u00d6hoi", StringUtil.capitalize("\u00f6hoi"));
        assertEquals("\u00dfhoi", StringUtil.capitalize("\u00dfhoi"));
    }

    @Test
    void stringEndsWith() {
        assertFalse(StringUtil.endsWithIgnoreCase(null, "bla"));
        assertFalse(StringUtil.endsWithIgnoreCase("bla", null));
        assertTrue(StringUtil.endsWithIgnoreCase(null, null));
        assertTrue(StringUtil.endsWithIgnoreCase(null, null));
        assertTrue(StringUtil.endsWithIgnoreCase("I have a cAt", "Cat"));
        assertFalse(StringUtil.endsWithIgnoreCase("at", "Cat"));
        assertTrue(StringUtil.endsWithIgnoreCase("at", ""));
    }


    @Test
    void stringParse() {

        try {
            StringUtil.parse(null, null);
            fail("NullPointerException expected");
        }
        catch (NullPointerException npe) {
            // all fine
        }


        // test null representation for primitives
        assertEquals(0, (int) StringUtil.parse(null, byte.class));
        assertEquals(0, (char) StringUtil.parse(null, char.class));
        assertEquals(0, (double) StringUtil.parse(null, double.class), 0);
        assertEquals(0f, (float) StringUtil.parse(null, float.class), 0);
        assertEquals(0, (int) StringUtil.parse(null, int.class));
        assertEquals(0L, (long) StringUtil.parse(null, long.class));
        assertEquals(0, (short) StringUtil.parse(null, short.class));
        assertEquals(false, StringUtil.parse(null, boolean.class));

        // special fun:
        try {
            StringUtil.parse(null, void.class);
            fail("IllegalStateException expected");
        }
        catch (IllegalStateException ise) {
            // all fine
        }

        assertNull(StringUtil.parse(null, Character.class));
        assertNull(StringUtil.parse(null, Double.class));
        assertNull(StringUtil.parse(null, Float.class));
        assertNull(StringUtil.parse(null, Integer.class));
        assertNull(StringUtil.parse(null, Long.class));
        assertNull(StringUtil.parse(null, Short.class));
        assertNull(StringUtil.parse(null, Boolean.class));

        try {
            StringUtil.parse(null, char[].class);
            fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException iae) {
            // all fine
        }

        assertEquals('C', (char) StringUtil.parse("C", char.class));
        assertEquals(35.2345, (double) StringUtil.parse("35.2345", double.class), 0.000001);
        assertEquals(35.2345, (float) StringUtil.parse("35.2345", float.class), 0.000001);
        assertEquals(42, (int) StringUtil.parse("42", int.class));
        assertEquals(42L, (long) StringUtil.parse("42", long.class));
        assertEquals(42, (short) StringUtil.parse("42", short.class));
        assertEquals(true, StringUtil.parse("true", boolean.class));
        assertEquals(true, StringUtil.parse("TRUE", boolean.class));
        assertEquals(false, StringUtil.parse("false", boolean.class));
        assertEquals(false, StringUtil.parse("FALSE", boolean.class));
        assertEquals(false, StringUtil.parse("bla", boolean.class));

        assertEquals('C', (char) StringUtil.parse("C", Character.class));
        assertEquals(35.2345, (double) StringUtil.parse("35.2345", Double.class), 0.000001);
        assertEquals(35.2345, (float) StringUtil.parse("35.2345", Float.class), 0.000001);
        assertEquals(42, (int) StringUtil.parse("42", Integer.class));
        assertEquals(42L, (long) StringUtil.parse("42", Long.class));
        assertEquals(42, (short) StringUtil.parse("42", Short.class));
        assertEquals(true, StringUtil.parse("true", Boolean.class));
        assertEquals(true, StringUtil.parse("TRUE", Boolean.class));
        assertEquals(false, StringUtil.parse("false", Boolean.class));
        assertEquals(false, StringUtil.parse("FALSE", Boolean.class));
        assertEquals(false, StringUtil.parse("bla", Boolean.class));

        try {
            StringUtil.parse(null, StringUtilTest.class);
            fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException iae) {
            // all fine
        }

    }

    @Test
    void stringJoin() {
        assertEquals("AAA,BBB,CCC", StringUtil.join(new String[]{"AAA", "BBB", "CCC"}, ","));
        assertEquals("AAA", StringUtil.join(new String[]{"AAA"}, ","));
        assertEquals("AAAnullBBBnullCCC", StringUtil.join(new String[]{"AAA", "BBB", "CCC"}, null));
        assertEquals("", StringUtil.join(new String[]{}, ","));
        assertNull(StringUtil.join(null, null));
        assertNull(StringUtil.join(null, ","));

    }

    @Test
    @Disabled("only needed for manual performance tests")
    void stringSplitPerformanceTest() {
        String val = "  asdfsfsfsfafasdf  basdfasf cs d efdfdfdfdfdfdfdf ghai asdf " +
                "asdflkj  lökajdf lkölkasdflk jklö adfk \n adslsfl \t adsfsfd";

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            StringUtil.split(val, "sd", 0);
        }

        long stop = System.nanoTime();
        logger.fine("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @Test
    @Disabled("only needed for manual performance tests")
    void stringsReplacePerformance() {
        String val = "This is my fnx test suite for fnx replacement to fnx=fnx";

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X Strings.replace(val, "fnx", "weirdo function");
            //X val.replace("fnx", "weirdo function");
            StringUtil.replace(val, "fnx", "weirdo function");
        }

        long stop = System.nanoTime();
        logger.fine("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @Test
    @Disabled("only needed for manual performance tests")
    void stringJoinPerformance() {
        String[] vals = {"A", "BDS", "DSD", "XYZ", "HOHOHO", "AND", "SOMETHING", "ELSE"};
        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X Strings.join(vals, "-.-");
            StringUtil.join(vals, "-.-");
        }

        long stop = System.nanoTime();
        logger.fine("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }
}

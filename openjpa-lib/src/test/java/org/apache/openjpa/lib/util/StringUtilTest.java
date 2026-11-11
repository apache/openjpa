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

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class StringUtilTest {
	
	private static final Logger logger = Logger.getLogger(StringUtilTest.class.getCanonicalName());

    @Test
    public void testStringSplit() {
        String val = " a b c \n d \n  \n";
        String[] jsplit = val.split(" ");
        String[] split1 = StringUtil.split(val, " ", 0);
        String[] split2 = StringUtil.split(val, " ", 30);

        String[] split3 = StringUtil.split(val, " ", 3);
        Assertions.assertEquals(3, split3.length);
        Assertions.assertEquals("", split3[0]);
        Assertions.assertEquals("a", split3[1]);
        Assertions.assertEquals("b c \n d \n  \n", split3[2]);
    }

    @Test
    public void testStringSplitEnding() {
        String val = "a%B%C%";
        String[] jsplit = val.split("%");
        String[] ssplit = StringUtil.split(val, "%", Integer.MAX_VALUE);
        Assertions.assertEquals(4, ssplit.length);
        Assertions.assertArrayEquals(ssplit, new String[]{"a", "B", "C", ""});
    }

    @Test
    public void testStringSplitFatTokenEnding() {
        String val = "a-.-B-.-C-.-";
        String[] jsplit = val.split("-.-");
        String[] ssplit = StringUtil.split(val, "-.-", Integer.MAX_VALUE);
        Assertions.assertEquals(4, ssplit.length);
        Assertions.assertArrayEquals(ssplit, new String[]{"a", "B", "C", ""});
    }

    @Test
    public void testTrimToNull() {
        Assertions.assertNull(StringUtil.trimToNull(null));
        Assertions.assertNull(StringUtil.trimToNull(" "));
        Assertions.assertNull(StringUtil.trimToNull("   "));
        Assertions.assertNull(StringUtil.trimToNull("   \n  "));
        Assertions.assertEquals("A", StringUtil.trimToNull("  A"));
        Assertions.assertEquals("A", StringUtil.trimToNull("A  "));
        Assertions.assertEquals("A", StringUtil.trimToNull("  A  "));
        Assertions.assertEquals("A", StringUtil.trimToNull("  A  \n  "));
    }

    @Test
    public void testStringCapitalize() {
        Assertions.assertNull(StringUtil.capitalize(null));
        Assertions.assertEquals("", StringUtil.capitalize(""));
        Assertions.assertEquals(" ", StringUtil.capitalize(" "));
        Assertions.assertEquals("Ahoi", StringUtil.capitalize("ahoi"));
        Assertions.assertEquals("Ahoi", StringUtil.capitalize("Ahoi"));
        Assertions.assertEquals(" ahoi", StringUtil.capitalize(" ahoi")); // no trim
        Assertions.assertEquals("\u00d6hoi", StringUtil.capitalize("\u00f6hoi"));
        Assertions.assertEquals("\u00dfhoi", StringUtil.capitalize("\u00dfhoi"));
    }

    @Test
    public void testStringEndsWith() {
        Assertions.assertFalse(StringUtil.endsWithIgnoreCase(null, "bla"));
        Assertions.assertFalse(StringUtil.endsWithIgnoreCase("bla", null));
        Assertions.assertTrue(StringUtil.endsWithIgnoreCase(null, null));
        Assertions.assertTrue(StringUtil.endsWithIgnoreCase(null, null));
        Assertions.assertTrue(StringUtil.endsWithIgnoreCase("I have a cAt", "Cat"));
        Assertions.assertFalse(StringUtil.endsWithIgnoreCase("at", "Cat"));
        Assertions.assertTrue(StringUtil.endsWithIgnoreCase("at", ""));
    }


    @Test
    public void testStringParse() {

        try {
            StringUtil.parse(null, null);
            Assertions.fail("NullPointerException expected");
        }
        catch (NullPointerException npe) {
            // all fine
        }


        // test null representation for primitives
        Assertions.assertEquals(0, (int) StringUtil.parse(null, byte.class));
        Assertions.assertEquals(0, (char) StringUtil.parse(null, char.class));
        Assertions.assertEquals(0, (double) StringUtil.parse(null, double.class), 0);
        Assertions.assertEquals(0f, (float) StringUtil.parse(null, float.class), 0);
        Assertions.assertEquals(0, (int) StringUtil.parse(null, int.class));
        Assertions.assertEquals(0L, (long) StringUtil.parse(null, long.class));
        Assertions.assertEquals(0, (short) StringUtil.parse(null, short.class));
        Assertions.assertEquals(false, StringUtil.parse(null, boolean.class));

        // special fun:
        try {
            StringUtil.parse(null, void.class);
            Assertions.fail("IllegalStateException expected");
        }
        catch (IllegalStateException ise) {
            // all fine
        }

        Assertions.assertNull(StringUtil.parse(null, Character.class));
        Assertions.assertNull(StringUtil.parse(null, Double.class));
        Assertions.assertNull(StringUtil.parse(null, Float.class));
        Assertions.assertNull(StringUtil.parse(null, Integer.class));
        Assertions.assertNull(StringUtil.parse(null, Long.class));
        Assertions.assertNull(StringUtil.parse(null, Short.class));
        Assertions.assertNull(StringUtil.parse(null, Boolean.class));

        try {
            StringUtil.parse(null, char[].class);
            Assertions.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException iae) {
            // all fine
        }

        Assertions.assertEquals('C', (char) StringUtil.parse("C", char.class));
        Assertions.assertEquals(35.2345, (double) StringUtil.parse("35.2345", double.class), 0.000001);
        Assertions.assertEquals(35.2345, (float) StringUtil.parse("35.2345", float.class), 0.000001);
        Assertions.assertEquals(42, (int) StringUtil.parse("42", int.class));
        Assertions.assertEquals(42L, (long) StringUtil.parse("42", long.class));
        Assertions.assertEquals(42, (short) StringUtil.parse("42", short.class));
        Assertions.assertEquals(true, StringUtil.parse("true", boolean.class));
        Assertions.assertEquals(true, StringUtil.parse("TRUE", boolean.class));
        Assertions.assertEquals(false, StringUtil.parse("false", boolean.class));
        Assertions.assertEquals(false, StringUtil.parse("FALSE", boolean.class));
        Assertions.assertEquals(false, StringUtil.parse("bla", boolean.class));

        Assertions.assertEquals('C', (char) StringUtil.parse("C", Character.class));
        Assertions.assertEquals(35.2345, (double) StringUtil.parse("35.2345", Double.class), 0.000001);
        Assertions.assertEquals(35.2345, (float) StringUtil.parse("35.2345", Float.class), 0.000001);
        Assertions.assertEquals(42, (int) StringUtil.parse("42", Integer.class));
        Assertions.assertEquals(42L, (long) StringUtil.parse("42", Long.class));
        Assertions.assertEquals(42, (short) StringUtil.parse("42", Short.class));
        Assertions.assertEquals(true, StringUtil.parse("true", Boolean.class));
        Assertions.assertEquals(true, StringUtil.parse("TRUE", Boolean.class));
        Assertions.assertEquals(false, StringUtil.parse("false", Boolean.class));
        Assertions.assertEquals(false, StringUtil.parse("FALSE", Boolean.class));
        Assertions.assertEquals(false, StringUtil.parse("bla", Boolean.class));

        try {
            StringUtil.parse(null, StringUtilTest.class);
            Assertions.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException iae) {
            // all fine
        }

    }

    @Test
    public void testStringJoin() {
        Assertions.assertEquals("AAA,BBB,CCC", StringUtil.join(new String[]{"AAA", "BBB", "CCC"}, ","));
        Assertions.assertEquals("AAA", StringUtil.join(new String[]{"AAA"}, ","));
        Assertions.assertEquals("AAAnullBBBnullCCC", StringUtil.join(new String[]{"AAA", "BBB", "CCC"}, null));
        Assertions.assertEquals("", StringUtil.join(new String[]{}, ","));
        Assertions.assertNull(StringUtil.join(null, null));
        Assertions.assertNull(StringUtil.join(null, ","));

    }

    @Test
    @Disabled("only needed for manual performance tests")
    public void stringSplitPerformanceTest() {
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
    public void testStringsReplacePerformance() {
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
    public void testStringJoinPerformance() {
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

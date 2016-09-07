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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class StringUtilTest {

    @Test
    public void testStringSplit() {
        String val = " a b c \n d \n  \n";
        String[] jsplit = val.split(" ");
        String[] split1 = StringUtil.split(val, " ", 0);
        String[] split2 = StringUtil.split(val, " ", 30);

        String[] split3 = StringUtil.split(val, " ", 3);
        Assert.assertEquals(3, split3.length);
        Assert.assertEquals("", split3[0]);
        Assert.assertEquals("a", split3[1]);
        Assert.assertEquals("b c \n d \n  \n", split3[2]);
    }

    @Test
    public void testStringSplitEnding() {
        String val = "a%B%C%";
        String[] jsplit = val.split("%");
        String[] ssplit = StringUtil.split(val, "%", Integer.MAX_VALUE);
        Assert.assertEquals(4, ssplit.length);
        Assert.assertArrayEquals(ssplit, new String[]{"a", "B", "C", ""});
    }

    @Test
    public void testStringSplitFatTokenEnding() {
        String val = "a-.-B-.-C-.-";
        String[] jsplit = val.split("-.-");
        String[] ssplit = StringUtil.split(val, "-.-", Integer.MAX_VALUE);
        Assert.assertEquals(4, ssplit.length);
        Assert.assertArrayEquals(ssplit, new String[]{"a", "B", "C", ""});
    }

    @Test
    public void testTrimToNull() {
        Assert.assertNull(StringUtil.trimToNull(null));
        Assert.assertNull(StringUtil.trimToNull(" "));
        Assert.assertNull(StringUtil.trimToNull("   "));
        Assert.assertNull(StringUtil.trimToNull("   \n  "));
        Assert.assertEquals("A", StringUtil.trimToNull("  A"));
        Assert.assertEquals("A", StringUtil.trimToNull("A  "));
        Assert.assertEquals("A", StringUtil.trimToNull("  A  "));
        Assert.assertEquals("A", StringUtil.trimToNull("  A  \n  "));
    }

    @Test
    public void testStringCapitalize() {
        Assert.assertNull(StringUtil.capitalize(null));
        Assert.assertEquals("", StringUtil.capitalize(""));
        Assert.assertEquals(" ", StringUtil.capitalize(" "));
        Assert.assertEquals("Ahoi", StringUtil.capitalize("ahoi"));
        Assert.assertEquals("Ahoi", StringUtil.capitalize("Ahoi"));
        Assert.assertEquals(" ahoi", StringUtil.capitalize(" ahoi")); // no trim
        Assert.assertEquals("\u00d6hoi", StringUtil.capitalize("\u00f6hoi"));
        Assert.assertEquals("\u00dfhoi", StringUtil.capitalize("\u00dfhoi"));
    }

    @Test
    public void testStringEndsWith() {
        Assert.assertFalse(StringUtil.endsWithIgnoreCase(null, "bla"));
        Assert.assertFalse(StringUtil.endsWithIgnoreCase("bla", null));
        Assert.assertTrue(StringUtil.endsWithIgnoreCase(null, null));
        Assert.assertTrue(StringUtil.endsWithIgnoreCase(null, null));
        Assert.assertTrue(StringUtil.endsWithIgnoreCase("I have a cAt", "Cat"));
        Assert.assertFalse(StringUtil.endsWithIgnoreCase("at", "Cat"));
        Assert.assertTrue(StringUtil.endsWithIgnoreCase("at", ""));
    }


    @Test
    public void testStringParse() {

        try {
            StringUtil.parse(null, null);
            Assert.fail("NullPointerException expected");
        }
        catch (NullPointerException npe) {
            // all fine
        }


        // test null representation for primitives
        Assert.assertEquals(0, (int) StringUtil.parse(null, byte.class));
        Assert.assertEquals(0, (char) StringUtil.parse(null, char.class));
        Assert.assertEquals(0, (double) StringUtil.parse(null, double.class), 0);
        Assert.assertEquals(0f, (float) StringUtil.parse(null, float.class), 0);
        Assert.assertEquals(0, (int) StringUtil.parse(null, int.class));
        Assert.assertEquals(0L, (long) StringUtil.parse(null, long.class));
        Assert.assertEquals(0, (short) StringUtil.parse(null, short.class));
        Assert.assertEquals(false, StringUtil.parse(null, boolean.class));

        // special fun:
        try {
            StringUtil.parse(null, void.class);
            Assert.fail("IllegalStateException expected");
        }
        catch (IllegalStateException ise) {
            // all fine
        }

        Assert.assertNull(StringUtil.parse(null, Character.class));
        Assert.assertNull(StringUtil.parse(null, Double.class));
        Assert.assertNull(StringUtil.parse(null, Float.class));
        Assert.assertNull(StringUtil.parse(null, Integer.class));
        Assert.assertNull(StringUtil.parse(null, Long.class));
        Assert.assertNull(StringUtil.parse(null, Short.class));
        Assert.assertNull(StringUtil.parse(null, Boolean.class));

        try {
            StringUtil.parse(null, char[].class);
            Assert.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException iae) {
            // all fine
        }

        Assert.assertEquals('C', (char) StringUtil.parse("C", char.class));
        Assert.assertEquals(35.2345, (double) StringUtil.parse("35.2345", double.class), 0.000001);
        Assert.assertEquals(35.2345, (float) StringUtil.parse("35.2345", float.class), 0.000001);
        Assert.assertEquals(42, (int) StringUtil.parse("42", int.class));
        Assert.assertEquals(42L, (long) StringUtil.parse("42", long.class));
        Assert.assertEquals(42, (short) StringUtil.parse("42", short.class));
        Assert.assertEquals(true, StringUtil.parse("true", boolean.class));
        Assert.assertEquals(true, StringUtil.parse("TRUE", boolean.class));
        Assert.assertEquals(false, StringUtil.parse("false", boolean.class));
        Assert.assertEquals(false, StringUtil.parse("FALSE", boolean.class));
        Assert.assertEquals(false, StringUtil.parse("bla", boolean.class));

        Assert.assertEquals('C', (char) StringUtil.parse("C", Character.class));
        Assert.assertEquals(35.2345, (double) StringUtil.parse("35.2345", Double.class), 0.000001);
        Assert.assertEquals(35.2345, (float) StringUtil.parse("35.2345", Float.class), 0.000001);
        Assert.assertEquals(42, (int) StringUtil.parse("42", Integer.class));
        Assert.assertEquals(42L, (long) StringUtil.parse("42", Long.class));
        Assert.assertEquals(42, (short) StringUtil.parse("42", Short.class));
        Assert.assertEquals(true, StringUtil.parse("true", Boolean.class));
        Assert.assertEquals(true, StringUtil.parse("TRUE", Boolean.class));
        Assert.assertEquals(false, StringUtil.parse("false", Boolean.class));
        Assert.assertEquals(false, StringUtil.parse("FALSE", Boolean.class));
        Assert.assertEquals(false, StringUtil.parse("bla", Boolean.class));

        try {
            StringUtil.parse(null, StringUtilTest.class);
            Assert.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException iae) {
            // all fine
        }

    }

    @Test
    public void testStringJoin() {
        Assert.assertEquals("AAA,BBB,CCC", StringUtil.join(new String[]{"AAA", "BBB", "CCC"}, ","));
        Assert.assertEquals("AAA", StringUtil.join(new String[]{"AAA"}, ","));
        Assert.assertEquals("AAAnullBBBnullCCC", StringUtil.join(new String[]{"AAA", "BBB", "CCC"}, null));
        Assert.assertEquals("", StringUtil.join(new String[]{}, ","));
        Assert.assertNull(StringUtil.join(null, null));
        Assert.assertNull(StringUtil.join(null, ","));

    }

    @Test
    @Ignore("only needed for manual performance tests")
    public void stringSplitPerformanceTest() {
        String val = "  asdfsfsfsfafasdf  basdfasf cs d efdfdfdfdfdfdfdf ghai asdf " +
                "asdflkj  lökajdf lkölkasdflk jklö adfk \n adslsfl \t adsfsfd";

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            StringUtil.split(val, "sd", 0);
            //X val.split("sd");
            //X serp.util.Strings.split(val, "sd", 0);
        }

        long stop = System.nanoTime();
        System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @Test
    @Ignore("only needed for manual performance tests")
    public void testStringsReplacePerformance() {
        String val = "This is my fnx test suite for fnx replacement to fnx=fnx";

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X Strings.replace(val, "fnx", "weirdo function");
            //X val.replace("fnx", "weirdo function");
            StringUtil.replace(val, "fnx", "weirdo function");
        }

        long stop = System.nanoTime();
        System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @Test
    @Ignore("only needed for manual performance tests")
    public void testStringJoinPerformance() {
        String[] vals = {"A", "BDS", "DSD", "XYZ", "HOHOHO", "AND", "SOMETHING", "ELSE"};
        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X Strings.join(vals, "-.-");
            StringUtil.join(vals, "-.-");
        }

        long stop = System.nanoTime();
        System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }
}

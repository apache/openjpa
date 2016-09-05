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
import serp.util.Strings;

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
}

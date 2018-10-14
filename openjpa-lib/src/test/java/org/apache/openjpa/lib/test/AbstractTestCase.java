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
package org.apache.openjpa.lib.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.log.LogFactoryImpl;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.apache.regexp.REUtil;

import org.junit.After;
import static org.junit.Assert.*;

/**
 * TestCase framework to run various tests against solarmetric code.
 * This class contains various utility methods for the following functions:
 * <ul>
 * <li>Using multiple, isolated ClassLoaders</li>
 * <li>Running a test in multiple concurrent threads</li>
 * <li>Assertion helpers</li>
 * <li>Creating random Strings, numbers, etc.</li>
 * </ul>
 *
 * @author Marc Prud'hommeaux
 * @author Patrick Linskey
 */
public abstract class AbstractTestCase {

    private static final Localizer _loc =
        Localizer.forPackage(AbstractTestCase.class);

    private Log log = null;


    protected final Log getLog() {
        if (log == null)
            log = newLog();
        return log;
    }

    protected Log newLog() {
        // this implementation leaves much to be desired, as it just
        // creates a new LogFactoryImpl each time, and does not apply
        // any configurations.
        return new LogFactoryImpl().getLog(this.getClass().getName());
    }




    @After
    public void tearDown() throws Exception {
        if ("true".equals(System.getProperty("meminfo")))
            printMemoryInfo();

    }

    //////////////////////////
    // Generating random data
    //////////////////////////

    /**
     * Support method to get a random Integer for testing.
     */
    public static Integer randomInt() {
        return new Integer((int) (Math.random() * Integer.MAX_VALUE));
    }

    /**
     * Support method to get a random Character for testing.
     */
    public static Character randomChar() {
        char [] TEST_CHAR_ARRAY = new char []{
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1',
            '2', '3', '4', '5', '6', '7', '8', '9' };

        return new Character(TEST_CHAR_ARRAY[
            (int) (Math.random() * TEST_CHAR_ARRAY.length)]);
    }

    /**
     * Support method to get a random Long for testing.
     */
    public static Long randomLong() {
        return new Long((long) (Math.random() * Long.MAX_VALUE));
    }


    /**
     * Support method to get a random String for testing.
     */
    public static String randomString() {
        // default to a small string, in case column sizes are
        // limited(such as with a string primary key)
        return randomString(50);
    }

    /**
     * Support method to get a random String for testing.
     */
    public static String randomString(int len) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < (int) (Math.random() * len) + 1; i++)
            buf.append(randomChar());
        return buf.toString();
    }


    ///////////////
    // Collections
    ///////////////

    ////////////////////
    // Assertion Helpers
    ////////////////////

    public void assertNotEquals(Object a, Object b) {
        if (a == null && b != null)
            return;
        if (a != null && b == null)
            return;
        if (!(a.equals(b)))
            return;
        if (!(b.equals(a)))
            return;

        fail("expected !<" + a + ">.equals(<" + b + ">)");
    }

    public void assertSize(int size, Object ob) {
        if (ob == null) {
            assertEquals(size, 0);
            return;
        }

        if (ob instanceof Collection)
            ob = ((Collection) ob).iterator();
        if (ob instanceof Iterator) {
            Iterator i = (Iterator) ob;
            int count = 0;
            while (i.hasNext()) {
                count++;
                i.next();
            }

            assertEquals(size, count);
        } else
            fail("assertSize: expected Collection, Iterator, "
                + "Query, or Extent, but got " + ob.getClass().getName());
    }

    /////////////////////
    // Generic utilities
    /////////////////////

    public void copy(File from, File to) throws IOException {
        copy(new FileInputStream(from), to);
    }

    public void copy(InputStream in, File to) throws IOException {
        FileOutputStream fout = new FileOutputStream(to);

        byte[] b = new byte[1024];

        for (int n = 0; (n = in.read(b)) != -1;)
            fout.write(b, 0, n);
    }

    /**
     * Print out information on memory usage.
     */
    public void printMemoryInfo() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        NumberFormat nf = NumberFormat.getInstance();
        getLog().warn(_loc.get("mem-info",
            nf.format(used),
            nf.format(total),
            nf.format(free)));
    }

    /**
     * Serialize and deserialize the object.
     *
     * @param validateEquality make sure the hashCode and equals
     * methods hold true
     */
    public static Object roundtrip(Object orig, boolean validateEquality)
        throws IOException, ClassNotFoundException {
        assertNotNull(orig);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(orig);
        ByteArrayInputStream bin = new ByteArrayInputStream(
            bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        Object result = in.readObject();

        if (validateEquality) {
            assertEquals(orig, result);
            assertEquals(orig.hashCode(), result.hashCode());
        }

        return result;
    }

    /**
     * @return true if the specified input matches the regular expression regex.
     */
    public static boolean matches(String regex, String input)
        throws RESyntaxException {
        RE re = REUtil.createRE(regex);
        return re.match(input);
    }


    /**
     * Check the list if strings and return the ones that match
     * the specified match.
     */
    public static List matches(String regex, Collection input)
        throws RESyntaxException {
        List matches = new ArrayList();
        for (Iterator i = input.iterator(); i.hasNext();) {
            String check = (String) i.next();
            if (matches(regex, check))
                matches.add(check);
        }

        return matches;
    }

}

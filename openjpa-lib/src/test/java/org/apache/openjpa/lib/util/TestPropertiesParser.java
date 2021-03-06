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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.openjpa.lib.util.FormatPreservingProperties.
        DuplicateKeyException;

import org.junit.Test;

import static org.junit.Assert.*;

// things to test:
// - delimiters in keys
// - escape chars, including \:, \= in files(as generated by Properties)
// - unicode
// - non-String keys / vals
// - list() method behavior

public class TestPropertiesParser {

    private static final String LS = System.getProperty( "line.separator" );

    @Test
    public void testSimpleProperties() throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append("key: value" + LS);
        buf.append("key2: value2"); // no EOL -- this is intentional
        Properties p = toProperties(buf.toString());
        assertProperties(new String[][]{
            { "key", "value" }, { "key2", "value2" } }, p);
    }

    @Test
    public void testComments() throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append("# this is a comment" + LS);
        buf.append(" # another one, with leading whitespace	" + LS);
        buf.append(" 	# 	and more with interesting whitespace	" + LS);
        buf.append("! and with a ! delimiter" + LS);
        buf.append("! and with escape \t chars" + LS);
        buf.append("#and a comment with no whitespace" + LS);
        Properties p = toProperties(buf.toString());
        assertEquals(0, p.size());
    }

    @Test
    public void testMixedContent() throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append("# this is a comment" + LS);
        buf.append(" # another one, with leading whitespace	" + LS);
        buf.append("foo: bar#baz" + LS);
        buf.append("! and with a ! delimiter" + LS);
        buf.append("! and with escape \t chars" + LS);
        Properties p = toProperties(buf.toString());
        assertProperties(new String[][]{ { "foo", "bar#baz" } }, p);
    }

    @Test
    public void testMultiLineInput() throws IOException {
        String s = "foo: bar\\" + LS + "more line goes here";
        Properties p = toProperties(s);
        assertProperties(
            new String[][]{ { "foo", "barmore line goes here" } }, p);
    }

    @Test
    public void testEmptyLines() throws IOException {
        Properties p = toProperties(LS + "foo: bar" + LS + LS + "baz: val");
        assertProperties(new String[][]{ { "foo", "bar" }, { "baz", "val" } },
            p);
    }

    @Test
    public void testAddProperties() throws IOException {
        // intentionally left out the trailing end line
        String s = "foo: bar" + LS + "baz: val";
        Properties p = toProperties(s);
        assertProperties(new String[][]{ { "foo", "bar" }, { "baz", "val" } },
            p);

        p.put("new-key", "val1");
        p.put("new-key-2", "val2");
        p.put("another-new-key", "val3");
        assertRoundTrip(s + LS + "new-key: val1" + LS + "new-key-2: val2" + LS +
            "another-new-key: val3" + LS, p);
    }

    @Test
    public void testAddAndMutateProperties() throws IOException {
        // intentionally left out the trailing end line
        Properties p = toProperties("foo: bar" + LS + "baz: val");
        assertProperties(new String[][]{ { "foo", "bar" }, { "baz", "val" } },
            p);

        p.put("new-key", "new value");
        p.put("foo", "barbar");
        assertRoundTrip("foo: barbar" + LS + "baz: val" + LS
            + "new-key: new value" + LS, p);
    }

    @Test
    public void testEscapedEquals() throws IOException {
        Properties p = toProperties("foo=bar\\=WARN,baz\\=TRACE");
        assertProperties(new String[][]{ { "foo", "bar=WARN,baz=TRACE" } }, p);
    }

    @Test
    public void testLineTypes() throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append("   !comment" + LS + " \t  " + LS + "name = no" + LS + "    "
            + "#morec\tomm\\" + LS + "ents" + LS + LS + "  dog=no\\cat   " + LS
            + "burps    :" + LS + "test=" + LS + "date today" + LS + LS + LS
            + "long\\" + LS + "   value=tryin \\" + LS + " "
            + "gto" + LS + "4:vier" + LS + "vier     :4");
        Properties p = toProperties(buf.toString());
        assertProperties(new String[][]{
            { "name", "no" }, { "ents", "" }, { "dog", "nocat   " },
            { "burps", "" }, { "test", "" }, { "date", "today" },
            { "longvalue", "tryin gto" }, { "4", "vier" }, { "vier", "4" },
        }, p);
    }

    @Test
    public void testSpecialChars() throws Throwable {
        testSpecialChars(false, true);
        testSpecialChars(true, true);
        testSpecialChars(false, false);
        testSpecialChars(true, false);
    }

    /**
     * Test that special characters work.
     *
     * @param formattingProps if true, test against the
     * FormatPreservingProperties, otherwise test
     * against a normal Properties instance(for validation of the test case).
     * @param value whether to test the key or the value
     */
    private void testSpecialChars(boolean formattingProps, boolean value)
        throws Throwable {
        List valueList = new ArrayList(Arrays.asList(new String[]{
            "xxyy", "xx\\yy", "xx" + LS + "yy", "xx\\nyy", "xx\tyy", "xx\\tyy",
            "xx\ryy", "xx\\ryy", "xx\fyy", "xx\\fyy", "xx\r" + LS + "\\\t\r\t"
            + LS + "yy",
            "xx\\r" + LS + "\\\t\\r\t\\nyy",
            "xx\r" + LS + "\\\\\\\\\\\\\\\\\\\\\\\\\\\t\r\t" + LS + "yy",
            "C:\\Program Files\\Some Application\\OpenJPA\\My File.dat", }));

        // also store every individual character
        for (char c = 'a'; c < 'Z'; c++) {
            valueList.add(new String(new char[]{ c }));
            valueList.add(new String(new char[]{ c, '\\', c }));
            valueList.add(new String(new char[]{ '\\', c }));
        }

        String[] values = (String[]) valueList.toArray(new String[0]);

        final String dummy = "XXX";

        for (String s : values) {
            // test special characters in either keys or values
            String val = value ? s : dummy;
            String key = value ? dummy : s;

            Properties p = formattingProps ?
                    new FormatPreservingProperties() : new Properties();
            if (p instanceof FormatPreservingProperties) {
                // set these properties so we behave the same way as
                // java.util.Properties
                ((FormatPreservingProperties) p).setDefaultEntryDelimiter('=');
                ((FormatPreservingProperties) p).
                        setAddWhitespaceAfterDelimiter(false);
            }

            p.setProperty(key, val);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            p.store(out, null);

            Properties copy = new Properties();
            copy.setProperty(key, val);
            ByteArrayOutputStream copyOut = new ByteArrayOutputStream();
            copy.store(copyOut, null);

            p = formattingProps ?
                    new FormatPreservingProperties() : new Properties();

            InputStream in = new BufferedInputStream
                    (new ByteArrayInputStream(out.toByteArray()));

            try {
                // make sure that the 2 properties serialized are the same
                String copyOutString = stripComments(copyOut.toByteArray());
                String outString = stripComments(out.toByteArray());
                assertEquals(copyOutString, outString);

                p.load(in);

                assertNotNull("Property \"" + key + "\" was null",
                        p.getProperty(key));
                assertEquals(val.trim(), p.getProperty(key).trim());
            }
            catch (Throwable ioe) {
                if (!formattingProps)
                    throw ioe;

                // bug(1211, ioe,
                // "Cannot store backslash in FormatPreservingProperties");
                throw ioe;
            }
        }
    }

    static Character randomChar() {
        char [] TEST_CHAR_ARRAY = new char []{
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1',
            '2', '3', '4', '5', '6', '7', '8', '9' };

        return TEST_CHAR_ARRAY[
                (int) (Math.random() * TEST_CHAR_ARRAY.length)];
    }

    static String randomString(int len) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < (int) (Math.random() * len) + 1; i++)
            buf.append(randomChar());
        return buf.toString();
    }

    @Test
    public void testEquivalentStore() throws IOException {
        Properties p1 = new Properties();
        FormatPreservingProperties p2 = new FormatPreservingProperties();

        ((FormatPreservingProperties) p2).setDefaultEntryDelimiter('=');
        ((FormatPreservingProperties) p2).setAddWhitespaceAfterDelimiter(false);

        String[] values =
            new String[] {
                "x",
                "x" + LS + "y",
                "x\\ny",
                "x\ty",
                "x\\ty",
                "x\fy",
                "x\\fy",
                "x\ry",
                "x\\ry",
                "C:\\Foo Bar\\Baz",
                randomString(5).replace('a', '\\'),
                randomString(500).replace('a', '\\'),
                randomString(5000).replace('a', '\\'),
                };

        for (String value : values) {
            p1.clear();
            p2.clear();

            p1.setProperty("xxx", value);
            p2.setProperty("xxx", value);

            ByteArrayOutputStream out1 = new ByteArrayOutputStream();
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();

            p1.store(out1, null);
            p2.store(out2, null);

            String s1 = new String(out1.toByteArray());
            String s2 = new String(out2.toByteArray());

            assertTrue("Expected <" + s1 + "> but was <" + s2 + ">",
                    s1.indexOf(s2) != -1);
        }
    }

    static String stripComments(byte[] bytes) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader
            (new ByteArrayInputStream(bytes)));
        StringBuilder sbuf = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            // skip comments
            if (line.trim().startsWith("#"))
                continue;

            sbuf.append(line);
            sbuf.append(LS);
        }

        return sbuf.toString();
    }

    @Test
    public void testDuplicateProperties() throws IOException {
        FormatPreservingProperties p = new FormatPreservingProperties();
        try {
            toProperties("foo=bar" + LS + "foo=baz", p);
            fail("expected duplicate keys to cause exception");
        } catch (DuplicateKeyException e) {
            // expected
        }

        // now test the expected behavior when duplicates are allowed.
        p = new FormatPreservingProperties();
        p.setAllowDuplicates(true);
        toProperties("foo=bar" + LS + "foo=baz", p);
        assertProperties(new String[][]{ { "foo", "baz" } }, p);
    }

    @Test
    public void testMultipleLoads() throws IOException {
        String props = "foo=bar" + LS + "baz=quux";
        String props2 = "a=b" + LS + "c=d";
        Properties vanilla = new Properties();
        vanilla.load(new BufferedInputStream
            (new StringBufferInputStream(props)));
        vanilla.load(new BufferedInputStream
            (new StringBufferInputStream(props2)));

        Properties p = new FormatPreservingProperties();
        p.load(new BufferedInputStream(new StringBufferInputStream(props)));
        p.load(new BufferedInputStream(new StringBufferInputStream(props2)));
        assertPropertiesSame(vanilla, p);
    }

    protected FormatPreservingProperties toProperties(String s)
        throws IOException {
        return toProperties(s, new FormatPreservingProperties());
    }

    protected FormatPreservingProperties toProperties(String s,
        FormatPreservingProperties p) throws IOException {
        Properties vanilla = new Properties();
        vanilla.load(new StringBufferInputStream(s));

        p.load(new StringBufferInputStream(s));
        assertRoundTrip(s, p);

        assertPropertiesSame(vanilla, p);

        return p;
    }

    private void assertRoundTrip(String s, Properties p) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store(out, null);
        assertEquals(s, out.toString());

        // also check serializable
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new ObjectOutputStream(bout).writeObject(p);

        try {
            FormatPreservingProperties deserialized =
                (FormatPreservingProperties) new ObjectInputStream
                    (new ByteArrayInputStream(bout.toByteArray())).
                    readObject();
            assertEquals(p, deserialized);

            out = new ByteArrayOutputStream();
            deserialized.store(out, null);
            assertEquals(s, out.toString());
        } catch (ClassNotFoundException cnfe) {
            fail(cnfe + "");
        }
    }


    private void assertPropertiesSame(Properties vanilla, Properties p) {
        assertEquals(vanilla, p);
    }

    protected void assertProperties(String[][] strings, Properties p) {
        for (String[] string : strings) assertEquals(string[1], p.get(string[0]));

        assertEquals(strings.length, p.size());
    }
}

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
package org.apache.openjpa.lib.conf;

import org.apache.openjpa.lib.test.*;
import org.apache.openjpa.lib.util.*;
import serp.util.*;

/**
 * Tests the {@link Configurations} class.
 * 
 * @author Abe White
 */
public class TestConfigurations extends AbstractTestCase {
    public TestConfigurations(String test) {
        super(test);
    }

    public void testParsePlugin() {
        String str = null;
        assertNull(Configurations.getClassName(str));
        assertNull(Configurations.getProperties(str));
        str = "foo";
        assertEquals("foo", Configurations.getClassName(str));
        assertNull(Configurations.getProperties(str));
        str = "a=b";
        assertNull(Configurations.getClassName(str));
        assertEquals("a=b", Configurations.getProperties(str));
        str = "a=b, c=d";
        assertNull(Configurations.getClassName(str));
        assertEquals("a=b, c=d", Configurations.getProperties(str));
        str = "foo(a=b, c=d)";
        assertEquals("foo", Configurations.getClassName(str));
        assertEquals("a=b, c=d", Configurations.getProperties(str));
        str = " foo( a=\"b,c d\", c=\"d\" ) ";
        assertEquals("foo", Configurations.getClassName(str));
        assertEquals("a=\"b,c d\", c=\"d\"", Configurations.getProperties(str));
        str = " foo( a='b,c d', c='d' ) ";
        assertEquals("foo", Configurations.getClassName(str));
        assertEquals("a='b,c d', c='d'", Configurations.getProperties(str));
    }

    public void testParseProperties() {
        Options opts = Configurations.parseProperties(null);
        assertEquals(0, opts.size());

        opts = Configurations.parseProperties(" foo=bar, biz=baz ");
        assertEquals(2, opts.size());
        assertEquals("bar", opts.getProperty("foo"));
        assertEquals("baz", opts.getProperty("biz"));

        opts = Configurations.parseProperties("foo=bar,biz=\"baz,=,baz\",x=y");
        assertEquals(3, opts.size());
        assertEquals("bar", opts.getProperty("foo"));
        assertEquals("baz,=,baz", opts.getProperty("biz"));
        assertEquals("y", opts.getProperty("x"));

        opts = Configurations.parseProperties
            ("foo=\"bar bar,10\",biz=\"baz baz\"");
        assertEquals(2, opts.size());
        assertEquals("bar bar,10", opts.getProperty("foo"));
        assertEquals("baz baz", opts.getProperty("biz"));
        opts = Configurations.parseProperties
            ("foo='bar bar,10',biz='baz baz'");
        assertEquals(2, opts.size());
        assertEquals("bar bar,10", opts.getProperty("foo"));
        assertEquals("baz baz", opts.getProperty("biz"));
    }

    public static void main(String[] args) {
        main(TestConfigurations.class);
    }
}

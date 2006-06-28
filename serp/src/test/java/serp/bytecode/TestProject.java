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
package serp.bytecode;

import junit.framework.*;

import junit.textui.*;

import java.io.*;


/**
 *  <p>Tests the {@link Project} type.</p>
 *
 *  @author Abe White
 */
public class TestProject extends TestCase {
    private Project _project = new Project();

    public TestProject(String test) {
        super(test);
    }

    /**
     *  Test the project name.
     */
    public void testName() {
        assertNull(_project.getName());
        assertNull(new Project(null).getName());
        assertEquals("foo", new Project("foo").getName());
    }

    /**
     *  Test loading classes by name.
     */
    public void testLoadByName() {
        BCClass bc;
        BCClass bc2;
        String[] names;
        String[] names2;

        // test primitive types
        names = new String[] {
                "boolean", "byte", "char", "double", "float", "int", "long",
                "short", "void"
            };
        names2 = new String[] { "Z", "B", "C", "D", "F", "I", "J", "S", "V" };

        for (int i = 0; i < names.length; i++) {
            bc = _project.loadClass(names[i]);
            bc2 = _project.loadClass(names2[i]);
            assertTrue(names[i], bc == bc2);
            assertTrue(names[i], bc.isPrimitive());
            assertEquals(names[i], bc.getName());
        }

        // test primitive array types
        names = new String[] {
                "boolean[]", "byte[]", "char[]", "double[]", "float[]", "int[]",
                "long[]", "short[]"
            };
        names2 = new String[] { "[Z", "[B", "[C", "[D", "[F", "[I", "[J", "[S" };

        for (int i = 0; i < names.length; i++) {
            bc = _project.loadClass(names[i]);
            bc2 = _project.loadClass(names2[i]);
            assertTrue(names[i], bc == bc2);
            assertTrue(names[i], bc.isArray());
            assertEquals(names2[i], bc.getName());
        }

        // test new object type
        bc = _project.loadClass("serp.Foo");
        bc2 = _project.loadClass("serp.Foo");
        assertTrue(bc == bc2);
        assertTrue(!bc.isPrimitive());
        assertTrue(!bc.isArray());
        assertEquals("serp.Foo", bc.getName());

        // test new object array type
        bc = _project.loadClass("serp.Foo[]");
        bc2 = _project.loadClass("[Lserp.Foo;");
        assertTrue(bc == bc2);
        assertTrue(bc.isArray());
        assertEquals("[Lserp.Foo;", bc.getName());

        // test existing object type
        bc = _project.loadClass(String.class.getName());
        bc2 = _project.loadClass(String.class);
        assertTrue(bc == bc2);
        assertTrue(!bc.isPrimitive());
        assertTrue(!bc.isArray());
        assertEquals(String.class.getName(), bc.getName());
        assertEquals("length", bc.getDeclaredMethod("length").getName());

        // test new object array type
        bc = _project.loadClass(String.class.getName() + "[]");
        bc2 = _project.loadClass(String[].class);
        assertTrue(bc == bc2);
        assertTrue(bc.isArray());
        assertEquals(String[].class.getName(), bc.getName());
    }

    /**
     *  Test loading classes by type.
     */
    public void testLoadByType() {
        BCClass bc;
        BCClass bc2;
        Class[] types;
        String[] names;

        // test primitive types
        types = new Class[] {
                boolean.class, byte.class, char.class, double.class, float.class,
                int.class, long.class, short.class, void.class
            };
        names = new String[] { "Z", "B", "C", "D", "F", "I", "J", "S", "V" };

        for (int i = 0; i < names.length; i++) {
            bc = _project.loadClass(types[i]);
            bc2 = _project.loadClass(names[i]);
            assertTrue(types[i].getName(), bc == bc2);
            assertTrue(types[i].getName(), bc.isPrimitive());
            assertEquals(types[i].getName(), bc.getName());
        }

        // test primitive array types
        types = new Class[] {
                boolean[].class, byte[].class, char[].class, double[].class,
                float[].class, int[].class, long[].class, short[].class,
            };
        names = new String[] { "[Z", "[B", "[C", "[D", "[F", "[I", "[J", "[S" };

        for (int i = 0; i < names.length; i++) {
            bc = _project.loadClass(types[i]);
            bc2 = _project.loadClass(names[i]);
            assertTrue(types[i].getName(), bc == bc2);
            assertTrue(types[i].getName(), bc.isArray());
            assertEquals(types[i].getName(), bc.getName());
        }

        // test existing object type
        bc = _project.loadClass(String.class);
        bc2 = _project.loadClass(String.class.getName());
        assertTrue(bc == bc2);
        assertTrue(!bc.isPrimitive());
        assertTrue(!bc.isArray());
        assertEquals(String.class.getName(), bc.getName());
        assertEquals("length", bc.getDeclaredMethod("length").getName());

        // test new object array type
        bc = _project.loadClass(String[].class);
        bc2 = _project.loadClass(String.class.getName() + "[]");
        assertTrue(bc == bc2);
        assertTrue(bc.isArray());
        assertEquals(String[].class.getName(), bc.getName());
    }

    /**
     *  Test loading classes by file.
     */
    public void testLoadByFile() {
        File file = new File(getClass().getResource("TestProject.class")
                                 .getFile());

        BCClass bc = _project.loadClass(file);
        BCClass bc2 = _project.loadClass(file);
        assertTrue(bc == bc2);
        assertTrue(!bc.isPrimitive());
        assertTrue(!bc.isArray());
        assertEquals(getClass().getName(), bc.getName());
        assertEquals("main", bc.getDeclaredMethod("main").getName());
    }

    /**
     *  Test loading classes by stream.
     */
    public void testLoadByStream() {
        InputStream in = getClass().getResourceAsStream("TestProject.class");
        InputStream in2 = getClass().getResourceAsStream("TestProject.class");

        BCClass bc = _project.loadClass(in);
        BCClass bc2 = _project.loadClass(in2);
        assertTrue(bc == bc2);
        assertTrue(!bc.isPrimitive());
        assertTrue(!bc.isArray());
        assertEquals(getClass().getName(), bc.getName());
        assertEquals("main", bc.getDeclaredMethod("main").getName());
    }

    /**
     *  Test retrieving all loaded classes.
     */
    public void testGetClasses() {
        BCClass[] bcs = _project.getClasses();
        assertEquals(0, bcs.length);

        BCClass[] added = new BCClass[3];
        added[0] = _project.loadClass("int");
        added[1] = _project.loadClass("serp.Foo");
        added[2] = _project.loadClass(String[].class);

        bcs = _project.getClasses();
        assertEquals(3, bcs.length);

        int matches;

        for (int i = 0; i < added.length; i++) {
            matches = 0;

            for (int j = 0; j < bcs.length; j++)
                if (added[i] == bcs[j]) {
                    matches++;
                }

            assertEquals(1, matches);
        }
    }

    /**
     *  Test renaming classes within the project.
     */
    public void testRename() {
        BCClass str = _project.loadClass(String.class);
        BCClass foo = _project.loadClass("serp.Foo");

        str.setName("java.lang.String2");
        assertEquals("java.lang.String2", str.getName());
        foo.setName("serp.Foo2");
        assertEquals("serp.Foo2", foo.getName());

        try {
            str.setName("serp.Foo2");
            fail("Set to existing name");
        } catch (IllegalStateException ise) {
        }

        assertEquals("java.lang.String2", str.getName());

        try {
            foo.setName("java.lang.String2");
            fail("Set to existing name");
        } catch (IllegalStateException ise) {
        }

        assertEquals("serp.Foo2", foo.getName());

        str.setName("serp.Foo");
        assertEquals("serp.Foo", str.getName());

        foo.setName("java.lang.String");
        assertEquals("java.lang.String", foo.getName());

        assertTrue(foo == _project.loadClass(String.class));
        assertTrue(str == _project.loadClass("serp.Foo"));
    }

    /**
     *  Test clearing classes.
     */
    public void testClear() {
        _project.clear();

        BCClass bc1 = _project.loadClass("int");
        BCClass bc2 = _project.loadClass("serp.Foo");
        BCClass bc3 = _project.loadClass(String[].class);

        assertTrue(bc1.isValid());
        assertTrue(bc2.isValid());
        assertTrue(bc3.isValid());

        assertEquals(3, _project.getClasses().length);
        _project.clear();
        assertEquals(0, _project.getClasses().length);

        // cleared classes should be invalid
        assertTrue(!bc1.isValid());
        assertTrue(!bc2.isValid());
        assertTrue(!bc3.isValid());
    }

    /**
     *  Test removing a class.
     */
    public void testRemove() {
        assertTrue(!_project.removeClass((String) null));
        assertTrue(!_project.removeClass((Class) null));
        assertTrue(!_project.removeClass((BCClass) null));

        BCClass bc1 = _project.loadClass("int");
        BCClass bc2 = _project.loadClass("serp.Foo");
        BCClass bc3 = _project.loadClass(String[].class);

        assertTrue(bc1.isValid());
        assertTrue(bc2.isValid());
        assertTrue(bc3.isValid());

        assertTrue(!_project.removeClass(new Project().loadClass("int")));
        assertTrue(_project.removeClass(bc1));
        assertTrue(!bc1.isValid());
        assertEquals(2, _project.getClasses().length);

        assertTrue(_project.removeClass("serp.Foo"));
        assertTrue(!bc1.isValid());
        assertEquals(1, _project.getClasses().length);

        assertTrue(_project.removeClass(String[].class));
        assertTrue(!bc1.isValid());
        assertEquals(0, _project.getClasses().length);
    }

    public static Test suite() {
        return new TestSuite(TestProject.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

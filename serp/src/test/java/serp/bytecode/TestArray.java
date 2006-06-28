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


/**
 *  <p>Tests the handling of array {@link BCClass}es.</p>
 *
 *  @author Abe White
 */
public class TestArray extends AbstractStateTest {
    private BCClass _bc2 = null;

    public TestArray(String test) {
        super(test);
    }

    public void setUp() {
        _bc = _project.loadClass(String[].class);
        _bc2 = _project.loadClass(int[][].class);
    }

    public void testType() {
        assertEquals(String[].class.getName(), _bc.getName());
        assertEquals("java.lang", _bc.getPackageName());
        assertEquals("String[]", _bc.getClassName());
        assertEquals(String[].class, _bc.getType());

        try {
            _bc.setName("Foo[]");
            fail("Allowed set name");
        } catch (UnsupportedOperationException uoe) {
        }

        assertTrue(!_bc.isPrimitive());
        assertTrue(_bc.isArray());

        assertEquals(int[][].class.getName(), _bc2.getName());
        assertNull(_bc2.getPackageName());
        assertEquals("int[][]", _bc2.getClassName());
        assertEquals(int[][].class, _bc2.getType());
    }

    public void testSuperclass() {
        assertEquals(Object.class.getName(), _bc.getSuperclassName());

        try {
            _bc.setSuperclass("Foo");
            fail("Allowed set superclass");
        } catch (UnsupportedOperationException uoe) {
        }
    }

    public void testComponent() {
        assertEquals(String.class.getName(), _bc.getComponentName());
        assertEquals(String.class, _bc.getComponentType());
        assertEquals(String.class, _bc.getComponentBC().getType());
        assertEquals(int[].class.getName(), _bc2.getComponentName());
        assertEquals(int[].class, _bc2.getComponentType());
        assertEquals(int[].class, _bc2.getComponentBC().getType());
    }

    public static Test suite() {
        return new TestSuite(TestArray.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

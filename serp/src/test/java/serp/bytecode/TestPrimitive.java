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
package serp.bytecode;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the handling of primitive {@link BCClass}es.
 *
 * @author Abe White
 */
public class TestPrimitive extends AbstractStateTest {

    public TestPrimitive(String test) {
        super(test);
    }

    public void setUp() {
        _bc = _project.loadClass(int.class);
    }

    public void testType() {
        assertEquals("int", _bc.getName());
        assertNull(_bc.getPackageName());
        assertEquals("int", _bc.getClassName());
        assertEquals(int.class, _bc.getType());
        try {
            _bc.setName("long");
            fail("Allowed set name");
        } catch (UnsupportedOperationException uoe) {
        }

        assertTrue(_bc.isPrimitive());
        assertTrue(!_bc.isArray());
    }

    public void testSuperclass() {
        assertNull(_bc.getSuperclassName());
        try {
            _bc.setSuperclass("long");
            fail("Allowed set superclass");
        } catch (UnsupportedOperationException uoe) {
        }
    }

    public void testComponent() {
        assertNull(_bc.getComponentName());
    }

    public static Test suite() {
        return new TestSuite(TestPrimitive.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

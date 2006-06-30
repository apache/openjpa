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

import junit.framework.*;
import junit.textui.*;

/**
 * Base class for testing the handling of the {@link PrimitiveState} and
 * {@link ArrayState}. Subclasses should set the {@link #_bc} member in
 * their {@link TestCase#setUp} method.
 * 
 * @author Abe White
 */
public abstract class AbstractStateTest extends TestCase {
    protected Project _project = new Project();
    protected BCClass _bc = null;

    public AbstractStateTest(String test) {
        super(test);
    }

    /**
     * Test the name and type operations.
     */
    public abstract void testType();

    /**
     * Test operations on the superclass.
     */
    public abstract void testSuperclass();

    /**
     * Test operations on the component type.
     */
    public abstract void testComponent();

    /**
     * Test the basics -- magic number, etc.
     */
    public void testBasics() {
        assertEquals(Constants.VALID_MAGIC, _bc.getMagic());
        try {
            _bc.setMagic(1);
            fail("Allowed set magic");
        } catch (UnsupportedOperationException uoe) {
        }

        assertEquals(Constants.MAJOR_VERSION, _bc.getMajorVersion());
        try {
            _bc.setMajorVersion(1);
            fail("Allowed set major version");
        } catch (UnsupportedOperationException uoe) {
        }

        assertEquals(Constants.MINOR_VERSION, _bc.getMinorVersion());
        try {
            _bc.setMinorVersion(1);
            fail("Allowed set minor version");
        } catch (UnsupportedOperationException uoe) {
        }

        assertEquals(Constants.ACCESS_PUBLIC | Constants.ACCESS_FINAL,
            _bc.getAccessFlags());
        try {
            _bc.setAccessFlags(1);
            fail("Allowed set access flags");
        } catch (UnsupportedOperationException uoe) {
        }

        try {
            _bc.getPool();
            fail("Allowed access constant pool");
        } catch (UnsupportedOperationException uoe) {
        }
    }

    /**
     * Test operations on interfaces.
     */
    public void testInterfaces() {
        assertEquals(0, _bc.getDeclaredInterfaceNames().length);
        assertEquals(0, _bc.getInterfaceNames().length);
        try {
            _bc.declareInterface("foo");
            fail("Allowed declare interface");
        } catch (UnsupportedOperationException uoe) {
        }

        _bc.clearDeclaredInterfaces();
        assertTrue(!_bc.removeDeclaredInterface((String) null));
        assertTrue(!_bc.removeDeclaredInterface("foo"));

        assertTrue(_bc.isInstanceOf(_bc.getName()));
        assertTrue(!_bc.isInstanceOf("foo"));
    }

    /**
     * Test operations on fields.
     */
    public void testFields() {
        assertEquals(0, _bc.getDeclaredFields().length);
        assertEquals(0, _bc.getFields().length);
        try {
            _bc.declareField("foo", int.class);
            fail("Allowed declare field");
        } catch (UnsupportedOperationException uoe) {
        }

        _bc.clearDeclaredFields();
        assertTrue(!_bc.removeDeclaredField((String) null));
        assertTrue(!_bc.removeDeclaredField("foo"));
    }

    /**
     * Test operations on methods.
     */
    public void testMethods() {
        assertEquals(0, _bc.getDeclaredMethods().length);
        try {
            _bc.declareMethod("foo", int.class, null);
            fail("Allowed declare method");
        } catch (UnsupportedOperationException uoe) {
        }

        _bc.clearDeclaredMethods();
        assertTrue(!_bc.removeDeclaredMethod((String) null));
        assertTrue(!_bc.removeDeclaredMethod("foo"));

        try {
            _bc.addDefaultConstructor();
            fail("Allowed add default constructor");
        } catch (UnsupportedOperationException uoe) {
        }
    }

    /**
     * Test operations on attributes.
     */
    public void testAttributes() {
        assertNull(_bc.getSourceFile(false));
        try {
            _bc.getSourceFile(true);
            fail("Allowed add source file");
        } catch (UnsupportedOperationException uoe) {
        }

        assertNull(_bc.getInnerClasses(false));
        try {
            _bc.getInnerClasses(true);
            fail("Allowed add inner classes");
        } catch (UnsupportedOperationException uoe) {
        }

        assertTrue(!_bc.isDeprecated());
        try {
            _bc.setDeprecated(true);
            fail("Allowed set deprecated");
        } catch (UnsupportedOperationException uoe) {
        }

        assertEquals(0, _bc.getAttributes().length);
        _bc.clearAttributes();
        assertTrue(!_bc.removeAttribute(Constants.ATTR_SYNTHETIC));
        try {
            _bc.addAttribute(Constants.ATTR_SYNTHETIC);
            fail("Allowed add attribute");
        } catch (UnsupportedOperationException uoe) {
        }
    }

    /**
     * Tests that these types cannot be written.
     */
    public void testWrite() {
        try {
            _bc.toByteArray();
        } catch (UnsupportedOperationException uoe) {
        }
    }
}

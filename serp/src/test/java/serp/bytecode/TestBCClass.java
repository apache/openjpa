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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the {@link BCClass} type.
 * UNFINISHED
 *
 * @author Abe White
 */
public class TestBCClass extends TestCase {

    private Project _project = new Project();
    private BCClass _bc = _project.loadClass(Integer.class);

    public TestBCClass(String test) {
        super(test);
    }

    /**
     * Test accessing the class project.
     */
    public void testProject() {
        assertTrue(_project == _bc.getProject());
        assertTrue(_bc.isValid());
        assertTrue(_project.removeClass(_bc));
        assertTrue(!_bc.isValid());
    }

    /**
     * Test read/write.
     */
    public void testReadWrite() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = Integer.class.getResourceAsStream("Integer.class");
        int ch;
        while ((ch = in.read()) != -1)
            out.write(ch);

        byte[] origBytes = out.toByteArray();
        byte[] bytes = _bc.toByteArray();
        assertEquals(origBytes.length, bytes.length);
        for (int i = 0; i < origBytes.length; i++)
            assertEquals(origBytes[i], bytes[i]);

        // also test copying one class to another
        BCClass bc2 = new Project().loadClass(_bc);
        bytes = bc2.toByteArray();
        assertEquals(origBytes.length, bytes.length);
        for (int i = 0; i < origBytes.length; i++)
            assertEquals(origBytes[i], bytes[i]);
    }

    /**
     * Test basics -- magic number, major version, minor version.
     */
    public void testBasics() {
        assertEquals(Constants.VALID_MAGIC, _bc.getMagic());
        _bc.setMagic(1);
        assertEquals(1, _bc.getMagic());

        assertTrue(Constants.MAJOR_VERSION <= _bc.getMajorVersion());
        _bc.setMajorVersion(1);
        assertEquals(1, _bc.getMajorVersion());

        _bc.setMinorVersion(1);
        assertEquals(1, _bc.getMinorVersion());

        assertTrue(!_bc.isPrimitive());
        assertTrue(!_bc.isArray());
        assertNull(_bc.getComponentName());
        assertNull(_bc.getComponentType());
        assertNull(_bc.getComponentBC());
    }

    /**
     * Test access flags.
     */
    public void testAccessFlags() {
        assertEquals(Constants.ACCESS_PUBLIC | Constants.ACCESS_SUPER
            | Constants.ACCESS_FINAL, _bc.getAccessFlags());
        assertTrue(_bc.isPublic());
        assertTrue(!_bc.isPackage());
        assertTrue(_bc.isFinal());
        assertTrue(!_bc.isInterface());
        assertTrue(!_bc.isAbstract());

        _bc.setAccessFlags(Constants.ACCESS_ABSTRACT
            | Constants.ACCESS_INTERFACE);
        assertTrue(!_bc.isPublic());
        assertTrue(_bc.isPackage());
        assertTrue(!_bc.isFinal());
        assertTrue(_bc.isInterface());
        assertTrue(_bc.isAbstract());

        _bc.setAccessFlags(Constants.ACCESS_PUBLIC
            | Constants.ACCESS_SUPER | Constants.ACCESS_FINAL);

        _bc.makePackage();
        assertTrue(!_bc.isPublic());
        assertTrue(_bc.isPackage());
        _bc.makePublic();
        assertTrue(_bc.isPublic());
        assertTrue(!_bc.isPackage());

        _bc.setFinal(false);
        assertTrue(!_bc.isFinal());
        _bc.setFinal(true);
        assertTrue(_bc.isFinal());

        _bc.setAbstract(true);
        assertTrue(_bc.isAbstract());
        _bc.setAbstract(false);
        assertTrue(!_bc.isAbstract());

        _bc.setInterface(true);
        assertTrue(_bc.isInterface());
        assertTrue(_bc.isAbstract());
        _bc.setInterface(false);
        assertTrue(!_bc.isInterface());
    }

    /**
     * Test class type operations.
     */
    public void testType() {
        assertEquals(Integer.class.getName(), _bc.getName());
        assertEquals("java.lang", _bc.getPackageName());
        assertEquals("Integer", _bc.getClassName());
        assertEquals(Integer.class, _bc.getType());

        _bc.setName("serp.Foo");
        assertEquals("serp.Foo", _bc.getName());
    }

    /**
     * Test superclass operations.
     */
    public void testSuperclass() {
        assertEquals(Number.class.getName(), _bc.getSuperclassName());
        assertEquals(Number.class, _bc.getSuperclassType());
        assertEquals(Number.class.getName(), _bc.getSuperclassBC().getName());
        assertEquals(null, _bc.getSuperclassBC().getSuperclassBC().
            getSuperclassBC());

        _bc.setSuperclass(String.class);
        assertEquals(String.class.getName(), _bc.getSuperclassName());

        _bc.setSuperclass((BCClass) null);
        _bc.setSuperclass((Class) null);
        _bc.setSuperclass((String) null);
        assertNull(_bc.getSuperclassName());
        assertNull(_bc.getSuperclassType());
        assertNull(_bc.getSuperclassBC());

        assertEquals(0, _bc.getSuperclassIndex());
    }

    /**
     * Test operations on interfaces.
     */
    public void testInterfaces() {
        Object[] interfaces = _bc.getInterfaceNames();
        assertEquals(2, interfaces.length);
        assertEquals(Comparable.class.getName(), interfaces[0]);
        assertEquals(Serializable.class.getName(), interfaces[1]);

        interfaces = _bc.getInterfaceTypes();
        assertEquals(2, interfaces.length);
        assertEquals(Comparable.class, interfaces[0]);
        assertEquals(Serializable.class, interfaces[1]);

        interfaces = _bc.getInterfaceBCs();
        assertEquals(2, interfaces.length);
        assertEquals(Comparable.class, ((BCClass) interfaces[0]).getType());
        assertEquals(Serializable.class, ((BCClass) interfaces[1]).getType());

        interfaces = _bc.getDeclaredInterfaceNames();
        assertEquals(1, interfaces.length);
        assertEquals(Comparable.class.getName(), interfaces[0]);

        interfaces = _bc.getDeclaredInterfaceTypes();
        assertEquals(1, interfaces.length);
        assertEquals(Comparable.class, interfaces[0]);

        interfaces = _bc.getDeclaredInterfaceBCs();
        assertEquals(1, interfaces.length);
        assertEquals(Comparable.class, ((BCClass) interfaces[0]).getType());

        assertTrue(_bc.isInstanceOf(Comparable.class.getName()));
        assertTrue(_bc.isInstanceOf(Comparable.class));
        assertTrue(_bc.isInstanceOf(_project.loadClass(Comparable.class)));
        assertTrue(_bc.isInstanceOf(Serializable.class));
        assertTrue(!_bc.isInstanceOf(Cloneable.class.getName()));
        assertTrue(!_bc.isInstanceOf(Cloneable.class));
        assertTrue(!_bc.isInstanceOf(_project.loadClass(Cloneable.class)));

        _bc.clearDeclaredInterfaces();
        interfaces = _bc.getInterfaceNames();
        assertEquals(1, interfaces.length);
        assertEquals(Serializable.class.getName(), interfaces[0]);

        interfaces = _bc.getDeclaredInterfaceNames();
        assertEquals(0, interfaces.length);

        _bc.declareInterface(Comparable.class.getName());
        assertTrue(_bc.isInstanceOf(Comparable.class.getName()));
        interfaces = _bc.getDeclaredInterfaceNames();
        assertEquals(1, interfaces.length);
        assertEquals(Comparable.class.getName(), interfaces[0]);

        assertTrue(!_bc.removeDeclaredInterface(Serializable.class));
        assertTrue(_bc.removeDeclaredInterface(Comparable.class.getName()));
        interfaces = _bc.getDeclaredInterfaceNames();
        assertEquals(0, interfaces.length);

        _bc.declareInterface(Comparable.class);
        assertTrue(_bc.isInstanceOf(Comparable.class));
        interfaces = _bc.getDeclaredInterfaceTypes();
        assertEquals(1, interfaces.length);
        assertEquals(Comparable.class, interfaces[0]);

        assertTrue(_bc.removeDeclaredInterface(Comparable.class));
        interfaces = _bc.getDeclaredInterfaceNames();
        assertEquals(0, interfaces.length);

        _bc.declareInterface(_project.loadClass(Comparable.class));
        assertTrue(_bc.isInstanceOf(_project.loadClass(Comparable.class)));
        interfaces = _bc.getDeclaredInterfaceBCs();
        assertEquals(1, interfaces.length);
        assertEquals(Comparable.class, ((BCClass) interfaces[0]).getType());

        assertTrue(_bc.removeDeclaredInterface
            (_project.loadClass(Comparable.class)));
        interfaces = _bc.getDeclaredInterfaceNames();
        assertEquals(0, interfaces.length);
    }

    public static Test suite() {
        return new TestSuite(TestBCClass.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

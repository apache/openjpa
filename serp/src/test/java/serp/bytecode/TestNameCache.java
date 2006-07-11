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
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the {@link NameCache} utility type.
 *
 * @author Abe White
 */
public class TestNameCache extends TestCase {

    private NameCache _cache = null;

    public TestNameCache(String test) {
        super(test);
    }

    public void setUp() {
        _cache = new Project().getNameCache();
    }

    /**
     * Tests that class names are correctly converted to internal form.
     */
    public void testInternalForm() {
        assertEquals("I", _cache.getInternalForm("int", true));
        assertEquals("I", _cache.getInternalForm("int", false));
        assertEquals("I", _cache.getInternalForm("I", true));
        assertEquals("I", _cache.getInternalForm("I", false));

        assertEquals("B", _cache.getInternalForm("byte", true));
        assertEquals("C", _cache.getInternalForm("char", true));
        assertEquals("D", _cache.getInternalForm("double", true));
        assertEquals("F", _cache.getInternalForm("float", true));
        assertEquals("J", _cache.getInternalForm("long", true));
        assertEquals("S", _cache.getInternalForm("short", true));
        assertEquals("Z", _cache.getInternalForm("boolean", true));
        assertEquals("V", _cache.getInternalForm("void", true));

        assertEquals("Ljava/lang/Object;", _cache.getInternalForm
            (Object.class.getName(), true));
        assertEquals("java/lang/Object", _cache.getInternalForm
            (Object.class.getName(), false));
        assertEquals("Ljava/lang/Object;", _cache.getInternalForm
            ("Ljava/lang/Object;", true));
        assertEquals("java/lang/Object", _cache.getInternalForm
            ("Ljava/lang/Object;", false));
    }

    /**
     * Tests that array class names are correctly converted to internal form.
     */
    public void testArrayInternalForm() {
        assertEquals("[B", _cache.getInternalForm
            (byte[].class.getName(), false));
        assertEquals("[B", _cache.getInternalForm
            (byte[].class.getName(), true));
        assertEquals("[B", _cache.getInternalForm("byte[]", false));
        assertEquals("[B", _cache.getInternalForm("byte[]", true));

        assertEquals("[[Ljava/lang/Object;", _cache.getInternalForm
            (Object[][].class.getName(), false));
        assertEquals("[[Ljava/lang/Object;", _cache.getInternalForm
            (Object[][].class.getName(), true));
        assertEquals("[[Ljava/lang/Object;", _cache.getInternalForm
            ("java.lang.Object[][]", false));
        assertEquals("[[Ljava/lang/Object;", _cache.getInternalForm
            ("java.lang.Object[][]", true));
    }

    /**
     * Tests that class names are correctly converted to external form.
     */
    public void testExternalForm() {
        assertEquals("byte", _cache.getExternalForm("B", true));
        assertEquals("byte", _cache.getExternalForm("byte", true));
        assertEquals("byte", _cache.getExternalForm("B", false));
        assertEquals("byte", _cache.getExternalForm("byte", false));

        assertEquals("byte", _cache.getExternalForm("byte", false));
        assertEquals("byte", _cache.getExternalForm("B", true));
        assertEquals("char", _cache.getExternalForm("char", false));
        assertEquals("char", _cache.getExternalForm("C", true));
        assertEquals("double", _cache.getExternalForm("double", false));
        assertEquals("double", _cache.getExternalForm("D", true));
        assertEquals("float", _cache.getExternalForm("float", false));
        assertEquals("float", _cache.getExternalForm("F", true));
        assertEquals("int", _cache.getExternalForm("int", false));
        assertEquals("int", _cache.getExternalForm("I", true));
        assertEquals("long", _cache.getExternalForm("long", false));
        assertEquals("long", _cache.getExternalForm("J", true));
        assertEquals("short", _cache.getExternalForm("short", false));
        assertEquals("short", _cache.getExternalForm("S", true));
        assertEquals("boolean", _cache.getExternalForm("boolean", false));
        assertEquals("boolean", _cache.getExternalForm("Z", true));
        assertEquals("void", _cache.getExternalForm("void", false));
        assertEquals("void", _cache.getExternalForm("V", true));

        assertEquals("[B", _cache.getExternalForm("byte[]", false));
        assertEquals("[C", _cache.getExternalForm("char[]", false));
        assertEquals("[D", _cache.getExternalForm("double[]", false));
        assertEquals("[F", _cache.getExternalForm("float[]", false));
        assertEquals("[I", _cache.getExternalForm("int[]", false));
        assertEquals("[J", _cache.getExternalForm("long[]", false));
        assertEquals("[S", _cache.getExternalForm("short[]", false));
        assertEquals("[Z", _cache.getExternalForm("boolean[]", false));

        assertEquals("java.lang.Object", _cache.getExternalForm
            ("java.lang.Object", true));
        assertEquals("java.lang.Object", _cache.getExternalForm
            ("java/lang/Object", true));
        assertEquals("java.lang.Object", _cache.getExternalForm
            ("java/lang/Object", false));
        assertEquals("java.lang.Object", _cache.getExternalForm
            ("Ljava/lang/Object;", false));
    }

    /**
     * Tests that array class names are correctly converted to external form.
     */
    public void testArrayExternalForm() {
        assertEquals("byte[]", _cache.getExternalForm("byte[]", true));
        assertEquals("byte[]", _cache.getExternalForm
            (byte[].class.getName(), true));
        assertEquals("[B", _cache.getExternalForm("byte[]", false));
        assertEquals("[B", _cache.getExternalForm
            (byte[].class.getName(), false));

        assertEquals("java.lang.Object[][]", _cache.getExternalForm
            ("java.lang.Object[][]", true));
        assertEquals("java.lang.Object[][]", _cache.getExternalForm
            (Object[][].class.getName(), true));
        assertEquals("[[Ljava.lang.Object;", _cache.getExternalForm
            ("java.lang.Object[][]", false));
        assertEquals("[[Ljava.lang.Object;", _cache.getExternalForm
            (Object[][].class.getName(), false));
    }

    /**
     * Tests that method descriptors are correctly formed.
     */
    public void testDescriptors() {
        assertEquals("()V", _cache.getDescriptor("V", new String[0]));
        assertEquals("()V", _cache.getDescriptor("void", null));
        assertEquals("()Ljava/lang/Object;", _cache.getDescriptor
            ("java.lang.Object", null));
        assertEquals("(Z)V", _cache.getDescriptor
            ("void", new String[]{ "boolean" }));
        assertEquals("([ZLjava/lang/Object;I)[I", _cache.getDescriptor
            ("int[]", new String[]{ "[Z", "Ljava/lang/Object;", "int" }));
    }

    /**
     * Test that return types are extracted from method descriptors.
     */
    public void testDescriptorReturnName() {
        assertEquals("", _cache.getDescriptorReturnName("foo"));
        assertEquals("V", _cache.getDescriptorReturnName("()V"));
        assertEquals("[Ljava/lang/Object;", _cache.getDescriptorReturnName
            ("(IZLjava/lang/String;)[Ljava/lang/Object;"));
    }

    /**
     * Test that param types are extracted from method descriptors.
     */
    public void testDescriptorParamNames() {
        assertEquals(0, _cache.getDescriptorParamNames("foo").length);

        String[] params = _cache.getDescriptorParamNames
            ("([ZLjava/lang/Object;I)[I");
        assertEquals(3, params.length);
        assertEquals("[Z", params[0]);
        assertEquals("Ljava/lang/Object;", params[1]);
        assertEquals("I", params[2]);
    }

    /**
     * Test {@link NameCache#getComponentTypeName}.
     */
    public void testComponentTypes() {
        assertNull(_cache.getComponentName(null));
        assertNull(_cache.getComponentName(int.class.getName()));
        assertNull(_cache.getComponentName(String.class.getName()));
        assertEquals(int.class.getName(),
            _cache.getComponentName(int[].class.getName()));
        assertEquals(int[][].class.getName(),
            _cache.getComponentName(int[][][].class.getName()));
        assertEquals(String.class.getName(),
            _cache.getComponentName(String[].class.getName()));
        assertEquals(String[][].class.getName(),
            _cache.getComponentName(String[][][].class.getName()));
    }

    public static Test suite() {
        return new TestSuite(TestNameCache.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

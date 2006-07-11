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
 * Tests the {@link Attributes} type.
 *
 * @author Abe White
 */
public class TestAttributes extends TestCase {

    private Project _project = new Project();
    private Attributes _attrs = _project.loadClass("serp.Attrs");
    private Attributes _attrs2 = _project.loadClass("serp.Attrs2");

    public TestAttributes(String test) {
        super(test);
    }

    /**
     * Test getting attributes.
     */
    public void testGetAttributes() {
        assertEquals(0, _attrs.getAttributes().length);
        assertNull(_attrs.getAttribute(Constants.ATTR_SYNTHETIC));

        Attribute attr1 = _attrs.addAttribute(Constants.ATTR_DEPRECATED);
        Attribute attr2 = _attrs.addAttribute(Constants.ATTR_SYNTHETIC);

        assertEquals(2, _attrs.getAttributes().length);
        assertNull(_attrs.getAttribute(Constants.ATTR_CODE));
        assertTrue(attr1 == _attrs.getAttribute(Constants.ATTR_DEPRECATED));
        assertTrue(attr2 == _attrs.getAttribute(Constants.ATTR_SYNTHETIC));
        assertEquals(0, _attrs.getAttributes(Constants.ATTR_CODE).length);
        assertEquals(1, _attrs.getAttributes(Constants.ATTR_DEPRECATED).
            length);
        assertEquals(1, _attrs.getAttributes(Constants.ATTR_SYNTHETIC). length);
        assertTrue(attr1 == _attrs.getAttributes
            (Constants.ATTR_DEPRECATED)[0]);
        assertTrue(attr2 == _attrs.getAttributes(Constants.ATTR_SYNTHETIC)[0]);

        Attribute attr3 = _attrs.addAttribute(Constants.ATTR_DEPRECATED);
        assertEquals(3, _attrs.getAttributes().length);
        assertEquals(2, _attrs.getAttributes(Constants.ATTR_DEPRECATED).
            length);
    }

    /**
     * Test setting attributes.
     */
    public void testSetAttributes() {
        Attribute attr1 = _attrs.addAttribute(Constants.ATTR_DEPRECATED);
        Attribute attr2 = _attrs.addAttribute(Constants.ATTR_SYNTHETIC);

        _attrs2.setAttributes(_attrs.getAttributes());
        assertEquals(2, _attrs2.getAttributes().length);
        assertEquals(Constants.ATTR_DEPRECATED, _attrs2.getAttribute
            (Constants.ATTR_DEPRECATED).getName());
        assertEquals(Constants.ATTR_SYNTHETIC, _attrs2.getAttribute
            (Constants.ATTR_SYNTHETIC).getName());
        assertTrue(attr1 != _attrs2.getAttribute(Constants.ATTR_DEPRECATED));
        assertTrue(attr2 != _attrs2.getAttribute(Constants.ATTR_SYNTHETIC));

        Attribute attr3 = _attrs.addAttribute(Constants.ATTR_SOURCE);
        _attrs2.setAttributes(new Attribute[]{ attr3 });
        assertEquals(1, _attrs2.getAttributes().length);
        assertEquals(Constants.ATTR_SOURCE, _attrs2.getAttributes()[0].
            getName());
    }

    /**
     * Test adding attributs.
     */
    public void testAddAttributes() {
        SourceFile attr1 = (SourceFile) _attrs.addAttribute
            (Constants.ATTR_SOURCE);
        assertEquals(attr1.getName(), Constants.ATTR_SOURCE);
        assertTrue(attr1 != _attrs.addAttribute(Constants.ATTR_SOURCE));
        assertEquals(2, _attrs.getAttributes(Constants.ATTR_SOURCE).length);
        attr1.setFile("foo");

        SourceFile attr2 = (SourceFile) _attrs2.addAttribute(attr1);
        assertTrue(attr1 != attr2);
        assertEquals("foo", attr2.getFileName());
    }

    /**
     * Test clearing attributes.
     */
    public void testClear() {
        _attrs.clearAttributes();

        Attribute attr1 = _attrs.addAttribute(Constants.ATTR_SYNTHETIC);
        Attribute attr2 = _attrs.addAttribute(Constants.ATTR_DEPRECATED);

        assertTrue(attr1.isValid());
        assertTrue(attr2.isValid());

        assertEquals(2, _attrs.getAttributes().length);
        _attrs.clearAttributes();
        assertEquals(0, _attrs.getAttributes().length);

        // cleared classes should be invalid
        assertTrue(!attr1.isValid());
        assertTrue(!attr2.isValid());
    }

    /**
     * Test removing a class.
     */
    public void testRemoveAttribute() {
        assertTrue(!_attrs.removeAttribute((String) null));
        assertTrue(!_attrs.removeAttribute((Attribute) null));
        assertTrue(!_attrs.removeAttribute(Constants.ATTR_SYNTHETIC));
        assertTrue(!_attrs.removeAttribute(_attrs2.addAttribute
            (Constants.ATTR_SYNTHETIC)));

        Attribute attr1 = _attrs.addAttribute(Constants.ATTR_SYNTHETIC);
        Attribute attr2 = _attrs.addAttribute(Constants.ATTR_DEPRECATED);

        assertTrue(attr1.isValid());
        assertTrue(attr2.isValid());
        assertEquals(2, _attrs.getAttributes().length);

        assertTrue(_attrs.removeAttribute(attr1.getName()));
        assertEquals(1, _attrs.getAttributes().length);

        assertTrue(!_attrs.removeAttribute(_attrs2.addAttribute
            (attr2.getName())));
        assertTrue(_attrs.removeAttribute(attr2));
        assertEquals(0, _attrs.getAttributes().length);

        assertTrue(!attr1.isValid());
        assertTrue(!attr2.isValid());
    }

    public static Test suite() {
        return new TestSuite(TestAttributes.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

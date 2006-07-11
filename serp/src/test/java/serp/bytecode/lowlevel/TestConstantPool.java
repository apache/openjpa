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
package serp.bytecode.lowlevel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the {@link ConstantPool} type.
 *
 * @author Abe White
 */
public class TestConstantPool extends TestCase {

    private ConstantPool _pool = new ConstantPool();
    private IntEntry _intEntry = new IntEntry(1);
    private LongEntry _longEntry = new LongEntry(2L);
    private UTF8Entry _utf8Entry = new UTF8Entry("4");

    public TestConstantPool(String test) {
        super(test);
    }

    /**
     * Tests adding entries.
     */
    public void testAdd() {
        assertEquals(0, _pool.size());
        assertEquals(1, _pool.addEntry(_intEntry));
        assertEquals(1, _pool.size());
        assertEquals(2, _pool.addEntry(_longEntry));
        assertEquals(3, _pool.size());
        assertEquals(4, _pool.addEntry(_utf8Entry));
        assertEquals(4, _pool.size());

        assertEquals(_intEntry, _pool.getEntry(1));
        assertEquals(_longEntry, _pool.getEntry(2));
        assertEquals(_utf8Entry, _pool.getEntry(4));
        assertEquals(1, _pool.findIntEntry(1, false));
        assertEquals(2, _pool.findLongEntry(2L, false));
        assertEquals(4, _pool.findUTF8Entry("4", false));

        // add an entry multiple times
        assertEquals(4, _pool.addEntry(_utf8Entry));
        assertEquals(5, _pool.addEntry(new UTF8Entry("4")));

        // cannot add to another pool
        ConstantPool pool = new ConstantPool();
        try {
            pool.addEntry(_intEntry);
            fail("Entry already in another pool");
        } catch (IllegalStateException ise) {
        }
        assertEquals(0, pool.size());

        _pool.removeEntry(_intEntry);
        assertEquals(0, _pool.indexOf(_intEntry));

        pool.addEntry(_intEntry);
        assertEquals(1, pool.size());
        assertEquals(_intEntry, pool.getEntry(1));
    }

    /**
     * Tests removing entries.
     */
    public void testRemove() {
        _pool.addEntry(_intEntry);
        _pool.addEntry(_longEntry);
        _pool.addEntry(_utf8Entry);

        assertEquals(4, _pool.size());
        assertTrue(_pool.removeEntry(_longEntry));
        assertEquals(2, _pool.size());
        assertEquals(_intEntry, _pool.getEntry(1));
        assertEquals(_utf8Entry, _pool.getEntry(2));
        assertEquals(1, _pool.findIntEntry(1, false));
        assertEquals(2, _pool.findUTF8Entry("4", false));
        assertEquals(0, _pool.findLongEntry(2L, false));

        assertTrue(!_pool.removeEntry(_longEntry));

        assertTrue(_pool.removeEntry(_intEntry));
        assertEquals(1, _pool.size());
        assertEquals(_utf8Entry, _pool.getEntry(1));
        assertEquals(0, _pool.findIntEntry(1, false));
        assertEquals(1, _pool.findUTF8Entry("4", false));

        assertTrue(_pool.removeEntry(_utf8Entry));
        assertEquals(0, _pool.size());
        try {
            _pool.getEntry(1);
            fail("Invalid index");
        } catch (IndexOutOfBoundsException ioobe) {
        }
        assertEquals(0, _pool.findUTF8Entry("4", false));
    }

    /**
     * Tests mutating entries.
     */
    public void testMutate() {
        _intEntry.setValue(2);
        _pool.addEntry(_intEntry);
        _pool.addEntry(_longEntry);
        _pool.addEntry(_utf8Entry);

        assertEquals(1, _pool.findIntEntry(2, false));
        assertEquals(2, _pool.findLongEntry(2L, false));
        assertEquals(4, _pool.findUTF8Entry("4", false));

        _intEntry.setValue(1);
        _longEntry.setValue(3L);
        _utf8Entry.setValue("foo");

        assertEquals(1, _pool.findIntEntry(1, false));
        assertEquals(2, _pool.findLongEntry(3L, false));
        assertEquals(4, _pool.findUTF8Entry("foo", false));
    }

    /**
     * Tests finding the index of entries.
     */
    public void testIndexOf() {
        _pool.addEntry(_intEntry);
        _pool.addEntry(_longEntry);
        _pool.addEntry(_utf8Entry);

        assertEquals(1, _pool.indexOf(_intEntry));
        assertEquals(2, _pool.indexOf(_longEntry));
        assertEquals(4, _pool.indexOf(_utf8Entry));

        _pool.removeEntry(_longEntry);
        assertEquals(1, _pool.indexOf(_intEntry));
        assertEquals(0, _pool.indexOf(_longEntry));
        assertEquals(2, _pool.indexOf(_utf8Entry));
    }

    /**
     * Tests getting all entries.
     */
    public void testGetEntries() {
        _pool.addEntry(_intEntry);
        _pool.addEntry(_longEntry);
        _pool.addEntry(_utf8Entry);

        Entry[] entries = _pool.getEntries();
        assertEquals(3, entries.length);
        assertEquals(_intEntry, entries[0]);
        assertEquals(_longEntry, entries[1]);
        assertEquals(_utf8Entry, entries[2]);

        _pool.clear();
        assertEquals(0, _pool.getEntries().length);
    }

    /**
     * Tests getting entries by index.
     */
    public void testGetEntry() {
        _pool.addEntry(_intEntry);
        _pool.addEntry(_longEntry);
        _pool.addEntry(_utf8Entry);

        assertEquals(_intEntry, _pool.getEntry(1));
        assertEquals(_longEntry, _pool.getEntry(2));
        assertEquals(_utf8Entry, _pool.getEntry(4));

        try {
            _pool.getEntry(0);
            fail("0 index");
        } catch (IndexOutOfBoundsException ioobe) {
        }

        try {
            _pool.getEntry(_pool.size() + 1);
            fail("size() + 1 index");
        } catch (IndexOutOfBoundsException ioobe) {
        }

        try {
            _pool.getEntry(3);
            fail("Wide index");
        } catch (IndexOutOfBoundsException ioobe) {
        }
    }

    /**
     * Test clearing the pool.
     */
    public void testClear() {
        // make sure clearing empty pool OK
        _pool.clear();
        assertEquals(0, _pool.size());

        _pool.addEntry(_intEntry);
        _pool.addEntry(_longEntry);
        _pool.addEntry(_utf8Entry);

        _pool.clear();
        assertEquals(0, _pool.size());
        assertEquals(0, _pool.findIntEntry(1, false));
        assertEquals(0, _pool.findUTF8Entry("4", false));
    }

    /**
     * Test finding entry indexes.
     */
    public void testFind() {
        int double1 = _pool.addEntry(new DoubleEntry(1D));
        int double2 = _pool.addEntry(new DoubleEntry(2D));
        int float1 = _pool.addEntry(new FloatEntry(1F));
        int float2 = _pool.addEntry(new FloatEntry(2F));
        int int1 = _pool.addEntry(new IntEntry(1));
        int int2 = _pool.addEntry(new IntEntry(2));
        int long1 = _pool.addEntry(new LongEntry(1L));
        int long2 = _pool.addEntry(new LongEntry(2L));
        int utf1 = _pool.addEntry(new UTF8Entry("1"));
        int utf2 = _pool.addEntry(new UTF8Entry("2"));
        int string1 = _pool.addEntry(new StringEntry(utf1));
        int string2 = _pool.addEntry(new StringEntry(utf2));
        int class1 = _pool.addEntry(new ClassEntry(utf1));
        int class2 = _pool.addEntry(new ClassEntry(utf2));
        int name1 = _pool.addEntry(new NameAndTypeEntry(utf1, utf2));
        int name2 = _pool.addEntry(new NameAndTypeEntry(utf2, utf1));
        int field1 = _pool.addEntry(new FieldEntry(class1, name1));
        int field2 = _pool.addEntry(new FieldEntry(class2, name2));
        int method1 = _pool.addEntry(new MethodEntry(class1, name1));
        int method2 = _pool.addEntry(new MethodEntry(class2, name2));
        int imethod1 = _pool.addEntry(new InterfaceMethodEntry(class1, name1));
        int imethod2 = _pool.addEntry(new InterfaceMethodEntry(class2, name2));

        assertEquals(0, _pool.findDoubleEntry(0D, false));
        assertEquals(double1, _pool.findDoubleEntry(1D, false));
        assertEquals(double2, _pool.findDoubleEntry(2D, false));
        assertEquals(double1, _pool.findDoubleEntry(1D, true));
        assertEquals(double2, _pool.findDoubleEntry(2D, true));
        assertEquals(_pool.size() + 1, _pool.findDoubleEntry(0D, true));

        assertEquals(0, _pool.findFloatEntry(0F, false));
        assertEquals(float1, _pool.findFloatEntry(1F, false));
        assertEquals(float2, _pool.findFloatEntry(2F, false));
        assertEquals(float1, _pool.findFloatEntry(1F, true));
        assertEquals(float2, _pool.findFloatEntry(2F, true));
        assertEquals(_pool.size() + 1, _pool.findFloatEntry(0F, true));

        assertEquals(0, _pool.findIntEntry(0, false));
        assertEquals(int1, _pool.findIntEntry(1, false));
        assertEquals(int2, _pool.findIntEntry(2, false));
        assertEquals(int1, _pool.findIntEntry(1, true));
        assertEquals(int2, _pool.findIntEntry(2, true));
        assertEquals(_pool.size() + 1, _pool.findIntEntry(0, true));

        assertEquals(0, _pool.findLongEntry(0L, false));
        assertEquals(long1, _pool.findLongEntry(1L, false));
        assertEquals(long2, _pool.findLongEntry(2L, false));
        assertEquals(long1, _pool.findLongEntry(1L, true));
        assertEquals(long2, _pool.findLongEntry(2L, true));
        assertEquals(_pool.size() + 1, _pool.findLongEntry(0L, true));

        assertEquals(0, _pool.findUTF8Entry("0", false));
        assertEquals(utf1, _pool.findUTF8Entry("1", false));
        assertEquals(utf2, _pool.findUTF8Entry("2", false));
        assertEquals(utf1, _pool.findUTF8Entry("1", true));
        assertEquals(utf2, _pool.findUTF8Entry("2", true));
        assertEquals(_pool.size() + 1, _pool.findUTF8Entry("0", true));

        assertEquals(0, _pool.findStringEntry("0", false));
        assertEquals(string1, _pool.findStringEntry("1", false));
        assertEquals(string2, _pool.findStringEntry("2", false));
        assertEquals(string1, _pool.findStringEntry("1", true));
        assertEquals(string2, _pool.findStringEntry("2", true));
        assertEquals(_pool.size() + 1, _pool.findStringEntry("0", true));
        assertEquals(_pool.size() + 2, _pool.findStringEntry("aaa", true));

        assertEquals(0, _pool.findClassEntry("0", false));
        assertEquals(class1, _pool.findClassEntry("1", false));
        assertEquals(class2, _pool.findClassEntry("2", false));
        assertEquals(class1, _pool.findClassEntry("1", true));
        assertEquals(class2, _pool.findClassEntry("2", true));
        assertEquals(_pool.size() + 1, _pool.findClassEntry("0", true));
        assertEquals(_pool.size() + 2, _pool.findStringEntry("bbb", true));

        assertEquals(0, _pool.findNameAndTypeEntry("0", "1", false));
        assertEquals(name1, _pool.findNameAndTypeEntry("1", "2", false));
        assertEquals(name2, _pool.findNameAndTypeEntry("2", "1", false));
        assertEquals(name1, _pool.findNameAndTypeEntry("1", "2", true));
        assertEquals(name2, _pool.findNameAndTypeEntry("2", "1", true));
        assertEquals(_pool.size() + 1, _pool.findNameAndTypeEntry
            ("0", "1", true));
        assertEquals(_pool.size() + 2, _pool.findNameAndTypeEntry
            ("2", "3", true));
        assertEquals(_pool.size() + 3, _pool.findNameAndTypeEntry
            ("ccc", "ddd", true));

        assertEquals(0, _pool.findFieldEntry("0", "1", "2", false));
        assertEquals(field1, _pool.findFieldEntry("1", "1", "2", false));
        assertEquals(field2, _pool.findFieldEntry("2", "2", "1", false));
        assertEquals(field1, _pool.findFieldEntry("1", "1", "2", true));
        assertEquals(field2, _pool.findFieldEntry("2", "2", "1", true));
        assertEquals(_pool.size() + 1, _pool.findFieldEntry
            ("1", "2", "1", true));
        assertEquals(_pool.size() + 3, _pool.findFieldEntry
            ("1", "3", "4", true));
        assertEquals(_pool.size() + 6, _pool.findFieldEntry
            ("eee", "fff", "ggg", true));

        assertEquals(0, _pool.findMethodEntry("0", "1", "2", false));
        assertEquals(method1, _pool.findMethodEntry("1", "1", "2", false));
        assertEquals(method2, _pool.findMethodEntry("2", "2", "1", false));
        assertEquals(method1, _pool.findMethodEntry("1", "1", "2", true));
        assertEquals(method2, _pool.findMethodEntry("2", "2", "1", true));
        assertEquals(_pool.size() + 1, _pool.findMethodEntry
            ("1", "2", "1", true));
        assertEquals(_pool.size() + 3, _pool.findMethodEntry
            ("1", "3", "5", true));
        assertEquals(_pool.size() + 6, _pool.findMethodEntry
            ("hhh", "iii", "jjj", true));

        assertEquals(0, _pool.findInterfaceMethodEntry("0", "1", "2", false));
        assertEquals(imethod1, _pool.findInterfaceMethodEntry
            ("1", "1", "2", false));
        assertEquals(imethod2, _pool.findInterfaceMethodEntry
            ("2", "2", "1", false));
        assertEquals(imethod1, _pool.findInterfaceMethodEntry
            ("1", "1", "2", true));
        assertEquals(imethod2, _pool.findInterfaceMethodEntry
            ("2", "2", "1", true));
        assertEquals(_pool.size() + 1, _pool.findInterfaceMethodEntry
            ("1", "2", "1", true));
        assertEquals(_pool.size() + 3, _pool.findInterfaceMethodEntry
            ("1", "3", "6", true));
        assertEquals(_pool.size() + 6, _pool.findInterfaceMethodEntry
            ("kkk", "lll", "mmm", true));
    }

    public static Test suite() {
        return new TestSuite(TestConstantPool.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}

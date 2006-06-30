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

import java.io.*;
import java.util.*;
import serp.bytecode.visitor.*;
import serp.util.*;

/**
 * A bytecode constant pool, containing entries for all strings,
 * constants, classes, etc referenced in the class structure and method
 * opcodes. In keeping with the low-level bytecode representation, all pool
 * indexes are 1-based and {@link LongEntry}s and {@link DoubleEntry}s each
 * occupy two indexes in the pool.
 * 
 * @author Abe White
 */
public class ConstantPool implements VisitAcceptor {
    private List _entries = new ArrayList(50);
    private Map _lookup = new HashMap(50);

    /**
     * Default constructor.
     */
    public ConstantPool() {
    }

    /**
     * Return all the entries in the pool.
     */
    public Entry[] getEntries() {
        List entries = new ArrayList(_entries.size());
        Entry entry;
        for (Iterator itr = _entries.iterator(); itr.hasNext();) {
            entry = (Entry) itr.next();
            if (entry != null)
                entries.add(entry);
        }
        return(Entry[]) entries.toArray(new Entry[entries.size()]);
    }

    /**
     * Retrieve the entry at the specified 1-based index.
     * 
     * @throws IndexOutOfBoundsException if index is invalid,
     * including the case that it points to the second slot of a
     * long or double entry
     */
    public Entry getEntry(int index) {
        Entry entry = (Entry) _entries.get(index - 1);
        if (entry == null)
            throw new IndexOutOfBoundsException("index = " + index);
        return entry;
    }

    /**
     * Return the index of the given entry, or 0 if it is not in the pool.
     */
    public int indexOf(Entry entry) {
        if (entry == null || entry.getPool() != this)
            return 0;
        return entry.getIndex();
    }

    /**
     * Add an entry to the pool.
     * 
     * @return the index at which the entry was added
     */
    public int addEntry(Entry entry) {
        if (entry.getPool() != this)
            addEntry(getKey(entry), entry);
        return entry.getIndex();
    }

    /**
     * Add an entry to the pool using the given key.
     */
    private int addEntry(Object key, Entry entry) {
        entry.setPool(this);
        _entries.add(entry);
        entry.setIndex(_entries.size());

        _lookup.put(key, entry);
        if (entry.isWide())
            _entries.add(null);
        return entry.getIndex();
    }

    /**
     * Remove the given entry from the pool.
     * 
     * @return false if the entry is not in the pool, true otherwise
     */
    public boolean removeEntry(Entry entry) {
        if (entry == null || entry.getPool() != this)
            return false;

        int index = entry.getIndex() - 1;
        entry.setPool(null);
        entry.setIndex(0);

        _entries.remove(index);
        if (entry.isWide())
            _entries.remove(index);
        _lookup.remove(getKey(entry));

        // rehash all the entries after the removed one with their new index
        Object key;
        for (int i = index; i < _entries.size(); i++) {
            entry = (Entry) _entries.get(i);
            if (entry != null) {
                key = getKey(entry);
                _lookup.remove(key);
                entry.setIndex(i + 1);
                _lookup.put(key, entry);
            }
        }
        return true;
    }

    /**
     * Clear all entries from the pool.
     */
    public void clear() {
        Entry entry;
        for (Iterator itr = _entries.iterator(); itr.hasNext();) {
            entry = (Entry) itr.next();
            if (entry != null) {
                entry.setPool(null);
                entry.setIndex(0);
            }
        }

        _entries.clear();
        _lookup.clear();
    }

    /**
     * Return the number of places occupied in the pool, including the fact
     * that long and double entries occupy two places.
     */
    public int size() {
        return _entries.size();
    }

    /**
     * Return the index of the {@link UTF8Entry} with the given value, or
     * 0 if it does not exist.
     * 
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findUTF8Entry(String value, boolean add) {
        if (value == null) {
            if (add)
                throw new NullPointerException("value = null");
            return 0;
        }

        int index = find(value);
        if (!add || index > 0)
            return index;
        return addEntry(value, new UTF8Entry(value));
    }

    /**
     * Return the constant pool index of the {@link DoubleEntry} for the given
     * value, or 0 if it does not exist.
     * 
     * @param value the value to find
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findDoubleEntry(double value, boolean add) {
        Double key = new Double(value);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new DoubleEntry(value));
    }

    /**
     * Return the constant pool index of the {@link FloatEntry} for the given
     * value, or 0 if it does not exist.
     * 
     * @param value the value to find
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findFloatEntry(float value, boolean add) {
        Float key = new Float(value);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new FloatEntry(value));
    }

    /**
     * Return the constant pool index of the {@link IntEntry} for the given
     * value, or 0 if it does not exist.
     * 
     * @param value the value to find
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findIntEntry(int value, boolean add) {
        Integer key = Numbers.valueOf(value);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new IntEntry(value));
    }

    /**
     * Return the constant pool index of the {@link LongEntry} for the given
     * value, or 0 if it does not exist.
     * 
     * @param value the value to find
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findLongEntry(long value, boolean add) {
        Long key = Numbers.valueOf(value);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new LongEntry(value));
    }

    /**
     * Return the constant pool index of the {@link StringEntry} for the given
     * string value, or 0 if it does not exist.
     * 
     * @param value the value to find
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findStringEntry(String value, boolean add) {
        int valueIndex = findUTF8Entry(value, add);
        if (valueIndex == 0)
            return 0;

        StringKey key = new StringKey(valueIndex);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new StringEntry(valueIndex));
    }

    /**
     * Return the constant pool index of the {@link ClassEntry} for the given
     * class name, or 0 if it does not exist.
     * 
     * @param name the class name in internal form
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findClassEntry(String name, boolean add) {
        int nameIndex = findUTF8Entry(name, add);
        if (nameIndex == 0)
            return 0;

        ClassKey key = new ClassKey(nameIndex);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new ClassEntry(nameIndex));
    }

    /**
     * Return the constant pool index of the {@link NameAndTypeEntry} for the
     * given name and descriptor, or 0 if it does not exist.
     * 
     * @param name the name of the entity
     * @param desc the descriptor of the entity in internal form
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findNameAndTypeEntry(String name, String desc, boolean add) {
        int nameIndex = findUTF8Entry(name, add);
        if (nameIndex == 0)
            return 0;
        int descIndex = findUTF8Entry(desc, add);
        if (descIndex == 0)
            return 0;

        NameAndTypeKey key = new NameAndTypeKey(nameIndex, descIndex);
        int index = find(key);
        if (!add || index > 0)
            return index;
        return addEntry(key, new NameAndTypeEntry(nameIndex, descIndex));
    }

    /**
     * Return the constant pool index of the {@link FieldEntry} for the
     * given name, descriptor, and owner class name.
     * 
     * @param owner the name of the field's owning class in internal form
     * @param name the name of the field
     * @param desc the descriptor of the field in internal form
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findFieldEntry(String owner, String name, String desc,
        boolean add) {
        return findComplexEntry(owner, name, desc, Entry.FIELD, add);
    }

    /**
     * Return the constant pool index of the {@link MethodEntry} for the
     * given name, descriptor, and owner class name.
     * 
     * @param owner the name of the method's owning class in internal form
     * @param name the name of the method
     * @param desc the descriptor of the method in internal form
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findMethodEntry(String owner, String name, String desc,
        boolean add) {
        return findComplexEntry(owner, name, desc, Entry.METHOD, add);
    }

    /**
     * Return the constant pool index of the {@link InterfaceMethodEntry} for
     * the given name, descriptor, and owner class name.
     * 
     * @param owner the name of the method's owning class in internal form
     * @param name the name of the method
     * @param desc the descriptor of the method in internal form
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    public int findInterfaceMethodEntry(String owner, String name, String desc,
        boolean add) {
        return findComplexEntry(owner, name, desc, Entry.INTERFACEMETHOD, add);
    }

    /**
     * Return the constant pool index of the {@link ComplexEntry} for the
     * given name, descriptor, and owner class name.
     * 
     * @param owner the name of the owning class in internal form
     * @param name the name of the entity
     * @param desc the descriptor of the entity in internal form
     * @param type the type of entry: field, method, interface method
     * @param add if true, the entry will be added if it does not
     * already exist, and the new entry's index returned
     */
    private int findComplexEntry(String owner, String name, String desc,
        int type, boolean add) {
        int classIndex = findClassEntry(owner, add);
        if (classIndex == 0)
            return 0;
        int descIndex = findNameAndTypeEntry(name, desc, add);
        if (descIndex == 0)
            return 0;

        Object key = null;
        switch (type) {
        case Entry.FIELD:
            key = new FieldKey(classIndex, descIndex);
            break;
        case Entry.METHOD:
            key = new MethodKey(classIndex, descIndex);
            break;
        case Entry.INTERFACEMETHOD:
            key = new InterfaceMethodKey(classIndex, descIndex);
            break;
        }
        int index = find(key);
        if (!add || index > 0)
            return index;

        Entry entry = null;
        switch (type) {
        case Entry.FIELD:
            entry = new FieldEntry(classIndex, descIndex);
            break;
        case Entry.METHOD:
            entry = new MethodEntry(classIndex, descIndex);
            break;
        case Entry.INTERFACEMETHOD:
            entry = new InterfaceMethodEntry(classIndex, descIndex);
            break;
        }
        return addEntry(key, entry);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterConstantPool(this);

        Entry entry;
        for (Iterator itr = _entries.iterator(); itr.hasNext();) {
            entry = (Entry) itr.next();
            if (entry == null)
                continue;

            visit.enterEntry(entry);
            entry.acceptVisit(visit);
            visit.exitEntry(entry);
        }

        visit.exitConstantPool(this);
    }

    /**
     * Fill the constant pool from the given bytecode stream.
     */
    public void read(DataInput in) throws IOException {
        clear();

        int entryCount = in.readUnsignedShort();
        Entry entry;
        for (int i = 1; i < entryCount; i++) {
            entry = Entry.read(in);
            addEntry(entry);

            if (entry.isWide())
                i++;
        }
    }

    /**
     * Write the constant pool to the given bytecode stream.
     */
    public void write(DataOutput out) throws IOException {
        out.writeShort(_entries.size() + 1);

        Entry entry;
        for (Iterator itr = _entries.iterator(); itr.hasNext();) {
            entry = (Entry) itr.next();
            if (entry != null)
                Entry.write(entry, out);
        }
    }

    /**
     * Called by constant pool entries when they are mutated.
     */
    void modifyEntry(Object origKey, Entry entry) {
        _lookup.remove(origKey);
        _lookup.put(getKey(entry), entry);
    }

    /**
     * Returns the constant pool index of the entry with the given key.
     */
    private int find(Object key) {
        Entry entry = (Entry) _lookup.get(key);
        if (entry == null)
            return 0;
        return entry.getIndex();
    }

    /**
     * Return the hash key used for the specified entry.
     */
    static Object getKey(Entry entry) {
        switch (entry.getType()) {
        case Entry.CLASS:
            return new ClassKey(((ClassEntry) entry).getNameIndex());
        case Entry.FIELD:
            FieldEntry fe = (FieldEntry) entry;
            return new FieldKey(fe.getClassIndex(), fe.getNameAndTypeIndex());
        case Entry.METHOD:
            MethodEntry me = (MethodEntry) entry;
            return new MethodKey(me.getClassIndex(), me.getNameAndTypeIndex());
        case Entry.INTERFACEMETHOD:
            InterfaceMethodEntry ime = (InterfaceMethodEntry) entry;
            return new InterfaceMethodKey(ime.getClassIndex(),
                ime.getNameAndTypeIndex());
        case Entry.STRING:
            return new StringKey(((StringEntry) entry).getStringIndex());
        case Entry.INT:
        case Entry.FLOAT:
        case Entry.LONG:
        case Entry.DOUBLE:
        case Entry.UTF8:
            return((ConstantEntry) entry).getConstant();
        case Entry.NAMEANDTYPE:
            NameAndTypeEntry nte = (NameAndTypeEntry) entry;
            return new NameAndTypeKey(nte.getNameIndex(),
                nte.getDescriptorIndex());
        default:
            return null;
        }
    }

    /**
     * Base class key for entries with one ptr to another entry.
     */
    private static abstract class PtrKey {
        private final int _index;

        public PtrKey(int index) {
            _index = index;
        }

        public int hashCode() {
            return _index;
        }

        public boolean equals(Object other) {
            if (other == this)
                return true;
            if (other.getClass() != getClass())
                return false;
            return((PtrKey) other)._index == _index;
        }
    }

    /**
     * Key for string entries.
     */
    private static class StringKey extends PtrKey {
        public StringKey(int index) {
            super(index);
        }
    }

    /**
     * Key for class entries.
     */
    private static class ClassKey extends PtrKey {
        public ClassKey(int index) {
            super(index);
        }
    }

    /**
     * Base class key for entries with two ptr to other entries.
     */
    private static abstract class DoublePtrKey {
        private final int _index1;
        private final int _index2;

        public DoublePtrKey(int index1, int index2) {
            _index1 = index1;
            _index2 = index2;
        }

        public int hashCode() {
            return _index1 ^ _index2;
        }

        public boolean equals(Object other) {
            if (other == this)
                return true;
            if (other.getClass() != getClass())
                return false;

            DoublePtrKey key = (DoublePtrKey) other;
            return key._index1 == _index1 && key._index2 == _index2;
        }
    }

    /**
     * Key for name and type entries.
     */
    private static class NameAndTypeKey extends DoublePtrKey {
        public NameAndTypeKey(int index1, int index2) {
            super(index1, index2);
        }
    }

    /**
     * Key for field entries.
     */
    private static class FieldKey extends DoublePtrKey {
        public FieldKey(int index1, int index2) {
            super(index1, index2);
        }
    }

    /**
     * Key for method entries.
     */
    private static class MethodKey extends DoublePtrKey {
        public MethodKey(int index1, int index2) {
            super(index1, index2);
        }
    }

    /**
     * Key for interface method entries.
     */
    private static class InterfaceMethodKey extends DoublePtrKey {
        public InterfaceMethodKey(int index1, int index2) {
            super(index1, index2);
        }
    }
}


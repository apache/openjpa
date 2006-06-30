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

/**
 * Base type for all constant pool entries. Entries should generally be
 * considered immutable; modifying an entry directly can have dire
 * consequences, and often renders the resulting class file invalid.
 *  Entries cannot be shared among constant pools.
 * 
 * @author Abe White
 */
public abstract class Entry implements VisitAcceptor {
    public static final int UTF8 = 1;
    public static final int INT = 3;
    public static final int FLOAT = 4;
    public static final int LONG = 5;
    public static final int DOUBLE = 6;
    public static final int CLASS = 7;
    public static final int STRING = 8;
    public static final int FIELD = 9;
    public static final int METHOD = 10;
    public static final int INTERFACEMETHOD = 11;
    public static final int NAMEANDTYPE = 12;

    private ConstantPool _pool = null;
    private int _index = 0;

    /**
     * Read a single entry from the given bytecode stream and returns it.
     */
    public static Entry read(DataInput in) throws IOException {
        Entry entry = create(in.readUnsignedByte());
        entry.readData(in);
        return entry;
    }

    /**
     * Write the given entry to the given bytecode stream.
     */
    public static void write(Entry entry, DataOutput out) throws IOException {
        out.writeByte(entry.getType());
        entry.writeData(out);
    }

    /**
     * Create an entry based on its type code.
     */
    public static Entry create(int type) {
        switch (type) {
        case CLASS:
            return new ClassEntry();
        case FIELD:
            return new FieldEntry();
        case METHOD:
            return new MethodEntry();
        case INTERFACEMETHOD:
            return new InterfaceMethodEntry();
        case STRING:
            return new StringEntry();
        case INT:
            return new IntEntry();
        case FLOAT:
            return new FloatEntry();
        case LONG:
            return new LongEntry();
        case DOUBLE:
            return new DoubleEntry();
        case NAMEANDTYPE:
            return new NameAndTypeEntry();
        case UTF8:
            return new UTF8Entry();
        default:
            throw new IllegalArgumentException("type = " + type);
        }
    }

    /**
     * Return the type code for this entry type.
     */
    public abstract int getType();

    /**
     * Return true if this is a wide entry -- i.e. if it takes up two
     * places in the constant pool. Returns false by default.
     */
    public boolean isWide() {
        return false;
    }

    /**
     * Returns the constant pool containing this entry, or null if none.
     */
    public ConstantPool getPool() {
        return _pool;
    }

    /**
     * Returns the index of the entry in the owning constant pool, or 0.
     */
    public int getIndex() {
        return _index;
    }

    /**
     * This method is called after reading the entry type from bytecode.
     * It should read all the data for this entry from the given stream.
     */
    abstract void readData(DataInput in) throws IOException;

    /**
     * This method is called after writing the entry type to bytecode.
     * It should write all data for this entry to the given stream.
     */
    abstract void writeData(DataOutput out) throws IOException;

    /**
     * Subclasses must call this method before their state is mutated.
     */
    Object beforeModify() {
        if (_pool == null)
            return null;
        return _pool.getKey(this);
    }

    /**
     * Subclasses must call this method when their state is mutated.
     */
    void afterModify(Object key) {
        if (_pool != null)
            _pool.modifyEntry(key, this);
    }

    /**
     * Sets the owning pool of the entry.
     */
    void setPool(ConstantPool pool) {
        // attempting to overwrite current pool?
        if (_pool != null && pool != null && _pool != pool)
            throw new IllegalStateException("Entry already belongs to a pool");

        _pool = pool;
    }

    /**
     * Set the index of this entry within the pool.
     */
    void setIndex(int index) {
        _index = index;
    }
}

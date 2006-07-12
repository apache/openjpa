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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import serp.bytecode.visitor.BCVisitor;

/**
 * Entry containing indexes referencing a name and a descriptor. Used
 * to describe fields and methods of other classes referenced by opcodes.
 *
 * @author Abe White
 */
public class NameAndTypeEntry extends Entry {

    private int _nameIndex = 0;
    private int _descriptorIndex = 0;

    /**
     * Default constructor.
     */
    public NameAndTypeEntry() {
    }

    /**
     * Constructor.
     *
     * @param nameIndex the constant pool index of the
     * {@link UTF8Entry} containing the name of this entity
     * @param descriptorIndex the constant pool index of the
     * {@link UTF8Entry} containing the descriptor for this entity
     */
    public NameAndTypeEntry(int nameIndex, int descriptorIndex) {
        _nameIndex = nameIndex;
        _descriptorIndex = descriptorIndex;
    }

    public int getType() {
        return Entry.NAMEANDTYPE;
    }

    /**
     * Return the constant pool index of the {@link UTF8Entry}
     * containing the name of this entity.
     */
    public int getNameIndex() {
        return _nameIndex;
    }

    /**
     * Set the constant pool index of the {@link UTF8Entry}
     * containing the name of this entity.
     */
    public void setNameIndex(int nameIndex) {
        Object key = beforeModify();
        _nameIndex = nameIndex;
        afterModify(key);
    }

    /**
     * Return the name's referenced {@link UTF8Entry}. This method can only
     * be run for entries that have been added to a constant pool.
     */
    public UTF8Entry getNameEntry() {
        return (UTF8Entry) getPool().getEntry(_nameIndex);
    }

    /**
     * Return the constant pool index of the {@link UTF8Entry}
     * containing the descriptor for this entity.
     */
    public int getDescriptorIndex() {
        return _descriptorIndex;
    }

    /**
     * Set the constant pool index of a {@link UTF8Entry}
     * containing the descriptor for this entity.
     */
    public void setDescriptorIndex(int descriptorIndex) {
        Object key = beforeModify();
        _descriptorIndex = descriptorIndex;
        afterModify(key);
    }

    /**
     * Return the descriptor's referenced {@link UTF8Entry}. This method
     * can only be run for entries that have been added to a constant pool.
     */
    public UTF8Entry getDescriptorEntry() {
        return (UTF8Entry) getPool().getEntry(_descriptorIndex);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterNameAndTypeEntry(this);
        visit.exitNameAndTypeEntry(this);
    }

    void readData(DataInput in) throws IOException {
        _nameIndex = in.readUnsignedShort();
        _descriptorIndex = in.readUnsignedShort();
    }

    void writeData(DataOutput out) throws IOException {
        out.writeShort(_nameIndex);
        out.writeShort(_descriptorIndex);
    }
}

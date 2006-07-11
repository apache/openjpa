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

/**
 * Base class for field, method, and interface method constant pool
 * entries. All complex entries reference the {@link ClassEntry} of the
 * class that owns the entity and a {@link NameAndTypeEntry} describing
 * the entity.
 *
 * @author Abe White
 */
public abstract class ComplexEntry extends Entry {

    private int _classIndex = 0;
    private int _nameAndTypeIndex = 0;

    /**
     * Default constructor.
     */
    public ComplexEntry() {
    }

    /**
     * Constructor.
     *
     * @param classIndex       the constant pool index of the
     *                         {@link ClassEntry} describing the owner of this entity
     * @param nameAndTypeIndex the constant pool index of the
     *                         {@link NameAndTypeEntry} describing this entity
     */
    public ComplexEntry(int classIndex, int nameAndTypeIndex) {
        _classIndex = classIndex;
        _nameAndTypeIndex = nameAndTypeIndex;
    }

    /**
     * Return the constant pool index of the {@link ClassEntry} describing
     * the owning class of this entity. Defaults to 0.
     */
    public int getClassIndex() {
        return _classIndex;
    }

    /**
     * Set the constant pool index of the {@link ClassEntry} describing
     * the owning class of this entity.
     */
    public void setClassIndex(int classIndex) {
        Object key = beforeModify();
        _classIndex = classIndex;
        afterModify(key);
    }

    /**
     * Return the referenced {@link ClassEntry}. This method can only
     * be run for entries that have been added to a constant pool.
     */
    public ClassEntry getClassEntry() {
        return (ClassEntry) getPool().getEntry(_classIndex);
    }

    /**
     * Return the constant pool index of the {@link NameAndTypeEntry}
     * describing this entity.
     */
    public int getNameAndTypeIndex() {
        return _nameAndTypeIndex;
    }

    /**
     * Set the constant pool index of the {@link NameAndTypeEntry}
     * describing this entity.
     */
    public void setNameAndTypeIndex(int nameAndTypeIndex) {
        Object key = beforeModify();
        _nameAndTypeIndex = nameAndTypeIndex;
        afterModify(key);
    }

    /**
     * Return the referenced {@link NameAndTypeEntry}. This method can only
     * be run for entries that have been added to a constant pool.
     */
    public NameAndTypeEntry getNameAndTypeEntry() {
        return (NameAndTypeEntry) getPool().getEntry(_nameAndTypeIndex);
    }

    void readData(DataInput in) throws IOException {
        _classIndex = in.readUnsignedShort();
        _nameAndTypeIndex = in.readUnsignedShort();
    }

    void writeData(DataOutput out) throws IOException {
        out.writeShort(_classIndex);
        out.writeShort(_nameAndTypeIndex);
    }
}

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import serp.bytecode.lowlevel.ConstantPool;
import serp.bytecode.lowlevel.UTF8Entry;

/**
 * A local variable or local variable type.
 *
 * @author Abe White
 */
public abstract class Local implements BCEntity, InstructionPtr {

    private LocalTable _owner = null;
    private InstructionPtrStrategy _target = new InstructionPtrStrategy(this);

    private int _length = 0;
    private int _nameIndex = 0;
    private int _descriptorIndex = 0;
    private int _index = 0;

    Local(LocalTable owner) {
        _owner = owner;
    }

    /**
     * The owning table.
     */
    public LocalTable getTable() {
        return _owner;
    }

    void invalidate() {
        _owner = null;
    }

    //////////////////////////
    // Local index operations
    //////////////////////////

    /**
     * Get the local variable index of the current frame for this local.
     */
    public int getLocal() {
        return _index;
    }

    /**
     * Set the local variable index of the current frame for this local.
     */
    public void setLocal(int index) {
        _index = index;
    }

    /**
     * Return the parameter that this local corresponds to, or -1 if none.
     */
    public int getParam() {
        return getCode().getParamsIndex(getLocal());
    }

    /**
     * Set the method parameter that this local corresponds to.
     */
    public void setParam(int param) {
        setLocal(_owner.getCode().getLocalsIndex(param));
    }

    /**
     * Return the index into the code byte array at which this local starts.
     */
    public int getStartPc() {
        return _target.getByteIndex();
    }

    ////////////////////////////
    // Start, Length operations
    ////////////////////////////

    /**
     * Return the instruction marking the beginning of this local.
     */
    public Instruction getStart() {
        return _target.getTargetInstruction();
    }

    /**
     * Set the index into the code byte array at which this local starts.
     */
    public void setStartPc(int startPc) {
        _target.setByteIndex(startPc);
    }

    /**
     * Set the {@link Instruction} marking the beginning this local.
     * The instruction must already be a part of the method.
     * WARNING: if this instruction is deleted, the results are undefined.
     */
    public void setStart(Instruction instruction) {
        _target.setTargetInstruction(instruction);
    }

    public void updateTargets() {
        _target.updateTargets();
    }

    public void replaceTarget(Instruction oldTarget, Instruction newTarget) {
        _target.replaceTarget(oldTarget, newTarget);
    }

    /**
     * Get the number of bytes for which this local has a value in
     * the code byte array.
     */
    public int getLength() {
        return _length;
    }

    /**
     * Set the number of bytes for which this local has a value in
     * the code byte array.
     */
    public void setLength(int length) {
        _length = length;
    }

    /////////////////////////
    // Name, Type operations
    /////////////////////////

    /**
     * Return the {@link ConstantPool} index of the {@link UTF8Entry} that
     * describes the name of this local. Defaults to 0.
     */
    public int getNameIndex() {
        return _nameIndex;
    }

    /**
     * Set the {@link ConstantPool} index of the {@link UTF8Entry} that
     * describes the name of this local.
     */
    public void setNameIndex(int nameIndex) {
        _nameIndex = nameIndex;
    }

    /**
     * Return the name of this local, or null if unset.
     */
    public String getName() {
        if (getNameIndex() == 0)
            return null;
        return ((UTF8Entry) getPool().getEntry(getNameIndex())).getValue();
    }

    /**
     * Set the name of this inner local.
     */
    public void setName(String name) {
        if (name == null)
            setNameIndex(0);
        else
            setNameIndex(getPool().findUTF8Entry(name, true));
    }

    /**
     * Return the {@link ConstantPool} index of the {@link UTF8Entry} that
     * describes this local. Defaults to 0.
     */
    public int getTypeIndex() {
        return _descriptorIndex;
    }

    /**
     * Set the {@link ConstantPool} index of the {@link UTF8Entry} that
     * describes this local.
     */
    public void setTypeIndex(int index) {
        _descriptorIndex = index;
    }

    /**
     * Return the full name of the local's type, or null if unset.
     */
    public String getTypeName() {
        if (getTypeIndex() == 0)
            return null;

        UTF8Entry entry = (UTF8Entry) getPool().getEntry(getTypeIndex());
        return getProject().getNameCache().getExternalForm
            (entry.getValue(), false);
    }

    /**
     * Set the type of this local.
     */
    public void setType(String type) {
        if (type == null)
            setTypeIndex(0);
        else {
            type = getProject().getNameCache().getInternalForm(type, true);
            setTypeIndex(getPool().findUTF8Entry(type, true));
        }
    }

    ///////////////////////////
    // BCEntity implementation
    ///////////////////////////

    public Project getProject() {
        return _owner.getProject();
    }

    public ConstantPool getPool() {
        return _owner.getPool();
    }

    public ClassLoader getClassLoader() {
        return _owner.getClassLoader();
    }

    public boolean isValid() {
        return _owner != null;
    }

    //////////////////
    // I/O operations
    //////////////////

    void read(DataInput in) throws IOException {
        setStartPc(in.readUnsignedShort());
        setLength(in.readUnsignedShort());
        setNameIndex(in.readUnsignedShort());
        setTypeIndex(in.readUnsignedShort());
        setLocal(in.readUnsignedShort());
    }

    void write(DataOutput out) throws IOException {
        out.writeShort(getStartPc());
        out.writeShort(getLength());
        out.writeShort(getNameIndex());
        out.writeShort(getTypeIndex());
        out.writeShort(getLocal());
    }

    public Code getCode() {
        return _owner.getCode();
    }
}

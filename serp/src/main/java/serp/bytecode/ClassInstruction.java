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

import java.io.*;
import serp.bytecode.lowlevel.*;
import serp.bytecode.visitor.*;

/**
 * An instruction that takes as an argument a class to operate
 * on. Examples include <code>anewarray, checkcast, instance, anew</code>, etc.
 * 
 * @author Abe White
 */
public class ClassInstruction extends TypedInstruction {
    private int _index = 0;

    ClassInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        return getStackChange();
    }

    public int getStackChange() {
        if (getOpcode() == Constants.NEW)
            return 1;
        return 0;
    }

    int getLength() {
        return super.getLength() + 2;
    }

    /**
     * Return the {@link ConstantPool} index of the
     * {@link ClassEntry} describing the class for this instruction.
     */
    public int getTypeIndex() {
        return _index;
    }

    /**
     * Set the {@link ConstantPool} index of the
     * {@link ClassEntry} describing the class for this instruction.
     * 
     * @return this instruction, for method chaining
     */
    public ClassInstruction setTypeIndex(int index) {
        _index = index;
        return this;
    }

    public String getTypeName() {
        if (_index == 0)
            return null;

        ClassEntry entry = (ClassEntry) getPool().getEntry(_index);
        return getProject().getNameCache().getExternalForm
            (entry.getNameEntry().getValue(), false);
    }

    public TypedInstruction setType(String type) {
        if (type == null)
            setTypeIndex(0);
        else {
            type = getProject().getNameCache().getInternalForm(type, false);
            setTypeIndex(getPool().findClassEntry(type, true));
        }
        return this;
    }

    /**
     * ClassInstructions are equal if the type they reference is the same or
     * unset and if their opcodes are equal.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        if (!super.equalsInstruction(other))
            return false;

        String type = getTypeName();
        String otherType = ((ClassInstruction) other).getTypeName();
        return type == null || otherType == null || type.equals(otherType);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterClassInstruction(this);
        visit.exitClassInstruction(this);
    }

    void read(Instruction other) {
        super.read(other);
        setType(((ClassInstruction) other).getTypeName());
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setTypeIndex(in.readUnsignedShort());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeShort(getTypeIndex());
    }
}

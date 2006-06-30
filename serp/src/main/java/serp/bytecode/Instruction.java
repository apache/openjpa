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
import java.util.*;
import serp.bytecode.lowlevel.*;
import serp.bytecode.visitor.*;

/**
 * An opcode in a method of a class.
 * 
 * @author Abe White
 */
public class Instruction extends CodeEntry implements BCEntity, VisitAcceptor {
    private Code _owner = null;
    private int _opcode = Constants.NOP;

    Instruction(Code owner) {
        _owner = owner;
    }

    Instruction(Code owner, int opcode) {
        _owner = owner;
        _opcode = opcode;
    }

    /**
     * Return the code block that owns this instruction.
     */
    public Code getCode() {
        return _owner;
    }

    /**
     * Return the name of this instruction.
     */
    public String getName() {
        return Constants.OPCODE_NAMES[_opcode];
    }

    /**
     * Return the opcode this instruction represents.
     */
    public int getOpcode() {
        return _opcode;
    }

    /**
     * Set the opcode this instruction represents. For internal use only.
     * 
     * @return this instruction, for method chaining
     */
    Instruction setOpcode(int opcode) {
        _opcode = opcode;
        return this;
    }

    /**
     * Return the index in the method code byte block at which this opcode
     * starts. Note that this information may be out of date if the code
     * block has been modified since last read/written.
     */
    public int getByteIndex() {
        if (_owner != null)
            return _owner.getByteIndex(this);
        return 0;
    }

    /**
     * Return the line number of this instruction, or null if none. This
     * method is subject to the validity constraints of {@link #getByteIndex}.
     * 
     * @see LineNumberTable#getLineNumber(Instruction)
     */
    public LineNumber getLineNumber() {
        LineNumberTable table = _owner.getLineNumberTable(false);
        if (table == null)
            return null;
        return table.getLineNumber(this);
    }

    /**
     * Return the length in bytes of this opcode, including all arguments.
     * For many opcodes this method relies on an up-to-date byte index.
     */
    int getLength() {
        return 1;
    }

    /**
     * Return the logical number of stack positions changed by this
     * instruction. In other words, ignore weirdness with longs and doubles
     * taking two stack positions.
     */
    public int getLogicalStackChange() {
        return getStackChange();
    }

    /**
     * Return the number of stack positions this instruction pushes
     * or pops during its execution.
     * 
     * @return 0 if the stack is not affected by this instruction, a
     * positive number if it pushes onto the stack, and a negative
     * number if it pops from the stack
     */
    public int getStackChange() {
        return 0;
    }

    /**
     * Instructions are equal if their opcodes are the same. Subclasses
     * should override this method to perform a template comparison:
     * instructions should compare equal to other instructions of the same
     * type where the data is either the same or the data is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        return other.getOpcode() == getOpcode();
    }

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

    public void acceptVisit(BCVisitor visit) {
    }

    void invalidate() {
        _owner = null;
    }

    /**
     * Copy the given instruction data.
     */
    void read(Instruction orig) {
    }

    /**
     * Read the arguments for this opcode from the given stream.
     * This method should be overridden by opcodes that take arguments.
     */
    void read(DataInput in) throws IOException {
    }

    /**
     * Write the arguments for this opcode to the given stream.
     * This method should be overridden by opcodes that take arguments.
     */
    void write(DataOutput out) throws IOException {
    }
}

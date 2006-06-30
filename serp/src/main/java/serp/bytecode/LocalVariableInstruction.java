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

import serp.bytecode.visitor.*;

/**
 * An instruction that has an argument of an index into the
 * local variable table of the current frame. This includes most of the
 * <code>load</code> and <code>store</code> instructions.
 *  The local variable table size is fixed by the <code>maxLocals</code>
 * property of the code block. Long and double types take up 2 local variable
 * indexes.
 *  Parameter values to methods are loaded into the local variable table
 * prior to the execution of the first instruction. The 0 index of the
 * table is set to the instance of the class the method is being invoked on.
 * 
 * @author Abe White
 */
public abstract class LocalVariableInstruction extends TypedInstruction {
    private int _index = -1;

    LocalVariableInstruction(Code owner) {
        super(owner);
    }

    LocalVariableInstruction(Code owner, int opcode) {
        super(owner, opcode);
        calculateLocal();
    }

    public String getTypeName() {
        return null;
    }

    public TypedInstruction setType(String type) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the index of the local variable that this instruction operates on.
     */
    public int getLocal() {
        return _index;
    }

    /**
     * Set the index of the local variable that this instruction operates on.
     * 
     * @return this instruction, for method chaining
     */
    public LocalVariableInstruction setLocal(int index) {
        _index = index;
        calculateOpcode();

        return this;
    }

    /**
     * Return the parameter that this instruction operates on, or -1 if none.
     */
    public int getParam() {
        return getCode().getParamsIndex(getLocal());
    }

    /**
     * Set the method parameter that this instruction operates on. This
     * will set both the local index and the type of the instruction based
     * on the current method parameters.
     */
    public LocalVariableInstruction setParam(int param) {
        int local = getCode().getLocalsIndex(param);
        if (local != -1) {
            BCMethod method = getCode().getMethod();
            setType(method.getParamNames()[param]);
        }
        return setLocal(local);
    }

    /**
     * Return the local variable object this instruction
     * operates on, or null if none.
     * 
     * @see LocalVariableTable#getLocalVariable(int)
     */
    public LocalVariable getLocalVariable() {
        LocalVariableTable table = getCode().getLocalVariableTable(false);
        if (table == null)
            return null;
        return table.getLocalVariable(getLocal());
    }

    /**
     * Set the local variable object this instruction
     * operates on. This method will set both the type and local index
     * of this instruction from the given local variable.
     * 
     * @return this instruction, for method chaining
     */
    public LocalVariableInstruction setLocalVariable(LocalVariable local) {
        if (local == null)
            return setLocal(-1);
        else {
            String type = local.getTypeName();
            if (type != null)
                setType(type);
            return setLocal(local.getLocal());
        }
    }

    /**
     * Two local variable instructions are equal if the local index they
     * reference is equal or if either index is 0/unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (this == other)
            return true;
        if (!getClass().equals(other.getClass()))
            return false;

        LocalVariableInstruction ins = (LocalVariableInstruction) other;
        int index = getLocal();
        int insIndex = ins.getLocal();
        return index == -1 || insIndex == -1 || index == insIndex;
    }

    void read(Instruction orig) {
        super.read(orig);
        setLocal(((LocalVariableInstruction) orig).getLocal());
    }

    /**
     * Subclasses with variable opcodes can use this method to be
     * notified that information possibly affecting the opcode has been changed.
     */
    void calculateOpcode() {
    }

    /**
     * Subclasses can use this method to calculate
     * the locals index based on their opcode.
     */
    void calculateLocal() {
    }
}

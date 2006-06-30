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
import serp.bytecode.visitor.*;

/**
 * The <code>wide</code> instruction, which is used to allow other
 * instructions to index values beyond what they can normally index baed
 * on the length of their arguments.
 * 
 * @author Abe White
 */
public class WideInstruction extends LocalVariableInstruction {
    private static final Class[][] _mappings = new Class[][] {
        { byte.class, int.class }, { boolean.class, int.class },
        { char.class, int.class }, { short.class, int.class },
        { void.class, int.class }, };

    private int _ins = Constants.NOP;
    private int _inc = -1;

    WideInstruction(Code owner) {
        super(owner, Constants.WIDE);
    }

    int getLength() {
        // opcode, ins, index
        int length = super.getLength() + 1 + 2;

        // increment
        if (getInstruction() == Constants.IINC)
            length += 2;

        return length;
    }

    public int getStackChange() {
        switch (getInstruction()) {
        case Constants.ILOAD:
        case Constants.FLOAD:
        case Constants.ALOAD:
            return 1;
        case Constants.LLOAD:
        case Constants.DLOAD:
            return 2;
        case Constants.ISTORE:
        case Constants.FSTORE:
        case Constants.ASTORE:
            return -1;
        case Constants.LSTORE:
        case Constants.DSTORE:
            return -2;
        default:
            return 0;
        }
    }

    public int getLogicalStackChange() {
        switch (getInstruction()) {
        case Constants.ILOAD:
        case Constants.FLOAD:
        case Constants.ALOAD:
        case Constants.LLOAD:
        case Constants.DLOAD:
            return 1;
        case Constants.ISTORE:
        case Constants.FSTORE:
        case Constants.ASTORE:
        case Constants.LSTORE:
        case Constants.DSTORE:
            return -1;
        default:
            return 0;
        }
    }

    public String getTypeName() {
        switch (getInstruction()) {
        case Constants.ILOAD:
        case Constants.ISTORE:
            return int.class.getName();
        case Constants.LLOAD:
        case Constants.LSTORE:
            return long.class.getName();
        case Constants.FLOAD:
        case Constants.FSTORE:
            return float.class.getName();
        case Constants.DLOAD:
        case Constants.DSTORE:
            return double.class.getName();
        case Constants.ALOAD:
        case Constants.ASTORE:
            return Object.class.getName();
        default:
            return null;
        }
    }

    public TypedInstruction setType(String type) {
        type = mapType(type, _mappings, true);

        switch (getInstruction()) {
        case Constants.ILOAD:
        case Constants.LLOAD:
        case Constants.FLOAD:
        case Constants.DLOAD:
        case Constants.ALOAD:
            if (type == null)
                throw new IllegalStateException();
            switch (type.charAt(0)) {
            case 'i':
                return(TypedInstruction) setInstruction(Constants.ILOAD);
            case 'l':
                return(TypedInstruction) setInstruction(Constants.LLOAD);
            case 'f':
                return(TypedInstruction) setInstruction(Constants.FLOAD);
            case 'd':
                return(TypedInstruction) setInstruction(Constants.DLOAD);
            default:
                return(TypedInstruction) setInstruction(Constants.ALOAD);
            }
        case Constants.ISTORE:
        case Constants.LSTORE:
        case Constants.FSTORE:
        case Constants.DSTORE:
        case Constants.ASTORE:
            if (type == null)
                throw new IllegalStateException();
            switch (type.charAt(0)) {
            case 'i':
                return(TypedInstruction) setInstruction(Constants.ISTORE);
            case 'l':
                return(TypedInstruction) setInstruction(Constants.LSTORE);
            case 'f':
                return(TypedInstruction) setInstruction(Constants.FSTORE);
            case 'd':
                return(TypedInstruction) setInstruction(Constants.DSTORE);
            default:
                return(TypedInstruction) setInstruction(Constants.ASTORE);
            }
        default:
            if (type != null)
                throw new IllegalStateException("Augmented instruction not "
                    + "typed");
            return this;
        }
    }

    /**
     * Return the opcode of the instruction to modify; this will return one
     * of the constants defined in {@link Constants}.
     */
    public int getInstruction() {
        return _ins;
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     */
    public WideInstruction setInstruction(Instruction ins) {
        if (ins == null)
            return setInstruction(Constants.NOP);

        setInstruction(ins.getOpcode());
        if (ins instanceof IIncInstruction)
            setIncrement(((IIncInstruction) ins).getIncrement());

        return this;
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     */
    public WideInstruction setInstruction(int opcode) {
        _ins = opcode;
        return this;
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction iinc() {
        return setInstruction(Constants.IINC);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction ret() {
        return setInstruction(Constants.RET);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction iload() {
        return setInstruction(Constants.ILOAD);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction fload() {
        return setInstruction(Constants.FLOAD);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction aload() {
        return setInstruction(Constants.ALOAD);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction lload() {
        return setInstruction(Constants.LLOAD);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction dload() {
        return setInstruction(Constants.DLOAD);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction istore() {
        return setInstruction(Constants.ISTORE);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction fstore() {
        return setInstruction(Constants.FSTORE);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction astore() {
        return setInstruction(Constants.ASTORE);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction lstore() {
        return setInstruction(Constants.LSTORE);
    }

    /**
     * Set the type of instruction this wide instruction modifies.
     * 
     * @return this instruction, for method chaining
     */
    public WideInstruction dstore() {
        return setInstruction(Constants.DSTORE);
    }

    /**
     * Return the increment for this instruction if it augments IINC, or -1
     * if unset.
     */
    public int getIncrement() {
        return _inc;
    }

    /**
     * Set the increment on this instruction if it augments IINC.
     * 
     * @return this Instruction, for method chaining
     */
    public WideInstruction setIncrement(int val) {
        _inc = val;
        return this;
    }

    /**
     * WideInstructions are equal if the instruction they augment is the same
     * or unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        if (!super.equalsInstruction(other))
            return false;
        if (!(other instanceof WideInstruction))
            return false;

        WideInstruction ins = (WideInstruction) other;

        int code = getInstruction();
        int otherCode = ins.getInstruction();
        if (code != otherCode)
            return false;

        if (code == Constants.IINC) {
            int inc = getIncrement();
            int otherInc = ins.getIncrement();
            return inc == -1 || otherInc == -1 || inc == otherInc;
        }
        return true;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterWideInstruction(this);
        visit.exitWideInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);
        setInstruction(((WideInstruction) orig).getInstruction());
    }

    void read(DataInput in) throws IOException {
        super.read(in);

        setInstruction(in.readUnsignedByte());
        setLocal(in.readUnsignedShort());
        if (getInstruction() == Constants.IINC)
            setIncrement(in.readUnsignedShort());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeByte(getInstruction());
        out.writeShort(getLocal());
        if (getInstruction() == Constants.IINC)
            out.writeShort(getIncrement());
    }
}

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

import java.util.*;
import serp.bytecode.visitor.*;

/**
 * One of the math operations defined in the {@link Constants} interface.
 * Changing the type or operation of the instruction will automatically
 * update the underlying opcode.
 * 
 * @author Abe White
 */
public class MathInstruction extends TypedInstruction {
    private static final Class[][] _mappings = new Class[][] {
        { byte.class, int.class }, { boolean.class, int.class },
        { char.class, int.class }, { short.class, int.class },
        { void.class, int.class }, { Object.class, int.class }, };

    private int _op = -1;
    private String _type = null;

    MathInstruction(Code owner) {
        super(owner);
    }

    MathInstruction(Code owner, int opcode) {
        super(owner, opcode);
        _op = getOperation();
    }

    public int getStackChange() {
        int op = getOperation();
        if (op == Constants.MATH_NEG || getOpcode() == Constants.NOP)
            return 0;

        String type = getTypeName();
        if (long.class.getName().equals(type)
            || double.class.getName().equals(type)) {
            switch (getOpcode()) {
                case(Constants.LSHL):
                case(Constants.LSHR):
                case(Constants.LUSHR):
                    return -1;
                default:
                    return -2;
            }
        }
        return -1;
    }

    public int getLogicalStackChange() {
        int op = getOperation();
        if (op == Constants.MATH_NEG || getOpcode() == Constants.NOP)
            return 0;
        return -1;
    }

    public String getTypeName() {
        switch (getOpcode()) {
        case Constants.IADD:
        case Constants.ISUB:
        case Constants.IMUL:
        case Constants.IDIV:
        case Constants.IREM:
        case Constants.INEG:
        case Constants.ISHL:
        case Constants.ISHR:
        case Constants.IUSHR:
        case Constants.IAND:
        case Constants.IOR:
        case Constants.IXOR:
            return int.class.getName();
        case Constants.LADD:
        case Constants.LSUB:
        case Constants.LMUL:
        case Constants.LDIV:
        case Constants.LREM:
        case Constants.LNEG:
        case Constants.LSHL:
        case Constants.LSHR:
        case Constants.LUSHR:
        case Constants.LAND:
        case Constants.LOR:
        case Constants.LXOR:
            return long.class.getName();
        case Constants.FADD:
        case Constants.FSUB:
        case Constants.FMUL:
        case Constants.FDIV:
        case Constants.FREM:
        case Constants.FNEG:
            return float.class.getName();
        case Constants.DADD:
        case Constants.DSUB:
        case Constants.DMUL:
        case Constants.DDIV:
        case Constants.DREM:
        case Constants.DNEG:
            return double.class.getName();
        default:
            return _type;
        }
    }

    public TypedInstruction setType(String type) {
        type = mapType(type, _mappings, true);

        // if an invalid type or op, revert to nop
        if (type == null || _op < 0) {
            _type = type;
            return(TypedInstruction) setOpcode(Constants.NOP);
        }

        // valid opcode, unset saved type
        _type = null;

        switch (type.charAt(0)) {
        case 'i':
            return(TypedInstruction) setOpcode(_op);
        case 'l':
            return(TypedInstruction) setOpcode(_op + 1);
        case 'f':
            return(TypedInstruction) setOpcode(_op + 2);
        case 'd':
            return(TypedInstruction) setOpcode(_op + 3);
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Set the math operation to be performed. This should be one of the
     * math constant defined in {@link Constants}.
     * 
     * @return this instruction, for method chaining
     */
    public MathInstruction setOperation(int operation) {
        _op = operation;

        // this calculates the opcode
        setType(getTypeName());
        return this;
    }

    /**
     * Return the operation for this math instruction; will be one of the
     * math constant defined in {@link Constants}, or -1 if unset.
     */
    public int getOperation() {
        switch (getOpcode()) {
        case Constants.IADD:
        case Constants.LADD:
        case Constants.FADD:
        case Constants.DADD:
            return Constants.MATH_ADD;
        case Constants.ISUB:
        case Constants.LSUB:
        case Constants.FSUB:
        case Constants.DSUB:
            return Constants.MATH_SUB;
        case Constants.IMUL:
        case Constants.LMUL:
        case Constants.FMUL:
        case Constants.DMUL:
            return Constants.MATH_MUL;
        case Constants.IDIV:
        case Constants.LDIV:
        case Constants.FDIV:
        case Constants.DDIV:
            return Constants.MATH_DIV;
        case Constants.IREM:
        case Constants.LREM:
        case Constants.FREM:
        case Constants.DREM:
            return Constants.MATH_REM;
        case Constants.INEG:
        case Constants.LNEG:
        case Constants.FNEG:
        case Constants.DNEG:
            return Constants.MATH_NEG;
        case Constants.ISHL:
        case Constants.LSHL:
            return Constants.MATH_SHL;
        case Constants.ISHR:
        case Constants.LSHR:
            return Constants.MATH_SHR;
        case Constants.IUSHR:
        case Constants.LUSHR:
            return Constants.MATH_USHR;
        case Constants.IAND:
        case Constants.LAND:
            return Constants.MATH_AND;
        case Constants.IOR:
        case Constants.LOR:
            return Constants.MATH_OR;
        case Constants.IXOR:
        case Constants.LXOR:
            return Constants.MATH_XOR;
        default:
            return _op;
        }
    }

    /**
     * MathInstructions are equal if they have the same operation and type,
     * or the operation and type of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (this == other)
            return true;
        if (!(other instanceof MathInstruction))
            return false;

        MathInstruction ins = (MathInstruction) other;

        int op = getOperation();
        int otherOp = ins.getOperation();
        boolean opEq = op == -1 || otherOp == -1 || op == otherOp;

        String type = getTypeName();
        String otherType = ins.getTypeName();
        boolean typeEq = type == null || otherType == null
            || type.equals(otherType);

        return opEq && typeEq;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterMathInstruction(this);
        visit.exitMathInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);
        MathInstruction ins = (MathInstruction) orig;
        _type = ins._type;
        _op = ins._op;
    }
}

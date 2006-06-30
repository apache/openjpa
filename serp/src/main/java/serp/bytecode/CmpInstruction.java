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
 * An instruction comparing two stack values. Examples include
 * <code>lcmp, fcmpl</code>, etc.
 * 
 * @author Abe White
 */
public class CmpInstruction extends TypedInstruction {
    private static Class[][] _mappings = new Class[][] {
        { int.class, long.class }, { byte.class, long.class },
        { char.class, long.class }, { short.class, long.class },
        { boolean.class, long.class }, { void.class, long.class },
        { Object.class, long.class }, };

    CmpInstruction(Code owner) {
        super(owner);
    }

    CmpInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        switch (getOpcode()) {
        case Constants.NOP:
            return 0;
        default:
            return -1;
        }
    }

    public int getStackChange() {
        switch (getOpcode()) {
        case Constants.LCMP:
        case Constants.DCMPL:
        case Constants.DCMPG:
            return -3;
        case Constants.NOP:
            return 0;
        default:
            return -1;
        }
    }

    public String getTypeName() {
        switch (getOpcode()) {
        case Constants.LCMP:
            return long.class.getName();
        case Constants.FCMPL:
        case Constants.FCMPG:
            return float.class.getName();
        case Constants.DCMPL:
        case Constants.DCMPG:
            return double.class.getName();
        default:
            return null;
        }
    }

    public TypedInstruction setType(String type) {
        type = mapType(type, _mappings, true);

        if (type == null)
            return(TypedInstruction) setOpcode(Constants.NOP);

        int opcode = getOpcode();
        switch (type.charAt(0)) {
        case 'l':
            return(TypedInstruction) setOpcode(Constants.LCMP);
        case 'f':
            if (opcode == Constants.FCMPL || opcode == Constants.DCMPL)
                return(TypedInstruction) setOpcode(Constants.FCMPL);
            return(TypedInstruction) setOpcode(Constants.FCMPG);
        case 'd':
            if (opcode == Constants.FCMPL || opcode == Constants.DCMPL)
                return(TypedInstruction) setOpcode(Constants.DCMPL);
            return(TypedInstruction) setOpcode(Constants.DCMPG);
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Return the number that will be placed on the stack if this instruction
     * is of type float or double and one of the operands is NaN. For
     * FCMPG or DCMPG, this value will be 1; for FCMPL or DCMPL this value
     * will be -1. For LCMP or if the type is unset, this value will be 0.
     */
    public int getNaNValue() {
        switch (getOpcode()) {
        case Constants.FCMPL:
        case Constants.DCMPL:
            return -1;
        case Constants.FCMPG:
        case Constants.DCMPG:
            return 1;
        default:
            return 0;
        }
    }

    /**
     * Set the number that will be placed on the stack if this instruction
     * is of type float or double and one of the operands is NaN. For
     * FCMPG or DCMPG, this value should be 1; for FCMPL or DCMPL this value
     * should be -1. For LCMP, this value should be 0.
     * 
     * @return this instruction, for method chaining
     */
    public CmpInstruction setNaNValue(int nan) {
        switch (getOpcode()) {
        case Constants.FCMPL:
        case Constants.FCMPG:
            if (nan == 1)
                setOpcode(Constants.FCMPG);
            else if (nan == -1)
                setOpcode(Constants.FCMPL);
            else
                throw new IllegalArgumentException("Invalid nan for type");
        case Constants.DCMPL:
        case Constants.DCMPG:
            if (nan == 1)
                setOpcode(Constants.DCMPG);
            else if (nan == -1)
                setOpcode(Constants.DCMPL);
            else
                throw new IllegalArgumentException("Invalid nan for type");
        default:
            if (nan != 0)
                throw new IllegalArgumentException("Invalid nan for type");
        }
        return this;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterCmpInstruction(this);
        visit.exitCmpInstruction(this);
    }
}

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

import serp.bytecode.visitor.BCVisitor;

/**
 * Represents an instruction that manipulates the stack of the current
 * frame. Using the {@link #setType} methods is a hint about the type being
 * manipulated that might cause this instruction to use the wide version
 * of the opcode it represents(if manipulating a long or double). This
 * saves the developer from having to decide at compile time whether to
 * use <code>pop</code> or <code>pop2</code>, etc.
 *
 * @author Abe White
 */
public class StackInstruction extends TypedInstruction {

    StackInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        return getStackChange();
    }

    public int getStackChange() {
        switch (getOpcode()) {
            case Constants.POP:
                return -1;
            case Constants.POP2:
                return -2;
            case Constants.DUP:
            case Constants.DUPX1:
            case Constants.DUPX2:
                return 1;
            case Constants.DUP2:
            case Constants.DUP2X1:
            case Constants.DUP2X2:
                return 2;
            default:
                return 0;
        }
    }

    /**
     * This method will always return null; use {@link #isWide} to determine
     * if this is pop2, dup2, etc.
     */
    public String getTypeName() {
        return null;
    }

    public TypedInstruction setType(String type) {
        type = getProject().getNameCache().getExternalForm(type, false);
        return setWide(long.class.getName().equals(type)
            || double.class.getName().equals(type));
    }

    /**
     * Return whether to use the wide form of the current opcode for
     * operations on longs or doubles.
     */
    public boolean isWide() {
        switch (getOpcode()) {
            case Constants.POP2:
            case Constants.DUP2:
            case Constants.DUP2X1:
            case Constants.DUP2X2:
                return true;
            default:
                return false;
        }
    }

    /**
     * Set whether to use the wide form of the current opcode for operations
     * on longs or doubles.
     *
     * @return this instruction, for method chaining
     */
    public StackInstruction setWide(boolean wide) {
        switch (getOpcode()) {
            case Constants.POP:
                if (wide) setOpcode(Constants.POP2);
                break;
            case Constants.POP2:
                if (!wide) setOpcode(Constants.POP);
                break;
            case Constants.DUP:
                if (wide) setOpcode(Constants.DUP2);
                break;
            case Constants.DUP2:
                if (!wide) setOpcode(Constants.DUP);
                break;
            case Constants.DUPX1:
                if (wide) setOpcode(Constants.DUP2X1);
                break;
            case Constants.DUP2X1:
                if (!wide) setOpcode(Constants.DUPX1);
                break;
            case Constants.DUPX2:
                if (wide) setOpcode(Constants.DUP2X2);
                break;
            case Constants.DUP2X2:
                if (!wide) setOpcode(Constants.DUPX2);
                break;
        }
        return this;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterStackInstruction(this);
        visit.exitStackInstruction(this);
    }
}

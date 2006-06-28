/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

import serp.bytecode.visitor.*;

import java.io.*;

import java.util.*;


/**
 *  <p>An instruction that specifies a position in the code block to jump to.
 *  Examples include <code>go2, jsr</code>, etc.</p>
 *
 *  @author Abe White
 */
public class JumpInstruction extends Instruction implements InstructionPtr {
    private InstructionPtrStrategy _target = new InstructionPtrStrategy(this);

    JumpInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        return getStackChange();
    }

    public int getStackChange() {
        if (getOpcode() == Constants.JSR) {
            return 1;
        }

        return 0;
    }

    int getLength() {
        switch (getOpcode()) {
        case Constants.GOTOW:
        case Constants.JSRW:
            return super.getLength() + 4;

        default:
            return super.getLength() + 2;
        }
    }

    /**
      *  Get the current target instruction to jump to, if it has been set.
     */
    public Instruction getTarget() {
        return _target.getTargetInstruction();
    }

    /**
      *  Set the instruction to jump to; the instruction must already be
     *  added to the code block.
      *
     *  @return this instruction, for method chaining
     */
    public JumpInstruction setTarget(Instruction instruction) {
        _target.setTargetInstruction(instruction);

        return this;
    }

    /**
      *  JumpInstructions are equal if they represent the same operation and
     *  the instruction they jump to is the
     *  same, or if the jump Instruction of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (this == other) {
            return true;
        }

        if (!super.equalsInstruction(other)) {
            return false;
        }

        Instruction target = ((JumpInstruction) other).getTarget();

        return ((target == null) || (getTarget() == null) ||
        (target == getTarget()));
    }

    public void updateTargets() {
        _target.updateTargets();
    }

    public void replaceTarget(Instruction oldTarget, Instruction newTarget) {
        _target.replaceTarget(oldTarget, newTarget);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterJumpInstruction(this);
        visit.exitJumpInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);
        _target.setByteIndex(((JumpInstruction) orig)._target.getByteIndex());
    }

    void read(DataInput in) throws IOException {
        super.read(in);

        switch (getOpcode()) {
        case Constants.GOTOW:
        case Constants.JSRW:
            setOffset(in.readInt());

            break;

        default:
            setOffset(in.readShort());
        }
    }

    void write(DataOutput out) throws IOException {
        super.write(out);

        switch (getOpcode()) {
        case Constants.GOTOW:
        case Constants.JSRW:
            out.writeInt(getOffset());

            break;

        default:
            out.writeShort(getOffset());
        }
    }

    void calculateOpcode() {
        int offset;

        switch (getOpcode()) {
        case Constants.GOTO:
        case Constants.GOTOW:
            offset = getOffset();

            if (offset < (2 << 16)) {
                setOpcode(Constants.GOTO);
            } else {
                setOpcode(Constants.GOTOW);
            }

            break;

        case Constants.JSR:
        case Constants.JSRW:
            offset = getOffset();

            if (offset < (2 << 16)) {
                setOpcode(Constants.JSR);
            } else {
                setOpcode(Constants.JSRW);
            }

            break;
        }
    }

    public void setOffset(int offset) {
        _target.setByteIndex(getByteIndex() + offset);
        calculateOpcode();
    }

    public int getOffset() {
        return _target.getByteIndex() - getByteIndex();
    }
}

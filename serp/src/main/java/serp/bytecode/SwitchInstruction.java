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

import java.io.DataInput;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 *  Contains functionality common to the different switch types
 *  (TableSwitch and LookupSwitch).
 *
 *  @author Eric Lindauer
 */
public abstract class SwitchInstruction extends JumpInstruction {
    private List _cases = new LinkedList();

    public SwitchInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    /**
     *  Returns the current byte offsets for the different
     *  switch cases in this Instruction.
     */
    public int[] getOffsets() {
        int bi = getByteIndex();
        int[] offsets = new int[_cases.size()];

        for (int i = 0; i < offsets.length; i++)
            offsets[i] = ((InstructionPtrStrategy) _cases.get(i)).getByteIndex() -
                bi;

        return offsets;
    }

    /**
     *  Sets the offsets for the instructions representing the different
     *  switch statement cases.  WARNING: these offsets will not be changed
     *  in the event that the code is modified following this call. It is
     *  typically a good idea to follow this call with a call to updateTargets
     *  as soon as the instructions at the given offsets are valid, at which
     *  point the Instructions themselves will be used as the targets and the
     *  offsets will be updated as expected.
     */
    public void setOffsets(int[] offsets) {
        int bi = getByteIndex();
        _cases.clear();

        for (int i = 0; i < offsets.length; i++) {
            InstructionPtrStrategy next = new InstructionPtrStrategy(this);
            next.setByteIndex(offsets[i] + bi);
            _cases.add(next);
        }
    }

    public int countTargets() {
        return _cases.size();
    }

    int getLength() {
        // don't call super.getLength(), cause JumpInstruction will return
        // value assuming this is an 'if' or 'goto' instruction
        int length = 1;

        // make the first byte of the 'default' a multiple of 4 from the
        // start of the method
        int byteIndex = getByteIndex() + 1;

        for (; (byteIndex % 4) != 0; byteIndex++, length++)
            ;

        return length;
    }

    /**
     *  Synonymous with {@link #getTarget}.
     */
    public Instruction getDefaultTarget() {
        return getTarget();
    }

    /**
     *  Synonymous with {@link #getOffset}.
     */
    public int getDefaultOffset() {
        return getOffset();
    }

    /**
     *  Synonymous with {@link #setOffset}.
     */
    public SwitchInstruction setDefaultOffset(int offset) {
        setOffset(offset);

        return this;
    }

    /**
     *  Synonymous with {@link #setTarget}.
     */
    public SwitchInstruction setDefaultTarget(Instruction ins) {
        return (SwitchInstruction) setTarget(ins);
    }

    /**
     *  Return the targets for this switch, or empty array if not set.
     */
    public Instruction[] getTargets() {
        Instruction[] result = new Instruction[_cases.size()];

        for (int i = 0; i < _cases.size(); i++)
            result[i] = ((InstructionPtrStrategy) _cases.get(i)).getTargetInstruction();

        return result;
    }

    /**
     *  Set the jump points for this switch.
     *
     *  @return this instruction, for method chaining
     */
    public SwitchInstruction setTargets(Instruction[] targets) {
        _cases.clear();

        if (targets != null) {
            for (int i = 0; i < targets.length; i++)
                addTarget(targets[i]);
        }

        return this;
    }

    /**
     *  Add a target to this switch.
     *
     *  @return this instruction, for method chaining
     */
    public SwitchInstruction addTarget(Instruction target) {
        _cases.add(new InstructionPtrStrategy(this, target));

        return this;
    }

    public int getLogicalStackChange() {
        return getStackChange();
    }

    public int getStackChange() {
        return -1;
    }

    public void updateTargets() {
        super.updateTargets();

        for (Iterator itr = _cases.iterator(); itr.hasNext();)
            ((InstructionPtrStrategy) itr.next()).updateTargets();
    }

    public void replaceTarget(Instruction oldTarget, Instruction newTarget) {
        super.replaceTarget(oldTarget, newTarget);

        for (Iterator itr = _cases.iterator(); itr.hasNext();)
            ((InstructionPtrStrategy) itr.next()).replaceTarget(oldTarget,
                newTarget);
    }

    void read(Instruction orig) {
        super.read(orig);

        SwitchInstruction ins = (SwitchInstruction) orig;
        _cases.clear();

        for (Iterator itr = ins._cases.iterator(); itr.hasNext();) {
            InstructionPtrStrategy incoming = (InstructionPtrStrategy) itr.next();
            InstructionPtrStrategy next = new InstructionPtrStrategy(this);
            next.setByteIndex(incoming.getByteIndex());
            _cases.add(next);
        }
    }

    void clearTargets() {
        _cases.clear();
    }

    void readTarget(DataInput in) throws IOException {
        InstructionPtrStrategy next = new InstructionPtrStrategy(this);
        next.setByteIndex(getByteIndex() + in.readInt());
        _cases.add(next);
    }

    /**
     *  Set the match-jumppt pairs for this switch.
     *
     *  @return this instruction, for method chaining
     */
    public SwitchInstruction setCases(int[] matches, Instruction[] targets) {
        setMatches(matches);
        setTargets(targets);

        return this;
    }

    public SwitchInstruction setMatches(int[] matches) {
        clearMatches();

        for (int i = 0; i < matches.length; i++)
            addMatch(matches[i]);

        return this;
    }

    /**
     *  Add a case to this switch.
     *
     *  @return this instruction, for method chaining
     */
    public SwitchInstruction addCase(int match, Instruction target) {
        addMatch(match);
        addTarget(target);

        return this;
    }

    public abstract SwitchInstruction addMatch(int match);

    public abstract int[] getMatches();

    abstract void clearMatches();
}

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
 *  <p>The <code>tableswitch</code> instruction.</p>
 *
 *  @author Abe White
 */
public class TableSwitchInstruction extends JumpInstruction {
    // case info
    private int _low = 0;
    private int _high = 0;
    private List _cases = new LinkedList();

    TableSwitchInstruction(Code owner) {
        super(owner, Constants.TABLESWITCH);
    }

    /**
     *  Returns the current byte offsets for the different
     *  switch cases in this Instruction.
     */
    public int[] getOffsets() {
        int bi = getByteIndex();
        int[] offsets = new int[_cases.size()];

        for (int i = 0; i < _cases.size(); i++)
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

    int getLength() {
        // don't call super
        int length = 1;

        // make the first byte of the 'default' a multiple of 4 from the
        // start of the method
        int byteIndex = getByteIndex() + 1;

        for (; (byteIndex % 4) != 0; byteIndex++, length++)
            ;

        // default, low, high
        length += 12;

        // offsets
        length += (4 * _cases.size());

        return length;
    }

    /**
     *  Synonymous with {@link #getTarget}.
     */
    public Instruction getDefaultTarget() {
        return getTarget();
    }

    /**
     *  Synonymous with {@link #setTarget}.
     */
    public TableSwitchInstruction setDefaultTarget(Instruction ins) {
        return (TableSwitchInstruction) setTarget(ins);
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
    public TableSwitchInstruction setDefaultOffset(int offset) {
        setOffset(offset);

        return this;
    }

    public int getLow() {
        return _low;
    }

    public TableSwitchInstruction setLow(int low) {
        _low = low;

        return this;
    }

    public int getHigh() {
        return _high;
    }

    public TableSwitchInstruction setHigh(int high) {
        _high = high;

        return this;
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
    public TableSwitchInstruction setTargets(Instruction[] targets) {
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
    public TableSwitchInstruction addTarget(Instruction target) {
        _cases.add(new InstructionPtrStrategy(this, target));

        return this;
    }

    public int getStackChange() {
        return -1;
    }

    private Instruction findTarget(int jumpByteIndex, List inss) {
        Instruction ins;

        for (Iterator itr = inss.iterator(); itr.hasNext();) {
            ins = (Instruction) itr.next();

            if (ins.getByteIndex() == jumpByteIndex) {
                return ins;
            }
        }

        return null;
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

    public void acceptVisit(BCVisitor visit) {
        visit.enterTableSwitchInstruction(this);
        visit.exitTableSwitchInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);

        TableSwitchInstruction ins = (TableSwitchInstruction) orig;
        setLow(ins.getLow());
        setHigh(ins.getHigh());

        for (Iterator itr = ins._cases.iterator(); itr.hasNext();) {
            InstructionPtrStrategy incoming = (InstructionPtrStrategy) itr.next();
            InstructionPtrStrategy next = new InstructionPtrStrategy(this);
            next.setByteIndex(incoming.getByteIndex());
            _cases.add(next);
        }
    }

    void read(DataInput in) throws IOException {
        // don't call super
        int bi = getByteIndex();

        for (int byteIndex = bi + 1; (byteIndex % 4) != 0; byteIndex++)
            in.readByte();

        setOffset(in.readInt());
        setLow(in.readInt());
        setHigh(in.readInt());

        _cases.clear();

        for (int i = 0; i < (_high - _low + 1); i++) {
            InstructionPtrStrategy next = new InstructionPtrStrategy(this);
            next.setByteIndex(bi + in.readInt());
            _cases.add(next);
        }
    }

    void write(DataOutput out) throws IOException {
        // don't call super
        int bi = getByteIndex();

        for (int byteIndex = bi + 1; (byteIndex % 4) != 0; byteIndex++)
            out.writeByte(0);

        out.writeInt(getOffset());
        out.writeInt(getLow());
        out.writeInt(getHigh());

        for (Iterator itr = _cases.iterator(); itr.hasNext();)
            out.writeInt(((InstructionPtrStrategy) itr.next()).getByteIndex() -
                bi);
    }
}

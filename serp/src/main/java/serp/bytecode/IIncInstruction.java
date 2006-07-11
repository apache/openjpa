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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import serp.bytecode.visitor.BCVisitor;

/**
 * The <code>iinc</code> instruction.
 *
 * @author Abe White
 */
public class IIncInstruction extends LocalVariableInstruction {

    private int _inc = 0;

    IIncInstruction(Code owner) {
        super(owner, Constants.IINC);
    }

    int getLength() {
        return super.getLength() + 2;
    }

    /**
     * Return the increment for this IINC instruction.
     */
    public int getIncrement() {
        return _inc;
    }

    /**
     * Set the increment on this IINC instruction.
     *
     * @return this Instruction, for method chaining
     */
    public IIncInstruction setIncrement(int val) {
        _inc = val;
        return this;
    }

    public boolean equalsInstruction(Instruction other) {
        if (this == other)
            return true;
        if (!(other instanceof IIncInstruction))
            return false;
        if (!super.equalsInstruction(other))
            return false;

        IIncInstruction ins = (IIncInstruction) other;
        return (getIncrement() == 0 || ins.getIncrement() == 0
            || getIncrement() == ins.getIncrement());
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterIIncInstruction(this);
        visit.exitIIncInstruction(this);
    }

    void read(Instruction other) {
        super.read(other);
        setIncrement(((IIncInstruction) other).getIncrement());
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setLocal(in.readUnsignedByte());
        setIncrement(in.readByte());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeByte(getLocal());
        out.writeByte(getIncrement());
    }
}

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
 * The <code>multianewarray</code> instruction, which creates a new
 * multi-dimensional array.
 *
 * @author Abe White
 */
public class MultiANewArrayInstruction extends ClassInstruction {

    private int _dims = -1;

    MultiANewArrayInstruction(Code owner) {
        super(owner, Constants.MULTIANEWARRAY);
    }

    int getLength() {
        return super.getLength() + 1;
    }

    public int getLogicalStackChange() {
        return getStackChange();
    }

    public int getStackChange() {
        return -(getDimensions()) + 1;
    }

    /**
     * Return the dimensions of the array, or -1 if not set.
     */
    public int getDimensions() {
        return _dims;
    }

    /**
     * Set the dimensions of the array.
     *
     * @return this instruction, for method chaining
     */
    public MultiANewArrayInstruction setDimensions(int dims) {
        _dims = dims;
        return this;
    }

    /**
     * Two MultiANewArray instructions are equal if they have the same
     * type and dimensions, or if the type and dimensions of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        if (!(other instanceof MultiANewArrayInstruction))
            return false;
        if (!super.equalsInstruction(other))
            return false;

        MultiANewArrayInstruction ins = (MultiANewArrayInstruction) other;
        int dims = getDimensions();
        int otherDims = ins.getDimensions();
        return dims == -1 || otherDims == -1 || dims == otherDims;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterMultiANewArrayInstruction(this);
        visit.exitMultiANewArrayInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);
        setDimensions(((MultiANewArrayInstruction) orig).getDimensions());
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setDimensions(in.readUnsignedByte());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeByte(getDimensions());
    }
}

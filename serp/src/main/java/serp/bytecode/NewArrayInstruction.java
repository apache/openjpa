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
import java.util.*;
import serp.bytecode.visitor.*;

/**
 * The <code>newarray</code> instruction, which is used to create new
 * arrays of primitive types.
 * 
 * @author Abe White
 */
public class NewArrayInstruction extends TypedInstruction {
    private static final Class[][] _mappings = new Class[][] {
        { void.class, int.class }, { Object.class, int.class }, };

    private int _code = -1;

    NewArrayInstruction(Code owner) {
        super(owner, Constants.NEWARRAY);
    }

    int getLength() {
        return super.getLength() + 1;
    }

    public String getTypeName() {
        switch (getTypeCode()) {
        case Constants.ARRAY_BOOLEAN:
            return boolean.class.getName();
        case Constants.ARRAY_CHAR:
            return char.class.getName();
        case Constants.ARRAY_FLOAT:
            return float.class.getName();
        case Constants.ARRAY_DOUBLE:
            return double.class.getName();
        case Constants.ARRAY_BYTE:
            return byte.class.getName();
        case Constants.ARRAY_SHORT:
            return short.class.getName();
        case Constants.ARRAY_INT:
            return int.class.getName();
        case Constants.ARRAY_LONG:
            return long.class.getName();
        default:
            return null;
        }
    }

    public TypedInstruction setType(String type) {
        type = mapType(type, _mappings, true);
        if (type == null)
            return setTypeCode(-1);

        switch (type.charAt(0)) {
        case 'b':
            if (boolean.class.getName().equals(type))
                return setTypeCode(Constants.ARRAY_BOOLEAN);
            return setTypeCode(Constants.ARRAY_BYTE);
        case 'c':
            return setTypeCode(Constants.ARRAY_CHAR);
        case 'f':
            return setTypeCode(Constants.ARRAY_FLOAT);
        case 'd':
            return setTypeCode(Constants.ARRAY_DOUBLE);
        case 's':
            return setTypeCode(Constants.ARRAY_SHORT);
        case 'i':
            return setTypeCode(Constants.ARRAY_INT);
        case 'l':
            return setTypeCode(Constants.ARRAY_LONG);
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Return the array code used in the lowlevel bytecode, or -1 if unset.
     */
    public int getTypeCode() {
        return _code;
    }

    /**
     * Set the array code used in the lowlevel bytecode.
     * 
     * @return this instruction, for method chaining
     */
    public NewArrayInstruction setTypeCode(int code) {
        _code = code;
        return this;
    }

    /**
     * NewArray instructions are equal if the array type is the same,
     * of if the array type of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (this == other)
            return true;
        if (!(other instanceof NewArrayInstruction))
            return false;

        NewArrayInstruction ins = (NewArrayInstruction) other;
        int code = getTypeCode();
        int otherCode = ins.getTypeCode();
        return code == -1 || otherCode == -1 || code == otherCode;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterNewArrayInstruction(this);
        visit.exitNewArrayInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);
        setTypeCode(((NewArrayInstruction) orig).getTypeCode());
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setTypeCode(in.readUnsignedByte());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeByte(getTypeCode());
    }
}

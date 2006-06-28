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


/**
 *  <p>Loads a value from the locals table to the stack.</p>
 *
 *  @author Abe White
 */
public class LoadInstruction extends LocalVariableInstruction {
    private static final Class[][] _mappings = new Class[][] {
            { byte.class, int.class },
            { boolean.class, int.class },
            { char.class, int.class },
            { short.class, int.class },
            { void.class, int.class },
        };
    String _type = null;

    LoadInstruction(Code owner) {
        super(owner);
    }

    LoadInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    int getLength() {
        switch (getOpcode()) {
        case Constants.ILOAD:
        case Constants.LLOAD:
        case Constants.FLOAD:
        case Constants.DLOAD:
        case Constants.ALOAD:
            return super.getLength() + 1;

        default:
            return super.getLength();
        }
    }

    public int getStackChange() {
        switch (getOpcode()) {
        case Constants.LLOAD:
        case Constants.LLOAD0:
        case Constants.LLOAD1:
        case Constants.LLOAD2:
        case Constants.LLOAD3:
        case Constants.DLOAD:
        case Constants.DLOAD0:
        case Constants.DLOAD1:
        case Constants.DLOAD2:
        case Constants.DLOAD3:
            return 2;

        case Constants.NOP:
            return 0;

        default:
            return 1;
        }
    }

    public int getLogicalStackChange() {
        switch (getOpcode()) {
        case Constants.NOP:
            return 0;

        default:
            return 1;
        }
    }

    public String getTypeName() {
        switch (getOpcode()) {
        case Constants.ILOAD:
        case Constants.ILOAD0:
        case Constants.ILOAD1:
        case Constants.ILOAD2:
        case Constants.ILOAD3:
            return int.class.getName();

        case Constants.LLOAD:
        case Constants.LLOAD0:
        case Constants.LLOAD1:
        case Constants.LLOAD2:
        case Constants.LLOAD3:
            return long.class.getName();

        case Constants.FLOAD:
        case Constants.FLOAD0:
        case Constants.FLOAD1:
        case Constants.FLOAD2:
        case Constants.FLOAD3:
            return float.class.getName();

        case Constants.DLOAD:
        case Constants.DLOAD0:
        case Constants.DLOAD1:
        case Constants.DLOAD2:
        case Constants.DLOAD3:
            return double.class.getName();

        case Constants.ALOAD:
        case Constants.ALOAD0:
        case Constants.ALOAD1:
        case Constants.ALOAD2:
        case Constants.ALOAD3:
            return Object.class.getName();

        default:
            return _type;
        }
    }

    public TypedInstruction setType(String type) {
        type = mapType(type, _mappings, true);

        int local = getLocal();

        // if an invalid type or local, revert to nop
        if ((type == null) || (local < 0)) {
            _type = type;

            return (TypedInstruction) setOpcode(Constants.NOP);
        }

        // valid opcode, unset saved type
        _type = null;

        switch (type.charAt(0)) {
        case 'i':
            return (TypedInstruction) setOpcode((local > 3) ? Constants.ILOAD
                                                            : (Constants.ILOAD0 +
                local));

        case 'l':
            return (TypedInstruction) setOpcode((local > 3) ? Constants.LLOAD
                                                            : (Constants.LLOAD0 +
                local));

        case 'f':
            return (TypedInstruction) setOpcode((local > 3) ? Constants.FLOAD
                                                            : (Constants.FLOAD0 +
                local));

        case 'd':
            return (TypedInstruction) setOpcode((local > 3) ? Constants.DLOAD
                                                            : (Constants.DLOAD0 +
                local));

        default:
            return (TypedInstruction) setOpcode((local > 3) ? Constants.ALOAD
                                                            : (Constants.ALOAD0 +
                local));
        }
    }

    /**
     *  Equivalent to <code>setLocal (0).setType (Object.class)</code>; the
     *  <code>this</code> ptr is always passed in local variable 0.
     *
     *  @return this instruction, for method chaining
     */
    public LoadInstruction setThis() {
        return (LoadInstruction) setLocal(0).setType(Object.class);
    }

    /**
     *  Equivalent to <code>getLocal () == 0 && getType () ==
     *  Object.class</code>; the <code>this</code> ptr
     *  is always passed in local variable 0.
     */
    public boolean isThis() {
        return (getLocal() == 0) && (getType() == Object.class);
    }

    /**
     *  LoadInstructions are equal if the type they reference the same
     *  type and locals index or if either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this) {
            return true;
        }

        if (!super.equalsInstruction(other)) {
            return false;
        }

        String type = getTypeName();
        String otherType = ((LoadInstruction) other).getTypeName();

        return (type == null) || (otherType == null) || type.equals(otherType);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterLoadInstruction(this);
        visit.exitLoadInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);

        LoadInstruction ins = (LoadInstruction) orig;
        _type = ins._type;
    }

    void read(DataInput in) throws IOException {
        super.read(in);

        switch (getOpcode()) {
        case Constants.ILOAD:
        case Constants.LLOAD:
        case Constants.FLOAD:
        case Constants.DLOAD:
        case Constants.ALOAD:
            setLocal(in.readUnsignedByte());

            break;
        }
    }

    void write(DataOutput out) throws IOException {
        super.write(out);

        switch (getOpcode()) {
        case Constants.ILOAD:
        case Constants.LLOAD:
        case Constants.FLOAD:
        case Constants.DLOAD:
        case Constants.ALOAD:
            out.writeByte(getLocal());
        }
    }

    void calculateOpcode() {
        // taken care of when setting type
        setType(getTypeName());
    }

    void calculateLocal() {
        switch (getOpcode()) {
        case Constants.ILOAD0:
        case Constants.LLOAD0:
        case Constants.FLOAD0:
        case Constants.DLOAD0:
        case Constants.ALOAD0:
            setLocal(0);

            break;

        case Constants.ILOAD1:
        case Constants.LLOAD1:
        case Constants.FLOAD1:
        case Constants.DLOAD1:
        case Constants.ALOAD1:
            setLocal(1);

            break;

        case Constants.ILOAD2:
        case Constants.LLOAD2:
        case Constants.FLOAD2:
        case Constants.DLOAD2:
        case Constants.ALOAD2:
            setLocal(2);

            break;

        case Constants.ILOAD3:
        case Constants.LLOAD3:
        case Constants.FLOAD3:
        case Constants.DLOAD3:
        case Constants.ALOAD3:
            setLocal(3);

            break;
        }
    }
}

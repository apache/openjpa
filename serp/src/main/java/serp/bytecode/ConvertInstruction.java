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
import serp.util.Strings;

/**
 * A conversion opcode such as <code>i2l, f2i</code>, etc.
 * Changing the types of the instruction will automatically
 * update the underlying opcode. Converting from one type to the same
 * type will result in a <code>nop</code>.
 *
 * @author Abe White
 */
public class ConvertInstruction extends TypedInstruction {

    private static final Class[][] _mappings = new Class[][]{
        { boolean.class, int.class }, { void.class, int.class },
        { Object.class, int.class }, };
    private static final Class[][] _fromMappings = new Class[][]{
        { boolean.class, int.class }, { void.class, int.class },
        { Object.class, int.class }, { byte.class, int.class },
        { char.class, int.class }, { short.class, int.class }, };

    String _toType = null;
    String _fromType = null;

    ConvertInstruction(Code owner) {
        super(owner);
    }

    ConvertInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        return 0;
    }

    public int getStackChange() {
        switch (getOpcode()) {
            case Constants.I2L:
            case Constants.I2D:
            case Constants.F2L:
            case Constants.F2D:
                return 1;
            case Constants.L2I:
            case Constants.L2F:
            case Constants.D2I:
            case Constants.D2F:
                return -1;
            default:
                return 0;
        }
    }

    public String getTypeName() {
        switch (getOpcode()) {
            case Constants.L2I:
            case Constants.F2I:
            case Constants.D2I:
                return int.class.getName();
            case Constants.I2L:
            case Constants.F2L:
            case Constants.D2L:
                return long.class.getName();
            case Constants.I2F:
            case Constants.L2F:
            case Constants.D2F:
                return float.class.getName();
            case Constants.I2D:
            case Constants.L2D:
            case Constants.F2D:
                return double.class.getName();
            case Constants.I2B:
                return byte.class.getName();
            case Constants.I2C:
                return char.class.getName();
            case Constants.I2S:
                return short.class.getName();
            default:
                return _toType;
        }
    }

    public TypedInstruction setType(String type) {
        String toType = mapType(type, _mappings, true);
        String fromType = getFromTypeName();

        // if no valid opcode, remember current types in case they reset one
        // to create a valid opcode
        if (toType == null || fromType == null || toType.equals(fromType)) {
            _toType = toType;
            _fromType = fromType;
            return (TypedInstruction) setOpcode(Constants.NOP);
        }

        // ok, valid conversion possible, forget saved types
        _toType = null;
        _fromType = null;

        char to = toType.charAt(0);
        char from = fromType.charAt(0);

        switch (to) {
            case 'i':
                switch (from) {
                    case 'l':
                        return (TypedInstruction) setOpcode(Constants.L2I);
                    case 'f':
                        return (TypedInstruction) setOpcode(Constants.F2I);
                    case 'd':
                        return (TypedInstruction) setOpcode(Constants.D2I);
                }
            case 'l':
                switch (from) {
                    case 'i':
                        return (TypedInstruction) setOpcode(Constants.I2L);
                    case 'f':
                        return (TypedInstruction) setOpcode(Constants.F2L);
                    case 'd':
                        return (TypedInstruction) setOpcode(Constants.D2L);
                }
            case 'f':
                switch (from) {
                    case 'i':
                        return (TypedInstruction) setOpcode(Constants.I2F);
                    case 'l':
                        return (TypedInstruction) setOpcode(Constants.L2F);
                    case 'd':
                        return (TypedInstruction) setOpcode(Constants.D2F);
                }
            case 'd':
                switch (from) {
                    case 'i':
                        return (TypedInstruction) setOpcode(Constants.I2D);
                    case 'l':
                        return (TypedInstruction) setOpcode(Constants.L2D);
                    case 'f':
                        return (TypedInstruction) setOpcode(Constants.F2D);
                }
            case 'b':
                if (from == 'i')
                    return (TypedInstruction) setOpcode(Constants.I2B);
            case 'C':
                if (from == 'i')
                    return (TypedInstruction) setOpcode(Constants.I2C);
            case 'S':
                if (from == 'i')
                    return (TypedInstruction) setOpcode(Constants.I2S);
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Return the name of the type being converted from.
     * If neither type has been set, this method will return null.
     */
    public String getFromTypeName() {
        switch (getOpcode()) {
            case Constants.I2L:
            case Constants.I2F:
            case Constants.I2D:
            case Constants.I2B:
            case Constants.I2S:
            case Constants.I2C:
                return int.class.getName();
            case Constants.L2I:
            case Constants.L2F:
            case Constants.L2D:
                return long.class.getName();
            case Constants.F2I:
            case Constants.F2L:
            case Constants.F2D:
                return float.class.getName();
            case Constants.D2I:
            case Constants.D2L:
            case Constants.D2F:
                return double.class.getName();
            default:
                return _fromType;
        }
    }

    /**
     * Return the {@link Class} of the type being converted from.
     * If neither type has been set, this method will return null.
     */
    public Class getFromType() {
        String type = getFromTypeName();
        if (type == null)
            return null;
        return Strings.toClass(type, getClassLoader());
    }

    /**
     * Return the bytecode of the type being converted from.
     * If neither type has been set, this method will return null.
     */
    public BCClass getFromTypeBC() {
        String type = getFromTypeName();
        if (type == null)
            return null;
        return getProject().loadClass(type, getClassLoader());
    }

    /**
     * Set the type being converted from. Types that have no direct
     * support will be converted accordingly.
     *
     * @return this instruction, for method chaining
     */
    public ConvertInstruction setFromType(String type) {
        String fromType = mapType(type, _fromMappings, true);
        String toType = getTypeName();

        // if no valid opcode, remember current types in case they reset one
        // to create a valid opcode
        if (toType == null || fromType == null || toType.equals(fromType)) {
            _toType = toType;
            _fromType = fromType;
            return (ConvertInstruction) setOpcode(Constants.NOP);
        }

        // ok, valid conversion possible, forget saved types
        _toType = null;
        _fromType = null;

        char to = toType.charAt(0);
        char from = fromType.charAt(0);

        switch (from) {
            case 'i':
                switch (to) {
                    case 'l':
                        return (ConvertInstruction) setOpcode(Constants.I2L);
                    case 'f':
                        return (ConvertInstruction) setOpcode(Constants.I2F);
                    case 'd':
                        return (ConvertInstruction) setOpcode(Constants.I2D);
                    case 'b':
                        return (ConvertInstruction) setOpcode(Constants.I2B);
                    case 'c':
                        return (ConvertInstruction) setOpcode(Constants.I2C);
                    case 's':
                        return (ConvertInstruction) setOpcode(Constants.I2S);
                }
            case 'l':
                switch (to) {
                    case 'i':
                        return (ConvertInstruction) setOpcode(Constants.L2I);
                    case 'f':
                        return (ConvertInstruction) setOpcode(Constants.L2F);
                    case 'd':
                        return (ConvertInstruction) setOpcode(Constants.L2D);
                }
            case 'f':
                switch (to) {
                    case 'i':
                        return (ConvertInstruction) setOpcode(Constants.F2I);
                    case 'l':
                        return (ConvertInstruction) setOpcode(Constants.F2L);
                    case 'd':
                        return (ConvertInstruction) setOpcode(Constants.F2D);
                }
            case 'd':
                switch (to) {
                    case 'i':
                        return (ConvertInstruction) setOpcode(Constants.D2I);
                    case 'l':
                        return (ConvertInstruction) setOpcode(Constants.D2L);
                    case 'f':
                        return (ConvertInstruction) setOpcode(Constants.D2F);
                }
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Set the type being converted from. Types that have no direct
     * support will be converted accordingly.
     *
     * @return this instruction, for method chaining
     */
    public ConvertInstruction setFromType(Class type) {
        if (type == null)
            return setFromType((String) null);
        return setFromType(type.getName());
    }

    /**
     * Set the type being converted from. Types that have no direct
     * support will be converted accordingly.
     *
     * @return this instruction, for method chaining
     */
    public ConvertInstruction setFromType(BCClass type) {
        if (type == null)
            return setFromType((String) null);
        return setFromType(type.getName());
    }

    /**
     * ConvertInstructions are equal if the types they convert between are
     * either equal or unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        if (!(other instanceof ConvertInstruction))
            return false;

        ConvertInstruction ins = (ConvertInstruction) other;
        if (getOpcode() != Constants.NOP && getOpcode() == ins.getOpcode())
            return true;

        String type = getTypeName();
        String otherType = ins.getTypeName();
        if (!(type == null || otherType == null || type.equals(otherType)))
            return false;

        type = getFromTypeName();
        otherType = ins.getFromTypeName();
        return type == null || otherType == null || type.equals(otherType);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterConvertInstruction(this);
        visit.exitConvertInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);

        ConvertInstruction ins = (ConvertInstruction) orig;
        _toType = ins._toType;
        _fromType = ins._fromType;
    }
}

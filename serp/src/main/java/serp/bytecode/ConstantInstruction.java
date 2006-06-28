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

import serp.bytecode.lowlevel.*;

import serp.bytecode.visitor.*;

import serp.util.*;

import java.io.*;


/**
 *  <p>An instruction that that loads a constant onto the stack.
 *  The opcode represented by this instruction may change depending on the
 *  type and value of the constant set.  For example, if the constant value
 *  is initially set to 5, the opcode will be <code>iconst5</code>; if later
 *  incremented        to 6, the opcode will be changed to <code>bipush(6)</code>.</p>
 *
 *  @author Abe White
 */
public class ConstantInstruction extends TypedInstruction {
    private int _arg = -1;

    ConstantInstruction(Code owner) {
        super(owner);
    }

    ConstantInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    int getLength() {
        switch (getOpcode()) {
        case Constants.BIPUSH:
        case Constants.LDC:
            return super.getLength() + 1;

        case Constants.SIPUSH:
        case Constants.LDCW:
        case Constants.LDC2W:
            return super.getLength() + 2;

        default:
            return super.getLength();
        }
    }

    public int getStackChange() {
        String type = getTypeName();

        if (double.class.getName().equals(type) ||
                long.class.getName().equals(type)) {
            return 2;
        }

        return 1;
    }

    public int getLogicalStackChange() {
        return 1;
    }

    public String getTypeName() {
        int opcode = getOpcode();

        switch (opcode) {
        case Constants.NOP:
            return null;

        case Constants.ACONSTNULL:
            return Object.class.getName();

        case Constants.ICONSTM1:
        case Constants.ICONST0:
        case Constants.ICONST1:
        case Constants.ICONST2:
        case Constants.ICONST3:
        case Constants.ICONST4:
        case Constants.ICONST5:
        case Constants.BIPUSH:
        case Constants.SIPUSH:
            return int.class.getName();

        case Constants.LCONST0:
        case Constants.LCONST1:
            return long.class.getName();

        case Constants.FCONST0:
        case Constants.FCONST1:
        case Constants.FCONST2:
            return float.class.getName();

        case Constants.DCONST0:
        case Constants.DCONST1:
            return double.class.getName();
        }

        Entry entry = getPool().getEntry(_arg);

        switch (entry.getType()) {
        case Entry.UTF8:
        case Entry.STRING:
            return String.class.getName();

        case Entry.INT:
            return int.class.getName();

        case Entry.FLOAT:
            return float.class.getName();

        case Entry.LONG:
            return long.class.getName();

        case Entry.DOUBLE:
            return double.class.getName();

        case Entry.CLASS:
            return Class.class.getName();

        default:
            return null;
        }
    }

    public TypedInstruction setType(String type) {
        throw new UnsupportedOperationException("Use setValue");
    }

    /**
     *  Return the value of the constant as its wrapper type, or null if
     *  not set.  Returns class values as the class name.
     */
    public Object getValue() {
        int opcode = getOpcode();

        switch (opcode) {
        case Constants.NOP:
        case Constants.ACONSTNULL:
            return null;

        case Constants.ICONSTM1:
        case Constants.ICONST0:
        case Constants.ICONST1:
        case Constants.ICONST2:
        case Constants.ICONST3:
        case Constants.ICONST4:
        case Constants.ICONST5:
            return Numbers.valueOf(opcode - Constants.ICONST0);

        case Constants.LCONST0:
        case Constants.LCONST1:
            return Numbers.valueOf((long) (opcode - Constants.LCONST0));

        case Constants.FCONST0:
        case Constants.FCONST1:
        case Constants.FCONST2:
            return new Float(opcode - Constants.FCONST0);

        case Constants.DCONST0:
        case Constants.DCONST1:
            return new Double(opcode - Constants.DCONST0);

        case Constants.BIPUSH:
        case Constants.SIPUSH:
            return Numbers.valueOf(_arg);

        default:

            Entry entry = getPool().getEntry(_arg);
            Object val = ((ConstantEntry) entry).getConstant();

            if (entry.getType() == Entry.CLASS) {
                return getProject().getNameCache()
                           .getExternalForm((String) val, false);
            }

            return val;
        }
    }

    /**
     *  Set the constant to the given value.  The value should be
     *  an instance of String, Integer, Long, Double, Float, Class, BCClass, or
     *  null depending on the constant type.  If the given value is not
     *  supported directly, it will be converted accordingly.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(Object value) {
        if (value instanceof Boolean) {
            value = Numbers.valueOf((((Boolean) value).booleanValue()) ? 1 : 0);
        } else if (value instanceof Character) {
            value = Numbers.valueOf((int) ((Character) value).charValue());
        } else if (value instanceof Byte) {
            value = Numbers.valueOf(((Byte) value).intValue());
        } else if (value instanceof Short) {
            value = Numbers.valueOf(((Short) value).intValue());
        } else if ((value != null) && !(value instanceof Number) &&
                !(value instanceof String) && !(value instanceof Class) &&
                !(value instanceof BCClass)) {
            throw new IllegalArgumentException("value = " + value);
        }

        calculateOpcode(value, false);

        return this;
    }

    /**
      *  Return the string value of this constant, or null if not set.
     */
    public String getStringValue() {
        return (String) getValue();
    }

    /**
      *  Return the int value of this constant, or 0 if not set.
     */
    public int getIntValue() {
        Object value = getValue();

        return (value == null) ? 0 : ((Number) value).intValue();
    }

    /**
      *  Return the long value of this constant, or 0 if not set.
     */
    public long getLongValue() {
        Object value = getValue();

        return (value == null) ? 0L : ((Number) value).longValue();
    }

    /**
      *  Return the float value of this constant, or 0 if not set.
     */
    public float getFloatValue() {
        Object value = getValue();

        return (value == null) ? 0F : ((Number) value).floatValue();
    }

    /**
      *  Return the double value of this constant, or 0 if not set.
     */
    public double getDoubleValue() {
        Object value = getValue();

        return (value == null) ? 0D : ((Number) value).doubleValue();
    }

    /**
      *  Return the class value of this constant, or null if not set.
     */
    public String getClassNameValue() {
        return (String) getValue();
    }

    /**
     *  Set this constant to null.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setNull() {
        calculateOpcode(null, false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(String value) {
        calculateOpcode(value, false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(Class value) {
        calculateOpcode(value, false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(BCClass value) {
        calculateOpcode(value, false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(int value) {
        calculateOpcode(Numbers.valueOf(value), false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(long value) {
        calculateOpcode(Numbers.valueOf(value), false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(float value) {
        calculateOpcode(new Float(value), false);

        return this;
    }

    /**
      *  Set the value of this constant.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(double value) {
        calculateOpcode(new Double(value), false);

        return this;
    }

    /**
     *  Set the value of this constant; note that this type is converted
     *  to int.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(boolean value) {
        return setValue((value) ? 1 : 0);
    }

    /**
     *  Set the value of this constant; note that this type is converted
     *  to int.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(short value) {
        return setValue((int) value);
    }

    /**
     *  Set the value of this constant; note that this type is converted
     *  to int.
      *
     *  @return this instruction, for method chaining
     */
    public ConstantInstruction setValue(char value) {
        return setValue((int) value);
    }

    /**
     *  ConstantInstructions are equal if the const they reference is the same,
     *  or if the const of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ConstantInstruction)) {
            return false;
        }

        Object value = getValue();
        Object otherValue = ((ConstantInstruction) other).getValue();

        return (value == null) || (otherValue == null) ||
        value.equals(otherValue);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterConstantInstruction(this);
        visit.exitConstantInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);

        ConstantInstruction ci = (ConstantInstruction) orig;
        calculateOpcode(ci.getValue(), ci.getOpcode() == Constants.LDCW);
    }

    void read(DataInput in) throws IOException {
        super.read(in);

        switch (getOpcode()) {
        case Constants.BIPUSH:
        case Constants.LDC:
            _arg = in.readUnsignedByte();

            break;

        case Constants.SIPUSH:
        case Constants.LDCW:
        case Constants.LDC2W:
            _arg = in.readUnsignedShort();
        }
    }

    void write(DataOutput out) throws IOException {
        super.write(out);

        switch (getOpcode()) {
        case Constants.BIPUSH:
        case Constants.LDC:
            out.writeByte(_arg);

            break;

        case Constants.SIPUSH:
        case Constants.LDCW:
        case Constants.LDC2W:
            out.writeShort(_arg);

            break;
        }
    }

    private void calculateOpcode(Object value, boolean wide) {
        _arg = -1;

        if (value == null) {
            setOpcode(Constants.ACONSTNULL);
        } else if (value instanceof Float) {
            float floatVal = ((Float) value).floatValue();

            if ((floatVal == 0) || (floatVal == 1) || (floatVal == 2)) {
                setOpcode(Constants.FCONST0 + (int) floatVal);
            } else {
                _arg = getPool().findFloatEntry((float) floatVal, true);
                setOpcode(((_arg > 255) || wide) ? Constants.LDCW : Constants.LDC);
            }
        } else if (value instanceof Long) {
            long longVal = ((Long) value).longValue();

            if ((longVal == 0) || (longVal == 1)) {
                setOpcode(Constants.LCONST0 + (int) longVal);
            } else {
                _arg = getPool().findLongEntry(longVal, true);
                setOpcode(Constants.LDC2W);
            }
        } else if (value instanceof Double) {
            double doubleVal = ((Double) value).doubleValue();

            if ((doubleVal == 0) || (doubleVal == 1)) {
                setOpcode(Constants.DCONST0 + (int) doubleVal);
            } else {
                _arg = getPool().findDoubleEntry(doubleVal, true);
                setOpcode(Constants.LDC2W);
            }
        } else if (value instanceof Integer) {
            int intVal = ((Integer) value).intValue();

            if ((intVal >= -1) && (intVal <= 5)) {
                setOpcode(Constants.ICONST0 + intVal);
            } else if ((intVal >= -(2 << 6)) && (intVal < (2 << 6))) {
                setOpcode(Constants.BIPUSH);
                _arg = intVal;
            } else if ((intVal >= -(2 << 14)) && (intVal < (2 << 14))) {
                setOpcode(Constants.SIPUSH);
                _arg = intVal;
            } else {
                _arg = getPool().findIntEntry(intVal, true);
                setOpcode(((_arg > 255) || wide) ? Constants.LDCW : Constants.LDC);
            }
        } else if (value instanceof String) {
            _arg = getPool().findStringEntry((String) value, true);
            setOpcode(((_arg > 255) || wide) ? Constants.LDCW : Constants.LDC);
        } else if (value instanceof Class) {
            String name = getProject().getNameCache()
                              .getInternalForm(((Class) value).getName(), false);
            _arg = getPool().findClassEntry(name, true);
            setOpcode(Constants.LDCW);
        } else if (value instanceof BCClass) {
            BCClass bc = (BCClass) value;
            ClassEntry entry = (ClassEntry) bc.getPool().getEntry(bc.getIndex());

            if (bc.getPool() == getPool()) {
                _arg = getPool().indexOf(entry);
            } else {
                _arg = getPool()
                           .findClassEntry((String) entry.getConstant(), true);
            }

            setOpcode(Constants.LDCW);
        } else {
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }
}

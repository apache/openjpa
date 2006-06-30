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
import serp.bytecode.lowlevel.*;
import serp.bytecode.visitor.*;

/**
 * A constant value for a member field.
 * 
 * @author Abe White
 */
public class ConstantValue extends Attribute {
    int _valueIndex = 0;

    ConstantValue(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    int getLength() {
        return 2;
    }

    /**
     * Return the owning field.
     */
    public BCField getField() {
        return(BCField) getOwner();
    }

    /**
     * Return the {@link ConstantPool} index of the {@link ConstantEntry}
     * holding the value of this constant. Defaults to 0.
     */
    public int getValueIndex() {
        return _valueIndex;
    }

    /**
     * Set the {@link ConstantPool} of the {@link ConstantEntry}
     * holding the value of this constant.
     */
    public void setValueIndex(int valueIndex) {
        _valueIndex = valueIndex;
    }

    /**
     * Return the type of constant this attribute represents, or null if
     * not set.
     */
    public String getTypeName() {
        Class type = getType();
        if (type == null)
            return null;
        return type.getName();
    }

    /**
     * Return the type of constant this attribute represents(String.class,
     * int.class, etc), or null if not set.
     */
    public Class getType() {
        Object value = getValue();
        if (value == null)
            return null;

        Class type = value.getClass();
        if (type == Integer.class)
            return int.class;
        if (type == Float.class)
            return float.class;
        if (type == Double.class)
            return double.class;
        if (type == Long.class)
            return long.class;
        return String.class;
    }

    /**
     * Return the bytecode for the type of constant this attribute represents.
     */
    public BCClass getTypeBC() {
        return getProject().loadClass(getType());
    }

    /**
     * Return the value of this constant as an Object of the appropriate
     * type(String, Integer, Double, etc), or null if not set.
     */
    public Object getValue() {
        if (_valueIndex <= 0)
            return null;
        return((ConstantEntry) getPool().getEntry(_valueIndex)). getConstant();
    }

    /**
     * Set the value of this constant using the appropriate wrapper Object
     * type(String, Integer, Double, etc). Types that are not directly
     * supported will be converted accordingly if possible.
     */
    public void setValue(Object value) {
        Class type = value.getClass();

        if (type == Boolean.class)
            setIntValue((((Boolean) value).booleanValue()) ? 1 : 0);
        else if (type == Character.class)
            setIntValue((int) ((Character) value).charValue());
        else if (type == Byte.class || type == Integer.class
            || type == Short.class)
            setIntValue(((Number) value).intValue());
        else if (type == Float.class)
            setFloatValue(((Number) value).floatValue());
        else if (type == Double.class)
            setDoubleValue(((Number) value).doubleValue());
        else if (type == Long.class)
            setLongValue(((Number) value).longValue());
        else
            setStringValue(value.toString());
    }

    /**
     * Get the value of this int constant, or 0 if not set.
     */
    public int getIntValue() {
        if (getValueIndex() <= 0)
            return 0;
        return((IntEntry) getPool().getEntry(getValueIndex())).getValue();
    }

    /**
     * Set the value of this int constant.
     */
    public void setIntValue(int value) {
        setValueIndex(getPool().findIntEntry(value, true));
    }

    /**
     * Get the value of this float constant.
     */
    public float getFloatValue() {
        if (getValueIndex() <= 0)
            return 0F;
        return((FloatEntry) getPool().getEntry(getValueIndex())).getValue();
    }

    /**
     * Set the value of this float constant.
     */
    public void setFloatValue(float value) {
        setValueIndex(getPool().findFloatEntry(value, true));
    }

    /**
     * Get the value of this double constant.
     */
    public double getDoubleValue() {
        if (getValueIndex() <= 0)
            return 0D;
        return((DoubleEntry) getPool().getEntry(getValueIndex())). getValue();
    }

    /**
     * Set the value of this double constant.
     */
    public void setDoubleValue(double value) {
        setValueIndex(getPool().findDoubleEntry(value, true));
    }

    /**
     * Get the value of this long constant.
     */
    public long getLongValue() {
        if (getValueIndex() <= 0)
            return 0L;
        return((LongEntry) getPool().getEntry(getValueIndex())).getValue();
    }

    /**
     * Set the value of this long constant.
     */
    public void setLongValue(long value) {
        setValueIndex(getPool().findLongEntry(value, true));
    }

    /**
     * Get the value of this string constant.
     */
    public String getStringValue() {
        if (getValueIndex() <= 0)
            return null;
        return((StringEntry) getPool().getEntry(getValueIndex())).
            getStringEntry().getValue();
    }

    /**
     * Set the value of this string constant.
     */
    public void setStringValue(String value) {
        setValueIndex(getPool().findStringEntry(value, true));
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterConstantValue(this);
        visit.exitConstantValue(this);
    }

    void read(Attribute other) {
        setValue(((ConstantValue) other).getValue());
    }

    void read(DataInput in, int length) throws IOException {
        setValueIndex(in.readUnsignedShort());
    }

    void write(DataOutput out, int length) throws IOException {
        out.writeShort(getValueIndex());
    }
}

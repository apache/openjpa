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
package serp.bytecode.lowlevel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import serp.bytecode.visitor.BCVisitor;

/**
 * A constant float value in the constant pool.
 *
 * @author Abe White
 */
public class FloatEntry extends Entry implements ConstantEntry {

    private float _value = 0.0F;

    /**
     * Default constructor.
     */
    public FloatEntry() {
    }

    /**
     * Constructor.
     *
     * @param value the constant float value of this entry
     */
    public FloatEntry(float value) {
        _value = value;
    }

    public int getType() {
        return Entry.FLOAT;
    }

    /**
     * Return the value of this constant.
     */
    public float getValue() {
        return _value;
    }

    /**
     * Set the value of this constant.
     */
    public void setValue(float value) {
        Object key = beforeModify();
        _value = value;
        afterModify(key);
    }

    public Object getConstant() {
        return new Float(getValue());
    }

    public void setConstant(Object value) {
        setValue(((Number) value).floatValue());
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterFloatEntry(this);
        visit.exitFloatEntry(this);
    }

    void readData(DataInput in) throws IOException {
        _value = in.readFloat();
    }

    void writeData(DataOutput out) throws IOException {
        out.writeFloat(_value);
    }
}

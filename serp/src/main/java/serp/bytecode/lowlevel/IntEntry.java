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
import serp.util.Numbers;

/**
 * A constant int value in the constant pool.
 *
 * @author Abe White
 */
public class IntEntry extends Entry implements ConstantEntry {

    private int _value = -1;

    /**
     * Default constructor.
     */
    public IntEntry() {
    }

    /**
     * Constructor.
     *
     * @param value the constant int value of this entry
     */
    public IntEntry(int value) {
        _value = value;
    }

    public int getType() {
        return Entry.INT;
    }

    /**
     * Return the value of this constant.
     */
    public int getValue() {
        return _value;
    }

    /**
     * Set the value of this constant.
     */
    public void setValue(int value) {
        Object key = beforeModify();
        _value = value;
        afterModify(key);
    }

    public Object getConstant() {
        return Numbers.valueOf(getValue());
    }

    public void setConstant(Object value) {
        setValue(((Number) value).intValue());
    }

    protected void readData(DataInput in) throws IOException {
        _value = in.readInt();
    }

    protected void writeData(DataOutput out) throws IOException {
        out.writeInt(_value);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterIntEntry(this);
        visit.exitIntEntry(this);
    }
}

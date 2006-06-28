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
package serp.bytecode.lowlevel;

import serp.bytecode.visitor.*;

import serp.util.*;

import java.io.*;


/**
 *  <p>A long constant in the constant pool.</p>
 *
 *  @author Abe White
 */
public class LongEntry extends Entry implements ConstantEntry {
    private long _value = 0L;

    /**
     *  Default constructor.
     */
    public LongEntry() {
    }

    /**
     *  Constructor.
     *
     *  @param value        the constant long value of this entry
     */
    public LongEntry(long value) {
        _value = value;
    }

    public boolean isWide() {
        return true;
    }

    public int getType() {
        return Entry.LONG;
    }

    /**
     *  Return the value of the constant.
     */
    public long getValue() {
        return _value;
    }

    /**
     *  Set the value of the constant.
     */
    public void setValue(long value) {
        Object key = beforeModify();
        _value = value;
        afterModify(key);
    }

    public Object getConstant() {
        return Numbers.valueOf(getValue());
    }

    public void setConstant(Object value) {
        setValue(((Number) value).longValue());
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterLongEntry(this);
        visit.exitLongEntry(this);
    }

    void readData(DataInput in) throws IOException {
        _value = in.readLong();
    }

    void writeData(DataOutput out) throws IOException {
        out.writeLong(_value);
    }
}

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
 * A String constant in the constant pool. String constants
 * hold a reference to a {@link UTF8Entry} that stores the actual value.
 *
 * @author Abe White
 */
public class StringEntry extends Entry implements ConstantEntry {

    private int _stringIndex = -1;

    /**
     * Default constructor.
     */
    public StringEntry() {
    }

    /**
     * Constructor.
     *
     * @param stringIndex the constant pool index of the {@link UTF8Entry}
     *                    containing the value of this string
     */
    public StringEntry(int stringIndex) {
        _stringIndex = stringIndex;
    }

    public int getType() {
        return Entry.STRING;
    }

    /**
     * Return the constant pool index of the {@link UTF8Entry}
     * storing the value of this string.
     */
    public int getStringIndex() {
        return _stringIndex;
    }

    /**
     * Set the constant pool index of the {@link UTF8Entry}
     * storing the value of this string.
     */
    public void setStringIndex(int stringIndex) {
        Object key = beforeModify();
        _stringIndex = stringIndex;
        afterModify(key);
    }

    /**
     * Return the referenced {@link UTF8Entry}. This method can only
     * be run for entries that have been added to a constant pool.
     */
    public UTF8Entry getStringEntry() {
        return (UTF8Entry) getPool().getEntry(_stringIndex);
    }

    public Object getConstant() {
        return getStringEntry().getValue();
    }

    public void setConstant(Object value) {
        getStringEntry().setConstant(value);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterStringEntry(this);
        visit.exitStringEntry(this);
    }

    void readData(DataInput in) throws IOException {
        _stringIndex = in.readUnsignedShort();
    }

    void writeData(DataOutput out) throws IOException {
        out.writeShort(_stringIndex);
    }
}

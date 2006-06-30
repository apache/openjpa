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

import java.io.*;
import serp.bytecode.visitor.*;

/**
 * A unicode string value in the constant pool.
 * 
 * @author Abe White
 */
public class UTF8Entry extends Entry implements ConstantEntry {
    private String _value = "";

    /**
     * Default constructor.
     */
    public UTF8Entry() {
    }

    /**
     * Constructor.
     * 
     * @param value the constant string value of this entry
     */
    public UTF8Entry(String value) {
        _value = value;
    }

    public int getType() {
        return Entry.UTF8;
    }

    /**
     * Return the value of the entry.
     */
    public String getValue() {
        return _value;
    }

    /**
     * Set the value of the entry.
     */
    public void setValue(String value) {
        if (value == null)
            throw new NullPointerException("value = null");

        Object key = beforeModify();
        _value = value;
        afterModify(key);
    }

    public Object getConstant() {
        return getValue();
    }

    public void setConstant(Object value) {
        setValue((String) value);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterUTF8Entry(this);
        visit.exitUTF8Entry(this);
    }

    void readData(DataInput in) throws IOException {
        _value = in.readUTF();
    }

    void writeData(DataOutput out) throws IOException {
        out.writeUTF(_value);
    }
}

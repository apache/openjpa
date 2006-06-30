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
import serp.bytecode.visitor.*;

/**
 * An unrecognized attribute; class files are allowed to contain
 * attributes that are not recognized, and the JVM must ignore them.
 * 
 * @author Abe White
 */
public class UnknownAttribute extends Attribute {
    private byte[] _value = new byte[0];

    UnknownAttribute(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    int getLength() {
        return _value.length;
    }

    /**
     * The value is of unknown content, so it is stored as a byte array.
     */
    public byte[] getValue() {
        return _value;
    }

    /**
     * The value is of unknown content, so it is stored as a byte array.
     */
    public void setValue(byte[] value) {
        if (value == null)
            value = new byte[0];
        _value = value;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterUnknownAttribute(this);
        visit.exitUnknownAttribute(this);
    }

    void read(Attribute other) {
        setValue(((UnknownAttribute) other).getValue());
    }

    void read(DataInput in, int length) throws IOException {
        _value = new byte[length];
        in.readFully(_value);
    }

    void write(DataOutput out, int length) throws IOException {
        out.write(_value);
    }
}

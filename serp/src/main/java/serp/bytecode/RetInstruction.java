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
 * The <code>ret</code> instruction is used in the implementation of finally.
 * 
 * @author Abe White
 */
public class RetInstruction extends LocalVariableInstruction {
    RetInstruction(Code owner) {
        super(owner, Constants.RET);
    }

    int getLength() {
        return super.getLength() + 1;
    }

    public boolean equalsInstruction(Instruction other) {
        if (this == other)
            return true;
        if (!(other instanceof RetInstruction))
            return false;
        return super.equalsInstruction(other);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterRetInstruction(this);
        visit.exitRetInstruction(this);
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setLocal(in.readUnsignedByte());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeByte(getLocal());
    }
}

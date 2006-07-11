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

/**
 * Store a value from the stack into an array.
 *
 * @author Abe White
 */
public class ArrayStoreInstruction extends ArrayInstruction {

    private static final Class[][] _mappings = new Class[][]{
        { boolean.class, int.class }, { void.class, int.class }, };

    ArrayStoreInstruction(Code owner) {
        super(owner);
    }

    ArrayStoreInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        switch (getOpcode()) {
            case Constants.NOP:
                return 0;
            default:
                return -3;
        }
    }

    public int getStackChange() {
        switch (getOpcode()) {
            case Constants.DASTORE:
            case Constants.LASTORE:
                return -4;
            case Constants.NOP:
                return 0;
            default:
                return -3;
        }
    }

    public String getTypeName() {
        switch (getOpcode()) {
            case Constants.IASTORE:
                return int.class.getName();
            case Constants.LASTORE:
                return long.class.getName();
            case Constants.FASTORE:
                return float.class.getName();
            case Constants.DASTORE:
                return double.class.getName();
            case Constants.AASTORE:
                return Object.class.getName();
            case Constants.BASTORE:
                return byte.class.getName();
            case Constants.CASTORE:
                return char.class.getName();
            case Constants.SASTORE:
                return short.class.getName();
            default:
                return null;
        }
    }

    public TypedInstruction setType(String type) {
        type = mapType(type, _mappings, true);
        if (type == null)
            return (TypedInstruction) setOpcode(Constants.NOP);

        switch (type.charAt(0)) {
            case 'i':
                return (TypedInstruction) setOpcode(Constants.IASTORE);
            case 'l':
                return (TypedInstruction) setOpcode(Constants.LASTORE);
            case 'f':
                return (TypedInstruction) setOpcode(Constants.FASTORE);
            case 'd':
                return (TypedInstruction) setOpcode(Constants.DASTORE);
            case 'b':
                return (TypedInstruction) setOpcode(Constants.BASTORE);
            case 'c':
                return (TypedInstruction) setOpcode(Constants.CASTORE);
            case 's':
                return (TypedInstruction) setOpcode(Constants.SASTORE);
            default:
                return (TypedInstruction) setOpcode(Constants.AASTORE);
        }
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterArrayStoreInstruction(this);
        visit.exitArrayStoreInstruction(this);
    }
}

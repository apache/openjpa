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


/**
 *  <p>An if instruction such as <code>ifnull, ifeq</code>, etc.</p>
 *
 *  @author Abe White
 */
public class IfInstruction extends JumpInstruction {
    IfInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        return getStackChange();
    }

    public int getStackChange() {
        switch (getOpcode()) {
        case Constants.IFACMPEQ:
        case Constants.IFACMPNE:
        case Constants.IFICMPEQ:
        case Constants.IFICMPNE:
        case Constants.IFICMPLT:
        case Constants.IFICMPGT:
        case Constants.IFICMPLE:
        case Constants.IFICMPGE:
            return -2;

        case Constants.IFEQ:
        case Constants.IFNE:
        case Constants.IFLT:
        case Constants.IFGT:
        case Constants.IFLE:
        case Constants.IFGE:
        case Constants.IFNULL:
        case Constants.IFNONNULL:
            return -1;

        default:
            return super.getStackChange();
        }
    }

    public String getTypeName() {
        switch (getOpcode()) {
        case Constants.IFACMPEQ:
        case Constants.IFACMPNE:
        case Constants.IFNULL:
        case Constants.IFNONNULL:
            return "java.lang.Object";

        default:
            return "I";
        }
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterIfInstruction(this);
        visit.exitIfInstruction(this);
    }
}

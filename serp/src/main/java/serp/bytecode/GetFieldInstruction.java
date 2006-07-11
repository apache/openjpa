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
 * Loads a value from a field onto the stack.
 *
 * @author Abe White
 */
public class GetFieldInstruction extends FieldInstruction {

    GetFieldInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    public int getLogicalStackChange() {
        if (getOpcode() == Constants.GETSTATIC)
            return 1;
        return 0;
    }

    public int getStackChange() {
        String type = getFieldTypeName();
        if (type == null)
            return 0;

        int stack = 0;
        if (long.class.getName().equals(type)
            || double.class.getName().equals(type))
            stack++;
        if (getOpcode() == Constants.GETSTATIC)
            stack++;
        return stack;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterGetFieldInstruction(this);
        visit.exitGetFieldInstruction(this);
    }
}

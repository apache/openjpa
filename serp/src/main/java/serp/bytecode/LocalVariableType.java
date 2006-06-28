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

import serp.bytecode.visitor.*;


/**
 *  <p>A local variable type contains the name, signature, index and scope
 *  of a generics-using local used in opcodes.</p>
 *
 *  @author Abe White
 */
public class LocalVariableType extends Local {
    LocalVariableType(LocalVariableTypeTable owner) {
        super(owner);
    }

    /**
     *  The owning table.
     */
    public LocalVariableTypeTable getLocalVariableTypeTable() {
        return (LocalVariableTypeTable) getTable();
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterLocalVariableType(this);
        visit.exitLocalVariableType(this);
    }
}

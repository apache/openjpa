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

/**
 * Any array load or store instruction. This class has
 * no functionality beyond the {@link TypedInstruction} but is provided
 * so that users can easily identify array instructions in code if need be.
 *
 * @author Abe White
 */
public abstract class ArrayInstruction extends TypedInstruction {

    ArrayInstruction(Code owner) {
        super(owner);
    }

    ArrayInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }
}

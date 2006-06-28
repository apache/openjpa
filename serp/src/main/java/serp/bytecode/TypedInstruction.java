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

import serp.util.*;

import java.util.*;


/**
 *  <p>Any typed instruction.</p>
 *
 *  @author Abe White
 */
public abstract class TypedInstruction extends Instruction {
    private static final Set _opcodeTypes = new HashSet();

    static {
        _opcodeTypes.add(int.class.getName());
        _opcodeTypes.add(long.class.getName());
        _opcodeTypes.add(float.class.getName());
        _opcodeTypes.add(double.class.getName());
        _opcodeTypes.add(Object.class.getName());
        _opcodeTypes.add(byte.class.getName());
        _opcodeTypes.add(char.class.getName());
        _opcodeTypes.add(short.class.getName());
        _opcodeTypes.add(boolean.class.getName());
        _opcodeTypes.add(void.class.getName());
    }

    TypedInstruction(Code owner) {
        super(owner);
    }

    TypedInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    /**
     *  Return the type for the given name.  Takes into account
     *  the given mappings and the demote flag.
     *
     *  @param mappings        mappings of one type to another; for example,
     *                                          array instruction treat booleans as ints, so
     *                                          to reflect that there should be an index x of the
     *                                          array such that mappings[x][0] = boolean.class and
     *                                          mappings[x][1] = int.class; may be null if
     *                                          no special mappings are needed
     *  @param demote                if true, all object types will be demoted to
     *                                          Object.class
     */
    String mapType(String type, Class[][] mappings, boolean demote) {
        if (type == null) {
            return null;
        }

        type = getProject().getNameCache().getExternalForm(type, false);

        if (!_opcodeTypes.contains(type) && demote) {
            type = Object.class.getName();
        }

        if (mappings != null) {
            for (int i = 0; i < mappings.length; i++)
                if (mappings[i][0].getName().equals(type)) {
                    type = mappings[i][1].getName();
                }
        }

        return type;
    }

    /**
     *  Return the type name for this instruction.
     *  If the type has not been set, this method will return null.
     */
    public abstract String getTypeName();

    /**
     *  Return the type for this instruction.
     *  If the type has not been set, this method will return null.
     */
    public Class getType() {
        String type = getTypeName();

        if (type == null) {
            return null;
        }

        return Strings.toClass(type, getClassLoader());
    }

    /**
     *  Return the type for this instruction.
     *  If the type has not been set, this method will return null.
     */
    public BCClass getTypeBC() {
        String type = getTypeName();

        if (type == null) {
            return null;
        }

        return getProject().loadClass(type, getClassLoader());
    }

    /**
     *  Set the type of this instruction.  Types that have no direct
     *  support will be converted accordingly.
     *
     *  @return this instruction, for method chaining
     */
    public abstract TypedInstruction setType(String type);

    /**
     *  Set the type of this instruction.  Types that have no direct
     *  support will be converted accordingly.
     *
     *  @return this instruction, for method chaining
     */
    public TypedInstruction setType(Class type) {
        if (type == null) {
            return setType((String) null);
        }

        return setType(type.getName());
    }

    /**
     *  Set the type of this instruction.  Types that have no direct
     *  support will be converted accordingly.
     *
     *  @return this instruction, for method chaining
     */
    public TypedInstruction setType(BCClass type) {
        if (type == null) {
            return setType((String) null);
        }

        return setType(type.getName());
    }
}

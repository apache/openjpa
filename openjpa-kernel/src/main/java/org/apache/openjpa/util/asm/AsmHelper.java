/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util.asm;

import java.lang.reflect.Method;

import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;

/**
 * Utility methods to deal with ASM bytecode
 * 
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public final class AsmHelper {
    private AsmHelper() {
        // utility class ct
    }

    /**
     * Calclates the proper Return instruction opcode for the given class
     * 
     * @param type the type to get returned
     * @return the proper Opcode RETURN, ARETURN, IRETURN, etc
     */
    public static int getReturnInsn(Class type) {
        if (type.equals(Void.TYPE)) {
            return Opcodes.RETURN;
        }
        if (type.isPrimitive()) {
            if (Integer.TYPE.equals(type)) {
                return Opcodes.IRETURN;
            }
            else if (Boolean.TYPE.equals(type)) {
                return Opcodes.IRETURN;
            }
            else if (Character.TYPE.equals(type)) {
                return Opcodes.IRETURN;
            }
            else if (Byte.TYPE.equals(type)) {
                return Opcodes.IRETURN;
            }
            else if (Short.TYPE.equals(type)) {
                return Opcodes.IRETURN;
            }
            else if (Float.TYPE.equals(type)) {
                return Opcodes.FRETURN;
            }
            else if (Long.TYPE.equals(type)) {
                return Opcodes.LRETURN;
            }
            else if (Double.TYPE.equals(type)) {
                return Opcodes.DRETURN;
            }
        }
        return Opcodes.ARETURN;
    }

    /**
     * Calclates the proper STORE instruction opcode for the given type
     *
     * @param type the type to get stored
     * @return the proper Opcode ISTORE, ASTORE, LSTORE, etc
     */
    public static int getStoreInsn(Class<?> type) {
        if (type.equals(Void.TYPE)) {
            throw new IllegalArgumentException("Void type cannot be stored");
        }
        if (type.isPrimitive()) {
            if (Integer.TYPE.equals(type)) {
                return Opcodes.ISTORE;
            }
            else if (Boolean.TYPE.equals(type)) {
                return Opcodes.ISTORE;
            }
            else if (Character.TYPE.equals(type)) {
                return Opcodes.ISTORE;
            }
            else if (Byte.TYPE.equals(type)) {
                return Opcodes.ISTORE;
            }
            else if (Short.TYPE.equals(type)) {
                return Opcodes.ISTORE;
            }
            else if (Float.TYPE.equals(type)) {
                return Opcodes.FSTORE;
            }
            else if (Long.TYPE.equals(type)) {
                return Opcodes.LSTORE;
            }
            else if (Double.TYPE.equals(type)) {
                return Opcodes.DSTORE;
            }
        }

        return Opcodes.ASTORE;
    }


    /**
     * Calclates the proper LOAD instruction opcode for the given type.
     * This is the appropriate bytecode instruction to load a value from a variable to the stack.
     *
     * @param type the type to get loaded
     * @return the proper Opcode ILOAD, ALOAD, LLOAD, etc
     */
    public static int getLoadInsn(Class<?> type) {
        if (type.equals(Void.TYPE)) {
            throw new IllegalArgumentException("Void type cannot be loaded");
        }
        if (type.isPrimitive()) {
            if (Integer.TYPE.equals(type)) {
                return Opcodes.ILOAD;
            }
            else if (Boolean.TYPE.equals(type)) {
                return Opcodes.ILOAD;
            }
            else if (Character.TYPE.equals(type)) {
                return Opcodes.ILOAD;
            }
            else if (Byte.TYPE.equals(type)) {
                return Opcodes.ILOAD;
            }
            else if (Short.TYPE.equals(type)) {
                return Opcodes.ILOAD;
            }
            else if (Float.TYPE.equals(type)) {
                return Opcodes.FLOAD;
            }
            else if (Long.TYPE.equals(type)) {
                return Opcodes.LLOAD;
            }
            else if (Double.TYPE.equals(type)) {
                return Opcodes.DLOAD;
            }
        }

        return Opcodes.ALOAD;
    }

    /**
     * Get the internal names for the given classes
     * @see Type#getInternalName(Class) 
     */
    public static String[] getInternalNames(Class[] classes) {
        String[] internalNames = new String[classes.length];

        for (int i=0; i<classes.length; i++) {
            internalNames[i] = Type.getInternalName(classes[i]);
        }
        return internalNames;
    }


    /**
     * get the ASM Types for the given classes
     * @see Type#getType(Method)
     */
    public static Type[] getParamTypes(Class[] params) {
        Type[] types = new Type[params.length];
        for (int i=0; i<types.length; i++) {
            types[i] = Type.getType(params[i]);
        }

        return types;
    }

}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.AbstractInsnNode;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

import serp.bytecode.BCClass;
import serp.bytecode.Project;

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
     * Read the binary bytecode from the class with the given name
     * @param classBytes the binary of the class
     * @return the ClassNode constructed from that class
     */
    public static ClassNode readClassNode(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        return classNode;
    }

    /**
     * Read the binary bytecode from the class with the given name
     * @param classLoader the ClassLoader to use
     * @param className the fully qualified class name to read. e.g. "org.mycorp.mypackage.MyEntity"
     * @return the ClassNode constructed from that class
     */
    public static ClassNode readClassNode(ClassLoader classLoader, String className) {
        ClassReader cr;
        final String classResourceName = className.replace(".", "/") + ".class";
        try (final InputStream classBytesStream = classLoader.getResourceAsStream(classResourceName)) {
            cr = new ClassReader(classBytesStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        return classNode;
    }

    /**
     * temporary helper class to convert BCClass to ASM ClassWriter
     * @deprecated must get removed when done with migrating from Serp to ASM
     */
    public static ClassWriterTracker toClassWriter(BCClass bcClass) {
        ClassReader cr = new ClassReader(bcClass.toByteArray());
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cr.accept(cw, 0);  // 0 -> don't skip anything
        ClassWriterTracker cwt = new ClassWriterTracker(cw, bcClass.getClassLoader());
        cwt.setName(bcClass.getName());

        return cwt;
    }

    /**
     * temporary helper class to convert ClassWriterTracker to BCClass
     * @deprecated must get removed when done with migrating from Serp to ASM
     */
    public static BCClass toBCClass(ClassWriterTracker cwt) {
        final byte[] classBytes = cwt.getCw().toByteArray();
        BCClass bcClass = new Project().loadClass(new ByteArrayInputStream(classBytes), cwt.getClassLoader());
        bcClass.setName(cwt.getName());
        return bcClass;
    }

    /**
     * temporary helper class to convert BCClass to ASM ClassNode
     * @deprecated must get removed when done with migrating from Serp to ASM
     */
    public static ClassNodeTracker toClassNode(BCClass bcClass) {
        ClassReader cr = new ClassReader(bcClass.toByteArray());
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        cr.accept(classNode, 0);

        return new ClassNodeTracker(classNode, bcClass.getClassLoader());
    }

    /**
     * Take the changes from ClassNodeTracker and read it into the given BCClass instance.
     * Effectively replace all the content of BCClass with the content from our ClassNode
     */
    public static void readIntoBCClass(ClassNodeTracker cnt, BCClass bcClass) {
        // sadly package scoped
        try {
            Method readMethod = BCClass.class.getDeclaredMethod("read", InputStream.class, ClassLoader.class);

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cnt.getClassNode().accept(cw);
            final byte[] classBytes = cw.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(classBytes);

            readMethod.setAccessible(true);
            readMethod.invoke(bcClass, bais, bcClass.getClassLoader());
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * @return true if the instruction is an LOAD instruction
     */
    public static boolean isLoadInsn(AbstractInsnNode insn) {
        return insn.getOpcode() == Opcodes.ALOAD
                || insn.getOpcode() == Opcodes.ILOAD
                || insn.getOpcode() == Opcodes.IALOAD
                || insn.getOpcode() == Opcodes.LLOAD
                || insn.getOpcode() == Opcodes.LALOAD
                || insn.getOpcode() == Opcodes.FLOAD
                || insn.getOpcode() == Opcodes.FALOAD
                || insn.getOpcode() == Opcodes.DLOAD
                || insn.getOpcode() == Opcodes.DALOAD
                || insn.getOpcode() == Opcodes.BALOAD
                || insn.getOpcode() == Opcodes.CALOAD
                || insn.getOpcode() == Opcodes.SALOAD;


    }

    /**
     * @return true if the instruction is an ALOAD_0
     */
    public static boolean isThisInsn(AbstractInsnNode insn) {
        return insn instanceof VarInsnNode
                && insn.getOpcode() == Opcodes.ALOAD
                && ((VarInsnNode)insn).var == 0;
    }
}

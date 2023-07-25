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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import org.apache.xbean.asm9.Attribute;
import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.AbstractInsnNode;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.FieldInsnNode;
import org.apache.xbean.asm9.tree.InsnList;
import org.apache.xbean.asm9.tree.InsnNode;
import org.apache.xbean.asm9.tree.IntInsnNode;
import org.apache.xbean.asm9.tree.LdcInsnNode;
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.TypeInsnNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

/**
 * Utility methods to deal with ASM bytecode
 * 
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public final class AsmHelper {
    public static final Type TYPE_OBJECT = Type.getType(Object.class);
    private static final char[] PRIMITIVE_DESCRIPTORS = {'V','Z','C','B','S','I','F','J','D'};
    public static final Attribute[] ATTRS = new Attribute[] {
            new RedefinedAttribute()
    };

    private AsmHelper() {
        // utility class ct
    }


    /**
     * Read the binary bytecode from the class with the given name
     * @param clazz the class to read into the ClassNode
     * @return the ClassNode constructed from that class
     */
    public static ClassNode readClassNode(Class<?> clazz) {
        int extPos = clazz.getName().lastIndexOf('.') + 1;
        String className = clazz.getName().substring(extPos);
        try (InputStream in = clazz.getResourceAsStream(className + ".class")) {
            ClassReader cr = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, ATTRS, 0);

            return classNode;
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Read the binary bytecode from the class with the given name
     * @param classLoader the ClassLoader to use
     * @param className the fully qualified class name to read. e.g. "org.mycorp.mypackage.MyEntity"
     * @return the ClassNode constructed from that class
     */
    public static ClassNode readClassNode(ClassLoader classLoader, String className) throws ClassNotFoundException {
        ClassReader cr;
        final String classResourceName = className.replace(".", "/") + ".class";
        try (final InputStream classBytesStream = classLoader.getResourceAsStream(classResourceName)) {
            cr = new ClassReader(classBytesStream);
        }
        catch (IOException e) {
            throw new ClassNotFoundException("Cannot read ClassNode for class " + className, e);
        }
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ATTRS, 0);

        return classNode;
    }

    public static byte[] getClassBytes(final String typeName)
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        final InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(typeName + ".class");
        if (stream == null) {
            return null;
        }
        try {
            int c;
            byte[] buffer = new byte[1024];
            while ((c = stream.read(buffer)) >= 0) {
                baos.write(buffer, 0, c);
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // no-op
            }
        }
        return baos.toByteArray();
    }
    /**
     * Create a byte[] of that class represented by the ClassNodeTracker
     */
    public static byte[] toByteArray(ClassNodeTracker cnt) {
        ClassWriter cw = new BCClassWriter(ClassWriter.COMPUTE_FRAMES, cnt.getClassLoader());
        cnt.getClassNode().accept(cw);
        return cw.toByteArray();
    }

    public static Optional<MethodNode> getMethodNode(ClassNode classNode, Method meth) {
        final String mDesc = Type.getMethodDescriptor(meth);
        return classNode.methods.stream()
                .filter(mn -> mn.name.equals(meth.getName()) && mn.desc.equals(mDesc))
                .findAny();
    }

    public static Optional<MethodNode> getMethodNode(ClassNode classNode, String methodName, Class<?> returnType, Class<?>... paramTypes) {
        Type[] parms = Arrays.stream(paramTypes)
                .map(Type::getType)
                .toArray(Type[]::new);

        final String mDesc = Type.getMethodDescriptor(Type.getType(returnType), parms);
        return classNode.methods.stream()
                .filter(mn -> mn.name.equals(methodName) && mn.desc.equals(mDesc))
                .findAny();
    }

    /**
     * Calclates the proper Return instruction opcode for the given class
     *
     * @param type the type to get returned
     * @return the proper Opcode RETURN, ARETURN, IRETURN, etc
     */
    public static int getReturnInsn(Class<?> type) {
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

    public static AbstractInsnNode getLoadConstantInsn(Object val) {
        if (val == null) {
            return new InsnNode(Opcodes.ACONST_NULL);
        }
        if (val instanceof Integer) {
            final int iVal = (Integer) val;
            switch (iVal) {
                case 0:
                    return new InsnNode(Opcodes.ICONST_0);
                case 1:
                    return new InsnNode(Opcodes.ICONST_1);
                case 2:
                    return new InsnNode(Opcodes.ICONST_2);
                case 3:
                    return new InsnNode(Opcodes.ICONST_3);
                case 4:
                    return new InsnNode(Opcodes.ICONST_4);
                case 5:
                    return new InsnNode(Opcodes.ICONST_5);
                default:
                    break;
            }
            if (iVal < 0 && iVal >= -128 || iVal >= 6 && iVal < 128) {
                // use bipush for small numbers
                return new IntInsnNode(Opcodes.BIPUSH, iVal);
            }
            else if (iVal < -128 && iVal >= -32768 || iVal >= 128 && iVal < 32768) {
                // use sipush for a bit bigger numbers
                return new IntInsnNode(Opcodes.SIPUSH, iVal);
            }
        }

        if (val instanceof Boolean) {
            if ((Boolean)val) {
                return new InsnNode(Opcodes.ICONST_1);
            }
            else {
                return new InsnNode(Opcodes.ICONST_0);
            }
        }

        if (val instanceof Long) {
            if ((Long) val == 0L) {
                return new InsnNode(Opcodes.LCONST_0);
            }
            if ((Long) val == 1L) {
                return new InsnNode(Opcodes.LCONST_1);
            }
        }

        if (val instanceof Float) {
            if ((Float) val == 0F) {
                return new InsnNode(Opcodes.FCONST_0);
            }
            if ((Float) val == 1F) {
                return new InsnNode(Opcodes.FCONST_1);
            }
            if ((Float) val == 2F) {
                return new InsnNode(Opcodes.FCONST_2);
            }
        }

        if (val instanceof Double) {
            if ((Double) val == 0D) {
                return new InsnNode(Opcodes.DCONST_0);
            }
            if ((Double) val == 1D) {
                return new InsnNode(Opcodes.DCONST_1);
            }
        }

        if (val instanceof Class) {
            if (boolean.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (char.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Character.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (int.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Integer.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (long.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Long.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (byte.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Byte.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (short.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Short.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (float.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Float.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            if (double.class.equals(val)) {
                return new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Double.class), "TYPE",
                                         Type.getDescriptor(Class.class));
            }
            return new LdcInsnNode(Type.getType((Class<?>) val));
        }

        return new LdcInsnNode(val);
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
    public static String[] getInternalNames(Class<?>[] classes) {
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
    public static Type[] getParamTypes(Class<?>[] params) {
        Type[] types = new Type[params.length];
        for (int i=0; i<types.length; i++) {
            types[i] = Type.getType(params[i]);
        }

        return types;
    }

    /**
     * Determine the 0-based index of the parameter of LOAD or STORE position varPos
     * @param methodNode
     * @param varPos the position on the stack
     * @return the index of the parameter which corresponds to this varPos
     */
    public static int getParamIndex(MethodNode methodNode, int varPos) {
        boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) > 0;
        if (!isStatic) {
            // only static methods start with 0
            // non-static have the this* on pos 0.
            varPos--;
        }
        final Type[] paramTypes = Type.getArgumentTypes(methodNode.desc);
        int pos = 0;
        for (int i=0; i<paramTypes.length; i++) {
            if (pos==varPos) {
                return i;
            }
            pos += paramTypes[i].getSize();
        }
        throw new IllegalArgumentException("Cannot determine parameter position " + varPos + " for method " + methodNode.name);
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

    /**
     * Get the corresponding LOAD instruction for the given STORE instruction.
     * 
     * @param storeInsnOpcode e.g. {@link Opcodes#ISTORE}
     * @throws IllegalArgumentException if the given opcode is not a STORE instruction
     */
    public static int getCorrespondingLoadInsn(int storeInsnOpcode) {
        switch (storeInsnOpcode) {
            case Opcodes.ISTORE:
                return Opcodes.ILOAD;
            case Opcodes.LSTORE:
                return Opcodes.LLOAD;
            case Opcodes.FSTORE:
                return Opcodes.FLOAD;
            case Opcodes.DSTORE:
                return Opcodes.DLOAD;
            case Opcodes.ASTORE:
                return Opcodes.ALOAD;
            case Opcodes.IASTORE:
                return Opcodes.IALOAD;
            case Opcodes.LASTORE:
                return Opcodes.LALOAD;
            case Opcodes.FASTORE:
                return Opcodes.FALOAD;
            case Opcodes.DASTORE:
                return Opcodes.DALOAD;
            case Opcodes.AASTORE:
                return Opcodes.AALOAD;
            case Opcodes.BASTORE:
                return Opcodes.BALOAD;
            case Opcodes.CASTORE:
                return Opcodes.CALOAD;
            case Opcodes.SASTORE:
                return Opcodes.SALOAD;
            default:
                throw new IllegalArgumentException("Not a STORE instruction: " + storeInsnOpcode);
        }
    }

    /**
     * Get the corresponding STORE instruction for the given LOAD instruction.
     * 
     * @param loadInsnOpcode e.g. {@link Opcodes#FLOAD}
     * @throws IllegalArgumentException if the given opcode is not a STORE instruction
     */
    public static int getCorrespondingStoreInsn(int loadInsnOpcode) {
        switch (loadInsnOpcode) {
            case Opcodes.ILOAD:
                return Opcodes.ISTORE;
            case Opcodes.LLOAD:
                return Opcodes.LSTORE;
            case Opcodes.FLOAD:
                return Opcodes.FSTORE;
            case Opcodes.DLOAD:
                return Opcodes.DSTORE;
            case Opcodes.ALOAD:
                return Opcodes.ASTORE;
            case Opcodes.IALOAD:
                return Opcodes.IASTORE;
            case Opcodes.LALOAD:
                return Opcodes.LASTORE;
            case Opcodes.FALOAD:
                return Opcodes.FASTORE;
            case Opcodes.DALOAD:
                return Opcodes.DASTORE;
            case Opcodes.AALOAD:
                return Opcodes.AASTORE;
            case Opcodes.BALOAD:
                return Opcodes.BASTORE;
            case Opcodes.CALOAD:
                return Opcodes.CASTORE;
            case Opcodes.SALOAD:
                return Opcodes.SASTORE;
            default:
                throw new IllegalArgumentException("Not a LOAD instruction: " + loadInsnOpcode);
        }
    }

    /**
     * @param typeDesc the internal type descriptor from the bytecode. See {@link ClassNode#name}
     * @param includeVoid if the Void.class type also counts as primitive
     */
    public static boolean isPrimitive(String typeDesc, boolean includeVoid) {
        if (typeDesc != null && typeDesc.length() == 1) {
            char typeChar = typeDesc.charAt(0);
            for (int i = includeVoid ? 0: 1; i<PRIMITIVE_DESCRIPTORS.length; i++) {
                if (PRIMITIVE_DESCRIPTORS[i] == typeChar) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the class from the described type
     * @param typeDesc
     * @return described class or {@code null} if it could not be loaded
     */
    public static Class<?> getDescribedClass(ClassLoader classLoader, String typeDesc) {
        if (typeDesc == null || typeDesc.isEmpty()) {
            return null;
        }
        if (typeDesc.charAt(0) == '[') {
            switch (typeDesc.charAt(1)) {
                case 'V':
                    throw new IllegalArgumentException("There is no such thing as a void array");
                case 'Z':
                    return boolean[].class;
                case 'C':
                    return char[].class;
                case 'B':
                    return byte[].class;
                case 'S':
                    return short[].class;
                case 'I':
                    return int[].class;
                case 'F':
                    return float[].class;
                case 'J':
                    return long[].class;
                case 'D':
                    return double[].class;
                default:
                    // some object array
                    Class<?> clazz = getClass(classLoader, typeDesc.substring(1));
                    return Array.newInstance(clazz, 0).getClass();
            }
        }

        switch (typeDesc.charAt(0)) {
            case 'V':
                return void.class;
            case 'Z':
                return boolean.class;
            case 'C':
                return char.class;
            case 'B':
                return byte.class;
            case 'S':
                return short.class;
            case 'I':
                return int.class;
            case 'F':
                return float.class;
            case 'J':
                return long.class;
            case 'D':
                return double.class;
            case 'L':
                if (typeDesc.charAt(typeDesc.length()-1) == ';')
                return getClass(classLoader, typeDesc.substring(1, typeDesc.length()-1));
            default:
                // some kind of class
                return getClass(classLoader, typeDesc);
        }
    }


    /**
     * Helper method to add the code necessary to throw the given
     * exception type, sans message.
     */
    public static InsnList throwException(Class type) {
        return throwException(type, null);
    }

    /**
     * Helper method to add the code necessary to throw the given
     * exception type, sans message.
     */
    public static InsnList throwException(Class type, String msg) {
        InsnList instructions = new InsnList();
        instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(type)));
        instructions.add(new InsnNode(Opcodes.DUP));
        if (msg != null) {
            instructions.add(AsmHelper.getLoadConstantInsn(msg));
        }
        String desc = msg != null
                ? Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class))
                : Type.getMethodDescriptor(Type.VOID_TYPE);
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(type),
                                            "<init>",
                                            desc));
        instructions.add(new InsnNode(Opcodes.ATHROW));

        return instructions;
    }


    public static Class<?> getClass(ClassLoader classLoader, String internalTypeName) {
        try {
            return Class.forName(internalTypeName.replace("/", "."), false, classLoader);
        }
        catch (NoClassDefFoundError | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Calculate the next local variable position.
     * For a non-static method the position 0 on the stack is the this pointer.
     * After that there are all the method parameters.
     * For a static method the method parameters begin at position zero.
     * This method does calculate the first unused stack position which can be used for xLOAD/xSTORE opterations, e.g.
     * <code>
     * int nextVarPos = AsmHelper.getLocalVarPos(myMethodNode);
     * instructions.add(AsmHelper.getLoadConstantInsn(4711));
     * instructions.add(new VarInsnNode(Opcodes.ISTORE, nextVarPos);
     * </code>
     *
     * @return the 0-based position on the stack at which the local variables can be located.
     */
    public static int getLocalVarPos(MethodNode meth) {
        final Type[] paramTypes = Type.getArgumentTypes(meth.desc);

        // stack position 0 is this pointer for non-static methods
        // In other words: for a static method the first free stack location is 1,
        // for non-static it is 2
        int pos = ((meth.access & Opcodes.ACC_STATIC) > 0) ? 0 : 1;

        // and now add the size of the parameters
        for (Type paramType : paramTypes) {
            pos += paramType.getSize();
        }

        return pos;
    }
}

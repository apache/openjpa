/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.enhance;

import java.lang.reflect.Constructor;

import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassNodeTracker;
import org.apache.openjpa.util.asm.EnhancementClassLoader;
import org.apache.openjpa.util.asm.EnhancementProject;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.FieldInsnNode;
import org.apache.xbean.asm9.tree.FieldNode;
import org.apache.xbean.asm9.tree.InsnList;
import org.apache.xbean.asm9.tree.InsnNode;
import org.apache.xbean.asm9.tree.JumpInsnNode;
import org.apache.xbean.asm9.tree.LabelNode;
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.TableSwitchInsnNode;
import org.apache.xbean.asm9.tree.TypeInsnNode;
import org.apache.xbean.asm9.tree.VarInsnNode;


/**
 * Factory for creating new {@link DynamicStorage} classes. Can be
 * extended to decorate/modify the generated instances behavior.
 *
 * @author Steve Kim
 * @author Mark Struberg rework to ASM
 * @since 0.3.2.0
 */
public class DynamicStorageGenerator {

    // prefix for generic generated classes.
    private static final String PREFIX = "openjpastorage$";

    /**
     * Constant to throw an exception on invalid index passed to type set/get
     * methods
     */
    protected static final int POLICY_EXCEPTION = 0;

    /**
     * Constant to not generate type set/get methods.
     */
    protected static final int POLICY_EMPTY = 1;

    /**
     * Constant to be as silent as possible during invalid index passed
     * to set/get type methods. On getting an Object, for example,
     * null will be returned.
     * However, on primitive gets, an exception will be thrown.
     */
    protected static final int POLICY_SILENT = 2;

    // wrappers for primitive types
    private static final Class[][] WRAPPERS = new Class[][]{
        { boolean.class, Boolean.class },
        { byte.class, Byte.class },
        { char.class, Character.class },
        { int.class, Integer.class },
        { short.class, Short.class },
        { long.class, Long.class },
        { float.class, Float.class },
        { double.class, Double.class },
    };

    // primitive types
    private static final int[] TYPES = new int[]{
        JavaTypes.BOOLEAN,
        JavaTypes.BYTE,
        JavaTypes.CHAR,
        JavaTypes.INT,
        JavaTypes.SHORT,
        JavaTypes.LONG,
        JavaTypes.FLOAT,
        JavaTypes.DOUBLE,
        JavaTypes.OBJECT
    };

    // the project/classloader for the classes.
    private final EnhancementProject _project = new EnhancementProject();
    private final EnhancementClassLoader _loader = new EnhancementClassLoader(_project, DynamicStorage.class.getClassLoader());

    /**
     * Generate a generic {@link DynamicStorage} instance with the given
     * array of {@link JavaTypes} constants and the given object as
     * the user key for generation.
     */
    public DynamicStorage generateStorage(int[] types, Object obj) {
        if (obj == null)
            return null;

        String name = getClassName(obj);
        ClassNodeTracker bc = _project.loadClass(name);
        declareClasses(bc);
        addDefaultConstructor(bc);

        int objectCount = declareFields(types, bc);
        addFactoryMethod(bc);
        addFieldCount(bc, types, objectCount);
        addSetMethods(bc, types, objectCount);
        addGetMethods(bc, types);
        addInitialize(bc, objectCount);
        decorate(obj, bc, types);
        return createFactory(bc);
    }

    private void addDefaultConstructor(ClassNodeTracker cnt) {
        ClassNode classNode = cnt.getClassNode();
        // find the default constructor
        final boolean hasDefaultCt = classNode.methods.stream()
                .anyMatch(m -> m.name.equals("<init>") && m.desc.equals("()V"));
        if (!hasDefaultCt) {
            MethodNode ctNode = new MethodNode(Opcodes.ACC_PUBLIC,
                                               "<init>",
                                               Type.getMethodDescriptor(Type.VOID_TYPE),
                                               null, null);
            ctNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            ctNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.superName,
                                                       "<init>", "()V"));
            ctNode.instructions.add(new InsnNode(Opcodes.RETURN));
            classNode.methods.add(ctNode);
        }
    }



    /**
     * Return a class name to use for the given user key. By default,
     * returns the stringified key prefixed by PREFIX.
     */
    protected String getClassName(Object obj) {
        return PREFIX + obj.toString();
    }

    /**
     * Return the default field ACCESS constant for generated fields from
     * {@link Opcodes}.
     */
    protected int getFieldAccess() {
        return Opcodes.ACC_PRIVATE;
    }

    /**
     * Return the name for the generated field at the given index. Returns
     * <code>"field" + i</code> by default.
     */
    protected String getFieldName(int index) {
        return "field" + index;
    }

    /**
     * Return the policy constant for how to create type methods.
     */
    protected int getCreateFieldMethods(int type) {
        return POLICY_EXCEPTION;
    }

    /**
     * Decorate the generated class.
     */
    protected void decorate(Object obj, ClassNodeTracker cls, int[] types) {
    }

    /**
     * Create a stub factory instance for the given class.
     */
    protected DynamicStorage createFactory(ClassNodeTracker bc) {
        try {
            Class cls = Class.forName(bc.getClassNode().name.replace("/", "."), false, _loader);
            Constructor cons = cls.getConstructor((Class[]) null);
            DynamicStorage data = (DynamicStorage) cons.newInstance((Object[]) null);
            _project.clear(); // remove old refs
            return data;
        } catch (Throwable t) {
            throw new InternalException("cons-access", t).setFatal(true);
        }
    }

    /**
     * Add interface or superclass declarations to the generated class.
     */
    protected void declareClasses(ClassNodeTracker bc) {
        bc.declareInterface(DynamicStorage.class);
    }

    /**
     * Implement the newInstance method.
     */
    private void addFactoryMethod(ClassNodeTracker bc) {
        final ClassNode classNode = bc.getClassNode();
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC,
                                           "newInstance",
                                           Type.getMethodDescriptor(Type.getType(DynamicStorage.class)),
                                           null, null);
        classNode.methods.add(method);
        InsnList instructions = method.instructions;
        instructions.add(new TypeInsnNode(Opcodes.NEW, classNode.name));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            classNode.name,
                                            "<init>",
                                            Type.getMethodDescriptor(Type.VOID_TYPE)));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * Implement getFieldCount/getObjectCount.
     */
    private void addFieldCount(ClassNodeTracker bc, int[] types, int objectCount) {
        final ClassNode classNode = bc.getClassNode();
        MethodNode getFc = new MethodNode(Opcodes.ACC_PUBLIC,
                                           "getFieldCount",
                                           Type.getMethodDescriptor(Type.INT_TYPE),
                                           null, null);
        classNode.methods.add(getFc);
        getFc.instructions.add(AsmHelper.getLoadConstantInsn(types.length));
        getFc.instructions.add(new InsnNode(Opcodes.IRETURN));

        MethodNode getOc = new MethodNode(Opcodes.ACC_PUBLIC,
                                          "getObjectCount",
                                          Type.getMethodDescriptor(Type.INT_TYPE),
                                          null, null);
        classNode.methods.add(getOc);
        getOc.instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
        getOc.instructions.add(new InsnNode(Opcodes.IRETURN));
    }

    /**
     * Implement initialize.
     */
    private void addInitialize(ClassNodeTracker bc, int objectCount) {
        final ClassNode classNode = bc.getClassNode();
        MethodNode initMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                          "initialize",
                                          Type.getMethodDescriptor(Type.VOID_TYPE),
                                          null, null);
        classNode.methods.add(initMeth);
        InsnList instructions = initMeth.instructions;

        LabelNode lblEndIf = null;
        if (objectCount > 0) {
            // if (objects == null)
            // 		objects = new Object[objectCount];
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
            lblEndIf = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIf));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
            instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(Object.class)));
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
        }

        if (lblEndIf != null) {
            instructions.add(lblEndIf);
        }
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Declare the primitive fields and the object field.
     */
    private int declareFields(int[] types, ClassNodeTracker bc) {
        ClassNode classNode = bc.getClassNode();
        classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "objects", Type.getDescriptor(Object[].class), null, null));

        int objectCount = 0;
        Class type;
        for (int i = 0; i < types.length; i++) {
            type = forType(types[i]);
            if (type == Object.class)
                objectCount++;
            else {
                classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, getFieldName(i), Type.getDescriptor(type), null, null));
            }
        }
        return objectCount;
    }

    /**
     * Add all the typed set by index method.
     */
    private void addSetMethods(ClassNodeTracker bc, int[] types, int totalObjects) {
        for (int type : TYPES) {
            addSetMethod(type, bc, types, totalObjects);
        }
    }

    /**
     * Add the typed set by index method.
     */
    private void addSetMethod(int typeCode, ClassNodeTracker bc, int[] types, int totalObjects) {
        int handle = getCreateFieldMethods(typeCode);
        if (handle == POLICY_EMPTY) {
            return;
        }

        Class type = forType(typeCode);

        // public void set<Type> (int field, <type> val)
        String name = Object.class.equals(type) ? "Object" : StringUtil.capitalize(type.getName());
        name = "set" + name;

        ClassNode classNode = bc.getClassNode();
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC,
                                           name,
                                           Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(type)),
                                           null, null);
        classNode.methods.add(method);
        InsnList instructions = method.instructions;

        // switch (field)
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 1)); // switch on first parameter which is an int

        LabelNode defLbl = new LabelNode();
        TableSwitchInsnNode switchNd = new TableSwitchInsnNode(0, types.length - 1, defLbl);
        instructions.add(switchNd);


        int objectCount = 0;
        for (int i = 0; i < types.length; i++) {
            // default: throw new IllegalArgumentException
            if (!isCompatible(types[i], typeCode)) {
                LabelNode caseLbl = new LabelNode();
                switchNd.labels.add(caseLbl);
                instructions.add(caseLbl);
                instructions.add(getDefaultSetInstructions(handle));
                continue;
            }

            LabelNode caseLbl = new LabelNode();
            switchNd.labels.add(caseLbl);
            instructions.add(caseLbl);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

            if (typeCode >= JavaTypes.OBJECT) {
                // if (objects == null)
                // 		objects = new Object[totalObjects];
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));

                LabelNode lblEndNonNull = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndNonNull));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(AsmHelper.getLoadConstantInsn(totalObjects));
                instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(Object.class)));
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));

                instructions.add(lblEndNonNull);

                // objects[objectCount] = val;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
                instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd param
                instructions.add(new InsnNode(Opcodes.AASTORE));

                objectCount++;
            } else {
                // case i: fieldi = val;
                instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(type), 2)); // 2nd param is primitive
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, "field" + i, Type.getInternalName(type)));
            }
            // return
            instructions.add(new InsnNode(Opcodes.RETURN));
        }


        // default:
        instructions.add(defLbl);
        instructions.add(getDefaultSetInstructions(handle));
    }

    private static InsnList getDefaultSetInstructions(int handle) {
        InsnList defaultInsns;
        if (handle == POLICY_SILENT) {
            defaultInsns = new InsnList();
            defaultInsns.add(new InsnNode(Opcodes.RETURN));
        }
        else {
            defaultInsns = AsmHelper.throwException(IllegalArgumentException.class);
        }
        return defaultInsns;
    }

    /**
     * Add all typed get by index method for the given fields.
     */
    private void addGetMethods(ClassNodeTracker bc, int[] types) {
        for (int type : TYPES) {
            addGetMethod(type, bc, types);
        }
    }

    /**
     * Add typed get by index method.
     */
    private void addGetMethod(int typeCode, ClassNodeTracker bc, int[] types) {
        int handle = getCreateFieldMethods(typeCode);
        if (handle == POLICY_EMPTY) {
            return;
        }
        Class type = forType(typeCode);

        // public <type> get<Type>Field (int field)
        String name = "get" + (Object.class.equals(type) ? "Object" : StringUtil.capitalize(type.getName()));

        ClassNode classNode = bc.getClassNode();
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                         name,
                                         Type.getMethodDescriptor(Type.getType(type), Type.INT_TYPE),
                                         null, null);
        classNode.methods.add(meth);
        InsnList instructions = meth.instructions;

        // switch (field)
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 1)); // switch on first parameter which is an int

        LabelNode defLbl = new LabelNode();
        TableSwitchInsnNode switchNd = new TableSwitchInsnNode(0, types.length - 1, defLbl);
        instructions.add(switchNd);


        int objectCount = 0;
        for (int i = 0; i < types.length; i++) {
            // default: throw new IllegalArgumentException
            if (!isCompatible(types[i], typeCode)) {
                LabelNode caseLbl = new LabelNode();
                switchNd.labels.add(caseLbl);
                instructions.add(caseLbl);
                instructions.add(getDefaultGetInstructions(typeCode, handle));
                continue;
            }

            LabelNode caseLbl = new LabelNode();
            switchNd.labels.add(caseLbl);
            instructions.add(caseLbl);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            if (typeCode >= JavaTypes.OBJECT) {
                // if (objects == null)
                // 		return null;
                // return objects[objectCount];
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));

                LabelNode lblEndNonNull = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndNonNull));
                instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                instructions.add(new InsnNode(Opcodes.ARETURN));

                instructions.add(lblEndNonNull);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
                instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
                instructions.add(new InsnNode(Opcodes.AALOAD));
                instructions.add(new InsnNode(Opcodes.ARETURN));

                objectCount++;
            } else {
                // case i: return fieldi;
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "field" + i, Type.getInternalName(type)));
                instructions.add(new InsnNode(AsmHelper.getReturnInsn(type)));
            }
        }

        // default:
        instructions.add(defLbl);
        instructions.add(getDefaultGetInstructions(typeCode, handle));
    }

    private static InsnList getDefaultGetInstructions(int typeCode, int handle) {
        InsnList defaultInsns;
        if (typeCode == JavaTypes.OBJECT && handle == POLICY_SILENT) {
            defaultInsns = new InsnList();
            defaultInsns.add(new InsnNode(Opcodes.ACONST_NULL));
            defaultInsns.add(new InsnNode(Opcodes.ARETURN));
        }
        else {
            defaultInsns = AsmHelper.throwException(IllegalArgumentException.class);
        }
        return defaultInsns;
    }

    /////////////
    // Utilities
    /////////////

    /**
     * Add a bean field of the given name and type.
     */
    protected FieldNode addBeanField(ClassNodeTracker bc, String name, Class type) {
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }

        ClassNode classNode = bc.getClassNode();

        // private <type> <name>
        FieldNode field = new FieldNode(getFieldAccess(), name, Type.getDescriptor(type), null, null);
        classNode.fields.add(field);

        name = StringUtil.capitalize(name);

        // getter
        {
            String prefix = (type == boolean.class) ? "is" : "get";
            MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                             prefix + name,
                                             Type.getMethodDescriptor(Type.getType(type)),
                                             null, null);
            classNode.methods.add(meth);
            meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            meth.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, field.name, Type.getDescriptor(type)));
            meth.instructions.add(new InsnNode(AsmHelper.getReturnInsn(type)));
        }

        // setter
        {
            MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                             "set" + name,
                                             Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(type)),
                                             null, null);
            classNode.methods.add(meth);

            meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            meth.instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(type), 1)); // value parameter
            meth.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, field.name, Type.getDescriptor(type)));
            meth.instructions.add(new InsnNode(Opcodes.RETURN));

        }
        return field;
    }

    /**
     * Return true if the given field type and storage type are compatible.
     */
    protected boolean isCompatible(int fieldType, int storageType) {
        if (storageType == JavaTypes.OBJECT)
            return fieldType >= JavaTypes.OBJECT;
        return fieldType == storageType;
    }

    /**
     * Return the proper type for the given {@link JavaTypes} constant.
     */
    protected Class forType(int type) {
        switch (type) {
            case JavaTypes.BOOLEAN:
                return boolean.class;
            case JavaTypes.BYTE:
                return byte.class;
            case JavaTypes.CHAR:
                return char.class;
            case JavaTypes.INT:
                return int.class;
            case JavaTypes.SHORT:
                return short.class;
            case JavaTypes.LONG:
                return long.class;
            case JavaTypes.FLOAT:
                return float.class;
            case JavaTypes.DOUBLE:
                return double.class;
        }
        return Object.class;
    }

    /**
     * get the wrapper for the given {@link JavaTypes} constant.
     */
    protected Class getWrapper(int type) {
        return getWrapper(forType(type));
    }

    /**
     * Get the wrapper for the given type.
     */
    protected Class getWrapper(Class c) {
        for (Class[] wrapper : WRAPPERS) {
            if (wrapper[0].equals(c)) {
                return wrapper[1];
            }
        }
        return c;
    }
}

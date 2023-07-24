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
package org.apache.openjpa.datacache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PCDataGenerator;
import org.apache.openjpa.kernel.AbstractPCData;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassNodeTracker;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.FieldInsnNode;
import org.apache.xbean.asm9.tree.InsnList;
import org.apache.xbean.asm9.tree.InsnNode;
import org.apache.xbean.asm9.tree.JumpInsnNode;
import org.apache.xbean.asm9.tree.LabelNode;
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.TypeInsnNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

import serp.bytecode.BCClass;
import serp.bytecode.BCField;
import serp.bytecode.BCMethod;
import serp.bytecode.Code;
import serp.bytecode.Instruction;
import serp.bytecode.JumpInstruction;
import serp.bytecode.Project;

/**
 * A {@link PCDataGenerator} instance which generates properly
 * synchronized instances suitable for use in the cache. In addition,
 * proper timed behavior is added.
 *
 * @author Steve Kim
 * @since 0.3.3.0
 */
public class DataCachePCDataGenerator extends PCDataGenerator {

    public static final String POSTFIX = "datacache";

    private static final Set _synchs = new HashSet(Arrays.asList
        (new String []{ "getData", "setData", "clearData", "getImplData",
            "setImplData", "setIntermediate", "getIntermediate",
            "isLoaded", "setLoaded", "setVersion", "getVersion", "store"
        }));

    public DataCachePCDataGenerator(OpenJPAConfiguration conf) {
        super(conf);
    }

    @Override
    protected String getUniqueName(Class type) {
        return super.getUniqueName(type) + POSTFIX;
    }

    @Override
    protected void finish(DynamicPCData data, ClassMetaData meta) {
        int timeout = meta.getDataCacheTimeout();
        if (timeout > 0)
            ((Timed) data).setTimeout(timeout + System.currentTimeMillis());
        else
            ((Timed) data).setTimeout(-1);
    }

    @Override
    protected void decorate(ClassNodeTracker cnt, ClassMetaData meta) {
        enhanceToData(cnt);
        enhanceToNestedData(cnt);
        replaceNewEmbeddedPCData(cnt);
        addSynchronization(cnt);

        //X TODO REMOVE
        BCClass _bc = new Project().loadClass(cnt.getClassNode().name.replace("/", "."));
        AsmHelper.readIntoBCClass(cnt, _bc);

        addTimeout(_bc);

        cnt.setClassNode(AsmHelper.toClassNode(cnt.getProject(), _bc).getClassNode());
    }

    private void enhanceToData(ClassNodeTracker cnt) {
        ClassNode classNode = cnt.getClassNode();
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                         "toData",
                                         Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT,
                                                                  Type.getType(FieldMetaData.class),
                                                                  AsmHelper.TYPE_OBJECT,
                                                                  Type.getType(StoreContext.class)),
                                         null, null);
        classNode.methods.add(meth);
        final InsnList instructions = meth.instructions;

        // if (fmd.isLRS ()))
        // 		return NULL;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, FieldMetaData
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            Type.getInternalName(FieldMetaData.class),
                                            "isLRS",
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE)));
        LabelNode lblEndIfEq = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIfEq));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(AbstractPCData.class),
                                           "NULL", AsmHelper.TYPE_OBJECT.getDescriptor()));
        instructions.add(new InsnNode(Opcodes.ARETURN));

        instructions.add(lblEndIfEq);
        // super.toData (fmd, val, ctx);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, FieldMetaData
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd param, Object
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // 3rd param, StoreContext
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(AbstractPCData.class),
                                            "toData",
                                            Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT,
                                                                     Type.getType(FieldMetaData.class),
                                                                     AsmHelper.TYPE_OBJECT,
                                                                     Type.getType(StoreContext.class))));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    private void enhanceToNestedData(ClassNodeTracker cnt) {
        ClassNode classNode = cnt.getClassNode();
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                         "toNestedData",
                                         Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT,
                                                                  Type.getType(ValueMetaData.class),
                                                                  AsmHelper.TYPE_OBJECT,
                                                                  Type.getType(StoreContext.class)),
                                         null, null);
        classNode.methods.add(meth);
        final InsnList instructions = meth.instructions;

        // if (val == null)
        // 		return null;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, ValueMetaData

        LabelNode lblEndIfNN = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIfNN));
        instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        instructions.add(new InsnNode(Opcodes.ARETURN));

        instructions.add(lblEndIfNN);
        // int type = vmd.getDeclaredTypeCode ();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, ValueMetaData
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(ValueMetaData.class),
                                            "getDeclaredTypeCode",
                                            Type.getMethodDescriptor(Type.INT_TYPE)));
        int varPos = AsmHelper.getLocalVarPos(meth);
        instructions.add(new VarInsnNode(Opcodes.ISTORE, varPos));

        // if (type != JavaTypes.COLLECTION &&
        // 	   type != JavaTypes.MAP &&
        // 	   type != JavaTypes.ARRAY)
        // 	   return super.toNestedData(type, val, ctx);
        // 	else
        // 		return NULL;
        LabelNode lblEndIf = new LabelNode();
        instructions.add(new VarInsnNode(Opcodes.ILOAD, varPos));
        instructions.add(AsmHelper.getLoadConstantInsn(JavaTypes.COLLECTION));
        instructions.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, lblEndIf));
        instructions.add(new VarInsnNode(Opcodes.ILOAD, varPos));
        instructions.add(AsmHelper.getLoadConstantInsn(JavaTypes.MAP));
        instructions.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, lblEndIf));
        instructions.add(new VarInsnNode(Opcodes.ILOAD, varPos));
        instructions.add(AsmHelper.getLoadConstantInsn(JavaTypes.ARRAY));
        instructions.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, lblEndIf));

        // if block
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, ValueMetaData
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd param, Object
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // 3rd param, StoreContext
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(AbstractPCData.class),
                                            "toNestedData",
                                            Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT,
                                                                     Type.getType(ValueMetaData.class),
                                                                     AsmHelper.TYPE_OBJECT,
                                                                     Type.getType(StoreContext.class))));
        instructions.add(new InsnNode(Opcodes.ARETURN));

        // end if
        instructions.add(lblEndIf);
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(AbstractPCData.class),
                                           "NULL", AsmHelper.TYPE_OBJECT.getDescriptor()));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    private void replaceNewEmbeddedPCData(ClassNodeTracker cnt) {
        ClassNode classNode = cnt.getClassNode();
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                         "newEmbeddedPCData",
                                         Type.getMethodDescriptor(Type.getType(AbstractPCData.class),
                                                                  Type.getType(OpenJPAStateManager.class)),
                                         null, null);
        classNode.methods.add(meth);
        final InsnList instructions = meth.instructions;

        instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(DataCachePCDataImpl.class)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(OpenJPAStateManager.class),
                                            "getId",
                                            Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT)));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(OpenJPAStateManager.class),
                                            "getMetaData",
                                            Type.getMethodDescriptor(Type.getType(ClassMetaData.class))));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(DataCachePCDataImpl.class),
                                            "<init>",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.TYPE_OBJECT, Type.getType(ClassMetaData.class))));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * Add a bean field of the given name and type.
     */
    @Deprecated
    private BCField addBeanField(BCClass bc, String name, Class type) {
        if (name == null)
            throw new IllegalArgumentException("name == null");

        // private <type> <name>
        BCField field = bc.declareField(name, type);
        field.setAccessFlags(getFieldAccess());
        name = StringUtil.capitalize(name);

        // getter
        String prefix = (type == boolean.class) ? "is" : "get";
        BCMethod method = bc.declareMethod(prefix + name, type, null);
        method.makePublic();
        Code code = method.getCode(true);
        code.aload().setThis();
        code.getfield().setField(field);
        code.xreturn().setType(type);
        code.calculateMaxStack();
        code.calculateMaxLocals();

        // setter
        method = bc.declareMethod("set" + name, void.class,
                                  new Class[]{ type });
        method.makePublic();
        code = method.getCode(true);
        code.aload().setThis();
        code.xload().setParam(0).setType(type);
        code.putfield().setField(field);
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
        return field;
    }

    private void addTimeout(BCClass bc) {
        bc.declareInterface(DataCachePCData.class);
        bc.declareInterface(Timed.class);

        // public boolean isTimedOut ();
        BCField field = addBeanField(bc, "timeout", long.class);
        BCMethod meth = bc.declareMethod("isTimedOut", boolean.class, null);
        Code code = meth.getCode(true);

        // if (timeout == -1) ...
        code.aload().setThis();
        code.getfield().setField(field);
        code.constant().setValue(-1L);
        code.lcmp();
        JumpInstruction ifneg = code.ifeq();

        // if (timeout >= System.currentTimeMillis ())
        code.aload().setThis();
        code.getfield().setField(field);
        code.invokestatic().setMethod(System.class, "currentTimeMillis",
            long.class, null);
        code.lcmp();
        JumpInstruction ifnexp = code.ifge();

        // return true;
        code.constant().setValue(1);

        // ... else return false;
        JumpInstruction go2 = code.go2();
        Instruction flse = code.constant().setValue(0);
        ifneg.setTarget(flse);
        ifnexp.setTarget(flse);
        go2.setTarget(code.ireturn());

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    private void addSynchronization(ClassNodeTracker cnt) {
        final ClassNode classNode = cnt.getClassNode();
        for (MethodNode m : classNode.methods) {
            if ((m.access & Opcodes.ACC_PUBLIC) > 0 && _synchs.contains(m.name)) {
                m.access |= Opcodes.ACC_SYNCHRONIZED;
            }
        }

        // add synchronized isLoaded call.
        // public synchronized boolean isLoaded (int field)
        // {
        // 		return super.isLoaded (field);
        // }
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED,
                                         "isLoaded",
                                         Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE),
                                         null, null);
        classNode.methods.add(meth);
        InsnList instructions = meth.instructions;

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 1)); // 1st parameter int
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(AbstractPCData.class),
                                            "isLoaded",
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
        instructions.add(new InsnNode(Opcodes.IRETURN));
    }

    /**
     * Simple interface to give access to expiration time.
     */
    public interface Timed {

        void setTimeout(long time);
    }
}

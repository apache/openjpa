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

import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.AbstractPCData;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCData;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassNodeTracker;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.FieldInsnNode;
import org.apache.xbean.asm9.tree.FieldNode;
import org.apache.xbean.asm9.tree.InsnList;
import org.apache.xbean.asm9.tree.InsnNode;
import org.apache.xbean.asm9.tree.JumpInsnNode;
import org.apache.xbean.asm9.tree.LabelNode;
import org.apache.xbean.asm9.tree.LookupSwitchInsnNode;
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.TypeInsnNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

import serp.bytecode.BCClass;
import serp.bytecode.BCMethod;
import serp.bytecode.Code;
import serp.bytecode.Constants;
import serp.bytecode.Instruction;
import serp.bytecode.JumpInstruction;
import serp.bytecode.Project;

/**
 * Generates {@link PCData} instances which avoid primitive wrappers
 * to optimize memory use and performance at the cost of slightly higher
 * startup time.
 *
 * @author Steve Kim
 * @author Mark Struberg rework to ASM
 * @since 0.3.2
 */
public class PCDataGenerator extends DynamicStorageGenerator {

    private static final Localizer _loc = Localizer.forPackage
        (PCDataGenerator.class);

    protected static final String POSTFIX = "$openjpapcdata";

    private final Map<Class<?>, DynamicStorage> _generated = new ConcurrentHashMap<>();
    private final OpenJPAConfiguration _conf;
    private final Log _log;

    public PCDataGenerator(OpenJPAConfiguration conf) {
        _conf = conf;
        _log = _conf.getLogFactory().getLog(OpenJPAConfiguration.LOG_ENHANCE);
    }

    /**
     * Return the configuration.
     */
    public OpenJPAConfiguration getConfiguration() {
        return _conf;
    }

    /**
     * Return a {@link PCData} instance for the given oid and metadata.
     */
    public PCData generatePCData(Object oid, ClassMetaData meta) {
        if (meta == null)
            return null;
        Class<?> type = meta.getDescribedType();
        DynamicStorage storage = _generated.get(type);
        if (storage == null) {
            storage = generateStorage(meta);
            _generated.put(type, storage);
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("pcdata-created", type.getName(), meta));
        }
        DynamicPCData data = (DynamicPCData) storage.newInstance();
        data.setId(oid);
        data.setStorageGenerator(this);
        finish(data, meta);
        return data;
    }

    /**
     * Actually generate the factory instance.
     */
    private DynamicStorage generateStorage(ClassMetaData meta) {
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("pcdata-generate", meta));

        FieldMetaData[] fields = meta.getFields();
        int[] types = new int[fields.length];
        for (int i = 0; i < types.length; i++)
            types[i] = replaceType(fields[i]);
        return generateStorage(types, meta);
    }

    /**
     * Perform any final actions before the pcdata is returned to client code.
     */
    protected void finish(DynamicPCData data, ClassMetaData meta) {
    }

    @Override
    protected int getCreateFieldMethods(int typeCode) {
        if (typeCode >= JavaTypes.OBJECT)
            return POLICY_SILENT;
        // don't bother creating set/get<Primitive> methods
        return POLICY_EMPTY;
    }

    @Override
    protected void declareClasses(ClassNodeTracker bc) {
        super.declareClasses(bc);
        bc.declareInterface(DynamicPCData.class);
        bc.getClassNode().superName = Type.getInternalName(AbstractPCData.class);
    }

    @Override
    protected final String getClassName(Object obj) {
        return getUniqueName(((ClassMetaData) obj).getDescribedType());
    }

    /**
     * Creates a unique name for the given type's pcdata implementation.
     */
    protected String getUniqueName(Class<?> type) {
        return type.getName() + "$" + System.identityHashCode(type) + POSTFIX;
    }

    @Override
    protected final void decorate(Object obj, ClassNodeTracker bc, int[] types) {
        super.decorate(obj, bc, types);
        ClassMetaData meta = (ClassMetaData) obj;

        enhanceConstructor(bc);
        addBaseFields(bc);
        addImplDataMethods(bc, meta);
        addGetType(bc, meta);
        addVersionMethods(bc);
        addFieldImplDataMethods(bc, meta);
        addLoadMethod(bc, meta);
        addLoadWithFieldsMethod(bc, meta);
        addStoreMethods(bc, meta);

        BCClass _bc = new Project().loadClass(bc.getClassNode().name.replace("/", "."));
        AsmHelper.readIntoBCClass(bc, _bc);

        addNewEmbedded(_bc);
        addGetData(_bc);

        bc = AsmHelper.toClassNode(bc.getProject(), _bc);

        decorate(bc, meta);
    }

    /**
     * Apply additional decoration to generated class.
     */
    protected void decorate(ClassNodeTracker bc, ClassMetaData meta) {
    }

    /**
     * Enhance constructor to initialize fields
     */
    private void enhanceConstructor(ClassNodeTracker bc) {
        ClassNode classNode = bc.getClassNode();

        // find the default constructor
        MethodNode defaultCt = classNode.methods.stream()
                .filter(m -> m.name.equals("<init>") && m.desc.equals("()V"))
                .findFirst()
                .get();

        InsnList instructions = new InsnList();


        // private BitSet loaded = new BitSet();
        FieldNode loaded = addBeanField(bc, "loaded", BitSet.class);

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(BitSet.class)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(AsmHelper.getLoadConstantInsn(classNode.fields.size()));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(BitSet.class),
                                            "<init>",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
        instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, loaded.name, loaded.desc));

        defaultCt.instructions.insertBefore(defaultCt.instructions.getLast(), instructions);
    }

    /**
     * Have to load the type since it may not be available to the
     * same classloader (i.e. rar vs. ear). The context classloader
     * (i.e. the user app classloader) should be fine.
     */
    private void addGetType(ClassNodeTracker bc, ClassMetaData meta) {
        ClassNode classNode = bc.getClassNode();
        FieldNode typeField = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "type", Type.getDescriptor(Class.class), null, null);
        classNode.fields.add(typeField);

        // public Class getType() {
        MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC,
                                           "getType",
                                           Type.getMethodDescriptor(Type.getType(Class.class)),
                                           null, null);
        classNode.methods.add(getter);

        InsnList instructions = getter.instructions;

        // use name as constant filled with meta.getDescribedType().getName()
        // if (type == null) {
        //     type = PCDataGenerator.getType(name)
        // }
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, typeField.name, typeField.desc));

        LabelNode lblEndIfNN = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIfNN));

        // actual type = PCDataGenerator.getType(name)
        instructions.add(AsmHelper.getLoadConstantInsn(meta.getDescribedType().getName()));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                            Type.getInternalName(PCDataGenerator.class),
                                            "getType",
                                            Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(String.class))));
        instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, typeField.name, typeField.desc));

        instructions.add(lblEndIfNN);
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, typeField.name, typeField.desc));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    public static Class<?> getType(String className) {
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        catch (ClassNotFoundException cnfe) {
            throw new InternalException();
        }
    }

    /**
     * Declare standard dynamic pcdata fields.
     */
    private void addBaseFields(ClassNodeTracker bc) {
        addBeanField(bc, "id", Object.class);
        FieldNode field = addBeanField(bc, "storageGenerator", PCDataGenerator.class);
        field.access |= Constants.ACCESS_TRANSIENT;
    }

    /**
     * Add methods for loading and storing class-level impl data.
     */
    private void addImplDataMethods(ClassNodeTracker bc, ClassMetaData meta) {
        ClassNode classNode = bc.getClassNode();

        // void storeImplData(OpenJPAStateManager);
        MethodNode storeM = new MethodNode(Opcodes.ACC_PUBLIC,
                                         "storeImplData",
                                         Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class)),
                                         null, null);
        classNode.methods.add(storeM);
        InsnList instructions = storeM.instructions;

        FieldNode impl = null;
        if (!usesImplData(meta)) {
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
        else {
            // if (sm.isImplDataCacheable())
            //         setImplData(sm.getImplData());
            impl = addBeanField(bc, "implData", Object.class);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "isImplDataCacheable",
                                                Type.getMethodDescriptor(Type.BOOLEAN_TYPE)));
            LabelNode lblEndIfEq = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIfEq));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getImplData",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT)));

            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                "setImplData",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.TYPE_OBJECT)));

            instructions.add(lblEndIfEq);
            instructions.add(new InsnNode(Opcodes.RETURN));
        }

        // void loadImplData(OpenJPAStateManager);
        MethodNode loadM = new MethodNode(Opcodes.ACC_PUBLIC,
                                           "loadImplData",
                                           Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class)),
                                           null, null);
        classNode.methods.add(loadM);
        instructions = loadM.instructions;

        if (!usesImplData(meta)) {
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
        else {
            // if (sm.getImplData() == null && implData != null)
            //         sm.setImplData(impl, true);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getImplData",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT)));
            LabelNode lblEndIf = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIf));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));

            LabelNode lblEndIf2 = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIf2));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));
            instructions.add(AsmHelper.getLoadConstantInsn(true));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "setImplData",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.TYPE_OBJECT, Type.BOOLEAN_TYPE)));
            instructions.add(lblEndIf);
            instructions.add(lblEndIf2);
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
    }

    /**
     * Add methods for loading and storing class-level impl data.
     */
    private void addFieldImplDataMethods(ClassNodeTracker cnt, ClassMetaData meta) {
        ClassNode classNode = cnt.getClassNode();
        int count = countImplDataFields(meta);
        FieldNode impl = null;

        // public void loadImplData(OpenJPAStateManager sm, int i)
        {
            MethodNode meth = new MethodNode(Opcodes.ACC_PRIVATE,
                                             "loadImplData",
                                             Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class), Type.INT_TYPE),
                                             null, null);
            classNode.methods.add(meth);
            InsnList instructions = meth.instructions;

            if (count == 0) {
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
            else {
                // Object[] fieldImpl
                impl = new FieldNode(Opcodes.ACC_PRIVATE, "fieldImpl", Type.getDescriptor(Object[].class), null, null);
                classNode.fields.add(impl);

                // if (fieldImpl != null)
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));

                LabelNode lblEndIf = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIf));
                instructions.add(new InsnNode(Opcodes.RETURN));
                instructions.add(lblEndIf);

                // Object obj = null;
                int objVarPos = AsmHelper.getLocalVarPos(meth);
                instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, objVarPos));

                LabelNode lblEnd = new LabelNode();
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
                // switch(i)
                LookupSwitchInsnNode lSwitch = new LookupSwitchInsnNode(lblEnd, null, null);
                FieldMetaData[] fields = meta.getFields();
                int cacheable = 0;
                for (int i = 0; i < fields.length; i++) {
                    if (!usesImplData(fields[i])) {
                        continue;
                    }

                    // case x: obj = fieldImpl[y]; break;
                    LabelNode lblCase = new LabelNode();
                    instructions.add(lblCase);
                    lSwitch.keys.add(i);
                    lSwitch.labels.add(lblCase);
                    instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));
                    instructions.add(AsmHelper.getLoadConstantInsn(cacheable++));
                    instructions.add(new InsnNode(Opcodes.AALOAD));
                    instructions.add(new VarInsnNode(Opcodes.ASTORE, objVarPos));
                    instructions.add(new JumpInsnNode(Opcodes.GOTO, lblEnd));
                }
                // 'default:' is empty

                instructions.add(lblEnd);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, objVarPos));

                // if (obj != null) return;
                lblEndIf = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIf));
                instructions.add(new InsnNode(Opcodes.RETURN));

                // end if
                instructions.add(lblEndIf);

                // sm.setImplData(index, impl);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter OpenJPAStateManager
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 2)); // 2nd parameter int
                instructions.add(new VarInsnNode(Opcodes.ALOAD, objVarPos)); // the previously stored fieldImpl
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(OpenJPAStateManager.class),
                                                    "setImplData",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, AsmHelper.TYPE_OBJECT)));
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
        }

        // void storeImplData(OpenJPAStateManager sm, int index, boolean loaded)
        {
            MethodNode meth = new MethodNode(Opcodes.ACC_PRIVATE,
                                             "storeImplData",
                                             Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                      Type.getType(OpenJPAStateManager.class), Type.INT_TYPE, Type.BOOLEAN_TYPE),
                                             null, null);
            classNode.methods.add(meth);
            InsnList instructions = meth.instructions;

            if (count == 0) {
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
            else {
                // int arrIdx = -1;
                // switch(index)
                int arrIdxVarPos = AsmHelper.getLocalVarPos(meth);
                instructions.add(AsmHelper.getLoadConstantInsn(-1));
                instructions.add(new VarInsnNode(Opcodes.ISTORE, arrIdxVarPos));
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 2)); // 2nd param int

                LabelNode lblEnd = new LabelNode();
                // switch(i)
                LookupSwitchInsnNode lSwitch = new LookupSwitchInsnNode(lblEnd, null, null);

                FieldMetaData[] fields = meta.getFields();
                int cacheable = 0;
                for (int i = 0; i < fields.length; i++) {
                    if (!usesImplData(fields[i])) {
                        continue;
                    }

                    // case x: arrIdx = y; break;
                    LabelNode lblCase = new LabelNode();
                    instructions.add(lblCase);
                    lSwitch.keys.add(i);
                    lSwitch.labels.add(lblCase);
                    instructions.add(AsmHelper.getLoadConstantInsn(cacheable++));
                    instructions.add(new VarInsnNode(Opcodes.ISTORE, arrIdxVarPos));
                    instructions.add(new JumpInsnNode(Opcodes.GOTO, lblEnd));
                }
                // 'default:' is empty

                instructions.add(lblEnd);

                // if (arrIdx != -1)
                instructions.add(AsmHelper.getLoadConstantInsn(-1));
                LabelNode lblEndIf = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IF_ICMPNE, lblEndIf));
                instructions.add(new InsnNode(Opcodes.RETURN));

                // end if
                instructions.add(lblEndIf);

                // if (loaded)
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 3)); // 3rd param, boolean
                lblEndIf = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf));

                // Object obj = sm.getImplData(index)
                int objVarPos = arrIdxVarPos+1;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, OpenJPAStateManager
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 2)); // 2st param, int
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(OpenJPAStateManager.class),
                                                    "getImplData",
                                                    Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT, Type.INT_TYPE)));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, objVarPos));

                // if (fieldImpl == null)
                //     fieldImpl = new Object[fields];

                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));

                LabelNode lblEndIfNN = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIfNN));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(AsmHelper.getLoadConstantInsn(count));
                instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(Object.class)));
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, impl.name, impl.desc));
                instructions.add(lblEndIfNN);

                // fieldImpl[arrIdx] = obj;
                // return;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));
                instructions.add(new VarInsnNode(Opcodes.ILOAD, arrIdxVarPos));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, objVarPos));
                instructions.add(new InsnNode(Opcodes.AASTORE));
                instructions.add(new InsnNode(Opcodes.RETURN));

                instructions.add(lblEndIf);

                // if (fieldImpl != null)
                //         fieldImpl[index] = null;
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));
                lblEndIfNN = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIfNN));
                instructions.add(new InsnNode(Opcodes.RETURN));

                instructions.add(lblEndIfNN);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, impl.name, impl.desc));
                instructions.add(new VarInsnNode(Opcodes.ILOAD, arrIdxVarPos));
                instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                instructions.add(new InsnNode(Opcodes.AASTORE));
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
        }
    }

    /**
     * Add methods for loading and storing version data.
     */
    protected void addVersionMethods(ClassNodeTracker bc) {
        ClassNode classNode = bc.getClassNode();

        final FieldNode versionField = addBeanField(bc, "version", Object.class);

        // void storeVersion(OpenJPAStateManager sm);
        {
            MethodNode storeMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                  "storeVersion",
                                                  Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class)),
                                                  null, null);
            classNode.methods.add(storeMeth);
            InsnList instructions = storeMeth.instructions;


            // version = sm.getVersion();
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getVersion",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT)));
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, versionField.name, versionField.desc));
            instructions.add(new InsnNode(Opcodes.RETURN));
        }

        // void loadVersion(OpenJPAStateManager sm)
        {
            MethodNode loadMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                 "loadVersion",
                                                 Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class)),
                                                 null, null);
            classNode.methods.add(loadMeth);
            InsnList instructions = loadMeth.instructions;

            // if (sm.getVersion() == null)
            //         sm.setVersion(version);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getVersion",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT)));

            LabelNode lblEndIf = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIf));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, versionField.name, versionField.desc));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "setVersion",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.TYPE_OBJECT)));

            instructions.add(lblEndIf);
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
    }

    private void addLoadMethod(ClassNodeTracker cnt, ClassMetaData meta) {
        final ClassNode classNode = cnt.getClassNode();

        // public void load(OpenJPAStateManager sm, FetchConfiguration fetch, Object context)
        MethodNode meth = addLoadMethod(cnt, false);
        InsnList instructions = meth.instructions;

        FieldMetaData[] fmds = meta.getFields();

        int localVarPos = AsmHelper.getLocalVarPos(meth);
        instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, localVarPos));
        int interVarPos = localVarPos + 1;
        instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, interVarPos));

        int objectCount = 0;
        boolean intermediate;
        LabelNode lblEndIf = null;
        for (int i = 0; i < fmds.length; i++) {
            if (lblEndIf != null) {
                instructions.add(lblEndIf);
            }
            lblEndIf = new LabelNode();

            intermediate = usesIntermediate(fmds[i]);

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

            // if (loaded.get(i)) or (!loaded.get(i)) depending on inter resp
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "get",
                                                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
            instructions.add(new JumpInsnNode(intermediate ? Opcodes.IFNE : Opcodes.IFEQ, lblEndIf));

            // if (fetch.requiresFetch(fmds[i])!=FetchConfiguration.FETCH_NONE)

            if (intermediate) {
                addLoadIntermediate(classNode, instructions, i, objectCount, interVarPos);

                LabelNode lblElse = lblEndIf;
                lblEndIf = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.GOTO, lblEndIf));
                instructions.add(lblElse);
            }

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd param FetchConfiguration
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param OpenJPAStateManager
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getMetaData",
                                                Type.getMethodDescriptor(Type.getType(ClassMetaData.class))));
            instructions.add(AsmHelper.getLoadConstantInsn(fmds[i].getIndex()));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ClassMetaData.class),
                                                "getField",
                                                Type.getMethodDescriptor(Type.getType(FieldMetaData.class), Type.INT_TYPE)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(FetchConfiguration.class),
                                                "requiresFetch",
                                                Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(FieldMetaData.class))));
            instructions.add(AsmHelper.getLoadConstantInsn(FetchConfiguration.FETCH_NONE));
            instructions.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, lblEndIf));
            addLoad(classNode, instructions, fmds[i], objectCount, localVarPos, false);


            if (replaceType(fmds[i]) >= JavaTypes.OBJECT) {
                objectCount++;
            }
        }

        if (lblEndIf != null) {
            instructions.add(lblEndIf);
        }

        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    private void addLoadWithFieldsMethod(ClassNodeTracker cnt, ClassMetaData meta) {
        final ClassNode classNode = cnt.getClassNode();

        // public void load(OpenJPAStateManager sm, BitSet fields, FetchConfiguration fetch, Object conn)
        MethodNode meth = addLoadMethod(cnt, true);
        InsnList instructions = meth.instructions;

        FieldMetaData[] fmds = meta.getFields();

        int localVarPos = AsmHelper.getLocalVarPos(meth);
        instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, localVarPos));
        int interVarPos = localVarPos + 1;
        instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, interVarPos));

        int objectCount = 0;
        boolean intermediate;
        LabelNode lblEndIf = null;
        for (int i = 0; i < fmds.length; i++) {
            if (lblEndIf != null) {
                instructions.add(lblEndIf);
            }
            lblEndIf = new LabelNode();

            intermediate = usesIntermediate(fmds[i]);

            // if (fields.get(i))
            // {
            //         if (loaded.get(i))
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd param, BitSet
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "get",
                                                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));

            instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "get",
                                                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));

            LabelNode lblEndIf2 = null;
            if (intermediate) {
                // nested if/else. Thus we need another jump target
                lblEndIf2 = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf2));
            }
            else {
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf));
            }

            addLoad(classNode, instructions, fmds[i], objectCount, localVarPos, true);

            if (usesImplData(fmds[i])) {
                // loadImplData(sm, i);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param, OpenJPAStateManager
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    classNode.name,
                                                    "loadImplData",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                             Type.getType(OpenJPAStateManager.class), Type.INT_TYPE)));
            }

            // fields.clear(i);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd param, BitSet
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "clear",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));

            if (intermediate) {
                instructions.add(new JumpInsnNode(Opcodes.GOTO, lblEndIf));

                // else part
                instructions.add(lblEndIf2);
                addLoadIntermediate(classNode, instructions, i, objectCount, interVarPos);
            }

            if (replaceType(fmds[i]) >= JavaTypes.OBJECT) {
                objectCount++;
            }
        }

        if (lblEndIf != null) {
            instructions.add(lblEndIf);
        }

        instructions.add(new InsnNode(Opcodes.RETURN));

    }

    /**
     * Declare and start the base load method.
     */
    private MethodNode addLoadMethod(ClassNodeTracker cnt, boolean fields) {
        String mDesc;
        if (fields) {
            mDesc = Type.getMethodDescriptor(Type.VOID_TYPE,
                                             Type.getType(OpenJPAStateManager.class),
                                             Type.getType(BitSet.class),
                                             Type.getType(FetchConfiguration.class),
                                             AsmHelper.TYPE_OBJECT);
        }
        else {
            mDesc = Type.getMethodDescriptor(Type.VOID_TYPE,
                                             Type.getType(OpenJPAStateManager.class),
                                             Type.getType(FetchConfiguration.class),
                                             AsmHelper.TYPE_OBJECT);
        }
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                         "load",
                                         mDesc,
                                         null, null);
        cnt.getClassNode().methods.add(meth);

        // loadVersion(sm);
        meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // OpenJPAStateManager
        meth.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                 cnt.getClassNode().name,
                                                 "loadVersion",
                                                 Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class))));

        //loadImplData(sm);
        meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // OpenJPAStateManager
        meth.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                 cnt.getClassNode().name,
                                                 "loadImplData",
                                                 Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class))));
        return meth;
    }

    /**
     * Add the field load.
     */
    private void addLoad(ClassNode classNode, InsnList instructions, FieldMetaData fmd,
                         int objectCount, int localVarPos, boolean fields) {
        int index = fmd.getIndex();
        int typeCode = replaceType(fmd);
        if (typeCode < JavaTypes.OBJECT) {
            // sm.store<type>(i, field<i>)
            Class<?> type = forType(fmd.getTypeCode());
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, getFieldName(index), Type.getDescriptor(type)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "store" + StringUtil.capitalize(type.getName()),
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(type))));
        }
        else {
            // fmd = sm.getMetaData().getField(i);
            int offset = fields ? 1 : 0;
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getMetaData",
                                                Type.getMethodDescriptor(Type.getType(ClassMetaData.class))));
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ClassMetaData.class),
                                                "getField",
                                                Type.getMethodDescriptor(Type.getType(FieldMetaData.class), Type.INT_TYPE)));
            instructions.add(new VarInsnNode(Opcodes.ASTORE, localVarPos));

            // sm.storeField(i, toField(sm, fmd, objects[objectCount],
            //         fetch, context);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new VarInsnNode(Opcodes.ALOAD, localVarPos));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
            instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
            instructions.add(new InsnNode(Opcodes.AALOAD));

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2+offset));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 3+offset));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                "toField",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT,
                                                                         Type.getType(OpenJPAStateManager.class),
                                                                         Type.getType(FieldMetaData.class),
                                                                         AsmHelper.TYPE_OBJECT,
                                                                         Type.getType(FetchConfiguration.class),
                                                                         AsmHelper.TYPE_OBJECT)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "storeField",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, AsmHelper.TYPE_OBJECT)));
        }
    }

    /**
     * Load intermediate data if possible.
     */
    private void addLoadIntermediate(ClassNode classNode, InsnList instructions, int index, int objectCount, int interVarPos) {
        // {
        //    Object inter = objects[objectCount];
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
        instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
        instructions.add(new InsnNode(Opcodes.AALOAD));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, interVarPos));

        //    if (inter != null && !sm.getLoaded().get(index))
        LabelNode lblEndIf = new LabelNode();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, interVarPos));
        instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIf));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(OpenJPAStateManager.class),
                                            "getLoaded",
                                            Type.getMethodDescriptor(Type.getType(BitSet.class))));
        instructions.add(AsmHelper.getLoadConstantInsn(index));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            Type.getInternalName(BitSet.class),
                                            "get",
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
        instructions.add(new JumpInsnNode(Opcodes.IFNE, lblEndIf));

        //            sm.setIntermediate(index, inter);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st Param
        instructions.add(AsmHelper.getLoadConstantInsn(index));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, interVarPos));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(OpenJPAStateManager.class),
                                            "setIntermediate",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, AsmHelper.TYPE_OBJECT)));

        //    }  // end if - done with lblEndIf
        instructions.add(lblEndIf);
    }

    private void addStoreMethods(ClassNodeTracker cnt, ClassMetaData meta) {
        // i.e. void store(OpenJPAStateManager sm, BitSet fields);
        addStoreMethod(cnt, meta, true);
        // i.e. void store(OpenJPAStateManager sm);
        addStoreMethod(cnt, meta, false);
    }

    private void addStoreMethod(ClassNodeTracker cnt, ClassMetaData meta, boolean fields) {

        MethodNode store;
        if (fields) {
            store = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED,
                                   "store",
                                   Type.getMethodDescriptor(Type.VOID_TYPE,
                                                            Type.getType(OpenJPAStateManager.class), Type.getType(BitSet.class)),
                                   null, null);
        }
        else {
            store = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED,
                                   "store",
                                   Type.getMethodDescriptor(Type.VOID_TYPE,
                                                            Type.getType(OpenJPAStateManager.class)),
                                   null, null);
        }
        ClassNode classNode = cnt.getClassNode();
        classNode.methods.add(store);
        InsnList instructions = store.instructions;

        // initialize();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            "initialize",
                                            Type.getMethodDescriptor(Type.VOID_TYPE)));

        // storeVersion(sm);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            "storeVersion",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class))));

        // storeImplData(sm);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            "storeImplData",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class))));

        FieldMetaData[] fmds = meta.getFields();
        int objectCount = 0;
        for (int i = 0; i < fmds.length; i++) {
            LabelNode lblEndIf = new LabelNode();

            if (fields) {
                //  if (fields != null && fields.get(index))
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd parameter, BitSet
                instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIf));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd parameter, BitSet
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    Type.getInternalName(BitSet.class),
                                                    "get",
                                                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf));
            } else {
                // if (sm.getLoaded().get(index)))
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter, OpenJPAStateManager
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(OpenJPAStateManager.class),
                                                    "getLoaded",
                                                    Type.getMethodDescriptor(Type.getType(BitSet.class))));
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    Type.getInternalName(BitSet.class),
                                                    "get",
                                                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf));
            }
            addStore(classNode, store, instructions, fmds[i], objectCount);

            if (usesIntermediate(fmds[i])) {
                // } else { ..
                LabelNode lblEndElse = new LabelNode(); // new jump target for end else
                instructions.add(new JumpInsnNode(Opcodes.GOTO, lblEndElse));
                instructions.add(lblEndIf); // actually this is now the begin of the else part
                lblEndIf = new LabelNode(); //X TODO not sure!

                // if (!loaded.get(index))
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    Type.getInternalName(BitSet.class),
                                                    "get",
                                                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
                instructions.add(new JumpInsnNode(Opcodes.IFNE, lblEndElse));
                // Object val = sm.getIntermediate(index);
                // if (val != null)
                //         objects[objectCount] = val;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(OpenJPAStateManager.class),
                                                    "getIntermediate",
                                                    Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT, Type.INT_TYPE)));
                int localVarPos = AsmHelper.getLocalVarPos(store);
                instructions.add(new VarInsnNode(Opcodes.ASTORE, localVarPos));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, localVarPos));
                instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndElse));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
                instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, localVarPos));
                instructions.add(new InsnNode(Opcodes.AASTORE));

                instructions.add(lblEndElse);
            }

            if (lblEndIf != null) {
                instructions.add(lblEndIf);
            }

            if (replaceType(fmds[i]) >= JavaTypes.OBJECT)
                objectCount++;
        }

        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    private void addStore(ClassNode classNode, MethodNode meth, InsnList instructions, FieldMetaData fmd, int objectCount) {
        int typeCode = replaceType(fmd);
        int index = fmd.getIndex();
        if (typeCode < JavaTypes.OBJECT) {
            Class<?> type = forType(typeCode);

            // field<i> = sm.fetch<Type>(index)
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "fetch" + StringUtil.capitalize(type.getName()),
                                                Type.getMethodDescriptor(Type.getType(type), Type.INT_TYPE)));
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, getFieldName(index), Type.getDescriptor(type)));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "set",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
        }
        else {
            // Object val = toData(sm.getMetaData().getField(index),
            //         sm.fetchField(index, false), sm.getContext());
            int localVarPos = AsmHelper.getLocalVarPos(meth);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getMetaData",
                                                Type.getMethodDescriptor(Type.getType(ClassMetaData.class))));
            instructions.add(AsmHelper.getLoadConstantInsn(fmd.getIndex()));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ClassMetaData.class),
                                                "getField",
                                                Type.getMethodDescriptor(Type.getType(FieldMetaData.class), Type.INT_TYPE)));

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(AsmHelper.getLoadConstantInsn(fmd.getIndex()));
            instructions.add(AsmHelper.getLoadConstantInsn(false));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "fetchField",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT, Type.INT_TYPE, Type.BOOLEAN_TYPE)));

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "getContext",
                                                Type.getMethodDescriptor(Type.getType(StoreContext.class))));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                "toData",
                                                Type.getMethodDescriptor(AsmHelper.TYPE_OBJECT,
                                                                         Type.getType(FieldMetaData.class),
                                                                         AsmHelper.TYPE_OBJECT,
                                                                         Type.getType(StoreContext.class))));
            instructions.add(new VarInsnNode(Opcodes.ASTORE, localVarPos));

            // if (val == NULL) {
            //         val = null;
            //         loaded.clear(index);
            //     } else
            //         loaded.set(index);
            //     objects[objectCount] = val;
            instructions.add(new VarInsnNode(Opcodes.ALOAD, localVarPos));
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(AbstractPCData.class),
                                               "NULL", AsmHelper.TYPE_OBJECT.getDescriptor()));
            LabelNode lblElse = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IF_ACMPNE, lblElse));
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            instructions.add(new VarInsnNode(Opcodes.ASTORE, localVarPos));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "clear",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
            LabelNode lblEndIf = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.GOTO, lblEndIf));

            instructions.add(lblElse);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
            instructions.add(AsmHelper.getLoadConstantInsn(index));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(BitSet.class),
                                                "set",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));

            instructions.add(lblEndIf);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "objects", Type.getDescriptor(Object[].class)));
            instructions.add(AsmHelper.getLoadConstantInsn(objectCount));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, localVarPos));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        if (!usesImplData(fmd)) {
            return;
        }

        // storeImplData(sm, i, loaded.get(i);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(AsmHelper.getLoadConstantInsn(index));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "loaded", Type.getDescriptor(BitSet.class)));
        instructions.add(AsmHelper.getLoadConstantInsn(index));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            Type.getInternalName(BitSet.class),
                                            "get",
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE)));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            "storeImplData",
                                            Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                     Type.getType(OpenJPAStateManager.class), Type.INT_TYPE, Type.BOOLEAN_TYPE)));
    }

    private void addNewEmbedded(BCClass bc) {
        // void newEmbeddedPCData(OpenJPAStateManager embedded)
        BCMethod meth = bc.declareMethod("newEmbeddedPCData", PCData.class,
            new Class[]{ OpenJPAStateManager.class });
        Code code = meth.getCode(true);
        // return getStorageGenerator().generatePCData
        //         (sm.getId(), sm.getMetaData());
        code.aload().setThis();
        code.getfield().setField("storageGenerator", PCDataGenerator.class);
        code.aload().setParam(0);
        code.invokeinterface().setMethod(OpenJPAStateManager.class,
            "getId", Object.class, null);
        code.aload().setParam(0);
        code.invokeinterface().setMethod(OpenJPAStateManager.class,
            "getMetaData", ClassMetaData.class, null);
        code.invokevirtual().setMethod(PCDataGenerator.class,
            "generatePCData", PCData.class, new Class[]
            { Object.class, ClassMetaData.class });
        code.areturn();
        code.calculateMaxLocals();
        code.calculateMaxStack();
    }

    private void addGetData(BCClass bc) {
        // return getObjectField(i);
        BCMethod method = bc.declareMethod("getData", Object.class,
            new Class[]{ int.class });
        Code code = method.getCode(true);
        code.aload().setThis();
        code.iload().setParam(0);
        code.invokevirtual().setMethod("getObject", Object.class,
            new Class[]{ int.class });
        code.areturn();
        code.calculateMaxLocals();
        code.calculateMaxStack();
    }

    /////////////
    // Utilities
    /////////////

    /**
     * Return a valid {@link JavaTypes} constant for the given field
     */
    protected int replaceType(FieldMetaData fmd) {
        if (usesIntermediate(fmd))
            return JavaTypes.OBJECT;
        return fmd.getTypeCode();
    }

    /**
     * Whether the given field uses a cacheable intermediate value.
     */
    protected boolean usesIntermediate(FieldMetaData fmd) {
        return fmd.usesIntermediate();
    }

    /**
     * Whether the given type might have cacheable class-level impl data.
     */
    protected boolean usesImplData(ClassMetaData meta) {
        return true;
    }

    /**
     * Whether the given field might have cacheable impl data.
     */
    protected boolean usesImplData(FieldMetaData fmd) {
        return fmd.usesImplData() == null;
    }

    /**
     * The number of fields with cacheable impl data.
     */
    private int countImplDataFields(ClassMetaData meta) {
        FieldMetaData[] fmds = meta.getFields();
        int count = 0;
        for (FieldMetaData fmd : fmds)
            if (usesImplData(fmd))
                count++;
        return count;
    }

    /**
     * Set the collection of {@link JumpInstruction}s to the given instruction,
     * clearing the collection in the process.
     */
    protected void setTarget(Instruction ins, Collection<Instruction> jumps) {
        for (Instruction jump : jumps) {
            ((JumpInstruction) jump).setTarget(ins);
        }
        jumps.clear();
    }

    /**
     * Transform the given array of classes to strings.
     */
    private static String[] toStrings(Class<?>[] cls) {
        String[] strings = new String[cls.length];
        for (int i = 0; i < strings.length; i++)
            strings[i] = cls[i].getName();
        return strings;
    }


    /**
     * Dynamic {@link PCData}s generated will implement this interface
     * to simplify initialization.
     */
    public interface DynamicPCData extends PCData {

        void setId(Object oid);

        void setStorageGenerator (PCDataGenerator generator);
    }
}

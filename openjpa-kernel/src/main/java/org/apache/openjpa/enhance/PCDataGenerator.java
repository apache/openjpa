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
import java.util.LinkedList;
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
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.TypeInsnNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

import serp.bytecode.BCClass;
import serp.bytecode.BCField;
import serp.bytecode.BCMethod;
import serp.bytecode.Code;
import serp.bytecode.Constants;
import serp.bytecode.Instruction;
import serp.bytecode.JumpInstruction;
import serp.bytecode.LookupSwitchInstruction;
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

        BCClass _bc = new Project().loadClass(bc.getClassNode().name.replace("/", "."));
        AsmHelper.readIntoBCClass(bc, _bc);

        addFieldImplDataMethods(_bc, meta);
        addLoadMethod(_bc, meta);
        addLoadWithFieldsMethod(_bc, meta);
        addStoreMethods(_bc, meta);
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
                                                Type.getMethodDescriptor(Type.getType(Object.class))));

            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                "setImplData",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class))));

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
                                                Type.getMethodDescriptor(Type.getType(Object.class))));
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
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class), Type.BOOLEAN_TYPE)));
            instructions.add(lblEndIf);
            instructions.add(lblEndIf2);
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
    }

    /**
     * Add methods for loading and storing class-level impl data.
     */
    private void addFieldImplDataMethods(BCClass bc, ClassMetaData meta) {
        // public void loadImplData(OpenJPAStateManager sm, int i)
        BCMethod meth = bc.declareMethod("loadImplData", void.class,
            new Class[]{ OpenJPAStateManager.class, int.class });
        meth.makePrivate();
        Code code = meth.getCode(true);

        int count = countImplDataFields(meta);
        BCField impl = null;
        if (count == 0)
            code.vreturn();
        else {
            // Object[] fieldImpl
            impl = bc.declareField("fieldImpl", Object[].class);
            impl.makePrivate();

            // if (fieldImpl != null)
            code.aload().setThis();
            code.getfield().setField(impl);
            JumpInstruction ifins = code.ifnonnull();
            code.vreturn();

            // Object obj = null;
            int obj = code.getNextLocalsIndex();
            ifins.setTarget(code.constant().setNull());
            code.astore().setLocal(obj);

            // establish switch target, then move before it
            Instruction target = code.aload().setLocal(obj);
            code.previous();

            // switch(i)
            code.iload().setParam(1);
            LookupSwitchInstruction lswitch = code.lookupswitch();
            FieldMetaData[] fields = meta.getFields();
            int cacheable = 0;
            for (int i = 0; i < fields.length; i++) {
                if (!usesImplData(fields[i]))
                    continue;
                // case x: obj = fieldImpl[y]; break;
                lswitch.addCase(i, code.aload().setThis());
                code.getfield().setField(impl);
                code.constant().setValue(cacheable++);
                code.aaload();
                code.astore().setLocal(obj);
                code.go2().setTarget(target);
            }
            lswitch.setDefaultTarget(target);

            // if (obj != null)
            code.next();    // jump back over target
            ifins = code.ifnonnull();
            code.vreturn();

            // sm.setImplData(index, impl);
            ifins.setTarget(code.aload().setParam(0));
            code.iload().setParam(1);
            code.aload().setLocal(obj);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "setImplData", void.class,
                new Class[]{ int.class, Object.class });
            code.vreturn();
        }
        code.calculateMaxLocals();
        code.calculateMaxStack();

        // void storeImplData(OpenJPAStateManager sm, int index, boolean loaded)
        meth = bc.declareMethod("storeImplData", void.class,
            new Class[]{ OpenJPAStateManager.class, int.class, boolean.class });
        code = meth.getCode(true);
        if (count == 0)
            code.vreturn();
        else {
            // int arrIdx = -1;
            // switch(index)
            int arrIdx = code.getNextLocalsIndex();
            code.constant().setValue(-1);
            code.istore().setLocal(arrIdx);
            code.iload().setParam(1);
            LookupSwitchInstruction lswitch = code.lookupswitch();

            // establish switch target, then move before it
            Instruction switchTarget = code.iload().setLocal(arrIdx);
            code.previous();

            FieldMetaData[] fields = meta.getFields();
            int cacheable = 0;
            for (int i = 0; i < fields.length; i++) {
                if (!usesImplData(fields[i]))
                    continue;
                // case x: arrIdx = y; break;
                lswitch.addCase(i, code.constant().setValue(cacheable++));
                code.istore().setLocal(arrIdx);
                code.go2().setTarget(switchTarget);
            }
            lswitch.setDefaultTarget(switchTarget);
            code.next();    // step over switch target

            // if (arrIdx != -1)
            code.constant().setValue(-1);
            JumpInstruction ifins = code.ificmpne();
            code.vreturn();

            // create null target, then move before it
            Instruction nullTarget = code.aload().setThis();
            code.previous();

            // if (loaded)
            ifins.setTarget(code.iload().setParam(2));
            code.ifeq().setTarget(nullTarget);

            // Object obj = sm.getImplData(index)
            int obj = code.getNextLocalsIndex();
            code.aload().setParam(0);
            code.iload().setParam(1);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "getImplData", Object.class, new Class[]{ int.class });
            code.astore().setLocal(obj);

            // if (obj != null)
            code.aload().setLocal(obj);
            code.ifnull().setTarget(nullTarget);

            // if (fieldImpl == null)
            //         fieldImpl = new Object[fields];
            code.aload().setThis();
            code.getfield().setField(impl);
            ifins = code.ifnonnull();
            code.aload().setThis();
            code.constant().setValue(count);
            code.anewarray().setType(Object.class);
            code.putfield().setField(impl);

            // fieldImpl[arrIdx] = obj;
            // return;
            ifins.setTarget(code.aload().setThis());
            code.getfield().setField(impl);
            code.iload().setLocal(arrIdx);
            code.aload().setLocal(obj);
            code.aastore();
            code.vreturn();

            // if (fieldImpl != null)
            //         fieldImpl[index] = null;
            code.next(); // step over nullTarget
            code.getfield().setField(impl);
            ifins = code.ifnonnull();
            code.vreturn();
            ifins.setTarget(code.aload().setThis());
            code.getfield().setField(impl);
            code.iload().setLocal(arrIdx);
            code.constant().setNull();
            code.aastore();
            code.vreturn();
        }
        code.calculateMaxStack();
        code.calculateMaxLocals();
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
                                                Type.getMethodDescriptor(Type.getType(Object.class))));
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
                                                Type.getMethodDescriptor(Type.getType(Object.class))));

            LabelNode lblEndIf = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIf));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, versionField.name, versionField.desc));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(OpenJPAStateManager.class),
                                                "setVersion",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class))));

            instructions.add(lblEndIf);
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
    }

    private void addLoadMethod(BCClass bc, ClassMetaData meta) {
        // public void load(OpenJPAStateManager sm, FetchConfiguration fetch,
        //         Object context)
        Code code = addLoadMethod(bc, false);
        FieldMetaData[] fmds = meta.getFields();
        Collection<Instruction> jumps = new LinkedList<>();
        Collection<Instruction> jumps2;

        int local = code.getNextLocalsIndex();
        code.constant().setNull();
        code.astore().setLocal(local);
        int inter = code.getNextLocalsIndex();
        code.constant().setNull();
        code.astore().setLocal(inter);

        int objectCount = 0;
        boolean intermediate;
        for (int i = 0; i < fmds.length; i++) {
            jumps2 = new LinkedList<>();
            intermediate = usesIntermediate(fmds[i]);
            setTarget(code.aload().setThis(), jumps);
            // if (loaded.get(i)) or (!loaded.get(i)) depending on inter resp
            code.getfield().setField("loaded", BitSet.class);
            code.constant().setValue(i);
            code.invokevirtual().setMethod(BitSet.class, "get",
                boolean.class, new Class[]{ int.class });
            jumps.add(code.ifne());

            if (intermediate)
                addLoadIntermediate(code, i, objectCount, jumps2, inter);
            jumps2.add(code.go2());

            // if (fetch.requiresFetch(fmds[i])!=FetchConfiguration.FETCH_NONE)
            setTarget(code.aload().setParam(1), jumps);
            code.aload().setParam(0);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "getMetaData", ClassMetaData.class, null);
            code.constant().setValue(fmds[i].getIndex());
            code.invokevirtual().setMethod(ClassMetaData.class,
                "getField", FieldMetaData.class, new Class[]{int.class});
            code.invokeinterface().setMethod (FetchConfiguration.class,
                "requiresFetch", int.class, new Class[]{FieldMetaData.class});
            code.constant().setValue(FetchConfiguration.FETCH_NONE);
            jumps2.add(code.ificmpeq());
            addLoad(bc, code, fmds[i], objectCount, local, false);

            jumps = jumps2;
            if (replaceType(fmds[i]) >= JavaTypes.OBJECT)
                objectCount++;
        }
        setTarget(code.vreturn(), jumps);
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    private void addLoadWithFieldsMethod(BCClass bc, ClassMetaData meta) {
        Code code = addLoadMethod(bc, true);
        // public void load(OpenJPAStateManager sm, BitSet fields,
        //         FetchConfiguration fetch, Object conn)
        FieldMetaData[] fmds = meta.getFields();
        Collection<Instruction> jumps = new LinkedList<>();
        Collection<Instruction> jumps2;
        int objectCount = 0;
        boolean intermediate;
        int local = code.getNextLocalsIndex();
        code.constant().setNull();
        code.astore().setLocal(local);
        int inter = code.getNextLocalsIndex();
        code.constant().setNull();
        code.astore().setLocal(inter);

        for (int i = 0; i < fmds.length; i++) {
            jumps2 = new LinkedList<>();
            intermediate = usesIntermediate(fmds[i]);
            // if (fields.get(i))
            // {
            //         if (loaded.get(i))
            setTarget(code.aload().setParam(1), jumps);
            code.constant().setValue(i);
            code.invokevirtual().setMethod(BitSet.class, "get",
                boolean.class, new Class[]{ int.class });
            jumps2.add(code.ifeq());
            code.aload().setThis();
            code.getfield().setField("loaded", BitSet.class);
            code.constant().setValue(i);
            code.invokevirtual().setMethod(BitSet.class, "get",
                boolean.class, new Class[]{ int.class });
            if (intermediate)
                jumps.add(code.ifeq());
            else
                jumps2.add(code.ifeq());

            addLoad(bc, code, fmds[i], objectCount, local, true);
            if (usesImplData(fmds[i])) {
                // loadImplData(sm, i);
                code.aload().setThis();
                code.aload().setParam(0);
                code.constant().setValue(i);
                code.invokevirtual().setMethod("loadImplData", void.class,
                    new Class[]{ OpenJPAStateManager.class, int.class });
            }

            // fields.clear(i);
            code.aload().setParam(1);
            code.constant().setValue(i);
            code.invokevirtual().setMethod(BitSet.class, "clear", void.class,
                new Class[] { int.class });

            jumps2.add(code.go2());

            if (intermediate)
                setTarget(addLoadIntermediate
                    (code, i, objectCount, jumps2, inter), jumps);

            jumps = jumps2;
            if (replaceType(fmds[i]) >= JavaTypes.OBJECT)
                objectCount++;
        }
        setTarget(code.vreturn(), jumps);
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Declare and start the base load method.
     */
    private Code addLoadMethod(BCClass bc, boolean fields) {
        Class<?>[] args = null;
        if (fields)
            args = new Class[]{ OpenJPAStateManager.class, BitSet.class,
                FetchConfiguration.class, Object.class };
        else
            args = new Class[]{ OpenJPAStateManager.class,
                FetchConfiguration.class, Object.class };
        BCMethod load = bc.declareMethod("load", void.class, args);
        Code code = load.getCode(true);

        //loadVersion(sm);
        code.aload().setThis();
        code.aload().setParam(0);
        code.invokevirtual().setMethod("loadVersion", void.class,
            new Class[]{ OpenJPAStateManager.class });

        //loadImplData(sm);
        code.aload().setThis();
        code.aload().setParam(0);
        code.invokevirtual().setMethod("loadImplData", void.class,
            new Class[]{ OpenJPAStateManager.class });
        return code;
    }

    /**
     * Add the field load.
     */
    private Instruction addLoad(BCClass bc, Code code, FieldMetaData fmd,
        int objectCount, int local, boolean fields) {
        int index = fmd.getIndex();
        int typeCode = replaceType(fmd);
        Instruction first;
        if (typeCode < JavaTypes.OBJECT) {
            // sm.store<type>(i, field<i>)
            Class<?> type = forType(fmd.getTypeCode());
            first = code.aload().setParam(0);
            code.constant().setValue(index);
            code.aload().setThis();
            code.getfield().setField(getFieldName(index), type);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "store" + StringUtil.capitalize(type.getName()),
                void.class, new Class[]{ int.class, type });
        } else {
            // fmd = sm.getMetaData().getField(i);
            int offset = fields ? 1 : 0;
            first = code.aload().setParam(0);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "getMetaData", ClassMetaData.class, null);
            code.constant().setValue(fmd.getIndex());
            code.invokevirtual().setMethod(ClassMetaData.class, "getField",
                FieldMetaData.class, new Class[]{ int.class });
            code.astore().setLocal(local);
            // sm.storeField(i, toField(sm, fmd, objects[objectCount],
            //         fetch, context);
            code.aload().setParam(0);
            code.constant().setValue(index);
            code.aload().setThis();
            code.aload().setParam(0);
            code.aload().setLocal(local);
            code.aload().setThis();
            code.getfield().setField("objects", Object[].class);
            code.constant().setValue(objectCount);
            code.aaload();
            code.aload().setParam(1 + offset);
            code.aload().setParam(2 + offset);
            code.invokevirtual().setMethod(bc.getName(), "toField",
                Object.class.getName(), toStrings(new Class[]{
                OpenJPAStateManager.class, FieldMetaData.class,
                Object.class, FetchConfiguration.class, Object.class }));
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "storeField", void.class,
                new Class[]{ int.class, Object.class });
        }
        return first;
    }

    /**
     * Load intermediate data if possible.
     */
    private Instruction addLoadIntermediate(Code code, int index,
        int objectCount, Collection<Instruction> jumps2, int inter) {
        // {
        //         Object inter = objects[objectCount];
        Instruction first = code.aload().setThis();
        code.getfield().setField("objects", Object[].class);
        code.constant().setValue(objectCount);
        code.aaload();
        code.astore().setLocal(inter);
        //         if (inter != null && !sm.getLoaded().get(index))
        code.aload().setLocal(inter);
        jumps2.add(code.ifnull());
        code.aload().setParam(0);
        code.invokeinterface().setMethod(OpenJPAStateManager.class,
            "getLoaded", BitSet.class, null);
        code.constant().setValue(index);
        code.invokevirtual().setMethod(BitSet.class, "get",
            boolean.class, new Class[]{ int.class });
        jumps2.add(code.ifne());
        //            sm.setIntermediate(index, inter);
        //    }  // end else
        code.aload().setParam(0);
        code.constant().setValue(index);
        code.aload().setLocal(inter);
        code.invokeinterface().setMethod(OpenJPAStateManager.class,
            "setIntermediate", void.class,
            new Class[]{ int.class, Object.class });
        return first;
    }

    private void addStoreMethods(BCClass bc, ClassMetaData meta) {
        // i.e. void store(OpenJPAStateManager sm, BitSet fields);
        addStoreMethod(bc, meta, true);
        // i.e. void store(OpenJPAStateManager sm);
        addStoreMethod(bc, meta, false);
    }

    private void addStoreMethod(BCClass bc, ClassMetaData meta, boolean fields) {
        BCMethod store;
        if (fields)
            store = bc.declareMethod("store", void.class,
                new Class[]{ OpenJPAStateManager.class, BitSet.class });
        else
            store = bc.declareMethod("store", void.class,
                new Class[]{ OpenJPAStateManager.class });
        Code code = store.getCode(true);

        // initialize();
        code.aload().setThis();
        code.invokevirtual().setMethod("initialize", void.class, null);

        // storeVersion(sm);
        code.aload().setThis();
        code.aload().setParam(0);
        code.invokevirtual().setMethod("storeVersion", void.class,
            new Class[]{ OpenJPAStateManager.class });

        // storeImplData(sm);
        code.aload().setThis();
        code.aload().setParam(0);
        code.invokevirtual().setMethod("storeImplData", void.class,
            new Class[]{ OpenJPAStateManager.class });

        FieldMetaData[] fmds = meta.getFields();
        Collection<Instruction> jumps = new LinkedList<>();
        int objectCount = 0;
        for (int i = 0; i < fmds.length; i++) {
            if (fields) {
                //  if (fields != null && fields.get(index))
                setTarget(code.aload().setParam(1), jumps);
                jumps.add(code.ifnull());
                code.aload().setParam(1);
                code.constant().setValue(i);
                code.invokevirtual().setMethod(BitSet.class, "get",
                    boolean.class, new Class[]{ int.class });
                jumps.add(code.ifeq());
            } else {
                // if (sm.getLoaded().get(index)))
                setTarget(code.aload().setParam(0), jumps);
                code.invokeinterface().setMethod(OpenJPAStateManager.class,
                    "getLoaded", BitSet.class, null);
                code.constant().setValue(i);
                code.invokevirtual().setMethod(BitSet.class, "get",
                    boolean.class, new Class[]{ int.class });
                jumps.add(code.ifeq());
            }
            addStore(bc, code, fmds[i], objectCount);
            if (usesIntermediate(fmds[i])) {
                JumpInstruction elseIns = code.go2();
                // else if (!loaded.get(index))
                setTarget(code.aload().setThis(), jumps);
                jumps.add(elseIns);
                code.getfield().setField("loaded", BitSet.class);
                code.constant().setValue(i);
                code.invokevirtual().setMethod(BitSet.class, "get",
                    boolean.class, new Class[]{ int.class });
                jumps.add(code.ifne());
                // Object val = sm.getIntermediate(index);
                // if (val != null)
                //         objects[objectCount] = val;
                code.aload().setParam(0);
                code.constant().setValue(i);
                code.invokeinterface().setMethod(OpenJPAStateManager.class,
                    "getIntermediate", Object.class, new Class[]{ int.class });
                int local = code.getNextLocalsIndex();
                code.astore().setLocal(local);
                code.aload().setLocal(local);
                jumps.add(code.ifnull());
                code.aload().setThis();
                code.getfield().setField("objects", Object[].class);
                code.constant().setValue(objectCount);
                code.aload().setLocal(local);
                code.aastore();
            }
            if (replaceType(fmds[i]) >= JavaTypes.OBJECT)
                objectCount++;
        }
        setTarget(code.vreturn(), jumps);
        code.calculateMaxLocals();
        code.calculateMaxStack();
    }

    private void addStore(BCClass bc, Code code, FieldMetaData fmd,
        int objectCount) {
        int typeCode = replaceType(fmd);
        int index = fmd.getIndex();
        if (typeCode < JavaTypes.OBJECT) {
            Class<?> type = forType(typeCode);
            // field<i> = sm.fetch<Type>(index)
            code.aload().setThis();
            code.aload().setParam(0);
            code.constant().setValue(index);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "fetch" + StringUtil.capitalize(type.getName()), type,
                new Class[]{ int.class });
            code.putfield().setField(getFieldName(index), type);
            code.aload().setThis();
            code.getfield().setField("loaded", BitSet.class);
            code.constant().setValue(index);
            code.invokevirtual().setMethod(BitSet.class, "set", void.class,
                new Class[]{ int.class });
        } else {
            // Object val = toData(sm.getMetaData().getField(index),
            //         sm.fetchField(index, false), sm.getContext());
            int local = code.getNextLocalsIndex();
            code.aload().setThis();
            code.aload().setParam(0);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "getMetaData", ClassMetaData.class, null);
            code.constant().setValue(fmd.getIndex());
            code.invokevirtual().setMethod(ClassMetaData.class,
                "getField", FieldMetaData.class, new Class[]{ int.class });
            code.aload().setParam(0);
            code.constant().setValue(fmd.getIndex());
            code.constant().setValue(false);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "fetchField", Object.class, new Class[]
                { int.class, boolean.class });
            code.aload().setParam(0);
            code.invokeinterface().setMethod(OpenJPAStateManager.class,
                "getContext", StoreContext.class, null);
            code.invokevirtual().setMethod(bc.getName(), "toData",
                Object.class.getName(), toStrings(new Class []{
                FieldMetaData.class, Object.class, StoreContext.class }));
            code.astore().setLocal(local);

            // if (val == NULL) {
            //         val = null;
            //         loaded.clear(index);
            //     } else
            //         loaded.set(index);
            //     objects[objectCount] = val;
            code.aload().setLocal(local);
            code.getstatic().setField(AbstractPCData.class, "NULL",
                Object.class);
            JumpInstruction ifins = code.ifacmpne();
            code.constant().setNull();
            code.astore().setLocal(local);
            code.aload().setThis();
            code.getfield().setField("loaded", BitSet.class);
            code.constant().setValue(index);
            code.invokevirtual().setMethod(BitSet.class, "clear", void.class,
                new Class[]{ int.class });
            JumpInstruction go2 = code.go2();
            ifins.setTarget(code.aload().setThis());
            code.getfield().setField("loaded", BitSet.class);
            code.constant().setValue(index);
            code.invokevirtual().setMethod(BitSet.class, "set", void.class,
                new Class[]{ int.class });
            go2.setTarget(code.aload().setThis());
            code.getfield().setField("objects", Object[].class);
            code.constant().setValue(objectCount);
            code.aload().setLocal(local);
            code.aastore();
        }
        if (!usesImplData(fmd))
            return;

        // storeImplData(sm, i, loaded.get(i);
        code.aload().setThis();
        code.aload().setParam(0);
        code.constant().setValue(index);
        code.aload().setThis();
        code.getfield().setField("loaded", BitSet.class);
        code.constant().setValue(index);
        code.invokevirtual().setMethod(BitSet.class, "get", boolean.class,
            new Class[]{ int.class });
        code.invokevirtual().setMethod("storeImplData", void.class,
            new Class[]{ OpenJPAStateManager.class, int.class, boolean.class });
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
     * Add method which defers to AbstractPCData.
     */
    protected void callAbstractPCData(BCClass bc, String name, Class<?> retType,
        Class<?>[] args) {
        BCMethod meth = bc.declareMethod(name, retType, args);
        Code code = meth.getCode(true);
        code.aload().setThis();
        for (int i = 0; i < args.length; i++)
            code.xload().setParam(i).setType(args[i]);
        code.invokevirtual().setMethod(AbstractPCData.class, name, retType,
            args);
        if (!void.class.equals(retType))
            code.xreturn().setType(retType);
        code.calculateMaxLocals();
        code.calculateMaxStack();
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

        PCDataGenerator getStorageGenerator();

        void setStorageGenerator (PCDataGenerator generator);
    }
}

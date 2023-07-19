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
package org.apache.openjpa.meta;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
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
import org.apache.xbean.asm9.tree.InsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

/**
 * Creates implementations of managed interfaces.  Will throw exceptions
 * on unknown properties.
 *
 * @author Steve Kim
 */
class InterfaceImplGenerator {
    private static final Localizer _loc = Localizer.forPackage(InterfaceImplGenerator.class);
    private static final String POSTFIX = "openjpaimpl";

    private final MetaDataRepository _repos;
    private final Map<Class<?>,Class<?>> _impls = new WeakHashMap<>();
    private final EnhancementProject _project = new EnhancementProject();


    /**
     * Constructor.  Supply repository.
     */
    public InterfaceImplGenerator(MetaDataRepository repos) {
        _repos = repos;
    }

    /**
     * Create a concrete implementation of the given type, possibly
     * returning a cached version of the class.
     */
    public synchronized Class<?> createImpl(ClassMetaData meta) {
        Class<?> iface = meta.getDescribedType();

        // check cache.
        Class<?> impl = _impls.get(iface);
        if (impl != null)
            return impl;

        // distinct temp project / loader for enhancing
        EnhancementProject _enhProject = new EnhancementProject();

        ClassLoader parentLoader = AccessController.doPrivileged(J2DoPrivHelper.getClassLoaderAction(iface));
        EnhancementClassLoader loader = new EnhancementClassLoader(_project, parentLoader);
        ClassNodeTracker bc = _project.loadClass(getClassName(meta), loader);
        bc.declareInterface(iface);
        ClassMetaData sup = meta.getPCSuperclassMetaData();
        if (sup != null) {
            bc.getClassNode().superName = Type.getInternalName(sup.getInterfaceImpl());
        }

        FieldMetaData[] fields = meta.getDeclaredFields();
        Set<Method> methods = new HashSet<>();

        for (FieldMetaData field : fields) {
            addField(bc, iface, field, methods);
        }
        invalidateNonBeanMethods(bc, iface, methods);

        // first load the base Class<?> as the enhancer requires the class
        // to be available
        try {
            meta.setInterfaceImpl(Class.forName(bc.getClassNode().name.replace("/", "."), true, loader));
        } catch (Throwable t) {
            throw new InternalException(_loc.get("interface-load", iface, loader), t).setFatal(true);
        }

        // copy the current class bytecode into the enhancer project.
        final byte[] classBytes = AsmHelper.toByteArray(bc);
        final ClassNodeTracker bcEnh = _enhProject.loadClass(classBytes, parentLoader);
        PCEnhancer enhancer = new PCEnhancer(_repos, bcEnh, meta);

        int result = enhancer.run();
        if (result != PCEnhancer.ENHANCE_PC)
            throw new InternalException(_loc.get("interface-badenhance",
                iface)).setFatal(true);
        try {
            // load the Class<?> for real.
            EnhancementProject finalProject = new EnhancementProject();
            EnhancementClassLoader finalLoader = new EnhancementClassLoader(finalProject, parentLoader);
            final byte[] classBytes2 = AsmHelper.toByteArray(enhancer.getPCBytecode());

            // this is just to make the ClassLoader aware of the bytecode for the enhanced class
            finalProject.loadClass(classBytes2, finalLoader);

            String pcClassName = enhancer.getPCBytecode().getClassNode().name.replace("/", ".");
            impl = Class.forName(pcClassName, true, finalLoader);

        } catch (Throwable t) {
            //X throw new InternalException(_loc.get("interface-load2", iface, enhLoader), t).setFatal(true);
            throw new InternalException(_loc.get("interface-load2", iface, loader), t).setFatal(true);
        }
        // cache the generated impl.
        _impls.put(iface, impl);
        return impl;
    }

    /**
     * Add bean getters and setters, also recording seen methods
     * into the given set.
     */
    private void addField (ClassNodeTracker cnt, Class<?> iface, FieldMetaData fmd, Set<Method> methods) {
        final ClassNode classNode = cnt.getClassNode();
        String fieldName = fmd.getName();
        Class<?> type = fmd.getDeclaredType();
        FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE, fieldName, Type.getDescriptor(type), null, null);
        classNode.fields.add(field);

        // getter
        String getterName = (isGetter(iface, fmd) ? "get" : "is") + StringUtil.capitalize(fieldName);
        MethodNode meth = new MethodNode(Opcodes.ACC_PUBLIC,
                                         getterName,
                                         Type.getMethodDescriptor(Type.getType(type)),
                                         null, null);
        classNode.methods.add(meth);
        meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        meth.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, fieldName, Type.getDescriptor(type)));
        meth.instructions.add(new InsnNode(AsmHelper.getReturnInsn(type)));
        methods.add(getMethodSafe(iface, meth.name, null));

        // setter
        String setterName = "set" + StringUtil.capitalize(fieldName);
        meth = new MethodNode(Opcodes.ACC_PUBLIC,
                              setterName,
                              Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(type)),
                              null, null);
        classNode.methods.add(meth);
        meth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        meth.instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(type), 1)); // 1st parameter
        meth.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, fieldName, Type.getDescriptor(type)));
        meth.instructions.add(new InsnNode(Opcodes.RETURN));
        methods.add(getMethodSafe(iface, meth.name, type));
    }

    /**
     * Invalidate methods on the interface which are not managed.
     */
    private void invalidateNonBeanMethods(ClassNodeTracker cnt, Class<?> iface, Set<Method> methods) {
        Method[] meths = AccessController.doPrivileged(J2DoPrivHelper.getDeclaredMethodsAction(iface));


        Class<?> unimplementedExceptionType = _repos.getMetaDataFactory().getDefaults().getUnimplementedExceptionType();

        for (Method method : meths) {
            if (methods.contains(method)) {
                continue;
            }
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC,
                                                   method.getName(),
                                                   Type.getMethodDescriptor(method),
                                                   null, null);
            methodNode.instructions.add(AsmHelper.throwException(unimplementedExceptionType));
            cnt.getClassNode().methods.add(methodNode);
        }
    }

    /**
     * Return a unique Class<?> name.
     */
    protected final String getClassName(ClassMetaData meta) {
        Class<?> iface = meta.getDescribedType();
        return iface.getName() + "$" + System.identityHashCode(iface) + POSTFIX;
    }

    /**
     * Convenience method to return the given method / arg.
     */
    private static Method getMethodSafe(Class<?> iface, String name, Class<?> arg) {
        try {
            return AccessController.doPrivileged(
                J2DoPrivHelper.getDeclaredMethodAction(
                    iface, name, arg == null ? null : new Class[]{arg}));
        } catch (PrivilegedActionException pae) {
            throw new InternalException (_loc.get ("interface-mismatch", name));
        }
    }

    private static boolean isGetter(Class<?> iface, FieldMetaData fmd) {
        if (fmd.getType() != boolean.class && fmd.getType() != Boolean.class)
            return true;
        try {
            Method meth = AccessController.doPrivileged(
                J2DoPrivHelper.getDeclaredMethodAction(iface, "is" +
                    StringUtil.capitalize(fmd.getName()), (Class[]) null));
            return meth == null;
        } catch (PrivilegedActionException pae) {}
        return true;
    }

    boolean isImplType(Class<?> cls) {
        return (cls.getName().endsWith(POSTFIX)
            && cls.getName().indexOf('$') != -1);
    }

    public Class<?> toManagedInterface(Class<?> cls) {
        Class<?>[] ifaces = cls.getInterfaces();
        for (Class<?> iface : ifaces) {
            if (_impls.get(iface) == cls)
                return iface;
        }
        throw new IllegalArgumentException(cls.getName());
    }
}

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
package org.apache.openjpa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.lib.util.ClassUtil;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Opcodes;
import org.junit.Test;

/**
 * Tests that {@link ImplHelper#getEntityClass} unwraps generated
 * pcsubclasses, including nested ones.
 */
public class TestImplHelperEntityClass {

    public static class SimpleEntity {
    }

    @Test
    public void testNullReturnsNull() {
        assertNull(ImplHelper.getEntityClass(null));
    }

    @Test
    public void testPlainClassReturnsActualClass() {
        assertEquals(SimpleEntity.class,
            ImplHelper.getEntityClass(new SimpleEntity()));
    }

    @Test
    public void testUnwrapsNestedPCSubclasses() throws Exception {
        SubclassLoader loader = new SubclassLoader(getClass().getClassLoader());
        Class<?> sub = loader.definePCSubclass(SimpleEntity.class);
        Class<?> subSub = loader.definePCSubclass(sub);

        assertEquals(SimpleEntity.class,
            ImplHelper.getEntityClass(sub.getDeclaredConstructor().newInstance()));
        assertEquals(SimpleEntity.class,
            ImplHelper.getEntityClass(subSub.getDeclaredConstructor().newInstance()));
    }

    /**
     * Defines classes whose names match the ones the enhancer generates for
     * dynamically-created persistence-capable subclasses.
     */
    private static class SubclassLoader extends ClassLoader {

        SubclassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> definePCSubclass(Class<?> superclass) {
            String name = ClassUtil.getPackageName(PCEnhancer.class) + "."
                + superclass.getName().replace('.', '$')
                + PCEnhancer.PCSUBCLASS_SUFFIX;
            byte[] bytes = generate(name.replace('.', '/'),
                superclass.getName().replace('.', '/'));
            return defineClass(name, bytes, 0, bytes.length);
        }

        private byte[] generate(String internalName, String superName) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, internalName, null,
                superName, null);
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>",
                "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
            cw.visitEnd();
            return cw.toByteArray();
        }
    }
}

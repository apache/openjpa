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
package org.apache.openjpa.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;

/**
 * Helper to keep track of generated methods when using ASM ClassWriter.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class ClassWriterTracker {

    private final ClassWriter cw;
    private List<String> createdMethods = new ArrayList<>();

    public ClassWriterTracker(ClassWriter cw) {
        this.cw = cw;
    }

    public ClassWriter getCw() {
        return cw;
    }

    public MethodVisitor visitMethod(final int access,
                                    final String name,
                                    final String descriptor,
                                    final String signature,
                                    final String[] exceptionTypes) {
        MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptionTypes);

        createdMethods.add(name + descriptor);

        return mv;
    }

    public boolean hasMethod(final String name, final String descriptor) {
        return createdMethods.contains(name + descriptor);
    }
}

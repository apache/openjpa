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
package org.apache.openjpa.enhance.asm;

import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassNodeTracker;


/**
 * A special ClassLoader to handle classes currently under bytecode enhancement.
 * Inspired by the Serp BCClassLoader, but for ASM based enhancement.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author: Abe White
 */
public class EnhancementClassLoader extends ClassLoader {

    private final EnhancementProject project;

    public EnhancementClassLoader(EnhancementProject project) {
        this.project = project;
    }

    public EnhancementClassLoader(ClassLoader parent, EnhancementProject project) {
        super(parent);
        this.project = project;
    }

    public EnhancementProject getProject() {
        return project;
    }


    protected Class findClass(String name) throws ClassNotFoundException {
        byte[] bytes;
        try {
            ClassNodeTracker type;
            if (!project.containsClass(name)) {
                type = createClass(name);
            }
            else {
                type = project.loadClass(name);
            }
            if (type == null) {
                throw new ClassNotFoundException(name);
            }

            bytes = AsmHelper.toByteArray(type);
        } catch (RuntimeException re) {
            throw new ClassNotFoundException(re.toString());
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * Override this method if unfound classes should be created on-the-fly.
     * Returns null by default.
     */
    protected ClassNodeTracker createClass(String name) {
        return null;
    }
}

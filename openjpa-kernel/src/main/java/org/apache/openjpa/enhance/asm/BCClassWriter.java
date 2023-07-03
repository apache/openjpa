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

import org.apache.xbean.asm9.ClassWriter;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class BCClassWriter extends ClassWriter {
    private final ClassLoader _loader;

    public BCClassWriter(int flags, ClassLoader loader) {
        super(flags);
        _loader = loader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        Class<?> class1;
        Class<?> class2;
        try {
            class1 = _loader.loadClass(type1.replace('/', '.'));
            class2 = _loader.loadClass(type2.replace('/', '.'));
        }
        catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        if (class1.isAssignableFrom(class2)) {
            return type1;
        }
        if (class2.isAssignableFrom(class1)) {
            return type2;
        }
        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        }
        do {
            class1 = class1.getSuperclass();
        } while (!class1.isAssignableFrom(class2));
        return class1.getName().replace('.', '/');
    }
}

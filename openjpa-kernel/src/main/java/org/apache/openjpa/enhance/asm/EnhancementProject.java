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

import java.util.HashMap;

import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassNodeTracker;
import org.apache.xbean.asm9.tree.ClassNode;


/**
 * Keep track of classes under enhancement.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class EnhancementProject {

    private HashMap<String, ClassNodeTracker> classNodTrackers = new HashMap<>();


    /**
     * Return true if the project already contains the given class.
     */
    public boolean containsClass(String type) {
        return classNodTrackers.containsKey(type);
    }


    /**
     * Load a class with the given name.
     *
     * @see #loadClass(String,ClassLoader)
     */
    public ClassNodeTracker loadClass(String name) {
        return loadClass(name, null);
    }

    /**
     * Load the bytecode for the class with the given name.
     * If a {@link ClassNodeTracker} with the given name already exists in this project,
     * it will be returned. Otherwise, a new {@link ClassNodeTracker} will be created
     * with the given name and returned. If the name represents an existing
     * type, the returned instance will contain the parsed bytecode for
     * that type. If the name is of a primitive or array type, the returned
     * instance will act accordingly.
     *
     * @param name the name of the class, including package
     * @param loader the class loader to use to search for an existing
     * class with the given name; if null defaults to the
     * context loader of the current thread
     * @throws RuntimeException on parse error
     */
    public ClassNodeTracker loadClass(String name, ClassLoader loader) {
        ClassNodeTracker cached = classNodTrackers.get(name);
        if (cached != null) {
            return cached;
        }

        // check for existing type
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        final ClassNode classNode = AsmHelper.readClassNode(loader, name);
        ClassNodeTracker cnt = new ClassNodeTracker(classNode, loader);
        classNodTrackers.put(name, cnt);
        return cnt;
    }

}

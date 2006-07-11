/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

import serp.bytecode.lowlevel.ConstantPool;

/**
 * Interface implemented by all bytecode entities. Entities must be able
 * to access the project, constant pool, and class loader of the current class.
 *
 * @author Abe White
 */
public interface BCEntity {

    /**
     * Return the project of the current class.
     */
    public Project getProject();

    /**
     * Return the constant pool of the current class.
     */
    public ConstantPool getPool();

    /**
     * Return the class loader to use when loading related classes.
     */
    public ClassLoader getClassLoader();

    /**
     * Return false if this entity has been removed from its parent; in this
     * case the results of any operations on the entity are undefined.
     */
    public boolean isValid();
}

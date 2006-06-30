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

import serp.bytecode.visitor.*;
import serp.util.*;

/**
 * A local variable contains the name, description, index and scope
 * of a local used in opcodes.
 * 
 * @author Abe White
 */
public class LocalVariable extends Local {
    LocalVariable(LocalVariableTable owner) {
        super(owner);
    }

    /**
     * The owning table.
     */
    public LocalVariableTable getLocalVariableTable() {
        return(LocalVariableTable) getTable();
    }

    /**
     * Return the type of this local.
     * If the type has not been set, this method will return null.
     */
    public Class getType() {
        String type = getTypeName();
        if (type == null)
            return null;
        return Strings.toClass(type, getClassLoader());
    }

    /**
     * Return the type of this local.
     * If the type has not been set, this method will return null.
     */
    public BCClass getTypeBC() {
        String type = getTypeName();
        if (type == null)
            return null;
        return getProject().loadClass(type, getClassLoader());
    }

    /**
     * Set the type of this local.
     */
    public void setType(Class type) {
        if (type == null)
            setType((String) null);
        else
            setType(type.getName());
    }

    /**
     * Set the type of this local.
     */
    public void setType(BCClass type) {
        if (type == null)
            setType((String) null);
        else
            setType(type.getName());
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterLocalVariable(this);
        visit.exitLocalVariable(this);
    }
}

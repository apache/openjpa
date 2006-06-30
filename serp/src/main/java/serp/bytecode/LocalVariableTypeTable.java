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

/**
 * Code blocks compiled from source have local variable type tables mapping
 * generics-using locals used in opcodes to their names and signatures.
 * 
 * @author Abe White
 */
public class LocalVariableTypeTable extends LocalTable {
    LocalVariableTypeTable(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    /**
     * Return all the locals of this method.
     */
    public LocalVariableType[] getLocalVariableTypes() {
        return(LocalVariableType[]) getLocals();
    }

    /**
     * Return the local with the given locals index, or null if none.
     */
    public LocalVariableType getLocalVariableType(int local) {
        return(LocalVariableType) getLocal(local);
    }

    /**
     * Return the local with the given name, or null if none. If multiple
     * locals have the given name, which is returned is undefined.
     */
    public LocalVariableType getLocalVariableType(String name) {
        return(LocalVariableType) getLocal(name);
    }

    /**
     * Return all locals with the given name, or empty array if none.
     */
    public LocalVariableType[] getLocalVariableTypes(String name) {
        return(LocalVariableType[]) getLocals(name);
    }

    /**
     * Import a local from another method/class. Note that
     * the program counter and length from the given local is copied
     * directly, and thus will be incorrect unless this method is the same
     * as the one the local is copied from, or the pc and length are reset.
     */
    public LocalVariableType addLocalVariableType(LocalVariableType local) {
        return(LocalVariableType) addLocal(local);
    }

    /**
     * Add a local to this table.
     */
    public LocalVariableType addLocalVariableType() {
        return(LocalVariableType) addLocal();
    }

    /**
     * Add a local to this table.
     */
    public LocalVariableType addLocalVariableType(String name, String type) {
        return(LocalVariableType) addLocal(name, type);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterLocalVariableTypeTable(this);

        LocalVariableType[] locals = (LocalVariableType[]) getLocals();
        for (int i = 0; i < locals.length; i++)
            locals[i].acceptVisit(visit);

        visit.exitLocalVariableTypeTable(this);
    }

    protected Local newLocal() {
        return new LocalVariableType(this);
    }

    protected Local[] newLocalArray(int size) {
        return new LocalVariableType[size];
    }
}

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Code blocks compiled from source have local tables mapping
 * locals used in opcodes to their names and descriptions.
 *
 * @author Abe White
 */
public abstract class LocalTable extends Attribute implements InstructionPtr {

    private List _locals = new ArrayList();

    LocalTable(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    /**
     * Return all the locals of this method.
     */
    public Local[] getLocals() {
        return (Local[]) _locals.toArray(newLocalArray(_locals.size()));
    }

    /**
     * Return the local with the given locals index, or null if none.
     */
    public Local getLocal(int local) {
        for (int i = 0; i < _locals.size(); i++)
            if (((Local) _locals.get(i)).getLocal() == local)
                return (Local) _locals.get(i);
        return null;
    }

    /**
     * Return the local with the given name, or null if none. If multiple
     * locals have the given name, which is returned is undefined.
     */
    public Local getLocal(String name) {
        String loc;
        for (int i = 0; i < _locals.size(); i++) {
            loc = ((Local) _locals.get(i)).getName();
            if ((loc == null && name == null)
                || (loc != null && loc.equals(name)))
                return (Local) _locals.get(i);
        }
        return null;
    }

    /**
     * Return all locals with the given name, or empty array if none.
     */
    public Local[] getLocals(String name) {
        List matches = new LinkedList();
        String loc;
        for (int i = 0; i < _locals.size(); i++) {
            loc = ((Local) _locals.get(i)).getName();
            if ((loc == null && name == null)
                || (loc != null && loc.equals(name)))
                matches.add(_locals.get(i));
        }
        return (Local[]) matches.toArray(newLocalArray(matches.size()));
    }

    /**
     * Set the locals of this table. This method is useful when
     * importing locals from another method.
     */
    public void setLocals(Local[] locals) {
        clear();
        if (locals != null)
            for (int i = 0; i < locals.length; i++)
                addLocal(locals[i]);
    }

    /**
     * Import a local from another method/class. Note that
     * the program counter and length from the given local is copied
     * directly, and thus will be incorrect unless this method is the same
     * as the one the local is copied from, or the pc and length are reset.
     */
    public Local addLocal(Local local) {
        Local newLocal = addLocal(local.getName(), local.getTypeName());
        newLocal.setStartPc(local.getStartPc());
        newLocal.setLength(local.getLength());
        return newLocal;
    }

    /**
     * Add a local to this table.
     */
    public Local addLocal() {
        Local local = newLocal();
        _locals.add(local);
        return local;
    }

    /**
     * Create a new element of this table.
     */
    protected abstract Local newLocal();

    /**
     * Create a new array.
     */
    protected abstract Local[] newLocalArray(int size);

    /**
     * Add a local to this table.
     */
    public Local addLocal(String name, String type) {
        Local local = addLocal();
        local.setName(name);
        local.setType(type);
        return local;
    }

    /**
     * Clear all locals from this table.
     */
    public void clear() {
        for (int i = 0; i < _locals.size(); i++)
            ((Local) _locals.get(i)).invalidate();
        _locals.clear();
    }

    /**
     * Removes the local with the given locals index from the table.
     *
     * @return true if a local was removed, false otherwise
     */
    public boolean removeLocal(int local) {
        return removeLocal(getLocal(local));
    }

    /**
     * Removes the local with the given name from this method.
     *
     * @return true if a local was removed, false otherwise
     */
    public boolean removeLocal(String name) {
        return removeLocal(getLocal(name));
    }

    /**
     * Removes a local from this method. After this method, the local
     * will be invalid, and the result of any operations on it is undefined.
     *
     * @return true if a local was removed, false otherwise
     */
    public boolean removeLocal(Local local) {
        if (local == null || !_locals.remove(local))
            return false;

        local.invalidate();
        return true;
    }

    public void updateTargets() {
        for (int i = 0; i < _locals.size(); i++)
            ((Local) _locals.get(i)).updateTargets();
    }

    public void replaceTarget(Instruction oldTarget, Instruction newTarget) {
        for (int i = 0; i < _locals.size(); i++)
            ((Local) _locals.get(i)).replaceTarget(oldTarget, newTarget);
    }

    public Code getCode() {
        return (Code) getOwner();
    }

    int getLength() {
        return 2 + 10 * _locals.size();
    }

    void read(Attribute other) {
        setLocals(((LocalTable) other).getLocals());
    }

    void read(DataInput in, int length) throws IOException {
        clear();
        int numLocals = in.readUnsignedShort();

        Local Local;
        for (int i = 0; i < numLocals; i++) {
            Local = addLocal();
            Local.read(in);
        }
    }

    void write(DataOutput out, int length) throws IOException {
        out.writeShort(_locals.size());
        for (int i = 0; i < _locals.size(); i++)
            ((Local) _locals.get(i)).write(out);
    }
}

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
import java.util.Collection;
import java.util.LinkedList;

import serp.bytecode.lowlevel.ComplexEntry;
import serp.bytecode.lowlevel.ConstantPool;
import serp.bytecode.lowlevel.UTF8Entry;

/**
 * A member field or method of a class.
 *
 * @author Abe White
 */
public abstract class BCMember extends Attributes {

    private BCClass _owner = null;
    private int _access = Constants.ACCESS_PRIVATE;
    private int _nameIndex = 0;
    private int _descriptorIndex = 0;
    private Collection _attrs = new LinkedList();

    BCMember(BCClass owner) {
        _owner = owner;
    }

    /**
     * Return the {@link BCClass} that declares this member.
     */
    public BCClass getDeclarer() {
        return _owner;
    }

    /////////////////////
    // Access operations
    /////////////////////

    /**
     * Return the access flags for this member as a bit array of
     * ACCESS_XXX constants from {@link Constants}. This can be used to
     * transfer access flags between members without getting/setting each
     * possible access flag. Defaults to {@link Constants#ACCESS_PRIVATE}
     */
    public int getAccessFlags() {
        return _access;
    }

    /**
     * Set the access flags for this member as a bit array of
     * ACCESS_XXX constants from {@link Constants}. This can be used to
     * transfer access flags between members without getting/setting each
     * possible access flag. Defaults to {@link Constants#ACCESS_PRIVATE}
     */
    public void setAccessFlags(int access) {
        _access = access;
    }

    /**
     * Manipulate the member access flags.
     */
    public boolean isPublic() {
        return (getAccessFlags() & Constants.ACCESS_PUBLIC) > 0;
    }

    /**
     * Manipulate the member access flags.
     */
    public void makePublic() {
        setAccessFlags(getAccessFlags() | Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PROTECTED);
    }

    /**
     * Manipulate the member access flags.
     */
    public boolean isProtected() {
        return (getAccessFlags() & Constants.ACCESS_PROTECTED) > 0;
    }

    /**
     * Manipulate the member access flags.
     */
    public void makeProtected() {
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() | Constants.ACCESS_PROTECTED);
    }

    /**
     * Manipulate the member access flags.
     */
    public boolean isPrivate() {
        return (getAccessFlags() & Constants.ACCESS_PRIVATE) > 0;
    }

    /**
     * Manipulate the member access flags.
     */
    public void makePrivate() {
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() | Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PROTECTED);
    }

    /**
     * Manipulate the member access flags.
     */
    public boolean isPackage() {
        boolean hasAccess = false;
        hasAccess = hasAccess
            || (getAccessFlags() & Constants.ACCESS_PRIVATE) > 0;
        hasAccess = hasAccess
            || (getAccessFlags() & Constants.ACCESS_PROTECTED) > 0;
        hasAccess = hasAccess
            || (getAccessFlags() & Constants.ACCESS_PUBLIC) > 0;
        return !hasAccess;
    }

    /**
     * Manipulate the member access flags.
     */
    public void makePackage() {
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PROTECTED);
    }

    /**
     * Manipulate the member access flags.
     */
    public boolean isFinal() {
        return (getAccessFlags() & Constants.ACCESS_FINAL) > 0;
    }

    /**
     * Manipulate the member access flags.
     */
    public void setFinal(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_FINAL);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_FINAL);
    }

    /**
     * Manipulate the member access flags.
     */
    public boolean isStatic() {
        return (getAccessFlags() & Constants.ACCESS_STATIC) > 0;
    }

    /**
     * Manipulate the member access flags.
     */
    public void setStatic(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_STATIC);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_STATIC);
    }

    /////////////////////////
    // Descriptor operations
    /////////////////////////

    /**
     * Return the index in the class {@link ConstantPool} of the
     * {@link UTF8Entry} holding the name of this member.
     */
    public int getNameIndex() {
        return _nameIndex;
    }

    /**
     * Set the index in the class {@link ConstantPool} of the
     * {@link UTF8Entry} holding the name of this member.
     */
    public void setNameIndex(int index) {
        String origName = getName();
        _nameIndex = index;

        // change all the references in the owning class
        setEntry(origName, getDescriptor());
    }

    /**
     * Return the index in the class {@link ConstantPool} of the
     * {@link UTF8Entry} holding the descriptor of this member.
     */
    public int getDescriptorIndex() {
        return _descriptorIndex;
    }

    /**
     * Set the index in the class {@link ConstantPool} of the
     * {@link UTF8Entry} holding the descriptor of this member.
     */
    public void setDescriptorIndex(int index) {
        String origDesc = getDescriptor();
        _descriptorIndex = index;

        // change all the references in the owning class
        setEntry(getName(), origDesc);
    }

    /**
     * Return the name of this member.
     */
    public String getName() {
        return ((UTF8Entry) getPool().getEntry(_nameIndex)).getValue();
    }

    /**
     * Set the name of this member.
     */
    public void setName(String name) {
        String origName = getName();

        // reset the name
        _nameIndex = getPool().findUTF8Entry(name, true);

        // change all references in the owning class
        setEntry(origName, getDescriptor());
    }

    /**
     * Return the descriptor of this member, in internal form.
     */
    public String getDescriptor() {
        return ((UTF8Entry) getPool().getEntry(_descriptorIndex)).getValue();
    }

    /**
     * Set the descriptor of this member.
     */
    public void setDescriptor(String desc) {
        String origDesc = getDescriptor();

        // reset the desc
        desc = getProject().getNameCache().getInternalForm(desc, true);
        _descriptorIndex = getPool().findUTF8Entry(desc, true);

        // change all the references in the owning class
        setEntry(getName(), origDesc);
    }

    /**
     * Resets the {@link ComplexEntry} of the owning class corresponding to
     * this member. Changes in the member will therefore propogate to all
     * code in the class.
     */
    private void setEntry(String origName, String origDesc) {
        // find the entry matching this member, if any
        String owner = getProject().getNameCache().getInternalForm
            (_owner.getName(), false);
        ConstantPool pool = getPool();

        int index;
        if (this instanceof BCField)
            index = pool.findFieldEntry(origName, origDesc, owner, false);
        else if (!_owner.isInterface())
            index = pool.findMethodEntry(origName, origDesc, owner, false);
        else
            index = pool.findInterfaceMethodEntry(origName, origDesc,
                owner, false);

        // change the entry to match the new info; this is dones so
        // that refs to the member in code will still be valid after the
        // change, without changing any other constants that happened to match
        // the old name and/or descriptor
        if (index != 0) {
            ComplexEntry complex = (ComplexEntry) pool.getEntry(index);
            int ntIndex = pool.findNameAndTypeEntry(getName(),
                getDescriptor(), true);
            complex.setNameAndTypeIndex(ntIndex);
        }
    }

    ///////////////////////
    // Convenience methods
    ///////////////////////

    /**
     * Convenience method to return deprecation information for the member.
     * Acts internally through the {@link Attributes} interface.
     */
    public boolean isDeprecated() {
        return getAttribute(Constants.ATTR_DEPRECATED) != null;
    }

    /**
     * Convenience method to set whether this member should be considered
     * deprecated. Acts internally through the {@link Attributes} interface.
     */
    public void setDeprecated(boolean on) {
        if (!on)
            removeAttribute(Constants.ATTR_DEPRECATED);
        else if (!isDeprecated())
            addAttribute(Constants.ATTR_DEPRECATED);
    }

    /**
     * Convenience method to return synthetic information for the member.
     * Acts internally through the {@link Attributes} interface.
     */
    public boolean isSynthetic() {
        return getAttribute(Constants.ATTR_SYNTHETIC) != null;
    }

    /**
     * Convenience method to set whether this member should be considered
     * synthetic. Acts internally through the {@link Attributes} interface.
     */
    public void setSynthetic(boolean on) {
        if (!on)
            removeAttribute(Constants.ATTR_SYNTHETIC);
        else if (!isSynthetic())
            addAttribute(Constants.ATTR_SYNTHETIC);
    }

    ////////////////////////////////
    // Implementation of Attributes
    ////////////////////////////////

    public Project getProject() {
        return _owner.getProject();
    }

    public ConstantPool getPool() {
        return _owner.getPool();
    }

    public ClassLoader getClassLoader() {
        return _owner.getClassLoader();
    }

    public boolean isValid() {
        return _owner != null;
    }

    Collection getAttributesHolder() {
        return _attrs;
    }

    /**
     * Either this method or {@link #read} must be called prior to use
     * of this class. The given descriptor must be in internal form.
     */
    void initialize(String name, String descriptor) {
        _nameIndex = getPool().findUTF8Entry(name, true);
        _descriptorIndex = getPool().findUTF8Entry(descriptor, true);
    }

    /**
     * Used when this member is deleted from its class.
     */
    void invalidate() {
        _owner = null;
    }

    void read(DataInput in) throws IOException {
        _access = in.readUnsignedShort();
        _nameIndex = in.readUnsignedShort();
        _descriptorIndex = in.readUnsignedShort();

        readAttributes(in);
    }

    void write(DataOutput out) throws IOException {
        out.writeShort(_access);
        out.writeShort(_nameIndex);
        out.writeShort(_descriptorIndex);

        writeAttributes(out);
    }
}

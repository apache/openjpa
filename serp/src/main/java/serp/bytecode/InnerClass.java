/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

import serp.bytecode.lowlevel.*;

import serp.bytecode.visitor.*;

import serp.util.*;

import java.io.*;


/**
 *  <p>Any referenced class that is not a package member is represented by
 *  this structure.  This includes member classes and interfaces.</p>
 *
 *  @author Abe White
 */
public class InnerClass implements BCEntity, VisitAcceptor {
    private int _index = 0;
    private int _nameIndex = 0;
    private int _ownerIndex = 0;
    private int _access = Constants.ACCESS_PRIVATE;
    private InnerClasses _owner = null;

    InnerClass(InnerClasses owner) {
        _owner = owner;
    }

    /**
     *  Inner classes are stored in an {@link InnerClasses} attribute.
     */
    public InnerClasses getOwner() {
        return _owner;
    }

    void invalidate() {
        _owner = null;
    }

    /////////////////////
    // Access operations
    /////////////////////

    /**
      *  Return the access flags of the inner class.
     */
    public int getAccessFlags() {
        return _access;
    }

    /**
      *  Set the access flags of the inner class.
     */
    public void setAccessFlags(int accessFlags) {
        _access = accessFlags;
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public boolean isPublic() {
        return (getAccessFlags() & Constants.ACCESS_PUBLIC) > 0;
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public void makePublic() {
        setAccessFlags(getAccessFlags() | Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PROTECTED);
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public boolean isProtected() {
        return (getAccessFlags() & Constants.ACCESS_PROTECTED) > 0;
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public void makeProtected() {
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() | Constants.ACCESS_PROTECTED);
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public boolean isPrivate() {
        return (getAccessFlags() & Constants.ACCESS_PRIVATE) > 0;
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public void makePrivate() {
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PUBLIC);
        setAccessFlags(getAccessFlags() | Constants.ACCESS_PRIVATE);
        setAccessFlags(getAccessFlags() & ~Constants.ACCESS_PROTECTED);
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public boolean isFinal() {
        return (getAccessFlags() & Constants.ACCESS_FINAL) > 0;
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public void setFinal(boolean on) {
        if (on) {
            setAccessFlags(getAccessFlags() | Constants.ACCESS_FINAL);
        } else {
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_FINAL);
        }
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public boolean isStatic() {
        return (getAccessFlags() & Constants.ACCESS_STATIC) > 0;
    }

    /**
     *  Manipulate the inner class access flags.
     */
    public void setStatic(boolean on) {
        if (on) {
            setAccessFlags(getAccessFlags() | Constants.ACCESS_STATIC);
        } else {
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_STATIC);
        }
    }

    /**
     *  Manipulate the class access flags.
     */
    public boolean isInterface() {
        return (getAccessFlags() & Constants.ACCESS_INTERFACE) > 0;
    }

    /**
     *  Manipulate the class access flags.
     */
    public void setInterface(boolean on) {
        if (on) {
            setAccessFlags(getAccessFlags() | Constants.ACCESS_INTERFACE);
            setAbstract(true);
        } else {
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_INTERFACE);
        }
    }

    /**
     *  Manipulate the class access flags.
     */
    public boolean isAbstract() {
        return (getAccessFlags() & Constants.ACCESS_ABSTRACT) > 0;
    }

    /**
     *  Manipulate the class access flags.
     */
    public void setAbstract(boolean on) {
        if (on) {
            setAccessFlags(getAccessFlags() | Constants.ACCESS_INTERFACE);
        } else {
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_INTERFACE);
        }
    }

    ////////////////////////////////
    // Name, type, owner operations
    ////////////////////////////////

    /**
     *  Return the {@link ConstantPool} index of the {@link UTF8Entry} that
     *  describes the simple name this class is referred to in source, or
     *  0 for anonymous classes.
     */
    public int getNameIndex() {
        return _nameIndex;
    }

    /**
     *  Set the {@link ConstantPool} index of the {@link UTF8Entry} that
     *  describes the simple name this class is referred to in source, or
     *  0 for anonymous classes.
     */
    public void setNameIndex(int nameIndex) {
        _nameIndex = nameIndex;
    }

    /**
     *  Return the simple name of this inner class, or null if anonymous.
     */
    public String getName() {
        if (getNameIndex() == 0) {
            return null;
        }

        return ((UTF8Entry) getPool().getEntry(getNameIndex())).getValue();
    }

    /**
     *  Set the simple name of this inner class.
     */
    public void setName(String name) {
        if (name == null) {
            setNameIndex(0);
        } else {
            setNameIndex(getPool().findUTF8Entry(name, true));
        }
    }

    /**
     *  Return the {@link ConstantPool} index of the {@link ClassEntry} that
     *  describes this class, or 0 if none.
     */
    public int getTypeIndex() {
        return _index;
    }

    /**
     *  Set the {@link ConstantPool} index of the {@link ClassEntry} that
     *  describes this class.
     */
    public void setTypeIndex(int index) {
        _index = index;
    }

    /**
     *  Return the full name of the inner class, or null if unset.
     */
    public String getTypeName() {
        if (getTypeIndex() == 0) {
            return null;
        }

        ClassEntry entry = (ClassEntry) getPool()
                                            .getEntry(getTypeIndex());

        return getProject().getNameCache()
                   .getExternalForm(entry.getNameEntry().getValue(), false);
    }

    /**
     *  Return the type of the inner class.
     *  If the type has not been set, this method will return null.
     */
    public Class getType() {
        String type = getTypeName();

        if (type == null) {
            return null;
        }

        return Strings.toClass(type, getClassLoader());
    }

    /**
     *  Return the type for this instruction.
     *  If the type has not been set, this method will return null.
     */
    public BCClass getTypeBC() {
        String type = getTypeName();

        if (type == null) {
            return null;
        }

        return getProject().loadClass(type, getClassLoader());
    }

    /**
     *  Set the type of this inner class.
     */
    public void setType(String type) {
        if (type == null) {
            setTypeIndex(0);
        } else {
            type = getProject().getNameCache().getInternalForm(type, false);
            setTypeIndex(getPool().findClassEntry(type, true));
        }
    }

    /**
     *  Set the type of this inner class.
     */
    public void setType(Class type) {
        if (type == null) {
            setType((String) null);
        } else {
            setType(type.getName());
        }
    }

    /**
     *  Set the type of this inner class.
     */
    public void setType(BCClass type) {
        if (type == null) {
            setType((String) null);
        } else {
            setType(type.getName());
        }
    }

    /**
     *  Return the {@link ConstantPool} index of the {@link ClassEntry} that
     *  describes the declaring class, or 0 if this class is not a member class.
     */
    public int getDeclarerIndex() {
        return _ownerIndex;
    }

    /**
     *  Set the {@link ConstantPool} index of the {@link ClassEntry} that
     *  describes the declaring class, or 0 if this class is not a member class.
     */
    public void setDeclarerIndex(int ownerIndex) {
        _ownerIndex = ownerIndex;
    }

    /**
     *  Return the full name of the declaring class, or null if unset/not a
     *  member.
     */
    public String getDeclarerName() {
        if (getDeclarerIndex() == 0) {
            return null;
        }

        ClassEntry entry = (ClassEntry) getPool().getEntry(getDeclarerIndex());

        return getProject().getNameCache()
                   .getExternalForm(entry.getNameEntry().getValue(), false);
    }

    /**
     *  Return the type of the declaring class.
     *  If the type has not been set or the class is not a member, this method
     *  will return null.
     */
    public Class getDeclarerType() {
        String type = getDeclarerName();

        if (type == null) {
            return null;
        }

        return Strings.toClass(type, getClassLoader());
    }

    /**
     *  Return the type for this instruction.
     *  If the type has not been set or the class is not a member, this method
     *  will return null.
     */
    public BCClass getDeclarerBC() {
        String type = getDeclarerName();

        if (type == null) {
            return null;
        }

        return getProject().loadClass(type, getClassLoader());
    }

    /**
     *  Set the type of this declaring class.
     */
    public void setDeclarer(String type) {
        if (type == null) {
            setDeclarerIndex(0);
        } else {
            type = getProject().getNameCache().getInternalForm(type, false);
            setDeclarerIndex(getPool().findClassEntry(type, true));
        }
    }

    /**
     *  Set the type of this declaring class.
     */
    public void setDeclarer(Class type) {
        if (type == null) {
            setDeclarer((String) null);
        } else {
            setDeclarer(type.getName());
        }
    }

    /**
     *  Set the type of this declaring class.
     */
    public void setDeclarer(BCClass type) {
        if (type == null) {
            setDeclarer((String) null);
        } else {
            setDeclarer(type.getName());
        }
    }

    ///////////////////////////
    // BCEntity implementation
    ///////////////////////////
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

    public void acceptVisit(BCVisitor visit) {
        visit.enterInnerClass(this);
        visit.exitInnerClass(this);
    }

    //////////////////
    // I/O operations
    //////////////////
    void read(DataInput in) throws IOException {
        setTypeIndex(in.readUnsignedShort());
        setDeclarerIndex(in.readUnsignedShort());
        setNameIndex(in.readUnsignedShort());
        setAccessFlags(in.readUnsignedShort());
    }

    void write(DataOutput out) throws IOException {
        out.writeShort(getTypeIndex());
        out.writeShort(getDeclarerIndex());
        out.writeShort(getNameIndex());
        out.writeShort(getAccessFlags());
    }
}

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

import java.util.*;


/**
 *  <p>The State type is extended by various concrete types to change
 *  the behavior of a {@link BCClass}.  All methods in this base
 *  implementation throw an {@link UnsupportedOperationException}</p>
 *
 *  @author Abe White
 */
class State {
    /**
     *  A singleton instance of this type that can be used to make a
     *  class invalid.
     */
    public static final State INVALID = new State();

    /**
     *  Return the magic number of the bytecode class.
     */
    public int getMagic() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Set the magic number of the bytecode class.
     */
    public void setMagic(int magic) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the major number of the bytecode class.
     */
    public int getMajorVersion() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Set the major version of the bytecode class.
     */
    public void setMajorVersion(int major) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the minor number of the bytecode class.
     */
    public int getMinorVersion() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Set the minor version of the bytecode class.
     */
    public void setMinorVersion(int minor) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the access flags of the bytecode class.
     */
    public int getAccessFlags() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Set the access flags of the bytecode class.
     */
    public void setAccessFlags(int access) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the {@link ConstantPool} index of the {@link ClassEntry}
     *  for this class, or 0 if none.
     */
    public int getIndex() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Set the {@link ConstantPool} index of the {@link ClassEntry}
     *  for this class.
     */
    public void setIndex(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the {@link ConstantPool} index of the {@link ClassEntry}
     *  for the superclass of this class, or 0 if none.
     */
    public int getSuperclassIndex() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Set the {@link ConstantPool} index of the {@link ClassEntry}
     *  for the superclass of this class.  Throws
     *  {@link UnsupportedOperationException} by default.
     */
    public void setSuperclassIndex(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the {@link ConstantPool} indexes of the {@link ClassEntry}s
     *  for the indexes of this class, or empty collection if none.  If the
     *  state does not support changing the interfaces, the returned
     *  collection should be immutable.
     */
    public Collection getInterfacesHolder() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the {@link BCField}s of this class, or empty collection if none.
     *  If the state does not support changing the fields, the returned
     *  collection should be immutable.
     */
    public Collection getFieldsHolder() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the {@link BCMethod}s of this class, or empty collection if none.
     *  If the state does not support changing the methods, the returned
     *  collection should be immutable.
     */
    public Collection getMethodsHolder() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the {@link Attribute}s of this class, or empty collection if
     *  none.  If the state does not support changing the attributes, the
     *  returned collection should be immutable.
     */
    public Collection getAttributesHolder() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the constant pool of the class.
     */
    public ConstantPool getPool() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the name of the class.  The name should be in a form suitable
     *  for a {@link Class#forName} call.
     */
    public String getName() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the name of the superclass.  The name should be in a form
     *  suitable for a {@link Class#forName} call, or null if none.
     */
    public String getSuperclassName() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return the name of the component type of this array, or null if not
     *  an array.  The name should be in a form suitable for a
     *  {@link Class#forName} call.
     */
    public String getComponentName() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return true if this class is a primitive.
     */
    public boolean isPrimitive() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Return true if this class is an array.
     */
    public boolean isArray() {
        throw new UnsupportedOperationException();
    }
}

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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import serp.bytecode.lowlevel.ClassEntry;
import serp.bytecode.lowlevel.ConstantPool;

/**
 * State implementing the behavior of an object type.
 *
 * @author Abe White
 */
class ObjectState extends State {

    private final ConstantPool _pool = new ConstantPool();
    private final NameCache _names;

    private int _index = 0;
    private int _superclassIndex = 0;
    private int _magic = Constants.VALID_MAGIC;
    private int _major = Constants.MAJOR_VERSION;
    private int _minor = Constants.MINOR_VERSION;
    private int _access = Constants.ACCESS_PUBLIC | Constants.ACCESS_SUPER;

    private final Collection _interfaces = new HashSet();
    private final Collection _fields = new LinkedList();
    private final Collection _methods = new LinkedList();
    private final Collection _attributes = new LinkedList();

    public ObjectState(NameCache names) {
        _names = names;
    }

    public int getMagic() {
        return _magic;
    }

    public void setMagic(int magic) {
        _magic = magic;
    }

    public int getMajorVersion() {
        return _major;
    }

    public void setMajorVersion(int major) {
        _major = major;
    }

    public int getMinorVersion() {
        return _minor;
    }

    public void setMinorVersion(int minor) {
        _minor = minor;
    }

    public int getAccessFlags() {
        return _access;
    }

    public void setAccessFlags(int access) {
        _access = access;
    }

    public int getIndex() {
        return _index;
    }

    public void setIndex(int index) {
        _index = index;
    }

    public int getSuperclassIndex() {
        return _superclassIndex;
    }

    public void setSuperclassIndex(int index) {
        _superclassIndex = index;
    }

    public Collection getInterfacesHolder() {
        return _interfaces;
    }

    public Collection getFieldsHolder() {
        return _fields;
    }

    public Collection getMethodsHolder() {
        return _methods;
    }

    public Collection getAttributesHolder() {
        return _attributes;
    }

    public ConstantPool getPool() {
        return _pool;
    }

    public String getName() {
        if (_index == 0)
            return null;
        return _names.getExternalForm(((ClassEntry) _pool.getEntry(_index)).
            getNameEntry().getValue(), false);
    }

    public String getSuperclassName() {
        if (_superclassIndex == 0)
            return null;
        return _names.getExternalForm(((ClassEntry) _pool.getEntry
            (_superclassIndex)).getNameEntry().getValue(), false);
    }

    public String getComponentName() {
        return null;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isArray() {
        return false;
    }
}

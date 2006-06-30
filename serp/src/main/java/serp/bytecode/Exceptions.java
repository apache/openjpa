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

import java.io.*;
import java.util.*;
import serp.bytecode.lowlevel.*;
import serp.bytecode.visitor.*;
import serp.util.*;

/**
 * Attribute declaring the checked exceptions a method can throw.
 * 
 * @author Abe White
 */
public class Exceptions extends Attribute {
    private List _indexes = new LinkedList();

    Exceptions(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    int getLength() {
        return 2 + 2 * _indexes.size();
    }

    /**
     * Return the owning method.
     */
    public BCMethod getMethod() {
        return(BCMethod) getOwner();
    }

    /**
     * Return the indexes in the class {@link ConstantPool} of the
     * {@link ClassEntry}s for the exception types thrown by this method, or
     * an empty array if none.
     */
    public int[] getExceptionIndexes() {
        int[] indexes = new int[_indexes.size()];
        Iterator itr = _indexes.iterator();
        for (int i = 0; i < indexes.length; i++)
            indexes[i] = ((Integer) itr.next()).intValue();
        return indexes;
    }

    /**
     * Set the indexes in the class {@link ConstantPool} of the
     * {@link ClassEntry}s for the exception types thrown by this method. Use
     * null or an empty array for none.
     */
    public void setExceptionIndexes(int[] exceptionIndexes) {
        _indexes.clear();
        if (exceptionIndexes != null)
            for (int i = 0; i < exceptionIndexes.length; i++)
                _indexes.add(Numbers.valueOf(exceptionIndexes[i]));
    }

    /**
     * Return the names of the exception types for this method, or an empty
     * array if none. The names will be in a form suitable for a
     * {@link Class#forName} call.
     */
    public String[] getExceptionNames() {
        String[] names = new String[_indexes.size()];
        Iterator itr = _indexes.iterator();
        int index;
        ClassEntry entry;
        for (int i = 0; i < names.length; i++) {
            index = ((Number) itr.next()).intValue();
            entry = (ClassEntry) getPool().getEntry(index);
            names[i] = getProject().getNameCache().getExternalForm
                (entry.getNameEntry().getValue(), false);
        }
        return names;
    }

    /**
     * Return the {@link Class} objects for the exception types for this
     * method, or an empty array if none.
     */
    public Class[] getExceptionTypes() {
        String[] names = getExceptionNames();
        Class[] types = new Class[names.length];
        for (int i = 0; i < names.length; i++)
            types[i] = Strings.toClass(names[i], getClassLoader());
        return types;
    }

    /**
     * Return bytecode for the exception types of this
     * method, or an empty array if none.
     */
    public BCClass[] getExceptionBCs() {
        String[] names = getExceptionNames();
        BCClass[] types = new BCClass[names.length];
        for (int i = 0; i < names.length; i++)
            types[i] = getProject().loadClass(names[i], getClassLoader());
        return types;
    }

    /**
     * Set the checked exceptions thrown by this method. Use null or an
     * empty array for none.
     */
    public void setExceptions(String[] exceptions) {
        if (exceptions != null)
            for (int i = 0; i < exceptions.length; i++)
                if (exceptions[i] == null)
                    throw new NullPointerException("exceptions[" + i
                        + "] = null");

        clear();
        if (exceptions != null)
            for (int i = 0; i < exceptions.length; i++)
                addException(exceptions[i]);
    }

    /**
     * Set the checked exceptions thrown by this method. Use null or an
     * empty array for none.
     */
    public void setExceptions(Class[] exceptions) {
        String[] names = null;
        if (exceptions != null) {
            names = new String[exceptions.length];
            for (int i = 0; i < exceptions.length; i++)
                names[i] = exceptions[i].getName();
        }
        setExceptions(names);
    }

    /**
     * Set the checked exceptions thrown by this method. Use null or an
     * empty array for none.
     */
    public void setExceptions(BCClass[] exceptions) {
        String[] names = null;
        if (exceptions != null) {
            names = new String[exceptions.length];
            for (int i = 0; i < exceptions.length; i++)
                names[i] = exceptions[i].getName();
        }
        setExceptions(names);
    }

    /**
     * Clear this method of all exception declarations.
     */
    public void clear() {
        _indexes.clear();
    }

    /**
     * Remove an exception type thrown by this method.
     * 
     * @return true if the method had the exception type, false otherwise
     */
    public boolean removeException(String type) {
        String internalForm = getProject().getNameCache().
            getInternalForm(type, false);
        ClassEntry entry;
        for (Iterator itr = _indexes.iterator(); itr.hasNext();) {
            entry = (ClassEntry) getPool().getEntry
                (((Integer) itr.next()).intValue());

            if (entry.getNameEntry().getValue().equals(internalForm)) {
                itr.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Remove an exception thrown by this method.
     * 
     * @return true if the method had the exception type, false otherwise
     */
    public boolean removeException(Class type) {
        if (type == null)
            return false;
        return removeException(type.getName());
    }

    /**
     * Remove an exception thrown by this method.
     * 
     * @return true if the method had the exception type, false otherwise
     */
    public boolean removeException(BCClass type) {
        if (type == null)
            return false;
        return removeException(type.getName());
    }

    /**
     * Add an exception type to those thrown by this method.
     */
    public void addException(String type) {
        int index = getPool().findClassEntry(getProject().getNameCache().
            getInternalForm(type, false), true);
        _indexes.add(Numbers.valueOf(index));
    }

    /**
     * Add an exception to those thrown by this method.
     */
    public void addException(Class type) {
        addException(type.getName());
    }

    /**
     * Add an exception to those thrown by this method.
     */
    public void addException(BCClass type) {
        addException(type.getName());
    }

    /**
     * Return true if the method declares that it throws the given
     * exception type.
     */
    public boolean throwsException(String type) {
        String[] exceptions = getExceptionNames();
        for (int i = 0; i < exceptions.length; i++)
            if (exceptions[i].equals(type))
                return true;
        return false;
    }

    /**
     * Return true if the method declares that it throws the given
     * exception type.
     */
    public boolean throwsException(Class type) {
        if (type == null)
            return false;
        return throwsException(type.getName());
    }

    /**
     * Return true if the method declares that it throws the given
     * exception type.
     */
    public boolean throwsException(BCClass type) {
        if (type == null)
            return false;
        return throwsException(type.getName());
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterExceptions(this);
        visit.exitExceptions(this);
    }

    void read(Attribute other) {
        setExceptions(((Exceptions) other).getExceptionNames());
    }

    void read(DataInput in, int length) throws IOException {
        _indexes.clear();
        int exceptionCount = in.readUnsignedShort();
        for (int i = 0; i < exceptionCount; i++)
            _indexes.add(Numbers.valueOf((int) in.readUnsignedShort()));
    }

    void write(DataOutput out, int length) throws IOException {
        out.writeShort(_indexes.size());
        for (Iterator itr = _indexes.iterator(); itr.hasNext();)
            out.writeShort(((Number) itr.next()).shortValue());
    }
}

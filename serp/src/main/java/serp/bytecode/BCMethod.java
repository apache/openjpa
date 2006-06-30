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
 * A method of a class.
 * 
 * @author Abe White
 */
public class BCMethod extends BCMember implements VisitAcceptor {
    BCMethod(BCClass owner) {
        super(owner);
    }

    /////////////////////
    // Access operations
    /////////////////////

    /**
     * Manipulate the method access flags.
     */
    public boolean isSynchronized() {
        return(getAccessFlags() & Constants.ACCESS_SYNCHRONIZED) > 0;
    }

    /**
     * Manipulate the method access flags.
     */
    public void setSynchronized(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_SYNCHRONIZED);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_SYNCHRONIZED);
    }

    /**
     * Manipulate the method access flags.
     */
    public boolean isNative() {
        return(getAccessFlags() & Constants.ACCESS_NATIVE) > 0;
    }

    /**
     * Manipulate the method access flags.
     */
    public void setNative(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_NATIVE);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_NATIVE);
    }

    /**
     * Manipulate the method access flags.
     */
    public boolean isAbstract() {
        return(getAccessFlags() & Constants.ACCESS_ABSTRACT) > 0;
    }

    /**
     * Manipulate the method access flags.
     */
    public void setAbstract(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_ABSTRACT);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_ABSTRACT);
    }

    /**
     * Manipulate the method access flags.
     */
    public boolean isStrict() {
        return(getAccessFlags() & Constants.ACCESS_STRICT) > 0;
    }

    /**
     * Manipulate the method access flags.
     */
    public void setStrict(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_STRICT);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_STRICT);
    }

    /////////////////////
    // Return operations
    /////////////////////

    /**
     * Return the name of the type returned by this method. The name
     * will be given in a form suitable for a {@link Class#forName} call.
     * 
     * @see BCMember#getDescriptor
     */
    public String getReturnName() {
        return getProject().getNameCache().getExternalForm(getProject().
            getNameCache().getDescriptorReturnName(getDescriptor()), false);
    }

    /**
     * Return the {@link Class} object for the return type of this method.
     * 
     * @see BCMember#getDescriptor
     */
    public Class getReturnType() {
        return Strings.toClass(getReturnName(), getClassLoader());
    }

    /**
     * Return the bytecode for the return type of this method.
     * 
     * @see BCMember#getDescriptor
     */
    public BCClass getReturnBC() {
        return getProject().loadClass(getReturnName(), getClassLoader());
    }

    /**
     * Set the return type of this method.
     */
    public void setReturn(String name) {
        setDescriptor(getProject().getNameCache().getDescriptor(name,
            getParamNames()));
    }

    /**
     * Set the return type of this method.
     */
    public void setReturn(Class type) {
        setReturn(type.getName());
    }

    /**
     * Set the return type of this method.
     */
    public void setReturn(BCClass type) {
        setReturn(type.getName());
    }

    ////////////////////////
    // Parameter operations
    ////////////////////////

    /**
     * Return the names of all the parameter types for this method. The names
     * will be returned in a form suitable for a {@link Class#forName} call.
     * 
     * @see BCMember#getDescriptor
     */
    public String[] getParamNames() {
        // get the parameter types from the descriptor
        String[] params = getProject().getNameCache().
            getDescriptorParamNames(getDescriptor());

        // convert them to external form
        for (int i = 0; i < params.length; i++)
            params[i] = getProject().getNameCache().
                getExternalForm(params[i], false);
        return params;
    }

    /**
     * Return the {@link Class} objects for all the parameter types for this
     * method.
     * 
     * @see BCMember#getDescriptor
     */
    public Class[] getParamTypes() {
        String[] paramNames = getParamNames();
        Class[] params = new Class[paramNames.length];
        for (int i = 0; i < paramNames.length; i++)
            params[i] = Strings.toClass(paramNames[i], getClassLoader());
        return params;
    }

    /**
     * Return the bytecode for all the parameter types for this method.
     * 
     * @see BCMember#getDescriptor
     */
    public BCClass[] getParamBCs() {
        String[] paramNames = getParamNames();
        BCClass[] params = new BCClass[paramNames.length];
        for (int i = 0; i < paramNames.length; i++)
            params[i] = getProject().loadClass(paramNames[i], getClassLoader());
        return params;
    }

    /**
     * Set the parameter types of this method.
     * 
     * @see BCMember#setDescriptor
     */
    public void setParams(String[] names) {
        if (names == null)
            names = new String[0];
        setDescriptor(getProject().getNameCache().
            getDescriptor(getReturnName(), names));
    }

    /**
     * Set the parameter type of this method.
     * 
     * @see BCMember#setDescriptor
     */
    public void setParams(Class[] types) {
        if (types == null)
            setParams((String[]) null);
        else {
            String[] names = new String[types.length];
            for (int i = 0; i < types.length; i++)
                names[i] = types[i].getName();
            setParams(names);
        }
    }

    /**
     * Set the parameter type of this method.
     * 
     * @see BCMember#setDescriptor
     */
    public void setParams(BCClass[] types) {
        if (types == null)
            setParams((String[]) null);
        else {
            String[] names = new String[types.length];
            for (int i = 0; i < types.length; i++)
                names[i] = types[i].getName();
            setParams(names);
        }
    }

    /**
     * Add a parameter type to this method.
     */
    public void addParam(String type) {
        String[] origParams = getParamNames();
        String[] params = new String[origParams.length + 1];

        for (int i = 0; i < origParams.length; i++)
            params[i] = origParams[i];
        params[origParams.length] = type;
        setParams(params);
    }

    /**
     * Add a parameter type to this method.
     */
    public void addParam(Class type) {
        addParam(type.getName());
    }

    /**
     * Add a parameter type to this method.
     */
    public void addParam(BCClass type) {
        addParam(type.getName());
    }

    /**
     * Add a parameter type to this method.
     * 
     * @see java.util.List#add(int,Object)
     */
    public void addParam(int pos, String type) {
        String[] origParams = getParamNames();
        if (pos < 0 || pos >= origParams.length)
            throw new IndexOutOfBoundsException("pos = " + pos);

        String[] params = new String[origParams.length + 1];
        for (int i = 0, index = 0; i < params.length; i++) {
            if (i == pos)
                params[i] = type;
            else
                params[i] = origParams[index++];
        }
        setParams(params);
    }

    /**
     * Add a parameter type to this method.
     * 
     * @see java.util.List#add(int,Object)
     */
    public void addParam(int pos, Class type) {
        addParam(pos, type.getName());
    }

    /**
     * Add a parameter type to this method.
     * 
     * @see java.util.List#add(int,Object)
     */
    public void addParam(int pos, BCClass type) {
        addParam(pos, type.getName());
    }

    /**
     * Change a parameter type of this method.
     * 
     * @see java.util.List#set(int,Object)
     */
    public void setParam(int pos, String type) {
        String[] origParams = getParamNames();
        if (pos < 0 || pos >= origParams.length)
            throw new IndexOutOfBoundsException("pos = " + pos);

        String[] params = new String[origParams.length];
        for (int i = 0; i < params.length; i++) {
            if (i == pos)
                params[i] = type;
            else
                params[i] = origParams[i];
        }
        setParams(params);
    }

    /**
     * Change a parameter type of this method.
     * 
     * @see java.util.List#set(int,Object)
     */
    public void setParam(int pos, Class type) {
        setParam(pos, type.getName());
    }

    /**
     * Change a parameter type of this method.
     * 
     * @see java.util.List#set(int,Object)
     */
    public void setParam(int pos, BCClass type) {
        setParam(pos, type.getName());
    }

    /**
     * Clear all parameters from this method.
     */
    public void clearParams() {
        setParams((String[]) null);
    }

    /**
     * Remove a parameter from this method.
     */
    public void removeParam(int pos) {
        String[] origParams = getParamNames();
        if (pos < 0 || pos >= origParams.length)
            throw new IndexOutOfBoundsException("pos = " + pos);

        String[] params = new String[origParams.length - 1];
        for (int i = 0, index = 0; i < origParams.length; i++)
            if (i != pos)
                params[index++] = origParams[i];
        setParams(params);
    }

    ///////////////////////
    // Convenience methods
    ///////////////////////

    /**
     * Return the checked exceptions information for the method.
     * Acts internally through the {@link Attributes} interface.
     * 
     * @param add if true, a new exceptions attribute will be added
     * if not already present
     * @return the exceptions information, or null if none and the
     * <code>add</code> param is set to false
     */
    public Exceptions getExceptions(boolean add) {
        Exceptions exceptions = (Exceptions) getAttribute
            (Constants.ATTR_EXCEPTIONS);
        if (!add || exceptions != null)
            return exceptions;

        if (exceptions == null)
            exceptions = (Exceptions) addAttribute(Constants.ATTR_EXCEPTIONS);
        return exceptions;
    }

    /**
     * Remove the exceptions attribute for the method.
     * Acts internally through the {@link Attributes} interface.
     * 
     * @return true if there was a value to remove
     */
    public boolean removeExceptions() {
        return removeAttribute(Constants.ATTR_EXCEPTIONS);
    }

    /**
     * Return the code for the method. If the code already exists, its
     * iterator will be reset to the first instruction.
     * Acts internally through the {@link Attributes} interface.
     * 
     * @param add if true, a new code attribute will be added
     * if not already present
     * @return the code for the metohd, or null if none and the
     * <code>add</code> param is set to false
     */
    public Code getCode(boolean add) {
        Code code = (Code) getAttribute(Constants.ATTR_CODE);
        if (code != null) {
            code.beforeFirst();
            return code;
        }
        if (!add)
            return null;

        return(Code) addAttribute(Constants.ATTR_CODE);
    }

    /**
     * Remove the code attribute from the method.
     * Acts internally through the {@link Attributes} interface.
     * 
     * @return true if there was a value to remove
     */
    public boolean removeCode() {
        return removeAttribute(Constants.ATTR_CODE);
    }

    ////////////////////////////////
    // VisitAcceptor implementation
    ////////////////////////////////

    public void acceptVisit(BCVisitor visit) {
        visit.enterBCMethod(this);
        visitAttributes(visit);
        visit.exitBCMethod(this);
    }

    void initialize(String name, String descriptor) {
        super.initialize(name, descriptor);
        makePublic();
    }
}

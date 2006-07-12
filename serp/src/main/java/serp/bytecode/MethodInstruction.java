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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import serp.bytecode.lowlevel.ComplexEntry;
import serp.bytecode.lowlevel.ConstantPool;
import serp.bytecode.visitor.BCVisitor;
import serp.util.Strings;

/**
 * An instruction that invokes a method.
 *
 * @author Abe White
 */
public class MethodInstruction extends Instruction {

    private int _index = 0;

    MethodInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    int getLength() {
        if (getOpcode() == Constants.INVOKEINTERFACE)
            return super.getLength() + 4;
        return super.getLength() + 2;
    }

    public int getLogicalStackChange() {
        String ret = getMethodReturnName();
        if (ret == null)
            return 0;

        int stack = 0;

        // subtract a stack pos for the this ptr
        if (getOpcode() != Constants.INVOKESTATIC)
            stack--;

        // and for each arg
        String[] params = getMethodParamNames();
        for (int i = 0; i < params.length; i++)
            stack--;

        // add for the return value, if any
        if (!void.class.getName().equals(ret))
            stack++;

        return stack;
    }

    public int getStackChange() {
        String ret = getMethodReturnName();
        if (ret == null)
            return 0;

        int stack = 0;

        // subtract a stack pos for the this ptr
        if (getOpcode() != Constants.INVOKESTATIC)
            stack--;

        // and for each arg(2 for longs, doubles)
        String[] params = getMethodParamNames();
        for (int i = 0; i < params.length; i++, stack--)
            if (long.class.getName().equals(params[i])
                || double.class.getName().equals(params[i]))
                stack--;

        // add for the return value, if any
        if (!void.class.getName().equals(ret))
            stack++;
        if (long.class.getName().equals(ret)
            || double.class.getName().equals(ret))
            stack++;

        return stack;
    }

    /////////////////////
    // Method operations
    /////////////////////

    /**
     * Return the index in the class {@link ConstantPool} of the
     * {@link ComplexEntry} describing the method to operate on.
     */
    public int getMethodIndex() {
        return _index;
    }

    /**
     * Set the index in the class {@link ConstantPool} of the
     * {@link ComplexEntry} describing the method to operate on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodIndex(int index) {
        _index = index;
        return this;
    }

    /**
     * Return the method this instruction operates on, or null if not set.
     */
    public BCMethod getMethod() {
        String dec = getMethodDeclarerName();
        if (dec == null)
            return null;

        BCClass bc = getProject().loadClass(dec, getClassLoader());
        BCMethod[] meths = bc.getMethods(getMethodName(),
            getMethodParamNames());

        if (meths.length == 0)
            return null;
        return meths[0];
    }

    /**
     * Set the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(BCMethod method) {
        if (method == null)
            return setMethodIndex(0);
        return setMethod(method.getDeclarer().getName(), method.getName(),
            method.getReturnName(), method.getParamNames());
    }

    /**
     * Set the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(Method method) {
        if (method == null)
            return setMethodIndex(0);
        return setMethod(method.getDeclaringClass(), method.getName(),
            method.getReturnType(), method.getParameterTypes());
    }

    /**
     * Set the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(Constructor method) {
        if (method == null)
            return setMethodIndex(0);

        setOpcode(Constants.INVOKESPECIAL);
        return setMethod(method.getDeclaringClass(), "<init>",
            void.class, method.getParameterTypes());
    }

    /**
     * Set the method this instruction operates on.
     *
     * @param dec the full class name of the method's declaring class
     * @param name the method name
     * @param returnType the full class name of the method return type
     * @param param the full class names of the method param types
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(String dec, String name,
        String returnType, String[] params) {
        if (name == null && returnType == null && dec == null
            && (params == null || params.length == 0))
            return setMethodIndex(0);

        if (dec == null)
            dec = "";
        if (name == null)
            name = "";
        if (returnType == null)
            returnType = "";
        if (params == null)
            params = new String[0];

        NameCache cache = getProject().getNameCache();
        returnType = cache.getInternalForm(returnType, true);
        dec = cache.getInternalForm(dec, false);
        for (int i = 0; i < params.length; i++)
            params[i] = cache.getInternalForm(params[i], true);

        String desc = cache.getDescriptor(returnType, params);

        if (getOpcode() == Constants.INVOKEINTERFACE)
            return setMethodIndex(getPool().findInterfaceMethodEntry
                (dec, name, desc, true));
        return setMethodIndex(getPool().findMethodEntry
            (dec, name, desc, true));
    }

    /**
     * Set the method this instruction operates on, for methods that are
     * declared by the current class.
     *
     * @param name the method name
     * @param returnType the full class name of the method return type
     * @param param the full class names of the method param types
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(String name, String returnType,
        String[] params) {
        BCClass owner = getCode().getMethod().getDeclarer();
        return setMethod(owner.getName(), name, returnType, params);
    }

    /**
     * Set the method this instruction operates on.
     *
     * @param dec the method's declaring class
     * @param name the method name
     * @param returnType the class of the method return type
     * @param param the class of the method param types
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(Class dec, String name,
        Class returnType, Class[] params) {
        String decName = (dec == null) ? null : dec.getName();
        String returnName = (returnType == null) ? null : returnType.getName();
        String[] paramNames = null;
        if (params != null) {
            paramNames = new String[params.length];
            for (int i = 0; i < params.length; i++)
                paramNames[i] = params[i].getName();
        }
        return setMethod(decName, name, returnName, paramNames);
    }

    /**
     * Set the method this instruction operates on, for methods that are
     * declared by the current class.
     *
     * @param name the method name
     * @param returnType the class of the method return type
     * @param param the class of the method param types
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(String name, Class returnType,
        Class[] params) {
        BCClass owner = getCode().getMethod().getDeclarer();
        String returnName = (returnType == null) ? null : returnType.getName();
        String[] paramNames = null;
        if (params != null) {
            paramNames = new String[params.length];
            for (int i = 0; i < params.length; i++)
                paramNames[i] = params[i].getName();
        }
        return setMethod(owner.getName(), name, returnName, paramNames);
    }

    /**
     * Set the method this instruction operates on.
     *
     * @param dec the method's declaring class
     * @param name the method name
     * @param returnType the class of the method return type
     * @param param the class of the method param types
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(BCClass dec, String name,
        BCClass returnType, BCClass[] params) {
        String decName = (dec == null) ? null : dec.getName();
        String returnName = (returnType == null) ? null : returnType.getName();
        String[] paramNames = null;
        if (params != null) {
            paramNames = new String[params.length];
            for (int i = 0; i < params.length; i++)
                paramNames[i] = params[i].getName();
        }
        return setMethod(decName, name, returnName, paramNames);
    }

    /**
     * Set the method this instruction operates on, for methods that are
     * declared by the current class.
     *
     * @param name the method name
     * @param returnType the class of the method return type
     * @param param the class of the method param types
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethod(String name, BCClass returnType,
        BCClass[] params) {
        BCClass owner = getCode().getMethod().getDeclarer();
        String returnName = (returnType == null) ? null : returnType.getName();
        String[] paramNames = null;
        if (params != null) {
            paramNames = new String[params.length];
            for (int i = 0; i < params.length; i++)
                paramNames[i] = params[i].getName();
        }
        return setMethod(owner.getName(), name, returnName, paramNames);
    }

    /////////////////////////////////////////
    // Name, Return, Param, Owner operations
    /////////////////////////////////////////

    /**
     * Return the name of the method this instruction operates on, or null
     * if not set.
     */
    public String getMethodName() {
        int index = getMethodIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String name = entry.getNameAndTypeEntry().getNameEntry().getValue();
        if (name.length() == 0)
            return null;

        return name;
    }

    /**
     * Set the name of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodName(String name) {
        return setMethod(getMethodDeclarerName(), name,
            getMethodReturnName(), getMethodParamNames());
    }

    /**
     * Return the return type of the method this instruction operates on,
     * or null if not set.
     */
    public String getMethodReturnName() {
        int index = getMethodIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String desc = entry.getNameAndTypeEntry().getDescriptorEntry().
            getValue();
        NameCache cache = getProject().getNameCache();
        String name = cache.getExternalForm(cache.getDescriptorReturnName
            (desc), false);
        if (name.length() == 0)
            return null;

        return name;
    }

    /**
     * Return the return type of the method this instruction operates on,
     * or null if not set.
     */
    public Class getMethodReturnType() {
        String type = getMethodReturnName();
        if (type == null)
            return null;
        return Strings.toClass(type, getClassLoader());
    }

    /**
     * Return the return type of the method this instruction operates on,
     * or null if not set.
     */
    public BCClass getMethodReturnBC() {
        String type = getMethodReturnName();
        if (type == null)
            return null;
        return getProject().loadClass(type, getClassLoader());
    }

    /**
     * Set the return type of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodReturn(String type) {
        return setMethod(getMethodDeclarerName(), getMethodName(), type,
            getMethodParamNames());
    }

    /**
     * Set the return type of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodReturn(Class type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setMethodReturn(name);
    }

    /**
     * Set the return type of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodReturn(BCClass type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setMethodReturn(name);
    }

    /**
     * Return the param types of the method this instruction operates on,
     * or empty array if none.
     */
    public String[] getMethodParamNames() {
        int index = getMethodIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String desc = entry.getNameAndTypeEntry().getDescriptorEntry().
            getValue();
        NameCache cache = getProject().getNameCache();
        String[] names = cache.getDescriptorParamNames(desc);
        for (int i = 0; i < names.length; i++)
            names[i] = cache.getExternalForm(names[i], false);
        return names;
    }

    /**
     * Return the param types of the method this instruction operates on,
     * or empty array if none.
     */
    public Class[] getMethodParamTypes() {
        String[] paramNames = getMethodParamNames();
        Class[] params = new Class[paramNames.length];
        for (int i = 0; i < paramNames.length; i++)
            params[i] = Strings.toClass(paramNames[i], getClassLoader());
        return params;
    }

    /**
     * Return the param types of the method this instruction operates on,
     * or empty array if none.
     */
    public BCClass[] getMethodParamBCs() {
        String[] paramNames = getMethodParamNames();
        BCClass[] params = new BCClass[paramNames.length];
        for (int i = 0; i < paramNames.length; i++)
            params[i] = getProject().loadClass(paramNames[i], getClassLoader());
        return params;
    }

    /**
     * Set the param types of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodParams(String[] types) {
        return setMethod(getMethodDeclarerName(), getMethodName(),
            getMethodReturnName(), types);
    }

    /**
     * Set the param types of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public void setMethodParams(Class[] types) {
        if (types == null)
            setMethodParams((String[]) null);
        else {
            String[] names = new String[types.length];
            for (int i = 0; i < types.length; i++)
                names[i] = types[i].getName();
            setMethodParams(names);
        }
    }

    /**
     * Set the param types of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public void setMethodParams(BCClass[] types) {
        if (types == null)
            setMethodParams((String[]) null);
        else {
            String[] names = new String[types.length];
            for (int i = 0; i < types.length; i++)
                names[i] = types[i].getName();
            setMethodParams(names);
        }
    }

    /**
     * Return the declaring type of the method this instruction operates on,
     * or null if not set.
     */
    public String getMethodDeclarerName() {
        int index = getMethodIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String name = getProject().getNameCache().getExternalForm
            (entry.getClassEntry().getNameEntry().getValue(), false);
        if (name.length() == 0)
            return null;
        return name;
    }

    /**
     * Return the declaring type of the method this instruction operates on,
     * or null if not set.
     */
    public Class getMethodDeclarerType() {
        String type = getMethodDeclarerName();
        if (type == null)
            return null;
        return Strings.toClass(type, getClassLoader());
    }

    /**
     * Return the declaring type of the method this instruction operates on,
     * or null if not set.
     */
    public BCClass getMethodDeclarerBC() {
        String type = getMethodDeclarerName();
        if (type == null)
            return null;
        return getProject().loadClass(type, getClassLoader());
    }

    /**
     * Set the declaring type of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodDeclarer(String type) {
        return setMethod(type, getMethodName(), getMethodReturnName(),
            getMethodParamNames());
    }

    /**
     * Set the declaring type of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodDeclarer(Class type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setMethodDeclarer(name);
    }

    /**
     * Set the declaring type of the method this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public MethodInstruction setMethodDeclarer(BCClass type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setMethodDeclarer(name);
    }

    /**
     * MethodInstructions are equal if the method they reference is the same,
     * or if the method of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        if (!(other instanceof MethodInstruction))
            return false;
        if (!super.equalsInstruction(other))
            return false;

        MethodInstruction ins = (MethodInstruction) other;

        String s1 = getMethodName();
        String s2 = ins.getMethodName();
        if (!(s1 == null || s2 == null || s1.equals(s2)))
            return false;

        s1 = getMethodReturnName();
        s2 = ins.getMethodReturnName();
        if (!(s1 == null || s2 == null || s1.equals(s2)))
            return false;

        s1 = getMethodDeclarerName();
        s2 = ins.getMethodDeclarerName();
        if (!(s1 == null || s2 == null || s1.equals(s2)))
            return false;

        String[] p1 = getMethodParamNames();
        String[] p2 = ins.getMethodParamNames();
        if (!(p1.length == 0 || p2.length == 0 || p1.length == p2.length))
            return false;
        for (int i = 0; i < p1.length; i++)
            if (!(p1[i] == null || p2[i] == null || p1[i].equals(p2[i])))
                return false;

        return true;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterMethodInstruction(this);
        visit.exitMethodInstruction(this);
    }

    void read(Instruction orig) {
        super.read(orig);
        MethodInstruction ins = (MethodInstruction) orig;
        setMethod(ins.getMethodDeclarerName(), ins.getMethodName(),
            ins.getMethodReturnName(), ins.getMethodParamNames());
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setMethodIndex(in.readUnsignedShort());
        if (getOpcode() == Constants.INVOKEINTERFACE) {
            in.readByte();
            in.readByte();
        }
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeShort(getMethodIndex());
        if (getOpcode() == Constants.INVOKEINTERFACE) {
            String[] args = getMethodParamNames();
            int count = 1;
            for (int i = 0; i < args.length; i++, count++)
                if (long.class.getName().equals(args[i])
                    || double.class.getName().equals(args[i]))
                    count++;

            out.writeByte(count);
            out.writeByte(0);
        }
    }
}

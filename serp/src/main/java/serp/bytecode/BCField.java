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

import serp.bytecode.visitor.BCVisitor;
import serp.bytecode.visitor.VisitAcceptor;
import serp.util.Strings;

/**
 * A field of a class.
 *
 * @author Abe White
 */
public class BCField extends BCMember implements VisitAcceptor {

    BCField(BCClass owner) {
        super(owner);
    }

    /**
     * Manipulate the field access flags.
     */
    public boolean isVolatile() {
        return (getAccessFlags() & Constants.ACCESS_VOLATILE) > 0;
    }

    /**
     * Manipulate the field access flags.
     */
    public void setVolatile(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_VOLATILE);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_VOLATILE);
    }

    /**
     * Manipulate the field access flags.
     */
    public boolean isTransient() {
        return (getAccessFlags() & Constants.ACCESS_TRANSIENT) > 0;
    }

    /**
     * Manipulate the field access flags.
     */
    public void setTransient(boolean on) {
        if (on)
            setAccessFlags(getAccessFlags() | Constants.ACCESS_TRANSIENT);
        else
            setAccessFlags(getAccessFlags() & ~Constants.ACCESS_TRANSIENT);
    }

    /**
     * Return the name of the type of this field. The name will be given in
     * a form suitable for a {@link Class#forName} call.
     *
     * @see BCMember#getDescriptor
     */
    public String getTypeName() {
        return getProject().getNameCache().getExternalForm
            (getDescriptor(), false);
    }

    /**
     * Return the {@link Class} object for the type of this field.
     */
    public Class getType() {
        return Strings.toClass(getTypeName(), getClassLoader());
    }

    /**
     * Return the bytecode for the type of this field.
     */
    public BCClass getTypeBC() {
        return getProject().loadClass(getTypeName(), getClassLoader());
    }

    /**
     * Set the name of the type of this field.
     *
     * @see BCMember#setDescriptor
     */
    public void setType(String type) {
        setDescriptor(type);
    }

    /**
     * Set the type of this field.
     *
     * @see BCMember#setDescriptor
     */
    public void setType(Class type) {
        setType(type.getName());
    }

    /**
     * Set the type of this field.
     *
     * @see BCMember#setDescriptor
     */
    public void setType(BCClass type) {
        setType(type.getName());
    }

    /**
     * Return the constant value information for the field.
     * Acts internally through the {@link Attributes} interface.
     *
     * @param add if true, a new constant value attribute will be added
     * if not already present
     * @return the constant value information, or null if none and the
     *         <code>add</code> param is set to false
     */
    public ConstantValue getConstantValue(boolean add) {
        ConstantValue constant = (ConstantValue) getAttribute
            (Constants.ATTR_CONST);
        if (!add || constant != null)
            return constant;

        if (constant == null)
            constant = (ConstantValue) addAttribute(Constants.ATTR_CONST);
        return constant;
    }

    /**
     * Remove the constant value attribute for the field.
     * Acts internally through the {@link Attributes} interface.
     *
     * @return true if there was a value to remove
     */
    public boolean removeConstantValue() {
        return removeAttribute(Constants.ATTR_CONST);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterBCField(this);
        visitAttributes(visit);
        visit.exitBCField(this);
    }

    void initialize(String name, String descriptor) {
        super.initialize(name, descriptor);
        makePrivate();
    }
}

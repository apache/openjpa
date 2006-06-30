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

/**
 * Abstract superclass for all bytecode entities that hold attributes.
 * 
 * @author Abe White
 */
public abstract class Attributes implements BCEntity {
    /**
     * Return all the attributes owned by this entity.
     * 
     * @return all owned attributes, or empty array if none
     */
    public Attribute[] getAttributes() {
        Collection attrs = getAttributesHolder();
        return(Attribute[]) attrs.toArray(new Attribute[attrs.size()]);
    }

    /**
     * Return the attribute with the given name. If multiple attributes
     * share the name, which is returned is undefined.
     */
    public Attribute getAttribute(String name) {
        Collection attrs = getAttributesHolder();
        Attribute attr;
        for (Iterator itr = attrs.iterator(); itr.hasNext();) {
            attr = (Attribute) itr.next();
            if (attr.getName().equals(name))
                return attr;
        }

        return null;
    }

    /**
     * Return all attributes with the given name.
     * 
     * @return the matching attributes, or empty array if none
     */
    public Attribute[] getAttributes(String name) {
        List matches = new LinkedList();

        Collection attrs = getAttributesHolder();
        Attribute attr;
        for (Iterator itr = attrs.iterator(); itr.hasNext();) {
            attr = (Attribute) itr.next();
            if (attr.getName().equals(name))
                matches.add(attr);
        }
        return(Attribute[]) matches.toArray(new Attribute[matches.size()]);
    }

    /**
     * Set the attributes for this entity; this method is useful for importing
     * all attributes from another entity. Set to null or empty array if none.
     */
    public void setAttributes(Attribute[] attrs) {
        clearAttributes();
        if (attrs != null)
            for (int i = 0; i < attrs.length; i++)
                addAttribute(attrs[i]);
    }

    /**
     * Import an attribute from another entity, or make a copy of one
     * on this entity.
     */
    public Attribute addAttribute(Attribute attr) {
        Attribute newAttr = addAttribute(attr.getName());
        newAttr.read(attr);
        return newAttr;
    }

    /**
     * Add an attribute of the given type.
     */
    public Attribute addAttribute(String name) {
        Attribute attr = Attribute.create(name, this);
        getAttributesHolder().add(attr);
        return attr;
    }

    /**
     * Clear all attributes from this entity.
     */
    public void clearAttributes() {
        Collection attrs = getAttributesHolder();
        Attribute attr;
        for (Iterator itr = attrs.iterator(); itr.hasNext();) {
            attr = (Attribute) itr.next();
            itr.remove();
            attr.invalidate();
        }
    }

    /**
     * Remove all attributes with the given name from this entity.
     * 
     * @return true if an attribute was removed, false otherwise
     */
    public boolean removeAttribute(String name) {
        return removeAttribute(getAttribute(name));
    }

    /**
     * Remove the given attribute. After being removed, the attribute
     * is invalid, and the result of any operations on it are undefined.
     * 
     * @return true if the attribute was removed, false otherwise
     */
    public boolean removeAttribute(Attribute attribute) {
        if (attribute == null || !getAttributesHolder().remove(attribute))
            return false;
        attribute.invalidate();
        return true;
    }

    /**
     * Convenience method to be called by BCEntities when being visited
     * by a {@link BCVisitor}; this method will allow the visitor to visit all
     * attributes of this entity.
     */
    void visitAttributes(BCVisitor visit) {
        Attribute attr;
        for (Iterator itr = getAttributesHolder().iterator(); itr.hasNext();) {
            attr = (Attribute) itr.next();
            visit.enterAttribute(attr);
            attr.acceptVisit(visit);
            visit.exitAttribute(attr);
        }
    }

    /**
     * Build the attribute list from the given stream.
     * Relies on the ability of attributes to read themselves, and
     * requires access to the constant pool, which must already by read.
     */
    void readAttributes(DataInput in) throws IOException {
        Collection attrs = getAttributesHolder();
        attrs.clear();

        Attribute attribute;
        String name;
        for (int i = in.readUnsignedShort(); i > 0; i--) {
            name = ((UTF8Entry) getPool().getEntry(in.readUnsignedShort())).
                getValue();
            attribute = addAttribute(name);
            attribute.read(in, in.readInt());
        }
    }

    /**
     * Writes all the owned attributes to the given stream.
     * Relies on the ability of attributes to write themselves.
     */
    void writeAttributes(DataOutput out) throws IOException {
        Collection attrs = getAttributesHolder();
        out.writeShort(attrs.size());

        Attribute attribute;
        int length;
        for (Iterator itr = attrs.iterator(); itr.hasNext();) {
            attribute = (Attribute) itr.next();
            out.writeShort(attribute.getNameIndex());
            length = attribute.getLength();
            out.writeInt(length);
            attribute.write(out, length);
        }
    }

    /**
     * Return the collection used to hold the attributes of this entity.
     */
    abstract Collection getAttributesHolder();
}

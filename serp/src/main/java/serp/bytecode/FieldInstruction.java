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
import java.lang.reflect.Field;

import serp.bytecode.lowlevel.ComplexEntry;
import serp.bytecode.lowlevel.ConstantPool;
import serp.util.Strings;

/**
 * Instruction that takes as an argument a field to operate
 * on. Examples include <code>getfield, getstatic, setfield, setstatic</code>.
 *
 * @author Abe White
 */
public abstract class FieldInstruction extends Instruction {

    private int _index = 0;

    FieldInstruction(Code owner, int opcode) {
        super(owner, opcode);
    }

    int getLength() {
        return super.getLength() + 2;
    }

    ////////////////////
    // Field operations
    ////////////////////

    /**
     * Return the index in the class {@link ConstantPool} of the
     * {@link ComplexEntry} describing the field to operate on.
     */
    public int getFieldIndex() {
        return _index;
    }

    /**
     * Set the index in the class {@link ConstantPool} of the
     * {@link ComplexEntry} describing the field to operate on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldIndex(int index) {
        _index = index;
        return this;
    }

    /**
     * Return the field this instruction operates on, or null if not set.
     */
    public BCField getField() {
        String dec = getFieldDeclarerName();
        if (dec == null)
            return null;

        BCClass bc = getProject().loadClass(dec, getClassLoader());
        BCField[] fields = bc.getFields(getFieldName());

        if (fields.length == 0)
            return null;
        return fields[0];
    }

    /**
     * Set the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(BCField field) {
        if (field == null)
            return setFieldIndex(0);
        return setField(field.getDeclarer().getName(), field.getName(),
            field.getTypeName());
    }

    /**
     * Set the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(Field field) {
        if (field == null)
            return setFieldIndex(0);
        return setField(field.getDeclaringClass(), field.getName(),
            field.getType());
    }

    /**
     * Set the field this instruction operates on.
     *
     * @param dec  the full class name of the field's declaring class
     * @param name the field name
     * @param type the full class name of the field type
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(String dec, String name, String type) {
        if (dec == null && name == null && type == null)
            return setFieldIndex(0);

        if (dec == null)
            dec = "";
        if (name == null)
            name = "";
        if (type == null)
            type = "";

        dec = getProject().getNameCache().getInternalForm(dec, false);
        type = getProject().getNameCache().getInternalForm(type, true);

        return setFieldIndex(getPool().findFieldEntry(dec, name, type, true));
    }

    /**
     * Set the field this instruction operates on, for fields that are
     * declared by the current class.
     *
     * @param name the field name
     * @param type the full class name of the field type
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(String name, String type) {
        BCClass owner = getCode().getMethod().getDeclarer();
        return setField(owner.getName(), name, type);
    }

    /**
     * Set the field this instruction operates on.
     *
     * @param dec  the field's declaring class
     * @param name the field name
     * @param type the class of the field type
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(Class dec, String name, Class type) {
        String decName = (dec == null) ? null : dec.getName();
        String typeName = (type == null) ? null : type.getName();
        return setField(decName, name, typeName);
    }

    /**
     * Set the field this instruction operates on, for fields that are
     * declared by the current class.
     *
     * @param name the field name
     * @param type the class of the field type
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(String name, Class type) {
        BCClass owner = getCode().getMethod().getDeclarer();
        String typeName = (type == null) ? null : type.getName();
        return setField(owner.getName(), name, typeName);
    }

    /**
     * Set the field this instruction operates on.
     *
     * @param dec  the field's declaring class
     * @param name the field name
     * @param type the class of the field type
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(BCClass dec, String name, BCClass type) {
        String decName = (dec == null) ? null : dec.getName();
        String typeName = (type == null) ? null : type.getName();
        return setField(decName, name, typeName);
    }

    /**
     * Set the field this instruction operates on, for fields that are
     * declared by the current class.
     *
     * @param name the field name
     * @param type the class of the field type
     * @return this instruction, for method chaining
     */
    public FieldInstruction setField(String name, BCClass type) {
        BCClass owner = getCode().getMethod().getDeclarer();
        String typeName = (type == null) ? null : type.getName();
        return setField(owner.getName(), name, typeName);
    }

    ////////////////////////////////
    // Name, Type, Owner operations
    ////////////////////////////////

    /**
     * Return the name of the field this instruction operates on, or null
     * if not set.
     */
    public String getFieldName() {
        int index = getFieldIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String name = entry.getNameAndTypeEntry().getNameEntry().getValue();
        if (name.length() == 0)
            return null;
        return name;
    }

    /**
     * Set the name of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldName(String name) {
        return setField(getFieldDeclarerName(), name, getFieldTypeName());
    }

    /**
     * Return the type of the field this instruction operates on, or null
     * if not set.
     */
    public String getFieldTypeName() {
        int index = getFieldIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String name = getProject().getNameCache().getExternalForm(entry.
            getNameAndTypeEntry().getDescriptorEntry().getValue(), false);
        if (name.length() == 0)
            return null;
        return name;
    }

    /**
     * Return the type of the field this instruction operates on, or null
     * if not set.
     */
    public Class getFieldType() {
        String type = getFieldTypeName();
        if (type == null)
            return null;
        return Strings.toClass(type, getClassLoader());
    }

    /**
     * Return the type of the field this instruction operates on, or null
     * if not set.
     */
    public BCClass getFieldTypeBC() {
        String type = getFieldTypeName();
        if (type == null)
            return null;
        return getProject().loadClass(type, getClassLoader());
    }

    /**
     * Set the type of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldType(String type) {
        return setField(getFieldDeclarerName(), getFieldName(), type);
    }

    /**
     * Set the type of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldType(Class type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setFieldType(name);
    }

    /**
     * Set the type of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldType(BCClass type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setFieldType(name);
    }

    /**
     * Return the declaring class of the field this instruction operates on,
     * or null if not set.
     */
    public String getFieldDeclarerName() {
        int index = getFieldIndex();
        if (index == 0)
            return null;

        ComplexEntry entry = (ComplexEntry) getPool().getEntry(index);
        String name = getProject().getNameCache().getExternalForm(entry.
            getClassEntry().getNameEntry().getValue(), false);
        if (name.length() == 0)
            return null;
        return name;
    }

    /**
     * Return the declaring class of the field this instruction operates on,
     * or null if not set.
     */
    public Class getFieldDeclarerType() {
        String type = getFieldDeclarerName();
        if (type == null)
            return null;
        return Strings.toClass(type, getClassLoader());
    }

    /**
     * Return the declaring class of the field this instruction operates on,
     * or null if not set.
     */
    public BCClass getFieldDeclarerBC() {
        String type = getFieldDeclarerName();
        if (type == null)
            return null;
        return getProject().loadClass(type, getClassLoader());
    }

    /**
     * Set the declaring class of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldDeclarer(String type) {
        return setField(type, getFieldName(), getFieldTypeName());
    }

    /**
     * Set the declaring class of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldDeclarer(Class type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setFieldDeclarer(name);
    }

    /**
     * Set the declaring class of the field this instruction operates on.
     *
     * @return this instruction, for method chaining
     */
    public FieldInstruction setFieldDeclarer(BCClass type) {
        String name = null;
        if (type != null)
            name = type.getName();
        return setFieldDeclarer(name);
    }

    /**
     * FieldInstructions are equal if the field they reference is the same,
     * or if the field of either is unset.
     */
    public boolean equalsInstruction(Instruction other) {
        if (other == this)
            return true;
        if (!(other instanceof FieldInstruction))
            return false;
        if (!super.equalsInstruction(other))
            return false;

        FieldInstruction ins = (FieldInstruction) other;

        String s1 = getFieldName();
        String s2 = ins.getFieldName();
        if (!(s1 == null || s2 == null || s1.equals(s2)))
            return false;

        s1 = getFieldTypeName();
        s2 = ins.getFieldTypeName();
        if (!(s1 == null || s2 == null || s1.equals(s2)))
            return false;

        s1 = getFieldDeclarerName();
        s2 = ins.getFieldDeclarerName();
        if (!(s1 == null || s2 == null || s1.equals(s2)))
            return false;

        return true;
    }

    void read(Instruction orig) {
        super.read(orig);
        FieldInstruction ins = (FieldInstruction) orig;
        setField(ins.getFieldDeclarerName(), ins.getFieldName(),
            ins.getFieldTypeName());
    }

    void read(DataInput in) throws IOException {
        super.read(in);
        setFieldIndex(in.readUnsignedShort());
    }

    void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeShort(getFieldIndex());
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.enhance;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Localizer.Message;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.util.UserException;
import org.apache.xbean.asm9.tree.ClassNode;


/**
 * <p>Validates that a given type meets the JPA contract, plus a few
 * OpenJPA-specific additions for subclassing / redefinition:
 *
 *   <ul>
 *        <li>must have an accessible no-args constructor</li>
 *        <li>must be a public or protected class</li>
 *        <li>must not be final</li>
 *        <li>must not extend an enhanced class</li>
 *       <li>all persistent data represented by accessible setter/getter
 *            methods (persistent properties)</li>
 *        <li>if versioning is to be used, exactly one persistent property for
 *         the numeric version data</li> <!-- ##### is this true? -->
 *
 *     <li>When using property access, the backing field for a persistent
 *         property must be:
 *            <ul>
 *             <!-- ##### JPA validation of these needs to be tested -->
 *                <li>private</li>
 *                <li>set only in the designated setter,
 *                    in the constructor, or in {@link Object#clone()},
 *                 <code>readObject(ObjectInputStream)</code>, or
 *                    {@link Externalizable#readExternal(ObjectInput)}.</li>
 *                <li>read only in the designated getter and the
 *                    constructor.</li>
 *           </ul>
 *        </li>
 *    </ul>
 *
 * <p>If you use this technique and use the <code>new</code> keyword instead
 * of a OpenJPA-supplied construction routine, OpenJPA will need to do extra
 * work with persistent-new-flushed instances, since OpenJPA cannot in this
 * case track what happens to such an instance.</p>
 * <p>
 * @since 1.0.0
 */
public class PCSubclassValidator {

    private static final Localizer loc =
            Localizer.forPackage(PCSubclassValidator.class);

    private final ClassMetaData meta;
    private final ClassNode classNode;
    private final Log log;
    private final boolean failOnContractViolations;

    private Collection errors;
    private Collection contractViolations;

    public PCSubclassValidator(ClassMetaData meta, ClassNode classNode, Log log, boolean enforceContractViolations) {
        this.meta = meta;
        this.classNode = classNode;
        this.log = log;
        this.failOnContractViolations = enforceContractViolations;
    }

    public void assertCanSubclass() {
        Class<?> superclass = meta.getDescribedType();
        String name = superclass.getName();
        if (superclass.isInterface()) {
            addError(loc.get("subclasser-no-ifaces", name), meta);
        }
        if (Modifier.isFinal(superclass.getModifiers())) {
            addError(loc.get("subclasser-no-final-classes", name), meta);
        }
        if (Modifier.isPrivate(superclass.getModifiers())) {
            addError(loc.get("subclasser-no-private-classes", name), meta);
        }
        if (PersistenceCapable.class.isAssignableFrom(superclass)) {
            addError(loc.get("subclasser-super-already-pc", name), meta);
        }

        try {
            Constructor c = superclass.getDeclaredConstructor();
            if (!(Modifier.isProtected(c.getModifiers())
                    || Modifier.isPublic(c.getModifiers()))) {
                addError(loc.get("subclasser-private-ctor", name), meta);
            }
        }
        catch (NoSuchMethodException e) {
            addError(loc.get("subclasser-no-void-ctor", name), meta);
        }

        if (AccessCode.isProperty(meta.getAccessType())) {
            checkPropertiesAreInterceptable();
        }

        if (errors != null && !errors.isEmpty()) {
            throw new UserException(errors.toString());
        }
        else if (contractViolations != null &&
                !contractViolations.isEmpty() && log.isWarnEnabled()) {
            log.warn(contractViolations.toString());
        }
    }

    private void checkPropertiesAreInterceptable() {
        // just considers accessor methods for now.
        FieldMetaData[] fmds = meta.getFields();
        for (FieldMetaData fmd : fmds) {
            Method getter = getBackingMember(fmd);
            if (getter == null) {
                addError(loc.get("subclasser-no-getter",
                                 fmd.getName()), fmd);
                continue;
            }
            Field returnedField = checkGetterIsSubclassable(getter, fmd);

            Method setter = setterForField(fmd);
            if (setter == null) {
                addError(loc.get("subclasser-no-setter", fmd.getName()),
                         fmd);
                continue;
            }
            Field assignedField = checkSetterIsSubclassable(setter, fmd);
            if (assignedField == null) {
                continue;
            }

            if (!Objects.equals(assignedField, returnedField)) {
                addContractViolation(loc.get("subclasser-setter-getter-field-mismatch",
                                             fmd.getName(), returnedField, assignedField),
                                     fmd);
            }

            // ### scan through all the rest of the class to make sure it
            // ### doesn't use the field.
        }
    }

    private Method getBackingMember(FieldMetaData fmd) {
        Member back = fmd.getBackingMember();
        if (back instanceof Method) {
            return (Method) back;
        }

        Method getter = Reflection.findGetter(meta.getDescribedType(), fmd.getName(), false);
        if (getter != null) {
            fmd.backingMember(getter);
        }
        return getter;
    }

    private Method setterForField(FieldMetaData fmd) {
        try {
            return fmd.getDeclaringType().getDeclaredMethod("set" + StringUtil.capitalize(fmd.getName()), fmd.getDeclaredType());
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * @return the name of the field that is returned by <code>meth</code>, or
     * <code>null</code> if something other than a single field is
     * returned, or if it cannot be determined what is returned.
     */
    private Field checkGetterIsSubclassable(Method meth, FieldMetaData fmd) {
        checkMethodIsSubclassable(meth, fmd);

        Field field = PCEnhancer.getReturnedField(classNode, meth);

        if (field == null) {
            addContractViolation(loc.get("subclasser-invalid-getter", fmd.getName()), fmd);
            return null;
        }
        else {
            return field;
        }
    }

    /**
     * @return the field that is set in <code>meth</code>, or
     * <code>null</code> if something other than a single field is
     * set, or if it cannot be determined what is set.
     */
    private Field checkSetterIsSubclassable(Method meth, FieldMetaData fmd) {
        checkMethodIsSubclassable(meth, fmd);

        Field field = PCEnhancer.getAssignedField(classNode, meth);

        if (field == null) {
            addContractViolation(loc.get("subclasser-invalid-setter", fmd.getName()), fmd);
            return null;
        }
        else {
            return field;
        }
    }

    private void checkMethodIsSubclassable(Method meth, FieldMetaData fmd) {
        String className = fmd.getDefiningMetaData().
                getDescribedType().getName();
        if (!(Modifier.isProtected(meth.getModifiers())
                || Modifier.isPublic(meth.getModifiers()))) {
            addError(loc.get("subclasser-private-accessors-unsupported", className, meth.getName()), fmd);
        }
        if (Modifier.isFinal(meth.getModifiers())) {
            addError(loc.get("subclasser-final-methods-not-allowed", className, meth.getName()), fmd);
        }
        if (Modifier.isNative(meth.getModifiers())) {
            addContractViolation(loc.get("subclasser-native-methods-not-allowed", className, meth.getName()), fmd);
        }
        if (Modifier.isStatic(meth.getModifiers())) {
            addError(loc.get("subclasser-static-methods-not-supported", className, meth.getName()), fmd);
        }
    }

    private void addError(Message s, ClassMetaData cls) {
        if (errors == null) {
            errors = new ArrayList();
        }

        errors.add(loc.get("subclasser-error-meta", s,
                           cls.getDescribedType().getName(),
                           cls.getSourceFile()));
    }

    private void addError(Message s, FieldMetaData fmd) {
        if (errors == null) {
            errors = new ArrayList();
        }

        errors.add(loc.get("subclasser-error-field", s,
                           fmd.getFullName(),
                           fmd.getDeclaringMetaData().getSourceFile()));
    }

    private void addContractViolation(Message m, FieldMetaData fmd) {
        // add the violation as an error in case we're processing violations
        // as errors; this keeps them in the order that they were found rather
        // than just adding the violations to the end of the list.
        if (failOnContractViolations) {
            addError(m, fmd);
        }

        if (contractViolations == null) {
            contractViolations = new ArrayList();
        }

        contractViolations.add(loc.get
                ("subclasser-contract-violation-field", m.getMessage(),
                 fmd.getFullName(), fmd.getDeclaringMetaData().getSourceFile()));
    }
}

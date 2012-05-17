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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.ObjectId;
import org.apache.openjpa.kernel.StateManagerImpl;

/**
 * Implementation of the {@link PersistenceCapable} interface that can handle
 * the persistence-capable contract for instances that were not enhanced
 * before class load time.
 *
 * @since 1.0.0
 */
public class ReflectingPersistenceCapable
    implements PersistenceCapable, ManagedInstanceProvider {

    private Object o;
    private StateManager sm;
    private PersistenceCapable pcSubclassInstance;
    private ClassMetaData meta;

    public ReflectingPersistenceCapable(Object o, OpenJPAConfiguration conf) {
        this.o = o;
        Class type = o.getClass();
        pcSubclassInstance = PCRegistry.newInstance(type, null, false);
        meta = conf.getMetaDataRepositoryInstance()
            .getMetaData(type, null, true);
    }

    public int pcGetEnhancementContractVersion() {
        return PCEnhancer.ENHANCER_VERSION;
    }

    public Object pcGetGenericContext() {
        if (sm == null)
            return null;
        else
            return sm.getGenericContext();
    }

    public StateManager pcGetStateManager() {
        return sm;
    }

    public void pcReplaceStateManager(StateManager sm) {
        this.sm = sm;
    }

    public void pcProvideField(int i) {
        switch (meta.getField(i).getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                sm.providedBooleanField(this, i,
                    ((Boolean) getValue(i, o)).booleanValue());
                break;
            case JavaTypes.BYTE:
                sm.providedByteField(this, i,
                    ((Byte) getValue(i, o)).byteValue());
                break;
            case JavaTypes.CHAR:
                sm.providedCharField(this, i,
                    ((Character) getValue(i, o)).charValue());
                break;
            case JavaTypes.DOUBLE:
                sm.providedDoubleField(this, i,
                    ((Double) getValue(i, o)).doubleValue());
                break;
            case JavaTypes.FLOAT:
                sm.providedFloatField(this, i,
                    ((Float) getValue(i, o)).floatValue());
                break;
            case JavaTypes.INT:
                sm.providedIntField(this, i,
                    ((Integer) getValue(i, o)).intValue());
                break;
            case JavaTypes.LONG:
                sm.providedLongField(this, i,
                    ((Long) getValue(i, o)).longValue());
                break;
            case JavaTypes.SHORT:
                sm.providedShortField(this, i,
                    ((Short) getValue(i, o)).shortValue());
                break;
            case JavaTypes.STRING:
                sm.providedStringField(this, i,
                    (String) getValue(i, o));
                break;
            default:
                sm.providedObjectField(this, i, getValue(i, o));
                break;
        }
    }

    public void pcProvideFields(int[] fieldIndices) {
        for(int i = 0; i < fieldIndices.length; i++)
            pcProvideField(fieldIndices[i]);
    }

    public void pcReplaceField(int i) {
        switch(meta.getField(i).getTypeCode()) {
            case JavaTypes.BOOLEAN:
                setValue(i, o, Boolean.valueOf(
                    sm.replaceBooleanField(this, i)));
                break;
            case JavaTypes.BYTE:
                setValue(i, o, new Byte(sm.replaceByteField(this, i)));
                break;
            case JavaTypes.CHAR:
                setValue(i, o, new Character(sm.replaceCharField(this, i)));
                break;
            case JavaTypes.DOUBLE:
                setValue(i, o, new Double(sm.replaceDoubleField(this, i)));
                break;
            case JavaTypes.FLOAT:
                setValue(i, o, new Float(sm.replaceFloatField(this, i)));
                break;
            case JavaTypes.INT:
                setValue(i, o, new Integer(sm.replaceIntField(this, i)));
                break;
            case JavaTypes.LONG:
                setValue(i, o, new Long(sm.replaceLongField(this, i)));
                break;
            case JavaTypes.SHORT:
                setValue(i, o, new Short(sm.replaceShortField(this, i)));
                break;
            case JavaTypes.STRING:
                setValue(i, o, sm.replaceStringField(this, i));
                break;
            default:
                setValue(i, o, sm.replaceObjectField(this, i));
                break;
        }
    }

    public void pcReplaceFields(int[] fieldIndices) {
        for(int i = 0; i < fieldIndices.length; i++)
            pcReplaceField(fieldIndices[i]);
    }

    public void pcCopyField(Object fromObject, int i) {
        // this doesn't need switch treatment because we're just
        // reflecting on both sides, bypassing field managers.
        setValue(i, o, getValue(i, fromObject));
    }

    public void pcCopyFields(Object fromObject, int[] fieldIndices) {
        for(int i = 0; i < fieldIndices.length; i++)
            pcCopyField(fromObject, fieldIndices[i]);
    }

    public void pcDirty(String fieldName) {
        if (sm != null)
            sm.dirty(fieldName);
    }

    public Object pcFetchObjectId() {
        if (sm != null)
            return sm.fetchObjectId();
        else
            return null;
    }

    public Object pcGetVersion() {
        if (sm == null)
            return null;
        else
            return sm.getVersion();
    }

    public boolean pcIsDirty() {
        if (sm == null)
            return false;
        else {
            if (sm instanceof StateManagerImpl)
                ((StateManagerImpl) sm).dirtyCheck();
            return sm.isDirty();
        }
    }

    public boolean pcIsTransactional() {
        if (sm == null)
            return false;
        else
            return sm.isTransactional();
    }

    public boolean pcIsPersistent() {
        if (sm == null)
            return false;
        else
            return sm.isPersistent();
    }

    public boolean pcIsNew() {
        if (sm == null)
            return false;
        else
            return sm.isNew();
    }

    public boolean pcIsDeleted() {
        if (sm == null)
            return false;
        else
            return sm.isDeleted();
    }

    // null == unknown
    public Boolean pcIsDetached() {
        if (sm != null)
            return Boolean.valueOf(sm.isDetached());

        // ##### we could do a lot more here if a detached state field
        // ##### was specified.
        return null;
    }

    public PersistenceCapable pcNewInstance(StateManager sm, boolean clear) {
        return pcSubclassInstance.pcNewInstance(sm, clear);
    }

    public PersistenceCapable pcNewInstance(StateManager sm, Object oid,
        boolean clear) {
        return pcSubclassInstance.pcNewInstance(sm, oid, clear);
    }

    public Object pcNewObjectIdInstance() {
        FieldMetaData[] pkFields = meta.getPrimaryKeyFields();
        Object[] pks = new Object[pkFields.length];
        for (int i = 0; i < pkFields.length; i++)
            pks[i] = getValue(pkFields[i].getIndex(), o);
        return ApplicationIds.fromPKValues(pks, meta);
    }
    
    public Object pcNewObjectIdInstance(Object oid) {
        return pcSubclassInstance.pcNewObjectIdInstance(oid);
    }

    public void pcCopyKeyFieldsToObjectId(Object oid) {
        Object target;
        if (oid instanceof ObjectId)
            target = ((ObjectId) oid).getId();
        else
            target = oid;

        FieldMetaData[] pks = meta.getPrimaryKeyFields();
        for (int i = 0; i < pks.length; i++) {
            Object val = getValue(pks[i].getIndex(), o);
            Field f = Reflection.findField(target.getClass(), pks[i].getName(),
                true);
            Reflection.set(target, f, val);
        }
    }

    public void pcCopyKeyFieldsToObjectId(FieldSupplier supplier, Object obj) {
        // This is only ever invoked against PCs in the PCRegistry. Such PCs
        // will always be enhanced types or subtypes of user types, and will
        // never be a ReflectingPersistenceCapable.
        throw new InternalException();
    }

    public void pcCopyKeyFieldsFromObjectId(FieldConsumer consumer,
        Object obj) {
        // This is only ever invoked against PCs in the PCRegistry. Such PCs
        // will always be enhanced types or subtypes of user types, and will
        // never be a ReflectingPersistenceCapable.
        throw new InternalException();
    }

    public Object pcGetDetachedState() {
        // ##### we can implement this if a state field has been set
        return null;
    }

    public void pcSetDetachedState(Object state) {
        // StateManagerImpl will invoke this with null during instance
        // initialization
        if (state != null)
            throw new UnsupportedOperationException();
        // ##### we can implement this if a state field has been set
    }

    public Object getManagedInstance() {
        return o;
    }

    private Object getValue(int i, Object o) {
        if (meta.getAccessType() == ClassMetaData.ACCESS_PROPERTY) {
            if (!meta.isIntercepting()) {
                Method meth = Reflection.findGetter(meta.getDescribedType(),
                    meta.getField(i).getName(), true);
                return Reflection.get(o, meth);
            } else {
                Field field = Reflection.findField(meta.getDescribedType(),
                    toFieldName(i), true);
                return Reflection.get(o, field);
            }
        } else {
            Field field = (Field) meta.getField(i).getBackingMember();
            return Reflection.get(o, field);
        }
    }

    private String toFieldName(int i) {
        if (pcSubclassInstance instanceof AttributeTranslator)
            return ((AttributeTranslator) pcSubclassInstance)
                .pcAttributeIndexToFieldName(i);
        else
            return meta.getField(i).getName();
    }

    private void setValue(int i, Object o, Object val) {
        if (meta.getAccessType() == ClassMetaData.ACCESS_PROPERTY) {
            if (!meta.isIntercepting()) {
                Method meth = Reflection.findSetter(meta.getDescribedType(),
                    meta.getField(i).getName(), true);
                Reflection.set(o, meth, val);
            } else {
                Field field = Reflection.findField(meta.getDescribedType(),
                    toFieldName(i), true);
                Reflection.set(o, field, val);
            }
        } else {
            Field field = (Field) meta.getField(i).getBackingMember();
            Reflection.set(o, field, val);
        }
    }
}

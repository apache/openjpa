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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.ObjectId;

/**
 * Implementation of the {@link PersistenceCapable} interface that can handle
 * the persistence-capable contract for instances that were not enhanced
 * before class load time.
 *
 * @since 1.0.0
 */
public class ReflectingPersistenceCapable
    implements PersistenceCapable, ManagedInstanceProvider, Serializable {

    
    private static final long serialVersionUID = 1L;
    private Object o;
    private StateManager sm;

    // this will be reconstituted in readObject()
    private transient PersistenceCapable pcSubclassInstance;

    // this will reconstituted by a call to pcReplaceStateManager() by the
    // instance that has a reference to the deserialized data
    private transient ClassMetaData meta;

    private boolean serializationUserVisible = true;

    public ReflectingPersistenceCapable(Object o, OpenJPAConfiguration conf) {
        this.o = o;
        Class type = o.getClass();
        pcSubclassInstance = PCRegistry.newInstance(type, null, false);
        meta = conf.getMetaDataRepositoryInstance()
            .getMetaData(type, null, true);
    }

    @Override
    public int pcGetEnhancementContractVersion() {
        return PCEnhancer.ENHANCER_VERSION;
    }

    @Override
    public Object pcGetGenericContext() {
        if (sm == null)
            return null;
        else
            return sm.getGenericContext();
    }

    @Override
    public StateManager pcGetStateManager() {
        return sm;
    }

    @Override
    public void pcReplaceStateManager(StateManager sm) {
        this.sm = sm;
        if (meta == null && sm instanceof OpenJPAStateManager)
            meta = ((OpenJPAStateManager) sm).getMetaData();
    }

    @Override
    public void pcProvideField(int i) {
        Object value = getValue(i, o);
        switch (meta.getField(i).getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                sm.providedBooleanField(this, i, value == null ? false :
                        (Boolean) value);
                break;
            case JavaTypes.BYTE:
                sm.providedByteField(this, i, value == null ? 0 :
                        (Byte) value);
                break;
            case JavaTypes.CHAR:
                sm.providedCharField(this, i, value == null ? 0 :
                        (Character) value);
                break;
            case JavaTypes.DOUBLE:
                sm.providedDoubleField(this, i, value == null ? 0 :
                        (Double) value);
                break;
            case JavaTypes.FLOAT:
                sm.providedFloatField(this, i, value == null ? 0 :
                        (Float) value);
                break;
            case JavaTypes.INT:
                sm.providedIntField(this, i, value == null ? 0 :
                        (Integer) value);
                break;
            case JavaTypes.LONG:
                sm.providedLongField(this, i, value == null ? 0 :
                        (Long) value);
                break;
            case JavaTypes.SHORT:
                sm.providedShortField(this, i, value == null ? 0 :
                        (Short) value);
                break;
            case JavaTypes.STRING:
                sm.providedStringField(this, i, (String) value);
                break;
            default:
                sm.providedObjectField(this, i, value);
                break;
        }
    }

    @Override
    public void pcProvideFields(int[] fieldIndices) {
        for (int fieldIndex : fieldIndices) {
            pcProvideField(fieldIndex);
        }
    }

    @Override
    public void pcReplaceField(int i) {
        switch(meta.getField(i).getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                setValue(i, o, sm.replaceBooleanField(this, i));
                break;
            case JavaTypes.BYTE:
                setValue(i, o, sm.replaceByteField(this, i));
                break;
            case JavaTypes.CHAR:
                setValue(i, o, sm.replaceCharField(this, i));
                break;
            case JavaTypes.DOUBLE:
                setValue(i, o, sm.replaceDoubleField(this, i));
                break;
            case JavaTypes.FLOAT:
                setValue(i, o, sm.replaceFloatField(this, i));
                break;
            case JavaTypes.INT:
                setValue(i, o, sm.replaceIntField(this, i));
                break;
            case JavaTypes.LONG:
                setValue(i, o, sm.replaceLongField(this, i));
                break;
            case JavaTypes.SHORT:
                setValue(i, o, sm.replaceShortField(this, i));
                break;
            case JavaTypes.STRING:
                setValue(i, o, sm.replaceStringField(this, i));
                break;
            default:
                setValue(i, o, sm.replaceObjectField(this, i));
                break;
        }
    }

    @Override
    public void pcReplaceFields(int[] fieldIndices) {
        for (int fieldIndex : fieldIndices) {
            pcReplaceField(fieldIndex);
        }
    }

    public void pcCopyField(Object fromObject, int i) {
        // this doesn't need switch treatment because we're just
        // reflecting on both sides, bypassing field managers.
        setValue(i, o, getValue(i, fromObject));
    }

    @Override
    public void pcCopyFields(Object fromObject, int[] fieldIndices) {
        if (fromObject instanceof ReflectingPersistenceCapable)
            fromObject = ((ReflectingPersistenceCapable) fromObject)
                .getManagedInstance();

        for (int fieldIndex : fieldIndices) {
            pcCopyField(fromObject, fieldIndex);
        }
    }

    @Override
    public void pcDirty(String fieldName) {
        if (sm != null)
            sm.dirty(fieldName);
    }

    @Override
    public Object pcFetchObjectId() {
        if (sm != null)
            return sm.fetchObjectId();
        else
            return null;
    }

    @Override
    public Object pcGetVersion() {
        if (sm == null)
            return null;
        else
            return sm.getVersion();
    }

    @Override
    public boolean pcIsDirty() {
        if (sm == null)
            return false;
        else {
            if (sm instanceof StateManagerImpl)
                ((StateManagerImpl) sm).dirtyCheck();
            return sm.isDirty();
        }
    }

    @Override
    public boolean pcIsTransactional() {
        if (sm == null)
            return false;
        else
            return sm.isTransactional();
    }

    @Override
    public boolean pcIsPersistent() {
        if (sm == null)
            return false;
        else
            return sm.isPersistent();
    }

    @Override
    public boolean pcIsNew() {
        if (sm == null)
            return false;
        else
            return sm.isNew();
    }

    @Override
    public boolean pcIsDeleted() {
        if (sm == null)
            return false;
        else
            return sm.isDeleted();
    }

    // null == unknown
    @Override
    public Boolean pcIsDetached() {
        if (sm != null)
            return sm.isDetached();

        // ##### we could do a lot more here if a detached state field
        // ##### was specified.
        return null;
    }

    @Override
    public PersistenceCapable pcNewInstance(StateManager sm, boolean clear) {
        return pcSubclassInstance.pcNewInstance(sm, clear);
    }

    @Override
    public PersistenceCapable pcNewInstance(StateManager sm, Object oid,
        boolean clear) {
        return pcSubclassInstance.pcNewInstance(sm, oid, clear);
    }

    @Override
    public Object pcNewObjectIdInstance() {
        FieldMetaData[] pkFields = meta.getPrimaryKeyFields();
        Object[] pks = new Object[pkFields.length];
        for (int i = 0; i < pkFields.length; i++)
            pks[i] = getValue(pkFields[i].getIndex(), o);
        return ApplicationIds.fromPKValues(pks, meta);
    }

    @Override
    public Object pcNewObjectIdInstance(Object oid) {
        return pcSubclassInstance.pcNewObjectIdInstance(oid);
    }

    @Override
    public void pcCopyKeyFieldsToObjectId(Object oid) {
        Object target;
        if (oid instanceof ObjectId)
            target = ((ObjectId) oid).getId();
        else
            target = oid;

        FieldMetaData[] pks = meta.getPrimaryKeyFields();
        for (FieldMetaData pk : pks) {
            Object val = getValue(pk.getIndex(), o);
            Field f = Reflection.findField(target.getClass(), pk.getName(),
                    true);
            Reflection.set(target, f, val);
        }
    }

    @Override
    public void pcCopyKeyFieldsToObjectId(FieldSupplier supplier, Object obj) {
        // This is only ever invoked against PCs in the PCRegistry. Such PCs
        // will always be enhanced types or subtypes of user types, and will
        // never be a ReflectingPersistenceCapable.
        throw new InternalException();
    }

    @Override
    public void pcCopyKeyFieldsFromObjectId(FieldConsumer consumer,
        Object obj) {
        // This is only ever invoked against PCs in the PCRegistry. Such PCs
        // will always be enhanced types or subtypes of user types, and will
        // never be a ReflectingPersistenceCapable.
        throw new InternalException();
    }

    @Override
    public Object pcGetDetachedState() {
        // ##### we can implement this if a state field has been set
        return null;
    }

    @Override
    public void pcSetDetachedState(Object state) {
        // StateManagerImpl will invoke this with null during instance
        // initialization
        if (state != null)
            throw new UnsupportedOperationException();
        // ##### we can implement this if a state field has been set
    }

    public void pcSetSerializationUserVisible(boolean userVisible) {
        serializationUserVisible = userVisible;
    }

    public boolean pcIsSerializationUserVisible() {
        return serializationUserVisible;
    }

    @Override
    public Object getManagedInstance() {
        return o;
    }

    private Object getValue(int i, Object o) {
        FieldMetaData fmd = meta.getField(i);
        if (AccessCode.isProperty(fmd.getAccessType())) {
            Field field = Reflection.findField(meta.getDescribedType(),
                toFieldName(i), true);
            return Reflection.get(o, field);
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
        FieldMetaData fmd = meta.getField(i);
        if (AccessCode.isProperty(fmd.getAccessType())) {
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(meta.getDescribedType());
    }

    private void readObject(ObjectInputStream in)
        throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        Class type = (Class) in.readObject();
        pcSubclassInstance = PCRegistry.newInstance(type, null, false);
        ImplHelper.registerPersistenceCapable(this);
    }
}

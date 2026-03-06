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

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.InternalException;

/**
 * Implementation of {@link PersistenceCapable} for Java record types.
 * Records cannot be enhanced (they are final), so this class provides
 * the PersistenceCapable contract using an internal value array and
 * the record's canonical constructor for materialization.
 *
 * <p>JPA 3.2 allows records as embeddable classes. During loading,
 * field values are collected in an internal array. When the managed
 * instance is requested, the record is constructed via its canonical
 * constructor.</p>
 *
 * @since 4.0.0
 */
public class RecordPersistenceCapable
        implements PersistenceCapable, ManagedInstanceProvider {

    private final Class<?> recordClass;
    private final ClassMetaData meta;
    private final Constructor<?> canonicalConstructor;
    private Object[] fieldValues;
    private Object recordInstance;
    private StateManager sm;

    /**
     * Prototype constructor used for PCRegistry registration.
     */
    public RecordPersistenceCapable(final Class<?> recordClass,
                                    final ClassMetaData meta) {
        this.recordClass = recordClass;
        this.meta = meta;
        this.canonicalConstructor = findCanonicalConstructor(recordClass);
    }

    /**
     * Instance constructor used during loading.
     */
    private RecordPersistenceCapable(final Class<?> recordClass,
                                     final ClassMetaData meta,
                                     final Constructor<?> canonicalConstructor,
                                     final StateManager sm) {
        this.recordClass = recordClass;
        this.meta = meta;
        this.canonicalConstructor = canonicalConstructor;
        this.sm = sm;
        this.fieldValues = new Object[meta.getFields().length];
    }

    /**
     * Instance constructor wrapping an existing record (for persistence).
     */
    public RecordPersistenceCapable(final Class<?> recordClass,
                                    final ClassMetaData meta,
                                    final Object recordInstance) {
        this.recordClass = recordClass;
        this.meta = meta;
        this.canonicalConstructor = findCanonicalConstructor(recordClass);
        this.recordInstance = recordInstance;
    }

    private static Constructor<?> findCanonicalConstructor(
            final Class<?> recordClass) {
        final RecordComponent[] components = recordClass.getRecordComponents();
        final Class<?>[] types = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        try {
            final Constructor<?> ctor =
                    recordClass.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor;
        } catch (final NoSuchMethodException e) {
            throw new InternalException(
                    "Cannot find canonical constructor for record "
                    + recordClass.getName(), e);
        }
    }

    /**
     * Constructs the actual record instance from accumulated field values.
     * Maps field metadata indices to canonical constructor parameter positions
     * based on record component order.
     */
    public Object materialize() {
        if (recordInstance != null) {
            return recordInstance;
        }
        try {
            final RecordComponent[] components =
                    recordClass.getRecordComponents();
            final Object[] args = new Object[components.length];
            for (int c = 0; c < components.length; c++) {
                final String compName = components[c].getName();
                // Find the corresponding field metadata index
                final FieldMetaData[] fields = meta.getFields();
                for (int f = 0; f < fields.length; f++) {
                    if (fields[f].getName().equals(compName)) {
                        args[c] = fieldValues[f];
                        break;
                    }
                }
                // If no match found, use default value for the type
                if (args[c] == null && components[c].getType().isPrimitive()) {
                    args[c] = getDefaultPrimitiveValue(
                            components[c].getType());
                }
            }
            recordInstance = canonicalConstructor.newInstance(args);
            return recordInstance;
        } catch (final Exception e) {
            throw new InternalException(
                    "Failed to construct record " + recordClass.getName(), e);
        }
    }

    /**
     * Re-reads field values from the given StateManager and constructs
     * a fresh record instance. Used after attach/merge where field values
     * are stored in the SM rather than in this wrapper's fieldValues array.
     */
    public void rematerialize(
            final org.apache.openjpa.kernel.OpenJPAStateManager stateMgr) {
        try {
            final RecordComponent[] components =
                    recordClass.getRecordComponents();
            final Object[] args = new Object[components.length];
            final FieldMetaData[] fields = meta.getFields();
            for (int c = 0; c < components.length; c++) {
                final String compName = components[c].getName();
                for (int f = 0; f < fields.length; f++) {
                    if (fields[f].getName().equals(compName)) {
                        args[c] = stateMgr.fetch(f);
                        break;
                    }
                }
                if (args[c] == null
                        && components[c].getType().isPrimitive()) {
                    args[c] = getDefaultPrimitiveValue(
                            components[c].getType());
                }
            }
            recordInstance = canonicalConstructor.newInstance(args);
        } catch (final Exception e) {
            throw new InternalException(
                    "Failed to rematerialize record "
                    + recordClass.getName(), e);
        }
    }

    private Object getFieldValue(final int i) {
        if (recordInstance != null
                && recordClass.isInstance(recordInstance)) {
            try {
                final RecordComponent[] components =
                        recordClass.getRecordComponents();
                final String fieldName = meta.getField(i).getName();
                for (final RecordComponent comp : components) {
                    if (comp.getName().equals(fieldName)) {
                        return comp.getAccessor().invoke(recordInstance);
                    }
                }
            } catch (final Exception e) {
                throw new InternalException(
                        "Failed to read record component "
                        + meta.getField(i).getName(), e);
            }
        }
        return fieldValues != null ? fieldValues[i] : null;
    }

    private static Object getDefaultPrimitiveValue(final Class<?> type) {
        if (type == boolean.class) return Boolean.FALSE;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        return null;
    }

    // ---- PersistenceCapable implementation ----

    @Override
    public int pcGetEnhancementContractVersion() {
        return PCEnhancer.ENHANCER_VERSION;
    }

    @Override
    public Object pcGetGenericContext() {
        return sm == null ? null : sm.getGenericContext();
    }

    @Override
    public StateManager pcGetStateManager() {
        return sm;
    }

    @Override
    public void pcReplaceStateManager(final StateManager sm) {
        this.sm = sm;
    }

    @Override
    public void pcProvideField(final int i) {
        final Object value = getFieldValue(i);
        switch (meta.getField(i).getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                sm.providedBooleanField(this, i,
                        value == null ? false : (Boolean) value);
                break;
            case JavaTypes.BYTE:
                sm.providedByteField(this, i,
                        value == null ? 0 : (Byte) value);
                break;
            case JavaTypes.CHAR:
                sm.providedCharField(this, i,
                        value == null ? 0 : (Character) value);
                break;
            case JavaTypes.DOUBLE:
                sm.providedDoubleField(this, i,
                        value == null ? 0 : (Double) value);
                break;
            case JavaTypes.FLOAT:
                sm.providedFloatField(this, i,
                        value == null ? 0 : (Float) value);
                break;
            case JavaTypes.INT:
                sm.providedIntField(this, i,
                        value == null ? 0 : (Integer) value);
                break;
            case JavaTypes.LONG:
                sm.providedLongField(this, i,
                        value == null ? 0 : (Long) value);
                break;
            case JavaTypes.SHORT:
                sm.providedShortField(this, i,
                        value == null ? 0 : (Short) value);
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
    public void pcProvideFields(final int[] fieldIndices) {
        for (final int idx : fieldIndices) {
            pcProvideField(idx);
        }
    }

    @Override
    public void pcReplaceField(final int i) {
        switch (meta.getField(i).getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                fieldValues[i] = sm.replaceBooleanField(this, i);
                break;
            case JavaTypes.BYTE:
                fieldValues[i] = sm.replaceByteField(this, i);
                break;
            case JavaTypes.CHAR:
                fieldValues[i] = sm.replaceCharField(this, i);
                break;
            case JavaTypes.DOUBLE:
                fieldValues[i] = sm.replaceDoubleField(this, i);
                break;
            case JavaTypes.FLOAT:
                fieldValues[i] = sm.replaceFloatField(this, i);
                break;
            case JavaTypes.INT:
                fieldValues[i] = sm.replaceIntField(this, i);
                break;
            case JavaTypes.LONG:
                fieldValues[i] = sm.replaceLongField(this, i);
                break;
            case JavaTypes.SHORT:
                fieldValues[i] = sm.replaceShortField(this, i);
                break;
            case JavaTypes.STRING:
                fieldValues[i] = sm.replaceStringField(this, i);
                break;
            default:
                fieldValues[i] = sm.replaceObjectField(this, i);
                break;
        }
        // Invalidate cached record since a field changed
        recordInstance = null;
    }

    @Override
    public void pcReplaceFields(final int[] fieldIndices) {
        for (final int idx : fieldIndices) {
            pcReplaceField(idx);
        }
    }

    @Override
    public void pcCopyFields(final Object fromObject,
                             final int[] fieldIndices) {
        for (final int idx : fieldIndices) {
            if (fromObject instanceof RecordPersistenceCapable) {
                fieldValues[idx] =
                        ((RecordPersistenceCapable) fromObject)
                                .getFieldValue(idx);
            } else {
                fieldValues[idx] = readRecordComponent(fromObject, idx);
            }
        }
        recordInstance = null;
    }

    private Object readRecordComponent(final Object record, final int i) {
        try {
            final String fieldName = meta.getField(i).getName();
            for (final RecordComponent comp :
                    recordClass.getRecordComponents()) {
                if (comp.getName().equals(fieldName)) {
                    return comp.getAccessor().invoke(record);
                }
            }
        } catch (final Exception e) {
            throw new InternalException(
                    "Failed to read record component", e);
        }
        return null;
    }

    @Override
    public void pcDirty(final String fieldName) {
        if (sm != null) {
            sm.dirty(fieldName);
        }
    }

    @Override
    public Object pcFetchObjectId() {
        return sm == null ? null : sm.fetchObjectId();
    }

    @Override
    public Object pcGetVersion() {
        return sm == null ? null : sm.getVersion();
    }

    @Override
    public boolean pcIsDirty() {
        return sm != null && sm.isDirty();
    }

    @Override
    public boolean pcIsTransactional() {
        return sm != null && sm.isTransactional();
    }

    @Override
    public boolean pcIsPersistent() {
        return sm != null && sm.isPersistent();
    }

    @Override
    public boolean pcIsNew() {
        return sm != null && sm.isNew();
    }

    @Override
    public boolean pcIsDeleted() {
        return sm != null && sm.isDeleted();
    }

    @Override
    public Boolean pcIsDetached() {
        return sm != null ? sm.isDetached() : null;
    }

    @Override
    public PersistenceCapable pcNewInstance(final StateManager sm,
                                           final boolean clear) {
        return new RecordPersistenceCapable(
                recordClass, meta, canonicalConstructor, sm);
    }

    @Override
    public PersistenceCapable pcNewInstance(final StateManager sm,
                                           final Object oid,
                                           final boolean clear) {
        return new RecordPersistenceCapable(
                recordClass, meta, canonicalConstructor, sm);
    }

    @Override
    public Object pcNewObjectIdInstance() {
        return null; // records are embeddables, no identity
    }

    @Override
    public Object pcNewObjectIdInstance(final Object oid) {
        return null; // records are embeddables, no identity
    }

    @Override
    public void pcCopyKeyFieldsToObjectId(final Object oid) {
        // no-op for embeddables
    }

    @Override
    public void pcCopyKeyFieldsToObjectId(final FieldSupplier supplier,
                                          final Object obj) {
        // no-op for embeddables
    }

    @Override
    public void pcCopyKeyFieldsFromObjectId(final FieldConsumer consumer,
                                            final Object obj) {
        // no-op for embeddables
    }

    @Override
    public Object pcGetDetachedState() {
        return null;
    }

    @Override
    public void pcSetDetachedState(final Object state) {
        // no-op
    }

    // ---- ManagedInstanceProvider implementation ----

    @Override
    public Object getManagedInstance() {
        if (recordInstance != null) {
            return recordInstance;
        }
        // Return this as the managed instance proxy so the broker can
        // track the embedded state manager via getStateManager(obj).
        // The actual record is created via materialize() after all
        // fields have been loaded.
        return this;
    }

    /**
     * Registers the given record class with PCRegistry so that
     * the standard embedding machinery can work with it.
     */
    public static void registerRecordType(final Class<?> recordClass,
                                          final ClassMetaData meta) {
        if (PCRegistry.isRegistered(recordClass)) {
            return;
        }
        final FieldMetaData[] fields = meta.getFields();
        final String[] fieldNames = new String[fields.length];
        final Class<?>[] fieldTypes = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldNames[i] = fields[i].getName();
            fieldTypes[i] = fields[i].getDeclaredType();
        }
        final RecordPersistenceCapable prototype =
                new RecordPersistenceCapable(recordClass, meta);
        PCRegistry.register(recordClass, fieldNames, fieldTypes,
                new byte[fields.length], null, null, prototype);
    }
}

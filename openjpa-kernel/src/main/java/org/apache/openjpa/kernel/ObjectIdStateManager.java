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
package org.apache.openjpa.kernel;

import java.io.IOException;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.BitSet;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.ImplHelper;

/**
 * State manager used to access state of embedded object id primary key fields.
 *
 * @author Abe White
 */
public class ObjectIdStateManager
    implements OpenJPAStateManager {

    private static final Byte ZERO_BYTE = (byte) 0;
    private static final Character ZERO_CHAR = (char) 0;
    private static final Double ZERO_DOUBLE = (double) 0;
    private static final Float ZERO_FLOAT = (float) 0;
    private static final Short ZERO_SHORT = (short) 0;

    private Object _oid;
    private final OpenJPAStateManager _owner;
    private final ValueMetaData _vmd;

    /**
     * Constructor; supply embedded object id and its owner.
     *
     * @param owner may be null
     */
    public ObjectIdStateManager(Object oid, OpenJPAStateManager owner,
        ValueMetaData ownerVal) {
        _oid = oid;
        _owner = owner;
        _vmd = ownerVal;
    }

    @Override
    public Object getGenericContext() {
        return (_owner == null) ? null : _owner.getGenericContext();
    }

    @Override
    public Object getPCPrimaryKey(Object oid, int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StateManager replaceStateManager(StateManager sm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVersion() {
        return null;
    }

    @Override
    public void setVersion(Object version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isTransactional() {
        return false;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public boolean isDetached() {
        return true;
    }

    @Override
    public boolean isVersionUpdateRequired() {
        return false;
    }

    @Override
    public boolean isVersionCheckRequired() {
        return false;
    }

    @Override
    public void dirty(String field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object fetchObjectId() {
        return null;
    }

    @Override
    public void accessingField(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean serializing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean writeDetached(ObjectOutput out)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void proxyDetachedDeserialized(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingBooleanField(PersistenceCapable pc, int idx,
        boolean cur, boolean next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingCharField(PersistenceCapable pc, int idx, char cur,
        char next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingByteField(PersistenceCapable pc, int idx, byte cur,
        byte next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingShortField(PersistenceCapable pc, int idx, short cur,
        short next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingIntField(PersistenceCapable pc, int idx, int cur,
        int next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingLongField(PersistenceCapable pc, int idx, long cur,
        long next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingFloatField(PersistenceCapable pc, int idx, float cur,
        float next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingDoubleField(PersistenceCapable pc, int idx, double cur,
        double next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingStringField(PersistenceCapable pc, int idx, String cur,
        String next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void settingObjectField(PersistenceCapable pc, int idx, Object cur,
        Object next, int set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedBooleanField(PersistenceCapable pc, int idx,
        boolean cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedCharField(PersistenceCapable pc, int idx, char cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedByteField(PersistenceCapable pc, int idx, byte cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedShortField(PersistenceCapable pc, int idx, short cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedIntField(PersistenceCapable pc, int idx, int cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedLongField(PersistenceCapable pc, int idx, long cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedFloatField(PersistenceCapable pc, int idx, float cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedDoubleField(PersistenceCapable pc, int idx,
        double cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedStringField(PersistenceCapable pc, int idx,
        String cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedObjectField(PersistenceCapable pc, int idx,
        Object cur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replaceBooleanField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public char replaceCharField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte replaceByteField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short replaceShortField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int replaceIntField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long replaceLongField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float replaceFloatField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double replaceDoubleField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String replaceStringField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object replaceObjectField(PersistenceCapable pc, int idx) {
        throw new UnsupportedOperationException();
    }

    ///////////////////////////////////
    // OpenJPAStateManager implementation
    ///////////////////////////////////

    @Override
    public void initialize(Class forType, PCState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void load(FetchConfiguration fetch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getManagedInstance() {
        return _oid;
    }

    @Override
    public PersistenceCapable getPersistenceCapable() {
        return ImplHelper.toPersistenceCapable(_oid,
            _vmd.getRepository().getConfiguration());
    }

    @Override
    public ClassMetaData getMetaData() {
        return _vmd.getEmbeddedMetaData();
    }

    @Override
    public OpenJPAStateManager getOwner() {
        return _owner;
    }

    @Override
    public int getOwnerIndex() {
        return _vmd.getFieldMetaData().getIndex();
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

    @Override
    public boolean isFlushed() {
        return false;
    }

    @Override
    public boolean isFlushedDirty() {
        return false;
    }

    @Override
    public boolean isProvisional() {
        return false;
    }

    @Override
    public BitSet getLoaded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BitSet getDirty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BitSet getFlushed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BitSet getUnloaded(FetchConfiguration fetch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object newProxy(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object newFieldProxy(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefaultValue(int field) {
        Object val = getValue(field);
        if (val == null)
            return true;

        FieldMetaData fmd = getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                return Boolean.FALSE.equals(val);
            case JavaTypes.CHAR:
                return (Character) val == 0;
            case JavaTypes.BYTE:
            case JavaTypes.DOUBLE:
            case JavaTypes.FLOAT:
            case JavaTypes.INT:
            case JavaTypes.LONG:
            case JavaTypes.SHORT:
                return ((Number) val).intValue() == 0;
            case JavaTypes.STRING:
                return "".equals(val);
            default:
                return false;
        }
    }

    @Override
    public StoreContext getContext() {
        return (_owner == null) ? null : _owner.getContext();
    }

    @Override
    public PCState getPCState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObjectId() {
        return null;
    }

    @Override
    public void setObjectId(Object oid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean assignObjectId(boolean flush) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getId() {
        return null;
    }

    @Override
    public Object getLock() {
        return null;
    }

    @Override
    public void setLock(Object lock) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNextVersion(Object version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getImplData() {
        return null;
    }

    @Override
    public Object setImplData(Object data, boolean cacheable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImplDataCacheable() {
        return false;
    }

    @Override
    public Object getImplData(int field) {
        return null;
    }

    @Override
    public Object setImplData(int field, Object data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImplDataCacheable(int field) {
        return false;
    }

    @Override
    public Object getIntermediate(int field) {
        return null;
    }

    @Override
    public void setIntermediate(int field, Object data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removed(int field, Object removed, boolean key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean beforeRefresh(boolean all) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dirty(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeBoolean(int field, boolean extVal) {
        setValue(field, (extVal) ? Boolean.TRUE : Boolean.FALSE, true);
    }

    @Override
    public void storeByte(int field, byte extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeChar(int field, char extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeInt(int field, int extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeShort(int field, short extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeLong(int field, long extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeFloat(int field, float extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeDouble(int field, double extVal) {
        setValue(field, extVal, true);
    }

    @Override
    public void storeString(int field, String extVal) {
        setValue(field, extVal, extVal != null);
    }

    @Override
    public void storeObject(int field, Object extVal) {
        setValue(field, extVal, extVal != null);
    }

    @Override
    public void store(int field, Object extVal) {
        boolean forceInst = true;
        if (extVal == null) {
            extVal = getDefaultValue(field);
            forceInst = false;
        }
        setValue(field, extVal, forceInst);
    }

    @Override
    public void storeBooleanField(int field, boolean extVal) {
        storeBoolean(field, extVal);
    }

    @Override
    public void storeByteField(int field, byte extVal) {
        storeByte(field, extVal);
    }

    @Override
    public void storeCharField(int field, char extVal) {
        storeChar(field, extVal);
    }

    @Override
    public void storeIntField(int field, int extVal) {
        storeInt(field, extVal);
    }

    @Override
    public void storeShortField(int field, short extVal) {
        storeShort(field, extVal);
    }

    @Override
    public void storeLongField(int field, long extVal) {
        storeLong(field, extVal);
    }

    @Override
    public void storeFloatField(int field, float extVal) {
        storeFloat(field, extVal);
    }

    @Override
    public void storeDoubleField(int field, double extVal) {
        storeDouble(field, extVal);
    }

    @Override
    public void storeStringField(int field, String extVal) {
        storeString(field, extVal);
    }

    @Override
    public void storeObjectField(int field, Object extVal) {
        storeObject(field, extVal);
    }

    @Override
    public void storeField(int field, Object value) {
        store(field, value);
    }

    @Override
    public boolean fetchBoolean(int field) {
        return (Boolean) getValue(field);
    }

    @Override
    public byte fetchByte(int field) {
        return ((Number) getValue(field)).byteValue();
    }

    @Override
    public char fetchChar(int field) {
        return (Character) getValue(field);
    }

    @Override
    public short fetchShort(int field) {
        return ((Number) getValue(field)).shortValue();
    }

    @Override
    public int fetchInt(int field) {
        return ((Number) getValue(field)).intValue();
    }

    @Override
    public long fetchLong(int field) {
        return ((Number) getValue(field)).longValue();
    }

    @Override
    public float fetchFloat(int field) {
        return ((Number) getValue(field)).floatValue();
    }

    @Override
    public double fetchDouble(int field) {
        return ((Number) getValue(field)).doubleValue();
    }

    @Override
    public String fetchString(int field) {
        return (String) getValue(field);
    }

    @Override
    public Object fetchObject(int field) {
        return getValue(field);
    }

    @Override
    public Object fetch(int field) {
        Object ret = getValue(field);
        if (ret == null)
            ret = getDefaultValue(field);
        return ret;
    }

    @Override
    public boolean fetchBooleanField(int field) {
        return fetchBoolean(field);
    }

    @Override
    public byte fetchByteField(int field) {
        return fetchByte(field);
    }

    @Override
    public char fetchCharField(int field) {
        return fetchChar(field);
    }

    @Override
    public short fetchShortField(int field) {
        return fetchShort(field);
    }

    @Override
    public int fetchIntField(int field) {
        return fetchInt(field);
    }

    @Override
    public long fetchLongField(int field) {
        return fetchLong(field);
    }

    @Override
    public float fetchFloatField(int field) {
        return fetchFloat(field);
    }

    @Override
    public double fetchDoubleField(int field) {
        return fetchDouble(field);
    }

    @Override
    public String fetchStringField(int field) {
        return fetchString(field);
    }

    @Override
    public Object fetchObjectField(int field) {
        return fetch(field);
    }

    @Override
    public Object fetchField(int field, boolean transitions) {
        return fetch(field);
    }

    @Override
    public Object fetchInitialField(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRemote(int field, Object value) {
        store(field, value);
    }

    public void lock() {
    }

    public void unlock() {
    }

    /**
     * Return the default value of the given field based on its type.
     */
    private Object getDefaultValue(int field) {
        FieldMetaData fmd = getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                return Boolean.FALSE;
            case JavaTypes.BYTE:
                return ZERO_BYTE;
            case JavaTypes.CHAR:
                return ZERO_CHAR;
            case JavaTypes.DOUBLE:
                return ZERO_DOUBLE;
            case JavaTypes.FLOAT:
                return ZERO_FLOAT;
            case JavaTypes.INT:
                return 0;
            case JavaTypes.LONG:
                return 0L;
            case JavaTypes.SHORT:
                return ZERO_SHORT;
            default:
                return null;
        }
    }

    /**
     * Return the value of the given field using reflection.
     * Relies on the fact that all oid fields/properties are made public
     * during enhancement.
     */
    private Object getValue(int field) {
        if (_oid == null)
            return null;

        FieldMetaData fmd = getMetaData().getField(field);
        Object val = null;
        if (fmd.getBackingMember() instanceof Field)
            val = Reflection.get(_oid, (Field) fmd.getBackingMember());
        else if (fmd.getBackingMember() instanceof Method)
            val = Reflection.get(_oid, (Method) fmd.getBackingMember());
        else if (AccessCode.isField(fmd.getDefiningMetaData().getAccessType()))
            val = Reflection.get(_oid, Reflection.findField(_oid.getClass(),
                fmd.getName(), true));
        else
            val = Reflection.get(_oid, Reflection.findGetter(_oid.getClass(),
            fmd.getName(), true));

        if (fmd.getValue().getEmbeddedMetaData() != null)
            return new ObjectIdStateManager(val, null, fmd);
        return val;
    }

    /**
     * Set the value of the given field using reflection.
     * Relies on the fact that all oid fields/properties are made public
     * during enhancement.
     */
    private void setValue(int field, Object val, boolean forceInst) {
        if (_oid == null && forceInst) {
            try {
                _oid = J2DoPrivHelper.newInstance(getMetaData().getDescribedType());
            } catch (Exception e) {
                throw new GeneralException(e);
            }
        } else if (_oid == null)
            return;

        FieldMetaData fmd = getMetaData().getField(field);
        if (fmd.getBackingMember() instanceof Field)
            Reflection.set(_oid, (Field) fmd.getBackingMember(), val);
        else if (AccessCode.isField(fmd.getDefiningMetaData().getAccessType()))
            Reflection.set(_oid, Reflection.findField(_oid.getClass(),
                fmd.getName(), true), val);
        else
            Reflection.set(_oid, Reflection.findSetter(_oid.getClass(),
                fmd.getName(), fmd.getDeclaredType(), true), val);
	}

    @Override
    public boolean isDelayed(int field) {
        return false;
    }

    @Override
    public void setDelayed(int field, boolean delay) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadDelayedField(int field) {
        throw new UnsupportedOperationException();
    }
}

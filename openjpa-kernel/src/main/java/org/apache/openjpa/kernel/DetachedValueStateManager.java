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

import java.io.ObjectOutput;
import java.util.BitSet;

import org.apache.openjpa.enhance.FieldManager;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.UnsupportedException;

/**
 * Implementation of {@link OpenJPAStateManager} designed to retrieve
 * values from a detached instance, including when managed by a
 * {@link DetachedStateManager}.
 */
public class DetachedValueStateManager
    extends TransferFieldManager
    implements OpenJPAStateManager {

    private static final Localizer _loc = Localizer.forPackage
        (DetachedValueStateManager.class);

    private PersistenceCapable _pc;
    private StoreContext _ctx;
    private ClassMetaData _meta;

    public DetachedValueStateManager(Object pc, StoreContext ctx) {
        this(ImplHelper.toPersistenceCapable(pc, ctx.getConfiguration()),
            ctx.getConfiguration().getMetaDataRepositoryInstance()
                .getMetaData(ImplHelper.getManagedInstance(pc).getClass(),
            ctx.getClassLoader(), true), ctx);
    }

    public DetachedValueStateManager(PersistenceCapable pc, ClassMetaData meta,
        StoreContext ctx) {
        _pc = ImplHelper.toPersistenceCapable(pc, ctx.getConfiguration());
        _meta = meta;
        _ctx = ctx;
    }

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
        return _pc;
    }

    @Override
    public PersistenceCapable getPersistenceCapable() {
        return _pc;
    }

    @Override
    public ClassMetaData getMetaData() {
        return _meta;
    }

    @Override
    public OpenJPAStateManager getOwner() {
        return null;
    }

    @Override
    public int getOwnerIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmbedded() {
        return false;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public StoreContext getContext() {
        return _ctx;
    }

    @Override
    public PCState getPCState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getId() {
        return getObjectId();
    }

    @Override
    public Object getObjectId() {
        throw new UnsupportedOperationException();
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
    public Object getLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLock(Object lock) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion(Object version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNextVersion(Object version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersionUpdateRequired() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersionCheckRequired() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getImplData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setImplData(Object data, boolean cacheable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImplDataCacheable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getImplData(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setImplData(int field, Object data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImplDataCacheable(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getIntermediate(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIntermediate(int field, Object value) {
        throw new UnsupportedOperationException();
    }

    void provideField(int field) {
        if (_pc.pcGetStateManager() != null)
            throw new InternalException(_loc.get("detach-val-mismatch", _pc));
        _pc.pcReplaceStateManager(this);
        _pc.pcProvideField(field);
        _pc.pcReplaceStateManager(null);
    }

    @Override
    public boolean fetchBoolean(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte fetchByte(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public char fetchChar(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double fetchDouble(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float fetchFloat(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fetchInt(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long fetchLong(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object fetchObject(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short fetchShort(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String fetchString(int field) {
        throw new UnsupportedOperationException();
    }

    public Object fetchFromDetachedSM(DetachedStateManager sm, int field) {
        sm.lock();
        sm.provideField(field);
        Object val = fetchField(sm, field);
        sm.clear();
        sm.unlock();
        return val;
    }

    @Override
    public Object fetch(int field) {
        StateManager sm = _pc.pcGetStateManager();
        if (sm != null) {
            if (sm instanceof DetachedStateManager)
                return fetchFromDetachedSM((DetachedStateManager) sm, field);
            if (_ctx.getAllowReferenceToSiblingContext() && sm instanceof StateManagerImpl) {
                return ((StateManagerImpl) sm).fetch(field);
            }
            throw new UnsupportedException(_loc.get("detach-val-badsm", _pc));
        }
        provideField(field);
        Object val = fetchField(field, false);
        clear();
        return _meta.getField(field).getExternalValue(val, _ctx.getBroker());
    }

    @Override
    public Object fetchField(int field, boolean transitions) {
        if (transitions)
            throw new IllegalArgumentException();
        return fetchField(this, field);
    }

    private Object fetchField(FieldManager fm, int field) {
        FieldMetaData fmd = _meta.getField(field);
        if (fmd == null)
            throw new InternalException();

        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.STRING:
                return fm.fetchStringField(field);
            case JavaTypes.OBJECT:
                return fm.fetchObjectField(field);
            case JavaTypes.BOOLEAN:
                return (fm.fetchBooleanField(field)) ? Boolean.TRUE
                    : Boolean.FALSE;
            case JavaTypes.BYTE:
                return Byte.valueOf(fm.fetchByteField(field));
            case JavaTypes.CHAR:
                return Character.valueOf(fm.fetchCharField(field));
            case JavaTypes.DOUBLE:
                return Double.valueOf(fm.fetchDoubleField(field));
            case JavaTypes.FLOAT:
                return Float.valueOf(fm.fetchFloatField(field));
            case JavaTypes.INT:
                return fm.fetchIntField(field);
            case JavaTypes.LONG:
                return fm.fetchLongField(field);
            case JavaTypes.SHORT:
                return Short.valueOf(fm.fetchShortField(field));
            default:
                return fm.fetchObjectField(field);
        }
    }

    @Override
    public Object fetchInitialField(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeBoolean(int field, boolean externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeByte(int field, byte externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeChar(int field, char externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeDouble(int field, double externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeFloat(int field, float externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeInt(int field, int externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeLong(int field, long externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeObject(int field, Object externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeShort(int field, short externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeString(int field, String externalVal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(int field, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeField(int field, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dirty(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removed(int field, Object removed, boolean key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean beforeRefresh(boolean refreshAll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRemote(int field, Object value) {
        throw new UnsupportedOperationException();
    }

    ///////////////////////////////
    // StateManager implementation
    ///////////////////////////////

    @Override
    public Object getGenericContext() {
        return _ctx;
    }

    @Override
    public Object getPCPrimaryKey(Object oid, int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StateManager replaceStateManager(StateManager sm) {
        return sm;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public boolean isTransactional() {
        return false;
    }

    @Override
    public boolean isPersistent() {
        return true;
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
    public void dirty(String field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object fetchObjectId() {
        return getObjectId();
    }

    @Override
    public boolean serializing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean writeDetached(ObjectOutput out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void proxyDetachedDeserialized(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accessingField(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void providedBooleanField(PersistenceCapable pc, int idx,
        boolean cur) {
        longval = cur ? 1 : 0;
    }

    @Override
    public void providedCharField(PersistenceCapable pc, int idx, char cur) {
        longval = cur;
    }

    @Override
    public void providedByteField(PersistenceCapable pc, int idx, byte cur) {
        longval = cur;
    }

    @Override
    public void providedShortField(PersistenceCapable pc, int idx, short cur) {
        longval = cur;
    }

    @Override
    public void providedIntField(PersistenceCapable pc, int idx, int cur) {
        longval = cur;
    }

    @Override
    public void providedLongField(PersistenceCapable pc, int idx, long cur) {
        longval = cur;
    }

    @Override
    public void providedFloatField(PersistenceCapable pc, int idx, float cur) {
        dblval = cur;
    }

    @Override
    public void providedDoubleField(PersistenceCapable pc, int idx,
        double cur) {
        dblval = cur;
    }

    @Override
    public void providedStringField(PersistenceCapable pc, int idx,
        String cur) {
        objval = cur;
    }

    @Override
    public void providedObjectField(PersistenceCapable pc, int idx,
        Object cur) {
        objval = cur;
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


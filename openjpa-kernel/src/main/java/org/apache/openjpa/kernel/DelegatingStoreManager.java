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

import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;

/**
 * Base class for store manager decorators that delegate to another
 * store manager for some operations.
 *
 * @author Abe White
 */
public abstract class DelegatingStoreManager
    implements StoreManager {

    private final StoreManager _store;
    private final DelegatingStoreManager _del;

    /**
     * Constructor. Supply delegate.
     */
    public DelegatingStoreManager(StoreManager store) {
        _store = store;
        if (store instanceof DelegatingStoreManager)
            _del = (DelegatingStoreManager) _store;
        else
            _del = null;
    }

    /**
     * Return the wrapped store manager.
     */
    public StoreManager getDelegate() {
        return _store;
    }

    /**
     * Return the base underlying native store manager.
     */
    public StoreManager getInnermostDelegate() {
        return (_del == null) ? _store : _del.getInnermostDelegate();
    }

    @Override
    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingStoreManager)
            other = ((DelegatingStoreManager) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    @Override
    public void setContext(StoreContext ctx) {
        _store.setContext(ctx);
    }

    @Override
    public void beginOptimistic() {
        _store.beginOptimistic();
    }

    @Override
    public void rollbackOptimistic() {
        _store.rollbackOptimistic();
    }

    @Override
    public void begin() {
        _store.begin();
    }

    @Override
    public void commit() {
        _store.commit();
    }

    @Override
    public void rollback() {
        _store.rollback();
    }

    @Override
    public boolean exists(OpenJPAStateManager sm, Object context) {
        return _store.exists(sm, context);
    }

    @Override
    public boolean syncVersion(OpenJPAStateManager sm, Object context) {
        return _store.syncVersion(sm, context);
    }

    @Override
    public boolean initialize(OpenJPAStateManager sm, PCState state,
        FetchConfiguration fetch, Object context) {
        return _store.initialize(sm, state, fetch, context);
    }

    @Override
    public boolean load(OpenJPAStateManager sm, BitSet fields,
        FetchConfiguration fetch, int lockLevel, Object context) {
        return _store.load(sm, fields, fetch, lockLevel, context);
    }

    @Override
    public Collection<Object> loadAll(Collection<OpenJPAStateManager> sms, PCState state, int load,
        FetchConfiguration fetch, Object context) {
        return _store.loadAll(sms, state, load, fetch, context);
    }

    @Override
    public void beforeStateChange(OpenJPAStateManager sm, PCState fromState,
        PCState toState) {
        _store.beforeStateChange(sm, fromState, toState);
    }

    @Override
    public Collection<Exception> flush(Collection<OpenJPAStateManager> sms) {
        return _store.flush(sms);
    }

    @Override
    public boolean assignObjectId(OpenJPAStateManager sm, boolean preFlush) {
        return _store.assignObjectId(sm, preFlush);
    }

    @Override
    public boolean assignField(OpenJPAStateManager sm, int field,
        boolean preFlush) {
        return _store.assignField(sm, field, preFlush);
    }

    @Override
    public Class<?> getManagedType(Object oid) {
        return _store.getManagedType(oid);
    }

    @Override
    public Class<?> getDataStoreIdType(ClassMetaData meta) {
        return _store.getDataStoreIdType(meta);
    }

    @Override
    public Object copyDataStoreId(Object oid, ClassMetaData meta) {
        return _store.copyDataStoreId(oid, meta);
    }

    @Override
    public Object newDataStoreId(Object oidVal, ClassMetaData meta) {
        return _store.newDataStoreId(oidVal, meta);
    }

    @Override
    public Object getClientConnection() {
        return _store.getClientConnection();
    }

    @Override
    public void retainConnection() {
        _store.retainConnection();
    }

    @Override
    public void releaseConnection() {
        _store.releaseConnection();
    }

    @Override
    public ResultObjectProvider executeExtent(ClassMetaData meta,
        boolean subclasses, FetchConfiguration fetch) {
        return _store.executeExtent(meta, subclasses, fetch);
    }

    @Override
    public StoreQuery newQuery(String language) {
        return _store.newQuery(language);
    }

    @Override
    public FetchConfiguration newFetchConfiguration() {
        return _store.newFetchConfiguration();
    }

    @Override
    public void close() {
        _store.close();
    }

    @Override
    public int compareVersion(OpenJPAStateManager state, Object v1, Object v2) {
        return _store.compareVersion(state, v1, v2);
    }

    @Override
    public Seq getDataStoreIdSequence(ClassMetaData forClass) {
        return _store.getDataStoreIdSequence(forClass);
    }

    @Override
    public Seq getValueSequence(FieldMetaData fmd) {
        return _store.getValueSequence(fmd);
    }

    @Override
    public boolean cancelAll() {
        return _store.cancelAll();
	}

    @Override
    public boolean isCached(List<Object> oids, BitSet edata) {
        return _store.isCached(oids, edata);
    }

}

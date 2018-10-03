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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.RuntimeExceptionTranslator;

///////////////////////////////////////////////////////////////
// NOTE: when adding a public API method, be sure to add it to
// JDO and JPA facades!
///////////////////////////////////////////////////////////////

/**
 * Delegating broker that can also perform exception translation
 * for use in facades.
 *
 * @since 0.4.0
 * @author Abe White
 */
public class DelegatingBroker
    implements Broker {

    private final Broker _broker;
    private final DelegatingBroker _del;
    private final RuntimeExceptionTranslator _trans;

    /**
     * Constructor; supply delegate.
     */
    public DelegatingBroker(Broker broker) {
        this(broker, null);
    }

    /**
     * Constructor; supply delegate and exception translator.
     */
    public DelegatingBroker(Broker broker, RuntimeExceptionTranslator trans) {
        _broker = broker;
        if (broker instanceof DelegatingBroker)
            _del = (DelegatingBroker) broker;
        else
            _del = null;
        _trans = trans;
    }

    /**
     * Return the direct delegate.
     */
    public Broker getDelegate() {
        return _broker;
    }

    /**
     * Return the native delegate.
     */
    public Broker getInnermostDelegate() {
        return (_del == null) ? _broker : _del.getInnermostDelegate();
    }

    @Override
    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingBroker)
            other = ((DelegatingBroker) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    /**
     * Translate the OpenJPA exception.
     */
    protected RuntimeException translate(RuntimeException re) {
        return (_trans == null) ? re : _trans.translate(re);
    }

    /**
     * Translate the exception with the failed object.
     *
     * @param re exception raised by the delegate.
     * @param failed the context that failed.
     *
     * @return the translated exception. If the given input exception had not set
     * the failed instance, then sets the given instance as the failed context.
     */
    protected RuntimeException translate(RuntimeException re, Object failed) {
        if (re instanceof OpenJPAException) {
            Object o = ((OpenJPAException) re).getFailedObject();
            if (o == null || "null".equals(o)) {
                ((OpenJPAException) re).setFailedObject(Exceptions.toString(failed));
            }
        }
        return (_trans == null) ? re : _trans.translate(re);
    }

    @Override
    public Broker getBroker() {
        return this;
    }

    @Override
    public OpenJPAConfiguration getConfiguration() {
        try {
            return _broker.getConfiguration();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public FetchConfiguration getFetchConfiguration() {
        try {
            return _broker.getFetchConfiguration();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public FetchConfiguration pushFetchConfiguration() {
        try {
            return _broker.pushFetchConfiguration();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public FetchConfiguration pushFetchConfiguration(FetchConfiguration fc) {
        try {
            return _broker.pushFetchConfiguration(fc);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void popFetchConfiguration() {
        try {
            _broker.popFetchConfiguration();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        try {
            return _broker.getClassLoader();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public LockManager getLockManager() {
        try {
            return _broker.getLockManager();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public DelegatingStoreManager getStoreManager() {
        try {
            return _broker.getStoreManager();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public String getConnectionUserName() {
        try {
            return _broker.getConnectionUserName();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public String getConnectionPassword() {
        try {
            return _broker.getConnectionPassword();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Map<String,Object> getProperties() {
        try {
            return _broker.getProperties();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Set<String> getSupportedProperties() {
        try {
            return _broker.getSupportedProperties();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object find(Object oid, boolean validate, FindCallbacks call) {
        try {
            return _broker.find(oid, validate, call);
        } catch (RuntimeException re) {
            throw translate(re, oid);
        }
    }

    @Override
    public Object[] findAll(Collection oids, boolean validate,
        FindCallbacks call) {
        try {
            return _broker.findAll(oids, validate, call);
        } catch (RuntimeException re) {
            throw translate(re, oids);
        }
    }

    @Override
    public Object findCached(Object oid, FindCallbacks call) {
        try {
            return _broker.findCached(oid, call);
        } catch (RuntimeException re) {
            throw translate(re, oid);
        }
    }

    @Override
    public Object find(Object oid, FetchConfiguration fetch, BitSet exclude,
        Object edata, int flags) {
        try {
            return _broker.find(oid, fetch, exclude, edata, flags);
        } catch (RuntimeException re) {
            throw translate(re, oid);
        }
    }

    @Override
    public Object[] findAll(Collection oids, FetchConfiguration fetch,
        BitSet exclude, Object edata, int flags) {
        try {
            return _broker.findAll(oids, fetch, exclude, edata, flags);
        } catch (RuntimeException re) {
            throw translate(re, oids);
        }
    }

    @Override
    public Iterator extentIterator(Class cls, boolean subs,
        FetchConfiguration fetch, boolean ignoreChanges) {
        try {
            return _broker.extentIterator(cls, subs, fetch, ignoreChanges);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void retrieve(Object obj, boolean fgOnly, OpCallbacks call) {
        try {
            _broker.retrieve(obj, fgOnly, call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void retrieveAll(Collection objs, boolean fgOnly, OpCallbacks call) {
        try {
            _broker.retrieveAll(objs, fgOnly, call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public OpenJPAStateManager embed(Object obj, Object id,
        OpenJPAStateManager owner, ValueMetaData ownerMeta) {
        try {
            return _broker.embed(obj, id, owner, ownerMeta);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Class getObjectIdType(Class cls) {
        try {
            return _broker.getObjectIdType(cls);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object newObjectId(Class cls, Object val) {
        try {
            return _broker.newObjectId(cls, val);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getManagedObjects() {
        try {
            return _broker.getManagedObjects();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getTransactionalObjects() {
        try {
            return _broker.getTransactionalObjects();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getPendingTransactionalObjects() {
        try {
            return _broker.getPendingTransactionalObjects();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getDirtyObjects() {
        try {
            return _broker.getDirtyObjects();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getOrderDirtyObjects() {
        try {
            return _broker.getOrderDirtyObjects();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setOrderDirtyObjects(boolean order) {
        try {
            _broker.setOrderDirtyObjects(order);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getPersistedTypes() {
        try {
            return _broker.getPersistedTypes();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getUpdatedTypes() {
        try {
            return _broker.getUpdatedTypes();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection getDeletedTypes() {
        try {
            return _broker.getDeletedTypes();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public OpenJPAStateManager getStateManager(Object obj) {
        try {
            return _broker.getStateManager(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getLockLevel(Object obj) {
        try {
            return _broker.getLockLevel(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object getVersion(Object obj) {
        try {
            return _broker.getVersion(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isDirty(Object obj) {
        try {
            return _broker.isDirty(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isTransactional(Object obj) {
        try {
            return _broker.isTransactional(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isPersistent(Object obj) {
        try {
            return _broker.isPersistent(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isNew(Object obj) {
        try {
            return _broker.isNew(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isDeleted(Object obj) {
        try {
            return _broker.isDeleted(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object getObjectId(Object obj) {
        try {
            return _broker.getObjectId(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isManaged() {
        try {
            return _broker.isManaged();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isActive() {
        try {
            return _broker.isActive();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isStoreActive() {
        try {
            return _broker.isStoreActive();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean hasConnection() {
        try {
            return _broker.hasConnection();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object getConnection() {
        try {
            return _broker.getConnection();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void lock() {
        try {
            _broker.lock();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void unlock() {
        try {
            _broker.unlock();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean beginOperation(boolean read) {
        try {
            return _broker.beginOperation(read);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean endOperation() {
        try {
            return _broker.endOperation();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setImplicitBehavior(OpCallbacks call,
        RuntimeExceptionTranslator ex) {
        try {
            _broker.setImplicitBehavior(call, ex);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public BrokerFactory getBrokerFactory() {
        try {
            return _broker.getBrokerFactory();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getConnectionRetainMode() {
        try {
            return _broker.getConnectionRetainMode();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public ManagedRuntime getManagedRuntime() {
        try {
            return _broker.getManagedRuntime();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public InverseManager getInverseManager() {
        try {
            return _broker.getInverseManager();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getMultithreaded() {
        try {
            return _broker.getMultithreaded();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setMultithreaded(boolean multi) {
        try {
            _broker.setMultithreaded(multi);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getIgnoreChanges() {
        try {
            return _broker.getIgnoreChanges();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setIgnoreChanges(boolean ignore) {
        try {
            _broker.setIgnoreChanges(ignore);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getNontransactionalRead() {
        try {
            return _broker.getNontransactionalRead();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setNontransactionalRead(boolean read) {
        try {
            _broker.setNontransactionalRead(read);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getNontransactionalWrite() {
        try {
            return _broker.getNontransactionalWrite();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setNontransactionalWrite(boolean write) {
        try {
            _broker.setNontransactionalWrite(write);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getRestoreState() {
        try {
            return _broker.getRestoreState();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setRestoreState(int restore) {
        try {
            _broker.setRestoreState(restore);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getOptimistic() {
        try {
            return _broker.getOptimistic();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setOptimistic(boolean opt) {
        try {
            _broker.setOptimistic(opt);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getRetainState() {
        try {
            return _broker.getRetainState();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setRetainState(boolean retain) {
        try {
            _broker.setRetainState(retain);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getAutoClear() {
        try {
            return _broker.getAutoClear();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setAutoClear(int clear) {
        try {
            _broker.setAutoClear(clear);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getAutoDetach() {
        try {
            return _broker.getAutoDetach();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setAutoDetach(int flags) {
        try {
            _broker.setAutoDetach(flags);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setAutoDetach(int flag, boolean on) {
        try {
            _broker.setAutoDetach(flag, on);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getDetachState() {
        try {
            return _broker.getDetachState();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setDetachState(int mode) {
        try {
            _broker.setDetachState(mode);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isDetachedNew() {
        try {
            return _broker.isDetachedNew();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setDetachedNew(boolean isNew) {
        try {
            _broker.setDetachedNew(isNew);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getSyncWithManagedTransactions() {
        try {
            return _broker.getSyncWithManagedTransactions();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setSyncWithManagedTransactions(boolean sync) {
        try {
            _broker.setSyncWithManagedTransactions(sync);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getEvictFromDataCache() {
        try {
            return _broker.getEvictFromDataCache();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setEvictFromDataCache(boolean evict) {
        try {
            _broker.setEvictFromDataCache(evict);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getPopulateDataCache() {
        try {
            return _broker.getPopulateDataCache();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setPopulateDataCache(boolean cache) {
        try {
            _broker.setPopulateDataCache(cache);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isTrackChangesByType() {
        try {
            return _broker.isTrackChangesByType();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setTrackChangesByType(boolean largeTransaction) {
        try {
            _broker.setTrackChangesByType(largeTransaction);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object putUserObject(Object key, Object val) {
        try {
            return _broker.putUserObject(key, val);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object getUserObject(Object key) {
        try {
            return _broker.getUserObject(key);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void addTransactionListener(Object listener) {
        try {
            _broker.addTransactionListener(listener);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void removeTransactionListener(Object listener) {
        try {
            _broker.removeTransactionListener(listener);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Collection<Object> getTransactionListeners() {
        try {
            return _broker.getTransactionListeners();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getTransactionListenerCallbackMode() {
        try {
            return _broker.getTransactionListenerCallbackMode();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setTransactionListenerCallbackMode(int mode) {
        try {
            _broker.setTransactionListenerCallbackMode(mode);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void addLifecycleListener(Object listener, Class[] classes) {
        try {
            _broker.addLifecycleListener(listener, classes);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void removeLifecycleListener(Object listener) {
        try {
            _broker.removeLifecycleListener(listener);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public int getLifecycleListenerCallbackMode() {
        try {
            return _broker.getLifecycleListenerCallbackMode();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setLifecycleListenerCallbackMode(int mode) {
        try {
            _broker.setLifecycleListenerCallbackMode(mode);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public LifecycleEventManager getLifecycleEventManager() {
        try {
            return _broker.getLifecycleEventManager();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void begin() {
        try {
            _broker.begin();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void commit() {
        try {
            _broker.commit();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void rollback() {
        try {
            _broker.rollback();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean syncWithManagedTransaction() {
        try {
            return _broker.syncWithManagedTransaction();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void commitAndResume() {
        try {
            _broker.commitAndResume();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void rollbackAndResume() {
        try {
            _broker.rollbackAndResume();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setRollbackOnly() {
        try {
            _broker.setRollbackOnly();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setRollbackOnly(Throwable cause) {
        try {
            _broker.setRollbackOnly(cause);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Throwable getRollbackCause() {
        try {
            return _broker.getRollbackCause();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            return _broker.getRollbackOnly();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void setSavepoint(String name) {
        try {
            _broker.setSavepoint(name);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void rollbackToSavepoint() {
        try {
            _broker.rollbackToSavepoint();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void rollbackToSavepoint(String name) {
        try {
            _broker.rollbackToSavepoint(name);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void releaseSavepoint() {
        try {
            _broker.releaseSavepoint();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void releaseSavepoint(String name) {
        try {
            _broker.releaseSavepoint(name);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void flush() {
        try {
            _broker.flush();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void preFlush() {
        try {
            _broker.preFlush();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void validateChanges() {
        try {
            _broker.validateChanges();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void beginStore() {
        try {
            _broker.beginStore();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void persist(Object obj, OpCallbacks call) {
        try {
            _broker.persist(obj, call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void persistAll(Collection objs, OpCallbacks call) {
        try {
            _broker.persistAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public OpenJPAStateManager persist(Object obj, Object id,
        OpCallbacks call) {
        try {
            return _broker.persist(obj, id, call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void delete(Object obj, OpCallbacks call) {
        try {
            _broker.delete(obj, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public void deleteAll(Collection objs, OpCallbacks call) {
        try {
            _broker.deleteAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void release(Object obj, OpCallbacks call) {
        try {
            _broker.release(obj, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public void releaseAll(Collection objs, OpCallbacks call) {
        try {
            _broker.releaseAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void refresh(Object obj, OpCallbacks call) {
        try {
            _broker.refresh(obj, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public void refreshAll(Collection objs, OpCallbacks call) {
        try {
            _broker.refreshAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void evict(Object obj, OpCallbacks call) {
        try {
            _broker.evict(obj, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public void evictAll(Collection objs, OpCallbacks call) {
        try {
            _broker.evictAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void evictAll(OpCallbacks call) {
        try {
            _broker.evictAll(call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void evictAll(Extent extent, OpCallbacks call) {
        try {
            _broker.evictAll(extent, call);
        } catch (RuntimeException re) {
            throw translate(re, extent.getElementType());
        }
    }

    @Override
    public Object detach(Object obj, OpCallbacks call) {
        try {
            return _broker.detach(obj, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public Object[] detachAll(Collection objs, OpCallbacks call) {
        try {
            return _broker.detachAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void detachAll(OpCallbacks call) {
        try {
            _broker.detachAll(call);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void detachAll(OpCallbacks call, boolean flush) {
        try {
            _broker.detachAll(call, flush);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object attach(Object obj, boolean copyNew, OpCallbacks call) {
        try {
            return _broker.attach(obj, copyNew, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public Object[] attachAll(Collection objs, boolean copyNew,
        OpCallbacks call) {
        try {
            return _broker.attachAll(objs, copyNew, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void transactional(Object pc, boolean updateVersion, OpCallbacks
        call) {
        try {
            _broker.transactional(pc, updateVersion, call);
        } catch (RuntimeException re) {
            throw translate(re, pc);
        }
    }

    @Override
    public void transactionalAll(Collection objs, boolean updateVersion,
        OpCallbacks call) {
        try {
            _broker.transactionalAll(objs, updateVersion, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void nontransactional(Object pc, OpCallbacks call) {
        try {
            _broker.nontransactional(pc, call);
        } catch (RuntimeException re) {
            throw translate(re, pc);
        }
    }

    @Override
    public void nontransactionalAll(Collection objs, OpCallbacks call) {
        try {
            _broker.nontransactionalAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public Extent newExtent(Class cls, boolean subs) {
        try {
            return _broker.newExtent(cls, subs);
        } catch (RuntimeException re) {
            throw translate(re, cls);
        }
    }

    @Override
    public Query newQuery(String language, Class cls, Object query) {
        try {
            return _broker.newQuery(language, cls, query);
        } catch (RuntimeException re) {
            throw translate(re, query);
        }
    }

    @Override
    public Query newQuery(String language, Object query) {
        try {
            return _broker.newQuery(language, query);
        } catch (RuntimeException re) {
            throw translate(re, query);
        }
    }

    @Override
    public Seq getIdentitySequence(ClassMetaData meta) {
        try {
            return _broker.getIdentitySequence(meta);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Seq getValueSequence(FieldMetaData fmd) {
        try {
            return _broker.getValueSequence(fmd);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void lock(Object obj, int level, int timeout, OpCallbacks call) {
        try {
            _broker.lock(obj, level, timeout, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public void lock(Object obj, OpCallbacks call) {
        try {
            _broker.lock(obj, call);
        } catch (RuntimeException re) {
            throw translate(re, obj);
        }
    }

    @Override
    public void lockAll(Collection objs, int level, int timeout,
        OpCallbacks call) {
        try {
            _broker.lockAll(objs, level, timeout, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public void lockAll(Collection objs, OpCallbacks call) {
        try {
            _broker.lockAll(objs, call);
        } catch (RuntimeException re) {
            throw translate(re, objs);
        }
    }

    @Override
    public boolean cancelAll() {
        try {
            return _broker.cancelAll();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void dirtyType(Class cls) {
        try {
            _broker.dirtyType(cls);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void close() {
        try {
            _broker.close();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isClosed() {
        try {
            return _broker.isClosed();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isCloseInvoked() {
        try {
            return _broker.isCloseInvoked();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void assertOpen() {
        try {
            _broker.assertOpen();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void assertActiveTransaction() {
        try {
            _broker.assertActiveTransaction();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void assertNontransactionalRead() {
        try {
            _broker.assertNontransactionalRead();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void assertWriteOperation() {
        try {
            _broker.assertWriteOperation();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    ///////////////////////////////////////////////
    // Implementation of Synchronization interface
    ///////////////////////////////////////////////

    @Override
    public void beforeCompletion() {
        try {
            _broker.beforeCompletion();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public void afterCompletion(int status) {
        try {
            _broker.afterCompletion(status);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public Object newInstance(Class cls) {
        try {
            return _broker.newInstance(cls);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean isDetached(Object obj) {
        try {
            return _broker.isDetached(obj);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    @Override
    public boolean getCachePreparedQuery() {
        return _broker.getCachePreparedQuery();
    }

    @Override
    public void setCachePreparedQuery(boolean flag) {
        _broker.setCachePreparedQuery(flag);
    }

    @Override
    public String getConnectionFactoryName() {
        return _broker.getConnectionFactoryName();
    }

    @Override
    public void setConnectionFactoryName(String connectionFactoryName) {
        _broker.setConnectionFactoryName(connectionFactoryName);
    }

    @Override
    public String getConnectionFactory2Name() {
        return _broker.getConnectionFactory2Name();
    }

    @Override
    public void setConnectionFactory2Name(String connectionFactory2Name) {
        _broker.setConnectionFactory2Name(connectionFactory2Name);
    }

    @Override
    public Object getConnectionFactory() {
        return _broker.getConnectionFactory();
    }

    @Override
    public Object getConnectionFactory2() {
        return _broker.getConnectionFactory2();
    }

    @Override
    public boolean isCached(List<Object> oid) {
        return _broker.isCached(oid);
    }

    @Override
    public boolean getAllowReferenceToSiblingContext() {
        return _broker.getAllowReferenceToSiblingContext();
    }

    @Override
    public void setAllowReferenceToSiblingContext(boolean allow) {
        _broker.setAllowReferenceToSiblingContext(allow);
    }

    @Override
    public boolean getPostLoadOnMerge() {
        return _broker.getPostLoadOnMerge();
    }

    @Override
    public void setPostLoadOnMerge(boolean allow) {
        _broker.setPostLoadOnMerge(allow);
    }


}

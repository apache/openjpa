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
package org.apache.openjpa.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.DelegatingBroker;
import org.apache.openjpa.kernel.FindCallbacks;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.OpCallbacks;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.UserException;

/**
 * Implementation of {@link EntityManager} interface.
 *
 * @author Patrick Linskey
 * @author Abe White
 * @nojavadoc
 */
public class EntityManagerImpl
    implements OpenJPAEntityManagerSPI, Externalizable,
    FindCallbacks, OpCallbacks, Closeable, OpenJPAEntityTransaction {

    private static final Localizer _loc = Localizer.forPackage
        (EntityManagerImpl.class);
    private static final Object[] EMPTY_OBJECTS = new Object[0];

    private DelegatingBroker _broker;
    private EntityManagerFactoryImpl _emf;
    private Map<FetchConfiguration,FetchPlan> _plans =
        new IdentityHashMap<FetchConfiguration,FetchPlan>(1);

    private RuntimeExceptionTranslator ret =
        PersistenceExceptions.getRollbackTranslator(this);

    public EntityManagerImpl() {
        // for Externalizable
    }

    /**
     * Constructor; supply factory and delegate.
     */
    public EntityManagerImpl(EntityManagerFactoryImpl factory,
        Broker broker) {
        initialize(factory, broker);
    }

    private void initialize(EntityManagerFactoryImpl factory, Broker broker) {
        _emf = factory;
        _broker = new DelegatingBroker(broker, ret);
        _broker.setImplicitBehavior(this, ret);
    }

    /**
     * Broker delegate.
     */
    public Broker getBroker() {
        return _broker.getDelegate();
    }

    public OpenJPAEntityManagerFactory getEntityManagerFactory() {
        return _emf;
    }

    public OpenJPAConfiguration getConfiguration() {
        return _broker.getConfiguration();
    }

    public FetchPlan getFetchPlan() {
        assertNotCloseInvoked();
        _broker.lock();
        try {
            FetchConfiguration fc = _broker.getFetchConfiguration();
            FetchPlan fp = _plans.get(fc);
            if (fp == null) {
                fp = _emf.toFetchPlan(_broker, fc);
                _plans.put(fc, fp);
            }
            return fp;
        } finally {
            _broker.unlock();
        }
    }

    public FetchPlan pushFetchPlan() {
        assertNotCloseInvoked();
        _broker.lock();
        try {
            _broker.pushFetchConfiguration();
            return getFetchPlan();
        } finally {
            _broker.unlock();
        }
    }

    public void popFetchPlan() {
        assertNotCloseInvoked();
        _broker.lock();
        try {
            _broker.popFetchConfiguration();
        } finally {
            _broker.unlock();
        }
    }

    public ConnectionRetainMode getConnectionRetainMode() {
        return ConnectionRetainMode.fromKernelConstant(
            _broker.getConnectionRetainMode());
    }

    public boolean isTransactionManaged() {
        return _broker.isManaged();
    }

    public boolean isManaged() {
        return _broker.isManaged();
    }

    public ManagedRuntime getManagedRuntime() {
        return _broker.getManagedRuntime();
    }

    public boolean getSyncWithManagedTransactions() {
        return _broker.getSyncWithManagedTransactions();
    }

    public void setSyncWithManagedTransactions(boolean sync) {
        assertNotCloseInvoked();
        _broker.setSyncWithManagedTransactions(sync);
    }

    public ClassLoader getClassLoader() {
        return _broker.getClassLoader();
    }

    public String getConnectionUserName() {
        return _broker.getConnectionUserName();
    }

    public String getConnectionPassword() {
        return _broker.getConnectionPassword();
    }

    public boolean getMultithreaded() {
        return _broker.getMultithreaded();
    }

    public void setMultithreaded(boolean multithreaded) {
        assertNotCloseInvoked();
        _broker.setMultithreaded(multithreaded);
    }

    public boolean getIgnoreChanges() {
        return _broker.getIgnoreChanges();
    }

    public void setIgnoreChanges(boolean val) {
        assertNotCloseInvoked();
        _broker.setIgnoreChanges(val);
    }

    public boolean getNontransactionalRead() {
        return _broker.getNontransactionalRead();
    }

    public void setNontransactionalRead(boolean val) {
        assertNotCloseInvoked();
        _broker.setNontransactionalRead(val);
    }

    public boolean getNontransactionalWrite() {
        return _broker.getNontransactionalWrite();
    }

    public void setNontransactionalWrite(boolean val) {
        assertNotCloseInvoked();
        _broker.setNontransactionalWrite(val);
    }

    public boolean getOptimistic() {
        return _broker.getOptimistic();
    }

    public void setOptimistic(boolean val) {
        assertNotCloseInvoked();
        _broker.setOptimistic(val);
    }

    public RestoreStateType getRestoreState() {
        return RestoreStateType.fromKernelConstant(_broker.getRestoreState());
    }

    public void setRestoreState(RestoreStateType val) {
        assertNotCloseInvoked();
        _broker.setRestoreState(val.toKernelConstant());
    }

    public void setRestoreState(int restore) {
        assertNotCloseInvoked();
        _broker.setRestoreState(restore);
    }

    public boolean getRetainState() {
        return _broker.getRetainState();
    }

    public void setRetainState(boolean val) {
        assertNotCloseInvoked();
        _broker.setRetainState(val);
    }

    public AutoClearType getAutoClear() {
        return AutoClearType.fromKernelConstant(_broker.getAutoClear());
    }

    public void setAutoClear(AutoClearType val) {
        assertNotCloseInvoked();
        _broker.setAutoClear(val.toKernelConstant());
    }

    public void setAutoClear(int autoClear) {
        assertNotCloseInvoked();
        _broker.setAutoClear(autoClear);
    }

    public DetachStateType getDetachState() {
        return DetachStateType.fromKernelConstant(_broker.getDetachState());
    }

    public void setDetachState(DetachStateType type) {
        assertNotCloseInvoked();
        _broker.setDetachState(type.toKernelConstant());
    }

    public void setDetachState(int detach) {
        assertNotCloseInvoked();
        _broker.setDetachState(detach);
    }

    public EnumSet<AutoDetachType> getAutoDetach() {
        return AutoDetachType.toEnumSet(_broker.getAutoDetach());
    }

    public void setAutoDetach(AutoDetachType flag) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(AutoDetachType.fromEnumSet(EnumSet.of(flag)));
    }

    public void setAutoDetach(EnumSet<AutoDetachType> flags) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(AutoDetachType.fromEnumSet(flags));
    }

    public void setAutoDetach(int autoDetachFlags) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(autoDetachFlags);
    }

    public void setAutoDetach(AutoDetachType value, boolean on) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(AutoDetachType.fromEnumSet(EnumSet.of(value)),on);
    }

    public void setAutoDetach(int flag, boolean on) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(flag, on);
    }

    public boolean getEvictFromStoreCache() {
        return _broker.getEvictFromDataCache();
    }

    public void setEvictFromStoreCache(boolean evict) {
        assertNotCloseInvoked();
        _broker.setEvictFromDataCache(evict);
    }

    public boolean getPopulateStoreCache() {
        return _broker.getPopulateDataCache();
    }

    public void setPopulateStoreCache(boolean cache) {
        assertNotCloseInvoked();
        _broker.setPopulateDataCache(cache);
    }

    public boolean isTrackChangesByType() {
        return _broker.isTrackChangesByType();
    }

    public void setTrackChangesByType(boolean trackByType) {
        assertNotCloseInvoked();
        _broker.setTrackChangesByType(trackByType);
    }

    public boolean isLargeTransaction() {
        return isTrackChangesByType();
    }

    public void setLargeTransaction(boolean value) {
        setTrackChangesByType(value);
    }

    public Object getUserObject(Object key) {
        return _broker.getUserObject(key);
    }

    public Object putUserObject(Object key, Object val) {
        assertNotCloseInvoked();
        return _broker.putUserObject(key, val);
    }

    public void addTransactionListener(Object listener) {
        assertNotCloseInvoked();
        _broker.addTransactionListener(listener);
    }

    public void removeTransactionListener(Object listener) {
        assertNotCloseInvoked();
        _broker.removeTransactionListener(listener);
    }

    public EnumSet<CallbackMode> getTransactionListenerCallbackModes() {
        return CallbackMode.toEnumSet(
            _broker.getTransactionListenerCallbackMode());
    }

    public void setTransactionListenerCallbackMode(CallbackMode mode) {
        assertNotCloseInvoked();
        _broker.setTransactionListenerCallbackMode(
            CallbackMode.fromEnumSet(EnumSet.of(mode)));
    }

    public void setTransactionListenerCallbackMode(EnumSet<CallbackMode> modes){
        assertNotCloseInvoked();
        _broker.setTransactionListenerCallbackMode(
            CallbackMode.fromEnumSet(modes));
    }

    public int getTransactionListenerCallbackMode() {
        return _broker.getTransactionListenerCallbackMode();
    }

    public void setTransactionListenerCallbackMode(int callbackMode) {
        throw new UnsupportedOperationException();
    }

    public void addLifecycleListener(Object listener, Class... classes) {
        assertNotCloseInvoked();
        _broker.addLifecycleListener(listener, classes);
    }

    public void removeLifecycleListener(Object listener) {
        assertNotCloseInvoked();
        _broker.removeLifecycleListener(listener);
    }

    public EnumSet<CallbackMode> getLifecycleListenerCallbackModes() {
        return CallbackMode.toEnumSet(
            _broker.getLifecycleListenerCallbackMode());
    }

    public void setLifecycleListenerCallbackMode(CallbackMode mode) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(
            CallbackMode.fromEnumSet(EnumSet.of(mode)));
    }

    public void setLifecycleListenerCallbackMode(EnumSet<CallbackMode> modes) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(
            CallbackMode.fromEnumSet(modes));
    }

    public int getLifecycleListenerCallbackMode() {
        return _broker.getLifecycleListenerCallbackMode();
    }

    public void setLifecycleListenerCallbackMode(int callbackMode) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(callbackMode);
    }

    @SuppressWarnings("unchecked")
    public <T> T getReference(Class<T> cls, Object oid) {
        assertNotCloseInvoked();
        oid = _broker.newObjectId(cls, oid);
        return (T) _broker.find(oid, false, this);
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> cls, Object oid) {
        assertNotCloseInvoked();
        oid = _broker.newObjectId(cls, oid);
        return (T) _broker.find(oid, true, this);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] findAll(Class<T> cls, Object... oids) {
        if (oids.length == 0)
            return (T[]) Array.newInstance(cls, 0);
        Collection<T> ret = findAll(cls, Arrays.asList(oids));
        return ret.toArray((T[]) Array.newInstance(cls, ret.size()));
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> findAll(final Class<T> cls, Collection oids) {
        assertNotCloseInvoked();
        Object[] objs = _broker.findAll(oids, true, new FindCallbacks() {
            public Object processArgument(Object oid) {
                return _broker.newObjectId(cls, oid);
            }

            public Object processReturn(Object oid, OpenJPAStateManager sm) {
                return EntityManagerImpl.this.processReturn(oid, sm);
            }
        });
        return (Collection<T>) Arrays.asList(objs);
    }

    @SuppressWarnings("unchecked")
    public <T> T findCached(Class<T> cls, Object oid) {
        assertNotCloseInvoked();
        return (T) _broker.findCached(_broker.newObjectId(cls, oid), this);
    }

    public Class getObjectIdClass(Class cls) {
        assertNotCloseInvoked();
        if (cls == null)
            return null;
        return JPAFacadeHelper.fromOpenJPAObjectIdClass
                (_broker.getObjectIdType(cls));
    }

    public OpenJPAEntityTransaction getTransaction() {
        if (_broker.isManaged())
            throw new InvalidStateException(_loc.get("get-managed-trans"),
                null, null, false);
        return this;
    }

    public void joinTransaction() {
        assertNotCloseInvoked();
        if (!_broker.syncWithManagedTransaction())
            throw new TransactionRequiredException(_loc.get
                ("no-managed-trans"), null, null, false);
    }

    public void begin() {
        _broker.begin();
    }

    public void commit() {
        try {
            _broker.commit();
        } catch (RollbackException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // RollbackExceptions are special and aren't handled by the
            // normal exception translator, since the spec says they
            // should be thrown whenever the commit fails for any reason at
            // all, wheras the exception translator handles exceptions that
            // are caused for specific reasons
            throw new RollbackException(e);
        }
    }

    public void rollback() {
        _broker.rollback();
    }

    public void commitAndResume() {
        _broker.commitAndResume();
    }

    public void rollbackAndResume() {
        _broker.rollbackAndResume();
    }

    public Throwable getRollbackCause() {
        if (!isActive())
            throw new IllegalStateException(_loc.get("no-transaction")
                .getMessage());

        return _broker.getRollbackCause();
    }

    public boolean getRollbackOnly() {
        if (!isActive())
            throw new IllegalStateException(_loc.get("no-transaction")
                .getMessage());

        return _broker.getRollbackOnly();
    }

    public void setRollbackOnly() {
        _broker.setRollbackOnly();
    }

    public void setRollbackOnly(Throwable cause) {
        _broker.setRollbackOnly(cause);
    }

    public void setSavepoint(String name) {
        assertNotCloseInvoked();
        _broker.setSavepoint(name);
    }

    public void rollbackToSavepoint() {
        assertNotCloseInvoked();
        _broker.rollbackToSavepoint();
    }

    public void rollbackToSavepoint(String name) {
        assertNotCloseInvoked();
        _broker.rollbackToSavepoint(name);
    }

    public void releaseSavepoint() {
        assertNotCloseInvoked();
        _broker.releaseSavepoint();
    }

    public void releaseSavepoint(String name) {
        assertNotCloseInvoked();
        _broker.releaseSavepoint(name);
    }

    public void flush() {
        assertNotCloseInvoked();
        _broker.assertOpen();
        _broker.assertActiveTransaction();
        _broker.flush();
    }

    public void preFlush() {
        assertNotCloseInvoked();
        _broker.preFlush();
    }

    public void validateChanges() {
        assertNotCloseInvoked();
        _broker.validateChanges();
    }

    public boolean isActive() {
        return isOpen() && _broker.isActive();
    }

    public boolean isStoreActive() {
        return _broker.isStoreActive();
    }

    public void beginStore() {
        _broker.beginStore();
    }

    public boolean contains(Object entity) {
        assertNotCloseInvoked();
        if (entity == null)
            return false;
        OpenJPAStateManager sm = _broker.getStateManager(entity);
        if (sm == null
            && !ImplHelper.isManagedType(getConfiguration(), entity.getClass()))
            throw new ArgumentException(_loc.get("not-entity",
                entity.getClass()), null, null, true);
        return sm != null && !sm.isDeleted();
    }

    public boolean containsAll(Object... entities) {
        for (Object entity : entities)
            if (!contains(entity))
                return false;
        return true;
    }

    public boolean containsAll(Collection entities) {
        for (Object entity : entities)
            if (!contains(entity))
                return false;
        return true;
    }

    public void persist(Object entity) {
        assertNotCloseInvoked();
        _broker.persist(entity, this);
    }

    public void persistAll(Object... entities) {
        persistAll(Arrays.asList(entities));
    }

    public void persistAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.persistAll(entities, this);
    }

    public void remove(Object entity) {
        assertNotCloseInvoked();
        _broker.delete(entity, this);
    }

    public void removeAll(Object... entities) {
        removeAll(Arrays.asList(entities));
    }

    public void removeAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.deleteAll(entities, this);
    }

    public void release(Object entity) {
        assertNotCloseInvoked();
        _broker.release(entity, this);
    }

    public void releaseAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.releaseAll(entities, this);
    }

    public void releaseAll(Object... entities) {
        releaseAll(Arrays.asList(entities));
    }

    public void refresh(Object entity) {
        assertNotCloseInvoked();
        _broker.assertWriteOperation();
        _broker.refresh(entity, this);
    }

    public void refreshAll() {
        assertNotCloseInvoked();
        _broker.assertWriteOperation();
        _broker.refreshAll(_broker.getTransactionalObjects(), this);
    }

    public void refreshAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.assertWriteOperation();
        _broker.refreshAll(entities, this);
    }

    public void refreshAll(Object... entities) {
        refreshAll(Arrays.asList(entities));
    }

    public void retrieve(Object entity) {
        assertNotCloseInvoked();
        _broker.retrieve(entity, true, this);
    }

    public void retrieveAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.retrieveAll(entities, true, this);
    }

    public void retrieveAll(Object... entities) {
        retrieveAll(Arrays.asList(entities));
    }

    public void evict(Object entity) {
        assertNotCloseInvoked();
        _broker.evict(entity, this);
    }

    public void evictAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.evictAll(entities, this);
    }

    public void evictAll(Object... entities) {
        evictAll(Arrays.asList(entities));
    }

    public void evictAll() {
        assertNotCloseInvoked();
        _broker.evictAll(this);
    }

    public void evictAll(Class cls) {
        assertNotCloseInvoked();
        _broker.evictAll(_broker.newExtent(cls, true), this);
    }

    public void evictAll(Extent extent) {
        assertNotCloseInvoked();
        _broker.evictAll(((ExtentImpl) extent).getDelegate(), this);
    }

    @SuppressWarnings("unchecked")
    public <T> T detach(T entity) {
        assertNotCloseInvoked();
        return (T) _broker.detach(entity, this);
    }

    public Object[] detachAll(Object... entities) {
        assertNotCloseInvoked();
        return _broker.detachAll(Arrays.asList(entities), this);
    }

    public Collection detachAll(Collection entities) {
        assertNotCloseInvoked();
        return Arrays.asList(_broker.detachAll(entities, this));
    }

    @SuppressWarnings("unchecked")
    public <T> T merge(T entity) {
        assertNotCloseInvoked();
        return (T) _broker.attach(entity, true, this);
    }

    public Object[] mergeAll(Object... entities) {
        if (entities.length == 0)
            return EMPTY_OBJECTS;
        return mergeAll(Arrays.asList(entities)).toArray();
    }

    public Collection mergeAll(Collection entities) {
        assertNotCloseInvoked();
        return Arrays.asList(_broker.attachAll(entities, true, this));
    }

    public void transactional(Object entity, boolean updateVersion) {
        assertNotCloseInvoked();
        _broker.transactional(entity, updateVersion, this);
    }

    public void transactionalAll(Collection objs, boolean updateVersion) {
        assertNotCloseInvoked();
        _broker.transactionalAll(objs, updateVersion, this);
    }

    public void transactionalAll(Object[] objs, boolean updateVersion) {
        assertNotCloseInvoked();
        _broker.transactionalAll(Arrays.asList(objs), updateVersion, this);
    }

    public void nontransactional(Object entity) {
        assertNotCloseInvoked();
        _broker.nontransactional(entity, this);
    }

    public void nontransactionalAll(Collection objs) {
        assertNotCloseInvoked();
        _broker.nontransactionalAll(objs, this);
    }

    public void nontransactionalAll(Object[] objs) {
        assertNotCloseInvoked();
        _broker.nontransactionalAll(Arrays.asList(objs), this);
    }

    public Generator getNamedGenerator(String name) {
        assertNotCloseInvoked();
        try {
            SequenceMetaData meta = _broker.getConfiguration().
                getMetaDataRepositoryInstance().getSequenceMetaData(name,
                _broker.getClassLoader(), true);
            Seq seq = meta.getInstance(_broker.getClassLoader());
            return new GeneratorImpl(seq, name, _broker, null);
        } catch (RuntimeException re) {
            throw PersistenceExceptions.toPersistenceException(re);
        }
    }

    public Generator getIdGenerator(Class forClass) {
        assertNotCloseInvoked();
        try {
            ClassMetaData meta = _broker.getConfiguration().
                getMetaDataRepositoryInstance().getMetaData(forClass,
                _broker.getClassLoader(), true);
            Seq seq = _broker.getIdentitySequence(meta);
            return (seq == null) ? null : new GeneratorImpl(seq, null, _broker,
                meta);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    public Generator getFieldGenerator(Class forClass, String fieldName) {
        assertNotCloseInvoked();
        try {
            ClassMetaData meta = _broker.getConfiguration().
                getMetaDataRepositoryInstance().getMetaData(forClass,
                _broker.getClassLoader(), true);
            FieldMetaData fmd = meta.getField(fieldName);
            if (fmd == null)
                throw new ArgumentException(_loc.get("no-named-field",
                    forClass, fieldName), null, null, false);

            Seq seq = _broker.getValueSequence(fmd);
            return (seq == null) ? null : new GeneratorImpl(seq, null, _broker,
                meta);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    public <T> Extent<T> createExtent(Class<T> cls, boolean subclasses) {
        assertNotCloseInvoked();
        return new ExtentImpl<T>(this, _broker.newExtent(cls, subclasses));
    }

    public OpenJPAQuery createQuery(String query) {
        return createQuery(JPQLParser.LANG_JPQL, query);
    }

    public OpenJPAQuery createQuery(String language, String query) {
        assertNotCloseInvoked();
        return new QueryImpl(this, ret, _broker.newQuery(language, query));
    }

    public OpenJPAQuery createQuery(Query query) {
        if (query == null)
            return createQuery((String) null);
        assertNotCloseInvoked();
        org.apache.openjpa.kernel.Query q = ((QueryImpl) query).getDelegate();
        return new QueryImpl(this, ret, _broker.newQuery(q.getLanguage(),
            q));
    }

    public OpenJPAQuery createNamedQuery(String name) {
        assertNotCloseInvoked();
        _broker.assertOpen();
        try {
            QueryMetaData meta = _broker.getConfiguration().
                getMetaDataRepositoryInstance().getQueryMetaData(null, name,
                _broker.getClassLoader(), true);
            org.apache.openjpa.kernel.Query del =
                _broker.newQuery(meta.getLanguage(), null);
            meta.setInto(del);
            del.compile();

            OpenJPAQuery q = new QueryImpl(this, ret, del);
            String[] hints = meta.getHintKeys();
            Object[] values = meta.getHintValues();
            for (int i = 0; i < hints.length; i++)
                q.setHint(hints[i], values[i]);
            return q;
        } catch (RuntimeException re) {
            throw PersistenceExceptions.toPersistenceException(re);
        }
    }

    public OpenJPAQuery createNativeQuery(String query) {
        validateSQL(query);
        return createQuery(QueryLanguages.LANG_SQL, query);
    }

    public OpenJPAQuery createNativeQuery(String query, Class cls) {
        return createNativeQuery(query).setResultClass(cls);
    }

    public OpenJPAQuery createNativeQuery(String query, String mappingName) {
        assertNotCloseInvoked();
        validateSQL(query);
        org.apache.openjpa.kernel.Query kernelQuery = _broker.newQuery(
            QueryLanguages.LANG_SQL, query);
        kernelQuery.setResultMapping(null, mappingName);
        return new QueryImpl(this, ret, kernelQuery);
    }

    /**
     * Validate that the user provided SQL.
     */
    private static void validateSQL(String query) {
        if (StringUtils.trimToNull(query) == null)
            throw new ArgumentException(_loc.get("no-sql"), null, null, false);
    }

    public void setFlushMode(FlushModeType flushMode) {
        assertNotCloseInvoked();
        _broker.assertOpen();
        _broker.getFetchConfiguration().setFlushBeforeQueries
            (toFlushBeforeQueries(flushMode));
    }

    public FlushModeType getFlushMode() {
        assertNotCloseInvoked();
        _broker.assertOpen();
        return fromFlushBeforeQueries(_broker.getFetchConfiguration().
            getFlushBeforeQueries());
    }

    /**
     * Translate our internal flush constant to a flush mode enum value.
     */
    static FlushModeType fromFlushBeforeQueries(int flush) {
        switch (flush) {
            case QueryFlushModes.FLUSH_TRUE:
                return FlushModeType.AUTO;
            case QueryFlushModes.FLUSH_FALSE:
                return FlushModeType.COMMIT;
            default:
                return null;
        }
    }

    /**
     * Translate a flush mode enum value to our internal flush constant.
     */
    static int toFlushBeforeQueries(FlushModeType flushMode) {
        // choose default for null
        if (flushMode == null)
            return QueryFlushModes.FLUSH_WITH_CONNECTION;
        if (flushMode == FlushModeType.AUTO)
            return QueryFlushModes.FLUSH_TRUE;
        if (flushMode == FlushModeType.COMMIT)
            return QueryFlushModes.FLUSH_FALSE;
        throw new ArgumentException(flushMode.toString(), null, null, false);
    }

    public void clear() {
        assertNotCloseInvoked();
        _broker.detachAll(this, false);
    }

    public Object getDelegate() {
        _broker.assertOpen();
        assertNotCloseInvoked();
        return this;
    }

    public LockModeType getLockMode(Object entity) {
        assertNotCloseInvoked();
        return fromLockLevel(_broker.getLockLevel(entity));
    }

    public void lock(Object entity, LockModeType mode) {
        assertNotCloseInvoked();
        _broker.lock(entity, toLockLevel(mode), -1, this);
    }

    public void lock(Object entity) {
        assertNotCloseInvoked();
        _broker.lock(entity, this);
    }

    public void lock(Object entity, LockModeType mode, int timeout) {
        assertNotCloseInvoked();
        _broker.lock(entity, toLockLevel(mode), timeout, this);
    }

    public void lockAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.lockAll(entities, this);
    }

    public void lockAll(Collection entities, LockModeType mode, int timeout) {
        assertNotCloseInvoked();
        _broker.lockAll(entities, toLockLevel(mode), timeout, this);
    }

    public void lockAll(Object... entities) {
        lockAll(Arrays.asList(entities));
    }

    public void lockAll(Object[] entities, LockModeType mode, int timeout) {
        lockAll(Arrays.asList(entities), mode, timeout);
    }

    /**
     * Translate our internal lock level to a javax.persistence enum value.
     */
    static LockModeType fromLockLevel(int level) {
        if (level < LockLevels.LOCK_READ)
            return null;
        if (level < LockLevels.LOCK_WRITE)
            return LockModeType.READ;
        return LockModeType.WRITE;
    }

    /**
     * Translate the javax.persistence enum value to our internal lock level.
     */
    static int toLockLevel(LockModeType mode) {
        if (mode == null)
            return LockLevels.LOCK_NONE;
        if (mode == LockModeType.READ)
            return LockLevels.LOCK_READ;
        if (mode == LockModeType.WRITE)
            return LockLevels.LOCK_WRITE;
        throw new ArgumentException(mode.toString(), null, null, true);
    }

    public boolean cancelAll() {
        return _broker.cancelAll();
    }

    public Object getConnection() {
        return _broker.getConnection();
    }

    public Collection getManagedObjects() {
        return _broker.getManagedObjects();
    }

    public Collection getTransactionalObjects() {
        return _broker.getTransactionalObjects();
    }

    public Collection getPendingTransactionalObjects() {
        return _broker.getPendingTransactionalObjects();
    }

    public Collection getDirtyObjects() {
        return _broker.getDirtyObjects();
    }

    public boolean getOrderDirtyObjects() {
        return _broker.getOrderDirtyObjects();
    }

    public void setOrderDirtyObjects(boolean order) {
        assertNotCloseInvoked();
        _broker.setOrderDirtyObjects(order);
    }

    public void dirtyClass(Class cls) {
        assertNotCloseInvoked();
        _broker.dirtyType(cls);
    }

    @SuppressWarnings("unchecked")
    public Collection<Class> getPersistedClasses() {
        return (Collection<Class>) _broker.getPersistedTypes();
    }

    @SuppressWarnings("unchecked")
    public Collection<Class> getUpdatedClasses() {
        return (Collection<Class>) _broker.getUpdatedTypes();
    }

    @SuppressWarnings("unchecked")
    public Collection<Class> getRemovedClasses() {
        return (Collection<Class>) _broker.getDeletedTypes();
    }

    public <T> T createInstance(Class<T> cls) {
        assertNotCloseInvoked();
        return (T) _broker.newInstance(cls);
    }

    public void close() {
        assertNotCloseInvoked();
        _broker.close();
    }

    public boolean isOpen() {
        return !_broker.isCloseInvoked();
    }

    public void dirty(Object o, String field) {
        assertNotCloseInvoked();
        OpenJPAStateManager sm = _broker.getStateManager(o);
        try {
            if (sm != null)
                sm.dirty(field);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    public Object getObjectId(Object o) {
        assertNotCloseInvoked();
        return JPAFacadeHelper.fromOpenJPAObjectId(_broker.getObjectId(o));
    }

    public boolean isDirty(Object o) {
        assertNotCloseInvoked();
        return _broker.isDirty(o);
    }

    public boolean isTransactional(Object o) {
        assertNotCloseInvoked();
        return _broker.isTransactional(o);
    }

    public boolean isPersistent(Object o) {
        assertNotCloseInvoked();
        return _broker.isPersistent(o);
    }

    public boolean isNewlyPersistent(Object o) {
        assertNotCloseInvoked();
        return _broker.isNew(o);
    }

    public boolean isRemoved(Object o) {
        assertNotCloseInvoked();
        return _broker.isDeleted(o);
    }

    public boolean isDetached(Object entity) {
        assertNotCloseInvoked();
        return _broker.isDetached(entity);
    }

    public Object getVersion(Object o) {
        assertNotCloseInvoked();
        return _broker.getVersion(o);
    }

    /**
     * Throw appropriate exception if close has been invoked but the broker
     * is still open.  We test only for this because if the broker is already
     * closed, it will throw its own more informative exception when we 
     * delegate the pending operation to it.
     */
    void assertNotCloseInvoked() {
        if (!_broker.isClosed() && _broker.isCloseInvoked())
            throw new InvalidStateException(_loc.get("close-invoked"), null,
                null, true);
    }

    ////////////////////////////////
    // FindCallbacks implementation
    ////////////////////////////////

    public Object processArgument(Object arg) {
        return arg;
    }

    public Object processReturn(Object oid, OpenJPAStateManager sm) {
        return (sm == null || sm.isDeleted()) ? null : sm.getManagedInstance();
    }

    //////////////////////////////
    // OpCallbacks implementation
    //////////////////////////////

    public int processArgument(int op, Object obj, OpenJPAStateManager sm) {
        switch (op) {
            case OP_DELETE:
                // cascade through non-persistent non-detached instances
                if (sm == null && !_broker.isDetached(obj))
                    return ACT_CASCADE;
                if (sm != null && !sm.isDetached() && !sm.isPersistent())
                    return ACT_CASCADE;
                // ignore deleted instances
                if (sm != null && sm.isDeleted())
                    return ACT_NONE;
                break;
            case OP_ATTACH:
                // die on removed
                if (sm != null && sm.isDeleted())
                    throw new UserException(_loc.get("removed",
                        Exceptions.toString(obj))).setFailedObject(obj);
                // cascade through managed instances
                if (sm != null && !sm.isDetached())
                    return ACT_CASCADE;
                break;
            case OP_REFRESH:
                // die on unmanaged instances
                if (sm == null)
                    throw new UserException(_loc.get("not-managed",
                        Exceptions.toString(obj))).setFailedObject(obj);
                break;
        }
        return ACT_RUN | ACT_CASCADE;
    }

    public int hashCode() {
        return _broker.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof EntityManagerImpl))
            return false;
        return _broker.equals(((EntityManagerImpl) other)._broker);
    }

    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        try {
            ret = PersistenceExceptions.getRollbackTranslator(this);

            // this assumes that serialized Brokers are from something
            // that extends AbstractBrokerFactory.
            Object factoryKey = in.readObject();
            AbstractBrokerFactory factory =
                AbstractBrokerFactory.getPooledFactoryForKey(factoryKey);
            byte[] brokerBytes = (byte[]) in.readObject();
            ObjectInputStream innerIn = new BrokerBytesInputStream(brokerBytes,
                factory.getConfiguration());

            Broker broker = (Broker) innerIn.readObject();
            EntityManagerFactoryImpl emf = (EntityManagerFactoryImpl)
                JPAFacadeHelper.toEntityManagerFactory(
                    broker.getBrokerFactory());
            broker.putUserObject(JPAFacadeHelper.EM_KEY, this);
            initialize(emf, broker);
        } catch (RuntimeException re) {
            try {
                re = ret.translate(re);
            } catch (Exception e) {
                // ignore
            }
            throw re;
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            // this requires that only AbstractBrokerFactory-sourced
            // brokers can be serialized
            Object factoryKey = ((AbstractBrokerFactory) _broker
                .getBrokerFactory()).getPoolKey();
            out.writeObject(factoryKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream innerOut = new ObjectOutputStream(baos);
            innerOut.writeObject(_broker.getDelegate());
            innerOut.flush();
            out.writeObject(baos.toByteArray());
        } catch (RuntimeException re) {
            try {
                re = ret.translate(re);
            } catch (Exception e) {
                // ignore
            }
            throw re;
        }
    }

    private static class BrokerBytesInputStream extends ObjectInputStream {

        private OpenJPAConfiguration conf;

        BrokerBytesInputStream(byte[] bytes, OpenJPAConfiguration conf)
            throws IOException {
            super(new ByteArrayInputStream(bytes));
            if (conf == null)
                throw new IllegalArgumentException(
                    "Illegal null argument to ObjectInputStreamWithLoader");
            this.conf = conf;
        }

        /**
         * Make a primitive array class
         */
        private Class primitiveType(char type) {
            switch (type) {
                case 'B': return byte.class;
                case 'C': return char.class;
                case 'D': return double.class;
                case 'F': return float.class;
                case 'I': return int.class;
                case 'J': return long.class;
                case 'S': return short.class;
                case 'Z': return boolean.class;
                default: return null;
            }
        }

        protected Class resolveClass(ObjectStreamClass classDesc)
            throws IOException, ClassNotFoundException {

            String cname = classDesc.getName();
            if (cname.startsWith("[")) {
                // An array
                Class component;		// component class
                int dcount;			    // dimension
                for (dcount=1; cname.charAt(dcount)=='['; dcount++) ;
                if (cname.charAt(dcount) == 'L') {
                    component = lookupClass(cname.substring(dcount+1,
                        cname.length()-1));
                } else {
                    if (cname.length() != dcount+1) {
                        throw new ClassNotFoundException(cname);// malformed
                    }
                    component = primitiveType(cname.charAt(dcount));
                }
                int dim[] = new int[dcount];
                for (int i=0; i<dcount; i++) {
                    dim[i]=0;
                }
                return Array.newInstance(component, dim).getClass();
            } else {
                return lookupClass(cname);
            }
        }

        /**
         * If this is a generated subclass, look up the corresponding Class
         * object via metadata.
         */
        private Class lookupClass(String className)
            throws ClassNotFoundException {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (PCEnhancer.isPCSubclassName(className)) {
                    String superName = PCEnhancer.toManagedTypeName(className);
                    ClassMetaData[] metas = conf.getMetaDataRepositoryInstance()
                        .getMetaDatas();
                    for (int i = 0; i < metas.length; i++) {
                        if (superName.equals(
                            metas[i].getDescribedType().getName())) {
                            return PCRegistry.getPCType(
                                metas[i].getDescribedType());
                        }
                    }

                    // if it's not found, try to look for it anyways
                    return Class.forName(className);
                } else {
                    throw e;
                }
            }
        }
    }
}

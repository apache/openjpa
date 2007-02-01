/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.DelegatingBroker;
import org.apache.openjpa.kernel.FindCallbacks;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.OpCallbacks;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.jpql.JPQLParser;
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
    implements OpenJPAEntityManager, FindCallbacks, OpCallbacks {

    private static final Localizer _loc = Localizer.forPackage
        (EntityManagerImpl.class);

    private final DelegatingBroker _broker;
    private final EntityManagerFactoryImpl _emf;
    private FetchPlan _fetch = null;

    /**
     * Constructor; supply factory and delegate.
     */
    public EntityManagerImpl(EntityManagerFactoryImpl factory,
        Broker broker) {
        _emf = factory;
        RuntimeExceptionTranslator translator =
            PersistenceExceptions.getRollbackTranslator(this);
        _broker = new DelegatingBroker(broker, translator);
        _broker.setImplicitBehavior(this, translator);
    }

    /**
     * Broker delegate.
     */
    public Broker getBroker() {
        return _broker.getDelegate();
    }

    public ConnectionMetaData getMetaData()
        throws ResourceException {
        return _broker.getMetaData();
    }

    public Interaction createInteraction()
        throws ResourceException {
        assertNotCloseInvoked();
        return _broker.createInteraction();
    }

    public LocalTransaction getLocalTransaction()
        throws ResourceException {
        return this;
    }

    public ResultSetInfo getResultSetInfo()
        throws ResourceException {
        return _broker.getResultSetInfo();
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
            if (_fetch == null)
                _fetch = _emf.toFetchPlan(_broker,
                    _broker.getFetchConfiguration());
            return _fetch;
        } finally {
            _broker.unlock();
        }
    }

    public int getConnectionRetainMode() {
        return _broker.getConnectionRetainMode();
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

    public int getRestoreState() {
        return _broker.getRestoreState();
    }

    public void setRestoreState(int val) {
        assertNotCloseInvoked();
        _broker.setRestoreState(val);
    }

    public boolean getRetainState() {
        return _broker.getRetainState();
    }

    public void setRetainState(boolean val) {
        assertNotCloseInvoked();
        _broker.setRetainState(val);
    }

    public int getAutoClear() {
        return _broker.getAutoClear();
    }

    public void setAutoClear(int val) {
        assertNotCloseInvoked();
        _broker.setAutoClear(val);
    }

    public int getDetachState() {
        return _broker.getDetachState();
    }

    public void setDetachState(int mode) {
        assertNotCloseInvoked();
        _broker.setDetachState(mode);
    }

    public int getAutoDetach() {
        return _broker.getAutoDetach();
    }

    public void setAutoDetach(int flags) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(flags);
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

    public boolean isLargeTransaction() {
        return _broker.isLargeTransaction();
    }

    public void setLargeTransaction(boolean largeTransaction) {
        assertNotCloseInvoked();
        _broker.setLargeTransaction(largeTransaction);
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

    public int getTransactionListenerCallbackMode() {
        return _broker.getTransactionListenerCallbackMode();
    }

    public void setTransactionListenerCallbackMode(int mode) {
        assertNotCloseInvoked();
        _broker.setTransactionListenerCallbackMode(mode);
    }

    public void addLifecycleListener(Object listener, Class... classes) {
        assertNotCloseInvoked();
        _broker.addLifecycleListener(listener, classes);
    }

    public void removeLifecycleListener(Object listener) {
        assertNotCloseInvoked();
        _broker.removeLifecycleListener(listener);
    }

    public int getLifecycleListenerCallbackMode() {
        return _broker.getLifecycleListenerCallbackMode();
    }

    public void setLifecycleListenerCallbackMode(int mode) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(mode);
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
        return OpenJPAPersistence.fromOpenJPAObjectIdClass
                (_broker.getObjectIdType(cls));
    }

    public EntityTransaction getTransaction() {
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

    public boolean getRollbackOnly() {
        if (!isActive())
            throw new IllegalStateException(_loc.get("no-transaction")
                .getMessage());

        return _broker.getRollbackOnly();
    }

    public void setRollbackOnly() {
        _broker.setRollbackOnly();
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
        if (sm == null && !ImplHelper.isManagedType(entity.getClass()))
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
        _broker.evictAll(extent.getDelegate(), this);
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
            return new Object[0];
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
        return new QueryImpl(this, _broker.newQuery(language, query));
    }

    public OpenJPAQuery createQuery(Query query) {
        if (query == null)
            return createQuery((String) null);
        assertNotCloseInvoked();
        org.apache.openjpa.kernel.Query q = ((QueryImpl) query).getDelegate();
        return new QueryImpl(this, _broker.newQuery(q.getLanguage(),
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

            OpenJPAQuery q = new QueryImpl(this, del);
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
        return new QueryImpl(this, kernelQuery);
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
        _broker.detachAll(this);
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
        return OpenJPAPersistence.fromOpenJPAObjectId(_broker.getObjectId(o));
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
}

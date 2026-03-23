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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Query;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Timeout;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.DataCacheRetrieveMode;
import org.apache.openjpa.kernel.DataCacheStoreMode;
import org.apache.openjpa.kernel.DelegatingBroker;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FindCallbacks;
import org.apache.openjpa.kernel.OpCallbacks;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.MultiQueryMetaData;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.criteria.CriteriaBuilderImpl;
import org.apache.openjpa.persistence.criteria.OpenJPACriteriaBuilder;
import org.apache.openjpa.persistence.criteria.OpenJPACriteriaQuery;
import org.apache.openjpa.persistence.validation.ValidationUtils;
import org.apache.openjpa.util.BlacklistClassResolver;
import org.apache.openjpa.util.ExceptionInfo;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.UserException;


/**
 * Implementation of {@link EntityManager} interface.
 *
 * @author Patrick Linskey
 * @author Abe White
 */
public class EntityManagerImpl
    implements OpenJPAEntityManagerSPI, Externalizable,
    FindCallbacks, OpCallbacks, Closeable, OpenJPAEntityTransaction {

    private static final Localizer _loc = Localizer.forPackage(EntityManagerImpl.class);
    private static final Object[] EMPTY_OBJECTS = new Object[0];

    private static final String GET_LOCK_MODE = "getLockMode";
    private static final String LOCK = "lock";
    private static final String REFRESH = "refresh";

    private DelegatingBroker _broker;
    private EntityManagerFactoryImpl _emf;
    private final Map<FetchConfiguration,FetchPlan> _plans = new IdentityHashMap<>(1);
    protected RuntimeExceptionTranslator _ret = PersistenceExceptions.getRollbackTranslator(this);
    private boolean _convertPositionalParams = false;
    private boolean _isJoinedToTransaction;
    private boolean _closedMethodCall;
    private Map<String, Object> properties;

    public EntityManagerImpl() {
        // for Externalizable
    }

    /**
     * Constructor; supply factory and delegate.
     */
    public EntityManagerImpl(EntityManagerFactoryImpl factory, Broker broker) {
        initialize(factory, broker);
    }

    private void initialize(EntityManagerFactoryImpl factory, Broker broker) {
        _emf = factory;
        _broker = new DelegatingBroker(broker, _ret);
        _broker.setImplicitBehavior(this, _ret);
        _broker.putUserObject(JPAFacadeHelper.EM_KEY, this);
        _convertPositionalParams =
            factory.getConfiguration().getCompatibilityInstance().getConvertPositionalParametersToNamed();
    }

    /**
     * Translate the given exception, marking the transaction for
     * rollback if active per JPA spec section 3.3.7.1.
     */
    RuntimeException translateException(RuntimeException re) {
        RuntimeException ex = PersistenceExceptions.toPersistenceException(re);
        if (!(ex instanceof NonUniqueResultException)
            && !(ex instanceof NoResultException)
            && !(ex instanceof LockTimeoutException)
            && !(ex instanceof QueryTimeoutException)) {
            try {
                if ((isOpen() || _closedMethodCall) && _broker.isActive()) {
                    _broker.setRollbackOnly(ex);
                }
            } catch (Exception ignore) {
                // best effort
            } finally {
                _closedMethodCall = false;
            }
        }
        return ex;
    }

    /**
     * Translate a checked exception via PersistenceExceptions, then
     * mark the transaction for rollback per JPA spec section 3.3.7.1.
     */
    RuntimeException translateException(Exception e) {
        if (e instanceof RuntimeException) {
            return translateException((RuntimeException) e);
        }
        return translateException(PersistenceExceptions.toPersistenceException(e));
    }

    /**
     * Broker delegate.
     */
    public Broker getBroker() {
        return _broker.getDelegate();
    }

    @Override
    public OpenJPAEntityManagerFactory getEntityManagerFactory() {
        try {
            assertNotCloseInvoked();
        } catch (RuntimeException re) {
            throw translateException(re);
        }
        return _emf;
    }

    @Override
    public OpenJPAConfiguration getConfiguration() {
        return _broker.getConfiguration();
    }

    @Override
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

    @Override
    public FetchPlan pushFetchPlan() {
		return pushFetchPlan(null);
    }

    public FetchPlan pushFetchPlan(FetchConfiguration fc) {
        assertNotCloseInvoked();
        _broker.lock();
        try {
            _broker.pushFetchConfiguration(fc);
            return getFetchPlan();
        } finally {
            _broker.unlock();
        }
    }

    @Override
    public void popFetchPlan() {
        assertNotCloseInvoked();
        _broker.lock();
        try {
            _plans.remove(_broker.getFetchConfiguration());
            _broker.popFetchConfiguration();
        } finally {
            _broker.unlock();
        }
    }

    @Override
    public ConnectionRetainMode getConnectionRetainMode() {
        return ConnectionRetainMode.fromKernelConstant(
            _broker.getConnectionRetainMode());
    }

    @Override
    public boolean isTransactionManaged() {
        return _broker.isManaged();
    }

    @Override
    public boolean isManaged() {
        return _broker.isManaged();
    }

    @Override
    public ManagedRuntime getManagedRuntime() {
        return _broker.getManagedRuntime();
    }

    @Override
    public boolean getSyncWithManagedTransactions() {
        return _broker.getSyncWithManagedTransactions();
    }

    @Override
    public void setSyncWithManagedTransactions(boolean sync) {
        assertNotCloseInvoked();
        _broker.setSyncWithManagedTransactions(sync);
    }

    @Override
    public ClassLoader getClassLoader() {
        return _broker.getClassLoader();
    }

    @Override
    public String getConnectionUserName() {
        return _broker.getConnectionUserName();
    }

    @Override
    public String getConnectionPassword() {
        return _broker.getConnectionPassword();
    }

    @Override
    public boolean getMultithreaded() {
        return _broker.getMultithreaded();
    }

    @Override
    public void setMultithreaded(boolean multithreaded) {
        assertNotCloseInvoked();
        _broker.setMultithreaded(multithreaded);
        properties = null;
    }

    @Override
    public boolean getIgnoreChanges() {
        return _broker.getIgnoreChanges();
    }

    @Override
    public void setIgnoreChanges(boolean val) {
        assertNotCloseInvoked();
        _broker.setIgnoreChanges(val);
        properties = null;
    }

    @Override
    public boolean getNontransactionalRead() {
        return _broker.getNontransactionalRead();
    }

    @Override
    public void setNontransactionalRead(boolean val) {
        assertNotCloseInvoked();
        _broker.setNontransactionalRead(val);
        properties = null;
    }

    @Override
    public boolean getNontransactionalWrite() {
        return _broker.getNontransactionalWrite();
    }

    @Override
    public void setNontransactionalWrite(boolean val) {
        assertNotCloseInvoked();
        _broker.setNontransactionalWrite(val);
        properties = null;
    }

    @Override
    public boolean getOptimistic() {
        return _broker.getOptimistic();
    }

    @Override
    public void setOptimistic(boolean val) {
        assertNotCloseInvoked();
        _broker.setOptimistic(val);
        properties = null;
    }

    @Override
    public RestoreStateType getRestoreState() {
        return RestoreStateType.fromKernelConstant(_broker.getRestoreState());
    }

    @Override
    public void setRestoreState(RestoreStateType val) {
        assertNotCloseInvoked();
        _broker.setRestoreState(val.toKernelConstant());
        properties = null;
    }

    @Override
    public void setRestoreState(int restore) {
        assertNotCloseInvoked();
        _broker.setRestoreState(restore);
        properties = null;
    }

    @Override
    public boolean getRetainState() {
        return _broker.getRetainState();
    }

    @Override
    public void setRetainState(boolean val) {
        assertNotCloseInvoked();
        _broker.setRetainState(val);
        properties = null;
    }

    @Override
    public AutoClearType getAutoClear() {
        return AutoClearType.fromKernelConstant(_broker.getAutoClear());
    }

    @Override
    public void setAutoClear(AutoClearType val) {
        assertNotCloseInvoked();
        _broker.setAutoClear(val.toKernelConstant());
        properties = null;
    }

    @Override
    public void setAutoClear(int autoClear) {
        assertNotCloseInvoked();
        _broker.setAutoClear(autoClear);
        properties = null;
    }

    @Override
    public DetachStateType getDetachState() {
        return DetachStateType.fromKernelConstant(_broker.getDetachState());
    }

    @Override
    public void setDetachState(DetachStateType type) {
        assertNotCloseInvoked();
        _broker.setDetachState(type.toKernelConstant());
        properties = null;
    }

    @Override
    public void setDetachState(int detach) {
        assertNotCloseInvoked();
        _broker.setDetachState(detach);
        properties = null;
    }

    @Override
    public EnumSet<AutoDetachType> getAutoDetach() {
        return AutoDetachType.toEnumSet(_broker.getAutoDetach());
    }

    @Override
    public void setAutoDetach(AutoDetachType flag) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(AutoDetachType.fromEnumSet(EnumSet.of(flag)));
        properties = null;
    }

    @Override
    public void setAutoDetach(EnumSet<AutoDetachType> flags) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(AutoDetachType.fromEnumSet(flags));
        properties = null;
    }

    @Override
    public void setAutoDetach(int autoDetachFlags) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(autoDetachFlags);
        properties = null;
    }

    @Override
    public void setAutoDetach(AutoDetachType value, boolean on) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(AutoDetachType.fromEnumSet(EnumSet.of(value)),on);
        properties = null;
    }

    @Override
    public void setAutoDetach(int flag, boolean on) {
        assertNotCloseInvoked();
        _broker.setAutoDetach(flag, on);
        properties = null;
    }

    @Override
    public boolean getEvictFromStoreCache() {
        return _broker.getEvictFromDataCache();
    }

    @Override
    public void setEvictFromStoreCache(boolean evict) {
        assertNotCloseInvoked();
        _broker.setEvictFromDataCache(evict);
        properties = null;
    }

    @Override
    public boolean getPopulateStoreCache() {
        return _broker.getPopulateDataCache();
    }

    @Override
    public void setPopulateStoreCache(boolean cache) {
        assertNotCloseInvoked();
        _broker.setPopulateDataCache(cache);
        properties = null;
    }

    @Override
    public boolean isTrackChangesByType() {
        return _broker.isTrackChangesByType();
    }

    @Override
    public void setTrackChangesByType(boolean trackByType) {
        assertNotCloseInvoked();
        _broker.setTrackChangesByType(trackByType);
        properties = null;
    }

    @Override
    public boolean isLargeTransaction() {
        return isTrackChangesByType();
    }

    @Override
    public void setLargeTransaction(boolean value) {
        setTrackChangesByType(value);
    }

    @Override
    public Object getUserObject(Object key) {
        return _broker.getUserObject(key);
    }

    @Override
    public Object putUserObject(Object key, Object val) {
        assertNotCloseInvoked();
        return _broker.putUserObject(key, val);
    }

    @Override
    public void addTransactionListener(Object listener) {
        assertNotCloseInvoked();
        _broker.addTransactionListener(listener);
    }

    @Override
    public void removeTransactionListener(Object listener) {
        assertNotCloseInvoked();
        _broker.removeTransactionListener(listener);
    }

    @Override
    public EnumSet<CallbackMode> getTransactionListenerCallbackModes() {
        return CallbackMode.toEnumSet(
            _broker.getTransactionListenerCallbackMode());
    }

    @Override
    public void setTransactionListenerCallbackMode(CallbackMode mode) {
        assertNotCloseInvoked();
        _broker.setTransactionListenerCallbackMode(
            CallbackMode.fromEnumSet(EnumSet.of(mode)));
    }

    @Override
    public void setTransactionListenerCallbackMode(EnumSet<CallbackMode> modes){
        assertNotCloseInvoked();
        _broker.setTransactionListenerCallbackMode(
            CallbackMode.fromEnumSet(modes));
    }

    @Override
    public int getTransactionListenerCallbackMode() {
        return _broker.getTransactionListenerCallbackMode();
    }

    @Override
    public void setTransactionListenerCallbackMode(int callbackMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLifecycleListener(Object listener, Class... classes) {
        assertNotCloseInvoked();
        _broker.addLifecycleListener(listener, classes);
    }

    @Override
    public void removeLifecycleListener(Object listener) {
        assertNotCloseInvoked();
        _broker.removeLifecycleListener(listener);
    }

    @Override
    public EnumSet<CallbackMode> getLifecycleListenerCallbackModes() {
        return CallbackMode.toEnumSet(
            _broker.getLifecycleListenerCallbackMode());
    }

    @Override
    public void setLifecycleListenerCallbackMode(CallbackMode mode) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(
            CallbackMode.fromEnumSet(EnumSet.of(mode)));
    }

    @Override
    public void setLifecycleListenerCallbackMode(EnumSet<CallbackMode> modes) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(
            CallbackMode.fromEnumSet(modes));
    }

    @Override
    public int getLifecycleListenerCallbackMode() {
        return _broker.getLifecycleListenerCallbackMode();
    }

    @Override
    public void setLifecycleListenerCallbackMode(int callbackMode) {
        assertNotCloseInvoked();
        _broker.setLifecycleListenerCallbackMode(callbackMode);
    }

    /**
     * Validates that the given primary key value is of the correct type for the
     * given entity class. Throws IllegalArgumentException if the entity class
     * is not a managed entity, if the PK is null, or if the PK type does not
     * match the entity's declared primary key type.
     *
     * @param cls the entity class
     * @param oid the primary key value
     * @param methodName the calling method name for error messages
     */
    private void validatePrimaryKey(Class<?> cls, Object oid, String methodName) {
        MetaDataRepository repo = _broker.getConfiguration()
            .getMetaDataRepositoryInstance();
        ClassMetaData meta = repo.getMetaData(cls, null, false);
        if (meta == null) {
            throw new IllegalArgumentException(
                _loc.get("not-entity", cls).getMessage());
        }
        if (oid == null) {
            throw new IllegalArgumentException(
                _loc.get("null-pk", cls).getMessage());
        }
        // For application identity with OpenJPA single-field identity,
        // validate the PK value type matches the declared PK field type
        if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION
            && meta.isOpenJPAIdentity()) {
            FieldMetaData[] pkFields = meta.getPrimaryKeyFields();
            if (pkFields.length == 1) {
                Class<?> expectedType =
                    Filters.wrap(pkFields[0].getDeclaredType());
                Class<?> actualType = Filters.wrap(oid.getClass());
                if (!expectedType.isAssignableFrom(actualType)
                    && !isNumericCompatible(expectedType, actualType)) {
                    throw new IllegalArgumentException(
                        _loc.get("bad-pk-type", cls,
                            expectedType.getName(),
                            actualType.getName()).getMessage());
                }
            }
        } else if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION) {
            // Composite PK: the oid must be assignable to the ID class
            // or be a stringified form or Object[] (handled by newObjectId)
            Class<?> idClass = meta.getObjectIdType();
            if (idClass != null
                && !ImplHelper.isAssignable(idClass, oid.getClass())
                && !(oid instanceof String)
                && !(oid instanceof Object[])) {
                throw new IllegalArgumentException(
                    _loc.get("bad-pk-type", cls,
                        idClass.getName(),
                        oid.getClass().getName()).getMessage());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getReference(Class<T> cls, Object oid) {
        assertNotCloseInvoked();
        try {
            validatePrimaryKey(cls, oid, "getReference");
            oid = _broker.newObjectId(cls, oid);
            return (T) _broker.find(oid, false, this);
        } catch (jakarta.persistence.EntityNotFoundException enfe) {
            throw enfe;
        } catch (RuntimeException re) {
            throw translateException(re);
        } catch (Exception e) {
            // Per JPA spec, getReference() should throw EntityNotFoundException
            // if the entity does not exist in the database. With runtime-enhanced
            // (unenhanced) entities, the broker may eagerly load the entity to
            // determine the concrete subclass type, and throw ObjectNotFoundException
            // immediately rather than returning a hollow proxy.
            throw new jakarta.persistence.EntityNotFoundException(
                e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> cls, Object oid) {
        assertNotCloseInvoked();
        try {
            validateFindArguments(cls, oid);
            oid = _broker.newObjectId(cls, oid);
            return (T) _broker.find(oid, true, this);
        } catch (RuntimeException re) {
            throw translateException(re);
        }
    }

    @Override
    public <T> T find(Class<T> cls, Object oid, LockModeType mode) {
        return find(cls, oid, mode, null);
    }

    @Override
    public <T> T find(Class<T> cls, Object oid,
        Map<String, Object> properties){
        return find(cls, oid, null, properties);
    }
    
	@Override
	public <T> T find(Class<T> cls, Object oid, FindOption... options) {
		Map<String, Object> props = new HashMap<>();
		LockModeType mode = null;
		for (FindOption opt: options) {
			if (opt instanceof LockModeType lmt) {
				mode = lmt;
			} else if (opt instanceof CacheRetrieveMode crm) {
				props.put(JPAProperties.CACHE_RETRIEVE_MODE, crm);
			} else if (opt instanceof CacheStoreMode csm) {
				props.put(JPAProperties.CACHE_STORE_MODE, csm);
			} else if (opt instanceof PessimisticLockScope pls) {
				props.put(JPAProperties.LOCK_SCOPE, pls);
			} else if (opt instanceof Timeout timeout) {
				props.put(JPAProperties.LOCK_TIMEOUT, timeout.milliseconds());
			}
			// open to custom options
		}
		return find(cls, oid, mode, props);
	}

    @Override
    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> cls, Object oid, LockModeType mode, Map<String, Object> properties) {
        assertNotCloseInvoked();
        try {
            validateFindArguments(cls, oid);
            properties = cloneProperties(properties);
            configureCurrentCacheModes(pushFetchPlan(), properties);
            configureCurrentFetchPlan(getFetchPlan(), properties, mode, true);
            try {
                oid = _broker.newObjectId(cls, oid);
                return (T) _broker.find(oid, true, this);
            } finally {
                popFetchPlan();
            }
        } catch (RuntimeException re) {
            throw translateException(re);
        }
    }

    /**
     * Validates find/getReference arguments per JPA spec: class must be
     * an entity, PK must not be null, and PK type must be compatible.
     */
    private void validateFindArguments(Class<?> cls, Object oid) {
        if (oid == null) {
            throw new IllegalArgumentException(
                _loc.get("null-pk", cls).getMessage());
        }
        // Verify cls is a known entity
        MetaDataRepository repos = _broker.getConfiguration()
            .getMetaDataRepositoryInstance();
        ClassMetaData meta = repos.getMetaData(cls,
            _broker.getClassLoader(), false);
        if (meta == null) {
            throw new IllegalArgumentException(
                "Class \"" + cls.getName()
                + "\" is not a known entity type.");
        }
        validatePrimaryKeyType(cls, oid);
    }

    /**
     * Validates that the given primary key value is type-compatible with
     * the entity's declared primary key type. Per JPA spec, find() must
     * throw IllegalArgumentException if the PK type is not valid.
     */
    private void validatePrimaryKeyType(Class<?> cls, Object oid) {
        MetaDataRepository repos = _broker.getConfiguration()
            .getMetaDataRepositoryInstance();
        ClassMetaData meta = repos.getMetaData(cls,
            _broker.getClassLoader(), false);
        if (meta == null) {
            return;
        }
        if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION) {
            if (meta.isOpenJPAIdentity()) {
                // single-field PK: check that the value type is compatible
                FieldMetaData pkField = meta.getPrimaryKeyFields()[0];
                Class<?> pkType = pkField.getDeclaredType();
                // Skip validation for derived identity (@Id @ManyToOne/@OneToOne)
                // where the PK field type is an entity and the passed value
                // is the related entity's PK, not the entity itself
                if (pkField.getDeclaredTypeMetaData() != null) {
                    return;
                }
                if (pkType.isPrimitive()) {
                    pkType = Filters.wrap(pkType);
                }
                if (!pkType.isInstance(oid)
                    && !isNumericCompatible(pkType, oid.getClass())) {
                    throw new IllegalArgumentException(
                        _loc.get("bad-pk-type", cls.getName(),
                            pkType.getName(), oid.getClass().getName())
                            .getMessage());
                }
            }
            // For compound PK or IdClass, let newObjectId() handle validation
            // since the value may be the IdClass, a stringified form, or
            // a related entity's PK (derived identity)
        }
    }

    private static final Class<?>[] NUMERIC_WIDENING_ORDER = {
        Byte.class, Short.class, Integer.class, Long.class,
        Float.class, Double.class, java.math.BigInteger.class,
        java.math.BigDecimal.class
    };

    private static boolean isNumericCompatible(Class<?> target, Class<?> value) {
        if (!Number.class.isAssignableFrom(target)
            || !Number.class.isAssignableFrom(value)) {
            return false;
        }
        // Allow widening (e.g. Integer → Long) but not narrowing
        int targetIdx = numericIndex(target);
        int valueIdx = numericIndex(value);
        if (targetIdx < 0 || valueIdx < 0) {
            return false;
        }
        return valueIdx <= targetIdx;
    }

    private static int numericIndex(Class<?> cls) {
        for (int i = 0; i < NUMERIC_WIDENING_ORDER.length; i++) {
            if (NUMERIC_WIDENING_ORDER[i] == cls) {
                return i;
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] findAll(Class<T> cls, Object... oids) {
        if (oids.length == 0)
            return (T[]) Array.newInstance(cls, 0);
        Collection<T> ret = findAll(cls, Arrays.asList(oids));
        return ret.toArray((T[]) Array.newInstance(cls, ret.size()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> findAll(final Class<T> cls, Collection oids) {
        assertNotCloseInvoked();
        Object[] objs = _broker.findAll(oids, true, new FindCallbacks() {
            @Override
            public Object processArgument(Object oid) {
                return _broker.newObjectId(cls, oid);
            }

            @Override
            public Object processReturn(Object oid, OpenJPAStateManager sm) {
                return EntityManagerImpl.this.processReturn(oid, sm);
            }
        });
        return (Collection<T>) Arrays.asList(objs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T findCached(Class<T> cls, Object oid) {
        assertNotCloseInvoked();
        return (T) _broker.findCached(_broker.newObjectId(cls, oid), this);
    }

    @Override
    public Class getObjectIdClass(Class cls) {
        assertNotCloseInvoked();
        if (cls == null)
            return null;
        return JPAFacadeHelper.fromOpenJPAObjectIdClass
                (_broker.getObjectIdType(cls));
    }

    @Override
    public OpenJPAEntityTransaction getTransaction() {
        if (_broker.isManaged())
            throw new InvalidStateException(_loc.get("get-managed-trans"),
                null, null, false);
        return this;
    }

    @Override
    public void joinTransaction() {
        if (!_broker.syncWithManagedTransaction()) {
            throw new TransactionRequiredException(_loc.get
                    ("no-managed-trans"), null, null, false);
        } else {
            _isJoinedToTransaction = true;
        }

        assertNotCloseInvoked();
        if (!_broker.syncWithManagedTransaction())
            throw new TransactionRequiredException(_loc.get
                ("no-managed-trans"), null, null, false);
    }

    @Override
    public boolean isJoinedToTransaction() {
        assertNotCloseInvoked();
        return isActive() && _isJoinedToTransaction;
    }

    @Override
    public void begin() {
        _broker.begin();
        _isJoinedToTransaction = true;
    }

    @Override
    public void commit() {
        try {
            _broker.commit();
        } catch (RollbackException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
        	// Per JPA 2.0 spec, if the exception was due to a JSR-303
            // constraint violation, the ConstraintViolationException should be
            // thrown.  Since JSR-303 is optional, the cast to RuntimeException
            // prevents the introduction of a runtime dependency on the BV API.
            if (ValidationUtils.isConstraintViolationException(e))
                throw (RuntimeException)e;
            // RollbackExceptions are special and aren't handled by the
            // normal exception translator, since the spec says they
            // should be thrown whenever the commit fails for any reason at
            // all, wheras the exception translator handles exceptions that
            // are caused for specific reasons

            // pass along the failed object if one is available.
            Object failedObject = null;
            if (e instanceof ExceptionInfo){
            	failedObject = ((ExceptionInfo)e).getFailedObject();
            }

            throw new RollbackException(e).setFailedObject(failedObject);
        } finally {
            _isJoinedToTransaction = false;
        }
    }

    @Override
    public void rollback() {
        _broker.rollback();
        _isJoinedToTransaction = false;
    }

    @Override
    public void commitAndResume() {
        _broker.commitAndResume();
    }

    @Override
    public void rollbackAndResume() {
        _broker.rollbackAndResume();
    }

    @Override
    public Throwable getRollbackCause() {
        if (!isActive())
            throw new IllegalStateException(_loc.get("no-transaction")
                .getMessage());

        return _broker.getRollbackCause();
    }

    @Override
    public boolean getRollbackOnly() {
        if (!_broker.isActive())
            throw new IllegalStateException(_loc.get("no-transaction")
                .getMessage());

        return _broker.getRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        _broker.setRollbackOnly();
    }

    @Override
    public void setRollbackOnly(Throwable cause) {
        _broker.setRollbackOnly(cause);
    }

    @Override
    public void setSavepoint(String name) {
        assertNotCloseInvoked();
        _broker.setSavepoint(name);
    }

    @Override
    public void rollbackToSavepoint() {
        assertNotCloseInvoked();
        _broker.rollbackToSavepoint();
    }

    @Override
    public void rollbackToSavepoint(String name) {
        assertNotCloseInvoked();
        _broker.rollbackToSavepoint(name);
    }

    @Override
    public void releaseSavepoint() {
        assertNotCloseInvoked();
        _broker.releaseSavepoint();
    }

    @Override
    public void releaseSavepoint(String name) {
        assertNotCloseInvoked();
        _broker.releaseSavepoint(name);
    }

    @Override
    public void flush() {
        assertNotCloseInvoked();
        _broker.assertOpen();
        _broker.assertActiveTransaction();
        _broker.flush();
    }

    @Override
    public void preFlush() {
        assertNotCloseInvoked();
        _broker.preFlush();
    }

    @Override
    public void validateChanges() {
        assertNotCloseInvoked();
        _broker.validateChanges();
    }

    @Override
    public boolean isActive() {
        try {
            return _broker.isActive();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isStoreActive() {
        return _broker.isStoreActive();
    }

    @Override
    public void beginStore() {
        _broker.beginStore();
    }

    @Override
    public boolean contains(Object entity) {
        assertNotCloseInvoked();
        if (entity == null)
            return false;
        OpenJPAStateManager sm = _broker.getStateManager(entity);
        if (sm == null
            && !ImplHelper.isManagedType(getConfiguration(), entity.getClass()))
            throw _ret.translate(new ArgumentException(_loc.get("not-entity",
                entity.getClass()), null, null, true));
        return sm != null && !sm.isDeleted();
    }

    @Override
    public boolean containsAll(Object... entities) {
        for (Object entity : entities)
            if (!contains(entity))
                return false;
        return true;
    }

    @Override
    public boolean containsAll(Collection entities) {
        for (Object entity : entities)
            if (!contains(entity))
                return false;
        return true;
    }

    @Override
    public void persist(Object entity) {
        assertNotCloseInvoked();
        _broker.persist(entity, this);
    }

    @Override
    public void persistAll(Object... entities) {
        persistAll(Arrays.asList(entities));
    }

    @Override
    public void persistAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.persistAll(entities, this);
    }

    @Override
    public void remove(Object entity) {
        assertNotCloseInvoked();
        _broker.delete(entity, this);
    }

    @Override
    public void removeAll(Object... entities) {
        removeAll(Arrays.asList(entities));
    }

    @Override
    public void removeAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.deleteAll(entities, this);
    }

    @Override
    public void release(Object entity) {
        assertNotCloseInvoked();
        _broker.release(entity, this);
    }

    @Override
    public void releaseAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.releaseAll(entities, this);
    }

    @Override
    public void releaseAll(Object... entities) {
        releaseAll(Arrays.asList(entities));
    }

    @Override
    public void refresh(Object entity) {
        refresh(entity, null, null);
    }

    @Override
    public void refresh(Object entity, LockModeType mode) {
        refresh(entity, mode, null);
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        refresh(entity, null, properties);
    }

    @Override
    public void refresh(Object entity, LockModeType mode, Map<String, Object> properties) {
        assertNotCloseInvoked();
        assertValidAttchedEntity(REFRESH, entity);

        _broker.assertWriteOperation();
        configureCurrentCacheModes(pushFetchPlan(), properties);
        configureCurrentFetchPlan(getFetchPlan(), properties, mode, true);
        DataCacheRetrieveMode rmode = getFetchPlan().getCacheRetrieveMode();
        if (DataCacheRetrieveMode.USE.equals(rmode) || rmode == null) {
            getFetchPlan().setCacheRetrieveMode(DataCacheRetrieveMode.BYPASS);
        }
        try {
            _broker.refresh(entity, this);
        } finally {
            popFetchPlan();
        }
    }

    @Override
    public void refreshAll() {
        assertNotCloseInvoked();
        _broker.assertWriteOperation();
        _broker.refreshAll(_broker.getTransactionalObjects(), this);
    }

    @Override
    public void refreshAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.assertWriteOperation();
        _broker.refreshAll(entities, this);
    }

    @Override
    public void refreshAll(Object... entities) {
        refreshAll(Arrays.asList(entities));
    }

    @Override
    public void retrieve(Object entity) {
        assertNotCloseInvoked();
        _broker.retrieve(entity, true, this);
    }

    @Override
    public void retrieveAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.retrieveAll(entities, true, this);
    }

    @Override
    public void retrieveAll(Object... entities) {
        retrieveAll(Arrays.asList(entities));
    }

    @Override
    public void evict(Object entity) {
        assertNotCloseInvoked();
        _broker.evict(entity, this);
    }

    @Override
    public void evictAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.evictAll(entities, this);
    }

    @Override
    public void evictAll(Object... entities) {
        evictAll(Arrays.asList(entities));
    }

    @Override
    public void evictAll() {
        assertNotCloseInvoked();
        _broker.evictAll(this);
    }

    @Override
    public void evictAll(Class cls) {
        assertNotCloseInvoked();
        _broker.evictAll(_broker.newExtent(cls, true), this);
    }

    @Override
    public void evictAll(Extent extent) {
        assertNotCloseInvoked();
        _broker.evictAll(((ExtentImpl) extent).getDelegate(), this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T detachCopy(T entity) {
        assertNotCloseInvoked();
        Compatibility compat = this.getConfiguration().
            getCompatibilityInstance();
        boolean copyOnDetach = compat.getCopyOnDetach();
        boolean cascadeWithDetach = compat.getCascadeWithDetach();
        // Set compatibility options to get 1.x detach behavior
        compat.setCopyOnDetach(true);
        compat.setCascadeWithDetach(true);
        try {
            T t = (T)_broker.detach(entity, this);
            return t;
        } finally {
            // Reset compatibility options
            compat.setCopyOnDetach(copyOnDetach);
            compat.setCascadeWithDetach(cascadeWithDetach);
        }
    }

    @Override
    public Object[] detachAll(Object... entities) {
        assertNotCloseInvoked();
        return _broker.detachAll(Arrays.asList(entities), this);
    }

    @Override
    public Collection detachAll(Collection entities) {
        assertNotCloseInvoked();
        return Arrays.asList(_broker.detachAll(entities, this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T merge(T entity) {
        assertNotCloseInvoked();
        return (T) _broker.attach(entity, true, this);
    }

    @Override
    public Object[] mergeAll(Object... entities) {
        if (entities.length == 0)
            return EMPTY_OBJECTS;
        return mergeAll(Arrays.asList(entities)).toArray();
    }

    @Override
    public Collection mergeAll(Collection entities) {
        assertNotCloseInvoked();
        return Arrays.asList(_broker.attachAll(entities, true, this));
    }

    @Override
    public void transactional(Object entity, boolean updateVersion) {
        assertNotCloseInvoked();
        _broker.transactional(entity, updateVersion, this);
    }

    @Override
    public void transactionalAll(Collection objs, boolean updateVersion) {
        assertNotCloseInvoked();
        _broker.transactionalAll(objs, updateVersion, this);
    }

    @Override
    public void transactionalAll(Object[] objs, boolean updateVersion) {
        assertNotCloseInvoked();
        _broker.transactionalAll(Arrays.asList(objs), updateVersion, this);
    }

    @Override
    public void nontransactional(Object entity) {
        assertNotCloseInvoked();
        _broker.nontransactional(entity, this);
    }

    @Override
    public void nontransactionalAll(Collection objs) {
        assertNotCloseInvoked();
        _broker.nontransactionalAll(objs, this);
    }

    @Override
    public void nontransactionalAll(Object[] objs) {
        assertNotCloseInvoked();
        _broker.nontransactionalAll(Arrays.asList(objs), this);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public <T> Extent<T> createExtent(Class<T> cls, boolean subclasses) {
        assertNotCloseInvoked();
        return new ExtentImpl<T>(this, _broker.newExtent(cls, subclasses));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypedQuery<T> createQuery(String query, Class<T> resultClass) {
        checkTuple(resultClass);
        return createQuery(query).setResultClass(resultClass);
    }

    private <T> void checkTuple(Class<T> resultClass) {
        if (Tuple.class == resultClass) {
            throw new PersistenceException("Tuple is not a valid type", null, null, true);
        }
    }

    @Override
    public OpenJPAQuery createQuery(String query) {
        return createQuery(JPQLParser.LANG_JPQL, query);
    }

    @Override
    public OpenJPAQuery createQuery(String language, String query) {
        assertNotCloseInvoked();
        try {
            // We need
            if (query != null && _convertPositionalParams && JPQLParser.LANG_JPQL.equals(language)) {
                query = query.replaceAll("[\\?]", "\\:_");
            }
            String qid = query;
            PreparedQuery pq = JPQLParser.LANG_JPQL.equals(language)
                ? getPreparedQuery(qid) : null;
            org.apache.openjpa.kernel.Query q = (pq == null || !pq.isInitialized())
                ? _broker.newQuery(language, query)
                : _broker.newQuery(pq.getLanguage(), pq);
            // have to validate JPQL according to spec
            if (pq == null && JPQLParser.LANG_JPQL.equals(language))
                q.compile();
            if (pq != null) {
                pq.setInto(q);
            }
            return newQueryImpl(q, null).setId(qid);
        } catch (RuntimeException re) {
            throw translateException(re);
        }
    }

    @Override
    public OpenJPAQuery createQuery(Query query) {
        if (query == null)
            return createQuery((String) null);
        assertNotCloseInvoked();
        org.apache.openjpa.kernel.Query q = ((QueryImpl) query).getDelegate();
        return newQueryImpl(_broker.newQuery(q.getLanguage(), q), null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        checkTuple(resultClass);
        return createNamedQuery(name).setResultClass(resultClass);
    }

    @Override
    public OpenJPAQuery createNamedQuery(String name) {
        assertNotCloseInvoked();
        _broker.assertOpen();
        try {
            QueryMetaData meta = _broker.getConfiguration().
                getMetaDataRepositoryInstance().getQueryMetaData(null, name,
                _broker.getClassLoader(), true);
            String qid = meta.getQueryString();

            PreparedQuery pq = JPQLParser.LANG_JPQL.equals(meta.getLanguage()) ? getPreparedQuery(qid) : null;
            org.apache.openjpa.kernel.Query del =
                (pq == null || !pq.isInitialized()) ? _broker.newQuery(meta.getLanguage(), meta.getQueryString())
                    : _broker.newQuery(pq.getLanguage(), pq);

            if (pq != null) {
                pq.setInto(del);
            } else {
                meta.setInto(del);
                del.compile();
            }

            OpenJPAQuery q = newQueryImpl(del, meta).setId(qid);
            String[] hints = meta.getHintKeys();
            Object[] values = meta.getHintValues();
            for (int i = 0; i < hints.length; i++)
                q.setHint(hints[i], values[i]);

            // Restore JPA-level properties from addNamedQuery (JPA 3.2)
            if (meta.getFlushType() != -1) {
                q.setFlushMode(fromFlushBeforeQueries(meta.getFlushType()));
            }
            if (meta.getMaxResults() != -1) {
                q.setMaxResults(meta.getMaxResults());
            }
            if (meta.getLockMode() != null) {
                q.setLockMode(jakarta.persistence.LockModeType.valueOf(
                    meta.getLockMode()));
            }

            return q;
        } catch (RuntimeException re) {
            throw translateException(re);
        }
    }

    @Override
    public OpenJPAQuery createNativeQuery(String query) {
        validateSQL(query);
        return createQuery(QueryLanguages.LANG_SQL, query);
    }

    @Override
    public OpenJPAQuery createNativeQuery(String query, Class cls) {
        checkTuple(cls);
        return createNativeQuery(query).setResultClass(cls);
    }

    @Override
    public OpenJPAQuery createNativeQuery(String query, String mappingName) {
        assertNotCloseInvoked();
        validateSQL(query);
        org.apache.openjpa.kernel.Query kernelQuery = _broker.newQuery(
            QueryLanguages.LANG_SQL, query);
        kernelQuery.setResultMapping(null, mappingName);
        return newQueryImpl(kernelQuery, null);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        assertNotCloseInvoked();
        try {
            QueryMetaData meta = getQueryMetadata(name);
            if (!(meta instanceof MultiQueryMetaData)) {
                throw new RuntimeException(name + " is not an identifier for a Stored Procedure Query");
            }
            return newProcedure(((MultiQueryMetaData) meta).getProcedureName(), (MultiQueryMetaData) meta);
        } catch (RuntimeException re) {
            throw translateException(re);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        try {
            return newProcedure(procedureName, null);
        } catch (PersistenceException pe) {
            throw new IllegalArgumentException(pe.getMessage(), pe);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        String tempName = "StoredProcedure-"+System.nanoTime();
        MultiQueryMetaData meta = new MultiQueryMetaData(null, tempName, procedureName, true);
        for (Class<?> res : resultClasses) {
            meta.addComponent(res);
        }
        try {
            return newProcedure(procedureName, meta);
        } catch (PersistenceException pe) {
            throw new IllegalArgumentException(pe.getMessage(), pe);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        String tempName = "StoredProcedure-"+System.nanoTime();
        MultiQueryMetaData meta = new MultiQueryMetaData(null, tempName, procedureName, true);
        for (String mapping : resultSetMappings) {
            meta.addComponent(mapping);
        }
        return newProcedure(procedureName, meta);
    }

    /**
     * Creates a query to execute a Stored Procedure.
     * <br>
     * Construction of a {@link StoredProcedureQuery} object is a three step process
     * <LI>
     * <LI>a {@link org.apache.openjpa.kernel.Query kernel query} {@code kQ} is created for
     * {@link QueryLanguages#LANG_SQL SQL} language with the string {@code S}
     * <LI>a {@link QueryImpl facade query} {@code fQ} is created that delegates to the kernel query {@code kQ}
     * <LI>a {@link StoredProcedureQueryImpl stored procedure query} is created that delegates to the facade query
     * {@code fQ}.
     * <br>
     *
     */
    private StoredProcedureQuery newProcedure(String procedureName, MultiQueryMetaData meta) {
        org.apache.openjpa.kernel.QueryImpl kernelQuery = (org.apache.openjpa.kernel.QueryImpl)
                _broker.newQuery(QueryLanguages.LANG_STORED_PROC, procedureName);
        kernelQuery.getStoreQuery().setQuery(meta);
        if (meta != null) {
            getConfiguration().getMetaDataRepositoryInstance().addQueryMetaData(meta);
            kernelQuery.setResultMapping(null, meta.getResultSetMappingName());
        }
        return new StoredProcedureQueryImpl(procedureName, meta, new QueryImpl(this, _ret, kernelQuery, meta));
    }

    protected <T> QueryImpl<T> newQueryImpl(org.apache.openjpa.kernel.Query kernelQuery, QueryMetaData qmd) {
        return new QueryImpl<>(this, _ret, kernelQuery, qmd);
    }

    /**
     * @Deprecated -- Use org.apache.openjpa.persistence.EntityManagerImpl.newQueryImpl(Query kernelQuery, QueryMetaData
     *             qmd)
     * <br>
     *             Leave this method here as extenders of OpenJPA might depend on this hook to allow interception of
     *             query creation
     */
    protected <T> QueryImpl<T> newQueryImpl(org.apache.openjpa.kernel.Query kernelQuery) {
        return new QueryImpl<>(this, _ret, kernelQuery, null);
    }

    /**
     * Validate that the user provided SQL.
     */
    protected void validateSQL(String query) {
        if (StringUtil.trimToNull(query) == null)
            throw new ArgumentException(_loc.get("no-sql"), null, null, false);
    }

    PreparedQueryCache getPreparedQueryCache() {
        return _broker.getCachePreparedQuery() ?
            getConfiguration().getQuerySQLCacheInstance() : null;
    }

    /**
     * Gets the prepared query cached by the given key.
     *
     * @return the cached PreparedQuery or null if none exists.
     */
    PreparedQuery getPreparedQuery(String id) {
        PreparedQueryCache cache = getPreparedQueryCache();
        return (cache == null) ? null : cache.get(id);
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        assertNotCloseInvoked();
        _broker.assertOpen();
        _broker.getFetchConfiguration().setFlushBeforeQueries
            (toFlushBeforeQueries(flushMode));
        properties = null;
    }

    @Override
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

    /**
     * Used by Java EE Containers that wish to pool OpenJPA EntityManagers.  The specification
     * doesn't allow the closing of connections with the clear() method.  By introducing this
     * new method, we can do additional processing (and maybe more efficient processing) to
     * properly prepare an EM for pooling.
     *
     * @deprecated - use {@link clear()} instead.
     */
    @Deprecated
    public void prepareForPooling() {
        assertNotCloseInvoked();
        clear();
        // Do not close connection if ConnectionRetainMode is set to Always...
        if (getConnectionRetainMode() != ConnectionRetainMode.ALWAYS) {
            _broker.lock();  // since this direct close path is not protected...
            try {
                _broker.getStoreManager().close();
            } finally {
                _broker.unlock();
            }
        }
    }

    @Override
    public void clear() {
        assertNotCloseInvoked();
        _broker.detachAll(this, false);
        _plans.clear();
    }

    @Override
    public Object getDelegate() {
        try {
            _broker.assertOpen();
            assertNotCloseInvoked();
        } catch (RuntimeException re) {
            throw translateException(re);
        }
        return this;
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        assertNotCloseInvoked();
        _broker.assertActiveTransaction();
        assertValidAttchedEntity(GET_LOCK_MODE, entity);
        return MixedLockLevelsHelper.fromLockLevel(
            _broker.getLockLevel(entity));
    }

    @Override
    public void lock(Object entity, LockModeType mode) {
        lock(entity, mode, -1);
    }

    @Override
    public void lock(Object entity) {
        assertNotCloseInvoked();
        assertValidAttchedEntity(LOCK, entity);
        _broker.lock(entity, this);
    }

    @Override
    public void lock(Object entity, LockModeType mode, int timeout) {
        assertNotCloseInvoked();
        assertValidAttchedEntity(LOCK, entity);

        configureCurrentFetchPlan(pushFetchPlan(), null, mode, false);
        try {
            _broker.lock(entity, MixedLockLevelsHelper.toLockLevel(mode),  timeout, this);
        } finally {
            popFetchPlan();
        }
    }

    @Override
    public void lock(Object entity, LockModeType mode, Map<String, Object> properties) {
        assertNotCloseInvoked();
        assertValidAttchedEntity(LOCK, entity);
        _broker.assertActiveTransaction();
        properties = cloneProperties(properties);
        configureCurrentCacheModes(pushFetchPlan(), properties);
        configureCurrentFetchPlan(getFetchPlan(), properties, mode, false);
        try {
            _broker.lock(entity, MixedLockLevelsHelper.toLockLevel(mode),
                _broker.getFetchConfiguration().getLockTimeout(), this);
        } finally {
            popFetchPlan();
        }
    }

    @Override
    public void lockAll(Collection entities) {
        assertNotCloseInvoked();
        _broker.lockAll(entities, this);
    }

    @Override
    public void lockAll(Collection entities, LockModeType mode, int timeout) {
        assertNotCloseInvoked();
        _broker.lockAll(entities, MixedLockLevelsHelper.toLockLevel(mode),
            timeout, this);
    }

    @Override
    public void lockAll(Object... entities) {
        lockAll(Arrays.asList(entities));
    }

    @Override
    public void lockAll(Object[] entities, LockModeType mode, int timeout) {
        lockAll(Arrays.asList(entities), mode, timeout);
    }

    @Override
    public boolean cancelAll() {
        return _broker.cancelAll();
    }

    @Override
    public Object getConnection() {
        return _broker.getConnection();
    }

    @Override
    public Collection getManagedObjects() {
        return _broker.getManagedObjects();
    }

    @Override
    public Collection getTransactionalObjects() {
        return _broker.getTransactionalObjects();
    }

    @Override
    public Collection getPendingTransactionalObjects() {
        return _broker.getPendingTransactionalObjects();
    }

    @Override
    public Collection getDirtyObjects() {
        return _broker.getDirtyObjects();
    }

    @Override
    public boolean getOrderDirtyObjects() {
        return _broker.getOrderDirtyObjects();
    }

    @Override
    public void setOrderDirtyObjects(boolean order) {
        assertNotCloseInvoked();
        _broker.setOrderDirtyObjects(order);
    }

    @Override
    public void dirtyClass(Class cls) {
        assertNotCloseInvoked();
        _broker.dirtyType(cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Class> getPersistedClasses() {
        return (Collection<Class>) _broker.getPersistedTypes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Class> getUpdatedClasses() {
        return (Collection<Class>) _broker.getUpdatedTypes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Class> getRemovedClasses() {
        return (Collection<Class>) _broker.getDeletedTypes();
    }

    @Override
    public <T> T createInstance(Class<T> cls) {
        assertNotCloseInvoked();
        return (T) _broker.newInstance(cls);
    }

    @Override
    public void close() {
        assertNotCloseInvoked();
        Log log = _emf.getConfiguration().getLog(OpenJPAConfiguration.LOG_RUNTIME);
        if (log.isTraceEnabled()) {
            log.trace(this + ".close() invoked.");
        }
        _broker.close();
        _plans.clear();
    }

    @Override
    public boolean isOpen() {
        return !_broker.isCloseInvoked();
    }

    @Override
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

    @Override
    public Object getObjectId(Object o) {
        assertNotCloseInvoked();
        return JPAFacadeHelper.fromOpenJPAObjectId(_broker.getObjectId(o));
    }

    @Override
    public boolean isDirty(Object o) {
        assertNotCloseInvoked();
        return _broker.isDirty(o);
    }

    @Override
    public boolean isTransactional(Object o) {
        assertNotCloseInvoked();
        return _broker.isTransactional(o);
    }

    @Override
    public boolean isPersistent(Object o) {
        assertNotCloseInvoked();
        return _broker.isPersistent(o);
    }

    @Override
    public boolean isNewlyPersistent(Object o) {
        assertNotCloseInvoked();
        return _broker.isNew(o);
    }

    @Override
    public boolean isRemoved(Object o) {
        assertNotCloseInvoked();
        return _broker.isDeleted(o);
    }

    @Override
    public boolean isDetached(Object entity) {
        assertNotCloseInvoked();
        return _broker.isDetached(entity);
    }

    @Override
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
    protected void assertNotCloseInvoked() {
        if (_broker.isClosed() || _broker.isCloseInvoked()) {
            _closedMethodCall = true;
            throw new InvalidStateException(_loc.get("close-invoked"), null,
                null, true);
        }
    }

    /**
     * Mark the transaction for rollback if active. Called before throwing
     * exceptions from EM methods per JPA spec section 3.3.7.1.
     */
    public void markRollbackOnException(RuntimeException ex) {
        try {
            if (_broker.isActive()) {
                _broker.setRollbackOnly(ex);
            }
        } catch (Exception ignore) {
            // broker may be closed
        }
    }

    /**
     * Throw IllegalArgumentExceptionif if entity is not a valid entity or
     * if it is detached.
     */
    void assertValidAttchedEntity(String call, Object entity) {
        OpenJPAStateManager sm = _broker.getStateManager(entity);
        if (sm == null || !sm.isPersistent() || sm.isDetached() || (call.equals(REFRESH) && sm.isDeleted())) {
            throw translateException(new IllegalArgumentException(_loc.get("invalid_entity_argument",
                call, entity == null ? "null" : Exceptions.toString(entity)).getMessage()));
        }
    }

    ////////////////////////////////
    // FindCallbacks implementation
    ////////////////////////////////

    @Override
    public Object processArgument(Object arg) {
        return arg;
    }

    @Override
    public Object processReturn(Object oid, OpenJPAStateManager sm) {
        return (sm == null || sm.isDeleted()) ? null : sm.getManagedInstance();
    }

    //////////////////////////////
    // OpCallbacks implementation
    //////////////////////////////

    @Override
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
            case OP_DETACH:
                if (sm == null || !sm.isPersistent() || sm.isDetached())
                    return ACT_NONE;
                break;
        }
        return ACT_RUN | ACT_CASCADE;
    }

    @Override
    public int hashCode() {
        return (_broker == null) ? 0 : _broker.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if ((other == null) || (other.getClass() != this.getClass()))
            return false;
        if (_broker == null)
            return false;
        return _broker.equals(((EntityManagerImpl) other)._broker);
    }

    @Override
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        try {
            _ret = PersistenceExceptions.getRollbackTranslator(this);

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
                re = _ret.translate(re);
            } catch (Exception e) {
                // ignore
            }
            throw re;
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            // this requires that only AbstractBrokerFactory-sourced
            // brokers can be serialized
            Object factoryKey = ((AbstractBrokerFactory) _broker
                .getBrokerFactory()).getPoolKey();
            out.writeObject(factoryKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream innerOut = new ObjectOutputStream(baos);
            _broker.getDelegate().putUserObject(JPAFacadeHelper.EM_KEY, null);
            innerOut.writeObject(_broker.getDelegate());
            innerOut.flush();
            out.writeObject(baos.toByteArray());
        } catch (RuntimeException re) {
            try {
                re = _ret.translate(re);
            } catch (Exception e) {
                // ignore
            }
            throw re;
        }
    }

    public void setProperties(final Map<String, Object> emEmptyPropsProperties) {
        this.properties = emEmptyPropsProperties;
    }

    private static class BrokerBytesInputStream extends ObjectInputStream {

        private final OpenJPAConfiguration conf;

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

        @Override
        protected Class<?> resolveClass(ObjectStreamClass classDesc)
            throws IOException, ClassNotFoundException {

            String cname = BlacklistClassResolver.DEFAULT.check(classDesc.getName());
            if (cname.startsWith("[")) {
                // An array
                Class<?> component;		// component class
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
                int[] dim = new int[dcount];
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
        private Class<?> lookupClass(String className)
            throws ClassNotFoundException {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (PCEnhancer.isPCSubclassName(className)) {
                    String superName = PCEnhancer.toManagedTypeName(className);
                    ClassMetaData[] metas = conf.getMetaDataRepositoryInstance()
                        .getMetaDatas();
                    for (ClassMetaData meta : metas) {
                        if (superName.equals(
                                meta.getDescribedType().getName())) {
                            return PCRegistry.getPCType(
                                    meta.getDescribedType());
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

    @Override
    public void detach(Object entity) {
        if (entity == null)
            throw new IllegalArgumentException(_loc.get("null-detach").getMessage());
        assertNotCloseInvoked();
        // Per JPA spec, detach should throw IAE if the argument is not an entity
        ClassMetaData meta = _broker.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData(entity.getClass(), null, false);
        if (meta == null) {
            throw translateException(new IllegalArgumentException(
                _loc.get("not-entity", entity.getClass()).getMessage()));
        }
        _broker.detach(entity, this);
    }

    /**
     * Create a query from the given CritriaQuery.
     * Compile to register the parameters in this query.
     */
    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        assertNotCloseInvoked();
        try {
            ((OpenJPACriteriaQuery<T>) criteriaQuery).compile();

            // Snapshot the CriteriaQuery state so that subsequent modifications
            // to the original CriteriaQuery do not affect this query (JPA spec).
            Object snapshot = CriteriaBuilderImpl.snapshotQuery(criteriaQuery);

            org.apache.openjpa.kernel.Query kernelQuery = _broker.newQuery(OpenJPACriteriaBuilder.LANG_CRITERIA,
                snapshot);

            QueryImpl<T> facadeQuery = newQueryImpl(kernelQuery, null).setId(criteriaQuery.toString());
            Set<ParameterExpression<?>> params = criteriaQuery.getParameters();

            for (ParameterExpression<?> param : params) {
                facadeQuery.declareParameter(param, param);
            }
            return facadeQuery;
        } catch (IllegalStateException ise) {
            // Per JPA spec, createQuery(CriteriaQuery) throws IAE for invalid criteria
            IllegalArgumentException iae = new IllegalArgumentException(ise.getMessage(), ise);
            markRollbackOnException(iae);
            throw iae;
        } catch (RuntimeException re) {
            markRollbackOnException(re);
            throw translateException(re);
        }
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        assertNotCloseInvoked();
        try {
            // Snapshot the CriteriaUpdate state so that subsequent modifications
            // to the original CriteriaUpdate do not affect this query (JPA spec).
            Object snapshot = CriteriaBuilderImpl.snapshotQuery(updateQuery);

            org.apache.openjpa.kernel.Query kernelQuery =
                _broker.newQuery(OpenJPACriteriaBuilder.LANG_CRITERIA, snapshot);

            QueryImpl<?> facadeQuery = newQueryImpl(kernelQuery, null).setId(updateQuery.toString());
            Set<ParameterExpression<?>> params = updateQuery.getParameters();
            for (ParameterExpression<?> param : params) {
                facadeQuery.declareParameter(param, param);
            }
            return facadeQuery;
        } catch (RuntimeException re) {
            markRollbackOnException(re);
            throw translateException(re);
        }
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        assertNotCloseInvoked();
        try {
            // Snapshot the CriteriaDelete state so that subsequent modifications
            // to the original CriteriaDelete do not affect this query (JPA spec).
            Object snapshot = CriteriaBuilderImpl.snapshotQuery(deleteQuery);

            org.apache.openjpa.kernel.Query kernelQuery =
                _broker.newQuery(OpenJPACriteriaBuilder.LANG_CRITERIA, snapshot);

            QueryImpl<?> facadeQuery = newQueryImpl(kernelQuery, null).setId(deleteQuery.toString());
            Set<ParameterExpression<?>> params = deleteQuery.getParameters();
            for (ParameterExpression<?> param : params) {
                facadeQuery.declareParameter(param, param);
            }
            return facadeQuery;
        } catch (RuntimeException re) {
            markRollbackOnException(re);
            throw translateException(re);
        }
    }

    @Override
    public OpenJPAQuery createDynamicQuery(
        org.apache.openjpa.persistence.query.QueryDefinition qdef) {
        String jpql = _emf.getDynamicQueryBuilder().toJPQL(qdef);
        return createQuery(jpql);
    }

    /**
     * Get the properties used currently by this entity manager.
     * The property keys and their values are harvested from kernel artifacts namely
     * the Broker and FetchPlan by reflection.
     * These property keys and values that denote the bean properties/values of the kernel artifacts
     * are converted to the original keys/values that user used to set the properties.
     *
     */
    @Override
    public Map<String, Object> getProperties() {
        if (properties != null) {
            return properties;
        }
        Map<String,Object> props = _broker.getProperties();
        for (String s : _broker.getSupportedProperties()) {
            String kernelKey = getBeanPropertyName(s);
            Method getter = Reflection.findGetter(this.getClass(), kernelKey, false);
            if (getter != null) {
                String userKey = JPAProperties.getUserName(kernelKey);
                Object kvalue  = Reflection.get(this, getter);
                props.put(userKey.equals(kernelKey) ? s : userKey, JPAProperties.convertToUserValue(userKey, kvalue));
            }
        }
        FetchPlan fetch = getFetchPlan();
        Class<?> fetchType = fetch.getClass();
        Set<String> fProperties = Reflection.getBeanStylePropertyNames(fetchType);
        for (String s : fProperties) {
            String kernelKey = getBeanPropertyName(s);
            Method getter = Reflection.findGetter(fetchType, kernelKey, false);
            if (getter != null) {
                String userKey = JPAProperties.getUserName(kernelKey);
                Object kvalue  = Reflection.get(fetch, getter);
                props.put(userKey.equals(kernelKey) ? s : userKey, JPAProperties.convertToUserValue(userKey, kvalue));
            }
        }
        properties = props; // no need to synchronize, we don't care of the actual ref, we just want it as value
        return props;
    }

    @Override
    public OpenJPACriteriaBuilder getCriteriaBuilder() {
        try {
            assertNotCloseInvoked();
        } catch (RuntimeException re) {
            throw translateException(re);
        }
        return _emf.getCriteriaBuilder();
    }

    @Override
    public Set<String> getSupportedProperties() {
        return _broker.getSupportedProperties();
    }

    /**
     * Unwraps this receiver to an instance of the given class, if possible.
     *
     * @exception if the given class is null, generic <code>Object.class</code> or a class
     * that is not wrapped by this receiver.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> cls) {
        if (cls != null && cls != Object.class) {
            Object[] delegates = new Object[] { _broker.getInnermostDelegate(), _broker.getDelegate(), _broker, this };
            for (Object o : delegates) {
                if (cls.isInstance(o))
                    return (T) o;
            }
            // Only call getConnection() once we are certain that is the type that we need to unwrap.
            if (cls.isAssignableFrom(Connection.class)) {
                Object o = getConnection();
                if(o instanceof Connection){
                    return (T) o;
                }else{
                    // Try and cleanup if  aren't going to return the connection back to the caller.
                    ImplHelper.close(o);
                }
            }
        }
        // Set this transaction to rollback only (as per spec) here because the raised exception
        // does not go through normal exception translation pathways
        RuntimeException ex = new PersistenceException(_loc.get("unwrap-em-invalid", cls).toString(), null,
                this, false);
        if (isActive())
            setRollbackOnly(ex);
        throw ex;
    }

    @Override
    public void setQuerySQLCache(boolean flag) {
        _broker.setCachePreparedQuery(flag);
    }

    @Override
    public boolean getQuerySQLCache() {
        return _broker.getCachePreparedQuery();
    }

    RuntimeExceptionTranslator getExceptionTranslator() {
        return _ret;
    }

    /**
     * Populate the given FetchPlan with the given properties.
     * Optionally overrides the given lock mode.
     */
    private void configureCurrentFetchPlan(FetchPlan fetch, Map<String, Object> properties,
            LockModeType lock, boolean requiresTxn) {
        // handle properties in map first
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equals(JPAProperties.LOCK_SCOPE)) {
                    fetch.setLockScope((PessimisticLockScope)value);
                } else
                    fetch.setHint(key, value);
            }
        }
        // override with the specific lockMode, if needed.
        if (lock != null && lock != LockModeType.NONE) {
            if (requiresTxn) {
                _broker.assertActiveTransaction();
            }
            // Override read lock level
            LockModeType curReadLockMode = fetch.getReadLockMode();
            if (lock != curReadLockMode)
                fetch.setReadLockMode(lock);
        }
    }

    /**
     * Populate the fetch configuration with specified cache mode properties.
     * The cache mode properties modify the fetch configuration and remove those
     * properties. This method should be called <em>before</em> the fetch configuration of the current
     * context has been pushed.
     * @param fetch the fetch configuration of the current context. Not the
     * new configuration pushed (and later popped) during a single operation.
     *
     * @param properties
     */
    private void configureCurrentCacheModes(FetchPlan fetch, Map<String, Object> properties) {
        if (properties == null)
            return;
        CacheRetrieveMode rMode = JPAProperties.getEnumValue(CacheRetrieveMode.class,
                JPAProperties.CACHE_RETRIEVE_MODE, properties);
        if (rMode != null) {
        	fetch.setCacheRetrieveMode(DataCacheRetrieveMode.valueOf(rMode.toString().trim().toUpperCase(Locale.ENGLISH)));
            properties.remove(JPAProperties.CACHE_RETRIEVE_MODE);
        }
        CacheStoreMode sMode = JPAProperties.getEnumValue(CacheStoreMode.class,
                JPAProperties.CACHE_STORE_MODE, properties);
        if (sMode != null) {
            fetch.setCacheStoreMode(DataCacheStoreMode.valueOf(sMode.toString().trim().toUpperCase(Locale.ENGLISH)));
            properties.remove(JPAProperties.CACHE_STORE_MODE);
        }
    }

    @Override
    public Metamodel getMetamodel() {
        try {
            assertNotCloseInvoked();
        } catch (RuntimeException re) {
            throw translateException(re);
        }
        return _emf.getMetamodel();
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        assertNotCloseInvoked();
        MetamodelImpl mm = _emf.getMetamodel();
        if (mm.entity(rootType) == null) {
            throw new IllegalArgumentException(
                rootType.getName() + " is not a managed entity type");
        }
        return new EntityGraphImpl<>(rootType, mm);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        assertNotCloseInvoked();
        EntityGraphImpl<?> named = _emf.getEntityGraphImpl(graphName);
        if (named == null) {
            return null;
        }
        return named.copy();
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        assertNotCloseInvoked();
        EntityGraphImpl<?> eg = _emf.getEntityGraphImpl(graphName);
        if (eg == null) {
            throw new IllegalArgumentException(
                "No EntityGraph found with name: " + graphName);
        }
        return eg;
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        assertNotCloseInvoked();
        MetamodelImpl mm = _emf.getMetamodel();
        if (mm.entity(entityClass) == null) {
            throw new IllegalArgumentException(
                entityClass.getName() + " is not a managed entity type");
        }
        return _emf.getEntityGraphsForType(entityClass);
    }

    /**
     * Sets the given property to the given value, reflectively.
     *
     * The property key is transposed to a bean-style property.
     * The value is converted to a type consumable by the kernel.
     * After requisite transformation, if the value can not be set
     * on either this instance or its fetch plan by reflection,
     * then an warning message (not an exception as per JPA specification) is issued.
     */
    @Override
    public void setProperty(String prop, Object value) {
        assertNotCloseInvoked();
        properties = null;
        if (!setKernelProperty(this, prop, value)) {
            if (!setKernelProperty(this.getFetchPlan(), prop, value)) {
                Log log = getConfiguration().getLog(OpenJPAConfiguration.LOG_RUNTIME);
                if (log.isWarnEnabled()) {
                    log.warn(_loc.get("ignored-em-prop", prop, value == null ? "" : value.getClass()+":" + value));
                 }
            }
        }
    }

    /**
     * Attempt to set the given property and value to the given target instance.
     * The original property is transposed to a bean-style property name.
     * The original value is transformed to a type consumable by the target.
     *
     * @return if the property can be set to the given target.
     */
    private boolean setKernelProperty(Object target, String original, Object value) {
        String beanProp = getBeanPropertyName(original);
        JPAProperties.record(beanProp, original);
        Class<?> kType  = null;
        Object   kValue = null;
        Method setter = Reflection.findSetter(target.getClass(), beanProp, false);
        if (setter != null) {
            kType  = setter.getParameterTypes()[0];
            kValue = convertUserValue(original, value, kType);
            Reflection.set(target, setter, kValue);
            properties = null;
            return true;
        } else {
            Field field = Reflection.findField(target.getClass(), beanProp, false);
            if (field != null) {
                kType  = field.getType();
                kValue = convertUserValue(original, value, kType);
                Reflection.set(target, field, kValue);
                properties = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Extract a bean-style property name from the given string.
     * If the given string is <code>"a.b.xyz"</code> then returns <code>"xyz"</code>
     */
    String getBeanPropertyName(String user) {
        String result = user;
        if (JPAProperties.isValidKey(user)) {
            result = JPAProperties.getBeanProperty(user);
        } else {
            int dot = user.lastIndexOf('.');
            if (dot != -1)
                result = user.substring(dot+1);
        }
        return result;
    }


    /**
     * Convert the given value to a value consumable by OpenJPA kernel constructs.
     */
    Object convertUserValue(String key, Object value, Class<?> targetType) {
        if (JPAProperties.isValidKey(key))
            return JPAProperties.convertToKernelValue(targetType, key, value);
        if (value instanceof String val) {
            if ("null".equals(value)) {
                return null;
            } else {
                int parenIndex = val.indexOf('(');
                if (!String.class.equals(targetType) && (parenIndex > 0)) {
                    val = val.substring(0, parenIndex);
                }
                return StringUtil.parse(val, targetType);
            }
        } else if (value instanceof AutoDetachType) {
        	EnumSet<AutoDetachType> autoDetachFlags = EnumSet.noneOf(AutoDetachType.class);
        	autoDetachFlags.add((AutoDetachType)value);
        	return autoDetachFlags;
        } else if (value instanceof AutoDetachType[]) {
        	EnumSet<AutoDetachType> autoDetachFlags = EnumSet.noneOf(AutoDetachType.class);
        	autoDetachFlags.addAll(Arrays.asList((AutoDetachType[])value));
        	return autoDetachFlags;
        }
        return value;
    }

    private Map<String, Object> cloneProperties(Map<String, Object> properties) {
        if (properties != null) {
            properties = new HashMap<>(properties);
        }
        return properties;
    }

    private QueryMetaData getQueryMetadata(String name) {
        MetaDataRepository repos = _broker.getConfiguration().getMetaDataRepositoryInstance();
        QueryMetaData meta = repos.getQueryMetaData(null, name, _broker.getClassLoader(), true);
        if (meta == null) {
            throw new RuntimeException("No query named [" + name + "]");
        }
        return meta;
    }

	@Override
	public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getReference(T entity) {
		assertNotCloseInvoked();
		if (entity == null) {
			throw new IllegalArgumentException("entity is null");
		}
		// JPA 3.2: getReference(entity) extracts the entity class and PK,
		// then delegates to getReference(Class, Object) for lazy loading
		Class<T> entityClass = (Class<T>) entity.getClass();
		ClassMetaData meta = _broker.getConfiguration()
			.getMetaDataRepositoryInstance()
			.getMetaData(entityClass, null, false);
		if (meta == null) {
			throw new IllegalArgumentException(
				_loc.get("not-entity", entityClass).getMessage());
		}
		// Extract the primary key value from the entity instance
		FieldMetaData[] pkFields = meta.getPrimaryKeyFields();
		Object pk = null;
		if (pkFields.length == 1) {
			FieldMetaData pkField = pkFields[0];
			if (pkField.getBackingMember() instanceof Method) {
				try {
					pk = ((Method) pkField.getBackingMember()).invoke(entity);
				} catch (Exception e) {
					throw new IllegalArgumentException("Cannot extract PK from entity", e);
				}
			} else if (pkField.getBackingMember() instanceof Field f) {
				try {
                    f.setAccessible(true);
					pk = f.get(entity);
				} catch (Exception e) {
					throw new IllegalArgumentException("Cannot extract PK from entity", e);
				}
			}
		}
		return getReference(entityClass, pk);
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, LockOption... options) {
		Map<String, Object> properties = null;
		if (options != null) {
			properties = new HashMap<>();
			for (LockOption option : options) {
				if (option instanceof Timeout) {
					properties.put("jakarta.persistence.lock.timeout", ((Timeout) option).milliseconds());
				} else if (option instanceof PessimisticLockScope) {
					properties.put("jakarta.persistence.lock.scope", option);
				}
			}
		}
		lock(entity, lockMode, properties);
	}

	@Override
	public void refresh(Object entity, RefreshOption... options) {
		LockModeType lockMode = null;
		Map<String, Object> properties = null;
		if (options != null) {
			properties = new HashMap<>();
			for (RefreshOption option : options) {
				if (option instanceof Timeout) {
					properties.put("jakarta.persistence.lock.timeout", ((Timeout) option).milliseconds());
				} else if (option instanceof PessimisticLockScope) {
					properties.put("jakarta.persistence.lock.scope", option);
				} else if (option instanceof LockModeType) {
					lockMode = (LockModeType) option;
				} else if (option instanceof CacheStoreMode) {
					properties.put("jakarta.persistence.cache.storeMode", option);
				}
			}
		}
		refresh(entity, lockMode, properties);
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
    	return getFetchPlan().getCacheRetrieveMode() == DataCacheRetrieveMode.USE ? CacheRetrieveMode.USE : CacheRetrieveMode.BYPASS;
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
    	getFetchPlan().setCacheRetrieveMode(cacheRetrieveMode == CacheRetrieveMode.USE
    			? DataCacheRetrieveMode.USE : DataCacheRetrieveMode.BYPASS);
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		DataCacheStoreMode storeMode = switch (cacheStoreMode) {
			case USE: yield DataCacheStoreMode.USE;
			case REFRESH: yield DataCacheStoreMode.REFRESH;
			default: yield DataCacheStoreMode.BYPASS;
		};
		getFetchPlan().setCacheStoreMode(storeMode);
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
    	return switch (getFetchPlan().getCacheStoreMode()) {
	    	case USE: yield CacheStoreMode.USE;
	    	case REFRESH: yield CacheStoreMode.REFRESH;
	    	default: yield CacheStoreMode.BYPASS;
    	};
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
		if (selectQuery instanceof CriteriaQuery) {
			return createQuery((CriteriaQuery<T>) selectQuery);
		}

		org.apache.openjpa.persistence.criteria.CriteriaSelectImpl<T> setOp =
			(org.apache.openjpa.persistence.criteria.CriteriaSelectImpl<T>) selectQuery;
		setOp.compile();

		org.apache.openjpa.kernel.Query kernelQuery =
			_broker.newQuery(OpenJPACriteriaBuilder.LANG_CRITERIA, setOp);

		QueryImpl<T> facadeQuery = newQueryImpl(kernelQuery, null)
			.setId(selectQuery.toString());
		Set<ParameterExpression<?>> params = setOp.getParameters();
		for (ParameterExpression<?> param : params) {
			facadeQuery.declareParameter(param, param);
		}
		return facadeQuery;
	}

	@Override
	public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> void runWithConnection(ConnectionConsumer<C> action) {
		assertNotCloseInvoked();
		C connection = (C) getConnection();
		try {
			action.accept(connection);
		} catch (Exception e) {
			throw PersistenceExceptions.toPersistenceException(e);
		} finally {
			if (connection instanceof AutoCloseable) {
				try {
					((AutoCloseable) connection).close();
				} catch (Exception e) {
					// ignore close exceptions
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
		assertNotCloseInvoked();
		C connection = (C) getConnection();
		try {
			return function.apply(connection);
		} catch (Exception e) {
			throw PersistenceExceptions.toPersistenceException(e);
		} finally {
			if (connection instanceof AutoCloseable) {
				try {
					((AutoCloseable) connection).close();
				} catch (Exception e2) {
					// ignore close exceptions
				}
			}
		}
	}

	@Override
	public void setTimeout(Integer timeout) {
		assertNotCloseInvoked();
		if (timeout != null) {
			getFetchPlan().setQueryTimeout(timeout);
		}
	}

	@Override
	public Integer getTimeout() {
		assertNotCloseInvoked();
		int timeout = getFetchPlan().getQueryTimeout();
		return timeout > 0 ? timeout : null;
	}
	
}

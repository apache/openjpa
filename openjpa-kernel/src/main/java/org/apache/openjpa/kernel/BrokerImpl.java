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
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;
import javax.resource.cci.ResultSetInfo;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.map.IdentityMap;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.collections.set.MapBackedSet;
import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.event.CallbackModes;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.TransactionEvent;
import org.apache.openjpa.event.TransactionEventManager;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.ReferenceHashMap;
import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.lib.util.ReferenceMap;
import org.apache.openjpa.lib.util.concurrent.ReentrantLock;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.CallbackException;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.NoTransactionException;
import org.apache.openjpa.util.ObjectExistsException;
import org.apache.openjpa.util.ObjectId;
import org.apache.openjpa.util.ObjectNotFoundException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.OptimisticException;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;

/**
 * Concrete {@link Broker}. The broker handles object-level behavior,
 * but leaves all interaction with the data store to a {@link StoreManager}
 * that must be supplied at initialization.
 *
 * @author Abe White
 */
public class BrokerImpl
    implements Broker, FindCallbacks {

    /**
     * Incremental flush.
     */
    protected static final int FLUSH_INC = 0;

    /**
     * Flush in preparation of commit.
     */
    protected static final int FLUSH_COMMIT = 1;

    /**
     * Flush to check consistency of cache, then immediately rollback changes.
     */
    protected static final int FLUSH_ROLLBACK = 2;

    /**
     * Run persistence-by-reachability and other flush-time operations without
     * accessing the database.
     */
    protected static final int FLUSH_LOGICAL = 3;

    static final int STATUS_INIT = 0;
    static final int STATUS_TRANSIENT = 1;
    static final int STATUS_OID_ASSIGN = 2;
    static final int STATUS_COMMIT_NEW = 3;

    private static final int FLAG_ACTIVE = 2 << 0;
    private static final int FLAG_STORE_ACTIVE = 2 << 1;
    private static final int FLAG_CLOSE_INVOKED = 2 << 2;
    private static final int FLAG_PRESTORING = 2 << 3;
    private static final int FLAG_DEREFDELETING = 2 << 4;
    private static final int FLAG_FLUSHING = 2 << 5;
    private static final int FLAG_STORE_FLUSHING = 2 << 6;
    private static final int FLAG_FLUSHED = 2 << 7;
    private static final int FLAG_FLUSH_REQUIRED = 2 << 8;
    private static final int FLAG_REMOTE_LISTENER = 2 << 9;
    private static final int FLAG_RETAINED_CONN = 2 << 10;
    private static final int FLAG_TRANS_ENDING = 2 << 11;

    private static final Localizer _loc =
        Localizer.forPackage(BrokerImpl.class);

    //	the store manager in use; this may be a decorator such as a
    //	data cache store manager around the native store manager
    private DelegatingStoreManager _store = null;

    // ref to producing factory and configuration
    private AbstractBrokerFactory _factory = null;
    private OpenJPAConfiguration _conf = null;
    private Compatibility _compat = null;
    private FetchConfiguration _fc = null;
    private Log _log = null;
    private String _user = null;
    private String _pass = null;
    private ManagedRuntime _runtime = null;
    private LockManager _lm = null;
    private InverseManager _im = null;
    private final JCAHelper _jca = new JCAHelper();
    private ReentrantLock _lock = null;
    private OpCallbacks _call = null;
    private RuntimeExceptionTranslator _extrans = null;

    // cache class loader associated with the broker
    private ClassLoader _loader = Thread.currentThread().
        getContextClassLoader();

    // user state
    private Synchronization _sync = null;
    private Map _userObjects = null;

    // managed object caches
    private ManagedCache _cache = null;
    private TransactionalCache _transCache = null;
    private Set _transAdditions = null;
    private Set _derefCache = null;
    private Set _derefAdditions = null;
    private Map _loading = null;
    private Set _operating = null;
    private Set _persistedClss = null;
    private Set _updatedClss = null;
    private Set _deletedClss = null;
    private Set _pending = null;

    // track instances that become transactional after the first savepoint
    // (the first uses the transactional cache)
    private Set _savepointCache = null;
    private LinkedMap _savepoints = null;
    private SavepointManager _spm = null;

    // track open queries and extents so we can free their resources on close
    private ReferenceHashSet _queries = null;
    private ReferenceHashSet _extents = null;

    // track operation stack depth
    private int _operationCount = 0;

    // options
    private boolean _nontransRead = false;
    private boolean _nontransWrite = false;
    private boolean _retainState = false;
    private int _autoClear = CLEAR_DATASTORE;
    private int _restoreState = RESTORE_IMMUTABLE;
    private boolean _optimistic = false;
    private boolean _ignoreChanges = false;
    private boolean _multithreaded = false;
    private boolean _managed = false;
    private boolean _syncManaged = false;
    private int _connRetainMode = CONN_RETAIN_DEMAND;
    private boolean _evictDataCache = false;
    private boolean _populateDataCache = true;
    private boolean _largeTransaction = false;
    private int _autoDetach = 0;
    private int _detachState = DETACH_LOADED;
    private boolean _detachedNew = true;
    private int _callbackMode = CallbackModes.CALLBACK_IGNORE;
    private boolean _orderDirty = false;

    // status
    private int _flags = 0;
    private RuntimeException _closed = null;

    // event managers
    private TransactionEventManager _transEventManager = null;
    private LifecycleEventManager _lifeEventManager = null;

    /**
     * Set the persistence manager's authentication. This is the first
     * method called after construction.
     *
     * @param user the username this broker represents; used when pooling
     * brokers to make sure that a request to the factory for
     * a connection with an explicit user is delegated to a suitable broker
     * @param pass the password for the above user
     */
    public void setAuthentication(String user, String pass) {
        _user = user;
        _pass = pass;
    }

    /**
     * Initialize the persistence manager. This method is called
     * automatically by the factory before use.
     *
     * @param factory the factory used to create this broker
     * @param sm a concrete StoreManager implementation to
     * handle interaction with the data store
     * @param managed the transaction mode
     * @param connMode the connection retain mode
     */
    public void initialize(AbstractBrokerFactory factory,
        DelegatingStoreManager sm, boolean managed, int connMode) {
        _conf = factory.getConfiguration();
        _compat = _conf.getCompatibilityInstance();

        _factory = factory;
        _log = _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        _cache = new ManagedCache(newManagedObjectCache());
        _lifeEventManager = new LifecycleEventManager();
        _callbackMode = _conf.getMetaDataRepositoryInstance().
            getMetaDataFactory(). getDefaults().getCallbackMode();
        _connRetainMode = connMode;
        _managed = managed;
        if (managed)
            _runtime = _conf.getManagedRuntimeInstance();
        else
            _runtime = new LocalManagedRuntime(this);

        // setup default options
        _factory.configureBroker(this);
        _operating = MapBackedSet.decorate(new IdentityMap());

        // make sure to do this after configuring broker so that store manager
        // can look to broker configuration; we set both store and lock managers
        // before initializing them because they may each try to access the
        // other in thier initialization
        _store = sm;
        _lm = _conf.newLockManagerInstance();
        _im = _conf.newInverseManagerInstance();
        _spm = _conf.getSavepointManagerInstance();
        _store.setContext(this);
        _lm.setContext(this);

        if (_connRetainMode == CONN_RETAIN_ALWAYS)
            retainConnection();
        _fc = _store.newFetchConfiguration();
        _fc.setContext(this);

        // synch with the global transaction in progress, if any
        if (_factory.syncWithManagedTransaction(this, false))
            beginInternal();
    }

    /**
     * Close on finalize.
     */
    protected void finalize()
        throws Throwable {
        super.finalize();
        if (!isClosed())
            free();
    }

    /**
     * Create a {@link Map} to be used for the primary managed object cache.
     * Maps oids to state managers. By default, this creates a
     * {@link ReferenceMap} with soft values.
     */
    protected Map newManagedObjectCache() {
        return new ReferenceHashMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    //////////////////////////////////////////
    // Implementation of Connection interface
    //////////////////////////////////////////

    public ConnectionMetaData getMetaData()
        throws ResourceException {
        return _jca;
    }

    public Interaction createInteraction()
        throws ResourceException {
        return _jca;
    }

    public LocalTransaction getLocalTransaction()
        throws ResourceException {
        return this;
    }

    public ResultSetInfo getResultSetInfo()
        throws ResourceException {
        return _jca;
    }

    //////////////////////////////////
    // Implementation of StoreContext
    //////////////////////////////////

    public Broker getBroker() {
        return this;
    }

    //////////////
    // Properties
    //////////////

    public void setImplicitBehavior(OpCallbacks call,
        RuntimeExceptionTranslator ex) {
        if (_call == null)
            _call = call;
        if (_extrans == null)
            _extrans = ex;
    }

    RuntimeExceptionTranslator getInstanceExceptionTranslator() {
        return (_operationCount == 0) ? _extrans : null;
    }

    public BrokerFactory getBrokerFactory() {
        return _factory;
    }

    public OpenJPAConfiguration getConfiguration() {
        return _conf;
    }

    public FetchConfiguration getFetchConfiguration() {
        return _fc;
    }

    public int getConnectionRetainMode() {
        return _connRetainMode;
    }

    public boolean isManaged() {
        return _managed;
    }

    public ManagedRuntime getManagedRuntime() {
        return _runtime;
    }

    public ClassLoader getClassLoader() {
        return _loader;
    }

    public DelegatingStoreManager getStoreManager() {
        return _store;
    }

    public LockManager getLockManager() {
        return _lm;
    }

    public InverseManager getInverseManager() {
        return _im;
    }

    public String getConnectionUserName() {
        return _user;
    }

    public String getConnectionPassword() {
        return _pass;
    }

    public boolean getMultithreaded() {
        return _multithreaded;
    }

    public void setMultithreaded(boolean multithreaded) {
        assertOpen();
        _multithreaded = multithreaded;
        if (multithreaded && _lock == null)
            _lock = new ReentrantLock();
        else if (!multithreaded)
            _lock = null;
    }

    public boolean getIgnoreChanges() {
        return _ignoreChanges;
    }

    public void setIgnoreChanges(boolean val) {
        assertOpen();
        _ignoreChanges = val;
    }

    public boolean getNontransactionalRead() {
        return _nontransRead;
    }

    public void setNontransactionalRead(boolean val) {
        assertOpen();
        if ((_flags & FLAG_PRESTORING) != 0)
            throw new UserException(_loc.get("illegal-op-in-prestore"));

        // make sure the runtime supports it
        if (val && !_conf.supportedOptions().contains
            (_conf.OPTION_NONTRANS_READ))
            throw new UnsupportedException(_loc.get
                ("nontrans-read-not-supported"));

        _nontransRead = val;
    }

    public boolean getNontransactionalWrite() {
        return _nontransWrite;
    }

    public void setNontransactionalWrite(boolean val) {
        assertOpen();
        if ((_flags & FLAG_PRESTORING) != 0)
            throw new UserException(_loc.get("illegal-op-in-prestore"));

        _nontransWrite = val;
    }

    public boolean getOptimistic() {
        return _optimistic;
    }

    public void setOptimistic(boolean val) {
        assertOpen();
        if ((_flags & FLAG_ACTIVE) != 0)
            throw new InvalidStateException(_loc.get("trans-active",
                "Optimistic"));

        // make sure the runtime supports it
        if (val && !_conf.supportedOptions().contains(_conf.OPTION_OPTIMISTIC))
            throw new UnsupportedException(_loc.get
                ("optimistic-not-supported"));

        _optimistic = val;
    }

    public int getRestoreState() {
        return _restoreState;
    }

    public void setRestoreState(int val) {
        assertOpen();
        if ((_flags & FLAG_ACTIVE) != 0)
            throw new InvalidStateException(_loc.get("trans-active",
                "Restore"));

        _restoreState = val;
    }

    public boolean getRetainState() {
        return _retainState;
    }

    public void setRetainState(boolean val) {
        assertOpen();
        if ((_flags & FLAG_PRESTORING) != 0)
            throw new UserException(_loc.get("illegal-op-in-prestore"));
        _retainState = val;
    }

    public int getAutoClear() {
        return _autoClear;
    }

    public void setAutoClear(int val) {
        assertOpen();
        _autoClear = val;
    }

    public int getAutoDetach() {
        return _autoDetach;
    }

    public void setAutoDetach(int detachFlags) {
        assertOpen();
        _autoDetach = detachFlags;
    }

    public void setAutoDetach(int detachFlag, boolean on) {
        assertOpen();
        if (on)
            _autoDetach |= detachFlag;
        else
            _autoDetach &= ~detachFlag;
    }

    public int getDetachState() {
        return _detachState;
    }

    public void setDetachState(int mode) {
        assertOpen();
        _detachState = mode;
    }

    public boolean isDetachedNew() {
        return _detachedNew;
    }

    public void setDetachedNew(boolean isNew) {
        assertOpen();
        _detachedNew = isNew;
    }

    public boolean getSyncWithManagedTransactions() {
        return _syncManaged;
    }

    public void setSyncWithManagedTransactions(boolean sync) {
        assertOpen();
        _syncManaged = sync;
    }

    public boolean getEvictFromDataCache() {
        return _evictDataCache;
    }

    public void setEvictFromDataCache(boolean evict) {
        assertOpen();
        _evictDataCache = evict;
    }

    public boolean getPopulateDataCache() {
        return _populateDataCache;
    }

    public void setPopulateDataCache(boolean cache) {
        assertOpen();
        _populateDataCache = cache;
    }

    public boolean isLargeTransaction() {
        return _largeTransaction;
    }

    public void setLargeTransaction(boolean largeTransaction) {
        assertOpen();
        _largeTransaction = largeTransaction;
    }

    public Object getUserObject(Object key) {
        beginOperation(false);
        try {
            return (_userObjects == null) ? null : _userObjects.get(key);
        } finally {
            endOperation();
        }
    }

    public Object putUserObject(Object key, Object val) {
        beginOperation(false);
        try {
            if (val == null)
                return (_userObjects == null) ? null : _userObjects.remove(key);

            if (_userObjects == null)
                _userObjects = new HashMap();
            return _userObjects.put(key, val);
        } finally {
            endOperation();
        }
    }

    //////////
    // Events
    //////////

    public void addLifecycleListener(Object listener, Class[] classes) {
        beginOperation(false);
        try {
            _lifeEventManager.addListener(listener, classes);
        } finally {
            endOperation();
        }
    }

    public void removeLifecycleListener(Object listener) {
        beginOperation(false);
        try {
            _lifeEventManager.removeListener(listener);
        } finally {
            endOperation();
        }
    }

    /**
     * Give state managers access to the lifecycle event manager.
     */
    public LifecycleEventManager getLifecycleEventManager() {
        return _lifeEventManager;
    }

    /**
     * Fire lifecycle events, handling any exceptions appropriately.
     *
     * @return whether events are being processed at this time.
     */
    boolean fireLifecycleEvent(Object src, Object related, ClassMetaData meta,
        int eventType) {
        if (_lifeEventManager == null) // uninitialized
            return false;

        Exception[] exceps = _lifeEventManager.fireEvent(src, related, meta,
            eventType);
        if (exceps.length == 0
            || (_callbackMode & CallbackModes.CALLBACK_IGNORE) != 0)
            return true;

        OpenJPAException ke = new CallbackException
            (_loc.get("callback-err", meta)).
            setNestedThrowables(exceps).setFatal(true);
        if ((_callbackMode & CallbackModes.CALLBACK_ROLLBACK) != 0
            && (_flags & FLAG_ACTIVE) != 0)
            setRollbackOnlyInternal();
        if ((_callbackMode & CallbackModes.CALLBACK_LOG) != 0
            && _log.isWarnEnabled())
            _log.warn(ke);
        if ((_callbackMode & CallbackModes.CALLBACK_RETHROW) != 0)
            throw ke;
        return true;
    }

    public void addTransactionListener(Object tl) {
        beginOperation(false);
        try {
            if (_transEventManager == null)
                _transEventManager = new TransactionEventManager();

            _transEventManager.addListener(tl);
            if (tl instanceof RemoteCommitEventManager)
                _flags |= FLAG_REMOTE_LISTENER;
        } finally {
            endOperation();
        }
    }

    public void removeTransactionListener(Object tl) {
        beginOperation(false);
        try {
            if (_transEventManager != null
                && _transEventManager.removeListener(tl)
                && (tl instanceof RemoteCommitEventManager))
                _flags &= ~FLAG_REMOTE_LISTENER;
        } finally {
            endOperation();
        }
    }

    ///////////
    // Lookups
    ///////////

    public Object find(Object oid, boolean validate, FindCallbacks call) {
        int flags = OID_COPY | OID_ALLOW_NEW | OID_NODELETED;
        if (!validate)
            flags |= OID_NOVALIDATE;
        return find(oid, _fc, null, null, flags, call);
    }

    public Object find(Object oid, FetchConfiguration fetch, BitSet exclude,
        Object edata, int flags) {
        return find(oid, fetch, exclude, edata, flags, null);
    }

    /**
     * Internal finder.
     */
    protected Object find(Object oid, FetchConfiguration fetch, BitSet exclude,
        Object edata, int flags, FindCallbacks call) {
        if (call == null)
            call = this;
        oid = call.processArgument(oid);
        if (oid == null) {
            if ((flags & OID_NOVALIDATE) == 0)
                throw new ObjectNotFoundException(_loc.get("null-oid"));
            return call.processReturn(oid, null);
        }
        if (fetch == null)
            fetch = _fc;

        beginOperation(true);
        try {
            assertNontransactionalRead();

            // cached instance?
            StateManagerImpl sm = getStateManagerImplById(oid,
                (flags & OID_ALLOW_NEW) != 0 || (_flags & FLAG_FLUSHED) != 0);
            if (sm != null) {
                if (!requiresLoad(sm, true, edata, flags))
                    return call.processReturn(oid, sm);

                if (!sm.isLoading()) {
                    // make sure all the configured fields are loaded; do this
                    // after making instance transactional for locking
                    if (!sm.isTransactional() && useTransactionalState(fetch))
                        sm.transactional();
                    boolean loaded = sm.isLoading();
                    if (!loaded) {
                        try {
                            loaded = sm.load(fetch, StateManagerImpl.LOAD_FGS, 
                                exclude, edata, false);
                        } catch (ObjectNotFoundException onfe) {
                            if ((flags & OID_NODELETED) != 0
                                || (flags & OID_NOVALIDATE) != 0)
                                throw onfe;
                            return call.processReturn(oid, null);
                        }
                    }

                    // if no data needed to be loaded and the user wants to
                    // validate, just make sure the object exists
                    if (!loaded && (flags & OID_NOVALIDATE) == 0
                        && _compat.getValidateTrueChecksStore()
                        && !sm.isTransactional()
                        && !_store.exists(sm, edata)) {
                        if ((flags & OID_NODELETED) == 0)
                            return call.processReturn(oid, null);
                        throw new ObjectNotFoundException(_loc.get
                            ("del-instance", sm.getManagedInstance(), oid)).
                            setFailedObject(sm.getManagedInstance());
                    }
                }

                // since the object was cached, we may need to upgrade lock
                // if current level is higher than level of initial load
                if ((_flags & FLAG_ACTIVE) != 0) {
                    int level = fetch.getReadLockLevel();
                    _lm.lock(sm, level, fetch.getLockTimeout(), edata);
                    sm.readLocked(level, fetch.getWriteLockLevel());
                }
                return call.processReturn(oid, sm);
            }

            // if there's no cached sm for a new/transient id type, we
            // it definitely doesn't exist
            if (oid instanceof StateManagerId)
                return call.processReturn(oid, null);

            // initialize a new state manager for the datastore instance
            sm = newStateManagerImpl(oid, (flags & OID_COPY) != 0);
            boolean load = requiresLoad(sm, false, edata, flags);
            sm = initialize(sm, load, fetch, edata);
            if (sm == null) {
                if ((flags & OID_NOVALIDATE) != 0)
                    throw new ObjectNotFoundException(oid);
                return call.processReturn(oid, null);
            }

            // make sure all configured fields were loaded
            if (load) {
                try {
                    sm.load(fetch, StateManagerImpl.LOAD_FGS, exclude,
                        edata, false);
                } catch (ObjectNotFoundException onfe) {
                    if ((flags & OID_NODELETED) != 0
                        || (flags & OID_NOVALIDATE) != 0)
                        throw onfe;
                    return call.processReturn(oid, null);
                }
            }
            return call.processReturn(oid, sm);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Initialize a newly-constructed state manager.
     */
    protected StateManagerImpl initialize(StateManagerImpl sm, boolean load,
        FetchConfiguration fetch, Object edata) {
        if (!load) {
            sm.initialize(sm.getMetaData().getDescribedType(),
                PCState.HOLLOW);
        } else {
            PCState state = (useTransactionalState(fetch))
                ? PCState.PCLEAN : PCState.PNONTRANS;
            sm.setLoading(true);
            try {
                if (!_store.initialize(sm, state, fetch, edata))
                    return null;
            } finally {
                sm.setLoading(false);
            }
        }
        return sm;
    }

    public Object[] findAll(Collection oids, boolean validate,
        FindCallbacks call) {
        int flags = OID_COPY | OID_ALLOW_NEW | OID_NODELETED;
        if (!validate)
            flags |= OID_NOVALIDATE;
        return findAll(oids, _fc, null, null, flags, call);
    }

    public Object[] findAll(Collection oids, FetchConfiguration fetch,
        BitSet exclude, Object edata, int flags) {
        return findAll(oids, fetch, exclude, edata, flags, null);
    }

    /**
     * Internal finder.
     */
    protected Object[] findAll(Collection oids, FetchConfiguration fetch,
        BitSet exclude, Object edata, int flags, FindCallbacks call) {
        // throw any exceptions for null oids up immediately
        if (oids == null)
            throw new NullPointerException("oids == null");
        if ((flags & OID_NOVALIDATE) != 0 && oids.contains(null))
            throw new UserException(_loc.get("null-oids"));

        // we have to use a map of oid->sm rather than a simple
        // array, so that we make sure not to create multiple sms for equivalent
        // oids if the user has duplicates in the given array
        _loading = new HashMap((int) (oids.size() * 1.33 + 1));
        if (call == null)
            call = this;
        if (fetch == null)
            fetch = _fc;

        beginOperation(true);
        try {
            assertNontransactionalRead();

            // collection of state managers to pass to store manager
            List load = null;
            StateManagerImpl sm;
            boolean initialized;
            boolean transState = useTransactionalState(fetch);
            Object obj, oid;
            int idx = 0;
            for (Iterator itr = oids.iterator(); itr.hasNext(); idx++) {
                // if we've already seen this oid, skip repeats
                obj = itr.next();
                oid = call.processArgument(obj);
                if (oid == null || _loading.containsKey(obj))
                    continue;

                // if we don't have a cached instance or it is not transactional
                // and is hollow or we need to validate, load it
                sm = getStateManagerImplById(oid, (flags & OID_ALLOW_NEW) != 0
                    || (_flags & FLAG_FLUSHED) != 0);
                initialized = sm != null;
                if (!initialized)
                    sm = newStateManagerImpl(oid, (flags & OID_COPY) != 0);

                _loading.put(obj, sm);
                if (requiresLoad(sm, initialized, edata, flags)) {
                    transState = transState || useTransactionalState(fetch);
                    if (initialized && !sm.isTransactional() && transState)
                        sm.transactional();
                    if (load == null)
                        load = new ArrayList(oids.size() - idx);
                    load.add(sm);
                } else if (!initialized)
                    sm.initialize(sm.getMetaData().getDescribedType(),
                        PCState.HOLLOW);
            }

            // pass all state managers in need of loading or validation to the
            // store manager
            if (load != null) {
                PCState state = (transState) ? PCState.PCLEAN
                    : PCState.PNONTRANS;
                Collection failed = _store.loadAll(load, state,
                    StoreManager.FORCE_LOAD_NONE, fetch, edata);

                // set failed instances to null
                if (failed != null && !failed.isEmpty()) {
                    if ((flags & OID_NOVALIDATE) != 0)
                        throw newObjectNotFoundException(failed);
                    for (Iterator itr = failed.iterator(); itr.hasNext();)
                        _loading.put(itr.next(), null);
                }
            }

            // create results array; make sure all configured fields are
            // loaded in each instance
            Object[] results = new Object[oids.size()];
            boolean active = (_flags & FLAG_ACTIVE) != 0;
            int level = fetch.getReadLockLevel();
            idx = 0;
            for (Iterator itr = oids.iterator(); itr.hasNext(); idx++) {
                oid = itr.next();
                sm = (StateManagerImpl) _loading.get(oid);
                if (sm != null && requiresLoad(sm, true, edata, flags)) {
                    try {
                        sm.load(fetch, StateManagerImpl.LOAD_FGS,
                        	exclude, edata, false);
                        if (active) {
                            _lm.lock(sm, level, fetch.getLockTimeout(), edata);
                            sm.readLocked(level, fetch.getWriteLockLevel());
                        }
                    }
                    catch (ObjectNotFoundException onfe) {
                        if ((flags & OID_NODELETED) != 0
                            || (flags & OID_NOVALIDATE) != 0)
                            throw onfe;
                        sm = null;
                    }
                }
                results[idx] = call.processReturn(oid, sm);
            }
            return results;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            _loading = null;
            endOperation();
        }
    }

    /**
     * Return whether the given instance needs loading before being returned
     * to the user.
     */
    private boolean requiresLoad(OpenJPAStateManager sm, boolean initialized,
        Object edata, int flags) {
        if ((flags & OID_NOVALIDATE) == 0)
            return true;
        if (edata != null) // take advantage of existing result
            return true;
        if (initialized && sm.getPCState() != PCState.HOLLOW)
            return false;
        if (!initialized && sm.getMetaData().getPCSubclasses().length > 0)
            return true;
        return !_compat.getValidateFalseReturnsHollow();
    }

    /**
     * Return whether to use a transactional state.
     */
    private boolean useTransactionalState(FetchConfiguration fetch) {
        return (_flags & FLAG_ACTIVE) != 0 && (!_optimistic
            || _autoClear == CLEAR_ALL
            || fetch.getReadLockLevel() != LOCK_NONE);
    }

    public Object findCached(Object oid, FindCallbacks call) {
        if (call == null)
            call = this;
        oid = call.processArgument(oid);
        if (oid == null)
            return call.processReturn(oid, null);

        beginOperation(true);
        try {
            StateManagerImpl sm = getStateManagerImplById(oid, true);
            return call.processReturn(oid, sm);
        } finally {
            endOperation();
        }
    }

    public Class getObjectIdType(Class cls) {
        if (cls == null)
            return null;

        beginOperation(false);
        try {
            ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
                getMetaData(cls, _loader, false);
            if (meta == null
                || meta.getIdentityType() == ClassMetaData.ID_UNKNOWN)
                return null;
            if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION)
                return meta.getObjectIdType();

            return _store.getDataStoreIdType(meta);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public Object newObjectId(Class cls, Object val) {
        if (val == null)
            return null;

        beginOperation(false);
        try {
            ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
                getMetaData(cls, _loader, true);

            // delegate to store manager for datastore ids
            if (meta.getIdentityType() == ClassMetaData.ID_DATASTORE) {
                if (val instanceof String
                    && ((String) val).startsWith(StateManagerId.STRING_PREFIX))
                    return new StateManagerId((String) val);
                return _store.newDataStoreId(val, meta);
            } else if (meta.getIdentityType() == ClassMetaData.ID_UNKNOWN)
                throw new UserException(_loc.get("meta-unknownid", cls));

            if (val instanceof String
                && !_conf.getCompatibilityInstance().getStrictIdentityValues())
            {
                // bug #958: section 9.6 of the JDO 1.0.1 specification states
                // that a fatal internal exception should be thrown when
                // invoking this method on an abstract class
                if (Modifier.isAbstract(cls.getModifiers()))
                    throw new InternalException(_loc.get("objectid-abstract",
                        cls));
                return PCRegistry.newObjectId(cls, (String) val);
            }

            if (meta.getObjectIdType().isAssignableFrom(val.getClass())) {
                if (!meta.isOpenJPAIdentity() && meta.isObjectIdTypeShared())
                    return new ObjectId(cls, val);
                return val;
            }

            Object[] arr = (val instanceof Object[]) ? (Object[]) val
                : new Object[]{ val };
            return ApplicationIds.fromPKValues(arr, meta);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (ClassCastException cce) {
            throw new UserException(_loc.get("bad-id-value", val,
                val.getClass().getName(), cls)).setCause(cce);
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Create a new state manager for the given oid.
     */
    private StateManagerImpl newStateManagerImpl(Object oid, boolean copy) {
        // see if we're in the process of loading this oid in a loadAll call
        StateManagerImpl sm;
        if (_loading != null) {
            sm = (StateManagerImpl) _loading.get(oid);
            if (sm != null && sm.getPersistenceCapable() == null)
                return sm;
        }

        // find metadata for the oid
        Class pcType = _store.getManagedType(oid);
        MetaDataRepository repos = _conf.getMetaDataRepositoryInstance();
        ClassMetaData meta;
        if (pcType != null)
            meta = repos.getMetaData(pcType, _loader, true);
        else
            meta = repos.getMetaData(oid, _loader, true);

        // copy the oid if needed
        if (copy && _compat.getCopyObjectIds()) {
            if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION)
                oid = ApplicationIds.copy(oid, meta);
            else if (meta.getIdentityType() == ClassMetaData.ID_UNKNOWN)
                throw new UserException(_loc.get("meta-unknownid", meta));
            else
                oid = _store.copyDataStoreId(oid, meta);
        }

        sm = newStateManagerImpl(oid, meta);
        sm.setObjectId(oid);
        return sm;
    }

    /**
     * Create a state manager for the given oid and metadata.
     */
    protected StateManagerImpl newStateManagerImpl(Object oid,
        ClassMetaData meta) {
        return new StateManagerImpl(oid, meta, this);
    }

    ///////////////
    // Transaction
    ///////////////

    public void begin() {
        beginOperation(true);
        try {
            if ((_flags & FLAG_ACTIVE) != 0)
                throw new InvalidStateException(_loc.get("active"));
            _factory.syncWithManagedTransaction(this, true);
            beginInternal();
        } finally {
            endOperation();
        }
    }

    /**
     * Notify the store manager of a transaction.
     */
    private void beginInternal() {
        try {
            beginStoreManagerTransaction(_optimistic);
            _flags |= FLAG_ACTIVE;

            // start locking
            if (!_optimistic) {
                _fc.setReadLockLevel(_conf.getReadLockLevelConstant());
                _fc.setWriteLockLevel(_conf.getWriteLockLevelConstant());
                _fc.setLockTimeout(_conf.getLockTimeout());
            }
            _lm.beginTransaction();

            if (_transEventManager != null
                && _transEventManager.hasBeginListeners())
                _transEventManager.fireEvent(new TransactionEvent(this,
                    TransactionEvent.AFTER_BEGIN, null, null, null, null));
        } catch (OpenJPAException ke) {
            // if we already started the transaction, don't let it commit
            if ((_flags & FLAG_ACTIVE) != 0)
                setRollbackOnlyInternal();
            throw ke.setFatal(true);
        } catch (RuntimeException re) {
            // if we already started the transaction, don't let it commit
            if ((_flags & FLAG_ACTIVE) != 0)
                setRollbackOnlyInternal();
            throw new StoreException(re).setFatal(true);
        }

        if (_pending != null) {
            StateManagerImpl sm;
            for (Iterator it = _pending.iterator(); it.hasNext();) {
                sm = (StateManagerImpl) it.next();
                sm.transactional();
                if (sm.isDirty())
                    setDirty(sm, true);
            }
            _pending = null;
        }
    }

    public void beginStore() {
        beginOperation(true);
        try {
            assertTransactionOperation();
            if ((_flags & FLAG_STORE_ACTIVE) == 0)
                beginStoreManagerTransaction(false);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new StoreException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Begin a store manager transaction.
     */
    private void beginStoreManagerTransaction(boolean optimistic) {
        if (!optimistic) {
            retainConnection();
            _store.begin();
            _flags |= FLAG_STORE_ACTIVE;
        } else {
            if (_connRetainMode == CONN_RETAIN_TRANS)
                retainConnection();
            _store.beginOptimistic();
        }
    }

    /**
     * End the current store manager transaction. Throws an
     * exception to signal a forced rollback after failed commit, otherwise
     * returns any exception encountered during the end process.
     */
    private RuntimeException endStoreManagerTransaction(boolean rollback) {
        boolean forcedRollback = false;
        boolean releaseConn = false;
        RuntimeException err = null;
        try {
            if ((_flags & FLAG_STORE_ACTIVE) != 0) {
                releaseConn = _connRetainMode != CONN_RETAIN_ALWAYS;
                if (rollback)
                    _store.rollback();
                else
                    _store.commit();
            } else {
                releaseConn = _connRetainMode == CONN_RETAIN_TRANS;
                _store.rollbackOptimistic();
            }
        }
        catch (RuntimeException re) {
            if (!rollback) {
                forcedRollback = true;
                try { _store.rollback(); } catch (RuntimeException re2) {}
            }
            err = re;
        } finally {
            _flags &= ~FLAG_STORE_ACTIVE;
        }

        if (releaseConn) {
            try {
                releaseConnection();
            } catch (RuntimeException re) {
                if (err == null)
                    err = re;
            }
        }

        if (forcedRollback)
            throw err;
        return err;
    }

    public void commit() {
        beginOperation(false);
        try {
            assertTransactionOperation();

            javax.transaction.Transaction trans =
                _runtime.getTransactionManager().getTransaction();
            if (trans == null)
                throw new InvalidStateException(_loc.get("null-trans"));

            // this commit on the transaction will cause our
            // beforeCompletion method to be invoked
            trans.commit();
        } catch (OpenJPAException ke) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), ke);
            throw ke;
        } catch (Exception e) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), e);
            throw new StoreException(e);
        } finally {
            endOperation();
        }
    }

    public void rollback() {
        beginOperation(false);
        try {
            assertTransactionOperation();

            javax.transaction.Transaction trans =
                _runtime.getTransactionManager().getTransaction();
            if (trans != null)
                trans.rollback();
        } catch (OpenJPAException ke) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), ke);
            throw ke;
        } catch (Exception e) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), e);
            throw new StoreException(e);
        } finally {
            endOperation();
        }
    }

    public boolean syncWithManagedTransaction() {
        assertOpen();
        lock();
        try {
            if ((_flags & FLAG_ACTIVE) != 0)
                throw new InvalidStateException(_loc.get("active"));
            if (!_managed)
                throw new InvalidStateException(_loc.get("trans-not-managed"));
            if (_factory.syncWithManagedTransaction(this, false)) {
                beginInternal();
                return true;
            }
            return false;
        } finally {
            unlock();
        }
    }

    public void commitAndResume() {
        endAndResume(true);
    }

    public void rollbackAndResume() {
        endAndResume(false);
    }

    private void endAndResume(boolean commit) {
        beginOperation(false);
        try {
            if (commit)
                commit();
            else
                rollback();
            begin();
        } finally {
            endOperation();
        }
    }

    public boolean getRollbackOnly() {
        beginOperation(true);
        try {
            if ((_flags & FLAG_ACTIVE) == 0)
                return false;

            javax.transaction.Transaction trans =
                _runtime.getTransactionManager().getTransaction();
            if (trans == null)
                return false;
            return trans.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        } finally {
            endOperation();
        }
    }

    public void setRollbackOnly() {
        beginOperation(true);
        try {
            assertTransactionOperation();
            setRollbackOnlyInternal();
        } finally {
            endOperation();
        }
    }

    /**
     * Mark the current transaction as rollback-only.
     */
    private void setRollbackOnlyInternal() {
        try {
            javax.transaction.Transaction trans =
                _runtime.getTransactionManager().getTransaction();
            if (trans == null)
                throw new InvalidStateException(_loc.get("null-trans"));
            trans.setRollbackOnly();
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    public void setSavepoint(String name) {
        beginOperation(true);
        try {
            assertActiveTransaction();
            if (_savepoints != null && _savepoints.containsKey(name))
                throw new UserException(_loc.get("savepoint-exists", name));

            if ((_flags & FLAG_FLUSHED) != 0
                && !_spm.supportsIncrementalFlush())
                throw new UnsupportedException(_loc.get
                    ("savepoint-flush-not-supported"));

            OpenJPASavepoint save = _spm.newSavepoint(name, this);
            if (_savepoints == null || _savepoints.isEmpty()) {
                save.save(getTransactionalStates());
                _savepoints = new LinkedMap();
            } else {
                if (_savepointCache == null)
                    save.save(Collections.EMPTY_LIST);
                else {
                    save.save(_savepointCache);
                    _savepointCache.clear();
                }
            }
            _savepoints.put(name, save);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        } finally {
            endOperation();
        }
    }

    public void releaseSavepoint() {
        beginOperation(false);
        try {
            if (_savepoints == null || _savepoints.isEmpty())
                throw new UserException(_loc.get("no-lastsavepoint"));
            releaseSavepoint((String) _savepoints.get
                (_savepoints.size() - 1));
        } finally {
            endOperation();
        }
    }

    public void releaseSavepoint(String savepoint) {
        beginOperation(false);
        try {
            assertActiveTransaction();

            int index = (_savepoints == null) ? -1
                : _savepoints.indexOf(savepoint);
            if (index < 0)
                throw new UserException(_loc.get("no-savepoint", savepoint));

            // clear old in reverse
            OpenJPASavepoint save;
            while (_savepoints.size() > index + 1) {
                save = (OpenJPASavepoint) _savepoints.remove
                    (_savepoints.size() - 1);
                save.release(false);
            }

            save = (OpenJPASavepoint) _savepoints.remove(index);
            save.release(true);
            if (_savepointCache != null)
                _savepointCache.clear();
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        } finally {
            endOperation();
        }
    }

    public void rollbackToSavepoint() {
        beginOperation(false);
        try {
            if (_savepoints == null || _savepoints.isEmpty())
                throw new UserException(_loc.get("no-lastsavepoint"));
            rollbackToSavepoint((String) _savepoints.get
                (_savepoints.size() - 1));
        } finally {
            endOperation();
        }
    }

    public void rollbackToSavepoint(String savepoint) {
        beginOperation(false);
        try {
            assertActiveTransaction();

            int index = (_savepoints == null) ? -1
                : _savepoints.indexOf(savepoint);
            if (index < 0)
                throw new UserException(_loc.get("no-savepoint", savepoint));

            // clear old in reverse
            OpenJPASavepoint save;
            while (_savepoints.size() > index + 1) {
                save = (OpenJPASavepoint) _savepoints.remove
                    (_savepoints.size() - 1);
                save.release(false);
            }

            save = (OpenJPASavepoint) _savepoints.remove(index);
            Collection saved = save.rollback(_savepoints.values());
            if (_savepointCache != null)
                _savepointCache.clear();
            if (_transCache != null) {
                // build up a new collection of states
                TransactionalCache oldTransCache = _transCache;
                TransactionalCache newTransCache = new TransactionalCache
                    (_orderDirty);
                _transCache = null;

                // currently there is the assumption that incremental
                // flush is either a) not allowed, or b) required
                // pre-savepoint.  this solves a number of issues including
                // storing flushed states as well as OID handling.
                // if future plugins do not follow this, we need to cache
                // more info per state
                SavepointFieldManager fm;
                StateManagerImpl sm;
                for (Iterator itr = saved.iterator(); itr.hasNext();) {
                    fm = (SavepointFieldManager) itr.next();
                    sm = fm.getStateManager();
                    sm.rollbackToSavepoint(fm);
                    oldTransCache.remove(sm);
                    if (sm.isDirty())
                        newTransCache.addDirty(sm);
                    else
                        newTransCache.addClean(sm);
                }
                for (Iterator itr = oldTransCache.iterator(); itr.hasNext();) {
                    sm = (StateManagerImpl) itr.next();
                    sm.rollback();
                    removeFromTransaction(sm);
                }
                oldTransCache.clear();
                _transCache = newTransCache;
            }
        }
        catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        } finally {
            endOperation();
        }
    }

    public void flush() {
        beginOperation(true);
        try {
            // return silently if no trans is active, or if this is a reentrant
            // call, which can happen if the store manager tries to get an
            // auto-inc oid during flush
            if ((_flags & FLAG_ACTIVE) == 0
                || (_flags & FLAG_STORE_FLUSHING) != 0)
                return;

            // make sure the runtime supports it
            if (!_conf.supportedOptions().contains(_conf.OPTION_INC_FLUSH))
                throw new UnsupportedException(_loc.get
                    ("incremental-flush-not-supported"));
            if (_savepoints != null && !_savepoints.isEmpty()
                && !_spm.supportsIncrementalFlush())
                throw new UnsupportedException(_loc.get
                    ("savepoint-flush-not-supported"));

            try {
                flushSafe(FLUSH_INC);
                _flags |= FLAG_FLUSHED;
            } catch (OpenJPAException ke) {
                // rollback on flush error; objects may be in inconsistent state
                setRollbackOnly();
                throw ke.setFatal(true);
            } catch (RuntimeException re) {
                // rollback on flush error; objects may be in inconsistent state
                setRollbackOnly();
                throw new StoreException(re).setFatal(true);
            }
        }
        finally {
            endOperation();
        }
    }

    public void preFlush() {
        beginOperation(true);
        try {
            if ((_flags & FLAG_ACTIVE) != 0)
                flushSafe(FLUSH_LOGICAL);
        } finally {
            endOperation();
        }
    }

    public void validateChanges() {
        beginOperation(true);
        try {
            // if no trans, just return; if active datastore trans, flush
            if ((_flags & FLAG_ACTIVE) == 0)
                return;
            if ((_flags & FLAG_STORE_ACTIVE) != 0) {
                flush();
                return;
            }

            // make sure the runtime supports inc flush
            if (!_conf.supportedOptions().contains(_conf.OPTION_INC_FLUSH))
                throw new UnsupportedException(_loc.get
                    ("incremental-flush-not-supported"));

            try {
                flushSafe(FLUSH_ROLLBACK);
            } catch (OpenJPAException ke) {
                throw ke;
            } catch (RuntimeException re) {
                throw new StoreException(re);
            }
        }
        finally {
            endOperation();
        }
    }

    public boolean isActive() {
        beginOperation(true);
        try {
            return (_flags & FLAG_ACTIVE) != 0;
        } finally {
            endOperation();
        }
    }

    public boolean isStoreActive() {
        // we need to lock here, because we might be in the middle of an
        // atomic transaction process (e.g., commitAndResume)
        beginOperation(true);
        try {
            return (_flags & FLAG_STORE_ACTIVE) != 0;
        } finally {
            endOperation();
        }
    }

    /**
     * Return whether the current transaction is ending, i.e. in the 2nd phase
     * of a commit or rollback
     */
    boolean isTransactionEnding() {
        return (_flags & FLAG_TRANS_ENDING) != 0;
    }

    public boolean beginOperation(boolean syncTrans) {
        lock();
        try {
            assertOpen();

            if (syncTrans && _operationCount == 0 && _syncManaged
                && (_flags & FLAG_ACTIVE) == 0)
                syncWithManagedTransaction();
            return _operationCount++ == 1;
        } catch (OpenJPAException ke) {
            unlock();
            throw ke;
        } catch (RuntimeException re) {
            unlock();
            throw new GeneralException(re);
        }
    }

    /**
     * Mark the operation over. If outermost caller of stack, returns true
     * and will detach manageed instances if necessary.
     */
    public boolean endOperation() {
        try {
            if (_operationCount == 1 && (_autoDetach & DETACH_NONTXREAD) != 0
                && (_flags & FLAG_ACTIVE) == 0) {
                detachAllInternal(null);
            }
            if (_operationCount < 1)
                throw new InternalException();
            return _operationCount == 1;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            _operationCount--;
            if (_operationCount == 0)
                _operating.clear();
            unlock();
        }
    }

    public Synchronization getSynchronization() {
        return _sync;
    }

    public void setSynchronization(Synchronization sync) {
        assertOpen();
        _sync = sync;
    }

    ///////////////////////////////////////////////
    // Implementation of Synchronization interface
    ///////////////////////////////////////////////

    public void beforeCompletion() {
        beginOperation(false);
        try {
            // user-supplied synchronization
            if (_sync != null)
                _sync.beforeCompletion();

            flushSafe(FLUSH_COMMIT);
        } catch (OpenJPAException ke) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), ke);
            throw ke;
        } catch (RuntimeException re) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), re);
            throw new StoreException(re);
        } finally {
            endOperation();
        }
    }

    public void afterCompletion(int status) {
        beginOperation(false);
        try {
            assertActiveTransaction();

            _flags |= FLAG_TRANS_ENDING;
            endTransaction(status);
            if (_sync != null)
                _sync.afterCompletion(status);

            if ((_autoDetach & DETACH_COMMIT) != 0)
                detachAllInternal(null);

            // in an ee context, it's possible that the user tried to close
            // us but we didn't actually close because we were waiting on this
            // transaction; if that's true, then close now
            if ((_flags & FLAG_CLOSE_INVOKED) != 0
                && _compat.getCloseOnManagedCommit())
                free();
        } catch (OpenJPAException ke) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), ke);
            throw ke;
        } catch (RuntimeException re) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("end-trans-error"), re);
            throw new StoreException(re);
        } finally {
            _flags &= ~FLAG_ACTIVE;
            _flags &= ~FLAG_FLUSHED;
            _flags &= ~FLAG_TRANS_ENDING;

            if (_transEventManager != null 
                && _transEventManager.hasEndListeners()) {
                _transEventManager.fireEvent(new TransactionEvent(this,
                    status == Status.STATUS_COMMITTED
                        ? TransactionEvent.AFTER_COMMIT_COMPLETE
                        : TransactionEvent.AFTER_ROLLBACK_COMPLETE,
                    null, null, null, null));
            }

            endOperation();
        }
    }

    /**
     * Flush safely, catching reentrant calls.
     */
    private void flushSafe(int reason) {
        if ((_flags & FLAG_FLUSHING) != 0)
            throw new InvalidStateException(_loc.get("reentrant-flush"));

        _flags |= FLAG_FLUSHING;
        try {
            flush(reason);
        } finally {
            _flags &= ~FLAG_FLUSHING;
        }
    }

    /**
     * Flush the transactional state to the data store. Subclasses that
     * customize commit behavior should override this method. The method
     * assumes that the persistence manager is locked, is not closed,
     * and has an active transaction.
     *
     * @param reason one of {@link #FLUSH_INC}, {@link #FLUSH_COMMIT},
     * {@link #FLUSH_ROLLBACK}, or {@link #FLUSH_LOGICAL}
     * @since 0.2.5
     */
    protected void flush(int reason) {
        // this will enlist proxied states as necessary so we know whether we
        // have anything to flush
        Collection transactional = getTransactionalStates();

        // do we actually have to flush?  only if our flags say so, or if
        // we have transaction listeners that need to be invoked for commit
        // (no need to invoke them on inc flush if nothing is dirty).  we
        // special case the remote commit listener used by the datacache cause
        // we know it doesn't require the commit event when nothing changes
        boolean flush = (_flags & FLAG_FLUSH_REQUIRED) != 0;
        boolean listeners = _transEventManager != null
            && (_transEventManager.hasFlushListeners()
            || _transEventManager.hasEndListeners())
            && ((_flags & FLAG_REMOTE_LISTENER) == 0
            || _transEventManager.getListeners().size() > 1);
        if (!flush && (reason != FLUSH_COMMIT || !listeners))
            return;

        Collection mobjs = null;
        _flags |= FLAG_PRESTORING;
        try {
            if (flush) {
                // call pre store on all currently transactional objs
                for (Iterator itr = transactional.iterator(); itr.hasNext();)
                    ((StateManagerImpl) itr.next()).beforeFlush(reason, _call);
                flushAdditions(transactional, reason);
            }

            // hopefully now all dependent instances that are going to end
            // up referenced have been marked as such; delete unrefed
            // dependents
            _flags |= FLAG_DEREFDELETING;
            if (flush && _derefCache != null && !_derefCache.isEmpty()) {
                for (Iterator itr = _derefCache.iterator(); itr.hasNext();)
                    deleteDeref((StateManagerImpl) itr.next());
                flushAdditions(transactional, reason);
            }

            if (reason != FLUSH_LOGICAL) {
                // if no datastore transaction, start one; even if we don't
                // think we'll need to flush at this point, our transaction
                // listeners might introduce some dirty objects or interact
                // directly with the database
                if ((_flags & FLAG_STORE_ACTIVE) == 0)
                    beginStoreManagerTransaction(false);

                if (_transEventManager != null
                    && (_transEventManager.hasFlushListeners()
                    || _transEventManager.hasEndListeners())
                    && (flush || reason == FLUSH_COMMIT)) {
                    // fire events
                    mobjs = new ManagedObjectCollection(transactional);
                    if (reason == FLUSH_COMMIT
                        && _transEventManager.hasEndListeners()) {
                        _transEventManager.fireEvent(new TransactionEvent
                            (this, TransactionEvent.BEFORE_COMMIT, mobjs,
                                _persistedClss, _updatedClss, _deletedClss));

                        flushAdditions(transactional, reason);
                        flush = (_flags & FLAG_FLUSH_REQUIRED) != 0;
                    }

                    if (flush && _transEventManager.hasFlushListeners()) {
                        _transEventManager.fireEvent(new TransactionEvent
                            (this, TransactionEvent.BEFORE_FLUSH, mobjs,
                                _persistedClss, _updatedClss, _deletedClss));
                        flushAdditions(transactional, reason);
                    }
                }
            }
        }
        finally {
            _flags &= ~FLAG_PRESTORING;
            _flags &= ~FLAG_DEREFDELETING;
            _transAdditions = null;
            _derefAdditions = null;

            // also clear derefed set; the deletes have been recorded
            if (_derefCache != null)
                _derefCache.clear();
        }

        // flush to store manager
        List exceps = null;
        try {
            if (flush && reason != FLUSH_LOGICAL) {
                _flags |= FLAG_STORE_FLUSHING;
                exceps = add(exceps,
                    newFlushException(_store.flush(transactional)));
            }
        }
        finally {
            _flags &= ~FLAG_STORE_FLUSHING;

            if (reason == FLUSH_ROLLBACK)
                exceps = add(exceps, endStoreManagerTransaction(true));
            else if (reason != FLUSH_LOGICAL)
                _flags &= ~FLAG_FLUSH_REQUIRED;

            // mark states as flushed
            if (flush) {
                StateManagerImpl sm;
                for (Iterator itr = transactional.iterator(); itr.hasNext();) {
                    sm = (StateManagerImpl) itr.next();

                    // the state may have become transient, such as if
                    // it is embedded and the owner has been deleted during
                    // this flush process; bug #1100
                    if (sm.getPCState() == PCState.TRANSIENT)
                        continue;

                    sm.afterFlush(reason);
                    if (reason == FLUSH_INC) {
                        // if not about to clear trans cache for commit anyway,
                        // re-cache dirty objects with default soft refs; we
                        // don't need hard refs now that the changes have been
                        // flushed
                        sm.proxyFields(true, false);
                        _transCache.flushed(sm);
                    }
                }
            }
        }

        // throw any exceptions to shortcut listeners on fail
        throwNestedExceptions(exceps, true);

        if (flush && reason != FLUSH_ROLLBACK && reason != FLUSH_LOGICAL
            && _transEventManager != null
            && _transEventManager.hasFlushListeners()) {
            _transEventManager.fireEvent(new TransactionEvent(this,
                TransactionEvent.AFTER_FLUSH, mobjs, _persistedClss,
                _updatedClss, _deletedClss));
        }
    }

    /**
     * Flush newly-transactional objects.
     */
    private void flushAdditions(Collection transactional, int reason) {
        boolean loop;
        do {
            // flush new transactional instances; note logical or
            loop = flushTransAdditions(transactional, reason)
                | deleteDerefAdditions(_derefCache);
        } while (loop);
    }

    /**
     * Flush transactional additions.
     */
    private boolean flushTransAdditions(Collection transactional, int reason) {
        if (_transAdditions == null || _transAdditions.isEmpty())
            return false;

        // keep local transactional list copy up to date
        transactional.addAll(_transAdditions);

        // copy the change set, then clear it for the next iteration
        StateManagerImpl[] states = (StateManagerImpl[]) _transAdditions.
            toArray(new StateManagerImpl[_transAdditions.size()]);
        _transAdditions.clear();

        for (int i = 0; i < states.length; i++)
            states[i].beforeFlush(reason, _call);
        return true;
    }

    /**
     * Delete new dereferenced objects.
     */
    private boolean deleteDerefAdditions(Collection derefs) {
        if (_derefAdditions == null || _derefAdditions.isEmpty())
            return false;

        // remember these additions in case one becomes derefed again later
        derefs.addAll(_derefAdditions);

        StateManagerImpl[] states = (StateManagerImpl[]) _derefAdditions.
            toArray(new StateManagerImpl[_derefAdditions.size()]);
        _derefAdditions.clear();

        for (int i = 0; i < states.length; i++)
            deleteDeref(states[i]);
        return true;
    }

    /**
     * Delete a dereferenced dependent.
     */
    private void deleteDeref(StateManagerImpl sm) {
        int action = processArgument(OpCallbacks.OP_DELETE,
            sm.getManagedInstance(), sm, null);
        if ((action & OpCallbacks.ACT_RUN) != 0)
            sm.delete();
        if ((action & OpCallbacks.ACT_CASCADE) != 0)
            sm.cascadeDelete(_call);
    }

    /**
     * Determine the action to take based on the user's given callbacks and
     * our implicit behavior.
     */
    private int processArgument(int op, Object obj, OpenJPAStateManager sm,
        OpCallbacks call) {
        if (call != null)
            return call.processArgument(op, obj, sm);
        if (_call != null)
            return _call.processArgument(op, obj, sm);
        return OpCallbacks.ACT_RUN | OpCallbacks.ACT_CASCADE;
    }

    /**
     * Throw the proper exception based on the given set of flush errors, or
     * do nothing if no errors occurred.
     */
    private OpenJPAException newFlushException(Collection exceps) {
        if (exceps == null || exceps.isEmpty())
            return null;

        Throwable[] t = (Throwable[]) exceps.toArray
            (new Throwable[exceps.size()]);
        List failed = new ArrayList(t.length);

        // create fatal exception with nested exceptions for all the failed
        // objects; if all OL exceptions, throw a top-level OL exception
        boolean opt = true;
        for (int i = 0; opt && i < t.length; i++) {
            opt = t[i] instanceof OptimisticException;
            if (opt) {
                Object f = ((OptimisticException) t[i]).getFailedObject();
                if (f != null)
                    failed.add(f);
            }
        }
        if (opt && !failed.isEmpty())
            return new OptimisticException(failed, t);
        if (opt)
            return new OptimisticException(t);
        return new StoreException(_loc.get("rolled-back")).
            setNestedThrowables(t).setFatal(true);
    }

    /**
     * End the current transaction, making appropriate state transitions.
     */
    protected void endTransaction(int status) {
        // if a data store transaction was in progress, do the
        // appropriate transaction change
        boolean rollback = status != Status.STATUS_COMMITTED;
        List exceps = null;

        try {
            exceps = add(exceps, endStoreManagerTransaction(rollback));
        } catch (RuntimeException re) {
            rollback = true;
            exceps = add(exceps, re);
        }

        // go back to default none lock level
        _fc.setReadLockLevel(LOCK_NONE);
        _fc.setWriteLockLevel(LOCK_NONE);
        _fc.setLockTimeout(-1);

        Collection transStates = _transCache;
        if (transStates == null)
            transStates = Collections.EMPTY_LIST;

        // fire after rollback/commit event
        Collection mobjs = null;
        if (_transEventManager != null && _transEventManager.hasEndListeners())
        {
            mobjs = new ManagedObjectCollection(transStates);
            int eventType = (rollback) ? TransactionEvent.AFTER_ROLLBACK
                : TransactionEvent.AFTER_COMMIT;
            _transEventManager.fireEvent(new TransactionEvent(this, eventType,
                mobjs, _persistedClss, _updatedClss, _deletedClss));
        }

        // null transactional caches now so that all the removeFromTransaction
        // calls as we transition each object don't have to do any work; don't
        // clear trans cache object because we still need the transStates
        // reference to it below
        _transCache = null;
        if (_persistedClss != null)
            _persistedClss.clear();
        if (_updatedClss != null)
            _updatedClss.clear();
        if (_deletedClss != null)
            _deletedClss.clear();

        // new cache would get cleared anyway during transitions, but doing so
        // immediately saves us some lookups
        _cache.clearNew();

        // tell all derefed instances they're no longer derefed; we can't
        // rely on rollback and commit calls below cause some instances might
        // not be transactional
        if (_derefCache != null && !_derefCache.isEmpty()) {
            for (Iterator itr = _derefCache.iterator(); itr.hasNext();)
                ((StateManagerImpl) itr.next()).setDereferencedDependent
                    (false, false);
            _derefCache.clear();
        }

        // peform commit or rollback state transitions on each instance
        StateManagerImpl sm;
        for (Iterator itr = transStates.iterator(); itr.hasNext();) {
            sm = (StateManagerImpl) itr.next();
            try {
                if (rollback) {
                    // tell objects that may have been derefed then flushed
                    // (and therefore deleted) to un-deref
                    sm.setDereferencedDependent(false, false);
                    sm.rollback();
                } else
                    sm.commit();
            } catch (RuntimeException re) {
                exceps = add(exceps, re);
            }
        }

        // notify the lock manager to clean up and release remaining locks
        _lm.endTransaction();

        // clear old savepoints in reverse
        OpenJPASavepoint save;
        while (_savepoints != null && _savepoints.size() > 0) {
            save =
                (OpenJPASavepoint) _savepoints.remove(_savepoints.size() - 1);
            save.release(false);
        }
        _savepoints = null;
        _savepointCache = null;

        // fire after state change event
        if (_transEventManager != null && _transEventManager.hasEndListeners())
            _transEventManager.fireEvent(new TransactionEvent(this,
                TransactionEvent.AFTER_STATE_TRANSITIONS, mobjs, null, null,
                null));

        // now clear trans cache; keep cleared version rather than
        // null to avoid having to re-create the set later; more efficient
        if (transStates != Collections.EMPTY_LIST) {
            _transCache = (TransactionalCache) transStates;
            _transCache.clear();
        }

        throwNestedExceptions(exceps, true);
    }

    ////////////////////
    // Object lifecycle
    ////////////////////

    public void persist(Object obj, OpCallbacks call) {
        persist(obj, null, true, call);
    }

    public OpenJPAStateManager persist(Object obj, Object id,
        OpCallbacks call) {
        return persist(obj, id, true, call);
    }

    public void persistAll(Collection objs, OpCallbacks call) {
        persistAll(objs, true, call);
    }

    /**
     * Persist the given objects.  Indicate whether this was an explicit persist
     * (PNEW) or a provisonal persist (PNEWPROVISIONAL).
     */
    public void persistAll(Collection objs, boolean explicit, 
        OpCallbacks call) {
        if (objs.isEmpty())
            return;

        beginOperation(true);
        List exceps = null;
        try {
            assertWriteOperation();

            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                try {
                    persist(itr.next(), explicit, call);
                } catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }
        } finally {
            endOperation();
        }
        throwNestedExceptions(exceps, false);
    }

    /**
     * If the given element is not null, add it to the given list,
     * creating the list if necessary.
     */
    private List add(List l, Object o) {
        if (o == null)
            return l;
        if (l == null)
            l = new LinkedList();
        l.add(o);
        return l;
    }

    /**
     * Throw an exception wrapping the given nested exceptions.
     */
    private void throwNestedExceptions(List exceps, boolean datastore) {
        if (exceps == null || exceps.isEmpty())
            return;
        if (datastore && exceps.size() == 1)
            throw (RuntimeException) exceps.get(0);

        boolean fatal = false;
        Throwable[] t = (Throwable[]) exceps.toArray
            (new Throwable[exceps.size()]);
        for (int i = 0; i < t.length; i++) {
            if (t[i] instanceof OpenJPAException
                && ((OpenJPAException) t[i]).isFatal())
                fatal = true;
        }
        OpenJPAException err;
        if (datastore)
            err = new StoreException(_loc.get("nested-exceps"));
        else
            err = new UserException(_loc.get("nested-exceps"));
        throw err.setNestedThrowables(t).setFatal(fatal);
    }

    /**
     * Persist the given object.  Indicate whether this was an explicit persist
     * (PNEW) or a provisonal persist (PNEWPROVISIONAL)
     */
    public void persist(Object obj, boolean explicit, OpCallbacks call) {
        persist(obj, null, explicit, call);
    }

    /**
     * Persist the given object.  Indicate whether this was an explicit persist
     * (PNEW) or a provisonal persist (PNEWPROVISIONAL).
     * See {@link Broker} for details on this method.
     */
    public OpenJPAStateManager persist(Object obj, Object id, boolean explicit,
        OpCallbacks call) {
        if (obj == null)
            return null;

        beginOperation(true);
        try {
            assertWriteOperation();

            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if (!_operating.add(obj))
                return sm;

            int action = processArgument(OpCallbacks.OP_PERSIST, obj, sm, call);
            if (action == OpCallbacks.ACT_NONE)
                return sm;

            // ACT_CASCADE
            if ((action & OpCallbacks.ACT_RUN) == 0) {
                if (sm != null)
                    sm.cascadePersist(call);
                else
                    cascadeTransient(OpCallbacks.OP_PERSIST, obj, call,
                        "persist");
                return sm;
            }

            // ACT_RUN
            PersistenceCapable pc;
            if (sm != null) {
                if (sm.isDetached())
                    throw new ObjectExistsException(_loc.get
                        ("persist-detached", Exceptions.toString(obj))).
                        setFailedObject(obj);

                if (!sm.isEmbedded()) {
                    sm.persist();
                    _cache.persist(sm);
                    if ((action & OpCallbacks.ACT_CASCADE) != 0)
                        sm.cascadePersist(call);
                    return sm;
                }

                // an embedded field; notify the owner that the value has
                // changed by becoming independently persistent
                sm.getOwner().dirty(sm.getOwnerMetaData().
                    getFieldMetaData().getIndex());
                _cache.persist(sm);
                pc = sm.getPersistenceCapable();
            } else {
                pc = assertPersistenceCapable(obj);
                if (pc.pcIsDetached() == Boolean.TRUE)
                    throw new ObjectExistsException(_loc.get
                        ("persist-detached", Exceptions.toString(obj))).
                        setFailedObject(obj);
            }

            ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
                getMetaData(obj.getClass(), _loader, true);
            fireLifecycleEvent(obj, null, meta, LifecycleEvent.BEFORE_PERSIST);

            // create id for instance
            if (id == null) {
                if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION)
                    id = ApplicationIds.create(pc, meta);
                else if (meta.getIdentityType() == ClassMetaData.ID_UNKNOWN)
                    throw new UserException(_loc.get("meta-unknownid", meta));
                else
                    id = StateManagerId.newInstance(this);
            }

            // make sure we don't already have the instance cached
            StateManagerImpl other = getStateManagerImplById(id, false);
            if (other != null && !other.isDeleted() && !other.isNew())
                throw new ObjectExistsException(_loc.get("cache-exists",
                    obj.getClass().getName(), id)).setFailedObject(obj);

            // if had embedded sm, null it
            if (sm != null)
                pc.pcReplaceStateManager(null);

            // create new sm
            sm = new StateManagerImpl(id, meta, this);
            if ((_flags & FLAG_ACTIVE) != 0) {
                if (explicit)
                    sm.initialize(pc, PCState.PNEW);
                else
                    sm.initialize(pc, PCState.PNEWPROVISIONAL);
            } else
                sm.initialize(pc, PCState.PNONTRANSNEW);
            if ((action & OpCallbacks.ACT_CASCADE) != 0)
                sm.cascadePersist(call);
            return sm;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Temporarily manage the given instance in order to cascade the given
     * operation through it.
     */
    private void cascadeTransient(int op, Object obj, OpCallbacks call,
        String errOp) {
        PersistenceCapable pc = assertPersistenceCapable(obj);

        // if using detached state manager, don't replace
        if (pc.pcGetStateManager() != null)
            throw newDetachedException(obj, errOp);

        ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
            getMetaData(obj.getClass(), _loader, true);
        StateManagerImpl sm = new StateManagerImpl(StateManagerId.
            newInstance(this), meta, this);
        sm.initialize(pc, PCState.TLOADED);
        try {
            switch (op) {
                case OpCallbacks.OP_PERSIST:
                    sm.cascadePersist(call);
                    break;
                case OpCallbacks.OP_DELETE:
                    sm.cascadeDelete(call);
                    break;
                case OpCallbacks.OP_REFRESH:
                    sm.gatherCascadeRefresh(call);
                    break;
                default:
                    throw new InternalException(String.valueOf(op));
            }
        }
        finally {
            sm.release(true);
        }
    }

    public void deleteAll(Collection objs, OpCallbacks call) {
        beginOperation(true);
        try {
            assertWriteOperation();

            List exceps = null;
            Object obj;
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                try {
                    obj = itr.next();
                    if (obj != null)
                        delete(obj, getStateManagerImpl(obj, true), call);
                } catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }
            throwNestedExceptions(exceps, false);
        } finally {
            endOperation();
        }
    }

    public void delete(Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true);
        try {
            assertWriteOperation();
            delete(obj, getStateManagerImpl(obj, true), call);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Internal delete.
     */
    void delete(Object obj, StateManagerImpl sm, OpCallbacks call) {
        if (!_operating.add(obj))
            return;

        int action = processArgument(OpCallbacks.OP_DELETE, obj, sm, call);
        if (action == OpCallbacks.ACT_NONE)
            return;

        // ACT_CASCADE
        if ((action & OpCallbacks.ACT_RUN) == 0) {
            if (sm != null)
                sm.cascadeDelete(call);
            else
                cascadeTransient(OpCallbacks.OP_DELETE, obj, call, "delete");
            return;
        }

        // ACT_RUN
        if (sm != null) {
            if (sm.isDetached())
                throw newDetachedException(obj, "delete");
            if ((action & OpCallbacks.ACT_CASCADE) != 0)
                sm.cascadeDelete(call);
            sm.delete();
        } else if (assertPersistenceCapable(obj).pcIsDetached() == Boolean.TRUE)
            throw newDetachedException(obj, "delete");
    }

    /**
     * Throw an exception indicating that the current action can't be
     * performed on a detached object.
     */
    private OpenJPAException newDetachedException(Object obj,
        String operation) {
        throw new UserException(_loc.get("bad-detached-op", operation,
            Exceptions.toString(obj))).setFailedObject(obj);
    }

    public void releaseAll(Collection objs, OpCallbacks call) {
        beginOperation(false);
        try {
            List exceps = null;
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                try {
                    release(itr.next(), call);
                } catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }
            throwNestedExceptions(exceps, false);
        } finally {
            endOperation();
        }
    }

    public void release(Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(false);
        try {
            StateManagerImpl sm = getStateManagerImpl(obj, true);
            int action = processArgument(OpCallbacks.OP_RELEASE, obj, sm, call);

            if (sm == null)
                return;
            if ((action & OpCallbacks.ACT_RUN) != 0 && sm.isPersistent()) {
                boolean pending = sm.isPendingTransactional();
                sm.release(true);
                if (pending)
                    removeFromPendingTransaction(sm);
            }
        }
        catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public OpenJPAStateManager embed(Object obj, Object id,
        OpenJPAStateManager owner, ValueMetaData ownerMeta) {
        beginOperation(true);
        try {
            StateManagerImpl orig = getStateManagerImpl(obj, true);
            if (orig != null) {
                // if already embedded, nothing to do
                if (orig.getOwner() == owner && orig.getMetaData().
                    getEmbeddingMetaData() == ownerMeta)
                    return orig;

                // otherwise make sure pc is fully loaded for when we copy its
                // data below
                orig.load(_fc, StateManagerImpl.LOAD_ALL, null, null, false);
            }

            // create new state manager with embedded metadata
            ClassMetaData meta = ownerMeta.getEmbeddedMetaData();
            if (meta == null)
                throw new InternalException(_loc.get("bad-embed", ownerMeta));

            if (id == null)
                id = StateManagerId.newInstance(this);

            StateManagerImpl sm = new StateManagerImpl(id, meta, this);
            sm.setOwner((StateManagerImpl) owner, ownerMeta);

            PersistenceCapable copy;
            PCState state;
            Class type = meta.getDescribedType();
            if (type.isInterface())
                type = meta.getInterfaceImpl();
            if (obj != null) {
                // give copy and the original instance the same state manager
                // so that we can copy fields from one to the other
                StateManagerImpl copySM;
                PersistenceCapable pc;
                if (orig == null) {
                    copySM = sm;
                    pc = assertPersistenceCapable(obj);
                    pc.pcReplaceStateManager(sm);
                } else {
                    copySM = orig;
                    pc = orig.getPersistenceCapable();
                }

                try {
                    // copy the instance.  we do this even if it doesn't already
                    // have a state manager in case it is later assigned to a
                    // PC field; at that point it's too late to copy
                    copy = PCRegistry.newInstance(type, copySM, false);
                    int[] fields = new int[meta.getFields().length];
                    for (int i = 0; i < fields.length; i++)
                        fields[i] = i;
                    copy.pcCopyFields(pc, fields);
                    state = PCState.ECOPY;
                    copy.pcReplaceStateManager(null);
                } finally {
                    // if the instance didn't have a state manager to start,
                    // revert it to being transient
                    if (orig == null)
                        pc.pcReplaceStateManager(null);
                }
            } else {
                copy = PCRegistry.newInstance(type, sm, false);
                if ((_flags & FLAG_ACTIVE) != 0 && !_optimistic)
                    state = PCState.ECLEAN;
                else
                    state = PCState.ENONTRANS;
            }

            sm.initialize(copy, state);
            return sm;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * If not already cached, create an empty copy of the given state
     * manager in the given state.
     */
    OpenJPAStateManager copy(OpenJPAStateManager copy, PCState state) {
        beginOperation(true);
        try {
            assertOpen();
            Object oid = copy.fetchObjectId();
            Class type = copy.getManagedInstance().getClass();
            if (oid == null)
                throw new InternalException();
            // cached instance?
            StateManagerImpl sm = null;
            if (!copy.isEmbedded())
                sm = getStateManagerImplById(oid, true);
            if (sm == null) {
                MetaDataRepository repos = _conf.
                    getMetaDataRepositoryInstance();
                ClassMetaData meta = repos.getMetaData(type, _loader, true);
                // construct a new state manager with all info known
                sm = new StateManagerImpl(oid, meta, this);
                sm.setObjectId(oid);
                sm.initialize(sm.getMetaData().getDescribedType(), state);
            }
            return sm;
        } finally {
            endOperation();
        }
    }

    public void refreshAll(Collection objs, OpCallbacks call) {
        if (objs.isEmpty())
            return;

        beginOperation(true);
        try {
            assertNontransactionalRead();

            for (Iterator itr = objs.iterator(); itr.hasNext();)
                gatherCascadeRefresh(itr.next(), call);
            if (_operating.isEmpty())
                return;
            if (_operating.size() == 1)
                refreshInternal(_operating.iterator().next(), call);
            else
                refreshInternal(_operating, call);
        } finally {
            endOperation();
        }
    }

    public void refresh(Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true);
        try {
            assertNontransactionalRead();

            gatherCascadeRefresh(obj, call);
            if (_operating.isEmpty())
                return;
            if (_operating.size() == 1)
                refreshInternal(_operating.iterator().next(), call);
            else
                refreshInternal(_operating, call);
        } finally {
            endOperation();
        }
    }

    /**
     * Gathers all objects reachable through cascade-refresh relations
     * into the operating set.
     */
    void gatherCascadeRefresh(Object obj, OpCallbacks call) {
        if (obj == null)
            return;
        if (!_operating.add(obj))
            return;

        StateManagerImpl sm = getStateManagerImpl(obj, false);
        int action = processArgument(OpCallbacks.OP_REFRESH, obj, sm, call);
        if ((action & OpCallbacks.ACT_CASCADE) == 0)
            return;

        if (sm != null)
            sm.gatherCascadeRefresh(call);
        else
            cascadeTransient(OpCallbacks.OP_REFRESH, obj, call, "refresh");
    }

    /**
     * This method is called with the full set of objects reachable via
     * cascade-refresh relations from the user-given instances.
     */
    protected void refreshInternal(Collection objs, OpCallbacks call) {
        List exceps = null;
        try {
            // collect instances that need a refresh
            Collection load = null;
            StateManagerImpl sm;
            Object obj;
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                obj = itr.next();
                if (obj == null)
                    continue;

                try {
                    sm = getStateManagerImpl(obj, true);
                    if ((processArgument(OpCallbacks.OP_REFRESH, obj, sm, call)
                        & OpCallbacks.ACT_RUN) == 0)
                        continue;

                    if (sm != null) {
                        if (sm.isDetached())
                            throw newDetachedException(obj, "refresh");
                        else if (sm.beforeRefresh(true)) {
                            if (load == null)
                                load = new ArrayList(objs.size());
                            load.add(sm);
                        }
                    } else if (assertPersistenceCapable(obj).pcIsDetached()
                        == Boolean.TRUE)
                        throw newDetachedException(obj, "refresh");
                } catch (OpenJPAException ke) {
                    exceps = add(exceps, ke);
                }
            }

            // refresh all
            if (load != null) {
                Collection failed = _store.loadAll(load, null,
                    _store.FORCE_LOAD_REFRESH, _fc, null);
                if (failed != null && !failed.isEmpty())
                    exceps = add(exceps, newObjectNotFoundException(failed));

                // perform post-refresh transitions and make sure all fetch
                // group fields are loaded
                for (Iterator itr = load.iterator(); itr.hasNext();) {
                    sm = (StateManagerImpl) itr.next();
                    if (failed != null && failed.contains(sm.getId()))
                        continue;

                    try {
                        sm.afterRefresh();
                        sm.load(_fc, StateManagerImpl.LOAD_FGS, null, null, 
                            false);
                    } catch (OpenJPAException ke) {
                        exceps = add(exceps, ke);
                    }
                }
            }

            // now invoke postRefresh on all the instances
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                try {
                    sm = getStateManagerImpl(itr.next(), true);
                    if (sm != null && !sm.isDetached())
                        fireLifecycleEvent(sm.getManagedInstance(), null,
                            sm.getMetaData(), LifecycleEvent.AFTER_REFRESH);
                } catch (OpenJPAException ke) {
                    exceps = add(exceps, ke);
                }
            }
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        }
        throwNestedExceptions(exceps, false);
    }

    /**
     * Optimization for single-object refresh.
     */
    protected void refreshInternal(Object obj, OpCallbacks call) {
        try {
            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if ((processArgument(OpCallbacks.OP_REFRESH, obj, sm, call)
                & OpCallbacks.ACT_RUN) == 0)
                return;

            if (sm != null) {
                if (sm.isDetached())
                    throw newDetachedException(obj, "refresh");
                else if (sm.beforeRefresh(false)) {
                    sm.load(_fc, StateManagerImpl.LOAD_FGS, null, null, false);
                    sm.afterRefresh();
                }
                fireLifecycleEvent(sm.getManagedInstance(), null,
                    sm.getMetaData(), LifecycleEvent.AFTER_REFRESH);
            } else if (assertPersistenceCapable(obj).pcIsDetached()
                == Boolean.TRUE)
                throw newDetachedException(obj, "refresh");
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        }
    }

    public void retrieveAll(Collection objs, boolean dfgOnly,
        OpCallbacks call) {
        if (objs.isEmpty())
            return;
        if (objs.size() == 1) {
            retrieve(objs.iterator().next(), dfgOnly, call);
            return;
        }

        List exceps = null;
        beginOperation(true);
        try {
            assertOpen();
            assertNontransactionalRead();

            // collect all hollow instances for load
            Object obj;
            Collection load = null;
            StateManagerImpl sm;
            Collection sms = new ArrayList(objs.size());
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                obj = itr.next();
                if (obj == null)
                    continue;

                try {
                    sm = getStateManagerImpl(obj, true);
                    if ((processArgument(OpCallbacks.OP_RETRIEVE, obj, sm, call)
                        & OpCallbacks.ACT_RUN) == 0)
                        continue;

                    if (sm != null) {
                        if (sm.isDetached())
                            throw newDetachedException(obj, "retrieve");
                        if (sm.isPersistent()) {
                            sms.add(sm);
                            if (sm.getPCState() == PCState.HOLLOW) {
                                if (load == null)
                                    load = new ArrayList();
                                load.add(sm);
                            }
                        }
                    } else if (assertPersistenceCapable(obj).pcIsDetached()
                        == Boolean.TRUE)
                        throw newDetachedException(obj, "retrieve");
                } catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }

            // load all hollow instances
            Collection failed = null;
            if (load != null) {
                int mode = (dfgOnly) ? _store.FORCE_LOAD_DFG
                    : _store.FORCE_LOAD_ALL;
                failed = _store.loadAll(load, null, mode, _fc, null);
                if (failed != null && !failed.isEmpty())
                    exceps = add(exceps, newObjectNotFoundException(failed));
            }

            // retrieve all non-failed instances
            for (Iterator itr = sms.iterator(); itr.hasNext();) {
                sm = (StateManagerImpl) itr.next();
                if (failed != null && failed.contains(sm.getId()))
                    continue;

                int mode = (dfgOnly) ? StateManagerImpl.LOAD_FGS
                    : StateManagerImpl.LOAD_ALL;
                try {
                    sm.beforeRead(-1);
                    sm.load(_fc, mode, null, null, false);
                } catch (OpenJPAException ke) {
                    exceps = add(exceps, ke);
                }
            }
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
        throwNestedExceptions(exceps, false);
    }

    public void retrieve(Object obj, boolean dfgOnly, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true);
        try {
            assertOpen();
            assertNontransactionalRead();

            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if ((processArgument(OpCallbacks.OP_RETRIEVE, obj, sm, call)
                & OpCallbacks.ACT_RUN) == 0)
                return;

            if (sm != null) {
                if (sm.isDetached())
                    throw newDetachedException(obj, "retrieve");
                if (sm.isPersistent()) {
                    int mode = (dfgOnly) ? StateManagerImpl.LOAD_FGS
                        : StateManagerImpl.LOAD_ALL;
                    sm.beforeRead(-1);
                    sm.load(_fc, mode, null, null, false);
                }
            } else if (assertPersistenceCapable(obj).pcIsDetached()
                == Boolean.TRUE)
                throw newDetachedException(obj, "retrieve");
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public void evictAll(OpCallbacks call) {
        beginOperation(false);
        try {
            // evict all PClean and PNonTrans objects
            Collection c = getManagedStates();
            StateManagerImpl sm;
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                sm = (StateManagerImpl) itr.next();
                if (sm.isPersistent() && !sm.isDirty())
                    evict(sm.getManagedInstance(), call);
            }
        }
        finally {
            endOperation();
        }
    }

    public void evictAll(Collection objs, OpCallbacks call) {
        List exceps = null;
        beginOperation(false);
        try {
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                try {
                    evict(itr.next(), call);
                } catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }
        } finally {
            endOperation();
        }
        throwNestedExceptions(exceps, false);
    }

    public void evictAll(Extent extent, OpCallbacks call) {
        if (extent == null)
            return;

        beginOperation(false);
        try {
            // evict all PClean and PNonTrans objects in extent
            Collection c = getManagedStates();
            StateManagerImpl sm;
            Class cls;
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                sm = (StateManagerImpl) itr.next();
                if (sm.isPersistent() && !sm.isDirty()) {
                    cls = sm.getMetaData().getDescribedType();
                    if (cls == extent.getElementType()
                        || (extent.hasSubclasses()
                        && extent.getElementType().isAssignableFrom(cls)))
                        evict(sm.getManagedInstance(), call);
                }
            }
        } finally {
            endOperation();
        }
    }

    public void evict(Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(false);
        try {
            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if ((processArgument(OpCallbacks.OP_EVICT, obj, sm, call)
                & OpCallbacks.ACT_RUN) == 0)
                return;
            if (sm == null)
                return;

            sm.evict();
            if (_evictDataCache && sm.getObjectId() != null) {
                DataCache cache = sm.getMetaData().getDataCache();
                if (cache != null)
                    cache.remove(sm.getObjectId());
            }
        }
        catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public Object detach(Object obj, OpCallbacks call) {
        if (obj == null)
            return null;
        if (call == null)
            call = _call;

        beginOperation(true);
        try {
            return new DetachManager(this, false, call).detach(obj);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public Object[] detachAll(Collection objs, OpCallbacks call) {
        if (objs == null)
            return null;
        if (objs.isEmpty())
            return new Object[0];
        if (call == null)
            call = _call;

        beginOperation(true);
        try {
            return new DetachManager(this, false, call).detachAll(objs);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public void detachAll(OpCallbacks call) {
        beginOperation(true);
        try {
            if ((_flags & FLAG_FLUSH_REQUIRED) != 0)
                flush();
            detachAllInternal(call);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    private void detachAllInternal(OpCallbacks call) {
        Collection states = getManagedStates();
        StateManagerImpl sm;
        for (Iterator itr = states.iterator(); itr.hasNext();) {
            sm = (StateManagerImpl) itr.next();
            if (!sm.isPersistent())
                itr.remove();
            else if (!sm.getMetaData().isDetachable()) {
                sm.release(true);
                itr.remove();
            }
        }
        if (states.isEmpty())
            return;

        if (call == null)
            call = _call;
        new DetachManager(this, true, call).detachAll
            (new ManagedObjectCollection(states));
    }

    public Object attach(Object obj, boolean copyNew, OpCallbacks call) {
        if (obj == null)
            return null;

        beginOperation(true);
        try {
            // make sure not to try to set rollback only if this fails
            assertWriteOperation();
            try {
                return new AttachManager(this, copyNew, call).attach(obj);
            } catch (OptimisticException oe) {
                setRollbackOnly();
                throw oe.setFatal(true);
            } catch (OpenJPAException ke) {
                throw ke;
            } catch (RuntimeException re) {
                throw new GeneralException(re);
            }
        }
        finally {
            endOperation();
        }
    }

    public Object[] attachAll(Collection objs, boolean copyNew,
        OpCallbacks call) {
        if (objs == null)
            return null;
        if (objs.isEmpty())
            return new Object[0];

        beginOperation(true);
        try {
            // make sure not to try to set rollback only if this fails
            assertWriteOperation();
            try {
                return new AttachManager(this, copyNew, call).attachAll(objs);
            } catch (OptimisticException oe) {
                setRollbackOnly();
                throw oe.setFatal(true);
            } catch (OpenJPAException ke) {
                throw ke;
            } catch (RuntimeException re) {
                throw new GeneralException(re);
            }
        }
        finally {
            endOperation();
        }
    }

    public void nontransactionalAll(Collection objs, OpCallbacks call) {
        beginOperation(true);
        try {
            List exceps = null;
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                try {
                    nontransactional(itr.next(), call);
                } catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }
            throwNestedExceptions(exceps, false);
        } finally {
            endOperation();
        }
    }

    public void nontransactional(Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true);
        try {
            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if ((processArgument(OpCallbacks.OP_NONTRANSACTIONAL, obj, sm, call)
                & OpCallbacks.ACT_RUN) == 0)
                return;
            if (sm != null)
                sm.nontransactional();
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Make the given instances transactional.
     */
    public void transactionalAll(Collection objs, boolean updateVersion,
        OpCallbacks call) {
        if (objs.isEmpty())
            return;
        if (objs.size() == 1) {
            transactional(objs.iterator().next(), updateVersion, call);
            return;
        }

        beginOperation(true);
        try {
            // collect all hollow instances for load, and make unmananged
            // instances transient-transactional
            Collection load = null;
            Object obj;
            StateManagerImpl sm;
            ClassMetaData meta;
            Collection sms = new ArrayList(objs.size());
            List exceps = null;
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                obj = itr.next();
                if (obj == null)
                    continue;

                try {
                    sm = getStateManagerImpl(obj, true);
                    if ((processArgument(OpCallbacks.OP_TRANSACTIONAL, obj, sm,
                        call) & OpCallbacks.ACT_RUN) == 0)
                        continue;

                    if (sm == null) {
                        // manage transient instance
                        meta = _conf.getMetaDataRepositoryInstance().
                            getMetaData(obj.getClass(), _loader, true);

                        sm = new StateManagerImpl
                            (StateManagerId.newInstance(this), meta, this);
                        sm.initialize(assertPersistenceCapable(obj),
                            PCState.TCLEAN);
                    } else if (sm.isPersistent()) {
                        assertActiveTransaction();
                        sms.add(sm);
                        if (sm.getPCState() == PCState.HOLLOW) {
                            if (load == null)
                                load = new ArrayList();
                            load.add(sm);
                        }

                        sm.setCheckVersion(true);
                        if (updateVersion)
                            sm.setUpdateVersion(true);
                        _flags |= FLAG_FLUSH_REQUIRED; // version check/up
                    }
                }
                catch (UserException ue) {
                    exceps = add(exceps, ue);
                }
            }

            // load all hollow instances
            Collection failed = null;
            if (load != null) {
                failed = _store.loadAll(load, null, _store.FORCE_LOAD_NONE,
                    _fc, null);
                if (failed != null && !failed.isEmpty())
                    exceps = add(exceps,
                        newObjectNotFoundException(failed));
            }

            transactionalStatesAll(sms, failed, exceps);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Make the given instances transactional.
     */
    public void transactional(Object obj, boolean updateVersion,
        OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true);
        try {
            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if ((processArgument(OpCallbacks.OP_TRANSACTIONAL, obj, sm, call)
                & OpCallbacks.ACT_RUN) == 0)
                return;

            if (sm != null && sm.isPersistent()) {
                assertActiveTransaction();
                sm.transactional();
                sm.load(_fc, StateManagerImpl.LOAD_FGS, null, null, false);
                sm.setCheckVersion(true);
                if (updateVersion)
                    sm.setUpdateVersion(true);
                _flags |= FLAG_FLUSH_REQUIRED; // version check/up
            } else if (sm == null) {
                // manage transient instance
                ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
                    getMetaData(obj.getClass(), _loader, true);
                Object id = StateManagerId.newInstance(this);
                sm = new StateManagerImpl(id, meta, this);
                sm.initialize(assertPersistenceCapable(obj),
                    PCState.TCLEAN);
            }
        }
        catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Transition the given state managers to transactional.
     */
    private void transactionalStatesAll(Collection sms, Collection failed,
        List exceps) {
        // make instances transactional and make sure they are loaded
        StateManagerImpl sm;
        for (Iterator itr = sms.iterator(); itr.hasNext();) {
            sm = (StateManagerImpl) itr.next();
            if (failed != null && failed.contains(sm.getId()))
                continue;

            try {
                sm.transactional();
                sm.load(_fc, StateManagerImpl.LOAD_FGS, null, null, false);
            } catch (OpenJPAException ke) {
                exceps = add(exceps, ke);
            }
        }
        throwNestedExceptions(exceps, false);
    }

    /////////////////
    // Extent, Query
    /////////////////

    public Extent newExtent(Class type, boolean subclasses) {
        return newExtent(type, subclasses, null);
    }

    private Extent newExtent(Class type, boolean subclasses,
        FetchConfiguration fetch) {
        beginOperation(true);
        try {
            ExtentImpl extent = new ExtentImpl(this, type, subclasses, fetch);
            if (_extents == null)
                _extents = new ReferenceHashSet(ReferenceHashSet.WEAK);
            _extents.add(extent);

            return extent;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public Iterator extentIterator(Class type, boolean subclasses,
        FetchConfiguration fetch, boolean ignoreChanges) {
        Extent extent = newExtent(type, subclasses, fetch);
        extent.setIgnoreChanges(ignoreChanges);
        return extent.iterator();
    }

    public Query newQuery(String lang, Class cls, Object query) {
        Query q = newQuery(lang, query);
        q.setCandidateType(cls, true);
        return q;
    }

    public Query newQuery(String lang, Object query) {
        // common mistakes
        if (query instanceof Extent || query instanceof Class)
            throw new UserException(_loc.get("bad-new-query"));

        beginOperation(false);
        try {
            StoreQuery sq = _store.newQuery(lang);
            if (sq == null) {
                ExpressionParser ep = QueryLanguages.parserForLanguage(lang);
                if (ep != null)
                    sq = new ExpressionStoreQuery(ep);
                else if (QueryLanguages.LANG_METHODQL.equals(lang))
                    sq = new MethodStoreQuery();
                else
                    throw new UnsupportedException(lang);
            }

            Query q = newQueryImpl(lang, sq);
            q.setIgnoreChanges(_ignoreChanges);
            if (query != null)
                q.setQuery(query);

            // track queries
            if (_queries == null)
                _queries = new ReferenceHashSet(ReferenceHashSet.WEAK);
            _queries.add(q);
            return q;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    /**
     * Create a new query.
     */
    protected QueryImpl newQueryImpl(String lang, StoreQuery sq) {
        return new QueryImpl(this, lang, sq);
    }

    public Seq getIdentitySequence(ClassMetaData meta) {
        if (meta == null)
            return null;
        return getSequence(meta, null);
    }

    public Seq getValueSequence(FieldMetaData fmd) {
        if (fmd == null)
            return null;
        return getSequence(fmd.getDefiningMetaData(), fmd);
    }

    /**
     * Return a sequence for the given class and optional field.
     */
    private Seq getSequence(ClassMetaData meta, FieldMetaData fmd) {
        // get sequence strategy from metadata
        int strategy;
        if (fmd == null)
            strategy = meta.getIdentityStrategy();
        else
            strategy = fmd.getValueStrategy();

        // we can handle non-native strategies without the store manager
        switch (strategy) {
            case ValueStrategies.UUID_HEX:
                return UUIDHexSeq.getInstance();
            case ValueStrategies.UUID_STRING:
                return UUIDStringSeq.getInstance();
            case ValueStrategies.SEQUENCE:
                SequenceMetaData smd = (fmd == null)
                    ? meta.getIdentitySequenceMetaData()
                    : fmd.getValueSequenceMetaData();
                return smd.getInstance(_loader);
            default:
                // use store manager for native sequence
                if (fmd == null) {
                    // this will return a sequence even for app id classes, 
                    // which is what we want for backwards-compatibility
                    return _store.getDataStoreIdSequence(meta);
                }
                return _store.getValueSequence(fmd);
        }
    }

    ///////////
    // Locking
    ///////////

    public void lock(Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true); // have to sync or lock level always NONE
        try {
            lock(obj, _fc.getWriteLockLevel(), _fc.getLockTimeout(), call);
        } finally {
            endOperation();
        }
    }

    public void lock(Object obj, int level, int timeout, OpCallbacks call) {
        if (obj == null)
            return;

        beginOperation(true);
        try {
            assertActiveTransaction();

            StateManagerImpl sm = getStateManagerImpl(obj, true);
            if ((processArgument(OpCallbacks.OP_LOCK, obj, sm, call)
                & OpCallbacks.ACT_RUN) == 0)
                return;
            if (sm == null || !sm.isPersistent())
                return;

            _lm.lock(sm, level, timeout, null);
            sm.readLocked(level, level); // use same level for future write
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    public void lockAll(Collection objs, OpCallbacks call) {
        if (objs.isEmpty())
            return;

        beginOperation(true); // have to sync or lock level always NONE
        try {
            lockAll(objs, _fc.getWriteLockLevel(), _fc.getLockTimeout(),
                call);
        } finally {
            endOperation();
        }
    }

    public void lockAll(Collection objs, int level, int timeout,
        OpCallbacks call) {
        if (objs.isEmpty())
            return;
        if (objs.size() == 1) {
            lock(objs.iterator().next(), level, timeout, call);
            return;
        }

        beginOperation(true);
        try {
            assertActiveTransaction();

            Collection sms = new ArrayList(objs.size());
            Object obj;
            StateManagerImpl sm;
            for (Iterator itr = objs.iterator(); itr.hasNext();) {
                obj = itr.next();
                if (obj == null)
                    continue;

                sm = getStateManagerImpl(obj, true);
                if ((processArgument(OpCallbacks.OP_LOCK, obj, sm, call)
                    & OpCallbacks.ACT_RUN) == 0)
                    continue;
                if (sm != null && sm.isPersistent())
                    sms.add(sm);
            }

            _lm.lockAll(sms, level, timeout, null);
            for (Iterator itr = sms.iterator(); itr.hasNext();)
                ((StateManagerImpl) itr.next()).readLocked(level, level);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            endOperation();
        }
    }

    //////////////
    // Connection
    //////////////

    public boolean cancelAll() {
        // this method does not lock, since we want to allow a different
        // thread to be able to cancel on a locked-up persistence manager

        assertOpen();
        try {
            // if we're flushing, have to set rollback only -- do this before we
            // attempt to cancel, because otherwise the cancel might case the
            // transaction to complete before we have a chance to set the
            // rollback only flag
            if ((_flags & FLAG_STORE_FLUSHING) != 0)
                setRollbackOnlyInternal();
            return _store.cancelAll();
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new StoreException(re);
        }
    }

    public Object getConnection() {
        assertOpen();
        if (!_conf.supportedOptions().contains
            (_conf.OPTION_DATASTORE_CONNECTION))
            throw new UnsupportedException(_loc.get("conn-not-supported"));

        return _store.getClientConnection();
    }

    public boolean hasConnection() {
        assertOpen();
        return (_flags & FLAG_RETAINED_CONN) != 0;
    }

    /**
     * Tell store to retain connection if we haven't already.
     */
    private void retainConnection() {
        if ((_flags & FLAG_RETAINED_CONN) == 0) {
            _store.retainConnection();
            _flags |= FLAG_RETAINED_CONN;
        }
    }

    /**
     * Tell store to release connection if we have retained one.
     */
    private void releaseConnection() {
        if ((_flags & FLAG_RETAINED_CONN) != 0) {
            _store.releaseConnection();
            _flags &= ~FLAG_RETAINED_CONN;
        }
    }

    /////////
    // Cache
    /////////

    public Collection getManagedObjects() {
        beginOperation(false);
        try {
            return new ManagedObjectCollection(getManagedStates());
        } finally {
            endOperation();
        }
    }

    public Collection getTransactionalObjects() {
        beginOperation(false);
        try {
            return new ManagedObjectCollection(getTransactionalStates());
        } finally {
            endOperation();
        }
    }

    public Collection getPendingTransactionalObjects() {
        beginOperation(false);
        try {
            return new ManagedObjectCollection
                (getPendingTransactionalStates());
        } finally {
            endOperation();
        }
    }

    public Collection getDirtyObjects() {
        beginOperation(false);
        try {
            return new ManagedObjectCollection(getDirtyStates());
        } finally {
            endOperation();
        }
    }

    public boolean getOrderDirtyObjects() {
        return _orderDirty;
    }

    public void setOrderDirtyObjects(boolean order) {
        _orderDirty = order;
    }

    /**
     * Return a copy of all managed state managers.
     */
    protected Collection getManagedStates() {
        return _cache.copy();
    }

    /**
     * Return a copy of all transactional state managers.
     */
    protected Collection getTransactionalStates() {
        if (_transCache == null)
            return Collections.EMPTY_LIST;
        return _transCache.copy();
    }

    /**
     * Return a copy of all dirty state managers.
     */
    protected Collection getDirtyStates() {
        if (_transCache == null)
            return Collections.EMPTY_LIST;

        return _transCache.copyDirty();
    }

    /**
     * Return a copy of all state managers which will become
     * transactional upon the next transaction.
     */
    protected Collection getPendingTransactionalStates() {
        if (_pending == null)
            return Collections.EMPTY_LIST;
        return new ArrayList(_pending);
    }

    /**
     * Set the cached StateManager for the instance that had the given oid.
     * This method must not be called multiple times for new instances.
     *
     * @param id the id previously used by the instance
     * @param sm the state manager for the instance; if the state
     * manager is transient, we'll stop managing the instance;
     * if it has updated its oid, we'll re-cache under the new oid
     * @param status one of our STATUS constants describing why we're
     * setting the state manager
     */
    void setStateManager(Object id, StateManagerImpl sm, int status) {
        lock();
        try {
            switch (status) {
                case STATUS_INIT:
                    _cache.add(sm);
                    break;
                case STATUS_TRANSIENT:
                    _cache.remove(id, sm);
                    break;
                case STATUS_OID_ASSIGN:
                    _cache.assignObjectId(id, sm);
                    break;
                case STATUS_COMMIT_NEW:
                    _cache.commitNew(id, sm);
                    break;
                default:
                    throw new InternalException();
            }
        }
        finally {
            unlock();
        }
    }

    /**
     * Notify the broker that the given state manager should
     * be added to the set of instances involved in the current transaction.
     */
    void addToTransaction(StateManagerImpl sm) {
        // we only add clean instances now; dirty instances are added in
        // the setDirty callback
        if (sm.isDirty())
            return;

        lock();
        try {
            if (_transCache == null)
                _transCache = new TransactionalCache(_orderDirty);
            _transCache.addClean(sm);
        } finally {
            unlock();
        }
    }

    /**
     * Notify the persistence manager that the given state manager should
     * be removed from the set of instances involved in the current transaction.
     */
    void removeFromTransaction(StateManagerImpl sm) {
        lock();
        try {
            if (_transCache != null)
                _transCache.remove(sm);
            if (_derefCache != null && !sm.isPersistent())
                _derefCache.remove(sm);
        } finally {
            unlock();
        }
    }

    /**
     * Notification that the given instance has been dirtied. This
     * notification is given when an object first transitions to a dirty state,
     * and every time the object is modified by the user thereafter.
     */
    void setDirty(StateManagerImpl sm, boolean firstDirty) {
        if (sm.isPersistent())
            _flags |= FLAG_FLUSH_REQUIRED;

        if (_savepoints != null && !_savepoints.isEmpty()) {
            if (_savepointCache == null)
                _savepointCache = new HashSet();
            _savepointCache.add(sm);
        }

        if (firstDirty && sm.isTransactional()) {
            lock();
            try {
                // cache dirty instance
                if (_transCache == null)
                    _transCache = new TransactionalCache(_orderDirty);
                _transCache.addDirty(sm);

                // also record that the class is dirty
                if (sm.isNew()) {
                    if (_persistedClss == null)
                        _persistedClss = new HashSet();
                    _persistedClss.add(sm.getMetaData().getDescribedType());
                } else if (sm.isDeleted()) {
                    if (_deletedClss == null)
                        _deletedClss = new HashSet();
                    _deletedClss.add(sm.getMetaData().getDescribedType());
                } else {
                    if (_updatedClss == null)
                        _updatedClss = new HashSet();
                    _updatedClss.add(sm.getMetaData().getDescribedType());
                }

                // if tracking changes and this instance wasn't already dirty,
                // add to changed set; we use this for detecting instances that
                // enter the transaction during pre store
                if ((_flags & FLAG_PRESTORING) != 0) {
                    if (_transAdditions == null)
                        _transAdditions = new HashSet();
                    _transAdditions.add(sm);
                }
            } finally {
                unlock();
            }
        }
    }

    /**
     * Notify the broker that the given state manager should
     * be added to the set of instances that will become transactional
     * on the next transaction
     */
    void addToPendingTransaction(StateManagerImpl sm) {
        lock();
        try {
            if (_pending == null)
                _pending = new HashSet();
            _pending.add(sm);
        } finally {
            unlock();
        }
    }

    /**
     * Notify the persistence manager that the given state manager should
     * be removed from the set of instances involved in the next transaction.
     */
    void removeFromPendingTransaction(StateManagerImpl sm) {
        lock();
        try {
            if (_pending != null)
                _pending.remove(sm);
            if (_derefCache != null && !sm.isPersistent())
                _derefCache.remove(sm);
        } finally {
            unlock();
        }
    }

    /**
     * Add a dereferenced dependent object to the persistence manager's cache.
     * On flush, these objects will be deleted.
     */
    void addDereferencedDependent(StateManagerImpl sm) {
        lock();
        try {
            // if we're in the middle of flush and introducing more derefs
            // via instance callbacks, add them to the special additions set
            if ((_flags & FLAG_DEREFDELETING) != 0) {
                if (_derefAdditions == null)
                    _derefAdditions = new HashSet();
                _derefAdditions.add(sm);
            } else {
                if (_derefCache == null)
                    _derefCache = new HashSet();
                _derefCache.add(sm);
            }
        }
        finally {
            unlock();
        }
    }

    /**
     * Remove the given previously dereferenced dependent object from the
     * cache. It is now referenced.
     */
    void removeDereferencedDependent(StateManagerImpl sm) {
        lock();
        try {
            boolean removed = false;
            if (_derefAdditions != null)
                removed = _derefAdditions.remove(sm);
            if (!removed && (_derefCache == null || !_derefCache.remove(sm)))
                throw new InvalidStateException(_loc.get("not-derefed",
                    Exceptions.toString(sm.getManagedInstance()))).
                    setFailedObject(sm.getManagedInstance()).
                    setFatal(true);
        } finally {
            unlock();
        }
    }

    public void dirtyType(Class cls) {
        if (cls == null)
            return;

        beginOperation(false);
        try {
            if (_updatedClss == null)
                _updatedClss = new HashSet();
            _updatedClss.add(cls);
        } finally {
            endOperation();
        }
    }

    public Collection getPersistedTypes() {
        if (_persistedClss == null || _persistedClss.isEmpty())
            return Collections.EMPTY_LIST;
        return Collections.unmodifiableCollection(_persistedClss);
    }

    public Collection getUpdatedTypes() {
        if (_updatedClss == null || _updatedClss.isEmpty())
            return Collections.EMPTY_LIST;
        return Collections.unmodifiableCollection(_updatedClss);
    }

    public Collection getDeletedTypes() {
        if (_deletedClss == null || _deletedClss.isEmpty())
            return Collections.EMPTY_LIST;
        return Collections.unmodifiableCollection(_deletedClss);
    }

    ///////////
    // Closing
    ///////////

    public boolean isClosed() {
        return _closed != null;
    }

    public void close() {
        beginOperation(false);
        try {
            // throw an exception if closing in an active local trans
            if (!_managed && (_flags & FLAG_ACTIVE) != 0)
                throw new InvalidStateException(_loc.get("active"));

            // only close if not active; if active managed trans wait
            // for completion
            _flags |= FLAG_CLOSE_INVOKED;

            if ((_flags & FLAG_ACTIVE) == 0)
                free();
        } finally {
            endOperation();
        }
    }

    /**
     * Free the resources used by this persistence manager.
     */
    protected void free() {
        RuntimeException err = null;
        if ((_autoDetach & DETACH_CLOSE) != 0) {
            try {
                detachAllInternal(_call);
            } catch (RuntimeException re) {
                err = re;
            }
        }

        _sync = null;
        _userObjects = null;
        _cache.clear();
        _transCache = null;
        _persistedClss = null;
        _updatedClss = null;
        _deletedClss = null;
        _derefCache = null;
        _pending = null;
        _loader = null;
        _transEventManager = null;
        _lifeEventManager = null;

        OpenJPASavepoint save;
        while (_savepoints != null && !_savepoints.isEmpty()) {
            save =
                (OpenJPASavepoint) _savepoints.remove(_savepoints.size() - 1);
            save.release(false);
        }
        _savepoints = null;
        _savepointCache = null;

        if (_queries != null) {
            for (Iterator itr = _queries.iterator(); itr.hasNext();) {
                try {
                    ((Query) itr.next()).closeResources();
                } catch (RuntimeException re) {
                }
            }
            _queries = null;
        }

        if (_extents != null) {
            Extent e;
            for (Iterator itr = _extents.iterator(); itr.hasNext();) {
                e = (Extent) itr.next();
                try {
                    e.closeAll();
                } catch (RuntimeException re) {
                }
            }
            _extents = null;
        }

        try {
            releaseConnection();
        } catch (RuntimeException re) {
        }

        _lm.close();
        _store.close();
        _closed = new IllegalStateException();
        _flags = 0;

        if (err != null)
            throw err;
    }

    ///////////////////
    // Synchronization
    ///////////////////

    public void lock() {
        if (_lock != null)
            _lock.lock();
    }

    public void unlock() {
        if (_lock != null)
            _lock.unlock();
    }

    ////////////////////
    // State management
    ////////////////////

    public Object newInstance(Class cls) {
        assertOpen();

        if (cls.isInterface()) {
            ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
                getMetaData(cls, _loader, true);
            cls = meta.getInterfaceImpl();
        } else if (Modifier.isAbstract(cls.getModifiers()))
            throw new UnsupportedOperationException(_loc.get
                ("new-abstract", cls).getMessage());

        // 1.5 doesn't initialize classes without a true Class.forName
        if (!PCRegistry.isRegistered(cls)) {
            try {
                Class.forName(cls.getName(), true, cls.getClassLoader());
            } catch (Throwable t) {
            }
        }
        return PCRegistry.newInstance(cls, null, false);
    }

    public Object getObjectId(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcFetchObjectId();
        return null;
    }

    public int getLockLevel(Object o) {
        assertOpen();
        if (o == null)
            return LockLevels.LOCK_NONE;

        OpenJPAStateManager sm = getStateManager(o);
        if (sm == null)
            return LockLevels.LOCK_NONE;
        return getLockManager().getLockLevel(sm);
    }

    public Object getVersion(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcGetVersion();
        return null;
    }

    public boolean isDirty(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcIsDirty();
        return false;
    }

    public boolean isTransactional(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcIsTransactional();
        return false;
    }

    public boolean isPersistent(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcIsPersistent();
        return false;
    }

    public boolean isNew(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcIsNew();
        return false;
    }

    public boolean isDeleted(Object obj) {
        assertOpen();
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable) obj).pcIsDeleted();
        return false;
    }

    public boolean isDetached(Object obj) {
        if (!(obj instanceof PersistenceCapable))
            return false;

        PersistenceCapable pc = (PersistenceCapable) obj;
        Boolean detached = pc.pcIsDetached();
        if (detached != null)
            return detached.booleanValue();

        // last resort: instance is detached if it has a store record
        ClassMetaData meta = _conf.getMetaDataRepositoryInstance().
            getMetaData(pc.getClass(), _loader, true);
        Object oid = ApplicationIds.create(pc, meta);
        if (oid == null)
            return false;
        return find(oid, null, EXCLUDE_ALL, null, 0) != null;
    }

    public OpenJPAStateManager getStateManager(Object obj) {
        assertOpen();
        return getStateManagerImpl(obj, false);
    }

    /**
     * Return the state manager for the given instance, or null.
     *
     * @param assertThisContext if true, thow an exception if the given
     * object is managed by another broker
     */
    protected StateManagerImpl getStateManagerImpl(Object obj,
        boolean assertThisContext) {
        if (obj instanceof PersistenceCapable) {
            PersistenceCapable pc = (PersistenceCapable) obj;
            if (pc.pcGetGenericContext() == this)
                return (StateManagerImpl) pc.pcGetStateManager();
            if (assertThisContext && pc.pcGetGenericContext() != null)
                throw new UserException(_loc.get("not-managed",
                    Exceptions.toString(obj))).setFailedObject(obj);
        }
        return null;
    }

    /**
     * Return the state manager for the given oid.
     *
     * @param allowNew if true, objects made persistent in the current
     * transaction will be included in the search; if
     * multiple new objects match the given oid, it is
     * undefined which will be returned
     */
    protected StateManagerImpl getStateManagerImplById(Object oid,
        boolean allowNew) {
        return _cache.getById(oid, allowNew);
    }

    /**
     * Return the given instance as a {@link PersistenceCapable}.
     * If the instance is not manageable throw the proper exception.
     */
    protected PersistenceCapable assertPersistenceCapable(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof PersistenceCapable)
            return (PersistenceCapable) obj;

        // check for difference instances of the PersistenceCapable interface
        // and throw a better error that mentions the class loaders
        Class[] intfs = obj.getClass().getInterfaces();
        for (int i = 0; intfs != null && i < intfs.length; i++) {
            if (intfs[i].getName().equals(PersistenceCapable.class.getName())) {
                throw new UserException(_loc.get("pc-loader-different",
                    Exceptions.toString(obj),
                    PersistenceCapable.class.getClassLoader(),
                    intfs[i].getClassLoader())).setFailedObject(obj);
            }
        }

        // not enhanced
        throw new UserException(_loc.get("pc-cast",
            Exceptions.toString(obj))).setFailedObject(obj);
    }

    /////////
    // Utils
    /////////

    public void assertOpen() {
        if (_closed != null)
            throw new InvalidStateException(_loc.get("closed"), _closed).
                setFatal(true);
    }

    public void assertActiveTransaction() {
        if ((_flags & FLAG_ACTIVE) == 0)
            throw new NoTransactionException(_loc.get("not-active"));
    }

    /**
     * Throw exception if a transaction-related operation is attempted and
     * no transaction is active.
     */
    private void assertTransactionOperation() {
        if ((_flags & FLAG_ACTIVE) == 0)
            throw new InvalidStateException(_loc.get("not-active"));
    }

    public void assertNontransactionalRead() {
        if ((_flags & FLAG_ACTIVE) == 0 && !_nontransRead)
            throw new InvalidStateException(_loc.get("non-trans-read"));
    }

    public void assertWriteOperation() {
        if ((_flags & FLAG_ACTIVE) == 0 && (!_nontransWrite
            || (_autoDetach & DETACH_NONTXREAD) != 0))
            throw new NoTransactionException(_loc.get("write-operation"));
    }

    /**
     * Return an object not found exception containing nested exceptions
     * for all of the given failed objects.
     */
    private static ObjectNotFoundException newObjectNotFoundException
        (Collection failed) {
        Throwable[] t = new Throwable[failed.size()];
        int idx = 0;
        for (Iterator itr = failed.iterator(); itr.hasNext(); idx++)
            t[idx] = new ObjectNotFoundException(itr.next());
        return new ObjectNotFoundException(failed, t);
    }

    ////////////////////////////////
    // FindCallbacks implementation
    ////////////////////////////////

    public Object processArgument(Object oid) {
        return oid;
    }

    public Object processReturn(Object oid, OpenJPAStateManager sm) {
        return (sm == null) ? null : sm.getManagedInstance();
    }

    /**
     * Cache of managed objects.
     */
    private static class ManagedCache {

        private final Map _main; // oid -> sm
        private Map _conflicts = null; // conflict oid -> new sm
        private Map _news = null; // tmp id -> new sm
        private Collection _embeds = null; // embedded/non-persistent sms

        /**
         * Constructor; supply primary cache map.
         */
        public ManagedCache(Map cache) {
            _main = cache;
        }

        /**
         * Return the instance for the given oid, optionally allowing
         * new instances.
         */
        public StateManagerImpl getById(Object oid, boolean allowNew) {
            if (oid == null)
                return null;

            // check main cache for oid
            StateManagerImpl sm = (StateManagerImpl) _main.get(oid);
            StateManagerImpl sm2;
            if (sm != null) {
                // if it's a new instance, we know it's the only match, because
                // other pers instances override new instances in _cache
                if (sm.isNew())
                    return (allowNew) ? sm : null;
                if (!allowNew || !sm.isDeleted())
                    return sm;

                // sm is deleted; check conflict cache
                if (_conflicts != null) {
                    sm2 = (StateManagerImpl) _conflicts.get(oid);
                    if (sm2 != null)
                        return sm2;
                }
            }

            // at this point sm is null or deleted; check the new cache for
            // any matches. this allows us to match app id objects to new
            // instances without permanant oids
            if (allowNew && _news != null && !_news.isEmpty()) {
                sm2 = (StateManagerImpl) _news.get(oid);
                if (sm2 != null)
                    return sm2;
            }
            return sm;
        }

        /**
         * Call this method when a new state manager initializes itself.
         */
        public void add(StateManagerImpl sm) {
            if (!sm.isPersistent() || sm.isEmbedded()) {
                if (_embeds == null)
                    _embeds = new ReferenceHashSet(ReferenceHashSet.WEAK);
                _embeds.add(sm);
                return;
            }

            // initializing new instance; put in new cache because won't have
            // permanent oid yet
            if (sm.isNew()) {
                if (_news == null)
                    _news = new HashMap();
                _news.put(sm.getId(), sm);
                return;
            }

            // initializing persistent instance; put in main cache
            StateManagerImpl orig = (StateManagerImpl) _main.put
                (sm.getObjectId(), sm);
            if (orig != null) {
                _main.put(sm.getObjectId(), orig);
                throw new UserException(_loc.get("dup-load",
                    sm.getObjectId(), Exceptions.toString
                    (orig.getManagedInstance()))).
                    setFailedObject(sm.getManagedInstance());
            }
        }

        /**
         * Remove the given state manager from the cache when it transitions
         * to transient.
         */
        public void remove(Object id, StateManagerImpl sm) {
            // if it has a permanent oid, remove from main / conflict cache,
            // else remove from embedded/nontrans cache, and if not there
            // remove from new cache
            Object orig;
            if (sm.getObjectId() != null) {
                orig = _main.remove(id);
                if (orig != sm) {
                    if (orig != null)
                        _main.put(id, orig); // put back
                    if (_conflicts != null) {
                        orig = _conflicts.remove(id);
                        if (orig != null && orig != sm)
                            _conflicts.put(id, orig); // put back
                    }
                }
            } else
            if ((_embeds == null || !_embeds.remove(sm)) && _news != null) {
                orig = _news.remove(id);
                if (orig != null && orig != sm)
                    _news.put(id, orig); // put back
            }
        }

        /**
         * An embedded or nonpersistent managed instance has been persisted.
         */
        public void persist(StateManagerImpl sm) {
            if (_embeds != null)
                _embeds.remove(sm);
        }

        /**
         * A new instance has just been assigned a permanent oid.
         */
        public void assignObjectId(Object id, StateManagerImpl sm) {
            // if assigning oid, remove from new cache and put in primary; may
            // not be in new cache if another new instance had same id
            StateManagerImpl orig = (StateManagerImpl) _news.remove(id);
            if (orig != null && orig != sm)
                _news.put(id, orig); // put back

            // put in main cache, but make sure we don't replace another
            // instance with the same oid
            orig = (StateManagerImpl) _main.put(sm.getObjectId(), sm);
            if (orig != null) {
                _main.put(sm.getObjectId(), orig);
                if (!orig.isDeleted())
                    throw new UserException(_loc.get("dup-oid-assign",
                        sm.getObjectId(), Exceptions.toString
                        (sm.getManagedInstance()))).
                        setFailedObject(sm.getManagedInstance());

                // same oid as deleted instance; put in conflict cache
                if (_conflicts == null)
                    _conflicts = new HashMap();
                _conflicts.put(sm.getObjectId(), sm);
            }
        }

        /**
         * A new instance has committed; recache under permanent oid.
         */
        public void commitNew(Object id, StateManagerImpl sm) {
            // if the id didn't change, the instance was already assigned an
            // id, but it could have been in conflict cache
            StateManagerImpl orig;
            if (sm.getObjectId() == id) {
                orig = (_conflicts == null) ? null
                    : (StateManagerImpl) _conflicts.remove(id);
                if (orig == sm) {
                    orig = (StateManagerImpl) _main.put(id, sm);
                    if (orig != null && !orig.isDeleted()) {
                        _main.put(sm.getObjectId(), orig);
                        throw new UserException(_loc.get("dup-oid-assign",
                            sm.getObjectId(), Exceptions.toString
                            (sm.getManagedInstance()))).setFailedObject
                            (sm.getManagedInstance()).setFatal(true);
                    }
                }
                return;
            }

            // oid changed, so it must previously have been a new instance
            // without an assigned oid.  remove it from the new cache; ok if
            // we end up removing another instance with same id
            if (_news != null)
                _news.remove(id);

            // and put into main cache now that id is asssigned
            orig = (StateManagerImpl) _main.put(sm.getObjectId(), sm);
            if (orig != null && orig != sm && !orig.isDeleted()) {
                // put back orig and throw error
                _main.put(sm.getObjectId(), orig);
                throw new UserException(_loc.get("dup-oid-assign",
                    sm.getObjectId(), Exceptions.toString
                    (sm.getManagedInstance()))).setFailedObject
                    (sm.getManagedInstance()).setFatal(true);
            }
        }

        /**
         * Return a copy of all cached persistent objects.
         */
        public Collection copy() {
            // proxies not included here because the state manager is always
            // present in other caches too

            int size = _main.size();
            if (_conflicts != null)
                size += _conflicts.size();
            if (_news != null)
                size += _news.size();
            if (_embeds != null)
                size += _embeds.size();
            if (size == 0)
                return Collections.EMPTY_LIST;

            List copy = new ArrayList(size);
            for (Iterator itr = _main.values().iterator(); itr.hasNext();)
                copy.add(itr.next());
            if (_conflicts != null && !_conflicts.isEmpty())
                for (Iterator itr = _conflicts.values().iterator();
                    itr.hasNext();)
                    copy.add(itr.next());
            if (_news != null && !_news.isEmpty())
                for (Iterator itr = _news.values().iterator(); itr.hasNext();)
                    copy.add(itr.next());
            if (_embeds != null && !_embeds.isEmpty())
                for (Iterator itr = _embeds.iterator(); itr.hasNext();)
                    copy.add(itr.next());
            return copy;
        }

        /**
         * Clear the cache.
         */
        public void clear() {
            _main.clear();
            if (_conflicts != null)
                _conflicts.clear();
            if (_news != null)
                _news.clear();
            if (_embeds != null)
                _embeds.clear();
        }

        /**
         * Clear new instances without permanent oids.
         */
        public void clearNew() {
            if (_news != null)
                _news.clear();
        }
    }

    /**
     * Transactional cache that holds soft refs to clean instances.
     */
    private static class TransactionalCache
        implements Set {

        private final boolean _orderDirty;
        private Set _dirty = null;
        private Set _clean = null;

        public TransactionalCache(boolean orderDirty) {
            _orderDirty = orderDirty;
        }

        /**
         * Return a copy of all transactional state managers.
         */
        public Collection copy() {
            if (isEmpty())
                return Collections.EMPTY_LIST;

            // size may not be entirely accurate due to refs expiring, so
            // manually copy each object; doesn't matter this way if size too
            // big by some
            List copy = new ArrayList(size());
            if (_dirty != null)
                for (Iterator itr = _dirty.iterator(); itr.hasNext();)
                    copy.add(itr.next());
            if (_clean != null)
                for (Iterator itr = _clean.iterator(); itr.hasNext();)
                    copy.add(itr.next());
            return copy;
        }

        /**
         * Return a copy of all dirty state managers.
         */
        public Collection copyDirty() {
            if (_dirty == null || _dirty.isEmpty())
                return Collections.EMPTY_LIST;
            return new ArrayList(_dirty);
        }

        /**
         * Transfer the given instance from the dirty cache to the clean cache.
         */
        public void flushed(StateManagerImpl sm) {
            if (sm.isDirty() && _dirty != null && _dirty.remove(sm))
                addCleanInternal(sm);
        }

        /**
         * Add the given instance to the clean cache.
         */
        public void addClean(StateManagerImpl sm) {
            if (addCleanInternal(sm) && _dirty != null)
                _dirty.remove(sm);
        }

        private boolean addCleanInternal(StateManagerImpl sm) {
            if (_clean == null)
                _clean = new ReferenceHashSet(ReferenceHashSet.SOFT);
            return _clean.add(sm);
        }

        /**
         * Add the given instance to the dirty cache.
         */
        public void addDirty(StateManagerImpl sm) {
            if (_dirty == null) {
                if (_orderDirty)
                    _dirty = MapBackedSet.decorate(new LinkedMap());
                else
                    _dirty = new HashSet();
            }
            if (_dirty.add(sm))
                removeCleanInternal(sm);
        }

        /**
         * Remove the given instance from the cache.
         */
        public boolean remove(StateManagerImpl sm) {
            return removeCleanInternal(sm)
                || (_dirty != null && _dirty.remove(sm));
        }

        private boolean removeCleanInternal(StateManagerImpl sm) {
            return _clean != null && _clean.remove(sm);
        }

        public Iterator iterator() {
            IteratorChain chain = new IteratorChain();
            if (_dirty != null && !_dirty.isEmpty())
                chain.addIterator(_dirty.iterator());
            if (_clean != null && !_clean.isEmpty())
                chain.addIterator(_clean.iterator());
            return chain;
        }

        public boolean contains(Object obj) {
            return (_dirty != null && _dirty.contains(obj))
                || (_clean != null && _clean.contains(obj));
        }

        public boolean containsAll(Collection coll) {
            for (Iterator itr = coll.iterator(); itr.hasNext();)
                if (!contains(itr.next()))
                    return false;
            return true;
        }

        public void clear() {
            if (_dirty != null)
                _dirty.clear();
            if (_clean != null)
                _clean.clear();
        }

        public boolean isEmpty() {
            return (_dirty == null || _dirty.isEmpty())
                && (_clean == null || _clean.isEmpty());
        }

        public int size() {
            int size = 0;
            if (_dirty != null)
                size += _dirty.size();
            if (_clean != null)
                size += _clean.size();
            return size;
        }

        public boolean add(Object obj) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection coll) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object obj) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection coll) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray(Object[] arr) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Unique id for state managers of new datastore instances without assigned
     * object ids.
     */
    private static class StateManagerId
        implements Serializable {

        public static final String STRING_PREFIX = "openjpasm:";

        private static long _generator = 0;

        private final int _bhash;
        private final long _id;

        public static StateManagerId newInstance(Broker b) {
            return new StateManagerId(System.identityHashCode(b), _generator++);
        }

        private StateManagerId(int bhash, long id) {
            _bhash = bhash;
            _id = id;
        }

        public StateManagerId(String str) {
            str = str.substring(STRING_PREFIX.length());
            int idx = str.indexOf(':');
            _bhash = Integer.parseInt(str.substring(0, idx));
            _id = Long.parseLong(str.substring(idx + 1));
        }

        public boolean equals(Object other) {
            if (other == this)
                return true;
            if (!(other instanceof StateManagerId))
                return false;
            StateManagerId sid = (StateManagerId) other;
            return _bhash == sid._bhash && _id == sid._id;
        }

        public int hashCode() {
            return (int) (_id ^ (_id >>> 32));
        }

        public String toString() {
            return STRING_PREFIX + _bhash + ":" + _id;
        }
    }

    /**
     * Collection type that holds state managers but whose interface deals
     * with the corresponding managed objects.
     */
    private static class ManagedObjectCollection
        extends AbstractCollection {

        private final Collection _states;

        public ManagedObjectCollection(Collection states) {
            _states = states;
        }

        public Collection getStateManagers() {
            return _states;
        }

        public int size() {
            return _states.size();
        }

        public Iterator iterator() {
            return new Iterator() {
                private final Iterator _itr = _states.iterator();

                public boolean hasNext() {
                    return _itr.hasNext();
                }

                public Object next() {
                    return ((OpenJPAStateManager) _itr.next()).
                        getManagedInstance();
                }

                public void remove() {
                    throw new UnsupportedException();
                }
            };
        }
    }

    /**
     * Helper class to implement JCA interfaces. This is placed in a
     * separate class so that its methods do not interfere with the
     * persistence manager APIs.
     */
    private class JCAHelper
        implements Interaction, ResultSetInfo, ConnectionMetaData {
        ///////////////////////////////////////////
        // Implementation of Interaction interface
        ///////////////////////////////////////////

        public void clearWarnings() {
        }

        public Record execute(InteractionSpec spec, Record input)
            throws ResourceException {
            throw new NotSupportedException("execute");
        }

        public boolean execute(InteractionSpec spec, Record input,
            Record output)
            throws ResourceException {
            throw new NotSupportedException("execute");
        }

        public Connection getConnection() {
            return BrokerImpl.this;
        }

        public ResourceWarning getWarnings() {
            return null;
        }

        public void close() {
        }

        /////////////////////////////////////////////
        // Implementation of ResultSetInfo interface
        /////////////////////////////////////////////

        public boolean deletesAreDetected(int type) {
            return true;
        }

        public boolean insertsAreDetected(int type) {
            return true;
        }

        public boolean othersDeletesAreVisible(int type) {
            return true;
        }

        public boolean othersInsertsAreVisible(int type) {
            return true;
        }

        public boolean othersUpdatesAreVisible(int type) {
            return true;
        }

        public boolean ownDeletesAreVisible(int type) {
            return true;
        }

        public boolean ownInsertsAreVisible(int type) {
            return true;
        }

        public boolean ownUpdatesAreVisible(int type) {
            return true;
        }

        public boolean supportsResultSetType(int type) {
            return true;
        }

        public boolean supportsResultTypeConcurrency(int type,
            int concurrency) {
            return true;
        }

        public boolean updatesAreDetected(int type) {
            return true;
        }

        ///////////////////////////////////////////////////
        // Implementation of ConnectionMetaData interface
        ///////////////////////////////////////////////////

        public String getEISProductName() {
            return _conf.getConnectionDriverName();
        }

        public String getEISProductVersion() {
            return "";
        }

        public String getUserName() {
            return _user;
        }
    }
}

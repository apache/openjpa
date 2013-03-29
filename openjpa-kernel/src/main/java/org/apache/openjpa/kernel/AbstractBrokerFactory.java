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

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.collections.set.MapBackedSet;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAVersion;
import org.apache.openjpa.conf.BrokerValue;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.datacache.DataCacheStoreManager;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.ManagedClassSubclasser;
import org.apache.openjpa.event.BrokerFactoryEvent;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashSet;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UserException;

/**
 * Abstract implementation of the {@link BrokerFactory}
 * that must be subclassed for a specific runtime.
 *
 * @author Abe White
 */
public abstract class AbstractBrokerFactory
    implements BrokerFactory {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractBrokerFactory.class);

    // static mapping of configurations to pooled broker factories
    private static final Map _pool = Collections.synchronizedMap(new HashMap());

    // configuration
    private final OpenJPAConfiguration _conf;
    private transient boolean _readOnly = false;
    private transient boolean _closed = false;
    private transient RuntimeException _closedException = null;
    private Map _userObjects = null;

    // internal lock: spec forbids synchronization on this object
    private final ReentrantLock _lock = new ReentrantLock();

    // maps global transactions to associated brokers
    private transient ConcurrentHashMap _transactional
        = new ConcurrentHashMap();

    // weak-ref tracking of open brokers
    private transient Set _brokers;

    // cache the class names loaded from the persistent classes property so
    // that we can re-load them for each new broker
    private transient Collection _pcClassNames = null;
    private transient Collection _pcClassLoaders = null;
    private transient boolean _persistentTypesLoaded = false;

    // lifecycle listeners to pass to each broker
    private transient Map _lifecycleListeners = null;

    // transaction listeners to pass to each broker
    private transient List _transactionListeners = null;

    // key under which this instance can be stored in the broker pool
    // and later identified
    private Object _poolKey;

    /**
     * Return an internal factory pool key for the given configuration.
     *
     * @since 1.1.0
     */
    protected static Object toPoolKey(Map map) {
        Object key = Configurations.getProperty("Id", map);
        return ( key != null) ? key : map;
    }

    /**
     * Register <code>factory</code> in the pool under <code>key</code>.
     *
     * @since 1.1.0
     */
    protected static void pool(Object key, AbstractBrokerFactory factory) {
        synchronized(_pool) {
            _pool.put(key, factory);
            factory.setPoolKey(key);
            factory.makeReadOnly();
        }
    }

    /**
     * Return the pooled factory matching the given key, or null
     * if none. The key must be of the form created by {@link #getPoolKey}.
     */
    public static AbstractBrokerFactory getPooledFactoryForKey(Object key) {
        return (AbstractBrokerFactory) _pool.get(key);
    }

    /**
     * Constructor. Configuration must be provided on construction.
     */
    protected AbstractBrokerFactory(OpenJPAConfiguration config) {
        _conf = config;
        _brokers = newBrokerSet();
        getPcClassLoaders();
    }

    /**
     * Return the configuration for this factory.
     */
    public OpenJPAConfiguration getConfiguration() {
        return _conf;
    }

    public Broker newBroker() {
        return newBroker(_conf.getConnectionUserName(),
            _conf.getConnectionPassword());
    }

    public Broker newBroker(String user, String pass) {
        return newBroker(user, pass, _conf.isTransactionModeManaged(),
            _conf.getConnectionRetainModeConstant());
    }

    public Broker newBroker(boolean managed, int connRetainMode) {
        return newBroker(_conf.getConnectionUserName(),
            _conf.getConnectionPassword(), managed, connRetainMode);
    }

    public Broker newBroker(String user, String pass, boolean managed,
        int connRetainMode) {
        return newBroker(user, pass, managed, connRetainMode, true);
    }

    public Broker newBroker(String user, String pass, boolean managed,
        int connRetainMode, boolean findExisting) {
        try {
            assertOpen();
            makeReadOnly();

            BrokerImpl broker = null;
            if (findExisting)
                broker = findBroker(user, pass, managed);
            if (broker == null) {
                broker = newBrokerImpl(user, pass);
                initializeBroker(managed, connRetainMode, broker, false);
            }
            return broker;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        }
    }

    void initializeBroker(boolean managed, int connRetainMode,
        BrokerImpl broker, boolean fromDeserialization) {
        assertOpen();
        makeReadOnly();

        // decorate the store manager for data caching and custom
        // result object providers; always make sure it's a delegating
        // store manager, because it's easier for users to deal with
        // that way
        StoreManager sm = newStoreManager();
        DelegatingStoreManager dsm = null;
        if (_conf.getDataCacheManagerInstance().getSystemDataCache()
            != null)
            dsm = new DataCacheStoreManager(sm);
        dsm = new ROPStoreManager((dsm == null) ? sm : dsm);

        broker.initialize(this, dsm, managed, connRetainMode,
            fromDeserialization);
        if (!fromDeserialization)
            addListeners(broker);

        // if we're using remote events, register the event manager so
        // that it can broadcast commit notifications from the broker
        RemoteCommitEventManager remote = _conf.
            getRemoteCommitEventManager();
        if (remote.areRemoteEventsEnabled())
            broker.addTransactionListener(remote);

        loadPersistentTypes(broker.getClassLoader());
        _brokers.add(broker);
        _conf.setReadOnly(Configuration.INIT_STATE_FROZEN);
    }

    /**
     * Add factory-registered lifecycle listeners to the broker.
     */
    protected void addListeners(BrokerImpl broker) {
        if (_lifecycleListeners != null && !_lifecycleListeners.isEmpty()) {
            Map.Entry entry;
            for (Iterator itr = _lifecycleListeners.entrySet().iterator();
                itr.hasNext();) {
                entry = (Map.Entry) itr.next();
                broker.addLifecycleListener(entry.getKey(), (Class[])
                    entry.getValue());
            }
        }

        if (_transactionListeners != null && !_transactionListeners.isEmpty()) {
            for (Iterator itr = _transactionListeners.iterator();
                itr.hasNext(); ) {
                broker.addTransactionListener(itr.next());
            }
        }
    }

    /**
     * Load the configured persistent classes list. Performed automatically
     * whenever a broker is created.
     */
    public void loadPersistentTypes(ClassLoader envLoader) {
        // if we've loaded the persistent types and the class name list
        // is empty, then we can simply return. Note that there is a
        // potential threading scenario in which _persistentTypesLoaded is
        // false when read, but the work to populate _pcClassNames has
        // already been done. This is ok; _pcClassNames can tolerate
        // concurrent access, so the worst case is that the list is
        // persistent type data is processed multiple times, which this
        // algorithm takes into account.
        if (_persistentTypesLoaded && _pcClassNames.isEmpty())
            return;

        // cache persistent type names if not already
        ClassLoader loader = _conf.getClassResolverInstance().
            getClassLoader(getClass(), envLoader);
        Collection toRedefine = new ArrayList();
        if (!_persistentTypesLoaded) {
            Collection clss = _conf.getMetaDataRepositoryInstance().
                loadPersistentTypes(false, loader);
            if (clss.isEmpty())
                _pcClassNames = Collections.EMPTY_SET;
            else {
                Collection c = new ArrayList(clss.size());
                for (Iterator itr = clss.iterator(); itr.hasNext();) {
                    Class cls = (Class) itr.next();
                    c.add(cls.getName());
                    if (needsSub(cls))
                        toRedefine.add(cls);
                }
                getPcClassLoaders().add(loader);
                _pcClassNames = c;
            }
            _persistentTypesLoaded = true;
        } else {
            // reload with this loader
            if (getPcClassLoaders().add(loader)) {
                for (Iterator itr = _pcClassNames.iterator(); itr.hasNext();) {
                    try {
                        Class cls =
                            Class.forName((String) itr.next(), true, loader);
                        if (needsSub(cls))
                            toRedefine.add(cls);
                    } catch (Throwable t) {
                        _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME)
                            .warn(null, t);
                    }
                }
            }
        }

        // get the ManagedClassSubclasser into the loop
        ManagedClassSubclasser.prepareUnenhancedClasses(
            _conf, toRedefine, envLoader);
    }

    private boolean needsSub(Class cls) {
        return !cls.isInterface()
            && !PersistenceCapable.class.isAssignableFrom(cls);
    }

    public void addLifecycleListener(Object listener, Class[] classes) {
        lock();
        try {
            assertOpen();
            if (_lifecycleListeners == null)
                _lifecycleListeners = new HashMap(7);
            _lifecycleListeners.put(listener, classes);
        } finally {
            unlock();
        }
    }

    public void removeLifecycleListener(Object listener) {
        lock();
        try {
            assertOpen();
            if (_lifecycleListeners != null)
                _lifecycleListeners.remove(listener);
        } finally {
            unlock();
        }
    }

    public void addTransactionListener(Object listener) {
        lock();
        try {
            assertOpen();
            if (_transactionListeners == null)
                _transactionListeners = new LinkedList();
            _transactionListeners.add(listener);
        } finally {
            unlock();
        }
    }

    public void removeTransactionListener(Object listener) {
        lock();
        try {
            assertOpen();
            if (_transactionListeners != null)
                _transactionListeners.remove(listener);
        } finally {
            unlock();
        }
    }

    /**
     * Returns true if this broker factory is closed.
     */
    public boolean isClosed() {
        return _closed;
    }

    public void close() {
        lock();
        try {
            assertOpen();
            assertNoActiveTransaction();

            // remove from factory pool
            synchronized (_pool) {
                if (_pool.get(_poolKey) == this)
                    _pool.remove(_poolKey);
            }

            // close all brokers
            Broker broker;
            for (Iterator itr = _brokers.iterator(); itr.hasNext();) {
                broker = (Broker) itr.next();
                // Check for null because _brokers may contain weak references
                if ((broker != null) && (!broker.isClosed()))
                    broker.close();
            }

            if(_conf.metaDataRepositoryAvailable()) {
                // remove metadata repository from listener list
                PCRegistry.removeRegisterClassListener
                    (_conf.getMetaDataRepositoryInstance());
            }

            _conf.close();
            _closed = true;
            Log log = _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
            if (log.isTraceEnabled())
                _closedException = new IllegalStateException();
        } finally {
            unlock();
        }
    }

    /**
     * Subclasses should override this method to add a <code>Platform</code>
     * property listing the runtime platform, such as:
     * <code>OpenJPA JDBC Edition: Oracle Database</code>
     */
    public Properties getProperties() {
        // required props are VendorName and VersionNumber
        Properties props = new Properties();
        props.setProperty("VendorName", OpenJPAVersion.VENDOR_NAME);
        props.setProperty("VersionNumber", OpenJPAVersion.VERSION_NUMBER);
        props.setProperty("VersionId", OpenJPAVersion.VERSION_ID);
        return props;
    }

    public Object getUserObject(Object key) {
        lock();
        try {
            assertOpen();
            return (_userObjects == null) ? null : _userObjects.get(key);
        } finally {
            unlock();
        }
    }

    public Object putUserObject(Object key, Object val) {
        lock();
        try {
            assertOpen();
            if (val == null)
                return (_userObjects == null) ? null : _userObjects.remove(key);

            if (_userObjects == null)
                _userObjects = new HashMap();
            return _userObjects.put(key, val);
        } finally {
            unlock();
        }
    }

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    /**
     * Replaces the factory with this JVMs pooled version if it exists. Also
     * freezes the factory.
     */
    protected Object readResolve()
        throws ObjectStreamException {
        AbstractBrokerFactory factory = getPooledFactoryForKey(_poolKey);
        if (factory != null)
            return factory;

        // reset these transient fields to empty values
        _transactional = new ConcurrentHashMap();
        _brokers = newBrokerSet();

        // turn off logging while de-serializing BrokerFactory
        String saveLogConfig = _conf.getLog();
        _conf.setLog("none");
        makeReadOnly();
        // re-enable any logging which was in effect
        _conf.setLog(saveLogConfig);
        
        return this;
    }

    private Set newBrokerSet() {
        BrokerValue bv;
        if (_conf instanceof OpenJPAConfigurationImpl)
            bv = ((OpenJPAConfigurationImpl) _conf).brokerPlugin;
        else
            bv = (BrokerValue) _conf.getValue(BrokerValue.KEY);

        if (FinalizingBrokerImpl.class.isAssignableFrom(
            bv.getTemplateBrokerType(_conf))) {
            return MapBackedSet.decorate(new ConcurrentHashMap(),
                new Object() { });
        } else {
            return new ConcurrentReferenceHashSet(
                ConcurrentReferenceHashSet.WEAK);
        }
    }

    ////////////////////////
    // Methods for Override
    ////////////////////////

    /**
     * Return a new StoreManager for this runtime. Note that the instance
     * returned here may be wrapped before being passed to the
     * {@link #newBroker} method.
     */
    protected abstract StoreManager newStoreManager();

    /**
     * Find a pooled broker, or return null if none. If using
     * managed transactions, looks for a transactional broker;
     * otherwise returns null by default. This method will be called before
     * {@link #newStoreManager} so that factory subclasses implementing
     * pooling can return a matching manager before a new {@link StoreManager}
     * is created.
     */
    protected BrokerImpl findBroker(String user, String pass, boolean managed) {
        if (managed)
            return findTransactionalBroker(user, pass);
        return null;
    }

    /**
     * Return a broker configured with the proper settings.
     * By default, this method constructs a new
     * BrokerImpl of the class set for this factory.
     */
    protected BrokerImpl newBrokerImpl(String user, String pass) {
        BrokerImpl broker = _conf.newBrokerInstance(user, pass);
        if (broker == null)
            throw new UserException(_loc.get("no-broker-class",
                _conf.getBrokerImpl()));

        return broker;
    }

    /**
     * Setup transient state used by this factory based on the
     * current configuration, which will subsequently be locked down. This
     * method will be called before the first broker is requested,
     * and will be re-called each time the factory is deserialized into a JVM
     * that has no configuration for this data store.
     */
    protected void setup() {
    }

    /////////////
    // Utilities
    /////////////

    /**
     * Find a managed runtime broker associated with the
     * current transaction, or returns null if none.
     */
    protected BrokerImpl findTransactionalBroker(String user, String pass) {
        Transaction trans;
        ManagedRuntime mr = _conf.getManagedRuntimeInstance();
        Object txKey;
        try {
            trans = mr.getTransactionManager().
                getTransaction();
            txKey = mr.getTransactionKey();

            if (trans == null
                || trans.getStatus() == Status.STATUS_NO_TRANSACTION
                || trans.getStatus() == Status.STATUS_UNKNOWN)
                return null;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        }

        Collection brokers = (Collection) _transactional.get(txKey);
        if (brokers != null) {
            // we don't need to synchronize on brokers since one JTA transaction
            // can never be active on multiple concurrent threads.
            BrokerImpl broker;
            for (Iterator itr = brokers.iterator(); itr.hasNext();) {
                broker = (BrokerImpl) itr.next();
                if (StringUtils.equals(broker.getConnectionUserName(),
                    user) && StringUtils.equals
                    (broker.getConnectionPassword(), pass))
                    return broker;
            }
        }
        return null;
    }

    /**
     * Configures the given broker with the current factory option settings.
     */
    protected void configureBroker(BrokerImpl broker) {
        broker.setOptimistic(_conf.getOptimistic());
        broker.setNontransactionalRead(_conf.getNontransactionalRead());
        broker.setNontransactionalWrite(_conf.getNontransactionalWrite());
        broker.setRetainState(_conf.getRetainState());
        broker.setRestoreState(_conf.getRestoreStateConstant());
        broker.setAutoClear(_conf.getAutoClearConstant());
        broker.setIgnoreChanges(_conf.getIgnoreChanges());
        broker.setMultithreaded(_conf.getMultithreaded());
        broker.setAutoDetach(_conf.getAutoDetachConstant());
        broker.setDetachState(_conf.getDetachStateInstance().
            getDetachState());
    }

    /**
     * Freezes the configuration of this factory.
     */
    public void makeReadOnly() {
        if (_readOnly)
            return;

        lock();
        try {
            // check again
            if (_readOnly)
                return;
            _readOnly = true;

            Log log = _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
            if (log.isInfoEnabled())
                log.info(getFactoryInitializationBanner());
            if (log.isTraceEnabled()) {
                Map props = _conf.toProperties(true);
                String lineSep = J2DoPrivHelper.getLineSeparator();
                StringBuffer buf = new StringBuffer();
                Map.Entry entry;
                for (Iterator itr = props.entrySet().iterator();
                    itr.hasNext();) {
                    entry = (Map.Entry) itr.next();
                    buf.append(entry.getKey()).append(": ").
                        append(entry.getValue());
                    if (itr.hasNext())
                        buf.append(lineSep);
                }
                log.trace(_loc.get("factory-properties", buf.toString()));
            }

            // setup transient state
            setup();

            // register the metdata repository to auto-load persistent types
            // and make sure types are enhanced
            MetaDataRepository repos = _conf.getMetaDataRepositoryInstance();
            repos.setValidate(repos.VALIDATE_RUNTIME, true);
            repos.setResolve(repos.MODE_MAPPING_INIT, true);
            PCRegistry.addRegisterClassListener(repos);

            // freeze underlying configuration and eagerly initialize to
            // avoid synchronization
            _conf.setReadOnly(Configuration.INIT_STATE_FREEZING);
            _conf.instantiateAll();

            // fire an event for all the broker factory listeners
            // registered on the configuration.
            _conf.getBrokerFactoryEventManager().fireEvent(
                new BrokerFactoryEvent(this,
                    BrokerFactoryEvent.BROKER_FACTORY_CREATED));
        } finally {
            unlock();
        }
    }

    /**
     * Return an object to be written to the log when this broker factory
     * initializes. This happens after the configuration is fully loaded.
     */
    protected Object getFactoryInitializationBanner() {
        return _loc.get("factory-init", OpenJPAVersion.VERSION_NUMBER);
    }

    /**
     * Throw an exception if the factory is closed.  The exact message and
     * content of the exception varies whether TRACE is enabled or not.
     */
    private void assertOpen() {
        if (_closed) {
            if (_closedException == null)  // TRACE not enabled
                throw new InvalidStateException(_loc
                        .get("closed-factory-notrace"));
            else
                throw new InvalidStateException(_loc.get("closed-factory"))
                        .setCause(_closedException);
        }
    }

    ////////////////////
    // Broker utilities
    ////////////////////

    /**
     * Throws a {@link UserException} if a transaction is active. The thrown
     * exception will contain all the Brokers with active transactions as
     * failed objects in the nested exceptions.
     */
    private void assertNoActiveTransaction() {
        Collection excs;
        if (_transactional.isEmpty())
            return;

        excs = new ArrayList(_transactional.size());
        for (Iterator trans = _transactional.values().iterator();
            trans.hasNext();) {
            Collection brokers = (Collection) trans.next();
            for (Iterator itr = brokers.iterator(); itr.hasNext();) {
                excs.add(new InvalidStateException(_loc.get("active")).
                    setFailedObject(itr.next()));
            }
        }

        if (!excs.isEmpty())
            throw new InvalidStateException(_loc.get("nested-exceps")).
                setNestedThrowables((Throwable[]) excs.toArray
                    (new Throwable[excs.size()]));
    }

    /**
     * Synchronize the given broker with a managed transaction,
     * optionally starting one if none is in progress.
     *
     * @return true if synched with transaction, false otherwise
     */
    boolean syncWithManagedTransaction(BrokerImpl broker, boolean begin) {
        Transaction trans;
        try {
            ManagedRuntime mr = broker.getManagedRuntime();
            TransactionManager tm = mr.getTransactionManager();
            trans = tm.getTransaction();
            if (trans != null
                && (trans.getStatus() == Status.STATUS_NO_TRANSACTION
                || trans.getStatus() == Status.STATUS_UNKNOWN))
                trans = null;

            if (trans == null && begin) {
                tm.begin();
                trans = tm.getTransaction();
            } else if (trans == null)
                return false;

            // synch broker and trans
            trans.registerSynchronization(broker);

            // we don't need to synchronize on brokers or guard against multiple
            // threads using the same trans since one JTA transaction can never
            // be active on multiple concurrent threads.
            Object txKey = mr.getTransactionKey();
            Collection brokers = (Collection) _transactional.get(txKey);
            
            if (brokers == null) {
                brokers = new ArrayList(2);
                _transactional.put(txKey, brokers);
                trans.registerSynchronization(new RemoveTransactionSync(txKey));
            }
            brokers.add(broker);
            
            return true;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Returns a set of all the open brokers associated with this factory. The
     * returned set is unmodifiable, and may contain null references.
     */
    public Collection getOpenBrokers() {
        return Collections.unmodifiableCollection(_brokers);
    }

    /**
     * Release <code>broker</code> from any internal data structures. This
     * is invoked by <code>broker</code> after the broker is fully closed.
     *
     * @since 1.1.0
     */
    protected void releaseBroker(BrokerImpl broker) {
        _brokers.remove(broker);
    }

    /**
     * @return a key that can be used to obtain this broker factory from the
     * pool at a later time.
     *
     * @since 1.1.0
     */
    public Object getPoolKey() {
        return _poolKey;
    }

    /**
     * Set a key that can be used to obtain this broker factory from the
     * pool at a later time.
     *
     * @since 1.1.0
     */
    void setPoolKey(Object key) {
        _poolKey = key;
    }

    /**
     * Simple synchronization listener to remove completed transactions
     * from our cache.
     */
    private class RemoveTransactionSync
        implements Synchronization {

        private final Object _trans;

        public RemoveTransactionSync(Object trans) {
            _trans = trans;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            _transactional.remove (_trans);
		}
	}
    
    /**
     * Method insures that deserialized EMF has this reference re-instantiated
     */
    private Collection getPcClassLoaders() {
       if (_pcClassLoaders == null)
         _pcClassLoaders = new ConcurrentReferenceHashSet(
             ConcurrentReferenceHashSet.WEAK);
          
       return _pcClassLoaders;
    }
}

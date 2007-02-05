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

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAVersion;
import org.apache.openjpa.datacache.DataCacheStoreManager;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashSet;
import org.apache.openjpa.lib.util.concurrent.ReentrantLock;
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
    private static final Map _pool = Collections.synchronizedMap
        (new HashMap());

    // configuration
    private final OpenJPAConfiguration _conf;
    private transient boolean _readOnly = false;
    private transient RuntimeException _closed = null;
    private Map _userObjects = null;

    // internal lock: spec forbids synchronization on this object
    private final ReentrantLock _lock = new ReentrantLock();

    // maps global transactions to associated brokers
    private transient Map _transactional = new HashMap();

    // weak-ref tracking of open brokers
    private transient Collection _brokers = new ConcurrentReferenceHashSet
        (ConcurrentReferenceHashSet.WEAK);

    // cache the class names loaded from the persistent classes property so
    // that we can re-load them for each new broker
    private transient Collection _pcClassNames = null;
    private transient Collection _pcClassLoaders = null;

    // lifecycle listeners to pass to each broker
    private transient Map _lifecycleListeners = null;

    /**
     * Return the pooled factory matching the given configuration, or null
     * if none.
     */
    public static AbstractBrokerFactory getPooledFactory(
        OpenJPAConfiguration conf) {
        return (AbstractBrokerFactory) _pool.get(toPoolKey(conf));
    }

    private static Map toPoolKey(OpenJPAConfiguration conf) {
        return conf.toProperties(false);
    }

    /**
     * Return the pooled factory matching the given configuration data, or null
     * if none.
     */
    protected static AbstractBrokerFactory getPooledFactory(Map map) {
        return (AbstractBrokerFactory) _pool.get(map);
    }

    /**
     * Constructor. Configuration must be provided on construction.
     */
    protected AbstractBrokerFactory(OpenJPAConfiguration config) {
        _conf = config;
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

                broker = newBrokerImpl(user, pass);
                broker.initialize(this, dsm, managed, connRetainMode);
                addLifecycleListeners(broker);

                // if we're using remote events, register the event manager so
                // that it can broadcast commit notifications from the broker
                RemoteCommitEventManager remote = _conf.
                    getRemoteCommitEventManager();
                if (remote.areRemoteEventsEnabled())
                    broker.addTransactionListener(remote);

                loadPersistentTypes(broker.getClassLoader());
            }
            _brokers.add(broker);

            return broker;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        }
    }

    /**
     * Add factory-registered lifecycle listeners to the broker.
     */
    protected void addLifecycleListeners(BrokerImpl broker) {
        if (_lifecycleListeners == null || _lifecycleListeners.isEmpty())
            return;

        Map.Entry entry;
        for (Iterator itr = _lifecycleListeners.entrySet().iterator();
            itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            broker.addLifecycleListener(entry.getKey(), (Class[])
                entry.getValue());
        }
    }

    /**
     * Load the configured persistent classes list. Performed automatically
     * whenever a broker is created.
     */
    private void loadPersistentTypes(ClassLoader envLoader) {
        // no listed persistent types?
        if (_pcClassNames != null && _pcClassNames.isEmpty())
            return;

        // cache persistent type names if not already
        ClassLoader loader = _conf.getClassResolverInstance().
            getClassLoader(getClass(), envLoader);
        if (_pcClassNames == null) {
            Collection clss = _conf.getMetaDataRepositoryInstance().
                loadPersistentTypes(false, loader);
            if (clss.isEmpty())
                _pcClassNames = Collections.EMPTY_SET;
            else {
                _pcClassNames = new ArrayList(clss.size());
                for (Iterator itr = clss.iterator(); itr.hasNext();)
                    _pcClassNames.add(((Class) itr.next()).getName());
                _pcClassLoaders = new ReferenceHashSet(ReferenceHashSet.WEAK);
                _pcClassLoaders.add(loader);
            }
            return;
        }

        // reload with this loader
        if (_pcClassLoaders.add(loader)) {
            for (Iterator itr = _pcClassNames.iterator(); itr.hasNext();) {
                try {
                    Class.forName((String) itr.next(), true, loader);
                } catch (Throwable t) {
                    _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME)
                        .warn(null, t);
                }
            }
        }
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

    /**
     * Returns true if this broker factory is closed.
     */
    public boolean isClosed() {
        return _closed != null;
    }

    public void close() {
        lock();
        try {
            assertOpen();
            assertNoActiveTransaction();

            // remove from factory pool
            Map map = toPoolKey(_conf);
            synchronized (_pool) {
                if (_pool.get(map) == this)
                    _pool.remove(map);
            }

            // close all brokers
            Broker broker;
            for (Iterator itr = _brokers.iterator(); itr.hasNext();) {
                broker = (Broker) itr.next();
                // Check for null because _brokers contains weak references
                if ((broker != null) && (!broker.isClosed()))
                    broker.close();
            }

            // remove metadata repository from listener list
            PCRegistry.removeRegisterClassListener
                (_conf.getMetaDataRepositoryInstance());

            _conf.close();
            _closed = new IllegalStateException();
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
        AbstractBrokerFactory factory = getPooledFactory(_conf);
        if (factory != null)
            return factory;

        // reset these transient fields to empty values
        _transactional = new HashMap();
        _brokers = new ConcurrentReferenceHashSet(
                ConcurrentReferenceHashSet.WEAK);

        makeReadOnly();
        return this;
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
        Transaction trans = null;
        try {
            trans = _conf.getManagedRuntimeInstance().getTransactionManager().
                getTransaction();

            if (trans == null
                || trans.getStatus() == Status.STATUS_NO_TRANSACTION
                || trans.getStatus() == Status.STATUS_UNKNOWN)
                return null;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        }

        synchronized (_transactional) {
            Collection brokers = (Collection) _transactional.get(trans);
            if (brokers != null) {
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
     * Add the factory to the pool.
     */
    protected void pool() {
        synchronized (_pool) {
            _pool.put(toPoolKey(_conf), this);
            makeReadOnly();
        }
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
                String lineSep = System.getProperty("line.separator");
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
            _conf.setReadOnly(true);
            _conf.instantiateAll();
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
     * Throw an exception if the factory is closed.
     */
    private void assertOpen() {
        if (_closed != null)
            throw new InvalidStateException(_loc.get("closed-factory")).
                setCause(_closed);
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
        Collection excs = null;
        synchronized (_transactional) {
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
        Transaction trans = null;
        try {
            TransactionManager tm = broker.getManagedRuntime().
                getTransactionManager();
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

            synchronized (_transactional) {
                Collection brokers = (Collection) _transactional.get(trans);
                if (brokers == null) {
                    brokers = new ArrayList(2);
                    _transactional.put(trans, brokers);

                    // register a callback to remove the trans from the
                    // cache when it ends
                    trans.registerSynchronization
                        (new RemoveTransactionSync(trans));
                }
                brokers.add(broker);
            }

            return true;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Simple synchronization listener to remove completed transactions
     * from our cache.
     */
    private class RemoveTransactionSync
        implements Synchronization {

        private final Transaction _trans;

        public RemoveTransactionSync(Transaction trans) {
            _trans = trans;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            synchronized (_transactional)
			{
				_transactional.remove (_trans);
			}
		}
	}
}

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
package org.apache.openjpa.conf;

import java.util.Collection;
import java.util.HashSet;

import org.apache.openjpa.datacache.ConcurrentDataCache;
import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCacheManagerImpl;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.kernel.InverseManager;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.RestoreState;
import org.apache.openjpa.kernel.SavepointManager;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.conf.BooleanValue;
import org.apache.openjpa.lib.conf.ConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.IntValue;
import org.apache.openjpa.lib.conf.ObjectValue;
import org.apache.openjpa.lib.conf.PluginListValue;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.conf.StringListValue;
import org.apache.openjpa.lib.conf.StringValue;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.ProxyManager;
import org.apache.openjpa.conf.StoreFacadeTypeRegistry;

/**
 * Implementation of the {@link OpenJPAConfiguration} interface.
 *  On construction, the class will attempt to locate a default properties
 * file called <code>openjpa.properties</code> located at any top level token
 * of the CLASSPATH. See the {@link ConfigurationImpl} class description
 * for details.
 *
 * @see ConfigurationImpl
 * @author Marc Prud'hommeaux
 * @author Abe White
 */
public class OpenJPAConfigurationImpl
    extends ConfigurationImpl
    implements OpenJPAConfiguration {

    private static final Localizer _loc = Localizer.forPackage
        (OpenJPAConfigurationImpl.class);

    // cached state; some of this is created in getter methods, so make
    // protected in case subclasses want to access without creating
    protected MetaDataRepository metaRepository = null;
    protected RemoteCommitEventManager remoteEventManager = null;

    // openjpa properties
    public ObjectValue classResolverPlugin;
    public ObjectValue brokerPlugin;
    public ObjectValue dataCachePlugin;
    public ObjectValue dataCacheManagerPlugin;
    public IntValue dataCacheTimeout;
    public ObjectValue queryCachePlugin;
    public BooleanValue dynamicDataStructs;
    public ObjectValue managedRuntimePlugin;
    public BooleanValue transactionMode;
    public IntValue connectionRetainMode;
    public IntValue fetchBatchSize;
    public StringListValue fetchGroups;
    public IntValue flushBeforeQueries;
    public IntValue lockTimeout;
    public IntValue readLockLevel;
    public IntValue writeLockLevel;
    public ObjectValue seqPlugin;
    public PluginListValue filterListenerPlugins;
    public PluginListValue aggregateListenerPlugins;
    public BooleanValue retryClassRegistration;
    public ObjectValue proxyManagerPlugin;
    public StringValue connectionUserName;
    public StringValue connectionPassword;
    public StringValue connectionURL;
    public StringValue connectionDriverName;
    public ObjectValue connectionFactory;
    public StringValue connectionFactoryName;
    public StringValue connectionProperties;
    public StringValue connectionFactoryProperties;
    public BooleanValue connectionFactoryMode;
    public StringValue connection2UserName;
    public StringValue connection2Password;
    public StringValue connection2URL;
    public StringValue connection2DriverName;
    public StringValue connection2Properties;
    public ObjectValue connectionFactory2;
    public StringValue connectionFactory2Name;
    public StringValue connectionFactory2Properties;
    public BooleanValue optimistic;
    public IntValue autoClear;
    public BooleanValue retainState;
    public IntValue restoreState;
    public ObjectValue detachStatePlugin;
    public BooleanValue ignoreChanges;
    public BooleanValue nontransactionalRead;
    public BooleanValue nontransactionalWrite;
    public BooleanValue multithreaded;
    public StringValue mapping;
    public PluginValue metaFactoryPlugin;
    public ObjectValue metaRepositoryPlugin;
    public ObjectValue lockManagerPlugin;
    public ObjectValue inverseManagerPlugin;
    public ObjectValue savepointManagerPlugin;
    public ObjectValue orphanedKeyPlugin;
    public ObjectValue compatibilityPlugin;

    // custom values
    public BrokerFactoryValue brokerFactoryPlugin;
    public RemoteCommitProviderValue remoteProviderPlugin;
    public AutoDetachValue autoDetach;

    private Collection supportedOptions = new HashSet(33);
    private String spec = null;
    
    private final StoreFacadeTypeRegistry _storeFacadeRegistry =
        new StoreFacadeTypeRegistry();

    /**
     * Default constructor. Attempts to load default properties.
     */
    public OpenJPAConfigurationImpl() {
        this(true);
    }

    /**
     * Constructor.
     *
     * @param loadDefaults whether to attempt to load the default
     * <code>openjpa.properties</code> resource
     */
    public OpenJPAConfigurationImpl(boolean loadDefaults) {
        this(true, loadDefaults);
    }

    /**
     * Constructor.
     *
     * @param derivations whether to apply product derivations
     * @param loadDefaults whether to attempt to load the default
     * <code>openjpa.properties</code> resource
     */
    public OpenJPAConfigurationImpl(boolean derivations, boolean loadDefaults) {
        super(false);
        String[] aliases;

        // setup super's log factory plugin
        logFactoryPlugin.setProperty("Log");
        logFactoryPlugin.setAlias("openjpa", 
            "org.apache.openjpa.lib.log.LogFactoryImpl");
        aliases = logFactoryPlugin.getAliases();
        logFactoryPlugin.setDefault(aliases[0]);
        logFactoryPlugin.setString(aliases[0]);

        classResolverPlugin = addPlugin("ClassResolver", true);
        aliases = new String[]{
            "default", "org.apache.openjpa.util.ClassResolverImpl",
            // deprecated alias
            "spec", "org.apache.openjpa.util.ClassResolverImpl",
        };
        classResolverPlugin.setAliases(aliases);
        classResolverPlugin.setDefault(aliases[0]);
        classResolverPlugin.setString(aliases[0]);
        classResolverPlugin.setInstantiatingGetter("getClassResolverInstance");

        brokerFactoryPlugin = new BrokerFactoryValue();
        addValue(brokerFactoryPlugin);

        brokerPlugin = addPlugin("BrokerImpl", false);
        aliases = new String[]{ "default", BrokerImpl.class.getName() };
        brokerPlugin.setAliases(aliases);
        brokerPlugin.setDefault(aliases[0]);
        brokerPlugin.setString(aliases[0]);

        dataCacheManagerPlugin = addPlugin("DataCacheManager", true);
        aliases = new String[]{
            "default", DataCacheManagerImpl.class.getName(),
        };
        dataCacheManagerPlugin.setAliases(aliases);
        dataCacheManagerPlugin.setDefault(aliases[0]);
        dataCacheManagerPlugin.setString(aliases[0]);
        dataCacheManagerPlugin.setInstantiatingGetter("getDataCacheManager");

        dataCachePlugin = addPlugin("DataCache", false);
        aliases = new String[]{
            "false", null,
            "true", ConcurrentDataCache.class.getName(),
            "concurrent", ConcurrentDataCache.class.getName(),
        };
        dataCachePlugin.setAliases(aliases);
        dataCachePlugin.setDefault(aliases[0]);
        dataCachePlugin.setString(aliases[0]);

        dataCacheTimeout = addInt("DataCacheTimeout");
        dataCacheTimeout.setDefault("-1");
        dataCacheTimeout.set(-1);

        queryCachePlugin = addPlugin("QueryCache", true);
        aliases = new String[]{
            "true", ConcurrentQueryCache.class.getName(),
            "concurrent", ConcurrentQueryCache.class.getName(),
            "false", null,
        };
        queryCachePlugin.setAliases(aliases);
        queryCachePlugin.setDefault(aliases[0]);
        queryCachePlugin.setString(aliases[0]);

        dynamicDataStructs = addBoolean("DynamicDataStructs");
        dynamicDataStructs.setDefault("false");
        dynamicDataStructs.set(false);

        lockManagerPlugin = addPlugin("LockManager", false);
        aliases = new String[]{
            "none", "org.apache.openjpa.kernel.NoneLockManager",
            "version", "org.apache.openjpa.kernel.VersionLockManager",
        };
        lockManagerPlugin.setAliases(aliases);
        lockManagerPlugin.setDefault(aliases[0]);
        lockManagerPlugin.setString(aliases[0]);

        inverseManagerPlugin = addPlugin("InverseManager", false);
        aliases = new String[]{
            "false", null,
            "true", "org.apache.openjpa.kernel.InverseManager",
        };
        inverseManagerPlugin.setAliases(aliases);
        inverseManagerPlugin.setDefault(aliases[0]);
        inverseManagerPlugin.setString(aliases[0]);

        savepointManagerPlugin = addPlugin("SavepointManager", true);
        aliases = new String[]{
            "in-mem", "org.apache.openjpa.kernel.InMemorySavepointManager",
        };
        savepointManagerPlugin.setAliases(aliases);
        savepointManagerPlugin.setDefault(aliases[0]);
        savepointManagerPlugin.setString(aliases[0]);
        savepointManagerPlugin.setInstantiatingGetter
            ("getSavepointManagerInstance");

        orphanedKeyPlugin = addPlugin("OrphanedKeyAction", true);
        aliases = new String[]{
            "log", "org.apache.openjpa.event.LogOrphanedKeyAction",
            "exception", "org.apache.openjpa.event.ExceptionOrphanedKeyAction",
            "none", "org.apache.openjpa.event.NoneOrphanedKeyAction",
        };
        orphanedKeyPlugin.setAliases(aliases);
        orphanedKeyPlugin.setDefault(aliases[0]);
        orphanedKeyPlugin.setString(aliases[0]);
        orphanedKeyPlugin.setInstantiatingGetter
            ("getOrphanedKeyActionInstance");

        remoteProviderPlugin = new RemoteCommitProviderValue();
        addValue(remoteProviderPlugin);

        transactionMode = addBoolean("TransactionMode");
        aliases = new String[]{
            "local", "false",
            "managed", "true",
        };
        transactionMode.setAliases(aliases);
        transactionMode.setDefault(aliases[0]);

        managedRuntimePlugin = addPlugin("ManagedRuntime", true);
        aliases = new String[]{
            "auto", "org.apache.openjpa.ee.AutomaticManagedRuntime",
            "jndi", "org.apache.openjpa.ee.JNDIManagedRuntime",
            "invocation", "org.apache.openjpa.ee.InvocationManagedRuntime",
        };
        managedRuntimePlugin.setAliases(aliases);
        managedRuntimePlugin.setDefault(aliases[0]);
        managedRuntimePlugin.setString(aliases[0]);
        managedRuntimePlugin.setInstantiatingGetter
            ("getManagedRuntimeInstance");

        proxyManagerPlugin = addPlugin("ProxyManager", true);
        aliases = new String[]{ "default",
            "org.apache.openjpa.util.ProxyManagerImpl" };
        proxyManagerPlugin.setAliases(aliases);
        proxyManagerPlugin.setDefault(aliases[0]);
        proxyManagerPlugin.setString(aliases[0]);
        proxyManagerPlugin.setInstantiatingGetter("getProxyManagerInstance");

        mapping = addString("Mapping");
        metaFactoryPlugin = addPlugin("MetaDataFactory", false);

        metaRepositoryPlugin = addPlugin("MetaDataRepository", false);
        aliases = new String[]{ "default", 
            "org.apache.openjpa.meta.MetaDataRepository" };
        metaRepositoryPlugin.setAliases(aliases);
        metaRepositoryPlugin.setDefault(aliases[0]);
        metaRepositoryPlugin.setString(aliases[0]);

        connectionFactory = addObject("ConnectionFactory");
        connectionFactory.setInstantiatingGetter("getConnectionFactory");

        connectionFactory2 = addObject("ConnectionFactory2");
        connectionFactory2.setInstantiatingGetter("getConnectionFactory2");

        connectionUserName = addString("ConnectionUserName");
        connectionPassword = addString("ConnectionPassword");
        connectionURL = addString("ConnectionURL");
        connectionDriverName = addString("ConnectionDriverName");
        connectionFactoryName = addString("ConnectionFactoryName");
        connectionProperties = addString("ConnectionProperties");
        connectionFactoryProperties = addString("ConnectionFactoryProperties");
        connection2UserName = addString("Connection2UserName");
        connection2Password = addString("Connection2Password");
        connection2URL = addString("Connection2URL");
        connection2DriverName = addString("Connection2DriverName");
        connection2Properties = addString("Connection2Properties");
        connectionFactory2Properties = addString(
            "ConnectionFactory2Properties");
        connectionFactory2Name = addString("ConnectionFactory2Name");

        connectionFactoryMode = addBoolean("ConnectionFactoryMode");
        aliases = new String[]{
            "local", "false",
            "managed", "true",
        };
        connectionFactoryMode.setAliases(aliases);
        connectionFactoryMode.setDefault(aliases[0]);

        optimistic = addBoolean("Optimistic");
        optimistic.setDefault("true");
        optimistic.set(true);

        autoClear = addInt("AutoClear");
        aliases = new String[]{
            "datastore", String.valueOf(AutoClear.CLEAR_DATASTORE),
            "all", String.valueOf(AutoClear.CLEAR_ALL),
        };
        autoClear.setAliases(aliases);
        autoClear.setDefault(aliases[0]);
        autoClear.set(AutoClear.CLEAR_DATASTORE);

        retainState = addBoolean("RetainState");
        retainState.setDefault("true");
        retainState.set(true);

        restoreState = addInt("RestoreState");
        aliases = new String[]{
            "none", String.valueOf(RestoreState.RESTORE_NONE),
            "false", String.valueOf(RestoreState.RESTORE_NONE),
            "immutable", String.valueOf(RestoreState.RESTORE_IMMUTABLE),
            // "true" for compat with jdo RestoreValues
            "true", String.valueOf(RestoreState.RESTORE_IMMUTABLE),
            "all", String.valueOf(RestoreState.RESTORE_ALL),
        };
        restoreState.setAliases(aliases);
        restoreState.setDefault(aliases[0]);
        restoreState.set(RestoreState.RESTORE_IMMUTABLE);

        autoDetach = new AutoDetachValue();
        addValue(autoDetach);

        detachStatePlugin = addPlugin("DetachState", true);
        aliases = new String[]{
            "loaded", DetachOptions.Loaded.class.getName(),
            "fgs", DetachOptions.FetchGroups.class.getName(),
            "all", DetachOptions.All.class.getName(),
        };
        detachStatePlugin.setAliases(aliases);
        detachStatePlugin.setDefault(aliases[0]);
        detachStatePlugin.setString(aliases[0]);
        detachStatePlugin.setInstantiatingGetter("getDetachStateInstance");

        ignoreChanges = addBoolean("IgnoreChanges");

        nontransactionalRead = addBoolean("NontransactionalRead");
        nontransactionalRead.setDefault("true");
        nontransactionalRead.set(true);

        nontransactionalWrite = addBoolean("NontransactionalWrite");
        multithreaded = addBoolean("Multithreaded");

        fetchBatchSize = addInt("FetchBatchSize");
        fetchBatchSize.setDefault("-1");
        fetchBatchSize.set(-1);

        fetchGroups = addStringList("FetchGroups");
        fetchGroups.setDefault("default");
        fetchGroups.set(new String[]{ "default" });

        flushBeforeQueries = addInt("FlushBeforeQueries");
        aliases = new String[]{
            "true", String.valueOf(QueryFlushModes.FLUSH_TRUE),
            "false", String.valueOf(QueryFlushModes.FLUSH_FALSE),
            "with-connection", String.valueOf
            (QueryFlushModes.FLUSH_WITH_CONNECTION),
        };
        flushBeforeQueries.setAliases(aliases);
        flushBeforeQueries.setDefault(aliases[0]);
        flushBeforeQueries.set(QueryFlushModes.FLUSH_TRUE);

        lockTimeout = addInt("LockTimeout");
        lockTimeout.setDefault("-1");
        lockTimeout.set(-1);

        readLockLevel = addInt("ReadLockLevel");
        aliases = new String[]{
            "read", String.valueOf(LockLevels.LOCK_READ),
            "write", String.valueOf(LockLevels.LOCK_WRITE),
            "none", String.valueOf(LockLevels.LOCK_NONE),
        };
        readLockLevel.setAliases(aliases);
        readLockLevel.setDefault(aliases[0]);
        readLockLevel.set(LockLevels.LOCK_READ);

        writeLockLevel = addInt("WriteLockLevel");
        aliases = new String[]{
            "read", String.valueOf(LockLevels.LOCK_READ),
            "write", String.valueOf(LockLevels.LOCK_WRITE),
            "none", String.valueOf(LockLevels.LOCK_NONE),
        };
        writeLockLevel.setAliases(aliases);
        writeLockLevel.setDefault(aliases[1]);
        writeLockLevel.set(LockLevels.LOCK_WRITE);

        seqPlugin = new SeqValue("Sequence");
        seqPlugin.setInstantiatingGetter("getSequenceInstance");
        addValue(seqPlugin);

        connectionRetainMode = addInt("ConnectionRetainMode");
        aliases = new String[]{
            "on-demand",
            String.valueOf(ConnectionRetainModes.CONN_RETAIN_DEMAND),
            "transaction",
            String.valueOf(ConnectionRetainModes.CONN_RETAIN_TRANS),
            "always",
            String.valueOf(ConnectionRetainModes.CONN_RETAIN_ALWAYS),
            // deprecated
            "persistence-manager",
            String.valueOf(ConnectionRetainModes.CONN_RETAIN_ALWAYS),
        };
        connectionRetainMode.setAliases(aliases);
        connectionRetainMode.setDefault(aliases[0]);
        connectionRetainMode.setAliasListComprehensive(true);
        connectionRetainMode.set(ConnectionRetainModes.CONN_RETAIN_DEMAND);

        filterListenerPlugins = addPluginList("FilterListeners");
        filterListenerPlugins.setInstantiatingGetter
            ("getFilterListenerInstances");

        aggregateListenerPlugins = addPluginList("AggregateListeners");
        aggregateListenerPlugins.setInstantiatingGetter
            ("getAggregateListenerInstances");

        retryClassRegistration = addBoolean("RetryClassRegistration");

        compatibilityPlugin = addPlugin("Compatibility", true);
        aliases = new String[]{ "default", Compatibility.class.getName() };
        compatibilityPlugin.setAliases(aliases);
        compatibilityPlugin.setDefault(aliases[0]);
        compatibilityPlugin.setString(aliases[0]);
        compatibilityPlugin.setInstantiatingGetter("getCompatibilityInstance");

        // initialize supported options that some runtimes may not support
        supportedOptions.add(OPTION_NONTRANS_READ);
        supportedOptions.add(OPTION_OPTIMISTIC);
        supportedOptions.add(OPTION_ID_APPLICATION);
        supportedOptions.add(OPTION_ID_DATASTORE);
        supportedOptions.add(OPTION_TYPE_COLLECTION);
        supportedOptions.add(OPTION_TYPE_MAP);
        supportedOptions.add(OPTION_TYPE_ARRAY);
        supportedOptions.add(OPTION_NULL_CONTAINER);
        supportedOptions.add(OPTION_EMBEDDED_RELATION);
        supportedOptions.add(OPTION_EMBEDDED_COLLECTION_RELATION);
        supportedOptions.add(OPTION_EMBEDDED_MAP_RELATION);
        supportedOptions.add(OPTION_INC_FLUSH);
        supportedOptions.add(OPTION_VALUE_AUTOASSIGN);
        supportedOptions.add(OPTION_VALUE_INCREMENT);
        supportedOptions.add(OPTION_DATASTORE_CONNECTION);

        if (derivations)
            ProductDerivations.beforeConfigurationLoad(this);
        if (loadDefaults)
            loadDefaults();
    }

    public Collection supportedOptions() {
        return supportedOptions;
    }

    public String getSpecification() {
        return spec;
    }

    public boolean setSpecification(String spec) {
        if (spec == null)
            return false;

        if (this.spec != null) {
            if (!this.spec.equals(spec)
                && getConfigurationLog().isWarnEnabled())
                getConfigurationLog().warn(_loc.get("diff-specs", this.spec,
                    spec));
            return false;
        }

        this.spec = spec;
        ProductDerivations.afterSpecificationSet(this);
        return true;
    }

    public void setClassResolver(String classResolver) {
        assertNotReadOnly();
        classResolverPlugin.setString(classResolver);
    }

    public String getClassResolver() {
        return classResolverPlugin.getString();
    }

    public void setClassResolver(ClassResolver classResolver) {
        assertNotReadOnly();
        classResolverPlugin.set(classResolver);
    }

    public ClassResolver getClassResolverInstance() {
        if (classResolverPlugin.get() == null)
            classResolverPlugin.instantiate(ClassResolver.class, this);
        return (ClassResolver) classResolverPlugin.get();
    }

    public void setBrokerFactory(String factory) {
        assertNotReadOnly();
        brokerFactoryPlugin.setString(factory);
    }

    public String getBrokerFactory() {
        return brokerFactoryPlugin.getString();
    }

    public void setBrokerImpl(String broker) {
        assertNotReadOnly();
        brokerPlugin.setString(broker);
    }

    public String getBrokerImpl() {
        return brokerPlugin.getString();
    }

    public BrokerImpl newBrokerInstance(String user, String pass) {
        BrokerImpl broker = (BrokerImpl) brokerPlugin.instantiate
            (BrokerImpl.class, this);
        if (broker != null)
            broker.setAuthentication(user, pass);
        return broker;
    }

    public void setDataCacheManager(String mgr) {
        assertNotReadOnly();
        dataCacheManagerPlugin.setString(mgr);
    }

    public String getDataCacheManager() {
        return dataCacheManagerPlugin.getString();
    }

    public void setDataCacheManager(DataCacheManager dcm) {
        assertNotReadOnly();
        if (dcm != null)
            dcm.initialize(this, dataCachePlugin, queryCachePlugin);
        dataCacheManagerPlugin.set(dcm);
    }

    public DataCacheManager getDataCacheManagerInstance() {
        DataCacheManager dcm = (DataCacheManager) dataCacheManagerPlugin.get();
        if (dcm == null) {
            dcm = (DataCacheManager) dataCacheManagerPlugin.instantiate
                (DataCacheManager.class, this);
            dcm.initialize(this, dataCachePlugin, queryCachePlugin);
        }
        return dcm;
    }

    public void setDataCache(String dataCache) {
        assertNotReadOnly();
        dataCachePlugin.setString(dataCache);
    }

    public String getDataCache() {
        return dataCachePlugin.getString();
    }

    public void setDataCacheTimeout(int dataCacheTimeout) {
        assertNotReadOnly();
        this.dataCacheTimeout.set(dataCacheTimeout);
    }

    public void setDataCacheTimeout(Integer dataCacheTimeout) {
        if (dataCacheTimeout != null)
            setDataCacheTimeout(dataCacheTimeout.intValue());
    }

    public int getDataCacheTimeout() {
        return dataCacheTimeout.get();
    }

    public void setQueryCache(String queryCache) {
        assertNotReadOnly();
        queryCachePlugin.setString(queryCache);
    }

    public String getQueryCache() {
        return queryCachePlugin.getString();
    }

    public boolean getDynamicDataStructs() {
        return dynamicDataStructs.get();
    }

    public void setDynamicDataStructs(boolean dynamic) {
        dynamicDataStructs.set(dynamic);
    }

    public void setDynamicDataStructs(Boolean dynamic) {
        setDynamicDataStructs(dynamic.booleanValue());
    }

    public void setLockManager(String lockManager) {
        assertNotReadOnly();
        lockManagerPlugin.setString(lockManager);
    }

    public String getLockManager() {
        return lockManagerPlugin.getString();
    }

    public LockManager newLockManagerInstance() {
        // don't validate plugin properties on instantiation because it
        // is likely that back ends will override defaults with their
        // own subclasses with new properties
        return (LockManager) lockManagerPlugin.instantiate(LockManager.class,
            this, false);
    }

    public void setInverseManager(String inverseManager) {
        assertNotReadOnly();
        inverseManagerPlugin.setString(inverseManager);
    }

    public String getInverseManager() {
        return inverseManagerPlugin.getString();
    }

    public InverseManager newInverseManagerInstance() {
        return (InverseManager) inverseManagerPlugin.instantiate
            (InverseManager.class, this);
    }

    public void setSavepointManager(String savepointManager) {
        assertNotReadOnly();
        savepointManagerPlugin.setString(savepointManager);
    }

    public String getSavepointManager() {
        return savepointManagerPlugin.getString();
    }

    public SavepointManager getSavepointManagerInstance() {
        if (savepointManagerPlugin.get() == null)
            savepointManagerPlugin.instantiate(SavepointManager.class, this);
        return (SavepointManager) savepointManagerPlugin.get();
    }

    public void setOrphanedKeyAction(String action) {
        assertNotReadOnly();
        orphanedKeyPlugin.setString(action);
    }

    public String getOrphanedKeyAction() {
        return orphanedKeyPlugin.getString();
    }

    public OrphanedKeyAction getOrphanedKeyActionInstance() {
        if (orphanedKeyPlugin.get() == null)
            orphanedKeyPlugin.instantiate(OrphanedKeyAction.class, this);
        return (OrphanedKeyAction) orphanedKeyPlugin.get();
    }

    public void setOrphanedKeyAction(OrphanedKeyAction action) {
        assertNotReadOnly();
        orphanedKeyPlugin.set(action);
    }

    public void setRemoteCommitProvider(String remoteCommitProvider) {
        assertNotReadOnly();
        remoteProviderPlugin.setString(remoteCommitProvider);
    }

    public String getRemoteCommitProvider() {
        return remoteProviderPlugin.getString();
    }

    public RemoteCommitProvider newRemoteCommitProviderInstance() {
        return remoteProviderPlugin.instantiateProvider(this);
    }

    public void setRemoteCommitEventManager
        (RemoteCommitEventManager remoteEventManager) {
        assertNotReadOnly();
        this.remoteEventManager = remoteEventManager;
        remoteProviderPlugin.configureEventManager(remoteEventManager);
    }

    public RemoteCommitEventManager getRemoteCommitEventManager() {
        if (remoteEventManager == null) {
            remoteEventManager = new RemoteCommitEventManager(this);
            remoteProviderPlugin.configureEventManager(remoteEventManager);
        }
        return remoteEventManager;
    }

    public void setTransactionMode(String transactionMode) {
        assertNotReadOnly();
        this.transactionMode.setString(transactionMode);
    }

    public String getTransactionMode() {
        return transactionMode.getString();
    }

    public void setTransactionModeManaged(boolean managed) {
        assertNotReadOnly();
        transactionMode.set(managed);
    }

    public boolean isTransactionModeManaged() {
        return transactionMode.get();
    }

    public void setManagedRuntime(String managedRuntime) {
        assertNotReadOnly();
        managedRuntimePlugin.setString(managedRuntime);
    }

    public String getManagedRuntime() {
        return managedRuntimePlugin.getString();
    }

    public void setManagedRuntime(ManagedRuntime managedRuntime) {
        assertNotReadOnly();
        managedRuntimePlugin.set(managedRuntime);
    }

    public ManagedRuntime getManagedRuntimeInstance() {
        if (managedRuntimePlugin.get() == null)
            managedRuntimePlugin.instantiate(ManagedRuntime.class, this);
        return (ManagedRuntime) managedRuntimePlugin.get();
    }

    public void setProxyManager(String proxyManager) {
        assertNotReadOnly();
        proxyManagerPlugin.setString(proxyManager);
    }

    public String getProxyManager() {
        return proxyManagerPlugin.getString();
    }

    public void setProxyManager(ProxyManager proxyManager) {
        assertNotReadOnly();
        proxyManagerPlugin.set(proxyManager);
    }

    public ProxyManager getProxyManagerInstance() {
        if (proxyManagerPlugin.get() == null)
            proxyManagerPlugin.instantiate(ProxyManager.class, this);
        return (ProxyManager) proxyManagerPlugin.get();
    }

    public void setMapping(String mapping) {
        assertNotReadOnly();
        this.mapping.setString(mapping);
    }

    public String getMapping() {
        return mapping.getString();
    }

    public void setMetaDataFactory(String meta) {
        assertNotReadOnly();
        this.metaFactoryPlugin.setString(meta);
    }

    public String getMetaDataFactory() {
        return metaFactoryPlugin.getString();
    }

    public MetaDataFactory newMetaDataFactoryInstance() {
        return (MetaDataFactory) metaFactoryPlugin.instantiate
            (MetaDataFactory.class, this);
    }

    public void setMetaDataRepository(String meta) {
        assertNotReadOnly();
        this.metaRepositoryPlugin.setString(meta);
    }

    public String getMetaDataRepository() {
        return metaRepositoryPlugin.getString();
    }

    public void setMetaDataRepository(MetaDataRepository meta) {
        assertNotReadOnly();
        metaRepository = meta;
    }

    public MetaDataRepository getMetaDataRepositoryInstance() {
        if (metaRepository == null)
            metaRepository = (MetaDataRepository) metaRepositoryPlugin.
                instantiate(MetaDataRepository.class, this);
        return metaRepository;
    }

    public MetaDataRepository newMetaDataRepositoryInstance() {
        return (MetaDataRepository) metaRepositoryPlugin.instantiate
            (MetaDataRepository.class, this);
    }

    public void setConnectionUserName(String connectionUserName) {
        assertNotReadOnly();
        this.connectionUserName.setString(connectionUserName);
    }

    public String getConnectionUserName() {
        return connectionUserName.getString();
    }

    public void setConnectionPassword(String connectionPassword) {
        assertNotReadOnly();
        this.connectionPassword.setString(connectionPassword);
    }

    public String getConnectionPassword() {
        return connectionPassword.getString();
    }

    public void setConnectionURL(String connectionURL) {
        assertNotReadOnly();
        this.connectionURL.setString(connectionURL);
    }

    public String getConnectionURL() {
        return connectionURL.getString();
    }

    public void setConnectionDriverName(String driverName) {
        assertNotReadOnly();
        this.connectionDriverName.setString(driverName);
    }

    public String getConnectionDriverName() {
        return connectionDriverName.getString();
    }

    public void setConnectionProperties(String connectionProperties) {
        assertNotReadOnly();
        this.connectionProperties.setString(connectionProperties);
    }

    public String getConnectionProperties() {
        return connectionProperties.getString();
    }

    public void setConnectionFactoryProperties
        (String connectionFactoryProperties) {
        assertNotReadOnly();
        this.connectionFactoryProperties.setString(connectionFactoryProperties);
    }

    public String getConnectionFactoryProperties() {
        return connectionFactoryProperties.getString();
    }

    public String getConnectionFactoryMode() {
        return connectionFactoryMode.getString();
    }

    public void setConnectionFactoryMode(String mode) {
        assertNotReadOnly();
        connectionFactoryMode.setString(mode);
    }

    public boolean isConnectionFactoryModeManaged() {
        return connectionFactoryMode.get();
    }

    public void setConnectionFactoryModeManaged(boolean managed) {
        assertNotReadOnly();
        connectionFactoryMode.set(managed);
    }

    public void setConnectionFactoryName(String connectionFactoryName) {
        assertNotReadOnly();
        this.connectionFactoryName.setString(connectionFactoryName);
    }

    public String getConnectionFactoryName() {
        return connectionFactoryName.getString();
    }

    public void setConnectionFactory(Object factory) {
        assertNotReadOnly();
        connectionFactory.set(factory);
    }

    public Object getConnectionFactory() {
        if (connectionFactory.get() == null)
            connectionFactory.set(lookupConnectionFactory
                (getConnectionFactoryName()), true);
        return connectionFactory.get();
    }

    /**
     * Lookup the connection factory at the given name.
     */
    private Object lookupConnectionFactory(String name) {
        if (name == null || name.trim().length() == 0)
            return null;

        return Configurations.lookup(name);
    }

    public void setConnection2UserName(String connection2UserName) {
        assertNotReadOnly();
        this.connection2UserName.setString(connection2UserName);
    }

    public String getConnection2UserName() {
        return connection2UserName.getString();
    }

    public void setConnection2Password(String connection2Password) {
        assertNotReadOnly();
        this.connection2Password.setString(connection2Password);
    }

    public String getConnection2Password() {
        return connection2Password.getString();
    }

    public void setConnection2URL(String connection2URL) {
        assertNotReadOnly();
        this.connection2URL.setString(connection2URL);
    }

    public String getConnection2URL() {
        return connection2URL.getString();
    }

    public void setConnection2DriverName(String driverName) {
        assertNotReadOnly();
        this.connection2DriverName.setString(driverName);
    }

    public String getConnection2DriverName() {
        return connection2DriverName.getString();
    }

    public void setConnection2Properties(String connection2Properties) {
        assertNotReadOnly();
        this.connection2Properties.setString(connection2Properties);
    }

    public String getConnection2Properties() {
        return connection2Properties.getString();
    }

    public void setConnectionFactory2Properties
        (String connectionFactory2Properties) {
        assertNotReadOnly();
        this.connectionFactory2Properties.setString
            (connectionFactory2Properties);
    }

    public String getConnectionFactory2Properties() {
        return connectionFactory2Properties.getString();
    }

    public void setConnectionFactory2Name(String connectionFactory2Name) {
        assertNotReadOnly();
        this.connectionFactory2Name.setString(connectionFactory2Name);
    }

    public String getConnectionFactory2Name() {
        return connectionFactory2Name.getString();
    }

    public void setConnectionFactory2(Object factory) {
        assertNotReadOnly();
        connectionFactory2.set(factory);
    }

    public Object getConnectionFactory2() {
        if (connectionFactory2.get() == null)
            connectionFactory2.set(lookupConnectionFactory
                (getConnectionFactory2Name()), false);
        return connectionFactory2.get();
    }

    public void setOptimistic(boolean optimistic) {
        assertNotReadOnly();
        this.optimistic.set(optimistic);
    }

    public void setOptimistic(Boolean optimistic) {
        if (optimistic != null)
            setOptimistic(optimistic.booleanValue());
    }

    public boolean getOptimistic() {
        return optimistic.get();
    }

    public void setAutoClear(String clear) {
        assertNotReadOnly();
        autoClear.setString(clear);
    }

    public String getAutoClear() {
        return autoClear.getString();
    }

    public void setAutoClear(int clear) {
        assertNotReadOnly();
        autoClear.set(clear);
    }

    public int getAutoClearConstant() {
        return autoClear.get();
    }

    public void setRetainState(boolean retainState) {
        assertNotReadOnly();
        this.retainState.set(retainState);
    }

    public void setRetainState(Boolean retainState) {
        if (retainState != null)
            setRetainState(retainState.booleanValue());
    }

    public boolean getRetainState() {
        return retainState.get();
    }

    public void setRestoreState(String restoreState) {
        assertNotReadOnly();
        this.restoreState.setString(restoreState);
    }

    public String getRestoreState() {
        return restoreState.getString();
    }

    public void setRestoreState(int restoreState) {
        assertNotReadOnly();
        this.restoreState.set(restoreState);
    }

    public int getRestoreStateConstant() {
        return restoreState.get();
    }

    public void setAutoDetach(String autoDetach) {
        assertNotReadOnly();
        this.autoDetach.setString(autoDetach);
    }

    public String getAutoDetach() {
        return autoDetach.getString();
    }

    public void setAutoDetach(int autoDetachFlags) {
        autoDetach.set(autoDetachFlags);
    }

    public int getAutoDetachConstant() {
        return autoDetach.get();
    }

    public void setDetachState(String detachState) {
        assertNotReadOnly();
        detachStatePlugin.setString(detachState);
    }

    public String getDetachState() {
        return detachStatePlugin.getString();
    }

    public void setDetachState(DetachOptions detachState) {
        assertNotReadOnly();
        detachStatePlugin.set(detachState);
    }

    public DetachOptions getDetachStateInstance() {
        if (detachStatePlugin.get() == null)
            detachStatePlugin.instantiate(DetachOptions.class, this);
        return (DetachOptions) detachStatePlugin.get();
    }

    public void setIgnoreChanges(boolean ignoreChanges) {
        assertNotReadOnly();
        this.ignoreChanges.set(ignoreChanges);
    }

    public void setIgnoreChanges(Boolean ignoreChanges) {
        if (ignoreChanges != null)
            setIgnoreChanges(ignoreChanges.booleanValue());
    }

    public boolean getIgnoreChanges() {
        return ignoreChanges.get();
    }

    public void setNontransactionalRead(boolean nontransactionalRead) {
        assertNotReadOnly();
        this.nontransactionalRead.set(nontransactionalRead);
    }

    public void setNontransactionalRead(Boolean nontransactionalRead) {
        if (nontransactionalRead != null)
            setNontransactionalRead(nontransactionalRead.booleanValue());
    }

    public boolean getNontransactionalRead() {
        return nontransactionalRead.get();
    }

    public void setNontransactionalWrite(boolean nontransactionalWrite) {
        assertNotReadOnly();
        this.nontransactionalWrite.set(nontransactionalWrite);
    }

    public void setNontransactionalWrite(Boolean nontransactionalWrite) {
        if (nontransactionalWrite != null)
            setNontransactionalWrite(nontransactionalWrite.booleanValue());
    }

    public boolean getNontransactionalWrite() {
        return nontransactionalWrite.get();
    }

    public void setMultithreaded(boolean multithreaded) {
        assertNotReadOnly();
        this.multithreaded.set(multithreaded);
    }

    public void setMultithreaded(Boolean multithreaded) {
        if (multithreaded != null)
            setMultithreaded(multithreaded.booleanValue());
    }

    public boolean getMultithreaded() {
        return multithreaded.get();
    }

    public void setFetchBatchSize(int fetchBatchSize) {
        assertNotReadOnly();
        this.fetchBatchSize.set(fetchBatchSize);
    }

    public void setFetchBatchSize(Integer fetchBatchSize) {
        if (fetchBatchSize != null)
            setFetchBatchSize(fetchBatchSize.intValue());
    }

    public int getFetchBatchSize() {
        return fetchBatchSize.get();
    }

    public void setFetchGroups(String fetchGroups) {
        assertNotReadOnly();
        this.fetchGroups.setString(fetchGroups);
    }

    public String getFetchGroups() {
        return fetchGroups.getString();
    }

    public String[] getFetchGroupsList() {
        return fetchGroups.get();
    }

    public void setFetchGroups(String[] fetchGroups) {
        this.fetchGroups.set(fetchGroups);
    }

    public void setFlushBeforeQueries(String flush) {
        assertNotReadOnly();
        flushBeforeQueries.setString(flush);
    }

    public String getFlushBeforeQueries() {
        return flushBeforeQueries.getString();
    }

    public void setFlushBeforeQueries(int flush) {
        assertNotReadOnly();
        flushBeforeQueries.set(flush);
    }

    public int getFlushBeforeQueriesConstant() {
        return flushBeforeQueries.get();
    }

    public void setLockTimeout(int timeout) {
        assertNotReadOnly();
        lockTimeout.set(timeout);
    }

    public void setLockTimeout(Integer timeout) {
        if (timeout != null)
            setLockTimeout(timeout.intValue());
    }

    public int getLockTimeout() {
        return lockTimeout.get();
    }

    public void setReadLockLevel(String level) {
        assertNotReadOnly();
        readLockLevel.setString(level);
    }

    public String getReadLockLevel() {
        return readLockLevel.getString();
    }

    public void setReadLockLevel(int level) {
        assertNotReadOnly();
        readLockLevel.set(level);
    }

    public int getReadLockLevelConstant() {
        return readLockLevel.get();
    }

    public void setWriteLockLevel(String level) {
        assertNotReadOnly();
        writeLockLevel.setString(level);
    }

    public String getWriteLockLevel() {
        return writeLockLevel.getString();
    }

    public void setWriteLockLevel(int level) {
        assertNotReadOnly();
        writeLockLevel.set(level);
    }

    public int getWriteLockLevelConstant() {
        return writeLockLevel.get();
    }

    public void setSequence(String sequence) {
        assertNotReadOnly();
        seqPlugin.setString(sequence);
    }

    public String getSequence() {
        return seqPlugin.getString();
    }

    public void setSequence(Seq seq) {
        assertNotReadOnly();
        seqPlugin.set(seq);
    }

    public Seq getSequenceInstance() {
        if (seqPlugin.get() == null)
            seqPlugin.instantiate(Seq.class, this);
        return (Seq) seqPlugin.get();
    }

    public void setConnectionRetainMode(String connectionRetainMode) {
        assertNotReadOnly();
        this.connectionRetainMode.setString(connectionRetainMode);
    }

    public String getConnectionRetainMode() {
        return connectionRetainMode.getString();
    }

    public void setConnectionRetainMode(int connectionRetainMode) {
        assertNotReadOnly();
        this.connectionRetainMode.set(connectionRetainMode);
    }

    public int getConnectionRetainModeConstant() {
        return connectionRetainMode.get();
    }

    public void setFilterListeners(String filterListeners) {
        assertNotReadOnly();
        filterListenerPlugins.setString(filterListeners);
    }

    public String getFilterListeners() {
        return filterListenerPlugins.getString();
    }

    public void setFilterListeners(FilterListener[] listeners) {
        assertNotReadOnly();
        filterListenerPlugins.set(listeners);
    }

    public FilterListener[] getFilterListenerInstances() {
        if (filterListenerPlugins.get() == null)
            filterListenerPlugins.instantiate(FilterListener.class, this);
        return (FilterListener[]) filterListenerPlugins.get();
    }

    public void setAggregateListeners(String aggregateListeners) {
        assertNotReadOnly();
        aggregateListenerPlugins.setString(aggregateListeners);
    }

    public String getAggregateListeners() {
        return aggregateListenerPlugins.getString();
    }

    public void setAggregateListeners(AggregateListener[] listeners) {
        assertNotReadOnly();
        aggregateListenerPlugins.set(listeners);
    }

    public AggregateListener[] getAggregateListenerInstances() {
        if (aggregateListenerPlugins.get() == null)
            aggregateListenerPlugins.instantiate(AggregateListener.class, this);
        return (AggregateListener[]) aggregateListenerPlugins.get();
    }

    public void setRetryClassRegistration(boolean retry) {
        assertNotReadOnly();
        retryClassRegistration.set(retry);
    }

    public void setRetryClassRegistration(Boolean retry) {
        if (retry != null)
            setRetryClassRegistration(retry.booleanValue());
    }

    public boolean getRetryClassRegistration() {
        return retryClassRegistration.get();
    }

    public String getCompatibility() {
        return compatibilityPlugin.getString();
    }

    public void setCompatibility(String compatibility) {
        compatibilityPlugin.setString(compatibility);
    }

    public Compatibility getCompatibilityInstance() {
        if (compatibilityPlugin.get() == null)
            compatibilityPlugin.instantiate(Compatibility.class, this);
        return (Compatibility) compatibilityPlugin.get();
    }

    public StoreFacadeTypeRegistry getStoreFacadeTypeRegistry() {
        return _storeFacadeRegistry;
    }

    public void instantiateAll() {
        super.instantiateAll();
        getMetaDataRepositoryInstance();
        getRemoteCommitEventManager();
    }

    public void close() {
        ImplHelper.close(metaRepository);
        ImplHelper.close(remoteEventManager);
        super.close();
    }

    public Log getConfigurationLog() {
        return getLog(LOG_RUNTIME);
	}
}

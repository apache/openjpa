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
package org.apache.openjpa.conf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.datacache.ConcurrentDataCache;
import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCacheManagerImpl;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.event.BrokerFactoryEventManager;
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
import org.apache.openjpa.lib.conf.*;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.ProxyManager;
import org.apache.openjpa.util.StoreFacadeTypeRegistry;
import org.apache.openjpa.enhance.RuntimeUnenhancedClasssesModes;

/**
 * Implementation of the {@link OpenJPAConfiguration} interface.
 *
 * @see ConfigurationImpl
 * @author Marc Prud'hommeaux
 * @author Abe White
 */
public class OpenJPAConfigurationImpl
    extends ConfigurationImpl
    implements OpenJPAConfiguration {

    private static final Localizer _loc =
        Localizer.forPackage(OpenJPAConfigurationImpl.class);

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
    public IntValue maxFetchDepth;
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
    public BooleanValue refreshFromDataCache;
    public BooleanValue multithreaded;
    public StringValue mapping;
    public PluginValue metaFactoryPlugin;
    public ObjectValue metaRepositoryPlugin;
    public ObjectValue lockManagerPlugin;
    public ObjectValue inverseManagerPlugin;
    public ObjectValue savepointManagerPlugin;
    public ObjectValue orphanedKeyPlugin;
    public ObjectValue compatibilityPlugin;
    public QueryCompilationCacheValue queryCompilationCachePlugin;
    public IntValue runtimeUnenhancedClasses;
    public CacheMarshallersValue cacheMarshallerPlugins;

    // custom values
    public BrokerFactoryValue brokerFactoryPlugin;
    public RemoteCommitProviderValue remoteProviderPlugin;
    public AutoDetachValue autoDetach;

    private Collection supportedOptions = new HashSet(33);
    private String spec = null;
    private final StoreFacadeTypeRegistry _storeFacadeRegistry =
        new StoreFacadeTypeRegistry();
    private BrokerFactoryEventManager _brokerFactoryEventManager =
        new BrokerFactoryEventManager(this);

    /**
     * Default constructor. Attempts to load global properties.
     */
    public OpenJPAConfigurationImpl() {
        this(true);
    }

    /**
     * Constructor.
     *
     * @param loadGlobals whether to attempt to load the global properties
     */
    public OpenJPAConfigurationImpl(boolean loadGlobals) {
        this(true, loadGlobals);
    }

    /**
     * Constructor.
     *
     * @param derivations whether to apply product derivations
     * @param loadGlobals whether to attempt to load the global properties
     */
    public OpenJPAConfigurationImpl(boolean derivations, boolean loadGlobals) {
        super(false);
        String[] aliases;

        classResolverPlugin = addPlugin("ClassResolver", true);
        aliases =
            new String[] { "default",
                "org.apache.openjpa.util.ClassResolverImpl",
                // deprecated alias
                "spec", "org.apache.openjpa.util.ClassResolverImpl", };
        classResolverPlugin.setAliases(aliases);
        classResolverPlugin.setDefault(aliases[0]);
        classResolverPlugin.setString(aliases[0]);
        classResolverPlugin.setInstantiatingGetter("getClassResolverInstance");

        brokerFactoryPlugin = new BrokerFactoryValue();
        addValue(brokerFactoryPlugin);

        brokerPlugin = new BrokerValue();
        addValue(brokerPlugin);

        dataCacheManagerPlugin = addPlugin("DataCacheManager", true);
        aliases =
            new String[] { "default", DataCacheManagerImpl.class.getName(), };
        dataCacheManagerPlugin.setAliases(aliases);
        dataCacheManagerPlugin.setDefault(aliases[0]);
        dataCacheManagerPlugin.setString(aliases[0]);
        dataCacheManagerPlugin.setInstantiatingGetter("getDataCacheManager");

        dataCachePlugin = addPlugin("DataCache", false);
        aliases = new String[] { 
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
        dataCacheTimeout.setDynamic(true);

        queryCachePlugin = addPlugin("QueryCache", true);
        aliases = new String[] { 
            "true", ConcurrentQueryCache.class.getName(),
            "concurrent", ConcurrentQueryCache.class.getName(), 
            "false", null, 
        };
        queryCachePlugin.setAliases(aliases);
        queryCachePlugin.setDefault(aliases[0]);
        queryCachePlugin.setString(aliases[0]);
        
        refreshFromDataCache = addBoolean("RefreshFromDataCache");
        refreshFromDataCache.setDefault("false");
        refreshFromDataCache.set(false);
        refreshFromDataCache.setDynamic(true);
        
        dynamicDataStructs = addBoolean("DynamicDataStructs");
        dynamicDataStructs.setDefault("false");
        dynamicDataStructs.set(false);

        lockManagerPlugin = addPlugin("LockManager", false);
        aliases =
            new String[] { "none", "org.apache.openjpa.kernel.NoneLockManager",
                "version", "org.apache.openjpa.kernel.VersionLockManager", };
        lockManagerPlugin.setAliases(aliases);
        lockManagerPlugin.setDefault(aliases[0]);
        lockManagerPlugin.setString(aliases[0]);

        inverseManagerPlugin = addPlugin("InverseManager", false);
        aliases =
            new String[] { "false", null, "true",
                "org.apache.openjpa.kernel.InverseManager", };
        inverseManagerPlugin.setAliases(aliases);
        inverseManagerPlugin.setDefault(aliases[0]);
        inverseManagerPlugin.setString(aliases[0]);

        savepointManagerPlugin = addPlugin("SavepointManager", true);
        aliases =
            new String[] { "in-mem",
                "org.apache.openjpa.kernel.InMemorySavepointManager", };
        savepointManagerPlugin.setAliases(aliases);
        savepointManagerPlugin.setDefault(aliases[0]);
        savepointManagerPlugin.setString(aliases[0]);
        savepointManagerPlugin
            .setInstantiatingGetter("getSavepointManagerInstance");

        orphanedKeyPlugin = addPlugin("OrphanedKeyAction", true);
        aliases =
            new String[] { "log",
                "org.apache.openjpa.event.LogOrphanedKeyAction", "exception",
                "org.apache.openjpa.event.ExceptionOrphanedKeyAction", "none",
                "org.apache.openjpa.event.NoneOrphanedKeyAction", };
        orphanedKeyPlugin.setAliases(aliases);
        orphanedKeyPlugin.setDefault(aliases[0]);
        orphanedKeyPlugin.setString(aliases[0]);
        orphanedKeyPlugin
            .setInstantiatingGetter("getOrphanedKeyActionInstance");

        remoteProviderPlugin = new RemoteCommitProviderValue();
        addValue(remoteProviderPlugin);

        transactionMode = addBoolean("TransactionMode");
        aliases = new String[] { "local", "false", "managed", "true", };
        transactionMode.setAliases(aliases);
        transactionMode.setDefault(aliases[0]);

        managedRuntimePlugin = addPlugin("ManagedRuntime", true);
        aliases =
            new String[] { "auto",
                "org.apache.openjpa.ee.AutomaticManagedRuntime", "jndi",
                "org.apache.openjpa.ee.JNDIManagedRuntime", "invocation",
                "org.apache.openjpa.ee.InvocationManagedRuntime", };
        managedRuntimePlugin.setAliases(aliases);
        managedRuntimePlugin.setDefault(aliases[0]);
        managedRuntimePlugin.setString(aliases[0]);
        managedRuntimePlugin
            .setInstantiatingGetter("getManagedRuntimeInstance");

        proxyManagerPlugin = addPlugin("ProxyManager", true);
        aliases =
            new String[] { "default",
                "org.apache.openjpa.util.ProxyManagerImpl" };
        proxyManagerPlugin.setAliases(aliases);
        proxyManagerPlugin.setDefault(aliases[0]);
        proxyManagerPlugin.setString(aliases[0]);
        proxyManagerPlugin.setInstantiatingGetter("getProxyManagerInstance");

        mapping = addString("Mapping");
        metaFactoryPlugin = addPlugin("MetaDataFactory", false);

        metaRepositoryPlugin = (ObjectValue)
            addValue(new MetaDataRepositoryValue());

        connectionFactory = addObject("ConnectionFactory");
        connectionFactory.setInstantiatingGetter("getConnectionFactory");

        connectionFactory2 = addObject("ConnectionFactory2");
        connectionFactory2.setInstantiatingGetter("getConnectionFactory2");
        // This is done because this plug-in may get initialized very lazily
        // when the runtime needs it for flush or a sequence. To keep it
        // dynamic allows it to be set even when the configuration is frozen
        connectionFactory.setDynamic(true);
        connectionFactory2.setDynamic(true);


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
        connectionFactory2Properties =
            addString("ConnectionFactory2Properties");
        connectionFactory2Name = addString("ConnectionFactory2Name");

        connectionFactoryMode = addBoolean("ConnectionFactoryMode");
        aliases = new String[] { "local", "false", "managed", "true", };
        connectionFactoryMode.setAliases(aliases);
        connectionFactoryMode.setDefault(aliases[0]);

        optimistic = addBoolean("Optimistic");
        optimistic.setDefault("true");
        optimistic.set(true);

        autoClear = addInt("AutoClear");
        aliases =
            new String[] { "datastore",
                String.valueOf(AutoClear.CLEAR_DATASTORE), "all",
                String.valueOf(AutoClear.CLEAR_ALL), };
        autoClear.setAliases(aliases);
        autoClear.setDefault(aliases[0]);
        autoClear.set(AutoClear.CLEAR_DATASTORE);
        autoClear.setAliasListComprehensive(true);

        retainState = addBoolean("RetainState");
        retainState.setDefault("true");
        retainState.set(true);

        restoreState = addInt("RestoreState");
        aliases =
            new String[] { "none", String.valueOf(RestoreState.RESTORE_NONE),
                "false", String.valueOf(RestoreState.RESTORE_NONE),
                "immutable", String.valueOf(RestoreState.RESTORE_IMMUTABLE),
                // "true" for compat with jdo RestoreValues
                "true", String.valueOf(RestoreState.RESTORE_IMMUTABLE), "all",
                String.valueOf(RestoreState.RESTORE_ALL), };
        restoreState.setAliases(aliases);
        restoreState.setDefault(aliases[0]);
        restoreState.set(RestoreState.RESTORE_IMMUTABLE);
        restoreState.setAliasListComprehensive(true);

        autoDetach = new AutoDetachValue();
        addValue(autoDetach);

        detachStatePlugin = addPlugin("DetachState", true);
        aliases = new String[] {
            "loaded", DetachOptions.Loaded.class.getName(),
            "fgs", DetachOptions.FetchGroups.class.getName(),
            "fetch-groups", DetachOptions.FetchGroups.class.getName(), 
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
        fetchBatchSize.setDynamic(true);

        maxFetchDepth = addInt("MaxFetchDepth");
        maxFetchDepth.setDefault("-1");
        maxFetchDepth.set(-1);

        fetchGroups = addStringList("FetchGroups");
        fetchGroups.setDefault("default");
        fetchGroups.set(new String[] { "default" });

        flushBeforeQueries = addInt("FlushBeforeQueries");
        aliases =
            new String[] { "true", String.valueOf(QueryFlushModes.FLUSH_TRUE),
                "false", String.valueOf(QueryFlushModes.FLUSH_FALSE),
                "with-connection",
                String.valueOf(QueryFlushModes.FLUSH_WITH_CONNECTION), };
        flushBeforeQueries.setAliases(aliases);
        flushBeforeQueries.setDefault(aliases[0]);
        flushBeforeQueries.set(QueryFlushModes.FLUSH_TRUE);
        flushBeforeQueries.setAliasListComprehensive(true);

        lockTimeout = addInt("LockTimeout");
        lockTimeout.setDefault("-1");
        lockTimeout.set(-1);
        lockTimeout.setDynamic(true);
        
        readLockLevel = addInt("ReadLockLevel");
        aliases =
            new String[] {
                "read", String.valueOf(LockLevels.LOCK_READ),
                "write", String.valueOf(LockLevels.LOCK_WRITE),
                "none", String.valueOf(LockLevels.LOCK_NONE),
            };
        readLockLevel.setAliases(aliases);
        readLockLevel.setDefault(aliases[0]);
        readLockLevel.set(LockLevels.LOCK_READ);
        readLockLevel.setAliasListComprehensive(true);

        writeLockLevel = addInt("WriteLockLevel");
        aliases =
            new String[] {
                "read", String.valueOf(LockLevels.LOCK_READ),
                "write", String.valueOf(LockLevels.LOCK_WRITE),
                "none", String.valueOf(LockLevels.LOCK_NONE),
            };
        writeLockLevel.setAliases(aliases);
        writeLockLevel.setDefault(aliases[1]);
        writeLockLevel.set(LockLevels.LOCK_WRITE);
        writeLockLevel.setAliasListComprehensive(true);

        seqPlugin = new SeqValue("Sequence");
        seqPlugin.setInstantiatingGetter("getSequenceInstance");
        addValue(seqPlugin);

        connectionRetainMode = addInt("ConnectionRetainMode");
        aliases =
            new String[] {
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
        filterListenerPlugins
            .setInstantiatingGetter("getFilterListenerInstances");

        aggregateListenerPlugins = addPluginList("AggregateListeners");
        aggregateListenerPlugins
            .setInstantiatingGetter("getAggregateListenerInstances");

        retryClassRegistration = addBoolean("RetryClassRegistration");

        compatibilityPlugin = addPlugin("Compatibility", true);
        aliases = new String[] { "default", Compatibility.class.getName() };
        compatibilityPlugin.setAliases(aliases);
        compatibilityPlugin.setDefault(aliases[0]);
        compatibilityPlugin.setString(aliases[0]);
        compatibilityPlugin.setInstantiatingGetter("getCompatibilityInstance");
        
        queryCompilationCachePlugin = new QueryCompilationCacheValue(
            "QueryCompilationCache");
        queryCompilationCachePlugin.setInstantiatingGetter(
            "getQueryCompilationCacheInstance");
        addValue(queryCompilationCachePlugin);
        
        runtimeUnenhancedClasses = addInt("RuntimeUnenhancedClasses");
        runtimeUnenhancedClasses.setAliases(new String[] {
            "supported", String.valueOf(
                RuntimeUnenhancedClasssesModes.SUPPORTED),
            "unsupported", String.valueOf(
                RuntimeUnenhancedClasssesModes.UNSUPPORTED),
            "warn", String.valueOf(
                RuntimeUnenhancedClasssesModes.WARN),
        });
        runtimeUnenhancedClasses.setDefault("supported");
        runtimeUnenhancedClasses.setString("supported");
        runtimeUnenhancedClasses.setAliasListComprehensive(true);

        cacheMarshallerPlugins = (CacheMarshallersValue)
            addValue(new CacheMarshallersValue(this));

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
        if (loadGlobals)
            loadGlobals();
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
                getConfigurationLog().warn(
                    _loc.get("diff-specs", this.spec, spec));
            return false;
        }
        this.spec = spec;
        ProductDerivations.afterSpecificationSet(this);
        return true;
    }

    public void setClassResolver(String classResolver) {
        classResolverPlugin.setString(classResolver);
    }

    public String getClassResolver() {
        return classResolverPlugin.getString();
    }

    public void setClassResolver(ClassResolver classResolver) {
        classResolverPlugin.set(classResolver);
    }

    public ClassResolver getClassResolverInstance() {
        if (classResolverPlugin.get() == null)
            classResolverPlugin.instantiate(ClassResolver.class, this);
        return (ClassResolver) classResolverPlugin.get();
    }

    public void setBrokerFactory(String factory) {
        brokerFactoryPlugin.setString(factory);
    }

    public String getBrokerFactory() {
        return brokerFactoryPlugin.getString();
    }

    public void setBrokerImpl(String broker) {
        brokerPlugin.setString(broker);
    }

    public String getBrokerImpl() {
        return brokerPlugin.getString();
    }

    public BrokerImpl newBrokerInstance(String user, String pass) {
        BrokerImpl broker =
            (BrokerImpl) brokerPlugin.instantiate(BrokerImpl.class, this);
        if (broker != null)
            broker.setAuthentication(user, pass);
        return broker;
    }

    public void setDataCacheManager(String mgr) {
        dataCacheManagerPlugin.setString(mgr);
    }

    public String getDataCacheManager() {
        return dataCacheManagerPlugin.getString();
    }

    public void setDataCacheManager(DataCacheManager dcm) {
        if (dcm != null)
            dcm.initialize(this, dataCachePlugin, queryCachePlugin);
        dataCacheManagerPlugin.set(dcm);
    }

    public DataCacheManager getDataCacheManagerInstance() {
        DataCacheManager dcm = (DataCacheManager) dataCacheManagerPlugin.get();
        if (dcm == null) {
            dcm =
                (DataCacheManager) dataCacheManagerPlugin.instantiate(
                    DataCacheManager.class, this);
            dcm.initialize(this, dataCachePlugin, queryCachePlugin);
        }
        return dcm;
    }

    public void setDataCache(String dataCache) {
        dataCachePlugin.setString(dataCache);
    }

    public String getDataCache() {
        return dataCachePlugin.getString();
    }

    public void setDataCacheTimeout(int dataCacheTimeout) {
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
        queryCachePlugin.setString(queryCache);
    }

    public String getQueryCache() {
        return queryCachePlugin.getString();
    }
    
    public boolean getRefreshFromDataCache() {
    	return refreshFromDataCache.get();
    }
    
    public void setRefreshFromDataCache(boolean flag) {
    	refreshFromDataCache.set(flag);
    }
    
    public void setRefreshFromDataCache(Boolean flag) {
    	if (flag != null) {
    		refreshFromDataCache.set(flag.booleanValue());
    	}
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
        inverseManagerPlugin.setString(inverseManager);
    }

    public String getInverseManager() {
        return inverseManagerPlugin.getString();
    }

    public InverseManager newInverseManagerInstance() {
        return (InverseManager) inverseManagerPlugin.instantiate(
            InverseManager.class, this);
    }

    public void setSavepointManager(String savepointManager) {
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
        orphanedKeyPlugin.set(action);
    }

    public void setRemoteCommitProvider(String remoteCommitProvider) {
        remoteProviderPlugin.setString(remoteCommitProvider);
    }

    public String getRemoteCommitProvider() {
        return remoteProviderPlugin.getString();
    }

    public RemoteCommitProvider newRemoteCommitProviderInstance() {
        return remoteProviderPlugin.instantiateProvider(this);
    }

    public void setRemoteCommitEventManager(
        RemoteCommitEventManager remoteEventManager) {
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
        this.transactionMode.setString(transactionMode);
    }

    public String getTransactionMode() {
        return transactionMode.getString();
    }

    public void setTransactionModeManaged(boolean managed) {
        transactionMode.set(managed);
    }

    public boolean isTransactionModeManaged() {
        return transactionMode.get();
    }

    public void setManagedRuntime(String managedRuntime) {
        managedRuntimePlugin.setString(managedRuntime);
    }

    public String getManagedRuntime() {
        return managedRuntimePlugin.getString();
    }

    public void setManagedRuntime(ManagedRuntime managedRuntime) {
        managedRuntimePlugin.set(managedRuntime);
    }

    public ManagedRuntime getManagedRuntimeInstance() {
        if (managedRuntimePlugin.get() == null)
            managedRuntimePlugin.instantiate(ManagedRuntime.class, this);
        return (ManagedRuntime) managedRuntimePlugin.get();
    }

    public void setProxyManager(String proxyManager) {
        proxyManagerPlugin.setString(proxyManager);
    }

    public String getProxyManager() {
        return proxyManagerPlugin.getString();
    }

    public void setProxyManager(ProxyManager proxyManager) {
        proxyManagerPlugin.set(proxyManager);
    }

    public ProxyManager getProxyManagerInstance() {
        if (proxyManagerPlugin.get() == null)
            proxyManagerPlugin.instantiate(ProxyManager.class, this);
        return (ProxyManager) proxyManagerPlugin.get();
    }

    public void setMapping(String mapping) {
        this.mapping.setString(mapping);
    }

    public String getMapping() {
        return mapping.getString();
    }

    public void setMetaDataFactory(String meta) {
        this.metaFactoryPlugin.setString(meta);
    }

    public String getMetaDataFactory() {
        return metaFactoryPlugin.getString();
    }

    public MetaDataFactory newMetaDataFactoryInstance() {
        return (MetaDataFactory) metaFactoryPlugin.instantiate(
            MetaDataFactory.class, this);
    }

    public void setMetaDataRepository(String meta) {
        this.metaRepositoryPlugin.setString(meta);
    }

    public String getMetaDataRepository() {
        return metaRepositoryPlugin.getString();
    }

    public void setMetaDataRepository(MetaDataRepository meta) {
        metaRepository = meta;
    }

    public MetaDataRepository getMetaDataRepositoryInstance() {
        if (metaRepository == null)
            metaRepository = newMetaDataRepositoryInstance();
        return metaRepository;
    }
    
    public boolean metaDataRepositoryAvailable(){ 
        return metaRepository != null; 
    }

    public MetaDataRepository newMetaDataRepositoryInstance() {
        return (MetaDataRepository) metaRepositoryPlugin.instantiate(
            MetaDataRepository.class, this);
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName.setString(connectionUserName);
    }

    public String getConnectionUserName() {
        return connectionUserName.getString();
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword.setString(connectionPassword);
    }

    public String getConnectionPassword() {
        return connectionPassword.getString();
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL.setString(connectionURL);
    }

    public String getConnectionURL() {
        return connectionURL.getString();
    }

    public void setConnectionDriverName(String driverName) {
        this.connectionDriverName.setString(driverName);
    }

    public String getConnectionDriverName() {
        return connectionDriverName.getString();
    }

    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties.setString(connectionProperties);
    }

    public String getConnectionProperties() {
        return connectionProperties.getString();
    }

    public void setConnectionFactoryProperties(
        String connectionFactoryProperties) {
        this.connectionFactoryProperties.setString(connectionFactoryProperties);
    }

    public String getConnectionFactoryProperties() {
        return connectionFactoryProperties.getString();
    }

    public String getConnectionFactoryMode() {
        return connectionFactoryMode.getString();
    }

    public void setConnectionFactoryMode(String mode) {
        connectionFactoryMode.setString(mode);
    }

    public boolean isConnectionFactoryModeManaged() {
        return connectionFactoryMode.get();
    }

    public void setConnectionFactoryModeManaged(boolean managed) {
        connectionFactoryMode.set(managed);
    }

    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName.setString(connectionFactoryName);
    }

    public String getConnectionFactoryName() {
        return connectionFactoryName.getString();
    }

    public void setConnectionFactory(Object factory) {
        connectionFactory.set(factory);
    }

    public Object getConnectionFactory() {
        if (connectionFactory.get() == null)
            connectionFactory.set(
                lookupConnectionFactory(getConnectionFactoryName()), true);
        return connectionFactory.get();
    }

    /**
     * Lookup the connection factory at the given name.
     */
    private Object lookupConnectionFactory(String name) {
        name = StringUtils.trimToNull(name);
        if (name == null)
            return null;
        try {
        	return Configurations.lookup(name);
        } catch (Exception ex) {
        	return null;
        }
    }

    public void setConnection2UserName(String connection2UserName) {
        this.connection2UserName.setString(connection2UserName);
    }

    public String getConnection2UserName() {
        return connection2UserName.getString();
    }

    public void setConnection2Password(String connection2Password) {
        this.connection2Password.setString(connection2Password);
    }

    public String getConnection2Password() {
        return connection2Password.getString();
    }

    public void setConnection2URL(String connection2URL) {
        this.connection2URL.setString(connection2URL);
    }

    public String getConnection2URL() {
        return connection2URL.getString();
    }

    public void setConnection2DriverName(String driverName) {
        this.connection2DriverName.setString(driverName);
    }

    public String getConnection2DriverName() {
        return connection2DriverName.getString();
    }

    public void setConnection2Properties(String connection2Properties) {
        this.connection2Properties.setString(connection2Properties);
    }

    public String getConnection2Properties() {
        return connection2Properties.getString();
    }

    public void setConnectionFactory2Properties(
        String connectionFactory2Properties) {
        this.connectionFactory2Properties
            .setString(connectionFactory2Properties);
    }

    public String getConnectionFactory2Properties() {
        return connectionFactory2Properties.getString();
    }

    public void setConnectionFactory2Name(String connectionFactory2Name) {
        this.connectionFactory2Name.setString(connectionFactory2Name);
    }

    public String getConnectionFactory2Name() {
        return connectionFactory2Name.getString();
    }

    public void setConnectionFactory2(Object factory) {
        connectionFactory2.set(factory);
    }

    public Object getConnectionFactory2() {
        if (connectionFactory2.get() == null)
            connectionFactory2.set(
                lookupConnectionFactory(getConnectionFactory2Name()), false);
        return connectionFactory2.get();
    }

    public void setOptimistic(boolean optimistic) {
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
        autoClear.setString(clear);
    }

    public String getAutoClear() {
        return autoClear.getString();
    }

    public void setAutoClear(int clear) {
        autoClear.set(clear);
    }

    public int getAutoClearConstant() {
        return autoClear.get();
    }

    public void setRetainState(boolean retainState) {
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
        this.restoreState.setString(restoreState);
    }

    public String getRestoreState() {
        return restoreState.getString();
    }

    public void setRestoreState(int restoreState) {
        this.restoreState.set(restoreState);
    }

    public int getRestoreStateConstant() {
        return restoreState.get();
    }

    public void setAutoDetach(String autoDetach) {
        this.autoDetach.setString(autoDetach);
    }

    public String getAutoDetach() {
        return autoDetach.getString();
    }

    public void setAutoDetach(int autoDetachFlags) {
        autoDetach.setConstant(autoDetachFlags);
    }

    public int getAutoDetachConstant() {
        return autoDetach.getConstant();
    }

    public void setDetachState(String detachState) {
        detachStatePlugin.setString(detachState);
    }

    public String getDetachState() {
        return detachStatePlugin.getString();
    }

    public void setDetachState(DetachOptions detachState) {
        detachStatePlugin.set(detachState);
    }

    public DetachOptions getDetachStateInstance() {
        if (detachStatePlugin.get() == null)
            detachStatePlugin.instantiate(DetachOptions.class, this);
        return (DetachOptions) detachStatePlugin.get();
    }

    public void setIgnoreChanges(boolean ignoreChanges) {
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
        this.fetchBatchSize.set(fetchBatchSize);
    }

    public void setFetchBatchSize(Integer fetchBatchSize) {
        if (fetchBatchSize != null)
            setFetchBatchSize(fetchBatchSize.intValue());
    }

    public int getFetchBatchSize() {
        return fetchBatchSize.get();
    }

    public void setMaxFetchDepth(int maxFetchDepth) {
        this.maxFetchDepth.set(maxFetchDepth);
    }

    public void setMaxFetchDepth(Integer maxFetchDepth) {
        if (maxFetchDepth != null)
            setMaxFetchDepth(maxFetchDepth.intValue());
    }

    public int getMaxFetchDepth() {
        return maxFetchDepth.get();
    }

    public void setFetchGroups(String fetchGroups) {
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
        flushBeforeQueries.setString(flush);
    }

    public String getFlushBeforeQueries() {
        return flushBeforeQueries.getString();
    }

    public void setFlushBeforeQueries(int flush) {
        flushBeforeQueries.set(flush);
    }

    public int getFlushBeforeQueriesConstant() {
        return flushBeforeQueries.get();
    }

    public void setLockTimeout(int timeout) {
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
        readLockLevel.setString(level);
    }

    public String getReadLockLevel() {
        return readLockLevel.getString();
    }

    public void setReadLockLevel(int level) {
        readLockLevel.set(level);
    }

    public int getReadLockLevelConstant() {
        return readLockLevel.get();
    }

    public void setWriteLockLevel(String level) {
        writeLockLevel.setString(level);
    }

    public String getWriteLockLevel() {
        return writeLockLevel.getString();
    }

    public void setWriteLockLevel(int level) {
        writeLockLevel.set(level);
    }

    public int getWriteLockLevelConstant() {
        return writeLockLevel.get();
    }

    public void setSequence(String sequence) {
        seqPlugin.setString(sequence);
    }

    public String getSequence() {
        return seqPlugin.getString();
    }

    public void setSequence(Seq seq) {
        seqPlugin.set(seq);
    }

    public Seq getSequenceInstance() {
        if (seqPlugin.get() == null)
            seqPlugin.instantiate(Seq.class, this);
        return (Seq) seqPlugin.get();
    }

    public void setConnectionRetainMode(String connectionRetainMode) {
        this.connectionRetainMode.setString(connectionRetainMode);
    }

    public String getConnectionRetainMode() {
        return connectionRetainMode.getString();
    }

    public void setConnectionRetainMode(int connectionRetainMode) {
        this.connectionRetainMode.set(connectionRetainMode);
    }

    public int getConnectionRetainModeConstant() {
        return connectionRetainMode.get();
    }

    public void setFilterListeners(String filterListeners) {
        filterListenerPlugins.setString(filterListeners);
    }

    public String getFilterListeners() {
        return filterListenerPlugins.getString();
    }

    public void setFilterListeners(FilterListener[] listeners) {
        filterListenerPlugins.set(listeners);
    }

    public FilterListener[] getFilterListenerInstances() {
        if (filterListenerPlugins.get() == null)
            filterListenerPlugins.instantiate(FilterListener.class, this);
        return (FilterListener[]) filterListenerPlugins.get();
    }

    public void setAggregateListeners(String aggregateListeners) {
        aggregateListenerPlugins.setString(aggregateListeners);
    }

    public String getAggregateListeners() {
        return aggregateListenerPlugins.getString();
    }

    public void setAggregateListeners(AggregateListener[] listeners) {
        aggregateListenerPlugins.set(listeners);
    }

    public AggregateListener[] getAggregateListenerInstances() {
        if (aggregateListenerPlugins.get() == null)
            aggregateListenerPlugins.instantiate(AggregateListener.class, this);
        return (AggregateListener[]) aggregateListenerPlugins.get();
    }

    public void setRetryClassRegistration(boolean retry) {
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

    public String getQueryCompilationCache() {
        return queryCompilationCachePlugin.getString();
    }

    public void setQueryCompilationCache(String queryCompilationCache) {
        queryCompilationCachePlugin.setString(queryCompilationCache);
    }
    
    public Map getQueryCompilationCacheInstance() {
        if (queryCompilationCachePlugin.get() == null)
            queryCompilationCachePlugin.instantiate(Map.class, this);
        return (Map) queryCompilationCachePlugin.get();
    }

    public StoreFacadeTypeRegistry getStoreFacadeTypeRegistry() {
        return _storeFacadeRegistry;
    }

    public BrokerFactoryEventManager getBrokerFactoryEventManager() {
        return _brokerFactoryEventManager;
    }

    public String getRuntimeUnenhancedClasses() {
        return runtimeUnenhancedClasses.getString();
    }

    public int getRuntimeUnenhancedClassesConstant() {
        return runtimeUnenhancedClasses.get();
    }

    public void setRuntimeUnenhancedClasses(int mode) {
        runtimeUnenhancedClasses.set(mode);
    }

    public String getCacheMarshallers() {
        return cacheMarshallerPlugins.getString();
    }

    public void setCacheMarshallers(String marshallers) {
        cacheMarshallerPlugins.setString(marshallers);
    }

    public Map getCacheMarshallerInstances() {
        return cacheMarshallerPlugins.getInstancesAsMap();
    }

    public void setRuntimeUnenhancedClasses(String mode) {
        runtimeUnenhancedClasses.setString(mode);
    }

    public void instantiateAll() {
        super.instantiateAll();
        getMetaDataRepositoryInstance();
        getRemoteCommitEventManager();
        cacheMarshallerPlugins.initialize();
    }

    protected void preClose() {
        ImplHelper.close(metaRepository);
        ImplHelper.close(remoteEventManager);
        super.preClose();
    }

    public Log getConfigurationLog() {
        return getLog(LOG_RUNTIME);
    }
}

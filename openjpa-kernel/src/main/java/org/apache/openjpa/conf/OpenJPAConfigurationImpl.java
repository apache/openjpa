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

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.openjpa.audit.AuditLogger;
import org.apache.openjpa.audit.Auditor;
import org.apache.openjpa.datacache.CacheDistributionPolicy;
import org.apache.openjpa.datacache.ConcurrentDataCache;
import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCacheManagerImpl;
import org.apache.openjpa.datacache.PartitionedDataCache;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.enhance.RuntimeUnenhancedClassesModes;
import org.apache.openjpa.event.BrokerFactoryEventManager;
import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.instrumentation.InstrumentationManager;
import org.apache.openjpa.instrumentation.InstrumentationManagerImpl;
import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.kernel.FinderCache;
import org.apache.openjpa.kernel.InverseManager;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.RestoreState;
import org.apache.openjpa.kernel.SavepointManager;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.conf.BooleanValue;
import org.apache.openjpa.lib.conf.ClassListValue;
import org.apache.openjpa.lib.conf.ConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.IntValue;
import org.apache.openjpa.lib.conf.ObjectValue;
import org.apache.openjpa.lib.conf.PluginListValue;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.lib.conf.StringListValue;
import org.apache.openjpa.lib.conf.StringValue;
import org.apache.openjpa.lib.encryption.EncryptionProvider;
import org.apache.openjpa.lib.instrumentation.InstrumentationLevel;
import org.apache.openjpa.lib.instrumentation.InstrumentationProvider;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.ProxyManager;
import org.apache.openjpa.util.StoreFacadeTypeRegistry;
import org.apache.openjpa.validation.ValidatingLifecycleEventManager;

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

    // private static final Localizer _loc = Localizer.forPackage(OpenJPAConfigurationImpl.class);

    // cached state; some of this is created in getter methods, so make
    // protected in case subclasses want to access without creating
    protected MetaDataRepository metaRepository = null;
    protected RemoteCommitEventManager remoteEventManager = null;

    // openjpa properties
    public ObjectValue classResolverPlugin;
    public BrokerValue brokerPlugin;
    public ObjectValue dataCachePlugin;
    public ObjectValue dataCacheManagerPlugin;
    public ObjectValue auditorPlugin;
    public ObjectValue cacheDistributionPolicyPlugin;
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
    public IntValue queryTimeout;
    public IntValue readLockLevel;
    public IntValue writeLockLevel;
    public ObjectValue seqPlugin;
    public PluginListValue filterListenerPlugins;
    public PluginListValue aggregateListenerPlugins;
    public BooleanValue retryClassRegistration;
    public ObjectValue proxyManagerPlugin;
    public StringValue connectionUserName;
    public StringValue connectionPassword;
    public PluginValue encryptionProvider;
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
    public MetaDataRepositoryValue metaRepositoryPlugin;
    public ObjectValue lockManagerPlugin;
    public ObjectValue inverseManagerPlugin;
    public ObjectValue savepointManagerPlugin;
    public ObjectValue orphanedKeyPlugin;
    public ObjectValue compatibilityPlugin;
    public ObjectValue callbackPlugin;
    public QueryCompilationCacheValue queryCompilationCachePlugin;
    public IntValue runtimeUnenhancedClasses;
    public CacheMarshallersValue cacheMarshallerPlugins;
    public BooleanValue eagerInitialization;
    public PluginValue preparedQueryCachePlugin;
    public PluginValue finderCachePlugin;
    public ObjectValue specification;
    public StringValue validationMode;
    public ObjectValue validationFactory;
    public ObjectValue validator;
    public ObjectValue lifecycleEventManager;
    public StringValue validationGroupPrePersist;
    public StringValue validationGroupPreUpdate;
    public StringValue validationGroupPreRemove;
    public StringValue dataCacheMode;
    public BooleanValue dynamicEnhancementAgent;
    public ObjectValue instrumentationManager;
    public PluginListValue instrumentationProviders;
    public BooleanValue postLoadOnMerge;
    public BooleanValue optimizeIdCopy;
    public BooleanValue useTcclForSelectNew;
    public ClassListValue typesWithoutEnhancement;

    // JPA Properties
    public IntValue databaseAction;
    public IntValue scriptsAction;
    public IntValue createSource;
    public IntValue dropSource;
    public StringValue createScriptSource;
    public StringValue dropScriptSource;
    public StringValue createScriptTarget;
    public StringValue dropScriptTarget;
    public StringValue loadScriptSource;

    // custom values
    public BrokerFactoryValue brokerFactoryPlugin;
    public RemoteCommitProviderValue remoteProviderPlugin;
    public AutoDetachValue autoDetach;

    private Collection<String> supportedOptions = new HashSet<>(33);
    private final StoreFacadeTypeRegistry _storeFacadeRegistry = new StoreFacadeTypeRegistry();
    private BrokerFactoryEventManager _brokerFactoryEventManager = new BrokerFactoryEventManager(this);
    private Map<String, Object> _peMap; //contains persistence environment-specific info
    private boolean _allowSetLifeCycleEventManager = true;
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
        aliases = new String[] {
                "default", "org.apache.openjpa.util.ClassResolverImpl",
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

        cacheDistributionPolicyPlugin = addPlugin("CacheDistributionPolicy", true);
        aliases = new String[] {
                "default",    "org.apache.openjpa.datacache.DefaultCacheDistributionPolicy",
                "type-based", "org.apache.openjpa.datacache.TypeBasedCacheDistributionPolicy"};
        cacheDistributionPolicyPlugin.setAliases(aliases);
        cacheDistributionPolicyPlugin.setDefault(aliases[0]);
        cacheDistributionPolicyPlugin.setString(aliases[0]);
        cacheDistributionPolicyPlugin.setInstantiatingGetter("getCacheDistributionPolicy");

        dataCachePlugin = addPlugin("DataCache", false);
        aliases = new String[] {
            "false", null,
            "true", ConcurrentDataCache.class.getName(),
            "concurrent", ConcurrentDataCache.class.getName(),
            "partitioned", PartitionedDataCache.class.getName(),
        };
        dataCachePlugin.setAliases(aliases);
        dataCachePlugin.setDefault(aliases[0]);
        dataCachePlugin.setString(aliases[0]);

        dataCacheTimeout = addInt("DataCacheTimeout");
        dataCacheTimeout.setDefault("-1");
        dataCacheTimeout.set(-1);
        dataCacheTimeout.setDynamic(true);

        queryCachePlugin = addPlugin("QueryCache", false);
        aliases = new String[] {
            "false", null,
            "true", ConcurrentQueryCache.class.getName(),
            "concurrent", ConcurrentQueryCache.class.getName(),
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
            new String[] {
                "none", "org.apache.openjpa.kernel.NoneLockManager",
                "version", "org.apache.openjpa.kernel.VersionLockManager", };
        lockManagerPlugin.setAliases(aliases);
        lockManagerPlugin.setDefault(aliases[0]);
        lockManagerPlugin.setString(aliases[0]);

        inverseManagerPlugin = addPlugin("InverseManager", false);
        aliases = new String[] {
                "false", null,
                "true",  "org.apache.openjpa.kernel.InverseManager", };
        inverseManagerPlugin.setAliases(aliases);
        inverseManagerPlugin.setDefault(aliases[0]);
        inverseManagerPlugin.setString(aliases[0]);

        savepointManagerPlugin = addPlugin("SavepointManager", true);
        aliases = new String[] {
                "in-mem", "org.apache.openjpa.kernel.InMemorySavepointManager", };
        savepointManagerPlugin.setAliases(aliases);
        savepointManagerPlugin.setDefault(aliases[0]);
        savepointManagerPlugin.setString(aliases[0]);
        savepointManagerPlugin.setInstantiatingGetter("getSavepointManagerInstance");

        orphanedKeyPlugin = addPlugin("OrphanedKeyAction", true);
        aliases = new String[] {
                "log",       "org.apache.openjpa.event.LogOrphanedKeyAction",
                "exception", "org.apache.openjpa.event.ExceptionOrphanedKeyAction",
                "none",      "org.apache.openjpa.event.NoneOrphanedKeyAction", };
        orphanedKeyPlugin.setAliases(aliases);
        orphanedKeyPlugin.setDefault(aliases[0]);
        orphanedKeyPlugin.setString(aliases[0]);
        orphanedKeyPlugin.setInstantiatingGetter("getOrphanedKeyActionInstance");

        remoteProviderPlugin = new RemoteCommitProviderValue();
        addValue(remoteProviderPlugin);

        transactionMode = addBoolean("TransactionMode");
        aliases = new String[] { "local", "false", "managed", "true", };
        transactionMode.setAliases(aliases);
        transactionMode.setDefault(aliases[0]);

        managedRuntimePlugin = addPlugin("ManagedRuntime", true);
        aliases = new String[] {
                "auto",       "org.apache.openjpa.ee.AutomaticManagedRuntime",
                "jndi",       "org.apache.openjpa.ee.JNDIManagedRuntime",
                "invocation", "org.apache.openjpa.ee.InvocationManagedRuntime", };
        managedRuntimePlugin.setAliases(aliases);
        managedRuntimePlugin.setDefault(aliases[0]);
        managedRuntimePlugin.setString(aliases[0]);
        managedRuntimePlugin
            .setInstantiatingGetter("getManagedRuntimeInstance");

        proxyManagerPlugin = addPlugin("ProxyManager", true);
        aliases = new String[] {
                "default", "org.apache.openjpa.util.ProxyManagerImpl" };
        proxyManagerPlugin.setAliases(aliases);
        proxyManagerPlugin.setDefault(aliases[0]);
        proxyManagerPlugin.setString(aliases[0]);
        proxyManagerPlugin.setInstantiatingGetter("getProxyManagerInstance");

        mapping = addString("Mapping");
        metaFactoryPlugin = addPlugin("MetaDataFactory", false);

        metaRepositoryPlugin = (MetaDataRepositoryValue) addValue(new MetaDataRepositoryValue());

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
        connectionUserName.addEquivalentKey("jakarta.persistence.jdbc.user");

        connectionPassword = addString("ConnectionPassword");
        connectionPassword.addEquivalentKey("jakarta.persistence.jdbc.password");
        connectionPassword.hide();

        encryptionProvider = addPlugin("EncryptionProvider",true);

        connectionURL = addString("ConnectionURL");
        connectionURL.addEquivalentKey("jakarta.persistence.jdbc.url");

        connectionDriverName = addString("ConnectionDriverName");
        connectionDriverName.addEquivalentKey("jakarta.persistence.jdbc.driver");

        connectionFactoryName = addString("ConnectionFactoryName");
        connectionProperties = addString("ConnectionProperties");
        connectionFactoryProperties = addString("ConnectionFactoryProperties");
        connection2UserName = addString("Connection2UserName");
        connection2Password = addString("Connection2Password");
        connection2Password.hide();

        connection2URL = addString("Connection2URL");
        connection2DriverName = addString("Connection2DriverName");
        connection2Properties = addString("Connection2Properties");
        connectionFactory2Properties = addString("ConnectionFactory2Properties");
        connectionFactory2Name = addString("ConnectionFactory2Name");

        connectionFactoryMode = addBoolean("ConnectionFactoryMode");
        aliases = new String[] { "local", "false", "managed", "true", };
        connectionFactoryMode.setAliases(aliases);
        connectionFactoryMode.setDefault(aliases[0]);

        optimistic = addBoolean("Optimistic");
        optimistic.setDefault("true");
        optimistic.set(true);

        postLoadOnMerge = addBoolean("PostLoadOnMerge");
        postLoadOnMerge.setDefault("false");
        postLoadOnMerge.set(false);

        optimizeIdCopy = addBoolean("OptimizeIdCopy");
        optimizeIdCopy.setDefault("false");
        optimizeIdCopy.set(false);

        databaseAction = addInt("jakarta.persistence.schema-generation.database.action");
        aliases = new String[] {
                "none", String.valueOf(SchemaGenerationAction.NONE),
                "create", String.valueOf(SchemaGenerationAction.CREATE),
                "drop-and-create", String.valueOf(SchemaGenerationAction.DROP_AND_CREATE),
                "drop", String.valueOf(SchemaGenerationAction.DROP)
        };
        databaseAction.setAliases(aliases);
        databaseAction.setDefault(aliases[0]);
        databaseAction.setAliasListComprehensive(true);

        scriptsAction = addInt("jakarta.persistence.schema-generation.scripts.action");
        aliases = new String[] {
                "none", String.valueOf(SchemaGenerationAction.NONE),
                "create", String.valueOf(SchemaGenerationAction.CREATE),
                "drop-and-create", String.valueOf(SchemaGenerationAction.DROP_AND_CREATE),
                "drop", String.valueOf(SchemaGenerationAction.DROP)
        };
        scriptsAction.setAliases(aliases);
        scriptsAction.setDefault(aliases[0]);
        scriptsAction.setAliasListComprehensive(true);

        createSource = addInt("jakarta.persistence.schema-generation.create-source");
        aliases = new String[] {
                "none", String.valueOf(SchemaGenerationSource.NONE),
                "metadata", String.valueOf(SchemaGenerationSource.METADATA),
                "script", String.valueOf(SchemaGenerationSource.SCRIPT),
                "metadata-then-script", String.valueOf(SchemaGenerationSource.METADATA_THEN_SCRIPT),
                "script-then-metadata", String.valueOf(SchemaGenerationSource.SCRIPT_THEN_METADATA)
        };
        createSource.setAliases(aliases);
        createSource.setDefault(aliases[0]);
        createSource.setAliasListComprehensive(true);

        dropSource = addInt("jakarta.persistence.schema-generation.drop-source");
        aliases = new String[] {
                "metadata", String.valueOf(SchemaGenerationSource.METADATA),
                "script", String.valueOf(SchemaGenerationSource.SCRIPT),
                "metadata-then-script", String.valueOf(SchemaGenerationSource.METADATA_THEN_SCRIPT),
                "script-then-metadata", String.valueOf(SchemaGenerationSource.SCRIPT_THEN_METADATA)
        };
        dropSource.setAliases(aliases);
        dropSource.setDefault(aliases[0]);
        dropSource.setAliasListComprehensive(true);

        createScriptSource = addString("jakarta.persistence.schema-generation.create-script-source");
        dropScriptSource = addString("jakarta.persistence.schema-generation.drop-script-source");
        createScriptTarget = addString("jakarta.persistence.schema-generation.scripts.create-target");
        dropScriptTarget = addString("jakarta.persistence.schema-generation.scripts.drop-target");
        loadScriptSource = addString("jakarta.persistence.sql-load-script-source");

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
        aliases = new String[] {
                "none",      String.valueOf(RestoreState.RESTORE_NONE),
                "false",     String.valueOf(RestoreState.RESTORE_NONE),
                "immutable", String.valueOf(RestoreState.RESTORE_IMMUTABLE),
                // "true" for compat with jdo RestoreValues
                "true",      String.valueOf(RestoreState.RESTORE_IMMUTABLE),
                "all",       String.valueOf(RestoreState.RESTORE_ALL), };
        restoreState.setAliases(aliases);
        restoreState.setDefault(aliases[0]);
        restoreState.set(RestoreState.RESTORE_IMMUTABLE);
        restoreState.setAliasListComprehensive(true);

        autoDetach = new AutoDetachValue();
        addValue(autoDetach);

        detachStatePlugin = addPlugin("DetachState", true);
        aliases = new String[] {
            "loaded",       DetachOptions.Loaded.class.getName(),
            "fgs",          DetachOptions.FetchGroups.class.getName(),
            "fetch-groups", DetachOptions.FetchGroups.class.getName(),
            "all",          DetachOptions.All.class.getName(),
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
        aliases = new String[] {
                "true",            String.valueOf(QueryFlushModes.FLUSH_TRUE),
                "false",           String.valueOf(QueryFlushModes.FLUSH_FALSE),
                "with-connection", String.valueOf(QueryFlushModes.FLUSH_WITH_CONNECTION),
        };
        flushBeforeQueries.setAliases(aliases);
        flushBeforeQueries.setDefault(aliases[0]);
        flushBeforeQueries.set(QueryFlushModes.FLUSH_TRUE);
        flushBeforeQueries.setAliasListComprehensive(true);

        lockTimeout = addInt("LockTimeout");
        lockTimeout.addEquivalentKey("jakarta.persistence.lock.timeout");
        lockTimeout.setDefault("-1");
        lockTimeout.setDynamic(true);

        readLockLevel = addInt("ReadLockLevel");
        aliases = new String[] {
                "read", String.valueOf(LockLevels.LOCK_READ),
                "write", String.valueOf(LockLevels.LOCK_WRITE),
                "none", String.valueOf(LockLevels.LOCK_NONE),
            };
        readLockLevel.setAliases(aliases);
        readLockLevel.setDefault(aliases[0]);
        readLockLevel.set(LockLevels.LOCK_READ);
        readLockLevel.setAliasListComprehensive(true);

        writeLockLevel = addInt("WriteLockLevel");
        aliases = new String[] {
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
        aliases = new String[] {
                "on-demand",   String.valueOf(ConnectionRetainModes.CONN_RETAIN_DEMAND),
                "transaction", String.valueOf(ConnectionRetainModes.CONN_RETAIN_TRANS),
                "always",      String.valueOf(ConnectionRetainModes.CONN_RETAIN_ALWAYS),
                // deprecated
                "persistence-manager", String.valueOf(ConnectionRetainModes.CONN_RETAIN_ALWAYS),
            };
        connectionRetainMode.setAliases(aliases);
        connectionRetainMode.setDefault(aliases[0]);
        connectionRetainMode.setAliasListComprehensive(true);
        connectionRetainMode.set(ConnectionRetainModes.CONN_RETAIN_DEMAND);

        filterListenerPlugins = addPluginList("FilterListeners");
        filterListenerPlugins.setInstantiatingGetter("getFilterListenerInstances");

        aggregateListenerPlugins = addPluginList("AggregateListeners");
        aggregateListenerPlugins.setInstantiatingGetter("getAggregateListenerInstances");

        retryClassRegistration = addBoolean("RetryClassRegistration");

        compatibilityPlugin = addPlugin("Compatibility", true);
        aliases = new String[] { "default", Compatibility.class.getName() };
        compatibilityPlugin.setAliases(aliases);
        compatibilityPlugin.setDefault(aliases[0]);
        compatibilityPlugin.setString(aliases[0]);
        compatibilityPlugin.setInstantiatingGetter("getCompatibilityInstance");

        callbackPlugin = addPlugin("Callbacks", true);
        aliases = new String[] { "default", CallbackOptions.class.getName() };
        callbackPlugin.setAliases(aliases);
        callbackPlugin.setDefault(aliases[0]);
        callbackPlugin.setString(aliases[0]);
        callbackPlugin.setInstantiatingGetter("getCallbackOptionsInstance");

        queryCompilationCachePlugin = new QueryCompilationCacheValue("QueryCompilationCache");
        queryCompilationCachePlugin.setInstantiatingGetter("getQueryCompilationCacheInstance");
        addValue(queryCompilationCachePlugin);

        runtimeUnenhancedClasses = addInt("RuntimeUnenhancedClasses");
        runtimeUnenhancedClasses.setAliases(new String[] {
            "supported",   String.valueOf(RuntimeUnenhancedClassesModes.SUPPORTED),
            "unsupported", String.valueOf(RuntimeUnenhancedClassesModes.UNSUPPORTED),
            "warn",        String.valueOf(RuntimeUnenhancedClassesModes.WARN),
        });
        runtimeUnenhancedClasses.setDefault("unsupported");
        runtimeUnenhancedClasses.setString("unsupported");
        runtimeUnenhancedClasses.setAliasListComprehensive(true);

        cacheMarshallerPlugins = (CacheMarshallersValue) addValue(new CacheMarshallersValue(this));

        eagerInitialization = addBoolean("InitializeEagerly");

        specification = new SpecificationPlugin(this, "Specification");
        addValue(specification);
        specification.setInstantiatingGetter("getSpecificationInstance");

        queryTimeout = addInt("jakarta.persistence.query.timeout");
        queryTimeout.setDefault("-1");
        queryTimeout.setDynamic(true);

        lifecycleEventManager = addPlugin("LifecycleEventManager", true);
        aliases = new String[] {
            "default", LifecycleEventManager.class.getName(),
            "validating", ValidatingLifecycleEventManager.class.getName(),
        };
        lifecycleEventManager.setAliases(aliases);
        lifecycleEventManager.setDefault(aliases[0]);
        lifecycleEventManager.setString(aliases[0]);
        lifecycleEventManager.setInstantiatingGetter("getLifecycleEventManagerInstance");

        dynamicEnhancementAgent  = addBoolean("DynamicEnhancementAgent");
        dynamicEnhancementAgent.setDefault("true");
        dynamicEnhancementAgent.set(true);

        instrumentationManager = addPlugin("InstrumentationManager", true);
        aliases =
            new String[] { "default", InstrumentationManagerImpl.class.getName(), };
        instrumentationManager.setAliases(aliases);
        instrumentationManager.setDefault(aliases[0]);
        instrumentationManager.setString(aliases[0]);
        instrumentationManager.setInstantiatingGetter("getInstrumentationManager");

        instrumentationProviders = addPluginList("Instrumentation");
        aliases = new String[] { "jmx", "org.apache.openjpa.instrumentation.jmx.JMXProvider" };
        instrumentationProviders.setAliases(aliases);
        instrumentationProviders.setInstantiatingGetter("getInstrumentationInstances");

        auditorPlugin = addPlugin("Auditor", true);
        aliases = new String[] { "default", AuditLogger.class.getName(), };
        auditorPlugin.setAliases(aliases);
        auditorPlugin.setInstantiatingGetter("getAuditorInstance");

        useTcclForSelectNew = addBoolean("UseTCCLinSelectNew");
        useTcclForSelectNew.setDefault("false");
        useTcclForSelectNew.set(false);

        typesWithoutEnhancement = new ClassListValue();

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
        supportedOptions.add(OPTION_POSTLOAD_ON_MERGE);
        supportedOptions.add(OPTION_USE_TCCL_IN_SELECT_NEW);

        if (derivations)
            ProductDerivations.beforeConfigurationLoad(this);
        if (loadGlobals)
            loadGlobals();
    }

    @Override
    public Collection<String> supportedOptions() {
        return supportedOptions;
    }

    /**
     * Get the name of the Specification only (not the version or other
     * information) or an empty String if not set.
     *
     */
    @Override
    public String getSpecification() {
        Specification spec = getSpecificationInstance();
        return spec == null ? "" : spec.getName();
    }

    @Override
    public Specification getSpecificationInstance() {
        return (Specification)specification.get();
    }

    /**
     * Sets Specification from the given String.
     *
     * @param spec should be encoded in the format specified in {@link
     * Specification}.
     */
    @Override
    public void setSpecification(String spec) {
        specification.setString(spec);
    }

    @Override
    public void setSpecification(Specification newSpec) {
        specification.set(newSpec);
    }

    @Override
    public void setClassResolver(String classResolver) {
        classResolverPlugin.setString(classResolver);
    }

    @Override
    public String getClassResolver() {
        return classResolverPlugin.getString();
    }

    @Override
    public void setClassResolver(ClassResolver classResolver) {
        classResolverPlugin.set(classResolver);
    }

    @Override
    public ClassResolver getClassResolverInstance() {
        if (classResolverPlugin.get() == null)
            classResolverPlugin.instantiate(ClassResolver.class, this);
        return (ClassResolver) classResolverPlugin.get();
    }

    @Override
    public void setBrokerFactory(String factory) {
        brokerFactoryPlugin.setString(factory);
    }

    @Override
    public String getBrokerFactory() {
        return brokerFactoryPlugin.getString();
    }

    @Override
    public void setBrokerImpl(String broker) {
        brokerPlugin.setString(broker);
    }

    @Override
    public String getBrokerImpl() {
        return brokerPlugin.getString();
    }

    @Override
    public BrokerImpl newBrokerInstance(String user, String pass) {
        BrokerImpl broker = (BrokerImpl) brokerPlugin.instantiate(BrokerImpl.class, this);
        if (broker != null)
            broker.setAuthentication(user, pass);
        return broker;
    }

    @Override
    public void setDataCacheManager(String mgr) {
        dataCacheManagerPlugin.setString(mgr);
    }

    @Override
    public String getDataCacheManager() {
        return dataCacheManagerPlugin.getString();
    }

    @Override
    public void setDataCacheManager(DataCacheManager dcm) {
        if (dcm != null)
            dcm.initialize(this, dataCachePlugin, queryCachePlugin);
        dataCacheManagerPlugin.set(dcm);
    }

    @Override
    public DataCacheManager getDataCacheManagerInstance() {
        DataCacheManager dcm = (DataCacheManager) dataCacheManagerPlugin.get();
        if (dcm == null) {
            dcm = (DataCacheManager) dataCacheManagerPlugin.instantiate(DataCacheManager.class, this);
            if (dcm != null) {
                dcm.initialize(this, dataCachePlugin, queryCachePlugin);
            }
        }
        return dcm;
    }

    @Override
    public void setDataCache(String dataCache) {
        dataCachePlugin.setString(dataCache);
    }

    @Override
    public String getDataCache() {
        return dataCachePlugin.getString();
    }

    @Override
    public void setDataCacheTimeout(int dataCacheTimeout) {
        this.dataCacheTimeout.set(dataCacheTimeout);
    }

    @Override
    public void setDataCacheTimeout(Integer dataCacheTimeout) {
        if (dataCacheTimeout != null)
            setDataCacheTimeout(dataCacheTimeout.intValue());
    }

    @Override
    public int getDataCacheTimeout() {
        return dataCacheTimeout.get();
    }

    @Override
    public void setQueryCache(String queryCache) {
        queryCachePlugin.setString(queryCache);
    }

    @Override
    public String getQueryCache() {
        return queryCachePlugin.getString();
    }

    @Override
    public boolean getRefreshFromDataCache() {
    	return refreshFromDataCache.get();
    }

    @Override
    public void setRefreshFromDataCache(boolean flag) {
    	refreshFromDataCache.set(flag);
    }

    @Override
    public void setRefreshFromDataCache(Boolean flag) {
    	if (flag != null) {
    		refreshFromDataCache.set(flag);
    	}
    }

    @Override
    public boolean getDynamicDataStructs() {
        return dynamicDataStructs.get();
    }

    @Override
    public void setDynamicDataStructs(boolean dynamic) {
        dynamicDataStructs.set(dynamic);
    }

    @Override
    public void setDynamicDataStructs(Boolean dynamic) {
        setDynamicDataStructs(dynamic.booleanValue());
    }

    @Override
    public void setLockManager(String lockManager) {
        lockManagerPlugin.setString(lockManager);
    }

    @Override
    public String getLockManager() {
        return lockManagerPlugin.getString();
    }

    @Override
    public LockManager newLockManagerInstance() {
        // don't validate plugin properties on instantiation because it
        // is likely that back ends will override defaults with their
        // own subclasses with new properties
        return (LockManager) lockManagerPlugin.instantiate(LockManager.class,
            this, false);
    }

    @Override
    public void setInverseManager(String inverseManager) {
        inverseManagerPlugin.setString(inverseManager);
    }

    @Override
    public String getInverseManager() {
        return inverseManagerPlugin.getString();
    }

    @Override
    public InverseManager newInverseManagerInstance() {
        return (InverseManager) inverseManagerPlugin.instantiate(InverseManager.class, this);
    }

    @Override
    public void setSavepointManager(String savepointManager) {
        savepointManagerPlugin.setString(savepointManager);
    }

    @Override
    public String getSavepointManager() {
        return savepointManagerPlugin.getString();
    }

    @Override
    public SavepointManager getSavepointManagerInstance() {
        if (savepointManagerPlugin.get() == null)
            savepointManagerPlugin.instantiate(SavepointManager.class, this);
        return (SavepointManager) savepointManagerPlugin.get();
    }

    @Override
    public void setOrphanedKeyAction(String action) {
        orphanedKeyPlugin.setString(action);
    }

    @Override
    public String getOrphanedKeyAction() {
        return orphanedKeyPlugin.getString();
    }

    @Override
    public OrphanedKeyAction getOrphanedKeyActionInstance() {
        if (orphanedKeyPlugin.get() == null)
            orphanedKeyPlugin.instantiate(OrphanedKeyAction.class, this);
        return (OrphanedKeyAction) orphanedKeyPlugin.get();
    }

    @Override
    public void setOrphanedKeyAction(OrphanedKeyAction action) {
        orphanedKeyPlugin.set(action);
    }

    @Override
    public void setRemoteCommitProvider(String remoteCommitProvider) {
        remoteProviderPlugin.setString(remoteCommitProvider);
    }

    @Override
    public String getRemoteCommitProvider() {
        return remoteProviderPlugin.getString();
    }

    @Override
    public RemoteCommitProvider newRemoteCommitProviderInstance() {
        return remoteProviderPlugin.instantiateProvider(this);
    }

    @Override
    public void setRemoteCommitEventManager(
        RemoteCommitEventManager remoteEventManager) {
        this.remoteEventManager = remoteEventManager;
        remoteProviderPlugin.configureEventManager(remoteEventManager);
    }

    @Override
    public RemoteCommitEventManager getRemoteCommitEventManager() {
        if (remoteEventManager == null) {
            remoteEventManager = new RemoteCommitEventManager(this);
            remoteProviderPlugin.configureEventManager(remoteEventManager);
        }
        return remoteEventManager;
    }

    @Override
    public void setTransactionMode(String transactionMode) {
        this.transactionMode.setString(transactionMode);
    }

    @Override
    public String getTransactionMode() {
        return transactionMode.getString();
    }

    @Override
    public void setTransactionModeManaged(boolean managed) {
        transactionMode.set(managed);
    }

    @Override
    public boolean isTransactionModeManaged() {
        return transactionMode.get();
    }

    @Override
    public void setManagedRuntime(String managedRuntime) {
        managedRuntimePlugin.setString(managedRuntime);
    }

    @Override
    public String getManagedRuntime() {
        return managedRuntimePlugin.getString();
    }

    @Override
    public void setManagedRuntime(ManagedRuntime managedRuntime) {
        managedRuntimePlugin.set(managedRuntime);
    }

    @Override
    public ManagedRuntime getManagedRuntimeInstance() {
        if (managedRuntimePlugin.get() == null)
            managedRuntimePlugin.instantiate(ManagedRuntime.class, this);
        return (ManagedRuntime) managedRuntimePlugin.get();
    }

    @Override
    public void setProxyManager(String proxyManager) {
        proxyManagerPlugin.setString(proxyManager);
    }

    @Override
    public String getProxyManager() {
        return proxyManagerPlugin.getString();
    }

    @Override
    public void setProxyManager(ProxyManager proxyManager) {
        proxyManagerPlugin.set(proxyManager);
    }

    @Override
    public ProxyManager getProxyManagerInstance() {
        if (proxyManagerPlugin.get() == null)
            proxyManagerPlugin.instantiate(ProxyManager.class, this);
        return (ProxyManager) proxyManagerPlugin.get();
    }

    @Override
    public void setMapping(String mapping) {
        this.mapping.setString(mapping);
    }

    @Override
    public String getMapping() {
        return mapping.getString();
    }

    @Override
    public void setMetaDataFactory(String meta) {
        this.metaFactoryPlugin.setString(meta);
    }

    @Override
    public String getMetaDataFactory() {
        return metaFactoryPlugin.getString();
    }

    @Override
    public MetaDataFactory newMetaDataFactoryInstance() {
        return (MetaDataFactory) metaFactoryPlugin.instantiate(
            MetaDataFactory.class, this);
    }

    @Override
    public void setMetaDataRepository(String meta) {
        this.metaRepositoryPlugin.setString(meta);
    }

    @Override
    public String getMetaDataRepository() {
        return metaRepositoryPlugin.getString();
    }

    @Override
    public void setMetaDataRepository(MetaDataRepository meta) {
        metaRepository = meta;
    }

    @Override
    public MetaDataRepository getMetaDataRepositoryInstance() {
        if (metaRepository == null)
            metaRepository = newMetaDataRepositoryInstance();
        return metaRepository;
    }

    @Override
    public boolean metaDataRepositoryAvailable(){
        return metaRepository != null;
    }

    @Override
    public MetaDataRepository newMetaDataRepositoryInstance() {
        return (MetaDataRepository) metaRepositoryPlugin.instantiate(
            MetaDataRepository.class, this);
    }

    @Override
    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName.setString(connectionUserName);
    }

    @Override
    public String getConnectionUserName() {
        return connectionUserName.getString();
    }

    @Override
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword.setString(connectionPassword);
    }

    @Override
    public String getConnectionPassword() {
    	EncryptionProvider p = getEncryptionProvider();
    	if(p != null) {
    		return p.decrypt(connectionPassword.getString());
    	}
        return connectionPassword.getString();
    }

    @Override
    public void setConnectionURL(String connectionURL) {
        this.connectionURL.setString(connectionURL);
    }

    @Override
    public String getConnectionURL() {
        return connectionURL.getString();
    }

    @Override
    public void setConnectionDriverName(String driverName) {
        this.connectionDriverName.setString(driverName);
    }

    @Override
    public String getConnectionDriverName() {
        return connectionDriverName.getString();
    }

    @Override
    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties.setString(connectionProperties);
    }

    @Override
    public String getConnectionProperties() {
        return connectionProperties.getString();
    }

    @Override
    public void setConnectionFactoryProperties(
        String connectionFactoryProperties) {
        this.connectionFactoryProperties.setString(connectionFactoryProperties);
    }

    @Override
    public String getConnectionFactoryProperties() {
        return connectionFactoryProperties.getString();
    }

    @Override
    public String getConnectionFactoryMode() {
        return connectionFactoryMode.getString();
    }

    @Override
    public void setConnectionFactoryMode(String mode) {
        connectionFactoryMode.setString(mode);
    }

    @Override
    public boolean isConnectionFactoryModeManaged() {
        return connectionFactoryMode.get();
    }

    @Override
    public void setConnectionFactoryModeManaged(boolean managed) {
        connectionFactoryMode.set(managed);
    }

    @Override
    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName.setString(connectionFactoryName);
    }

    @Override
    public String getConnectionFactoryName() {
        return connectionFactoryName.getString();
    }

    @Override
    public void setConnectionFactory(Object factory) {
        connectionFactory.set(factory);
    }

    @Override
    public Object getConnectionFactory() {
        if (connectionFactory.get() == null)
            connectionFactory.set(
                lookupConnectionFactory(getConnectionFactoryName(),
                		connectionFactory.getProperty()), true);
        return connectionFactory.get();
    }

    /**
     * Lookup the connection factory at the given name.
     */
    private Object lookupConnectionFactory(String name, String userKey) {
        name = StringUtil.trimToNull(name);
        if (name == null)
            return null;
        try {
        	return Configurations.lookup(name, userKey,
        			getLog(OpenJPAConfiguration.LOG_RUNTIME));
        } catch (Exception ex) {
        	return null;
        }
    }

    @Override
    public void setConnection2UserName(String connection2UserName) {
        this.connection2UserName.setString(connection2UserName);
    }

    @Override
    public String getConnection2UserName() {
        return connection2UserName.getString();
    }

    @Override
    public void setConnection2Password(String connection2Password) {
        this.connection2Password.setString(connection2Password);
    }

    @Override
    public String getConnection2Password() {
    	EncryptionProvider p = getEncryptionProvider();
    	if(p != null){
    		return p.decrypt(connection2Password.getString());
    	}
        return connection2Password.getString();
    }

    @Override
    public void setConnection2URL(String connection2URL) {
        this.connection2URL.setString(connection2URL);
    }

    @Override
    public String getConnection2URL() {
        return connection2URL.getString();
    }

    @Override
    public void setConnection2DriverName(String driverName) {
        this.connection2DriverName.setString(driverName);
    }

    @Override
    public String getConnection2DriverName() {
        return connection2DriverName.getString();
    }

    @Override
    public void setConnection2Properties(String connection2Properties) {
        this.connection2Properties.setString(connection2Properties);
    }

    @Override
    public String getConnection2Properties() {
        return connection2Properties.getString();
    }

    @Override
    public void setConnectionFactory2Properties(
        String connectionFactory2Properties) {
        this.connectionFactory2Properties
            .setString(connectionFactory2Properties);
    }

    @Override
    public String getConnectionFactory2Properties() {
        return connectionFactory2Properties.getString();
    }

    @Override
    public void setConnectionFactory2Name(String connectionFactory2Name) {
        this.connectionFactory2Name.setString(connectionFactory2Name);
    }

    @Override
    public String getConnectionFactory2Name() {
        return connectionFactory2Name.getString();
    }

    @Override
    public void setConnectionFactory2(Object factory) {
        connectionFactory2.set(factory);
    }

    @Override
    public Object getConnectionFactory2() {
        if (connectionFactory2.get() == null)
            connectionFactory2.set(
                lookupConnectionFactory(getConnectionFactory2Name(),
                		connectionFactory2.getProperty()), false);
        return connectionFactory2.get();
    }

    @Override
    public void setOptimistic(boolean optimistic) {
        this.optimistic.set(optimistic);
    }

    @Override
    public void setOptimistic(Boolean optimistic) {
        if (optimistic != null)
            setOptimistic(optimistic.booleanValue());
    }

    @Override
    public boolean getOptimistic() {
        return optimistic.get();
    }

    @Override
    public void setAutoClear(String clear) {
        autoClear.setString(clear);
    }

    @Override
    public String getAutoClear() {
        return autoClear.getString();
    }

    @Override
    public void setAutoClear(int clear) {
        autoClear.set(clear);
    }

    @Override
    public int getAutoClearConstant() {
        return autoClear.get();
    }

    @Override
    public void setRetainState(boolean retainState) {
        this.retainState.set(retainState);
    }

    @Override
    public void setRetainState(Boolean retainState) {
        if (retainState != null)
            setRetainState(retainState.booleanValue());
    }

    @Override
    public boolean getRetainState() {
        return retainState.get();
    }

    @Override
    public void setRestoreState(String restoreState) {
        this.restoreState.setString(restoreState);
    }

    @Override
    public String getRestoreState() {
        return restoreState.getString();
    }

    @Override
    public void setRestoreState(int restoreState) {
        this.restoreState.set(restoreState);
    }

    @Override
    public int getRestoreStateConstant() {
        return restoreState.get();
    }

    @Override
    public void setAutoDetach(String autoDetach) {
        this.autoDetach.setString(autoDetach);
    }

    @Override
    public String getAutoDetach() {
        return autoDetach.getString();
    }

    @Override
    public void setAutoDetach(int autoDetachFlags) {
        autoDetach.setConstant(autoDetachFlags);
    }

    @Override
    public int getAutoDetachConstant() {
        return autoDetach.getConstant();
    }

    @Override
    public void setDetachState(String detachState) {
        detachStatePlugin.setString(detachState);
    }

    public String getDetachState() {
        return detachStatePlugin.getString();
    }

    @Override
    public void setDetachState(DetachOptions detachState) {
        detachStatePlugin.set(detachState);
    }

    @Override
    public DetachOptions getDetachStateInstance() {
        if (detachStatePlugin.get() == null)
            detachStatePlugin.instantiate(DetachOptions.class, this);
        return (DetachOptions) detachStatePlugin.get();
    }

    @Override
    public void setIgnoreChanges(boolean ignoreChanges) {
        this.ignoreChanges.set(ignoreChanges);
    }

    @Override
    public void setIgnoreChanges(Boolean ignoreChanges) {
        if (ignoreChanges != null)
            setIgnoreChanges(ignoreChanges.booleanValue());
    }

    @Override
    public boolean getIgnoreChanges() {
        return ignoreChanges.get();
    }

    @Override
    public void setNontransactionalRead(boolean nontransactionalRead) {
        this.nontransactionalRead.set(nontransactionalRead);
    }

    @Override
    public void setNontransactionalRead(Boolean nontransactionalRead) {
        if (nontransactionalRead != null)
            setNontransactionalRead(nontransactionalRead.booleanValue());
    }

    @Override
    public boolean getNontransactionalRead() {
        return nontransactionalRead.get();
    }

    @Override
    public void setNontransactionalWrite(boolean nontransactionalWrite) {
        this.nontransactionalWrite.set(nontransactionalWrite);
    }

    @Override
    public void setNontransactionalWrite(Boolean nontransactionalWrite) {
        if (nontransactionalWrite != null)
            setNontransactionalWrite(nontransactionalWrite.booleanValue());
    }

    @Override
    public boolean getNontransactionalWrite() {
        return nontransactionalWrite.get();
    }

    @Override
    public void setMultithreaded(boolean multithreaded) {
        this.multithreaded.set(multithreaded);
    }

    @Override
    public void setMultithreaded(Boolean multithreaded) {
        if (multithreaded != null)
            setMultithreaded(multithreaded.booleanValue());
    }

    @Override
    public boolean getMultithreaded() {
        return multithreaded.get();
    }

    @Override
    public void setFetchBatchSize(int fetchBatchSize) {
        this.fetchBatchSize.set(fetchBatchSize);
    }

    @Override
    public void setFetchBatchSize(Integer fetchBatchSize) {
        if (fetchBatchSize != null)
            setFetchBatchSize(fetchBatchSize.intValue());
    }

    @Override
    public int getFetchBatchSize() {
        return fetchBatchSize.get();
    }

    @Override
    public void setMaxFetchDepth(int maxFetchDepth) {
        this.maxFetchDepth.set(maxFetchDepth);
    }

    @Override
    public void setMaxFetchDepth(Integer maxFetchDepth) {
        if (maxFetchDepth != null)
            setMaxFetchDepth(maxFetchDepth.intValue());
    }

    @Override
    public int getMaxFetchDepth() {
        return maxFetchDepth.get();
    }

    @Override
    public void setFetchGroups(String fetchGroups) {
        this.fetchGroups.setString(fetchGroups);
    }

    @Override
    public String getFetchGroups() {
        return fetchGroups.getString();
    }

    @Override
    public String[] getFetchGroupsList() {
        return fetchGroups.get();
    }

    @Override
    public void setFetchGroups(String[] fetchGroups) {
        this.fetchGroups.set(fetchGroups);
    }

    @Override
    public void setFlushBeforeQueries(String flush) {
        flushBeforeQueries.setString(flush);
    }

    @Override
    public String getFlushBeforeQueries() {
        return flushBeforeQueries.getString();
    }

    @Override
    public void setFlushBeforeQueries(int flush) {
        flushBeforeQueries.set(flush);
    }

    @Override
    public int getFlushBeforeQueriesConstant() {
        return flushBeforeQueries.get();
    }

    @Override
    public void setLockTimeout(int timeout) {
        lockTimeout.set(timeout);
    }

    @Override
    public void setLockTimeout(Integer timeout) {
        if (timeout != null)
            setLockTimeout(timeout.intValue());
    }

    @Override
    public int getLockTimeout() {
        return lockTimeout.get();
    }

    @Override
    public int getQueryTimeout() {
        return queryTimeout.get();
    }

    @Override
    public void setQueryTimeout(int timeout) {
         queryTimeout.set(timeout);
    }

    @Override
    public void setReadLockLevel(String level) {
        readLockLevel.setString(level);
    }

    @Override
    public String getReadLockLevel() {
        return readLockLevel.getString();
    }

    @Override
    public void setReadLockLevel(int level) {
        readLockLevel.set(level);
    }

    @Override
    public int getReadLockLevelConstant() {
        return readLockLevel.get();
    }

    @Override
    public void setWriteLockLevel(String level) {
        writeLockLevel.setString(level);
    }

    @Override
    public String getWriteLockLevel() {
        return writeLockLevel.getString();
    }

    @Override
    public void setWriteLockLevel(int level) {
        writeLockLevel.set(level);
    }

    @Override
    public int getWriteLockLevelConstant() {
        return writeLockLevel.get();
    }

    @Override
    public void setSequence(String sequence) {
        seqPlugin.setString(sequence);
    }

    @Override
    public String getSequence() {
        return seqPlugin.getString();
    }

    @Override
    public void setSequence(Seq seq) {
        seqPlugin.set(seq);
    }

    @Override
    public Seq getSequenceInstance() {
        if (seqPlugin.get() == null)
            seqPlugin.instantiate(Seq.class, this);
        return (Seq) seqPlugin.get();
    }

    @Override
    public void setConnectionRetainMode(String connectionRetainMode) {
        this.connectionRetainMode.setString(connectionRetainMode);
    }

    @Override
    public String getConnectionRetainMode() {
        return connectionRetainMode.getString();
    }

    @Override
    public void setConnectionRetainMode(int connectionRetainMode) {
        this.connectionRetainMode.set(connectionRetainMode);
    }

    @Override
    public int getConnectionRetainModeConstant() {
        return connectionRetainMode.get();
    }

    @Override
    public void setFilterListeners(String filterListeners) {
        filterListenerPlugins.setString(filterListeners);
    }

    @Override
    public String getFilterListeners() {
        return filterListenerPlugins.getString();
    }

    @Override
    public void setFilterListeners(FilterListener[] listeners) {
        filterListenerPlugins.set(listeners);
    }

    @Override
    public FilterListener[] getFilterListenerInstances() {
        if (filterListenerPlugins.get() == null)
            filterListenerPlugins.instantiate(FilterListener.class, this);
        return (FilterListener[]) filterListenerPlugins.get();
    }

    @Override
    public void setAggregateListeners(String aggregateListeners) {
        aggregateListenerPlugins.setString(aggregateListeners);
    }

    @Override
    public String getAggregateListeners() {
        return aggregateListenerPlugins.getString();
    }

    @Override
    public void setAggregateListeners(AggregateListener[] listeners) {
        aggregateListenerPlugins.set(listeners);
    }

    @Override
    public AggregateListener[] getAggregateListenerInstances() {
        if (aggregateListenerPlugins.get() == null)
            aggregateListenerPlugins.instantiate(AggregateListener.class, this);
        return (AggregateListener[]) aggregateListenerPlugins.get();
    }

    @Override
    public void setRetryClassRegistration(boolean retry) {
        retryClassRegistration.set(retry);
    }

    @Override
    public void setRetryClassRegistration(Boolean retry) {
        if (retry != null)
            setRetryClassRegistration(retry.booleanValue());
    }

    @Override
    public boolean getRetryClassRegistration() {
        return retryClassRegistration.get();
    }

    @Override
    public String getCompatibility() {
        return compatibilityPlugin.getString();
    }

    @Override
    public void setCompatibility(String compatibility) {
        compatibilityPlugin.setString(compatibility);
    }

    /**
     * If a Compatibility instance is associated with the Specification,
     * we will configure this Compatibility instance instead of instantiating a
     * new one so that the compatibility flags set in compliance with the
     * Specification can be preserved.
     */
    @Override
    public Compatibility getCompatibilityInstance() {
        if (compatibilityPlugin.get() == null) {
            Specification spec = getSpecificationInstance();
            Compatibility comp = spec != null ? spec.getCompatibility() : null;
            if (comp == null)
                compatibilityPlugin.instantiate(Compatibility.class, this);
            else
                compatibilityPlugin.configure(comp, this);
        }
        return (Compatibility) compatibilityPlugin.get();
    }

    @Override
    public String getCallbackOptions() {
        return callbackPlugin.getString();
    }

    @Override
    public void setCallbackOptions(String options) {
        callbackPlugin.setString(options);
    }

    @Override
    public CallbackOptions getCallbackOptionsInstance() {
        if (callbackPlugin.get() == null)
            callbackPlugin.instantiate(CallbackOptions.class, this);
        return (CallbackOptions) callbackPlugin.get();
    }

    @Override
    public String getQueryCompilationCache() {
        return queryCompilationCachePlugin.getString();
    }

    @Override
    public void setQueryCompilationCache(String queryCompilationCache) {
        queryCompilationCachePlugin.setString(queryCompilationCache);
    }

    @Override
    public Map getQueryCompilationCacheInstance() {
        if (queryCompilationCachePlugin.get() == null)
            queryCompilationCachePlugin.instantiate(Map.class, this);
        return (Map) queryCompilationCachePlugin.get();
    }

    @Override
    public StoreFacadeTypeRegistry getStoreFacadeTypeRegistry() {
        return _storeFacadeRegistry;
    }

    @Override
    public BrokerFactoryEventManager getBrokerFactoryEventManager() {
        return _brokerFactoryEventManager;
    }

    @Override
    public String getRuntimeUnenhancedClasses() {
        return runtimeUnenhancedClasses.getString();
    }

    @Override
    public int getRuntimeUnenhancedClassesConstant() {
        return runtimeUnenhancedClasses.get();
    }

    @Override
    public void setRuntimeUnenhancedClasses(int mode) {
        runtimeUnenhancedClasses.set(mode);
    }

    @Override
    public void setRuntimeUnenhancedClasses(String mode) {
        runtimeUnenhancedClasses.setString(mode);
    }

    @Override
    public String getCacheMarshallers() {
        return cacheMarshallerPlugins.getString();
    }

    @Override
    public void setCacheMarshallers(String marshallers) {
        cacheMarshallerPlugins.setString(marshallers);
    }

    @Override
    public Map getCacheMarshallerInstances() {
        return cacheMarshallerPlugins.getInstancesAsMap();
    }

    @Override
    public boolean isInitializeEagerly() {
        return eagerInitialization.get();
    }

    @Override
    public void setInitializeEagerly(boolean retry) {
        eagerInitialization.set(retry);
    }

    @Override
    public void setValidationMode(String mode) {
        validationMode.setString(mode);
    }

    @Override
    public String getValidationMode() {
        String mode = validationMode.getString();
        if (mode == null)
            mode = validationMode.getDefault();
        return mode;
    }

    @Override
    public void setValidationGroupPrePersist(String vgPrePersist) {
        validationGroupPrePersist.setString(vgPrePersist);
    }

    @Override
    public String getValidationGroupPrePersist() {
        String vgPrePersist = validationGroupPrePersist.getString();
        if (vgPrePersist == null)
            vgPrePersist = validationGroupPrePersist.getDefault();
        return vgPrePersist;
    }

    @Override
    public void setValidationGroupPreUpdate(String vgPreUpdate) {
        validationGroupPreUpdate.setString(vgPreUpdate);
    }

    @Override
    public String getValidationGroupPreUpdate() {
        String vgPreUpdate = validationGroupPreUpdate.getString();
        if (vgPreUpdate == null)
            vgPreUpdate = validationGroupPreUpdate.getDefault();
        return vgPreUpdate;
    }

    @Override
    public void setValidationGroupPreRemove(String vgPreRemove) {
        validationGroupPreRemove.setString(vgPreRemove);
    }

    @Override
    public String getValidationGroupPreRemove() {
        String vgPreRemove = validationGroupPreRemove.getString();
        if (vgPreRemove == null)
            vgPreRemove = validationGroupPreRemove.getDefault();
        return vgPreRemove;
    }

    @Override
    public String getInstrumentation() {
        return instrumentationProviders.getString();
    }

    @Override
    public void setInstrumentation(String providers) {
        instrumentationProviders.setString(providers);
    }

    public InstrumentationProvider[] getInstrumentationInstances() {
        if (instrumentationProviders.get() == null)
            instrumentationProviders.instantiate(InstrumentationProvider.class, this);
        return (InstrumentationProvider[]) instrumentationProviders.get();
    }

    public void setInstrumentationManager(String mgr) {
        instrumentationManager.setString(mgr);
    }

    public String getInstrumentationManager() {
        return instrumentationManager.getString();
    }

    public void setInstrumentationManager(InstrumentationManager im) {
        if (im != null)
            im.initialize(this, instrumentationProviders);
        instrumentationManager.set(im);
    }

    @Override
    public InstrumentationManager getInstrumentationManagerInstance() {
        InstrumentationManager im = (InstrumentationManager) instrumentationManager.get();
        if (im == null) {
            im = (InstrumentationManager) instrumentationManager.instantiate(InstrumentationManager.class, this);
            if (im != null) {
                im.initialize(this, instrumentationProviders);
                im.start(InstrumentationLevel.IMMEDIATE, this);
            }
        }
        return im;
    }

    @Override
    public void instantiateAll() {
        super.instantiateAll();
        getMetaDataRepositoryInstance();
        getRemoteCommitEventManager();
        getAuditorInstance();
        cacheMarshallerPlugins.initialize();
        if (isInitializeEagerly()) {
            getConnectionFactory();
            getConnectionFactory2();
        }
    }

    @Override
    protected void preClose() {
        ImplHelper.close(metaRepository);
        ImplHelper.close(remoteEventManager);
        ImplHelper.close(getInstrumentationManagerInstance());
        super.preClose();
    }

    @Override
    public Log getConfigurationLog() {
        return getLog(LOG_RUNTIME);
    }

    @Override
    public void setQuerySQLCache(String querySQLCache) {
        preparedQueryCachePlugin.setString(querySQLCache);
    }

    @Override
    public void setQuerySQLCache(PreparedQueryCache querySQLCache) {
        preparedQueryCachePlugin.set(querySQLCache);
    }

    @Override
    public String getQuerySQLCache() {
        return preparedQueryCachePlugin.getString();
    }

    @Override
    public PreparedQueryCache getQuerySQLCacheInstance() {
        if (preparedQueryCachePlugin == null)
            return null;

        if (preparedQueryCachePlugin.get() == null) {
            preparedQueryCachePlugin.instantiate(PreparedQueryCache.class,
                    this);
        }
        return (PreparedQueryCache)preparedQueryCachePlugin.get();
    }

    @Override
    public void setFinderCache(String finderCache) {
        finderCachePlugin.setString(finderCache);
    }

    @Override
    public String getFinderCache() {
        return finderCachePlugin.getString();
    }

    @Override
    public FinderCache getFinderCacheInstance() {
        if (finderCachePlugin == null) { // xmlstore case
            return null;
        }
        if (finderCachePlugin.get() == null) {
            finderCachePlugin.instantiate(FinderCache.class, this);
        }
        return (FinderCache)finderCachePlugin.get();
    }

    @Override
    public Object getValidationFactoryInstance() {
        return validationFactory.get();
    }

    @Override
    public void setValidationFactory(Object factory) {
        validationFactory.set(factory);
    }

    @Override
    public Object getValidatorInstance() {
        return validator.get();
    }

    @Override
    public void setValidatorInstance(Object val) {
        validator.set(val);
    }

    @Override
    public String getLifecycleEventManager() {
        return lifecycleEventManager.getString();
    }

    @Override
    public LifecycleEventManager getLifecycleEventManagerInstance() {
        LifecycleEventManager lem = null;
        if (!getCompatibilityInstance().isSingletonLifecycleEventManager() ||
                (lem = (LifecycleEventManager)lifecycleEventManager.get()) == null) {
            lem = (LifecycleEventManager)lifecycleEventManager
                .instantiate(LifecycleEventManager.class, this);
        }
        return lem;
    }

    @Override
    public void setLifecycleEventManager(String lem) {
        if (_allowSetLifeCycleEventManager) {
            _allowSetLifeCycleEventManager = false;
            // Only allow this to be called once even if the configuration is frozen. This can happen if a configuration
            // is eagerly initialized and validation is being used.
            lifecycleEventManager.setDynamic(true);
            lifecycleEventManager.setString(lem);
            lifecycleEventManager.setDynamic(false);
        } else {
            // If the configuration is frozen this will result in a warning message and/or an exception.
            lifecycleEventManager.setString(lem);
        }
    }

    @Override
    public boolean getDynamicEnhancementAgent() {
        return dynamicEnhancementAgent.get();
    }

    @Override
    public void setDynamicEnhancementAgent(boolean dynamic) {
        dynamicEnhancementAgent.set(dynamic);
    }

    @Override
    public void setEncryptionProvider(String p) {
        encryptionProvider.setString(p);
    }

    @Override
    public EncryptionProvider getEncryptionProvider() {
        if (encryptionProvider.get() == null)
            encryptionProvider.instantiate(EncryptionProvider.class, this);
        return (EncryptionProvider) encryptionProvider.get();
    }

    @Override
    public void setDataCacheMode(String mode) {
        this.dataCacheMode.setString(mode);
    }

    @Override
    public String getDataCacheMode() {
        return dataCacheMode.getString();
    }


    @Override
    public String getCacheDistributionPolicy() {
        return cacheDistributionPolicyPlugin.getString();
    }

    @Override
    public CacheDistributionPolicy getCacheDistributionPolicyInstance() {
        CacheDistributionPolicy policy = (CacheDistributionPolicy) cacheDistributionPolicyPlugin.get();
        if (policy == null) {
            policy =  (CacheDistributionPolicy)
                cacheDistributionPolicyPlugin.instantiate(CacheDistributionPolicy.class, this);
        }
        return policy;
    }

    @Override
    public void setCacheDistributionPolicy(String policyPlugin) {
        cacheDistributionPolicyPlugin.setString(policyPlugin);
    }

    @Override
    public void setCacheDistributionPolicyInstance(CacheDistributionPolicy policy) {
        cacheDistributionPolicyPlugin.set(policy);
    }

    public void setPersistenceEnvironment(Map<String, Object> peMap) {
        this._peMap = peMap;
    }

    public Map<String, Object> getPersistenceEnvironment() {
        return _peMap;
    }

    @Override
    public Auditor getAuditorInstance() {
    	Auditor auditor = (Auditor) auditorPlugin.get();
        if (auditor == null) {
            auditor = (Auditor) auditorPlugin.instantiate(Auditor.class, this);
       }
       return auditor;
    }

    @Override
    public void setAuditorInstance(Auditor auditor) {
    	auditorPlugin.set(auditor);
    }

    @Override
    public String getAuditor() {
    	return auditorPlugin.getString();
    }

    @Override
    public void setAuditor(String auditor) {
    	auditorPlugin.setString(auditor);
    }

    @Override
    public boolean getPostLoadOnMerge() {
        return postLoadOnMerge.get();
    }

    @Override
    public void setPostLoadOnMerge(boolean postLoadOnMerge) {
        this.postLoadOnMerge.set(postLoadOnMerge);
    }

    @Override
    public void setPostLoadOnMerge(Boolean postLoadOnMerge) {
        if (postLoadOnMerge != null)
            setPostLoadOnMerge(postLoadOnMerge.booleanValue());
    }

    @Override
    public boolean getOptimizeIdCopy() {
        return optimizeIdCopy.get();
    }

    @Override
    public void setOptimizeIdCopy(boolean optimizeId) {
        optimizeIdCopy.set(optimizeId);
    }

    @Override
    public void setOptimizeIdCopy(Boolean optimizeId) {
        if (optimizeId != null) {
            setOptimizeIdCopy(optimizeId.booleanValue());
        }
    }

    @Override
    public String getDatabaseAction() {
        return databaseAction.getString();
    }

    @Override
    public int getDatabaseActionConstant() {
        return databaseAction.get();
    }

    @Override
    public String getScriptsAction() {
        return scriptsAction.getString();
    }

    @Override
    public int getScriptsActionConstant() {
        return scriptsAction.get();
    }

    @Override
    public String getCreateSource() {
        return createSource.getString();
    }

    @Override
    public int getCreateSourceConstant() {
        return createSource.get();
    }

    @Override
    public String getDropSource() {
        return dropSource.getString();
    }

    @Override
    public int getDropSourceConstant() {
        return dropSource.get();
    }

    @Override
    public String getCreateScriptSource() {
        return createScriptSource.getString();
    }

    @Override
    public String getDropScriptSource() {
        return dropScriptSource.getString();
    }

    @Override
    public String getCreateScriptTarget() {
        return createScriptTarget.getString();
    }

    @Override
    public String getDropScriptTarget() {
        return dropScriptTarget.getString();
    }

    @Override
    public String getLoadScriptSource() {
        return loadScriptSource.getString();
    }

    @Override
    public boolean getUseTCCLinSelectNew() {
        return useTcclForSelectNew.get();
    }

    @Override
    public void setUseTCCLinSelectNew(boolean useTcclForSelectNew) {
        this.useTcclForSelectNew.set(useTcclForSelectNew);
    }

    @Override
    public void setUseTCCLinSelectNew(Boolean useTcclForSelectNew) {
        if (useTcclForSelectNew != null) {
            setUseTCCLinSelectNew(useTcclForSelectNew.booleanValue());
        }
    }

    @Override
    public Collection<Class<?>> getTypesWithoutEnhancement() {
        return asList(typesWithoutEnhancement.get());
    }

    @Override
    public void setTypesWithoutEnhancement(Collection<Class<?>> value) {
        typesWithoutEnhancement.set(value.toArray(new Class[value.size()]));
    }
}


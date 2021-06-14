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
import java.util.Map;

import org.apache.openjpa.audit.Auditor;
import org.apache.openjpa.datacache.CacheDistributionPolicy;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCacheMode;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.enhance.RuntimeUnenhancedClassesModes;
import org.apache.openjpa.event.BrokerFactoryEventManager;
import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.instrumentation.InstrumentationManager;
import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FinderCache;
import org.apache.openjpa.kernel.InverseManager;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.RestoreState;
import org.apache.openjpa.kernel.SavepointManager;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.encryption.EncryptionProvider;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;
import org.apache.openjpa.util.ProxyManager;
import org.apache.openjpa.util.StoreFacadeTypeRegistry;

/**
 * Defines the properties necessary to configure runtime properties and
 * connect to a data source. There is a 1-1 relation between a configuration
 * and a {@link BrokerFactory}.
 *  All setter methods that take primitive parameters also have wrapper
 * setter with the appropriate primitive wrapper. This is so the interface
 * can be in accordance with the J2EE Connector Architecture.
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @see Configuration
 */
public interface OpenJPAConfiguration
        extends Configuration {

    /**
     * Name of logger for metadata-related messages:
     * <code>openjpa.MetaData</code>.
     */
    String LOG_METADATA = "openjpa.MetaData";

    /**
     * Name of logger for enhancement-related messages:
     * <code>openjpa.Enhance</code>.
     */
    String LOG_ENHANCE = "openjpa.Enhance";

    /**
     * Name of logger for messages from the runtime system:
     * <code>openjpa.Runtime</code>.
     */
    String LOG_RUNTIME = "openjpa.Runtime";

    /**
     * Name of logger for query logging:
     * <code>openjpa.Query</code>.
     */
    String LOG_QUERY = "openjpa.Query";

    /**
     * Name of logger for messages from the data cache:
     * <code>openjpa.DataCache</code>.
     */
    String LOG_DATACACHE = "openjpa.DataCache";

    /**
     * Name of logger for messages from the development tools:
     * <code>openjpa.Tool</code>.
     */
    String LOG_TOOL = "openjpa.Tool";

    /**
     * Option for runtimes that support nontransactional reads.
     */
    String OPTION_NONTRANS_READ =
            "openjpa.option.NontransactionalRead";

    /**
     * Option for runtimes that support optimistic transactions.
     */
    String OPTION_OPTIMISTIC =
            "openjpa.option.Optimistic";

    /**
     * Option for runtimes that support application identity.
     */
    String OPTION_ID_APPLICATION =
            "openjpa.option.ApplicationIdentity";

    /**
     * Option for runtimes that support application identity.
     */
    String OPTION_ID_DATASTORE =
            "openjpa.option.DatastoreIdentity";

    /**
     * Option for SQL support.
     */
    String OPTION_QUERY_SQL = "openjpa.option.SQL";

    /**
     * Option for runtimes that support persistent collection fields.
     */
    String OPTION_TYPE_COLLECTION =
            "openjpa.option.Collection";

    /**
     * Option for runtimes that support persistent map fields.
     */
    String OPTION_TYPE_MAP = "openjpa.option.Map";

    /**
     * Option for runtimes that support persistent array fields.
     */
    String OPTION_TYPE_ARRAY = "openjpa.option.Array";

    /**
     * Option for runtime that can differentiate between null and empty
     * container fields.
     */
    String OPTION_NULL_CONTAINER =
            "openjpa.option.NullContainer";

    /**
     * Option for runtimes that support embedded relations to other
     * persistence capable objects.
     */
    String OPTION_EMBEDDED_RELATION =
            "openjpa.option.EmbeddedRelation";

    /**
     * Option for runtimes that support collections of embedded
     * relations to other persistence capable objects.
     */
    String OPTION_EMBEDDED_COLLECTION_RELATION =
            "openjpa.option.EmbeddedCollectionRelation";

    /**
     * Option for runtimes that support maps of embedded
     * relations to other persistence capable objects.
     */
    String OPTION_EMBEDDED_MAP_RELATION =
            "openjpa.option.EmbeddedMapRelation";

    /**
     * Option for runtimes that support incremental flushing.
     */
    String OPTION_INC_FLUSH =
            "openjpa.option.IncrementalFlush";

    /**
     * Option for runtimes that the autoassign value strategy.
     */
    String OPTION_VALUE_AUTOASSIGN =
            "openjpa.option.AutoassignValue";

    /**
     * Option for runtimes that the increment value strategy.
     */
    String OPTION_VALUE_INCREMENT =
            "openjpa.option.IncrementValue";

    /**
     * Option for runtimes that support returning the datastore connection.
     */
    String OPTION_DATASTORE_CONNECTION =
            "openjpa.option.DataStoreConnection";

    /**
     * Option for runtimes that support returning the datastore connection
     * that is a JDBC Connection.
     */
    String OPTION_JDBC_CONNECTION =
            "openjpa.option.JDBCConnection";

    /**
     * Option for enable fire &#064;PostLoad events on merge operations
     */
    String OPTION_POSTLOAD_ON_MERGE =
            "openjpa.option.PostLoadOnMerge";

    /**
     * Option to enable TCCL if the result class for select new is not found
     * in the entities classloader
     */
    String OPTION_USE_TCCL_IN_SELECT_NEW =
            "openjpa.option.UseTCCLinSelectNew";

    /**
     * Return the set of option strings supported by this runtime. This set
     * is mutable.
     */
    Collection<String> supportedOptions();

    /**
     * Get a name of the Specification. Specification determines various
     * important default behaviors.
     */
    String getSpecification();

    /**
     * Get the Specification. Specification determines various important default
     * behaviors.
     *
     * @since 2.0.0
     */
    Specification getSpecificationInstance();

    /**
     * Set the Specification for this configuration.
     * Specification determines various default properties and behavior.
     * For example, {@link Compatibility compatibility} options during runtime.
     *
     * This change will trigger all registered Product Derivations to mutate
     * other configuration properties.
     *
     * @param spec fullname of the specification that possibly encodes major and
     * minor version information. For encoding format
     * @see Specification
     *
     * @since 1.1.0
     */
    void setSpecification(String spec);

    /**
     * Set the Specification for this configuration.
     * Specification determines various default properties and behavior.
     * For example, {@link Compatibility compatibility} options during runtime.
     *
     * This change will trigger all registered Product Derivations to mutate
     * other configuration properties.
     *
     * @param spec fullname of the specification that possibly encodes major and
     * minor version information. For encoding format
     * @see Specification
     *
     * @since 2.0.0
     */
    void setSpecification(Specification spec);

    /**
     * The plugin string for the {@link ClassResolver} to use for custom
     * class loading.
     */
    String getClassResolver();

    /**
     * The plugin string for the {@link ClassResolver} to use for custom
     * class loading.
     */
    void setClassResolver(String classResolver);

    /**
     * The {@link ClassResolver} to use.
     */
    ClassResolver getClassResolverInstance();

    /**
     * The {@link ClassResolver} to use.
     */
    void setClassResolver(ClassResolver classResolver);

    /**
     * The {@link BrokerFactory} class to use.
     */
    String getBrokerFactory();

    /**
     * The {@link BrokerFactory} class to use.
     */
    void setBrokerFactory(String factory);

    /**
     * The plugin string of the {@link BrokerImpl} extension to create.
     */
    String getBrokerImpl();

    /**
     * The plugin string of the {@link BrokerImpl} extension to create.
     */
    void setBrokerImpl(String broker);

    /**
     * Create a new broker instance with the configured plugin data.
     */
    BrokerImpl newBrokerInstance(String user, String pass);

    /**
     * The {@link DataCache} to use for level-2 data store caching.
     */
    String getDataCache();

    /**
     * The {@link DataCache} to use for level-2 data store caching.
     */
    void setDataCache(String dataCache);

    /**
     * The data cache manager manages this configuration's cache instances.
     */
    String getDataCacheManager();

    /**
     * The data cache manager manages this configuration's cache instances.
     */
    void setDataCacheManager(String mgr);

    /**
     * The data cache manager manages this configuration's cache instances.
     * The cache manager is created if it has not been set. Once the cache
     * manager has been set/created, all changes to caching configuration
     * must proceed through the cache manager.
     *
     * @since 0.3.0
     */
    DataCacheManager getDataCacheManagerInstance();

    /**
     * The data cache manager manages this configuration's cache instances.
     *
     * @since 0.3.0
     */
    void setDataCacheManager(DataCacheManager manager);

    /**
     * Default data cache timeout.
     *
     * @since 0.2.5
     */
    int getDataCacheTimeout();

    /**
     * Default data cache timeout.
     *
     * @since 0.2.5
     */
    void setDataCacheTimeout(int timeout);

    /**
     * Wrapper for JCA usage of {@link #setDataCacheTimeout(int)}.
     *
     * @since 0.2.5
     */
    void setDataCacheTimeout(Integer timeout);

    /**
     * Gets whether entity state is to be refreshed from {@link DataCache}.
     * The entities are never refreshed from DataCache if lock is being applied
     * (e.g. in a pessimistic transaction) and hence this setting only refers
     * to behavior when not locking.
     * This flag can be used to overwrite RetrieveMode.BYPASS.
     * By default, however, this falg is false.
     *
     * @since 1.2.0
     */
    boolean getRefreshFromDataCache();

    /**
     * Sets whether entity state is to be refreshed from {@link DataCache}.
     * The entities are never refreshed from DataCache if lock is being applied
     * (e.g. in a pessimistic transaction) and hence this setting only refers
     * to behavior when not locking.
     *
     * @since 1.2.0
     */
    void setRefreshFromDataCache(boolean refreshFromDataCache);

    /**
     * Sets whether entity state is to be refreshed from {@link DataCache}.
     * The entities are never refreshed from DataCache if lock is being applied
     * (e.g. in a pessimistic transaction) and hence this setting only refers
     * to behavior when not locking.
     *
     * @since 1.2.0
     */
    void setRefreshFromDataCache(Boolean refreshFromDataCache);

    /**
     * The plugin to use for level-2 data store query caching.
     *
     * @since 0.2.5
     */
    String getQueryCache();

    /**
     * The plugin to use for level-2 data store query caching.
     *
     * @since 0.2.5
     */
    void setQueryCache(String queryCache);

    /**
     * Return whether to generate dynamic data structures
     * where possible for cache and runtime usage.
     *
     * @since 0.3.3
     */
    boolean getDynamicDataStructs();

    /**
     * Set whether to generate dynamic data structures
     * where possible for cache and runtime usage.
     *
     * @since 0.3.3
     */
    void setDynamicDataStructs(boolean dynamic);

    /**
     * Wrapper for JCA usage of {@link #setDynamicDataStructs(boolean)}.
     */
    void setDynamicDataStructs(Boolean dynamic);

    /**
     * The plugin to use for datastore lock management.
     *
     * @since 0.3.1
     */
    String getLockManager();

    /**
     * The plugin to use for datastore lock management.
     *
     * @since 0.3.1
     */
    void setLockManager(String lockManager);

    /**
     * Return a new lock manager instance using the configured plugin settings.
     */
    LockManager newLockManagerInstance();

    /**
     * The plugin to use for managing inverse relations.
     *
     * @since 0.3.2
     */
    String getInverseManager();

    /**
     * The plugin to use for managing inverse relations.
     *
     * @since 0.3.2
     */
    void setInverseManager(String inverse);

    /**
     * Return a new inverse manager instance using the configured plugin
     * settings.
     *
     * @since 0.3.2
     */
    InverseManager newInverseManagerInstance();

    /**
     * The plugin to use for savepoint management.
     *
     * @since 0.3.4
     */
    String getSavepointManager();

    /**
     * The plugin to use for savepoint management.
     *
     * @since 0.3.4
     */
    void setSavepointManager(String savepointManager);

    /**
     * Return the configured savepoint manager instance.
     */
    SavepointManager getSavepointManagerInstance();

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    String getOrphanedKeyAction();

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    void setOrphanedKeyAction(String action);

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    OrphanedKeyAction getOrphanedKeyActionInstance();

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    void setOrphanedKeyAction(OrphanedKeyAction action);

    /**
     * The plugin to use for remote commit notification.
     *
     * @since 0.2.5
     */
    String getRemoteCommitProvider();

    /**
     * The plugin to use for remote commit notification.
     *
     * @since 0.2.5
     */
    void setRemoteCommitProvider(String remoteCommitProvider);

    /**
     * Create a remote commit provider from the configured plugin.
     *
     * @since 0.3.0
     */
    RemoteCommitProvider newRemoteCommitProviderInstance();

    /**
     * The remote event manager that manages this configuration's remote
     * event listeners.
     *
     * @since 0.3.0
     */
    RemoteCommitEventManager getRemoteCommitEventManager();

    /**
     * The remote event manager that manages this configuration's remote
     * event listeners.
     *
     * @since 0.3.0
     */
    void setRemoteCommitEventManager(RemoteCommitEventManager manager);

    /**
     * Specifies the behavior of the transaction model. Possible values are:
     * <ul>
     * <li><code>local</code>: Perform transaction operations locally.</li>
     * <li><code>managed</code>: Use managed environment's global
     * transactions.</li>
     * </ul>
     *
     * @since 0.2.5
     */
    String getTransactionMode();

    /**
     * Specifies the behavior of the transaction model. Possible values are:
     * <ul>
     * <li><code>local</code>: Perform transaction operations locally.</li>
     * <li><code>managed</code>: Use managed environment's global
     * transactions.</li>
     * </ul>
     *
     * @since 0.2.5
     */
    void setTransactionMode(String mode);

    /**
     * Return whether managed transactions are being used.
     */
    boolean isTransactionModeManaged();

    /**
     * Set whether managed transactions are being used.
     */
    void setTransactionModeManaged(boolean managed);

    /**
     * The plugin string for the {@link ManagedRuntime} to use for managed
     * environments.
     */
    String getManagedRuntime();

    /**
     * The plugin string for the {@link ManagedRuntime} to use for managed
     * environments.
     */
    void setManagedRuntime(String managedRuntime);

    /**
     * The plugin to use for integrating with a managed runtime.
     */
    ManagedRuntime getManagedRuntimeInstance();

    /**
     * The plugin to use for integrating with a managed runtime.
     */
    void setManagedRuntime(ManagedRuntime runtime);

    /**
     * The plugin string for the {@link ProxyManager} to use for second
     * class object proxies.
     */
    String getProxyManager();

    /**
     * The plugin string for the {@link ProxyManager} to use for second
     * class object proxies.
     */
    void setProxyManager(String proxyManager);

    /**
     * The {@link ProxyManager} to use.
     */
    ProxyManager getProxyManagerInstance();

    /**
     * The {@link ProxyManager} to use.
     */
    void setProxyManager(ProxyManager manager);

    /**
     * The name mapping to use for this data store.
     */
    String getMapping();

    /**
     * The name mapping to use for this data store.
     */
    void setMapping(String mapping);

    /**
     * A plugin string describing the {@link MetaDataFactory} to use.
     */
    String getMetaDataFactory();

    /**
     * A plugin string describing the {@link MetaDataFactory} to use.
     */
    void setMetaDataFactory(String meta);

    /**
     * Create a new {@link MetaDataFactory} to use with a repository.
     */
    MetaDataFactory newMetaDataFactoryInstance();

    /**
     * A plugin string describing the {@link MetaDataRepository} to use.
     */
    String getMetaDataRepository();

    /**
     * A plugin string describing the {@link MetaDataRepository} to use.
     */
    void setMetaDataRepository(String meta);

    /**
     * The metadata repository of managed class information. If no
     * repository has been set, creates one.
     *
     * @since 0.3.0
     */
    MetaDataRepository getMetaDataRepositoryInstance();

    /**
     * Returns true if a metaDataRepository has been created for this
     * configuration.
     *
     * @since 1.1.0 1.0.1
     */
    boolean metaDataRepositoryAvailable();

    /**
     * Create a new empty metadata repository of the configured type.
     */
    MetaDataRepository newMetaDataRepositoryInstance();

    /**
     * The metadata repository of managed class information.
     *
     * @since 0.3.0
     */
    void setMetaDataRepository(MetaDataRepository mdRepos);

    /**
     * The user name for the data store connection.
     */
    String getConnectionUserName();

    /**
     * The user name for the data store connection.
     */
    void setConnectionUserName(String connectionUserName);

    /**
     * The password for the data store connection.
     */
    String getConnectionPassword();

    /**
     * The password for the data store connection.
     */
    void setConnectionPassword(String connectionPassword);

    /**
     * The URL for the data store connection.
     */
    String getConnectionURL();

    /**
     * The URL for the data store connection.
     */
    void setConnectionURL(String connectionURL);

    /**
     * Class name of the connection driver.
     */
    String getConnectionDriverName();

    /**
     * Class name of the connection driver.
     */
    void setConnectionDriverName(String driverName);

    /**
     * The name for the data store connection factory.
     */
    String getConnectionFactoryName();

    /**
     * The name for the data store connection factory.
     */
    void setConnectionFactoryName(String cfName);

    /**
     * The connection factory, possibly from JNDI.
     */
    Object getConnectionFactory();

    /**
     * The connection factory.
     */
    void setConnectionFactory(Object factory);

    /**
     * These properties provide any additional information needed to
     * establish connections.
     */
    String getConnectionProperties();

    /**
     * These properties provide any additional information needed to
     * establish connections.
     */
    void setConnectionProperties(String props);

    /**
     * Configuration properties for the connection factory.
     */
    String getConnectionFactoryProperties();

    /**
     * Configuration properties for the connection factory.
     */
    void setConnectionFactoryProperties(String props);

    /**
     * The mode of the connection factory in use. Available options are:
     * <ul>
     * <li>local: OpenJPA controls the connections.</li>
     * <li>managed: Connections are automatically enlisted in
     * the current global transaction by an application server.</li>
     * </ul> Defaults to local.
     */
    String getConnectionFactoryMode();

    /**
     * The mode of the connection factory in use. Available options are:
     * <ul>
     * <li>local: OpenJPA controls the connections.</li>
     * <li>managed: Connections are automatically enlisted in
     * the current global transaction by an application server.</li>
     * </ul> Defaults to local.
     */
    void setConnectionFactoryMode(String mode);

    /**
     * Whether connections are automatically enlisted in global transactions.
     */
    boolean isConnectionFactoryModeManaged();

    /**
     * Whether connections are automatically enlisted in global transactions.
     */
    void setConnectionFactoryModeManaged(boolean managed);

    /**
     * The user name for the non-XA data store connection.
     */
    String getConnection2UserName();

    /**
     * The user name for the non-XA data store connection.
     */
    void setConnection2UserName(String connectionUserName);

    /**
     * The password for the non-XA data store connection.
     */
    String getConnection2Password();

    /**
     * The password for the non-XA data store connection.
     */
    void setConnection2Password(String connectionPassword);

    /**
     * The URL for the non-XA data store connection.
     */
    String getConnection2URL();

    /**
     * The URL for the non-XA data store connection.
     */
    void setConnection2URL(String connectionURL);

    /**
     * Class name of the non-XA connection driver.
     */
    String getConnection2DriverName();

    /**
     * Class name of the non-XA connection driver.
     */
    void setConnection2DriverName(String driverName);

    /**
     * The name for the second data store connection factory.
     */
    String getConnectionFactory2Name();

    /**
     * The name for the second data store connection factory.
     */
    void setConnectionFactory2Name(String cf2Name);

    /**
     * The non-XA connection factory.
     */
    Object getConnectionFactory2();

    /**
     * The non-XA connection factory.
     */
    void setConnectionFactory2(Object factory);

    /**
     * These properties provide any additional information needed to
     * establish non-XA connections.
     *
     * @since 0.3.0
     */
    String getConnection2Properties();

    /**
     * These properties provide any additional information needed to
     * establish non-XA connections.
     *
     * @since 0.3.0
     */
    void setConnection2Properties(String props);

    /**
     * Configuration properties for the non-XA connection factory.
     *
     * @since 0.2.5
     */
    String getConnectionFactory2Properties();

    /**
     * Configuration properties for the non-XA connection factory.
     *
     * @since 0.2.5
     */
    void setConnectionFactory2Properties(String props);

    /**
     * Whether to use optimistic transactions by default.
     */
    boolean getOptimistic();

    /**
     * Whether to use optimistic transactions by default.
     */
    void setOptimistic(boolean optimistic);

    /**
     * Wrapper for JCA usage of {@link #setOptimistic(boolean)}.
     */
    void setOptimistic(Boolean optimistic);

    /**
     * Whether to retain state after a transaction by default.
     */
    boolean getRetainState();

    /**
     * Whether to retain state after a transaction by default.
     */
    void setRetainState(boolean retainState);

    /**
     * Wrapper for JCA usage of {@link #setRetainState(boolean)}.
     */
    void setRetainState(Boolean retainState);

    /**
     * Whether instances clear their state when entering a transaction.
     */
    String getAutoClear();

    /**
     * Whether instances clear their state when entering a transaction.
     */
    void setAutoClear(String clear);

    /**
     * Return the {@link AutoClear} constant.
     */
    int getAutoClearConstant();

    /**
     * Whether instances clear their state when entering a transaction.
     */
    void setAutoClear(int clear);

    /**
     * Whether to restore initial state on rollback by default.
     */
    String getRestoreState();

    /**
     * Whether to restore initial state on rollback by default.
     */
    void setRestoreState(String restoreState);

    /**
     * Return the {@link RestoreState} constant.
     */
    int getRestoreStateConstant();

    /**
     * Whether to restore initial state on rollback by default.
     */
    void setRestoreState(int restoreState);

    /**
     * Whether changes in the current transaction are taken into account when
     * executing queries and iterating extents.
     */
    boolean getIgnoreChanges();

    /**
     * Whether changes in the current transaction are taken into account when
     * executing queries and iterating extents.
     */
    void setIgnoreChanges(boolean ignoreChanges);

    /**
     * Wrapper for JCA usage of {@link #setIgnoreChanges(boolean)}.
     */
    void setIgnoreChanges(Boolean ignoreChanges);

    /**
     * A comma-separated list of events which trigger auto-detachment
     * in place of managed states. Possible values are:
     * <ul>
     * <li><code>commit</code>: When the current transaction commits.</li>
     * <li><code>close</code>: When the broker closes.</li>
     * <li><code>nontx-read</code>: When instances are read
     * non-transactionally.</li>
     * </ul>
     */
    String getAutoDetach();

    /**
     * A comma-separated list of events which trigger auto-detachment
     * in place of managed states. Possible values are:
     * <ul>
     * <li><code>commit</code>: When the current transaction commits.</li>
     * <li><code>close</code>: When the broker closes.</li>
     * <li><code>nontx-read</code>: When instances are read
     * non-transactionally.</li>
     * </ul>
     */
    void setAutoDetach(String detach);

    /**
     * The {@link AutoDetach} flags.
     */
    int getAutoDetachConstant();

    /**
     * The {@link AutoDetach} flags.
     */
    void setAutoDetach(int flags);

    /**
     * Which field values to include when detaching.
     */
    void setDetachState(String detachState);

    /**
     * Return the instance specified by the detach state plugin.
     */
    DetachOptions getDetachStateInstance();

    /**
     * Return the instance specified by the detach state plugin.
     */
    void setDetachState(DetachOptions detachState);

    /**
     * Whether persistent state is accessible outside a transaction by default.
     */
    boolean getNontransactionalRead();

    /**
     * Whether persistent state is accessible outside a transaction by default.
     */
    void setNontransactionalRead(boolean ntRead);

    /**
     * Wrapper for JCA usage of {@link #setNontransactionalRead(boolean)}.
     */
    void setNontransactionalRead(Boolean ntRead);

    /**
     * Whether persistent state can be modified outside a transaction by
     * default.
     */
    boolean getNontransactionalWrite();

    /**
     * Whether persistent state can be modified outside a transaction by
     * default.
     */
    void setNontransactionalWrite(boolean ntWrite);

    /**
     * Wrapper for JCA usage of {@link #setNontransactionalWrite(boolean)}.
     */
    void setNontransactionalWrite(Boolean ntWrite);

    /**
     * Whether brokers or their managed objects will be used by multiple
     * concurrent threads.
     */
    boolean getMultithreaded();

    /**
     * Whether brokers or their managed objects will be used by multiple
     * concurrent threads.
     */
    void setMultithreaded(boolean multithreaded);

    /**
     * Wrapper for JCA usage of {@link #setMultithreaded(boolean)}.
     */
    void setMultithreaded(Boolean multithreaded);

    /**
     * Get the size of the batch that will be pre-selected when accessing
     * elements in a query or relationship. Use -1 to prefetch all results.
     */
    int getFetchBatchSize();

    /**
     * Set the size of the batch that will be pre-selected when accessing
     * elements in a query or relationship. Use -1 to prefetch all results.
     */
    void setFetchBatchSize(int size);

    /**
     * Wrapper for JCA usage of {@link #setFetchBatchSize(int)}.
     */
    void setFetchBatchSize(Integer size);

    /**
     * The maximum relation depth to traverse when eager fetching.  Use
     * -1 for no limit.
     */
    int getMaxFetchDepth();

    /**
     * The maximum relation depth to traverse when eager fetching.  Use
     * -1 for no limit.
     */
    void setMaxFetchDepth(int depth);

    /**
     * Wrapper for JCA usage of {@link #setMaxFetchDepth(int)}.
     */
    void setMaxFetchDepth(Integer size);

    /**
     * Comma-separated list of fetch group names that will be pre-set for
     * all new {@link FetchConfiguration}s.
     *
     * @since 0.2.5
     */
    String getFetchGroups();

    /**
     * Comma-separated list of fetch group names that will be pre-set for
     * all new {@link FetchConfiguration}s.
     *
     * @since 0.2.5
     */
    void setFetchGroups(String groups);

    /**
     * List of fetch group names that will be pre-set for all new
     * {@link FetchConfiguration}s.
     */
    String[] getFetchGroupsList();

    /**
     * List of fetch group names that will be pre-set for all new
     * {@link FetchConfiguration}s.
     */
    void setFetchGroups(String[] names);

    /**
     * Returns whether or not OpenJPA should automatically flush
     * modifications to the data store before executing queries.
     *
     * @since 0.2.5
     */
    String getFlushBeforeQueries();

    /**
     * Sets whether or not OpenJPA should automatically flush
     * modifications to the data store before executing queries.
     *
     * @since 0.2.5
     */
    void setFlushBeforeQueries(String flush);

    /**
     * Returns one of {@link QueryFlushModes#FLUSH_TRUE},
     * {@link QueryFlushModes#FLUSH_FALSE}, or
     * {@link QueryFlushModes#FLUSH_WITH_CONNECTION}, as determined
     * by parsing the string returned by {@link #getFlushBeforeQueries}.
     *
     * @since 0.2.5
     */
    int getFlushBeforeQueriesConstant();

    /**
     * Set to one of {@link QueryFlushModes#FLUSH_TRUE},
     * {@link QueryFlushModes#FLUSH_FALSE}, or
     * {@link QueryFlushModes#FLUSH_WITH_CONNECTION}.
     *
     * @since 0.2.5
     */
    void setFlushBeforeQueries(int flushBeforeQueries);

    /**
     * The time to wait for an object lock in milliseconds, or -1 for no
     * timeout.
     *
     * @since 0.3.1
     */
    int getLockTimeout();

    /**
     * The time to wait for an object lock in milliseconds, or -1 for no
     * timeout.
     *
     * @since 0.3.1
     */
    void setLockTimeout(int timeout);

    /**
     * Wrapper for JCA usage of {@link #setLockTimeout(int)}.
     *
     * @since 0.3.1
     */
    void setLockTimeout(Integer timeout);

    /**
     * The time to wait for a query to execute in milliseconds, or -1 for no
     * timeout.
     *
     * @since 2.0.0
     */
    int getQueryTimeout();

    /**
     * The time to wait for a query to execute in milliseconds, or -1 for no
     * timeout.
     *
     * @since 0.3.1
     */
    void setQueryTimeout(int timeout);

    /**
     * The default read lock level to use during non-optimistic transactions.
     * Defaults to <code>read</code>.
     *
     * @since 0.3.1
     */
    String getReadLockLevel();

    /**
     * The default read lock level to use during non-optimistic transactions.
     * Defaults to <code>read</code>.
     *
     * @since 0.3.1
     */
    void setReadLockLevel(String level);

    /**
     * The numeric read lock level.
     *
     * @since 0.3.1
     */
    int getReadLockLevelConstant();

    /**
     * The numeric read lock level.
     *
     * @since 0.3.1
     */
    void setReadLockLevel(int level);

    /**
     * The default write lock level to use during non-optimistic transactions.
     * Defaults to <code>write</code>.
     *
     * @since 0.3.1
     */
    String getWriteLockLevel();

    /**
     * The default write lock level to use during non-optimistic transactions.
     * Defaults to <code>write</code>.
     *
     * @since 0.3.1
     */
    void setWriteLockLevel(String level);

    /**
     * The numeric write lock level.
     *
     * @since 0.3.1
     */
    int getWriteLockLevelConstant();

    /**
     * The numeric write lock level.
     *
     * @since 0.3.1
     */
    void setWriteLockLevel(int level);

    /**
     * Plugin string for the default system {@link Seq}.
     */
    String getSequence();

    /**
     * Plugin string for the default system {@link Seq}.
     */
    void setSequence(String sequence);

    /**
     * The default system sequence.
     */
    Seq getSequenceInstance();

    /**
     * The default system sequence.
     */
    void setSequence(Seq sequence);

    /**
     * Specifies the behavior of the broker with respect to data store
     * connections. Possible values are:
     * <ul>
     * <li><code>always</code>: Each broker obtains a single connection and
     * uses it until the broker is closed.</li>
     * <li><code>transaction</code>: A connection is obtained when each
     * transaction begins (optimistic or datastore), and is released
     * when the transaction completes.</li>
     * <li><code>on-demand</code>: Connections are obtained only when needed.
     * This is the default mode. It is equivalent to the previous option
     * when datastore transactions are used. For optimistic transactions,
     * though, it means that a connection will be retained only for
     * the duration of the data store commit process.</li>
     * </ul>
     *
     * @since 0.2.5
     */
    String getConnectionRetainMode();

    /**
     * Specifies the behavior of the broker with respect to data store
     * connections. Possible values are:
     * <ul>
     * <li><code>always</code>: Each broker obtains a single connection and
     * uses it until the broker is closed.</li>
     * <li><code>transaction</code>: A connection is obtained when each
     * transaction begins (optimistic or datastore), and is released
     * when the transaction completes.</li>
     * <li><code>on-demand</code>: Connections are obtained only when needed.
     * This is the default mode. It is equivalent to the previous option
     * when datastore transactions are used. For optimistic transactions,
     * though, it means that a connection will be retained only for
     * the duration of the data store commit process.</li>
     * </ul>
     *
     * @since 0.2.5
     */
    void setConnectionRetainMode(String mode);

    /**
     * Return the connection retain mode as one of the following symbolic
     * constants:
     * <ul>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_ALWAYS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_TRANS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_DEMAND}</li>
     * </ul>
     */
    int getConnectionRetainModeConstant();

    /**
     * Set the connection retain mode as one of the following symbolic
     * constants:
     * <ul>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_ALWAYS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_TRANS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_DEMAND}</li>
     * </ul>
     */
    void setConnectionRetainMode(int mode);

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link FilterListener}s to use.
     */
    String getFilterListeners();

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link FilterListener}s to use.
     */
    void setFilterListeners(String listeners);

    /**
     * Return the query filter listeners. If none have been set explicitly,
     * this method instantiates the listeners from the set plugin list.
     */
    FilterListener[] getFilterListenerInstances();

    /**
     * Set the query filter listeners. Overrides the list of listener classes.
     */
    void setFilterListeners(FilterListener[] listeners);

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link AggregateListener}s to use.
     */
    String getAggregateListeners();

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link AggregateListener}s to use.
     */
    void setAggregateListeners(String listeners);

    /**
     * Return the query function listeners. If none have been set explicitly,
     * this method instantiates the listeners from the set plugin list.
     */
    AggregateListener[] getAggregateListenerInstances();

    /**
     * Set the query function listeners. Overrides the list of listener classes.
     */
    void setAggregateListeners(AggregateListener[] listeners);

    /**
     * Whether to warn and defer registration instead of throwing an
     * exception when a registered persistent class cannot be processed.
     * Should only be set to true in complex classloader topologies.
     * Defaults to <code>false</code>.
     *
     * @since 0.3.2.3
     */
    boolean getRetryClassRegistration();

    /**
     * Whether to warn and defer registration instead of throwing an
     * exception when a registered persistent class cannot be processed.
     * Should only be set to true in complex classloader topologies.
     * Defaults to <code>false</code>.
     *
     * @since 0.3.2.3
     */
    void setRetryClassRegistration(boolean warn);

    /**
     * Wrapper for JCA usage of {@link #setRetryClassRegistration(boolean)}.
     *
     * @since 0.3.2.3
     */
    void setRetryClassRegistration(Boolean warn);

    /**
     * Backwards compatibility options.
     */
    String getCompatibility();

    /**
     * Backwards compatibility options.
     */
    void setCompatibility(String compatibility);

    /**
     * Backwards compatibility options.
     */
    Compatibility getCompatibilityInstance ();

    /**
     * Options for configuring callbacks as a String.
     *
     * @since 2.0.0
     */
    String getCallbackOptions();

    /**
     * Options for configuring callbacks.
     *
     * @since 2.0.0
     */
    CallbackOptions getCallbackOptionsInstance();

    /**
     * Options for configuring callbacks set as a comma-separated string value
     * pair.
     *
     * @since 2.0.0
     */
    void setCallbackOptions(String options);

    /**
     * Configuration settings for the query compilation cache to use.
     * @see QueryCompilationCacheValue
     * @since 0.9.6
     */
    String getQueryCompilationCache();

    /**
     * Configuration settings for the query compilation cache to use.
     * @see QueryCompilationCacheValue
     * @since 0.9.6
     */
    void setQueryCompilationCache(String conf);

    /**
     * Configuration settings for the query compilation cache to use.
     * @see QueryCompilationCacheValue
     * @since 0.9.6
     */
    Map getQueryCompilationCacheInstance();

    /**
     * Return the {@link StoreFacadeTypeRegistry} instance associated with this
     * configuration.
     */
    StoreFacadeTypeRegistry getStoreFacadeTypeRegistry();

    /**
     * Return the {@link org.apache.openjpa.event.BrokerFactoryEventManager}
     * associated with this configuration.
     *
     * @since 1.0.0
     */
    BrokerFactoryEventManager getBrokerFactoryEventManager();

    /**
     * Specifies how OpenJPA handles unenhanced types. Possible values are:
     * <ul>
     * <li><code>supported</code>: Runtime optimization of persistent types
     * is available. This is the default</li>
     * <li><code>unsupported</code>: Runtime optimization of persistent types
     * is not available. An exception will be thrown if the system loads with
     * persistent types that are not enhanced.</li>
     * <li><code>warn</code>: Runtime optimization of persistent types is
     * not available, but no exception will be thrown initially. A warning will
     * be logged instead. It is likely that the system will fail at a later
     * point. This might be suitable for environments with complex classloader
     * configurations.</li>
     * </ul>
     *
     * @since 1.0.0
     */
    String getRuntimeUnenhancedClasses();

    /**
     * Specifies how OpenJPA handles unenhanced types.
     *
     * @see {@link #getRuntimeUnenhancedClasses()}
     * @since 1.0.0
     */
    void setRuntimeUnenhancedClasses(String mode);

    /**
     * Return the runtime class optimization setting as one of the
     * following symbolic constants:
     * <ul>
     * <li>{@link RuntimeUnenhancedClassesModes#SUPPORTED}</li>
     * <li>{@link RuntimeUnenhancedClassesModes#UNSUPPORTED}</li>
     * <li>{@link RuntimeUnenhancedClassesModes#WARN}</li>
     * </ul>
     *
     * @since 1.0.0
     */
    int getRuntimeUnenhancedClassesConstant();

    /**
     * Set the runtime class optimization setting as one of the
     * following symbolic constants:
     * <ul>
     * <li>{@link RuntimeUnenhancedClassesModes#SUPPORTED}</li>
     * <li>{@link RuntimeUnenhancedClassesModes#UNSUPPORTED}</li>
     * <li>{@link RuntimeUnenhancedClassesModes#WARN}</li>
     * </ul>
     *
     * @since 1.0.0
     */
    void setRuntimeUnenhancedClasses(int mode);
    /**
     * Whether OpenJPA will attempt to dynamically load the enhancement agent.
     */
    boolean getDynamicEnhancementAgent();
    /**
     * Sets whether OpenJPA will attempt to dynamically load the enhancement
     * agent.
     */
    void setDynamicEnhancementAgent(boolean dynamic);
    /**
     * A comma-separted list of the plugin strings specifying the
     * {@link CacheMarshaller}s to use.
     *
     * @since 1.1.0
     */
    String getCacheMarshallers();

    /**
     * A comma-separated list of the plugin strings specifying the
     * {@link CacheMarshaller}s to use.
     *
     * @since 1.1.0
     */
    void setCacheMarshallers(String marshallers);

    /**
     * Return the cache marshaller listeners.
     *
     * @since 1.1.0
     */
    Map<String,CacheMarshaller> getCacheMarshallerInstances();

    /**
     * Affirms if all configured elements are initialized eagerly as opposed
     * to lazily on-demand.
     *
     * @since 1.3.0
     */
    boolean isInitializeEagerly();

    /**
     * Sets whether all configured elements will be initialized eagerly or
     * lazily on-demand.
     *
     * @since 1.3.0
     */
    void setInitializeEagerly(boolean flag);

    /**
     * Return PreparedQueryCache used for caching datastore queries.
     *
     * @since 2.0.0
     */
    PreparedQueryCache getQuerySQLCacheInstance();

    /**
     * Gets the configuration of QuerySQLCache.
     *
     * @since 2.0.0
     */
    String getQuerySQLCache();

    /**
     * Sets QuerySQLCache with the given cache.
     *
     * @since 2.0.0
     */
    void setQuerySQLCache(PreparedQueryCache cache);

    /**
     * Sets QuerySQLCache with the given configuration.
     *
     * @since 2.0.0
     */
    void setQuerySQLCache(String config);

    /**
     * Get the cache of finder queries.
     *
     * @since 2.0.0
     */
    FinderCache getFinderCacheInstance();

    /**
     * Get the string configuration of the finder cache.
     *
     * @since 2.0.0
     */
    String getFinderCache();

    /**
     * Set the finder cache from a string configuration.
     *
     * @since 2.0.0
     */
    void setFinderCache(String cache);

    /**
     * The bean validation mode to use for managed classes.
     * Defaults to <code>AUTO</code>.
     *
     * @since 2.0.0
     */
    String getValidationMode();

    /**
     * Set the bean validation mode to use for managed classes.
     * If not set, defaults to <code>AUTO</code>.
     *
     * @since 2.0.0
     */
    void setValidationMode(String mode);

    /**
     * The ValidatorFactory provided by the container or application.
     * Defaults to <code>null</code>.
     *
     * @since 2.0.0
     */
    Object getValidationFactoryInstance();

    /**
     * Set the container or application provided ValidatorFactory instance.
     * If not set, defaults to <code>null</code>.
     *
     * @since 2.0.0
     */
    void setValidationFactory(Object factory);

    /**
     * The Validator provided by the container or created by the runtime.
     * Defaults to <code>null</code>.
     *
     * @since 2.0.0
     */
    Object getValidatorInstance();

    /**
     * Set the container or application provided Validator instance.
     * If not set, defaults to <code>null</code>.
     *
     * @since 2.0.0
     */
    void setValidatorInstance(Object val);

    /**
     * Gets the lifecycle event manager instance classname.
     *
     * @since 2.0.0
     */
    String getLifecycleEventManager();

    /**
     * Gets the lifecycle event manager instance.
     *
     * @since 2.0.0
     */
    LifecycleEventManager getLifecycleEventManagerInstance();

    /**
     * Sets the lifecycle event manager instance classname.
     *
     * @since 2.0.0
     */
    void setLifecycleEventManager(String eventMgr);

    /**
     * Gets the validation groups for pre-persist
     *
     * @since 2.0.0
     */
    String getValidationGroupPrePersist();

    /**
     * Sets the validation groups for pre-persist
     *
     * @since 2.0.0
     */
    void setValidationGroupPrePersist(String vgPrePersist);

    /**
     * Gets the validation groups for pre-update
     *
     * @since 2.0.0
     */
    String getValidationGroupPreUpdate();

    /**
     * Sets the validation groups for pre-update
     *
     * @since 2.0.0
     */
    void setValidationGroupPreUpdate(String vgPreUpdate);

    /**
     * Gets the validation groups for pre-remove
     *
     * @since 2.0.0
     */
    String getValidationGroupPreRemove();

    /**
     * Sets the validation groups for pre-remove
     *
     * @since 2.0.0
     */
    void setValidationGroupPreRemove(String vgPreRemove);

    /**
     * Sets the {@link EncryptionProvider}.
     *
     */
    void setEncryptionProvider(String className);

    /**
     * Gets the {@link EncryptionProvider}.
     *
     * @return EncryptionProvider
     */
    EncryptionProvider getEncryptionProvider();


    /**
     * Set the {@link DataCacheMode}
     *
     * @param mode One of the Sting constants from {@link DataCacheMode}
     * @since 2.0.0
     */
    void setDataCacheMode(String mode);

    /**
     * Return the String constant that matches the {@link DataCacheMode}
     * @return DataCacheMode
     * @since 2.0.0
     */
    String getDataCacheMode();

    /**
     * Gets the policy object that determines distribution of cached instances
     * across named partitions of L2 data cache.
     *
     * @return an implementation of {@link CacheDistributionPolicy}.
     * @since 2.0.0
     */
    CacheDistributionPolicy getCacheDistributionPolicyInstance();

    /**
     * Sets the policy object that determines distribution of cached instances
     * across named partitions of L2 data cache.
     *
     * @param policy a non-null implementation of {@link CacheDistributionPolicy}.
     * @since 2.0.0
     */
    void setCacheDistributionPolicyInstance(CacheDistributionPolicy policy);

    /**
     * Gets the plug-in string that described the policy to distribute cached instances
     * across named partitions of L2 data cache.
     *
     * @return a plug-in string for {@link CacheDistributionPolicy}.
     * @since 2.0.0
     */
    String getCacheDistributionPolicy();

    /**
     * Sets the plug-in string that describes the policy to distribute cached instances
     * across named partitions of L2 data cache.
     *
     * @param a plug-in string for {@link CacheDistributionPolicy}.
     * @since 2.0.0
     */
    void setCacheDistributionPolicy(String policyPlugin);

    /**
     * Gets the plug-in string that defines instrumentation providers and what
     * they instrument.
     * @return a plug-in string for the instrumentation configuration
     * @since 2.1.0
     */
    String getInstrumentation();

    /**
     * Sets the plug-in string that defines instrumentation providers and what
     * they instrument.
     * @param providers a plug-in string for the instrumentation configuration
     * @since 2.1.0
     */
    void setInstrumentation(String providers);

    /**
     * Gets an instance of the instrumentation manager.  The instrumentation
     * provides access to configured instrumentation providers and can be used
     * to manage them at runtime.
     * @return an instance of the instrumentation manager
     * @since 2.1.0
     */
    InstrumentationManager getInstrumentationManagerInstance();


    /**
     * Gets the singular instance of {@link Auditor} associated with this configuration.
     *
     * @since 2.2.0
     */
    Auditor getAuditorInstance();

    /**
     * Sets the singular instance of {@link Auditor} associated with this configuration.
     *
     * @since 2.2.0
     */

    void setAuditorInstance(Auditor auditor);

    /**
     * Gets the plug-in string of {@link Auditor} specified in this configuration.
     *
     * @since 2.2.0
     */
    String getAuditor();

    /**
     * Sets the plug-in string of {@link Auditor} specified in this configuration.
     *
     * @since 2.2.0
     */
    void setAuditor(String s);

    /**
     * Whether to send &#064;PostLoad events on a merge operation.
     * @since 2.2.0
     */
    boolean getPostLoadOnMerge();

    /**
     * Whether to send &#064;PostLoad events on a merge operation.
     * @since 2.2.0
     */
    void setPostLoadOnMerge(boolean postLoadOnMerge);

    /**
     * Whether to send &#064;PostLoad events on a merge operation.
     * @since 2.2.0
     */
    void setPostLoadOnMerge(Boolean postLoadOnMerge);

    /**
     * Whether to attempt to optimize id class copy operations during the
     * enhancement process.  Optimization is only applicable for simple id classes
     * that have a constructor with the proper construction parameters and
     * direct assignments to fields within the id class during construction.
     * If the optimization cannot occur, the enhancer will fallback to the
     * noraml behavior.
     * @since 2.2.0
     */
    boolean getOptimizeIdCopy();

    /**
     * Whether to attempt to optimize id class copy operations during the
     * enhancement process.  Optimization is only applicable for simple id classes
     * that have a constructor with the proper construction parameters and
     * direct assignments to fields within the id class during construction.
     * If the optimization cannot occur, the enhancer will fallback to the
     * normal behavior.
     * @since 2.2.0
     */
    void setOptimizeIdCopy(boolean optimizeIds);

    /**
     * Whether to attempt to optimize id class copy operations during the
     * enhancement process.  Optimization is only applicable for simple id classes
     * that have a constructor with the proper construction parameters and
     * direct assignments to fields within the id class during construction.
     * If the optimization cannot occur, the enhancer will fallback to the
     * normal behavior.
     * @since 2.2.0
     */
    void setOptimizeIdCopy(Boolean optimizeIds);

    /**
     * Indicates if the {@link Thread#contextClassLoader} should be used
     * as fallback if the result class for {@code select new} is not found
     * by the classloader of the entity.
     *
     * @since 2.4.2
     */
    boolean getUseTCCLinSelectNew();

    /**
     * Indicates if the {@link Thread#contextClassLoader} should be used
     * as fallback if the result class for {@code select new} is not found
     * by the classloader of the entity.
     *
     * @since 2.4.2
     */
    void setUseTCCLinSelectNew(boolean useTcclForSelectNew);

    /**
     * Indicates if the {@link Thread#contextClassLoader} should be used
     * as fallback if the result class for {@code select new} is not found
     * by the classloader of the entity.
     *
     * @since 2.4.2
     */
    void setUseTCCLinSelectNew(Boolean useTcclForSelectNew);

    /**
     * database action from the persistence unit.
     * See 'javax.persistence.schema-generation.database.action'
     * @return openjpa internal schemaAction which corresponds to the JPA action.
     * @since 3.0.0
     */
    String getDatabaseAction();

    /**
     * @see #getDatabaseAction()
     * @since 3.0.0
     */
    int getDatabaseActionConstant();

    /**
     * Similar to {@link #getDatabaseAction()} but write the generated SQL
     * to files.
     * @see #getDatabaseAction()
     * @since 3.0.0
     */
    String getScriptsAction();

    /**
     * @see #getScriptsAction()
     * @since 3.0.0
     */
    int getScriptsActionConstant();

    /**
     * Optional run a DB action before accessing the database.
     * The possible values in the persistence unit are:
     * <ul>
     *     <li>metadata</li>
     *     <li>script - see {@link #getCreateScriptSource()}</li>
     *     <li>metadata-then-script</li>
     *     <li>script-then-metadata</li>
     * </ul>
     * @see #getScriptsAction()
     * @since 3.0.0
     */
    String getCreateSource();

    /**
     * @see #getCreateSource()
     * @since 3.0.0
     */
    int getCreateSourceConstant();

    /**
     * Similar to {@link #getCreateSource()} but for dropping the database schema and content.
     * @see #getCreateSource()
     * @since 3.0.0
     */
    String getDropSource();

    /**
     * @see #getDatabaseAction()
     * @since 3.0.0
     */
    int getDropSourceConstant();

    /**
     * @see #getDatabaseAction()
     * @since 3.0.0
     */
    String getCreateScriptSource();

    /**
     * @see #getCreateSource()
     * @since 3.0.0
     */
    String getDropScriptSource();

    /**
     * @see #getDatabaseAction() 'script'
     * @since 3.0.0
     */
    String getCreateScriptTarget();

    /**
     * @see #getDatabaseAction() 'script'
     * @since 3.0.0
     */
    String getDropScriptTarget();

    /**
     * Can be used to load a set of initial data into the database.
     * @since 3.0.0
     */
    String getLoadScriptSource();

    /**
     * @return types which should not get enhanced although they are on the included directories.
     * @since 3.1.1
     */
    Collection<Class<?>> getTypesWithoutEnhancement();

    /**
     * @see #getTypesWithoutEnhancement()
     * @since 3.1.1
     */
    void setTypesWithoutEnhancement(Collection<Class<?>> typesWithoutEnhancement);
}


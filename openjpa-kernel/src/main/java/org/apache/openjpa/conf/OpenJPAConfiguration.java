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

import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.InverseManager;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.RestoreState;
import org.apache.openjpa.kernel.SavepointManager;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.conf.Configuration;
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
    public static final String LOG_METADATA = "openjpa.MetaData";

    /**
     * Name of logger for enhancement-related messages:
     * <code>openjpa.Enhance</code>.
     */
    public static final String LOG_ENHANCE = "openjpa.Enhance";

    /**
     * Name of logger for messages from the runtime system:
     * <code>openjpa.Runtime</code>.
     */
    public static final String LOG_RUNTIME = "openjpa.Runtime";

    /**
     * Name of logger for query logging:
     * <code>openjpa.Query</code>.
     */
    public static final String LOG_QUERY = "openjpa.Query";

    /**
     * Name of logger for messages from the data cache:
     * <code>openjpa.DataCache</code>.
     */
    public static final String LOG_DATACACHE = "openjpa.DataCache";

    /**
     * Name of logger for messages from the development tools:
     * <code>openjpa.Tool</code>.
     */
    public static final String LOG_TOOL = "openjpa.Tool";

    /**
     * Option for runtimes that support nontransactional reads.
     */
    public static final String OPTION_NONTRANS_READ =
        "openjpa.option.NontransactionalRead";

    /**
     * Option for runtimes that support optimistic transactions.
     */
    public static final String OPTION_OPTIMISTIC =
        "openjpa.option.Optimistic";

    /**
     * Option for runtimes that support application identity.
     */
    public static final String OPTION_ID_APPLICATION =
        "openjpa.option.ApplicationIdentity";

    /**
     * Option for runtimes that support application identity.
     */
    public static final String OPTION_ID_DATASTORE =
        "openjpa.option.DatastoreIdentity";

    /**
     * Option for SQL support.
     */
    public static final String OPTION_QUERY_SQL = "openjpa.option.SQL";

    /**
     * Option for runtimes that support persistent collection fields.
     */
    public static final String OPTION_TYPE_COLLECTION =
        "openjpa.option.Collection";

    /**
     * Option for runtimes that support persistent map fields.
     */
    public static final String OPTION_TYPE_MAP = "openjpa.option.Map";

    /**
     * Option for runtimes that support persistent array fields.
     */
    public static final String OPTION_TYPE_ARRAY = "openjpa.option.Array";

    /**
     * Option for runtime that can differentiate between null and empty
     * container fields.
     */
    public static final String OPTION_NULL_CONTAINER =
        "openjpa.option.NullContainer";

    /**
     * Option for runtimes that support embedded relations to other
     * persistence capable objects.
     */
    public static final String OPTION_EMBEDDED_RELATION =
        "openjpa.option.EmbeddedRelation";

    /**
     * Option for runtimes that support collections of embedded
     * relations to other persistence capable objects.
     */
    public static final String OPTION_EMBEDDED_COLLECTION_RELATION =
        "openjpa.option.EmbeddedCollectionRelation";

    /**
     * Option for runtimes that support maps of embedded
     * relations to other persistence capable objects.
     */
    public static final String OPTION_EMBEDDED_MAP_RELATION =
        "openjpa.option.EmbeddedMapRelation";

    /**
     * Option for runtimes that support incremental flushing.
     */
    public static final String OPTION_INC_FLUSH =
        "openjpa.option.IncrementalFlush";

    /**
     * Option for runtimes that the autoassign value strategy.
     */
    public static final String OPTION_VALUE_AUTOASSIGN =
        "openjpa.option.AutoassignValue";

    /**
     * Option for runtimes that the increment value strategy.
     */
    public static final String OPTION_VALUE_INCREMENT =
        "openjpa.option.IncrementValue";

    /**
     * Option for runtimes that support returning the datastore connection.
     */
    public static final String OPTION_DATASTORE_CONNECTION =
        "openjpa.option.DataStoreConnection";

    /**
     * Option for runtimes that support returning the datastore connection
     * that is a JDBC Connection.
     */
    public static final String OPTION_JDBC_CONNECTION =
        "openjpa.option.JDBCConnection";

    /**
     * Return the set of option strings supported by this runtime. This set
     * is mutable.
     */
    public Collection supportedOptions();

    /**
     * A configuration can be set with defaults for a specific specification.
     */
    public String getSpecification();

    /**
     * Set the specification that this configuration should use for the
     * various properties that need to have different defaults for different
     * spec environments. This should be invoked before any configuration
     * options are set, as it will mutate various values.
     * You can only assign the specification once, though it is not fatal
     * to attempt to do so multiple times. Attempts to set to null will
     * be ignored.
     */
    public boolean setSpecification(String spec);

    /**
     * The plugin string for the {@link ClassResolver} to use for custom
     * class loading.
     */
    public String getClassResolver();

    /**
     * The plugin string for the {@link ClassResolver} to use for custom
     * class loading.
     */
    public void setClassResolver(String classResolver);

    /**
     * The {@link ClassResolver} to use.
     */
    public ClassResolver getClassResolverInstance();

    /**
     * The {@link ClassResolver} to use.
     */
    public void setClassResolver(ClassResolver classResolver);

    /**
     * The {@link BrokerFactory} class to use.
     */
    public String getBrokerFactory();

    /**
     * The {@link BrokerFactory} class to use.
     */
    public void setBrokerFactory(String factory);

    /**
     * The plugin string of the {@link BrokerImpl} extension to create.
     */
    public String getBrokerImpl();

    /**
     * The plugin string of the {@link BrokerImpl} extension to create.
     */
    public void setBrokerImpl(String broker);

    /**
     * Create a new broker instance with the configured plugin data.
     */
    public BrokerImpl newBrokerInstance(String user, String pass);

    /**
     * The {@link DataCache} to use for level-2 data store caching.
     */
    public String getDataCache();

    /**
     * The {@link DataCache} to use for level-2 data store caching.
     */
    public void setDataCache(String dataCache);

    /**
     * The data cache manager manages this configuration's cache instances.
     */
    public String getDataCacheManager();

    /**
     * The data cache manager manages this configuration's cache instances.
     */
    public void setDataCacheManager(String mgr);

    /**
     * The data cache manager manages this configuration's cache instances.
     * The cache manager is created if it has not been set. Once the cache
     * manager has been set/created, all changes to caching configuration
     * must proceed through the cache manager.
     *
     * @since 0.3.0
     */
    public DataCacheManager getDataCacheManagerInstance();

    /**
     * The data cache manager manages this configuration's cache instances.
     *
     * @since 0.3.0
     */
    public void setDataCacheManager(DataCacheManager manager);

    /**
     * Default data cache timeout.
     *
     * @since 0.2.5
     */
    public int getDataCacheTimeout();

    /**
     * Default data cache timeout.
     *
     * @since 0.2.5
     */
    public void setDataCacheTimeout(int timeout);

    /**
     * Wrapper for JCA usage of {@link #setDataCacheTimeout(int)}.
     *
     * @since 0.2.5
     */
    public void setDataCacheTimeout(Integer timeout);

    /**
     * The plugin to use for level-2 data store query caching.
     *
     * @since 0.2.5
     */
    public String getQueryCache();

    /**
     * The plugin to use for level-2 data store query caching.
     *
     * @since 0.2.5
     */
    public void setQueryCache(String queryCache);

    /**
     * Return whether to generate dynamic data structures
     * where possible for cache and runtime usage.
     *
     * @since 0.3.3
     */
    public boolean getDynamicDataStructs();

    /**
     * Set whether to generate dynamic data structures
     * where possible for cache and runtime usage.
     *
     * @since 0.3.3
     */
    public void setDynamicDataStructs(boolean dynamic);

    /**
     * Wrapper for JCA usage of {@link #setDynamicDataStructs(boolean)}.
     */
    public void setDynamicDataStructs(Boolean dynamic);

    /**
     * The plugin to use for datastore lock management.
     *
     * @since 0.3.1
     */
    public String getLockManager();

    /**
     * The plugin to use for datastore lock management.
     *
     * @since 0.3.1
     */
    public void setLockManager(String lockManager);

    /**
     * Return a new lock manager instance using the configured plugin settings.
     */
    public LockManager newLockManagerInstance();

    /**
     * The plugin to use for managing inverse relations.
     *
     * @since 0.3.2
     */
    public String getInverseManager();

    /**
     * The plugin to use for managing inverse relations.
     *
     * @since 0.3.2
     */
    public void setInverseManager(String inverse);

    /**
     * Return a new inverse manager instance using the configured plugin
     * settings.
     *
     * @since 0.3.2
     */
    public InverseManager newInverseManagerInstance();

    /**
     * The plugin to use for savepoint management.
     *
     * @since 0.3.4
     */
    public String getSavepointManager();

    /**
     * The plugin to use for savepoint management.
     *
     * @since 0.3.4
     */
    public void setSavepointManager(String savepointManager);

    /**
     * Return the configured savepoint manager instance.
     */
    public SavepointManager getSavepointManagerInstance();

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    public String getOrphanedKeyAction();

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    public void setOrphanedKeyAction(String action);

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    public OrphanedKeyAction getOrphanedKeyActionInstance();

    /**
     * The action to take when an orphaned key is detected.
     *
     * @since 0.3.2.2
     */
    public void setOrphanedKeyAction(OrphanedKeyAction action);

    /**
     * The plugin to use for remote commit notification.
     *
     * @since 0.2.5
     */
    public String getRemoteCommitProvider();

    /**
     * The plugin to use for remote commit notification.
     *
     * @since 0.2.5
     */
    public void setRemoteCommitProvider(String remoteCommitProvider);

    /**
     * Create a remote commit provider from the configured plugin.
     *
     * @since 0.3.0
     */
    public RemoteCommitProvider newRemoteCommitProviderInstance();

    /**
     * The remote event manager that manages this configuration's remote
     * event listeners.
     *
     * @since 0.3.0
     */
    public RemoteCommitEventManager getRemoteCommitEventManager();

    /**
     * The remote event manager that manages this configuration's remote
     * event listeners.
     *
     * @since 0.3.0
     */
    public void setRemoteCommitEventManager(RemoteCommitEventManager manager);

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
    public String getTransactionMode();

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
    public void setTransactionMode(String mode);

    /**
     * Return whether managed transactions are being used.
     */
    public boolean isTransactionModeManaged();

    /**
     * Set whether managed transactions are being used.
     */
    public void setTransactionModeManaged(boolean managed);

    /**
     * The plugin string for the {@link ManagedRuntime} to use for managed
     * environments.
     */
    public String getManagedRuntime();

    /**
     * The plugin string for the {@link ManagedRuntime} to use for managed
     * environments.
     */
    public void setManagedRuntime(String managedRuntime);

    /**
     * The plugin to use for integrating with a managed runtime.
     */
    public ManagedRuntime getManagedRuntimeInstance();

    /**
     * The plugin to use for integrating with a managed runtime.
     */
    public void setManagedRuntime(ManagedRuntime runtime);

    /**
     * The plugin string for the {@link ProxyManager} to use for second
     * class object proxies.
     */
    public String getProxyManager();

    /**
     * The plugin string for the {@link ProxyManager} to use for second
     * class object proxies.
     */
    public void setProxyManager(String proxyManager);

    /**
     * The {@link ProxyManager} to use.
     */
    public ProxyManager getProxyManagerInstance();

    /**
     * The {@link ProxyManager} to use.
     */
    public void setProxyManager(ProxyManager manager);

    /**
     * The name mapping to use for this data store.
     */
    public String getMapping();

    /**
     * The name mapping to use for this data store.
     */
    public void setMapping(String mapping);

    /**
     * A plugin string describing the {@link MetaDataFactory} to use.
     */
    public String getMetaDataFactory();

    /**
     * A plugin string describing the {@link MetaDataFactory} to use.
     */
    public void setMetaDataFactory(String meta);

    /**
     * Create a new {@link MetaDataFactory} to use with a repository.
     */
    public MetaDataFactory newMetaDataFactoryInstance();

    /**
     * A plugin string describing the {@link MetaDataRepository} to use.
     */
    public String getMetaDataRepository();

    /**
     * A plugin string describing the {@link MetaDataRepository} to use.
     */
    public void setMetaDataRepository(String meta);

    /**
     * The metadata repository of managed class information. If no
     * repository has been set, creates one.
     *
     * @since 0.3.0
     */
    public MetaDataRepository getMetaDataRepositoryInstance();

    /**
     * Create a new empty metadata repository of the configured type.
     */
    public MetaDataRepository newMetaDataRepositoryInstance();

    /**
     * The metadata repository of managed class information.
     *
     * @since 0.3.0
     */
    public void setMetaDataRepository(MetaDataRepository mdRepos);

    /**
     * The user name for the data store connection.
     */
    public String getConnectionUserName();

    /**
     * The user name for the data store connection.
     */
    public void setConnectionUserName(String connectionUserName);

    /**
     * The password for the data store connection.
     */
    public String getConnectionPassword();

    /**
     * The password for the data store connection.
     */
    public void setConnectionPassword(String connectionPassword);

    /**
     * The URL for the data store connection.
     */
    public String getConnectionURL();

    /**
     * The URL for the data store connection.
     */
    public void setConnectionURL(String connectionURL);

    /**
     * Class name of the connection driver.
     */
    public String getConnectionDriverName();

    /**
     * Class name of the connection driver.
     */
    public void setConnectionDriverName(String driverName);

    /**
     * The name for the data store connection factory.
     */
    public String getConnectionFactoryName();

    /**
     * The name for the data store connection factory.
     */
    public void setConnectionFactoryName(String cfName);

    /**
     * The connection factory, possibly from JNDI.
     */
    public Object getConnectionFactory();

    /**
     * The connection factory.
     */
    public void setConnectionFactory(Object factory);

    /**
     * These properties provide any additional information needed to
     * establish connections.
     */
    public String getConnectionProperties();

    /**
     * These properties provide any additional information needed to
     * establish connections.
     */
    public void setConnectionProperties(String props);

    /**
     * Configuration properties for the connection factory.
     */
    public String getConnectionFactoryProperties();

    /**
     * Configuration properties for the connection factory.
     */
    public void setConnectionFactoryProperties(String props);

    /**
     * The mode of the connection factory in use. Available options are:
     * <ul>
     * <li>local: OpenJPA controls the connections.</li>
     * <li>managed: Connections are automatically enlisted in
     * the current global transaction by an application server.</li>
     * </ul> Defaults to local.
     */
    public String getConnectionFactoryMode();

    /**
     * The mode of the connection factory in use. Available options are:
     * <ul>
     * <li>local: OpenJPA controls the connections.</li>
     * <li>managed: Connections are automatically enlisted in
     * the current global transaction by an application server.</li>
     * </ul> Defaults to local.
     */
    public void setConnectionFactoryMode(String mode);

    /**
     * Whether connections are automatically enlisted in global transactions.
     */
    public boolean isConnectionFactoryModeManaged();

    /**
     * Whether connections are automatically enlisted in global transactions.
     */
    public void setConnectionFactoryModeManaged(boolean managed);

    /**
     * The user name for the non-XA data store connection.
     */
    public String getConnection2UserName();

    /**
     * The user name for the non-XA data store connection.
     */
    public void setConnection2UserName(String connectionUserName);

    /**
     * The password for the non-XA data store connection.
     */
    public String getConnection2Password();

    /**
     * The password for the non-XA data store connection.
     */
    public void setConnection2Password(String connectionPassword);

    /**
     * The URL for the non-XA data store connection.
     */
    public String getConnection2URL();

    /**
     * The URL for the non-XA data store connection.
     */
    public void setConnection2URL(String connectionURL);

    /**
     * Class name of the non-XA connection driver.
     */
    public String getConnection2DriverName();

    /**
     * Class name of the non-XA connection driver.
     */
    public void setConnection2DriverName(String driverName);

    /**
     * The name for the second data store connection factory.
     */
    public String getConnectionFactory2Name();

    /**
     * The name for the second data store connection factory.
     */
    public void setConnectionFactory2Name(String cf2Name);

    /**
     * The non-XA connection factory.
     */
    public Object getConnectionFactory2();

    /**
     * The non-XA connection factory.
     */
    public void setConnectionFactory2(Object factory);

    /**
     * These properties provide any additional information needed to
     * establish non-XA connections.
     *
     * @since 0.3.0
     */
    public String getConnection2Properties();

    /**
     * These properties provide any additional information needed to
     * establish non-XA connections.
     *
     * @since 0.3.0
     */
    public void setConnection2Properties(String props);

    /**
     * Configuration properties for the non-XA connection factory.
     *
     * @since 0.2.5
     */
    public String getConnectionFactory2Properties();

    /**
     * Configuration properties for the non-XA connection factory.
     *
     * @since 0.2.5
     */
    public void setConnectionFactory2Properties(String props);

    /**
     * Whether to use optimistic transactions by default.
     */
    public boolean getOptimistic();

    /**
     * Whether to use optimistic transactions by default.
     */
    public void setOptimistic(boolean optimistic);

    /**
     * Wrapper for JCA usage of {@link #setOptimistic(boolean)}.
     */
    public void setOptimistic(Boolean optimistic);

    /**
     * Whether to retain state after a transaction by default.
     */
    public boolean getRetainState();

    /**
     * Whether to retain state after a transaction by default.
     */
    public void setRetainState(boolean retainState);

    /**
     * Wrapper for JCA usage of {@link #setRetainState(boolean)}.
     */
    public void setRetainState(Boolean retainState);

    /**
     * Whether instances clear their state when entering a transaction.
     */
    public String getAutoClear();

    /**
     * Whether instances clear their state when entering a transaction.
     */
    public void setAutoClear(String clear);

    /**
     * Return the {@link AutoClear} constant.
     */
    public int getAutoClearConstant();

    /**
     * Whether instances clear their state when entering a transaction.
     */
    public void setAutoClear(int clear);

    /**
     * Whether to restore initial state on rollback by default.
     */
    public String getRestoreState();

    /**
     * Whether to restore initial state on rollback by default.
     */
    public void setRestoreState(String restoreState);

    /**
     * Return the {@link RestoreState} constant.
     */
    public int getRestoreStateConstant();

    /**
     * Whether to restore initial state on rollback by default.
     */
    public void setRestoreState(int restoreState);

    /**
     * Whether changes in the current transaction are taken into account when
     * executing queries and iterating extents.
     */
    public boolean getIgnoreChanges();

    /**
     * Whether changes in the current transaction are taken into account when
     * executing queries and iterating extents.
     */
    public void setIgnoreChanges(boolean ignoreChanges);

    /**
     * Wrapper for JCA usage of {@link #setIgnoreChanges(boolean)}.
     */
    public void setIgnoreChanges(Boolean ignoreChanges);

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
    public String getAutoDetach();

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
    public void setAutoDetach(String detach);

    /**
     * The {@link AutoDetach} flags.
     */
    public int getAutoDetachConstant();

    /**
     * The {@link AutoDetach} flags.
     */
    public void setAutoDetach(int flags);

    /**
     * Which field values to include when detaching.
     */
    public void setDetachState(String detachState);

    /**
     * Return the instance specified by the detach state plugin.
     */
    public DetachOptions getDetachStateInstance();

    /**
     * Return the instance specified by the detach state plugin.
     */
    public void setDetachState(DetachOptions detachState);

    /**
     * Whether persistent state is accessible outside a transaction by default.
     */
    public boolean getNontransactionalRead();

    /**
     * Whether persistent state is accessible outside a transaction by default.
     */
    public void setNontransactionalRead(boolean ntRead);

    /**
     * Wrapper for JCA usage of {@link #setNontransactionalRead(boolean)}.
     */
    public void setNontransactionalRead(Boolean ntRead);

    /**
     * Whether persistent state can be modified outside a transaction by
     * default.
     */
    public boolean getNontransactionalWrite();

    /**
     * Whether persistent state can be modified outside a transaction by
     * default.
     */
    public void setNontransactionalWrite(boolean ntWrite);

    /**
     * Wrapper for JCA usage of {@link #setNontransactionalWrite(boolean)}.
     */
    public void setNontransactionalWrite(Boolean ntWrite);

    /**
     * Whether brokers or their managed objects will be used by multiple
     * concurrent threads.
     */
    public boolean getMultithreaded();

    /**
     * Whether brokers or their managed objects will be used by multiple
     * concurrent threads.
     */
    public void setMultithreaded(boolean multithreaded);

    /**
     * Wrapper for JCA usage of {@link #setMultithreaded(boolean)}.
     */
    public void setMultithreaded(Boolean multithreaded);

    /**
     * Get the size of the batch that will be pre-selected when accessing
     * elements in a query or relationship. Use -1 to prefetch all results.
     */
    public int getFetchBatchSize();

    /**
     * Set the size of the batch that will be pre-selected when accessing
     * elements in a query or relationship. Use -1 to prefetch all results.
     */
    public void setFetchBatchSize(int size);

    /**
     * Wrapper for JCA usage of {@link #setFetchBatchSize(int)}.
     */
    public void setFetchBatchSize(Integer size);

    /**
     * The maximum relation depth to traverse when eager fetching.  Use
     * -1 for no limit.
     */
    public int getMaxFetchDepth();

    /**
     * The maximum relation depth to traverse when eager fetching.  Use
     * -1 for no limit.
     */
    public void setMaxFetchDepth(int depth);

    /**
     * Wrapper for JCA usage of {@link #setMaxFetchDepth(int)}.
     */
    public void setMaxFetchDepth(Integer size);

    /**
     * Comma-separated list of fetch group names that will be pre-set for
     * all new {@link FetchConfiguration}s.
     *
     * @since 0.2.5
     */
    public String getFetchGroups();

    /**
     * Comma-separated list of fetch group names that will be pre-set for
     * all new {@link FetchConfiguration}s.
     *
     * @since 0.2.5
     */
    public void setFetchGroups(String groups);

    /**
     * List of fetch group names that will be pre-set for all new
     * {@link FetchConfiguration}s.
     */
    public String[] getFetchGroupsList();

    /**
     * List of fetch group names that will be pre-set for all new
     * {@link FetchConfiguration}s.
     */
    public void setFetchGroups(String[] names);

    /**
     * Returns whether or not OpenJPA should automatically flush
     * modifications to the data store before executing queries.
     *
     * @since 0.2.5
     */
    public String getFlushBeforeQueries();

    /**
     * Sets whether or not OpenJPA should automatically flush
     * modifications to the data store before executing queries.
     *
     * @since 0.2.5
     */
    public void setFlushBeforeQueries(String flush);

    /**
     * Returns one of {@link QueryFlushModes#FLUSH_TRUE},
     * {@link QueryFlushModes#FLUSH_FALSE}, or
     * {@link QueryFlushModes#FLUSH_WITH_CONNECTION}, as determined
     * by parsing the string returned by {@link #getFlushBeforeQueries}.
     *
     * @since 0.2.5
     */
    public int getFlushBeforeQueriesConstant();

    /**
     * Set to one of {@link QueryFlushModes#FLUSH_TRUE},
     * {@link QueryFlushModes#FLUSH_FALSE}, or
     * {@link QueryFlushModes#FLUSH_WITH_CONNECTION}.
     *
     * @since 0.2.5
     */
    public void setFlushBeforeQueries(int flushBeforeQueries);

    /**
     * The time to wait for an object lock in milliseconds, or -1 for no
     * timeout.
     *
     * @since 0.3.1
     */
    public int getLockTimeout();

    /**
     * The time to wait for an object lock in milliseconds, or -1 for no
     * timeout.
     *
     * @since 0.3.1
     */
    public void setLockTimeout(int timeout);

    /**
     * Wrapper for JCA usage of {@link #setLockTimeout(int)}.
     *
     * @since 0.3.1
     */
    public void setLockTimeout(Integer timeout);

    /**
     * The default read lock level to use during non-optimistic transactions.
     * Defaults to <code>read</code>.
     *
     * @since 0.3.1
     */
    public String getReadLockLevel();

    /**
     * The default read lock level to use during non-optimistic transactions.
     * Defaults to <code>read</code>.
     *
     * @since 0.3.1
     */
    public void setReadLockLevel(String level);

    /**
     * The numeric read lock level.
     *
     * @since 0.3.1
     */
    public int getReadLockLevelConstant();

    /**
     * The numeric read lock level.
     *
     * @since 0.3.1
     */
    public void setReadLockLevel(int level);

    /**
     * The default write lock level to use during non-optimistic transactions.
     * Defaults to <code>write</code>.
     *
     * @since 0.3.1
     */
    public String getWriteLockLevel();

    /**
     * The default write lock level to use during non-optimistic transactions.
     * Defaults to <code>write</code>.
     *
     * @since 0.3.1
     */
    public void setWriteLockLevel(String level);

    /**
     * The numeric write lock level.
     *
     * @since 0.3.1
     */
    public int getWriteLockLevelConstant();

    /**
     * The numeric write lock level.
     *
     * @since 0.3.1
     */
    public void setWriteLockLevel(int level);

    /**
     * Plugin string for the default system {@link Seq}.
     */
    public String getSequence();

    /**
     * Plugin string for the default system {@link Seq}.
     */
    public void setSequence(String sequence);

    /**
     * The default system sequence.
     */
    public Seq getSequenceInstance();

    /**
     * The default system sequence.
     */
    public void setSequence(Seq sequence);

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
    public String getConnectionRetainMode();

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
    public void setConnectionRetainMode(String mode);

    /**
     * Return the connection retain mode as one of the following symbolic
     * constants:
     * <ul>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_ALWAYS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_TRANS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_DEMAND}</li>
     * </ul>
     */
    public int getConnectionRetainModeConstant();

    /**
     * Set the connection retain mode as one of the following symbolic
     * constants:
     * <ul>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_ALWAYS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_TRANS}</li>
     * <li>{@link ConnectionRetainModes#CONN_RETAIN_DEMAND}</li>
     * </ul>
     */
    public void setConnectionRetainMode(int mode);

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link FilterListener}s to use.
     */
    public String getFilterListeners();

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link FilterListener}s to use.
     */
    public void setFilterListeners(String listeners);

    /**
     * Return the query filter listeners. If none have been set explicitly,
     * this method instantiates the listeners from the set plugin list.
     */
    public FilterListener[] getFilterListenerInstances();

    /**
     * Set the query filter listeners. Overrides the list of listener classes.
     */
    public void setFilterListeners(FilterListener[] listeners);

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link AggregateListener}s to use.
     */
    public String getAggregateListeners();

    /**
     * A comma-separted list of the plugin strings of the query
     * {@link AggregateListener}s to use.
     */
    public void setAggregateListeners(String listeners);

    /**
     * Return the query function listeners. If none have been set explicitly,
     * this method instantiates the listeners from the set plugin list.
     */
    public AggregateListener[] getAggregateListenerInstances();

    /**
     * Set the query function listeners. Overrides the list of listener classes.
     */
    public void setAggregateListeners(AggregateListener[] listeners);

    /**
     * Whether to warn and defer registration instead of throwing an
     * exception when a registered persistent class cannot be processed.
     * Should only be set to true in complex classloader topologies.
     * Defaults to <code>false</code>.
     *
     * @since 0.3.2.3
     */
    public boolean getRetryClassRegistration();

    /**
     * Whether to warn and defer registration instead of throwing an
     * exception when a registered persistent class cannot be processed.
     * Should only be set to true in complex classloader topologies.
	 * Defaults to <code>false</code>.
	 *
	 * @since 0.3.2.3
	 */
	public void setRetryClassRegistration (boolean warn);

	/**
	 * Wrapper for JCA usage of {@link #setRetryClassRegistration(boolean)}.
	 *
	 * @since 0.3.2.3
	 */
	public void setRetryClassRegistration (Boolean warn);

	/**
	 * Backwards compatibility options.
	 */
	public String getCompatibility ();

	/**
	 * Backwards compatibility options.
	 */
	public void setCompatibility (String compatibility);

	/**
	 * Backwards compatibility options.
	 */
	public Compatibility getCompatibilityInstance ();
    
    /**
     * Return the {@link StoreFacadeTypeRegistry} instance associated with this
     * configuration.
     */
    public StoreFacadeTypeRegistry getStoreFacadeTypeRegistry();
}

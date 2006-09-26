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
package org.apache.openjpa.jdbc.conf;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.JDBCBrokerFactory;
import org.apache.openjpa.jdbc.kernel.LRSSizes;
import org.apache.openjpa.jdbc.kernel.PessimisticLockManager;
import org.apache.openjpa.jdbc.kernel.UpdateManager;
import org.apache.openjpa.jdbc.meta.MappingDefaults;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.DataSourceFactory;
import org.apache.openjpa.jdbc.schema.DriverDataSource;
import org.apache.openjpa.jdbc.schema.SchemaFactory;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DBDictionaryFactory;
import org.apache.openjpa.jdbc.sql.SQLFactory;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.conf.IntValue;
import org.apache.openjpa.lib.conf.ObjectValue;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.lib.conf.StringListValue;
import org.apache.openjpa.lib.conf.StringValue;
import org.apache.openjpa.lib.jdbc.ConnectionDecorator;
import org.apache.openjpa.lib.jdbc.DecoratingDataSource;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MetaDataFactory;

/**
 * Default implementation of the {@link JDBCConfiguration} interface.
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 */
public class JDBCConfigurationImpl
    extends OpenJPAConfigurationImpl
    implements JDBCConfiguration {

    public StringValue schema;
    public StringListValue schemas;
    public IntValue transactionIsolation;
    public IntValue resultSetType;
    public IntValue fetchDirection;
    public FetchModeValue eagerFetchMode;
    public FetchModeValue subclassFetchMode;
    public IntValue lrsSize;
    public StringValue synchronizeMappings;
    public ObjectValue jdbcListenerPlugins;
    public ObjectValue connectionDecoratorPlugins;
    public PluginValue dbdictionaryPlugin;
    public ObjectValue updateManagerPlugin;
    public ObjectValue schemaFactoryPlugin;
    public ObjectValue sqlFactoryPlugin;
    public ObjectValue mappingDefaultsPlugin;
    public PluginValue driverDataSourcePlugin;
    public MappingFactoryValue mappingFactoryPlugin;

    // used internally
    private String firstUser = null;
    private String firstPass = null;
    private DecoratingDataSource dataSource = null;
    private DecoratingDataSource dataSource2 = null;

    /**
     * Default constructor. Attempts to load default properties.
     */
    public JDBCConfigurationImpl() {
        this(true);
    }

    /**
     * Constructor.
     *
     * @param loadGlobals whether to attempt to load the global properties
     */
    public JDBCConfigurationImpl(boolean loadGlobals) {
        this(true, loadGlobals);
    }

    /**
     * Constructor.
     *
     * @param derivations whether to apply product derivations
     * @param loadGlobals whether to attempt to load the global properties
     */
    public JDBCConfigurationImpl(boolean derivations, boolean loadGlobals) {
        super(false, false);
        String[] aliases;

        schema = addString("jdbc.Schema");
        schemas = addStringList("jdbc.Schemas");

        transactionIsolation = addInt("jdbc.TransactionIsolation");
        aliases = new String[]{
            "default", String.valueOf(-1),
            "none", String.valueOf(Connection.TRANSACTION_NONE),
            "read-committed", String.valueOf
            (Connection.TRANSACTION_READ_COMMITTED),
            "read-uncommitted", String.valueOf
            (Connection.TRANSACTION_READ_UNCOMMITTED),
            "repeatable-read", String.valueOf
            (Connection.TRANSACTION_REPEATABLE_READ),
            "serializable", String.valueOf(Connection.TRANSACTION_SERIALIZABLE)
        };
        transactionIsolation.setAliases(aliases);
        transactionIsolation.setDefault(aliases[0]);
        transactionIsolation.set(-1);
        transactionIsolation.setAliasListComprehensive(true);

        resultSetType = addInt("jdbc.ResultSetType");
        aliases = new String[]{
            "forward-only", String.valueOf(ResultSet.TYPE_FORWARD_ONLY),
            "scroll-sensitive", String.valueOf
            (ResultSet.TYPE_SCROLL_SENSITIVE),
            "scroll-insensitive", String.valueOf
            (ResultSet.TYPE_SCROLL_INSENSITIVE),
        };
        resultSetType.setAliases(aliases);
        resultSetType.setDefault(aliases[0]);
        resultSetType.set(ResultSet.TYPE_FORWARD_ONLY);
        resultSetType.setAliasListComprehensive(true);

        fetchDirection = addInt("jdbc.FetchDirection");
        aliases = new String[]{
            "forward", String.valueOf(ResultSet.FETCH_FORWARD),
            "reverse", String.valueOf(ResultSet.FETCH_REVERSE),
            "unknown", String.valueOf(ResultSet.FETCH_UNKNOWN),
        };
        fetchDirection.setAliases(aliases);
        fetchDirection.setDefault(aliases[0]);
        fetchDirection.set(ResultSet.FETCH_FORWARD);
        fetchDirection.setAliasListComprehensive(true);

        eagerFetchMode = new FetchModeValue("jdbc.EagerFetchMode");
        eagerFetchMode.setDefault(FetchModeValue.EAGER_PARALLEL);
        eagerFetchMode.set(EagerFetchModes.EAGER_PARALLEL);
        addValue(eagerFetchMode);

        subclassFetchMode = new FetchModeValue("jdbc.SubclassFetchMode");
        subclassFetchMode.setDefault(FetchModeValue.EAGER_JOIN);
        subclassFetchMode.set(EagerFetchModes.EAGER_JOIN);
        addValue(subclassFetchMode);

        lrsSize = addInt("jdbc.LRSSize");
        aliases = new String[]{
            "query", String.valueOf(LRSSizes.SIZE_QUERY),
            "unknown", String.valueOf(LRSSizes.SIZE_UNKNOWN),
            "last", String.valueOf(LRSSizes.SIZE_LAST),
        };
        lrsSize.setAliases(aliases);
        lrsSize.setDefault(aliases[0]);
        lrsSize.set(LRSSizes.SIZE_QUERY);
        lrsSize.setAliasListComprehensive(true);

        synchronizeMappings = addString("jdbc.SynchronizeMappings");
        aliases = new String[]{ "false", null };
        synchronizeMappings.setAliases(aliases);
        synchronizeMappings.setDefault(aliases[0]);

        jdbcListenerPlugins = addPluginList("jdbc.JDBCListeners");
        jdbcListenerPlugins.setInstantiatingGetter("getJDBCListenerInstances");

        connectionDecoratorPlugins = addPluginList
            ("jdbc.ConnectionDecorators");
        connectionDecoratorPlugins.setInstantiatingGetter
            ("getConnectionDecoratorInstances");

        dbdictionaryPlugin = addPlugin("jdbc.DBDictionary", true);
        aliases = new String[]{
            "access", "org.apache.openjpa.jdbc.sql.AccessDictionary",
            "db2", "org.apache.openjpa.jdbc.sql.DB2Dictionary",
            "derby", "org.apache.openjpa.jdbc.sql.DerbyDictionary",
            "empress", "org.apache.openjpa.jdbc.sql.EmpressDictionary",
            "foxpro", "org.apache.openjpa.jdbc.sql.FoxProDictionary",
            "hsql", "org.apache.openjpa.jdbc.sql.HSQLDictionary",
            "informix", "org.apache.openjpa.jdbc.sql.InformixDictionary",
            "jdatastore", "org.apache.openjpa.jdbc.sql.JDataStoreDictionary",
            "mysql", "org.apache.openjpa.jdbc.sql.MySQLDictionary",
            "oracle", "org.apache.openjpa.jdbc.sql.OracleDictionary",
            "pointbase", "org.apache.openjpa.jdbc.sql.PointbaseDictionary",
            "postgres", "org.apache.openjpa.jdbc.sql.PostgresDictionary",
            "sqlserver", "org.apache.openjpa.jdbc.sql.SQLServerDictionary",
            "sybase", "org.apache.openjpa.jdbc.sql.SybaseDictionary",
        };
        dbdictionaryPlugin.setAliases(aliases);
        dbdictionaryPlugin.setInstantiatingGetter("getDBDictionaryInstance");

        updateManagerPlugin = addPlugin("jdbc.UpdateManager", true);
        aliases = new String[]{
            "default",
            "org.apache.openjpa.jdbc.kernel.OperationOrderUpdateManager",
            "operation-order",
            "org.apache.openjpa.jdbc.kernel.OperationOrderUpdateManager",
        };
        updateManagerPlugin.setAliases(aliases);
        updateManagerPlugin.setDefault(aliases[0]);
        updateManagerPlugin.setString(aliases[0]);
        updateManagerPlugin.setInstantiatingGetter("getUpdateManagerInstance");

        driverDataSourcePlugin = addPlugin("jdbc.DriverDataSource", false);
        aliases = new String[]{
            "simple", "org.apache.openjpa.jdbc.schema.SimpleDriverDataSource",
        };
        driverDataSourcePlugin.setAliases(aliases);
        driverDataSourcePlugin.setDefault(aliases[0]);
        driverDataSourcePlugin.setString(aliases[0]);

        schemaFactoryPlugin = addPlugin("jdbc.SchemaFactory", true);
        aliases = new String[]{
            "dynamic", "org.apache.openjpa.jdbc.schema.DynamicSchemaFactory",
            "native", "org.apache.openjpa.jdbc.schema.LazySchemaFactory",
            "file", "org.apache.openjpa.jdbc.schema.FileSchemaFactory",
            "table", "org.apache.openjpa.jdbc.schema.TableSchemaFactory",
            // deprecated alias
            "db", "org.apache.openjpa.jdbc.schema.TableSchemaFactory",
        };
        schemaFactoryPlugin.setAliases(aliases);
        schemaFactoryPlugin.setDefault(aliases[0]);
        schemaFactoryPlugin.setString(aliases[0]);
        schemaFactoryPlugin.setInstantiatingGetter("getSchemaFactoryInstance");

        sqlFactoryPlugin = addPlugin("jdbc.SQLFactory", true);
        aliases = new String[]{
            "default", "org.apache.openjpa.jdbc.sql.SQLFactoryImpl",
        };
        sqlFactoryPlugin.setAliases(aliases);
        sqlFactoryPlugin.setDefault(aliases[0]);
        sqlFactoryPlugin.setString(aliases[0]);
        sqlFactoryPlugin.setInstantiatingGetter("getSQLFactoryInstance");

        mappingFactoryPlugin = new MappingFactoryValue("jdbc.MappingFactory");
        addValue(mappingFactoryPlugin);

        mappingDefaultsPlugin = addPlugin("jdbc.MappingDefaults", true);
        aliases = new String[]{
            "default", "org.apache.openjpa.jdbc.meta.MappingDefaultsImpl",
        };
        mappingDefaultsPlugin.setAliases(aliases);
        mappingDefaultsPlugin.setDefault(aliases[0]);
        mappingDefaultsPlugin.setString(aliases[0]);
        mappingDefaultsPlugin.setInstantiatingGetter
            ("getMappingDefaultsInstance");

        // set up broker factory defaults
        brokerFactoryPlugin.setAlias("jdbc", JDBCBrokerFactory.class.getName());
        brokerFactoryPlugin.setDefault("jdbc");
        brokerFactoryPlugin.setString("jdbc");

        // set new default for mapping repos
        metaRepositoryPlugin.setAlias("default",
            "org.apache.openjpa.jdbc.meta.MappingRepository");
        metaRepositoryPlugin.setDefault("default");
        metaRepositoryPlugin.setString("default");

        // set new default for lock manager
        lockManagerPlugin.setAlias("pessimistic",
            PessimisticLockManager.class.getName());
        lockManagerPlugin.setDefault("pessimistic");
        lockManagerPlugin.setString("pessimistic");

        // native savepoint manager options
        savepointManagerPlugin.setAlias("jdbc",
            "org.apache.openjpa.jdbc.kernel.JDBC3SavepointManager");

        // set new aliases and defaults for sequence
        seqPlugin.setAliases(JDBCSeqValue.ALIASES);
        seqPlugin.setDefault(JDBCSeqValue.ALIASES[0]);
        seqPlugin.setString(JDBCSeqValue.ALIASES[0]);

        // this static initializer is to get past a weird
        // ClassCircularityError that happens only under IBM's
        // JDK 1.3.1 on Linux from within the JRun ClassLoader;
        // while exact causes are unknown, it is almost certainly
        // a bug in JRun, and we can get around it by forcing
        // Instruction.class to be loaded and initialized
        // before TypedInstruction.class
        try { serp.bytecode.lowlevel.Entry.class.getName(); } 
        catch (Throwable t) {}
        try { serp.bytecode.Instruction.class.getName(); } 
        catch (Throwable t) {}

        supportedOptions().add(OPTION_QUERY_SQL);
        supportedOptions().add(OPTION_JDBC_CONNECTION);
        supportedOptions().remove(OPTION_VALUE_INCREMENT);
        supportedOptions().remove(OPTION_NULL_CONTAINER);

        if (derivations)
            ProductDerivations.beforeConfigurationLoad(this);
        if (loadGlobals)
            loadGlobals();
    }

    /**
     * Copy constructor
     */
    public JDBCConfigurationImpl(JDBCConfiguration conf) {
        this(true, false);
        if (conf != null)
            fromProperties(conf.toProperties(false));
    }

    public void setSchema(String schema) {
        assertNotReadOnly();
        this.schema.setString(schema);
    }

    public String getSchema() {
        return schema.getString();
    }

    public void setSchemas(String schemas) {
        assertNotReadOnly();
        this.schemas.setString(schemas);
    }

    public String getSchemas() {
        return schemas.getString();
    }

    public void setSchemas(String[] schemas) {
        assertNotReadOnly();
        this.schemas.set(schemas);
    }

    public String[] getSchemasList() {
        return schemas.get();
    }

    public void setTransactionIsolation(String transactionIsolation) {
        assertNotReadOnly();
        this.transactionIsolation.setString(transactionIsolation);
    }

    public String getTransactionIsolation() {
        return transactionIsolation.getString();
    }

    public void setTransactionIsolation(int transactionIsolation) {
        assertNotReadOnly();
        this.transactionIsolation.set(transactionIsolation);
    }

    public int getTransactionIsolationConstant() {
        return transactionIsolation.get();
    }

    public void setResultSetType(String resultSetType) {
        assertNotReadOnly();
        this.resultSetType.setString(resultSetType);
    }

    public String getResultSetType() {
        return resultSetType.getString();
    }

    public void setResultSetType(int resultSetType) {
        assertNotReadOnly();
        this.resultSetType.set(resultSetType);
    }

    public int getResultSetTypeConstant() {
        return resultSetType.get();
    }

    public void setFetchDirection(String fetchDirection) {
        assertNotReadOnly();
        this.fetchDirection.setString(fetchDirection);
    }

    public String getFetchDirection() {
        return fetchDirection.getString();
    }

    public void setFetchDirection(int fetchDirection) {
        assertNotReadOnly();
        this.fetchDirection.set(fetchDirection);
    }

    public int getFetchDirectionConstant() {
        return fetchDirection.get();
    }

    public void setEagerFetchMode(String eagerFetchMode) {
        assertNotReadOnly();
        this.eagerFetchMode.setString(eagerFetchMode);
    }

    public String getEagerFetchMode() {
        return eagerFetchMode.getString();
    }

    public void setEagerFetchMode(int eagerFetchMode) {
        assertNotReadOnly();
        this.eagerFetchMode.set(eagerFetchMode);
    }

    public int getEagerFetchModeConstant() {
        return eagerFetchMode.get();
    }

    public void setSubclassFetchMode(String subclassFetchMode) {
        assertNotReadOnly();
        this.subclassFetchMode.setString(subclassFetchMode);
    }

    public String getSubclassFetchMode() {
        return subclassFetchMode.getString();
    }

    public void setSubclassFetchMode(int subclassFetchMode) {
        assertNotReadOnly();
        this.subclassFetchMode.set(subclassFetchMode);
    }

    public int getSubclassFetchModeConstant() {
        return subclassFetchMode.get();
    }

    public void setLRSSize(String lrsSize) {
        assertNotReadOnly();
        this.lrsSize.setString(lrsSize);
    }

    public String getLRSSize() {
        return lrsSize.getString();
    }

    public void setLRSSize(int lrsSize) {
        assertNotReadOnly();
        this.lrsSize.set(lrsSize);
    }

    public int getLRSSizeConstant() {
        return lrsSize.get();
    }

    public void setSynchronizeMappings(String synchronizeMappings) {
        assertNotReadOnly();
        this.synchronizeMappings.set(synchronizeMappings);
    }

    public String getSynchronizeMappings() {
        return synchronizeMappings.get();
    }

    public void setJDBCListeners(String jdbcListeners) {
        assertNotReadOnly();
        jdbcListenerPlugins.setString(jdbcListeners);
    }

    public String getJDBCListeners() {
        return jdbcListenerPlugins.getString();
    }

    public void setJDBCListeners(JDBCListener[] listeners) {
        assertNotReadOnly();
        jdbcListenerPlugins.set(listeners);
    }

    public JDBCListener[] getJDBCListenerInstances() {
        if (jdbcListenerPlugins.get() == null)
            jdbcListenerPlugins.instantiate(JDBCListener.class, this);
        return (JDBCListener[]) jdbcListenerPlugins.get();
    }

    public void setConnectionDecorators(String connectionDecorators) {
        assertNotReadOnly();
        connectionDecoratorPlugins.setString(connectionDecorators);
    }

    public String getConnectionDecorators() {
        return connectionDecoratorPlugins.getString();
    }

    public void setConnectionDecorators(ConnectionDecorator[] decorators) {
        assertNotReadOnly();
        connectionDecoratorPlugins.set(decorators);
    }

    public ConnectionDecorator[] getConnectionDecoratorInstances() {
        if (connectionDecoratorPlugins.get() == null) {
            connectionDecoratorPlugins.instantiate
                (ConnectionDecorator.class, this);
        }
        return (ConnectionDecorator[]) connectionDecoratorPlugins.get();
    }

    public void setDBDictionary(String dbdictionary) {
        assertNotReadOnly();
        dbdictionaryPlugin.setString(dbdictionary);
    }

    public String getDBDictionary() {
        return dbdictionaryPlugin.getString();
    }

    public void setDBDictionary(DBDictionary dbdictionary) {
        // we can't allow the dictionary to be set after the connection
        // factory, due to initialization issues
        if (connectionFactory.get() != null
            || connectionFactory2.get() != null)
            throw new IllegalStateException();

        assertNotReadOnly();
        dbdictionaryPlugin.set(dbdictionary);
    }

    public DBDictionary getDBDictionaryInstance() {
        // lock on connection factory name, since getting the connection
        // factory and getting the dictionary have to use the same locks to
        // prevent deadlock since they call each other
        DBDictionary dbdictionary = (DBDictionary) dbdictionaryPlugin.get();
        if (dbdictionary == null) {
            String clsName = dbdictionaryPlugin.getClassName();
            String props = dbdictionaryPlugin.getProperties();
            if (clsName != null && clsName.length() > 0) {
                dbdictionary = DBDictionaryFactory.newDBDictionary
                    (this, clsName, props);
            } else {
                // if the dictionary class isn't set, try to guess from
                // connection URL and driver name
                dbdictionary = DBDictionaryFactory.calculateDBDictionary
                    (this, getConnectionURL(), getConnectionDriverName(),
                        props);

                // if the url and driver name aren't enough, connect to
                // the DB and use the connection metadata
                if (dbdictionary == null) {
                    Log log = getLog(LOG_JDBC);
                    if (log.isInfoEnabled()) {
                        Localizer loc = Localizer.forPackage
                            (JDBCConfigurationImpl.class);
                        log.info(loc.get("connecting-for-dictionary"));
                    }

                    // use the base connection factory rather than the
                    // configured data source b/c the data source relies
                    // on passing the connection through the dictionary,
                    // resulting in infinite loops
                    DataSource ds = createConnectionFactory();
                    dbdictionary = DBDictionaryFactory.newDBDictionary
                        (this, getDataSource(null, ds), props);
                }
            }
            dbdictionaryPlugin.set(dbdictionary, true);
        }
        return dbdictionary;
    }

    public void setUpdateManager(String updateManager) {
        assertNotReadOnly();
        updateManagerPlugin.setString(updateManager);
    }

    public String getUpdateManager() {
        return updateManagerPlugin.getString();
    }

    public void setUpdateManager(UpdateManager updateManager) {
        assertNotReadOnly();
        updateManagerPlugin.set(updateManager);
    }

    public UpdateManager getUpdateManagerInstance() {
        if (updateManagerPlugin.get() == null)
            updateManagerPlugin.instantiate(UpdateManager.class, this);
        return (UpdateManager) updateManagerPlugin.get();
    }

    public void setDriverDataSource(String driverDataSource) {
        assertNotReadOnly();
        driverDataSourcePlugin.setString(driverDataSource);
    }

    public String getDriverDataSource() {
        return driverDataSourcePlugin.getString();
    }

    public DriverDataSource newDriverDataSourceInstance() {
        return (DriverDataSource) driverDataSourcePlugin.
            instantiate(DriverDataSource.class, this);
    }

    public void setSchemaFactory(String schemaFactory) {
        assertNotReadOnly();
        schemaFactoryPlugin.setString(schemaFactory);
    }

    public String getSchemaFactory() {
        return schemaFactoryPlugin.getString();
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        assertNotReadOnly();
        schemaFactoryPlugin.set(schemaFactory);
    }

    public SchemaFactory getSchemaFactoryInstance() {
        if (schemaFactoryPlugin.get() == null)
            schemaFactoryPlugin.instantiate(SchemaFactory.class, this);
        return (SchemaFactory) schemaFactoryPlugin.get();
    }

    public void setSQLFactory(String sqlFactory) {
        assertNotReadOnly();
        sqlFactoryPlugin.setString(sqlFactory);
    }

    public String getSQLFactory() {
        return sqlFactoryPlugin.getString();
    }

    public void setSQLFactory(SQLFactory sqlFactory) {
        assertNotReadOnly();
        sqlFactoryPlugin.set(sqlFactory);
    }

    public SQLFactory getSQLFactoryInstance() {
        if (sqlFactoryPlugin.get() == null)
            sqlFactoryPlugin.instantiate(SQLFactory.class, this);
        return (SQLFactory) sqlFactoryPlugin.get();
    }

    public String getMappingFactory() {
        return mappingFactoryPlugin.getString();
    }

    public void setMappingFactory(String mapping) {
        assertNotReadOnly();
        mappingFactoryPlugin.setString(mapping);
    }

    public MetaDataFactory newMetaDataFactoryInstance() {
        return mappingFactoryPlugin.instantiateMetaDataFactory(this,
            metaFactoryPlugin, getMapping());
    }

    public void setMappingDefaults(String mapping) {
        assertNotReadOnly();
        this.mappingDefaultsPlugin.setString(mapping);
    }

    public String getMappingDefaults() {
        return mappingDefaultsPlugin.getString();
    }

    public void setMappingDefaults(MappingDefaults mapping) {
        assertNotReadOnly();
        mappingDefaultsPlugin.set(mapping);
    }

    public MappingDefaults getMappingDefaultsInstance() {
        if (mappingDefaultsPlugin.get() == null)
            mappingDefaultsPlugin.instantiate(MappingDefaults.class, this);
        return (MappingDefaults) mappingDefaultsPlugin.get();
    }

    public MappingRepository getMappingRepositoryInstance() {
        return (MappingRepository) getMetaDataRepositoryInstance();
    }

    public MappingRepository newMappingRepositoryInstance() {
        return (MappingRepository) newMetaDataRepositoryInstance();
    }

    public BrokerImpl newBrokerInstance(String user, String pass) {
        BrokerImpl broker = super.newBrokerInstance(user, pass);

        // record first non-null broker user and pass in case no global settings
        if (broker != null && user != null && firstUser == null) {
            firstUser = user;
            firstPass = pass;
        }
        return broker;
    }

    public Object getConnectionFactory() {
        // override to configure data source
        if (dataSource == null) {
            DecoratingDataSource ds = createConnectionFactory();
            dataSource = DataSourceFactory.installDBDictionary
                (getDBDictionaryInstance(), ds, this, false);
        }
        return dataSource;
    }

    public void setConnectionFactory(Object factory) {
        // there's a lot of one-time initialization involved for
        // connection factories, so ignore resets
        if (factory == connectionFactory.get())
            return;

        // override to configure data source
        assertNotReadOnly();
        if (factory != null) {
            // need to ensure it is decorated before we set the dict
            DecoratingDataSource ds =
                setupConnectionFactory((DataSource) factory, false);
            dataSource = DataSourceFactory.installDBDictionary
                (getDBDictionaryInstance(), ds, this, false);
        } else
            connectionFactory.set(null);
    }

    /**
     * Ensure that the specified DataSource is decorated and set in the cache.
     */
    private DecoratingDataSource setupConnectionFactory(DataSource ds,
        boolean factory2) {
        if (ds == null)
            return null;

        DecoratingDataSource dds;
        if (ds instanceof DecoratingDataSource)
            dds = (DecoratingDataSource) ds;
        else
            dds = DataSourceFactory.decorateDataSource(ds, this, factory2);

        if (!factory2 && connectionFactory.get() != ds)
            connectionFactory.set(dds, true);
        else if (factory2 && connectionFactory2.get() != ds)
            connectionFactory2.set(dds, true);

        return dds;
    }

    public Object getConnectionFactory2() {
        // override to configure data source
        if (dataSource2 == null) {
            DataSource ds = (DataSource) connectionFactory2.get();
            if (ds == null) {
                // the driver name is always required, so if not specified,
                // then no connection factory 2
                String driver = getConnection2DriverName();
                if (driver != null && driver.length() > 0)
                    ds = DataSourceFactory.newDataSource(this, true);
            }
            if (ds != null) {
                DecoratingDataSource dds =
                    setupConnectionFactory(ds, true); // before dict
                dataSource2 = DataSourceFactory.installDBDictionary
                    (getDBDictionaryInstance(), dds, this, true);
            }
        }
        return dataSource2;
    }

    public void setConnectionFactory2(Object factory) {
        if (factory == connectionFactory2.get())
            return;

        // override to configure data source
        assertNotReadOnly();
        if (factory != null) {
            // need to ensure it is decorated before we set the dict
            DecoratingDataSource ds = setupConnectionFactory((DataSource)
                factory, true);
            dataSource2 = DataSourceFactory.installDBDictionary
                (getDBDictionaryInstance(), ds, this, true);
        } else
            connectionFactory2.set(null);
    }

    /**
     * Create the connection factory if necessary.
     */
    private DecoratingDataSource createConnectionFactory() {
        DataSource ds = (DataSource) connectionFactory.get();
        if (ds != null)
            return setupConnectionFactory(ds, false);

        ds = (DataSource) super.getConnectionFactory(); // JNDI lookup
        if (ds == null)
            ds = DataSourceFactory.newDataSource(this, false);

        return setupConnectionFactory(ds, false);
    }

    public DataSource getDataSource(StoreContext ctx) {
        return getDataSource(ctx, (DataSource) getConnectionFactory());
    }

    public DataSource getDataSource2(StoreContext ctx) {
        // if there is no connection factory 2, use the primary factory
        DataSource ds = (DataSource) getConnectionFactory2();
        if (ds == null)
            return getDataSource(ctx);

        // prefer the global connection 2 auth info if given
        String user = getConnection2UserName();
        String pass = getConnection2Password();
        if (user == null && pass == null) {
            // no global auth info; use the context if given, or the first
            // context if not
            if (ctx == null) {
                user = firstUser;
                pass = firstPass;
            } else {
                user = ctx.getConnectionUserName();
                pass = ctx.getConnectionPassword();
            }
        }
        return DataSourceFactory.defaultsDataSource(ds, user, pass);
    }

    /**
     * This version allows us to pass in which data source to wrap internally;
     * useful during initialization before the connection factory is
     * completely configured.
     */
    private DataSource getDataSource(StoreContext ctx, DataSource ds) {
        String user, pass;
        if (ctx == null) {
            // if no context, default to the global auth info, or the auth info
            // of the first context if none
            user = getConnectionUserName();
            if (user == null)
                user = firstUser;
            pass = getConnectionPassword();
            if (pass == null)
                pass = firstPass;
        } else {
            // use the context's auth info
            user = ctx.getConnectionUserName();
            pass = ctx.getConnectionPassword();
        }
        return DataSourceFactory.defaultsDataSource(ds, user, pass);
    }

    /**
     * Free the data sources.
     */
    public void close() {
        if (dataSource != null) {
            getDBDictionaryInstance().closeDataSource(dataSource);
            connectionFactory.set(null, true); // so super doesn't close it
        }
        if (dataSource2 != null) {
            getDBDictionaryInstance().closeDataSource(dataSource);
            connectionFactory2.set(null, true); // so super doesn't close it
        }
        super.close();
    }

    protected boolean isInvalidProperty(String propName) {
        if (super.isInvalidProperty(propName))
            return true;

        // handle openjpa.jdbc.SomeMisspelledProperty, but not
        // openjpa.someotherimplementation.SomeProperty
        String[] prefixes = ProductDerivations.getConfigurationPrefixes();
        for (int i = 0; i < prefixes.length; i++)
            if (propName.toLowerCase().startsWith(prefixes[i] + ".jdbc"))
                return true; 
        return false;
    }
}

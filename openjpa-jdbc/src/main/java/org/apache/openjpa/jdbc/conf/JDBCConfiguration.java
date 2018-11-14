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
package org.apache.openjpa.jdbc.conf;

import javax.sql.DataSource;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifierUtil;
import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.LRSSizes;
import org.apache.openjpa.jdbc.kernel.UpdateManager;
import org.apache.openjpa.jdbc.meta.MappingDefaults;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.DriverDataSource;
import org.apache.openjpa.jdbc.schema.SchemaFactory;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.SQLFactory;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.identifier.IdentifierUtil;
import org.apache.openjpa.lib.jdbc.ConnectionDecorator;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.meta.MetaDataFactory;

/**
 * Configuration that defines the properties necessary to configure
 * runtime and connect to a JDBC DataSource.
 *
 * @author Marc Prud'hommeaux
 */
public interface JDBCConfiguration
    extends OpenJPAConfiguration {

    /**
     * Name of the logger for SQL execution messages:
     * <code>openjpa.jdbc.SQL</code>.
     */
    String LOG_SQL = "openjpa.jdbc.SQL";

    /**
     * Name of the logger for additional jdbc messages:
     * <code>openjpa.jdbc.DIAG</code>.
     */
    String LOG_DIAG = "openjpa.jdbc.SQLDiag";

    /**
     * Name of the logger for JDBC-related messages:
     * <code>openjpa.jdbc.JDBC</code>.
     */
    String LOG_JDBC = "openjpa.jdbc.JDBC";

    /**
     * Name of the logger for schema-related messages:
     * <code>openjpa.jdbc.Schema</code>.
     */
    String LOG_SCHEMA = "openjpa.jdbc.Schema";

    /**
     * Default schema for unqualified tables.
     */
    String getSchema();

    /**
     * Default schema for unqualified tables.
     */
    void setSchema(String schema);

    /**
     * Comma-separated list of modifiable schemas for persistent instances.
     */
    String getSchemas();

    /**
     * Comma-separated list of modifiable schemas for persistent instances.
     */
    void setSchemas(String schemas);

    /**
     * Modificable schema components.
     */
    String[] getSchemasList();

    /**
     * Modifiable schema components.
     */
    void setSchemas(String[] schemas);

    /**
     * The transaction isolation level to use at the database level.
     * Possible values are:
     * <ul>
     * <li><code>default</code>: The JDBC driver's default isolation level.</li>
     * <li><code>none</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_NONE} level.</li>
     * <li><code>read-committed</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_READ_COMMITTED} level.</li>
     * <li><code>read-uncommitted</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_READ_UNCOMMITTED} level.</li>
     * <li><code>repeatable-read</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_REPEATABLE_READ} level.</li>
     * <li><code>serializable</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_SERIALIZABLE} level.</li>
     * </ul>
     */
    String getTransactionIsolation();

    /**
     * The transaction isolation level to use at the database level.
     * Possible values are:
     * <ul>
     * <li><code>default</code>: The JDBC driver's default isolation level.</li>
     * <li><code>none</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_NONE} level.</li>
     * <li><code>read-committed</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_READ_COMMITTED} level.</li>
     * <li><code>read-uncommitted</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_READ_UNCOMMITTED} level.</li>
     * <li><code>repeatable-read</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_REPEATABLE_READ} level.</li>
     * <li><code>serializable</code>: The standard JDBC
     * {@link java.sql.Connection#TRANSACTION_SERIALIZABLE} level.</li>
     * </ul>
     */
    void setTransactionIsolation(String level);

    /**
     * Return the proper transaction isolation level constant from
     * {@link java.sql.Connection}, or -1 for the default level.
     */
    int getTransactionIsolationConstant();

    /**
     * Set the proper transaction isolation level constant from
     * {@link java.sql.Connection}, or -1 for the default level.
     */
    void setTransactionIsolation(int level);

    /**
     * The JDBC result set type. Defaults to <code>forward-only</code>.
     * <ul>
     * <li><code>forward-only</code>: The standard JDBC
     * {@link java.sql.ResultSet#TYPE_FORWARD_ONLY} type.</li>
     * <li><code>scroll-sensitive</code>: The standard JDBC
     * {@link java.sql.ResultSet#TYPE_SCROLL_SENSITIVE} type.</li>
     * <li><code>scroll-insensitive</code>: The standard JDBC
     * {@link java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE} type.</li>
     * </ul>
     */
    String getResultSetType();

    /**
     * Return the result set constant for the result set type.
     */
    int getResultSetTypeConstant();

    /**
     * The JDBC result set type. Defaults to <code>forward-only</code>.
     * <ul>
     * <li><code>forward-only</code>: The standard JDBC
     * {@link java.sql.ResultSet#TYPE_FORWARD_ONLY} type.</li>
     * <li><code>scroll-sensitive</code>: The standard JDBC
     * {@link java.sql.ResultSet#TYPE_SCROLL_SENSITIVE} type.</li>
     * <li><code>scroll-insensitive</code>: The standard JDBC
     * {@link java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE} type.</li>
     * </ul>
     */
    void setResultSetType(String type);

    /**
     * Set the result set constant type.
     */
    void setResultSetType(int type);

    /**
     * The JDBC fetch direction. Defaults to <code>forward</code>.
     * <ul>
     * <li><code>forward</code>: The standard JDBC
     * {@link java.sql.ResultSet#FETCH_FORWARD} direction.</li>
     * <li><code>reverse</code>: The standard JDBC
     * {@link java.sql.ResultSet#FETCH_REVERSE} direction.</li>
     * <li><code>unknown</code>: The standard JDBC
     * {@link java.sql.ResultSet#FETCH_UNKNOWN} direction.</li>
     * </ul>
     */
    String getFetchDirection();

    /**
     * Return the result set constant for the fetch direction.
     */
    int getFetchDirectionConstant();

    /**
     * The JDBC fetch direction. Defaults to <code>forward</code>.
     * <ul>
     * <li><code>forward</code>: The standard JDBC
     * {@link java.sql.ResultSet#FETCH_FORWARD} direction.</li>
     * <li><code>reverse</code>: The standard JDBC
     * {@link java.sql.ResultSet#FETCH_REVERSE} direction.</li>
     * <li><code>unknown</code>: The standard JDBC
     * {@link java.sql.ResultSet#FETCH_UNKNOWN} direction.</li>
     * </ul>
     */
    void setFetchDirection(String direction);

    /**
     * Set the result set fetch direction constant.
     */
    void setFetchDirection(int direction);

    /**
     * Specifies the default eager fetch mode to use. Defaults to
     * <code>parallel</code> unless the query is by-oid. Possible values are:
     * <ul>
     * <li><code>none</code>: When querying for an object, do not try to
     * select for related objects at the same time.</li>
     * <li><code>join</code>: When querying for objects, also select for
     * 1-1 relations in the configured fetch groups using joins.</li>
     * <li><code>parallel</code>: When querying for objects, also select for
     * both 1-1 relations using joins and to-many relations using batched
     * selects.</li>
     * </li>
     * </ul>
     *
     * @since 0.3.0
     */
    String getEagerFetchMode();

    /**
     * Specifies the default eager fetch mode to use. Defaults to
     * <code>parallel</code> unless the query is by-oid. Possible values are:
     * <ul>
     * <li><code>none</code>: When querying for an object, do not try to
     * select for related objects at the same time.</li>
     * <li><code>join</code>: When querying for objects, also select for
     * 1-1 relations in the configured fetch groups using joins.</li>
     * <li><code>parallel</code>: When querying for objects, also select for
     * both 1-1 relations using joins and to-many relations using batched
     * selects.</li>
     * </ul>
     */
    void setEagerFetchMode(String mode);

    /**
     * Return the eager fetch mode as one of the following symbolic constants:
     * <ul>
     * <li>{@link EagerFetchModes#EAGER_NONE}</li>
     * <li>{@link EagerFetchModes#EAGER_JOIN}</li>
     * <li>{@link EagerFetchModes#EAGER_PARALLEL}</li>
     * </ul>
     *
     * @since 0.3.0
     */
    int getEagerFetchModeConstant();

    /**
     * Set the eager fetch mode as one of the following symbolic constants:
     * <ul>
     * <li>{@link EagerFetchModes#EAGER_NONE}</li>
     * <li>{@link EagerFetchModes#EAGER_JOIN}</li>
     * <li>{@link EagerFetchModes#EAGER_PARALLEL}</li>
     * </ul>
     *
     * @since 0.3.0
     */
    void setEagerFetchMode(int eagerFetchMode);

    /**
     * Specifies the default subclass fetch mode to use. Defaults to
     * <code>join</code> unless the query is by-oid. Possible values are:
     * <ul>
     * <li><code>none</code>: Only select base class data.</li>
     * <li><code>join</code>: Select both base class and all possible subclass
     * data using joins.</li>
     * <li><code>parallel</code>: Select for each possible subclass
     * separately.</li>
     * </ul>
     *
     * @since 0.3.2
     */
    String getSubclassFetchMode();

    /**
     * Specifies the default subclass fetch mode to use. Defaults to
     * <code>join</code> unless the query is by-oid. Possible values are:
     * <ul>
     * <li><code>none</code>: Only select base class data.</li>
     * <li><code>join</code>: Select both base class and all possible subclass
     * data using joins.</li>
     * <li><code>parallel</code>: Select for each possible subclass
     * separately.</li>
     * </ul>
     *
     * @since 0.3.2
     */
    void setSubclassFetchMode(String mode);

    /**
     * Return the subclass fetch mode as one of the following symbolic
     * constants:
     * <ul>
     * <li>{@link EagerFetchModes#EAGER_NONE}</li>
     * <li>{@link EagerFetchModes#EAGER_JOIN}</li>
     * <li>{@link EagerFetchModes#EAGER_PARALLEL}</li>
     * </ul>
     *
     * @since 0.3.2
     */
    int getSubclassFetchModeConstant();

    /**
     * Set the subclass fetch mode as one of the following symbolic constants:
     * <ul>
     * <li>{@link EagerFetchModes#EAGER_NONE}</li>
     * <li>{@link EagerFetchModes#EAGER_JOIN}</li>
     * <li>{@link EagerFetchModes#EAGER_PARALLEL}</li>
     * </ul>
     *
     * @since 0.3.2
     */
    void setSubclassFetchMode(int subclassFetchMode);

    /**
     * How to obtain the size of large result sets. Defaults to
     * <code>unknown</code>.
     * <ul>
     * <li><code>unknown</code>: Do not attempt to calculate the size of
     * large result sets; return {@link Integer#MAX_VALUE}.</li>
     * <li><code>last</code>: For result sets that support random access,
     * calculate the size using {@link java.sql.ResultSet#last}.</li>
     * <li><code>query</code>: Use a separate COUNT query to calculate the
     * size of the results.</li>
     * </ul>
     */
    String getLRSSize();

    /**
     * Return the {@link LRSSizes} constant for the large result set size
     * setting.
     */
    int getLRSSizeConstant();

    /**
     * How to obtain the size of large result sets. Defaults to
     * <code>unknown</code>.
     * <ul>
     * <li><code>unknown</code>: Do not attempt to calculate the size of
     * large result sets; return {@link Integer#MAX_VALUE}.</li>
     * <li><code>last</code>: For result sets that support random access,
     * calculate the size using {@link java.sql.ResultSet#last}.</li>
     * <li><code>query</code>: Use a separate COUNT query to calculate the
     * size of the results.</li>
     * </ul>
     */
    void setLRSSize(String lrsSize);

    /**
     * Set the fetch configuration large result set size constant.
     */
    void setLRSSize(int size);

    /**
     * Whether OpenJPA should try to automatically refresh O/R mapping
     * information and the database schema.
     */
    String getSynchronizeMappings();

    /**
     * Whether OpenJPA should try to automatically refresh O/R mapping
     * information and the database schema.
     */
    void setSynchronizeMappings(String synchronizeMappings);

    /**
     * A comma-separated list of the {@link JDBCListener} plugins for
     * listening to {@link JDBCEvent}s.
     */
    String getJDBCListeners();

    /**
     * A comma-separated list of the {@link JDBCListener} plugins for
     * listening to {@link JDBCEvent}s.
     */
    void setJDBCListeners(String jdbcListeners);

    /**
     * The {@link JDBCListener}s to use.
     */
    JDBCListener[] getJDBCListenerInstances();

    /**
     * The {@link JDBCListener}s to use.
     */
    void setJDBCListeners(JDBCListener[] jdbcListeners);

    /**
     * A comma-separated list of the {@link ConnectionDecorator} for adding
     * functionality to JDBC connections.
     */
    String getConnectionDecorators();

    /**
     * A comma-separated list of the {@link ConnectionDecorator} for
     * adding functionality to JDBC connections.
     */
    void setConnectionDecorators(String decorators);

    /**
     * The {@link ConnectionDecorator}s to use.
     */
    ConnectionDecorator[] getConnectionDecoratorInstances();

    /**
     * The {@link ConnectionDecorator}s to use.
     */
    void setConnectionDecorators(ConnectionDecorator[] decorators);

    /**
     * The {@link DBDictionary} to use to define the RDBMS SQL information.
     */
    String getDBDictionary();

    /**
     * The {@link DBDictionary} to use to define the RDBMS SQL information.
     */
    void setDBDictionary(String dbdictionary);

    /**
     * The {@link DBDictionary} to use.
     */
    DBDictionary getDBDictionaryInstance();

    /**
     * The {@link DBDictionary} to use.
     */
    void setDBDictionary(DBDictionary dbdictionary);

    /**
     * The {@link UpdateManager} to use for managing SQL updates.
     */
    String getUpdateManager();

    /**
     * The {@link UpdateManager} to use for managing SQL updates.
     */
    void setUpdateManager(String updateManager);

    /**
     * The {@link UpdateManager} for runtime data store interaction.
     */
    UpdateManager getUpdateManagerInstance();

    /**
     * The {@link UpdateManager} for runtime data store interaction.
     */
    void setUpdateManager(UpdateManager updateManager);

    /**
     * The {@link DriverDataSource} to use for creating a {@link DataSource}
     * from a JDBC {@link Driver}.
     */
    String getDriverDataSource();

    /**
     * The {@link DriverDataSource} to use for creating a {@link DataSource}
     * from a JDBC {@link Driver}.
     */
    void setDriverDataSource(String driverDataSource);

    /**
     * Create an instance of the {@link DriverDataSource} to use
     * for creating a {@link DataSource} from a JDBC {@link Driver}.
     */
    DataSource newDriverDataSourceInstance();

    /**
     * The plugin string for the {@link SchemaFactory} to use to provide
     * schema information during system initialization.
     */
    String getSchemaFactory();

    /**
     * The plugin string for the {@link SchemaFactory} to use to provide
     * schema information during system initialization.
     */
    void setSchemaFactory(String schemaFactory);

    /**
     * The {@link SchemaFactory} to use for schema information.
     */
    SchemaFactory getSchemaFactoryInstance();

    /**
     * The {@link SchemaFactory} to use for schema information.
     */
    void setSchemaFactory(SchemaFactory schemaFactory);

    /**
     * The SQL factory to use for SQL constructs.
     */
    String getSQLFactory();

    /**
     * The SQL factory to use for SQL constructs.
     */
    SQLFactory getSQLFactoryInstance();

    /**
     * The SQL factory to use for SQL constructs.
     */
    void setSQLFactory(String sqlFactory);

    /**
     * The SQL factory to use for SQL constructs.
     */
    void setSQLFactory(SQLFactory sqlFactory);

    /**
     * A plugin string describing the {@link MetaDataFactory} to use for
     * loading and storing object-relational mapping data.
     */
    String getMappingFactory();

    /**
     * A plugin string describing the {@link MetaDataFactory} to use for
     * loading and storing object-relational mapping data.
     */
    void setMappingFactory(String mappingFactory);

    /**
     * A plugin string describing the {@link MappingDefaults} to use.
     *
     * @since 0.4.0
     */
    String getMappingDefaults();

    /**
     * A plugin string describing the {@link MappingDefaults} to use.
     *
     * @since 0.4.0
     */
    void setMappingDefaults(String map);

    /**
     * The {@link MappingDefaults} to use with a repository.
     *
     * @since 0.4.0
     */
    MappingDefaults getMappingDefaultsInstance();

    /**
     * The {@link MappingDefaults} to use with a repository.
     *
     * @since 0.4.0
     */
    void setMappingDefaults(MappingDefaults map);

    /**
     * Return the mapping repository. Convenience method to cast from
     * the internal metadata repository.
     */
    MappingRepository getMappingRepositoryInstance();

    /**
     * Return a new empty mapping repository of the configured type.
     * Convenience method to cast from metadata repository.
     */
    MappingRepository newMappingRepositoryInstance();

    /**
     * Return the primary data source to use. The data source will
     * automatically use the given context's user name and password on calls
     * to {@link DataSource#getConnection}. If the given context is null, the
     * data source will use the configuration's default connection user name
     * and password. If those too are null and the first context has been
     * obtained already, then the user name and password for that context
     * will be used, as we know they represent a valid combination. This
     * method avoids casting the result of
     * {@link OpenJPAConfiguration#getConnectionFactory}, and avoids having to
     * pass in the user name and password to obtain connections.
     */
    DataSource getDataSource(StoreContext ctx);

    /**
     * Return the non-enlisted data source to use. If there is a valid
     * non-xa connection factory configured, then it will be returned. Its
     * default user name and password on calls to
     * {@link DataSource#getConnection} will be the specified connection 2
     * user name and password. If those are null and the given context is
     * non-null, its user name password will be used instead. If the context
     * is null too, then the user name and password used to retrieve the first
     * context will be used. If there is no second connection factory the
     * primary connection factory is used.
     *
     * @see #getDataSource
     */
    DataSource getDataSource2(StoreContext ctx);

    /**
     * Gets the String constant that matches the {@link IdentifierUtil}
     * @return String-based name of the {@link IdentifierUtil}
     */
    String getIdentifierUtil();

    /**
     * Gets the {@link DBIdentifierUtil}
     * @return DBIdentifierUtil
     */
    DBIdentifierUtil getIdentifierUtilInstance();

    /**
     * Sets the {@link DBIdentifierUtil}
     * @param util instance of the identifier utility
     */
    void setIdentifierUtil(DBIdentifierUtil util);

}

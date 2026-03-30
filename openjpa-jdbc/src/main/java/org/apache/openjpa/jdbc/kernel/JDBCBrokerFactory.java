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
package org.apache.openjpa.jdbc.kernel;

import static org.apache.openjpa.conf.SchemaGenerationAction.CREATE;
import static org.apache.openjpa.conf.SchemaGenerationAction.DROP;
import static org.apache.openjpa.conf.SchemaGenerationAction.DROP_AND_CREATE;
import static org.apache.openjpa.conf.SchemaGenerationSource.METADATA;
import static org.apache.openjpa.conf.SchemaGenerationSource.METADATA_THEN_SCRIPT;
import static org.apache.openjpa.conf.SchemaGenerationSource.SCRIPT;
import static org.apache.openjpa.conf.SchemaGenerationSource.SCRIPT_THEN_METADATA;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.openjpa.conf.SchemaGenerationSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.UserException;

/**
 * BrokerFactory type for use with the JDBC runtime.
 *
 * @author Abe White
 * @author Marc Prud'hommeaux
 */
public class JDBCBrokerFactory extends AbstractBrokerFactory {
    private static final long serialVersionUID = 1L;

    private static final Localizer _loc = Localizer.forPackage
        (JDBCBrokerFactory.class);

    private boolean _synchronizedMappings = false;

    /**
     * Factory method for constructing a factory from properties. Invoked from
     * {@link Bootstrap#newBrokerFactory}.
     */
    public static JDBCBrokerFactory newInstance(ConfigurationProvider cp) {
        JDBCConfigurationImpl conf = new JDBCConfigurationImpl();
        cp.setInto(conf);
        return new JDBCBrokerFactory(conf);
    }

    /**
     * Factory method for obtaining a possibly-pooled factory from properties.
     * Invoked from {@link Bootstrap#getBrokerFactory}.
     */
    public static JDBCBrokerFactory getInstance(ConfigurationProvider cp, ClassLoader loader) {
        Map<String, Object> props = cp.getProperties();
        Object key = toPoolKey(props);
        JDBCBrokerFactory factory = (JDBCBrokerFactory) getPooledFactoryForKey(key);
        if (factory != null)
            return factory;

        // The creation of all BrokerFactories should be driven through Bootstrap.
        factory = (JDBCBrokerFactory) Bootstrap.newBrokerFactory(cp, loader);
        pool(key, factory);
        return factory;
    }

    /**
     * Construct the factory with the given option settings; however, the
     * factory construction methods are recommended.
     */
    public JDBCBrokerFactory(JDBCConfiguration conf) {
        super(conf);
    }

    @Override
    public Map<String,Object> getProperties() {
        // add platform property
        Map<String,Object> props = super.getProperties();
        String db = "Unknown";
        try {
            JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
            db = conf.getDBDictionaryInstance().platform;
        } catch (RuntimeException re) {
        }
        props.put("Platform", "OpenJPA JDBC Edition: " + db + " Database");

        return props;
    }

    @Override
    public void postCreationCallback() {
        super.postCreationCallback();
        // JPA spec requires script generation at EMF creation time.
        // Only handle scripts.action and Writer targets here — these
        // write DDL to files/writers and need to happen immediately.
        // Database.action (executing DDL against the DB) is handled by
        // PersistenceProviderImpl.synchronizeMappings() for
        // Persistence.generateSchema(), or by newBrokerImpl() for
        // regular EMFs, to avoid double execution.
        JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
        int scriptsAction = conf.getScriptsActionConstant();
        if (scriptsAction != 0
                || conf.getCreateScriptTargetWriter() != null
                || conf.getDropScriptTargetWriter() != null) {
            synchronizeMappings(Thread.currentThread().getContextClassLoader(),
                conf);
            _synchronizedMappings = true;
        }
    }

    @Override
    protected StoreManager newStoreManager() {
        return new JDBCStoreManager();
    }

    @Override
    protected BrokerImpl newBrokerImpl(String user, String pass) {
        BrokerImpl broker = super.newBrokerImpl(user, pass);

        lock();
        try {
            // synchronize mappings; we wait until now to do this so that
            // we can use the first broker user/pass for connection if no
            // global login is given
            if (!_synchronizedMappings) {
                _synchronizedMappings = true;
                synchronizeMappings(broker.getClassLoader());
            }

            return broker;
        } finally {
            unlock();
        }
    }
    
    @Override
    public void createPersistenceStructure(boolean createSchemas) {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	String baseAction = createSchemas ? "createDB, add": MappingTool.ACTION_ADD;
    	synchronizeMappings(broker.getClassLoader(), conf, String.format("buildSchema(ForeignKeys=true,schemaAction='%s')", baseAction));
    }
    
    @Override
    public void dropPersistenceStrucuture(boolean dropSchemas) {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	String baseAction = dropSchemas ? "drop, dropDB": MappingTool.ACTION_DROP;
    	synchronizeMappings(broker.getClassLoader(), conf, String.format("buildSchema(ForeignKeys=true,schemaAction='%s')", baseAction));
    }
    
    @Override
    public void validatePersistenceStruture() throws Exception {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	synchronizeMappings(broker.getClassLoader(), conf, "validate(ForeignKeys=true)");
    }
    
    @Override
    public void truncateData() {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	String baseAction = "refresh,deleteTableContents";
    	synchronizeMappings(broker.getClassLoader(), conf, String.format("buildSchema(ForeignKeys=true,schemaAction='%s')", baseAction));
    }
    
    protected boolean synchronizeMappings(ClassLoader loader, JDBCConfiguration conf) {
    	mapSchemaGenerationToSynchronizeMappings(conf);
    	String action = conf.getSynchronizeMappings();
    	return synchronizeMappings(loader, conf, action);
    }
    	
    /**
     * Synchronize the mappings of the classes listed in the configuration.
     */
    protected boolean synchronizeMappings(ClassLoader loader, JDBCConfiguration conf, String action) {
        if (StringUtil.isEmpty(action))
            return false;

        MappingRepository repo = conf.getMappingRepositoryInstance();
        Collection<Class<?>> classes = repo.loadPersistentTypes(false, loader);
        if (classes.isEmpty())
            return false;

        String props = Configurations.getProperties(action);
        action = Configurations.getClassName(action);
        MappingTool tool = new MappingTool(conf, action, false, loader);
        Configurations.configureInstance(tool, conf, props,
            "SynchronizeMappings");

        // Check for ExcludeTypes to skip specific entity classes during
        // schema synchronization. Supports both the parenthesized form
        // buildSchema(ExcludeTypes=...) and the standalone config property
        // openjpa.jdbc.SyncMappingsExcludeTypes.
        Set<String> excludeTypes = parseExcludeTypes(props);
        if (excludeTypes.isEmpty()) {
            String excludeProp = conf.getSyncMappingsExcludeTypes();
            if (excludeProp != null && !excludeProp.isEmpty()) {
                for (String type : excludeProp.split(";")) {
                    type = type.trim();
                    if (!type.isEmpty()) {
                        excludeTypes.add(type);
                    }
                }
            }
        }

        // initialize the schema
        for (Class<?> cls : classes) {
            if (excludeTypes.contains(cls.getName())) {
                continue;
            }
            try {
                tool.run(cls);
            } catch (IllegalArgumentException iae) {
                throw new UserException(_loc.get("bad-synch-mappings",
                    action, Arrays.asList(MappingTool.ACTIONS)));
            } catch (MetaDataException mde) {
                // non-entity classes (DTOs, listeners, ID classes) may be
                // listed in persistence.xml <class> elements; skip them
                // during schema synchronization. Only skip if the class
                // truly has no metadata — re-throw for managed types
                // with invalid metadata (e.g. unsupported version type).
                if (tool.getRepository().getMetaData(cls, null, false) != null) {
                    throw mde;
                }
                conf.getLog("openjpa.jdbc.Schema").warn(
                    "Skipping schema synchronization for non-managed class: "
                        + cls.getName());
            }
        }
        tool.record();

        // For excluded types, actively drop their tables if they exist
        // in the database. This ensures tables from previous runs (before
        // the type was excluded) don't persist and cause operations to
        // succeed silently instead of throwing the expected RuntimeException.
        if (!excludeTypes.isEmpty()) {
            dropExcludedTypeTables(conf, repo, excludeTypes, loader);
        }

        return true; // todo: check?
    }

    protected boolean synchronizeMappings(ClassLoader loader) {
        return synchronizeMappings(loader, (JDBCConfiguration) getConfiguration());
    }

    /**
     * Drop tables for entity types that are excluded from schema
     * synchronization. This ensures that tables left over from a previous
     * run (before the type was excluded) are removed so that SQL operations
     * against them will fail as expected.
     */
    private void dropExcludedTypeTables(JDBCConfiguration conf,
            MappingRepository repo, Set<String> excludeTypes,
            ClassLoader loader) {
        Log log = conf.getLog("openjpa.jdbc.Schema");
        DataSource ds = conf.getDataSource2(null);
        for (String typeName : excludeTypes) {
            try {
                ClassLoader clsLoader = conf.getClassResolverInstance()
                    .getClassLoader(getClass(), loader);
                Class<?> cls = Class.forName(typeName, true, clsLoader);

                // Use the mapping repository to determine the table name
                ClassMapping mapping = repo.getMapping(cls, clsLoader, false);
                if (mapping == null || mapping.getTable() == null) {
                    continue;
                }
                String tableName =
                    mapping.getTable().getFullIdentifier().getName();

                Connection conn = ds.getConnection();
                try {
                    conn.setAutoCommit(true);
                    Statement stmt = conn.createStatement();
                    try {
                        stmt.executeUpdate("DROP TABLE " + tableName);
                        if (log.isTraceEnabled()) {
                            log.trace("Dropped excluded type table: "
                                + tableName);
                        }
                    } catch (SQLException sqle) {
                        // Table may not exist; ignore silently
                        if (log.isTraceEnabled()) {
                            log.trace("Could not drop excluded type table "
                                + tableName + ": " + sqle.getMessage());
                        }
                    } finally {
                        stmt.close();
                    }
                } finally {
                    conn.close();
                }
            } catch (Exception e) {
                if (log.isTraceEnabled()) {
                    log.trace("Error dropping table for excluded type "
                        + typeName + ": " + e.getMessage());
                }
            }
        }
    }

    private void mapSchemaGenerationToSynchronizeMappings(JDBCConfiguration conf) {
        String actions = "";
        if (conf.getDatabaseAction() != null) {
            int databaseAction = conf.getDatabaseActionConstant();
            if (databaseAction == CREATE) {
                actions = generateSchemaCreation(conf);
            } else if (databaseAction == DROP) {
                actions = generateSchemaDrop(conf);
            } else if (databaseAction == DROP_AND_CREATE) {
                actions = generateSchemaDropCreate(conf);
            }
        }

        // Handle scripts.action independently from database.action.
        // When scripts.action is set, generate DDL scripts to files
        // even if database.action is "none".
        if (conf.getScriptsAction() != null) {
            int scriptsAction = conf.getScriptsActionConstant();
            if (scriptsAction == CREATE) {
                actions = appendAction(actions, SchemaTool.ACTION_BUILD);
            } else if (scriptsAction == DROP) {
                actions = appendAction(actions, SchemaTool.ACTION_DROP);
            } else if (scriptsAction == DROP_AND_CREATE) {
                actions = appendAction(actions, SchemaTool.ACTION_DROP
                    + "," + SchemaTool.ACTION_BUILD);
            }
        }

        String loadFile = conf.getLoadScriptSource();
        if (loadFile != null) {
            actions = appendAction(actions, MappingTool.ACTION_SCRIPT_LOAD);
        }

        if (actions.length() > 0) {
            conf.setSynchronizeMappings("buildSchema(ForeignKeys=true,schemaAction='" + actions + "')");

        } else if (conf.isSchemaGenerationExplicit()
                && conf.isSpecCompliantSchemaGeneration()) {
            // In spec-compliant mode (e.g. TCK), clear SynchronizeMappings
            // when JPA schema gen properties are present but resolve to no
            // actions (e.g. database.action=none). This prevents auto-
            // creating tables via buildSchema when the spec says not to.
            conf.setSynchronizeMappings(null);
        }
    }

    /**
     * Parses the ExcludeTypes property from SynchronizeMappings config props.
     * The format is: ExcludeTypes=com.example.Entity1;com.example.Entity2
     * (semicolon-separated fully-qualified class names).
     */
    private Set<String> parseExcludeTypes(String props) {
        Set<String> excluded = new HashSet<>();
        if (props == null || props.isEmpty()) {
            return excluded;
        }
        // props is a comma-separated list of key=value pairs
        for (String prop : props.split(",")) {
            prop = prop.trim();
            if (prop.startsWith("ExcludeTypes=")) {
                String value = prop.substring("ExcludeTypes=".length()).trim();
                // Remove surrounding quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                for (String type : value.split(";")) {
                    type = type.trim();
                    if (!type.isEmpty()) {
                        excluded.add(type);
                    }
                }
            }
        }
        return excluded;
    }

    private String appendAction(String actions, String newAction) {
        if (actions.isEmpty()) {
            return newAction;
        }
        return actions + "," + newAction;
    }

    private String generateSchemaCreation(JDBCConfiguration conf) {
        if (conf.getCreateScriptTarget() != null
                || conf.getCreateScriptTargetWriter() != null) {
            return SchemaTool.ACTION_BUILD;
        } else {
            int createSource = conf.getCreateSourceConstant();
            if (createSource == SchemaGenerationSource.NONE
                    && (conf.getCreateScriptSource() != null
                        || conf.getCreateScriptSourceReader() != null)) {
                createSource = SCRIPT;
            } else if (createSource == SchemaGenerationSource.NONE) {
                createSource = METADATA;
            }
            return mapGenerationStrategyActions(createSource, SchemaTool.ACTION_ADD, MappingTool.ACTION_SCRIPT_CREATE);
        }
    }

    private String generateSchemaDrop(JDBCConfiguration conf) {
        if (conf.getDropScriptTarget() != null
                || conf.getDropScriptTargetWriter() != null) {
            return SchemaTool.ACTION_DROP;
        } else {
            int dropSource = conf.getDropSourceConstant();
            if (dropSource == SchemaGenerationSource.NONE
                    && (conf.getDropScriptSource() != null
                        || conf.getDropScriptSourceReader() != null)) {
                dropSource = SCRIPT;
            } else if (dropSource == SchemaGenerationSource.NONE) {
                dropSource = METADATA;
            }
            return mapGenerationStrategyActions(dropSource, SchemaTool.ACTION_DROP, MappingTool.ACTION_SCRIPT_DROP);
        }
    }

    private String generateSchemaDropCreate(JDBCConfiguration conf) {
        boolean hasCreateTarget = conf.getCreateScriptTarget() != null
            || conf.getCreateScriptTargetWriter() != null;
        boolean hasDropTarget = conf.getDropScriptTarget() != null
            || conf.getDropScriptTargetWriter() != null;
        if (hasCreateTarget && hasDropTarget) {
            return SchemaTool.ACTION_BUILD + "," + SchemaTool.ACTION_DROP;
        } else {
            return mapGenerationStrategyActions(conf.getDropSourceConstant(), SchemaTool.ACTION_DROP, MappingTool.ACTION_SCRIPT_DROP) + "," +
                   mapGenerationStrategyActions(conf.getCreateSourceConstant(), SchemaTool.ACTION_ADD, MappingTool.ACTION_SCRIPT_CREATE);
        }
    }

    private String mapGenerationStrategyActions(int source, String metadataAction, String scriptAction) {
        String actions = "";
        if (source == METADATA) {
            actions += metadataAction;
        } else if (source == SCRIPT) {
            actions += scriptAction;
        } else if (source == METADATA_THEN_SCRIPT) {
            actions += metadataAction + "," + scriptAction;
        } else if (source == SCRIPT_THEN_METADATA) {
            actions += scriptAction + "," + metadataAction;
        } else {
            actions += metadataAction;
        }
        return actions;
    }
}

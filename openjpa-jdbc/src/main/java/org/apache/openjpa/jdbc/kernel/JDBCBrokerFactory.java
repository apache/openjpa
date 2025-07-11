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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.conf.SchemaGenerationSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.util.OpenJPAException;
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
    	synchronizeMappings(broker.getClassLoader(), conf, String.format("buildSchema(ForeignKeys=true,SchemaAction='%s')", baseAction));
    }
    
    @Override
    public void dropPersistenceStrucuture(boolean dropSchemas) {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	String baseAction = dropSchemas ? "drop, dropDB": MappingTool.ACTION_DROP;
    	synchronizeMappings(broker.getClassLoader(), conf, String.format("buildSchema(ForeignKeys=true,SchemaAction='%s')", baseAction));
    }
    
    @Override
    public void validatePersistenceStruture() throws OpenJPAException {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	synchronizeMappings(broker.getClassLoader(), conf, "buildSchema(ForeignKeys=true,SchemaAction='retain')");
    }
    
    @Override
    public void truncateData() {
    	JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
    	Broker broker = super.newBrokerImpl(conf.getConnectionUserName(), conf.getConnectionPassword());
    	String baseAction = "refresh,deleteTableContents";
    	synchronizeMappings(broker.getClassLoader(), conf, String.format("buildSchema(ForeignKeys=true,SchemaAction='%s')", baseAction));
    }
    
    protected boolean synchronizeMappings(ClassLoader loader, JDBCConfiguration conf) {
    	return synchronizeMappings(loader, conf, conf.getSynchronizeMappings());
    }

    /**
     * Synchronize the mappings of the classes listed in the configuration.
     */
    protected boolean synchronizeMappings(ClassLoader loader, JDBCConfiguration conf, String action) {
        mapSchemaGenerationToSynchronizeMappings(conf);
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

        // initialize the schema
        for (Class<?> cls : classes) {
            try {
                tool.run(cls);
            } catch (IllegalArgumentException iae) {
                throw new UserException(_loc.get("bad-synch-mappings",
                    action, Arrays.asList(MappingTool.ACTIONS)));
            }
        }
        tool.record();
        return true; // todo: check?
    }

    protected boolean synchronizeMappings(ClassLoader loader) {
        return synchronizeMappings(loader, (JDBCConfiguration) getConfiguration());
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

        String loadFile = conf.getLoadScriptSource();
        if (loadFile != null) {
            actions += "," + MappingTool.ACTION_SCRIPT_LOAD;
        }

        if (actions.length() > 0) {
            conf.setSynchronizeMappings("buildSchema(ForeignKeys=true,SchemaAction='" + actions + "')");
        }
    }

    private String generateSchemaCreation(JDBCConfiguration conf) {
        if (conf.getCreateScriptTarget() != null) {
            return SchemaTool.ACTION_BUILD;
        } else {
            int createSource = conf.getCreateSourceConstant();
            if (createSource == SchemaGenerationSource.NONE && conf.getCreateScriptSource() != null) {
                createSource = SCRIPT;
            } else {
                createSource = METADATA;
            }
            return mapGenerationStrategyActions(createSource, SchemaTool.ACTION_ADD, MappingTool.ACTION_SCRIPT_CREATE);
        }
    }

    private String generateSchemaDrop(JDBCConfiguration conf) {
        if (conf.getDropScriptTarget() != null) {
            return SchemaTool.ACTION_DROP;
        } else {
            int dropSource = conf.getDropSourceConstant();
            if (dropSource == SchemaGenerationSource.NONE && conf.getDropScriptSource() != null) {
                dropSource = SCRIPT;
            } else {
                dropSource = METADATA;
            }
            return mapGenerationStrategyActions(dropSource, SchemaTool.ACTION_DROP, MappingTool.ACTION_SCRIPT_DROP);
        }
    }

    private String generateSchemaDropCreate(JDBCConfiguration conf) {
        if (conf.getCreateScriptTarget() != null && conf.getDropScriptTarget() != null) {
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

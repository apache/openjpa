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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.conf.SchemaGenerationSource;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

import static org.apache.openjpa.conf.SchemaGenerationAction.CREATE;
import static org.apache.openjpa.conf.SchemaGenerationAction.DROP;
import static org.apache.openjpa.conf.SchemaGenerationAction.DROP_AND_CREATE;
import static org.apache.openjpa.conf.SchemaGenerationSource.METADATA;
import static org.apache.openjpa.conf.SchemaGenerationSource.METADATA_THEN_SCRIPT;
import static org.apache.openjpa.conf.SchemaGenerationSource.SCRIPT;
import static org.apache.openjpa.conf.SchemaGenerationSource.SCRIPT_THEN_METADATA;
import static org.apache.openjpa.jdbc.meta.MappingTool.ACTION_ADD;
import static org.apache.openjpa.jdbc.meta.MappingTool.ACTION_DROP;
import static org.apache.openjpa.jdbc.meta.MappingTool.ACTION_SCRIPT_CREATE;
import static org.apache.openjpa.jdbc.meta.MappingTool.ACTION_SCRIPT_DROP;
import static org.apache.openjpa.jdbc.meta.MappingTool.ACTION_SCRIPT_LOAD;

/**
 * BrokerFactory type for use with the JDBC runtime.
 *
 * @author Abe White
 * @author Marc Prud'hommeaux
 */
@SuppressWarnings("serial")
public class JDBCBrokerFactory
    extends AbstractBrokerFactory {

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

    protected StoreManager newStoreManager() {
        return new JDBCStoreManager();
    }

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

    /**
     * Synchronize the mappings of the classes listed in the configuration.
     */
    protected void synchronizeMappings(ClassLoader loader, 
        JDBCConfiguration conf) {
        mapSchemaGenerationToSynchronizeMappings(conf);
        String action = conf.getSynchronizeMappings();
        if (StringUtil.isEmpty(action))
            return;

        MappingRepository repo = conf.getMappingRepositoryInstance();
        Collection<Class<?>> classes = repo.loadPersistentTypes(false, loader);
        if (classes.isEmpty())
            return;

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
    }
    
    protected void synchronizeMappings(ClassLoader loader) {
        synchronizeMappings(loader, (JDBCConfiguration) getConfiguration());
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
            actions += "," + ACTION_SCRIPT_LOAD;
        }

        if (actions.length() > 0) {
            conf.setSynchronizeMappings("buildSchema(ForeignKeys=true,SchemaAction='" + actions + "')");
        }
    }

    private String generateSchemaCreation(JDBCConfiguration conf) {
        if (conf.getCreateScriptTarget() != null) {
            return MappingTool.ACTION_ADD;
        } else {
            int createSource = conf.getCreateSourceConstant();
            if (createSource == SchemaGenerationSource.NONE && conf.getCreateScriptSource() != null) {
                createSource = SCRIPT;
            } else {
                createSource = METADATA;
            }
            return mapGenerationStrategyActions(createSource, ACTION_ADD, ACTION_SCRIPT_CREATE);
        }
    }

    private String generateSchemaDrop(JDBCConfiguration conf) {
        if (conf.getDropScriptTarget() != null) {
            return MappingTool.ACTION_DROP;
        } else {
            int dropSource = conf.getDropSourceConstant();
            if (dropSource == SchemaGenerationSource.NONE && conf.getDropScriptSource() != null) {
                dropSource = SCRIPT;
            } else {
                dropSource = METADATA;
            }
            return mapGenerationStrategyActions(dropSource, ACTION_DROP, ACTION_SCRIPT_DROP);
        }
    }

    private String generateSchemaDropCreate(JDBCConfiguration conf) {
        if (conf.getCreateScriptTarget() != null && conf.getDropScriptTarget() != null) {
            return MappingTool.ACTION_ADD + "," + MappingTool.ACTION_DROP;
        } else {
            return mapGenerationStrategyActions(conf.getDropSourceConstant(), ACTION_DROP, ACTION_SCRIPT_DROP) + "," +
                   mapGenerationStrategyActions(conf.getCreateSourceConstant(), ACTION_ADD, ACTION_SCRIPT_CREATE);
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

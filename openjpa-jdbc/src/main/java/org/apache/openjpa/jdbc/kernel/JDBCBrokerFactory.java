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
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
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

/**
 * BrokerFactory type for use with the JDBC runtime.
 *
 * @author Abe White
 * @author Marc Prud'hommeaux
 */
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
    public static JDBCBrokerFactory getInstance(ConfigurationProvider cp) {
        JDBCBrokerFactory factory = (JDBCBrokerFactory) getPooledFactory
            (cp.getProperties());
        if (factory != null)
            return factory;

        factory = newInstance(cp);
        factory.pool();
        return factory;
    }

    /**
     * Factory method for constructing a factory from a configuration.
     */
    public static synchronized JDBCBrokerFactory getInstance
        (JDBCConfiguration conf) {
        JDBCBrokerFactory factory = (JDBCBrokerFactory) getPooledFactory
            (conf.toProperties(false));
        if (factory != null)
            return factory;

        factory = new JDBCBrokerFactory(conf);
        factory.pool();
        return factory;
    }

    /**
     * Construct the factory with the given option settings; however, the
     * factory construction methods are recommended.
     */
    public JDBCBrokerFactory(JDBCConfiguration conf) {
        super(conf);
    }

    public Properties getProperties() {
        // add platform property
        Properties props = super.getProperties();
        String db = "Unknown";
        try {
            JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
            db = conf.getDBDictionaryInstance().platform;
        } catch (RuntimeException re) {
        }
        props.setProperty("Platform",
            "OpenJPA JDBC Edition: " + db + " Database");

        return props;
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
    private void synchronizeMappings(ClassLoader loader) {
        JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
        String action = conf.getSynchronizeMappings();
        if (StringUtils.isEmpty(action))
            return;

        MappingRepository repo = conf.getMappingRepositoryInstance();
        Collection classes = repo.loadPersistentTypes(false, loader);
        if (classes.isEmpty())
            return;

        String props = Configurations.getProperties(action);
        action = Configurations.getClassName(action);
        MappingTool tool = new MappingTool(conf, action, false);
        Configurations.configureInstance(tool, conf, props,
            "SynchronizeMappings");

        // initialize the schema
        Class cls;
        for (Iterator itr = classes.iterator(); itr.hasNext();) {
            cls = (Class) itr.next();
            try {
                tool.run(cls);
            } catch (IllegalArgumentException iae) {
                throw new UserException(_loc.get("bad-synch-mappings",
                    action, Arrays.asList(MappingTool.ACTIONS)));
            }
        }
        tool.record();
    }
}

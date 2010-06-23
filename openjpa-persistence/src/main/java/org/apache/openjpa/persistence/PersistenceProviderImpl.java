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
package org.apache.openjpa.persistence;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.openjpa.conf.BrokerValue;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.enhance.PCClassFileTransformer;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;


/**
 * Bootstrapping class that allows the creation of a stand-alone
 * {@link EntityManager}.
 *
 * @see javax.persistence.Persistence#createEntityManagerFactory(String,Map)
 * @published
 */
public class PersistenceProviderImpl
    implements PersistenceProvider {

    static final String CLASS_TRANSFORMER_OPTIONS = "ClassTransformerOptions";
    private static final String EMF_POOL = "EntityManagerFactoryPool";

    private static final Localizer _loc = Localizer.forPackage(
        PersistenceProviderImpl.class);

    /**
     * Loads the entity manager specified by <code>name</code>, applying
     * the properties in <code>m</code> as overrides to the properties defined
     * in the XML configuration file for <code>name</code>. If <code>name</code>
     * is <code>null</code>, this method loads the XML in the resource
     * identified by <code>resource</code>, and uses the first resource found
     * when doing this lookup, regardless of the name specified in the XML
     * resource or the name of the jar that the resource is contained in.
     * This does no pooling of EntityManagersFactories.
     */
    public OpenJPAEntityManagerFactory createEntityManagerFactory(String name,
        String resource, Map m) {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        try {
            Object poolValue = Configurations.removeProperty(EMF_POOL, m);
            ConfigurationProvider cp = pd.load(resource, name, m);
            if (cp == null)
                return null;

            BrokerFactory factory = getBrokerFactory(cp, poolValue, null);
            return JPAFacadeHelper.toEntityManagerFactory(factory);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    private BrokerFactory getBrokerFactory(ConfigurationProvider cp,
        Object poolValue, ClassLoader loader) {
        // handle "true" and "false"
        if (poolValue instanceof String
            && ("true".equalsIgnoreCase((String) poolValue)
                || "false".equalsIgnoreCase((String) poolValue)))
            poolValue = Boolean.valueOf((String) poolValue);

        if (poolValue != null && !(poolValue instanceof Boolean)) {
            // we only support boolean settings for this option currently.
            throw new IllegalArgumentException(poolValue.toString());
        }
        
        if (poolValue == null || !((Boolean) poolValue).booleanValue())
            return Bootstrap.newBrokerFactory(cp, loader);
        else
            return Bootstrap.getBrokerFactory(cp, loader);
    }

    public OpenJPAEntityManagerFactory createEntityManagerFactory(String name,
        Map m) {
        return createEntityManagerFactory(name, null, m);
    }

    public OpenJPAEntityManagerFactory createContainerEntityManagerFactory(
        PersistenceUnitInfo pui, Map m) {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        try {
            Object poolValue = Configurations.removeProperty(EMF_POOL, m);
            ConfigurationProvider cp = pd.load(pui, m);
            if (cp == null)
                return null;

            // add enhancer
            Exception transformerException = null;
            String ctOpts = (String) Configurations.getProperty
                (CLASS_TRANSFORMER_OPTIONS, pui.getProperties());
            try {
                pui.addTransformer(new ClassTransformerImpl(cp, ctOpts,
                    pui.getNewTempClassLoader(), newConfigurationImpl()));
            } catch (Exception e) {
                // fail gracefully
                transformerException = e;
            }

            // if the BrokerImpl hasn't been specified, switch to the
            // non-finalizing one, since anything claiming to be a container
            // should be doing proper resource management.
            if (!Configurations.containsProperty(BrokerValue.KEY,
                cp.getProperties())) {
                cp.addProperty("openjpa." + BrokerValue.KEY, 
                    getDefaultBrokerAlias());
            }

            BrokerFactory factory = getBrokerFactory(cp, poolValue,
                pui.getClassLoader());
            if (transformerException != null) {
                Log log = factory.getConfiguration().getLog(
                    OpenJPAConfiguration.LOG_RUNTIME);
                if (log.isTraceEnabled()) {
                    log.warn(
                        _loc.get("transformer-registration-error-ex", pui),
                        transformerException);
                } else {
                    log.warn(
                        _loc.get("transformer-registration-error", pui));
                }
            }
            return JPAFacadeHelper.toEntityManagerFactory(factory);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /*
     * Returns a default Broker alias to be used when no openjpa.BrokerImpl
     *  is specified. This method allows PersistenceProvider subclass to
     *  override the default broker alias.
     */
    protected String getDefaultBrokerAlias() {
        return BrokerValue.NON_FINALIZING_ALIAS;
    }
    
    /*
     * Return a new instance of Configuration subclass used by entity
     * enhancement in ClassTransformerImpl. If OpenJPAConfigurationImpl
     * instance is used, configuration options declared in configuration
     * sub-class will not be recognized and a warning is posted in the log.
     */
    protected OpenJPAConfiguration newConfigurationImpl() {
        return new OpenJPAConfigurationImpl();
    }
    
    /**
     * Java EE 5 class transformer.
     */
    private static class ClassTransformerImpl
        implements ClassTransformer {

        private final ClassFileTransformer _trans;

        private ClassTransformerImpl(ConfigurationProvider cp, String props, 
            final ClassLoader tmpLoader, OpenJPAConfiguration conf) {
            cp.setInto(conf);
            // use the tmp loader for everything
            conf.setClassResolver(new ClassResolver() {
                public ClassLoader getClassLoader(Class context, 
                    ClassLoader env) {
                    return tmpLoader;
                }
            });
            conf.setReadOnly(Configuration.INIT_STATE_FREEZING);

            MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
            repos.setResolve(MetaDataModes.MODE_MAPPING, false);
            _trans = new PCClassFileTransformer(repos,
                Configurations.parseProperties(props), tmpLoader);
        }

        public byte[] transform(ClassLoader cl, String name,
            Class<?> previousVersion, ProtectionDomain pd, byte[] bytes)
            throws IllegalClassFormatException {
            return _trans.transform(cl, name, previousVersion, pd, bytes);
        }
    }
}

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

import org.apache.openjpa.conf.BrokerValue;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.enhance.PCClassFileTransformer;
import org.apache.openjpa.enhance.PCEnhancerAgent;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.MultiClassLoader;
import org.apache.openjpa.meta.AbstractCFMetaDataFactory;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.osgi.BundleUtils;
import org.apache.openjpa.persistence.validation.ValidationUtils;
import org.apache.openjpa.util.ClassResolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import jakarta.persistence.spi.TransformerException;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;


/**
 * Bootstrapping class that allows the creation of a stand-alone
 * {@link EntityManager}.
 *
 * @see jakarta.persistence.Persistence#createEntityManagerFactory(String,Map)
 * @published
 */
public class PersistenceProviderImpl
    implements PersistenceProvider, ProviderUtil {

    static final String CLASS_TRANSFORMER_OPTIONS = "ClassTransformerOptions";
    private static final String EMF_POOL = "EntityManagerFactoryPool";

    private static final Localizer _loc = Localizer.forPackage(PersistenceProviderImpl.class);

    private Log _log;
    /**
     * Loads the entity manager specified by <code>name</code>, applying
     * the properties in <code>m</code> as overrides to the properties defined
     * in the XML configuration file for <code>name</code>. If <code>name</code>
     * is <code>null</code>, this method loads the XML in the resource
     * identified by <code>resource</code>, and uses the first resource found
     * when doing this lookup, regardless of the name specified in the XML
     * resource or the name of the jar that the resource is contained in.
     * This does no pooling of EntityManagersFactories.
     * @return EntityManagerFactory or null
     */
    public OpenJPAEntityManagerFactory createEntityManagerFactory(String name, String resource, Map m) {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        try {
            Object poolValue = Configurations.removeProperty(EMF_POOL, m);
            ConfigurationProvider cp = pd.load(resource, name, m);
            if (cp == null) {
                return null;
            }

            BrokerFactory factory = getBrokerFactory(cp, poolValue, BundleUtils.getBundleClassLoader());
            OpenJPAConfiguration conf = factory.getConfiguration();
            conf.setUserClassLoader(BundleUtils.getBundleClassLoader());
            _log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
            pd.checkPuNameCollisions(_log,name);

            // add enhancer
            loadAgent(factory);

            // Create appropriate LifecycleEventManager
            loadValidator(factory);

            if (conf.getConnectionRetainModeConstant() == ConnectionRetainModes.CONN_RETAIN_ALWAYS) {
                // warn about EMs holding on to connections.
                _log.warn(_loc.get("retain-always", conf.getId()));
            }

            OpenJPAEntityManagerFactory emf = JPAFacadeHelper.toEntityManagerFactory(factory);
            if (_log.isTraceEnabled()) {
                _log.trace(this + " creating " + emf + " for PU " + name + ".");
            }
            return emf;
        } catch (Exception e) {
            if (_log != null) {
                _log.error(_loc.get("create-emf-error", name), e);
            }

            /*
             *
             * Maintain 1.x behavior of throwing exceptions, even though
             * JPA2 9.2 - createEMF "must" return null for PU it can't handle.
             *
             * JPA 2.0 Specification Section 9.2 states:
             * "If a provider does not qualify as the provider for the named persistence unit,
             * it must return null when createEntityManagerFactory is invoked on it."
             * That specification compliance behavior has happened few lines above on null return.
             * Throwing runtime exception in the following code is valid (and useful) behavior
             * because the qualified provider has encountered an unexpected situation.
             */
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    private BrokerFactory getBrokerFactory(ConfigurationProvider cp, Object poolValue, ClassLoader loader) {
        // handle "true" and "false"
        if (poolValue instanceof String pv && ("true".equalsIgnoreCase(pv) || "false".equalsIgnoreCase(pv))) {
            poolValue = Boolean.valueOf(pv);
        }

        if (poolValue != null && !(poolValue instanceof Boolean)) {
            // we only support boolean settings for this option currently.
            throw new IllegalArgumentException(poolValue.toString());
        }
        
        if (poolValue == null || !((Boolean) poolValue)) {
            return Bootstrap.newBrokerFactory(cp, loader);
        } else {
            return Bootstrap.getBrokerFactory(cp, loader);
        }
    }

    @Override
    public OpenJPAEntityManagerFactory createEntityManagerFactory(String name, Map m) {
        return createEntityManagerFactory(name, null, m);
    }

    @Override
    public OpenJPAEntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map m) {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        try {
            Object poolValue = Configurations.removeProperty(EMF_POOL, m);
            ConfigurationProvider cp = pd.load(pui, m);
            if (cp == null)
                return null;

            // add enhancer
            Exception transformerException = null;
            String ctOpts = (String) Configurations.getProperty(CLASS_TRANSFORMER_OPTIONS, pui.getProperties());
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
            if (!Configurations.containsProperty(BrokerValue.KEY, cp.getProperties())) {
                cp.addProperty("openjpa." + BrokerValue.KEY, getDefaultBrokerAlias());
            }


            ClassLoader loader = pui.getClassLoader();
            if (BundleUtils.runningUnderOSGi()) {
                // OPENJPA-1491 : If running under OSGi, use the Bundle's ClassLoader instead of the application one
                // OPENJPA-2542 : Also try to load from app loader in the case of a user implemented interface/config
                loader = new MultiClassLoader(BundleUtils.getBundleClassLoader(), loader);
            }
            BrokerFactory factory = getBrokerFactory(cp, poolValue, loader);
            OpenJPAConfiguration conf = factory.getConfiguration();
            setPersistenceEnvironmentInfo(conf, pui);
            _log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
            // now we can log any transformer exceptions from above
            if (transformerException != null) {
                if (_log.isTraceEnabled()) {
                    _log.warn(_loc.get("transformer-registration-error-ex", pui), transformerException);
                } else {
                    _log.warn(_loc.get("transformer-registration-error", pui));
                }
            }

            if (conf.getConnectionRetainModeConstant() == ConnectionRetainModes.CONN_RETAIN_ALWAYS) {
                // warn about container managed EMs holding on to connections.
                _log.warn(_loc.get("cm-retain-always",conf.getId()));
            }

            // Create appropriate LifecycleEventManager
            loadValidator(factory);

            OpenJPAEntityManagerFactory emf = JPAFacadeHelper.toEntityManagerFactory(factory);
            if (_log.isTraceEnabled()) {
                _log.trace(this + " creating container " + emf + " for PU " + pui.getPersistenceUnitName() + ".");
            }

            return emf;
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    @Override
    public void generateSchema(final PersistenceUnitInfo info, final Map map) {
        final Map runMap = map == null ? new HashMap<>() : new HashMap<>(map);

        if (!acceptProvider(runMap)) {
            return;
        }

        runMap.put("jakarta.persistence.schema-generation.database.action", "create");
        final OpenJPAEntityManagerFactory factory = createContainerEntityManagerFactory(info, runMap);
        try {
            synchronizeMappings(factory);
        } finally {
            factory.close();
        }
    }

    @Override
    public boolean generateSchema(final String persistenceUnitName, final Map map) {
        final Map runMap = map == null ? new HashMap<>() : new HashMap<>(map);

        if (!acceptProvider(runMap)) {
            return false;
        }

        runMap.put("jakarta.persistence.schema-generation.database.action", "create");
        final OpenJPAEntityManagerFactory factory = createEntityManagerFactory(persistenceUnitName, runMap);
        try {
            final Object obj = synchronizeMappings(factory);
            return Boolean.class.cast(obj) ? Boolean.class.cast(obj) : true;
        } finally {
            factory.close();
        }
    }

    // if persistence provider is specific, don't do anything
    // only allowed to process if persistence provider matches or if not provider is specified
    public boolean acceptProvider(final Map properties) {
        Object provider = properties.get("jakarta.persistence.provider");

        // provider is specified, so it has to match
        if (provider != null) {
            if (provider instanceof Class) {
                provider = ((Class) provider).getName();
            }
            try {
                if (!((String) provider).equals(org.apache.openjpa.persistence.PersistenceProviderImpl.class.getName())) {
                    return false;
                }

            } catch (final ClassCastException e) {
                return false;
                // not a recognized provider property value so must be another provider.
            }
        }

        // no provider specified
        return true;

    }

    private Object synchronizeMappings(final OpenJPAEntityManagerFactory factory) {
        if (EntityManagerFactoryImpl.class.isInstance(factory)) {
            final EntityManagerFactoryImpl entityManagerFactory = EntityManagerFactoryImpl.class.cast(factory);
            final BrokerFactory brokerFactory = entityManagerFactory.getBrokerFactory();
            if (!AbstractBrokerFactory.class.isInstance(brokerFactory)) {
                throw new IllegalArgumentException("expected AbstractBrokerFactory but got " + brokerFactory);
            }
            try {
                final Method synchronizeMappings = brokerFactory.getClass()
                        .getDeclaredMethod("synchronizeMappings", ClassLoader.class);
                if (!synchronizeMappings.isAccessible()) {
                    synchronizeMappings.setAccessible(true);
                }
                return synchronizeMappings.invoke(brokerFactory, Thread.currentThread().getContextClassLoader());
            } catch (final NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                final Throwable targetException = e.getTargetException();
                if (RuntimeException.class.isInstance(targetException)) {
                    throw RuntimeException.class.cast(targetException);
                }
                throw new IllegalStateException(targetException);
            }
        } else {
            throw new IllegalArgumentException("expected EntityManagerFactoryImpl but got " + factory);
        }
    }

    public void setPersistenceEnvironmentInfo(OpenJPAConfiguration conf, PersistenceUnitInfo pui) {
        // OPENJPA-1460 Fix scope visibility of orm.xml when it is packaged in both ear file and war file
        if (conf instanceof OpenJPAConfigurationImpl) {
            Map<String, Object> peMap =((OpenJPAConfigurationImpl)conf).getPersistenceEnvironment();
            if (peMap == null) {
                peMap = new HashMap<>();
                ((OpenJPAConfigurationImpl)conf).setPersistenceEnvironment(peMap);
            }
            peMap.put(AbstractCFMetaDataFactory.PERSISTENCE_UNIT_ROOT_URL, pui.getPersistenceUnitRootUrl());
            peMap.put(AbstractCFMetaDataFactory.MAPPING_FILE_NAMES, pui.getMappingFileNames());
            peMap.put(AbstractCFMetaDataFactory.JAR_FILE_URLS, pui.getJarFileUrls());
        }
    }

    /*
     * Returns a ProviderUtil for use with entities managed by this
     * persistence provider.
     */
    @Override
    public ProviderUtil getProviderUtil() {
        return this;
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
            // use the temporary loader for everything
            conf.setClassResolver(new ClassResolver() {
                @Override
                public ClassLoader getClassLoader(Class<?> context, ClassLoader env) {
                    return tmpLoader;
                }
            });
            conf.setReadOnly(Configuration.INIT_STATE_FREEZING);

            MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
            repos.setResolve(MetaDataModes.MODE_MAPPING, false);
            _trans = PCClassFileTransformer.newInstance(repos,
                Configurations.parseProperties(props), tmpLoader);
        }

        @Override
        public byte[] transform(ClassLoader cl, String name, Class<?> previousVersion, ProtectionDomain pd, byte[] bytes)
                throws TransformerException {
            try {
                return _trans.transform(cl, name, previousVersion, pd, bytes);
            }
            catch (IllegalClassFormatException e) {
                throw new TransformerException(e);
            }
        }
    }

    /**
     * This private worker method will attempt load the PCEnhancerAgent.
     */
    private void loadAgent(BrokerFactory factory) {
        OpenJPAConfiguration conf = factory.getConfiguration();
        Log log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);

        if (conf.getDynamicEnhancementAgent()) {
            boolean res = PCEnhancerAgent.loadDynamicAgent(log);
            if (log.isInfoEnabled() && res){
                log.info(_loc.get("dynamic-agent"));
            }
        }
    }

    /**
     * This private worker method will attempt to setup the proper
     * LifecycleEventManager type based on if the jakarta.validation APIs are
     * available and a ValidatorImpl is required by the configuration.
     * @throws if validation setup failed and was required by the config
     */
    private void loadValidator(BrokerFactory factory) {
        OpenJPAConfiguration conf = factory.getConfiguration();
        Log log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);

        if ((ValidationUtils.setupValidation(conf)) &&
                log.isInfoEnabled()) {
            log.info(_loc.get("vlem-creation-info"));
        }
    }

    /**
     * Determines whether the specified object is loaded.
     *
     * @return LoadState.LOADED - if all implicit or explicit EAGER fetch
     *         attributes are loaded
     *         LoadState.NOT_LOADED - if any implicit or explicit EAGER fetch
     *         attribute is not loaded
     *         LoadState.UNKNOWN - if the entity is not managed by this
     *         provider.
     */
    @Override
    public LoadState isLoaded(Object obj) {
        return isLoadedWithoutReference(obj, null);
    }

    /**
     * Determines whether the attribute on the specified object is loaded.  This
     * method may access the value of the attribute to determine load state (but
     * currently does not).
     *
     * @return LoadState.LOADED - if the attribute is loaded.
     *         LoadState.NOT_LOADED - if the attribute is not loaded or any
     *         EAGER fetch attributes of the entity are not loaded.
     *         LoadState.UNKNOWN - if the entity is not managed by this
     *         provider or if it does not contain the persistent
     *         attribute.
     */
    @Override
    public LoadState isLoadedWithReference(Object obj, String attr) {
        // TODO: Are there be any cases where OpenJPA will need to examine
        // the contents of a field to determine load state?  If so, per JPA
        // contract, this method permits that sort of access. In the extremely
        // unlikely case that the the entity is managed by multiple providers,
        // even if it doesn't trigger loading in OpenJPA, accessing field data
        // could trigger loading by an alternate provider.
        return isLoadedWithoutReference(obj, attr);
    }

    /**
     * Determines whether the attribute on the specified object is loaded.  This
     * method does not access the value of the attribute to determine load
     * state.
     *
     * @return LoadState.LOADED - if the attribute is loaded.
     *         LoadState.NOT_LOADED - if the attribute is not loaded or any
     *         EAGER fetch attributes of the entity are not loaded.
     *         LoadState.UNKNOWN - if the entity is not managed by this
     *         provider or if it does not contain the persistent
     *         attribute.
     */
    @Override
    public LoadState isLoadedWithoutReference(Object obj, String attr) {
        if (obj == null) {
            return LoadState.UNKNOWN;
        }

        return OpenJPAPersistenceUtil.isLoaded(obj, attr);
    }

	@Override
	public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}
}

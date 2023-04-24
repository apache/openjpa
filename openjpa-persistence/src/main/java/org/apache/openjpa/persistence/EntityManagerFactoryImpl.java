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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.spi.LoadState;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.kernel.DelegatingBrokerFactory;
import org.apache.openjpa.kernel.DelegatingFetchConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.Value;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.persistence.criteria.CriteriaBuilderImpl;
import org.apache.openjpa.persistence.criteria.OpenJPACriteriaBuilder;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.query.OpenJPAQueryBuilder;
import org.apache.openjpa.persistence.query.QueryBuilderImpl;

/**
 * Implementation of {@link EntityManagerFactory} that acts as a
 * facade to a {@link BrokerFactory}.
 *
 * @author Marc Prud'hommeaux
 */
public class EntityManagerFactoryImpl
    implements OpenJPAEntityManagerFactory, OpenJPAEntityManagerFactorySPI,
    Closeable, PersistenceUnitUtil {

    private static final long serialVersionUID = 1L;

    private static final Localizer _loc = Localizer.forPackage
        (EntityManagerFactoryImpl.class);

    private DelegatingBrokerFactory _factory = null;
    private transient Constructor<FetchPlan> _plan = null;
    private transient StoreCache _cache = null;
    private transient QueryResultCache _queryCache = null;
    private transient MetamodelImpl _metaModel;
    private transient Map<String, Object> properties;
    private transient Map<String, Object> emEmptyPropsProperties;

    /**
     * Default constructor provided for auto-instantiation.
     */
    public EntityManagerFactoryImpl() {
    }

    /**
     * Supply delegate on construction.
     */
    public EntityManagerFactoryImpl(BrokerFactory factory) {
        setBrokerFactory(factory);
    }

    /**
     * Delegate.
     */
    public BrokerFactory getBrokerFactory() {
        return _factory.getDelegate();
    }

    /**
     * Delegate must be provided before use.
     */
    public void setBrokerFactory(BrokerFactory factory) {
        _factory = new DelegatingBrokerFactory(factory,
            PersistenceExceptions.TRANSLATOR);
    }

    @Override
    public OpenJPAConfiguration getConfiguration() {
        return _factory.getConfiguration();
    }

    @Override
    public Map<String,Object> getProperties() {
        if (properties == null) {
            Map<String,Object> props = _factory.getProperties();
            // convert to user readable values
            if (emEmptyPropsProperties != null) {
                props.putAll(emEmptyPropsProperties);
            } else {
                props.putAll(doCreateEM(SynchronizationType.SYNCHRONIZED, null, true).getProperties());
            }
            // no need to sync or volatile, worse case concurrent threads create 2 instances
            // we just want to avoid to do it after some "init" phase
            this.properties = props;
        }
        return properties;
    }

    @Override
    public Object putUserObject(Object key, Object val) {
        return _factory.putUserObject(key, val);
    }

    @Override
    public Object getUserObject(Object key) {
        return _factory.getUserObject(key);
    }

    @Override
    public StoreCache getStoreCache() {
        _factory.lock();
        try {
            if (_cache == null) {
                OpenJPAConfiguration conf = _factory.getConfiguration();
                _cache = new StoreCacheImpl(this,
                    conf.getDataCacheManagerInstance().getSystemDataCache());
            }
            return _cache;
        } finally {
            _factory.unlock();
        }
    }

    @Override
    public StoreCache getStoreCache(String cacheName) {
        return new StoreCacheImpl(this, _factory.getConfiguration().
            getDataCacheManagerInstance().getDataCache(cacheName, true));
    }

    @Override
    public QueryResultCache getQueryResultCache() {
        _factory.lock();
        try {
            if (_queryCache == null)
                _queryCache = new QueryResultCacheImpl(_factory.
                    getConfiguration().getDataCacheManagerInstance().
                    getSystemQueryCache());
            return _queryCache;
        } finally {
            _factory.unlock();
        }
    }

    @Override
    public OpenJPAEntityManagerSPI createEntityManager() {
        return createEntityManager((Map) null);
    }

    @Override
    public OpenJPAEntityManagerSPI createEntityManager(SynchronizationType synchronizationType) {
        return createEntityManager(synchronizationType, null);
    }

    @Override
    public OpenJPAEntityManagerSPI createEntityManager(Map props) {
        return createEntityManager(SynchronizationType.SYNCHRONIZED, props);
    }


    /**
     * Creates and configures a entity manager with the given properties.
     *
     * The property keys in the given map can be either qualified or not.
     *
     * @return list of exceptions raised or empty list.
     */
    @Override
    public OpenJPAEntityManagerSPI createEntityManager(SynchronizationType synchronizationType, Map props) {
        return doCreateEM(synchronizationType, props, false);
    }

    private OpenJPAEntityManagerSPI doCreateEM(SynchronizationType synchronizationType,
                                               Map props,
                                               boolean byPassSynchronizeMappings) {
        if (synchronizationType == null) {
            throw new NullPointerException("SynchronizationType must not be null");
        }
        if (SynchronizationType.UNSYNCHRONIZED.equals(synchronizationType)) {
            throw new UnsupportedOperationException("TODO - implement JPA 2.1 feature");
        }

        if (props == null) {
            props = Collections.EMPTY_MAP;
        }
        else if (!props.isEmpty()) {
            props = new HashMap(props);
        }

        boolean canCacheGetProperties = props.isEmpty(); // nominal case

        OpenJPAConfiguration conf = getConfiguration();
        Log log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        String user = (String) Configurations.removeProperty("ConnectionUserName", props);
        if (user == null)
            user = conf.getConnectionUserName();
        String pass = (String) Configurations.removeProperty("ConnectionPassword", props);
        if (pass == null)
            pass = conf.getConnectionPassword();

        String str = (String) Configurations.removeProperty("TransactionMode", props);
        boolean managed;
        if (str == null)
            managed = conf.isTransactionModeManaged();
        else {
            Value val = conf.getValue("TransactionMode");
            managed = Boolean.parseBoolean(val.unalias(str));
        }

        Object obj = Configurations.removeProperty("ConnectionRetainMode", props);
        int retainMode;
        if (obj instanceof Number) {
            retainMode = ((Number) obj).intValue();
        } else if (obj == null) {
            retainMode = conf.getConnectionRetainModeConstant();
        } else {
            Value val = conf.getValue("ConnectionRetainMode");
            try {
                retainMode = Integer.parseInt(val.unalias((String) obj));
            } catch (Exception e) {
                throw new ArgumentException(_loc.get("bad-em-prop", "openjpa.ConnectionRetainMode", obj),
                    new Throwable[]{ e }, obj, true);
            }
        }

        // jakarta.persistence.jtaDataSource and openjpa.ConnectionFactory name are equivalent.
        // prefer jakarta.persistence for now.
        String cfName = (String) Configurations.removeProperty("jtaDataSource", props);
        if(cfName == null) {
            cfName = (String) Configurations.removeProperty("ConnectionFactoryName", props);
        }

        String cf2Name = (String) Configurations.removeProperty("nonJtaDataSource", props);

        if(cf2Name == null) {
            cf2Name = (String) Configurations.removeProperty("ConnectionFactory2Name", props);
        }

        if (log != null && log.isTraceEnabled()) {
            if(StringUtil.isNotEmpty(cfName)) {
                log.trace("Found ConnectionFactoryName from props: " + cfName);
            }
            if(StringUtil.isNotEmpty(cf2Name)) {
                log.trace("Found ConnectionFactory2Name from props: " + cf2Name);
            }
        }
        validateCfNameProps(conf, cfName, cf2Name);

        Broker broker = byPassSynchronizeMappings ?
                conf.newBrokerInstance(user, pass) :
                _factory.newBroker(user, pass, managed, retainMode, false, cfName, cf2Name);

        // add autodetach for close and rollback conditions to the configuration
        broker.setAutoDetach(AutoDetach.DETACH_CLOSE, true);
        broker.setAutoDetach(AutoDetach.DETACH_ROLLBACK, true);
        broker.setDetachedNew(false);

        OpenJPAEntityManagerSPI em = newEntityManagerImpl(broker);

        // allow setting of other bean properties of EM
        if (!props.isEmpty()) {
            Set<Map.Entry> entrySet = props.entrySet();
            for (Map.Entry entry : entrySet) {
                em.setProperty(entry.getKey().toString(), entry.getValue());
            }
        }
        if (canCacheGetProperties) {
            if (emEmptyPropsProperties == null) {
                emEmptyPropsProperties = em.getProperties();
            } else if (EntityManagerImpl.class.isInstance(em)) {
                EntityManagerImpl.class.cast(em).setProperties(emEmptyPropsProperties);
            }
        }
        if (log != null && log.isTraceEnabled()) {
            log.trace(this + " created EntityManager " + em + ".");
        }
        return em;
    }

    /**
     * Create a new entity manager around the given broker.
     */
    protected EntityManagerImpl newEntityManagerImpl(Broker broker) {
        return new EntityManagerImpl(this, broker);
    }

    @Override
    public void addLifecycleListener(Object listener, Class... classes) {
        _factory.addLifecycleListener(listener, classes);
    }

    @Override
    public void removeLifecycleListener(Object listener) {
        _factory.removeLifecycleListener(listener);
    }

    @Override
    public void addTransactionListener(Object listener) {
        _factory.addTransactionListener(listener);
    }

    @Override
    public void removeTransactionListener(Object listener) {
        _factory.removeTransactionListener(listener);
    }

    @Override
    public void close() {
        Log log = _factory.getConfiguration().getLog(OpenJPAConfiguration.LOG_RUNTIME);
        if (log.isTraceEnabled()) {
            log.trace(this + ".close() invoked.");
        }
        _factory.close();
    }

    @Override
    public boolean isOpen() {
        return !_factory.isClosed();
    }

    @Override
    public int hashCode() {
        return (_factory == null) ? 0 : _factory.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if ((other == null) || (other.getClass() != this.getClass()))
            return false;
        if (_factory == null)
            return false;
        return _factory.equals(((EntityManagerFactoryImpl) other)._factory);
    }

    /**
     * Create a store-specific facade for the given fetch configuration.
	 * If no facade class exists, we use the default {@link FetchPlan}.
     */
    FetchPlan toFetchPlan(Broker broker, FetchConfiguration fetch) {
        if (fetch == null)
            return null;

        if (fetch instanceof DelegatingFetchConfiguration)
            fetch = ((DelegatingFetchConfiguration) fetch).
                getInnermostDelegate();

        try {
            if (_plan == null) {
                Class storeType = (broker == null) ? null : broker.
                    getStoreManager().getInnermostDelegate().getClass();
                Class cls = _factory.getConfiguration().
                    getStoreFacadeTypeRegistry().
                    getImplementation(FetchPlan.class, storeType,
                    		FetchPlanImpl.class);
                _plan = cls.getConstructor(FetchConfiguration.class);
            }
            return _plan.newInstance(fetch);
        } catch (InvocationTargetException ite) {
            throw PersistenceExceptions.toPersistenceException
                (ite.getTargetException());
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
	}

    @Override
    public Cache getCache() {
        _factory.assertOpen();
        return getStoreCache();
    }

    @Override
    public OpenJPACriteriaBuilder getCriteriaBuilder() {
        return new CriteriaBuilderImpl().setMetaModel(getMetamodel());
    }

    @Override
    public OpenJPAQueryBuilder getDynamicQueryBuilder() {
        return new QueryBuilderImpl(this);
    }

    @Override
    public Set<String> getSupportedProperties() {
        return _factory.getSupportedProperties();
    }

    @Override
    public MetamodelImpl getMetamodel() {
        if (_metaModel == null) {
            MetaDataRepository mdr = getConfiguration().getMetaDataRepositoryInstance();
            mdr.setValidate(MetaDataRepository.VALIDATE_RUNTIME, true);
            mdr.setResolve(MetaDataModes.MODE_MAPPING_INIT, true);
            _metaModel = new MetamodelImpl(mdr);
        }
        return _metaModel;
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return this;
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        org.apache.openjpa.kernel.Query kernelQuery = ((QueryImpl<?>)query).getDelegate();
        MetaDataRepository metaDataRepositoryInstance = _factory.getConfiguration().getMetaDataRepositoryInstance();
        QueryMetaData metaData = metaDataRepositoryInstance.newQueryMetaData(null, null);
        metaData.setFrom(kernelQuery);
        metaDataRepositoryInstance.addQueryMetaData(metaData);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        if (cls.isInstance(this)) {
            return cls.cast(this);
        }
        throw new jakarta.persistence.PersistenceException(this + " is not a " + cls);
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    /**
     * Get the identifier for the specified entity.  If not managed by any
     * of the em's in this PU or not persistence capable, return null.
     */
    @Override
    public Object getIdentifier(Object entity) {
        return OpenJPAPersistenceUtil.getIdentifier(this, entity);
    }

    @Override
    public boolean isLoaded(Object entity) {
        return isLoaded(entity, null);
    }

    @Override
    public boolean isLoaded(Object entity, String attribute) {
        if (entity == null) {
            return false;
        }
        return (OpenJPAPersistenceUtil.isManagedBy(this, entity) &&
                (OpenJPAPersistenceUtil.isLoaded(entity, attribute) == LoadState.LOADED));
    }

    private void validateCfNameProps(OpenJPAConfiguration conf, String cfName, String cf2Name) {
        if (StringUtil.isNotEmpty(cfName) || StringUtil.isNotEmpty(cf2Name)) {
            if (conf.getDataCache() != "false" && conf.getDataCache() != null) {
                throw new ArgumentException(_loc.get("invalid-cfname-prop", new Object[] {
                    "openjpa.DataCache (L2 Cache)",
                    cfName,
                    cf2Name }), null, null, true);

            }
            if (conf.getQueryCache() != "false" && conf.getQueryCache() != null) {
                throw new ArgumentException(_loc.get("invalid-cfname-prop", new Object[] {
                    "openjpa.QueryCache",
                    cfName,
                    cf2Name }), null, null, true);
            }
            Object syncMap = conf.toProperties(false).get("openjpa.jdbc.SynchronizeMappings");
            if(syncMap != null) {
                throw new ArgumentException(_loc.get("invalid-cfname-prop", new Object[] {
                    "openjpa.jdbc.SynchronizeMappings",
                    cfName,
                    cf2Name }), null, null, true);
            }
        }
    }
}

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.spi.LoadState;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.util.ImplHelper;
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
import org.apache.openjpa.meta.EntityGraphMetaData;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.persistence.criteria.CriteriaBuilderImpl;
import org.apache.openjpa.persistence.criteria.OpenJPACriteriaBuilder;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.query.OpenJPAQueryBuilder;
import org.apache.openjpa.persistence.query.QueryBuilderImpl;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.UserException;

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

    private static final Localizer _loc = Localizer.forPackage(EntityManagerFactoryImpl.class);

    private DelegatingBrokerFactory _factory = null;
    private transient Constructor<FetchPlan> _plan = null;
    private transient StoreCache _cache = null;
    private transient QueryResultCache _queryCache = null;
    private transient MetamodelImpl _metaModel;
    private final java.util.concurrent.ConcurrentHashMap<String, EntityGraphImpl<?>>
        _entityGraphs = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean _entityGraphsInitialized;
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
        if (_factory.isClosed()) {
            throw new IllegalStateException(
                "EntityManagerFactory is closed.");
        }
        if (properties == null) {
            Map<String,Object> props = _factory.getProperties();
            // convert to user readable values
            if (emEmptyPropsProperties != null) {
                props.putAll(emEmptyPropsProperties);
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
        if (_factory.isClosed()) {
            throw new IllegalStateException(
                "EntityManagerFactory is closed.");
        }
        if (synchronizationType == null) {
            throw new NullPointerException("SynchronizationType must not be null");
        }
        // Per JPA spec, SynchronizationType is only for JTA EntityManagerFactories.
        // A RESOURCE_LOCAL EMF must throw IllegalStateException.
        if (SynchronizationType.UNSYNCHRONIZED.equals(synchronizationType)) {
            if (getTransactionType() == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
                throw new IllegalStateException(
                    "SynchronizationType.UNSYNCHRONIZED is not supported for " +
                    "RESOURCE_LOCAL EntityManagerFactory.");
            }
            throw new IllegalStateException(
                "SynchronizationType.UNSYNCHRONIZED is not yet supported.");
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
            } else if (em instanceof EntityManagerImpl) {
                ((EntityManagerImpl) em).setProperties(emEmptyPropsProperties);
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
        if (_factory.isClosed()) {
            throw new IllegalStateException(
                "EntityManagerFactory is already closed.");
        }
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
        if (_factory.isClosed()) {
            throw new IllegalStateException(
                "EntityManagerFactory is closed.");
        }
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
        if (!isOpen()) {
            throw new IllegalStateException(
                "EntityManagerFactory is closed.");
        }
        if (_metaModel == null) {
            MetaDataRepository mdr = getConfiguration().getMetaDataRepositoryInstance();
            mdr.setValidate(MetaDataRepository.VALIDATE_RUNTIME, true);
            mdr.setResolve(MetaDataModes.MODE_MAPPING_INIT, true);
            _metaModel = new MetamodelImpl(mdr);
        }
        return _metaModel;
    }
    
    @Override
    public String getName() {
    	return (String) _factory.getProperties().get("openjpa.Id");
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        if (_factory.isClosed()) {
            throw new IllegalStateException(
                "EntityManagerFactory is closed.");
        }
        return this;
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        QueryImpl<?> queryImpl = (QueryImpl<?>) query;
        org.apache.openjpa.kernel.Query kernelQuery = queryImpl.getDelegate();
        MetaDataRepository repos = _factory.getConfiguration().getMetaDataRepositoryInstance();
        QueryMetaData metaData = repos.newQueryMetaData(null, name);
        metaData.setFrom(kernelQuery);

        // If the source query uses the Criteria language, convert to JPQL
        // so that createNamedQuery can recreate it without needing the
        // original CriteriaQuery object (CriteriaBuilder.parse() only
        // accepts CriteriaQuery objects, not strings).
        if (OpenJPACriteriaBuilder.LANG_CRITERIA.equals(metaData.getLanguage())) {
            metaData.setLanguage(org.apache.openjpa.kernel.jpql.JPQLParser.LANG_JPQL);
            // For criteria queries, the kernel query string is null.
            // Use the facade's getQueryString() which returns the JPQL
            // generated from the CriteriaQuery (stored as the query id).
            String jpql = queryImpl.getQueryString();
            if (jpql != null) {
                metaData.setQueryString(jpql);
            }
        }

        // Capture JPA-level query properties per JPA 3.2 spec
        // FlushMode
        try {
            jakarta.persistence.FlushModeType fm = query.getFlushMode();
            if (fm != null) {
                metaData.setFlushType(
                    EntityManagerImpl.toFlushBeforeQueries(fm));
            }
        } catch (Exception e) {
            // ignore if not supported for this query type
        }

        // MaxResults
        try {
            int maxResults = query.getMaxResults();
            if (maxResults != Integer.MAX_VALUE) {
                metaData.setMaxResults(maxResults);
            }
        } catch (Exception e) {
            // ignore if not supported for this query type
        }

        // LockMode (only for JPQL and Criteria queries, not native)
        try {
            jakarta.persistence.LockModeType lm = query.getLockMode();
            if (lm != null) {
                metaData.setLockMode(lm.name());
            }
        } catch (Exception e) {
            // ignore - native queries don't support getLockMode()
        }

        // Remove any existing query with this name, then add the new one
        repos.removeQueryMetaData(repos.getQueryMetaData(null, name,
            _factory.getConfiguration().getClassResolverInstance()
                .getClassLoader(null, null), false));
        repos.addQueryMetaData(metaData);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        if (cls.isInstance(this)) {
            return cls.cast(this);
        }
        throw new jakarta.persistence.PersistenceException(this + " is not a " + cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        initEntityGraphs();
        if (entityGraph instanceof EntityGraphImpl) {
            EntityGraphImpl<T> copy = ((EntityGraphImpl<T>) entityGraph).copyWithName(graphName);
            _entityGraphs.put(graphName, copy);
        } else {
            throw new IllegalArgumentException("Unknown EntityGraph implementation: "
                + entityGraph.getClass());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType) {
        initEntityGraphs();
        Map<String, EntityGraph<? extends E>> result = new HashMap<>();
        for (Map.Entry<String, EntityGraphImpl<?>> entry : _entityGraphs.entrySet()) {
            if (entityType.isAssignableFrom(entry.getValue().getEntityType())) {
                result.put(entry.getKey(), (EntityGraph<? extends E>) entry.getValue());
            }
        }
        return result;
    }

    EntityGraphImpl<?> getEntityGraphImpl(String graphName) {
        initEntityGraphs();
        return _entityGraphs.get(graphName);
    }

    @SuppressWarnings("unchecked")
    <T> List<EntityGraph<? super T>> getEntityGraphsForType(Class<T> entityClass) {
        initEntityGraphs();
        List<EntityGraph<? super T>> result = new ArrayList<>();
        for (EntityGraphImpl<?> eg : _entityGraphs.values()) {
            if (entityClass.isAssignableFrom(eg.getEntityType())
                    || eg.getEntityType().isAssignableFrom(entityClass)) {
                result.add((EntityGraph<? super T>) eg);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void initEntityGraphs() {
        if (_entityGraphsInitialized) return;
        synchronized (_entityGraphs) {
            if (_entityGraphsInitialized) return;
            MetaDataRepository mdr = getConfiguration()
                .getMetaDataRepositoryInstance();

            // First, check MDR for annotation-parser-sourced metadata
            Collection<EntityGraphMetaData> metas =
                mdr.getEntityGraphMetaDatas();
            MetamodelImpl mm = getMetamodel();

            if (metas.isEmpty()) {
                // Annotation parser may not have run in the right mode.
                // Scan entity classes directly for @NamedEntityGraph.
                for (org.apache.openjpa.meta.ClassMetaData cmd
                        : mdr.getMetaDatas()) {
                    Class<?> cls = cmd.getDescribedType();
                    scanNamedEntityGraphs(cls, mdr);
                }
                metas = mdr.getEntityGraphMetaDatas();
            }

            for (EntityGraphMetaData egm : metas) {
                EntityGraphImpl<?> eg = buildEntityGraph(egm, mm);
                _entityGraphs.put(eg.getName(), eg);
            }
            _entityGraphsInitialized = true;
        }
    }

    private void scanNamedEntityGraphs(Class<?> cls,
            MetaDataRepository mdr) {
        jakarta.persistence.NamedEntityGraphs negs =
            cls.getAnnotation(jakarta.persistence.NamedEntityGraphs.class);
        if (negs != null) {
            for (jakarta.persistence.NamedEntityGraph neg : negs.value()) {
                addEntityGraphFromAnnotation(cls, neg, mdr);
            }
        }
        jakarta.persistence.NamedEntityGraph neg =
            cls.getAnnotation(jakarta.persistence.NamedEntityGraph.class);
        if (neg != null) {
            addEntityGraphFromAnnotation(cls, neg, mdr);
        }
    }

    private void addEntityGraphFromAnnotation(Class<?> cls,
            jakarta.persistence.NamedEntityGraph graph,
            MetaDataRepository mdr) {
        String graphName = graph.name();
        if (graphName == null || graphName.isEmpty()) {
            jakarta.persistence.Entity entityAnno =
                cls.getAnnotation(jakarta.persistence.Entity.class);
            if (entityAnno != null && entityAnno.name() != null
                    && !entityAnno.name().isEmpty()) {
                graphName = entityAnno.name();
            } else {
                graphName = cls.getSimpleName();
            }
        }

        EntityGraphMetaData egm = new EntityGraphMetaData();
        egm.setName(graphName);
        egm.setEntityClass(cls);
        egm.setIncludeAllAttributes(graph.includeAllAttributes());

        for (jakarta.persistence.NamedAttributeNode node
                : graph.attributeNodes()) {
            egm.getAttributeNodes().add(
                new EntityGraphMetaData.AttributeNodeData(
                    node.value(), node.subgraph(), node.keySubgraph()));
        }

        for (jakarta.persistence.NamedSubgraph sg : graph.subgraphs()) {
            EntityGraphMetaData.SubgraphData sgData =
                new EntityGraphMetaData.SubgraphData(sg.name(), sg.type());
            for (jakarta.persistence.NamedAttributeNode node
                    : sg.attributeNodes()) {
                sgData.getAttributeNodes().add(
                    new EntityGraphMetaData.AttributeNodeData(
                        node.value(), node.subgraph(), node.keySubgraph()));
            }
            egm.getSubgraphs().add(sgData);
        }

        for (jakarta.persistence.NamedSubgraph sg
                : graph.subclassSubgraphs()) {
            EntityGraphMetaData.SubgraphData sgData =
                new EntityGraphMetaData.SubgraphData(sg.name(), sg.type());
            for (jakarta.persistence.NamedAttributeNode node
                    : sg.attributeNodes()) {
                sgData.getAttributeNodes().add(
                    new EntityGraphMetaData.AttributeNodeData(
                        node.value(), node.subgraph(), node.keySubgraph()));
            }
            egm.getSubclassSubgraphs().add(sgData);
        }

        mdr.addEntityGraphMetaData(graphName, egm);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private EntityGraphImpl<?> buildEntityGraph(EntityGraphMetaData egm,
            MetamodelImpl mm) {
        EntityGraphImpl eg = new EntityGraphImpl(
            egm.getName(), egm.getEntityClass(), mm);

        // add explicit attribute nodes
        for (EntityGraphMetaData.AttributeNodeData nodeData
                : egm.getAttributeNodes()) {
            eg.addAttributeNodeDirect(nodeData.attributeName());
        }

        // build subgraphs by name for wiring
        Map<String, SubgraphImpl<?>> subgraphMap = new HashMap<>();
        for (EntityGraphMetaData.SubgraphData sgData : egm.getSubgraphs()) {
            Class<?> sgType = sgData.getType();
            if (sgType == void.class || sgType == Object.class) {
                // default type — not specified in annotation
                sgType = egm.getEntityClass();
            }
            SubgraphImpl sg = new SubgraphImpl(sgType, mm);
            for (EntityGraphMetaData.AttributeNodeData nodeData
                    : sgData.getAttributeNodes()) {
                sg.addAttributeNodeDirect(nodeData.attributeName());
            }
            subgraphMap.put(sgData.getName(), sg);
        }

        // wire subgraphs into attribute nodes
        for (EntityGraphMetaData.AttributeNodeData nodeData
                : egm.getAttributeNodes()) {
            String sgRef = nodeData.subgraphName();
            if (sgRef != null && !sgRef.isEmpty()) {
                SubgraphImpl<?> sg = subgraphMap.get(sgRef);
                if (sg != null) {
                    AttributeNodeImpl<?> node =
                        eg.getOrCreateNode(nodeData.attributeName());
                    node.addSubgraph(sg.getClassType(), sg);
                }
            }
            String keySgRef = nodeData.keySubgraphName();
            if (keySgRef != null && !keySgRef.isEmpty()) {
                SubgraphImpl<?> sg = subgraphMap.get(keySgRef);
                if (sg != null) {
                    AttributeNodeImpl<?> node =
                        eg.getOrCreateNode(nodeData.attributeName());
                    node.addKeySubgraph(sg.getClassType(), sg);
                }
            }
        }

        return eg;
    }
    
    /**
     * Get the identifier for the specified entity.  If not managed by any
     * of the em's in this PU or not persistence capable, return null.
     */
    @Override
    public Object getIdentifier(Object entity) {
        if (!ImplHelper.isManageable(entity)) {
            throw new IllegalArgumentException(_loc.get("invalid_entity_argument",
                    "getIdentifier", entity == null ? "null" : Exceptions.toString(entity)).getMessage());
        }
        Object id = OpenJPAPersistenceUtil.getIdentifier(this, entity);
        if (id != null) {
            return id;
        }
        // For unmanaged entities (new, detached, or not yet persisted),
        // read the identity field value directly from the entity using
        // metadata and reflection.
        return getIdentifierFromFields(entity);
    }

    /**
     * Reads the identity field value directly from the entity using
     * metadata, supporting both enhanced and unenhanced entities.
     */
    private Object getIdentifierFromFields(Object entity) {
        try {
            MetaDataRepository repos = _factory.getConfiguration()
                .getMetaDataRepositoryInstance();
            Class<?> cls = entity.getClass();
            org.apache.openjpa.meta.ClassMetaData meta =
                repos.getMetaData(cls, null, false);
            if (meta == null) {
                return null;
            }
            org.apache.openjpa.meta.FieldMetaData[] pkFields =
                meta.getPrimaryKeyFields();
            if (pkFields == null || pkFields.length == 0) {
                return null;
            }
            if (pkFields.length == 1) {
                java.lang.reflect.Member member = pkFields[0].getBackingMember();
                if (member instanceof java.lang.reflect.Field f) {
                    f.setAccessible(true);
                    return f.get(entity);
                } else if (member instanceof java.lang.reflect.Method m) {
                    m.setAccessible(true);
                    return m.invoke(entity);
                }
            }
        } catch (Exception e) {
            // If reflection fails, return null
        }
        return null;
    }

    @Override
    public boolean isLoaded(Object entity) {
        return isLoaded(entity, (String) null);
    }

    @Override
    public boolean isLoaded(Object entity, String attribute) {
        if (entity == null) {
            return false;
        }
        return (OpenJPAPersistenceUtil.isManagedBy(this, entity) &&
                (OpenJPAPersistenceUtil.isLoaded(entity, attribute) == LoadState.LOADED));
    }
    
    @Override
    public <E> boolean isLoaded(E entity, Attribute<? super E, ?> attribute) {
    	return isLoaded(entity, attribute.getName());
    }
    
    @Override
    public SchemaManager getSchemaManager() {
    	if (!this.isOpen()) {
    		throw new IllegalStateException("EntityManagerFactory is closed.");
    	}
    	return new SchemaManagerImpl(_factory);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> R callInTransaction(Function<EntityManager, R> work) {
    	EntityManager em = createEntityManager();
    	boolean startedTransaction = false;
    	boolean jtaTransaction = getTransactionType() == PersistenceUnitTransactionType.JTA;
    	Broker broker = em.unwrap(Broker.class);
    	try {
    		if (jtaTransaction) {
    			if (!broker.syncWithManagedTransaction()) {
    				broker.begin();
    				startedTransaction = true;
    			}
    		} else {
    			em.getTransaction().begin();
    			startedTransaction = true;
    		}
    		R result = work.apply(em);
    		if (startedTransaction) {
    			if (jtaTransaction) {
    				broker.commit();
    			} else {
    				em.getTransaction().commit();
    			}
    		}
    		// For unenhanced (runtime-subclassed) entities, the user's function
    		// may return an original POJO that was passed to persist().
    		// Entities loaded via find() on other EMs are subclass instances,
    		// causing getClass() mismatches in equals(). To ensure consistent
    		// class identity, re-find managed entities from the database so that
    		// the returned instance is a subclass (matching what find() returns).
    		if (result != null && em.contains(result)) {
    			try {
    				Object id = getPersistenceUnitUtil().getIdentifier(result);
    				if (id != null) {
    					Class<R> entityClass = (Class<R>) result.getClass();
    					em.clear();
    					R refound = em.find(entityClass, id);
    					if (refound != null) {
    						result = refound;
    					}
    				}
    			} catch (Exception e) {
    				// If re-find fails, return original result
    			}
    		}
    		return result;
    	} catch (Exception ex) {
    		if (jtaTransaction) {
    			broker.rollback();
    		} else {
    			try {
    				em.getTransaction().rollback();
    			} catch (Exception rollbackEx) {
    				// Transaction may already be rolled back
    			}
    		}
    		throw new UserException(ex.getMessage(), ex);
    	} finally {
    		em.close();
    	}
    }
    
    @Override
    public void runInTransaction(Consumer<EntityManager> work) {
    	callInTransaction(em -> {
    		work.accept(em);
    		return null;
    	});
    }
    
    @Override
    public <T> Class<? extends T> getClass(T entity) {
    	if (!OpenJPAPersistenceUtil.isManagedBy(this, entity)) {
    		throw new jakarta.persistence.PersistenceException(_loc.get("invalid_entity_argument",
                    "getClass", entity == null ? "null" : Exceptions.toString(entity)).getMessage());
    	}
    	return OpenJPAPersistenceUtil.getClass(this, entity);
    }
    
    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
    }
    
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
    	return "managed".equalsIgnoreCase(_factory.getConfiguration().getTransactionMode())
    			? PersistenceUnitTransactionType.JTA
    			: PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }
    
    @Override
    public Object getVersion(Object entity) {
    	if (!OpenJPAPersistenceUtil.isManagedBy(this, entity)) {
    		throw new IllegalArgumentException(_loc.get("invalid_entity_argument",
                    "load", entity == null ? "null" : Exceptions.toString(entity)).getMessage());
    	}
    	return OpenJPAPersistenceUtil.getVersion(this, entity);
    }
    
    @Override
    public boolean isInstance(Object entity, Class<?> entityClass) {
        if (entity == null || entityClass == null) {
            return false;
        }
        if (!OpenJPAPersistenceUtil.isManagedBy(this, entity)) {
            return false;
        }
        // Use the metadata's described type to handle unenhanced entity subclasses
        Class<?> entityType = OpenJPAPersistenceUtil.getClass(this, entity);
        if (entityType != null) {
            return entityClass.isAssignableFrom(entityType);
        }
        return entityClass.isAssignableFrom(entity.getClass());
    }
    
    @Override
    public void load(Object entity) {
    	if (!OpenJPAPersistenceUtil.isManagedBy(this, entity)) {
    		throw new IllegalArgumentException(_loc.get("invalid_entity_argument",
                    "load", entity == null ? "null" : Exceptions.toString(entity)).getMessage());
    	}
    	OpenJPAPersistenceUtil.load(this, entity);
    }
    
    @Override
    public void load(Object entity, String attributeName) {
    	if (!OpenJPAPersistenceUtil.isManagedBy(this, entity)) {
    		throw new IllegalArgumentException(_loc.get("invalid_entity_argument",
                    "load", entity == null ? "null" : Exceptions.toString(entity)).getMessage());
    	}
    	OpenJPAPersistenceUtil.load(this, entity, attributeName);
    }
    
    @Override
    public <E> void load(E entity, Attribute<? super E, ?> attribute) {
    	load(entity, attribute.getName());
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

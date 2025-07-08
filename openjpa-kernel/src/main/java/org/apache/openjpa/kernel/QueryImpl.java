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
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.Constant;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Val;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.rop.BatchedResultObjectProvider;
import org.apache.openjpa.lib.rop.EagerResultList;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.ClassUtil;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.lib.util.collections.AbstractReferenceMap;
import org.apache.openjpa.lib.util.collections.LinkedMap;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.NoResultException;
import org.apache.openjpa.util.NonUniqueResultException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;


/**
 * Implementation of the {@link Query} interface.
 *
 * @author Abe White
 */
public class QueryImpl implements Query {
    private static final long serialVersionUID = 1L;

    private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

    private final String _language;
    private final StoreQuery _storeQuery;
    private transient final BrokerImpl _broker;
    private transient final Log _log;
    private transient ClassLoader _loader = null;

    // query has its own internal lock
    private ReentrantLock _lock;

    // unparsed state
    private Class<?> _class = null;
    private boolean _subclasses = true;
    private boolean _readOnly = false;
    private String _query = null;
    private String _params = null;

    // parsed state
    private transient Compilation _compiled = null;
    private transient boolean _compiling = false;
    private transient ResultPacker _packer = null;

    // candidates
    private transient Collection<?> _collection = null;
    private transient Extent _extent = null;

    // listeners
    private Map<String,FilterListener> _filtListeners = null;
    private Map<String,AggregateListener> _aggListeners = null;

    // configuration for loading objects
    private FetchConfiguration _fc = null;
    private boolean _ignoreChanges = false;
    private Class<?> _resultMappingScope = null;
    private String _resultMappingName = null;

    // these fields should only be used directly after we have a compilation,
    // because their values may be encoded in the query string
    private Boolean _unique = null;
    private Class<?> _resultClass = null;
    private transient long _startIdx = 0;
    private transient long _endIdx = Long.MAX_VALUE;
    private transient boolean _rangeSet = false;

    // remember the list of all the results we have returned so we
    // can free their resources when close or closeAll is called
    private transient final Collection<RemoveOnCloseResultList> _resultLists =
        new ReferenceHashSet(AbstractReferenceMap.ReferenceStrength.WEAK);

    private boolean _printParameters = false;
    /**
     * Construct a query managed by the given broker.
     */
    public QueryImpl(Broker broker, String language, StoreQuery storeQuery) {
        _broker = (BrokerImpl) broker;
        _language = language;
        _storeQuery = storeQuery;
        _fc = (FetchConfiguration) broker.getFetchConfiguration().clone();
        _log = broker.getConfiguration().getLog(OpenJPAConfiguration.LOG_QUERY);
        _storeQuery.setContext(this);
        _printParameters = _broker.getPrintParameters();
        if (_broker != null && _broker.getMultithreaded())
            _lock = new ReentrantLock();
    }

    /**
     * Internal store query.
     */
    public StoreQuery getStoreQuery() {
        return _storeQuery;
    }

    @Override
    public Broker getBroker() {
        return _broker;
    }

    @Override
    public Query getQuery() {
        return this;
    }

    @Override
    public StoreContext getStoreContext() {
        return _broker;
    }

    @Override
    public String getLanguage() {
        return _language;
    }

    @Override
    public FetchConfiguration getFetchConfiguration() {
        return _fc;
    }

    @Override
    public String getQueryString() {
        return _query;
    }

    @Override
    public boolean getIgnoreChanges() {
        assertOpen();
        return _ignoreChanges;
    }

    @Override
    public void setIgnoreChanges(boolean flag) {
        lock();
        try {
            assertOpen();
            // allowed modification: no read-only check
            _ignoreChanges = flag;
        } finally {
            unlock();
        }
    }

    @Override
    public boolean isReadOnly() {
        assertOpen();
        return _readOnly;
    }

    @Override
    public void setReadOnly(boolean flag) {
        lock();
        try {
            assertOpen();
            _readOnly = flag;
        } finally {
            unlock();
        }
    }

    @Override
    public void addFilterListener(FilterListener listener) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (_filtListeners == null)
                _filtListeners = new HashMap<>(5);
            _filtListeners.put(listener.getTag(), listener);
        } finally {
            unlock();
        }
    }

    @Override
    public void removeFilterListener(FilterListener listener) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (_filtListeners != null)
                _filtListeners.remove(listener.getTag());
        } finally {
            unlock();
        }
    }

    @Override
    public Collection<FilterListener> getFilterListeners() {
        if (_filtListeners == null)
            return Collections.emptyList();
        else
            return _filtListeners.values();
    }

    @Override
    public FilterListener getFilterListener(String tag) {
        // first check listeners for this query
        if (_filtListeners != null) {
            FilterListener listen = _filtListeners.get(tag);
            if (listen != null)
                return listen;
        }

        // check user-defined listeners from configuration
        FilterListener[] confListeners = _broker.getConfiguration().
            getFilterListenerInstances();
        for (FilterListener confListener : confListeners)
            if (confListener.getTag().equals(tag))
                return confListener;

        // check store listeners
        return _storeQuery.getFilterListener(tag);
    }

    @Override
    public void addAggregateListener(AggregateListener listener) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (_aggListeners == null)
                _aggListeners = new HashMap<>(5);
            _aggListeners.put(listener.getTag(), listener);
        } finally {
            unlock();
        }
    }

    @Override
    public void removeAggregateListener(AggregateListener listener) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (_aggListeners != null)
                _aggListeners.remove(listener.getTag());
        } finally {
            unlock();
        }
    }

    @Override
    public Collection<AggregateListener> getAggregateListeners() {
        if (_aggListeners == null)
            return Collections.emptyList();
        else
            return _aggListeners.values();
    }

    @Override
    public AggregateListener getAggregateListener(String tag) {
        // first check listeners for this query
        if (_aggListeners != null) {
            AggregateListener listen = _aggListeners.
                get(tag);
            if (listen != null)
                return listen;
        }

        // check user-defined listeners from configuration
        AggregateListener[] confListeners = _broker.getConfiguration().
            getAggregateListenerInstances();
        for (AggregateListener confListener : confListeners)
            if (confListener.getTag().equals(tag))
                return confListener;

        // check store listeners
        return _storeQuery.getAggregateListener(tag);
    }

    @Override
    public Extent getCandidateExtent() {
        // if just the class is set, fetch the corresponding extent; if the
        // extent is already set but its ignore cache setting is wrong,
        // get a new extent with the correct setting (don't modify orig extent
        // in case the user has a reference to it and might use it)
        lock();
        try {
            Class<?> cls = getCandidateType();
            if (_extent == null && _collection == null && _broker != null
                && cls != null) {
                _extent = _broker.newExtent(cls, _subclasses);
                _extent.setIgnoreChanges(_ignoreChanges);
            } else if (_extent != null
                && _extent.getIgnoreChanges() != _ignoreChanges && cls != null){
                _extent = _broker.newExtent(cls, _extent.hasSubclasses());
                _extent.setIgnoreChanges(_ignoreChanges);
            }
            return _extent;
        } finally {
            unlock();
        }
    }

    @Override
    public void setCandidateExtent(Extent candidateExtent) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();

            if (candidateExtent == _extent)
                return;
            if (candidateExtent == null) {
                _extent = null;
                return;
            }

            // if extent then not collection
            _extent = candidateExtent;
            _collection = null;

            boolean invalidate = false;
            if (_extent.getElementType() != _class) {
                _class = _extent.getElementType();
                _loader = null;
                invalidate = true;
            }
            if (_extent.hasSubclasses() != _subclasses) {
                _subclasses = _extent.hasSubclasses();
                invalidate = true;
            }
            if (invalidate)
                invalidateCompilation();
        } finally {
            unlock();
        }
    }

    @Override
    public Collection<?> getCandidateCollection() {
        assertOpen();
        return _collection;
    }

    @Override
    public void setCandidateCollection(Collection<?> candidateCollection) {
        if (!_storeQuery.supportsInMemoryExecution())
            throw new UnsupportedException(_loc.get("query-nosupport",
                _language));

        lock();
        try {
            assertOpen();

            // if collection then not extent
            _collection = candidateCollection;
            if (_collection != null)
                _extent = null;
        } finally {
            unlock();
        }
    }

    @Override
    public Class getCandidateType() {
        lock();
        try {
            assertOpen();
            if (_class != null || _compiled != null || _query == null
                || _broker == null)
                return _class;

            // check again after compilation; maybe encoded in string
            compileForCompilation();
            return _class;
        } finally {
            unlock();
        }
    }

    @Override
    public void setCandidateType(Class candidateClass, boolean subs) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            _class = candidateClass;
            _subclasses = subs;
            _loader = null;
            invalidateCompilation();
        } finally {
            unlock();
        }
    }

    @Override
    public boolean hasSubclasses() {
        return _subclasses;
    }

    @Override
    public String getResultMappingName() {
        assertOpen();
        return _resultMappingName;
    }

    @Override
    public Class getResultMappingScope() {
        assertOpen();
        return _resultMappingScope;
    }

    @Override
    public void setResultMapping(Class<?> scope, String name) {
        lock();
        try {
            assertOpen();
            _resultMappingScope = scope;
            _resultMappingName = name;
            _packer = null;
        } finally {
            unlock();
        }
    }

    @Override
    public boolean isUnique() {
        lock();
        try {
            assertOpen();
            if (_unique != null)
                return _unique;
            if ((_query == null && _language.endsWith("JPQL")) || _compiling || _broker == null)
                return false;

            // check again after compilation; maybe encoded in string
            if (_compiled == null) {
                compileForCompilation();
                if (_unique != null)
                    return _unique;
            }

            // no explicit setting; default
            StoreQuery.Executor ex = compileForExecutor();
            if (!ex.isAggregate(_storeQuery))
                return false;
            return !ex.hasGrouping(_storeQuery);
        } finally {
            unlock();
        }
    }

    /**
     * Affirms if this query has originated by parsing a string-based query.
     */
    public boolean isParsedQuery() {
        return getQueryString() != null;
    }

    @Override
    public void setUnique(boolean unique) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            _unique = (unique) ? Boolean.TRUE : Boolean.FALSE;
        } finally {
            unlock();
        }
    }

    @Override
    public Class getResultType() {
        lock();
        try {
            assertOpen();
            if (_resultClass != null || _compiled != null || _query == null
                || _broker == null)
                return _resultClass;

            // check again after compilation; maybe encoded in string
            compileForCompilation();
            return _resultClass;
        } finally {
            unlock();
        }
    }

    @Override
    public void setResultType(Class cls) {
        lock();
        try {
            assertOpen();
            // allowed modification: no read-only check
            _resultClass = cls;
            _packer = null;
        } finally {
            unlock();
        }
    }

    @Override
    public long getStartRange() {
        assertOpen();
        return _startIdx;
    }

    @Override
    public long getEndRange() {
        assertOpen();
        return _endIdx;
    }

    @Override
    public void setRange(long start, long end) {
        if (start < 0 || end < 0)
            throw new UserException(_loc.get("invalid-range",
                String.valueOf(start), String.valueOf(end)));

        if (end - start > Integer.MAX_VALUE && end != Long.MAX_VALUE)
            throw new UserException(_loc.get("range-too-big",
                String.valueOf(start), String.valueOf(end)));

        lock();
        try {
            assertOpen();
            // allowed modification: no read-only check
            _startIdx = start;
            _endIdx = end;
            _rangeSet = true;
        } finally {
            unlock();
        }
    }

    @Override
    public String getParameterDeclaration() {
        lock();
        try {
            assertOpen();
            if (_params != null || _compiled != null || _compiling
                || _broker == null)
                return _params;

            // check again after compilation; maybe encoded in string
            compileForCompilation();
            return _params;
        } finally {
            unlock();
        }
    }

    @Override
    public void declareParameters(String params) {
        if (!_storeQuery.supportsParameterDeclarations())
            throw new UnsupportedException(_loc.get("query-nosupport",
                _language));

        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            _params = StringUtil.trimToNull(params);
            invalidateCompilation();
        } finally {
            unlock();
        }
    }

    @Override
    public void compile() {
        lock();
        try {
            assertOpen();
            StoreQuery.Executor ex = compileForExecutor();
            getResultPacker(_storeQuery, ex);
            ex.validate(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public Object getCompilation() {
        lock();
        try {
            return compileForCompilation().storeData;
        } finally {
            unlock();
        }
    }

    /**
     * Compile query properties.
     */
    private Compilation compileForCompilation() {
        if (_compiled != null || _compiling)
            return _compiled;

        assertNotSerialized();
        assertOpen();

        boolean readOnly = _readOnly;
        _readOnly = false;
        _compiling = true;
        try {
            _compiled = compilationFromCache();
            return _compiled;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        } finally {
            _compiling = false;
            _readOnly = readOnly;
        }
    }

    /**
     * Find the cached compilation for the current query, creating one if it
     * does not exist.
     */
    @SuppressWarnings("unchecked")
    protected Compilation compilationFromCache() {
        Map compCache = _broker.getConfiguration().getQueryCompilationCacheInstance();
        if (compCache == null || !isParsedQuery()) {
            return newCompilation();
        } else {
            CompilationKey key = new CompilationKey();
            key.queryType = _storeQuery.getClass();
            key.candidateType = getCandidateType();
            key.subclasses = hasSubclasses();
            key.query = getQueryString();
            key.language = getLanguage();
            key.storeKey = _storeQuery.newCompilationKey();
            Compilation comp = (Compilation) compCache.get(key);

            // parse declarations if needed
            if (comp == null) {
                comp = newCompilation();
                // only cache those queries that can be compiled
                if (comp.storeData != null) {

                    synchronized (compCache) {
                        Compilation existingComp = (Compilation) compCache.get(key);
                        if (existingComp == null) {
                            compCache.put(key, comp);
                        } else {
                            comp = existingComp;
                        }
                    }
                }
            } else {
                _storeQuery.populateFromCompilation(comp.storeData);
            }

            return comp;
        }
    }

    /**
     * Create and populate a new compilation.
     */
    private Compilation newCompilation() {
        Compilation comp = new Compilation();
        comp.storeData = _storeQuery.newCompilation();
        _storeQuery.populateFromCompilation(comp.storeData);
        return comp;
    }

    /**
     * Compile for execution, choosing between datastore and in-mem
     * compilation based on what we support and our settings.
     */
    private StoreQuery.Executor compileForExecutor() {
        Compilation comp = compileForCompilation();
        if (_collection == null) {
            if (comp.datastore != null)
                return comp.datastore;
            if (comp.memory != null)
                return comp.memory;
            if (_storeQuery.supportsDataStoreExecution())
                return compileForDataStore(comp);
            return compileForInMemory(comp);
        }

        if (comp.memory != null)
            return comp.memory;
        if (comp.datastore != null)
            return comp.datastore;
        if (_storeQuery.supportsInMemoryExecution())
            return compileForInMemory(comp);
        return compileForDataStore(comp);
    }

    /**
     * Create an expression tree for datastore execution.
     */
    private StoreQuery.Executor compileForDataStore(Compilation comp) {
        if (comp.datastore == null)
            comp.datastore = createExecutor(false);
        return comp.datastore;
    }

    /**
     * Create an expression tree for in-memory execution.
     */
    private StoreQuery.Executor compileForInMemory(Compilation comp) {
        if (comp.memory == null)
            comp.memory = createExecutor(true);
        return comp.memory;
    }

    /**
     * Return a query executor of the proper type.
     */
    private StoreQuery.Executor createExecutor(boolean inMem) {
        assertCandidateType();

        MetaDataRepository repos = _broker.getConfiguration().
            getMetaDataRepositoryInstance();
        ClassMetaData meta = repos.getMetaData(_class,
            _broker.getClassLoader(), false);

        ClassMetaData[] metas;
        if (_class == null || _storeQuery.supportsAbstractExecutors())
            metas = new ClassMetaData[]{ meta };
        else if (_subclasses && (meta == null || meta.isManagedInterface()))
            metas = repos.getImplementorMetaDatas(_class,
                _broker.getClassLoader(), true);
        else if (meta != null && (_subclasses || meta.isMapped()))
            metas = new ClassMetaData[]{ meta };
        else
            metas = StoreQuery.EMPTY_METAS;

        if (metas.length == 0)
            throw new UserException(_loc.get("no-impls", _class));
        try {
            if (metas.length == 1) {
                if (inMem)
                    return _storeQuery.newInMemoryExecutor(metas[0],
                        _subclasses);
                return _storeQuery.newDataStoreExecutor(metas[0], _subclasses);
            }

            // multiple implementors
            StoreQuery.Executor[] es = new StoreQuery.Executor[metas.length];
            for (int i = 0; i < es.length; i++) {
                if (inMem)
                    es[i] = _storeQuery.newInMemoryExecutor(metas[i], true);
                else
                    es[i] = _storeQuery.newDataStoreExecutor(metas[i], true);
            }
            return new MergedExecutor(es);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (RuntimeException re) {
            throw new GeneralException(re);
        }
    }

    /**
     * Clear any compilation, forcing this query to be recompiled
     * next time it's executed. This should be invoked whenever any
     * state changes that would cause the underlying query
     * representation to change.
     *
     * @since 0.3.0
     */
    private boolean invalidateCompilation() {
        if (_compiling)
            return false;
        _storeQuery.invalidateCompilation();
        _compiled = null;
        _packer = null;
        return true;
    }

    @Override
    public Object execute() {
        return execute((Object[]) null);
    }

    @Override
    public Object execute(Object[] params) {
        return execute(OP_SELECT, params);
    }

    @Override
    public Object execute(Map params) {
        return execute(OP_SELECT, params);
    }

    private Object execute(int operation, Object[] params) {
        if (params == null)
            params = StoreQuery.EMPTY_OBJECTS;

        lock();
        try {
            assertNotSerialized();
            _broker.beginOperation(true);
            try {
                assertOpen();
                _broker.assertNontransactionalRead();

                // get executor
                Compilation comp = compileForCompilation();
                StoreQuery.Executor ex = (isInMemory(operation))
                    ? compileForInMemory(comp) : compileForDataStore(comp);

                assertParameters(_storeQuery, ex, params);
                if (_log.isTraceEnabled())
                    logExecution(operation, ex.getOrderedParameterTypes(_storeQuery),
                        params);

                if (operation == OP_SELECT)
                    return execute(_storeQuery, ex, params);
                if (operation == OP_DELETE)
                    return delete(_storeQuery, ex, params);
                if (operation == OP_UPDATE)
                    return update(_storeQuery, ex, params);
                throw new UnsupportedException();
            } catch (OpenJPAException ke) {
                throw ke;
            } catch (Exception e) {
                throw new UserException(e);
            } finally {
                _broker.endOperation();
            }
        }
        finally {
            unlock();
        }
    }

    private Object execute(int operation, Map params) {
        if (params == null)
            params = Collections.EMPTY_MAP;

        lock();
        try {
            _broker.beginOperation(true);
            try {
                assertNotSerialized();
                assertOpen();
                _broker.assertNontransactionalRead();

                // get executor
                Compilation comp = compileForCompilation();
                StoreQuery.Executor ex = (isInMemory(operation))
                    ? compileForInMemory(comp) : compileForDataStore(comp);

                assertParameters(_storeQuery, ex, params);
                Object[] arr = (params.isEmpty()) ? StoreQuery.EMPTY_OBJECTS :
                    ex.toParameterArray(_storeQuery, params);
                if (_log.isTraceEnabled())
                    logExecution(operation, params);

                if (operation == OP_SELECT)
                    return execute(_storeQuery, ex, arr);
                if (operation == OP_DELETE)
                    return delete(_storeQuery, ex, arr);
                if (operation == OP_UPDATE)
                    return update(_storeQuery, ex, arr);
                throw new UnsupportedException();
            } catch (OpenJPAException ke) {
                throw ke;
            } catch (Exception e) {
                throw new UserException(_loc.get("query-execution-error",
                        _query), e);
            } finally {
                _broker.endOperation();
            }
        }
        finally {
            unlock();
        }
    }

    @Override
    public long deleteAll() {
        return deleteAll((Object[]) null);
    }

    @Override
    public long deleteAll(Object[] params) {
        return ((Number) execute(OP_DELETE, params)).longValue();
    }

    @Override
    public long deleteAll(Map params) {
        return ((Number) execute(OP_DELETE, params)).longValue();
    }

    @Override
    public long updateAll() {
        return updateAll((Object[]) null);
    }

    @Override
    public long updateAll(Object[] params) {
        return ((Number) execute(OP_UPDATE, params)).longValue();
    }

    @Override
    public long updateAll(Map params) {
        return ((Number) execute(OP_UPDATE, params)).longValue();
    }

    /**
     * Converts the values of given <code>params</code> Map into an array in
     * consultation of the <code>paramTypes</code> Map.
     *
     * The indexing of the resultant array is significant for following
     * interrelated but tacit assumptions:
     * The values in the returned Object[] is consumed by {@link Parameter}
     * expressions. Query parsing creates these Parameters and sets their
     * key and index. The index set on the Parameter by the parser is the
     * same index used to access the Object[] elements returned by this method.
     *
     * {@link JPQLExpressionBuilder} creates and populates parameters as
     * follows:
     * The parameter key is not the token encountered by the parser, but
     * converted to Integer or String based on the context in which the token
     * appeared.
     * The index for positional (Integer) parameter is the value of the key
     * minus 1.
     * The index for named (String) parameter is the order in which the
     * token appeared to parser during scanning.
     *
     *
     * The first LinkedMap argument to this method is the result of parsing.
     * This LinkedMap contains the parameter key and their expected
     * (if determinable) value types. That it is a LinkedMap points to the
     * fact that an ordering is implicit. The ordering of the keys in this Map
     * is the same as the order in which parser encountered the parameter
     * tokens.
     *
     * For example, parsing result of the following two JPQL queries
     *   a) UPDATE CompUser e SET e.name= ?1, e.age = ?2 WHERE e.userid = ?3
     *   b) UPDATE CompUser e SET e.name= :name, e.age = :age WHERE e.userid =
     *          :id
     * The parameter keys will appear in the order (3,2,1) or (:id, :name, :age)
     * in the given LinkedMap because WHERE clause is parsed before SET clause.
     * The corresponding Parameter Expressions created by the parser will have
     * following key and index:
     *    a) 1:0, 2:1, 3:2
     *    b) name:1, age:2, id:0
     *
     * The purpose of this method is to produce an Object[] with an indexing
     * scheme that matches the indexing of the Parameter Expression.
     * The user map (the second argument) should produce the following Object[]
     * in two above-mentioned cases
     *   a) {1:"Shannon",2:29,3:5032} --> ["Shannon", 29, 5032]
     *   b) {"name":"Shannon", "age":29, "id":5032} --> [5032, "Shannon", 29]
     *
     */

    /**
     * Return whether we should execute this query in memory.
     */
    private boolean isInMemory(int operation) {
        // if there are any dirty instances in the current trans that are
        // involved in this query, we have to execute in memory or flush
        boolean inMem = !_storeQuery.supportsDataStoreExecution()
            || _collection != null;
        if (!inMem && (!_ignoreChanges || operation != OP_SELECT)
            && _broker.isActive() && isAccessPathDirty()) {
            int flush = _fc.getFlushBeforeQueries();
            if ((flush == FLUSH_TRUE
                || (flush == FLUSH_WITH_CONNECTION && _broker.hasConnection())
                || operation != OP_SELECT
                || !_storeQuery.supportsInMemoryExecution())
                && _broker.getConfiguration().supportedOptions().
                contains(OpenJPAConfiguration.OPTION_INC_FLUSH)) {
                _broker.flush();
            } else {
                if (_log.isInfoEnabled())
                    _log.info(_loc.get("force-in-mem", _class));
                inMem = true;
            }
        }

        if (inMem && !_storeQuery.supportsInMemoryExecution())
            throw new InvalidStateException(_loc.get("cant-exec-inmem",
                _language));
        return inMem;
    }

    /**
     * Execute the query using the given compilation, executor, and parameter
     * values. All other execute methods delegate to this one or to
     * {@link #execute(int, Map)} after validation and locking.
     */
    private Object execute(StoreQuery q, StoreQuery.Executor ex,
        Object[] params)
        throws Exception {
        // if this is an impossible result range, return null / empty list
        StoreQuery.Range range = new StoreQuery.Range(_startIdx, _endIdx);
        if (!_rangeSet)
            ex.getRange(q, params, range);
        if (range.start >= range.end)
            return emptyResult(q, ex);

        // execute; if we have a result class or we have only one result
        // and so need to remove it from its array, wrap in a packing rop
        range.lrs = isLRS(range.start, range.end);
        ResultObjectProvider rop = ex.executeQuery(q, params, range);
        try {
            return toResult(q, ex, rop, range);
        } catch (Exception e) {
            if (rop != null)
                try { rop.close(); } catch (Exception e2) {}
            throw e;
        }
    }

    /**
     * Delete the query using the given executor, and parameter
     * values. All other execute methods delegate to this one or to
     * {@link #delete(StoreQuery, StoreQuery.Executor, Object[])} after validation and locking.
     * The return value will be a Number indicating the number of
     * instances deleted.
     */
    private Number delete(StoreQuery q, StoreQuery.Executor ex, Object[] params)
        throws Exception {
        assertBulkModify(q, ex, params);
        return ex.executeDelete(q, params);
    }

    @Override
    public Number deleteInMemory(StoreQuery q, StoreQuery.Executor executor,
        Object[] params) {
        try {
            Object o = execute(q, executor, params);
            if (!(o instanceof Collection))
                o = Collections.singleton(o);

            int size = 0;
            for (Iterator i = ((Collection) o).iterator(); i.hasNext(); size++)
                _broker.delete(i.next(), null);
            return size;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new UserException(e);
        }
    }

    /**
     * Update the query using the given compilation, executor, and parameter
     * values. All other execute methods delegate to this one or to
     * {@link #update(StoreQuery, StoreQuery.Executor, Object[])} after validation and locking.
     * The return value will be a Number indicating the number of
     * instances updated.
     */
    private Number update(StoreQuery q, StoreQuery.Executor ex, Object[] params)
        throws Exception {
        assertBulkModify(q, ex, params);
        return ex.executeUpdate(q, params);
    }

    @Override
    public Number updateInMemory(StoreQuery q, StoreQuery.Executor executor,
        Object[] params) {
        try {
            Object o = execute(q, executor, params);
            if (!(o instanceof Collection))
                o = Collections.singleton(o);

            int size = 0;
            for (Iterator i = ((Collection) o).iterator(); i.hasNext(); size++)
                updateInMemory(i.next(), params, q);
            return size;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new UserException(e);
        }
    }

    /**
     * Set the values for the updates in memory.
     *
     * @param ob the persistent instance to change
     * @param params the parameters passed to the query
     */
    private void updateInMemory(Object ob, Object[] params, StoreQuery q) {
        for (Object o : getUpdates().entrySet()) {
            Entry e = (Entry) o;
            Path path = (Path) e.getKey();
            FieldMetaData fmd = path.last();
            OpenJPAStateManager sm = _broker.getStateManager(ob);

            Object val;
            Object value = e.getValue();
            if (value instanceof Val) {
                val = ((Val) value).
                        evaluate(ob, null, getStoreContext(), params);
            }
            else if (value instanceof Literal) {
                val = ((Literal) value).getValue();
            }
            else if (value instanceof Constant) {
                val = ((Constant) value).getValue(params);
            }
            else {
                try {
                    val = q.evaluate(value, ob, params, sm);
                }
                catch (UnsupportedException e1) {
                    throw new UserException(
                            _loc.get("fail-to-get-update-value"));
                }
            }

            int i = fmd.getIndex();
            PersistenceCapable into = ImplHelper.toPersistenceCapable(ob,
                    _broker.getConfiguration());

            // set the actual field in the instance
            int set = OpenJPAStateManager.SET_USER;
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.BOOLEAN:
                    sm.settingBooleanField(into, i, sm.fetchBooleanField(i),
                            val == null ? false : (Boolean) val,
                            set);
                    break;
                case JavaTypes.BYTE:
                    sm.settingByteField(into, i, sm.fetchByteField(i),
                            val == null ? 0 : ((Number) val).byteValue(), set);
                    break;
                case JavaTypes.CHAR:
                    sm.settingCharField(into, i, sm.fetchCharField(i),
                            val == null ? 0 : val.toString().charAt(0), set);
                    break;
                case JavaTypes.DOUBLE:
                    sm.settingDoubleField(into, i, sm.fetchDoubleField(i),
                            val == null ? 0 : ((Number) val).doubleValue(), set);
                    break;
                case JavaTypes.FLOAT:
                    sm.settingFloatField(into, i, sm.fetchFloatField(i),
                            val == null ? 0 : ((Number) val).floatValue(), set);
                    break;
                case JavaTypes.INT:
                    sm.settingIntField(into, i, sm.fetchIntField(i),
                            val == null ? 0 : ((Number) val).intValue(), set);
                    break;
                case JavaTypes.LONG:
                    sm.settingLongField(into, i, sm.fetchLongField(i),
                            val == null ? 0 : ((Number) val).longValue(), set);
                    break;
                case JavaTypes.SHORT:
                    sm.settingShortField(into, i, sm.fetchShortField(i),
                            val == null ? 0 : ((Number) val).shortValue(), set);
                    break;
                case JavaTypes.STRING:
                    sm.settingStringField(into, i, sm.fetchStringField(i),
                            val == null ? null : val.toString(), set);
                    break;
                case JavaTypes.DATE:
                case JavaTypes.NUMBER:
                case JavaTypes.BOOLEAN_OBJ:
                case JavaTypes.BYTE_OBJ:
                case JavaTypes.CHAR_OBJ:
                case JavaTypes.DOUBLE_OBJ:
                case JavaTypes.FLOAT_OBJ:
                case JavaTypes.INT_OBJ:
                case JavaTypes.LONG_OBJ:
                case JavaTypes.SHORT_OBJ:
                case JavaTypes.BIGDECIMAL:
                case JavaTypes.BIGINTEGER:
                case JavaTypes.LOCALE:
                case JavaTypes.OBJECT:
                case JavaTypes.OID:
                case JavaTypes.ENUM:
                    sm.settingObjectField(into, i, sm.fetchObjectField(i), val,
                            set);
                    break;
                default:
                    throw new UserException(_loc.get("only-update-primitives"));
            }
        }
    }

    /**
     * Trace log that the query is executing.
     */
    private void logExecution(int op, OrderedMap<Object, Class<?>> types, Object[] params) {
        OrderedMap<Object, Object> pmap = new OrderedMap<>();
        if (params.length > 0) {
            if (types != null && types.size() == params.length) {
                int i = 0;
                for (Object o : types.keySet()) {
                    pmap.put(o, params[i++]);
                }
            } else {
                for (int i = 0; i < params.length; i++)
                    pmap.put(String.valueOf(i), params[i]);
            }
        }
        logExecution(op, pmap);
    }

    /**
     * Trace log that the query is executing.
     */
    private void logExecution(int op, Map<Object, Object> params) {
        String s = _query;
        if (StringUtil.isEmpty(s))
            s = toString();

        String msg = "executing-query";
        if (!params.isEmpty()) {
            msg = "executing-query-with-params";
        }

        // If we aren't supposed to print parameters, replace values with '?'
        Object p = (_printParameters) ? params : "?";
        _log.trace(_loc.get(msg, s, p));
    }

    /**
     * Return whether this should be treated as a potential large result set.
     */
    private boolean isLRS(long start, long end) {
        long range = end - start;
        return _fc.getFetchBatchSize() >= 0
            && !(range <= _fc.getFetchBatchSize()
            || (_fc.getFetchBatchSize() == 0 && range <= 50));
    }

    /**
     * Return the query result for the given result object provider.
     */
    protected Object toResult(StoreQuery q, StoreQuery.Executor ex,
        ResultObjectProvider rop, StoreQuery.Range range)
        throws Exception {
        if (rop instanceof BatchedResultObjectProvider) {
            return new QueryResultCallback(this, q, ex, (BatchedResultObjectProvider) rop, range);
        }
        // pack projections if necessary
        String[] aliases = ex.getProjectionAliases(q);
        if (!ex.isPacking(q)) {
            ResultPacker packer = getResultPacker(q, ex);
            if (packer != null || aliases.length == 1)
                rop = new PackingResultObjectProvider(rop, packer,
                    aliases.length);
        }

        // if single result, extract it
        if (_unique == Boolean.TRUE || (aliases.length > 0
            && !ex.hasGrouping(q) && ex.isAggregate(q)))
            return singleResult(rop, range);

        // now that we've executed the query, we can call isAggregate and
        // hasGrouping efficiently
        boolean detach = (_broker.getAutoDetach() &
            AutoDetach.DETACH_NONTXREAD) > 0 && !_broker.isActive();
        boolean lrs = range.lrs && !ex.isAggregate(q) && !ex.hasGrouping(q);
        ResultList<?> res;
        try {
            res = (!detach && lrs) ? _fc.newResultList(rop) : new EagerResultList(rop);
            res.setUserObject(new Object[]{rop,ex});
            _resultLists.add(decorateResultList(res));
        } catch (OpenJPAException e) {
            if (e.getFailedObject() == null) {
                e.setFailedObject(getQueryString());
            }
            throw e;
        }
        return res;
    }

    /**
     * Optionally decorate the native result.
     */
    protected RemoveOnCloseResultList decorateResultList(ResultList<?> res) {
        return new RemoveOnCloseResultList(res);
    }

    /**
     * Return a result packer for this projection, or null.
     */
    private ResultPacker getResultPacker(StoreQuery q, StoreQuery.Executor ex) {
        if (_packer != null)
            return _packer;

        Class<?> resultClass = (_resultClass != null) ? _resultClass
            : ex.getResultClass(q);
        if (resultClass == null)
            return null;

        String[] aliases = ex.getProjectionAliases(q);
        ResultShape<?> shape = ex.getResultShape(q);
        if (shape != null) { // using JPA2.0 style result shape for packing
            if (aliases.length == 0) {
                _packer = new ResultShapePacker(new Class[]{_class}, new String[]{""}, resultClass, shape);
            } else {
                _packer = new ResultShapePacker(ex.getProjectionTypes(q), aliases, resultClass, shape);
            }
        } else {
            if (aliases.length == 0) {
                // result class but no result; means candidate is being set
                // into some result class
                _packer = new ResultPacker(_class, getAlias(), resultClass);
            } else if (resultClass != null) { // projection
                Class<?>[] types = ex.getProjectionTypes(q);
                _packer = new ResultPacker(types, aliases, resultClass);
            }
        }
        return _packer;
    }

    /**
     * Create an empty result for this query.
     */
    private Object emptyResult(StoreQuery q, StoreQuery.Executor ex) {
        if (_unique == Boolean.TRUE || (_unique == null
            && !ex.hasGrouping(q) && ex.isAggregate(q)))
            return null;
        return Collections.EMPTY_LIST;
    }

    /**
     * Extract an expected single result from the given provider. Used when
     * the result is an ungrouped aggregate or the unique flag is set to true.
     */
    private Object singleResult(ResultObjectProvider rop,
        StoreQuery.Range range)
        throws Exception {
        rop.open();
        try {
            // move to expected result
            boolean next = rop.next();

            // extract single result; throw an exception if multiple results
            // match and not constrainted by range, or if a unique query with
            // no results
            Object single = null;
            if (next) {
                single = rop.getResultObject();
                if (range.end != range.start + 1 && rop.next())
                    throw new NonUniqueResultException(_loc.get("not-unique",
                        _class, _query));
            } else if (_unique == Boolean.TRUE)
                throw new NoResultException(_loc.get("no-result",
                    _class, _query));

            // if unique set to false, use collection
            if (_unique == Boolean.FALSE) {
                if (!next)
                    return Collections.EMPTY_LIST;
                // Collections.singletonList is JDK 1.3, so...
                return Arrays.asList(new Object[]{ single });
            }

            // return single result
            return single;
        } finally {
            rop.close();
        }
    }

    /**
     * Calculates whether the access path of this query intersects with
     * any dirty objects in the transaction.
     */
    private boolean isAccessPathDirty() {
        return isAccessPathDirty(_broker, getAccessPathMetaDatas());
    }

    public static boolean isAccessPathDirty(Broker broker,
        ClassMetaData[] accessMetas) {
        Collection<Class<?>> persisted = broker.getPersistedTypes();
        Collection<Class<?>> updated = broker.getUpdatedTypes();
        Collection<Class<?>> deleted = broker.getDeletedTypes();
        if (persisted.isEmpty() && updated.isEmpty() && deleted.isEmpty())
            return false;

        // if no access metas, assume every dirty object affects path just
        // to be safe
        if (accessMetas.length == 0)
            return true;

        // compare dirty classes to the access path classes
        Class<?> accClass;
        for (ClassMetaData accessMeta : accessMetas) {
            if (accessMeta == null)
                continue;
            // shortcut if actual class is dirty
            accClass = accessMeta.getDescribedType();
            if (persisted.contains(accClass) || updated.contains(accClass)
                    || deleted.contains(accClass))
                return true;

            // check for dirty subclass
            for (Class<?> item : persisted)
                if (accClass.isAssignableFrom(item))
                    return true;
            for (Class<?> value : updated)
                if (accClass.isAssignableFrom(value))
                    return true;
            for (Class<?> aClass : deleted)
                if (accClass.isAssignableFrom(aClass))
                    return true;
        }

        // no intersection
        return false;
    }

    @Override
    public void closeAll() {
        closeResults(true);
    }

    @Override
    public void closeResources() {
        closeResults(false);
    }

    /**
     * Close open results.
     */
    private void closeResults(boolean force) {
        lock();
        try {
            assertOpen();

            RemoveOnCloseResultList res;
            for (RemoveOnCloseResultList resultList : _resultLists) {
                res = resultList;
                if (force || res.isProviderOpen())
                    res.close(false);
            }
            _resultLists.clear();
        } finally {
            unlock();
        }
    }

    @Override
    public String[] getDataStoreActions(Map params) {
        if (params == null)
            params = Collections.EMPTY_MAP;

        lock();
        try {
            assertNotSerialized();
            assertOpen();

            StoreQuery.Executor ex = compileForExecutor();
            assertParameters(_storeQuery, ex, params);
            Object[] arr = ex.toParameterArray(_storeQuery, params);
            StoreQuery.Range range = new StoreQuery.Range(_startIdx, _endIdx);
            if (!_rangeSet)
                ex.getRange(_storeQuery, arr, range);
            return ex.getDataStoreActions(_storeQuery, arr, range);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new UserException(e);
        } finally {
            unlock();
        }
    }

    @Override
    public boolean setQuery(Object query) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();

            if (query == null || query instanceof String) {
                invalidateCompilation();
                _query = (String) query;
                if (_query != null)
                    _query = _query.trim();
                return true;
            }
            if (!(query instanceof QueryImpl))
                return _storeQuery.setQuery(query);

            // copy all non-transient state from the given query
            invalidateCompilation();
            QueryImpl q = (QueryImpl) query;
            _class = q._class;
            _subclasses = q._subclasses;
            _query = q._query;
            _ignoreChanges = q._ignoreChanges;
            _unique = q._unique;
            _resultClass = q._resultClass;
            _params = q._params;
            _resultMappingScope = q._resultMappingScope;
            _resultMappingName = q._resultMappingName;
            _readOnly = q._readOnly;

            // don't share mutable objects
            _fc.copy(q._fc);
            if (q._filtListeners != null)
                _filtListeners = new HashMap<>(q._filtListeners);
            if (q._aggListeners != null)
                _aggListeners = new HashMap<>(q._aggListeners);
            return true;
        } finally {
            unlock();
        }
    }

    @Override
    public String getAlias() {
        lock();
        try {
            String alias = compileForExecutor().getAlias(_storeQuery);
            if (alias == null)
                alias = ClassUtil.getClassName(_class);
            return alias;
        } finally {
            unlock();
        }
    }

    @Override
    public String[] getProjectionAliases() {
        lock();
        try {
            return compileForExecutor().getProjectionAliases(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public Class<?>[] getProjectionTypes() {
        lock();
        try {
            return compileForExecutor().getProjectionTypes(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public int getOperation() {
        lock();
        try {
            return compileForExecutor().getOperation(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public boolean isAggregate() {
        lock();
        try {
            return compileForExecutor().isAggregate(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public boolean isDistinct() {
        lock();
        try {
            return compileForExecutor().isDistinct(_storeQuery);
        } finally {
            unlock();
        }
    }


    @Override
    public boolean hasGrouping() {
        lock();
        try {
            return compileForExecutor().hasGrouping(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public ClassMetaData[] getAccessPathMetaDatas() {
        lock();
        try {
            ClassMetaData[] metas = compileForExecutor().
                getAccessPathMetaDatas(_storeQuery);
            return (metas == null) ? StoreQuery.EMPTY_METAS : metas;
        } finally {
            unlock();
        }
    }

    @Override
    public OrderedMap<Object,Class<?>> getOrderedParameterTypes() {
        lock();
        try {
            return compileForExecutor().getOrderedParameterTypes(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public LinkedMap getParameterTypes() {
        lock();
        try {
            LinkedMap wrap = new LinkedMap();
            wrap.putAll(compileForExecutor().getOrderedParameterTypes(_storeQuery));
            return wrap;
        } finally {
            unlock();
        }
    }


    @Override
    public Map getUpdates() {
        lock();
        try {
            return compileForExecutor().getUpdates(_storeQuery);
        } finally {
            unlock();
        }
    }

    @Override
    public void lock() {
        if (_lock != null)
            _lock.lock();
    }

    @Override
    public void unlock() {
        if (_lock != null)
            _lock.unlock();
    }

    public synchronized void startLocking() {
    	if (_lock == null) {
    		_lock = new ReentrantLock();
    	}
    }

    public synchronized void stopLocking() {
    	if (_lock != null && !_broker.getMultithreaded())
    		_lock = null;
    }



    /////////
    // Utils
    /////////

    @Override
    public Class classForName(String name, String[] imports) {
        // full class name or primitive type?
        Class type = toClass(name);
        if (type != null)
            return type;

        // first check the aliases map in the MetaDataRepository
        ClassLoader loader = (_class == null) ? _loader : _class.getClassLoader();
        ClassMetaData meta = _broker.getConfiguration().
            getMetaDataRepositoryInstance().getMetaData(name, loader, false);
        if (meta != null)
            return meta.getDescribedType();

        // try the name in the package of the candidate class
        if (_class != null) {
            String fullName = _class.getName().substring
                (0, _class.getName().lastIndexOf('.') + 1) + name;
            type = toClass(fullName);
            if (type != null)
                return type;
        }

        // try java.lang
        type = toClass("java.lang." + name);
        if (type != null)
            return type;

        // try each import
        if (imports != null && imports.length > 0) {
            String dotName = "." + name;
            String importName;
            for (String anImport : imports) {
                importName = anImport;

                // full class name import
                if (importName.endsWith(dotName))
                    type = toClass(importName);
                    // wildcard; strip to package
                else if (importName.endsWith(".*")) {
                    importName = importName.substring
                            (0, importName.length() - 1);
                    type = toClass(importName + name);
                }
                if (type != null)
                    return type;
            }
        }
        return null;
    }

    /**
     * Return the {@link Class} for the given name, or null if name not valid.
     */
    private Class toClass(String name) {
        if (_loader == null)
            _loader = _broker.getConfiguration().getClassResolverInstance().
                getClassLoader(_class, _broker.getClassLoader());
        try {
            return ClassUtil.toClass(name, _loader);
        } catch (RuntimeException | NoClassDefFoundError re) {
        }
        return null;
    }

    @Override
    public void assertOpen() {
        if (_broker != null)
            _broker.assertOpen();
    }

    @Override
    public void assertNotReadOnly() {
        if (_readOnly)
            throw new InvalidStateException(_loc.get("read-only"));
    }

    @Override
    public void assertNotSerialized() {
        if (_broker == null)
            throw new InvalidStateException(_loc.get("serialized"));
    }

    /**
     * Check that a candidate class has been set for the query.
     */
    private void assertCandidateType() {
        if (_class == null && _storeQuery.requiresCandidateType())
            throw new InvalidStateException(_loc.get("no-class"));
    }

    /**
     * Check that we are in a state to be able to perform a bulk operation;
     * also flush the current modfications if any elements are currently dirty.
     */
    private void assertBulkModify(StoreQuery q, StoreQuery.Executor ex,
        Object[] params) {
        _broker.assertActiveTransaction();
        if (_startIdx != 0 || _endIdx != Long.MAX_VALUE)
            throw new UserException(_loc.get("no-modify-range"));
        if (_resultClass != null)
            throw new UserException(_loc.get("no-modify-resultclass"));
        StoreQuery.Range range = new StoreQuery.Range();
        ex.getRange(q, params, range);
        if (range.start != 0 || range.end != Long.MAX_VALUE)
            throw new UserException(_loc.get("no-modify-range"));
    }

    /**
     * Checks that the passed parameters match the declarations.
     */
    protected void assertParameters(StoreQuery q, StoreQuery.Executor ex,
        Object[] params) {
        if (!q.requiresParameterDeclarations() || !isParsedQuery())
            return;

        OrderedMap<Object,Class<?>> paramTypes = ex.getOrderedParameterTypes(q);
        int typeCount = paramTypes.size();
        if (typeCount > params.length)
            throw new UserException(_loc.get("unbound-params",
                paramTypes.keySet()));

        Iterator<Map.Entry<Object,Class<?>>> itr = paramTypes.entrySet().iterator();
        Map.Entry<Object,Class<?>> entry;
        for (int i = 0; itr.hasNext(); i++) {
            entry = itr.next();
            if (entry.getValue().isPrimitive() && params[i] == null)
                throw new UserException(_loc.get("null-primitive-param", entry.getKey()));
        }
    }

    protected void assertParameters(StoreQuery q, StoreQuery.Executor ex, Map params) {
        if (!q.requiresParameterDeclarations())
            return;

        OrderedMap<Object,Class<?>> paramTypes = ex.getOrderedParameterTypes(q);
        for (Object actual : params.keySet()) {
            if (!paramTypes.containsKey(actual))
            throw new UserException(_loc.get("unbound-params",
                actual, paramTypes.keySet()));
        }
        for (Object expected : paramTypes.keySet()) {
            if (!params.containsKey(expected))
            throw new UserException(_loc.get("unbound-params",
                expected, paramTypes.keySet()));
        }

        for (Entry<Object, Class<?>> entry : paramTypes.entrySet()) {
            if (entry.getValue().isPrimitive()
                && params.get(entry.getKey()) == null)
                throw new UserException(_loc.get("null-primitive-param", entry.getKey()));
        }
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(255);
        buf.append("Query: ").append(super.toString());
        buf.append("; candidate class: ").append(_class);
        buf.append("; query: ").append(_query);
        return buf.toString();
    }

    /**
     * Struct of compiled query properties.
     */
    protected static class Compilation
        implements Serializable {

        
        private static final long serialVersionUID = 1L;
        public StoreQuery.Executor memory = null;
        public StoreQuery.Executor datastore = null;
        public Object storeData = null;
    }

    /**
     * Struct to hold the unparsed properties associated with a query.
     */
    private static class CompilationKey
        implements Serializable {

        
        private static final long serialVersionUID = 1L;
        public Class queryType = null;
        public Class candidateType = null;
        public boolean subclasses = true;
        public String query = null;
        public String language = null;
        public Object storeKey = null;

        @Override
        public int hashCode() {
            int rs = 17;
            rs = 37 * rs + ((queryType == null) ? 0 : queryType.hashCode());
            rs = 37 * rs + ((query == null) ? 0 : query.hashCode());
            rs = 37 * rs + ((language == null) ? 0 : language.hashCode());
            rs = 37 * rs + ((storeKey == null) ? 0 : storeKey.hashCode());
            if (subclasses)
              rs++;
            return rs;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this)
                return true;
            if (other == null || other.getClass() != getClass())
                return false;

            CompilationKey key = (CompilationKey) other;
            if (key.queryType != queryType
                || !Objects.equals(key.query, query)
                || !Objects.equals(key.language, language))
                return false;
            if (key.subclasses != subclasses)
                return false;
            if (!Objects.equals(key.storeKey, storeKey))
                return false;

            // allow either candidate type to be null because it might be
            // encoded in the query string, but if both are set then they
            // must be equal
            return key.candidateType == null || candidateType == null
                || key.candidateType == candidateType;
        }
    }

    /**
     * A merged executor executes multiple Queries and returns
     * a merged result list with the appropriate ordering (if more than
     * one query needs to be executed). This executor has the following
     * limitations:
     * <ul>
     * <li>It cannot combine aggregates.
     * <li>It cannot collate the result lists if ordering is specified and
     * a result string is given, but does not include the ordering
     * criteria.</li>
     * <li>It cannot filter duplicate results from different result lists if
     * the result is marked distinct. This would require tracking all
     * previous results, which would interfere with large result set
     * handling.</li>
     * </ul>
     *
     * @author Marc Prud'hommeaux
         */
    private static class MergedExecutor
        implements StoreQuery.Executor {

        private final StoreQuery.Executor[] _executors;

        public MergedExecutor(StoreQuery.Executor[] executors) {
            _executors = executors;
        }

        @Override
        public QueryExpressions[] getQueryExpressions() {
            return _executors[0].getQueryExpressions();
        }

        @Override
        public ResultObjectProvider executeQuery(StoreQuery q,
            Object[] params, StoreQuery.Range range) {
            if (_executors.length == 1)
                return _executors[0].executeQuery(q, params, range);

            // use lrs settings if we couldn't take advantage of the start index
            // so that hopefully the skip to the start will be efficient
            StoreQuery.Range ropRange = new StoreQuery.Range(0, range.end);
            ropRange.lrs = range.lrs || (range.start > 0 && q.getContext().
                getFetchConfiguration().getFetchBatchSize() >= 0);

            // execute the query; we cannot use the lower bound of the result
            // range, but we can take advantage of the upper bound
            ResultObjectProvider[] rops =
                new ResultObjectProvider[_executors.length];
            for (int i = 0; i < _executors.length; i++)
                rops[i] = _executors[i].executeQuery(q, params, ropRange);

            boolean[] asc = _executors[0].getAscending(q);
            ResultObjectProvider rop;
            if (asc.length == 0)
                rop = new MergedResultObjectProvider(rops);
            else
                rop = new OrderingMergedResultObjectProvider(rops, asc,
                    _executors, q, params);

            // if there is a lower bound, wrap in range rop
            if (range.start != 0)
                rop = new RangeResultObjectProvider(rop, range.start,
                    range.end);
            return rop;
        }

        @Override
        public Number executeDelete(StoreQuery q, Object[] params) {
            long num = 0;
            for (StoreQuery.Executor executor : _executors) {
                num += executor.executeDelete(q, params).longValue();
            }
            return num;
        }

        @Override
        public Number executeUpdate(StoreQuery q, Object[] params) {
            long num = 0;
            for (StoreQuery.Executor executor : _executors) {
                num += executor.executeUpdate(q, params).longValue();
            }
            return num;
        }

        @Override
        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            StoreQuery.Range range) {
            if (_executors.length == 1)
                return _executors[0].getDataStoreActions(q, params, range);

            List results = new ArrayList(_executors.length);
            StoreQuery.Range ropRange = new StoreQuery.Range(0L, range.end);
            String[] actions;
            for (StoreQuery.Executor executor : _executors) {
                actions = executor.getDataStoreActions(q, params, ropRange);
                if (actions != null && actions.length > 0)
                    results.addAll(Arrays.asList(actions));
            }
            return (String[]) results.toArray(new String[results.size()]);
        }

        @Override
        public void validate(StoreQuery q) {
            _executors[0].validate(q);
        }

        @Override
        public void getRange(StoreQuery q, Object[] params,
            StoreQuery.Range range) {
            _executors[0].getRange(q, params, range);
        }

        @Override
        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int idx) {
            // unfortunately, at this point (must be a merged rop containing
            // other merged rops) we have no idea which executor to extract
            // the value from
            return _executors[0].getOrderingValue(q, params, resultObject, idx);
        }

        @Override
        public boolean[] getAscending(StoreQuery q) {
            return _executors[0].getAscending(q);
        }

        @Override
        public String getAlias(StoreQuery q) {
            return _executors[0].getAlias(q);
        }

        @Override
        public String[] getProjectionAliases(StoreQuery q) {
            return _executors[0].getProjectionAliases(q);
        }

        @Override
        public Class getResultClass(StoreQuery q) {
            return _executors[0].getResultClass(q);
        }

        @Override
        public ResultShape<?> getResultShape(StoreQuery q) {
            return _executors[0].getResultShape(q);
        }

        @Override
        public Class[] getProjectionTypes(StoreQuery q) {
            return _executors[0].getProjectionTypes(q);
        }

        @Override
        public boolean isPacking(StoreQuery q) {
            return _executors[0].isPacking(q);
        }

        @Override
        public ClassMetaData[] getAccessPathMetaDatas(StoreQuery q) {
            if (_executors.length == 1)
                return _executors[0].getAccessPathMetaDatas(q);

            // create set of base class metadatas in access path
            List metas = null;
            for (StoreQuery.Executor executor : _executors)
                metas = Filters.addAccessPathMetaDatas(metas, executor.
                        getAccessPathMetaDatas(q));
            if (metas == null)
                return StoreQuery.EMPTY_METAS;
            return (ClassMetaData[]) metas.toArray
                (new ClassMetaData[metas.size()]);
        }

        @Override
        public boolean isAggregate(StoreQuery q) {
            if (!_executors[0].isAggregate(q))
                return false;

            // we can't merge aggregates
            throw new UnsupportedException(_loc.get("merged-aggregate",
                q.getContext().getCandidateType(),
                q.getContext().getQueryString()));
        }

        @Override
        public boolean isDistinct(StoreQuery q) {
            return _executors[0].isDistinct(q);
        }

        @Override
        public int getOperation(StoreQuery q) {
            return _executors[0].getOperation(q);
        }

        @Override
        public boolean hasGrouping(StoreQuery q) {
            return _executors[0].hasGrouping(q);
        }

        @Override
        public OrderedMap<Object,Class<?>> getOrderedParameterTypes(StoreQuery q) {
            return _executors[0].getOrderedParameterTypes(q);
        }

        @Override
        public LinkedMap getParameterTypes(StoreQuery q) {
            return _executors[0].getParameterTypes(q);
        }

        @Override
        public Object[] toParameterArray(StoreQuery q, Map userParams) {
            return _executors[0].toParameterArray(q, userParams);
        }


        @Override
        public Map getUpdates(StoreQuery q) {
            return _executors[0].getUpdates(q);
        }
    }

    /**
     * Result object provider that packs results before returning them.
     */
    public static class PackingResultObjectProvider
        implements ResultObjectProvider {

        private final ResultObjectProvider _delegate;
        private final ResultPacker _packer;
        private final int _len;

        public PackingResultObjectProvider(ResultObjectProvider delegate,
            ResultPacker packer, int resultLength) {
            _delegate = delegate;
            _packer = packer;
            _len = resultLength;
        }

        @Override
        public boolean supportsRandomAccess() {
            return _delegate.supportsRandomAccess();
        }

        @Override
        public void open()
            throws Exception {
            _delegate.open();
        }

        @Override
        public Object getResultObject()
            throws Exception {
            Object ob = _delegate.getResultObject();
            if (_packer == null && _len == 1)
                return ((Object[]) ob)[0];
            if (_packer == null)
                return ob;
            if (_len == 0)
                return _packer.pack(ob);
            return _packer.pack((Object[]) ob);
        }

        @Override
        public boolean next()
            throws Exception {
            return _delegate.next();
        }

        @Override
        public boolean absolute(int pos)
            throws Exception {
            return _delegate.absolute(pos);
        }

        @Override
        public int size()
            throws Exception {
            return _delegate.size();
        }

        @Override
        public void reset()
            throws Exception {
            _delegate.reset();
        }

        @Override
        public void close()
            throws Exception {
            _delegate.close();
        }

        @Override
        public void handleCheckedException(Exception e) {
            _delegate.handleCheckedException(e);
        }

        public ResultObjectProvider getDelegate() {
            return _delegate;
        }
    }

    /**
     * Result list that removes itself from the query's open result list
     * when it is closed. Public for testing.
     */
    public class RemoveOnCloseResultList
        implements ResultList {

        
        private static final long serialVersionUID = 1L;
        private final ResultList _res;

        public RemoveOnCloseResultList(ResultList res) {
            _res = res;
        }

        public ResultList getDelegate() {
            return _res;
        }

        @Override
        public boolean isProviderOpen() {
            return _res.isProviderOpen();
        }

        @Override
        public Object getUserObject() {
            return _res.getUserObject();
        }

        @Override
        public void setUserObject(Object opaque) {
            _res.setUserObject(opaque);
        }

        @Override
        public boolean isClosed() {
            return _res.isClosed();
        }

        @Override
        public void close() {
            close(true);
        }

        public void close(boolean remove) {
            if (isClosed())
                return;

            _res.close();
            if (!remove)
                return;

            lock();
            try {
                // don't use standard _resultLists.remove method b/c relies on
                // collection equality, which relies on element equality, which
                // means we end up traversing entire result lists!
                for (Iterator itr = _resultLists.iterator(); itr.hasNext();) {
                    if (itr.next() == this) {
                        itr.remove();
                        break;
                    }
                }
            } finally {
                unlock();
            }
        }

        @Override
        public int size() {
            return _res.size();
        }

        @Override
        public boolean isEmpty() {
            return _res.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return _res.contains(o);
        }

        @Override
        public Iterator iterator() {
            return _res.iterator();
        }

        @Override
        public Object[] toArray() {
            return _res.toArray();
        }

        @Override
        public Object[] toArray(Object[] a) {
            return _res.toArray(a);
        }

        @Override
        public boolean add(Object o) {
            return _res.add(o);
        }

        @Override
        public boolean remove(Object o) {
            return _res.remove(o);
        }

        @Override
        public boolean containsAll(Collection c) {
            return _res.containsAll(c);
        }

        @Override
        public boolean addAll(Collection c) {
            return _res.addAll(c);
        }

        @Override
        public boolean addAll(int idx, Collection c) {
            return _res.addAll(idx, c);
        }

        @Override
        public boolean removeAll(Collection c) {
            return _res.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection c) {
            return _res.retainAll(c);
        }

        @Override
        public void clear() {
            _res.clear();
        }

        @Override
        public Object get(int idx) {
            return _res.get(idx);
        }

        @Override
        public Object set(int idx, Object o) {
            return _res.set(idx, o);
        }

        @Override
        public void add(int idx, Object o) {
            _res.add(idx, o);
        }

        @Override
        public Object remove(int idx) {
            return _res.remove(idx);
        }

        @Override
        public int indexOf(Object o) {
            return _res.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return _res.lastIndexOf(o);
        }

        @Override
        public ListIterator listIterator() {
            return _res.listIterator();
        }

        @Override
        public ListIterator listIterator(int idx) {
            return _res.listIterator(idx);
        }

        @Override
        public List subList(int start, int end) {
            return _res.subList(start, end);
        }

        @Override
        public boolean equals(Object o) {
            return _res.equals(o);
        }

        @Override
        public int hashCode() {
            return _res.hashCode();
        }

        @Override
        public String toString ()
		{
			return _res.toString ();
		}

		public Object writeReplace ()
		{
			return _res;
		}
	}
}

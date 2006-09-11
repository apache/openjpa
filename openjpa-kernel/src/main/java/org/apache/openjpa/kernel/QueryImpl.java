/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.Constant;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.rop.EagerResultList;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.lib.util.concurrent.ReentrantLock;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;
import serp.util.Numbers;
import serp.util.Strings;

/**
 * Implementation of the {@link Query} interface.
 *
 * @author Abe White
 * @nojavadoc
 */
public class QueryImpl
    implements Query {

    private static Localizer _loc = Localizer.forPackage(QueryImpl.class);

    private final String _language;
    private final StoreQuery _storeQuery;
    private transient final BrokerImpl _broker;
    private transient final Log _log;
    private transient ClassLoader _loader = null;

    // query has its own internal lock
    private final ReentrantLock _lock = new ReentrantLock();

    // unparsed state
    private Class _class = null;
    private boolean _subclasses = true;
    private boolean _readOnly = false;
    private String _query = null;
    private String _params = null;

    // parsed state
    private transient Compilation _compiled = null;
    private transient boolean _compiling = false;
    private transient ResultPacker _packer = null;

    // candidates
    private transient Collection _collection = null;
    private transient Extent _extent = null;

    // listeners
    private Map _filtListeners = null;
    private Map _aggListeners = null;

    // configuration for loading objects
    private FetchConfiguration _fc = null;
    private boolean _ignoreChanges = false;
    private Class _resultMappingScope = null;
    private String _resultMappingName = null;

    // these fields should only be used directly after we have a compilation,
    // because their values may be encoded in the query string
    private Boolean _unique = null;
    private Class _resultClass = null;
    private transient long _startIdx = 0;
    private transient long _endIdx = Long.MAX_VALUE;
    private transient boolean _rangeSet = false;

    // remember the list of all the results we have returned so we
    // can free their resources when close or closeAll is called
    private transient final Collection _resultLists = new ReferenceHashSet
        (ReferenceHashSet.WEAK);

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
    }

    /**
     * Internal store query.
     */
    public StoreQuery getStoreQuery() {
        return _storeQuery;
    }

    public Broker getBroker() {
        return _broker;
    }

    public Query getQuery() {
        return this;
    }

    public StoreContext getStoreContext() {
        return _broker;
    }

    public String getLanguage() {
        return _language;
    }

    public FetchConfiguration getFetchConfiguration() {
        return _fc;
    }

    public String getQueryString() {
        return _query;
    }

    public boolean getIgnoreChanges() {
        assertOpen();
        return _ignoreChanges;
    }

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

    public boolean isReadOnly() {
        assertOpen();
        return _readOnly;
    }

    public void setReadOnly(boolean flag) {
        lock();
        try {
            assertOpen();
            _readOnly = flag;
        } finally {
            unlock();
        }
    }

    public void addFilterListener(FilterListener listener) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (_filtListeners == null)
                _filtListeners = new HashMap(5);
            _filtListeners.put(listener.getTag(), listener);
        } finally {
            unlock();
        }
    }

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

    public Collection getFilterListeners() {
        return (_filtListeners == null) ? Collections.EMPTY_LIST
            : _filtListeners.values();
    }

    public FilterListener getFilterListener(String tag) {
        // first check listeners for this query
        if (_filtListeners != null) {
            FilterListener listen = (FilterListener) _filtListeners.get(tag);
            if (listen != null)
                return listen;
        }

        // check user-defined listeners from configuration
        FilterListener[] confListeners = _broker.getConfiguration().
            getFilterListenerInstances();
        for (int i = 0; i < confListeners.length; i++)
            if (confListeners[i].getTag().equals(tag))
                return confListeners[i];

        // check store listeners
        return _storeQuery.getFilterListener(tag);
    }

    public void addAggregateListener(AggregateListener listener) {
        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (_aggListeners == null)
                _aggListeners = new HashMap(5);
            _aggListeners.put(listener.getTag(), listener);
        } finally {
            unlock();
        }
    }

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

    public Collection getAggregateListeners() {
        return (_aggListeners == null) ? Collections.EMPTY_LIST
            : _aggListeners.values();
    }

    public AggregateListener getAggregateListener(String tag) {
        // first check listeners for this query
        if (_aggListeners != null) {
            AggregateListener listen = (AggregateListener) _aggListeners.
                get(tag);
            if (listen != null)
                return listen;
        }

        // check user-defined listeners from configuration
        AggregateListener[] confListeners = _broker.getConfiguration().
            getAggregateListenerInstances();
        for (int i = 0; i < confListeners.length; i++)
            if (confListeners[i].getTag().equals(tag))
                return confListeners[i];

        // check store listeners
        return _storeQuery.getAggregateListener(tag);
    }

    public Extent getCandidateExtent() {
        // if just the class is set, fetch the corresponding extent; if the
        // extent is already set but its ignore cache setting is wrong,
        // get a new extent with the correct setting (don't modify orig extent
        // in case the user has a reference to it and might use it)
        lock();
        try {
            Class cls = getCandidateType();
            if (_extent == null && _collection == null && _broker != null
                && cls != null) {
                _extent = _broker.newExtent(cls, _subclasses);
                _extent.setIgnoreChanges(_ignoreChanges);
            } else if (_extent != null
                && _extent.getIgnoreChanges() != _ignoreChanges && cls != null)
            {
                _extent = _broker.newExtent(cls, _extent.hasSubclasses());
                _extent.setIgnoreChanges(_ignoreChanges);
            }
            return _extent;
        } finally {
            unlock();
        }
    }

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

    public Collection getCandidateCollection() {
        assertOpen();
        return _collection;
    }

    public void setCandidateCollection(Collection candidateCollection) {
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

    public boolean hasSubclasses() {
        return _subclasses;
    }

    public String getResultMappingName() {
        assertOpen();
        return _resultMappingName;
    }

    public Class getResultMappingScope() {
        assertOpen();
        return _resultMappingScope;
    }

    public void setResultMapping(Class scope, String name) {
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

    public boolean isUnique() {
        lock();
        try {
            assertOpen();
            if (_unique != null)
                return _unique.booleanValue();
            if (_query == null || _compiling || _broker == null)
                return false;

            // check again after compilation; maybe encoded in string
            if (_compiled == null) {
                compileForCompilation();
                if (_unique != null)
                    return _unique.booleanValue();
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

    public long getStartRange() {
        assertOpen();
        return _startIdx;
    }

    public long getEndRange() {
        assertOpen();
        return _endIdx;
    }

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

    public String getParameterDeclaration() {
        lock();
        try {
            assertOpen();
            if (_params != null || _compiled != null || _compiling
                || _broker == null)
                return _params;

            compileForCompilation();
            return _params;
        } finally {
            unlock();
        }
    }

    public void declareParameters(String params) {
        if (!_storeQuery.supportsParameterDeclarations())
            throw new UnsupportedException(_loc.get("query-nosupport",
                _language));

        lock();
        try {
            assertOpen();
            assertNotReadOnly();
            if (params != null)
                params = params.trim();
            if (params != null && params.length() == 0)
                params = null;
            _params = params;
            invalidateCompilation();
        } finally {
            unlock();
        }
    }

    public void compile() {
        lock();
        try {
            assertOpen();
            StoreQuery.Executor ex = compileForExecutor();
            getResultPacker(ex);
            ex.validate(_storeQuery);
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
            _compiled = newCompilation();
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
     * Create and initialize a query compilation based on current data.
     */
    protected Compilation newCompilation() {
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

    public Object execute() {
        return execute((Object[]) null);
    }

    public Object execute(Object[] params) {
        return execute(OP_SELECT, params);
    }

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

                assertParameters(ex, params);
                if (_log.isTraceEnabled())
                    logExecution(operation, ex.getParameterTypes(_storeQuery),
                        params);

                if (operation == OP_SELECT)
                    return execute(ex, params);
                if (operation == OP_DELETE)
                    return delete(ex, params);
                if (operation == OP_UPDATE)
                    return update(ex, params);
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

                Object[] arr = (params.isEmpty()) ? StoreQuery.EMPTY_OBJECTS :
                    toParameterArray(ex.getParameterTypes(_storeQuery), params);
                assertParameters(ex, arr);
                if (_log.isTraceEnabled())
                    logExecution(operation, params);

                if (operation == OP_SELECT)
                    return execute(ex, arr);
                if (operation == OP_DELETE)
                    return delete(ex, arr);
                if (operation == OP_UPDATE)
                    return update(ex, arr);
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

    public long deleteAll() {
        return deleteAll((Object[]) null);
    }

    public long deleteAll(Object[] params) {
        return ((Number) execute(OP_DELETE, params)).longValue();
    }

    public long deleteAll(Map params) {
        return ((Number) execute(OP_DELETE, params)).longValue();
    }

    public long updateAll() {
        return updateAll((Object[]) null);
    }

    public long updateAll(Object[] params) {
        return ((Number) execute(OP_UPDATE, params)).longValue();
    }

    public long updateAll(Map params) {
        return ((Number) execute(OP_UPDATE, params)).longValue();
    }

    private Object[] toParameterArray(LinkedMap paramTypes, Map params) {
        if (params == null || params.isEmpty())
            return StoreQuery.EMPTY_OBJECTS;

        Object[] arr = new Object[params.size()];
        Map.Entry entry;
        Object key;
        int idx;
        int base = -1;
        for (Iterator itr = params.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            key = entry.getKey();
            idx = (paramTypes == null) ? -1 : paramTypes.indexOf(key);

            // allow positional parameters and natural order parameters
            if (idx != -1)
                arr[idx] = entry.getValue();
            else if (key instanceof Number) {
                if (base == -1)
                    base = positionalParameterBase(params.keySet());
                arr[((Number) key).intValue() - base] = entry.getValue();
            } else
                throw new UserException(_loc.get("bad-param-name", key));
        }
        return arr;
    }

    /**
     * Return the base (generally 0 or 1) to use for positional parameters.
     */
    private static int positionalParameterBase(Collection params) {
        int low = Integer.MAX_VALUE;
        Object obj;
        int val;
        for (Iterator itr = params.iterator(); itr.hasNext();) {
            obj = itr.next();
            if (!(obj instanceof Number))
                return 0; // use 0 base when params are mixed types

            val = ((Number) obj).intValue();
            if (val == 0)
                return val;
            if (val < low)
                low = val;
        }
        return low;
    }

    /**
     * Return whether we should execute this query in memory.
     */
    private boolean isInMemory(int operation) {
        // if no candidates, create candidate extent
        if (_collection == null && _extent == null && _class != null) {
            _extent = _broker.newExtent(_class, _subclasses);
            _extent.setIgnoreChanges(_ignoreChanges);
        }

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
     * {@link #execute(StoreQuery.Executor,Map)} after validation and locking.
     */
    private Object execute(StoreQuery.Executor ex, Object[] params)
        throws Exception {
        // if this is an impossible result range, return null / empty list
        StoreQuery.Range range = new StoreQuery.Range(_startIdx, _endIdx);
        if (!_rangeSet)
            ex.getRange(_storeQuery, params, range);
        if (range.start >= range.end)
            return emptyResult(ex);

        // execute; if we have a result class or we have only one result
        // and so need to remove it from its array, wrap in a packing rop
        range.lrs = isLRS(range.start, range.end);
        ResultObjectProvider rop = ex.executeQuery(_storeQuery, params, range);
        try {
            return toResult(ex, rop, range);
        } catch (Exception e) {
            if (rop != null)
                try { rop.close(); } catch (Exception e2) {}
            throw e;
        }
    }

    /**
     * Delete the query using the given executor, and parameter
     * values. All other execute methods delegate to this one or to
     * {@link #delete(StoreQuery.Executor,Map)} after validation and locking.
     * The return value will be a Number indicating the number of
     * instances deleted.
     */
    private Number delete(StoreQuery.Executor ex, Object[] params)
        throws Exception {
        assertBulkModify(ex, params);
        return ex.executeDelete(_storeQuery, params);
    }

    public Number deleteInMemory(StoreQuery.Executor executor,
        Object[] params) {
        try {
            Object o = execute(executor, params);
            if (!(o instanceof Collection))
                o = Collections.singleton(o);

            int size = 0;
            for (Iterator i = ((Collection) o).iterator(); i.hasNext(); size++)
                _broker.delete(i.next(), null);
            return Numbers.valueOf(size);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new UserException(e);
        }
    }

    /**
     * Update the query using the given compilation, executor, and parameter
     * values. All other execute methods delegate to this one or to
     * {@link #update(StoreQuery.Executor,Map)} after validation and locking.
     * The return value will be a Number indicating the number of
     * instances updated.
     */
    private Number update(StoreQuery.Executor ex, Object[] params)
        throws Exception {
        assertBulkModify(ex, params);
        return ex.executeUpdate(_storeQuery, params);
    }

    public Number updateInMemory(StoreQuery.Executor executor,
        Object[] params) {
        try {
            Object o = execute(executor, params);
            if (!(o instanceof Collection))
                o = Collections.singleton(o);

            int size = 0;
            for (Iterator i = ((Collection) o).iterator(); i.hasNext(); size++)
                updateInMemory(i.next(), params);
            return Numbers.valueOf(size);
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
    private void updateInMemory(Object ob, Object[] params) {
        for (Iterator it = getUpdates().entrySet().iterator();
            it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            FieldMetaData fmd = (FieldMetaData) e.getKey();
            if (!(e.getValue() instanceof Constant))
                throw new UserException(_loc.get("only-update-constants"));
            Constant value = (Constant) e.getValue();
            Object val = value.getValue(params);

            OpenJPAStateManager sm = _broker.getStateManager(ob);
            int i = fmd.getIndex();
            PersistenceCapable into = (PersistenceCapable) ob;

            // set the actual field in the instance
            int set = OpenJPAStateManager.SET_USER;
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.BOOLEAN:
                    sm.settingBooleanField(into, i, sm.fetchBooleanField(i),
                        val == null ? false : ((Boolean) val).booleanValue(),
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
    private void logExecution(int op, LinkedMap types, Object[] params) {
        Map pmap = Collections.EMPTY_MAP;
        if (params.length > 0) {
            pmap = new HashMap((int) (params.length * 1.33 + 1));
            if (types != null && types.size() == params.length) {
                int i = 0;
                for (Iterator itr = types.keySet().iterator(); itr.hasNext();)
                    pmap.put(itr.next(), params[i++]);
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
    private void logExecution(int op, Map params) {
        String s = _query;
        if (s == null || s.length() == 0)
            s = toString();

        String msg = "executing-query";
        if (!params.isEmpty())
            msg += "-with-params";

        _log.trace(_loc.get(msg, s, params));
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
    protected Object toResult(StoreQuery.Executor ex, ResultObjectProvider rop,
        StoreQuery.Range range)
        throws Exception {
        // pack projections if necessary
        String[] aliases = ex.getProjectionAliases(_storeQuery);
        if (!ex.isPacking(_storeQuery)) {
            ResultPacker packer = getResultPacker(ex);
            if (packer != null || aliases.length == 1)
                rop = new PackingResultObjectProvider(rop, packer,
                    aliases.length);
        }

        // if single result, extract it
        if (_unique == Boolean.TRUE || (aliases.length > 0
            && !ex.hasGrouping(_storeQuery) && ex.isAggregate(_storeQuery)))
            return singleResult(rop, range);

        // now that we've executed the query, we can call isAggregate and
        // hasGrouping efficiently
        boolean detach = (_broker.getAutoDetach() &
            AutoDetach.DETACH_NONTXREAD) > 0 && !_broker.isActive();
        boolean lrs = range.lrs && !ex.isAggregate(_storeQuery)
            && !ex.hasGrouping(_storeQuery);
        ResultList res = (!detach && lrs) ? _fc.newResultList(rop)
            : new EagerResultList(rop);

        _resultLists.add(decorateResultList(res));
        return res;
    }

    /**
     * Optionally decorate the native result.
     */
    protected ResultList decorateResultList(ResultList res) {
        return new RemoveOnCloseResultList(res);
    }

    /**
     * Return a result packer for this projection, or null.
     */
    private ResultPacker getResultPacker(StoreQuery.Executor ex) {
        if (_packer != null)
            return _packer;

        Class resultClass = (_resultClass != null) ? _resultClass
            : ex.getResultClass(_storeQuery);
        if (resultClass == null)
            return null;

        String[] aliases = ex.getProjectionAliases(_storeQuery);
        if (aliases.length == 0) {
            // result class but no result; means candidate is being set
            // into some result class
            _packer = new ResultPacker(_class, getAlias(), resultClass);
        } else if (resultClass != null) {
            // projection
            Class[] types = ex.getProjectionTypes(_storeQuery);
            _packer = new ResultPacker(types, aliases, resultClass);
        }
        return _packer;
    }

    /**
     * Create an empty result for this query.
     */
    private Object emptyResult(StoreQuery.Executor ex) {
        if (_unique == Boolean.TRUE || (_unique == null
            && !ex.hasGrouping(_storeQuery) && ex.isAggregate(_storeQuery)))
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
            // match and not constrainted by range, as per spec
            Object single = null;
            if (next) {
                single = rop.getResultObject();
                if (range.end != range.start + 1 && rop.next())
                    throw new InvalidStateException(_loc.get("not-unique",
                        _class, _query));
            }

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
        Collection persisted = broker.getPersistedTypes();
        Collection updated = broker.getUpdatedTypes();
        Collection deleted = broker.getDeletedTypes();
        if (persisted.isEmpty() && updated.isEmpty() && deleted.isEmpty())
            return false;

        // if no access metas, assume every dirty object affects path just
        // to be safe
        if (accessMetas.length == 0)
            return true;

        // compare dirty classes to the access path classes
        Class accClass;
        for (int i = 0; i < accessMetas.length; i++) {
            // shortcut if actual class is dirty
            accClass = accessMetas[i].getDescribedType();
            if (persisted.contains(accClass) || updated.contains(accClass)
                || deleted.contains(accClass))
                return true;

            // check for dirty subclass
            for (Iterator dirty = persisted.iterator(); dirty.hasNext();)
                if (accClass.isAssignableFrom((Class) dirty.next()))
                    return true;
            for (Iterator dirty = updated.iterator(); dirty.hasNext();)
                if (accClass.isAssignableFrom((Class) dirty.next()))
                    return true;
            for (Iterator dirty = deleted.iterator(); dirty.hasNext();)
                if (accClass.isAssignableFrom((Class) dirty.next()))
                    return true;
        }

        // no intersection
        return false;
    }

    public void closeAll() {
        closeResults(true);
    }

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
            for (Iterator itr = _resultLists.iterator(); itr.hasNext();) {
                res = (RemoveOnCloseResultList) itr.next();
                if (force || res.isProviderOpen())
                    res.close(false);
            }
            _resultLists.clear();
        } finally {
            unlock();
        }
    }

    public String[] getDataStoreActions(Map params) {
        if (params == null)
            params = Collections.EMPTY_MAP;

        lock();
        try {
            assertNotSerialized();
            assertOpen();

            StoreQuery.Executor ex = compileForExecutor();
            Object[] arr = toParameterArray(ex.getParameterTypes(_storeQuery),
                params);
            assertParameters(ex, arr);
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
                _filtListeners = new HashMap(q._filtListeners);
            if (q._aggListeners != null)
                _aggListeners = new HashMap(q._aggListeners);
            return true;
        } finally {
            unlock();
        }
    }

    public String getAlias() {
        lock();
        try {
            String alias = compileForExecutor().getAlias(_storeQuery);
            if (alias == null)
                alias = Strings.getClassName(_class);
            return alias;
        } finally {
            unlock();
        }
    }

    public String[] getProjectionAliases() {
        lock();
        try {
            return compileForExecutor().getProjectionAliases(_storeQuery);
        } finally {
            unlock();
        }
    }

    public Class[] getProjectionTypes() {
        lock();
        try {
            return compileForExecutor().getProjectionTypes(_storeQuery);
        } finally {
            unlock();
        }
    }

    public int getOperation() {
        lock();
        try {
            return compileForExecutor().getOperation(_storeQuery);
        } finally {
            unlock();
        }
    }

    public boolean isAggregate() {
        lock();
        try {
            return compileForExecutor().isAggregate(_storeQuery);
        } finally {
            unlock();
        }
    }

    public boolean hasGrouping() {
        lock();
        try {
            return compileForExecutor().hasGrouping(_storeQuery);
        } finally {
            unlock();
        }
    }

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

    public LinkedMap getParameterTypes() {
        lock();
        try {
            return compileForExecutor().getParameterTypes(_storeQuery);
        } finally {
            unlock();
        }
    }

    public Map getUpdates() {
        lock();
        try {
            return compileForExecutor().getUpdates(_storeQuery);
        } finally {
            unlock();
        }
    }

    public void lock() {
        if (_broker == null || _broker.getMultithreaded())
            _lock.lock();
    }

    public void unlock() {
        if (_lock.isLocked())
            _lock.unlock();
    }

    public Object getCompilation() {
        lock();
        try {
            return compileForCompilation().storeData;
        } finally {
            unlock();
        }
    }

    /////////
    // Utils
    /////////

    public Class classForName(String name, String[] imports) {
        // full class name or primitive type?
        Class type = toClass(name);
        if (type != null)
            return type;

        // first check the aliases map in the MetaDataRepository
        ClassLoader loader = (_class == null) ? _loader
            : _class.getClassLoader();
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
            for (int i = 0; i < imports.length; i++) {
                importName = imports[i];

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
            return Strings.toClass(name, _loader);
        } catch (RuntimeException re) {
        } catch (NoClassDefFoundError ncdfe) {
        }
        return null;
    }

    public void assertOpen() {
        if (_broker != null)
            _broker.assertOpen();
    }

    public void assertNotReadOnly() {
        if (_readOnly)
            throw new InvalidStateException(_loc.get("read-only"));
    }

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
    private void assertBulkModify(StoreQuery.Executor ex, Object[] params) {
        _broker.assertActiveTransaction();
        if (_startIdx != 0 || _endIdx != Long.MAX_VALUE)
            throw new UserException(_loc.get("no-modify-range"));
        if (_resultClass != null)
            throw new UserException(_loc.get("no-modify-resultclass"));
        StoreQuery.Range range = new StoreQuery.Range();
        ex.getRange(_storeQuery, params, range);
        if (range.start != 0 || range.end != Long.MAX_VALUE)
            throw new UserException(_loc.get("no-modify-range"));
    }

    /**
     * Checks that the passed parameters match the declarations.
     */
    protected void assertParameters(StoreQuery.Executor ex, Object[] params) {
        if (!_storeQuery.requiresParameterDeclarations())
            return;

        LinkedMap paramTypes = ex.getParameterTypes(_storeQuery);
        int typeCount = paramTypes.size();
        if (typeCount > params.length)
            throw new UserException(_loc.get("unbound-params",
                paramTypes.keySet()));
        if (typeCount < params.length)
            throw new UserException(_loc.get("extra-params", new Object[]
                { String.valueOf(typeCount), String.valueOf(params.length) }));

        Iterator itr = paramTypes.entrySet().iterator();
        Map.Entry entry;
        for (int i = 0; itr.hasNext(); i++) {
            entry = (Map.Entry) itr.next();
            if (((Class) entry.getValue()).isPrimitive() && params[i] == null)
                throw new UserException(_loc.get("null-primitive-param",
                    entry.getKey()));
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(64);
        buf.append("Query: ").append(super.toString());
        buf.append("; candidate class: ").append(_class);
        buf.append("; query: ").append(_query);
        return buf.toString();
    }

    /**
     * Struct of compiled query properties.
     */
    protected static class Compilation {

        public StoreQuery.Executor memory = null;
        public StoreQuery.Executor datastore = null;
        public Object storeData = null;
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
     * @nojavadoc
     */
    private static class MergedExecutor
        implements StoreQuery.Executor {

        private final StoreQuery.Executor[] _executors;

        public MergedExecutor(StoreQuery.Executor[] executors) {
            _executors = executors;
        }

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

        public Number executeDelete(StoreQuery q, Object[] params) {
            long num = 0;
            for (int i = 0; i < _executors.length; i++)
                num += _executors[i].executeDelete(q, params).longValue();
            return Numbers.valueOf(num);
        }

        public Number executeUpdate(StoreQuery q, Object[] params) {
            long num = 0;
            for (int i = 0; i < _executors.length; i++)
                num += _executors[i].executeUpdate(q, params).longValue();
            return Numbers.valueOf(num);
        }

        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            StoreQuery.Range range) {
            if (_executors.length == 1)
                return _executors[0].getDataStoreActions(q, params, range);

            List results = new ArrayList(_executors.length);
            StoreQuery.Range ropRange = new StoreQuery.Range(0L, range.end);
            String[] actions;
            for (int i = 0; i < _executors.length; i++) {
                actions = _executors[i].getDataStoreActions(q, params,ropRange);
                if (actions != null && actions.length > 0)
                    results.addAll(Arrays.asList(actions));
            }
            return (String[]) results.toArray(new String[results.size()]);
        }

        public void validate(StoreQuery q) {
            _executors[0].validate(q);
        }

        public void getRange(StoreQuery q, Object[] params, 
            StoreQuery.Range range) {
            _executors[0].getRange(q, params, range);
        }

        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int idx) {
            // unfortunately, at this point (must be a merged rop containing
            // other merged rops) we have no idea which executor to extract
            // the value from
            return _executors[0].getOrderingValue(q, params, resultObject, idx);
        }

        public boolean[] getAscending(StoreQuery q) {
            return _executors[0].getAscending(q);
        }

        public String getAlias(StoreQuery q) {
            return _executors[0].getAlias(q);
        }

        public String[] getProjectionAliases(StoreQuery q) {
            return _executors[0].getProjectionAliases(q);
        }

        public Class getResultClass(StoreQuery q) {
            return _executors[0].getResultClass(q);
        }

        public Class[] getProjectionTypes(StoreQuery q) {
            return _executors[0].getProjectionTypes(q);
        }

        public boolean isPacking(StoreQuery q) {
            return _executors[0].isPacking(q);
        }

        public ClassMetaData[] getAccessPathMetaDatas(StoreQuery q) {
            if (_executors.length == 1)
                return _executors[0].getAccessPathMetaDatas(q);

            // create set of base class metadatas in access path
            List metas = null;
            for (int i = 0; i < _executors.length; i++)
                metas = Filters.addAccessPathMetaDatas(metas, _executors[i].
                    getAccessPathMetaDatas(q));
            if (metas == null)
                return StoreQuery.EMPTY_METAS;
            return (ClassMetaData[]) metas.toArray
                (new ClassMetaData[metas.size()]);
        }

        public boolean isAggregate(StoreQuery q) {
            if (!_executors[0].isAggregate(q))
                return false;

            // we can't merge aggregates
            throw new UnsupportedException(_loc.get("merged-aggregate",
                q.getContext().getCandidateType(),
                q.getContext().getQueryString()));
        }

        public int getOperation(StoreQuery q) {
            return _executors[0].getOperation(q);
        }

        public boolean hasGrouping(StoreQuery q) {
            return _executors[0].hasGrouping(q);
        }

        public LinkedMap getParameterTypes(StoreQuery q) {
            return _executors[0].getParameterTypes(q);
        }

        public Map getUpdates(StoreQuery q) {
            return _executors[0].getUpdates(q);
        }
    }

    /**
     * Result object provider that packs results before returning them.
     */
    private static class PackingResultObjectProvider
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

        public boolean supportsRandomAccess() {
            return _delegate.supportsRandomAccess();
        }

        public void open()
            throws Exception {
            _delegate.open();
        }

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

        public boolean next()
            throws Exception {
            return _delegate.next();
        }

        public boolean absolute(int pos)
            throws Exception {
            return _delegate.absolute(pos);
        }

        public int size()
            throws Exception {
            return _delegate.size();
        }

        public void reset()
            throws Exception {
            _delegate.reset();
        }

        public void close()
            throws Exception {
            _delegate.close();
        }

        public void handleCheckedException(Exception e) {
            _delegate.handleCheckedException(e);
        }
    }

    /**
     * Result list that removes itself from the query's open result list
     * when it is closed. Public for testing.
     */
    public class RemoveOnCloseResultList
        implements ResultList {

        private final ResultList _res;

        public RemoveOnCloseResultList(ResultList res) {
            _res = res;
        }

        public ResultList getDelegate() {
            return _res;
        }

        public boolean isProviderOpen() {
            return _res.isProviderOpen();
        }

        public boolean isClosed() {
            return _res.isClosed();
        }

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

        public int size() {
            return _res.size();
        }

        public boolean isEmpty() {
            return _res.isEmpty();
        }

        public boolean contains(Object o) {
            return _res.contains(o);
        }

        public Iterator iterator() {
            return _res.iterator();
        }

        public Object[] toArray() {
            return _res.toArray();
        }

        public Object[] toArray(Object[] a) {
            return _res.toArray(a);
        }

        public boolean add(Object o) {
            return _res.add(o);
        }

        public boolean remove(Object o) {
            return _res.remove(o);
        }

        public boolean containsAll(Collection c) {
            return _res.containsAll(c);
        }

        public boolean addAll(Collection c) {
            return _res.addAll(c);
        }

        public boolean addAll(int idx, Collection c) {
            return _res.addAll(idx, c);
        }

        public boolean removeAll(Collection c) {
            return _res.removeAll(c);
        }

        public boolean retainAll(Collection c) {
            return _res.retainAll(c);
        }

        public void clear() {
            _res.clear();
        }

        public Object get(int idx) {
            return _res.get(idx);
        }

        public Object set(int idx, Object o) {
            return _res.set(idx, o);
        }

        public void add(int idx, Object o) {
            _res.add(idx, o);
        }

        public Object remove(int idx) {
            return _res.remove(idx);
        }

        public int indexOf(Object o) {
            return _res.indexOf(o);
        }

        public int lastIndexOf(Object o) {
            return _res.lastIndexOf(o);
        }

        public ListIterator listIterator() {
            return _res.listIterator();
        }

        public ListIterator listIterator(int idx) {
            return _res.listIterator(idx);
        }

        public List subList(int start, int end) {
            return _res.subList(start, end);
        }

        public boolean equals(Object o) {
            return _res.equals(o);
        }

        public int hashCode() {
            return _res.hashCode();
        }

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

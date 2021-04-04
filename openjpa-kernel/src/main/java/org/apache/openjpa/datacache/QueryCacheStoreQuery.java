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
package org.apache.openjpa.datacache;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.openjpa.datacache.AbstractQueryCache.EvictPolicy;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.ResultShape;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.lib.util.collections.LinkedMap;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ObjectNotFoundException;


/**
 * A {@link StoreQuery} implementation that caches the OIDs involved in
 * the query, and can determine whether or not the query has been dirtied.
 *
 * @author Patrick Linskey
 * @since 0.2.5.0
 */
public class QueryCacheStoreQuery
    implements StoreQuery {

    private final StoreQuery _query;
    private final QueryCache _cache;
    private StoreContext _sctx;
    private MetaDataRepository _repos;

    /**
     * Create a new instance that delegates to <code>query</code> if no
     * cached results are available.
     */
    public QueryCacheStoreQuery(StoreQuery query, QueryCache cache) {
        _query = query;
        _cache = cache;
    }

    /**
     * Return the {@link QueryCache} that this object is associated with.
     */
    public QueryCache getCache() {
        return _cache;
    }

    /**
     * Delegate.
     */
    public StoreQuery getDelegate() {
        return _query;
    }

    /**
     * Look in the query cache for a result for the given query
     * key. Only look if this query is being executed outside a
     * transaction or in a transaction with IgnoreChanges set to true
     * or in a transaction with IgnoreChanges set to false but in which
     * none of the classes involved in this query have been touched.
     *  Caching is not used when using object locking.
     * This is because we must obtain locks on the
     * data, and it is likely that making n trips to the database to
     * make the locks will be slower than running the query against
     * the database.
     *  If the fetch configuration has query caching disabled,
     * then this method returns <code>null</code>.
     *  Return the list if we meet the above criteria and if a list
     * is found for <code>qk</code>. Else, return
     * <code>null</code>.
     *  This implementation means that queries against the cache
     * are of READ_COMMITTED isolation level. It'd be nice to support
     * READ_SERIALIZABLE -- to do so, we'd just return false when in
     * a transaction.
     */
    private List<Object> checkCache(QueryKey qk, FetchConfiguration loadFc) {
        if (qk == null)
            return null;
        FetchConfiguration fetch = getContext().getFetchConfiguration();
        if (!fetch.getQueryCacheEnabled())
            return null;
        if (fetch.getReadLockLevel() > LockLevels.LOCK_NONE)
            return null;

        // get the cached data
        QueryResult res = _cache.get(qk);
        if (res == null) {
            return null;
        }

        // this if block is invoked if the evictOnTimestamp is set to true
        if (_cache instanceof AbstractQueryCache) {
            AbstractQueryCache qcache = (AbstractQueryCache) _cache;
            if (qcache.getEvictPolicy() == EvictPolicy.TIMESTAMP) {
                Set<String> classNames = qk.getAcessPathClassNames();
                List<String> keyList = new ArrayList<>(classNames);

                List<Long> timestamps =
                    qcache.getAllEntityTimestamp(keyList);
                long queryTS = res.getTimestamp();
                if (timestamps != null) {
                    for (Long ts: timestamps) {
                        // if this is true we have to evict the query
                        // from cache
                        if (queryTS <= ts) {
                            qcache.remove(qk);
                            return null;
                        }
                    }
                }
            }
        }

        if (res.isEmpty()) {
            return Collections.emptyList();
        }

        int projs = getContext().getProjectionAliases().length;
        if (projs == 0) {
            // We're only going to return the cached results if we have ALL results cached. This could be improved
            // in the future to be a little more intelligent.
            if (!getContext().getStoreContext().isCached(res)) {
                return null;
            }
        }
        return new CachedList(res, projs != 0, _sctx, loadFc);
    }

    /**
     * Wrap the result object provider returned by our delegate in a
     * caching provider.
     */
    private ResultObjectProvider wrapResult(ResultObjectProvider rop,
        QueryKey key) {
        if (key == null)
            return rop;
        return new CachingResultObjectProvider(rop, getContext().
            getProjectionAliases().length > 0, key);
    }

    /**
     * Copy a projection element for caching / returning.
     */
    private static Object copyProjection(Object obj, StoreContext ctx, FetchConfiguration fc) {
        if (obj == null)
            return null;
        switch (JavaTypes.getTypeCode(obj.getClass())) {
            case JavaTypes.STRING:
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
            case JavaTypes.OID:
                return obj;
            case JavaTypes.DATE:
                return ((Date) obj).clone();
            case JavaTypes.LOCALE:
                return ((Locale) obj).clone();
            default:
                if (obj instanceof CachedObjectId)
                    return fromObjectId(((CachedObjectId) obj).oid, ctx, fc);
                Object oid = ctx.getObjectId(obj);
                if (oid != null)
                    return new CachedObjectId(oid);
                return obj;
        }
    }

    /**
     * Return the result object based on its cached oid.
     */
    private static Object fromObjectId(Object oid, StoreContext sctx, FetchConfiguration fc) {
        if (oid == null)
            return null;

        Object obj = sctx.find(oid, fc, null, null, 0);
        if (obj == null)
            throw new ObjectNotFoundException(oid);
        return obj;
    }

    public Object writeReplace()
        throws ObjectStreamException {
        return _query;
    }

    @Override
    public QueryContext getContext() {
        return _query.getContext();
    }

    @Override
    public void setContext(QueryContext qctx) {
        _query.setContext(qctx);
        _sctx = qctx.getStoreContext();
        _repos = _sctx.getConfiguration().getMetaDataRepositoryInstance();
    }

    @Override
    public boolean setQuery(Object query) {
        return _query.setQuery(query);
    }

    @Override
    public FilterListener getFilterListener(String tag) {
        return _query.getFilterListener(tag);
    }

    @Override
    public AggregateListener getAggregateListener(String tag) {
        return _query.getAggregateListener(tag);
    }

    @Override
    public Object newCompilationKey() {
        return _query.newCompilationKey();
    }

    @Override
    public Object newCompilation() {
        return _query.newCompilation();
    }

    @Override
    public Object getCompilation() {
        return _query.getCompilation();
    }

    @Override
    public void populateFromCompilation(Object comp) {
        _query.populateFromCompilation(comp);
    }

    @Override
    public void invalidateCompilation() {
        _query.invalidateCompilation();
    }

    @Override
    public boolean supportsDataStoreExecution() {
        return _query.supportsDataStoreExecution();
    }

    @Override
    public boolean supportsInMemoryExecution() {
        return _query.supportsInMemoryExecution();
    }

    @Override
    public Executor newInMemoryExecutor(ClassMetaData meta, boolean subs) {
        return _query.newInMemoryExecutor(meta, subs);
    }

    @Override
    public Executor newDataStoreExecutor(ClassMetaData meta, boolean subs) {
        Executor ex = _query.newDataStoreExecutor(meta, subs);
        return new QueryCacheExecutor(ex, meta, subs,
                      getContext().getFetchConfiguration());
    }

    @Override
    public boolean supportsAbstractExecutors() {
        return _query.supportsAbstractExecutors();
    }

    @Override
    public boolean requiresCandidateType() {
        return _query.requiresCandidateType();
    }

    @Override
    public boolean requiresParameterDeclarations() {
        return _query.requiresParameterDeclarations();
    }

    @Override
    public boolean supportsParameterDeclarations() {
        return _query.supportsParameterDeclarations();
    }

    @Override
    public Object evaluate(Object value, Object ob, Object[] params,
        OpenJPAStateManager sm) {
        return _query.evaluate(value, ob, params, sm);
    }

    /**
     * Caching executor.
     */
    private static class QueryCacheExecutor
        implements Executor {

        private final Executor _ex;
        private final Class<?> _candidate;
        private final boolean _subs;
        private final FetchConfiguration _fc;

        public QueryCacheExecutor(Executor ex, ClassMetaData meta,
            boolean subs, FetchConfiguration fc) {
            _ex = ex;
            _candidate = (meta == null) ? null : meta.getDescribedType();
            _subs = subs;
            _fc = fc;
        }

        @Override
        public ResultObjectProvider executeQuery(StoreQuery q, Object[] params, Range range) {
            QueryCacheStoreQuery cq = (QueryCacheStoreQuery) q;
            Object parsed = cq.getDelegate().getCompilation();
            QueryKey key =
                QueryKey.newInstance(cq.getContext(), _ex.isPacking(q), params, _candidate, _subs, range.start,
                    range.end, parsed);

            // Create a new FetchConfiguration that will be used to ensure that any JOIN FETCHed fields are loaded
            StoreContext store = q.getContext().getStoreContext();
            FetchConfiguration cacheFc = store.pushFetchConfiguration();

            // OPENJPA-2586: If the FetchConfig for this executor contains fields,
            // then add them to the new FetchConfig.
            if (!_fc.getFields().isEmpty()) {
              cacheFc.addFields(_fc.getFields());
            }

            for (QueryExpressions qe : _ex.getQueryExpressions()) {
                for (String fetchFields : qe.fetchPaths) {
                    cacheFc.addField(fetchFields);
                }
                for (String fetchFields : qe.fetchInnerPaths) {
                    cacheFc.addField(fetchFields);
                }
            }
            try {
                List<Object> cached = cq.checkCache(key, cacheFc);
                if (cached != null) {
                    return new ListResultObjectProvider(cached);
                }
            } finally {
                store.popFetchConfiguration();
            }

            ResultObjectProvider rop = _ex.executeQuery(cq.getDelegate(), params, range);
            if (_fc.getQueryCacheEnabled())
                return cq.wrapResult(rop, key);
            else
                return rop;
        }

        @Override
        public QueryExpressions[] getQueryExpressions() {
            return _ex.getQueryExpressions();
        }

        /**
         * Clear the cached queries associated with the access path
         * classes in the query. This is done when bulk operations
         * (such as deletes or updates) are performed so that the
         * cache remains up-to-date.
         */
        private void clearAccessPath(StoreQuery q) {
            if (q == null)
                return;

            ClassMetaData[] cmd = getAccessPathMetaDatas(q);
            if (cmd == null || cmd.length == 0)
                return;

            List<Class<?>> classes = new ArrayList<>(cmd.length);
            for (int i = 0; i < cmd.length; i++)
                classes.add(cmd[i].getDescribedType());

            // evict from the query cache
            QueryCacheStoreQuery cq = (QueryCacheStoreQuery) q;
            cq.getCache().onTypesChanged(new TypesChangedEvent
                (q.getContext(), classes));

            // evict from the data cache
            for (int i = 0; i < cmd.length; i++) {
                if (cmd[i].getDataCache() != null && cmd[i].getDataCache().getEvictOnBulkUpdate())
                    cmd[i].getDataCache().removeAll(
                        cmd[i].getDescribedType(), true);
            }
        }

        @Override
        public Number executeDelete(StoreQuery q, Object[] params) {
            try {
                return _ex.executeDelete(unwrap(q), params);
            } finally {
                clearAccessPath(q);
            }
        }

        @Override
        public Number executeUpdate(StoreQuery q, Object[] params) {
            try {
                return _ex.executeUpdate(unwrap(q), params);
            } finally {
                clearAccessPath(q);
            }
        }

        @Override
        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            Range range) {
            return EMPTY_STRINGS;
        }

        @Override
        public void validate(StoreQuery q) {
            _ex.validate(unwrap(q));
        }

        @Override
        public void getRange(StoreQuery q, Object[] params, Range range) {
            _ex.getRange(q, params, range);
        }

        @Override
        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int orderIndex) {
            return _ex.getOrderingValue(unwrap(q), params, resultObject,
                orderIndex);
        }

        @Override
        public boolean[] getAscending(StoreQuery q) {
            return _ex.getAscending(unwrap(q));
        }

        @Override
        public boolean isPacking(StoreQuery q) {
            return _ex.isPacking(unwrap(q));
        }

        @Override
        public String getAlias(StoreQuery q) {
            return _ex.getAlias(unwrap(q));
        }

        @Override
        public Class<?> getResultClass(StoreQuery q) {
            return _ex.getResultClass(unwrap(q));
        }

        @Override
        public ResultShape<?> getResultShape(StoreQuery q) {
            return _ex.getResultShape(q);
        }

        @Override
        public String[] getProjectionAliases(StoreQuery q) {
            return _ex.getProjectionAliases(unwrap(q));
        }

        @Override
        public Class<?>[] getProjectionTypes(StoreQuery q) {
            return _ex.getProjectionTypes(unwrap(q));
        }

        @Override
        public ClassMetaData[] getAccessPathMetaDatas(StoreQuery q) {
            return _ex.getAccessPathMetaDatas(unwrap(q));
        }

        @Override
        public int getOperation(StoreQuery q) {
            return _ex.getOperation(unwrap(q));
        }

        @Override
        public boolean isAggregate(StoreQuery q) {
            return _ex.isAggregate(unwrap(q));
        }

        @Override
        public boolean isDistinct(StoreQuery q) {
            return _ex.isDistinct(unwrap(q));
        }

        @Override
        public boolean hasGrouping(StoreQuery q) {
            return _ex.hasGrouping(unwrap(q));
        }

        @Override
        public OrderedMap<Object, Class<?>> getOrderedParameterTypes(StoreQuery q) {
            return _ex.getOrderedParameterTypes(unwrap(q));
        }

        @Override
        public LinkedMap getParameterTypes(StoreQuery q) {
            return _ex.getParameterTypes(unwrap(q));
        }

        @Override
        public Object[] toParameterArray(StoreQuery q, Map userParams) {
            return _ex.toParameterArray(q, userParams);
        }

        @Override
        public Map getUpdates(StoreQuery q) {
            return _ex.getUpdates(unwrap(q));
        }

        private static StoreQuery unwrap(StoreQuery q) {
            return ((QueryCacheStoreQuery) q).getDelegate();
        }
    }

    /**
     * Result list implementation for a cached query result. Public
     * for testing.
     */
    public static class CachedList extends AbstractList<Object>
        implements Serializable {

        private final QueryResult _res;
        private final boolean _proj;
        private final StoreContext _sctx;
        private final FetchConfiguration _fc;

        public CachedList(QueryResult res, boolean proj, StoreContext ctx, FetchConfiguration fc) {
            _res = res;
            _proj = proj;
            _sctx = ctx;
            _fc = fc;
        }

        @Override
        public Object get(int idx) {
            if (!_proj)
                return fromObjectId(_res.get(idx), _sctx, _fc);

            Object[] cached = (Object[]) _res.get(idx);
            if (cached == null)
                return null;
            Object[] uncached = new Object[cached.length];
            for (int i = 0; i < cached.length; i++)
                uncached[i] = copyProjection(cached[i], _sctx, _fc);
            return uncached;
        }

        @Override
        public int size() {
            return _res.size();
        }

        public Object writeReplace()
            throws ObjectStreamException {
            return new ArrayList<>(this);
        }
    }

    /**
     * A wrapper around a {@link ResultObjectProvider} that builds up a list of
     * all the OIDs in this list and registers that list with the
     * query cache. Abandons monitoring and registering if one of the classes
     * in the access path is modified while the query results are being loaded.
     */
    private class CachingResultObjectProvider
        implements ResultObjectProvider, TypesChangedListener {

        private final ResultObjectProvider _rop;
        private final boolean _proj;
        private final QueryKey _qk;
        private final TreeMap<Integer,Object> _data = new TreeMap<>();
        private boolean _maintainCache = true;
        private int _pos = -1;

        // used to determine list size without necessarily calling size(),
        // which may require a DB trip or return Integer.MAX_VALUE
        private int _max = -1;
        private int _size = Integer.MAX_VALUE;

        /**
         * Constructor. Supply delegate result provider and our query key.
         */
        public CachingResultObjectProvider(ResultObjectProvider rop,
            boolean proj, QueryKey key) {
            _rop = rop;
            _proj = proj;
            _qk = key;
            _cache.addTypesChangedListener(this);
        }

        /**
         * Stop caching.
         */
        private void abortCaching() {
            if (!_maintainCache)
                return;

            // this can be called via an event from another thread
            synchronized (this) {
                // it's important that we set this flag first so that any
                // subsequent calls to this object are bypassed.
                _maintainCache = false;
                _cache.removeTypesChangedListener(this);
                _data.clear();
            }
        }

        /**
         * Check whether we've buffered all results, while optionally adding
         * the given result.
         */
        private void checkFinished(Object obj, boolean result) {
            // this can be called at the same time as abortCaching via
            // a types changed event
            boolean finished = false;
            synchronized (this) {
                if (_maintainCache) {
                    if (result) {
                        Integer index = _pos;
                        if (!_data.containsKey(index)) {
                            Object cached;
                            if (obj == null)
                                cached = null;
                            else if (!_proj)
                                cached = _sctx.getObjectId(obj);
                            else {
                                Object[] arr = (Object[]) obj;
                                Object[] cp = new Object[arr.length];
                                for (int i = 0; i < arr.length; i++)
                                    cp[i] = copyProjection(arr[i], _sctx, null);
                                cached = cp;
                            }
                            if (cached != null)
                                _data.put(index, cached);
                        }
                    }
                    finished = _size == _data.size();
                }
            }

            if (finished) {
                // an abortCaching call can sneak in here via onExpire; the
                // cache is locked during event firings, so the lock here will
                // wait for it (or will force the next firing to wait)
                _cache.writeLock();
                try {
                    // make sure we didn't abort
                    if (_maintainCache) {
                        QueryResult res = null;
                        synchronized (this) {
                            res = new QueryResult(_qk, _data.values());
                            res.setTimestamp(System.currentTimeMillis());
                        }
                        _cache.put(_qk, res);
                        abortCaching();
                    }
                }
                finally {
                    _cache.writeUnlock();
                }
            }
        }

        @Override
        public boolean supportsRandomAccess() {
            return _rop.supportsRandomAccess();
        }

        @Override
        public void open()
            throws Exception {
            _rop.open();
        }

        @Override
        public Object getResultObject()
            throws Exception {
            Object obj = _rop.getResultObject();
            checkFinished(obj, true);
            return obj;
        }

        @Override
        public boolean next()
            throws Exception {
            _pos++;
            boolean next = _rop.next();
            if (!next && _pos == _max + 1) {
                _size = _pos;
                checkFinished(null, false);
            } else if (next && _pos > _max)
                _max = _pos;
            return next;
        }

        @Override
        public boolean absolute(int pos)
            throws Exception {
            _pos = pos;
            boolean valid = _rop.absolute(pos);
            if (!valid && _pos == _max + 1) {
                _size = _pos;
                checkFinished(null, false);
            } else if (valid && _pos > _max)
                _max = _pos;
            return valid;
        }

        @Override
        public int size()
            throws Exception {
            if (_size != Integer.MAX_VALUE)
                return _size;
            int size = _rop.size();
            _size = size;
            checkFinished(null, false);
            return size;
        }

        @Override
        public void reset()
            throws Exception {
            _rop.reset();
            _pos = -1;
        }

        @Override
        public void close()
            throws Exception {
            abortCaching();
            _rop.close();
        }

        @Override
        public void handleCheckedException(Exception e) {
            _rop.handleCheckedException(e);
        }

        @Override
        public void onTypesChanged(TypesChangedEvent ev) {
            if (_qk.changeInvalidatesQuery(ev.getTypes()))
                abortCaching();
        }
    }

    /**
     * Struct to recognize cached oids.
     */
    private static class CachedObjectId implements java.io.Serializable {

        
        private static final long serialVersionUID = 1L;
        public final Object oid;

        public CachedObjectId (Object oid)
        {
            this.oid = oid;
        }
    }
}

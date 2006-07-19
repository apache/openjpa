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
package org.apache.openjpa.datacache;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ObjectNotFoundException;
import serp.util.Numbers;

/**
 * A {@link StoreQuery} implementation that caches the OIDs involved in
 * the query, and can determine whether or not the query has been dirtied.
 *
 * @author Patrick Linskey
 * @since 2.5.0
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
    private List checkCache(QueryKey qk) {
        if (qk == null)
            return null;
        FetchConfiguration fetch = getContext().getFetchConfiguration();
        if (!fetch.getQueryCache())
            return null;
        if (fetch.getReadLockLevel() > LockLevels.LOCK_NONE)
            return null;

        // get the cached oids
        QueryResult res = _cache.get(qk);
        if (res == null)
            return null;
        if (res.isEmpty())
            return Collections.EMPTY_LIST;

        int projs = getContext().getProjectionAliases().length;
        if (projs == 0) {
            // make sure the data cache contains the oids for the query result;
            // if it doesn't, then using the result could be slower than not
            // using it becauseo of the individual by-oid lookups
            ClassMetaData meta = _repos.getMetaData(getContext().
                getCandidateType(), _sctx.getClassLoader(), true);
            BitSet idxs = meta.getDataCache().containsAll(res);

            // eventually we should optimize this to figure out how many objects
            // the cache is missing and if only a few do a bulk fetch for them
            int len = idxs.length();
            if (len < res.size())
                return null;
            for (int i = 0; i < len; i++)
                if (!idxs.get(i))
                    return null;
        }
        return new CachedList(res, projs != 0, _sctx);
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
    private static Object copyProjection(Object obj, StoreContext ctx) {
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
                    return fromObjectId(((CachedObjectId) obj).oid, ctx);
                Object oid = ctx.getObjectId(obj);
                if (oid != null)
                    return new CachedObjectId(oid);
                return obj;
        }
    }

    /**
     * Return the result object based on its cached oid.
     */
    private static Object fromObjectId(Object oid, StoreContext sctx) {
        if (oid == null)
            return null;

        Object obj = sctx.find(oid, null, null, null, 0);
        if (obj == null)
            throw new ObjectNotFoundException(oid);
        return obj;
    }

    public Object writeReplace()
        throws ObjectStreamException {
        return _query;
    }

    public QueryContext getContext() {
        return _query.getContext();
    }

    public void setContext(QueryContext qctx) {
        _query.setContext(qctx);
        _sctx = qctx.getStoreContext();
        _repos = _sctx.getConfiguration().getMetaDataRepository();
    }

    public boolean setQuery(Object query) {
        return _query.setQuery(query);
    }

    public FilterListener getFilterListener(String tag) {
        return _query.getFilterListener(tag);
    }

    public AggregateListener getAggregateListener(String tag) {
        return _query.getAggregateListener(tag);
    }

    public Object newCompilationKey() {
        return _query.newCompilationKey();
    }

    public Object newCompilation() {
        return _query.newCompilation();
    }

    public void populateFromCompilation(Object comp) {
        _query.populateFromCompilation(comp);
    }

    public void invalidateCompilation() {
        _query.invalidateCompilation();
    }

    public boolean supportsDataStoreExecution() {
        return _query.supportsDataStoreExecution();
    }

    public boolean supportsInMemoryExecution() {
        return _query.supportsInMemoryExecution();
    }

    public Executor newInMemoryExecutor(ClassMetaData meta, boolean subs) {
        return _query.newInMemoryExecutor(meta, subs);
    }

    public Executor newDataStoreExecutor(ClassMetaData meta, boolean subs) {
        Executor ex = _query.newDataStoreExecutor(meta, subs);
        return new QueryCacheExecutor(ex, meta, subs);
    }

    public boolean supportsAbstractExecutors() {
        return _query.supportsAbstractExecutors();
    }

    public boolean requiresCandidateType() {
        return _query.requiresCandidateType();
    }

    public boolean requiresParameterDeclarations() {
        return _query.requiresParameterDeclarations();
    }

    public boolean supportsParameterDeclarations() {
        return _query.supportsParameterDeclarations();
    }

    /**
     * Caching executor.
     */
    private static class QueryCacheExecutor
        implements Executor {

        private final Executor _ex;
        private final Class _candidate;
        private final boolean _subs;

        public QueryCacheExecutor(Executor ex, ClassMetaData meta,
            boolean subs) {
            _ex = ex;
            _candidate = (meta == null) ? null : meta.getDescribedType();
            _subs = subs;
        }

        public ResultObjectProvider executeQuery(StoreQuery q, Object[] params,
            boolean lrs, long startIdx, long endIdx) {
            QueryCacheStoreQuery cq = (QueryCacheStoreQuery) q;
            QueryKey key = QueryKey.newInstance(cq.getContext(),
                _ex.isPacking(q), params, _candidate, _subs, startIdx, endIdx);
            List cached = cq.checkCache(key);
            if (cached != null)
                return new ListResultObjectProvider(cached);

            ResultObjectProvider rop = _ex.executeQuery(cq.getDelegate(),
                params, lrs, startIdx, endIdx);
            return cq.wrapResult(rop, key);
        }

        public ResultObjectProvider executeQuery(StoreQuery q, Map params,
            boolean lrs, long startIdx, long endIdx) {
            QueryCacheStoreQuery cq = (QueryCacheStoreQuery) q;
            QueryKey key = QueryKey.newInstance(cq.getContext(),
                _ex.isPacking(q), params, _candidate, _subs, startIdx, endIdx);
            List cached = cq.checkCache(key);
            if (cached != null)
                return new ListResultObjectProvider(cached);

            ResultObjectProvider rop = _ex.executeQuery(cq.getDelegate(),
                params, lrs, startIdx, endIdx);
            return cq.wrapResult(rop, key);
        }

        /**
         * Clear the cached queries associated with the access path
         * classes in the query. This is done when bulk operations
         * (such as deletes or updates) are performed so that the
         * cache remains up-to-date.
         */
        private void clearAccesssPath(StoreQuery q) {
            if (q == null)
                return;

            ClassMetaData[] cmd = getAccessPathMetaDatas(q);
            if (cmd == null || cmd.length == 0)
                return;

            Class[] classes = new Class[cmd.length];
            for (int i = 0; i < cmd.length; i++)
                classes[i] = cmd[i].getDescribedType();

            QueryCacheStoreQuery cq = (QueryCacheStoreQuery) q;
            cq.getCache().onTypesChanged(new TypesChangedEvent
                (q.getContext(), Arrays.asList(classes)));
        }

        public Number executeDelete(StoreQuery q, Object[] params) {
            try {
                return _ex.executeDelete(unwrap(q), params);
            } finally {
                clearAccesssPath(q);
            }
        }

        public Number executeDelete(StoreQuery q, Map params) {
            try {
                return _ex.executeDelete(unwrap(q), params);
            } finally {
                clearAccesssPath(q);
            }
        }

        public Number executeUpdate(StoreQuery q, Object[] params) {
            try {
                return _ex.executeUpdate(unwrap(q), params);
            } finally {
                clearAccesssPath(q);
            }
        }

        public Number executeUpdate(StoreQuery q, Map params) {
            try {
                return _ex.executeUpdate(unwrap(q), params);
            } finally {
                clearAccesssPath(q);
            }
        }

        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            long startIdx, long endIdx) {
            return EMPTY_STRINGS;
        }

        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int orderIndex) {
            return _ex.getOrderingValue(unwrap(q), params, resultObject,
                orderIndex);
        }

        public boolean[] getAscending(StoreQuery q) {
            return _ex.getAscending(unwrap(q));
        }

        public boolean isPacking(StoreQuery q) {
            return _ex.isPacking(unwrap(q));
        }

        public String getAlias(StoreQuery q) {
            return _ex.getAlias(unwrap(q));
        }

        public Class getResultClass(StoreQuery q) {
            return _ex.getResultClass(unwrap(q));
        }

        public String[] getProjectionAliases(StoreQuery q) {
            return _ex.getProjectionAliases(unwrap(q));
        }

        public Class[] getProjectionTypes(StoreQuery q) {
            return _ex.getProjectionTypes(unwrap(q));
        }

        public ClassMetaData[] getAccessPathMetaDatas(StoreQuery q) {
            return _ex.getAccessPathMetaDatas(unwrap(q));
        }

        public int getOperation(StoreQuery q) {
            return _ex.getOperation(unwrap(q));
        }

        public boolean isAggregate(StoreQuery q) {
            return _ex.isAggregate(unwrap(q));
        }

        public boolean hasGrouping(StoreQuery q) {
            return _ex.hasGrouping(unwrap(q));
        }

        public LinkedMap getParameterTypes(StoreQuery q) {
            return _ex.getParameterTypes(unwrap(q));
        }

        public Map getUpdates(StoreQuery q) {
            return _ex.getUpdates(unwrap(q));
        }

        private static StoreQuery unwrap(StoreQuery q) {
            return ((QueryCacheStoreQuery) q).getDelegate();
        }
    }

    /**
     * Result list implementation for a cached query result. Package-protected
     * for testing.
     */
    public static class CachedList
        extends AbstractList
        implements Serializable {

        private final QueryResult _res;
        private final boolean _proj;
        private final StoreContext _sctx;

        public CachedList(QueryResult res, boolean proj, StoreContext ctx) {
            _res = res;
            _proj = proj;
            _sctx = ctx;
        }

        public Object get(int idx) {
            if (!_proj)
                return fromObjectId(_res.get(idx), _sctx);

            Object[] cached = (Object[]) _res.get(idx);
            if (cached == null)
                return null;
            Object[] uncached = new Object[cached.length];
            for (int i = 0; i < cached.length; i++)
                uncached[i] = copyProjection(cached[i], _sctx);
            return uncached;
        }

        public int size() {
            return _res.size();
        }

        public Object writeReplace()
            throws ObjectStreamException {
            return new ArrayList(this);
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
        private final TreeMap _data = new TreeMap();
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
                        Integer index = Numbers.valueOf(_pos);
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
                                    cp[i] = copyProjection(arr[i], _sctx);
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
                        QueryResult res = new QueryResult(_qk, _data.values());
                        _cache.put(_qk, res);
                        abortCaching();
                    }
                }
                finally {
                    _cache.writeUnlock();
                }
            }
        }

        public boolean supportsRandomAccess() {
            return _rop.supportsRandomAccess();
        }

        public void open()
            throws Exception {
            _rop.open();
        }

        public Object getResultObject()
            throws Exception {
            Object obj = _rop.getResultObject();
            checkFinished(obj, true);
            return obj;
        }

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

        public int size()
            throws Exception {
            if (_size != Integer.MAX_VALUE)
                return _size;
            int size = _rop.size();
            _size = size;
            checkFinished(null, false);
            return size;
        }

        public void reset()
            throws Exception {
            _rop.reset();
            _pos = -1;
        }

        public void close()
            throws Exception {
            abortCaching();
            _rop.close();
        }

        public void handleCheckedException(Exception e) {
            _rop.handleCheckedException(e);
        }

        public void onTypesChanged(TypesChangedEvent ev) {
            if (_qk.changeInvalidatesQuery(ev.getTypes()))
                abortCaching();
        }
    }

    /**
     * Struct to recognize cached oids.
     */
    private static class CachedObjectId {

        public final Object oid;

        public CachedObjectId (Object oid)
		{
			this.oid = oid;
		}
	}
}

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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.openjpa.enhance.PCDataGenerator;
import org.apache.openjpa.kernel.DelegatingStoreManager;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FetchState;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;

/**
 * StoreManager proxy that delegates to a data cache when possible.
 *
 * @author Patrick Linskey
 * @nojavadoc
 */
public class DataCacheStoreManager
    extends DelegatingStoreManager {

    // all the state managers changed in this transaction
    private Collection _inserts = null; // statemanagers
    private Map _updates = null; // statemanager -> fmd set
    private Collection _deletes = null; // statemanagers

    // the owning context
    private StoreContext _ctx = null;

    // pc data generator
    private PCDataGenerator _gen = null;

    /**
     * Constructor.
     *
     * @param sm the store manager to delegate to
     */
    public DataCacheStoreManager(StoreManager sm) {
        super(sm);
    }

    public void setContext(StoreContext ctx) {
        _ctx = ctx;
        _gen = ctx.getConfiguration().getDataCacheManagerInstance().
            getPCDataGenerator();
        super.setContext(ctx);
    }

    public void begin() {
        super.begin();
    }

    public void commit() {
        try {
            super.commit();
            updateCaches();
        }
        finally {
            _inserts = null;
            _updates = null;
            _deletes = null;
        }
    }

    public void rollback() {
        try {
            super.rollback();
        }
        finally {
            _inserts = null;
            _updates = null;
            _deletes = null;
        }
    }

    /**
     * Evict all members of the given classes.
     */
    private void evictTypes(Collection classes) {
        if (classes.isEmpty())
            return;

        MetaDataRepository mdr = _ctx.getConfiguration().
            getMetaDataRepository();
        ClassLoader loader = _ctx.getClassLoader();

        Class cls;
        DataCache cache;
        for (Iterator itr = classes.iterator(); itr.hasNext();) {
            cls = (Class) itr.next();
            cache = mdr.getMetaData(cls, loader, false).getDataCache();
            if (cache != null)
                cache.removeAll(cls, false);
        }
    }

    /**
     * Update all caches with the committed inserts, updates, and deletes.
     */
    private void updateCaches() {
        // map each data cache to the modifications we need to perform
        Map modMap = null;
        Modifications mods;
        OpenJPAStateManager sm;
        DataCachePCData data;
        DataCache cache;

        // create pc datas for inserts
        if (_ctx.getPopulateDataCache() && _inserts != null) {
            for (Iterator itr = _inserts.iterator(); itr.hasNext();) {
                sm = (OpenJPAStateManager) itr.next();
                cache = sm.getMetaData().getDataCache();
                if (cache == null)
                    continue;

                if (modMap == null)
                    modMap = new HashMap();
                mods = getModifications(modMap, cache);
                data = newPCData(sm);
                data.store(sm);
                mods.additions.add(new PCDataHolder(data, sm));
            }
        }

        // update pcdatas for updates
        Map.Entry entry;
        if (_updates != null) {
            BitSet fields;
            for (Iterator itr = _updates.entrySet().iterator();
                itr.hasNext();) {
                entry = (Map.Entry) itr.next();
                sm = (OpenJPAStateManager) entry.getKey();
                fields = (BitSet) entry.getValue();

                cache = sm.getMetaData().getDataCache();
                if (cache == null)
                    continue;

                // it's ok not to clone the object that we get from the cache,
                // since we're inside the commit() method, so any modifications
                // to the underlying cache are valid. If the commit had not
                // already succeeded, then we'd want to clone the retrieved
                // object.
                if (modMap == null)
                    modMap = new HashMap();
                data = cache.get(sm.getObjectId());
                mods = getModifications(modMap, cache);

                // data should always be non-null, since the object is
                // dirty, but maybe it got dropped from the cache in the
                // interim
                if (data == null) {
                    data = newPCData(sm);
                    data.store(sm);
                    mods.newUpdates.add(new PCDataHolder(data, sm));
                } else {
                    data.store(sm, fields);
                    mods.existingUpdates.add(new PCDataHolder(data, sm));
                }
            }
        }

        // remove pcdatas for deletes
        if (_deletes != null) {
            for (Iterator itr = _deletes.iterator(); itr.hasNext();) {
                sm = (OpenJPAStateManager) itr.next();
                cache = sm.getMetaData().getDataCache();
                if (cache == null)
                    continue;

                if (modMap == null)
                    modMap = new HashMap();
                mods = getModifications(modMap, cache);
                mods.deletes.add(sm.getObjectId());
            }
        }

        // notify the caches of the changes
        if (modMap != null) {
            for (Iterator itr = modMap.entrySet().iterator(); itr.hasNext();) {
                entry = (Map.Entry) itr.next();
                cache = (DataCache) entry.getKey();
                mods = (Modifications) entry.getValue();

                // make sure we're not caching old versions
                cache.writeLock();
                try {
                    transformToVersionSafePCDatas(cache, mods.additions);
                    transformToVersionSafePCDatas(cache, mods.newUpdates);
                    transformToVersionSafePCDatas(cache, mods.existingUpdates);
                    cache.commit(mods.additions, mods.newUpdates,
                        mods.existingUpdates, mods.deletes);
                }
                finally {
                    cache.writeUnlock();
                }
            }
        }

        // if we were in largeTransaction mode, then we have recorded
        // the classes of updated/deleted objects and these now need to be
        // evicted
        if (_ctx.isLargeTransaction()) {
            evictTypes(_ctx.getDeletedTypes());
            evictTypes(_ctx.getUpdatedTypes());
        }

        // and notify the query cache.  notify in one batch to reduce synch
        QueryCache queryCache = _ctx.getConfiguration().
            getDataCacheManagerInstance().getSystemQueryCache();
        if (queryCache != null) {
            Collection pers = _ctx.getPersistedTypes();
            Collection del = _ctx.getDeletedTypes();
            Collection up = _ctx.getUpdatedTypes();
            int size = pers.size() + del.size() + up.size();
            if (size > 0) {
                Collection types = new ArrayList(size);
                types.addAll(pers);
                types.addAll(del);
                types.addAll(up);
                queryCache.onTypesChanged(new TypesChangedEvent(this, types));
            }
        }
    }

    /**
     * Transforms a collection of {@link PCDataHolder}s that might contain
     * stale instances into a collection of up-to-date {@link DataCachePCData}s.
     */
    private void transformToVersionSafePCDatas(DataCache cache,
        List holders) {
        PCDataHolder holder;
        DataCachePCData oldpc;
        for (ListIterator iter = holders.listIterator(); iter.hasNext();) {
            holder = (PCDataHolder) iter.next();
            oldpc = cache.get(holder.sm.getObjectId());
            if (oldpc != null && compareVersion(holder.sm,
                holder.sm.getVersion(), oldpc.getVersion()) == VERSION_EARLIER)
                iter.remove();
            else
                iter.set(holder.pcdata);
        }
    }

    /**
     * Return a {@link Modifications} instance to track modifications
     * to the given cache, creating and caching the instance if it does
     * not already exist in the given map.
     */
    private static Modifications getModifications(Map modMap, DataCache cache) {
        Modifications mods = (Modifications) modMap.get(cache);
        if (mods == null) {
            mods = new Modifications();
            modMap.put(cache, mods);
        }
        return mods;
    }

    public boolean exists(OpenJPAStateManager sm, Object edata) {
        DataCache cache = sm.getMetaData().getDataCache();
        if (cache != null && !isLocking(null)
            && cache.contains(sm.getObjectId()))
            return true;
        return super.exists(sm, edata);
    }

    public boolean syncVersion(OpenJPAStateManager sm, Object edata) {
        DataCache cache = sm.getMetaData().getDataCache();
        if (cache == null || sm.isEmbedded())
            return super.syncVersion(sm, edata);

        DataCachePCData data;
        Object version = null;
        data = cache.get(sm.getObjectId());
        if (!isLocking(null) && data != null)
            version = data.getVersion();

        // if we have a cached version update from there
        if (version != null) {
            if (!version.equals(sm.getVersion())) {
                sm.setVersion(version);
                return false;
            }
            return true;
        }

        // use data store version
        return super.syncVersion(sm, edata);
    }

    public boolean initialize(OpenJPAStateManager sm, PCState state,
        FetchState fetchState, Object edata) {
        DataCache cache = sm.getMetaData().getDataCache();
        if (cache == null || sm.isEmbedded())
            return super.initialize(sm, state, fetchState, edata);

        DataCachePCData data = cache.get(sm.getObjectId());
        FetchConfiguration fetch = (fetchState == null)
            ? _ctx.getFetchConfiguration()
            : fetchState.getFetchConfiguration();
        if (data != null && !isLocking(fetch)) {
            //### the 'data.type' access here probably needs to be
            //### addressed for bug 511
            sm.initialize(data.getType(), state);
            data.load(sm, fetchState, edata);
            return true;
        }

        // initialize from store manager
        if (!super.initialize(sm, state, fetchState, edata))
            return false;
        if (!_ctx.getPopulateDataCache())
            return true;

        // make sure that we're not trying to cache an old version
        cache.writeLock();
        try {
            data = cache.get(sm.getObjectId());
            if (data != null && compareVersion(sm, sm.getVersion(),
                data.getVersion()) == VERSION_EARLIER)
                return true;

            // cache newly loaded info. It is safe to cache data frorm
            // initialize() because this method is only called upon
            // initial load of the data.
            if (data == null)
                data = newPCData(sm);
            data.store(sm);
            cache.put(data);
        }
        finally {
            cache.writeUnlock();
        }
        return true;
    }

    public boolean load(OpenJPAStateManager sm, BitSet fields,
        FetchState fetchState, int lockLevel, Object edata) {
        FetchConfiguration fetch = (fetchState == null)
            ? _ctx.getFetchConfiguration()
            : fetchState.getFetchConfiguration();
        DataCache cache = sm.getMetaData().getDataCache();
        if (cache == null || sm.isEmbedded())
            return super.load(sm, fields, fetchState, lockLevel, edata);

        DataCachePCData data = cache.get(sm.getObjectId());
        if (lockLevel == LockLevels.LOCK_NONE && !isLocking(fetch)
            && data != null)
            data.load(sm, fields, fetchState, edata);
        if (fields.length() == 0)
            return true;

        // load from store manager; clone the set of still-unloaded fields
        // so that if the store manager decides to modify it it won't affect us
        if (!super.load(sm, (BitSet) fields.clone(), fetchState,
            lockLevel, edata))
            return false;
        if (!_ctx.getPopulateDataCache())
            return true;

        // make sure that we're not trying to cache an old version
        cache.writeLock();
        try {
            data = cache.get(sm.getObjectId());
            if (data != null && compareVersion(sm, sm.getVersion(),
                data.getVersion()) == VERSION_EARLIER)
                return true;

            // cache newly loaded info
            boolean isNew = data == null;
            if (isNew)
                data = newPCData(sm);
            data.store(sm, fields);
            if (isNew)
                cache.put(data);
            else
                cache.update(data);
        }
        finally {
            cache.writeUnlock();
        }
        return true;
    }

    public Collection loadAll(Collection sms, PCState state, int load,
        FetchState fetchState, Object edata) {
        FetchConfiguration fetch = (fetchState == null)
            ? _ctx.getFetchConfiguration()
            : fetchState.getFetchConfiguration();
        if (isLocking(fetch))
            return super.loadAll(sms, state, load, fetchState, edata);

        Map unloaded = null;
        OpenJPAStateManager sm;
        DataCache cache;
        DataCachePCData data;
        BitSet fields;

        for (Iterator itr = sms.iterator(); itr.hasNext();) {
            sm = (OpenJPAStateManager) itr.next();
            cache = sm.getMetaData().getDataCache();
            if (cache == null || sm.isEmbedded()) {
                unloaded = addUnloaded(sm, null, unloaded);
                continue;
            }

            if (sm.getManagedInstance() == null) {
                data = cache.get(sm.getObjectId());
                if (data != null) {
                    //### the 'data.type' access here probably needs
                    //### to be addressed for bug 511
                    sm.initialize(data.getType(), state);
                    data.load(sm, fetchState, edata);
                } else
                    unloaded = addUnloaded(sm, null, unloaded);
            } else if (load != FORCE_LOAD_NONE
                || sm.getPCState() == PCState.HOLLOW) {
                data = cache.get(sm.getObjectId());
                if (data != null) {
                    // load unloaded fields
                    fields = sm.getUnloaded(fetchState);
                    data.load(sm, fields, fetchState, edata);
                    if (fields.length() > 0)
                        unloaded = addUnloaded(sm, fields, unloaded);
                } else
                    unloaded = addUnloaded(sm, null, unloaded);
            } else if (!cache.contains(sm.getObjectId()))
                unloaded = addUnloaded(sm, null, unloaded);
        }

        if (unloaded == null)
            return Collections.EMPTY_LIST;

        // load with delegate
        Collection failed = super.loadAll(unloaded.keySet(), state, load,
            fetchState, edata);
        if (!_ctx.getPopulateDataCache())
            return failed;

        // for each loaded instance, merge loaded state into cached data
        Map.Entry entry;
        boolean isNew;
        for (Iterator itr = unloaded.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            sm = (OpenJPAStateManager) entry.getKey();
            fields = (BitSet) entry.getValue();

            cache = sm.getMetaData().getDataCache();
            if (cache == null || sm.isEmbedded() || (failed != null
                && failed.contains(sm.getId())))
                continue;

            // make sure that we're not trying to cache an old version
            cache.writeLock();
            try {
                data = cache.get(sm.getObjectId());
                if (data != null && compareVersion(sm, sm.getVersion(),
                    data.getVersion()) == VERSION_EARLIER)
                    continue;

                isNew = data == null;
                if (isNew)
                    data = newPCData(sm);
                if (fields == null)
                    data.store(sm);
                else
                    data.store(sm, fields);
                if (isNew)
                    cache.put(data);
                else
                    cache.update(data);
            }
            finally {
                cache.writeUnlock();
            }
        }
        return failed;
    }

    /**
     * Helper method to add an unloaded instance to the given map.
     */
    private static Map addUnloaded(OpenJPAStateManager sm, BitSet fields,
        Map unloaded) {
        if (unloaded == null)
            unloaded = new HashMap();
        unloaded.put(sm, fields);
        return unloaded;
    }

    public Collection flush(Collection states) {
        Collection exceps = super.flush(states);
        if (!exceps.isEmpty() || _ctx.isLargeTransaction())
            return exceps;

        OpenJPAStateManager sm;
        for (Iterator itr = states.iterator(); itr.hasNext();) {
            sm = (OpenJPAStateManager) itr.next();

            if (sm.getPCState() == PCState.PNEW) {
                if (_inserts == null)
                    _inserts = new LinkedList();
                _inserts.add(sm);
            } else if (_inserts != null &&
                sm.getPCState() == PCState.PNEWDELETED ||
                sm.getPCState() == PCState.PNEWFLUSHEDDELETED)
                _inserts.remove(sm);
            else if (sm.getPCState() == PCState.PDIRTY) {
                if (_updates == null)
                    _updates = new HashMap();
                _updates.put(sm, sm.getDirty());
            } else if (sm.getPCState() == PCState.PDELETED) {
                if (_deletes == null)
                    _deletes = new LinkedList();
                _deletes.add(sm);
            }
        }
        return Collections.EMPTY_LIST;
    }

    public StoreQuery newQuery(String language) {
        StoreQuery q = super.newQuery(language);

        // if the query can't be parsed or it's using a non-parsed language
        // (one for which there is no OpenJPA ExpressionParser), we can't cache it.
        if (q == null || QueryLanguages.parserForLanguage(language) == null)
            return q;

        QueryCache queryCache = _ctx.getConfiguration().
            getDataCacheManagerInstance().getSystemQueryCache();
        if (queryCache == null)
            return q;

        return new QueryCacheStoreQuery(q, queryCache);
    }

    /**
     * Create a new cacheable instance for the given state manager.
     */
    private DataCachePCData newPCData(OpenJPAStateManager sm) {
        ClassMetaData meta = sm.getMetaData();
        if (_gen != null)
            return (DataCachePCData) _gen.generatePCData
                (sm.getObjectId(), meta);
        return new DataCachePCDataImpl(sm.getObjectId(), meta);
    }

    /**
     * Return whether the context is locking loaded data.
     */
    private boolean isLocking(FetchConfiguration fetch) {
        if (fetch == null)
            fetch = _ctx.getFetchConfiguration();
        return fetch.getReadLockLevel() > LockLevels.LOCK_NONE;
    }

    /**
     * Structure used during the commit process to track cache modifications.
     */
    private static class Modifications {

        public final List additions = new LinkedList();
        public final List newUpdates = new LinkedList();
        public final List existingUpdates = new LinkedList();
        public final List deletes = new LinkedList();
    }

    private static class PCDataHolder {

        public final DataCachePCData pcdata;
        public final OpenJPAStateManager sm;

        public PCDataHolder(DataCachePCData pcdata,
            OpenJPAStateManager sm) {
            this.pcdata = pcdata;
            this.sm = sm;
		}
	}
}


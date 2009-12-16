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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.openjpa.enhance.PCDataGenerator;
import org.apache.openjpa.kernel.DelegatingStoreManager;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.OptimisticException;

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
        } finally {
            _inserts = null;
            _updates = null;
            _deletes = null;
        }
    }

    public void rollback() {
        try {
            super.rollback();
        } finally {
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
            getMetaDataRepositoryInstance();
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
                } finally {
                    cache.writeUnlock();
                }
            }
        }

        // if we were in largeTransaction mode, then we have recorded
        // the classes of updated/deleted objects and these now need to be
        // evicted
        if (_ctx.isTrackChangesByType()) {
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

        Map<Object,Integer> ids = new HashMap<Object,Integer>(holders.size());
        // this list could be removed if DataCache.getAll() took a Collection
        List idList = new ArrayList(holders.size());
        int i = 0;
        for (PCDataHolder holder : (List<PCDataHolder>) holders) {
            ids.put(holder.sm.getObjectId(), i++);
            idList.add(holder.sm.getObjectId());
        }

        Map<Object,DataCachePCData> pcdatas = cache.getAll(idList);
        for (Entry<Object,DataCachePCData> entry : pcdatas.entrySet()) {
            Integer index = ids.get(entry.getKey());
            DataCachePCData oldpc = entry.getValue();
            PCDataHolder holder = (PCDataHolder) holders.get(index);
            if (oldpc != null && compareVersion(holder.sm,
                holder.sm.getVersion(), oldpc.getVersion()) == VERSION_EARLIER)
                holders.remove(index);
            else
                holders.set(index, holder.pcdata);
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
        FetchConfiguration fetch, Object edata) {
        DataCache cache = sm.getMetaData().getDataCache();
        if (cache == null || sm.isEmbedded())
            return super.initialize(sm, state, fetch, edata);

        DataCachePCData data = cache.get(sm.getObjectId());
        if (data != null && !isLocking(fetch)) {
            //### the 'data.type' access here probably needs to be
            //### addressed for bug 511
            sm.initialize(data.getType(), state);
            data.load(sm, fetch, edata);
            return true;
        }

        // initialize from store manager
        if (!super.initialize(sm, state, fetch, edata))
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
        } finally {
            cache.writeUnlock();
        }
        return true;
    }

    public boolean load(OpenJPAStateManager sm, BitSet fields,
        FetchConfiguration fetch, int lockLevel, Object edata) {
        DataCache cache = sm.getMetaData().getDataCache();
        if (cache == null || sm.isEmbedded())
            return super.load(sm, fields, fetch, lockLevel, edata);

        DataCachePCData data = cache.get(sm.getObjectId());
        if (lockLevel == LockLevels.LOCK_NONE && !isLocking(fetch)
            && data != null)
            data.load(sm, fields, fetch, edata);
        if (fields.length() == 0)
            return true;

        // load from store manager; clone the set of still-unloaded fields
        // so that if the store manager decides to modify it it won't affect us
        if (!super.load(sm, (BitSet) fields.clone(), fetch, lockLevel, edata))
            return false;
        if (!_ctx.getPopulateDataCache())
            return true;
        // Do not load changes into cache if the instance has been flushed
        if (sm.isFlushed())
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
        } finally {
            cache.writeUnlock();
        }
        return true;
    }

    /*
     * The next two protected methods remove casting to OpenJPAId
     * (see JIRA OPENJPA-1407).  This avoids a ClassCastException when the 
     * loadAll method is called within openJPA dependent products like Kodo 
     * whose state manager may not return an OpenJPAId.  The change was 
     * implemented as calls to these two protected methods, so that a simple 
     * OpenJPA test case might flag any regressions to this change, even 
     * through OpenJPA itself is comfortable with the cast.
    */
    protected void addObjectIdToOIDList(List oidList, OpenJPAStateManager sm) {
        oidList.add(sm.getObjectId());
    }
    
    protected DataCachePCData getDataFromDataMap(Map dataMap, OpenJPAStateManager sm) {
        return (DataCachePCData) dataMap.get(sm.getObjectId());
    }

    public Collection loadAll(Collection sms, PCState state, int load,
    		FetchConfiguration fetch, Object edata) {
        if (isLocking(fetch))
            return super.loadAll(sms, state, load, fetch, edata);

        Map unloaded = null;
        List smList = null;
        Map caches = new HashMap();
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

            if (sm.getManagedInstance() == null
                || load != FORCE_LOAD_NONE
                || sm.getPCState() == PCState.HOLLOW) {
                smList = (List) caches.get(cache);
                if (smList == null) {
                    smList = new ArrayList();
                    caches.put(cache, smList);
                }
                smList.add(sm);
            } else if (!cache.contains(sm.getObjectId()))
                unloaded = addUnloaded(sm, null, unloaded);
        }
        
        for (Iterator itr = caches.keySet().iterator(); itr.hasNext();) {
            cache = (DataCache) itr.next();
            smList = (List) caches.get(cache);
            List oidList = new ArrayList(smList.size());

            for (itr=smList.iterator();itr.hasNext();) {
                sm = (OpenJPAStateManager) itr.next();
                // avoid cast to OpenJPAId
                addObjectIdToOIDList(oidList,sm);
            }
            
            Map dataMap = cache.getAll(oidList);

            for (itr=smList.iterator();itr.hasNext();) {
                sm = (OpenJPAStateManager) itr.next();
                // avoid cast to OpenJPAId
                data = getDataFromDataMap(dataMap, sm);

                if (sm.getManagedInstance() == null) {
                    if (data != null) {
                        //### the 'data.type' access here probably needs
                        //### to be addressed for bug 511
                        sm.initialize(data.getType(), state);
                        data.load(sm, fetch, edata);
                    } else
                        unloaded = addUnloaded(sm, null, unloaded);
                } else if (load != FORCE_LOAD_NONE
                        || sm.getPCState() == PCState.HOLLOW) {
                    data = cache.get(sm.getObjectId());
                    if (data != null) {
                        // load unloaded fields
                        fields = sm.getUnloaded(fetch);
                        data.load(sm, fields, fetch, edata);
                        if (fields.length() > 0)
                            unloaded = addUnloaded(sm, fields, unloaded);
                    } else
                        unloaded = addUnloaded(sm, null, unloaded);
                }
            }
        }

        if (unloaded == null)
            return Collections.EMPTY_LIST;

        // load with delegate
        Collection failed = super.loadAll(unloaded.keySet(), state, load,
            fetch, edata);
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
            } finally {
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

        // if there were errors evict bad instances and don't record changes
        if (!exceps.isEmpty()) {
            for (Iterator iter = exceps.iterator(); iter.hasNext(); ) {
                Exception e = (Exception) iter.next();
                if (e instanceof OptimisticException)
                    notifyOptimisticLockFailure((OptimisticException) e);
            }
            return exceps;
        }

        // if large transaction mode don't record individual changes
        if (_ctx.isTrackChangesByType())
            return exceps;

        OpenJPAStateManager sm;
        for (Iterator itr = states.iterator(); itr.hasNext();) {
            sm = (OpenJPAStateManager) itr.next();

            if (sm.getPCState() == PCState.PNEW && !sm.isFlushed()) {
                if (_inserts == null)
                    _inserts = new ArrayList();
                _inserts.add(sm);

                // may have been re-persisted
                if (_deletes != null)
                    _deletes.remove(sm);
            } else if (_inserts != null 
                && (sm.getPCState() == PCState.PNEWDELETED 
                || sm.getPCState() == PCState.PNEWFLUSHEDDELETED))
                _inserts.remove(sm);
            else if (sm.getPCState() == PCState.PDIRTY) {
                if (_updates == null)
                    _updates = new HashMap();
                _updates.put(sm, sm.getDirty());
            } else if (sm.getPCState() == PCState.PDELETED) {
                if (_deletes == null)
                    _deletes = new HashSet();
                _deletes.add(sm);
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Fire local staleness detection events from the cache the OID (if
     * available) that resulted in an optimistic lock exception iff the
     * version information in the cache matches the version information
     * in the state manager for the failed instance. This means that we
     * will evict data from the cache for records that should have
     * successfully committed according to the data cache but
     * did not. The only predictable reason that could cause this behavior
     * is a concurrent out-of-band modification to the database that was not 
     * communicated to the cache. This logic makes OpenJPA's data cache 
     * somewhat tolerant of such behavior, in that the cache will be cleaned 
     * up as failures occur.
     */
    private void notifyOptimisticLockFailure(OptimisticException e) {
        Object o = e.getFailedObject();
        OpenJPAStateManager sm = _ctx.getStateManager(o);
        if (sm == null)
            return;
        Object oid = sm.getId();
        boolean remove;

        // this logic could be more efficient -- we could aggregate
        // all the cache->oid changes, and then use DataCache.removeAll() 
        // and less write locks to do the mutation.
        ClassMetaData meta = sm.getMetaData();
        DataCache cache = meta.getDataCache();
        if (cache == null)
            return;

        cache.writeLock();
        try {
            DataCachePCData data = cache.get(oid);
            if (data == null)
                return;

            switch (compareVersion(sm, sm.getVersion(), data.getVersion())) {
                case StoreManager.VERSION_LATER:
                case StoreManager.VERSION_SAME:
                    // This tx's current version is later than or the same as 
                    // the data cache version. In this case, the commit should 
                    // have succeeded from the standpoint of the cache. Remove 
                    // the instance from cache in the hopes that the cache is 
                    // out of sync.
                    remove = true;
                    break;
                case StoreManager.VERSION_EARLIER:
                    // This tx's current version is earlier than the data 
                    // cache version. This is a normal optimistic lock failure. 
                    // Do not clean up the cache; it probably already has the 
                    // right values, and if not, it'll get cleaned up by a tx
                    // that fails in one of the other case statements.
                    remove = false;
                    break;
                case StoreManager.VERSION_DIFFERENT:
                    // The version strategy for the failed object does not
                    // store enough information to optimize for expected
                    // failures. Clean up the cache.
                    remove = true;
                    break;
                default:
                    // Unexpected return value. Remove to be future-proof.
                    remove = true;
                    break;
            }
            if (remove)
                // remove directly instead of via the RemoteCommitListener
                // since we have a write lock here already, so this is more
                // efficient than read-locking and then write-locking later.
                cache.remove(sm.getId());
        } finally {
            cache.writeUnlock();
        }

        // fire off a remote commit stalenesss detection event.
        _ctx.getConfiguration().getRemoteCommitEventManager()
            .fireLocalStaleNotification(oid);
    }

    public StoreQuery newQuery(String language) {
        StoreQuery q = super.newQuery(language);

        // if the query can't be parsed or it's using a non-parsed language
        // (one for which there is no ExpressionParser), we can't cache it.
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
        return new DataCachePCDataImpl(sm.fetchObjectId(), meta);
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

        public final List additions = new ArrayList();
        public final List newUpdates = new ArrayList();
        public final List existingUpdates = new ArrayList();
        public final List deletes = new ArrayList();
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


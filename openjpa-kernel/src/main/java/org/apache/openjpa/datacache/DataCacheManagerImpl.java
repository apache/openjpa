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

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.conf.ObjectValue;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.util.ImplHelper;

/**
 * Default data cache manager.
 *
 * @author Abe White
 * @author Patrick Linskey
 */
public class DataCacheManagerImpl
    implements Closeable, DataCacheManager {

    private DataCache _cache = null;
    private QueryCache _queryCache = null;
    private DataCachePCDataGenerator _pcGenerator = null;
    private DataCacheScheduler _scheduler = null;
    private CacheDistributionPolicy _policy = new CacheDistributionPolicy.Default();

    public void initialize(OpenJPAConfiguration conf, ObjectValue dataCache, ObjectValue queryCache) {
        _cache = (DataCache) dataCache.instantiate(DataCache.class, conf);
        if (_cache == null)
            return;
         
        // create helpers before initializing caches
        if (conf.getDynamicDataStructs())
            _pcGenerator = new DataCachePCDataGenerator(conf);
        _scheduler = new DataCacheScheduler(conf);

        _cache.initialize(this);
        _queryCache = (QueryCache) queryCache.instantiate(QueryCache.class, conf);
        if (_queryCache != null)
            _queryCache.initialize(this);
    }

    public DataCache getSystemDataCache() {
        return getDataCache(null, false);
    }

    public DataCache getDataCache(String name) {
        return getDataCache(name, false);
    }

    /**
     * Returns the named cache. 
     */
    public DataCache getDataCache(String name, boolean create) {
        if (name == null || (_cache != null && name.equals(_cache.getName())))
            return _cache;
        if (_cache != null)
            return _cache.getPartition(name, create);
        return null;
    }

    public QueryCache getSystemQueryCache() {
        return _queryCache;
    }

    public DataCachePCDataGenerator getPCDataGenerator() {
        return _pcGenerator;
    }

    public DataCacheScheduler getDataCacheScheduler() {
        return _scheduler;
    }

    public void close() {
        ImplHelper.close(_cache);
        ImplHelper.close(_queryCache);
        if (_scheduler != null)
            _scheduler.stop();
    }

    public DataCache selectCache(OpenJPAStateManager sm) {
        if (sm == null)
            return null;
        if (_cache instanceof AbstractDataCache 
        && ((AbstractDataCache)_cache).isExcludedType(sm.getMetaData().getDescribedType().getName()))
            return null;
        String name = _policy.selectCache(sm, null);
        return name == null ? null : getDataCache(name);
    }
    
    public CacheDistributionPolicy getDistributionPolicy() {
        return _policy;
    }
    
    public void setDistributionPolicy(CacheDistributionPolicy policy) {
        _policy = policy;
    }
}

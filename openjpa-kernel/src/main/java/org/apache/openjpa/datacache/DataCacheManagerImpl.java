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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PCDataGenerator;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.conf.ObjectValue;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.ImplHelper;

import serp.util.Strings;

/**
 * Default data cache manager provides handle to utilities {@linkplain PCDataGenerator}, {@linkplain DataCacheScheduler}
 * and {@linkplain CacheDistributionPolicy} for the cache operation. This implementation also determines whether a
 * managed type is eligible to cache.
 * 
 * @author Abe White
 * @author Patrick Linskey
 * @author Pinaki Poddar
 */
public class DataCacheManagerImpl
    implements Closeable, DataCacheManager {

    private OpenJPAConfiguration _conf;
    private DataCache _cache = null;
    private QueryCache _queryCache = null;
    private DataCachePCDataGenerator _pcGenerator = null;
    private DataCacheScheduler _scheduler = null;
    private CacheDistributionPolicy _policy = new CacheDistributionPolicy.Default();
    private Set<String> _excludedTypes;
    private Set<String> _includedTypes;

    public void initialize(OpenJPAConfiguration conf, ObjectValue dataCache, ObjectValue queryCache) {
        _conf = conf;
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
     * If the given name is name or the name of the cache plugin then returns the main cache.
     * Otherwise, {@linkplain DataCache#getPartition(String, boolean) delegates} to the main cache
     * to obtain a partition.   
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

    /**
     * Select cache for the given managed instance.
     * If type based verification affirms the type to be cached then the instance based policy 
     * is called to determine the target cache.  
     */
    public DataCache selectCache(OpenJPAStateManager sm) {
        if (sm == null || !isCachable(sm.getMetaData()))
            return null;
        String name = _policy.selectCache(sm, null);
        return name == null ? null : getDataCache(name);
    }
    
    /**
     * Gets the instance-based cache distribution policy, if configured. 
     */
    public CacheDistributionPolicy getDistributionPolicy() {
        return _policy;
    }
    
    /**
     * Sets the instance-based cache distribution policy. 
     */
    public void setDistributionPolicy(CacheDistributionPolicy policy) {
        _policy = policy;
    }
    
    /**
     * Affirms if the given type is eligible for cache.
     */
    public boolean isCachable(ClassMetaData meta) {
        Boolean isCachable = isCacheableByPlugin(meta);
        if (isCachable == null) {
            isCachable = isCacheableByMode(meta);
            if (isCachable == null) {
                isCachable = isCacheableByType(meta);
            }
        }
        return isCachable;
    }
    
    /**
     * Affirms the given class is eligible to be cached according to the cache mode
     * and the cache enable flag on the given metadata.
     *  
     * @return TRUE or FALSE if  cache mode is configured. null otherwise.
     */
    private Boolean isCacheableByMode(ClassMetaData meta) { 
        String mode = _conf.getDataCacheMode();
        if (DataCacheMode.ALL.toString().equalsIgnoreCase(mode))
            return true;
        if (DataCacheMode.NONE.toString().equalsIgnoreCase(mode))
            return false;
        if (DataCacheMode.ENABLE_SELECTIVE.toString().equalsIgnoreCase(mode))
            return Boolean.TRUE.equals(meta.getCacheEnabled());
        if (DataCacheMode.DISABLE_SELECTIVE.toString().equalsIgnoreCase(mode))
            return !Boolean.FALSE.equals(meta.getCacheEnabled());
        return null;
    }
    
    /**
     * Is the given type cacheable by @DataCache annotation.
     *  
     * @see ClassMetaData#getDataCacheName()
     */
    private Boolean isCacheableByType(ClassMetaData meta) {
        return meta.getDataCacheName() != null;
    }
    
    /**
     * Is the given type cacheable by excludeTypes/includeTypes plug-in properties.
     *  
     * @param meta the given type
     * @return TRUE or FALSE if the type has appeared in the plug-in property.
     * null otherwise.
     */
    private Boolean isCacheableByPlugin(ClassMetaData meta) {
        String className = meta.getDescribedType().getName();
        if (_excludedTypes != null && _excludedTypes.contains(className)) {  
            return Boolean.FALSE;
        } 
        if (_includedTypes != null && _includedTypes.contains(className)) {
            return Boolean.TRUE;
        }
        return null;
    }

    /**
     * Gets the excluded types, if configured.
     */
    public Set<String> getExcludedTypes() {
        return _excludedTypes;
    }
    
    /**
     * Sets excluded types from a semicolon separated list of type names.
     */
    public void setExcludedTypes(String types) {
        _excludedTypes = parseNames(types);
    }

    /**
     * Gets the included types, if configured.
     */
    public Set<String> getIncludedTypes() {
        return _excludedTypes;
    }
    
    /**
     * Sets included types from a semicolon separated list of type names.
     */
    public void setIncludedTypes(String types) {
        _includedTypes = parseNames(types);
    }
    
    private Set<String> parseNames(String types) {
        if (StringUtils.isEmpty(types))
            return Collections.emptySet();
        String[] names = Strings.split(types, ";", 0);
        Set<String> set = new HashSet<String>();
        set.addAll(Arrays.asList(names));
        
        return  Collections.unmodifiableSet(set);
    }

}

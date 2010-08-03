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
package org.apache.openjpa.instrumentation;

import java.util.Date;

import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.datacache.QueryCache;
import org.apache.openjpa.lib.instrumentation.AbstractInstrument;
import org.apache.openjpa.lib.instrumentation.InstrumentationLevel;

/**
 * Provides a basic instrument implementation wrapper for the query cache.  This
 * class can be extended to create a provider specific instrument for the
 * query cache.
 */
public abstract class AbstractQueryCacheInstrument extends AbstractInstrument
    implements QueryCacheInstrument {

    /**
     * Value indicating that cache statistics are not available.
     */
    public static final long NO_STATS = -1;
    
    private QueryCache _qc;
    private String _configId = null;
    private String _configRef = null;
        
    public void setQueryCache(QueryCache qc) {
        _qc = qc;
    }
    
    // TODO : Cache stats must be added to query cache.  They will likely be
    // tracked by a QueryStatistics type when that takes place.
    private CacheStatistics getStatistics() {
        if (_qc == null)
            return null;
        return null; // _qc.getStatistics();
    }

    public long getHitCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getHitCount();
        return NO_STATS;
    }
            
    public long getReadCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getReadCount();
        return NO_STATS;
    }
        
    public long getTotalHitCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalHitCount();
        return NO_STATS;
    }
    
    public long getTotalReadCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalReadCount();
        return NO_STATS;
    }
    
    public long getTotalWriteCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalWriteCount();
        return NO_STATS;
    }
        
    public long getWriteCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getWriteCount();
        return NO_STATS;
    }
    
    public void reset() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            stats.reset();        
    }
    
    public Date sinceDate() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.since();
        return null;
    }
    
    public Date startDate() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.start();        
        return null;
    }
    
    public String getConfigId() {
        return _configId;
    }

    public void setConfigId(String cid) {
        _configId = cid;
    }
    
    public String getContextRef() {
        return _configRef;
    }
    
    public void setContextRef(String cref) {
        _configRef = cref;
    }
    
    public InstrumentationLevel getLevel() {
        return InstrumentationLevel.FACTORY;
    }

}

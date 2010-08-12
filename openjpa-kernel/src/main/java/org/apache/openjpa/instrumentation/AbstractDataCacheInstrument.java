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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.lib.instrumentation.AbstractInstrument;
import org.apache.openjpa.lib.instrumentation.InstrumentationLevel;

/**
 * Provides a basic instrument implementation wrapper for the data cache.  This
 * class can be extended to create a provider specific instrument for the
 * data cache.
 */
public abstract class AbstractDataCacheInstrument extends AbstractInstrument 
    implements DataCacheInstrument {

    /**
     * Value indicating that cache statistics are not available.
     */
    public static final long NO_STATS = -1;

    private DataCache _dc = null;
    private String _configID = null;
    private String _configRef = null;
        
    public void setDataCache(DataCache dc) {
        _dc = dc;
    }
    
    public void setConfigId(String cid) {
        _configID = cid;
    }
    
    public void setContextRef(String cref) {
        _configRef = cref;
    }
        
    public long getHitCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getHitCount();
        return NO_STATS;
    }
    
    public long getHitCount(String className) 
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getHitCount(clazz);
    }
    
    public long getReadCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getReadCount();
        return NO_STATS;
    }
    
    public long getReadCount(String className)
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getReadCount(clazz);        
    }
    
    public long getTotalHitCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalHitCount();
        return NO_STATS;
    }

    public long getTotalHitCount(String className)
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getTotalHitCount(clazz);      
    }
    
    public long getTotalReadCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalReadCount();
        return NO_STATS;
    }

    public long getTotalReadCount(String className)
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getTotalReadCount(clazz);      
    }
    
    public long getTotalWriteCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalWriteCount();
        return NO_STATS;
    }

    public long getTotalWriteCount(String className)
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getTotalWriteCount(clazz);      
    }
    
    public long getWriteCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getWriteCount();
        return NO_STATS;
    }

    public long getWriteCount(String className)
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getWriteCount(clazz);      
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
        return _configID;
    }

    public String getContextRef() {
        return _configRef;
    }

    public String getCacheName() {
        if (_dc != null)
            return _dc.getName();
        return null;
    }
    
    private CacheStatistics getStatistics() {
        if (_dc != null) {
            return _dc.getStatistics();
        }
        return null;
    }

    private long getWriteCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getWriteCount(c);
        return NO_STATS;
    }
    
    private long getTotalWriteCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalWriteCount(c);
        return NO_STATS;
    }
    
    private long getTotalReadCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalReadCount(c);
        return NO_STATS;
    }
    
    private long getTotalHitCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalHitCount(c);
        return NO_STATS;
    }

    private long getReadCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getReadCount(c);
        return NO_STATS;
    }

    private long getHitCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getHitCount(c);
        return NO_STATS;
    }
    
    public long getEvictionCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getEvictionCount();
        return NO_STATS;
    }

    public long getEvictionCount(String className) 
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getEvictionCount(clazz);
    }

    public long getEvictionCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getEvictionCount(c);
        return NO_STATS;        
    }
    
    public long getTotalEvictionCount() {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalEvictionCount();
        return NO_STATS;
    }

    public long getTotalEvictionCount(String className) 
        throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getTotalEvictionCount(clazz);
    }

    public long getTotalEvictionCount(Class<?> c) {
        CacheStatistics stats = getStatistics();
        if (stats != null)
            return stats.getTotalEvictionCount(c);
        return NO_STATS;
    }
    
    @SuppressWarnings("unchecked")
    public Set<String> classNames() {
        CacheStatistics stats = getStatistics();
        if (stats != null) {
            Set<String> classNames = new HashSet<String>();
            Set<Class<?>> clazzNames = stats.classNames();
            for (Class<?> clazz : clazzNames) {
                if (clazz != null) {
                    classNames.add(clazz.getName());
                }
            }
            return classNames;
        }
        return Collections.EMPTY_SET;
    }
    
    public InstrumentationLevel getLevel() {
        return InstrumentationLevel.FACTORY;
    }
}

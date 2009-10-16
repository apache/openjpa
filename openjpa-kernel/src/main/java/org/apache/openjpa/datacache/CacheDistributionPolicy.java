package org.apache.openjpa.datacache;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A policy determines the name of the cache where a given entity state will be cached.
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 *
 */
public interface CacheDistributionPolicy {
    /**
     * Selects the name of the cache where the given managed proxy object state be cached.
     * 
     * @param sm the managed proxy object to be cached
     * @param context the context of invocation. No specific semantics is 
     * attributed currently. Can be null.
     *  
     * @return name of the cache or null if the managed instance need not be cached.
     */
    String selectCache(OpenJPAStateManager sm, Object context);
    
    /**
     * A default implementation that selects the cache by the type of the given
     * managed instance.
     * 
     * @see ClassMetaData#getDataCacheName()
     *
     */
    public static class Default implements CacheDistributionPolicy {
        public String selectCache(OpenJPAStateManager sm, Object context) {
            return sm.getMetaData().getDataCacheName();
        }
    }
}

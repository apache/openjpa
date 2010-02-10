package org.apache.openjpa.datacache;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A default implementation that selects the cache by the type of the given managed instance.
 * The name of the cache is determined by {@link ClassMetaData#getDataCacheName() name as specified} by
 * the metadata. 
 * 
 * @see ClassMetaData#getDataCacheName()
 *
 */
public class DefaultCacheDistributionPolicy implements CacheDistributionPolicy {
    public String selectCache(OpenJPAStateManager sm, Object context) {
        return sm.getMetaData().getDataCacheName();
        
    }

    public void endConfiguration() {
    }

    public void setConfiguration(Configuration conf) {
    }

    public void startConfiguration() {
    }
}

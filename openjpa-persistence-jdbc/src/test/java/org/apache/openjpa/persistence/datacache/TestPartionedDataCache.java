package org.apache.openjpa.persistence.datacache;

import org.apache.openjpa.datacache.CacheDistributionPolicy;
import org.apache.openjpa.datacache.ConcurrentDataCache;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.PartitionedDataCache;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.persistence.StoreCacheImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.UserException;

public class TestPartionedDataCache extends SingleEMFTestCase {
    public void setUp() {
        super.setUp("openjpa.DataCache", "partitioned(PartitionType=concurrent,partitions="+
                "'(name=a,cacheSize=100),(name=b,cacheSize=200)')",
                    "openjpa.RemoteCommitProvider", "sjvm",
        "openjpa.DataCacheManager", 
        "DistributionPolicy=org.apache.openjpa.persistence.datacache.TestPartionedDataCache$TestPolicy");
    }
    
    public void testPropertyParsing() {
        PartitionedDataCache cache = new PartitionedDataCache();
        String badProperty = "(name=a,cacheSize=100),(name=b,cacheSize=200";// missing last bracket
        try {
            cache.setPartitions(badProperty);
            fail("Expected parse error on " + badProperty);
        } catch (UserException e) {
            System.err.println(e);
        }
        badProperty = "(name=a,cacheSize=100)(name=b,cacheSize=200)";// missing comma
        try {
            cache.setPartitions(badProperty);
            fail("Expected parse error on " + badProperty);
        } catch (UserException e) {
            System.err.println(e);
        }
        badProperty = "(cacheSize=100),(name=b,cacheSize=200)";// missing name
        try {
            cache.setPartitions(badProperty);
            fail("Expected parse error on " + badProperty);
        } catch (UserException e) {
            System.err.println(e);
        }
        badProperty = "(name=a,cacheSize=100),(name=a,cacheSize=200)";// duplicate name
        try {
            cache.setPartitions(badProperty);
            fail("Expected parse error on " + badProperty);
        } catch (UserException e) {
            System.err.println(e);
        }
        badProperty = "(name=default,cacheSize=100),(name=a,cacheSize=200)";// default name
        try {
            cache.setPartitions(badProperty);
            fail("Expected parse error on " + badProperty);
        } catch (UserException e) {
            System.err.println(e);
        }
        
    }
    
    public void testPolicy() {
        CacheDistributionPolicy policy = emf.getConfiguration().getDataCacheManagerInstance().getDistributionPolicy();
        assertNotNull(policy);
        assertTrue(policy.getClass() + " not TestPolicy", policy instanceof TestPolicy);
        
    }
    public void testPluginConfiguration() {
        DataCache cache = ((StoreCacheImpl)emf.getStoreCache()).getDelegate();
        assertTrue(cache instanceof PartitionedDataCache);
        assertFalse(cache.getPartitionNames().isEmpty());
        assertNotNull(cache.getPartition("a", false));
        assertNotNull(cache.getPartition("b", false));
        assertNull(cache.getPartition("c", false));
        assertCacheConfiguration("a", 100);
        assertCacheConfiguration("b", 200);
    }
    
    void assertCacheConfiguration(String name, int size) {
        DataCache cache = emf.getConfiguration().getDataCacheManagerInstance().getDataCache(name);
        assertNotNull(cache);
        assertTrue(cache instanceof ConcurrentDataCache);
        assertEquals(size, ((ConcurrentDataCache)cache).getCacheSize());
    }
    
    public static class TestPolicy implements CacheDistributionPolicy {

        public String selectCache(OpenJPAStateManager sm, Object context) {
            return "a";
        }
        
    }
}

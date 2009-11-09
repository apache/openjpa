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
package org.apache.openjpa.persistence.jdbc.query.cache;

import java.util.Collections;

import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.datacache.QueryCache;
import org.apache.openjpa.datacache.TypesChangedEvent;
import org.apache.openjpa.datacache.AbstractQueryCache.EvictPolicy;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;

public class TestQueryTimestampEviction extends AbstractQueryCacheTest {
    public void setUp() throws Exception {
        super.setUp(
                "openjpa.DataCache", "true",
                "openjpa.QueryCache",
                "CacheSize=1000, EvictPolicy='timestamp'",
                "openjpa.RemoteCommitProvider", "sjvm");
    }

    /**
     * Verify that the persistent unit property configuration is enabling
     * the TIMESTAMP Eviction Policy.
     */
    public void testTimestampEvictionEnablement() {
        ConcurrentQueryCache qc = getQueryCache();
        EvictPolicy ep = qc.getEvictPolicy();
        assertTrue(ep == EvictPolicy.TIMESTAMP);
    }
    
    public void testLoadQueries() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
        	return;
        }                                 
        loadQueryCache();
        int cacheSizeBeforeUpdate = queryCacheGet();
        updateAnEntity();
        int cacheSizeAfterUpdate = queryCacheGet();

        // If evictPolicy is timestamp the querycache size should be equal to
        // cacheSizeBeforeUpdate value.
        assertEquals(cacheSizeBeforeUpdate, cacheSizeAfterUpdate);

        this.recreateData = false;
    }

    /**
     * This testcase was added for OPENJPA-1379. Prior to this fix, the main thread holds a lock on
     * the QueryCache which it never released. As a result, thread t2 will never successfully obtain
     * the writeLock().
     * 
     * The main thread holds the writeLock because setUp(..) calls deleteAllData() which eventually
     * results in AbstractQueryCache.onTypesChanges(TypesChangedEvent) being called.
     * 
     * @throws Exception
     */
    public void testWriteLock() throws Exception {
        final QueryCache qc = emf.getConfiguration().getDataCacheManagerInstance().getSystemQueryCache();
        Thread t2 = new Thread() {
            public void run() {
                qc.writeLock();
                qc.writeUnlock();
            }
        };
        t2.start();
        t2.join(5000);
        
        if (t2.getState().equals(java.lang.Thread.State.WAITING)) {
            fail("The thread is still waiting on a writeLock()!");
        }
    }
}


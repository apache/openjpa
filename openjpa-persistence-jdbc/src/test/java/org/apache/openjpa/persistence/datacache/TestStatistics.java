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
package org.apache.openjpa.persistence.datacache;

import javax.persistence.EntityManager;

import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.persistence.StoreCache;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests statistics of data cache operation.
 *  
 * @author Pinaki Poddar
 *
 */
public class TestStatistics extends SingleEMFTestCase {
    private static final boolean L2Cached = true;
    private static final boolean L1Cached = true;
    private static CachedPerson person;
    private EntityManager em;
    private StoreCache cache;
    CacheStatistics stats;
    public void setUp() {
        
        super.setUp(CLEAR_TABLES, CachedPerson.class,
                "openjpa.DataCache", "true",
                "openjpa.QueryCache", "true",
                "openjpa.RemoteCommitProvider", "sjvm");
        cache = emf.getStoreCache();
        assertNotNull(cache);
        stats = cache.getStatistics();
        assertNotNull(stats);
        em = emf.createEntityManager();
        
        if (person == null) {
            person = createData();
        }
        stats.reset();
        em.clear();
    }
    
    public CachedPerson createData() {
        em.getTransaction().begin();
        CachedPerson p = new CachedPerson();
        p.setId((int)System.currentTimeMillis());
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }
    
    /**
     * Finding an entity from a clean should hit the L2 cache.
     */
    public void testFind() {
        Object pid = person.getId();
        int N = 0;
        for (int i = 0; i < N; i++) {
            assertCached(person, pid, !L1Cached, L2Cached);
            long[] before = snapshot();
            CachedPerson p = em.find(CachedPerson.class, pid);
            long[] after = snapshot();
            assertDelta(before, after, 1, 1, 0); //READ:1 HIT:1, WRITE:0
            assertCached(p, pid, L1Cached, L2Cached);
        }
    }
    
    /**
     * Get {hit,read,write} count for the cache across all instances.
     */
    long[] snapshot() {
        return new long[]{stats.getReadCount(), stats.getHitCount(), stats.getWriteCount()};
    }
    
    /**
     * Get {hit,read,write} count for the cache across given class extent.
     */
    long[] snapshot(Class<?> cls) {
        return new long[]{stats.getReadCount(cls), stats.getHitCount(cls), stats.getWriteCount(cls)};
    }
    
    void assertDelta(long[] before, long[] after, long readDelta, long hitDelta, long writeDelta) {
        assertEquals("READ count mismatch",  readDelta,  after[0] - before[0]);
        assertEquals("HIT count mismatch",   hitDelta,   after[1] - before[1]);
        assertEquals("WRITE count mismatch", writeDelta, after[2] - before[2]);
    }
    
    
    void assertCached(Object o, Object oid, boolean l1, boolean l2) {
        boolean l1a = em.contains(o);
        boolean l2a = cache.contains(o.getClass(), oid);
        if (l1 != l1a) {
            fail("Expected " + (l1 ? "":"not") + " to find instance " + 
                    o.getClass().getSimpleName()+":"+oid + " in L1 cache");
        }
        if (l2 != l2a) {
            fail("Expected " + (l2 ? "":"not") + " to find instance " + 
                    o.getClass().getSimpleName()+":"+oid + " in L2 cache");
        }
    }
    
    void print(String msg, CacheStatistics stats) {
        System.err.println(msg + stats + " H:" + stats.getHitCount() + " R:" + stats.getReadCount() + " W:" + 
                stats.getWriteCount());
    }
}

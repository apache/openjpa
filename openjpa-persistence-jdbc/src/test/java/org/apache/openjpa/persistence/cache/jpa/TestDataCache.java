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

package org.apache.openjpa.persistence.cache.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.datacache.ConcurrentDataCache;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCachePCDataImpl;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.StoreCache;
import org.apache.openjpa.persistence.StoreCacheImpl;
import org.apache.openjpa.persistence.cache.jpa.model.EmbeddableData;
import org.apache.openjpa.persistence.cache.jpa.model.LeftHand;
import org.apache.openjpa.persistence.cache.jpa.model.RightHand;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.CacheMap;

public class TestDataCache extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(CLEAR_TABLES,
                "openjpa.DataCache","true",
                LeftHand.class,
                RightHand.class,
                EmbeddableData.class);
        emf.createEntityManager();
    }
    
    public void testNoRawEmbeddableStoreInDatacache() {
        final EntityManager em = emf.createEntityManager();
        final OpenJPAEntityManager oem = em.unwrap(OpenJPAEntityManager.class);
        final OpenJPAEntityManagerFactory oemf = oem.getEntityManagerFactory();
        final StoreCacheImpl sc = (StoreCacheImpl) oemf.getStoreCache();
        final DataCache dc = sc.getDelegate();
        final ConcurrentDataCache cdc = (ConcurrentDataCache) dc;
        
        
        final LeftHand lh = new LeftHand();
        lh.setId(System.currentTimeMillis());
        lh.setStrData("left hand");
        
        final RightHand rh = new RightHand();
        rh.setId(System.currentTimeMillis());
        rh.setStrData("right hand");
        
        final EmbeddableData ed = new EmbeddableData();
        ed.setEmbeddedString("Embedded String");
        ed.setLazyEmbeddedString("Lazy String");
        rh.setEmb(ed);
        
        final ArrayList<RightHand> rhList = new ArrayList<RightHand>();
        rhList.add(rh);
        lh.setRhList(rhList);
        
        em.getTransaction().begin();
        em.persist(lh);
        em.persist(rh);
        em.getTransaction().commit();
        
        CacheMap cm = cdc.getCacheMap();
        validateCache(cm);
        
        em.clear();
        oemf.getCache().evictAll();
        assertEquals(0, cm.size());
        
        Query q = em.createQuery("SELECT lh FROM LeftHand lh");
        List resultList = q.getResultList();
        for (Object o : resultList) {
            System.out.println(o);
        }
        
        validateCache(cm);
    }
    
    private void validateCache(CacheMap cm) {
        assertNotNull(cm);
        
        final Collection afterCommitCacheEntries = cm.values();
        assertNotNull(afterCommitCacheEntries);       
        
        // Expecting 1 entry for LeftHand, and 1 entry for righthand
        boolean foundLeft = false;
        boolean foundRight = false;
        Iterator<?> i1 = afterCommitCacheEntries.iterator();
        while (i1.hasNext()) {
            DataCachePCDataImpl o = (DataCachePCDataImpl) i1.next();
            assertNotNull(o);
            Class<?> type = o.getType();
            if (LeftHand.class.equals(type)) {
                foundLeft = true;
            } else if (RightHand.class.equals(type)) {
                foundRight = true;
            } else if (EmbeddableData.class.equals(type)) {
                // Found raw Embeddable in the datacache
                fail();
            }  
        }
        
        assertTrue(foundLeft);
        assertTrue(foundRight);
        
        assertEquals(2, cm.size());
    }
}

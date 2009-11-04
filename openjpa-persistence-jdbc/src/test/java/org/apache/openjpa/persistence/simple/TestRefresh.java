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
package org.apache.openjpa.persistence.simple;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;

import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.persistence.JPAProperties;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestRefresh extends SingleEMTestCase {

    public void setUp() {
        super.setUp(CLEAR_TABLES, Item.class, 
             "openjpa.AutoDetach", "commit",
             "openjpa.DataCache", "true",
             "openjpa.RemoteCommitProvider", "sjvm");
    }

    public void testFlushRefreshNewInstance() {
        em.getTransaction().begin();
        Item item = new Item();
        item.setItemData("Test Data");
        em.persist(item);
        em.flush();
        em.refresh(item);
        em.getTransaction().commit();
        assertEquals("Test Data", item.getItemData());
    }
    
    /**
     * Refresh always bypass L2 cache.
     * According to JPA 2.0 Spec Section 3.7.2:
     * "The retrieveMode property is ignored for the refresh method,
     *  which always causes data to be retrieved from the database, not the cache."
     */
    public void testRefreshBypassL2Cache() {
        String original = "Original L2 Cached Data";
        String sneakUpdate = "Sneak Update";
        em.getTransaction().begin();
        Item item = new Item();
        item.setItemData(original);
        em.persist(item);
        em.getTransaction().commit();
        assertCached(Item.class, item.getItemId());
        
        // Sneakily update with SQL
        String sql = "UPDATE I_ITEM SET I_DATA=?1 WHERE I_ID=?2";
        em.getTransaction().begin();
        int updateCount = em.createNativeQuery(sql)
            .setParameter(1, sneakUpdate)
            .setParameter(2, item.getItemId())
            .executeUpdate();
        assertEquals(1, updateCount);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        // Find will find the L2 cached data
        item = em.find(Item.class, item.getItemId());
        assertEquals(original, item.getItemData());
        // But refresh will get the actual database record
        em.refresh(item);
        assertEquals(sneakUpdate, item.getItemData());

        // Even if cache retrieve mode is set to USE
        em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
        em.refresh(item);
        assertEquals(sneakUpdate, item.getItemData());
        em.getTransaction().rollback();
    }
    
    public void testCacheRetrieveModeSetting() {
        OpenJPAEntityManager em = emf.createEntityManager();
        em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
        Map<String, Object> properties = em.getProperties();
        if (!properties.containsKey(JPAProperties.CACHE_RETRIEVE_MODE)) {
            System.err.println(properties);
            fail("Expected " + JPAProperties.CACHE_RETRIEVE_MODE + " properties be returned");
        }
        Object mode = properties.get(JPAProperties.CACHE_RETRIEVE_MODE);
        assertEquals(mode, CacheRetrieveMode.USE);
    }
    
    public void testCacheStoreModeSetting() {
        OpenJPAEntityManager em = emf.createEntityManager();
        em.setProperty(JPAProperties.CACHE_STORE_MODE, CacheStoreMode.USE);
        Map<String, Object> properties = em.getProperties();
        if (!properties.containsKey(JPAProperties.CACHE_STORE_MODE)) {
            System.err.println(properties);
            fail("Expected " + JPAProperties.CACHE_STORE_MODE + " properties be returned");
        }
        Object mode = properties.get(JPAProperties.CACHE_STORE_MODE);
        assertEquals(mode, CacheStoreMode.USE);
    }
    
    
    void assertCached(Class<?> cls, Object oid) {
        assertTrue(cls + ":" + oid + " should be in L2 cache, but not", emf.getCache().contains(cls, oid));
    }
    
    void assertNotCached(Class<?> cls, Object oid) {
        assertTrue(cls + ":" + oid + " should not be in L2 cache, but is", !emf.getCache().contains(cls, oid));
    }
    
}

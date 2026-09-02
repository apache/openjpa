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
 * Unless required by applicable law or agEmployee_Last_Name to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.property;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;

import org.apache.openjpa.kernel.DataCacheRetrieveMode;
import org.apache.openjpa.kernel.DataCacheStoreMode;
import org.apache.openjpa.persistence.FetchPlan;
import org.apache.openjpa.persistence.JPAProperties;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * <b>TestEMProperties</b> is used to test various persistence properties set through EntityManager.setProperty() API
 * to ensure no errors are thrown.
 */
public class TestEMProperties extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(EntityContact.class,
              EmbeddableAddress.class,
              DROP_TABLES, "jakarta.persistence.query.timeout", 23456);
    }

    public void testQueryTimeoutPropertyDefault() {
        EntityManager em = emf.createEntityManager();

        String sql = "select * from EntityContact";
        OpenJPAQuery<?> query = OpenJPAPersistence.cast(em.createNativeQuery(sql));
        assertEquals(23456, query.getFetchPlan().getQueryTimeout());

        em.clear();
        em.close();
    }

    public void testQueryTimeoutPropertyOnEntityManagerCreation() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("jakarta.persistence.query.timeout", "12345");
        // Setting a value of type String should convert if possible and not return an error
        EntityManager em = emf.createEntityManager(properties);

        String sql = "select * from EntityContact";
        OpenJPAQuery<?> query = OpenJPAPersistence.cast(em.createNativeQuery(sql));
        assertEquals(12345, query.getFetchPlan().getQueryTimeout());

        em.clear();
        em.close();
    }

    public void testQueryTimeoutPropertySetOnEntityManager() {
        EntityManager em = emf.createEntityManager();

        // Setting a value of type String should convert if possible and not return an error
        em.setProperty("jakarta.persistence.query.timeout", "12345");

        String sql = "select * from EntityContact";
        OpenJPAQuery<?> query = OpenJPAPersistence.cast(em.createNativeQuery(sql));
        assertEquals(12345, query.getFetchPlan().getQueryTimeout());

        em.clear();
        em.close();
    }

    public void testCacheModeStringSetOnEntityManager() {
        EntityManager em = emf.createEntityManager();

        em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, "BYPASS");
        em.setProperty(JPAProperties.CACHE_STORE_MODE, "REFRESH");

        assertEquals(CacheRetrieveMode.BYPASS, em.getCacheRetrieveMode());
        assertEquals(CacheStoreMode.REFRESH, em.getCacheStoreMode());
        FetchPlan fetchPlan = OpenJPAPersistence.cast(em).getFetchPlan();
        assertEquals(DataCacheRetrieveMode.BYPASS, fetchPlan.getCacheRetrieveMode());
        assertEquals(DataCacheStoreMode.REFRESH, fetchPlan.getCacheStoreMode());

        em.close();
    }

    public void testCacheModeStringOnEntityManagerCreation() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JPAProperties.CACHE_RETRIEVE_MODE, "BYPASS");
        properties.put(JPAProperties.CACHE_STORE_MODE, "REFRESH");
        EntityManager em = emf.createEntityManager(properties);

        assertEquals(CacheRetrieveMode.BYPASS, em.getCacheRetrieveMode());
        assertEquals(CacheStoreMode.REFRESH, em.getCacheStoreMode());
        FetchPlan fetchPlan = OpenJPAPersistence.cast(em).getFetchPlan();
        assertEquals(DataCacheRetrieveMode.BYPASS, fetchPlan.getCacheRetrieveMode());
        assertEquals(DataCacheStoreMode.REFRESH, fetchPlan.getCacheStoreMode());

        em.close();
    }

    public void testCacheModeStringIsCaseInsensitive() {
        EntityManager em = emf.createEntityManager();

        em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, "use");
        em.setProperty(JPAProperties.CACHE_STORE_MODE, " refresh ");

        assertEquals(CacheRetrieveMode.USE, em.getCacheRetrieveMode());
        assertEquals(CacheStoreMode.REFRESH, em.getCacheStoreMode());

        em.close();
    }

    public void testCacheModeEnumStillWorks() {
        EntityManager em = emf.createEntityManager();

        em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS);
        em.setProperty(JPAProperties.CACHE_STORE_MODE, CacheStoreMode.REFRESH);

        assertEquals(CacheRetrieveMode.BYPASS, em.getCacheRetrieveMode());
        assertEquals(CacheStoreMode.REFRESH, em.getCacheStoreMode());
        em.close();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JPAProperties.CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS);
        properties.put(JPAProperties.CACHE_STORE_MODE, CacheStoreMode.REFRESH);
        em = emf.createEntityManager(properties);

        assertEquals(CacheRetrieveMode.BYPASS, em.getCacheRetrieveMode());
        assertEquals(CacheStoreMode.REFRESH, em.getCacheStoreMode());

        em.close();
    }

    public void testInvalidCacheModeStringFails() {
        EntityManager em = emf.createEntityManager();
        try {
            em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, "NOPE");
            fail("Expected an IllegalArgumentException for an invalid cache retrieve mode");
        } catch (IllegalArgumentException iae) {
            String message = iae.getMessage();
            assertTrue(message, message.contains("cache.retrieveMode"));
            assertTrue(message, message.contains("USE"));
            assertTrue(message, message.contains("BYPASS"));
        } finally {
            em.close();
        }
    }

    public void testNullCacheModeResetsToDefault() {
        EntityManager em = emf.createEntityManager();

        em.setProperty(JPAProperties.CACHE_STORE_MODE, null);
        em.setProperty(JPAProperties.CACHE_RETRIEVE_MODE, "null");

        FetchPlan fetchPlan = OpenJPAPersistence.cast(em).getFetchPlan();
        assertEquals(DataCacheRetrieveMode.USE, fetchPlan.getCacheRetrieveMode());
        assertEquals(DataCacheStoreMode.USE, fetchPlan.getCacheStoreMode());

        em.close();
    }
}

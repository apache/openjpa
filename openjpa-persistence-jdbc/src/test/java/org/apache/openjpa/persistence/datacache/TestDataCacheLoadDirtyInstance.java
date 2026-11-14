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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loading a field of an instance with unflushed changes must not put those
 * changes in the data cache, since they are lost when the transaction rolls
 * back.
 */
class TestDataCacheLoadDirtyInstance extends SQLListenerTestCase {

    private static final long ID = 1L;

    @BeforeEach
    public void setUp() {
        // RefreshFromDataCache lets a refresh go through the data cache, so
        // that its loaded state is cached
        super.setUp(CLEAR_TABLES, CachedLazyEntity.class, "openjpa.DataCache", "true",
            "openjpa.RefreshFromDataCache", "true");

        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CachedLazyEntity e = new CachedLazyEntity();
            e.setId(ID);
            e.setName("orig");
            e.setNote("note");
            e.getTags().add("one");
            em.persist(e);
            em.getTransaction().commit();
        }
        emf.getCache().evictAll();

        // cache the instance without its lazy fields
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.find(CachedLazyEntity.class, ID);
        }
        assertTrue(emf.getCache().contains(CachedLazyEntity.class, ID));
    }

    @Test
    void loadFieldDoesNotCacheDirtyBasicField() {
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            e.setName("dirty");
            assertEquals("note", e.getNote());
            em.getTransaction().rollback();
        }

        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            sql.clear();
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            assertEquals("orig", e.getName());
            // the clean field loaded alongside the dirty one is still cached
            assertEquals("note", e.getNote());
            assertEquals(0, sql.size());
        }
    }

    @Test
    void loadFieldDoesNotCacheDirtyCollection() {
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            e.getTags().add("bad");
            assertEquals("note", e.getNote());
            em.getTransaction().rollback();
        }

        assertCommittedState();
    }

    @Test
    void findAllDoesNotCacheDirtyField() {
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            emf.getCache().evict(CachedLazyEntity.class, ID);
            e.setName("dirty");
            // the instance is not in the cache, so it is loaded from the
            // database and cached again
            em.findAll(CachedLazyEntity.class, ID);
            assertTrue(emf.getCache().contains(CachedLazyEntity.class, ID));
            em.getTransaction().rollback();
        }

        assertCommittedState();
    }

    @Test
    void loadFieldOfFlushedInstanceDoesNotCacheIt() {
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            emf.getCache().evict(CachedLazyEntity.class, ID);
            e.setName("dirty");
            em.flush();
            assertEquals("note", e.getNote());
            assertFalse(emf.getCache().contains(CachedLazyEntity.class, ID));
            em.getTransaction().rollback();
        }

        assertCommittedState();
    }

    @Test
    void refreshOfFlushedInstanceDoesNotCacheIt() {
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CachedLazyEntity other = new CachedLazyEntity();
            other.setId(ID + 1);
            other.setName("other");
            em.persist(other);
            em.getTransaction().commit();

            em.getTransaction().begin();
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            emf.getCache().evict(CachedLazyEntity.class, ID);
            e.setName("dirty");
            em.flush();
            // refreshing more than one instance loads them through loadAll,
            // which reads the flushed state from the database
            em.refreshAll(e, other);
            assertEquals("dirty", e.getName());
            assertFalse(emf.getCache().contains(CachedLazyEntity.class, ID));
            em.getTransaction().rollback();
        }

        assertCommittedState();
    }

    private void assertCommittedState() {
        try (OpenJPAEntityManager em = emf.createEntityManager()) {
            CachedLazyEntity e = em.find(CachedLazyEntity.class, ID);
            assertEquals("orig", e.getName());
            assertEquals(1, e.getTags().size());
            assertEquals("one", e.getTags().get(0));
            assertEquals("note", e.getNote());
        }
    }
}

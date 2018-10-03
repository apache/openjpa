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

import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.EntityManagerImpl;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestDataCacheStoreLazyFk extends SQLListenerTestCase {
    Object[] p = new Object[] { CLEAR_TABLES, CachedEntityStatistics.class, "openjpa.DataCache", "true" };

    @Override
    public void setUp() {
        super.setUp(p);
    }

    public void testCacheHit() throws Exception {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        // Change the eager field to lazy to make testing easier.
        ClassMetaData cmd =
            em.getConfiguration().getMetaDataRepositoryInstance().getMetaData(CachedEntityStatistics.class, null, true);
        cmd.getField("eagerList").setInDefaultFetchGroup(false);
        try {
            em.getTransaction().begin();
            CachedEntityStatistics e = new CachedEntityStatistics();
            CachedEntityStatistics lazy = new CachedEntityStatistics();
            Set<CachedEntityStatistics> lazyList = new HashSet<>();
            lazyList.add(lazy);
            e.setLazyList(lazyList);
            em.persist(e);
            em.persist(lazy);
            em.flush();
            em.clear();

            // Should prime the cache
            em.find(CachedEntityStatistics.class, e.getId()).getLazyList();
            em.clear();
            sql.clear();

            CachedEntityStatistics c = em.find(CachedEntityStatistics.class, e.getId());
            c.getLazyList();
            assertEquals(0, sql.size());

            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
}

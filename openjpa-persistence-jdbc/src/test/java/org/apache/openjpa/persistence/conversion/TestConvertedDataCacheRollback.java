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
package org.apache.openjpa.persistence.conversion;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * An element of a collection converted as a whole that is changed in place
 * does not mark the field dirty until the instance is checked at flush. A
 * load of another field before that must not put the changed value into the
 * data cache, or it outlives a rollback.
 */
public class TestConvertedDataCacheRollback extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(ConvertedListEntity.class, DROP_TABLES, "openjpa.DataCache", "true");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedListEntity e = new ConvertedListEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getLazyItems().add(new Item("lazy"));
            em.persist(e);
            em.getTransaction().commit();
        }
        // cache the instance without its lazy field
        emf.getCache().evictAll();
        try (EntityManager em = emf.createEntityManager()) {
            em.find(ConvertedListEntity.class, 1L);
        }
        assertTrue(emf.getCache().contains(ConvertedListEntity.class, 1L));
    }

    public void testElementChangedInPlaceIsNotCachedByALoad() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedListEntity e = em.find(ConvertedListEntity.class, 1L);
            e.getItems().get(0).setName("changed");
            // loads the lazy field from the database and caches the instance
            assertEquals(1, e.getLazyItems().size());
            em.getTransaction().rollback();
        }

        try (EntityManager em = emf.createEntityManager()) {
            ConvertedListEntity e = em.find(ConvertedListEntity.class, 1L);
            assertEquals("one", e.getItems().get(0).getName());
        }
    }
}

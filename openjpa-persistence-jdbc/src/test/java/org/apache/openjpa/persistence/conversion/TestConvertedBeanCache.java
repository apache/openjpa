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

import static org.apache.openjpa.persistence.conversion.TestConvertedBean.assertSettings;
import static org.apache.openjpa.persistence.conversion.TestConvertedBean.newSettings;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * The data cache keeps the value of a converted attribute apart from the
 * values held by the instances it loads.
 */
public class TestConvertedBeanCache extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(ConvertedBeanEntity.class, SettingsHolder.class, CLEAR_TABLES,
            "openjpa.DataCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity e = new ConvertedBeanEntity();
            e.setId(1L);
            e.setSettings(newSettings("crit", "user1"));
            e.setPoint(new Point(1, 2));
            em.persist(e);
            em.getTransaction().commit();
        }
    }

    public void testGetterOnlyStateSurvivesLoadFromCache() {
        try (EntityManager em = emf.createEntityManager()) {
            em.find(ConvertedBeanEntity.class, 1L);
        }
        assertTrue(emf.getCache().contains(ConvertedBeanEntity.class, 1L));
        try (EntityManager em = emf.createEntityManager()) {
            assertSettings(em.find(ConvertedBeanEntity.class, 1L).getSettings(), "crit", "user1");
        }
    }

    /**
     * A change made in place and rolled back is not seen through the cache,
     * and each persistence context gets its own value.
     */
    public void testValueChangedInPlaceIsNotShared() {
        try (EntityManager em = emf.createEntityManager()) {
            em.find(ConvertedBeanEntity.class, 1L);
        }
        try (EntityManager em1 = emf.createEntityManager();
             EntityManager em2 = emf.createEntityManager()) {
            ConvertedBeanEntity e1 = em1.find(ConvertedBeanEntity.class, 1L);
            ConvertedBeanEntity e2 = em2.find(ConvertedBeanEntity.class, 1L);
            assertNotSame(e1.getSettings(), e2.getSettings());

            em1.getTransaction().begin();
            ((StaticSettings) e1.getSettings()).getUsers().put("user3", "pw");
            e1.getPoint().setX(5);
            em1.flush();
            em1.getTransaction().rollback();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            assertSettings(e.getSettings(), "crit", "user1");
            assertEquals(1, e.getPoint().getX());
        }
    }

    public void testValueChangedInPlaceIsCached() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            ((StaticSettings) e.getSettings()).getUsers().put("user3", "pw");
            em.getTransaction().commit();
        }
        assertTrue(emf.getCache().contains(ConvertedBeanEntity.class, 1L));
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(2,
                ((StaticSettings) em.find(ConvertedBeanEntity.class, 1L).getSettings()).getUsers().size());
        }
    }
}

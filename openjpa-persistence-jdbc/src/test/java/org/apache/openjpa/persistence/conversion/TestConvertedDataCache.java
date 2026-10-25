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

import java.nio.charset.StandardCharsets;
import java.util.Date;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.kernel.PCData;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.StoreCacheImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * The data cache must not hand the same converted attribute value to more
 * than one persistence context, so a change made in place in one context
 * does not reach the others.
 */
public class TestConvertedDataCache extends SingleEMFTestCase {

    private static final long TIME = 1000000000000L;

    @Override
    public void setUp() {
        setUp(ConvertedListEntity.class, ConvertedCacheEntity.class,
            DROP_TABLES,
            "openjpa.DataCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedListEntity e = new ConvertedListEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            e.getLazyItems().add(new Item("lazy"));
            e.setItem(new Item("single"));
            em.persist(e);

            ConvertedCacheEntity c = new ConvertedCacheEntity();
            c.setId(1L);
            c.getElements().add(new Item("first"));
            c.getElements().add(new Item("second"));
            c.setLabel(new Label("label"));
            c.setBytes(new Buffer(
                "bytes".getBytes(StandardCharsets.UTF_8), null));
            c.setDate(new Buffer(null, new Date(TIME)));
            em.persist(c);
            em.getTransaction().commit();
        }
        assertTrue(emf.getCache().contains(ConvertedListEntity.class, 1L));
        assertTrue(emf.getCache().contains(ConvertedCacheEntity.class, 1L));
    }

    @Override
    public void tearDown() throws Exception {
        LabelConverter.failToDatabase = false;
        LabelConverter.failToEntity = false;
        super.tearDown();
    }

    public void testAnElementChangedInPlaceDoesNotReachOtherContexts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedListEntity.class, 1L).getItems().get(0)
                .setName("changed");
            em.getTransaction().rollback();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedListEntity found = em.find(ConvertedListEntity.class, 1L);
            assertEquals(2, found.getItems().size());
            assertEquals("one", found.getItems().get(0).getName());
            assertEquals("two", found.getItems().get(1).getName());
        }
    }

    public void testAnElementConvertedOnItsOwnDoesNotReachOtherContexts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedCacheEntity.class, 1L).getElements().get(0)
                .setName("changed");
            em.getTransaction().rollback();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedCacheEntity found =
                em.find(ConvertedCacheEntity.class, 1L);
            assertEquals(2, found.getElements().size());
            assertEquals("first", found.getElements().get(0).getName());
            assertEquals("second", found.getElements().get(1).getName());
        }
    }

    public void testASingleValueChangedInPlaceDoesNotReachOtherContexts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedCacheEntity.class, 1L).getLabel()
                .setName("changed");
            em.getTransaction().rollback();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("label",
                em.find(ConvertedCacheEntity.class, 1L).getLabel().getName());
        }
    }

    public void testADatabaseArrayChangedInPlaceDoesNotReachOtherContexts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedCacheEntity.class, 1L).getBytes().getBytes()[0] =
                'X';
            em.getTransaction().rollback();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("bytes", new String(
                em.find(ConvertedCacheEntity.class, 1L).getBytes().getBytes(),
                StandardCharsets.UTF_8));
        }
    }

    public void testADatabaseDateChangedInPlaceDoesNotReachOtherContexts() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedCacheEntity.class, 1L).getDate().getDate()
                .setTime(0L);
            em.getTransaction().rollback();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(TIME, em.find(ConvertedCacheEntity.class, 1L)
                .getDate().getDate().getTime());
        }
    }

    public void testEachContextGetsItsOwnValues() {
        try (EntityManager em1 = emf.createEntityManager();
             EntityManager em2 = emf.createEntityManager()) {
            ConvertedListEntity e1 = em1.find(ConvertedListEntity.class, 1L);
            ConvertedListEntity e2 = em2.find(ConvertedListEntity.class, 1L);
            assertNotSame(e1.getItems().get(0), e2.getItems().get(0));
            assertNotSame(e1.getLazyItems().get(0), e2.getLazyItems().get(0));
            assertNotSame(e1.getItem(), e2.getItem());
            assertEquals("lazy", e2.getLazyItems().get(0).getName());
            assertEquals("single", e2.getItem().getName());

            ConvertedCacheEntity c1 = em1.find(ConvertedCacheEntity.class, 1L);
            ConvertedCacheEntity c2 = em2.find(ConvertedCacheEntity.class, 1L);
            assertNotSame(c1.getElements().get(0), c2.getElements().get(0));
            assertNotSame(c1.getLabel(), c2.getLabel());
            assertNotSame(c1.getBytes().getBytes(), c2.getBytes().getBytes());
            assertNotSame(c1.getDate().getDate(), c2.getDate().getDate());
        }
    }

    public void testACommittedChangeIsLoadedFromTheCache() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedListEntity.class, 1L).setItem(new Item("new"));
            em.getTransaction().commit();
        }
        assertTrue(emf.getCache().contains(ConvertedListEntity.class, 1L));
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("new",
                em.find(ConvertedListEntity.class, 1L).getItem().getName());
        }
    }

    public void testANullValueIsLoadedFromTheCache() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(ConvertedListEntity.class, 1L).setItem(null);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedListEntity found = em.find(ConvertedListEntity.class, 1L);
            assertNull(found.getItem());
            assertEquals(2, found.getItems().size());
        }
    }

    public void testAFieldIsLeftOutOfTheCacheWhenTheConverterFails() {
        emf.getCache().evictAll();
        LabelConverter.failToDatabase = true;
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("label",
                em.find(ConvertedCacheEntity.class, 1L).getLabel().getName());
        }
        LabelConverter.failToDatabase = false;

        PCData data = getCachedData(ConvertedCacheEntity.class, 1L);
        assertNotNull(data);
        assertFalse(data.isLoaded(getFieldIndex("label")));
        assertTrue(data.isLoaded(getFieldIndex("elements")));
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("label",
                em.find(ConvertedCacheEntity.class, 1L).getLabel().getName());
        }
    }

    public void testAConverterThatFailsOnACacheHitFailsTheFind() {
        assertTrue(getCachedData(ConvertedCacheEntity.class, 1L)
            .isLoaded(getFieldIndex("label")));
        LabelConverter.failToEntity = true;
        try (EntityManager em = emf.createEntityManager()) {
            em.find(ConvertedCacheEntity.class, 1L).getLabel();
            fail("expected the converter failure");
        } catch (PersistenceException pe) {
            assertTrue(pe.getMessage(),
                pe.getMessage().contains("convertToEntityAttribute"));
        }
    }

    private PCData getCachedData(Class<?> cls, Object id) {
        Object oid = JPAFacadeHelper.toOpenJPAObjectId(
            JPAFacadeHelper.getMetaData(emf, cls), id);
        DataCache cache = ((StoreCacheImpl) OpenJPAPersistence.cast(emf)
            .getStoreCache()).getDelegate();
        return cache.get(oid);
    }

    private int getFieldIndex(String name) {
        ClassMetaData meta = JPAFacadeHelper.getMetaData(emf,
            ConvertedCacheEntity.class);
        return meta.getField(name).getIndex();
    }
}

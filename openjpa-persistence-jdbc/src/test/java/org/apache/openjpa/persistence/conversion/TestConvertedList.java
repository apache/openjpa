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

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.strats.ConverterValueHandler;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A converter whose attribute type is the collection itself maps the whole
 * collection to a single column. Before OPENJPA-3017 the attribute was left
 * out of the mapping altogether and its contents were lost without an error.
 */
public class TestConvertedList extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(ConvertedListEntity.class, DROP_TABLES);
    }

    public void testTheAttributeIsMappedToOneColumn() {
        ClassMapping cm = ((JDBCConfiguration) emf.getConfiguration())
            .getMappingRepositoryInstance()
            .getMapping(ConvertedListEntity.class, null, true);
        FieldMapping fm = (FieldMapping) cm.getField("items");
        assertNotNull("the converted attribute must be mapped", fm);
        assertEquals("the whole collection is one column value",
            1, fm.getColumns().length);
        assertTrue("the converter must map the value as a whole, got "
            + fm.getHandler(), fm.getHandler() instanceof ConverterValueHandler);
    }

    public void testRoundTrip() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedListEntity e = new ConvertedListEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedListEntity found = em.find(ConvertedListEntity.class, 1L);
            assertEquals(2, found.getItems().size());
            assertEquals("one", found.getItems().get(0).getName());
            assertEquals("two", found.getItems().get(1).getName());
        }
    }

    public void testTheListIsLoadedEagerly() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedListEntity e = new ConvertedListEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            em.persist(e);
            em.getTransaction().commit();
        }
        ConvertedListEntity detached;
        try (EntityManager em = emf.createEntityManager()) {
            detached = em.find(ConvertedListEntity.class, 1L);
        }
        List<Item> items = detached.getItems();
        assertNotNull("a converted attribute is basic and so eager", items);
        assertEquals(2, items.size());
        assertEquals("one", items.get(0).getName());
        assertEquals("two", items.get(1).getName());
    }

    public void testAnExplicitLazyFetchIsHonoured() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedListEntity e = new ConvertedListEntity();
            e.setId(1L);
            e.getLazyItems().add(new Item("one"));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedListEntity found = em.find(ConvertedListEntity.class, 1L);
            assertTrue(emf.getPersistenceUnitUtil().isLoaded(found, "items"));
            assertFalse("fetch = LAZY must keep the attribute out of the fetch",
                emf.getPersistenceUnitUtil().isLoaded(found, "lazyItems"));
            assertEquals(1, found.getLazyItems().size());
            assertEquals("one", found.getLazyItems().get(0).getName());
        }
    }
}

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
 * A collection converted as a whole whose column comes from a mapping file.
 * The column belongs to the attribute, not to its elements; before
 * OPENJPA-3017 it was put on the element mapping, and mapping the field
 * failed with "this mapping cannot have columns in this context".
 */
public class TestConvertedListXml extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(XmlConvertedListEntity.class, DROP_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "converted-list-xml";
    }

    public void testTheMappingFileColumnIsTheAttributeColumn() {
        ClassMapping cm = ((JDBCConfiguration) emf.getConfiguration())
            .getMappingRepositoryInstance()
            .getMapping(XmlConvertedListEntity.class, null, true);
        FieldMapping fm = (FieldMapping) cm.getField("items");
        assertNotNull("the converted attribute must be mapped", fm);
        assertEquals(1, fm.getColumns().length);
        assertEquals("ITEMS_TEXT", fm.getColumns()[0].getIdentifier().getName());
        assertTrue("the converter must map the value as a whole, got "
            + fm.getHandler(), fm.getHandler() instanceof ConverterValueHandler);
    }

    public void testRoundTrip() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            XmlConvertedListEntity e = new XmlConvertedListEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            XmlConvertedListEntity found = em.find(XmlConvertedListEntity.class, 1L);
            assertEquals(2, found.getItems().size());
            assertEquals("one", found.getItems().get(0).getName());
            assertEquals("two", found.getItems().get(1).getName());
        }
    }

    public void testTheListIsLoadedEagerly() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            XmlConvertedListEntity e = new XmlConvertedListEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            em.persist(e);
            em.getTransaction().commit();
        }
        XmlConvertedListEntity detached;
        try (EntityManager em = emf.createEntityManager()) {
            detached = em.find(XmlConvertedListEntity.class, 1L);
        }
        List<Item> items = detached.getItems();
        assertNotNull("a basic attribute without a fetch type is eager", items);
        assertEquals(2, items.size());
        assertEquals("one", items.get(0).getName());
        assertEquals("two", items.get(1).getName());
    }
}

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
package org.apache.openjpa.persistence.dynamicschema;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests an entity named and with attributes named after words Derby rejects
 * as unquoted identifiers: the schema is created, and the entity is persisted,
 * found and queried. Other databases reserve other words, so the tests only
 * run on Derby.
 */
public class TestReservedWordEntityNames extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(Window.class, DROP_TABLES);
    }

    public void testColumnNames() {
        if (!(getDBDictionary() instanceof DerbyDictionary)) {
            return;
        }
        ClassMapping mapping = getMapping(Window.class);
        for (String field : List.of("cursors", "window", "definer", "invoker", "xmlquery", "deterministic")) {
            assertEquals(field.toUpperCase(Locale.ENGLISH) + "0",
                mapping.getFieldMapping(field).getColumns()[0].getIdentifier().getName());
        }
    }

    public void testPersistFindAndQuery() {
        if (!(getDBDictionary() instanceof DerbyDictionary)) {
            return;
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Window window = new Window();
            window.setId(1);
            window.setCursors("c");
            window.setWindow("w");
            window.setDefiner("d");
            window.setInvoker("i");
            window.setXmlquery("x");
            window.setDeterministic(true);
            em.persist(window);
            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            Window window = em.find(Window.class, 1);
            assertNotNull(window);
            assertEquals("c", window.getCursors());
            assertEquals("w", window.getWindow());
            assertEquals("d", window.getDefiner());
            assertEquals("i", window.getInvoker());
            assertEquals("x", window.getXmlquery());
            assertTrue(window.isDeterministic());
        }

        try (EntityManager em = emf.createEntityManager()) {
            List<Window> windows = em.createQuery(
                "SELECT w FROM Window w WHERE w.cursors = :cursors AND w.window = :window"
                    + " AND w.deterministic = true ORDER BY w.definer, w.invoker, w.xmlquery", Window.class)
                .setParameter("cursors", "c")
                .setParameter("window", "w")
                .getResultList();
            assertEquals(1, windows.size());
            assertEquals(1, windows.get(0).getId());
            assertEquals("x", em.createQuery("SELECT w.xmlquery FROM Window w WHERE w.cursors = 'c'", String.class)
                .getSingleResult());
        }
    }
}

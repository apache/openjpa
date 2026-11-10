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
 * Tests entities named and with attributes named after words Derby rejects
 * as unquoted identifiers, with tables, join tables and collection tables
 * named by the JPA mapping defaults: the schema is created, and the entities
 * are persisted, found, queried and updated. Other databases reserve other
 * words, so the tests only run on Derby.
 */
public class TestReservedWordEntityNames extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setSupportedDatabases(DerbyDictionary.class);
        if (isTestsDisabled()) {
            return;
        }
        setUp(Window.class, Order.class, User.class, UserSession.class, DROP_TABLES);
    }

    public void testTableNames() {
        // the default table names Derby rejects get a 0 appended, the others
        // are used as they are
        assertEquals("WINDOW0", getMapping(Window.class).getTable().getIdentifier().getName());
        assertEquals("ORDER0", getMapping(Order.class).getTable().getIdentifier().getName());
        assertEquals("USER0", getMapping(User.class).getTable().getIdentifier().getName());
        assertEquals("Session", getMapping(UserSession.class).getTable().getIdentifier().getName());
        assertEquals("ORDER0_WINDOW0", getFieldTableName(Order.class, "windows"));
        assertEquals("Window_panes", getFieldTableName(Window.class, "panes"));
        assertEquals("User_roles", getFieldTableName(User.class, "roles"));
        assertEquals("SESSION_USER0", getFieldTableName(UserSession.class, "user"));
    }

    public void testColumnNames() {
        ClassMapping mapping = getMapping(Window.class);
        for (String field : List.of("cursors", "definer", "invoker", "xmlquery", "deterministic")) {
            assertEquals(field.toUpperCase(Locale.ENGLISH) + "0",
                mapping.getFieldMapping(field).getColumns()[0].getIdentifier().getName());
        }
        // WINDOW0 is the name of the table already
        assertEquals("WINDOW01", mapping.getFieldMapping("window").getColumns()[0].getIdentifier().getName());
    }

    public void testPersistFindAndQuery() {
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
            window.getPanes().add("left");
            window.getPanes().add("right");
            em.persist(window);
            User user = new User();
            user.setId(2);
            user.setName("u");
            user.getRoles().add("admin");
            em.persist(user);
            Order order = new Order();
            order.setId(3);
            order.setDescription("o");
            order.setUser(user);
            order.getWindows().add(window);
            em.persist(order);
            UserSession session = new UserSession();
            session.setId(4);
            session.getUser().add("u");
            em.persist(session);
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
            assertEquals(List.of("left", "right"), window.getPanes());
            Order order = em.find(Order.class, 3);
            assertNotNull(order);
            assertEquals("o", order.getDescription());
            assertEquals("u", order.getUser().getName());
            assertEquals(1, order.getWindows().size());
            assertEquals(1, order.getWindows().get(0).getId());
            assertTrue(order.getUser().getRoles().contains("admin"));
            assertTrue(em.find(UserSession.class, 4).getUser().contains("u"));
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
            assertEquals(3, (int) em.createQuery("SELECT o.id FROM Order o JOIN o.windows w JOIN o.user u"
                    + " WHERE w.window = 'w' AND 'admin' MEMBER OF u.roles AND 'right' MEMBER OF w.panes",
                    Integer.class)
                .getSingleResult());
            assertEquals(4, (int) em.createQuery("SELECT s.id FROM Session s WHERE 'u' MEMBER OF s.user",
                    Integer.class)
                .getSingleResult());
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, em.createQuery("UPDATE Window w SET w.definer = 'e' WHERE w.cursors = 'c'")
                .executeUpdate());
            assertEquals(1, em.createQuery("UPDATE Order o SET o.description = 'p' WHERE o.user.id = 2")
                .executeUpdate());
            assertEquals(1, em.createQuery("UPDATE User u SET u.name = 'v' WHERE u.id = 2").executeUpdate());
            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("e", em.find(Window.class, 1).getDefiner());
            assertEquals("p", em.find(Order.class, 3).getDescription());
            assertEquals("v", em.find(User.class, 2).getName());
        }
    }

    private String getFieldTableName(Class<?> cls, String field) {
        return getMapping(cls).getFieldMapping(field).getTable().getIdentifier().getName();
    }
}

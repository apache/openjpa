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
package org.apache.openjpa.persistence.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.StringJoiner;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A bulk update or delete whose WHERE clause only reads the table of the
 * entity is a single statement on that table, without a subquery that
 * selects the primary keys of the matching rows, on the databases that let
 * the statement give the table an alias.
 */
class TestBulkSingleTableRange extends SQLListenerTestCase {

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, BulkRangeOrder.class, BulkRangeRushOrder.class,
            BulkRangeLine.class);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (int i = 1; i <= 4; i++) {
                BulkRangeOrder order = (i <= 2) ? new BulkRangeOrder(i, "NEW")
                    : new BulkRangeRushOrder(i, "NEW");
                em.persist(order);
                em.persist(new BulkRangeLine(2 * i - 1, order, i));
                em.persist(new BulkRangeLine(2 * i, order, i));
            }
            em.getTransaction().commit();
        }
        resetSQL();
    }

    @Test
    void deleteLinesAndOrdersByRange() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int lines = em.createQuery("DELETE FROM BulkRangeLine l"
                + " WHERE l.order.id BETWEEN :from AND :to")
                .setParameter("from", 2L)
                .setParameter("to", 3L)
                .executeUpdate();
            int orders = em.createQuery("DELETE FROM BulkRangeOrder o"
                + " WHERE o.id BETWEEN :from AND :to")
                .setParameter("from", 2L)
                .setParameter("to", 3L)
                .executeUpdate();
            em.getTransaction().commit();
            assertEquals(4, lines, "deleted lines");
            assertEquals(2, orders, "deleted orders");
        }
        assertDirect(bulkStatement("DELETE", "BRG_LINE"), "ORDER_ID",
            "DELETE FROM BRG_LINE t0 WHERE (t0.ORDER_ID >= ? AND t0.ORDER_ID <= ?)");
        assertDirect(bulkStatement("DELETE", "BRG_ORDER"), "ID",
            "DELETE FROM BRG_ORDER t0 WHERE (t0.ID >= ? AND t0.ID <= ?)");
        assertEquals("1,2,7,8", ids("BulkRangeLine"), "remaining lines");
        assertEquals("1,4", ids("BulkRangeOrder"), "remaining orders");
    }

    @Test
    void updateOrdersByRange() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("UPDATE BulkRangeOrder o"
                + " SET o.status = :status WHERE o.id BETWEEN :from AND :to")
                .setParameter("status", "SHIPPED")
                .setParameter("from", 2L)
                .setParameter("to", 3L)
                .executeUpdate();
            em.getTransaction().commit();
            assertEquals(2, count, "updated orders");
        }
        assertDirect(bulkStatement("UPDATE", "BRG_ORDER"), "STATUS",
            "UPDATE BRG_ORDER t0 SET STATUS = ?, VERSION = VERSION + 1"
            + " WHERE (t0.ID >= ? AND t0.ID <= ?)");
        try (EntityManager em = emf.createEntityManager()) {
            for (long id = 1; id <= 4; id++) {
                BulkRangeOrder order = em.find(BulkRangeOrder.class, id);
                boolean changed = id == 2 || id == 3;
                assertEquals(changed ? "SHIPPED" : "NEW", order.getStatus(),
                    "status of order " + id);
                assertEquals(changed ? 2 : 1, order.getVersion(),
                    "version of order " + id);
            }
        }
    }

    @Test
    void updateOrdersFromOwnColumns() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("UPDATE BulkRangeOrder o"
                + " SET o.status = CONCAT(o.status, '-1')"
                + " WHERE o.status = 'NEW' AND o.id > :id")
                .setParameter("id", 2L)
                .executeUpdate();
            em.getTransaction().commit();
            assertEquals(2, count, "updated orders");
        }
        // the concatenation and the literal differ between databases
        String statement = bulkStatement("UPDATE", "BRG_ORDER");
        assertDirect(statement, "STATUS", null);
        if (isTargetAliasDictionary()) {
            assertTrue(statement.startsWith("UPDATE BRG_ORDER t0 SET STATUS = ")
                && statement.contains(", VERSION = VERSION + 1 WHERE (t0.STATUS = ")
                && statement.endsWith(" AND t0.ID > ?)"),
                "single table bulk statement: " + statement);
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("NEW", em.find(BulkRangeOrder.class, 2L).getStatus(),
                "status of order 2");
            assertEquals("NEW-1", em.find(BulkRangeOrder.class, 3L).getStatus(),
                "status of order 3");
        }
    }

    /**
     * The condition on the discriminator column that restricts the statement
     * to the subclass is in the same table.
     */
    @Test
    void deleteSubclassByRange() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("DELETE FROM BulkRangeRushOrder o"
                + " WHERE o.id BETWEEN :from AND :to")
                .setParameter("from", 1L)
                .setParameter("to", 3L)
                .executeUpdate();
            em.getTransaction().commit();
            assertEquals(1, count, "deleted orders");
        }
        assertDirect(bulkStatement("DELETE", "BRG_ORDER"), "ORDER_TYPE",
            "DELETE FROM BRG_ORDER t0 WHERE (t0.ID >= ? AND t0.ID <= ?)"
            + " AND t0.ORDER_TYPE = ?");
        assertEquals("1,2,4", ids("BulkRangeOrder"), "remaining orders");
    }

    /**
     * A condition on another table still needs the subquery.
     */
    @Test
    void deleteLinesByOrderStatus() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("UPDATE BulkRangeOrder o SET o.status = 'OLD'"
                + " WHERE o.id = 2").executeUpdate();
            int count = em.createQuery("DELETE FROM BulkRangeLine l"
                + " WHERE l.order.status = :status")
                .setParameter("status", "OLD")
                .executeUpdate();
            em.getTransaction().commit();
            assertEquals(2, count, "deleted lines");
        }
        if (!getDBDictionary().allowsAliasInBulkClause) {
            assertTrue(bulkStatement("DELETE", "BRG_LINE").contains("SELECT"),
                "delete with a join is not done with a subquery");
        }
        assertEquals("1,2,5,6,7,8", ids("BulkRangeLine"), "remaining lines");
    }

    /**
     * Asserts that the given bulk statement reads the given column and has no
     * subquery where the database allows it. On Derby and PostgreSQL, which
     * name the table of the statement with an alias, the statement must be
     * the given one, unless it is null.
     */
    private void assertDirect(String statement, String column, String targetAliasStatement) {
        assertTrue(statement.contains(column), statement + " does not read " + column);
        DBDictionary dict = getDBDictionary();
        if (isTargetAliasDictionary()) {
            assertFalse(statement.contains("SELECT"),
                "single table bulk statement has a subquery: " + statement);
            if (targetAliasStatement != null) {
                assertEquals(targetAliasStatement, statement, "single table bulk statement");
            }
        } else if (dict.supportsSubselect && (dict.allowsAliasInBulkClause
            || dict.allowsTargetAliasInBulkClause)) {
            assertFalse(statement.contains("SELECT"),
                "single table bulk statement has a subquery: " + statement);
        }
    }

    private boolean isTargetAliasDictionary() {
        DBDictionary dict = getDBDictionary();
        return dict instanceof DerbyDictionary || dict instanceof PostgresDictionary;
    }

    private String bulkStatement(String verb, String table) {
        String found = null;
        for (String statement : sql) {
            if (statement.startsWith(verb) && statement.contains(table)) {
                found = statement;
            }
        }
        assertNotNull(found, "no " + verb + " of " + table + " in " + sql);
        return found;
    }

    private String ids(String entity) {
        try (EntityManager em = emf.createEntityManager()) {
            List<Long> ids = em.createQuery("SELECT e.id FROM " + entity
                + " e ORDER BY e.id", Long.class).getResultList();
            StringJoiner joiner = new StringJoiner(",");
            for (Long id : ids) {
                joiner.add(String.valueOf(id));
            }
            return joiner.toString();
        }
    }
}

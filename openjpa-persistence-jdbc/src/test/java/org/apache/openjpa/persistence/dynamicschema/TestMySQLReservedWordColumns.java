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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MariaDBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Attributes named after words that MySQL 8.0 and later or MariaDB reserve -
 * window functions, <code>SYSTEM</code>, <code>OFFSET</code> - get a column
 * name the database accepts, and an entity named after such a word a table
 * name it accepts, so that the table can be created, written to and queried
 * (OPENJPA-3074).
 */
class TestMySQLReservedWordColumns extends SingleEMFTestCase {

    @Override
    protected boolean isSupported(DBDictionary dict) {
        // other databases reserve other words, Derby e.g. WINDOW
        return dict instanceof MySQLDictionary || dict instanceof MariaDBDictionary;
    }

    @BeforeEach
    public void setUp() {
        super.setUp(CLEAR_TABLES, RankedEntry.class, Window.class, Linear.class, Dual.class, Ntile.class);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new Window(1, "first"));
            em.persist(new Window(2, "second"));
            em.persist(new RankedEntry(1, 10, "a", 2));
            em.persist(new RankedEntry(2, 20, "b", 1));
            em.persist(new RankedEntry(3, 10, "b", 3));
            em.persist(new Linear(1, 5, 10, 2));
            em.persist(new Linear(2, 7, 20, 1));
            em.persist(new Dual(1, "a-z", "first"));
            em.persist(new Ntile(1, "k1,k2"));
            em.getTransaction().commit();
        }
    }

    @Test
    void find() {
        try (EntityManager em = emf.createEntityManager()) {
            RankedEntry entry = em.find(RankedEntry.class, 2);
            assertEquals(20, entry.getRank());
            assertEquals("b", entry.getWindow());
            assertEquals("lead 2", entry.getLead());
            assertEquals("system 2", entry.getSystem());
            assertEquals(1, entry.getRowNumber());
            assertEquals(2, entry.getLag());
        }
    }

    @Test
    void query() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Integer> ids = em.createQuery("SELECT e.id FROM RankedEntry e WHERE e.rank = :rank "
                + "ORDER BY e.rowNumber DESC", Integer.class)
                .setParameter("rank", 10)
                .getResultList();
            assertEquals(List.of(3, 1), ids);

            List<Object[]> rows = em.createQuery("SELECT e.window, MAX(e.rank) FROM RankedEntry e "
                + "GROUP BY e.window ORDER BY e.window", Object[].class)
                .getResultList();
            assertEquals(2, rows.size());
            assertEquals("a", rows.get(0)[0]);
            assertEquals(10, ((Number) rows.get(0)[1]).intValue());
            assertEquals("b", rows.get(1)[0]);
            assertEquals(20, ((Number) rows.get(1)[1]).intValue());
        }
    }

    @Test
    void update() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("UPDATE RankedEntry e SET e.rank = e.rank + 1 WHERE e.window = :window")
                .setParameter("window", "b")
                .executeUpdate();
            assertEquals(2, count);
            RankedEntry entry = em.find(RankedEntry.class, 1);
            entry.setRank(5);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(5, em.find(RankedEntry.class, 1).getRank());
            assertEquals(21, em.find(RankedEntry.class, 2).getRank());
            assertEquals(11, em.find(RankedEntry.class, 3).getRank());
        }
    }

    /**
     * The column name of an attribute named after such a word is delimited,
     * as for the words that were reserved before. Of the words of the entity
     * MariaDB reserves <code>ROW_NUMBER</code>, and <code>RANK</code> as the
     * name of a built-in function. A name given in <code>@Column</code> is
     * treated alike.
     */
    @Test
    void columnNames() {
        DBDictionary dict = getDBDictionary();
        ClassMapping mapping = getMapping(RankedEntry.class);
        boolean mysql = !(dict instanceof MariaDBDictionary);
        assertEquals("\"RANK\"", columnName(mapping, "rank"));
        assertEquals(mysql ? "\"WINDOW\"" : "WINDOW", columnName(mapping, "window"));
        assertEquals(mysql ? "\"SYSTEM\"" : "SYSTEM", columnName(mapping, "system"));
        assertEquals("\"ROW_NUMBER\"", columnName(mapping, "rowNumber"));
        // a delimited name is kept
        assertEquals("\"LAG\"", columnName(mapping, "lag"));

        // only MariaDB reserves OFFSET and RETURNING
        ClassMapping linear = getMapping(Linear.class);
        assertEquals(mysql ? "OFFSET" : "\"OFFSET\"", columnName(linear, "offset"));
        assertEquals("\"LIMIT\"", columnName(linear, "limit"));
        assertEquals("\"NTILE\"", columnName(linear, "ntile"));
        ClassMapping dual = getMapping(Dual.class);
        assertEquals("\"RANGE\"", columnName(dual, "range"));
        assertEquals(mysql ? "RETURNING" : "\"RETURNING\"", columnName(dual, "returning"));
        assertEquals("\"KEYS\"", columnName(getMapping(Ntile.class), "keys"));
    }

    /**
     * The table of an entity named after a word that MySQL or MariaDB
     * reserve, and whose attributes are named after such words, can be
     * written to and queried, also with the words in the query.
     */
    @Test
    void reservedTableAndColumnNames() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(Linear.class, 1).setOffset(6);
            em.find(Dual.class, 1).setReturning("changed");
            assertEquals(1, em.createQuery("UPDATE Linear l SET l.ntile = l.ntile + l.limit WHERE l.offset = 7")
                .executeUpdate());
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            Linear linear = em.find(Linear.class, 1);
            assertEquals(6, linear.getOffset());
            assertEquals(10, linear.getLimit());
            assertEquals(2, linear.getNtile());
            List<Integer> ids = em.createQuery("SELECT l.id FROM Linear l WHERE l.limit > :limit "
                + "ORDER BY l.ntile, l.offset", Integer.class)
                .setParameter("limit", 5)
                .getResultList();
            assertEquals(List.of(1, 2), ids);
            assertEquals(21, em.find(Linear.class, 2).getNtile());

            Dual dual = em.find(Dual.class, 1);
            assertEquals("a-z", dual.getRange());
            assertEquals("changed", dual.getReturning());
            assertEquals(List.of("changed"), em.createQuery("SELECT d.returning FROM Dual d WHERE d.range = 'a-z'",
                String.class).getResultList());

            assertEquals("k1,k2", em.find(Ntile.class, 1).getKeys());
            assertEquals(1L, (long) em.createQuery("SELECT COUNT(n) FROM Ntile n WHERE n.keys LIKE 'k1%'",
                Long.class).getSingleResult());
        }
    }

    @Test
    void reservedTableName() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(Window.class, 1).setLabel("changed");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("changed", em.find(Window.class, 1).getLabel());
            List<String> labels = em.createQuery("SELECT w.label FROM Window w ORDER BY w.id", String.class)
                .getResultList();
            assertEquals(List.of("changed", "second"), labels);
        }
    }

    /**
     * The default table name of an entity named after such a word is
     * delimited. MariaDB does not reserve <code>WINDOW</code>; it reserves
     * <code>NTILE</code> in a table name as the name of a built-in function.
     */
    @Test
    void tableName() {
        DBDictionary dict = getDBDictionary();
        boolean mysql = !(dict instanceof MariaDBDictionary);
        assertEquals(mysql ? "\"WINDOW\"" : "WINDOW", tableName(Window.class));
        assertEquals("\"LINEAR\"", tableName(Linear.class));
        assertEquals("\"DUAL\"", tableName(Dual.class));
        assertEquals("\"NTILE\"", tableName(Ntile.class));
    }

    private String tableName(Class<?> type) {
        return getMapping(type).getTable().getIdentifier().getName().toUpperCase(Locale.ROOT);
    }

    private static String columnName(ClassMapping mapping, String field) {
        return mapping.getFieldMapping(field).getColumns()[0].getIdentifier().getName().toUpperCase(Locale.ROOT);
    }
}

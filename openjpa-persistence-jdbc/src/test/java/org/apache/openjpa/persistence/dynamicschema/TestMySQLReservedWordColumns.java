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
import org.apache.openjpa.jdbc.sql.AbstractMySQLDictionary;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MariaDBDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Attributes named after words that MySQL 8.0 and later reserve - window
 * functions, <code>SYSTEM</code> - get a column name the database accepts,
 * so that the table can be created, written to and queried (OPENJPA-3074).
 */
public class TestMySQLReservedWordColumns extends SingleEMFTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, RankedEntry.class);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new RankedEntry(1, 10, "a", 2));
            em.persist(new RankedEntry(2, 20, "b", 1));
            em.persist(new RankedEntry(3, 10, "b", 3));
            em.getTransaction().commit();
        }
    }

    public void testFind() {
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

    public void testQuery() {
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

    public void testUpdate() {
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
     * On MySQL the default column name of an attribute named after such a
     * word gets a <code>0</code> appended, as for the words that were
     * reserved before. MariaDB reserves none of the words of the entity but
     * <code>ROW_NUMBER</code>. A name given in <code>@Column</code> is
     * treated alike, unless it is delimited.
     */
    public void testColumnNames() {
        DBDictionary dict = getDBDictionary();
        if (!(dict instanceof AbstractMySQLDictionary)) {
            return;
        }
        ClassMapping mapping = getMapping(RankedEntry.class);
        boolean mysql = !(dict instanceof MariaDBDictionary);
        assertEquals(mysql ? "RANK0" : "RANK", columnName(mapping, "rank"));
        assertEquals(mysql ? "WINDOW0" : "WINDOW", columnName(mapping, "window"));
        assertEquals(mysql ? "SYSTEM0" : "SYSTEM", columnName(mapping, "system"));
        assertEquals("ROW_NUMBER0", columnName(mapping, "rowNumber"));
        // a delimited name is kept
        assertEquals("\"LAG\"", columnName(mapping, "lag"));
    }

    private static String columnName(ClassMapping mapping, String field) {
        return mapping.getFieldMapping(field).getColumns()[0].getIdentifier().getName().toUpperCase(Locale.ROOT);
    }
}

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

import java.util.List;
import java.util.StringJoiner;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bulk updates that set a column outside the primary table of the updated
 * entity: a column of a joined superclass table or of a secondary table.
 */
class TestBulkUpdateMultipleTables extends SingleEMFTestCase {

    private static final String PRODUCT = "BulkSubqueryProduct";
    private static final String SOFTWARE = "BulkSubquerySoftware";
    private static final String PART = "BulkSubqueryPart";
    private static final String ENGINE = "BulkSubqueryEngine";

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, BulkSubqueryItem.class, BulkSubqueryProduct.class,
            BulkSubquerySoftware.class, BulkSubqueryPart.class,
            BulkSubqueryEngine.class, BulkSubqueryLine.class,
            BulkVersionedPart.class, BulkVersionedEngine.class,
            BulkCompositePart.class, BulkCompositeEngine.class,
            // fewer keys than rows per statement, so the tables are updated
            // in several chunks of keys
            "openjpa.jdbc.DBDictionary", "inClauseLimit=3");

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (int i = 1; i <= 4; i++) {
                if (i <= 2) {
                    em.persist(new BulkSubqueryProduct(i, i));
                    em.persist(new BulkSubqueryPart(i, i));
                } else {
                    em.persist(new BulkSubquerySoftware(i, i));
                    em.persist(new BulkSubqueryEngine(i, i, 10 * i));
                    em.persist(new BulkVersionedEngine(i, i, 10 * i));
                }
                em.persist(new BulkCompositeEngine((i <= 2) ? 1 : 2, i, i,
                    10 * i));
            }
            em.persist(new BulkCompositePart(3, 5, 5));
            em.getTransaction().commit();
        }
    }

    @Test
    void updateSuperclassColumn() {
        assertEquals(2, execute("UPDATE " + ENGINE
            + " e SET e.quantity = e.quantity + 10"));
        assertEquals("1=1,2=2,3=13,4=14", quantities(PART));
        assertEquals("3=30,4=40", powers());
    }

    @Test
    void updateSuperclassColumnWhereSubclassColumn() {
        assertEquals(1, execute("UPDATE " + ENGINE
            + " e SET e.quantity = :value WHERE e.power > :min", 0, 30));
        assertEquals("1=1,2=2,3=3,4=0", quantities(PART));
        assertEquals("3=30,4=40", powers());
    }

    @Test
    void updateSuperclassAndSubclassColumns() {
        // the WHERE clause reads both tables and no longer matches the row
        // once its quantity is set, so both tables have to be updated for
        // the rows that matched before the update
        assertEquals(1, execute("UPDATE " + ENGINE
            + " e SET e.quantity = e.quantity * 10, e.power = e.power + 1"
            + " WHERE e.quantity = 4 AND e.power > 30"));
        assertEquals("1=1,2=2,3=3,4=40", quantities(PART));
        assertEquals("3=30,4=41", powers());
    }

    @Test
    void updateSuperclassColumnWithSubquery() {
        assertEquals(1, execute("UPDATE " + ENGINE
            + " e SET e.quantity = 0 WHERE e.power < "
            + "(SELECT MAX(s.power) FROM " + ENGINE + " s)"));
        assertEquals("1=1,2=2,3=0,4=4", quantities(PART));
    }

    @Test
    void updateSecondaryTableColumn() {
        assertEquals(4, execute("UPDATE " + PRODUCT
            + " e SET e.detail = 'changed'"));
        assertEquals("1=changed,2=changed,3=changed,4=changed", details());
        assertEquals("1=1,2=2,3=3,4=4", quantities(PRODUCT));
    }

    @Test
    void updateSecondaryTableColumnWherePrimaryColumn() {
        assertEquals(2, execute("UPDATE " + PRODUCT
            + " e SET e.detail = :value WHERE e.quantity > :min",
            "changed", 2));
        assertEquals("1=detail 1,2=detail 2,3=changed,4=changed", details());
    }

    @Test
    void updatePrimaryAndSecondaryTableColumns() {
        // the WHERE clause reads both tables and no longer matches the row
        // once its detail is set
        assertEquals(1, execute("UPDATE " + PRODUCT
            + " e SET e.detail = 'changed', e.quantity = e.quantity + 10"
            + " WHERE e.detail = 'detail 2' AND e.quantity < 3"));
        assertEquals("1=detail 1,2=changed,3=detail 3,4=detail 4", details());
        assertEquals("1=1,2=12,3=3,4=4", quantities(PRODUCT));
    }

    @Test
    void updateSecondaryTableColumnOfSubclass() {
        assertEquals(1, execute("UPDATE " + SOFTWARE
            + " e SET e.detail = 'changed' WHERE e.quantity = 4"));
        assertEquals("1=detail 1,2=detail 2,3=detail 3,4=changed", details());
    }

    @Test
    void updateSecondaryTableColumnToNull() {
        assertEquals(2, execute("UPDATE " + PRODUCT
            + " e SET e.detail = NULL WHERE e.detail IN ('detail 1', 'detail 3')"));
        assertEquals("1=null,2=detail 2,3=null,4=detail 4", details());
    }

    @Test
    void updateSubclassColumnWithVersionInSuperclassTable() {
        assertEquals(1, execute("UPDATE BulkVersionedEngine e"
            + " SET e.power = e.power + 1 WHERE e.quantity > 3"));
        assertEquals("3=30/1,4=41/2", versions());

        assertEquals(2, execute("UPDATE BulkVersionedEngine e"
            + " SET e.quantity = e.quantity + 10, e.power = 0"));
        assertEquals("3=0/2,4=0/3", versions());
        assertEquals("3=13,4=14", quantities("BulkVersionedPart"));
    }

    @Test
    void updateVersionedEntityWithBulkUpdatedState() {
        try (EntityManager em = emf.createEntityManager()) {
            BulkVersionedEngine engine = em.find(BulkVersionedEngine.class, 4L);
            assertEquals(1, engine.getVersion());

            em.getTransaction().begin();
            assertEquals(1, em.createQuery("UPDATE BulkVersionedEngine e"
                + " SET e.quantity = 0 WHERE e.power = 40").executeUpdate());
            em.getTransaction().commit();

            em.refresh(engine);
            assertEquals(0, engine.getQuantity());
            assertEquals(2, engine.getVersion());
        }
    }

    @Test
    void updateSuperclassColumnFromSubclassColumn() {
        assertEquals(1, execute("UPDATE " + ENGINE
            + " e SET e.quantity = e.power + 100 WHERE e.power = 30"));
        assertEquals("1=1,2=2,3=130,4=4", quantities(PART));
        assertEquals("3=30,4=40", powers());
    }

    @Test
    void updatePrimaryTableColumnFromSecondaryTableColumn() {
        assertEquals(1, execute("UPDATE " + PRODUCT
            + " e SET e.quantity = LENGTH(e.detail) WHERE e.id = 1"));
        assertEquals("1=8,2=2,3=3,4=4", quantities(PRODUCT));
    }

    @Test
    void updateSecondaryTableColumnFromItself() {
        assertEquals(2, execute("UPDATE " + PRODUCT
            + " e SET e.detail = CONCAT(e.detail, ' changed')"
            + " WHERE e.quantity > 2"));
        assertEquals("1=detail 1,2=detail 2,3=detail 3 changed,"
            + "4=detail 4 changed", details());
    }

    @Test
    void updateCompositeKeyColumnsOfAllTables() {
        // the root, subclass and secondary tables name their key columns
        // differently, and the WHERE clause reads the root and subclass table
        assertEquals(2, execute("UPDATE BulkCompositeEngine e"
            + " SET e.quantity = e.quantity + 10, e.power = e.power + 1,"
            + " e.label = 'changed'"
            + " WHERE e.power > 10 AND e.name <> 'name 2-3'"));
        assertEquals("1-1=1/10/label 1-1,1-2=12/21/changed,"
            + "2-3=3/30/label 2-3,2-4=14/41/changed", compositeEngines());
        assertEquals("1-1=1,1-2=12,2-3=3,2-4=14,3-5=5", compositeQuantities());
    }

    @Test
    void updateSecondaryTableColumnFromSuperclassColumn() {
        assertEquals(1, execute("UPDATE BulkCompositeEngine e"
            + " SET e.label = e.name WHERE e.number = 1"));
        assertEquals("1-1=1/10/name 1-1,1-2=2/20/label 1-2,"
            + "2-3=3/30/label 2-3,2-4=4/40/label 2-4", compositeEngines());
    }

    private int execute(String jpql, Object... params) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Query q = em.createQuery(jpql);
            if (params.length > 0) {
                q.setParameter("value", params[0]);
                q.setParameter("min", params[1]);
            }
            int count = q.executeUpdate();
            em.getTransaction().commit();
            return count;
        }
    }

    private String details() {
        try (EntityManager em = emf.createEntityManager()) {
            return toString(em.createQuery("SELECT e.id, e.detail FROM "
                + PRODUCT + " e ORDER BY e.id", Object[].class)
                .getResultList());
        }
    }

    private String powers() {
        try (EntityManager em = emf.createEntityManager()) {
            return toString(em.createQuery("SELECT e.id, e.power FROM "
                + ENGINE + " e ORDER BY e.id", Object[].class)
                .getResultList());
        }
    }

    private String versions() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> rows = em.createQuery("SELECT e.id, e.power,"
                + " e.version FROM BulkVersionedEngine e ORDER BY e.id",
                Object[].class).getResultList();
            StringJoiner result = new StringJoiner(",");
            for (Object[] row : rows) {
                result.add(row[0] + "=" + row[1] + "/" + row[2]);
            }
            return result.toString();
        }
    }

    private String compositeEngines() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> rows = em.createQuery("SELECT e.region, e.number,"
                + " e.quantity, e.power, e.label FROM BulkCompositeEngine e"
                + " ORDER BY e.region, e.number", Object[].class)
                .getResultList();
            StringJoiner result = new StringJoiner(",");
            for (Object[] row : rows) {
                result.add(row[0] + "-" + row[1] + "=" + row[2] + "/" + row[3]
                    + "/" + row[4]);
            }
            return result.toString();
        }
    }

    private String compositeQuantities() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> rows = em.createQuery("SELECT e.region, e.number,"
                + " e.quantity FROM BulkCompositePart e"
                + " ORDER BY e.region, e.number", Object[].class)
                .getResultList();
            StringJoiner result = new StringJoiner(",");
            for (Object[] row : rows) {
                result.add(row[0] + "-" + row[1] + "=" + row[2]);
            }
            return result.toString();
        }
    }

    private String quantities(String entity) {
        try (EntityManager em = emf.createEntityManager()) {
            return toString(em.createQuery("SELECT e.id, e.quantity FROM "
                + entity + " e ORDER BY e.id", Object[].class)
                .getResultList());
        }
    }

    private static String toString(List<Object[]> rows) {
        StringJoiner result = new StringJoiner(",");
        for (Object[] row : rows) {
            result.add(row[0] + "=" + row[1]);
        }
        return result.toString();
    }
}

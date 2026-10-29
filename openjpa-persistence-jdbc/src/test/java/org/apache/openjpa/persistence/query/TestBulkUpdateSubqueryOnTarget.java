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

import java.util.List;
import java.util.StringJoiner;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Bulk updates and deletes whose WHERE clause has a subquery over the
 * updated entity. MySQL rejects a statement that reads the updated table in
 * a subquery (error 1093), and the subquery must see the rows as they were
 * before the statement.
 */
public class TestBulkUpdateSubqueryOnTarget extends SingleEMFTestCase {

    private static final String ITEM = "BulkSubqueryItem";
    private static final String PRODUCT = "BulkSubqueryProduct";
    private static final String SOFTWARE = "BulkSubquerySoftware";
    private static final String PART = "BulkSubqueryPart";
    private static final String ENGINE = "BulkSubqueryEngine";

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, BulkSubqueryItem.class, BulkSubqueryProduct.class,
            BulkSubquerySoftware.class, BulkSubqueryPart.class,
            BulkSubqueryEngine.class, BulkSubqueryLine.class);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (int i = 1; i <= 4; i++) {
                em.persist(new BulkSubqueryItem(i, i));
                em.persist(new BulkSubqueryLine((i + 1) / 2, i, i));
                if (i <= 2) {
                    em.persist(new BulkSubqueryProduct(i, i));
                    em.persist(new BulkSubqueryPart(i, i));
                } else {
                    em.persist(new BulkSubquerySoftware(i, i));
                    em.persist(new BulkSubqueryEngine(i, i, 10 * i));
                }
            }
            em.getTransaction().commit();
        }
    }

    public void testUpdateExistsCorrelatedToTarget() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 1, execute("UPDATE " + entity
                + " e SET e.quantity = e.quantity * 10 WHERE EXISTS (SELECT s FROM "
                + entity + " s WHERE e.id = :id)", 1L));
            assertEquals(entity, "1=10,2=2,3=3,4=4", quantities(entity));
        }
    }

    public void testUpdateParametersInSetAndSubquery() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();
                int count = em.createQuery("UPDATE " + entity
                    + " e SET e.quantity = e.quantity * :factor"
                    + " WHERE e.quantity >= :min AND EXISTS (SELECT s FROM "
                    + entity + " s WHERE e.id = :id AND s.quantity > :sub)")
                    .setParameter("factor", 7)
                    .setParameter("min", 2)
                    .setParameter("id", 3L)
                    .setParameter("sub", 3)
                    .executeUpdate();
                em.getTransaction().commit();
                assertEquals(entity, 1, count);
            }
            assertEquals(entity, "1=1,2=2,3=21,4=4", quantities(entity));
        }
    }

    public void testUpdateExistsCorrelated() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 3, execute("UPDATE " + entity
                + " e SET e.quantity = e.quantity + 10 WHERE EXISTS (SELECT s FROM "
                + entity + " s WHERE s.quantity > e.quantity)"));
            assertEquals(entity, "1=11,2=12,3=13,4=4", quantities(entity));
        }
    }

    public void testUpdateInSubquery() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 2, execute("UPDATE " + entity
                + " e SET e.quantity = 0 WHERE e.id IN (SELECT s.id FROM "
                + entity + " s WHERE s.quantity > 2)"));
            assertEquals(entity, "1=1,2=2,3=0,4=0", quantities(entity));
        }
    }

    public void testUpdateSubquerySeesStateBeforeUpdate() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 3, execute("UPDATE " + entity
                + " e SET e.quantity = e.quantity + 3 WHERE e.quantity < "
                + "(SELECT MAX(s.quantity) FROM " + entity + " s)"));
            assertEquals(entity, "1=4,2=5,3=6,4=4", quantities(entity));
        }
    }

    public void testUpdateSubclass() {
        assertEquals(1, execute("UPDATE " + SOFTWARE
            + " e SET e.quantity = e.quantity + 3 WHERE e.quantity < "
            + "(SELECT MAX(s.quantity) FROM " + SOFTWARE + " s)"));
        assertEquals("3=6,4=4", quantities(SOFTWARE));
        assertEquals("1=1,2=2,3=6,4=4", quantities(PRODUCT));
    }

    public void testUpdateSubclassTable() {
        assertEquals(1, execute("UPDATE " + ENGINE
            + " e SET e.power = e.power + 1 WHERE e.quantity > 3"));
        assertEquals("3=30,4=41", powers());

        assertEquals(1, execute("UPDATE " + ENGINE
            + " e SET e.power = 0 WHERE e.quantity < "
            + "(SELECT MAX(s.quantity) FROM " + ENGINE + " s)"));
        assertEquals("3=0,4=41", powers());
    }

    public void testCompositePrimaryKey() {
        assertEquals(3, execute("UPDATE BulkSubqueryLine e"
            + " SET e.quantity = e.quantity + 3 WHERE e.quantity < "
            + "(SELECT MAX(s.quantity) FROM BulkSubqueryLine s)"));
        assertEquals("1/1=4,1/2=5,2/3=6,2/4=4", lines());

        assertEquals(2, execute("DELETE FROM BulkSubqueryLine e"
            + " WHERE EXISTS (SELECT s FROM BulkSubqueryLine s"
            + " WHERE s.orderId = e.orderId AND s.quantity > e.quantity)"));
        assertEquals("1/2=5,2/3=6", lines());
    }

    public void testDeleteExistsCorrelatedToTarget() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 1, execute("DELETE FROM " + entity
                + " e WHERE EXISTS (SELECT s FROM " + entity
                + " s WHERE e.id = :id)", 1L));
            assertEquals(entity, "2=2,3=3,4=4", quantities(entity));
        }
    }

    public void testDeleteInSubquery() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 2, execute("DELETE FROM " + entity
                + " e WHERE e.id IN (SELECT s.id FROM " + entity
                + " s WHERE s.quantity > 2)"));
            assertEquals(entity, "1=1,2=2", quantities(entity));
        }
    }

    public void testDeleteScalarSubquery() {
        for (String entity : new String[] { ITEM, PRODUCT, PART }) {
            assertEquals(entity, 3, execute("DELETE FROM " + entity
                + " e WHERE e.quantity < (SELECT MAX(s.quantity) FROM "
                + entity + " s)"));
            assertEquals(entity, "4=4", quantities(entity));
        }
    }

    public void testDeleteSubclass() {
        for (String entity : new String[] { SOFTWARE, ENGINE }) {
            assertEquals(entity, 1, execute("DELETE FROM " + entity
                + " e WHERE e.id IN (SELECT s.id FROM " + entity
                + " s WHERE s.quantity > 3)"));
            assertEquals(entity, "3=3", quantities(entity));
        }
    }

    private int execute(String jpql, Object... params) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Query q = em.createQuery(jpql);
            if (params.length > 0) {
                q.setParameter("id", params[0]);
            }
            int count = q.executeUpdate();
            em.getTransaction().commit();
            return count;
        }
    }

    private String powers() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> rows = em.createQuery("SELECT e.id, e.power FROM "
                + ENGINE + " e ORDER BY e.id", Object[].class).getResultList();
            return toString(rows);
        }
    }

    private String lines() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> rows = em.createQuery("SELECT e.orderId, e.lineNo,"
                + " e.quantity FROM BulkSubqueryLine e ORDER BY e.lineNo",
                Object[].class).getResultList();
            StringJoiner result = new StringJoiner(",");
            for (Object[] row : rows) {
                result.add(row[0] + "/" + row[1] + "=" + row[2]);
            }
            return result.toString();
        }
    }

    private String quantities(String entity) {
        try (EntityManager em = emf.createEntityManager()) {
            List<Object[]> rows = em.createQuery("SELECT e.id, e.quantity FROM "
                + entity + " e ORDER BY e.id", Object[].class).getResultList();
            return toString(rows);
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

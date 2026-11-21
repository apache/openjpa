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
package org.apache.openjpa.persistence.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Removing an entity does not load the lazy to-one relations it does not
 * cascade the removal to: the foreign key values are in its row already.
 */
class TestRemoveLazyRelation extends SQLListenerTestCase {

    private static final int ORDERS = 3;
    private static final int LINES = 2;

    @BeforeEach
    public void setUp() {
        setUp(RemoveCustomer.class, RemoveProduct.class, RemoveOrder.class, RemoveOrderLine.class,
            "openjpa.jdbc.MappingDefaults",
            "ForeignKeyDeleteAction=restrict, JoinForeignKeyDeleteAction=restrict",
            "openjpa.jdbc.DBDictionary", "batchLimit=50",
            CLEAR_TABLES);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            RemoveCustomer customer = new RemoveCustomer(1, "customer");
            em.persist(customer);
            RemoveProduct[] products = new RemoveProduct[LINES];
            for (int p = 0; p < LINES; p++) {
                products[p] = new RemoveProduct(p + 1, "sku" + p);
                em.persist(products[p]);
            }
            for (int o = 1; o <= ORDERS; o++) {
                RemoveOrder order = new RemoveOrder(o, customer);
                for (int l = 0; l < LINES; l++) {
                    order.addLine(new RemoveOrderLine(o * 100L + l, products[l], l + 1));
                }
                em.persist(order);
            }
            em.getTransaction().commit();
        }
    }

    @Test
    void removeDoesNotLoadLazyToOneRelations() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            List<RemoveOrder> orders = em.createQuery(
                "select o from RemoveOrder o", RemoveOrder.class).getResultList();
            resetSQL();
            for (RemoveOrder order : orders) {
                em.remove(order);
            }
            em.getTransaction().commit();
        }
        assertNoneSQLAnyOrder("SELECT .* FROM DEL_RM_CUSTOMER .*", "SELECT .* FROM DEL_RM_PRODUCT .*");

        try (EntityManager em = emf.createEntityManager()) {
            for (int o = 1; o <= ORDERS; o++) {
                assertNull(em.find(RemoveOrder.class, (long) o), "order " + o + " not removed");
            }
            assertEquals(0L, em.createQuery("select count(l) from RemoveOrderLine l").getSingleResult(),
                "lines not removed");
            assertEquals(1L, em.createQuery("select count(c) from RemoveCustomer c").getSingleResult(),
                "customer removed");
            assertEquals((long) LINES, em.createQuery("select count(p) from RemoveProduct p").getSingleResult(),
                "products removed");
        }
    }

    /**
     * A related instance that is removed in the same flush is still known
     * when the relation field is not loaded, so the row referencing it is
     * deleted first.
     */
    @Test
    void removeOrdersDeleteOfUnloadedRelation() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            RemoveProduct product = em.find(RemoveProduct.class, 1L);
            List<RemoveOrderLine> lines = em.createQuery(
                "select l from RemoveOrderLine l where l.product.id = 1", RemoveOrderLine.class).getResultList();
            assertEquals(ORDERS, lines.size(), "lines of product");
            resetSQL();
            em.remove(product);
            for (RemoveOrderLine line : lines) {
                em.remove(line);
            }
            em.getTransaction().commit();
        }
        assertNoneSQLAnyOrder("SELECT .* FROM DEL_RM_CUSTOMER .*", "SELECT .* FROM DEL_RM_PRODUCT .*");
        int lineDelete = -1;
        int productDelete = -1;
        for (int i = 0; i < sql.size(); i++) {
            if (sql.get(i).startsWith("DELETE FROM DEL_RM_LINE")) {
                lineDelete = i;
            } else if (sql.get(i).startsWith("DELETE FROM DEL_RM_PRODUCT")) {
                productDelete = i;
            }
        }
        assertTrue(lineDelete >= 0 && productDelete > lineDelete, "line not deleted before product: " + sql);

        try (EntityManager em = emf.createEntityManager()) {
            assertNull(em.find(RemoveProduct.class, 1L), "product not removed");
            assertEquals((long) ORDERS * (LINES - 1),
                em.createQuery("select count(l) from RemoveOrderLine l").getSingleResult(),
                "lines of product not removed");
        }
    }

    /**
     * A related instance that is removed and replaced by a new instance with
     * the same id in the same flush: the row referencing the removed instance
     * is still deleted before it, and the new instance is inserted.
     */
    @Test
    void removeOrdersDeleteOfUnloadedRelationReplacedByNewInstance() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            RemoveProduct product = em.find(RemoveProduct.class, 1L);
            List<RemoveOrderLine> lines = em.createQuery(
                "select l from RemoveOrderLine l where l.product.id = 1", RemoveOrderLine.class).getResultList();
            assertEquals(ORDERS, lines.size(), "lines of product");
            resetSQL();
            em.remove(product);
            em.persist(new RemoveProduct(1, "again"));
            for (RemoveOrderLine line : lines) {
                em.remove(line);
            }
            em.getTransaction().commit();
        }
        int lineDelete = -1;
        int productDelete = -1;
        int productInsert = -1;
        for (int i = 0; i < sql.size(); i++) {
            if (sql.get(i).startsWith("DELETE FROM DEL_RM_LINE")) {
                lineDelete = i;
            } else if (sql.get(i).startsWith("DELETE FROM DEL_RM_PRODUCT")) {
                productDelete = i;
            } else if (sql.get(i).startsWith("INSERT INTO DEL_RM_PRODUCT")) {
                productInsert = i;
            }
        }
        assertTrue(lineDelete >= 0 && productDelete > lineDelete, "line not deleted before product: " + sql);
        assertTrue(productInsert > productDelete, "product not inserted after delete: " + sql);

        try (EntityManager em = emf.createEntityManager()) {
            RemoveProduct product = em.find(RemoveProduct.class, 1L);
            assertEquals("again", product.getSku(), "new product not stored");
            assertEquals((long) ORDERS * (LINES - 1),
                em.createQuery("select count(l) from RemoveOrderLine l").getSingleResult(),
                "lines of product not removed");
        }
    }
}

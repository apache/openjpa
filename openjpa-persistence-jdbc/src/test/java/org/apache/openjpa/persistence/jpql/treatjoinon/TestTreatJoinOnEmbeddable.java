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
package org.apache.openjpa.persistence.jpql.treatjoinon;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPQL TREAT, JOIN ON, and embeddable query result handling.
 * Covers:
 * - TREAT(entity AS SubType).field in WHERE clause
 * - JOIN TREAT(path AS SubType) alias in FROM clause
 * - JOIN ... ON (condition) clause
 * - Numeric literal suffixes (D/d, F/f, L/l) with TREAT
 * - Scientific notation with TREAT
 * - Embeddable objects from query projections are not managed
 */
public class TestTreatJoinOnEmbeddable extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(TProduct.class, TSoftwareProduct.class,
            TLineItem.class, TOrder.class,
            TCustomer.class, TCountry.class,
            DROP_TABLES);
        createTestData();
    }

    private void createTestData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Create software products
        TSoftwareProduct sp1 = new TSoftwareProduct();
        sp1.setName("Software A");
        sp1.setRevisionNumber(1.0);
        em.persist(sp1);

        TSoftwareProduct sp2 = new TSoftwareProduct();
        sp2.setName("Software B");
        sp2.setRevisionNumber(2.0);
        em.persist(sp2);

        TSoftwareProduct sp3 = new TSoftwareProduct();
        sp3.setName("Software C");
        sp3.setRevisionNumber(1.0);
        em.persist(sp3);

        // Create a hardware product (base type)
        TProduct hw = new TProduct();
        hw.setName("Hardware X");
        em.persist(hw);

        // Create orders with line items
        TOrder order1 = new TOrder();
        em.persist(order1);

        TLineItem li1 = new TLineItem();
        li1.setQuantity(3);
        li1.setProduct(sp1);
        li1.setOrder(order1);
        em.persist(li1);

        TLineItem li2 = new TLineItem();
        li2.setQuantity(10);
        li2.setProduct(sp2);
        li2.setOrder(order1);
        em.persist(li2);

        TOrder order2 = new TOrder();
        em.persist(order2);

        TLineItem li3 = new TLineItem();
        li3.setQuantity(1);
        li3.setProduct(hw);
        li3.setOrder(order2);
        em.persist(li3);

        // Create customer with embedded country
        TCustomer cust = new TCustomer();
        cust.setName("John");
        TCountry country = new TCountry();
        country.setCode("USA");
        country.setCountry("United States");
        cust.setCountry(country);
        em.persist(cust);

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test TREAT in WHERE clause: TREAT(p AS TSoftwareProduct).revisionNumber
     * Corresponds to TCK treatInWhereClauseTest
     */
    public void testTreatInWhereClause() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<String> results = em.createQuery(
            "SELECT p.name FROM TProduct p WHERE TREAT(p AS TSoftwareProduct).revisionNumber = 1.0",
            String.class).getResultList();

        Collections.sort(results);
        assertEquals(2, results.size());
        assertTrue(results.contains("Software A"));
        assertTrue(results.contains("Software C"));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test TREAT in WHERE with D suffix for double literal.
     * Corresponds to TCK appropriateSuffixesTest
     */
    public void testTreatWithDoubleSuffix() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<String> results = em.createQuery(
            "SELECT p.name FROM TProduct p WHERE TREAT(p AS TSoftwareProduct).revisionNumber = 1.0D",
            String.class).getResultList();

        assertEquals(2, results.size());
        assertTrue(results.contains("Software A"));
        assertTrue(results.contains("Software C"));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test TREAT in WHERE with scientific notation.
     * Corresponds to TCK sqlApproximateNumericLiteralTest
     */
    public void testTreatWithScientificNotation() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<String> results = em.createQuery(
            "SELECT p.name FROM TProduct p WHERE TREAT(p AS TSoftwareProduct).revisionNumber = 1E0",
            String.class).getResultList();

        assertEquals(2, results.size());
        assertTrue(results.contains("Software A"));
        assertTrue(results.contains("Software C"));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test JOIN TREAT(path AS SubType) alias.
     * Corresponds to TCK treatJoinClassTest
     */
    public void testTreatJoinClass() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<String> results = em.createQuery(
            "SELECT s.name FROM TLineItem l JOIN TREAT(l.product AS TSoftwareProduct) s",
            String.class).getResultList();

        // TREAT join should only return software products (not hardware)
        // We have 2 line items with software products (sp1, sp2) and 1 with hardware
        assertNotNull(results);
        Collections.sort(results);
        assertEquals("TREAT join should return only software products", 2, results.size());
        assertTrue(results.contains("Software A"));
        assertTrue(results.contains("Software B"));
        // Hardware X should NOT be in the results due to TREAT filtering
        assertFalse(results.contains("Hardware X"));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test JOIN ... ON (condition) clause.
     * Corresponds to TCK joinOnExpressionTest
     */
    public void testJoinOnExpression() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<TOrder> results = em.createQuery(
            "SELECT o FROM TOrder o INNER JOIN o.lineItems l ON (l.quantity > 5)",
            TOrder.class).getResultList();

        // Only order1 has a line item with quantity > 5 (li2 has quantity 10)
        assertEquals(1, results.size());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test that embeddable objects returned from query projections are not managed.
     * Corresponds to TCK embeddableNotManagedTest
     */
    public void testEmbeddableNotManagedInQueryResult() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<Object[]> results = em.createQuery(
            "SELECT c, c.country FROM TCustomer c WHERE c.name = :name")
            .setParameter("name", "John")
            .getResultList();

        assertEquals(1, results.size());
        Object[] row = results.get(0);
        TCustomer cust = (TCustomer) row[0];
        TCountry country = (TCountry) row[1];

        // The embeddable from projection should be a different object from entity's
        assertNotSame(cust.getCountry(), country);

        // Modify the projected embeddable - should not throw
        country.setCode("CHA");
        country.setCountry("China");

        // Flush and refresh the entity
        em.flush();
        em.refresh(cust);

        // The entity's embedded country should NOT be affected
        assertNotEquals("CHA", cust.getCountry().getCode());
        assertEquals("USA", cust.getCountry().getCode());

        em.getTransaction().commit();
        em.close();
    }
}

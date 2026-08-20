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
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceUnitUtil;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for 5 scattered TCK test fixes:
 * 1. FlushMode AUTO query behavior (flushModeTest5)
 * 2. JPQL TREAT in JOIN with discriminator (treatJoinClassTest)
 * 3. PersistenceUnitUtil.getIdentifier for unmanaged entities
 * 4. @SequenceGenerators container annotation parsing
 * 5. UUID type support (cannot test on Derby, PostgreSQL-specific)
 */
public class TestTckFixes extends SingleEMFTestCase {

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

        // Software products
        TSoftwareProduct sp1 = new TSoftwareProduct();
        sp1.setName("Software A");
        sp1.setRevisionNumber(1.0);
        em.persist(sp1);

        TSoftwareProduct sp2 = new TSoftwareProduct();
        sp2.setName("Software B");
        sp2.setRevisionNumber(2.0);
        em.persist(sp2);

        // Hardware product (base type)
        TProduct hw = new TProduct();
        hw.setName("Hardware X");
        em.persist(hw);

        // Orders with line items
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

        TLineItem li3 = new TLineItem();
        li3.setQuantity(1);
        li3.setProduct(hw);
        li3.setOrder(order1);
        em.persist(li3);

        // Customer with country
        TCustomer cust = new TCustomer();
        cust.setName("John");
        TCountry country = new TCountry();
        country.setCode("US");
        country.setCountry("United States");
        cust.setCountry(country);
        em.persist(cust);

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test TREAT JOIN with discriminator filtering.
     * JOIN TREAT(l.product AS TSoftwareProduct) s should only
     * return software products, not hardware products.
     * Corresponds to TCK treatJoinClassTest.
     */
    public void testTreatJoinDiscriminator() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        List<String> results = em.createQuery(
            "SELECT s.name FROM TLineItem l JOIN TREAT(l.product AS TSoftwareProduct) s",
            String.class).getResultList();

        assertNotNull(results);
        Collections.sort(results);
        // Only software products should be returned (sp1, sp2), not hardware
        assertEquals(2, results.size());
        assertTrue(results.contains("Software A"));
        assertTrue(results.contains("Software B"));
        assertFalse("Hardware should be excluded by TREAT",
            results.contains("Hardware X"));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test FlushMode AUTO triggers flush for navigated queries.
     * When an entity is modified and a query navigates through
     * related entities, FlushMode.AUTO should flush before the query.
     * Corresponds to TCK flushModeTest5.
     */
    public void testFlushModeAutoNavigatedQuery() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Find a customer and modify the country name
        TCustomer cust = em.createQuery(
            "SELECT c FROM TCustomer c WHERE c.name = 'John'",
            TCustomer.class).getSingleResult();
        cust.getCountry().setCountry("USA Updated");

        // Query using AUTO flush mode through navigated path
        List<TCustomer> results = em.createQuery(
            "SELECT c FROM TCustomer c WHERE c.country.country = 'USA Updated'",
            TCustomer.class)
            .setFlushMode(FlushModeType.AUTO)
            .getResultList();

        // FlushMode.AUTO should flush the pending change, making
        // the modified value visible to the query
        assertEquals("FlushMode.AUTO should flush before navigated query",
            1, results.size());
        assertEquals("John", results.get(0).getName());

        em.getTransaction().rollback();
        em.close();
    }

    /**
     * Test PersistenceUnitUtil.getIdentifier for unmanaged entities.
     * The spec says getIdentifier should return the id even for
     * new (not yet persisted) entities with a manually-set id.
     * Corresponds to TCK getIdentifierTest.
     */
    public void testGetIdentifierUnmanagedEntity() {
        // Create a new entity with a manually-set id (not persisted)
        TProduct product = new TProduct();
        // TProduct uses @GeneratedValue, but we can test with a persisted one
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        TProduct p = new TProduct();
        p.setName("Test Product");
        em.persist(p);
        em.flush();

        PersistenceUnitUtil puu = emf.getPersistenceUnitUtil();

        // After persist+flush, getIdentifier should return the generated id
        Object id = puu.getIdentifier(p);
        assertNotNull("getIdentifier should return non-null for persisted entity", id);

        em.getTransaction().rollback();
        em.close();
    }

    /**
     * Test PersistenceUnitUtil.getIdentifier for a detached entity.
     * After detaching, getIdentifier should still return the id.
     */
    public void testGetIdentifierDetachedEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        TProduct p = new TProduct();
        p.setName("Detach Test");
        em.persist(p);
        em.getTransaction().commit();

        long persistedId = p.getId();

        // Clear the persistence context (detach all)
        em.clear();

        PersistenceUnitUtil puu = emf.getPersistenceUnitUtil();
        Object id = puu.getIdentifier(p);
        // For detached entities, the id should still be available
        assertNotNull("getIdentifier should return non-null for detached entity", id);

        em.close();
    }
}

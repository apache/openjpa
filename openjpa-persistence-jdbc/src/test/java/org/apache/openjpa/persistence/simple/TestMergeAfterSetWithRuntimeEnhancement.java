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
package org.apache.openjpa.persistence.simple;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that field values set via setter and then merge()'d are persisted
 * correctly with runtime-enhanced property-access entities.
 *
 * Mirrors the TCK pattern: persist entity, then set fields, merge, flush.
 * TCK tests like queryTest8 fail because totalPrice set via merge isn't persisted.
 */
public class TestMergeAfterSetWithRuntimeEnhancement extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedPropertyOrderEntity.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false",
            "openjpa.Compatibility", "StrictIdentityValues=true",
            "openjpa.Log", "SQL=TRACE");
    }

    /**
     * Test: persist entity, then set a field and merge - the value should be persisted.
     * This mirrors TCK createOrderData: persist order, then setTotalPrice, merge.
     */
    public void testSetFieldAfterPersistAndMerge() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedPropertyOrderEntity order = new UnenhancedPropertyOrderEntity();
        order.setId("1");
        em.persist(order);
        em.flush();

        // Now set totalPrice after persist (TCK pattern)
        order.setTotalPrice(5000.0);
        em.merge(order);
        em.flush();

        em.getTransaction().commit();
        em.close();

        // Query in new EM to verify value was persisted
        em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedPropertyOrderEntity found = em.find(UnenhancedPropertyOrderEntity.class, "1");
        assertNotNull("Order should be found", found);
        assertEquals("totalPrice should be 5000.0", 5000.0, found.getTotalPrice(), 0.001);

        // Also test JPQL query with the field
        Query q = em.createQuery(
            "SELECT o FROM UnenhancedPropertyOrderEntity o WHERE o.totalPrice > 4500");
        List<?> results = q.getResultList();
        assertEquals("Should find 1 order with totalPrice > 4500", 1, results.size());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test: Exact TCK pattern - persist+flush in same tx, then set field,
     * merge, flush, commit. Then query in a new tx.
     * This is exactly what createOrderData does.
     */
    public void testTCKPatternPersistFlushSetMergeFlushCommitThenQuery() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Step 1: persist and flush (like TCK persist+doFlush)
        UnenhancedPropertyOrderEntity order1 = new UnenhancedPropertyOrderEntity();
        order1.setId("10");
        em.persist(order1);
        em.flush();

        UnenhancedPropertyOrderEntity order2 = new UnenhancedPropertyOrderEntity();
        order2.setId("11");
        em.persist(order2);
        em.flush();

        // Step 2: set totalPrice on the SAME reference, then merge+flush
        // (TCK: orderRef[0].setTotalPrice(totalPrice); em.merge(orderRef[0]); doFlush();)
        order1.setTotalPrice(5000.0);
        order1.setCustomerName("Alice");
        em.merge(order1);
        em.flush();

        order2.setTotalPrice(500.0);
        order2.setCustomerName("Bob");
        em.merge(order2);
        em.flush();

        em.getTransaction().commit();
        em.close();

        // Step 3: Query in new EM (like TCK test method)
        em = emf.createEntityManager();
        em.getTransaction().begin();

        // This query mirrors queryTest8: NOT totalPrice < 4500
        List<?> results = em.createQuery(
            "SELECT o FROM UnenhancedPropertyOrderEntity o WHERE NOT o.totalPrice < 4500")
            .getResultList();
        assertEquals("Should find 1 order with totalPrice >= 4500", 1, results.size());

        // This mirrors test_ANDconditionTT
        results = em.createQuery(
            "SELECT o FROM UnenhancedPropertyOrderEntity o WHERE o.customerName = 'Alice' AND o.totalPrice > 500")
            .getResultList();
        assertEquals("Should find Alice with totalPrice > 500", 1, results.size());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test: persist entity in one transaction, then find+set+merge in another.
     */
    public void testSetFieldAfterFindAndMerge() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedPropertyOrderEntity order = new UnenhancedPropertyOrderEntity();
        order.setId("2");
        order.setTotalPrice(100.0);
        em.persist(order);
        em.getTransaction().commit();
        em.close();

        // New EM: find, modify, merge
        em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedPropertyOrderEntity found = em.find(UnenhancedPropertyOrderEntity.class, "2");
        assertNotNull(found);
        found.setTotalPrice(9999.0);
        em.merge(found);
        em.getTransaction().commit();
        em.close();

        // Verify
        em = emf.createEntityManager();
        UnenhancedPropertyOrderEntity verify = em.find(UnenhancedPropertyOrderEntity.class, "2");
        assertEquals("totalPrice should be updated to 9999.0", 9999.0, verify.getTotalPrice(), 0.001);
        em.close();
    }
}

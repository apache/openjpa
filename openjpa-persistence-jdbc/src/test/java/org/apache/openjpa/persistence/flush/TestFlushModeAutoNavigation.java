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
package org.apache.openjpa.persistence.flush;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that FlushModeType.AUTO correctly flushes dirty entities
 * before queries that navigate multi-hop relationships.
 * Reproduces TCK core.query.flushmode.Client2#flushModeTest5.
 *
 * The test modifies a Spouse entity's lastName, then queries through
 * Order->Customer->Spouse with FlushModeType.AUTO. The dirty Spouse
 * should be flushed before the query executes.
 */
public class TestFlushModeAutoNavigation extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(
            UnenhancedFlushSpouse.class,
            UnenhancedFlushCustomer.class,
            UnenhancedFlushOrder.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            DROP_TABLES
        );

        // Populate test data
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedFlushCustomer cust10 =
            new UnenhancedFlushCustomer("10", "Kate P. Hudson");
        em.persist(cust10);

        UnenhancedFlushSpouse s4 =
            new UnenhancedFlushSpouse("4", "Thomas", "Mullen");
        s4.setCustomer(cust10);
        em.persist(s4);

        cust10.setSpouse(s4);
        em.merge(cust10);

        UnenhancedFlushOrder order11 =
            new UnenhancedFlushOrder("11", cust10);
        em.persist(order11);

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Mirrors TCK flushModeTest5: modify Spouse.lastName,
     * query through Order->Customer->Spouse with FlushModeType.AUTO.
     */
    public void testFlushModeAutoMultiHopNavigation() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Find and modify Spouse
        UnenhancedFlushSpouse s4 = em.find(UnenhancedFlushSpouse.class, "4");
        assertNotNull("Spouse 4 should exist", s4);
        assertEquals("Mullen", s4.getLastName());
        s4.setLastName("Miller");

        // Query through multi-hop navigation with AUTO flush
        List<UnenhancedFlushOrder> result = em.createQuery(
            "SELECT o FROM UnenhancedFlushOrder o " +
            "WHERE o.customer.spouse.lastName = 'Miller'",
            UnenhancedFlushOrder.class)
            .setFlushMode(FlushModeType.AUTO)
            .getResultList();

        assertEquals("Should find 1 order (order 11) via the updated spouse",
            1, result.size());
        assertEquals("11", result.get(0).getId());

        em.getTransaction().rollback();
        em.close();
    }

    /**
     * Mirrors TCK flushModeTest3: modify Customer.name,
     * query through Order->Customer with FlushModeType.AUTO.
     */
    public void testFlushModeAutoSingleHopNavigation() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Find and modify Customer
        UnenhancedFlushCustomer cust =
            em.find(UnenhancedFlushCustomer.class, "10");
        assertNotNull("Customer 10 should exist", cust);
        cust.setName("Michael Bouschen");

        // Query through single-hop navigation with AUTO flush
        List<UnenhancedFlushOrder> result = em.createQuery(
            "SELECT o FROM UnenhancedFlushOrder o " +
            "WHERE o.customer.name = 'Michael Bouschen'",
            UnenhancedFlushOrder.class)
            .setFlushMode(FlushModeType.AUTO)
            .getResultList();

        assertEquals("Should find 1 order via the updated customer name",
            1, result.size());
        assertEquals("11", result.get(0).getId());

        em.getTransaction().rollback();
        em.close();
    }
}

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
package org.apache.openjpa.persistence.cascade.mx1;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests cascade persist/all on ManyToOne: persist B with cascade, remove both,
 * re-persist removed B, verify cascade re-persists A.
 *
 * Mirrors TCK tests:
 * - cascadeAllMX1Test2 (CascadeType.ALL)
 * - persistMX1Test2 (CascadeType.PERSIST)
 */
public class TestCascadePersistManyToOne extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            CascadeMx1A.class,
            CascadeMx1BCascadeAll.class,
            CascadeMx1BCascadePersist.class,
            CLEAR_TABLES);
    }

    /**
     * Tests cascadeAllMX1Test2 pattern:
     * 1. Create A and B(cascade=ALL, ref to A), persist B (A cascaded)
     * 2. Remove A and B, flush
     * 3. Verify B is gone (find returns null)
     * 4. Re-persist B, flush
     * 5. Verify B is managed and b.getA1() != null
     */
    public void testCascadeAllRePersistedRemovedEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        CascadeMx1A aRef = new CascadeMx1A("2", "bean2", 2);
        CascadeMx1BCascadeAll b1 = new CascadeMx1BCascadeAll("2", "b2", 2, aRef);
        em.persist(b1);
        em.flush();

        CascadeMx1A newA1 = b1.getA1Info();
        assertNotNull("A should have been cascade-persisted", newA1);

        // Remove A first, then B
        em.remove(newA1);
        em.remove(b1);
        em.flush();

        // Verify B is removed
        CascadeMx1BCascadeAll foundB = em.find(CascadeMx1BCascadeAll.class, "2");
        assertNull("B should be removed from database", foundB);

        // Re-persist the removed B (which still has reference to A)
        em.persist(b1);
        em.flush();

        // Verify B is managed and A reference is still set
        assertTrue("B should be managed after re-persist",
            em.contains(b1));
        assertNotNull("B's A reference should not be null after re-persist",
            b1.getA1());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests persistMX1Test2 pattern (CascadeType.PERSIST):
     * 1. Create A and B(cascade=PERSIST, ref to A), persist B (A cascaded)
     * 2. Remove A and B (find B first), flush
     * 3. Verify B is gone
     * 4. Re-persist B, flush
     * 5. Verify B is managed and b.getA1() != null
     */
    public void testPersistCascadeRePersistedRemovedEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        CascadeMx1A aRef = new CascadeMx1A("2", "bean2", 2);
        CascadeMx1BCascadePersist b1 =
            new CascadeMx1BCascadePersist("2", "b2", 2, aRef);
        em.persist(b1);
        em.flush();

        CascadeMx1A newA1 = b1.getA1Info();
        assertNotNull("A should have been cascade-persisted", newA1);

        // Remove A, then find and remove B
        em.remove(newA1);
        CascadeMx1BCascadePersist foundForRemove =
            em.find(CascadeMx1BCascadePersist.class, "2");
        em.remove(foundForRemove);
        em.flush();

        // Verify B is removed
        CascadeMx1BCascadePersist foundB =
            em.find(CascadeMx1BCascadePersist.class, "2");
        assertNull("B should be removed from database", foundB);

        // Re-persist the removed B (which still has reference to A)
        em.persist(b1);
        em.flush();

        // Verify B is managed and A reference is still set
        assertTrue("B should be managed after re-persist",
            em.contains(b1));
        assertNotNull("B's A reference should not be null after re-persist",
            b1.getA1());

        em.getTransaction().commit();
        em.close();
    }
}

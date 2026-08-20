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
 * Tests cascade ALL on ManyToOne with unenhanced (runtime enhanced)
 * entities: persist, remove, re-persist.
 * Mirrors TCK cascadeAllMX1Test2 and persistMX1Test2.
 */
public class TestUnenhancedCascadeMx1 extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            UnenhancedCascadeA.class,
            UnenhancedCascadeB.class,
            CLEAR_TABLES);
    }

    /**
     * Persist B(cascade=ALL, ref to A), remove both, re-persist B.
     * Verify B is managed and b.getA1() != null.
     */
    public void testCascadeAllRePersistedRemovedEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedCascadeA aRef = new UnenhancedCascadeA("2", "bean2", 2);
        UnenhancedCascadeB b1 =
            new UnenhancedCascadeB("2", "b2", 2, aRef);
        em.persist(b1);
        em.flush();

        UnenhancedCascadeA newA1 = b1.getA1Info();
        assertNotNull("A should have been cascade-persisted", newA1);

        // Remove A first, then B
        em.remove(newA1);
        em.remove(b1);
        em.flush();

        // Verify B is removed
        UnenhancedCascadeB foundB =
            em.find(UnenhancedCascadeB.class, "2");
        assertNull("B should be removed", foundB);

        // Re-persist removed B (still has A reference)
        em.persist(b1);
        em.flush();

        assertTrue("B should be managed",
            em.contains(b1));
        assertNotNull("B's A ref should not be null",
            b1.getA1());

        em.getTransaction().commit();
        em.close();
    }
}

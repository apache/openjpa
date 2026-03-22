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

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests derived identity ex2b: @EmbeddedId with non-@Embeddable IdClass field + @MapsId.
 * The EmbeddedId (DID2bDependentId) contains a field of type DID2bEmployeeId
 * which is an @IdClass (NOT @Embeddable).
 *
 * This test uses RUNTIME ENHANCEMENT (unenhanced entities).
 * Mirrors TCK core/derivedid/ex2b/Client.DIDTest.
 */
public class TestUnenhancedDerivedIdEx2b extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedDID2bEmployeeId.class,
            UnenhancedDID2bEmployee.class,
            UnenhancedDID2bDependent.class,
            UnenhancedDID2bDependentId.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false");
    }

    public void testEx2b_EmbeddedIdWithNonEmbeddableIdClass() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        try {
            final UnenhancedDID2bEmployeeId eId1 = new UnenhancedDID2bEmployeeId("Java", "Duke");
            final UnenhancedDID2bEmployeeId eId2 = new UnenhancedDID2bEmployeeId("C", "foo");
            final UnenhancedDID2bEmployee employee1 = new UnenhancedDID2bEmployee(eId1);
            final UnenhancedDID2bEmployee employee2 = new UnenhancedDID2bEmployee(eId2);
            final UnenhancedDID2bDependent dep1 = new UnenhancedDID2bDependent(
                new UnenhancedDID2bDependentId("Obama", eId1), employee1);
            final UnenhancedDID2bDependent dep2 = new UnenhancedDID2bDependent(
                new UnenhancedDID2bDependentId("Michelle", eId1), employee1);
            final UnenhancedDID2bDependent dep3 = new UnenhancedDID2bDependent(
                new UnenhancedDID2bDependentId("John", eId2), employee2);

            em.persist(dep1);
            em.persist(dep2);
            em.persist(dep3);
            em.persist(employee1);
            em.persist(employee2);
            em.flush();

            // Find
            UnenhancedDID2bDependent newDependent = em.find(UnenhancedDID2bDependent.class,
                new UnenhancedDID2bDependentId("Obama", new UnenhancedDID2bEmployeeId("Java", "Duke")));
            assertNotNull("Should find dependent by EmbeddedId key", newDependent);
            em.refresh(newDependent);

            // Query 1
            List depList = em.createQuery(
                "Select d from UnenhancedDID2bDependent d where d.id.name='Obama' and d.emp.firstName='Java'")
                .getResultList();
            assertTrue("Query 1 should return results", depList.size() > 0);
            assertSame("Should be same managed instance", dep1, depList.get(0));

            // Query 2 (navigating into embedded id)
            List depList2 = em.createQuery(
                "Select d from UnenhancedDID2bDependent d where d.id.name='Obama' and d.id.empPK.firstName='Java'")
                .getResultList();
            assertTrue("Query 2 should return results", depList2.size() > 0);
            assertSame("Should be same managed instance", dep1, depList2.get(0));

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("ex2b test failed with exception: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}

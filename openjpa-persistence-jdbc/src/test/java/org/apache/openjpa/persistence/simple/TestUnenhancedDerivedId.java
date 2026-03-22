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
 * Tests derived identity (@IdClass, @EmbeddedId, @MapsId) with RUNTIME
 * ENHANCEMENT (unenhanced entities). Mirrors the TCK tests:
 * - core/derivedid/ex1a (DIDTest) — @IdClass + @Id @ManyToOne
 * - core/derivedid/ex1b (DIDTest) — @EmbeddedId + @MapsId
 * - core/derivedid/ex2b (DIDTest) — @EmbeddedId with non-@Embeddable IdClass field
 * - core/derivedid/ex3a (DIDTest) — @IdClass + @Id @ManyToOne, composite parent PK
 * - core/annotations/mapsid (persistMX1Test1) — same as ex1b pattern
 *
 * All entities use Unenhanced* prefix to skip build-time enhancement.
 */
public class TestUnenhancedDerivedId extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedDID1Employee.class,
            UnenhancedDID1Dependent.class,
            UnenhancedDID1DependentId.class,
            UnenhancedDID1bEmployee.class,
            UnenhancedDID1bDependent.class,
            UnenhancedDID1bDependentId.class,
            UnenhancedDID3EmployeeId.class,
            UnenhancedDID3Employee.class,
            UnenhancedDID3Dependent.class,
            UnenhancedDID3DependentId.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false");
    }

    /**
     * ex1a: @IdClass with @Id @ManyToOne, simple parent PK (long).
     * Mirrors TCK core/derivedid/ex1a/Client.DIDTest.
     */
    public void testEx1a_IdClassWithIdManyToOne() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        try {
            final UnenhancedDID1Employee employee1 = new UnenhancedDID1Employee(1L, "Duke");
            final UnenhancedDID1Employee employee2 = new UnenhancedDID1Employee(2L, "foo");

            final UnenhancedDID1Dependent dep1 = new UnenhancedDID1Dependent("Obama", employee1);
            final UnenhancedDID1Dependent dep2 = new UnenhancedDID1Dependent("Michelle", employee1);
            final UnenhancedDID1Dependent dep3 = new UnenhancedDID1Dependent("John", employee2);

            em.persist(dep1);
            em.persist(dep2);
            em.persist(dep3);
            em.persist(employee1);
            em.persist(employee2);
            em.flush();

            // Find with composite IdClass key
            UnenhancedDID1Dependent newDependent = em.find(UnenhancedDID1Dependent.class,
                new UnenhancedDID1DependentId("Obama", 1L));
            assertNotNull("Should find dependent by IdClass key", newDependent);
            em.refresh(newDependent);

            // Query
            List depList = em.createQuery(
                "Select d from UnenhancedDID1Dependent d where d.name='Obama' and d.emp.name='Duke'")
                .getResultList();
            assertTrue("Query should return results", depList.size() > 0);

            newDependent = (UnenhancedDID1Dependent) depList.get(0);
            assertSame("Should be same managed instance", dep1, newDependent);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("ex1a test failed with exception: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * ex1b: @EmbeddedId + @MapsId, simple parent PK (long).
     * Mirrors TCK core/derivedid/ex1b/Client.DIDTest.
     */
    public void testEx1b_EmbeddedIdWithMapsId() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        try {
            final UnenhancedDID1bEmployee employee1 = new UnenhancedDID1bEmployee(1L, "Duke");
            final UnenhancedDID1bEmployee employee2 = new UnenhancedDID1bEmployee(2L, "foo");

            final UnenhancedDID1bDependentId depId1 = new UnenhancedDID1bDependentId("Obama", 1L);
            final UnenhancedDID1bDependentId depId2 = new UnenhancedDID1bDependentId("Michelle", 1L);
            final UnenhancedDID1bDependentId depId3 = new UnenhancedDID1bDependentId("John", 2L);

            final UnenhancedDID1bDependent dep1 = new UnenhancedDID1bDependent(depId1, employee1);
            final UnenhancedDID1bDependent dep2 = new UnenhancedDID1bDependent(depId2, employee1);
            final UnenhancedDID1bDependent dep3 = new UnenhancedDID1bDependent(depId3, employee2);

            em.persist(dep1);
            em.persist(dep2);
            em.persist(dep3);
            em.persist(employee1);
            em.persist(employee2);
            em.flush();

            // Find with EmbeddedId key
            UnenhancedDID1bDependent newDependent = em.find(UnenhancedDID1bDependent.class, depId1);
            assertNotNull("Should find dependent by EmbeddedId key", newDependent);
            em.refresh(newDependent);

            // Query
            List depList = em.createQuery(
                "Select d from UnenhancedDID1bDependent d where d.id.name='Obama' and d.emp.name='Duke'")
                .getResultList();
            assertTrue("Query should return results", depList.size() > 0);

            newDependent = (UnenhancedDID1bDependent) depList.get(0);
            assertSame("Should be same managed instance", dep1, newDependent);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("ex1b test failed with exception: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * ex3a: @IdClass with @Id @ManyToOne, composite parent PK (@EmbeddedId).
     * Mirrors TCK core/derivedid/ex3a/Client.DIDTest.
     */
    public void testEx3a_IdClassWithCompositeParentPK() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        try {
            final UnenhancedDID3EmployeeId eId1 = new UnenhancedDID3EmployeeId("Java", "Duke");
            final UnenhancedDID3EmployeeId eId2 = new UnenhancedDID3EmployeeId("C", "foo");

            final UnenhancedDID3Employee employee1 = new UnenhancedDID3Employee(eId1);
            final UnenhancedDID3Employee employee2 = new UnenhancedDID3Employee(eId2);

            final UnenhancedDID3DependentId depId1 = new UnenhancedDID3DependentId("Obama", eId1);
            final UnenhancedDID3DependentId depId2 = new UnenhancedDID3DependentId("Michelle", eId1);
            final UnenhancedDID3DependentId depId3 = new UnenhancedDID3DependentId("John", eId2);

            final UnenhancedDID3Dependent dep1 = new UnenhancedDID3Dependent(depId1, employee1);
            final UnenhancedDID3Dependent dep2 = new UnenhancedDID3Dependent(depId2, employee1);
            final UnenhancedDID3Dependent dep3 = new UnenhancedDID3Dependent(depId3, employee2);

            em.persist(dep1);
            em.persist(dep2);
            em.persist(dep3);
            em.persist(employee1);
            em.persist(employee2);
            em.flush();

            // Find
            UnenhancedDID3Dependent newDependent = em.find(UnenhancedDID3Dependent.class, depId1);
            assertNotNull("Should find dependent by IdClass key", newDependent);
            em.refresh(newDependent);

            // Query
            List depList = em.createQuery(
                "Select d from UnenhancedDID3Dependent d where d.name2='Obama' and d.emp.empId.firstName='Java'")
                .getResultList();
            assertTrue("Query should return results", depList.size() > 0);

            newDependent = (UnenhancedDID3Dependent) depList.get(0);
            assertSame("Should be same managed instance", dep1, newDependent);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("ex3a test failed with exception: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * mapsid: @MapsId ManyToOne — same pattern as ex1b.
     * Mirrors TCK core/annotations/mapsid/Client.persistMX1Test1.
     */
    public void testMapsId_PersistMX1() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        try {
            final UnenhancedDID1bEmployee employee1 = new UnenhancedDID1bEmployee(1L, "Duke");
            final UnenhancedDID1bEmployee employee2 = new UnenhancedDID1bEmployee(2L, "foo");

            final UnenhancedDID1bDependent dep1 = new UnenhancedDID1bDependent(
                new UnenhancedDID1bDependentId("Obama", 1L), employee1);
            final UnenhancedDID1bDependent dep2 = new UnenhancedDID1bDependent(
                new UnenhancedDID1bDependentId("Michelle", 1L), employee1);
            final UnenhancedDID1bDependent dep3 = new UnenhancedDID1bDependent(
                new UnenhancedDID1bDependentId("John", 2L), employee2);

            em.persist(dep1);
            em.persist(dep2);
            em.persist(dep3);
            em.persist(employee1);
            em.persist(employee2);
            em.flush();

            // Find
            UnenhancedDID1bDependent newDependent = em.find(UnenhancedDID1bDependent.class,
                new UnenhancedDID1bDependentId("Obama", 1L));
            assertNotNull("Should find dependent", newDependent);
            em.refresh(newDependent);

            // Query
            List depList = em.createQuery(
                "Select d from UnenhancedDID1bDependent d where d.id.name='Obama' and d.emp.name='Duke'")
                .getResultList();

            UnenhancedDID1bDependent resultDep = null;
            if (depList.size() > 0) {
                resultDep = (UnenhancedDID1bDependent) depList.get(0);
            }

            assertSame("Should be same managed instance", dep1, resultDep);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            fail("mapsid test failed with exception: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}

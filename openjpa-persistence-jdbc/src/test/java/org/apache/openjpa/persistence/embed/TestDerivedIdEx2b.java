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
package org.apache.openjpa.persistence.embed;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests derived identity ex2b (JPA 2.4.1.3): @EmbeddedId with non-@Embeddable
 * IdClass field + @MapsId. Mirrors TCK core/derivedid/ex2b/Client.DIDTest.
 *
 * Uses BUILD-TIME enhanced entities.
 */
public class TestDerivedIdEx2b extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            DID2bEmployee.class,
            DID2bDependent.class,
            DID2bDependentId.class);
    }

    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        DID2bEmployeeId eId1 = new DID2bEmployeeId("Java", "Duke");
        DID2bEmployee emp1 = new DID2bEmployee(eId1);
        DID2bDependent dep1 = new DID2bDependent(
            new DID2bDependentId("Obama", eId1), emp1);

        em.persist(emp1);
        em.persist(dep1);
        em.flush();

        // Find by composite EmbeddedId
        DID2bDependent found = em.find(DID2bDependent.class,
            new DID2bDependentId("Obama", new DID2bEmployeeId("Java", "Duke")));
        assertNotNull("Should find dependent by EmbeddedId key", found);

        em.getTransaction().commit();
        em.close();
    }

    public void testQueryViaRelation() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        DID2bEmployeeId eId1 = new DID2bEmployeeId("Java", "Duke");
        DID2bEmployee emp1 = new DID2bEmployee(eId1);
        DID2bDependent dep1 = new DID2bDependent(
            new DID2bDependentId("Obama", eId1), emp1);

        em.persist(emp1);
        em.persist(dep1);
        em.flush();

        // Query via relationship: d.emp.firstName
        List depList = em.createQuery(
            "Select d from DID2bDependent d where d.id.name='Obama' and d.emp.firstName='Java'")
            .getResultList();
        assertTrue("Query via relation should return results", depList.size() > 0);

        em.getTransaction().commit();
        em.close();
    }

    public void testQueryViaEmbeddedIdPath() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        DID2bEmployeeId eId1 = new DID2bEmployeeId("Java", "Duke");
        DID2bEmployee emp1 = new DID2bEmployee(eId1);
        DID2bDependent dep1 = new DID2bDependent(
            new DID2bDependentId("Obama", eId1), emp1);

        em.persist(emp1);
        em.persist(dep1);
        em.flush();

        // Query navigating into the embedded id: d.id.empPK.firstName
        List depList = em.createQuery(
            "Select d from DID2bDependent d where d.id.name='Obama' and d.id.empPK.firstName='Java'")
            .getResultList();
        assertTrue("Query via embedded id path should return results", depList.size() > 0);

        em.getTransaction().commit();
        em.close();
    }
}

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
 * Regression test for OPENJPA-2968: the @MapsId columns of a non-@Embeddable
 * @IdClass field inside an @EmbeddedId must be matched to the members of the
 * id class by NAME, not by the order in which the id class happens to declare
 * its fields. DID2cEmployeeId declares lastName before firstName, the reverse
 * of the @JoinColumns order of DID2cDependent.
 */
public class TestDerivedIdEx2c extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            DID2cEmployee.class,
            DID2cDependent.class,
            DID2cDependentId.class);
    }

    private DID2cDependent create(EntityManager em) {
        DID2cEmployeeId eId = new DID2cEmployeeId("Sam", "Vaughn");
        DID2cEmployee emp = new DID2cEmployee(eId);
        DID2cDependent dep = new DID2cDependent(
            new DID2cDependentId("Joe", eId), emp);
        em.persist(emp);
        em.persist(dep);
        em.flush();
        return dep;
    }

    public void testFindDoesNotDependOnIdClassFieldOrder() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        create(em);
        em.clear();

        DID2cDependent found = em.find(DID2cDependent.class,
            new DID2cDependentId("Joe", new DID2cEmployeeId("Sam", "Vaughn")));
        assertNotNull("Should find dependent by EmbeddedId key", found);
        assertNotNull(found.getId().getEmpPK());
        assertEquals("Sam", found.getId().getEmpPK().getFirstName());
        assertEquals("Vaughn", found.getId().getEmpPK().getLastName());

        em.getTransaction().commit();
        em.close();
    }

    public void testQueryViaEmbeddedIdPath() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        create(em);

        List<?> depList = em.createQuery(
            "select d from DID2cDependent d where d.id.name = :n "
            + "and d.id.empPK.firstName = :fn")
            .setParameter("n", "Joe")
            .setParameter("fn", "Sam")
            .getResultList();
        assertEquals(1, depList.size());

        em.getTransaction().commit();
        em.close();
    }

    public void testQueryViaRelation() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        create(em);

        List<?> depList = em.createQuery(
            "select d from DID2cDependent d where d.emp.firstName = :fn")
            .setParameter("fn", "Sam")
            .getResultList();
        assertEquals(1, depList.size());

        em.getTransaction().commit();
        em.close();
    }
}

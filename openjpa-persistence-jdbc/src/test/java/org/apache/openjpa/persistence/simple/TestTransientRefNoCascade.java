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

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * A relation with no cascade that points at a transient instance must be
 * reported at flush time, while a relation that points at a detached
 * instance stays tolerated (see OPENJPA-2940). Transient instances without
 * an assigned identity must not cause a datastore lookup, because an
 * instance without identity can never have been persisted.
 */
public class TestTransientRefNoCascade extends SQLListenerTestCase {

    @Override
    public void setUp() {
        setUp(UnenhancedNCDepartment.class,
              UnenhancedNCAssignedDepartment.class,
              UnenhancedNCEmployee.class,
              CLEAR_TABLES,
              "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * An instance without an assigned identity was never persisted, so the
     * flush must fail and must not probe the datastore for it.
     */
    public void testTransientWithoutIdentityIsRejectedWithoutLookup() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedNCEmployee emp = new UnenhancedNCEmployee(1L, "Smith");
            emp.setDepartment(new UnenhancedNCDepartment(null, "Ghost"));
            em.persist(emp);
            sql.clear();
            try {
                em.flush();
                fail("Flush must not accept a transient reference on a "
                    + "relation without cascade");
            } catch (InvalidStateException ise) {
                assertNotNull(ise.getFailedObject());
            }
            assertEquals("No datastore lookup must be done for an instance "
                + "without an assigned identity: " + sql,
                0, countSelects("TRNSREF_DEPT"));
        }
    }

    /**
     * An instance with an identity that has no store record was never
     * persisted either, so the flush must still fail after the lookup.
     */
    public void testTransientWithIdentityIsRejected() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedNCEmployee emp = new UnenhancedNCEmployee(2L, "Jones");
            emp.setDepartment(new UnenhancedNCDepartment(4711L, "Ghost"));
            em.persist(emp);
            sql.clear();
            try {
                em.flush();
                fail("Flush must not accept a transient reference on a "
                    + "relation without cascade");
            } catch (InvalidStateException ise) {
                assertNotNull(ise.getFailedObject());
            }
            assertEquals("The identity must be looked up exactly once: " + sql,
                1, countSelects("TRNSREF_DEPT"));
        }
        assertNull("The employee must not have been written",
            find(UnenhancedNCEmployee.class, 2L));
    }

    /**
     * An instance of an entity with an application assigned identity that
     * holds no identity at all must be rejected without a lookup, even if a
     * store record exists for the default value of the identity type.
     */
    public void testTransientWithoutAssignedIdentityIsRejectedWithoutLookup() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new UnenhancedNCAssignedDepartment(0L, "Zero"));
            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedNCEmployee emp = new UnenhancedNCEmployee(5L, "Brown");
            emp.setAssignedDepartment(
                new UnenhancedNCAssignedDepartment(null, "Ghost"));
            em.persist(emp);
            sql.clear();
            try {
                em.flush();
                fail("Flush must not accept a transient reference on a "
                    + "relation without cascade");
            } catch (InvalidStateException ise) {
                assertNotNull(ise.getFailedObject());
            }
            assertEquals("No datastore lookup must be done for an instance "
                + "without an assigned identity: " + sql,
                0, countSelects("TRNSREF_ADEPT"));
        }
        assertNull("The employee must not have been written",
            find(UnenhancedNCEmployee.class, 5L));
    }

    /**
     * A detached instance whose application assigned identity is the default
     * value of its type stays allowed: the identity is assigned, only the
     * store record settles the question.
     */
    public void testDetachedReferenceWithDefaultIdentityIsTolerated() {
        UnenhancedNCAssignedDepartment dept =
            new UnenhancedNCAssignedDepartment(0L, "Zero");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(dept);
            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedNCEmployee emp = new UnenhancedNCEmployee(6L, "Clark");
            emp.setAssignedDepartment(dept);
            em.persist(emp);
            em.flush();
            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedNCEmployee emp = em.find(UnenhancedNCEmployee.class, 6L);
            assertNotNull(emp);
            assertNotNull("The detached reference must have been written",
                emp.getAssignedDepartment());
            assertEquals(Long.valueOf(0L),
                emp.getAssignedDepartment().getId());
        }
    }

    /**
     * A detached instance stays allowed on a relation without cascade.
     */
    public void testDetachedReferenceIsTolerated() {
        UnenhancedNCDepartment dept = new UnenhancedNCDepartment(null, "Marketing");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(dept);
            em.getTransaction().commit();
        }
        Long deptId = dept.getId();
        assertNotNull(deptId);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedNCEmployee emp = new UnenhancedNCEmployee(3L, "Miller");
            emp.setDepartment(dept);
            em.persist(emp);
            em.flush();
            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedNCEmployee emp = em.find(UnenhancedNCEmployee.class, 3L);
            assertNotNull(emp);
            assertNotNull("The detached reference must have been written",
                emp.getDepartment());
            assertEquals(deptId, emp.getDepartment().getId());
        }
    }

    private <T> T find(Class<T> cls, Object id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(cls, id);
        }
    }

    private int countSelects(String table) {
        int count = 0;
        for (String statement : sql) {
            if (statement.startsWith("SELECT") && statement.contains(table))
                count++;
        }
        return count;
    }
}

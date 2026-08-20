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

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for TCK failure: joinColumnUpdatable
 *
 * When a @ManyToOne field has @JoinColumn(insertable=false, updatable=false),
 * setting the field to a detached/unmanaged entity and flushing should
 * NOT throw an InvalidStateException about unmanaged objects, because
 * the FK column won't be written to the DB.
 *
 * Uses runtime enhancement (unenhanced entities) to mirror TCK behavior.
 */
public class TestJoinColumnUpdatable extends SingleEMFTestCase {

    private UnenhancedJCDepartment dept1;
    private UnenhancedJCDepartment dept2;

    @Override
    public void setUp() {
        setUp(UnenhancedJCDepartment.class, UnenhancedJCEmployee.class,
              CLEAR_TABLES,
              "openjpa.RuntimeUnenhancedClasses", "supported");

        // Create initial data
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            dept1 = new UnenhancedJCDepartment(1, "Marketing");
            dept2 = new UnenhancedJCDepartment(2, "Administration");
            em.persist(dept1);
            em.persist(dept2);

            UnenhancedJCEmployee emp = new UnenhancedJCEmployee(6, "Smith");
            emp.setDepartment(dept1);
            em.persist(emp);
            em.flush();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Mirrors TCK joinColumnUpdatable test for Employee2:
     * @JoinColumn(insertable=false, updatable=false)
     *
     * Find managed employee, set department to a detached reference,
     * merge+flush. Since updatable=false, the FK should NOT be updated
     * and flush should NOT fail with "unmanaged object" error.
     */
    public void testJoinColumnUpdatableFalse() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.clear();

            UnenhancedJCEmployee emp = em.find(UnenhancedJCEmployee.class, 6);
            assertNotNull("Employee should be found", emp);

            // Set department to dept2 (which is detached - from prior tx)
            emp.setDepartment(dept2);
            em.merge(emp);
            em.flush();

            em.clear();

            // Since updatable=false, the FK should NOT have been updated
            // Re-find and check department is still null (since insertable
            // was also false, the FK was never written)
            emp = em.find(UnenhancedJCEmployee.class, 6);
            assertNotNull("Employee should still exist", emp);
            UnenhancedJCDepartment dept = emp.getDepartment();
            // With insertable=false, the FK_DEPT column was never set,
            // so the department should be null after re-load
            assertNull("Department should be null since insertable=false "
                + "(FK was never written)", dept);

            em.getTransaction().commit();
        } catch (Exception e) {
            fail("Should not throw exception for unmanaged object on "
                + "non-updatable JoinColumn: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}

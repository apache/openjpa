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
package org.apache.openjpa.persistence.mapkey;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that @OneToMany(mappedBy="department") resolves correctly
 * when Employee has @Id on both field and getter + @ManyToOne on getter.
 * This is the pattern used by TCK EntityGraph Employee/Department.
 */
public class TestUnenhancedMappedBy extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            UnenhancedMBEmployee.class,
            UnenhancedMBDepartment.class,
            CLEAR_TABLES);
    }

    /**
     * Simple persist + find with @OneToMany(mappedBy) as List.
     * Verifies mappedBy="department" resolves to the @ManyToOne getter.
     */
    public void testMappedByWithDualAnnotatedId() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMBDepartment dept =
            new UnenhancedMBDepartment(1, "Engineering");
        UnenhancedMBEmployee emp =
            new UnenhancedMBEmployee(1, "John", "Doe");
        emp.setSalary(50000f);
        emp.setDepartment(dept);

        em.persist(dept);
        em.persist(emp);
        em.getTransaction().commit();
        em.close();

        // Verify basic find works
        em = emf.createEntityManager();
        UnenhancedMBDepartment found =
            em.find(UnenhancedMBDepartment.class, 1);
        assertNotNull("Department should be found", found);
        assertEquals("Engineering", found.getName());
        em.close();
    }
}

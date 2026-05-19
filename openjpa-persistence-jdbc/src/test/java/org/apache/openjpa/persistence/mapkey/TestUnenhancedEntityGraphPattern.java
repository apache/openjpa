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

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Reproduces TCK EntityGraph pattern with runtime enhancement:
 * Employee has @Id on both field and getter + @ManyToOne on getter,
 * Department has @OneToMany(mappedBy) @MapKey on getter.
 */
public class TestUnenhancedEntityGraphPattern extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            UnenhancedEGEmployee.class, UnenhancedEGDepartment.class,
            CLEAR_TABLES);
    }

    public void testPersistAndFindWithMappedByMap() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedEGDepartment dept =
            new UnenhancedEGDepartment(1, "Engineering");
        UnenhancedEGEmployee emp1 =
            new UnenhancedEGEmployee(1, "John", "Doe");
        emp1.setSalary(50000f);
        emp1.setDepartment(dept);
        UnenhancedEGEmployee emp2 =
            new UnenhancedEGEmployee(2, "Jane", "Smith");
        emp2.setSalary(60000f);
        emp2.setDepartment(dept);

        em.persist(dept);
        em.persist(emp1);
        em.persist(emp2);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        UnenhancedEGDepartment found =
            em.find(UnenhancedEGDepartment.class, 1);
        assertNotNull("Department should be found", found);
        Map<String, UnenhancedEGEmployee> map =
            found.getLastNameEmployees();
        assertNotNull("Map should not be null", map);
        assertEquals("Should have 2 employees", 2, map.size());
        assertNotNull("Should find by lastName Doe", map.get("Doe"));
        assertNotNull("Should find by lastName Smith", map.get("Smith"));
        em.close();
    }
}

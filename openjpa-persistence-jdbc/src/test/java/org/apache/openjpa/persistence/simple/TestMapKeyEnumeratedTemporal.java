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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for @MapKeyEnumerated and @MapKeyTemporal on
 * @OneToMany(mappedBy) Map fields.
 *
 * Reproduces TCK mapkeyenumerated and mapkeytemporal test patterns:
 * - Multiple entities sharing the same table with different
 *   @MapKeyEnumerated types (ORDINAL vs STRING) on the same column
 * - @OneToMany(mappedBy) with @MapKeyTemporal(DATE) for Date map keys
 */
public class TestMapKeyEnumeratedTemporal extends SingleEMFTestCase {

    /**
     * Test that @MapKeyEnumerated(STRING) works on a OneToMany(mappedBy)
     * Map field, even when another entity shares the same employee table
     * with @MapKeyEnumerated (ORDINAL) on the same column.
     *
     * This mirrors the TCK mapkeyenumerated test where Department (STRING)
     * and Department2 (ORDINAL) both have employees on EMP_MAPKEYCOL
     * with OFFICE_ID column.
     */
    public void testMapKeyEnumeratedWithSharedTable() {
        setUp(DROP_TABLES,
            MkeDepartment.class,
            MkeDepartmentOrdinal.class,
            MkeEmployee.class,
            MkeEmployeeOrdinal.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Create STRING enum department with employees
        MkeDepartment dept = new MkeDepartment(1, "Engineering");
        MkeEmployee emp1 = new MkeEmployee(1, "Smith");
        emp1.setDepartment(dept);
        MkeEmployee emp2 = new MkeEmployee(2, "Jones");
        emp2.setDepartment(dept);
        MkeEmployee emp3 = new MkeEmployee(3, "Adams");
        emp3.setDepartment(dept);

        Map<MapKeyEnum, MkeEmployee> empMap = new HashMap<>();
        empMap.put(MapKeyEnum.ONE, emp1);
        empMap.put(MapKeyEnum.TWO, emp2);
        empMap.put(MapKeyEnum.THREE, emp3);
        dept.setLastNameEmployees(empMap);

        em.persist(dept);
        em.persist(emp1);
        em.persist(emp2);
        em.persist(emp3);
        em.merge(dept);

        // Create ORDINAL enum department with employees
        MkeDepartmentOrdinal dept2 = new MkeDepartmentOrdinal(2, "Sales");
        MkeEmployeeOrdinal emp4 = new MkeEmployeeOrdinal(4, "Brown");
        emp4.setDepartment(dept2);
        MkeEmployeeOrdinal emp5 = new MkeEmployeeOrdinal(5, "White");
        emp5.setDepartment(dept2);

        Map<MapKeyEnum, MkeEmployeeOrdinal> empMap2 = new HashMap<>();
        empMap2.put(MapKeyEnum.ONE, emp4);
        empMap2.put(MapKeyEnum.TWO, emp5);
        dept2.setLastNameEmployees(empMap2);

        em.persist(dept2);
        em.persist(emp4);
        em.persist(emp5);
        em.merge(dept2);

        em.getTransaction().commit();
        em.clear();

        // Verify STRING enum department
        MkeDepartment found = em.find(MkeDepartment.class, 1);
        assertNotNull("Department should be found", found);
        assertNotNull("Map should not be null", found.getLastNameEmployees());
        assertEquals("Map should have 3 entries", 3,
            found.getLastNameEmployees().size());
        assertNotNull("Key ONE should exist",
            found.getLastNameEmployees().get(MapKeyEnum.ONE));
        assertEquals("Smith", found.getLastNameEmployees()
            .get(MapKeyEnum.ONE).getLastName());

        em.close();
    }

    /**
     * Test that @MapKeyTemporal(DATE) works on a OneToMany(mappedBy)
     * Map field. Verifies Date map keys are correctly stored and
     * retrieved.
     */
    public void testMapKeyTemporalOneToMany() {
        setUp(DROP_TABLES,
            MktDepartment.class,
            MktEmployee.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MktDepartment dept = new MktDepartment(1, "Engineering");
        MktEmployee emp1 = new MktEmployee(1, "Smith");
        emp1.setDepartment(dept);
        MktEmployee emp2 = new MktEmployee(2, "Jones");
        emp2.setDepartment(dept);

        Date date1 = new Date(1000000000000L); // 2001-09-08
        Date date2 = new Date(1100000000000L); // 2004-11-09

        Map<Date, MktEmployee> empMap = new HashMap<>();
        empMap.put(date1, emp1);
        empMap.put(date2, emp2);
        dept.setLastNameEmployees(empMap);

        em.persist(dept);
        em.persist(emp1);
        em.persist(emp2);
        em.merge(dept);

        em.getTransaction().commit();
        em.clear();

        // Verify temporal department
        MktDepartment found = em.find(MktDepartment.class, 1);
        assertNotNull("Department should be found", found);
        assertNotNull("Map should not be null", found.getLastNameEmployees());
        assertEquals("Map should have 2 entries", 2,
            found.getLastNameEmployees().size());

        em.close();
    }

    /**
     * Test @MapKeyEnumerated(STRING) on @ElementCollection
     * Map field with embeddable values.
     */
    public void testMapKeyEnumeratedElementCollection() {
        setUp(DROP_TABLES,
            MapKeyEnumEntity.class,
            MapValueEmbeddable.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MapKeyEnumEntity dept = new MapKeyEnumEntity(1, "Engineering");
        Map<MapKeyEnum, MapValueEmbeddable> employees = new HashMap<>();
        employees.put(MapKeyEnum.ONE,
            new MapValueEmbeddable(100, "Alice"));
        employees.put(MapKeyEnum.TWO,
            new MapValueEmbeddable(200, "Bob"));
        dept.setLastNameEmployees(employees);
        em.persist(dept);

        em.getTransaction().commit();
        em.clear();

        MapKeyEnumEntity found = em.find(MapKeyEnumEntity.class, 1);
        assertNotNull(found);
        assertNotNull(found.getLastNameEmployees());
        assertEquals(2, found.getLastNameEmployees().size());

        MapValueEmbeddable alice = found.getLastNameEmployees().get(
            MapKeyEnum.ONE);
        assertNotNull(alice);
        assertEquals(100, alice.employeeId);
        assertEquals("Alice", alice.employeeName);

        em.close();
    }

    /**
     * Test @MapKeyTemporal(DATE) on @ElementCollection
     * Map field with embeddable values.
     */
    public void testMapKeyTemporalElementCollection() {
        setUp(DROP_TABLES,
            MapKeyTemporalEntity.class,
            MapValueEmbeddable.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MapKeyTemporalEntity dept = new MapKeyTemporalEntity(1, "Sales");
        Map<Date, MapValueEmbeddable> employees = new HashMap<>();
        Date date1 = new Date(1000000000000L);
        Date date2 = new Date(1100000000000L);
        employees.put(date1,
            new MapValueEmbeddable(300, "Charlie"));
        employees.put(date2,
            new MapValueEmbeddable(400, "Diana"));
        dept.setLastNameEmployees(employees);
        em.persist(dept);

        em.getTransaction().commit();
        em.clear();

        MapKeyTemporalEntity found = em.find(MapKeyTemporalEntity.class, 1);
        assertNotNull(found);
        assertNotNull(found.getLastNameEmployees());
        assertEquals(2, found.getLastNameEmployees().size());

        em.close();
    }

    /**
     * Test @MapKeyTemporal(DATE) with multiple entities sharing the same
     * table. Mirrors exact TCK mapkeytemporal layout where Department
     * and Department2 both map to DEPARTMENT2, and Employee and Employee2
     * both map to EMP_MAPKEYCOL2.
     */
    public void testMapKeyTemporalSharedTable() {
        setUp(DROP_TABLES,
            MktDeptShared.class,
            MktDeptShared2.class,
            MktEmpShared.class,
            MktEmpShared2.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Create Department (type 1) with employees
        MktDeptShared dept1 = new MktDeptShared(1, "Engineering");
        MktEmpShared emp1 = new MktEmpShared(1, "Smith");
        emp1.setDepartment(dept1);
        MktEmpShared emp2 = new MktEmpShared(2, "Jones");
        emp2.setDepartment(dept1);

        Date date1 = new Date(1000000000000L);
        Date date2 = new Date(1100000000000L);

        Map<Date, MktEmpShared> empMap1 = new HashMap<>();
        empMap1.put(date1, emp1);
        empMap1.put(date2, emp2);
        dept1.setLastNameEmployees(empMap1);

        em.persist(dept1);
        em.persist(emp1);
        em.persist(emp2);

        // Create Department2 (type 2, shared table) with employees
        MktDeptShared2 dept2 = new MktDeptShared2(5, "Sales");
        MktEmpShared2 emp3 = new MktEmpShared2(3, "Brown");
        emp3.setDepartment(dept2);
        MktEmpShared2 emp4 = new MktEmpShared2(4, "White");
        emp4.setDepartment(dept2);

        Date date3 = new Date(1200000000000L);
        Date date4 = new Date(1300000000000L);

        Map<Date, MktEmpShared2> empMap2 = new HashMap<>();
        empMap2.put(date3, emp3);
        empMap2.put(date4, emp4);
        dept2.setLastNameEmployees(empMap2);

        em.persist(dept2);
        em.persist(emp3);
        em.persist(emp4);

        // Merge departments to populate map key columns
        em.merge(dept1);
        em.merge(dept2);

        em.getTransaction().commit();
        em.clear();

        // Verify Department 1
        MktDeptShared found1 = em.find(MktDeptShared.class, 1);
        assertNotNull("Department1 should be found", found1);
        assertNotNull("Map should not be null", found1.getLastNameEmployees());
        assertEquals("Map should have 2 entries", 2,
            found1.getLastNameEmployees().size());

        em.close();
    }
}

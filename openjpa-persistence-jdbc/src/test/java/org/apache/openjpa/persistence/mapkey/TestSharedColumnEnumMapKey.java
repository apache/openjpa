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
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that ORDINAL and STRING @MapKeyEnumerated mappings can share
 * the same table and column. This mirrors the JPA 3.2 TCK scenario
 * where Department (STRING) and Department2 (ORDINAL) both map to
 * EMP_MAPKEYCOL.OFFICE_ID.
 *
 * When buildSchema runs, the column must remain VARCHAR (for STRING)
 * and the ORDINAL handler must correctly read/write ordinal values
 * as strings in the VARCHAR column.
 */
public class TestSharedColumnEnumMapKey extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(SharedColDeptOrdinal.class,
              SharedColEmpOrdinal.class,
              SharedColDeptString.class,
              SharedColEmpString.class,
              "openjpa.RuntimeUnenhancedClasses", "supported",
              DROP_TABLES);
    }

    public void testOrdinalMapKeyPersistAndLoad() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        SharedColDeptOrdinal dept = new SharedColDeptOrdinal();
        dept.setName("OrdDept");

        SharedColEmpOrdinal emp0 = new SharedColEmpOrdinal();
        emp0.setName("EmpOrd0");
        emp0.setDepartment(dept);

        SharedColEmpOrdinal emp1 = new SharedColEmpOrdinal();
        emp1.setName("EmpOrd1");
        emp1.setDepartment(dept);

        dept.getEmployees().put(SharedColOffices.OFF000, emp0);
        dept.getEmployees().put(SharedColOffices.OFF001, emp1);

        em.persist(dept);
        em.persist(emp0);
        em.persist(emp1);

        em.getTransaction().commit();

        long deptId = dept.getId();
        long emp0Id = emp0.getId();

        em.clear();

        // Verify via native SQL that ordinal values are stored
        Query nq = em.createNativeQuery(
            "SELECT OFFICE_ID FROM SC_EMP WHERE ID = ?1");
        nq.setParameter(1, emp0Id);
        List<?> results = nq.getResultList();
        assertFalse("Expected result from native query",
            results.isEmpty());
        String storedVal = results.get(0).toString().trim();
        assertEquals("Ordinal value should be stored as '0'",
            "0", storedVal);

        // Verify loading back through JPA
        SharedColDeptOrdinal loaded = em.find(
            SharedColDeptOrdinal.class, deptId);
        assertNotNull(loaded);
        Map<SharedColOffices, SharedColEmpOrdinal> emps =
            loaded.getEmployees();
        assertEquals(2, emps.size());
        assertNotNull("OFF000 key should be present",
            emps.get(SharedColOffices.OFF000));
        assertNotNull("OFF001 key should be present",
            emps.get(SharedColOffices.OFF001));
        assertEquals("EmpOrd0",
            emps.get(SharedColOffices.OFF000).getName());

        em.close();
    }

    public void testStringMapKeyPersistAndLoad() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        SharedColDeptString dept = new SharedColDeptString();
        dept.setName("StrDept");

        SharedColEmpString emp0 = new SharedColEmpString();
        emp0.setName("EmpStr0");
        emp0.setDepartment(dept);

        SharedColEmpString emp1 = new SharedColEmpString();
        emp1.setName("EmpStr1");
        emp1.setDepartment(dept);

        dept.getEmployees().put(SharedColOffices.OFF002, emp0);
        dept.getEmployees().put(SharedColOffices.OFF003, emp1);

        em.persist(dept);
        em.persist(emp0);
        em.persist(emp1);

        em.getTransaction().commit();

        long deptId = dept.getId();
        long emp0Id = emp0.getId();

        em.clear();

        // Verify via native SQL that string names are stored
        Query nq = em.createNativeQuery(
            "SELECT OFFICE_ID FROM SC_EMP WHERE ID = ?1");
        nq.setParameter(1, emp0Id);
        List<?> results = nq.getResultList();
        assertFalse("Expected result from native query",
            results.isEmpty());
        String storedVal = results.get(0).toString().trim();
        assertEquals("String value should be stored as enum name",
            "OFF002", storedVal);

        // Verify loading back through JPA
        SharedColDeptString loaded = em.find(
            SharedColDeptString.class, deptId);
        assertNotNull(loaded);
        Map<SharedColOffices, SharedColEmpString> emps =
            loaded.getEmployees();
        assertEquals(2, emps.size());
        assertNotNull("OFF002 key should be present",
            emps.get(SharedColOffices.OFF002));
        assertNotNull("OFF003 key should be present",
            emps.get(SharedColOffices.OFF003));
        assertEquals("EmpStr0",
            emps.get(SharedColOffices.OFF002).getName());

        em.close();
    }

    public void testBothTypesCoexist() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Persist ordinal department
        SharedColDeptOrdinal ordDept = new SharedColDeptOrdinal();
        ordDept.setName("OrdDeptBoth");

        SharedColEmpOrdinal ordEmp = new SharedColEmpOrdinal();
        ordEmp.setName("OrdEmpBoth");
        ordEmp.setDepartment(ordDept);
        ordDept.getEmployees().put(SharedColOffices.OFF000, ordEmp);

        em.persist(ordDept);
        em.persist(ordEmp);

        // Persist string department
        SharedColDeptString strDept = new SharedColDeptString();
        strDept.setName("StrDeptBoth");

        SharedColEmpString strEmp = new SharedColEmpString();
        strEmp.setName("StrEmpBoth");
        strEmp.setDepartment(strDept);
        strDept.getEmployees().put(SharedColOffices.OFF004, strEmp);

        em.persist(strDept);
        em.persist(strEmp);

        em.getTransaction().commit();

        long ordDeptId = ordDept.getId();
        long strDeptId = strDept.getId();

        em.clear();

        // Load both back and verify
        SharedColDeptOrdinal loadedOrd = em.find(
            SharedColDeptOrdinal.class, ordDeptId);
        assertNotNull(loadedOrd);
        assertEquals(1, loadedOrd.getEmployees().size());
        assertNotNull("Ordinal OFF000 key should be present",
            loadedOrd.getEmployees().get(SharedColOffices.OFF000));

        SharedColDeptString loadedStr = em.find(
            SharedColDeptString.class, strDeptId);
        assertNotNull(loadedStr);
        assertEquals(1, loadedStr.getEmployees().size());
        assertNotNull("String OFF004 key should be present",
            loadedStr.getEmployees().get(SharedColOffices.OFF004));

        em.close();
    }
}

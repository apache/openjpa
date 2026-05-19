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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for @MapKeyEnumerated and @MapKeyTemporal annotations.
 * Reproduces TCK failures from mapkeyenumerated and mapkeytemporal tests.
 * Uses runtime enhancement (subclassing mode) to match TCK behavior.
 */
public class TestMapKeyEnumeratedTemporal extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
              CLEAR_TABLES,
              MKEDepartment.class, MKEEmployee.class,
              MKEDepartment4.class, MKEmbeddedEmployee.class,
              MKTDepartment.class, MKTEmployee.class,
              MKTDepartment4.class, MKTEmbeddedEmployee.class);
    }

    /**
     * Tests @MapKeyEnumerated(EnumType.STRING) on a OneToMany map.
     * Mirrors TCK mapKeyEnumeratedTest.
     */
    public void testMapKeyEnumerated() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MKEDepartment dept = new MKEDepartment(1, "Marketing");
        em.persist(dept);

        MKEEmployee emp1 = new MKEEmployee(1, "Alan", "Frechette");
        emp1.setDepartment(dept);
        MKEEmployee emp2 = new MKEEmployee(3, "Shelly", "McGowan");
        emp2.setDepartment(dept);
        MKEEmployee emp3 = new MKEEmployee(5, "Stephen", "DMilla");
        emp3.setDepartment(dept);

        em.persist(emp1);
        em.persist(emp2);
        em.persist(emp3);

        Map<MKEOffices, MKEEmployee> link = new HashMap<>();
        link.put(MKEOffices.OFF000, emp1);
        link.put(MKEOffices.OFF002, emp2);
        link.put(MKEOffices.OFF004, emp3);
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Now read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        MKEEmployee foundEmp = em.find(MKEEmployee.class, 1);
        assertNotNull("Employee should be found", foundEmp);
        MKEDepartment foundDept = foundEmp.getDepartment();
        assertNotNull("Department should be found", foundDept);

        Map<MKEOffices, MKEEmployee> emps = foundDept.getLastNameEmployees();
        assertNotNull("Map should not be null", emps);
        assertEquals("Map should have 3 entries", 3, emps.size());

        Set<MKEOffices> expectedKeys = new HashSet<>();
        expectedKeys.add(MKEOffices.OFF000);
        expectedKeys.add(MKEOffices.OFF002);
        expectedKeys.add(MKEOffices.OFF004);

        Set<MKEOffices> actualKeys = emps.keySet();
        assertTrue("Keys should match expected",
            expectedKeys.containsAll(actualKeys)
            && actualKeys.containsAll(expectedKeys));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests @MapKeyEnumerated(EnumType.STRING) on an ElementCollection map
     * with @Transient on the field and @ElementCollection on the getter
     * (mixed access pattern). Mirrors TCK elementCollectionTest for
     * mapkeyenumerated.
     */
    public void testMapKeyEnumeratedElementCollection() {

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MKEDepartment4 dept = new MKEDepartment4(7, "Accounts");
        em.persist(dept);

        Map<MKENumbers, MKEmbeddedEmployee> link = new HashMap<>();
        link.put(MKENumbers.one, new MKEmbeddedEmployee(8, "Grace"));
        link.put(MKENumbers.two, new MKEmbeddedEmployee(9, "Bender"));
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Now read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        MKEDepartment4 foundDept = em.find(MKEDepartment4.class, 7);
        assertNotNull("Department should be found", foundDept);
        Map<MKENumbers, MKEmbeddedEmployee> emps = foundDept.getLastNameEmployees();
        assertNotNull("Map should not be null", emps);
        assertEquals("Map should have 2 entries", 2, emps.size());

        Set<MKENumbers> expectedKeys = new HashSet<>();
        expectedKeys.add(MKENumbers.one);
        expectedKeys.add(MKENumbers.two);

        Set<MKENumbers> actualKeys = emps.keySet();
        assertTrue("Keys should match expected",
            expectedKeys.containsAll(actualKeys)
            && actualKeys.containsAll(expectedKeys));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests @MapKeyTemporal(TemporalType.DATE) on a OneToMany map.
     * Mirrors TCK mapKeyTemporalTest.
     */
    public void testMapKeyTemporal() throws Exception {
        Date d1 = new SimpleDateFormat("yyyy-MM-dd").parse("2000-02-14");
        Date d2 = new SimpleDateFormat("yyyy-MM-dd").parse("2001-06-27");
        Date d3 = new SimpleDateFormat("yyyy-MM-dd").parse("2002-07-07");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MKTDepartment dept = new MKTDepartment(1, "Marketing");
        em.persist(dept);

        MKTEmployee emp1 = new MKTEmployee(1, "Frechette");
        emp1.setDepartment(dept);
        MKTEmployee emp2 = new MKTEmployee(3, "McGowan");
        emp2.setDepartment(dept);
        MKTEmployee emp3 = new MKTEmployee(5, "DMilla");
        emp3.setDepartment(dept);

        em.persist(emp1);
        em.persist(emp2);
        em.persist(emp3);

        Map<Date, MKTEmployee> link = new HashMap<>();
        link.put(d1, emp1);
        link.put(d2, emp2);
        link.put(d3, emp3);
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Now read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        MKTEmployee foundEmp = em.find(MKTEmployee.class, 1);
        assertNotNull("Employee should be found", foundEmp);
        MKTDepartment foundDept = foundEmp.getDepartment();
        assertNotNull("Department should be found", foundDept);

        Map<Date, MKTEmployee> emps = foundDept.getLastNameEmployees();
        assertNotNull("Map should not be null", emps);
        assertEquals("Map should have 3 entries", 3, emps.size());

        Set<Date> expectedKeys = new HashSet<>();
        expectedKeys.add(d1);
        expectedKeys.add(d2);
        expectedKeys.add(d3);

        Set<Date> actualKeys = emps.keySet();
        assertTrue("Keys should match expected",
            expectedKeys.containsAll(actualKeys)
            && actualKeys.containsAll(expectedKeys));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests @MapKeyTemporal(TemporalType.DATE) on an ElementCollection map
     * with @Transient on the field and @ElementCollection on the getter
     * (mixed access pattern). Mirrors TCK elementCollectionTest for
     * mapkeytemporal.
     */
    public void testMapKeyTemporalElementCollection() throws Exception {
        Date d6 = new SimpleDateFormat("yyyy-MM-dd").parse("2000-10-14");
        Date d7 = new SimpleDateFormat("yyyy-MM-dd").parse("2001-11-27");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MKTDepartment4 dept = new MKTDepartment4(5, "Shipping");
        em.persist(dept);

        Map<Date, MKTEmbeddedEmployee> link = new HashMap<>();
        link.put(d6, new MKTEmbeddedEmployee(6, "Donahue"));
        link.put(d7, new MKTEmbeddedEmployee(7, "Sanborn"));
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Now read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        MKTDepartment4 foundDept = em.find(MKTDepartment4.class, 5);
        assertNotNull("Department should be found", foundDept);

        Map<Date, MKTEmbeddedEmployee> emps = foundDept.getLastNameEmployees();
        assertNotNull("Map should not be null", emps);
        assertEquals("Map should have 2 entries", 2, emps.size());

        Set<Date> expectedKeys = new HashSet<>();
        expectedKeys.add(d6);
        expectedKeys.add(d7);

        Set<Date> actualKeys = emps.keySet();
        assertTrue("Keys should match expected",
            expectedKeys.containsAll(actualKeys)
            && actualKeys.containsAll(expectedKeys));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests that the default map key column name follows JPA 3.2 spec 11.1.35:
     * when no @MapKeyColumn is specified, the default column name should be
     * &lt;collection_attribute_name&gt;_KEY (e.g. "lastNameEmployees_KEY"),
     * not just "KEY" or "KEY0" or "KEY2".
     */
    public void testDefaultMapKeyColumnName() {
        // Force metadata resolution by opening an EM
        EntityManager em = emf.createEntityManager();

        // Check MKEDepartment4 (ElementCollection with @MapKeyEnumerated, no @MapKeyColumn)
        ClassMapping mapping4 = (ClassMapping) JPAFacadeHelper
            .getMetaData(emf, MKEDepartment4.class);
        FieldMapping fm4 = mapping4.getFieldMapping("lastNameEmployees");
        assertNotNull("Field mapping for lastNameEmployees should exist", fm4);
        assertEquals("Default map key column should be lastNameEmployees_KEY",
            "lastNameEmployees_KEY",
            fm4.getKeyMapping().getColumns()[0].getName());

        // Check MKTDepartment4 (ElementCollection with explicit @MapKeyColumn(name="THEDATE"))
        ClassMapping mappingT4 = (ClassMapping) JPAFacadeHelper
            .getMetaData(emf, MKTDepartment4.class);
        FieldMapping fmT4 = mappingT4.getFieldMapping("lastNameEmployees");
        assertNotNull("Field mapping for lastNameEmployees should exist", fmT4);
        assertEquals("Explicit @MapKeyColumn name should be preserved",
            "THEDATE",
            fmT4.getKeyMapping().getColumns()[0].getName());

        // Check MKEDepartment (OneToMany with explicit @MapKeyColumn(name="OFFICE_ID"))
        ClassMapping mapping = (ClassMapping) JPAFacadeHelper
            .getMetaData(emf, MKEDepartment.class);
        FieldMapping fm = mapping.getFieldMapping("lastNameEmployees");
        assertNotNull("Field mapping for lastNameEmployees should exist", fm);
        assertEquals("Explicit @MapKeyColumn name should be preserved",
            "OFFICE_ID",
            fm.getKeyMapping().getColumns()[0].getName());

        em.close();
    }

}

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
 * Tests for @MapKeyEnumerated and @MapKeyTemporal with RUNTIME
 * enhancement (Unenhanced prefix skips build-time enhancer).
 * Reproduces TCK mapkeyenumerated and mapkeytemporal failures.
 */
public class TestUnenhancedMapKeyEnumTemporal extends SingleEMFTestCase {

    /**
     * Tests @MapKeyEnumerated(STRING) on @OneToMany(mappedBy) Map
     * with runtime enhancement. Mirrors TCK mapKeyEnumeratedTest.
     */
    public void testMapKeyEnumeratedOneToMany() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            DROP_TABLES,
            UnenhancedMKEDept.class, UnenhancedMKEEmp.class);

        // Inspect metadata
        ClassMapping cm = (ClassMapping) JPAFacadeHelper
            .getMetaData(emf, UnenhancedMKEDept.class);
        FieldMapping fm = cm.getFieldMapping("lastNameEmployees");
        assertNotNull("FieldMapping for lastNameEmployees should exist", fm);
        String keyStrategy = fm.getKeyMapping().getValueInfo()
            .getStrategy();
        assertNotNull("Key strategy should be set (EnumValueHandler)",
            keyStrategy);
        assertTrue("Key strategy should contain EnumValueHandler: "
            + keyStrategy,
            keyStrategy.contains("EnumValueHandler"));
        assertTrue("Key strategy should have StoreOrdinal=false for STRING: "
            + keyStrategy,
            keyStrategy.contains("StoreOrdinal=false"));

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKEDept dept = new UnenhancedMKEDept(1, "Marketing");
        em.persist(dept);

        UnenhancedMKEEmp emp1 = new UnenhancedMKEEmp(1, "Alan", "Frechette");
        emp1.setDepartment(dept);
        UnenhancedMKEEmp emp2 = new UnenhancedMKEEmp(3, "Shelly", "McGowan");
        emp2.setDepartment(dept);
        UnenhancedMKEEmp emp3 = new UnenhancedMKEEmp(5, "Stephen", "DMilla");
        emp3.setDepartment(dept);

        em.persist(emp1);
        em.persist(emp2);
        em.persist(emp3);

        Map<UnenhancedMKEOffices, UnenhancedMKEEmp> link = new HashMap<>();
        link.put(UnenhancedMKEOffices.OFF000, emp1);
        link.put(UnenhancedMKEOffices.OFF002, emp2);
        link.put(UnenhancedMKEOffices.OFF004, emp3);
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Read back in fresh EM
        em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKEEmp foundEmp = em.find(UnenhancedMKEEmp.class, 1);
        assertNotNull("Employee should be found", foundEmp);
        UnenhancedMKEDept foundDept = foundEmp.getDepartment();
        assertNotNull("Department should be found", foundDept);

        Map<UnenhancedMKEOffices, UnenhancedMKEEmp> emps =
            foundDept.getLastNameEmployees();
        assertNotNull("Map should not be null", emps);
        assertEquals("Map should have 3 entries", 3, emps.size());

        Set<UnenhancedMKEOffices> expectedKeys = new HashSet<>();
        expectedKeys.add(UnenhancedMKEOffices.OFF000);
        expectedKeys.add(UnenhancedMKEOffices.OFF002);
        expectedKeys.add(UnenhancedMKEOffices.OFF004);

        Set<UnenhancedMKEOffices> actualKeys = emps.keySet();
        assertTrue("Keys should match expected",
            expectedKeys.containsAll(actualKeys)
            && actualKeys.containsAll(expectedKeys));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests @MapKeyEnumerated(STRING) on @ElementCollection with
     * @Transient field and runtime enhancement.
     * Mirrors TCK elementCollectionTest for mapkeyenumerated.
     */
    public void testMapKeyEnumeratedElementCollection() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            CLEAR_TABLES,
            UnenhancedMKEDept4.class, UnenhancedMKEmbedEmp.class);

        // Check identity type
        ClassMapping cm = (ClassMapping) JPAFacadeHelper
            .getMetaData(emf, UnenhancedMKEDept4.class);
        assertEquals("Should be APPLICATION identity",
            ClassMetaData.ID_APPLICATION, cm.getIdentityType());
        FieldMetaData[] pks = cm.getPrimaryKeyFields();
        assertEquals("Should have 1 PK field", 1, pks.length);
        assertEquals("PK field should be 'id'", "id", pks[0].getName());

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKEDept4 dept = new UnenhancedMKEDept4(7, "Accounts");
        em.persist(dept);

        Map<UnenhancedMKENumbers, UnenhancedMKEmbedEmp> link =
            new HashMap<>();
        link.put(UnenhancedMKENumbers.one,
            new UnenhancedMKEmbedEmp(8, "Grace"));
        link.put(UnenhancedMKENumbers.two,
            new UnenhancedMKEmbedEmp(9, "Bender"));
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKEDept4 foundDept =
            em.find(UnenhancedMKEDept4.class, 7);
        assertNotNull("Department should be found", foundDept);
        Map<UnenhancedMKENumbers, UnenhancedMKEmbedEmp> emps =
            foundDept.getLastNameEmployees();
        assertNotNull("Map should not be null", emps);
        assertEquals("Map should have 2 entries", 2, emps.size());

        Set<UnenhancedMKENumbers> expectedKeys = new HashSet<>();
        expectedKeys.add(UnenhancedMKENumbers.one);
        expectedKeys.add(UnenhancedMKENumbers.two);

        Set<UnenhancedMKENumbers> actualKeys = emps.keySet();
        assertTrue("Keys should match expected",
            expectedKeys.containsAll(actualKeys)
            && actualKeys.containsAll(expectedKeys));

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests @MapKeyTemporal(DATE) on @OneToMany(mappedBy) Map
     * with runtime enhancement. Mirrors TCK mapKeyTemporalTest.
     */
    public void testMapKeyTemporalOneToMany() throws Exception {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            DROP_TABLES,
            UnenhancedMKTDept.class, UnenhancedMKTEmp.class);

        Date d1 = new SimpleDateFormat("yyyy-MM-dd").parse("2000-02-14");
        Date d2 = new SimpleDateFormat("yyyy-MM-dd").parse("2001-06-27");
        Date d3 = new SimpleDateFormat("yyyy-MM-dd").parse("2002-07-07");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKTDept dept = new UnenhancedMKTDept(1, "Marketing");
        em.persist(dept);

        UnenhancedMKTEmp emp1 = new UnenhancedMKTEmp(1, "Frechette");
        emp1.setDepartment(dept);
        UnenhancedMKTEmp emp2 = new UnenhancedMKTEmp(3, "McGowan");
        emp2.setDepartment(dept);
        UnenhancedMKTEmp emp3 = new UnenhancedMKTEmp(5, "DMilla");
        emp3.setDepartment(dept);

        em.persist(emp1);
        em.persist(emp2);
        em.persist(emp3);

        Map<Date, UnenhancedMKTEmp> link = new HashMap<>();
        link.put(d1, emp1);
        link.put(d2, emp2);
        link.put(d3, emp3);
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKTEmp foundEmp = em.find(UnenhancedMKTEmp.class, 1);
        assertNotNull("Employee should be found", foundEmp);
        UnenhancedMKTDept foundDept = foundEmp.getDepartment();
        assertNotNull("Department should be found", foundDept);

        Map<Date, UnenhancedMKTEmp> emps =
            foundDept.getLastNameEmployees();
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
     * Tests @MapKeyTemporal(DATE) on @ElementCollection with
     * @Transient field and runtime enhancement.
     * Mirrors TCK elementCollectionTest for mapkeytemporal.
     */
    public void testMapKeyTemporalElementCollection() throws Exception {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            CLEAR_TABLES,
            UnenhancedMKTDept4.class, UnenhancedMKEmbedEmp.class);

        Date d6 = new SimpleDateFormat("yyyy-MM-dd").parse("2000-10-14");
        Date d7 = new SimpleDateFormat("yyyy-MM-dd").parse("2001-11-27");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKTDept4 dept = new UnenhancedMKTDept4(5, "Shipping");
        em.persist(dept);

        Map<Date, UnenhancedMKEmbedEmp> link = new HashMap<>();
        link.put(d6, new UnenhancedMKEmbedEmp(6, "Donahue"));
        link.put(d7, new UnenhancedMKEmbedEmp(7, "Sanborn"));
        dept.setLastNameEmployees(link);

        em.merge(dept);
        em.getTransaction().commit();
        em.close();

        // Read back
        em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedMKTDept4 foundDept =
            em.find(UnenhancedMKTDept4.class, 5);
        assertNotNull("Department should be found", foundDept);

        Map<Date, UnenhancedMKEmbedEmp> emps =
            foundDept.getLastNameEmployees();
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
}

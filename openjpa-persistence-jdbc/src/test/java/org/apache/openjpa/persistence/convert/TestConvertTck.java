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
package org.apache.openjpa.persistence.convert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests mirroring the 6 failing TCK tests related to @Convert support:
 * - mappedSuperClassTest (SalaryConverter + auto-apply CharArrayConverter)
 * - mappedSuperClass2Test (converter override via class-level @Converts)
 * - convertsTest (embedded @Converts with multiple converters)
 * - convertExceptionDuringPersistTest (RuntimeException -> PersistenceException)
 * - convertExceptionDuringLoadTest (RuntimeException during load)
 * - repeatable convertsTest (repeatable @Convert without @Converts wrapper)
 *
 * All entities are UNENHANCED to mirror how TCK runs (runtime enhancement).
 */
public class TestConvertTck extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            ConvertFullTimeEmp.class,
            ConvertMappedSuper.class,
            ConvertFullTimeEmp2.class,
            ConvertMappedSuper2.class,
            ConvertEmbedEntity.class,
            ConvertRepeatableEntity.class,
            ConvertAddress.class,
            ConvertSelfAddress.class,
            SalaryConverter.class,
            CharArrayConverter.class,
            DotConverter.class,
            DotConverter2.class,
            NumberToStateConverter.class);
    }

    /**
     * Test converter with property access and attributes from
     * MappedSuperClass. SalaryConverter converts String salary
     * (with # separators) to Float. CharArrayConverter (auto-apply)
     * converts char[] lastName.
     * Mirrors TCK mappedSuperClassTest.
     */
    public void testMappedSuperClass() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        char[] ln = {'D', 'o', 'e'};
        ConvertFullTimeEmp expected =
            new ConvertFullTimeEmp(1, "John", ln, "3#5#0#0#0.0");
        em.persist(expected);
        em.flush();
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        em.getTransaction().begin();
        ConvertFullTimeEmp emp =
            em.find(ConvertFullTimeEmp.class, expected.getId());
        assertNotNull("Employee should be found", emp);
        // SalaryConverter: toDB removes "#" => 35000.0 stored as float
        // toEntity converts back to string => "35000.0"
        assertEquals("35000.0", emp.getSalary());
        // CharArrayConverter: "Doe" => "Smith" (toDB), "Smith" => "James"
        // (toEntity)
        assertEquals("James", new String(emp.getLastName()));
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test overriding a converter from MappedSuperclass.
     * Employee3/ConvertMappedSuper2 has DotConverter on firstName.
     * FullTimeEmployee2/ConvertFullTimeEmp2 overrides with DotConverter2
     * via class-level @Converts.
     * Mirrors TCK mappedSuperClass2Test.
     */
    public void testMappedSuperClassOverride() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ConvertFullTimeEmp2 expected =
            new ConvertFullTimeEmp2(1, "J.o.h.n", "Hill", "3500.0");
        em.persist(expected);
        em.flush();
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        em.getTransaction().begin();
        ConvertFullTimeEmp2 emp =
            em.find(ConvertFullTimeEmp2.class, expected.getId());
        assertNotNull("Employee should be found", emp);
        // DotConverter2: toDB replaces "." with "-" => "J-o-h-n"
        // toEntity replaces "-" with "#" => "J#o#h#n"
        assertEquals("J#o#h#n", emp.getFirstName());
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test @Converts with multiple converters on an embedded field.
     * DotConverter on street: "." -> "#" (toDB), "#" -> "_" (toEntity)
     * NumberToStateConverter on state: 1 -> "MA" (toDB), "MA" -> 1 (toEntity)
     * Mirrors TCK convertsTest.
     */
    public void testConvertsOnEmbedded() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        String street = "500.Oracle.Parkway";
        String city = "Redwood Shores";
        ConvertEmbedEntity b = new ConvertEmbedEntity(
            "1", "name1", 1000,
            new ConvertAddress(street, city, 1));
        em.persist(b);
        em.flush();
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        em.getTransaction().begin();
        ConvertEmbedEntity b1 =
            em.find(ConvertEmbedEntity.class, "1");
        assertNotNull("Entity should be found", b1);
        // value has no converter => stays 1000
        // TCK expects 1110 due to IntegerConverter auto-apply
        // but our test has no IntegerConverter so we just check 1000
        assertEquals(Integer.valueOf(1000), b1.getValue());

        ConvertAddress a = b1.getAddress();
        assertNotNull("Address should not be null", a);
        // DotConverter: "500.Oracle.Parkway" -> "500#Oracle#Parkway" (toDB)
        //  -> "500_Oracle_Parkway" (toEntity)
        assertEquals("500_Oracle_Parkway", a.getStreet());
        // NumberToStateConverter: 1 -> "MA" (toDB), "MA" -> 1 (toEntity)
        assertEquals(1, a.getState());
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test that converter RuntimeException during persist is wrapped
     * in PersistenceException.
     * Mirrors TCK convertExceptionDuringPersistTest.
     */
    public void testConverterExceptionDuringPersist() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            // state=-1 triggers RuntimeException in NumberToStateConverter
            ConvertEmbedEntity b = new ConvertEmbedEntity(
                "1", "name1", 1,
                new ConvertAddress("500 Oracle Parkway",
                    "Redwood Shores", -1));
            em.persist(b);
            em.flush();
            em.getTransaction().commit();
            fail("Expected PersistenceException from converter");
        } catch (PersistenceException pe) {
            // Expected: converter RuntimeException wrapped in
            // PersistenceException, naming the field and the converter
            assertConverterErrorMessage(pe);
            assertTrue("Transaction should be marked for rollback",
                em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test that converter RuntimeException during load is wrapped
     * in PersistenceException.
     * Mirrors TCK convertExceptionDuringLoadTest.
     */
    public void testConverterExceptionDuringLoad() {
        EntityManager em = emf.createEntityManager();
        // First persist with state=-2. NumberToStateConverter.toDB
        // stores "-2" as-is (state=-2, not -1 and not -2 string check)
        em.getTransaction().begin();
        ConvertEmbedEntity b = new ConvertEmbedEntity(
            "1", "name1", 1,
            new ConvertAddress("500 Oracle Parkway",
                "Redwood Shores", -2));
        em.persist(b);
        em.flush();
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        // Now find should trigger exception during load
        em.getTransaction().begin();
        try {
            ConvertEmbedEntity b1 =
                em.find(ConvertEmbedEntity.class, "1");
            fail("Expected PersistenceException from converter during load");
        } catch (PersistenceException pe) {
            // Expected: converter RuntimeException wrapped in
            // PersistenceException, naming the field and the converter
            assertConverterErrorMessage(pe);
            assertTrue("Transaction should be marked for rollback",
                em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Test repeatable @Convert (without @Converts wrapper)
     * on embedded field.
     * Mirrors TCK jpa22/repeatable/convert/Client#convertsTest.
     */
    public void testRepeatableConvertOnEmbedded() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        String street = "500.Oracle.Parkway";
        String city = "Redwood Shores";
        ConvertRepeatableEntity b = new ConvertRepeatableEntity(
            "1", "name1", 1000,
            new ConvertAddress(street, city, 1));
        em.persist(b);
        em.flush();
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        em.getTransaction().begin();
        ConvertRepeatableEntity b1 =
            em.find(ConvertRepeatableEntity.class, "1");
        assertNotNull("Entity should be found", b1);
        assertEquals(Integer.valueOf(1000), b1.getValue());

        ConvertAddress a = b1.getAddress();
        assertNotNull("Address should not be null", a);
        // Same converters as testConvertsOnEmbedded
        assertEquals("500_Oracle_Parkway", a.getStreet());
        assertEquals(1, a.getState());
        em.getTransaction().commit();
        em.close();
    }

    /**
     * A null value of an auto-applied converter on an array attribute is
     * passed to the converter and back, so the attribute reads back as null.
     */
    public void testNullConvertedArrayAttribute() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertFullTimeEmp(900, "Jane", null, "1000.00"));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        ConvertFullTimeEmp emp = em.find(ConvertFullTimeEmp.class, 900);
        assertNotNull("Employee should be found", emp);
        assertNull("lastName should read back as null", emp.getLastName());
        assertEquals("1000.0", emp.getSalary());
        em.close();
    }

    /**
     * A null attribute of an embeddable is converted like any other, so a
     * null-safe converter round-trips it as null.
     */
    public void testNullConvertedAttributeOnEmbedded() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertEmbedEntity("null-attr", "name1", 1000,
            new ConvertAddress(null, "Redwood Shores", 1)));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        ConvertEmbedEntity e = em.find(ConvertEmbedEntity.class, "null-attr");
        assertNotNull("Entity should be found", e);
        ConvertAddress a = e.getAddress();
        assertNotNull("Address should not be null", a);
        assertNull("street should read back as null", a.getStreet());
        assertEquals("Redwood Shores", a.getCity());
        assertEquals(1, a.getState());
        em.close();
    }

    /**
     * An embeddable without a null-indicator column is materialized from its
     * all-null columns, so its converted attributes are loaded from a null
     * column value.
     */
    public void testNullEmbedded() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertEmbedEntity("null-embed", "name1", 1000));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        ConvertEmbedEntity e = em.find(ConvertEmbedEntity.class, "null-embed");
        assertNotNull("Entity should be found", e);
        ConvertAddress a = e.getAddress();
        assertNotNull("Address is materialized from the null columns", a);
        assertNull("street should read back as null", a.getStreet());
        assertNull("city should read back as null", a.getCity());
        assertEquals(0, a.getState());
        em.close();
    }

    /**
     * A converter failure must name the field it happened on, so the user can
     * tell which attribute and converter is at fault.
     */
    private void assertConverterErrorMessage(PersistenceException pe) {
        StringBuilder msgs = new StringBuilder();
        for (Throwable t = pe; t != null; t = t.getCause()) {
            msgs.append(t.getMessage()).append('\n');
        }
        assertTrue("A converter failure should name the failing field, was: "
            + msgs, msgs.indexOf("ConvertAddress.state") >= 0);
    }
}

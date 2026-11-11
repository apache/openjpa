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
package org.apache.openjpa.persistence.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that BigDecimal @Id entities work correctly when sharing a table
 * with BigInteger @Id entities. This reproduces the TCK failures:
 * - annotations.id.Client#FieldBigDecimalIdTest
 * - annotations.id.Client#PropertyBigDecimalIdTest
 *
 * The root cause is that when multiple entities share the same table column
 * (e.g., DATATYPES3.ID) with different Java types (BigDecimal vs BigInteger),
 * the column's javaType gets set to whichever entity is mapped last. When
 * loading, the value is read using the column's type rather than the field's
 * declared type, causing a ClassCastException in pcReplaceField.
 *
 * The fix is in ImmutableValueHandler.toObjectValue() which now converts
 * numeric values to the expected field type when a mismatch is detected.
 */
public class TestBigDecimalIdSharedTable extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(SharedTableBigDecimalId.class,
              SharedTableBigIntegerId.class,
              PropertyBigDecimalIdEntity.class,
              PropertyBigIntegerIdEntity.class,
              DROP_TABLES);
    }

    /**
     * Test field-access BigDecimal @Id persist and find with shared table.
     * Mirrors TCK FieldBigDecimalIdTest.
     */
    @Test
    public void testFieldBigDecimalIdPersistAndFind() {
        BigDecimal id = new BigDecimal(new BigInteger("1"));
        BigDecimal value = new BigDecimal(new BigInteger("1"));

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        SharedTableBigDecimalId entity = new SharedTableBigDecimalId(id, value);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        // Find in a new EM to force DB load
        em = emf.createEntityManager();
        SharedTableBigDecimalId found =
            em.find(SharedTableBigDecimalId.class, id);
        assertNotNull(found, "BigDecimal ID entity should be found");
        assertEquals(0,
            id.compareTo(found.getValue()), "Value should match");
        em.close();
    }

    /**
     * Test field-access BigInteger @Id persist and find with shared table.
     * Ensures BigInteger still works after the BigDecimal fix.
     */
    @Test
    public void testFieldBigIntegerIdPersistAndFind() {
        BigInteger id = new BigInteger("2");
        BigInteger value = new BigInteger("2");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        SharedTableBigIntegerId entity =
            new SharedTableBigIntegerId(id, value);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        SharedTableBigIntegerId found =
            em.find(SharedTableBigIntegerId.class, id);
        assertNotNull(found, "BigInteger ID entity should be found");
        assertEquals(id, found.getValue(), "Value should match");
        em.close();
    }

    /**
     * Test property-access BigDecimal @Id persist and find with shared table.
     * Mirrors TCK PropertyBigDecimalIdTest.
     */
    @Test
    public void testPropertyBigDecimalIdPersistAndFind() {
        BigDecimal id = new BigDecimal(new BigInteger("1"));
        BigDecimal value = new BigDecimal(new BigInteger("1"));

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        PropertyBigDecimalIdEntity entity =
            new PropertyBigDecimalIdEntity(id, value);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        PropertyBigDecimalIdEntity found =
            em.find(PropertyBigDecimalIdEntity.class, id);
        assertNotNull(found, "Property BigDecimal ID entity should be found");
        assertEquals(0,
            id.compareTo(found.getBigDecimal()), "Value should match");
        em.close();
    }

    /**
     * Test property-access BigInteger @Id persist and find with shared table.
     * Ensures BigInteger still works after the BigDecimal fix.
     */
    @Test
    public void testPropertyBigIntegerIdPersistAndFind() {
        BigInteger id = new BigInteger("2");
        BigInteger value = new BigInteger("2");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        PropertyBigIntegerIdEntity entity =
            new PropertyBigIntegerIdEntity(id, value);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        PropertyBigIntegerIdEntity found =
            em.find(PropertyBigIntegerIdEntity.class, id);
        assertNotNull(found, "Property BigInteger ID entity should be found");
        assertEquals(id, found.getBigInteger(), "Value should match");
        em.close();
    }

    /**
     * Test that both BigDecimal and BigInteger entities can coexist
     * in the same persistence unit and be loaded in sequence.
     */
    @Test
    public void testMixedBigDecimalBigIntegerFind() {
        BigDecimal decId = new BigDecimal("100");
        BigInteger intId = new BigInteger("200");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new SharedTableBigDecimalId(decId, decId));
        em.persist(new SharedTableBigIntegerId(intId, intId));
        em.getTransaction().commit();
        em.close();

        // Load both in a single EM session
        em = emf.createEntityManager();
        SharedTableBigDecimalId decFound =
            em.find(SharedTableBigDecimalId.class, decId);
        SharedTableBigIntegerId intFound =
            em.find(SharedTableBigIntegerId.class, intId);

        assertNotNull(decFound, "BigDecimal entity should be found");
        assertNotNull(intFound, "BigInteger entity should be found");
        assertTrue(decFound.getValue() instanceof BigDecimal,
            "BigDecimal value type check");
        assertTrue(intFound.getValue() instanceof BigInteger,
            "BigInteger value type check");
        em.close();
    }
}

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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPA 3.2 ID type and field type fixes:
 * - BigDecimal @Id with shared table (BigDecimal/BigInteger)
 * - OffsetDateTime column compatibility (TIMESTAMP_WITH_TIMEZONE)
 * - UUID @Id persist and find
 */
public class TestIdTypeFixes extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(SharedTableBigDecimalId.class,
              SharedTableBigIntegerId.class,
              UUIDIdEntity.class,
              DateTimeTypesEntity.class,
              DROP_TABLES);
    }

    /**
     * Test that BigDecimal @Id works when sharing a table with
     * a BigInteger @Id entity. Previously this threw:
     * ClassCastException: BigDecimal cannot be cast to BigInteger
     */
    public void testBigDecimalIdSharedTable() {
        BigDecimal id = new BigDecimal("12345.67");
        BigDecimal value = new BigDecimal("99.99");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        SharedTableBigDecimalId entity = new SharedTableBigDecimalId(id, value);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        // Verify the entity can be found
        em = emf.createEntityManager();
        SharedTableBigDecimalId found = em.find(SharedTableBigDecimalId.class, id);
        assertNotNull("BigDecimal ID entity should be found", found);
        assertEquals(0, id.compareTo(found.getId()));
        em.close();
    }

    /**
     * Test that BigInteger @Id works when sharing a table with
     * a BigDecimal @Id entity.
     */
    public void testBigIntegerIdSharedTable() {
        BigInteger id = new BigInteger("67890");
        BigInteger value = new BigInteger("42");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        SharedTableBigIntegerId entity = new SharedTableBigIntegerId(id, value);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        // Verify the entity can be found
        em = emf.createEntityManager();
        SharedTableBigIntegerId found = em.find(SharedTableBigIntegerId.class, id);
        assertNotNull("BigInteger ID entity should be found", found);
        assertEquals(id, found.getId());
        em.close();
    }

    /**
     * Test that UUID @Id entities can be persisted and found.
     * Previously this failed on PostgreSQL with:
     * "operator does not exist: character varying = uuid"
     */
    public void testUUIDIdPersistAndFind() {
        UUID id = UUID.randomUUID();
        String name = "Test UUID Entity";

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        UUIDIdEntity entity = new UUIDIdEntity(id, name);
        em.persist(entity);
        em.getTransaction().commit();
        em.close();

        // Verify the entity can be found
        em = emf.createEntityManager();
        UUIDIdEntity found = em.find(UUIDIdEntity.class, id);
        assertNotNull("UUID ID entity should be found", found);
        assertEquals(id, found.getId());
        assertEquals(name, found.getName());
        em.close();
    }

    /**
     * Test that OffsetDateTime fields can be persisted and retrieved.
     * Previously this failed with:
     * "column is not compatible with the expected type unknown(2014)"
     */
    public void testDateTimeTypes() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        DateTimeTypesEntity entity = new DateTimeTypesEntity();
        entity.setLocalDate(LocalDate.of(2024, 6, 15));
        entity.setLocalTime(LocalTime.of(10, 30, 45));
        entity.setLocalDateTime(LocalDateTime.of(2024, 6, 15, 10, 30, 45));
        entity.setOffsetTime(OffsetTime.of(10, 30, 45, 0, ZoneOffset.UTC));
        entity.setOffsetDateTime(OffsetDateTime.of(2024, 6, 15, 10, 30, 45, 0,
            ZoneOffset.UTC));
        entity.setInstantVal(Instant.parse("2024-06-15T10:30:45Z"));
        entity.setYearVal(Year.of(2024));

        em.persist(entity);
        em.getTransaction().commit();
        Long id = entity.getId();
        em.close();

        // Verify the entity can be loaded
        em = emf.createEntityManager();
        DateTimeTypesEntity found = em.find(DateTimeTypesEntity.class, id);
        assertNotNull("DateTimeTypes entity should be found", found);
        assertNotNull("OffsetDateTime should not be null", found.getOffsetDateTime());
        assertNotNull("Instant should not be null", found.getInstantVal());
        assertNotNull("Year should not be null", found.getYearVal());
        em.close();
    }

    /**
     * Test that Column.isCompatible handles TIMESTAMP_WITH_TIMEZONE
     * and TIME_WITH_TIMEZONE types as self-compatible.
     */
    public void testTimestampWithTimezoneCompatibility() {
        Column col = new Column();
        col.setType(Types.TIMESTAMP_WITH_TIMEZONE);

        // TIMESTAMP_WITH_TIMEZONE should be compatible with itself
        assertTrue("TIMESTAMP_WITH_TIMEZONE should be compatible with itself",
            col.isCompatible(Types.TIMESTAMP_WITH_TIMEZONE, null, 0, 0));

        // TIMESTAMP_WITH_TIMEZONE should be compatible with TIMESTAMP
        assertTrue("TIMESTAMP_WITH_TIMEZONE should be compatible with TIMESTAMP",
            col.isCompatible(Types.TIMESTAMP, null, 0, 0));

        // TIMESTAMP should be compatible with TIMESTAMP_WITH_TIMEZONE
        Column tsCol = new Column();
        tsCol.setType(Types.TIMESTAMP);
        assertTrue("TIMESTAMP should be compatible with TIMESTAMP_WITH_TIMEZONE",
            tsCol.isCompatible(Types.TIMESTAMP_WITH_TIMEZONE, null, 0, 0));
    }

    /**
     * Test that TIME_WITH_TIMEZONE is self-compatible.
     */
    public void testTimeWithTimezoneCompatibility() {
        Column col = new Column();
        col.setType(Types.TIME_WITH_TIMEZONE);

        assertTrue("TIME_WITH_TIMEZONE should be compatible with itself",
            col.isCompatible(Types.TIME_WITH_TIMEZONE, null, 0, 0));
        assertTrue("TIME_WITH_TIMEZONE should be compatible with TIME",
            col.isCompatible(Types.TIME, null, 0, 0));
    }

    /**
     * Test that Schemas.getJDBCName returns proper names for
     * TIMESTAMP_WITH_TIMEZONE and TIME_WITH_TIMEZONE.
     */
    public void testSchemasGetJDBCName() {
        assertEquals("timestamp_with_timezone",
            Schemas.getJDBCName(Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals("time_with_timezone",
            Schemas.getJDBCName(Types.TIME_WITH_TIMEZONE));
    }

    /**
     * Test that Schemas.getJavaType returns proper types for
     * TIMESTAMP_WITH_TIMEZONE and TIME_WITH_TIMEZONE.
     */
    public void testSchemasGetJavaType() {
        assertEquals(OffsetDateTime.class,
            Schemas.getJavaType(Types.TIMESTAMP_WITH_TIMEZONE, 0, 0));
        assertEquals(OffsetTime.class,
            Schemas.getJavaType(Types.TIME_WITH_TIMEZONE, 0, 0));
    }

    /**
     * Test that JavaTypes.convert handles BigDecimal/BigInteger conversions.
     */
    public void testJavaTypesConvert() {
        BigDecimal bd = new BigDecimal("123");
        BigInteger bi = new BigInteger("456");

        // BigDecimal -> BigInteger conversion
        Object result = JavaTypes.convert(bd, JavaTypes.BIGINTEGER);
        assertTrue("Should convert BigDecimal to BigInteger",
            result instanceof BigInteger);
        assertEquals(new BigInteger("123"), result);

        // BigInteger -> BigDecimal conversion
        result = JavaTypes.convert(bi, JavaTypes.BIGDECIMAL);
        assertTrue("Should convert BigInteger to BigDecimal",
            result instanceof BigDecimal);
    }
}

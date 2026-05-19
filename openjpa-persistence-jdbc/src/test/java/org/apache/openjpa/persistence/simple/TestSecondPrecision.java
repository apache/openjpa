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

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that JPA 3.2 @Column(secondPrecision) is correctly handled
 * for time-based column types.
 */
public class TestSecondPrecision extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, SecondPrecisionEntity.class);
    }

    public void testPersistAndFind() {
        LocalDateTime now = LocalDateTime.now();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        SecondPrecisionEntity e = new SecondPrecisionEntity();
        e.setId(1);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setDayOnly(now);
        e.setDefaultPrecision(now);
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        SecondPrecisionEntity loaded = em.find(SecondPrecisionEntity.class, 1L);
        assertNotNull(loaded);
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getUpdatedAt());
        assertNotNull(loaded.getDayOnly());
        assertNotNull(loaded.getDefaultPrecision());
        em.close();
    }

    public void testSecondPrecisionMetadata() {
        // Verify that secondPrecision is correctly parsed from the annotation
        // by checking the column metadata through the mapping info
        org.apache.openjpa.jdbc.meta.ClassMapping mapping =
            (org.apache.openjpa.jdbc.meta.ClassMapping)
            emf.getConfiguration().getMetaDataRepositoryInstance()
                .getMetaData(SecondPrecisionEntity.class, null, true);
        assertNotNull(mapping);

        org.apache.openjpa.jdbc.meta.FieldMapping createdAtMapping =
            mapping.getFieldMapping("createdAt");
        org.apache.openjpa.jdbc.schema.Column[] cols = createdAtMapping.getColumns();
        assertEquals(1, cols.length);
        assertEquals(3, cols[0].getSecondPrecision());

        org.apache.openjpa.jdbc.meta.FieldMapping updatedAtMapping =
            mapping.getFieldMapping("updatedAt");
        cols = updatedAtMapping.getColumns();
        assertEquals(1, cols.length);
        assertEquals(6, cols[0].getSecondPrecision());

        org.apache.openjpa.jdbc.meta.FieldMapping dayOnlyMapping =
            mapping.getFieldMapping("dayOnly");
        cols = dayOnlyMapping.getColumns();
        assertEquals(1, cols.length);
        assertEquals(0, cols[0].getSecondPrecision());
    }
}

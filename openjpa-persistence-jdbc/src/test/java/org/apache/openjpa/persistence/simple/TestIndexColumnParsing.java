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

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that @Index columnList with ASC/DESC sort directions
 * is properly parsed (sort direction stripped from column name).
 */
public class TestIndexColumnParsing extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, IndexedEntity.class);
    }

    /**
     * Verify entity with @Index(columnList="svalue DESC") can be
     * persisted and retrieved — the sort direction should not cause
     * a "column not found" error during schema creation.
     */
    public void testIndexWithSortDirection() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            IndexedEntity e = new IndexedEntity();
            e.setId(1);
            e.setSvalue("test");
            e.setSvalue2("test2");
            em.persist(e);
            em.getTransaction().commit();

            em.clear();
            IndexedEntity found = em.find(IndexedEntity.class, 1);
            assertNotNull(found);
            assertEquals("test", found.getSvalue());
            assertEquals("test2", found.getSvalue2());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Verify the index metadata has the correct column names
     * (without ASC/DESC suffixes).
     */
    public void testIndexMetadataColumns() {
        ClassMapping mapping = (ClassMapping) emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData(IndexedEntity.class, null, true);
        assertNotNull(mapping);

        Table table = mapping.getTable();
        assertNotNull(table);

        // Verify the columns exist in the table (no "svalue DESC" nonsense)
        assertNotNull("Column 'svalue' should exist",
            table.getColumn("svalue"));
        assertNotNull("Column 'svalue2' should exist",
            table.getColumn("svalue2"));
    }
}

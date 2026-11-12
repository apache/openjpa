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
package org.apache.openjpa.persistence.jdbc.annotations;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * The default name of an index over a delimited column must not contain the
 * delimiters of that column name (OPENJPA-3079).
 */
public class TestDelimitedIndexColumnName extends SingleEMFTestCase {
    @Override
    public void setUp() {
        setUp(EntityWithDelimitedIndexColumn.class, DROP_TABLES);
    }

    public void testDefaultIndexName() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance()
            .getMapping(EntityWithDelimitedIndexColumn.class, null, true);
        Table table = cls.getTable();

        Index idx = null;
        for (Index index : table.getIndexes()) {
            if (index.getColumns().length == 2) {
                idx = index;
            }
        }
        assertNotNull("Index over rank and AMOUNT should exist", idx);
        String name = idx.getIdentifier().getName();
        assertFalse("Index name " + name + " contains a delimiter", name.contains("\""));
        assertEquals("I_DLM_IDX_RANK_AMOUNT", name);
    }

    public void testSchemaCreated() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            EntityWithDelimitedIndexColumn e = new EntityWithDelimitedIndexColumn();
            e.setId(1);
            e.setRank(2);
            e.setAmount(3);
            em.persist(e);
            em.getTransaction().commit();
            em.clear();

            EntityWithDelimitedIndexColumn found = em.find(EntityWithDelimitedIndexColumn.class, 1L);
            assertNotNull(found);
            assertEquals(2, found.getRank());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}

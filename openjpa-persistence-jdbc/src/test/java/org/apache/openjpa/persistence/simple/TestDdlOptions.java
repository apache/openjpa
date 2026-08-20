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
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA 3.2 options member on DDL annotations.
 */
public class TestDdlOptions extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, OptionsEntity.class);
    }

    public void testColumnOptionsInMetadata() {
        ClassMapping mapping = (ClassMapping) emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData(OptionsEntity.class, null, true);
        assertNotNull(mapping);

        FieldMapping nameMapping = mapping.getFieldMapping("name");
        Column[] cols = nameMapping.getColumns();
        assertEquals(1, cols.length);
        assertEquals("CHECK (name IS NOT NULL)", cols[0].getOptions());
    }

    public void testColumnOptionsInDdl() {
        ClassMapping mapping = (ClassMapping) emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData(OptionsEntity.class, null, true);
        DBDictionary dict = ((JDBCConfiguration) emf.getConfiguration())
            .getDBDictionaryInstance();

        org.apache.openjpa.jdbc.schema.Table table = mapping.getTable();
        String[] ddl = dict.getCreateTableSQL(table);
        assertNotNull(ddl);
        assertEquals(1, ddl.length);

        // The column DDL should contain the options string
        assertTrue("DDL should contain column options: " + ddl[0],
            ddl[0].contains("CHECK (name IS NOT NULL)"));
    }

    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        OptionsEntity e = new OptionsEntity();
        e.setId(1);
        e.setName("test");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        OptionsEntity loaded = em.find(OptionsEntity.class, 1L);
        assertNotNull(loaded);
        assertEquals("test", loaded.getName());
        em.close();
    }
}

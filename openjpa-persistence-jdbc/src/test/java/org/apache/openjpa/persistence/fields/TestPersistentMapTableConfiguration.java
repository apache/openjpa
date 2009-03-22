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
package org.apache.openjpa.persistence.fields;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.jdbc.meta.ClassMapping;

public class TestPersistentMapTableConfiguration
    extends SQLListenerTestCase {

    public void setUp() {
        setUp(NonstandardMappingEntity.class);
    }

    public void testPersistentMapMetaData() {
        ClassMapping cm = (ClassMapping) JPAFacadeHelper.getMetaData(emf,
            NonstandardMappingEntity.class);
        assertEquals("NONSTD_MAPPING_MAP",
            cm.getFieldMapping("map").getTable().getName());
    }

    public void testPersistentMapInsert() {
        NonstandardMappingEntity e = new NonstandardMappingEntity();
        OpenJPAEntityManager em = emf.createEntityManager();
        em.getIdGenerator(NonstandardMappingEntity.class).allocate(1);
        sql.clear();
        try {
            em.getTransaction().begin();
            em.persist(e);
            e.getMap().put("foo", "bar");
            em.flush();
            assertEquals(2, sql.size());
            assertSQL(".*NONSTD_MAPPING_MAP.*");
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }
    }
}
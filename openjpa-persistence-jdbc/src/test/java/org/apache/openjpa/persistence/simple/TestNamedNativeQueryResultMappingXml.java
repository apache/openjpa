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

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests JPA 3.2 inline result set mapping on named-native-query in ORM XML.
 */
public class TestNamedNativeQueryResultMappingXml extends SQLListenerTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES, NativeQueryResultEntity.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        NativeQueryResultEntity e1 = new NativeQueryResultEntity();
        e1.setId(1);
        e1.setName("Alice");
        em.persist(e1);
        NativeQueryResultEntity e2 = new NativeQueryResultEntity();
        e2.setId(2);
        e2.setName("Bob");
        em.persist(e2);
        em.getTransaction().commit();
        em.close();
    }

    @Override
    protected String getPersistenceUnitName() {
        return "native-query-inline-result";
    }

    public void testEntityResultMappingXml() {
        EntityManager em = emf.createEntityManager();
        @SuppressWarnings("unchecked")
        List<NativeQueryResultEntity> results = em.createNamedQuery(
            "NativeQueryResultEntity.xmlFindAll").getResultList();
        assertNotNull(results);
        assertEquals(2, results.size());
        for (NativeQueryResultEntity e : results) {
            assertNotNull(e.getName());
            assertTrue(em.contains(e));
        }
        em.close();
    }

    public void testColumnResultMappingXml() {
        EntityManager em = emf.createEntityManager();
        @SuppressWarnings("unchecked")
        List<Object> results = em.createNamedQuery(
            "NativeQueryResultEntity.xmlFindNames").getResultList();
        assertNotNull(results);
        assertEquals(2, results.size());
        em.close();
    }
}

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

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that entities with @Id on field + @Column on getters (without
 * explicit @Access) work correctly. Reproduces TCK pattern where
 * a MappedSuperclass has @Id/@Version on fields and @Column on getters,
 * and a subclass entity has only @Column on getters.
 */
public class TestMixedAccess extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(MixedAccessBase.class, MixedAccessChild.class, CLEAR_TABLES);
    }

    public void testMetadataResolution() {
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        em.close();
    }

    public void testPersistAndFind() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MixedAccessChild emp = new MixedAccessChild();
        emp.setId("E1");
        emp.setFirstName("John");
        emp.setSalary(50000f);

        em.persist(emp);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        MixedAccessChild found = em.find(MixedAccessChild.class, "E1");
        assertNotNull(found);
        assertEquals("John", found.getFirstName());
        assertEquals(50000f, found.getSalary(), 0.01f);
        em.close();
    }
}

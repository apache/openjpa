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
package org.apache.openjpa.persistence.callbacks;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that when an entity-listener class has both @PostLoad annotation
 * and an XML orm.xml &lt;post-load&gt; declaration for the same method,
 * the callback fires only once (XML overrides annotation per JPA spec).
 *
 * Regression test for OPENJPA-2940: XML callback listeners should not
 * duplicate annotation-declared callbacks.
 */
public class TestXmlCallbackDedup extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "xml-callback-dedup-pu";
    }

    /**
     * Verifies that a @PostLoad callback fires exactly once when the same
     * listener method is declared in both annotations and XML.
     */
    public void testPostLoadCallbackNotDuplicated() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            XmlCallbackEntity entity = new XmlCallbackEntity("test1", "Test");
            em.persist(entity);
            em.flush();

            // Clear and re-query to trigger PostLoad
            em.clear();
            Query q = em.createQuery(
                "SELECT e FROM XmlCallbackEntity e WHERE e.id = 'test1'");
            List<?> results = q.getResultList();
            assertFalse("Expected at least one result", results.isEmpty());

            XmlCallbackEntity loaded = (XmlCallbackEntity) results.get(0);
            List<String> calls = loaded.getPostLoadCalls();

            // The listener should fire exactly once, not twice
            assertEquals("PostLoad should fire exactly once, not duplicated",
                1, calls.size());
            assertEquals("XmlCallbackListener", calls.get(0));

            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}

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

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

/**
 * The properties returned by the factory are cached on first access. Verifies that the cache
 * is invalidated once the entity manager level defaults become known, so that the outcome does
 * not depend on whether an entity manager was created before the first getProperties() call.
 */
public class TestEMFPropertiesCache extends AbstractPersistenceTestCase {

    public void testEmDefaultsAppearAfterFirstEntityManagerIsCreated() {
        OpenJPAEntityManagerFactorySPI emf = createEMF();
        try {
            // first access happens before any entity manager exists
            Map<String, Object> before = emf.getProperties();
            assertNotNull(before);

            EntityManager em = emf.createEntityManager();
            try {
                Map<String, Object> emProps = em.getProperties();
                Map<String, Object> after = emf.getProperties();
                for (String key : emProps.keySet()) {
                    assertTrue("EntityManager property " + key + " is missing from the factory properties",
                        after.containsKey(key));
                }
            } finally {
                closeEM(em);
            }
        } finally {
            closeEMF(emf);
        }
    }

    public void testEmDefaultsArePresentWhenEntityManagerIsCreatedFirst() {
        OpenJPAEntityManagerFactorySPI emf = createEMF();
        try {
            EntityManager em = emf.createEntityManager();
            try {
                Map<String, Object> emProps = em.getProperties();
                Map<String, Object> props = emf.getProperties();
                for (String key : emProps.keySet()) {
                    assertTrue("EntityManager property " + key + " is missing from the factory properties",
                        props.containsKey(key));
                }
            } finally {
                closeEM(em);
            }
        } finally {
            closeEMF(emf);
        }
    }
}

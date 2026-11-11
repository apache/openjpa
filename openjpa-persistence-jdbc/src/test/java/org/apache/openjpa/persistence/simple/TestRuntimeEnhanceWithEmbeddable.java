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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Date;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests runtime enhancement with embeddable + entity (property access).
 * Reproduces the TCK ShelfLife + Product pattern that causes NPE
 * during enhancement when both redefine and createSubclass are active.
 */
public class TestRuntimeEnhanceWithEmbeddable extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedProductEntity.class,
            UnenhancedShelfLife.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false",
            "openjpa.Compatibility", "StrictIdentityValues=true");
    }

    /**
     * Test: persist entity with embedded, then update via merge.
     * This triggers enhancement of both the entity and embeddable.
     */
    @Test
    public void testPersistAndMergeWithEmbeddable() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedProductEntity product = new UnenhancedProductEntity();
        product.setId("P1");
        product.setName("Widget");
        product.setPrice(10.0);
        product.setShelfLife(new UnenhancedShelfLife(
            Date.valueOf("2025-01-01"), Date.valueOf("2026-01-01")));
        em.persist(product);
        em.flush();

        // Update price after persist (TCK pattern)
        product.setPrice(25.0);
        em.merge(product);
        em.flush();

        em.getTransaction().commit();
        em.close();

        // Verify in new EM
        em = emf.createEntityManager();
        UnenhancedProductEntity found = em.find(UnenhancedProductEntity.class, "P1");
        assertNotNull(found, "Product should be found");
        assertEquals(25.0, found.getPrice(), 0.001, "Price should be 25.0");
        assertNotNull(found.getShelfLife(), "ShelfLife should not be null");
        assertEquals(Date.valueOf("2025-01-01"),
            found.getShelfLife().getInceptionDate(), "InceptionDate should match");
        em.close();
    }

    /**
     * Test: persist + set + merge + query (full TCK pattern with embeddable).
     */
    @Test
    public void testQueryAfterMergeWithEmbeddable() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedProductEntity p1 = new UnenhancedProductEntity();
        p1.setId("P10");
        p1.setName("Alpha");
        em.persist(p1);
        em.flush();

        UnenhancedProductEntity p2 = new UnenhancedProductEntity();
        p2.setId("P11");
        p2.setName("Beta");
        em.persist(p2);
        em.flush();

        // Set prices after persist
        p1.setPrice(100.0);
        em.merge(p1);
        em.flush();

        p2.setPrice(200.0);
        em.merge(p2);
        em.flush();

        em.getTransaction().commit();
        em.close();

        // Query
        em = emf.createEntityManager();
        em.getTransaction().begin();

        List<?> results = em.createQuery(
            "SELECT p FROM UnenhancedProductEntity p WHERE p.price > 150").getResultList();
        assertEquals(1, results.size(), "Should find 1 product with price > 150");

        em.getTransaction().commit();
        em.close();
    }
}

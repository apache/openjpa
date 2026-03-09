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

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that runtime subclass enhancement works for entities extending
 * a MappedSuperclass with a protected no-arg constructor.
 * Mirrors TCK test ee.jakarta.tck.persistence.core.entitytest.apitests.Client
 * which uses Coffee extending CoffeeMappedSC (protected constructor).
 * Before the fix, this threw VerifyError: "Bad access to protected init method"
 * in the generated pcsubclass writeReplace() method.
 */
public class TestUnenhancedProtectedConstructor extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedCoffeeEntity.class,
            UnenhancedMappedSuperclass.class,
            "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * Mirrors TCK setup — pcsubclass generation triggers writeReplace().
     */
    public void testCreateEMWithProtectedConstructorSuperclass() {
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        em.close();
    }

    /**
     * Mirrors TCK createTestData + entityAPITest2 (find) + entityAPITest5 (remove).
     * Persist multiple entities, find by PK, remove, verify removal.
     */
    public void testPersistFindRemove() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new UnenhancedCoffeeEntity(1, "hazelnut", 1.0f));
        em.persist(new UnenhancedCoffeeEntity(2, "vanilla creme", 2.0f));
        em.persist(new UnenhancedCoffeeEntity(3, "decaf", 3.0f));
        em.persist(new UnenhancedCoffeeEntity(4, "breakfast blend", 4.0f));
        em.persist(new UnenhancedCoffeeEntity(5, "mocha", 5.0f));
        em.getTransaction().commit();
        em.clear();

        // find
        UnenhancedCoffeeEntity found = em.find(UnenhancedCoffeeEntity.class, 1);
        assertNotNull("Entity should be found by PK", found);
        assertEquals("hazelnut", found.getBrandName());
        assertEquals(Float.valueOf(1.0f), found.getPrice());

        // find returns null for non-existent PK (TCK entityAPITest2)
        UnenhancedCoffeeEntity notFound =
            em.find(UnenhancedCoffeeEntity.class, 99);
        assertNull("find() should return null for non-existent PK", notFound);

        // remove (TCK entityAPITest5)
        em.getTransaction().begin();
        UnenhancedCoffeeEntity toRemove =
            em.find(UnenhancedCoffeeEntity.class, 1);
        em.remove(toRemove);
        em.getTransaction().commit();
        em.clear();

        assertNull("Removed entity should not be found",
            em.find(UnenhancedCoffeeEntity.class, 1));

        em.close();
    }

    /**
     * Mirrors TCK entityAPITest8 (merge).
     * Modify a detached entity, merge it back, verify update.
     */
    public void testMerge() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new UnenhancedCoffeeEntity(10, "original", 5.0f));
        em.getTransaction().commit();
        em.clear();

        // detach and modify
        UnenhancedCoffeeEntity detached =
            em.find(UnenhancedCoffeeEntity.class, 10);
        em.detach(detached);
        detached.setBrandName("updated");
        detached.setPrice(9.99f);

        // merge
        em.getTransaction().begin();
        UnenhancedCoffeeEntity merged = em.merge(detached);
        em.getTransaction().commit();
        em.clear();

        UnenhancedCoffeeEntity found =
            em.find(UnenhancedCoffeeEntity.class, 10);
        assertNotNull(found);
        assertEquals("updated", found.getBrandName());
        assertEquals(Float.valueOf(9.99f), found.getPrice());

        em.close();
    }

    /**
     * Mirrors TCK entityAPITest14 (JPQL query on inherited field).
     * Query using a field inherited from the MappedSuperclass.
     */
    public void testQueryInheritedField() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new UnenhancedCoffeeEntity(20, "decaf blend", 3.0f));
        em.persist(new UnenhancedCoffeeEntity(21, "regular", 4.0f));
        em.persist(new UnenhancedCoffeeEntity(22, "decaf mocha", 5.0f));
        em.getTransaction().commit();
        em.clear();

        List<UnenhancedCoffeeEntity> results = em.createQuery(
            "SELECT c FROM UnenhancedCoffeeEntity c WHERE c.brandName LIKE '%decaf%'",
            UnenhancedCoffeeEntity.class).getResultList();

        assertEquals("Should find 2 decaf coffees", 2, results.size());
        for (UnenhancedCoffeeEntity c : results) {
            assertTrue("Brand should contain 'decaf'",
                c.getBrandName().contains("decaf"));
        }

        em.close();
    }
}

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
package org.apache.openjpa.persistence.entitymanager;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests EntityManager spec compliance for JPA 3.2 TCK failures:
 * - CriteriaDelete/CriteriaUpdate createQuery + executeUpdate with rollback
 * - getReference with non-entity class causes rollback
 * - find with invalid PK type throws IllegalArgumentException
 * - merge of new entity
 */
public class TestEntityManagerSpecCompliance extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(UnenhancedOrder.class, UnenhancedCoffee.class,
            UnenhancedDoesNotExist.class,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            DROP_TABLES);
        // Drop the DOESNOTEXIST table so CriteriaDelete/Update tests
        // get the expected error when executing against it
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("DROP TABLE DOESNOTEXIST").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    /**
     * TCK: entityManagerMethodsRuntimeExceptionsCauseRollback5Test
     * createQuery(CriteriaDelete) on non-existent table, executeUpdate
     * should throw RuntimeException and mark tx for rollback.
     */
    public void testCriteriaDeleteRollback() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaDelete<UnenhancedDoesNotExist> cd =
                cb.createCriteriaDelete(UnenhancedDoesNotExist.class);
            cd.from(UnenhancedDoesNotExist.class);
            try {
                Query q = em.createQuery(cd);
                q.executeUpdate();
                fail("RuntimeException not thrown for CriteriaDelete on non-existent table");
            } catch (RuntimeException e) {
                // expected
                assertTrue("Transaction should be marked for rollback",
                    em.getTransaction().getRollbackOnly());
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: entityManagerMethodsRuntimeExceptionsCauseRollback7Test
     * createQuery(CriteriaUpdate) on non-existent table, executeUpdate
     * should throw RuntimeException and mark tx for rollback.
     */
    public void testCriteriaUpdateRollback() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<UnenhancedDoesNotExist> cu =
                cb.createCriteriaUpdate(UnenhancedDoesNotExist.class);
            Root<UnenhancedDoesNotExist> root = cu.from(UnenhancedDoesNotExist.class);
            cu.where(cb.equal(root.get("id"), 1));
            cu.set(root.get("firstName"), "foobar");
            try {
                Query q = em.createQuery(cu);
                q.executeUpdate();
                fail("RuntimeException not thrown for CriteriaUpdate on non-existent table");
            } catch (RuntimeException e) {
                // expected
                assertTrue("Transaction should be marked for rollback",
                    em.getTransaction().getRollbackOnly());
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: entityManagerMethodsRuntimeExceptionsCauseRollback23Test
     * getReference with non-entity class should throw RuntimeException
     * and mark tx for rollback.
     */
    public void testGetReferenceNonEntityRollback() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            // TestEntityManagerSpecCompliance is not an entity
            em.getReference(TestEntityManagerSpecCompliance.class, "doesnotexist");
            fail("RuntimeException not thrown for getReference with non-entity");
        } catch (RuntimeException e) {
            // expected
            assertTrue("Transaction should be marked for rollback",
                em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: entityAPITest4
     * find(Coffee.class, longId) where Coffee PK is Integer should throw
     * IllegalArgumentException for wrong PK type.
     */
    public void testFindInvalidPKType() {
        EntityManager em = emf.createEntityManager();
        try {
            long longId = 55L;
            // UnenhancedCoffee has Integer PK, passing long should throw IAE
            em.find(UnenhancedCoffee.class, longId);
            fail("IllegalArgumentException not thrown for invalid PK type");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    /**
     * TCK: findExceptionsTest - tests various find() overloads for proper
     * exception handling with invalid args.
     */
    public void testFindExceptions() {
        EntityManager em = emf.createEntityManager();
        int pass = 0;
        Map<String, Object> myMap = new HashMap<>();
        myMap.put("some.cts.specific.property", "nothing.in.particular");

        // Test 1: find(Class, Object) with non-entity class
        try {
            em.getTransaction().begin();
            em.find(TestEntityManagerSpecCompliance.class, 1);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for non-entity find");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 2: find(Class, Object) with wrong PK type
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, "PK");
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for invalid PK type");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 3: find(Class, Object) with null PK
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, null);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for null PK");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 4: find(Class, Object, Map) with non-entity class
        try {
            em.getTransaction().begin();
            em.find(TestEntityManagerSpecCompliance.class, 1, myMap);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for non-entity find with map");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 5: find(Class, Object, Map) with wrong PK type
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, "PK", myMap);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for invalid PK type with map");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 6: find(Class, Object, Map) with null PK
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, null, myMap);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for null PK with map");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 7: find(Class, Object, LockModeType) with non-entity
        try {
            em.getTransaction().begin();
            em.find(TestEntityManagerSpecCompliance.class, 1, LockModeType.NONE);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for non-entity find with lock");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 8: find(Class, Object, LockModeType) with wrong PK type
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, "PK", LockModeType.NONE);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for invalid PK type with lock");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 9: find(Class, Object, LockModeType) with null PK
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, null, LockModeType.NONE);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown for null PK with lock");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 10: find(Class, Object, LockModeType) without tx - TransactionRequiredException
        try {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } catch (Exception e) { /* ignore */ }
        try {
            em.find(UnenhancedOrder.class, 1, LockModeType.PESSIMISTIC_READ);
            fail("TransactionRequiredException not thrown");
        } catch (TransactionRequiredException e) {
            pass++;
        }

        // Test 11: find(Class, Object, LockModeType, Map) with non-entity
        try {
            em.getTransaction().begin();
            em.find(TestEntityManagerSpecCompliance.class, 1, LockModeType.NONE, myMap);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 12: find(Class, Object, LockModeType, Map) with wrong PK type
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, "PK", LockModeType.NONE, myMap);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 13: find(Class, Object, LockModeType, Map) with null PK
        try {
            em.getTransaction().begin();
            em.find(UnenhancedOrder.class, null, LockModeType.NONE, myMap);
            em.getTransaction().commit();
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            pass++;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Test 14: find(Class, Object, LockModeType, Map) without tx
        try {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } catch (Exception e) { /* ignore */ }
        try {
            em.find(UnenhancedOrder.class, 0, LockModeType.PESSIMISTIC_READ, myMap);
            fail("TransactionRequiredException not thrown");
        } catch (TransactionRequiredException e) {
            pass++;
        }

        em.close();

        assertEquals("Expected all 14 checks to pass", 14, pass);
    }

    /**
     * TCK: mergeTest
     * merge(new Order) should persist the new entity.
     */
    public void testMergeNewEntity() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            UnenhancedOrder o1 = em.merge(
                new UnenhancedOrder(9, 999, "desc999"));
            em.getTransaction().commit();

            emf.getCache().evictAll();

            UnenhancedOrder o2 = em.find(UnenhancedOrder.class, 9);
            assertNotNull("Merged entity should be findable", o2);
            assertEquals(o1, o2);
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }
}

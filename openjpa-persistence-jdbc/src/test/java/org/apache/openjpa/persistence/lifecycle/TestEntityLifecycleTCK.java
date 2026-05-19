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
package org.apache.openjpa.persistence.lifecycle;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.lifecycle.entity.CascadeManyToOneA;
import org.apache.openjpa.persistence.lifecycle.entity.CascadeManyToOneB;
import org.apache.openjpa.persistence.lifecycle.entity.CascadeOneToOneA;
import org.apache.openjpa.persistence.lifecycle.entity.CascadeOneToOneB;
import org.apache.openjpa.persistence.lifecycle.entity.PersistManyToOneA;
import org.apache.openjpa.persistence.lifecycle.entity.PersistManyToOneB;
import org.apache.openjpa.persistence.lifecycle.entity.PersistOneToOneA;
import org.apache.openjpa.persistence.lifecycle.entity.PersistOneToOneB;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Reproduces JPA TCK entity lifecycle test failures related to:
 * - Cascade remove on OneToOne with cascade=ALL and orphanRemoval
 * - Cascade remove on ManyToOne with cascade=ALL
 * - Remove of detached entity throwing IllegalArgumentException
 * - Persist of removed entity with cascade persist
 *
 * The root cause was that orphanRemoval=true was overwriting
 * cascade=REMOVE/ALL's CASCADE_IMMEDIATE with CASCADE_AUTO,
 * preventing immediate cascade of remove operations.
 */
public class TestEntityLifecycleTCK extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES,
              CascadeOneToOneA.class,
              CascadeOneToOneB.class,
              CascadeManyToOneA.class,
              CascadeManyToOneB.class,
              PersistOneToOneA.class,
              PersistOneToOneB.class,
              PersistManyToOneA.class,
              PersistManyToOneB.class);
    }

    // ====================================================================
    // cascadeall.oneXone tests (cascadeAll1X1Test8, 9, 10)
    // ====================================================================

    /**
     * TCK cascadeAll1X1Test8: Remove B cascades to A via cascade=ALL.
     * After em.remove(B), em.contains(A) should be false.
     * Verifies cascade remove + orphanRemoval do not conflict.
     */
    public void testCascadeRemoveOneToOne() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CascadeOneToOneA a1 = new CascadeOneToOneA("8", "a8", 8);
            CascadeOneToOneB bRef = new CascadeOneToOneB("8", "a8", 8, a1);
            em.persist(bRef);

            CascadeOneToOneA a2 = bRef.getA1();
            assertTrue("bRef should be managed", em.contains(bRef));
            assertSame("a2 should be same as a1", a1, a2);

            // Remove B - should cascade to A
            CascadeOneToOneB foundB = em.find(CascadeOneToOneB.class, "8");
            em.remove(foundB);

            // After cascade remove, A should no longer be contained
            assertFalse("After cascade remove of B, A should not be contained",
                        em.contains(a2));
            assertFalse("After remove, B should not be contained",
                        em.contains(bRef));

            em.flush();

            // After flush, find should return null
            CascadeOneToOneA stillExists =
                em.find(CascadeOneToOneA.class, "8");
            assertNull("A should be removed from DB after cascade",
                       stillExists);

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK cascadeAll1X1Test9: Remove B, contains(A) returns false
     * because remove cascades from B to A via cascade=ALL.
     */
    public void testCascadeRemoveContains() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CascadeOneToOneA a1 = new CascadeOneToOneA("9", "a9", 9);
            CascadeOneToOneB bRef = new CascadeOneToOneB("9", "a9", 9, a1);
            em.persist(bRef);

            CascadeOneToOneA a2 = bRef.getA1();
            assertTrue("bRef should be managed", em.contains(bRef));
            assertSame(a1, a2);

            CascadeOneToOneB newB = em.find(CascadeOneToOneB.class, "9");
            em.remove(newB);

            assertFalse(
                "After cascade remove of B, contains(A) should be false",
                em.contains(a2));

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK cascadeAll1X1Test10: Remove new (unmanaged) entity B cascades
     * remove to managed A. Per JPA spec, remove on new entity is ignored
     * but cascade still applies to referenced managed entities.
     */
    public void testCascadeRemoveNewEntity() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CascadeOneToOneA a1 = new CascadeOneToOneA("10", "a10", 10);
            em.persist(a1);

            CascadeOneToOneB bRef =
                new CascadeOneToOneB("10", "b10", 10, a1);
            // bRef is new (not persisted)
            assertTrue("a1 should be managed", em.contains(a1));
            em.remove(bRef);

            boolean status = em.contains(a1);
            assertFalse(
                "After cascade remove through new B, a1 should not be "
                + "contained", status);

            em.flush();

            CascadeOneToOneA stillExists =
                em.find(CascadeOneToOneA.class, "10");
            assertNull("A should be removed after cascade", stillExists);

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    // ====================================================================
    // cascadeall.manyXone test (cascadeAllMX1Test2)
    // ====================================================================

    /**
     * TCK cascadeAllMX1Test2: Remove ManyToOne entities then re-persist.
     * After remove+flush, persist(B) should make B managed again
     * and cascade persist to A.
     */
    public void testCascadeAllManyToOnePersistAfterRemove() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CascadeManyToOneA aRef = new CascadeManyToOneA("2", "bean2", 2);
            CascadeManyToOneB b1 =
                new CascadeManyToOneB("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            CascadeManyToOneA newA1 = b1.getA1Info();
            assertNotNull("A should be persisted via cascade", newA1);

            // Remove both A and B
            em.remove(newA1);
            em.remove(b1);
            em.flush();

            // After flush, find should return null
            CascadeManyToOneB newB = em.find(CascadeManyToOneB.class, "2");
            assertNull("B should be removed", newB);

            // Re-persist b1 - should cascade to A
            em.persist(b1);
            em.flush();
            assertTrue("After re-persist, B should be managed",
                       em.contains(b1));
            assertNotNull("After re-persist, B.a1 should not be null",
                          b1.getA1());

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    // ====================================================================
    // detach tests (detachBasicTest1, detach1X1Test2, detach1XMTest2)
    // ====================================================================

    /**
     * TCK detachBasicTest1: Remove on a detached entity should throw
     * IllegalArgumentException or cause commit failure.
     */
    public void testRemoveDetachedThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            // Persist entity in separate transaction
            em.getTransaction().begin();
            CascadeOneToOneA aRef = new CascadeOneToOneA("1", "a1", 1);
            em.persist(aRef);
            em.getTransaction().commit();

            // Clear cache to detach
            em.clear();
            if (em.getEntityManagerFactory().getCache() != null) {
                em.getEntityManagerFactory().getCache().evictAll();
            }

            // Now aRef is detached
            em.getTransaction().begin();
            assertFalse("After clear, entity should be detached",
                        em.contains(aRef));

            try {
                em.remove(aRef);
                // If no exception, commit should fail
                em.getTransaction().commit();
                fail("Expected IllegalArgumentException or commit failure "
                     + "for remove of detached entity");
            } catch (IllegalArgumentException iae) {
                // Expected per JPA spec
            } catch (Exception e) {
                // PersistenceException on commit is also acceptable
                if (!(e instanceof jakarta.persistence.PersistenceException)) {
                    fail("Expected IllegalArgumentException or "
                         + "PersistenceException, got: " + e.getClass());
                }
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK detach1X1Test2: Remove on a detached OneToOne entity should
     * throw IllegalArgumentException.
     */
    public void testRemoveDetachedOneToOneThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            // Create and persist B with its related A
            em.getTransaction().begin();
            CascadeOneToOneA aRef = new CascadeOneToOneA("d1", "ad1", 1);
            CascadeOneToOneB bRef =
                new CascadeOneToOneB("d1", "bd1", 1, aRef);
            em.persist(bRef);
            em.getTransaction().commit();

            // Clear to detach
            em.clear();
            if (em.getEntityManagerFactory().getCache() != null) {
                em.getEntityManagerFactory().getCache().evictAll();
            }

            em.getTransaction().begin();
            assertFalse("After clear, B should be detached",
                        em.contains(bRef));

            try {
                em.remove(bRef);
                em.getTransaction().commit();
                fail("Expected IllegalArgumentException or commit failure");
            } catch (IllegalArgumentException iae) {
                // Expected
            } catch (Exception e) {
                if (!(e instanceof jakarta.persistence.PersistenceException)) {
                    fail("Expected IllegalArgumentException or "
                         + "PersistenceException, got: " + e.getClass());
                }
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    // ====================================================================
    // persist tests (persist1X1Test2, persistMX1Test2)
    // ====================================================================

    /**
     * TCK persist1X1Test2: Persist a removed OneToOne entity - it
     * becomes managed. Uses cascade=PERSIST with orphanRemoval=true.
     */
    public void testPersistRemovedOneToOne() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            PersistOneToOneA a1 = new PersistOneToOneA("2", "a2", 2);
            PersistOneToOneB bRef = new PersistOneToOneB("2", "b2", 2, a1);
            em.persist(bRef);

            PersistOneToOneB foundB =
                em.find(PersistOneToOneB.class, "2");
            assertTrue("B should be managed", em.contains(foundB));

            // Remove B and flush
            em.remove(em.find(PersistOneToOneB.class, "2"));
            em.flush();

            // Re-persist the removed entity
            em.persist(bRef);
            assertTrue("After re-persist, B should be managed",
                       em.contains(bRef));

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK persistMX1Test2: Persist a removed ManyToOne entity.
     * After remove+flush, persist(B) should cascade persist to A,
     * making both managed.
     */
    public void testPersistRemovedManyToOne() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            PersistManyToOneA aRef = new PersistManyToOneA("2", "bean2", 2);
            PersistManyToOneB b1 =
                new PersistManyToOneB("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            PersistManyToOneA newA1 = b1.getA1Info();
            assertNotNull("A should be persisted via cascade", newA1);

            // Remove A and B, flush
            em.remove(newA1);
            em.remove(b1);
            em.flush();

            // Verify B is removed
            PersistManyToOneB newB = em.find(PersistManyToOneB.class, "2");
            assertNull("B should be removed", newB);

            // Re-persist b1 (cascade=PERSIST cascades to A)
            em.persist(b1);
            em.flush();
            assertTrue("After re-persist, B should be managed",
                       em.contains(b1));
            assertNotNull("After re-persist, B.a1 should not be null",
                          b1.getA1());

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK cascadeAll1X1Test2: Persist a removed entity with cascade=ALL.
     * Same as persist test but with cascade=ALL + orphanRemoval.
     */
    public void testPersistRemovedCascadeAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CascadeOneToOneA a1 = new CascadeOneToOneA("2", "a2", 2);
            CascadeOneToOneB bRef = new CascadeOneToOneB("2", "b2", 2, a1);
            em.persist(bRef);

            CascadeOneToOneB foundB =
                em.find(CascadeOneToOneB.class, "2");
            assertTrue("B should be managed", em.contains(foundB));

            // Remove B and flush (cascade=ALL cascades remove to A)
            em.remove(em.find(CascadeOneToOneB.class, "2"));
            em.flush();

            // Re-persist the removed entity
            em.persist(bRef);
            assertTrue("After re-persist, B should be managed",
                       em.contains(bRef));

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}

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
 * Tests JPA spec compliance for entity lifecycle operations:
 * <ul>
 * <li>find() throws IAE for wrong PK type</li>
 * <li>remove() throws IAE (or commit fails) for detached entities</li>
 * <li>persist() on a removed entity makes it managed again</li>
 * </ul>
 */
public class TestEntityLifecycleSpec extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(MxOneParent.class, MxOneChild.class, CoffeeBean.class,
            DROP_TABLES);
    }

    /**
     * JPA spec: find(Class, wrongType) must throw IllegalArgumentException
     * if the second argument is not a valid type for the entity's PK.
     * Coffee has Integer PK; passing Long should throw IAE.
     */
    public void testFindWithWrongPKTypeThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            em.find(CoffeeBean.class, 55L);
            fail("Expected IllegalArgumentException for wrong PK type "
                + "(Long instead of Integer)");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    /**
     * JPA spec: remove() on a detached entity must throw IAE
     * or the transaction commit must fail.
     * Create entity, persist in one tx, then in a new tx try remove.
     */
    public void testRemoveDetachedEntityThrowsIAE() {
        MxOneParent a = new MxOneParent("1", "parent1", 1);

        // persist in first transaction
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(a);
        em.getTransaction().commit();
        em.close();

        // evict from cache
        emf.getCache().evictAll();

        // new EM - entity should be detached
        em = emf.createEntityManager();
        em.getTransaction().begin();
        assertFalse("Entity should be detached (not in new EM context)",
            em.contains(a));

        boolean pass = false;
        try {
            em.remove(a);
        } catch (IllegalArgumentException e) {
            pass = true;
        }

        if (!pass) {
            // if no IAE, the spec allows the commit to fail
            try {
                em.getTransaction().commit();
                // if commit succeeds, that's also a failure
            } catch (Exception e) {
                pass = true;
            }
        } else {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        assertTrue("remove() on detached entity should throw IAE "
            + "or cause transaction failure", pass);
        em.close();
    }

    /**
     * JPA spec: if X is a removed entity, persist(X) makes it managed.
     * persist B (cascade to A), flush, remove A+B, flush,
     * re-persist B, flush - should succeed with B managed.
     */
    public void testPersistAfterRemoveWithCascade() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MxOneParent a = new MxOneParent("2", "parent2", 2);
        MxOneChild b = new MxOneChild("2", "child2", 2, a);
        em.persist(b);
        em.flush();

        MxOneParent parentRef = b.getParent();
        assertNotNull("Parent should be set on child", parentRef);

        // remove both, flush
        em.remove(parentRef);
        em.remove(b);
        em.flush();

        // verify B is removed
        MxOneChild found = em.find(MxOneChild.class, "2");
        assertNull("Child should be removed from DB", found);

        // re-persist
        em.persist(b);
        em.flush();

        assertTrue("B should be managed after re-persist",
            em.contains(b));
        assertNotNull("B should still reference parent",
            b.getParent());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Same as testPersistAfterRemoveWithCascade but without the parent
     * reference - tests simpler persist-remove-persist cycle.
     */
    public void testPersistAfterRemoveSimple() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MxOneParent a = new MxOneParent("3", "parent3", 3);
        em.persist(a);
        em.flush();

        // remove and flush
        em.remove(a);
        em.flush();

        // verify removed
        MxOneParent found = em.find(MxOneParent.class, "3");
        assertNull("Entity should be removed", found);

        // re-persist
        em.persist(a);
        em.flush();

        assertTrue("Entity should be managed after re-persist",
            em.contains(a));

        em.getTransaction().commit();
        em.close();
    }
}

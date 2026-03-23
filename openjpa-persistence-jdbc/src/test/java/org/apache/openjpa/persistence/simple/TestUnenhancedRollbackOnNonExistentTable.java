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
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that CriteriaDelete/CriteriaUpdate on an entity whose table does
 * NOT exist throws a RuntimeException and marks the transaction for rollback.
 * Mirrors TCK tests:
 * - entityManagerMethodsRuntimeExceptionsCauseRollback5Test (CriteriaDelete)
 * - entityManagerMethodsRuntimeExceptionsCauseRollback7Test (CriteriaUpdate)
 *
 * Key: SynchronizeMappings is disabled (empty) so the table is NOT
 * auto-created. This mirrors the TCK scenario where the DoesNotExist
 * table is intentionally absent from the DDL scripts.
 */
public class TestUnenhancedRollbackOnNonExistentTable extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(UnenhancedDoesNotExistEntity.class,
              "openjpa.RuntimeUnenhancedClasses", "supported",
              "openjpa.jdbc.SynchronizeMappings", "");
    }

    /**
     * TCK: entityManagerMethodsRuntimeExceptionsCauseRollback5Test
     * CriteriaDelete on non-existent table should throw RuntimeException
     * and mark transaction for rollback.
     */
    public void testCriteriaDeleteOnNonExistentTable() {
        EntityManager em = emf.createEntityManager();
        boolean pass = false;
        try {
            em.getTransaction().begin();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaDelete<UnenhancedDoesNotExistEntity> cd =
                cb.createCriteriaDelete(UnenhancedDoesNotExistEntity.class);
            cd.from(UnenhancedDoesNotExistEntity.class);
            Query q = em.createQuery(cd);

            try {
                q.executeUpdate();
                fail("RuntimeException should have been thrown for "
                    + "CriteriaDelete on non-existent table");
            } catch (RuntimeException e) {
                // expected
                assertTrue("Transaction should be marked for rollback",
                    em.getTransaction().getRollbackOnly());
                pass = true;
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        assertTrue("Test should have passed", pass);
    }

    /**
     * TCK: entityManagerMethodsRuntimeExceptionsCauseRollback7Test
     * CriteriaUpdate on non-existent table should throw RuntimeException
     * and mark transaction for rollback.
     */
    public void testCriteriaUpdateOnNonExistentTable() {
        EntityManager em = emf.createEntityManager();
        boolean pass = false;
        try {
            em.getTransaction().begin();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<UnenhancedDoesNotExistEntity> cu =
                cb.createCriteriaUpdate(UnenhancedDoesNotExistEntity.class);
            Root<UnenhancedDoesNotExistEntity> root =
                cu.from(UnenhancedDoesNotExistEntity.class);
            cu.where(cb.equal(root.get("id"), 1));
            cu.set(root.get("firstName"), "foobar");
            Query q = em.createQuery(cu);

            try {
                q.executeUpdate();
                fail("RuntimeException should have been thrown for "
                    + "CriteriaUpdate on non-existent table");
            } catch (RuntimeException e) {
                // expected
                assertTrue("Transaction should be marked for rollback",
                    em.getTransaction().getRollbackOnly());
                pass = true;
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        assertTrue("Test should have passed", pass);
    }
}

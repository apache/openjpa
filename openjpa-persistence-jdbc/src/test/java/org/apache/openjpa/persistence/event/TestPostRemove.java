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
package org.apache.openjpa.persistence.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jta.TransactionWrapper;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests PostRemove callback within a pseudo-container environment.
 *
 * According to JPA 2.0 Specification Section 3.5.2 (edited for PostRemove only and readability)
 *
 * "a) The PostRemove callback method is invoked for an entity after the entity has been removed.
 *
 *  b) This callback will also be invoked on all entities to which these operations are cascaded.
 *
 *  c) The PostRemove method will be invoked after the database delete operation. The database operation
 *     may occur directly after the remove operation have been invoked or they may occur directly after
 *     a flush operation has occurred (which may be at the end of the transaction)."
 *  <br>
 *  The test runs within a test harness that emulates application managed transaction semantics
 *  of a container.
 *
 * @author Pinaki Poddar
 *
 */
public class TestPostRemove extends SingleEMFTestCase {
    @RegisterExtension
    TransactionWrapper wrapper = new TransactionWrapper();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(PostRemoveCallbackEntity.class);
    }

    @Override
    public String getPersistenceUnitName() {
        return "post-remove";
    }

    @Test
    void postRemoveInvokedOnlyAfterDatabaseDeleteWithLogicalFlush() {
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();
        PostRemoveCallbackEntity pc = new PostRemoveCallbackEntity();
        em.persist(pc);
        em.flush();
        em.remove(pc);
        wrapper.commit();
        assertTrue(isPostRemovedInvoked(pc)
                && pc.getPostRemoveTime() <= System.nanoTime(), "PostRemove not called after commit");
        em.close();
    }

    @Test
    void postRemoveInvokedAfterDatabaseDeleteWithoutFlush() {
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();
        PostRemoveCallbackEntity pc = new PostRemoveCallbackEntity();
        em.persist(pc);
        em.remove(pc);
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called before commit");
        wrapper.commit();
        assertTrue(pc.getPostRemoveTime() <= System.nanoTime(), "PostRemove not called after commit");
        em.close();
    }

    @Test
    void postRemoveNotInvokedAfterRollback() {
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();
        PostRemoveCallbackEntity pc = new PostRemoveCallbackEntity();
        em.persist(pc);
        em.remove(pc);
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called before rollback");
        wrapper.rollback();
        assertTrue(pc.getPostRemoveTime() <= System.nanoTime(), "PostRemove called after rollback");
        em.close();
    }

    @Test
    void postRemoveNotInvokedAfterRollbackWithIntermediateFlush() {
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();
        PostRemoveCallbackEntity pc = new PostRemoveCallbackEntity();
        em.persist(pc);
        em.flush();
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called after flush");
        em.remove(pc);
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called before rollback");
        wrapper.rollback();
        assertTrue(pc.getPostRemoveTime() <= System.nanoTime(), "PostRemove called after rollback");
        em.close();
    }

    @Test
    void postRemoveInvokedOnFlushThatIssuesDatabaseDelete() {
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();
        PostRemoveCallbackEntity pc = new PostRemoveCallbackEntity();
        em.persist(pc);
        wrapper.commit();
        em.close();

        em = emf.createEntityManager();
        em.joinTransaction();
        pc = em.find(PostRemoveCallbackEntity.class, pc.getId());
        assertNotNull(pc);
        em.remove(pc);
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called after logical remove");
        em.flush();
        assertTrue(isPostRemovedInvoked(pc), "PostRemove not called after delete flush");
        wrapper.commit();
        assertTrue(pc.getPostRemoveTime() <= System.nanoTime(), "PostRemove not called after commit");
        em.close();
    }


    @Test
    void postRemoveNotInvokedAfterDatabaseInsert() {
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();
        PostRemoveCallbackEntity pc = new PostRemoveCallbackEntity();
        em.persist(pc);
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called before commit");
        wrapper.commit();
        assertFalse(isPostRemovedInvoked(pc), "PostRemove called after commit");
        em.close();
    }

    boolean isPostRemovedInvoked(PostRemoveCallbackEntity pc) {
        return pc.getPostRemoveTime() != 0;
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc.query.bulk;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

/**
 * <code>FlushAfterInMemoryBulkOperation=false</code> suppresses the context
 * flush that follows a bulk operation executed in memory. The operation itself
 * is still applied to the managed instances, so it is written by the next flush
 * or at commit - only the timing goes back to what 4.1.x did.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2951">OPENJPA-2951</A>
 */
public class TestInMemoryBulkFlushCompat extends AbstractInMemoryBulkFlushTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteCompositeOwner.class,
            BulkDeleteItem.class,
            "openjpa.Compatibility", "FlushAfterInMemoryBulkOperation=false");
        createData();
        sql.clear();
    }

    /**
     * Nothing is written during <code>executeUpdate()</code>, but the
     * instances are marked for deletion, so the commit deletes their rows.
     */
    public void testInMemoryDeleteIsNotFlushed() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, deleteInMemory(em));
            assertNotSQL("DELETE FROM .*BULK_CID_OWNER.*");
            assertNotSQL("DELETE FROM .*BULK_CID_TAGS.*");
            em.getTransaction().commit();
        }
        // the in-memory application of the operation still happened
        assertNull(ownerName());
    }

    /**
     * The same for an update: no statement during
     * <code>executeUpdate()</code>, but the managed instance carries the new
     * value and the commit writes it.
     */
    public void testInMemoryUpdateIsNotFlushed() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, updateInMemory(em));
            assertNotSQL("UPDATE .*BULK_CID_OWNER.*");
            em.getTransaction().commit();
        }
        assertEquals("changed", ownerName());
    }

    /**
     * The price of the suppressed flush: the reported row count has not
     * reached the database, so clearing the context before the commit loses
     * the operation outright.
     */
    public void testClearedContextLosesTheDelete() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, deleteInMemory(em));
            // the candidate SELECT pins the in-memory path; no DELETE follows
            assertSQL("SELECT .*FROM .*BULK_CID_OWNER.*");
            assertNotSQL("DELETE FROM .*BULK_CID_OWNER.*");
            em.clear();
            assertNotNull(em.find(BulkDeleteCompositeOwner.class, ownerId()));
            em.getTransaction().commit();
        }
        assertEquals("owner", ownerName());
    }

    /**
     * An unrelated pending change stays pending, which is what the option is
     * for.
     */
    public void testUnrelatedPendingChangeStaysPending() {
        try (EntityManager em = emf.createEntityManager()) {
            em.setFlushMode(FlushModeType.COMMIT);
            em.getTransaction().begin();
            em.find(BulkDeleteItem.class, 1L).setName("dirty");
            sql.clear();
            assertEquals(1, deleteInMemory(em));
            assertNotSQL("UPDATE .*BULK_ITEM.*");
            em.getTransaction().rollback();
        }
        assertEquals("item", unrelatedName());
    }
}

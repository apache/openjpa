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

import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * A bulk update or delete that has to be executed in memory applies the
 * operation to the managed instances, so by default the persistence context is
 * flushed afterwards and the reported row count has actually reached the
 * database.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2951">OPENJPA-2951</A>
 */
public class TestInMemoryBulkFlush extends AbstractInMemoryBulkFlushTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteCompositeOwner.class,
            BulkDeleteItem.class);
        createData();
        sql.clear();
    }

    /**
     * The deletes reach the database during <code>executeUpdate()</code>, so
     * a later <code>find()</code> in the same transaction does not see the
     * deleted instance even when the context has been cleared in between.
     */
    public void testInMemoryDeleteIsVisibleAfterClear() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, deleteInMemory(em));
            // the candidate SELECT proves the operation took the in-memory
            // path; the DELETE that follows it is the trailing flush
            assertAllSQLInOrder("SELECT .*FROM .*BULK_CID_OWNER.*",
                "DELETE FROM .*BULK_CID_OWNER WHERE .*");
            em.clear();
            assertNull(em.find(BulkDeleteCompositeOwner.class, ownerId()));
            em.getTransaction().commit();
        }
        assertNull(ownerName());
    }

    /**
     * The same for an update: the row carries the new value before the
     * transaction commits.
     */
    public void testInMemoryUpdateIsVisibleAfterClear() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, updateInMemory(em));
            // the candidate was loaded by the find() of updateInMemory(), so
            // a single-statement bulk UPDATE would have emitted no SELECT of
            // its own; the UPDATE that follows it is the trailing flush
            assertAllSQLInOrder("SELECT .*FROM .*BULK_CID_OWNER.*",
                "UPDATE .*BULK_CID_OWNER SET .*");
            em.clear();
            assertEquals("changed", em.find(BulkDeleteCompositeOwner.class,
                ownerId()).getName());
            em.getTransaction().commit();
        }
        assertEquals("changed", ownerName());
    }

    /**
     * The cost of the flush: it is the whole persistence context that is
     * flushed, so a pending change to an entity that has nothing to do with
     * the bulk delete is written as a side effect of
     * <code>executeUpdate()</code>. The flush mode has to be
     * <code>COMMIT</code> to show it for a query that is executed against the
     * datastore first - under <code>AUTO</code> that query flushes the context
     * before the bulk operation even starts.
     */
    public void testUnrelatedPendingChangeIsFlushedAsWell() {
        try (EntityManager em = emf.createEntityManager()) {
            em.setFlushMode(FlushModeType.COMMIT);
            em.getTransaction().begin();
            em.find(BulkDeleteItem.class, 1L).setName("dirty");
            sql.clear();
            assertEquals(1, deleteInMemory(em));
            assertSQL("UPDATE .*BULK_ITEM SET .*");
            em.getTransaction().rollback();
        }
    }

    /**
     * Under <code>FlushModeType.AUTO</code> the pending change is written
     * before the bulk operation, by the query that loads the candidates, so
     * the trailing flush adds no side effect of its own.
     */
    public void testUnrelatedPendingChangeIsFlushedByCandidateQuery() {
        try (EntityManager em = emf.createEntityManager()) {
            em.setFlushMode(FlushModeType.AUTO);
            em.getTransaction().begin();
            em.find(BulkDeleteItem.class, 1L).setName("dirty");
            sql.clear();
            assertEquals(1, deleteInMemory(em));
            assertAllSQLInOrder("UPDATE .*BULK_ITEM SET .*",
                "SELECT .*FROM .*BULK_CID_OWNER.*");
            em.getTransaction().rollback();
        }
    }

    /**
     * A query whose candidates were handed over with
     * <code>setCandidateCollection()</code> is executed in memory from the
     * start, so <code>QueryImpl.isInMemory()</code> short-circuits the
     * pre-flush and the trailing flush is the only flush - even under
     * <code>FlushModeType.AUTO</code>. The order of the statements shows it:
     * the candidate <code>SELECT</code> of the <code>find()</code> comes
     * before the write of the unrelated instance, so that write cannot come
     * from a pre-flush.
     */
    public void testCandidateCollectionUnderAutoFlushesUnrelatedChange() {
        try (EntityManager em = emf.createEntityManager()) {
            em.setFlushMode(FlushModeType.AUTO);
            em.getTransaction().begin();
            em.find(BulkDeleteItem.class, 1L).setName("dirty");
            sql.clear();
            assertEquals(1, updateInMemory(em));
            assertAllSQLInOrder("SELECT .*FROM .*BULK_CID_OWNER.*",
                "UPDATE .*BULK_ITEM SET .*");
            em.getTransaction().rollback();
        }
    }

    /**
     * A savepoint set through the default <code>in-mem</code>
     * <code>SavepointManager</code> cannot be flushed incrementally. The
     * trailing flush is then skipped instead of failing the bulk operation,
     * so the operation still reports its row count and is written at commit.
     */
    public void testSavepointSuppressesTheFlushInsteadOfFailing() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OpenJPAPersistence.cast(em).setSavepoint("sp");
            sql.clear();
            assertEquals(1, deleteInMemory(em));
            assertNotSQL("DELETE FROM .*BULK_CID_OWNER.*");
            em.getTransaction().commit();
        }
        assertNull(ownerName());
    }
}

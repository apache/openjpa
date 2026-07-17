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
package org.apache.openjpa.persistence.delete;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

/**
 * Deleting an entity whose database row has already been removed (e.g. by a
 * database-level ON DELETE CASCADE triggered by a related row's deletion)
 * must be tolerated for entities without a version, because the update count
 * of 0 carries no optimistic signal. For versioned entities the DELETE
 * statement includes the version in its WHERE clause, so an update count of
 * 0 signals a concurrent modification and must surface as an
 * {@link OptimisticLockException}.
 *
 * Exercises all three statement manager code paths: unbatched
 * (PreparedStatementManagerImpl.flushAndUpdate), a batch containing a single
 * row (BatchingPreparedStatementManagerImpl.flushSingleRow) and a batch of
 * several rows (BatchingPreparedStatementManagerImpl.checkUpdateCount).
 */
public class TestDeleteMissingRecord extends AbstractPersistenceTestCase {

    private EntityManagerFactory newEmf(int batchLimit) {
        return createEMF(
            DeleteUnversionedEntity.class, DeleteVersionedEntity.class,
            "openjpa.jdbc.SynchronizeMappings", "buildSchema",
            "openjpa.jdbc.DBDictionary", "batchLimit=" + batchLimit,
            CLEAR_TABLES);
    }

    public void testUnversionedDeleteMissingRecordUnbatched() {
        assertUnversionedDeleteTolerated(0, 1);
    }

    public void testVersionedDeleteMissingRecordUnbatched() {
        assertVersionedDeleteThrows(0, 1);
    }

    public void testUnversionedDeleteMissingRecordSingleRowBatch() {
        assertUnversionedDeleteTolerated(100, 1);
    }

    public void testVersionedDeleteMissingRecordSingleRowBatch() {
        assertVersionedDeleteThrows(100, 1);
    }

    public void testUnversionedDeleteMissingRecordMultiRowBatch() {
        assertUnversionedDeleteTolerated(100, 2);
    }

    public void testVersionedDeleteMissingRecordMultiRowBatch() {
        assertVersionedDeleteThrows(100, 2);
    }

    private void assertUnversionedDeleteTolerated(int batchLimit,
        int entityCount) {
        EntityManagerFactory emf = newEmf(batchLimit);
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            for (int i = 1; i <= entityCount; i++) {
                em.persist(new DeleteUnversionedEntity(i, "name" + i));
            }
            em.getTransaction().commit();

            em.getTransaction().begin();
            DeleteUnversionedEntity[] entities =
                new DeleteUnversionedEntity[entityCount];
            for (int i = 1; i <= entityCount; i++) {
                entities[i - 1] = em.find(DeleteUnversionedEntity.class, i);
                assertNotNull(entities[i - 1]);
            }
            deleteBehindBack(emf, "DEL_UNVER_ENT");
            for (DeleteUnversionedEntity e : entities) {
                em.remove(e);
            }
            // must not throw although the DELETEs affect no rows
            em.getTransaction().commit();
            em.close();
        } finally {
            closeEMF(emf);
        }
    }

    private void assertVersionedDeleteThrows(int batchLimit,
        int entityCount) {
        EntityManagerFactory emf = newEmf(batchLimit);
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            for (int i = 1; i <= entityCount; i++) {
                em.persist(new DeleteVersionedEntity(i, "name" + i));
            }
            em.getTransaction().commit();

            em.getTransaction().begin();
            DeleteVersionedEntity[] entities =
                new DeleteVersionedEntity[entityCount];
            for (int i = 1; i <= entityCount; i++) {
                entities[i - 1] = em.find(DeleteVersionedEntity.class, i);
                assertNotNull(entities[i - 1]);
            }
            deleteBehindBack(emf, "DEL_VER_ENT");
            for (DeleteVersionedEntity e : entities) {
                em.remove(e);
            }
            try {
                em.flush();
                fail("Expected an OptimisticLockException because the "
                    + "versioned row was concurrently deleted");
            } catch (OptimisticLockException expected) {
            } finally {
                em.getTransaction().rollback();
                em.close();
            }
        } finally {
            closeEMF(emf);
        }
    }

    /**
     * Delete all rows of the given table in a separate transaction, mimicking
     * a concurrent removal (or a database-level ON DELETE CASCADE) the
     * entity manager does not know about.
     */
    private void deleteBehindBack(EntityManagerFactory emf, String table) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("DELETE FROM " + table).executeUpdate();
        em.getTransaction().commit();
        em.close();
    }
}

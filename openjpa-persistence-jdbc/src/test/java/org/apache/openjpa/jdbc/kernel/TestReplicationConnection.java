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
package org.apache.openjpa.jdbc.kernel;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A replication connection, whose driver sends the statements of a read-only
 * connection to a replica, is kept read-only only outside of a transaction
 * (OPENJPA-2943). The sequence, the schema tool, bulk and native updates and
 * pessimistic locks, which do not all go through a flush, write on writable
 * connections. Derby stands in for the replica: it rejects the writes of a
 * read-only connection.
 */
public class TestReplicationConnection extends SingleEMFTestCase {

    @Override
    public void setUp() throws Exception {
        if (!isDerby()) {
            return;
        }
        // the schema is created through connections of the dictionary
        super.setUp(DROP_TABLES, ReplicatedEntity.class,
            "openjpa.jdbc.DBDictionary", ReplicationDerbyDictionary.class.getName());
    }

    private static boolean isDerby() {
        String url = System.getProperty("openjpa.ConnectionURL", "jdbc:derby:");
        return url.startsWith("jdbc:derby:");
    }

    @Override
    public void tearDown() throws Exception {
        if (emf != null) {
            super.tearDown();
        }
    }

    public void testReadOnlyOutsideOfTransaction() throws Exception {
        if (emf == null) {
            return;
        }
        OpenJPAEntityManager em = OpenJPAPersistence.cast(emf.createEntityManager());
        try {
            Connection conn = (Connection) em.getConnection();
            try {
                assertTrue(conn.isReadOnly());
                try (Statement stmnt = conn.createStatement()) {
                    stmnt.executeUpdate("UPDATE OJ2943_REPLICATED SET name = 'x'");
                    fail("a read-only connection must not write");
                } catch (SQLException expected) {
                    // as a replica rejects the write
                }
            } finally {
                conn.close();
            }
        } finally {
            em.close();
        }
    }

    public void testWritesInTransaction() {
        if (emf == null) {
            return;
        }
        EntityManager em = emf.createEntityManager();
        try {
            assertWrites(em);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * A connection retained from outside of a transaction is read-only until
     * the transaction begins.
     */
    public void testWritesInTransactionOnRetainedConnection() throws Exception {
        if (emf == null) {
            return;
        }
        OpenJPAEntityManager em = OpenJPAPersistence.cast(emf.createEntityManager(
            Collections.singletonMap("openjpa.ConnectionRetainMode", "always")));
        try {
            Connection conn = (Connection) em.getConnection();
            try {
                assertTrue(conn.isReadOnly());
            } finally {
                conn.close();
            }
            assertWrites(em);

            em.getTransaction().begin();
            conn = (Connection) em.getConnection();
            try {
                assertFalse(conn.isReadOnly());
            } finally {
                conn.close();
            }
            em.getTransaction().rollback();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Writes that go through a flush, and the ones that do not: the sequence,
     * bulk and native updates, and a pessimistic lock.
     */
    private void assertWrites(EntityManager em) {
        em.getTransaction().begin();
        ReplicatedEntity e = new ReplicatedEntity("a");
        em.persist(e);
        em.getTransaction().commit();
        long id = e.getId();

        em.getTransaction().begin();
        assertEquals(1, em.createQuery("UPDATE ReplicatedEntity e SET e.name = 'b' WHERE e.id = :id")
            .setParameter("id", id).executeUpdate());
        em.getTransaction().commit();

        em.getTransaction().begin();
        assertEquals(1, em.createNativeQuery("UPDATE OJ2943_REPLICATED SET name = 'c' WHERE id = ?")
            .setParameter(1, id).executeUpdate());
        em.getTransaction().commit();

        em.clear();
        em.getTransaction().begin();
        ReplicatedEntity found = em.find(ReplicatedEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        assertEquals("c", found.getName());
        found.setName("d");
        em.getTransaction().commit();

        em.clear();
        assertEquals("d", em.find(ReplicatedEntity.class, id).getName());
    }
}

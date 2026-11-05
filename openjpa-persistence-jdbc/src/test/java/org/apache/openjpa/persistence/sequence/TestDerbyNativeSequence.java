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
package org.apache.openjpa.persistence.sequence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.NativeJDBCSeq;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Apache Derby has sequence objects since 10.6, so a
 * <code>@SequenceGenerator</code> is backed by a real sequence there rather
 * than by a row of the OpenJPA sequence table (OPENJPA-3034).
 */
public class TestDerbyNativeSequence extends SQLListenerTestCase {

    @Override
    public void setUp() throws Exception {
        setUp(NamedSeqGenEntity.class, CLEAR_TABLES);
    }

    private DBDictionary dict() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        if (!dict.isConnected()) {
            conf.getConnectionFactory();
        }
        return dict;
    }

    private boolean isDerby() {
        if (dict() instanceof DerbyDictionary) {
            return true;
        }
        System.out.println(getClass().getName() + "." + getName() + " skipped: not running on Apache Derby");
        return false;
    }

    public void testDerbyDeclaresSequences() {
        if (!isDerby()) {
            return;
        }
        DBDictionary dict = dict();
        assertEquals("VALUES (NEXT VALUE FOR {0})", dict.nextSequenceQuery);
        assertTrue("Derby has no ALTER SEQUENCE", dict.disableAlterSeqenceIncrementBy);

        Sequence seq = new Sequence(DBIdentifier.newSequence("OJPA3034_SEQ"), null);
        seq.setInitialValue(10);
        seq.setIncrement(1);
        seq.setAllocate(1);
        assertEquals("CREATE SEQUENCE OJPA3034_SEQ START WITH 10 INCREMENT BY 1 AS BIGINT",
            dict.getCreateSequenceSQL(seq)[0]);
        assertEquals("DROP SEQUENCE OJPA3034_SEQ RESTRICT", dict.getDropSequenceSQL(seq)[0]);
    }

    /**
     * Identifiers are drawn from the sequence, a block of allocationSize per
     * NEXT VALUE FOR, and no ALTER SEQUENCE is attempted.
     */
    public void testGeneratorUsesTheSequence() {
        if (!isDerby()) {
            return;
        }
        MetaDataRepository repos = emf.getConfiguration().getMetaDataRepositoryInstance();
        repos.getMetaData(NamedSeqGenEntity.class, null, true);
        Seq seq = repos.getSequenceMetaData(NamedSeqGenEntity.GENERATOR, null, true).getInstance(null);
        assertTrue(seq.getClass().getName(), seq instanceof NativeJDBCSeq);

        int count = 2 * NamedSeqGenEntity.ALLOCATE + 1;
        List<Long> ids = new ArrayList<>();
        resetSQL();
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int i = 0; i < count; i++) {
                NamedSeqGenEntity e = new NamedSeqGenEntity();
                e.setName("e" + i);
                em.persist(e);
                ids.add(e.getId());
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        for (int i = 1; i < ids.size(); i++) {
            assertEquals("identifiers of one block are consecutive: " + ids, ids.get(i - 1) + 1, (long) ids.get(i));
        }
        assertTrue(ids.toString(), ids.get(0) >= NamedSeqGenEntity.INITIAL);

        int nextValues = 0;
        for (String sql : sql) {
            assertFalse(sql, sql.toUpperCase().contains("ALTER SEQUENCE"));
            assertFalse(sql, sql.toUpperCase().contains("OPENJPA_SEQUENCES_TABLE"));
            if (sql.toUpperCase().contains("NEXT VALUE FOR " + NamedSeqGenEntity.SEQUENCE)) {
                nextValues++;
            }
        }
        assertEquals("one NEXT VALUE FOR per block of allocationSize", 3, nextValues);
    }

    /**
     * The schema tool finds the sequence it created, and the drop statement
     * Derby requires (with RESTRICT) removes it.
     */
    public void testSequenceIsReflectedAndDropped() throws Exception {
        if (!isDerby()) {
            return;
        }
        DBDictionary dict = dict();
        // make sure the sequence exists
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new NamedSeqGenEntity());
        em.getTransaction().commit();
        em.close();

        DBIdentifier name = DBIdentifier.newSequence(NamedSeqGenEntity.SEQUENCE);
        try (Connection conn = ((JDBCConfiguration) emf.getConfiguration()).getDataSource2(null).getConnection()) {
            Sequence[] seqs = dict.getSequences(conn.getMetaData(), DBIdentifier.NULL, DBIdentifier.NULL, name, conn);
            assertEquals(1, seqs.length);
            assertEquals(NamedSeqGenEntity.SEQUENCE, seqs[0].getIdentifier().getName());
            assertEquals(0, dict.getSequences(conn.getMetaData(), DBIdentifier.NULL, DBIdentifier.NULL,
                DBIdentifier.newSequence("OJPA3034_NO_SUCH_SEQ"), conn).length);

            try (Statement stmnt = conn.createStatement()) {
                stmnt.execute(dict.getDropSequenceSQL(seqs[0])[0]);
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            assertEquals(0, dict.getSequences(conn.getMetaData(), DBIdentifier.NULL, DBIdentifier.NULL,
                name, conn).length);
        }
    }

    /**
     * Derby cannot ALTER a sequence. A sequence that already exists with an
     * INCREMENT BY smaller than the allocation size must not be cached in
     * blocks of allocationSize: the database hands out the values in between
     * to other callers. OpenJPA reads the increment and draws every
     * identifier from the sequence instead.
     */
    public void testPreExistingSequenceWithSmallerIncrement() throws Exception {
        if (!isDerby()) {
            return;
        }
        DBDictionary dict = dict();
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        try (Connection conn = conf.getDataSource2(null).getConnection()) {
            conn.setAutoCommit(true);
            try (Statement stmnt = conn.createStatement()) {
                try {
                    stmnt.execute("DROP SEQUENCE " + NamedSeqGenEntity.SEQUENCE + " RESTRICT");
                } catch (SQLException e) {
                    // did not exist
                }
                stmnt.execute("CREATE SEQUENCE " + NamedSeqGenEntity.SEQUENCE
                    + " START WITH 1000 INCREMENT BY 1 AS BIGINT");
            }
            Sequence seq = new Sequence(DBIdentifier.newSequence(NamedSeqGenEntity.SEQUENCE), null);
            assertEquals(Long.valueOf(1), dict.getSequenceIncrement(conn, seq));
            assertEquals(Long.valueOf(0), dict.getSequenceIncrement(conn,
                new Sequence(DBIdentifier.newSequence("OJPA3034_NO_SUCH_SEQ"), null)));

            int count = NamedSeqGenEntity.ALLOCATE + 3;
            Set<Long> ids = new HashSet<>();
            Set<Long> drawnElsewhere = new HashSet<>();
            resetSQL();
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                for (int i = 0; i < count; i++) {
                    NamedSeqGenEntity e = new NamedSeqGenEntity();
                    e.setName("e" + i);
                    em.persist(e);
                    assertTrue("duplicate id " + e.getId(), ids.add(e.getId()));
                    // another client of the same sequence
                    try (Statement stmnt = conn.createStatement();
                         ResultSet rs = stmnt.executeQuery("VALUES (NEXT VALUE FOR "
                             + NamedSeqGenEntity.SEQUENCE + ")")) {
                        assertTrue(rs.next());
                        drawnElsewhere.add(rs.getLong(1));
                    }
                }
                em.getTransaction().commit();
            } finally {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                em.close();
            }

            Set<Long> both = new HashSet<>(ids);
            both.retainAll(drawnElsewhere);
            assertTrue("identifiers also handed out by the database: " + both, both.isEmpty());
            int nextValues = 0;
            for (String sql : sql) {
                if (sql.toUpperCase().contains("NEXT VALUE FOR " + NamedSeqGenEntity.SEQUENCE)) {
                    nextValues++;
                }
            }
            // the statements of the other client are counted as well
            assertEquals("one NEXT VALUE FOR per identifier", 2 * count, nextValues);
        } finally {
            // leave it to the next test to create the sequence OpenJPA's way
            try (Connection conn = conf.getDataSource2(null).getConnection();
                 Statement stmnt = conn.createStatement()) {
                conn.setAutoCommit(true);
                stmnt.execute("DROP SEQUENCE " + NamedSeqGenEntity.SEQUENCE + " RESTRICT");
            }
        }
    }
}

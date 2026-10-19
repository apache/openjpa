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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.NativeJDBCSeq;
import org.apache.openjpa.jdbc.kernel.TableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.ValueTableJDBCSeq;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * A named <code>@SequenceGenerator</code> has to work on every database,
 * including those whose dictionary defines no
 * {@link DBDictionary#nextSequenceQuery} and therefore has no native sequence
 * objects (MySQL, MariaDB &lt; 10.3, Derby as configured by OpenJPA). On such
 * a database the generator is backed by a table sequence instead; the
 * observable contract - unique identifiers that honour
 * <code>initialValue</code> and <code>allocationSize</code> - is the same
 * either way, which is what this test asserts.
 *<p>
 * The test therefore runs against both kinds of database, and asks the
 * dictionary which kind it is on rather than assuming: whether a database has
 * sequence objects is a property of the dictionary
 * ({@link DBDictionary#nextSequenceQuery}), which a dictionary may only set
 * once it has seen the server - MariaDB has sequences from 10.3 on and
 * {@link org.apache.openjpa.jdbc.sql.MariaDBDictionary} decides in
 * <code>connectedConfiguration()</code>. Only which {@link Seq}
 * implementation backs the generator, and that
 * <code>EmulateNativeSequences=false</code> brings the old failure back, are
 * specific to the fallback.
 *
 * @see <a href="https://issues.apache.org/jira/browse/OPENJPA-3014">OPENJPA-3014</a>
 */
public class TestNamedSequenceGenerator extends SQLListenerTestCase {

    private static final int ENTITY1_COUNT = 10;
    private static final int ENTITY2_COUNT = 5;
    private static final String SEQUENCE_TABLE = "OPENJPA_SEQUENCES_TABLE";

    @Override
    public void setUp() throws Exception {
        setUp(NamedSeqGenEntity.class, NamedSeqGenEntity2.class, CLEAR_TABLES);
    }

    private DBDictionary dict() {
        return connectedDict((JDBCConfiguration) emf.getConfiguration());
    }

    /**
     * The dictionary of the given configuration, having seen a connection.
     *<p>
     * Whether the database has sequence objects can depend on the server
     * version, so a dictionary may only set
     * {@link DBDictionary#nextSequenceQuery} in
     * <code>connectedConfiguration()</code>, and a dictionary identified from
     * the connection URL alone has not been connected until something asks
     * the configuration for its connection factory - which is exactly what
     * {@link org.apache.openjpa.jdbc.meta.SequenceMapping} does before it
     * decides whether to substitute the table sequence. Reading
     * <code>nextSequenceQuery</code> off an unconnected dictionary would make
     * this test expect the fallback on a server that does have sequences.
     */
    private static DBDictionary connectedDict(JDBCConfiguration conf) {
        DBDictionary dict = conf.getDBDictionaryInstance();
        if (!dict.isConnected()) {
            conf.getConnectionFactory();
        }
        return dict;
    }

    private boolean hasNativeSequences() {
        return dict().nextSequenceQuery != null;
    }

    private void skip(String reason) {
        System.out.println(getClass().getName() + "." + getName()
            + " skipped: " + reason);
    }

    /**
     * Creating the entity manager factory and persisting through a named
     * generator must not fail on a database without native sequences. Before
     * OPENJPA-3014 this threw
     * <code>ArgumentException: Error instantiating named sequence ...: Your
     * database dictionary does not support native sequences</code>.
     */
    public void testNamedGeneratorYieldsUniqueIdentifiers() {
        List<Long> ids = new ArrayList<>();

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int i = 0; i < ENTITY1_COUNT; i++) {
                NamedSeqGenEntity e = new NamedSeqGenEntity();
                e.setName("e1-" + i);
                em.persist(e);
                ids.add(e.getId());
            }
            for (int i = 0; i < ENTITY2_COUNT; i++) {
                NamedSeqGenEntity2 e = new NamedSeqGenEntity2();
                e.setName("e2-" + i);
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

        Set<Long> unique = new HashSet<>(ids);
        assertEquals("generated identifiers are not unique: " + ids,
            ids.size(), unique.size());

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Long id : ids) {
            assertTrue("identifier " + id + " is below the configured initialValue "
                + NamedSeqGenEntity.INITIAL, id >= NamedSeqGenEntity.INITIAL);
            min = Math.min(min, id);
            max = Math.max(max, id);
        }

        // Identifiers are handed out in blocks of allocationSize. Each of the
        // two entity classes may hold a block of its own, so the values seen
        // here have to fit into that many blocks - a generator that stepped
        // the identifiers themselves by allocationSize, instead of caching a
        // block and handing out consecutive values from it, would exceed it.
        // That allocationSize reaches the sequence at all is asserted by
        // testAllocationSizeIsHonouredByTheGenerator, which a contiguous
        // generator ignoring allocationSize does not satisfy.
        int blocks = blocks(ENTITY1_COUNT) + blocks(ENTITY2_COUNT);
        long span = max - min + 1;
        assertTrue("identifiers " + min + ".." + max + " span " + span
            + ", which is more than " + blocks + " blocks of "
            + NamedSeqGenEntity.ALLOCATE,
            span <= (long) blocks * NamedSeqGenEntity.ALLOCATE);

        // and the rows really are there
        em = emf.createEntityManager();
        try {
            assertEquals(ENTITY1_COUNT, ((Number) em.createQuery(
                "SELECT COUNT(e) FROM NamedSeqGenEntity e").getSingleResult()).intValue());
            assertEquals(ENTITY2_COUNT, ((Number) em.createQuery(
                "SELECT COUNT(e) FROM NamedSeqGenEntity2 e").getSingleResult()).intValue());
        } finally {
            em.close();
        }
    }

    private static int blocks(int count) {
        return (count + NamedSeqGenEntity.ALLOCATE - 1) / NamedSeqGenEntity.ALLOCATE;
    }

    /**
     * <code>allocationSize</code> exists to keep the generator from going to
     * the database per identifier, so the observable effect of honouring it
     * is the number of statements that advance the generator's counter: one
     * allocation per block of {@link NamedSeqGenEntity#ALLOCATE}, not one per
     * value. The span assertion in
     * {@link #testNamedGeneratorYieldsUniqueIdentifiers()} cannot see this -
     * a generator that fetched a fresh block of one per value would still
     * hand out consecutive identifiers - so it is asserted here.
     *<p>
     * This holds on both paths, so it is measured in a way that works on
     * both: see {@link #sequenceAllocations()}.
     */
    public void testAllocationSizeIsHonouredByTheGenerator() {
        int count = 15;
        // connect the dictionary before the statements are collected, so that
        // sequenceAllocations() sees only the statements of the run below
        boolean nativeSequences = hasNativeSequences();
        sql.clear();

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int i = 0; i < count; i++) {
                NamedSeqGenEntity e = new NamedSeqGenEntity();
                e.setName("alloc-" + i);
                em.persist(e);
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        int allocations = sequenceAllocations();
        int expected = blocks(count);
        assertTrue("expected about " + expected + " allocations of "
            + NamedSeqGenEntity.ALLOCATE + " for " + count + " identifiers, "
            + "a generator ignoring allocationSize would need " + count
            + "; saw " + allocations + " on the "
            + (nativeSequences ? "native" : "table backed") + " path in " + sql,
            allocations > 0 && allocations <= expected + 1);
        assertTrue("allocationSize was ignored: " + allocations
            + " allocations for " + count + " identifiers in " + sql,
            allocations < count);
    }

    /**
     * The number of statements in {@link #sql} that advanced the generator's
     * counter, on either path: the <code>UPDATE</code> of the counter row
     * where a table sequence backs the generator, and the dictionary's
     * <code>NextSequenceQuery</code> against the sequence object - for
     * instance <code>SELECT NEXTVAL(OJPA3014_SEQ)</code> on MariaDB - where a
     * native sequence does. DDL naming the sequence does not advance it: the
     * <code>CREATE SEQUENCE</code> of the schema tool and the
     * <code>ALTER SEQUENCE ... INCREMENT BY</code> that
     * {@link org.apache.openjpa.jdbc.kernel.NativeJDBCSeq} issues once, to
     * make the sequence step by the allocation size, are not counted.
     */
    private int sequenceAllocations() {
        int allocations = 0;
        for (String statement : sql) {
            String upper = statement.toUpperCase();
            if (upper.contains(SEQUENCE_TABLE)) {
                // the fallback reads the counter row and writes it back; only
                // the write hands out a block
                if (upper.startsWith("UPDATE")) {
                    allocations++;
                }
            } else if (upper.contains(NamedSeqGenEntity.SEQUENCE.toUpperCase())
                && !upper.startsWith("CREATE") && !upper.startsWith("ALTER")
                && !upper.startsWith("DROP")) {
                allocations++;
            }
        }
        return allocations;
    }

    /**
     * The sequence a generator names, with the catalog and schema it names it
     * in, has to reach whichever sequence implementation backs the generator.
     * On the fallback that name keys the counter row, so that two sequences of
     * one name in different schemas stay distinct; the key format is part of
     * what an existing schema holds, so it is pinned. On the native path the
     * same name identifies the sequence object.
     */
    public void testQualifiedSequenceNameReachesTheSequence() {
        OpenJPAEntityManagerFactorySPI other = createEMF(QualifiedSeqGenEntity.class);
        try {
            JDBCConfiguration conf = (JDBCConfiguration) other.getConfiguration();
            DBDictionary dict = connectedDict(conf);
            MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
            repos.getMetaData(QualifiedSeqGenEntity.class, null, true);
            SequenceMetaData smd = repos.getSequenceMetaData(
                QualifiedSeqGenEntity.GENERATOR, null, true);
            assertNotNull(smd);
            Seq seq = smd.getInstance(null);
            if (dict.nextSequenceQuery == null) {
                assertTrue("expected a table backed sequence on " + dict.platform
                    + ", got " + seq.getClass().getName(), seq instanceof ValueTableJDBCSeq);
                assertEquals(QualifiedSeqGenEntity.CATALOG + "."
                    + QualifiedSeqGenEntity.SCHEMA + "." + QualifiedSeqGenEntity.SEQUENCE,
                    ((ValueTableJDBCSeq) seq).getPrimaryKeyValue());
            } else {
                assertTrue("expected a native sequence on " + dict.platform
                    + ", got " + seq.getClass().getName(), seq instanceof NativeJDBCSeq);
                assertEquals(QualifiedSeqGenEntity.SEQUENCE,
                    ((NativeJDBCSeq) seq).getSequence());
                assertEquals(QualifiedSeqGenEntity.SCHEMA,
                    ((NativeJDBCSeq) seq).getSchemaIdentifier().getName());
            }
        } finally {
            closeEMF(other);
        }
    }

    /**
     * The fallback is a compatibility option, so turning it off has to bring
     * back the failure it replaced.
     */
    public void testCompatibilityOptionRestoresTheFailure() {
        if (hasNativeSequences()) {
            skip(dict().platform + " has native sequences, so no generator"
                + " falls back and the option makes no difference");
            return;
        }
        OpenJPAEntityManagerFactorySPI other = createEMF(
            NamedSeqGenEntity.class, NamedSeqGenEntity2.class,
            "openjpa.Compatibility", "EmulateNativeSequences=false");
        try {
            EntityManager em = other.createEntityManager();
            em.getTransaction().begin();
            em.persist(new NamedSeqGenEntity());
            em.getTransaction().commit();
            em.close();
            fail("expected the named generator to fail without a native sequence");
        } catch (RuntimeException expected) {
            assertTrue("unexpected failure: " + expected,
                expected.getMessage() != null
                && expected.getMessage().contains("does not support native sequences"));
        } finally {
            closeEMF(other);
        }
    }

    /**
     * <code>initialValue</code> and <code>allocationSize</code> travel on the
     * {@link SequenceMetaData}, not in the sequence plugin string, so they
     * have to reach whichever {@link Seq} implementation ends up backing the
     * generator.
     */
    public void testInitialValueAndAllocationSizeReachTheSequence() {
        MetaDataRepository repos = emf.getConfiguration().getMetaDataRepositoryInstance();
        // resolving the entity registers the generator it declares
        repos.getMetaData(NamedSeqGenEntity.class, null, true);
        SequenceMetaData smd = repos.getSequenceMetaData(NamedSeqGenEntity.GENERATOR, null, true);
        assertNotNull(smd);
        assertEquals(NamedSeqGenEntity.INITIAL, smd.getInitialValue());
        assertEquals(NamedSeqGenEntity.ALLOCATE, smd.getAllocate());

        Seq seq = smd.getInstance(null);
        if (dict().nextSequenceQuery == null) {
            assertTrue("expected a table backed sequence on "
                + dict().platform + ", got " + seq.getClass().getName(),
                seq instanceof ValueTableJDBCSeq);
            // keyed by the sequence name, so entities sharing the generator
            // share one counter
            assertEquals(NamedSeqGenEntity.SEQUENCE,
                ((ValueTableJDBCSeq) seq).getPrimaryKeyValue());
            assertEquals(NamedSeqGenEntity.INITIAL,
                ((TableJDBCSeq) seq).getInitialValue());
            assertEquals(NamedSeqGenEntity.ALLOCATE,
                ((TableJDBCSeq) seq).getAllocate());
        } else {
            assertTrue("expected a native sequence on " + dict().platform
                + ", got " + seq.getClass().getName(),
                seq instanceof NativeJDBCSeq);
            assertEquals(NamedSeqGenEntity.INITIAL,
                ((NativeJDBCSeq) seq).getInitialValue());
            assertEquals(NamedSeqGenEntity.ALLOCATE,
                ((NativeJDBCSeq) seq).getAllocate());
            assertEquals(NamedSeqGenEntity.SEQUENCE,
                ((NativeJDBCSeq) seq).getSequence());
        }
    }
}

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
package org.apache.openjpa.persistence.optlockex.timestamp;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A timestamp version column of a schema not created by OpenJPA may keep
 * fewer fractional digits of a second than the dictionary assumes - the TCK
 * creates the version column of core.annotations.version.Client4 as a MySQL
 * <code>TIMESTAMP</code> without fractional seconds and as a SQL Server
 * <code>DATETIME</code>. The version kept in memory then never matched the
 * stored one and the next update of the instance failed with an
 * OptimisticLockException (OPENJPA-3034). The precision of the column is
 * read from the database metadata, once per version column, and the
 * versions are generated and bound at it. A precision declared in the
 * mapping is used without a lookup.
 */
public class TestTimestampVersionColumnPrecision extends SingleEMFTestCase {

    @Override
    public void setUp() throws Exception {
        if (!isDerby()) {
            return;
        }
        super.setUp(DROP_TABLES, CoarseTSVersionEntity.class, DeclaredTSVersionEntity.class,
            SharedTSVersionEntityA.class, SharedTSVersionEntityB.class,
            "openjpa.jdbc.DBDictionary", CoarseTimestampDerbyDictionary.class.getName());
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

    public void testVersionsAreKeptAtTheColumnPrecision() {
        if (emf == null) {
            return;
        }
        assertTrue(((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance()
            instanceof DerbyDictionary);

        CoarseTimestampDerbyDictionary.COLUMN_LOOKUPS.set(0);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CoarseTSVersionEntity e = new CoarseTSVersionEntity("1");
            em.persist(e);
            em.getTransaction().commit();
            assertEquals("the version is generated at the precision of the column: " + e.getVersion(),
                0, e.getVersion().getNanos());

            Column col = ((JDBCConfiguration) emf.getConfiguration()).getMappingRepositoryInstance()
                .getMapping(CoarseTSVersionEntity.class, null, true).getVersion().getColumns()[0];
            assertEquals("the precision read from the database is recorded on the column", 0, col.getPrecision());

            // the cached instance is updated, its version has to match the stored one
            for (int i = 0; i < 2; i++) {
                em.getTransaction().begin();
                CoarseTSVersionEntity found = em.find(CoarseTSVersionEntity.class, "1");
                assertSame(e, found);
                found.setName("name" + i);
                em.getTransaction().commit();
                assertEquals(0, found.getVersion().getNanos());
            }
            assertEquals("the metadata is read once, not on every flush",
                1, CoarseTimestampDerbyDictionary.COLUMN_LOOKUPS.get());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        em = emf.createEntityManager();
        try {
            CoarseTSVersionEntity e = em.find(CoarseTSVersionEntity.class, "1");
            assertEquals("name1", e.getName());
            assertEquals(0, e.getVersion().getNanos());
        } finally {
            em.close();
        }
    }

    /**
     * <code>@Column(secondPrecision = 0)</code>: the versions are generated
     * at whole seconds, as the column keeps them, and the database metadata
     * is not consulted.
     */
    public void testDeclaredSecondPrecisionIsHonoured() {
        if (emf == null) {
            return;
        }
        CoarseTimestampDerbyDictionary.COLUMN_LOOKUPS.set(0);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            DeclaredTSVersionEntity e = new DeclaredTSVersionEntity("1");
            em.persist(e);
            em.getTransaction().commit();
            assertEquals("the version is generated at the declared precision: " + e.getVersion(),
                0, e.getVersion().getNanos());
            Column col = ((JDBCConfiguration) emf.getConfiguration()).getMappingRepositoryInstance()
                .getMapping(DeclaredTSVersionEntity.class, null, true).getVersion().getColumns()[0];
            assertEquals("secondPrecision reaches the version column", 0, col.getSecondPrecision());

            for (int i = 0; i < 2; i++) {
                em.getTransaction().begin();
                DeclaredTSVersionEntity found = em.find(DeclaredTSVersionEntity.class, "1");
                assertSame(e, found);
                found.setName("name" + i);
                em.getTransaction().commit();
                assertEquals(0, found.getVersion().getNanos());
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        assertEquals("no metadata lookup for a declared precision",
            0, CoarseTimestampDerbyDictionary.COLUMN_LOOKUPS.get());
    }

    /**
     * Two entities mapped to the same table share the version column. The
     * precision read for it from the metadata, which depends on the column
     * type, applies to both; it is read once.
     */
    public void testSharedVersionColumnIsResolvedOnce() {
        if (emf == null) {
            return;
        }
        CoarseTimestampDerbyDictionary.COLUMN_LOOKUPS.set(0);
        int half = CoarseTimestampDerbyDictionary.HALF_SECOND;
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            SharedTSVersionEntityA a = new SharedTSVersionEntityA("a");
            em.persist(a);
            em.getTransaction().commit();
            assertEquals(a.getVersion().toString(), 0, a.getVersion().getNanos() % half);

            for (int i = 0; i < 4; i++) {
                em.getTransaction().begin();
                SharedTSVersionEntityB b = em.find(SharedTSVersionEntityB.class, "b");
                if (b == null) {
                    b = new SharedTSVersionEntityB("b");
                    em.persist(b);
                } else {
                    b.setName("name" + i);
                }
                em.getTransaction().commit();
                assertEquals(b.getVersion().toString(), 0, b.getVersion().getNanos() % half);
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        assertEquals("one lookup for the shared column", 1, CoarseTimestampDerbyDictionary.COLUMN_LOOKUPS.get());
    }
}

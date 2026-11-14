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
package org.apache.openjpa.persistence.jdbc.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.H2Dictionary;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A <code>BigInteger</code> with more than 19 digits must survive a round
 * trip through a schema created by OpenJPA. On H2 such a value used to fail
 * with "Numeric value out of range", as the column was created as
 * <code>BIGINT</code> (OPENJPA-3058).
 */
class TestBigIntegerMapping extends SingleEMFTestCase {

    private static final BigInteger WIDE_ID = new BigInteger("123456789012345678901234567890");
    private static final BigInteger WIDE_AMOUNT = new BigInteger("-98765432109876543210987654321");

    private BigInteger id;
    private BigInteger amount;

    @BeforeEach
    public void setUp() {
        super.setUp(DROP_TABLES, BigIntegerEntity.class, BigIntegerRefEntity.class);

        // databases other than H2 that store a BigInteger in a BIGINT column
        // are limited to its range
        DBDictionary dict = getDictionary();
        if (!(dict instanceof H2Dictionary)
            && dict.getJDBCType(JavaTypes.BIGINTEGER, false, 0, 0) == Types.BIGINT) {
            id = BigInteger.valueOf(Long.MAX_VALUE);
            amount = BigInteger.valueOf(Long.MIN_VALUE);
        } else {
            id = WIDE_ID;
            amount = WIDE_AMOUNT;
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            BigIntegerEntity e = new BigIntegerEntity(id, amount);
            em.persist(e);
            em.persist(new BigIntegerEntity(BigInteger.ONE, BigInteger.TEN));
            em.persist(new BigIntegerRefEntity(1, e));
            em.getTransaction().commit();
        }
    }

    @Test
    void valueIsKeptOnFind() {
        try (EntityManager em = emf.createEntityManager()) {
            BigIntegerEntity e = em.find(BigIntegerEntity.class, id);
            assertNotNull(e);
            assertEquals(id, e.getId());
            assertEquals(amount, e.getAmount());
        }
    }

    @Test
    void valueIsKeptOnUpdate() {
        BigInteger updated = amount.add(BigInteger.ONE);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(BigIntegerEntity.class, id).setAmount(updated);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(updated, em.find(BigIntegerEntity.class, id).getAmount());
        }
    }

    @Test
    void valueIsComparedInQuery() {
        try (EntityManager em = emf.createEntityManager()) {
            List<BigInteger> ids = em.createQuery("SELECT e.id FROM BigIntegerEntity e "
                + "WHERE e.amount = :amount", BigInteger.class)
                .setParameter("amount", amount)
                .getResultList();
            assertEquals(List.of(id), ids);

            List<BigInteger> amounts = em.createQuery("SELECT e.amount FROM BigIntegerEntity e "
                + "WHERE e.id = :id", BigInteger.class)
                .setParameter("id", id)
                .getResultList();
            assertEquals(List.of(amount), amounts);
        }
    }

    @Test
    void foreignKeyToBigIntegerId() {
        try (EntityManager em = emf.createEntityManager()) {
            BigIntegerRefEntity ref = em.find(BigIntegerRefEntity.class, 1L);
            assertNotNull(ref.getTarget());
            assertEquals(id, ref.getTarget().getId());
            assertEquals(amount, ref.getTarget().getAmount());

            List<Long> refs = em.createQuery("SELECT r.id FROM BigIntegerRefEntity r "
                + "WHERE r.target.id = :id", Long.class)
                .setParameter("id", id)
                .getResultList();
            assertEquals(List.of(1L), refs);
        }
    }

    @Test
    void columnTypesOnH2() throws Exception {
        if (!(getDictionary() instanceof H2Dictionary)) {
            return;
        }
        Map<String, Integer> cols = new HashMap<>();
        try (Connection conn = ((JDBCConfiguration) emf.getConfiguration()).getDataSource2(null).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            for (String table : new String[] { "MAP_BIGINT", "MAP_BIGINT_REF" }) {
                try (ResultSet rs = meta.getColumns(null, null, table, null)) {
                    while (rs.next()) {
                        cols.put(table + "." + rs.getString("COLUMN_NAME").toUpperCase(Locale.ENGLISH),
                            rs.getInt("DATA_TYPE"));
                    }
                }
            }
        }
        assertEquals(Integer.valueOf(Types.NUMERIC), cols.get("MAP_BIGINT.ID"), cols.toString());
        assertEquals(Integer.valueOf(Types.NUMERIC), cols.get("MAP_BIGINT.AMOUNT"), cols.toString());
        assertEquals(Integer.valueOf(Types.NUMERIC), cols.get("MAP_BIGINT_REF.TARGET_ID"), cols.toString());
    }

    /**
     * Tables created by earlier releases keep their <code>BIGINT</code>
     * columns, which still hold values in the range of a <code>long</code>.
     */
    @Test
    void existingBigintColumnsOnH2() throws Exception {
        if (!(getDictionary() instanceof H2Dictionary)) {
            return;
        }
        try (Connection conn = ((JDBCConfiguration) emf.getConfiguration()).getDataSource2(null).getConnection();
            Statement stmnt = conn.createStatement()) {
            stmnt.execute("DROP TABLE MAP_BIGINT_REF");
            stmnt.execute("DROP TABLE MAP_BIGINT");
            stmnt.execute("CREATE TABLE MAP_BIGINT (ID BIGINT NOT NULL, AMOUNT BIGINT, PRIMARY KEY (ID))");
            stmnt.execute("CREATE TABLE MAP_BIGINT_REF (ID BIGINT NOT NULL, TARGET_ID BIGINT, PRIMARY KEY (ID))");
        }

        BigInteger oldId = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger oldAmount = BigInteger.valueOf(Long.MIN_VALUE);
        EntityManagerFactory oldEmf = createEMF(FRESH_EMF, BigIntegerEntity.class, BigIntegerRefEntity.class,
            "openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        try {
            try (EntityManager em = oldEmf.createEntityManager()) {
                em.getTransaction().begin();
                BigIntegerEntity e = new BigIntegerEntity(oldId, oldAmount);
                em.persist(e);
                em.persist(new BigIntegerRefEntity(2, e));
                em.getTransaction().commit();
            }
            try (EntityManager em = oldEmf.createEntityManager()) {
                BigIntegerRefEntity ref = em.find(BigIntegerRefEntity.class, 2L);
                assertEquals(oldId, ref.getTarget().getId());
                assertEquals(oldAmount, ref.getTarget().getAmount());
                assertEquals(List.of(oldId), em.createQuery("SELECT e.id FROM BigIntegerEntity e "
                    + "WHERE e.amount = :amount", BigInteger.class)
                    .setParameter("amount", oldAmount)
                    .getResultList());
            }
            try (Connection conn = ((JDBCConfiguration) emf.getConfiguration()).getDataSource2(null).getConnection();
                ResultSet rs = conn.getMetaData().getColumns(null, null, "MAP_BIGINT", "AMOUNT")) {
                assertTrue(rs.next());
                assertEquals(Types.BIGINT, rs.getInt("DATA_TYPE"));
            }
        } finally {
            closeEMF(oldEmf);
        }
    }

    private DBDictionary getDictionary() {
        return ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance();
    }
}

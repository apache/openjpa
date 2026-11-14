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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.H2Dictionary;
import org.apache.openjpa.jdbc.sql.MariaDBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.SQLServerDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fractional values of decimal attributes must survive a round trip through
 * a schema created by OpenJPA. On H2 2.x an unsized NUMERIC column has scale
 * 0, so 2.5 used to be stored as 3 (OPENJPA-3051). MySQL and MariaDB read an
 * unsized DECIMAL as DECIMAL(10,0) (OPENJPA-3062), SQL Server as
 * DECIMAL(18,0) (OPENJPA-3064).
 */
class TestDecimalScale extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        super.setUp(DROP_TABLES, DecimalScaleEntity.class);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new DecimalScaleEntity(1, new BigDecimal("2.5"), new BigDecimal("1234.5678"),
                new BigDecimal("0.125"), new BigInteger("1234567890123"), 7.25));
            em.persist(new DecimalScaleEntity(2, new BigDecimal("100"), new BigDecimal("100"),
                null, null, null));
            em.getTransaction().commit();
        }
    }

    @Test
    void fractionIsKeptOnFind() {
        try (EntityManager em = emf.createEntityManager()) {
            DecimalScaleEntity e = em.find(DecimalScaleEntity.class, 1L);
            assertDecimal("2.5", e.getAmount());
            assertDecimal("1234.5678", e.getUnsized());
            assertDecimal("0.125", e.getScaleOnly());
            assertEquals(new BigInteger("1234567890123"), e.getBigInteger());
            assertEquals(7.25, e.getDoubleScale(), 0.0);
        }
    }

    @Test
    void fractionIsKeptOnUpdate() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(DecimalScaleEntity.class, 1L).setAmount(new BigDecimal("0.75"));
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertDecimal("0.75", em.find(DecimalScaleEntity.class, 1L).getAmount());
        }
    }

    @Test
    void fractionIsComparedInQuery() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Long> ids = em.createQuery("SELECT e.id FROM DecimalScaleEntity e "
                + "WHERE e.amount = :amount AND e.unsized > 1234.5", Long.class)
                .setParameter("amount", new BigDecimal("2.5"))
                .getResultList();
            assertEquals(List.of(1L), ids);

            BigDecimal sum = em.createQuery("SELECT e.amount + e.scaleOnly FROM DecimalScaleEntity e "
                + "WHERE e.id = 1", BigDecimal.class).getSingleResult();
            assertDecimal("2.625", sum);
        }
    }

    @Test
    void wholeNumberKeepsPlainScale() {
        if (!isH2()) {
            return;
        }
        try (EntityManager em = emf.createEntityManager()) {
            DecimalScaleEntity e = em.find(DecimalScaleEntity.class, 2L);
            assertEquals(new BigDecimal("100"), e.getUnsized());
            assertEquals("100", e.getUnsized().toString());
        }
    }

    @Test
    void quotientHasLimitedDigits() {
        try (EntityManager em = emf.createEntityManager()) {
            BigDecimal quot = em.createQuery("SELECT e.unsized / 3 FROM DecimalScaleEntity e "
                + "WHERE e.id = 2", BigDecimal.class).getSingleResult();
            assertEquals(0, new BigDecimal("33.3").compareTo(quot.setScale(1, RoundingMode.DOWN)));
            assertTrue(quot.toPlainString().length() < 100, quot.toPlainString());
        }
    }

    @Test
    void columnTypesOnH2() throws Exception {
        if (!isH2()) {
            return;
        }
        Map<String, int[]> cols = getColumnSizes();
        assertSize(cols, "AMOUNT", 10, 2);
        // DECFLOAT(38) has no fixed scale
        assertSize(cols, "UNSIZED", 38, 0);
        assertSize(cols, "SCALEONLY", 38, 0);
        assertSize(cols, "DOUBLESCALE", 38, 0);
    }

    /**
     * MySQL and MariaDB read an unsized DECIMAL as DECIMAL(10,0) (OPENJPA-3062).
     */
    @Test
    void columnTypesOnMySQLAndMariaDB() throws Exception {
        DBDictionary dict = ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance();
        if (!(dict instanceof MySQLDictionary) && !(dict instanceof MariaDBDictionary)) {
            return;
        }
        Map<String, int[]> cols = getColumnSizes();
        assertSize(cols, "AMOUNT", 10, 2);
        assertSize(cols, "UNSIZED", 65, 30);
        assertSize(cols, "SCALEONLY", 65, 3);
        assertSize(cols, "DOUBLESCALE", 65, 2);
    }

    /**
     * SQL Server reads an unsized DECIMAL as DECIMAL(18,0) (OPENJPA-3064).
     */
    @Test
    void columnTypesOnSQLServer() throws Exception {
        if (!(((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance()
            instanceof SQLServerDictionary)) {
            return;
        }
        Map<String, int[]> cols = getColumnSizes();
        assertSize(cols, "AMOUNT", 10, 2);
        assertSize(cols, "UNSIZED", 38, 10);
        assertSize(cols, "SCALEONLY", 38, 3);
        assertSize(cols, "DOUBLESCALE", 38, 2);
    }

    private Map<String, int[]> getColumnSizes() throws SQLException {
        Map<String, int[]> cols = new HashMap<>();
        try (Connection conn = ((JDBCConfiguration) emf.getConfiguration()).getDataSource2(null).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "MAP_DECSCALE", null)) {
                while (rs.next()) {
                    cols.put(rs.getString("COLUMN_NAME").toUpperCase(Locale.ENGLISH),
                        new int[] { rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS") });
                }
            }
        }
        return cols;
    }

    private boolean isH2() {
        return ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance() instanceof H2Dictionary;
    }

    private static void assertSize(Map<String, int[]> cols, String name, int size, int digits) {
        int[] col = cols.get(name);
        assertNotNull(col, name + " in " + cols.keySet());
        assertEquals(size, col[0], name + " size");
        assertEquals(digits, col[1], name + " digits");
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), expected + " <> " + actual);
    }
}

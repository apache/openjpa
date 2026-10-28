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
package org.apache.openjpa.jdbc.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.PersistenceTestCase;

/**
 * Storage of Java <code>char</code> and <code>Character</code> fields in
 * a schema created by OpenJPA, in a pre-created <code>CHAR(1)</code> column
 * like the one of the Jakarta Persistence TCK, and with an explicit
 * <code>StoreCharsAsNumbers=true</code> (OPENJPA-3033).
 */
public class TestCharStorage extends PersistenceTestCase {

    private static final String DICT_PROP = "openjpa.jdbc.DBDictionary";

    /**
     * A schema created by OpenJPA maps char fields according to the
     * dictionary default, which is a CHAR column on Derby.
     */
    public void testCharFieldsInGeneratedSchema() throws Exception {
        OpenJPAEntityManagerFactorySPI emf = createEMF(CharStorageBase.class, CharStorageEntity.class, DROP_TABLES);
        try {
            DBDictionary dict = dictionary(emf);
            assertDefault(dict);
            populate(emf, CharStorageEntity.class);
            int expectedType = dict.storeCharsAsNumbers ? Types.INTEGER : Types.CHAR;
            assertColumnType(emf, "CHARSTORAGE", "CHARDATA", expectedType);
            assertColumnType(emf, "CHARSTORAGE", "CHARACTERDATA", expectedType);
            assertStoredValue(emf, "CHARSTORAGE", dict.storeCharsAsNumbers);
            assertRoundTripAndQueries(emf, CharStorageEntity.class);
        } finally {
            closeEMF(emf);
        }
    }

    /**
     * Char fields mapped to an existing CHAR(1) column, as created by the
     * schema of the Jakarta Persistence TCK, are written and read as
     * characters. Dictionaries that still store chars as numbers by default
     * (such as H2) need StoreCharsAsNumbers=false for such a column.
     */
    public void testCharFieldsInPrecreatedCharColumn() throws Exception {
        OpenJPAEntityManagerFactorySPI emf = createEMF(CharStorageBase.class, CharStoragePrecreatedEntity.class);
        DBDictionary dict = dictionary(emf);
        assertDefault(dict);
        if (dict.storeCharsAsNumbers) {
            closeEMF(emf);
            emf = createEMF(CharStorageBase.class, CharStoragePrecreatedEntity.class,
                DICT_PROP, withStoreCharsAsNumbers(System.getProperty(DICT_PROP), false));
            assertFalse(dictionary(emf).storeCharsAsNumbers);
        }
        try {
            try (Connection conn = connection(emf); Statement stmt = conn.createStatement()) {
                try {
                    stmt.execute("DROP TABLE CHARSTORAGE_PRE");
                } catch (SQLException e) {
                    // table did not exist
                }
                stmt.execute("CREATE TABLE CHARSTORAGE_PRE (ID INTEGER NOT NULL, CHARDATA CHAR(1), "
                    + "CHARACTERDATA CHAR(1), PRIMARY KEY (ID))");
            }
            populate(emf, CharStoragePrecreatedEntity.class);
            assertStoredValue(emf, "CHARSTORAGE_PRE", false);
            assertRoundTripAndQueries(emf, CharStoragePrecreatedEntity.class);
        } finally {
            closeEMF(emf);
        }
    }

    /**
     * An explicit StoreCharsAsNumbers=true keeps the numeric mapping that
     * schemas created by earlier releases rely on.
     */
    public void testExplicitStoreCharsAsNumbers() throws Exception {
        OpenJPAEntityManagerFactorySPI emf = createEMF(CharStorageBase.class, CharStorageNumericEntity.class, DROP_TABLES,
            DICT_PROP, withStoreCharsAsNumbers(System.getProperty(DICT_PROP), true));
        try {
            assertTrue(dictionary(emf).storeCharsAsNumbers);
            populate(emf, CharStorageNumericEntity.class);
            assertColumnType(emf, "CHARSTORAGE_NUM", "CHARDATA", Types.INTEGER);
            assertColumnType(emf, "CHARSTORAGE_NUM", "CHARACTERDATA", Types.INTEGER);
            assertStoredValue(emf, "CHARSTORAGE_NUM", true);
            assertRoundTripAndQueries(emf, CharStorageNumericEntity.class);
        } finally {
            closeEMF(emf);
        }
    }

    /**
     * Derby and SQL Server store chars as CHAR unless configured otherwise.
     */
    private static void assertDefault(DBDictionary dict) {
        if (dict instanceof DerbyDictionary || dict instanceof SQLServerDictionary) {
            assertFalse(dict.platform + " must store chars as CHAR by default", dict.storeCharsAsNumbers);
        }
    }

    /**
     * Adds StoreCharsAsNumbers=<code>value</code> to the dictionary plugin
     * string the test profile configured, keeping its other properties.
     */
    private static String withStoreCharsAsNumbers(String dict, boolean value) {
        String prop = "StoreCharsAsNumbers=" + value;
        if (dict == null || dict.trim().isEmpty()) {
            return prop;
        }
        String trimmed = dict.trim();
        if (trimmed.endsWith(")")) {
            String head = trimmed.substring(0, trimmed.length() - 1);
            return head + (head.endsWith("(") ? "" : ",") + prop + ")";
        }
        if (trimmed.contains("=")) {
            return trimmed + "," + prop;
        }
        return trimmed + "(" + prop + ")";
    }

    private static <T extends CharStorageBase> void populate(OpenJPAEntityManagerFactorySPI emf, Class<T> type)
        throws Exception {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM " + type.getSimpleName() + " e").executeUpdate();
            T first = type.getDeclaredConstructor().newInstance();
            first.setId(1);
            first.setCharData('a');
            first.setCharacterData('b');
            em.persist(first);
            T second = type.getDeclaredConstructor().newInstance();
            second.setId(2);
            second.setCharData('*');
            second.setCharacterData(null);
            em.persist(second);
            em.getTransaction().commit();
        }
    }

    private static <T extends CharStorageBase> void assertRoundTripAndQueries(OpenJPAEntityManagerFactorySPI emf,
        Class<T> type) {
        String entity = type.getSimpleName();
        try (EntityManager em = emf.createEntityManager()) {
            T first = em.find(type, 1);
            assertEquals('a', first.getCharData());
            assertEquals(Character.valueOf('b'), first.getCharacterData());
            T second = em.find(type, 2);
            assertEquals('*', second.getCharData());
            assertNull(second.getCharacterData());

            List<T> byParam = em.createQuery("SELECT e FROM " + entity + " e WHERE e.charData = :c", type)
                .setParameter("c", 'a').getResultList();
            assertIds(byParam, 1);
            byParam = em.createQuery("SELECT e FROM " + entity + " e WHERE e.characterData = :c", type)
                .setParameter("c", Character.valueOf('b')).getResultList();
            assertIds(byParam, 1);

            List<T> byLiteral = em.createQuery("SELECT e FROM " + entity + " e WHERE e.charData = '*'", type)
                .getResultList();
            assertIds(byLiteral, 2);
            byLiteral = em.createQuery("SELECT e FROM " + entity + " e WHERE e.characterData = 'b'", type)
                .getResultList();
            assertIds(byLiteral, 1);

            List<Character> selected = em.createQuery("SELECT e.charData FROM " + entity
                + " e WHERE e.id = 1", Character.class).getResultList();
            assertEquals(1, selected.size());
            assertEquals(Character.valueOf('a'), selected.get(0));
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            T first = em.find(type, 1);
            first.setCharData('x');
            first.setCharacterData('y');
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            T first = em.find(type, 1);
            assertEquals('x', first.getCharData());
            assertEquals(Character.valueOf('y'), first.getCharacterData());
        }
    }

    private static void assertIds(List<? extends CharStorageBase> result, int id) {
        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    private static void assertStoredValue(OpenJPAEntityManagerFactorySPI emf, String table, boolean numeric)
        throws SQLException {
        try (Connection conn = connection(emf); Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT CHARDATA, CHARACTERDATA FROM " + table + " WHERE ID = 1")) {
            assertTrue(rs.next());
            if (numeric) {
                assertEquals('a', rs.getInt(1));
                assertEquals('b', rs.getInt(2));
            } else {
                assertEquals("a", rs.getString(1));
                assertEquals("b", rs.getString(2));
            }
        }
    }

    private static void assertColumnType(OpenJPAEntityManagerFactorySPI emf, String table, String column,
        int expected) throws SQLException {
        try (Connection conn = connection(emf)) {
            DatabaseMetaData meta = conn.getMetaData();
            boolean found = false;
            for (String name : new String[] { table, table.toLowerCase() }) {
                try (ResultSet rs = meta.getColumns(null, null, name, null)) {
                    while (rs.next()) {
                        if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                            assertEquals(table + "." + column, expected, rs.getInt("DATA_TYPE"));
                            if (expected == Types.CHAR) {
                                assertEquals(table + "." + column + " size", 1, rs.getInt("COLUMN_SIZE"));
                            }
                            found = true;
                        }
                    }
                }
            }
            assertTrue("column " + table + "." + column + " not found", found);
        }
    }

    private static DBDictionary dictionary(OpenJPAEntityManagerFactorySPI emf) {
        return ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance();
    }

    private static Connection connection(OpenJPAEntityManagerFactorySPI emf) throws SQLException {
        return ((JDBCConfiguration) emf.getConfiguration()).getDataSource2(null).getConnection();
    }
}

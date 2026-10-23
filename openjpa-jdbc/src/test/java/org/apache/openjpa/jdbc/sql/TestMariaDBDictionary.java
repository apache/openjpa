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

import static org.apache.openjpa.jdbc.sql.Columns.column;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.lib.conf.Configurations;
import org.junit.Test;

/**
 * Tests for {@link MariaDBDictionary}, in particular the version gate that
 * declares sequence support from MariaDB 10.3 on, that an explicitly
 * configured <code>NextSequenceQuery</code> survives
 * {@link DBDictionary#connectedConfiguration(Connection)}, and the SQL that
 * creates and reflects on sequences (OPENJPA-3014).
 */
public class TestMariaDBDictionary {

    private static final String NEXT_SEQUENCE_QUERY = "SELECT NEXTVAL({0})";

    @Test
    public void testNoSequenceSupportBefore103() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("10.2.44-MariaDB"));
        assertNull("MariaDB 10.2 has no sequence objects", dict.nextSequenceQuery);
        assertNull("no sequence support means no CREATE SEQUENCE",
            dict.getCreateSequenceSQL(sequence()));
    }

    @Test
    public void testSequenceSupportFrom103() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("10.3.39-MariaDB"));
        assertEquals(NEXT_SEQUENCE_QUERY, dict.nextSequenceQuery);
    }

    @Test
    public void testSequenceSupportOnMariaDB11() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("11.8.2-MariaDB"));
        assertEquals(NEXT_SEQUENCE_QUERY, dict.nextSequenceQuery);
    }

    /**
     * A MariaDB 10 or 11 server reports <code>5.5.5-10.3.39-MariaDB</code> to
     * a client that is not MariaDB aware. Which dictionary is chosen is
     * decided by the product name, and the product version is consulted only
     * when the name maps to MySQL - which is exactly what such a client
     * reports (DBDictionaryFactory.newDBDictionary, lines 93-102) - so the
     * prefixed form is the one this dictionary sees there, and the version
     * gate has to see through the prefix.
     */
    @Test
    public void testSequenceSupportWithReplicationCompatibilityPrefix() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("5.5.5-10.3.39-MariaDB"));
        assertEquals("10.3 reported as 5.5.5-10.3.39-MariaDB must report sequence support",
            NEXT_SEQUENCE_QUERY, dict.nextSequenceQuery);
    }

    /**
     * A prefixed 11.x version is synthetic: MariaDB dropped the
     * <code>5.5.5-</code> prefix in 11.0, and an 11.x server reports
     * <code>11.8.9-MariaDB-ubu2404</code> to every client. The case is kept
     * because the parser must not depend on which side of that change a
     * version comes from.
     */
    @Test
    public void testSequenceSupportWithPrefixedVendorSuffix() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("5.5.5-11.4.2-MariaDB-ubu2404"));
        assertEquals(NEXT_SEQUENCE_QUERY, dict.nextSequenceQuery);
    }

    @Test
    public void testPrefixedVersionBelow103KeepsTheFallback() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("5.5.5-10.2.44-MariaDB"));
        assertNull(dict.nextSequenceQuery);
    }

    /**
     * Stripping the prefix moves every version gated feature of this
     * dictionary, not only sequence support: 10.2 and later also get
     * microsecond precision temporal columns, so the prefixed form of such a
     * version has to produce the same DDL types as the plain one.
     */
    @Test
    public void testPrefixedVersionGetsTheSameTemporalTypesAsThePlainForm() throws Exception {
        for (String productVersion : new String[] { "10.2.44-MariaDB", "5.5.5-10.2.44-MariaDB" }) {
            MariaDBDictionary dict = newConfiguredDictionary(null);
            dict.connectedConfiguration(connection(productVersion));
            assertEquals(productVersion, "DATETIME{0}", dict.timestampTypeName);
            assertEquals(productVersion, 6, dict.dateFractionDigits);
            assertTrue(productVersion, dict.fractionalTypeNameSet.contains("DATETIME{0}"));
        }
    }

    @Test
    public void testPre102KeepsWholeSecondTemporalTypes() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("10.1.48-MariaDB"));
        assertEquals("DATETIME", dict.timestampTypeName);
        assertEquals(0, dict.dateFractionDigits);
    }

    @Test
    public void testMajorMinorVersionsStripTheCompatibilityPrefix() {
        assertVersion(10, 3, "10.3.39-MariaDB");
        assertVersion(10, 3, "5.5.5-10.3.39-MariaDB");
        assertVersion(11, 4, "5.5.5-11.4.2-MariaDB-ubu2404");
        assertVersion(10, 2, "5.5.5-10.2.44-MariaDB");
    }

    private static void assertVersion(int maj, int min, String productVersion) {
        int[] versions = MariaDBDictionary.getMajorMinorVersions(productVersion);
        assertEquals(productVersion, maj, versions[0]);
        assertEquals(productVersion, min, versions[1]);
    }

    @Test
    public void testExplicitNextSequenceQuerySurvivesOnModernMariaDB() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary("NextSequenceQuery='SELECT NEXT VALUE FOR {0}'");
        assertEquals("SELECT NEXT VALUE FOR {0}", dict.nextSequenceQuery);
        dict.connectedConfiguration(connection("11.8.2-MariaDB"));
        assertEquals("an explicitly configured NextSequenceQuery must not be overridden",
            "SELECT NEXT VALUE FOR {0}", dict.nextSequenceQuery);
    }

    @Test
    public void testExplicitNextSequenceQuerySurvivesOnLegacyMariaDB() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary("NextSequenceQuery='SELECT NEXT VALUE FOR {0}'");
        dict.connectedConfiguration(connection("10.2.44-MariaDB"));
        assertEquals("SELECT NEXT VALUE FOR {0}", dict.nextSequenceQuery);
    }

    @Test
    public void testProgrammaticPublicFieldAssignmentSurvives() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.nextSequenceQuery = "SELECT NEXT VALUE FOR {0}";
        dict.connectedConfiguration(connection("11.8.2-MariaDB"));
        assertEquals("SELECT NEXT VALUE FOR {0}", dict.nextSequenceQuery);
    }

    @Test
    public void testDictionaryDeclaresNoSequenceSupportBeforeConnecting() {
        assertNull(new MariaDBDictionary().nextSequenceQuery);
    }

    /**
     * OpenJPA caches sequence values itself, so the server side cache only
     * adds gaps: the sequence is created with NOCACHE.
     */
    @Test
    public void testCreateSequenceIsNoCache() throws Exception {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection("11.8.2-MariaDB"));
        String[] sql = dict.getCreateSequenceSQL(sequence());
        assertNotNull(sql);
        assertEquals(1, sql.length);
        assertTrue(sql[0], sql[0].toUpperCase().startsWith("CREATE SEQUENCE "));
        assertTrue("expected a NOCACHE suffix, got " + sql[0], sql[0].endsWith(" NOCACHE"));
    }

    /**
     * The predicates have to appear in the order
     * {@link DBDictionary#getSequences} binds them, and only for the
     * identifiers it binds. Without a schema the query must still be scoped
     * to the connected database, because INFORMATION_SCHEMA spans the whole
     * server.
     */
    @Test
    public void testSequencesSQLIsScopedToTheConnectedDatabase() {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        String sql = dict.getSequencesSQL(DBIdentifier.NULL, DBIdentifier.NULL);
        assertTrue(sql, sql.contains("TABLE_TYPE = 'SEQUENCE'"));
        assertTrue("an unqualified lookup must not span every database on the server: " + sql,
            sql.contains("TABLE_SCHEMA = DATABASE()"));
        assertEquals("no identifier is bound, so there must be no parameter marker: " + sql,
            -1, sql.indexOf('?'));
    }

    @Test
    public void testSequencesSQLBindsTheSchemaOnly() {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        String sql = dict.getSequencesSQL(DBIdentifier.newSchema("APPDB"), DBIdentifier.NULL);
        assertTrue(sql, sql.contains("UPPER(TABLE_SCHEMA) = UPPER(?)"));
        assertTrue("an explicit schema replaces the DATABASE() scope: " + sql,
            !sql.contains("DATABASE()"));
        assertEquals("exactly one bound identifier: " + sql, sql.indexOf('?'), sql.lastIndexOf('?'));
    }

    @Test
    public void testSequencesSQLBindsTheSequenceOnly() {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        String sql = dict.getSequencesSQL(DBIdentifier.NULL, DBIdentifier.newSequence("OJPA3014_SEQ"));
        assertTrue(sql, sql.contains("TABLE_SCHEMA = DATABASE()"));
        assertTrue(sql, sql.contains("UPPER(TABLE_NAME) = UPPER(?)"));
        assertEquals("exactly one bound identifier: " + sql, sql.indexOf('?'), sql.lastIndexOf('?'));
    }

    @Test
    public void testSequencesSQLBindsSchemaBeforeSequence() {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        String sql = dict.getSequencesSQL(DBIdentifier.newSchema("APPDB"),
            DBIdentifier.newSequence("OJPA3014_SEQ"));
        int schemaIdx = sql.indexOf("UPPER(TABLE_SCHEMA)");
        int seqIdx = sql.indexOf("UPPER(TABLE_NAME)");
        assertTrue(sql, schemaIdx > 0 && seqIdx > 0);
        assertTrue("getSequences() binds the schema first: " + sql, schemaIdx < seqIdx);
    }

    /**
     * A column declared with <code>columnDefinition = "json"</code> must not
     * get the column length appended, MariaDB rejects a size on JSON (OPENJPA-3018).
     */
    @Test
    public void testJsonColumnDefinitionHasNoSize() {
        MariaDBDictionary dict = newConfiguredDictionary(null);
        assertEquals("json", dict.getTypeName(column(Types.VARCHAR, "json", 255)));
        assertEquals("VARCHAR(255)", dict.getTypeName(column(Types.VARCHAR, null, 255)));
    }

    private static Sequence sequence() {
        Schema schema = new Schema();
        Sequence seq = new Sequence(DBIdentifier.newSequence("OJPA3014_SEQ"), schema);
        seq.setInitialValue(100);
        seq.setIncrement(7);
        return seq;
    }

    /**
     * Creates a dictionary configured the same way user supplied
     * <code>openjpa.jdbc.DBDictionary=mariadb(...)</code> plugin properties
     * are applied.
     */
    private static MariaDBDictionary newConfiguredDictionary(String props) {
        MariaDBDictionary dict = new MariaDBDictionary();
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), props, "DBDictionary");
        return dict;
    }

    /**
     * A {@link Connection} whose {@link DatabaseMetaData} reports the given
     * MariaDB product version and sensible defaults for everything else.
     */
    private static Connection connection(final String productVersion) {
        final DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
            TestMariaDBDictionary.class.getClassLoader(), new Class<?>[] { DatabaseMetaData.class },
            new DefaultHandler() {
                @Override
                protected Object handle(Method method, Object[] args) {
                    switch (method.getName()) {
                        case "getDatabaseProductName":
                            return "MySQL";
                        case "getDatabaseProductVersion":
                            return productVersion;
                        case "getJDBCMajorVersion":
                            return 4;
                        case "getJDBCMinorVersion":
                            return 2;
                        case "getDriverName":
                            return "MariaDB Connector/J";
                        case "getDriverVersion":
                            return "3.4.1";
                        case "getIdentifierQuoteString":
                            return "`";
                        case "getURL":
                            return "jdbc:mariadb://localhost/test";
                        default:
                            return null;
                    }
                }
            });
        return (Connection) Proxy.newProxyInstance(TestMariaDBDictionary.class.getClassLoader(),
            new Class<?>[] { Connection.class }, new DefaultHandler() {
                @Override
                protected Object handle(Method method, Object[] args) {
                    if ("getMetaData".equals(method.getName())) {
                        return metaData;
                    }
                    return null;
                }
            });
    }

    /**
     * Invocation handler returning type appropriate defaults (false / 0 / "" / null)
     * for every method unless {@link #handle(Method, Object[])} provides a value.
     */
    private abstract static class DefaultHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "toString":
                    return "proxy";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    break;
            }
            Object value = handle(method, args);
            return value != null ? value : defaultValue(method.getReturnType());
        }

        protected abstract Object handle(Method method, Object[] args);

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class) {
                return Boolean.FALSE;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == float.class) {
                return 0f;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == char.class) {
                return (char) 0;
            }
            if (type == String.class) {
                return "";
            }
            return null;
        }
    }
}

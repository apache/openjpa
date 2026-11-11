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
import static org.apache.openjpa.jdbc.sql.Columns.decimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfigurationImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.jdbc.ConfiguringConnectionDecorator;
import org.apache.openjpa.lib.jdbc.LoggingConnectionDecorator;
import org.apache.openjpa.meta.JavaTypes;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestMySQLDictionary {
    @RegisterExtension
    public Mockery context = new JUnit5Mockery();

    /**
     * Puts back the delimiting of reserved words that a configured MySQL or
     * MariaDB dictionary sets for the whole JVM.
     */
    @AfterEach
    public void tearDown() {
        TestAbstractMySQLDictionary.resetDelimitingOfReservedWords();
    }

    @Test
    public void testDBDictionaryGetBatchFetchSize() throws Exception {
        DBDictionary db = new MySQLDictionary();
        assertEquals(Integer.MIN_VALUE, db.getBatchFetchSize(1));
    }



    /**
     * <P>
     * Ensure that <code>SQLBuffer.prepareStatement</code> calls
     * <code>setFetchSize(Integer.MIN_VALUE)</code> when using MySQL.
     * </P>
     *
     * @throws Exception
     *             If any of the expectations are not met or any unexpected
     *             method calls are made
     */
    @Test
    public void testPreparedStatementGetFetchBatchSize() throws Exception {
        DBDictionary db = new MySQLDictionary();
        SQLBuffer sql = new SQLBuffer(db);

        final PreparedStatement mockStatement = context.mock(PreparedStatement.class);
        final Connection mockConnection = context.mock(Connection.class);

        // Expected method calls on the mock objects above. If any of these are
        // do not occur, or if any other methods are invoked on the mock objects
        // an exception will be thrown and the test will fail.
        context.checking(new Expectations()
        {
            {
                oneOf(mockConnection).prepareStatement(with(any(String.class)));
                will(returnValue(mockStatement));
                oneOf(mockStatement).setFetchSize(Integer.MIN_VALUE);
            }
        });

        JDBCFetchConfiguration fetch = new JDBCFetchConfigurationImpl();
        fetch.setResultSetType(ResultSet.TYPE_FORWARD_ONLY);
        fetch.setFetchBatchSize(1);

        sql.prepareStatement(mockConnection, fetch, -1, -1);
    }

    /**
     * <P>
     * Ensure that <code>SQLBuffer.prepareCall()</code> calls
     * <code>setFetchSize(Integer.MIN_VALUE)</code> when using MySQL.
     * </P>
     *
     * @throws Exception
     *             If any of the expectations are not met or any unexpected
     *             method calls are made
     */
    @Test
    public void testPreparedCallGetFetchBatchSize() throws Exception {
        DBDictionary db = new MySQLDictionary();
        SQLBuffer sql = new SQLBuffer(db);

        final CallableStatement mockStatement = context.mock(CallableStatement.class);
        final Connection mockConnection = context.mock(Connection.class);

        // Expected method calls on the mock objects above. If any of these are
        // do not occur, or if any other methods are invoked on the mock objects
        // an exception will be thrown and the test will fail.
        context.checking(new Expectations() {
            {
                oneOf(mockConnection).prepareCall(with(any(String.class)));
                will(returnValue(mockStatement));
                oneOf(mockStatement).setFetchSize(Integer.MIN_VALUE);
            }
        });

        JDBCFetchConfiguration fetch = new JDBCFetchConfigurationImpl();
        fetch.setResultSetType(ResultSet.TYPE_FORWARD_ONLY);
        fetch.setFetchBatchSize(1);

        sql.prepareCall(mockConnection, fetch, -1, -1);
    }

    /**
     * A column declared with <code>columnDefinition = "json"</code> must not
     * get the column length appended, MySQL rejects a size on JSON (OPENJPA-3018).
     */
    @Test
    public void testJsonColumnDefinitionHasNoSize() {
        DBDictionary db = new MySQLDictionary();
        assertEquals("json", db.getTypeName(column(Types.VARCHAR, "json", 255)));
        assertEquals("VARCHAR(255)", db.getTypeName(column(Types.VARCHAR, null, 255)));
    }

    @Test
    public void testExtractWeekIsIsoWeek() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("WEEK({1}, 3)", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.WEEK));
        assertEquals("EXTRACT(DAY FROM {1})", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.DAY));
    }

    @Test
    public void testExtractSecondKeepsFraction() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("(EXTRACT(SECOND FROM {1}) + EXTRACT(MICROSECOND FROM {1}) / 1000000.0)",
            dict.getExtractDateTimeFieldFunction(DateTimeExtractField.SECOND));
    }

    /**
     * MySQL reads an unsized <code>DECIMAL</code> as <code>DECIMAL(10,0)</code>,
     * which rounds every stored value to an integer. The precision and scale
     * of a decimal column are written into the DDL, and an unsized decimal
     * column of a non integral Java type gets the largest precision with
     * room for a fraction (OPENJPA-3062).
     */
    @Test
    public void testDecimalPrecisionAndScaleAreKept() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("NUMERIC(10, 2)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));
        assertEquals("NUMERIC(10)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 0)));
        assertEquals("NUMERIC(19)", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 19, 0)));
        assertEquals("NUMERIC(12, 4)", dict.getTypeName(decimal(JavaTypes.DOUBLE, 12, 4)));

        Column col = decimal(JavaTypes.BIGDECIMAL, 8, 3);
        col.setType(Types.DECIMAL);
        assertEquals("DECIMAL(8, 3)", dict.getTypeName(col));
    }

    @Test
    public void testUnsizedDecimalKeepsFraction() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("NUMERIC(65, 30)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("NUMERIC(65, 30)", dict.getTypeName(decimal(JavaTypes.NUMBER, 0, 0)));
        assertEquals("NUMERIC(65, 2)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 2)));
        assertEquals("NUMERIC(65, 3)", dict.getTypeName(decimal(JavaTypes.DOUBLE, 0, 3)));

        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setType(Types.DECIMAL);
        assertEquals("DECIMAL(65, 30)", dict.getTypeName(col));
    }

    /**
     * A scale above the largest scale the database allows is lowered to it,
     * so that the column can still be created.
     */
    @Test
    public void testUnsizedDecimalScaleIsCapped() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("NUMERIC(65, 30)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 30)));
        assertEquals("NUMERIC(65, 30)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 35)));
    }

    @Test
    public void testUnsizedIntegralDecimalIsUnchanged() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 0, 0)));
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.LONG, 0, 0)));
    }

    @Test
    public void testDecimalColumnDefinitionIsUnchanged() {
        DBDictionary dict = new MySQLDictionary();
        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setTypeName("NUMERIC");
        assertEquals("NUMERIC", dict.getTypeName(col));
        col.setTypeName("DECIMAL(7,1)");
        assertEquals("DECIMAL(7,1)", dict.getTypeName(col));
    }

    @Test
    public void testConfiguredDecimalTypeNameIsKept() {
        DBDictionary dict = new MySQLDictionary();
        dict.numericTypeName = "DECIMAL(19,4)";
        assertEquals("DECIMAL(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("DECIMAL(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));

        dict.numericTypeName = "DOUBLE";
        assertEquals("DOUBLE", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
    }

    /**
     * Date-time literals are written as typed literals: the driver turns the JDBC escape
     * into a plain string literal, which gives COALESCE or CASE a string result (OPENJPA-2935).
     */
    @Test
    public void testDateTimeLiteralIsTyped() {
        DBDictionary dict = new MySQLDictionary();
        assertEquals("DATE '2020-01-02'", dict.toJDBCEscapedDateTimeLiteral("{d '2020-01-02'}", Literal.TYPE_DATE));
        assertEquals("TIME '03:04:05'", dict.toJDBCEscapedDateTimeLiteral("{t '03:04:05'}", Literal.TYPE_TIME));
        assertEquals("TIMESTAMP '2020-01-02 03:04:05.123456'",
                dict.toJDBCEscapedDateTimeLiteral("{ts '2020-01-02 03:04:05.123456'}", Literal.TYPE_TIMESTAMP));
    }

    /**
     * A replication connection of Connector/J 8 and later, opened for a
     * <code>jdbc:mysql:replication:</code> URL, implements its
     * <code>ReplicationConnection</code> interface. It is recognized behind
     * OpenJPA's own decorators, which the store's connection always has.
     */
    @Test
    public void testReplicationConnectionBehindDecorators() throws Exception {
        MySQLDictionary dict = new ReplicationTestDictionary();
        assertTrue(dict.isReplicationConnection(decorated(connection(FakeReplicationConnection.class))));
        assertTrue(dict.isReplicationConnection(connection(FakeReplicationConnection.class)));
        assertFalse(dict.isReplicationConnection(decorated(connection())));
    }

    /**
     * A connection pool's wrapper does not implement the interface itself, but
     * is a wrapper for it.
     */
    @Test
    public void testReplicationConnectionBehindPoolWrapper() throws Exception {
        MySQLDictionary dict = new ReplicationTestDictionary();
        assertTrue(dict.isReplicationConnection(decorated(poolWrapper(connection(FakeReplicationConnection.class)))));
        assertFalse(dict.isReplicationConnection(decorated(poolWrapper(connection()))));
    }

    /**
     * The configured driver and URL do not make a connection of another data
     * source, e.g. a plain one of <code>openjpa.Connection2URL</code>, a
     * replication connection.
     */
    @Test
    public void testConfigurationDoesNotOverrideConnection() throws Exception {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName("com.mysql.jdbc.ReplicationDriver");
        conf.setConnectionURL("jdbc:mysql:replication://source,replica/test");
        MySQLDictionary dict = new ReplicationTestDictionary();
        dict.setConfiguration(conf);
        assertFalse(dict.isReplicationConnection(decorated(connection())));
    }

    /**
     * Without the driver's replication interfaces, the legacy
     * <code>com.mysql.jdbc.ReplicationDriver</code> of Connector/J 5 is still
     * recognized by its configured name.
     */
    @Test
    public void testLegacyReplicationDriverName() throws Exception {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName("com.mysql.jdbc.ReplicationDriver");
        MySQLDictionary dict = new NoDriverDictionary();
        dict.setConfiguration(conf);
        assertTrue(dict.isReplicationConnection(decorated(connection())));

        conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName("com.mysql.cj.jdbc.Driver");
        dict = new NoDriverDictionary();
        dict.setConfiguration(conf);
        assertFalse(dict.isReplicationConnection(decorated(connection())));
    }

    /**
     * Decorating a connection leaves its read-only state alone; the store
     * decides when a replication connection is read-only.
     */
    @Test
    public void testDecorateLeavesConnectionWritable() throws Exception {
        MySQLDictionary dict = new ReplicationTestDictionary();
        dict.setConfiguration(new JDBCConfigurationImpl());
        dict.connected = true;
        Connection conn = connection(FakeReplicationConnection.class);
        dict.decorate(conn);
        assertFalse(conn.isReadOnly());
    }

    /**
     * Stands in for Connector/J's <code>ReplicationConnection</code>; the
     * driver is not on the test class path.
     */
    public interface FakeReplicationConnection extends Connection {
    }

    /**
     * A dictionary that looks for the stand-in instead of Connector/J's
     * interfaces, by name like for those.
     */
    private static class ReplicationTestDictionary extends MySQLDictionary {
        @Override
        protected String[] getReplicationConnectionTypeNames() {
            return new String[] { FakeReplicationConnection.class.getName() };
        }
    }

    /**
     * A dictionary that cannot load the driver's replication interfaces.
     */
    private static class NoDriverDictionary extends MySQLDictionary {
        @Override
        protected String[] getReplicationConnectionTypeNames() {
            return new String[] { "org.apache.openjpa.jdbc.sql.NoSuchReplicationConnection" };
        }
    }

    /**
     * The connection wrapped by OpenJPA's logging and configuring decorators,
     * as a data source of the store hands it out.
     */
    private static Connection decorated(Connection conn) throws Exception {
        ConfiguringConnectionDecorator configuring = new ConfiguringConnectionDecorator();
        configuring.setAutoCommit(Boolean.TRUE);
        return configuring.decorate(new LoggingConnectionDecorator().decorate(conn));
    }

    /**
     * A pool's wrapper, which delegates <code>isWrapperFor</code>.
     */
    private static Connection poolWrapper(Connection conn) {
        return (Connection) Proxy.newProxyInstance(TestMySQLDictionary.class.getClassLoader(),
            new Class<?>[] { Connection.class }, (proxy, method, args) -> {
                if ("isWrapperFor".equals(method.getName())) {
                    return ((Class<?>) args[0]).isInstance(conn) || conn.isWrapperFor((Class<?>) args[0]);
                }
                return method.invoke(conn, args);
            });
    }

    /**
     * A connection that keeps its read-only and auto-commit state and, like
     * the connections of Connector/J, is a wrapper for the interfaces it
     * implements.
     */
    private static Connection connection(Class<?>... interfaces) {
        Class<?>[] types = new Class<?>[interfaces.length + 1];
        types[0] = Connection.class;
        System.arraycopy(interfaces, 0, types, 1, interfaces.length);
        AtomicBoolean readOnly = new AtomicBoolean();
        AtomicBoolean autoCommit = new AtomicBoolean(true);
        return (Connection) Proxy.newProxyInstance(TestMySQLDictionary.class.getClassLoader(), types,
            (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "setReadOnly" -> {
                        readOnly.set((Boolean) args[0]);
                        yield null;
                    }
                    case "isReadOnly" -> readOnly.get();
                    case "setAutoCommit" -> {
                        autoCommit.set((Boolean) args[0]);
                        yield null;
                    }
                    case "getAutoCommit" -> autoCommit.get();
                    case "isWrapperFor" -> ((Class<?>) args[0]).isInstance(proxy);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "connection";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
    }

    /**
     * Before 5.7 MySQL has no fractions of a second in its temporal columns.
     */
    @Test
    public void testWholeSecondTemporalTypesBefore57() throws Exception {
        MySQLDictionary dict = newConfiguredDictionary();
        dict.connectedConfiguration(connection(5, 6, "5.6.51", 8));
        assertEquals("DATETIME", dict.timestampTypeName);
        assertEquals("TIME", dict.timeTypeName);
        assertEquals(0, dict.dateFractionDigits);
        assertFalse(dict.fractionalTypeNameSet.contains("DATETIME{0}"));
        assertFalse(dict.fractionalTypeNameSet.contains("TIME{0}"));
        assertTrue(dict.supportsXMLColumn);
        assertTrue(dict.supportsSubselect);
        assertFalse(dict.driverDeserializesBlobs);
    }

    @Test
    public void testFractionalSecondTemporalTypesFrom57() throws Exception {
        for (int[] version : new int[][] { { 5, 7 }, { 8, 0 }, { 8, 4 } }) {
            String name = version[0] + "." + version[1];
            MySQLDictionary dict = newConfiguredDictionary();
            dict.connectedConfiguration(connection(version[0], version[1], name + ".0", 8));
            assertEquals("DATETIME{0}", dict.timestampTypeName, name);
            assertEquals("TIME{0}", dict.timeTypeName, name);
            assertEquals(6, dict.dateFractionDigits, name);
            assertTrue(dict.fractionalTypeNameSet.contains("DATETIME{0}"), name);
            assertTrue(dict.fractionalTypeNameSet.contains("TIME{0}"), name);
            assertFalse(dict.fixedSizeTypeNameSet.contains("DATETIME{0}"), name);
            assertTrue(dict.supportsXMLColumn, name);
        }
    }

    @Test
    public void testNoXMLColumnBefore51() throws Exception {
        MySQLDictionary dict = newConfiguredDictionary();
        dict.connectedConfiguration(connection(5, 0, "5.0.96", 8));
        assertFalse(dict.supportsXMLColumn);
        assertTrue(dict.supportsSubselect);
        assertEquals("DATETIME", dict.timestampTypeName);
    }

    @Test
    public void testNoSubselectsBefore41() throws Exception {
        MySQLDictionary dict = newConfiguredDictionary();
        dict.connectedConfiguration(connection(4, 0, "4.0.27", 8));
        assertFalse(dict.supportsSubselect);
        assertFalse(dict.allowsAliasInBulkClause);
        assertFalse(dict.supportsForeignKeysComposite);
        assertFalse(dict.supportsXMLColumn);
    }

    /**
     * A driver that is not JDBC 3 compliant reports the server version only
     * in the product version, which is parsed instead.
     */
    @Test
    public void testProductVersionOfNonJDBC3Driver() throws Exception {
        MySQLDictionary old = newConfiguredDictionary();
        old.connectedConfiguration(connection(2, 0, 0, "4.0.27-nt", 8));
        assertFalse(old.supportsSubselect);

        MySQLDictionary current = newConfiguredDictionary();
        current.connectedConfiguration(connection(2, 0, 0, "5.7.44-log", 8));
        assertTrue(current.supportsSubselect);
        assertEquals("DATETIME{0}", current.timestampTypeName);
        assertEquals(6, current.dateFractionDigits);
    }

    @Test
    public void testDriverBefore5DeserializesBlobs() throws Exception {
        MySQLDictionary dict = newConfiguredDictionary();
        dict.connectedConfiguration(connection(8, 0, "8.0.40", 4));
        assertTrue(dict.driverDeserializesBlobs);
    }

    private static MySQLDictionary newConfiguredDictionary() {
        MySQLDictionary dict = new MySQLDictionary();
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), (String) null, "DBDictionary");
        return dict;
    }

    /**
     * A {@link Connection} of a JDBC 4 driver whose {@link DatabaseMetaData}
     * reports the given MySQL server and driver versions.
     */
    private static Connection connection(int major, int minor, String productVersion, int driverMajor) {
        return connection(4, major, minor, productVersion, driverMajor);
    }

    private static Connection connection(int jdbcMajor, int major, int minor, String productVersion,
        int driverMajor) {
        DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
            TestMySQLDictionary.class.getClassLoader(), new Class<?>[] { DatabaseMetaData.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getDatabaseProductName" -> "MySQL";
                case "getDatabaseProductVersion" -> productVersion;
                case "getDatabaseMajorVersion" -> major;
                case "getDatabaseMinorVersion" -> minor;
                case "getJDBCMajorVersion" -> jdbcMajor;
                case "getJDBCMinorVersion" -> 0;
                case "getDriverMajorVersion" -> driverMajor;
                case "getDriverMinorVersion" -> 0;
                case "getDriverName" -> "MySQL Connector/J";
                case "getDriverVersion" -> driverMajor + ".0.0";
                case "getIdentifierQuoteString" -> "`";
                case "getURL" -> "jdbc:mysql://localhost/test";
                case "toString" -> "metaData";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
        return (Connection) Proxy.newProxyInstance(TestMySQLDictionary.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metaData;
                case "toString" -> "connection";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
    }

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
        return null;
    }
}

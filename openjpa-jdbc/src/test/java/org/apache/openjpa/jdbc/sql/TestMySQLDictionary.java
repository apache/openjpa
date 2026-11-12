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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
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
import org.apache.openjpa.lib.jdbc.ConfiguringConnectionDecorator;
import org.apache.openjpa.lib.jdbc.LoggingConnectionDecorator;
import org.apache.openjpa.meta.JavaTypes;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestMySQLDictionary {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

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
}

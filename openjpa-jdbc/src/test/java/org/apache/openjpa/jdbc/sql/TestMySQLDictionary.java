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

import java.sql.CallableStatement;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mysql.cj.jdbc.ha.ReplicationConnection;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfigurationImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
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
     * The legacy <code>com.mysql.jdbc.ReplicationDriver</code> of Connector/J 5
     * still gets read-only connections.
     */
    @Test
    public void testDecorateReadOnlyForLegacyReplicationDriver() throws Exception {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName("com.mysql.jdbc.ReplicationDriver");
        conf.setConnectionURL("jdbc:mysql://source,replica/test");
        AtomicBoolean readOnly = new AtomicBoolean();
        decorate(conf, connection(readOnly));
        assertTrue(readOnly.get());
    }

    /**
     * Connector/J 8 and later dropped the <code>ReplicationDriver</code>; a
     * replication connection is configured with the URL scheme instead.
     */
    @Test
    public void testDecorateReadOnlyForReplicationUrl() throws Exception {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName("com.mysql.cj.jdbc.Driver");
        conf.setConnectionURL("jdbc:mysql:replication://source,replica/test");
        AtomicBoolean readOnly = new AtomicBoolean();
        decorate(conf, connection(readOnly));
        assertTrue(readOnly.get());

        conf = new JDBCConfigurationImpl();
        conf.setConnectionURL("jdbc:mysql+srv:replication://_mysql._tcp.example.com/test");
        readOnly = new AtomicBoolean();
        decorate(conf, connection(readOnly));
        assertTrue(readOnly.get());
    }

    /**
     * A replication connection obtained from a <code>DataSource</code> has no
     * URL in the configuration, but still implements Connector/J's
     * <code>ReplicationConnection</code>, possibly behind a pool's wrapper.
     */
    @Test
    public void testDecorateReadOnlyForReplicationConnection() throws Exception {
        AtomicBoolean readOnly = new AtomicBoolean();
        decorate(new JDBCConfigurationImpl(), connection(readOnly, ReplicationConnection.class));
        assertTrue(readOnly.get());
    }

    @Test
    public void testDecorateLeavesPlainConnectionWritable() throws Exception {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName("com.mysql.cj.jdbc.Driver");
        conf.setConnectionURL("jdbc:mysql://localhost/test");
        AtomicBoolean readOnly = new AtomicBoolean();
        decorate(conf, connection(readOnly));
        assertFalse(readOnly.get());

        readOnly = new AtomicBoolean();
        decorate(new JDBCConfigurationImpl(), connection(readOnly));
        assertFalse(readOnly.get());
    }

    private static void decorate(JDBCConfiguration conf, Connection conn) throws Exception {
        MySQLDictionary dict = new MySQLDictionary();
        dict.setConfiguration(conf);
        dict.connected = true;
        assertTrue(conn == dict.decorate(conn));
    }

    /**
     * A connection that records <code>setReadOnly</code> and, like the
     * connections of Connector/J, is a wrapper for the given interface.
     */
    private static Connection connection(AtomicBoolean readOnly, Class<?>... interfaces) {
        Class<?>[] types = new Class<?>[interfaces.length + 1];
        types[0] = Connection.class;
        System.arraycopy(interfaces, 0, types, 1, interfaces.length);
        return (Connection) Proxy.newProxyInstance(TestMySQLDictionary.class.getClassLoader(), types,
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "setReadOnly":
                        readOnly.set((Boolean) args[0]);
                        return null;
                    case "isWrapperFor":
                        return ((Class<?>) args[0]).isInstance(proxy);
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }
}

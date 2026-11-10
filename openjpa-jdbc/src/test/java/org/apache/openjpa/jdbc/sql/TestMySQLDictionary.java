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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfigurationImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.Literal;
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
}

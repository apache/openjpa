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

import static org.apache.openjpa.jdbc.sql.Columns.decimal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.Test;

public class TestSQLServerDictionary {

    /**
     * SQL Server reads an unsized <code>DECIMAL</code> as <code>DECIMAL(18,0)</code>,
     * which rounds every stored value to an integer. The precision and scale
     * of a decimal column are written into the DDL (OPENJPA-3064).
     */
    @Test
    public void testDecimalPrecisionAndScaleAreKept() {
        DBDictionary dict = new SQLServerDictionary();
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
        DBDictionary dict = new SQLServerDictionary();
        assertEquals("NUMERIC(38, 10)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("NUMERIC(38, 10)", dict.getTypeName(decimal(JavaTypes.NUMBER, 0, 0)));
        assertEquals("NUMERIC(38, 2)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 2)));
        assertEquals("NUMERIC(38, 3)", dict.getTypeName(decimal(JavaTypes.DOUBLE, 0, 3)));
        assertEquals("NUMERIC(38, 38)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 40)));

        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setType(Types.DECIMAL);
        assertEquals("DECIMAL(38, 10)", dict.getTypeName(col));
    }

    @Test
    public void testUnsizedIntegralDecimalIsUnchanged() {
        DBDictionary dict = new SQLServerDictionary();
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.BIGINTEGER, 0, 0)));
        assertEquals("NUMERIC", dict.getTypeName(decimal(JavaTypes.LONG, 0, 0)));
    }

    @Test
    public void testDecimalColumnDefinitionIsUnchanged() {
        DBDictionary dict = new SQLServerDictionary();
        Column col = decimal(JavaTypes.BIGDECIMAL, 0, 0);
        col.setTypeName("NUMERIC");
        assertEquals("NUMERIC", dict.getTypeName(col));
        col.setTypeName("DECIMAL(7,1)");
        assertEquals("DECIMAL(7,1)", dict.getTypeName(col));
    }

    @Test
    public void testConfiguredDecimalTypeNameIsKept() {
        DBDictionary dict = new SQLServerDictionary();
        dict.numericTypeName = "DECIMAL(19,4)";
        assertEquals("DECIMAL(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
        assertEquals("DECIMAL(19,4)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 10, 2)));

        dict.numericTypeName = "FLOAT(53)";
        assertEquals("FLOAT(53)", dict.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 0)));
    }

    /**
     * DATEPART(SECOND) returns the whole seconds only; the fraction that JPA
     * includes is added from DATEPART(NANOSECOND) (OPENJPA-3064).
     */
    @Test
    public void testExtractSecondKeepsFraction() {
        DBDictionary dict = new SQLServerDictionary();
        assertEquals("(DATEPART(SECOND, {1}) + DATEPART(NANOSECOND, {1}) / 1000000000.0)",
            dict.getExtractDateTimeFieldFunction(DateTimeExtractField.SECOND));
    }

    /**
     * DATEPART(WEEK) counts weeks from January 1st; JPA defines the ISO-8601
     * week (OPENJPA-3064).
     */
    @Test
    public void testExtractWeekIsIsoWeek() {
        DBDictionary dict = new SQLServerDictionary();
        assertEquals("DATEPART(ISO_WEEK, {1})", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.WEEK));
        assertEquals("DATEPART(DAY, {1})", dict.getExtractDateTimeFieldFunction(DateTimeExtractField.DAY));
    }

    /**
     * SQL Server refuses to drop a column that the primary key or an index
     * depends on, so these are dropped first, in the same transaction as the
     * column (OPENJPA-3064).
     */
    @Test
    public void testDropColumnDropsDependentObjectsFirst() {
        DBDictionary dict = new SQLServerDictionary();
        Table table = new Table(DBIdentifier.newTable("TPRODUCT"), null);
        Column col = table.addColumn(DBIdentifier.newColumn("id"));
        String[] sql = dict.getDropColumnSQL(col);
        assertEquals(1, sql.length);
        assertTrue(sql[0], sql[0].contains("FROM sys.foreign_keys f"));
        assertTrue(sql[0], sql[0].contains("FROM sys.indexes i"));
        assertTrue(sql[0], sql[0].contains("FROM sys.default_constraints d"));
        assertTrue(sql[0], sql[0].contains("COLUMNPROPERTY(OBJECT_ID(N'TPRODUCT'), N'id', 'ColumnId')"));
        assertTrue(sql[0], sql[0].contains("BEGIN TRY BEGIN TRANSACTION; EXEC sp_executesql @sql; "
            + "ALTER TABLE TPRODUCT DROP COLUMN id; COMMIT TRANSACTION; END TRY"));
        assertTrue(sql[0], sql[0].endsWith(
            "BEGIN CATCH IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION; THROW; END CATCH"));

        dict.supportsAlterTableWithDropColumn = false;
        assertEquals(0, dict.getDropColumnSQL(col).length);
    }

    /**
     * SQL Server has sequence objects since 2012 (major version 11), so a
     * <code>@SequenceGenerator</code> is backed by a real sequence there
     * rather than by the OpenJPA sequence table (OPENJPA-3034).
     */
    @Test
    public void testSequenceSupportFrom2012() throws Exception {
        SQLServerDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection(10));
        assertNull("SQL Server 2008 has no sequence objects", dict.nextSequenceQuery);
        assertNull(dict.getCreateSequenceSQL(sequence(10)));

        dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection(14));
        assertEquals("SELECT NEXT VALUE FOR {0}", dict.nextSequenceQuery);
        String[] sql = dict.getCreateSequenceSQL(sequence(10));
        assertEquals(1, sql.length);
        assertEquals("CREATE SEQUENCE OJPA3034_SEQ START WITH 10 INCREMENT BY 5", sql[0]);
        assertEquals("ALTER SEQUENCE OJPA3034_SEQ INCREMENT BY 5", dict.getAlterSequenceSQL(sequence(10)));
        assertEquals("DROP SEQUENCE OJPA3034_SEQ", dict.getDropSequenceSQL(sequence(10))[0]);
    }

    /**
     * Without START WITH a SQL Server sequence starts at the minimum of its
     * type, so an initial value of 0 has to be spelled out.
     */
    @Test
    public void testInitialValueZeroIsExplicit() throws Exception {
        SQLServerDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection(16));
        assertEquals("CREATE SEQUENCE OJPA3034_SEQ START WITH 0 INCREMENT BY 5",
            dict.getCreateSequenceSQL(sequence(0))[0]);
    }

    @Test
    public void testExplicitNextSequenceQuerySurvives() throws Exception {
        SQLServerDictionary dict = newConfiguredDictionary("NextSequenceQuery='SELECT NEXT VALUE FOR {0} AS X'");
        dict.connectedConfiguration(connection(16));
        assertEquals("SELECT NEXT VALUE FOR {0} AS X", dict.nextSequenceQuery);
    }

    /**
     * The predicates appear in the order {@link DBDictionary#getSequences}
     * binds them; without a schema the lookup is scoped to the default schema.
     */
    @Test
    public void testSequencesSQL() throws Exception {
        SQLServerDictionary dict = newConfiguredDictionary(null);
        assertNull("no sequence support, nothing to reflect",
            dict.getSequencesSQL(DBIdentifier.NULL, DBIdentifier.NULL));
        dict.connectedConfiguration(connection(16));
        String sql = dict.getSequencesSQL(DBIdentifier.NULL, DBIdentifier.NULL);
        assertTrue(sql, sql.contains("FROM sys.sequences WHERE schema_id = SCHEMA_ID()"));
        assertEquals(sql, -1, sql.indexOf('?'));
        sql = dict.getSequencesSQL(DBIdentifier.newSchema("dbo"), DBIdentifier.newSequence("S"));
        assertTrue(sql, sql.endsWith("WHERE SCHEMA_NAME(schema_id) = ? AND name = ?"));
    }

    /**
     * A DATETIME column (three fractional digits) keeps multiples of 1/300
     * second; values are bound in multiples of 10 ms, which DATETIME stores
     * exactly, so that a timestamp version compares equal (OPENJPA-3034).
     * Other three digit columns, DATETIME2(3), keep whole milliseconds.
     */
    @Test
    public void testTimestampPrecisionOfDatetimeColumn() {
        SQLServerDictionary dict = new SQLServerDictionary();
        Column col = new Column();
        assertEquals("unknown precision: the dictionary default", dict.datePrecision,
            dict.getTimestampPrecision(col));
        col.setPrecision(3);
        assertEquals("type unknown: three digits are milliseconds", 1_000_000, dict.getTimestampPrecision(col));
        col.setTypeIdentifier(DBIdentifier.newColumnDefinition("datetime2"));
        assertEquals("DATETIME2(3) keeps whole milliseconds", 1_000_000, dict.getTimestampPrecision(col));
        col.setTypeIdentifier(DBIdentifier.newColumnDefinition("datetime"));
        assertEquals(10_000_000, dict.getTimestampPrecision(col));
        col.setTypeIdentifier(DBIdentifier.newColumnDefinition("DATETIME"));
        assertEquals(10_000_000, dict.getTimestampPrecision(col));
        col.setTypeIdentifier(DBIdentifier.newColumnDefinition("datetime2"));
        col.setPrecision(7);
        assertEquals(100, dict.getTimestampPrecision(col));
        col.setPrecision(0);
        assertEquals(1_000_000_000, dict.getTimestampPrecision(col));
    }

    private static Sequence sequence(int initial) {
        Sequence seq = new Sequence(DBIdentifier.newSequence("OJPA3034_SEQ"), new Schema());
        seq.setInitialValue(initial);
        seq.setIncrement(1);
        seq.setAllocate(5);
        return seq;
    }

    private static SQLServerDictionary newConfiguredDictionary(String props) {
        SQLServerDictionary dict = new SQLServerDictionary();
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), props, "DBDictionary");
        return dict;
    }

    /**
     * A {@link Connection} whose {@link DatabaseMetaData} reports a SQL Server
     * of the given major version, accessed through the Microsoft driver.
     */
    private static Connection connection(final int majorVersion) {
        final DatabaseMetaData meta = (DatabaseMetaData) Proxy.newProxyInstance(
            TestSQLServerDictionary.class.getClassLoader(), new Class<?>[] { DatabaseMetaData.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getDatabaseProductName":
                        return "Microsoft SQL Server";
                    case "getDatabaseProductVersion":
                        return majorVersion + ".0.1000";
                    case "getDatabaseMajorVersion":
                        return majorVersion;
                    case "getDriverName":
                        return "Microsoft JDBC Driver 12.4 for SQL Server";
                    case "getDriverMajorVersion":
                        return 12;
                    case "getURL":
                        return "jdbc:sqlserver://localhost;selectMethod=cursor";
                    case "getIdentifierQuoteString":
                        return "\"";
                    default:
                        return defaultValue(method.getReturnType());
                }
            });
        return (Connection) Proxy.newProxyInstance(TestSQLServerDictionary.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            (proxy, method, args) -> "getMetaData".equals(method.getName())
                ? meta : defaultValue(method.getReturnType()));
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
        if (type == short.class) {
            return (short) 0;
        }
        return null;
    }

    /**
     * SQL Server reports a procedure as <code>name;version</code>, its parameters with a
     * leading <code>@</code> and an additional <code>@RETURN_VALUE</code>. The procedure must
     * be called with the JDBC escape syntax, as SQL Server does not know a bare <code>CALL</code>.
     */
    @Test
    public void testStoredProcedureIsCalledWithEscapeSyntax() throws SQLException {
        DBDictionary dict = new SQLServerDictionary();
        dict.setConfiguration(new JDBCConfigurationImpl());
        DatabaseMetaData meta = procedureMetaData("GetEmpFirstNameFromOut;1",
            new Object[] {"@RETURN_VALUE", DatabaseMetaData.procedureColumnReturn, Types.INTEGER, "int"},
            new Object[] {"@IN_PARAM", DatabaseMetaData.procedureColumnIn, Types.INTEGER, "int"},
            new Object[] {"@OUT_PARAM", DatabaseMetaData.procedureColumnInOut, Types.VARCHAR, "varchar"});

        StoredProcedure sp = dict.getStoredProcedure(meta, null, null, "GetEmpFirstNameFromOut");
        assertEquals("GetEmpFirstNameFromOut", sp.getName());
        assertEquals(2, sp.getColumns().length);
        assertEquals("IN_PARAM", sp.getInColumns()[0].getIdentifier().getName());
        assertEquals("OUT_PARAM", sp.getInOutColumns()[0].getIdentifier().getName());
        assertEquals("{call GetEmpFirstNameFromOut(?,?)}", dict.getStoredProcedureCallSQL(sp));

        StoredProcedure noParams = dict.getStoredProcedure(procedureMetaData("DeleteAllEmp;1",
            new Object[] {"@RETURN_VALUE", DatabaseMetaData.procedureColumnReturn, Types.INTEGER, "int"}),
            null, null, "DeleteAllEmp");
        assertEquals("{call DeleteAllEmp}", dict.getStoredProcedureCallSQL(noParams));
    }

    /**
     * Emulates {@link DatabaseMetaData#getProcedureColumns} for a single procedure.
     * Each column is given as name, column type, data type and type name.
     */
    private static DatabaseMetaData procedureMetaData(String procedure, Object[]... columns) {
        InvocationHandler rs = new InvocationHandler() {
            private int row = -1;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                switch (method.getName()) {
                    case "next":
                        return ++row < columns.length;
                    case "close":
                        return null;
                    case "getString":
                        switch ((Integer) args[0]) {
                            case 3: return procedure;
                            case 4: return (String) columns[row][0];
                            case 7: return (String) columns[row][3];
                            default: return null;
                        }
                    case "getShort":
                        return (Integer) args[0] == 5 ? (short) (int) (Integer) columns[row][1] : (short) 0;
                    case "getInt":
                        return (Integer) args[0] == 6 ? (Integer) columns[row][2] : 0;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            }
        };
        return (DatabaseMetaData) Proxy.newProxyInstance(TestSQLServerDictionary.class.getClassLoader(),
            new Class<?>[] {DatabaseMetaData.class}, (proxy, method, args) -> {
                if ("getProcedureColumns".equals(method.getName())) {
                    return Proxy.newProxyInstance(TestSQLServerDictionary.class.getClassLoader(),
                        new Class<?>[] {ResultSet.class}, rs);
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }
}

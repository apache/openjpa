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
import static org.junit.Assert.assertTrue;

import java.sql.Types;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
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
}

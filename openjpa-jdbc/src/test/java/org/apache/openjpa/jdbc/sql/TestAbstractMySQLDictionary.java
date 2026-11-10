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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.meta.JavaTypes;
import org.junit.Test;

/**
 * The defaults that {@link MySQLDictionary} and {@link MariaDBDictionary}
 * share through {@link AbstractMySQLDictionary} and those they keep apart,
 * and the words that MySQL 8.0 and later reserve (OPENJPA-3074).
 */
public class TestAbstractMySQLDictionary {

    private static final List<String> MYSQL_8_WORDS = List.of("RANK", "DENSE_RANK", "ROW_NUMBER", "LAG",
        "LEAD", "GROUPS", "WINDOW", "OVER", "CUME_DIST", "PERCENT_RANK", "NTILE", "FIRST_VALUE", "LAST_VALUE",
        "NTH_VALUE", "RECURSIVE", "LATERAL", "SYSTEM", "EMPTY", "JSON_TABLE", "ROW", "ROWS", "OF", "FUNCTION",
        "CUBE", "GROUPING", "EXCEPT", "INTERSECT", "QUALIFY", "TABLESAMPLE", "EXTERNAL", "LIBRARY");

    @Test
    public void testSharedDefaults() {
        for (AbstractMySQLDictionary dict : List.of(new MySQLDictionary(), new MariaDBDictionary())) {
            String name = dict.getClass().getSimpleName();
            assertEquals(name, "innodb", dict.tableType);
            assertTrue(name, dict.useClobs);
            assertFalse(name, dict.driverDeserializesBlobs);
            assertFalse(name, dict.optimizeMultiTableDeletes);
            assertEquals(name, "`", dict.getLeadingDelimiter());
            assertEquals(name, "SELECT NOW()", dict.validationSQL);
            assertEquals(name, "DATETIME", dict.timestampTypeName);
            assertEquals(name, "TEXT", dict.clobTypeName);
            assertEquals(name, "SIGNED", dict.longCastTypeName);
            assertEquals(name, "CHAR(0)", dict.searchStringNoEscape);
            assertEquals(name, 64, dict.maxColumnNameLength);
            assertTrue(name, dict.backslashEscapesInStringLiterals);
            assertTrue(name, dict.supportsAutoAssign);
            assertFalse(name, dict.storeCharsAsNumbers);
            assertFalse(name, dict.supportsXMLColumn);
            assertEquals(name, 0, dict.dateFractionDigits);
        }
    }

    @Test
    public void testDefaultsThatDiffer() {
        MySQLDictionary mysql = new MySQLDictionary();
        MariaDBDictionary mariadb = new MariaDBDictionary();
        assertEquals("MySQL", mysql.platform);
        assertEquals("MariaDB", mariadb.platform);

        // MariaDB decides by the version of the server, MySQL never allows it
        assertFalse(mysql.allowsBulkTargetInSubselect);
        assertTrue(mariadb.allowsBulkTargetInSubselect);

        assertEquals("SIGNED", mysql.integerCastTypeName);
        assertEquals(new DBDictionary().integerCastTypeName, mariadb.integerCastTypeName);

        assertTrue(mysql.reservedWordSet.contains("LIBRARY"));
        assertFalse(mariadb.reservedWordSet.contains("LIBRARY"));

        assertEquals(Integer.MIN_VALUE, mysql.getBatchFetchSize(100));
        assertEquals(100, mariadb.getBatchFetchSize(100));

        assertNotEquals(MySQLDictionary.SELECT_HINT, MariaDBDictionary.SELECT_HINT);

        // the largest scale of a decimal column
        assertEquals("NUMERIC(65, 30)", mysql.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 38)));
        assertEquals("NUMERIC(65, 38)", mariadb.getTypeName(decimal(JavaTypes.BIGDECIMAL, 0, 38)));
    }

    /**
     * The public fields are still set from the
     * <code>openjpa.jdbc.DBDictionary</code> plugin properties on both
     * dictionaries.
     */
    @Test
    public void testPublicFieldsAreConfigurable() {
        String props = "TableType=MyISAM, UseClobs=false, DriverDeserializesBlobs=true, OptimizeMultiTableDeletes=true";
        for (AbstractMySQLDictionary dict : List.of(new MySQLDictionary(), new MariaDBDictionary())) {
            String name = dict.getClass().getSimpleName();
            Configurations.configureInstance(dict, new JDBCConfigurationImpl(), props, "DBDictionary");
            assertEquals(name, "MyISAM", dict.tableType);
            assertFalse(name, dict.useClobs);
            assertTrue(name, dict.driverDeserializesBlobs);
            assertTrue(name, dict.optimizeMultiTableDeletes);
        }
    }

    /**
     * A subclass of the dictionaries still sees and sets the fields and
     * overrides the methods that moved to the common superclass.
     */
    @Test
    public void testUserSubclass() {
        MySQLDictionary dict = new MySQLDictionary() {
            {
                tableType = "MyISAM";
            }

            @Override
            public String[] getCreateTableSQL(Table table) {
                String[] sql = super.getCreateTableSQL(table);
                sql[0] = sql[0] + " COMMENT = 'custom'";
                return sql;
            }
        };
        Table table = new Table(DBIdentifier.newTable("T1"), null);
        table.addColumn(DBIdentifier.newColumn("C1")).setJavaType(JavaTypes.INT);
        String sql = dict.getCreateTableSQL(table)[0];
        assertTrue(sql, sql.endsWith(" ENGINE = MyISAM COMMENT = 'custom'"));
    }

    /**
     * MySQL delimits the reserved words in identifiers, so a column or table
     * named after such a word keeps its name, quoted.
     */
    @Test
    public void testMySQL8ReservedWordsAreDelimited() {
        MySQLDictionary dict = configure(new MySQLDictionary(), null);
        for (String word : MYSQL_8_WORDS) {
            assertTrue(word, dict.getInvalidColumnWordSet().contains(word));
        }
        for (String word : MYSQL_8_WORDS) {
            if (!"NTILE".equals(word)) {
                assertTrue(word, dict.reservedWordSet.contains(word));
            }
        }
        assertEquals("\"rank\"", validColumnName(dict, "rank"));
        assertEquals("\"window\"", validColumnName(dict, "window"));
        assertEquals("\"ROW_NUMBER\"", validColumnName(dict, "ROW_NUMBER"));
        assertEquals("RANKING", validColumnName(dict, "ranking"));
        // as an earlier release did for a word it already knew as invalid only
        assertEquals("NTILE0", validColumnName(dict, "ntile"));

        assertEquals("\"Function\"", validTableName(dict, "Function"));
        assertEquals("\"WINDOW\"", validTableName(dict, "WINDOW"));
        assertEquals("RANKING", validTableName(dict, "RANKING"));
    }

    /**
     * MariaDB does not delimit reserved words: a name that is such a word
     * gets a <code>0</code> appended.
     */
    @Test
    public void testMariaDBAppendsZeroToReservedTableNames() {
        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), null);
        assertEquals("LINEAR0", validTableName(mariadb, "LINEAR"));
        assertEquals("RANK", validTableName(mariadb, "RANK"));
    }

    /**
     * MariaDB reserves few of the words: <code>RANK</code> or
     * <code>WINDOW</code> are valid column names there.
     */
    @Test
    public void testMariaDBReservesOnlySomeOfTheWords() {
        MariaDBDictionary dict = configure(new MariaDBDictionary(), null);
        assertEquals("RANK", validColumnName(dict, "rank"));
        assertEquals("WINDOW", validColumnName(dict, "window"));
        assertEquals("ROW_NUMBER0", validColumnName(dict, "row_number"));
        assertEquals("OVER0", validColumnName(dict, "over"));
    }

    /**
     * <code>UpdatedMySQLReservedWords=false</code> keeps the names an earlier
     * release generated. The words that were reserved before, such as
     * <code>ROWS</code> of the SQL standard or the <code>LIBRARY</code> that
     * MySQL reserved already, are still delimited on MySQL.
     */
    @Test
    public void testCompatibilityKeepsTheEarlierNames() {
        MySQLDictionary mysql = configure(new MySQLDictionary(), "UpdatedMySQLReservedWords=false");
        assertEquals("RANK", validColumnName(mysql, "rank"));
        assertEquals("WINDOW", validColumnName(mysql, "window"));
        assertFalse(mysql.getInvalidColumnWordSet().contains("RANK"));
        assertFalse(mysql.reservedWordSet.contains("RANK"));
        assertEquals("NTILE0", validColumnName(mysql, "ntile"));
        assertEquals("FUNCTION", validTableName(mysql, "FUNCTION"));
        assertEquals("WINDOW", validTableName(mysql, "WINDOW"));
        assertEquals("\"ROWS\"", validTableName(mysql, "ROWS"));
        assertEquals("\"LIBRARY\"", validTableName(mysql, "LIBRARY"));

        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), "UpdatedMySQLReservedWords=false");
        assertEquals("ROW_NUMBER", validColumnName(mariadb, "row_number"));
        assertEquals("OVER0", validColumnName(mariadb, "over"));
        assertEquals("LINEAR", validTableName(mariadb, "LINEAR"));
    }

    private static <T extends DBDictionary> T configure(T dict, String compatibility) {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        if (compatibility != null) {
            conf.setCompatibility(compatibility);
        }
        Configurations.configureInstance(dict, conf, (String) null, "DBDictionary");
        return dict;
    }

    private static String validTableName(DBDictionary dict, String name) {
        return dict.getValidTableName(DBIdentifier.newTable(name), new Schema()).getName();
    }

    private static String validColumnName(DBDictionary dict, String name) {
        return dict.getValidColumnName(DBIdentifier.newColumn(name), new Table()).getName();
    }
}

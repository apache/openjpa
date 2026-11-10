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
import static org.junit.Assert.assertSame;
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
 * and the words that MySQL 8.0 and later and MariaDB reserve (OPENJPA-3074).
 */
public class TestAbstractMySQLDictionary {

    private static final List<String> MYSQL_8_WORDS = List.of("RANK", "DENSE_RANK", "ROW_NUMBER", "LAG",
        "LEAD", "GROUPS", "WINDOW", "OVER", "CUME_DIST", "PERCENT_RANK", "NTILE", "FIRST_VALUE", "LAST_VALUE",
        "NTH_VALUE", "RECURSIVE", "LATERAL", "SYSTEM", "EMPTY", "JSON_TABLE", "ROW", "ROWS", "OF", "FUNCTION",
        "CUBE", "GROUPING", "EXCEPT", "INTERSECT", "QUALIFY", "TABLESAMPLE", "EXTERNAL", "LIBRARY");

    /**
     * Words that only MariaDB reserves.
     */
    private static final List<String> MARIADB_WORDS = List.of("OFFSET", "RETURNING", "PORTION", "PAGE_CHECKSUM",
        "STATS_AUTO_RECALC", "STATS_PERSISTENT", "STATS_SAMPLE_PAGES", "REF_SYSTEM_ID", "DELETE_DOMAIN_ID",
        "DO_DOMAIN_IDS", "IGNORE_DOMAIN_IDS", "MASTER_DEMOTE_TO_REPLICA", "MASTER_DEMOTE_TO_SLAVE", "PARSE_VCOL_EXPR",
        "MASTER_SSL_VERIFY_SERVER_CERT", "VECTOR", "TO_DATE", "CONVERSION", "CURRENT_PATH", "SQL_AFTER_GTIDS",
        "SQL_BEFORE_GTIDS");

    /**
     * Some of the invalid column words that MySQL and MariaDB reserve for
     * table names too.
     */
    private static final List<String> TABLE_WORDS = List.of("DUAL", "KEYS", "LIMIT", "RANGE", "LOCK", "RELEASE",
        "SHOW", "USE", "LONG", "SIGNAL");

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
            assertTrue(word, dict.reservedWordSet.contains(word));
        }
        assertEquals("\"rank\"", validColumnName(dict, "rank"));
        assertEquals("\"window\"", validColumnName(dict, "window"));
        assertEquals("\"ROW_NUMBER\"", validColumnName(dict, "ROW_NUMBER"));
        assertEquals("\"ntile\"", validColumnName(dict, "ntile"));
        assertEquals("RANKING", validColumnName(dict, "ranking"));

        assertEquals("\"Function\"", validTableName(dict, "Function"));
        assertEquals("\"WINDOW\"", validTableName(dict, "WINDOW"));
        assertEquals("\"Ntile\"", validTableName(dict, "Ntile"));
        assertEquals("RANKING", validTableName(dict, "RANKING"));
    }

    /**
     * The words that the generated list of invalid column words has, and
     * that MySQL and MariaDB reserve for table names too, are reserved words,
     * so that a table named after one of them is delimited as well.
     */
    @Test
    public void testInvalidColumnWordsAreReservedForTables() {
        for (AbstractMySQLDictionary dict : List.of(configure(new MySQLDictionary(), null),
            configure(new MariaDBDictionary(), null))) {
            String name = dict.getClass().getSimpleName();
            for (String word : TABLE_WORDS) {
                assertTrue(name + " " + word, dict.getInvalidColumnWordSet().contains(word));
                assertTrue(name + " " + word, dict.reservedWordSet.contains(word));
            }
            assertEquals(name, "\"Dual\"", validTableName(dict, "Dual"));
            assertEquals(name, "\"Range\"", validTableName(dict, "Range"));
            assertEquals(name, "\"limit\"", validColumnName(dict, "limit"));
        }
        assertTrue(configure(new MySQLDictionary(), null).reservedWordSet.contains("NTILE"));
        // MariaDB does not reserve them, so a table keeps its undelimited name
        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), null);
        assertTrue(mariadb.getInvalidColumnWordSet().contains("DATABASE"));
        assertFalse(mariadb.reservedWordSet.contains("DATABASE"));
        assertEquals("DATABASE", validTableName(mariadb, "Database"));
    }

    /**
     * MariaDB delimits the reserved words in identifiers as MySQL does, so a
     * table or column named after such a word keeps its name, quoted.
     */
    @Test
    public void testMariaDBDelimitsReservedWords() {
        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), null);
        assertEquals("\"LINEAR\"", validTableName(mariadb, "LINEAR"));
        assertEquals("\"Order\"", validTableName(mariadb, "Order"));
        assertEquals("\"Offset\"", validTableName(mariadb, "Offset"));
        assertEquals("WINDOW", validTableName(mariadb, "WINDOW"));
        assertEquals("\"order\"", validColumnName(mariadb, "order"));
        assertEquals("\"row_number\"", validColumnName(mariadb, "row_number"));
        assertEquals("\"returning\"", validColumnName(mariadb, "returning"));
    }

    /**
     * A name that is built from a delimited name, such as the default join
     * table or join column of an entity named after a reserved word, is
     * delimited too and keeps the case of that name. With the earlier names
     * it is built from the name with the <code>0</code> appended.
     */
    @Test
    public void testNamesBuiltFromADelimitedName() {
        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), null);
        DBIdentifier order = mariadb.getValidTableName(DBIdentifier.newTable("Order"), new Schema());
        assertEquals("\"Order_items\"", DBIdentifier.combine(order, "items").getName());
        assertEquals("\"Order_ID\"", DBIdentifier.combine(order, "ID").getName());
        MySQLDictionary mysql = configure(new MySQLDictionary(), null);
        DBIdentifier linear = mysql.getValidTableName(DBIdentifier.newTable("Linear"), new Schema());
        assertEquals("\"Linear_items\"", DBIdentifier.combine(linear, "items").getName());

        mariadb = configure(new MariaDBDictionary(), "UpdatedReservedWords=false");
        order = mariadb.getValidTableName(DBIdentifier.newTable("Order"), new Schema());
        assertEquals("ORDER0_items", DBIdentifier.combine(order, "items").getName());
    }

    /**
     * MariaDB reserves few of the words of MySQL 8.0: <code>WINDOW</code> is
     * a valid name there. It reserves words that MySQL does not, such as
     * <code>OFFSET</code> or <code>RETURNING</code>.
     */
    @Test
    public void testMariaDBReservesOnlySomeOfTheWords() {
        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), null);
        assertEquals("WINDOW", validColumnName(mariadb, "window"));
        assertEquals("WINDOW", validTableName(mariadb, "Window"));
        assertEquals("\"over\"", validColumnName(mariadb, "over"));
        MySQLDictionary mysql = configure(new MySQLDictionary(), null);
        for (String word : MARIADB_WORDS) {
            assertTrue(word, mariadb.getInvalidColumnWordSet().contains(word));
            assertTrue(word, mariadb.reservedWordSet.contains(word));
            assertFalse(word, mysql.getInvalidColumnWordSet().contains(word));
        }
    }

    /**
     * The names of built-in functions are reserved in a table name on
     * MariaDB, under the <code>IGNORE_SPACE</code> SQL mode that MariaDB
     * Connector/J sets, but they are valid column names. They are delimited
     * as the other reserved words.
     */
    @Test
    public void testMariaDBReservesFunctionNamesForTables() {
        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), null);
        for (String word : List.of("NTILE", "RANK", "NOW", "GROUP_CONCAT", "STDDEV", "COUNT", "MAX")) {
            assertTrue(word, mariadb.reservedWordSet.contains(word));
            assertFalse(word, mariadb.getInvalidColumnWordSet().contains(word));
        }
        assertEquals("\"Ntile\"", validTableName(mariadb, "Ntile"));
        assertEquals("\"Rank\"", validTableName(mariadb, "Rank"));
        assertEquals("\"rank\"", validColumnName(mariadb, "rank"));

        mariadb = configure(new MariaDBDictionary(), "UpdatedReservedWords=false");
        assertFalse(mariadb.reservedWordSet.contains("NTILE"));
        assertEquals("NTILE", validTableName(mariadb, "Ntile"));
        assertEquals("RANK", validColumnName(mariadb, "rank"));
        // the words of the SQL standard were reserved before
        assertEquals("COUNT0", validTableName(mariadb, "Count"));
    }

    /**
     * <code>UpdatedReservedWords=false</code> keeps the names
     * an earlier release generated. The words that were reserved before,
     * such as <code>ROWS</code> of the SQL standard or the
     * <code>LIBRARY</code> that MySQL reserved already, are still delimited
     * on MySQL; MariaDB appends a <code>0</code> to them again.
     */
    @Test
    public void testCompatibilityKeepsTheEarlierNames() {
        MySQLDictionary mysql = configure(new MySQLDictionary(), "UpdatedReservedWords=false");
        assertEquals("RANK", validColumnName(mysql, "rank"));
        assertEquals("WINDOW", validColumnName(mysql, "window"));
        assertFalse(mysql.getInvalidColumnWordSet().contains("RANK"));
        assertFalse(mysql.reservedWordSet.contains("RANK"));
        assertEquals("NTILE0", validColumnName(mysql, "ntile"));
        assertEquals("LIMIT0", validColumnName(mysql, "limit"));
        assertEquals("FUNCTION", validTableName(mysql, "FUNCTION"));
        assertEquals("WINDOW", validTableName(mysql, "WINDOW"));
        assertEquals("DUAL", validTableName(mysql, "Dual"));
        assertEquals("\"ROWS\"", validTableName(mysql, "ROWS"));
        assertEquals("\"LIBRARY\"", validTableName(mysql, "LIBRARY"));
        for (String word : TABLE_WORDS) {
            assertTrue(word, mysql.getInvalidColumnWordSet().contains(word));
            assertFalse(word, mysql.reservedWordSet.contains(word));
        }

        MariaDBDictionary mariadb = configure(new MariaDBDictionary(), "UpdatedReservedWords=false");
        assertEquals("ROW_NUMBER", validColumnName(mariadb, "row_number"));
        assertEquals("OFFSET", validColumnName(mariadb, "offset"));
        assertEquals("OVER0", validColumnName(mariadb, "over"));
        assertEquals("ORDER0", validColumnName(mariadb, "order"));
        assertEquals("LIMIT0", validColumnName(mariadb, "limit"));
        assertEquals("LINEAR", validTableName(mariadb, "LINEAR"));
        assertEquals("DUAL", validTableName(mariadb, "Dual"));
        assertEquals("ORDER0", validTableName(mariadb, "Order"));
        for (String word : MARIADB_WORDS) {
            assertFalse(word, mariadb.getInvalidColumnWordSet().contains(word));
            assertFalse(word, mariadb.reservedWordSet.contains(word));
        }
        for (String word : TABLE_WORDS) {
            assertTrue(word, mariadb.getInvalidColumnWordSet().contains(word));
            assertFalse(word, mariadb.reservedWordSet.contains(word));
        }
    }

    /**
     * Whether a reserved word is delimited is up to each dictionary rather
     * than to the dictionary that was configured last in the JVM: a MySQL or
     * MariaDB dictionary and a dictionary of another database, configured in
     * either order, name the identifiers as each does alone, and the
     * identifiers created without a dictionary do not delimit reserved words
     * (OPENJPA-3076).
     */
    @Test
    public void testReservedWordsAreDelimitedPerDictionary() {
        for (boolean mysqlFirst : new boolean[] {true, false}) {
            MySQLDictionary mysql = mysqlFirst ? configure(new MySQLDictionary(), null) : null;
            DerbyDictionary derby = configure(new DerbyDictionary(), null);
            if (!mysqlFirst) {
                mysql = configure(new MySQLDictionary(), null);
            }
            assertEquals("\"Window\"", validTableName(mysql, "Window"));
            assertEquals("\"rank\"", validColumnName(mysql, "rank"));
            assertEquals("WINDOW", validTableName(derby, "Window"));
            assertEquals("RANK", validColumnName(derby, "rank"));

            DBIdentifier window = DBIdentifier.newTable("Window");
            assertEquals("Window", window.getName());
            assertSame(window, derby.normalize(window));
            assertEquals("\"Window\"", mysql.normalize(window).getName());
            assertEquals("Window", window.getName());
        }
        for (boolean mariadbFirst : new boolean[] {true, false}) {
            MariaDBDictionary mariadb = mariadbFirst ? configure(new MariaDBDictionary(), null) : null;
            PostgresDictionary postgres = configure(new PostgresDictionary(), null);
            if (!mariadbFirst) {
                mariadb = configure(new MariaDBDictionary(), null);
            }
            assertEquals("\"offset\"", validColumnName(mariadb, "offset"));
            assertEquals("\"Order\"", validTableName(mariadb, "Order"));
            assertEquals("OFFSET0", validColumnName(postgres, "offset"));
            assertEquals("ORDER0", validTableName(postgres, "Order"));
            assertEquals("Order", DBIdentifier.newTable("Order").getName());
        }
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

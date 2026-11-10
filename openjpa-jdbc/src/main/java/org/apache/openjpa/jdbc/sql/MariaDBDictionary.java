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


import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.lib.jdbc.DelegatingConnection;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ExceptionInfo;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.StoreException;

/*
 * Dictionary for MariaDB, based off the MySQLDictionary.
 *
 */
public class MariaDBDictionary extends AbstractMySQLDictionary {
    private static final Localizer _loc = Localizer.forPackage(MariaDBDictionary.class);

    public static final String SELECT_HINT = "openjpa.hint.MariaDBSelectHint";

    /**
     * The URL scheme of a MariaDB Connector/J replication connection.
     */
    private static final String REPLICATION_URL_PREFIX = "jdbc:mariadb:replication:";

    /**
     * The replication compatibility prefix a MariaDB 10 or 11 server prepends
     * to its reported product version for clients that are not MariaDB aware,
     * as in <code>5.5.5-10.3.39-MariaDB</code>.
     */
    private static final String COMPAT_VERSION_PREFIX = "5.5.5-";

    /**
     * The words that MariaDB reserves and that are missing from the generated
     * list of invalid column words (OPENJPA-3074): those that MySQL 8.0 and
     * later reserve as well, and those that only MariaDB reserves, such as
     * <code>OFFSET</code>, <code>RETURNING</code> or <code>PORTION</code>.
     * Taken from <code>INFORMATION_SCHEMA.KEYWORDS</code> of MariaDB 10.11
     * and 12.3, and checked with an unquoted <code>CREATE TABLE</code>.
     * MariaDB 12 no longer reserves <code>ACCESSIBLE</code> and
     * <code>MASTER_SSL_VERIFY_SERVER_CERT</code>, MariaDB 10.11 does not
     * reserve yet <code>CONVERSION</code>, <code>CURRENT_PATH</code>,
     * <code>SQL_AFTER_GTIDS</code>, <code>SQL_BEFORE_GTIDS</code>,
     * <code>TO_DATE</code> and <code>VECTOR</code>. They are reserved words
     * as well.
     */
    private static final List<String> UPDATED_RESERVED_WORDS = List.of(
        "ACCESSIBLE", "CONVERSION", "CURRENT_PATH", "DELETE_DOMAIN_ID", "DO_DOMAIN_IDS", "IGNORE_DOMAIN_IDS",
        "LINEAR", "MASTER_DEMOTE_TO_REPLICA", "MASTER_DEMOTE_TO_SLAVE", "MASTER_SSL_VERIFY_SERVER_CERT", "OFFSET",
        "PAGE_CHECKSUM", "PARSE_VCOL_EXPR", "PORTION", "READ_WRITE", "REF_SYSTEM_ID", "RETURNING", "ROW_NUMBER",
        "SQL_AFTER_GTIDS", "SQL_BEFORE_GTIDS", "STATS_AUTO_RECALC", "STATS_PERSISTENT", "STATS_SAMPLE_PAGES",
        "TO_DATE", "VECTOR");

    /**
     * The invalid column words that MariaDB reserves and that are missing
     * from the reserved words, so that a table, rather than a column, named
     * after one of them failed to be created (OPENJPA-3074). Taken from
     * MariaDB 10.11 and 12.3 as above. They are reserved words now, which
     * this dictionary delimits in table and column names.
     */
    private static final List<String> UPDATED_RESERVED_TABLE_WORDS = List.of(
        "ANALYZE", "ASENSITIVE", "BEFORE", "BIGINT", "CALL", "CONDITION", "CURRENT_ROLE", "DATABASES", "DAY_HOUR",
        "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DELAYED", "DETERMINISTIC", "DISTINCTROW", "DIV", "DUAL",
        "EACH", "ELSEIF", "ENCLOSED", "ESCAPED", "EXIT", "EXPLAIN", "FLOAT8", "FORCE", "FULLTEXT", "HIGH_PRIORITY",
        "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE", "INOUT", "INT3", "INT8", "ITERATE",
        "KEYS", "KILL", "LEAVE", "LIMIT", "LINES", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB",
        "LONGTEXT", "LOOP", "LOW_PRIORITY", "MAXVALUE", "MEDIUMBLOB", "MEDIUMTEXT", "MIDDLEINT",
        "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", "NO_WRITE_TO_BINLOG", "OPTIMIZE", "OPTIONALLY",
        "OUT", "OVER", "PARTITION", "PURGE", "RANGE", "READS", "RECURSIVE", "REGEXP", "RELEASE", "RENAME", "REPEAT",
        "REQUIRE", "RESIGNAL", "RETURN", "RLIKE", "SCHEMAS", "SECOND_MICROSECOND", "SENSITIVE", "SEPARATOR", "SHOW",
        "SIGNAL", "SPATIAL", "SPECIFIC", "SQLEXCEPTION", "SQLWARNING", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS",
        "SQL_SMALL_RESULT", "SSL", "STRAIGHT_JOIN", "TERMINATED", "TINYBLOB", "TINYINT", "TINYTEXT", "TRIGGER",
        "UNDO", "UNLOCK", "USE", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VARBINARY", "VARCHARACTER", "WHILE",
        "XOR", "YEAR_MONTH");

    /**
     * The names of built-in functions that MariaDB reserves in a table name
     * under the <code>IGNORE_SPACE</code> SQL mode, which MariaDB Connector/J
     * adds to the session's <code>sql_mode</code>: a table name followed by a
     * blank and a parenthesis, as in <code>CREATE TABLE Ntile (</code> or
     * <code>INSERT INTO Ntile (</code>, is taken for a function call
     * (OPENJPA-3074). Taken from <code>INFORMATION_SCHEMA.SQL_FUNCTIONS</code>
     * and <code>INFORMATION_SCHEMA.KEYWORDS</code> of MariaDB 10.11 and 12.3,
     * and checked with an unquoted <code>CREATE TABLE</code> under
     * <code>IGNORE_SPACE</code>, leaving out those that are reserved words
     * already. A column of such a name is valid.
     */
    private static final List<String> IGNORE_SPACE_FUNCTION_WORDS = List.of(
        "BIT_AND", "BIT_OR", "BIT_XOR", "CUME_DIST", "CURDATE", "CURTIME", "DATE_ADD", "DATE_SUB", "DENSE_RANK",
        "FIRST_VALUE", "GROUP_CONCAT", "JSON_ARRAYAGG", "JSON_OBJECTAGG", "LAG", "LEAD", "MEDIAN", "MID", "NOW",
        "NTH_VALUE", "NTILE", "PERCENTILE_CONT", "PERCENTILE_DISC", "PERCENT_RANK", "RANK", "STD", "STDDEV",
        "STDDEV_POP", "STDDEV_SAMP", "ST_COLLECT", "SUBSTR", "VARIANCE", "VAR_POP", "VAR_SAMP");

    /**
     * The largest scale of a decimal column on MariaDB.
     */
    private static final int MAX_DECIMAL_SCALE = 38;

    public MariaDBDictionary() {
        platform = "MariaDB";

        // reservedWordSet subset that CANNOT be used as valid column names
        // (i.e., without surrounding them with double-quotes)
        // generated at 2021-05-02T16:53:15.561 via org.apache.openjpa.reservedwords.ReservedWordsIT
        invalidColumnWordSet.addAll(Arrays.asList(new String[] {
            "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT", "BINARY",
            "BLOB", "BOTH", "BY", "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION",
            "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "CURRENT_USER", "CURSOR", "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC",
            "DECIMAL", "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW",
            "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE", "ELSEIF", "ENCLOSED", "END-EXEC", "ESCAPED", "EXCEPT", "EXISTS",
            "EXIT", "EXPLAIN", "FALSE", "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT",
            "GRANT", "GROUP", "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE",
            "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8",
            "INTEGER", "INTERSECT", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "KEY", "KEYS", "KILL", "LEADING", "LEAVE",
            "LEFT", "LIKE", "LIMIT", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT",
            "LOOP", "LOW_PRIORITY", "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND",
            "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL", "NO_WRITE_TO_BINLOG", "NOT", "NULL", "NUMERIC", "ON", "OPTIMIZE",
            "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "OVER", "PARTITION", "PRECISION", "PRIMARY", "PROCEDURE",
            "PURGE", "RANGE", "READ", "READS", "REAL", "RECURSIVE", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT",
            "REPLACE", "REQUIRE", "RESIGNAL", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE", "ROWS", "SCHEMA", "SCHEMAS",
            "SECOND_MICROSECOND", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SIGNAL", "SMALLINT", "SPATIAL", "SPECIFIC", "SQL",
            "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SSL", "STARTING",
            "STRAIGHT_JOIN", "TABLE", "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER",
            "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME",
            "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING", "WHEN", "WHERE", "WHILE", "WITH",
            "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL",
            // end generated.
            // the following keywords used to be defined as reserved words in the past, but now seem to work
            // we still add them for compat reasons
            "OPTION", "SELECT",
        }));
        addUpdatedReservedWords(UPDATED_RESERVED_WORDS);
        addUpdatedReservedTableWords(UPDATED_RESERVED_TABLE_WORDS);
        addUpdatedReservedTableWords(IGNORE_SPACE_FUNCTION_WORDS);
    }

    /**
     * MariaDB delimits a name that is a reserved word, as MySQL does, unless
     * the compatibility option
     * <code>UpdatedMySQLAndMariaDBReservedWords=false</code> asks for the
     * earlier names, which append a <code>0</code> to such a name instead.
     */
    @Override
    protected boolean delimitsReservedWords() {
        return usesUpdatedReservedWords();
    }

    @Override
    protected String getSelectHint() {
        return SELECT_HINT;
    }

    /**
     * MariaDB allows a scale of 38; an unsized decimal column still gets a
     * scale of 30, the largest scale MySQL allows, which keeps the DDL the
     * same on both.
     */
    @Override
    protected int getMaxDecimalScale() {
        return MAX_DECIMAL_SCALE;
    }

    @Override
    public void connectedConfiguration(Connection conn) throws SQLException {
        super.connectedConfiguration(conn);

        DatabaseMetaData metaData = conn.getMetaData();
        int maj = 0;
        int min = 0;
        try {
            // The product version looks like 4.1.3-nt or 5.1.30
            String productVersion = metaData.getDatabaseProductVersion();
            int[] versions = getMajorMinorVersions(productVersion);
            maj = versions[0];
            min = versions[1];
        } catch (IllegalArgumentException e) {
            // we don't understand the version format.
            // That is ok. We just take the default values.
            if (log.isWarnEnabled()) {
                log.warn(e.toString(), e);
            }
        }

        supportsXMLColumn = true;

        if (maj > 10 || (maj == 10 && min > 1)) {
            // MariaDB supports fraction of a second
            useFractionalSecondTemporalTypes();
        }

        // Before 11.1 MariaDB rejects a multi-table DELETE - the form that
        // requiresTargetForDelete produces - whose WHERE clause has a subquery
        // that reads the target table (error 1093), so match the keys of the
        // affected rows in a derived table as on MySQL (OPENJPA-3036).
        if (maj < 11 || (maj == 11 && min < 1)) {
            allowsBulkTargetInSubselect = false;
        }

        // MariaDB 10.3 introduced sequence objects: CREATE SEQUENCE with
        // NEXTVAL()/NEXT VALUE FOR. Older servers have none, and a named
        // @SequenceGenerator falls back to a table sequence there
        // (OPENJPA-3014). There is no setter for NextSequenceQuery and hence
        // no flag saying the user configured one, but this dictionary leaves
        // it null in its constructor and DBDictionary.endConfiguration() nulls
        // an empty value out, so a value seen here was configured explicitly
        // and is left alone.
        if (nextSequenceQuery == null && (maj > 10 || (maj == 10 && min >= 3))) {
            nextSequenceQuery = "SELECT NEXTVAL({0})";
        }
    }

    /**
     * MariaDB caches 1000 increments of a sequence per server by default, and
     * discards what is left of that range whenever the cache is flushed - an
     * <code>ALTER SEQUENCE</code>, a restart - so the identifiers jump by up
     * to <code>1000 * increment</code>. OpenJPA caches values itself, in
     * blocks of the generator's allocation size, and
     * {@link org.apache.openjpa.jdbc.kernel.NativeJDBCSeq} issues
     * <code>ALTER SEQUENCE ... INCREMENT BY</code> once per sequence instance
     * to do so, which is exactly such a flush: the server side cache adds
     * nothing but those gaps. Create the sequence with <code>NOCACHE</code>.
     *<p>
     * Measured on MariaDB 11.8 with a sequence created
     * <code>START WITH 100 INCREMENT BY 7</code>: <code>NEXTVAL</code>
     * returns 100 and leaves <code>NEXT_NOT_CACHED_VALUE</code> at 7100, and
     * after <code>ALTER SEQUENCE ... INCREMENT BY 7</code> the next
     * <code>NEXTVAL</code> returns 7100; with <code>NOCACHE</code> the same
     * statements return 100 and then 107.
     */
    @Override
    public String[] getCreateSequenceSQL(Sequence seq) {
        String[] sql = super.getCreateSequenceSQL(seq);
        if (sql != null) {
            for (int i = 0; i < sql.length; i++) {
                sql[i] = sql[i] + " NOCACHE";
            }
        }
        return sql;
    }

    /**
     * MariaDB has no <code>INFORMATION_SCHEMA.SEQUENCES</code> before 10.6;
     * a sequence is listed in <code>INFORMATION_SCHEMA.TABLES</code> with a
     * <code>TABLE_TYPE</code> of <code>SEQUENCE</code> in every version that
     * has sequences at all. Schema names are matched case insensitively
     * because {@link DBDictionary#getSequences} upper cases the schema it
     * passes, while MariaDB keeps the case of the schema as created.
     *<p>
     * <code>INFORMATION_SCHEMA</code> spans every database on the server, not
     * just the connected one, and every caller in OpenJPA asks for a null
     * schema - <code>SchemaGenerator.generateSequences()</code> passes
     * {@link DBIdentifier#NULL} deliberately, and an unqualified
     * <code>@SequenceGenerator</code> has none. Unrestricted, the reflection
     * would report sequences of other applications' databases, which makes
     * the schema tool skip a <code>CREATE SEQUENCE</code> it owes and, on the
     * drop side, issue <code>DROP SEQUENCE</code> outside the configured
     * database. The no schema case is therefore scoped to
     * <code>DATABASE()</code>, which takes no bind parameter and so leaves
     * the parameter indices {@link DBDictionary#getSequences} binds intact.
     */
    @Override
    protected String getSequencesSQL(String schemaName, String sequenceName) {
        return getSequencesSQL(DBIdentifier.newSchema(schemaName),
            DBIdentifier.newSequence(sequenceName));
    }

    @Override
    protected String getSequencesSQL(DBIdentifier schemaName, DBIdentifier sequenceName) {
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT TABLE_SCHEMA AS SEQUENCE_SCHEMA, ")
            .append("TABLE_NAME AS SEQUENCE_NAME ")
            .append("FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'SEQUENCE'");
        if (!DBIdentifier.isNull(schemaName)) {
            buf.append(" AND UPPER(TABLE_SCHEMA) = UPPER(?)");
        } else {
            buf.append(" AND TABLE_SCHEMA = DATABASE()");
        }
        if (!DBIdentifier.isNull(sequenceName)) {
            buf.append(" AND UPPER(TABLE_NAME) = UPPER(?)");
        }
        return buf.toString();
    }

    @Override
    protected void setDelimitedCase(DatabaseMetaData metaData) {
        // Determination of case sensitivity is not accurate; MariaDB JIRA CONJ-55
        delimitedCase = SCHEMA_CASE_PRESERVE;
    }

    @Override
    public Connection decorate(Connection conn)  throws SQLException {
        conn = super.decorate(conn);
        return conn;
    }

    /**
     * Whether the given connection was opened by MariaDB Connector/J with a
     * <code>jdbc:mariadb:replication:</code> URL, which routes the statements
     * of a read-only connection to a replica. The driver reports that URL in
     * the connection's metadata without a round trip to the server.
     */
    @Override
    public boolean isReplicationConnection(Connection conn) throws SQLException {
        Connection inner = conn instanceof DelegatingConnection delegating
            ? delegating.getInnermostDelegate() : conn;
        String url;
        try {
            url = inner.getMetaData().getURL();
        } catch (SQLException e) {
            // a driver or pool that cannot tell is not taken as replicated
            if (log.isTraceEnabled()) {
                log.trace(e.toString(), e);
            }
            return false;
        }
        return url != null
            && url.regionMatches(true, 0, REPLICATION_URL_PREFIX, 0, REPLICATION_URL_PREFIX.length());
    }

    /**
     * The major and minor version of the server, as
     * {@link DatabaseMetaData#getDatabaseProductVersion} reports it.
     *<p>
     * A MariaDB 10 or 11 server reports its version to a client that is not
     * MariaDB aware with a replication compatibility prefix -
     * <code>5.5.5-10.3.39-MariaDB</code> rather than
     * <code>10.3.39-MariaDB</code> - and both forms reach this dictionary.
     * Which dictionary is used is decided by the product name:
     * {@link DBDictionaryFactory#newDBDictionary(org.apache.openjpa.jdbc.conf.JDBCConfiguration,
     * javax.sql.DataSource, String)} matches
     * {@link DatabaseMetaData#getDatabaseProductName}, falls back to the URL
     * protocol, and consults
     * {@link DatabaseMetaData#getDatabaseProductVersion} only when the name
     * maps to MySQL - which is what a client that is not MariaDB aware
     * reports, and such a client is also the one that passes the prefixed
     * version through. Reading the leading group of that form would yield 5.5
     * and withhold every version gated feature from a server that has it -
     * sequence support from 10.3 on, and the microsecond precision temporal
     * types from 10.2 on - so the prefix is stripped before parsing.
     */
    static int[] getMajorMinorVersions(String versionStr)
        throws IllegalArgumentException {
        versionStr = versionStr.trim();
        if (versionStr.startsWith(COMPAT_VERSION_PREFIX)) {
            versionStr = versionStr.substring(COMPAT_VERSION_PREFIX.length());
        }
        return parseMajorMinorVersions(versionStr);
    }

    /**
     * Write the reader with {@link ResultSet#updateCharacterStream(int, Reader)}
     * instead of the writer of {@link java.sql.Clob#setCharacterStream(long)}.
     * The Connector/J clob writer starts at a wrong position when the clob was
     * read from a result set and fails with an ArrayIndexOutOfBoundsException.
     */
    @Override
    public void updateClob(Select sel, JDBCStore store, Reader reader)
        throws SQLException {
        SQLBuffer sql = sel.toSelect(true, store.getFetchConfiguration());
        Connection conn = store.getConnection();
        try (PreparedStatement stmnt = sql.prepareStatement(conn, store.getFetchConfiguration(),
            ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            setTimeouts(stmnt, store.getFetchConfiguration(), true);
            try (ResultSet res = stmnt.executeQuery()) {
                if (!res.next()) {
                    throw new InternalException(_loc.get("stream-exception"));
                }
                res.updateCharacterStream(1, reader);
                res.updateRow();
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    @Override
    protected int matchErrorState(Map<Integer,Set<String>> errorStates, SQLException ex) {
        int state = super.matchErrorState(errorStates, ex);

        if (state == ExceptionInfo.GENERAL &&
                ex.getNextException() != null &&
                "JZ0002".equalsIgnoreCase(ex.getNextException().getSQLState())) {
            if (conf != null && conf.getLockTimeout() != -1) {
                state = StoreException.LOCK;
            } else {
                state = StoreException.QUERY;
            }
        }

        return state;
    }

    @Override
    public boolean isFatalException(int subtype, SQLException ex) {
        if ((subtype == StoreException.LOCK  && ex.getErrorCode() == 1205) // ER_LOCK_WAIT_TIMEOUT
          ||(subtype == StoreException.LOCK  && ex.getErrorCode() == 1969) // general ER_STATEMENT_TIMEOUT
          ||(subtype == StoreException.LOCK  && "JZ0002".equalsIgnoreCase(ex.getSQLState()))
          ||(subtype == StoreException.QUERY && ex.getErrorCode() == 1317)) {
            return false;
        }
        if (ex.getErrorCode() == 0 && ex.getSQLState() == null) {
            return false;
        }
        return super.isFatalException(subtype, ex);
    }

    @Override
    public void setTime(PreparedStatement stmnt, int idx, Time val, Calendar cal, Column col) throws SQLException {
        // nail down to Jan 1st 1970, because MariaDB uses getTime LONG  and freaks out.
        final Date date = new Date(val.getTime());
        date.setYear(70);
        date.setMonth(0);
        date.setDate(1);
        val = new Time(date.getTime());

        super.setTime(stmnt, idx, val, cal, col);
    }
}

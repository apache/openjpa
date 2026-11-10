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


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.jdbc.identifier.Normalizer;
import org.apache.openjpa.lib.identifier.IdentifierRule;
import org.apache.openjpa.lib.jdbc.DelegatingConnection;
import org.apache.openjpa.util.ExceptionInfo;
import org.apache.openjpa.util.StoreException;

/**
 * Dictionary for MySQL.
 */
public class MySQLDictionary
    extends AbstractMySQLDictionary {

    /**
     * The replication driver of Connector/J 5, removed in Connector/J 8.
     */
    private static final String REPLICATION_DRIVER = "com.mysql.jdbc.ReplicationDriver";

    /**
     * The interfaces a replication connection implements in Connector/J 8
     * and later, which opens one for a <code>jdbc:mysql:replication:</code>
     * URL, and in Connector/J 5.
     */
    private static final String[] REPLICATION_CONNECTION_TYPES = {
        "com.mysql.cj.jdbc.ha.ReplicationConnection",
        "com.mysql.jdbc.ReplicationConnection",
    };

    /**
     * The replication connection interfaces of the driver, resolved on first
     * use; empty if the driver has none or is not visible.
     */
    private volatile List<Class<?>> _replicationTypes = null;

    public static final String SELECT_HINT = "openjpa.hint.MySQLSelectHint";

    /**
     * The words that MySQL 8.0 and later reserve and that are missing from
     * the generated list of invalid column words: those that came with window
     * functions, common table expressions, <code>JSON_TABLE</code>,
     * <code>LATERAL</code>, <code>QUALIFY</code> and
     * <code>TABLESAMPLE</code>, <code>EXTERNAL</code> and <code>LIBRARY</code>
     * of MySQL 9, and some that MySQL 5.7 reserved already
     * (OPENJPA-3074). Taken from <code>INFORMATION_SCHEMA.KEYWORDS</code> of
     * MySQL 8.4 and 9.7.
     */
    private static final List<String> UPDATED_INVALID_COLUMN_WORDS = List.of(
        "ACCESSIBLE", "CUBE", "CUME_DIST", "DENSE_RANK", "EMPTY", "EXCEPT", "EXTERNAL", "FIRST_VALUE",
        "FUNCTION", "GROUPING", "GROUPS", "INTERSECT", "IO_AFTER_GTIDS", "IO_BEFORE_GTIDS", "JSON_TABLE",
        "LAG", "LAST_VALUE", "LATERAL", "LEAD", "LIBRARY", "LINEAR", "NTH_VALUE", "OF", "OPTIMIZER_COSTS",
        "OVER", "PERCENT_RANK", "QUALIFY", "RANK", "READ_WRITE", "RECURSIVE", "ROW", "ROW_NUMBER", "ROWS",
        "STORED", "SYSTEM", "TABLESAMPLE", "VIRTUAL", "WINDOW");

    /**
     * The largest scale of a decimal column on MySQL.
     */
    private static final int MAX_DECIMAL_SCALE = 30;

    public MySQLDictionary() {
        platform = "MySQL";
        allowsBulkTargetInSubselect = false;
        reservedWordSet.add("LIBRARY");

        // reservedWordSet subset that CANNOT be used as valid column names
        // (i.e., without surrounding them with double-quotes)
        // generated at 2021-05-02T15:40:16.383 via org.apache.openjpa.reservedwords.ReservedWordsIT
        invalidColumnWordSet.addAll(Arrays.asList(new String[] {
            "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT", "BINARY",
            "BLOB", "BOTH", "BY", "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION",
            "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
            "CURSOR", "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL",
            "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV",
            "DOUBLE", "DROP", "DUAL", "EACH", "ELSE", "ELSEIF", "ENCLOSED", "END-EXEC", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN",
            "FALSE", "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT", "GENERATED", "GET",
            "GRANT", "GROUP", "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE",
            "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8",
            "INTEGER", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "KEY", "KEYS", "KILL", "LEADING", "LEAVE", "LEFT", "LIKE",
            "LIMIT", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY",
            "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND",
            "MOD", "MODIFIES", "NATURAL", "NO_WRITE_TO_BINLOG", "NOT", "NULL", "NUMERIC", "ON", "OPTIMIZE", "OPTION", "OPTIONALLY",
            "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "PARTITION", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE", "RANGE", "READ",
            "READS", "REAL", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESIGNAL", "RESTRICT",
            "RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SENSITIVE", "SEPARATOR", "SET",
            "SHOW", "SIGNAL", "SMALLINT", "SPATIAL", "SPECIFIC", "SQL", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT",
            "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SSL", "STARTING", "STRAIGHT_JOIN", "TABLE", "TERMINATED", "THEN", "TINYBLOB",
            "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE",
            "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER",
            "VARYING", "WHEN", "WHERE", "WHILE", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL", "NTILE",
            // end generated.
            // the following keywords used to be defined as reserved words in the past, but now seem to work
            // we still add them for compat reasons
            "INDEX", "SELECT"
        }));
        invalidColumnWordSet.addAll(UPDATED_INVALID_COLUMN_WORDS);

        integerCastTypeName = "SIGNED";
    }

    @Override
    protected Collection<String> getUpdatedInvalidColumnWords() {
        return UPDATED_INVALID_COLUMN_WORDS;
    }

    @Override
    protected String getSelectHint() {
        return SELECT_HINT;
    }

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
        if (isJDBC3) {
            maj = metaData.getDatabaseMajorVersion();
            min = metaData.getDatabaseMinorVersion();
        } else {
            try {
                // The product version looks like 4.1.3-nt or 5.1.30
                String productVersion = metaData.getDatabaseProductVersion();
                int[] versions = parseMajorMinorVersions(productVersion);
                maj = versions[0];
                min = versions[1];
            } catch (IllegalArgumentException e) {
                // we don't understand the version format.
                // That is ok. We just take the default values.
                if (log.isWarnEnabled()) {
                    log.warn(e.toString(), e);
                }
            }
        }
        if (maj < 4 || (maj == 4 && min < 1)) {
            supportsSubselect = false;
            allowsAliasInBulkClause = false;
            supportsForeignKeysComposite = false;
        }
        if (maj > 5 || (maj == 5 && min >= 1)) {
            supportsXMLColumn = true;
        }
        if (maj > 5 || (maj == 5 && min >= 7)) {
            // from this version on MySQL supports fractions of a second
            useFractionalSecondTemporalTypes();
        }

        if (metaData.getDriverMajorVersion() < 5) {
            driverDeserializesBlobs = true;
        }
    }

    /**
     * Whether the given connection is a Connector/J replication connection,
     * which routes the statements of a read-only connection to a replica:
     * one opened with a <code>jdbc:mysql:replication:</code> URL by
     * Connector/J 8 and later, or by the <code>ReplicationDriver</code> of
     * Connector/J 5, also behind the wrapper of a connection pool. The
     * configured driver name is only consulted when the driver's replication
     * connection interfaces cannot be loaded.
     */
    @Override
    public boolean isReplicationConnection(Connection conn) throws SQLException {
        Connection inner = conn instanceof DelegatingConnection delegating
            ? delegating.getInnermostDelegate() : conn;
        List<Class<?>> types = getReplicationConnectionTypes(inner);
        if (types.isEmpty()) {
            return conf != null && REPLICATION_DRIVER.equals(conf.getConnectionDriverName());
        }
        for (Class<?> type : types) {
            if (type.isInstance(inner)) {
                return true;
            }
            try {
                if (inner.isWrapperFor(type)) {
                    return true;
                }
            } catch (SQLException | LinkageError e) {
                // a driver or pool that cannot tell is not taken as replicated
                if (log.isTraceEnabled()) {
                    log.trace(e.toString(), e);
                }
            }
        }
        return false;
    }

    /**
     * The names of the interfaces a replication connection of the driver
     * implements.
     */
    protected String[] getReplicationConnectionTypeNames() {
        return REPLICATION_CONNECTION_TYPES;
    }

    /**
     * The replication connection interfaces of the driver, loaded once through
     * the class loader of the given connection, the context class loader or
     * this class's loader, since the driver need not be on the class path.
     */
    private List<Class<?>> getReplicationConnectionTypes(Connection conn) {
        List<Class<?>> types = _replicationTypes;
        if (types != null) {
            return types;
        }
        ClassLoader[] loaders = {
            conn.getClass().getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            MySQLDictionary.class.getClassLoader(),
        };
        types = new ArrayList<>();
        for (String name : getReplicationConnectionTypeNames()) {
            for (ClassLoader loader : loaders) {
                if (loader != null) {
                    try {
                        types.add(Class.forName(name, false, loader));
                        break;
                    } catch (ClassNotFoundException | LinkageError e) {
                        // try the next loader
                    }
                }
            }
        }
        types = List.copyOf(types);
        _replicationTypes = types;
        return types;
    }

    @Override
    protected void configureNamingRules() {
        super.configureNamingRules();
        IdentifierRule rule = Normalizer.getNamingConfiguration().getDefaultIdentifierRule();
        rule.setDelimitReservedWords(true);
        rule.setReservedWords(reservedWordSet);
    }

    @Override
    public int getBatchFetchSize(int batchFetchSize) {
        return Integer.MIN_VALUE;
    }

    @Override
    protected int matchErrorState(Map<Integer,Set<String>> errorStates, SQLException ex) {
        int state = super.matchErrorState(errorStates, ex);
        // OPENJPA-1616 - Special case for MySQL not returning a SQLState for timeouts
        if (state == ExceptionInfo.GENERAL && ex.getErrorCode() == 0 && ex.getSQLState() == null) {
            // look at the nested MySQL exception for more details
            SQLException sqle = ex.getNextException();
            if (sqle != null
            		&& (sqle.toString().startsWith("com.mysql.jdbc.exceptions.MySQLTimeoutException") ||
            				sqle.toString().startsWith("com.mysql.cj.jdbc.exceptions.MySQLTimeoutException"))) {
                if (conf != null && conf.getLockTimeout() != -1) {
                    state = StoreException.LOCK;
                } else {
                    state = StoreException.QUERY;
                }
            }
        }
        return state;
    }

    @Override
    public boolean isFatalException(int subtype, SQLException ex) {
        if ((subtype == StoreException.LOCK  && ex.getErrorCode() == 1205)
          ||(subtype == StoreException.QUERY && ex.getErrorCode() == 1317)) {
            return false;
        }
        if (ex.getErrorCode() == 0 && ex.getSQLState() == null)
            return false;
        return super.isFatalException(subtype, ex);
    }
}

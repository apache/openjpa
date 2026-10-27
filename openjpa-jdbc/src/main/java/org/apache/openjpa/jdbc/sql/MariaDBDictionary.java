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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.DBIdentifier.DBIdentifierType;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.util.ExceptionInfo;
import org.apache.openjpa.util.StoreException;

/*
 * Dictionary for MariaDB, based off the MySQLDictionary.
 *
 */
public class MariaDBDictionary extends DBDictionary {
    public static final String SELECT_HINT = "openjpa.hint.MariaDBSelectHint";

    public static final String DELIMITER_BACK_TICK = "`";

    /**
     * The replication compatibility prefix a MariaDB 10 or 11 server prepends
     * to its reported product version for clients that are not MariaDB aware,
     * as in <code>5.5.5-10.3.39-MariaDB</code>.
     */
    private static final String COMPAT_VERSION_PREFIX = "5.5.5-";

    /**
     * The MySQL table type to use when creating tables; defaults to innodb.
     */
    public String tableType = "innodb";

    /**
     * Whether to use clobs; defaults to true. Set this to false if you have an
     * old version of MySQL which does not handle clobs properly.
     */
    public boolean useClobs = true;

    /**
     * Whether the driver automatically deserializes blobs.
     */
    public boolean driverDeserializesBlobs = false;

    /**
     * Whether to inline multi-table bulk-delete operations into MySQL's
     * combined <code>DELETE FROM foo, bar, baz</code> syntax.
     * Defaults to false, since this may fail in the presence of InnoDB tables
     * with foreign keys.
     * @link http://dev.mysql.com/doc/refman/5.0/en/delete.html
     */
    public boolean optimizeMultiTableDeletes = false;

    public static final String tinyBlobTypeName = "TINYBLOB";
    public static final String mediumBlobTypeName = "MEDIUMBLOB";
    public static final String longBlobTypeName = "LONGBLOB";
    public static final String tinyTextTypeName = "TINYTEXT";
    public static final String mediumTextTypeName = "MEDIUMTEXT";
    public static final String longTextTypeName = "LONGTEXT";

    public MariaDBDictionary() {
        platform = "MariaDB";
        validationSQL = "SELECT NOW()";
        distinctCountColumnSeparator = ",";

        supportsDeferredConstraints = false;
        constraintNameMode = CONS_NAME_MID;
        supportsMultipleNontransactionalResultSets = false;
        requiresAliasForSubselect = true; // new versions
        requiresTargetForDelete = true;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;

        datePrecision = MICRO;

        concatenateFunction = "CONCAT({0},{1})";

        maxTableNameLength = 64;
        maxColumnNameLength = 64;
        maxIndexNameLength = 64;
        maxConstraintNameLength = 64;
        maxIndexesPerTable = 32;
        schemaCase = SCHEMA_CASE_PRESERVE;

        supportsAutoAssign = true;
        lastGeneratedKeyQuery = "SELECT LAST_INSERT_ID()";
        autoAssignClause = "AUTO_INCREMENT";

        clobTypeName = "TEXT";
        longVarcharTypeName = "TEXT";
        longVarbinaryTypeName = "LONG VARBINARY";
        timestampTypeName = "DATETIME";
        xmlTypeName = "TEXT";
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "BOOL", "LONG VARBINARY", "MEDIUMBLOB", "LONGBLOB",
            "TINYBLOB", "LONG VARCHAR", "MEDIUMTEXT", "LONGTEXT", "TEXT",
            "TINYTEXT", "DOUBLE PRECISION", "ENUM", "SET", "DATETIME", "JSON",
        }));
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "AUTO_INCREMENT", "BINARY", "BLOB", "CHANGE", "ENUM", "INFILE",
            "INT1", "INT2", "INT4", "FLOAT1", "FLOAT2", "FLOAT4", "LOAD",
            "MEDIUMINT", "OUTFILE", "REPLACE", "STARTING", "TEXT", "UNSIGNED",
            "ZEROFILL", "INDEX",
        }));

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

        requiresSearchStringEscapeForLike = true;

        // MariaDB silently ignores ESCAPE '' and keeps its default backslash
        // escape, and rejects ESCAPE '' outright when sql_mode contains
        // NO_BACKSLASH_ESCAPES, so "no escape character" has to be spelled
        // with a character that cannot occur in a pattern (OPENJPA-3009)
        searchStringNoEscape = "CHAR(0)";

        // MariaDB requires double-escape for strings
        searchStringEscape = "\\\\";

        typeModifierSet.addAll(Arrays.asList(new String[] { "UNSIGNED", "ZEROFILL" }));

        setLeadingDelimiter(DELIMITER_BACK_TICK);
        setTrailingDelimiter(DELIMITER_BACK_TICK);

        fixedSizeTypeNameSet.remove("NUMERIC");

        dateFractionDigits = 0;
        supportsUnsizedCharOnCast = false;
        // MariaDB has no BIGINT cast target; SIGNED [INTEGER] yields a 64 bit signed value
        longCastTypeName = "SIGNED";
    }

    /**
     * EXTRACT(WEEK) counts weeks from Sunday; mode 3 of WEEK gives the ISO week
     * that JPA defines.
     */
    @Override
    public String getExtractDateTimeFieldFunction(DateTimeExtractField extField) {
        if (extField == DateTimeExtractField.WEEK) {
            return "WEEK({1}, 3)";
        }
        return super.getExtractDateTimeFieldFunction(extField);
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
            if (log.isWarnEnabled())
                log.warn(e.toString(), e);
        }

        supportsXMLColumn = true;

        if (maj > 10 || (maj == 10 && min > 1)) {
            // MariaDB supports fraction of a second
            timestampTypeName = "DATETIME{0}";
            fixedSizeTypeNameSet.remove(timestampTypeName);
            fractionalTypeNameSet.add(timestampTypeName);

            // Request microsecond precision for temporal columns so @Version
            // Instant/LocalDateTime can detect concurrent updates within a
            // single whole second. Matches PostgreSQL/Derby defaults.
            dateFractionDigits = 6;

            timeTypeName = "TIME{0}";
            fractionalTypeNameSet.add(timeTypeName);
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
        int beginIndex = 0;

        versionStr = versionStr.trim();
        if (versionStr.startsWith(COMPAT_VERSION_PREFIX)) {
            versionStr = versionStr.substring(COMPAT_VERSION_PREFIX.length()).trim();
        }
        char[] charArr = versionStr.toCharArray();
        for (int i = 0; i < charArr.length; i++) {
            if (Character.isDigit(charArr[i])) {
                beginIndex = i;
                break;
            }
        }

        int endIndex = charArr.length;
        for (int i = beginIndex+1; i < charArr.length; i++) {
            if (charArr[i] != '.' && !Character.isDigit(charArr[i])) {
                endIndex = i;
                break;
            }
        }

        String[] arr = versionStr.substring(beginIndex, endIndex).split("\\.");
        if (arr.length < 2)
            throw new IllegalArgumentException();

        int maj = Integer.parseInt(arr[0]);
        int min = Integer.parseInt(arr[1]);
        return new int[]{maj, min};
    }

    @Override
    public String[] getCreateTableSQL(Table table) {
        String[] sql = super.getCreateTableSQL(table);
        if (!StringUtil.isEmpty(tableType))
            sql[0] = sql[0] + " ENGINE = " + tableType;
        return sql;
    }

    @Override
    public String[] getDropIndexSQL(Index index) {
        return new String[]{ "DROP INDEX " + getFullName(index) + " ON "
            + getFullName(index.getTable(), false) };
    }

    /**
     * Return <code>ALTER TABLE &lt;table name&gt; DROP PRIMARY KEY</code>.
     */
    @Override
    public String[] getDropPrimaryKeySQL(PrimaryKey pk) {
        if (DBIdentifier.isNull(pk.getIdentifier()))
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(pk.getTable(), false)
            + " DROP PRIMARY KEY" };
    }

    /**
     * Return <code>ALTER TABLE &lt;table name&gt; DROP FOREIGN KEY
     * &lt;fk name&gt;</code>.
     */
    @Override
    public String[] getDropForeignKeySQL(ForeignKey fk, Connection conn) {
        if (DBIdentifier.isNull(fk.getIdentifier())) {
            DBIdentifier fkName = fk.loadIdentifierFromDB(this,conn);
            String[] retVal = (fkName == null) ?  new String[0] :
                new String[]{ "ALTER TABLE "
                + getFullName(fk.getTable(), false)
                + " DROP FOREIGN KEY " + toDBName(fkName) };
            return retVal;
        }
        return new String[]{ "ALTER TABLE "
            + getFullName(fk.getTable(), false)
            + " DROP FOREIGN KEY " + toDBName(fk.getIdentifier()) };
    }

    @Override
    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        String[] sql = super.getAddPrimaryKeySQL(pk);

        // mysql requires that a column be declared NOT NULL before
        // it can be made a primary key.
        Column[] cols = pk.getColumns();
        String[] ret = new String[cols.length + sql.length];
        for (int i = 0; i < cols.length; i++) {
            ret[i] = "ALTER TABLE " + getFullName(cols[i].getTable(), false)
                + " CHANGE " + toDBName(cols[i].getIdentifier())
                + " " + toDBName(cols[i].getIdentifier()) // name twice
                + " " + getTypeName(cols[i]) + " NOT NULL";
        }

        System.arraycopy(sql, 0, ret, cols.length, sql.length);
        return ret;
    }

    @Override
    public String[] getDeleteTableContentsSQL(Table[] tables,Connection conn) {
        // mysql >= 4 supports more-optimal delete syntax
        if (!optimizeMultiTableDeletes)
            return super.getDeleteTableContentsSQL(tables,conn);
        else {
            StringBuilder buf = new StringBuilder(tables.length * 8);
            buf.append("DELETE FROM ");
            for (int i = 0; i < tables.length; i++) {
                buf.append(toDBName(tables[i].getFullIdentifier()));
                if (i < tables.length - 1)
                    buf.append(", ");
            }
            return new String[] { buf.toString() };
        }
    }

    @Override
    protected void appendSelectRange(SQLBuffer buf, long start, long end,
        boolean subselect) {
        buf.append(" LIMIT ").appendValue(start).append(", ");
        if (end == Long.MAX_VALUE)
            buf.appendValue(Long.MAX_VALUE);
        else
            buf.appendValue(end - start);
    }

    @Override
    protected Column newColumn(ResultSet colMeta)
        throws SQLException {
        Column col = super.newColumn(colMeta);
        if (col.isNotNull() && "0".equals(col.getDefaultString()))
            col.setDefaultString(null);
        return col;
    }

    @Override
    public Object getBlobObject(ResultSet rs, int column, JDBCStore store)
        throws SQLException {
        // if the user has set a get-blob strategy explicitly or the driver
        // does not automatically deserialize, delegate to super
        if (useGetBytesForBlobs || useGetObjectForBlobs || !driverDeserializesBlobs)
            return super.getBlobObject(rs, column, store);

        // most mysql drivers deserialize on getObject
        return rs.getObject(column);
    }

    @Override
    public int getPreferredType(int type) {
        if (type == Types.CLOB && !useClobs) {
            return Types.LONGVARCHAR;
        }
        else if (type == Types.TIME_WITH_TIMEZONE) {
            // MariaDB doesn't support SQL-2003 'WITH TIMEZONE' nor the respective JDBC types.
            return Types.TIME;
        }
        else if (type == Types.TIMESTAMP_WITH_TIMEZONE) {
            // MariaDB doesn't support SQL-2003 'WITH TIMEZONE' nor the respective JDBC types.
            return Types.TIMESTAMP;
        }

        return super.getPreferredType(type);
    }

    /**
     * Append XML comparison.
     *
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison
     * @param rhs the right hand side of the comparison
     * @param lhsxml indicates whether the left operand maps to XML
     * @param rhsxml indicates whether the right operand maps to XML
     */
    @Override
    public void appendXmlComparison(SQLBuffer buf, String op, FilterValue lhs,
        FilterValue rhs, boolean lhsxml, boolean rhsxml) {
        super.appendXmlComparison(buf, op, lhs, rhs, lhsxml, rhsxml);
        if (lhsxml)
            appendXmlValue(buf, lhs);
        else
            lhs.appendTo(buf);
        buf.append(" ").append(op).append(" ");
        if (rhsxml)
            appendXmlValue(buf, rhs);
        else
            rhs.appendTo(buf);
    }

    /**
     * Append XML column value so that it can be used in comparisons.
     *
     * @param buf the SQL buffer to write the value
     * @param val the value to be written
     */
    private void appendXmlValue(SQLBuffer buf, FilterValue val) {
        buf.append("ExtractValue(").
            append(val.getColumnAlias(val.getFieldMapping().getColumns()[0])).
            append(",'/*/");
        val.appendTo(buf);
        buf.append("')");
    }

    /**
     * Check to see if we have set the {@link #SELECT_HINT} in the
     * fetch configuration, and if so, append the MySQL hint after the
     * "SELECT" part of the query.
     */
    @Override
    public String getSelectOperation(JDBCFetchConfiguration fetch) {
        Object hint = fetch == null ? null : fetch.getHint(SELECT_HINT);
        String select = "SELECT";
        if (hint != null)
            select += " " + hint;
        return select;
    }

    @Override
    protected Collection<String> getSelectTableAliases(Select sel) {
        Set<String> result = new HashSet<>();
        List<String> selects = sel.getIdentifierAliases();
        for (String s : selects) {
            String tableAlias = s.substring(0, s.indexOf('.'));
            result.add(tableAlias);
        }
        return result;
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
        if (ex.getErrorCode() == 0 && ex.getSQLState() == null)
            return false;
        return super.isFatalException(subtype, ex);
    }

    /**
     * OPENJPA-740 Special case for MySql special column types,
     * like LONGTEXT, LONGBLOG etc..
     * @see org.apache.openjpa.jdbc.sql.DBDictionary#getTypeName(org.apache.openjpa.jdbc.schema.Column)
     */
    @Override
    public String getTypeName(Column col) {
        // handle blobs differently, if the DBItentifierType is NULL (e.g. no column definition is set).
        if (col.getType() == Types.BLOB && col.getTypeIdentifier().getType() == DBIdentifierType.NULL) {
            if (col.getSize() <= 0)   // unknown size
                return blobTypeName;  // return old default of 64KB
            else if (col.getSize() <= 255)
                return tinyBlobTypeName;
            else if (col.getSize() <= 65535)
                return blobTypeName;  // old default of 64KB
            else if (col.getSize() <= 16777215)
                return mediumBlobTypeName;
            else
                return longBlobTypeName;
        } else if (col.getType() == Types.CLOB && col.getTypeIdentifier().getType() == DBIdentifierType.NULL) {
            if (col.getSize() <= 0)   // unknown size
                return clobTypeName;  // return old default of 64KB
            else if (col.getSize() <= 255)
                return tinyTextTypeName;
            else if (col.getSize() <= 65535)
                return clobTypeName;  // old default of 64KB
            else if (col.getSize() <= 16777215)
                return mediumTextTypeName;
            else
                return longTextTypeName;
        } else {
            return super.getTypeName(col);
        }
    }

    @Override
    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("LOCATE(");
        find.appendTo(buf);
        buf.append(", ");
        str.appendTo(buf);
        if (start != null) {
            buf.append(", ");
            start.appendTo(buf);
        }
        buf.append(")");
    }

    /**
     * MariaDB / MySQL do not support ANSI SQL {@code NULLS FIRST} / {@code NULLS LAST}.
     * Emulate via an auxiliary {@code &lt;expr&gt; IS NULL} sort key.
     * <p>
     * MariaDB's default NULL ordering places NULLs before non-NULLs when
     * sorting ASC and after non-NULLs when sorting DESC. When the requested
     * precedence already matches that default, nothing extra is emitted.
     * Otherwise the last order term {@code &lt;expr&gt; ASC|DESC} is rewritten
     * to {@code &lt;expr&gt; IS NULL &lt;sort&gt;, &lt;expr&gt; ASC|DESC}.
     */
    @Override
    public void appendNullsPrecedence(SQLBuffer ordering, int nullPrecedence) {
        final String nullSort = (nullPrecedence == QueryExpressions.NULLS_FIRST) ? "DESC" : "ASC";
        emulateNullsPrecedence(ordering, nullPrecedence,
                expr -> expr + " IS NULL " + nullSort + ", " + expr);
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

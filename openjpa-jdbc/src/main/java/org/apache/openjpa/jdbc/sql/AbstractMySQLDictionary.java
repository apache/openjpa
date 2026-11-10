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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.DBIdentifier.DBIdentifierType;
import org.apache.openjpa.jdbc.identifier.Normalizer;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.identifier.IdentifierRule;
import org.apache.openjpa.lib.util.StringUtil;

/**
 * Base dictionary for the MySQL family of databases (MySQL and MariaDB),
 * which share the SQL dialect, the DDL types and the behaviour of their
 * JDBC drivers that this dictionary deals with. The differences, such as
 * the reserved words, the version dependent features and the error codes,
 * are left to the subclasses.
 */
public abstract class AbstractMySQLDictionary
    extends DBDictionary {

    public static final String DELIMITER_BACK_TICK = "`";

    /**
     * The table type to use when creating tables; defaults to innodb.
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

    /**
     * The largest precision of a decimal column.
     */
    private static final int MAX_DECIMAL_PRECISION = 65;

    /**
     * The scale of an unsized decimal column, the largest scale MySQL allows,
     * which keeps the DDL the same on MySQL and MariaDB.
     */
    private static final int UNSIZED_DECIMAL_SCALE = 30;

    /**
     * The words that {@link #addUpdatedReservedWords(Collection)} added to
     * the invalid column words and to the reserved words.
     */
    private final Set<String> _updatedInvalidColumnWords = new HashSet<>();
    private final Set<String> _updatedReservedWords = new HashSet<>();

    /**
     * Whether the compatibility options leave the updated reserved words in
     * place.
     */
    private boolean _usesUpdatedReservedWords = true;

    public AbstractMySQLDictionary() {
        validationSQL = "SELECT NOW()";
        distinctCountColumnSeparator = ",";

        supportsDeferredConstraints = false;
        // CONSTRAINT <name> FOREIGN KEY / UNIQUE (...), the form of the
        // standard. Since MySQL 8.0.16 the name in FOREIGN KEY <name> (...)
        // only names the backing index and the constraint gets a generated
        // <table>_ibfk_<n> name, so a later DROP FOREIGN KEY <name> fails.
        // Note that FK constraint names must be unique per database, not per table.
        constraintNameMode = CONS_NAME_BEFORE;
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

        requiresSearchStringEscapeForLike = true;

        // MySQL rejects ESCAPE '' when sql_mode contains NO_BACKSLASH_ESCAPES,
        // and MariaDB silently ignores it and keeps its default backslash
        // escape, so "no escape character" is spelled with a character that
        // cannot occur in a pattern (OPENJPA-3009)
        searchStringNoEscape = "CHAR(0)";

        // MySQL and MariaDB require double-escape for strings
        searchStringEscape = "\\\\";

        // a backslash in a string literal is an escape character unless
        // sql_mode contains NO_BACKSLASH_ESCAPES. The sql_mode is not checked,
        // because a session can change it at any time; a value with a
        // backslash is bound as a parameter instead, which is correct either way
        backslashEscapesInStringLiterals = true;

        typeModifierSet.addAll(Arrays.asList(new String[] { "UNSIGNED", "ZEROFILL" }));

        setLeadingDelimiter(DELIMITER_BACK_TICK);
        setTrailingDelimiter(DELIMITER_BACK_TICK);

        // the precision and scale of a decimal column are part of its DDL type,
        // an unsized DECIMAL is DECIMAL(10,0) (OPENJPA-3062)
        fixedSizeTypeNameSet.remove("NUMERIC");
        fixedSizeTypeNameSet.remove("DECIMAL");

        dateFractionDigits = 0;
        supportsUnsizedCharOnCast = false;
        // there is no BIGINT cast target; SIGNED [INTEGER] yields a 64 bit signed value
        longCastTypeName = "SIGNED";
        // MySQL and MariaDB support the CHAR data type (OPENJPA-3001)
        storeCharsAsNumbers = false;
    }

    /**
     * Adds words that the database reserves to the
     * {@link #invalidColumnWordSet invalid column words} and to the
     * {@link #reservedWordSet reserved words}, as far as they are missing
     * there (OPENJPA-3074). The compatibility option
     * <code>UpdatedMySQLAndMariaDBReservedWords=false</code> removes the words
     * that were added here again, so that the names keep the form an earlier
     * release generated.
     *
     * @see org.apache.openjpa.conf.Compatibility#getUpdatedMySQLAndMariaDBReservedWords()
     */
    protected void addUpdatedReservedWords(Collection<String> words) {
        for (String word : words) {
            if (invalidColumnWordSet.add(word)) {
                _updatedInvalidColumnWords.add(word);
            }
            if (reservedWordSet.add(word)) {
                _updatedReservedWords.add(word);
            }
        }
    }

    /**
     * Adds words that the database reserves in a table name, and that are
     * either {@link #invalidColumnWordSet invalid column words} already or
     * valid column names, to the {@link #reservedWordSet reserved words}
     * only, as far as they are missing there (OPENJPA-3074). The
     * compatibility option
     * <code>UpdatedMySQLAndMariaDBReservedWords=false</code> removes them
     * again, as the {@link #addUpdatedReservedWords(Collection) updated
     * reserved words}.
     */
    protected void addUpdatedReservedTableWords(Collection<String> words) {
        for (String word : words) {
            if (reservedWordSet.add(word)) {
                _updatedReservedWords.add(word);
            }
        }
    }

    /**
     * Whether the {@link #addUpdatedReservedWords(Collection) updated
     * reserved words} and
     * {@link #addUpdatedReservedTableWords(Collection) reserved table words}
     * are in use, which the compatibility option
     * <code>UpdatedMySQLAndMariaDBReservedWords=false</code> turns off. Known
     * once the configuration is set.
     */
    protected boolean usesUpdatedReservedWords() {
        return _usesUpdatedReservedWords;
    }

    /**
     * Whether a table, column or other name that is a
     * {@link #reservedWordSet reserved word} is delimited in the SQL, so that
     * it keeps its name, rather than renamed, e.g. with a <code>0</code>
     * appended.
     */
    protected abstract boolean delimitsReservedWords();

    /**
     * Removes the {@link #addUpdatedReservedWords(Collection) updated
     * reserved words} if the compatibility options ask for the earlier names.
     * This has to happen before the naming rules are configured, which take a
     * copy of the words.
     */
    @Override
    public void setConfiguration(Configuration conf) {
        if (conf instanceof OpenJPAConfiguration openjpaConf
            && !openjpaConf.getCompatibilityInstance().getUpdatedMySQLAndMariaDBReservedWords()) {
            _usesUpdatedReservedWords = false;
            invalidColumnWordSet.removeAll(_updatedInvalidColumnWords);
            reservedWordSet.removeAll(_updatedReservedWords);
        }
        super.setConfiguration(conf);
    }

    /**
     * Makes the default identifier rule delimit the reserved words if
     * {@link #delimitsReservedWords()}. An identifier is delimited when it is
     * created, by {@link Normalizer}, whose rule is shared by all dictionaries
     * of the JVM, so the dictionary that was configured last decides.
     */
    @Override
    protected void configureNamingRules() {
        super.configureNamingRules();
        if (delimitsReservedWords()) {
            IdentifierRule rule = Normalizer.getNamingConfiguration().getDefaultIdentifierRule();
            rule.setDelimitReservedWords(true);
            rule.setReservedWords(reservedWordSet);
        }
    }

    /**
     * The name of the fetch configuration hint whose value is appended after
     * the <code>SELECT</code> keyword of a query.
     *
     * @see #getSelectOperation(JDBCFetchConfiguration)
     */
    protected abstract String getSelectHint();

    /**
     * The largest scale of a decimal column the database allows.
     */
    protected abstract int getMaxDecimalScale();

    /**
     * The major and minor version in a product version such as
     * <code>4.1.3-nt</code> or <code>5.1.30</code>: the first two numbers of
     * the first group of digits and dots.
     *
     * @throws IllegalArgumentException if the version has no such numbers
     */
    static int[] parseMajorMinorVersions(String versionStr)
        throws IllegalArgumentException {
        int beginIndex = 0;

        versionStr = versionStr.trim();
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
        if (arr.length < 2) {
            throw new IllegalArgumentException();
        }

        int maj = Integer.parseInt(arr[0]);
        int min = Integer.parseInt(arr[1]);
        return new int[]{maj, min};
    }

    /**
     * Use the temporal types with a fraction of a second, which MySQL has
     * from 5.7 and MariaDB from 10.2 on.
     */
    protected void useFractionalSecondTemporalTypes() {
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

    /**
     * EXTRACT(WEEK) counts weeks from Sunday; mode 3 of WEEK gives the ISO week
     * that JPA defines. EXTRACT(SECOND) returns the whole seconds only; the
     * fraction of the second that JPA includes is added from
     * EXTRACT(MICROSECOND).
     */
    @Override
    public String getExtractDateTimeFieldFunction(DateTimeExtractField extField) {
        if (extField == DateTimeExtractField.WEEK) {
            return "WEEK({1}, 3)";
        }
        if (extField == DateTimeExtractField.SECOND) {
            return "(" + super.getExtractDateTimeFieldFunction(extField) + " + "
                + extractDateTimeFieldFunction.replace("{0}", "MICROSECOND") + " / 1000000.0)";
        }
        return super.getExtractDateTimeFieldFunction(extField);
    }

    /**
     * {@link java.sql.Time} has no fraction of a second, so a {@link LocalTime}
     * is passed to and read from the driver as it is, keeping the fraction that
     * a <code>TIME(6)</code> column or <code>EXTRACT(SECOND)</code> holds.
     */
    @Override
    public void setLocalTime(PreparedStatement stmnt, int idx, LocalTime val, Column col) throws SQLException {
        stmnt.setObject(idx, val);
    }

    @Override
    public LocalTime getLocalTime(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, LocalTime.class);
    }

    @Override
    public String[] getCreateTableSQL(Table table) {
        String[] sql = super.getCreateTableSQL(table);
        if (!StringUtil.isEmpty(tableType)) {
            sql[0] = sql[0] + " ENGINE = " + tableType;
        }
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
        if (DBIdentifier.isNull(pk.getIdentifier())) {
            return new String[0];
        }
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
        if (!optimizeMultiTableDeletes) {
            return super.getDeleteTableContentsSQL(tables,conn);
        }
        StringBuilder buf = new StringBuilder(tables.length * 8);
        buf.append("DELETE FROM ");
        for (int i = 0; i < tables.length; i++) {
            buf.append(toDBName(tables[i].getFullIdentifier()));
            if (i < tables.length - 1) {
                buf.append(", ");
            }
        }
        return new String[] { buf.toString() };
    }

    @Override
    protected void appendSelectRange(SQLBuffer buf, long start, long end,
        boolean subselect) {
        buf.append(" LIMIT ").appendValue(start).append(", ");
        if (end == Long.MAX_VALUE) {
            buf.appendValue(Long.MAX_VALUE);
        } else {
            buf.appendValue(end - start);
        }
    }

    @Override
    protected Column newColumn(ResultSet colMeta)
        throws SQLException {
        Column col = super.newColumn(colMeta);
        if (col.isNotNull() && "0".equals(col.getDefaultString())) {
            col.setDefaultString(null);
        }
        return col;
    }

    @Override
    public Object getBlobObject(ResultSet rs, int column, JDBCStore store)
        throws SQLException {
        // if the user has set a get-blob strategy explicitly or the driver
        // does not automatically deserialize, delegate to super
        if (useGetBytesForBlobs || useGetObjectForBlobs || !driverDeserializesBlobs) {
            return super.getBlobObject(rs, column, store);
        }

        // most mysql drivers deserialize on getObject
        return rs.getObject(column);
    }

    @Override
    public int getPreferredType(int type) {
        if (type == Types.CLOB && !useClobs) {
            return Types.LONGVARCHAR;
        } else if (type == Types.TIME_WITH_TIMEZONE) {
            // MySQL and MariaDB support neither SQL-2003 'WITH TIMEZONE' nor the respective JDBC types.
            return Types.TIME;
        } else if (type == Types.TIMESTAMP_WITH_TIMEZONE) {
            // MySQL and MariaDB support neither SQL-2003 'WITH TIMEZONE' nor the respective JDBC types.
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
        if (lhsxml) {
            appendXmlValue(buf, lhs);
        } else {
            lhs.appendTo(buf);
        }
        buf.append(" ").append(op).append(" ");
        if (rhsxml) {
            appendXmlValue(buf, rhs);
        } else {
            rhs.appendTo(buf);
        }
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
     * Check to see if we have set the {@link #getSelectHint() select hint} in
     * the fetch configuration, and if so, append the hint after the "SELECT"
     * part of the query.
     */
    @Override
    public String getSelectOperation(JDBCFetchConfiguration fetch) {
        Object hint = fetch == null ? null : fetch.getHint(getSelectHint());
        String select = "SELECT";
        if (hint != null) {
            select += " " + hint;
        }
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

    /**
     * OPENJPA-740 Special case for MySql special column types,
     * like LONGTEXT, LONGBLOG etc..
     * @see org.apache.openjpa.jdbc.sql.DBDictionary#getTypeName(org.apache.openjpa.jdbc.schema.Column)
     */
    @Override
    public String getTypeName(Column col) {
        // handle blobs differently, if the DBItentifierType is NULL (e.g. no column definition is set).
        if (col.getType() == Types.BLOB && col.getTypeIdentifier().getType() == DBIdentifierType.NULL) {
            if (col.getSize() <= 0) { // unknown size
                return blobTypeName;  // return old default of 64KB
            } else if (col.getSize() <= 255) {
                return tinyBlobTypeName;
            } else if (col.getSize() <= 65535) {
                return blobTypeName;  // old default of 64KB
            } else if (col.getSize() <= 16777215) {
                return mediumBlobTypeName;
            } else {
                return longBlobTypeName;
            }
        } else if (col.getType() == Types.CLOB && col.getTypeIdentifier().getType() == DBIdentifierType.NULL) {
            if (col.getSize() <= 0) { // unknown size
                return clobTypeName;  // return old default of 64KB
            } else if (col.getSize() <= 255) {
                return tinyTextTypeName;
            } else if (col.getSize() <= 65535) {
                return clobTypeName;  // old default of 64KB
            } else if (col.getSize() <= 16777215) {
                return mediumTextTypeName;
            } else {
                return longTextTypeName;
            }
        } else if (isUnsizedDecimal(col)) {
            return getUnsizedDecimalTypeName(col);
        } else {
            return super.getTypeName(col);
        }
    }

    /**
     * MySQL and MariaDB read an unsized <code>NUMERIC</code> or
     * <code>DECIMAL</code> as <code>DECIMAL(10,0)</code>, which rounds every
     * value to an integer. A decimal column of a non integral Java type
     * without a precision is created with the largest precision and the given
     * scale, or a scale of 30 if none is given, so that the fraction of the
     * value is kept. A scale above {@link #getMaxDecimalScale()}, which the
     * database rejects, is lowered to it. A <code>NumericTypeName</code> or
     * <code>DecimalTypeName</code> set by the user is kept.
     */
    private String getUnsizedDecimalTypeName(Column col) {
        String typeName = getTypeName(col.getType());
        if (!"NUMERIC".equalsIgnoreCase(typeName) && !"DECIMAL".equalsIgnoreCase(typeName)) {
            return super.getTypeName(col);
        }
        int scale = col.getDecimalDigits() > 0
            ? Math.min(col.getDecimalDigits(), getMaxDecimalScale()) : UNSIZED_DECIMAL_SCALE;
        return typeName + "(" + MAX_DECIMAL_PRECISION + ", " + scale + ")";
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
     * MySQL / MariaDB do not support ANSI SQL {@code NULLS FIRST} / {@code NULLS LAST}.
     * Emulate via an auxiliary {@code &lt;expr&gt; IS NULL} sort key.
     * <p>
     * The default NULL ordering places NULLs before non-NULLs when sorting
     * ASC and after non-NULLs when sorting DESC. When the requested precedence
     * already matches that default, nothing extra is emitted. Otherwise the
     * last order term {@code &lt;expr&gt; ASC|DESC} is rewritten to
     * {@code &lt;expr&gt; IS NULL &lt;sort&gt;, &lt;expr&gt; ASC|DESC}.
     */
    @Override
    public void appendNullsPrecedence(SQLBuffer ordering, int nullPrecedence) {
        final String nullSort = (nullPrecedence == QueryExpressions.NULLS_FIRST) ? "DESC" : "ASC";
        emulateNullsPrecedence(ordering, nullPrecedence,
                expr -> expr + " IS NULL " + nullSort + ", " + expr);
    }

    /**
     * MySQL Connector/J and MariaDB Connector/J translate a JDBC escaped
     * date-time literal into a string literal, so that COALESCE or CASE with
     * a date-time column has a string result. A SQL standard typed literal
     * keeps the date-time type.
     */
    @Override
    public String toJDBCEscapedDateTimeLiteral(String escape, int parseType) {
        return toTypedDateTimeLiteral(escape, parseType);
    }
}

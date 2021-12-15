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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.StoreException;

/**
 * Dictionary for H2 ({@link http://www.h2database.com}).
 *
 * @since 0.9.7
 */
public class H2Dictionary extends DBDictionary {
    private final static List<String> V2_KEYWORDS = Arrays.asList(
            "ALL",
            "AND",
            "ANY",
            "ARRAY",
            "AS",
            "ASYMMETRIC",
            "AUTHORIZATION",
            "BETWEEN",
            "BOTH",
            "CASE",
            "CAST",
            "CHECK",
            "CONSTRAINT",
            "CROSS",
            "CURRENT_CATALOG",
            "CURRENT_DATE",
            "CURRENT_PATH",
            "CURRENT_ROLE",
            "CURRENT_SCHEMA",
            "CURRENT_TIME",
            "CURRENT_TIMESTAMP",
            "CURRENT_USER",
            "DAY",
            "DEFAULT",
            "DISTINCT",
            "ELSE",
            "END",
            "EXCEPT",
            "EXISTS",
            "FALSE",
            "FETCH",
            "FILTER",
            "FOR",
            "FOREIGN",
            "FROM",
            "FULL",
            "GROUP",
            "GROUPS",
            "HAVING",
            "HOUR",
            "IF",
            "ILIKE",
            "IN",
            "INNER",
            "INTERSECT",
            "INTERSECTS",
            "INTERVAL",
            "IS",
            "JOIN",
            "KEY",
            "LEADING",
            "LEFT",
            "LIKE",
            "LIMIT",
            "LOCALTIME",
            "LOCALTIMESTAMP",
            "MINUS",
            "MINUTE",
            "MONTH",
            "NATURAL",
            "NOT",
            "NULL",
            "OFFSET",
            "ON",
            "OR",
            "ORDER",
            "OVER",
            "PARTITION",
            "PRIMARY",
            "QUALIFY",
            "RANGE",
            "REGEXP",
            "RIGHT",
            "ROW",
            "ROWNUM",
            "ROWS",
            "SECOND",
            "SELECT",
            "SESSION_USER",
            "SET",
            "SOME",
            "SYMMETRIC",
            "SYSTEM_USER",
            "TABLE",
            "TO",
            "TOP",
            "TRAILING",
            "TRUE",
            "UESCAPE",
            "UNION",
            "UNIQUE",
            "UNKNOWN",
            "USER",
            "USING",
            "VALUE",
            "VALUES",
            "WHEN",
            "WHERE",
            "WINDOW",
            "WITH",
            "YEAR",
            "_ROWID_");

    public H2Dictionary() {
        platform = "H2";
        validationSQL = "CALL 1";
        closePoolSQL = "SHUTDOWN";

        supportsAutoAssign = true;
        lastGeneratedKeyQuery = "CALL IDENTITY()";
        autoAssignClause = "IDENTITY";
        autoAssignTypeName = "INTEGER";
        nextSequenceQuery = "CALL NEXT VALUE FOR {0}";

        stringLengthFunction = "LENGTH({0})";
        trimLeadingFunction = "LTRIM({0})";
        trimTrailingFunction = "RTRIM({0})";
        trimBothFunction = "TRIM({0})";

        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;
        rangePosition = RANGE_POST_LOCK;
        supportsDeferredConstraints = false;

        supportsNullTableForGetPrimaryKeys = true;
        supportsNullTableForGetIndexInfo = true;

        supportsLockingWithOuterJoin = false;
        supportsLockingWithInnerJoin = false;

        // no timezone support for time in h2
        timeWithZoneTypeName = "TIME";

        reservedWordSet.addAll(Arrays.asList(new String[] {
                "ALL",
                "CHECK",
                "CONSTRAINT",
                "CROSS",
                "CURRENT_DATE",
                "CURRENT_TIME",
                "CURRENT_TIMESTAMP",
                "DISTINCT",
                "EXCEPT",
                "EXISTS",
                "FALSE",
                "FETCH",
                "FOR",
                "FOREIGN",
                "FROM",
                "FULL",
                "GROUP",
                "HAVING",
                "INNER",
                "INTERSECT",
                "IS",
                "JOIN",
                "LIKE",
                "LIMIT",
                "MINUS",
                "NATURAL",
                "NOT",
                "NULL",
                "OFFSET",
                "ON",
                "ORDER",
                "PRIMARY",
                "ROWNUM",
                "SELECT",
                "SYSDATE",
                "SYSTIME",
                "SYSTIMESTAMP",
                "TODAY",
                "TRUE",
                "UNION",
                "UNIQUE",
                "WHERE",
                "WITH"
        }));

        // reservedWordSet subset that CANNOT be used as valid column names
        // (i.e., without surrounding them with double-quotes)
        // generated at 2021-05-02T14:32:50.704 via org.apache.openjpa.reservedwords.ReservedWordsIT
        invalidColumnWordSet.addAll(Arrays.asList(new String[] {
            "CHECK", "CONSTRAINT", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "DISTINCT", "END-EXEC",
            "EXCEPT", "EXISTS", "FALSE", "FETCH", "FOR", "FOREIGN", "FROM", "FULL", "GROUP", "HAVING", "INNER", "INTERSECT",
            "IS", "JOIN", "LIKE", "LIMIT", "MINUS", "NATURAL", "NOT", "NULL", "OFFSET", "ON", "ORDER", "PRIMARY", "ROWNUM",
            "SELECT", "SYSDATE", "TRUE", "UNION", "UNIQUE", "WHERE", "WITH",
        }));
    }

    @Override
    public void connectedConfiguration(Connection conn) throws SQLException {
        super.connectedConfiguration(conn);
        if (versionLaterThan(1)) {
            supportsGetGeneratedKeys = true;
            supportsNullTableForGetPrimaryKeys = false;
            supportsNullTableForGetIndexInfo = false;
            autoAssignClause = "GENERATED ALWAYS AS IDENTITY";
            bitTypeName = "BOOLEAN";
            timeTypeName = "TIME(9)";
            timestampTypeName = "TIMESTAMP(9)";
            timeWithZoneTypeName = "TIME(9) WITH TIME ZONE";
            timestampWithZoneTypeName = "TIMESTAMP(9) WITH TIME ZONE";
            booleanRepresentation = BooleanRepresentationFactory.BOOLEAN;
            booleanTypeName = "BOOLEAN";
            reservedWordSet.clear();
            reservedWordSet.addAll(V2_KEYWORDS);
            invalidColumnWordSet.clear();
            invalidColumnWordSet.addAll(V2_KEYWORDS);
        }
    }

    @Override
    public int getJDBCType(int metaTypeCode, boolean lob) {
        int type = super.getJDBCType(metaTypeCode, lob);
        switch (type) {
        case Types.BIGINT:
            if (metaTypeCode == JavaTypes.BIGINTEGER)
                return Types.NUMERIC;
            break;
        }
        return type;
    }

    @Override
    public int getPreferredType(int type) {
        if(type == Types.BIT)
            return Types.BOOLEAN;
        return type;
    }

    @Override
    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        return new String[0];
    }

    @Override
    public String[] getDropPrimaryKeySQL(PrimaryKey pk) {
        return new String[0];
    }

    @Override
    public String[] getAddColumnSQL(Column column) {
        return new String[] {
            "ALTER TABLE " + getFullName(column.getTable(), false)
                + " ADD COLUMN " + getDeclareColumnSQL(column, true)
        };
    }

    @Override
    public String[] getCreateTableSQL(Table table) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ").append(getFullName(table, false))
            .append(" (");

        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(getDeclareColumnSQL(cols[i], false));
        }

        PrimaryKey pk = table.getPrimaryKey();
        String pkStr;
        if (pk != null) {
            pkStr = getPrimaryKeyConstraintSQL(pk);
            if (!StringUtil.isEmpty(pkStr))
                buf.append(", ").append(pkStr);
        }

        Unique[] unqs = table.getUniques();
        String unqStr;
        for (Unique unq : unqs) {
            unqStr = getUniqueConstraintSQL(unq);
            if (unqStr != null)
                buf.append(", ").append(unqStr);
        }

        buf.append(")");
        return new String[] { buf.toString() };
    }

    @Override
    protected String getPrimaryKeyConstraintSQL(PrimaryKey pk) {
        if (versionLaterThan(1)) {
            return super.getPrimaryKeyConstraintSQL(pk);
        } else {
            Column[] cols = pk.getColumns();
            if (cols.length == 1 && cols[0].isAutoAssigned())
                return null;
            return super.getPrimaryKeyConstraintSQL(pk);
        }
    }

    @Override
    public boolean isSystemIndex(String name, Table table) {
        return name.toUpperCase(Locale.ENGLISH).startsWith("SYSTEM_");
    }

    @Override
    public boolean isSystemIndex(DBIdentifier name, Table table) {
        if (DBIdentifier.isNull(name)) {
            return false;
        }
        return name.getName().toUpperCase(Locale.ENGLISH).startsWith("SYSTEM_");
    }

    @Override
    protected String getSequencesSQL(String schemaName, String sequenceName) {
        return getSequencesSQL(DBIdentifier.newSchema(schemaName), DBIdentifier.newSequence(sequenceName));
    }

    @Override
    protected String getSequencesSQL(DBIdentifier schemaName, DBIdentifier sequenceName) {
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT SEQUENCE_SCHEMA, SEQUENCE_NAME FROM ")
            .append("INFORMATION_SCHEMA.SEQUENCES");
        if (!DBIdentifier.isNull(schemaName) || !DBIdentifier.isNull(sequenceName))
            buf.append(" WHERE ");
        if (!DBIdentifier.isNull(schemaName)) {
            buf.append("SEQUENCE_SCHEMA = ?");
            if (!DBIdentifier.isNull(sequenceName))
                buf.append(" AND ");
        }
        if (!DBIdentifier.isNull(sequenceName))
            buf.append("SEQUENCE_NAME = ?");
        return buf.toString();
    }

    @Override
    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        return getColumns(meta, DBIdentifier.newCatalog(catalog), DBIdentifier.newSchema(schemaName),
            DBIdentifier.newTable(tableName), DBIdentifier.newColumn(columnName), conn);
    }

    @Override
    public Column[] getColumns(DatabaseMetaData meta, DBIdentifier catalog,
        DBIdentifier schemaName, DBIdentifier tableName, DBIdentifier columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);
        return cols;
    }

    @Override
    public void setLocalDate(PreparedStatement stmnt, int idx, LocalDate val, Column col) throws SQLException {
        stmnt.setObject(idx, val);
    }

    @Override
    public LocalDate getLocalDate(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    @Override
    public void setLocalTime(PreparedStatement stmnt, int idx, LocalTime val, Column col) throws SQLException {
        stmnt.setObject(idx, val);
    }

    @Override
    public LocalTime getLocalTime(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, LocalTime.class);
    }

    @Override
    public void setLocalDateTime(PreparedStatement stmnt, int idx, LocalDateTime val, Column col) throws SQLException {
        stmnt.setObject(idx, val);
    }

    @Override
    public LocalDateTime getLocalDateTime(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    @Override
    public void setOffsetDateTime(PreparedStatement stmnt, int idx, OffsetDateTime val, Column col) throws SQLException {
        stmnt.setObject(idx, val);
    }

    @Override
    public int getInt(ResultSet rs, int column) throws SQLException {
        //rs.getInt perform rounding `25.5 -> 26`
        final BigDecimal decRes = rs.getBigDecimal(column);
        return decRes == null ? 0 : decRes.intValue();
    }

    /**
     * h2 does intentionally not support {@code getTimestamp()} for 'TIME WITH TIME ZONE' columns.
     * See h2 ticket #413.
     */
    @Override
    public OffsetDateTime getOffsetDateTime(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }

    @Override
    protected void appendSelectRange(SQLBuffer buf, long start, long end,
        boolean subselect) {
        if (end != Long.MAX_VALUE)
            buf.append(" LIMIT ").appendValue(end - start);
        if (start != 0) {
            buf.append(" OFFSET ").appendValue(start);
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

    @Override
    public boolean isFatalException(int subtype, SQLException ex) {
        int errorCode = ex.getErrorCode();
        if ((subtype == StoreException.QUERY || subtype == StoreException.LOCK)
            && (57014 == errorCode || 50200 == errorCode)) {
            return false;
        }
        return super.isFatalException(subtype, ex);
    }
}

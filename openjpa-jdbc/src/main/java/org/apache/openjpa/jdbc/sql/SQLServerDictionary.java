/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.openjpa.jdbc.sql;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.Normalizer;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.StoreException;


/**
 * Dictionary for Microsoft SQL Server.
 */
public class SQLServerDictionary extends AbstractSQLServerDictionary {

    public static final String VENDOR_MICROSOFT = "microsoft";
    public static final String VENDOR_NETDIRECT = "netdirect";
    public static final String VENDOR_JTDS = "jtds";

    private static final Localizer _loc =
        Localizer.forPackage(SQLServerDictionary.class);

    /**
     * The largest precision of a decimal column on SQL Server.
     */
    private static final int MAX_DECIMAL_PRECISION = 38;

    /**
     * The largest scale of a decimal column on SQL Server, which cannot be
     * larger than the precision.
     */
    private static final int MAX_DECIMAL_SCALE = MAX_DECIMAL_PRECISION;

    /**
     * The scale of an unsized decimal column. It leaves 28 digits before the
     * decimal point, more than a <code>long</code> has.
     */
    private static final int UNSIZED_DECIMAL_SCALE = 10;

    private String schemaCase = SCHEMA_CASE_PRESERVE;
    /**
     * Flag whether to treat UNIQUEIDENTIFIER as VARBINARY or VARCHAR
     */
    public boolean uniqueIdentifierAsVarbinary = true;

    /**
     * SQLServer doesn't like a java.sql.Time as default.
     * So either we send it as String or people configure sendTimeAsDatetime=false on the Connection.
     * This is depending how the Database actually is setup.
     * To mitigate misconfiguration we can work around by sending the time as String to the JDBC driver.
     */
    public Boolean sendTimeAsString = null;

    public SQLServerDictionary() {
        platform = "Microsoft SQL Server";
        // SQLServer locks on a table-by-table basis
        forUpdateClause = null;
        tableForUpdateClause = "WITH (UPDLOCK)";
        supportsNullTableForGetColumns = false;
        requiresAliasForSubselect = true;
        stringLengthFunction = "LEN({0})";

        timeWithZoneTypeName = "TIME";
        timestampWithZoneTypeName = "DATETIMEOFFSET";

        indexPhysicalForeignKeys = true; // MS-SQLServer does not automatically create an index for a foreign key so we will

        // SQL Server supports CHAR, so Java char fields are stored as CHAR(1)
        // rather than as their numeric code point. Schemas created by earlier
        // releases need an explicit StoreCharsAsNumbers=true (OPENJPA-3033).
        storeCharsAsNumbers = false;

        // the precision and scale of a decimal column are part of its DDL type,
        // an unsized DECIMAL is DECIMAL(18,0) (OPENJPA-3064)
        fixedSizeTypeNameSet.remove("NUMERIC");
        fixedSizeTypeNameSet.remove("DECIMAL");

        // reservedWordSet subset that CANNOT be used as valid column names
        // (i.e., without surrounding them with double-quotes)
        // generated at 2021-05-02T16:15:30.630 via org.apache.openjpa.reservedwords.ReservedWordsIT
        invalidColumnWordSet.addAll(Arrays.asList(new String[] {
            "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTHORIZATION", "BACKUP", "BEGIN", "BETWEEN", "BREAK", "BROWSE",
            "BULK", "BY", "CASCADE", "CASE", "CHECK", "CHECKPOINT", "CLOSE", "CLUSTERED", "COALESCE", "COLLATE", "COLUMN",
            "COMMIT", "COMPUTE", "CONSTRAINT", "CONTAINS", "CONTAINSTABLE", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT",
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", "DBCC", "DEALLOCATE",
            "DECLARE", "DEFAULT", "DELETE", "DENY", "DESC", "DISTINCT", "DISTRIBUTED", "DOUBLE", "DROP", "ELSE", "END", "END-EXEC",
            "ERRLVL", "ESCAPE", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FETCH", "FILE", "FILLFACTOR", "FOR",
            "FOREIGN", "FREETEXT", "FREETEXTTABLE", "FROM", "FULL", "FUNCTION", "GOTO", "GRANT", "GROUP", "HAVING", "HOLDLOCK",
            "IDENTITY", "IDENTITY_INSERT", "IDENTITYCOL", "IF", "IN", "INDEX", "INNER", "INSERT", "INTERSECT", "INTO", "IS",
            "JOIN", "KEY", "KILL", "LEFT", "LIKE", "LINENO", "MERGE", "NATIONAL", "NOCHECK", "NONCLUSTERED", "NOT", "NULL",
            "NULLIF", "OF", "OFF", "OFFSETS", "ON", "OPEN", "OPENDATASOURCE", "OPENQUERY", "OPENROWSET", "OPENXML", "OPTION",
            "OR", "ORDER", "OUTER", "OVER", "PERCENT", "PLAN", "PRIMARY", "PRINT", "PROC", "PROCEDURE", "PUBLIC", "RAISERROR",
            "READ", "READTEXT", "RECONFIGURE", "REFERENCES", "REPLICATION", "RESTORE", "RESTRICT", "RETURN", "REVOKE", "RIGHT",
            "ROLLBACK", "ROWCOUNT", "ROWGUIDCOL", "RULE", "SAVE", "SCHEMA", "SELECT", "SESSION_USER", "SET", "SETUSER", "SHUTDOWN",
            "SOME", "STATISTICS", "SYSTEM_USER", "TABLE", "TABLESAMPLE", "TEXTSIZE", "THEN", "TO", "TOP", "TRAN", "TRANSACTION",
            "TRIGGER", "TRUNCATE", "TSEQUAL", "UNION", "UNIQUE", "UPDATE", "UPDATETEXT", "USE", "USER", "VALUES", "VARYING",
            "VIEW", "WAITFOR", "WHEN", "WHERE", "WHILE", "WITH", "WRITETEXT",
        }));
    }

    @Override
    public void connectedConfiguration(Connection conn) throws SQLException {
        super.connectedConfiguration(conn);
        boolean requiresWarnings = true;
        DatabaseMetaData meta = conn.getMetaData();
        String driverName = meta.getDriverName();
        String url = meta.getURL();
        if (driverVendor == null) {
            // serverMajorVersion of 8==2000, 9==2005, 10==2008,  11==2012
            if (meta.getDatabaseMajorVersion() >= 9) {
                setSupportsXMLColumn(true);
                if (sendTimeAsString == null) {
                    sendTimeAsString = Boolean.FALSE;
                }
            }
            if (meta.getDatabaseMajorVersion() >= 10) {
                // MSSQL 2008 supports new date, time and datetime2 types
                // Use DATETIME2 which has 100ns vs. 3.333msec precision
                dateTypeName = "DATE";
                timeTypeName = "TIME";
                timestampTypeName = "DATETIME2";
                datePrecision = MICRO / 10;
            }
            if (meta.getDatabaseMajorVersion() >= 11) {
                //SQLServer 2012 supports range select
                rangePosition = RANGE_POST_SELECT;
                supportsSelectStartIndex = true;
                supportsSelectEndIndex = true;
            }
            if (driverName != null) {
                if (driverName.startsWith("Microsoft SQL Server")) {
                    // v1.1, 1.2, 2.0 or 3.0 driver
                    driverVendor = VENDOR_MICROSOFT;
                    if (meta.getDriverMajorVersion() >= 2) {
                        // see http://blogs.msdn.com/jdbcteam/archive/2007/05/\
                        // 02/what-is-adaptive-response-buffering-and-why-\
                        // should-i-use-it.aspx
                        // 2.0 driver connectURL automatically includes
                        // responseBuffering=adaptive
                        // and disableStatementPooling=true
                        requiresWarnings = false;
                    }
                } else {
                    if ("NetDirect JSQLConnect".equals(driverName))
                        driverVendor = VENDOR_NETDIRECT;
                    else if (driverName.startsWith("jTDS"))
                        driverVendor = VENDOR_JTDS;
                    else if ("SQLServer".equals(driverName)) {
                        if (url != null &&
                            url.startsWith("jdbc:microsoft:sqlserver:"))
                            driverVendor = VENDOR_MICROSOFT;
                        else if (url != null &&
                            url.startsWith("jdbc:datadirect:sqlserver:"))
                            driverVendor = VENDOR_DATADIRECT;
                        else
                            driverVendor = VENDOR_OTHER;
                    }
                    // old way of determining xml support
                    if (driverName.indexOf(platform) != -1) {
                        String versionString =
                            driverName.substring(platform.length() + 1);
                        if (versionString.indexOf(" ") != -1)
                            versionString = versionString.substring(0,
                                versionString.indexOf(" "));
                        int version = Integer.parseInt(versionString);
                        if (version >= 2005)
                            setSupportsXMLColumn(true);
                    }
                }
            } else {
                driverVendor = VENDOR_OTHER;
            }
        }

        // SQL Server 2012 introduced sequence objects. Without a
        // NextSequenceQuery a @SequenceGenerator was backed by a row of the
        // OpenJPA sequence table (OPENJPA-3014), and the schema tool generated
        // no CREATE SEQUENCE for it. A value seen here was configured
        // explicitly and is left alone.
        if (nextSequenceQuery == null && meta.getDatabaseMajorVersion() >= 11) {
            nextSequenceQuery = "SELECT NEXT VALUE FOR {0}";
        }

        // warn about not using cursors for pre-2.0 MS driver
        // as connectURL includes selectMethod=direct
        if (((VENDOR_MICROSOFT.equalsIgnoreCase(driverVendor) &&
            requiresWarnings) ||
            VENDOR_DATADIRECT.equalsIgnoreCase(driverVendor)) &&
            (url.toLowerCase(Locale.ENGLISH).indexOf("selectmethod=cursor") == -1))
            log.warn(_loc.get("sqlserver-cursor", url));

        // warn about prepared statement caching if using pre-2.0 MS drivers
        // as connectURL includes responseBuffering=full
        String props = conf.getConnectionFactoryProperties();
        if ((props != null) &&
            VENDOR_MICROSOFT.equalsIgnoreCase(driverVendor) &&
            requiresWarnings &&
            (props.toLowerCase(Locale.ENGLISH).indexOf("maxcachedstatements=0") == -1))
            log.warn(_loc.get("sqlserver-cachedstmnts"));
    }

    /**
     * A <code>DATETIME</code> column, reported with three fractional digits,
     * stores multiples of 1/300 of a second, which the driver's
     * <code>DATETIME2</code> parameter of a millisecond value does not
     * compare equal to: .123 is stored as .1233333. Multiples of 10
     * milliseconds are exact in both types. A <code>DATETIME2(3)</code>
     * column keeps whole milliseconds and is left to the default.
     */
    @Override
    public int getTimestampPrecision(Column col) {
        if (col != null && col.getPrecision() == 3
            && "DATETIME".equalsIgnoreCase(col.getTypeIdentifier().getName())) {
            return MILLI * 10;
        }
        return super.getTimestampPrecision(col);
    }

    /**
     * A SQL Server sequence created without <code>START WITH</code> starts at
     * the minimum value of its type, -2^63 for the default
     * <code>BIGINT</code>, rather than at 0, so an initial value of 0 is
     * given explicitly.
     */
    @Override
    public String[] getCreateSequenceSQL(Sequence seq) {
        String[] sql = super.getCreateSequenceSQL(seq);
        if (sql != null && seq.getInitialValue() == 0) {
            String name = getFullName(seq);
            for (int i = 0; i < sql.length; i++) {
                int idx = sql[i].indexOf(name) + name.length();
                sql[i] = sql[i].substring(0, idx) + " START WITH 0" + sql[i].substring(idx);
            }
        }
        return sql;
    }

    /**
     * SQL Server lists sequences in <code>sys.sequences</code>. Without a
     * schema the query is restricted to the default schema of the user, where
     * an unqualified sequence is created; <code>sys.sequences</code> only
     * covers the current database.
     */
    @Override
    protected String getSequencesSQL(String schemaName, String sequenceName) {
        return getSequencesSQL(DBIdentifier.newSchema(schemaName),
            DBIdentifier.newSequence(sequenceName));
    }

    @Override
    protected String getSequencesSQL(DBIdentifier schemaName, DBIdentifier sequenceName) {
        if (nextSequenceQuery == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT SCHEMA_NAME(schema_id) AS SEQUENCE_SCHEMA, ")
            .append("name AS SEQUENCE_NAME FROM sys.sequences");
        if (!DBIdentifier.isNull(schemaName)) {
            buf.append(" WHERE SCHEMA_NAME(schema_id) = ?");
        } else {
            buf.append(" WHERE schema_id = SCHEMA_ID()");
        }
        if (!DBIdentifier.isNull(sequenceName)) {
            buf.append(" AND name = ?");
        }
        return buf.toString();
    }

    /**
     * DATEPART(WEEK) counts the weeks from January 1st; DATEPART(ISO_WEEK)
     * gives the ISO-8601 week that JPA defines. DATEPART(SECOND) returns the
     * whole seconds only; the fraction of the second that JPA includes is
     * added from DATEPART(NANOSECOND).
     */
    @Override
    public String getExtractDateTimeFieldFunction(DateTimeExtractField extField) {
        if (extField == DateTimeExtractField.WEEK) {
            return extractDateTimeFieldFunction.replace("{0}", "ISO_WEEK");
        }
        if (extField == DateTimeExtractField.SECOND) {
            return "(" + super.getExtractDateTimeFieldFunction(extField) + " + "
                + extractDateTimeFieldFunction.replace("{0}", "NANOSECOND") + " / 1000000000.0)";
        }
        return super.getExtractDateTimeFieldFunction(extField);
    }

    /**
     * SQL Server reads an unsized <code>NUMERIC</code> or <code>DECIMAL</code>
     * as <code>DECIMAL(18,0)</code>, which rounds every value to an integer. A
     * decimal column of a non integral Java type without a precision is
     * created with the largest precision and the given scale, or a scale of
     * 10 if none is given, so that the fraction of the value is kept. A scale
     * above 38, which SQL Server rejects, is lowered to 38. A
     * <code>NumericTypeName</code> or <code>DecimalTypeName</code> set by the
     * user is kept.
     */
    @Override
    public String getTypeName(Column col) {
        if (isUnsizedDecimal(col)) {
            String typeName = getTypeName(col.getType());
            if ("NUMERIC".equalsIgnoreCase(typeName) || "DECIMAL".equalsIgnoreCase(typeName)) {
                int scale = col.getDecimalDigits() > 0
                    ? Math.min(col.getDecimalDigits(), MAX_DECIMAL_SCALE) : UNSIZED_DECIMAL_SCALE;
                return typeName + "(" + MAX_DECIMAL_PRECISION + ", " + scale + ")";
            }
        }
        return super.getTypeName(col);
    }

    @Override
    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        return getColumns(meta, DBIdentifier.newCatalog(catalog),
            DBIdentifier.newSchema(schemaName),
            DBIdentifier.newTable(tableName),
            DBIdentifier.newColumn(columnName),
            conn);
    }

    @Override
    public Column[] getColumns(DatabaseMetaData meta, DBIdentifier catalog,
        DBIdentifier schemaName, DBIdentifier tableName, DBIdentifier columnName, Connection conn)
        throws SQLException {

        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);

        // for opta driver, which reports nvarchar as unknown type
        for (int i = 0; cols != null && i < cols.length; i++) {
            String typeName = cols[i].getTypeIdentifier().getName();
            if (typeName == null)
                continue;

            typeName = typeName.toUpperCase(Locale.ENGLISH);
            if ("NVARCHAR".equals(typeName))
                cols[i].setType(Types.VARCHAR);
            else if ("UNIQUEIDENTIFIER".equals(typeName)) {
                if (uniqueIdentifierAsVarbinary)
                    cols[i].setType(Types.VARBINARY);
                else
                    cols[i].setType(Types.VARCHAR);
            } else if ("NCHAR".equals(typeName))
                cols[i].setType(Types.CHAR);
            else if ("NTEXT".equals(typeName))
                cols[i].setType(Types.CLOB);
        }
        return cols;
    }

    /**
     * SQL Server refuses to drop a column that the primary key, a unique
     * constraint, an index, a default constraint or a foreign key of the
     * table depends on, while most other databases drop these with the
     * column. The schema tool drops single columns when a table holds columns
     * that are not mapped, for example the columns of a subclass that is not
     * listed. Drop the objects that depend on the column, as found in the
     * catalog views, and then the column, in one transaction, so that nothing
     * is dropped if the column cannot be dropped, for example because a
     * foreign key of another table references the primary key.
     */
    @Override
    public String[] getDropColumnSQL(Column column) {
        String[] dropColumn = super.getDropColumnSQL(column);
        if (dropColumn.length == 0) {
            return dropColumn;
        }
        String table = getFullName(column.getTable(), false);
        String tableLiteral = "N'" + table.replace("'", "''") + "'";
        String columnLiteral = "N'" + toDBName(column.getIdentifier(), false).replace("'", "''") + "'";
        String tableId = "OBJECT_ID(" + tableLiteral + ")";
        String columnId = "COLUMNPROPERTY(" + tableId + ", " + columnLiteral + ", 'ColumnId')";
        String dropConstraint = "N'ALTER TABLE ' + " + tableLiteral + " + N' DROP CONSTRAINT ' + QUOTENAME(";
        StringBuilder sql = new StringBuilder()
            .append("DECLARE @sql NVARCHAR(MAX) = N''; ")
            .append("SELECT @sql = @sql + ").append(dropConstraint).append("f.name) + N'; ' ")
            .append("FROM sys.foreign_keys f WHERE f.parent_object_id = ").append(tableId)
            .append(" AND EXISTS (SELECT 1 FROM sys.foreign_key_columns fc ")
            .append("WHERE fc.constraint_object_id = f.object_id AND fc.parent_column_id = ")
            .append(columnId).append("); ")
            .append("SELECT @sql = @sql + CASE WHEN i.is_primary_key = 1 OR i.is_unique_constraint = 1 ")
            .append("THEN ").append(dropConstraint).append("i.name) ")
            .append("ELSE N'DROP INDEX ' + QUOTENAME(i.name) + N' ON ' + ").append(tableLiteral)
            .append(" END + N'; ' FROM sys.indexes i WHERE i.object_id = ").append(tableId)
            .append(" AND EXISTS (SELECT 1 FROM sys.index_columns ic ")
            .append("WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.column_id = ")
            .append(columnId).append("); ")
            .append("SELECT @sql = @sql + ").append(dropConstraint).append("d.name) + N'; ' ")
            .append("FROM sys.default_constraints d WHERE d.parent_object_id = ").append(tableId)
            .append(" AND d.parent_column_id = ").append(columnId).append("; ")
            .append("BEGIN TRY BEGIN TRANSACTION; EXEC sp_executesql @sql; ");
        for (String s : dropColumn) {
            sql.append(s).append("; ");
        }
        sql.append("COMMIT TRANSACTION; END TRY ")
            .append("BEGIN CATCH IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION; THROW; END CATCH");
        return new String[]{ sql.toString() };
    }

    @Override
    protected void appendLength(SQLBuffer buf, int type) {
        if (type == Types.VARCHAR)
            buf.append("(").append(Integer.toString(characterColumnSize))
                .append(")");
    }

    /**
     * If this dictionary supports XML type, use this method to append xml
     * predicate.
     *
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison
     * @param rhs the right hand side of the comparison
     * @param lhsxml indicates whether the left operand maps to xml
     * @param rhsxml indicates whether the right operand maps to xml
     */
    @Override
    public void appendXmlComparison(SQLBuffer buf, String op, FilterValue lhs,
        FilterValue rhs, boolean lhsxml, boolean rhsxml) {
        super.appendXmlComparison(buf, op, lhs, rhs, lhsxml, rhsxml);
        if (lhsxml && rhsxml)
            appendXmlComparison2(buf, op, lhs, rhs);
        else if (lhsxml)
            appendXmlComparison1(buf, op, lhs, rhs);
        else
            appendXmlComparison1(buf, op, rhs, lhs);
    }

    /**
     * Append an xml comparison predicate
     *
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison (maps to xml column)
     * @param rhs the right hand side of the comparison
     */
    private void appendXmlComparison1(SQLBuffer buf, String op,
        FilterValue lhs, FilterValue rhs) {
        boolean castrhs = rhs.isConstant();
        if (castrhs)
            appendXmlValue(buf, lhs);
        else
            appendXmlExist(buf, lhs);
        buf.append(" ").append(op).append(" ");
        if (castrhs)
            rhs.appendTo(buf);
        else {
            buf.append("sql:column(\"");
            rhs.appendTo(buf);
            buf.append("\")").append("]') = 1");
        }
    }

    private void appendXmlExist(SQLBuffer buf, FilterValue lhs) {
        buf.append(lhs.getColumnAlias(lhs.getFieldMapping().getColumns()[0]))
            .append(".exist('").append("/*[");
        lhs.appendTo(buf);
    }

    /**
     * Append an xml comparison predicate (both operands map to xml column)
     *
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison (maps to xml column)
     * @param rhs the right hand side of the comparison (maps to xml column)
     */
    private void appendXmlComparison2(SQLBuffer buf, String op,
        FilterValue lhs, FilterValue rhs) {
        appendXmlValue(buf, lhs);
        buf.append(" ").append(op).append(" ");
        appendXmlValue(buf, rhs);
    }

    private void appendXmlValue(SQLBuffer buf, FilterValue val) {
        Class<?> rc = Filters.wrap(val.getType());
        int type = getJDBCType(JavaTypes.getTypeCode(rc), false);
        boolean isXmlAttribute = (val.getXmlMapping() == null)
            ? false : val.getXmlMapping().isXmlAttribute();
        buf.append(val.getColumnAlias(val.getFieldMapping().getColumns()[0]))
            .append(".value(").append("'(/*/");
        val.appendTo(buf);
        if (!isXmlAttribute)
            buf.append("/text()");
        buf.append(")[1]','").append(getTypeName(type));
        appendLength(buf, type);
        buf.append("')");
    }

    /**
     * SQL Server reports a procedure as {@code name;version} and its parameters with a leading {@code @}.
     * Both are stripped so that the procedure can be called by its name and its parameters can be
     * referenced by the names they were declared with.
     */
    @Override
    public StoredProcedure getStoredProcedure(DatabaseMetaData meta, DBIdentifier catalog, DBIdentifier schema,
                                              String procedure) throws SQLException {
        StoredProcedure sp = super.getStoredProcedure(meta, catalog, schema, procedure);
        if (sp == null) {
            return null;
        }
        String name = Normalizer.removeDelimiters(sp.getName());
        int version = name == null ? -1 : name.lastIndexOf(';');
        if (version > 0) {
            sp.setName(name.substring(0, version));
        }
        for (Column col : sp.getColumns()) {
            String param = Normalizer.removeDelimiters(col.getIdentifier().getName());
            if (param != null && param.length() > 1 && param.charAt(0) == '@') {
                col.setIdentifier(DBIdentifier.newColumn(param.substring(1)));
            }
        }
        return sp;
    }

    /**
     * SQL Server does not understand a bare {@code CALL} statement, hence the JDBC escape syntax
     * {@code {call name(?, ...)}} is used.
     */
    @Override
    public String getStoredProcedureCallSQL(StoredProcedure procedure) {
        StringBuilder buf = new StringBuilder("{call ").append(procedure.getName());
        int params = procedure.getColumns().length;
        if (params > 0) {
            buf.append('(');
            for (int i = 0; i < params; i++) {
                buf.append(i == 0 ? "?" : ",?");
            }
            buf.append(')');
        }
        return buf.append('}').toString();
    }

    /**
     * Return DB specific schemaCase
     */
    @Override
    public String getSchemaCase() {
        return schemaCase;
    }

    @Override
    public void setSupportsXMLColumn(boolean b) {
        super.setSupportsXMLColumn(b);
        // MS SQL Server requires XML data in UTF-16 or UCS-2 instead of JAXB default of UTF-8
        super.setXMLTypeEncoding("UTF-16");
    }

    @Override
    public boolean isFatalException(int subtype,  SQLException ex) {
        String errorState = ex.getSQLState();
        if ((subtype == StoreException.LOCK  || subtype == StoreException.QUERY)
          &&("1222".equals(errorState) || "HY008".equals(errorState) || "HYT00".equals(errorState)))
         return false;
        return super.isFatalException(subtype, ex);
    }

    /**
     * Obtain an {@link InputStream} by using {@link ResultSet#getBlob(int)} and
     * {@link Blob#getBinaryStream()}.
     * Unfortunately this will load entire BLOB into memory.
     * The alternative {@link ResultSet#getBinaryStream(int)} provides true streaming but
     * the stream can be consumed only as long as {@link ResultSet} is open.
     */
    @Override
    public InputStream getLOBStream(JDBCStore store, ResultSet rs, int column) throws SQLException {
        Blob blob = rs.getBlob(column);
        if (blob == null) {
            return null;
        }
        return blob.getBinaryStream();
    }

    /**
     * Obtain a {@link Reader} by using {@link ResultSet#getClob(int)} and
     * {@link Clob#getCharacterStream()}.
     * Unfortunately this will load entire CLOB into memory.
     * The alternative {@link ResultSet#getCharacterStream(int)} provides true streaming but
     * the stream can be consumed only as long as {@link ResultSet} is open.
     */
    @Override
    public Reader getCharacterStream(ResultSet rs, int column) throws SQLException {
        Clob clob = rs.getClob(column);
        if (clob == null) {
            return null;
        }
        return clob.getCharacterStream();
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

    /**
     * h2 does intentionally not support {@code getTimestamp()} for 'TIME WITH TIME ZONE' columns.
     * See h2 ticket #413.
     */
    @Override
    public OffsetDateTime getOffsetDateTime(ResultSet rs, int column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }

    @Override
    public void setTime(PreparedStatement stmnt, int idx, Time val, Calendar cal, Column col) throws SQLException {
        if (sendTimeAsString) {
            stmnt.setString(idx, val.toString());
        }
        else {
            // use Time
            super.setTime(stmnt, idx, val, cal, col);
        }
    }

    @Override
    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("CHARINDEX(");
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
    protected void appendSelectRange(SQLBuffer buf, long start, long end, boolean subselect) {
        //SQL Server 2012 supports range select
        if (this.getMajorVersion() >= 11) {
            //we need an order by clause....
            if (!buf.getSQL().contains(" ORDER BY ")) {
                buf.append(" ORDER BY 1 ");
            }
            buf.append(" OFFSET ").append(Long.toString(start)).append(" ROWS ").
                    append(" FETCH NEXT ").append(Long.toString(end - start)).append(" ROWS ONLY ");
        } else {
            super.appendSelectRange(buf, start, end, subselect);
        }
    }

    /**
     * MSSQLServer do not support ANSI SQL {@code NULLS FIRST} / {@code NULLS LAST}.
     * Emulate via an auxiliary {@code IIF(&lt;expr&gt; IS NULL, 0, 1)} sort key.
     * <p>
     * MSSQLServer's default NULL ordering places NULLs before non-NULLs when sorting
     * ASC and after non-NULLs when sorting DESC. When the requested precedence
     * already matches that default, nothing extra is emitted. Otherwise the
     * last order term {@code &lt;expr&gt; ASC|DESC} is rewritten to
     * {@code IIF(&lt;expr&gt; IS NULL, 0, 1) &lt;sort&gt;, &lt;expr&gt; ASC|DESC}.
     */
    @Override
    public void appendNullsPrecedence(SQLBuffer ordering, int nullPrecedence) {
        final String nullSort = (nullPrecedence == QueryExpressions.NULLS_FIRST) ? "ASC" : "DESC";
        emulateNullsPrecedence(ordering, nullPrecedence,
                expr -> "IIF(" + expr + " IS NULL, 0, 1) " + nullSort + ", " + expr);
    }

    public String toJDBCEscapedDateTimeLiteral(String escape, int parseType) {
        // SQL Server interprets ODBC's {t '...'} as datetime,
        // which has no implicit conversion to TIME -> err
        if (parseType == Literal.TYPE_TIME) {
            int start = escape.indexOf('\'');
            int end = escape.lastIndexOf('\'');
            if (start >= 0 && end > start) {
                return "CAST(" + escape.substring(start, end + 1) + " AS TIME)";
            }
        }
        return escape;
    }
}

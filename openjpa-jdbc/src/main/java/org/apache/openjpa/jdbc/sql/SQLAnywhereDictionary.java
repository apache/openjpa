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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.Normalizer;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.util.StoreException;

/**
 * Dictionary for SAP SQL Anywhere
 */
public class SQLAnywhereDictionary
    extends DBDictionary {

    public static final String VENDOR_SQLANYWHERE = "SAP SQLAnywhere";

    public SQLAnywhereDictionary() {
        platform = "SQL Anywhere";

        // SQLA does not support DEFERRABLE / INITIALLY DEFERRED constraints
        supportsDeferredConstraints = false;

        // SQLA does not support UNIQUE constraints when one of the columns is NULLable.
        supportsNullUniqueColumn = false;

        // SQLA supports comments on tables and columns using COMMENT ON syntax, but that is not used.
        supportsComments = false;

        /* reservedWords;
           Following the style of other DBDictionary, the reservedWords string is not set.

           The reservedWordSet is computed using the following query on SQLA V17
           (... replaced with SQL92 keywords from sql-keywords.rsrc):

            select list(reserved_word order by reserved_word )
            from (
            select UPPER(reserved_word) as reserved_word
            from sa_reserved_words()
            except all
            select row_value as reserved_word
            from sa_split_list('...',',')
            ORDER BY 1
            ) DT

            Individual databases might be /onfigured differently from the default:
            - reserved_keywords option -- enable the LIMIT keyword
            - non_keywords option -- disable particular keywords, allowing their use as identifiers
            These configurations are not considered and the reservedWordSet is
            based on the maximal set of possible keywords / reserved words.
        */
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "ARRAY", "ATTACH", "BACKUP", "BIGINT", "BINARY", "BOTTOM", "BREAK", "CALL", "CAPABILITY", "CHAR_CONVERT",
            "CHECKPOINT", "COMMENT", "COMPRESSED", "CONFLICT", "CONTAINS", "CUBE", "DATETIMEOFFSET", "DBSPACE",
            "DELETING", "DETACH", "DO", "DYNAMIC", "ELSEIF", "ENCRYPTED", "ENDIF", "EXECUTING", "EXECUTING_USER",
            "EXISTING", "EXTERNLOGIN", "FORCE", "FORWARD", "HOLDLOCK", "IDENTIFIED", "IF", "INDEX", "INOUT",
            "INSERTING", "INSTALL", "INSTEAD", "INTEGRATED", "INVOKING", "INVOKING_USER", "JSON", "KERBEROS",
            "LATERAL", "LIMIT", "LOCK", "LOGIN", "LONG", "MEMBERSHIP", "MERGE", "MESSAGE", "MODE", "MODIFY", "NEW",
            "NOHOLDLOCK", "NOTIFY", "NVARCHAR", "OFF", "OPENSTRING", "OPENXML", "OPTIONS", "OTHERS", "OUT", "OVER",
            "PASSTHROUGH", "PIVOT", "PRINT", "PROC", "PROCEDURE_OWNER", "PUBLICATION", "RAISERROR", "READTEXT",
            "REFERENCE", "REFRESH", "RELEASE", "REMOTE", "REMOVE", "RENAME", "REORGANIZE", "RESOURCE", "RESTORE",
            "RETURN", "ROLLUP", "ROW", "ROWTYPE", "SAVE", "SAVEPOINT", "SENSITIVE", "SETUSER", "SHARE", "SPATIAL",
            "START", "STOP", "SUBTRANS", "SUBTRANSACTION", "SYNCHRONIZE", "TINYINT", "TOP", "TRAN", "TREAT",
            "TRIGGER", "TRUNCATE", "TSEQUAL", "UNBOUNDED", "UNIQUEIDENTIFIER", "UNNEST", "UNPIVOT", "UNSIGNED",
            "UPDATING", "VALIDATE", "VARBINARY", "VARBIT", "VARIABLE", "VARRAY", "WAIT", "WAITFOR", "WHILE",
            "WINDOW", "WITHIN", "WRITETEXT", "XML"
        }));

        /* We want to include all SQL-92 reserved words from SQLAnywhere.
           Those are not yet in reservedWordSet (added in endConfiguration).
           Use the precise set:

            select LIST(STRING('"',word,'"') order by word ) words
            from (
            select upper(reserved_word) word
            from sa_reserved_words()
            ) D
        */
        invalidColumnWordSet.addAll(Arrays.asList(new String[]{
            "ADD", "ALL", "ALTER", "AND", "ANY", "ARRAY", "AS", "ASC", "ATTACH", "BACKUP", "BEGIN", "BETWEEN",
            "BIGINT", "BINARY", "BIT", "BOTTOM", "BREAK", "BY", "CALL", "CAPABILITY", "CASCADE", "CASE", "CAST",
            "CHAR", "CHAR_CONVERT", "CHARACTER", "CHECK", "CHECKPOINT", "CLOSE", "COMMENT", "COMMIT", "COMPRESSED",
            "CONFLICT", "CONNECT", "CONSTRAINT", "CONTAINS", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CUBE",
            "CURRENT", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATE", "DATETIMEOFFSET", "DBSPACE",
            "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE", "DELETING", "DESC", "DETACH", "DISTINCT",
            "DO", "DOUBLE", "DROP", "DYNAMIC", "ELSE", "ELSEIF", "ENCRYPTED", "END", "ENDIF", "ESCAPE", "EXCEPT",
            "EXCEPTION", "EXEC", "EXECUTE", "EXECUTING", "EXECUTING_USER", "EXISTING", "EXISTS", "EXTERNLOGIN",
            "FETCH", "FIRST", "FLOAT", "FOR", "FORCE", "FOREIGN", "FORWARD", "FROM", "FULL", "GOTO", "GRANT",
            "GROUP", "HAVING", "HOLDLOCK", "IDENTIFIED", "IF", "IN", "INDEX", "INNER", "INOUT", "INSENSITIVE",
            "INSERT", "INSERTING", "INSTALL", "INSTEAD", "INT", "INTEGER", "INTEGRATED", "INTERSECT", "INTO",
            "INVOKING", "INVOKING_USER", "IS", "ISOLATION", "JOIN", "JSON", "KERBEROS", "KEY", "LATERAL", "LEFT",
            "LIKE", "LIMIT", "LOCK", "LOGIN", "LONG", "MATCH", "MEMBERSHIP", "MERGE", "MESSAGE", "MODE", "MODIFY",
            "NATURAL", "NCHAR", "NEW", "NO", "NOHOLDLOCK", "NOT", "NOTIFY", "NULL", "NUMERIC", "NVARCHAR", "OF",
            "OFF", "ON", "OPEN", "OPENSTRING", "OPENXML", "OPTION", "OPTIONS", "OR", "ORDER", "OTHERS", "OUT",
            "OUTER", "OVER", "PASSTHROUGH", "PIVOT", "PRECISION", "PREPARE", "PRIMARY", "PRINT", "PRIVILEGES",
            "PROC", "PROCEDURE", "PROCEDURE_OWNER", "PUBLICATION", "RAISERROR", "READTEXT", "REAL", "REFERENCE",
            "REFERENCES", "REFRESH", "RELEASE", "REMOTE", "REMOVE", "RENAME", "REORGANIZE", "RESOURCE", "RESTORE",
            "RESTRICT", "RETURN", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW", "ROWTYPE", "SAVE", "SAVEPOINT",
            "SCROLL", "SELECT", "SENSITIVE", "SESSION", "SESSION_USER", "SET", "SETUSER", "SHARE", "SMALLINT",
            "SOME", "SPATIAL", "SQLCODE", "SQLSTATE", "START", "STOP", "SUBTRANS", "SUBTRANSACTION", "SYNCHRONIZE",
            "TABLE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TINYINT", "TO", "TOP", "TRAN", "TREAT", "TRIGGER",
            "TRUNCATE", "TSEQUAL", "UNBOUNDED", "UNION", "UNIQUE", "UNIQUEIDENTIFIER", "UNKNOWN", "UNNEST",
            "UNPIVOT", "UNSIGNED", "UPDATE", "UPDATING", "USER", "USING", "VALIDATE", "VALUES", "VARBINARY",
            "VARBIT", "VARCHAR", "VARIABLE", "VARRAY", "VARYING", "VIEW", "WAIT", "WAITFOR", "WHEN", "WHERE",
            "WHILE", "WINDOW", "WITH", "WITHIN", "WORK", "WRITETEXT", "XML"
        }));

        systemSchemaSet.addAll(Arrays.asList(new String[]{
             "SYS", "PUBLIC", "dbo", "diagnostics", "SA_DEBUG", "rs_systabgroup", "ml_server"
        }));

        // A SELECT statement may start with SELECT or WITH.
        selectWordSet.add("WITH");

        /* Based on the following query with names removed if they support a size.
            select list(string('"',x,'"'),',' order by x)
            from (
            ( select  upper(domain_name) x
            from sys.sysdomain
            union all
            select upper(type_name) from sys.sysusertype )
            except
            select UPPER(row_value) as x
            from sa_split_list('BIGINT,BIT,BLOB,CLOB,DATE,DECIMAL,DISTINCT,DOUBLE,FLOAT,INTEGER,'
                                ||'JAVA_OBJECT,NULL,NUMERIC,OTHER,REAL,REF,SMALLINT,STRUCT,TIME,TIMESTAMP,TINYINT')
            ) D
        */
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "ARRAY","DATETIME","LONG BINARY","LONG NVARCHAR","LONG VARBIT","LONG VARCHAR","MONEY","NTEXT","ROW",
            "SMALLDATETIME","SMALLMONEY","ST_GEOMETRY","SYSNAME","TEXT","TIMESTAMP WITH TIME ZONE","UNIQUEIDENTIFIER",
            "UNIQUEIDENTIFIERSTR","UNSIGNED BIGINT","UNSIGNED INT","UNSIGNED SMALLINT","XML",
        }));

        // Set in configureNamingUtil
        // schemaCase = SCHEMA_CASE_PRESERVE;

        // The SQL used to validate that a connection is still in a valid state.
        validationSQL = "SELECT NOW()";

        // SQLA can use FOR UPDATE but it requires setting ansi_update_constraints option. Use table lock hints.
        forUpdateClause = null;
        tableForUpdateClause = "WITH (UPDLOCK)";

        // Use TOP n OFFSET m. The alternative LIMIT/OFFSET might be disabled due to reserved_keywords.
        rangePosition = RANGE_POST_DISTINCT;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;

        // In the FROM clause, the correlation name is required according to ISO/IEC 2095-2:2011 7.6 <table reference>
        //   <derived table> [ AS ] <correlation name>
        requiresAliasForSubselect = true;

        // TODO: review -- this might be possible.
        allowsAliasInBulkClause = false;

        // SQLA interprets '\' as an escape character within string literals.
        searchStringEscape = "\\\\";

        // The '%' character is interpreted as modulo.
        supportsModOperator = true;

        // TODO: this can be supported by SQLA
        supportsXMLColumn = false;

        // SQLA has no restrictions on functions over long strings.
        supportsCaseConversionForLob = true;

        stringLengthFunction = "LENGTH({0})";
        bitLengthFunction = "(BYTE_LENGTH({0}) * 8)";

        trimLeadingFunction = "LTRIM({0})";
        trimTrailingFunction = "RTRIM({0})";
        trimBothFunction = "LTRIM(RTRIM({0}))";

        currentDateFunction = "CURRENT DATE";
        currentTimeFunction = "CURRENT TIME";

        datePrecision = MICRO;

        // The characterColumnSize is 255 -- SQLA supports up to 32767 but not sure if that is needed.

        useGetStringForClobs = true;
        useSetStringForClobs = true;
        useSetBytesForBlobs = true;

        dateTypeName = "TIMESTAMP";
        timeTypeName = "TIMESTAMP";
        blobTypeName = "LONG BINARY";
        clobTypeName = "LONG VARCHAR";
        longVarbinaryTypeName = "LONG BINARY";
        longVarcharTypeName = "LONG VARCHAR";

        // TODO: the XML type is encoded with DB charset.
        // xmlTypeEncoding = "UTF-8";

        // schema metadata
        supportsNullTableForGetColumns = false;

        // TODO: Should we include VIEW in the tableTypes?
        // tableTypes = "TABLE,VIEW";

        // auto-increment
        supportsAutoAssign = true;
        autoAssignClause = "DEFAULT AUTOINCREMENT";
        lastGeneratedKeyQuery = "SELECT @@IDENTITY";

        // SQLA does support sequences (since version 12); doesn't appear to be needed if auto-increment is available.

        nextSequenceQuery = "SELECT {0}.NEXTVAL";
        sequenceSQL = "SELECT USER_NAME AS SEQUENCE_SCHEMA, SEQUENCE_NAME "
                      +"FROM SYS.SYSSEQUENCE S JOIN SYS.SYSUSER U ON S.owner = U.user_id";

        //batchLimit = UNLIMITED;
        //reportsSuccessNoInfoOnBatchUpdates = true;
    }

    @Override
    protected String getSequencesSQL(String schemaName, String sequenceName) {
        return getSequencesSQL(DBIdentifier.newSchema(schemaName), DBIdentifier.newSequence(sequenceName));
    }

    @Override
    protected String getSequencesSQL(DBIdentifier schemaName, DBIdentifier sequenceName) {
        StringBuilder buf = new StringBuilder();
        buf.append( "SELECT USER_NAME AS SEQUENCE_SCHEMA, SEQUENCE_NAME " )
           .append( "FROM SYS.SYSSEQUENCE S JOIN SYS.SYSUSER U ON S.owner = U.user_id");

        if (!DBIdentifier.isNull(schemaName) || !DBIdentifier.isNull(sequenceName))
            buf.append(" WHERE ");
        if (!DBIdentifier.isNull(schemaName)) {
            buf.append("USER_NAME = ?");
            if (!DBIdentifier.isNull(sequenceName))
                buf.append(" AND ");
        }
        if (!DBIdentifier.isNull(sequenceName))
            buf.append("SEQUENCE_NAME = ?");
        return buf.toString();
    }

    @Override
    protected void appendSelectRange(SQLBuffer buf, long start, long end, boolean subselect) {
        if(end == Long.MAX_VALUE && start == 0) {
            // No range.
            return;
        }
        if(end != Long.MAX_VALUE) {
            buf.append(" TOP ").appendValue(end - start);
        } else {
            buf.append(" TOP ALL ");    // not supported by IQ.
        }

        if(start != 0)
            buf.append(" START AT ").appendValue(start+1); // START AT is 1 based, OFFSET is 0 based.
    }

    @Override
    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find, FilterValue start) {
        buf.append("LOCATE(");
        str.appendTo(buf);
        buf.append(", ");
        find.appendTo(buf);
        if (start != null) {
            buf.append(", ");
            start.appendTo(buf);
        }
        buf.append(")");
    }

    /* SQLA uses not-NULL as the default for BIT types. Override to NULL explicitly. */
    @Override
    protected String getDeclareColumnSQL(Column col, boolean alter) {
        String s = super.getDeclareColumnSQL(col, alter);

        if(col.getType() == Types.BIT && !col.isNotNull()) {
            StringBuilder buf = new StringBuilder();
            buf.append(s);
            buf.append(" NULL");
            s = buf.toString();
        }
        return s;
    }

    @Override
    /* SQLA does not allow catalog names on input to meta-data calls. */
    protected String getCatalogNameForMetadata(DBIdentifier catalogName) {
        return null;
    }

    @Override
    /* SQLA does not allow catalog names on input to meta-data calls. */
    protected String getColumnNameForMetadata(String columnName) {
        return null;
    }

    /* There is an issue with table names that are reserved words. This does not appear to
       be handled by the framework -- these names are not delimited. In the tests, there
       is a table named Message which is reserved in SQLA. Attempts to fix this by
       merely adjusting the naming rules did not succeed and might require broader
       framework changes.
     */
    @Override
    protected void configureNamingRules() {
        super.configureNamingRules();
    }

    @Override
    public void connectedConfiguration(Connection conn)
        throws SQLException {

        super.connectedConfiguration(conn);
    }

    @Override
    protected void setStatementQueryTimeout(PreparedStatement stmnt, int timeout) throws SQLException {
        int timeout_sec = timeout/1000;
        boolean different = (stmnt.getQueryTimeout() != timeout_sec);

        super.setStatementQueryTimeout( stmnt, timeout );
        if(different) {
            setSQLAnywhereOption(stmnt.getConnection(), "request_timeout", String.valueOf(timeout_sec));
        }
    }

    /* Set a temporary option for the current connection
     */
    private void setSQLAnywhereOption(Connection conn, String optName, String value) throws SQLException {
        StringBuilder buf = new StringBuilder();
        PreparedStatement stmnt = null;

        buf.append("SET TEMPORARY OPTION ");
        buf.append(optName);
        buf.append(" = ");
        if(value != null) {
            buf.append(value);
        }

        try {
            stmnt = conn.prepareStatement(buf.toString());
            stmnt.execute();
        } catch (Exception e) {
            if (log.isTraceEnabled())
                log.trace(e.toString(), e);
        } finally {
            if (stmnt != null)
                try {
                    stmnt.close();
                } catch (SQLException se) {
                }
        }
    }

    @Override
    protected int matchErrorState(Map<Integer,Set<String>> errorStates, SQLException ex) {
        int state = super.matchErrorState(errorStates, ex);

        if (state == StoreException.GENERAL ) {
            switch( ex.getErrorCode() ) {
            case -299:  // INTERRUPTED
            case -1043: // REQUEST_TIMEOUT
                if (conf != null && conf.getLockTimeout() != -1) {
                    state = StoreException.LOCK;
                } else {
                    state = StoreException.QUERY;
                }
                log.trace("SQLAnywhere: mapped "+ex.getErrorCode()+" to "+state);
                break;
            case -193:  // PRIMARY_KEY_NOT_UNIQUE
            case -196:  // INDEX_NOT_UNIQUE
                state = StoreException.OBJECT_EXISTS;
                break;
            case -194: // INVALID_FOREIGN_KEY
            case -198: // PRIMARY_KEY_VALUE_REF -- restrict error
                state = StoreException.REFERENTIAL_INTEGRITY;
                break;
            }
        }
        log.trace("SQLAnywhere: matchErrorState "+ex.getSQLState()+" "+ex.getErrorCode()+" to "+state);
        return state;
    }

    @Override
    public boolean isFatalException(int subtype, SQLException ex) {
        switch( ex.getErrorCode() ) {
        case -210:      // LOCKED
            if(conf != null && (conf.getLockTimeout() != -1 || conf.getQueryTimeout() != -1)) {
                // This error is typically caused by attempting to read or write a row that is locked by
                // another user, while the database option 'blocking' is set to Off or Request_timeout is set.
                return false;
            }
            break;
        case -299:      // INTERRUPTED
        case -1043:     // REQUEST_TIMEOUT
            return false;
        }
        return super.isFatalException(subtype, ex);
    }
}

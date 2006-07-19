/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.openjpa.jdbc.schema.Sequence;

/**
 * Dictionary for IBM DB2 database.
 */
public class DB2Dictionary
    extends AbstractDB2Dictionary {

    public DB2Dictionary() {
        platform = "DB2";
        validationSQL = "SELECT DISTINCT(CURRENT TIMESTAMP) FROM "
            + "SYSIBM.SYSTABLES";
        supportsSelectEndIndex = true;

        nextSequenceQuery = "SELECT NEXTVAL FOR {0}";

        binaryTypeName = "BLOB(1M)";
        longVarbinaryTypeName = "BLOB(1M)";
        varbinaryTypeName = "BLOB(1M)";
        clobTypeName = "CLOB(1M)";
        longVarcharTypeName = "LONG VARCHAR";

        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "LONG VARCHAR FOR BIT DATA", "LONG VARCHAR", "LONG VARGRAPHIC",
        }));

        maxConstraintNameLength = 18;
        maxIndexNameLength = 18;
        maxColumnNameLength = 30;
        supportsDeferredConstraints = false;
        supportsDefaultDeleteAction = false;
        supportsAlterTableWithDropColumn = false;

        supportsNullTableForGetColumns = false;

        reservedWordSet.addAll(Arrays.asList(new String[]{
            "AFTER", "ALIAS", "ALLOW", "APPLICATION", "ASSOCIATE", "ASUTIME",
            "AUDIT", "AUX", "AUXILIARY", "BEFORE", "BINARY", "BUFFERPOOL",
            "CACHE", "CALL", "CALLED", "CAPTURE", "CARDINALITY", "CCSID",
            "CLUSTER", "COLLECTION", "COLLID", "COMMENT", "CONCAT",
            "CONDITION", "CONTAINS", "COUNT_BIG", "CURRENT_LC_CTYPE",
            "CURRENT_PATH", "CURRENT_SERVER", "CURRENT_TIMEZONE", "CYCLE",
            "DATA", "DATABASE", "DAYS", "DB2GENERAL", "DB2GENRL", "DB2SQL",
            "DBINFO", "DEFAULTS", "DEFINITION", "DETERMINISTIC", "DISALLOW",
            "DO", "DSNHATTR", "DSSIZE", "DYNAMIC", "EACH", "EDITPROC", "ELSEIF",
            "ENCODING", "END-EXEC1", "ERASE", "EXCLUDING", "EXIT", "FENCED",
            "FIELDPROC", "FILE", "FINAL", "FREE", "FUNCTION", "GENERAL",
            "GENERATED", "GRAPHIC", "HANDLER", "HOLD", "HOURS", "IF",
            "INCLUDING", "INCREMENT", "INDEX", "INHERIT", "INOUT", "INTEGRITY",
            "ISOBID", "ITERATE", "JAR", "JAVA", "LABEL", "LC_CTYPE", "LEAVE",
            "LINKTYPE", "LOCALE", "LOCATOR", "LOCATORS", "LOCK", "LOCKMAX",
            "LOCKSIZE", "LONG", "LOOP", "MAXVALUE", "MICROSECOND",
            "MICROSECONDS", "MINUTES", "MINVALUE", "MODE", "MODIFIES", "MONTHS",
            "NEW", "NEW_TABLE", "NOCACHE", "NOCYCLE", "NODENAME", "NODENUMBER",
            "NOMAXVALUE", "NOMINVALUE", "NOORDER", "NULLS", "NUMPARTS", "OBID",
            "OLD", "OLD_TABLE", "OPTIMIZATION", "OPTIMIZE", "OUT", "OVERRIDING",
            "PACKAGE", "PARAMETER", "PART", "PARTITION", "PATH", "PIECESIZE",
            "PLAN", "PRIQTY", "PROGRAM", "PSID", "QUERYNO", "READS", "RECOVERY",
            "REFERENCING", "RELEASE", "RENAME", "REPEAT", "RESET", "RESIGNAL",
            "RESTART", "RESULT", "RESULT_SET_LOCATOR", "RETURN", "RETURNS",
            "ROUTINE", "ROW", "RRN", "RUN", "SAVEPOINT", "SCRATCHPAD",
            "SECONDS", "SECQTY", "SECURITY", "SENSITIVE", "SIGNAL", "SIMPLE",
            "SOURCE", "SPECIFIC", "SQLID", "STANDARD", "START", "STATIC",
            "STAY", "STOGROUP", "STORES", "STYLE", "SUBPAGES", "SYNONYM",
            "SYSFUN", "SYSIBM", "SYSPROC", "SYSTEM", "TABLESPACE", "TRIGGER",
            "TYPE", "UNDO", "UNTIL", "VALIDPROC", "VARIABLE", "VARIANT", "VCAT",
            "VOLUMES", "WHILE", "WLM", "YEARS",
        }));
    }

    public boolean supportsRandomAccessResultSet(Select sel,
        boolean forUpdate) {
        return !forUpdate
            && super.supportsRandomAccessResultSet(sel, forUpdate);
    }

    protected void appendSelectRange(SQLBuffer buf, long start, long end) {
        // appends the literal range string, since DB2 is unable to handle
        // a bound parameter for it
        buf.append(" FETCH FIRST ").append(Long.toString(end)).
            append(" ROWS ONLY");
    }

    public String[] getCreateSequenceSQL(Sequence seq) {
        String[] sql = super.getCreateSequenceSQL(seq);
        if (seq.getAllocate() > 1)
            sql[0] += " CACHE " + seq.getAllocate();
        return sql;
    }

    protected String getSequencesSQL(String schemaName, String sequenceName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT SEQSCHEMA AS SEQUENCE_SCHEMA, ").
            append("SEQNAME AS SEQUENCE_NAME FROM SYSCAT.SEQUENCES");
        if (schemaName != null || sequenceName != null)
            buf.append(" WHERE ");
        if (schemaName != null) {
            buf.append("SEQSCHEMA = ?");
            if (sequenceName != null)
                buf.append(" AND ");
        }
        if (sequenceName != null)
            buf.append("SEQNAME = ?");
        return buf.toString();
    }

    public Connection decorate(Connection conn)
        throws SQLException {
        // some versions of the DB2 driver seem to default to
        // READ_UNCOMMITTED, which will prevent locking from working
        // (multiple SELECT ... FOR UPDATE statements are allowed on
        // the same instance); if we have not overridden the
        // transaction isolation in the configuration, default to
        // TRANSACTION_READ_COMMITTED
        conn = super.decorate(conn);

        if (conf.getTransactionIsolationConstant() == -1
            && conn.getTransactionIsolation() < conn.TRANSACTION_READ_COMMITTED)
            conn.setTransactionIsolation(conn.TRANSACTION_READ_COMMITTED);

        return conn;
    }
}

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
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.schema.Sequence;

/**
 * Dictionary for IBM DB2 database.
 */
public class DB2Dictionary
    extends AbstractDB2Dictionary {
    // variables to support optimize clause
    public String optimizeClause = "optimize for";
    public String rowClause = "row";
    public DB2Dictionary() {
        platform = "DB2";
        validationSQL = "SELECT DISTINCT(CURRENT TIMESTAMP) FROM "
            + "SYSIBM.SYSTABLES";
        supportsSelectEndIndex = true;

        nextSequenceQuery = "VALUES NEXTVAL FOR {0}";

        sequenceSQL = "SELECT SEQSCHEMA AS SEQUENCE_SCHEMA, "
            + "SEQNAME AS SEQUENCE_NAME FROM SYSCAT.SEQUENCES";
        sequenceSchemaSQL = "SEQSCHEMA = ?";
        sequenceNameSQL = "SEQNAME = ?";
        characterColumnSize = 254;

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
        requiresCastForMathFunctions = true;
        requiresCastForComparisons = true;

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
        buf.append(sequenceSQL);
        if (schemaName != null || sequenceName != null)
            buf.append(" WHERE ");
        if (schemaName != null) {
            buf.append(sequenceSchemaSQL);
            if (sequenceName != null)
                buf.append(" AND ");
        }
        if (sequenceName != null)
            buf.append(sequenceNameSQL);
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

    private boolean isJDBC3(DatabaseMetaData meta) {
        try {
            // JDBC3-only method, so it might throw a AbstractMethodError
            return meta.getJDBCMajorVersion() >= 3;
        } catch (Throwable t) {
            return false;
        }
    }

    public void connectedConfiguration(Connection conn) throws SQLException {
    	super.connectedConfiguration(conn);

    	DatabaseMetaData metaData = conn.getMetaData();
    	if (isJDBC3(metaData)) {
			int maj = metaData.getDatabaseMajorVersion();
	    	int min = metaData.getDatabaseMinorVersion();

	    	if (maj >= 9 || (maj == 8 && min >= 2)) {
	    		supportsLockingWithMultipleTables = true;
	    		supportsLockingWithInnerJoin = true;
	    		supportsLockingWithOuterJoin = true;
	    		forUpdateClause = "WITH RR USE AND KEEP UPDATE LOCKS";
	    	}

            if (metaData.getDatabaseProductVersion().indexOf("DSN") != -1) {
                // DB2 Z/OS
                characterColumnSize = 255;
                lastGeneratedKeyQuery = "SELECT IDENTITY_VAL_LOCAL() FROM "
                    + "SYSIBM.SYSDUMMY1";
                nextSequenceQuery = "SELECT NEXTVAL FOR {0} FROM "
                    + "SYSIBM.SYSDUMMY1";
                sequenceSQL = "SELECT SCHEMA AS SEQUENCE_SCHEMA, "
                    + "NAME AS SEQUENCE_NAME FROM SYSIBM.SYSSEQUENCES";
                sequenceSchemaSQL = "SCHEMA = ?";
                sequenceNameSQL = "NAME = ?";
                if (maj == 8) {
                    // DB2 Z/OS Version 8: no bigint support, hence map Java
                    // long to decimal
                    bigintTypeName = "DECIMAL(31,0)";
                }
            }
        }
    }
    
    /** Based on the expectedResultCount of the select create the optimize
     *  for clause
     */ 
    public String getOptimizeClause(JDBCFetchConfiguration fetch, 
            int expectedResultCount) {
        Integer rows = null;
        StringBuffer optimizeString = new StringBuffer();
        if (expectedResultCount != 0)
            optimizeString.append(" ").append(optimizeClause).append(" ")
            .append(expectedResultCount).append(" ")
            .append(rowClause).append(" ");
        return optimizeString.toString();    
    }

    /** Override the DBDictionary toSelect to call getOptimizeClause and append 
    *   to the select string
    */   
    public SQLBuffer toSelect(SQLBuffer selects, JDBCFetchConfiguration fetch,
            SQLBuffer from, SQLBuffer where, SQLBuffer group,
            SQLBuffer having, SQLBuffer order,
            boolean distinct, boolean forUpdate, long start, long end,
            int expectedResultCount) {
        String optimizeString = null;
        SQLBuffer selString = toOperation(getSelectOperation(fetch), 
                selects, from, where,
                group, having, order, distinct,
                forUpdate, start, end);
        if (fetch != null)
            optimizeString = getOptimizeClause(fetch, expectedResultCount);
        if (optimizeString != null && optimizeString.length() > 0)
            selString.append(optimizeString);
        return selString;
    }
    
    /** Override the DBDictionary toSelect to pass expectedResultcount to the 
     * other toSelect method
     */
    public SQLBuffer toSelect(Select sel, boolean forUpdate,
            JDBCFetchConfiguration fetch) {
        sel.addJoinClassConditions();
        boolean update = forUpdate && sel.getFromSelect() == null;
        SQLBuffer select = getSelects(sel, false, update);
        SQLBuffer ordering = null;
        if (!sel.isAggregate() || sel.getGrouping() != null)
            ordering = sel.getOrdering();
        SQLBuffer from;
        if (sel.getFromSelect() != null)
            from = getFromSelect(sel, forUpdate);
        else
            from = getFrom(sel, update);
        SQLBuffer where = getWhere(sel, update);
        return toSelect(select, fetch, from, where, sel.getGrouping(),
                sel.getHaving(), ordering, sel.isDistinct(), forUpdate,
                sel.getStartIndex(), 
                sel.getEndIndex(),sel.getExpectedResultCount());
    }
}

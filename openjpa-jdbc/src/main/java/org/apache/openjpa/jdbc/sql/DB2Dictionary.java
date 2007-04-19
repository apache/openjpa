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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.util.OpenJPAException;

/**
 * Dictionary for IBM DB2 database.
 */
public class DB2Dictionary
    extends AbstractDB2Dictionary {

    public String optimizeClause = "optimize for";
    public String rowClause = "row";
    private int db2ServerType = 0;
    private static final int db2ISeriesV5R3AndEarlier = 1;
    private static final int db2UDBV81OrEarlier = 2;
    private static final int db2ZOSV8xOrLater = 3;
    private static final int db2UDBV82AndLater = 4;
    private static final int db2ISeriesV5R4AndLater = 5;
	private static final String forUpdateOfClause = "FOR UPDATE OF";
    private static final String withRSClause = "WITH RS";
    private static final String withRRClause = "WITH RR";
    private static final String useKeepUpdateLockClause
        = "USE AND KEEP UPDATE LOCKS";
    private static final String useKeepExclusiveLockClause
        = "USE AND KEEP EXCLUSIVE LOCKS";
    private static final String forReadOnlyClause = "FOR READ ONLY";

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

	    	// Determine the type of DB2 database
	    	if (isDB2ISeriesV5R3AndEarlier(metaData))
	    	    db2ServerType = db2ISeriesV5R3AndEarlier;
	    	else if (isDB2UDBV81OrEarlier(metaData,maj,min))
	    	    db2ServerType = db2UDBV81OrEarlier;
	    	else if (isDB2ZOSV8xOrLater(metaData,maj))
	    	    db2ServerType = db2ZOSV8xOrLater;
	    	else if (isDB2UDBV82AndLater(metaData,maj,min))
	    	    db2ServerType = db2UDBV82AndLater;
	    	else if (isDB2ISeriesV5R4AndLater(metaData))
	    	    db2ServerType = db2ISeriesV5R4AndLater;

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

    /**
     * Get the update clause for the query based on the
     * updateClause and isolationLevel hints
     */
    protected String getForUpdateClause(JDBCFetchConfiguration fetch,
        boolean forUpdate) {
        int isolationLevel;
        StringBuffer forUpdateString = new StringBuffer();
        try {
            // Determine the isolationLevel; the fetch
            // configuration data overrides the persistence.xml value
            if (fetch != null && fetch.getIsolation() != -1)
                isolationLevel = fetch.getIsolation();
            else
                isolationLevel = conf.getTransactionIsolationConstant();

            if (forUpdate) {
                switch(db2ServerType) {
                case db2ISeriesV5R3AndEarlier:
                case db2UDBV81OrEarlier:
                    if (isolationLevel ==
                        Connection.TRANSACTION_READ_UNCOMMITTED) {
                        forUpdateString.append(" ").append(withRSClause)
                            .append(" ").append(forUpdateOfClause);
                    } else
                        forUpdateString.append(" ").append(forUpdateOfClause);
                    break;
                case db2ZOSV8xOrLater:
                case db2UDBV82AndLater:
                    if (isolationLevel == Connection.TRANSACTION_SERIALIZABLE) {
                        forUpdateString.append(" ").append(forReadOnlyClause)
                            .append(" ").append(withRRClause)
                            .append(" ").append(useKeepUpdateLockClause);   
                    } else {
                        forUpdateString.append(" ").append(forReadOnlyClause)
                            .append(" ").append(withRSClause)
                            .append(" ").append(useKeepUpdateLockClause);                            
                    }
                    break;
                case db2ISeriesV5R4AndLater:
                    if (isolationLevel == Connection.TRANSACTION_SERIALIZABLE) {
                        forUpdateString.append(" ").append(forReadOnlyClause)
                            .append(" ").append(withRRClause)
                            .append(" ").append(useKeepExclusiveLockClause);       
                    } else {
                        forUpdateString.append(" ").append(forReadOnlyClause)
                            .append(" ").append(withRSClause)
                            .append(" ").append(useKeepExclusiveLockClause);
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
            if (log.isTraceEnabled())
                log.error(e.toString(),e);
        }
        return forUpdateString.toString();
    }

    public boolean isDB2UDBV82AndLater(DatabaseMetaData metadata, int maj,
        int min) throws SQLException {
        boolean match = false;
        if (metadata.getDatabaseProductVersion().indexOf("SQL") != -1
            && ((maj == 8 && min >= 2) ||(maj >= 8)))
            match = true;
        return match;
    }

    public boolean isDB2ZOSV8xOrLater(DatabaseMetaData metadata, int maj)
       throws SQLException {
       boolean match = false;
       if (metadata.getDatabaseProductVersion().indexOf("DSN") != -1
           && maj >= 8)
           match = true;
        return match;
    }

    public boolean isDB2ISeriesV5R3AndEarlier(DatabaseMetaData metadata)
       throws SQLException {
       boolean match = false;
       if (metadata.getDatabaseProductVersion().indexOf("AS") != -1
           && generateVersionNumber(metadata.getDatabaseProductVersion())
           <= 530)
           match = true;
       return match;
    }

    public boolean isDB2ISeriesV5R4AndLater(DatabaseMetaData metadata)
       throws SQLException {
       boolean match = false;
       if (metadata.getDatabaseProductVersion().indexOf("AS") != -1
           && generateVersionNumber(metadata.getDatabaseProductVersion())
           >= 540)
           match = true;
      return match;
    }

    public boolean isDB2UDBV81OrEarlier(DatabaseMetaData metadata, int maj,
        int min) throws SQLException {
        boolean match = false;
        if (metadata.getDatabaseProductVersion().indexOf("SQL") != -1 &&
           ((maj == 8 && min <= 1)|| maj < 8))
            match = true;
        return match;
    }

    /** Get the version number for the ISeries
     */
    protected  int generateVersionNumber(String versionString) {
        String s = versionString.substring(versionString.indexOf('V'));
        s = s.toUpperCase();
        int i = -1;
        StringTokenizer stringtokenizer = new StringTokenizer(s, "VRM", false);
        if (stringtokenizer.countTokens() == 3)
        {
            String s1 = stringtokenizer.nextToken();
            s1 = s1 + stringtokenizer.nextToken();
            s1 = s1 + stringtokenizer.nextToken();
            i = Integer.parseInt(s1);
        }
        return i;
    }

    public SQLBuffer toSelect(Select sel, boolean forUpdate,
        JDBCFetchConfiguration fetch) {
        SQLBuffer buf = super.toSelect(sel, forUpdate, fetch);

        if (sel.getExpectedResultCount() > 0) {
            buf.append(" ").append(optimizeClause).append(" ")
                .append(String.valueOf(sel.getExpectedResultCount()))
                .append(" ").append(rowClause);
        }

        return buf;
    }

    public OpenJPAException newStoreException(String msg, SQLException[] causes,
        Object failed) {
        if (causes != null && causes.length > 0)
            msg = appendExtendedExceptionMsg(msg, causes[0]);
        return super.newStoreException(msg, causes, failed);
    }

    /**
     *  Append exception information from SQLCA to the exsisting
     *  exception meassage
     */
    private String appendExtendedExceptionMsg(String msg, SQLException sqle){
       final String GETSQLCA ="getSqlca";
       String exceptionMsg = new String();
       try {
            Method sqlcaM2 = sqle.getNextException().getClass()
                             .getMethod(GETSQLCA,null);
            Object sqlca = sqlcaM2.invoke(sqle.getNextException(),
                                          new Object[] {});
            Method  getSqlErrpMethd = sqlca.getClass().
            getMethod("getSqlErrp", null);
            Method  getSqlWarnMethd = sqlca.getClass().
            getMethod("getSqlWarn", null);
            Method  getSqlErrdMethd = sqlca.getClass().
            getMethod("getSqlErrd", null);
            exceptionMsg = exceptionMsg.concat( "SQLCA OUTPUT" +
                    "[Errp=" +getSqlErrpMethd.invoke(sqlca,new Object[]{})
                    + ", Errd=" + Arrays.toString((int[])
                            (getSqlErrdMethd.invoke(sqlca, new Object[]{}))));
            String Warn = new String((char[])getSqlWarnMethd.
                    invoke(sqlca, new Object[]{}));
            if(Warn.trim().length() != 0)
                exceptionMsg = exceptionMsg.concat(", Warn=" +Warn + "]" );
            else
                exceptionMsg = exceptionMsg.concat( "]" );
            msg = msg.concat(exceptionMsg);
            return msg;
        } catch (Throwable t) {
            return sqle.getMessage();
        }
    }

    public int getDb2ServerType() {
        return db2ServerType;
    }
}

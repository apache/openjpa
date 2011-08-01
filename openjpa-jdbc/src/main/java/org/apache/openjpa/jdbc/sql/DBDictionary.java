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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.ExpContext;
import org.apache.openjpa.jdbc.kernel.exps.ExpState;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.kernel.exps.Null;
import org.apache.openjpa.jdbc.kernel.exps.Val;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.DataSourceFactory;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.NameSet;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.jdbc.ConnectionDecorator;
import org.apache.openjpa.lib.jdbc.LoggingConnectionDecorator;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Localizer.Message;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.LockException;
import org.apache.openjpa.util.ObjectExistsException;
import org.apache.openjpa.util.ObjectNotFoundException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.OptimisticException;
import org.apache.openjpa.util.ReferentialIntegrityException;
import org.apache.openjpa.util.Serialization;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;

import serp.util.Numbers;
import serp.util.Strings;

/**
 * Class which allows the creation of SQL dynamically, in a
 * database agnostic fashion. Subclass for the nuances of different data stores.
 */
public class DBDictionary
    implements Configurable, ConnectionDecorator, JoinSyntaxes,
    LoggingConnectionDecorator.SQLWarningHandler {

    public static final String VENDOR_OTHER = "other";
    public static final String VENDOR_DATADIRECT = "datadirect";

    public static final String SCHEMA_CASE_UPPER = "upper";
    public static final String SCHEMA_CASE_LOWER = "lower";
    public static final String SCHEMA_CASE_PRESERVE = "preserve";

    public static final String CONS_NAME_BEFORE = "before";
    public static final String CONS_NAME_MID = "mid";
    public static final String CONS_NAME_AFTER = "after";
    
    public int blobBufferSize = 50000;
    public int clobBufferSize = 50000;

    protected static final int RANGE_POST_SELECT = 0;
    protected static final int RANGE_PRE_DISTINCT = 1;
    protected static final int RANGE_POST_DISTINCT = 2;
    protected static final int RANGE_POST_LOCK = 3;

    protected static final int NANO = 1;
    protected static final int MICRO = NANO * 1000;
    protected static final int MILLI = MICRO * 1000;
    protected static final int CENTI = MILLI * 10;
    protected static final int DECI = MILLI * 100;
    protected static final int SEC = MILLI * 1000;

    protected static final int NAME_ANY = 0;
    protected static final int NAME_TABLE = 1;
    protected static final int NAME_SEQUENCE = 2;
    
    protected static final int UNLIMITED = -1;
    protected static final int NO_BATCH = 0;

    private static final String ZERO_DATE_STR =
        "'" + new java.sql.Date(0) + "'";
    private static final String ZERO_TIME_STR = "'" + new Time(0) + "'";
    private static final String ZERO_TIMESTAMP_STR =
        "'" + new Timestamp(0) + "'";

    private static final Localizer _loc = Localizer.forPackage
        (DBDictionary.class);

    // schema data
    public String platform = "Generic";
    public String driverVendor = null;
    public String catalogSeparator = ".";
    public boolean createPrimaryKeys = true;
    public String constraintNameMode = CONS_NAME_BEFORE;
    public int maxTableNameLength = 128;
    public int maxColumnNameLength = 128;
    public int maxConstraintNameLength = 128;
    public int maxIndexNameLength = 128;
    public int maxIndexesPerTable = Integer.MAX_VALUE;
    public boolean supportsForeignKeys = true;
    public boolean supportsUniqueConstraints = true;
    public boolean supportsDeferredConstraints = true;
    public boolean supportsRestrictDeleteAction = true;
    public boolean supportsCascadeDeleteAction = true;
    public boolean supportsNullDeleteAction = true;
    public boolean supportsDefaultDeleteAction = true;
    public boolean supportsRestrictUpdateAction = true;
    public boolean supportsCascadeUpdateAction = true;
    public boolean supportsNullUpdateAction = true;
    public boolean supportsDefaultUpdateAction = true;
    public boolean supportsAlterTableWithAddColumn = true;
    public boolean supportsAlterTableWithDropColumn = true;
    public boolean supportsComments = false;
    public String reservedWords = null;
    public String systemSchemas = null;
    public String systemTables = null;
    public String selectWords = null;
    public String fixedSizeTypeNames = null;
    public String schemaCase = SCHEMA_CASE_UPPER;
    public boolean setStringRightTruncationOn = true;

    // sql
    public String validationSQL = null;
    public String closePoolSQL = null;
    public String initializationSQL = null;
    public int joinSyntax = SYNTAX_SQL92;
    public String outerJoinClause = "LEFT OUTER JOIN";
    public String innerJoinClause = "INNER JOIN";
    public String crossJoinClause = "CROSS JOIN";
    public boolean requiresConditionForCrossJoin = false;
    public String forUpdateClause = "FOR UPDATE";
    public String tableForUpdateClause = null;
    public String distinctCountColumnSeparator = null;
    public boolean supportsSelectForUpdate = true;
    public boolean supportsLockingWithDistinctClause = true;
    public boolean supportsLockingWithMultipleTables = true;
    public boolean supportsLockingWithOrderClause = true;
    public boolean supportsLockingWithOuterJoin = true;
    public boolean supportsLockingWithInnerJoin = true;
    public boolean supportsLockingWithSelectRange = true;
    public boolean supportsQueryTimeout = true;
    public boolean simulateLocking = false;
    public boolean supportsSubselect = true;
    public boolean supportsCorrelatedSubselect = true;
    public boolean supportsHaving = true;
    public boolean supportsSelectStartIndex = false;
    public boolean supportsSelectEndIndex = false;
    public int rangePosition = RANGE_POST_SELECT;
    public boolean requiresAliasForSubselect = false;
    public boolean requiresTargetForDelete = false;
    public boolean allowsAliasInBulkClause = true;
    public boolean supportsMultipleNontransactionalResultSets = true;
    public boolean requiresSearchStringEscapeForLike = true;
    public String searchStringEscape = "\\";
    public boolean requiresCastForMathFunctions = false;
    public boolean requiresCastForComparisons = false;
    public boolean supportsModOperator = false;
    public boolean supportsXMLColumn = false;
    public boolean reportsSuccessNoInfoOnBatchUpdates = false;
    public boolean supportsCaseConversionForLob = false;
    public boolean useWildCardForCount = false;
    
    /**
     * Some Databases append whitespace after the schema name 
     */
    public boolean trimSchemaName = false;

    // functions
    public String castFunction = "CAST({0} AS {1})";
    public String toLowerCaseFunction = "LOWER({0})";
    public String toUpperCaseFunction = "UPPER({0})";
    public String stringLengthFunction = "CHAR_LENGTH({0})";
    public String bitLengthFunction = "(OCTET_LENGTH({0}) * 8)";
    public String trimLeadingFunction = "TRIM(LEADING {1} FROM {0})";
    public String trimTrailingFunction = "TRIM(TRAILING {1} FROM {0})";
    public String trimBothFunction = "TRIM(BOTH {1} FROM {0})";
    public String concatenateFunction = "({0}||{1})";
    public String concatenateDelimiter = "'OPENJPATOKEN'";
    public String substringFunctionName = "SUBSTRING";
    public String currentDateFunction = "CURRENT_DATE";
    public String currentTimeFunction = "CURRENT_TIME";
    public String currentTimestampFunction = "CURRENT_TIMESTAMP";
    public String dropTableSQL = "DROP TABLE {0}";

    // types
    public boolean storageLimitationsFatal = false;
    public boolean storeLargeNumbersAsStrings = false;
    public boolean storeCharsAsNumbers = true;
    public boolean useGetBytesForBlobs = false;
    public boolean useSetBytesForBlobs = false;
    public boolean useGetObjectForBlobs = false;
    public boolean useGetStringForClobs = false;
    public boolean useSetStringForClobs = false;
    public int maxEmbeddedBlobSize = -1;
    public int maxEmbeddedClobSize = -1;
    public int inClauseLimit = -1;
    public int datePrecision = MILLI;
    public int characterColumnSize = 255;
    public String arrayTypeName = "ARRAY";
    public String bigintTypeName = "BIGINT";
    public String binaryTypeName = "BINARY";
    public String bitTypeName = "BIT";
    public String blobTypeName = "BLOB";
    public String booleanTypeName = "BOOLEAN";
    public String charTypeName = "CHAR";
    public String clobTypeName = "CLOB";
    public String dateTypeName = "DATE";
    public String decimalTypeName = "DECIMAL";
    public String distinctTypeName = "DISTINCT";
    public String doubleTypeName = "DOUBLE";
    public String floatTypeName = "FLOAT";
    public String integerTypeName = "INTEGER";
    public String javaObjectTypeName = "JAVA_OBJECT";
    public String longVarbinaryTypeName = "LONGVARBINARY";
    public String longVarcharTypeName = "LONGVARCHAR";
    public String nullTypeName = "NULL";
    public String numericTypeName = "NUMERIC";
    public String otherTypeName = "OTHER";
    public String realTypeName = "REAL";
    public String refTypeName = "REF";
    public String smallintTypeName = "SMALLINT";
    public String structTypeName = "STRUCT";
    public String timeTypeName = "TIME";
    public String timestampTypeName = "TIMESTAMP";
    public String tinyintTypeName = "TINYINT";
    public String varbinaryTypeName = "VARBINARY";
    public String varcharTypeName = "VARCHAR";
    public String xmlTypeName = "XML";
    public String getStringVal = "";

    // schema metadata
    public boolean useSchemaName = true;
    public String tableTypes = "TABLE";
    public boolean supportsSchemaForGetTables = true;
    public boolean supportsSchemaForGetColumns = true;
    public boolean supportsNullTableForGetColumns = true;
    public boolean supportsNullTableForGetPrimaryKeys = false;
    public boolean supportsNullTableForGetIndexInfo = false;
    public boolean supportsNullTableForGetImportedKeys = false;
    public boolean useGetBestRowIdentifierForPrimaryKeys = false;
    public boolean requiresAutoCommitForMetaData = false;

    // auto-increment
    public int maxAutoAssignNameLength = 31;
    public String autoAssignClause = null;
    public String autoAssignTypeName = null;
    public boolean supportsAutoAssign = false;
    public String lastGeneratedKeyQuery = null;
    public String nextSequenceQuery = null;
    public String sequenceSQL = null;
    public String sequenceSchemaSQL = null;
    public String sequenceNameSQL = null;
    // most native sequences can be run inside the business transaction
    public int nativeSequenceType= Seq.TYPE_TRANSACTIONAL;
    public boolean useNativeSequenceCache = true;

	protected JDBCConfiguration conf = null;
    protected Log log = null;
    protected boolean connected = false;
    protected boolean isJDBC3 = false;
    protected final Set reservedWordSet = new HashSet();
    protected final Set systemSchemaSet = new HashSet();
    protected final Set systemTableSet = new HashSet();
    protected final Set fixedSizeTypeNameSet = new HashSet();
    protected final Set typeModifierSet = new HashSet();
    
    /**
     * Some JDBC drivers - ie DB2 type 2 on Z/OS throw exceptions on
     * setQueryTimeout when provided specific input values.
     * To remain consistent with earlier versions of the driver we should ignore
     * the exception.
     * 
     * This variable will be removed in future releases when we can handle the
     * exception properly.
     */ 
    @Deprecated
    public boolean ignoreSQLExceptionOnSetQueryTimeout = false; 

    /**
     * If a native query begins with any of the values found here then it will
     * be treated as a select statement.  
     */
    protected final Set selectWordSet = new HashSet();

    // when we store values that lose precion, track the types so that the
    // first time it happens we can warn the user
    private Set _precisionWarnedTypes = null;

    // cache lob methods
    private Method _setBytes = null;
    private Method _setString = null;
    private Method _setCharStream = null;

    // batchLimit value:
    // -1 = unlimited
    // 0  = no batch
    // any positive number = batch limit
    public int batchLimit = NO_BATCH;
    
    public final Map<Integer,Set<String>> sqlStateCodes = 
    	new HashMap<Integer, Set<String>>();
                                              
    public DBDictionary() {
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "BIGINT", "BIT", "BLOB", "CLOB", "DATE", "DECIMAL", "DISTINCT",
            "DOUBLE", "FLOAT", "INTEGER", "JAVA_OBJECT", "NULL", "NUMERIC",
            "OTHER", "REAL", "REF", "SMALLINT", "STRUCT", "TIME", "TIMESTAMP",
            "TINYINT",
        }));
        
        selectWordSet.add("SELECT");
    }

    /**
     * This method is called when the dictionary first sees any connection.
     * It is used to initialize dictionary metadata if needed. If you
     * override this method, be sure to call
     * <code>super.connectedConfiguration</code>.
     */
    public void connectedConfiguration(Connection conn)
        throws SQLException {
        if (!connected) {
            DatabaseMetaData metaData = null;
            try {
                metaData = conn.getMetaData();
                try {
                    // JDBC3-only method, so it might throw a 
                    // AbstractMethodError
                    isJDBC3 = metaData.getJDBCMajorVersion() >= 3;
                } catch (Throwable t) {
                    // ignore if not JDBC3
                }
            } catch (Exception e) {
                if (log.isTraceEnabled())
                    log.trace(e.toString(), e);
            }

            if (log.isTraceEnabled()) {                    
                log.trace(DBDictionaryFactory.toString(metaData));

                if (isJDBC3)
                    log.trace(_loc.get("connection-defaults", new Object[]{
                        conn.getAutoCommit(), conn.getHoldability(),
                        conn.getTransactionIsolation()}));
            }
        }
        connected = true;
    }

    //////////////////////
    // ResultSet wrappers
    //////////////////////

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Array getArray(ResultSet rs, int column)
        throws SQLException {
        return rs.getArray(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public InputStream getAsciiStream(ResultSet rs, int column)
        throws SQLException {
        return rs.getAsciiStream(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public BigDecimal getBigDecimal(ResultSet rs, int column)
        throws SQLException {
        if (storeLargeNumbersAsStrings) {
            String str = getString(rs, column);
            return (str == null) ? null : new BigDecimal(str);
        }
        return rs.getBigDecimal(column);
    }

    /**
     * Returns the specified column value as an unknown numeric type;
     * we try from the most generic to the least generic.
     */
    public Number getNumber(ResultSet rs, int column)
        throws SQLException {
        // try from the most generic, and if errors occur, try
        // less generic types; this enables us to handle values
        // like Double.NaN without having to introspect on the
        // ResultSetMetaData (bug #1053). note that we handle
        // generic exceptions, since some drivers may throw
        // NumberFormatExceptions, whereas others may throw SQLExceptions
        try {
            return getBigDecimal(rs, column);
        } catch (Exception e1) {
            try {
                return new Double(getDouble(rs, column));
            } catch (Exception e2) {
                try {
                    return new Float(getFloat(rs, column));
                } catch (Exception e3) {
                    try {
                        return Numbers.valueOf(getLong(rs, column));
                    } catch (Exception e4) {
                        try {
                            return Numbers.valueOf(getInt(rs, column));
                        } catch (Exception e5) {
                        }
                    }
                }
            }

            if (e1 instanceof RuntimeException)
                throw(RuntimeException) e1;
            if (e1 instanceof SQLException)
                throw(SQLException) e1;
        }

        return null;
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public BigInteger getBigInteger(ResultSet rs, int column)
        throws SQLException {
        if (storeLargeNumbersAsStrings) {
            String str = getString(rs, column);
            return (str == null) ? null : new BigDecimal(str).toBigInteger();
        }
        BigDecimal bd = getBigDecimal(rs, column);
        return (bd == null) ? null : bd.toBigInteger();
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public InputStream getBinaryStream(ResultSet rs, int column)
        throws SQLException {
        return rs.getBinaryStream(column);
    }

    public InputStream getLOBStream(JDBCStore store, ResultSet rs,
        int column) throws SQLException {
        return rs.getBinaryStream(column);
    }
    
    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Blob getBlob(ResultSet rs, int column)
        throws SQLException {
        return rs.getBlob(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Object getBlobObject(ResultSet rs, int column, JDBCStore store)
        throws SQLException {
        InputStream in = null;
        if (useGetBytesForBlobs || useGetObjectForBlobs) {
            byte[] bytes = getBytes(rs, column);
            if (bytes != null && bytes.length > 0)
                in = new ByteArrayInputStream(bytes);
        } else {
            Blob blob = getBlob(rs, column);
            if (blob != null && blob.length() > 0)
                in = blob.getBinaryStream();
        }
        if (in == null)
            return null;

        try {
            if (store == null)
                return Serialization.deserialize(in, null);
            return Serialization.deserialize(in, store.getContext());
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public boolean getBoolean(ResultSet rs, int column)
        throws SQLException {
        return rs.getBoolean(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public byte getByte(ResultSet rs, int column)
        throws SQLException {
        return rs.getByte(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public byte[] getBytes(ResultSet rs, int column)
        throws SQLException {
        if (useGetBytesForBlobs)
            return rs.getBytes(column);
        if (useGetObjectForBlobs)
            return (byte[]) rs.getObject(column);

        Blob blob = getBlob(rs, column);
        if (blob == null)
            return null;
        int length = (int) blob.length();
        if (length == 0)
            return null;
        return blob.getBytes(1, length);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type. Converts the date from a {@link Timestamp} by default.
     */
    public Calendar getCalendar(ResultSet rs, int column)
        throws SQLException {
        Date d = getDate(rs, column);
        if (d == null)
            return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal;
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public char getChar(ResultSet rs, int column)
        throws SQLException {
        if (storeCharsAsNumbers)
            return (char) getInt(rs, column);

        String str = getString(rs, column);
        return (StringUtils.isEmpty(str)) ? 0 : str.charAt(0);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Reader getCharacterStream(ResultSet rs, int column)
        throws SQLException {
        return rs.getCharacterStream(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Clob getClob(ResultSet rs, int column)
        throws SQLException {
        return rs.getClob(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public String getClobString(ResultSet rs, int column)
        throws SQLException {
        if (useGetStringForClobs)
            return rs.getString(column);

        Clob clob = getClob(rs, column);
        if (clob == null)
            return null;
        if (clob.length() == 0)
            return "";

        // unlikely that we'll have strings over Integer.MAX_VALUE chars
        return clob.getSubString(1, (int) clob.length());
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type. Converts the date from a {@link Timestamp} by default.
     */
    public Date getDate(ResultSet rs, int column)
        throws SQLException {
        Timestamp tstamp = getTimestamp(rs, column, null);
        if (tstamp == null)
            return null;

        // get the fractional seconds component, rounding away anything beyond
        // milliseconds
        int fractional = (int) Math.round(tstamp.getNanos() / (double) MILLI);

        // get the millis component; some JDBC drivers round this to the
        // nearest second, while others do not
        long millis = (tstamp.getTime() / 1000L) * 1000L;
        return new Date(millis + fractional);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public java.sql.Date getDate(ResultSet rs, int column, Calendar cal)
        throws SQLException {
        if (cal == null)
            return rs.getDate(column);
        return rs.getDate(column, cal);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public double getDouble(ResultSet rs, int column)
        throws SQLException {
        return rs.getDouble(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public float getFloat(ResultSet rs, int column)
        throws SQLException {
        return rs.getFloat(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public int getInt(ResultSet rs, int column)
        throws SQLException {
        return rs.getInt(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Locale getLocale(ResultSet rs, int column)
        throws SQLException {
        String str = getString(rs, column);
        if (StringUtils.isEmpty(str))
            return null;

        String[] params = Strings.split(str, "_", 3);
        if (params.length < 3)
            return null;
        return new Locale(params[0], params[1], params[2]);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public long getLong(ResultSet rs, int column)
        throws SQLException {
        return rs.getLong(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Object getObject(ResultSet rs, int column, Map map)
        throws SQLException {
        if (map == null)
            return rs.getObject(column);
        return rs.getObject(column, map);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Ref getRef(ResultSet rs, int column, Map map)
        throws SQLException {
        return rs.getRef(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public short getShort(ResultSet rs, int column)
        throws SQLException {
        return rs.getShort(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public String getString(ResultSet rs, int column)
        throws SQLException {
        return rs.getString(column);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Time getTime(ResultSet rs, int column, Calendar cal)
        throws SQLException {
        if (cal == null)
            return rs.getTime(column);
        return rs.getTime(column, cal);
    }

    /**
     * Convert the specified column of the SQL ResultSet to the proper
     * java type.
     */
    public Timestamp getTimestamp(ResultSet rs, int column, Calendar cal)
        throws SQLException {
        if (cal == null)
            return rs.getTimestamp(column);
        return rs.getTimestamp(column, cal);
    }

    //////////////////////////////
    // PreparedStatement wrappers
    //////////////////////////////

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setArray(PreparedStatement stmnt, int idx, Array val,
        Column col)
        throws SQLException {
        stmnt.setArray(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setAsciiStream(PreparedStatement stmnt, int idx,
        InputStream val, int length, Column col)
        throws SQLException {
        stmnt.setAsciiStream(idx, val, length);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setBigDecimal(PreparedStatement stmnt, int idx, BigDecimal val,
        Column col)
        throws SQLException {
        if ((col != null && col.isCompatible(Types.VARCHAR, null, 0, 0))
            || (col == null && storeLargeNumbersAsStrings))
            setString(stmnt, idx, val.toString(), col);
        else
            stmnt.setBigDecimal(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setBigInteger(PreparedStatement stmnt, int idx, BigInteger val,
        Column col)
        throws SQLException {
        if ((col != null && col.isCompatible(Types.VARCHAR, null, 0, 0))
            || (col == null && storeLargeNumbersAsStrings))
            setString(stmnt, idx, val.toString(), col);
        else
            setBigDecimal(stmnt, idx, new BigDecimal(val), col);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setBinaryStream(PreparedStatement stmnt, int idx,
        InputStream val, int length, Column col)
        throws SQLException {
        stmnt.setBinaryStream(idx, val, length);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setBlob(PreparedStatement stmnt, int idx, Blob val, Column col)
        throws SQLException {
        stmnt.setBlob(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement. Uses the
     * {@link #serialize} method to serialize the value.
     */
    public void setBlobObject(PreparedStatement stmnt, int idx, Object val,
        Column col, JDBCStore store)
        throws SQLException {
        setBytes(stmnt, idx, serialize(val, store), col);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setBoolean(PreparedStatement stmnt, int idx, boolean val,
        Column col)
        throws SQLException {
        stmnt.setInt(idx, (val) ? 1 : 0);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setByte(PreparedStatement stmnt, int idx, byte val, Column col)
        throws SQLException {
        stmnt.setByte(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setBytes(PreparedStatement stmnt, int idx, byte[] val,
        Column col)
        throws SQLException {
        if (useSetBytesForBlobs)
            stmnt.setBytes(idx, val);
        else
            setBinaryStream(stmnt, idx, new ByteArrayInputStream(val),
                val.length, col);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setChar(PreparedStatement stmnt, int idx, char val, Column col)
        throws SQLException {
        if ((col != null && col.isCompatible(Types.INTEGER, null, 0, 0))
            || (col == null && storeCharsAsNumbers))
            setInt(stmnt, idx, (int) val, col);
        else
            setString(stmnt, idx, String.valueOf(val), col);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setCharacterStream(PreparedStatement stmnt, int idx,
        Reader val, int length, Column col)
        throws SQLException {
        stmnt.setCharacterStream(idx, val, length);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setClob(PreparedStatement stmnt, int idx, Clob val, Column col)
        throws SQLException {
        stmnt.setClob(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setClobString(PreparedStatement stmnt, int idx, String val,
        Column col)
        throws SQLException {
        if (useSetStringForClobs)
            stmnt.setString(idx, val);
        else {
            // set reader from string
            StringReader in = new StringReader(val);
            setCharacterStream(stmnt, idx, in, val.length(), col);
        }
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setDate(PreparedStatement stmnt, int idx, Date val, Column col)
        throws SQLException {
        if (col != null && col.getType() == Types.DATE)
            setDate(stmnt, idx, new java.sql.Date(val.getTime()), null, col);
        else if (col != null && col.getType() == Types.TIME)
            setTime(stmnt, idx, new Time(val.getTime()), null, col);
        else if (val instanceof Timestamp)
            setTimestamp(stmnt, idx,(Timestamp) val, null, col);   
        else
            setTimestamp(stmnt, idx, new Timestamp(val.getTime()), null, col);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setDate(PreparedStatement stmnt, int idx, java.sql.Date val,
        Calendar cal, Column col)
        throws SQLException {
        if (cal == null)
            stmnt.setDate(idx, val);
        else
            stmnt.setDate(idx, val, cal);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setCalendar(PreparedStatement stmnt, int idx, Calendar val,
        Column col)
        throws SQLException {
        // by default we merely delegate the the Date parameter
        setDate(stmnt, idx, val.getTime(), col);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setDouble(PreparedStatement stmnt, int idx, double val,
        Column col)
        throws SQLException {
        stmnt.setDouble(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setFloat(PreparedStatement stmnt, int idx, float val,
        Column col)
        throws SQLException {
        stmnt.setFloat(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setInt(PreparedStatement stmnt, int idx, int val, Column col)
        throws SQLException {
        stmnt.setInt(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setLong(PreparedStatement stmnt, int idx, long val, Column col)
        throws SQLException {
        stmnt.setLong(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setLocale(PreparedStatement stmnt, int idx, Locale val,
        Column col)
        throws SQLException {
        setString(stmnt, idx, val.getLanguage() + "_" + val.getCountry()
            + "_" + val.getVariant(), col);
    }

    /**
     * Set the given value as a parameters to the statement. The column
     * type will come from {@link Types}.
     */
    public void setNull(PreparedStatement stmnt, int idx, int colType,
        Column col)
        throws SQLException {
        stmnt.setNull(idx, colType);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setNumber(PreparedStatement stmnt, int idx, Number num,
        Column col)
        throws SQLException {
        // check for known floating point types to give driver a chance to
        // handle special numbers like NaN and infinity; bug #1053
        if (num instanceof Double)
            setDouble(stmnt, idx, ((Double) num).doubleValue(), col);
        else if (num instanceof Float)
            setFloat(stmnt, idx, ((Float) num).floatValue(), col);
        else
            setBigDecimal(stmnt, idx, new BigDecimal(num.toString()), col);
    }

    /**
     * Set the given value as a parameters to the statement. The column
     * type will come from {@link Types}.
     */
    public void setObject(PreparedStatement stmnt, int idx, Object val,
        int colType, Column col)
        throws SQLException {
        if (colType == -1 || colType == Types.OTHER)
            stmnt.setObject(idx, val);
        else
            stmnt.setObject(idx, val, colType);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setRef(PreparedStatement stmnt, int idx, Ref val, Column col)
        throws SQLException {
        stmnt.setRef(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setShort(PreparedStatement stmnt, int idx, short val,
        Column col)
        throws SQLException {
        stmnt.setShort(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setString(PreparedStatement stmnt, int idx, String val,
        Column col)
        throws SQLException {
        stmnt.setString(idx, val);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setTime(PreparedStatement stmnt, int idx, Time val,
        Calendar cal, Column col)
        throws SQLException {
        if (cal == null)
            stmnt.setTime(idx, val);
        else
            stmnt.setTime(idx, val, cal);
    }

    /**
     * Set the given value as a parameter to the statement.
     */
    public void setTimestamp(PreparedStatement stmnt, int idx,
        Timestamp val, Calendar cal, Column col)
        throws SQLException {
        // ensure that we do not insert dates at a greater precision than
        // that at which they will be returned by a SELECT
        int rounded = (int) Math.round(val.getNanos() /
            (double) datePrecision);
        int nanos = rounded * datePrecision;
        if (nanos > 999999999) {
            // rollover to next second
            val.setTime(val.getTime() + 1000);
            nanos = 0;
        }

        val.setNanos(nanos);

        if (cal == null)
            stmnt.setTimestamp(idx, val);
        else
            stmnt.setTimestamp(idx, val, cal);
    }

    /**
     * Set a column value into a prepared statement.
     *
     * @param stmnt the prepared statement to parameterize
     * @param idx the index of the parameter in the prepared statement
     * @param val the value of the column
     * @param col the column being set
     * @param type the field mapping type code for the value
     * @param store the store manager for the current context
     */
    public void setTyped(PreparedStatement stmnt, int idx, Object val,
        Column col, int type, JDBCStore store)
        throws SQLException {
        if (val == null) {
            setNull(stmnt, idx, (col == null) ? Types.OTHER : col.getType(),
                col);
            return;
        }

        Sized s;
        Calendard c;
        switch (type) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BOOLEAN_OBJ:
                setBoolean(stmnt, idx, ((Boolean) val).booleanValue(), col);
                break;
            case JavaTypes.BYTE:
            case JavaTypes.BYTE_OBJ:
                setByte(stmnt, idx, ((Number) val).byteValue(), col);
                break;
            case JavaTypes.CHAR:
            case JavaTypes.CHAR_OBJ:
                setChar(stmnt, idx, ((Character) val).charValue(), col);
                break;
            case JavaTypes.DOUBLE:
            case JavaTypes.DOUBLE_OBJ:
                setDouble(stmnt, idx, ((Number) val).doubleValue(), col);
                break;
            case JavaTypes.FLOAT:
            case JavaTypes.FLOAT_OBJ:
                setFloat(stmnt, idx, ((Number) val).floatValue(), col);
                break;
            case JavaTypes.INT:
            case JavaTypes.INT_OBJ:
                setInt(stmnt, idx, ((Number) val).intValue(), col);
                break;
            case JavaTypes.LONG:
            case JavaTypes.LONG_OBJ:
                setLong(stmnt, idx, ((Number) val).longValue(), col);
                break;
            case JavaTypes.SHORT:
            case JavaTypes.SHORT_OBJ:
                setShort(stmnt, idx, ((Number) val).shortValue(), col);
                break;
            case JavaTypes.STRING:
                if (col != null && (col.getType() == Types.CLOB
                    || col.getType() == Types.LONGVARCHAR))
                    setClobString(stmnt, idx, (String) val, col);
                else
                    setString(stmnt, idx, (String) val, col);
                break;
            case JavaTypes.OBJECT:
                setBlobObject(stmnt, idx, val, col, store);
                break;
            case JavaTypes.DATE:
                setDate(stmnt, idx, (Date) val, col);
                break;
            case JavaTypes.CALENDAR:
                setCalendar(stmnt, idx, (Calendar) val, col);
                break;
            case JavaTypes.BIGDECIMAL:
                setBigDecimal(stmnt, idx, (BigDecimal) val, col);
                break;
            case JavaTypes.BIGINTEGER:
                setBigInteger(stmnt, idx, (BigInteger) val, col);
                break;
            case JavaTypes.NUMBER:
                setNumber(stmnt, idx, (Number) val, col);
                break;
            case JavaTypes.LOCALE:
                setLocale(stmnt, idx, (Locale) val, col);
                break;
            case JavaSQLTypes.SQL_ARRAY:
                setArray(stmnt, idx, (Array) val, col);
                break;
            case JavaSQLTypes.ASCII_STREAM:
                s = (Sized) val;
                setAsciiStream(stmnt, idx, (InputStream) s.value, s.size, col);
                break;
            case JavaSQLTypes.BINARY_STREAM:
                s = (Sized) val;
                setBinaryStream(stmnt, idx, (InputStream) s.value, s.size, col);
                break;
            case JavaSQLTypes.BLOB:
                setBlob(stmnt, idx, (Blob) val, col);
                break;
            case JavaSQLTypes.BYTES:
                setBytes(stmnt, idx, (byte[]) val, col);
                break;
            case JavaSQLTypes.CHAR_STREAM:
                s = (Sized) val;
                setCharacterStream(stmnt, idx, (Reader) s.value, s.size, col);
                break;
            case JavaSQLTypes.CLOB:
                setClob(stmnt, idx, (Clob) val, col);
                break;
            case JavaSQLTypes.SQL_DATE:
                if (val instanceof Calendard) {
                    c = (Calendard) val;
                    setDate(stmnt, idx, (java.sql.Date) c.value, c.calendar,
                        col);
                } else
                    setDate(stmnt, idx, (java.sql.Date) val, null, col);
                break;
            case JavaSQLTypes.REF:
                setRef(stmnt, idx, (Ref) val, col);
                break;
            case JavaSQLTypes.TIME:
                if (val instanceof Calendard) {
                    c = (Calendard) val;
                    setTime(stmnt, idx, (Time) c.value, c.calendar, col);
                } else
                    setTime(stmnt, idx, (Time) val, null, col);
                break;
            case JavaSQLTypes.TIMESTAMP:
                if (val instanceof Calendard) {
                    c = (Calendard) val;
                    setTimestamp(stmnt, idx, (Timestamp) c.value, c.calendar,
                        col);
                } else
                    setTimestamp(stmnt, idx, (Timestamp) val, null, col);
                break;
            default:
                if (col != null && (col.getType() == Types.BLOB
                    || col.getType() == Types.VARBINARY))
                    setBlobObject(stmnt, idx, val, col, store);
                else
                    setObject(stmnt, idx, val, col.getType(), col);
        }
    }

    /**
     * Set a completely unknown parameter into a prepared statement.
     */
    public void setUnknown(PreparedStatement stmnt, int idx, Object val,
        Column col)
        throws SQLException {
        Sized sized = null;
        Calendard cald = null;
        if (val instanceof Sized) {
            sized = (Sized) val;
            val = sized.value;
        } else if (val instanceof Calendard) {
            cald = (Calendard) val;
            val = cald.value;
        }

        if (val == null)
            setNull(stmnt, idx, (col == null) ? Types.OTHER : col.getType(),
                col);
        else if (val instanceof String)
            setString(stmnt, idx, val.toString(), col);
        else if (val instanceof Integer)
            setInt(stmnt, idx, ((Integer) val).intValue(), col);
        else if (val instanceof Boolean)
            setBoolean(stmnt, idx, ((Boolean) val).booleanValue(), col);
        else if (val instanceof Long)
            setLong(stmnt, idx, ((Long) val).longValue(), col);
        else if (val instanceof Float)
            setFloat(stmnt, idx, ((Float) val).floatValue(), col);
        else if (val instanceof Double)
            setDouble(stmnt, idx, ((Double) val).doubleValue(), col);
        else if (val instanceof Byte)
            setByte(stmnt, idx, ((Byte) val).byteValue(), col);
        else if (val instanceof Character)
            setChar(stmnt, idx, ((Character) val).charValue(), col);
        else if (val instanceof Short)
            setShort(stmnt, idx, ((Short) val).shortValue(), col);
        else if (val instanceof Locale)
            setLocale(stmnt, idx, (Locale) val, col);
        else if (val instanceof BigDecimal)
            setBigDecimal(stmnt, idx, (BigDecimal) val, col);
        else if (val instanceof BigInteger)
            setBigInteger(stmnt, idx, (BigInteger) val, col);
        else if (val instanceof Array)
            setArray(stmnt, idx, (Array) val, col);
        else if (val instanceof Blob)
            setBlob(stmnt, idx, (Blob) val, col);
        else if (val instanceof byte[])
            setBytes(stmnt, idx, (byte[]) val, col);
        else if (val instanceof Clob)
            setClob(stmnt, idx, (Clob) val, col);
        else if (val instanceof Ref)
            setRef(stmnt, idx, (Ref) val, col);
        else if (val instanceof java.sql.Date)
            setDate(stmnt, idx, (java.sql.Date) val,
                (cald == null) ? null : cald.calendar, col);
        else if (val instanceof Timestamp)
            setTimestamp(stmnt, idx, (Timestamp) val,
                (cald == null) ? null : cald.calendar, col);
        else if (val instanceof Time)
            setTime(stmnt, idx, (Time) val,
                (cald == null) ? null : cald.calendar, col);
        else if (val instanceof Date)
            setDate(stmnt, idx, (Date) val, col);
        else if (val instanceof Calendar)
            setDate(stmnt, idx, ((Calendar) val).getTime(), col);
        else if (val instanceof Reader)
            setCharacterStream(stmnt, idx, (Reader) val,
                (sized == null) ? 0 : sized.size, col);
        else
            throw new UserException(_loc.get("bad-param", val.getClass()));
    }

    /**
     * Return the serialized bytes for the given object.
     */
    public byte[] serialize(Object val, JDBCStore store)
        throws SQLException {
        if (val == null)
            return null;
        if (val instanceof SerializedData)
            return ((SerializedData) val).bytes;
        return Serialization.serialize(val, store.getContext());
    }

    /**
     * Invoke the JDK 1.4 <code>setBytes</code> method on the given BLOB object.
     */
    public void putBytes(Object blob, byte[] data)
        throws SQLException {
        if (_setBytes == null) {
            try {
                _setBytes = blob.getClass().getMethod("setBytes",
                    new Class[]{ long.class, byte[].class });
            } catch (Exception e) {
                throw new StoreException(e);
            }
        }
        invokePutLobMethod(_setBytes, blob,
            new Object[]{ Numbers.valueOf(1L), data });
    }

    /**
     * Invoke the JDK 1.4 <code>setString</code> method on the given CLOB
     * object.
     */
    public void putString(Object clob, String data)
        throws SQLException {
        if (_setString == null) {
            try {
                _setString = clob.getClass().getMethod("setString",
                    new Class[]{ long.class, String.class });
            } catch (Exception e) {
                throw new StoreException(e);
            }
        }
        invokePutLobMethod(_setString, clob,
            new Object[]{ Numbers.valueOf(1L), data });
    }

    /**
     * Invoke the JDK 1.4 <code>setCharacterStream</code> method on the given
     * CLOB object.
     */
    public void putChars(Object clob, char[] data)
        throws SQLException {
        if (_setCharStream == null) {
            try {
                _setCharStream = clob.getClass().getMethod
                    ("setCharacterStream", new Class[]{ long.class });
            } catch (Exception e) {
                throw new StoreException(e);
            }
        }

        Writer writer = (Writer) invokePutLobMethod(_setCharStream, clob,
            new Object[]{ Numbers.valueOf(1L) });
        try {
            writer.write(data);
            writer.flush();
        } catch (IOException ioe) {
            throw new SQLException(ioe.toString());
        }
    }

    /**
     * Invoke the given LOB method on the given target with the given data.
     */
    private static Object invokePutLobMethod(Method method, Object target,
        Object[] args)
        throws SQLException {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof SQLException)
                throw(SQLException) t;
            throw new StoreException(t);
        } catch (Exception e) {
            throw new StoreException(e);
        }
    }

    /**
     * Warn that a particular value could not be stored precisely.
     * After the first warning for a particular type, messages
     * will be turned into trace messages.
     */
    protected void storageWarning(Object orig, Object converted) {
        boolean warn;
        synchronized (this) {
            if (_precisionWarnedTypes == null)
                _precisionWarnedTypes = new HashSet();
            warn = _precisionWarnedTypes.add(orig.getClass());
        }

        if (storageLimitationsFatal || (warn && log.isWarnEnabled())
            || (!warn && log.isTraceEnabled())) {
            Message msg = _loc.get("storage-restriction", new Object[]{
                platform,
                orig,
                orig.getClass().getName(),
                converted,
            });

            if (storageLimitationsFatal)
                throw new StoreException(msg);

            if (warn)
                log.warn(msg);
            else
                log.trace(msg);
        }
    }

    /////////
    // Types
    /////////

    /**
     * Return the preferred {@link Types} constant for the given
     * {@link JavaTypes} or {@link JavaSQLTypes} constant.
     */
    public int getJDBCType(int metaTypeCode, boolean lob) {
        if (lob) {
            switch (metaTypeCode) {
                case JavaTypes.STRING:
                case JavaSQLTypes.ASCII_STREAM:
                case JavaSQLTypes.CHAR_STREAM:
                    return getPreferredType(Types.CLOB);
                default:
                    return getPreferredType(Types.BLOB);
            }
        }

        switch (metaTypeCode) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BOOLEAN_OBJ:
                return getPreferredType(Types.BIT);
            case JavaTypes.BYTE:
            case JavaTypes.BYTE_OBJ:
                return getPreferredType(Types.TINYINT);
            case JavaTypes.CHAR:
            case JavaTypes.CHAR_OBJ:
                if (storeCharsAsNumbers)
                    return getPreferredType(Types.INTEGER);
                return getPreferredType(Types.CHAR);
            case JavaTypes.DOUBLE:
            case JavaTypes.DOUBLE_OBJ:
                return getPreferredType(Types.DOUBLE);
            case JavaTypes.FLOAT:
            case JavaTypes.FLOAT_OBJ:
                return getPreferredType(Types.REAL);
            case JavaTypes.INT:
            case JavaTypes.INT_OBJ:
                return getPreferredType(Types.INTEGER);
            case JavaTypes.LONG:
            case JavaTypes.LONG_OBJ:
                return getPreferredType(Types.BIGINT);
            case JavaTypes.SHORT:
            case JavaTypes.SHORT_OBJ:
                return getPreferredType(Types.SMALLINT);
            case JavaTypes.STRING:
            case JavaTypes.LOCALE:
            case JavaSQLTypes.ASCII_STREAM:
            case JavaSQLTypes.CHAR_STREAM:
                return getPreferredType(Types.VARCHAR);
            case JavaTypes.BIGINTEGER:
                if (storeLargeNumbersAsStrings)
                    return getPreferredType(Types.VARCHAR);
                return getPreferredType(Types.BIGINT);
            case JavaTypes.BIGDECIMAL:
                if (storeLargeNumbersAsStrings)
                    return getPreferredType(Types.VARCHAR);
                return getPreferredType(Types.NUMERIC);
            case JavaTypes.NUMBER:
                if (storeLargeNumbersAsStrings)
                    return getPreferredType(Types.VARCHAR);
                return getPreferredType(Types.NUMERIC);
            case JavaTypes.CALENDAR:
            case JavaTypes.DATE:
                return getPreferredType(Types.TIMESTAMP);
            case JavaSQLTypes.SQL_ARRAY:
                return getPreferredType(Types.ARRAY);
            case JavaSQLTypes.BINARY_STREAM:
            case JavaSQLTypes.BLOB:
            case JavaSQLTypes.BYTES:
                return getPreferredType(Types.BLOB);
            case JavaSQLTypes.CLOB:
                return getPreferredType(Types.CLOB);
            case JavaSQLTypes.SQL_DATE:
                return getPreferredType(Types.DATE);
            case JavaSQLTypes.TIME:
                return getPreferredType(Types.TIME);
            case JavaSQLTypes.TIMESTAMP:
                return getPreferredType(Types.TIMESTAMP);
            default:
                return getPreferredType(Types.BLOB);
        }
    }

    /**
     * Return the preferred {@link Types} type for the given one. Returns
     * the given type by default.
     */
    public int getPreferredType(int type) {
        return type;
    }

    /**
     * Return the preferred database type name for the given column's type
     * from {@link Types}.
     */
    public String getTypeName(Column col) {
        if (!StringUtils.isEmpty(col.getTypeName()))
            return appendSize(col, col.getTypeName());

        if (col.isAutoAssigned() && autoAssignTypeName != null)
            return appendSize(col, autoAssignTypeName);

        return appendSize(col, getTypeName(col.getType()));
    }

    /**
     * Returns the type name for the specific constant as defined
     * by {@link java.sql.Types}.
     *
     * @param type the type
     * @return the name for the type
     */
    public String getTypeName(int type) {
        switch (type) {
            case Types.ARRAY:
                return arrayTypeName;
            case Types.BIGINT:
                return bigintTypeName;
            case Types.BINARY:
                return binaryTypeName;
            case Types.BIT:
                return bitTypeName;
            case Types.BLOB:
                return blobTypeName;
            case Types.BOOLEAN:
                return booleanTypeName;
            case Types.CHAR:
                return charTypeName;
            case Types.CLOB:
                return clobTypeName;
            case Types.DATE:
                return dateTypeName;
            case Types.DECIMAL:
                return decimalTypeName;
            case Types.DISTINCT:
                return distinctTypeName;
            case Types.DOUBLE:
                return doubleTypeName;
            case Types.FLOAT:
                return floatTypeName;
            case Types.INTEGER:
                return integerTypeName;
            case Types.JAVA_OBJECT:
                return javaObjectTypeName;
            case Types.LONGVARBINARY:
                return longVarbinaryTypeName;
            case Types.LONGVARCHAR:
                return longVarcharTypeName;
            case Types.NULL:
                return nullTypeName;
            case Types.NUMERIC:
                return numericTypeName;
            case Types.OTHER:
                return otherTypeName;
            case Types.REAL:
                return realTypeName;
            case Types.REF:
                return refTypeName;
            case Types.SMALLINT:
                return smallintTypeName;
            case Types.STRUCT:
                return structTypeName;
            case Types.TIME:
                return timeTypeName;
            case Types.TIMESTAMP:
                return timestampTypeName;
            case Types.TINYINT:
                return tinyintTypeName;
            case Types.VARBINARY:
                return varbinaryTypeName;
            case Types.VARCHAR:
                return varcharTypeName;
            default:
                return otherTypeName;
        }
    }

    /**
     * Helper method to add size properties to the specified type.
     * If present, the string "{0}" will be replaced with the size definition;
     * otherwise the size definition will be appended to the type name.
     * If your database has column types that don't allow size definitions,
     * override this method to return the unaltered type name for columns of
     * those types (or add the type names to the
     * <code>fixedSizeTypeNameSet</code>).
     * 
     * <P>Some databases support "type modifiers" for example the unsigned
     * "modifier" in MySQL. In these cases the size should go between the type 
     * and the "modifier", instead of after the modifier. For example 
     * CREATE table FOO ( myint INT (10) UNSIGNED . . .) instead of 
     * CREATE table FOO ( myint INT UNSIGNED (10) . . .).
     * Type modifiers should be added to <code>typeModifierSet</code> in 
     * subclasses. 
     */
    protected String appendSize(Column col, String typeName) {
        if (fixedSizeTypeNameSet.contains(typeName.toUpperCase()))
            return typeName;
        if (typeName.indexOf('(') != -1)
            return typeName;

        String size = null;
        if (col.getSize() > 0) {
            StringBuffer buf = new StringBuffer(10);
            buf.append("(").append(col.getSize());
            if (col.getDecimalDigits() > 0)
                buf.append(", ").append(col.getDecimalDigits());
            buf.append(")");
            size = buf.toString();
        }

        return insertSize(typeName, size);
    }

    /**
     * Helper method that inserts a size clause for a given SQL type. 
     * 
     * @see appendSize
     * 
     * @param typeName  The SQL type ie INT
     * @param size      The size clause ie (10)
     * @return          The typeName + size clause. Usually the size clause will 
     *                  be appended to typeName. If the typeName contains a 
     *                  marker : {0} or if typeName contains a modifier the 
     *                  size clause will be inserted appropriately.   
     */
    protected String insertSize(String typeName, String size) {
    	if (StringUtils.isEmpty(size)) {
            int idx = typeName.indexOf("{0}");
            if (idx != -1) {
                return typeName.substring(0, idx);
            }
            return typeName;
        }
    	
        int idx = typeName.indexOf("{0}");
        if (idx != -1) {
            // replace '{0}' with size
            String ret = typeName.substring(0, idx);
            if (size != null)
                ret = ret + size;
            if (typeName.length() > idx + 3)
                ret = ret + typeName.substring(idx + 3);
            return ret;
        }
        if (!typeModifierSet.isEmpty()) {
            String s;
            idx = typeName.length();
            int curIdx = -1;
            for (Iterator i = typeModifierSet.iterator(); i.hasNext();) {
                s = (String) i.next();
                if (typeName.toUpperCase().indexOf(s) != -1) {
                    curIdx = typeName.toUpperCase().indexOf(s);
                    if (curIdx != -1 && curIdx < idx) {
                        idx = curIdx;
                    }
                }
            }
            if(idx != typeName.length()) {
                String ret = typeName.substring(0, idx);
                ret = ret + size;
                ret = ret + ' ' + typeName.substring(idx);
                return ret;
            }
        }
        return typeName + size;
    }

    ///////////
    // Selects
    ///////////

    /**
     * Set the name of the join syntax to use: sql92, traditional, database
     */
    public void setJoinSyntax(String syntax) {
        if ("sql92".equals(syntax))
            joinSyntax = SYNTAX_SQL92;
        else if ("traditional".equals(syntax))
            joinSyntax = SYNTAX_TRADITIONAL;
        else if ("database".equals(syntax))
            joinSyntax = SYNTAX_DATABASE;
        else if (!StringUtils.isEmpty(syntax))
            throw new IllegalArgumentException(syntax);
    }

    /**
     * Return a SQL string to act as a placeholder for the given column.
     */
    public String getPlaceholderValueString(Column col) {
        switch (col.getType()) {
            case Types.BIGINT:
            case Types.BIT:
            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.SMALLINT:
            case Types.TINYINT:
                return "0";
            case Types.CHAR:
                return (storeCharsAsNumbers) ? "0" : "' '";
            case Types.CLOB:
            case Types.LONGVARCHAR:
            case Types.VARCHAR:
                return "''";
            case Types.DATE:
                return ZERO_DATE_STR;
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                return "0.0";
            case Types.TIME:
                return ZERO_TIME_STR;
            case Types.TIMESTAMP:
                return ZERO_TIMESTAMP_STR;
            default:
                return "NULL";
        }
    }

    /**
     * Create a SELECT COUNT statement in the proper join syntax for the
     * given instance.
     */
    public SQLBuffer toSelectCount(Select sel) {
        SQLBuffer selectSQL = new SQLBuffer(this);
        SQLBuffer from;
        sel.addJoinClassConditions();
        if (sel.getFromSelect() != null)
            from = getFromSelect(sel, false);
        else
            from = getFrom(sel, false);
        SQLBuffer where = getWhere(sel, false);

        // if no grouping and no range, we might be able to get by without
        // a subselect
        if (sel.getGrouping() == null && sel.getStartIndex() == 0
            && sel.getEndIndex() == Long.MAX_VALUE) {
            // if the select has no identifier cols, use COUNT(*)
            List aliases = (!sel.isDistinct()) ? Collections.EMPTY_LIST
                : sel.getIdentifierAliases();
            if (useWildCardForCount || aliases.isEmpty()) {
                selectSQL.append("COUNT(*)");
                return toSelect(selectSQL, null, from, where, null, null, null,
                    false, false, 0, Long.MAX_VALUE);
            }

            // if there is a single distinct col, use COUNT(DISTINCT col)
            if (aliases.size() == 1) {
                selectSQL.append("COUNT(DISTINCT ").
                    append(aliases.get(0).toString()).append(")");
                return toSelect(selectSQL, null, from, where, null, null, null,
                    false, false, 0, Long.MAX_VALUE);
            }

            // can we combine distinct cols?
            if (distinctCountColumnSeparator != null) {
                selectSQL.append("COUNT(DISTINCT ");
                for (int i = 0; i < aliases.size(); i++) {
                    if (i > 0) {
                        selectSQL.append(" ");
                        selectSQL.append(distinctCountColumnSeparator);
                        selectSQL.append(" ");
                    }
                    selectSQL.append(aliases.get(i).toString());
                }
                selectSQL.append(")");
                return toSelect(selectSQL, null, from, where, null, null, null,
                    false, false, 0, Long.MAX_VALUE);
            }
        }

        // since we can't combine distinct cols, we have to perform an outer
        // COUNT(*) select using the original select as a subselect in the
        // FROM clause
        assertSupport(supportsSubselect, "SupportsSubselect");

        SQLBuffer subSelect = getSelects(sel, true, false);
        SQLBuffer subFrom = from;
        from = new SQLBuffer(this);
        from.append("(");
        from.append(toSelect(subSelect, null, subFrom, where,
            sel.getGrouping(), sel.getHaving(), null, sel.isDistinct(),
            false, sel.getStartIndex(), sel.getEndIndex(), true));
        from.append(")");
        if (requiresAliasForSubselect)
            from.append(" ").append(Select.FROM_SELECT_ALIAS);

        selectSQL.append("COUNT(*)");
        return toSelect(selectSQL, null, from, null, null, null, null,
            false, false, 0, Long.MAX_VALUE);
    }

    /**
     * Create a DELETE statement for the specified Select. If the
     * database does not support the bulk delete statement (such as
     * cases where a subselect is required and the database doesn't support
     * subselects), this method should return null.
     */
    public SQLBuffer toDelete(ClassMapping mapping, Select sel, 
        Object[] params) {
        return toBulkOperation(mapping, sel, null, params, null);
    }

    public SQLBuffer toUpdate(ClassMapping mapping, Select sel,
        JDBCStore store, Object[] params, Map updates) {
        return toBulkOperation(mapping, sel, store, params, updates);
    }

    /**
     * Returns the SQL for a bulk operation, either a DELETE or an UPDATE.
     *
     * @param mapping the mappng against which we are operating
     * @param sel the Select that will constitute the WHERE clause
     * @param store the current store
     * @param updateParams the Map that holds the update parameters; a null
     * value indicates that this is a delete operation
     * @return the SQLBuffer for the update, or <em>null</em> if it is not
     * possible to perform the bulk update
     */
    protected SQLBuffer toBulkOperation(ClassMapping mapping, Select sel,
        JDBCStore store, Object[] params, Map updateParams) {
        SQLBuffer sql = new SQLBuffer(this);
        if (updateParams == null) {
          if (requiresTargetForDelete) {
            sql.append("DELETE ");
            SQLBuffer deleteTargets = getDeleteTargets(sel);
            sql.append(deleteTargets);
            sql.append(" FROM ");
          } else {
            sql.append("DELETE FROM ");
          }
        }
        else
            sql.append("UPDATE ");
        sel.addJoinClassConditions();

        // if there is only a single table in the select, then we can
        // just issue a single DELETE FROM TABLE WHERE <conditions>
        // statement; otherwise, since SQL doesn't allow deleting
        // from one of a multi-table select, we need to issue a subselect
        // like DELETE FROM TABLE WHERE EXISTS
        // (SELECT 1 FROM TABLE t0 WHERE t0.ID = TABLE.ID); also, some
        // databases do not allow aliases in delete statements, which
        // also causes us to use a subselect
        if (sel.getTableAliases().size() == 1 && supportsSubselect
            && allowsAliasInBulkClause) {
            SQLBuffer from;
            if (sel.getFromSelect() != null)
                from = getFromSelect(sel, false);
            else
                from = getFrom(sel, false);

            sql.append(from);
            appendUpdates(sel, store, sql, params, updateParams,
                allowsAliasInBulkClause);

            SQLBuffer where = sel.getWhere();
            if (where != null && !where.isEmpty()) {
                sql.append(" WHERE ");
                sql.append(where);
            }
            return sql;
        }

        Table table = mapping.getTable();
        String tableName = getFullName(table, false);

        // only use a  subselect if the where is not empty; otherwise
        // an unqualified delete or update will work
        if (sel.getWhere() == null || sel.getWhere().isEmpty()) {
            sql.append(tableName);
            appendUpdates(sel, store, sql, params, updateParams, false);
            return sql;
        }

        // we need to use a subselect if we are to bulk delete where
        // the select includes multiple tables; if the database
        // doesn't support it, then we need to sigal this by returning null
        if (!supportsSubselect || !supportsCorrelatedSubselect)
            return null;

        Column[] pks = mapping.getPrimaryKeyColumns();
        sel.clearSelects();
        sel.setDistinct(true);

        // if we have only a single PK, we can use a non-correlated
        // subquery (using an IN statement), which is much faster than
        // a correlated subquery (since a correlated subquery needs
        // to be executed once for each row in the table)
        if (pks.length == 1) {
            sel.select(pks[0]);
            sql.append(tableName);
            appendUpdates(sel, store, sql, params, updateParams, false);
            sql.append(" WHERE ").
                append(pks[0]).append(" IN (").
                append(sel.toSelect(false, null)).append(")");
        } else {
            sel.clearSelects();
            sel.setDistinct(false);

            // since the select is using a correlated subquery, we
            // only need to select a bogus virtual column
            sel.select("1", null);

            // add in the joins to the table
            Column[] cols = table.getPrimaryKey().getColumns();
            SQLBuffer buf = new SQLBuffer(this);
            buf.append("(");
            for (int i = 0; i < cols.length; i++) {
                if (i > 0)
                    buf.append(" AND ");

                // add in "t0.PK = MYTABLE.PK"
                buf.append(sel.getColumnAlias(cols[i], null)).append(" = ").
                    append(table).append(catalogSeparator).append(cols[i]);
            }
            buf.append(")");
            sel.where(buf, null);

            sql.append(tableName);
            appendUpdates(sel, store, sql, params, updateParams, false);
            sql.append(" WHERE EXISTS (").
                append(sel.toSelect(false, null)).append(")");
        }
        return sql;
    }

    protected SQLBuffer getDeleteTargets(Select sel) {
      SQLBuffer deleteTargets = new SQLBuffer(this);
      Collection aliases = sel.getTableAliases();
      // Assumes aliases are of the form "TABLENAME t0"
      for (Iterator itr = aliases.iterator(); itr.hasNext();) {
        String tableAlias = itr.next().toString();
        int spaceIndex = tableAlias.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < tableAlias.length() - 1) {
          if (allowsAliasInBulkClause) {
            deleteTargets.append(tableAlias.substring(spaceIndex + 1));
          } else {
            deleteTargets.append(tableAlias.substring(0, spaceIndex));
          }
        } else {
          deleteTargets.append(tableAlias);
        }
        if (itr.hasNext())
          deleteTargets.append(", ");
      }      
      return deleteTargets;      
    }

    protected void appendUpdates(Select sel, JDBCStore store, SQLBuffer sql,
        Object[] params, Map updateParams, boolean allowAlias) {
        if (updateParams == null || updateParams.size() == 0)
            return;

        // manually build up the SET clause for the UPDATE statement
        sql.append(" SET ");
        ExpContext ctx = new ExpContext(store, params, 
            store.getFetchConfiguration());

        // If the updates map contains any version fields, assume that the
        // optimistic lock version data is being handled properly by the
        // caller. Otherwise, give the version indicator an opportunity to
        // add more update clauses as needed.
        boolean augmentUpdates = true;

        for (Iterator i = updateParams.entrySet().iterator(); i.hasNext();) {
            Map.Entry next = (Map.Entry) i.next();
            Path path = (Path) next.getKey();
            FieldMapping fmd = (FieldMapping) path.last();

            if (fmd.isVersion())
                augmentUpdates = false;

            Val val = (Val) next.getValue();
            if (val == null)
            	val = new Null();
            Column col = fmd.getColumns()[0];
            if (allowAlias) {
              sql.append(sel.getColumnAlias(col));
            } else {
              sql.append(col.getName());  
            }            
            sql.append(" = ");

            ExpState state = val.initialize(sel, ctx, 0);
            // JDBC Paths are always PCPaths; PCPath implements Val
            ExpState pathState = ((Val) path).initialize(sel, ctx, 0);
            calculateValue(val, sel, ctx, state, path, pathState);

            // append the value with a null for the Select; i
            // indicates that the
            int length = val.length(sel, ctx, state);
            for (int j = 0; j < length; j++)
                val.appendTo((allowAlias) ? sel : null, ctx, state, sql, j);

            if (i.hasNext())
                sql.append(", ");
        }

        if (augmentUpdates) {
            Path path = (Path) updateParams.keySet().iterator().next();
            FieldMapping fm = (FieldMapping) path.last();
            ClassMapping meta = fm.getDeclaringMapping();
            Map updates = meta.getVersion().getBulkUpdateValues();
            for (Iterator iter = updates.entrySet().iterator();
                iter.hasNext(); ) {
                Map.Entry e = (Map.Entry) iter.next();
                Column col = (Column) e.getKey();
                String val = (String) e.getValue();
                sql.append(", ").append(col.getName())
                    .append(" = ").append(val);
            }
        }
    }
    
    /**
     * Create SQL to delete the contents of the specified tables. 
     * The default implementation drops all non-deferred RESTRICT foreign key 
     * constraints involving the specified tables, issues DELETE statements 
     * against the tables, and then adds the dropped constraints back in. 
     * Databases with more optimal ways of deleting the contents of several 
     * tables should override this method.
     */
    public String[] getDeleteTableContentsSQL(Table[] tables) {
        Collection sql = new ArrayList();
        
        // collect and drop non-deferred physical restrict constraints, and
        // collect the DELETE FROM statements
        Collection deleteSQL = new ArrayList(tables.length);
        Collection restrictConstraints = new LinkedHashSet();
        for (int i = 0; i < tables.length; i++) {
            ForeignKey[] fks = tables[i].getForeignKeys();
            for (int j = 0; j < fks.length; j++) {
                if (!fks[j].isLogical() && !fks[j].isDeferred() 
                    && fks[j].getDeleteAction() == ForeignKey.ACTION_RESTRICT)
                restrictConstraints.add(fks[j]);
                String[] constraintSQL = getDropForeignKeySQL(fks[j]);
                sql.addAll(Arrays.asList(constraintSQL));
            }
            
            deleteSQL.add("DELETE FROM " + tables[i].getFullName());
        }
        
        // add the delete statements after all the constraint mutations
        sql.addAll(deleteSQL);
        
        // add the deleted constraints back to the schema
        for (Iterator iter = restrictConstraints.iterator(); iter.hasNext(); ) {
            String[] constraintSQL = 
                getAddForeignKeySQL((ForeignKey) iter.next());
            sql.addAll(Arrays.asList(constraintSQL));
        }
        
        return (String[]) sql.toArray(new String[sql.size()]);
    }

    /**
     * Create a SELECT statement in the proper join syntax for the given
     * instance.
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
            sel.getStartIndex(), sel.getEndIndex(), sel);
    }

    /**
     * Return the portion of the select statement between the FROM keyword
     * and the WHERE keyword.
     */
    protected SQLBuffer getFrom(Select sel, boolean forUpdate) {
        SQLBuffer fromSQL = new SQLBuffer(this);
        Collection aliases = sel.getTableAliases();
        if (aliases.size() < 2 || sel.getJoinSyntax() != SYNTAX_SQL92) {
            for (Iterator itr = aliases.iterator(); itr.hasNext();) {
                fromSQL.append(itr.next().toString());
                if (forUpdate && tableForUpdateClause != null)
                    fromSQL.append(" ").append(tableForUpdateClause);
                if (itr.hasNext())
                    fromSQL.append(", ");
            }
        } else {
            Iterator itr = sel.getJoinIterator();
            boolean first = true;
            while (itr.hasNext()) {
                fromSQL.append(toSQL92Join((Join) itr.next(), forUpdate,
                    first));
                first = false;
            }
        }
        return fromSQL;
    }

    /**
     * Return the FROM clause for a select that selects from a tmp table
     * created by an inner select.
     */
    protected SQLBuffer getFromSelect(Select sel, boolean forUpdate) {
        SQLBuffer fromSQL = new SQLBuffer(this);
        fromSQL.append("(");
        fromSQL.append(toSelect(sel.getFromSelect(), forUpdate, null));
        fromSQL.append(")");
        if (requiresAliasForSubselect)
            fromSQL.append(" ").append(Select.FROM_SELECT_ALIAS);
        return fromSQL;
    }

    /**
     * Return the WHERE portion of the select statement, or null if no where
     * conditions.
     */
    protected SQLBuffer getWhere(Select sel, boolean forUpdate) {
        Joins joins = sel.getJoins();
        if (sel.getJoinSyntax() == SYNTAX_SQL92
            || joins == null || joins.isEmpty())
            return sel.getWhere();

        SQLBuffer where = new SQLBuffer(this);
        if (sel.getWhere() != null)
            where.append(sel.getWhere());
        if (joins != null)
            sel.append(where, joins);
        return where;
    }

    /**
     * Use the given join instance to create SQL joining its tables in
     * the traditional style.
     */
    public SQLBuffer toTraditionalJoin(Join join) {
        ForeignKey fk = join.getForeignKey();
        if (fk == null)
            return null;

        boolean inverse = join.isForeignKeyInversed();
        Column[] from = (inverse) ? fk.getPrimaryKeyColumns()
            : fk.getColumns();
        Column[] to = (inverse) ? fk.getColumns()
            : fk.getPrimaryKeyColumns();

        // do column joins
        SQLBuffer buf = new SQLBuffer(this);
        int count = 0;
        for (int i = 0; i < from.length; i++, count++) {
            if (count > 0)
                buf.append(" AND ");
            buf.append(join.getAlias1()).append(".").append(from[i]);
            buf.append(" = ");
            buf.append(join.getAlias2()).append(".").append(to[i]);
        }

        // do constant joins
        Column[] constCols = fk.getConstantColumns();
        for (int i = 0; i < constCols.length; i++, count++) {
            if (count > 0)
                buf.append(" AND ");
            if (inverse)
                buf.appendValue(fk.getConstant(constCols[i]), constCols[i]);
            else
                buf.append(join.getAlias1()).append(".").
                    append(constCols[i]);
            buf.append(" = ");

            if (inverse)
                buf.append(join.getAlias2()).append(".").
                    append(constCols[i]);
            else
                buf.appendValue(fk.getConstant(constCols[i]), constCols[i]);
        }

        Column[] constColsPK = fk.getConstantPrimaryKeyColumns();
        for (int i = 0; i < constColsPK.length; i++, count++) {
            if (count > 0)
                buf.append(" AND ");
            if (inverse)
                buf.append(join.getAlias1()).append(".").
                    append(constColsPK[i]);
            else
                buf.appendValue(fk.getPrimaryKeyConstant(constColsPK[i]),
                    constColsPK[i]);
            buf.append(" = ");

            if (inverse)
                buf.appendValue(fk.getPrimaryKeyConstant(constColsPK[i]),
                    constColsPK[i]);
            else
                buf.append(join.getAlias2()).append(".").
                    append(constColsPK[i]);
        }
        return buf;
    }

    /**
     * Use the given join instance to create SQL joining its tables in
     * the SQL92 style.
     */
    public SQLBuffer toSQL92Join(Join join, boolean forUpdate, boolean first) {
        SQLBuffer buf = new SQLBuffer(this);
        if (first) {
            buf.append(join.getTable1()).append(" ").
                append(join.getAlias1());
            if (forUpdate && tableForUpdateClause != null)
                buf.append(" ").append(tableForUpdateClause);
        }

        buf.append(" ");
        if (join.getType() == Join.TYPE_OUTER)
            buf.append(outerJoinClause);
        else if (join.getType() == Join.TYPE_INNER)
            buf.append(innerJoinClause);
        else // cross
            buf.append(crossJoinClause);
        buf.append(" ");

        buf.append(join.getTable2()).append(" ").append(join.getAlias2());
        if (forUpdate && tableForUpdateClause != null)
            buf.append(" ").append(tableForUpdateClause);

        if (join.getForeignKey() != null)
            buf.append(" ON ").append(toTraditionalJoin(join));
        else if (requiresConditionForCrossJoin &&
            join.getType() == Join.TYPE_CROSS)
            buf.append(" ON (1 = 1)");

        return buf;
    }

    /**
     * Use the given join instance to create SQL joining its tables in
     * the database's native syntax. Throws an exception by default.
     */
    public SQLBuffer toNativeJoin(Join join) {
        throw new UnsupportedException();
    }

    /**
     * Returns if the given foreign key can be eagerly loaded using other joins.
     */
    public boolean canOuterJoin(int syntax, ForeignKey fk) {
        return syntax != SYNTAX_TRADITIONAL;
    }

    /**
     * Combine the given components into a SELECT statement.
     */
    public SQLBuffer toSelect(SQLBuffer selects, JDBCFetchConfiguration fetch,
        SQLBuffer from, SQLBuffer where, SQLBuffer group,
        SQLBuffer having, SQLBuffer order,
        boolean distinct, boolean forUpdate, long start, long end) {
        return toOperation(getSelectOperation(fetch), selects, from, where,
            group, having, order, distinct, start, end,
            getForUpdateClause(fetch, forUpdate, null));
    }

    /**
     * Combine the given components into a SELECT statement.
     */
    public SQLBuffer toSelect(SQLBuffer selects, JDBCFetchConfiguration fetch,
        SQLBuffer from, SQLBuffer where, SQLBuffer group,
        SQLBuffer having, SQLBuffer order,
        boolean distinct, boolean forUpdate, long start, long end,
        boolean subselect) {
        return toOperation(getSelectOperation(fetch), selects, from, where,
            group, having, order, distinct, start, end,
            getForUpdateClause(fetch, forUpdate, null), subselect);
    }
    
    public SQLBuffer toSelect(SQLBuffer selects, JDBCFetchConfiguration fetch,
            SQLBuffer from, SQLBuffer where, SQLBuffer group,
            SQLBuffer having, SQLBuffer order,
            boolean distinct, boolean forUpdate, long start, long end,
            boolean subselect, boolean checkTableForUpdate) {
            return toOperation(getSelectOperation(fetch), selects, from, where,
                group, having, order, distinct, start, end,
                getForUpdateClause(fetch, forUpdate, null), subselect, checkTableForUpdate);
        }

    /**
     * Combine the given components into a SELECT statement.
     */
    public SQLBuffer toSelect(SQLBuffer selects, JDBCFetchConfiguration fetch,
        SQLBuffer from, SQLBuffer where, SQLBuffer group,
        SQLBuffer having, SQLBuffer order,
        boolean distinct, boolean forUpdate, long start, long end,
        Select sel) {
        return toOperation(getSelectOperation(fetch), selects, from, where,
            group, having, order, distinct, start, end,
            getForUpdateClause(fetch, forUpdate, sel));
    }

    /**
     * Get the update clause for the query based on the
     * updateClause and isolationLevel hints
     */
    protected String getForUpdateClause(JDBCFetchConfiguration fetch,
        boolean isForUpdate, Select sel) {
        if (fetch != null && fetch.getIsolation() != -1) {
            throw new InvalidStateException(_loc.get(
                "isolation-level-config-not-supported", getClass().getName()));
        } else if (isForUpdate && !simulateLocking) {
            assertSupport(supportsSelectForUpdate, "SupportsSelectForUpdate");
            return forUpdateClause;
        } else {
            return null;
        }
    }

    /**
     * Return the "SELECT" operation clause, adding any available hints, etc.
     */
    public String getSelectOperation(JDBCFetchConfiguration fetch) {
        return "SELECT";
    }

    /**
     * Return the SQL for the given selecting operation.
     */
    public SQLBuffer toOperation(String op, SQLBuffer selects,
        SQLBuffer from, SQLBuffer where, SQLBuffer group, SQLBuffer having,
        SQLBuffer order, boolean distinct, long start, long end,
        String forUpdateClause) {
        return toOperation(op, selects, from, where, group, having, order,
            distinct, start, end, forUpdateClause, false);
    }
    
    /**
     * Return the SQL for the given selecting operation.
     */
    public SQLBuffer toOperation(String op, SQLBuffer selects,
        SQLBuffer from, SQLBuffer where, SQLBuffer group, SQLBuffer having,
        SQLBuffer order, boolean distinct, long start, long end,
        String forUpdateClause, boolean subselect) {
        return toOperation(op, selects, from, where, group, having, order,
                distinct, start, end, forUpdateClause, subselect, false);
    }

    /**
     * Return the SQL for the given selecting operation.
     */
    private SQLBuffer toOperation(String op, SQLBuffer selects,
        SQLBuffer from, SQLBuffer where, SQLBuffer group, SQLBuffer having,
        SQLBuffer order, boolean distinct, long start, long end,
        String forUpdateClause, boolean subselect, boolean checkTableForUpdate) {
        SQLBuffer buf = new SQLBuffer(this);
        buf.append(op);

        boolean range = start != 0 || end != Long.MAX_VALUE;
        if (range && rangePosition == RANGE_PRE_DISTINCT)
            appendSelectRange(buf, start, end, subselect);
        if (distinct)
            buf.append(" DISTINCT");
        if (range && rangePosition == RANGE_POST_DISTINCT)
            appendSelectRange(buf, start, end, subselect);

        buf.append(" ").append(selects).append(" FROM ").append(from);

        if (checkTableForUpdate
                && (StringUtils.isEmpty(forUpdateClause) && !StringUtils
                        .isEmpty(tableForUpdateClause))) {
            buf.append(" ").append(tableForUpdateClause);
        }

        if (where != null && !where.isEmpty())
            buf.append(" WHERE ").append(where);
        if (group != null && !group.isEmpty())
            buf.append(" GROUP BY ").append(group);
        if (having != null && !having.isEmpty()) {
            assertSupport(supportsHaving, "SupportsHaving");
            buf.append(" HAVING ").append(having);
        }
        if (order != null && !order.isEmpty())
            buf.append(" ORDER BY ").append(order);
        if (range && rangePosition == RANGE_POST_SELECT)
            appendSelectRange(buf, start, end, subselect);
        if (forUpdateClause != null)
            buf.append(" ").append(forUpdateClause);
        if (range && rangePosition == RANGE_POST_LOCK)
            appendSelectRange(buf, start, end, subselect);
        return buf;
    }

    /**
     * If this dictionary can select ranges,
     * use this method to append the range SQL.
     */
    protected void appendSelectRange(SQLBuffer buf, long start, long end,
        boolean subselect) {
    }

    /**
     * Return the portion of the select statement between the SELECT keyword
     * and the FROM keyword.
     */
    protected SQLBuffer getSelects(Select sel, boolean distinctIdentifiers,
        boolean forUpdate) {
        // append the aliases for all the columns
        SQLBuffer selectSQL = new SQLBuffer(this);
        List aliases;
        if (distinctIdentifiers)
            aliases = sel.getIdentifierAliases();
        else
            aliases = sel.getSelectAliases();

        Object alias;
        for (int i = 0; i < aliases.size(); i++) {
            alias = aliases.get(i);
            appendSelect(selectSQL, alias, sel, i);
            if (i < aliases.size() - 1)
                selectSQL.append(", ");
        }
        return selectSQL;
    }

    /**
     * Append <code>elem</code> to <code>selectSQL</code>.
     * @param selectSQL The SQLBuffer to append to.
     * @param alias A {@link SQLBuffer} or a {@link String} to append.
     *
     * @since 1.1.0
     */
    protected void appendSelect(SQLBuffer selectSQL, Object elem, Select sel,
        int idx) {
        if (elem instanceof SQLBuffer)
            selectSQL.append((SQLBuffer) elem);
        else
            selectSQL.append(elem.toString());
    }

    /**
     * Returns true if a "FOR UPDATE" clause can be used for the specified
     * Select object.
     */
    public boolean supportsLocking(Select sel) {
        if (sel.isAggregate())
            return false;
        if (!supportsSelectForUpdate)
            return false;
        if (!supportsLockingWithSelectRange && (sel.getStartIndex() != 0
            || sel.getEndIndex() != Long.MAX_VALUE))
            return false;

        // only inner select is locked
        if (sel.getFromSelect() != null)
            sel = sel.getFromSelect();

        if (!supportsLockingWithDistinctClause && sel.isDistinct())
            return false;
        if (!supportsLockingWithMultipleTables
            && sel.getTableAliases().size() > 1)
            return false;
        if (!supportsLockingWithOrderClause && sel.getOrdering() != null)
            return false;
        if (!supportsLockingWithOuterJoin || !supportsLockingWithInnerJoin) {
            for (Iterator itr = sel.getJoinIterator(); itr.hasNext();) {
                Join join = (Join) itr.next();
                if (!supportsLockingWithOuterJoin
                    && join.getType() == Join.TYPE_OUTER)
                    return false;
                if (!supportsLockingWithInnerJoin
                    && join.getType() == Join.TYPE_INNER)
                    return false;
            }
        }
        return true;
    }

    /**
     * Return false if the given select requires a forward-only result set.
     */
    public boolean supportsRandomAccessResultSet(Select sel,
        boolean forUpdate) {
        return !sel.isAggregate();
    }

    /**
     * Assert that the given dictionary flag is true. If it is not true,
     * throw an error saying that the given setting needs to return true for
     * the current operation to work.
     */
    public void assertSupport(boolean feature, String property) {
        if (!feature)
            throw new UnsupportedException(_loc.get("feature-not-supported",
                getClass(), property));
    }

    ////////////////////
    // Query functions
    ////////////////////

    /**
     * Invoke this database's substring function.
     *
     * @param buf the SQL buffer to write the substring invocation to
     * @param str a query value representing the target string
     * @param start a query value representing the start index
     * @param end a query value representing the end index, or null for none
     */
    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
        FilterValue end) {
        buf.append(substringFunctionName).append("(");
        str.appendTo(buf);
        buf.append(", ");
        if (start.getValue() instanceof Number) {
            long startLong = toLong(start);
            buf.append(Long.toString(startLong + 1));
        } else {
            buf.append("(");
            start.appendTo(buf);
            buf.append(" + 1)");
        }
        if (end != null) {
            buf.append(", ");
            if (start.getValue() instanceof Number
                && end.getValue() instanceof Number) {
                long startLong = toLong(start);
                long endLong = toLong(end);
                buf.append(Long.toString(endLong - startLong));
            } else {
                end.appendTo(buf);
                buf.append(" - (");
                start.appendTo(buf);
                buf.append(")");
            }
        }
        buf.append(")");
    }

    long toLong(FilterValue litValue) {
        return ((Number) litValue.getValue()).longValue();
    }

    /**
     * Invoke this database's indexOf function.
     *
     * @param buf the SQL buffer to write the indexOf invocation to
     * @param str a query value representing the target string
     * @param find a query value representing the search string
     * @param start a query value representing the start index, or null
     * to start at the beginning
     */
    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("(INSTR((");
        if (start != null)
            substring(buf, str, start, null);
        else
            str.appendTo(buf);
        buf.append("), (");
        find.appendTo(buf);
        buf.append(")) - 1");
        if (start != null) {
            buf.append(" + ");
            start.appendTo(buf);
        }
        buf.append(")");
    }

    /**
     * Append the numeric parts of a mathematical function.
     *
     * @param buf the SQL buffer to write the math function
     * @param op the mathematical operation to perform
     * @param lhs the left hand side of the math function
     * @param rhs the right hand side of the math function
     */
    public void mathFunction(SQLBuffer buf, String op, FilterValue lhs,
        FilterValue rhs) {
        boolean castlhs = false;
        boolean castrhs = false;
        Class lc = Filters.wrap(lhs.getType());
        Class rc = Filters.wrap(rhs.getType());
        int type = 0;
        if (requiresCastForMathFunctions && (lc != rc
            || (lhs.isConstant() && rhs.isConstant()))) {
            Class c = Filters.promote(lc, rc);
            type = getJDBCType(JavaTypes.getTypeCode(c), false);
            if (type != Types.VARBINARY && type != Types.BLOB) {
                castlhs = (lhs.isConstant() && rhs.isConstant()) || lc != c;
                castrhs = (lhs.isConstant() && rhs.isConstant()) || rc != c;
            }
        }

        boolean mod = "MOD".equals(op);
        if (mod) {
            if (supportsModOperator)
                op = "%";
            else
                buf.append(op);
        }
        buf.append("(");

        if (castlhs)
            appendCast(buf, lhs, type);
        else
            lhs.appendTo(buf);

        if (mod && !supportsModOperator)
            buf.append(", ");
        else
            buf.append(" ").append(op).append(" ");

        if (castrhs)
            appendCast(buf, rhs, type);
        else
            rhs.appendTo(buf);

        buf.append(")");
    }

    /**
     * Append a comparison.
     *
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison
     * @param rhs the right hand side of the comparison
     */
    public void comparison(SQLBuffer buf, String op, FilterValue lhs,
        FilterValue rhs) {
        boolean lhsxml = lhs.getXPath() != null;
        boolean rhsxml = rhs.getXPath() != null;
        if (lhsxml || rhsxml) {
            appendXmlComparison(buf, op, lhs, rhs, lhsxml, rhsxml);
            return;
        }
        boolean castlhs = false;
        boolean castrhs = false;
        Class lc = Filters.wrap(lhs.getType());
        Class rc = Filters.wrap(rhs.getType());
        int type = 0;
        if (requiresCastForComparisons && (lc != rc
            || (lhs.isConstant() && rhs.isConstant()))) {
            Class c = Filters.promote(lc, rc);
            type = getJDBCType(JavaTypes.getTypeCode(c), false);
            if (type != Types.VARBINARY && type != Types.BLOB) {
                castlhs = (lhs.isConstant() && rhs.isConstant()) || lc != c;
                castrhs = (lhs.isConstant() && rhs.isConstant()) || rc != c;
            }
        }

        if (castlhs)
            appendCast(buf, lhs, type);
        else
            lhs.appendTo(buf);

        buf.append(" ").append(op).append(" ");

        if (castrhs)
            appendCast(buf, rhs, type);
        else
            rhs.appendTo(buf);
    }

    /**
     * If this dictionary supports XML type,
     * use this method to append xml predicate.
     */
    public void appendXmlComparison(SQLBuffer buf, String op, FilterValue lhs,
        FilterValue rhs, boolean lhsxml, boolean rhsxml) {
        assertSupport(supportsXMLColumn, "SupportsXMLColumn");
    }

    /**
     * Append SQL for the given numeric value to the buffer, casting as needed.
     */
    protected void appendNumericCast(SQLBuffer buf, FilterValue val) {
        if (val.isConstant())
            appendCast(buf, val, Types.NUMERIC);
        else
            val.appendTo(buf);
    }

    /**
     * Cast the specified value to the specified type.
     *
     * @param buf the buffer to append the cast to
     * @param val the value to cast
     * @param type the type of the case, e.g. {@link Types#NUMERIC}
     */
    public void appendCast(SQLBuffer buf, Object val, int type) {
        // Convert the cast function: "CAST({0} AS {1})"
        int firstParam = castFunction.indexOf("{0}");
        String pre = castFunction.substring(0, firstParam); // "CAST("
        String mid = castFunction.substring(firstParam + 3);
        int secondParam = mid.indexOf("{1}");
        String post;
        if (secondParam > -1) {
            post = mid.substring(secondParam + 3); // ")"
            mid = mid.substring(0, secondParam); // " AS "
        } else
            post = "";

        buf.append(pre);
        if (val instanceof FilterValue)
            ((FilterValue) val).appendTo(buf);
        else if (val instanceof SQLBuffer)
            buf.append(((SQLBuffer) val));
        else
            buf.append(val.toString());
        buf.append(mid);
        buf.append(getTypeName(type));
        appendLength(buf, type);
        buf.append(post);
    }
    
    protected void appendLength(SQLBuffer buf, int type) {        
    }

    
    /**
     * add CAST for a function operator where operand is a param
     * @param func  function name
     * @param val 
     * @return updated func
     */
    public String addCastAsType(String func, Val val) {
        return null;
    }    


    ///////////
    // DDL SQL
    ///////////

    /**
     * Increment the reference count of any table components that this
     * dictionary adds that are not used by mappings. Does nothing by default.
     */
    public void refSchemaComponents(Table table) {
    }

    /**
     * Returns the full name of the table, including the schema (delimited
     * by {@link #catalogSeparator}).
     */
    public String getFullName(Table table, boolean logical) {
        if (!useSchemaName || table.getSchemaName() == null)
            return table.getName();
        if (logical || ".".equals(catalogSeparator))
            return table.getFullName();
        return table.getSchemaName() + catalogSeparator + table.getName();
    }

    /**
     * Returns the full name of the index, including the schema (delimited
     * by the result of {@link #catalogSeparator}).
     */
    public String getFullName(Index index) {
        if (!useSchemaName || index.getSchemaName() == null)
            return index.getName();
        if (".".equals(catalogSeparator))
            return index.getFullName();
        return index.getSchemaName() + catalogSeparator + index.getName();
    }

    /**
     * Returns the full name of the sequence, including the schema (delimited
     * by the result of {@link #catalogSeparator}).
     */
    public String getFullName(Sequence seq) {
        if (!useSchemaName || seq.getSchemaName() == null)
            return seq.getName();
        if (".".equals(catalogSeparator))
            return seq.getFullName();
        return seq.getSchemaName() + catalogSeparator + seq.getName();
    }

    /**
     * Make any necessary changes to the given table name to make it valid
     * for the current DB.
     */
    public String getValidTableName(String name, Schema schema) {
        while (name.startsWith("_"))
            name = name.substring(1);
        return makeNameValid(name, schema.getSchemaGroup(),
            maxTableNameLength, NAME_TABLE);
    }

    /**
     * Make any necessary changes to the given sequence name to make it valid
     * for the current DB.
     */
    public String getValidSequenceName(String name, Schema schema) {
        while (name.startsWith("_"))
            name = name.substring(1);
        return makeNameValid("S_" + name, schema.getSchemaGroup(),
            maxTableNameLength, NAME_SEQUENCE);
    }

    /**
     * Make any necessary changes to the given column name to make it valid
     * for the current DB.  The column name will be made unique for the
     * specified table.
     */
    public String getValidColumnName(String name, Table table) {
        return getValidColumnName(name, table, true);
    }

    /**
     * Make any necessary changes to the given column name to make it valid
     * for the current DB.  If checkForUniqueness is true, the column name will 
     * be made unique for the specified table.
     */
    public String getValidColumnName(String name, Table table,
        boolean checkForUniqueness) {
        while (name.startsWith("_"))
            name = name.substring(1);
        return makeNameValid(name, table, maxColumnNameLength, NAME_ANY,
            checkForUniqueness);
    }

    /**
     * Make any necessary changes to the given primary key name to make it
     * valid for the current DB.
     */
    public String getValidPrimaryKeyName(String name, Table table) {
        while (name.startsWith("_"))
            name = name.substring(1);
        return makeNameValid("P_" + name, table.getSchema().getSchemaGroup(),
            maxConstraintNameLength, NAME_ANY);
    }

    /**
     * Make any necessary changes to the given foreign key name to make it
     * valid for the current DB.
     */
    public String getValidForeignKeyName(String name, Table table,
        Table toTable) {
        while (name.startsWith("_"))
            name = name.substring(1);
        String tableName = table.getName();
        int len = Math.min(tableName.length(), 7);
        name = "F_" + shorten(tableName, len) + "_" + name;
        return makeNameValid(name, table.getSchema().getSchemaGroup(),
            maxConstraintNameLength, NAME_ANY);
    }

    /**
     * Make any necessary changes to the given index name to make it valid
     * for the current DB.
     */
    public String getValidIndexName(String name, Table table) {
        while (name.startsWith("_"))
            name = name.substring(1);
        String tableName = table.getName();
        int len = Math.min(tableName.length(), 7);
        name = "I_" + shorten(tableName, len) + "_" + name;
        return makeNameValid(name, table.getSchema().getSchemaGroup(),
            maxIndexNameLength, NAME_ANY);
    }

    /**
     * Make any necessary changes to the given unique constraint name to make
     * it valid for the current DB.
     */
    public String getValidUniqueName(String name, Table table) {
        while (name.startsWith("_"))
            name = name.substring(1);
        String tableName = table.getName();
        int len = Math.min(tableName.length(), 7);
        name = "U_" + shorten(tableName, len) + "_" + name;
        return makeNameValid(name, table.getSchema().getSchemaGroup(),
            maxConstraintNameLength, NAME_ANY);
    }

    /**
     * Shorten the specified name to the specified target name. This will
     * be done by first stripping out the vowels, and then removing
     * characters from the middle of the word until it reaches the target
     * length.
     */
    protected static String shorten(String name, int targetLength) {
        if (name == null || name.length() <= targetLength)
            return name;

        StringBuffer nm = new StringBuffer(name);
        while (nm.length() > targetLength) {
            if (!stripVowel(nm)) {
                // cut out the middle char
                nm.replace(nm.length() / 2, (nm.length() / 2) + 1, "");
            }
        }
        return nm.toString();
    }

    /**
     * Remove vowels from the specified StringBuffer.
     *
     * @return true if any vowels have been removed
     */
    private static boolean stripVowel(StringBuffer name) {
        if (name == null || name.length() == 0)
            return false;

        char[] vowels = { 'A', 'E', 'I', 'O', 'U', };
        for (int i = 0; i < vowels.length; i++) {
            int index = name.toString().toUpperCase().indexOf(vowels[i]);
            if (index != -1) {
                name.replace(index, index + 1, "");
                return true;
            }
        }
        return false;
    }

    /**
     * Shortens the given name to the given maximum length, then checks that
     * it is not a reserved word. If it is reserved, appends a "0". If
     * the name conflicts with an existing schema component, the last
     * character is replace with '0', then '1', etc.
     * Note that the given max len may be 0 if the database metadata is
     * incomplete.
     */
    protected String makeNameValid(String name, NameSet set, int maxLen,
        int nameType) {
        return makeNameValid(name, set, maxLen, nameType, true);
    }

    /**
     * Shortens the given name to the given maximum length, then checks that
     * it is not a reserved word. If it is reserved, appends a "0". If
     * the name conflicts with an existing schema component and uniqueness
     * checking is enabled, the last character is replace with '0', then 
     * '1', etc. 
     * Note that the given max len may be 0 if the database metadata is 
     * incomplete.
     */
    protected String makeNameValid(String name, NameSet set, int maxLen,
        int nameType, boolean checkForUniqueness) {
        if (maxLen < 1)
            maxLen = 255;
        if (name.length() > maxLen)
            name = name.substring(0, maxLen);
        if (reservedWordSet.contains(name.toUpperCase())) {
            if (name.length() == maxLen)
                name = name.substring(0, name.length() - 1);
            name += "0";
        }

        // now make sure the name is unique
        if (set != null && checkForUniqueness) {
            outer:
            for (int version = 1, chars = 1; true; version++) {
                // for table names, we check for the table itself in case the
                // name set is lazy about schema reflection
                switch (nameType) {
                    case NAME_TABLE:
                        if (!((SchemaGroup) set).isKnownTable(name))
                            break outer;
                        break;
                    case NAME_SEQUENCE:
                        if (!((SchemaGroup) set).isKnownSequence(name))
                            break outer;
                        break;
                    default:
                        if (!set.isNameTaken(name))
                            break outer;
                }

                // a single char for the version is probably enough, but might
                // as well be general about it...
                if (version > 1)
                    name = name.substring(0, name.length() - chars);
                if (version >= Math.pow(10, chars))
                    chars++;
                if (name.length() + chars > maxLen)
                    name = name.substring(0, maxLen - chars);
                name = name + version;
            }
        }
        return name.toUpperCase();
    }

    /**
     * Return a series of SQL statements to create the given table, complete
     * with columns. Indexes and constraints will be created separately.
     */
    public String[] getCreateTableSQL(Table table) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(getFullName(table, false));
        if (supportsComments && table.hasComment()) {
            buf.append(" ");
            comment(buf, table.getComment());
            buf.append("\n    (");
        } else {
            buf.append(" (");
        }

        // do this before getting the columns so we know how to handle
        // the last comma
        StringBuffer endBuf = new StringBuffer();
        PrimaryKey pk = table.getPrimaryKey();
        String pkStr;
        if (pk != null) {
            pkStr = getPrimaryKeyConstraintSQL(pk);
            if (pkStr != null)
                endBuf.append(pkStr);
        }

        Unique[] unqs = table.getUniques();
        String unqStr;
        for (int i = 0; i < unqs.length; i++) {
            unqStr = getUniqueConstraintSQL(unqs[i]);
            if (unqStr != null) {
                if (endBuf.length() > 0)
                    endBuf.append(", ");
                endBuf.append(unqStr);
            }
        }

        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++) {
            buf.append(getDeclareColumnSQL(cols[i], false));
            if (i < cols.length - 1 || endBuf.length() > 0)
                buf.append(", ");
            if (supportsComments && cols[i].hasComment()) {
                comment(buf, cols[i].getComment());
                buf.append("\n    ");
            }
        }

        buf.append(endBuf.toString());
        buf.append(")");
        return new String[]{ buf.toString() };
    }

    protected StringBuffer comment(StringBuffer buf, String comment) {
        return buf.append("-- ").append(comment);
    }

    /**
     * Return a series of SQL statements to drop the given table. Indexes
     * will be dropped separately. Returns
     * <code>DROP TABLE &lt;table name&gt;</code> by default.
     */
    public String[] getDropTableSQL(Table table) {
        String drop = MessageFormat.format(dropTableSQL, new Object[]{
            getFullName(table, false) });
        return new String[]{ drop };
    }

    /**
     * Return a series of SQL statements to create the given sequence. Returns
     * <code>CREATE SEQUENCE &lt;sequence name&gt;[ START WITH &lt;start&gt;]
     * [ INCREMENT BY &lt;increment&gt;]</code> by default.
     */
    public String[] getCreateSequenceSQL(Sequence seq) {
        if (nextSequenceQuery == null)
            return null;

        StringBuffer buf = new StringBuffer();
        buf.append("CREATE SEQUENCE ");
        buf.append(getFullName(seq));
        if (seq.getInitialValue() != 0)
            buf.append(" START WITH ").append(seq.getInitialValue());
        
        if (seq.getIncrement() > 1 && useNativeSequenceCache){
        	buf.append(" INCREMENT BY ").append(seq.getIncrement());
        }
        else if ((seq.getIncrement() > 1) || (seq.getAllocate() > 1)){
            buf.append(" INCREMENT BY ").append(seq.getIncrement() * seq.getAllocate());
        }
        
        return new String[]{ buf.toString() };
    }

    /**
     * Return a series of SQL statements to drop the given sequence. Returns
     * <code>DROP SEQUENCE &lt;sequence name&gt;</code> by default.
     */
    public String[] getDropSequenceSQL(Sequence seq) {
        return new String[]{ "DROP SEQUENCE " + getFullName(seq) };
    }

    /**
     * Return a series of SQL statements to create the given index. Returns
     * <code>CREATE [UNIQUE] INDEX &lt;index name&gt; ON &lt;table name&gt;
     * (&lt;col list&gt;)</code> by default.
     */
    public String[] getCreateIndexSQL(Index index) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE ");
        if (index.isUnique())
            buf.append("UNIQUE ");
        buf.append("INDEX ").append(index.getName());

        buf.append(" ON ").append(getFullName(index.getTable(), false));
        buf.append(" (").append(Strings.join(index.getColumns(), ", ")).
            append(")");

        return new String[]{ buf.toString() };
    }

    /**
     * Return a series of SQL statements to drop the given index. Returns
     * <code>DROP INDEX &lt;index name&gt;</code> by default.
     */
    public String[] getDropIndexSQL(Index index) {
        return new String[]{ "DROP INDEX " + getFullName(index) };
    }

    /**
     * Return a series of SQL statements to add the given column to
     * its table. Return an empty array if operation not supported. Returns
     * <code>ALTER TABLE &lt;table name&gt; ADD (&lt;col dec&gt;)</code>
     * by default.
     */
    public String[] getAddColumnSQL(Column column) {
        if (!supportsAlterTableWithAddColumn)
            return new String[0];

        String dec = getDeclareColumnSQL(column, true);
        if (dec == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(column.getTable(), false) + " ADD " + dec };
    }

    /**
     * Return a series of SQL statements to drop the given column from
     * its table. Return an empty array if operation not supported. Returns
     * <code>ALTER TABLE &lt;table name&gt; DROP COLUMN &lt;col name&gt;</code>
     * by default.
     */
    public String[] getDropColumnSQL(Column column) {
        if (!supportsAlterTableWithDropColumn)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(column.getTable(), false)
            + " DROP COLUMN " + column };
    }

    /**
     * Return a series of SQL statements to add the given primary key to
     * its table. Return an empty array if operation not supported.
     * Returns <code>ALTER TABLE &lt;table name&gt; ADD
     * &lt;pk cons sql &gt;</code> by default.
     */
    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        String pksql = getPrimaryKeyConstraintSQL(pk);
        if (pksql == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(pk.getTable(), false) + " ADD " + pksql };
    }

    /**
     * Return a series of SQL statements to drop the given primary key from
     * its table. Return an empty array if operation not supported.
     * Returns <code>ALTER TABLE &lt;table name&gt; DROP CONSTRAINT
     * &lt;pk name&gt;</code> by default.
     */
    public String[] getDropPrimaryKeySQL(PrimaryKey pk) {
        if (pk.getName() == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(pk.getTable(), false)
            + " DROP CONSTRAINT " + pk.getName() };
    }

    /**
     * Return a series of SQL statements to add the given foreign key to
     * its table. Return an empty array if operation not supported.
     * Returns <code>ALTER TABLE &lt;table name&gt; ADD
     * &lt;fk cons sql &gt;</code> by default.
     */
    public String[] getAddForeignKeySQL(ForeignKey fk) {
        String fkSQL = getForeignKeyConstraintSQL(fk);
        if (fkSQL == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(fk.getTable(), false) + " ADD " + fkSQL };
    }

    /**
     * Return a series of SQL statements to drop the given foreign key from
     * its table. Return an empty array if operation not supported.
     * Returns <code>ALTER TABLE &lt;table name&gt; DROP CONSTRAINT
     * &lt;fk name&gt;</code> by default.
     */
    public String[] getDropForeignKeySQL(ForeignKey fk) {
        if (fk.getName() == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(fk.getTable(), false)
            + " DROP CONSTRAINT " + fk.getName() };
    }

    /**
     * Return the declaration SQL for the given column. This method is used
     * for each column from within {@link #getCreateTableSQL} and
     * {@link #getAddColumnSQL}.
     */
    protected String getDeclareColumnSQL(Column col, boolean alter) {
        StringBuffer buf = new StringBuffer();
        buf.append(col).append(" ");
        buf.append(getTypeName(col));

        // can't add constraints to a column we're adding after table
        // creation, cause some data might already be inserted
        if (!alter) {
            if (col.getDefaultString() != null && !col.isAutoAssigned())
                buf.append(" DEFAULT ").append(col.getDefaultString());
            if (col.isNotNull())
                buf.append(" NOT NULL");
        }
        if (col.isAutoAssigned()) {
            if (!supportsAutoAssign)
                log.warn(_loc.get("invalid-autoassign", platform, col));
            else if (autoAssignClause != null)
                buf.append(" ").append(autoAssignClause);
        }
        return buf.toString();
    }

    /**
     * Return the declaration SQL for the given primary key. This method is
     * used from within {@link #getCreateTableSQL} and
     * {@link #getAddPrimaryKeySQL}. Returns
     * <code>CONSTRAINT &lt;pk name&gt; PRIMARY KEY (&lt;col list&gt;)</code>
     * by default.
     */
    protected String getPrimaryKeyConstraintSQL(PrimaryKey pk) {
        // if we have disabled the creation of primary keys, abort here
        if (!createPrimaryKeys)
            return null;

        String name = pk.getName();
        if (name != null && reservedWordSet.contains(name.toUpperCase()))
            name = null;

        StringBuffer buf = new StringBuffer();
        if (name != null && CONS_NAME_BEFORE.equals(constraintNameMode))
            buf.append("CONSTRAINT ").append(name).append(" ");
        buf.append("PRIMARY KEY ");
        if (name != null && CONS_NAME_MID.equals(constraintNameMode))
            buf.append(name).append(" ");
        buf.append("(").append(Strings.join(pk.getColumns(), ", ")).
            append(")");
        if (name != null && CONS_NAME_AFTER.equals(constraintNameMode))
            buf.append(" CONSTRAINT ").append(name);
        return buf.toString();
    }

    /**
     * Return the declaration SQL for the given foreign key, or null if it is
     * not supported. This method is used from within
     * {@link #getCreateTableSQL} and {@link #getAddForeignKeySQL}. Returns
     * <code>CONSTRAINT &lt;cons name&gt; FOREIGN KEY (&lt;col list&gt;)
     * REFERENCES &lt;foreign table&gt; (&lt;col list&gt;)
     * [ON DELETE &lt;action&gt;] [ON UPDATE &lt;action&gt;]</code> by default.
     */
    protected String getForeignKeyConstraintSQL(ForeignKey fk) {
        if (!supportsForeignKeys)
            return null;
        if (fk.getDeleteAction() == ForeignKey.ACTION_NONE)
            return null;
        if (fk.isDeferred() && !supportsDeferredForeignKeyConstraints())
            return null;
        if (!supportsDeleteAction(fk.getDeleteAction())
            || !supportsUpdateAction(fk.getUpdateAction()))
            return null;

        Column[] locals = fk.getColumns();
        Column[] foreigns = fk.getPrimaryKeyColumns();

        int delActionId = fk.getDeleteAction();
        if (delActionId == ForeignKey.ACTION_NULL) {
            for (int i = 0; i < locals.length; i++) {
                if (locals[i].isNotNull())
                    delActionId = ForeignKey.ACTION_NONE;
            }
        }

        String delAction = getActionName(delActionId);
        String upAction = getActionName(fk.getUpdateAction());

        StringBuffer buf = new StringBuffer();
        if (fk.getName() != null
            && CONS_NAME_BEFORE.equals(constraintNameMode))
            buf.append("CONSTRAINT ").append(fk.getName()).append(" ");
        buf.append("FOREIGN KEY ");
        if (fk.getName() != null && CONS_NAME_MID.equals(constraintNameMode))
            buf.append(fk.getName()).append(" ");
        buf.append("(").append(Strings.join(locals, ", ")).append(")");
        buf.append(" REFERENCES ");
        buf.append(getFullName(foreigns[0].getTable(), false));
        buf.append(" (").append(Strings.join(foreigns, ", ")).append(")");
        if (delAction != null)
            buf.append(" ON DELETE ").append(delAction);
        if (upAction != null)
            buf.append(" ON UPDATE ").append(upAction);
        if (fk.isDeferred())
            buf.append(" INITIALLY DEFERRED");
        if (supportsDeferredForeignKeyConstraints())
            buf.append(" DEFERRABLE");
        if (fk.getName() != null
            && CONS_NAME_AFTER.equals(constraintNameMode))
            buf.append(" CONSTRAINT ").append(fk.getName());
        return buf.toString();
    }

    /**
     * Whether or not this dictionary supports deferred foreign key constraints.
     * This implementation returns {@link #supportsUniqueConstraints}.
     *
     * @since 1.1.0
     */
    protected boolean supportsDeferredForeignKeyConstraints() {
        return supportsDeferredConstraints;
    }

    /**
     * Return the name of the given foreign key action.
     */
    private String getActionName(int action) {
        switch (action) {
            case ForeignKey.ACTION_CASCADE:
                return "CASCADE";
            case ForeignKey.ACTION_NULL:
                return "SET NULL";
            case ForeignKey.ACTION_DEFAULT:
                return "SET DEFAULT";
            default:
                return null;
        }
    }

    /**
     * Whether this database supports the given foreign key delete action.
     */
    public boolean supportsDeleteAction(int action) {
        if (action == ForeignKey.ACTION_NONE)
            return true;
        if (!supportsForeignKeys)
            return false;
        switch (action) {
            case ForeignKey.ACTION_RESTRICT:
                return supportsRestrictDeleteAction;
            case ForeignKey.ACTION_CASCADE:
                return supportsCascadeDeleteAction;
            case ForeignKey.ACTION_NULL:
                return supportsNullDeleteAction;
            case ForeignKey.ACTION_DEFAULT:
                return supportsDefaultDeleteAction;
            default:
                return false;
        }
    }

    /**
     * Whether this database supports the given foreign key update action.
     */
    public boolean supportsUpdateAction(int action) {
        if (action == ForeignKey.ACTION_NONE)
            return true;
        if (!supportsForeignKeys)
            return false;
        switch (action) {
            case ForeignKey.ACTION_RESTRICT:
                return supportsRestrictUpdateAction;
            case ForeignKey.ACTION_CASCADE:
                return supportsCascadeUpdateAction;
            case ForeignKey.ACTION_NULL:
                return supportsNullUpdateAction;
            case ForeignKey.ACTION_DEFAULT:
                return supportsDefaultUpdateAction;
            default:
                return false;
        }
    }

    /**
     * Return the declaration SQL for the given unique constraint. This
     * method is used from within {@link #getCreateTableSQL}.
     * Returns	<code>CONSTRAINT &lt;name&gt; UNIQUE (&lt;col list&gt;)</code>
     * by default.
     */
    protected String getUniqueConstraintSQL(Unique unq) {
        if (!supportsUniqueConstraints
            || (unq.isDeferred() && !supportsDeferredUniqueConstraints()))
            return null;

        StringBuffer buf = new StringBuffer();
        if (unq.getName() != null
            && CONS_NAME_BEFORE.equals(constraintNameMode))
            buf.append("CONSTRAINT ").append(unq.getName()).append(" ");
        buf.append("UNIQUE ");
        if (unq.getName() != null && CONS_NAME_MID.equals(constraintNameMode))
            buf.append(unq.getName()).append(" ");
        buf.append("(").append(Strings.join(unq.getColumns(), ", ")).
            append(")");
        if (unq.isDeferred())
            buf.append(" INITIALLY DEFERRED");
        if (supportsDeferredUniqueConstraints())
            buf.append(" DEFERRABLE");
        if (unq.getName() != null
            && CONS_NAME_AFTER.equals(constraintNameMode))
            buf.append(" CONSTRAINT ").append(unq.getName());
        return buf.toString();
    }

    /**
     * Whether or not this dictionary supports deferred unique constraints.
     * This implementation returns {@link #supportsUniqueConstraints}.
     *
     * @since 1.1.0
     */
    protected boolean supportsDeferredUniqueConstraints() {
        return supportsDeferredConstraints;
    }

    /////////////////////
    // Database metadata
    /////////////////////

    /**
     * This method is used to filter system tables from database metadata.
     * Return true if the given table name represents a system table that
     * should not appear in the schema definition. By default, returns
     * true only if the given table is in the internal list of system tables,
     * or if the given schema is in the list of system schemas and is not
     * the target schema.
     *
     * @param name the table name
     * @param schema the table schema; may be null
     * @param targetSchema if true, then the given schema was listed by
     * the user as one of his schemas
     */
    public boolean isSystemTable(String name, String schema,
        boolean targetSchema) {
        if (systemTableSet.contains(name.toUpperCase()))
            return true;
        return !targetSchema && schema != null
            && systemSchemaSet.contains(schema.toUpperCase());
    }

    /**
     * This method is used to filter system indexes from database metadata.
     * Return true if the given index name represents a system index that
     * should not appear in the schema definition. Returns false by default.
     *
     * @param name the index name
     * @param table the index table
     */
    public boolean isSystemIndex(String name, Table table) {
        return false;
    }

    /**
     * This method is used to filter system sequences from database metadata.
     * Return true if the given sequence represents a system sequence that
     * should not appear in the schema definition. Returns true if system
     * schema by default.
     *
     * @param name the table name
     * @param schema the table schema; may be null
     * @param targetSchema if true, then the given schema was listed by
     * the user as one of his schemas
     */
    public boolean isSystemSequence(String name, String schema,
        boolean targetSchema) {
        return !targetSchema && schema != null
            && systemSchemaSet.contains(schema.toUpperCase());
    }

    /**
     * Reflect on the schema to find tables matching the given name pattern.
     */
    public Table[] getTables(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, Connection conn)
        throws SQLException {
        if (!supportsSchemaForGetTables)
            schemaName = null;
        else
            schemaName = getSchemaNameForMetadata(schemaName);

        String[] types = Strings.split(tableTypes, ",", 0);
        for (int i = 0; i < types.length; i++)
            types[i] = types[i].trim();

        beforeMetadataOperation(conn);
        ResultSet tables = null;
        try {
            tables = meta.getTables(getCatalogNameForMetadata(catalog),
                schemaName, getTableNameForMetadata(tableName), types);
            List tableList = new ArrayList();
            while (tables != null && tables.next())
                tableList.add(newTable(tables));
            return (Table[]) tableList.toArray(new Table[tableList.size()]);
        } finally {
            if (tables != null)
                try {
                    tables.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Create a new table from the information in the schema metadata.
     */
    protected Table newTable(ResultSet tableMeta)
        throws SQLException {
        Table t = new Table();
        t.setName(tableMeta.getString("TABLE_NAME"));
        return t;
    }

    /**
     * Reflect on the schema to find sequences matching the given name pattern.
     * Returns an empty array by default, as there is no standard way to
     * retrieve a list of sequences.
     */
    public Sequence[] getSequences(DatabaseMetaData meta, String catalog,
        String schemaName, String sequenceName, Connection conn)
        throws SQLException {
        String str = getSequencesSQL(schemaName, sequenceName);
        if (str == null)
            return new Sequence[0];

        PreparedStatement stmnt = prepareStatement(conn, str);        
        ResultSet rs = null;
        try {
            int idx = 1;
            if (schemaName != null)
                stmnt.setString(idx++, schemaName.toUpperCase());
            if (sequenceName != null)
                stmnt.setString(idx++, sequenceName);

            rs = executeQuery(conn, stmnt, str);
            return getSequence(rs);            
         } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException se) {
                }
            if (stmnt != null)    
                try {
                    stmnt.close();
                } catch (SQLException se) {
                }
        }
    }

    /**
     * Create a new sequence from the information in the schema metadata.
     */
    protected Sequence newSequence(ResultSet sequenceMeta)
        throws SQLException {
        Sequence seq = new Sequence();
        seq.setSchemaName(sequenceMeta.getString("SEQUENCE_SCHEMA"));
        seq.setName(sequenceMeta.getString("SEQUENCE_NAME"));
        return seq;
    }

    /**
     * Return the SQL needed to select the list of sequences.
     */
    protected String getSequencesSQL(String schemaName, String sequenceName) {
        return null;
    }

    /**
     * Reflect on the schema to find columns matching the given table and
     * column patterns.
     */
    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        if (tableName == null && !supportsNullTableForGetColumns)
            return null;

        if (!supportsSchemaForGetColumns)
            schemaName = null;
        else
            schemaName = getSchemaNameForMetadata(schemaName);

        beforeMetadataOperation(conn);
        ResultSet cols = null;
        try {
            cols = meta.getColumns(getCatalogNameForMetadata(catalog),
                schemaName, getTableNameForMetadata(tableName),
                getColumnNameForMetadata(columnName));

            List columnList = new ArrayList();
            while (cols != null && cols.next())
                columnList.add(newColumn(cols));
            return (Column[]) columnList.toArray
                (new Column[columnList.size()]);
        } finally {
            if (cols != null)
                try {
                    cols.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Create a new column from the information in the schema metadata.
     */
    protected Column newColumn(ResultSet colMeta)
        throws SQLException {
        Column c = new Column();
        c.setSchemaName(colMeta.getString("TABLE_SCHEM"));
        c.setTableName(colMeta.getString("TABLE_NAME"));
        c.setName(colMeta.getString("COLUMN_NAME"));
        c.setType(colMeta.getInt("DATA_TYPE"));
        c.setTypeName(colMeta.getString("TYPE_NAME"));
        c.setSize(colMeta.getInt("COLUMN_SIZE"));
        c.setDecimalDigits(colMeta.getInt("DECIMAL_DIGITS"));
        c.setNotNull(colMeta.getInt("NULLABLE")
            == DatabaseMetaData.columnNoNulls);

        String def = colMeta.getString("COLUMN_DEF");
        if (!StringUtils.isEmpty(def) && !"null".equalsIgnoreCase(def))
            c.setDefaultString(def);
        return c;
    }

    /**
     * Reflect on the schema to find primary keys for the given table pattern.
     */
    public PrimaryKey[] getPrimaryKeys(DatabaseMetaData meta,
        String catalog, String schemaName, String tableName, Connection conn)
        throws SQLException {
        if (useGetBestRowIdentifierForPrimaryKeys)
            return getPrimaryKeysFromBestRowIdentifier(meta, catalog,
                schemaName, tableName, conn);
        return getPrimaryKeysFromGetPrimaryKeys(meta, catalog,
            schemaName, tableName, conn);
    }

    /**
     * Reflect on the schema to find primary keys for the given table pattern.
     */
    protected PrimaryKey[] getPrimaryKeysFromGetPrimaryKeys
        (DatabaseMetaData meta, String catalog, String schemaName,
            String tableName, Connection conn)
        throws SQLException {
        if (tableName == null && !supportsNullTableForGetPrimaryKeys)
            return null;

        beforeMetadataOperation(conn);
        ResultSet pks = null;
        try {
            pks = meta.getPrimaryKeys(getCatalogNameForMetadata(catalog),
                getSchemaNameForMetadata(schemaName),
                getTableNameForMetadata(tableName));

            List pkList = new ArrayList();
            while (pks != null && pks.next())
                pkList.add(newPrimaryKey(pks));
            return (PrimaryKey[]) pkList.toArray
                (new PrimaryKey[pkList.size()]);
        } finally {
            if (pks != null)
                try {
                    pks.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Create a new primary key from the information in the schema metadata.
     */
    protected PrimaryKey newPrimaryKey(ResultSet pkMeta)
        throws SQLException {
        PrimaryKey pk = new PrimaryKey();
        pk.setSchemaName(pkMeta.getString("TABLE_SCHEM"));
        pk.setTableName(pkMeta.getString("TABLE_NAME"));
        pk.setColumnName(pkMeta.getString("COLUMN_NAME"));
        pk.setName(pkMeta.getString("PK_NAME"));
        return pk;
    }

    /**
     * Reflect on the schema to find primary keys for the given table pattern.
     */
    protected PrimaryKey[] getPrimaryKeysFromBestRowIdentifier
        (DatabaseMetaData meta, String catalog, String schemaName,
            String tableName, Connection conn)
        throws SQLException {
        if (tableName == null)
            return null;

        beforeMetadataOperation(conn);
        ResultSet pks = null;
        try {
            pks = meta.getBestRowIdentifier(catalog, schemaName,
                tableName, 0, false);

            List pkList = new ArrayList();
            while (pks != null && pks.next()) {
                PrimaryKey pk = new PrimaryKey();
                pk.setSchemaName(schemaName);
                pk.setTableName(tableName);
                pk.setColumnName(pks.getString("COLUMN_NAME"));
                pkList.add(pk);
            }
            return (PrimaryKey[]) pkList.toArray
                (new PrimaryKey[pkList.size()]);
        } finally {
            if (pks != null)
                try {
                    pks.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Reflect on the schema to find indexes matching the given table pattern.
     */
    public Index[] getIndexInfo(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, boolean unique,
        boolean approx, Connection conn)
        throws SQLException {
        if (tableName == null && !supportsNullTableForGetIndexInfo)
            return null;

        beforeMetadataOperation(conn);
        ResultSet indexes = null;
        try {
            indexes = meta.getIndexInfo(getCatalogNameForMetadata(catalog),
                getSchemaNameForMetadata(schemaName),
                getTableNameForMetadata(tableName), unique, approx);

            List indexList = new ArrayList();
            while (indexes != null && indexes.next())
                indexList.add(newIndex(indexes));
            return (Index[]) indexList.toArray(new Index[indexList.size()]);
        } finally {
            if (indexes != null)
                try {
                    indexes.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Create a new index from the information in the schema metadata.
     */
    protected Index newIndex(ResultSet idxMeta)
        throws SQLException {
        Index idx = new Index();
        idx.setSchemaName(idxMeta.getString("TABLE_SCHEM"));
        idx.setTableName(idxMeta.getString("TABLE_NAME"));
        idx.setColumnName(idxMeta.getString("COLUMN_NAME"));
        idx.setName(idxMeta.getString("INDEX_NAME"));
        idx.setUnique(!idxMeta.getBoolean("NON_UNIQUE"));
        return idx;
    }

    /**
     * Reflect on the schema to return foreign keys imported by the given
     * table pattern.
     */
    public ForeignKey[] getImportedKeys(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, Connection conn)
        throws SQLException {
        if (!supportsForeignKeys)
            return null;
        if (tableName == null && !supportsNullTableForGetImportedKeys)
            return null;

        beforeMetadataOperation(conn);
        ResultSet keys = null;
        try {
            keys = meta.getImportedKeys(getCatalogNameForMetadata(catalog),
                getSchemaNameForMetadata(schemaName),
                getTableNameForMetadata(tableName));

            List importedKeyList = new ArrayList();
            while (keys != null && keys.next())
                importedKeyList.add(newForeignKey(keys));
            return (ForeignKey[]) importedKeyList.toArray
                (new ForeignKey[importedKeyList.size()]);
        } finally {
            if (keys != null)
                try {
                    keys.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Create a new foreign key from the information in the schema metadata.
     */
    protected ForeignKey newForeignKey(ResultSet fkMeta)
        throws SQLException {
        ForeignKey fk = new ForeignKey();
        fk.setSchemaName(fkMeta.getString("FKTABLE_SCHEM"));
        fk.setTableName(fkMeta.getString("FKTABLE_NAME"));
        fk.setColumnName(fkMeta.getString("FKCOLUMN_NAME"));
        fk.setName(fkMeta.getString("FK_NAME"));
        fk.setPrimaryKeySchemaName(fkMeta.getString("PKTABLE_SCHEM"));
        fk.setPrimaryKeyTableName(fkMeta.getString("PKTABLE_NAME"));
        fk.setPrimaryKeyColumnName(fkMeta.getString("PKCOLUMN_NAME"));
        fk.setKeySequence(fkMeta.getShort("KEY_SEQ"));
        fk.setDeferred(fkMeta.getShort("DEFERRABILITY")
            == DatabaseMetaData.importedKeyInitiallyDeferred);

        int del = fkMeta.getShort("DELETE_RULE");
        switch (del) {
            case DatabaseMetaData.importedKeySetNull:
                fk.setDeleteAction(ForeignKey.ACTION_NULL);
                break;
            case DatabaseMetaData.importedKeySetDefault:
                fk.setDeleteAction(ForeignKey.ACTION_DEFAULT);
                break;
            case DatabaseMetaData.importedKeyCascade:
                fk.setDeleteAction(ForeignKey.ACTION_CASCADE);
                break;
            default:
                fk.setDeleteAction(ForeignKey.ACTION_RESTRICT);
                break;
        }
        return fk;
    }

    /**
     * Returns the table name that will be used for obtaining information
     * from {@link DatabaseMetaData}.
     */
    protected String getTableNameForMetadata(String tableName) {
        return convertSchemaCase(tableName);
    }

    /**
     * Returns the schema name that will be used for obtaining information
     * from {@link DatabaseMetaData}.
     */
    protected String getSchemaNameForMetadata(String schemaName) {
        if (schemaName == null)
            schemaName = conf.getSchema();
        return convertSchemaCase(schemaName);
    }

    /**
     * Returns the catalog name that will be used for obtaining information
     * from {@link DatabaseMetaData}.
     */
    protected String getCatalogNameForMetadata(String catalogName) {
        return convertSchemaCase(catalogName);
    }

    /**
     * Returns the column name that will be used for obtaining information
     * from {@link DatabaseMetaData}.
     */
    protected String getColumnNameForMetadata(String columnName) {
        return convertSchemaCase(columnName);
    }

    /**
     * Convert the specified schema name to a name that the database will
     * be able to understand.
     */
    protected String convertSchemaCase(String objectName) {
        if (objectName == null)
            return null;

        String scase = getSchemaCase();
        if (SCHEMA_CASE_LOWER.equals(scase))
            return objectName.toLowerCase();
        if (SCHEMA_CASE_PRESERVE.equals(scase))
            return objectName;
        return objectName.toUpperCase();
    }
    
    /**
     * Return DB specific schemaCase 
     */
    public String getSchemaCase(){
        return schemaCase;
    }
        
    /**
     * Prepared the connection for metadata operations.
     */
    private void beforeMetadataOperation(Connection c) {
        if (requiresAutoCommitForMetaData) {
            try {
                c.rollback();
            } catch (SQLException sqle) {
            }
            try {
                if (!c.getAutoCommit())
                    c.setAutoCommit(true);
            } catch (SQLException sqle) {
            }
        }
    }

    /////////////////////////////
    // Sequences and Auto-Assign
    /////////////////////////////

    /**
     * Return the last generated value for the given column.
     * Throws an exception by default if {@link #lastGeneratedKeyQuery} is null.
     */
    public Object getGeneratedKey(Column col, Connection conn)
        throws SQLException {
        if (lastGeneratedKeyQuery == null)
            throw new StoreException(_loc.get("no-auto-assign"));

        // replace things like "SELECT MAX({0}) FROM {1}"
        String query = lastGeneratedKeyQuery;
        if (query.indexOf('{') != -1) // only if the token is in the string
        {
            query = MessageFormat.format(query, new Object[]{
                col.getName(), getFullName(col.getTable(), false),
                getGeneratedKeySequenceName(col),
            });
        }

        PreparedStatement stmnt = prepareStatement(conn, query);
        ResultSet rs = null;
        try {
            rs = executeQuery(conn, stmnt, query);
            return getKey(rs, col);
        } finally {
            if (rs != null)
                try { rs.close(); } catch (SQLException se) {}
            if (stmnt != null)    
                try { stmnt.close(); } catch (SQLException se) {} 
        }
    }

    /**
     * Return the sequence name used by databases for the given autoassigned
     * column. This is only used by databases that require an explicit name
     * to be used for auto-assign support.
     */
    protected String getGeneratedKeySequenceName(Column col) {
        String tname = col.getTableName();
        String cname = col.getName();
        int max = maxAutoAssignNameLength;
        int extraChars = -max + tname.length() + 1 // <tname> + '_'
            + cname.length() + 4; // <cname> + '_SEQ'
        if (extraChars > 0) {
            // this assumes that tname is longer than extraChars
            tname = tname.substring(0, tname.length() - extraChars);
        }
        StringBuffer buf = new StringBuffer(max);
        buf.append(tname).append("_").append(cname).append("_SEQ");
        return buf.toString();
    }

    ///////////////////////////////
    // Configurable implementation
    ///////////////////////////////

    public void setConfiguration(Configuration conf) {
        this.conf = (JDBCConfiguration) conf;
        this.log = this.conf.getLog(JDBCConfiguration.LOG_JDBC);

        // warn about unsupported dicts
        if (log.isWarnEnabled() && !isSupported())
            log.warn(_loc.get("dict-not-supported", getClass()));
    }

    private boolean isSupported() {
        // if this is a custom dict, traverse to whatever openjpa dict it extends
        Class c = getClass();
        while (!c.getName().startsWith("org.apache.openjpa."))
            c = c.getSuperclass();

        // the generic dbdictionary is not considered a supported dict; all
        // other concrete dictionaries are
        if (c == DBDictionary.class)
            return false;
        return true;
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
        // initialize the set of reserved SQL92 words from resource
        InputStream in = DBDictionary.class.getResourceAsStream
            ("sql-keywords.rsrc");
        try {
            String keywords = new BufferedReader(new InputStreamReader(in)).
                readLine();
            in.close();
            reservedWordSet.addAll(Arrays.asList(Strings.split
                (keywords, ",", 0)));
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        } finally {
            try { in.close(); } catch (IOException e) {}
        }

        // add additional reserved words set by user
        if (reservedWords != null)
            reservedWordSet.addAll(Arrays.asList(Strings.split
                (reservedWords.toUpperCase(), ",", 0)));

        // add system schemas set by user
        if (systemSchemas != null)
            systemSchemaSet.addAll(Arrays.asList(Strings.split
                (systemSchemas.toUpperCase(), ",", 0)));

        // add system tables set by user
        if (systemTables != null)
            systemTableSet.addAll(Arrays.asList(Strings.split
                (systemTables.toUpperCase(), ",", 0)));

        // add fixed size type names set by the user
        if (fixedSizeTypeNames != null)
            fixedSizeTypeNameSet.addAll(Arrays.asList(Strings.split
                (fixedSizeTypeNames.toUpperCase(), ",", 0)));
        
        // if user has unset sequence sql, null it out so we know sequences
        // aren't supported
        nextSequenceQuery = StringUtils.trimToNull(nextSequenceQuery);
        
        if (selectWords != null)
            selectWordSet.addAll(Arrays.asList(Strings.split(selectWords
                    .toUpperCase(), ",", 0)));
        
        // initialize the error codes
        SQLErrorCodeReader codeReader = new SQLErrorCodeReader();
        String rsrc = "sql-error-state-codes.xml";
        InputStream stream = getClass().getResourceAsStream(rsrc);
        String dictionaryClassName = getClass().getName();
        if (stream == null) { // User supplied dictionary but no error codes xml
        	stream = DBDictionary.class.getResourceAsStream(rsrc); // use default
        	dictionaryClassName = getClass().getSuperclass().getName();
        }
        codeReader.parse(stream, dictionaryClassName, this);
    }
    
    public void addErrorCode(int errorType, String errorCode) {
    	if (errorCode == null || errorCode.trim().length() == 0)
    		return;
		Set<String> codes = sqlStateCodes.get(errorType);
    	if (codes == null) {
    		codes = new HashSet<String>();
    		codes.add(errorCode.trim());
    		sqlStateCodes.put(errorType, codes);
    	} else {
    		codes.add(errorCode.trim());
    	}
    }
    
    //////////////////////////////////////
    // ConnectionDecorator implementation
    //////////////////////////////////////

    /**
     * Decorate the given connection if needed. Some databases require special
     * handling for JDBC bugs. This implementation issues any
     * {@link #initializationSQL} that has been set for the dictionary but
     * does not decorate the connection.
     */
    public Connection decorate(Connection conn)
        throws SQLException {
        if (!connected)
            connectedConfiguration(conn);
        if (!StringUtils.isEmpty(initializationSQL)) {
            PreparedStatement stmnt = null;
            try {
                stmnt = conn.prepareStatement(initializationSQL);
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
        return conn;
    }

    /**
     * Implementation of the
     * {@link LoggingConnectionDecorator.SQLWarningHandler} interface
     * that allows customization of the actions to perform when a
     * {@link SQLWarning} occurs at any point on a {@link Connection},
     * {@link Statement}, or {@link ResultSet}. This method may
     * be used determine those warnings the application wants to
     * consider critical failures, and throw the warning in those
     * cases. By default, this method does nothing.
     *
     * @see LoggingConnectionDecorator#setWarningAction
     * @see LoggingConnectionDecorator#setWarningHandler
     */
    public void handleWarning(SQLWarning warning)
        throws SQLException {
    }

    /**
     * Return a new exception that wraps <code>causes</code>.
     * However, the details of exactly what type of exception is returned can
     * be determined by the implementation. This may take into account
     * DB-specific exception information in <code>causes</code>.
     */
    public OpenJPAException newStoreException(String msg, SQLException[] causes,
        Object failed) {
    	if (causes != null && causes.length > 0) {
    		OpenJPAException ret = narrow(msg, causes[0]);
    		ret.setFailedObject(failed).setNestedThrowables(causes);
    		return ret;
    	}
        return new StoreException(msg).setFailedObject(failed).
            setNestedThrowables(causes);
    }
    
    /**
     * Gets the subtype of StoreException by matching the given SQLException's
     * error state code to the list of error codes supplied by the dictionary.
     * Returns -1 if no matching code can be found.
     */
    OpenJPAException narrow(String msg, SQLException ex) {
    	String errorState = ex.getSQLState();
    	int errorType = StoreException.GENERAL;
    	for (Integer type : sqlStateCodes.keySet()) {
    		Set<String> erroStates = sqlStateCodes.get(type);
    		if (erroStates != null && erroStates.contains(errorState)) {
    			errorType = type;
    			break;
    		}
    	}
    	switch (errorType) {
	    	case StoreException.LOCK: 
	            return new LockException(msg);
	    	case StoreException.OBJECT_EXISTS:
	            return new ObjectExistsException(msg);
	    	case StoreException.OBJECT_NOT_FOUND:
	            return new ObjectNotFoundException(msg);
	    	case StoreException.OPTIMISTIC:
	            return new OptimisticException(msg);
	    	case StoreException.REFERENTIAL_INTEGRITY: 
	            return new ReferentialIntegrityException(msg);
	        default:
	            return new StoreException(msg);
        }
    }
    
    /**
     * Closes the specified {@link DataSource} and releases any
     * resources associated with it.
     *
     * @param dataSource the DataSource to close
     */
    public void closeDataSource(DataSource dataSource) {
        DataSourceFactory.closeDataSource(dataSource);
    }

    /**
     * Used by some mappings to represent data that has already been
     * serialized so that we don't have to serialize multiple times.
     */
    public static class SerializedData {

        public final byte[] bytes;

        public SerializedData(byte[] bytes) {
            this.bytes = bytes;
        }
    }
    
    /**
     * Return version column name
     * @param column
     * @param tableAlias : this is needed for platform specific version column
     * @return
     */
    public String getVersionColumn(Column column, String tableAlias) {
        return column.toString();
    }
    
    public void insertBlobForStreamingLoad(Row row, Column col, 
        JDBCStore store, Object ob, Select sel) throws SQLException {
        if (ob != null) {
            row.setBinaryStream(col, 
                new ByteArrayInputStream(new byte[0]), 0);
        } else {
            row.setNull(col);
        }
    }
    
    public void insertClobForStreamingLoad(Row row, Column col, Object ob)
    throws SQLException {
        if (ob != null) {
        row.setCharacterStream(col,
                new CharArrayReader(new char[0]), 0);
        } else {
            row.setNull(col);
        }
    }
    
    public void updateBlob(Select sel, JDBCStore store, InputStream is)
        throws SQLException {
        SQLBuffer sql = sel.toSelect(true, store.getFetchConfiguration());
        ResultSet res = null;
        Connection conn = store.getConnection();
        PreparedStatement stmnt = null;
        try {
            stmnt = sql.prepareStatement(conn, store.getFetchConfiguration(),
                ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
            res = stmnt.executeQuery();
            if (!res.next()) {
                throw new InternalException(_loc.get("stream-exception"));
            }
            Blob blob = res.getBlob(1);
            OutputStream os = blob.setBinaryStream(1);
            copy(is, os);
            os.close();
            res.updateBlob(1, blob);
            res.updateRow();

        } catch (IOException ioe) {
            throw new StoreException(ioe);
        } finally {
            if (res != null)
                try { res.close (); } catch (SQLException e) {}
            if (stmnt != null)
                try { stmnt.close (); } catch (SQLException e) {}
            if (conn != null)
                try { conn.close (); } catch (SQLException e) {}
        }
    }
    
    public void updateClob(Select sel, JDBCStore store, Reader reader)
        throws SQLException {
        SQLBuffer sql = sel.toSelect(true, store.getFetchConfiguration());
        ResultSet res = null;
        Connection conn = store.getConnection();
        PreparedStatement stmnt = null;
        try {
            stmnt = sql.prepareStatement(conn, store.getFetchConfiguration(),
                ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
            res = stmnt.executeQuery();
            if (!res.next()) {
                throw new InternalException(_loc.get("stream-exception"));
            }
            Clob clob = res.getClob(1);
            Writer writer = clob.setCharacterStream(1);
            copy(reader, writer);
            writer.close();
            res.updateClob(1, clob);
            res.updateRow();

        } catch (IOException ioe) {
            throw new StoreException(ioe);
        } finally {
            if (res != null) 
                try { res.close (); } catch (SQLException e) {}
            if (stmnt != null) 
                try { stmnt.close (); } catch (SQLException e) {}
            if (conn != null) 
                try { conn.close (); } catch (SQLException e) {}
        }    
    }
    
    protected long copy(InputStream in, OutputStream out) throws IOException {
        byte[] copyBuffer = new byte[blobBufferSize];
        long bytesCopied = 0;
        int read = -1;

        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
            out.write(copyBuffer, 0, read);
            bytesCopied += read;
        }
        return bytesCopied;
    }
    
    protected long copy(Reader reader, Writer writer) throws IOException {
        char[] copyBuffer = new char[clobBufferSize];
        long bytesCopied = 0;
        int read = -1;

        while ((read = reader.read(copyBuffer, 0, copyBuffer.length)) != -1) {
            writer.write(copyBuffer, 0, read);
            bytesCopied += read;
        }

        return bytesCopied;
    }
    
    /**
     * Attach CAST to the current function if necessary
     * 
     * @param val operand value
     * @parma func the sql function statement
     * @return a String with the correct CAST function syntax
     */
    public String getCastFunction(Val val, String func) {
        return func;
    }

    /**
     * Return the correct CAST function syntax.  This should be overriden by subclasses
     * that need access to the Column information.
     * 
     * @param val operand of cast
     * @param func original string
     * @param col database column
     * @return a String with the correct CAST function syntax
     */
    public String getCastFunction(Val val, String func, Column col) {
    	return getCastFunction (val, func);
    }
    
    /**
     * Create an index if necessary for some database tables
     */
    public void createIndexIfNecessary(Schema schema, String table,
            Column pkColumn) {
    }
    
    /**
     * Return the batchLimit
     */
    public int getBatchLimit(){
        return batchLimit;
    }
    
    /**
     * Set the batchLimit value
     */
    public void setBatchLimit(int limit){
        batchLimit = limit;
    }
    
    /**
     * Validate the batch process. In some cases, we can't batch the statements
     * due to some restrictions. For example, if the GeneratedType=IDENTITY,
     * we have to disable the batch process because we need to get the ID value
     * right away for the in-memory entity to use.
     */
    public boolean validateBatchProcess(RowImpl row, Column[] autoAssign,
            OpenJPAStateManager  sm, ClassMapping cmd ) {
        boolean disableBatch = false;
        if (getBatchLimit()== 0) return false;
        if (autoAssign != null && sm != null) {
            FieldMetaData[] fmd = cmd.getPrimaryKeyFields();
            int i = 0;
            while (!disableBatch && i < fmd.length) {
                if (fmd[i].getValueStrategy() == ValueStrategies.AUTOASSIGN)
                    disableBatch = true;
                i++;
            }
        }
        // go to each Dictionary to validate the batch capability
        if (!disableBatch)
            disableBatch = validateDBSpecificBatchProcess(disableBatch, row, 
                autoAssign, sm, cmd);
        return disableBatch;
    }
    
    /**
     * Allow each Dictionary to validate its own batch process. 
     */
    public boolean validateDBSpecificBatchProcess (boolean disableBatch, 
            RowImpl row, Column[] autoAssign, 
            OpenJPAStateManager  sm, ClassMapping cmd ) {
        return disableBatch;
    }
    
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of executing query.
     */
    protected ResultSet executeQuery(Connection conn, PreparedStatement stmnt, String sql 
        ) throws SQLException {
        return stmnt.executeQuery();
    }
            
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of preparing statement.
     */
    protected PreparedStatement prepareStatement(Connection conn, String sql)
        throws SQLException {
        return conn.prepareStatement(sql);
    }    
 
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of getting sequence from the result set.
     */
    protected Sequence[] getSequence(ResultSet rs) throws SQLException {
        List seqList = new ArrayList();
        while (rs != null && rs.next())
            seqList.add(newSequence(rs));
        return (Sequence[]) seqList.toArray(new Sequence[seqList.size()]);
    }
    
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of getting key from the result set.
     */
    protected Object getKey (ResultSet rs, Column col) throws SQLException {
        if (!rs.next())
            throw new StoreException(_loc.get("no-genkey"));
        Object key = rs.getObject(1);
        if (key == null)
            log.warn(_loc.get("invalid-genkey", col));
        return key;        
    }
    
    /**
     * This method is to provide override for non-JDBC or JDBC-like 
     * implementation of calculating value.
     */
    protected void calculateValue(Val val, Select sel, ExpContext ctx, 
        ExpState state, Path path, ExpState pathState) {
        val.calculateValue(sel, ctx, state, (Val) path, pathState);
    }

    /**
     * Determine whether the provided <code>sql</code> may be treated as a 
     * select statement on this database.
     *  
     * @param sql   A sql statement. 
     * 
     * @return true if <code>sql</code> represents a select statement.
     */
    public boolean isSelect(String sql) {
        Iterator i = selectWordSet.iterator();
        String cur;
        while (i.hasNext()) {
            cur = (String) i.next();
            if (sql.length() >= cur.length()
                    && sql.substring(0, cur.length()).equalsIgnoreCase(cur)) {
                return true;
            }
        }
        return false;
    }

    public boolean needsToCreateIndex(Index idx, Table table) {
        return true;
    }

    /**
     * Return batched statements update succes count
     * @param ps A PreparedStatement
     * @return return update count
     */
    public int getBatchUpdateCount(PreparedStatement ps) throws SQLException {
        return 0;
    }
    
    public boolean getTrimSchemaName() { 
        return trimSchemaName;
    }
    
    public void setTrimSchemaName(boolean trimSchemaName) { 
        this.trimSchemaName = trimSchemaName; 
    }
    
    public void deleteStream(JDBCStore store, Select sel) throws SQLException {
        // Do nothing
    }
    
    /**
     * Return parameter marker for INSERT and UPDATE statements.
     * Usually it is <code>?</code> but some database-specific types might require customization.
     * 
     * @param col column definition
     * @param val value to be inserted/updated
     * @return parameter marker
    */
    public String getMarkerForInsertUpdate(Column col, Object val) {
       return "?";
    }
}

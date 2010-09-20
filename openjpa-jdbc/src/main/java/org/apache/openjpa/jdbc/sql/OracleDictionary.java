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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.jdbc.DelegatingDatabaseMetaData;
import org.apache.openjpa.lib.jdbc.DelegatingPreparedStatement;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UserException;

import serp.util.Numbers;

/**
 * Dictionary for Oracle.
 */
public class OracleDictionary
    extends DBDictionary {

    public static final String SELECT_HINT = "openjpa.hint.OracleSelectHint";
    public static final String VENDOR_ORACLE = "oracle";

    private static final int BEHAVE_OTHER = 0;
    private static final int BEHAVE_ORACLE = 1;
    private static final int BEHAVE_DATADIRECT31 = 2;

    private static Blob EMPTY_BLOB = null;
    private static Clob EMPTY_CLOB = null;

    private static final Localizer _loc = Localizer.forPackage
        (OracleDictionary.class);

    /**
     * If true, then simulate auto-assigned values in Oracle by
     * using a trigger that inserts a sequence value into the
     * primary key value when a row is inserted.
     */
    public boolean useTriggersForAutoAssign = false;

    /**
     * The global sequence name to use for autoassign simulation.
     */
    public String autoAssignSequenceName = null;

    /**
     * Flag to use OpenJPA 0.3 style naming for auto assign sequence name and
     * trigger name for backwards compatibility.
     */
    public boolean openjpa3GeneratedKeyNames = false;

    /**
     * If true, then OpenJPA will attempt to use the special
     * OraclePreparedStatement.setFormOfUse method to
     * configure statements that it detects are operating on unicode fields.
     */
    public boolean useSetFormOfUseForUnicode = true;

    // some oracle drivers have problems with select for update; warn the
    // first time locking is attempted
    private boolean _checkedUpdateBug = false;
    private boolean _warnedCharColumn = false;
    private boolean _warnedNcharColumn = false;
    private int _driverBehavior = -1;

    // cache lob methods
    private Method _putBytes = null;
    private Method _putString = null;
    private Method _putChars = null;
    
    // batch limit
    private int defaultBatchLimit = 100;

    public OracleDictionary() {
        platform = "Oracle";
        validationSQL = "SELECT SYSDATE FROM DUAL";
        nextSequenceQuery = "SELECT {0}.NEXTVAL FROM DUAL";
        stringLengthFunction = "LENGTH({0})";
        joinSyntax = SYNTAX_DATABASE;
        maxTableNameLength = 30;
        maxColumnNameLength = 30;
        maxIndexNameLength = 30;
        maxConstraintNameLength = 30;
        maxEmbeddedBlobSize = 4000;
        maxEmbeddedClobSize = 4000;
        inClauseLimit = 1000;

        supportsDeferredConstraints = true;
        supportsLockingWithDistinctClause = false;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;

        systemSchemaSet.addAll(Arrays.asList(new String[]{
            "CTXSYS", "MDSYS", "SYS", "SYSTEM", "WKSYS", "WMSYS", "XDB",
        }));

        supportsXMLColumn = true;
        xmlTypeName = "XMLType";
        bigintTypeName = "NUMBER{0}";
        bitTypeName = "NUMBER{0}";
        decimalTypeName = "NUMBER{0}";
        doubleTypeName = "NUMBER{0}";
        integerTypeName = "NUMBER{0}";
        numericTypeName = "NUMBER{0}";
        smallintTypeName = "NUMBER{0}";
        tinyintTypeName = "NUMBER{0}";
        longVarcharTypeName = "LONG";
        binaryTypeName = "BLOB";
        varbinaryTypeName = "BLOB";
        longVarbinaryTypeName = "BLOB";
        timeTypeName = "DATE";
        varcharTypeName = "VARCHAR2{0}";
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "LONG RAW", "RAW", "LONG", "REF",
        }));
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "ACCESS", "AUDIT", "CLUSTER", "COMMENT", "COMPRESS", "EXCLUSIVE",
            "FILE", "IDENTIFIED", "INCREMENT", "INDEX", "INITIAL", "LOCK",
            "LONG", "MAXEXTENTS", "MINUS", "MODE", "NOAUDIT", "NOCOMPRESS",
            "NOWAIT", "OFFLINE", "ONLINE", "PCTFREE", "ROW",
        }));

        substringFunctionName = "SUBSTR";
        super.setBatchLimit(defaultBatchLimit);
        selectWordSet.add("WITH");
        reportsSuccessNoInfoOnBatchUpdates = true;
        requiresSearchStringEscapeForLike = false;
    }

    public void endConfiguration() {
        super.endConfiguration();
        if (useTriggersForAutoAssign)
            supportsAutoAssign = true;
    }

    public void connectedConfiguration(Connection conn)
        throws SQLException {
        super.connectedConfiguration(conn);
        if (driverVendor == null) {
            DatabaseMetaData meta = conn.getMetaData();
            String url = (meta.getURL() == null) ? "" : meta.getURL();
            String driverName = meta.getDriverName();
            String metadataClassName;
            if (meta instanceof DelegatingDatabaseMetaData)
                metadataClassName = ((DelegatingDatabaseMetaData) meta).
                    getInnermostDelegate().getClass().getName();
            else
                metadataClassName = meta.getClass().getName();

            // check both the driver class name and the URL for known patterns
            if (metadataClassName.startsWith("oracle.")
                || url.indexOf("jdbc:oracle:") != -1
                || "Oracle JDBC driver".equals(driverName)) {
                driverVendor = VENDOR_ORACLE + meta.getDriverMajorVersion()
                    + meta.getDriverMinorVersion();

                String productVersion = meta.getDatabaseProductVersion()
                    .split("Release ",0)[1].split("\\.",0)[0];
                int release = Integer.parseInt(productVersion);
                
                // warn sql92
                if (release <= 8) {
                    if (joinSyntax == SYNTAX_SQL92 && log.isWarnEnabled())
                        log.warn(_loc.get("oracle-syntax"));
                    joinSyntax = SYNTAX_DATABASE;
                    dateTypeName = "DATE"; // added oracle 9
                    timestampTypeName = "DATE"; // added oracle 9
                    supportsXMLColumn = false;
                }
                    // select of an xml column requires ".getStringVal()"
                    // suffix. eg. t0.xmlcol.getStringVal()
                    getStringVal = ".getStringVal()";
            } else if (metadataClassName.startsWith("com.ddtek.")
                || url.indexOf("jdbc:datadirect:oracle:") != -1
                || "Oracle".equals(driverName)) {
                driverVendor = VENDOR_DATADIRECT + meta.getDriverMajorVersion()
                    + meta.getDriverMinorVersion();
            } else
                driverVendor = VENDOR_OTHER;
        }
        cacheDriverBehavior(driverVendor);
    }

    /**
     * Cache constant for drivers with behaviors we have to deal with.
     */
    private void cacheDriverBehavior(String driverVendor) {
        if (_driverBehavior != -1)
            return;

        driverVendor = driverVendor.toLowerCase();
        if (driverVendor.startsWith(VENDOR_ORACLE))
            _driverBehavior = BEHAVE_ORACLE;
        else if (driverVendor.equals(VENDOR_DATADIRECT + "30")
            || driverVendor.equals(VENDOR_DATADIRECT + "31"))
            _driverBehavior = BEHAVE_DATADIRECT31;
        else
            _driverBehavior = BEHAVE_OTHER;
    }

    /**
     * Ensure that the driver vendor has been set, and if not, set it now.
     */
    public void ensureDriverVendor() {
        if (driverVendor != null) {
            cacheDriverBehavior(driverVendor);
            return;
        }

        if (log.isInfoEnabled())
            log.info(_loc.get("oracle-connecting-for-driver"));
        Connection conn = null;
        try {
            conn = conf.getDataSource2(null).getConnection();
            connectedConfiguration(conn);
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, this);
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException se) {
                }
        }
    }

    public boolean supportsLocking(Select sel) {
        if (!super.supportsLocking(sel))
            return false;
        return !requiresSubselectForRange(sel.getStartIndex(),
            sel.getEndIndex(), sel.isDistinct(), sel.getOrdering());
    }

    protected SQLBuffer getSelects(Select sel, boolean distinctIdentifiers,
        boolean forUpdate) {
        // if range doesn't require a subselect can use super
        if (!requiresSubselectForRange(sel.getStartIndex(),
            sel.getEndIndex(), sel.isDistinct(), sel.getOrdering()))
            return super.getSelects(sel, distinctIdentifiers, forUpdate);

        // if there are no joins involved or we're using a from select so
        // that all cols already have unique aliases, can use super
        if (sel.getFromSelect() != null || sel.getTableAliases().size() < 2)
            return super.getSelects(sel, distinctIdentifiers, forUpdate);

        // since none of the conditions above were met, we're dealing with
        // a select that uses joins and requires subselects to select the
        // proper range; alias all column values so that they are unique within
        // the subselect
        SQLBuffer selectSQL = new SQLBuffer(this);
        List aliases;
        if (distinctIdentifiers)
            aliases = sel.getIdentifierAliases();
        else
            aliases = sel.getSelectAliases();

        Object alias;
        int i = 0;
        for (Iterator itr = aliases.iterator(); itr.hasNext(); i++) {
            alias = itr.next();
            if (alias instanceof SQLBuffer)
                selectSQL.append((SQLBuffer) alias);
            else
                selectSQL.append(alias.toString());
            selectSQL.append(" AS c").append(String.valueOf(i));
            if (itr.hasNext())
                selectSQL.append(", ");
        }
        return selectSQL;
    }

    public boolean canOuterJoin(int syntax, ForeignKey fk) {
        if (!super.canOuterJoin(syntax, fk))
            return false;
        if (fk != null && syntax == SYNTAX_DATABASE) {
            if (fk.getConstants().length > 0)
                return false;
            if (fk.getPrimaryKeyConstants().length > 0)
                return false;
        }
        return true;
    }

    public SQLBuffer toNativeJoin(Join join) {
        if (join.getType() != Join.TYPE_OUTER)
            return toTraditionalJoin(join);

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
            buf.append("(+)");
        }

        // check constant joins
        if (fk.getConstantColumns().length > 0)
            throw new StoreException(_loc.get("oracle-constant",
                join.getTable1(), join.getTable2())).setFatal(true);

        if (fk.getConstantPrimaryKeyColumns().length > 0)
            throw new StoreException(_loc.get("oracle-constant",
                join.getTable1(), join.getTable2())).setFatal(true);
        return buf;
    }

    public SQLBuffer toSelect(SQLBuffer select, JDBCFetchConfiguration fetch,
        SQLBuffer tables, SQLBuffer where, SQLBuffer group,
        SQLBuffer having, SQLBuffer order,
        boolean distinct, boolean forUpdate, long start, long end,
        Select sel) {
        if (!_checkedUpdateBug) {
            ensureDriverVendor();
            if (forUpdate && _driverBehavior == BEHAVE_DATADIRECT31)
                log.warn(_loc.get("dd-lock-bug"));
            _checkedUpdateBug = true;
        }

        // if no range, use standard select
        if (start == 0 && end == Long.MAX_VALUE)
            return super.toSelect(select, fetch, tables, where, group, having,
                order, distinct, forUpdate, 0, Long.MAX_VALUE, sel);

        // if no skip, ordering, or distinct can use rownum directly
        SQLBuffer buf = new SQLBuffer(this);
        if (!requiresSubselectForRange(start, end, distinct, order)) {
            if (where != null && !where.isEmpty())
                buf.append(where).append(" AND ");
            buf.append("ROWNUM <= ").appendValue(end);
            return super.toSelect(select, fetch, tables, buf, group, having,
                order, distinct, forUpdate, 0, Long.MAX_VALUE, sel);
        }

        // if there is ordering, skip, or distinct we have to use subselects
        SQLBuffer newsel = super.toSelect(select, fetch, tables, where,
            group, having, order, distinct, forUpdate, 0, Long.MAX_VALUE,
            sel);

        // if no skip, can use single nested subselect
        if (start == 0) {
            buf.append(getSelectOperation(fetch) + " * FROM (");
            buf.append(newsel);
            buf.append(") WHERE ROWNUM <= ").appendValue(end);
            return buf;
        }

        // with a skip, we have to use a double-nested subselect to put
        // where conditions on the rownum
        buf.append(getSelectOperation(fetch)
            + " * FROM (SELECT r.*, ROWNUM RNUM FROM (");
        buf.append(newsel);
        buf.append(") r");
        if (end != Long.MAX_VALUE)
            buf.append(" WHERE ROWNUM <= ").appendValue(end);
        buf.append(") WHERE RNUM > ").appendValue(start);
        return buf;
    }

    /**
     * Return true if the select with the given parameters needs a
     * subselect to apply a range.
     */
    private boolean requiresSubselectForRange(long start, long end,
        boolean distinct, SQLBuffer order) {
        if (start == 0 && end == Long.MAX_VALUE)
            return false;
        return start != 0 || distinct || (order != null && !order.isEmpty());
    }

    /**
     * Check to see if we have set the {@link #SELECT_HINT} in the
     * fetch configuraiton, and if so, append the Orache hint after the
     * "SELECT" part of the query.
     */
    public String getSelectOperation(JDBCFetchConfiguration fetch) {
        Object hint = fetch == null ? null : fetch.getHint(SELECT_HINT);
        String select = "SELECT";
        if (hint != null)
            select += " " + hint;
        return select;
    }

    public void setString(PreparedStatement stmnt, int idx, String val,
        Column col)
        throws SQLException {
        // oracle NCHAR/NVARCHAR/NCLOB unicode columns require some
        // special handling to configure them correctly; see:
        // http://www.oracle.com/technology/sample_code/tech/java/
        // sqlj_jdbc/files/9i_jdbc/NCHARsupport4UnicodeSample/Readme.html
        String typeName = (col == null) ? null : col.getTypeName();
        if (useSetFormOfUseForUnicode && typeName != null &&
            (typeName.toLowerCase().startsWith("nvarchar") ||
                typeName.toLowerCase().startsWith("nchar") ||
                typeName.toLowerCase().startsWith("nclob"))) {
            Statement inner = stmnt;
            if (inner instanceof DelegatingPreparedStatement)
                inner = ((DelegatingPreparedStatement) inner).
                    getInnermostDelegate();
            if (isOraclePreparedStatement(inner)) {
                try {
                    inner.getClass().getMethod("setFormOfUse",
                        new Class[]{ int.class, short.class }).
                        invoke(inner,
                            new Object[]{
                                new Integer(idx),
                                Class.forName
                                    ("oracle.jdbc.OraclePreparedStatement").
                                    getField("FORM_NCHAR").get(null)
                            });
                } catch (Exception e) {
                    log.warn(e);
                }
            } else if (!_warnedNcharColumn && log.isWarnEnabled()) {
                _warnedNcharColumn = true;
                log.warn(_loc.get("unconfigured-nchar-cols"));
            }
        }

        // call setFixedCHAR for fixed width character columns to get padding
        // semantics
        if (col != null && col.getType() == Types.CHAR
            && val != null && val.length() != col.getSize()) {
            Statement inner = stmnt;
            if (inner instanceof DelegatingPreparedStatement)
                inner = ((DelegatingPreparedStatement) inner).
                    getInnermostDelegate();
            if (isOraclePreparedStatement(inner)) {
                try {
                    inner.getClass().getMethod("setFixedCHAR",
                        new Class[]{ int.class, String.class }).
                        invoke(inner, new Object[]{ new Integer(idx), val });
                    return;
                } catch (Exception e) {
                    log.warn(e);
                }
            }

            if (!_warnedCharColumn && log.isWarnEnabled()) {
                _warnedCharColumn = true;
                log.warn(_loc.get("unpadded-char-cols"));
            }
        }
        super.setString(stmnt, idx, val, col);
    }

    public void setNull(PreparedStatement stmnt, int idx, int colType,
        Column col)
        throws SQLException {
        if ((colType == Types.CLOB || colType == Types.BLOB) && col.isNotNull())
            throw new UserException(_loc.get("null-blob-in-not-nullable", col
                .getFullName()));
        if (colType == Types.BLOB && _driverBehavior == BEHAVE_ORACLE)
            stmnt.setBlob(idx, getEmptyBlob());
        else if (colType == Types.CLOB && _driverBehavior == BEHAVE_ORACLE
            && !col.isXML())
            stmnt.setClob(idx, getEmptyClob());
        else if ((colType == Types.STRUCT || colType == Types.OTHER)
            && col != null && col.getTypeName() != null)
            stmnt.setNull(idx, Types.STRUCT, col.getTypeName());
            // some versions of the Oracle JDBC driver will fail if calling
            // setNull with DATE; see bug #1171
        else if (colType == Types.DATE)
            super.setNull(stmnt, idx, Types.TIMESTAMP, col);
        // the Oracle driver does not support Types.OTHER with setNull
        else if (colType == Types.OTHER || col.isXML())
            super.setNull(stmnt, idx, Types.NULL, col);
        else
            super.setNull(stmnt, idx, colType, col);
    }

    public String getClobString(ResultSet rs, int column)
        throws SQLException {
        if (_driverBehavior != BEHAVE_ORACLE)
            return super.getClobString(rs, column);

        Clob clob = getClob(rs, column);
        if (clob == null)
            return null;
        if (clob.getClass().getName().equals("oracle.sql.CLOB")) {
            try {
                if (((Boolean) Class.forName("oracle.sql.CLOB").
                    getMethod("isEmptyLob", new Class[0]).
                    invoke(clob, new Object[0])).
                    booleanValue())
                    return null;
            } catch (Exception e) {
                // possibly different version of the driver
            }
        }
        if (clob.length() == 0)
            return null;

        // unlikely that we'll have strings over 4 billion chars
        return clob.getSubString(1, (int) clob.length());
    }

    public Timestamp getTimestamp(ResultSet rs, int column, Calendar cal)
        throws SQLException {
        if (cal == null) {
            try {
                return super.getTimestamp(rs, column, cal);
            } catch (ArrayIndexOutOfBoundsException ae) {
                // CR295604: issue a warning this this bug can be gotten
                // around with SupportsTimestampNanos=false
                log.warn(_loc.get("oracle-timestamp-bug"), ae);
                throw ae;
            }
        }

        // handle Oracle bug where nanos not returned from call with Calendar
        // parameter
        Timestamp ts = rs.getTimestamp(column, cal);
        if (ts != null && ts.getNanos() == 0)
            ts.setNanos(rs.getTimestamp(column).getNanos());
        return ts;
    }

    public Object getObject(ResultSet rs, int column, Map map)
        throws SQLException {
        // recent oracle drivers return oracle-specific types for timestamps
        // and dates
        Object obj = super.getObject(rs, column, map);
        if (obj == null)
            return null;
        if ("oracle.sql.DATE".equals(obj.getClass().getName()))
            obj = convertFromOracleType(obj, "dateValue");
        else if ("oracle.sql.TIMESTAMP".equals(obj.getClass().getName()))
            obj = convertFromOracleType(obj, "timestampValue");
        return obj;
    }

    /**
     * Convert an object from its proprietary Oracle type to the standard
     * Java type.
     */
    private static Object convertFromOracleType(Object obj,
        String convertMethod)
        throws SQLException {
        try {
            Method m = obj.getClass().getMethod(convertMethod, (Class[]) null);
            return m.invoke(obj, (Object[]) null);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException)
                t = ((InvocationTargetException) t).getTargetException();
            if (t instanceof SQLException)
                throw(SQLException) t;
            throw new SQLException(t.getMessage());
        }
    }

    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);

        for (int i = 0; cols != null && i < cols.length; i++) {
            if (cols[i].getTypeName() == null)
                continue;
            if (cols[i].getTypeName().toUpperCase().startsWith("TIMESTAMP"))
                cols[i].setType(Types.TIMESTAMP);
            else if ("BLOB".equalsIgnoreCase(cols[i].getTypeName()))
                cols[i].setType(Types.BLOB);
            else if ("CLOB".equalsIgnoreCase(cols[i].getTypeName())
                || "NCLOB".equalsIgnoreCase(cols[i].getTypeName()))
                cols[i].setType(Types.CLOB);
            else if ("FLOAT".equalsIgnoreCase(cols[i].getTypeName()))
                cols[i].setType(Types.FLOAT);
            else if ("NVARCHAR".equalsIgnoreCase(cols[i].getTypeName()))
                cols[i].setType(Types.VARCHAR);
            else if ("NCHAR".equalsIgnoreCase(cols[i].getTypeName()))
                cols[i].setType(Types.CHAR);
        }
        return cols;
    }

    public PrimaryKey[] getPrimaryKeys(DatabaseMetaData meta,
        String catalog, String schemaName, String tableName, Connection conn)
        throws SQLException {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT t0.OWNER AS TABLE_SCHEM, ").
            append("t0.TABLE_NAME AS TABLE_NAME, ").
            append("t0.COLUMN_NAME AS COLUMN_NAME, ").
            append("t0.CONSTRAINT_NAME AS PK_NAME ").
            append("FROM ALL_CONS_COLUMNS t0, ALL_CONSTRAINTS t1 ").
            append("WHERE t0.OWNER = t1.OWNER ").
            append("AND t0.CONSTRAINT_NAME = t1.CONSTRAINT_NAME ").
            append("AND t1.CONSTRAINT_TYPE = 'P'");
        if (schemaName != null)
            buf.append(" AND t0.OWNER = ?");
        if (tableName != null)
            buf.append(" AND t0.TABLE_NAME = ?");

        PreparedStatement stmnt = conn.prepareStatement(buf.toString());
        ResultSet rs = null;
        try {
            int idx = 1;
            if (schemaName != null)
                setString(stmnt, idx++, schemaName.toUpperCase(), null);
            if (tableName != null)
                setString(stmnt, idx++, tableName.toUpperCase(), null);

            rs = stmnt.executeQuery();
            List pkList = new ArrayList();
            while (rs != null && rs.next())
                pkList.add(newPrimaryKey(rs));
            return (PrimaryKey[]) pkList.toArray
                (new PrimaryKey[pkList.size()]);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception e) {
                }
            try {
                stmnt.close();
            } catch (Exception e) {
            }
        }
    }

    public Index[] getIndexInfo(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, boolean unique, boolean approx,
        Connection conn)
        throws SQLException {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT t0.INDEX_OWNER AS TABLE_SCHEM, ").
            append("t0.TABLE_NAME AS TABLE_NAME, ").
            append("DECODE(t1.UNIQUENESS, 'UNIQUE', 0, 'NONUNIQUE', 1) ").
            append("AS NON_UNIQUE, ").
            append("t0.INDEX_NAME AS INDEX_NAME, ").
            append("t0.COLUMN_NAME AS COLUMN_NAME ").
            append("FROM ALL_IND_COLUMNS t0, ALL_INDEXES t1 ").
            append("WHERE t0.INDEX_OWNER = t1.OWNER ").
            append("AND t0.INDEX_NAME = t1.INDEX_NAME");
        if (schemaName != null)
            buf.append(" AND t0.TABLE_OWNER = ?");
        if (tableName != null)
            buf.append(" AND t0.TABLE_NAME = ?");

        PreparedStatement stmnt = conn.prepareStatement(buf.toString());
        ResultSet rs = null;
        try {
            int idx = 1;
            if (schemaName != null)
                setString(stmnt, idx++, schemaName.toUpperCase(), null);
            if (tableName != null)
                setString(stmnt, idx++, tableName.toUpperCase(), null);

            rs = stmnt.executeQuery();
            List idxList = new ArrayList();
            while (rs != null && rs.next())
                idxList.add(newIndex(rs));
            return (Index[]) idxList.toArray(new Index[idxList.size()]);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception e) {
                }
            try {
                stmnt.close();
            } catch (Exception e) {
            }
        }
    }

    public ForeignKey[] getImportedKeys(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, Connection conn)
        throws SQLException {
        StringBuffer delAction = new StringBuffer("DECODE(t1.DELETE_RULE").
            append(", 'NO ACTION', ").append(meta.importedKeyNoAction).
            append(", 'RESTRICT', ").append(meta.importedKeyRestrict).
            append(", 'CASCADE', ").append(meta.importedKeyCascade).
            append(", 'SET NULL', ").append(meta.importedKeySetNull).
            append(", 'SET DEFAULT', ").append(meta.importedKeySetDefault).
            append(")");

        StringBuffer buf = new StringBuffer();
        buf.append("SELECT t2.OWNER AS PKTABLE_SCHEM, ").
            append("t2.TABLE_NAME AS PKTABLE_NAME, ").
            append("t2.COLUMN_NAME AS PKCOLUMN_NAME, ").
            append("t0.OWNER AS FKTABLE_SCHEM, ").
            append("t0.TABLE_NAME AS FKTABLE_NAME, ").
            append("t0.COLUMN_NAME AS FKCOLUMN_NAME, ").
            append("t0.POSITION AS KEY_SEQ, ").
            append(delAction).append(" AS DELETE_RULE, ").
            append("t0.CONSTRAINT_NAME AS FK_NAME, ").
            append("DECODE(t1.DEFERRED, 'DEFERRED', ").
            append(meta.importedKeyInitiallyDeferred).
            append(", 'IMMEDIATE', ").
            append(meta.importedKeyInitiallyImmediate).
            append(") AS DEFERRABILITY ").
            append("FROM ALL_CONS_COLUMNS t0, ALL_CONSTRAINTS t1, ").
            append("ALL_CONS_COLUMNS t2 ").
            append("WHERE t0.OWNER = t1.OWNER ").
            append("AND t0.CONSTRAINT_NAME = t1.CONSTRAINT_NAME ").
            append("AND t1.CONSTRAINT_TYPE = 'R' ").
            append("AND t1.R_OWNER = t2.OWNER ").
            append("AND t1.R_CONSTRAINT_NAME = t2.CONSTRAINT_NAME ").
            append("AND t0.POSITION = t2.POSITION");
        if (schemaName != null)
            buf.append(" AND t0.OWNER = ?");
        if (tableName != null)
            buf.append(" AND t0.TABLE_NAME = ?");
        buf.append(" ORDER BY t2.OWNER, t2.TABLE_NAME, t0.POSITION");

        PreparedStatement stmnt = conn.prepareStatement(buf.toString());
        ResultSet rs = null;
        try {
            int idx = 1;
            if (schemaName != null)
                setString(stmnt, idx++, schemaName.toUpperCase(), null);
            if (tableName != null)
                setString(stmnt, idx++, tableName.toUpperCase(), null);

            rs = stmnt.executeQuery();
            List fkList = new ArrayList();
            while (rs != null && rs.next())
                fkList.add(newForeignKey(rs));
            return (ForeignKey[]) fkList.toArray
                (new ForeignKey[fkList.size()]);
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception e) {
                }
            try {
                stmnt.close();
            } catch (Exception e) {
            }
        }
    }

    public String[] getCreateTableSQL(Table table) {
        // only override if we are simulating auto-incremenet with triggers
        String[] create = super.getCreateTableSQL(table);
        if (!useTriggersForAutoAssign)
            return create;

        Column[] cols = table.getColumns();
        List seqs = null;
        String seq, trig;
        for (int i = 0; cols != null && i < cols.length; i++) {
            if (!cols[i].isAutoAssigned())
                continue;
            if (seqs == null)
                seqs = new ArrayList(4);

            seq = autoAssignSequenceName;
            if (seq == null) {
                if (openjpa3GeneratedKeyNames)
                    seq = getOpenJPA3GeneratedKeySequenceName(cols[i]);
                else
                    seq = getGeneratedKeySequenceName(cols[i]);
                seqs.add("CREATE SEQUENCE " + seq + " START WITH 1");
            }
            if (openjpa3GeneratedKeyNames)
                trig = getOpenJPA3GeneratedKeyTriggerName(cols[i]);
            else
                trig = getGeneratedKeyTriggerName(cols[i]);

            // create the trigger that will insert new values into
            // the table whenever a row is created
            seqs.add("CREATE OR REPLACE TRIGGER " + trig
                + " BEFORE INSERT ON " + table.getName()
                + " FOR EACH ROW BEGIN SELECT " + seq + ".nextval INTO "
                + ":new." + cols[i].getName() + " FROM DUAL; "
                + "END " + trig + ";");
        }
        if (seqs == null)
            return create;

        // combine create table sql and create seqences sql
        String[] sql = new String[create.length + seqs.size()];
        System.arraycopy(create, 0, sql, 0, create.length);
        for (int i = 0; i < seqs.size(); i++)
            sql[create.length + i] = (String) seqs.get(i);
        return sql;
    }

    public String[] getCreateSequenceSQL(Sequence seq) {
        String[] sql = super.getCreateSequenceSQL(seq);
        if (seq.getAllocate() > 1)
            sql[0] += " CACHE " + seq.getAllocate();
        return sql;
    }
    
    protected String getSequencesSQL(String schemaName, String sequenceName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT SEQUENCE_OWNER AS SEQUENCE_SCHEMA, ").
            append("SEQUENCE_NAME FROM ALL_SEQUENCES");
        if (schemaName != null || sequenceName != null)
            buf.append(" WHERE ");
        if (schemaName != null) {
            buf.append("SEQUENCE_OWNER = ?");
            if (sequenceName != null)
                buf.append(" AND ");
        }
        if (sequenceName != null)
            buf.append("SEQUENCE_NAME = ?");
        return buf.toString();
    }

    public boolean isSystemSequence(String name, String schema,
        boolean targetSchema) {
        if (super.isSystemSequence(name, schema, targetSchema))
            return true;

        // filter out generated sequences used for auto-assign
        return (autoAssignSequenceName != null
            && name.equalsIgnoreCase(autoAssignSequenceName))
            || (autoAssignSequenceName == null
            && name.toUpperCase().startsWith("ST_"));
    }

    public Object getGeneratedKey(Column col, Connection conn)
        throws SQLException {
        if (!useTriggersForAutoAssign)
            return Numbers.valueOf(0L);

        // if we simulate auto-assigned columns using triggers and
        // sequences, then return the current value of the sequence
        // from autoAssignSequenceName
        String seq = autoAssignSequenceName;
        if (seq == null && openjpa3GeneratedKeyNames)
            seq = getOpenJPA3GeneratedKeySequenceName(col);
        else if (seq == null)
            seq = getGeneratedKeySequenceName(col);
        PreparedStatement stmnt = conn.prepareStatement("SELECT " + seq
            + ".currval FROM DUAL");
        ResultSet rs = null;
        try {
            rs = stmnt.executeQuery();
            rs.next();
            return Numbers.valueOf(rs.getLong(1));
        } finally {
            if (rs != null)
                try { rs.close(); } catch (SQLException se) {}
            try { stmnt.close(); } catch (SQLException se) {}
        }
    }

    /**
     * Trigger name for simulating auto-assign values on the given column.
     */
    protected String getGeneratedKeyTriggerName(Column col) {
        // replace trailing _SEQ with _TRG
        String seqName = getGeneratedKeySequenceName(col);
        return seqName.substring(0, seqName.length() - 3) + "TRG";
    }

    /**
     * Returns a OpenJPA 3-compatible name for an auto-assign sequence.
     */
    protected String getOpenJPA3GeneratedKeySequenceName(Column col) {
        Table table = col.getTable();
        return makeNameValid("SEQ_" + table.getName(), table.getSchema().
            getSchemaGroup(), maxTableNameLength, NAME_ANY);
    }

    /**
     * Returns a OpenJPA 3-compatible name for an auto-assign trigger.
     */
    protected String getOpenJPA3GeneratedKeyTriggerName(Column col) {
        Table table = col.getTable();
        return makeNameValid("TRIG_" + table.getName(), table.getSchema().
            getSchemaGroup(), maxTableNameLength, NAME_ANY);
    }

    /**
     * Invoke Oracle's <code>putBytes</code> method on the given BLOB object.
     * Uses reflection in case the blob is wrapped in another
     * vendor-specific class; for example Weblogic wraps oracle thin driver
     * lobs in its own interfaces with the same methods.
     */
    public void putBytes(Object blob, byte[] data)
        throws SQLException {
        if (blob == null)
            return;
        if (_putBytes == null) {
            try {
                _putBytes = blob.getClass().getMethod("putBytes",
                    new Class[]{ long.class, byte[].class });
            } catch (Exception e) {
                throw new StoreException(e);
            }
        }
        invokePutLobMethod(_putBytes, blob, data);
    }

    /**
     * Invoke Oracle's <code>putString</code> method on the given CLOB object.
     * Uses reflection in case the clob is wrapped in another
     * vendor-specific class; for example Weblogic wraps oracle thin driver
     * lobs in its own interfaces with the same methods.
     */
    public void putString(Object clob, String data)
        throws SQLException {
        if (_putString == null) {
            try {
                _putString = clob.getClass().getMethod("putString",
                    new Class[]{ long.class, String.class });
            } catch (Exception e) {
                throw new StoreException(e);
            }
        }
        invokePutLobMethod(_putString, clob, data);
    }

    /**
     * Invoke Oracle's <code>putChars</code> method on the given CLOB
     * object. Uses reflection in case the clob is wrapped in another
     * vendor-specific class; for example Weblogic wraps oracle thin driver
     * lobs in its own interfaces with the same methods.
     */
    public void putChars(Object clob, char[] data)
        throws SQLException {
        if (_putChars == null) {
            try {
                _putChars = clob.getClass().getMethod("putChars",
                    new Class[]{ long.class, char[].class });
            } catch (Exception e) {
                throw new StoreException(e);
            }
        }
        invokePutLobMethod(_putChars, clob, data);
    }

    /**
     * Invoke the given LOB method on the given target with the given data.
     */
    private static void invokePutLobMethod(Method method, Object target,
        Object data)
        throws SQLException {
        try {
            method.invoke(target, new Object[]{ Numbers.valueOf(1L), data });
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof SQLException)
                throw(SQLException) t;
            throw new StoreException(t);
        } catch (Exception e) {
            throw new StoreException(e);
        }
    }

    private Clob getEmptyClob()
        throws SQLException {
        if (EMPTY_CLOB != null)
            return EMPTY_CLOB;
        try {
            return EMPTY_CLOB = (Clob) Class.forName("oracle.sql.CLOB",true, 
                    AccessController.doPrivileged(J2DoPrivHelper
                            .getContextClassLoaderAction())).
                getMethod("empty_lob", new Class[0]).
                invoke(null, new Object[0]);
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    private Blob getEmptyBlob()
        throws SQLException {
        if (EMPTY_BLOB != null)
            return EMPTY_BLOB;
        try {
            return EMPTY_BLOB = (Blob) Class.forName("oracle.sql.BLOB",true, 
                    AccessController.doPrivileged(J2DoPrivHelper
                            .getContextClassLoaderAction())).
                getMethod("empty_lob", new Class[0]).
                invoke(null, new Object[0]);
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    private static boolean isOraclePreparedStatement(Statement stmnt) {
        try {
            return Class.forName("oracle.jdbc.OraclePreparedStatement").
                isInstance(stmnt);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * If this dictionary supports XML type,
     * use this method to append xml predicate.
     * 
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison
     * @param rhs the right hand side of the comparison
     */
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
        appendXmlExtractValue(buf, lhs);
        buf.append(" ").append(op).append(" ");
        rhs.appendTo(buf);
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
        appendXmlExtractValue(buf, lhs);
        buf.append(" ").append(op).append(" ");
        appendXmlExtractValue(buf, rhs);
    }
    
    private void appendXmlExtractValue(SQLBuffer buf, FilterValue val) {
        buf.append("extractValue(").
            append(val.getColumnAlias(
            val.getFieldMapping().getColumns()[0])).
            append(",'/*/");
        val.appendTo(buf);
        buf.append("')");
    }
    
    public void insertBlobForStreamingLoad(Row row, Column col, Object ob)
        throws SQLException {
        if (ob == null)
            col.setType(Types.OTHER);
        row.setNull(col);
    }
    
    public void insertClobForStreamingLoad(Row row, Column col, Object ob)
        throws SQLException {
        if (ob == null)
            col.setType(Types.OTHER);
        row.setNull(col);
    }

    public int getBatchUpdateCount(PreparedStatement ps) throws SQLException {
        int updateSuccessCnt = 0;
        if (batchLimit != 0 && ps != null) {
            updateSuccessCnt = ps.getUpdateCount();
            if (log.isTraceEnabled())
                log.trace(_loc.get("batch-update-success-count",
                    updateSuccessCnt));
        }
        return updateSuccessCnt;
    }
}

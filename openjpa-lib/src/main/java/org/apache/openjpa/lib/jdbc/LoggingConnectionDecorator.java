/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.BatchUpdateException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.lib.log.Log;

/**
 * A {@link ConnectionDecorator} that creates logging connections and
 * {@link ReportingSQLException}s.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class LoggingConnectionDecorator implements ConnectionDecorator {

    private static final String SEP = System.getProperty("line.separator");

    private static final int WARN_IGNORE = 0;
    private static final int WARN_LOG_TRACE = 1;
    private static final int WARN_LOG_INFO = 2;
    private static final int WARN_LOG_WARN = 3;
    private static final int WARN_LOG_ERROR = 4;
    private static final int WARN_THROW = 5;
    private static final int WARN_HANDLE = 6;
    private static final String[] WARNING_ACTIONS = new String[7];

    static {
        WARNING_ACTIONS[WARN_IGNORE] = "ignore";
        WARNING_ACTIONS[WARN_LOG_TRACE] = "trace";
        WARNING_ACTIONS[WARN_LOG_INFO] = "info";
        WARNING_ACTIONS[WARN_LOG_WARN] = "warn";
        WARNING_ACTIONS[WARN_LOG_ERROR] = "error";
        WARNING_ACTIONS[WARN_THROW] = "throw";
        WARNING_ACTIONS[WARN_HANDLE] = "handle";
    }

    private final DataSourceLogs _logs = new DataSourceLogs();
    private SQLFormatter _formatter;
    private boolean _prettyPrint;
    private int _prettyPrintLineLength = 60;
    private int _warningAction = WARN_IGNORE;
    private SQLWarningHandler _warningHandler;
    private boolean _trackParameters = true;

    /**
     * If set to <code>true</code>, pretty-print SQL by running it
     * through {@link SQLFormatter#prettyPrint}. If
     * <code>false</code>, don't pretty-print, and output SQL logs in
     * a single line. Pretty-printed SQL can be easier for a human to
     * read, but is harder to parse with tools like grep.
     */
    public void setPrettyPrint(boolean prettyPrint) {
        _prettyPrint = prettyPrint;
        if (_formatter == null && _prettyPrint) {
            _formatter = new SQLFormatter();
            _formatter.setLineLength(_prettyPrintLineLength);
        } else if (!_prettyPrint)
            _formatter = null;
    }

    /**
     * @see #setPrettyPrint
     */
    public boolean getPrettyPrint() {
        return _prettyPrint;
    }

    /**
     * The number of characters to print per line when
     * pretty-printing of SQL is enabled. Defaults to 60 to provide
     * some space for any ant-related characters on the left of a
     * standard 80-character display.
     */
    public void setPrettyPrintLineLength(int length) {
        _prettyPrintLineLength = length;
        if (_formatter != null)
            _formatter.setLineLength(length);
    }

    /**
     * @see #setPrettyPrintLineLength
     */
    public int getPrettyPrintLineLength() {
        return _prettyPrintLineLength;
    }

    /**
     * Whether to track parameters for the purposes of reporting exceptions.
     */
    public void setTrackParameters(boolean trackParameters) {
        _trackParameters = trackParameters;
    }

    /**
     * Whether to track parameters for the purposes of reporting exceptions.
     */
    public boolean getTrackParameters() {
        return _trackParameters;
    }

    /**
     * What to do with SQL warnings.
     */
    public void setWarningAction(String warningAction) {
        int index = Arrays.asList(WARNING_ACTIONS).indexOf(warningAction);
        if (index < 0)
            index = WARN_IGNORE;
        _warningAction = index;
    }

    /**
     * What to do with SQL warnings.
     */
    public String getWarningAction() {
        return WARNING_ACTIONS[_warningAction];
    }

    /**
     * What to do with SQL warnings.
     */
    public void setWarningHandler(SQLWarningHandler warningHandler) {
        _warningHandler = warningHandler;
    }

    /**
     * What to do with SQL warnings.
     */
    public SQLWarningHandler getWarningHandler() {
        return _warningHandler;
    }

    /**
     * The log to write to.
     */
    public DataSourceLogs getLogs() {
        return _logs;
    }

    public Connection decorate(Connection conn) throws SQLException {
        return new LoggingConnection(conn);
    }

    private SQLException wrap(SQLException sqle, Statement stmnt) {
        if (sqle instanceof ReportingSQLException)
            return (ReportingSQLException) sqle;

        return new ReportingSQLException(sqle, stmnt);
    }

    private SQLException wrap(SQLException sqle, String sql) {
        if (sqle instanceof ReportingSQLException)
            return (ReportingSQLException) sqle;

        return new ReportingSQLException(sqle, sql);
    }

    /**
     * Interface that allows customization of what to do when
     * {@link SQLWarning}s occur.
     */
    public static interface SQLWarningHandler {

        public void handleWarning(SQLWarning warning) throws SQLException;
    }

    private class LoggingConnection extends DelegatingConnection {

        public LoggingConnection(Connection conn) throws SQLException {
            super(conn);
        }

        protected PreparedStatement prepareStatement(String sql, boolean wrap)
            throws SQLException {
            try {
                PreparedStatement stmnt = super.prepareStatement(sql, false);
                return new LoggingPreparedStatement(stmnt, sql);
            } catch (SQLException se) {
                throw wrap(se, sql);
            }
        }

        protected PreparedStatement prepareStatement(String sql, int rsType,
            int rsConcur, boolean wrap) throws SQLException {
            try {
                PreparedStatement stmnt = super.prepareStatement
                    (sql, rsType, rsConcur, false);
                return new LoggingPreparedStatement(stmnt, sql);
            } catch (SQLException se) {
                throw wrap(se, sql);
            }
        }

        protected Statement createStatement(boolean wrap) throws SQLException {
            Statement stmnt = super.createStatement(false);
            return new LoggingStatement(stmnt);
        }

        protected Statement createStatement(int type, int concurrency,
            boolean wrap) throws SQLException {
            Statement stmnt = super.createStatement(type, concurrency, false);
            return new LoggingStatement(stmnt);
        }

        public void commit() throws SQLException {
            long start = System.currentTimeMillis();

            try {
                super.commit();
            }
            finally {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("commit", start, this);
                handleSQLWarning();
            }
        }

        public void rollback() throws SQLException {
            long start = System.currentTimeMillis();

            try {
                super.rollback();
            }
            finally {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("rollback", start, this);
                handleSQLWarning();
            }
        }

        public void close() throws SQLException {
            long start = System.currentTimeMillis();

            try {
                super.close();
            }
            finally {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("close", start, this);
            }
        }

        public Savepoint setSavepoint() throws SQLException {
            long start = System.currentTimeMillis();
            try {
                return super.setSavepoint();
            }
            finally {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("savepoint", start, this);
                handleSQLWarning();
            }
        }

        public Savepoint setSavepoint(String name) throws SQLException {
            long start = System.currentTimeMillis();
            try {
                return super.setSavepoint(name);
            }
            finally {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("savepoint: " + name, start, this);
                handleSQLWarning();
            }
        }

        public void rollback(Savepoint savepoint) throws SQLException {
            long start = System.currentTimeMillis();
            try {
                super.rollback(savepoint);
            }
            finally {
                if (_logs.isJDBCEnabled()) {
                    String name = null;
                    try {
                        name = savepoint.getSavepointName();
                    } catch (SQLException sqe) {
                        name = String.valueOf(savepoint.getSavepointId());
                    }
                    _logs.logJDBC("rollback: " + name, start, this);
                }
                handleSQLWarning();
            }
        }

        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            long start = System.currentTimeMillis();
            try {
                super.releaseSavepoint(savepoint);
            }
            finally {
                if (_logs.isJDBCEnabled()) {
                    String name = null;
                    try {
                        name = savepoint.getSavepointName();
                    } catch (SQLException sqe) {
                        name = String.valueOf(savepoint.getSavepointId());
                    }
                    _logs.logJDBC("release: " + name, start, this);
                }
                handleSQLWarning();
            }
        }

        protected Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability, boolean wrap)
            throws SQLException {
            Statement stmnt = super.createStatement(resultSetType,
                resultSetConcurrency, resultSetHoldability, false);
            handleSQLWarning();
            return new LoggingStatement(stmnt);
        }

        protected PreparedStatement prepareStatement(String sql,
            int resultSetType, int resultSetConcurrency,
            int resultSetHoldability, boolean wrap) throws SQLException {
            try {
                PreparedStatement stmnt = super.prepareStatement
                    (sql, resultSetType, resultSetConcurrency,
                        resultSetHoldability, false);
                handleSQLWarning();
                return new LoggingPreparedStatement(stmnt, sql);
            } catch (SQLException se) {
                throw wrap(se, sql);
            }
        }

        protected PreparedStatement prepareStatement(String sql,
            int autoGeneratedKeys, boolean wrap) throws SQLException {
            try {
                PreparedStatement stmnt = super.prepareStatement
                    (sql, autoGeneratedKeys, false);
                handleSQLWarning();
                return new LoggingPreparedStatement(stmnt, sql);
            } catch (SQLException se) {
                throw wrap(se, sql);
            }
        }

        protected PreparedStatement prepareStatement(String sql,
            int[] columnIndexes, boolean wrap) throws SQLException {
            try {
                PreparedStatement stmnt = super.prepareStatement
                    (sql, columnIndexes, false);
                handleSQLWarning();
                return new LoggingPreparedStatement(stmnt, sql);
            } catch (SQLException se) {
                throw wrap(se, sql);
            }
        }

        protected PreparedStatement prepareStatement(String sql,
            String[] columnNames, boolean wrap) throws SQLException {
            try {
                PreparedStatement stmnt = super.prepareStatement
                    (sql, columnNames, false);
                handleSQLWarning();
                return new LoggingPreparedStatement(stmnt, sql);
            } catch (SQLException se) {
                throw wrap(se, sql);
            }
        }

        protected DatabaseMetaData getMetaData(boolean wrap)
            throws SQLException {
            return new LoggingDatabaseMetaData(super.getMetaData(false));
        }

        /**
         * Handle any {@link SQLWarning}s on the current {@link Connection}.
         *
         * @see #handleSQLWarning(SQLWarning)
         */
        private void handleSQLWarning() throws SQLException {
            if (_warningAction == WARN_IGNORE)
                return;

            try {
                handleSQLWarning(getWarnings());
            }
            finally {
                clearWarnings();
            }
        }

        /**
         * Handle any {@link SQLWarning}s on the specified {@link Statement}.
         *
         * @see #handleSQLWarning(SQLWarning)
         */
        private void handleSQLWarning(Statement stmnt) throws SQLException {
            if (_warningAction == WARN_IGNORE)
                return;

            try {
                handleSQLWarning(stmnt.getWarnings());
            }
            finally {
                stmnt.clearWarnings();
            }
        }

        /**
         * Handle any {@link SQLWarning}s on the specified {@link ResultSet}.
         *
         * @see #handleSQLWarning(SQLWarning)
         */
        private void handleSQLWarning(ResultSet rs) throws SQLException {
            if (_warningAction == WARN_IGNORE)
                return;

            try {
                handleSQLWarning(rs.getWarnings());
            }
            finally {
                rs.clearWarnings();
            }
        }

        /**
         * Handle the specified {@link SQLWarning} depending on the
         * setting of the {@link #setWarningAction} attribute.
         *
         * @param warning the warning to handle
         */
        void handleSQLWarning(SQLWarning warning) throws SQLException {
            if (warning == null)
                return;
            if (_warningAction == WARN_IGNORE)
                return;

            Log log = _logs.getJDBCLog();
            for (; warning != null; warning = warning.getNextWarning()) {
                switch (_warningAction) {
                    case WARN_LOG_TRACE:
                        if (log.isTraceEnabled())
                            log.trace(warning);
                        break;
                    case WARN_LOG_INFO:
                        if (log.isInfoEnabled())
                            log.info(warning);
                        break;
                    case WARN_LOG_WARN:
                        if (log.isWarnEnabled())
                            log.warn(warning);
                        break;
                    case WARN_LOG_ERROR:
                        if (log.isErrorEnabled())
                            log.error(warning);
                        break;
                    case WARN_THROW:
                        // just throw it as if it were a SQLException
                        throw warning;
                    case WARN_HANDLE:
                        if (_warningHandler != null)
                            _warningHandler.handleWarning(warning);
                        break;
                    default:
                        // ignore
                        break;
                }
            }
        }

        private class LoggingDatabaseMetaData
            extends DelegatingDatabaseMetaData {

            public LoggingDatabaseMetaData(DatabaseMetaData meta) {
                super(meta, LoggingConnection.this);
            }

            public ResultSet getBestRowIdentifier(String catalog,
                String schema, String table, int scope, boolean nullable)
                throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getBestRowIdentifier: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getBestRowIdentifier(catalog, schema,
                    table, scope, nullable);
            }

            public ResultSet getCatalogs() throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getCatalogs", LoggingConnection.this);
                return super.getCatalogs();
            }

            public ResultSet getColumnPrivileges(String catalog, String schema,
                String table, String columnNamePattern) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getColumnPrivileges: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getColumnPrivileges(catalog, schema,
                    table, columnNamePattern);
            }

            public ResultSet getColumns(String catalog, String schemaPattern,
                String tableNamePattern, String columnNamePattern)
                throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getColumns: "
                        + catalog + ", " + schemaPattern + ", "
                        + tableNamePattern + ", " + columnNamePattern,
                        LoggingConnection.this);
                return super.getColumns(catalog, schemaPattern,
                    tableNamePattern, columnNamePattern);
            }

            public ResultSet getCrossReference(String primaryCatalog,
                String primarySchema, String primaryTable,
                String foreignCatalog, String foreignSchema,
                String foreignTable) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getCrossReference: "
                        + primaryCatalog + ", " + primarySchema + ", "
                        + primaryTable + ", " + foreignCatalog + ", "
                        + foreignSchema + ", " + foreignSchema,
                        LoggingConnection.this);
                return super.getCrossReference(primaryCatalog, primarySchema,
                    primaryTable, foreignCatalog, foreignSchema, foreignTable);
            }

            public ResultSet getExportedKeys(String catalog, String schema,
                String table) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getExportedKeys: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getExportedKeys(catalog, schema, table);
            }

            public ResultSet getImportedKeys(String catalog, String schema,
                String table) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getImportedKeys: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getImportedKeys(catalog, schema, table);
            }

            public ResultSet getIndexInfo(String catalog, String schema,
                String table, boolean unique, boolean approximate)
                throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getIndexInfo: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getIndexInfo(catalog, schema, table, unique,
                    approximate);
            }

            public ResultSet getPrimaryKeys(String catalog, String schema,
                String table) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getPrimaryKeys: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getPrimaryKeys(catalog, schema, table);
            }

            public ResultSet getProcedureColumns(String catalog,
                String schemaPattern, String procedureNamePattern,
                String columnNamePattern) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getProcedureColumns: "
                        + catalog + ", " + schemaPattern + ", "
                        + procedureNamePattern + ", " + columnNamePattern,
                        LoggingConnection.this);
                return super.getProcedureColumns(catalog, schemaPattern,
                    procedureNamePattern, columnNamePattern);
            }

            public ResultSet getProcedures(String catalog,
                String schemaPattern, String procedureNamePattern)
                throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getProcedures: "
                        + catalog + ", " + schemaPattern + ", "
                        + procedureNamePattern, LoggingConnection.this);
                return super.getProcedures(catalog, schemaPattern,
                    procedureNamePattern);
            }

            public ResultSet getSchemas() throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getSchemas", LoggingConnection.this);
                return super.getSchemas();
            }

            public ResultSet getTablePrivileges(String catalog,
                String schemaPattern, String tableNamePattern)
                throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getTablePrivileges", LoggingConnection.this);
                return super.getTablePrivileges(catalog, schemaPattern,
                    tableNamePattern);
            }

            public ResultSet getTables(String catalog, String schemaPattern,
                String tableNamePattern, String[] types) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getTables: "
                        + catalog + ", " + schemaPattern + ", "
                        + tableNamePattern, LoggingConnection.this);
                return super.getTables(catalog, schemaPattern,
                    tableNamePattern, types);
            }

            public ResultSet getTableTypes() throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getTableTypes", LoggingConnection.this);
                return super.getTableTypes();
            }

            public ResultSet getTypeInfo() throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getTypeInfo", LoggingConnection.this);
                return super.getTypeInfo();
            }

            public ResultSet getUDTs(String catalog, String schemaPattern,
                String typeNamePattern, int[] types) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getUDTs", LoggingConnection.this);
                return super.getUDTs(catalog, schemaPattern,
                    typeNamePattern, types);
            }

            public ResultSet getVersionColumns(String catalog,
                String schema, String table) throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("getVersionColumns: "
                        + catalog + ", " + schema + ", " + table,
                        LoggingConnection.this);
                return super.getVersionColumns(catalog, schema, table);
            }
        }

        /**
         * Statement wrapper that logs SQL to the parent data source and
         * remembers the last piece of SQL to be executed on it.
         */
        private class LoggingStatement extends DelegatingStatement {

            private String _sql = null;

            public LoggingStatement(Statement stmnt) throws SQLException {
                super(stmnt, LoggingConnection.this);
            }

            public void appendInfo(StringBuffer buf) {
                if (_sql != null) {
                    buf.append(" ");
                    if (_formatter != null) {
                        buf.append(SEP);
                        buf.append(_formatter.prettyPrint(_sql));
                    } else {
                        buf.append(_sql);
                    }
                }
            }

            protected ResultSet wrapResult(ResultSet rs, boolean wrap) {
                if (!wrap || rs == null)
                    return super.wrapResult(rs, wrap);
                return new LoggingResultSet(rs, this);
            }

            public void cancel() throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("cancel " + this + ": " + _sql,
                        LoggingConnection.this);

                super.cancel();
            }

            protected ResultSet executeQuery(String sql, boolean wrap)
                throws SQLException {
                long start = System.currentTimeMillis();

                _sql = sql;
                try {
                    return super.executeQuery(sql, wrap);
                } catch (SQLException se) {
                    throw wrap(se, LoggingStatement.this);
                }
                finally {
                    if (_logs.isSQLEnabled())
                        _logs.logSQL("executing " + this, start,
                            LoggingConnection.this);
                    handleSQLWarning(LoggingStatement.this);
                }
            }

            public int executeUpdate(String sql) throws SQLException {
                long start = System.currentTimeMillis();

                _sql = sql;
                try {
                    return super.executeUpdate(sql);
                } catch (SQLException se) {
                    throw wrap(se, LoggingStatement.this);
                }
                finally {
                    if (_logs.isSQLEnabled())
                        _logs.logSQL("executing " + this, start,
                            LoggingConnection.this);
                    handleSQLWarning(LoggingStatement.this);
                }
            }

            public boolean execute(String sql) throws SQLException {
                long start = System.currentTimeMillis();

                _sql = sql;
                try {
                    return super.execute(sql);
                } catch (SQLException se) {
                    throw wrap(se, LoggingStatement.this);
                }
                finally {
                    if (_logs.isSQLEnabled())
                        _logs.logSQL("executing " + this, start,
                            LoggingConnection.this);
                    handleSQLWarning(LoggingStatement.this);
                }
            }
        }

        private class LoggingPreparedStatement
            extends DelegatingPreparedStatement {

            private final String _sql;
            private List _params = null;
            private List _paramBatch = null;

            public LoggingPreparedStatement(PreparedStatement stmnt, String sql)
                throws SQLException {
                super(stmnt, LoggingConnection.this);
                _sql = sql;
            }

            protected ResultSet wrapResult(ResultSet rs, boolean wrap) {
                if (!wrap || rs == null)
                    return super.wrapResult(rs, wrap);
                return new LoggingResultSet(rs, this);
            }

            protected ResultSet executeQuery(String sql, boolean wrap)
                throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.executeQuery(sql, wrap);
                } catch (SQLException se) {
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            public int executeUpdate(String sql) throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.executeUpdate(sql);
                } catch (SQLException se) {
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            public boolean execute(String sql) throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.execute(sql);
                } catch (SQLException se) {
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            protected ResultSet executeQuery(boolean wrap) throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.executeQuery(wrap);
                } catch (SQLException se) {
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            public int executeUpdate() throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.executeUpdate();
                } catch (SQLException se) {
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            public int[] executeBatch() throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.executeBatch();
                } catch (SQLException se) {
                    // if the exception is a BatchUpdateException, and
                    // we are tracking parameters, then set the current
                    // parameter set to be the index of the failed
                    // statement so that the ReportingSQLException will
                    // show the correct param
                    if (se instanceof BatchUpdateException
                        && _paramBatch != null && shouldTrackParameters()) {
                        int[] count = ((BatchUpdateException) se).
                            getUpdateCounts();
                        if (count != null && count.length <= _paramBatch.size())
                        {
                            int index = -1;
                            for (int i = 0; i < count.length; i++) {
                                // -3 is Statement.STATEMENT_FAILED, but is
                                // only available in JDK 1.4+
                                if (count[i] == -3) {
                                    index = i;
                                    break;
                                }
                            }

                            // no -3 element: it may be that the server stopped
                            // processing, so the size of the count will be
                            // the index
                            if (index == -1)
                                index = count.length + 1;

                            // set the current params to the saved values
                            if (index < _paramBatch.size())
                                _params = (List) _paramBatch.get(index);
                        }
                    }
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing batch", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            public boolean execute() throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    return super.execute();
                } catch (SQLException se) {
                    throw wrap(se, LoggingPreparedStatement.this);
                }
                finally {
                    log("executing", start);
                    clearLogParameters(true);
                    handleSQLWarning(LoggingPreparedStatement.this);
                }
            }

            public void cancel() throws SQLException {
                if (_logs.isJDBCEnabled())
                    _logs.logJDBC("cancel " + this + ": " + _sql,
                        LoggingConnection.this);

                super.cancel();
            }

            public void setNull(int i1, int i2) throws SQLException {
                setLogParameter(i1, "null", null);
                super.setNull(i1, i2);
            }

            public void setBoolean(int i, boolean b) throws SQLException {
                setLogParameter(i, b);
                super.setBoolean(i, b);
            }

            public void setByte(int i, byte b) throws SQLException {
                setLogParameter(i, b);
                super.setByte(i, b);
            }

            public void setShort(int i, short s) throws SQLException {
                setLogParameter(i, s);
                super.setShort(i, s);
            }

            public void setInt(int i1, int i2) throws SQLException {
                setLogParameter(i1, i2);
                super.setInt(i1, i2);
            }

            public void setLong(int i, long l) throws SQLException {
                setLogParameter(i, l);
                super.setLong(i, l);
            }

            public void setFloat(int i, float f) throws SQLException {
                setLogParameter(i, f);
                super.setFloat(i, f);
            }

            public void setDouble(int i, double d) throws SQLException {
                setLogParameter(i, d);
                super.setDouble(i, d);
            }

            public void setBigDecimal(int i, BigDecimal bd)
                throws SQLException {
                setLogParameter(i, "BigDecimal", bd);
                super.setBigDecimal(i, bd);
            }

            public void setString(int i, String s) throws SQLException {
                setLogParameter(i, "String", s);
                super.setString(i, s);
            }

            public void setBytes(int i, byte[] b) throws SQLException {
                setLogParameter(i, "byte[]", b);
                super.setBytes(i, b);
            }

            public void setDate(int i, Date d) throws SQLException {
                setLogParameter(i, "Date", d);
                super.setDate(i, d);
            }

            public void setTime(int i, Time t) throws SQLException {
                setLogParameter(i, "Time", t);
                super.setTime(i, t);
            }

            public void setTimestamp(int i, Timestamp t) throws SQLException {
                setLogParameter(i, "Timestamp", t);
                super.setTimestamp(i, t);
            }

            public void setAsciiStream(int i1, InputStream is, int i2)
                throws SQLException {
                setLogParameter(i1, "InputStream", is);
                super.setAsciiStream(i1, is, i2);
            }

            public void setUnicodeStream(int i1, InputStream is, int i2)
                throws SQLException {
                setLogParameter(i1, "InputStream", is);
                super.setUnicodeStream(i2, is, i2);
            }

            public void setBinaryStream(int i1, InputStream is, int i2)
                throws SQLException {
                setLogParameter(i1, "InputStream", is);
                super.setBinaryStream(i1, is, i2);
            }

            public void clearParameters() throws SQLException {
                clearLogParameters(false);
                super.clearParameters();
            }

            public void setObject(int i1, Object o, int i2, int i3)
                throws SQLException {
                setLogParameter(i1, "Object", o);
                super.setObject(i1, o, i2, i3);
            }

            public void setObject(int i1, Object o, int i2)
                throws SQLException {
                setLogParameter(i1, "Object", o);
                super.setObject(i1, o, i2);
            }

            public void setObject(int i, Object o) throws SQLException {
                setLogParameter(i, "Object", o);
                super.setObject(i, o);
            }

            public void addBatch() throws SQLException {
                long start = System.currentTimeMillis();

                try {
                    super.addBatch();
                    if (shouldTrackParameters()) {
                        // make sure our list is initialized
                        if (_paramBatch == null)
                            _paramBatch = new ArrayList();
                        // copy parameters since they will be re-used
                        if (_params != null)
                            _paramBatch.add(new ArrayList(_params));
                        else
                            _paramBatch.add(null);
                    }
                }
                finally {
                    log("batching", start);
                }
            }

            public void setCharacterStream(int i1, Reader r, int i2)
                throws SQLException {
                setLogParameter(i1, "Reader", r);
                super.setCharacterStream(i1, r, i2);
            }

            public void setRef(int i, Ref r) throws SQLException {
                setLogParameter(i, "Ref", r);
                super.setRef(i, r);
            }

            public void setBlob(int i, Blob b) throws SQLException {
                setLogParameter(i, "Blob", b);
                super.setBlob(i, b);
            }

            public void setClob(int i, Clob c) throws SQLException {
                setLogParameter(i, "Clob", c);
                super.setClob(i, c);
            }

            public void setArray(int i, Array a) throws SQLException {
                setLogParameter(i, "Array", a);
                super.setArray(i, a);
            }

            public ResultSetMetaData getMetaData() throws SQLException {
                return super.getMetaData();
            }

            public void setDate(int i, Date d, Calendar c) throws SQLException {
                setLogParameter(i, "Date", d);
                super.setDate(i, d, c);
            }

            public void setTime(int i, Time t, Calendar c) throws SQLException {
                setLogParameter(i, "Time", t);
                super.setTime(i, t, c);
            }

            public void setTimestamp(int i, Timestamp t, Calendar c)
                throws SQLException {
                setLogParameter(i, "Timestamp", t);
                super.setTimestamp(i, t, c);
            }

            public void setNull(int i1, int i2, String s) throws SQLException {
                setLogParameter(i1, "null", null);
                super.setNull(i1, i2, s);
            }

            protected void appendInfo(StringBuffer buf) {
                buf.append(" ");
                if (_formatter != null) {
                    buf.append(SEP);
                    buf.append(_formatter.prettyPrint(_sql));
                    buf.append(SEP);
                } else {
                    buf.append(_sql);
                }

                StringBuffer paramBuf = null;
                if (_params != null && !_params.isEmpty()) {
                    paramBuf = new StringBuffer();
                    for (Iterator itr = _params.iterator(); itr.hasNext();) {
                        paramBuf.append(itr.next());
                        if (itr.hasNext())
                            paramBuf.append(", ");
                    }
                }

                if (paramBuf != null) {
                    if (!_prettyPrint)
                        buf.append(" ");
                    buf.append("[params=").
                        append(paramBuf.toString()).append("]");
                }
                super.appendInfo(buf);
            }

            private void log(String msg, long startTime) throws SQLException {
                if (_logs.isSQLEnabled())
                    _logs.logSQL(msg + " " + this, startTime,
                        LoggingConnection.this);
            }

            private void clearLogParameters(boolean batch) {
                if (_params != null)
                    _params.clear();
                if (batch && _paramBatch != null)
                    _paramBatch.clear();
            }

            private boolean shouldTrackParameters() {
                return _trackParameters || _logs.isSQLEnabled();
            }

            private void setLogParameter(int index, boolean val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(boolean) " + val);
            }

            private void setLogParameter(int index, byte val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(byte) " + val);
            }

            private void setLogParameter(int index, double val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(double) " + val);
            }

            private void setLogParameter(int index, float val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(float) " + val);
            }

            private void setLogParameter(int index, int val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(int) " + val);
            }

            private void setLogParameter(int index, long val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(long) " + val);
            }

            private void setLogParameter(int index, short val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(short) " + val);
            }

            private void setLogParameter(int index, String type, Object val) {
                if (shouldTrackParameters())
                    setLogParameter(index, "(" + type + ") " + val);
            }

            private void setLogParameter(int index, String val) {
                if (_params == null)
                    _params = new ArrayList();
                while (_params.size() < index)
                    _params.add(null);
                if (val.length() > 80)
                    val = val.substring(0, 77) + "...";
                _params.set(index - 1, val);
            }
        }

        private class LoggingResultSet extends DelegatingResultSet {

            public LoggingResultSet(ResultSet rs, Statement stmnt) {
                super(rs, stmnt);
            }

            public boolean next() throws SQLException {
                try {
                    return super.next();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public void close() throws SQLException {
                try {
                    super.close();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public void beforeFirst() throws SQLException {
                try {
                    super.beforeFirst();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public void afterLast() throws SQLException {
                try {
                    super.afterLast();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public boolean first() throws SQLException {
                try {
                    return super.first();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public boolean last() throws SQLException {
                try {
                    return super.last();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public boolean absolute(int a) throws SQLException {
                try {
                    return super.absolute(a);
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public boolean relative(int a) throws SQLException {
                try {
                    return super.relative(a);
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }

            public boolean previous() throws SQLException {
                try {
                    return super.previous();
                }
                finally {
                    handleSQLWarning(LoggingResultSet.this);
                }
            }
        }
    }
}

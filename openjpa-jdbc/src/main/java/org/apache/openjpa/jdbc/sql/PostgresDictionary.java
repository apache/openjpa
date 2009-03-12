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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.jdbc.DelegatingConnection;
import org.apache.openjpa.lib.jdbc.DelegatingPreparedStatement;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.StoreException;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;

/**
 * Dictionary for Postgres.
 */
public class PostgresDictionary
    extends DBDictionary {

    private static final Localizer _loc = Localizer.forPackage
        (PostgresDictionary.class);

    /**
     * SQL statement to load all sequence schema,name pairs from all schemas.
     */
    public String allSequencesSQL = "SELECT NULL AS SEQUENCE_SCHEMA, relname " +
        "AS SEQUENCE_NAME FROM pg_class WHERE relkind='S'";

    /**
     * SQL statement to load schema,name pairs for all sequences with a
     * certain name from all schemas.
     */
    public String namedSequencesFromAllSchemasSQL = "SELECT NULL AS " +
        "SEQUENCE_SCHEMA, relname AS SEQUENCE_NAME FROM pg_class " +
        "WHERE relkind='S' AND relname = ?";

    /**
     * SQL statement to load schema,name pairs from a named schema.
     */
    public String allSequencesFromOneSchemaSQL = "SELECT NULL AS " +
        "SEQUENCE_SCHEMA, relname AS SEQUENCE_NAME FROM pg_class, " +
        "pg_namespace WHERE relkind='S' AND pg_class.relnamespace = " +
        "pg_namespace.oid AND nspname = ?";

    /**
     * SQL statement to load a sequence's schema,name pair from one schema.
     */
    public String namedSequenceFromOneSchemaSQL = "SELECT NULL AS " +
        "SEQUENCE_SCHEMA, relname AS SEQUENCE_NAME FROM pg_class, " +
        "pg_namespace WHERE relkind='S' AND pg_class.relnamespace = " +
        "pg_namespace.oid AND relname = ? AND nspname = ?";

    /**
     * Some Postgres drivers do not support the {@link Statement#setFetchSize}
     * method.
     */
    public boolean supportsSetFetchSize = true;

    public PostgresDictionary() {
        platform = "PostgreSQL";
        validationSQL = "SELECT NOW()";
        datePrecision = CENTI;
        supportsAlterTableWithDropColumn = false;
        supportsDeferredConstraints = true;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;

        // PostgreSQL requires double-escape for strings
        searchStringEscape = "\\\\";

        maxTableNameLength = 63;
        maxColumnNameLength = 63;
        maxIndexNameLength = 63;
        maxConstraintNameLength = 63;
        maxAutoAssignNameLength = 63;
        schemaCase = SCHEMA_CASE_LOWER;
        rangePosition = RANGE_POST_LOCK;
        requiresAliasForSubselect = true;
        allowsAliasInBulkClause = false;

        // {2} is the result of getGeneratedKeySequenceName; the
        // single-quote escape will result in SELECT CURVAL('mysequence')
        lastGeneratedKeyQuery = "SELECT CURRVAL(''{2}'')";
        supportsAutoAssign = true;
        autoAssignTypeName = "BIGSERIAL";
        nextSequenceQuery = "SELECT NEXTVAL(''{0}'')";

        useGetBytesForBlobs = true;
        useSetBytesForBlobs = true;
        useGetStringForClobs = true;
        useSetStringForClobs = true;
        bitTypeName = "BOOL";
        smallintTypeName = "SMALLINT";
        realTypeName = "FLOAT8";
        tinyintTypeName = "SMALLINT";
        binaryTypeName = "BYTEA";
        blobTypeName = "BYTEA";
        longVarbinaryTypeName = "BYTEA";
        varbinaryTypeName = "BYTEA";
        clobTypeName = "TEXT";
        longVarcharTypeName = "TEXT";
        doubleTypeName = "DOUBLE PRECISION";
        varcharTypeName = "VARCHAR{0}";
        timestampTypeName = "ABSTIME";
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "BOOL", "BYTEA", "NAME", "INT8", "INT2", "INT2VECTOR", "INT4",
            "REGPROC", "TEXT", "OID", "TID", "XID", "CID", "OIDVECTOR",
            "SET", "FLOAT4", "FLOAT8", "ABSTIME", "RELTIME", "TINTERVAL",
            "MONEY",
        }));

        supportsLockingWithDistinctClause = false;
        supportsLockingWithOuterJoin = false;
        supportsNullTableForGetImportedKeys = true;

        reservedWordSet.addAll(Arrays.asList(new String[]{
            "ABORT", "ACL", "AGGREGATE", "APPEND", "ARCHIVE", "ARCH_STORE",
            "BACKWARD", "BINARY", "CHANGE", "CLUSTER", "COPY", "DATABASE",
            "DELIMITER", "DELIMITERS", "DO", "EXPLAIN", "EXTEND",
            "FORWARD", "HEAVY", "INDEX", "INHERITS", "ISNULL", "LIGHT",
            "LISTEN", "LOAD", "MERGE", "NOTHING", "NOTIFY", "NOTNULL",
            "OID", "OIDS", "PURGE", "RECIPE", "RENAME", "REPLACE",
            "RETRIEVE", "RETURNS", "RULE", "SETOF", "STDIN", "STDOUT",
            "STORE", "VACUUM", "VERBOSE", "VERSION",
        }));
    }

    public Date getDate(ResultSet rs, int column)
        throws SQLException {
        try {
            return super.getDate(rs, column);
        } catch (StringIndexOutOfBoundsException sioobe) {
            // there is a bug in some versions of the postgres JDBC
            // driver such that a date with not enough numbers in it
            // will throw a parsing exception: this tries to work
            // around it. The bug only occurs when there is a trailing
            // millisecond missing from the end. E.g., when the date is
            // like:
            // 2066-10-19 22:08:32.83
            // rather than what the driver expects:
            // 2066-10-19 22:08:32.830
            String dateStr = rs.getString(column);
            SimpleDateFormat fmt = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss.SS");
            try {
                return fmt.parse(dateStr);
            } catch (ParseException pe) {
                throw new SQLException(pe.toString());
            }
        }
    }

    public byte getByte(ResultSet rs, int column)
        throws SQLException {
        // postgres does not perform automatic conversions, so attempting to
        // get a whole number out of a decimal will throw an exception.
        // fall back to performing manual conversion if the initial get fails
        try {
            return super.getByte(rs, column);
        } catch (SQLException sqle) {
            return super.getBigDecimal(rs, column).byteValue();
        }
    }

    public short getShort(ResultSet rs, int column)
        throws SQLException {
        // postgres does not perform automatic conversions, so attempting to
        // get a whole number out of a decimal will throw an exception.
        // fall back to performing manual conversion if the initial get fails
        try {
            return super.getShort(rs, column);
        } catch (SQLException sqle) {
            return super.getBigDecimal(rs, column).shortValue();
        }
    }

    public int getInt(ResultSet rs, int column)
        throws SQLException {
        // postgres does not perform automatic conversions, so attempting to
        // get a whole number out of a decimal will throw an exception.
        // fall back to performing manual conversion if the initial get fails
        try {
            return super.getInt(rs, column);
        } catch (SQLException sqle) {
            return super.getBigDecimal(rs, column).intValue();
        }
    }

    public long getLong(ResultSet rs, int column)
        throws SQLException {
        // postgres does not perform automatic conversions, so attempting to
        // get a whole number out of a decimal will throw an exception.
        // fall back to performing manual conversion if the initial get fails
        try {
            return super.getLong(rs, column);
        } catch (SQLException sqle) {
            return super.getBigDecimal(rs, column).longValue();
        }
    }

    public void setBoolean(PreparedStatement stmnt, int idx, boolean val,
        Column col)
        throws SQLException {
        // postgres actually requires that a boolean be set: it cannot
        // handle a numeric argument.
        stmnt.setBoolean(idx, val);
    }

    public void setNull(PreparedStatement stmnt, int idx, int colType,
        Column col)
        throws SQLException {
        // OPENJPA-
        if (colType == Types.BLOB)
            colType = Types.BINARY;
        stmnt.setNull(idx, colType);
    }

    protected void appendSelectRange(SQLBuffer buf, long start, long end,
        boolean subselect) {
        if (end != Long.MAX_VALUE)
            buf.append(" LIMIT ").appendValue(end - start);
        if (start != 0)
            buf.append(" OFFSET ").appendValue(start);
    }

    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("(POSITION(");
        find.appendTo(buf);
        buf.append(" IN ");
        if (start != null)
            substring(buf, str, start, null);
        else
            str.appendTo(buf);
        buf.append(") - 1");
        if (start != null) {
            buf.append(" + ");
            start.appendTo(buf);
        }
        buf.append(")");
    }

    public String[] getCreateSequenceSQL(Sequence seq) {
        String[] sql = super.getCreateSequenceSQL(seq);
        if (seq.getAllocate() > 1)
            sql[0] += " CACHE " + seq.getAllocate();
        return sql;
    }

    protected boolean supportsDeferredUniqueConstraints() {
        // Postgres only supports deferred foreign key constraints.
        return false;
    }

    protected String getSequencesSQL(String schemaName, String sequenceName) {
        if (schemaName == null && sequenceName == null)
            return allSequencesSQL;
        else if (schemaName == null)
            return namedSequencesFromAllSchemasSQL;
        else if (sequenceName == null)
            return allSequencesFromOneSchemaSQL;
        else
            return namedSequenceFromOneSchemaSQL;
    }

    public boolean isSystemSequence(String name, String schema,
        boolean targetSchema) {
        if (super.isSystemSequence(name, schema, targetSchema))
            return true;

        // filter out generated sequences used for bigserial cols, which are
        // of the form <table>_<col>_seq
        int idx = name.indexOf('_');
        return idx != -1 && idx != name.length() - 4
            && name.toUpperCase().endsWith("_SEQ");
    }

    public boolean isSystemTable(String name, String schema,
        boolean targetSchema) {
        // names starting with "pg_" are reserved for Postgresql internal use
        return super.isSystemTable(name, schema, targetSchema)
            || (name != null && name.toLowerCase().startsWith("pg_"));
    }

    public boolean isSystemIndex(String name, Table table) {
        // names starting with "pg_" are reserved for Postgresql internal use
        return super.isSystemIndex(name, table)
            || (name != null && name.toLowerCase().startsWith("pg_"));
    }

    public Connection decorate(Connection conn)
        throws SQLException {
        return new PostgresConnection(super.decorate(conn), this);
    }

    public InputStream getLOBStream(JDBCStore store, ResultSet rs,
        int column) throws SQLException {
        DelegatingConnection conn = (DelegatingConnection)store
            .getConnection();
        conn.setAutoCommit(false);
        LargeObjectManager lom = ((PGConnection)conn.getInnermostDelegate())
        .getLargeObjectAPI();
        if (rs.getInt(column) != -1) {
            LargeObject lo = lom.open(rs.getInt(column));
            return lo.getInputStream();
        } else {
            return null;
        }
    }

    public void insertBlobForStreamingLoad(Row row, Column col, 
        JDBCStore store, Object ob, Select sel) throws SQLException {
        if (row.getAction() == Row.ACTION_INSERT) {
            insertPostgresBlob(row, col, store, ob);
        } else if (row.getAction() == Row.ACTION_UPDATE) {
            updatePostgresBlob(row, col, store, ob, sel);
        }
    }

    private void insertPostgresBlob(Row row, Column col, JDBCStore store,
        Object ob) throws SQLException {
        if (ob != null) {
            col.setType(Types.INTEGER);
            DelegatingConnection conn = (DelegatingConnection)store
            .getConnection();
            try {
                conn.setAutoCommit(false);
                PGConnection pgconn = (PGConnection) conn.getInnermostDelegate();
                LargeObjectManager lom = pgconn.getLargeObjectAPI();
                // The create method is valid in versions previous 8.3
                // in 8.3 this methos is deprecated, use createLO
                int oid = lom.create();
                LargeObject lo = lom.open(oid, LargeObjectManager.WRITE);
                OutputStream os = lo.getOutputStream();
                copy((InputStream)ob, os);
                lo.close();
                row.setInt(col, oid);
            } catch (IOException ioe) {
                throw new StoreException(ioe);
            } finally {
                conn.close();
            }
        } else {
            row.setInt(col, -1);
        }
    }
    
    private void updatePostgresBlob(Row row, Column col, JDBCStore store,
        Object ob, Select sel) throws SQLException {
        SQLBuffer sql = sel.toSelect(true, store.getFetchConfiguration());
        ResultSet res = null;
        DelegatingConnection conn = 
            (DelegatingConnection) store.getConnection();
        PreparedStatement stmnt = null;
        try {
            stmnt = sql.prepareStatement(conn, store.getFetchConfiguration(),
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            res = stmnt.executeQuery();
            if (!res.next()) {
                throw new InternalException(_loc.get("stream-exception"));
            }
            int oid = res.getInt(1);
            if (oid != -1) {
                conn.setAutoCommit(false);
                PGConnection pgconn = (PGConnection)conn
                    .getInnermostDelegate();
                LargeObjectManager lom = pgconn.getLargeObjectAPI();
                if (ob != null) {
                    LargeObject lo = lom.open(oid, LargeObjectManager.WRITE);
                    OutputStream os = lo.getOutputStream();
                    copy((InputStream)ob, os);
                    lo.close();
                } else {
                    lom.delete(oid);
                    row.setInt(col, -1);
                }
            } else {
                if (ob != null) {
                    conn.setAutoCommit(false);
                    PGConnection pgconn = (PGConnection)conn
                        .getInnermostDelegate();
                    LargeObjectManager lom = pgconn.getLargeObjectAPI();
                    oid = lom.create();
                    LargeObject lo = lom.open(oid, LargeObjectManager.WRITE);
                    OutputStream os = lo.getOutputStream();
                    copy((InputStream)ob, os);
                    lo.close();
                    row.setInt(col, oid);
                }
            }

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
    
    public void updateBlob(Select sel, JDBCStore store, InputStream is)
        throws SQLException {
        //Do nothing
    }

    public void deleteStream(JDBCStore store, Select sel) throws SQLException {
        SQLBuffer sql = sel.toSelect(true, store.getFetchConfiguration());
        ResultSet res = null;
        DelegatingConnection conn = 
            (DelegatingConnection) store.getConnection();
        PreparedStatement stmnt = null;
        try {
            stmnt = sql.prepareStatement(conn, store.getFetchConfiguration(),
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            res = stmnt.executeQuery();
            if (!res.next()) {
                throw new InternalException(_loc.get("stream-exception"));
            }
            int oid = res.getInt(1);
            if (oid != -1) {
                conn.setAutoCommit(false);
                PGConnection pgconn = (PGConnection)conn
                    .getInnermostDelegate();
                LargeObjectManager lom = pgconn.getLargeObjectAPI();
                lom.delete(oid);
            }
        } finally {
            if (res != null)
                try { res.close (); } catch (SQLException e) {}
            if (stmnt != null)
                try { stmnt.close (); } catch (SQLException e) {}
            if (conn != null)
                try { conn.close (); } catch (SQLException e) {}
        }
    }
    
    /**
     * Connection wrapper to work around the postgres empty result set bug.
     */
    private static class PostgresConnection
        extends DelegatingConnection {

        private final PostgresDictionary _dict;

        public PostgresConnection(Connection conn, PostgresDictionary dict) {
            super(conn);
            _dict = dict;
        }

        protected PreparedStatement prepareStatement(String sql, boolean wrap)
            throws SQLException {
            return new PostgresPreparedStatement(super.prepareStatement
                (sql, false), this, _dict);
        }

        protected PreparedStatement prepareStatement(String sql, int rsType,
            int rsConcur, boolean wrap)
            throws SQLException {
            return new PostgresPreparedStatement(super.prepareStatement
                (sql, rsType, rsConcur, false), this, _dict);
        }
    }

    /**
     * Statement wrapper to work around the postgres empty result set bug.
     */
    private static class PostgresPreparedStatement
        extends DelegatingPreparedStatement {

        private final PostgresDictionary _dict;

        public PostgresPreparedStatement(PreparedStatement ps,
            Connection conn, PostgresDictionary dict) {
            super(ps, conn);
            _dict = dict;
        }

        protected ResultSet executeQuery(boolean wrap)
            throws SQLException {
            try {
                return super.executeQuery(wrap);
            } catch (SQLException se) {
                // we need to make our best guess whether this is the empty
                // ResultSet bug, since this exception could occur
                // for other reasons (like an invalid query string). Note
                // that Postgres error messages are localized, so we
                // cannot just parse the exception String.
                ResultSet rs = getResultSet(wrap);

                // ResultSet should be empty: if not, then maybe an
                // actual error occured
                if (rs == null)
                    throw se;

                return rs;
            }
        }

        public void setFetchSize(int i)
            throws SQLException {
            // some postgres drivers do not support the setFetchSize method
            try {
                if (_dict.supportsSetFetchSize)
                    super.setFetchSize(i);
            } catch (SQLException e) {
                _dict.supportsSetFetchSize = false;
                if (_dict.log.isWarnEnabled())
                    _dict.log.warn(_loc.get("psql-no-set-fetch-size"), e);
            }
        }
    }
}

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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.util.UnsupportedException;

/**
 * Dictionary for Informix database. Notable features:
 * <ul>
 * <li>Informix does not allow pessimistic locking on scrollable result
 * sets.</li>
 * <li>SET LOCK MODE TO WAIT N statements are issued to wait on locks. See
 * {@link #lockWaitSeconds} and {@link #lockModeEnabled}.</li>
 * <li>LOCK MODE ROW is used by default for table creation to allow the
 * maximum concurrency.</li>
 * </ul>
 */
public class InformixDictionary
    extends DBDictionary {

    /**
     * If true, then we will issue a "SET LOCK MODE TO WAIT N"
     * statement whenever we create a {@link Connection}, in order
     * allow waiting on locks.
     */
    public boolean lockModeEnabled = false;

    /**
     * If {@link #lockModeEnabled} is <code>true</code>, then this
     * parameter specifies the number of seconds we will wait to
     * obtain a lock for inserts and pessimistic locking.
     */
    public int lockWaitSeconds = 30;

    /**
     * Informix JDBC metadata for all known drivers returns with the
     * table catalog and the table schema name swapped. A <code>true</code>
     * value for this property indicates that they should be reversed.
     */
    public boolean swapSchemaAndCatalog = true;

    // weak set of connections we've already executed lock mode sql on
    private final Collection _seenConnections = new ReferenceHashSet
        (ReferenceHashSet.WEAK);

    public InformixDictionary() {
        platform = "Informix";
        validationSQL = "SELECT FIRST 1 CURRENT TIMESTAMP "
            + "FROM informix.systables";

        supportsAutoAssign = true;
        autoAssignTypeName = "serial";
        lastGeneratedKeyQuery = "SELECT FIRST 1 DBINFO('sqlca.sqlerrd1') "
            + "FROM informix.systables";

        // informix actually does support deferred constraints, but not
        // in the table definition; deferred constraints can be activated by
        // invoking "set constraints all deferred" on the connection, which
        // we don't do yet
        supportsDeferredConstraints = false;
        constraintNameMode = CONS_NAME_AFTER;

        // informix supports "CLOB" type, but any attempt to insert
        // into them raises: "java.sql.SQLException: Can't convert fromnull"
        useGetStringForClobs = true;
        longVarcharTypeName = "TEXT";
        clobTypeName = "TEXT";
        smallintTypeName = "INT8";
        tinyintTypeName = "INT8";
        floatTypeName = "FLOAT";
        bitTypeName = "BOOLEAN";
        blobTypeName = "BYTE";
        doubleTypeName = "NUMERIC(32,20)";
        dateTypeName = "DATE";
        timeTypeName = "DATETIME HOUR TO SECOND";
        timestampTypeName = "DATETIME YEAR TO FRACTION(3)";
        doubleTypeName = "NUMERIC(32,20)";
        floatTypeName = "REAL";
        bigintTypeName = "NUMERIC(32,0)";
        doubleTypeName = "DOUBLE PRECISION";
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "BYTE", "DOUBLE PRECISION", "INTERVAL", "SMALLFLOAT", "TEXT",
            "INT8",
        }));

        supportsQueryTimeout = false;
        supportsLockingWithDistinctClause = false;
        supportsLockingWithMultipleTables = false;
        supportsLockingWithOrderClause = false;

        // the informix JDBC drivers have problems with using the
        // schema name in reflection on columns and tables
        supportsSchemaForGetColumns = false;
        supportsSchemaForGetTables = false;

        // Informix doesn't support aliases in deletes if the table has an index
        allowsAliasInBulkClause = false;

        // Informix doesn't understand "X CROSS JOIN Y", but it does understand
        // the equivalent "X JOIN Y ON 1 = 1"
        crossJoinClause = "JOIN";
        requiresConditionForCrossJoin = true;

        concatenateFunction = "CONCAT({0},{1})";
        nextSequenceQuery = "SELECT {0}.NEXTVAL FROM SYSTABLES WHERE TABID=1";
        supportsCorrelatedSubselect = false;
        swapSchemaAndCatalog = false;

        // Informix does not support foreign key delete action NULL or DEFAULT
        supportsNullDeleteAction = false;
        supportsDefaultDeleteAction = false;

        trimSchemaName = true;
    }

    public void connectedConfiguration(Connection conn)
        throws SQLException {
        super.connectedConfiguration(conn);
        if (driverVendor == null) {
            DatabaseMetaData meta = conn.getMetaData();
            if ("Informix".equalsIgnoreCase(meta.getDriverName()))
                driverVendor = VENDOR_DATADIRECT;
            else
                driverVendor = VENDOR_OTHER;
        }
        if (isJDBC3) {
            conn.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
            if (log.isTraceEnabled())
                log.trace(_loc.get("connection-defaults", new Object[]{
                        new Boolean(conn.getAutoCommit()), 
                        new Integer(conn.getHoldability()),
                        new Integer(conn.getTransactionIsolation())}));
        }
    }

    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);

        // treat logvarchar as clob
        for (int i = 0; cols != null && i < cols.length; i++)
            if (cols[i].getType() == Types.LONGVARCHAR)
                cols[i].setType(Types.CLOB);
        return cols;
    }

    public Column newColumn(ResultSet colMeta)
        throws SQLException {
        Column col = super.newColumn(colMeta);
        if (swapSchemaAndCatalog)
            col.setSchemaName(colMeta.getString("TABLE_CAT"));
        return col;
    }

    public PrimaryKey newPrimaryKey(ResultSet pkMeta)
        throws SQLException {
        PrimaryKey pk = super.newPrimaryKey(pkMeta);
        if (swapSchemaAndCatalog)
            pk.setSchemaName(pkMeta.getString("TABLE_CAT"));
        return pk;
    }

    public Index newIndex(ResultSet idxMeta)
        throws SQLException {
        Index idx = super.newIndex(idxMeta);
        if (swapSchemaAndCatalog)
            idx.setSchemaName(idxMeta.getString("TABLE_CAT"));
        return idx;
    }

    public void setBoolean(PreparedStatement stmnt, int idx, boolean val,
        Column col)
        throws SQLException {
        // informix actually requires that a boolean be set: it cannot
        // handle a numeric argument
        stmnt.setString(idx, val ? "t" : "f");
    }

    public String[] getCreateTableSQL(Table table) {
        String[] create = super.getCreateTableSQL(table);
        create[0] = create[0] + " LOCK MODE ROW";
        return create;
    }

    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        String pksql = getPrimaryKeyConstraintSQL(pk);
        if (pksql == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(pk.getTable(), false) + " ADD CONSTRAINT " + pksql };
    }

    public String[] getAddForeignKeySQL(ForeignKey fk) {
        String fksql = getForeignKeyConstraintSQL(fk);
        if (fksql == null)
            return new String[0];
        return new String[]{ "ALTER TABLE "
            + getFullName(fk.getTable(), false) + " ADD CONSTRAINT " + fksql };
    }

    public boolean supportsRandomAccessResultSet(Select sel,
        boolean forUpdate) {
        return !forUpdate && !sel.isLob()
            && super.supportsRandomAccessResultSet(sel, forUpdate);
    }

    public Connection decorate(Connection conn)
        throws SQLException {
        conn = super.decorate(conn);

        // if we haven't already done so, initialize the lock mode of the
        // connection
        if (lockModeEnabled && _seenConnections.add(conn)) {
            String sql = "SET LOCK MODE TO WAIT";
            if (lockWaitSeconds > 0)
                sql = sql + " " + lockWaitSeconds;

            Statement stmnt = null;
            try {
                stmnt = conn.createStatement();
                stmnt.executeUpdate(sql);
            } catch (SQLException se) {
                throw SQLExceptions.getStore(se, this);
            } finally {
                if (stmnt != null)
                    try {
                        stmnt.close();
                    } catch (SQLException se) {
                    }
            }
        }

        // the datadirect driver requires that we issue a rollback before using
        // each connection
        if (VENDOR_DATADIRECT.equalsIgnoreCase(driverVendor))
            try {
                conn.rollback();
            } catch (SQLException se) {
            }
        return conn;
    }

    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
            FilterValue start) {
        throw new UnsupportedException();
    }

    public boolean needsToCreateIndex(Index idx, Table table) {
       // Informix will automatically create a unique index for the
       // primary key, so don't create another index again
       PrimaryKey pk = table.getPrimaryKey();
       if (pk != null && idx.columnsMatch(pk.getColumns()))
           return false;
       return true;
    }
}

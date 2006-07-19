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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.jdbc.DelegatingConnection;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Dictionary for Sybase.
 *  The main point of interest is that by default, every table
 * that is created will have a unique column named "UNQ_INDEX" of
 * the "IDENTITY" type. OpenJPA will not ever utilize this column. However,
 * due to internal Sybase restrictions, this column is required
 * in order to support pessimistic (datastore) locking, since Sybase
 * requires that any tables in a "SELECT ... FOR UPDATE" clause have
 * a unique index that is <strong>not</strong> included in the list
 * of columns, as described in the
 * <a href="http://www.sybase.com/detail/1,6904,1023075,00.html"
 * >Sybase documentation</a>. This behavior can be surpressed by setting the
 * dictionary property <code>CreateIdentityColumn=false</code>. The
 * name of the unique column can be changed by setting the property
 * <code>IdentityColumnName=COLUMN_NAME</code>.
 *  A good Sybase type reference is can be found <a
 * href="http://www.ispirer.com/doc/sqlways36/sybase/syb_dtypes.html">here</a>.
 */
public class SybaseDictionary
    extends AbstractSQLServerDictionary {

    private static Localizer _loc = Localizer.forPackage
        (SybaseDictionary.class);

    /**
     * If true, then whenever the <code>schematool</code> creates a
     * table, it will append an additional IDENTITY column to the
     * table's creation SQL. This is so Sybase will be able to
     * perform <code>SELECT...FOR UPDATE</code> statements.
     */
    public boolean createIdentityColumn = true;

    /**
     * If {@link #createIdentityColumn} is true, then the
     * <code>identityColumnName</code> will be the name of the
     * additional unique column that will be created.
     */
    public String identityColumnName = "UNQ_INDEX";

    public SybaseDictionary() {
        platform = "Sybase";
        schemaCase = SCHEMA_CASE_PRESERVE;
        forUpdateClause = "FOR UPDATE AT ISOLATION SERIALIZABLE";

        supportsLockingWithDistinctClause = false;
        supportsNullTableForGetColumns = false;
        requiresAutoCommitForMetaData = true;

        maxTableNameLength = 30;
        maxColumnNameLength = 30;
        maxIndexNameLength = 30;
        maxConstraintNameLength = 30;

        bigintTypeName = "NUMERIC(38)";
        bitTypeName = "TINYINT";

        // Sybase doesn't understand "X CROSS JOIN Y", but it does understand
        // the equivalent "X JOIN Y ON 1 = 1"
        crossJoinClause = "JOIN";
        requiresConditionForCrossJoin = true;

        // these tables should not be reflected on
        systemTableSet.addAll(Arrays.asList(new String[]{
            "IJDBC_FUNCTION_ESCAPES", "JDBC_FUNCTION_ESCAPES",
            "SPT_IJDBC_CONVERSION", "SPT_IJDBC_MDA", "SPT_IJDBC_TABLE_TYPES",
            "SPT_JDBC_CONVERSION", "SPT_JDBC_TABLE_TYPES", "SPT_JTEXT",
            "SPT_LIMIT_TYPES", "SPT_MDA", "SPT_MONITOR", "SPT_VALUES",
            "SYBLICENSESLOG",
        }));

        // reserved words specified at:
        // http://manuals.sybase.com/onlinebooks/group-as/asg1250e/
        // refman/@Generic__BookTextView/26603
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "ARITH_OVERFLOW", "BREAK", "BROWSE", "BULK", "CHAR_CONVERT",
            "CHECKPOINT", "CLUSTERED", "COMPUTE", "CONFIRM", "CONTROLROW",
            "DATABASE", "DBCC", "DETERMINISTIC", "DISK DISTINCT", "DUMMY",
            "DUMP", "ENDTRAN", "ERRLVL", "ERRORDATA", "ERROREXIT", "EXCLUSIVE",
            "EXIT", "EXP_ROW_SIZE", "FILLFACTOR", "FUNC", "FUNCTION",
            "HOLDLOCK", "IDENTITY_GAP", "IDENTITY_INSERT", "IDENTITY_START",
            "IF", "INDEX", "INOUT", "INSTALL", "INTERSECT", "JAR", "KILL",
            "LINENO", "LOAD", "LOCK", "MAX_ROWS_PER_PAGE", "MIRROR",
            "MIRROREXIT", "MODIFY", "NEW", "NOHOLDLOCK", "NONCLUSTERED",
            "NUMERIC_TRUNCATION", "OFF", "OFFSETS", "ONCE", "ONLINE", "OUT",
            "OVER", "PARTITION", "PERM", "PERMANENT", "PLAN", "PRINT", "PROC",
            "PROCESSEXIT", "PROXY_TABLE", "QUIESCE", "RAISERROR", "READ",
            "READPAST", "READTEXT", "RECONFIGURE", "REFERENCES REMOVE", "REORG",
            "REPLACE", "REPLICATION", "RESERVEPAGEGAP", "RETURN", "RETURNS",
            "ROLE", "ROWCOUNT", "RULE", "SAVE", "SETUSER", "SHARED",
            "SHUTDOWN", "SOME", "STATISTICS", "STRINGSIZE", "STRIPE",
            "SYB_IDENTITY", "SYB_RESTREE", "SYB_TERMINATE", "TEMP", "TEXTSIZE",
            "TRAN", "TRIGGER", "TRUNCATE", "TSEQUAL", "UNPARTITION", "USE",
            "USER_OPTION", "WAITFOR", "WHILE", "WRITETEXT",
        }));
    }

    public int getJDBCType(int metaTypeCode, boolean lob) {
        switch (metaTypeCode) {
            // the default mapping for BYTE is a TINYINT, but Sybase's TINYINT
            // type can't handle the complete range for a Java byte
            case JavaTypes.BYTE:
            case JavaTypes.BYTE_OBJ:
                return getPreferredType(Types.SMALLINT);
            default:
                return super.getJDBCType(metaTypeCode, lob);
        }
    }

    public void setBigInteger(PreparedStatement stmnt, int idx, BigInteger val,
        Column col)
        throws SQLException {
        // setBigDecimal doesn't work here: in one case, a stored value
        // of 7799438514924349440 turns into 7799438514924349400
        // setObject gets around this in the Sybase JDBC drivers
        setObject(stmnt, idx, new BigDecimal(val), Types.BIGINT, col);
    }

    public String[] getAddForeignKeySQL(ForeignKey fk) {
        // Sybase has problems with adding foriegn keys via ALTER TABLE command
        return new String[0];
    }

    public String[] getCreateTableSQL(Table table) {
        if (!createIdentityColumn)
            return super.getCreateTableSQL(table);

        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(getFullName(table, false)).
            append(" (");

        Column[] cols = table.getColumns();
        boolean hasIdentity = false;

        for (int i = 0; i < cols.length; i++) {
            if (cols[i].isAutoAssigned())
                hasIdentity = true;

            buf.append(i == 0 ? "" : ", ");
            buf.append(getDeclareColumnSQL(cols[i], false));
        }

        // add an identity column if we do not already have one
        if (!hasIdentity)
            buf.append(", ").append(identityColumnName).
                append(" NUMERIC IDENTITY UNIQUE");

        PrimaryKey pk = table.getPrimaryKey();
        if (pk != null)
            buf.append(", ").append(getPrimaryKeyConstraintSQL(pk));

        Unique[] unqs = table.getUniques();
        String unqStr;
        for (int i = 0; i < unqs.length; i++) {
            unqStr = getUniqueConstraintSQL(unqs[i]);
            if (unqStr != null)
                buf.append(", ").append(unqStr);
        }

        buf.append(")");
        return new String[]{ buf.toString() };
    }

    protected String getDeclareColumnSQL(Column col, boolean alter) {
        StringBuffer buf = new StringBuffer();
        buf.append(col).append(" ");
        buf.append(getTypeName(col));

        // can't add constraints to a column we're adding after table
        // creation, cause some data might already be inserted
        if (!alter) {
            if (col.getDefaultString() != null && !col.isAutoAssigned())
                buf.append(" DEFAULT ").append(col.getDefaultString());
            if (col.isAutoAssigned())
                buf.append(" IDENTITY");
        }

        if (col.isNotNull())
            buf.append(" NOT NULL");
        else if (!col.isPrimaryKey()) {
            // sybase forces you to explicitly specify that
            // you will allow NULL values
            buf.append(" NULL");
        }

        return buf.toString();
    }

    public String[] getDropColumnSQL(Column column) {
        // Sybase uses "ALTER TABLE DROP <COLUMN_NAME>" rather than the
        // usual "ALTER TABLE DROP COLUMN <COLUMN_NAME>"
        return new String[]{ "ALTER TABLE "
            + getFullName(column.getTable(), false) + " DROP " + column };
    }

    public void refSchemaComponents(Table table) {
        // note that we use getColumns() rather than getting the column by name
        // because under some circumstances this method is called under the
        // dynamic schema factory, where getting a column by name creates
        // that column
        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++)
            if (identityColumnName.equalsIgnoreCase(cols[i].getName()))
                cols[i].ref();
    }

    public void endConfiguration() {
        super.endConfiguration();

        // warn about jdbc compliant flag
        String url = conf.getConnectionURL();
        if (url != null && url.length() > 0
            && url.toLowerCase().indexOf("jdbc:sybase:tds") != -1
            && url.toLowerCase().indexOf("be_as_jdbc_compliant_as_possible=")
            == -1) {
            log.warn(_loc.get("sybase-compliance", url));
        }
    }

    public Connection decorate(Connection conn)
        throws SQLException {
        return new SybaseConnection(super.decorate(conn));
    }

    /**
     * Connection wrapper to cache the {@link Connection#getCatalog} result,
     * which takes a very long time with the Sybase Connection (and
     * which we frequently invoke).
     */
    private static class SybaseConnection
        extends DelegatingConnection {

        private String _catalog = null;

        public SybaseConnection(Connection conn) {
            super(conn);
        }

        public String getCatalog()
            throws SQLException {
            if (_catalog == null)
                _catalog = super.getCatalog();
            return _catalog;
        }

        public void setAutoCommit(boolean autocommit)
            throws SQLException {
            // the sybase jdbc driver demands that the Connection always
            // be rolled back before autocommit status changes. Failure to
            // do so will yield "SET CHAINED command not allowed within
            // multi-statement transaction." exceptions
            try {
                super.setAutoCommit(autocommit);
            } catch (SQLException e) {
                // failed for some reason: try rolling back and then
                // setting autocommit again.
                if (autocommit)
                    super.commit();
                else
                    super.rollback();
                super.setAutoCommit(autocommit);
            }
        }
    }
}

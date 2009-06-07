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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;

/**
 * Dictionary for MySQL.
 */
public class MySQLDictionary
    extends DBDictionary {

    public static final String SELECT_HINT = "openjpa.hint.MySQLSelectHint";

    /**
     * The MySQL table type to use when creating tables; defaults to innodb.
     */
    public String tableType = "innodb";

    /**
     * Whether to use clobs; defaults to true. Set this to false if you have an
     * old version of MySQL which does not handle clobs properly.
     */
    public boolean useClobs = true;

    /**
     * Whether the driver automatically deserializes blobs.
     */
    public boolean driverDeserializesBlobs = false;

    /**
     * Whether to inline multi-table bulk-delete operations into MySQL's 
     * combined <code>DELETE FROM foo, bar, baz</code> syntax. 
     * Defaults to false, since this may fail in the presence of InnoDB tables
     * with foreign keys.
     * @see http://dev.mysql.com/doc/refman/5.0/en/delete.html
     */
    public boolean optimizeMultiTableDeletes = false;

    public MySQLDictionary() {
        platform = "MySQL";
        validationSQL = "SELECT NOW()";
        distinctCountColumnSeparator = ",";

        supportsDeferredConstraints = false;
        constraintNameMode = CONS_NAME_MID;
        supportsMultipleNontransactionalResultSets = false;
        requiresAliasForSubselect = true; // new versions
        requiresTargetForDelete = true;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;

        concatenateFunction = "CONCAT({0},{1})";

        maxTableNameLength = 64;
        maxColumnNameLength = 64;
        maxIndexNameLength = 64;
        maxConstraintNameLength = 64;
        maxIndexesPerTable = 32;
        schemaCase = SCHEMA_CASE_PRESERVE;

        supportsAutoAssign = true;
        lastGeneratedKeyQuery = "SELECT LAST_INSERT_ID()";
        autoAssignClause = "AUTO_INCREMENT";

        clobTypeName = "TEXT";
        longVarcharTypeName = "TEXT";
        longVarbinaryTypeName = "LONG VARBINARY";
        timestampTypeName = "DATETIME";
        xmlTypeName = "TEXT";
        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "BOOL", "LONG VARBINARY", "MEDIUMBLOB", "LONGBLOB",
            "TINYBLOB", "LONG VARCHAR", "MEDIUMTEXT", "LONGTEXT", "TEXT",
            "TINYTEXT", "DOUBLE PRECISION", "ENUM", "SET", "DATETIME",
        }));
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "INT1", "INT2", "INT4", "FLOAT1", "FLOAT2", "FLOAT4",
            "AUTO_INCREMENT", "BINARY", "BLOB", "CHANGE", "ENUM", "INFILE",
            "LOAD", "MEDIUMINT", "OPTION", "OUTFILE", "REPLACE",
            "SET", "STARTING", "TEXT", "UNSIGNED", "ZEROFILL",
        }));
        // reservedWordSet subset that can be used as valid column names
        validColumnWordSet.addAll(Arrays.asList(new String[]{
            "C", "COUNT", "DATE", "DATA", "NAME", "NULLABLE", "NUMBER", 
            "TIMESTAMP", "TYPE", "VALUE", 
        }));

        // MySQL requires double-escape for strings
        searchStringEscape = "\\\\";

        typeModifierSet.addAll(Arrays.asList(new String[] { "UNSIGNED",
            "ZEROFILL" }));
    }

    public void connectedConfiguration(Connection conn) throws SQLException {
        super.connectedConfiguration(conn);

        DatabaseMetaData metaData = conn.getMetaData();
        int maj = 0;
        int min = 0;
        if (isJDBC3) {
            maj = metaData.getDatabaseMajorVersion();
            min = metaData.getDatabaseMinorVersion();
        } else {
            try {
                // The product version looks like 4.1.3-nt or 5.1.30
                String productVersion = metaData.getDatabaseProductVersion();
                int[] versions = getMajorMinorVersions(productVersion);
                maj = versions[0];
                min = versions[1];
            } catch (IllegalArgumentException e) {
                // we don't understand the version format.
                // That is ok. We just take the default values.
                if (log.isWarnEnabled())
                    log.warn(e.toString(), e);
            }
        }
        if (maj < 4 || (maj == 4 && min < 1)) {
            supportsSubselect = false;
            allowsAliasInBulkClause = false;
        }
        if (maj > 5 || (maj == 5 && min >= 1))
            supportsXMLColumn = true;

        if (metaData.getDriverMajorVersion() < 5)
            driverDeserializesBlobs = true;
    }

    private static int[] getMajorMinorVersions(String versionStr)
        throws IllegalArgumentException {
        int beginIndex = 0;

        versionStr = versionStr.trim();
        char[] charArr = versionStr.toCharArray();
        for (int i = 0; i < charArr.length; i++) {
            if (Character.isDigit(charArr[i])) {
                beginIndex = i;
                break;
            }
        }

        int endIndex = charArr.length;
        for (int i = beginIndex+1; i < charArr.length; i++) {
            if (charArr[i] != '.' && !Character.isDigit(charArr[i])) {
                endIndex = i;
                break;
            }
        }

        String[] arr = versionStr.substring(beginIndex, endIndex).split("\\.");
        if (arr.length < 2)
            throw new IllegalArgumentException();

        int maj = Integer.parseInt(arr[0]);
        int min = Integer.parseInt(arr[1]);
        return new int[]{maj, min};
    }

    public String[] getCreateTableSQL(Table table) {
        String[] sql = super.getCreateTableSQL(table);
        if (!StringUtils.isEmpty(tableType))
            sql[0] = sql[0] + " TYPE = " + tableType;
        return sql;
    }

    public String[] getDropIndexSQL(Index index) {
        return new String[]{ "DROP INDEX " + getFullName(index) + " ON "
            + getFullName(index.getTable(), false) };
    }

    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        String[] sql = super.getAddPrimaryKeySQL(pk);

        // mysql requires that a column be declared NOT NULL before
        // it can be made a primary key.
        Column[] cols = pk.getColumns();
        String[] ret = new String[cols.length + sql.length];
        for (int i = 0; i < cols.length; i++) {
            ret[i] = "ALTER TABLE " + getFullName(cols[i].getTable(), false)
                + " CHANGE " + cols[i].getName()
                + " " + cols[i].getName() // name twice
                + " " + getTypeName(cols[i]) + " NOT NULL";
        }

        System.arraycopy(sql, 0, ret, cols.length, sql.length);
        return ret;
    }

    protected String getForeignKeyConstraintSQL(ForeignKey fk) {
        // mysql does not support composite foreign keys
        if (fk.getColumns().length > 1)
            return null;
        return super.getForeignKeyConstraintSQL(fk);
    }
    
    public String[] getDeleteTableContentsSQL(Table[] tables,Connection conn) {
        // mysql >= 4 supports more-optimal delete syntax
        if (!optimizeMultiTableDeletes)
            return super.getDeleteTableContentsSQL(tables,conn);
        else {
            StringBuffer buf = new StringBuffer(tables.length * 8);
            buf.append("DELETE FROM ");
            for (int i = 0; i < tables.length; i++) {
                buf.append(tables[i].getFullName());
                if (i < tables.length - 1)
                    buf.append(", ");
            }
            return new String[] { buf.toString() };
        }
    }

    protected void appendSelectRange(SQLBuffer buf, long start, long end,
        boolean subselect) {
        buf.append(" LIMIT ").appendValue(start).append(", ");
        if (end == Long.MAX_VALUE)
            buf.appendValue(Long.MAX_VALUE);
        else
            buf.appendValue(end - start);
    }

    protected Column newColumn(ResultSet colMeta)
        throws SQLException {
        Column col = super.newColumn(colMeta);
        if (col.isNotNull() && "0".equals(col.getDefaultString()))
            col.setDefaultString(null);
        return col;
    }

    public Object getBlobObject(ResultSet rs, int column, JDBCStore store)
        throws SQLException {
        // if the user has set a get-blob strategy explicitly or the driver
        // does not automatically deserialize, delegate to super
        if (useGetBytesForBlobs || useGetObjectForBlobs
            || !driverDeserializesBlobs)
            return super.getBlobObject(rs, column, store);

        // most mysql drivers deserialize on getObject
        return rs.getObject(column);
    }

    public int getPreferredType(int type) {
        if (type == Types.CLOB && !useClobs)
            return Types.LONGVARCHAR;
        return super.getPreferredType(type);
    }
    
    /**
     * Append XML comparison.
     * 
     * @param buf the SQL buffer to write the comparison
     * @param op the comparison operation to perform
     * @param lhs the left hand side of the comparison
     * @param rhs the right hand side of the comparison
     * @param lhsxml indicates whether the left operand maps to XML
     * @param rhsxml indicates whether the right operand maps to XML
     */
    public void appendXmlComparison(SQLBuffer buf, String op, FilterValue lhs,
        FilterValue rhs, boolean lhsxml, boolean rhsxml) {
        super.appendXmlComparison(buf, op, lhs, rhs, lhsxml, rhsxml);
        if (lhsxml)
            appendXmlValue(buf, lhs);
        else
            lhs.appendTo(buf);
        buf.append(" ").append(op).append(" ");
        if (rhsxml)
            appendXmlValue(buf, rhs);
        else
            rhs.appendTo(buf);
    }
    
    /**
     * Append XML column value so that it can be used in comparisons.
     * 
     * @param buf the SQL buffer to write the value
     * @param val the value to be written
     */
    private void appendXmlValue(SQLBuffer buf, FilterValue val) {
        buf.append("ExtractValue(").
            append(val.getColumnAlias(val.getFieldMapping().getColumns()[0])).
            append(",'/*/");
        val.appendTo(buf);
        buf.append("')");
    }
    
    public int getBatchFetchSize(int batchFetchSize) {
        return Integer.MIN_VALUE;
    }

    /**
     * Check to see if we have set the {@link #SELECT_HINT} in the
     * fetch configuration, and if so, append the MySQL hint after the
     * "SELECT" part of the query.
     */
    @Override
    public String getSelectOperation(JDBCFetchConfiguration fetch) {
        Object hint = fetch == null ? null : fetch.getHint(SELECT_HINT);
        String select = "SELECT";
        if (hint != null)
            select += " " + hint;
        return select;
    }    
}

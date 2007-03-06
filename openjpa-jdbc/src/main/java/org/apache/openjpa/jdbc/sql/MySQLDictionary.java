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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
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

    /**
     * The MySQL table type to use when creating tables; defaults to innodb.
     */
    public String tableType = "innodb";

    /**
     * Whether to use clobs. Some older versions of MySQL do not handle
     * clobs properly so we default to false here.
     */
    public boolean useClobs = true;

    /**
     * Whether the driver automatically deserializes blobs.
     */
    public boolean driverDeserializesBlobs = true;

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
        supportsSubselect = false; // old versions
        requiresAliasForSubselect = true; // new versions
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;
        allowsAliasInBulkClause = false;

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

        // MySQL requires double-escape for strings
        searchStringEscape = "\\\\";
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
    
    public String[] getDeleteTableContentsSQL(Table[] tables) {
        // mysql >= 4 supports more-optimal delete syntax
        if (!optimizeMultiTableDeletes)
            return super.getDeleteTableContentsSQL(tables);
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

    protected void appendSelectRange(SQLBuffer buf, long start, long end) {
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
}

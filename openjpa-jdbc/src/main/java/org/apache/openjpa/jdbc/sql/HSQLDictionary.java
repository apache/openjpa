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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.hsqldb.Trace;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.ReferentialIntegrityException;
import serp.util.Numbers;

/**
 * Dictionary for Hypersonic SQL database.
 */
public class HSQLDictionary
    extends DBDictionary {

    /**
     * Sets whether HSQL should use "CREATED CACHED TABLE" rather than
     * "CREATE TABLE", which allows disk-based database operations.
     */
    public boolean cacheTables = false;

    private SQLBuffer _oneBuffer = new SQLBuffer(this).append("1");

    public HSQLDictionary() {
        platform = "HSQL";
        validationSQL = "CALL 1";
        closePoolSQL = "SHUTDOWN";

        supportsAutoAssign = true;
        lastGeneratedKeyQuery = "CALL IDENTITY()";
        autoAssignClause = "IDENTITY";
        autoAssignTypeName = "INTEGER";
        nextSequenceQuery = "SELECT NEXT VALUE FOR {0} FROM"
            + " INFORMATION_SCHEMA.SYSTEM_SEQUENCES";
        crossJoinClause = "JOIN";
        requiresConditionForCrossJoin = true;
        stringLengthFunction = "LENGTH({0})";
        trimLeadingFunction = "LTRIM({0})";
        trimTrailingFunction = "RTRIM({0})";
        trimBothFunction = "LTRIM(RTRIM({0}))";

        // HSQL 1.8.0 does support schema names in the table ("schema.table"),
        // but doesn't support it for columns references ("schema.table.column")
        useSchemaName = false;
        supportsSelectForUpdate = false;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;
        rangePosition = RANGE_PRE_DISTINCT;
        supportsDeferredConstraints = false;

        useGetObjectForBlobs = true;
        blobTypeName = "VARBINARY";

        supportsNullTableForGetPrimaryKeys = true;
        supportsNullTableForGetIndexInfo = true;

        requiresCastForMathFunctions = true;
        requiresCastForComparisons = true;

        reservedWordSet.addAll(Arrays.asList(new String[]{
            "BEFORE", "BIGINT", "BINARY", "CACHED", "DATETIME", "LIMIT",
            "LONGVARBINARY", "LONGVARCHAR", "OBJECT", "OTHER",
            "SAVEPOINT", "TEMP", "TEXT", "TRIGGER", "TINYINT",
            "VARBINARY", "VARCHAR_IGNORECASE",
        }));
    }

    public int getJDBCType(int metaTypeCode, boolean lob) {
        int type = super.getJDBCType(metaTypeCode, lob);
        switch (type) {
            case Types.BIGINT:
                if (metaTypeCode == JavaTypes.BIGINTEGER)
                    return Types.NUMERIC;
                break;
        }
        return type;
    }

    public int getPreferredType(int type) {
        switch (type) {
            case Types.CLOB:
                return Types.VARCHAR;
            case Types.BLOB:
                return Types.VARBINARY;
            default:
                return super.getPreferredType(type);
        }
    }

    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        return new String[0];
    }

    public String[] getDropPrimaryKeySQL(PrimaryKey pk) {
        return new String[0];
    }

    public String[] getAddColumnSQL(Column column) {
        return new String[]{ "ALTER TABLE "
            + getFullName(column.getTable(), false)
            + " ADD COLUMN " + getDeclareColumnSQL(column, true) };
    }

    public String[] getCreateTableSQL(Table table) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE ");
        if (cacheTables)
            buf.append("CACHED ");
        buf.append("TABLE ").append(getFullName(table, false)).append(" (");

        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(getDeclareColumnSQL(cols[i], false));
        }

        PrimaryKey pk = table.getPrimaryKey();
        String pkStr;
        if (pk != null) {
            pkStr = getPrimaryKeyConstraintSQL(pk);
            if (pkStr != null && pkStr.length() > 0)
                buf.append(", ").append(pkStr);
        }

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

    protected String getPrimaryKeyConstraintSQL(PrimaryKey pk) {
        Column[] cols = pk.getColumns();
        if (cols.length == 1 && cols[0].isAutoAssigned())
            return null;
        return super.getPrimaryKeyConstraintSQL(pk);
    }

    public boolean isSystemIndex(String name, Table table) {
        return name.toUpperCase().startsWith("SYS_");
    }

    protected String getSequencesSQL(String schemaName, String sequenceName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT SEQUENCE_SCHEMA, SEQUENCE_NAME FROM ").
            append("INFORMATION_SCHEMA.SYSTEM_SEQUENCES");
        if (schemaName != null || sequenceName != null)
            buf.append(" WHERE ");
        if (schemaName != null) {
            buf.append("SEQUENCE_SCHEMA = ?");
            if (sequenceName != null)
                buf.append(" AND ");
        }
        if (sequenceName != null)
            buf.append("SEQUENCE_NAME = ?");
        return buf.toString();
    }

    protected SQLBuffer toOperation(String op, SQLBuffer selects, 
        SQLBuffer from, SQLBuffer where, SQLBuffer group, SQLBuffer having, 
        SQLBuffer order, boolean distinct, boolean forUpdate, long start, 
        long end) {
        // hsql requires ordering when limit is used
        if ((start != 0 || end != Long.MAX_VALUE)
            && (order == null || order.isEmpty()))
            order = _oneBuffer;
        return super.toOperation(op, selects, from, where, group, having,
            order, distinct, forUpdate, start, end);
    }

    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);

        for (int i = 0; cols != null && i < cols.length; i++)
            if ("BOOLEAN".equalsIgnoreCase(cols[i].getTypeName()))
                cols[i].setType(Types.BIT);
        return cols;
    }

    public void setLong(PreparedStatement stmnt, int idx, long val, Column col)
        throws SQLException {
        if (val == Long.MIN_VALUE) {
            val = Long.MIN_VALUE + 1;
            storageWarning(Numbers.valueOf(Long.MIN_VALUE),
                Numbers.valueOf(val));
        }
        super.setLong(stmnt, idx, val, col);
    }

    public void setBigDecimal(PreparedStatement stmnt, int idx, BigDecimal val,
        Column col)
        throws SQLException {
        // hsql can't compare a BigDecimal equal to any other type, so try
        // to set type based on column
        int type = (val == null || col == null) ? JavaTypes.BIGDECIMAL
            : col.getJavaType();
        switch (type) {
            case JavaTypes.DOUBLE:
            case JavaTypes.DOUBLE_OBJ:
                setDouble(stmnt, idx, val.doubleValue(), col);
                break;
            case JavaTypes.FLOAT:
            case JavaTypes.FLOAT_OBJ:
                setDouble(stmnt, idx, val.floatValue(), col);
                break;
            default:
                super.setBigDecimal(stmnt, idx, val, col);
        }
    }

    protected void appendSelectRange(SQLBuffer buf, long start, long end) {
        // HSQL doesn't parameters in range
        buf.append(" LIMIT ").append(String.valueOf(start)).append(" ");
        if (end == Long.MAX_VALUE)
            buf.append(String.valueOf(0));
        else
            buf.append(String.valueOf(end - start));
    }

    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
        FilterValue end) {
        buf.append(substringFunctionName).append("((");
        str.appendTo(buf);
        buf.append("), (");
        start.appendTo(buf);
        buf.append(" + 1)");
        if (end != null) {
            buf.append(", (");
            appendNumericCast(buf, end);
            buf.append(" - (");
            appendNumericCast(buf, start);
            buf.append("))");
        }
        buf.append(")");
    }

    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("(LOCATE(");
        find.appendTo(buf);
        buf.append(", ");
        str.appendTo(buf);
        if (start != null) {
            buf.append(", (");
            start.appendTo(buf);
            buf.append(" + 1)");
        }
        buf.append(") - 1)");
    }

    public String getPlaceholderValueString(Column col) {
        String type = getTypeName(col.getType());
        int idx = type.indexOf("{0}");
        if (idx != -1) {
            String pre = type.substring(0, idx);
            if (type.length() > idx + 3)
                type = pre + type.substring(idx + 3);
            else
                type = pre;
        }
        return "NULL AS " + type;
    }

    public OpenJPAException newStoreException(String msg, SQLException[] causes,
        Object failed) {
        OpenJPAException ke = super.newStoreException(msg, causes, failed);
        if (ke instanceof ReferentialIntegrityException
            && causes[0].getErrorCode() == -Trace.VIOLATION_OF_UNIQUE_INDEX) {
            ((ReferentialIntegrityException) ke).setIntegrityViolation
                (ReferentialIntegrityException.IV_UNIQUE);
        }
        return ke;
    }
}

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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Support for the H2 database ({@link http://www.h2database.com}).
 *
 * @since 0.9.7
 */
public class H2Dictionary extends DBDictionary {

    public H2Dictionary() {
        platform = "H2";
        validationSQL = "CALL 1";
        closePoolSQL = "SHUTDOWN";

        supportsAutoAssign = true;
        lastGeneratedKeyQuery = "CALL IDENTITY()";
        autoAssignClause = "IDENTITY";
        autoAssignTypeName = "INTEGER";
        nextSequenceQuery = "CALL NEXT VALUE FOR {0}";

        // CROSS JOIN is currently not supported
        crossJoinClause = "JOIN";
        requiresConditionForCrossJoin = true;
        stringLengthFunction = "LENGTH({0})";
        trimLeadingFunction = "LTRIM({0})";
        trimTrailingFunction = "RTRIM({0})";
        trimBothFunction = "TRIM({0})";

        useSchemaName = true;
        supportsSelectForUpdate = true;
        supportsSelectStartIndex = true;
        supportsSelectEndIndex = true;
        rangePosition = RANGE_POST_LOCK;
        supportsDeferredConstraints = false;

        blobTypeName = "BLOB";
        doubleTypeName = "DOUBLE";

        supportsNullTableForGetPrimaryKeys = true;
        supportsNullTableForGetIndexInfo = true;

        requiresCastForMathFunctions = false;
        requiresCastForComparisons = false;

        reservedWordSet.addAll(Arrays.asList(new String[] {
            "CURRENT_TIMESTAMP", "CURRENT_TIME", "CURRENT_DATE", "CROSS",
            "DISTINCT", "EXCEPT", "EXISTS", "FROM", "FOR", "FALSE", "FULL",
            "GROUP", "HAVING", "INNER", "INTERSECT", "IS", "JOIN", "LIKE",
            "MINUS", "NATURAL", "NOT", "NULL", "ON", "ORDER", "PRIMARY",
            "ROWNUM", "SELECT", "SYSDATE", "SYSTIME", "SYSTIMESTAMP", "TODAY",
            "TRUE", "UNION", "WHERE" 
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
        return super.getPreferredType(type);
    }

    public String[] getAddPrimaryKeySQL(PrimaryKey pk) {
        return new String[0];
    }

    public String[] getDropPrimaryKeySQL(PrimaryKey pk) {
        return new String[0];
    }

    public String[] getAddColumnSQL(Column column) {
        return new String[] { 
            "ALTER TABLE " + getFullName(column.getTable(), false) 
                + " ADD COLUMN " + getDeclareColumnSQL(column, true) 
        };
    }

    public String[] getCreateTableSQL(Table table) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(getFullName(table, false))
            .append(" (");

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
            if (!StringUtils.isEmpty(pkStr))
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
        return new String[] { buf.toString() };
    }

    protected String getPrimaryKeyConstraintSQL(PrimaryKey pk) {
        Column[] cols = pk.getColumns();
        if (cols.length == 1 && cols[0].isAutoAssigned())
            return null;
        return super.getPrimaryKeyConstraintSQL(pk);
    }

    public boolean isSystemIndex(String name, Table table) {
        return name.toUpperCase(Locale.ENGLISH).startsWith("SYSTEM_");
    }

    protected String getSequencesSQL(String schemaName, String sequenceName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT SEQUENCE_SCHEMA, SEQUENCE_NAME FROM ")
            .append("INFORMATION_SCHEMA.SEQUENCES");
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

    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName, 
            columnName, conn);
        return cols;
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
}

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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;

/**
 * Dictionary for Pointbase Embedded.
 */
public class PointbaseDictionary
    extends DBDictionary {

    public PointbaseDictionary() {
        platform = "Pointbase Embedded";
        supportsDeferredConstraints = false;
        supportsMultipleNontransactionalResultSets = false;
        requiresAliasForSubselect = true;

        supportsLockingWithDistinctClause = false;
        supportsLockingWithMultipleTables = false;
        supportsLockingWithDistinctClause = false;

        bitTypeName = "TINYINT";
        blobTypeName = "BLOB(1M)";
        longVarbinaryTypeName = "BLOB(1M)";
        charTypeName = "CHARACTER{0}";
        clobTypeName = "CLOB(1M)";
        doubleTypeName = "DOUBLE PRECISION";
        floatTypeName = "FLOAT";
        bigintTypeName = "BIGINT";
        integerTypeName = "INTEGER";
        realTypeName = "REAL";
        smallintTypeName = "SMALLINT";
        tinyintTypeName = "TINYINT";

        // there is no build-in function for getting the last generated
        // key in Pointbase; using MAX will have to suffice
        supportsAutoAssign = true;
        lastGeneratedKeyQuery = "SELECT MAX({0}) FROM {1}";
        autoAssignTypeName = "BIGINT IDENTITY";
    }

    public int getPreferredType(int type) {
        switch (type) {
            case Types.LONGVARCHAR:
                return Types.CLOB;
            default:
                return super.getPreferredType(type);
        }
    }

    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);

        // pointbase reports the type for a CLOB field as VARCHAR: override it
        for (int i = 0; cols != null && i < cols.length; i++)
            if (cols[i].getTypeName().toUpperCase().startsWith("CLOB"))
                cols[i].setType(Types.CLOB);
        return cols;
    }

    public String getFullName(Index index) {
        return getFullName(index.getTable(), false) + "." + index.getName();
    }

    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
        FilterValue end) {
        // SUBSTRING in Pointbase is of the form:
        // SELECT SUBSTRING(SOME_COLUMN FROM 1 FOR 5)
        buf.append("SUBSTRING(");
        str.appendTo(buf);
        buf.append(" FROM ");
        start.appendTo(buf);
        buf.append(" + 1");
        if (end != null) {
            buf.append(" FOR ");
            end.appendTo(buf);
            buf.append(" - (");
            start.appendTo(buf);
            buf.append(")");
        }
        buf.append(")");
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

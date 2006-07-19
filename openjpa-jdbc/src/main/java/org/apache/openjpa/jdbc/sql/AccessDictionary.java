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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;

/**
 * Dictionary for Access via DataDirect SequeLink and DataDirect ODBC
 * FoxPro driver. This will not work with any other combination of JDBC/ODBC
 * server and ODBC driver.
 */
public class AccessDictionary
    extends DBDictionary {

    public AccessDictionary() {
        platform = "Microsoft Access";
        joinSyntax = SYNTAX_TRADITIONAL;
        validationSQL = "SELECT 1";
        reservedWordSet.add("VALUE");

        supportsAutoAssign = true;
        autoAssignTypeName = "COUNTER";
        lastGeneratedKeyQuery = "SELECT @@identity";
        maxTableNameLength = 64;
        maxColumnNameLength = 64;
        maxIndexNameLength = 64;
        maxConstraintNameLength = 64;

        useGetBytesForBlobs = true;
        useGetBestRowIdentifierForPrimaryKeys = true;

        binaryTypeName = "LONGBINARY";
        blobTypeName = "LONGBINARY";
        longVarbinaryTypeName = "LONGBINARY";
        clobTypeName = "LONGCHAR";
        longVarcharTypeName = "LONGCHAR";
        bigintTypeName = "REAL";
        numericTypeName = "REAL";
        integerTypeName = "INTEGER";
        smallintTypeName = "SMALLINT";
        tinyintTypeName = "SMALLINT";

        supportsForeignKeys = false;
        supportsDeferredConstraints = false;
        maxIndexesPerTable = 32;
    }

    public void setLong(PreparedStatement stmnt, int idx, long val, Column col)
        throws SQLException {
        // the access driver disallows setLong for some reason; use
        // setInt if possible, otherwise use setDouble

        if (val < Integer.MAX_VALUE && val > Integer.MIN_VALUE)
            stmnt.setInt(idx, (int) val);
        else
            stmnt.setDouble(idx, val);
    }

    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
        FilterValue end) {
        buf.append("MID(");
        str.appendTo(buf);
        buf.append(", (");
        start.appendTo(buf);
        buf.append(" + 1)");
        if (end != null) {
            buf.append(", (");
            end.appendTo(buf);
            buf.append(" - ");
            start.appendTo(buf);
            buf.append(")");
        }
        buf.append(")");
    }
}


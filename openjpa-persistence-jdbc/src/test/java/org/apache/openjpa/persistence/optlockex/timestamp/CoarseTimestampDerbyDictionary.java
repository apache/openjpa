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
package org.apache.openjpa.persistence.optlockex.timestamp;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;

/**
 * A Derby dictionary whose metadata reports the timestamp columns of table
 * OJ3034_TSV with no fractional digits (and those of OJ3034_TSVS with one), as MySQL does for a
 * <code>TIMESTAMP</code> column created without a fractional seconds
 * precision. Counts the lookups of single columns of the test tables.
 */
public class CoarseTimestampDerbyDictionary extends DerbyDictionary {

    public static final AtomicInteger COLUMN_LOOKUPS = new AtomicInteger();
    public static final int HALF_SECOND = 500_000_000;

    public CoarseTimestampDerbyDictionary() {
        super();
        // embedded Derby reads metadata inside a transaction just fine
        requiresAutoCommitForMetaData = false;
    }

    @Override
    public Column[] getColumns(DatabaseMetaData meta, DBIdentifier catalog, DBIdentifier schemaName,
        DBIdentifier tableName, DBIdentifier columnName, Connection conn) throws SQLException {
        if (!DBIdentifier.isNull(columnName) && !DBIdentifier.isNull(tableName)
            && tableName.getName().toUpperCase().startsWith("OJ3034_TSV")) {
            COLUMN_LOOKUPS.incrementAndGet();
        }
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName, columnName, conn);
        for (int i = 0; cols != null && i < cols.length; i++) {
            if ("OJ3034_TSV".equalsIgnoreCase(cols[i].getTableIdentifier().getName())
                && cols[i].getType() == java.sql.Types.TIMESTAMP) {
                cols[i].setDecimalDigits(0);
            } else if ("OJ3034_TSVS".equalsIgnoreCase(cols[i].getTableIdentifier().getName())
                && cols[i].getType() == java.sql.Types.TIMESTAMP) {
                cols[i].setDecimalDigits(1);
            }
        }
        return cols;
    }

    /**
     * Like SQL Server's <code>DATETIME</code>, the granularity of a column
     * depends on its type, not only on its fractional digits: one digit of a
     * <code>TIMESTAMP</code> stands for half a second here.
     */
    @Override
    public int getTimestampPrecision(Column col) {
        if (col != null && col.getPrecision() == 1
            && "TIMESTAMP".equalsIgnoreCase(col.getTypeIdentifier().getName())) {
            return HALF_SECOND;
        }
        return super.getTimestampPrecision(col);
    }
}

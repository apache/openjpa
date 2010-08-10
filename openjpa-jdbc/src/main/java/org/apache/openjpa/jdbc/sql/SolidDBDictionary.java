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

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;

/**
 * Dictionary for SolidDB database.
 */
public class SolidDBDictionary
    extends DBDictionary {

    /**
     * Sets whether tables are to be located in-memory or on disk.
     * Creating in-memory tables should append "STORE MEMORY" to the 
     * "CREATE TABLE" statement. Creating disk-based tables should 
     * append "STORE DISK".
     */
    public boolean storeIsMemory = true;

    public SolidDBDictionary() {
        platform = "SolidDB";
        bitTypeName = "TINYINT";
        blobTypeName = "LONG VARBINARY";
        booleanTypeName = "TINYINT";
        clobTypeName = "LONG VARCHAR";
        doubleTypeName = "DOUBLE PRECISION";
        
        allowsAliasInBulkClause = false;
        useGetStringForClobs = true;
        useSetStringForClobs = true;
        
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "BIGINT", "BINARY", "DATE", "TIME", 
            "TINYINT", "VARBINARY"
        }));
    }

    @Override
    public String[] getCreateTableSQL(Table table) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ").append(getFullName(table, false)).append(" (");
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

        buf.append(") STORE ");
        if (storeIsMemory)
            buf.append("MEMORY");
        else
            buf.append("DISK");
        return new String[]{ buf.toString() };
    }

    public String convertSchemaCase(DBIdentifier objectName) {
        if (objectName != null && objectName.getName() == null)
            return "";
        return super.convertSchemaCase(objectName);
    }
}

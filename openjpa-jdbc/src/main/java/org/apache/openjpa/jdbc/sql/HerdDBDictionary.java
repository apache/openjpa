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

import java.sql.Types;

/**
 * Dictionary for HerdDB.
 */
public class HerdDBDictionary
    extends org.apache.openjpa.jdbc.sql.DBDictionary {

    private static final String DELIMITER_BACK_TICK = "`";

    public HerdDBDictionary() {
        platform = "HerdDB";
        databaseProductName = "HerdDB";
        supportsForeignKeys = false;
        supportsUniqueConstraints = false;
        supportsCascadeDeleteAction = false;
        schemaCase = SCHEMA_CASE_LOWER;
        delimitedCase = SCHEMA_CASE_LOWER;

        // make OpenJPA escape everything, because Apache Calcite has a lot of reserved words, like 'User', 'Value'...
        setDelimitIdentifiers(true);
        setSupportsDelimitedIdentifiers(true);
        setLeadingDelimiter(DELIMITER_BACK_TICK);
        setTrailingDelimiter(DELIMITER_BACK_TICK);
    }
}


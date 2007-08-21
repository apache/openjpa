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
package org.apache.openjpa.persistence.jdbc;

/////////////////////////////////////////////////////////
// NOTE: when adding a new type, make sure to update the
// table in PersistenceMappingParser
/////////////////////////////////////////////////////////

/**
 * Set of mapping tags used in JPA.
 *
 * @author Abe White
 * @nojavadoc
 */
enum MappingTag {

    ASSOC_OVERRIDE,
    ASSOC_OVERRIDES,
    ATTR_OVERRIDE,
    ATTR_OVERRIDES,
    COL,
    COLUMN_NAME,
    COLUMN_RESULT,
    DISCRIM_COL,
    DISCRIM_VAL,
    ENTITY_RESULT,
    ENUMERATED,
    FIELD_RESULT,
    GEN_ID_TABLE,
    INHERITANCE,
    JOIN_COL,
    JOIN_COLS,
    JOIN_TABLE,
    PK_JOIN_COL,
    PK_JOIN_COLS,
    SECONDARY_TABLE,
    SECONDARY_TABLES,
    SQL_RESULT_SET_MAPPING,
    SQL_RESULT_SET_MAPPINGS,
    TABLE,
    TABLE_GEN,
    TEMPORAL,
    // openjpa extensions
    CLASS_CRIT,
    COLS,
    CONTAINER_TABLE,
    DATASTORE_ID_COL,
    DISCRIM_STRAT,
    EAGER_FETCH_MODE,
    ELEM_CLASS_CRIT,
    ELEM_FK,
    ELEM_INDEX,
    ELEM_JOIN_COL,
    ELEM_JOIN_COLS,
    ELEM_NONPOLY,
    EMBEDDED_MAPPING,
    FK,
    INDEX,
    MAPPING_OVERRIDE,
    MAPPING_OVERRIDES,
    NONPOLY,
    ORDER_COL,
    STRAT,
    SUBCLASS_FETCH_MODE,
    UNIQUE,
    VERSION_COL,
    VERSION_COLS,
    VERSION_STRAT,
    X_JOIN_COL,
    X_JOIN_COLS,
    X_SECONDARY_TABLE,
    X_SECONDARY_TABLES,
    X_TABLE,
}

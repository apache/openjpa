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
package org.apache.openjpa.jdbc.identifier;

import org.apache.openjpa.jdbc.identifier.DBIdentifier.DBIdentifierType;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.NameSet;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.lib.identifier.IdentifierUtil;

/**
 * An interface for DB identifier utility-style operations.  This interface
 * extends the basic operations provided by IdentifierUtil with additional
 * operations those specific to DBIdentifiers and identifier conversion.
 */
public interface DBIdentifierUtil extends IdentifierUtil {

    // Legacy values for naming operations
    int ANY = 0;
    int TABLE = 1;
    int SEQUENCE = 2;
    int COLUMN = 3;

    /**
     * Shortens the given name to the given maximum length, then checks that
     * it is not a reserved word. If it is reserved, appends a "0". If
     * the name conflicts with an existing schema component and uniqueness
     * checking is enabled, the last character is replace with '0', then
     * '1', etc.
     * Note that the given max len may be 0 if the database metadata is
     * incomplete.
     *
     * Note: If the name is delimited, make sure the ending delimiter is
     * not stripped off.
     */
    DBIdentifier makeIdentifierValid(DBIdentifier sname, NameSet set, int maxLen,
        boolean checkForUniqueness);

    /**
     * Shortens the given name to the given maximum length, then checks that
     * it is not a reserved word. If it is reserved, appends a "0". If
     * the name conflicts with an existing schema component and uniqueness
     * checking is enabled, the last character is replace with '0', then
     * '1', etc.
     * Note that the given max len may be 0 if the database metadata is
     * incomplete.
     *
     * Note: If the name is delimited, make sure the ending delimiter is
     * not stripped off.
     */
    DBIdentifier makeNameValid(String name, NameSet set, int maxLen,
        int nameType, boolean checkForUniqueness);

    /**
     * Returns a valid column name/identifier, based upon the configuration and
     * provided parameters.
     * @param name
     * @param table
     * @param maxLen
     * @param checkForUniqueness
     */
    DBIdentifier getValidColumnIdentifier(DBIdentifier name, Table table, int maxLen,
        boolean checkForUniqueness);

    /**
     * Returns a valid index identifier, based upon the configuration and
     * provided parameters.
     * @param name
     * @param table
     * @param maxLen
     * @param checkForUniqueness
     */
    DBIdentifier getValidIndexIdentifier(DBIdentifier name, Table table, int maxLen);

    /**
     * Returns a valid index identifier, based upon the configuration and
     * provided parameters.
     * @param name
     * @param table
     * @param maxLen
     * @param checkForUniqueness
     */
    DBIdentifier getValidSequenceIdentifier(DBIdentifier name, Schema schema, int maxLen);

    /**
     * Returns a valid table identifier, based upon the configuration and provided
     * parameters.
     * @param name
     * @param schema
     * @param maxLen
     */
    DBIdentifier getValidTableIdentifier(DBIdentifier name, Schema schema, int maxLen);

    /**
     * Returns a valid unique constraint identifier, based upon the configuration and
     * provided parameters.
     * @param name
     * @param table
     * @param maxLen
     */
    DBIdentifier getValidUniqueIdentifier(DBIdentifier name, Table table, int maxLen);

    /**
     * Returns a valid foreign key identifier, based upon the configuration and
     * provided parameters.
     * @param name
     * @param table
     * @param toTable
     * @param maxLen
     */
    DBIdentifier getValidForeignKeyIdentifier(DBIdentifier name, Table table, Table toTable, int maxLen);

    /**
     * Converts the specified identifier to a format required by the database.
     * @param name
     */
    String toDBName(DBIdentifier name);

    /**
     * Converts the specified identifier to a format required by the database,
     * optionally delimiting the name.
     * @param name
     * @param delimit
     */
    String toDBName(DBIdentifier name, boolean delimit);

    /**
     * Converts the specified string to a format required by the database.
     * @param name
     */
    String toDBName(String name);

    /**
     * Converts the specified string to a format required by the database,
     * optionally delimiting the name.
     * @param name
     */
    String toDBName(String name, boolean delimit);

    /**
     * Converts the name returned by the database to an identifier of the
     * specified type.
     * @param name
     */
    DBIdentifier fromDBName(String name, DBIdentifierType id);

    /**
     * Appends multiple columns names together into comma delimited string.
     * @param columns
     */
    String appendColumns(Column[] columns);

    /**
     * Converts the name of the specified delimiter to the appropriate
     * case as defined by the configuration.
     * @param columns
     */
    DBIdentifier convertSchemaCase(DBIdentifier schema);

    /**
     * Appends multiple names together using the appropriate name delimiter.
     * @param resultId
     * @param names
     */
    DBIdentifier append(DBIdentifierType resultId, DBIdentifier...names);

    /**
     * Returns a generated key sequence identifier for the column.
     * @param col
     * @param maxLen
     */
    DBIdentifier getGeneratedKeySequenceName(Column col, int maxLen);

    /**
     * Converts a provided alias to a format specified in the configuration.
     * @param alias
     */
    String convertAlias(String alias);
}

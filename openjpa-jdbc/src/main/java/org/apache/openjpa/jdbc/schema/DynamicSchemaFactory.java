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
package org.apache.openjpa.jdbc.schema;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.QualifiedDBIdentifier;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;

/**
 * Factory whose schema group dynamically fills itself with information
 * as mappings validate themselves in their <code>map</code> method.
 * Tables are added when {@link SchemaGroup#findTable} is called. Columns
 * are added to tables when {@link Table#getColumn} is called. And the
 * column types are set when {@link Column#isCompatible} is called.
 *
 * @author Abe White
 */
public class DynamicSchemaFactory
    extends SchemaGroup
    implements SchemaFactory, Configurable {

    private static final long serialVersionUID = 1L;
    private transient DBDictionary _dict = null;
    private DBIdentifier _schema = DBIdentifier.NULL;

    @Override
    public void setConfiguration(Configuration conf) {
        JDBCConfiguration jconf = (JDBCConfiguration) conf;
        _dict = jconf.getDBDictionaryInstance();
        _schema = DBIdentifier.newSchema(jconf.getSchema());
    }

    @Override
    public void startConfiguration() {
    }

    @Override
    public void endConfiguration() {
    }

    @Override
    public SchemaGroup readSchema() {
        return this;
    }

    @Override
    public void storeSchema(SchemaGroup schema) {
        // nothing to do
    }

    @Override
    public boolean isKnownTable(Table table) {
        return super.findTable(table) != null;
    }

    @Override
    public boolean isKnownTable(String name) {
        return super.findTable(name) != null;
    }

    @Override
    public boolean isKnownTable(QualifiedDBIdentifier path) {
        return super.findTable(path) != null;
    }

    @Override
    public Table findTable(String name) {
        return super.findTable(name);
    }

    public Table findTable(DBIdentifier name) {
        if (name == null)
            return null;

        QualifiedDBIdentifier path = QualifiedDBIdentifier.getPath(name);
        return findTable(path);
    }

    @Override
    public Table findTable(QualifiedDBIdentifier path) {
        if (DBIdentifier.isNull(path))
            return null;

        Table table = super.findTable(path);
        if (table != null)
            return table;

        // if full name, split
        DBIdentifier schemaName = DBIdentifier.NULL;
        DBIdentifier tableName = path.getUnqualifiedName();
        if (!DBIdentifier.isNull(path.getSchemaName())) {
            schemaName = path.getSchemaName();
        } else {
            schemaName = _schema;
        }

        Schema schema = getSchema(schemaName);
        if (schema == null) {
            schema = addSchema(schemaName);
        }

        // Ensure only valid table name(s) are added to the schema
        if (tableName.getName().length() > _dict.maxTableNameLength) {
            return schema.addTable(tableName,
                _dict.getValidTableName(tableName, getSchema(schemaName)));
        }

        return schema.addTable(tableName);
    }


//    protected Table newTable(String name, Schema schema) {
//        return new DynamicTable(name, schema);
//    }

    @Override
    protected Table newTable(DBIdentifier name, Schema schema) {
        return new DynamicTable(name, schema);
    }

//    protected Column newColumn(String name, Table table) {
//        return new DynamicColumn(name, table);
//    }

    @Override
    protected Column newColumn(DBIdentifier name, Table table) {
        return new DynamicColumn(name, table);
    }

    /**
     * Table type that adds columns when {@link #getColumn} is called.
     */
    private class DynamicTable
        extends Table {

        
        private static final long serialVersionUID = 1L;

        public DynamicTable(String name, Schema schema) {
            super(name, schema);
        }

        public DynamicTable(DBIdentifier name, Schema schema) {
            super(name, schema);
        }

        /**
         * @deprecated
         */
        @Deprecated
        @Override
        public Column getColumn(String name) {
            return getColumn(name, null);
        }

        @Override
        public Column getColumn(DBIdentifier name, boolean create) {
            return getColumn(name, null, create);
        }

        @Override
        public Column getColumn(DBIdentifier name) {
            return getColumn(name, null);
        }

        /**
         * @deprecated
         */
        @Deprecated
        public Column getColumn(String name, DBDictionary dict) {
            if (name == null)
                return null;
            return getColumn(DBIdentifier.newColumn(name), dict);
        }

        public Column getColumn(DBIdentifier name, DBDictionary dict) {
            return getColumn(name, dict, true);
        }

        public Column getColumn(DBIdentifier name, DBDictionary dict, boolean create) {
            if (DBIdentifier.isNull(name))
                return null;

            Column col = super.getColumn(name);
            if (col != null || !create)
                return col;

            // Ensure only valid column name(s) are added to the table
            if ((name.getName().length() > _dict.maxColumnNameLength) ||
                _dict.getInvalidColumnWordSet().contains(
                    DBIdentifier.toUpper(name).getName())) {
                return addColumn(name,
                    _dict.getValidColumnName(name, this));
            }

            return addColumn(name);
        }
    }

    /**
     * Column type that sets its type when {@link #isCompatible} is called.
     */
    private class DynamicColumn
        extends Column {

        
        private static final long serialVersionUID = 1L;

        /**
         * @deprecated
         */
        @Deprecated
        public DynamicColumn(String name, Table table) {
            super(name, table);
        }

        public DynamicColumn(DBIdentifier name, Table table) {
            super(name, table);
        }

        @Override
        public boolean isCompatible(int type, String typeName, int size,
            int decimals) {
            if (getType() != Types.OTHER)
                return super.isCompatible(type, typeName, size, decimals);

            if (type == Types.VARCHAR && size <= 0)
                size = _dict.characterColumnSize;
            setType(type);
            setSize(size);
            if (typeName != null)
                setTypeIdentifier(DBIdentifier.newColumnDefinition(typeName));
            if (decimals >= 0)
                setDecimalDigits(decimals);
            return true;
        }
    }
}

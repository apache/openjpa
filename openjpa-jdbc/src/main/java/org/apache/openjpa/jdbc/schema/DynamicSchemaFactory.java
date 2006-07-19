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
package org.apache.openjpa.jdbc.schema;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
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

    private DBDictionary _dict = null;
    private String _schema = null;

    public void setConfiguration(Configuration conf) {
        JDBCConfiguration jconf = (JDBCConfiguration) conf;
        _dict = jconf.getDBDictionaryInstance();
        _schema = jconf.getSchema();
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }

    public SchemaGroup readSchema() {
        return this;
    }

    public void storeSchema(SchemaGroup schema) {
        // nothing to do
    }

    public boolean isKnownTable(Table table) {
        return super.findTable(table) != null;
    }

    public boolean isKnownTable(String name) {
        return super.findTable(name) != null;
    }

    public Table findTable(String name) {
        if (name == null)
            return null;

        Table table = super.findTable(name);
        if (table != null)
            return table;

        // if full name, split
        String schemaName = null;
        String tableName = name;
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx != -1) {
            schemaName = name.substring(0, dotIdx);
            tableName = name.substring(dotIdx + 1);
        } else
            schemaName = _schema;

        Schema schema = getSchema(schemaName);
        if (schema == null)
            schema = addSchema(schemaName);
        return schema.addTable(tableName);
    }

    protected Table newTable(String name, Schema schema) {
        return new DynamicTable(name, schema);
    }

    protected Column newColumn(String name, Table table) {
        return new DynamicColumn(name, table);
    }

    /**
     * Table type that adds columns when {@link #getColumn} is called.
     */
    private static class DynamicTable
        extends Table {

        public DynamicTable(String name, Schema schema) {
            super(name, schema);
        }

        public Column getColumn(String name) {
            if (name == null)
                return null;

            Column col = super.getColumn(name);
            if (col != null)
                return col;
            return addColumn(name);
        }
    }

    /**
     * Column type that sets its type when {@link #isCompatible} is called.
     */
    private class DynamicColumn
        extends Column {

        public DynamicColumn(String name, Table table) {
            super(name, table);
        }

        public boolean isCompatible(int type, int size) {
            if (getType() != Types.OTHER)
                return super.isCompatible(type, size);

            if (type == Types.VARCHAR && size <= 0)
                size = _dict.characterColumnSize;
            setType(type);
            setSize(size);
            return true;
        }
    }
}

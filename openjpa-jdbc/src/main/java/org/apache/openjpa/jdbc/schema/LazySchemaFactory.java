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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;

/**
 * Factory that uses database metadata to construct the system schema.
 * The lazy schema factory only loads table data as it is requested. It
 * does not properly support operations that require knowledge of the entire
 * schema.
 *
 * @author Abe White
 */
public class LazySchemaFactory
    extends SchemaGroup
    implements SchemaFactory, Configurable {

    private JDBCConfiguration _conf = null;
    private SchemaGenerator _gen = null;
    private Connection _conn = null;
    private DatabaseMetaData _meta = null;
    private boolean _indexes = false;
    private boolean _pks = false;
    private boolean _fks = false;

    public boolean getPrimaryKeys() {
        return _pks;
    }

    public void setPrimaryKeys(boolean pks) {
        _pks = pks;
    }

    public boolean getForeignKeys() {
        return _fks;
    }

    public void setForeignKeys(boolean fks) {
        _fks = fks;
    }

    public boolean getIndexes() {
        return _indexes;
    }

    public void setIndexes(boolean idx) {
        _indexes = idx;
    }

    public SchemaGroup readSchema() {
        return this;
    }

    public void storeSchema(SchemaGroup schema) {
        // nothing to do
    }

    public Table findTable(String name) {
        if (name == null)
            return null;

        Table table = super.findTable(name);
        if (table != null)
            return table;

        generateSchemaObject(name, true);
        return super.findTable(name);
    }

    public Sequence findSequence(String name) {
        if (name == null)
            return null;

        Sequence seq = super.findSequence(name);
        if (seq != null)
            return seq;

        generateSchemaObject(name, false);
        return super.findSequence(name);
    }

    /**
     * Generate the table or sequence with the given name.
     */
    private void generateSchemaObject(String name, boolean isTable) {
        // if full name, split
        String schemaName = null;
        String objectName = name;

        // look for the standard schema separator...
        int dotIdx = name.indexOf('.');
        // ... or the dictionary schema separator
        if (dotIdx == -1) {
            String sep = _conf.getDBDictionaryInstance().catalogSeparator;
            if (!".".equals(sep))
                dotIdx = name.indexOf(sep);
        }

        if (dotIdx != -1) {
            schemaName = name.substring(0, dotIdx);
            objectName = name.substring(dotIdx + 1);
        }

        // we share a single connection across all schemas, so synch
        // on the schema group
        synchronized (this) {
            boolean close = false;
            try {
                // use the existing connection if possible; this method
                // might be reentrant if generating the foreign keys for
                // this table (see below) requires loading additional
                // tables; in that case we don't want to spawn additional
                // connections
                if (_conn == null) {
                    _conn = _conf.getDataSource2(null).getConnection();
                    close = true;
                    _meta = _conn.getMetaData();
                }

                if (isTable) {
                    // generate the table from database metadata
                    _gen.generateTables(schemaName, objectName, _conn, _meta);
                    Table table = super.findTable(name);

                    if (table != null) {
                        if (_pks)
                            _gen.generatePrimaryKeys(table.getSchemaName(),
                                table.getName(), _conn, _meta);
                        if (_indexes)
                            _gen.generateIndexes(table.getSchemaName(),
                                table.getName(), _conn, _meta);

                        // generate foreign keys from the table; this might
                        // end up re-calling this getTable method if the foreign
                        // key links to a table that hasn't been loaded yet
                        if (_fks)
                            _gen.generateForeignKeys(table.getSchemaName(),
                                table.getName(), _conn, _meta);
                    }
                } else
                    _gen.generateSequences(schemaName, objectName, _conn,
                        _meta);
            } catch (SQLException se) {
                throw SQLExceptions.getStore(se,
                    _conf.getDBDictionaryInstance());
            } finally {
                if (close) {
                    try {
                        _conn.close();
                    } catch (SQLException se) {
                    }
                    _conn = null;
                }
            }
        }
    }

    public void setConfiguration(Configuration conf) {
        _conf = (JDBCConfiguration) conf;
        _gen = new SchemaGenerator(_conf);
        _gen.setSchemaGroup(this);
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }
}

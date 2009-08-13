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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;

/**
 * The SchemaGenerator creates {@link Schema}s matching the current
 * database. All schemas are added to the current {@link SchemaGroup}.
 *  Note that tables whose name starts with "OPENJPA_" will be not be added
 * to the database schema. This enables the creation of special tables
 * that will never be dropped by the {@link SchemaTool}.
 *
 * @author Abe White
 */
public class SchemaGenerator {

    private static final Localizer _loc = Localizer.forPackage
        (SchemaGenerator.class);

    private final DataSource _ds;
    private final DBDictionary _dict;
    private final Log _log;
    private final Object[][] _allowed;
    private boolean _indexes = true;
    private boolean _fks = true;
    private boolean _pks = true;
    private boolean _seqs = true;
    private boolean _openjpaTables = true;
    private SchemaGroup _group = null;

    private List _listeners = null;
    private int _schemaObjects = 0;

    /**
     * Constructor.
     *
     * @param conf configuration for connecting to the data store
     */
    public SchemaGenerator(JDBCConfiguration conf) {
        // note: we cannot assert developer capabilities in this tool like
        // we normally do because this class is also used at runtime

        _ds = conf.getDataSource2(null);
        _log = conf.getLog(JDBCConfiguration.LOG_SCHEMA);

        // cache this now so that if the conn pool only has 1 connection we
        // don't conflict later with the open databasemetadata connection
        _dict = conf.getDBDictionaryInstance();

        // create a table of allowed schema and tables to reflect on
        _allowed = parseSchemasList(conf.getSchemasList());
    }

    /**
     * Given a list of schema names and table names (where table names
     * are always of the form schema.table, or just .table if the schema is
     * unknown), creates a table mapping schema name to table list. Returns
     * null if no args are given. If no tables are given for a particular
     * schema, maps the schema name to null.
     */
    private static Object[][] parseSchemasList(String[] args) {
        if (args == null || args.length == 0)
            return null;

        Map schemas = new HashMap();
        String schema, table;
        int dotIdx;
        Collection tables;
        for (int i = 0; i < args.length; i++) {
            dotIdx = args[i].indexOf('.');
            if (dotIdx == -1) {
                schema = args[i];
                table = null;
            } else if (dotIdx == 0) {
                schema = null;
                table = args[i].substring(1);
            } else {
                schema = args[i].substring(0, dotIdx);
                table = args[i].substring(dotIdx + 1);
            }

            // if just a schema name, map schema to null
            if (table == null && !schemas.containsKey(schema))
                schemas.put(schema, null);
            else if (table != null) {
                tables = (Collection) schemas.get(schema);
                if (tables == null) {
                    tables = new LinkedList();
                    schemas.put(schema, tables);
                }
                tables.add(table);
            }
        }

        Object[][] parsed = new Object[schemas.size()][2];
        Map.Entry entry;
        int idx = 0;
        for (Iterator itr = schemas.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            tables = (Collection) entry.getValue();

            parsed[idx][0] = entry.getKey();
            if (tables != null)
                parsed[idx][1] = tables.toArray(new String[tables.size()]);
            idx++;
        }
        return parsed;
    }

    /**
     * Return whether indexes should be generated. Defaults to true.
     */
    public boolean getIndexes() {
        return _indexes;
    }

    /**
     * Set whether indexes should be generated. Defaults to true.
     */
    public void setIndexes(boolean indexes) {
        _indexes = indexes;
    }

    /**
     * Return whether foreign keys should be generated. Defaults to true.
     */
    public boolean getForeignKeys() {
        return _fks;
    }

    /**
     * Set whether foreign keys should be generated. Defaults to true.
     */
    public void setForeignKeys(boolean fks) {
        _fks = fks;
    }

    /**
     * Return whether primary keys should be generated. Defaults to true.
     */
    public boolean getPrimaryKeys() {
        return _pks;
    }

    /**
     * Set whether primary keys should be generated. Defaults to true.
     */
    public void setPrimaryKeys(boolean pks) {
        _pks = pks;
    }

    /**
     * Return whether sequences should be generated. Defaults to true.
     */
    public boolean getSequences() {
        return _seqs;
    }

    /**
     * Set whether sequences should be generated. Defaults to true.
     */
    public void setSequences(boolean seqs) {
        _seqs = seqs;
    }

    /**
     * Whether to generate info on special tables used by OpenJPA components
     * for bookkeeping. Defaults to true.
     */
    public boolean getOpenJPATables() {
        return _openjpaTables;
    }

    /**
     * Whether to generate info on special tables used by OpenJPA components
     * for bookkeeping. Defaults to true.
     */
    public void setOpenJPATables(boolean openjpaTables) {
        _openjpaTables = openjpaTables;
    }

    /**
     * Return the current schema group.
     */
    public SchemaGroup getSchemaGroup() {
        if (_group == null)
            _group = new SchemaGroup();
        return _group;
    }

    /**
     * Set the schema group to add generated schemas to.
     */
    public void setSchemaGroup(SchemaGroup group) {
        _group = group;
    }

    /**
     * Generate all schemas set in the configuration. This method also
     * calls {@link #generateIndexes}, {@link #generatePrimaryKeys}, and
     * {@link #generateForeignKeys} automatically.
     */
    public void generateSchemas()
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-schemas"));
        generateSchemas(null);
    }

    /**
     * Generate the schemas and/or tables named in the given strings.
     * This method calls {@link #generateIndexes},
     * {@link #generatePrimaryKeys}, and {@link #generateForeignKeys}
     * automatically.
     */
    public void generateSchemas(String[] schemasAndTables)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-schemas"));

        Object[][] schemaMap;
        if (schemasAndTables == null || schemasAndTables.length == 0)
            schemaMap = _allowed;
        else
            schemaMap = parseSchemasList(schemasAndTables);

        if (schemaMap == null) {
            generateSchema(null, null);

            // estimate the number of schema objects we will need to visit
            // in order to estimate progresss total for any listeners
            int numTables = getTables(null).size();
            _schemaObjects += numTables
                + (_pks ? numTables : 0)
                + (_indexes ? numTables : 0)
                + (_fks ? numTables : 0);

            if (_pks)
                generatePrimaryKeys(null, null);
            if (_indexes)
                generateIndexes(null, null);
            if (_fks)
                generateForeignKeys(null, null);
            return;
        }

        // generate all schemas and tables
        for (int i = 0; i < schemaMap.length; i++)
            generateSchema((String) schemaMap[i][0],
                (String[]) schemaMap[i][1]);

        // generate pks, indexes, fks
        String schemaName;
        String[] tableNames;
        for (int i = 0; i < schemaMap.length; i++) {
            schemaName = (String) schemaMap[i][0];
            tableNames = (String[]) schemaMap[i][1];

            // estimate the number of schema objects we will need to visit
            // in order to estimate progresss total for any listeners
            int numTables = (tableNames != null) ? tableNames.length
                : getTables(schemaName).size();
            _schemaObjects += numTables
                + (_pks ? numTables : 0)
                + (_indexes ? numTables : 0)
                + (_fks ? numTables : 0);

            if (_pks)
                generatePrimaryKeys(schemaName, tableNames);
            if (_indexes)
                generateIndexes(schemaName, tableNames);
            if (_fks)
                generateForeignKeys(schemaName, tableNames);
        }
    }

    /**
     * Add a fully-constructed {@link Schema} matching the given database
     * schema to the current group. No foreign keys are generated because
     * some keys might span schemas. You must call
     * {@link #generatePrimaryKeys}, {@link #generateIndexes}, and
     * {@link #generateForeignKeys} separately.
     *
     * @param name the database name of the schema, or null for all schemas
     * @param tableNames a list of tables to generate in the schema, or null
     * to generate all tables
     */
    public void generateSchema(String name, String[] tableNames)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-schema", name));

        // generate tables, including columns and primary keys
        Connection conn = _ds.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        try {
            if (tableNames == null)
                generateTables(name, null, conn, meta);
            else
                for (int i = 0; i < tableNames.length; i++)
                    generateTables(name, tableNames[i], conn, meta);

            if (_seqs)
                generateSequences(name, null, conn, meta);
        } finally {
            // some databases require a commit after metadata to release locks
            try {
                conn.commit();
            } catch (SQLException se) {
            }
            try {
                conn.close();
            } catch (SQLException se) {
            }
        }
    }

    /**
     * Generate primary key information for the given schema. This method
     * must be called in addition to {@link #generateSchema}. It should
     * only be called after all schemas are generated. The schema name and
     * tables array can be null to indicate that indexes should be generated
     * for all schemas and/or tables.
     */
    public void generatePrimaryKeys(String schemaName, String[] tableNames)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-all-primaries", schemaName));

        Connection conn = _ds.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        try {
            if (tableNames == null)
                generatePrimaryKeys(schemaName, null, conn, meta);
            else
                for (int i = 0; i < tableNames.length; i++)
                    generatePrimaryKeys(schemaName, tableNames[i], conn, meta);
        } finally {
            // some databases require a commit after metadata to release locks
            try {
                conn.commit();
            } catch (SQLException se) {
            }
            try {
                conn.close();
            } catch (SQLException se) {
            }
        }
    }

    /**
     * Generate index information for the given schema. This method
     * must be called in addition to {@link #generateSchema}. It should
     * only be called after all schemas are generated. The schema name and
     * tables array can be null to indicate that indexes should be generated
     * for all schemas and/or tables.
     */
    public void generateIndexes(String schemaName, String[] tableNames)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-all-indexes", schemaName));

        Connection conn = _ds.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        try {
            if (tableNames == null)
                generateIndexes(schemaName, null, conn, meta);
            else
                for (int i = 0; i < tableNames.length; i++)
                    generateIndexes(schemaName, tableNames[i], conn, meta);
        } finally {
            // some databases require a commit after metadata to release locks
            try {
                conn.commit();
            } catch (SQLException se) {
            }
            try {
                conn.close();
            } catch (SQLException se) {
            }
        }
    }

    /**
     * Generate foreign key information for the given schema. This method
     * must be called in addition to {@link #generateSchema}. It should
     * only be called after all schemas are generated. The schema name and
     * tables array can be null to indicate that indexes should be generated
     * for all schemas and/or tables.
     */
    public void generateForeignKeys(String schemaName, String[] tableNames)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-all-foreigns", schemaName));

        Connection conn = _ds.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        try {
            if (tableNames == null)
                generateForeignKeys(schemaName, null, conn, meta);
            else
                for (int i = 0; i < tableNames.length; i++)
                    generateForeignKeys(schemaName, tableNames[i], conn, meta);
        } finally {
            // some databases require a commit after metadata to release locks
            try {
                conn.commit();
            } catch (SQLException se) {
            }
            try {
                conn.close();
            } catch (SQLException se) {
            }
        }
    }

    /**
     * Adds all tables matching the given name pattern to the schema.
     */
    public void generateTables(String schemaName, String tableName,
        Connection conn, DatabaseMetaData meta)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-columns", schemaName,
            tableName));
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("gen-tables", schemaName, tableName));

        Column[] cols = _dict.getColumns(meta, conn.getCatalog(), schemaName,
            tableName, null, conn);

        // when we want to get all the columns for all tables, we need to build
        // a list of tables to verify because some databases (e.g., Postgres)
        // will include indexes in the list of columns, and there is no way to
        // distinguish the indexes from proper columns
        Set tableNames = null;
        if (tableName == null || "%".equals(tableName)) {
            Table[] tables = _dict.getTables(meta, conn.getCatalog(),
                schemaName, tableName, conn);
            tableNames = new HashSet();
            for (int i = 0; tables != null && i < tables.length; i++) {
                if (cols == null)
                    tableNames.add(tables[i].getName());
                else
                    tableNames.add(tables[i].getName().toUpperCase());
            }
        }

        // if database can't handle null table name, recurse on each known name
        if (cols == null && tableName == null) {
            for (Iterator itr = tableNames.iterator(); itr.hasNext();)
                generateTables(schemaName, (String) itr.next(), conn, meta);
            return;
        }

        SchemaGroup group = getSchemaGroup();
        Schema schema;
        Table table;
        String tableSchema;
        for (int i = 0; cols != null && i < cols.length; i++) {
            tableName = cols[i].getTableName();
            tableSchema = StringUtils.trimToNull(cols[i].getSchemaName());

            // ignore special tables
            if (!_openjpaTables &&
                (tableName.toUpperCase().startsWith("OPENJPA_")
                    || tableName.toUpperCase().startsWith("JDO_"))) // legacy
                continue;
            if (_dict.isSystemTable(tableName, tableSchema, schemaName != null))
                continue;

            // ignore tables not in list, or not allowed by schemas property
            if (tableNames != null
                && !tableNames.contains(tableName.toUpperCase()))
                continue;
            if (!isAllowedTable(tableSchema, tableName))
                continue;

            schema = group.getSchema(tableSchema);
            if (schema == null)
                schema = group.addSchema(tableSchema);

            table = schema.getTable(tableName);
            if (table == null) {
                table = schema.addTable(tableName);
                if (_log.isTraceEnabled())
                    _log.trace(_loc.get("col-table", table));
            }

            if (_log.isTraceEnabled())
                _log.trace(_loc.get("gen-column", cols[i].getName(), table));

            if (table.getColumn(cols[i].getName()) == null)
                table.importColumn(cols[i]);
        }
    }

    /**
     * Return whether the given table is allowed by the user's schema list.
     */
    private boolean isAllowedTable(String schema, String table) {
        if (_allowed == null)
            return true;

        // do case-insensitive comparison on allowed table and schema names
        String[] tables;
        String[] anySchemaTables = null;
        for (int i = 0; i < _allowed.length; i++) {
            if (_allowed[i][0] == null) {
                anySchemaTables = (String[]) _allowed[i][1];
                if (schema == null)
                    break;
                continue;
            }
            if (!StringUtils.equalsIgnoreCase(schema, (String) _allowed[i][0]))
                continue;

            if (table == null)
                return true;
            tables = (String[]) _allowed[i][1];
            if (tables == null)
                return true;
            for (int j = 0; j < tables.length; j++)
                if (StringUtils.equalsIgnoreCase(table, tables[j]))
                    return true;
        }

        if (anySchemaTables != null) {
            if (table == null)
                return true;
            for (int i = 0; i < anySchemaTables.length; i++)
                if (StringUtils.equalsIgnoreCase(table, anySchemaTables[i]))
                    return true;
        }
        return false;
    }

    /**
     * Generates table primary keys.
     */
    public void generatePrimaryKeys(String schemaName, String tableName,
        Connection conn, DatabaseMetaData meta)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-primary",
            schemaName, tableName));
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("gen-pks", schemaName, tableName));

        // if looking for a non-existant table, just return
        SchemaGroup group = getSchemaGroup();
        if (tableName != null && group.findTable(tableName) == null)
            return;

        // if the database can't use a table name wildcard, recurse on each
        // concrete table in the requested schema(s)
        PrimaryKey[] pks = _dict.getPrimaryKeys(meta, conn.getCatalog(),
            schemaName, tableName, conn);
        Table table;
        if (pks == null && tableName == null) {
            Collection tables = getTables(schemaName);
            for (Iterator itr = tables.iterator(); itr.hasNext();) {
                table = (Table) itr.next();
                generatePrimaryKeys(table.getSchemaName(),
                    table.getName(), conn, meta);
            }
            return;
        }

        Schema schema;
        PrimaryKey pk;
        String name;
        String colName;
        for (int i = 0; pks != null && i < pks.length; i++) {
            schemaName = StringUtils.trimToNull(pks[i].getSchemaName());
            schema = group.getSchema(schemaName);
            if (schema == null)
                continue;
            table = schema.getTable(pks[i].getTableName());
            if (table == null)
                continue;

            colName = pks[i].getColumnName();
            name = pks[i].getName();
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("gen-pk", name, table, colName));

            pk = table.getPrimaryKey();
            if (pk == null)
                pk = table.addPrimaryKey(name);
            pk.addColumn(table.getColumn(colName));
        }
    }

    /**
     * Generates table indexes.
     */
    public void generateIndexes(String schemaName, String tableName,
        Connection conn, DatabaseMetaData meta)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-indexes",
            schemaName, tableName));
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("gen-indexes", schemaName, tableName));

        // if looking for a non-existant table, just return
        SchemaGroup group = getSchemaGroup();
        if (tableName != null && group.findTable(tableName) == null)
            return;

        // if the database can't use a table name wildcard, recurse on each
        // concrete table in the requested schema(s)
        Index[] idxs = _dict.getIndexInfo(meta, conn.getCatalog(),
            schemaName, tableName, false, true, conn);
        Table table;
        if (idxs == null && tableName == null) {
            Collection tables = getTables(schemaName);
            for (Iterator itr = tables.iterator(); itr.hasNext();) {
                table = (Table) itr.next();
                generateIndexes(table.getSchemaName(),
                    table.getName(), conn, meta);
            }
            return;
        }

        Schema schema;
        Index idx;
        String name;
        String colName;
        String pkName;
        for (int i = 0; idxs != null && i < idxs.length; i++) {
            schemaName = StringUtils.trimToNull(idxs[i].getSchemaName());
            schema = group.getSchema(schemaName);
            if (schema == null)
                continue;
            table = schema.getTable(idxs[i].getTableName());
            if (table == null)
                continue;

            if (table.getPrimaryKey() != null)
                pkName = table.getPrimaryKey().getName();
            else
                pkName = null;

            // statistics don't have names; skip them
            name = idxs[i].getName();
            if (StringUtils.isEmpty(name)
                || (pkName != null && name.equalsIgnoreCase(pkName))
                || _dict.isSystemIndex(name, table))
                continue;

            colName = idxs[i].getColumnName();
            if (table.getColumn(colName) == null)
                continue;

            if (_log.isTraceEnabled())
                _log.trace(_loc.get("gen-index", name, table, colName));

            // same index may have multiple rows for multiple cols
            idx = table.getIndex(name);
            if (idx == null) {
                idx = table.addIndex(name);
                idx.setUnique(idxs[i].isUnique());
            }
            idx.addColumn(table.getColumn(colName));
        }
    }

    /**
     * Generates table foreign keys.
     */
    public void generateForeignKeys(String schemaName, String tableName,
        Connection conn, DatabaseMetaData meta)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-foreign",
            schemaName, tableName));
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("gen-fks", schemaName, tableName));

        // if looking for a non-existant table, just return
        SchemaGroup group = getSchemaGroup();
        if (tableName != null && group.findTable(tableName) == null)
            return;

        // if the database can't use a table name wildcard, recurse on each
        // concrete table in the requested schema(s)
        ForeignKey[] fks = _dict.getImportedKeys(meta, conn.getCatalog(),
            schemaName, tableName, conn);
        Table table;
        if (fks == null && tableName == null) {
            Collection tables = getTables(schemaName);
            for (Iterator itr = tables.iterator(); itr.hasNext();) {
                table = (Table) itr.next();
                generateForeignKeys(table.getSchemaName(),
                    table.getName(), conn, meta);
            }
            return;
        }

        Schema schema;
        Table pkTable;
        ForeignKey fk;
        String name;
        String pkSchemaName;
        String pkTableName;
        String pkColName;
        String fkColName;
        int seq;
        boolean seqWas0 = false; // some drivers incorrectly start at 0
        Collection invalids = null;
        for (int i = 0; fks != null && i < fks.length; i++) {
            schemaName = StringUtils.trimToNull(fks[i].getSchemaName());
            schema = group.getSchema(schemaName);
            if (schema == null)
                continue;
            table = schema.getTable(fks[i].getTableName());
            if (table == null)
                continue;

            name = fks[i].getName();
            fkColName = fks[i].getColumnName();
            pkColName = fks[i].getPrimaryKeyColumnName();
            seq = fks[i].getKeySequence();
            if (seq == 0)
                seqWas0 = true;
            if (seqWas0)
                seq++;

            // find pk table
            pkSchemaName = fks[i].getPrimaryKeySchemaName();
            if(_dict.getTrimSchemaName()) {
                pkSchemaName= StringUtils.trimToNull(pkSchemaName);
            }
            pkTableName = fks[i].getPrimaryKeyTableName();
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("gen-fk", new Object[]{ name, table,
                    fkColName, pkTableName, pkColName, seq + "" }));

            if (!StringUtils.isEmpty(pkSchemaName))
                pkTableName = pkSchemaName + "." + pkTableName;
            pkTable = group.findTable(pkTableName);
            if (pkTable == null)
                throw new SQLException(_loc.get("gen-nofktable",
                    table, pkTableName).getMessage());

            // this sucks, because it is *not* guaranteed to work;
            // the fk resultset is ordered by primary key table, then
            // sequence number within the foreign key (some drivers don't
            // use sequence numbers correctly either); since fk names
            // are allowed to be null, all this adds up to the fact
            // that there is absolutely no way to distinguish between
            // multiple multi-column fks to the same table; we're going
            // to rely on fk name here and hope it works
            fk = table.getForeignKey(name);

            // start a new fk?
            if (seq == 1 || fk == null) {
                fk = table.addForeignKey(name);
                fk.setDeferred(fks[i].isDeferred());
                fk.setDeleteAction(fks[i].getDeleteAction());
            }

            if (invalids == null || !invalids.contains(fk)) {
                try {
                    fk.join(table.getColumn(fkColName),
                        pkTable.getColumn(pkColName));
                } catch (IllegalArgumentException iae) {
                    if (_log.isWarnEnabled())
                        _log.warn(_loc.get("bad-join", iae.toString()));
                    if (invalids == null)
                        invalids = new HashSet();
                    invalids.add(fk);
                }
            }
        }

        // remove invalid fks
        if (invalids != null) {
            for (Iterator itr = invalids.iterator(); itr.hasNext();) {
                fk = (ForeignKey) itr.next();
                fk.getTable().removeForeignKey(fk);
            }
        }
    }

    /**
     * Adds all sequences matching the given name pattern to the schema.
     */
    public void generateSequences(String schemaName, String sequenceName,
        Connection conn, DatabaseMetaData meta)
        throws SQLException {
        fireGenerationEvent(_loc.get("generating-sequences", schemaName));
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("gen-seqs", schemaName, sequenceName));

        // since all the sequences are generated under the default schema
        // therefore, we can use the null schemaname to search
        Sequence[] seqs = _dict.getSequences(meta, conn.getCatalog(),
            null, sequenceName, conn);

        SchemaGroup group = getSchemaGroup();
        Schema schema;
        String sequenceSchema;
        for (int i = 0; seqs != null && i < seqs.length; i++) {
            sequenceName = seqs[i].getName();
            sequenceSchema = StringUtils.trimToNull(seqs[i].getSchemaName());

            // ignore special tables
            if (!_openjpaTables &&
                (sequenceName.toUpperCase().startsWith("OPENJPA_")
                    || sequenceName.toUpperCase().startsWith("JDO_"))) // legacy
                continue;
            if (_dict.isSystemSequence(sequenceName, sequenceSchema,
                schemaName != null))
                continue;
            if (!isAllowedTable(sequenceSchema, null))
                continue;

            schema = group.getSchema(sequenceSchema);
            if (schema == null)
                schema = group.addSchema(sequenceSchema);
            if (schema.getSequence(sequenceName) == null)
                schema.addSequence(sequenceName);
        }
    }

    /**
     * Notify any listeners that a schema object was generated. Returns
     * true if generation should continue.
     */
    private void fireGenerationEvent(Object schemaObject)
        throws SQLException {
        if (schemaObject == null)
            return;
        if (_listeners == null || _listeners.size() == 0)
            return;

        Event e = new Event(schemaObject, _schemaObjects);
        for (Iterator i = _listeners.iterator(); i.hasNext();) {
            Listener l = (Listener) i.next();
            if (!l.schemaObjectGenerated(e))
                throw new SQLException(_loc.get("refresh-cancelled")
                    .getMessage());
        }
    }

    /**
     * Adds a listener for schema generation events.
     *
     * @param l the listener to add
     */
    public void addListener(Listener l) {
        if (_listeners == null)
            _listeners = new LinkedList();
        _listeners.add(l);
    }

    /**
     * Removes a schema generation listener for schema events.
     *
     * @param l the listener to remove
     * @return true if it was successfully removed
     */
    public boolean removeListener(Listener l) {
        return _listeners != null && _listeners.remove(l);
    }

    /**
     * Return all tables for the given schema name, or all tables in
     * the schema group if null is given.
     */
    private Collection getTables(String schemaName) {
        SchemaGroup group = getSchemaGroup();
        if (schemaName != null) {
            Schema schema = group.getSchema(schemaName);
            if (schema == null)
                return Collections.EMPTY_LIST;
            return Arrays.asList(schema.getTables());
        }

        Schema[] schemas = group.getSchemas();
        Collection tables = new LinkedList();
        for (int i = 0; i < schemas.length; i++)
            tables.addAll(Arrays.asList(schemas[i].getTables()));
        return tables;
    }

    /**
     * A listener for a potentially lengthy schema generation process.
     */
    public static interface Listener {

        boolean schemaObjectGenerated(Event e);
    }

    /**
     * An event corresponding to the generation of a schema object.
     */
    public class Event
        extends EventObject {

        private final int _total;

        public Event(Object ob, int total) {
            super(ob);
            _total = total;
        }

        public int getTotal() {
            return _total;
        }
    }
}

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
package org.apache.openjpa.jdbc.meta;

import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Localizer.Message;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataContext;
import org.apache.openjpa.util.MetaDataException;
import serp.util.Strings;

/**
 * Base class storing raw mapping information; defines utility methods for
 * converting raw mapping information to full mapping to the schema.
 *
 * @author Abe White
 */
public abstract class MappingInfo
    implements Serializable {

    public static final int JOIN_NONE = 0;
    public static final int JOIN_FORWARD = 1;
    public static final int JOIN_INVERSE = 2;

    private static final Object NULL = new Object();

    private static final Localizer _loc = Localizer.forPackage
        (MappingInfo.class);

    private String _strategy = null;
    private List _cols = null;
    private Index _idx = null;
    private Unique _unq = null;
    private ForeignKey _fk = null;
    private boolean _canIdx = true;
    private boolean _canUnq = true;
    private boolean _canFK = true;
    private int _join = JOIN_NONE;
    private ColumnIO _io = null;

    /**
     * Mapping strategy name.
     */
    public String getStrategy() {
        return _strategy;
    }

    /**
     * Mapping strategy name.
     */
    public void setStrategy(String strategy) {
        _strategy = strategy;
    }

    /**
     * Raw column data.
     */
    public List getColumns() {
        return (_cols == null) ? Collections.EMPTY_LIST : _cols;
    }

    /**
     * Raw column data.
     */
    public void setColumns(List cols) {
        _cols = cols;
    }

    /**
     * Raw index.
     */
    public Index getIndex() {
        return _idx;
    }

    /**
     * Raw index.
     */
    public void setIndex(Index idx) {
        _idx = idx;
    }

    /**
     * The user can mark columns as explicitly non-indexable.
     */
    public boolean canIndex() {
        return _canIdx;
    }

    /**
     * The user can mark columns as explicitly non-indexable.
     */
    public void setCanIndex(boolean indexable) {
        _canIdx = indexable;
    }

    /**
     * Raw foreign key information.
     */
    public ForeignKey getForeignKey() {
        return _fk;
    }

    /**
     * Raw foreign key information.
     */
    public void setForeignKey(ForeignKey fk) {
        _fk = fk;
        if (fk != null && _join == JOIN_NONE)
            _join = JOIN_FORWARD;
    }

    /**
     * The user can mark columns as explicitly not having a foreign key.
     */
    public boolean canForeignKey() {
        return _canFK;
    }

    /**
     * The user can mark columns as explicitly not having a foreign key.
     */
    public void setCanForeignKey(boolean fkable) {
        _canFK = fkable;
    }

    /**
     * Raw unique constraint information.
     */
    public Unique getUnique() {
        return _unq;
    }

    /**
     * Raw unique constraint information.
     */
    public void setUnique(Unique unq) {
        _unq = unq;
    }

    /**
     * The user can mark columns as explicitly not having a unique constraint.
     */
    public boolean canUnique() {
        return _canUnq;
    }

    /**
     * The user can mark columns as explicitly not having a unique constraint.
     */
    public void setCanUnique(boolean uniquable) {
        _canUnq = uniquable;
    }

    /**
     * I/O for the columns created by the last call to {@link #createColumns},
     * or for the foreign key created by the last call to
     * {@link #createForeignKey}. This is also expected to be set correctly
     * prior to calls to {@link #syncColumns} and {@link #syncForeignKey}.
     */
    public ColumnIO getColumnIO() {
        return _io;
    }

    /**
     * I/O for the columns created by the last call to {@link #createColumns},
     * or for the foreign key created by the last call to
     * {@link #createForeignKey}. This is also expected to be set correctly
     * prior to calls to {@link #syncColumns} and {@link #syncForeignKey}.
     */
    public void setColumnIO(ColumnIO io) {
        _io = io;
    }

    /**
     * Direction of the join that the columns of this mapping info form. This
     * is usually automatically set by {@link #createForeignKey}. This flag
     * is also expected to be set correctly prior to calls to
     * {@link #syncForeignKey} if the join is inversed.
     */
    public int getJoinDirection() {
        return _join;
    }

    /**
     * Direction of the join that the columns of this mapping info form. This
     * is usually automatically set by {@link #createForeignKey}. This flag
     * is also expected to be set correctly prior to calls to
     * {@link #syncForeignKey} if the join is inversed.
     */
    public void setJoinDirection(int join) {
        _join = join;
    }

    /**
     * Clear all mapping information.
     */
    public void clear() {
        clear(true);
    }

    /**
     * Clear mapping information.
     *
     * @param canFlags whether to clear information about whether we
     * can place indexed, foreign keys, etc on this mapping
     */
    protected void clear(boolean canFlags) {
        _strategy = null;
        _cols = null;
        _io = null;
        _idx = null;
        _unq = null;
        _fk = null;
        _join = JOIN_NONE;
        if (canFlags) {
            _canIdx = true;
            _canFK = true;
            _canUnq = true;
        }
    }

    /**
     * Copy missing info from the instance to this one.
     */
    public void copy(MappingInfo info) {
        if (_strategy == null)
            _strategy = info.getStrategy();
        if (_canIdx && _idx == null) {
            if (info.getIndex() != null)
                _idx = info.getIndex();
            else
                _canIdx = info.canIndex();
        }
        if (_canUnq && _unq == null) {
            if (info.getUnique() != null)
                _unq = info.getUnique();
            else
                _canUnq = info.canUnique();
        }
        if (_canFK && _fk == null) {
            if (info.getForeignKey() != null)
                _fk = info.getForeignKey();
            else
                _canFK = info.canForeignKey();
        }

        List cols = getColumns();
        List icols = info.getColumns();
        if (!icols.isEmpty() && (cols.isEmpty()
            || cols.size() == icols.size())) {
            if (cols.isEmpty())
                cols = new ArrayList(icols.size());
            for (int i = 0; i < icols.size(); i++) {
                if (cols.size() == i)
                    cols.add(new Column());
                ((Column) cols.get(i)).copy((Column) icols.get(i));
            }
            setColumns(cols);
        }
    }

    /**
     * Return true if this info has columns, foreign key information, index
     * information, etc.
     */
    public boolean hasSchemaComponents() {
        return (_cols != null && !_cols.isEmpty())
            || _idx != null
            || _unq != null
            || _fk != null
            || !_canIdx
            || !_canFK
            || !_canUnq;
    }

    /**
     * Assert that the user did not supply any columns, index, unique
     * constraint, or foreign key for this mapping.
     */
    public void assertNoSchemaComponents(MetaDataContext context, boolean die) {
        if (_cols == null || _cols.isEmpty()) {
            assertNoIndex(context, die);
            assertNoUnique(context, die);
            assertNoForeignKey(context, die);
            return;
        }

        Message msg = _loc.get("unexpected-cols", context);
        if (die)
            throw new MetaDataException(msg);
        context.getRepository().getLog().warn(msg);
    }

    /**
     * Assert that this info has the given strategy or no strategy.
     */
    public void assertStrategy(MetaDataContext context, Object contextStrat,
        Object expected, boolean die) {
        if (contextStrat == expected)
            return;

        String strat;
        if (contextStrat == null) {
            if (_strategy == null)
                return;
            if (_strategy.equals(expected.getClass().getName()))
                return;
            if (expected instanceof Strategy
                && _strategy.equals(((Strategy) expected).getAlias()))
                return;
            strat = _strategy;
        } else if (contextStrat instanceof Strategy)
            strat = ((Strategy) contextStrat).getAlias();
        else
            strat = contextStrat.getClass().getName();

        Message msg = _loc.get("unexpected-strategy", context, expected,
            strat);
        if (die)
            throw new MetaDataException(msg);
        context.getRepository().getLog().warn(msg);
    }

    /**
     * Assert that the user did not try to place an index on this mapping.
     */
    public void assertNoIndex(MetaDataContext context, boolean die) {
        if (_idx == null)
            return;

        Message msg = _loc.get("unexpected-index", context);
        if (die)
            throw new MetaDataException(msg);
        context.getRepository().getLog().warn(msg);
    }

    /**
     * Assert that the user did not try to place a unique constraint on this
     * mapping.
     */
    public void assertNoUnique(MetaDataContext context, boolean die) {
        if (_unq == null)
            return;

        Message msg = _loc.get("unexpected-unique", context);
        if (die)
            throw new MetaDataException(msg);
        context.getRepository().getLog().warn(msg);
    }

    /**
     * Assert that the user did not try to place a foreign key on this mapping.
     */
    public void assertNoForeignKey(MetaDataContext context, boolean die) {
        if (_fk == null)
            return;

        Message msg = _loc.get("unexpected-fk", context);
        if (die)
            throw new MetaDataException(msg);
        context.getRepository().getLog().warn(msg);
    }

    /**
     * Assert that the user did not try to join.
     */
    public void assertNoJoin(MetaDataContext context, boolean die) {
        boolean join = false;
        if (_cols != null) {
            Column col;
            for (int i = 0; !join && i < _cols.size(); i++) {
                col = (Column) _cols.get(i);
                if (col.getTarget() != null)
                    join = true;
            }
        }
        if (!join)
            return;

        Message msg = _loc.get("unexpected-join", context);
        if (die)
            throw new MetaDataException(msg);
        context.getRepository().getLog().warn(msg);
    }

    /**
     * Find or generate a table for a mapping.
     *
     * @param context the mapping that uses the table
     * @param def default table name provider
     * @param schemaName default schema if known, or null
     * @param given given table name
     * @param adapt whether we can alter the schema or mappings
     */
    public Table createTable(MetaDataContext context, TableDefaults def,
        String schemaName, String given, boolean adapt) {
        MappingRepository repos = (MappingRepository) context.getRepository();
        if (given == null && (def == null || (!adapt
            && !repos.getMappingDefaults().defaultMissingInfo())))
            throw new MetaDataException(_loc.get("no-table", context));

        if (schemaName == null)
            schemaName = Schemas.getNewTableSchema((JDBCConfiguration)
                repos.getConfiguration());

        // if no given and adapting or defaulting missing info, use template
        SchemaGroup group = repos.getSchemaGroup();
        Schema schema = null;
        if (given == null) {
            schema = group.getSchema(schemaName);
            if (schema == null)
                schema = group.addSchema(schemaName);
            given = def.get(schema);
        }

        String fullName;
        int dotIdx = given.lastIndexOf('.');
        if (dotIdx == -1)
            fullName = (schemaName == null) ? given : schemaName + "." + given;
        else {
            fullName = given;
            schema = null;
            schemaName = given.substring(0, dotIdx);
            given = given.substring(dotIdx + 1);
        }

        // look for named table using full name and findTable, which allows
        // the dynamic schema factory to create the table if needed
        Table table = group.findTable(fullName);
        if (table != null)
            return table;
        if (!adapt)
            throw new MetaDataException(_loc.get("bad-table", given, context));

        // named table doesn't exist; create it
        if (schema == null) {
            schema = group.getSchema(schemaName);
            if (schema == null)
                schema = group.addSchema(schemaName);
        }
        table = schema.getTable(given);
        if (table == null)
            table = schema.addTable(given);
        return table;
    }

    /**
     * Retrieve/create columns on the given table by merging the given
     * template information with any user-provided information.
     *
     * @param context the mapping we're retrieving columns for
     * @param prefix localized error message key prefix
     * @param tmplates template columns
     * @param table the table for the columns
     * @param adapt whether we can modify the existing mapping or schema
     */
    protected Column[] createColumns(MetaDataContext context, String prefix,
        Column[] tmplates, Table table, boolean adapt) {
        assertTable(context, table);
        if (prefix == null)
            prefix = "generic";

        // the user has to give the right number of expected columns for this
        // mapping, or none at all if we're adapting.  can't just given one of
        // n columns because we don't know which of the n columns the info
        // applies to
        List given = getColumns();
        boolean fill = ((MappingRepository) context.getRepository()).
            getMappingDefaults().defaultMissingInfo();
        if ((!given.isEmpty() || (!adapt && !fill))
            && given.size() != tmplates.length)
            throw new MetaDataException(_loc.get(prefix + "-num-cols",
                context, String.valueOf(tmplates.length),
                String.valueOf(given.size())));

        Column[] cols = new Column[tmplates.length];
        _io = null;
        Column col;
        for (int i = 0; i < tmplates.length; i++) {
            col = (given.isEmpty()) ? null : (Column) given.get(i);
            cols[i] = mergeColumn(context, prefix, tmplates[i], true, col,
                table, adapt, fill);
            setIOFromColumnFlags(col, i);
        }
        return cols;
    }

    /**
     * Set the proper internal column I/O metadata for the given column's flags.
     */
    private void setIOFromColumnFlags(Column col, int i) {
        if (col == null || (!col.getFlag(Column.FLAG_UNINSERTABLE)
            && !col.getFlag(Column.FLAG_UNUPDATABLE)))
            return;

        if (_io == null)
            _io = new ColumnIO();
        _io.setInsertable(i, !col.getFlag(Column.FLAG_UNINSERTABLE));
        _io.setUpdatable(i, !col.getFlag(Column.FLAG_UNUPDATABLE));
    }

    /**
     * Assert that the given table is non-null.
     */
    private static void assertTable(MetaDataContext context, Table table) {
        if (table == null)
            throw new MetaDataException(_loc.get("unmapped", context));
    }

    /**
     * Merge the given columns if possible.
     *
     * @param context the mapping we're retrieving columns for
     * @param prefix localized error message key prefix
     * @param tmplate template for expected column information
     * @param compat whether the existing column type must be compatible
     * with the type of the template column
     * @param given the given column information from mapping info
     * @param table the table for the columns
     * @param adapt whether we can modify the existing mapping or schema
     * @param fill whether to default missing column information
     */
    protected static Column mergeColumn(MetaDataContext context, String prefix,
        Column tmplate, boolean compat, Column given, Table table,
        boolean adapt, boolean fill) {
        assertTable(context, table);

        // if not adapting must provide column name at a minimum
        String colName = (given == null) ? null : given.getName();
        if (colName == null && !adapt && !fill)
            throw new MetaDataException(_loc.get(prefix + "-no-col-name",
                context));

        // determine the column name based on given info, or template if none;
        // also make sure that if the user gave a column name, he didn't try
        // to put the column in an unexpected table
        if (colName == null)
            colName = tmplate.getName();
        int dotIdx = colName.lastIndexOf('.');
        if (dotIdx == 0)
            colName = colName.substring(1);
        else if (dotIdx != -1) {
            findTable(context, colName.substring(0, dotIdx), table,
                null, null);
            colName = colName.substring(dotIdx + 1);
        }

        // find existing column
        Column col = table.getColumn(colName);
        if (col == null && !adapt)
            throw new MetaDataException(_loc.get(prefix + "-bad-col-name",
                context, colName, table));

        MappingRepository repos = (MappingRepository) context.getRepository();
        DBDictionary dict = repos.getDBDictionary();

        // use information from template column by default, allowing any
        // user-given specifics to override it
        int type = tmplate.getType();
        int size = tmplate.getSize();
        if (type == Types.OTHER)
            type = dict.getJDBCType(tmplate.getJavaType(), size == -1);
        boolean ttype = true;
        int otype = type;
        String typeName = tmplate.getTypeName();
        Boolean notNull = null;
        if (tmplate.isNotNullExplicit())
            notNull = (tmplate.isNotNull()) ? Boolean.TRUE : Boolean.FALSE;
        int decimals = tmplate.getDecimalDigits();
        String defStr = tmplate.getDefaultString();
        boolean autoAssign = tmplate.isAutoAssigned();
        boolean relationId = tmplate.isRelationId();
        String targetField = tmplate.getTargetField();
        if (given != null) {
            // use given type if provided, but warn if it isn't compatible with
            // the expected column type
            if (given.getType() != Types.OTHER) {
                ttype = false;
                if (compat && !given.isCompatible(type, typeName, size, 
                    decimals)) {
                    Log log = repos.getLog();
                    if (log.isWarnEnabled())
                        log.warn(_loc.get(prefix + "-incompat-col",
                            context, colName, Schemas.getJDBCName(type)));
                }
                otype = given.getType();
                type = dict.getPreferredType(otype);
            }
            typeName = given.getTypeName();
            size = given.getSize();
            decimals = given.getDecimalDigits();

            // leave this info as the template defaults unless the user
            // explicitly turns it on in the given column
            if (given.isNotNullExplicit())
                notNull = (given.isNotNull()) ? Boolean.TRUE : Boolean.FALSE;
            if (given.getDefaultString() != null)
                defStr = given.getDefaultString();
            if (given.isAutoAssigned())
                autoAssign = true;
            if (given.isRelationId())
                relationId = true;
        }

        // default char column size if original type is char (test original
        // type rather than final type because orig might be clob, translated
        // to an unsized varchar, which is supported by some dbs)
        if (size == 0 && (otype == Types.VARCHAR || otype == Types.CHAR))
            size = dict.characterColumnSize;

        // create column, or make sure existing column matches expected type
        if (col == null) {
            col = table.addColumn(colName);
            col.setType(type);
        } else if ((compat || !ttype) && !col.isCompatible(type, typeName, 
            size, decimals)) {
            // if existing column isn't compatible with desired type, die if
            // can't adapt, else warn and change the existing column type
            Message msg = _loc.get(prefix + "-bad-col", context,
                Schemas.getJDBCName(type), col.getDescription());
            if (!adapt)
                throw new MetaDataException(msg);
            Log log = repos.getLog();
            if (log.isWarnEnabled())
                log.warn(msg);

            col.setType(type);
        } else if (given != null && given.getType() != Types.OTHER) {
            // as long as types are compatible, set column to expected type
            col.setType(type);
        }

        // always set the java type and autoassign to expected values, even on
        // an existing column, since we don't get this from the DB
        if (compat)
            col.setJavaType(tmplate.getJavaType());
        else if (col.getJavaType() == JavaTypes.OBJECT) {
            if (given != null && given.getJavaType() != JavaTypes.OBJECT)
                col.setJavaType(given.getJavaType());
            else
                col.setJavaType(JavaTypes.getTypeCode
                    (Schemas.getJavaType(col.getType(), col.getSize(),
                        col.getDecimalDigits())));
        }
        col.setAutoAssigned(autoAssign);
        col.setRelationId(relationId);
        col.setTargetField(targetField);

        // we need this for runtime, and the dynamic schema factory might
        // not know it, so set it even if not adapting
        if (defStr != null)
            col.setDefaultString(defStr);
        if (notNull != null)
            col.setNotNull(notNull.booleanValue());

        // add other details if adapting
        if (adapt) {
            if (typeName != null)
                col.setTypeName(typeName);
            if (size != 0)
                col.setSize(size);
            if (decimals != 0)
                col.setDecimalDigits(decimals);
        }
        return col;
    }

    /**
     * Find the table named by a column or target.
     *
     * @param context context for error messages, etc.
     * @param name the table name, possibly including schema
     * @param expected the expected table; may be null
     * @param inverse the possible inverse table; may be null
     * @param rel if we're finding the target table of a join, the
     * joined-to type; allows us to also look in its superclass tables
     */
    private static Table findTable(MetaDataContext context, String name,
        Table expected, Table inverse, ClassMapping rel) {
        // is this the expected table?
        if (expected == null && rel != null)
            expected = rel.getTable();
        if (expected != null && isTableName(name, expected))
            return expected;

        // check for inverse
        if (inverse != null && isTableName(name, inverse))
            return inverse;

        // superclass table?
        if (rel != null)
            rel = rel.getJoinablePCSuperclassMapping();
        while (rel != null) {
            if (isTableName(name, rel.getTable()))
                return rel.getTable();
            rel = rel.getJoinablePCSuperclassMapping();
        }

        // none of the possible tables
        throw new MetaDataException(_loc.get("col-wrong-table", context,
            expected, name));
    }

    /**
     * Return whether the given name matches the given table.
     */
    private static boolean isTableName(String name, Table table) {
        return name.equalsIgnoreCase(table.getName())
            || name.equalsIgnoreCase(table.getFullName());
    }

    /**
     * Retrieve/create an index on the given columns by merging the given
     * template information with any user-provided information.
     *
     * @param context the mapping we're retrieving an index for
     * @param prefix localized error message key prefix
     * @param tmplate template for expected index information
     * @param cols the indexed columns
     * @param adapt whether we can modify the existing mapping or schema
     */
    protected Index createIndex(MetaDataContext context, String prefix,
        Index tmplate, Column[] cols, boolean adapt) {
        if (prefix == null)
            prefix = "generic";

        // can't create an index if there are no cols
        if (cols == null || cols.length == 0) {
            if (_idx != null)
                throw new MetaDataException(_loc.get(prefix
                    + "-no-index-cols", context));
            return null;
        }

        // look for an existing index on these columns
        Table table = cols[0].getTable();
        Index[] idxs = table.getIndexes();
        Index exist = null;
        for (int i = 0; i < idxs.length; i++) {
            if (idxs[i].columnsMatch(cols)) {
                exist = idxs[i];
                break;
            }
        }

        // remove existing index?
        if (!_canIdx) {
            if (exist == null)
                return null;
            if (!adapt)
                throw new MetaDataException(_loc.get(prefix + "-index-exists",
                    context));
            table.removeIndex(exist);
            return null;
        }

        // if we have an existing index, merge given info into it
        if (exist != null) {
            if (_idx != null && _idx.isUnique() && !exist.isUnique()) {
                if (!adapt)
                    throw new MetaDataException(_loc.get(prefix
                        + "-index-not-unique", context));
                exist.setUnique(true);
            }
            return exist;
        }

        // if no defaults return null
        MappingRepository repos = (MappingRepository) context.getRepository();
        boolean fill = repos.getMappingDefaults().defaultMissingInfo();
        if (_idx == null && (tmplate == null || (!adapt && !fill)))
            return null;

        String name = null;
        boolean unq;
        if (_idx != null) {
            name = _idx.getName();
            unq = _idx.isUnique();
        } else
            unq = tmplate.isUnique();

        // if no name provided by user info, make one
        if (name == null) {
            if (tmplate != null)
                name = tmplate.getName();
            else {
                name = cols[0].getName();
                name = repos.getDBDictionary().getValidIndexName(name, table);
            }
        }

        Index idx = table.addIndex(name);
        idx.setUnique(unq);
        idx.setColumns(cols);
        return idx;
    }

    /**
     * Retrieve/create a unique constraint on the given columns by merging the
     * given template information with any user-provided information.
     *
     * @param context the mapping we're retrieving a constraint for
     * @param prefix localized error message key prefix
     * @param tmplate template for expected unique information
     * @param cols the constraint columns
     * @param adapt whether we can modify the existing mapping or schema
     */
    protected Unique createUnique(MetaDataContext context, String prefix,
        Unique tmplate, Column[] cols, boolean adapt) {
        if (prefix == null)
            prefix = "generic";

        // can't create a constraint if there are no cols
        if (cols == null || cols.length == 0) {
            if (_unq != null || tmplate != null)
                throw new MetaDataException(_loc.get(prefix
                    + "-no-unique-cols", context));
            return null;
        }

        // look for an existing constraint on these columns
        Table table = cols[0].getTable();
        Unique[] unqs = table.getUniques();
        Unique exist = null;
        for (int i = 0; i < unqs.length; i++) {
            if (unqs[i].columnsMatch(cols)) {
                exist = unqs[i];
                break;
            }
        }

        // remove existing unique?
        if (!_canUnq) {
            if (exist == null)
                return null;
            if (!adapt)
                throw new MetaDataException(_loc.get(prefix
                    + "-unique-exists", context));
            table.removeUnique(exist);
            return null;
        }

        // no defaults; return existing constraint (if any)
        if (tmplate == null && _unq == null)
            return exist;

        MappingRepository repos = (MappingRepository) context.getRepository();
        if (exist != null) {
            if (_unq != null && _unq.isDeferred() && !exist.isDeferred()) {
                Log log = repos.getLog();
                if (log.isWarnEnabled())
                    log.warn(_loc.get(prefix + "-defer-unique", context));
            }
            return exist;
        }

        // dict can't handle unique constraints?
        DBDictionary dict = repos.getDBDictionary();
        if (_unq != null && !dict.supportsUniqueConstraints) {
            Log log = repos.getLog();
            if (log.isWarnEnabled())
                log.warn(_loc.get(prefix + "-unique-support", context));
            return null;
        }

        boolean fill = repos.getMappingDefaults().defaultMissingInfo();
        if (!adapt && !fill && _unq == null)
            return null;

        String name;
        boolean deferred;
        if (_unq != null) {
            name = _unq.getName();
            deferred = _unq.isDeferred();
        } else {
            name = tmplate.getName();
            deferred = tmplate.isDeferred();
        }

        if (deferred && !dict.supportsDeferredConstraints) {
            Log log = repos.getLog();
            if (log.isWarnEnabled())
                log.warn(_loc.get(prefix + "-create-defer-unique",
                    context, dict.platform));
            deferred = false;
        }

        Unique unq = table.addUnique(name);
        unq.setDeferred(deferred);
        unq.setColumns(cols);
        return unq;
    }

    /**
     * Retrieve/create a foreign key (possibly logical) on the given columns
     * by merging the given template information with any user-provided
     * information.
     *
     * @param context the mapping we're retrieving a key for
     * @param prefix localized error message key prefix
     * @param given the columns given by the user
     * @param def defaults provider
     * @param table the table for the key
     * @param cls type we're joining from
     * @param rel target type we're joining to
     * @param inversable whether the foreign key can be inversed
     * @param adapt whether we can modify the existing mapping or schema
     */
    protected ForeignKey createForeignKey(MetaDataContext context,
        String prefix, List given, ForeignKeyDefaults def, Table table,
        ClassMapping cls, ClassMapping rel, boolean inversable, boolean adapt) {
        assertTable(context, table);
        if (prefix == null)
            prefix = "generic";

        // collect the foreign key columns and their targets
        Object[][] joins = createJoins(context, prefix, table, cls, rel,
            given, def, inversable, adapt);
        _join = JOIN_FORWARD;

        // establish local table using any join between two columns; if we only
        // find constant joins, then keep default local table (directionless)
        Table local = table;
        Table foreign = rel.getTable();
        Table tmp;
        boolean constant = false;
        boolean localSet = false;
        for (int i = 0; i < joins.length; i++) {
            if (joins[i][1]instanceof Column) {
                tmp = ((Column) joins[i][0]).getTable();
                if (!localSet) {
                    local = tmp;
                    localSet = true;
                } else if (tmp != local)
                    throw new MetaDataException(_loc.get(prefix
                        + "-mult-fk-tables", context, local, tmp));
                foreign = ((Column) joins[i][1]).getTable();

                if (joins[i][2] == Boolean.TRUE)
                    _join = JOIN_INVERSE;
            } else
                constant = true;
        }

        // if this is not a constant join, look for existing foreign key
        // on local columns
        ForeignKey exist = null;
        if (!constant && local.getForeignKeys().length > 0) {
            Column[] cols = new Column[joins.length];
            Column[] pks = new Column[joins.length];
            for (int i = 0; i < joins.length; i++) {
                cols[i] = (Column) joins[i][0];
                pks[i] = (Column) joins[i][1];
            }

            ForeignKey[] fks = local.getForeignKeys();
            for (int i = 0; i < fks.length; i++) {
                if (fks[i].getConstantColumns().length == 0
                    && fks[i].getConstantPrimaryKeyColumns().length == 0
                    && fks[i].columnsMatch(cols, pks)) {
                    exist = fks[i];
                    break;
                }
            }
        }

        MappingRepository repos = (MappingRepository) context.getRepository();
        DBDictionary dict = repos.getDBDictionary();
        if (exist != null) {
            // make existing key logical?
            if (!_canFK) {
                if (exist.getDeleteAction() != exist.ACTION_NONE && !adapt)
                    throw new MetaDataException(_loc.get(prefix
                        + "-fk-exists", context));
                exist.setDeleteAction(exist.ACTION_NONE);
            }

            if (_fk != null && _fk.isDeferred() && !exist.isDeferred()) {
                Log log = repos.getLog();
                if (log.isWarnEnabled())
                    log.warn(_loc.get(prefix + "-defer-fk", context));
            }

            // allow user-given info to override existing key if we're adapting;
            // template info cannot override existing key
            if (adapt && _fk != null) {
                if (_fk.getUpdateAction() != ForeignKey.ACTION_NONE)
                    exist.setUpdateAction(_fk.getUpdateAction());
                if (_fk.getDeleteAction() != ForeignKey.ACTION_NONE)
                    exist.setDeleteAction(_fk.getDeleteAction());
            }
            setIOFromJoins(exist, joins);
            return exist;
        }

        String name = null;
        int delAction = ForeignKey.ACTION_NONE;
        int upAction = ForeignKey.ACTION_NONE;
        boolean deferred = false;
        boolean fill = repos.getMappingDefaults().defaultMissingInfo();
        ForeignKey tmplate = (def == null) ? null
            : def.get(local, foreign, _join == JOIN_INVERSE);
        if (_fk != null && (tmplate == null || (!adapt && !fill))) {
            // if not adapting or no template info use given data
            name = _fk.getName();
            delAction = _fk.getDeleteAction();
            upAction = _fk.getUpdateAction();
            deferred = _fk.isDeferred();
        } else if (_canFK && (adapt || fill)) {
            if (_fk == null && tmplate != null) {
                // no user given info; use template data
                name = tmplate.getName();
                delAction = tmplate.getDeleteAction();
                upAction = tmplate.getUpdateAction();
                deferred = tmplate.isDeferred();
            } else if (_fk != null && tmplate != null) {
                // merge user and template data, always letting user info win
                name = _fk.getName();
                if (name == null && tmplate.getName() != null)
                    name = tmplate.getName();
                delAction = _fk.getDeleteAction();
                if (delAction == ForeignKey.ACTION_NONE)
                    delAction = tmplate.getDeleteAction();
                upAction = _fk.getUpdateAction();
                if (upAction == ForeignKey.ACTION_NONE)
                    upAction = tmplate.getUpdateAction();
                deferred = _fk.isDeferred();
            }
        }

        if (!dict.supportsDeleteAction(delAction)
            || !dict.supportsUpdateAction(upAction)) {
            Log log = repos.getLog();
            if (log.isWarnEnabled())
                log.warn(_loc.get(prefix + "-unsupported-fk-action", context));
            delAction = ForeignKey.ACTION_NONE;
            upAction = ForeignKey.ACTION_NONE;
        }
        if (deferred && !dict.supportsDeferredConstraints) {
            Log log = repos.getLog();
            if (log.isWarnEnabled())
                log.warn(_loc.get(prefix + "-create-defer-fk",
                    context, dict.platform));
            deferred = false;
        }

        // create foreign key with merged info
        ForeignKey fk = local.addForeignKey(name);
        fk.setDeleteAction(delAction);
        fk.setUpdateAction(upAction);
        fk.setDeferred(deferred);

        // add joins to key
        Column col;
        for (int i = 0; i < joins.length; i++) {
            col = (Column) joins[i][0];
            if (joins[i][1]instanceof Column)
                fk.join(col, (Column) joins[i][1]);
            else if ((joins[i][2] == Boolean.TRUE) != (_join == JOIN_INVERSE))
                fk.joinConstant(joins[i][1], col);
            else
                fk.joinConstant(col, joins[i][1]);
        }
        setIOFromJoins(fk, joins);
        return fk;
    }

    /**
     * Use the join information to populate our internal column I/O data.
     */
    private void setIOFromJoins(ForeignKey fk, Object[][] joins) {
        List cols = getColumns();
        _io = null;
        if (cols.isEmpty())
            return;

        int constIdx = 0;
        int idx;
        for (int i = 0; i < joins.length; i++) {
            // const columns are indexed after std join columns in fk IO
            if (joins[i][1]instanceof Column)
                idx = i - constIdx;
            else if ((joins[i][2] == Boolean.TRUE) == (_join == JOIN_INVERSE))
                idx = fk.getColumns().length + constIdx++;
            else
                continue;
            setIOFromColumnFlags((Column) cols.get(i), idx);
        }
    }

    /**
     * Create or retrieve the foreign key joins.
     *
     * @param context the mapping we're retrieving a key for
     * @param prefix localized error message key prefix
     * @param table the table for the key
     * @param cls type we're joining from, if applicable
     * @param rel target type we're joining to
     * @param given the columns given by the user
     * @param def foreign key defaults provider
     * @param inversable whether the foreign key can be inversed
     * @param adapt whether we can modify the existing mapping or schema
     * @return array of tuples where the first element is the
     * local column (or in the case of a constant join the
     * sole column), the second is the target column (or
     * constant), and the third is {@link Boolean#TRUE} if
     * this is an inverse join
     */
    private Object[][] createJoins(MetaDataContext context,
        String prefix, Table table, ClassMapping cls, ClassMapping rel,
        List given, ForeignKeyDefaults def, boolean inversable, boolean adapt) {
        MappingRepository repos = (MappingRepository) context.getRepository();
        boolean fill = repos.getMappingDefaults().defaultMissingInfo();
        Object[][] joins;

        // if no columns given, just create mirrors of target columns
        if (given.isEmpty()) {
            if (!adapt && !fill)
                throw new MetaDataException(_loc.get(prefix + "-no-fk-cols",
                    context));

            Column[] targets = rel.getPrimaryKeyColumns();
            joins = new Object[targets.length][3];
            Column tmplate;
            for (int i = 0; i < targets.length; i++) {
                tmplate = new Column();
                tmplate.setName(targets[i].getName());
                tmplate.setJavaType(targets[i].getJavaType());
                tmplate.setType(targets[i].getType());
                tmplate.setTypeName(targets[i].getTypeName());
                tmplate.setSize(targets[i].getSize());
                tmplate.setDecimalDigits(targets[i].getDecimalDigits());

                if (def != null)
                    def.populate(table, rel.getTable(), tmplate, targets[i],
                        false, i, targets.length);
                joins[i][0] = mergeColumn(context, prefix, tmplate, true,
                    null, table, adapt, fill);
                joins[i][1] = targets[i];
            }
            return joins;
        }

        // use given columns to create join.  we don't try to use any of the
        // template columns, even if the user doesn't give a column linking to
        // every primary key of the target type -- users are allowed to create
        // partial joins.  this means, though, that if a user wants to specify
        // info for one join column, he has to at least create elements for
        // all of them

        joins = new Object[given.size()][3];
        Column col;
        for (int i = 0; i < joins.length; i++) {
            col = (Column) given.get(i);
            mergeJoinColumn(context, prefix, col, joins, i, table, cls, rel,
                def, inversable && !col.getFlag(Column.FLAG_PK_JOIN), adapt,
                fill);
        }
        return joins;
    }

    /**
     * Create or retrieve a foreign key column for a join.
     *
     * @param context the mapping we're retrieving a key for
     * @param prefix localized error message key prefix
     * @param given the given local foreign key column
     * @param joins array of joins
     * @param idx index of the join array to populate
     * @param table the table for the key
     * @param cls the type we're joining from
     * @param rel target type we're joining to
     * @param def foreign key defaults provider;
     * use null to mirror target column names
     * @param inversable whether the foreign key can be inversed
     * @param adapt whether we can modify the existing mapping or schema
     * @param fill whether to default missing column information
     */
    private void mergeJoinColumn(MetaDataContext context, String prefix,
        Column given, Object[][] joins, int idx, Table table, ClassMapping cls,
        ClassMapping rel, ForeignKeyDefaults def, boolean inversable,
        boolean adapt, boolean fill) {
        // default to the primary key column name if this is a pk join
        String name = given.getName();
        if (name == null && given != null
            && given.getFlag(Column.FLAG_PK_JOIN) && cls != null) {
            Column[] pks = cls.getPrimaryKeyColumns();
            if (pks.length == 1)
                name = pks[0].getName();
        }

        // if we can't adapt, then the user must at least give a column name
        if (name == null && !adapt && !fill)
            throw new MetaDataException(_loc.get(prefix + "-no-fkcol-name",
                context));

        // check to see if the column isn't in the expected table; it might
        // be an inverse join or a join to a base class of the target type
        Table local = table;
        Table foreign = rel.getTable();
        boolean fullName = false;
        boolean inverse = false;
        if (name != null) {
            int dotIdx = name.lastIndexOf('.');
            if (dotIdx != -1) {
                // allow use of '.' without prefix to mean "use expected
                // foreign table"
                if (dotIdx == 0)
                    local = foreign;
                else
                    local = findTable(context, name.substring(0, dotIdx),
                        local, foreign, null);
                fullName = true;
                name = name.substring(dotIdx + 1);

                // if inverse join, then swap local and foreign tables
                if (local != table) {
                    foreign = table;
                    inverse = true;
                }
            }
        }
        boolean forceInverse = !fullName && _join == JOIN_INVERSE;
        if (forceInverse) {
            local = foreign;
            foreign = table;
            inverse = true;
        }

        // determine target
        String targetName = given.getTarget();
        Object target = null;
        Table ttable = null;
        boolean constant = false;
        boolean fullTarget = false;
        if (targetName == null && given.getTargetField() != null) {
            ClassMapping tcls = (inverse) ? cls : rel;
            String fieldName = given.getTargetField();
            int dotIdx = fieldName.lastIndexOf('.');
            fullTarget = dotIdx != -1;

            if (dotIdx == 0) {
                // allow use of '.' without prefix to mean "use expected local
                // cls"; but if we already inversed no need to switch again
                if (!inverse)
                    tcls = cls;
                fieldName = fieldName.substring(1);
            } else if (dotIdx > 0) {
                // must be class + field name
                tcls = findClassMapping(context, fieldName.substring
                    (0, dotIdx), cls, rel);
                fieldName = fieldName.substring(dotIdx + 1);
            }
            if (tcls == null)
                throw new MetaDataException(_loc.get(prefix
                    + "-bad-fktargetcls", context, fieldName, name));

            FieldMapping field = tcls.getFieldMapping(fieldName);
            if (field == null)
                throw new MetaDataException(_loc.get(prefix
                    + "-bad-fktargetfield", new Object[]{ context, fieldName,
                    name, tcls }));
            if (field.getColumns().length != 1)
                throw new MetaDataException(_loc.get(prefix
                    + "-fktargetfield-cols", context, fieldName, name));
            ttable = (field.getJoinForeignKey() != null) ? field.getTable()
                : field.getDefiningMapping().getTable();
            targetName = field.getColumns()[0].getName();
        } else if (targetName != null) {
            if (targetName.charAt(0) == '\'') {
                constant = true;
                target = targetName.substring(1, targetName.length() - 1);
            } else if (targetName.charAt(0) == '-'
                || targetName.charAt(0) == '.'
                || Character.isDigit(targetName.charAt(0))) {
                constant = true;
                try {
                    if (targetName.indexOf('.') == -1)
                        target = new Integer(targetName);
                    else
                        target = new Double(targetName);
                } catch (RuntimeException re) {
                    throw new MetaDataException(_loc.get(prefix
                        + "-bad-fkconst", context, targetName, name));
                }
            } else if ("null".equalsIgnoreCase(targetName))
                constant = true;
            else {
                int dotIdx = targetName.lastIndexOf('.');
                fullTarget = dotIdx != -1;
                if (dotIdx == 0) {
                    // allow use of '.' without prefix to mean "use expected
                    // local table", but ignore if we're already inversed
                    if (!inverse)
                        ttable = local;
                    targetName = targetName.substring(1);
                } else if (dotIdx != -1) {
                    ttable = findTable(context, targetName.substring(0,
                        dotIdx), foreign, local, (inverse) ? cls : rel);
                    targetName = targetName.substring(dotIdx + 1);
                }
            }
        }

        // use explicit target table if available
        if (ttable == local && local != foreign) {
            // swap, unless user gave incompatible table in column name
            if (fullName)
                throw new MetaDataException(_loc.get(prefix
                    + "-bad-fktarget-inverse", new Object[]{ context, name,
                    foreign, ttable }));
            local = foreign;
            foreign = ttable;
        } else if (ttable != null) {
            // ttable might be a table of a base class of the target
            foreign = ttable;
        }

        // check to see if we inversed; if this is a same-table join, then
        // consider it an implicit inverse if the user includes the table name
        // in the column name, but not in the column target, or if the user
        // gives no column name but a full target name
        inverse = inverse || local != table || (local == foreign
            && ((fullName && !fullTarget) || (name == null && fullTarget)));
        if (!inversable && !constant && inverse) {
            if (local == foreign)
                throw new MetaDataException(_loc.get(prefix
                    + "-bad-fk-self-inverse", context, local));
            throw new MetaDataException(_loc.get(prefix + "-bad-fk-inverse",
                context, local, table));
        }
        if (name == null && constant)
            throw new MetaDataException(_loc.get(prefix
                + "-no-fkcol-name-adapt", context));

        if (name == null && targetName == null) {
            // if no name or target is provided and there's more than one likely
            // join possibility, too ambiguous
            PrimaryKey pk = foreign.getPrimaryKey();
            if (joins.length != 1 || pk == null || pk.getColumns().length != 1)
                throw new MetaDataException(_loc.get(prefix
                    + "-no-fkcol-name-adapt", context));

            // assume target is pk column
            targetName = pk.getColumns()[0].getName();
        } else if (name != null && targetName == null) {
            // if one primary key column use it for target; if multiple joins
            // look for a foreign column with same name as local column
            PrimaryKey pk = foreign.getPrimaryKey();
            if (joins.length == 1 && pk != null && pk.getColumns().length == 1)
                targetName = pk.getColumns()[0].getName();
            else if (foreign.getColumn(name) != null)
                targetName = name;
            else
                throw new MetaDataException(_loc.get(prefix
                    + "-no-fkcol-target-adapt", context, name));
        }

        // find the target column, and create template for local column based
        // on it
        Column tmplate = new Column();
        tmplate.setName(name);
        if (!constant) {
            Column tcol = foreign.getColumn(targetName);
            if (tcol == null)
                throw new MetaDataException(_loc.get(prefix + "-bad-fktarget",
                    new Object[]{ context, targetName, name, foreign }));

            if (name == null)
                tmplate.setName(tcol.getName());
            tmplate.setJavaType(tcol.getJavaType());
            tmplate.setType(tcol.getType());
            tmplate.setTypeName(tcol.getTypeName());
            tmplate.setSize(tcol.getSize());
            tmplate.setDecimalDigits(tcol.getDecimalDigits());
            target = tcol;
        } else if (target instanceof String)
            tmplate.setJavaType(JavaTypes.STRING);
        else if (target instanceof Integer)
            tmplate.setJavaType(JavaTypes.INT);
        else if (target instanceof Double)
            tmplate.setJavaType(JavaTypes.DOUBLE);

        // populate template, but let user-given name override default name
        if (def != null)
            def.populate(local, foreign, tmplate, target, inverse, idx,
                joins.length);
        if (name != null)
            tmplate.setName(name);

        // create or merge local column
        Column col = mergeColumn(context, prefix, tmplate, true, given, local,
            adapt, fill);

        joins[idx][0] = col;
        joins[idx][1] = target;
        if (inverse)
            joins[idx][2] = Boolean.TRUE;
    }

    /**
     * Find the target class mapping given the user's class name.
     *
     * @param context for error messages
     * @param clsName class name given by user
     * @param cls original source mapping
     * @param rel original target mapping
     */
    private static ClassMapping findClassMapping(MetaDataContext context,
        String clsName, ClassMapping cls, ClassMapping rel) {
        if (isClassMappingName(clsName, cls))
            return cls;
        if (isClassMappingName(clsName, rel))
            return rel;
        throw new MetaDataException(_loc.get("target-wrong-cls", new Object[]
            { context, clsName, cls, rel }));
    }

    /**
     * Return whether the given name matches the given mapping.
     */
    private static boolean isClassMappingName(String name, ClassMapping cls) {
        if (cls == null)
            return false;
        if (name.equals(cls.getDescribedType().getName())
            || name.equals(Strings.getClassName(cls.getDescribedType())))
            return true;
        return isClassMappingName(name, cls.getPCSuperclassMapping());
    }

    /**
     * Sets internal column information to match the given mapped columns.
     *
     * @param forceJDBCType whether to force the jdbc-type of the columns
     * to be set, even when it matches the default for the columns' java type
     */
    protected void syncColumns(MetaDataContext context, Column[] cols,
        boolean forceJDBCType) {
        if (cols == null || cols.length == 0)
            _cols = null;
        else {
            _cols = new ArrayList(cols.length);
            Column col;
            for (int i = 0; i < cols.length; i++) {
                col = syncColumn(context, cols[i], cols.length,
                    forceJDBCType, cols[i].getTable(), null, null, false);
                setColumnFlagsFromIO(col, i);
                _cols.add(col);
            }
        }
    }

    /**
     * Set I/O flags on the column.
     */
    private void setColumnFlagsFromIO(Column col, int i) {
        if (_io == null)
            return;
        col.setFlag(Column.FLAG_UNUPDATABLE, !_io.isUpdatable(i, false));
        col.setFlag(Column.FLAG_UNINSERTABLE, !_io.isInsertable(i, false));
    }

    /**
     * Sets internal index information to match given mapped index.
     */
    protected void syncIndex(MetaDataContext context, Index idx) {
        if (idx == null) {
            _idx = null;
            return;
        }

        _canIdx = true;
        _idx = new Index();
        _idx.setName(idx.getName());
        _idx.setUnique(idx.isUnique());
    }

    /**
     * Sets internal constraint information to match given mapped constraint.
     */
    protected void syncUnique(MetaDataContext context, Unique unq) {
        if (unq == null) {
            _unq = null;
            return;
        }

        _canUnq = true;
        _unq = new Unique();
        _unq.setName(unq.getName());
        _unq.setDeferred(unq.isDeferred());
    }

    /**
     * Sets internal constraint and column information to match given mapped
     * constraint.
     *
     * @param local default local table
     * @param target default target table
     */
    protected void syncForeignKey(MetaDataContext context, ForeignKey fk,
        Table local, Table target) {
        if (fk == null) {
            _fk = null;
            _cols = null;
            _join = JOIN_NONE;
            return;
        }
        if (_join == JOIN_NONE)
            _join = JOIN_FORWARD;

        if (fk.isLogical())
            _fk = null;
        else {
            _canFK = true;
            _fk = new ForeignKey();
            _fk.setName(fk.getName());
            _fk.setDeleteAction(fk.getDeleteAction());
            _fk.setUpdateAction(fk.getUpdateAction());
            _fk.setDeferred(fk.isDeferred());
        }

        Column[] cols = fk.getColumns();
        Column[] pkCols = fk.getPrimaryKeyColumns();
        Column[] ccols = fk.getConstantColumns();
        Object[] cs = fk.getConstants();
        Column[] cpkCols = fk.getConstantPrimaryKeyColumns();
        Object[] cpks = fk.getPrimaryKeyConstants();

        int size = cols.length + ccols.length + cpkCols.length;
        _cols = new ArrayList(size);
        Column col;
        for (int i = 0; i < cols.length; i++) {
            col = syncColumn(context, cols[i], size, false, local,
                target, pkCols[i], _join == JOIN_INVERSE);
            setColumnFlagsFromIO(col, i);
            _cols.add(col);
        }
        Object constant;
        for (int i = 0; i < ccols.length; i++) {
            constant = (cs[i] == null) ? NULL : cs[i];
            col = syncColumn(context, ccols[i], size, false, local,
                target, constant, _join == JOIN_INVERSE);
            setColumnFlagsFromIO(col, cols.length + i);
            _cols.add(col);
        }
        for (int i = 0; i < cpkCols.length; i++) {
            constant = (cpks[i] == null) ? NULL : cpks[i];
            _cols.add(syncColumn(context, cpkCols[i], size, false, target,
                local, constant, _join != JOIN_INVERSE));
        }
    }

    /**
     * Create a copy of the given column with the raw mapping information
     * set correctly, and without settings that match defaults.
     *
     * @param num the number of columns for this mapping
     * @param forceJDBCType whether the jdbc-type of the created column
     * should be set, even if it matches the default
     * for the given column's java type
     * @param colTable expected table for the column
     * @param targetTable expected target table for join column
     * @param target target column or object for join column; for a
     * constant null target, use {@link #NULL}
     * @param inverse whether join column is for inverse join
     */
    protected static Column syncColumn(MetaDataContext context, Column col,
        int num, boolean forceJDBCType, Table colTable, Table targetTable,
        Object target, boolean inverse) {
        // use full name for cols that aren't in the expected table, or that
        // are inverse joins
        DBDictionary dict = ((MappingRepository) context.getRepository()).
            getDBDictionary();
        Column copy = new Column();
        if (col.getTable() != colTable || inverse)
            copy.setName(dict.getFullName(col.getTable(), true)
                + "." + col.getName());
        else
            copy.setName(col.getName());

        // set target if not default
        if (target != null) {
            if (target == NULL)
                copy.setTarget("null");
            else if (target instanceof Column) {
                Column tcol = (Column) target;
                if ((!inverse && tcol.getTable() != targetTable)
                    || (inverse && tcol.getTable() != colTable))
                    copy.setTarget(dict.getFullName(tcol.getTable(), true)
                        + "." + tcol.getName());
                else if (!defaultTarget(col, tcol, num))
                    copy.setTarget(tcol.getName());
            } else if (target instanceof Number)
                copy.setTarget(target.toString());
            else
                copy.setTarget("'" + target + "'");
        } else if (num > 1)
            copy.setTargetField(col.getTargetField());

        if (col.getSize() != 0 && col.getSize() != dict.characterColumnSize
            && (col.getSize() != -1 || !col.isLob()))
            copy.setSize(col.getSize());
        if (col.getDecimalDigits() != 0)
            copy.setDecimalDigits(col.getDecimalDigits());
        if (col.getDefaultString() != null)
            copy.setDefaultString(col.getDefaultString());
        if (col.isNotNull() && !col.isPrimaryKey()
            && (!isPrimitive(col.getJavaType()) || isForeignKey(col)))
            copy.setNotNull(true);

        // set type name if not default
        String typeName = col.getTypeName();
        if (typeName != null || copy.getSize() != 0
            || copy.getDecimalDigits() != 0) {
            // is this the dict default type? have to ensure jdbc-type set
            // prior to finding dict default
            copy.setType(col.getType());
            String defName = dict.getTypeName(copy);
            copy.setType(Types.OTHER);

            // copy should not have size info set if it isn't used in type name
            boolean defSized = defName.indexOf('(') != -1;
            if (!defSized) {
                if (copy.getSize() > 0)
                    copy.setSize(0);
                copy.setDecimalDigits(0);
            }

            if (typeName != null) {
                // make sure to strip size for comparison
                if (typeName.indexOf('(') == -1 && defSized)
                    defName = defName.substring(0, defName.indexOf('('));
                if (!typeName.equalsIgnoreCase(defName))
                    copy.setTypeName(typeName);
            }
        }

        // set jdbc-type if not default or if forced
        if (forceJDBCType
            || (target != null && !(target instanceof Column)
            && col.getType() != Types.VARCHAR)
            || dict.getJDBCType(col.getJavaType(), false) != col.getType())
            copy.setType(col.getType());

        return copy;
    }

    /** 
     * Return whether the given column belongs to a foreign key.
     */ 
    private static boolean isForeignKey(Column col) 
    {       
        if (col.getTable() == null)
            return false;
        ForeignKey[] fks = col.getTable().getForeignKeys();
        for (int i = 0; i < fks.length; i++) 
            if (fks[i].containsColumn(col) 
                || fks[i].containsConstantColumn(col))
                return true;
        return false;
    }

    /**
     * Return true if the given type code represents a primitive.
     */
    private static boolean isPrimitive(int type) {
        switch (type) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BYTE:
            case JavaTypes.CHAR:
            case JavaTypes.DOUBLE:
            case JavaTypes.FLOAT:
            case JavaTypes.INT:
            case JavaTypes.LONG:
            case JavaTypes.SHORT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if the given target column matches the default.
     * If there is only one column involved in the join and it links to the
     * single target table pk column, or if the column name is the same as
     * the target column name, then the target is the default.
     */
    private static boolean defaultTarget(Column col, Column targetCol,
        int num) {
        if (col.getName().equals(targetCol.getName()))
            return true;
        if (num > 1)
            return false;

        PrimaryKey pk = targetCol.getTable().getPrimaryKey();
        if (pk == null || pk.getColumns().length != 1)
            return false;
        return targetCol == pk.getColumns()[0];
    }

    /**
     * Supplies default table information.
     */
    public static interface TableDefaults {

        /**
         * Return the default table name.
         */
        public String get(Schema schema);
    }

    /**
     * Supplies default foreign key information.
     */
    public static interface ForeignKeyDefaults {

        /**
         * Return a default foreign key for the given tables, or null to
         * create a logical foreign key only. Do not fill in the columns of
         * the foreign key, only attributes like its name, delete action, etc.
         * Do not add the foreign key to the table.
         */
        public ForeignKey get(Table local, Table foreign, boolean inverse);

        /**
         * Populate the given foreign key column with defaults.
         *
         * @param target the target column or constant value
         * @param pos the index of this column in the foreign key
         * @param cols the number of columns in the foreign key
         */
        public void populate(Table local, Table foreign, Column col,
            Object target, boolean inverse, int pos, int cols);
	}
}

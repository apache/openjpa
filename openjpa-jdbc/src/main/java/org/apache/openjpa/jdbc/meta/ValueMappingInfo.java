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
package org.apache.openjpa.jdbc.meta;

import java.util.List;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.MetaDataException;

/**
 * Information about the mapping from a field value to the schema, in
 * raw form. The columns and tables used in mapping info will not be part of
 * the {@link SchemaGroup} used at runtime. Rather, they will be structs
 * with the relevant pieces of information filled in.
 *
 * @author Abe White
 */
public class ValueMappingInfo
    extends MappingInfo {

    private static final Localizer _loc = Localizer.forPackage
        (ValueMappingInfo.class);

    private boolean _criteria = false;
    private boolean _canNull = true;

    /**
     * Whether to use class criteria when joining to related type.
     */
    public boolean getUseClassCriteria() {
        return _criteria;
    }

    /**
     * Whether to use class criteria when joining to related type.
     */
    public void setUseClassCriteria(boolean criteria) {
        _criteria = criteria;
    }

    /**
     * Whether user has explicitly turned null indicator column off.
     */
    public boolean canIndicateNull() {
        return _canNull;
    }

    /**
     * Whether user has explicitly turned null indicator column off.
     */
    public void setCanIndicateNull(boolean ind) {
        _canNull = ind;
    }

    /**
     * Return the join from this value to its related type.
     *
     * @param name base name for value mapping
     * @param inversable whether an inverse join is allowed
     */
    public ForeignKey getTypeJoin(final ValueMapping val, final String name,
        boolean inversable, boolean adapt) {
        ClassMapping rel = val.getTypeMapping();
        if (rel == null)
            return null;

        ForeignKeyDefaults def = new ForeignKeyDefaults() {
            public ForeignKey get(Table local, Table foreign, boolean inverse) {
                return val.getMappingRepository().getMappingDefaults().
                    getForeignKey(val, name, local, foreign, inverse);
            }

            public void populate(Table local, Table foreign, Column col,
                Object target, boolean inverse, int pos, int cols) {
                val.getMappingRepository().getMappingDefaults().
                    populateForeignKeyColumn(val, name, local, foreign, col,
                        target, inverse, pos, cols);
            }
        };
        return createForeignKey(val, null, getColumns(), def,
            val.getFieldMapping().getTable(), val.getFieldMapping().
            getDefiningMapping(), rel, inversable, adapt);
    }

    /**
     * Return the join from the related type to this value.
     */
    public ForeignKey getInverseTypeJoin(final ValueMapping val,
        final String name, boolean adapt) {
        ClassMapping rel = val.getTypeMapping();
        if (rel == null || rel.getTable() == null)
            return null;

        ForeignKeyDefaults def = new ForeignKeyDefaults() {
            public ForeignKey get(Table local, Table foreign, boolean inverse) {
                return val.getMappingRepository().getMappingDefaults().
                    getForeignKey(val, name, local, foreign, !inverse);
            }

            public void populate(Table local, Table foreign, Column col,
                Object target, boolean inverse, int pos, int cols) {
                val.getMappingRepository().getMappingDefaults().
                    populateForeignKeyColumn(val, name, local, foreign, col,
                        target, !inverse, pos, cols);
            }
        };
        return createForeignKey(val, null, getColumns(), def, rel.getTable(),
            rel, val.getFieldMapping().getDefiningMapping(), false, adapt);
    }

    /**
     * Return the columns for this value, based on the given templates.
     */
    public Column[] getColumns(ValueMapping val, String name,
        Column[] tmplates, Table table, boolean adapt) {
        orderColumnsByTargetField(val, tmplates, adapt);
        val.getMappingRepository().getMappingDefaults().populateColumns
            (val, name, table, tmplates);
        return createColumns(val, null, tmplates, table, adapt);
    }

    /**
     * Make given columns match up with the target fields supplied on the
     * templates.
     */
    private void orderColumnsByTargetField(ValueMapping val, Column[] tmplates,
        boolean adapt) {
        if (tmplates.length < 2 || tmplates[0].getTargetField() == null)
            return;
        List cols = getColumns();
        if (cols.isEmpty() || cols.size() != tmplates.length)
            return;

        int pos = 0;
        Column cur = (Column) cols.get(0);
        Column next;
        for (int i = 0; i < cols.size(); i++) {
            if (cur.getTargetField() == null)
                throw new MetaDataException(_loc.get("no-targetfield", val));

            pos = findTargetField(tmplates, cur.getTargetField());
            if (pos == -1)
                throw new MetaDataException(_loc.get("bad-targetfield",
                    val, cur.getTargetField()));

            next = (Column) cols.get(pos);
            cols.set(pos, cur);
            cur = next;
        }
    }

    /**
     * Return the position of the template column with the given target field.
     */
    public int findTargetField(Column[] tmplates, String target) {
        for (int i = 0; i < tmplates.length; i++)
            if (target.equals(tmplates[i].getTargetField()))
                return i;
        return -1;
    }

    /**
     * Return a unique constraint for the given columns, or null if none.
     */
    public Unique getUnique(ValueMapping val, String name, boolean adapt) {
        Column[] cols = val.getColumns();
        if (cols.length == 0)
            return null;

        Unique unq = val.getMappingRepository().getMappingDefaults().
            getUnique(val, name, cols[0].getTable(), cols);
        return createUnique(val, null, unq, cols, adapt);
    }

    /**
     * Return an index for the given columns, or null if none.
     */
    public Index getIndex(ValueMapping val, String name, boolean adapt) {
        Column[] cols = val.getColumns();
        if (cols.length == 0)
            return null;

        Index idx = val.getMappingRepository().getMappingDefaults().
            getIndex(val, name, cols[0].getTable(), cols);
        return createIndex(val, null, idx, cols, adapt);
    }

    /**
     * Return the null indicator column for this value, or null if none.
     */
    public Column getNullIndicatorColumn(ValueMapping val, String name,
        Table table, boolean adapt) {
        // reset IO
        setColumnIO(null);

        // has the user explicitly turned null indicator off?
        if (!_canNull)
            return null;

        // extract given null-ind column
        List cols = getColumns();
        Column given = (cols.isEmpty()) ? null : (Column) cols.get(0);
        MappingDefaults def = val.getMappingRepository().getMappingDefaults();
        if (given == null && (!adapt && !def.defaultMissingInfo()))
            return null;

        Column tmplate = new Column();
        tmplate.setName(name + "_null");
        tmplate.setJavaType(JavaTypes.INT);
        if (!def.populateNullIndicatorColumns(val, name, table, new Column[]
            { tmplate }) && given == null)
            return null;

        if (given != null && (given.getFlag(Column.FLAG_UNINSERTABLE)
            || given.getFlag(Column.FLAG_UNUPDATABLE))) {
            ColumnIO io = new ColumnIO();
            io.setInsertable(0, !given.getFlag(Column.FLAG_UNINSERTABLE));
            io.setUpdatable(0, !given.getFlag(Column.FLAG_UNUPDATABLE));
            setColumnIO(io);
        }

        if (given != null && given.getName() != null) {
            // test if given column name is actually a field name, in which
            // case we use its column as the null indicator column
            ClassMapping embed = val.getEmbeddedMapping();
            FieldMapping efm = (embed == null) ? null
                : embed.getFieldMapping(given.getName());
            if (efm != null && efm.getColumns().length > 0)
                given.setName(efm.getColumns()[0].getName());
        }
        boolean compat = given == null || given.getName() == null
            || table == null || !table.isNameTaken(given.getName());

        return mergeColumn(val, "null-ind", tmplate, compat, given,
            table, adapt, def.defaultMissingInfo());
    }

    /**
     * Synchronize internal information with the mapping data for the given
     * value.
     */
    public void syncWith(ValueMapping val) {
        clear(false);

        _criteria = val.getUseClassCriteria();
        setColumnIO(val.getColumnIO());
        if (val.getForeignKey() != null && val.getTypeMapping() != null
            && val.getTypeMapping().getTable() != null) {
            FieldMapping fm = val.getFieldMapping();
            Table local = (fm.getJoinForeignKey() != null) ? fm.getTable()
                : fm.getDefiningMapping().getTable();
            Table foreign;
            if (val.getJoinDirection() == ValueMapping.JOIN_EXPECTED_INVERSE) {
                foreign = local;
                local = val.getTypeMapping().getTable();
                setJoinDirection(JOIN_FORWARD);
            } else {
                foreign = val.getTypeMapping().getTable();
                setJoinDirection((val.getJoinDirection() == val.JOIN_FORWARD)
                    ? JOIN_FORWARD : JOIN_INVERSE);
            }
            syncForeignKey(val, val.getForeignKey(), local, foreign);
        } else
            syncColumns(val, val.getColumns(), false);

        syncIndex(val, val.getValueIndex());
        syncUnique(val, val.getValueUnique());

        // explicit handler strategy if the handler isn't the expected default
        if (val.getHandler() != null) {
            ValueHandler def = val.getFieldMapping().getMappingRepository().
                defaultHandler(val);
            if (def == null || val.getHandler().getClass() != def.getClass())
                setStrategy(val.getHandler().getClass().getName());
        }
    }

    protected void clear(boolean canFlags) {
        super.clear(canFlags);
        if (canFlags) {
            _criteria = false;
            _canNull = true;
        }
    }

    public void copy(MappingInfo info) {
        super.copy(info);
        if (!(info instanceof ValueMappingInfo))
            return;

        ValueMappingInfo vinfo = (ValueMappingInfo) info;
        if (!_criteria)
            _criteria = vinfo.getUseClassCriteria();
        if (_canNull)
            _canNull = vinfo.canIndicateNull();
    }
}

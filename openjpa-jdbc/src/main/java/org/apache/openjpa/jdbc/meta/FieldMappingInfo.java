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

import java.util.List;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.xml.Commentable;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.MetaDataException;

/**
 * Information about the mapping from a field to the schema, in raw form.
 * The columns and tables used in mapping info will not be part of the
 * {@link SchemaGroup} used at runtime. Rather, they will be structs
 * with the relevant pieces of information filled in.
 *
 * @author Abe White
 */
public class FieldMappingInfo
    extends MappingInfo
    implements Commentable {

    private static final Localizer _loc = Localizer.forPackage
        (FieldMappingInfo.class);

    private String _tableName = null;
    private boolean _outer = false;
    private Column _orderCol = null;
    private boolean _canOrderCol = true;
    private String[] _comments = null;

    /**
     * The user-supplied name of the table for this field.
     */
    public String getTableName() {
        return _tableName;
    }

    /**
     * The user-supplied name of the table for this field.
     */
    public void setTableName(String tableName) {
        _tableName = tableName;
    }

    /**
     * Whether the field's table is joined to the class table through an
     * outer join.
     */
    public boolean isJoinOuter() {
        return _outer;
    }

    /**
     * Whether the field's table is joined to the class table through an
     * outer join.
     */
    public void setJoinOuter(boolean outer) {
        _outer = outer;
    }

    /**
     * Raw synthetic ordering column.
     */
    public Column getOrderColumn() {
        return _orderCol;
    }

    /**
     * Raw synthetic ordering column.
     */
    public void setOrderColumn(Column order) {
        _orderCol = order;
    }

    /**
     * Whether we can have an ordering column.
     */
    public boolean canOrderColumn() {
        return _canOrderCol;
    }

    /**
     * Whether we can have an ordering column.
     */
    public void setCanOrderColumn(boolean canOrder) {
        _canOrderCol = canOrder;
    }

    /**
     * Return the table for the given field, or null if no table given.
     */
    public Table getTable(final FieldMapping field, boolean create,
        boolean adapt) {
        if (_tableName == null && !create)
            return null;

        Table table = field.getDefiningMapping().getTable();
        String schemaName = (table == null) ? null 
            : table.getSchema().getName();

        // if we have no join columns defined, there may be class-level join
        // information with a more fully-qualified name for our table
        String tableName = _tableName;
        if (tableName != null && getColumns().isEmpty())
            tableName = field.getDefiningMapping().getMappingInfo().
                getSecondaryTableName(tableName);

        return createTable(field, new TableDefaults() {
            public String get(Schema schema) {
                // delay this so that we don't do schema reflection for unique
                // table name unless necessary
                return field.getMappingRepository().getMappingDefaults().
                    getTableName(field, schema);
            }
        }, schemaName, tableName, adapt);
    }

    /**
     * Return the join from the field table to the owning class table.
     */
    public ForeignKey getJoin(final FieldMapping field, Table table,
        boolean adapt) {
        // if we have no join columns defined, check class-level join
        List cols = getColumns();
        if (cols.isEmpty())
            cols = field.getDefiningMapping().getMappingInfo().
                getSecondaryTableJoinColumns(_tableName);

        ForeignKeyDefaults def = new ForeignKeyDefaults() {
            public ForeignKey get(Table local, Table foreign, boolean inverse) {
                return field.getMappingRepository().getMappingDefaults().
                    getJoinForeignKey(field, local, foreign);
            }

            public void populate(Table local, Table foreign, Column col,
                Object target, boolean inverse, int pos, int cols) {
                field.getMappingRepository().getMappingDefaults().
                    populateJoinColumn(field, local, foreign, col, target,
                        pos, cols);
            }
        };
        ClassMapping cls = field.getDefiningMapping();
        return createForeignKey(field, "join", cols, def, table, cls, cls,
            false, adapt);
    }

    /**
     * Unique constraint on the field join.
     */
    public Unique getJoinUnique(FieldMapping field, boolean def,
        boolean adapt) {
        ForeignKey fk = field.getJoinForeignKey();
        if (fk == null)
            return null;

        Unique unq = null;
        if (fk.getColumns().length > 0)
            unq = field.getMappingRepository().getMappingDefaults().
                getJoinUnique(field, fk.getTable(), fk.getColumns());
        return createUnique(field, "join", unq, fk.getColumns(), adapt);
    }

    /**
     * Index on the field join.
     */
    public Index getJoinIndex(FieldMapping field, boolean adapt) {
        ForeignKey fk = field.getJoinForeignKey();
        if (fk == null)
            return null;

        Index idx = null;
        if (fk.getColumns().length > 0)
            idx = field.getMappingRepository().getMappingDefaults().
                getJoinIndex(field, fk.getTable(), fk.getColumns());
        return createIndex(field, "join", idx, fk.getColumns(), adapt);
    }

    /**
     * Return the ordering column for this field, or null if none.
     */
    public Column getOrderColumn(FieldMapping field, Table table,
        boolean adapt) {
        if (_orderCol != null && field.getOrderDeclaration() != null)
            throw new MetaDataException(_loc.get("order-conflict", field));

        // reset IO
        setColumnIO(null);

        // has user has explicitly turned ordering off?
        if (!_canOrderCol || field.getOrderDeclaration() != null)
            return null;

        // if no defaults return null
        MappingDefaults def = field.getMappingRepository().
            getMappingDefaults();
        if (_orderCol == null && (!adapt && !def.defaultMissingInfo()))
            return null;

        Column tmplate = new Column();
        tmplate.setName("ordr");
        tmplate.setJavaType(JavaTypes.INT);
        if (!def.populateOrderColumns(field, table, new Column[]{ tmplate })
            && _orderCol == null)
            return null;

        if (_orderCol != null && (_orderCol.getFlag(Column.FLAG_UNINSERTABLE)
            || _orderCol.getFlag(Column.FLAG_UNUPDATABLE))) {
            ColumnIO io = new ColumnIO();
            io.setInsertable(0, !_orderCol.getFlag(Column.FLAG_UNINSERTABLE));
            io.setUpdatable(0, !_orderCol.getFlag(Column.FLAG_UNUPDATABLE));
            setColumnIO(io);
        }

        return mergeColumn(field, "order", tmplate, true, _orderCol, table,
            adapt, def.defaultMissingInfo());
    }

    /**
     * Synchronize internal information with the mapping data for the given
     * field.
     */
    public void syncWith(FieldMapping field) {
        clear(false);

        if (field.getJoinForeignKey() != null)
            _tableName = field.getMappingRepository().getDBDictionary().
                getFullName(field.getTable(), true);

        ClassMapping def = field.getDefiningMapping();
        setColumnIO(field.getJoinColumnIO());
        if (field.getJoinForeignKey() != null && def.getTable() != null)
            syncForeignKey(field, field.getJoinForeignKey(),
                field.getTable(), def.getTable());
        _outer = field.isJoinOuter();

        syncIndex(field, field.getJoinIndex());
        syncUnique(field, field.getJoinUnique());
        syncOrderColumn(field);
        syncStrategy(field);
    }

    /**
     * Synchronize internal mapping strategy information with the given field.
     */
    public void syncStrategy(FieldMapping field) {
        setStrategy(null);
        if (field.getHandler() != null || field.getStrategy() == null)
            return;

        // explicit strategy if the strategy isn't the expected default
        Strategy strat = field.getMappingRepository().defaultStrategy
            (field, false);
        if (strat == null || !strat.getAlias().equals(field.getAlias()))
            setStrategy(field.getAlias());
    }

    /**
     * Synchronize internal order column information with the given field.
     */
    public void syncOrderColumn(FieldMapping field) {
        if (field.getOrderColumn() != null)
            _orderCol = syncColumn(field, field.getOrderColumn(), 1, false,
                field.getTable(), null, null, false);
        else
            _orderCol = null;
    }

    public boolean hasSchemaComponents() {
        return super.hasSchemaComponents() || _tableName != null
            || _orderCol != null;
    }

    protected void clear(boolean canFlags) {
        super.clear(canFlags);
        _tableName = null;
        _orderCol = null;
        if (canFlags)
            _canOrderCol = true;
    }

    public void copy(MappingInfo info) {
        super.copy(info);
        if (!(info instanceof FieldMappingInfo))
            return;

        FieldMappingInfo finfo = (FieldMappingInfo) info;
        if (_tableName == null)
            _tableName = finfo.getTableName();
        if (!_outer)
            _outer = finfo.isJoinOuter();
        if (_canOrderCol && _orderCol == null)
            _canOrderCol = finfo.canOrderColumn();
        if (_canOrderCol && finfo.getOrderColumn() != null) {
            if (_orderCol == null)
                _orderCol = new Column();
            _orderCol.copy(finfo.getOrderColumn());
        }
    }

    public String[] getComments() {
        return (_comments == null) ? EMPTY_COMMENTS : _comments;
    }

    public void setComments(String[] comments) {
        _comments = comments;
    }
}

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

import java.sql.SQLException;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.strats.NoneFieldStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.MetaDataException;

/**
 * Specialization of metadata for relational databases.
 *
 * @author Abe White
 */
public class FieldMapping
    extends FieldMetaData
    implements ValueMapping, FieldStrategy {

    private static final Localizer _loc = Localizer.forPackage
        (FieldMapping.class);

    private final ValueMapping _val;
    private final ValueMapping _key;
    private final ValueMapping _elem;
    private final FieldMappingInfo _info;
    private final JDBCColumnOrder _orderCol = new JDBCColumnOrder();
    private FieldStrategy _strategy = null;

    private ForeignKey _fk = null;
    private ColumnIO _io = null;
    private Unique _unq = null;
    private Index _idx = null;
    private boolean _outer = false;
    private int _fetchMode = Integer.MAX_VALUE;

    /**
     * Constructor.
     */
    public FieldMapping(String name, Class type, ClassMapping owner) {
        super(name, type, owner);
        _info = owner.getMappingRepository().newMappingInfo(this);
        _val = (ValueMapping) getValue();
        _key = (ValueMapping) getKey();
        _elem = (ValueMapping) getElement();

        setUsesIntermediate(false);
        setUsesImplData(Boolean.FALSE);
    }

    ///////
    // ORM
    ///////

    /**
     * Raw mapping data about field's join to parent table, as well as
     * miscellaneous specialized columns like order column.
     */
    public FieldMappingInfo getMappingInfo() {
        return _info;
    }

    /**
     * The strategy used to map this mapping.
     */
    public FieldStrategy getStrategy() {
        return _strategy;
    }

    /**
     * The strategy used to map this mapping. The <code>adapt</code>
     * parameter determines whether to adapt when mapping the strategy;
     * use null if the strategy should not be mapped.
     */
    public void setStrategy(FieldStrategy strategy, Boolean adapt) {
        // set strategy first so we can access it during mapping
        FieldStrategy orig = _strategy;
        _strategy = strategy;
        if (strategy != null) {
            try {
                strategy.setFieldMapping(this);
                if (adapt != null)
                    strategy.map(adapt.booleanValue());
            } catch (RuntimeException re) {
                // reset strategy
                _strategy = orig;
                throw re;
            }

            // if set to unmapped, clear defined field cache in parent
            if (!isMapped())
                getDefiningMapping().clearDefinedFieldCache();
        }
    }

    /**
     * The mapping's primary table.
     */
    public Table getTable() {
        if (_fk != null)
            return _fk.getTable();
        if (_val.getForeignKey() != null)
            return _val.getForeignKey().getTable();
        return getDefiningMapping().getTable();
    }

    /**
     * I/O information on the join columns.
     */
    public ColumnIO getJoinColumnIO() {
        return (_io == null) ? ColumnIO.UNRESTRICTED : _io;
    }

    /**
     * I/O information on the join columns.
     */
    public void setJoinColumnIO(ColumnIO io) {
        _io = io;
    }

    /**
     * Foreign key linking the field table to the class' primary table.
     */
    public ForeignKey getJoinForeignKey() {
        return _fk;
    }

    /**
     * Foreign key linking the field table to the class' primary table.
     */
    public void setJoinForeignKey(ForeignKey fk) {
        _fk = fk;
    }

    /**
     * Unique constraint on join foreign key columns.
     */
    public Unique getJoinUnique() {
        return _unq;
    }

    /**
     * Unique constraint on join foreign key columns.
     */
    public void setJoinUnique(Unique unq) {
        _unq = unq;
    }

    /**
     * Index on join foreign key columns.
     */
    public Index getJoinIndex() {
        return _idx;
    }

    /**
     * Index on join foreign key columns.
     */
    public void setJoinIndex(Index idx) {
        _idx = idx;
    }

    /**
     * Whether to use an outer join from the class' primary table.
     */
    public boolean isJoinOuter() {
        return _outer;
    }

    /**
     * Whether to use an outer join from the class' primary table.
     */
    public void setJoinOuter(boolean outer) {
        _outer = outer;
    }

    /**
     * Field order column, if any.
     */
    public Column getOrderColumn() {
        return _orderCol.getColumn();
    }

    /**
     * Field order column, if any.
     */
    public void setOrderColumn(Column order) {
        _orderCol.setColumn(order);
    }

    /**
     * I/O information for order column.
     */
    public ColumnIO getOrderColumnIO() {
        return _orderCol.getColumnIO();
    }

    /**
     * I/O information for order column.
     */
    public void setOrderColumnIO(ColumnIO io) {
        _orderCol.setColumnIO(io);
    }

    /**
     * Increment the reference count of used schema components.
     */
    public void refSchemaComponents() {
        if (_fk != null) {
            _fk.ref();
            _fk.refColumns();
        }
        if (_orderCol.getColumn() != null)
            _orderCol.getColumn().ref();
        _val.refSchemaComponents();
        _key.refSchemaComponents();
        _elem.refSchemaComponents();
    }

    /**
     * Clear mapping information, including strategy.
     */
    public void clearMapping() {
        _strategy = null;
        _fk = null;
        _unq = null;
        _idx = null;
        _outer = false;
        _orderCol.setColumn(null);
        _val.clearMapping();
        _key.clearMapping();
        _elem.clearMapping();
        _info.clear();
        setResolve(MODE_MAPPING, false);
    }

    /**
     * Update {@link MappingInfo} with our current mapping information.
     */
    public void syncMappingInfo() {
        if (isVersion()) {
            // we rely on the fact that the version will setup our mapping
            // info correctly when it is synced
        } else if (getMappedByMapping() != null) {
            _info.clear();
            _val.getValueInfo().clear();
            _key.getValueInfo().clear();
            _elem.getValueInfo().clear();

            FieldMapping mapped = getMappedByMapping();
            _info.syncStrategy(this);
            if (_orderCol.getColumn() != null
                && mapped.getOrderColumn() == null)
                _info.syncOrderColumn(this);
            _val.getValueInfo().setUseClassCriteria
                (_val.getUseClassCriteria());
            _key.getValueInfo().setUseClassCriteria
                (_key.getUseClassCriteria());
            _elem.getValueInfo().setUseClassCriteria
                (_elem.getUseClassCriteria());
        } else {
            _info.syncWith(this);
            _val.syncMappingInfo();
            _key.syncMappingInfo();
            _elem.syncMappingInfo();
        }
    }

    /**
     * Returns true if field class does not use the "none" strategy (including
     * if it has a null strategy, and therefore is probably in the process of
     * being mapped).
     */
    public boolean isMapped() {
        return _strategy != NoneFieldStrategy.getInstance();
    }

    //////////////////////
    // MetaData interface
    //////////////////////

    /**
     * The eager fetch mode, as one of the eager constants in
     * {@link JDBCFetchConfiguration}.
     */
    public int getEagerFetchMode() {
        if (_fetchMode == Integer.MAX_VALUE)
            _fetchMode = FetchConfiguration.DEFAULT;
        return _fetchMode;
    }

    /**
     * The eager fetch mode, as one of the eager constants in
     * {@link JDBCFetchConfiguration}.
     */
    public void setEagerFetchMode(int mode) {
        _fetchMode = mode;
    }

    /**
     * Convenience method to perform cast from
     * {@link FieldMetaData#getRepository}
     */
    public MappingRepository getMappingRepository() {
        return (MappingRepository) getRepository();
    }

    /**
     * Convenience method to perform cast from
     * {@link FieldMetaData#getDefiningMetaData}
     */
    public ClassMapping getDefiningMapping() {
        return (ClassMapping) getDefiningMetaData();
    }

    /**
     * Convenience method to perform cast from
     * {@link FieldMetaData#getDeclaringMetaData}
     */
    public ClassMapping getDeclaringMapping() {
        return (ClassMapping) getDeclaringMetaData();
    }

    /**
     * Convenience method to perform cast from {@link FieldMetaData#getKey}
     */
    public ValueMapping getKeyMapping() {
        return _key;
    }

    /**
     * Convenience method to perform cast from {@link FieldMetaData#getElement}
     */
    public ValueMapping getElementMapping() {
        return _elem;
    }

    /**
     * Convenience method to perform cast from {@link FieldMetaData#getValue}
     */
    public ValueMapping getValueMapping() {
        return (ValueMapping) getValue();
    }

    /**
     * Convenience method to perform cast from
     * {@link FieldMetaData#getMappedByMetaData}
     */
    public FieldMapping getMappedByMapping() {
        return (FieldMapping) getMappedByMetaData();
    }

    /**
     * Convenience method to perform cast from
     * {@link FieldMetaData#getInverseMetaDatas}
     */
    public FieldMapping[] getInverseMappings() {
        return (FieldMapping[]) getInverseMetaDatas();
    }

    public boolean resolve(int mode) {
        int cur = getResolve();
        if (super.resolve(mode))
            return true;
        if ((mode & MODE_MAPPING) != 0 && (cur & MODE_MAPPING) == 0)
            resolveMapping();
        if ((mode & MODE_MAPPING_INIT) != 0 && (cur & MODE_MAPPING_INIT) == 0)
            initializeMapping();
        return false;
    }

    /**
     * Resolve the mapping information for this field.
     */
    private void resolveMapping() {
        MappingRepository repos = getMappingRepository();
        if (repos.getMappingDefaults().defaultMissingInfo()) {
            // copy embedded template mapping info
            ClassMapping cls = getDefiningMapping();
            if (cls.getEmbeddingMapping() != null) {
                ClassMapping orig = repos.getMapping(cls.getDescribedType(),
                    cls.getEnvClassLoader(), true);
                FieldMapping tmplate = orig.getFieldMapping(getName());
                if (tmplate != null)
                    copyMappingInfo(tmplate);
            }
            // copy superclass field info
            else if (cls.isMapped() && cls.getPCSuperclass() != null
                && cls.getDescribedType() != getDeclaringType()) {
                FieldMapping sup = cls.getPCSuperclassMapping().
                    getFieldMapping(getName());
                if (sup != null)
                    copyMappingInfo(sup);
            }
        }

        if (_strategy == null) {
            if (isVersion())
                _strategy = NoneFieldStrategy.getInstance();
            else
                repos.getStrategyInstaller().installStrategy(this);
        }
        Log log = getRepository().getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("field-strategy", getName(),
                _strategy.getAlias()));

        // mark mapped columns
        if (_orderCol.getColumn() != null) {
            if (getOrderColumnIO().isInsertable(0, false))
                _orderCol.getColumn().setFlag(Column.FLAG_DIRECT_INSERT, true);
            if (getOrderColumnIO().isUpdatable(0, false))
                _orderCol.getColumn().setFlag(Column.FLAG_DIRECT_UPDATE, true);
        }
        if (_fk != null) {
            Column[] cols = _fk.getColumns();
            ColumnIO io = getJoinColumnIO();
            for (int i = 0; i < cols.length; i++) {
                if (io.isInsertable(i, false))
                    cols[i].setFlag(Column.FLAG_FK_INSERT, true);
                if (io.isUpdatable(i, false))
                    cols[i].setFlag(Column.FLAG_FK_UPDATE, true);
            }
        }

        _val.resolve(MODE_MAPPING);
        _key.resolve(MODE_MAPPING);
        _elem.resolve(MODE_MAPPING);
    }

    /**
     * Copy mapping info from the given instance to this one.
     */
    public void copyMappingInfo(FieldMapping fm) {
        setMappedBy(fm.getMappedBy());
        _info.copy(fm.getMappingInfo());
        _val.copyMappingInfo(fm.getValueMapping());
        _key.copyMappingInfo(fm.getKeyMapping());
        _elem.copyMappingInfo(fm.getElementMapping());
    }

    /**
     * Prepare mapping for runtime use.
     */
    private void initializeMapping() {
        _val.resolve(MODE_MAPPING_INIT);
        _key.resolve(MODE_MAPPING_INIT);
        _val.resolve(MODE_MAPPING_INIT);
        _strategy.initialize();
    }

    public void copy(FieldMetaData fmd) {
        super.copy(fmd);
        if (_fetchMode == Integer.MAX_VALUE)
            _fetchMode = ((FieldMapping) fmd).getEagerFetchMode();
    }

    protected boolean validateDataStoreExtensionPrefix(String prefix) {
        return "jdbc-".equals(prefix);
    }

    ////////////////////////////////
    // FieldStrategy implementation
    ////////////////////////////////

    public String getAlias() {
        return assertStrategy().getAlias();
    }

    public void map(boolean adapt) {
        assertStrategy().map(adapt);
    }

    /**
     * Map this field to its table, optionally requiring that it be
     * in another table. Utility method for use by mapping strategies.
     */
    public void mapJoin(boolean adapt, boolean joinRequired) {
        Table table = _info.getTable(this, joinRequired, adapt);
        ForeignKey join = null;
        if (table != null)
            join = _info.getJoin(this, table, adapt);
        if (join == null && joinRequired)
            throw new MetaDataException(_loc.get("join-required", this));

        if (join == null) {
            _info.assertNoJoin(this, true);
            _info.assertNoForeignKey(this, !adapt);
            _info.assertNoUnique(this, !adapt);
            _info.assertNoIndex(this, !adapt);
        } else {
            _fk = join;
            _io = _info.getColumnIO();
            _outer = _info.isJoinOuter();
            _unq = _info.getJoinUnique(this, false, adapt);
            _idx = _info.getJoinIndex(this, adapt);
        }
    }

    /**
     * Maps the primary key on the secondary table for this field, if the
     * user's defaults create one. This must be called after
     * this field is mapped so that it's table has its columns set.
     */
    public void mapPrimaryKey(boolean adapt) {
        if (adapt && _fk != null && _fk.getTable().getPrimaryKey() == null)
            getMappingRepository().getMappingDefaults().
                installPrimaryKey(this, _fk.getTable());
    }

    public void initialize() {
        assertStrategy().initialize();
    }

    public void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        assertStrategy().insert(sm, store, rm);
    }

    public void update(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        assertStrategy().update(sm, store, rm);
    }

    public void delete(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        assertStrategy().delete(sm, store, rm);
    }

    /**
     * Delete the row for this object if the reference foreign key exists.
     * Utility method for use by mapping strategies.
     */
    public void deleteRow(OpenJPAStateManager sm, JDBCStore store,
        RowManager rm)
        throws SQLException {
        if (_fk != null) {
            Row row = rm.getRow(getTable(), Row.ACTION_DELETE, sm, true);
            row.whereForeignKey(_fk, sm);
        }
    }

    /**
     * Return the row to use for this field. This method is meant only for
     * single-value fields that might reside in a table that is joined to
     * the primary table through the join foreign key. It is not
     * meant for multi-valued fields like collections and maps. The method
     * checks whether we're using an outer join and if so it deletes the
     * field's previous value, then if the field is non-null returns an insert
     * row for the new value. The join foreign key will already be set on
     * the returned row; mapping strategies just need to set their own values.
     * Utility method for use by mapping strategies.
     */
    public Row getRow(OpenJPAStateManager sm, JDBCStore store, RowManager rm,
        int action)
        throws SQLException {
        Row row = null;
        boolean newOuterRow = false;
        if (_fk != null && _outer && action != Row.ACTION_DELETE) {
            // if updating with outer join, delete old value first, then insert;
            // we can't just update b/c the row might not exist
            if (action == Row.ACTION_UPDATE) {
                // maybe some other field already is updating?
                row = rm.getRow(getTable(), Row.ACTION_UPDATE, sm, false);
                if (row == null) {
                    Row del = rm.getRow(getTable(), Row.ACTION_DELETE, sm,
                        true);
                    del.whereForeignKey(_fk, sm);
                }
            } else
                row = rm.getRow(getTable(), Row.ACTION_INSERT, sm, false);

            // only update/insert if the row exists already or the value is
            // not null/default
            if (row == null && !isNullValue(sm)) {
                row = rm.getRow(getTable(), Row.ACTION_INSERT, sm, true);
                newOuterRow = true;
            }
        } else
            row = rm.getRow(getTable(), action, sm, true);

        // setup fk
        if (row != null && _fk != null) {
            if (row.getAction() == Row.ACTION_INSERT)
                row.setForeignKey(_fk, _io, sm);
            else
                row.whereForeignKey(_fk, sm);

            // if this is a new outer joined row, mark it invalid until
            // some mapping actually sets information on it
            if (newOuterRow)
                row.setValid(false);
        }
        return row;
    }

    /**
     * Return true if this field is null/default in the given instance.
     */
    private boolean isNullValue(OpenJPAStateManager sm) {
        switch (getTypeCode()) {
            case JavaTypes.BOOLEAN:
                return !sm.fetchBoolean(getIndex());
            case JavaTypes.BYTE:
                return sm.fetchByte(getIndex()) == 0;
            case JavaTypes.CHAR:
                return sm.fetchChar(getIndex()) == 0;
            case JavaTypes.DOUBLE:
                return sm.fetchDouble(getIndex()) == 0;
            case JavaTypes.FLOAT:
                return sm.fetchFloat(getIndex()) == 0;
            case JavaTypes.INT:
                return sm.fetchInt(getIndex()) == 0;
            case JavaTypes.LONG:
                return sm.fetchLong(getIndex()) == 0;
            case JavaTypes.SHORT:
                return sm.fetchShort(getIndex()) == 0;
            case JavaTypes.STRING:
                return sm.fetchString(getIndex()) == null;
            default:
                return sm.fetchObject(getIndex()) == null;
        }
    }

    public Boolean isCustomInsert(OpenJPAStateManager sm, JDBCStore store) {
        return assertStrategy().isCustomInsert(sm, store);
    }

    public Boolean isCustomUpdate(OpenJPAStateManager sm, JDBCStore store) {
        return assertStrategy().isCustomUpdate(sm, store);
    }

    public Boolean isCustomDelete(OpenJPAStateManager sm, JDBCStore store) {
        return assertStrategy().isCustomDelete(sm, store);
    }

    public void customInsert(OpenJPAStateManager sm, JDBCStore store)
        throws SQLException {
        assertStrategy().customInsert(sm, store);
    }

    public void customUpdate(OpenJPAStateManager sm, JDBCStore store)
        throws SQLException {
        assertStrategy().customUpdate(sm, store);
    }

    public void customDelete(OpenJPAStateManager sm, JDBCStore store)
        throws SQLException {
        assertStrategy().customDelete(sm, store);
    }

    public void setFieldMapping(FieldMapping owner) {
        assertStrategy().setFieldMapping(owner);
    }

    public int supportsSelect(Select sel, int type, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch) {
        return assertStrategy().supportsSelect(sel, type, sm, store, fetch);
    }

    public void selectEagerParallel(SelectExecutor sel, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, int eagerMode) {
        assertStrategy().selectEagerParallel(sel, sm, store, fetch, eagerMode);
    }

    public void selectEagerJoin(Select sel, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, int eagerMode) {
        assertStrategy().selectEagerJoin(sel, sm, store, fetch, eagerMode);
    }

    public boolean isEagerSelectToMany() {
        return assertStrategy().isEagerSelectToMany();
    }

    public int select(Select sel, OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, int eagerMode) {
        return assertStrategy().select(sel, sm, store, fetch, eagerMode);
    }

    /**
     * Return any joins needed to get from the primary table to this table.
     */
    public Joins join(Select sel) {
        if (_fk == null)
            return null;

        Joins joins = sel.newJoins();
        if (_outer)
            return joins.outerJoin(_fk, true, false);
        return joins.join(_fk, true, false);
    }

    /**
     * Add a <code>wherePrimaryKey</code> or <code>whereForeignKey</code>
     * condition to the given select, depending on whether we have a join
     * foreign key.
     */
    public void wherePrimaryKey(Select sel, OpenJPAStateManager sm,
        JDBCStore store) {
        if (_fk != null)
            sel.whereForeignKey(_fk, sm.getObjectId(), getDefiningMapping(),
                store);
        else
            sel.wherePrimaryKey(sm.getObjectId(), getDefiningMapping(),
                store);
    }

    /**
     * Add ordering to the given select for all non-relation order values,
     * including the synthetic order column, if any.
     *
     * @param elem the related type we're fetching, or null
     * @param joins the joins to this field's table
     */
    public void orderLocal(Select sel, ClassMapping elem, Joins joins) {
        _orderCol.order(sel, elem, joins);
        JDBCOrder[] orders = (JDBCOrder[]) getOrders();
        for (int i = 0; i < orders.length; i++)
            if (!orders[i].isInRelation())
                orders[i].order(sel, elem, joins);
    }

    /**
     * Add ordering to the given select for all relation-based values.
     *
     * @param elem the related type we're fetching
     * @param joins the joins across the relation
     */
    public void orderRelation(Select sel, ClassMapping elem, Joins joins) {
        JDBCOrder[] orders = (JDBCOrder[]) getOrders();
        for (int i = 0; i < orders.length; i++)
            if (orders[i].isInRelation())
                orders[i].order(sel, elem, joins);
    }

    public Object loadEagerParallel(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Object res)
        throws SQLException {
        return assertStrategy().loadEagerParallel(sm, store, fetch, res);
    }

    public void loadEagerJoin(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
        assertStrategy().loadEagerJoin(sm, store, fetch, res);
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
        assertStrategy().load(sm, store, fetch, res);
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        assertStrategy().load(sm, store, fetch);
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return assertStrategy().toDataStoreValue(val, store);
    }

    public Object toKeyDataStoreValue(Object val, JDBCStore store) {
        return assertStrategy().toKeyDataStoreValue(val, store);
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel, Joins joins) {
        assertStrategy().appendIsEmpty(sql, sel, joins);
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel, Joins joins) {
        assertStrategy().appendIsNotEmpty(sql, sel, joins);
    }

    public void appendIsNull(SQLBuffer sql, Select sel, Joins joins) {
        assertStrategy().appendIsNull(sql, sel, joins);
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel, Joins joins) {
        assertStrategy().appendIsNotNull(sql, sel, joins);
    }

    public void appendSize(SQLBuffer sql, Select sel, Joins joins) {
        assertStrategy().appendSize(sql, sel, joins);
    }

    public Joins join(Joins joins, boolean forceOuter) {
        return assertStrategy().join(joins, forceOuter);
    }

    public Joins joinKey(Joins joins, boolean forceOuter) {
        return assertStrategy().joinKey(joins, forceOuter);
    }

    public Joins joinRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        return assertStrategy().joinRelation(joins, forceOuter, traverse);
    }

    public Joins joinKeyRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        return assertStrategy().joinKeyRelation(joins, forceOuter, traverse);
    }

    /**
     * Joins from the owning class' table to the table where this field lies
     * using the join foreign key. Utility method for use by mapping strategies.
     */
    public Joins join(Joins joins, boolean forceOuter, boolean toMany) {
        if (_fk == null)
            return joins;
        if (_outer || forceOuter)
            return joins.outerJoin(_fk, true, toMany);
        return joins.join(_fk, true, toMany);
    }

    public Object loadProjection(JDBCStore store, JDBCFetchConfiguration fetch,
        Result res, Joins joins)
        throws SQLException {
        return assertStrategy().loadProjection(store, fetch, res, joins);
    }

    public Object loadKeyProjection(JDBCStore store,
        JDBCFetchConfiguration fetch, Result res, Joins joins)
        throws SQLException {
        return assertStrategy()
            .loadKeyProjection(store, fetch, res, joins);
    }

    public boolean isVersionable() {
        return assertStrategy().isVersionable();
    }

    public void where(OpenJPAStateManager sm, JDBCStore store, RowManager rm,
        Object prevValue)
        throws SQLException {
        assertStrategy().where(sm, store, rm, prevValue);
    }

    private FieldStrategy assertStrategy() {
        if (_strategy == null)
            throw new InternalException();
        return _strategy;
    }

    ///////////////////////////////
    // ValueMapping implementation
    ///////////////////////////////

    public ValueMappingInfo getValueInfo() {
        return _val.getValueInfo();
    }

    public ValueHandler getHandler() {
        return _val.getHandler();
    }

    public void setHandler(ValueHandler handler) {
        _val.setHandler(handler);
    }

    public FieldMapping getFieldMapping() {
        return this;
    }

    public ClassMapping getTypeMapping() {
        return _val.getTypeMapping();
    }

    public ClassMapping getDeclaredTypeMapping() {
        return _val.getDeclaredTypeMapping();
    }

    public ClassMapping getEmbeddedMapping() {
        return _val.getEmbeddedMapping();
    }

    public FieldMapping getValueMappedByMapping() {
        return _val.getValueMappedByMapping();
    }

    public Column[] getColumns() {
        return _val.getColumns();
    }

    public void setColumns(Column[] cols) {
        _val.setColumns(cols);
    }

    public ColumnIO getColumnIO() {
        return _val.getColumnIO();
    }

    public void setColumnIO(ColumnIO io) {
        _val.setColumnIO(io);
    }

    public ForeignKey getForeignKey() {
        return _val.getForeignKey();
    }

    public ForeignKey getForeignKey(ClassMapping target) {
        return _val.getForeignKey(target);
    }

    public void setForeignKey(ForeignKey fk) {
        _val.setForeignKey(fk);
    }

    public int getJoinDirection() {
        return _val.getJoinDirection();
    }

    public void setJoinDirection(int direction) {
        _val.setJoinDirection(direction);
    }

    public void setForeignKey(Row row, OpenJPAStateManager sm)
        throws SQLException {
        _val.setForeignKey(row, sm);
    }

    public void whereForeignKey(Row row, OpenJPAStateManager sm)
        throws SQLException {
        _val.whereForeignKey(row, sm);
    }

    public ClassMapping[] getIndependentTypeMappings() {
        return _val.getIndependentTypeMappings();
    }

    public int getSelectSubclasses() {
        return _val.getSelectSubclasses();
    }

    public Unique getValueUnique() {
        return _val.getValueUnique();
    }

    public void setValueUnique(Unique unq) {
        _val.setValueUnique(unq);
    }

    public Index getValueIndex() {
        return _val.getValueIndex();
    }

    public void setValueIndex(Index idx) {
        _val.setValueIndex(idx);
    }

    public boolean getUseClassCriteria() {
        return _val.getUseClassCriteria();
    }

    public void setUseClassCriteria(boolean criteria) {
        _val.setUseClassCriteria(criteria);
    }

    public int getPolymorphic() {
        return _val.getPolymorphic();
    }

    public void setPolymorphic(int poly) {
        _val.setPolymorphic(poly);
    }

    public void mapConstraints(String name, boolean adapt) {
        _val.mapConstraints(name, adapt);
    }

    public void copyMappingInfo(ValueMapping vm) {
        _val.copyMappingInfo(vm);
    }
}

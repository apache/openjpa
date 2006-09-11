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
package org.apache.openjpa.jdbc.meta.strats;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Embeddable;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingInfo;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.ValueMappingInfo;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.jdbc.sql.Union;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.OpenJPAId;

/**
 * Mapping for a single-valued relation to another entity.
 *
 * @author Abe White
 * @since 0.4.0
 */
public class RelationFieldStrategy
    extends AbstractFieldStrategy
    implements Embeddable {

    private static final Localizer _loc = Localizer.forPackage
        (RelationFieldStrategy.class);

    private Boolean _fkOid = null;

    public void map(boolean adapt) {
        if (field.getTypeCode() != JavaTypes.PC || field.isEmbeddedPC())
            throw new MetaDataException(_loc.get("not-relation", field));

        field.getKeyMapping().getValueInfo().assertNoSchemaComponents
            (field.getKey(), !adapt);
        field.getElementMapping().getValueInfo().assertNoSchemaComponents
            (field.getElement(), !adapt);
        boolean criteria = field.getValueInfo().getUseClassCriteria();

        // check for named inverse
        FieldMapping mapped = field.getMappedByMapping();
        if (mapped != null) {
            field.getMappingInfo().assertNoSchemaComponents(field, !adapt);
            field.getValueInfo().assertNoSchemaComponents(field, !adapt);
            mapped.resolve(mapped.MODE_META | mapped.MODE_MAPPING);

            if (!mapped.getDefiningMapping().isMapped())
                throw new MetaDataException(_loc.get("mapped-by-unmapped",
                    field, mapped));

            if (mapped.getTypeCode() == JavaTypes.PC) {
                if (mapped.getJoinDirection() == mapped.JOIN_FORWARD) {
                    field.setJoinDirection(field.JOIN_INVERSE);
                    field.setColumns(mapped.getDefiningMapping().
                        getPrimaryKeyColumns());
                } else if (isTypeUnjoinedSubclass(mapped))
                    throw new MetaDataException(_loc.get
                        ("mapped-inverse-unjoined", field.getName(),
                            field.getDefiningMapping(), mapped));

                field.setForeignKey(mapped.getForeignKey
                    (field.getDefiningMapping()));
            } else if (mapped.getElement().getTypeCode() == JavaTypes.PC) {
                if (isTypeUnjoinedSubclass(mapped.getElementMapping()))
                    throw new MetaDataException(_loc.get
                        ("mapped-inverse-unjoined", field.getName(),
                            field.getDefiningMapping(), mapped));

                // warn the user about making the collection side the owner
                Log log = field.getRepository().getLog();
                if (log.isInfoEnabled())
                    log.info(_loc.get("coll-owner", field, mapped));
                field.setForeignKey(mapped.getElementMapping().
                    getForeignKey());
            } else
                throw new MetaDataException(_loc.get("not-inv-relation",
                    field, mapped));

            field.setUseClassCriteria(criteria);
            return;
        }

        // this is necessary to support openjpa 3 mappings, which didn't
        // differentiate between secondary table joins and relations built
        // around an inverse key: check to see if we're mapped as a secondary
        // table join but we're in the table of the related type, and if so
        // switch our join mapping info to our value mapping info
        String tableName = field.getMappingInfo().getTableName();
        Table table = field.getTypeMapping().getTable();
        ValueMappingInfo vinfo = field.getValueInfo();
        if (tableName != null && table != null
            && (tableName.equalsIgnoreCase(table.getName())
            || tableName.equalsIgnoreCase(table.getFullName()))) {
            vinfo.setJoinDirection(MappingInfo.JOIN_INVERSE);
            vinfo.setColumns(field.getMappingInfo().getColumns());
            field.getMappingInfo().setTableName(null);
            field.getMappingInfo().setColumns(null);
        }

        field.mapJoin(adapt, false);
        if (field.getTypeMapping().isMapped()) {
            ForeignKey fk = vinfo.getTypeJoin(field, field.getName(), true,
                adapt);
            field.setForeignKey(fk);
            field.setColumnIO(vinfo.getColumnIO());
            if (vinfo.getJoinDirection() == vinfo.JOIN_INVERSE)
                field.setJoinDirection(field.JOIN_INVERSE);
        } else
            RelationStrategies.mapRelationToUnmappedPC(field, field.getName(),
                adapt);

        field.setUseClassCriteria(criteria);
        field.mapConstraints(field.getName(), adapt);
        field.mapPrimaryKey(adapt);
    }

    /**
     * Return whether our defining mapping is an unjoined subclass of
     * the type of the given value.
     */
    private boolean isTypeUnjoinedSubclass(ValueMapping mapped) {
        ClassMapping def = field.getDefiningMapping();
        for (; def != null; def = def.getJoinablePCSuperclassMapping())
            if (def == mapped.getTypeMapping())
                return false;
        return true;
    }

    public void initialize() {
        field.setUsesIntermediate(true);

        ForeignKey fk = field.getForeignKey();
        if (fk == null)
            _fkOid = Boolean.TRUE;
        else if (field.getJoinDirection() != FieldMapping.JOIN_INVERSE)
            _fkOid = field.getTypeMapping().isForeignKeyObjectId(fk);
    }

    public void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        if (field.getMappedBy() != null)
            return;

        OpenJPAStateManager rel = RelationStrategies.getStateManager
            (sm.fetchObjectField(field.getIndex()), store.getContext());
        if (field.getJoinDirection() == field.JOIN_INVERSE)
            updateInverse(sm, rel, store, rm, sm);
        else {
            Row row = field.getRow(sm, store, rm, Row.ACTION_INSERT);
            if (row != null)
                field.setForeignKey(row, rel);
        }
    }

    public void update(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        if (field.getMappedBy() != null)
            return;

        OpenJPAStateManager rel = RelationStrategies.getStateManager
            (sm.fetchObjectField(field.getIndex()), store.getContext());

        if (field.getJoinDirection() == field.JOIN_INVERSE) {
            nullInverse(sm, rm);
            updateInverse(sm, rel, store, rm, sm);
        } else {
            Row row = field.getRow(sm, store, rm, Row.ACTION_UPDATE);
            if (row != null)
                field.setForeignKey(row, rel);
        }
    }

    public void delete(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        if (field.getMappedBy() != null)
            return;

        if (field.getJoinDirection() == field.JOIN_INVERSE) {
            if (sm.getLoaded().get(field.getIndex())) {
                OpenJPAStateManager rel = RelationStrategies.getStateManager(sm.
                    fetchObjectField(field.getIndex()), store.getContext());
                updateInverse(sm, rel, store, rm, null);
            } else
                nullInverse(sm, rm);
        } else {
            field.deleteRow(sm, store, rm);

            // if our foreign key has a delete action, we need to set the
            // related object so constraints can be evaluated
            OpenJPAStateManager rel = RelationStrategies.getStateManager
                (sm.fetchObjectField(field.getIndex()), store.getContext());
            if (rel != null) {
                ForeignKey fk = field.getForeignKey((ClassMapping)
                    rel.getMetaData());
                if (fk.getDeleteAction() == ForeignKey.ACTION_RESTRICT) {
                    Row row = field.getRow(sm, store, rm, Row.ACTION_DELETE);
                    row.setForeignKey(fk, null, rel);
                }
            }
        }
    }

    /**
     * Null inverse relations that reference the given object.
     */
    private void nullInverse(OpenJPAStateManager sm, RowManager rm)
        throws SQLException {
        ForeignKey fk = field.getForeignKey();
        ColumnIO io = field.getColumnIO();
        if (fk.getDeleteAction() != ForeignKey.ACTION_NONE 
            || !io.isAnyUpdatable(fk, true))
            return;

        // null inverse if not already enforced by fk
        if (field.getIndependentTypeMappings().length != 1)
            throw RelationStrategies.uninversable(field);
        Row row = rm.getAllRows(fk.getTable(), Row.ACTION_UPDATE);
        row.setForeignKey(fk, io, null);
        row.whereForeignKey(fk, sm);
        rm.flushAllRows(row);
    }

    /**
     * This method updates the inverse columns of our relation
     * with the given object.
     */
    private void updateInverse(OpenJPAStateManager sm, OpenJPAStateManager rel,
        JDBCStore store, RowManager rm, OpenJPAStateManager sm2)
        throws SQLException {
        // nothing to do if inverse is null or about to be deleted
        //### should we throw an exception if the inverse is null?
        if (rel == null || rel.isDeleted())
            return;

        ForeignKey fk = field.getForeignKey();
        ColumnIO io = field.getColumnIO();

        int action;
        if (rel.isNew() && !rel.isFlushed()) {
            if (sm2 == null || !io.isAnyInsertable(fk, false))
                return;
            action = Row.ACTION_INSERT;
        } else {
            if (!io.isAnyUpdatable(fk, sm2 == null))
                return;
            action = Row.ACTION_UPDATE;
        }

        if (field.getIndependentTypeMappings().length != 1)
            throw RelationStrategies.uninversable(field);

        // get the row for the inverse object; the row might be in a secondary
        // table if there is a field controlling the foreign key
        Row row = null;
        FieldMapping[] invs = field.getInverseMappings();
        for (int i = 0; i < invs.length; i++) {
            if (invs[i].getMappedByMetaData() == field
                && invs[i].getTypeCode() == JavaTypes.PC) {
                row = invs[i].getRow(rel, store, rm, action);
                break;
            }
        }
        ClassMapping relMapping = field.getTypeMapping();
        if (row == null)
            row = rm.getRow(relMapping.getTable(), action, rel, true);

        // if this is an update, this might be the only mod to the row, so
        // make sure the where condition is set
        if (action == Row.ACTION_UPDATE
            && row.getTable() == relMapping.getTable())
            row.wherePrimaryKey(rel);

        // update the inverse pointer with our oid value
        row.setForeignKey(fk, io, sm2);
    }

    public int supportsSelect(Select sel, int type, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch) {
        if (type == Select.TYPE_JOINLESS)
            return (field.getJoinDirection() != field.JOIN_INVERSE
                && sel.isSelected(field.getTable())) ? 1 : 0;
        if (type == Select.TYPE_TWO_PART)
            return 1;

        // already cached?
        if (sm != null) {
            Object oid = sm.getIntermediate(field.getIndex());
            if (store.getContext().findCached(oid, null) != null)
                return 0;
        }

        ClassMapping[] clss = field.getIndependentTypeMappings();
        switch (type) {
            case Select.EAGER_PARALLEL:
                return clss.length;
            case Select.EAGER_OUTER:
                return (clss.length == 1 && store.getDBDictionary().canOuterJoin
                    (sel.getJoinSyntax(), field.getForeignKey(clss[0]))) ? 1 :
                    0;
            case Select.EAGER_INNER:
                return (clss.length == 1) ? 1 : 0;
            default:
                return 0;
        }
    }

    public void selectEagerParallel(SelectExecutor sel,
        final OpenJPAStateManager sm, final JDBCStore store,
        final JDBCFetchConfiguration fetch, final int eagerMode) {
        final ClassMapping[] clss = field.getIndependentTypeMappings();
        if (!(sel instanceof Union))
            selectEagerParallel((Select) sel, clss[0], store, fetch, eagerMode);
        else {
            Union union = (Union) sel;
            if (fetch.getSubclassFetchMode (field.getTypeMapping()) 
                != JDBCFetchConfiguration.EAGER_JOIN)
                union.abortUnion();
            union.select(new Union.Selector() {
                public void select(Select sel, int idx) {
                    selectEagerParallel(sel, clss[idx], store, fetch,
                        eagerMode);
                }
            });
        }
    }

    /**
     * Perform an eager parallel select.
     */
    private void selectEagerParallel(Select sel, ClassMapping cls,
        JDBCStore store, JDBCFetchConfiguration fetch, int eagerMode) {
        sel.selectPrimaryKey(field.getDefiningMapping());
        // set a variable name that does not conflict with any in the query;
        // using a variable guarantees that the selected data will use different
        // aliases and joins than any existing WHERE conditions on this field
        // that might otherwise limit the relations that match
        Joins joins = sel.newJoins().setVariable("*");
        eagerJoin(joins, cls, true);
        sel.select(cls, field.getSelectSubclasses(), store, fetch, eagerMode, 
            joins);
    }

    public void selectEagerJoin(Select sel, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, int eagerMode) {
        // limit the eager mode to single on recursive eager fetching b/c
        // at this point the select has been modified and an attempt to
        // clone it for a to-many eager select can result in a clone that
        // produces invalid SQL
        ClassMapping cls = field.getIndependentTypeMappings()[0];
        sel.select(cls, field.getSelectSubclasses(), store, fetch,
            JDBCFetchConfiguration.EAGER_JOIN,
            eagerJoin(sel.newJoins(), cls, false));
    }

    /**
     * Add the joins needed to select/load eager data.
     */
    private Joins eagerJoin(Joins joins, ClassMapping cls, boolean forceInner) {
        boolean inverse = field.getJoinDirection() == field.JOIN_INVERSE;
        if (!inverse)
            joins = join(joins, false);

        // and join into relation
        ForeignKey fk = field.getForeignKey(cls);
        if (!forceInner && field.getNullValue() != FieldMapping.NULL_EXCEPTION)
            return joins.outerJoinRelation(field.getName(), fk, field.
                getTypeMapping(), field.getSelectSubclasses(), inverse, false);
        return joins.joinRelation(field.getName(), fk, field.getTypeMapping(), 
            field.getSelectSubclasses(), inverse, false);
    }

    public int select(Select sel, OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, int eagerMode) {
        if (field.getJoinDirection() == field.JOIN_INVERSE)
            return -1;
        // already cached oid?
        if (sm != null && sm.getIntermediate(field.getIndex()) != null)
            return -1;
        if (!Boolean.TRUE.equals(_fkOid))
            return -1;
        sel.select(field.getColumns(), field.join(sel));
        return 0;
    }

    public Object loadEagerParallel(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Object res)
        throws SQLException {
        // process batched results if we haven't already
        Map rels;
        if (res instanceof Result)
            rels = processEagerParallelResult(sm, store, fetch, (Result) res);
        else
            rels = (Map) res;

        // store object for this oid in instance
        sm.storeObject(field.getIndex(), rels.remove(sm.getObjectId()));
        return rels;
    }

    /**
     * Process the given batched result.
     */
    private Map processEagerParallelResult(OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
        // do same joins as for load
        //### cheat: we know typical result joins only care about the relation
        //### path; thus we can ignore different mappings
        ClassMapping[] clss = field.getIndependentTypeMappings();
        Joins joins = res.newJoins().setVariable("*");
        eagerJoin(joins, clss[0], true);

        Map rels = new HashMap();
        ClassMapping owner = field.getDefiningMapping();
        ClassMapping cls;
        Object oid;
        while (res.next()) {
            cls = res.getBaseMapping();
            if (cls == null)
                cls = clss[0];
            oid = owner.getObjectId(store, res, null, true, null);
            rels.put(oid, res.load(cls, store, fetch, joins));
        }
        res.close();

        return rels;
    }

    public void loadEagerJoin(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
        ClassMapping cls = field.getIndependentTypeMappings()[0];
        sm.storeObject(field.getIndex(), res.load(cls, store, fetch,
            eagerJoin(res.newJoins(), cls, false)));
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
        if (field.getJoinDirection() == field.JOIN_INVERSE)
            return;
        // cached oid?
        if (sm != null && sm.getIntermediate(field.getIndex()) != null)
            return;
        if (!Boolean.TRUE.equals(_fkOid))
            return;
        if (!res.containsAll(field.getColumns()))
            return;

        // get the related object's oid
        ClassMapping relMapping = field.getTypeMapping();
        Object oid = null;
        if (relMapping.isMapped()) {
            oid = relMapping.getObjectId(store, res, field.getForeignKey(),
                field.getPolymorphic() != ValueMapping.POLY_FALSE, null);
        } else {
            Column[] cols = field.getColumns();
            if (relMapping.getIdentityType() == ClassMapping.ID_DATASTORE) {
                long id = res.getLong(cols[0]);
                if (!res.wasNull())
                    oid = store.newDataStoreId(id, relMapping, true);
            } else { 
                // application id
                if (cols.length == 1) {
                    Object val = res.getObject(cols[0], null, null);
                    if (val != null)
                        oid = ApplicationIds.fromPKValues(new Object[]{ val },
                            relMapping);
                } else {
                    Object[] vals = new Object[cols.length];
                    for (int i = 0; i < cols.length; i++) {
                        vals[i] = res.getObject(cols[i], null, null);
                        if (vals[i] == null)
                            break;
                        if (i == cols.length - 1)
                            oid = ApplicationIds.fromPKValues(vals, relMapping);
                    }
                }
            }
        }

        if (oid == null)
            sm.storeObject(field.getIndex(), null);
        else
            sm.setIntermediate(field.getIndex(), oid);
    }

    public void load(final OpenJPAStateManager sm, final JDBCStore store,
        final JDBCFetchConfiguration fetch)
        throws SQLException {
        // check for cached oid value, or load oid if no way to join
        if (Boolean.TRUE.equals(_fkOid)) {
            Object oid = sm.getIntermediate(field.getIndex());
            if (oid != null) {
                Object val = store.find(oid, field, fetch);
                sm.storeObject(field.getIndex(), val);
                return;
            }
        }

        final ClassMapping[] rels = field.getIndependentTypeMappings();
        final int subs = field.getSelectSubclasses();
        final Joins[] resJoins = new Joins[rels.length];

        // select related mapping columns; joining from the related type
        // back to our fk table if not an inverse mapping (in which case we
        // can just make sure the inverse cols == our pk values)
        Union union = store.getSQLFactory().newUnion(rels.length);
        union.setSingleResult(true);
        if (fetch.getSubclassFetchMode(field.getTypeMapping())
            != JDBCFetchConfiguration.EAGER_JOIN)
            union.abortUnion();
        union.select(new Union.Selector() {
            public void select(Select sel, int idx) {
                if (field.getJoinDirection() == field.JOIN_INVERSE)
                    sel.whereForeignKey(field.getForeignKey(rels[idx]),
                        sm.getObjectId(), field.getDefiningMapping(), store);
                else {
                    resJoins[idx] = sel.newJoins().joinRelation(field.getName(),
                        field.getForeignKey(rels[idx]), rels[idx],
                        field.getSelectSubclasses(), false, false);
                    field.wherePrimaryKey(sel, sm, store);
                }
                sel.select(rels[idx], subs, store, fetch, fetch.EAGER_JOIN, 
                    resJoins[idx]);
            }
        });

        Result res = union.execute(store, fetch);
        try {
            Object val = null;
            if (res.next())
                val = res.load(rels[res.indexOf()], store, fetch,
                    resJoins[res.indexOf()]);
            sm.storeObject(field.getIndex(), val);
        } finally {
            res.close();
        }
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return RelationStrategies.toDataStoreValue(field, val, store);
    }

    public void appendIsNull(SQLBuffer sql, Select sel, Joins joins) {
        // if no inverse, just join to mapping's table (usually a no-op
        // because it'll be in the primary table) and see if fk cols are null;
        // if inverse, then we have to do a sub-select to see if any inverse
        // objects point back to this field's owner
        if (field.getJoinDirection() != field.JOIN_INVERSE) {
            //### probably need some sort of subselect here on fk constants
            joins = join(joins, false);
            Column[] cols = field.getColumns();
            if (cols.length == 0)
                sql.append("1 <> 1");
            else
                sql.append(sel.getColumnAlias(cols[0], joins)).
                    append(" IS ").appendValue(null, cols[0]);
        } else
            testInverseNull(sql, sel, joins, true);
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel, Joins joins) {
        // if no inverse, just join to mapping's table (usually a no-op
        // because it'll be in the primary table) and see if fk cols aren't
        // null; if inverse, then we have to do a sub-select to see if any
        // inverse objects point back to this field's owner
        if (field.getJoinDirection() != field.JOIN_INVERSE) {
            //### probably need some sort of subselect here on fk constants
            joins = join(joins, false);
            Column[] cols = field.getColumns();
            if (cols.length == 0)
                sql.append("1 = 1");
            else
                sql.append(sel.getColumnAlias(cols[0], joins)).
                    append(" IS NOT ").appendValue(null, cols[0]);
        } else
            testInverseNull(sql, sel, joins, false);
    }

    /**
     * Append SQL for a sub-select testing whether an inverse object exists
     * for this relation.
     */
    private void testInverseNull(SQLBuffer sql, Select sel, Joins joins,
        boolean empty) {
        DBDictionary dict = field.getMappingRepository().getDBDictionary();
        dict.assertSupport(dict.supportsSubselect, "SupportsSubselect");

        if (field.getIndependentTypeMappings().length != 1)
            throw RelationStrategies.uninversable(field);

        if (empty)
            sql.append("0 = ");
        else
            sql.append("0 < ");

        ForeignKey fk = field.getForeignKey();
        ContainerFieldStrategy.appendJoinCount(sql, sel, joins, dict, field,
            fk);
    }

    public Joins join(Joins joins, boolean forceOuter) {
        // if we're not in an inverse object table join normally, otherwise
        // already traversed the relation; just join back to owner table
        if (field.getJoinDirection() != field.JOIN_INVERSE)
            return field.join(joins, forceOuter, false);
        ClassMapping[] clss = field.getIndependentTypeMappings();
        if (clss.length != 1)
            throw RelationStrategies.uninversable(field);
        if (forceOuter)
            return joins.outerJoinRelation(field.getName(),
                field.getForeignKey(), clss[0], field.getSelectSubclasses(), 
                true, false);
        return joins.joinRelation(field.getName(), field.getForeignKey(),
            clss[0], field.getSelectSubclasses(), true, false);
    }

    public Joins joinRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        // if this is an inverse mapping it's already joined to the relation
        if (field.getJoinDirection() == field.JOIN_INVERSE)
            return joins;
        ClassMapping[] clss = field.getIndependentTypeMappings();
        if (clss.length != 1) {
            if (traverse)
                throw RelationStrategies.unjoinable(field);
            return joins;
        }
        if (forceOuter)
            return joins.outerJoinRelation(field.getName(), 
                field.getForeignKey(clss[0]), clss[0], 
                field.getSelectSubclasses(), false, false);
        return joins.joinRelation(field.getName(), field.getForeignKey(clss[0]),
            clss[0], field.getSelectSubclasses(), false, false);
    }

    /////////////////////////////
    // Embeddable implementation
    /////////////////////////////

    public Column[] getColumns() {
        return field.getColumns();
    }

    public ColumnIO getColumnIO() {
        return field.getColumnIO();
    }

    public Object[] getResultArguments() {
        return null;
    }

    public Object toEmbeddedDataStoreValue(Object val, JDBCStore store) {
        return toDataStoreValue(val, store);
    }

    public Object toEmbeddedObjectValue(Object val) {
        return UNSUPPORTED;
    }

    public void loadEmbedded(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Object val)
        throws SQLException {
        ClassMapping relMapping = field.getTypeMapping();
        Object oid;
        if (val == null)
            oid = null;
        else if (relMapping.getIdentityType() == ClassMapping.ID_DATASTORE)
            oid = store.newDataStoreId(((Number) val).longValue(), relMapping,
                field.getPolymorphic() != ValueMapping.POLY_FALSE);
        else {
            Object[] pks = (getColumns().length == 1) ? new Object[]{ val }
                : (Object[]) val;
            boolean nulls = true;
            for (int i = 0; nulls && i < pks.length; i++)
                nulls = pks[i] == null;
            if (nulls)
                oid = null;
            else {
                oid = ApplicationIds.fromPKValues(pks, relMapping);
                if (field.getPolymorphic() == ValueMapping.POLY_FALSE
                    && oid instanceof OpenJPAId) {
                    ((OpenJPAId) oid).setManagedInstanceType(relMapping.
                        getDescribedType());
                }
            }
        }

        if (oid == null)
            sm.storeObject(field.getIndex(), null);
        else
            sm.setIntermediate(field.getIndex(), oid);
    }
}

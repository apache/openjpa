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
package org.apache.openjpa.jdbc.meta.strats;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.ValueHandler;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.ValueMappingInfo;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.jdbc.sql.Union;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ChangeTracker;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.Proxies;
import org.apache.openjpa.util.Proxy;

/**
 * <p>Mapping for a map whose keys are relations to other persistent objects
 * and whose values are controlled by a {@link ValueHandler}.</p>
 *
 * @author Abe White
 * @since 0.4.0, 1.1.0
 */
public class RelationHandlerMapTableFieldStrategy
    extends MapTableFieldStrategy {

    
    private static final long serialVersionUID = 1L;

    private static final Localizer _loc = Localizer.forPackage
        (RelationHandlerMapTableFieldStrategy.class);

    private Column[] _vcols = null;
    private ColumnIO _vio = null;
    private boolean _vload = false;

    @Override
    public Column[] getKeyColumns(ClassMapping cls) {
        return field.getKeyMapping().getColumns();
    }

    @Override
    public Column[] getValueColumns(ClassMapping cls) {
        return _vcols;
    }

    @Override
    public void selectKey(Select sel, ClassMapping key, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, Joins joins) {
        sel.select(key, field.getKeyMapping().getSelectSubclasses(),
            store, fetch, EagerFetchModes.EAGER_NONE, joins);
    }

    @Override
    public void selectValue(Select sel, ClassMapping val,
        OpenJPAStateManager sm, JDBCStore store, JDBCFetchConfiguration fetch,
        Joins joins) {
        sel.select(_vcols, joins);
    }

    @Override
    public Result[] getResults(final OpenJPAStateManager sm,
        final JDBCStore store, final JDBCFetchConfiguration fetch,
        final int eagerMode, final Joins[] resJoins, boolean lrs)
        throws SQLException {
        ValueMapping key = field.getKeyMapping();
        final ClassMapping[] keys = key.getIndependentTypeMappings();
        Union union = store.getSQLFactory().newUnion(keys.length);
        if (fetch.getSubclassFetchMode(key.getTypeMapping())
            != EagerFetchModes.EAGER_JOIN)
            union.abortUnion();
        union.setLRS(lrs);
        union.select(new Union.Selector() {
            @Override
            public void select(Select sel, int idx) {
                sel.select(_vcols);
                sel.whereForeignKey(field.getJoinForeignKey(),
                    sm.getObjectId(), field.getDefiningMapping(), store);

                Joins joins = joinKeyRelation(sel.newJoins(), keys[idx]);
                sel.select(keys[idx], field.getKeyMapping().
                    getSelectSubclasses(), store, fetch, eagerMode, joins);

                //### cheat: result joins only care about the relation path;
                //### thus we can use first mapping of union only
                if (idx == 0)
                    resJoins[0] = joins;
            }
        });
        Result res = union.execute(store, fetch);
        return new Result[]{ res, res };
    }

    @Override
    public Object loadKey(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res, Joins joins)
        throws SQLException {
        ClassMapping key = res.getBaseMapping();
        if (key == null)
            key = field.getKeyMapping().getIndependentTypeMappings()[0];
        return res.load(key, store, fetch, joins);
    }

    @Override
    public Object loadValue(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res, Joins joins)
        throws SQLException {
        return HandlerStrategies.loadObject(field.getElementMapping(),
            sm, store, fetch, res, joins, _vcols, _vload);
    }

    @Override
    public Joins joinKeyRelation(Joins joins, ClassMapping key) {
        ValueMapping vm = field.getKeyMapping();
        return joins.joinRelation(field.getName(), vm.getForeignKey(key), key,
            vm.getSelectSubclasses(), false, false);
    }

    @Override
    public void map(boolean adapt) {
        super.map(adapt);

        ValueMapping key = field.getKeyMapping();
        if (key.getTypeCode() != JavaTypes.PC || key.isEmbeddedPC())
            throw new MetaDataException(_loc.get("not-relation", key));
        ValueMapping val = field.getElementMapping();
        if (val.getHandler() == null)
            throw new MetaDataException(_loc.get("no-handler", val));
        assertNotMappedBy();

        field.mapJoin(adapt, true);
        _vio = new ColumnIO();
        _vcols = HandlerStrategies.map(val, "value", _vio, adapt);

        if (key.getTypeMapping().isMapped()) {
            ValueMappingInfo vinfo = key.getValueInfo();
            ForeignKey fk = vinfo.getTypeJoin(key, "key", false, adapt);
            key.setForeignKey(fk);
            key.setColumnIO(vinfo.getColumnIO());
        } else
            RelationStrategies.mapRelationToUnmappedPC(key, "key", adapt);

        key.mapConstraints("key", adapt);
        field.mapPrimaryKey(adapt);
    }

    @Override
    public void initialize() {
        _vload = field.getElementMapping().getHandler().
            objectValueRequiresLoad(field.getElementMapping());
    }

    @Override
    public void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        insert(sm, store, rm, (Map) sm.fetchObject(field.getIndex()));
    }

    private void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm,
        Map map)
        throws SQLException {
        if (map == null || map.isEmpty())
            return;

        Row row = rm.getSecondaryRow(field.getTable(), Row.ACTION_INSERT);
        row.setForeignKey(field.getJoinForeignKey(), field.getJoinColumnIO(),
            sm);

        ValueMapping val = field.getElementMapping();
        ValueMapping key = field.getKeyMapping();
        StoreContext ctx = store.getContext();
        OpenJPAStateManager keysm;
        Map.Entry entry;
        for (Object o : map.entrySet()) {
            entry = (Map.Entry) o;
            keysm = RelationStrategies.getStateManager(entry.getKey(), ctx);
            key.setForeignKey(row, keysm);
            HandlerStrategies.set(val, entry.getValue(), store, row, _vcols,
                    _vio, true);
            rm.flushSecondaryRow(row);
        }
    }

    @Override
    public void update(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        Map map = (Map) sm.fetchObject(field.getIndex());
        ChangeTracker ct = null;
        if (map instanceof Proxy) {
            Proxy proxy = (Proxy) map;
            if (Proxies.isOwner(proxy, sm, field.getIndex()))
                ct = proxy.getChangeTracker();
        }

        // if no fine-grained change tracking then just delete and reinsert
        if (ct == null || !ct.isTracking()) {
            delete(sm, store, rm);
            insert(sm, store, rm, map);
            return;
        }

        // delete the removes
        ValueMapping key = field.getKeyMapping();
        StoreContext ctx = store.getContext();
        Collection rem = ct.getRemoved();
        OpenJPAStateManager keysm;
        if (!rem.isEmpty()) {
            Row delRow = rm.getSecondaryRow(field.getTable(),
                Row.ACTION_DELETE);
            delRow.whereForeignKey(field.getJoinForeignKey(), sm);
            for (Object o : rem) {
                keysm = RelationStrategies.getStateManager(o, ctx);
                key.whereForeignKey(delRow, keysm);
                rm.flushSecondaryRow(delRow);
            }
        }

        // insert the adds
        ValueMapping val = field.getElementMapping();
        Collection add = ct.getAdded();
        Object mkey;
        if (!add.isEmpty()) {
            Row addRow = rm.getSecondaryRow(field.getTable(),
                Row.ACTION_INSERT);
            addRow.setForeignKey(field.getJoinForeignKey(),
                field.getJoinColumnIO(), sm);

            for (Object o : add) {
                mkey = o;
                keysm = RelationStrategies.getStateManager(mkey, ctx);
                key.setForeignKey(addRow, keysm);
                HandlerStrategies.set(val, map.get(mkey), store, addRow,
                        _vcols, _vio, true);
                rm.flushSecondaryRow(addRow);
            }
        }

        // update the changes
        Collection change = ct.getChanged();
        if (!change.isEmpty()) {
            Row changeRow = rm.getSecondaryRow(field.getTable(),
                Row.ACTION_UPDATE);
            changeRow.whereForeignKey(field.getJoinForeignKey(), sm);

            for (Object o : change) {
                mkey = o;
                keysm = RelationStrategies.getStateManager(mkey, ctx);
                key.whereForeignKey(changeRow, keysm);
                HandlerStrategies.set(val, map.get(mkey), store, changeRow,
                        _vcols, _vio, true);
                rm.flushSecondaryRow(changeRow);
            }
        }
    }

    @Override
    public Joins joinRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        if (traverse)
            HandlerStrategies.assertJoinable(field.getElementMapping());
        return joins;
    }

    @Override
    public Joins joinKeyRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        ValueMapping key = field.getKeyMapping();
        ClassMapping[] clss = key.getIndependentTypeMappings();
        if (clss.length != 1) {
            if (traverse)
                throw RelationStrategies.unjoinable(field.getKeyMapping());
            return joins;
        }
        if (forceOuter)
            return joins.outerJoinRelation(field.getName(),
                key.getForeignKey(clss[0]), clss[0], key.getSelectSubclasses(),
                false, false);
        return joins.joinRelation(field.getName(),
            key.getForeignKey(clss[0]), clss[0], key.getSelectSubclasses(),
            false, false);
    }

    @Override
    public Object toDataStoreValue(Object val, JDBCStore store) {
        return HandlerStrategies.toDataStoreValue(field.getElementMapping(),
            val, _vcols, store);
    }

    @Override
    public Object toKeyDataStoreValue(Object val, JDBCStore store) {
        return RelationStrategies.toDataStoreValue(field.getKeyMapping(), val,
            store);
    }
}

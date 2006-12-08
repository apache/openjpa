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
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.FieldStrategy;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.MetaDataException;

/**
 * Base class for map mappings. Handles managing the secondary table
 * used to hold map keys and values and loading. Subclasses must implement
 * abstract methods and insert/update behavior as well as overriding
 * {@link FieldStrategy#toDataStoreValue},
 * {@link FieldStrategy#toKeyDataStoreValue},
 * {@link FieldStrategy#joinRelation}, and
 * {@link FieldStrategy#joinKeyRelation} if necessary.
 *
 * @author Abe White
 */
public abstract class MapTableFieldStrategy
    extends ContainerFieldStrategy
    implements LRSMapFieldStrategy {

    private static final Localizer _loc = Localizer.forPackage
        (MapTableFieldStrategy.class);

    public FieldMapping getFieldMapping() {
        return field;
    }

    public ClassMapping[] getIndependentKeyMappings(boolean traverse) {
        return (traverse) ? field.getKeyMapping().getIndependentTypeMappings()
            : ClassMapping.EMPTY_MAPPINGS;
    }

    public ClassMapping[] getIndependentValueMappings(boolean traverse) {
        return (traverse) ? field.getElementMapping().
            getIndependentTypeMappings() : ClassMapping.EMPTY_MAPPINGS;
    }

    public ForeignKey getJoinForeignKey(ClassMapping cls) {
        return field.getJoinForeignKey();
    }

    public Object deriveKey(JDBCStore store, Object value) {
        return null;
    }

    public Object deriveValue(JDBCStore store, Object key) {
        return null;
    }

    /**
     * Invokes {@link FieldStrategy#joinKeyRelation} by default.
     */
    public Joins joinKeyRelation(Joins joins, ClassMapping key) {
        return joinKeyRelation(joins, false, false);
    }

    /**
     * Invokes {@link FieldStrategy#joinRelation} by default.
     */
    public Joins joinValueRelation(Joins joins, ClassMapping val) {
        return joinRelation(joins, false, false);
    }

    public void map(boolean adapt) {
        if (field.getTypeCode() != JavaTypes.MAP)
            throw new MetaDataException(_loc.get("not-map", field));
        if (field.getKey().getValueMappedBy() != null)
            throw new MetaDataException(_loc.get("mapped-by-key", field));
        field.getValueInfo().assertNoSchemaComponents(field, !adapt);
    }

    public void delete(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        Row row = rm.getAllRows(field.getTable(), Row.ACTION_DELETE);
        row.whereForeignKey(field.getJoinForeignKey(), sm);
        rm.flushAllRows(row);
    }

    public int supportsSelect(Select sel, int type, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch) {
        return 0;
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        if (field.isLRS()) {
            sm.storeObjectField(field.getIndex(), new LRSProxyMap(this));
            return;
        }

        // select all and load into a normal proxy
        Joins[] joins = new Joins[2];
        Result[] res = getResults(sm, store, fetch,
            JDBCFetchConfiguration.EAGER_PARALLEL, joins, false);
        try {
            Map map = (Map) sm.newProxy(field.getIndex());
            Object key, val;
            while (res[0].next()) {
                if (res[1] != res[0] && !res[1].next())
                    break;

                key = loadKey(sm, store, fetch, res[0], joins[0]);
                val = loadValue(sm, store, fetch, res[1], joins[1]);
                map.put(key, val);
            }
            sm.storeObject(field.getIndex(), map);
        } finally {
            res[0].close();
            if (res[1] != res[0])
                res[1].close();
        }
    }

    public Object loadKeyProjection(JDBCStore store,
        JDBCFetchConfiguration fetch, Result res, Joins joins)
        throws SQLException {
        return loadKey(null, store, fetch, res, joins);
    }

    public Object loadProjection(JDBCStore store, JDBCFetchConfiguration fetch,
        Result res, Joins joins)
        throws SQLException {
        return loadValue(null, store, fetch, res, joins);
    }

    public Joins join(Joins joins, boolean forceOuter) {
        return field.join(joins, forceOuter, true);
    }

    public Joins joinKey(Joins joins, boolean forceOuter) {
        return field.join(joins, forceOuter, true);
    }

    protected ForeignKey getJoinForeignKey() {
        return field.getJoinForeignKey();
    }

    protected ClassMapping[] getIndependentElementMappings(boolean traverse) {
        return ClassMapping.EMPTY_MAPPINGS;
    }
}

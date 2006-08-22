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
package org.apache.openjpa.jdbc.kernel.exps;

import java.sql.SQLException;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.strats.ContainerFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.LRSMapFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationStrategies;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Returns the value of the given map's key.
 *
 * @author Marc Prud'hommeaux
 */
class GetMapValue
    extends AbstractVal {
    private final Val _map;
    private final Val _key;
    private Joins _joins = null;
    private ClassMetaData _meta = null;
    private Class _cast = null;

    /**
     * Constructor. Provide the map and key to operate on.
     */
    public GetMapValue(Val map, Val key) {
        _map = map;
        _key = key;
    }

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        if (_cast != null)
            return _cast;

        return _map.getType();
    }

    public void setImplicitType(Class type) {
        _cast = type;
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        _map.initialize(sel, store, false);
        _key.initialize(sel, store, false);
        _joins = sel.and(_map.getJoins(), _key.getJoins());
    }

    public Joins getJoins() {
        return _joins;
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return _map.toDataStoreValue(val, store);
    }


    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        sel.select(newSQLBuffer(sel, store, params, fetch), this);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _map.selectColumns(sel, store, params, true, fetch);
        _key.selectColumns(sel, store, params, true, fetch);
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
        sel.groupBy(newSQLBuffer(sel, store, params, fetch));
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
        sel.orderBy(newSQLBuffer(sel, store, params, fetch), asc, false);
    }

    private SQLBuffer newSQLBuffer(Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        calculateValue(sel, store, params, null, fetch);
        SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
        appendTo(buf, 0, sel, store, params, fetch);
        clearParameters();
        return buf;
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return Filters.convert(res.getObject(this,
            JavaSQLTypes.JDBC_DEFAULT, null), getType());
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        _map.calculateValue(sel, store, params, null, fetch);
        _key.calculateValue(sel, store, params, null, fetch);
    }

    public void clearParameters() {
        _map.clearParameters();
        _key.clearParameters();
    }

    public int length() {
        return 1;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (!(_map instanceof PCPath))
            throw new UnsupportedOperationException();

        if (!(_key instanceof Const))
            throw new UnsupportedOperationException();

        PCPath map = (PCPath) _map;
        Object key = ((Const) _key).getValue();

        FieldMapping field = map.getFieldMapping();

        if (!(field.getStrategy() instanceof LRSMapFieldStrategy))
            throw new UnsupportedOperationException();

        LRSMapFieldStrategy strat = (LRSMapFieldStrategy) field.getStrategy();

        ClassMapping[] clss = strat.getIndependentValueMappings(true);
        if (clss != null && clss.length > 1)
            throw RelationStrategies.unjoinable(field);

        ClassMapping cls = (clss.length == 0) ? null : clss[0];
        ForeignKey fk = strat.getJoinForeignKey(cls);
        DBDictionary dict = store.getDBDictionary();
        SQLBuffer sub = new SQLBuffer(dict);

        // manually create a subselect for the Map's value
        sub.append("(SELECT ");
        Column[] values = field.getElementMapping().getColumns();
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sub.append(", ");
            sub.append(values[i].getFullName());
        }

        sub.append(" FROM ").append(values[0].getTable().getFullName()).
            append(" WHERE ");

        // add in the joins
        ContainerFieldStrategy.appendUnaliasedJoin(sub, sel, null,
            dict, field, fk);

        sub.append(" AND ");

        key = strat.toKeyDataStoreValue(key, store);
        Column[] cols = strat.getKeyColumns(cls);
        Object[] vals = (cols.length == 1) ? null : (Object[]) key;

        for (int i = 0; i < cols.length; i++) {
            sub.append(cols[i].getFullName());

            if (vals == null)
                sub.append((key == null) ? " IS " : " = ").
                    appendValue(key, cols[i]);
            else
                sub.append((vals[i] == null) ? " IS " : " = ").
                    appendValue(vals[i], cols[i]);
        }

        sub.append(")");

        sql.append(sub);
    }
}

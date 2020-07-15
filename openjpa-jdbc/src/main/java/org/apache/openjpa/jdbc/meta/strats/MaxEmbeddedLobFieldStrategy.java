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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.ValueMappingInfo;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Base class for LOBs on databases that limit the max embedded LOB size.
 *
 * @author Abe White
 * @since 0.4.0
 */
abstract class MaxEmbeddedLobFieldStrategy
    extends HandlerFieldStrategy {

    
    private static final long serialVersionUID = 1L;

    /**
     * Return the expected type of the field from {@link JavaTypes} or
     * {@link JavaSQLTypes}.
     */
    protected abstract int getExpectedJavaType();

    /**
     * Set the value of the owning field into the given row.
     */
    protected abstract void update(OpenJPAStateManager sm, Row row)
        throws SQLException;

    /**
     * Return whether this is a custom insert/update.
     */
    protected abstract Boolean isCustom(OpenJPAStateManager sm,
        JDBCStore store);

    /**
     * Set the data from the given state manager into the result set.
     */
    protected abstract void putData(OpenJPAStateManager sm, ResultSet rs,
        DBDictionary dict)
        throws SQLException;

    @Override
    public void map(boolean adapt) {
        assertNotMappedBy();

        // map join key, if any
        field.mapJoin(adapt, false);
        field.getKeyMapping().getValueInfo().assertNoSchemaComponents
            (field.getKey(), !adapt);
        field.getElementMapping().getValueInfo().assertNoSchemaComponents
            (field.getElement(), !adapt);

        ValueMappingInfo vinfo = field.getValueInfo();
        vinfo.assertNoJoin(field, true);
        vinfo.assertNoForeignKey(field, !adapt);

        DBIdentifier fieldName = DBIdentifier.newColumn(field.getName(), false);

        // get value columns
        Column tmpCol = new Column();
        tmpCol.setIdentifier(fieldName);
        tmpCol.setJavaType(getExpectedJavaType());
        tmpCol.setSize(-1);
        _cols = vinfo.getColumns(field, fieldName,
            new Column[]{ tmpCol }, field.getTable(), adapt);
        _io = vinfo.getColumnIO();
        if (_io == null)
            _io = field.getColumnIO();
        field.setColumns(_cols);
        field.setColumnIO(_io);
        field.mapConstraints(fieldName, adapt);
        field.mapPrimaryKey(adapt);
    }

    @Override
    public Boolean isCustomInsert(OpenJPAStateManager sm, JDBCStore store) {
        if (!field.getColumnIO().isInsertable(0, false))
            return Boolean.FALSE;
        return isCustom(sm, store);
    }

    @Override
    public Boolean isCustomUpdate(OpenJPAStateManager sm, JDBCStore store) {
        if (!field.getColumnIO().isUpdatable(0, false))
            return Boolean.FALSE;
        return isCustom(sm, store);
    }

    @Override
    public void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        if (!field.getColumnIO().isInsertable(0, false))
            return;
        Row row = field.getRow(sm, store, rm, Row.ACTION_INSERT);
        if (row != null)
            update(sm, row);
    }

    @Override
    public void update(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        if (!field.getColumnIO().isUpdatable(0, false))
            return;
        Row row = field.getRow(sm, store, rm, Row.ACTION_UPDATE);
        if (row != null)
            update(sm, row);
    }

    @Override
    public void delete(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        field.deleteRow(sm, store, rm);
    }

    @Override
    public void customInsert(OpenJPAStateManager sm, JDBCStore store)
        throws SQLException {
        customUpdate(sm, store);
    }

    @Override
    public void customUpdate(OpenJPAStateManager sm, JDBCStore store)
        throws SQLException {
        JDBCFetchConfiguration fetch = store.getFetchConfiguration();
        // select existing value for update
        Column col = field.getColumns()[0];
        Select sel = store.getSQLFactory().newSelect();
        sel.select(col);
        field.wherePrimaryKey(sel, sm, store);
        SQLBuffer sql = sel.toSelect(true, fetch);

        Connection conn = store.getConnection();
        DBDictionary dict = store.getDBDictionary();
        PreparedStatement stmnt = null;
        ResultSet rs = null;
        try {
            stmnt = sql.prepareStatement(conn,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
            dict.setTimeouts(stmnt, fetch, true);
            rs = stmnt.executeQuery();
            rs.next();

            putData(sm, rs, store.getDBDictionary());
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException se) {
                }
            if (stmnt != null)
                try {
                    stmnt.close();
                } catch (SQLException se) {
                }
            try {
                conn.close();
            } catch (SQLException se) {
            }
        }
    }

    @Override
    public int supportsSelect(Select sel, int type, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch) {
        if (type == Select.TYPE_JOINLESS && sel.isSelected(field.getTable()))
            return 1;
        return 0;
    }

    @Override
    public int select(Select sel, OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, int eagerMode) {
        if (sel.isDistinct() ||
            eagerMode == EagerFetchModes.EAGER_NONE)
            return -1;
        sel.select(field.getColumns()[0], field.join(sel));
        return 1;
    }

    @Override
    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
        Column col = field.getColumns()[0];
        if (res.contains(col))
            sm.store(field.getIndex(), load(col, res, null));
    }

    @Override
    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        Column col = field.getColumns()[0];
        Select sel = store.getSQLFactory().newSelect();
        sel.select(col);
        field.wherePrimaryKey(sel, sm, store);

        Result res = sel.execute(store, fetch);
        Object val = null;
        try {
            if (res.next())
                val = load(col, res, null);
        } finally {
            res.close();
        }
        sm.store(field.getIndex(), val);
    }

    /**
     * Load this value from the given result.
     */
    protected Object load(Column col, Result res, Joins joins)
        throws SQLException {
        return res.getObject(col, null, joins);
    }

    @Override
    public Joins join(Joins joins, boolean forceOuter) {
        return field.join(joins, forceOuter, false);
    }

    @Override
    public Object loadProjection(JDBCStore store, JDBCFetchConfiguration fetch,
        Result res, Joins joins)
        throws SQLException {
        return load(field.getColumns()[0], res, joins);
    }

    @Override
    public boolean isVersionable() {
        return false;
    }

    @Override
    public void where(OpenJPAStateManager sm, JDBCStore store, RowManager rm,
        Object prevValue)
        throws SQLException {
    }

    protected abstract Object getValue(OpenJPAStateManager sm);
}

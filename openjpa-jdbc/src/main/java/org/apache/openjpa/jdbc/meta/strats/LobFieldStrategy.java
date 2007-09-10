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

import java.io.InputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.ValueMappingInfo;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Direct mapping from a stream value to a column.
 *
 * @author Ignacio Andreu
 * @since 1.1.0
 */
public class LobFieldStrategy extends AbstractFieldStrategy {

    private int fieldType;

    public void map(boolean adapt) {
        assertNotMappedBy();
        field.mapJoin(adapt, false);
        field.getKeyMapping().getValueInfo().assertNoSchemaComponents
            (field.getKey(), !adapt);
        field.getElementMapping().getValueInfo().assertNoSchemaComponents
            (field.getElement(), !adapt);
        field.setStream(true);
        ValueMappingInfo vinfo = field.getValueInfo();
        vinfo.assertNoJoin(field, true);
        vinfo.assertNoForeignKey(field, !adapt);
        Column tmpCol = new Column();
        tmpCol.setName(field.getName());
        tmpCol.setJavaType(field.getTypeCode());
        tmpCol.setType(fieldType);
        tmpCol.setSize(-1);

        Column[] cols = vinfo.getColumns(field, field.getName(),
            new Column[]{ tmpCol }, field.getTable(), adapt);

        field.setColumns(cols);
        field.setColumnIO(vinfo.getColumnIO());
        field.mapConstraints(field.getName(), adapt);
        field.mapPrimaryKey(adapt);
    }

    public Boolean isCustomInsert(OpenJPAStateManager sm, JDBCStore store) {
        return null;
    }

    public void insert(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        Object ob = toDataStoreValue(sm.fetchObjectField
            (field.getIndex()), store);
        Row row = field.getRow(sm, store, rm, Row.ACTION_INSERT);
        if (field.getColumnIO().isInsertable(0, ob == null)) {
            if (ob != null) {
                if (isBlob()) {
                    store.getDBDictionary().insertBlobForStreamingLoad
                        (row, field.getColumns()[0]);
                } else {
                    store.getDBDictionary().insertClobForStreamingLoad
                        (row, field.getColumns()[0]);
                }
            } else {
                Column col = field.getColumns()[0];
                col.setType(Types.OTHER);
                row.setNull(col);
            }
        }
    }

    public void customInsert(OpenJPAStateManager sm, JDBCStore store)
        throws SQLException {
        Object ob = toDataStoreValue(sm.fetchObjectField
            (field.getIndex()), store);
        if (field.getColumnIO().isInsertable(0, ob == null)) {
            if (ob != null) {
                Select sel = createSelect(sm, store);
                if (isBlob()) {
                    store.getDBDictionary().updateBlob
                        (sel, store, (InputStream)ob);
                } else {
                    store.getDBDictionary().updateClob
                        (sel, store, (Reader)ob);
                }
            }
        }
    }

    public void update(OpenJPAStateManager sm, JDBCStore store, RowManager rm)
        throws SQLException {
        Object ob = toDataStoreValue(sm.fetchObjectField
            (field.getIndex()), store);
        if (field.getColumnIO().isUpdatable(0, ob == null)) {
            if (ob != null) {
                Select sel = createSelect(sm, store);
                if (isBlob()) {
                    store.getDBDictionary().updateBlob
                        (sel, store, (InputStream)ob);
                } else {
                    store.getDBDictionary().updateClob
                        (sel, store, (Reader)ob);
                }
            } else {
                Row row = field.getRow(sm, store, rm, Row.ACTION_UPDATE);
                Column col = field.getColumns()[0];
                col.setType(Types.OTHER);
                row.setNull(col);
            }
        }
    }

    public int supportsSelect(Select sel, int type, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch) {
        if (type == Select.TYPE_JOINLESS && sel.isSelected(field.getTable()))
            return 1;
        return 0;
    }

    public int select(Select sel, OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, int eagerMode) {
        sel.select(field.getColumns()[0], field.join(sel));
        return 1;
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res) throws SQLException {
        Column col = field.getColumns()[0];
        if (res.contains(col)) {
            if (isBlob()) {
                sm.storeObject(field.getIndex(), res.getBinaryStream(col));
            } else {
                sm.storeObject(field.getIndex(), res.getCharacterStream(col));
            }
        }
    }

    protected void assertNotMappedBy() {
        if (field != null && field.getMappedBy() != null)
            throw new UnsupportedOperationException();
    }

    public void setFieldMapping(FieldMapping owner) {
        if (owner.getType().isAssignableFrom(InputStream.class)) {
            fieldType = Types.BLOB;
        } else if (owner.getType().isAssignableFrom(Reader.class)) {
            fieldType = Types.CLOB;
        }
        field = owner;
    }

    private boolean isBlob() {
        if (fieldType == Types.BLOB)
            return true;
        return false;
    }

    private Select createSelect(OpenJPAStateManager sm, JDBCStore store) {
        Select sel = store.getSQLFactory().newSelect();
        sel.select(field.getColumns()[0]);
        sel.selectPrimaryKey(field.getDefiningMapping());
        sel.wherePrimaryKey
            (sm.getObjectId(), field.getDefiningMapping(), store);
        sel.setLob(true);
        return sel;
    }
}

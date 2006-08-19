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
import org.apache.openjpa.jdbc.meta.Joinable;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.Id;
import org.apache.openjpa.util.OpenJPAId;
import org.apache.openjpa.util.UserException;
import serp.util.Numbers;

/**
 * Select the oid value of an object; typically used in projections.
 *
 * @author Abe White
 */
class GetObjectId
    extends AbstractVal {

    private static final Localizer _loc = Localizer.forPackage
        (GetObjectId.class);

    private final PCPath _path;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Provide the value whose oid to extract.
     */
    public GetObjectId(PCPath path) {
        _path = path;
    }

    public Column[] getColumns() {
        return _path.getClassMapping().getPrimaryKeyColumns();
    }

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public Class getType() {
        return Object.class;
    }

    public void setImplicitType(Class type) {
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        _path.initialize(sel, store, false);
        _path.joinRelation();

        // it's difficult to get calls on non-pc fields to always return null
        // without screwing up the SQL, to just don't let users call it on
        // non-pc fields at all
        if (_path.getClassMapping() == null
            || _path.getClassMapping().getEmbeddingMapping() != null)
            throw new UserException(_loc.get("bad-getobjectid",
                _path.getFieldMapping()));
    }

    public Joins getJoins() {
        return _path.getJoins();
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        // if datastore identity, try to convert to a long value
        ClassMapping mapping = _path.getClassMapping();
        if (mapping.getIdentityType() == mapping.ID_DATASTORE) {
            if (val instanceof Id)
                return Numbers.valueOf(((Id) val).getId());
            return Filters.convert(val, long.class);
        }

        // if unknown identity, can't do much
        if (mapping.getIdentityType() == mapping.ID_UNKNOWN)
            return (val instanceof OpenJPAId) ?
                ((OpenJPAId) val).getIdObject() : val;

        // application identity; convert to pk values in the same order as
        // the mapping's primary key columns will be returned
        Object[] pks = ApplicationIds.toPKValues(val, mapping);
        if (pks.length == 1)
            return pks[0];
        if (val == null)
            return pks;
        while (!mapping.isPrimaryKeyObjectId(false))
            mapping = mapping.getJoinablePCSuperclassMapping();

        // relies on single-column primary key field mappings
        Column[] cols = mapping.getPrimaryKeyColumns();
        Object[] ordered = new Object[cols.length];
        Joinable join;
        for (int i = 0; i < cols.length; i++) {
            join = mapping.assertJoinable(cols[i]);
            ordered[i] = pks[mapping.getField(join.getFieldIndex()).
                getPrimaryKeyIndex()];
        }
        return ordered;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        selectColumns(sel, store, params, true, fetch);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _path.selectColumns(sel, store, params, true, fetch);
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
        _path.groupBy(sel, store, params, fetch);
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
        _path.orderBy(sel, store, params, asc, fetch);
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return _path.load(res, store, true, fetch);
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        _path.calculateValue(sel, store, params, null, fetch);
    }

    public void clearParameters() {
        _path.clearParameters();
    }

    public int length() {
        return _path.length();
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        _path.appendTo(sql, index, sel, store, params, fetch);
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _path.acceptVisit(visitor);
        visitor.exit(this);
    }
}


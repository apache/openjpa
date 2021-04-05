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
package org.apache.openjpa.jdbc.kernel.exps;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.Constant;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A literal or parameter in the filter.
 *
 * @author Abe White
 */
abstract class Const
    extends AbstractVal
    implements Constant {

    
    private static final long serialVersionUID = 1L;
    private ClassMetaData _meta = null;

    @Override
    public ClassMetaData getMetaData() {
        return _meta;
    }

    @Override
    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    /**
     * Return the SQL value of this constant.
     */
    public Object getSQLValue(Select sel, ExpContext ctx, ExpState state) {
        return getValue(ctx, state);
    }

    /**
     * Return true if this constant's SQL value is equivalent to NULL.
     */
    public boolean isSQLValueNull(Select sel, ExpContext ctx, ExpState state) {
        Object val = getSQLValue(sel, ctx, state);
        if (val == null)
            return true;
        if (!(val instanceof Object[]))
            return false;

        // all-null array is considered null
        Object[] arr = (Object[]) val;
        for (Object o : arr)
            if (o != null)
                return false;
        return true;
    }

    /**
     * Return the value of this constant.  May be more robust than the
     * parameters-only form.
     */
    public Object getValue(ExpContext ctx, ExpState state) {
        return getValue(ctx.params);
    }

    @Override
    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        return new ConstExpState();
    }

    /**
     * Constant expression state.
     */
    protected static class ConstExpState
        extends ExpState {

        public Column[] cols = null;

        /**
         * Return the column for the value at the specified index, or null.
         */
        public Column getColumn(int index) {
            return (cols != null && cols.length > index) ? cols[index] : null;
        }
    }

    @Override
    public void calculateValue(Select sel, ExpContext ctx, ExpState state,
        Val other, ExpState otherState) {
        if (other instanceof PCPath)
            ((ConstExpState) state).cols = ((PCPath) other).
                getColumns(otherState);
    }

    @Override
    public void select(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
        sel.select(newSQLBuffer(sel, ctx, state), this);
    }

    private SQLBuffer newSQLBuffer(Select sel, ExpContext ctx, ExpState state) {
        calculateValue(sel, ctx, state, null, null);
        SQLBuffer buf = new SQLBuffer(ctx.store.getDBDictionary());
        appendTo(sel, ctx, state, buf, 0);
        return buf;
    }

    @Override
    public void selectColumns(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
    }

    @Override
    public void groupBy(Select sel, ExpContext ctx, ExpState state) {
        sel.groupBy(newSQLBuffer(sel, ctx, state));
    }

    @Override
    public void orderBy(Select sel, ExpContext ctx, ExpState state,
        boolean asc) {
        sel.orderBy(newSQLBuffer(sel, ctx, state), asc, false, getSelectAs());
    }

    @Override
    public Object load(ExpContext ctx, ExpState state, Result res)
        throws SQLException {
        return getValue(ctx, state);
    }

    @Override
    public int length(Select sel, ExpContext ctx, ExpState state) {
        return 1;
    }

    @Override
    public void appendIsEmpty(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        Object obj = getValue(ctx, state);
        if (obj instanceof Collection && ((Collection) obj).isEmpty())
            sql.append(TRUE);
        else if (obj instanceof Map && ((Map) obj).isEmpty())
            sql.append(TRUE);
        else
            sql.append(FALSE);
    }

    @Override
    public void appendIsNotEmpty(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql){
        Object obj = getValue(ctx, state);
        if (obj instanceof Collection && ((Collection) obj).isEmpty())
            sql.append(FALSE);
        else if (obj instanceof Map && ((Map) obj).isEmpty())
            sql.append(FALSE);
        else
            sql.append(TRUE);
    }

    @Override
    public void appendSize(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        Object obj = getValue(ctx, state);
        if (obj instanceof Collection)
            sql.appendValue(((Collection) obj).size());
        else if (obj instanceof Map)
            sql.appendValue(((Map) obj).size());
        else
            sql.append("1");
    }

    @Override
    public void appendIsNull(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        if (isSQLValueNull(sel, ctx, state))
            sql.append(TRUE);
        else
            sql.append(FALSE);
    }

    @Override
    public void appendIsNotNull(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        if (!isSQLValueNull(sel, ctx, state))
            sql.append(TRUE);
        else
            sql.append(FALSE);
    }
}

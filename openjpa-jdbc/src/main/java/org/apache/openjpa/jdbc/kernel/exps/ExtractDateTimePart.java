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

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.DateTimeExtractPart;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Returns the date or time part of a given temporal.
 *
 */
public class ExtractDateTimePart
    extends AbstractVal {

    private static final long serialVersionUID = 1L;
    private final Val _val;
    private final DateTimeExtractPart _part;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Provide the date and the field to operate on.
     */
    public ExtractDateTimePart(Val val, DateTimeExtractPart part) {
        _val = val;
        _part = part;
    }

    public Val getVal() {
        return _val;
    }

    @Override
    public ClassMetaData getMetaData() {
        return _meta;
    }

    @Override
    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    @Override
    public Class getType() {
        if (_part == DateTimeExtractPart.TIME) {
            return Time.class;
        } else if (_part == DateTimeExtractPart.DATE) {
            return Date.class;
        }
        throw new IllegalStateException();
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        ExpState valueState =  _val.initialize(sel, ctx, 0);
        return new ExtractDateTimePartExpState(valueState.joins, valueState);
    }

    /**
     * Expression state.
     */
    private static class ExtractDateTimePartExpState
        extends ExpState {

        public final ExpState valueState;

        public ExtractDateTimePartExpState(Joins joins, ExpState valueState) {
            super(joins);
            this.valueState = valueState;
        }
    }

    @Override
    public void select(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
        sel.select(newSQLBuffer(sel, ctx, state), this);
    }

    @Override
    public void selectColumns(Select sel, ExpContext ctx, ExpState state, boolean pks) {
        ExtractDateTimePartExpState edtstate = (ExtractDateTimePartExpState) state;
        _val.selectColumns(sel, ctx, edtstate.valueState, true);
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

    private SQLBuffer newSQLBuffer(Select sel, ExpContext ctx, ExpState state) {
        calculateValue(sel, ctx, state, null, null);
        SQLBuffer buf = new SQLBuffer(ctx.store.getDBDictionary());
        appendTo(sel, ctx, state, buf, 0);
        return buf;
    }

    @Override
    public Object load(ExpContext ctx, ExpState state, Result res)
        throws SQLException {
        return Filters.convert(res.getObject(this,
            JavaSQLTypes.JDBC_DEFAULT, null), getType());
    }

    @Override
    public void calculateValue(Select sel, ExpContext ctx, ExpState state,
        Val other, ExpState otherState) {
        ExtractDateTimePartExpState edtstate = (ExtractDateTimePartExpState) state;
        _val.calculateValue(sel, ctx, edtstate.valueState, null, null);
    }

    @Override
    public int length(Select sel, ExpContext ctx, ExpState state) {
        return 1;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql, int index) {
        ExtractDateTimePartExpState edtstate = (ExtractDateTimePartExpState) state;
        sql.append("CAST( ");
        _val.appendTo(sel, ctx, edtstate.valueState, sql, 0);
        sql.append(" AS ");
        sql.append(_part == DateTimeExtractPart.DATE ? "DATE)" : "TIME)");
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }

    @Override
    public int getId() {
        return Val.EXTRACTDTP_VAL;
    }
}


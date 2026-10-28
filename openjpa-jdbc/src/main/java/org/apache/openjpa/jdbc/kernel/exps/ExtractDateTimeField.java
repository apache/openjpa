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
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Returns the temporal field of a given date or time.
 *
 */
public class ExtractDateTimeField extends AbstractVal {
    private static final long serialVersionUID = 1L;
    private final Val _val;
    private final DateTimeExtractField _field;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Provide the date and the field to operate on.
     */
    public ExtractDateTimeField(Val val, DateTimeExtractField field) {
        _val = val;
        _field = field;
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
        return _field == DateTimeExtractField.SECOND ? float.class : int.class;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        ExpState valueState =  _val.initialize(sel, ctx, 0);
        return new ExtractDateTimeFieldExpState(valueState.joins, valueState);
    }

    /**
     * Expression state.
     */
    private static class ExtractDateTimeFieldExpState extends ExpState {
        public final ExpState valueState;

        public ExtractDateTimeFieldExpState(Joins joins, ExpState valueState) {
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
        ExtractDateTimeFieldExpState edtstate = (ExtractDateTimeFieldExpState) state;
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
        ExtractDateTimeFieldExpState edtstate = (ExtractDateTimeFieldExpState) state;
        _val.calculateValue(sel, ctx, edtstate.valueState, null, null);
    }

    @Override
    public int length(Select sel, ExpContext ctx, ExpState state) {
        return 1;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql, int index) {
        DBDictionary dict = ctx.store.getDBDictionary();
        String func = dict.getExtractDateTimeFieldFunction(_field);

        ExtractDateTimeFieldExpState edtstate = (ExtractDateTimeFieldExpState) state;
        String castType = getOperandCastType(ctx, edtstate.valueState, dict);
        int start = 0;
        for (int valPart = func.indexOf("{1}"); valPart != -1; valPart = func.indexOf("{1}", start)) {
            sql.append(func.substring(start, valPart));
            if (castType != null) {
                sql.append("CAST(");
            }
            _val.appendTo(sel, ctx, edtstate.valueState, sql, 0);
            if (castType != null) {
                sql.append(" AS ").append(castType).append(")");
            }
            start = valPart + 3;
        }
        sql.append(func.substring(start));
    }

    /**
     * Return the SQL type to cast the value to when it is bound as a parameter on a
     * database that cannot type such an operand by itself, or null for no cast.
     */
    private String getOperandCastType(ExpContext ctx, ExpState valueState, DBDictionary dict) {
        if (!dict.requiresCastForExtractOperand || !(_val instanceof Const constVal)
            || (_val instanceof Lit lit && lit.isRaw())) {
            return null;
        }
        Object value = constVal.getValue(ctx, valueState);
        if (value instanceof LocalDate || value instanceof java.sql.Date) {
            return dict.dateTypeName;
        }
        if (value instanceof LocalTime || value instanceof OffsetTime || value instanceof Time) {
            return dict.timeTypeName;
        }
        return dict.timestampTypeName;
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }

    @Override
    public int getId() {
        return Val.EXTRACTDTF_VAL;
    }
}


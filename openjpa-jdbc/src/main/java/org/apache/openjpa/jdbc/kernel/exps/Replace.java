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

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Take a string and replaces part of it with a replacement string.
 *
 * @author Abe White
 * @author Paulo Cristovão Filho
 */
public class Replace
    extends AbstractVal {

    
    private static final long serialVersionUID = 1L;
    private final Val _orig;
    private final Val _subs;
    private final Val _repl;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Provide the strings to operate on.
     */
    public Replace(Val orig, Val pattern, Val replacement) {
        _orig = orig;
        _subs = pattern;
        _repl = replacement;
    }

    @Override
    public ClassMetaData getMetaData() {
        return _meta;
    }

    @Override
    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Class getType() {
        return String.class;
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void setImplicitType(Class type) {
    }

    @Override
    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        ExpState s1 = _orig.initialize(sel, ctx, 0);
        ExpState s2 = _subs.initialize(sel, ctx, 0);
        ExpState s3 = _repl.initialize(sel, ctx, 0);
        return new ReplaceExpState(sel.and(s1.joins, sel.and(s2.joins, s3.joins)), s1, s2, s3);
    }

    @Override
    public void select(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
        sel.select(newSQLBuffer(sel, ctx, state), this);
    }

    @Override
    public void selectColumns(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
        ReplaceExpState rstate = (ReplaceExpState) state;
        _orig.selectColumns(sel, ctx, rstate.orig, true);
        _subs.selectColumns(sel, ctx, rstate.subs, true);
        _repl.selectColumns(sel, ctx, rstate.repl, true);
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
        ReplaceExpState rstate = (ReplaceExpState) state;
        _orig.calculateValue(sel, ctx, rstate.orig, null, null);
        _subs.calculateValue(sel, ctx, rstate.subs, null, null);
        _repl.calculateValue(sel, ctx, rstate.repl, null, null);
    }

    @Override
    public int length(Select sel, ExpContext ctx, ExpState state) {
        return 1;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql, int index) {
    	ReplaceExpState rstate = (ReplaceExpState) state;
        FilterValue str = new FilterValueImpl(sel, ctx, rstate.orig, _orig);
        FilterValue pat = new FilterValueImpl(sel, ctx, rstate.subs, _subs);
        FilterValue repl = new FilterValueImpl(sel, ctx, rstate.repl, _repl);
        ctx.store.getDBDictionary().replace(sql, str, pat, repl);
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _orig.acceptVisit(visitor);
        _subs.acceptVisit(visitor);
        _repl.acceptVisit(visitor);
        visitor.exit(this);
    }

    @Override
    public int getId() {
        return Val.SUBSTRING_VAL;
    }
    
    private static class ReplaceExpState extends ExpState {
    	private final ExpState orig;
    	private final ExpState subs;
    	private final ExpState repl;
    	
    	public ReplaceExpState(Joins joins, ExpState orig, ExpState subs, ExpState repl) {
    		super(joins);
    		this.orig = orig;
    		this.subs = subs;
    		this.repl = repl;
    	}
    }
}


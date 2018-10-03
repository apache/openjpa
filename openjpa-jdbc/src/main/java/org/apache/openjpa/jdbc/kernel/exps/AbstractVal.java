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

import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.Value;

/**
 * Abstract value for easy extension.
 *
 * @author Marc Prud'hommeaux
 */
abstract class AbstractVal
    implements Val {

    
    private static final long serialVersionUID = 1L;
    protected static final String TRUE = "1 = 1";
    protected static final String FALSE = "1 <> 1";
    private String _alias = null;

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public boolean isAggregate() {
        return false;
    }

    @Override
    public boolean isXPath() {
        return false;
    }

    @Override
    public Object toDataStoreValue(Select sel, ExpContext ctx, ExpState state,
        Object val) {
        return val;
    }

    @Override
    public void appendIsEmpty(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        sql.append(FALSE);
    }

    @Override
    public void appendIsNotEmpty(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql){
        sql.append(TRUE);
    }

    @Override
    public void appendIsNull(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        appendTo(sel, ctx, state, sql, 0);
        sql.append(" IS ").appendValue(null);
    }

    @Override
    public void appendIsNotNull(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        appendTo(sel, ctx, state, sql, 0);
        sql.append(" IS NOT ").appendValue(null);
    }

    @Override
    public void appendIndex(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        sql.append("1");
    }

    @Override
    public void appendType(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        sql.append("1");
    }

    @Override
    public void appendSize(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql) {
        sql.append("1");
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        visitor.exit(this);
    }

    @Override
    public int getId() {
        return Val.VAL;
    }

    @Override
    public String getAlias() {
        return _alias;
    }

    @Override
    public void setAlias(String alias) {
        _alias = alias;
    }

    @Override
    public Value getSelectAs() {
        return _alias != null ? this : null;
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}


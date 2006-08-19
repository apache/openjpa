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

import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;

/**
 * Tests whether a value is IN a subquery.
 *
 * @author Abe White
 */
class InSubQExpression
    implements Exp {

    private final Val _val;
    private final SubQ _sub;

    /**
     * Constructor. Supply the value to test and the subquery.
     */
    public InSubQExpression(Val val, SubQ sub) {
        _val = val;
        _sub = sub;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _val.initialize(sel, store, false);
        _sub.initialize(sel, store, false);
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        _val.calculateValue(sel, store, params, null, fetch);
        _sub.calculateValue(sel, store, params, null, fetch);
        _val.appendTo(buf, 0, sel, store, params, fetch);
        buf.append(" IN ");
        _sub.appendTo(buf, 0, sel, store, params, fetch);
        _val.clearParameters();
        _sub.clearParameters();
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _val.selectColumns(sel, store, params, true, fetch);
        _sub.selectColumns(sel, store, params, pks, fetch);
    }

    public Joins getJoins() {
        return _val.getJoins();
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        _sub.acceptVisit(visitor);
        visitor.exit(this);
    }
}

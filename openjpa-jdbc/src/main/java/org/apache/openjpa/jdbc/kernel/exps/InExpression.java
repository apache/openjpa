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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;

/**
 * Tests whether a value is IN a collection.
 *
 * @author Abe White
 */
class InExpression
    implements Exp {

    private final Val _val;
    private final Const _const;

    /**
     * Constructor. Supply the value to test and the constant to obtain
     * the parameters from.
     */
    public InExpression(Val val, Const constant) {
        _val = val;
        _const = constant;
    }

    public Const getConst() {
        return _const;
    }

    public Val getValue() {
        return _val;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        _val.initialize(sel, store, false);
        _const.initialize(sel, store, false);
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        _val.calculateValue(sel, store, params, null, fetch);
        _const.calculateValue(sel, store, params, null, fetch);

        Collection coll = getCollection();
        if (coll != null) {
            Collection ds = new ArrayList(coll.size());
            for (Iterator itr = coll.iterator(); itr.hasNext();)
                ds.add(_val.toDataStoreValue(itr.next(), store));
            coll = ds;
        }

        Column[] cols = null;
        if (_val instanceof PCPath)
            cols = ((PCPath) _val).getColumns();
        else if (_val instanceof GetObjectId)
            cols = ((GetObjectId) _val).getColumns();

        if (coll == null || coll.isEmpty())
            buf.append("1 <> 1");
        else if (_val.length() == 1)
            inContains(buf, sel, store, params, fetch, coll, cols);
        else
            orContains(buf, sel, store, params, fetch, coll, cols);
        sel.append(buf, _val.getJoins());

        _val.clearParameters();
        _const.clearParameters();
    }

    /**
     * Construct an IN clause with the value of the given collection.
     */
    private void inContains(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch,
        Collection coll, Column[] cols) {
        _val.appendTo(buf, 0, sel, store, params, fetch);
        buf.append(" IN (");

        Column col = (cols != null && cols.length == 1) ? cols[0] : null;
        for (Iterator itr = coll.iterator(); itr.hasNext();) {
            buf.appendValue(itr.next(), col);
            if (itr.hasNext())
                buf.append(", ");
        }
        buf.append(")");
    }

    /**
     * If the value to test is a compound key, we can't use IN,
     * so create a clause like '(a = b AND c = d) OR (e = f AND g = h) ...'
     */
    private void orContains(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch, Collection coll,
        Column[] cols) {
        if (coll.size() > 1)
            buf.append("(");

        Object[] vals;
        Column col;
        for (Iterator itr = coll.iterator(); itr.hasNext();) {
            vals = (Object[]) itr.next();

            buf.append("(");
            for (int i = 0; i < vals.length; i++) {
                col = (cols != null && cols.length == vals.length)
                    ? cols[i] : null;
                if (i > 0)
                    buf.append(" AND ");

                _val.appendTo(buf, i, sel, store, params, fetch);
                if (vals[i] == null)
                    buf.append(" IS ");
                else
                    buf.append(" = ");
                buf.appendValue(vals[i], col);
            }
            buf.append(")");

            if (itr.hasNext())
                buf.append(" OR ");
        }
        if (coll.size() > 1)
            buf.append(")");
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _val.selectColumns(sel, store, params, true, fetch);
        _const.selectColumns(sel, store, params, pks, fetch);
    }

    public Joins getJoins() {
        return _val.getJoins();
    }

    /**
     * Return the collection to test for containment with.
     */
    protected Collection getCollection() {
        return (Collection) _const.getValue();
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        _const.acceptVisit(visitor);
        visitor.exit(this);
    }
}

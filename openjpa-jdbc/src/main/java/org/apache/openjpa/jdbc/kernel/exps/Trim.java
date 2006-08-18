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

import java.lang.Math;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Returns the number of characters in a string.
 *
 * @author Marc Prud'hommeaux
 */
class Trim
    extends AbstractVal
    implements Val {

    private final Val _val;
    private final Val _trimChar;
    private final Boolean _where;
    private ClassMetaData _meta = null;
    private String _func = null;

    /**
     * Constructor. Provide the string to operate on.
     */
    public Trim(Val val, Val trimChar, Boolean where) {
        _val = val;
        _trimChar = trimChar;
        _where = where;
    }

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        return String.class;
    }

    public void setImplicitType(Class type) {
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        _val.initialize(sel, store, false);

        DBDictionary dict = store.getDBDictionary();
        if (_where == null) {
            _func = dict.trimBothFunction;
            dict.assertSupport(_func != null, "TrimBothFunction");
        } else if (_where.equals(Boolean.TRUE)) {
            _func = dict.trimLeadingFunction;
            dict.assertSupport(_func != null, "TrimLeadingFunction");
        } else if (_where.equals(Boolean.FALSE)) {
            _func = dict.trimTrailingFunction;
            dict.assertSupport(_func != null, "TrimTrailingFunction");
        }
    }

    public Joins getJoins() {
        return _val.getJoins();
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        sel.select(newSQLBuffer(sel, store, params, fetch), this);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        _val.selectColumns(sel, store, params, true, fetch);
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
        sel.groupBy(newSQLBuffer(sel, store, params, fetch));
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
        sel.orderBy(newSQLBuffer(sel, store, params, fetch), asc, false);
    }

    private SQLBuffer newSQLBuffer(Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        calculateValue(sel, store, params, null, fetch);
        SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
        appendTo(buf, 0, sel, store, params, fetch);
        clearParameters();
        return buf;
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return Filters.convert(res.getObject(this,
            JavaSQLTypes.JDBC_DEFAULT, null), getType());
    }

    public boolean hasVariable(Variable var) {
        return _val.hasVariable(var);
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        _val.calculateValue(sel, store, params, null, fetch);
    }

    public void clearParameters() {
        _val.clearParameters();
    }

    public int length() {
        return 1;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        _val.calculateValue(sel, store, params, _trimChar, fetch);
        _trimChar.calculateValue(sel, store, params, _val, fetch);

        int fromPart = _func.indexOf("{0}");
        int charPart = _func.indexOf("{1}");

        if (charPart == -1)
            charPart = _func.length();

        String part1 = _func.substring(0, Math.min(fromPart, charPart));

        String part2 = _func.substring(Math.min(fromPart, charPart) + 3,
            Math.max(fromPart, charPart));

        String part3 = null;
        if (charPart != _func.length())
            part3 = _func.substring(Math.max(fromPart, charPart) + 3);

        sql.append(part1);
        (fromPart < charPart ? _val : _trimChar).
            appendTo(sql, 0, sel, store, params, fetch);
        sql.append(part2);

        if (charPart != _func.length()) {
            (fromPart > charPart ? _val : _trimChar).
                appendTo(sql, 0, sel, store, params, fetch);
            sql.append(part3);
        } else {
            // since the trim statement did not specify the token for
            // where to specify the trim char (denoted by "{1}"),
            // we do not have the ability to trim off non-whitespace
            // characters; throw an exception when we attempt to do so
            if (!(_trimChar instanceof Literal)
                || String.valueOf(((Literal) _trimChar).getValue()).
                trim().length() != 0) {
                store.getDBDictionary().assertSupport(false,
                    "TrimNonWhitespaceCharacters");
            }
        }
    }
}


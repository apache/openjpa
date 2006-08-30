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
package org.apache.openjpa.jdbc.sql;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import serp.util.Numbers;

/**
 * Buffer for SQL statements that can be used to create
 * java.sql.PreparedStatements.
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @since 0.2.4
 */
public final class SQLBuffer
    implements Serializable, Cloneable {

    private static final String PARAMETER_TOKEN = "?";

    private final DBDictionary _dict;
    private final StringBuffer _sql = new StringBuffer();
    private List _subsels = null;
    private List _params = null;
    private List _cols = null;

    /**
     * Default constructor.
     */
    public SQLBuffer(DBDictionary dict) {
        _dict = dict;
    }

    /**
     * Copy constructor.
     */
    public SQLBuffer(SQLBuffer buf) {
        _dict = buf._dict;
        append(buf);
    }

    /**
     * Perform a shallow clone of this SQL Buffer.
     */
    public Object clone() {
        return new SQLBuffer(this);
    }

    /**
     * Return true if the buffer is emtpy.
     */
    public boolean isEmpty() {
        return _sql.length() == 0;
    }

    /**
     * Append all SQL and parameters of the given buffer.
     */
    public SQLBuffer append(SQLBuffer buf) {
        append(buf, _sql.length(), (_params == null) ? 0 : _params.size(),
            true);
        return this;
    }

    /**
     * Append all SQL and parameters of the given buffer at the given positions.
     */
    private void append(SQLBuffer buf, int sqlIndex, int paramIndex,
        boolean subsels) {
        if (subsels) {
            // only allow appending of buffers with subselects, not insertion
            if (_subsels != null && !_subsels.isEmpty()
                && sqlIndex != _sql.length())
                throw new IllegalStateException();
            if (buf._subsels != null && !buf._subsels.isEmpty()) {
                if (sqlIndex != _sql.length())
                    throw new IllegalStateException();
                if (_subsels == null)
                    _subsels = new ArrayList(buf._subsels.size());
                for (int i = 0; i < buf._subsels.size(); i++)
                    _subsels.add(((Subselect) buf._subsels.get(i)).
                        clone(sqlIndex, paramIndex));
            }
        }

        if (sqlIndex == _sql.length())
            _sql.append(buf._sql.toString());
        else
            _sql.insert(sqlIndex, buf._sql.toString());

        if (buf._params != null) {
            if (_params == null)
                _params = new ArrayList();
            if (_cols == null && buf._cols != null) {
                _cols = new ArrayList();
                while (_cols.size() < _params.size())
                    _cols.add(null);
            }

            if (paramIndex == _params.size()) {
                _params.addAll(buf._params);
                if (buf._cols != null)
                    _cols.addAll(buf._cols);
                else if (_cols != null)
                    while (_cols.size() < _params.size())
                        _cols.add(null);
            } else {
                _params.addAll(paramIndex, buf._params);
                if (buf._cols != null)
                    _cols.addAll(paramIndex, buf._cols);
                else if (_cols != null)
                    while (_cols.size() < _params.size())
                        _cols.add(paramIndex, null);
            }
        }
    }

    public SQLBuffer append(Table table) {
        _sql.append(_dict.getFullName(table, false));
        return this;
    }

    public SQLBuffer append(Sequence seq) {
        _sql.append(_dict.getFullName(seq));
        return this;
    }

    public SQLBuffer append(Column col) {
        _sql.append(col.getName());
        return this;
    }

    public SQLBuffer append(String s) {
        _sql.append(s);
        return this;
    }

    /**
     * Append a subselect. This delays resolution of the select SQL.
     */
    public SQLBuffer append(Select sel, JDBCFetchConfiguration fetch) {
        return append(sel, fetch, false);
    }

    /**
     * Append a subselect count. This delays resolution of the select SQL.
     */
    public SQLBuffer appendCount(Select sel, JDBCFetchConfiguration fetch) {
        return append(sel, fetch, true);
    }

    /**
     * Append a subselect. This delays resolution of the select SQL.
     */
    private SQLBuffer append(Select sel, JDBCFetchConfiguration fetch,
        boolean count) {
        _sql.append("(");
        Subselect sub = new Subselect();
        sub.select = sel;
        sub.fetch = fetch;
        sub.count = count;
        sub.sqlIndex = _sql.length();
        sub.paramIndex = (_params == null) ? 0 : _params.size();
        _sql.append(")");

        if (_subsels == null)
            _subsels = new ArrayList(2);
        _subsels.add(sub);
        return this;
    }

    /**
     * Replace a subselect.
     */
    public boolean replace(Select old, Select sel) {
        if (_subsels == null)
            return false;
        Subselect sub;
        for (int i = 0; i < _subsels.size(); i++) {
            sub = (Subselect) _subsels.get(i);
            if (sub.select == old) {
                sub.select = sel;
                return true;
            }
        }
        return false;
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(Object o) {
        return appendValue(o, null);
    }

    /**
     * Append a parameter value for a specific column.
     */
    public SQLBuffer appendValue(Object o, Column col) {
        if (o == null)
            _sql.append("NULL");
        else if (o instanceof Raw)
            _sql.append(o.toString());
        else {
            _sql.append(PARAMETER_TOKEN);

            // initialize param and col lists; we hold off on col list until
            // we get the first non-null col
            if (_params == null)
                _params = new ArrayList();
            if (col != null && _cols == null) {
                _cols = new ArrayList();
                while (_cols.size() < _params.size())
                    _cols.add(null);
            }

            _params.add(o);
            if (_cols != null)
                _cols.add(col);
        }
        return this;
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(boolean b) {
        return appendValue(b, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(boolean b, Column col) {
        return appendValue((b) ? Boolean.TRUE : Boolean.FALSE, col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(byte b) {
        return appendValue(b, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(byte b, Column col) {
        return appendValue(new Byte(b), col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(char c) {
        return appendValue(c, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(char c, Column col) {
        return appendValue(new Character(c), col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(double d) {
        return appendValue(d, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(double d, Column col) {
        return appendValue(new Double(d), col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(float f) {
        return appendValue(f, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(float f, Column col) {
        return appendValue(new Float(f), col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(int i) {
        return appendValue(i, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(int i, Column col) {
        return appendValue(Numbers.valueOf(i), col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(long l) {
        return appendValue(l, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(long l, Column col) {
        return appendValue(Numbers.valueOf(l), col);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(short s) {
        return appendValue(s, null);
    }

    /**
     * Append a parameter value.
     */
    public SQLBuffer appendValue(short s, Column col) {
        return appendValue(new Short(s), col);
    }

    /**
     * Return the list of parameter values.
     */
    public List getParameters() {
        return (_params == null) ? Collections.EMPTY_LIST : _params;
    }

    /**
     * Return the SQL for this buffer.
     */
    public String getSQL() {
        return getSQL(false);
    }

    /**
     * Returns the SQL for this buffer.
     *
     * @param replaceParams if true, then replace parameters with the
     * actual parameter values
     */
    public String getSQL(boolean replaceParams) {
        if (_subsels != null && !_subsels.isEmpty()) {
            // add subsels backwards so that the stored insertion points of
            // later subsels remain valid
            Subselect sub;
            SQLBuffer buf;
            for (int i = _subsels.size() - 1; i >= 0; i--) {
                sub = (Subselect) _subsels.get(i);
                if (sub.count)
                    buf = sub.select.toSelectCount();
                else
                    buf = sub.select.toSelect(false, sub.fetch);
                append(buf, sub.sqlIndex, sub.paramIndex, false);
            }
            _subsels.clear();
        }

        String sql = _sql.toString();
        if (!replaceParams || _params == null || _params.isEmpty())
            return sql;

        StringBuffer buf = new StringBuffer();
        Iterator pi = _params.iterator();
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) != '?') {
                buf.append(sql.charAt(i));
                continue;
            }

            Object param = pi.hasNext() ? pi.next() : null;
            if (param == null)
                buf.append("NULL");
            else if (param instanceof Number || param instanceof Boolean)
                buf.append(param);
            else if (param instanceof String || param instanceof Character)
                buf.append("'").append(param).append("'");
            else
                buf.append("?");
        }
        return buf.toString();
    }

    /**
     * Create and populate the parameters of a prepared statement using
     * the SQL in this buffer.
     */
    public PreparedStatement prepareStatement(Connection conn)
        throws SQLException {
        return prepareStatement(conn, ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * Create and populate the parameters of a prepared statement using
     * the SQL in this buffer.
     */
    public PreparedStatement prepareStatement(Connection conn, int rsType,
        int rsConcur)
        throws SQLException {
        return prepareStatement(conn, null, rsType, rsConcur);
    }

    /**
     * Create and populate the parameters of a prepred statement using the
     * SQL in this buffer and the given fetch configuration.
     */
    public PreparedStatement prepareStatement(Connection conn,
        JDBCFetchConfiguration fetch, int rsType, int rsConcur)
        throws SQLException {
        if (rsType == -1 && fetch == null)
            rsType = ResultSet.TYPE_FORWARD_ONLY;
        else if (rsType == -1)
            rsType = fetch.getResultSetType();
        if (rsConcur == -1)
            rsConcur = ResultSet.CONCUR_READ_ONLY;

        PreparedStatement stmnt;
        if (rsType == ResultSet.TYPE_FORWARD_ONLY
            && rsConcur == ResultSet.CONCUR_READ_ONLY)
            stmnt = conn.prepareStatement(getSQL());
        else
            stmnt = conn.prepareStatement(getSQL(), rsType, rsConcur);
        try {
            setParameters(stmnt);
            if (fetch != null) {
                if (fetch.getFetchBatchSize() > 0)
                    stmnt.setFetchSize(fetch.getFetchBatchSize());
                if (rsType != ResultSet.TYPE_FORWARD_ONLY
                    && fetch.getFetchDirection() != ResultSet.FETCH_FORWARD)
                    stmnt.setFetchDirection(fetch.getFetchDirection());
            }
            return stmnt;
        } catch (SQLException se) {
            try {
                stmnt.close();
            } catch (SQLException se2) {
            }
            throw se;
        }
    }

    /**
     * Create and populate the parameters of a prepared statement using
     * the SQL in this buffer.
     */
    public CallableStatement prepareCall(Connection conn)
        throws SQLException {
        return prepareCall(conn, ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * Create and populate the parameters of a prepared statement using
     * the SQL in this buffer.
     */
    public CallableStatement prepareCall(Connection conn, int rsType,
        int rsConcur)
        throws SQLException {
        return prepareCall(conn, null, rsType, rsConcur);
    }

    /**
     * Create and populate the parameters of a prepred statement using the
     * SQL in this buffer and the given fetch configuration.
     */
    public CallableStatement prepareCall(Connection conn,
        JDBCFetchConfiguration fetch, int rsType, int rsConcur)
        throws SQLException {
        if (rsType == -1 && fetch == null)
            rsType = ResultSet.TYPE_FORWARD_ONLY;
        else if (rsType == -1)
            rsType = fetch.getResultSetType();
        if (rsConcur == -1)
            rsConcur = ResultSet.CONCUR_READ_ONLY;

        CallableStatement stmnt;
        if (rsType == ResultSet.TYPE_FORWARD_ONLY
            && rsConcur == ResultSet.CONCUR_READ_ONLY)
            stmnt = conn.prepareCall(getSQL());
        else
            stmnt = conn.prepareCall(getSQL(), rsType, rsConcur);
        try {
            setParameters(stmnt);
            if (fetch != null) {
                if (fetch.getFetchBatchSize() > 0)
                    stmnt.setFetchSize(fetch.getFetchBatchSize());
                if (rsType != ResultSet.TYPE_FORWARD_ONLY
                    && fetch.getFetchDirection() != ResultSet.FETCH_FORWARD)
                    stmnt.setFetchDirection(fetch.getFetchDirection());
            }
            return stmnt;
        } catch (SQLException se) {
            try {
                stmnt.close();
            } catch (SQLException se2) {
            }
            throw se;
        }
    }

    /**
     * Populate the parameters of an existing PreparedStatement
     * with values from this buffer.
     */
    public void setParameters(PreparedStatement ps)
        throws SQLException {
        if (_params == null)
            return;

        Column col;
        for (int i = 0; i < _params.size(); i++) {
            col = (_cols == null) ? null : (Column) _cols.get(i);
            _dict.setUnknown(ps, i + 1, _params.get(i), col);
        }
    }

    public int hashCode() {
        int hash = _sql.hashCode();
        return (_params == null) ? hash : hash ^ _params.hashCode();
    }

    /**
     * Compare internal SQL without resolving subselects or stringifying
     * parameters.
     */
    public boolean sqlEquals(String sql) {
        return _sql.toString().equals(sql);
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof SQLBuffer))
            return false;

        SQLBuffer buf = (SQLBuffer) other;
        return _sql.equals(buf._sql)
            && ObjectUtils.equals(_params, buf._params);
    }

    /**
     * Represents a subselect.
     */
    private static class Subselect {

        public Select select;
        public JDBCFetchConfiguration fetch;
        public boolean count;
        public int sqlIndex;
        public int paramIndex;

        public Subselect clone(int sqlIndex, int paramIndex) {
            if (sqlIndex == 0 && paramIndex == 0)
                return this;

            Subselect sub = new Subselect();
            sub.select = select;
            sub.fetch = fetch;
            sub.count = count;
            sub.sqlIndex = this.sqlIndex + sqlIndex;
            sub.paramIndex = this.paramIndex + paramIndex;
            return sub;
        }
    }
}


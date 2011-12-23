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
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.exps.Val;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.kernel.exps.Parameter;


/**
 * Buffer for SQL statements that can be used to create
 * java.sql.PreparedStatements.
 * This buffer holds the SQL statement parameters and their corresponding 
 * columns. The parameters introduced by the runtime system are distinguished
 * from the parameters set by the user.  
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @author Pinaki Poddar
 * 
 * @since 0.2.4
 */
@SuppressWarnings("serial")
public final class SQLBuffer
    implements Serializable, Cloneable {

    private static final String PARAMETER_TOKEN = "?";

    private final DBDictionary _dict;
    private final StringBuilder _sql = new StringBuilder();
    private List<Subselect> _subsels = null;
    private List<BindParameter> _params = null;
    
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
     * Return true if the buffer is empty.
     */
    public boolean isEmpty() {
        return _sql.length() == 0;
    }

    /**
     * Append all SQL and parameters of the given buffer.
     */
    public SQLBuffer append(SQLBuffer buf) {
        append(buf, _sql.length(), (_params == null) ? 0 : _params.size(), true);
        return this;
    }

    /**
     * Append all SQL and parameters of the given buffer at the given positions.
     */
    private void append(SQLBuffer buf, int sqlIndex, int paramIndex, boolean subsels) {
        if (subsels) {
            // only allow appending of buffers with subselects, not insertion
            if (_subsels != null && !_subsels.isEmpty() && sqlIndex != _sql.length())
                throw new IllegalStateException();
            if (buf._subsels != null && !buf._subsels.isEmpty()) {
                if (sqlIndex != _sql.length())
                    throw new IllegalStateException();
                if (_subsels == null)
                    _subsels = new ArrayList<Subselect>(buf._subsels.size());
                for (int i = 0; i < buf._subsels.size(); i++)
                    _subsels.add((buf._subsels.get(i)).clone(sqlIndex, paramIndex));
            }
        }

        if (sqlIndex == _sql.length()) {
            _sql.append(buf._sql.toString());
        } else {
            _sql.insert(sqlIndex, buf._sql.toString());
        }
        
        if (buf._params != null) {
            if (_params == null)
                _params = new ArrayList<BindParameter>();

            if (paramIndex == _params.size()) {
                _params.addAll(buf._params);
            } else {
                _params.addAll(paramIndex, buf._params);
            }
        }
    }
    
    public SQLBuffer append(DBIdentifier name) {
        _sql.append(_dict.toDBName(name));
        return this;
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
        _sql.append(_dict.getColumnDBName(col));
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
            _subsels = new ArrayList<Subselect>(2);
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
    	_sql.append(o == null ? "NULL" : o.toString());
        return this;
    }
    
    /**
     * Append a system inserted parameter value for a specific column.
     */
    public SQLBuffer appendValue(Object o, Column col) {
        return appendValue(o, col, null);
    }
    
    /**
     * Append a user parameter value for a specific column. User parameters
     * are marked as opposed to the parameters inserted by the internal runtime
     * system. This helps to reuse the buffer by reparmeterizing it with new
     * set of user parameters while the 'internal' parameters remain unchanged.
     * 
     * @param userParam if non-null, designates a 'user' parameter.
     */
    public SQLBuffer appendValue(Object o, Column col, Parameter userParam) {
        if (o == null)
            _sql.append("NULL");
        else if (o instanceof Raw)
            _sql.append(o.toString());
        else {
            _sql.append(PARAMETER_TOKEN);

            if (_params == null)
                _params = new ArrayList<BindParameter>();
            BindParameter param = new BindParameter(userParam == null ? col : userParam, 
            		userParam != null, col, o);
            _params.add(param);
        }
        return this;
    }
 
    /**
     * Return the list of parameter values.
     */
    public List<BindParameter> getParameters() {
    	if (_params == null) return Collections.emptyList();
        return _params;
    }
    
    /**
     * Get the user parameter positions in the list of parameters. The odd 
     * element of the returned list contains an integer index that refers
     * to the position in the {@link #getParameters()} list. The even element
     * of the returned list refers to the user parameter key. 
     * This structure is preferred over a normal map because a user parameter 
     * may occur more than one in the parameters. 
     */
    public List<Object> getUserParameters() {
        if (_params == null)
            return Collections.emptyList();
        List<Object> userParam = new ArrayList<Object>();
        for (int i = 0; i < _params.size(); i++) {
        	BindParameter param = _params.get(i);
        	if (param.isUser()) {
        		userParam.add(i);
        		userParam.add(param.getKey());
        	}
        }
        return userParam;
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
        resolveSubselects();
        String sql = _sql.toString();
        if (!replaceParams || _params == null || _params.isEmpty())
            return sql;

        StringBuilder buf = new StringBuilder();
        Iterator<BindParameter> pi = _params.iterator();
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) != '?') {
                buf.append(sql.charAt(i));
                continue;
            }

            Object param = pi.hasNext() ? pi.next().getValue() : null;
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
     * Resolve our delayed subselects.
     */
    private void resolveSubselects() {
        if (_subsels == null || _subsels.isEmpty())
            return;

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
            buf.resolveSubselects();
            append(buf, sub.sqlIndex, sub.paramIndex, false);
        }
        _subsels.clear();
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
     * Create and populate the parameters of a prepared statement using the
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
                    stmnt.setFetchSize(
                        _dict.getBatchFetchSize(fetch.getFetchBatchSize()));
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
                    stmnt.setFetchSize(
                        _dict.getBatchFetchSize(fetch.getFetchBatchSize()));
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

        for (int i = 0; i < _params.size(); i++) {
        	BindParameter param = _params.get(i);
            _dict.setUnknown(ps, i + 1, param.getValue(), param.getColumn());
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
     * Replace SQL '?' with CAST string if required by DB platform
     * @param oper
     * @param val
     */
    public void addCastForParam(String oper, Val val) {
        if (_sql.charAt(_sql.length() - 1) == '?') {
            String castString = _dict.addCastAsType(oper, val);
            if (castString != null)
                _sql.replace(_sql.length() - 1, _sql.length(), castString);
        }
    }

    /**
     * Replace current buffer string with the new string
     * 
     * @param start replace start position
     * @param end replace end position
     * @param newString
     */
    public void replaceSqlString(int start, int end, String newString) {
        _sql.replace(start, end, newString);
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
    
    /**
     * Sets the binding parameters, clearing all existing parameters.
     * 
     * @param params a non-null list of parameters.
     */
    public void setParameters(List<?> params) {
    	if (params == null) {
    		throw new IllegalArgumentException("Can not set null parameter");
    	}
    	_params = new ArrayList<BindParameter>();
    	int i = 0;
    	for (Object p : params) {
    		BindParameter param = new BindParameter(i, false, null, p);
    		_params.add(param);
    	}
    }
    
}

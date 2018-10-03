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
package org.apache.openjpa.slice.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A template for multiple Statements being executed by multiple connections.
 *
 * @author Pinaki Poddar
 *
 */
public class DistributedTemplate<T extends Statement>
	implements Statement, Iterable<T> {


    protected List<T> stmts = new ArrayList<>();
	protected final DistributedConnection con;
	protected T master;

	public DistributedTemplate(DistributedConnection c) {
		con = c;
	}

	@Override
    public Iterator<T> iterator() {
		return stmts.iterator();
	}

	public void add(T s) {
		if (stmts.isEmpty())
			master = s;
		try {
			if (!con.contains(s.getConnection()))
                throw new IllegalArgumentException(s +
                        " has different connection");
			stmts.add(s);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
    public void addBatch(String sql) throws SQLException {
		for (T s:this)
			s.addBatch(sql);
	}

	@Override
    public void cancel() throws SQLException {
		for (T s:this)
			s.cancel();
	}

	@Override
    public void clearBatch() throws SQLException {
		for (T s:this)
			s.clearBatch();
	}

	@Override
    public void clearWarnings() throws SQLException {
		for (T s:this)
			s.clearWarnings();
	}

	@Override
    public void close() throws SQLException {
		for (T s:this)
			s.close();
	}

	@Override
    public boolean execute(String arg0) throws SQLException {
		boolean ret = true;
		for (T s:this)
			ret = s.execute(arg0) & ret;
		return ret;
	}

	@Override
    public boolean execute(String arg0, int arg1) throws SQLException {
		boolean ret = true;
		for (T s:this)
			ret = s.execute(arg0, arg1) & ret;
		return ret;
	}

	@Override
    public boolean execute(String arg0, int[] arg1) throws SQLException {
		boolean ret = true;
		for (T s:this)
			ret = s.execute(arg0, arg1) & ret;
		return ret;
	}

	@Override
    public boolean execute(String arg0, String[] arg1) throws SQLException {
		boolean ret = true;
		for (T s:this)
			ret = s.execute(arg0, arg1) & ret;
		return ret;
	}

	@Override
    public int[] executeBatch() throws SQLException {
		int[] ret = new int[0];
		for (Statement s:this) {
			int[] tmp = s.executeBatch();
			ret = new int[ret.length + tmp.length];
            System.arraycopy(tmp, 0, ret, ret.length-tmp.length, tmp.length);
		}
		return ret;
	}

	public ResultSet executeQuery() throws SQLException {
		DistributedResultSet rs = new DistributedResultSet();
		for (T s:this)
			rs.add(s.executeQuery(null));
		return rs;
	}

	@Override
    public ResultSet executeQuery(String arg0) throws SQLException {
		DistributedResultSet rs = new DistributedResultSet();
		for (T s:this)
			rs.add(s.executeQuery(arg0));
		return rs;
	}

	@Override
    public int executeUpdate(String arg0) throws SQLException {
		int ret = 0;
		for (T s:this)
			ret += s.executeUpdate(arg0);
		return ret;
	}

	@Override
    public int executeUpdate(String arg0, int arg1) throws SQLException {
		int ret = 0;
		for (T s:this)
			ret += s.executeUpdate(arg0, arg1);
		return ret;
	}

	@Override
    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		int ret = 0;
		for (T s:this)
			ret += s.executeUpdate(arg0, arg1);
		return ret;
	}

    @Override
    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		int ret = 0;
		for (T s:this)
			ret += s.executeUpdate(arg0, arg1);
		return ret;
	}

	@Override
    public Connection getConnection() throws SQLException {
		return con;
	}

	@Override
    public int getFetchDirection() throws SQLException {
		return master.getFetchDirection();
	}

	@Override
    public int getFetchSize() throws SQLException {
		return master.getFetchSize();
	}

	@Override
    public ResultSet getGeneratedKeys() throws SQLException {
		DistributedResultSet mrs = new DistributedResultSet();
		for (T s:this)
			mrs.add(s.getGeneratedKeys());
		return mrs;
	}

	@Override
    public int getMaxFieldSize() throws SQLException {
		return master.getMaxFieldSize();
	}

	@Override
    public int getMaxRows() throws SQLException {
		return master.getMaxRows();
	}

	@Override
    public boolean getMoreResults() throws SQLException {
		for (T s:this)
			if (s.getMoreResults())
				return true;
		return false;
	}

	@Override
    public boolean getMoreResults(int arg0) throws SQLException {
		for (T s:this)
			if (s.getMoreResults(arg0))
				return true;
		return false;
	}

	@Override
    public int getQueryTimeout() throws SQLException {
		return master.getQueryTimeout();
	}

	@Override
    public ResultSet getResultSet() throws SQLException {
		DistributedResultSet rs = new DistributedResultSet();
		for (T s:this)
			rs.add(s.getResultSet());
		return rs;
	}

	@Override
    public int getResultSetConcurrency() throws SQLException {
		return master.getResultSetConcurrency();
	}

	@Override
    public int getResultSetHoldability() throws SQLException {
		return master.getResultSetHoldability();
	}

	@Override
    public int getResultSetType() throws SQLException {
		return master.getResultSetType();
	}

	@Override
    public int getUpdateCount() throws SQLException {
		return master.getUpdateCount();
	}

	@Override
    public SQLWarning getWarnings() throws SQLException {
		return master.getWarnings();
	}

	@Override
    public void setCursorName(String name) throws SQLException {
		for (T s:this)
			s.setCursorName(name);
	}

	@Override
    public void setEscapeProcessing(boolean flag) throws SQLException {
		for (T s:this)
			s.setEscapeProcessing(flag);
	}

	@Override
    public void setFetchDirection(int dir) throws SQLException {
		for (T s:this)
			s.setFetchDirection(dir);
	}

	@Override
    public void setFetchSize(int size) throws SQLException {
		for (T s:this)
			s.setFetchSize(size);
	}

	@Override
    public void setMaxFieldSize(int size) throws SQLException {
		for (T s:this)
			s.setMaxFieldSize(size);
	}

	@Override
    public void setMaxRows(int n) throws SQLException {
		for (T s:this)
			s.setMaxRows(n);
	}

	@Override
    public void setQueryTimeout(int n) throws SQLException {
		for (T s:this)
			s.setQueryTimeout(n);
	}

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    // Java 7 methods follow

    @Override
    public boolean isCloseOnCompletion() throws SQLException{
    	throw new UnsupportedOperationException();
    }

    @Override
    public void closeOnCompletion() throws SQLException{
    	throw new UnsupportedOperationException();
    }
}

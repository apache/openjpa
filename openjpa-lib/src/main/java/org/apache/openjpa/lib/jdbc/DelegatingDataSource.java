/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.openjpa.lib.util.Closeable;

/**
 * Wrapper around an existing data source. Subclasses can override the
 * methods whose behavior they mean to change. The <code>equals</code> and
 * <code>hashCode</code> methods pass through to the base underlying data store.
 *
 * @author Abe White
 */
public class DelegatingDataSource implements DataSource, Closeable {

    private final DataSource _ds;
    private final DelegatingDataSource _del;

    /**
     * Constructor. Supply wrapped data source.
     */
    public DelegatingDataSource(DataSource ds) {
        _ds = ds;

        if (_ds instanceof DelegatingDataSource)
            _del = (DelegatingDataSource) _ds;
        else
            _del = null;
    }

    /**
     * Return the wrapped data source.
     */
    public DataSource getDelegate() {
        return _ds;
    }

    /**
     * Return the inner-most wrapped delegate.
     */
    public DataSource getInnermostDelegate() {
        return (_del == null) ? _ds : _del.getInnermostDelegate();
    }

    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingDataSource)
            other = ((DelegatingDataSource) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("datasource "). append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return _ds.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        _ds.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return _ds.getLoginTimeout();
    }

    public void setLoginTimeout(int timeout) throws SQLException {
        _ds.setLoginTimeout(timeout);
    }

    public Connection getConnection() throws SQLException {
        return _ds.getConnection();
    }

    public Connection getConnection(String user, String pass)
        throws SQLException {
        if (user == null && pass == null)
            return _ds.getConnection();
        return _ds.getConnection(user, pass);
    }

    public void close() throws Exception {
        if (_ds instanceof Closeable)
            ((Closeable) _ds).close();
    }
}

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.jdbc;

import org.apache.openjpa.lib.util.concurrent.*;

import java.sql.*;

import java.util.*;

import javax.sql.*;


/**
 *  <p>Delegating data source that maintains a list of
 *  {@link ConnectionDecorator}s.</p>
 *
 *  @author Abe White
 *  @nojavadoc */
public class DecoratingDataSource extends DelegatingDataSource {
    private List _decorators = new CopyOnWriteArrayList();
    private boolean _conf = false;

    /**
     *  Constructor.  Supply wrapped data source.
     */
    public DecoratingDataSource(DataSource ds) {
        super(ds);
    }

    /**
     *  Return a read-only list of connection decorators in the order they were
     *  added.
     */
    public Collection getDecorators() {
        return Collections.unmodifiableCollection(_decorators);
    }

    /**
     *  Add a connection decorator.
     */
    public void addDecorator(ConnectionDecorator decorator) {
        if (decorator != null) {
            _decorators.add(decorator);
        }
    }

    /**
     *  Add multiple connection decorators efficiently.
     */
    public void addDecorators(Collection decorators) {
        if (decorators != null) {
            _decorators.addAll(decorators);
        }
    }

    /**
     *  Remove a connection decorator.
     */
    public boolean removeDecorator(ConnectionDecorator decorator) {
        return _decorators.remove(decorator);
    }

    /**
     *  Clear all decorators.
     */
    public void clearDecorators() {
        _decorators.clear();
    }

    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();

        return decorate(conn);
    }

    public Connection getConnection(String user, String pass)
        throws SQLException {
        Connection conn = super.getConnection(user, pass);

        return decorate(conn);
    }

    private Connection decorate(Connection conn) throws SQLException {
        if (!_decorators.isEmpty()) {
            for (Iterator itr = _decorators.iterator(); itr.hasNext();)
                conn = ((ConnectionDecorator) itr.next()).decorate(conn);
        }

        return conn;
    }
}

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
package org.apache.openjpa.jdbc.schema;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.util.StoreException;

/**
 * Non-pooling driver data source.
 */
public class SimpleDriverDataSource
    implements DriverDataSource {

    private String _connectionDriverName;
    private String _connectionURL;
    private String _connectionUserName;
    private String _connectionPassword;
    private Properties _connectionProperties;
    private Properties _connectionFactoryProperties;
    private Driver _driver;
    private ClassLoader _classLoader;

    public Connection getConnection()
        throws SQLException {
        return getConnection(null);
    }

    public Connection getConnection(String username, String password)
        throws SQLException {
        Properties props = new Properties();
        if (username == null)
            username = _connectionUserName;
        if (username != null)
            props.put("user", username);

        if (password == null)
            password = _connectionPassword;
        if (password != null)
            props.put("password", password);

        return getConnection(props);
    }

    public Connection getConnection(Properties props)
        throws SQLException {
        return getDriver().connect(_connectionURL, props);
    }

    public int getLoginTimeout() {
        return 0;
    }

    public void setLoginTimeout(int seconds) {
    }

    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) {
    }

    public void initDBDictionary(DBDictionary dict) {
    }

    public void setConnectionURL(String connectionURL) {
        _connectionURL = connectionURL;
    }

    public String getConnectionURL() {
        return _connectionURL;
    }

    public void setConnectionUserName(String connectionUserName) {
        _connectionUserName = connectionUserName;
    }

    public String getConnectionUserName() {
        return _connectionUserName;
    }

    public void setConnectionPassword(String connectionPassword) {
        _connectionPassword = connectionPassword;
    }

    public void setConnectionProperties(Properties props) {
        _connectionProperties = props;
    }

    public Properties getConnectionProperties() {
        return _connectionProperties;
    }

    public void setConnectionFactoryProperties(Properties props) {
        _connectionFactoryProperties = props;
    }

    public Properties getConnectionFactoryProperties() {
        return _connectionFactoryProperties;
    }

    public List createConnectionDecorators() {
        return null;
    }

    public void setClassLoader(ClassLoader classLoader) {
        _classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return _classLoader;
    }

    public void setConnectionDriverName(String connectionDriverName) {
        _connectionDriverName = connectionDriverName;
    }

    public String getConnectionDriverName() {
        return _connectionDriverName;
    }

    private Driver getDriver() {
        if (_driver != null)
            return _driver;

        try { 
            _driver = DriverManager.getDriver(_connectionURL);
            if (_driver != null)
                return _driver;
        } catch (Exception e) {
        }

        try {
            Class.forName(_connectionDriverName, true, _classLoader);
        } catch (Exception e) {
        }
        try {
            _driver = DriverManager.getDriver(_connectionURL);
            if (_driver != null)
                return _driver;
        } catch (Exception e) {
        }

        try {
            Class c = Class.forName(_connectionDriverName,
                true, _classLoader);
            _driver = (Driver)AccessController.doPrivileged(
                J2DoPrivHelper.newInstanceAction(c));
            return _driver;
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw(RuntimeException) e;
            if (e instanceof PrivilegedActionException)
                e = ((PrivilegedActionException)e).getException();
            throw new StoreException(e);
        }
    }
}


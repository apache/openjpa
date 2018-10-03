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

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.jdbc.ConnectionDecorator;

/**
 * A DataSource that allows additional configuration options to be set
 * into it, so that it can wrap a JDBC driver or other DataSource.
 *
 * @author Marc Prud'hommeaux
 */
public interface DriverDataSource
    extends DataSource {

    /**
     * JDBC URL.
     */
    void setConnectionURL(String connectionURL);

    /**
     * JDBC URL.
     */
    String getConnectionURL();

    /**
     * Driver class name.
     */
    void setConnectionDriverName(String connectionDriverName);

    /**
     * Driver class name.
     */
    String getConnectionDriverName();

    /**
     * JDBC user name.
     */
    void setConnectionUserName(String connectionUserName);

    /**
     * JDBC user name.
     */
    String getConnectionUserName();

    /**
     * JDBC password.
     */
    void setConnectionPassword(String connectionPassword);

    /**
     * JDBC password.
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * Classloader for loading driver class, etc.
     */
    ClassLoader getClassLoader();

    /**
     * Configuration of datasource properties.
     */
    void setConnectionFactoryProperties(Properties props);

    /**
     * Configuration of datasource properties.
     */
    Properties getConnectionFactoryProperties();

    /**
     * Configuration of connection.
     */
    void setConnectionProperties(Properties props);

    /**
     * Configuration of connection.
     */
    Properties getConnectionProperties();

    /**
     * Provide any built-in decorators; may be null.
     */
    List<ConnectionDecorator> createConnectionDecorators();

    /**
     * Initialize self and dictionary once available.
     */
    void initDBDictionary(DBDictionary dict);
}


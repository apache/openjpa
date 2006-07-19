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
package org.apache.openjpa.jdbc.schema;

import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.conf.Configurable;

/**
 * A DataSource that allows additional configuration options to be set
 * into it, so that it can wrap a JDBC driver or other DataSource.
 *
 * @author Marc Prud'hommeaux
 */
public interface DriverDataSource
    extends DataSource, Configurable {

    public void setConnectionURL(String connectionURL);

    public String getConnectionURL();

    public void setConnectionDriverName(String connectionDriverName);

    public String getConnectionDriverName();

    public void setConnectionUserName(String connectionUserName);

    public String getConnectionUserName();

    public void setConnectionPassword(String connectionPassword);

    public void setClassLoader(ClassLoader classLoader);

    public ClassLoader getClassLoader();

    public void setConnectionFactoryProperties(Properties props);

    public Properties getConnectionFactoryProperties();

    public void setConnectionProperties(Properties props);

    public Properties getConnectionProperties();

    public List createConnectionDecorators();

    public void initDBDictionary(DBDictionary dict);
}


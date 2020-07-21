/*
 * Copyright 2020 Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.sql;

import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;

public class DBDictionaryFactoryTest {

    @Test
    public void testDictionaryClassForString() {
        JDBCConfiguration conf = new JDBCConfigurationImpl();

        String[] aliases = new String[]{
            "access", org.apache.openjpa.jdbc.sql.AccessDictionary.class.getName(),
            "db2", org.apache.openjpa.jdbc.sql.DB2Dictionary.class.getName(),
            "derby", org.apache.openjpa.jdbc.sql.DerbyDictionary.class.getName(),
            "empress", org.apache.openjpa.jdbc.sql.EmpressDictionary.class.getName(),
            "foxpro", org.apache.openjpa.jdbc.sql.FoxProDictionary.class.getName(),
            "h2", org.apache.openjpa.jdbc.sql.H2Dictionary.class.getName(),
            "hsql", org.apache.openjpa.jdbc.sql.HSQLDictionary.class.getName(),
            "informix", org.apache.openjpa.jdbc.sql.InformixDictionary.class.getName(),
            "ingres", org.apache.openjpa.jdbc.sql.IngresDictionary.class.getName(),
            "jdatastore", org.apache.openjpa.jdbc.sql.JDataStoreDictionary.class.getName(),
            "mariadb", org.apache.openjpa.jdbc.sql.MariaDBDictionary.class.getName(),
            "mysql", org.apache.openjpa.jdbc.sql.MySQLDictionary.class.getName(),
            "herddb", org.apache.openjpa.jdbc.sql.HerdDBDictionary.class.getName(),
            "oracle", org.apache.openjpa.jdbc.sql.OracleDictionary.class.getName(),
            "pointbase", org.apache.openjpa.jdbc.sql.PointbaseDictionary.class.getName(),
            "postgres", org.apache.openjpa.jdbc.sql.PostgresDictionary.class.getName(),
            "soliddb", org.apache.openjpa.jdbc.sql.SolidDBDictionary.class.getName(),
            "sqlserver", org.apache.openjpa.jdbc.sql.SQLServerDictionary.class.getName(),
            "sybase", org.apache.openjpa.jdbc.sql.SybaseDictionary.class.getName(),
            "maxdb", MaxDBDictionary.class.getName(),
            "jdbc:h2:", H2Dictionary.class.getName(),
            "h2 database", H2Dictionary.class.getName()
        };

        for (int i = 0; i < aliases.length; i++) {
            String key = aliases[i++];
            String expected = aliases[i];
            assertEquals(expected, DBDictionaryFactory.dictionaryClassForString(key, conf));
        }
    }

}

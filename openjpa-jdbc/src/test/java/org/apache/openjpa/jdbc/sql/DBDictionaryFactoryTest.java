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
            "access", AccessDictionary.class.getName(),
            "db2", DB2Dictionary.class.getName(),
            "derby", DerbyDictionary.class.getName(),
            "empress", EmpressDictionary.class.getName(),
            "foxpro", FoxProDictionary.class.getName(),
            "h2", H2Dictionary.class.getName(),
            "hsql", HSQLDictionary.class.getName(),
            "informix", InformixDictionary.class.getName(),
            "ingres", IngresDictionary.class.getName(),
            "jdatastore", JDataStoreDictionary.class.getName(),
            "mariadb", MariaDBDictionary.class.getName(),
            "mysql", MySQLDictionary.class.getName(),
            "herddb", HerdDBDictionary.class.getName(),
            "oracle", OracleDictionary.class.getName(),
            "pointbase", PointbaseDictionary.class.getName(),
            "postgres", PostgresDictionary.class.getName(),
            "soliddb", SolidDBDictionary.class.getName(),
            "sqlserver", SQLServerDictionary.class.getName(),
            "sybase", SybaseDictionary.class.getName(),
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

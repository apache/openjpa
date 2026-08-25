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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.junit.Test;

/**
 * Tests for {@link PostgresDictionary}, in particular the version dependent
 * <code>StoreCharsAsNumbers</code> default and that an explicit user setting
 * survives {@link DBDictionary#connectedConfiguration(Connection)} (OPENJPA-2971).
 */
public class TestPostgresDictionary {

    @Test
    public void testStoreCharsAsNumbersDefaultsToFalseOnModernPostgres() throws Exception {
        PostgresDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection(16, 0));
        assertFalse(dict.storeCharsAsNumbers);
        assertFalse(dict.getStoreCharsAsNumbers());
    }

    @Test
    public void testStoreCharsAsNumbersDefaultsToTrueOnLegacyPostgres() throws Exception {
        PostgresDictionary dict = newConfiguredDictionary(null);
        dict.connectedConfiguration(connection(8, 4));
        assertTrue(dict.storeCharsAsNumbers);
        assertTrue(dict.getStoreCharsAsNumbers());
    }

    @Test
    public void testExplicitStoreCharsAsNumbersSurvivesConnectedConfiguration() throws Exception {
        PostgresDictionary dict = newConfiguredDictionary("StoreCharsAsNumbers=true");
        assertTrue(dict.storeCharsAsNumbers);
        dict.connectedConfiguration(connection(16, 0));
        assertTrue("explicit StoreCharsAsNumbers=true must not be overridden on PostgreSQL 16",
            dict.storeCharsAsNumbers);
    }

    @Test
    public void testExplicitStoreCharsAsNumbersFalseSurvivesOnLegacyPostgres() throws Exception {
        PostgresDictionary dict = newConfiguredDictionary("StoreCharsAsNumbers=false");
        assertFalse(dict.storeCharsAsNumbers);
        dict.connectedConfiguration(connection(8, 4));
        assertFalse("explicit StoreCharsAsNumbers=false must not be overridden on PostgreSQL 8.4",
            dict.storeCharsAsNumbers);
    }

    @Test
    public void testSetterMarksValueAsExplicit() throws Exception {
        PostgresDictionary dict = newConfiguredDictionary(null);
        assertFalse(dict.isStoreCharsAsNumbersExplicit());
        dict.setStoreCharsAsNumbers(true);
        assertTrue(dict.isStoreCharsAsNumbersExplicit());
        dict.connectedConfiguration(connection(16, 0));
        assertTrue(dict.storeCharsAsNumbers);
    }

    @Test
    public void testDictionaryDefaultIsNativeCharStorageBeforeConnecting() {
        assertFalse(new PostgresDictionary().storeCharsAsNumbers);
        assertFalse(new PostgresDictionary().isStoreCharsAsNumbersExplicit());
    }

    @Test
    public void testProgrammaticPublicFieldAssignmentSurvivesOnModernPostgres() throws Exception {
        PostgresDictionary dict = newConfiguredDictionary(null);
        dict.storeCharsAsNumbers = true;
        dict.connectedConfiguration(connection(16, 0));
        assertTrue("storeCharsAsNumbers=true assigned via the public field must survive on PostgreSQL 16",
            dict.storeCharsAsNumbers);
    }

    @Test
    public void testSubclassConstructorAssignmentSurvivesOnModernPostgres() throws Exception {
        PostgresDictionary dict = new PostgresDictionary() {
            {
                storeCharsAsNumbers = true;
            }
        };
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), (String) null, "DBDictionary");
        dict.connectedConfiguration(connection(16, 0));
        assertTrue("storeCharsAsNumbers=true assigned by a subclass constructor must survive on PostgreSQL 16",
            dict.storeCharsAsNumbers);
    }

    @Test
    public void testExplicitStoreCharsAsNumbersThroughDBDictionaryFactory() throws Exception {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        DataSource ds = dataSource(connection(16, 0));

        DBDictionary explicit = DBDictionaryFactory.newDBDictionary(conf, ds, "StoreCharsAsNumbers=true");
        assertTrue(explicit instanceof PostgresDictionary);
        assertTrue("explicit StoreCharsAsNumbers=true must survive DBDictionaryFactory", explicit.storeCharsAsNumbers);

        DBDictionary auto = DBDictionaryFactory.newDBDictionary(conf, ds, null);
        assertTrue(auto instanceof PostgresDictionary);
        assertFalse("PostgreSQL 16 must default to native CHAR storage", auto.storeCharsAsNumbers);
    }

    /**
     * Creates a dictionary configured the same way user supplied
     * <code>openjpa.jdbc.DBDictionary=postgres(...)</code> plugin properties are applied.
     */
    private static PostgresDictionary newConfiguredDictionary(String props) {
        PostgresDictionary dict = new PostgresDictionary();
        Configurations.configureInstance(dict, new JDBCConfigurationImpl(), props, "DBDictionary");
        return dict;
    }

    private static DataSource dataSource(final Connection conn) {
        return (DataSource) Proxy.newProxyInstance(TestPostgresDictionary.class.getClassLoader(),
            new Class<?>[] { DataSource.class }, new DefaultHandler() {
                @Override
                protected Object handle(Method method, Object[] args) {
                    if ("getConnection".equals(method.getName())) {
                        return conn;
                    }
                    return null;
                }
            });
    }

    /**
     * A {@link Connection} whose {@link DatabaseMetaData} reports the given
     * PostgreSQL version and sensible defaults for everything else.
     */
    private static Connection connection(final int major, final int minor) {
        final DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
            TestPostgresDictionary.class.getClassLoader(), new Class<?>[] { DatabaseMetaData.class },
            new DefaultHandler() {
                @Override
                protected Object handle(Method method, Object[] args) {
                    switch (method.getName()) {
                        case "getDatabaseProductName":
                            return "PostgreSQL";
                        case "getDatabaseProductVersion":
                            return major + "." + minor;
                        case "getDatabaseMajorVersion":
                            return major;
                        case "getDatabaseMinorVersion":
                            return minor;
                        case "getJDBCMajorVersion":
                            return 4;
                        case "getJDBCMinorVersion":
                            return 2;
                        case "getDriverName":
                            return "PostgreSQL JDBC Driver";
                        case "getDriverVersion":
                            return "42.7";
                        case "getIdentifierQuoteString":
                            return "\"";
                        case "getURL":
                            return "jdbc:postgresql://localhost/test";
                        default:
                            return null;
                    }
                }
            });
        return (Connection) Proxy.newProxyInstance(TestPostgresDictionary.class.getClassLoader(),
            new Class<?>[] { Connection.class }, new DefaultHandler() {
                @Override
                protected Object handle(Method method, Object[] args) {
                    if ("getMetaData".equals(method.getName())) {
                        return metaData;
                    }
                    return null;
                }
            });
    }

    /**
     * Invocation handler returning type appropriate defaults (false / 0 / "" / null)
     * for every method unless {@link #handle(Method, Object[])} provides a value.
     */
    private abstract static class DefaultHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "toString":
                    return "proxy";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    break;
            }
            Object value = handle(method, args);
            return value != null ? value : defaultValue(method.getReturnType());
        }

        protected abstract Object handle(Method method, Object[] args);

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class) {
                return Boolean.FALSE;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == float.class) {
                return 0f;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == char.class) {
                return (char) 0;
            }
            if (type == String.class) {
                return "";
            }
            return null;
        }
    }
}

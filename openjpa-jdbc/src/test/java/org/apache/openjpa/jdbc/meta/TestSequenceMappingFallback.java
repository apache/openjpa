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
package org.apache.openjpa.jdbc.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.kernel.NativeJDBCSeq;
import org.apache.openjpa.jdbc.kernel.ValueTableJDBCSeq;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.OpenJPAException;
import org.junit.Test;

/**
 * Tests the gate in {@link SequenceMapping#instantiate(ClassLoader)} that
 * decides which sequence implementation backs a named generator asking for a
 * native sequence (OPENJPA-3014). No server is involved: the dictionary is a
 * stub and the connection factory is a stub <code>DataSource</code>, so the
 * decision is exercised on every platform, including the ones that have
 * sequence objects and would therefore only ever take the native path in an
 * end to end test.
 */
public class TestSequenceMappingFallback {

    private static final String SEQUENCE_QUERY = "SELECT NEXT VALUE FOR {0}";

    /**
     * A dictionary that only learns from the server that the database has
     * sequences, as {@link org.apache.openjpa.jdbc.sql.MariaDBDictionary}
     * does from 10.3 on.
     */
    public static class SequencesOnceConnected extends DBDictionary {

        @Override
        public void connectedConfiguration(Connection conn) throws SQLException {
            super.connectedConfiguration(conn);
            nextSequenceQuery = SEQUENCE_QUERY;
        }
    }

    /**
     * A dictionary for a database without sequence objects, like MySQL or
     * Derby: <code>nextSequenceQuery</code> stays null even when connected.
     */
    public static class NoSequences extends DBDictionary {
    }

    /**
     * A dictionary declaring sequence support in its constructor, like
     * Oracle, DB2, PostgreSQL, H2 or HSQL.
     */
    public static class SequencesDeclared extends DBDictionary {

        public SequencesDeclared() {
            nextSequenceQuery = SEQUENCE_QUERY;
        }
    }

    /**
     * A dictionary that declares no sequence support and whose
     * <code>connectedConfiguration</code> must never run, because obtaining a
     * connection fails. Instantiating a generator must then fail rather than
     * guess a backing store.
     */
    @Test
    public void testConnectFailureIsNotAnswerWithTheTableFallback() {
        JDBCConfigurationImpl conf = newConfiguration(SequencesOnceConnected.class,
            UnreachableDataSource.class);
        try {
            Seq seq = generator(conf).getInstance(null);
            fail("expected the connect failure to surface, got " + seq);
        } catch (MetaDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no connection could be obtained"));
            assertTrue("the real cause has to be reachable: " + e.getCause(),
                causeChain(e).contains(UnreachableDataSource.MESSAGE));
        }
    }

    /**
     * Connecting is what tells this dictionary that the database has
     * sequences, so the generator has to end up on the native path.
     */
    @Test
    public void testSequenceSupportLearnedOnConnectKeepsTheNativePath() {
        JDBCConfigurationImpl conf = newConfiguration(SequencesOnceConnected.class,
            StubDataSource.class);
        Seq seq = generator(conf).getInstance(null);
        assertEquals(NativeJDBCSeq.class, seq.getClass());
        assertTrue("the dictionary has to have been connected",
            conf.getDBDictionaryInstance().isConnected());
    }

    /**
     * A database that has no sequence objects at all, connected or not: the
     * generator is backed by a counter row keyed by the sequence name.
     */
    @Test
    public void testNoSequenceSupportTakesTheTableFallback() {
        JDBCConfigurationImpl conf = newConfiguration(NoSequences.class, StubDataSource.class);
        Seq seq = generator(conf).getInstance(null);
        assertEquals(ValueTableJDBCSeq.class, seq.getClass());
        assertEquals("OJPA3014_SEQ", ((ValueTableJDBCSeq) seq).getPrimaryKeyValue());
        assertEquals(100, ((ValueTableJDBCSeq) seq).getInitialValue());
        assertEquals(7, ((ValueTableJDBCSeq) seq).getAllocate());
    }

    /**
     * The decision is cached on the metadata, so it is taken once.
     */
    @Test
    public void testTheSubstituteIsInstantiatedOnce() {
        JDBCConfigurationImpl conf = newConfiguration(NoSequences.class, StubDataSource.class);
        SequenceMapping seq = generator(conf);
        assertSame(seq.getInstance(null), seq.getInstance(null));
    }

    /**
     * A dictionary that declares a NextSequenceQuery in its constructor is
     * resolved without connecting, so an offline mapping tool run does not
     * stall on a connect attempt: the data source here would fail if asked.
     */
    @Test
    public void testDeclaredSequenceSupportIsResolvedWithoutConnecting() {
        JDBCConfigurationImpl conf = newConfiguration(SequencesDeclared.class,
            UnreachableDataSource.class);
        Seq seq = generator(conf).getInstance(null);
        assertEquals(NativeJDBCSeq.class, seq.getClass());
        assertTrue("no connection may have been attempted",
            !conf.getDBDictionaryInstance().isConnected());
    }

    /**
     * EmulateNativeSequences=false restores the pre-4.2.0 failure.
     */
    @Test
    public void testEmulationCanBeTurnedOff() {
        JDBCConfigurationImpl conf = newConfiguration(NoSequences.class, StubDataSource.class);
        conf.setCompatibility("EmulateNativeSequences=false");
        try {
            Seq seq = generator(conf).getInstance(null);
            fail("expected the historical failure, got " + seq);
        } catch (MetaDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not support native sequences"));
        }
    }

    /**
     * A generator whose sequence name configures a sequence plugin itself is
     * left alone, connected or not.
     */
    @Test
    public void testAPluginStringGeneratorIsLeftAlone() {
        JDBCConfigurationImpl conf = newConfiguration(NoSequences.class, UnreachableDataSource.class);
        SequenceMapping seq = generator(conf);
        seq.setSequencePlugin(SequenceMapping.IMPL_VALUE_TABLE);
        Seq instance = seq.getInstance(null);
        assertEquals(ValueTableJDBCSeq.class, instance.getClass());
    }

    /**
     * Everything the exception reports as a cause, directly and as a nested
     * throwable of an OpenJPA exception.
     */
    private static String causeChain(Throwable t) {
        StringBuilder chain = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            chain.append(c).append('\n');
            if (c instanceof OpenJPAException) {
                for (Throwable nested : ((OpenJPAException) c).getNestedThrowables()) {
                    chain.append(nested).append('\n');
                }
            }
        }
        return chain.toString();
    }

    private static SequenceMapping generator(JDBCConfigurationImpl conf) {
        // the repository is wired up by hand rather than through
        // getMappingRepositoryInstance(), which would need a metadata factory
        // and hence a persistence unit; only getConfiguration() is used here
        MappingRepository repos = new MappingRepository();
        repos.setConfiguration(conf);
        SequenceMapping seq = (SequenceMapping) repos.addSequenceMetaData("OJPA3014_GEN");
        seq.setSequencePlugin(SequenceMetaData.IMPL_NATIVE);
        seq.setSequence("OJPA3014_SEQ");
        seq.setInitialValue(100);
        seq.setAllocate(7);
        return seq;
    }

    private static JDBCConfigurationImpl newConfiguration(Class<? extends DBDictionary> dict,
        Class<? extends DataSource> ds) {
        JDBCConfigurationImpl conf = new JDBCConfigurationImpl();
        conf.setDBDictionary(dict.getName());
        conf.setConnectionDriverName(ds.getName());
        conf.setConnectionURL("jdbc:openjpa-3014:stub");
        return conf;
    }

    /**
     * A data source handing out a connection that answers metadata calls with
     * type appropriate defaults.
     */
    public static class StubDataSource extends AbstractDataSource {

        @Override
        public Connection getConnection() {
            final DatabaseMetaData metaData = (DatabaseMetaData) proxy(DatabaseMetaData.class,
                name -> {
                    switch (name) {
                        case "getDatabaseProductName":
                            return "OpenJPA 3014 stub";
                        case "getDatabaseProductVersion":
                            return "1.0";
                        case "getIdentifierQuoteString":
                            return "\"";
                        case "getURL":
                            return "jdbc:openjpa-3014:stub";
                        default:
                            return null;
                    }
                });
            return (Connection) proxy(Connection.class,
                name -> "getMetaData".equals(name) ? metaData : null);
        }
    }

    /**
     * A data source that cannot connect, as a JNDI lookup that does not
     * resolve or a server that is down leaves one.
     */
    public static class UnreachableDataSource extends AbstractDataSource {

        static final String MESSAGE = "OPENJPA-3014: no connection";

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException(MESSAGE);
        }
    }

    private abstract static class AbstractDataSource implements DataSource {

        /**
         * A proxy answering with what the given function returns, and with a
         * type appropriate default (false / 0 / "" / null) otherwise.
         */
        static Object proxy(final Class<?> iface, final Function<String, Object> answers) {
            return Proxy.newProxyInstance(TestSequenceMappingFallback.class.getClassLoader(),
                new Class<?>[] { iface }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        switch (method.getName()) {
                            case "toString":
                                return "stub " + iface.getSimpleName();
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                break;
                        }
                        Object value = answers.apply(method.getName());
                        return value != null ? value : defaultValue(method.getReturnType());
                    }
                });
        }

        @Override
        public Connection getConnection(String user, String pass) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        static Object defaultValue(Class<?> type) {
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

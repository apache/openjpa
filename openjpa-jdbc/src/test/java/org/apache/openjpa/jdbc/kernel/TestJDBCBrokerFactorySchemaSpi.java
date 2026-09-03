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
package org.apache.openjpa.jdbc.kernel;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.kernel.BrokerImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the {@code BrokerFactory} schema management operations do not
 * instantiate a {@link BrokerImpl} and that they pass the thread context class
 * loader on to the mapping tool (OPENJPA-2962).
 *
 * This test needs no database: {@code synchronizeMappings} is stubbed out.
 */
public class TestJDBCBrokerFactorySchemaSpi {

    static final AtomicInteger CLONES = new AtomicInteger();

    /**
     * Plugin class for {@code openjpa.BrokerImpl}. The factory obtains brokers by
     * cloning the configured template instance, so counting clones counts the
     * brokers a schema operation creates.
     */
    public static class CountingBroker extends BrokerImpl {
        private static final long serialVersionUID = 1L;

        @Override
        public Object clone() throws CloneNotSupportedException {
            CLONES.incrementAndGet();
            return super.clone();
        }
    }

    /**
     * Captures the arguments handed to the mapping synchronization instead of
     * running it against a database.
     */
    static class CapturingFactory extends JDBCBrokerFactory {
        private static final long serialVersionUID = 1L;

        ClassLoader loader;
        String action;
        int calls;

        CapturingFactory(JDBCConfiguration conf) {
            super(conf);
        }

        @Override
        protected boolean synchronizeMappings(ClassLoader loader, JDBCConfiguration conf, String action) {
            this.loader = loader;
            this.action = action;
            this.calls++;
            return false;
        }
    }

    private CapturingFactory factory;

    @Before
    public void setUp() {
        JDBCConfigurationImpl conf = new JDBCConfigurationImpl();
        conf.setBrokerImpl(CountingBroker.class.getName());
        CLONES.set(0);
        factory = new CapturingFactory(conf);
    }

    private void assertNoBroker(String expectedAction) {
        assertEquals("schema management must not instantiate a Broker", 0, CLONES.get());
        assertEquals(1, factory.calls);
        assertSame(Thread.currentThread().getContextClassLoader(), factory.loader);
        assertEquals(expectedAction, factory.action);
    }

    @Test
    public void testCreatePersistenceStructure() {
        factory.createPersistenceStructure(false);
        assertNoBroker("buildSchema(ForeignKeys=true,schemaAction='add')");
    }

    @Test
    public void testCreatePersistenceStructureWithSchemas() {
        factory.createPersistenceStructure(true);
        assertNoBroker("buildSchema(ForeignKeys=true,schemaAction='createDB, add')");
    }

    @Test
    public void testDropPersistenceStructure() {
        factory.dropPersistenceStructure(false);
        assertNoBroker("buildSchema(ForeignKeys=true,schemaAction='drop')");
    }

    @Test
    public void testDropPersistenceStructureWithSchemas() {
        factory.dropPersistenceStructure(true);
        assertNoBroker("buildSchema(ForeignKeys=true,schemaAction='drop, dropDB')");
    }

    @Test
    public void testValidatePersistenceStructure() throws Exception {
        factory.validatePersistenceStructure();
        assertNoBroker("validate(ForeignKeys=true)");
    }

    @Test
    public void testTruncateData() {
        factory.truncateData();
        assertNoBroker("buildSchema(ForeignKeys=true,schemaAction='refresh,deleteTableContents')");
    }
}

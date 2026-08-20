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
package org.apache.openjpa.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.StoreManager;
import org.junit.Test;

import jakarta.persistence.SchemaManager;
import jakarta.persistence.SchemaValidationException;

/**
 * Tests that {@link SchemaManagerImpl} reports a store which does not implement the
 * schema operations as an unsupported operation rather than as a schema failure.
 */
public class TestSchemaManagerImpl {

    /**
     * A minimal, non-JDBC {@link AbstractBrokerFactory} which inherits the default
     * (unsupported) implementations of the four schema operations.
     */
    private static final class NoSchemaBrokerFactory extends AbstractBrokerFactory {
        private static final long serialVersionUID = 1L;

        private NoSchemaBrokerFactory() {
            super(new OpenJPAConfigurationImpl());
        }

        @Override
        protected StoreManager newStoreManager() {
            return null;
        }
    }

    /**
     * A factory whose validation fails the way a real store reports schema drift.
     */
    private static final class FailingBrokerFactory extends AbstractBrokerFactory {
        private static final long serialVersionUID = 1L;

        static final IllegalStateException FAILURE = new IllegalStateException("column FOO is missing");

        private FailingBrokerFactory() {
            super(new OpenJPAConfigurationImpl());
        }

        @Override
        public void validatePersistenceStructure() throws Exception {
            throw FAILURE;
        }

        @Override
        protected StoreManager newStoreManager() {
            return null;
        }
    }

    /**
     * A factory whose validation fails without carrying any message at all.
     */
    private static final class SilentlyFailingBrokerFactory extends AbstractBrokerFactory {
        private static final long serialVersionUID = 1L;

        private SilentlyFailingBrokerFactory() {
            super(new OpenJPAConfigurationImpl());
        }

        @Override
        public void validatePersistenceStructure() throws Exception {
            throw new IllegalStateException();
        }

        @Override
        protected StoreManager newStoreManager() {
            return null;
        }
    }

    @Test
    public void testValidateReportsUnsupportedOperationInsteadOfValidationFailure() {
        SchemaManager schemaManager = new SchemaManagerImpl(new NoSchemaBrokerFactory());

        UnsupportedOperationException uoe =
                assertThrows(UnsupportedOperationException.class, schemaManager::validate);
        assertNotNull("the unsupported operation must carry a message", uoe.getMessage());
        assertTrue(uoe.getMessage(), uoe.getMessage().contains("schema validation"));
    }

    @Test
    public void testCreateDropAndTruncateReportUnsupportedOperation() {
        SchemaManager schemaManager = new SchemaManagerImpl(new NoSchemaBrokerFactory());

        assertThrows(UnsupportedOperationException.class, () -> schemaManager.create(true));
        assertThrows(UnsupportedOperationException.class, () -> schemaManager.drop(true));
        assertThrows(UnsupportedOperationException.class, schemaManager::truncate);
    }

    @Test
    public void testValidateWrapsRealValidationFailures() {
        SchemaManager schemaManager = new SchemaManagerImpl(new FailingBrokerFactory());

        SchemaValidationException sve =
                assertThrows(SchemaValidationException.class, schemaManager::validate);
        assertEquals("Schema could not be validated: column FOO is missing", sve.getMessage());
        assertEquals(1, sve.getFailures().length);
        assertSame(FailingBrokerFactory.FAILURE, sve.getFailures()[0]);
    }

    @Test
    public void testValidateNeverReportsANullMessage() {
        SchemaManager schemaManager = new SchemaManagerImpl(new SilentlyFailingBrokerFactory());

        SchemaValidationException sve =
                assertThrows(SchemaValidationException.class, schemaManager::validate);
        assertEquals("Schema could not be validated: java.lang.IllegalStateException", sve.getMessage());
    }
}

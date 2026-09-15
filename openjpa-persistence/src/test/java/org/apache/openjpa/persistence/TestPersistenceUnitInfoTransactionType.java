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
import static org.junit.Assert.assertNull;

import java.util.Map;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;

import org.junit.Test;

/**
 * The transaction type is kept as {@link PersistenceUnitTransactionType} and only
 * exposed as the deprecated SPI enum through {@link PersistenceUnitInfoImpl#getTransactionType()}.
 */
@SuppressWarnings("removal")
public class TestPersistenceUnitInfoTransactionType {

    @Test
    public void testDefaultIsResourceLocal() {
        PersistenceUnitInfoImpl pinfo = new PersistenceUnitInfoImpl();
        assertEquals(jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL,
            pinfo.getTransactionType());
        assertNull(PersistenceUnitInfoImpl.toOpenJPAProperties(pinfo).get("openjpa.TransactionMode"));
    }

    @Test
    public void testFromPersistenceConfiguration() {
        PersistenceConfiguration config = new PersistenceConfiguration("tx")
            .transactionType(PersistenceUnitTransactionType.JTA);
        PersistenceUnitInfoImpl pinfo = PersistenceUnitInfoImpl.convert(config);
        assertEquals(jakarta.persistence.spi.PersistenceUnitTransactionType.JTA, pinfo.getTransactionType());
        assertEquals("managed", PersistenceUnitInfoImpl.toOpenJPAProperties(pinfo).get("openjpa.TransactionMode"));
    }

    @Test
    public void testFromUserPropertiesAsString() {
        assertJta("JTA");
    }

    @Test
    public void testFromUserPropertiesAsEnum() {
        assertJta(PersistenceUnitTransactionType.JTA);
    }

    @Test
    public void testFromUserPropertiesAsDeprecatedSpiEnum() {
        assertJta(jakarta.persistence.spi.PersistenceUnitTransactionType.JTA);
    }

    @Test
    public void testDeprecatedSetter() {
        PersistenceUnitInfoImpl pinfo = new PersistenceUnitInfoImpl();
        pinfo.setTransactionType(jakarta.persistence.spi.PersistenceUnitTransactionType.JTA);
        assertEquals(jakarta.persistence.spi.PersistenceUnitTransactionType.JTA, pinfo.getTransactionType());
    }

    private static void assertJta(Object value) {
        PersistenceUnitInfoImpl pinfo = new PersistenceUnitInfoImpl();
        pinfo.fromUserProperties(Map.of(JPAProperties.TRANSACTION_TYPE, value));
        assertEquals(jakarta.persistence.spi.PersistenceUnitTransactionType.JTA, pinfo.getTransactionType());
    }
}

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
package org.apache.openjpa.persistence.test;

import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.entity.EntityA;
import org.apache.openjpa.persistence.entity.EntityB;
import org.apache.openjpa.persistence.entity.EntityC;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import java.util.HashMap;
import java.util.Map;

/*
 * This tests that OpenJPA should not be doing anything if another provider is specified.
 */
public class TestPersistenceProviderFilteringTestCase extends SQLListenerTestCase {

    private final String persistenceUnitName = "test";

    @Override
    public void setUp() {
        setUp(DROP_TABLES, EntityA.class, EntityB.class, EntityC.class);

    }

    public void testGenerateSchemaNoProvider() {
        final PersistenceProviderImpl ppi = new PersistenceProviderImpl();
        final Map<Object, Object> map = new HashMap<>();
        assertTrue(ppi.generateSchema(persistenceUnitName, map));
    }

    public void testGenerateSchemaOpenJPAProvider() {
        final PersistenceProviderImpl ppi = new PersistenceProviderImpl();
        final Map<Object, Object> map = new HashMap<>();
        map.put("jakarta.persistence.provider", PersistenceProviderImpl.class.getName());
        assertTrue(ppi.generateSchema(persistenceUnitName, map));
    }

    public void testGenerateSchemaEclipseProvider() {
        final PersistenceProviderImpl ppi = new PersistenceProviderImpl();
        final Map<Object, Object> map = new HashMap<>();
        map.put("jakarta.persistence.provider", "org.eclipse.persistence.jpa.PersistenceProvider");
        assertFalse(ppi.generateSchema(persistenceUnitName, map));
    }

    public void testGenerateSchemaFakeProviderClass() {
        final PersistenceProviderImpl ppi = new PersistenceProviderImpl();
        final Map<Object, Object> map = new HashMap<>();
        map.put("jakarta.persistence.provider", FakeProvider.class);
        assertFalse(ppi.generateSchema(persistenceUnitName, map));
    }


    public static final class FakeProvider implements PersistenceProvider {

        @Override public EntityManagerFactory createEntityManagerFactory(final String s, final Map map) {
            return null;
        }

        @Override public EntityManagerFactory createContainerEntityManagerFactory(
            final PersistenceUnitInfo persistenceUnitInfo, final Map map) {
            return null;
        }

        @Override public void generateSchema(final PersistenceUnitInfo persistenceUnitInfo, final Map map) {

        }

        @Override public boolean generateSchema(final String s, final Map map) {
            return false;
        }

        @Override public ProviderUtil getProviderUtil() {
            return null;
        }
    }
}

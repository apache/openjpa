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
package org.apache.openjpa.lib.conf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that configuration properties can be specified both as new
 * jakarta.persistence.* namespace and old openjpa.* namespace. The two style can
 * also be mixed where one property is in javax.* namespace the other in
 * openjpa.*. But same property can not be specified in both style.
 *
 * Tests with different configuration sources such as persistence.xml, System
 * properties and runtime properties.
 *
 * @author Pinaki Poddar
 *
 */

@DatabasePlatform("org.apache.derby.jdbc.EmbeddedDriver")
class TestEquivalentConfiguration extends SingleEMFTestCase {
    private EntityManagerFactory emf1 = null;

    private Properties _system;

    private static final String OLD_STYLE_URL_KEY    = "openjpa.ConnectionURL";
    private static final String OLD_STYLE_DRIVER_KEY =
        "openjpa.ConnectionDriverName";
    private static final String NEW_STYLE_DRIVER_KEY =
        "jakarta.persistence.jdbc.driver";
    private static final String NEW_STYLE_URL_KEY    =
        "jakarta.persistence.jdbc.url";

    private static final String[] KEYS = {
        OLD_STYLE_DRIVER_KEY, OLD_STYLE_URL_KEY,
        NEW_STYLE_DRIVER_KEY, NEW_STYLE_URL_KEY };

    // NOTE: Non-standard naming because another test in the harness scans all
    // META-INF/persistence.xml and fails as this file contains conflicting
    // property keys for testing.
    private static final String PERSISTENCE_UNIT =
        "org/apache/openjpa/lib/conf/META-INF/persistence-config.xml";

    private static final String OLD_STYLE_UNIT_NAME = "old-style";
    private static final String NEW_STYLE_UNIT_NAME = "new-style";
    private static final String MIXED_STYLE_UNIT_NAME = "mixed-style";
    private static final String CONFLICT_STYLE_UNIT_NAME = "conflict-style";

    private static final String SYSTEM_CONFIGURED_UNIT_NAME  =
        "system-configured";
    private static final String RUNTIME_CONFIGURED_UNIT_NAME =
        "runtime-configured";

    private static final String URL    =
        "jdbc:derby:target/database/openjpa-test-database;create=true";
    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    @Override
    protected boolean isSupported(DBDictionary dict) {
        return dict instanceof DerbyDictionary;
    }

    /**
     * Erase that System properties to ensure they are not impacting the
     * configuration as test harness inserts the System properties for
     * connection parameters.
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Only run on Derby
        _system = backup();
        clear(_system);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (_system != null) {
            restore(_system);
        }
        super.tearDown();
        if (emf1 != null) {
            clear(emf1);
            closeEMF(emf1);
            emf1 = null;
        }
    }

    /**
     * Tests that openjpa.* namespace can be used for persistence.xml.
     */
    @Test
    void oldStylePersistenceUnitConfiguration() {
        emf1 = OpenJPAPersistence.createEntityManagerFactory(OLD_STYLE_UNIT_NAME,
            PERSISTENCE_UNIT);

        assertNotNull(emf1);
        assertTrue(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(OLD_STYLE_URL_KEY));
        assertFalse(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(NEW_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that javax.* namespace can be used for persistence.xml.
     */
    @Test
    void newStylePersistenceUnitConfiguration() {
        emf1 = OpenJPAPersistence.createEntityManagerFactory(NEW_STYLE_UNIT_NAME,
            PERSISTENCE_UNIT);

        assertNotNull(emf1);
        assertTrue(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(NEW_STYLE_URL_KEY));
        assertFalse(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(OLD_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* and javax.* namespace can both be used for
     * persistence.xml.
     */
    @Test
    void mixedStylePersistenceUnitConfiguration() {
        emf1 = OpenJPAPersistence.createEntityManagerFactory(
            MIXED_STYLE_UNIT_NAME, PERSISTENCE_UNIT);

        assertNotNull(emf1);
        assertTrue(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(NEW_STYLE_URL_KEY));
        assertFalse(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(OLD_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* and javax.* namespace can not both be used for the
     * same property in persistence.xml.
     *
     */

    @Test
    void conflictStylePersistenceUnitConfiguration() {
        try {
            emf1 = OpenJPAPersistence.createEntityManagerFactory(
                CONFLICT_STYLE_UNIT_NAME, PERSISTENCE_UNIT);
            fail();
        } catch (RuntimeException ex) {
            // good
        }
    }

    /**
     * Tests that javax.* namespace can be used for System properties.
     */
    @Test
    void newStyleSystemPropertyConfiguration() {
        try {
            System.setProperty(NEW_STYLE_DRIVER_KEY, DRIVER);
            System.setProperty(NEW_STYLE_URL_KEY, URL);

            emf1 = OpenJPAPersistence.createEntityManagerFactory(
                SYSTEM_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT);
        } finally {
            System.getProperties().remove(NEW_STYLE_DRIVER_KEY);
            System.getProperties().remove(NEW_STYLE_URL_KEY);
        }

        assertNotNull(emf1);
        assertTrue(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(NEW_STYLE_URL_KEY));
        assertFalse(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(OLD_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* namespace can be used for System properties.
     */
    @Test
    void oldStyleSystemPropertyConfiguration() {
        try {
            System.setProperty(OLD_STYLE_DRIVER_KEY, DRIVER);
            System.setProperty(OLD_STYLE_URL_KEY, URL);

            emf1 = OpenJPAPersistence.createEntityManagerFactory(
                SYSTEM_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT);
        } finally {
            System.getProperties().remove(OLD_STYLE_DRIVER_KEY);
            System.getProperties().remove(OLD_STYLE_URL_KEY);
        }
        assertNotNull(emf1);
        assertFalse(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(NEW_STYLE_URL_KEY));
        assertTrue(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(OLD_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* and javax.* namespace can both be used for System
     * properties.
     */
    @Test
    void mixedStyleSystemPropertyConfiguration() {
        System.setProperty(OLD_STYLE_DRIVER_KEY, DRIVER);
        System.setProperty(NEW_STYLE_URL_KEY, URL);
        try {
            emf1 = OpenJPAPersistence.createEntityManagerFactory(
                SYSTEM_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT);
        } finally {
            System.getProperties().remove(OLD_STYLE_DRIVER_KEY);
            System.getProperties().remove(NEW_STYLE_URL_KEY);
        }

        assertNotNull(emf1);
        assertFalse(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(NEW_STYLE_URL_KEY));
        assertTrue(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(OLD_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* and javax.* namespace can not both be used for the
     * same property in System configuration.
     */
    @Test
    void conflictStyleSystemPropertyConfiguration() {
        System.setProperty(OLD_STYLE_DRIVER_KEY, DRIVER);
        System.setProperty(NEW_STYLE_DRIVER_KEY, DRIVER);
        try {
            emf1 = OpenJPAPersistence.createEntityManagerFactory(
                SYSTEM_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT);
            fail();
        } catch (RuntimeException ex) {
            // good
        } finally {
            System.getProperties().remove(OLD_STYLE_DRIVER_KEY);
            System.getProperties().remove(NEW_STYLE_DRIVER_KEY);
        }
    }

    /**
     * Tests that openjpa.* namespace can be used for runtime configuration.
     */
    @Test
    void oldStyleRuntimePropertyConfiguration() {
        Properties conf = new Properties();
        conf.setProperty(OLD_STYLE_DRIVER_KEY, DRIVER);
        conf.setProperty(OLD_STYLE_URL_KEY, URL);

        emf1 = OpenJPAPersistence.createEntityManagerFactory(
                RUNTIME_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT, conf);

        assertNotNull(emf1);
        assertTrue(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(OLD_STYLE_URL_KEY));
        assertFalse(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(NEW_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that javax.* namespace can be used for runtime configuration.
     */
    @Test
    void newStyleRuntimePropertyConfiguration() {
        Properties conf = new Properties();
        conf.setProperty(NEW_STYLE_DRIVER_KEY, DRIVER);
        conf.setProperty(NEW_STYLE_URL_KEY, URL);

        emf1 = OpenJPAPersistence.createEntityManagerFactory(
            RUNTIME_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT, conf);

        assertNotNull(emf1);
        assertFalse(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(OLD_STYLE_URL_KEY));
        assertTrue(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(NEW_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* and javax.* namespace can both be used for runtime
     * configuration.
     */
    @Test
    void mixedStyleRuntimePropertyConfiguration() {
        Properties conf = new Properties();
        conf.setProperty(OLD_STYLE_DRIVER_KEY, DRIVER);
        conf.setProperty(NEW_STYLE_URL_KEY, URL);

        emf1 = OpenJPAPersistence.createEntityManagerFactory(
            RUNTIME_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT, conf);

        assertNotNull(emf1);
        assertTrue(containsProperty(OLD_STYLE_DRIVER_KEY));
        assertFalse(containsProperty(OLD_STYLE_URL_KEY));
        assertFalse(containsProperty(NEW_STYLE_DRIVER_KEY));
        assertTrue(containsProperty(NEW_STYLE_URL_KEY));

        verifyDatabaseConnection();
    }

    /**
     * Tests that openjpa.* and javax.* namespace can not both be used for the
     * same property in runtime configuration.
     */
    @Test
    void conflictStyleRuntimePropertyConfiguration() {
        Properties conf = new Properties();
        conf.setProperty(OLD_STYLE_DRIVER_KEY, DRIVER);
        conf.setProperty(NEW_STYLE_DRIVER_KEY, DRIVER);

        try {
            emf1 = OpenJPAPersistence.createEntityManagerFactory(
                RUNTIME_CONFIGURED_UNIT_NAME, PERSISTENCE_UNIT, conf);
            fail();
        } catch (RuntimeException ex) {
            // good
        }
    }

    void verifyDatabaseConnection() {
        String driver = OpenJPAPersistence.cast(emf1).getConfiguration()
                .getConnectionDriverName();

        EntityManager em = emf1.createEntityManager();
        em.getTransaction().begin();
        em.flush();
        em.getTransaction().commit();
    }

    boolean containsProperty(String key) {
        return getConfiguration().toProperties(true).containsKey(key);
    }

    OpenJPAConfiguration getConfiguration() {
        return ((OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.cast(emf1))
            .getConfiguration();
    }

    Properties backup() {
        Properties p = new Properties();
        for (String key : KEYS) {
            if (System.getProperty(key) != null) {
                p.setProperty(key, System.getProperty(key));
            }
        }
        return p;
    }

    private void clear(Properties p) {
        for (Object key : p.keySet()) {
            System.getProperties().remove(key);
        }
    }

    private void restore(Properties p) {
        for (Object key : p.keySet()) {
            System.setProperty(key.toString(), p.get(key).toString());
        }
    }
}

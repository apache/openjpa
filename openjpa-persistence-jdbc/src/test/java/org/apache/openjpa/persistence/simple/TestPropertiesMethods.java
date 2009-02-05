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
package org.apache.openjpa.persistence.simple;

import java.util.Map;
import java.util.Set;

import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.lib.conf.Value;
import org.apache.openjpa.persistence.AutoClearType;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * This test case tests the getProperties() and getSupportedProperties() methods
 * for the EntityManager and EntityManagerFactory.
 * 
 * @author Dianne Richards
 * 
 */
public class TestPropertiesMethods extends SingleEMFTestCase {
    OpenJPAEntityManager em;

    public void setUp() throws Exception {
        setUp("openjpa.DataCacheTimeout", "3",
            "openjpa.ConnectionURL",
            "jdbc:derby:target/database/jpa-test-database;create=true");
        assertNotNull(emf);
        em = emf.createEntityManager();
        assertNotNull(em);
    }
    
    /**
     * Test the EntityManager getProperties() method.
     */
    public void testEMGetProperties() {
        Map<String, Object> emProperties = em.getProperties();

        // First, check a default property
        String autoClear = (String) emProperties.get("openjpa.AutoClear");
        assertEquals(String.valueOf(AutoClear.CLEAR_DATASTORE), autoClear);
        
        // Next, check that the correct property key is returned for
        // some properties that can have 2 keys. The success of this test
        // case is dependent on the connection system values that are set
        // in the pom.xml file for the test harness. It assumes that the
        // system value keys are javax.persistence.jdbc.driver and 
        // openjpa.ConnectionProperties. If either one of these are changed,
        // this test case may fail.
        String javaxConnectionDriver =
            (String) emProperties.get("javax.persistence.jdbc.driver");
        assertNotNull(javaxConnectionDriver);
        String openjpaConnectionURL =
            (String) emProperties.get("openjpa.ConnectionURL");
        assertNotNull(openjpaConnectionURL);
        
        // Next, check that the javax.persistent property is returned instead
        // of the corresponding openjpa one when no value has been set.
        boolean javaxUserNameExists =
            emProperties.containsKey("javax.persistence.jdbc.user");
        assertTrue(javaxUserNameExists);
        boolean openjpaUserNameExists =
            emProperties.containsKey("openjpaConnectionUserName");
        assertFalse(openjpaUserNameExists);
        
        // Next, change a property and check for the changed value
        em.setAutoClear(AutoClearType.ALL);
        emProperties = em.getProperties();
        autoClear = (String) emProperties.get("openjpa.AutoClear");
        assertEquals(String.valueOf(AutoClear.CLEAR_ALL), autoClear);
        
        // Make sure the password property is not returned.
        boolean javaxPasswordExists =
            emProperties.containsKey("javax.persistence.jdbc.password");
        assertFalse(javaxPasswordExists);
        boolean openjpaPasswordExists =
            emProperties.containsKey("openjpa.ConnectionPassword");
        assertFalse(openjpaPasswordExists);
        
        // Add a dummy javax.persistence... equivalent key to one of the
        // values that can be changed to force the code down a specific path.
        Value autoClearValue = emf.getConfiguration().getValue("AutoClear");
        assertNotNull(autoClearValue);
        autoClearValue.addEquivalentKey("javax.persistence.AutoClear");
        emProperties = em.getProperties();
        assertFalse(emProperties.containsKey("openjpa.AutoClear"));
        assertTrue(emProperties.containsKey("javax.persistence.AutoClear"));
    }

    /**
     * Test the EntityManagerFactory getProperties() method.
     */
    public void testEMFGetProperties() {
        Map<String, Object> emfProperties = emf.getProperties();

        // First, check a default property
        String dataCacheManager =
            (String) emfProperties.get("openjpa.DataCacheManager");
        assertEquals("default", dataCacheManager);

        // Next, check a property that was set during emf creation
        String dataCacheTimeout =
            (String) emfProperties.get("openjpa.DataCacheTimeout");
        assertEquals(3, Integer.valueOf(dataCacheTimeout).intValue());

        // Next get the Platform value set by the JDBCBrokerFactory
        // or possibly a subclass
        String platform = (String) emfProperties.get("Platform");
        assertNotNull(platform);

        // Next get one of the values set by the AbstractBrokerFactory
        // or possibly a subclass
        String vendorName = (String) emfProperties.get("VendorName");
        assertNotNull(vendorName);
    }

    /**
     * Test the EntityManagerFactory getSupportedProperties() method.
     */
    public void testEMFGetSupportedProperties() {
        Set<String> emfSupportedProperties = emf.getSupportedProperties();
        assertNotNull(emfSupportedProperties);
        assertTrue(emfSupportedProperties.contains("openjpa.IgnoreChanges"));
    }

    /**
     * Test the EntityManager getSupportedProperties() method.
     */
    public void testEMGetSupportedProperties() {
        Set<String> emSupportedProperties = em.getSupportedProperties();
        assertNotNull(emSupportedProperties);
        assertTrue(emSupportedProperties.contains("openjpa.AutoDetach"));
        
        // Make sure the all possible keys are returned
        assertTrue(emSupportedProperties.contains(
            "javax.persistence.lock.timeout"));
        assertTrue(emSupportedProperties.contains("openjpa.LockTimeout"));
        
        // Make sure the spec property for query timeout, that only has one
        // key, is returned.
        assertTrue(emSupportedProperties.contains(
            "javax.persistence.query.timeout"));
    }

}

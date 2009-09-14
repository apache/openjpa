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
package org.apache.openjpa.jdbc.writebehind;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.PersistenceException;
import org.apache.openjpa.writebehind.WriteBehindConfigurationException;


/** 
 * <b>TestWriteBehindConfigurations</b> is used to create various configurations for the new JPA 2.0
 * WriteBehind capability to ensure that the appropriate exception is thrown for the invalid
 * configurations and an EntityManagerFactory is instantiated for the valid configurations
 */
public class TestWriteBehindConfigurations extends TestCase {


    /**
     * Invalid configuration since there is no DataCache specified
     */
    public void testInvalidConfiguration01() {
        EntityManagerFactory emf01 = null;
        Map map = new HashMap();
        map.put( "openjpa.WriteBehindCache", "true");
        map.put( "openjpa.WriteBehindCallback", "true" );
        map.put( "openjpa.RuntimeUnenhancedClasses", "supported" );
        try {
            emf01 = Persistence.createEntityManagerFactory("empty-pu", map);
            fail("testInvalidConfiguration01: Exception expected but not thrown");
        }
        catch (ArgumentException ae) {
        }
        catch (PersistenceException pe) {
        }
        catch (WriteBehindConfigurationException wbce) {
        }
        catch (Exception e) {
            fail("testInvalidConfiguration01: Caught unexpected exception: " + e);
            e.printStackTrace();
        }
        finally {
            assertNull(emf01);
            if (emf01 != null) {
                emf01.close();
            }
        }
    }  


    /**
     * Invalid configuration since there is no DataCache remote commit provider specified
     */
    public void testInvalidConfiguration02() {
        EntityManagerFactory emf02 = null;
        Map map = new HashMap();
        map.put( "openjpa.DataCache", "true" );
        map.put( "openjpa.WriteBehindCache", "true");
        map.put( "openjpa.WriteBehindCallback", "true(sleepTime=30000)" );
        map.put( "openjpa.RuntimeUnenhancedClasses", "unsupported" );
        try {
            emf02 = Persistence.createEntityManagerFactory("empty-pu", map);
            fail("testInvalidConfiguration02: Exception expected but not thrown");
        }
        catch (ArgumentException ae) {
        }
        catch (PersistenceException pe) {
        }
        catch (WriteBehindConfigurationException wbce) {
        }
        catch (Exception e) {
            fail("testInvalidConfiguration02: Caught unexpected exception: " + e);
            e.printStackTrace();
        }
        finally {
            assertNull(emf02);
            if (emf02 != null) {
                emf02.close();
            }
        }
    }  


    /**
     * Invalid configuration since there is no WriteBehindCallback specified
     */
    public void testInvalidConfiguration03() {
        EntityManagerFactory emf03 = null;
        Map map = new HashMap();
        map.put( "openjpa.DataCache", "true" );
        map.put( "openjpa.RemoteCommitProvider", "sjvm");
        map.put( "openjpa.WriteBehindCache", "true");
        map.put( "openjpa.RuntimeUnenhancedClasses", "warn" );
        try {
            emf03 = Persistence.createEntityManagerFactory("empty-pu", map);
            fail("testInvalidConfiguration03: Exception not thrown");
        }
        catch (ArgumentException ae) {
        }
        catch (PersistenceException pe) {
        }
        catch (WriteBehindConfigurationException wbce) {
        }
        catch (Exception e) {
            fail("testInvalidConfiguration03: Caught unexpected exception: " + e);
            e.printStackTrace();
        }
        finally {
            assertNull(emf03);
            if (emf03 != null) {
                emf03.close();
            }
        }
    }  


    /**
     * Invalid configuration since there is no DataCache remote commit provider nor a 
     * WriteBehindCallback specified
     */
    public void testInvalidConfiguration04() {
        EntityManagerFactory emf04 = null;
        Map map = new HashMap();
        map.put( "openjpa.DataCache", "true" );
        map.put( "openjpa.WriteBehindCache", "true");
        try {
            emf04 = Persistence.createEntityManagerFactory("empty-pu", map);
            fail("testInvalidConfiguration04: Exception not thrown");
        }
        catch (ArgumentException ae) {
        }
        catch (PersistenceException pe) {
        }
        catch (WriteBehindConfigurationException wbce) {
        }
        catch (Exception e) {
            fail("testInvalidConfiguration04: Caught unexpected exception: " + e);
            e.printStackTrace();
        }
        finally {
            assertNull(emf04);
            if (emf04 != null) {
                emf04.close();
            }
        }
    }  


    /**
     * Valid configuration 
     */
    public void testValidConfiguration01() {
        EntityManagerFactory emf05 = null;
        Map map = new HashMap();
        map.put( "openjpa.DataCache", "true" );
        map.put( "openjpa.RemoteCommitProvider", "sjvm");
        map.put( "openjpa.WriteBehindCache", "true");
        map.put( "openjpa.WriteBehindCallback", "true" );
        map.put( "openjpa.RuntimeUnenhancedClasses", "unsupported" );
        try {
            emf05 = Persistence.createEntityManagerFactory("empty-pu", map);
        }
        catch (Exception e) {
            fail("testValidConfiguration01: Caught unexpected exception: " + e);
            e.printStackTrace();
        }
        finally {
            assertNotNull(emf05);
            if (emf05 != null) {
                emf05.close();
            }
        }
    }  


    /**
     * Valid configuration 
     */
    public void testValidConfiguration02() {
        EntityManagerFactory emf06 = null;
        Map map = new HashMap();
        map.put( "openjpa.DataCache", "true" );
        map.put( "openjpa.RemoteCommitProvider", "sjvm");
        map.put( "openjpa.WriteBehindCache", "true");
        map.put( "openjpa.WriteBehindCallback", "true(sleepTime=30000)" );
        map.put( "openjpa.RuntimeUnenhancedClasses", "supported" );
        try {
            emf06 = Persistence.createEntityManagerFactory("empty-pu", map);
        }
        catch (Exception e) {
            fail("testValidConfiguration02: Caught unexpected exception: " + e);
            e.printStackTrace();
        }
        finally {
            assertNotNull(emf06);
            if (emf06 != null) {
                emf06.close();
            }
        }
    }  


    /**
     * Valid configuration 
     */
    public void testValidConfiguration03() {
        EntityManagerFactory emf07 = null;
        Map map = new HashMap();
        map.put( "openjpa.DataCache", "true" );
        map.put( "openjpa.RemoteCommitProvider", "sjvm");
        map.put( "openjpa.WriteBehindCache", "true");
        map.put( "openjpa.WriteBehindCallback", "true(sleepTime=30000)" );
        map.put( "openjpa.RuntimeUnenhancedClasses", "warn" );
        try {
            emf07 = Persistence.createEntityManagerFactory("empty-pu", map);
        }
        catch (Exception e) {
            fail("testValidConfiguration03: Caught unexpected exception: " + e);
            e.printStackTrace();
        }                                   
        finally {
            assertNotNull(emf07);
            if (emf07 != null) {
                emf07.close();
            }
        }
    }  
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.openjpa.integration.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.ValidationMode;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.validation.ValidatingLifecycleEventManager;

/**
 * Tests the new Bean Validation Factory support in the JPA 2.0 spec by
 * focusing on the following Validation Provider scenarios:
 *   1) Mode of NONE will create a LifecycleEventManager
 *   2) Mode of AUTO will create a ValidatingLifecycleEventManager
 *   3) Mode of CALLBACK will create a ValidatingLifecycleEventManager
 *   4) Verify a passed in ValidatorFactory is used
 * 
 * @version $Rev$ $Date$
 */
public class TestValidatingLEM extends SingleEMFTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, SimpleEntity.class);

        EntityManager em = null;
        // create some initial entities
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            getLog().trace("setup() - creating 1 SimpleEntity");
            em.getTransaction().begin();
            SimpleEntity se = new SimpleEntity("entity","1");
            em.persist(se);
            em.getTransaction().commit();
        } catch (Exception e) {
            fail("setup() - Unexpected Exception - " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   1) Mode of NONE will create a LifecycleEventManager
     */
    public void testValidatingLEM1() {
        getLog().trace("testValidatingLEM1() - NONE");
        OpenJPAEntityManagerFactory emf = null;

        // create our EMF
        emf = OpenJPAPersistence.createEntityManagerFactory(
            "simple-none-mode",
            "org/apache/openjpa/integration/validation/persistence.xml");
        assertNotNull(emf);
        // create EM
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            Query q = em.createNamedQuery("FindAll");
            @SuppressWarnings("unchecked")
            List results = q.getResultList();

            // verify created LifecycleEventManager type
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("NONE"));
//            Class<?> lem = conf.getLifecycleEventManagerInstance().getClass();
            LifecycleEventManager lem = conf.getLifecycleEventManagerInstance();
            assertNotNull(lem);
            System.out.println("**** LEM=" + lem.toString());
//            assertFalse("Expected a LifecycleEventManager instance", 
//                ValidatingLifecycleEventManager.class.isAssignableFrom(lem));
            ValidatingLifecycleEventManager vlem =
                (ValidatingLifecycleEventManager)lem;
            System.out.println("**** VLEM=" + vlem.toString());
        } catch (Exception e) {
            fail("Unexpected testValidatingLEM1() exception = " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   2) An invalid ValidationFactory with a mode of NONE will not
     *      cause an exception
     */
    public void XXXtestValidatingLEM2() {
        getLog().trace("testValidatingLEM2() - ignored invalid factory");
        OpenJPAEntityManagerFactory emf = null;

        // create the Map to test overrides
        //   Just use current class object, as we have no provider to test with
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("javax.persistence.validation.factory",
            this.getClass());

        // create our EMF
        emf = OpenJPAPersistence.createEntityManagerFactory(
            "simple-none-mode",
            "org/apache/openjpa/integration/validation/persistence.xml",
            props);
        assertNotNull(emf);
        // verify same "validation factory" object is returned
        OpenJPAConfiguration conf = emf.getConfiguration();
        assertNotNull(conf);
        assertEquals("ValidationFactory", 
            this.getClass(),
            conf.getValidationFactoryInstance());
    }

    /**
     * Scenario being tested:
     *   3) An invalid ValidationFactory with a mode of AUTO will not
     *      cause an exception
     */
    public void XXXtestValidatingLEM3() {
        getLog().trace("testValidatingLEM3() - optional invalid factory");
        OpenJPAEntityManagerFactory emf = null;

        // create the Map to test overrides
        //   Just use current class object, as we have no provider to test with
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("javax.persistence.validation.factory",
            this.getClass());

        // create our EMF
        emf = OpenJPAPersistence.createEntityManagerFactory(
            "simple-auto-mode",
            "org/apache/openjpa/integration/validation/persistence.xml",
            props);
        assertNotNull(emf);
        // verify same "validation factory" object is returned
        OpenJPAConfiguration conf = emf.getConfiguration();
        assertNotNull(conf);
        assertEquals("ValidationFactory", 
            this.getClass(),
            conf.getValidationFactoryInstance());
    }

    /**
     * Scenario being tested:
     *   4) An invalid ValidationFactory with a mode of CALLBACK will
     *      cause an exception
     */
    //@AllowFailure(message="This will fail until OPENJPA-1111 is resolved.")
    public void XXXtestValidatingLEM4() {
        getLog().trace("testValidatingLEM4() - required invalid factory");
        OpenJPAEntityManagerFactory emf = null;

        // create the Map to test overrides
        //   Just use current class object, as we have no provider to test with
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("javax.persistence.validation.factory",
            this.getClass());
        props.put("javax.persistence.validation.mode",
            String.valueOf(ValidationMode.CALLBACK));

        try {
            // create our EMF
            emf = OpenJPAPersistence.createEntityManagerFactory(
                "simple-callback-mode",
                "org/apache/openjpa/integration/validation/persistence.xml",
                props);
            assertNotNull(emf);
            // verify validation mode
            OpenJPAConfiguration conf = emf.getConfiguration();
            assertNotNull(conf);
            assertEquals("Validation mode", 
                String.valueOf(ValidationMode.CALLBACK),
                conf.getValidationMode());
            fail("Expected an exception when ValidationMode=CALLBACK and " +
                "an invalid ValidatorFactory is provided.");
        } catch (Exception e) {
            // expected
            getLog().trace("testValidatingLEM4() - caught expected " +
                "exception", e);
        }
    }


    /**
     * Internal convenience method for getting the OpenJPA logger
     * 
     * @return
     */
    private Log getLog() {
        return emf.getConfiguration().getLog("Tests");
    }
}

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

import javax.persistence.ValidationMode;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests the new Bean Validation constraint support in the JPA 2.0 spec by
 * focusing on the following Validation scenarios:
 *   1) Test @Null constraint exception on variables in mode=AUTO
 *   2) Test @NotNull constraint exception on variables in mode=AUTO
 *   3) Test no constraint exception when mode=NONE
 *    
 * @version $Rev$ $Date$
 */
public class TestConstraints extends SingleEMFTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, ConstraintNull.class);
    }

    /**
     * Scenario being tested:
     *   1) Test @Null constraint exception on variables in mode=AUTO
     */
    public void testNullConstraint() {
        getLog().trace("testNullConstraint() started");
        // create EM from default EMF
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("AUTO"));
            // create invalid ConstraintNull instance
            em.getTransaction().begin();
            ConstraintNull c = ConstraintNull.createInvalidNull();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testNullConstraint() failed");
            fail("Expected a Validation exception");
        } catch (Exception e) {
            // expected
            getLog().trace("Caught expected exception = " + e);
            getLog().trace("testNullConstraint() passed");
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   2) Test @NotNull constraint exception on variables in mode=AUTO
     */
    public void testNotNullConstraint() {
        getLog().trace("testNotNullConstraint() started");
        // create EM from default EMF
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("AUTO"));
            // create invalid ConstraintNull instance
            em.getTransaction().begin();
            ConstraintNull c = ConstraintNull.createInvalidNotNull();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testNotNullConstraint() failed");
            fail("Expected a Validation exception");
        } catch (Exception e) {
            // expected
            getLog().trace("Caught expected exception = " + e);
            getLog().trace("testNotNullConstraint() passed");
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   3) Test no constraint exception when mode=NONE
     */
    public void testNullConstraintIgnored() {
        getLog().trace("testNullConstraintIgnored() started");
        // create our EMF w/ props
        OpenJPAEntityManagerFactory emf = OpenJPAPersistence
            .createEntityManagerFactory(
                "null-none-mode",
                "org/apache/openjpa/integration/validation/persistence.xml");
        assertNotNull(emf);
        // create EM
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("NONE"));
            // create invalid ConstraintNull instance
            em.getTransaction().begin();
            ConstraintNull c = ConstraintNull.createInvalidNull();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testNullConstraintIgnored() passed");
        } catch (Exception e) {
            // unexpected
            getLog().trace("testNullConstraintIgnored() failed");
            fail("Unexpected Validation exception = " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
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

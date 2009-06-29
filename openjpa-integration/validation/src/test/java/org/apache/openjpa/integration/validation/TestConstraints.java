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

import javax.persistence.Query;
import javax.persistence.ValidationMode;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests the new Bean Validation constraint support in the JPA 2.0 spec by
 * focusing on the following Validation scenarios:
 *      
 *   Check special update/delete/ignore cases once:
 *   1) Update @Null constraint exception on variables in mode=AUTO
 *      Tests that a constraint violation will occur on invalid update.
 *   2) No invalid Delete @Null constraint exception when mode=AUTO
 *      Tests that a violation will not occur when deleting invalid entity.
 *   3) No invalid Persist constraint exception when mode=NONE
 *      Tests that no Validation Providers are used when disabled.
 *   
 *   Basic constraint tests for violation exceptions:
 *   4) Persist @Null constraint exception on variables in mode=AUTO
 *   5) Persist @NotNull constraint exception on variables in mode=AUTO
 *   7) Test @AssertTrue constraint exception on variables in mode=AUTO
 *   8) Test @AssertFalse constraint exception on variables in mode=AUTO
 *   
 *   Basic constraint test for no violations:
 *   6) Persist @NotNull and @Null constraints pass in mode=AUTO
 *   9) Test @AssertFalse and @AssertTrue constraints pass in mode=AUTO
 *
 * @version $Rev$ $Date$
 */
public class TestConstraints extends SingleEMFTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES,
            ConstraintNull.class, ConstraintBoolean.class);
    }

    /**
     * Scenario being tested:
     *   1) Update @Null constraint exception on variables in mode=AUTO
     *      Tests that a constraint violation will occur on invalid update.
     */
    public void testNullUpdateConstraint() {
        getLog().trace("testNullUpdateConstraint() started");
        
        // Part 1 - Create and persist a valid entity
        // create EM from default EMF
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("AUTO"));
            // create valid ConstraintNull instance
            em.getTransaction().begin();
            ConstraintNull c = ConstraintNull.createValid();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testNullUpdateConstraint() Part 1 of 2 passed");
        } catch (Exception e) {
            // unexpected
            getLog().trace("testNullUpdateConstraint() Part 1 of 2 failed");
            fail("Caught unexpected exception = " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
        
        // Part 2 - Verify that invalid properties are caught on an update
        // create EM from default EMF
        em = emf.createEntityManager();
        assertNotNull(em);        
        try {
            // update entity to be invalid
            ConstraintNull c = em.find(ConstraintNull.class, new Integer(1));
            em.getTransaction().begin();
            c.setNullRequired(new String("not null"));
            em.flush();
            em.getTransaction().commit();            
            getLog().trace("testNullUpdateConstraint() Part 2 of 2 failed");
            fail("Expected a Validation exception");
        } catch (Exception e) {
            // expected
            getLog().trace("Caught expected exception = " + e);
            getLog().trace("testNullUpdateConstraint() Part 2 of 2 passed");
        } finally {
            if ((em != null) && em.isOpen()) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   2) No invalid Delete @Null constraint exception when mode=AUTO
     *      Tests that a violation will not occur when deleting invalid entity.
     */
    public void testNullDeleteIgnored() {
        getLog().trace("testNullDeleteIgnored() started");
        
        // Part 1 - Create an invalid entity
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
            getLog().trace("testNullDeleteIgnored() Part 1 of 2 passed");
        } catch (Exception e) {
            // unexpected
            getLog().trace("testNullDeleteIgnored() Part 1 of 2 failed");
            fail("Unexpected Validation exception = " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }

        // Part 2 - Verify delete using default group does not cause Validation
        // create our EMF w/ validation mode=CALLBACK
        emf = OpenJPAPersistence.createEntityManagerFactory(
                "null-callback-mode",
                "org/apache/openjpa/integration/validation/persistence.xml");
        assertNotNull(emf);
        // create EM
        em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("CALLBACK"));
            // get the invalid entity to delete
            Query q = em.createQuery("DELETE FROM VNULL c WHERE c.id = 1");
            em.getTransaction().begin();
            int count = q.executeUpdate();
            em.getTransaction().commit();
            getLog().trace("testNullDeleteIgnored() Part 2 of 2 passed");
        } catch (Exception e) {
            // unexpected
            getLog().trace("testNullDeleteIgnored() Part 2 of 2 failed");
            fail("Unexpected Validation exception = " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }
    }
    
    /**
     * Scenario being tested:
     *   3) No invalid Persist constraint exception when mode=NONE
     *      Tests that no Validation Providers are used when disabled.
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
     * Scenario being tested:
     *   4) Test @Null constraint exception on variables in mode=AUTO
     *      Basic constraint test for a violation exception.
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
     *   5) Test @NotNull constraint exception on variables in mode=AUTO
     *      Basic constraint test for a violation exception.
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
     *   6) Test @NotNull and @Null constraints pass in mode=AUTO
     *      Basic constraint test for no violations.
     */
    public void testNullNotNullConstraint() {
        getLog().trace("testNullNotNullConstraint() started");
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
            ConstraintNull c = ConstraintNull.createValid();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testNullNotNullConstraint() passed");
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception = " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   7) Test @AssertTrue constraint exception on variables in mode=AUTO
     *      Basic constraint test for a violation exception.
     */
    public void testAssertTrueConstraint() {
        getLog().trace("testAssertTrueConstraint() started");
        // create EM from default EMF
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("AUTO"));
            // create invalid ConstraintBoolean instance
            em.getTransaction().begin();
            ConstraintBoolean c = ConstraintBoolean.createInvalidTrue();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testAssertTrueConstraint() failed");
            fail("Expected a Validation exception");
        } catch (Exception e) {
            // expected
            getLog().trace("Caught expected exception = " + e);
            getLog().trace("testAssertTrueConstraint() passed");
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   8) Test @AssertFalse constraint exception on variables in mode=AUTO
     *      Basic constraint test for a violation exception.
     */
    public void testAssertFalseConstraint() {
        getLog().trace("testAssertFalseConstraint() started");
        // create EM from default EMF
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("AUTO"));
            // create invalid ConstraintBoolean instance
            em.getTransaction().begin();
            ConstraintBoolean c = ConstraintBoolean.createInvalidFalse();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testAssertFalseConstraint() failed");
            fail("Expected a Validation exception");
        } catch (Exception e) {
            // expected
            getLog().trace("Caught expected exception = " + e);
            getLog().trace("testAssertFalseConstraint() passed");
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested:
     *   9) Test @AssertFalse and @AssertTrue constraints pass in mode=AUTO
     *      Basic constraint test for no violations.
     */
    public void testAssertTrueFalseConstraint() {
        getLog().trace("testAssertTrueFalseConstraint() started");
        // create EM from default EMF
        OpenJPAEntityManager em = emf.createEntityManager();
        assertNotNull(em);
        try {
            // verify Validation Mode
            OpenJPAConfiguration conf = em.getConfiguration();
            assertNotNull(conf);
            assertTrue("ValidationMode",
                conf.getValidationMode().equalsIgnoreCase("AUTO"));
            // create valid ConstraintBoolean instance
            em.getTransaction().begin();
            ConstraintBoolean c = ConstraintBoolean.createValid();
            em.persist(c);
            em.getTransaction().commit();
            getLog().trace("testAssertTrueFalseConstraint() passed");
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception = " + e);
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

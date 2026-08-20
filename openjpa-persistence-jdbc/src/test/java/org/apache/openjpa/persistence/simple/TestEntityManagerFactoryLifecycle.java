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

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for EMF lifecycle behavior per JPA 3.2 spec:
 * <ul>
 *   <li>createEntityManager(SynchronizationType) on RESOURCE_LOCAL EMF
 *       must throw IllegalStateException</li>
 *   <li>Various methods on a closed EMF must throw IllegalStateException</li>
 * </ul>
 */
public class TestEntityManagerFactoryLifecycle extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(AllFieldTypes.class);
    }

    /**
     * Per JPA spec 7.3.1, createEntityManager(SynchronizationType) with
     * UNSYNCHRONIZED on a RESOURCE_LOCAL EMF must throw IllegalStateException.
     */
    public void testCreateEMSyncTypeUnsynchronizedThrowsIllegalState() {
        try {
            emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED);
            fail("createEntityManager(UNSYNCHRONIZED) on RESOURCE_LOCAL EMF "
                + "should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * Per JPA spec 7.3.1, createEntityManager(SynchronizationType, Map) with
     * UNSYNCHRONIZED on a RESOURCE_LOCAL EMF must throw IllegalStateException.
     */
    public void testCreateEMSyncTypeMapUnsynchronizedThrowsIllegalState() {
        Map<String, Object> props = new HashMap<>();
        try {
            emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED, props);
            fail("createEntityManager(UNSYNCHRONIZED, Map) on RESOURCE_LOCAL "
                + "EMF should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * getProperties() on a closed EMF must throw IllegalStateException.
     */
    public void testGetPropertiesAfterCloseThrowsIllegalState() {
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.getProperties();
            fail("getProperties() on closed EMF should throw "
                + "IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * getPersistenceUnitUtil() on a closed EMF must throw
     * IllegalStateException.
     */
    public void testGetPersistenceUnitUtilAfterCloseThrowsIllegalState() {
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.getPersistenceUnitUtil();
            fail("getPersistenceUnitUtil() on closed EMF should throw "
                + "IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * close() on an already-closed EMF must throw IllegalStateException.
     */
    public void testCloseAfterCloseThrowsIllegalState() {
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.close();
            fail("close() on already-closed EMF should throw "
                + "IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * getCriteriaBuilder() on a closed EMF must throw IllegalStateException.
     */
    public void testGetCriteriaBuilderAfterCloseThrowsIllegalState() {
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.getCriteriaBuilder();
            fail("getCriteriaBuilder() on closed EMF should throw "
                + "IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * createEntityManager() on a closed EMF must throw IllegalStateException.
     */
    public void testCreateEMAfterCloseThrowsIllegalState() {
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.createEntityManager();
            fail("createEntityManager() on closed EMF should throw "
                + "IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * createEntityManager(SynchronizationType) on a closed EMF must throw
     * IllegalStateException (regardless of sync type).
     */
    public void testCreateEMSyncTypeAfterCloseThrowsIllegalState() {
        EntityManagerFactory separateEmf = createEMF(AllFieldTypes.class);
        assertNotNull(separateEmf);
        assertTrue(separateEmf.isOpen());

        separateEmf.close();
        assertFalse(separateEmf.isOpen());

        try {
            separateEmf.createEntityManager(
                SynchronizationType.UNSYNCHRONIZED);
            fail("createEntityManager(SynchronizationType) on closed EMF "
                + "should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }
}

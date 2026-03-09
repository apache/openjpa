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

import java.util.Arrays;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that @ElementCollection works with runtime subclass enhancement.
 * Mirrors TCK ee.jakarta.tck.persistence.core.annotations.elementcollection.Client2.
 */
public class TestElementCollectionBasic extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            UnenhancedElementCollectionEntity.class,
            "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * Mirrors TCK elementCollectionBasicType.
     * Persist entity with @ElementCollection List<String>, find, verify list is populated.
     */
    public void testElementCollectionBasicType() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        UnenhancedElementCollectionEntity expected =
            new UnenhancedElementCollectionEntity();
        expected.setId("1");
        expected.setPhones(Arrays.asList("781-442-2010", "781-442-2011", "781-442-2012"));
        em.persist(expected);
        em.flush();
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        UnenhancedElementCollectionEntity cust =
            em.find(UnenhancedElementCollectionEntity.class, "1");
        assertNotNull("Entity should be found", cust);

        // This mirrors TCK toString() which accesses this.phones directly
        // and would NPE if phones is null
        String str = cust.toString();
        assertNotNull(str);

        assertNotNull("phones should not be null", cust.getPhones());
        assertEquals(3, cust.getPhones().size());
        assertTrue(cust.getPhones().contains("781-442-2010"));
        assertTrue(cust.getPhones().contains("781-442-2011"));
        assertTrue(cust.getPhones().contains("781-442-2012"));

        em.getTransaction().commit();
        em.close();
    }
}

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
package org.apache.openjpa.persistence.orderby;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests @OrderBy with dot notation on ElementCollection of embeddables
 * containing nested embeddables. Uses runtime enhancement (unenhanced
 * entities with Unenhanced prefix to skip build-time enhancement).
 *
 * Mirrors TCK tests from
 * ee.jakarta.tck.persistence.core.annotations.orderby.Client2:
 * - propertyDotNotationTest
 * - fieldDotNotationTest
 */
public class TestOrderByDotNotation extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "supported",
            UnenhancedObOwnerProperty.class,
            UnenhancedObOwnerField.class,
            UnenhancedObAddress.class,
            UnenhancedObZipCode.class,
            CLEAR_TABLES);
    }

    /**
     * Tests @OrderBy("zipCode.zip DESC") with property access.
     * Addresses have zip codes: 01801, 88444, 01824.
     * Expected order DESC: 88444, 01824, 01801
     * Corresponds to: addr2, addr3, addr1
     */
    public void testPropertyDotNotationOrderBy() {
        UnenhancedObAddress addr1 = new UnenhancedObAddress(
            "1 Network Drive", "Burlington", "MA",
            new UnenhancedObZipCode("01801"));
        UnenhancedObAddress addr2 = new UnenhancedObAddress(
            "634 Goldstar Road", "Peabody", "MA",
            new UnenhancedObZipCode("88444"));
        UnenhancedObAddress addr3 = new UnenhancedObAddress(
            "3212 Boston Road", "Chelmsford", "MA",
            new UnenhancedObZipCode("01824"));

        List<UnenhancedObAddress> addrList = new ArrayList<>();
        addrList.add(addr1);
        addrList.add(addr2);
        addrList.add(addr3);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        UnenhancedObOwnerProperty owner =
            new UnenhancedObOwnerProperty("1", "owner1", addrList);
        em.persist(owner);
        em.flush();
        em.getTransaction().commit();
        em.close();

        // Clear cache and reload
        emf.getCache().evictAll();
        em = emf.createEntityManager();
        UnenhancedObOwnerProperty found =
            em.find(UnenhancedObOwnerProperty.class, "1");
        assertNotNull(found);
        List<UnenhancedObAddress> actual = found.getAddressList();
        assertEquals(3, actual.size());

        // Expected order: 88444 DESC, 01824, 01801
        assertEquals("88444", actual.get(0).getZipCode().getZip());
        assertEquals("01824", actual.get(1).getZipCode().getZip());
        assertEquals("01801", actual.get(2).getZipCode().getZip());

        em.close();
    }

    /**
     * Tests @OrderBy("zipcode.zip DESC") with field access.
     * Uses field name "zipcode" (lowercase c) not property name "zipCode".
     * Addresses have zip codes: 01801, 88444, 01824.
     * Expected order DESC: 88444, 01824, 01801
     */
    public void testFieldDotNotationOrderBy() {
        UnenhancedObAddress addr1 = new UnenhancedObAddress(
            "1 Network Drive", "Burlington", "MA",
            new UnenhancedObZipCode("01801"));
        UnenhancedObAddress addr2 = new UnenhancedObAddress(
            "634 Goldstar Road", "Peabody", "MA",
            new UnenhancedObZipCode("88444"));
        UnenhancedObAddress addr3 = new UnenhancedObAddress(
            "3212 Boston Road", "Chelmsford", "MA",
            new UnenhancedObZipCode("01824"));

        List<UnenhancedObAddress> addrList = new ArrayList<>();
        addrList.add(addr1);
        addrList.add(addr2);
        addrList.add(addr3);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        UnenhancedObOwnerField owner =
            new UnenhancedObOwnerField("2", "owner2", addrList);
        em.persist(owner);
        em.flush();
        em.getTransaction().commit();
        em.close();

        // Clear cache and reload
        emf.getCache().evictAll();
        em = emf.createEntityManager();
        UnenhancedObOwnerField found =
            em.find(UnenhancedObOwnerField.class, "2");
        assertNotNull(found);
        List<UnenhancedObAddress> actual = found.getAddressList();
        assertEquals(3, actual.size());

        // Expected order: 88444 DESC, 01824, 01801
        assertEquals("88444", actual.get(0).getZipCode().getZip());
        assertEquals("01824", actual.get(1).getZipCode().getZip());
        assertEquals("01801", actual.get(2).getZipCode().getZip());

        em.close();
    }
}

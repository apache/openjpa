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
package org.apache.openjpa.enhance.ids;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the use of the openjpa.OptimizeIdCopy configuration parameter.  This parameter
 * changes the behavior of the enhancer and thus, must be set before enhancement occurs.  There
 * is special enhancement task in main/ant/enhancer.xml to ensure this value is set
 * during the enhancement process for the entities used by this test.
 */
class TestOptimizeIdCopy extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(Device.class,Hardware.class,Software.class, CLEAR_TABLES);
    }

    /*
     * Verifies that constructor-based Id optimization occurs during Id copy. Asserts
     * only the proper/expected public constructor is called during the id copy operation.
     */
    @Test
    void idOptimization() {
        EntityManager em = emf.createEntityManager();

        // Add a software entity
        Software sw = new Software();
        int id = new Random().nextInt();
        sw.setIdInt(id);
        sw.setIdInteger(10);
        sw.setIdString("StringIdVal");

        em.getTransaction().begin();
        em.persist(sw);
        em.getTransaction().commit();
        em.clear();

        TypedQuery<Software> swq = em.createQuery("select sw from Software sw", Software.class);
        List<Software> swl = swq.getResultList();
        assertTrue(swl.size() > 0, "Software result list > 0");
        // Id copy optimization should have used the 3rd constructor
        assertFalse(SoftwareId.usedConstructor[0], "First constructor was not used");
        assertFalse(SoftwareId.usedConstructor[1], "Second constructor was not used");
        assertTrue(SoftwareId.usedConstructor[2], "Third (correct) constructor was used");
        em.close();
    }

    /*
     * Verifies that constructor based optimization functions even if parms
     * are different than field order
     */
    @Test
    void idOptimizationConstructorOutOfOrder() {
        EntityManager em = emf.createEntityManager();

        Hardware hw = new Hardware();
        String id = "Model" + (new Random().nextInt());
        hw.setModel("Model" + id);
        hw.setSerial("123XYZ");

        em.getTransaction().begin();
        em.persist(hw);
        em.getTransaction().commit();
        em.clear();

        TypedQuery<Hardware> hwq = em.createQuery("select hw from Hardware hw", Hardware.class);
        List<Hardware> hwl = hwq.getResultList();
        assertTrue(hwl.size() > 0, "Hardware result list > 0");
        // Id copy optimization should have used the first constructor
        assertTrue(HardwareId.usedConstructor[0], "First (correct) constructor was used");
        assertFalse(HardwareId.usedConstructor[1], "Second constructor was not used");
        em.close();
    }

    /*
     * Verifies that classes without a proper constructor do not get optimized
     */
    @Test
    void noOptimization() {
        EntityManager em = emf.createEntityManager();

        int id = new Random().nextInt();
        Device d = new Device();
        d.setId(id);
        d.setType(10);

        em.getTransaction().begin();
        em.persist(d);
        em.getTransaction().commit();
        em.clear();

        TypedQuery<Device> dq = em.createQuery("select d from Device d", Device.class);
        List<Device> dl = dq.getResultList();
        assertTrue(dl.size() > 0, "Device result list > 0");
        // Only the first, default constructor should have been called
        assertTrue(DeviceId.usedConstructor[0], "First (default) constructor was used");
        assertFalse(DeviceId.usedConstructor[1], "Second constructor was not used");
        assertFalse(DeviceId.usedConstructor[2], "Third constructor was not used");
        em.close();
    }
}

/*
 * TestMultiThreaded.java
 *
 * Created on October 12, 2006, 2:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
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
package org.apache.openjpa.persistence.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.kernel.common.apps.RuntimeTest1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestMultiThreaded extends BaseKernelTest {

    static int serial = 5;
    static int threads = 5;
    static int iterations = 5;

    private OpenJPAEntityManager pm;
    private Object id;
    private String name;

    @BeforeEach
    void setUp() {
        deleteAll(RuntimeTest1.class);

        OpenJPAEntityManager pm2 = getPM();
        startTx(pm2);
        name = "testMultiThreaded" + Math.random();
        RuntimeTest1 a = new RuntimeTest1(name,
            (int) (Math.random() * Integer.MAX_VALUE));
        pm2.persist(a);
        id = pm2.getObjectId(a);
        endTx(pm2);

        Map props = new HashMap();
        props.put("openjpa.Multithreaded", "true");
        OpenJPAEntityManagerFactory pmf =
            (OpenJPAEntityManagerFactory) getEmf(props);
        pm = pmf.createEntityManager();
        startTx(pm);
    }

    @AfterEach
    public void tearDown()
        throws Exception {
        try {
            rollbackTx(pm);
            endEm(pm);
        } catch (Exception e) {
            // this is not what we are testing
        }
        super.tearDown();
    }

    @Test
    void testgetTransaction() {
        mttest(serial, threads, iterations);
        pm.getTransaction();
    }

    @Test
    void getObjectById() {
        mttest(serial, threads, iterations);
        assertNotNull(pm.find(RuntimeTest1.class, id));
    }

    @Test
    void queryExecution() {
        mttest(serial, threads, iterations);
        OpenJPAQuery q = pm.createQuery("select o from RuntimeTest1 o "
            + "where o.stringField = '" + name + "'");
        assertEquals(1, q.getResultList().size());
    }

    @Test
    void deletePersistent() {
        mttest(serial, threads, iterations);
        pm.removeAll(pm.createQuery("select o from RuntimeTest1 o "
            + "where o.stringField = '" + name + "'").getResultList());
    }

    @Test
    void refreshAll() {
        mttest(serial, threads, iterations);
        pm.refreshAll();
    }

    @Test
    void evictAll() {
        mttest(serial, threads, iterations);
        pm.evictAll();
    }

    @Test
    void iterateExtent()
        throws Throwable {
        if (timeout(120 * 1000)) return;

        mttest(serial, threads, iterations);

        assertTrue(pm.getTransaction().isActive(),
            "Transaction should have been active");

        for (int i = 0; i < 3; i++) {
            pm.persist(new RuntimeTest1("testIterateExtent" + Math.random(),
                (int) (Math.random() * Integer.MAX_VALUE)));
        }

        assertTrue(pm.getTransaction().isActive(),
            "Transaction should have been active");

        for (Iterator i = pm.createExtent(RuntimeTest1.class, true).iterator();
            i.hasNext(); i.next())
            ;

        assertTrue(pm.getTransaction().isActive(),
            "Transaction should have been active");
    }
}

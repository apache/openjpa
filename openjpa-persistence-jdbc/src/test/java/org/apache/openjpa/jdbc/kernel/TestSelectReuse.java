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
package org.apache.openjpa.jdbc.kernel;

import java.util.Map;
import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.query.SimpleEntity;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestSelectReuse
    extends SingleEMTestCase {

    private long id;

    public void setUp() {
        setUp(SimpleEntity.class, CLEAR_TABLES,
            "openjpa.ConnectionRetainMode", "always");

        SimpleEntity se = new SimpleEntity();
        se.setName("foo");
        se.setValue("bar");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(se);
        em.getTransaction().commit();
        id = se.getId();
        em.close();
    }

    public void testFetchConfigurationEqualsHashCode() {
        Broker b = JPAFacadeHelper.toBroker(em);
        FetchConfiguration fc = b.getFetchConfiguration();
        FetchConfiguration clone = (FetchConfiguration) fc.clone();
        clone.setContext(null);
        assertEquals(fc.hashCode(), clone.hashCode());
        assertEquals(fc, clone);
    }

    public void testSelectReuseWithinSingleEM() {
        Broker b = JPAFacadeHelper.toBroker(em);
//        Map selects = ((JDBCConfiguration) b.getConfiguration())
//            .getSelectCacheInstance();
//        selects.clear();
        SimpleEntity se = em.find(SimpleEntity.class, id);
//        assertEquals(1, selects.size());
        em.clear();
        se = em.find(SimpleEntity.class, id);
//        assertEquals(1, selects.size());
        em.clear();
        se = em.find(SimpleEntity.class, id);
//        assertEquals(1, selects.size());
    }

    public void testSelectReuseAcrossMultipleEMs() {
        Broker b = JPAFacadeHelper.toBroker(em);
//        Map selects = ((JDBCConfiguration) b.getConfiguration())
//            .getSelectCacheInstance();
//        selects.clear();
        SimpleEntity se = em.find(SimpleEntity.class, id);
//        assertEquals(1, selects.size());
        em.close();
        em = emf.createEntityManager();
        se = em.find(SimpleEntity.class, id);
//        assertEquals(1, selects.size());
        em.close();
        em = emf.createEntityManager();
        se = em.find(SimpleEntity.class, id);
//        assertEquals(1, selects.size());
    }

    public void testPerformanceBenefit() {
        int count = 100000;
        perfTest(count);
        queryTest(count);
        long start = System.currentTimeMillis();
        perfTest(count);
        System.out.println("time for " + count + " runs: " +
            (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        queryTest(count);
        System.out.println("time for " + count + " runs: " +
            (System.currentTimeMillis() - start));
    }

    public static void main(String... args) throws Exception {
        TestSelectReuse tsr = new TestSelectReuse();
        tsr.setUp();
        tsr.testPerformanceBenefit();
        tsr.tearDown();
    }

    private void perfTest(int count) {
        for (int i = 0; i < count; i++) {
            SimpleEntity se = em.find(SimpleEntity.class, id);
            assertNotNull(se);
            em.clear();
//            em.close();
//            em = emf.createEntityManager();
        }
    }

    private void queryTest(int count) {
        for (int i = 0; i < count; i++) {
            SimpleEntity se = (SimpleEntity) em
                .createQuery("select o from simple o where o.id = :id")
                .setParameter("id", id)
                .getSingleResult();
            assertNotNull(se);
            em.clear();
        }
    }
}

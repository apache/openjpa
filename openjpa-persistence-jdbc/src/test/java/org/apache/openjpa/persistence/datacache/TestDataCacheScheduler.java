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
package org.apache.openjpa.persistence.datacache;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import org.apache.openjpa.persistence.datacache.common.apps.ScheduledEviction;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheScheduler;
import org.apache.openjpa.datacache.ConcurrentDataCache;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.util.Id;

public class TestDataCacheScheduler
    extends AbstractTestCase {

    private static final String MINUTES = getMinutes();

    public TestDataCacheScheduler(String str) {
        super(str, "datacachecactusapp");
    }

    private static String getMinutes() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 60; i++) {
            if (i % 2 == 0)
                buf.append(i).append(',');
        }
        return buf.toString();
    }

    public void setUp() {
        deleteAll(ScheduledEviction.class);
    }

    public void testRuntime()
        throws Exception {
        String sched = MINUTES + " * * * *";
        Map propsMap = new HashMap();
        propsMap
            .put("openjpa.DataCache", "true(EvictionSchedule=\"" + sched + "\")");
        propsMap.put("openjpa.RemoteCommitProvider", "sjvm");
        propsMap.put("openjpa.FlushBeforeQueries", "true");
        propsMap.put("openjpa.BrokerImpl", CacheTestBroker.class.getName());
        OpenJPAEntityManagerFactory emf =
            (OpenJPAEntityManagerFactory) getEmf(propsMap);

        ((OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.cast(emf))
            .getConfiguration().getDataCacheManagerInstance()
            .getDataCacheScheduler().setInterval(1);
        DataCache cache = JPAFacadeHelper.getMetaData(emf,
            ScheduledEviction.class).getDataCache();

        OpenJPAEntityManager em = (OpenJPAEntityManager) emf
            .createEntityManager();
        startTx(em);
        ScheduledEviction pc = new ScheduledEviction("Foo");
        em.persist(pc);
        Object oid = em.getObjectId(pc);
        Object oidwithclass = new Id(ScheduledEviction.class, oid.toString());
        endTx(em);
        endEm(em);

        cache.clear();// clear and wait until next run.
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (cal.get(Calendar.MINUTE) % 2 == 0)
            Thread.currentThread().sleep
                ((60 - cal.get(Calendar.SECOND)) * 1000);
        cal.setTime(new Date());
        assertTrue(cal.get(Calendar.MINUTE) % 2 == 1);
        em = (OpenJPAEntityManager) emf.createEntityManager();
        em.find(ScheduledEviction.class, oid);
        endEm(em);
        assertTrue(cache.contains(oidwithclass));

        Thread.currentThread().sleep(130 * 1000);
        assertFalse(cache.contains(oidwithclass));
        emf.close();
    }

    /**
     * too slow ! *
     */
    //FIXME Seetha Sep 26,2006
    /*public void XXXtestRunnable()
        throws Exception {
        KodoPersistenceManager pm = getPM();
        OpenJPAConfiguration conf = pm.getConfiguration();
        DataCacheScheduler scheduler = new DataCacheScheduler(conf);
        scheduler.setInterval(1);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int minute = (cal.get(Calendar.MINUTE) + 2) % 60;
        StringBuffer sched = new StringBuffer();
        sched.append(minute).append(' ');
        sched.append("* ");
        sched.append("* ");
        sched.append("* ");
        sched.append("* ");
        DummyCache cache = new DummyCache();
        scheduler.scheduleEviction(cache, sched.toString());

        Thread thread = new Thread(scheduler);
        thread.setDaemon(true);
        thread.start();
        // test that it did not run yet...
        Thread.currentThread().sleep(90 * 1000); // 90 seconds
        assertEquals(0, cache.clearCount);
        // test that it ran...
        Thread.currentThread().sleep(45 * 1000); // 45 seconds
        assertEquals(1, cache.clearCount);
        // test that it wasn't too eager
        Thread.currentThread().sleep(50 * 1000); // 90 seconds
        assertEquals(1, cache.clearCount);
        scheduler.stop();
    }*/

    /**
     * too slow *
     */
    /* public void XXXtestMonth()
        throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int month = cal.get(Calendar.MONTH);
        int month2 = month + 1;
        if (month2 > 12)
            month2 = 1;
        doTest("* * " + month + " *", "* * " + month2 + " *");
    }*/

    /**
     * too slow *
     */
    /* public void XXXtestDayOfMonth()
        throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int dom = cal.get(Calendar.DAY_OF_MONTH);
        doTest("* " + dom + " * *", "* " + (dom % 12 + 1) + " * *");
    }*/
    public void testDayOfWeek()
        throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int day = cal.get(Calendar.DAY_OF_WEEK);
        doTest("* * * " + day, "* * * " + (day % 7 + 1));
    }

    public void testHour()
        throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        doTest(hour + " * * *", ((hour + 1) % 24) + " * * *");
    }

    /**
     * Pass in 4 out of 5 tokens.
     */
    private void doTest(String valid, String invalid)
        throws Exception {

        OpenJPAEntityManagerFactory emf =
            (OpenJPAEntityManagerFactory) getEmf();
        OpenJPAConfiguration conf =
            ((OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.cast(emf))
                .getConfiguration();

        DataCacheScheduler scheduler = new DataCacheScheduler(conf);
        scheduler.setInterval(1);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        String sched = ((cal.get(Calendar.MINUTE) + 1) % 60) + " ";
        DummyCache validCache = new DummyCache();
        scheduler.scheduleEviction(validCache, sched + valid);
        DummyCache invalidCache = new DummyCache();
        scheduler.scheduleEviction(invalidCache, sched + invalid);
        Thread thread = new Thread(scheduler);
        thread.setDaemon(true);
        thread.start();
        // test that it did not run yet...
        Thread.currentThread().sleep(70 * 1000); // 70 seconds
        scheduler.stop();
//        assertEquals(2, validCache.clearCount);
        assertTrue("Wrong invocation count: " + validCache.clearCount,
            validCache.clearCount == 1 || validCache.clearCount == 2);
        assertEquals(0, invalidCache.clearCount);
    }

    private class DummyCache extends ConcurrentDataCache {

        int clearCount = 0;

        public void clear() {
            clearCount++;
        }
    }
}

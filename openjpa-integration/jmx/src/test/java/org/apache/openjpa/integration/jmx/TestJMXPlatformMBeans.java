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
package org.apache.openjpa.integration.jmx;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.Query;

import org.apache.openjpa.instrumentation.DataCacheInstrument;
import org.apache.openjpa.instrumentation.InstrumentationManager;
import org.apache.openjpa.instrumentation.PreparedQueryCacheInstrument;
import org.apache.openjpa.instrumentation.QueryCacheInstrument;
import org.apache.openjpa.instrumentation.jmx.JMXProvider;
import org.apache.openjpa.lib.instrumentation.Instrument;
import org.apache.openjpa.lib.instrumentation.InstrumentationProvider;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

public class TestJMXPlatformMBeans extends AbstractPersistenceTestCase {

    /**
     * Verifies data cache metrics are available through simple instrumentation.
     */
    public void testDataCacheInstrument() {
        OpenJPAEntityManagerFactorySPI oemf = createNamedEMF("openjpa-integration-jmx");
 
        // Verify an EMF was created with the supplied instrumentation
        assertNotNull(oemf);

        // Verify an instrumentation manager is available
        InstrumentationManager mgr = oemf.getConfiguration().getInstrumentationManagerInstance();
        assertNotNull(mgr);
        
        // Get the in-band data cache instrument
        Set<InstrumentationProvider> providers = mgr.getProviders();
        assertNotNull(providers);
        assertEquals(1, providers.size());
        InstrumentationProvider provider = providers.iterator().next();
        assertEquals(provider.getClass(), JMXProvider.class);
        Instrument inst = provider.getInstrumentByName("DataCache");
        assertNotNull(inst);
        assertTrue(inst instanceof DataCacheInstrument);
        DataCacheInstrument dci = (DataCacheInstrument)inst;
        assertEquals(dci.getCacheName(), "default");
        
        OpenJPAEntityManagerSPI oem = oemf.createEntityManager();
        
        CachedEntity ce = new CachedEntity();
        int id = new Random().nextInt();
        ce.setId(id);
        
        oem.getTransaction().begin();
        oem.persist(ce);
        oem.getTransaction().commit();
        oem.clear();
        assertTrue(oemf.getCache().contains(CachedEntity.class, id));
        ce = oem.find(CachedEntity.class, id);
        
        assertTrue(dci.getHitCount() > 0);
        assertTrue(dci.getHitCount(CachedEntity.class.getName()) > 0);
        assertTrue(dci.getWriteCount() > 0);
        assertTrue(dci.getCacheStatistics().classNames().contains(CachedEntity.class.getName()));
        // Thread out to do out-of-band MBean-based validation.  This could
        // have been done on the same thread, but threading out makes for a
        // more realistic test.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<Boolean> result = executor.submit(new DCMBeanCallable());
        try {
            assertTrue(result.get());
        } catch (Throwable t) {
            fail("DataCache verification failed: " + t);
        }
        
        closeEMF(oemf);
    }
    
    /**
     * Verifies query cache metrics are available through simple instrumentation.
     */
    public void testQueryCacheInstrument() {
        OpenJPAEntityManagerFactorySPI oemf = createNamedEMF("openjpa-integration-jmx");
 
        // Verify an EMF was created with the supplied instrumentation
        assertNotNull(oemf);

        // Verify an instrumentation manager is available
        InstrumentationManager mgr = oemf.getConfiguration().getInstrumentationManagerInstance();
        assertNotNull(mgr);
        
        // Get the in-band data cache instrument
        Set<InstrumentationProvider> providers = mgr.getProviders();
        assertNotNull(providers);
        assertEquals(1, providers.size());
        InstrumentationProvider provider = providers.iterator().next();
        assertEquals(provider.getClass(), JMXProvider.class);

        Instrument inst = provider.getInstrumentByName("QueryCache");
        assertNotNull(inst);
        assertTrue(inst instanceof QueryCacheInstrument);
        QueryCacheInstrument qci = (QueryCacheInstrument)inst;
        
        assertEquals(0,qci.getExecutionCount());
        assertEquals(0,qci.getTotalExecutionCount());
        assertEquals(0,qci.getHitCount());
        assertEquals(0,qci.getTotalHitCount());
        
        OpenJPAEntityManagerSPI oem = oemf.createEntityManager();
        
        CachedEntity ce = new CachedEntity();
        int id = new Random().nextInt();
        ce.setId(id);
        CachedEntity ce2 = new CachedEntity();
        id = new Random().nextInt();
        ce2.setId(id);

        oem.getTransaction().begin();
        oem.persist(ce);
        oem.persist(ce2);
        oem.getTransaction().commit();
       
        Query q = oem.createQuery("SELECT ce FROM CachedEntity ce");
        
        List<?> result = q.getResultList();
        assertNotNull(result);
        assertTrue(result.size() > 1);
        oem.clear();

        result = q.getResultList();
        
        assertTrue(qci.getExecutionCount() > 0);
        assertTrue(qci.getTotalExecutionCount() > 0);
        assertTrue(qci.getHitCount() > 0);
        assertTrue(qci.getTotalHitCount() > 0);

        // Thread out to do out-of-band MBean-based validation.  This could
        // have been done on the same thread, but threading out makes for a
        // more realistic test.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<Boolean> execResult = executor.submit(new QueryCachesMBeanCallable(
            QueryCachesMBeanCallable.QC_OBJNAME,
            QueryCachesMBeanCallable.QC_QM));
        try {
            assertTrue(execResult.get());
        } catch (Throwable t) {
            fail("QueryCache verification failed: " + t);
        }
        closeEMF(oemf);
    }

    /**
     * Verifies prepared query cache metrics are available through simple instrumentation.
     */
    public void testPreparedQueryCacheInstrument() {
        OpenJPAEntityManagerFactorySPI oemf = createNamedEMF("openjpa-integration-jmx-qsc");
 
        // Verify an EMF was created with the supplied instrumentation
        assertNotNull(oemf);

        // Verify an instrumentation manager is available
        InstrumentationManager mgr = oemf.getConfiguration().getInstrumentationManagerInstance();
        assertNotNull(mgr);
        
        // Get the in-band data cache instrument
        Set<InstrumentationProvider> providers = mgr.getProviders();
        assertNotNull(providers);
        assertEquals(1, providers.size());
        InstrumentationProvider provider = providers.iterator().next();
        assertEquals(provider.getClass(), JMXProvider.class);

        Instrument inst = provider.getInstrumentByName("QuerySQLCache");
        assertNotNull(inst);
        assertTrue(inst instanceof PreparedQueryCacheInstrument);
        PreparedQueryCacheInstrument qci = (PreparedQueryCacheInstrument)inst;
        
        assertEquals(0,qci.getExecutionCount());
        assertEquals(0,qci.getTotalExecutionCount());
        assertEquals(0,qci.getHitCount());
        assertEquals(0,qci.getTotalHitCount());
        
        OpenJPAEntityManagerSPI oem = oemf.createEntityManager();
        
        CachedEntity ce = new CachedEntity();
        int id = new Random().nextInt();
        ce.setId(id);
        CachedEntity ce2 = new CachedEntity();
        id = new Random().nextInt();
        ce2.setId(id);

        oem.getTransaction().begin();
        oem.persist(ce);
        oem.persist(ce2);
        oem.getTransaction().commit();
       
        Query q = oem.createQuery("SELECT ce FROM CachedEntity ce");
        
        List<?> result = q.getResultList();
        assertNotNull(result);
        assertTrue(result.size() > 1);
        oem.clear();

        result = q.getResultList();
        
        assertTrue(qci.getExecutionCount() > 0);
        assertTrue(qci.getTotalExecutionCount() > 0);
        assertTrue(qci.getHitCount() > 0);
        assertTrue(qci.getTotalHitCount() > 0);

        // Thread out to do out-of-band MBean-based validation.  This could
        // have been done on the same thread, but threading out makes for a
        // more realistic test.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<Boolean> execResult = executor.submit(new QueryCachesMBeanCallable(
            QueryCachesMBeanCallable.QSC_OBJNAME,
            QueryCachesMBeanCallable.QSC_QM));
        try {
            assertTrue(execResult.get());
        } catch (Throwable t) {
            fail("QueryCache verification failed: " + t);
        }

        closeEMF(oemf);
    }

    public class DCMBeanCallable implements Callable<Boolean> {

        public Boolean call() {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            assertNotNull(mbs);
            ObjectName objname = null;
            try {
                // Query for the DataCache bean
                objname = new ObjectName("org.apache.openjpa:type=DataCache,cfgid=openjpa-integration-jmx,*");
                Set<ObjectName> ons = mbs.queryNames(objname, null);
                assertEquals(1, ons.size());
                ObjectName on = ons.iterator().next();
                // Assert data cache attributes can be accessed and are being updated through the MBean
                long hitCount = (Long)mbs.getAttribute(on, "HitCount");
                long readCount = (Long)mbs.getAttribute(on, "ReadCount");
                long writeCount = (Long)mbs.getAttribute(on, "WriteCount");
                assertTrue(hitCount > 0);
                assertTrue(readCount > 0);
                assertTrue(writeCount > 0);
                // Assert data cache MBean methods can be invoked
                Object[] parms = new Object[] { CachedEntity.class.getName() };
                String[] sigs = new String[] { "java.lang.String" };
                long clsHitCount = (Long)mbs.invoke(on, "getHitCount", parms, sigs);
                long clsReadCount = (Long)mbs.invoke(on, "getReadCount", parms, sigs); 
                long clsWriteCount = (Long)mbs.invoke(on, "getWriteCount", parms, sigs);
                assertTrue(clsHitCount > 0);
                assertTrue(clsReadCount > 0);
                assertTrue(clsWriteCount > 0);
                
                // Comment out classNames portion of the test which is currently broken. 
                // Set<String> classNames = (Set<String>)mbs.invoke(on, "classNames", null, null);
                // assertNotNull(classNames);
                // assertTrue(classNames.contains(CachedEntity.class.getName()));
                
                // Invoke the reset method and recollect stats
                mbs.invoke(on, "reset", null, null);
                hitCount = (Long)mbs.getAttribute(on, "HitCount");
                readCount = (Long)mbs.getAttribute(on, "ReadCount");
                writeCount = (Long)mbs.getAttribute(on, "WriteCount");
                assertEquals(0, hitCount);
                assertEquals(0, readCount);
                assertEquals(0, writeCount);

                clsHitCount = (Long)mbs.invoke(on, "getHitCount", parms, sigs);
                clsReadCount = (Long)mbs.invoke(on, "getReadCount", parms, sigs); 
                clsWriteCount = (Long)mbs.invoke(on, "getWriteCount", parms, sigs);
                assertEquals(0, clsHitCount);
                assertEquals(0, clsReadCount);
                assertEquals(0, clsWriteCount);
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
                return false;
            }
            return true;
        }
    }

    public class QueryCachesMBeanCallable implements Callable<Boolean> {

        public static final String QC_OBJNAME = "org.apache.openjpa:type=QueryCache,cfgid=openjpa-integration-jmx,*";
        public static final String QSC_OBJNAME = "org.apache.openjpa:type=QuerySQLCache,cfgid=openjpa-integration-jmx-qsc,*";  
        public static final String QC_QM = "queryKeys";
        public static final String QSC_QM = "queries";
        
        private String _objNameStr;
        private String _queryMethod;
        
        public QueryCachesMBeanCallable(String objName, String queryMethod) {
            setObjName(objName);
            setQueryMethod(queryMethod);
        }
        
        @SuppressWarnings("unchecked")
        public Boolean call() {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            assertNotNull(mbs);
            ObjectName objname = null;
            try {
                // Query for the QueryCache bean
                objname = new ObjectName(getObjName());
                Set<ObjectName> ons = mbs.queryNames(objname, null);
                assertEquals(1, ons.size());
                ObjectName on = ons.iterator().next();
                // Assert query cache attributes can be accessed and are being updated through the MBean
                long hitCount = (Long)mbs.getAttribute(on, "HitCount");
                long execCount = (Long)mbs.getAttribute(on, "ExecutionCount");
                assertTrue(hitCount > 0);
                assertTrue(execCount > 0);
                // Assert data cache MBean methods can be invoked
                
                Set<String> keys = (Set<String>)mbs.invoke(on, getQueryMethod(), null, null);
                assertNotNull(keys);
                assertTrue(keys.size() > 0);
                String[] sigs = new String[] { "java.lang.String" };
                for (String key : keys) {
                    Object[] parms = new Object[] { key };
                    long queryHitCount = (Long)mbs.invoke(on, "getHitCount", parms, sigs);
                    long queryReadCount = (Long)mbs.invoke(on, "getExecutionCount", parms, sigs); 
                    assertTrue(queryHitCount > 0);
                    assertTrue(queryReadCount > 0);
                }
                // Invoke the reset method and recollect stats
                mbs.invoke(on, "reset", null, null);
                hitCount = (Long)mbs.getAttribute(on, "HitCount");
                execCount = (Long)mbs.getAttribute(on, "ExecutionCount");
                assertEquals(0, hitCount);
                assertEquals(0, execCount);

                keys = (Set<String>)mbs.invoke(on, getQueryMethod(), null, null);
                assertNotNull(keys);
                assertEquals(0, keys.size());
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
                return false;
            }
            return true;
        }

        public void setObjName(String objNameStr) {
            this._objNameStr = objNameStr;
        }

        public String getObjName() {
            return _objNameStr;
        }

        public void setQueryMethod(String _queryMethod) {
            this._queryMethod = _queryMethod;
        }

        public String getQueryMethod() {
            return _queryMethod;
        }
    }

}

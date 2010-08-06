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
import java.util.Random;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.openjpa.instrumentation.DataCacheInstrument;
import org.apache.openjpa.instrumentation.InstrumentationManager;
import org.apache.openjpa.instrumentation.jmx.JMXProvider;
import org.apache.openjpa.lib.instrumentation.Instrument;
import org.apache.openjpa.lib.instrumentation.InstrumentationProvider;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

public class TestJMXPlatformMBeans extends AbstractPersistenceTestCase {
    /**
     * Verifies the data cache metrics are available through simple instrumentation.
     */

    @SuppressWarnings("deprecation")
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
        
        // Thread out to do out-of-band MBean-based validation.  This could
        // have been done on the same thread, but threading out makes for a
        // more realistic test.
        Thread thr = new Thread(new DCMBeanThread());
        thr.start();
        try {
            thr.join(60000);  // Wait for 1 minute for the MBean thread to return.
            if (thr.isAlive()) {
                // MBean did not return within a minute, interrupt it.
                thr.interrupt();
                Thread.sleep(5000);
                if (thr.isAlive()) {
                    // Attempt to hard kill the thread to prevent the test from hanging
                    thr.stop();
                }
                fail("DataCache MBean verification thread failed.");
            }
        } catch (Throwable t) {
            fail("Caught unexpected throwable: " + t);
        }
        
        closeEMF(oemf);
    }
    
    public class DCMBeanThread implements Runnable {

        public void run() {
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
            }
        }
    }
}
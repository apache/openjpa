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
package org.apache.openjpa.instrumentation;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.openjpa.lib.instrumentation.Instrument;
import org.apache.openjpa.lib.instrumentation.InstrumentationProvider;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Verifies the configuration and usage of a simple instrumentation
 * provider.
 * @author jrbauer
 *
 */
public class TestInstrumentationProvider extends SingleEMFTestCase {

    public static final String INSTRUMENTATION = 
        "org.apache.openjpa.instrumentation.SimpleProvider(Instrument='DataCache,QueryCache,QuerySQLCache')";
    
    public void setUp() throws Exception {
        super.setUp("openjpa.Instrumentation", 
                    INSTRUMENTATION,
                    "openjpa.DataCache",
                    "true(EnableStatistics=true)",
                    "openjpa.QueryCache",
                    "true",
                    "openjpa.RemoteCommitProvider",
                    "sjvm");
    }

    /**
     * Verifies simple instrumentation provider config with instruments defined
     * for data cache and query cache.
     */
    public void testProviderConfig() {
        // Verify an EMF was created with the supplied instrumentation
        assertNotNull(emf);

        // Verify the instrumentation value was stored in the config
        String instrValue = emf.getConfiguration().getInstrumentation();
        assertEquals(instrValue, INSTRUMENTATION);

        // Verify an instrumentation manager is available
        InstrumentationManager mgr = emf.getConfiguration().getInstrumentationManagerInstance();
        assertNotNull(mgr);
        
        // Verify the manager is managing the correct provider
        Set<InstrumentationProvider> providers = mgr.getProviders();
        assertNotNull(providers);
        assertEquals(1, providers.size());
        InstrumentationProvider provider = providers.iterator().next();
        assertEquals(provider.getClass(), SimpleProvider.class);
        
        // Verify the provider has instruments registered for the caches
        Set<Instrument> instruments = provider.getInstruments();
        assertNotNull(instruments);
        assertEquals(3,instruments.size());
        
        // Lightweight verification of the instruments
        Instrument inst = provider.getInstrumentByName(DCInstrument.NAME);
        assertNotNull(inst);
        assertTrue(inst instanceof DataCacheInstrument);
        DataCacheInstrument dci = (DataCacheInstrument)inst;
        assertEquals(dci.getCacheName(), "default");
        inst = provider.getInstrumentByName(QCInstrument.NAME);
        assertNotNull(inst);
        assertTrue(inst instanceof QueryCacheInstrument);
        inst = provider.getInstrumentByName(QSCInstrument.NAME);
        assertNotNull(inst);
        assertTrue(inst instanceof PreparedQueryCacheInstrument);
    }
    
    /**
     * Verifies configuring and adding an instrument to a provider after the provider
     * has been initialized within the persistence unit. 
     */
    public void testDynamicInstrumentConfig() {
        InstrumentationManager mgr = emf.getConfiguration().getInstrumentationManagerInstance();
        assertNotNull(mgr);

        Set<InstrumentationProvider> providers = mgr.getProviders();
        assertNotNull(providers);
        assertEquals(1, providers.size());
        InstrumentationProvider provider = providers.iterator().next();
        assertEquals(provider.getClass(), SimpleProvider.class);

        verifyBrokerLevelInstrument(provider);
    }

    /**
     * Verifies configuring and adding an instrumentation provider dynamically after
     * the persistence unit has been created.
     */
    public void testDynamicProviderConfig() {
        InstrumentationManager mgr = emf.getConfiguration().getInstrumentationManagerInstance();
        assertNotNull(mgr);

        DynamicProvider dyp = new DynamicProvider();
        mgr.manageProvider(dyp);
        verifyBrokerLevelInstrument(dyp);
        assertTrue(dyp.isStarted());
        assertNotNull(dyp.getInstrumentByName(BrokerLevelInstrument.NAME));
        assertEquals(2, mgr.getProviders().size());
    }

    public void verifyBrokerLevelInstrument(InstrumentationProvider provider) {
        // Create a new broker level instrument and register it with the
        // provider
        BrokerLevelInstrument bli = new BrokerLevelInstrument();
        provider.addInstrument(bli);
        // Verify instrument has not been initialized or started 
        assertFalse(bli.isInitialized());
        assertFalse(bli.isStarted());
        
        // Create a new EM/Broker
        EntityManager em = emf.createEntityManager();
        // Vfy the instrument has been initialized and started
        assertTrue(bli.isInitialized());
        assertTrue(bli.isStarted());
        // Close the EM/Broker
        em.close();
        // Vfy the instrument has stopped
        assertFalse(bli.isStarted());
    }
    
    /**
     * Verifies the cache metrics are available through simple instrumentation.
     */
//    public void testCacheInstruments() {
//        
//    }

    /**
     * Verifies multiple instrumentation providers can be specified.
     */
//    public void testMultipleProviderConfig() {
//        
//    }
}

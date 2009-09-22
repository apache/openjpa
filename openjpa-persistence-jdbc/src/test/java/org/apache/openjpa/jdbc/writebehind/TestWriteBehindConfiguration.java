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
package org.apache.openjpa.jdbc.writebehind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Persistence;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.datacache.DataCacheStoreManager;
import org.apache.openjpa.kernel.DelegatingStoreManager;
import org.apache.openjpa.persistence.EntityManagerImpl;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.writebehind.SimpleWriteBehindCache;
import org.apache.openjpa.writebehind.SimpleWriteBehindCallback;
import org.apache.openjpa.writebehind.WriteBehindCache;
import org.apache.openjpa.writebehind.WriteBehindCacheManager;
import org.apache.openjpa.writebehind.WriteBehindCallback;
import org.apache.openjpa.writebehind.WriteBehindStoreManager;

public class TestWriteBehindConfiguration extends AbstractWriteBehindTestCase {
    public void testWriteBehindStoreManagerCreated() {
        assertNotNull(emf);
        assertNotNull(em);
        DelegatingStoreManager storeManager = ((EntityManagerImpl) em).getBroker().getStoreManager();
        
        storeManager = (DelegatingStoreManager) storeManager.getDelegate();
        assertTrue(String.format("Unexpected StoreManager type : %s",
            storeManager.getClass().getName()),
            storeManager instanceof DataCacheStoreManager);
        assertTrue(String.format("Unexpected StoreManager type : %s",
            storeManager.getDelegate().getClass().getName()), 
            storeManager.getDelegate() instanceof WriteBehindStoreManager);
    }

    public void testWriteBehindCacheCreated() {
        assertNotNull("EMF was not created.", emf);
        WriteBehindCacheManager manager = getWBCacheManager();
        assertNotNull("WriteBehindCacheManager should exist", manager);

        WriteBehindCache wbcache = manager.getSystemWriteBehindCache();
        assertNotNull("SystemWriteBehindCache should exist", wbcache);

        assertTrue(String.format("Expecting %s to be an instance of %s",
            wbcache, SimpleWriteBehindCache.class),
            wbcache instanceof SimpleWriteBehindCache);
    }

    public void testWriteBehindCallbackCreated() {
        assertNotNull("EMF was not created.", emf);
        WriteBehindCallback callback = getWBCallback();
        assertNotNull("WB Callback should exist", callback);

        assertTrue(String.format("Expecting %s to be an instance of %s",
            callback, SimpleWriteBehindCallback.class),
            callback instanceof SimpleWriteBehindCallback);

    }

    public void testCustomCacheInstanceCreated() {
        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put("openjpa.WriteBehindCache", CustomWriteBehindCache.class.getCanonicalName());
        props.put("openjpa.WriteBehindCallback", "true");

        WriteBehindCacheManager manager = getWBCacheManager(getCustomFactory(props));
        assertNotNull("WriteBehindCacheManager should exist", manager);

        WriteBehindCache wbcache = manager.getSystemWriteBehindCache();
        assertNotNull("SystemWriteBehindCache should exist", wbcache);

        assertTrue(String.format("Expecting %s to be an instance of %s",
            wbcache, CustomWriteBehindCache.class),
            wbcache instanceof CustomWriteBehindCache);
    }
    
    public void testCustomCallbackCreated() {
        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put("openjpa.WriteBehindCache", CustomWriteBehindCache.class.getCanonicalName());
        props.put("openjpa.WriteBehindCallback", CustomWriteBehindCallback.class.getCanonicalName());

        WriteBehindCallback callback = getWBCallback(getCustomFactory(props));
        assertNotNull("WB Callback should exist", callback);

        assertTrue(String.format("Expecting %s to be an instance of %s",
            callback, CustomWriteBehindCallback.class),
            callback instanceof CustomWriteBehindCallback);
    }
    
    
    public void testWriteBehindCallbackCreatedForEachConfiguration() {
        OpenJPAEntityManagerFactorySPI emf1 = emf;
        ArrayList<Object> clonedProps = new ArrayList<Object>();
        for(Object o : writeBehindProps) {
            // quick and dirty way to alter the properties, this should prevent the cached EMF from being reused.
            if(o.toString().equals("unsupported")) {
                // just change the runtimeUnenhancedClasses value.
                clonedProps.add("supported");
            }
            else {
                clonedProps.add(o);
            }
        }
        OpenJPAEntityManagerFactorySPI emf2 = createEMF(clonedProps.toArray()); 
        assertNotNull("Unable to create first EntityManager ", emf1);
        assertNotNull("Unable to create second EntityManager ", emf2);
        assertNotSame("Test is invalid if the same EMF is used", emf1, emf2);

        OpenJPAConfigurationImpl conf1 = (OpenJPAConfigurationImpl) emf1.getConfiguration();
        OpenJPAConfigurationImpl conf2 = (OpenJPAConfigurationImpl) emf2.getConfiguration();

        assertNotNull(conf1);
        assertNotNull(conf2);
        assertNotSame("Configuration objects for different EMFs should not be the same", conf1, conf2);

        assertNotSame("Each Configuration should have its own WriteBehindCallbackPlugins",
            conf1.writeBehindCallbackPlugin, conf2.writeBehindCallbackPlugin);
        
        WriteBehindCallback wbc1 = conf1.getWriteBehindCallbackInstance();
        WriteBehindCallback wbc2 = conf2.getWriteBehindCallbackInstance();
        assertNotNull(wbc1);
        assertNotNull(wbc2);
        assertNotSame("Each Configuration should have its own WriteBehindCallback", wbc1, wbc2);
    }
    
    public OpenJPAEntityManagerFactorySPI getCustomFactory(
        Map<String, Object> extraProps) {
        Map<String, Object> props = getPropertiesMap(getDefaultWriteBehindProperties());
        props.putAll(extraProps);

        OpenJPAEntityManagerFactorySPI customEMF =
            (OpenJPAEntityManagerFactorySPI) Persistence.createEntityManagerFactory(getPersistenceUnitName(), props);
        assertNotNull("EMF was not created.", customEMF);
        return customEMF;
    }
}

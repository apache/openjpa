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

import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.writebehind.entities.AbstractSimpleEntity;
import org.apache.openjpa.jdbc.writebehind.entities.SimpleNonGeneratedIdEntity;
import org.apache.openjpa.jdbc.writebehind.entities.SimpleTableGeneratedIdEntity;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SingleEMTestCase;
import org.apache.openjpa.writebehind.WriteBehindCache;
import org.apache.openjpa.writebehind.WriteBehindCacheManager;
import org.apache.openjpa.writebehind.WriteBehindCallback;

public abstract class AbstractWriteBehindTestCase extends SingleEMTestCase {
    protected static Object[] writeBehindProps =
        new Object[] { 
            "openjpa.DataCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm", 
            "openjpa.WriteBehindCache", "true",
            "openjpa.WriteBehindCallback", "true", 
            "openjpa.RuntimeUnenhancedClasses", "unsupported",
            SimpleNonGeneratedIdEntity.class, SimpleTableGeneratedIdEntity.class,
            AbstractSimpleEntity.class};
    
    protected static Object [] validatorProps = 
        new Object[] { 
        "openjpa.RuntimeUnenhancedClasses", "unsupported",
        SimpleNonGeneratedIdEntity.class, SimpleTableGeneratedIdEntity.class,
        AbstractSimpleEntity.class  };
    
    protected EntityManagerFactory _validatorEMF = null; 
    
    public void setUp() {
        if(emf == null) { 
            super.setUp(writeBehindProps);
        }
        if(_validatorEMF == null) { 
            _validatorEMF = createEMF(validatorProps);
        }
    }
    
    public static Object[] getDefaultWriteBehindProperties() { 
        return writeBehindProps;
    }

    protected WriteBehindCacheManager getWBCacheManager() {
        return getWBCacheManager(emf);
    }

    protected WriteBehindCacheManager getWBCacheManager(
        OpenJPAEntityManagerFactorySPI factory) {
        WriteBehindCacheManager wbcm =
            factory.getConfiguration().getWriteBehindCacheManagerInstance();
        return wbcm;
    }

    protected WriteBehindCache getWBCache() {
        return getWBCache(emf);
    }

    protected WriteBehindCache 
        getWBCache(OpenJPAEntityManagerFactorySPI factory) {
        return getWBCache(factory, ""); // TODO handle default name better
    }

    protected WriteBehindCallback getWBCallback() {
        return getWBCallback(emf);
    }

    protected WriteBehindCallback getWBCallback(
        OpenJPAEntityManagerFactorySPI factory) {
        return factory.getConfiguration().getWriteBehindCallbackInstance();
    }

    protected WriteBehindCache getWBCache(
        OpenJPAEntityManagerFactorySPI factory, String name) {
        WriteBehindCache wbc = null;
        if (StringUtils.isEmpty(name)) {
            wbc = getWBCacheManager(factory).getSystemWriteBehindCache();
        } else {
            wbc = getWBCacheManager(factory).getWriteBehindCache(name);
        }
        return wbc;
    }
    
    protected EntityManagerFactory getValidatorEMF() { 
        return _validatorEMF;
    }
    
    public void tearDown() throws Exception { 
//        _validatorEMF.close();
//        super.tearDown();
    }

}

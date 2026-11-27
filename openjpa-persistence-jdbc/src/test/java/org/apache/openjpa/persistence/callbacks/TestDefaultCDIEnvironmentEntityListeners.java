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
package org.apache.openjpa.persistence.callbacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Cdi(disableDiscovery = true, classes = CDIBean.class)
public class TestDefaultCDIEnvironmentEntityListeners extends SingleEMFTestCase {
	
	@Inject
	private CDIBean bean;

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES);
        CDIBean.reset();
    }
    
    @AfterEach
    public void after() {
    	CDIBean.reset();
    }

    @Override
    protected String getPersistenceUnitName() {
        return "cdi-listener-pu";
    }

    @Test
    void cacheableAndNonCacheableEntityListeners() {
    	// CDI is avaiable
    	assertNotNull(bean);
        try (EntityManager em = emf.createEntityManager()){
            em.getTransaction().begin();
            CDIListenedEntity entity = new CDIListenedEntity();
            assertNull(entity.getId());
            
            em.persist(entity);
            assertEquals(0, CDIBean.getCount());
            assertNull(entity.getValue());
            em.getTransaction().commit();
        }
    }
    
}

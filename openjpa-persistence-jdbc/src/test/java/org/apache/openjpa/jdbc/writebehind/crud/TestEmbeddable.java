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
package org.apache.openjpa.jdbc.writebehind.crud;

import java.util.Collection;

import org.apache.openjpa.jdbc.writebehind.entities.Embeddable01;
import org.apache.openjpa.jdbc.writebehind.entities.Entity01;
import org.apache.openjpa.jdbc.writebehind.entities.Entity02;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.writebehind.WriteBehindCache;
import org.apache.openjpa.writebehind.WriteBehindCacheManager;
import org.apache.openjpa.writebehind.WriteBehindCallback;

public class TestEmbeddable extends SingleEMFTestCase {

    private OpenJPAEntityManagerSPI         em = null;
    private OpenJPAEntityManagerFactorySPI  emf_noCache = null;
    private OpenJPAEntityManagerSPI         em_noCache = null;
    private WriteBehindCache                wbCache = null;
    private WriteBehindCacheManager         wbcm = null;
    private WriteBehindCallback             wbCallback = null;


    private static Object[] cacheProps =
        new Object[] { 
            Entity01.class, Embeddable01.class, Entity02.class, CLEAR_TABLES, RETAIN_DATA,
            "openjpa.DataCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm", 
            "openjpa.WriteBehindCache", "true",
            "openjpa.WriteBehindCallback", "true(sleepTime=15000)", 
            "openjpa.RuntimeUnenhancedClasses", "unsupported",
            "openjpa.Log", "DefaultLevel=WARN" 
        };

    private static Object [] noCacheProps = 
        new Object[] { 
            Entity01.class, Embeddable01.class, Entity02.class, CLEAR_TABLES, RETAIN_DATA,
            "openjpa.RuntimeUnenhancedClasses", "unsupported",
            "openjpa.Log", "DefaultLevel=WARN" 
        };

    public void setUp() throws Exception {
        // 
        // Create and verify the necessary WriteBehind cache objects
        // 
        emf = createEMF(cacheProps);
        assertNotNull(emf);
        em = emf.createEntityManager();
        assertNotNull(em);
        wbcm = emf.getConfiguration().getWriteBehindCacheManagerInstance();
        assertNotNull(wbcm);
        wbCache = wbcm.getSystemWriteBehindCache();
        assertNotNull(wbCache);
        wbCallback = emf.getConfiguration().getWriteBehindCallbackInstance();
        assertNotNull(wbCallback);

        // 
        // Create and verify the non-WriteBehind cache objects
        // 
        emf_noCache = createEMF(noCacheProps);
        assertNotNull(emf_noCache);
        em_noCache = emf_noCache.createEntityManager();
        assertNotNull(em_noCache);

        //
        // Clear the persistence contexts
        //
        em.clear();
        em_noCache.clear();

        // 
        // Clear the WriteBehind cache
        //
        wbCache.clear();
        assertTrue(wbCache.getSize() == 0);
        assertTrue(wbCache.isEmpty());
    }

    public void tearDown() {
        // 
        // WriteBehind cache EMF/EM
        //
        if (em != null) {
            em.close();
        }
        if (emf != null) {
            emf.close();
        }

        // 
        // Non-WriteBehind cache EMF/EF
        //
        if (em_noCache != null) {
            em_noCache.close();
        }
        if (emf_noCache != null) {
            emf_noCache.close();
        }
    }


    /**
     * Insert Entity01/Embeddable01 with manual flush of WriteBehind cache
     *
     * @exception Exception
     */
/*  public void testEntity01ManualFlush() { 

        // 
        // Create a new instance of the entity/embeddable class
        // 
        Entity01 newEntity01 = new Entity01();
        newEntity01.setId(1);
        newEntity01.setEnt01_str01("AA");
        newEntity01.setEnt01_str02("BBBB");
        newEntity01.setEnt01_str03("CCCCCCCC");
        newEntity01.setEnt01_int01(1);
        newEntity01.setEnt01_int02(2);
        newEntity01.setEnt01_int03(3);
        newEntity01.setEmb01_int01(4);
        newEntity01.setEmb01_int02(5);
        newEntity01.setEmb01_int03(6);

        // 
        // Persist the new entity/embeddable in the WriteBehind cache
        // 
        em.getTransaction().begin();
        em.persist(newEntity01);
        em.getTransaction().commit();

        // 
        // Verify the entity was saved in the WriteBehind cache
        // 
        assertTrue(wbCache.getSize() > 0);
        assertFalse(wbCache.isEmpty());
        assertTrue(wbCache.contains(newEntity01));

        // 
        // Verify the entity has not yet been saved in the database
        // 
        Entity01 findEntity01 = em_noCache.find(Entity01.class, 1);
        assertNull(findEntity01);

        // 
        // Flush the WriteBehind cache
        // 
        Collection<Exception> exceptions = wbCallback.flush();
        assertTrue(exceptions.size() == 0);
        assertTrue(wbCache.getSize() == 0);
        assertTrue(wbCache.isEmpty());

        // 
        // Verify the entity/embeddable has now been saved in the database
        // 
        em_noCache.clear();
        findEntity01 = em_noCache.find(Entity01.class, 1);
        assertNotNull(findEntity01);
        assertEquals(findEntity01.getId(), 1);
        assertEquals(findEntity01.getEnt01_str01(), "AA");
        assertEquals(findEntity01.getEnt01_str02(), "BBBB");
        assertEquals(findEntity01.getEnt01_str03(), "CCCCCCCC");
        assertEquals(findEntity01.getEnt01_int01(), 1);
        assertEquals(findEntity01.getEnt01_int02(), 2);
        assertEquals(findEntity01.getEnt01_int03(), 3);
        assertEquals(fndEntity01.getEmb01_int01(), 4);
        assertEquals(findEntity01.getEmb01_int02(), 5);
        assertEquals(findEntity01.getEmb01_int03(), 6);
    }  */


    /**
     * Insert Entity02 with manual flush of WriteBehind cache
     *
     * @exception Exception
     */
    public void testEntity02ManualFlush() { 

        // 
        // Create a new instance of the entity class
        // 
        Entity02 newEntity02 = new Entity02();
        newEntity02.setId(2);
        newEntity02.setEnt02_str01("DD");
        newEntity02.setEnt02_str02("EEEE");
        newEntity02.setEnt02_str03("FFFFFFFF");
        newEntity02.setEnt02_int01(7);
        newEntity02.setEnt02_int02(8);
        newEntity02.setEnt02_int03(9);

        // 
        // Persist the new entity in the WriteBehind cache
        // 
        em.getTransaction().begin();
        em.persist(newEntity02);
        em.getTransaction().commit();

        // 
        // Verify the entity was saved in the WriteBehind cache
        // 
        assertTrue(wbCache.getSize() > 0);
        assertFalse(wbCache.isEmpty());
        assertTrue(wbCache.contains(newEntity02));

        // 
        // Verify the entity has not yet been saved in the database
        // 
        Entity02 findEntity02 = em_noCache.find(Entity02.class, 2);
        assertNull(findEntity02);

        // 
        // Flush the WriteBehind cache
        // 
        Collection<Exception> exceptions = wbCallback.flush();
        assertTrue(exceptions.size() == 0);
        assertTrue(wbCache.getSize() == 0);
        assertTrue(wbCache.isEmpty());

        // 
        // Verify the entity has now been saved in the database
        // 
        em_noCache.clear();
        findEntity02 = em_noCache.find(Entity02.class, 2);
        assertNotNull(findEntity02);
        assertEquals(findEntity02.getId(), 2);
        assertEquals(findEntity02.getEnt02_str01(), "DD");
        assertEquals(findEntity02.getEnt02_str02(), "EEEE");
        assertEquals(findEntity02.getEnt02_str03(), "FFFFFFFF");
        assertEquals(findEntity02.getEnt02_int01(), 7);
        assertEquals(findEntity02.getEnt02_int02(), 8);
        assertEquals(findEntity02.getEnt02_int03(), 9);
    } 
}

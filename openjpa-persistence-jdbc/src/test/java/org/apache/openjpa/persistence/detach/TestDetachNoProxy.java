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
package org.apache.openjpa.persistence.detach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestDetachNoProxy extends SingleEMFTestCase {
    
    private static final int numEntities = 3;
    private static final String PROXY = new String("$proxy");
    private Log log;
    
    public void setUp() {
        setUp(DROP_TABLES, Entity20.class);
        log = emf.getConfiguration().getLog("test");
        
        /* This code is only for 2.0 and later
        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        assertNotNull(compat);
        if (log.isTraceEnabled()) {
            log.info("Before set, FlushBeforeDetach=" + compat.getFlushBeforeDetach());
            log.info("Before set, CopyOnDetach=" + compat.getCopyOnDetach());
            log.info("Before set, CascadeWithDetach=" + compat.getCascadeWithDetach());
        }
        compat.setFlushBeforeDetach(false);
        compat.setCopyOnDetach(false);
        compat.setCascadeWithDetach(false);
        if (log.isTraceEnabled()) {
            log.info("After set, FlushBeforeDetach=" + compat.getFlushBeforeDetach());
            log.info("After set, CopyOnDetach=" + compat.getCopyOnDetach());
            log.info("After set, CascadeWithDetach=" + compat.getCascadeWithDetach());
        }
        */
        createEntities(numEntities);
    }
    
    private void createEntities(int count) {
        Entity20 e20 = null;
        OpenJPAEntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i=0; i<count; i++) {
            e20 = new Entity20(i);
            em.persist(e20);
        }
        em.getTransaction().commit();
        em.close();
    }
    
    /* 
     * Verify that returned copy of detached entity does not use the proxy classes.
     */
    public void testDetach() {
        if (log.isTraceEnabled())
            log.info("***** testDetach() *****");
        Integer id = new Integer(0);
        OpenJPAEntityManager em = emf.createEntityManager();
        
        em.clear();
        Entity20 e20 = em.find(Entity20.class, id);
        if (log.isTraceEnabled())
            log.trace("** after find");
        assertTrue(em.contains(e20));
        verifySerializable(e20, true);
        
        // pre openjpa-2.0.0 behavior, where detach() returned the updated entity
        Entity20 e20Copy = em.detach(e20);
        if (log.isTraceEnabled())
            log.trace("** after detach");
        // original entity should have proxy classes and should not be detached
        assertFalse(em.isDetached(e20));
        verifySerializable(e20, true);
        // returned entity should not have any proxy classes and should be detached
        assertTrue(em.isDetached(e20Copy));
        verifySerializable(e20Copy, false);
               
        em.close();
    }

    /* 
     * This only works on 2.0.0 and later - new method
     * Verify that a detachCopy() returned entity does not contain any proxy classes.
     *
    public void testDetachCopy() {
        if (log.isTraceEnabled())
            log.info("***** testDetachCopy() *****");
        Integer id = new Integer(0);
        OpenJPAEntityManager em = emf.createEntityManager();
        em.clear();

        Entity20 e20 = em.find(Entity20.class, id);
        if (log.isTraceEnabled())
            log.trace("** after find");
        assertTrue(em.contains(e20));
        verifySerializable(e20, true);
                        
        // This only works on 2.0 and later - new method
        Entity20 e20copy = em.detachCopy(e20);
        if (log.isTraceEnabled())
            log.trace("** after detachCopy");
        assertTrue(em.isDetached(e20copy));
        verifySerializable(e20copy, false);
        
        em.close();
    }
    */

    /*
     * Verify that returned copies of detachAll() entities do not use the proxy classes.
     */
    public void testDetachAll() {
        if (log.isTraceEnabled())
            log.info("***** testDetachAll() *****");
        OpenJPAEntityManager em = emf.createEntityManager();
        em.clear();

        ArrayList<Entity20> e20List = new ArrayList<Entity20>(numEntities);
        for (int i=0; i<numEntities; i++) {
            Entity20 e20 = em.find(Entity20.class, new Integer(i));
            e20List.add(e20);
            if (log.isTraceEnabled())
                log.trace("** after find Entity20(" + i + ")");
            assertTrue(em.contains(e20));
            verifySerializable(e20, true);            
        }
        
        // pre openjpa-2.0.0 behavior, where detachAll() returned the updated entities
        ArrayList<Entity20> e20ListCopy = new ArrayList<Entity20>(em.detachAll(e20List));
        for (int i=0; i<numEntities; i++) {
            if (log.isTraceEnabled())
                log.trace("** after EM.detachAll() verify e20List(" + i + ")");
            Entity20 e20 = e20List.get(i);
            // original entity should have proxy classes and should not be detached
            assertFalse(em.isDetached(e20));
            verifySerializable(e20, true);
        }
        for (int i=0; i<numEntities; i++) {
            if (log.isTraceEnabled())
                log.trace("** after EM.detachAll() verify e20ListCopy(" + i + ")");
            Entity20 e20 = e20ListCopy.get(i);
            // entity should not have any proxy classes and should be detached
            assertTrue(em.isDetached(e20));
            verifySerializable(e20, false);
        }

        em.close();
    }

    /*
     * Verify that after EM.clear() in-place detached entities do not contain any proxy classes.
     */
    public void testClear() {
        if (log.isTraceEnabled())
            log.info("***** testClear() *****");
        OpenJPAEntityManager em = emf.createEntityManager();
        em.clear();

        ArrayList<Entity20> e20List = new ArrayList<Entity20>(numEntities);
        for (int i=0; i<numEntities; i++) {
            Entity20 e20 = em.find(Entity20.class, new Integer(i));
            e20List.add(e20);
            if (log.isTraceEnabled())
                log.trace("** after find Entity20(" + i + ")");
            assertTrue(em.contains(e20));
            verifySerializable(e20, true);            
        }

        em.clear();
        for (int i=0; i<numEntities; i++) {
            if (log.isTraceEnabled())
                log.trace("** after EM.clear() verify Entity20(" + i + ")");
            Entity20 e20 = e20List.get(i);
            assertTrue(em.isDetached(e20));
            verifySerializable(e20, false);
        }

        em.close();
    }

    
    private void verifySerializable(Entity20 e20, boolean usesProxy) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        byte[] e20bytes = null;
        
        if (log.isTraceEnabled())
            log.trace("verifySerializable() - before serialize");
        verifyEntities(e20, usesProxy);

        // first serialize
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(e20);
            e20bytes = baos.toByteArray();
        } catch (IOException e) {
            fail(e.toString());
        } finally {
            try {
                if (oos != null)
                    oos.close();
            } catch (IOException e) {
            }
        }
        
        // then deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(e20bytes);
        ObjectInputStream ois = null;
        Entity20 e20new = null;
        try {
            ois = new ObjectInputStream(bais);
            e20new = (Entity20) ois.readObject();
            if (log.isTraceEnabled())
                log.trace("verifySerializable() - after deserialize");
            verifyEntities(e20new, false);
        } catch (IOException e) {
            fail(e.toString());
        } catch (ClassNotFoundException e) {
            fail(e.toString());
        } finally {
            try {
                if (ois != null)
                    ois.close();
            } catch (IOException e) {
            }
        }

    }

    private void verifyEntities(Entity20 e20, boolean usesProxy) {
        if (log.isTraceEnabled()) {
            printClassNames(e20);
            log.trace("asserting expected proxy usage");
        }
        assertTrue("Expected sqlDate endsWith($proxy) to return " + usesProxy,
            usesProxy == e20.getDate().getClass().getCanonicalName().endsWith(PROXY));
        assertTrue("Expected sqlTime endsWith($proxy) to return " + usesProxy,
            usesProxy == e20.getTime().getClass().getCanonicalName().endsWith(PROXY));
        assertTrue("Expected sqlTimestamp endsWith($proxy) to return " + usesProxy,
            usesProxy == e20.getTimestamp().getClass().getCanonicalName().endsWith(PROXY));
        
    }
    
    private void printClassNames(Entity20 e20) {
        log.info("sqlDate = " + e20.getDate().getClass().getCanonicalName());
        log.info("sqlTime = " + e20.getTime().getClass().getCanonicalName());
        log.info("sqlTimestamp = " + e20.getTimestamp().getClass().getCanonicalName());
    }
    
}

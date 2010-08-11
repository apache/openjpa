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
package org.apache.openjpa.kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;

import org.apache.openjpa.jdbc.kernel.JDBCStoreManager;
import org.apache.openjpa.persistence.EntityManagerImpl;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.relations.TblChild;
import org.apache.openjpa.persistence.relations.TblGrandChild;
import org.apache.openjpa.persistence.relations.TblParent;
import org.apache.openjpa.persistence.simple.Person;

/*
 * Verify multiple permutations of openjpa.jdbc.QuerySQLCache settings.
 */
public class TestQuerySQLCache
    extends TestCase {
    
    final int nThreads = 5;
    final int nPeople = 100;
    final int nIterations = 10;

    /*
     * Verify QuerySQLCacheValue setting "all" is caching queries.
     */
    public void testAllCacheSetting() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", "all");
        OpenJPAEntityManagerFactorySPI emf = (OpenJPAEntityManagerFactorySPI)
            OpenJPAPersistence.cast(
                Persistence.createEntityManagerFactory("test", props));
        
        EntityManagerImpl em = (EntityManagerImpl)emf.createEntityManager();
        BrokerImpl broker = (BrokerImpl) em.getBroker();
        DelegatingStoreManager dstore = broker.getStoreManager();
        JDBCStoreManager jstore = 
            (JDBCStoreManager)dstore.getInnermostDelegate();

        em.getTransaction().begin();
        Person p = new Person();
        p.setId(1);
        em.persist(p);
        em.flush();
        em.getTransaction().commit();
        
        Person p1 = em.find(Person.class, 1);
        Map sqlCache = jstore.getQuerySQLCache();
        Set keys = sqlCache.keySet();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            Map cacheMap = (Map) iter.next();
            //make sure there is an entry in the cache
            assertEquals(1, cacheMap.size());   
        }
        
        em.getTransaction().begin();
        em.remove(p);
        em.flush();
        em.getTransaction().commit();
        
        em.close();
        emf.close();
    }
    
    /*
     * Verify QuerySQLCacheValue setting "true" uses the expected cache
     * implementation and is caching.
     */
    public void testTrueCacheSetting() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", "true");
        OpenJPAEntityManagerFactorySPI emf = (OpenJPAEntityManagerFactorySPI)
            OpenJPAPersistence.cast(
                Persistence.createEntityManagerFactory("test", props));
        
        EntityManagerImpl em = (EntityManagerImpl)emf.createEntityManager();
        BrokerImpl broker = (BrokerImpl) em.getBroker();
        DelegatingStoreManager dstore = broker.getStoreManager();
        JDBCStoreManager jstore = 
            (JDBCStoreManager)dstore.getInnermostDelegate();
        
        em.getTransaction().begin();
        Person p = new Person();
        p.setId(1);
        em.persist(p);
        em.flush();
        em.getTransaction().commit();
        
        Person p1 = em.find(Person.class, 1);
        Map sqlCache = jstore.getQuerySQLCache();
        Set keys = sqlCache.keySet();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            Map cacheMap = (Map) iter.next();
            //make sure there is an entry in the cache
            assertEquals(1, cacheMap.size());   
        }
        
        em.getTransaction().begin();
        em.remove(p);
        em.flush();
        em.getTransaction().commit();
        
        em.close();
        emf.close();
    }

    /*
     * Verify caching is disabled when the QuerySQLCacheValue setting is 
     * "false".
     */
    public void testFalseCacheSetting() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", "false");
        OpenJPAEntityManagerFactorySPI emf = (OpenJPAEntityManagerFactorySPI)
            OpenJPAPersistence.cast(
                Persistence.createEntityManagerFactory("test", props));
        
        EntityManagerImpl em = (EntityManagerImpl)emf.createEntityManager();
        BrokerImpl broker = (BrokerImpl) em.getBroker();
        DelegatingStoreManager dstore = broker.getStoreManager();
        JDBCStoreManager jstore = 
            (JDBCStoreManager)dstore.getInnermostDelegate();
        
        em.getTransaction().begin();
        Person p = new Person();
        p.setId(1);
        em.persist(p);
        em.flush();
        em.getTransaction().commit();
        
        Person p1 = em.find(Person.class, 1);

        assertFalse(jstore.isQuerySQLCacheOn());
        
        em.getTransaction().begin();
        em.remove(p);
        em.flush();
        em.getTransaction().commit();
        
        em.close();
        emf.close();
    }

    /*
     * Verify QuerySQLCacheValue setting with a custom cache backend uses
     * the expected cache implementation and is caching.
     */
    public void testCustomCacheSetting() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", 
            "org.apache.openjpa.kernel.TestQuerySQLCache.CustomCacheMap");
        OpenJPAEntityManagerFactorySPI emf = (OpenJPAEntityManagerFactorySPI)
            OpenJPAPersistence.cast(
                Persistence.createEntityManagerFactory("test", props));
        
        EntityManagerImpl em = (EntityManagerImpl)emf.createEntityManager();
        BrokerImpl broker = (BrokerImpl) em.getBroker();
        DelegatingStoreManager dstore = broker.getStoreManager();
        JDBCStoreManager jstore = 
            (JDBCStoreManager)dstore.getInnermostDelegate();
        
        em.getTransaction().begin();
        Person p = new Person();
        p.setId(1);
        em.persist(p);
        em.flush();
        em.getTransaction().commit();
        
        Person p1 = em.find(Person.class, 1);

        assertTrue(jstore.isQuerySQLCacheOn());

        Map sqlCache = jstore.getQuerySQLCache();
        Set keys = sqlCache.keySet();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            Map cacheMap = (Map) iter.next();
            assertTrue((cacheMap instanceof 
                org.apache.openjpa.kernel.TestQuerySQLCache.CustomCacheMap));
            //make sure there is an entry in the cache
            assertEquals(1, cacheMap.size());   
        }
        
        em.getTransaction().begin();
        em.remove(p);
        em.flush();
        em.getTransaction().commit();
        
        em.close();
        emf.close();
    }

    /*
     * Verify an exception is thrown if a bad cache implementation class
     * is specified.
     */
    public void testBadCustomCacheSetting() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", 
            "org.apache.openjpa.kernel.TestQuerySQLCache.BadCacheMap");
        
        try {
            OpenJPAEntityManagerFactorySPI emf = 
                (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.cast(
                        Persistence.createEntityManagerFactory("test", props));
            // EMF creation should throw an exception because the cache 
            // implementation class will not be found.
            assertFalse(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /*
     * Verify multi-threaded multi-entity manager finder works with the
     * QuerySQLCache set to "all".
     */
    public void testMultiEMCachingAll() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", 
            "all");
        runMultiEMCaching(props);        
    }

    /*
     * Verify multi-threaded multi-entity manager finder works with the
     * QuerySQLCache set to "true".
     */
    public void testMultiEMCachingTrue() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + Person.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", 
            "true");
        runMultiEMCaching(props);
    }

    /*
     * Verify QuerySQLCacheValue setting "true" uses the expected cache
     * implementation and is caching.
     */
    public void testEagerFetch() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types="
            + TblChild.class.getName() + ";"
            + TblGrandChild.class.getName() + ";"
            + TblParent.class.getName() + ")");
        props.put("openjpa.jdbc.QuerySQLCache", "true");
        OpenJPAEntityManagerFactorySPI emf = (OpenJPAEntityManagerFactorySPI)
            OpenJPAPersistence.cast(
                Persistence.createEntityManagerFactory("test", props));
        
        EntityManagerImpl em = (EntityManagerImpl)emf.createEntityManager();

        em.getTransaction().begin();
        em.createQuery("Delete from TblGrandChild").executeUpdate();
        em.createQuery("Delete from TblChild").executeUpdate();
        em.createQuery("Delete from TblParent").executeUpdate();
        em.getTransaction().commit();
        em.close();
        
        em = (EntityManagerImpl) emf.createEntityManager();
        
        
        em.getTransaction().begin();
        for (int i = 0; i < 2; i++) {
        	TblParent p = new TblParent();
        	p.setParentId(i);
    		TblChild c = new TblChild();
    		c.setChildId(i);
            c.setTblParent(p);
            p.addTblChild(c);
     		em.persist(p);
    		em.persist(c);

    		TblGrandChild gc = new TblGrandChild();
    		gc.setGrandChildId(i);
    		gc.setTblChild(c);
    		c.addTblGrandChild(gc);
    		
    		em.persist(p);
    		em.persist(c);
    		em.persist(gc);
        }
        em.flush();
        em.getTransaction().commit();
        em.clear();
        
        for (int i = 0; i < 2; i++) {
        	TblParent p = em.find(TblParent.class, i);
        	int pid = p.getParentId();
        	assertEquals(pid, i);
        	Collection<TblChild> children = p.getTblChildren();
        	boolean hasChild = false;
        	for (TblChild c : children) {
        		hasChild = true;
        		Collection<TblGrandChild> gchildren = c.getTblGrandChildren();
        		int cid = c.getChildId();
        		assertEquals(cid, i);
	        	boolean hasGrandChild = false;
        		for (TblGrandChild gc : gchildren) {
        			hasGrandChild = true;
        			int gcId = gc.getGrandChildId();
        			assertEquals(gcId, i);
        		}
        		assertTrue(hasGrandChild);
        	}
        	assertTrue(hasChild);
        }
        em.close();
        emf.close();
    }

    private void runMultiEMCaching(Map props) {

        EntityManagerFactory emfac = 
                Persistence.createEntityManagerFactory("test", props);

        EntityManager em = emfac.createEntityManager();            

        // Create some entities
        em.getTransaction().begin();
        for (int i = 0; i < nPeople; i++)
        {
            Person p = new Person();
            p.setId(i);
            em.persist(p);
        }
        em.flush();
        em.getTransaction().commit();
        em.close();

        Thread[] newThreads = new Thread[nThreads];
        FindPeople[] customer = new FindPeople[nThreads];
        
        for (int i=0; i < nThreads; i++) {
            customer[i] = new FindPeople(emfac, 0, nPeople, 
                nIterations, i);
            newThreads[i] = new Thread(customer[i]);
            newThreads[i].start();
        }
        
        // Wait for the worker threads to complete
        for (int i = 0; i < nThreads; i++) {
            try {
                newThreads[i].join();
            } catch (InterruptedException e) {
                this.fail("Caught Interrupted Exception: " + e);
            }
        }   

        // Run through the state of all runnables to assert if any of them
        // failed.
        for (int i = 0; i < nThreads; i++) {
            assertFalse(customer[i].hadFailures());
        }

        // Clean up the entities used in this test
        em = emfac.createEntityManager();            
        em.getTransaction().begin();

        for (int i = 0; i < nPeople; i++) {
            Person p = em.find(Person.class, i);
            em.remove(p);
        }
        em.flush();
        em.getTransaction().commit();
        em.close();
    }
    
    /*
     * Empty ConcurrentHashMap subclass. Useful for testing custom cache
     * storage implementations.
     */
    public class CustomCacheMap extends ConcurrentHashMap {
        
    }

    /*
     * Simple runnable to test finder in a tight loop.  Multiple instances
     * of this runnable will run simultaneously.
     */
    private class FindPeople implements Runnable {
        
        private int startId;
        private int endId;
        private int thread;
        private int iterations;
        private EntityManagerFactory emf;
        private boolean failures = false;
        
        public FindPeople(EntityManagerFactory emf, 
            int startId, int endId, int iterations, int thread) {
            super();
            this.startId = startId;
            this.endId = endId;
            this.thread = thread;
            this.iterations = iterations;
            this.emf = emf;
        }
        
        public boolean hadFailures()
        {
            return failures;
        }
        
        public void run() {
            try {            
                EntityManager em = emf.createEntityManager();            
                for (int j = 0; j < iterations; j++) {
                    
                    for (int i = startId; i < endId; i++) {
                        Person p1 = em.find(Person.class, i);
                        if (p1.getId() != i) {
                            System.out.println("Finder failed: " + i);
                            failures = true;
                            break;
                        }                    
                    }
                    em.clear();  
                }
                em.close();  
            } 
            catch (Exception e) {
               failures = true;
               System.out.println("Thread " + thread + " exception :" +
                   e );
            }
        }
    }
}

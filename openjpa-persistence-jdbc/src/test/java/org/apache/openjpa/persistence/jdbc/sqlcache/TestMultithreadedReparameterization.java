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

package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import junit.framework.TestCase;

import org.apache.openjpa.kernel.QueryStatistics;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * Test reparameterization of cached query under heavy load. 
 * 
 * @author Pinaki Poddar
 *
 */
public class TestMultithreadedReparameterization extends TestCase {
    private static String RESOURCE = "META-INF/persistence.xml"; 
    private static String UNIT_NAME = "PreparedQuery";
    protected static OpenJPAEntityManagerFactory emf;

    public void setUp() throws Exception {
        super.setUp();
        if (emf == null) {
            Properties config = new Properties();
            config.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true,SchemaAction='drop,add')");
            config.put("openjpa.Log", "SQL=WARN");
            config.put("openjpa.jdbc.QuerySQLCache", "true(EnableStatistics=true, MaxCacheSize=2)");
            config.put("openjpa.ConnectionFactoryProperties", "PrintParameters=true");
            emf = OpenJPAPersistence.createEntityManagerFactory(UNIT_NAME, RESOURCE, config);
        }
    }

    public void testReparameterizationUnderHeavyLoad() throws Exception {
        long baseId = System.currentTimeMillis();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        int nThreads = 80;
        for (int i = 0; i < nThreads; i++) {
            Person p = new Person();
            p.setId(baseId+i);
            p.setFirstName("First"+i);
            p.setLastName("Last"+i);
            p.setAge((short)(20+i));
            em.persist(p);
        }
        em.getTransaction().commit();

        String jpql = "select p from Person p " 
                    + "where p.id=:id and p.firstName=:first and p.lastName=:last and p.age=:age";
        int nRepeats = 20;
        Thread[] threads = new Thread[nThreads];
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        for (int i = 0; i < nThreads; i++) {
            Object[] args = {"id", baseId+i, "first", "First"+i, "last", "Last"+i, "age", (short)(20+i)};
            QueryThread thread = new QueryThread(emf.createEntityManager(), jpql, args, nRepeats, exceptions);
            threads[i] = new Thread(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        try {
            QueryStatistics<String> stats = emf.getConfiguration().getQuerySQLCacheInstance().getStatistics();
            for(Throwable t : exceptions) {
                fail((t.getCause() != null ? t.getCause().toString() : t.toString()));
            }
            assertEquals(nThreads*nRepeats,stats.getExecutionCount(), stats.getExecutionCount(jpql));
            assertEquals(nThreads*nRepeats-1,stats.getExecutionCount(), stats.getHitCount(jpql));
        } finally {
            //clear statistics for other tests
            emf.getConfiguration().getQuerySQLCacheInstance().clear();
        }
    }

    /**
     *  This is a test to verify that the PreparedQueryCache correctly swaps queries between
     *  the hard and the soft cache maps. It is important for this test that the max cache size
     *  is set to a number much smaller than the default (1000) to force swapping between hard
     *  and soft maps. During this swapping interval, it is possible that another thread will
     *  attempt to read from the maps and cause either NPE or CCE. 
     *  
     * @see OPENJPA-2646
     * @throws Exception
     */
    public void testCacheSwappingUnderHeavyLoad() throws Exception {
        final int nRuns = 10;
        final int nThreads = 20;
        //This value needs to be more than the max cache size to reliably cause cache
        //overflow to start swapping between hard -> soft cache
        // ("openjpa.jdbc.QuerySQLCache", "true(MaxCacheSize=2")
        final int nQueries = 10;

        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        for (int y = 0; y < nRuns; y++) {
            Thread[] threads = new Thread[nThreads];
            for (int i = 0; i < nThreads; i++) {
                threads[i] = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            EntityManager em = emf.createEntityManager();
                            // Since the cache (CacheMap) is set to a size of '2' all threads will 
                            // fill up the cache and constantly cause query strings to move 
                            // to/from the main cache and soft cache, eventually causing a 
                            // "cache miss" by a thread.
                            String qStr = "select p from Person p where p.firstName=:first and p.id = ";
                            for (int j = 0; j < nQueries; j++) {
                                Query q = em.createQuery(qStr + j);
                                q.setParameter("first", "test");
                                q.getResultList();
                            }
                            em.close();
                        } catch (Throwable t) {
                            System.err.println("\nThread (" + Thread.currentThread().getName()
                                    + "): Caught the following exception: " + t
                                    + "\n  With cause: " + t.getCause());
                            //catch the AssertionError so that we can fail the main Thread
                            exceptions.add(t);
                        }
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                synchronized (thread) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                for(Throwable t : exceptions) {
                    fail((t.getCause() != null ? t.getCause().toString() : t.toString()));
                }
            } finally {
                //clear statistics for other tests
                emf.getConfiguration().getQuerySQLCacheInstance().clear();
            }
        }
    }

    /**
     * Each thread executes same query with same parameters repeatedly.
     * 
     * @author Pinaki Poddar
     *
     */
    public static class QueryThread implements Runnable {
        public final EntityManager em;
        public final String jpql;
        public final Object[] args;
        public final int nTimes;
        public final List<Throwable> exceptions;
        public QueryThread(EntityManager em, String jpql, Object[] args, int r, List<Throwable> exceptions) {
            this.em = em;
            this.jpql = jpql;
            this.args = args;
            this.nTimes = r;
            this.exceptions = exceptions;
        }

        public void run()  {
            try {
                for (int i = 0; i < nTimes; i++) {
                    TypedQuery<Person> q = em.createQuery(jpql, Person.class);
                    for (int j = 0; j < args.length; j += 2) {
                        q.setParameter(args[j].toString(), args[j+1]);
                    }
                    List<Person> result = q.getResultList();
                    assertEquals(Thread.currentThread() + " failed", 1, result.size());
                    Person p = result.get(0);
                    assertEquals(args[1], p.getId());
                    assertEquals(args[3], p.getFirstName());
                    assertEquals(args[5], p.getLastName());
                    assertEquals(args[7], p.getAge());
                }
            } catch (Throwable t) {
                //catch the AssertionError so that we can fail the main Thread
                exceptions.add(t);
            }
        }
    }
}

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
package org.apache.openjpa.persistence.kernel;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DB2Dictionary;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.PessimisticLockException;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestPessimisticLockException extends SQLListenerTestCase {
    int pKey = 1;
    static boolean doSleep = true;

    @Override
    public void setUp() throws Exception {
        super.setUp(PessimisticLockEntity.class);
    }

    /*
     * This test has only been verified on DB2 and Oracle.
     */
    protected boolean skipTest() {
        if (emf.getConfiguration() instanceof JDBCConfiguration) {
            DBDictionary inst = ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance();
            return !((inst instanceof DB2Dictionary) || (inst instanceof OracleDictionary));
        }
        return true;
    }

    /*
     * This test will verify that two threads get a an appropriate pessimistic lock
     * when they both request one at the same time.  See JIRA OPENJPA-2547 for a more
     * detailed description of this test.
     */
    public void testPessimisticLockException() {
        if (!skipTest()) {

            populate();

            TestThread t1 = new TestThread();
            TestThread t2 = new TestThread();
            t1.start();
            t2.start();

            while ((t1.isAlive() || t2.isAlive())) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }

            // One, and only one, thread should get a PersistenceException
            if (t1.gotPLEx && t2.gotPLEx) {
                fail("Both threads got a PersistenceLockException!  "
                    + "Only one thread should have received a PersistenceLockException");
            } else if (!t1.gotPLEx && !t2.gotPLEx) {
                fail("Neither thread got a PersistenceLockException!  "
                    + "One thread should have received a PersistenceLockException");
            } else if (t1.count < 2 && t2.count < 2) {
                fail("PersistenceLockException was received, but not the expected number of times!  "
                    + "One thread should have received a PersistenceLockException at least twice.");
            }
        }
    }

    private class TestThread extends Thread {
        boolean gotPLEx = false;
        int count = 0;

        @Override
        public synchronized void run() {
            OpenJPAEntityManager oem = OpenJPAPersistence.cast(emf.createEntityManager());
            oem.getTransaction().begin();

            PessimisticLockEntity entity = oem.find(PessimisticLockEntity.class, pKey);

            boolean locked = false;
            while (!locked) {
                try {
                    oem.getFetchPlan().setLockTimeout(5000);
                    oem.lock(entity, LockModeType.PESSIMISTIC_READ);
                    locked = true;
                } catch (PessimisticLockException ple) {
                    gotPLEx = true;
                    count++;

                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException ie) {
                    }
                    oem.refresh(entity);
                } catch (Throwable pe) {
                    pe.printStackTrace();
                    fail("Caught an unexepected exception: " + pe);
                }

                // Only one thread needs to sleep (don't care about synchronization of 'doSleep' at this
                // point - if both threads happen to get here at the same time we will test for that later.)
                if (doSleep) {
                    doSleep = false;
                    try {
                        // Sleep log enough to ensure the other thread times out at least two times.
                        Thread.sleep(15000);
                    } catch (final InterruptedException ie) {
                    }
                }

                if (!oem.getTransaction().getRollbackOnly()) {
                    oem.getTransaction().commit();
                }
            }
        }
    }

    /*
     * This test verifies the correct number of SQL statements when using a pessimistic
     * lock (See JIRA OPENJPA-2449).  Prior to OPENJPA-2449, when requesting a pessimistic lock
     * we would do a 'select' to get the entity, and turn around and do another select to get a
     * Pessimistic lock...in other words, we'd generate (on DB2) these two SQL statements for the refresh:
     *
     * SELECT t0.name FROM PessimisticLockEntity t0 WHERE t0.id = ?
     * SELECT t0.name FROM PessimisticLockEntity t0 WHERE t0.id = ?  FOR READ ONLY WITH RR USE AND KEEP UPDATE LOCKS
     *
     * With the fix of OPENJPA-2449, we generate only one select, as follows:
     *
     * SELECT t0.name FROM PessimisticLockEntity t0 WHERE t0.id = ?  FOR READ ONLY WITH RR USE AND KEEP UPDATE LOCKS
     *
     * Not only does this save an SQL, but more importantly, the few millisecond delay between the two selects
     * won't occur.....in a multi-threaded env this delay could cause another thread to get the lock over this
     * one when the refresh occurs at the same time.
     */
    public void testSQLCount() {
        if (!skipTest()) {

            populate();
            resetSQL();

            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();

            PessimisticLockEntity plEnt = em.find(PessimisticLockEntity.class, pKey);

            em.refresh(plEnt, LockModeType.PESSIMISTIC_WRITE);

            plEnt.setName("test");
            em.getTransaction().commit();
            em.close();
            assertEquals("There should only be 3 SQL statements", 3, getSQLCount());
        }
    }

    public void populate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        PessimisticLockEntity pt = new PessimisticLockEntity();
        pt.setId(pKey);

        em.persist(pt);

        em.getTransaction().commit();
        em.close();
    }
}

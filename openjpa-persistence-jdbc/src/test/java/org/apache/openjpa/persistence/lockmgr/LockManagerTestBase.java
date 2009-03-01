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
package org.apache.openjpa.persistence.lockmgr;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Base class to support testing "version", "pessimistic" and "jpa2"
 * lock manager behaviors.
 */
public abstract class LockManagerTestBase extends SQLListenerTestCase {
    
    protected final SQLAssertions commonSelect = new SQLAssertions(
        SQLAssertType.AnySQLAnyOrder,
        "SELECT .* FROM LockEmployee .*"
    );
    protected final SQLAssertions commonForUpdate = new SQLAssertions(
        SQLAssertType.NoneSQLAnyOrder,
        ".* FOR UPDATE .*",
        ".* KEEP UPDATE LOCKS.*"        // DB2 SQL Statement Lock Pattern
    );
    protected final SQLAssertions commonSelectForUpdate = new SQLAssertions(
        SQLAssertType.AnySQLAnyOrder,
        "SELECT .* FROM LockEmployee .* FOR UPDATE .*",
        "SELECT .* FROM LockEmployee .* KEEP UPDATE LOCKS.*"
    );
    
    protected static final Class<?>[] ExpectingOptimisticLockExClass = 
        new Class<?>[] { javax.persistence.OptimisticLockException.class };
    protected static final Class<?>[] ExpectingPessimisticLockExClass = 
        new Class<?>[] { javax.persistence.PessimisticLockException.class };
    protected static final Class<?>[] ExpectingLockTimeoutExClass = 
        new Class<?>[] { javax.persistence.LockTimeoutException.class};
    protected static final Class<?>[] ExpectingAnyLockExClass = 
        new Class<?>[] { javax.persistence.PessimisticLockException.class,
                         javax.persistence.LockTimeoutException.class
                       };

    protected static final String Default_FirstName          = "Def FirstName";
    
    protected static final String TxtLockRead                = "[Lock Read, ";
    protected static final String TxtLockWrite               = "[Lock Write, ";
    protected static final String TxtLockOptimistic          = "[Lock Opt, ";
    protected static final String TxtLockOptimisticForceInc  = "[Lock OptFI, ";
    protected static final String TxtLockPessimisticRead     = "[Lock PesRd, ";
    protected static final String TxtLockPessimisticWrite    = "[Lock PesWr, ";
    protected static final String TxtLockPessimisticForceInc = "[Lock PesFI, ";
    
    protected static final String TxtCommit                  = "Commit] ";
    protected static final String TxtRollback                = "Rollback] ";
    
    protected static final String TxtThread1                 = "T1: ";
    protected static final String TxtThread2                 = "T2: ";
    
    protected enum CommitAction {
        Commit, Rollback
    };

    protected enum RollbackAction {
        Rolledback, NoRolledback
    };

    protected enum ThreadToRunFirst {
        RunThread1, RunThread2
    }
    
    protected enum ThreadToResumeFirst {
        ResumeThread1, ResumeThread2
    }
    
    protected enum MethodToCall {
        NoOp,
        Find, FindWaitLock, FindNoUpdate,
        Refresh,
        Lock, 
    }
    
    private String empTableName;
    private String taskTableName;
    private String storyTableName;

    private long waitInMsec = -1;
    
    protected void commonSetUp() {
        empTableName = getMapping(LockEmployee.class).getTable().getFullName();
        taskTableName = getMapping(LockTask.class).getTable().getFullName();
        storyTableName = getMapping(LockStory.class).getTable().getFullName();

        cleanupDB();

        LockEmployee e1, e2, e3;
        e1 = newTree(1);
        e2 = newTree(2);
        e3 = newTree(3);

        resetSQL();
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            em.persist(e1);
            em.persist(e2);
            em.persist(e3);
            em.getTransaction().commit();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        assertAllSQLInOrder(
            "INSERT INTO " + empTableName   + " .*",
            "INSERT INTO " + taskTableName  + " .*",
            "INSERT INTO " + storyTableName + " .*",
            "INSERT INTO " + empTableName   + " .*",
            "INSERT INTO " + taskTableName  + " .*",
            "INSERT INTO " + storyTableName + " .*",
            "INSERT INTO " + empTableName   + " .*",
            "INSERT INTO " + taskTableName  + " .*",
            "INSERT INTO " + storyTableName + " .*"
        );
        
        // dynamic runtime test to determine wait time.
        long speedCnt = -1;
        if (waitInMsec == -1) {
            speedCnt = platformSpeedTest();
            try {
                waitInMsec = 500000 / (speedCnt / 1000000);
            } catch (Throwable t) {
            }
        }
        if (waitInMsec <= 0) {
            waitInMsec = 30 * 1000; // default to 30sec
        }
        getLog().trace(
            "**** Speed Cont=" + speedCnt + ", waitTime(ms)=" + waitInMsec);
    }

    private long platformSpeedTest() {
        PlatformSpeedTestThread speedThread = new PlatformSpeedTestThread();
        speedThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logStack(e);
        }
        speedThread.interrupt();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            logStack(e);
        }
        return speedThread.getLoopCnt();
    }
    
    private void cleanupDB() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            em.createQuery("delete from " + empTableName).executeUpdate();
            em.createQuery("delete from " + taskTableName).executeUpdate();
            em.createQuery("delete from " + storyTableName).executeUpdate();

            em.getTransaction().commit();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Helper to create a tree of entities
     * 
     * @param id
     *            ID for the entities.
     * @return an unmanaged Employee instance with the appropriate relationships
     *         set.
     */
    private LockEmployee newTree(int id) {
        LockEmployee e = new LockEmployee();
        e.setId(id);

        LockTask t = new LockTask();
        t.setId(id);

        LockStory s = new LockStory();
        s.setId(id);

        Collection<LockTask> tasks = new ArrayList<LockTask>();
        tasks.add(t);

        Collection<LockStory> stories = new ArrayList<LockStory>();
        stories.add(s);

        e.setTasks(tasks);
        t.setEmployee(e);

        t.setStories(stories);
        s.setTask(t);

        return e;
    }
    
    protected Log getLog() {
        return emf.getConfiguration().getLog("Tests");
    }

    protected Log getDumpStackLog() {
        return emf.getConfiguration().getLog("DumpStack");
    }

    protected void logStack(Throwable t) {
        StringWriter str = new StringWriter();
        PrintWriter print = new PrintWriter(str);
        t.printStackTrace(print);
        getDumpStackLog().trace(str.toString());
    }
    
    // Compute the timeout value used before the resume step for
    // each thread
    private long computeSleepFromTimeout( Integer timeout ) {
        long value = 500;   // default to 500 msec
        if( timeout != null) {
            value = timeout * 2;
        }
        return value;
    }
    
    public void testFindNormal() {
        getLog().info("---> testFindNormal()");
        resetSQL();
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            LockEmployee e = em.find(LockEmployee.class, 1);
            getLog().trace("Found: " + e);
            assertNotNull(e);
            assertEquals("Expected=1, testing=" + e.getId(), 1, e.getId());
            commonSelect.validate();
            commonForUpdate.validate();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public void testFindLockNone() {
        getLog().info("---> testFindLockNone()");
        commonTestSequence(
            LockModeType.NONE,
            1,
            0,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
            );
    }
    
    protected void commonTestSequence(LockModeType mode,
        int id,                 // LockEmployee ids used, id & id+1
        int commitVersionDiff,  // version field difference on commit
        SQLAssertions commitSimilarSQLOrder, SQLAssertions commitNotAnySQL,
        SQLAssertions rollbackSimilarSQLOrder, SQLAssertions rollbackNotAnySQL){
        Log log = getLog();
        log.trace("commonTestSequence(lockmode=" + mode + ",id=" + id
            + ",verDiff=" + commitVersionDiff + "...)");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            resetSQL();
            try {
                em.find(LockEmployee.class, id, mode);
                if (mode != LockModeType.NONE)
                    fail("Need transaction begin to apply lock mode " + mode);
            } catch (TransactionRequiredException trex) {
                if (mode == LockModeType.NONE)
                    fail("Do not need transaction begin to for lock " + mode);
            } catch (Exception ex) {
                logStack(ex);
                fail("Need transaction begin to apply lock mode " + mode
                    + ". Exception encountered:" + ex.getMessage());
            }

            int lastVersion = -1;
            resetSQL();
            em.clear();
            em.getTransaction().begin();
            LockEmployee e1 = em.find(LockEmployee.class, id, mode);
            assertNotNull(e1);
            assertEquals("Expected=" + id + ", testing=" + e1.getId(), id, e1
                .getId());
            lastVersion = e1.getVersion();
            em.getTransaction().commit();
            if (commitSimilarSQLOrder != null)
                commitSimilarSQLOrder.validate();
            if (commitNotAnySQL != null)
                commitNotAnySQL.validate();

            resetSQL();
            em.clear();
            LockEmployee e2 = em.find(LockEmployee.class, id);
            assertNotNull(e2);
            assertEquals("Expected=" + id + ", testing=" + e1.getId(), id, e1
                .getId());
            assertEquals("Expected=" + (lastVersion + commitVersionDiff)
                + ", testing=" + e2.getVersion(), lastVersion
                + commitVersionDiff, e2.getVersion());

            lastVersion = -1;
            ++id;
            resetSQL();
            em.clear();
            em.getTransaction().begin();
            e1 = em.find(LockEmployee.class, id, mode);
            assertNotNull(e1);
            assertEquals("Expected=" + id + ", testing=" + e1.getId(), id, e1
                .getId());
            lastVersion = e1.getVersion();
            em.getTransaction().rollback();
            if (rollbackSimilarSQLOrder != null)
                rollbackSimilarSQLOrder.validate();
            if (rollbackNotAnySQL != null)
                rollbackNotAnySQL.validate();

            resetSQL();
            em.clear();
            e2 = em.find(LockEmployee.class, id);
            assertNotNull(e2);
            assertEquals("Expected=" + id + ", testing=" + e1.getId(), id, e1
                .getId());
            assertEquals("Expected=" + lastVersion + ", testing="
                + e2.getVersion(), lastVersion, e2.getVersion());
        } catch (Exception ex) {
            logStack(ex);
            Throwable rootCause = ex.getCause();
            String failStr = "Unexpected exception:" + ex.getClass().getName()
                + ":" + ex;
            if (rootCause != null) {
                failStr += "\n        -- Cause --> "
                    + rootCause.getClass().getName() + ":" + rootCause;
            }
            fail(failStr);
        } finally {
            if (em != null && em.isOpen()) {
                if (em.getTransaction().isActive()) {
                    if (em.getTransaction().getRollbackOnly()) {
                        log.trace("finally: rolledback");
                        em.getTransaction().rollback();
                        log.trace("finally: rolledback completed");
                    } else {
                        log.trace("finally: commit");
                        em.getTransaction().commit();
                        log.trace("finally: commit completed");
                    }
                }
                em.close();
            }
        }
    }
    
    private void notifyParent() {
        getLog().trace("notifyParent:");
        synchronized(this) {
            notify();
        }
    }
    
    public void concurrentLockingTest(
        int id, 
        int expectedVersionIncrement,
        String expectedFirstName,
        ThreadToRunFirst thread2Run,
        ThreadToResumeFirst thread2Resume,
        
        LockModeType t1LockMode,
        String t1ChangeValue, 
        CommitAction t1Commit,
        RollbackAction t1ExpectedSystemRolledback, 
        Class<?>[] t1ExpectedExceptions,
        MethodToCall t1Method,
        Integer t1Timeout,  // in msec
        
        LockModeType t2LockMode, 
        String t2ChangeValue,
        CommitAction t2Commit, 
        RollbackAction t2ExpectedSystemRolledback,
        Class<?>[] t2ExpectedExceptions, 
        MethodToCall t2Method,
        Integer t2Timeout  // in msec
        ) {
        
        Log log = getLog();
        log.trace("================= Concurrent Lock Test: =================");
        log.trace("   id=" + id + ", versionInc=" + expectedVersionIncrement
            + ", expectedFirstName='" + expectedFirstName + "', threadRunFirst="
            + thread2Run + ", thread2Resume=" + thread2Resume);
        log.trace("   t1:lock=" + t1LockMode + ", change='" + t1ChangeValue
            + "', " + (t1Commit == CommitAction.Commit ? "commit" : "rollback")
            + ", expectedSysRollback=" + t1ExpectedSystemRolledback
            + ", expectedEx=" + Arrays.toString(t1ExpectedExceptions)
            + ", method=" + t1Method + ", timeout=" + t1Timeout);
        log.trace("   t2:lock=" + t2LockMode + ", change='" + t2ChangeValue
            + "', " + (t2Commit == CommitAction.Commit ? "commit" : "rollback")
            + ", expectedSysRollback=" + t2ExpectedSystemRolledback
            + ", expectedEx=" + Arrays.toString(t2ExpectedExceptions)
            + ", method=" + t2Method + ", timeout=" + t2Timeout);
        long endTime = System.currentTimeMillis() + waitInMsec;
        
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            LockEmployee ei = em.find(LockEmployee.class, id);
            assertNotNull(ei);
            ei.setFirstName(Default_FirstName);
            em.getTransaction().commit();
        } catch (Exception ex) {
            logStack(ex);
            Throwable rootCause = ex.getCause();
            String failStr = "Unable to pre-initialize FirstName to known "
                + "value:" + ex.getClass().getName() + ":" + ex;
            if (rootCause != null) {
                failStr += "\n        -- Cause --> "
                    + rootCause.getClass().getName() + ":" + rootCause;
            }
            fail(failStr);
        } finally {
            if (em != null && em.isOpen()) {
                if (em.getTransaction().isActive()) {
                    if (em.getTransaction().getRollbackOnly()) {
                        log.trace("finally: rolledback");
                        em.getTransaction().rollback();
                        log.trace("finally: rolledback completed");
                    } else {
                        log.trace("finally: commit");
                        em.getTransaction().commit();
                        log.trace("finally: commit completed");
                    }
                }
                em.close();
            }
        }
        em = null;
        try {
            em = emf.createEntityManager();
            LockEmployee e0 = em.find(LockEmployee.class, id);
            assertNotNull(e0);
            int lastVersion = e0.getVersion();
            log.trace("Start version=" + lastVersion);
            em.clear();

            LockTestThread t1 = new LockTestThread(t1LockMode, id,
                t1ChangeValue, t1Commit, t1Method, t1Timeout);
            LockTestThread t2 = new LockTestThread(t2LockMode, id,
                t2ChangeValue, t2Commit, t2Method, t2Timeout);

            if (thread2Run == ThreadToRunFirst.RunThread1) {
                t1.start();
            } else {
                t2.start();
            }
            log.trace("wait on thread 1");
            synchronized (this){
                wait();
            }
            if (thread2Run == ThreadToRunFirst.RunThread2) {
                t1.start();
            } else {
                t2.start();
            }
            // continue the thread after each thread had a chance to fetch the
            // row
            if (thread2Resume == ThreadToResumeFirst.ResumeThread1) {
                Thread.sleep(computeSleepFromTimeout(t1Timeout));
                t1.resumeEmAction();
            } else {
                Thread.sleep(computeSleepFromTimeout(t2Timeout));
                t2.resumeEmAction();
            }
            if (thread2Resume == ThreadToResumeFirst.ResumeThread2) {
                Thread.sleep(computeSleepFromTimeout(t1Timeout));
                t1.resumeEmAction();
            } else {
                Thread.sleep(computeSleepFromTimeout(t2Timeout));
                t2.resumeEmAction();
            }

            log.trace("started children threads");

            // wait for threads to die or timeout
            log.trace("checking if thread is alive");
            while ((t1.isAlive() || t2.isAlive())
                && System.currentTimeMillis() < endTime) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                }
                log.trace("waiting children thread for completion ("
                    + (endTime - System.currentTimeMillis()) + " ms left)");
            }

            testThread(t1, t1ExpectedExceptions,
                t1ExpectedSystemRolledback, log);
            testThread(t2, t2ExpectedExceptions,
                t2ExpectedSystemRolledback, log);

            log.trace("verify lock and update are correct.");
            LockEmployee e = em.find(LockEmployee.class, id);
            log.trace("End version=" + e.getVersion());
            assertNotNull(e);
            assertEquals("Expected=" + id + ", testing=" + e.getId(), id, e
                .getId());
            assertEquals("Expected=" + (lastVersion + expectedVersionIncrement)
                + ", testing=" + e.getVersion(), lastVersion
                + expectedVersionIncrement, e.getVersion());
            assertEquals("Expected=" + expectedFirstName + ", testing="
                + e.getFirstName(), expectedFirstName, e.getFirstName());
        } catch (Exception ex) {
            logStack(ex);
            Throwable rootCause = ex.getCause();
            String failStr = "Unexpected exception:" + ex.getClass().getName()
                + ":" + ex;
            if (rootCause != null) {
                failStr += "\n        -- Cause --> "
                    + rootCause.getClass().getName() + ":" + rootCause;
            }
            fail(failStr);
        } finally {
            if (em != null && em.isOpen()) {
                if (em.getTransaction().isActive()) {
                    if (em.getTransaction().getRollbackOnly()) {
                        log.trace("finally: rolledback");
                        em.getTransaction().rollback();
                        log.trace("finally: rolledback completed");
                    } else {
                        log.trace("finally: commit");
                        em.getTransaction().commit();
                        log.trace("finally: commit completed");
                    }
                }
                em.close();
            }
        }
    }

    private boolean matchExpectedException(Class<?> expected,
        Exception tested) {
        assertNotNull(expected);
        Class<?> testExClass = null;
        boolean exMatched = true;
        if (tested != null) {
            testExClass = tested.getClass();
            exMatched = expected.isAssignableFrom(testExClass);
            if (!exMatched) {
                Throwable testEx = tested.getCause();
                if (testEx != null) {
                    testExClass = testEx.getClass();
                    exMatched = expected.isAssignableFrom(testExClass);
                }
            }
        } else {
            exMatched = false;
        }
        return exMatched;
    }
    
    private void testThread(LockTestThread t, Class<?>[] expectedExceptions,
        RollbackAction expectedSystemRolledback, Log log) {
        boolean alive = t.isAlive();
        if (alive) {
            log.trace(t.getName() + " is still alive");
            try {
                t.interrupt();
                t.join();
            } catch (Exception e) {
                logStack(e);
            }
        }
        Class<?> testExClass = null;
        boolean exMatched = false;
        if (expectedExceptions != null) {
            for( Class<?> expectedException : expectedExceptions) {
                if( matchExpectedException(expectedException, t.exception) ) {
                    exMatched = true;
                    break;
                }
            }
        } else {
            if (t.exception == null) {
                exMatched = true;
            } else {
                testExClass = t.exception.getClass();
            }
        }
        assertTrue(
            "Exception test: Expecting=" + Arrays.toString(expectedExceptions)
                + ", Testing=" + testExClass, exMatched);
        assertEquals(expectedSystemRolledback == RollbackAction.Rolledback,
            t.systemRolledback);
    }

    private class LockTestThread extends Thread {

        private LockModeType lockMode;
        private int id;
        private String changeValue;
        private boolean commit;
        private MethodToCall beforeWaitMethod;
        private Integer timeout;
        
        public Exception exception = null;
        public boolean systemRolledback = false;

        public LockTestThread(LockModeType lockMode, int id,
            String changeValue, CommitAction commit,
            MethodToCall beforeWaitMethod, Integer timeout) {
            this.lockMode = lockMode;
            this.id = id;
            this.changeValue = changeValue;
            this.commit = commit == CommitAction.Commit;
            this.beforeWaitMethod = beforeWaitMethod;
            this.timeout = timeout;
        }

        public synchronized void resumeEmAction () {
            notify();
        }
        
        public synchronized void run() {
            Log log = getLog();
            log.trace("enter run()");
            EntityManager em = null;
            long startTimeStamp = System.currentTimeMillis();
            try {
                em = emf.createEntityManager();
                em.getTransaction().begin();
                try {
                    log.trace(beforeWaitMethod + ": id=" + id + ", lock="
                        + lockMode + ", timeout=" + timeout);
                    LockEmployee e1 = null;
                    switch (beforeWaitMethod) {
                    case NoOp:
                        break;
                    case Find:
                    case FindNoUpdate:
                        if (timeout == null) {
                            e1 = em.find(LockEmployee.class, id, lockMode);
                        } else {
                            Map<String, Object> props = 
                                new HashMap<String, Object>(1);
                            props
                                .put("javax.persistence.lock.timeout", timeout);
                            e1 = em.find(LockEmployee.class, id, lockMode,
                                props);
                        }
                        break;
                    case FindWaitLock:
                        e1 = em.find(LockEmployee.class, id);
                        break;
                    case Refresh:
                        e1 = em.find(LockEmployee.class, id);
                        if (timeout == null) {
                            em.refresh(e1, lockMode);
                        } else {
                            Map<String, Object> props =
                                new HashMap<String, Object>(1);
                            props
                                .put("javax.persistence.lock.timeout", timeout);
                            em.refresh(e1, lockMode, props);
                        }
                        break;
                    case Lock:
                        e1 = em.find(LockEmployee.class, id);
                        if (timeout == null) {
                            em.lock(e1, lockMode);
                        } else {
                            Map<String, Object> props = 
                                new HashMap<String, Object>(1);
                            props
                                .put("javax.persistence.lock.timeout", timeout);
                            em.lock(e1, lockMode, props);
                        }
                        break;
                    }

                    log.trace(beforeWaitMethod + ": duration 1="
                        + (System.currentTimeMillis() - startTimeStamp));
                    log.trace(beforeWaitMethod + ": returns=" + e1);
                    notifyParent();
                    log.trace("childWait()");
                    wait();
                    switch (beforeWaitMethod) {
                    case NoOp:
                        log.trace(beforeWaitMethod
                            + ": No operation performed.");
                        break;
                    case FindWaitLock:
                        log
                            .trace(beforeWaitMethod + ": Lock(" + lockMode
                                + ")");
                        em.lock(e1, lockMode);
                        break;
                    case FindNoUpdate:
                        log.trace(beforeWaitMethod
                            + ": No update to firstName field per request.");
                        break;
                    default:
                        log.trace(beforeWaitMethod
                            + ": update first name from '" + e1.getFirstName()
                            + "' to '" + changeValue + "'");
                        e1.setFirstName(changeValue);
                        log.trace("update first name completed");
                    }
                } finally {
                    log.trace(beforeWaitMethod + ": duration 2="
                        + (System.currentTimeMillis() - startTimeStamp));
                    if (em != null) {
                        systemRolledback = em.getTransaction()
                            .getRollbackOnly();
                        if (systemRolledback) {
                            log.trace("rolledback by system");
                            em.getTransaction().rollback();
                            log.trace("rolledback by system completed");
                        } else {
                            if (commit) {
                                log.trace("commit update");
                                em.getTransaction().commit();
                                log.trace("commit completed");
                            } else {
                                log.trace("rollback update");
                                em.getTransaction().rollback();
                                log.trace("rollback completed");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                exception = e;
                log.trace("Caught exception:" + e);
                Class<?> exClass = e.getClass();
                if (javax.persistence.PersistenceException.class
                    .isAssignableFrom(exClass)
                    || javax.persistence.RollbackException.class
                        .isAssignableFrom(exClass)
                    || javax.persistence.OptimisticLockException.class
                        .isAssignableFrom(exClass)
                    || javax.persistence.PessimisticLockException.class
                        .isAssignableFrom(exClass)
                    || javax.persistence.EntityNotFoundException.class
                        .isAssignableFrom(exClass)) {
                    systemRolledback = true;
                }
                Throwable cause = e.getCause();
                if (cause != null)
                    log.trace("      root cause:" + cause);
                logStack(e);
            } finally {
                log.trace(beforeWaitMethod + ": duration 3="
                    + (System.currentTimeMillis() - startTimeStamp));
                if (em != null && em.isOpen()) {
                    if (em.getTransaction().isActive()) {
                        systemRolledback = em.getTransaction()
                            .getRollbackOnly();
                        if (systemRolledback) {
                            log.trace("finally: rolledback");
                            em.getTransaction().rollback();
                            log.trace("finally: rolledback completed");
                        } else {
                            log.trace("finally: commit");
                            em.getTransaction().commit();
                            log.trace("finally: commit completed");
                        }
                    }
                    em.close();
                }
            }
        }
    }
    
    class PlatformSpeedTestThread extends Thread {
        long loopCnt = 0;

        public long getLoopCnt() {
            return loopCnt;
        }
        
        public synchronized void run() {
            while (true) {
                ++loopCnt;
                if (this.isInterrupted())
                    break;
            }
        }
    }
}

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

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

/**
 * Test EntityManager find/namedQuery deadlock exceptions.
 */
public class TestCacheRefresh extends SequencedActionsTest {
    
    public void setUp() {
//        setSupportedDatabases(
//                org.apache.openjpa.jdbc.sql.DB2Dictionary.class,
//                org.apache.openjpa.jdbc.sql.DerbyDictionary.class
//);
//        if (isTestsDisabled()) {
//            return;
//        }

        setUp(LockEmployee.class
            , "javax.persistence.sharedCache.mode", "all"
            , "openjpa.LockManager", "mixed"
            , "openjpa.DataCache", "true"
            , "openjpa.RemoteCommitProvider", "sjvm"
//            , "openjpa.jdbc.TransactionIsolation", "read-committed"
        );
        commonSetUp();
        EntityManager em = emf.createEntityManager();
//        dbType = getDBType(em);
    }

    /* ======== Find dead lock exception test ============*/
    public void testCacheRefresh() {
//        String[] parameters = new String[] { "Thread 1: lock= " + t1Lock + ", expectedEx= "
//                + Arrays.toString(t1Exceptions) };
            
        Object[][] threadMain = {
            {Act.CreateEm},
            {Act.StartTx},
            
            {Act.Clear},
            {Act.ClearCache},
            
            {Act.Find, 1},
            {Act.TestInCache, 1, true},
            
            {Act.NewThread, 1 },
            {Act.StartThread, 1 },

            {Act.Wait, 0},
            {Act.Refresh},
            {Act.TestInCache, 1, true},
            {Act.TestEmployee, 1, "Modified First Name"},
            {Act.CommitTx},
            
            {Act.StartTx},
            {Act.Clear},

            {Act.Find, 1},                        
            {Act.TestInCache, 1, true},
            {Act.TestEmployee, 1, "Modified First Name"},// "Def FirstName"}, //"Modified First Name"},
            
            {Act.RollbackTx},
            {Act.Sleep, 500},
            {Act.WaitAllChildren},
        };
        Object[][] thread1 = {
            {Act.CreateEm},
            {Act.StartTx},
            {Act.FindWithLock, 1, LockModeType.PESSIMISTIC_FORCE_INCREMENT},            
            {Act.Notify, 0},
            
            {Act.UpdateEmployee, 1, "Modified First Name"},
            {Act.CommitTx},
//            {Act.Sleep, 3 },
        };
        launchActionSequence("TestCacheRefresh", null, threadMain, thread1);
    }
}

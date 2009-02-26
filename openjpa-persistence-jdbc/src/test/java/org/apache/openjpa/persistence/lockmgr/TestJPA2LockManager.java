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

import javax.persistence.LockModeType;

import org.apache.openjpa.persistence.test.AllowFailure;

/**
 * Test JPA 2.0 LockMode type behaviors with "jpa2" lock manager.
 */
public class TestJPA2LockManager extends LockManagerTestBase {
    public void setUp() {
        setUp(LockEmployee.class, LockTask.class, LockStory.class
//            , "openjpa.jdbc.FinderCache", "false"
            );
        commonSetUp();        
    }
    
    public void testFindLockRead() {
        getLog().info("---> testFindLockRead()");
        commonTestSequence(
            LockModeType.READ,
            2,
            0,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
        );
    }
    
    public void testFindLockWrite() {
        getLog().info("---> testFindLockWrite()");
        commonTestSequence(
            LockModeType.WRITE,
            1,
            1,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
        );
    }
    
    public void testFindLockOptimistic() {
        getLog().info("---> testFindLockOptimistic()");
        commonTestSequence(
            LockModeType.OPTIMISTIC,
            2,
            0,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
        );
    }
    
    public void testFindLockOptimisticForceIncrement() {
        getLog().info("---> testFindLockOptimisticForceIncrement()");
        commonTestSequence(
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            1,
            1,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
        );
    }
    
    public void testFindLockPessimisticRead() {
        getLog().info("---> testFindLockPessimisticRead()");
        commonTestSequence(
// TODO:            LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,   
            2,
            0,
            commonSelectForUpdate,
            null,
            commonSelectForUpdate,
            null
        );
    }
    
    public void testFindLockPessimisticWrite() {
        getLog().info("---> testFindLockPessimisticWrite()");
        commonTestSequence(
// TODO:           LockModeType.PESSIMISTIC_WRITE,   
            LockModeType.PESSIMISTIC,   
            1,
            0,
            commonSelectForUpdate,
            null,
            commonSelectForUpdate,
            null
        );
    }
    
    public void testFindLockPessimisticForceIncrement() {
        getLog().info("---> testFindLockPessimisticForceIncrement()");
        commonTestSequence(
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            2,
            1,
            commonSelectForUpdate,
            null,
            commonSelectForUpdate,
            null
        );
    }
    
    public void testConcurrentThread1ReadTest() {
        getLog().info("---> testConcurrentThread1ReadTest()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Read           commit
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockRead + TxtCommit 
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.READ,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Read           rollback
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockRead + TxtRollback
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.READ,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );

        //=======================================================
        // Thread 1: Optimistic     commit
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockOptimistic + TxtCommit
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic     rollback
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockOptimistic + TxtRollback
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic                 commit
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockOptimistic + TxtCommit
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
           LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic                 rollback
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockOptimistic + TxtRollback
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic             commit
        // Thread 2: Pessimistic_Read       commit
        baseFirstName = 1 + TxtLockOptimistic + TxtCommit
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic             rollback
        // Thread 2: Pessimistic_Read      commit
        baseFirstName = 1 + TxtLockOptimistic + TxtRollback
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic             commit
        // Thread 2: Pessimistic_Write      commit
        baseFirstName = 1 + TxtLockOptimistic + TxtCommit
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic             rollback
        // Thread 2: Pessimistic_Write      commit
        baseFirstName = 1 + TxtLockOptimistic + TxtRollback
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic                     commit
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockOptimistic + TxtCommit
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic                     rollback
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockOptimistic + TxtRollback
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
    }
        
    public void testConcurrentThread1WriteTest() {
        getLog().info("---> testConcurrentThread1WriteTest()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Write          commit
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockWrite + TxtCommit
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.WRITE,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Write          rollback
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockWrite + TxtRollback
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.WRITE,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );

        //=======================================================
        // Thread 1: Optimistic_Force_Increment commit
        // Thread 2: Optimistic                 commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtCommit
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment rollback
        // Thread 2: Optimistic                 commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtRollback
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment commit
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtCommit
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment rollback
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtRollback
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment commit
        // Thread 2: Pessimistic_Read           commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment rollback
        // Thread 2: Pessimistic_Read           commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtRollback
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment commit
        // Thread 2: Pessimistic_Write          commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment rollback
        // Thread 2: Pessimistic_Write          commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtRollback
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment  commit
        // Thread 2: Pessimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Optimistic_Force_Increment  rollback
        // Thread 2: Pessimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockOptimisticForceInc + TxtRollback
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
    }
        
    // TODO:
    @AllowFailure(message="OPENJPA-924 is preventing RR behavior: pessimistic lock "
        + "blocked read on thread 2, once thread-1 commit, thread-2 returns "
        + "with pre-thread 1 committed data. hence causing an "
        + "OptimisticLockException. Disable FinderCache to workaround the " 
        + "problem.")
    public void testConcurrentThread1PessimisticReadTest() {
        getLog().info("---> testConcurrentThread1PessimisticReadTest()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:           LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read               rollback
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtRollback
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );

        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Read     commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
                      + 2 + TxtLockRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read               rollback
        // Thread 2: Read                           commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtRollback
                      + 2 + TxtLockRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read               rollback
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtRollback
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Pessimistic_Read       commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 2, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read               rollback
        // Thread 2: Pessimistic_Read      commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtRollback
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Pessimistic_Write      commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 2, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read               rollback
        // Thread 2: Pessimistic_Write      commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtRollback
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 2, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 
            t2FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Read               rollback
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockPessimisticRead + TxtRollback
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
// TODO:         LockModeType.PESSIMISTIC_READ,
            LockModeType.PESSIMISTIC,
            t1FirstName, 
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 
            t2FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
    }

    // TODO:
    @AllowFailure(message="OPENJPA-924 is preventing RR behavior: pessimistic lock "
        + "blocked read on thread 2, once thread-1 commit, thread-2 returns "
        + "with pre-thread 1 committed data. hence causing an "
        + "OptimisticLockException. Disable FinderCache to workaround the " 
        + "problem.")
    public void testConcurrentThread1PessimisticForceIncTest() {
        getLog().info("---> testConcurrentThread1PessimisticForceIncTest()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    rollback
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtRollback
                      + 2 + TxtLockOptimistic + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );

        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Read     commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    rollback
        // Thread 2: Read                           commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtRollback
                      + 2 + TxtLockRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
           LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    rollback
        // Thread 2: Optimistic_Force_Increment commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtRollback
                      + 2 + TxtLockOptimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Pessimistic_Read       commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 2, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    rollback
        // Thread 2: Pessimistic_Read      commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtRollback
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Pessimistic_Write      commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 2, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    rollback
        // Thread 2: Pessimistic_Write      commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtRollback
                      + 2 + TxtLockPessimisticWrite + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
// TODO:           LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 2, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 
            t1FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 
            t2FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    rollback
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtRollback
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t2FirstName, 
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 
            t1FirstName, 
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 
            t2FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
    }
    
    /**
     * Test specific version condition per "3.4.4.2 When lock(entity,
     * LockModeType.PESSIMISTIC) is invoked on a versioned entity that is
     * already in the persistence context, the provider must also perform
     * optimistic version checks when obtaining the lock. An
     * OptimisticLockException must be thrown if the version checks fail.
     * Depending on the implementation strategy used by the provider, it is
     * possible that this exception may not be thrown until flush is called or
     * commit time, whichever occurs first."
     */
    public void testPessimisticLockOnUpdatedVersion() {
        getLog().info("---> testPessimisticLockOnUpdatedVersion()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Optimistic     commit
        baseFirstName = 1 + TxtLockOptimistic + TxtCommit
                      + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.OPTIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
// TODO:           LockModeType.PESSIMISTIC_READ, 
            LockModeType.PESSIMISTIC, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
            MethodToCall.FindWaitLock,
            null
        );
    }
    
    /**
     * Test version is updated for PessimisticForceIncrement
     * lock.
     */
    public void testPessimisticForceVersion() {
        getLog().info("---> testPessimisticForceVersion()");
        String baseFirstName;
        String t1FirstName;
        
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Noop
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        concurrentLockingTest(
            1, 0, Default_FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.FindNoUpdate,
            null,
            
            LockModeType.NONE, 
            null,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.NoOp,
            null
        );
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Noop
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        concurrentLockingTest(
            1, 1, Default_FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.FindNoUpdate,
            null,
            
            LockModeType.NONE, 
            null,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.NoOp,
            null
        );
    }
    
    /**
     * Test Pessimistic lock timeout condition.
     */
    public void testPessimisticLockTimeOut() {
        getLog().info("---> testPessimisticLockTimeOut()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Pessimistic_Read    commit
        // Thread 2: Noop
        baseFirstName = 1 + TxtLockPessimisticRead + TxtCommit
        			  + 2 + TxtLockPessimisticRead + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 0, Default_FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread2,
            
            LockModeType.PESSIMISTIC,
            t1FirstName,
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.FindNoUpdate,
            null,
            
            LockModeType.PESSIMISTIC,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingAnyLockExClass,
            MethodToCall.FindNoUpdate,
            5000
        );
    }
}

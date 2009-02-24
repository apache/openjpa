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

/**
 * Test JPA 2.0 LockTypeMode semantics using OpenJPA optimistic
 * "version" lock manager.
 *
 * @author Albert Lee
 * @since 2.0.0
 */
public class TestOptimisticLockManager extends LockManagerTestBase {
    
    public void setUp() {
        setUp(LockEmployee.class, LockTask.class, LockStory.class
            , "openjpa.LockManager", "version"
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
        getLog().info("---> testFindLockWRITE()");
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
       getLog().info("---> testFindLockPessimisticShared()");
       commonTestSequence(
// TODO:            LockModeType.PESSIMISTIC_READ,   
            LockModeType.PESSIMISTIC,   
            2,
            1,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
        );
    }
    
    public void testFindLockPessimisticWrite() {
        getLog().info("---> testFindLockPessimisticWrite()");
        commonTestSequence(
// TODO:            LockModeType.PESSIMISTIC_WRITE,   
            LockModeType.PESSIMISTIC,   
            1,
            1,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
        );
    }
    
    public void testFindLockPessimisticForceIncrement() {
        getLog().info("---> testFindLockPessimisticForceIncrement()");
        commonTestSequence(
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            2,
            1,
            commonSelect,
            commonForUpdate,
            commonSelect,
            commonForUpdate        
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
            
            LockModeType.PESSIMISTIC, 
// TODO:            LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
// TODO:            LockModeType.PESSIMISTIC_READ, 
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
            
            LockModeType.PESSIMISTIC, 
// TODO:            LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
// TODO:            LockModeType.PESSIMISTIC_WRITE, 
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
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
            
            LockModeType.PESSIMISTIC, 
// TODO:            LockModeType.PESSIMISTIC_READ, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
// TODO:            LockModeType.PESSIMISTIC_READ, 
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
            
            LockModeType.PESSIMISTIC, 
// TODO:            LockModeType.PESSIMISTIC_WRITE, 
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
// TODO:            LockModeType.PESSIMISTIC_WRITE, 
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
            
            LockModeType.PESSIMISTIC_FORCE_INCREMENT,
            t2FirstName,
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
        
    public void testConcurrentThread1PessimisticTest() {
        getLog().info("---> testConcurrentThread1PessimisticTest()");
        String baseFirstName;
        String t1FirstName;
        String t2FirstName;
        
        //=======================================================
        // Thread 1: Pessimistic_Force_Increment    commit
        // Thread 2: Pessimistic_Force_Increment    commit
        baseFirstName = 1 + TxtLockPessimisticForceInc + TxtCommit
                      + 2 + TxtLockPessimisticForceInc + TxtCommit; 
        t1FirstName = TxtThread1 + baseFirstName; 
        t2FirstName = TxtThread2 + baseFirstName; 
        concurrentLockingTest(
            1, 1, t1FirstName,
            ThreadToRunFirst.RunThread1,
            ThreadToResumeFirst.ResumeThread1,
            
            LockModeType.PESSIMISTIC,
            t1FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
            t2FirstName, 
            CommitAction.Commit,
            RollbackAction.Rolledback,
            ExpectingOptimisticLockExClass, 
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
            
            LockModeType.PESSIMISTIC,
            t1FirstName, 
            CommitAction.Rollback,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null,
            
            LockModeType.PESSIMISTIC, 
            t2FirstName, 
            CommitAction.Commit,
            RollbackAction.NoRolledback,
            null, 
            MethodToCall.Find,
            null
        );
    }
}

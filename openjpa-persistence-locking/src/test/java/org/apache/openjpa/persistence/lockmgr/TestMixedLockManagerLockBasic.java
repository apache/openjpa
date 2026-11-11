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

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test JPA 2.0 em.lock(LockMode) basic behaviors with "mixed" lock manager.
 */
class TestMixedLockManagerLockBasic extends SequencedActionsTest {

    @Override
    protected String getPersistenceUnitName() {
        return "locking-test";
    }

    @BeforeEach
    public void setUp() {
        setUp(LockEmployee.class
            , "openjpa.LockManager", "mixed"
            );
        commonSetUp();
    }

    @Test
    void lockRead() {
        common("testLockRead",
            LockModeType.READ, 0, 1);
    }

    @Test
    void lockWrite() {
        common("testLockWrite",
            LockModeType.WRITE, 1, 1);
    }

    @Test
    void lockOptimistic() {
        common("testLockOptimistic",
            LockModeType.OPTIMISTIC, 0, 1);
    }

    @Test
    void lockOptimisticForceInc() {
        common("testLockOptimisticForceInc",
            LockModeType.OPTIMISTIC_FORCE_INCREMENT, 1, 1);
    }

    @Test
    void lockPessimisticRead() {
        common("testLockPessimisticRead",
            LockModeType.PESSIMISTIC_READ, 0, 1);
    }

    @Test
    void lockPessimisticWrite() {
        common("testLockPessimisticWrite",
            LockModeType.PESSIMISTIC_WRITE, 0, 1);
    }

    @Test
    void lockPessimisticForceInc() {
        common("testLockPessimisticForceInc",
            LockModeType.PESSIMISTIC_FORCE_INCREMENT, 1, 1);
    }

    void common(String testName, LockModeType lockMode,
        int commitVersionIncrement, int updateCommitVersionIncrement) {
        Object[][] threadMain = {
            // Find entity, lock, no update and commit.
            { Act.CreateEm },
            { Act.StartTx },
            { Act.Clear },
            { Act.Find, 1 },
            { Act.TestEmployee, 1, Default_FirstName},
            { Act.SaveVersion },
            { Act.Lock, 1, lockMode },
            { Act.CommitTx },
            { Act.Clear },
            { Act.Find, 1 },
            { Act.TestEmployee, 1, Default_FirstName,
                commitVersionIncrement },

            // Find entity, lock, update and commit.
            { Act.StartTx },
            { Act.Clear },
            { Act.Find, 1 },
            { Act.TestEmployee, 1, Default_FirstName},
            { Act.SaveVersion },
            { Act.Lock, 1, lockMode },
            { Act.UpdateEmployee, 1, lockMode.toString() },
            { Act.CommitTx },
            { Act.Clear },
            { Act.Find, 1 },
            { Act.TestEmployee, 1, lockMode.toString(),
                updateCommitVersionIncrement },

            // Find entity, lock, update but rollback.
            { Act.StartTx },
            { Act.Clear },
            { Act.Find, 1 },
            { Act.TestEmployee, 1, lockMode.toString()},
            { Act.SaveVersion },
            { Act.Lock, 1, lockMode },
            { Act.UpdateEmployee, 1, lockMode.toString() + " Again" },
            { Act.RollbackTx },
            { Act.Clear },
            { Act.Find, 1 },
            { Act.TestEmployee, 1, lockMode.toString(), 0 },
            { Act.CloseEm }
        };
        launchActionSequence(testName, "LockMode=" + lockMode, threadMain);
    }
}

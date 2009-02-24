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
package org.apache.openjpa.persistence;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.kernel.PessimisticLockManager;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.LockLevels;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.log.Log;

/**
 * Test JPA 2.0 LockTypeMode semantics using JPA 2.0 "jpa2"
 * lock manager.
 *
 * @author Albert Lee
 * @since 2.0.0
 */
public class JPA2LockManager extends PessimisticLockManager {

    /*
     * (non-Javadoc)
     * @see org.apache.openjpa.jdbc.kernel.PessimisticLockManager
     *  #selectForUpdate(org.apache.openjpa.jdbc.sql.Select,int)
     */
    public boolean selectForUpdate(Select sel, int lockLevel) {
        return (lockLevel >= JPA2LockLevels.LOCK_PESSIMISTIC_READ) 
            ? super.selectForUpdate(sel, lockLevel) : false;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.openjpa.jdbc.kernel.PessimisticLockManager#
     *  lockInternal(org.apache.openjpa.kernel.OpenJPAStateManager, int, int,
     *               java.lang.Object)
     */
    protected void lockInternal(OpenJPAStateManager sm, int level, int timeout,
        Object sdata) {
        if (level >= JPA2LockLevels.LOCK_PESSIMISTIC_FORCE_INCREMENT) {
            setVersionCheckOnReadLock(true);
            setVersionUpdateOnWriteLock(true);
            super.lockInternal(sm, level, timeout, sdata);
        } else if (level >= JPA2LockLevels.LOCK_PESSIMISTIC_READ) {
            setVersionCheckOnReadLock(true);
            setVersionUpdateOnWriteLock(false);
            super.lockInternal(sm, level, timeout, sdata);
        } else if (level >= JPA2LockLevels.LOCK_OPTIMISTIC) {
            setVersionCheckOnReadLock(true);
            setVersionUpdateOnWriteLock(true);
            optimisticLockInternal(sm, level, timeout, sdata);
        }
    }
}

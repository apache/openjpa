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

import javax.persistence.LockModeType;

import org.apache.openjpa.kernel.LockLevels;

/**
 * Defines lock levels as per JPA 2.0 specification.
 * 
 * Translates JPA-defined lock levels and OpenJPA internal lock levels.
 * 
 * @author Albert Lee
 * @since 2.0.0
 */
public class JPA2LockLevels {
    /**
     * Generic optimistic no lock level. Defined since JPA 1.0.
     * 
     */
    public static final int LOCK_NONE = LockLevels.LOCK_NONE;

    /**
     * Generic optimistic read lock level. Defined since JPA 1.0.
     * Equivalent to <code>LOCK_OPTIMISTIC</code> which is preferred.
     */
    public static final int LOCK_READ = LockLevels.LOCK_READ;
    
    /**
     * Generic optimistic read lock level. Defined since JPA 2.0.
     * Value of 10.
     * 
     */
    public static final int LOCK_OPTIMISTIC = LOCK_READ;

    /**
     * Generic optimistic write lock level. Value of 20.
     */
    public static final int LOCK_OPTIMISTIC_FORCE_INCREMENT = 
        LockLevels.LOCK_WRITE;

    /**
     * Generic pessimistic read lock level. Value of 30.
     */
    public static final int LOCK_PESSIMISTIC_READ = 30;

    /**
     * Generic pessimistic write lock level. Value of 40.
     */
    public static final int LOCK_PESSIMISTIC_WRITE = 40;

    /**
     * Generic pessimistic force increment level. Value of 50.
     */
    public static final int LOCK_PESSIMISTIC_FORCE_INCREMENT = 50;

    /**
     * Translates the javax.persistence enum value to our internal lock level.
     */
    public static int toLockLevel(LockModeType mode) {
        if (mode == null || mode == LockModeType.NONE)
            return LockLevels.LOCK_NONE;
        if (mode == LockModeType.READ || mode == LockModeType.OPTIMISTIC)
            return LockLevels.LOCK_READ;
        if (mode == LockModeType.WRITE
            || mode == LockModeType.OPTIMISTIC_FORCE_INCREMENT)
            return LockLevels.LOCK_WRITE;
        // TODO: if (mode == LockModeType.PESSIMISTIC_READ)
        // TODO: return LockLevels.LOCK_PESSIMISTIC_READ;
        if (mode == LockModeType.PESSIMISTIC)
            return LOCK_PESSIMISTIC_WRITE;
        if (mode == LockModeType.PESSIMISTIC_FORCE_INCREMENT)
            return LOCK_PESSIMISTIC_FORCE_INCREMENT;
        throw new ArgumentException(mode.toString(), null, null, true);
    }

    /**
     * Translate our internal lock level to a javax.persistence enum value.
     */
    public static LockModeType fromLockLevel(int level) {
        if (level < LOCK_OPTIMISTIC)
            return null;
        if (level < LOCK_OPTIMISTIC_FORCE_INCREMENT)
            return LockModeType.READ;
        if (level < LOCK_PESSIMISTIC_READ)
            return LockModeType.WRITE;
        if (level < LOCK_PESSIMISTIC_WRITE)
            return LockModeType.PESSIMISTIC;
        // TODO: return LockModeType.PESSIMISTIC_READ;
        if (level < LOCK_PESSIMISTIC_FORCE_INCREMENT)
            return LockModeType.PESSIMISTIC;
        // TODO: return LockModeType.PESSIMISTIC_WRITE;
        return LockModeType.PESSIMISTIC_FORCE_INCREMENT;
    }
}

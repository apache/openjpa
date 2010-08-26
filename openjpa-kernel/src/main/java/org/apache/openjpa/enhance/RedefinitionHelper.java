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
package org.apache.openjpa.enhance;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.util.ImplHelper;

/**
 * Helper methods for managed types that use method redefinition for field
 * tracking.
 *
 * @since 1.0.0
 */
public class RedefinitionHelper {

    /**
     * Call {@link StateManagerImpl#dirtyCheck} if the argument is a
     * {@link StateManagerImpl}.
     */
    public static void dirtyCheck(StateManager sm) {
        if (sm instanceof StateManagerImpl)
            ((StateManagerImpl) sm).dirtyCheck();
    }

    /**
     * Notify the state manager for <code>o</code> (if any) that a field
     * is about to be accessed.
     */
    public static void accessingField(Object o, int absoluteIndex) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.accessingField(absoluteIndex);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, boolean cur,
        boolean next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingBooleanField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, char cur, char next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingCharField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, byte cur, byte next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingByteField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, short cur, short next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingShortField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, int cur, int next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingIntField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, long cur, long next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingLongField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, float cur, float next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingFloatField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, double cur,
        double next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingDoubleField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, String cur,
        String next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingStringField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }

    /**
     * Setting state callback.
     */
    public static void settingField(Object o, int idx, Object cur,
        Object next) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
        if (pc == null)
            return;
        StateManager sm = pc.pcGetStateManager();
        if (sm != null)
            sm.settingObjectField(pc, idx, cur, next,
                OpenJPAStateManager.SET_USER);
    }
}

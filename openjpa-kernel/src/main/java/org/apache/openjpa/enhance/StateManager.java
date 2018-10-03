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

import java.io.IOException;
import java.io.ObjectOutput;

/**
 * Internal state manager for managed instances.
 */
public interface StateManager {
    // DO NOT ADD ADDITIONAL DEPENDENCIES TO THIS CLASS

    int SET_USER = 0;
    int SET_REMOTE = 1;
    int SET_ATTACH = 2;

    /**
     * Persistence context.
     */
    Object getGenericContext();

    /**
     * Return the persistence-capable primary key object by extracting the
     * identity value of the related instance stored in the given field from
     * the given object id.
     */
    Object getPCPrimaryKey(Object oid, int field);

    /**
     * Change state manager.
     */
    StateManager replaceStateManager(StateManager sm);

    /**
     * Returns the optimistic version for this instance.
     */
    Object getVersion();

    /**
     * Whether the instance has been modified in this transaction.
     */
    boolean isDirty();

    /**
     * Whether the instance is transactional.
     */
    boolean isTransactional();

    /**
     * Whether the instance is persistent.
     */
    boolean isPersistent();

    /**
     * Whether the instance is newly-persisted in this transaction.
     */
    boolean isNew();

    /**
     * Whether the instance is deleted in this transaction.
     */
    boolean isDeleted();

    /**
     * Whether the instance is detached (i.e. this manager is a detached
     * state manager)
     */
    boolean isDetached();

    /**
     * Make named field dirty.
     */
    void dirty(String field);

    /**
     * Return the object id, assigning it if necessary.
     */
    Object fetchObjectId();

    /**
     * Callback to prepare instance for serialization.
     *
     * @return true to null detached state after serialize
     */
    boolean serializing();

    /**
     * Write detached state object and detached state manager to the
     * given stream.
     *
     * @return true if managed fields also written to stream
     */
    boolean writeDetached(ObjectOutput out)
        throws IOException;

    /**
     * Proxy the given detached field after deserialization.
     */
    void proxyDetachedDeserialized(int idx);

    /**
     * Field access callback.
     */
    void accessingField(int idx);

    /**
     * Setting state callback.
     */
    void settingBooleanField(PersistenceCapable pc, int idx,
        boolean cur, boolean next, int set);

    /**
     * Setting state callback.
     */
    void settingCharField(PersistenceCapable pc, int idx, char cur,
        char next, int set);

    /**
     * Setting state callback.
     */
    void settingByteField(PersistenceCapable pc, int idx, byte cur,
        byte next, int set);

    /**
     * Setting state callback.
     */
    void settingShortField(PersistenceCapable pc, int idx, short cur,
        short next, int set);

    /**
     * Setting state callback.
     */
    void settingIntField(PersistenceCapable pc, int idx, int cur,
        int next, int set);

    /**
     * Setting state callback.
     */
    void settingLongField(PersistenceCapable pc, int idx, long cur,
        long next, int set);

    /**
     * Setting state callback.
     */
    void settingFloatField(PersistenceCapable pc, int idx, float cur,
        float next, int set);

    /**
     * Setting state callback.
     */
    void settingDoubleField(PersistenceCapable pc, int idx, double cur,
        double next, int set);

    /**
     * Setting state callback.
     */
    void settingStringField(PersistenceCapable pc, int idx, String cur,
        String next, int set);

    /**
     * Setting state callback.
     */
    void settingObjectField(PersistenceCapable pc, int idx, Object cur,
        Object next, int set);

    /**
     * Provide state callback.
     */
    void providedBooleanField(PersistenceCapable pc, int idx,
        boolean cur);

    /**
     * Provide state callback.
     */
    void providedCharField(PersistenceCapable pc, int idx, char cur);

    /**
     * Provide state callback.
     */
    void providedByteField(PersistenceCapable pc, int idx, byte cur);

    /**
     * Provide state callback.
     */
    void providedShortField(PersistenceCapable pc, int idx, short cur);

    /**
     * Provide state callback.
     */
    void providedIntField(PersistenceCapable pc, int idx, int cur);

    /**
     * Provide state callback.
     */
    void providedLongField(PersistenceCapable pc, int idx, long cur);

    /**
     * Provide state callback.
     */
    void providedFloatField(PersistenceCapable pc, int idx, float cur);

    /**
     * Provide state callback.
     */
    void providedDoubleField(PersistenceCapable pc, int idx,
        double cur);

    /**
     * Provide state callback.
     */
    void providedStringField(PersistenceCapable pc, int idx,
        String cur);

    /**
     * Provide state callback.
     */
    void providedObjectField(PersistenceCapable pc, int idx,
        Object cur);

    /**
     * Replace state callback.
     */
    boolean replaceBooleanField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    char replaceCharField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    byte replaceByteField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    short replaceShortField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    int replaceIntField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    long replaceLongField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    float replaceFloatField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    double replaceDoubleField(PersistenceCapable pc, int idx);

    /**
     * Replace state callback.
     */
    String replaceStringField(PersistenceCapable pc, int idx);

	/**
	 * Replace state callback.
	 */
    Object replaceObjectField (PersistenceCapable pc, int idx);
}

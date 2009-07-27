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
package org.apache.openjpa.writebehind;

import java.util.Collection;
import java.util.List;

import org.apache.openjpa.kernel.StateManagerImpl;

/**
 * The WriteBehindCache stores updates to entities before flushing to the
 * database. A {@link WriteBehindCallback} instance will be used to write the
 * changes to the database.
 * 
 */
public interface WriteBehindCache {
    /**
     * Obtain the number of entities in the cache.
     * 
     * @return number of entities in the cache.
     */
    public int getSize();

    /**
     * Answer whether the provided object is included in the WriteBehindCache
     * 
     * @param o
     *            Object which may be in the cache
     * @return True if the object is in the cache, otherwise false.
     */
    public boolean contains(Object o);

    /**
     * Returns a string name that can be used by end-user-visible code to
     * identify this cache.
     */
    public String getName();

    /**
     * Sets a string name to be used to identify this cache to end-user needs.
     */
    public void setName(String name);

    /**
     * Add the provided {@link StateManagerImpl}s to the cache. Mimics the
     * StoreManager.flush() method. If the StateManagers cannot be added to the
     * cache or if any exceptions occur they will be returned to the caller in a
     * collection.
     * 
     * @param sms
     *            StateManagerImpls to add.
     * @return A collection of exceptions if any occurred when adding the
     *         StateManager to the cache. If no exceptions occur the collection
     *         will be empty.
     */
    public List<Exception> add(Collection<StateManagerImpl> sms);

    /**
     * Obtain the StateManagers currently in the cache.
     * 
     * @return collection of state managers.
     */
    public Collection<StateManagerImpl> getStateManagers();

    /**
     * Initialize any resources associated with the given
     * {@link WriteBehindCacheManager}.
     * 
     */
    public void initialize(WriteBehindCacheManager manager);

    /**
     * Obtain a cache key for the provided {@link StateManagerImpl}.
     * 
     * @param sm
     *            A StateManager
     * @return A key that may be used to cache the StateManager.
     */
    public WriteBehindCacheKey getKey(StateManagerImpl sm);

    /**
     * Determine whether the cache is empty.
     * 
     * @return true if there are no entities in the cache, otherwise false.
     */
    public boolean isEmpty();

    /**
     * Remove all entities from the cache.
     */
    public void clear();
}

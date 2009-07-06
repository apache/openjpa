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

public interface WriteBehindCache {
    public int getSize();
    public boolean contains(Object o);
    
    /**
     * Returns a string name that can be used by end-user-visible
     * code to identify this cache.
     */
    public String getName();

    /**
     * Sets a string name to be used to identify this cache to end-user needs.
     */
    public void setName(String name);

    public List<Exception> add(Collection<StateManagerImpl> sms);
    
    public Collection<StateManagerImpl> getStateManagers();
    
    /**
     * Initialize any resources associated with the given
     * {@link WriteBehindCacheManager}.
     */
    public void initialize(WriteBehindCacheManager manager);
    
    public WriteBehindCacheKey getKey(StateManagerImpl sm);
    public boolean isEmpty();
    public void clear(); 
}

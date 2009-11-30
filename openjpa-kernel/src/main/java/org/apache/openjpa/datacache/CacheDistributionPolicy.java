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
package org.apache.openjpa.datacache;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A policy determines the name of the cache where a given entity state will be cached.
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 *
 */
public interface CacheDistributionPolicy {
    /**
     * Selects the name of the cache where the given managed proxy object state be cached.
     * 
     * @param sm the managed proxy object to be cached
     * @param context the context of invocation. No specific semantics is 
     * attributed currently. Can be null.
     *  
     * @return name of the cache or null if the managed instance need not be cached.
     */
    String selectCache(OpenJPAStateManager sm, Object context);
    
    /**
     * A default implementation that selects the cache by the type of the given
     * managed instance.
     * 
     * @see ClassMetaData#getDataCacheName()
     *
     */
    public static class Default implements CacheDistributionPolicy {
        public String selectCache(OpenJPAStateManager sm, Object context) {
            return sm.getMetaData().getDataCacheName();
        }
    }
}

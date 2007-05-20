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

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.conf.ObjectValue;

/**
 * Manages the system's data and query caches. You can
 * retrieve the data cache manager from the {@link OpenJPAConfiguration}.
 *
 * @author Abe White
 * @author Patrick Linskey
 */
public interface DataCacheManager {

    /**
     * Initialize the manager, supplying the cache configuration.
     */
    public void initialize(OpenJPAConfiguration conf, ObjectValue dataCache,
        ObjectValue queryCache);

    /**
     * Return the system-wide data cache, or null if caching is not enabled.
     */
    public DataCache getSystemDataCache();

    /**
     * Return the named data cache, or null if it does not exist.
     */
    public DataCache getDataCache(String name);

    /**
     * Return the named data cache. If the given name is null, the default
     * data cache is returned.
     *
     * @param create if true, the cache will be created if it does
     * not already exist
     */
    public DataCache getDataCache(String name, boolean create);

    /**
     * Return the system query cache, or null if not configured.
     */
    public QueryCache getSystemQueryCache();

    /**
     * Return the PCData generator if configured.
     */
    public DataCachePCDataGenerator getPCDataGenerator();

    /**
     * Return the runnable which schedules evictions.
     */
    public DataCacheScheduler getDataCacheScheduler();

    /**
     * Close all caches.
     */
    public void close();
}

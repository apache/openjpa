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

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.conf.ObjectValue;

/**
 * Manages the system's WriteBehind cache(s). You can retrieve the data cache
 * manager from the {@link OpenJPAConfiguration}.
 * 
 */
public interface WriteBehindCacheManager {
    /**
     * Get the default WriteBehind cache.
     * 
     * @return If WriteBehind mode is enabled the default WriteBehind cache will
     *         be returned. If WriteBehind is not enabled return null.
     */
    public WriteBehindCache getSystemWriteBehindCache();

    /**
     * Obtain a named WriteBehindCache.
     * 
     * @param name
     *            Name of the WriteBehindCache to obtain
     * @return If WriteBehind mode is enabled a WriteBehindCache for 'name' will
     *         be returned (creating a new instance if needed). Otherwise return
     *         null.
     */
    public WriteBehindCache getWriteBehindCache(String name);

    /**
     * Initialize the WriteBehindCacheManager
     * 
     * @param conf
     *            OpenJPAConfiguration in use
     * @param writeBehindCache
     *            The pluginvalue for WritBehindCache.
     */
    public void initialize(OpenJPAConfiguration conf, ObjectValue writeBehindCache);
}

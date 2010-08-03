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
package org.apache.openjpa.instrumentation;

import java.util.Date;

/**
 * Interface for providing instrumented data cache metrics and operations.
 */
public interface QueryCacheInstrument {

    /**
     * Returns number of total read requests that have been found in cache since 
     * last reset.
     */
    public long getHitCount();

    /**
     * Returns number of total read requests since last reset
     */
    public long getReadCount();

    /**
     * Returns number of total write requests since last reset.
     */
    public long getWriteCount();

    /**
     * Returns number of total read requests since start.
     */
    public long getTotalReadCount(); 

    /**
     * Returns number of total read requests that has been found since start.
     */
    public long getTotalHitCount();

    /**
     * Returns number of total write requests for the given class since start.
     */
    public long getTotalWriteCount();
        
    /**
     * Returns the config id for the configuration attached to this cache
     */
    public String getConfigId();
    
    /**
     * Returns the system unique id for the configuration attached to this
     * cache
     */
    public String getContextRef();
    
    /**
     * Resets cache statistics
     */
    public void reset();
    
    /**
     * Returns date since cache statistics collection were last reset.
     */
    public Date sinceDate();

    /**
     * Returns date cache statistics collection started.
     */
    public Date startDate();
}

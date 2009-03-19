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
package org.apache.openjpa.slice;

import java.util.Map;

import org.apache.openjpa.kernel.Broker;

/**
 * Extension to Broker to allow dynamically add/remove slices.
 * 
 * @author Pinaki Poddar
 *
 */
public interface DistributedBroker extends Broker {
    /**
     * Adds the given slice with the given properties. This newly added slice
     * will participate in the current and subsequent transaction.
     * 
     * @param name logical name of the to be added slice. Must be different from
     * any currently available slices.
     * @see DistributedConfiguration#getAvailableSliceNames()
     * 
     * @param properties key-value pair of configuration for the slice to be
     * added. The keys must have openjpa.slice.&lt;name&gt;.* as prefix.
     * 
     * @see DistributedConfiguration#addSlice(String, Map)
     */
    Slice addSlice(String name, Map properties);
}

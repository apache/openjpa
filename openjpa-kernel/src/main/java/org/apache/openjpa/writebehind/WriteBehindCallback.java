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

import org.apache.openjpa.kernel.Broker;

/**
 * The WriteBehindCallback is responsible for flushing changes to the database
 * when OpenJPA is used in a Write-Behind mode.
 * 
 */
public interface WriteBehindCallback extends Runnable {

    /**
     * Initialize the WriteBehindCallback. The callback will pull changes from
     * the provided WriteBehindCache flush them using the provided broker. The
     * WriteBehindCallback is responsible for closing the Broker.
     * 
     * @param broker
     *            A new broker instance that the writebehind callback will use
     *            to flush changes to the database.
     * @param cache
     *            A {@link WriteBehindCache} which contains the inflight
     *            changes.
     */
    public void initialize(Broker broker, WriteBehindCache cache);

    /**
     * Manually flush changes to the database.
     * 
     * @return A Collection of Exceptions which occurred during the flush.
     */
    public Collection<Exception> flush();

    /**
     * Close the WriteBehindCallback releasing resources to the JVM
     */
    public void close();
}

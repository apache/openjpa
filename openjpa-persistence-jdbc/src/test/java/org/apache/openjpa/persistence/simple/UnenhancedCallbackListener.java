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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

/**
 * Entity listener that takes CallbackTracker (not Object) as parameter.
 * Mirrors TCK's ListenerAA which takes CallbackStatusIF.
 * Tests that OpenJPA can find callback methods with interface parameter types.
 *
 * Also verifies that the actual entity instance (not ReflectingPersistenceCapable)
 * is passed to the listener.
 */
public class UnenhancedCallbackListener {

    @PrePersist
    public void prePersist(CallbackTracker entity) {
        entity.addCallback("prePersist");
    }

    @PostPersist
    public void postPersist(Object entity) {
        ((CallbackTracker) entity).addCallback("postPersist");
    }

    @PreRemove
    public void preRemove(CallbackTracker entity) {
        entity.addCallback("preRemove");
    }

    @PostRemove
    public void postRemove(Object entity) {
        ((CallbackTracker) entity).addCallback("postRemove");
    }
}

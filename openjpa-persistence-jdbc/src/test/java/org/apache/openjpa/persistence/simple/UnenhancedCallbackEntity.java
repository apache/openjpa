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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Entity for testing callback listeners with runtime enhancement.
 * Named "Unenhanced*" so the build-time enhancer skips it.
 * Implements a callback-tracking interface similar to TCK's CallbackStatusIF.
 */
@Entity
@Table(name = "UNENHANCED_CALLBACK")
@EntityListeners(UnenhancedCallbackListener.class)
public class UnenhancedCallbackEntity implements CallbackTracker {

    @Id
    private int id;

    private String name;

    @Transient
    private final List<String> callbackLog = new ArrayList<>();

    public UnenhancedCallbackEntity() {
    }

    public UnenhancedCallbackEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getCallbackLog() {
        return callbackLog;
    }

    @Override
    public void addCallback(String event) {
        callbackLog.add(event);
    }
}

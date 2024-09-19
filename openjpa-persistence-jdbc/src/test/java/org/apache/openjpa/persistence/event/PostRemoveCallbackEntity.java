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
package org.apache.openjpa.persistence.event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PostRemove;

/**
 * An entity for testing PostRemove callback.
 *
 * @see TestPostRemove
 *
 * @author Pinaki Poddar
 *
 */
@Entity
public class PostRemoveCallbackEntity {
    @Id
    @GeneratedValue
    private long id;

    private String name;

    transient long postRemoveTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    @PostRemove
    public void postRemove() {
        if (postRemoveTime != 0) {
            throw new RuntimeException(".postRemove has been called more than once");
        }
//        if (id == 0) {
//            throw new RuntimeException(" must have an identity value assigned on PostRemove callback");
//        }
        postRemoveTime = System.nanoTime();
    }

    public long getPostRemoveTime() {
        return postRemoveTime;
    }
}

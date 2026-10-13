/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc.query.bulk;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * An entity whose remove callbacks force a bulk delete onto the in-memory
 * path, see <code>JDBCStoreQuery.executeDelete()</code>. The callbacks record
 * their own order so a test can tell when they fire.
 */
@Entity
@Table(name = "BULK_CALLBACK")
public class BulkCallbackEntity {

    /** The callbacks that have fired, in order. */
    public static final List<String> CALLBACKS = new ArrayList<>();

    @Id
    private long id;

    private String name;

    @PreRemove
    public void preRemove() {
        CALLBACKS.add("preRemove");
    }

    @PostRemove
    public void postRemove() {
        CALLBACKS.add("postRemove");
    }

    @PreUpdate
    public void preUpdate() {
        CALLBACKS.add("preUpdate");
    }

    @PostUpdate
    public void postUpdate() {
        CALLBACKS.add("postUpdate");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

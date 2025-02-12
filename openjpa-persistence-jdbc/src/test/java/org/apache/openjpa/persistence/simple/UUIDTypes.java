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

import static jakarta.persistence.GenerationType.AUTO;
import static jakarta.persistence.GenerationType.UUID;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class UUIDTypes {

    @Id
    @GeneratedValue
    private long id;

    private UUID basicUuid;

    @GeneratedValue(strategy = UUID)
    private String stringUuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getBasicUuid() {
        return basicUuid;
    }

    public void setBasicUuid(UUID basicUuid) {
        this.basicUuid = basicUuid;
    }

    public String getStringUuid() {
        return stringUuid;
    }

    public void setStringUuid(String stringUuid) {
        this.stringUuid = stringUuid;
    }

    public UUID getAutoUuid() {
        return autoUuid;
    }

    public void setAutoUuid(UUID autoUuid) {
        this.autoUuid = autoUuid;
    }

    @GeneratedValue(strategy = AUTO)
    private UUID autoUuid;

}

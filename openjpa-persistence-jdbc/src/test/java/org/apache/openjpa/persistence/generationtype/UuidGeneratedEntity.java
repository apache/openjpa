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
package org.apache.openjpa.persistence.generationtype;

import static jakarta.persistence.GenerationType.UUID;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class UuidGeneratedEntity {
    
    @Id
    @GeneratedValue
    @Column(name = "id_")
    private UUID id;

    @GeneratedValue(strategy = UUID)
    @Column(name = "nativeuuid_")
    private UUID nativeUuid;

    @GeneratedValue(strategy = UUID)
    @Column(name = "stringuuid_")
    private String stringUUID;

    private UUID basicUuid;

    @ManyToOne
    private UuidGeneratedEntity parent;
     
    public UuidGeneratedEntity() {
        super();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNativeUuid() {
        return nativeUuid;
    }

    public void setNativeUuid(UUID nativeUuid) {
        this.nativeUuid = nativeUuid;
    }

    public String getStringUUID() {
        return stringUUID;
    }

    public void setStringUUID(String stringUUID) {
        this.stringUUID = stringUUID;
    }

    public UUID getBasicUuid() {
        return basicUuid;
    }

    public void setBasicUuid(UUID basicUuid) {
        this.basicUuid = basicUuid;
    }

    public UuidGeneratedEntity getParent() {
        return parent;
    }

    public void setParent(UuidGeneratedEntity parent) {
        this.parent = parent;
    }
    
}

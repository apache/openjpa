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
package org.apache.openjpa.persistence.convert;

import jakarta.persistence.Basic;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity embedding the same embeddable as {@link ConvertEmbedEntity} but
 * without declaring any converter override. Used to verify that the
 * per-embedding overrides of another entity do not leak into the shared
 * embeddable metadata.
 */
@Entity
@Table(name = "CONV_PLAIN_EMBED")
public class ConvertPlainEmbedEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    @Basic
    protected String name;

    @Embedded
    protected ConvertAddress address;

    @Embedded
    protected ConvertSelfAddress selfAddress;

    public ConvertPlainEmbedEntity() {
    }

    public ConvertPlainEmbedEntity(String id, String name,
            ConvertAddress addr) {
        this.id = id;
        this.name = name;
        this.address = addr;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConvertAddress getAddress() {
        return address;
    }

    public void setAddress(ConvertAddress address) {
        this.address = address;
    }

    public ConvertSelfAddress getSelfAddress() {
        return selfAddress;
    }

    public void setSelfAddress(ConvertSelfAddress selfAddress) {
        this.selfAddress = selfAddress;
    }

    @Override
    public String toString() {
        return "ConvertPlainEmbedEntity[id=" + id
            + ", name=" + name
            + ", address=" + address + "]";
    }
}

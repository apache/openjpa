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
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity with @Converts on an embedded field.
 * Mirrors TCK B entity.
 */
@Entity
@Table(name = "CONV_B_EMBED")
public class ConvertEmbedEntity implements java.io.Serializable {

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected Integer value;

    @Embedded
    @Converts(value = {
        @Convert(attributeName = "street",
            converter = DotConverter.class),
        @Convert(attributeName = "state",
            converter = NumberToStateConverter.class)
    })
    protected ConvertAddress address;

    public ConvertEmbedEntity() {
    }

    public ConvertEmbedEntity(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public ConvertEmbedEntity(String id, String name, int value,
            ConvertAddress addr) {
        this.id = id;
        this.name = name;
        this.value = value;
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

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public ConvertAddress getAddress() {
        return address;
    }

    public void setAddress(ConvertAddress address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "ConvertEmbedEntity[id=" + id
            + ", name=" + name
            + ", value=" + value
            + ", address=" + address + "]";
    }
}

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
package org.apache.openjpa.persistence.jdbc.annotations;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Entity that uses @AttributeOverrides on its own declared fields
 * (not inherited from a mapped superclass). This mirrors the TCK's
 * A2 entity which caused "Superclass field java.lang.Object.name"
 * errors when @AttributeOverride referenced the entity's own field.
 */
@Entity
@Table(name = "ATTR_OVR_ENT")
@AttributeOverrides({
    @AttributeOverride(name = "name", column = @Column(name = "ENAME"))
})
@Access(AccessType.FIELD)
public class AttrOverrideEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    protected String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ATTR_OVR_ADDR",
        joinColumns = @JoinColumn(name = "ENT_ID"))
    @OrderBy("zipCode DESC")
    protected List<AttrOverrideEmbed> addresses =
        new ArrayList<>();

    public AttrOverrideEntity() {
    }

    public AttrOverrideEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AttrOverrideEmbed> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AttrOverrideEmbed> addresses) {
        this.addresses = addresses;
    }
}

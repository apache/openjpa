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

import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

/**
 * Entity that uses @ElementCollection with Map having enum keys and
 * embeddable values, with @AttributeOverrides using unqualified names
 * (no "key." or "value." prefix). Mirrors TCK Department4 from
 * mapkeyenumerated package.
 */
@Entity
@Table(name = "MKE_DEPT")
@Access(AccessType.FIELD)
public class MapKeyEnumEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private int id;

    private String name;

    @ElementCollection(targetClass = MapValueEmbeddable.class)
    @CollectionTable(name = "MKE_EMP_ENUM",
        joinColumns = @JoinColumn(name = "FK_DEPT"))
    @AttributeOverrides({
        @AttributeOverride(name = "employeeId",
            column = @Column(name = "EMP_ID")),
        @AttributeOverride(name = "employeeName",
            column = @Column(name = "EMP_NAME"))
    })
    @MapKeyEnumerated(EnumType.STRING)
    private Map<MapKeyEnum, MapValueEmbeddable> lastNameEmployees;

    public MapKeyEnumEntity() {
    }

    public MapKeyEnumEntity(int id, String name) {
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

    public Map<MapKeyEnum, MapValueEmbeddable> getLastNameEmployees() {
        return lastNameEmployees;
    }

    public void setLastNameEmployees(
            Map<MapKeyEnum, MapValueEmbeddable> lastNameEmployees) {
        this.lastNameEmployees = lastNameEmployees;
    }
}

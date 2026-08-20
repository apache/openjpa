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

import java.util.Date;
import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.Table;
import jakarta.persistence.TemporalType;

/**
 * Entity that uses @ElementCollection with Map having Date keys and
 * embeddable values, with @AttributeOverrides using unqualified names
 * (no "key." or "value." prefix). Mirrors TCK Department4 from
 * mapkeytemporal package.
 */
@Entity
@Table(name = "MKT_DEPT")
@Access(AccessType.FIELD)
public class MapKeyTemporalEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private int id;

    private String name;

    @ElementCollection(targetClass = MapValueEmbeddable.class)
    @CollectionTable(name = "MKT_EMP_DATE",
        joinColumns = @JoinColumn(name = "FK_DEPT"))
    @AttributeOverrides({
        @AttributeOverride(name = "employeeId",
            column = @Column(name = "EMP_ID")),
        @AttributeOverride(name = "employeeName",
            column = @Column(name = "EMP_NAME"))
    })
    @MapKeyColumn(name = "THEDATE")
    @MapKeyTemporal(TemporalType.DATE)
    private Map<Date, MapValueEmbeddable> lastNameEmployees;

    public MapKeyTemporalEntity() {
    }

    public MapKeyTemporalEntity(int id, String name) {
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

    public Map<Date, MapValueEmbeddable> getLastNameEmployees() {
        return lastNameEmployees;
    }

    public void setLastNameEmployees(
            Map<Date, MapValueEmbeddable> lastNameEmployees) {
        this.lastNameEmployees = lastNameEmployees;
    }
}

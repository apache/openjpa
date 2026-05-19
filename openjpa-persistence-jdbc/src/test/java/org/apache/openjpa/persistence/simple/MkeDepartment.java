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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Department entity with @OneToMany(mappedBy) Map keyed by enum (STRING).
 * Mirrors TCK mapkeyenumerated Department.
 */
@Entity
@Table(name = "MKE_DEPT_A")
public class MkeDepartment implements java.io.Serializable {

    private int id;
    private String name;
    private Map<MapKeyEnum, MkeEmployee> lastNameEmployees;

    public MkeDepartment() {
    }

    public MkeDepartment(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    @Column(name = "ID")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "department")
    @MapKeyColumn(name = "OFFICE_ID")
    @MapKeyEnumerated(EnumType.STRING)
    public Map<MapKeyEnum, MkeEmployee> getLastNameEmployees() {
        return lastNameEmployees;
    }

    public void setLastNameEmployees(Map<MapKeyEnum, MkeEmployee> lastNameEmployees) {
        this.lastNameEmployees = lastNameEmployees;
    }
}

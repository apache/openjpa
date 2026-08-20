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
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Department entity with @OneToMany(mappedBy) Map keyed by enum (ORDINAL default).
 * Mirrors TCK mapkeyenumerated Department2.
 * Shares the same Employee table as MkeDepartment to test column type conflict.
 */
@Entity
@Table(name = "MKE_DEPT_B")
public class MkeDepartmentOrdinal implements java.io.Serializable {

    private int id;
    private String name;
    private Map<MapKeyEnum, MkeEmployeeOrdinal> lastNameEmployees;

    public MkeDepartmentOrdinal() {
    }

    public MkeDepartmentOrdinal(int id, String name) {
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
    @MapKeyEnumerated()
    public Map<MapKeyEnum, MkeEmployeeOrdinal> getLastNameEmployees() {
        return lastNameEmployees;
    }

    public void setLastNameEmployees(Map<MapKeyEnum, MkeEmployeeOrdinal> lastNameEmployees) {
        this.lastNameEmployees = lastNameEmployees;
    }
}

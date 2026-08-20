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
package org.apache.openjpa.persistence.mapkey;

import java.util.Date;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TemporalType;

/**
 * Mirrors TCK mapkeytemporal Department: property access,
 * @OneToMany(mappedBy) with @MapKeyTemporal(DATE).
 * Uses Unenhanced prefix to skip build-time enhancement.
 */
@Entity
@Table(name = "UMKT_DEPT")
public class UnenhancedMKTDept implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private Map<Date, UnenhancedMKTEmp> lastNameEmployees;

    public UnenhancedMKTDept() {
    }

    public UnenhancedMKTDept(int id, String name) {
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
    @MapKeyColumn(name = "THEDATE")
    @MapKeyTemporal(TemporalType.DATE)
    public Map<Date, UnenhancedMKTEmp> getLastNameEmployees() {
        return lastNameEmployees;
    }

    public void setLastNameEmployees(
            Map<Date, UnenhancedMKTEmp> lastNameEmployees) {
        this.lastNameEmployees = lastNameEmployees;
    }
}

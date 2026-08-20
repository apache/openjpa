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
import jakarta.persistence.Transient;

/**
 * Entity with @Id on field (field access) and @ElementCollection on getter
 * with @Transient on the map field. This mirrors the TCK Department4 pattern
 * which uses mixed access with @MapKeyTemporal.
 */
@Entity
@Table(name = "MKT_DEPT4")
public class MKTDepartment4 implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private int id;

    private String name;

    @Transient
    private Map<Date, MKTEmbeddedEmployee> lastNameEmployees;

    public MKTDepartment4() {
    }

    public MKTDepartment4(int id, String name) {
        this.id = id;
        this.name = name;
    }

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

    @ElementCollection(targetClass = MKTEmbeddedEmployee.class)
    @CollectionTable(name = "MKT_EMP_COL4", joinColumns = @JoinColumn(name = "FK_DEPT5"))
    @AttributeOverrides({
        @AttributeOverride(name = "employeeId", column = @Column(name = "ID")),
        @AttributeOverride(name = "employeeName", column = @Column(name = "LASTNAME"))
    })
    @MapKeyColumn(name = "THEDATE")
    @MapKeyTemporal(TemporalType.DATE)
    public Map<Date, MKTEmbeddedEmployee> getLastNameEmployees() {
        return lastNameEmployees;
    }

    public void setLastNameEmployees(Map<Date, MKTEmbeddedEmployee> lastNameEmployees) {
        this.lastNameEmployees = lastNameEmployees;
    }
}

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
 * Mirrors TCK mapkeytemporal Department4.
 * Same table as TckMktDept/TckMktDept2 (TCK_DEPT2).
 * Uses @ElementCollection with @CollectionTable on the same table as employees.
 */
@Entity
@Table(name = "TCK_DEPT2")
public class TckMktDept4 implements java.io.Serializable {
    private static final long serialVersionUID = 22L;

    @Id
    private int id;

    private String name;

    @Transient
    private Map<Date, TckMktEmbEmp> lastNameEmployees;

    public TckMktDept4() {}

    public TckMktDept4(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Column(name = "ID")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @Column(name = "NAME")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @ElementCollection(targetClass = TckMktEmbEmp.class)
    @CollectionTable(name = "TCK_EMP2", joinColumns = @JoinColumn(name = "FK_DEPT5"))
    @AttributeOverrides({
        @AttributeOverride(name = "employeeId", column = @Column(name = "ID")),
        @AttributeOverride(name = "employeeName", column = @Column(name = "LASTNAME"))
    })
    @MapKeyColumn(name = "THEDATE")
    @MapKeyTemporal(TemporalType.DATE)
    public Map<Date, TckMktEmbEmp> getLastNameEmployees() { return lastNameEmployees; }
    public void setLastNameEmployees(Map<Date, TckMktEmbEmp> m) { this.lastNameEmployees = m; }
}

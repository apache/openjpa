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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mirrors TCK mapkeytemporal Employee2.
 * Same table as TckMktEmp (TCK_EMP2), ManyToOne to TckMktDept2.
 */
@Entity
@Table(name = "TCK_EMP2")
public class TckMktEmp2 implements java.io.Serializable {

    private int id;
    private String lastName;
    private TckMktDept2 department;

    public TckMktEmp2() {}

    public TckMktEmp2(int id, String lastName) {
        this.id = id;
        this.lastName = lastName;
    }

    @Id
    @Column(name = "ID")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @Column(name = "LASTNAME")
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @ManyToOne
    @JoinColumn(name = "FK_DEPT5")
    public TckMktDept2 getDepartment() { return department; }
    public void setDepartment(TckMktDept2 department) { this.department = department; }
}

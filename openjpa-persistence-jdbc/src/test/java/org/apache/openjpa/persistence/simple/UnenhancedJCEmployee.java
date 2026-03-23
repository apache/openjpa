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
 * Employee entity with @JoinColumn(insertable=false, updatable=false)
 * for testing that flush tolerates unmanaged references on
 * non-insertable/non-updatable FK fields.
 * Mirrors TCK Employee2 from mapkey test.
 */
@Entity
@Table(name = "UNENH_JC_EMP")
public class UnenhancedJCEmployee implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String lastName;
    private UnenhancedJCDepartment department;

    public UnenhancedJCEmployee() {}

    public UnenhancedJCEmployee(int id, String lastName) {
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
    @JoinColumn(name = "FK_DEPT", insertable = false, updatable = false)
    public UnenhancedJCDepartment getDepartment() { return department; }
    public void setDepartment(UnenhancedJCDepartment department) {
        this.department = department;
    }
}

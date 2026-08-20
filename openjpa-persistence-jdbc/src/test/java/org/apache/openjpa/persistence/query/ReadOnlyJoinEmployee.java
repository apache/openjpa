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
package org.apache.openjpa.persistence.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Employee entity with a read-only JoinColumn (insertable=false, updatable=false).
 * Used to test that OpenJPA does not throw when a detached/unmanaged entity
 * is referenced through a non-cascading, read-only join column.
 */
@Entity
@Table(name = "ROJO_EMP")
public class ReadOnlyJoinEmployee {

    @Id
    private int id;

    private String lastName;

    @ManyToOne
    @JoinColumn(name = "FK_DEPT", insertable = false, updatable = false)
    private ReadOnlyJoinDept department;

    public ReadOnlyJoinEmployee() {
    }

    public ReadOnlyJoinEmployee(int id, String lastName) {
        this.id = id;
        this.lastName = lastName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public ReadOnlyJoinDept getDepartment() {
        return department;
    }

    public void setDepartment(ReadOnlyJoinDept department) {
        this.department = department;
    }
}

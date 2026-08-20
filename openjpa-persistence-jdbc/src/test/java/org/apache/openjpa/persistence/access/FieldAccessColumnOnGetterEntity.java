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
package org.apache.openjpa.persistence.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete entity extending a MappedSuperclass that uses field access
 * (determined by @Id on field) with @Column annotations on getter methods.
 * The entity itself uses @Access(PROPERTY) — matching the TCK pattern
 * where FullTimeEmployee has @Access(PROPERTY) extending Employee.
 */
@Entity
@Table(name = "FACOG_ENTITY")
@AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "ID")),
    @AttributeOverride(name = "firstName",
                       column = @Column(name = "FIRSTNAME")),
    @AttributeOverride(name = "lastName",
                       column = @Column(name = "LASTNAME")),
    @AttributeOverride(name = "hireDate",
                       column = @Column(name = "HIREDATE"))
})
@Access(AccessType.PROPERTY)
public class FieldAccessColumnOnGetterEntity
        extends FieldAccessColumnOnGetter {

    private String department;

    public FieldAccessColumnOnGetterEntity() {
    }

    public FieldAccessColumnOnGetterEntity(int id, String firstName,
            String lastName, java.sql.Date hireDate, String department) {
        super(id, firstName, lastName, hireDate);
        this.department = department;
    }

    @Column(name = "DEPT")
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

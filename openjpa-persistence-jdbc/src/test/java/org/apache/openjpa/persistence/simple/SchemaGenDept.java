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

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Entity for testing @OrderColumn schema generation.
 */
@Entity
@Table(name = "SCHEMAGENDEPT")
public class SchemaGenDept implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private int deptId;

    @OneToMany(mappedBy = "department")
    @OrderColumn(name = "THEORDERCOLUMN")
    private List<SchemaGenEmp> employees;

    public SchemaGenDept() {}

    public SchemaGenDept(int id) {
        this.deptId = id;
    }

    public int getDeptId() { return deptId; }
    public void setDeptId(int id) { this.deptId = id; }
    public List<SchemaGenEmp> getEmployees() { return employees; }
    public void setEmployees(List<SchemaGenEmp> employees) { this.employees = employees; }
}

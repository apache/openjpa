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

import java.sql.Date;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mirrors TCK annotations.mapkey.Employee2.
 * Same table as TckMkEmp, but FK is insertable=false, updatable=false.
 */
@Cacheable(false)
@Entity
@Table(name = "TCK_MK_EMP")
public class TckMkEmp2 implements java.io.Serializable {

    private int id;
    private String firstName;
    private String lastName;
    private Date hireDate;
    private float salary;
    private TckMkDept department;

    public TckMkEmp2() {}

    public TckMkEmp2(int id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Id
    @Column(name = "ID")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @Column(name = "FIRSTNAME", insertable = false, updatable = false)
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    @Column(name = "LASTNAME")
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @Column(name = "HIREDATE")
    public Date getHireDate() { return hireDate; }
    public void setHireDate(Date hireDate) { this.hireDate = hireDate; }

    @Column(name = "SALARY")
    public float getSalary() { return salary; }
    public void setSalary(float salary) { this.salary = salary; }

    @ManyToOne
    @JoinColumn(name = "FK_DEPT", insertable = false, updatable = false)
    public TckMkDept getDepartment() { return department; }
    public void setDepartment(TckMkDept department) { this.department = department; }
}

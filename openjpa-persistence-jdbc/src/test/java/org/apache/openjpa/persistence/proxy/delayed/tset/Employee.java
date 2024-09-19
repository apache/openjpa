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
package org.apache.openjpa.persistence.proxy.delayed.tset;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.proxy.delayed.IDepartment;
import org.apache.openjpa.persistence.proxy.delayed.IEmployee;

@Entity
@Table(name="DC_EMPLOYEE")
public class Employee implements IEmployee, Serializable, Comparable<Employee> {

    private static final long serialVersionUID = 1878272252981151246L;

    @Id
    @GeneratedValue
    private int id;

    private String empName;

    @ManyToOne(targetEntity=Department.class)
    @JoinColumn(name="DEPT_ID")
    private IDepartment dept;

    @Override
    public void setEmpName(String empName) {
        this.empName = empName;
    }

    @Override
    public String getEmpName() {
        return empName;
    }


    @Override
    public void setId(int id) {
        this.id = id;
    }


    @Override
    public int getId() {
        return id;
    }


    @Override
    public void setDept(IDepartment dept) {
        this.dept = dept;
    }


    @Override
    public IDepartment getDept() {
        return dept;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Employee) {
            Employee e = (Employee)obj;
            return e.getId() == getId() && e.getEmpName().equals(getEmpName());
        }
        return false;
    }

    @Override
    public int compareTo(Employee e) {
        String nameId = getEmpName() + getId();
        String nameId2 = e.getEmpName() + e.getId();
        return nameId.compareTo(nameId2);
    }
}

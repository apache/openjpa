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

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Employee entity with composite IdClass PK for derived identity ex2b test.
 * Mirrors TCK DID2bEmployee.
 */
@Entity
@Table(name = "UNE_DID2B_EMP")
@IdClass(UnenhancedDID2bEmployeeId.class)
public class UnenhancedDID2bEmployee implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    String firstName;

    @Id
    String lastName;

    int empNo;

    public UnenhancedDID2bEmployee() {
    }

    public UnenhancedDID2bEmployee(UnenhancedDID2bEmployeeId eId) {
        this(eId.getFirstName(), eId.getLastName());
    }

    public UnenhancedDID2bEmployee(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public int getEmpNo() {
        return empNo;
    }

    public void setEmpNo(int empNo) {
        this.empNo = empNo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}

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
package org.apache.openjpa.persistence.embed;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Employee with composite @IdClass PK (JPA 2.4.1.3 ex2b).
 */
@Entity
@Table(name = "DID2B_EMP")
@IdClass(DID2bEmployeeId.class)
public class DID2bEmployee implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id String firstName;
    @Id String lastName;

    public DID2bEmployee() {}
    public DID2bEmployee(DID2bEmployeeId eid) {
        this.firstName = eid.getFirstName();
        this.lastName = eid.getLastName();
    }
    public String getFirstName() { return firstName; }
    public void setFirstName(String fn) { firstName = fn; }
    public String getLastName() { return lastName; }
    public void setLastName(String ln) { lastName = ln; }
}

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

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Employee entity with composite EmbeddedId PK for derived identity ex3a test.
 * Mirrors TCK DID3Employee.
 */
@Entity
@Table(name = "UNE_DID3_EMP")
public class UnenhancedDID3Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    UnenhancedDID3EmployeeId empId;

    public UnenhancedDID3Employee() {
    }

    public UnenhancedDID3Employee(UnenhancedDID3EmployeeId eId) {
        this.empId = eId;
    }

    public UnenhancedDID3EmployeeId getEmpId() {
        return empId;
    }

    public void setEmpId(UnenhancedDID3EmployeeId empId) {
        this.empId = empId;
    }
}

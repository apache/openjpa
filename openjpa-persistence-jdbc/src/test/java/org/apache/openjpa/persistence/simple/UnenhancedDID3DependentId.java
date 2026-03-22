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

/**
 * IdClass for UnenhancedDID3Dependent.
 * Mirrors TCK DID3DependentId.
 * Note: field names must match entity field names.
 * The entity uses 'name2' and 'emp', so IdClass uses 'name2' and 'emp'.
 * 'emp' field type = DID3EmployeeId (the parent entity's EmbeddedId type).
 */
public class UnenhancedDID3DependentId implements Serializable {

    String name2;
    UnenhancedDID3EmployeeId emp;

    public UnenhancedDID3DependentId() {
    }

    public UnenhancedDID3DependentId(String name2, UnenhancedDID3EmployeeId emp) {
        this.name2 = name2;
        this.emp = emp;
    }

    public UnenhancedDID3EmployeeId getEmp() {
        return emp;
    }

    public void setEmp(UnenhancedDID3EmployeeId emp) {
        this.emp = emp;
    }

    public String getName() {
        return name2;
    }

    public void setName(String name2) {
        this.name2 = name2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnenhancedDID3DependentId)) {
            return false;
        }
        UnenhancedDID3DependentId other = (UnenhancedDID3DependentId) obj;
        if ((this.name2 == null) ? (other.name2 != null) : !this.name2.equals(other.name2)) {
            return false;
        }
        if (this.emp != other.emp && (this.emp == null || !this.emp.equals(other.emp))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (this.name2 != null ? this.name2.hashCode() : 0);
        return hash;
    }
}

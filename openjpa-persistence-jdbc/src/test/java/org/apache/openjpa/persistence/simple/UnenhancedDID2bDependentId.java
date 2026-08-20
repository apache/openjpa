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

import jakarta.persistence.Embeddable;

/**
 * EmbeddedId class for UnenhancedDID2bDependent.
 * Contains a field of type UnenhancedDID2bEmployeeId which is an @IdClass, NOT @Embeddable.
 * Mirrors TCK DID2bDependentId.
 */
@Embeddable
public class UnenhancedDID2bDependentId implements Serializable {

    String name;
    UnenhancedDID2bEmployeeId empPK;

    public UnenhancedDID2bDependentId() {
    }

    public UnenhancedDID2bDependentId(String name, UnenhancedDID2bEmployeeId emp) {
        this.name = name;
        this.empPK = emp;
    }

    public UnenhancedDID2bEmployeeId getEmpPK() {
        return empPK;
    }

    public void setEmpPK(UnenhancedDID2bEmployeeId emp) {
        this.empPK = emp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnenhancedDID2bDependentId)) {
            return false;
        }
        UnenhancedDID2bDependentId other = (UnenhancedDID2bDependentId) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.empPK != other.empPK && (this.empPK == null || !this.empPK.equals(other.empPK))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}

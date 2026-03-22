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
 * EmbeddedId class for UnenhancedDID1bDependent.
 * Mirrors TCK DID1bDependentId.
 */
@Embeddable
public class UnenhancedDID1bDependentId implements Serializable {

    String name;
    long empPK;

    public UnenhancedDID1bDependentId() {
    }

    public UnenhancedDID1bDependentId(String name, long emp) {
        this.name = name;
        this.empPK = emp;
    }

    public long getEmpPK() {
        return empPK;
    }

    public void setEmpPK(long emp) {
        this.empPK = emp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return (int) empPK;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UnenhancedDID1bDependentId)) {
            return false;
        }
        UnenhancedDID1bDependentId other = (UnenhancedDID1bDependentId) object;
        if (this.empPK != other.empPK || this.name == null || !(this.name.equals(other.name))) {
            return false;
        }
        return true;
    }
}

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Dependent entity with @IdClass and @Id @ManyToOne for derived identity ex1a test.
 * Mirrors TCK DID1Dependent.
 */
@Entity
@Table(name = "UNE_DID1_DEP")
@IdClass(UnenhancedDID1DependentId.class)
public class UnenhancedDID1Dependent implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    String name;

    @Id
    @ManyToOne
    UnenhancedDID1Employee emp;

    public UnenhancedDID1Dependent() {
    }

    public UnenhancedDID1Dependent(String name, UnenhancedDID1Employee emp) {
        this.name = name;
        this.emp = emp;
    }

    public UnenhancedDID1Employee getEmp() {
        return emp;
    }

    public void setEmp(UnenhancedDID1Employee emp) {
        this.emp = emp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UnenhancedDID1Dependent)) {
            return false;
        }
        UnenhancedDID1Dependent other = (UnenhancedDID1Dependent) object;
        if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }
}

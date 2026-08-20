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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Dependent entity with @EmbeddedId + @MapsId for derived identity ex2b test.
 * The EmbeddedId contains a field of type DID2bEmployeeId (an @IdClass, NOT @Embeddable).
 * Mirrors TCK DID2bDependent.
 */
@Entity
@Table(name = "UNE_DID2B_DEP")
public class UnenhancedDID2bDependent implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    UnenhancedDID2bDependentId id;

    @MapsId("empPK")
    @JoinColumns({
        @JoinColumn(name = "FIRSTNAME", referencedColumnName = "firstName"),
        @JoinColumn(name = "LASTNAME", referencedColumnName = "lastName")
    })
    @ManyToOne
    UnenhancedDID2bEmployee emp;

    public UnenhancedDID2bDependent() {
    }

    public UnenhancedDID2bDependent(UnenhancedDID2bDependentId dId, UnenhancedDID2bEmployee emp) {
        this.id = dId;
        this.emp = emp;
    }

    public UnenhancedDID2bEmployee getEmp() {
        return emp;
    }

    public void setEmp(UnenhancedDID2bEmployee emp) {
        this.emp = emp;
    }

    public String getName() {
        return this.id.name;
    }

    public void setName(String name) {
        this.id.name = name;
    }
}

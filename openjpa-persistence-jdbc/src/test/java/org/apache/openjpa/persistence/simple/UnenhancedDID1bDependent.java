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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Dependent entity with @EmbeddedId + @MapsId for derived identity ex1b/mapsid test.
 * Mirrors TCK DID1bDependent.
 */
@Entity
@Table(name = "UNE_DID1B_DEP")
public class UnenhancedDID1bDependent implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    UnenhancedDID1bDependentId id;

    @MapsId("empPK")
    @ManyToOne
    UnenhancedDID1bEmployee emp;

    public UnenhancedDID1bDependent() {
    }

    public UnenhancedDID1bDependent(UnenhancedDID1bDependentId id, UnenhancedDID1bEmployee emp) {
        this.id = id;
        this.emp = emp;
    }

    public UnenhancedDID1bEmployee getEmp() {
        return emp;
    }

    public void setEmp(UnenhancedDID1bEmployee emp) {
        this.emp = emp;
    }
}

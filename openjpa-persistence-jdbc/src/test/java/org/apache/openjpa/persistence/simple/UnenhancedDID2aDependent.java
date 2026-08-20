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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Dependent entity with @IdClass + @Id @ManyToOne to composite parent.
 * Mirrors TCK DID2Dependent (ex2a pattern).
 */
@Entity
@IdClass(UnenhancedDID2aDependentId.class)
@Table(name = "UNE_DID2A_DEP")
public class UnenhancedDID2aDependent implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    String name;

    @Id
    @JoinColumns({
        @JoinColumn(name = "FK_FIRSTNAME", referencedColumnName = "firstName"),
        @JoinColumn(name = "FK_LASTNAME", referencedColumnName = "lastName")
    })
    @ManyToOne
    UnenhancedDID2aEmployee emp;

    public UnenhancedDID2aDependent() {
    }

    public UnenhancedDID2aDependent(UnenhancedDID2aDependentId dId, UnenhancedDID2aEmployee emp) {
        this.name = dId.getName();
        this.emp = emp;
    }

    public UnenhancedDID2aDependent(String name, UnenhancedDID2aEmployee emp) {
        this.name = name;
        this.emp = emp;
    }

    public UnenhancedDID2aEmployee getEmp() {
        return emp;
    }

    public void setEmp(UnenhancedDID2aEmployee emp) {
        this.emp = emp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

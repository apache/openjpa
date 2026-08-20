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

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Dependent with @EmbeddedId + @MapsId targeting non-@Embeddable IdClass field
 * (JPA 2.4.1.3 ex2b).
 */
@Entity
@Table(name = "DID2B_DEP")
public class DID2bDependent implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    DID2bDependentId id;

    @MapsId("empPK")
    @JoinColumns({
        @JoinColumn(name = "FK_FIRSTNAME", referencedColumnName = "firstName"),
        @JoinColumn(name = "FK_LASTNAME", referencedColumnName = "lastName")
    })
    @ManyToOne
    DID2bEmployee emp;

    public DID2bDependent() {}
    public DID2bDependent(DID2bDependentId did, DID2bEmployee e) {
        this.id = did;
        this.emp = e;
    }
    public DID2bDependentId getId() { return id; }
    public DID2bEmployee getEmp() { return emp; }
    public void setEmp(DID2bEmployee e) { emp = e; }
}

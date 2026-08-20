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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Dependent entity with @IdClass and @Id @ManyToOne for derived identity ex3a test.
 * Parent has composite EmbeddedId (two strings).
 * Mirrors TCK DID3Dependent.
 */
@Entity
@Table(name = "UNE_DID3_DEP")
@IdClass(UnenhancedDID3DependentId.class)
public class UnenhancedDID3Dependent implements Serializable {

    @Id
    @Column(name = "NAME")
    String name2;

    @Id
    @JoinColumns({
        @JoinColumn(name = "FIRSTNAME", referencedColumnName = "firstName"),
        @JoinColumn(name = "LASTNAME", referencedColumnName = "lastName")
    })
    @ManyToOne
    UnenhancedDID3Employee emp;

    public UnenhancedDID3Dependent() {
    }

    public UnenhancedDID3Dependent(String name2, UnenhancedDID3Employee emp) {
        this.name2 = name2;
        this.emp = emp;
    }

    public UnenhancedDID3Dependent(UnenhancedDID3DependentId dId, UnenhancedDID3Employee emp) {
        this.name2 = dId.getName();
        this.emp = emp;
    }

    public UnenhancedDID3Employee getEmp() {
        return emp;
    }

    public void setEmp(UnenhancedDID3Employee emp) {
        this.emp = emp;
    }

    public String getName() {
        return this.name2;
    }

    public void setName(String name2) {
        this.name2 = name2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnenhancedDID3Dependent)) {
            return false;
        }
        UnenhancedDID3Dependent other = (UnenhancedDID3Dependent) obj;
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
        int hash = 7;
        hash = 31 * hash + (this.name2 != null ? this.name2.hashCode() : 0);
        return hash;
    }
}

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
package org.apache.openjpa.persistence.common.apps;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.PersistentCollection;

/**
 * This hierarchy and the unrelated one rooted at
 * {@code org.apache.openjpa.persistence.criteria.CompUser} would both default to a table
 * called {@code CompUser}, with incompatible column types ({@code userid} nullable or not,
 * {@code ADD_ID} INTEGER or BIGINT). Whichever test package runs second therefore makes
 * {@code SynchronizeMappings=buildSchema(SchemaAction='drop,add')} drop and re-add every
 * column of that table on every EntityManagerFactory it creates. MariaDB performs such an
 * ADD/DROP COLUMN instantly and keeps the dropped columns in the record format, counting
 * them towards the 8126 byte maximum row size, so after about a dozen rotations the next
 * ALTER TABLE fails with "Row size too large" (OPENJPA-3016). An explicit, distinct table
 * name keeps the two hierarchies apart and the suite independent of test ordering.
 */
@Entity
@Table(name = "CMN_COMPUSER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class CompUser {
    @Basic
    @Column(length = 50)
    private String name;

    @Basic
    private int age;

    @Basic
    @Column(name = "compName", length = 50)
    private String computerName;

    @Basic
    @Lob
    private String nameAsLob;

    @PersistentCollection
    private String[] nicknames = new String[0];

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    @JoinColumn(name = "ADD_ID")
    private Address address;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int userid;

    @Enumerated
    @Basic
    private CreditRating creditRating;

    public CompUser() {
    }

    public CompUser(String name, String cName, Address address, int age) {
        this.name = name;
        this.computerName = cName;
        this.address = address;
        this.age = age;
        this.nameAsLob = name;
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserid() {
        return userid;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getNameAsLob() {
        return nameAsLob;
    }

    public void setNameAsLob(String nameAsLob) {
        this.nameAsLob = nameAsLob;
    }

    public CreditRating getRating() {
        return creditRating;
    }

    public void setRating(CreditRating rating) {
        this.creditRating = rating;
    }

    public enum CreditRating {
        POOR, GOOD, EXCELLENT
    }

    public String[] getNicknames() {
        return nicknames;
    }

    public void setNicknames(String[] nicknames) {
        this.nicknames = nicknames;
    }

    public CreditRating getCreditRating() {
        return creditRating;
    }

    public void setCreditRating(CreditRating creditRating) {
        this.creditRating = creditRating;
    }
}

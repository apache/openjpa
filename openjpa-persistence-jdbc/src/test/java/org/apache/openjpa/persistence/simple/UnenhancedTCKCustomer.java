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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Mimics the TCK schema30 Customer entity.
 * Uses PROPERTY access (annotations on getters).
 * Has two @OneToOne(cascade=ALL) relationships to Address
 * with explicit @JoinColumn names, plus an @Embedded Country.
 * Named with "Unenhanced" prefix to skip build-time enhancement.
 */
@Entity
@Table(name = "UTCK_CUSTOMER")
public class UnenhancedTCKCustomer implements java.io.Serializable {

    private String id;
    private String name;
    private UnenhancedTCKAddress home;
    private UnenhancedTCKAddress work;
    private UnenhancedTCKCountry country;

    public UnenhancedTCKCustomer() {
    }

    public UnenhancedTCKCustomer(String id, String name,
                                 UnenhancedTCKAddress home,
                                 UnenhancedTCKAddress work,
                                 UnenhancedTCKCountry country) {
        this.id = id;
        this.name = name;
        this.home = home;
        this.work = work;
        this.country = country;
    }

    @Id
    @Column(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String v) {
        this.id = v;
    }

    @Column(name = "NAME")
    public String getName() {
        return name;
    }

    public void setName(String v) {
        this.name = v;
    }

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK6_FOR_CUSTOMER_TABLE")
    public UnenhancedTCKAddress getHome() {
        return home;
    }

    public void setHome(UnenhancedTCKAddress v) {
        this.home = v;
    }

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK5_FOR_CUSTOMER_TABLE")
    public UnenhancedTCKAddress getWork() {
        return work;
    }

    public void setWork(UnenhancedTCKAddress v) {
        this.work = v;
    }

    @Embedded
    public UnenhancedTCKCountry getCountry() {
        return country;
    }

    public void setCountry(UnenhancedTCKCountry v) {
        this.country = v;
    }
}
